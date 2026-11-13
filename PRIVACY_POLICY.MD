# Privacy policy

Last updated: November 13, 2026

Apk Analyzer is an open source app built by Martin Styk. This policy explains what the app does with
information on your device and what leaves it. It applies to the Apk Analyzer phone and Android TV
apps.

The source code is public, so you can check every statement here:
https://github.com/MartinStyk/apk-analyzer

## The short version

* Apk Analyzer analyzes apps and APK files on your device. The analysis never leaves your device.
* Apk Analyzer has no server and no account. Martin Styk does not receive your app list, your APK
  files, or the results of any analysis.
* The app sends usage statistics, crash reports, and performance measurements to Google Firebase so
  the developer can fix bugs and see which features people use.
* The app does not sell your data, show ads, or share data for advertising.

## What stays on your device

### App analysis

To show you what an app contains, Apk Analyzer reads information about the apps installed on your
device and about APK files you open with it: names, versions, permissions, components, signing
certificates, features, file sizes, and similar details. It uses the "query all packages"
permission to list your installed apps. This information is processed on your device and is not
sent anywhere by the app.

### App usage statistics

If you grant usage access in Android settings, Apk Analyzer reads when and how long you used your
apps, to show this alongside each app. This data is only read and shown on your device. You can
revoke usage access at any time in Android settings.

### App history

Apk Analyzer keeps a history of the apps installed on your device so it can show what changed
between versions, such as new permissions or a different signing certificate. It records
snapshots of each app's details over time, for example after an app is installed or updated. The history is stored in a
database on your device, together with your device's Android ID, which tells apart history recorded
on different devices.

The history is included in Android backup (see [Android backup](#android-backup)). Clearing the app's
storage or uninstalling the app deletes it from your device.

### On-device AI descriptions

On devices that support it, Apk Analyzer can write a short description of an app using Gemini Nano,
the AI model built into Android. The model runs entirely on your device; the app's details are not
sent to any server to generate the description. Android may download the model to your device the
first time it is used.

### Exports

When you export app details, Apk Analyzer creates a file on your device. The file goes only where
you choose to save or share it.

### AI assistants on your device

On Android versions that support App Functions, an AI assistant on your device, such as Gemini, can
ask Apk Analyzer for information about your installed apps when you ask the assistant a question.
Apk Analyzer answers on your device. What the assistant then does with that answer is covered by the
assistant's own privacy policy.

## What leaves your device

### Android backup

Like most Android apps, Apk Analyzer allows Android's built-in backup. If you have backup turned on,
Android may copy the app's data, including your app history, to your Google account, and restore it
when you set up a new device. Google handles this backup under its own privacy policy, not the
developer. You can turn backup off in Android settings.

### Google Firebase

Apk Analyzer uses three Google Firebase services. Google processes this data on behalf of the
developer under the
[Firebase privacy and security terms](https://firebase.google.com/support/privacy).

**Firebase Analytics.** The app records which screens and features you use, for example that you
opened a section of an app's details or exported a list. These events do not include the names of
the apps you look at. Firebase also collects standard information such as a random app instance ID,
device model, Android version, app version, approximate country, and the time of use.

**Firebase Crashlytics.** When the app crashes or hits an error, it sends a crash report. A report
contains technical details about the error, your device model, Android version, and app version, and
a short log of what the app did just before the error. That log can include the package names of
apps on your device, for example the app the error happened with.

**Firebase Performance Monitoring.** The app measures how long things take, such as app startup
and loading screens, and sends these timings with your device model, Android version, and app
version.

The developer uses this data only to find and fix bugs, improve performance, and decide what to
work on next.

## Purchases

Apk Analyzer will offer an optional one-time Pro purchase through Google Play. Payments are handled
entirely by Google Play; the app never sees your payment details. This policy will be updated before
purchases become available.

## Children

Apk Analyzer is not directed at children under 13, and the developer does not knowingly collect
personal information from them.

## Changes to this policy

When this policy changes, the new version is published at the same address with a new "Last
updated" date. Changes that affect what data leaves your device are also described in the app's
release notes.

## Contact

Questions about this policy or about your data: open an issue at
https://github.com/MartinStyk/apk-analyzer/issues.
