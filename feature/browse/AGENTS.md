# feature:browse Module

## Purpose

Top-level "Browse by attribute" destination. Users choose a dimension, choose one of its buckets, then
view the live apps in that bucket before navigating to app detail.

The API module exposes the top-level navigation key and tab label. The phone implementation uses the
package `sk.styk.martin.apkanalyzer.feature.browse.phone`; the TV one uses
`sk.styk.martin.apkanalyzer.feature.browse.tv`.

## Package Map

Both `phone` and `tv` share the same layout:

* The root package owns the hub and the dimension icons. The phone hub shows one card per dimension;
  the TV hub shows one row per dimension with its top options as tiles (certificates grouped by
  publisher), ranked user apps first for the icons, and a "See all" tile.
* `options/` owns searchable bucket lists and dimension-specific row presentation.
* `apps/` owns the searchable app list for one bucket and its bucket detail.
* `navigation/` owns internal option and bucket destinations.

Phone-only: `analytics/`, the browse events and their mapping to tracked names and parameters, which
TV does not track. On TV the options screen is a list beside a
preview pane that follows focus (the option's apps and bucket detail), so users browse by moving focus
and click only to open the full app grid. Search sits behind a header button rather than an
always-visible field. The bucket detail also sits in a side panel next to the app grid instead of
behind an info button, and certificate buckets show the fingerprint there because the title is the
publisher.

## Data and Navigation Semantics

The navigation depth is hub -> options -> apps -> app detail; on TV a hub tile skips straight to apps,
while its "See all" tile opens options. Internal keys carry the dimension and raw bucket identity,
not a package list.

The apps screen recombines the live index with installed apps. Uninstalling an app while viewing a
bucket must update the list, and navigation state must remain small.

`AppIndexStatus` has loading and data states only because indexing is pure bucketing over upstream
flows whose public contracts do not fail. Do not invent an error state without first introducing a
real failure source.

Normalize dimension keys for navigation, but preserve the raw key separately from the friendly
label. Labeling, bucket lookup, and bucket detail live in `core:app-index` `browse/`, shared by the
phone and TV modules; neither module recreates them.

Certificate identity supports SHA-256, SHA-1, and MD5 bucket views. Unknown certificate subject
attributes use an explicit sentinel rather than a nullable navigation parameter.
