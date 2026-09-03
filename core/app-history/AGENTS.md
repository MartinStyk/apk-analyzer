# core:app-history Module

## Purpose and Boundary

Room-backed storage for full-state app history snapshots — one row per captured install instance,
enough to reconstruct the existing app-detail screens against a past point in time. The package is
`sk.styk.martin.apkanalyzer.core.apphistory`.

**Status:** capture is implemented and running (schema, pipeline, both triggers). Not yet built: the
diff engine (`HI-03`), any UI (`HI-06`/`HI-08`/`HI-14`), retention/pruning (`HI-04`), Drive backup
(`HI-16`/`HI-17`), the `HI-10` runtime-state (enabled/install-source) tier, and periodic `WorkManager`
reconciliation (today's reconciliation runs once per app process start only). See
[`docs/app/technical/app-history-capture-schema.md`](../../docs/app/technical/app-history-capture-schema.md)
for the full design — read it before touching this module; it is the source of truth for schema and
capture semantics, not this file.

## Package Map

Two domain families, each in its own subpackage — schema/persistence versus the capture pipeline
that populates it:

* `storage/` — three `@Dao` interfaces, one per concern, each with its own query-result types and its
  own file: `AppHistoryGateDao` (gate-check reads, used only by the capture pipeline),
  `AppHistoryWriteDao` (the insert path), `AppHistoryReadDao` (snapshot content reads — see
  [Reading a Snapshot](#reading-a-snapshot); not consumed by anything yet). Plus `AppHistoryDatabase`
  (`apkanalyzer.room` convention plugin, exposing all three DAOs) and `storage/di/AppHistoryStorageModule`.
  A single combined `AppHistoryDao` used to hold all three — split apart because gate-checking,
  writing, and reading are genuinely different callers with different needs (today's only real
  consumer, the capture repository, injects the gate and write DAOs but never the read one), not
  because Room requires it — Room happily supports one `@Dao` per database too.
  `storage/entity/` holds the two `@Entity` classes, `AppHistorySnapshotEntity` and
  `AppHistoryBlobEntity` (+ its `SectionType` column enum).
* `capture/` — the pipeline and its two triggers:
  * `AppHistoryCaptureRepository` (+ `Impl`) — *how* to capture: `reconcile(packageName)` and
    `reconcileAll()`.
  * `AppHistoryCaptureScheduler` (+ `AppHistoryCaptureSchedulerImpl`) — *when* to capture. The `Impl`
    also implements `DefaultLifecycleObserver`; its `onCreate(owner)` calls `start()`, which launches
    the reconciliation sweep and starts collecting the fast-path broadcast flow. See
    [Triggers](#triggers) for why `onCreate` and not `onStart`.
  * `capture/snapshot/` — the wire DTOs and mappers. See [DTO Boundary](#dto-boundary-not-a-detail).
  * `capture/di/AppHistoryCaptureModule` — binds `AppHistoryCaptureRepository`, binds
    `AppHistoryCaptureScheduler`, and separately binds the same `AppHistoryCaptureSchedulerImpl`
    `@IntoSet` as `DefaultLifecycleObserver` to force its construction at app launch (three `@Binds`
    methods over two interfaces, one underlying singleton).

Every type in both subpackages stays `internal` — nothing outside this module reads Room or
wire-format types, or calls the scheduler/repository, directly today.

## Schema

* `AppHistorySnapshotEntity` (`app_history_snapshot`) — one append-only row per captured install
  instance. Identity is `packageName` + `firstInstallTime` + `lastUpdateTime`, not version code, and
  that identity is enforced as a `unique` index, not just implied by the capture gate — the
  surrogate `id` stays the `@PrimaryKey` (a single-column handle `AppHistoryReadDao` and future
  history-list UI can address a snapshot by, instead of a three-column composite), but the unique
  index means a bug that ever bypassed `AppHistoryCaptureRepositoryImpl`'s per-package `Mutex` +
  gate check surfaces as a loud `insertSnapshot` `ABORT` failure rather than a silent duplicate row.
  Component/permission/signing/etc. sections are not inlined here; each is a hash column pointing
  into `app_history_blob`. A hash column is `null` if and only if that section's extraction failed.
* `AppHistoryBlobEntity` (`app_history_blob`) — content-addressed JSON, keyed by
  `(packageName, hash)`. Scoped per package, not globally shared, so deleting one app's history is
  two direct deletes with no reference-counting or GC sweep needed.
* `AppHistoryGateDao` — the two gate-check queries (single package, and all packages in one round
  trip). `AppHistoryWriteDao` — `insertSnapshotWithBlobs` (bulk `insertBlobs` with
  `OnConflictStrategy.IGNORE` — a blob's key is content-derived and legitimately repeats — then
  `insertSnapshot` with `OnConflictStrategy.ABORT`, both in one `@Transaction`). No mutate-in-place
  query exists; every write is an insert.
  `insertSnapshot`'s `ABORT` is deliberately explicit, not just Room's default left implicit: unlike
  the blob table, neither a surrogate-`id` collision nor a natural-key collision can legitimately
  happen. `AppHistorySnapshotEntity.id` is `@PrimaryKey(autoGenerate = true) = 0`, and every
  construction site leaves it at that default, which Room translates to `NULL` (SQLite assigns a
  fresh rowid); the natural key's uniqueness is what the capture gate exists to guarantee. Either
  kind of collision would mean a stale entity got reused or the gate got bypassed, and that should
  crash loudly, not silently corrupt append-only history the way `IGNORE` would.

`AppHistoryDatabase`/`AppHistoryStorageModule` follow the same shape as `core:user-preferences`'s
`UserPreferencesDatabase` (`exportSchema = false`, no destructive-migration fallback — a schema
change needs a real `Migration`; this hasn't shipped yet, so schema iteration during development
just means wiping local app data, not writing one).

## Reading a Snapshot

`AppHistoryReadDao.snapshotWithSections(id)` resolves one snapshot's full content — the scalar row plus
all eleven sections' JSON — as a single flat `@Query`, no `@Relation` and no `@Transaction` needed:
a single `SELECT` is already atomic, so there's nothing to wrap.

The read shape deliberately mirrors the write shape: `AppHistorySnapshot` has one
nullable `String` content property per section (`activitiesContent`, `signingContent`, ...),
matching `AppHistorySnapshotEntity`'s own `activitiesHash`/`signingHash`/... hash columns one for
one. The query gets there with eleven `LEFT JOIN`s against `app_history_blob` — the same table
aliased once per section, each joined on `(packageName, <that section's hash column>)` — plus one
`s.*` to pull in every scalar column via `@Embedded`. `LEFT JOIN` (not `INNER`) is what makes a
`NULL` hash column resolve to a `NULL` content column rather than dropping the row: `pb.hash = NULL`
is never true in SQL, so that join side simply finds no match. An earlier version of this read used
a `@DatabaseView` with an eleven-branch `UNION ALL` collapsed through `@Relation` into a
`List<AppHistoryResolvedSectionView>` — normalized and reusable, but it made every caller search the
list by `sectionType` to get one section, out of step with how the entity itself is shaped. Named
columns read better for the shape this data actually has: fetch one snapshot, look at its named
fields. Verified on-device against 111 real captured snapshots with zero partial-capture failures —
every named column resolved to the correct package's real content.

Content stays JSON `String` here too — decoding into `capture/snapshot/` DTOs is a future reader's
job (there is none yet); this module's contract ends at handing back the stored bytes correctly.

## DTO Boundary — Not a Detail

`core:apps` domain types (`Activity`, `Certificate`, `Permissions`, ...) are **never** `@Serializable`
and never serialized directly into a blob. `core:app-history` defines its own mirror types in
`capture/snapshot/` (one `@Serializable internal` DTO + a `toSnapshot()` mapper per captured section)
and maps to them at capture time.

This was a deliberate correction mid-implementation, not the original design. Serializing the live
domain model straight into permanent storage means an ordinary `core:apps` change unrelated to
history — renaming or retyping a captured field — silently breaks deserialization of every historical
blob written before the change. For a security-positioned app, corrupting old data silently is worse
than the alternative's real cost: a field added to a domain type isn't captured until someone updates
the matching DTO + mapper, which is a visible compile-time/code-review gap, not silent drift. Nested
enums in the DTOs are stored as their `.name` string rather than mirrored as DTO enums, to keep file
count down — lossless either way since nothing decodes this data yet (no diff engine).

The `kotlin-serialization` plugin and every `@Serializable` annotation are scoped to this module
alone — `core:apps`/`core:common` carry no serialization dependency or annotations because of this
module's needs.

## Capture Pipeline

`AppHistoryCaptureRepositoryImpl` implements the schema doc's Capture Gate and Capture Pipeline
sections exactly:

1. Resolve the package's current identity — `InstalledApp` from `InstalledAppsRepository` (already
   cached, so this is cheap even on the fast path), never a fresh device-wide `PackageManager` scan
   from this module.
2. Compare `lastUpdateTime`/`firstInstallTime` against the latest stored row (`AppHistoryGateDao`'s
   gate queries). Unchanged → return, no further work.
3. Gate open → concurrently call `AppDetailRepository.details()`, `IntentFiltersRepository`,
   `NativeLibrariesRepository`, `SigningSchemeRepository` on `DispatcherProvider.io()`.
   `AppDetailRepository` failure aborts the whole capture. Any of the other three failing
   (`Result.isFailure`) records that section's hash as `null` and keeps the rest — including hashing
   the legitimate `signingSchemeVersions` inner-`null` "structurally ambiguous" result, so a `null`
   hash column only ever means a genuine extraction failure, never a real answer.
4. Map each section to its `capture/snapshot/` DTO, encode with one shared `Json` instance, hash with
   `DigestManager.sha256Digest` (`core:common/digest`).
5. `INSERT OR IGNORE` each blob, then insert one new snapshot row. Always a new row, never an update.

**Observability.** `reconcileAll()` runs inside a `PerformanceTracker` trace (`app_history_reconcile`:
`appCount`, `captured_count`, `failed_count` attributes, outcome `Degraded` if any per-package
capture failed) and each `capture()` call inside its own (`app_history_capture`: `degraded_sections`
count, outcome `Error`/`Degraded`/`Success`) — matching the `<operation>_load` trace convention
`core:apps` already uses everywhere else. `capture()` itself returns a private `CaptureOutcome`
(`Aborted` / `Completed(degradedSectionCount)`) rather than owning its own trace, since it needs
`coroutineScope` for its `async`/`await` fan-out — making it a `PerformanceTrace` extension function
would shadow that receiver with `CoroutineScope` and force `this@capture` disambiguation everywhere;
the caller (`captureIfGateOpen`) owns the trace instead. `reconcileAll()`'s own started/finished
`Logger.i` pair (`InstalledAppsRepositoryImpl.loadAllApps()`'s same convention) only logs "finished"
on genuine completion — a thrown exception skips it, and the scheduler's existing `onFailure` warning
covers that case instead.

**Concurrent-capture race.** Reconciliation and the fast path run as two independent coroutines (see
[Triggers](#triggers)) and can both target the same package — e.g. a change broadcast for a package
arrives while reconciliation's sequential sweep hasn't reached it yet. Both would read the gate
before either writes, both see "changed," and both would insert a snapshot row with identical
`packageName`/`firstInstallTime`/`lastUpdateTime` were the gate-check-then-write not serialized.
`AppHistoryCaptureRepositoryImpl` guards this with a per-package `Mutex`
(`ConcurrentHashMap<PackageName, Mutex>`, `computeIfAbsent`), matching
`AppAiDescriptionRepositoryImpl`'s (`core:ai-insights`) per-key coalescing for the same class of
problem — one `Mutex` per package rather than one global lock, so a slow reconciliation sweep never
blocks the fast path from handling an unrelated package. `captureIfGateOpen` (the single place that
both `reconcile` and `reconcileAll` route through) does the gate re-check *inside* the lock:
the loser of the race re-checks after acquiring it, sees the winner's just-written row, and returns
without capturing — no wasted work beyond the lock wait, no duplicate row.
`reconcileAll`'s own batched gate comparison (`latestGateTimestampsForAllPackages`, one round trip)
is unaffected and stays the cheap first-pass filter it was designed to be — `captureIfGateOpen`'s
authoritative single-package re-check only runs for packages that pass it, not the full sweep.

## Triggers

Both triggers live in `AppHistoryCaptureSchedulerImpl.start()`, called once from its
`onCreate(owner)` override:

* **Reconciliation** — `reconcileAll()` sweeps every installed app
  (`InstalledAppsRepository.awaitFullyEnrichedApps()`) through the batched gate query. This is what
  guarantees completeness; broadcasts missed while the process was dead are otherwise lost.
* **Fast path** — collects `PackageChangesObserver.observe()` and calls `reconcile` on
  `Added`/`Replaced`, skipping `Removed` (reconciliation only ever visits currently-installed
  packages, so a removal needs no capture-side handling at all).

`start()` runs from `onCreate`, not `onStart` — `onStart` fires on every foreground return, which is
correct for `UsageStatsRepositoryImpl`'s genuinely volatile data but wrong here: reconciliation must
run once per process, and the fast-path collector must subscribe exactly once, not resubscribe (and
duplicate) on every resume. `onCreate` fires once, when `ProcessLifecycleOwner` reaches `CREATED` —
Lifecycle dispatches the backlog of already-passed states to an observer added slightly late, so this
fires reliably even though `ApkAnalyzer.onCreate()` registers the observer set itself. The
`@IntoSet DefaultLifecycleObserver` binding exists solely so that registration forces Hilt to
construct this singleton at app launch in the first place — nothing else in the app injects
`AppHistoryCaptureScheduler`/`AppHistoryCaptureRepository` directly, so without that forced
construction capture would never run. Periodic `WorkManager` reconciliation (covering long stretches
where the app is never opened) is a deferred follow-up — `androidx.work` is not yet a dependency.

`start()` guards itself with an `AtomicBoolean` so a second call is a no-op: `onCreate` is the only
caller today, but `start()` is also reachable through the separately bound `AppHistoryCaptureScheduler`
interface, and the delayed reconciliation launch plus the flow subscription are not otherwise
idempotent — a second call would launch a second reconciliation sweep and double-subscribe to the
fast path.

`PackageChangesObserver` (`core:apps`) surfaces the changed package name and
`PackageChangeAction` (`Added`/`Removed`/`Replaced`) parsed from the broadcast `Intent`; this module
was the reason that observer was extended off a bare `Flow<Unit>`.

## Module Wiring

`app/build.gradle.kts` depends on this module directly (`implementation(projects.core.appHistory)`)
purely to pull its Hilt bindings into the graph — no other module (feature or core) depends on
`core:app-history` today, so without that direct dependency Hilt would never see this module's
`@InstallIn(SingletonComponent::class)` modules and capture would silently never run. Keep that
dependency even if it looks unused from `app`'s own Kotlin code.
