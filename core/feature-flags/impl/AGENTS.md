# core:feature-flags:impl

Internal build-backed `FeatureFlagRepository` and build/variant default policy. Both app shells
link this module in every build. The `buildFeatureFlags` named binding is always available;
the `persistedFeatureFlags` named binding and editor binding are optional and supplied
by the debug module. A Hilt provider selects the build reader in release and the persisted reader
in phone debug, falling back to the build reader if storage is not linked. TV never links storage
and always selects the build reader.

The build reader never depends on storage. Release never selects the persisted reader, even if
accidentally supplied. The build-backed implementation returns false for every flag in release.
Its debug defaults are explicit per flag: inventory export is on for both variants; the three
history flags are on for phones and off for TV. There is no separate support set or policy.
The normal repository returns only Boolean values and never exposes editor status.
