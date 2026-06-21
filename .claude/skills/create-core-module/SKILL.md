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
}

dependencies {
    implementation(projects.core.common)
}
```

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

## Rules
- Core modules **never** depend on `feature` modules.
- Always depend on at least `core:common`.
- Use `@Singleton` scope for all repository/manager bindings.
- Use `DispatcherProvider` for all dispatcher switching — never hardcode `Dispatchers.IO`.
- Define an interface + `Impl` class for all repositories and managers.

## Namespace Mapping Reference

| Directory | namespace value |
|-----------|----------------|
| `core/apps` | `core.apps` |
| `core/app-permissions` | `core.apppermissions` |
| `core/app-statistics` | `core.appstatistics` |
| `core/user-preferences` | `core.userpreferences` |
| `core/ui-library` | `core.uilibrary` |
| `core/common` | `core.common` |

