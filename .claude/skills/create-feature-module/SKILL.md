---
name: create-feature-module
description: Use when creating a new feature module, screen, or feature area. Triggered by phrases like "create feature", "add feature module", "new feature", "create a screen for", "add new screen".
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

    buildFeatures {
        androidResources = false
    }
}
```

Drop the `buildFeatures` block once this module gets its own `res/` — most commonly in Step 8, when
a top-level destination needs its tab-label string in `feature/<name>/api/src/main/res/values/strings.xml`.

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

## Step 2 — Create impl module

**`feature/<name>/impl/build.gradle.kts`**
```kotlin
plugins {
    alias(libs.plugins.apkanalyzer.feature.impl)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.<name>.impl"

    buildFeatures {
        androidResources = false
    }
}

dependencies {
    api(projects.feature.<name>.api)
    // Add core module dependencies as needed:
    // implementation(projects.core.apps)
    // implementation(libs.kotlinx.collections.immutable)
    // implementation(libs.coil.compose)
}
```

Drop the `buildFeatures` block as soon as the screen has real UI copy and gets its own
`res/values/strings.xml` — every user-facing string in this module's Composables must come from
`stringResource`, so this almost always happens by the time Step 4's placeholder is replaced.

## Step 3 — Create entry provider

**`feature/<name>/impl/src/main/kotlin/sk/styk/martin/apkanalyzer/feature/<name>/impl/navigation/<Name>EntryProvider.kt`**

Without navigation (self-contained screen):
```kotlin
package sk.styk.martin.apkanalyzer.feature.<name>.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.feature.<name>.api.<Name>NavKey
import sk.styk.martin.apkanalyzer.feature.<name>.impl.<Name>Screen

fun EntryProviderScope<NavKey>.<name>Entries() {
    entry<<Name>NavKey> {
        <Name>Screen()
    }
}
```

With navigation (back or to other screens):
```kotlin
package sk.styk.martin.apkanalyzer.feature.<name>.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import sk.styk.martin.apkanalyzer.core.navigation.Navigator
import sk.styk.martin.apkanalyzer.feature.<name>.api.<Name>NavKey
import sk.styk.martin.apkanalyzer.feature.<name>.impl.<Name>Screen

fun EntryProviderScope<NavKey>.<name>Entries(navigator: Navigator) {
    entry<<Name>NavKey> {
        <Name>Screen(
            onBack = { navigator.goBack() },
        )
    }
}
```

## Step 4 — Create screen composable (placeholder)

**`feature/<name>/impl/src/main/kotlin/sk/styk/martin/apkanalyzer/feature/<name>/impl/<Name>Screen.kt`**
```kotlin
package sk.styk.martin.apkanalyzer.feature.<name>.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.uilibrary.components.Text
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme

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
":feature:<name>:impl",
```

## Step 6 — Add dependency in `app/build.gradle.kts`

```kotlin
implementation(projects.feature.<name>.impl)
```

## Step 7 — Wire in `app/src/main/kotlin/sk/styk/martin/apkanalyzer/ui/ApkAnalyzerApp.kt`

Add import:
```kotlin
import sk.styk.martin.apkanalyzer.feature.<name>.impl.navigation.<name>Entries
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

## Step 9 — Create module docs

Every feature carries one `AGENTS.md` at `feature/<name>/AGENTS.md` covering **both** its `api`
and `impl` submodules (not two separate files — see `feature/settings/AGENTS.md` or
`feature/app-detail/AGENTS.md` for the exact format: Purpose, Sub-modules, API module key types,
Impl Structure, Key Patterns, Dependencies), plus a one-line `CLAUDE.md` pointer at the same level:

**`feature/<name>/CLAUDE.md`**
```
@AGENTS.md
```

If the feature starts as a placeholder screen (Step 4's stub), say so explicitly in the `AGENTS.md`
purpose line — e.g. "Status: stub/placeholder — not yet implemented" (see
`feature/browse/AGENTS.md`) — so a future agent doesn't assume real behavior exists. Update it
once the feature is actually built out.

The root `AGENTS.md` does not list individual modules, so there is nothing to update there. Run
`./gradlew validateAgentContext` to confirm the new module has the AGENTS/CLAUDE pair it requires.

## Naming Reference

| Thing | Convention | Example |
|-------|-----------|---------|
| Module directory | kebab-case | `app-detail` |
| Gradle accessor | camelCase | `projects.feature.appDetail.impl` |
| Package segment | concatenated lowercase | `feature.appdetail.impl` |
| NavKey class | PascalCase + `NavKey` | `AppDetailNavKey` |
| Entry provider function | camelCase + `Entries` | `appDetailEntries` |

