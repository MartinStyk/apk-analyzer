# core:design-system Modules

## Purpose and Boundary

The app's design system, split so the phone and TV apps each get only their own UI toolkit:

| Module | Package | Built on | Used by |
|---|---|---|---|
| `foundation` | `core.designsystem.foundation` | Compose UI only | `phone` and `tv` |
| `phone` | `core.designsystem.phone` | Material3 | `app` and feature `phone` modules |
| `tv` | `core.designsystem.tv` | TV Material (`androidx.tv:tv-material`) | the `tv` app and TV modules |

`foundation` never depends on Material3 or TV Material. `phone` and `tv` never depend on each other.
Both expose `foundation` as an `api` dependency, so consumers never declare it directly.

Phone features must not import `androidx.compose.material3`, and TV code must not import
`androidx.tv.material3`. Wrap a new component in the matching module first. The `app` module is the
only exception, for shell-level `Scaffold` and theme plumbing.

## foundation

* `theme/` - `AppTheme`, the one entry point phone and TV code read colors and type from, plus
  `ApkAnalyzerColorPalette` with its light and dark values and the `ApkAnalyzerTypographyStyles`
  shape. `foundation` defines the shape only; each device theme provides the values through
  `LocalApkAnalyzerColors` and `LocalApkAnalyzerTypography`, because phone and TV read at different
  distances and size text differently. Reading `AppTheme` outside a device theme throws.
* `icons/` - `ApkAnalyzerIcons`; `icons/app/` registers the Coil `ImageLoader` that renders an
  `AppReference` as its package icon. `rememberAppAccentColor` samples that icon with AndroidX
  Palette for screens tinted by the app. `permissionIcon` and `requirementIcon` map Android
  permission and feature names to icons for both apps; they key on names, so they need no domain
  types.
* `res/mipmap-*` - the launcher icon both apps reference as `@mipmap/ic_launcher`.
* `animation/` - Navigation 3 transition metadata (`slideFromEndEntryMetadata`, `bottomEntryMetadata`)
  shared by both apps, so a screen pushed on phone and its TV counterpart move the same way.

## phone

* `theme/` - `ApkAnalyzerTheme`, which provides the phone values for `AppTheme`, plus the Material3
  color schemes, the phone type scale, and shapes.
* `components/` - reusable UI primitives and composed controls.
* `modifier/` and `lazylist/` - shared interaction, layout, and list behavior.

Search `components/` by composable name before adding or calling a component. The notable naming
exception is `InactiveSearchBar`, which lives in `SearchBarInactive.kt`.

* Feature colors and typography come from `AppTheme`; icons come from `ApkAnalyzerIcons`.
* Interactive status `Chip` usage goes through the component's click and long-click overload so the
  shared component owns semantics and its shape-clipped ripple.
* `Chip` is an interactive selector with a full touch target. `Tag` is a compact, non-interactive
  inline fact on a row. Do not use them interchangeably.

## tv

* `theme/` - `ApkAnalyzerTvTheme`, which provides the TV values for `AppTheme`. TV is dark-only by
  product decision, following Google's TV guidance; do not add a light palette or a theme setting.
  `ApkAnalyzerTvTypography` pins sizes about an eighth below TV Material, matching the Google TV home
  screen, plus a `monospace` style for fingerprints and hashes. Change sizes there, not by picking a
  smaller role: components keep semantic roles (a card title is `titleMedium`, a fact value
  `titleMedium` beside a `bodyMedium` label). Screen titles use `headlineSmall` with a `bodyMedium` subtitle.
* `components/` - thin wrappers over TV Material components, styled with the palette. Prefer a TV
  Material component over a hand-built layout. `ScreenSurface` (root background and text color),
  `NavigationDrawer` (TV Material's modal drawer; it reserves the collapsed rail's width so content
  never sits under it),  `FilterChip`, `ConfirmDialog` (focus starts on the confirm button), `Button`,
  `SearchField`, `SidePanel` (an end-aligned panel over the dimmed screen, for options that belong
  to the current screen), `SelectableListItem` (a TV `ListItem` with a check, or a caller-supplied icon and supporting
  line, when selected), `InfoCard` (a focusable card with title, icon, free content, and a warning
  tone that tints the whole card), `InfoListItem` (a label/value row with an optional leading icon
  and a chevron when it leads somewhere), `Tag` (a non-focusable fact), `SectionHeader` (the teal heading every TV list and grid uses for its
  sections), `SegmentedBar` and `SegmentedBarLegend` (proportions with a dot legend), `CodeBox`
  (monospace value for hashes and identifiers; `compact` for cards), `AccentBackdrop` (a soft radial glow in the app's color with a fine grain
  that hides banding on dark screens, usually from `rememberAppAccentColor`), `AppCard` (a 16:9 app tile tinted with the app's color: icon, an
  optional value pill such as the sort value, name and package; `AppCardWidth` is its grid minimum), `ScrollablePanel` (a surface for explanations that becomes focusable
  only when its content overflows, then scrolls on Up/Down), `IconBadge` (an icon in a tinted circle, the phone's icon
  treatment; use it instead of a bare icon on cards, rows, and headers), `Icon`, `Text`.
* `SearchField` is built on `BasicTextField` because TV Material has no text field. A text field
  swallows D-pad Up/Down for cursor movement, which traps remote users; it hands those keys back to
  focus navigation and keeps Left/Right for the cursor.
* `SearchField` takes `query`/`onQueryChange`, like the phone's `SearchBarActive`. The ViewModel must
  put the query into `state` synchronously and run only the expensive work (search, mapping) behind
  `flowOn`. If the whole state combine runs on a background dispatcher, the field re-renders stale
  text over the keystroke the keyboard just sent and typed letters disappear.
* `modifier/` - `bleedHorizontally` lets a horizontal row extend past the screen margins so a
  focused item's scale and border are not clipped at the row edges; pair it with matching
  horizontal content padding. `pointerClickable` makes a component work with pointer remotes and air
  mice: hovering draws a quiet outline without moving focus, so the list never scrolls under the
  pointer, and a click focuses the item and runs `onClick`. TV Material's clickables react only to
  the D-pad center key, so every clickable wrapper here applies it with the same callback and shape
  it passes to TV Material; a new clickable wrapper must too.
* `focus/` - `rememberRestorableFocus` gives a screen keyed focus restoration: give each focusable a
  stable key that carries its section, list the keys currently on screen so a vanished item falls
  back to the default, and it retries until something on the screen actually has focus.
  `MinimalFocusScroll` replaces TV's default pivot scrolling with "scroll only as far as needed",
  for screens like the app-detail hub whose header must stay visible while focus is on its buttons.
* TV Material marks some components experimental (`FilterChip` in 1.1.0). Opt in only inside the
  wrapper here, never in a feature, so an API change touches one file.

* Every interactive component must be reachable and operable with a D-pad alone; touch is not
  required on TV.
* Focused cards get a thin accent border and a slight lift, no glow (`FocusStyle.kt`); list rows get
  the same border over a `surfaceVariant` fill rather than an inverted white block. Keep focus quiet:
  reuse these values for a new focusable card or row rather than adding a stronger effect.
* Previews use `@Preview(device = Devices.TV_1080p)` and wrap content in `ApkAnalyzerTvTheme`.

## Shared Component Rules

* Components accept theme-consistent defaults and every component file includes private previews.
* A second copy of a shared shape, control, loading state, or interaction pattern belongs in this
  group rather than in a feature.
