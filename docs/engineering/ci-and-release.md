# CI and release

Everything that runs on GitHub Actions: what triggers each workflow, what it produces, and which
secrets it needs. What the verification gates themselves check is in
[verification.md](verification.md); this document covers the pipeline around them.

## Workflows

| Workflow | Trigger | Jobs | Produces |
|---|---|---|---|
| [`ci.yml`](../../.github/workflows/ci.yml) | Push and PR to `develop` | `verify`, `build-apk`, `distribute` | SARIF results, a debug APK artifact, an internal-tester build on push |
| [`agent-context.yml`](../../.github/workflows/agent-context.yml) | Push and PR to `develop` touching a context file, a skill, or the module graph | `validate` | Pass/fail on `validateAgentContext` |
| [`release.yml`](../../.github/workflows/release.yml) | Push of a `MAJOR.MINOR.PATCH` tag | `check-tag`, `verify`, `build-release`, `publish-github-release`, `publish-play-store`, `distribute-app-distribution` | Signed AAB and APK, a GitHub release, a Play beta release, an internal-tester build |
| [`promote-release.yml`](../../.github/workflows/promote-release.yml) | Manual `workflow_dispatch` | `promote` | A Play production release at the chosen rollout percentage |

`ci.yml` and `agent-context.yml` cancel in-progress runs for the same ref. `ci.yml` skips runs for
pushes that only touch Markdown, `.claude/`, `LICENSE`, `.editorconfig`, or `.gitignore` — the
`pull_request` trigger has no such filter, so a docs-only PR still gets a full run.

## Continuous integration

`verify` and `build-apk` run in parallel; neither waits on the other.

**`verify`** runs `spotlessCheck detektDebug :build-logic:convention:detektMain lintDebug
--continue`, so a failure in one tool doesn't hide findings from the rest. It then runs
`mergeSarifReports` — with `if: always()`, because the reports are only interesting when something
failed — and uploads the merged `build/reports/merged.sarif` to code scanning. The upload is skipped
for pull requests from forks, which have no `security-events: write` token.

**`build-apk`** assembles a debug APK named after where it came from: `pr<number>.<run number>`
for a pull request, `dev.<run number>` for a push to `develop`, passed through `-Pversion.name`.
The file is renamed to `apk-analyzer.apk` and uploaded as the `apk-analyzer` artifact with a 14-day
retention and no recompression, so the download is the APK itself.

**`distribute`** needs both jobs and runs only on `push`, never on a pull request. It downloads the
artifact and sends it to the `internal-testers` group on Firebase App Distribution, with release
notes built from the head commit message plus the commit SHA and a link back to the run.

## Release

Pushing an annotated `MAJOR.MINOR.PATCH` tag starts the release. The tag is the only input: it
supplies the version name, the version code, and the release notes.

**`check-tag`** reads the tag annotation and fails with a remediation message if it's empty — a
lightweight tag can't provide release notes. The annotation body becomes the GitHub release body.

**`verify`** repeats the CI gates but against the release variant (`lintRelease` rather than
`lintDebug`), so shrinker- and release-only lint findings block the release.

**`build-release`** derives `versionCode` from the tag as `MAJOR * 10000 + MINOR * 100 + PATCH`, and
passes the tag itself as `version.name`. Before building it asserts that all four signing secrets
are non-empty and fails if any is missing: the release `signingConfig` falls back to the debug key
when they're absent, and a debug-signed release that uploads successfully is worse than a failed
build. It then decodes the base64 keystore into the runner temp directory, appends the `signing.*`
properties to `~/.gradle/gradle.properties`, and runs `bundleRelease assembleRelease`. The AAB,
APK, and ProGuard `mapping.txt` are uploaded as three separate artifacts, the first two named
`apk-analyzer-<tag>`.

Three publishing jobs then run in parallel:

* **`publish-github-release`** attaches the APK to a GitHub release named after the tag, with the
  tag annotation as the body.
* **`publish-play-store`** runs `:app:publishBundle --track beta`, which uploads the AAB together
  with its mapping file. New releases always land on **beta**, never production.
* **`distribute-app-distribution`** sends the release APK to the `internal-testers` group with the
  tag as its release notes.

## Promotion to production

`promote-release.yml` is the only path to the production track, and it is always a deliberate manual
step. It takes two inputs:

| Input | Required | Meaning |
|---|---|---|
| `version` | Yes | The version being promoted, e.g. `3.5.0`. Used for the run name and the log only — it does not select which release is promoted. |
| `rollout-percentage` | No, defaults to `100` | Share of production users receiving the update, 1–100. Rejected unless it's a whole number in that range. |

The job first asks the Play Developer API whether the production track already has a release. If it
does, the run **updates** that release's rollout rather than promoting again; if the track is empty,
it promotes from **beta**. A rollout of 100 finishes the release as `completed`; anything lower sets
`inProgress` with the matching user fraction, so re-running with a higher percentage is how a staged
rollout is widened.

Because the promoted release is whatever currently sits on beta, promote soon after the release run
finishes — or check the beta track first.

## Shared pieces

Two composite actions keep the workflows from repeating themselves. Both assume `actions/checkout`
has already run.

* [`setup-gradle-build`](../../.github/actions/setup-gradle-build/action.yml) — installs Temurin JDK
  25, sets up Gradle with build scans enabled, and makes `gradlew` executable. Used by every job
  that invokes Gradle except `agent-context.yml`, which inlines the same steps.
* [`fetch-google-services`](../../.github/actions/fetch-google-services/action.yml) — replaces
  `app/google-services.json` with the real configuration pulled through the Firebase CLI, so the
  placeholder committed to the repository never reaches a CI build.

### Secrets

Names only; values live in repository settings.

| Secret | Used by |
|---|---|
| `FIREBASE_TOKEN`, `FIREBASE_APP_ID`, `FIREBASE_PROJECT_ID` | `fetch-google-services` in `ci.yml` and `release.yml` |
| `FIREBASE_TOKEN`, `FIREBASE_APP_ID` | The App Distribution steps in `ci.yml` and `release.yml` |
| `SIGN_KEY`, `SIGN_KEY_ALIAS`, `SIGN_KEY_STORE_PASSWORD`, `SIGN_KEY_PASSWORD` | `build-release` in `release.yml` |
| `GOOGLE_SERVICE_ACCOUNT` | `publish-play-store` in `release.yml`, and both Play steps in `promote-release.yml` |

`agent-context.yml` needs no secrets.

## Related

* [Verification](verification.md) — what each gate checks locally and in CI
* [AI workflow](ai-workflow.md) — what `validateAgentContext` enforces
* [`analyze-ci-failure`](../../.claude/skills/analyze-ci-failure/SKILL.md) — turning a red run into
  a root cause
