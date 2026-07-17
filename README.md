# ApkAnalyzer

[![Continuous integration](https://github.com/MartinStyk/AndroidApkAnalyzer/actions/workflows/android.yml/badge.svg)](https://github.com/MartinStyk/AndroidApkAnalyzer/actions/workflows/android.yml)
[![Release](https://github.com/MartinStyk/AndroidApkAnalyzer/actions/workflows/android-publish.yml/badge.svg)](https://github.com/MartinStyk/AndroidApkAnalyzer/actions/workflows/android-publish.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-blue.svg?logo=kotlin)](gradle/libs.versions.toml)

<img src="app/src/main/res/mipmap-xxhdpi/ic_launcher.png" width="72" align="left" alt="Apk Analyzer icon" />

**Detailed reports of the applications on your device — 📱**

Apk Analyzer is the *most downloaded APK analysis app* on Google Play. It inspects installed apps
and `.apk` files straight from device storage — no root required. It's open source and completely
ad-free.

<a href='https://play.google.com/store/apps/details?id=sk.styk.martin.apkanalyzer'><img alt='Get it on Google Play' height="60" src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/></a>

<br clear="left"/>

## What it does

**App reports** — naming and version data, target/minimum Android version, install and update
dates, signing certificate details, requested permissions with descriptions, activities with
launch options, services/broadcast receivers/content providers, required and optional hardware
features, and a full readable export of `AndroidManifest.xml`. Reports are available even for
`.apk` files you haven't installed yet, and app data (including the icon) can be saved to and
shared from device storage.

**Permissions** — which permissions are requested across all apps on your device, which apps
request a given permission, and each permission's protection level.

**Statistics** — Android version and signing-scheme distribution across your installed apps,
average permission/activity counts, and other collection-wide insights.

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin, coroutines + `Flow` exclusively for async work |
| UI | Jetpack Compose only — no XML layouts |
| DI | Hilt |
| Navigation | Navigation 3 (`androidx.navigation3`) |
| Build | Gradle version catalog + custom convention plugins (`build-logic/`) |
| Backend services | Firebase (Analytics, Crashlytics, Performance) |
| Formatting | Spotless (ktlint + compose-rules-ktlint) |

Exact versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml) — that file is the
single source of truth; nothing pins a version elsewhere.

## Architecture

A multi-module Gradle project with one strict dependency direction:

```
app                          → wires everything together (Hilt graph, nav host, Activity)
├── core/*                   → domain & infra: apps, app-permissions, app-statistics,
│                               common, navigation, ui-library, user-preferences
└── feature/<name>/{api,impl}
    ├── api                  → NavKey only, depends on nothing
    └── impl                 → screen + ViewModel, depends on its own api + needed core modules
```

Rules: `feature/*/api` depends on nothing; `feature/*/impl` depends on its own `api` plus whatever
`core/*` modules it needs; `core/*` modules may depend on other `core/*` modules but never on
`feature/*`; `app` depends on everything. Every module — including `app` itself — has its own
`AGENTS.md` documenting its purpose, structure, and key types; start there when working inside a
specific module instead of re-deriving it from scratch.

## Getting started

**Prerequisites:** Android Studio (latest stable). That's genuinely it — this project's Gradle
setup auto-provisions a matching JDK on first build, and Android Studio's own SDK Manager covers
the Android SDK. See [`.claude/skills/setup-local-tools/SKILL.md`](.claude/skills/setup-local-tools/SKILL.md)
if you want the full breakdown (including CLI-only / headless setup, and optional tools like the
Firebase CLI and GitHub CLI).

```bash
git clone https://github.com/MartinStyk/AndroidApkAnalyzer.git
cd AndroidApkAnalyzer
./gradlew assembleDebug   # first build downloads Gradle, the JDK, and all dependencies
```

`app/google-services.json` is already committed (a real Firebase project config, required for the
`:app` module's Firebase plugins to compile) — nothing to fetch or configure before your first
build.

**Common tasks:**

```bash
./gradlew installDebug       # build + install the debug build on a connected device/emulator
./gradlew spotlessApply      # auto-fix formatting — run before every commit
./gradlew spotlessCheck      # formatting check (what CI runs)
./gradlew lintDebug          # Android lint
```

## Contributing

Read [`AGENTS.md`](AGENTS.md) first — it documents module boundaries, navigation/DI/MVVM
conventions, naming rules, and where things belong before you add anything new. This repo is also
set up for AI-assisted development: `.claude/skills/` has task-specific skills (creating a feature
module, adding a UI component, wiring navigation, diagnosing a failed CI run, and more), and every
module carries its own `AGENTS.md`/`CLAUDE.md` for quick orientation.

Run `./gradlew spotlessApply spotlessCheck assembleDebug` before opening a PR.

## License

Licensed under the [GNU General Public License v3.0](LICENSE).
