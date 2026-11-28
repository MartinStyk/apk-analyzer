# core:feature-flags:api

Public feature flag contracts and test fixtures. Flags are Boolean capabilities, represented by
`FeatureFlag`, not generic configuration keys. `ApplicationVariant` is provided by each app shell.
Reads return Boolean values and editor observations return status lists; mutations return Unit.
Both contracts propagate storage exceptions rather than returning `Result`, matching
`PersistenceRepository`. This is an intentional exception to the general non-throwing repository rule.

The read repository exposes only effective Boolean values through per-flag observation and
one-shot reads. The optional editor repository owns status metadata (default and override origin)
and set/reset operations. Never add editor metadata to the normal reader.
The build implementation owns defaults; the API only declares flags
and contracts.
Consumers never infer defaults from flag names.
