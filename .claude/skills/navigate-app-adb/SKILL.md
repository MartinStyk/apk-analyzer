---
name: navigate-app-adb
description: Use to drive ApkAnalyzer's UI on a connected device/emulator via adb — dumping the screen, finding a tappable element by its visible text or content description, and tapping it — to inspect or verify a change without a human touching the device. Triggered by phrases like "navigate to X screen", "tap through the app", "check this screen on device", "verify this UI change via adb", "open app detail via adb", "go to the filter screen".
---

# Skill: Navigate ApkAnalyzer via adb

Drives the running app by dumping the on-screen UI tree, locating an element, and tapping its
center — the same loop repeated for each hop. Use this **after** the app is installed and running
(see the `run-app` skill for that part).

## The loop: dump → locate → tap → verify

### 1. Dump the current screen

```bash
adb shell uiautomator dump /sdcard/window_dump.xml
adb pull /sdcard/window_dump.xml .
```

(`uiautomator dump` captures every window, including Compose bottom sheets and dialogs — you don't
need a separate step for those.)

### 2. Find your target node

Everything on screen is locatable through two standard `AccessibilityNodeInfo` attributes that show
up directly in the dump — no special app wiring required for either one:

- **`text=`** — almost everything: chip labels, button text, list rows (an app row shows both its
  app name and package name), toolbar titles.
- **`content-desc=`** — the handful of icon-only controls that render no visible text at all (every
  `IconButton` and the recently-viewed `Switch` — see the reference below).

```bash
grep -o '<node[^>]*text="Filter"[^>]*>' window_dump.xml
grep -o '<node[^>]*content-desc="Sort"[^>]*>' window_dump.xml
```

Tapping the bounds of a `text=` node still triggers its parent's click handler — the tap only needs
to land somewhere inside the clickable ancestor. Pull the `bounds="[x1,y1][x2,y2]"` attribute off
whichever node you matched.

### 3. Tap it

```bash
adb shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 ))
```

For text fields, follow the tap with:

```bash
adb shell input text "search%squery"   # %s = literal space; adb input text can't take real spaces
```

To go back instead of tapping a back button node:

```bash
adb shell input keyevent 4
```

### 4. Verify

Either dump again (step 1) and confirm the expected text/description is present, or grab a
screenshot for a visual check:

```bash
adb exec-out screencap -p > screenshot.png
```

Then use the Read tool on `screenshot.png` to look at it directly.

## Gotchas

- **Off-screen elements don't dump.** A list item below the fold won't appear until you scroll
  (`adb shell input swipe <x1> <y1> <x2> <y2> <durationMs>`) or it's within the initial viewport.
- **Matches aren't always unique.** Two rows can share a label (e.g. two apps with the same display
  name) — if a match gives more than one hit, narrow using a more specific string on the same row
  (an app's package name is usually unique) or the row's `bounds` to disambiguate. Localized strings
  also change with the device's language.
- **A genuinely icon-only control needs a `contentDescription`, not a workaround.** If you add a
  button with no visible label, give it one via `IconButton`'s `contentDescription` parameter (a
  `stringResource` in the module that owns the screen — never a hardcoded literal) or, for a
  component that doesn't expose the parameter (e.g. `Switch`, or a `Chip` with an empty label),
  apply `Modifier.semantics { contentDescription = "..." }` directly at the call site. Don't reach
  for position-based taps — they break the moment layout shifts. Only do this for elements that
  render no text anywhere in their subtree; everything else is already findable by the text it
  already shows.

## Reference: icon-only controls (no visible text — match by `content-desc`)

Everything else is findable directly by its visible text. This is the complete list of what needs
`content-desc` matching instead:

| `content-desc` | Element |
|---|---|
| `Back` | Every back button in the app — the shared `Toolbar` component, the custom app-search/permission-filter back arrows, and app-detail's collapsing toolbar. Reused everywhere since only one is ever on screen at a time. |
| `Settings` | Apps list — opens settings |
| `Analyze APK file` | Apps list — opens APK-file analysis |
| `Sort` | Apps list — opens the sort bottom sheet |
| `Delete search: <query>` | App search — per-row delete icon on a search-history entry |
| `Clear date range` | Filter screen — the one chip that renders with an empty label |
| `Recently viewed apps` | Settings — the recently-viewed-apps toggle (bare `Switch`, reuses the same string as its adjacent label) |
