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

**This file tracks open work only.** Once an item reaches Done or Retired, its row moves to
[shipped.md](shipped.md) so this document doesn't fill up with finished work — look there for an ID
that isn't listed here. Section numbers can skip (there's no `1.1`, `1.3`, `1.4`, or `1.6` below)
because those sections shipped in full and moved out entirely; numbers never get reassigned to keep
cross-references stable.

---

## Part 1 — Free Tier

### 1.1b Browse by Attribute — *the replacement for the statistics screen*

`core:app-index` builds the `attribute → apps` side of Browse — target SDK, min SDK, install
source, permission, and signing certificate. See `core/app-index/AGENTS.md`. Everything in this
section has shipped except:

| ID    | Item                                | Status  | Notes                                                                                     |
|-------|-------------------------------------|---------|--------------------------------------------------------------------------------------------|
| CE-06 | "Also signed with this certificate" | Backlog | The other installed apps sharing this signer, listed on the app detail certificate screen. Data is already extracted; it needs a fingerprint-keyed lookup instead of `CE-05`'s organization-keyed bucket. **Revisit when that lookup is built** — see [features/app-detail.md](features/app-detail.md#deferred) |

### 1.2 App Detail

**Design doc:** [features/app-detail.md](features/app-detail.md) — approved, covers FR-10 … FR-18.

| ID    | Item                                | Status  | Notes                                                                              |
|-------|-------------------------------------|---------|-------------------------------------------------------------------------------------|
| FR-17 | Exported components view            | Partial | Exported/Unprotected filter chips ship in the Components screen (`FR-13`), backed by both intent filters (`EX-07`) and content-provider path permissions (`EX-08`), both done. Still a technical filter, not a risk verdict — needs a deliberate hub rule (see `RI-03`) before exposure becomes a "Worth knowing" finding |
| FR-18 | Custom permission audit             | Partial | Permissions screen's `Defined` scope lists the app's declared permissions with full detail sheets; no audit judgment (e.g. protection-level risk) applied yet |

### 1.5 Export & Share

| ID    | Item                        | Status | Notes                                                    |
|-------|-----------------------------|--------|-----------------------------------------------------------|
| FR-27 | Launch a component          | Todo   | `startForeignActivity` exists and is unused               |

### 1.7 Invisible Infrastructure

| ID    | Item                                | Status | Notes                                                                                                          |
|-------|-------------------------------------|--------|-----------------------------------------------------------------------------------------------------------------|
| FR-31 | Silent **app-state** snapshot collection | Todo | Records the whole analysed app state on every open — version, permissions (requested *and* granted), signing certificate, component counts, install source, manifest flags, sizes. Not permissions only. Feeds `HI-*`. Needs a local DB — see `OQ-01` |

### 1.8 Data Gaps — extraction that doesn't exist yet

These are raw-data items the app *could* read from `PackageManager` today but doesn't. All are free
tier by Principle 1, and every one of them also feeds a Pro pillar — which is why they're worth
doing in R0 rather than later.

| ID    | Item                              | Status  | Why it matters                                                                                       |
|-------|-----------------------------------|---------|---------------------------------------------------------------------------------------------------------|
| FR-33 | Native libraries / ABIs           | Todo    | Was in the Pro backlog; it's raw data, so free. Also the cheapest tracker-detection signal (`TR-03`) |
| FR-34 | Split APKs / config splits        | Todo    | Most modern installs are splits; APK size and "what's installed" are both wrong without it           |
| FR-36 | Signing scheme version & signers  | Todo    | v1–v4, multi-signer, rotation history — feeds `CE-02` / `CE-03`                                       |
| FR-37 | Storage breakdown                 | Todo    | `StorageStats` already gives app/data/cache; only the total is used                                  |
| FR-38 | Full install-source chain         | Todo    | Only `installingPackageName` is read; initiating + originating package are what actually distinguish a sideload from a store install |
| FR-44 | Declared `<uses-library>` entries | Backlog | Name and `android:required`, from the manifest for APK files and `sharedLibraryFiles` for installed apps. Deprioritized: most apps declare zero entries, and the ones that do are boilerplate (`android.test.runner`) or legacy trivia (`org.apache.http.legacy`) — the value is screen completeness, not a real finding. Revisit if a concrete tracker/risk signal ends up needing it |

**R0 remaining:** FR-17, FR-18 (Partial — blocked on `RI-03`, a Pro/R1 item), CE-06 (Backlog),
FR-27, FR-31 (blocked on `OQ-01`), FR-33, FR-34, FR-36 … FR-38, FR-44 (Backlog), plus `EN`.
Everything else originally scoped for R0 has shipped — see [shipped.md](shipped.md).

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
| RI-03 | Rule set: manifest hygiene     | Debuggable, cleartext, backup, exported-without-permission (`FR-32`, `EX-07`, `EX-08`) | S    |
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
|-------|------------------------------|-----------------------------------------------------------------|------|
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

| ID | Release         | Contents                                                                              | Duration       | Cumulative |
|----|-----------------|-----------------------------------------------------------------------------------------|----------------|------------|
| R0 | Free Rework     | Remaining: FR-17, FR-18, CE-06, FR-27, FR-31, FR-33 … FR-38, FR-44, plus `EN` — rest shipped, see [shipped.md](shipped.md) | ~7–8 weeks     | Week 8     |
| R1 | Pro Launch      | `HI`, `RI`, `TR`, `RP`, `CP`                                                             | ~5.5–6.5 weeks | Week 15    |
| R2 | Bulk Tools      | `BX`                                                                                     | ~1.5–2 weeks   | Week 17    |
| R3 | Optional polish | OP-01 … OP-03                                                                            | as-needed      | Ongoing    |

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
