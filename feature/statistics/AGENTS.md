# feature:statistics Module

## Purpose
Bottom-navigation top-level destination intended for device/app statistics (tab label "Stats"). **Status: stub/placeholder — not yet implemented.** Currently renders only a solid red `Box`, no state, no ViewModel, no logic. Structurally identical to `feature:permissions` — do not assume real behavior exists here.

## Sub-modules
- `api` — Contains `StatisticsNavKey` (top-level destination) and the "Stats" string resource used as the bottom-nav tab label
- `impl` — Placeholder screen only

## Package: `sk.styk.martin.apkanalyzer.feature.statistics.impl`

## API Module Key Types

```kotlin
@Serializable
object StatisticsNavKey : NavKey
```

## Impl Structure

```
navigation/
  StatisticsEntryProvider.kt  - statisticsEntries(): EntryProviderScope<NavKey> extension, registers StatisticsNavKey -> StatisticsScreen()
StatisticsScreen.kt           - @Composable stub: Box(fillMaxSize().background(Color.Red)) {}
```

## When Implementing This Feature

The real feature is expected to consume `core:app-statistics`'s `LocalApplicationStatisticManager` (batch device-wide statistics computation with progress reporting — it's `@WorkerThread` and re-runs from scratch on every install/uninstall, see `core/app-statistics/AGENTS.md` including its known bugs list before trusting computed percentages). Follow the MVVM pattern used by `feature:settings` or `feature:apps` (State/Action/Event + `@HiltViewModel`) rather than inventing a new shape, and model the manager's `Loading`/`Data` status as UI state explicitly since the computation can take a while.

## Dependencies
- `api`: `apkanalyzer.feature.api` plugin, no explicit dependencies
- `impl`: `apkanalyzer.feature.impl` plugin, `api(projects.feature.statistics.api)` only
