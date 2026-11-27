# core:feature-flags:debug

Debug-only DataStore reader implementing `FeatureFlagRepository`, also bound as the editor
`FeatureFlagOverridesRepository`. Both contracts use the same singleton and DataStore. The reader
injects the `buildFeatureFlags` named repository for defaults; it never injects the
unqualified active reader, which would create a dependency cycle. Only the phone app depends on
this implementation module, with `debugImplementation`; features consume `api`. TV never links it.

The isolated `debug_feature_flags` preferences file stores explicit Boolean overrides with stable
backend keys. It never stores build defaults. Reset removes
one key; reset-all removes all known flag keys. Report read and write failures as `Result` and
preserve coroutine cancellation. Reads log failures; the editor logs and reports failed writes.
The first effective result waits for storage, and read failures never fall back to build defaults.
Editor observations read the immutable build defaults once and observe persisted changes.
Normal per-flag reads remain reactive and distinct. Both use `FeatureFlagStatus.isEnabled`
internally so the editor and consumers agree. Any explicit override replaces the default, including
an enabled override of a disabled default. No application-variant or support policy belongs here.
