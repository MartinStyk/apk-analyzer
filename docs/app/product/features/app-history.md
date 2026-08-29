# App History — What Changed

**Roadmap:** [FR-31](../roadmap.md#17-invisible-infrastructure), [HI-01 … HI-21](../roadmap.md#hi--snapshot--history-pillar-1--what-changed) · R1
**Status:** Design in progress. The capture model, the tab structure and the change-first timeline
are agreed; screen detail below is a first pass and the Open questions are unresolved.
**Scope:** silent full-state capture of every installed app, a diff engine over it, a third
top-level What Changed tab, a per-app timeline reachable from app detail, background detection with
notifications for the changes that stand out, and backup of the store to the user's Google Drive.
Deliberately out: risk interpretation beyond a fixed significance list (that is `RI`),
APK-file-to-APK-file comparison (`OP-01`), and any cloud storage we operate ourselves.

## Why

The app answers *what is this app now*. Almost every question a suspicious person actually has is
temporal: did this change, when did it change, and did it get worse. Nothing in the app answers that
today, and nothing else on the device does either — Play shows a changelog the developer wrote, not
a diff of what the app now asks for. A permission that was added quietly in an update is invisible
by design, and a signing certificate change is invisible even to people who know to look for it.

This is also the one feature whose value is a function of how early it ships. A history view
installed today is worth nothing today and a great deal in six months, which is why capture
(`FR-31`) is free, unconditional, and lands ahead of everything it feeds.

## Audience

The app-detail audience — knows Android, wants the fact and the meaning — plus a second one that
app detail never had: **someone who isn't looking.** A notification reaches a person who did not
open the app and did not ask a question. That sets a much higher bar for what is allowed to fire.
An alert for a routine Play auto-update is not a feature; it is the reason the channel gets muted,
after which none of the real alerts arrive either.

---

## Design

### Identity is the install, not the version

A snapshot is keyed by `packageName + firstInstallTime + lastUpdateTime`, not by version code. Two
local builds routinely share a `versionCode`, and treating them as one entry would collapse exactly
the history a developer testing their own app wants to see.

That key is also what makes capture cheap. `InstalledApp` already carries `lastUpdateTime` and the
installed-app list is already loaded on every open, so deciding what needs re-analysis is a
set-difference against the store rather than a scan.

### State moves on two tiers

`lastUpdateTime` describes the APK. It does not move when the *system* changes something about the
app, and the most user-relevant change there is — a runtime permission grant — is exactly that kind
of change. Detection therefore has two tiers:

| Tier                 | Trigger                                              | Cost                       | Produces                                  |
|----------------------|------------------------------------------------------|----------------------------|-------------------------------------------|
| **Install instance** | `firstInstallTime` or `lastUpdateTime` moved         | Full analysis of that package | A new snapshot                          |
| **Observation**      | Enabled state or install source differ, same instance | Cheap field read | A change recorded against the existing instance |

An observation never creates a version. "Torch Pro's install source changed" belongs on v4.2's row,
not on a fictional v4.3.

Permission grant state is deliberately not tracked. It is device/runtime state — who currently has
which permission — not a fact about the app, the same distinction that already keeps device-feature
availability out of live `AppDetail`. See
[the capture schema doc](../../technical/app-history-capture-schema.md#changes-to-the-product-design).

**Data and cache size are not captured at all.** They're device/runtime state — how much this install
has accumulated on this device, not a fact about the app — the same reason usage stats and last-used
time are excluded. Only APK/total size is captured, and it's the one size fact that's diffed; user
data size would also drift on every open and turn change detection into noise even if it were kept.

### Snapshots are the only thing stored

Diffs are **computed on read, never persisted.** Full snapshots are required regardless — arbitrary
version comparison and "full detail at this version" both need complete state at an arbitrary point,
which consecutive diffs cannot reconstruct — so a stored diff is duplicated derivable data with two
long-term costs: a diff-engine fix would need a migration and a recompute pass instead of repairing
all history for free, and any field added to the diff later would apply only to changes recorded
after that release. Intent filters are the concrete case: R1 doesn't diff them, and a read-time
projection means a decision to start would light up across every user's entire existing history.

Storage is handled by **content addressing** rather than by delta chains. Permission sets, component
sets, certificates and library lists are hashed into per-package tables; a snapshot row is scalars
plus foreign keys into them. An update that changes nothing but `versionCode` and size reuses every
set and costs about a hundred bytes. Scoping the hash tables per package, rather than sharing them
globally, means deleting one app's entire history is a direct delete, not a garbage-collection sweep
over what other apps might still reference. That gives delta-like efficiency without a chain's
fragility — no keyframes, no reconstruct-by-replay, no corruption blast radius.

It also makes the read path naturally tiered, which is exactly what the screens need:

| Question                                   | Cost                        |
|--------------------------------------------|-----------------------------|
| Did permissions change? (stub label, log row) | Compare two set IDs       |
| *What* changed? (the unlocked content)     | Load only the sets that differ |

If the cross-app log ever proves slow, the answer is a derived cache that is explicitly rebuildable —
never a second source of truth.

### The baseline is the zero point

On first run the device has no history, so every one of ~200 apps is technically "new". The baseline
capture is defined to produce **zero** changes and zero alerts — it establishes the origin. The tab
is not empty on that day, though: it is seeded from install and update dates the platform already
recorded (`HI-21`, sketched under [First run](#first-run)). The first *observed* change is the first
thing that happens after the baseline.

This is also the only expensive capture in the design, and the only one that touches every app at
once, so it runs chunked, cancellable and resumable, off the critical path, with visible progress.
Its cost is unmeasured — see `OQ-07`.

### Change-first, with state one tap down

A list of version numbers makes the reader do the diffing. Every row in this feature is therefore a
*change*, not a *state*, following the same rule the app-detail hub already follows: the row answers
"is there anything worth my attention here" before it is tapped.

Full historical state is still reachable, and nearly free: because snapshots hold everything
`AppDetail` holds, the existing app-detail screens can be rendered from a snapshot instead of live
`PackageManager` data. That is one extra data source, not a second set of screens.

### Historical mode degrades explicitly

App detail already has two modes, and [app-detail.md](app-detail.md) requires the APK-file mode to
degrade out loud rather than render empty rows. Historical state is a third mode with the same
obligation: it is labelled *as seen on 2 March*, and must never present a stale fact as a live one.
Anything device-dependent — grant state relative to today, storage totals, last-used — is a fact
about the past or is dropped.

### One predicate behind every alert

The notification (`HI-12`) and the highlighted section of the tab (`HI-14`) are the same claim made
in two places, so they come from one significance rule set (`HI-18`). Two code paths each deciding
"is this worth flagging" will eventually disagree on screen, which is the failure the root
`AGENTS.md` calls out by name. The same rule set owns the free/locked classification below, for
exactly the same reason.

The R1 list is fixed and short:

| Change                                  | Why it stands out                                                     |
|-----------------------------------------|-----------------------------------------------------------------------|
| Signing certificate changed             | The strongest tamper signal the app can produce                       |
| Dangerous permission newly *requested*   | The app is asking for something it never asked for before             |
| Install source changed                  | A Play app that is suddenly sideloaded, or the reverse                |
| Became debuggable                       | Ships as a manifest hygiene finding in `RI-03`; here it is a *change* |
| Large APK size jump                     | Threshold, not a ratio — see Open questions                           |

It stays a list, not an engine. When `RI` lands it owns this judgment and `HI-18` becomes a caller.

### What is free, and how the sample is chosen

Capture runs for everyone, on every app, from the first launch. That makes the purchase
**retroactive** — the unlock is not "we start recording now" but *"eight months of history across
214 apps, readable immediately"* — and it is the strongest commercial argument in the design. The
offer gets better every month a free user declines it.

Two gate designs were rejected before this one:

- **A trial measured in sessions** spends itself during the only period when history is guaranteed
  to be empty. The user meets the feature at its worst, dismisses it, and the wall then lands on
  something they already decided was nothing.
- **A user-chosen sample** ("watch 3 apps free") is a rotating key to the entire product. Locking
  the choice to prevent that is worse than not offering it.

So the sample is **derived, never chosen, and holds no state.** Every change row renders as a
locked stub; four rule-based exceptions open:

| Free                                   | Rule                                    | Why                                                                                   |
|----------------------------------------|-----------------------------------------|----------------------------------------------------------------------------------------|
| The tab, digest counts, change counts  | —                                       | A count and a date are not the product. *What it was* is                              |
| Latest change on the device, in full   | `max(timestamp)`, evaluated live        | The sample (`HI-06`). It is a **card above the list, not an unlocked row** — an unlocked row would silently re-lock when a newer change arrived, and "I could read this yesterday" is worse than never showing it |
| Every certificate change, every app    | Change type                             | Rare, so it costs almost no revenue. Strongest signal, so it makes the free tier's silence trustworthy. And it is the best conversion trigger the product will ever have: the alert lands, the diff opens free, and six locked changes sit underneath it |
| First-seen rows, and `HI-10` observations | Change tier                          | An origin marker has no diff to hide, and an enabled-state or install-source flip is a cheap system fact, not the analysis being sold |
| Pre-history rows (`HI-21`)             | Recorded before tracking started        | There is nothing behind them to sell. Gating them would be charging for data we never captured |

The two-tier capture model does double duty here: an **observation** is the system or the user
changing something, and it is free; an **install-instance diff** is the *app* changing what it asks
for, which is the headline product and is gated.

A stub shows the version transition and the **category labels** it touched, never the values:

```
2 Mar   v312.0 → v315.0
        Permissions · Components · Size            🔒
```

You learn that permissions were touched. You do not learn whether it gained Camera or dropped
something unused — "gained 2 permissions" would already be the answer for the most common case.

Everything behind the stubs is Pro, along with the per-app timeline (`HI-08`), the cross-app log
(`HI-07`), arbitrary version comparison, non-certificate notifications, and Drive backup.

### A stub must not be able to lie

The worst outcome this design can produce is someone paying and unlocking nothing — a stub that
advertised `Permissions · Size` over content the store no longer holds. That is a refund and a
review.

Storing snapshots only makes this **structural rather than disciplinary**. A stub's labels are the
set IDs that differ between two consecutive snapshots, so a stub exists *if and only if* both
snapshots exist, and there is no separate summary that could outlive what it summarises. Pruning
cannot hollow a row out because there is no row to hollow: retention drops whole generations
(`HI-04`), the timeline gets shorter at the far end, and that is honest and visible.

Two rules still need stating, because they cover failures the schema cannot prevent:

**The gate fails open.** Locking is decided by inspecting what is actually there, so anything that
cannot be fully rendered — a migration that dropped a field, a truncated restore — is never offered
as unlockable. The worst case is giving away one row; the alternative is charging for one.

**A partial capture is left uncomparable and never diffed** (`HI-20`). No stored "partial" marker —
a section that failed to capture simply has no hash to compare against, so the diff engine can't
treat it as though it emptied out. This one outranks everything else here: diffing an incomplete
snapshot as though it were complete does not merely under-deliver,
it *fabricates* changes — *"Instagram removed 12 permissions"* because a split failed to read. A
security-positioned app inventing a security event is a worse defect than any gating bug.

One consequence for copy: the unlock surface quotes **counted** totals — *"214 apps · 1,847 changes
recorded"* — read from the store. Copy that can only say what it can count cannot oversell.

---

## Screens

### What Changed tab

The third top-level slot, freed when `feature:permissions` and `feature:statistics` collapsed into
`feature:browse` (see [shipped.md](../shipped.md#11b-browse-by-attribute)).

```
┌ What Changed ─────────────────────┐
│                                   │
│  Since 12 March                   │
│  4 updates · 2 new · 1 removed    │
│                                   │
│  Latest change                    │
│  ┌─────────────────────────────┐  │
│  │ Instagram · 2 Mar           │  │
│  │ +Contacts · +2 activities   │  │
│  │ +8 MB                     › │  │
│  └─────────────────────────────┘  │
│                                   │
│  Needs attention                  │
│  ┌─────────────────────────────┐  │
│  │ ⚠  WhatsApp                 │  │
│  │    Signing certificate      │  │
│  │    changed · 14 Feb       › │  │
│  └─────────────────────────────┘  │
│                                   │
│  All apps                         │
│  Instagram           12 changes › │
│  WhatsApp             8 changes › │
│  Signal               3 changes › │
│                                   │
│  No longer installed              │
│  Torch Pro            5 changes › │
└───────────────────────────────────┘
```

**Latest change** is the free sample (`HI-06`) — always in full, always the most recent thing that
happened on the device. **Needs attention** carries the `HI-18` set; certificate entries open for
everyone, the rest are Pro.

Four rules this screen lives or dies by:

- **The digest window is "since you last opened this tab", with a 7-day floor.** Literally "since
  last app open" empties itself for anyone who opens the app twice in an hour, and a headline
  reading *0 updates · 0 new* makes a working feature look dead.
- **The all-apps list is permanent.** The digest can legitimately be empty; the tab cannot. Sorted
  by most recently changed, never alphabetically — alphabetical ordering buries the only rows
  anyone came for.
- **The count is changes, not snapshots.** Someone sideloading nightly builds should not see
  "247 versions".
- **Uninstalled apps keep their history, in their own section.** *What did that app I deleted
  actually have* is a real question, and the answer survives the app. It does mean this list is
  "apps I have seen", not "apps installed", and the section header is what carries that.

### First run

Day one has zero changes by definition, and this is the first impression for every existing user who
updates into the feature. An empty screen here costs more than it looks: the entire gating model
depends on this tab earning a habit months before it earns money.

It is not actually without data. `firstInstallTime` and `lastUpdateTime` are history the platform
recorded before this feature existed, so the timeline is populated and completely truthful on first
launch (`HI-21`).

```
┌ What Changed ─────────────────────┐
│                                   │
│  Now tracking 214 apps            │
│  Started today                    │
│                                   │
│  You'll be told when an app       │
│  changes its signing certificate, │
│  asks for a new sensitive         │
│  permission, or changes where it  │
│  came from.                       │
│                                   │
│        [ Turn on alerts ]         │
│                                   │
│  Before tracking started          │
│  Signal          updated 2 days ago│
│  Instagram       updated 5 days ago│
│  TikTok        installed 3 weeks ago│
│  Chrome          updated last month│
└───────────────────────────────────┘
```

**Pre-history rows say when, never what** — *"Signal updated 2 days ago, we weren't watching yet"*.
That is the honest form of the tease, and it demonstrates the shape of the feature using the user's
own device on the day they meet it.

**They must not look like locked stubs.** A stub means *we have this, unlock it*; a pre-history row
means *nobody has this*. Rendering them alike would make the paywall lie on the user's first launch,
which is the same defect as a hollow stub in its most damaging possible location. No lock, distinct
treatment, and never counted in a per-app change count — a count only ever reflects observed
changes.

**While the baseline builds**, the screen shows real progress (*"analysing 214 apps · 62 done"*),
non-blocking and resumable across backgrounding.

**The transition out is derived, not stored.** The moment one observed change exists the normal
layout takes over and "Before tracking started" drops below it. On a device with Play auto-updates
that happens within days, so this state is short-lived — but short-lived is not the same as briefly
broken.

### Per-app timeline

Reached from the tab or from the app-detail hub (`HI-15`).

```
┌ Instagram ────────────────────────┐
│  Since you installed it           │
│  +6 permissions · +184 MB         │
│  same signer                   🔒 │
│                                   │
│  2 Mar   v312.0 → v315.0          │
│          Permissions · Components │
│          · Size                🔒 │
│                                   │
│  14 Feb  ⚠ Signing certificate    │
│          changed                ›  │
│                                   │
│  2 Feb   Install source changed   │
│          · Sideloaded           ›  │
│                                   │
│  8 Jan   First seen · v308.0    ›  │
└───────────────────────────────────┘
```

**"Since you installed it"** is pinned at the top and compares the first snapshot to the live
current state. It is one tap, costs nothing beyond the diff engine, and is probably the comparison
most people actually want — which is also why it is Pro.

The unlocked rows here are not exceptions carved for this screen; they fall out of the rules in
[What is free](#what-is-free-and-how-the-sample-is-chosen). A certificate change opens for everyone,
an enabled-state or install-source observation is a cheap system fact rather than analysis, and a
first-seen marker has no diff to withhold.

An uninstall followed by a reinstall is a **break in the chain**, drawn as one — same package, new
`firstInstallTime`, and the timeline says so rather than pretending it is a continuous line.

### Change detail

One change, grouped by category, before → after, with unchanged categories omitted rather than
listed as "no change". Each row follows the established idiom — tap to explain, long-press to copy —
and offers **See full detail at this version**, which opens the app-detail screens rendered from
that snapshot.

### Notification

Fires only for the `HI-18` set, and only from background detection (`HI-11`) — a change found while
the user is looking at the app does not need an interruption. One notification per app, not per
change; several apps collapse into a summary. Tapping opens that app's timeline, not the tab.

**Certificate changes notify everyone.** Every other alert is Pro. A free user's notification
therefore always opens into something readable in full — an alert that leads to a paywall is an
ambush, and this design never produces one.

Off until opted into, with per-type and per-app control (`HI-13`).

---

## Implementation order

1. **Store and capture** — `HI-01`, `HI-02`, `HI-10`, `HI-20`, consuming `FR-31`. No UI at all. This
   ships in the free build as early as possible, per Principle 3: everything below is worth more the
   longer this has been running. Capture integrity (`HI-20`) belongs here rather than later — a
   store that quietly accumulated partial snapshots for six months cannot be repaired afterwards.
2. **Baseline build and first run** — `HI-05`, `HI-21`. The chunked, resumable first-run pass, its
   progress state, and the pre-history timeline seeded from install and update dates. This is the
   first impression for every existing user, so it is not a polish item to be tacked on later.
3. **Diff engine and significance rules** — `HI-03`, `HI-18`. Still no UI; both are pure functions
   over the store and are the substrate for every screen after this.
4. **What Changed tab** — `HI-14`, with the free sample card `HI-06`, the locked stub and its unlock
   path `HI-19`, and the badge `HI-09`.
5. **Timeline, change detail, and the app-detail entry point** — `HI-08`, `HI-15`, including
   pointing the existing app-detail screens at a snapshot source.
6. **Background detection and notifications** — `HI-11`, `HI-12`, `HI-13`. Blocked on `OQ-06`.
7. **Retention and pruning** — `HI-04`, capping whole generations per app. Deliberately late: the
   real growth curve is only observable once steps 1–3 have been running on real devices, and
   content addressing may well push the problem out far enough that the cap is generous.
8. **Backup and restore** — `HI-16`, `HI-17`. Blocked on `OQ-08`.

Steps 1–3 are shippable in the free build before any of the Pro surface exists, and should be.

## Deferred

- **Intent-filter diffing.** Captured, not compared. The volume is enormous and a change log full of
  filter churn buries everything else. Revisit if a concrete signal needs it.
- **Data and cache size diffing.** Drifts on every open; APK size carries the real signal.
- **APK-file-to-APK-file comparison** — `OP-01`. Same diff engine, different entry point, no
  history involved. Worth doing only once `RI` can call a change risky.
- **Risk score over time** — `OP-03`. Needs `RI` to exist first.
- **Cross-device sync.** `HI-16` is a backup, not a sync. Real sync means infrastructure we operate
  and a recurring cost — that is `BL-01` territory.
- **Timelines for APK files.** A picked file has no stable identity to track across opens, and
  `FR-21` was already retired for the same reason.
- **A session-based trial** ("three opens, then locked"). Rejected: it spends itself during the only
  period when history is guaranteed to be empty, so the user meets the feature at its worst and the
  wall lands on something they already dismissed.
- **A user-chosen free sample** ("watch 3 apps"). Rejected: rotating the slots is a key to the whole
  product, and locking the choice to stop that is worse than not offering it.

## Open questions

Carried on the roadmap: `OQ-01` (local DB dependency), `OQ-06` (broadcast versus periodic worker),
`OQ-07` (baseline capture cost), `OQ-08` (Drive versus Auto Backup).

Specific to this design:

| Question                                                                                     | Options |
|----------------------------------------------------------------------------------------------|---------|
| What is a "large" size jump?                                                                 | Absolute threshold, ratio, or both. A ratio flags every small app; an absolute value flags every large one |
| Does a restore merge or replace?                                                             | `HI-17` says merge by install instance, but two devices on the same account produce genuinely conflicting timelines for one package |
| Should the digest count app *updates* that changed nothing observable?                       | An update with an identical snapshot is real to the user ("it updated") but empty to the diff. Counting it keeps the digest honest; hiding it keeps it useful |
| Does a free tab full of locked stubs go stale?                                               | The sample card and certificate alerts are the only things that refresh for a free user. If `EN-06`'s funnel shows the tab dying after the first week, the answer is a second derived free slot (oldest change? first change per app?), not a longer trial |
| Is "Since you installed it" Pro?                                                             | Marked Pro above because it is the most useful single comparison in the feature. But it is also the clearest demonstration of what history *is*, which argues for it being the sample instead of `HI-06`'s latest change |
