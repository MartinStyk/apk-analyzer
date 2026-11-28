# core:feature-flags Modules

Owns feature availability separately from user preferences. `api` holds typed flags, read values,
override states, and the optional editor contract. Normal readers expose only Boolean values;
editor status and mutations belong to the override contract. `impl` owns the build-backed read
repository and selects the active reader. `debug` owns the DataStore-backed reader and writer,
delegating defaults to the build reader, and is linked only by the phone app through
`debugImplementation`. All production code uses `src/main`; build selection is through module
dependencies, not variant source sets.

The build-backed implementation declares defaults explicitly:

| Build | Inventory export | What changed tab, app-detail history, history recording |
|---|---|---|
| Release, phone and TV | Off | Off |
| Phone debug | On | On |
| TV debug | On | Off |

Only phone debug builds read persisted overrides. TV always uses build defaults, even when old
phone overrides remain on disk. There is no support policy or support metadata in the editor.

Only explicit overrides are stored. Missing keys follow the current build default; resetting removes
keys instead of saving default values. Both contracts return values directly and propagate storage
exceptions, like `PersistenceRepository`; they do not provide fallback values or storage-error UI.
Never emit a default before persisted storage finishes loading.
Flag changes affect existing observers immediately; disabling recording must not delete history.
The What changed tab and app-detail History card have independent flags, separate from recording.
The recording flag says whether history recording exists in this build; the user's own on/off switch
sits on top of it in `core:app-history`'s `AppHistoryRecordingRepository`, which is what capture and
screens read.
