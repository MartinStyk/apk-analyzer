# feature:app-detail Module

## Purpose

Displays detailed analysis for an installed package or APK file: general information, permissions,
components, certificates, device requirements, manifest, split APKs, and native libraries.

The API module owns the cross-feature navigation input and distinguishes installed packages from APK
files. The phone implementation uses the package
`sk.styk.martin.apkanalyzer.feature.appdetail.phone`; the TV one, which opens installed packages
only, is described under [TV Module](#tv-module).

## Package Map

* The root implementation package owns the hub State/Action/Event/ViewModel and input adapters.
* `components/` owns interaction and loading/error UI shared across detail sections.
* The "Worth knowing" insight policy and the badges live in `core:apps` `insight/`, shared with TV.
* `generalinfo/`, `permissions/`, `appcomponents/`, `certificates/`, `requirements/`, `manifest/`,
  `splitapks/`, and `nativelibraries/` each own one detail destination.
* `navigation/` owns the feature entry provider and internal destination keys.

Each destination keeps its own State/Action/Event/ViewModel set. Runtime navigation inputs use
assisted injection and seed ViewModel state exactly once.

## TV Module

`feature/app-detail/tv` (package `sk.styk.martin.apkanalyzer.feature.appdetail.tv`) presents the same
data as the phone: a hub (header, App info and Play Store, "Worth knowing", and one card per section)
plus General info, Permissions, Components, Certificates, Device requirements, Native libraries,
Split APKs, and the manifest. It never depends on `phone`.

* Every rule and explanation both apps show lives in core: `core:apps` (`insight/`, `signing/`,
  `components/`, `devicefeatures/`, `packaging/`, `installsource/` text files and their strings) and
  `core:app-permissions` `detail/`. Each app keeps only its own screen text. Add a new explanation
  there, not in either feature module, so both apps stay translated and agree with each other.
* The hub is a fixed profile column (icon, name, package, badges, App info and Play Store) beside a
  three-column grid of section cards, with "Worth knowing" below; everything fits one screen. Cards
  use stacked headers and label-value fact lines so they stay readable at grid width.
* The hub and every section sit on an `AccentBackdrop` tinted by the app icon's color. Hub cards show
  data (stats, a protection-level bar, the fingerprint in a `CodeBox`) rather than sentences, and use
  the warning tone only when the card's own predicate flags a problem.
* Every row carries the same icon the phone uses for that thing; the pane repeats it with a status
  chip and puts identifiers and hashes in a `CodeBox`. Empty sections use `SectionEmpty`, which keeps
  the header and backdrop.
* Sections use `components/ListDetailScaffold` (via `InfoSectionList` for plain rows): focusing a row
  shows its explanation in the pane, so OK does nothing unless the row leads to another screen
  (chevron). This replaces the phone's tap-to-explain sheets; TV has no copy action.
* The pane becomes focusable only when its content overflows, so Right enters it and Up/Down scroll.
* Left out on TV: AI summary (no on-device model), APK/icon export (no document picker), copy/share,
  and text search inside sections. Launchable components start on OK (the row shows a play icon instead
  of a chevron), using the same `isLaunchable` predicates from `core:apps` as the phone.
* Empty and retry-less error states take focus themselves so the remote never falls through to the
  side menu.

## Cross-Cutting Interaction Rules

Every detail list row follows **tap = explain, long-press = copy**. If a row has no explanation yet,
add the missing sheet rather than disabling its tap affordance.

Use the shared detail-field and section loading/error components. Do not create private copies in a
sub-screen.

Initial scopes, filters, or focused items belong in assisted constructor state or saveable UI state,
not a `LaunchedEffect` that can reapply after configuration change and overwrite a user's choice.

When a verdict and icon describe the same fact, derive both from one predicate.

## Hub and Insight Policy

The hub presents neutral app facts separately from the deliberately narrow "Worth knowing" policy.
The insight evaluator currently covers debug access, granted high-impact permissions, questionable
signing validity or trust, sideloading, and materially old target SDK levels.

Do not promote raw component exposure, merely requested dangerous permissions, backup, or cleartext
flags into findings without an explicit product rule. Camera and microphone remain contextual facts;
high-impact access is limited to background location, messages, call history, contacts, and calendar.

Badge and filter predicates reuse typed policy from `core:apps`; features must not recreate platform
classification rules.

## Permission Screen Semantics

Narrowing lives in the ViewModel. The UI receives already-filtered, sectioned data.

* Scope is single-choice and appears only when the app declares permissions.
* Protection and grant filters are multi-choice; an empty selection means no filtering.
* Grant state appears only for installed packages.
* A permission the app declares itself is tracked explicitly because that explains many granted
  signature permissions.
* Grant explanations depend on both protection level and grant state. APK-file mode has no grant
  state and falls back to a protection-level explanation.

Permission rows and their detail sheets expose typed domain values and localized explanations, never
raw Android protection integers.

## Component Screen Semantics

One screen handles activities, services, receivers, and providers. Scope selects the type; "All"
sections by type. Exported items sort first.

`isUnprotected` means exported and unguarded, with these rules:

* Launcher activities are excluded for installed packages because their exported state is expected.
* Other activities require known non-launcher status and no permission guard.
* Services and receivers require no permission guard.
* Providers are unguarded when top-level read or write access is open or a path permission opens a
  path.
* APK-file launcher status is unknown, so its technical filter may include the launcher. Do not turn
  that uncertainty into a hub finding.

`isLaunchable` is a different predicate. It means an installed activity or receiver can receive the
requested launch intent. Do not derive it from `isUnprotected`: launcher activities are expected to
be launchable. Services and providers are excluded.

A successful `startActivity` means Android accepted the request, not that the target stayed open.
User-facing confirmation must not claim an app or screen opened.

Component details show complete intent-filter structure and provider path permissions. A manifest
parsing failure remains explicit in the sheet but does not fail the rest of app detail.

## Certificate Screen Semantics

Show current signing certificates first. Show verified rotation history separately,
newest-to-oldest, and identify the original key.

Fingerprints remain visible in SHA-256, SHA-1, then MD5 order. Public-key fingerprints may be
collapsed but use the shared hash component.

Use the domain's current-signer multiplicity and signing-scheme values directly. Do not re-derive
them from displayed certificate counts. Omit signing-scheme UI when analysis returns unknown; never
guess.

## Device Requirements Semantics

Keep required and optional features as the section structure rather than adding a scope filter.
Mark only unavailable requirements. Available and unknown requirements render no success marker.

Missing optional requirements use softer wording because they do not prevent installation.

OpenGL ES support is a version comparison, not a feature-name lookup. When unavailable, show both
required and device versions. Evaluate requirements for installed packages and APK files because
sideloading can bypass store compatibility checks.

## Split APK Semantics

Show a searchable flat list sorted by split kind. Classification uses installed split filename
conventions:

* Recognized configuration qualifiers map to ABI or screen density.
* Other `split_config.<qualifier>` values are treated as language qualifiers.
* Values without that prefix are dynamic feature modules.

Friendly labels are best effort. Unknown qualifiers fall back to their raw value rather than being
guessed into another category.

Exporting only the installed base APK is not a complete package when splits exist. Keep that
limitation explicit after export.

## Native Library Semantics

Group the list by library name, matching the app-level count. Put ABI-specific copies, sizes, and
containing APKs in the detail sheet rather than flattening them into duplicate rows.

Compare each library's shipped ABIs with `Build.SUPPORTED_ABIS` at the feature layer. Mark only
incompatible libraries, following the requirements screen's "only misses are marked" convention.

## Manifest and Export Semantics

Installed manifest viewing targets the base APK. Merged resource lookup can resolve an arbitrary
split, so report additional split manifests rather than presenting the base document as merged.

APK and icon export use system document creation. APK-file mode hides redundant APK export.
Temporary APK ownership must be released on all terminal lifecycle paths.

## Product Reference

[`docs/app/product/features/app-detail.md`](../../docs/app/product/features/app-detail.md) is the approved
design reference for remaining app-detail work.
