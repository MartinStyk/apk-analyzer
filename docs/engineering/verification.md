# Verification

What each quality gate checks, where it is configured, and which ones actually fail a build. The
commands themselves live in the [README](../../README.md#getting-started) and
[`CONTRIBUTING.md`](../../CONTRIBUTING.md), because a contributor needs them immediately; this
document is for anyone who wants to know what those commands are checking and why.

## The gates at a glance

| Gate | Enforces | Configured in | Fails CI |
|---|---|---|---|
| Spotless (ktlint + compose-rules) | Formatting and Compose naming/parameter rules | [`SpotlessPlugin.kt`](../../build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/SpotlessPlugin.kt) | Yes — `spotlessCheck` |
| Detekt | Static analysis of Kotlin, including `build-logic` itself, plus the custom `apkanalyzer` guardrail rules | [`.detekt/detekt.yml`](../../.detekt/detekt.yml), [`DetektPlugin.kt`](../../build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/DetektPlugin.kt) | Yes — at **warning** severity |
| Android Lint | Android/API correctness, resources, manifest, accessibility | AGP defaults; `lint { }` in [`app/build.gradle.kts`](../../app/build.gradle.kts) | Yes — `lintDebug` |
| `validateAgentContext` | Structure of the shared AI context files | [`ValidateAgentContextTask.kt`](../../build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/ValidateAgentContextTask.kt) | Yes — in its own workflow |
| `validateArchitecture` | Module graph, build file hygiene, source set structure, no baselines | [`ValidateArchitectureTask.kt`](../../build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/ValidateArchitectureTask.kt) | Yes — `verify` job |
| `validateTranslations` | Every locale has exactly the base keys and placeholders | [`ValidateTranslationsTask.kt`](../../build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/ValidateTranslationsTask.kt) | Yes — `verify` job |
| Unit tests | Behavior of `core` classes, on the JVM | [`UnitTestPlugin.kt`](../../build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/UnitTestPlugin.kt) | Yes — `testDebugUnitTest` |
| LeakCanary | Activity/Fragment/ViewModel leaks at runtime | `debugImplementation` in [`app/build.gradle.kts`](../../app/build.gradle.kts) — phone app only | No — advisory, debug builds only |

There is no instrumentation test suite. See
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

**Spotless does not report unused imports**; Detekt's `UnusedImport` does. The
[`spotless-fix`](../../.claude/skills/spotless-fix/SKILL.md) skill covers the fix loop for
violations that `spotlessApply` can't resolve on its own.

## Detekt

Detekt runs with `buildUponDefaultConfig`, the repository config at
[`.detekt/detekt.yml`](../../.detekt/detekt.yml), and — the part people trip over —
`failOnSeverity = Warning`. A Detekt *warning* fails the build, so there is no accumulating backlog
of "known" findings.

`build-logic` is analysed too, via `:build-logic:convention:detektMain` and
`:build-logic:detekt-rules:detektMain`; a convention plugin is production code here.

On top of the built-in rules, every module loads the custom `apkanalyzer` rule set from
[`build-logic/detekt-rules`](../../build-logic/detekt-rules/AGENTS.md). It turns the conventions in
`AGENTS.md` into build failures: the ViewModel contract, no comments, preview presence, scoped
Material3 and icons, NavKey shape, and more. [Guardrails](guardrails.md) lists every check, why it
exists, and the candidates that were rejected or deferred. Suppressing a guardrail is itself a
finding (`ForbiddenSuppress`).

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

The only suppression is `MissingTranslation` in [`app/build.gradle.kts`](../../app/build.gradle.kts)
(the `tv` app module sets none), because locales ship incrementally — a string is added in English first and translated in a follow-up
(see the [`translate-strings`](../../.claude/skills/translate-strings/SKILL.md) skill).

## LeakCanary

LeakCanary is a `debugImplementation` dependency, so it exists in debug builds only and never
touches a release APK. It is advisory: no build fails because of it. Watch its notifications while
exercising a change that holds a `Context`, a `ViewModel`, or a long-lived callback — the analysis
screens keep hold of large parsed structures, so a leaked screen is not a cheap leak.

## What no gate catches

What a green build proves nothing about:

* **Layout correctness.** A clean compile says nothing about a Compose screen. Text that wraps to
  three lines in a fixed-width column, or an item sheet that never opens, passes every gate. For any
  visual change, run it on a device — the [`run-app`](../../.claude/skills/run-app/SKILL.md) and
  [`navigate-app-adb`](../../.claude/skills/navigate-app-adb/SKILL.md) skills automate the loop.
* **Semantic conventions.** Two indicators sharing one predicate, unique `LazyColumn` keys, and
  user-facing copy. See the rejected entries in [Guardrails](guardrails.md).

## Running the gates

```bash
./gradlew spotlessApply                                   # required before every commit
./gradlew :feature:apps:phone:compileDebugKotlin           # fast single-module check while iterating
./gradlew spotlessCheck detektDebug :build-logic:convention:detektMain :build-logic:detekt-rules:detektMain lintDebug testDebugUnitTest :build-logic:detekt-rules:test :app:assembleDebug :tv:assembleDebug validateArchitecture validateTranslations
./gradlew validateAgentContext                            # after touching AGENTS.md or a skill
./gradlew -p build-logic :detekt-rules:test               # after changing a guardrail rule
```

Don't run the full check after every edit — CI runs it on every push. Iterate on a single-module
compile and let CI catch the rest.

## In CI

Every push and PR to `develop` runs `spotlessCheck detektDebug
:build-logic:convention:detektMain :build-logic:detekt-rules:detektMain lintDebug validateArchitecture
validateTranslations --continue` and uploads the merged SARIF to code scanning, with
`testDebugUnitTest :build-logic:detekt-rules:test` in its own parallel job. The jobs, artifacts, and the rest of the pipeline are in
[CI and release](ci-and-release.md).

The [`analyze-ci-failure`](../../.claude/skills/analyze-ci-failure/SKILL.md) skill is the procedure
for turning a red run into a root cause.

## Related

* [Coding standards](coding-standards.md) — the rules behind the findings
* [`docs/app/technical/kotlin-code-quality.md`](../app/technical/kotlin-code-quality.md) — the standing
  code-quality audit
