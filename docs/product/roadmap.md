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
that isn't listed here. Section numbers can skip (there's no `1.1`, `1.3`, `1.4`, `1.5`, or `1.6`
below) because those sections shipped in full and moved out entirely; numbers never get reassigned
to keep cross-references stable.

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

### 1.7 Invisible Infrastructure

| ID    | Item                                | Status | Notes                                                                                                          |
|-------|-------------------------------------|--------|-----------------------------------------------------------------------------------------------------------------|
| FR-31 | Silent **app-state** snapshot collection | Todo | Records the complete analysed app state — version, permissions requested, signing certificate, components, native libraries, install source, manifest flags, sizes. Not permissions only, and not the reduced app-detail hub set: the deep data (native libs, signing schemes) is exactly what makes a diff worth reading. Captured per **install instance**, not per version code. Feeds `HI-*`. Local DB already exists (Room, used elsewhere in the project) — not a new dependency; needs a cost measurement — see `OQ-07` |

### 1.8 Data Gaps — extraction that doesn't exist yet

These are raw-data items the app *could* read from `PackageManager` today but doesn't. All are free
tier by Principle 1, and every one of them also feeds a Pro pillar — which is why they're worth
doing in R0 rather than later.

| ID    | Item                              | Status  | Why it matters                                                                                       |
|-------|-----------------------------------|---------|---------------------------------------------------------------------------------------------------------|
| FR-37 | Storage breakdown                 | Todo    | `StorageStats` already gives app/data/cache; only the total is used                                  |
| FR-44 | Declared `<uses-library>` entries | Backlog | Name and `android:required`, from the manifest for APK files and `sharedLibraryFiles` for installed apps. Deprioritized: most apps declare zero entries, and the ones that do are boilerplate (`android.test.runner`) or legacy trivia (`org.apache.http.legacy`) — the value is screen completeness, not a real finding. Revisit if a concrete tracker/risk signal ends up needing it |

**R0 remaining:** FR-17, FR-18 (Partial — blocked on `RI-03`, a Pro/R1 item), CE-06 (Backlog),
FR-31, FR-37, FR-44 (Backlog), plus `EN`.
Everything else originally scoped for R0 has shipped — see [shipped.md](shipped.md).

---

## Part 2 — Pro Tier

The three pillars are the *story we sell*. Below they're decomposed into the **functional areas**
the work actually lands in — each maps to one module boundary. Areas are identified by their item
ID prefix rather than a letter, so an item keeps its ID if it moves.

| Area                              | Pillar(s) served | Lands in                          |
|-----------------------------------|------------------|------------------------------------|
| `EN` — Entitlement & Billing      | all              | `core:billing`, `core:entitlement` |
| `HI` — Snapshot & History         | 1                | `core:app-history`, `feature:app-history` |
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

**Design doc:** [features/app-history.md](features/app-history.md) — design in progress, covers
FR-31 and HI-01 … HI-18.

Snapshots capture **complete app state**, not permissions alone (`FR-31`), because the deep fields
are where the interesting diffs live: *"v4.2 → v4.3, gained Contacts, signer changed, added an
arm64 native library, grew 40 MB."* A signer change between versions is the single strongest tamper
signal the app can produce, and it is only visible if snapshots carry the certificate.

**Identity is the install, not the version.** Two local builds can share a `versionCode`, so a
snapshot is keyed by `packageName + firstInstallTime + lastUpdateTime`. That key is also what makes
capture cheap: the installed-app list the app already loads on every open carries `lastUpdateTime`,
so detecting what to re-analyse is a set-difference against the store, and steady state is a
handful of apps.

**`lastUpdateTime` covers the APK, not the app's state.** A runtime permission grant, a
disable, or an install-source adoption never moves it — and "this app now has your location" is the
most user-relevant change there is. So state is observed on two tiers: a new timestamp creates a new
install instance (`HI-02`), while the cheap volatile fields are re-observed against the existing one
(`HI-10`).

| ID    | Task                          | Detail                                                                        | Size |
|-------|-------------------------------|--------------------------------------------------------------------------------|------|
| HI-01 | Snapshot store                | Local DB (Room, already used elsewhere in the project) keyed by install instance. Permission sets, component sets, certificates and library lists are **content-addressed** — hashed into per-package tables and referenced by ID, so deleting one app's history is a direct delete rather than a garbage-collection sweep over what other apps might still reference. A snapshot row is scalars plus foreign keys, and an update that changes nothing but version and size reuses every set and costs almost nothing. See [the capture schema doc](../technical/app-history-capture-schema.md) | M |
| HI-02 | Delta capture on open         | Set-difference of the installed list against the store; full analysis runs only for changed packages. Off the critical path, never blocking first frame. Consumer of `FR-31` | S |
| HI-03 | Diff engine                   | **Diffs are computed on read, never stored.** Snapshots are the only source of truth: stored diffs would duplicate derivable data, freeze history in whatever format shipped first, and still not answer arbitrary-pair comparison. Covers version, permissions requested, certificate, components, native libraries, install source, flags, APK size. Data and cache size are not captured at all — device/runtime state, not app content, same reason usage stats and last-used time are excluded; only APK/total size is captured and diffed. Intent filters are captured but not diffed in R1: the volume is pure noise in a change log, and `HI-03` being a read-time projection means a later decision to diff them applies retroactively to all existing history | M |
| HI-04 | Retention & pruning           | Content addressing makes unchanged data nearly free, so pruning caps **whole generations per app** rather than hollowing rows out. A change row exists if and only if both its snapshots exist, so the timeline shortens at the far end instead of developing phantom entries | S |
| HI-05 | First-run baseline build      | The whole device has no history on day one. Chunked, cancellable, resumable, with a real progress state ("analysing 214 apps · 62 done") rather than a silent freeze. The only expensive capture in the design — see `OQ-07` | M |
| HI-21 | Pre-history seeding           | `firstInstallTime` and `lastUpdateTime` are history the platform recorded before this feature existed, so the timeline is populated and truthful on day one — "Signal · updated 2 days ago". These rows state *when*, never *what*, are never counted as observed changes, and must be visually distinct from locked stubs: a stub means "we have this, unlock it", a pre-history row means "nobody has this". Rendering them alike would make the paywall lie on the user's first launch | S |
| HI-20 | Capture integrity             | A partial capture is left uncomparable — a failed section simply has no hash to compare, with no stored "partial" marker needed — and is not retried until the package's identity next changes. Diffing an incomplete snapshot as if it were complete **fabricates** changes ("removed 12 permissions" because a read failed), which for a security-positioned app is a worse defect than any gating bug | S |
| HI-06 | Free sample card              | The latest change on the device, in full. A card above the list rather than an unlocked row, so a newer change never re-locks something already read | S |
| HI-07 | Full change log (Pro)         | All apps, chronological, filterable by change type                            | M    |
| HI-08 | Per-app timeline (Pro)        |                                                                                | S    |
| HI-09 | New-changes badge             |                                                                                | XS   |
| HI-10 | Runtime-state observation     | Enabled state and install source change without `lastUpdateTime` moving. Recorded against the existing install instance, not as a new one. Permission grant state is deliberately not tracked — device/runtime state, not a fact about the app | S |
| HI-11 | Background change detection   | Detect changes while the app is closed. Broadcast-driven fast path plus periodic reconciliation, not an either/or — see `OQ-06` | M |
| HI-12 | Change notifications          | Only the standout set: newly *requested* dangerous permission, certificate change, large APK size jump, install-source change, app became debuggable. Never routine updates — an alert per Play auto-update gets muted in a week. Certificate changes fire for everyone; the rest are Pro | S |
| HI-13 | Notification settings         | Which alert types fire, per-app mute, off until opted into. The opt-in is placed on the first-run screen (`HI-21`) — the one moment the user is looking at the feature, understands it, and has no history competing for attention | S |
| HI-14 | What Changed tab              | Third top-level slot, freed by `CE-05`. Digest since last open on top ("4 updates, 2 new apps") with the `HI-18` alerts called out; per-app history list with version counts below | M |
| HI-15 | App-detail entry point        | This app's timeline, opened from its detail hub                                | XS   |
| HI-16 | Drive backup (Pro)            | History does not survive a reinstall. User-triggered upload of the store to their own Google Drive — see `OQ-08` | M |
| HI-17 | Restore & reconcile           | A restored store describes a device that no longer matches. Merge by install instance and mark the gap rather than overwriting or silently dropping | M |
| HI-18 | Change-significance rules     | The one predicate behind both `HI-12`'s notifications and `HI-14`'s highlighted section, so the two can never disagree on screen. Also owns the free/locked classification, for the same reason. A fixed list in R1 that folds into `RI` when that area lands — it must not grow into a second rules engine | S |
| HI-19 | Locked stub row + unlock path | The stub component and the surface it leads to. Labels are **projected from the snapshot pair at read time** — which set IDs differ — so a stub can never promise content the store cannot deliver. The gate fails open. Uses `EN-04`'s pattern; the copy quotes counted totals ("214 apps · 1,847 changes"), not claims | S |

**Gating.** Capture runs for everyone, always, on every app — so the purchase is **retroactive**:
the unlock is not "we start recording now" but *"eight months of history across 214 apps, readable
immediately."* Almost nothing else in the app can offer that, and the offer strengthens every month
a free user doesn't take it.

The free sample is **derived, never chosen**. Anything the user selects is a rotating key to the
whole product, and anything time-boxed expires while the feature is still empty. So every change row
renders as a **locked stub** — version transition plus the category labels it touched
(`Permissions · Components · Size`), never the values — with exactly four rule-based exceptions,
none of which need stored state:

| Free                                    | Why                                                                     |
|-----------------------------------------|-------------------------------------------------------------------------|
| Capture, the tab, the digest counts     | The substrate, and counts are not the product                            |
| `HI-06` card — latest change on the device, in full | The sample, evaluated live. A card above the list, not an unlocked row, so nothing ever re-locks |
| Every certificate change, every app     | Rare, so it costs almost no revenue; the strongest signal, so it makes free-tier silence trustworthy; and the best conversion trigger there is |
| First-seen rows, and `HI-10` observations | Nothing to hide in an origin marker, and an enabled-state or install-source flip is a cheap system fact, not the analysis being sold |

Everything else is Pro: the content behind every stub, the per-app timeline (`HI-08`), the cross-app
log (`HI-07`), arbitrary version comparison, notifications other than certificate changes (`HI-12`),
and Drive backup (`HI-16`, `HI-17`).

**Area total: ~17–20 days**

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

**Suggested build order:** `EN` → `HI` → `RI` → `TR` → `RP` →
`BX` → `CP`. `HI` is no longer the cheap pillar it looked like, but it stays first because its value
depends on how long collection has been running — every week it ships later is a week of history
nobody has. `RI` and `TR` can run in parallel — they meet only at `RP`.

---

## Optional / Backlog within Pro

| ID    | Item                              | Notes                                                                            |
|-------|-----------------------------------|-----------------------------------------------------------------------------------|
| OP-01 | APK diff / compare tool           | Two arbitrary APK files, reusing `HI-03`'s diff engine. Worth building only if wired into `RI` — flag what changed as *risky*, not raw |
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
| R0 | Free Rework     | Remaining: FR-17, FR-18, CE-06, FR-31, FR-37, FR-44, plus `EN` — rest shipped, see [shipped.md](shipped.md) | ~7–8 weeks     | Week 8     |
| R1 | Pro Launch      | `HI`, `RI`, `TR`, `RP`, `CP`                                                             | ~7.5–8.5 weeks | Week 17    |
| R2 | Bulk Tools      | `BX`                                                                                     | ~1.5–2 weeks   | Week 19    |
| R3 | Optional polish | OP-01 … OP-03                                                                            | as-needed      | Ongoing    |

**Committed roadmap (R0–R2): ~17–19 weeks**

---

## Open Questions

| ID    | Question                                                                                                  |
|-------|------------------------------------------------------------------------------------------------------------|
| OQ-01 | Resolved — Room is already a dependency (`core:user-preferences`, `core:ai-insights`), so `HI-01` needs no new local-DB sign-off. The dependency question that remains is `WorkManager`, needed only for `HI-11`'s periodic-worker follow-up — see `OQ-03` |
| OQ-02 | `EN-01` needs the Play Billing library — same sign-off                                                     |
| OQ-03 | `BX-01` background scanning: coroutine + foreground service, or WorkManager (another dependency)?          |
| OQ-04 | `CP-02` remote refresh needs a hosting decision, and that decision sets the ongoing cost floor for Pro     |
| OQ-05 | `EX-06` DEX scanning on-device has a real performance and battery cost on large APKs — needs a spike before `TR` is committed |
| OQ-06 | `HI-11`: resolved in shape — `PackageChangesObserverImpl` already uses a context-registered receiver (manifest-declared implicit package broadcasts have been blocked since API 26 regardless), which only survives while the process is alive. Design is a fast broadcast path plus a periodic-worker reconciliation backstop for when it isn't — see [the capture schema doc](../technical/app-history-capture-schema.md#triggers). `WorkManager` sign-off (`OQ-03`) is the only remaining gap |
| OQ-07 | `HI-05`: what does a full-detail baseline actually cost on a 300-app device? Native libraries, signing schemes and split-manifest parsing are each file reads per app, which is why `core:apps` fetches them lazily today. Needs a measurement before `HI` is committed — same gate as `OQ-05` |
| OQ-08 | `HI-16`: Google Drive REST plus sign-in (user-triggered, restorable, Pro-gateable, new dependency) or Android Auto Backup (free, no code, but invisible, size-capped and impossible to gate or trigger)? |
