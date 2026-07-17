# core:app-permissions Module

## Purpose
Aggregates every Android permission (`android.permission.*`) requested across all apps installed on the device into a single deduplicated, sorted, human-readable list. Powers a device-wide "which apps use permission X" explorer. Depends on `core:apps` for raw installed-app/permission data.

## Package: `sk.styk.martin.apkanalyzer.core.apppermissions`

## Structure

```
DevicePermissionsRepository.kt      - Public interface: fun permissions(): Flow<List<DevicePermission>>
DevicePermissionsRepositoryImpl.kt  - internal @Singleton impl; builds a shared/cached flow from InstalledAppsRepository
PermissionLabelProvider.kt          - @Singleton; resolves a permission name to a human-readable label
model/
  DevicePermission.kt               - data class DevicePermission(val name: String, val label: String)
di/
  AppPermissionsModule.kt           - Hilt @Binds module (SingletonComponent)
res/values/                         - String resources for known permission labels (permission_label_camera, etc.)
```

## Key Interfaces

```kotlin
interface DevicePermissionsRepository {
    fun permissions(): Flow<List<DevicePermission>>
}

class PermissionLabelProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
) {
    fun getLabel(permissionName: String): String
}
```

## Notable Implementation Details

- The permissions flow is built once via `.flowOn(dispatcherProvider.io()).shareIn(appScope, SharingStarted.Lazily, replay = 1)` — all collectors share one cached computation, recomputed only when the underlying installed-apps flow emits again.
- `PermissionLabelProvider.getLabel` resolution order: (1) hardcoded map of ~45 well-known permissions via string resources, (2) `PackageManager.getPermissionInfo(name, 0).loadLabel(...)` wrapped in try/catch (many vendor permissions throw `NameNotFoundException`), (3) `createSimpleName` fallback — strips the package prefix and humanizes `SNAKE_CASE`.
- `createSimpleName` is a manual char-by-char `StringBuilder` mutation with a subtle off-by-one: the humanizing loop starts at index 1, so the very first character is never transformed. Worth fixing carefully if touched, not obviously by design.
- No memoization on `getLabel()` itself beyond the static known-labels map — unknown-permission lookups hit `PackageManager` again each time, though callers typically only call it once per unique permission thanks to `DevicePermissionsRepositoryImpl`'s caching.
- `parcelize` plugin is applied in `build.gradle.kts` but nothing in this module actually uses `@Parcelize` — likely inherited boilerplate from the module template, not a real requirement.

## Dependencies
- `apkanalyzer.library` + `apkanalyzer.hilt` + `parcelize` plugins
- `implementation(projects.core.apps)` — `InstalledAppsRepository`
- `implementation(projects.core.common)` — `DispatcherProvider`
