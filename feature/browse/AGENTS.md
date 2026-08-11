# feature:browse Module

## Purpose

Top-level "Browse by attribute" destination. Users choose a dimension, choose one of its buckets, then
view the live apps in that bucket before navigating to app detail.

The API module exposes the top-level navigation key and tab label. Implementation code uses the
package `sk.styk.martin.apkanalyzer.feature.browse.impl`.

## Package Map

* The root implementation package owns dimension models, the hub, shared labeling, and bucket
  normalization.
* `options/` owns searchable bucket lists and dimension-specific row presentation.
* `apps/` owns the searchable app list for one bucket.
* `navigation/` owns internal option and bucket destinations.

## Data and Navigation Semantics

The navigation depth is hub -> options -> apps -> app detail. Internal keys carry the dimension and
raw bucket identity, not a package list.

The apps screen recombines the live index with installed apps. Uninstalling an app while viewing a
bucket must update the list, and navigation state must remain small.

`AppIndexStatus` has loading and data states only because indexing is pure bucketing over upstream
flows whose public contracts do not fail. Do not invent an error state without first introducing a
real failure source.

Normalize dimension keys for navigation, but preserve the raw key separately from the friendly
label. Permission and SDK labels use core resolvers; enum-backed dimensions use this feature's
localized resources outside Compose.

Certificate identity supports SHA-256, SHA-1, and MD5 bucket views. Unknown certificate subject
attributes use an explicit sentinel rather than a nullable navigation parameter.
