# build-logic Module

## Purpose

`build-logic/convention` owns the Gradle convention plugins used throughout the repository. Android
SDK levels and the Kotlin JVM toolchain are centralized here and must not be repeated in module
build scripts.

## Convention Plugin Contracts

| Plugin | Responsibility |
|---|---|
| `apkanalyzer.agent-context` | Registers the root `validateAgentContext` task. |
| `apkanalyzer.library` | Configures Android libraries, Kotlin, SDK levels, and formatting. |
| `apkanalyzer.application` | Configures the Android app, Google services, Firebase, and release builds. |
| `apkanalyzer.feature.api` | Configures serialization and Navigation 3 for dependency-light feature APIs. |
| `apkanalyzer.feature.impl` | Configures Compose, Hilt, Navigation 3, and shared UI/navigation modules. |
| `apkanalyzer.hilt` | Applies Hilt and KSP compiler configuration. |
| `apkanalyzer.compose` | Applies Compose, serialization, the BOM, and shared Compose/navigation bundles. |
| `apkanalyzer.spotless` | Applies the repository ktlint and Compose formatting rules. |
| `apkanalyzer.detekt` | Applies baseline-free, type-resolved static analysis. |
| `apkanalyzer.sarif-merge` | Merges module Detekt and Lint SARIF reports for CI upload. |
| `apkanalyzer.appfunctions` | Applies KSP and the `androidx.appfunctions` runtime/compiler for App Functions declarations. |

## Non-Obvious Build Behavior

* `AndroidSdk.kt` and `Kotlin.kt` are the canonical SDK and toolchain sources.
* Spotless targets are project-relative so root and module formatting scopes do not overlap.
* Android modules run Detekt against the debug variant; convention code runs against its JVM main
  source set.
* Detekt and Android Lint already emit SARIF. The merge plugin combines those reports; do not add a
  second parser or reporting pipeline.
* `validateAgentContext` enforces module-scoped `AGENTS.md` coverage, adjacent `CLAUDE.md` adapters,
  skill metadata, unique adapters, and valid local Markdown links.
* The release `signingConfig` in `ApplicationPlugin` falls back to the debug keystore/credentials
  when the `signing.storeFile`/`signing.storePassword`/`signing.keyAlias`/`signing.keyPassword`
  Gradle properties aren't set, so a local `assembleRelease`/`bundleRelease` is always a signed,
  installable build rather than failing or producing an unsigned one. CI supplies the real `-P`
  properties, decoding the keystore from the `SIGN_KEY` secret first.
* `ApplicationPlugin` applies `com.github.triplet.play` (Gradle Play Publisher) by string ID with no
  extension configuration — every real invocation (`publishBundle`, `promoteArtifact`) passes its
  track/status/artifact-dir explicitly via CLI flags from the release workflows, so build-script
  defaults would never be read. It reads its service account credentials from the
  `ANDROID_PUBLISHER_CREDENTIALS` environment variable (its own convention, not a Gradle property).
  Its tasks don't need `app/google-services.json` to exist, even though this module also applies
  `com.google.gms.google-services` — that plugin's file check is lazy, not a configuration-time
  requirement.

## Adding a Convention Plugin

Implement the plugin in the convention source set, register it in the convention build, then expose
its alias through `gradle/libs.versions.toml`. Reuse the existing configuration helpers rather than
configuring SDK, Kotlin, Compose, Hilt, formatting, or analysis independently.
