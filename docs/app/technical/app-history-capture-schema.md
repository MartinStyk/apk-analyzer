# App History Capture Schema

**Roadmap:** [FR-31](../product/roadmap.md#17-invisible-infrastructure),
[HI-01, HI-02, HI-20](../product/roadmap.md#hi--snapshot--history-pillar-1--what-changed) — see
[app-history.md](../product/features/app-history.md) for the product design this implements.
**Status:** Implemented. `core:app-history` exists; the schema, capture pipeline, and both triggers
(reconciliation on process start, fast-path broadcast) are built and running — see
[`core/app-history/AGENTS.md`](../../../core/app-history/AGENTS.md) for the as-built module reference.
Still not built: the diff engine (`HI-03`), UI (`HI-08`/`HI-14`), retention (`HI-04`), backup
(`HI-16`/`HI-17`), the `HI-10` runtime-state tier, and periodic `WorkManager` reconciliation.
**Scope:** The on-device Room schema for full-state app history snapshots, what is and isn't
captured, the change-detection gate, and the two capture triggers. Does not cover the diff engine
(`HI-03`), UI (`HI-08`/`HI-14`), retention (`HI-04`), or backup (`HI-16`/`HI-17`).

**Implementation note (post-agreement correction):** this doc's "Why JSON Content" section below
still describes the *originally agreed* design — serializing the `core:apps` domain models
(`Activity`, `Certificate`, ...) directly. That was changed during implementation: those models are
never made `@Serializable`, because doing so would let an ordinary `core:apps` rename/retype silently
break deserialization of historical blobs. `core:app-history` instead defines its own mirror DTOs
(`capture/snapshot/`) and maps to them at capture time — see
[`core/app-history/AGENTS.md`](../../../core/app-history/AGENTS.md#dto-boundary--not-a-detail) for the
full rationale. The content-addressing, per-package scoping, and "JSON absorbs a new field for free"
arguments below are unaffected; only *which* type gets serialized changed.

## Reconstruction Target

A snapshot must be enough to render the existing app-detail screens against a past point in time,
per `app-history.md`'s "state one tap down" requirement. That target is exactly:

* `AppDetail` (`core:apps`): `info`, `signing`, `activities`, `services`, `contentProviders`,
  `receivers`, `permissions`, `features`.
* The three sections `AppDetail` deliberately excludes because they're expensive and only one
  screen each needs them live: intent filters (`IntentFiltersRepository`), native libraries
  (`NativeLibrariesRepository`), signing scheme (`SigningSchemeRepository`). History captures all
  three unconditionally, device-wide, reusing these repositories rather than re-implementing
  extraction.

**Explicitly excluded**, all for the same reason `AppDetail`'s own cache boundary excludes
device-dependent facts (`core/apps/AGENTS.md`):

* Permission grant state. Originally scoped as an `HI-10` observation; dropped for R1 — see
  [Changes to the Product Design](#changes-to-the-product-design).
* Anything already excluded from live `AppDetail`: device-feature availability, usage stats,
  storage stats, last-used time. This specifically includes `AppInfo.totalSize` — despite living on
  `AppInfo` alongside genuine APK facts, it's populated from `StorageStatsRepository.queryTotalSize()`,
  which sums `appBytes + dataBytes + cacheBytes` (`StorageStatsRepositoryImpl.queryPackageSize`) — the
  same device/runtime cache-and-data size this section already excludes, just reachable through a
  field that looks like an APK fact. Only `AppInfo.apkSize` (base + split APK file sizes) is captured.
* Filesystem paths — `AppInfo.apkDirectory`, `AppInfo.dataDirectory`,
  `InstalledSplitApk.filePath`. Install-instance plumbing, not app content; would read as a
  spurious change across a device migration for zero product value.

## Why Snapshots, Not Event-Sourcing

Matches `app-history.md`'s "Snapshots are the only thing stored": diffs are computed on read,
never persisted, because arbitrary version comparison and "full detail at this version" both need
complete state at an arbitrary point, which a chain of deltas can't reconstruct without replay.
This repo has no test infrastructure, so replay-based reconstruction (ordering-dependent,
silently wrong if buggy) is avoided in favor of full snapshots deduplicated by content hash —
correctness by construction, not by careful replay logic.

## Identity: `packageName` + `firstInstallTime` + `lastUpdateTime`

A snapshot's identity is the install instance, not the version (`app-history.md`, "Identity is the
install, not the version") — two local builds can share a `versionCode`, and an uninstall followed
by a reinstall must show as a break in the timeline, not a continuous line.

`lastUpdateTime` is always `>= firstInstallTime`, and a reinstall bumps both to the same new
moment — there's no real scenario where `firstInstallTime` changes without `lastUpdateTime` also
changing. No synthetic "latest APK change" field is needed; the two raw values are stored and
compared as-is.

`deviceId` (read live from `Settings.Secure.ANDROID_ID`, never a self-generated persisted value —
a persisted UUID would itself be swept up by app-data backup/restore and defeat the purpose) is
stored per snapshot so a future timeline renderer can distinguish "this app was updated" from "this
history continued on a new device," without changing the gate itself.

Not speculative on `HI-16`/`HI-17` (Drive backup, gated on unresolved `OQ-08`) specifically: Android's
own Auto Backup can already restore this app's private data — including the Room DB itself — to a new
device today, with zero product work on our part and independent of whether Drive backup ever ships.
`deviceId` protects against that platform behavior existing now, not against a future feature that
might not land.

## Why JSON Content, Not a Normalized Relational Schema

Each blob's `content` is a serialized value (kotlinx.serialization JSON, matching this project's
`@Serializable` convention for models), not a real relational table per section with typed columns.
Considered and rejected in favor of it: one table per section (`app_history_activity`,
`app_history_certificate`, ...) with columns mirroring each domain model, joined to snapshots via a
grouping/junction table.

* **The captured domain models aren't owned by this module.** `Activity`, `Certificate`,
  `PermissionDetails`, `ComponentIntentFilter`, `NativeLibraryFile` all live in `core:apps` and
  evolve on its schedule. `core:user-preferences/AGENTS.md` requires a `Migration` for every schema
  change — fine for one or two small, stable entities, but a real ongoing tax across nine-plus
  actively-evolving domain models this module doesn't control. JSON absorbs a new field for free;
  old rows simply don't have it, the same way `@Serializable` already defaults missing fields.
* **Nothing here ever queries into the content with SQL.** Every read path in `app-history.md` — the
  stub label, the diff detail screen, "since you installed it" — loads one section's content for one
  snapshot (or two, to diff) and works with it as a deserialized Kotlin object. Row-level SQL
  filtering into individual fields (`WHERE isExported = 1`) is not a capability anything here needs.
* **A relational version wouldn't avoid content-addressing anyway.** To get the same dedup (don't
  re-store an unchanged activity list on every version that doesn't touch it), a normalized schema
  still needs some way to decide "does this exact set of child rows already exist" before inserting —
  which converges back to hashing the set's content, just with nine extra tables and their
  join/grouping logic on top, for a storage-granularity benefit that doesn't matter at the data
  volumes already established here (low single-digit MB).

The real cost: a raw SQLite browse of `app_history_blob` shows opaque JSON text, not scannable
columns — genuinely less convenient in the Database Inspector during development. That's a
dev-time-convenience cost, not a correctness or performance one.

## Schema

### `app_history_snapshot`

One row per captured install instance/version. Append-only — never updated in place.

```kotlin
@Entity(
    tableName = "app_history_snapshot",
    indices = [Index(value = ["packageName", "lastUpdateTime", "firstInstallTime"])],
)
internal data class AppHistorySnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val deviceId: String,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val applicationName: String,
    val processName: String?,
    val versionCode: Long,
    val versionName: String?,
    val isSystemApp: Boolean,
    val isDebuggable: Boolean,
    val allowsBackup: Boolean,
    val usesCleartextTraffic: Boolean,
    val uid: Int?,
    val sharedUserId: String?,
    val description: String?,
    val installLocation: String,
    val installingPackage: String?,
    val initiatingPackage: String?,
    val originatingPackage: String?,
    val apkSize: Long,
    val targetSdkVersion: Int?,
    val minSdkVersion: Int?,
    val permissionsHash: String?,
    val activitiesHash: String?,
    val servicesHash: String?,
    val receiversHash: String?,
    val providersHash: String?,
    val featuresHash: String?,
    val signingHash: String?,
    val intentFiltersHash: String?,
    val nativeLibrariesHash: String?,
    val signingSchemeHash: String?,
    val installedSplitsHash: String?,
)
```

Every column is write-once at insert — see [Removal Handling](#removal-handling) for why no
mutate-in-place field is needed here. A hash column is `null` **if and only if** that section's
extraction failed (`Result.isFailure`) — see [Partial-Capture Marking](#partial-capture-marking) for
why every successful result, including a legitimate "unknown," must still produce a real hash rather
than a bare `null`.

The index deliberately omits `id`: it's a Room `@PrimaryKey(autoGenerate = true) Long`, which maps
to SQLite's `INTEGER PRIMARY KEY` — a direct alias for `rowid`. Every SQLite index on a rowid table
implicitly carries the rowid in each entry, so `id`-ordering within a `packageName` group is
already available for free; declaring it again would be redundant. `lastUpdateTime` and
`firstInstallTime` *do* need to be explicit index columns — they're not implicit — so both queries
below can be satisfied entirely from the index (a covering scan), without a table row lookup.

`uid` is included as a borderline call: OS-assigned rather than declared by the app, and can differ
across reinstalls/devices the same way timestamps can, but it's already surfaced in the live
`AppDetailState` and costs nothing to keep. Revisit if the "declared facts only" line should be
drawn tighter.

### `app_history_blob`

Content-addressed, scoped **per package** — deliberately not globally shared across packages.
Written once per distinct value observed *for that package*; a snapshot whose section content
matches something already stored for the same package just references the existing hash.

```kotlin
@Entity(
    tableName = "app_history_blob",
    primaryKeys = ["packageName", "hash"],
)
internal data class AppHistoryBlobEntity(
    val packageName: String,
    val hash: String,
    val content: String, // JSON
)
```

Insert with `OnConflictStrategy.IGNORE` against the composite `(packageName, hash)` key — no
existence check needed first, since the hash is a deterministic function of the content.

**Why no `sectionType` column.** An earlier version of this schema carried a `SectionType` enum
column on every blob row, and hashed content alone. That's unsound: most apps have several
genuinely empty sections (`"[]"` for `Receivers`, `Providers`, `Features`, ...), all identical
bytes, so they'd hash identically and collide onto the same `(packageName, hash)` row — leaving that
row's own `sectionType` correct for only whichever section won the insert race. On real captured
data, 68% of snapshot rows already had two or more section-hash columns pointing at one shared blob.
Forcing the section type into the hash to keep the column truthful would have turned every one of
those legitimate, storage-saving duplicates into a separately-stored copy — trading away the actual
point of content addressing to keep a column that nothing reads. A blob is just bytes; which
section(s) it represents is already fully answered by whichever snapshot column points at it
(`permissionsHash`, `activitiesHash`, ...), so the row doesn't need its own opinion.

**Why per-package, not global.** A globally-shared blob (keyed by `hash` alone) can be referenced by
any package's snapshots, so deleting one app's history can't safely delete its blobs without first
checking whether some other package's history still depends on them — that needs reference counting
or a garbage-collection sweep to be safe. Scoping to `(packageName, hash)` removes the problem
instead of solving it: deleting one app's entire history is two direct deletes, nothing else to
reason about —

```sql
DELETE FROM app_history_snapshot WHERE packageName = :packageName;
DELETE FROM app_history_blob WHERE packageName = :packageName;
```

— because a blob can never be referenced outside the package that produced it. The cost is losing
*cross-package* dedup (two different apps with byte-identical permission sets no longer share a row)
— always the minor benefit, not the reason for content-addressing. The dedup that actually matters —
the same app not re-storing unchanged permission/component/signing data on every update — is entirely
within one package's own timeline and is fully preserved.

`app-history.md`'s `HI-04` (retention/pruning, capping generations *per app*, already deferred to its
own implementation step) will still need a cleanup sweep when it lands — dropping a package's oldest
snapshots can orphan a blob within that *same* package's remaining rows — but it's a single-package
scan (`DELETE FROM app_history_blob WHERE packageName = ? AND hash NOT IN (SELECT ... FROM
app_history_snapshot WHERE packageName = ?)`), not a global one, and doesn't need full design until
that milestone.

## Field Audit

Every list/nested section, and what's stored in its blob versus derived at read time versus
dropped. "Derive" means the value is a pure function of already-stored fields and would go stale if
cached; recompute it with the same logic the live screens already use.

`Permissions` holds two structurally distinct lists — `defined` (custom permissions this app
declares for others) and `used` (permissions this app requests) — and conflating them is a known
foot-gun in this codebase (`core/ai-insights/AGENTS.md` calls out the same distinction explicitly).
Both are captured; only `used`'s per-entry `isGranted` is dropped.

| Section | Stored as-is | Derived at read time, not stored | Dropped |
|---|---|---|---|
| `permissions` | Both of `Permissions`' lists: `defined` (custom permissions this app declares for others) in full, and `used.map { it.permissionData }` (permissions this app requests — `name`, `PermissionDetails`) with `isGranted` stripped | — | `UsedPermission.isGranted` (grant state) |
| `activities` / `services` / `providers` / `receivers` | Every field on `Activity`, `Service`, `ContentProvider` (incl. `ProviderPathPermission`), `BroadcastReceiver` | — | — |
| `features` | `Feature.Hardware`, `Feature.OpenGlEs` in full | `OpenGlEs.versionName` (already a computed getter, never a stored field) | — |
| `signing` | `Certificate`: `signAlgorithm`, all six MD5/SHA1/SHA256 hashes, `validFrom`, `validUntil`, `serialNumber`, `issuer`/`subject`, `isSelfSigned` (a genuine verification result, not recomputable without the raw cert) | `AppSigning.hasMultipleSigners` (`= currentCertificates.size > 1`), `Certificate.trustLevel` (recomputed from `certificateHashSha256` against the known debug-cert hash), `signatureAlgorithmAssessment`, `formattedSha256Fingerprint` (already computed getters) | — |
| `intentFilters` | `ComponentIntentFilter` in full (`actions`, `categories`, `dataRules`, `uriRelativeGroups`, `priority`, `order`, `isAutoVerify`) | — | — |
| `nativeLibraries` | `NativeLibraryFile` (`name`, `abi`, `size`, `containingApkFileName`) | `abis`, `libraryNames`, `hasNativeCode` (already computed getters on `NativeLibraries`) | — |
| `signingScheme` | `List<SigningSchemeVersion>`, or `null` for unknown (never a guessed list) | — | — |
| `installedSplits` | `InstalledSplitApk`: `fileName`, `size`, `kind`, `qualifier` | — | `filePath` (filesystem path) |

Snapshot-row scalars follow the same rule: `AppInfo.source` (`AppSource`) and `minSdkLabel`/
`targetSdkLabel` are dropped from the row and recomputed from `installingPackage`/`initiatingPackage`
(reassembled into an `InstallSourceChain`) + `isSystemApp`, and from `minSdkVersion`/`targetSdkVersion`
respectively, using the same classifiers `AppDetailRepositoryImpl` already calls — storing them would
cache a pure function's output and go stale if that classification logic is ever corrected.

`AppInfo.installSourceChain` is a real 3-field struct (`installingPackage`, `initiatingPackage`,
`originatingPackage`, each `PackageName?`), not a single value — flattened into three separate nullable
columns above rather than one opaque `String?` column, since `resolveAppInstallSource` (the classifier
`source` is derived from) reads `installingPackage` and `initiatingPackage` independently. Small and
fixed-shape, so plain columns rather than a JSON blob — it doesn't need the content-addressing this doc
uses for list-shaped sections.

## Capture Gate

Two query shapes, both satisfied by the `(packageName, lastUpdateTime, firstInstallTime)` covering
index — never a per-column filter, always a full-row projection compared in Kotlin:

**Single package** (fast path):

```sql
SELECT lastUpdateTime, firstInstallTime FROM app_history_snapshot
WHERE packageName = :packageName
ORDER BY id DESC LIMIT 1
```

**All packages in one round trip** (reconciliation — never loop this per package):

```sql
SELECT packageName, lastUpdateTime, firstInstallTime
FROM app_history_snapshot
WHERE id IN (SELECT MAX(id) FROM app_history_snapshot GROUP BY packageName)
```

Load the batch result into an in-memory `Map<packageName, LatestGate>` once, then compare every
installed app against it purely in memory — zero further DB round trips during the sweep.

Write a new snapshot only if `current.lastUpdateTime != latest.lastUpdateTime ||
current.firstInstallTime != latest.firstInstallTime` (or no prior row exists). Comparing both,
rather than relying on `lastUpdateTime` alone, costs nothing extra and matches the stated identity
key explicitly.

## Capture Pipeline

Runs once per package that the gate says changed:

1. Resolve the package's current `PackageInfo` (existence + `lastUpdateTime`/`firstInstallTime`).
   Not installed → nothing to do (see [Removal Handling](#removal-handling)); this only happens via
   reconciliation racing an uninstall, since reconciliation only visits currently-installed packages.
2. Compare against the gate (above). Unchanged → return, no further work.
3. Gate open → concurrently call `AppDetailRepository.details()`, `IntentFiltersRepository`,
   `NativeLibrariesRepository`, `SigningSchemeRepository` on `DispatcherProvider.io()`.
   `AppDetailRepository` failing aborts the capture entirely — nothing is written, try again next
   time the gate opens. Any of the other three failing (`Result.isFailure`, not merely returning a
   legitimate `null`/empty content) for this one app records that section's hash as absent and
   keeps the rest — same tolerance `core:apps` already applies to a failed manifest split.
4. Serialize each list/nested section to canonical JSON (stable key/field ordering, so identical
   content always hashes identically) and hash it — including a successful-but-legitimately-unknown
   result (`signingScheme`'s inner `null`), so a hash column is only ever absent when extraction
   truly failed. See [Partial-Capture Marking](#partial-capture-marking).
5. `INSERT OR IGNORE` each section's `(packageName, hash, content)` into `app_history_blob`.
6. Insert one new `AppHistorySnapshotEntity` row with the scalars and the section hashes. Always a
   new row, never an update.

## Triggers

Two capture paths, sharing the same gate and pipeline above:

* **Fast path** — `PackageChangesObserver`'s install/update broadcast. Only fires while the process
  is alive (a context-registered receiver can't survive process death), so it's best-effort
  latency, not the correctness guarantee. `PackageChangesObserver` now emits
  `PackageChangeEvent(packageName, action)` (extended for this — its other consumers, which only ever
  needed "something changed, reload everything," were adapted to the new shape) instead of the bare
  `Flow<Unit>` this section originally described as a prerequisite.
* **Reconciliation** — sweeps every installed app (`InstalledAppsRepository`) through the batched
  gate query above. This is what actually guarantees completeness, since install/update broadcasts
  missed while the app was dead are otherwise lost forever. Runs once per app process start today —
  implemented by `AppHistoryCaptureScheduler`, a `DefaultLifecycleObserver` whose `onCreate` (not
  `onStart`, which re-fires on every foreground return) calls `start()`; see
  [`core/app-history/AGENTS.md`](../../../core/app-history/AGENTS.md#triggers). A periodic `WorkManager`
  job is an agreed follow-up once this pipeline is proven, to cover long stretches where the app is
  never opened (`androidx.work` is not yet a dependency — needs adding when that follow-up starts).

## Removal Handling

Per `app-history.md`: uninstalled apps keep their history in its own section, and an uninstall
followed by a reinstall is a break in the chain, drawn as one. Both are fully answerable at read
time from data already being stored, with no capture-side work at all:

* **"Is this package still installed?"** — a live comparison between the distinct `packageName`s
  that have history and the currently-installed set (`InstalledAppsRepository`), evaluated when the
  screen renders. Not a fact to capture or store; it's true only about right now.
* **"Was there a break in the chain?"** — structurally visible without an exact timestamp: if two
  adjacent snapshots for the same package have different `firstInstallTime`, an uninstall happened
  between them. The identity key we already store is sufficient; nothing extra is needed to detect
  the break, only to know precisely when it occurred.

An earlier version of this doc added a mutate-in-place `removedAt` column to capture the exact
uninstall moment. Dropped: nothing in `app-history.md`'s screens actually renders a removal date —
the "No longer installed" section shows a change count, not a date — so it was a field with no
confirmed consumer. `PackageManager` gives no way to reconstruct that date after the fact if it
turns out to be wanted later, so if a concrete "removed 3 weeks ago" requirement shows up, this
needs revisiting then, deliberately, rather than being spent as a speculative field now. Until then,
reconciliation only ever sweeps currently-installed packages (it iterates
`InstalledAppsRepository`'s live list), so a removed package is simply never visited — there is
nothing to detect or write. `ACTION_PACKAGE_REMOVED` in `PackageChangesObserverImpl` needs no new
handler for this design.

## Partial-Capture Marking

`app-history.md`'s `HI-20`: "diffing an incomplete snapshot as though it were complete... fabricates
changes." No schema mechanism is needed for this, and no separate "completeness" concept — the
future diff engine (`HI-03`, not started) is always comparing one section's hash against another
section's hash, never a whole snapshot at once (that's what the "Permissions · Components · Size"
stub label is built from), so a `null` hash on either side simply makes that one section
uncomparable. Ordinary null-handling in that comparison is the entire safeguard; nothing above it
needs naming or storing.

The one thing that has to be correct for that to hold: **a hash column is `null` if and only if that
section's `Result` failed — never for a legitimate result, including "unknown."** This matters for
exactly one section. `SigningSchemeRepository` returns `Result<List<SigningSchemeVersion>?>`, where
the *inner* `null` is a legitimate, successfully-determined "structurally ambiguous, never guess"
answer (`core:apps/AGENTS.md`), not a failure — left as a bare `null` it would be indistinguishable
from a failed capture. Capture must hash that legitimate-unknown result too (a canonical sentinel,
e.g. serialized JSON `null`), so it dedups into its own shared blob like any other repeated value.
Every other section's success type is non-nullable (`Result<AppDetail>`, `Result<NativeLibraries>`,
`Result<Map<ComponentIntentFilterKey, List<ComponentIntentFilter>>>`), so this ambiguity doesn't
exist for them — a successful call always has real content to hash, even an empty list.

Worth being precise about: `permissionsHash` through `installedSplitsHash` (eight of the eleven hash
columns) all derive from the single `AppDetailRepository.details()` call, and a failure there aborts
the whole row (step 3) rather than producing a partial one — so in any row that exists, those eight
are always all-present together, never independently null. Only `intentFiltersHash`,
`nativeLibrariesHash`, and `signingSchemeHash` (each its own separately-fetched repository) can
genuinely vary independently today. Modeling all eleven symmetrically isn't wrong — it's headroom for
`AppDetail` itself later being decomposed the way these three already are — but it's headroom, not a
reflection of today's real failure granularity.

## Open Questions

Not yet resolved — flagging rather than silently deciding:

* **Observation tier scope (`HI-10`).** Grant state is dropped (see below). Enabled-state and
  install-source observations are still notionally in scope per `app-history.md`, but have no
  schema here yet — they don't fit the content-addressed snapshot model (no new version, no full
  re-capture) and need their own lightweight append-only shape.
* **`uid` inclusion.** Flagged above as borderline; kept for now, worth revisiting.
* **Fast-path broadcast trigger not independently verified on-device.** Reconciliation was proven
  end-to-end on an emulator (real snapshot/blob rows, content-addressing visibly working, zero
  partial-capture failures). The fast path's `PackageChangesObserver` wiring compiles clean and
  reuses the same proven `capture()` path, but `ACTION_PACKAGE_REPLACED` is a protected broadcast
  `adb` can't synthesize, and a real reinstall kills the process before its own receiver reacts — so
  this is reasoned, not demonstrated, confidence. Worth a real install/update test on a throwaway
  package before relying on it. A real ordering bug was found (review, not on-device) and fixed on
  this path before it ever shipped: `AppDetailRepository` and its three siblings each independently
  subscribed to `PackageChangesObserver.observe()` to clear their caches, racing the fast path's own
  subscription with no ordering guarantee between them — a lost race could persist stale cached
  content under the new install timestamp, permanently, since the gate only compares timestamps and
  would never revisit it. Fixed by moving invalidation out of `observe()`'s independent collectors
  and into `PackageChangesObserver.runBeforeNotifying`, which runs synchronously inside `onReceive`
  before any event is emitted — see [`core/apps/AGENTS.md`](../../../core/apps/AGENTS.md).

## Changes to the Product Design

`app-history.md` originally scoped permission grant changes as part of the `HI-10` observation
tier. Dropped for R1: grant state is device/runtime state (who currently has what permission),
not a fact about the app, the same distinction that already excludes device-feature availability
from live `AppDetail`. `app-history.md` has been updated to remove grant-state examples and
rationale; enabled-state and install-source observations are unaffected by this change.
