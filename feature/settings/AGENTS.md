# feature:settings Module

## Purpose

Secondary destination for color-scheme selection and the recently viewed apps preference. It is
reached through explicit navigation rather than the bottom navigation bar.

Debug builds also expose a Feature flags destination. All code stays in `src/main`; the entry and
navigation events are gated by this module's `BuildConfig.DEBUG`. Release retains the original
Settings controls and never registers the flag editor destination. Developer-only flag copy is
English-only and marked non-translatable.

The API module exposes only the navigation key. Implementation code uses the package
`sk.styk.martin.apkanalyzer.feature.settings.phone`. This feature has no `tv` module; the TV app has
no settings screen.

## State and Navigation Rules

Settings follows the standard State/Action/Event/ViewModel pattern. Back is both an Action and a
one-shot Event: the ViewModel translates user intent, while the screen invokes the host callback.
Never inject or call `Navigator` from the ViewModel.

This feature has no repository of its own. Normal preferences observe and save typed keys through
`core:common`'s `PersistenceRepository`. Keep persistence out of Composables and do not duplicate
preference state in the app module.

The flag editor reads status metadata and writes through the optional override contract from
`core:feature-flags:api`, never through the normal Boolean reader. Only the phone app links the storage
implementation, and only in debug. Every flag can be overridden; there is no unsupported state.
Writes are serialized by disabling controls until persistence finishes; storage exceptions propagate
without recovery UI, like normal preferences. Reset removes overrides rather than persisting the
current defaults.

Color scheme and recently viewed state are combined into one immutable screen state so both update
reactively.
