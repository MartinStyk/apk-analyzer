# feature:permissions Module

## Purpose
Bottom-navigation top-level destination intended to show device-wide permission analysis (tab label "Permissions"). **Status: stub/placeholder — not yet implemented.** Currently renders only a solid blue `Box`, no state, no ViewModel, no logic. Do not assume this feature has real behavior — check before referencing it in other work.

## Sub-modules
- `api` — Contains `PermissionsNavKey` (top-level destination) and the "Permissions" string resource used as the bottom-nav tab label
- `impl` — Placeholder screen only

## Package: `sk.styk.martin.apkanalyzer.feature.permissions.impl`

## API Module Key Types

```kotlin
@Serializable
object PermissionsNavKey : NavKey
```
Note: plain `object`, not `data object` (differs from `SettingsNavKey` in `feature:settings`).

## Impl Structure

```
navigation/
  PermissionsEntryProvider.kt  - permissionEntries(): EntryProviderScope<NavKey> extension, registers PermissionsNavKey -> PermissionsScreen()
PermissionsScreen.kt           - @Composable stub: Box(fillMaxSize().background(Color.Blue)) {}
```

Entry function name is `permissionEntries()` — singular "permission", unlike the module/package name "permissions". Takes no `Navigator` parameter since the placeholder screen doesn't navigate anywhere.

## When Implementing This Feature

The real feature is expected to consume `core:app-permissions`'s `DevicePermissionsRepository` (device-wide deduplicated permission list with human-readable labels) — see `core/app-permissions/AGENTS.md`. Follow the MVVM pattern used by `feature:settings` or `feature:apps` (State/Action/Event + `@HiltViewModel`) rather than inventing a new shape.

## Dependencies
- `api`: `apkanalyzer.feature.api` plugin, no explicit dependencies
- `impl`: `apkanalyzer.feature.impl` plugin, `api(projects.feature.permissions.api)` only
