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
for the full design, including the entity/DAO schema — read it before touching this module; it is the
source of truth for schema and capture semantics, not this file.

## Package Map

Two domain subpackages: `storage/` (Room schema and DAOs — persistence only, no capture logic) and
`capture/` (the pipeline, its two triggers, the wire DTOs in `capture/snapshot/`, and Hilt bindings
in `capture/di/`). See [Reading a Snapshot](#reading-a-snapshot) for `storage/`'s read path — not
consumed by anything yet — and [Capture Pipeline](#capture-pipeline) / [Triggers](#triggers) for
`capture/`'s. Everything in both subpackages stays `internal`; nothing outside this module reads
Room or wire-format types, or calls the scheduler/repository, directly today.

`storage/` splits gate-checking, writing, and reading into three separate `@Dao` interfaces rather
than one combined DAO — not because Room requires it, but because those are genuinely different
callers with different needs (today's only real consumer, the capture repository, injects the gate
and write DAOs but never the read one).

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
the matching DTO + mapper, which is a visible compile-time/code-review gap, not silent drift. A
polymorphic DTO (`FeatureSnapshot`'s `Hardware`/`OpenGlEs` variants) carries an explicit
`@SerialName` on every variant for the same reason — the default discriminator is the Kotlin class
name, which would tie the wire format to a name nobody would think twice about renaming. Nested
enums, by contrast, are stored as their plain `.name` string rather than mirrored as DTO enums, to
keep file count down — lossless either way since nothing decodes this data yet (no diff engine).

The `kotlin-serialization` plugin and every `@Serializable` annotation are scoped to this module
alone — `core:apps`/`core:common` carry no serialization dependency or annotations because of this
module's needs.

## Reading a Snapshot

`AppHistoryReadDao.snapshotWithSections(id)` resolves one snapshot's full content — the scalar row
plus all eleven sections' JSON — as a single flat `@Query`, no `@Relation` and no `@Transaction`
needed: a single `SELECT` is already atomic. It joins `app_history_blob` once per section (aliased,
each on `(packageName, <that section's hash column>)`) with `LEFT JOIN`, not `INNER` — that's what
makes a `NULL` hash column resolve to a `NULL` content column instead of dropping the row — plus one
`s.*` to pull in every scalar column via `@Embedded`. An earlier version normalized this through a
`@DatabaseView`/`@Relation` into a `List<AppHistoryResolvedSectionView>` callers had to search by
`sectionType`; named columns fit this data's actual shape better — fetch one snapshot, read its
named fields.

Content stays JSON `String` here too — decoding into `capture/snapshot/` DTOs is a future reader's
job (there is none yet); this module's contract ends at handing back the stored bytes correctly.

## Capture Pipeline

`AppHistoryCaptureRepositoryImpl` implements the schema doc's
[Capture Gate](../../docs/app/technical/app-history-capture-schema.md#capture-gate) and
[Capture Pipeline](../../docs/app/technical/app-history-capture-schema.md#capture-pipeline) sections
exactly — read there for the gate/write steps. Two invariants live only in this implementation, not
in the design doc:

**Observability.** `reconcileAll()` and each `capture()` call each run inside their own
`PerformanceTracker` trace (`app_history_reconcile`, `app_history_capture`), matching the
`<operation>_load` convention `core:apps` already uses everywhere. `capture()` itself returns a
private `CaptureOutcome` (`Aborted(cause)` / `Completed(degradedSectionCount)`) rather than owning
its own trace, since it needs `coroutineScope` for its `async`/`await` fan-out — making it a
`PerformanceTrace` extension function would shadow that receiver with `CoroutineScope`; the caller
(`captureIfGateOpen`) owns the trace instead, records `Aborted` as any other capture failure, then
rethrows `cause` after the trace closes — an aborted capture must count as a real failure for both
`reconcile` and `reconcileAll`, not a captured-but-empty result.

**Concurrent-capture race.** Reconciliation and the fast path run as two independent coroutines (see
[Triggers](#triggers)) and can both target the same package — e.g. a change broadcast arrives while
reconciliation's sweep hasn't reached that package yet. Without serialization, both would read the
gate before either writes, both see "changed," and both insert a snapshot row with an identical
natural key. `AppHistoryCaptureRepositoryImpl` guards this with a per-package `Mutex`
(`ConcurrentHashMap<PackageName, Mutex>`, `computeIfAbsent`) — matching
`AppAiDescriptionRepositoryImpl`'s (`core:ai-insights`) per-key coalescing for the same class of
problem — and does the gate re-check *inside* the lock: the loser of the race re-checks after
acquiring it, sees the winner's just-written row, and returns without capturing. `reconcileAll`'s own
batched gate comparison stays the cheap first-pass filter it was designed to be; only the
per-package re-check inside the lock is authoritative.

## Triggers

Both triggers live in `AppHistoryCaptureSchedulerImpl.start()`, called once from its
`onCreate(owner)` override — not `onStart`, which re-fires on every foreground return and would
double-run reconciliation and double-subscribe the fast-path collector. `onCreate` fires once, when
`ProcessLifecycleOwner` reaches `CREATED`; Lifecycle dispatches the backlog of already-passed states
to an observer added slightly late, so this fires reliably even though `ApkAnalyzer.onCreate()`
registers the observer set itself. The `@IntoSet DefaultLifecycleObserver` binding exists solely so
that registration forces Hilt to construct this singleton at app launch in the first place — nothing
else in the app injects `AppHistoryCaptureScheduler`/`AppHistoryCaptureRepository` directly, so
without that forced construction capture would never run.

`start()` guards itself with an `AtomicBoolean` so a second call is a no-op: `onCreate` is the only
caller today, but `start()` is also reachable through the separately bound `AppHistoryCaptureScheduler`
interface, and neither the delayed reconciliation launch nor the flow subscription is otherwise
idempotent.

`PackageChangesObserver` (`core:apps`) surfaces the changed package name and
`PackageChangeAction` (`Added`/`Removed`/`Replaced`) parsed from the broadcast `Intent`; this module
was the reason that observer was extended off a bare `Flow<Unit>`.

## Module Wiring

`app/build.gradle.kts` depends on this module directly (`implementation(projects.core.appHistory)`)
purely to pull its Hilt bindings into the graph — no other module (feature or core) depends on
`core:app-history` today, so without that direct dependency Hilt would never see this module's
`@InstallIn(SingletonComponent::class)` modules and capture would silently never run. Keep that
dependency even if it looks unused from `app`'s own Kotlin code.
