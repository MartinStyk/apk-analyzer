# tv Module

## Purpose and Boundary

The Android TV application: wiring only, like `app` is for phones. It owns the TV `Application`, the
`LEANBACK_LAUNCHER` Activity, the TV navigation host, and the TV manifest. The package is
`sk.styk.martin.apkanalyzer.tv`.

It ships as its own app bundle under the same application ID as `app`, uploaded to the Play Console
Android TV track. Both bundles take the same `version.name`; the TV `version.code` carries a fixed
offset so the two never collide.

CI assembles the TV debug APK on every push and pull request and keeps it as the `apk-analyzer-tv`
artifact. Every release tag also uploads the TV bundle to the `tv:Beta TV` track; promoting it to
production is a separate choice — see [CI and release](../docs/engineering/ci-and-release.md).

## Dependency Rules

`tv` depends only on shared `core` modules, `core:design-system:tv`, and feature `api` and `tv` modules. It never
depends on `app`, `core:design-system:phone`, Material3, a feature `phone` module, or phone-only core modules
(`core:app-functions`, `core:ai-insights`, `core:apk-files`).

## Wiring

`TvApkAnalyzerApp.kt` is the assembly point for the TV Navigation 3 host and the side menu. Its
top-level destinations reuse the feature `api` NavKeys and tab labels, so a destination has the same
key and name on phone and TV. Each feature registers its TV screens through a `tv<Feature>Entries`
function in its `feature/*/tv/navigation/` package and wires it into the `entryProvider` there.

## Manifest Contracts

* `android.software.leanback` is required, so Play offers this bundle only to TVs.
* `android.hardware.touchscreen` is not required.
* `android.software.leanback.supports_touch` is `true`, telling Play the app works with pointer
  remotes and mice. That promise holds only while every clickable TV component applies
  `pointerClickable` (see [`core/design-system/AGENTS.md`](../core/design-system/AGENTS.md)).
* `QUERY_ALL_PACKAGES` and `PACKAGE_USAGE_STATS` match the phone manifest. Without the first,
  Android hides most installed apps from the list; without the second, the TV usage access screen
  never lists the app.
* The `LEANBACK_LAUNCHER` Activity and `android:banner` are mandatory for the TV home screen.
  `res/drawable-xhdpi/tv_banner.png` (640x360, the 320x180dp banner) is exported from
  `docs/store/graphics/tv_banner_template.html`, the same source as the Play listing banner; change
  the template and re-export both rather than editing the PNG.

`google-services.json` is a copy of the phone app's file; CI's `fetch-google-services` action writes
both. `debug.keystore` matches `app`'s so debug builds share one signature.
