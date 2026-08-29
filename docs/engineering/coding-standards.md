# Coding standards

The conventions a contributor needs before writing the first line. Everything here is enforced in
review, and most of it is enforced by [verification](verification.md) too. The short canonical list
is in [`AGENTS.md`](../../AGENTS.md) and the PR-level workflow is in
[`CONTRIBUTING.md`](../../CONTRIBUTING.md); this document explains the *why* behind the rules that
surprise people.

## No comments, no KDoc

There are effectively no comments in this codebase, and that is deliberate. A comment is a second
description of behaviour that no compiler checks, so it rots silently. Names, types and structure
carry the intent instead — and when they can't, that's the signal to restructure, not to annotate.

**Before** — the shape this codebase avoids:

```kotlin
// apps over 500 MB count as large
if (effectiveSize >= 500 * 1024 * 1024) add(AppDetailBadge.Large)

// consider an app unused after half a year
if (lastUsed.isBefore(now.minus(Duration.ofDays(180)))) {
    add(AppDetailInsight.Unused(monthsSinceLastUsed = monthsBetween(lastUsed, now)))
}
```

**After** — what the repository actually contains, in
[`AppDetailViewModel`](../../feature/app-detail/impl/src/main/kotlin/sk/styk/martin/apkanalyzer/feature/appdetail/impl/AppDetailViewModel.kt)
and
[`AppDetailInsightEvaluator`](../../feature/app-detail/impl/src/main/kotlin/sk/styk/martin/apkanalyzer/feature/appdetail/impl/insight/AppDetailInsightEvaluator.kt):

```kotlin
if (effectiveSize >= AppClassificationThresholds.LARGE_SIZE) add(AppDetailBadge.Large)

if (lastUsed.isBefore(now.minus(AppClassificationThresholds.UNUSED_PERIOD))) {
    add(AppDetailInsight.Unused(monthsSinceLastUsed = monthsBetween(lastUsed, now)))
}
```

The comments are gone because
[`AppClassificationThresholds`](../../core/apps/src/main/kotlin/sk/styk/martin/apkanalyzer/core/apps/AppClassificationThresholds.kt)
now names each number once, in one place, and every screen that classifies an app reads the same
constant. "Large" means the same thing in the badge, in the quick filter, and in the insight list —
which a comment next to a literal could never guarantee.

The only exception is an explicit request for a comment in a specific instance.

## Return values, not exceptions, across interfaces

A repository or manager interface never throws. It returns `Result<T>`, a nullable `T?`, or an empty
collection, so the failure is part of the signature and the caller cannot forget it:

```kotlin
interface AppDetailRepository {
    suspend fun details(reference: AppReference): Result<AppDetail>
}
```

— [`AppDetailRepository`](../../core/apps/src/main/kotlin/sk/styk/martin/apkanalyzer/core/apps/AppDetailRepository.kt),
with the same shape in
[`AppExportManager`](../../core/apps/src/main/kotlin/sk/styk/martin/apkanalyzer/core/apps/export/AppExportManager.kt)
and
[`SigningSchemeRepository`](../../core/apps/src/main/kotlin/sk/styk/martin/apkanalyzer/core/apps/signing/SigningSchemeRepository.kt),
where a `Result<List<SigningSchemeVersion>?>` distinguishes "failed to read" from "nothing to read".

Wrap the failing work with
[`runCatchingCancellable`](../../core/common/src/main/kotlin/sk/styk/martin/apkanalyzer/core/common/coroutines/RunCatching.kt),
never plain `runCatching`, inside a coroutine. Plain `runCatching` swallows `CancellationException`
and turns a cancelled scroll or a closed screen into a spurious failure; the wrapper rethrows it and
captures everything else.

Don't hide errors behind a broad `catch` that returns a default. Either the failure is part of the
signature or it propagates.

## Injected dispatchers

Never reference `Dispatchers.IO` or `Dispatchers.Default` directly. Inject `DispatcherProvider` from
[`core:common`](../../core/common/AGENTS.md) and switch explicitly:

```kotlin
.flowOn(dispatcherProvider.default())
withContext(dispatcherProvider.io()) { … }
```

Analysis work in this app is heavy — parsing manifests, reading certificates, walking APK entries —
so the thread it runs on is a correctness concern, not a detail. Going through the provider keeps
the choice visible at the call site and keeps the class substitutable.

Coroutines and flows are the only concurrency primitives. No `Thread`, no `Executor`, no
`runBlocking`.

## Keep mutable state private behind a read-only view

Declare the property with its read-only type and give it a `MutableStateFlow` backing field, using
Kotlin's explicit backing field syntax. Outside the class only the `StateFlow` is visible; inside it,
`.value =` works because the real backing field is the mutable flow.

```kotlin
@Singleton
internal class StorageStatsRepositoryImpl @Inject constructor(
    …
) : StorageStatsRepository {

    final override val totalSizes: StateFlow<Map<PackageName, AppSize>>
        field = MutableStateFlow<Map<PackageName, AppSize>>(emptyMap())

    private suspend fun fetchTotalSizes(packageNames: List<PackageName>, trigger: String) {
        …
        totalSizes.value = sizes
    }
}
```

— [`StorageStatsRepositoryImpl`](../../core/apps/src/main/kotlin/sk/styk/martin/apkanalyzer/core/apps/storagestats/StorageStatsRepositoryImpl.kt).

This replaces the older `private val _x = MutableStateFlow(); val x: StateFlow<X> = _x.asStateFlow()`
pair with a single declaration. The effect is the same either way: exposing the `MutableStateFlow`
itself would let any collector write to state it doesn't own, and every mutation path would have to
be found by grep rather than by reading one class.

The same rule applies inside ViewModels: `state` is declared as `StateFlow<FeatureState>` with a
`MutableStateFlow` backing field, and events go over a private `Channel` exposed as
`receiveAsFlow()`.

```kotlin
val state: StateFlow<ApkFilePickerState>
    field = MutableStateFlow<ApkFilePickerState>(ApkFilePickerState.Ready)
```

— [`ApkFilePickerViewModel`](../../feature/apps/impl/src/main/kotlin/sk/styk/martin/apkanalyzer/feature/apps/impl/components/apkfilepicker/ApkFilePickerViewModel.kt).

## Prefer one combined state over several fine-grained flows

A screen has one state. When several sources feed it, `combine()` them into a single
`StateFlow<State>` rather than exposing one flow per moving part.

```kotlin
private val source = MutableStateFlow<AppDetailSource>(AppDetailSource.Loading)
private val exportInProgress = MutableStateFlow<AppDetailExport?>(null)

val state: StateFlow<AppDetailState> = combine(source, exportInProgress) { source, exportInProgress ->
    …
}
```

— [`AppDetailViewModel`](../../feature/app-detail/impl/src/main/kotlin/sk/styk/martin/apkanalyzer/feature/appdetail/impl/AppDetailViewModel.kt).
[`AppsViewModel`](../../feature/apps/impl/src/main/kotlin/sk/styk/martin/apkanalyzer/feature/apps/impl/list/AppsViewModel.kt)
is the larger case: the filtered list, recents, sort type, sort direction and a permission rationale
collapse into one `AppsState`, and the sort override that quick filters imply is resolved once,
inside that `combine`, instead of in each consumer.

Three reasons this is the default:

* **Fewer recomposition triggers.** Five separately collected flows can recompose a screen five
  times for one logical change, and can render intermediate combinations that never made sense
  together — a list already filtered but still labelled with the previous sort.
* **One source of truth per screen.** Any rule that spans two inputs is computed in exactly one
  place. Two indicators of the same fact must come from one predicate; separate flows are how they
  drift apart.
* **It matches the ViewModel shape.** One `StateFlow<State>` and one `onAction` is the contract, so
  a combined state is what the rest of the codebase already expects to find.

Flows beyond `combine`'s five-argument arity use the six-argument overload in
[`Flows.kt`](../../core/common/src/main/kotlin/sk/styk/martin/apkanalyzer/core/common/coroutines/Flows.kt)
rather than nesting combines.

Note the detekt consequence: a constructor parameter used only in a property initializer such as a
`combine` chain is declared without `private val`, or `UnusedPrivateProperty` fires — see the
constructors of `AppsViewModel` and `FilterViewModel`.

## Compose

* Feature modules never import `androidx.compose.material3` — see
  [architecture](architecture.md#shape-3--a-design-system-not-scattered-material).
* No hardcoded user-facing strings. Use `stringResource` backed by the owning module's
  `res/values/strings.xml`, and follow the
  [`translate-strings`](../../.claude/skills/translate-strings/SKILL.md) skill when copy changes.
* Every list in a State class or Composable parameter is an `ImmutableList` from
  `kotlinx.collections.immutable`. `@Immutable` on State data classes, `@Stable` on non-data classes
  used as Composable parameters.
* Every file with `@Composable` functions carries `@Preview` functions: private, suffixed `Preview`,
  wrapped in `ApkAnalyzerTheme { }`, with realistic sample data. Preview the stateless content
  composable, never the ViewModel-dependent screen.
* Callbacks are present tense: `onClick`, `onSelectItem`, `onBack` — never `onClicked`,
  `onItemSelected`, `onBackPressed`.
* `LazyColumn` item keys must be unique across the whole list, not per section. A sectioned list
  keyed on an item identifier crashes when the same identifier appears in two sections; deduplicate
  in the ViewModel rather than compounding the key with the section.
* In app detail, every list row is `tap = explain, long-press = copy`. A row with nothing to show on
  tap means the explanation sheet is missing, not that the idiom is optional.

## Naming and language conventions

* `data object`, not plain `object`, for sealed interface members.
* A nullable primitive that encodes a variant or a third state is a type in disguise. A `Boolean?`
  meaning yes/no/unknown belongs in an enum or sealed interface — `Feature` and `FeatureAvailability`
  in [`core:apps`](../../core/apps/AGENTS.md) both replaced exactly that shape.
* Extract a shared helper when the second consumer appears — and grep for a third before writing
  your own. Duplicated composables here have reached three copies before anyone noticed.
* No wildcard imports. Spotless will not flag an import left behind by a deletion, so check by hand.
* Prefer `private`; `internal` for module-visible; `public` only for real public API.
* Logging goes through `Logger` from `core.common.logger`, never raw Timber: `Logger.d(TAG, "msg")`,
  `Logger.e(TAG, throwable, "msg")`, with a file-level `private const val TAG`.
* `@Serializable` for nav keys and new models. Parcelize only for existing Android-specific data
  already passed through intents or bundles.
* No debug code, temporary logging, or TODOs in production code.

## Dependencies and tests

* **No new dependencies without asking.**
  [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml) is the only source of coordinates
  and versions.
* **No test infrastructure exists here, by choice.** Don't add tests, test dependencies, or test
  source sets unless the issue explicitly asks for them.

## Related

* [Architecture](architecture.md) — the module graph and the shapes these conventions live inside
* [Verification](verification.md) — which of these rules a tool catches and which review catches
* [`docs/app/technical/kotlin-code-quality.md`](../app/technical/kotlin-code-quality.md) — the standing
  code-quality audit
