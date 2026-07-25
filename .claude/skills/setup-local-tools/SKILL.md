---
name: setup-local-tools
description: Use when setting up a new machine for this repo, checking whether required tools (gh, JDK, Android SDK, Firebase CLI) are installed and authenticated. Triggered by phrases like "set up my environment", "what do I need to install", "onboard me on this repo", "check my dev tools", "set up the firebase cli", "set up gh cli", "install the android sdk", "install a jdk".
---

# Skill: Set Up Local Tools for ApkAnalyzer + AI Agents

> The recommended local setup for building this repo and letting an AI coding agent work in it at
> full capability. Keep this list short — don't install things this repo already gets for free.
> There's no MCP server configured here: Claude Code already has Bash access, and both `gh` and
> the Firebase CLI are fully capable command-line tools on their own — an MCP layer would just be
> a second interface to the same thing. Call the CLIs directly instead.

## What you actually need to install

| Tool | Needed for | Check |
|---|---|---|
| `gh` (GitHub CLI), logged in | Inspecting/diagnosing GitHub Actions runs and PRs (`analyze-ci-failure` skill) | `gh auth status` |
| JDK 25 (only if missing — see Step 1) | Gradle toolchain — `jvmToolchain(25)` in `build-logic/convention` | `java -version` |
| Android SDK platform 37 + build-tools (only if missing — see Step 1) | `./gradlew assembleDebug`, running the app on a device/emulator | `ANDROID_HOME` set and `%ANDROID_HOME%\platforms\android-37` (or `$ANDROID_HOME/platforms/android-37`) exists |
| Firebase CLI, logged in (optional) | Only if you need to query Crashlytics/Analytics/App Distribution for the `apkanalyzer` Firebase project from the command line — not needed to build or run the app | `firebase --version` / `firebase login:list` |

Everything else below is either already on the machine or unnecessary — don't pre-install it "just
in case."

## What you likely do NOT need to install

| Thing | Why it's usually already covered |
|---|---|
| A standalone JDK 25 | Two independent paths usually already provide it: (1) Android Studio ships its own bundled JetBrains Runtime, and this project's `.idea/misc.xml` already points at it (`project-jdk-name="jbr-25"`). (2) For pure CLI/agent builds with no Android Studio at all, `gradle/gradle-daemon-jvm.properties` pins `toolchainVersion=25` with foojay Disco API URLs, so a plain `./gradlew ...` auto-downloads a matching JDK the first time it's needed (requires the `org.gradle.toolchains.foojay-resolver-convention` plugin in `settings.gradle.kts`, already applied). **Only install one manually if `java -version` shows nothing/wrong version AND you're not relying on Gradle's auto-provisioning** (e.g. offline, or you want a standalone `java` on `PATH` outside Gradle/Android Studio) — see Step 1 |
| Android SDK cmdline-tools, done manually | If Android Studio is installed (assume yes — this is an Android app), its SDK Manager already provides platform 37 + build-tools + `adb`. **Only install manually if `ANDROID_HOME` is unset or missing platform 37/build-tools** — e.g. a headless machine that will never run Android Studio (a CI runner, or an agent-only box) — see Step 1 |
| Node.js | Nothing in this repo needs it — no `package.json` anywhere, and the Firebase CLI ships as a standalone binary (see Step 2) that doesn't need Node. Only install Node if you have some other reason to |
| An MCP server config (`.mcp.json`) | Deliberately not present. Claude Code can already run `gh ...` / `firebase ...` directly via Bash with the exact same capability an MCP wrapper would expose, with no extra process to keep alive and no session restart needed after config changes |

## Step 1 — JDK 25 and Android SDK (only if Android Studio isn't installed, or the checks fail)

Check first — don't install blindly:

```bash
java -version                        # want: 25.x
echo $ANDROID_HOME                   # macOS/Linux — want: a real path
echo %ANDROID_HOME%                  # Windows cmd
$env:ANDROID_HOME                    # Windows PowerShell
ls "$ANDROID_HOME/platforms"         # want: android-37 present (adjust path syntax per OS)
```

If Android Studio is installed and these all resolve, **skip this step entirely** — open Android
Studio once, let it finish its own SDK setup, and both are already handled. Only install manually
if Android Studio is absent or one of the checks above comes back empty/wrong.

### Windows (PowerShell, winget)

```powershell
winget install EclipseAdoptium.Temurin.25.JDK
# Android SDK: install via Android Studio's SDK Manager if at all possible — it's simpler than
# the cmdline-tools path below. Only use cmdline-tools on a machine that will never run Android
# Studio (e.g. CI, a headless agent box):
#   1. Download cmdline-tools from https://developer.android.com/studio#command-tools
#   2. Unzip to %LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest
#   3. sdkmanager.bat "platform-tools" "platforms;android-37" "build-tools;37.0.0"
[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:LOCALAPPDATA\Android\Sdk", "User")
```

### macOS (Homebrew)

```bash
brew install --cask temurin@25
# Android SDK — prefer Android Studio's SDK Manager; for a headless box:
brew install --cask android-commandlinetools
sdkmanager "platform-tools" "platforms;android-37" "build-tools;37.0.0"
```

### Linux (Debian/Ubuntu — adapt package manager as needed)

```bash
sudo apt update && sudo apt install -y openjdk-25-jdk
# or: sdk install java 25-tem   (via sdkman)

# Android SDK — download cmdline-tools from developer.android.com/studio#command-tools, unzip, then:
sdkmanager "platform-tools" "platforms;android-37" "build-tools;37.0.0"
```

## Step 2 — Install `gh`

### Windows (PowerShell, winget)

```powershell
winget install GitHub.cli
gh auth login
```

### macOS (Homebrew)

```bash
brew install gh && gh auth login
```

### Linux (Debian/Ubuntu — adapt package manager as needed)

```bash
type -p curl >/dev/null && curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg \
  | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" \
  | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null
sudo apt update && sudo apt install -y gh
gh auth login
```

`gh auth login`'s interactive arrow-key menu needs a real TTY — if running through an agent's
non-interactive shell pass-through, use the flag form instead so it only needs one code paste in
the browser:

```bash
gh auth login --hostname github.com --git-protocol https --web
```

## Step 3 — Install the Firebase CLI (optional, standalone binary — no Node needed)

Only do this if you actually need to query the Firebase project from the command line. Use the
**standalone binary** distribution, not `npm install -g firebase-tools` — it needs nothing else on
the machine.

### Windows

```powershell
winget install -e --id Google.FirebaseCLI
```

### macOS / Linux

```bash
curl -sL firebase.tools | bash
```

Then authenticate (refuses to run in a non-interactive shell — run it in your own terminal, not
through an agent's shell pass-through):

```bash
firebase login
```

## Step 4 — Verify

```bash
gh auth status                # logged in
firebase --version             # if installed
firebase login:list            # shows an authenticated account, if installed
```

- [ ] `./gradlew assembleDebug` succeeds from the repo root (proves the JDK/SDK auto-provisioning path works even without a manual install)
- [ ] `adb devices -l` shows at least one connected device/emulator, if you plan to install/run the app

## Firebase config note

Unlike some repos, `app/google-services.json` **is committed** to this repo and required for the
`:app` module's Firebase plugins. It exists immediately after clone — nothing to fetch, don't delete it,
and don't regenerate it via `firebase apps:sdkconfig` unless intentionally repointing the app at a
different Firebase project. The Firebase CLI (Step 3) is unrelated to building the app — it's only
for ad-hoc Crashlytics/Analytics queries.

## Gotchas worth knowing

| Gotcha | Why it happens |
|---|---|
| A tool installed via `winget`/`brew`/`apt` mid-session isn't found (`command not found` / `not recognized`) | The shell process was started before install and cached the old `PATH`. Open a new terminal/session, or on Windows reload it: `$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")` |
| `./gradlew installDebug` fails with `INSTALL_FAILED_VERSION_DOWNGRADE` | The connected device already has a release build with a higher `versionCode` than the debug build's `versionCode = 1` (see `app/build.gradle.kts`). `adb install -r -d` only works if the existing install is itself debuggable; against a release build you must `adb uninstall sk.styk.martin.apkanalyzer` first (this wipes local app data — DataStore prefs, recently-viewed apps, search history) |
