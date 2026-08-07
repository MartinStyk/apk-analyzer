    # App Detail — Full Data Presentation

**Roadmap:** [FR-10 … FR-18](../roadmap.md#12-app-detail), [FR-25](../roadmap.md#15-export--share), plus [EX-07](../roadmap.md#18-data-gaps--extraction-that-doesnt-exist-yet) · R0
**Status:** Approved design. [Steps 1–3](#implementation-order), the hub rework, manifest viewer,
and export actions shipped; Requirements and the final polish pass remain
**Scope:** surface everything `AppDetail` holds inside `feature:app-detail`, provide a readable
manifest, and finish base-APK and icon export. The Requirements screen remains separate work.

## Why

`AppDetail` carries the complete analysis of an app, but the UI renders only a fraction of it.

| Data | Today |
|---|---|
| `AppInfo` (23 fields) | Fully shown on the General Info screen |
| `Permissions` | Full requested/defined lists with grant and protection details |
| `activities` / `services` / `receivers` / `contentProviders` | Full searchable, filterable lists with item details |
| `certificates` | All certificate fields, fingerprints, validity, signer count, and key rotation |
| `features` | Count only |

General Info, Permissions, Components, Certificates, and the manifest viewer are wired from the app
detail hub. Requirements remains.

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

### Where the device check lives

The Requirements screen needs to know whether *this device* satisfies what an app declares. That
answer could plausibly be baked into `AppDetail` — `Feature` gains an `isAvailableOnDevice` flag and
every consumer gets it for free. It should not be, for one reason that outlives the convenience:

**`AppDetail` describes an app. Device availability describes a pairing of an app and a device.**
`AppDetailRepositoryImpl` is a `@Singleton` holding a `ConcurrentHashMap<String, AppDetail>`
invalidated by `PackageChangesObserver` — a cache keyed on the app and invalidated on app change.
Putting device state inside it makes cached entries silently depend on a second input that the key
does not mention and the invalidation does not track. It is correct today, because device features
are fixed until reboot. It is the kind of correct that stops being correct quietly.

The cost of keeping them apart is one extra injection in one ViewModel. The cost of merging them is
a shared model carrying a field four of its five consumers ignore, and a cache whose key no longer
describes its contents.

So: `AppDetail` stays a description of the app, `DeviceFeaturesRepository` answers for the device,
and the ViewModel combines them while mapping to state — which is mapping, not resolving. The same
rule is why grant state is fine inside `UsedPermission`: that is a property of *this install*, which
is what `AppDetail` already models in `InstalledPackage` mode.

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
│ ⚠ Can access location in background    › │
└──────────────────────────────────────────┘
```

Each line deep-links to the relevant section. Most apps produce zero lines and the card does not
render. Attention is spent only where the app is unusual.

**Changed — cards preview instead of counting where the available evidence supports interpretation:**

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
│ SCHEME      v2, v3                     ⓘ │
│ ALGORITHM   SHA256withRSA                │
│ SERIAL      0A:1B:2C:3D:4E:5F:60:71      │
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
│ Public key fingerprints              ›   │  ← collapsed; same three inside
└──────────────────────────────────────────┘
```

**Certificate fingerprints are open, public key fingerprints are one tap down.** Six hash boxes is a
wall of hex, and the two groups answer different questions: the certificate hash is what you compare
against a published value, the public key hash is what you pin against. Everyone who opens this
screen wants the first; a narrower group wants the second. Nothing is hidden that you came for —
SHA-256 leads the open group, because it is the one people actually compare. Each box follows the
[hash convention](#hashes-and-fingerprints). MD5 stays for completeness and is last, since it
survives here as legacy trivia rather than as something anyone should use.

**Serial number renders as uppercase hex**, the way `keytool` and every CA print it, in the same
monospace style as a fingerprint. It cannot be an `Int`: real serials run to 128 bits, and
`Certificate.serialNumber` is currently `Int`, filled by `certificate.serialNumber.toInt()` in
`CertificateExtractorImpl`. `BigInteger.toInt()` keeps only the low 32 bits, so the field is
plausible-looking and wrong for most real certificates today. This screen is what makes that
visible, so the fix belongs to this work — see Step 3.

**Scheme states how the APK was signed** — `v1, v2, v3, v4` — because it is the signing fact with
consequences, and the one readers most often confuse with `ALGORITHM`. v1-only on a modern app is a
real signal; SHA-1 versus SHA-256 in the algorithm row mostly is not. The ⓘ explains the difference
between the two rows, which is the entire reason to show them adjacent.

Scheme is **not a field read** — the public `SigningInfo` API does not expose it, so it requires
parsing the APK signing block and depends on roadmap `FR-36`. What is free from `SigningInfo`
today, and shows regardless of whether `FR-36` lands: `hasMultipleSigners()` and
`hasPastSigningCertificates()`, the latter meaning the signing key was rotated. If `FR-36` slips,
the row shows rotation and multi-signer state and omits the version list rather than blocking the
screen.

"Self-signed" carries an ⓘ because it looks alarming and is completely normal on Android. Same for
an expired signing certificate on an already-installed app, which is harmless. Multiple
certificates render as stacked cards.

### Requirements (features)

```
┌──────────────────────────────────────────┐
│ ← Device requirements                    │
├──────────────────────────────────────────┤
│ (Hardware ▾)                             │  ← only when the app uses libraries
├──────────────────────────────────────────┤
│ ⚠ 1 requirement this device cannot meet  │  ← only when there is a miss
├──────────────────────────────────────────┤
│ REQUIRED · 9                             │
│ The app won't install without these      │
│                                          │
│ 📷  Camera                               │
│     android.hardware.camera              │
│ 📡  NFC                        Not on    │
│     android.hardware.nfc     this device │
│ 📶  Wi-Fi                                │
│ 🎮  OpenGL ES 3.0                        │
│     0x00030000                           │
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

**OpenGL ES is a version, not a name, and the model has to say so.** `getFeatures` in
`AppDetailRepositoryImpl` currently collapses the two into one string field with
`name = it.name ?: it.glEsVersion`, because a GL ES `FeatureInfo` carries a null `name` and a
`reqGlEsVersion` instead. The result is a row whose identifier is `3.0` — a requirement with no
indication of what is at 3.0. Nothing downstream can tell the two kinds apart without sniffing the
string, and only the count is consumed today, so the conflation has been invisible.

`Feature` becomes a sealed interface with a `Hardware` case carrying a name and an `OpenGlEs` case
carrying a version. That makes the display obvious rather than clever:

- **Label is "OpenGL ES 3.0"**, from `FeatureInfo.getGlEsVersion()`.
- **Raw identifier is the hex**, `0x00030000` — which honours
  [plain name first, raw identifier always present](#two-cross-cutting-rules) with the value that is
  genuinely useful, because the hex is literally what the manifest's `android:glEsVersion` attribute
  contains and what you would paste into a bug report.
- **The device check is a numeric comparison** of `reqGlEsVersion` against the device's, never a
  string compare — `"10.0" < "3.0"` lexically, and that will be a real version one day.
- **A miss names what the device has**: "Needs 3.1 · this device has 3.0" says more than "not on
  this device", and the version is the one requirement where the gap is a matter of degree.

**Every requirement is checked against this device.** This is the one screen that can turn a list of
identifiers into an answer, and the answer is *"will this run here?"*

The check comes from a **`DeviceFeaturesRepository` in `:core:apps`**, not from the ViewModel
touching `PackageManager`. Interface plus `internal` `Impl`, `@Binds`, `@Singleton`, `suspend`
accessor switching on `dispatcherProvider.io()`, per the module rules.

- **One call, not one per feature.** `getSystemAvailableFeatures()` returns the device's whole
  `FeatureInfo[]` in a single binder call; the repository turns it into a `Set<String>` and the
  per-feature check becomes set membership. `hasSystemFeature(name)` in a loop would be N binder
  calls to answer a question one call already answered.
- **Computed once per process.** Device features cannot change without a reboot, so the set is
  memoized in the `@Singleton` and never invalidated. No flow, no observer, no refresh.
- **OpenGL ES is not a set member.** The device's `FeatureInfo[]` carries one entry with a null
  `name` and `reqGlEsVersion` set to the highest supported version, and an app's GL ES requirement
  arrives the same way. That one is a numeric comparison, not a lookup, so the repository exposes
  the device's version separately rather than pretending it fits in the set — matching the
  `Feature.OpenGlEs` case on the app side.

- **Only misses are marked.** A column of green ticks is noise; a single "Not on this device" is the
  finding. When there are none, the screen looks exactly as it does today.
- **It matters most in APK-file mode**, which is the inverse of the rule everywhere else in this
  design. Elsewhere APK mode has less to show. Here it has more, and it is the mode where the answer
  is actionable: you learn the APK will not install *before* trying it.
- **A miss on an installed app is a real finding, not a contradiction.** Play filters downloads on
  `uses-feature`, but the platform does not enforce it at install time — so an app sideloaded past
  that filter can sit on a device that cannot satisfy its own stated requirements. That is worth
  surfacing, and it is why the check runs in both modes rather than only for APK files.
- **Optional requirements that are missing are stated more softly.** Missing an optional feature is
  by definition fine; it explains why part of the app may do nothing, and nothing more.

**Scope is `Hardware` or `Libraries`** — the same selector Permissions and Components use, rendering
only when the app declares `<uses-library>` entries. Hardware requirements and platform library
requirements are the same question one layer apart ("what does this app need that the device
supplies?"), and `org.apache.http.legacy` on a modern app says something the hardware list never
will. Libraries carry the same required/optional split, from `android:required`, and the same
device check — a declared library either resolves on this device or does not.

The library list needs extraction that does not exist yet; it depends on roadmap `FR-44`. If that
slips, the scope chip does not render and the screen is hardware-only, exactly as it is today.

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
- A `SelectorChip` in `:core:ui-library` — `OutlinedChip` with `ArrowDropDown` opening a
  `BottomSheet` of options. Every piece already exists in `:core:ui-library`; this composes them
  into a generic, reusable chip any screen can drive.
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
- Extend `SelectorChip` from Step 1 only if the five-option scope exposes a gap the two-option
  use did not.

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
- **Fix `Certificate.serialNumber`.** It is `Int`, assigned `certificate.serialNumber.toInt()` in
  `CertificateExtractorImpl`; `BigInteger.toInt()` truncates to the low 32 bits, so the value is
  wrong for any serial above 32 bits, which is most of them. Change the field to `String` and format
  it once in the extractor as uppercase colon-separated hex, matching `keytool` and the fingerprint
  convention. Do this before rendering the row — the display is what exposes the defect.
- Signing scheme: read `signingInfo.hasMultipleSigners()` and `hasPastSigningCertificates()`, which
  are available now, and the `v1 … v4` version list from `FR-36` if it has landed. Model the version
  list as nullable and omit that part of the row when it is absent, so the screen does not block on
  `FR-36`.
- `CertificatesNavKey`, ViewModel, state carrying all 13 certificate fields.
- Self-signed detection (issuer equals subject) and expiry evaluation against today.
- Debug-certificate banner, signer / validity / scheme / algorithm / serial blocks.
- Certificate fingerprint group open with all three hashes; public key fingerprint group in an
  expander, collapsed by default, same three hashes and same treatment inside.

### Step 4 — Requirements screen

- **Split `Feature` into a sealed interface** — `Hardware(name)` and `OpenGlEs(version)` — and stop
  writing `name = it.name ?: it.glEsVersion` in `AppDetailRepositoryImpl.getFeatures`. Keep the raw
  `reqGlEsVersion` int on the `OpenGlEs` case for the hex identifier and the version comparison; the
  formatted string is for the label only. The blast radius is one line of mapping and
  `featuresCount`, because nothing else reads `features` yet — which is why the change is cheap now
  and would not stay cheap.
- `FeaturesNavKey`, ViewModel, required/optional split.
- Curated friendly-name and icon mapping for well-known feature names, raw-string fallback for
  unrecognized `android.*` names, and the dedicated GL ES rendering.
- **`DeviceFeaturesRepository`** in `:core:apps` — interface + `internal` `Impl`, `@Binds`,
  `@Singleton`. Exposes the device's available feature names as a memoized `Set<String>` built from
  one `getSystemAvailableFeatures()` call, plus the device's GL ES version separately. Never throws;
  an unavailable package manager yields an empty set, which reads as "unknown" rather than "missing"
  at the UI.
- **Requirements ViewModel injects it alongside `AppDetailRepository`** and combines the two while
  mapping to state. It does not call `PackageManager` itself, and `AppDetail` does not carry the
  answer — see [the note on where this belongs](#where-the-device-check-lives).
- Mark misses only, count them into the summary line, and soften the wording for optional misses.
  Runs in both analysis modes — see the [screen notes](#requirements-features) for why an installed
  app can still miss.
- **Libraries scope**, only if `FR-44` has landed: reuse `SelectorChip` from Step 1, apply the
  same required/optional split and the same device check to declared `<uses-library>` entries. If
  `FR-44` has not landed, skip this bullet — the chip does not render and nothing else changes.

### Step 5 — Hub rework

- Card previews: dangerous permission group icons, certificate validity line and self-signed note,
  and required/optional feature split. Components stay as neutral per-type counts until `EX-07`
  provides the intent-filter evidence needed to interpret exposure.
- "Worth knowing" card with its rule set: debug or not-yet-valid certificate, a target at least four
  API levels behind the device, and actually granted high-impact access such as background location,
  messages, call history, contacts, or calendar. Exported components and merely requested
  permissions are not findings. Each row deep-links.
- Extend `AppDetailState.Loaded` with the derived values the previews need.
- **Save icon**: an icon loader in `:core:apps` returning the full-resolution bitmap for a package
  name or an APK path (setting `sourceDir` / `publicSourceDir` in the archive case), PNG encoding,
  an `ACTION_CREATE_DOCUMENT` launcher wired to `AppDetailEvent.SaveIcon` in
  `AppDetailEntryProvider`, and the success/failure messages. Replaces the
  `Logger.d("not yet implemented")` handler.
- **Manifest viewer**: readable namespaced XML for installed packages and APK files, lazy-rendered
  by line. Search shows each matching line with its full owning start tag, so a matching attribute
  keeps the component name and related attributes needed to interpret it. Installed packages read
  the base APK directly; when additional splits are installed, the screen states how many other
  manifest documents exist rather than presenting an arbitrary split or claiming a merged source.
- **Export APK**: `ACTION_CREATE_DOCUMENT` writes the installed base APK. When the app uses split
  APKs, a persistent bottom sheet explains that the saved base APK is not a complete install
  package. APK-file mode hides the redundant export action.

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
- **"Also signed with this certificate"** (`CE-06`) — listing the other installed apps that share
  this signer would turn the fingerprint section from a hex dump into an answer, and the data is
  already extracted for every app. It needs the device-wide certificate index (`CE-01`), so it is
  backlogged to be built with the certificate grouping work rather than bolted onto this screen
  first. Leave room for it below the fingerprint groups.

---

## Open questions

- **Repeated loading on APK-file input only.** `AppDetailRepositoryImpl` is `@Singleton` and holds a
  `ConcurrentHashMap<String, AppDetail>` keyed by package name, invalidated by
  `PackageChangesObserver`. Installed packages are parsed once and every section ViewModel after
  that is a map lookup — no problem there. But `details(AppReference.ApkFile(...))` has **no
  cache**: each call runs `getPackageArchiveInfo` plus certificate extraction against the file
  again. With five
  section screens, opening an APK re-parses the archive on every navigation. Fix is small — key the
  same cache by file path plus last-modified — but decide whether it belongs in this work or as a
  separate `:core:apps` change.
- **Permission descriptions on APK-file input.** `loadDescription()` resolves against the system
  package manager, so platform permissions work, but permissions declared by a not-installed app
  have no system description. The declaring-package fallback covers that case (see Step 1).
