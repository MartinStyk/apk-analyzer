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

---

## Part 1 — Free Tier

| Feature                                                                                 | Status             | Notes                                                                                                        |
|-----------------------------------------------------------------------------------------|--------------------|--------------------------------------------------------------------------------------------------------------|
| Installed app list + search/filter                                                      | Existing           | Entry point                                                                                                  |
| Browse by permission (reverse lookup — which apps have X permission)                    | Existing           | Already live in the app today                                                                                |
| Permissions view                                                                        | Existing           | Core data view                                                                                               |
| Components view                                                                         | Existing           |                                                                                                              |
| App details overview                                                                    | Existing           |                                                                                                              |
| Signature/certificate viewer                                                            | Existing           |                                                                                                              |
| Manifest viewer                                                                         | Existing           |                                                                                                              |
| APK export/share                                                                        | Existing           |                                                                                                              |
| Icon export                                                                             | Existing           |                                                                                                              |
| Exported components view                                                                | New                |                                                                                                              |
| Custom permission audit (app-defined, non-standard permissions)                         | New                |                                                                                                              |
| Dark theme / UI customization                                                           | New                |                                                                                                              |
| Install source filter (Play Store / sideloaded / other), added to the existing app list | New                |                                                                                                              |
| Silent permission-snapshot collection *(invisible infra, no UI)*                        | New                | Records a lightweight snapshot of each app's permissions on every open — feeds the Pro "What Changed" pillar |
| Browse by certificate (which apps share a signing cert)                                 | New, not scheduled | Natural next dimension to add to the existing browse-by screen once there's bandwidth                        |

**R0 build estimate: ~4 weeks**

---

## Part 2 — Pro Tier: Three Pillars

### Pillar 1 — What Changed

Free shows your single most recent permission change; Pro unlocks full history across every app,
indefinitely.

| Task                                    | Detail                              | Size |
|-----------------------------------------|-------------------------------------|------|
| Free "most recent change" teaser card   |                                     | S    |
| Full change history log (Pro)           | All apps, chronological, filterable | M    |
| Per-app timeline drill-down (Pro)       |                                     | S    |
| Empty-state handling for fresh installs |                                     | XS   |
| In-app "new changes" badge/indicator    |                                     | XS   |
| Local history retention/pruning policy  |                                     | XS   |

**Pillar total: ~2.5–3.5 days**

### Pillar 2 — Security & Privacy Report

One consolidated per-app report: permission risk, tracker/SDK detection, and certificate trust
signals.

| Task                                         | Detail                                                                                             | Size |
|----------------------------------------------|----------------------------------------------------------------------------------------------------|------|
| Risk rule set definition & curation          | Dangerous permission combos, severity banding (low/med/high)                                       | M    |
| Risk rule evaluation engine                  |                                                                                                    | S    |
| Tracker/SDK dataset sourcing & license check | Use an existing open dataset rather than build proprietary                                         | XS   |
| Tracker/SDK matcher                          | Scan APK contents against the signature list                                                       | M    |
| Tracker categorization                       | Ad / analytics / crash reporting / etc.                                                            | XS   |
| Certificate & SDK anomaly checks             | Self-signed/debug certs, deprecated target SDK, weak signing algorithm                             | S    |
| Certificate-sharing / clone-signal check     | Flags when installed apps unexpectedly share a signing certificate                                 | S    |
| Consolidated report UI                       | One screen, expandable sections per category                                                       | M    |
| Disclaimer/copy component                    | Risk-indicator language, no "safe"/"verified" claims                                               | XS   |
| Report result caching                        | Invalidate on app update                                                                           | XS   |
| Tracker list refresh mechanism               | Bundled-with-update vs. remote-fetchable — this is the one component with real ongoing upkeep cost | S    |

**Pillar total: ~12.5–17.5 days**

### Pillar 3 — Bulk Tools

Full-device audit and export, for the segment that wants to inspect everything at once rather than
app-by-app.

| Task                           | Detail                                                              | Size |
|--------------------------------|---------------------------------------------------------------------|------|
| Full-device scan orchestration | Background iteration + progress UI, uses the Pillar 2 report engine | M    |
| CSV export                     |                                                                     | XS   |
| PDF export                     |                                                                     | S    |
| JSON export                    |                                                                     | XS   |
| Bulk summary list UI           | Sortable, risk-flagged                                              | S    |
| Share/save flow                |                                                                     | XS   |

**Pillar total: ~6–8 days**

### Shared Infrastructure

| Task                             | Detail                                                                  | Size |
|----------------------------------|-------------------------------------------------------------------------|------|
| Unified paywall/entitlement gate | One reusable "sample free / complete Pro" pattern, used by every pillar | S    |
| Billing integration              | Play Billing, purchase state, restore purchases                         | S    |
| Shared design system pieces      | List rows, severity badges, expandable cards — used across all pillars  | S    |

**Suggested build order:** shared infrastructure first → Pillar 1 (cheapest, ships a fast win) →
Pillar 2 (the heaviest, and Pillar 3 depends on its engine) → Pillar 3.

---

## Optional / Backlog within Pro

| Feature                                             | Notes                                                                                                      |
|-----------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| APK diff/compare tool                               | Worth building only if wired into Pillar 2's risk engine — flag what changed as risky, not just a raw diff |
| Chart entry points into the free permission browser | Tap a chart segment to jump into a filtered view                                                           |
| Custom/exportable report templates                  | Polish on Pillar 2/3 exports                                                                               |
| Native library/ABI inspector                        | Free tier if ever built — same raw-data logic as Exported Components                                       |

---

## Backlog

| Item                                                                             | Notes                                                                                                                     |
|----------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| Subscription tier (real-time alerts, malware/reputation API, cloud sync, digest) | Highest ceiling, real recurring cost, heaviest build. Pillar 1's free-sample tap-through rate is a live signal for demand |
| B2B / team tier                                                                  | Different buyer, different sales motion                                                                                   |
| Affiliate referrals                                                              | Low effort, pick up opportunistically                                                                                     |
| Light ad tier                                                                    | Not the right first move for this audience                                                                                |

---

## Release Plan

| Release                  | Contents                                                                    | Duration                | Cumulative |
|--------------------------|-----------------------------------------------------------------------------|-------------------------|------------|
| **R0 — Free Rework**     | All free-tier features, silent snapshot collection, billing infra           | ~4 weeks                | Week 4     |
| **R1 — Pro Launch**      | Pillar 1 (What Changed), Pillar 2 (Security & Privacy Report), shared infra | ~3.5–4.5 weeks          | Week 8     |
| **R2 — Bulk Tools**      | Pillar 3                                                                    | ~1.5–2 weeks            | Week 10    |
| **R3 — Optional polish** | Diff tool, chart entry points, report templates                             | ~2–2.5 weeks, as-needed | Ongoing    |

**Committed roadmap (R0–R2): ~9–10 weeks**
