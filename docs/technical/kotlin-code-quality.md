# Kotlin Code Quality

**Status:** Proposed
**Scope:** Formatting, static analysis, compiler diagnostics, and automated enforcement of Kotlin
and Compose conventions

## Decision

Keep Spotless with ktlint as the formatting layer. Add Detekt for semantic Kotlin analysis and
strengthen Kotlin compiler and Android Lint diagnostics.

Replacing Spotless with the standalone ktlint Gradle plugin or ktfmt would only change how code is
formatted. It would not address complexity, unsafe constructs, exception handling, coroutine
misuse, debug code, or repository-specific conventions. Spotless remains useful as the single
formatting entry point and can also cover Gradle scripts and repository text files.

Each tool should have one responsibility:

| Responsibility | Tool |
|---|---|
| Deterministic Kotlin and Gradle formatting | Spotless with ktlint |
| Compose formatting conventions | compose-rules-ktlint |
| Kotlin idioms, complexity, and likely defects | Detekt |
| Android APIs and resources | Android Lint |
| Kotlin compiler diagnostics | Kotlin compiler |

Detekt formatting rules should remain disabled so they do not overlap or disagree with Spotless.

ktlint should be pinned explicitly to the newest compatible release. Pinning does not mean staying
on an old release: it makes the selected version visible, lets ktlint be upgraded independently of
Spotless, and prevents a Spotless upgrade from silently changing formatting behavior. The version
should be updated deliberately whenever a newer compatible ktlint release is available.

The repository should keep the `intellij_idea` ktlint style. `ktlint_official` would standardize
some wrapping and indentation decisions, but it would not make Kotlin more idiomatic or add semantic
analysis. The resulting repository-wide formatting churn has little value while Android Studio's
IntelliJ style is already consistent.

## Audit Snapshot

The following snapshot was taken on 2026-08-08.

### What is already strong

- `spotlessCheck` runs in CI and passes across all 16 Android modules.
- Every Android library receives Spotless through the library convention plugin.
- The app applies Spotless explicitly.
- compose-rules-ktlint is active.
- Signatures with three or more parameters are forced onto multiple lines.
- Android Lint runs in CI.
- Feature modules currently avoid direct Material 3 imports.

### Gaps

- Spotless does not cover root Gradle scripts or `build-logic` sources.
- The ktlint version is implicit in the Spotless version instead of being pinned independently.
- `.editorconfig` selects `intellij_idea` style and allows lines up to 240 characters.
- There is no Detekt configuration.
- Kotlin compiler warnings are not treated as errors.
- Android Lint warnings do not fail CI. Module reports contain 73 warnings in aggregate:
  22 unused resources, 21 typos, 9 plural candidates, and 21 other warnings.
- Repository conventions such as callback naming, required previews, logger usage, and injected
  dispatchers are mostly documented but not automated.

### Size indicators

The repository contains 255 Kotlin files and approximately 18,924 Kotlin lines:

| Indicator | Count |
|---|---:|
| Files over 300 lines | 13 |
| Files over 500 lines | 7 |
| Files over 1,000 lines | 1 |
| Lines over 120 characters | 159 |
| Lines over 160 characters | 12 |
| Lines over 240 characters | 0 |

The current 240-character limit therefore rejects no existing Kotlin line. Large screen files also
show why formatting alone cannot maintain readable Kotlin: ktlint can format a 1,000-line file but
cannot identify that it needs decomposition.

Examples currently outside automated enforcement include:

- `AppDetailScreen.kt` exceeds 1,100 lines.
- `AppsScreen.kt` uses the past-tense callback name `onAppClicked`.
- `FilterScreen.kt` contains an `XXX` error log.
- Multiple files containing Composables do not contain a preview, despite the repository
  convention.

## Implementation Plan

Each step is intended to be a separate pull request. Mechanical formatting changes must not be
combined with Detekt-driven refactoring.

### KQ-01: Make formatting deterministic and complete

**Changes**

- Add the newest compatible ktlint version explicitly to `gradle/libs.versions.toml`.
- Pass that version to both Kotlin and Kotlin Gradle Spotless steps.
- Update ktlint independently when newer compatible releases become available.
- Apply Spotless to root Gradle scripts and `build-logic`.
- Add a lightweight miscellaneous format for trailing whitespace and final newlines in Markdown,
  YAML, properties files, and `.gitignore`.
- Keep the existing formatting style in this step to avoid unrelated source changes.

**Exit criteria**

- `spotlessCheck` covers Android modules, root scripts, and `build-logic`.
- Spotless and ktlint versions can be upgraded independently.
- `spotlessApply` leaves the working tree clean after a second run.

### KQ-02: Enforce a practical line-length limit

**Depends on:** KQ-01

**Changes**

- Keep `ktlint_code_style = intellij_idea`.
- Reduce `max_line_length` from 240 to 160.
- Keep the existing intentional Compose rule exceptions.
- Apply the resulting mechanical formatting in this pull request only.

**Exit criteria**

- No Kotlin line exceeds 160 characters without an explicit, reviewed exception.
- All enabled ktlint and Compose rules pass.
- The pull request contains no behavioral refactoring.

### KQ-03: Add curated Detekt analysis

**Depends on:** KQ-01

**Changes**

- Add Detekt through a convention plugin so every Kotlin module uses the same configuration.
- Cover `build-logic` as well as Android modules.
- Enable default high-confidence potential-bug, exception, coroutine, performance, and style rules.
- Enable initial complexity limits:
  - method length: 60 lines;
  - class length: 600 lines;
  - cyclomatic complexity: 15;
  - nested block depth: 4.
- Keep formatting, KDoc, `MagicNumber`, and aggressive `TooManyFunctions` rules disabled.
- Run high-confidence checks with type resolution through Android variant tasks and the JVM main
  source set.
- Fix high-confidence findings before making the checks blocking.
- Keep complexity findings non-blocking until the affected code is refactored.
- Do not create a baseline. Stage rule severities instead of hiding accepted findings.
- Run every Android module's `detektDebug` task and `build-logic:convention:detektMain` in CI.

**Exit criteria**

- Detekt fails CI for new error-severity findings.
- No formatting rule is enforced by both Detekt and Spotless.
- The repository has no Detekt baseline.

### KQ-04: Pay down legacy complexity

**Depends on:** KQ-03

**Changes**

- Refactor visible complexity findings in small, behavior-preserving pull requests.
- Start with the largest screen files:
  - `AppDetailScreen.kt`;
  - `CertificatesScreen.kt`;
  - `FilterScreen.kt`;
  - `AppsScreen.kt`.
- Extract cohesive UI sections and sample preview data into focused files.
- Promote each complexity rule to blocking after its existing findings are fixed.

**Exit criteria**

- No production Kotlin file exceeds the agreed class or method limits.
- All enabled Detekt rules pass without a baseline.
- Refactoring does not change user-visible behavior.

### KQ-05: Make diagnostics blocking

**Depends on:** KQ-03

**Changes**

- Resolve the existing Android Lint findings.
- Disable only intentionally non-actionable version-update checks.
- Enable Android Lint `warningsAsErrors`.
- Resolve Kotlin compiler warnings.
- Enable Kotlin `allWarningsAsErrors`.
- Centralize both settings in convention plugins rather than configuring individual modules.

**Exit criteria**

- `lintDebug` reports no actionable warnings.
- Kotlin compilation reports no warnings.
- New compiler or Android Lint warnings fail CI.

### KQ-06: Automate repository-specific invariants

**Depends on:** KQ-03

Add custom rules only for conventions that have produced repeated defects. Prefer configuration of
existing Detekt or Android Lint rules before writing custom rules.

Initial candidates:

- reject raw Timber usage outside the logger wrapper;
- reject hardcoded `Dispatchers` outside `DispatcherProvider`;
- reject Material 3 imports in feature modules;
- reject debug tags such as `XXX`;
- enforce present-tense Composable callback names;
- require a preview in files that own previewable Composable content.

Some architectural rules depend on file paths or Gradle module boundaries and may be clearer as
focused architecture checks rather than AST lint rules.

**Exit criteria**

- Every custom rule protects a documented repository convention.
- Each rule has representative positive and negative tests.
- Exceptions are narrow and visible.

## Target CI Gate

After all steps, the main quality gate should be equivalent to:

```shell
./gradlew spotlessCheck detektDebug :build-logic:convention:detektMain lintDebug :app:assembleDebug
```

`spotlessApply` remains the only automatic formatting command. Detekt and Android Lint remain
check-only tools.

## Deferred

- Switching from ktlint to ktfmt. The expected benefit does not justify a repository-wide formatting
  change while semantic analysis is absent.
- Switching from `intellij_idea` to `ktlint_official`. It changes formatting rather than Kotlin
  quality and would create unnecessary churn.
- Enabling every Detekt rule. Broad rule sets produce noise and encourage suppressions.
- Enforcing KDoc coverage. This repository favors self-documenting code and does not require KDoc.
- Adding architecture-test frameworks before existing Detekt and Android Lint extension points have
  been exhausted.
