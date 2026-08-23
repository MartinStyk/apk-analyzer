---
name: capture-app-flow-media
description: Use to record screenshots or GIFs of ApkAnalyzer running on a device for the README or product docs. Triggered by phrases like "add screenshots", "record a gif of this flow", "capture app screenshots", "update the README screenshots", "make a demo gif".
---

# Capture App Flow Media

Produces README-ready screenshots or GIFs of real app flows on a connected device, driven headlessly
via `adb` — no human tapping through the phone. Builds on the `run-app` and `navigate-app-adb` skills;
read those first if the app isn't already installed and running.

## 1. Script the flow before touching the device

Don't improvise taps against a live recording — look up the exact on-screen strings first so the
`navigate-app-adb` dump→locate→tap loop finds them on the first try instead of burning device time on
trial and error:

```bash
grep -n 'string name=".*"' feature/<module>/impl/src/main/res/values/strings.xml
```

Write out the flow as a numbered list of screens and taps (starting point → each tap → end state)
before recording anything. A flow that wanders is worse than a shorter, deliberate one — one clear
interaction per recording beats a long tour.

## 2. Find the tools

- `adb`: often not on `PATH` — see the `run-app` skill's Step 1 (`$env:ANDROID_HOME\platform-tools\adb.exe`).
- `ffmpeg`: check `Get-Command ffmpeg`. If missing, install with
  `winget install --id Gyan.FFmpeg -e --accept-package-agreements --accept-source-agreements` — ask
  first, since installing software is a system change. The current shell won't pick up the new `PATH`
  entry; find the binary directly under
  `$env:LOCALAPPDATA\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-*\bin\ffmpeg.exe`
  and call it by full path for the rest of the session.

## 3. Record

`adb shell screenrecord` blocks the `adb` client until it stops, so run it as a background/async shell
call, perform the scripted taps while it records, then let it finish or stop it early:

```bash
adb -s <serial> shell screenrecord --time-limit 15 --bit-rate 8000000 /sdcard/flow.mp4   # run in background
# ... perform the scripted taps/scrolls against the running recording ...
adb -s <serial> pull /sdcard/flow.mp4 <scratchpad>/flow.mp4
adb -s <serial> shell rm /sdcard/flow.mp4
```

- **Trim the idle intro.** The main/apps-list screen should not sit static for more than ~2 seconds
  before the first action — start the recording right before the first tap, or trim the encoded
  output's leading frames (`ffmpeg -ss 00:00:0X`) rather than padding the gif with a dead opening.
- **A flow with many screens doesn't need to fit one recording.** Split it into segments and
  concatenate before converting: list the segment paths in a text file (`file '<path>'` per line) and
  run `ffmpeg -f concat -safe 0 -i list.txt -c copy combined.mp4`. Prioritize each sub-screen being on
  screen long enough to read over cramming everything into one fast take.
- Drive `adb` from PowerShell on Windows, not the Bash tool (Git Bash rewrites `/sdcard/...` paths) —
  see the `navigate-app-adb` skill's Windows gotchas. This doesn't apply to `screenrecord`/`pull`
  themselves (binary files pulled by the `adb` tool, not piped through shell redirection), but does
  apply to any `uiautomator dump` / `screencap` calls used to locate taps along the way.

## 4. Convert to GIF

Two-pass palette generation gives much better quality than a naive single-pass conversion at a given
file size:

```bash
ffmpeg -y -i flow.mp4 -vf "fps=12,scale=360:-1:flags=lanczos,palettegen" palette.png
ffmpeg -y -i flow.mp4 -i palette.png -filter_complex "fps=12,scale=360:-1:flags=lanczos[x];[x][1:v]paletteuse" out.gif
```

Target ~360px width, ~12fps. Keep files reasonably small (aim for a few MB), but don't sacrifice
legibility of on-screen text to hit an arbitrary size target — a longer flow covering several screens
legitimately runs larger than a single-interaction one.

## 5. Place files and update the README

- Save finals as `docs/images/<kebab-case-flow-name>.gif` (create the directory if it doesn't exist
  yet — it's assets only, not a module, so it needs no `AGENTS.md`).
- The README has a `## Screenshots` section (with a matching Table of Contents entry) using
  HTML-in-markdown `<img>`/`<table>` markup consistent with the header block's existing style. Update
  it in place rather than reinventing the layout — a 2-per-row `<table>` reads better than a single
  wide row once there are more than 2-3 images.
- Give each image real `alt` text describing the flow, and a short caption underneath.

## 6. Clean up and hand back

- Delete recordings and any files copied onto `/sdcard` for the flow (e.g. an APK copied into
  `Download/` to pick from a file picker) — `adb shell rm`.
- Check the repo root for a stray `window_dump.xml` — a `uiautomator dump` run from the wrong shell
  can leave one behind (see the `navigate-app-adb` skill's Windows gotcha) even when driven correctly
  most of the time; delete it if present.
- Don't commit — leave the new/changed files in the working tree for review, per the root `AGENTS.md`
  commit-authorship rule and the general "don't commit unless asked" convention.
- Don't run `spotlessApply`, `detektDebug`, or other Gradle verification — this is a markdown/asset
  change, not code.

## Delegating the capture work

This is a long, tool-noisy loop (many `uiautomator dump`s, recordings, ffmpeg passes) that produces
little the coordinating conversation needs to keep in its own context beyond the final file list and
README diff. If you're an agent capable of forking or spawning a sub-task for this, that's a good fit
— hand the sub-task the exact scripted flow from step 1, the tool paths from step 2, and steps 3-6
verbatim, and ask it to report back only the final summary (files produced, sizes, deviations from the
script, README confirmation).
