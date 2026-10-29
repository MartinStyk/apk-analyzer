---
name: create-feature-module
description: Use when creating a new feature module, screen, or feature area — the api and phone modules, plus a tv module when the feature also ships on Android TV. Triggered by phrases like "create feature", "add feature module", "new feature", "create a screen for", "add new screen".
---

# Create Feature Module

Given a feature name `<name>` (e.g., `about`):

## Step 1 — Create API module

**`feature/<name>/api/build.gradle.kts`**
```kotlin
plugins {
    alias(libs.plugins.apkanalyzer.feature.api)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.<name>.api"

    androidResources.enable = false
}
```

Drop the `androidResources.enable` line once this module gets its own `res/` — most commonly in
Step 8, when a top-level destination needs its tab-label string in
`feature/<name>/api/src/main/res/values/strings.xml`.

**`feature/<name>/api/src/main/kotlin/sk/styk/martin/apkanalyzer/feature/<name>/api/<Name>NavKey.kt`**
```kotlin
package sk.styk.martin.apkanalyzer.feature.<name>.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object <Name>NavKey : NavKey
```

If the NavKey needs parameters, use `data class` instead:
```kotlin
@Serializable
data class <Name>NavKey(val id: String) : NavKey
```

## Step 2 — Create phone module

**`feature/<name>/phone/build.gradle.kts`**
```kotlin
plugins {
    alias(libs.plugins.apkanalyzer.feature.phone)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.<name>.phone"

    androidResources.enable = false
}

dependencies {
    api(projects.feature.<name>.api)
    // Add core module dependencies as needed:
    // implementation(projects.core.apps)
    // implementation(libs.coil.compose)
}
```

Drop the `androidResources.enable` line as soon as the screen has real UI copy and gets its own
`res/values/strings.xml` — every user-facing string in this module's Composables must come from
`stringResource`, so this almost always happens by the time Step 4's placeholder is replaced.

## Step 3 — Create entry provider

**`feature/<name>/phone/src/main/kotlin/sk/styk/martin/apkanalyzer/feature/<name>/phone/navigation/<Name>EntryProvider.kt`**

Without navigation (self-contained screen):
```kotlin
package sk.styk.martin.apkanalyzer.feature.<name>.phone.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.feature.<name>.api.<Name>NavKey
import sk.styk.martin.apkanalyzer.feature.<name>.phone.<Name>Screen

fun EntryProviderScope<NavKey>.<name>Entries() {
    entry<<Name>NavKey> {
        <Name>Screen()
    }
}
```

With navigation (back or to other screens):
```kotlin
package sk.styk.martin.apkanalyzer.feature.<name>.phone.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.feature.<name>.api.<Name>NavKey
import sk.styk.martin.apkanalyzer.feature.<name>.phone.<Name>Screen

fun EntryProviderScope<NavKey>.<name>Entries(navigator: Navigator) {
    entry<<Name>NavKey> {
        <Name>Screen(
            onBack = { navigator.goBack() },
        )
    }
}
```

## Step 4 — Create screen composable (placeholder)

**`feature/<name>/phone/src/main/kotlin/sk/styk/martin/apkanalyzer/feature/<name>/phone/<Name>Screen.kt`**
```kotlin
package sk.styk.martin.apkanalyzer.feature.<name>.phone

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.designsystem.phone.components.Text
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme

@Composable
internal fun <Name>Screen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "<Name>")
    }
}

@Preview
@Composable
private fun <Name>ScreenPreview() {
    ApkAnalyzerTheme {
        <Name>Screen()
    }
}
```

## Step 5 — Register in `settings.gradle.kts`

Add inside `include(...)`:
```kotlin
":feature:<name>:api",
":feature:<name>:phone",
```

## Step 6 — Add dependency in `app/build.gradle.kts`

```kotlin
implementation(projects.feature.<name>.phone)
```

## Step 7 — Wire in `app/src/main/kotlin/sk/styk/martin/apkanalyzer/ui/ApkAnalyzerApp.kt`

Add import:
```kotlin
import sk.styk.martin.apkanalyzer.feature.<name>.phone.navigation.<name>Entries
```

Add inside `entryProvider { }` block:
```kotlin
<name>Entries()          // or <name>Entries(navigator) if Navigator is needed
```

## Step 8 — If top-level destination (bottom nav tab)

Add to `app/src/main/kotlin/sk/styk/martin/apkanalyzer/ui/navigation/TopLevelDestinations.kt`:
- Import the NavKey and add a `NavigationBarItem` to `TOP_LEVEL_DESTINATIONS`
- Choose icons from `ApkAnalyzerIcons`
- Add string resource in the api module: `feature/<name>/api/src/main/res/values/strings.xml`

## Step 9 — If the feature also ships on Android TV

Ask whether the feature belongs on TV; not every one does (`feature:settings` has no TV module). A
TV module is a sibling of `phone`, never a dependant of it — logic both need moves to `core`.

**`feature/<name>/tv/build.gradle.kts`**
```kotlin
plugins {
    alias(libs.plugins.apkanalyzer.feature.tv)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.<name>.tv"
}

dependencies {
    api(projects.feature.<name>.api)
    // implementation(projects.core.apps)
}
```

Mirror Steps 3–4 under `feature/<name>/tv/src/main/kotlin/sk/styk/martin/apkanalyzer/feature/<name>/tv/`
with these differences:

* The entry provider is `tv<Name>Entries(navigator: Navigator)` in `tv/navigation/`, and internal
  NavKeys are prefixed `Tv` (`TvBrowseOptionsNavKey`).
* Screens import from `sk.styk.martin.apkanalyzer.core.designsystem.tv.components` and preview with
  `@Preview(device = Devices.TV_1080p)` in `ApkAnalyzerTvTheme { }` from `core.designsystem.tv.theme`.
  Never Material3 or a phone component.
* Every clickable component needs `pointerClickable` and a restorable focus key — see
  [`core/design-system/AGENTS.md`](../../../core/design-system/AGENTS.md).

Then register `":feature:<name>:tv"` in `settings.gradle.kts`, add
`implementation(projects.feature.<name>.tv)` to `tv/build.gradle.kts`, and call `tv<Name>Entries(navigator)`
inside the `entryProvider` in `tv/src/main/kotlin/sk/styk/martin/apkanalyzer/tv/TvApkAnalyzerApp.kt`.
A top-level TV destination also needs its key in `TOP_LEVEL_KEYS` and a `NavigationDrawerItem` in
that file, reusing the tab-label string from the `api` module.

## Step 10 — Create module docs

Every feature carries one `AGENTS.md` at `feature/<name>/AGENTS.md` covering **all** its submodules
— `api`, `phone`, and `tv` if present — not one file per submodule. Follow
`feature/browse/AGENTS.md` or `feature/apps/AGENTS.md`: Purpose, Package Map, a TV Module section
when there is one, then the feature's own rules. Add a one-line `CLAUDE.md` pointer at the same level:

**`feature/<name>/CLAUDE.md`**
```
@AGENTS.md
```

If the feature starts as a placeholder screen (Step 4's stub), say so explicitly in the `AGENTS.md`
purpose line — e.g. "Status: stub/placeholder — not yet implemented" — so a future agent doesn't
assume real behavior exists. Update it once the feature is actually built out.

The root `AGENTS.md` does not list individual modules, so there is nothing to update there. Run
`./gradlew validateAgentContext` to confirm the new module has the AGENTS/CLAUDE pair it requires.

## Naming Reference

| Thing | Convention | Example |
|-------|-----------|---------|
| Module directory | kebab-case | `app-detail` |
| Gradle accessor | camelCase | `projects.feature.appDetail.phone` |
| Package segment | concatenated lowercase | `feature.appdetail.phone` |
| NavKey class | PascalCase + `NavKey` | `AppDetailNavKey` |
| Entry provider function | camelCase + `Entries` | `appDetailEntries` |
| TV entry provider function | `tv` + PascalCase + `Entries` | `tvAppDetailEntries` |
