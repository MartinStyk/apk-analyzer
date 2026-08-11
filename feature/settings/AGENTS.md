# feature:settings Module

## Purpose

Secondary destination for color-scheme selection and the recently viewed apps preference. It is
reached through explicit navigation rather than the bottom navigation bar.

The API module exposes only the navigation key. Implementation code uses the package
`sk.styk.martin.apkanalyzer.feature.settings.impl`.

## State and Navigation Rules

Settings follows the standard State/Action/Event/ViewModel pattern. Back is both an Action and a
one-shot Event: the ViewModel translates user intent, while the screen invokes the host callback.
Never inject or call `Navigator` from the ViewModel.

This feature has no repository of its own. It observes and saves typed keys through
`core:common`'s `PersistenceRepository`. Keep persistence out of Composables and do not duplicate
preference state in the app module.

Color scheme and recently viewed state are combined into one immutable screen state so both update
reactively.
