# App Detail — Full Data Presentation

**Roadmap:** [FR-10 … FR-18](../roadmap.md#12-app-detail), [FR-25](../roadmap.md#15-export--share), plus [EX-07](../roadmap.md#18-data-gaps--extraction-that-doesnt-exist-yet) · R0
**Status:** Approved design, not yet implemented
**Scope:** surface everything `AppDetail` holds inside `feature:app-detail`, and finish the icon
export action on the hub. Manifest viewer (`FR-16`) and APK export (`FR-24`) are out — see
[Deferred](#deferred).

## Why

`AppDetail` carries the complete analysis of an app, but the UI renders only a fraction of it.

| Data | Today |
|---|---|
| `AppInfo` (23 fields) | Fully shown on the General Info screen |
| `Permissions` | 3 counts. No list, grant state, protection level, or groups |
| `activities` / `services` / `receivers` / `contentProviders` | Counts only |
| `certificates` | First certificate, 4 of 13 fields |
| `features` | Count only |

Eight of the nine navigation targets in `AppDetailEntryProvider.kt` are
`Logger.d("not yet implemented")`. Only General Info is wired.

## Audience

Someone who knows Android, wants the raw facts, and still wants to be told what a fact means.
The design has to satisfy both halves: never hide data, never dump it.

---

## Design

### Three depths, one interaction idiom

1. **Hub** (`AppDetailScreen`) — each card answers "is there anything worth my attention here?"
   with a verdict and a preview, not a bare count. You decide whether to tap without tapping.
2. **Section screen** — the complete list, made survivable by search, filter chips, and ordering
   that puts signal first. Plain-language name is primary, raw identifier always visible under it.
3. **Item sheet** — a bottom sheet holding every raw field for one item, plus copy. The nerdy
   payload lives here, one tap down, never omitted.

`GeneralInfoScreen` already established the idiom: **tap = explain, long-press = copy**, with a
rationale bottom sheet. Every new screen uses it. This is the single mechanism that keeps the
surface calm while keeping context one tap away.

### Two cross-cutting rules

**APK-file mode degrades explicitly, never silently.** No grant state, no total size, no last-used.
Don't render an empty row — drop the column and, where it matters, say why ("Grant state is only
available for installed apps").

**Plain name first, raw identifier always present.** Never one or the other. The friendly label is
what you scan; the raw string is what you paste into a bug report.

### Search — narrowing, not navigation

Both list screens — Permissions and Components — carry search. It is not optional polish; a
32-permission list is scannable but a 428-activity list is only usable if you can type at it.

**It works differently from search in `feature:apps`, deliberately.** There, search is
*navigation*: the app list is a means to an end, you are trying to reach one app and leave, so
results are exit points and `AppSearchNavKey` is a separate destination. Here, search is
*narrowing*: the list **is** the destination. You type `provider` not to jump somewhere but to look
at the resulting set — how many, which are exported, how they compare — then adjust and look again.

That difference decides the mechanism. A separate search destination strips the scope selector and
the filter chips, so it cannot answer the compound question these screens exist for: *of the things
matching `provider`, which are exported?* Narrowing happens **in place**, on one screen, because
the surrounding context is what makes the narrowed set meaningful.

What does transfer from `AppsScreen` is the **layout machinery, not the navigation model**:
`collapsingHeader` / `collapsingHeaderContainer` from `:core:ui-library/modifier/`. Search and
filters are on screen when you arrive, scroll away while you read, and come back when you scroll
up.

### No tab rows — the scope selector

An earlier revision of this design pinned a tab row above the search field: Requested / Defined on
Permissions, and Activities / Services / Receivers / Providers on Components. That stacked four
layers of chrome — toolbar, tabs, search, chips — and two of them did the same job. **Tabs and
filter chips are both mechanisms for narrowing one set.** Tabs narrow by *type*, chips narrow by
*property*, and putting two different controls for one concept on top of each other is what made
the screen feel wrong.

So there are no tabs. Type becomes a **scope selector: the first chip in the filter row**, an
`OutlinedChip` with an `ArrowDropDown` trailing icon that opens a `BottomSheet` of options. The
design system already has both, and already distinguishes the two shapes — an outlined chip with a
chevron *selects one of several*, a filled `Chip` *toggles on and off*. The distinction the tab row
was making is carried by the component shape instead of by an extra row.

```
  before                          after
┌──────────────────────┐        ┌──────────────────────┐
│ ← Components         │ pinned │ ← Components         │ pinned
│ Act │ Svc │ Rcv │ Pr │ pinned ├──────────────────────┤
├──────────────────────┤        │ ⌕ Filter 578 …       │ collapsing
│ ⌕ Filter 428 …       │ collap │ (Activities ▾) [Exp] │ collapsing
│ [All] [Exported]     │ collap └──────────────────────┘
└──────────────────────┘
  2 rows always on screen         1 row always on screen
```

This also answers the cross-type question the tab row could not. Scope includes **All components**,
so *"what of this app is reachable from outside?"* is `All` + `Exported` — one view, one tap.
Under tabs it took four visits and mental addition.

Structure on both screens:

- **Toolbar pinned**, nothing else.
- **Search field and filter row in the collapsing header**, scrolling together. The scope chip
  leads the row, property chips follow.
- **Steady state while reading: toolbar only.**

Rules shared by both screens:

- **Matches the raw identifier and the friendly label.** Typing `CAMERA`, `camera`, or `Camera` all
  find the camera permission; `HomeActivity` or `features.home` both find the activity.
  Case-insensitive substring, no fuzzy matching.
- **Composes with scope and filters rather than replacing them.** Typing inside the Exported filter
  with scope Services narrows exported services. The query narrows what is already on screen; it
  never escapes to the full set.
- **The query survives scope changes**, so a term carries across component types.
- **Section headers reflect filtered counts**, so a header never claims 23 when 2 rows are visible.
- **Empty results get a real empty state** naming the query with a clear affordance, never a blank
  list.
- **The placeholder says "Filter", not "Search"** — "Filter 428 activities". It narrows in place
  rather than taking you somewhere, and the label should set that expectation. It names the count
  of the current scope, so it changes with the selector.

### Hashes and fingerprints

Follow the pattern the hub's certificate card already establishes: the value sits in a grey
`surfaceVariant` box with `Shapes.CardShape`, under a small uppercase label. Add one thing —
**hash values render in a monospace style**, so digit groups line up and two fingerprints can be
compared by eye. `ApkAnalyzerTypography` has no monospace style today; it gets one, and the hub's
existing fingerprint box adopts it too.

---

## Screens

### Hub (`AppDetailScreen`)

Toolbar, badges and overview card stay as they are. Three changes — two to the cards, one to the
actions row.

**New — "Worth knowing" card, rendered only when it has something to say:**

```
┌──────────────────────────────────────────┐
│ ⚠ Worth knowing                          │
│                                          │
│ ⚠ Signed with a debug certificate      › │
│ ⚠ Targets Android 10 — outdated        › │
│ ⚠ 12 components open to other apps     › │
│ ⚠ 6 dangerous permissions granted      › │
└──────────────────────────────────────────┘
```

Each line deep-links to the relevant section. Most apps produce zero lines and the card does not
render. Attention is spent only where the app is unusual.

**Changed — cards preview instead of counting:**

```
┌──────────────────────────────────────────┐
│ Permissions                            › │
│ 32 requested · 6 dangerous · 4 granted   │
│ 📷 🎤 📍 👥 📁  +1                        │
├──────────────────────────────────────────┤
│ Components                             › │
│ Activities        428                    │
│ Services           57                    │
│ Receivers          89                    │
│ Providers           4                    │
│ ⚠ 12 exported to other apps              │
├──────────────────────────────────────────┤
│ Signing                                › │
│ ✓ Google Inc. · self-signed              │
│ Valid until 12 Sep 2045                  │
│ SHA-256  A1:B2:C3:…                      │
├──────────────────────────────────────────┤
│ Requirements                           › │
│ 9 required · 3 optional                  │
│ 📷 📶 🔵 🎮                                │
└──────────────────────────────────────────┘
```

**Changed — Save icon does something.** The action, the event and the hub button all exist;
`AppDetailEntryProvider` answers it with `Logger.d("Save icon not yet implemented")`. It is finished
here rather than deferred, because it is a few hours of work against infrastructure that is already
in place and it is one of the two actions people came to this app for.

The icon is already loaded and decoded on this screen — `PackageIconFetcher` calls
`loadIcon(packageManager)` and `toBitmap()`. Saving reuses that path at full resolution instead of
the list thumbnail size.

- **Destination is chosen through `ACTION_CREATE_DOCUMENT`.** The user picks where it lands, which
  needs no storage permission on any supported API level. The `WRITE_EXTERNAL_STORAGE`
  (`maxSdkVersion="28"`) entry in the manifest is legacy and is not what makes this work.
- **PNG, at the icon's natural resolution.** Adaptive icons rasterise at their full bitmap size, so
  the output is what the launcher would draw, not a scaled-down copy.
- **Default filename is the package name** — `com.spotify.music.png`. It is unique, it sorts, and it
  says what the file is without being opened.
- **Works in APK-file mode.** The archive's icon resolves once `applicationInfo.sourceDir` and
  `publicSourceDir` are set to the file path — the one step that is easy to miss and produces a
  silent null otherwise.
- **Both outcomes are visible.** Success confirms with the chosen filename; failure says what failed.
  Never a silent no-op, which is what the button does today.

### Permissions

```
┌──────────────────────────────────────────┐
│ ← Permissions                            │  ← pinned, nothing else
├──────────────────────────────────────────┤
│ ⌕ Filter 32 permissions                  │  ┐ collapsing header —
│ (Requested ▾) [Dangerous] [Granted] [Denied] ┘ scrolls away while reading
├──────────────────────────────────────────┤
│ DANGEROUS · 6                            │
│ Needs your explicit approval             │
│                                          │
│ 📷 Camera                      ● Granted │
│    android.permission.CAMERA             │
│                                          │
│ 📍 Precise location            ○ Denied  │
│    ...ACCESS_FINE_LOCATION               │
│                                          │
│ SIGNATURE · 3                            │
│ Only apps signed with the same key       │
│                                          │
│ NORMAL · 23                              │
│ Granted automatically at install         │
└──────────────────────────────────────────┘
```

Ordering by protection level (Dangerous → Signature → Normal) puts signal on top and buries the
boring ones without hiding them. Each section header carries a one-line explanation of what that
protection level *means* — that is the context layer.

**Scope is `Requested` or `Defined`.** The chip renders only when the app defines permissions of
its own, which most do not — so the common case is search plus three property chips, and nothing
announces a distinction that does not exist for this app. Defined permissions are not a section in
the requested list: the property chips (Dangerous / Granted / Denied) are meaningless for a
permission this app declares for *others* to hold, and swapping scope swaps the chip set with it.

Grant pills render only for installed packages. Tap a row → sheet with friendly name, full name,
permission group, protection level with explanation, grant state, and what the permission actually
allows.

### Components — one screen, scoped by type

One screen rather than four, because the interesting question — *what of this app is reachable by
other apps?* — spans all four types. The scope selector carries the type, so the hub's four
component rows deep-link to this screen with their scope preselected, and the hub's "12 exported to
other apps" warning deep-links to `All components` + `Exported`.

```
┌──────────────────────────────────────────┐
│ ← Components                             │  ← pinned, nothing else
├──────────────────────────────────────────┤
│ ⌕ Filter 428 activities                  │  ┐ collapsing header —
│ (Activities ▾) [Exported] [Unprotected]  │  ┘ scrolls away while reading
├──────────────────────────────────────────┤
│ HomeActivity                           ⚠ │
│ com.spotify.music.features.home          │
│ [exported] [no permission]               │
│                                          │
│ HomeDetailActivity                       │
│ com.spotify.music.features.home          │
└──────────────────────────────────────────┘
```

Scope options are **All components**, then Activities, Services, Receivers, Providers. Under `All`
the list is sectioned by type with counts in the headers, so the sweep reads as one list rather
than four pretending to be one.

Three decisions carry the load on 428 items:

- **Exported / Unprotected filters, with an inline warning marker.** "Exported with no permission
  guard" is the one fact here with real consequences. Exported items sort first within a scope.
- **Simple class name bold, package path dimmed below.** You scan `HomeActivity`, not 62 characters
  of namespace.
- **Narrowing over class name and package path**, composing with the active scope and filter rather
  than escaping them — see [search rules](#search--narrowing-not-navigation).

Flag chips appear on a row only when true, so a rare `isolatedProcess` or `externalService` stands
out precisely because it is rare. Tap → sheet with every field for that type (activity: label,
targetActivity, parentName, permission, exported; service: all five flags; provider: authority,
read and write permissions).

Package grouping is **deferred** — see [Deferred](#deferred).

### Certificates

```
┌──────────────────────────────────────────┐
│ ← Signing certificate                    │
├──────────────────────────────────────────┤
│ ⚠ Debug certificate                      │  ← only when trustLevel == Debug
│   Signed with Android's shared debug key,│
│   not a private key. Fine for testing;   │
│   this build was not published by a      │
│   verified developer.                    │
├──────────────────────────────────────────┤
│ SIGNER                                   │
│ Google Inc. · US                         │
│ Self-signed                            ⓘ │
│                                          │
│ VALIDITY                                 │
│ 21 Aug 2008 → 12 Sep 2045   ✓ Valid      │
│                                          │
│ ALGORITHM   SHA256withRSA                │
│ SERIAL      1234567890                   │
├──────────────────────────────────────────┤
│ CERTIFICATE FINGERPRINTS                 │
│                                          │
│ SHA-256                               ⧉  │
│ ┌──────────────────────────────────────┐ │
│ │ A1:B2:C3:D4:E5:F6:A7:B8:C9:D0:E1:F2: │ │  ← grey box, monospace
│ │ A3:B4:C5:D6:E7:F8:A9:B0:C1:D2:E3:F4  │ │
│ └──────────────────────────────────────┘ │
│ SHA-1                                 ⧉  │
│ ┌──────────────────────────────────────┐ │
│ │ 38:91:8A:45:3D:07:19:93:54:F8:B1:9A… │ │
│ └──────────────────────────────────────┘ │
│ MD5                                   ⧉  │
│ ┌──────────────────────────────────────┐ │
│ │ 25:D6:8E:11:9F:4C:2A:70:B3:5E:C8:D1  │ │
│ └──────────────────────────────────────┘ │
├──────────────────────────────────────────┤
│ PUBLIC KEY FINGERPRINTS                  │
│ (same three, same treatment)             │
└──────────────────────────────────────────┘
```

All six hashes are visible by default — no expander, no toggle. This is a dedicated screen with
vertical room to spare, and hiding a fingerprint behind a tap defeats the reason someone opened
this screen. Each is a labelled grey monospace box with its own copy affordance, following the
[hash convention](#hashes-and-fingerprints). SHA-256 leads each group because it is the one people
actually compare.

"Self-signed" carries an ⓘ because it looks alarming and is completely normal on Android. Same for
an expired signing certificate on an already-installed app, which is harmless. Multiple
certificates render as stacked cards.

### Requirements (features)

```
┌──────────────────────────────────────────┐
│ ← Device requirements                    │
├──────────────────────────────────────────┤
│ REQUIRED · 9                             │
│ The app won't install without these      │
│                                          │
│ 📷  Camera                               │
│     android.hardware.camera              │
│ 📶  Wi-Fi                                │
│                                          │
│ OPTIONAL · 3                             │
│ Used when available                      │
│                                          │
│ 🔵  Bluetooth LE                         │
└──────────────────────────────────────────┘
```

The required/optional split is the entire point of the data. Well-known `android.hardware.*` and
`android.software.*` names map to a friendly label and icon; anything unrecognized falls back to
the raw string rather than being dropped.

---

## Implementation steps

Sections are built before the hub rework, because the "Worth knowing" card and the card previews
deep-link into screens that must already exist.

**No speculative foundations step.** Nothing shared is built ahead of a real consumer. A component
is created in the step that first needs it, and a shared abstraction is extracted when the *second*
consumer appears — not predicted at the first. Each step below therefore lists the shared work it
pulls in, and that work is justified by the screen being built in that same step.

### Step 1 — Permissions screen

Shared work this step pulls in:

- Promote `InfoRowItem` + `RationaleBottomSheet` out of `GeneralInfoScreen.kt` into
  `impl/components/`, and repoint `GeneralInfoScreen` at the shared version. This is a genuine
  second consumer, not a prediction — the permission item sheet is the same tap-to-explain /
  long-press-to-copy row.
- A `ScopeSelectorChip` in `impl/components/` — `OutlinedChip` with `ArrowDropDown` opening a
  `BottomSheet` of options. Every piece already exists in `:core:ui-library`; this composes them,
  so it starts local to the feature and only moves to the library if a third screen wants it. Use
  the `create-compose-component` skill if it does.
- Add only the icons this screen needs to `ApkAnalyzerIcons` (permission groups, copy, info).

No `TabRow` is built. An earlier revision of this plan added one to `:core:ui-library`; the
[scope selector](#no-tab-rows--the-scope-selector) replaced it, and the component that replaced it
is assembled from parts the design system already ships.

Screen work:

- Data: extend `Permission` in `:core:apps` with `description` from `PermissionInfo.loadDescription()`
  and `declaringPackage` from `PermissionInfo.packageName`. Both come free —
  `getUsedPermissions` already calls `getPermissionInfo` per permission, so this reads two more
  fields off an object it already has, with no extra IPC.
- Change `UsedPermission.isGranted` from `var` to `val`.
- Description resolution, in strict order:
  1. **Curated string in this module's `strings.xml`**, keyed by permission name, written for the
     well-known permissions. Always wins — the system text is often terse, and this is where the
     app's voice lives.
  2. **`loadDescription()`** from the system, for anything not curated.
  3. **Neither available** → show no description, or, when the permission is declared by another
     app, "Defined by <package>". Never a placeholder that says nothing.
- Protection-level decoding: base level via `PROTECTION_MASK_BASE`, plus the interesting
  `PROTECTION_FLAG_*` flags (appop, privileged, instant) shown in the item sheet.
- `PermissionsNavKey` in `impl/navigation/`, entry wired in `AppDetailEntryProvider`.
- ViewModel / State / Action / Event following the `GeneralInfoViewModel` shape, with the filter
  query in state and the narrowing done in the ViewModel.
- Screen: pinned toolbar only; `SearchBarActive` and the filter row — scope chip first, property
  chips after — inside a `collapsingHeader` block as `AppsScreen` does it; protection-level
  sections, grant pills, item sheet. The scope chip renders only when `permissions.defined` is
  non-empty. Narrowing matches permission name and friendly label — see
  [search rules](#search--narrowing-not-navigation).

By the end of this step the Loading / Error / Loaded scaffold exists in two places (General Info and
Permissions). Leave it duplicated — extract it in Step 2, when a third consumer proves the shape.

### Step 2 — Components screen

Shared work this step pulls in:

- Extract the Loading / Error / Loaded section scaffold into `impl/components/` now that three
  screens share it, and repoint General Info and Permissions at it.
- Promote `ScopeSelectorChip` from Step 1 only if the five-option scope exposes a gap the two-option
  use did not. A second consumer justifies extracting it to `:core:ui-library`; nothing else does.

Screen work:

- `ComponentsNavKey` carrying the initial scope **and** the initial filter, so the four hub rows
  deep-link to their type and the "exported" warning row deep-links to `All` + `Exported`.
- One ViewModel producing all four lists, each mapped to a shared row model (name, package, exported,
  permission, type-specific flags).
- Change the four `Service` flag fields from `var` to `val` while mapping them.
- Screen: pinned toolbar only; `SearchBarActive` and the filter row (scope chip, then Exported /
  Unprotected) inside a `collapsingHeader` block; exported-first ordering. Under scope `All` the
  list is sectioned by component type with counts in the headers. Narrowing matches simple class
  name and package path, composes with the active scope and filter, and survives scope changes —
  see [search rules](#search--narrowing-not-navigation).
- Item sheet per component type with every raw field.

### Step 3 — Certificates screen

- Add a monospace style to `ApkAnalyzerTypography` and a small labelled hash-box composable; adopt
  it in the hub's existing fingerprint box so the two screens match.
- Change `Certificate.startDate` / `endDate` from `java.util.Date` to **`java.time.LocalDate`**.
  These are validity *dates* — always displayed as a date, only ever compared for expiry — so a
  date type models them better than an instant, and `LocalDate` needs no new dependency
  (`kotlinx-datetime` is not in the version catalog and adding it needs a separate decision).
  Convert once in `CertificateExtractorImpl` from `X509Certificate.notBefore` / `notAfter` using
  the system zone, which is what `keytool` and `apksigner` print too.
- `CertificatesNavKey`, ViewModel, state carrying all 13 certificate fields.
- Self-signed detection (issuer equals subject) and expiry evaluation against today.
- Debug-certificate banner, signer / validity / algorithm / serial blocks.
- Certificate and public key fingerprint groups, all three hashes each, fully visible.

### Step 4 — Requirements screen

- `FeaturesNavKey`, ViewModel, required/optional split.
- Curated friendly-name and icon mapping for well-known feature names, raw-string fallback.

### Step 5 — Hub rework

- Card previews: dangerous permission group icons, exported component count, certificate validity
  line and self-signed note, required/optional feature split.
- "Worth knowing" card with its rule set: debug certificate, outdated target SDK, exported
  components without a permission guard, dangerous permissions granted. Each row deep-links.
- Extend `AppDetailState.Loaded` with the derived values the previews need.
- **Save icon**: an icon loader in `:core:apps` returning the full-resolution bitmap for a package
  name or an APK path (setting `sourceDir` / `publicSourceDir` in the archive case), PNG encoding,
  an `ACTION_CREATE_DOCUMENT` launcher wired to `AppDetailEvent.SaveIcon` in
  `AppDetailEntryProvider`, and the success/failure messages. Replaces the
  `Logger.d("not yet implemented")` handler.

### Step 6 — Polish pass

- APK-file mode audit across every new screen: verify nothing renders an empty or misleading row.
- Empty states for each section (an app with zero receivers, zero features, no certificate).
- Preview functions with realistic sample data on every new composable file.

---

## Deferred

- **Package grouping in the components list.** Confirmed out of scope for the first pass.
  Collapsible groups by package prefix would tame 428 items and reveal the app's architecture, but
  search plus the exported filters carry it for now. Revisit after Step 2 ships and the list has
  been used against a real 400-component app.
- **Manifest viewer** (`FR-16`), **Export APK** (`FR-24`) — existing stub actions on the hub, still
  in R0 but out of scope for this doc.

---

## Open questions

- **Repeated loading on APK-file input only.** `AppDetailRepositoryImpl` is `@Singleton` and holds a
  `ConcurrentHashMap<String, AppDetail>` keyed by package name, invalidated by
  `PackageChangesObserver`. Installed packages are parsed once and every section ViewModel after
  that is a map lookup — no problem there. But `apkFilePackageDetails` has **no cache**: each call
  runs `getPackageArchiveInfo` plus certificate extraction against the file again. With five
  section screens, opening an APK re-parses the archive on every navigation. Fix is small — key the
  same cache by file path plus last-modified — but decide whether it belongs in this work or as a
  separate `:core:apps` change.
- **Permission descriptions on APK-file input.** `loadDescription()` resolves against the system
  package manager, so platform permissions work, but permissions declared by a not-installed app
  have no system description. The declaring-package fallback covers that case (see Step 1).
