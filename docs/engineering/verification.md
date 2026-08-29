# Verification

What each quality gate checks, where it is configured, and which ones actually fail a build. The
commands themselves live in the [README](../../README.md#getting-started) and
[`CONTRIBUTING.md`](../../CONTRIBUTING.md), because a contributor needs them immediately; this
document is for anyone who wants to know what those commands are checking and why.

## The gates at a glance

| Gate | Enforces | Configured in | Fails CI |
|---|---|---|---|
| Spotless (ktlint + compose-rules) | Formatting and Compose naming/parameter rules | [`SpotlessPlugin.kt`](../../build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/SpotlessPlugin.kt) | Yes — `spotlessCheck` |
| Detekt | Static analysis of Kotlin, including `build-logic` itself | [`.detekt/detekt.yml`](../../.detekt/detekt.yml), [`DetektPlugin.kt`](../../build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/DetektPlugin.kt) | Yes — at **warning** severity |
| Android Lint | Android/API correctness, resources, manifest, accessibility | AGP defaults; `lint { }` in [`app/build.gradle.kts`](../../app/build.gradle.kts) | Yes — `lintDebug` |
| `validateAgentContext` | Structure of the shared AI context files | [`ValidateAgentContextTask.kt`](../../build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/ValidateAgentContextTask.kt) | Yes — in its own workflow |
| LeakCanary | Activity/Fragment/ViewModel leaks at runtime | `debugImplementation` in [`app/build.gradle.kts`](../../app/build.gradle.kts) | No — advisory, debug builds only |

There is no unit or instrumentation test suite, by choice. See
[coding standards](coding-standards.md#dependencies-and-tests).

## Spotless

`spotlessApply` is the one gate you must run locally before committing; `spotlessCheck` is what CI
runs. It applies ktlint to Kotlin sources, Gradle Kotlin DSL scripts, and trailing
whitespace/newlines in `.github` YAML, with the ktlint
[compose-rules](https://mrmans0n.github.io/compose-rules/) custom rule set layered on top.

Non-default overrides, all in
[`SpotlessPlugin.kt`](../../build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/SpotlessPlugin.kt):

* function and class signatures go multiline from three parameters up
* `CompositionLocal` allowlist and `lambda-param-in-effect` compose rules are disabled
* `@Composable` functions are exempt from the function-naming rule, so `PascalCase` composables pass

**Spotless does not report unused imports.** Deleting the last use of a symbol leaves its import
behind and no gate will tell you — check by hand. The
[`spotless-fix`](../../.claude/skills/spotless-fix/SKILL.md) skill covers the fix loop for
violations that `spotlessApply` can't resolve on its own.

## Detekt

Detekt runs with `buildUponDefaultConfig`, the repository config at
[`.detekt/detekt.yml`](../../.detekt/detekt.yml), and — the part people trip over —
`failOnSeverity = Warning`. A Detekt *warning* fails the build, so there is no accumulating backlog
of "known" findings.

`build-logic` is analysed too, via `:build-logic:convention:detektMain`; a convention plugin is
production code here.

The rule most often hit in this codebase is `UnusedPrivateProperty`: a constructor parameter that is
only read in a property initializer (a `combine` chain, for example) must be declared **without**
`private val`. `AppsViewModel` and `FilterViewModel` both do this.

Detekt writes SARIF, which CI merges with `mergeSarifReports` into `build/reports/merged.sarif` and
uploads to GitHub code scanning, so findings show up as annotations on the PR rather than only in a
log.

## Android Lint

`lintDebug` runs AGP's Android Lint across modules — API-level misuse, resource and manifest
problems, obsolete or unsafe platform calls, accessibility issues. Given `minSdk` 28 and `targetSdk`
37, Lint is the gate that catches "this call needs an API check" before a device does.

The only suppression is `MissingTranslation` in [`app/build.gradle.kts`](../../app/build.gradle.kts),
because locales ship incrementally — a string is added in English first and translated in a follow-up
(see the [`translate-strings`](../../.claude/skills/translate-strings/SKILL.md) skill).

## LeakCanary

LeakCanary is a `debugImplementation` dependency, so it exists in debug builds only and never
touches a release APK. It is advisory: no build fails because of it. Watch its notifications while
exercising a change that holds a `Context`, a `ViewModel`, or a long-lived callback — the analysis
screens keep hold of large parsed structures, so a leaked screen is not a cheap leak.

## What no gate catches

Two things a green build proves nothing about:

* **Layout correctness.** A clean compile says nothing about a Compose screen. Text that wraps to
  three lines in a fixed-width column, or an item sheet that never opens, passes every gate. For any
  visual change, run it on a device — the [`run-app`](../../.claude/skills/run-app/SKILL.md) and
  [`navigate-app-adb`](../../.claude/skills/navigate-app-adb/SKILL.md) skills automate the loop.
* **Unused imports**, as above.

## Running the gates

```bash
./gradlew spotlessApply                                   # required before every commit
./gradlew :feature:apps:impl:compileDebugKotlin           # fast single-module check while iterating
./gradlew spotlessCheck detektDebug :build-logic:convention:detektMain lintDebug :app:assembleDebug
./gradlew validateAgentContext                            # after touching AGENTS.md or a skill
```

Don't run the full check after every edit — CI runs it on every push. Iterate on a single-module
compile and let CI catch the rest.

## In CI

Every push and PR to `develop` runs `spotlessCheck detektDebug
:build-logic:convention:detektMain lintDebug --continue` and uploads the merged SARIF to code
scanning. The jobs, artifacts, and the rest of the pipeline are in
[CI and release](ci-and-release.md).

The [`analyze-ci-failure`](../../.claude/skills/analyze-ci-failure/SKILL.md) skill is the procedure
for turning a red run into a root cause.

## Related

* [Coding standards](coding-standards.md) — the rules behind the findings
* [`docs/technical/kotlin-code-quality.md`](../technical/kotlin-code-quality.md) — the standing
  code-quality audit
