# feature:app-detail Module

## Purpose
Displays detailed information about a single app (installed package or APK file). Shows general info, permissions, components, certificates, features, with sub-navigation to detail sections.

## Sub-modules
- `api` - Contains `AppDetailNavKey(detailInput: AppDetailInput)` and `AppDetailInput` sealed interface
- `impl` - Full implementation

## Package: `sk.styk.martin.apkanalyzer.feature.appdetail.impl`

## API Module Key Types

```kotlin
@Serializable
data class AppDetailNavKey(val detailInput: AppDetailInput) : NavKey

@Serializable
sealed interface AppDetailInput {
    @Serializable data class InstalledPackage(val packageName: String) : AppDetailInput
    @Serializable data class ApkFile(
        val apkFilePath: String,
        val lifetime: ApkFileLifetime = ApkFileLifetime.Persistent,
    ) : AppDetailInput
}
```

## Impl Structure

```
navigation/
  AppDetailEntryProvider.kt  - appDetailEntries(navigator)
  GeneralInfoNavKey.kt       - Internal nav key for general info sub-screen
  PermissionsNavKey.kt       - Internal nav key for the permissions sub-screen
  ComponentsNavKey.kt        - Internal nav key for the components sub-screen
  CertificatesNavKey.kt      - Internal nav key for the certificates sub-screen
  RequirementsNavKey.kt      - Internal nav key for the device requirements sub-screen
  ManifestNavKey.kt          - Internal nav key for the readable Android manifest
  SplitApksNavKey.kt         - Internal nav key for the split APK files sub-screen
  NativeLibrariesNavKey.kt   - Internal nav key for the native library files sub-screen
AppDetailScreen.kt           - Main detail screen Composable
AppDetailViewModel.kt        - Uses @HiltViewModel with AssistedFactory for AppDetailInput
AppDetailState.kt            - Loading/Loaded/Error states with full app detail data
AppDetailAction.kt           - User actions (retry, view manifest, export, navigate sections)
AppDetailEvent.kt            - Navigation/system events
AppDetailInputAdapters.kt    - Maps the navigation DTO to shared `AppReference`
components/
  AppDetailBadge.kt          - Badge classification (Sideloaded, DangerousPermissions, Unused, Large, System, etc.)
  AppDetailToolbar.kt        - Collapsing toolbar for the hub
  InfoRowItem.kt             - InfoRow + InfoRowItem + RationaleBottomSheet, shared by every sub-screen
  SectionScaffold.kt         - SectionLoading + SectionError, the Loading/Error branch every sub-screen shares
  DetailField.kt             - The labelled, tap-to-copy field every item bottom sheet is built from.
                               Use it; do not add a private copy — there were three before it was extracted
  SplitApkExportBottomSheet.kt - Persistent explanation after exporting only the base of a split app
insight/
  AppDetailInsight.kt          - Feature-owned "Worth knowing" finding model, plus the `SensitiveAccess` enum
  AppDetailInsightEvaluator.kt - Pure policy evaluator that turns an `AppDetail` into a list of insights
generalinfo/                 - General info sub-screen
permissions/                 - Permissions sub-screen (see below)
appcomponents/               - Components sub-screen (see below). Named `appcomponents`, not
                               `components`, because `components/` already holds shared UI pieces.
certificates/                - Signing certificate detail sub-screen
requirements/                - Device requirements (uses-feature) sub-screen (see below)
manifest/                    - Searchable readable Android manifest for installed and APK inputs
splitapks/                   - Split APK files sub-screen (see below)
nativelibraries/             - Native library files sub-screen (see below)
```

Each sub-screen directory carries its own State/Action/Event/ViewModel/Screen set, same MVI shape
as the hub.

### `permissions/`

```
PermissionsScreen.kt              - Pinned toolbar, collapsing filter header, sectioned list
PermissionDetailBottomSheet.kt    - The item sheet: every raw field for one permission, tap a field to copy
PermissionResources.kt            - Enum -> @StringRes / icon mapping, kept out of the screen,
                                    incl. `grantExplanationRes` (protection level x grant state)
PermissionsViewModel.kt           - Assisted-injected; combines a loaded source with the active narrowing
PermissionsState.kt               - Loading/Error/Loaded plus PermissionItem, PermissionSection,
                                    PermissionScope / GrantState enums, and multi-select filter state
PermissionsAction.kt              - Retry, ChangeQuery, SelectScope, property filter toggles, ClearNarrowing, CopyValue
PermissionsEvent.kt               - ShowCopiedFeedback
PermissionDescriptionProvider.kt  - @Singleton; curated string -> system loadDescription -> declaring package
```

Narrowing (query, scope, property filters) lives in the ViewModel, not the Composable — the screen
receives only the already-filtered sections. Scope is a single-choice chip that opens a bottom sheet
rather than a tab row, and it renders only when the app defines permissions of its own. Protection
level and grant state use multi-choice selector chips whose empty state applies no filter. Grant
pills and the grant-state selector render only in `InstalledPackage` mode.

The sheet explains *why* a permission has its grant state — the answer differs per protection level
(you allowed it / signing key match / system rules / automatic at install), so `grantExplanationRes`
keys on protection level x grant state and replaces the generic protection-level explanation. It
falls back to that generic sentence in `ApkFile` mode, where there is no grant state. A signature
permission is granted exactly when the keys match, so no certificate comparison is needed; the
`Privileged` flag is the one case that softens the wording. `PermissionItem.isSelfDeclared` marks
permissions the analysed app declares itself, which is the common source of granted signature
permissions.

### `appcomponents/`

```
ComponentsScreen.kt               - Pinned toolbar, collapsing filter header, sectioned list
ComponentDetailBottomSheet.kt     - The item sheet, per component type
IntentFiltersScreen.kt            - Searchable filters for one component; each row summarizes one
                                    filter and opens its full structured detail sheet
IntentFiltersViewModel.kt         - Assisted-injected with the app input and component class name
IntentFilterDetailBottomSheet.kt  - Full actions, categories, URI/content rules, and matching metadata
ComponentResources.kt             - Enum -> @StringRes / icon mapping
ComponentsViewModel.kt            - Assisted-injected with the initial scope and filters
ComponentsState.kt                - Loading/Error/Loaded plus ComponentItem, ComponentDetails,
                                    ComponentScope / ComponentFilter / ComponentType / ComponentFlag
ComponentsAction.kt / ComponentsEvent.kt
```

One screen for all four component types; the scope selector carries the type, so the hub's four
component rows deep-link with their scope preselected. Under scope `All` the list is sectioned by
type. Exported items sort first. `isGuarded` folds a provider's read/write permissions, its
`<path-permission>` entries, and the other types' single `permission` into one flag, so
`isUnprotected` (exported and unguarded) means the same thing everywhere. The component sheet shows
every declared intent filter as the requests, links, and content that can reach that component, plus
— for providers — every `<path-permission>` entry with its path, match type explanation, and
per-path read/write permission (`components_detail_section_path_permissions`); a path entry with
neither permission set is what makes an otherwise-guarded provider `isUnprotected`. It shows only a
filter count and links to a searchable full-screen list; filter counts do not appear on component
rows because quantity is not a risk signal. A manifest parsing failure remains explicit in the
component sheet but does not fail the rest of app detail. Path permissions are extracted and drive
the Components screen's own filter now, but exposure still does not feed the hub's "Worth knowing"
card — that needs a deliberate rule, not just accurate raw data.
`isLaunchable` is deliberately *not* `isUnprotected`: it is exported-and-unguarded (which is exactly
"we are allowed to start it") in `InstalledPackage` mode, for activities and receivers only. Launcher
activities are the most launchable thing there is, so reusing `isUnprotected` would hide the run
button precisely where it is most useful. Services are excluded because background-start restrictions
make `startService` fail or no-op from a backgrounded app, and providers have nothing to start.
**A successful `startActivity` only means the intent was accepted** — the target may finish itself
immediately when it needs extras, so the confirmation says a request was sent, never that something
opened.

Launcher activities are excluded from `isUnprotected`: they are exported with no permission guard by
definition, so warning about them is a false positive that devalues the real ones. They are excluded
from the `Unprotected` filter for the same reason, and the sheet explains why being exported is
expected there rather than claiming a permission guards it. APK-file analysis cannot resolve
launcher status, so its technical `Unprotected` filter may include the launcher; this state does not
feed the hub's findings.

**The initial scope and filters are `@Assisted` constructor parameters, not a `LaunchedEffect`.**
They seed the ViewModel's `narrowing` flow exactly once at construction; the ViewModel survives
configuration change, so a rotation cannot re-apply them over a choice the user has since made. The
same trap applies to `PermissionsScreen`'s `focusedPermission`, which is why the open sheet is keyed
by `rememberSaveable` name rather than restored by an effect.

### `certificates/`

The certificate screen shows current signing certificates first. A separate Signing history
section follows only when Android provides a verified rotation chain; previous keys are displayed
newest-to-oldest and the original key is identified explicitly. Certificate fingerprints are
always visible in SHA-256, SHA-1, MD5 order; public-key fingerprints use the same shared `HashBox`
component but remain collapsed until requested. Signing multiplicity and key-rotation data come
from the explicit current and past certificate lists in `AppDetail.signing`.

### `requirements/`

```
RequirementsScreen.kt             - Pinned toolbar, miss summary, required/optional sections
RequirementResources.kt           - Feature name -> @StringRes / icon maps, raw-name fallback
RequirementsViewModel.kt          - Assisted-injected; combines AppDetail with DeviceFeaturesRepository
RequirementsState.kt              - Loading/Error/Loaded plus RequirementSection, RequirementItem
                                    (Hardware / OpenGlEs) and RequirementAvailability
RequirementsAction.kt / RequirementsEvent.kt
```

The required/optional split is the point of the data, so it is the section structure rather than a
filter. **Only misses are marked** — a column of green ticks is noise, so `Available` renders no
marker and a device whose features could not be read yields `Unknown`, which also renders nothing.
Missing optional requirements are worded more softly than missing required ones: missing an optional
feature is by definition fine and only explains why part of the app does nothing.

OpenGL ES is a version comparison, not a set lookup, so its miss names both sides ("Needs 3.1 · this
device has 3.0") and its raw identifier is the hex the manifest's `android:glEsVersion` actually
contains. The check runs in **both** analysis modes: the platform does not enforce `uses-feature` at
install time, so a sideloaded app can sit on a device that cannot satisfy its own requirements.

There is no search and no scope selector. The `Libraries` scope from the design doc needs
`<uses-library>` extraction (roadmap `FR-44`), which does not exist yet.

### `splitapks/`

```
SplitApksScreen.kt              - Pinned toolbar, collapsing search header, flat list sorted by kind
SplitApkDetailBottomSheet.kt    - The item sheet: type, identifier, file name, size, full path
SplitApkResources.kt            - SplitApkKind -> icon/label mapping, plus the ABI/density lookup
                                   tables and the `java.util.Locale`-backed language display name
SplitApksViewModel.kt           - Assisted-injected; combines the loaded split list with the active
                                   search query
SplitApksState.kt               - Loading/Error/Loaded plus search query and item list
SplitApksAction.kt / SplitApksEvent.kt
```

One tap down from General Info's "Split APKs" row (`core:apps`'s `readInstalledSplits()` backs both).
Every installed split/config APK is classified by file-name convention alone: AGP only ever splits a
config APK along ABI, screen density, or language, so a `split_config.<qualifier>` file whose
qualifier isn't a known ABI or density code is a language split by elimination, and anything without
that prefix is a dynamic feature module. Friendly ABI/density names come from a fixed lookup table;
language qualifiers resolve through `java.util.Locale`, falling back to the raw qualifier when it
can't be parsed. This is best-effort labeling for a Storage-adjacent screen, not a verdict — an
unrecognized qualifier degrades to its raw string rather than guessing.

### `nativelibraries/`

```
NativeLibrariesScreen.kt            - Pinned toolbar, collapsing search header, list grouped by
                                       library name
NativeLibraryDetailBottomSheet.kt   - The item sheet: device support, built-for ABIs, total size,
                                       and a per-ABI breakdown (size + which APK it's bundled in)
NativeLibrariesViewModel.kt         - Assisted-injected; groups `AppDetail.nativeLibraries.files`
                                       (one row per (name, abi) pair) into one list item per distinct
                                       library name, then combines with the active search query
NativeLibrariesState.kt             - Loading/Error/Loaded plus `NativeLibraryItem` (grouped) and
                                       `NativeLibraryVariant` (one ABI's copy of that library)
NativeLibrariesAction.kt / NativeLibrariesEvent.kt
```

One tap down from General Info's "Native library files" row (`core:apps`'s `readNativeLibraries()`
backs both — see `core/apps/AGENTS.md`). The grouping mirrors the row it replaces: General Info has
always counted a library once even when the same file ships for multiple processor architectures, so
the full list keeps that grouping and pushes the per-architecture detail (size, which APK it's
bundled in) into the item sheet rather than flattening it into one row per architecture. Device
compatibility is computed the same way `GeneralInfoViewModel`'s app-wide
`isNativeLibraryDeviceIncompatible` is (against `Build.SUPPORTED_ABIS`), just per library instead of
per app — a library whose ABIs don't intersect the device's is tagged **only** on the row that misses,
following the same "only misses are marked" convention as `requirements/`.

## Key Patterns
- Uses **Assisted Injection** (`@HiltViewModel(assistedFactory = ...)`) because the ViewModel requires `AppDetailInput` at creation time.
- `AppDetailState.Loaded` is a large data class with all detail fields (no nested loading).
- Badge computation uses `AppClassificationThresholds` from `core:apps`.
- Sub-screens follow the `GeneralInfoScreen` idiom: **tap = explain, long-press = copy**.
- "Worth knowing" excludes component exposure and merely requested dangerous permissions. Component
  intent filters are available in the component detail sheet, but path-permission evidence is still
  missing, so the hub does not yet interpret exported components. It surfaces debug access, actually
  granted high-impact access, debug or not-yet-valid signing, and targets at least four API levels
  behind the device. Backup and cleartext flags remain neutral facts in General information rather
  than findings.
- Granted high-impact access is deliberately narrower than every dangerous permission: background
  location, messages, call history, contacts, and calendar. Camera and microphone stay in the
  permission preview because they are common and need app-purpose context before they become a
  useful finding. Special-access and service capabilities remain out until their grant state is
  extracted reliably.
- APK export writes the installed base APK through `ACTION_CREATE_DOCUMENT` and explicitly reports
  when split APKs mean the result is not a complete install package. APK-file mode hides this
  redundant action.
- Icon export supports installed packages and APK files at natural resolution through
  `ACTION_CREATE_DOCUMENT`.
- Installed manifest viewing targets the base APK directly. Merged resources can resolve
  `AndroidManifest.xml` from an arbitrary feature split. The screen reports additional installed
  split manifests instead of pretending the base document is a merged manifest.

## Design Doc

[`docs/product/features/app-detail.md`](../../docs/product/features/app-detail.md) is the approved
design for the remaining sub-screens (Components, Certificates, Requirements, hub rework). Read it
before adding one.

## Dependencies
- `core:apk-files` (TemporaryApkManager)
- `core:apps` (AppDetailRepository)
- `core:app-permissions` (PermissionLabelProvider)
- `core:user-preferences` (RecentlyViewedAppsRepository)
- `kotlinx-collections-immutable`
- `coil-compose`
