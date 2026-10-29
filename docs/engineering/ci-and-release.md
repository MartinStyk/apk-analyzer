# CI and release

Everything that runs on GitHub Actions: what triggers each workflow, what it produces, and which
secrets it needs. What the verification gates themselves check is in
[verification.md](verification.md); this document covers the pipeline around them.

## Workflows

| Workflow | Trigger | Jobs | Produces |
|---|---|---|---|
| [`ci.yml`](../../.github/workflows/ci.yml) | Push and PR to `develop` | `verify`, `build-apk`, `distribute` | SARIF results, a debug APK artifact, an internal-tester build on push |
| [`agent-context.yml`](../../.github/workflows/agent-context.yml) | Push and PR to `develop` touching a context file, a skill, or the module graph | `validate` | Pass/fail on `validateAgentContext` |
| [`release.yml`](../../.github/workflows/release.yml) | Push of a `MAJOR.MINOR.PATCH` tag | `check-tag`, `verify`, `build-release`, `publish-github-release`, `publish-play-store`, `distribute-app-distribution` | Signed phone and TV AABs and APKs, a GitHub release, Play beta releases for both apps, an internal-tester build |
| [`promote-release.yml`](../../.github/workflows/promote-release.yml) | Manual `workflow_dispatch` | `promote` | A Play production release of the phone app, the TV app, or both, at the chosen rollout percentage |

`ci.yml` and `agent-context.yml` cancel in-progress runs for the same ref. `ci.yml` skips runs for
pushes that only touch Markdown, `.claude/`, `LICENSE`, `.editorconfig`, or `.gitignore` — the
`pull_request` trigger has no such filter, so a docs-only PR still gets a full run.

## Continuous integration

`verify` and `build-apk` run in parallel; `distribute` needs both and runs only on `push`, never on
a pull request.

| Job | What it does |
|---|---|
| `verify` | Runs `spotlessCheck detektDebug :build-logic:convention:detektMain lintDebug --continue`, merges the SARIF reports, and uploads to code scanning |
| `build-apk` | Assembles a debug APK, renames it to `apk-analyzer.apk`, and uploads it with 14-day retention and no recompression |
| `distribute` | Downloads the APK and sends it to the `internal-testers` group on Firebase App Distribution |

Non-obvious details:

* The SARIF merge runs with `if: always()` and is skipped for fork PRs, which have no
  `security-events: write` token.
* `build-apk` names the file `pr<number>.<run number>` for a PR or `dev.<run number>` for a push to
  `develop`, passed through `-Pversion.name`.
* `distribute`'s release notes are the head commit message plus the commit SHA and a link back to
  the run.

## Release

Pushing an annotated `MAJOR.MINOR.PATCH` tag starts the release of both apps. The tag is the only
input: it supplies the version name, the version codes, and the release notes. Whether a release
reaches phone or TV users is decided later, at promotion.

| Job | What it does |
|---|---|
| `check-tag` | Fails with a remediation message if the tag annotation is empty — a lightweight tag can't provide release notes. The annotation body becomes the GitHub release body |
| `verify` | Repeats the CI gates against the release variant (`lintRelease` rather than `lintDebug`) |
| `build-release` | One matrix leg per app (`app`, `tv`): derives `versionCode` from the tag, decodes the signing keystore, runs `bundleRelease assembleRelease`, and uploads the AAB, APK, and ProGuard `mapping.txt` as separate artifacts |
| `publish-github-release` | Attaches both APKs to a GitHub release named after the tag |
| `publish-play-store` | One matrix leg per app: the phone bundle goes to `beta`, the TV bundle to `tv:Beta TV`. New releases never land on production |
| `distribute-app-distribution` | Sends the phone release APK to `internal-testers`, tagged with the release notes |

Non-obvious details:

* `versionCode` is `MAJOR * 10000 + MINOR * 100 + PATCH`, plus `10000000` for the TV app because
  Play requires unique version codes across all tracks of the same application ID. It is computed
  only by [`version-code.sh`](../../.github/scripts/version-code.sh); the tag itself becomes
  `version.name` for both apps.
* `build-release` fails outright if any of the four signing secrets is missing, rather than falling
  back to the debug key — a debug-signed release that uploads successfully is worse than a failed
  build.
* The phone and TV legs are independent: `fail-fast` is off, and the Play and App Distribution jobs
  run whenever `verify` passed, so a TV build failure doesn't block the phone upload (the TV publish
  leg then fails on its missing artifact). The GitHub release needs both builds.
* Both apps share one application ID, so concurrent Play edits would invalidate each other. The
  `publish-play-store` legs, and the `both` legs of `promote-release.yml`, run with
  `max-parallel: 1`.
* `tv:Beta TV` is the closed testing track named "Beta TV" on Play's dedicated Android TV form
  factor; a custom track's API name is `tv:` plus its exact name.

## Promotion to production

`promote-release.yml` is the only path to the production track, and it is always a deliberate manual
step.

| Input | Required | Meaning |
|---|---|---|
| `version` | Yes | The version being promoted, e.g. `3.5.0`. Selects the version code to promote on each app's track |
| `form-factor` | Yes, defaults to `phone` | `phone`, `tv`, or `both`. `both` runs one independent matrix leg per app, one after the other |
| `rollout-percentage` | No, defaults to `100` | Share of production users receiving the update, 1–100 |

* Promote an app only when the release changes something its users can see. Every release still
  lands on both beta tracks, so production versions of the two apps diverge and skip numbers.
* Each upload replaces the previous release on a beta track. A version can only be promoted while
  it is still there — once a newer tag has been released, promote that one instead; it contains
  everything earlier.
* If production already has an in-progress release of the same version, the run **updates** its
  rollout instead of promoting again; otherwise it promotes from the app's beta track. A halted
  production rollout fails the run on purpose.
* A rollout of `100` finishes the release as `completed`; anything lower sets `inProgress` at that
  percentage, so re-running with a higher number widens a staged rollout.

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

* [`release-app`](../../.claude/skills/release-app/SKILL.md) — cutting a version: picking it,
  drafting and approving the notes, and creating the annotated tag
* [Verification](verification.md) — what each gate checks locally and in CI
* [AI workflow](ai-workflow.md) — what `validateAgentContext` enforces
* [`analyze-ci-failure`](../../.claude/skills/analyze-ci-failure/SKILL.md) — turning a red run into
  a root cause
