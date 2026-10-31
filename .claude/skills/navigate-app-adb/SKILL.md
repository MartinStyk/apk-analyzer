---
name: navigate-app-adb
description: Use to drive ApkAnalyzer's UI on a connected device/emulator with the Android CLI (`android layout`, `android input`) or plain adb — dumping the screen, finding a tappable element by its visible text or content description, and tapping it — to inspect or verify a change without a human touching the device. Triggered by phrases like "navigate to X screen", "tap through the app", "check this screen on device", "verify this UI change via adb", "open app detail via adb", "go to the filter screen".
---

# Skill: Navigate ApkAnalyzer on a device

Drives the running app by reading the on-screen UI tree, locating an element, and tapping it — the
same loop repeated for each hop. Use this **after** the app is installed and running (see the
`run-app` skill for that part).

The loop uses the Android CLI (`android`, see the `setup-local-tools` skill). It matches elements by
selector and taps them in one command, with no dump files to pull or coordinates to compute. If
`android` isn't installed, use the [adb fallback](#fallback-plain-adb).

The loop below drives the phone app. On Android TV the dump works the same, but move with the
remote's keys instead of tapping, so the test exercises focus the way a user would:
`adb shell input keyevent DPAD_UP|DPAD_DOWN|DPAD_LEFT|DPAD_RIGHT`, `DPAD_CENTER` to select, and
`BACK`. The
focused node carries `FOCUSED` in its `state`; step until your target has it, then press
`DPAD_CENTER`. The string table at the end of this skill lists phone labels; TV labels live in
`feature/*/tv/src/main/res/values/strings.xml`.

## The loop: look → tap → verify

### 1. Look at the current screen

```bash
android layout --pretty -o layout.json
```

The JSON is a list of windows. Read only the `APPLICATION` window titled `Apk Analyzer` (plus the
`INPUT_METHOD` window when you need the keyboard) — the `SYSTEM` window carries status-bar
notification text from the user's other apps. Compose bottom sheets and dialogs appear in the app
window without a separate step. Each node has `text`, `content-desc`, `interactions`
(`CLICKABLE`, `SCROLLABLE`, `EDITABLE`…), `state` (`SELECTED`, `CHECKED`, `FOCUSED`), `bounds`, and
`center`.

### 2. Tap by selector

Everything on screen is locatable through two standard accessibility attributes — no special app
wiring required:

- **`text=`** — almost everything: chip labels, button text, list rows (an app row shows both its
  app name and package name), toolbar titles.
- **`desc=`** — the handful of icon-only controls that render no visible text at all (every
  `IconButton` and the recently-viewed `Switch` — see the reference below).

```bash
android input tap 'desc=Sort' --match=single
android input tap 'text=/^Apps$/' --match=single
android input tap 'text=/^com\.google\.android\.youtube$/' --match=single
android input tap 'text=Search apps' --wait=5000
```

A plain `text=` value is a **case-insensitive substring**: `text=Apps` also matches "Installed
apps". Use `/^…$/` for an exact match and always pass `--match=single`, so an ambiguous selector
fails with the list of matches instead of tapping whichever came first. The tap lands on the centre
of the matched node, which triggers its clickable ancestor. `--wait=<ms>` waits for the element to
appear after a screen transition.

### 3. Type, go back

```bash
android input text 'you tube' 'focused=true'
android input key back
```

`input text` replaces the field's content and takes real spaces. Tap the field first so it is the
`focused=true` node.

### 4. Verify

Run step 1 again and confirm the expected text or description is present, or grab a screenshot for
a visual check, then use the Read tool on it:

```bash
android screen capture -o screenshot.png
```

## Gotchas

- **The first `android layout` or `input` call per device installs a helper.** It takes over a
  minute and adds "Android CLI Instrumentation Server" to the installed-apps list. Later calls take
  about 2 seconds.
- **A locked device answers but shows nothing.** The app window disappears from `android layout`
  and taps report `No matching elements`. The long first call above is often what lets the screen
  time out. `adb shell dumpsys window | grep mDreamingLockscreen` reports `true` when the keyguard
  is up; a secure lock can't be dismissed from `adb`, so ask the user to unlock and to turn on
  **Developer options → Stay awake** (`svc power stayon usb` doesn't stick on OnePlus).
- **Don't use `android input scroll` on Compose lists.** At the default speed it reports success
  without moving the list; at `--speed=1500` it moves but `--until` gives up long before the end of
  a full app list. Scroll with `adb shell input swipe <x> <y1> <x> <y2> 400` and re-run
  `android layout`, and reach a specific app through Search rather than scrolling. Rows below the
  fold aren't in the dump until you scroll to them.
- **Floating overlays steal taps.** A Messenger chat head or similar bubble sits above the app and
  swallows a tap aimed at what's underneath — which can open someone's private conversation.
  `android input tap` always hits the matched node's centre, so when a bubble covers it, tap the
  left part of the row's `bounds` with `adb shell input tap <x> <y>` instead, and re-check the
  layout after every tap to confirm you are where you expect before tapping again.
- **Matches aren't always unique.** Two rows can share a label (e.g. two apps with the same display
  name) — `--match=single` reports them; narrow with a more specific string on the same row (an
  app's package name is usually unique). Localized strings also change with the device's language.
- **A genuinely icon-only control needs a `contentDescription`, not a workaround.** If you add a
  button with no visible label, give it one via `IconButton`'s `contentDescription` parameter (a
  `stringResource` in the module that owns the screen — never a hardcoded literal) or, for a
  component that doesn't expose the parameter (e.g. `Switch`, or a `Chip` with an empty label),
  apply `Modifier.semantics { contentDescription = "..." }` directly at the call site. Don't reach
  for position-based taps — they break the moment layout shifts. Only do this for elements that
  render no text anywhere in their subtree; everything else is already findable by the text it
  already shows.

## Fallback: plain adb

Without the Android CLI, dump with `uiautomator`, find the node, and tap the centre of its
`bounds="[x1,y1][x2,y2]"`:

```bash
adb shell uiautomator dump /sdcard/window_dump.xml
adb pull /sdcard/window_dump.xml .
grep -o '<node[^>]*content-desc="Sort"[^>]*>' window_dump.xml
adb shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 ))
adb shell input text "search%squery"   # %s = literal space
adb shell input keyevent 4             # back
```

On Windows run these from PowerShell, not the Bash tool — Git Bash rewrites `/sdcard/...` into a
Windows path (or prefix with `MSYS_NO_PATHCONV=1`). For screenshots, never redirect
`adb exec-out screencap -p` into a file from PowerShell, which corrupts the PNG; use
`adb shell screencap -p /sdcard/screen.png` then `adb pull /sdcard/screen.png`.

## Reference: icon-only controls (no visible text — match by `desc=`)

Everything else is findable directly by its visible text. This is the complete list of what needs
`desc=` matching instead:

| `content-desc` | Element |
|---|---|
| `Back` | Every back button in the app — the shared `Toolbar` component, the custom app-search/permission-filter back arrows, and app-detail's collapsing toolbar. Reused everywhere since only one is ever on screen at a time. |
| `Settings` | Apps list — opens settings |
| `Analyze APK file` | Apps list — opens APK-file analysis |
| `Sort` | Apps list — opens the sort bottom sheet |
| `Delete search: <query>` | App search — per-row delete icon on a search-history entry |
| `Clear date range` | Filter screen — the one chip that renders with an empty label |
| `Recently viewed apps` | Settings — the recently-viewed-apps toggle (bare `Switch`, reuses the same string as its adjacent label) |
