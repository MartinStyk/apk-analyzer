# Security Policy

## Supported versions

Only the latest release on Google Play is supported. Fixes ship in a new release rather than as
patches to older versions.

## Reporting a vulnerability

**Please do not open a public issue for a security vulnerability.**

Report it privately through
[GitHub's private vulnerability reporting](https://github.com/MartinStyk/AndroidApkAnalyzer/security/advisories/new),
or by email to **martin.styk@gmail.com** with `SECURITY` in the subject.

Please include:

* what the issue is and what an attacker could achieve
* the app version and Android version you reproduced it on
* steps to reproduce, ideally with a sample APK or app that triggers it
* any relevant logs or a crash trace

You can expect an acknowledgement within a few days and an assessment shortly after. Once a fix is
released you'll be credited in the release notes unless you'd rather not be.

## Scope

This app reads and parses untrusted input: arbitrary `.apk` files chosen by the user or handed over
by another app, and metadata from every installed package. The areas most worth scrutiny:

* **APK parsing and manifest extraction** — [`core:apps`](core/apps/AGENTS.md)
* **Temporary APK file handling and cleanup** — [`core:apk-files`](core/apk-files/AGENTS.md)
* **Exported components and the incoming `VIEW`/`SEND` intent surface** — the `app` module
* **Export and share flows**, including `FileProvider` URI exposure
* **The on-device AI path**, where app-derived data is fed into a prompt —
  [`core:ai-insights`](core/ai-insights/AGENTS.md)

Out of scope: reports that require a rooted or already-compromised device, findings in third-party
dependencies without a demonstrated impact on this app, and the two sensitive permissions the app
declares by design (`QUERY_ALL_PACKAGES` and `PACKAGE_USAGE_STATS`) — both are documented in the
[README](README.md#privacy-and-permissions).

## Data handling

App analysis runs on device. The app contains no networking code of its own; network access comes
only from Firebase (Analytics, Crashlytics, Performance) and from ML Kit downloading the on-device
AI model. See [`PRIVACY_POLICY.MD`](PRIVACY_POLICY.MD).
