---
name: spotless-fix
description: Use when fixing code formatting errors, ktlint violations, or style issues. Run after generating or modifying Kotlin files. Triggered by phrases like "fix formatting", "spotless", "ktlint error", "formatting violation", "fix code style", "run spotless".
---

# Spotless Fix

## Fix Commands

Fix all modules (run from project root):
```shell
./gradlew spotlessApply
```

Fix a specific module:
```shell
./gradlew :<module>:spotlessApply

# Examples:
./gradlew :app:spotlessApply
./gradlew :core:apps:spotlessApply
./gradlew :core:ui-library:spotlessApply
./gradlew :feature:apps:impl:spotlessApply
./gradlew :feature:app-detail:impl:spotlessApply
```

Check without fixing (see violations only):
```shell
./gradlew spotlessCheck
./gradlew :<module>:spotlessCheck
```

## Spotless Configuration Summary

Configured in `build-logic/convention/src/main/kotlin/sk/styk/martin/apkanalyzer/SpotlessPlugin.kt`:

| Rule | Value |
|------|-------|
| Multiline signatures | Forced when **3+ parameters** |
| Compose CompositionLocal allowlist | Disabled |
| Compose lambda-param-in-effect | Disabled |
| `@Composable` function naming | PascalCase allowed (ignored by ktlint) |
| Custom ruleset | `io.nlopez.compose.rules:ktlint` |

## Common Violations and Manual Fixes

### 1. Multiline function signature (3+ parameters)
```kotlin
// ❌ Wrong
fun myFun(param1: String, param2: Int, param3: Boolean) {}

// ✅ Correct — each param on its own line, trailing comma
fun myFun(
    param1: String,
    param2: Int,
    param3: Boolean,
) {}
```
Same rule applies to: constructor parameters, lambda parameters, function call arguments.

### 2. Wildcard imports
```kotlin
// ❌ Wrong
import kotlinx.coroutines.flow.*

// ✅ Correct — explicit imports only
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
```

### 3. Missing trailing comma in multiline lists
```kotlin
// ❌ Wrong
fun myFun(
    param1: String,
    param2: Int   // <-- missing trailing comma
)

// ✅ Correct
fun myFun(
    param1: String,
    param2: Int,
)
```

### 4. Callback naming (present tense, not past tense)
```kotlin
// ❌ Wrong
onClicked: () -> Unit
onItemSelected: (String) -> Unit
onBackPressed: () -> Unit

// ✅ Correct
onClick: () -> Unit
onSelectItem: (String) -> Unit
onBack: () -> Unit
```

### 5. Modifier parameter position
```kotlin
// ❌ Wrong — Modifier not after required params
@Composable
fun MyComponent(modifier: Modifier = Modifier, label: String) {}

// ✅ Correct — required params first, Modifier after
@Composable
fun MyComponent(
    label: String,
    modifier: Modifier = Modifier,
) {}
```

## After Running spotlessApply

If `spotlessApply` still fails or cannot auto-fix, manually verify:
1. No wildcard imports in any file
2. All 3+ parameter function signatures are multiline with trailing commas
3. All preview functions are `private` and suffixed with `Preview`
4. No `data object` is missing the `data` keyword in sealed hierarchies

