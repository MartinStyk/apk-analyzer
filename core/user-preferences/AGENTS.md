# core:user-preferences Module

## Purpose
Two small, independent DataStore-backed repositories for lightweight user-facing state: recently-viewed apps (an opt-in MRU list of packages resolved back to full `InstalledApp` objects) and search query history (a raw-string MRU list). Both are thin wrappers over `core:common`'s `PersistenceRepository` (DataStore).

## Package: `sk.styk.martin.apkanalyzer.core.userpreferences`

## Structure

```
RecentlyViewedAppsRepository.kt      - Public interface: recents(), addRecent(packageName), hasRecents()
RecentlyViewedAppsRepositoryImpl.kt  - internal impl; joins persisted recent package names with live InstalledApp data, gated by an enabled toggle
SearchHistoryRepository.kt           - Public interface: queries(), addQuery(query), removeQuery(query), clearAll()
SearchHistoryRepositoryImpl.kt       - internal impl; simple MRU list persisted via PersistenceRepository
di/
  UserPreferencesModule.kt           - Hilt @Binds module (SingletonComponent), binds both repositories, @Singleton scope
```

## Key Interfaces

```kotlin
interface RecentlyViewedAppsRepository {
    fun recents(): Flow<List<InstalledApp>>
    suspend fun addRecent(packageName: String)
    suspend fun hasRecents(): Boolean
}

interface SearchHistoryRepository {
    fun queries(): Flow<List<String>>
    suspend fun addQuery(query: String)
    suspend fun removeQuery(query: String)
    suspend fun clearAll()
}
```

## Notable Implementation Details

- `RecentlyViewedAppsRepositoryImpl.recents()` is gated by `Key.RecentlyViewedAppsEnabled`: disabled → always `flowOf(emptyList())`; enabled → `combine`s the persisted package-name list with `installedAppsRepository.apps()` and `mapNotNull`s away any package that's no longer installed. This **silently self-prunes uninstalled apps at read time without ever writing back** to the persisted key — the underlying stored list can grow stale with uninstalled package names that just get filtered on every read.
- `flatMapLatest` on the enabled flag means toggling the preference live-switches between empty and combined flows reactively.
- `addRecent`/`addQuery` both hand-roll the same "move-to-front, dedupe, cap length" MRU logic independently (`MAX_RECENTS = 8` vs. `MAX_HISTORY = 15`). If a third such repository is ever added, factor this into a shared helper in `core:common` instead of copying it a third time.
- Both `*Impl` classes are `internal`; only interfaces are public, bound via Hilt `@Binds`.
- No `shareIn`/`stateIn` caching on either flow (unlike `core:app-permissions`) — each collector independently re-triggers DataStore reads/combine.
- Underlying keys live in `core:common`'s `Key.kt`, not here: `Key.RecentlyViewedApps: Key<List<String>>`, `Key.RecentlyViewedAppsEnabled: Key<Boolean>`, `Key.SearchHistory: Key<List<String>>`.

## Dependencies
- `apkanalyzer.library` + `apkanalyzer.hilt` plugins (no `parcelize` — no Parcelable models here)
- `implementation(projects.core.apps)` — `InstalledAppsRepository`, `InstalledApp`
- `implementation(projects.core.common)` — `PersistenceRepository`, `Key`
