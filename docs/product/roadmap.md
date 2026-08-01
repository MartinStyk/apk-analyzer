# APK Analyzer — Product & Monetization Roadmap

**Context:** Live app, 83k installs / 29k MAU. Audience: engineers, power users, security-conscious
people. Scope: Free + Pro (one-time purchase). Subscription and diversification plays sit in the
backlog at the bottom.

## Guiding Principles

1. **Interpretation and bulk/time-saving are what's worth charging for.** Raw data (manifests,
   exported components, native libs, reverse-lookups) is either already free in this app or freely
   replicable elsewhere (Android Studio's own APK Analyzer, `aapt`, `apktool`). Pro is reserved for
   a verdict someone can't form themselves, or tedious work automated at scale.
2. **One consistent freemium mechanic.** Where something is gated, it follows the same pattern
   everywhere: a sample is free, the complete set is Pro. No feature withholds information outright
   as a hook — this audience notices and resents that.
3. **Silent data collection starts in the free tier, ahead of the Pro launch.** The flagship feature
   needs history to be useful, so snapshot collection begins as soon as the reworked free app
   ships — by the time Pro exists, there's already real history to show.

## How To Read This Document

Every line item has a stable ID. **IDs are never reused and never renumbered** — if an item is
dropped, its ID is retired.

| Status       | Meaning                                                                            |
|--------------|------------------------------------------------------------------------------------|
| **Done**     | Built and wired in the current Compose rewrite on `develop`                        |
| **Partial**  | Some of it works; the gap is named in Notes                                        |
| **Port**     | Shipped in the legacy Play Store build, **not yet rebuilt** in the rewrite         |
| **Todo**     | Never existed                                                                      |
| **Retired**  | Deliberately dropped or absorbed by another item. ID kept so old references resolve |
| **Backlog**  | Wanted, not scheduled. The Notes column names the trigger to revisit it             |

> **Important:** the previous revision of this document marked several items "Existing". That was
> true of the *shipped legacy app*, not of the rewrite this roadmap builds on. Eight shipped
> features still need porting (`FR-12` … `FR-16`, `FR-19`, `FR-24`, `FR-25`), and the R0 estimate
> has been reworked to account for them.

---

## Part 1 — Free Tier

### 1.1 Inventory & Browse

| ID    | Item                                     | Status  | Notes                                                                       |
|-------|------------------------------------------|---------|------------------------------------------------------------------------------|
| FR-01 | Installed app list                       | Done    | `feature:apps` — live flow, package-change observer                         |
| FR-02 | Search with history                      | Done    | `SearchAppsUseCase`, `SearchHistoryRepository`                              |
| FR-03 | Advanced filter sheet                    | Done    | Source, SDK, APK/total size, install & update date, unused, recently used   |
| FR-04 | Quick filter chips                       | Done    | 9 presets incl. sideloaded / system / Play                                  |
| FR-05 | Sorting                                  | Done    | Name, APK size, total size, install date, target SDK, last update, last use |
| FR-06 | Recently viewed apps                     | Done    | `RecentlyViewedAppsRepository`, toggleable in settings                      |
| FR-07 | Install source filter                    | Done    | Folded into FR-03/FR-04; was listed as "New" before                         |

### 1.1b Browse by Attribute — *the replacement for the statistics screen*

One index, one screen, N dimensions. Pick a dimension → get its buckets with counts → tap a bucket
→ get the apps. This subsumes both the browse-by-permission feature **and** device statistics: a
distribution *is* the bucket counts of an index, and rendering it this way makes every number a
door instead of a dead end.

`core:app-statistics` already builds two thirds of this. `StatisticsData` is literally
`Map<attributeValue, List<packageName>>` for install location, target SDK, min SDK, source and sign
algorithm — bucket → apps, package names retained. That half gets promoted. The `MathStatistics`
half (mean/median APK size, average activity/service/provider/receiver/permission counts) is the
part that was low-value trivia, and it gets dropped.

| ID    | Item                                | Status  | Notes                                                                                     |
|-------|-------------------------------------|---------|--------------------------------------------------------------------------------------------|
| CE-01 | Attribute index infrastructure      | Todo    | `attribute → apps` builder + bucket counts. Reuses the map half of `LocalApplicationStatisticManager`, including its proven full-device pass with progress |
| CE-05 | Browse screen                       | Todo    | Dimension picker → bucket list with counts → app list. One screen serves every dimension  |
| FR-08 | ↳ dimension: permission             | Partial | Multi-permission filter works in `feature:apps`; the standalone tab is a stub screen      |
| FR-09 | ↳ dimension: signing certificate    | Todo    | Group by cert fingerprint, not sign algorithm — "same publisher" is the useful question; algorithm has ~5 values and answers nothing |
| FR-40 | ↳ dimension: target SDK / min SDK   | Todo    | Already built in the maps. This is what the SDK chart was, made tappable                 |
| FR-41 | ↳ dimension: install source         | Todo    | Already built in the maps                                                                 |
| FR-42 | ↳ dimension: shared UID             | Todo    | Needs `FR-35`                                                                             |
| FR-43 | ↳ dimension: app category           | Todo    | Needs `FR-39`                                                                             |
| CE-06 | "Also signed with this certificate" | Backlog | The other installed apps sharing this signer, listed on the app detail certificate screen. Data is already extracted; it needs the `CE-01` index. **Revisit when CE-01 / FR-09 certificate grouping is built** — see [features/app-detail.md](features/app-detail.md#deferred) |

**Module consolidation:** `feature:permissions` and `feature:statistics` are both placeholder
screens and both hold a top-level nav slot. They collapse into one `feature:browse`. That frees a
bottom-nav slot — the natural home for Pillar 1's What Changed tab in R1.

### 1.2 App Detail

**Design doc:** [features/app-detail.md](features/app-detail.md) — approved, covers FR-10 … FR-18.

| ID    | Item                                | Status  | Notes                                                                              |
|-------|-------------------------------------|---------|-------------------------------------------------------------------------------------|
| FR-10 | Detail overview + badges            | Done    | Counts, badge computation, certificate summary card                                |
| FR-11 | General info screen                 | Done    | `feature:app-detail` → `generalinfo`                                               |
| FR-12 | Permissions view (per app)          | Port    | Action + event exist; nav target logs "not yet implemented"                        |
| FR-13 | Components views                    | Port    | Activities / services / receivers / providers — same, all four unwired             |
| FR-14 | Certificate detail view             | Port    | Extractor produces full data (MD5/SHA1/SHA256 of cert + key, validity, serial, issuer/subject); only a summary card is rendered |
| FR-15 | Features (uses-feature) view        | Port    | `Feature` model and count exist, no screen                                         |
| FR-16 | Manifest viewer                     | Port    | `ManifestParser` exists, no viewer                                                 |
| FR-17 | Exported components view            | Todo    | `isExported` is captured on all four component types; needs intent filters (`EX-07`) to be genuinely useful |
| FR-18 | Custom permission audit             | Todo    | Data ready — `Permissions.defined`                                                 |

### 1.3 APK File Analysis

| ID    | Item                                 | Status  | Notes                                                                        |
|-------|--------------------------------------|---------|-------------------------------------------------------------------------------|
| FR-19 | Open `.apk` from another app         | Port    | `VIEW` / `INSTALL_PACKAGE` intent filter is **commented out** in the manifest |
| FR-20 | Pick an `.apk` from storage          | Todo    | `AppDetailInput.ApkFile` and `FileUtil.copyUriToCache` exist; no entry point  |
| FR-21 | Recent APK files                     | Todo    | Cheap once FR-19/FR-20 land                                                  |

### 1.4 Device Statistics — **retired**

| ID    | Item                        | Status  | Notes                                                                            |
|-------|-----------------------------|---------|-----------------------------------------------------------------------------------|
| FR-22 | Device statistics screen    | Retired | Superseded by §1.1b. A chart of "how many apps target API 30" is the bucket counts of the target-SDK index, rendered without a way to act on it |
| FR-23 | Chart → filtered list jumps | Retired | Not a feature — it's what an attribute index does natively. It only existed as a task because statistics and browse were separate screens |

### 1.5 Export & Share

| ID    | Item                        | Status | Notes                                                    |
|-------|-----------------------------|--------|-----------------------------------------------------------|
| FR-24 | APK export / share          | Port   | Action + event exist, handler logs "not yet implemented" |
| FR-25 | Icon export                 | Port   | Designed in [features/app-detail.md](features/app-detail.md), Step 5 |
| FR-26 | Copy / share app summary    | Todo   | `ClipboardManager` already in `core:common`              |
| FR-27 | Launch a component          | Todo   | `startForeignActivity` exists and is unused              |

### 1.6 App & UI

| ID    | Item                            | Status | Notes                                        |
|-------|---------------------------------|--------|-----------------------------------------------|
| FR-28 | Theme / color scheme setting    | Done   | `ColorAppScheme`, follow-system default      |
| FR-29 | Settings screen                 | Done   | `feature:settings`                            |
| FR-30 | Usage-access permission flow    | Done   | Rationale sheet + settings deep link          |

### 1.7 Invisible Infrastructure

| ID    | Item                                | Status | Notes                                                                                                          |
|-------|-------------------------------------|--------|-----------------------------------------------------------------------------------------------------------------|
| FR-31 | Silent **app-state** snapshot collection | Todo | Records the whole analysed app state on every open — version, permissions (requested *and* granted), signing certificate, component counts, install source, manifest flags, sizes. Not permissions only. Feeds `HI-*`. Needs a local DB — see `OQ-01` |

### 1.8 Data Gaps — extraction that doesn't exist yet

These are raw-data items the app *could* read from `PackageManager` today but doesn't. All are free
tier by Principle 1, and every one of them also feeds a Pro pillar — which is why they're worth
doing in R0 rather than later.

| ID    | Item                              | Status | Why it matters                                                                                       |
|-------|-----------------------------------|--------|-------------------------------------------------------------------------------------------------------|
| FR-32 | Manifest security flags           | Todo   | `debuggable`, `allowBackup`, `usesCleartextTraffic`, network security config — the single highest-value/lowest-cost gap; direct input to `RI-03` |
| FR-33 | Native libraries / ABIs           | Todo   | Was in the Pro backlog; it's raw data, so free. Also the cheapest tracker-detection signal (`TR-03`) |
| FR-34 | Split APKs / config splits        | Todo   | Most modern installs are splits; APK size and "what's installed" are both wrong without it           |
| FR-35 | Shared UID group                  | Todo   | Security-relevant, one field, currently unread                                                       |
| FR-36 | Signing scheme version & signers  | Todo   | v1–v4, multi-signer, rotation history — feeds `CE-02` / `CE-03`                                       |
| FR-37 | Storage breakdown                 | Todo   | `StorageStats` already gives app/data/cache; only the total is used                                  |
| FR-38 | Full install-source chain         | Todo   | Only `installingPackageName` is read; initiating + originating package are what actually distinguish a sideload from a store install |
| FR-39 | App category                      | Todo   | One field; unlocks the `FR-43` browse dimension                                                      |
| EX-07 | Component intent filters          | Todo   | What an exported component actually responds to. Makes `FR-17` meaningful and feeds `RI-03`. Moved here from Pro — it is raw manifest data |
| FR-44 | Declared `<uses-library>` entries  | Todo   | Name and `android:required`, from the manifest for APK files and `sharedLibraryFiles` for installed apps. The platform-library half of device requirements — `org.apache.http.legacy` on a modern app is a signal the hardware list cannot give |

**R0 scope:** CE-01, CE-05, FR-08 … FR-21, FR-24 … FR-44, EX-07. Excludes the retired FR-22/FR-23 and the backlogged CE-06.
**R0 estimate: ~7–8 weeks** (was ~4, which assumed the ported items were already present).
Retiring the statistics screen roughly pays for the browse screen — one surface instead of two,
against an index that is already two-thirds built.

---

## Part 2 — Pro Tier

The three pillars are the *story we sell*. Below they're decomposed into the **functional areas**
the work actually lands in — each maps to one module boundary. Areas are identified by their item
ID prefix rather than a letter, so an item keeps its ID if it moves.

| Area                              | Pillar(s) served | Lands in                          |
|-----------------------------------|------------------|------------------------------------|
| `EN` — Entitlement & Billing      | all              | `core:billing`, `core:entitlement` |
| `HI` — Snapshot & History         | 1                | `core:app-history`                 |
| `RI` — Risk & Trust Rules         | 2, 3             | `core:risk`                        |
| `TR` — Tracker & SDK Detection    | 2, 3             | `core:trackers`                    |
| `RP` — Per-App Security Report    | 2                | `feature:security-report`          |
| `BX` — Device-Wide Audit & Export | 3                | `feature:bulk-audit`               |
| `CP` — Content Distribution       | 2                | build + backend                    |

**Two areas from the previous revision dissolved.** *Static Analysis Extraction* had one
tracker-only item (`EX-06`, now inside `TR`) and one free-tier item (`EX-07`, now in §1.8).
*Certificate Intelligence* was three different things wearing one hat: the index is free-tier
browse (`CE-01`), the clone verdict is a risk rule (`CE-02`), and the trust checks are risk rules
(`CE-03`, `CE-04`). No IDs were reused or renumbered.

### `EN` — Entitlement & Billing

| ID    | Task                            | Detail                                                        | Size |
|-------|---------------------------------|----------------------------------------------------------------|------|
| EN-01 | Play Billing client             | One-time product, purchase flow                                | S    |
| EN-02 | Purchase state + restore        | Local cache, re-verification on launch                         | S    |
| EN-03 | Entitlement gate API            | One `ProGate` every feature asks                               | XS   |
| EN-04 | "Sample free / complete Pro" UI | The single reusable gate pattern from Principle 2              | S    |
| EN-05 | Paywall screen                  | Plus the CTA placements that lead into it                      | S    |
| EN-06 | Purchase funnel analytics       | Gate impressions → tap → purchase; drives the Part 3 decisions | XS   |

**Area total: ~3–4 days**

### `HI` — Snapshot & History *(Pillar 1 — What Changed)*

Snapshots capture **whole app state**, not permissions alone (`FR-31`). That costs almost nothing
extra at write time and turns a permission log into an app history: *"v4.2 → v4.3, gained Contacts,
signer changed, grew 40 MB."* A signer change between versions is the single strongest tamper
signal the app can produce, and it is only visible if snapshots carry the certificate.

| ID    | Task                          | Detail                                                                        | Size |
|-------|-------------------------------|--------------------------------------------------------------------------------|------|
| HI-01 | Snapshot store                | Local DB, schema, migrations — **new dependency, see `OQ-01`**                 | M    |
| HI-02 | Snapshot writer               | Capture on app open; consumer of `FR-31`                                      | S    |
| HI-03 | Diff engine                   | Version, permissions requested/granted, certificate, components, flags, size  | M    |
| HI-04 | Retention & pruning           | Whole-state snapshots are bigger — dedupe unchanged fields, cap per app       | S    |
| HI-05 | Baseline & empty state        | First run has no history — must not look broken                               | XS   |
| HI-06 | Free teaser card              | Single most recent change                                                     | S    |
| HI-07 | Full change log (Pro)         | All apps, chronological, filterable by change type                            | M    |
| HI-08 | Per-app timeline (Pro)        |                                                                                | S    |
| HI-09 | New-changes badge             |                                                                                | XS   |

**Area total: ~6–8 days**

### `RI` — Risk & Trust Rules *(Pillar 2)*

One engine, one finding type, one severity scale — whatever the signal's origin. Certificate trust
is not a separate discipline from permission risk; both are "evaluate a fact about this app, emit a
finding."

| ID    | Task                           | Detail                                                                       | Size |
|-------|--------------------------------|-------------------------------------------------------------------------------|------|
| RI-01 | Rule model                     | Declarative rule + finding types                                              | S    |
| RI-02 | Rule set: permission combos    | Dangerous combinations, curated by hand                                       | M    |
| RI-03 | Rule set: manifest hygiene     | Debuggable, cleartext, backup, exported-without-permission (`FR-32`, `EX-07`) | S    |
| RI-04 | Rule set: SDK anomalies        | Deprecated target SDK, weak signing algorithm                                 | S    |
| CE-02 | Rule set: shared-cert / clone signal | Reads the `CE-01` index; flags apps that unexpectedly share a signer    | S    |
| CE-03 | Rule set: certificate trust    | Debug, self-signed, expired — extends `CertificateTrustLevel`                 | XS   |
| CE-04 | Known-publisher labeling       | "Signed by Google LLC" reads better than a fingerprint                        | S    |
| RI-05 | Severity banding               | Low / medium / high, and how findings roll up per app                         | XS   |
| RI-06 | Evaluation engine + caching    | Invalidate on app update                                                      | S    |

**Area total: ~6–7 days**

### `TR` — Tracker & SDK Detection *(Pillar 2)*

`EX-06` moved in here: DEX scanning has exactly one consumer, and splitting it across an area
boundary bought nothing.

| ID    | Task                         | Detail                                                          | Size |
|-------|------------------------------|------------------------------------------------------------------|------|
| EX-06 | Class/string scanning primitive | Read DEX package names out of an APK — the substrate `TR-03` needs; see `OQ-05` | M |
| TR-01 | Dataset sourcing + license   | Use an existing open dataset, don't build proprietary            | XS   |
| TR-02 | Dataset format + loader      | Bundled, versioned                                               | S    |
| TR-03 | Matcher                      | Runs against `EX-06` output and `FR-33` native libs              | M    |
| TR-04 | Categorization               | Ad / analytics / crash reporting / etc.                          | XS   |
| TR-05 | Confidence & false positives | A wrong tracker claim is the most damaging error we can make     | S    |

**Area total: ~6–7 days**

### `RP` — Per-App Security Report *(Pillar 2)*

**Scope: one app, one screen.** Opened from an app's detail page. Device-wide is a separate
product — that's `BX`, which runs this same engine across every installed app and summarises.

**Business value.** The free app answers *what*: this app requests 47 permissions, 12 of them
dangerous, and here is its certificate. The report answers *should I care* — the question a
permission list cannot answer no matter how well it's rendered. Concretely: a flashlight app
holding Contacts + SMS + Location, carrying six ad SDKs, signed with a debug certificate, targeting
API 26. Every one of those facts is already free in the app. The verdict — *these facts together
are abnormal for this kind of app, here is what each one means* — is not derivable by the user, is
not replicable with `aapt`, and is the thing Principle 1 says we can charge for.

Per Principle 2 the gate is a sample, not a wall: the report opens for everyone showing the
highest-severity finding in full; the remaining findings are Pro.

| ID    | Task                         | Detail                                                       | Size |
|-------|------------------------------|---------------------------------------------------------------|------|
| RP-01 | Report screen                | One app, expandable section per category                      | M    |
| RP-02 | Shared components            | Severity badge, finding row, expandable card                  | S    |
| RP-03 | Per-finding explanation copy | The actual product — the interpretation people pay for        | M    |
| RP-04 | Disclaimer component         | Risk-indicator language, never "safe" or "verified"           | XS   |
| RP-05 | Free-sample gate placement   | Top finding free, rest Pro. Uses `EN-04`                      | XS   |
| RP-06 | Entry points                 | From app detail and from app-list risk badges                 | S    |

**Area total: ~4–5 days**

### `BX` — Device-Wide Audit & Export *(Pillar 3)*

**Scope: every installed app at once.** Same rules, same findings, same severities as `RP` — the
difference is orchestration, a sortable cross-app summary, and export. The value here is not
interpretation (that's `RP`) but *tedium removed*: nobody opens 300 app reports by hand.



| ID    | Task                  | Detail                                                    | Size |
|-------|-----------------------|------------------------------------------------------------|------|
| BX-01 | Scan orchestration    | Background iteration, progress, cancel, resume             | M    |
| BX-02 | Result store          | Survives process death on a 400-app device                 | S    |
| BX-03 | Bulk summary list     | Sortable, risk-flagged                                     | S    |
| BX-04 | CSV export            |                                                            | XS   |
| BX-05 | JSON export           |                                                            | XS   |
| BX-06 | PDF export            |                                                            | S    |
| BX-07 | Share / save flow     | Storage Access Framework                                   | XS   |

**Area total: ~5–6 days**

### `CP` — Content Distribution

| ID    | Task                     | Detail                                                                       | Size |
|-------|--------------------------|-------------------------------------------------------------------------------|------|
| CP-01 | Dataset versioning       | Ship rules and tracker list with a version stamp                              | XS   |
| CP-02 | Remote refresh           | Needs a hosting decision; the one component with real ongoing upkeep cost     | S    |
| CP-03 | Update cadence           | Who refreshes the tracker list, how often — an ops commitment, not a build task | XS |

**Area total: ~1.5–2 days**

**Suggested build order:** `EN` → `HI` (cheapest pillar, ships a fast win) → `RI` → `TR` → `RP` →
`BX` → `CP`. `RI` and `TR` can run in parallel — they meet only at `RP`.

---

## Optional / Backlog within Pro

| ID    | Item                              | Notes                                                                            |
|-------|-----------------------------------|-----------------------------------------------------------------------------------|
| OP-01 | APK diff / compare tool           | Worth building only if wired into `RI` — flag what changed as *risky*, not raw   |
| OP-02 | Custom / exportable report templates | Polish on `BX` exports                                                        |
| OP-03 | Per-app risk score history        | `HI` × `RI` — "this app got riskier" is a stronger hook than either alone         |

*(The old backlog entry for a native-library inspector moved to the free tier as `FR-33`. Chart
entry points were retired with the statistics screen — see §1.4.)*

---

## Backlog

| ID    | Item                     | Notes                                                                                     |
|-------|--------------------------|--------------------------------------------------------------------------------------------|
| BL-01 | Subscription tier        | Real-time alerts, malware/reputation API, cloud sync, digest. Highest ceiling, real recurring cost. `HI-06`'s tap-through rate is the live demand signal |
| BL-02 | B2B / team tier          | Different buyer, different sales motion                                                    |
| BL-03 | Affiliate referrals      | Low effort, pick up opportunistically                                                      |
| BL-04 | Light ad tier            | Not the right first move for this audience                                                 |

---

## Release Plan

| ID | Release         | Contents                                                       | Duration     | Cumulative |
|----|-----------------|-----------------------------------------------------------------|--------------|------------|
| R0 | Free Rework     | CE-01, CE-05, FR-08 … FR-21, FR-24 … FR-44, EX-07, plus `EN`    | ~7–8 weeks   | Week 8     |
| R1 | Pro Launch      | `HI`, `RI`, `TR`, `RP`, `CP`                                    | ~5.5–6.5 weeks | Week 15  |
| R2 | Bulk Tools      | `BX`                                                            | ~1.5–2 weeks | Week 17    |
| R3 | Optional polish | OP-01 … OP-03                                                   | as-needed    | Ongoing    |

**Committed roadmap (R0–R2): ~15–17 weeks**

---

## Open Questions

| ID    | Question                                                                                                  |
|-------|------------------------------------------------------------------------------------------------------------|
| OQ-01 | `HI-01` needs a real local database. The project only has DataStore today, and adding a dependency requires sign-off |
| OQ-02 | `EN-01` needs the Play Billing library — same sign-off                                                     |
| OQ-03 | `BX-01` background scanning: coroutine + foreground service, or WorkManager (another dependency)?          |
| OQ-04 | `CP-02` remote refresh needs a hosting decision, and that decision sets the ongoing cost floor for Pro     |
| OQ-05 | `EX-06` DEX scanning on-device has a real performance and battery cost on large APKs — needs a spike before `TR` is committed |
