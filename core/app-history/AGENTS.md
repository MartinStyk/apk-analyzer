# core:app-history Module

## Purpose and Boundary

Room-backed storage for full-state app history snapshots — one row per captured install instance,
enough to reconstruct the existing app-detail screens against a past point in time. The package is
`sk.styk.martin.apkanalyzer.core.apphistory`.

**Status:** capture is implemented and running (schema, pipeline, all three triggers — fast-path
broadcast, on-launch reconciliation, and periodic `WorkManager` reconciliation). Restore merging
(`HI-17`'s "merge, don't overwrite" for Android's free Auto Backup path) is also implemented — see
[Restore Merge](#restore-merge). The diff engine (`HI-03`) is implemented as the per-app timeline
read API — see [Timeline and Diff](#timeline-and-diff) — but nothing consumes it yet. Not yet
built: any UI (`HI-06`/`HI-08`/`HI-14`), the significance rules (`HI-18`), retention/pruning
(`HI-04`), the Pro-gated Drive backup half of `HI-16`, and the `HI-10` runtime-state
(enabled/install-source) tier. See
[`docs/app/technical/app-history-capture-schema.md`](../../docs/app/technical/app-history-capture-schema.md)
for the full design, including the entity/DAO schema — read it before touching this module; it is the
source of truth for schema and capture semantics, not this file.

## Package Map

Four domain subpackages: `storage/` (`AppHistoryPersistenceRepository` at the root, its
`AppHistoryRecordEncoder`/`AppHistoryRecordDecoder` in `storage/util/`, and everything that is the stored
data in `storage/database/` — the Room database, DAOs, `entity/`, the serialized blob DTOs in
`snapshot/`, and blob compression; see [Persistence Repository](#persistence-repository)),
`capture/` (the pipeline, its triggers, and
Hilt bindings in `capture/di/`), `restore/` (the `BackupAgent` and the restore-merge routine — see
[Restore Merge](#restore-merge)), and `timeline/` (the public `AppHistoryRepository`, its
implementation, and the internal differ, with its public read models — `AppHistoryTimeline`,
`AppHistoryEntry`, `AppHistoryDiff` and their parts — in `timeline/model/`; see
[Timeline and Diff](#timeline-and-diff)). See
[Reading a Snapshot](#reading-a-snapshot) for `storage/`'s read path and
[Capture Pipeline](#capture-pipeline) / [Triggers](#triggers) for `capture/`'s. Everything stays
`internal` except `timeline/`'s repository interface and `timeline/model/`, and `AppHistoryBackupAgent`, which
the manifest instantiates by name; nothing outside this module reads Room or wire-format types, or
calls the scheduler/capture repository, directly.

`storage/` splits gate-checking, writing, and reading into three separate `@Dao` interfaces rather
than one combined DAO — not because Room requires it, but because those are genuinely different
call sites with different needs. Outside `storage/`, only the restore merge touches DAOs directly
(it copies raw rows between two databases of the same schema, a storage job with no domain step);
capture and the timeline go through `AppHistoryPersistenceRepository`.

## Persistence Repository

`AppHistoryPersistenceRepository` (internal, `storage/`) is the only translation layer between
`core:apps` domain types and the stored format, in both directions. Callers hand it and get back an
`AppHistoryRecord`: the capture identity (`AppHistoryCaptureKey` — `deviceId` plus the app's own
`firstInstallTime`/`lastUpdateTime`/`versionCode`, compared by the gate), `capturedAt`, a full `AppDetail`, and the
two separately fetched sections as `CapturedSection<T>` (`Captured(value)` / `Unavailable`). The key
holds exactly what the *live* app can be compared on: `deviceId` belongs in it, because install
timestamps can be identical across devices and a restored row from another device must not match the
live app; `capturedAt` deliberately does not, because the live app has no capture time and including
it would make every app look changed on every sweep.

* **Write** (`save`, via `AppHistoryRecordEncoder`): maps to the `storage/database/snapshot/` DTOs, serializes canonical JSON, hashes,
  compresses, and inserts the device row (`IGNORE`), blobs, and snapshot row in one transaction, so
  the snapshot's `deviceId` foreign key always resolves. An `Unavailable` section is stored as
  a `null` hash.
* **Read** (`observe`, via `AppHistoryRecordDecoder`): decodes every snapshot row, with its package's blobs, back into an `AppDetail` with
  `AnalysisMode.Historical`, recomputing what the schema doc says is derived rather than stored
  (`source` via `resolveAppInstallSource`, SDK labels via `SdkVersionResolver`, certificate trust via
  `resolveCertificateTrustLevel`, `hasMultipleSigners`). Grant state is not stored, so every
  `UsedPermission.isGranted` is `false`; every consumer already reports grant state only for
  `InstalledPackage`. Split file paths are not stored, so `InstalledSplitApk.filePath` is `null`.
  Permission descriptions are not stored, so every `PermissionDetails.description` is `null`: they
  are localized text that belongs to the declaring package, and were the largest part of the
  permissions blob. Activity labels are not stored either, so every `Activity.label` is `null`: a
  device language change would otherwise rewrite every app's activities blob on its next update.
  Each distinct section is decoded and mapped to its domain value once per emission, so snapshots
  with equal hashes share one domain instance; re-decoding only happens when the package's snapshot
  rows change (see [Reading a Snapshot](#reading-a-snapshot)).
* **Failure granularity.** A missing or undecodable blob for one of the two separately fetched sections
  degrades that section to `Unavailable` with a logged non-fatal. The eight `AppDetail` sections are
  written together from one `AppDetail` (capture aborts otherwise), so one of them missing or
  undecodable is corruption, not a partial capture: that one snapshot is left out with a logged
  non-fatal, so its neighbours diff against each other and the rest of the history stays readable. A
  failing Room query emits a failed `Result` for that emission, which the timeline reports as
  `AppHistoryTimeline.Unavailable`; the flow keeps running, so the next change can recover.

## DTO Boundary — Not a Detail

`core:apps` domain types (`Activity`, `Certificate`, `Permissions`, ...) are **never** `@Serializable`
and never serialized directly into a blob. `core:app-history` defines its own mirror types in
`storage/database/snapshot/` (one `@Serializable internal` DTO per captured section, with a `toSnapshot()`
mapper from the domain type and a `toDomain()` mapper back), used only by
`AppHistoryRecordEncoder` and `AppHistoryRecordDecoder`.

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
keep file count down — lossless either way; `toDomain()` maps them back with `valueOf`, so a stored
name the enum no longer has fails that section's decode rather than being guessed.

**A field added to an existing DTO must be nullable with a `null` default, meaning "not recorded",
and the differ must ignore it when either side is `null`.** Old blobs decode without the field, so a
non-null default would stand in for the real value in every snapshot written before the change, and
the first update after release would report that default changing to the real value — "exported: no
→ yes" for an app that changed nothing. The same holds when a `core:apps` fix changes how a captured
value is computed.

**Bump `BLOB_FORMAT_VERSION` (`storage/database/util/SnapshotJson.kt`) whenever the serialized form
changes** — a DTO field added, removed, renamed or retyped, an enum name stored differently, or a
`snapshotJson` setting changed. Every snapshot row records the version its sections were encoded
with in `blobFormatVersion`, so old content stays identifiable: a decoder that has to treat a past
format differently, or a one-off job that rewrites old blobs, can select exactly those rows. Room
cannot see this change — the blob columns stay `BLOB` — so the constant is the only record of it.
A blob shared between snapshots of different versions has byte-identical content, since the hash
matched, so it decodes the same under either.

The `kotlin-serialization` plugin and every `@Serializable` annotation are scoped to this module
alone — `core:apps`/`core:common` carry no serialization dependency or annotations because of this
module's needs.

## Reading a Snapshot

`AppHistoryReadDao.observeSnapshots(packageName)` watches only the package's `app_history_snapshot`
rows, oldest first: no blob bytes and no joins. Room invalidates per table, so every capture of any
package re-runs it, which is why it must stay cheap; `distinctUntilChanged` on the entities then
stops anything that did not change this package. Only a real change reaches `mapLatest`, which loads
the package's blobs once each with `AppHistoryReadDao.blobs(packageName)` and hands rows and blobs
to the decoder; a newer change cancels a decode still in progress. The two reads need no
transaction: blobs are inserted before their snapshot in one transaction and never deleted, so
every row the first read sees already has its blobs. An earlier version joined all eleven blobs in
one `@DatabaseView`, which repeated a shared blob's bytes in every row and re-ran the whole join on
unrelated captures.

Hashes stay inside `storage/`: callers above it receive full `AppDetail`s, and the differ compares
domain data only. Skipping unchanged sections by hash comparison was considered and rejected for
that reason; per-hash caching already limits decoding to distinct section contents.

Blob content is Deflate-compressed JSON (`ByteArray`), compressed and decompressed only through
`storage/database/util/BlobCompression.kt`; the hash is the first 64 bits of SHA-256 over the
uncompressed JSON, stored as a `Long`. The database file is
`app_history_v2.db` — the uncompressed `app_history.db` from 4.1.3–4.2.0 is deleted on startup rather
than migrated, see the schema doc's
[`app_history_blob`](../../docs/app/technical/app-history-capture-schema.md#app_history_blob) for why.
Blobs decode with `ignoreUnknownKeys`, so removing a DTO field never breaks old history. Encoding,
hashing, and decoding all go through the one `snapshotJson` in `storage/database/util/SnapshotJson.kt`,
which sets `explicitNulls = false`: a `null` field is left out of the JSON and decodes back as `null`.
Hashing with any other `Json` configuration would give identical content a different hash.

## Timeline and Diff

`AppHistoryRepository.timeline(packageName)` is the module's only public read API: newest first, one
`AppHistoryEntry` per snapshot — `FirstSeen` for the oldest, then one `Change` per later snapshot,
carrying the previous snapshot, an `AppHistoryDiff` against it, and a `Change.Kind`: `NewDevice`
(adjacent snapshots with different `deviceId`, i.e. history continued after a restore onto another
device; checked first, since a device move usually also changes `firstInstallTime`), `Reinstalled`
(different `firstInstallTime`, the schema doc's chain break), or `Updated`. The kind is the only thing
the three differ in besides their date, so they are one class rather than three; `Change.timestamp`
switches on it. `NewDevice` is dated by `capturedAt`, since the new device's install timestamps say
nothing about when history moved. Two devices restored from one backup and both still capturing would
interleave and alternate `NewDevice` changes; grouping a timeline per device is an open product
question, not handled here. Diffs are computed on read and never stored, per the schema doc. Snapshots
are ordered by `capturedAt`, never by `lastUpdateTime`, which moves backwards on a system-app rollback;
for the same reason an `Updated` change whose `lastUpdateTime` went backwards is dated by its
`capturedAt` instead of the factory timestamp it rolled back to.

An `Updated` change with no changed and no uncomparable category is dropped from the timeline. The
gate writes a row whenever the key moves — a same-APK reinstall or a Play re-delivery moves it
without changing content — and skipping those at write time would need somewhere to record the
observed key, or the gate would re-capture the app on every trigger. Hiding on read costs about 1 KB
per such row, since its blobs dedupe. The next change still diffs against the hidden row, whose
visible content equals the one before it. `Reinstalled` and `NewDevice` are never dropped: the event
itself is the information.

It emits `AppHistoryTimeline`, not a bare list: `Available(entries)` — where an empty list honestly
means "no history" — or `Unavailable` when the database read fails, so a screen can show an error
instead of an empty timeline that looks like "nothing recorded". The failure is logged once, as a
non-fatal, in the repository; `Unavailable` carries no `Throwable` into UI state. `catch` completes the
flow, so a retry is a fresh collection of `timeline()`. There is deliberately no `Loading` variant —
the consuming ViewModel's initial state covers it. An `Unavailable` separately fetched section is not a
timeline failure: it degrades to `SetChange.Uncomparable` inside an otherwise `Available` timeline.

**The differ works on domain objects only.** `AppHistoryDiffer` compares two `AppHistoryRecord`s —
`AppDetail` plus the two `CapturedSection`s — with plain equality; it knows nothing about hashes,
JSON, or compression. `Unavailable` on either side, or a signing scheme captured as unknown (`null`),
is `SetChange.Uncomparable` (`HI-20` — a failed section must never read as "removed everything").

**Categories come from the decoded diff, not from raw hash inequality.** `HI-19` describes stub labels
as "which set IDs differ", but a differing hash does not always mean a visible change — a requested
permission's details (protection level, group) change when its *declaring* package updates, and that
changes this app's permissions hash with no added/removed name. `AppHistoryDiff.changedCategories` is derived from the
same `SetChange`/`ValueChange` values a detail screen renders, so a row label can never name a
category whose detail is empty. Localized text (permission descriptions, activity labels) is not
stored at all, so a device language change can never read as an app change.

Diffed: version, min/target SDK, APK size, install source, the three manifest flags, requested and
defined permissions, the four component types, current signing certificates, signing schemes, and
native libraries. Captured but deliberately not diffed, per `HI-03`: features, installed splits.
Intent filters are not captured at all: they were the second-largest section, the differ never read
them, and starting to capture them later only covers history recorded from then on.

`SetChange.Changed` carries the `core:apps` items themselves — `UsedPermission`, `Permission`,
`Activity`, `Service`, `BroadcastReceiver`, `ContentProvider`, `Certificate`, `NativeLibraryFile`,
`SigningSchemeVersion` — not just their keys, and `modified` holds a `ValueChange` with both versions,
so a detail screen can explain an item ("dangerous permission", "signed by …") and show what changed
in it ("exported: no → yes") without going back to the store. Each set is matched by a key (component
or permission name, certificate SHA-256, native library name + ABI + containing APK) and judged
"modified" by a comparison value: the whole item, except never for requested permissions and
certificates — a requested permission's details
belong to its declaring package, and a certificate with the same SHA-256 is the same certificate.
Native libraries are keyed by containing APK too, so a library moving between base and split APKs
reads as removed plus added.

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

All three triggers live in `AppHistoryCaptureSchedulerImpl.start()`, called once from its
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

**Periodic `WorkManager` reconciliation.** Covers the case the other two triggers can't: the app
never opened for a long stretch, so no process ever runs to fire the on-launch sweep or observe a
broadcast. `schedulePeriodicReconciliation()` enqueues a weekly `AppHistoryReconciliationWorker`
(`@HiltWorker`, delegates straight to `captureRepository.reconcileAll()`) via
`enqueueUniquePeriodicWork(..., ExistingPeriodicWorkPolicy.KEEP, ...)` — `KEEP` so calling `start()`
on every app launch re-affirms the schedule without resetting its window each time. Constraints
(`setRequiresBatteryNotLow`, `setRequiresStorageNotLow`) are the "favorable conditions" gate; no
network constraint, since reconciliation is entirely on-device. This worker races the other two
triggers' calls to `reconcileAll()`/`reconcile()` under the same per-package `Mutex` described in
[Capture Pipeline](#capture-pipeline) — no additional synchronization needed here.

`HiltWorkerFactory` wiring lives in `app`: `ApkAnalyzer` implements `Configuration.Provider` and the
manifest removes `androidx.work`'s default `WorkManagerInitializer` startup entry, since Hilt must
construct the worker to inject `AppHistoryCaptureRepository` into it. `apkanalyzer.work` (this
module and `app`) is the convention plugin adding `androidx.work`/`androidx.hilt.work` and the
`androidx.hilt` KSP compiler that generates the `@HiltWorker` binding.

`AppHistoryCaptureSchedulerImpl` takes `Lazy<WorkManager>`, not `WorkManager` directly — this isn't
a style choice. `ApkAnalyzer` field-injects `lifecycleObservers: Set<DefaultLifecycleObserver>`
(which eagerly constructs this singleton) before it field-injects `workerFactory: HiltWorkerFactory`.
Resolving a bare `WorkManager` while building that set calls `WorkManager.getInstance(context)`,
which — since the manifest disabled the default initializer — falls into WorkManager's on-demand
path and reads `ApkAnalyzer.workManagerConfiguration`, which reads `workerFactory` back off the very
`ApkAnalyzer` instance still being injected: `UninitializedPropertyAccessException`, reproduced on a
real device, not caught by any compile-time check. `Lazy<WorkManager>` defers that resolution to
`schedulePeriodicReconciliation()` inside `start()`, called from `lifecycleObservers.forEach { ... }`
in `ApkAnalyzer.onCreate()` — after `super.onCreate()`'s field injection has fully completed.

## Restore Merge

`AppHistoryBackupAgent` (`restore/`, public — the manifest names it directly, same pattern as
`core:app-functions`'s service) is wired via `app`'s `android:backupAgent` manifest attribute. It
exists because Android's default Auto Backup (`android:allowBackup="true"`, already set for other
reasons) includes `app_history.db` in whole-app backups by default, and restoring it verbatim onto
a fresh install — the observed, verified-on-device behavior before this existed — silently replaces
the fresh capture with whatever was last backed up, discarding anything captured since. That is
exactly the gap `app-history.md`'s `HI-17` ("Restore & reconcile... merge by install instance and
mark the gap rather than overwriting or silently dropping") describes; this implements the merge
half of it against the free Auto Backup path, not the Pro-gated Drive backup (`HI-16`) — no new
dependency, no product decision on `OQ-08` (Drive vs. Auto Backup) required first.

**`android:fullBackupOnly="true"` is required, not optional.** A custom `BackupAgent` (as opposed
to `BackupAgentHelper`) defaults to *key/value* backup — `onBackup`/`onRestore`, both intentionally
no-ops here — unless this flag opts it into full-data Auto Backup instead. Without it, verified via
`bmgr backupnow` invoking `KeyValueBackupTask` against this agent, the app's backups (not just
`app_history.db` — everything Auto Backup used to carry) silently stop happening at all, and
`onRestoreFile` never fires on restore. `bmgr fullbackup` bypasses this distinction (it forces the
full-data path directly), which is why testing with it alone looked fine before this flag existed.

**Mechanism.** `onRestoreFile` intercepts only `app_history_v2.db`(`-wal`/`-shm`) by filename and
manually copies exactly `size` bytes to `app_history_v2_restore_staging.db` instead of the live path.
A backup taken before the v2 rename carries the old `app_history.db`, which this agent ignores, so
it restores through the default path and the scheduler's startup cleanup deletes it — see the schema
doc's [`app_history_blob`](../../docs/app/technical/app-history-capture-schema.md#app_history_blob).
Matching by filename alone, not also the restored file's parent directory: an earlier version of
this also required `destination.parentFile == getDatabasePath(APP_HISTORY_DATABASE_NAME).parentFile`
as a guard against a same-named file landing somewhere unexpected (e.g. device-protected storage,
not applicable to this app today), and that comparison — `File.equals()` between a `File` the
framework hands `onRestoreFile` and one built locally via `getDatabasePath()` — was reproduced on a
real device to always evaluate false, silently routing *every* restored file (including
`app_history.db` itself) through `super.onRestoreFile(...)` instead, defeating the whole feature
with no error or log line, since the non-matching branch logs nothing. Reverted rather than fixed
with a canonical-path comparison, since the scenario it guarded against cannot occur here.
This must read exactly `size` bytes from a plain `FileInputStream(data.fileDescriptor)` and never
close `data`: the platform contract reuses one shared pipe across every file restored for the
package, so an unbounded `copyTo` (reads to EOF) or closing the descriptor breaks delivery of
whichever file comes after this one. `android.app.backup.FullBackup.restoreFile`, which the default
`onRestoreFile` uses internally, is `@hide` and unavailable to app code — no platform or stdlib API
does a bounded, non-closing copy either, hence [`core:common`'s `InputStream.limited`](../common/AGENTS.md)
(`core/common/io/`), not a hand-rolled loop here: the input is wrapped with `.limited(size)` first, so
an ordinary `copyTo` naturally stops at exactly `size` bytes, and its `Long` return is checked against
`size` to catch a truncated transfer instead of silently staging a short file. On any failure —
truncation included — `LimitedInputStream.drainRemaining()` consumes whatever of that byte range was
never read, so the shared pipe still lands on the right offset for the next file even when staging
this one failed outright. Every non-matching file falls through to `super.onRestoreFile(...)`
unchanged. Redirecting instead of merging in-place matters because restore typically runs before the
live database has ever been opened this process lifetime — landing bytes on the live path directly
would make the restored copy *become* the live database, the exact behavior being fixed.

`AppHistoryRestoreMergerImpl.mergeIfPending()` runs from `AppHistoryCaptureSchedulerImpl.start()`
(fired first, before the other triggers) on the next ordinary app launch, not from
`onRestoreFinished()` — the restore-time callback's process invocation is not guaranteed to have a
live `AppHistoryDatabase` ready to merge into, whereas by the time `start()` runs the database is.
It is a no-op unless the staging file exists. When it does: opens the staging file as a second,
ordinary `Room.databaseBuilder(..., AppHistoryDatabase::class.java, ...)` instance and imports its
rows via `AppHistoryWriteDao.mergeRestoredData`, one `@Transaction` (matching the shape of the
capture path's own `insertSnapshotWithBlobs`, so a mid-import failure can never leave orphaned blob
rows with no matching snapshot). Device rows are imported first in the same transaction, skipping
ones the live database already has, because staged snapshots reference them through the
[`app_history_device`](../../docs/app/technical/app-history-capture-schema.md#app_history_device)
foreign key.

**Every migration must also apply to the staging database.** The staging file is a backup from
whatever app version wrote it, so once `AppHistoryDatabase` moves past version 1, an older backup is
an older schema. The staging builder here has no migrations today; opening an older file would fail,
and the read-failure path below deletes it as unusable — silently losing that restore. The first
version bump must give the live and staging builders one shared set of migrations. The schema is
exported to `core/app-history/schemas/` for writing them. Blobs insert IGNORE-on-conflict, safe because they're
content-addressed by `(packageName, hash)`. Snapshots also insert IGNORE-on-conflict, relying on the
table's `UNIQUE(packageName, capturedAt, lastUpdateTime, firstInstallTime)` index to skip ones the live database
already has — but only after resetting each row's `id` to `0` first, never carrying a foreign
autoincrement id into the live table (Room would insert at that literal id, potentially colliding
with an unrelated live row that happens to share the number).

That `id` reset is exactly why [the capture gate](../../docs/app/technical/app-history-capture-schema.md#capture-gate)
orders by `capturedAt` rather than raw `id` — a restored row always lands with a higher `id` than
anything already live (autoincrement only grows), so if the gate still picked "latest" by `id`, an
imported row that is chronologically *older* than the live data would outrank it and the gate would
keep re-capturing that package. A restored row keeps its original `capturedAt`, so it sorts where it
was observed. (`lastUpdateTime` was the ordering key before and is not safe either: it moves
backwards when a system app's updates are uninstalled — see the schema doc's
[Capture Gate](../../docs/app/technical/app-history-capture-schema.md#capture-gate).)

The merge is split into two `runCatchingCancellable`-wrapped phases, each owning its own outcome
because a failure means something different depending on which side it happens on. Reading the
staging file (`readStagedData()`) is the untrusted half — a corrupt file, a truncated transfer, or
(once this module's `@Database` version ever moves past 1) a staging file from an incompatible
schema will fail here, and there is no version of "retry" that fixes an unusable file, so any
non-cancelled failure deletes the staging files immediately and gives up on that restore. Writing
the already-validated rows into the live database (`liveWriteDao.mergeRestoredData`) is the
trusted half — the staged data is known-good by this point, so a failure here (disk full, an I/O
hiccup) is assumed transient and the staging files are deliberately *not* deleted, leaving the whole
merge to retry from scratch on the next launch. That retry is safe because the merge is idempotent —
blobs and snapshots both insert IGNORE-on-conflict — so replaying an already-partially-merged staging
file only re-skips what already landed. Neither phase deletes the staging files after a
`CancellationException`, which propagates instead of being caught, per
[`core:common`'s contract](../../core/common/AGENTS.md#durable-contracts): a cancelled merge (e.g.
process teardown mid-import) leaves the staging file in place to retry whole on the next launch,
rather than discarding a pending restore no coroutine ever got to finish.

The merge's bulk insert is not guarded by the per-package `Mutex` [Capture Pipeline](#capture-pipeline)
uses to serialize the fast path against reconciliation, so it is possible in principle for a live
capture and the merge to race on the same package's natural key at the exact same instant, aborting
that one capture attempt with a constraint violation. Deliberately not fixed with a shared lock here:
`reconcileAll()` already catches and logs each package's capture failure individually and continues
the sweep (see [Capture Pipeline](#capture-pipeline)'s concurrent-capture note), so a raced package
simply gets picked up again on the next trigger — exactly the tolerance already built for the fast
path racing reconciliation. That tolerance only holds because of the gate-ordering fix above; without
it, a raced (or even non-raced) restored row would poison the gate permanently instead of just once.

**Verifying this on-device is slow, not instant.** `bmgr backupnow <pkg>` exercises *key/value*
backup (`onBackup`, intentionally a no-op here) — it does not touch `onRestoreFile` at all. Use
`bmgr fullbackup <pkg>` for the actual full-data path this feature depends on. Even then, restoring
immediately after a same-session `bmgr fullbackup`/`bmgr backup`+`bmgr run` against the real Google
transport consistently reported `initiateOneRestore packageName=@pm@` /
`No more packages; finishing restore` on the device this was verified on — the transport appears to
need real server-side propagation time before a backup is exposed as restorable, independent of any
on-device command. `com.android.localtransport` doesn't help either: it does not persist a full-data
backup across an app uninstall, so `restoreAtInstall` finds nothing for it post-reinstall regardless.
The one clean, real reproduction of the original bug (and thus the only way this fix's `onRestoreFile`
path has been observed running end-to-end) came from a *naturally aged* backup the OS had already
propagated days earlier — not from anything forced in the same session.

## Module Wiring

`app/build.gradle.kts` depends on this module directly (`implementation(projects.core.appHistory)`)
purely to pull its Hilt bindings into the graph — no other module (feature or core) depends on
`core:app-history` today, so without that direct dependency Hilt would never see this module's
`@InstallIn(SingletonComponent::class)` modules and capture would silently never run. Keep that
dependency even if it looks unused from `app`'s own Kotlin code.
