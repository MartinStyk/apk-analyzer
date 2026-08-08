# build-logic Module

## Purpose
Gradle convention plugins that standardize build configuration across all modules. Located in `build-logic/convention/`.

## Package: `sk.styk.martin.apkanalyzer`

## Plugins

| Plugin | File | Applies |
|--------|------|---------|
| `apkanalyzer.agent-context` | `AgentContextPlugin.kt`, `ValidateAgentContextTask.kt` | Root-only `validateAgentContext` task for context pairs, module coverage, skill metadata, links, and duplicate adapters |
| `apkanalyzer.library` | `LibraryPlugin.kt` | `com.android.library` + `apkanalyzer.spotless` + compileSdk/minSdk + Kotlin JVM toolchain |
| `apkanalyzer.application` | `ApplicationPlugin.kt` | `com.android.application` + Google Services + Firebase (Crashlytics, Perf) + release config (minify+shrink) |
| `apkanalyzer.feature.api` | `FeatureApiPlugin.kt` | `apkanalyzer.library` + `kotlin.serialization` + `navigation3-runtime` |
| `apkanalyzer.feature.impl` | `FeatureImplPlugin.kt` | `apkanalyzer.library` + `apkanalyzer.hilt` + `apkanalyzer.compose` + `:core:ui-library` + `:core:navigation` |
| `apkanalyzer.hilt` | `HiltPlugin.kt` | `hilt.android` + `ksp` + hilt-compiler |
| `apkanalyzer.compose` | `ComposePlugin.kt` | `kotlin.compose` + `kotlin.serialization` + Compose BOM + compose bundle + navigation3 bundle |
| `apkanalyzer.spotless` | `SpotlessPlugin.kt` | Spotless + ktlint + compose-rules-ktlint custom ruleset |
| `apkanalyzer.detekt` | `DetektPlugin.kt` | Baseline-free, type-resolved Kotlin static analysis using the shared root configuration |
| `apkanalyzer.sarif-merge` | `SarifMergePlugin.kt` | Root-only `mergeSarifReports` task that combines every module's Detekt and Lint SARIF reports into one file for upload |

## Utility Files (`utils/`)

- `AndroidSdk.kt` - `COMPILE_SDK = 37`, `TARGET_SDK = 37`, `MIN_SDK = 28`
- `DependenciesExtension.kt` - `implementation()`, `api()` helper extensions
- `Kotlin.kt` - `configureKotlin()` (JVM toolchain 25)
- `ProjectExtensions.kt` - `Project.libs` accessor for version catalog

## Spotless Configuration
- ktlint with compose-rules-ktlint custom ruleset
- Multiline function signatures at 3+ parameters
- Compose naming rules (ignores `@Composable` for function naming)
- `compositionlocal-allowlist` and `lambda-param-in-effect` rules disabled
- Module applications format their Kotlin and Kotlin Gradle sources.
- The root application formats root Gradle scripts, `build-logic`, YAML workflows, and `.gitignore` files.
- Project-relative targets keep root and module formatting scopes from overlapping.

## Detekt Configuration
- Shared rules in `config/detekt/detekt.yml`
- Android modules analyze the debug variant with type resolution
- `build-logic` analyzes its main JVM source set
- CI runs every Android module's `detektDebug` task and `build-logic:convention:detektMain`

## SARIF Reporting
- Detekt (`dev.detekt`) and Android Lint (AGP) both emit a per-module `.sarif` report by default —
  no explicit `reports {}`/`lint {}` config is needed to turn SARIF on.
- The root-only `mergeSarifReports` task (`apkanalyzer.sarif-merge`) globs every module's
  `build/reports/detekt/*.sarif` and `build/reports/lint-results-*.sarif` and merges them into
  `build/reports/merged.sarif` using Detekt's own `ReportMergeTask` — no custom parsing script.
- CI runs Detekt and Lint with `--continue` so every module's report is produced even when one
  module fails, then runs `mergeSarifReports` and uploads the single merged file via
  `github/codeql-action/upload-sarif`, which shows results as PR annotations and in the repo's
  Security > Code scanning tab.

## When Adding a New Plugin
1. Create implementation class in `src/main/kotlin/sk/styk/martin/apkanalyzer/`
2. Register in `build.gradle.kts` `gradlePlugin { plugins { register(...) } }`
3. Add to `gradle/libs.versions.toml` under `[plugins]` section
