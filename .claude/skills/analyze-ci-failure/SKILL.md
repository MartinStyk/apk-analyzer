---
name: analyze-ci-failure
description: Use to check GitHub Actions build status or diagnose why a workflow run failed and propose a fix. Triggered by phrases like "check the latest build", "did CI pass", "why did the build fail", "check the GitHub Actions run", "analyze this workflow run", "check run <id>", "check the build for this PR".
---

# Skill: Analyze a GitHub Actions Run and Propose a Fix

> Uses the `gh` CLI (already authenticated) to inspect workflow runs on GitHub Actions, isolate the actual failing step from a noisy log, and turn it into a root cause + a proposed fix.

## Workflows in this repo

Both live at `.github/workflows/`.

| File | Name | Trigger | What it runs |
|---|---|---|---|
| `.github/workflows/android.yml` | `Continuous integration` | push to `develop` | `spotlessCheck`, `assembleDebug`, `lintDebug` (separate steps) |
| `.github/workflows/android-publish.yml` | `Release` | manual (`workflow_dispatch`, needs `versionNumber` + `versionName` inputs) | build, sign, and upload a release AAB to the Play Store beta track |

## Step 1 — Find the run

| Given | Command |
|---|---|
| "latest build" / "latest CI run" | `gh run list --workflow android.yml --limit 5` |
| "latest release run" | `gh run list --workflow android-publish.yml --limit 5` |
| A run ID or run URL | `gh run view <run-id>` |
| A PR number | `gh pr checks <pr-number>` |
| Only failures | `gh run list --status failure --limit 10` |

## Step 2 — Check job-level status, not just the run-level conclusion

`gh run view <run-id>` prints a ✓/X per job — read this before pulling logs. Both workflows here
have a single `build` job, so this step is less critical than in multi-job pipelines, but still
confirm which specific step inside `build` failed before assuming the cause.

## Step 3 — Pull only the failing job's log, then grep for the signal

```bash
gh run view <run-id> --log-failed | grep -inE "error:|FAILURE:|Execution failed|exception|##\[error\]"
```

Then pull context around the matching line number(s):

```bash
gh run view <run-id> --log-failed | sed -n '<start>,<end>p'
```

If grep finds nothing useful, the failure may be a plain `BUILD FAILED` with no separate `error:`
line — in that case find the last `> Task :...` line before the failure; that's the failing
Gradle task.

## Step 4 — Known failure signatures in this repo

| Symptom in the log | Root cause | Fix |
|---|---|---|
| `Task 'buildFreeRelease'`/`'bundleFreeRelease' not found` in `android-publish.yml`, or `releaseDirectory: app/build/outputs/bundle/freeRelease` not found by the signing step | `android-publish.yml` (unlike `android.yml`, this one is **not yet fixed**) builds `buildFreeRelease`/`bundleFreeRelease` and reads from a `freeRelease` output directory, but there is **no `free` product flavor** anywhere in this codebase (`build-logic`/`app` define no `productFlavors`) — this task reference is stale | Replace with the plain `release` build type equivalents: `./gradlew assembleRelease`/`bundleRelease`, output at `app/build/outputs/bundle/release`. Confirm with the user before editing — CI/CD pipeline changes are a shared-system change |
| `BUILD FAILED` with ktlint-style messages or Compose compile errors traceable to formatting | Unformatted/violating Kotlin broke compilation | Use the `spotless-fix` skill |
| `e: file:///path/to/File.kt:12:5 ...` | Kotlin compiler error, points directly at file:line | Open the file, fix per `AGENTS.md` conventions |
| Signing step fails in `android-publish.yml` (`sign_app`) | One of `SIGN_KEY`, `SIGN_KEY_ALIAS`, `SIGN_KEY_STORE_PASSWORD`, `SIGN_KEY_PASSWORD` repo secrets is missing/wrong, or the keystore is corrupt/mismatched with the alias | `gh secret list` to confirm all four exist; can't verify the keystore itself from CI logs — ask the user to check it locally |
| `Deploy to Play Store` step fails with an auth/permission error | `GOOGLE_SERVICE_ACCOUNT` secret is missing, expired, or the service account lacks Play Console API access for `sk.styk.martin.apkanalyzer` | Check `gh secret list`; Play Console access itself can't be fixed from CI — flag to the user |
| `Create production google-services.json` step produces an empty/invalid file | `GOOGLE_SERVICES` secret is missing or doesn't contain valid JSON | `gh secret list` to confirm it's set; content itself can't be inspected from the log (secrets are masked) |
| `android-publish.yml` still sets up JDK 17 (`android.yml` was bumped to JDK 21) but the project's Gradle toolchain wants 21 (`jvmToolchain(21)`, `gradle-daemon-jvm.properties`) | **Usually not the actual failure cause even so** — `gradle-daemon-jvm.properties` has `toolchainUrl.*` entries for the foojay Disco API, so Gradle auto-downloads a matching JDK 21 regardless of the JDK `setup-java` installed. Don't assume a JDK mismatch is the root cause without other evidence | If you do suspect it, search the log for `Downloading` / `Unpacking JDK` lines from the toolchain auto-provisioning, or a `No matching toolchains found` error, before concluding this is the cause |
| `gradle: Execution failed for task ':...:compileDebugKotlin'` or similar per-module compile task | Compile error scoped to one module | Narrow reproduction locally: `./gradlew :module:path:compileDebugKotlin` |

If a failure doesn't match this table, treat the grepped `error:`/`FAILURE:` line as authoritative
and reason from there — don't guess.

## Step 5 — Reproduce and propose a fix

1. Where possible, reproduce locally with the same Gradle task the workflow ran (see `run:` steps
   in the workflow YAML) before proposing a fix — this repo's CI failures are almost always
   reproducible with `./gradlew <same task>`.
2. State the root cause in one sentence, citing the exact log line.
3. Propose a minimal diff:
   - Workflow YAML bug → confirm with the user before editing `.github/workflows/*.yml` (CI/CD
     pipeline changes are a shared-system change — don't push them unasked).
   - Application/build code bug → fix the code per `AGENTS.md` conventions and run the
     `spotless-fix` skill if it's formatting-related.
4. **Never "fix" a failure by hiding it** — no removing the failing step, no
   `continue-on-error: true`, no `|| true`, no disabling a check — unless the user explicitly asks
   for that. The goal is root cause, not a green checkmark.

## Verification

- [ ] Identified the specific failing step (not just "the run failed")
- [ ] Found the actual `error:`/`FAILURE:` line, not just generic exit-code noise
- [ ] Root cause stated as one sentence with a cited log line
- [ ] Fix reproduces/resolves locally where the failure type allows it (e.g. a Gradle task)
- [ ] Fix addresses the cause, not just silences the check
