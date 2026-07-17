---
name: run-app
description: Use to build, install, and launch ApkAnalyzer on a connected device or emulator, or to verify a code change actually works at runtime rather than just compiling. Triggered by phrases like "run the app", "install on device", "launch the app", "try it on my phone", "deploy to device", "see if this works", "run it".
---

# Skill: Build, Install, and Run ApkAnalyzer on a Device

> Debug builds and any release build the user already has installed share the same
> `applicationId` (`sk.styk.martin.apkanalyzer` — no debug suffix), so installing over an existing
> release can hit a version conflict. Read Step 3 before installing, especially the first time on
> a given device.

## Step 1 — Find `adb`

`adb` is often not on `PATH`. Try it directly first; if that fails, it lives under the Android SDK:

```bash
adb devices -l || "$ANDROID_HOME/platform-tools/adb" devices -l   # macOS/Linux
```

```powershell
adb devices -l 2>$null; if (-not $?) { & "$env:ANDROID_HOME\platform-tools\adb.exe" devices -l }
```

If `ANDROID_HOME` isn't set either, see the `setup-local-tools` skill. Confirm at least one
device/emulator is listed as `device` (not `unauthorized` or `offline`) before continuing. If
multiple devices are attached, target one explicitly with `-s <serial>` on every `adb` command
below.

## Step 2 — Build and install

```bash
./gradlew installDebug
```

This assembles `:app:debug` and installs it via the AGP-managed install task. If it succeeds,
skip to Step 4.

## Step 3 — If install fails with `INSTALL_FAILED_VERSION_DOWNGRADE`

This happens when the device already has a build with a **higher** `versionCode` than the debug
build's `versionCode = 1` (see `app/build.gradle.kts`) — typically a release build (from the Play
Store, a manual release, or a prior CI artifact) already on the device.

1. Check whether `-d` (allow downgrade) is enough — **it only works if the existing install is
   itself debuggable**, so this recovers cleanly with no data loss when the device already has a
   debug build:
   ```bash
   adb install -r -d app/build/outputs/apk/debug/app-debug.apk
   ```
2. If that still fails (the existing install is a non-debuggable release build), the only way to
   get the debug build on is to uninstall first — **and this wipes the app's local data**
   (DataStore prefs: recently-viewed apps, search history, color scheme). **Ask the user before
   doing this** — it's exactly the kind of hard-to-reverse action that needs confirmation, not an
   assumption:
   ```bash
   adb uninstall sk.styk.martin.apkanalyzer
   ./gradlew installDebug
   ```

## Step 4 — Launch and verify it's actually running

Installing is not running. Launch the main activity and confirm the process is alive with no
immediate crash:

```bash
adb shell am start -n sk.styk.martin.apkanalyzer/.ui.ApkAnalyzerActivity
sleep 2
adb shell pidof sk.styk.martin.apkanalyzer   # a PID means it's running; empty means it crashed on launch
```

If `pidof` comes back empty, pull the crash from logcat rather than guessing:

```bash
adb logcat -d -t 200 | grep -A 30 "FATAL EXCEPTION"
```

## Verification

- [ ] `adb devices -l` showed a `device` (not `unauthorized`/`offline`) before installing
- [ ] Install succeeded — either directly, or via the `-r -d` / uninstall path in Step 3, with
      explicit user confirmation before any uninstall
- [ ] App launched via `am start` and `pidof` returned a PID (not empty)
- [ ] For a specific feature/screen change: actually navigated to it in the running app, not just
      confirmed the process didn't crash on the launch screen
