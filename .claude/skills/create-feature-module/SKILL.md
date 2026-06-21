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
}
```

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
}

dependencies {
    api(projects.feature.<name>.api)
    // Add core module dependencies as needed:
    // implementation(projects.core.apps)
    // implementation(libs.kotlinx.collections.immutable)
    // implementation(libs.coil.compose)
}
```

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

## Naming Reference

| Thing | Convention | Example |
|-------|-----------|---------|
| Module directory | kebab-case | `app-detail` |
| Gradle accessor | camelCase | `projects.feature.appDetail.impl` |
| Package segment | concatenated lowercase | `feature.appdetail.impl` |
| NavKey class | PascalCase + `NavKey` | `AppDetailNavKey` |
| Entry provider function | camelCase + `Entries` | `appDetailEntries` |

