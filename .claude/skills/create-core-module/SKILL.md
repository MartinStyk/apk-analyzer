---
name: create-core-module
description: Use when creating a new core or shared library module for domain logic, data access, repositories, managers, or utilities. Triggered by phrases like "create core module", "add repository", "new data layer", "create shared module", "add manager".
---

# Create Core Module

Given a core module name `<name>` (e.g., `app-export`):

> `<flatname>` = module name with hyphens removed (e.g., `app-export` → `appexport`, `user-preferences` → `userpreferences`)

## Step 1 — Create build file

**`core/<name>/build.gradle.kts`**

Without Hilt (pure utility module, no DI):
```kotlin
plugins {
    alias(libs.plugins.apkanalyzer.library)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.<flatname>"

    buildFeatures {
        androidResources = false
    }
}

dependencies {
    implementation(projects.core.common)
}
```

With Hilt (has repositories or managers with DI):
```kotlin
plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.<flatname>"

    buildFeatures {
        androidResources = false
    }
}

dependencies {
    implementation(projects.core.common)
}
```

Drop the `buildFeatures` block only if this module ends up owning its own `res/` (e.g. a
`strings.xml` for user-facing copy) — see `core/apps/build.gradle.kts` for that shape. Every module
without resources of its own must set `androidResources = false`; don't leave it unset "just in
case."

## Step 2 — Create repository interface

**`core/<name>/src/main/kotlin/sk/styk/martin/apkanalyzer/core/<flatname>/<Name>Repository.kt`**
```kotlin
package sk.styk.martin.apkanalyzer.core.<flatname>

import kotlinx.coroutines.flow.Flow

interface <Name>Repository {
    fun data(): Flow<List<MyModel>>
    suspend fun getById(id: String): MyModel?
}
```

Public interface methods must never throw. Use:
- `Result<T>` for recoverable operations with meaningful error info
- `T?` for optional/missing values with no error context needed
- `List<T>` (empty) for collection results that may be empty

## Step 3 — Create repository implementation

**`core/<name>/src/main/kotlin/sk/styk/martin/apkanalyzer/core/<flatname>/<Name>RepositoryImpl.kt`**
```kotlin
package sk.styk.martin.apkanalyzer.core.<flatname>

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import sk.styk.martin.apkanalyzer.core.common.coroutines.DispatcherProvider
import javax.inject.Inject

class <Name>RepositoryImpl @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
) : <Name>Repository {
    override fun data(): Flow<List<MyModel>> = flow {
        // implementation
    }.flowOn(dispatcherProvider.io())

    override suspend fun getById(id: String): MyModel? = withContext(dispatcherProvider.io()) {
        runCatching { /* ... */ }.getOrNull()
    }
}
```

## Step 4 — Create Hilt module (if using DI)

**`core/<name>/src/main/kotlin/sk/styk/martin/apkanalyzer/core/<flatname>/di/<Name>Module.kt`**
```kotlin
package sk.styk.martin.apkanalyzer.core.<flatname>.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sk.styk.martin.apkanalyzer.core.<flatname>.<Name>Repository
import sk.styk.martin.apkanalyzer.core.<flatname>.<Name>RepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface <Name>Module {
    @Binds
    @Singleton
    fun bind<Name>Repository(impl: <Name>RepositoryImpl): <Name>Repository
}
```

## Step 5 — Register in `settings.gradle.kts`

Add inside `include(...)`:
```kotlin
":core:<name>",
```

## Step 6 — Create module docs

Every module carries its own `AGENTS.md` (dense agent-reference notes: purpose, package, annotated
structure, key interfaces/exports, dependencies — see `core/apps/AGENTS.md` or `core/common/AGENTS.md`
for the exact format to match) plus a one-line `CLAUDE.md` pointer:

**`core/<name>/CLAUDE.md`**
```
@AGENTS.md
```

**`core/<name>/AGENTS.md`** — write it once the module's real content exists (not before Step 2-4
produce something to document). Don't leave it as a stub with placeholder text.

The root `AGENTS.md` does not list individual modules, so there is nothing to update there. Run
`./gradlew validateAgentContext` to confirm the new module has the AGENTS/CLAUDE pair it requires.

## Rules
- Core modules **never** depend on `feature` modules.
- Always depend on at least `core:common`.
- Use `@Singleton` scope for all repository/manager bindings.
- Use `DispatcherProvider` for all dispatcher switching — never hardcode `Dispatchers.IO`.
- Define an interface + `Impl` class for all repositories and managers.

## When the module grows a second repository/manager family

Steps 2-4 above put everything at the package root — that's correct for a module with **one**
repository/manager family. The moment a **second**, unrelated family joins it (e.g. the module
gains `<Other>Repository` alongside `<Name>Repository`), stop adding to the flat root/`model`/`util`
buckets and restructure by domain instead:

- Give each family its own subpackage (e.g. `<domain>/`) holding that family's interface, impl,
  models, and any private supporting types (parsers, analyzers, resolvers) together.
- Keep only the handful of models every family composes into at the module's `model/` root — not
  every data class the module has ever defined.
- Dissolve any grab-bag `util`/`analysis`/`helpers` file: move each function next to the model or
  domain it produces, not into one shared file.
- Mark a family's supporting interfaces `internal` unless another module genuinely consumes them
  directly (grep before assuming) — `internal` is module-wide, so this is independent of which
  subpackage the type lives in.
- Keep Hilt DI as a single `di/<Name>Module.kt` with `@Binds` grouped by domain via comments; don't
  fragment it into one `@Module` per subpackage.

`core:apps` is the worked example — see its `AGENTS.md` "Structure" section and the `signing/`,
`permissions/`, `components/`, `manifest/`, `installsource/`, `devicefeatures/`, `packaging/`,
`usagestats/`, `storagestats/`, `export/`, `sdkversion/` packages. A module with exactly one
repository/manager family (`core:app-permissions`, `core:app-index`, `core:apk-files`) stays flat —
don't restructure ahead of need.

## Namespace Mapping Reference

| Directory | namespace value |
|-----------|----------------|
| `core/apps` | `core.apps` |
| `core/app-permissions` | `core.apppermissions` |
| `core/app-index` | `core.appindex` |
| `core/user-preferences` | `core.userpreferences` |
| `core/ui-library` | `core.uilibrary` |
| `core/common` | `core.common` |

