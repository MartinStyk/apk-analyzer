# GitHub Copilot Instructions

Trust these instructions. Only search the codebase if the information here is incomplete or found to be in error.

## Repository Summary

Android multi-module app (Kotlin 2.4, Jetpack Compose, Hilt, Navigation 3) that inspects installed apps and APK files. ~18 Gradle modules, ~15k LOC Kotlin. No unit tests exist yet.

**Stack:** Kotlin `2.4.10`, AGP `9.3.0`, Gradle `9.6.1` (wrapper), JVM toolchain 21 (JetBrains), compileSdk/targetSdk 37, minSdk 28, Hilt `2.60.1`, Compose BOM `2026.06.01`, Navigation 3 `1.1.4`, Spotless `8.8.0` with ktlint + compose-rules-ktlint.

## Build & Validation Commands

Always run from the repository root. Use `./gradlew` (Linux/macOS) or `.\gradlew.bat` (Windows).

| Purpose | Command | Time | Notes |
|---------|---------|------|-------|
| **Format check (CI)** | `./gradlew spotlessCheck` | ~10s | CI runs this on every PR. Fails if any file has formatting violations. |
| **Auto-fix formatting** | `./gradlew spotlessApply` | ~10s | **Always run this after modifying or creating any `.kt` or `.kts` file.** |
| **Build debug APK** | `./gradlew assembleDebug` | ~30s–2min | Full compilation check. Succeeds on clean repo. |
| **Lint check** | `./gradlew lintDebug` | ~2min | Has a pre-existing error (`MissingDefaultResource` in `drawable-ldrtl`). May fail — do not block on this. |

### Validation sequence after making changes

Always run these two commands in this order after any code change:
```bash
./gradlew spotlessApply
./gradlew spotlessCheck
```
If `spotlessCheck` still fails after `spotlessApply`, manually fix: wildcard imports, missing trailing commas in multiline parameter lists (3+ params), or past-tense callback names (`onClicked` → `onClick`).

To also verify compilation: `./gradlew assembleDebug`

### CI pipeline (.github/workflows/android.yml)

Runs on PRs to `master`: `spotlessCheck` then `lintFreeDebug`. Note: `lintFreeDebug` references a removed product flavor — the actual working lint task is `lintDebug`.

### Critical build requirement

`app/google-services.json` is committed to the repo and required for the `:app` module (Firebase plugins). It exists after clone — do not delete it.

## Project Layout

```
├── app/                         # Application module — Activity, navigation host, Hilt wiring
├── core/
│   ├── common/                  # DispatcherProvider, Logger, ResourcesManager, shared models
│   ├── apps/                    # Repositories: InstalledApps, AppDetail, StorageStats, UsageStats
│   ├── app-permissions/         # DevicePermissionsRepository, PermissionLabelProvider
│   ├── app-statistics/          # LocalApplicationStatisticManager
│   ├── user-preferences/        # RecentlyViewedApps, SearchHistory repositories
│   ├── navigation/              # NavigationState, Navigator (Navigation 3 infrastructure)
│   └── ui-library/              # Theme, wrapped Material3 components, icons, animations
├── feature/
│   ├── apps/{api,impl}/         # App list, search, filter, sort
│   ├── app-detail/{api,impl}/   # App detail screen, general info
│   ├── permissions/{api,impl}/  # Permissions overview
│   ├── statistics/{api,impl}/   # Statistics overview
│   └── settings/{api,impl}/     # Settings screen
├── build-logic/convention/      # Custom Gradle plugins (see below)
├── gradle/libs.versions.toml    # All dependency versions — use this, do not add new libraries
└── settings.gradle.kts          # Module registration
```

### Module dependency rules

- `feature/*/api` — Contains only `@Serializable` NavKey. Depends on nothing.
- `feature/*/impl` — `api(projects.feature.*.api)` + needed `core` modules. Gets `:core:ui-library`, `:core:navigation`, Hilt, Compose via `apkanalyzer.feature.impl` plugin.
- `core/*` — Can depend on other `core` modules. **Never** depends on `feature` modules.
- `app` — Depends on all `feature/*/impl` and all `core` modules.

### Convention plugins (build-logic)

Use these in `build.gradle.kts` — do **not** apply raw Android/Kotlin/Hilt plugins directly:

| Plugin | Use for |
|--------|---------|
| `apkanalyzer.feature.api` | Feature API module (library + serialization + navigation3-runtime) |
| `apkanalyzer.feature.impl` | Feature impl module (library + hilt + compose + ui-library + navigation) |
| `apkanalyzer.library` | Core library module (library + spotless + SDK config) |
| `apkanalyzer.hilt` | Add Hilt + KSP to any module |
| `apkanalyzer.compose` | Add Compose + Navigation 3 to any module |

### Key configuration files

- `gradle/libs.versions.toml` — All versions and dependency coordinates
- `.editorconfig` — ktlint style: `intellij_idea`, max line length 240, Composable naming exempted
- `build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/SpotlessPlugin.kt` — Spotless rules: multiline at 3+ params, compose rules enabled

## Coding Conventions (Enforced by Spotless)

- **No wildcard imports.** Always import specific classes.
- **Multiline function signatures** when 3+ parameters. Each parameter on its own line with trailing comma.
- **Callback naming:** present tense (`onClick`, `onBack`). Never past tense (`onClicked`).
- **`data object`** for sealed interface singleton members, not plain `object`.
- **`@Immutable`** on State data classes. **`ImmutableList`** from `kotlinx.collections.immutable` for list properties.
- **No `androidx.compose.material3` imports in feature modules.** Use wrapped components from `core:ui-library`.
- Theme access: `AppTheme.colors`, `AppTheme.typography`. Icons: `ApkAnalyzerIcons`.
- Every `@Composable` file must include `@Preview` functions (private, suffixed `Preview`, wrapped in `ApkAnalyzerTheme`).
- Use `Logger` from `core:common` — not raw Timber. Pattern: `Logger.d("Tag", "message")`.

### Package naming

Root: `sk.styk.martin.apkanalyzer`. Module namespaces remove hyphens: `core/app-permissions` → `core.apppermissions`, `core/ui-library` → `core.uilibrary`.

## Mandatory Skill Loading

**You MUST read the skill file listed below BEFORE starting any of these tasks.** Do not begin implementation until you have read the full contents of the relevant skill file. Skills contain authoritative step-by-step instructions, file templates, naming rules, and checklists specific to this codebase. Skipping them will produce incorrect code.

| Task | You MUST read this file first |
|------|-------------------------------|
| Create a new feature module (api + impl) | `.claude/skills/create-feature-module/SKILL.md` |
| Create a new core/shared library module | `.claude/skills/create-core-module/SKILL.md` |
| Add a reusable Compose UI component to `:core:ui-library` | `.claude/skills/create-compose-component/SKILL.md` |
| Add a new screen, NavKey, or wire navigation | `.claude/skills/implement-navigation/SKILL.md` |
| Fix ktlint/formatting errors, run spotless | `.claude/skills/spotless-fix/SKILL.md` |

