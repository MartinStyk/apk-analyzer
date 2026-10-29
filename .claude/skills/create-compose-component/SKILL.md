---
name: create-compose-component
description: Use when creating a new reusable Compose UI component that should live in core:design-system:phone or core:design-system:tv. Triggered by phrases like "add UI component", "create component", "add reusable composable", "new composable component", "create a card", "create a button variant", "add to ui library".
---

# Create Compose UI Component

Phone components belong in:
`core/design-system/phone/src/main/kotlin/sk/styk/martin/apkanalyzer/core/designsystem/phone/components/<Name>.kt`

TV components belong in `core/design-system/tv/.../core/designsystem/tv/components/<Name>.kt` — see
[TV components](#tv-components). Anything both apps need without Material (icons, palette,
transitions) belongs in `core:design-system:foundation`. `phone` and `tv` never depend on each other.

## Component Template

```kotlin
package sk.styk.martin.apkanalyzer.core.designsystem.phone.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.ApkAnalyzerTheme
import sk.styk.martin.apkanalyzer.core.designsystem.foundation.theme.AppTheme

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
- Use shapes from `sk.styk.martin.apkanalyzer.core.designsystem.phone.theme.Shapes`.
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

Use a plain read-only `List` — `.compose/stability.conf` makes it stable. Never a `MutableList`:
```kotlin
@Composable
fun <Name>List(
    items: List<<Name>Data>,
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

## TV Components

The template and rules above apply, with these substitutions:

- Wrap `androidx.tv.material3`, not Material3; TV code never imports either directly outside this
  module. Opt in to experimental TV Material APIs only inside the wrapper.
- Preview with `@Preview(device = Devices.TV_1080p)` inside `ApkAnalyzerTvTheme` from
  `core.designsystem.tv.theme`. TV is dark-only — preview one theme.
- A clickable component applies `pointerClickable` (from `core.designsystem.tv.modifier`) with the
  same callback and shape it passes to TV Material, so pointer remotes and air mice work.
- It must be operable with the D-pad alone, and a focusable card or row reuses the focus border and
  lift from `FocusStyle.kt` rather than adding its own effect.

## Before Creating a New Component

Read the component inventory in [`core/design-system/AGENTS.md`](../../../core/design-system/AGENTS.md)
first — you may be able to extend an existing component instead. That table is the single
maintained list; don't duplicate it here.

If you do add a component, add a row to that table in the same change.

