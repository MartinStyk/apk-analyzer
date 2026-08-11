# core:user-preferences Module

## Purpose and Boundary

Owns lightweight user-facing history backed by `core:common` persistence. The package is
`sk.styk.martin.apkanalyzer.core.userpreferences`.

Recently viewed apps and search history are independent repository families. Keep their public
contracts separate even though both use most-recently-used ordering.

## Recently Viewed Semantics

Recently viewed apps are opt-in. Disabling the preference switches the exposed flow to an empty list
immediately.

Persist package names, then resolve them against the live installed-app list. Packages that are no
longer installed disappear from reads without rewriting persisted history. Preserve this behavior
unless the storage migration and cleanup policy are changed deliberately.

Adding a recent item moves it to the front, removes duplicates, and caps the list at eight.

## Search History Semantics

Search history stores raw query strings in most-recently-used order. Adding a query moves it to the
front, removes duplicates, and caps the list at fifteen. Removal and clearing update the persisted
list rather than maintaining feature-local state.
