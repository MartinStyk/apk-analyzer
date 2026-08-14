# core:user-preferences Module

## Purpose and Boundary

Owns lightweight user-facing history. The package is
`sk.styk.martin.apkanalyzer.core.userpreferences`.

Recently viewed apps and search history are independent repository families, each in its own
subpackage, and their public contracts stay separate even though both are keyed on package name and
surface most-recently-used ordering.

## Package Map

* `db/` — the single Room database (`apkanalyzer.room` convention plugin) both families share:
  `UserPreferencesDatabase`, plus the `di/` module that builds it. A family's own subpackage holds its
  `@Entity`/`@Dao` and provides its DAO from this shared database; Room doesn't require entities to be
  co-located with the `@Database` class, so this doesn't collapse the two families together.
* `recentlyviewed/` — one row per package with a view count and last-viewed timestamp.
* `searchhistory/` — one row per package with the query that led to it, a search count, and a
  last-searched timestamp.

Both families' `di/` modules bind their repository and provide their own DAO from `UserPreferencesDatabase`; only `db/di/UserPreferencesDatabaseModule` builds the database itself.

## Recently Viewed Semantics

Recently viewed apps are opt-in (`Key.RecentlyViewedAppsEnabled`, still in `core:common` DataStore —
it's a plain toggle, not history data). Disabling the preference switches the exposed flow to an empty
list immediately.

One row per package keyed on `packageName`, carrying `viewCount` and `lastViewedAt`. `addRecent` is a
single atomic upsert-increment query — never a read-modify-write of a whole list — so it stays correct
under concurrent calls and never truncates at write time. Persist package names, then resolve them
against the live installed-app list; `recents()` reads are capped at eight *at query time*
(`ORDER BY lastViewedAt DESC LIMIT 8`), so nothing is discarded from storage the way a capped in-memory
list would. Packages that are no longer installed disappear from reads without rewriting persisted
history.

`viewCount` is tracked on every view but has no query surface yet — no feature reads it today. It
exists so a future "most viewed" ranking (`ORDER BY viewCount DESC`) needs no migration; add the DAO
query only when a real caller needs it.

## Search History Semantics

Identity is the app, not the query text — a row is keyed on `packageName`, carrying the `query` that
most recently led to it, a `searchCount`, and `lastSearchedAt`. Searching different queries that both
land on the same app collapse into one row rather than producing separate history entries; `addSearch`
is the same atomic upsert-increment shape as `recentlyviewed`'s `addRecent`.

`history()` resolves each row's `packageName` against the live installed-app list, same pattern as
`recentlyviewed`'s `recents()` — but here a miss is *kept*, not filtered out: `SearchHistoryEntry.app`
is nullable, so a row for an app that's since been uninstalled still exists, with `app == null` as the
signal for the consuming feature to fall back to rendering the remembered `query` text instead of an
app row. `feature/apps/impl/search` is the only consumer and owns that fallback rendering plus the
tap behavior (open the app vs. re-run the query) — this module only guarantees the row survives and
says whether the app currently resolves.

Only `AppSearchAction.AppClicked` writes a row today — there is no "searched but selected nothing"
row, since this module has no second save trigger for that. `searchCount` is tracked the same
speculative way as `recentlyviewed`'s `viewCount`: captured now, no ranking query yet.

`UserPreferencesDatabase` has no destructive-migration fallback — a schema change without a real
`Migration` crashes on open rather than silently dropping data. Bump `version` and provide a
`Migration` when the schema changes.
