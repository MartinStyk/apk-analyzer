# core:app-index Module

## Purpose and Boundary

Builds device-wide `attribute -> apps` indexes for `feature:browse`. It is split per
[core API/impl split](../../docs/app/technical/core-api-impl-split.md): `api`
(`sk.styk.martin.apkanalyzer.core.appindex.api`) holds `AppIndexRepository`, the index models, and
the `browse/` contract; `impl` (`sk.styk.martin.apkanalyzer.core.appindex.impl`) holds the internal
repository, `BrowseDimensionLabelerImpl`, `BrowseBucketDetailResolverImpl`, the label strings only
they read, and the Hilt module.

The index itself is a pure bucketing layer over public `core:apps` repositories. It owns no
`PackageManager` access and must never reach into analysis internals.

`browse/` holds everything the phone and TV Browse screens share on top of the index: the
`BrowseDimension` and `BrowseSubAttribute` enums, bucket lookup, `BrowseDimensionLabeler` (friendly
labels and raw identifiers), hub summaries, option building and matching, and
`BrowseBucketDetailResolver`, plus their localized strings. Each app keeps only its own layout, icons,
and screen copy; a rule or label both Browse screens show goes here. Screen copy both Browse apps
show, such as counts and empty-search text, lives in `feature:browse`'s `domain`.

## Index Semantics

The index covers target SDK, minimum SDK, install source, requested permission, shared UID, app
category, and current-signing-certificate identity and subject attributes.

* Apps without a shared UID are omitted from that dimension; do not create an "unshared" bucket.
* `AppCategory.Undefined` is a real bucket because an undeclared category is meaningful.
* Multi-signer apps appear under every current signer.
* Certificate fingerprint dimensions use certificate hashes, not signature algorithms.
* Organization and country come from the certificate subject, matching app-detail terminology.
* `certificateByHash` carries one representative `Certificate` per SHA-256/SHA-1/MD5 hash so
  `feature:browse` can show per-bucket detail (algorithm strength, self-signed) without a second
  device-wide signing scan. First-seen wins; apps sharing a certificate hash are assumed to share its
  algorithm and self-signed status too.

The first dimensions reuse data already present on `InstalledApp`. Certificate grouping consumes the
heavier `AppSigningRepository` output.

## Execution and Lifecycle

Recompute from the installed-app and signing flows rather than subscribing separately to package
changes. Run CPU-bound grouping on `DispatcherProvider.default()`.

Share the combined index lazily with one replayed value. Certificate parsing and grouping should not
run until a real browse consumer subscribes, and concurrent collectors must not duplicate the work.

Uses-feature grouping remains out of scope until a real consumer justifies device-wide
`PackageManager` extraction. Add that source in `core:apps`, not in this module.
