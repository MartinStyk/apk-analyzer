---
name: create-compose-component
description: Use when creating a new reusable Compose UI component that should live in core:ui-library. Triggered by phrases like "add UI component", "create component", "add reusable composable", "new composable component", "create a card", "create a button variant", "add to ui library".
---

# Create Compose UI Component

All reusable components belong in:
`core/ui-library/src/main/kotlin/sk/styk/martin/apkanalyzer/core/uilibrary/components/<Name>.kt`

## Component Template

```kotlin
package sk.styk.martin.apkanalyzer.core.uilibrary.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.uilibrary.theme.AppTheme

@Composable
fun <Name>(
    // 1. Required state parameters (no defaults)
    label: String,
    // 2. Required callback parameters
    onClick: () -> Unit,
    // 3. modifier always here with default
    modifier: Modifier = Modifier,
    // 4. Optional parameters with defaults
    enabled: Boolean = true,
) {
    // Wrap the Material3 component
    // Apply theme colors via AppTheme.colors
    // Apply typography via AppTheme.typography
    // Apply shapes via Shapes.CardShape etc.
}
```

## Preview Requirements

Every component file MUST include `@Preview` functions. Minimum: one light, one dark.

```kotlin
@Preview
@Composable
private fun <Name>DefaultPreview() {
    ApkAnalyzerTheme {
        <Name>(
            label = "Example Label",
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun <Name>DarkPreview() {
    ApkAnalyzerTheme(isDarkTheme = true) {
        <Name>(
            label = "Example Label",
            onClick = {},
        )
    }
}
```

Preview additional states when relevant (selected, disabled, loading, error).

## Rules

### Must Follow
- Wrap `androidx.compose.material3` — feature modules must **not** import material3 directly.
- Use `AppTheme.colors.*` for all colors (never hardcoded hex or `MaterialTheme.colorScheme` in components).
- Use `AppTheme.typography.*` for all text styles.
- Use shapes from `sk.styk.martin.apkanalyzer.core.uilibrary.theme.Shapes`.
- Always include `modifier: Modifier = Modifier` parameter.
- Callback names: present tense (`onClick`, `onValueChange`, `onDismiss`). Never past tense.
- Preview functions: `private`, suffixed `Preview`, wrapped in `ApkAnalyzerTheme`.

### Data Classes for Component Parameters

If the component takes a structured data parameter, annotate with `@Immutable`:
```kotlin
@Immutable
data class <Name>Data(
    val id: String,
    val label: String,
    val isSelected: Boolean = false,
)
```

### List Parameters

Use `ImmutableList` from `kotlinx.collections.immutable`:
```kotlin
@Composable
fun <Name>List(
    items: ImmutableList<<Name>Data>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
)
```

### Non-Data Class Parameters

Annotate with `@Stable` if used as a Composable parameter but is not a data class:
```kotlin
@Stable
class <Name>State(...)
```

## Before Creating a New Component

Read the component inventory in [`core/ui-library/AGENTS.md`](../../../core/ui-library/AGENTS.md)
first — you may be able to extend an existing component instead. That table is the single
maintained list; don't duplicate it here.

If you do add a component, add a row to that table in the same change.

