# core:app-index Module

## Purpose and Boundary

Builds device-wide `attribute -> apps` indexes for `feature:browse`. The package is
`sk.styk.martin.apkanalyzer.core.appindex`.

This is a pure bucketing layer over public `core:apps` repositories. It owns no `PackageManager`
access and must never reach into analysis internals.

## Index Semantics

The index covers target SDK, minimum SDK, install source, requested permission, shared UID, app
category, and current-signing-certificate identity and subject attributes.

* Apps without a shared UID are omitted from that dimension; do not create an "unshared" bucket.
* `AppCategory.Undefined` is a real bucket because an undeclared category is meaningful.
* Multi-signer apps appear under every current signer.
* Certificate fingerprint dimensions use certificate hashes, not signature algorithms.
* Organization and country come from the certificate subject, matching app-detail terminology.

The first dimensions reuse data already present on `InstalledApp`. Certificate grouping consumes the
heavier `AppSigningRepository` output.

## Execution and Lifecycle

Recompute from the installed-app and signing flows rather than subscribing separately to package
changes. Run CPU-bound grouping on `DispatcherProvider.default()`.

Share the combined index lazily with one replayed value. Certificate parsing and grouping should not
run until a real browse consumer subscribes, and concurrent collectors must not duplicate the work.

Uses-feature grouping remains out of scope until a real consumer justifies device-wide
`PackageManager` extraction. Add that source in `core:apps`, not in this module.
