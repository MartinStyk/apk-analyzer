---
name: implement-navigation
description: Use when adding a new screen destination to a feature, wiring navigation between screens, implementing an entry provider, or connecting NavKeys. Triggered by phrases like "navigate to", "add screen to feature", "wire navigation", "entry provider", "add destination", "navigate from X to Y".
---

# Implement Navigation to a Screen

## Scenario A — Add an internal screen within an existing feature

Internal screens are sub-destinations within a feature (not top-level tabs).
Their NavKeys live in `feature/<feature>/impl/navigation/`.

### 1. Create internal NavKey

**`feature/<feature>/impl/src/main/kotlin/.../navigation/<Name>NavKey.kt`**

Without parameters:
```kotlin
package sk.styk.martin.apkanalyzer.feature.<feature>.impl.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal data object <Name>NavKey : NavKey
```

With parameters:
```kotlin
@Serializable
internal data class <Name>NavKey(val id: String) : NavKey
```

### 2. Create the screen composable

**`feature/<feature>/impl/src/main/kotlin/.../<subpackage>/<Name>Screen.kt`**
```kotlin
@Composable
internal fun <Name>Screen(
    onBack: () -> Unit,
) {
    // content
}
```

### 3. Register entry in the feature's entry provider

Open the existing `<Feature>EntryProvider.kt` and add a new `entry<>` block:

```kotlin
entry<<Name>NavKey>(
    metadata = slideFromEndEntryMetadata(),
) {
    <Name>Screen(
        onBack = { navigator.goBack() },
    )
}
```

With NavKey parameters (access via `key` lambda argument):
```kotlin
entry<<Name>NavKey>(
    metadata = slideFromEndEntryMetadata(),
) { key ->
    <Name>Screen(
        id = key.id,
        onBack = { navigator.goBack() },
    )
}
```

### 4. Trigger navigation from source screen

In the source screen's `entry<>` block in the entry provider:
```kotlin
onNavigateTo<Name> = { navigator.navigate(<Name>NavKey) }
// or with parameters:
onNavigateTo<Name> = { id -> navigator.navigate(<Name>NavKey(id)) }
```

---

## Scenario B — Navigate to a different feature's screen

Cross-feature navigation uses the target feature's `api` NavKey.

### 1. Add API dependency in source feature's `build.gradle.kts`

```kotlin
dependencies {
    implementation(projects.feature.<target>.api)
}
```

### 2. Import and use the target NavKey

In the source feature's entry provider:
```kotlin
import sk.styk.martin.apkanalyzer.feature.<target>.api.<Target>NavKey

// Inside the entry block:
onNavigateToTarget = { navigator.navigate(<Target>NavKey) }

// With parameters (real example):
onAppDetails = { packageName ->
    navigator.navigate(AppDetailNavKey(AppDetailInput.InstalledPackage(packageName)))
}
```

---

## Transition Metadata

Import from `sk.styk.martin.apkanalyzer.core.uilibrary.animation.*`

| Metadata | Use For |
|----------|---------|
| `slideFromEndEntryMetadata()` | Detail screens, drill-down sub-screens |
| `bottomEntryMetadata()` | Bottom sheets, filter panels, modal screens |
| *(none)* | Top-level tab destinations |
| Custom `NavDisplay.predictivePopTransitionSpec` | Special overlay transitions (e.g., search screen fade) |

Custom transition example (search overlay):
```kotlin
entry<AppSearchNavKey>(
    metadata = NavDisplay.predictivePopTransitionSpec { _ ->
        EnterTransition.None togetherWith fadeOut(tween(200))
    },
) { ... }
```

---

## Entry Provider Function Signatures

### No navigation needed (simple screen)
```kotlin
fun EntryProviderScope<NavKey>.<feature>Entries() {
    entry<<Feature>NavKey> {
        <Feature>Screen()
    }
}
```

### Back navigation only
```kotlin
fun EntryProviderScope<NavKey>.<feature>Entries(navigator: Navigator) {
    entry<<Feature>NavKey> {
        <Feature>Screen(
            onBack = { navigator.goBack() },
        )
    }
}
```

### Multiple internal screens
```kotlin
fun EntryProviderScope<NavKey>.<feature>Entries(navigator: Navigator) {
    entry<<Feature>NavKey> {
        <Feature>Screen(
            onBack = { navigator.goBack() },
            onNavigateToDetail = { id -> navigator.navigate(<Detail>NavKey(id)) },
        )
    }

    entry<<Detail>NavKey>(
        metadata = slideFromEndEntryMetadata(),
    ) { key ->
        <Detail>Screen(
            id = key.id,
            onBack = { navigator.goBack() },
        )
    }
}
```

---

## Wiring Checklist
- [ ] NavKey created (`@Serializable`, implements `NavKey`)
- [ ] Screen composable created with callback parameters
- [ ] Entry registered in entry provider with correct transition metadata
- [ ] Navigation call added in source entry block
- [ ] For **new top-level features**: entry provider called in `ApkAnalyzerApp.kt`
- [ ] For **cross-feature**: API module dependency added in `build.gradle.kts`

