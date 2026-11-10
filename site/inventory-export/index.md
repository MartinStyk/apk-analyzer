---
title: App inventory export format
description: Columns, values, and the JSON Schema of the CSV and JSON files Apk Analyzer exports.
---

# App inventory export format

Apk Analyzer exports information about your installed apps as a CSV or JSON file. This page lists
every column, the values it can hold, and how both formats write them, so you can read the file in
a spreadsheet or process it with a script.

**JSON Schema:** [`v1/schema.json`](v1/schema.json)

## How the files are built

* Each row (CSV) or object (JSON) is one app. Apps are sorted by name.
* A file holds only the columns you selected, always in the order of the [column table](#columns),
  whatever order you picked them in.
* A value is empty (CSV) or `null` (JSON) when it isn't available for that app, for example when
  the app's details can't be read or usage access isn't granted. An empty list means the app
  has none: `[]` in JSON, an empty cell in CSV.
* Column keys and the values listed on this page don't change within a format version. New
  columns and new values can be added; read unknown keys and values as "not recognized" rather
  than failing.

## JSON

The file is one object:

```json
{
  "$schema": "https://martinstyk.github.io/apk-analyzer/inventory-export/v1/schema.json",
  "schema_version": 1,
  "generated_at": "2026-11-03T14:49:12.345Z",
  "apps": [
    {
      "application_name": "Chrome",
      "package_name": "com.android.chrome",
      "version_name": "126.0.6478.122",
      "version_code": 647812233,
      "signing_schemes": ["v2", "v3"]
    }
  ]
}
```

* `schema_version` changes only when a change would break existing scripts. That version gets a new
  schema at a new address; this one stays as it is.
* `generated_at` is when the export ran.
* Numbers are JSON numbers, true / false values are JSON booleans, and lists are JSON arrays.
* Columns marked **No** under CSV hold nested records, described in [Records](#records).

## CSV

* UTF-8, comma-separated, with `\r\n` line endings. The first row holds the column keys.
* Cells containing a comma, quote, or line break are quoted, and quotes inside them are doubled.
* Lists are joined into one cell with `; `.
* A cell that would start with `=`, `+`, `-`, `@`, a tab, or a line break gets a leading `'`,
  so a spreadsheet shows it as text instead of running it as a formula. Strip the `'` if you
  process the file with a script.
* Columns marked **No** under CSV hold nested records and are only available in JSON. If you
  select them with CSV, the app tells you they are left out.

## Value formats

| Value | Format |
|---|---|
| timestamp | ISO-8601 instant in UTC, for example `2026-11-03T14:49:12.345Z` |
| fingerprint | Lowercase hexadecimal digest with no separators, for example `3b8f…e01a`. The same format as `apksigner verify --print-certs`. |
| number | Whole number. Sizes are in bytes. |

## Columns

| Key | Contains | Value | CSV |
|---|---|---|---|
| `application_name` | App name shown on the device. | text | Yes |
| `package_name` | Unique Android package name. | text | Yes |
| `process_name` | Default process the app runs in. | text, or empty | Yes |
| `uid` | Linux user ID assigned to the app. | number, or empty | Yes |
| `shared_user_id` | Shared user ID declared in the manifest, if any. | text, or empty | Yes |
| `category` | Category the app declares; null when it declares none. | text, one of `game`, `audio`, `video`, `image`, `social`, `news`, `maps`, `productivity`, `accessibility`, or empty | Yes |
| `description` | Description the app declares in its manifest. | text, or empty | Yes |
| `version_name` | Version shown to users. | text, or empty | Yes |
| `version_code` | Internal version number. | number | Yes |
| `min_sdk` | Lowest Android API level the app supports. | number | Yes |
| `target_sdk` | Android API level the app targets. | number | Yes |
| `app_type` | Whether the app is preinstalled with the system or installed by the user. | text, one of `system`, `user` | Yes |
| `install_source` | Where the app was installed from. | text, one of `google_play`, `samsung_galaxy_store`, `amazon_appstore`, `huawei_app_gallery`, `xiaomi_get_apps`, `f_droid`, `aurora_store`, `sideloaded`, `local_install`, `system_preinstalled`, `unknown` | Yes |
| `installer_package` | Package that installed the app. | text, or empty | Yes |
| `install_initiator` | Package that started the installation. | text, or empty | Yes |
| `install_originator` | Package the installation originally came from. | text, or empty | Yes |
| `first_installed` | When the app was first installed. | timestamp | Yes |
| `last_updated` | When the app was last updated. | timestamp | Yes |
| `last_used` | When the app was last used. Needs usage access. | timestamp, or empty | Yes |
| `apk_size_bytes` | Size of the app's APK files in bytes. | number | Yes |
| `total_size_bytes` | App, data, and cache size in bytes. Needs usage access. | number, or empty | Yes |
| `split_apks` | File names of installed split APKs. | list of text, or empty | Yes |
| `split_apk_details` | Installed split APKs with size and kind. | list of [split_apk](#split_apk) records, or empty | No |
| `install_location` | Preferred install location from the manifest. | text, one of `auto`, `internal_only`, `prefer_external`, or empty | Yes |
| `apk_directory` | Directory holding the app's APK files. | text, or empty | Yes |
| `data_directory` | Directory holding the app's private data. | text, or empty | Yes |
| `native_library_abis` | CPU architectures the app ships native code for, sorted. | list of text, or empty | Yes |
| `native_library_files` | Every bundled native library. | list of [native_library](#native_library) records, or empty | No |
| `debuggable` | Whether the app can be debugged. | true / false, or empty | Yes |
| `allows_backup` | Whether the app allows backup of its data. | true / false, or empty | Yes |
| `cleartext_traffic` | Whether the app allows unencrypted network traffic. | true / false, or empty | Yes |
| `signing_certificate_sha256` | SHA-256 fingerprint of each current signing certificate. | list of fingerprints, or empty | Yes |
| `signing_certificate_sha1` | SHA-1 fingerprint of each current signing certificate. | list of fingerprints, or empty | Yes |
| `signing_certificate_md5` | MD5 fingerprint of each current signing certificate. | list of fingerprints, or empty | Yes |
| `signing_public_key_sha256` | SHA-256 fingerprint of each current signer's public key. | list of fingerprints, or empty | Yes |
| `signing_public_key_sha1` | SHA-1 fingerprint of each current signer's public key. | list of fingerprints, or empty | Yes |
| `signing_public_key_md5` | MD5 fingerprint of each current signer's public key. | list of fingerprints, or empty | Yes |
| `signing_certificate_subject` | Organization, or name when there is none, of each current signer. | list of text, or empty | Yes |
| `has_multiple_signers` | Whether the app is signed by more than one certificate. | true / false, or empty | Yes |
| `signing_schemes` | APK signature schemes the app is signed with. | list of `v1`, `v2`, `v3`, `v3.1`, or empty | Yes |
| `signing_certificates` | Full records of the current signing certificates. | list of [certificate](#certificate) records, or empty | No |
| `signing_certificate_history` | Full records of past certificates from key rotation. | list of [certificate](#certificate) records, or empty | No |
| `requested_permissions` | Permissions the app requests. | list of text | Yes |
| `granted_permissions` | Requested permissions that are currently granted. | list of text, or empty | Yes |
| `defined_permissions` | Permissions the app defines for other apps. | list of [permission](#permission) records, or empty | No |
| `permission_details` | Requested permissions with details and grant state. | list of [requested_permission](#requested_permission) records, or empty | No |
| `activity_count` | Number of activities. | number, or empty | Yes |
| `service_count` | Number of services. | number, or empty | Yes |
| `receiver_count` | Number of broadcast receivers. | number, or empty | Yes |
| `provider_count` | Number of content providers. | number, or empty | Yes |
| `activities` | Every activity. | list of [activity](#activity) records, or empty | No |
| `services` | Every service. | list of [service](#service) records, or empty | No |
| `receivers` | Every broadcast receiver. | list of [receiver](#receiver) records, or empty | No |
| `providers` | Every content provider. | list of [provider](#provider) records, or empty | No |
| `intent_filters` | Intent filters grouped by component. | list of [component_intent_filters](#component_intent_filters) records, or empty | No |
| `features` | Hardware and OpenGL ES features the app uses. | list of [feature](#feature) records, or empty | No |
| `manifest` | Readable AndroidManifest.xml. | [manifest](#manifest) record, or empty | No |

## Records

Every field of a record is always present. Fields marked *or null* can be `null`.

### split_apk

| Field | Value |
|---|---|
| `file_name` | text |
| `file_path` | text, or null |
| `size_bytes` | number |
| `kind` | `dynamic_feature`, `abi`, `screen_density`, or `language` |
| `qualifier` | text, for example `arm64_v8a` or `en` |

### native_library

| Field | Value |
|---|---|
| `name` | text, for example `libchrome.so` |
| `abi` | text, for example `arm64-v8a` |
| `size_bytes` | number |
| `containing_apk` | file name of the APK holding the library |

### certificate

| Field | Value |
|---|---|
| `signature_algorithm` | text, for example `SHA256withRSA` |
| `certificate_md5`, `certificate_sha1`, `certificate_sha256` | fingerprint of the certificate |
| `public_key_md5`, `public_key_sha1`, `public_key_sha256` | fingerprint of the public key |
| `valid_from`, `valid_until` | timestamp |
| `serial_number` | text, colon-separated uppercase hexadecimal |
| `issuer`, `subject` | object with `name`, `organization`, and `country`, each text or null |
| `trust_level` | `valid`, or `debug` for the Android debug certificate |
| `self_signed` | true / false |

### permission

| Field | Value |
|---|---|
| `name` | text, for example `android.permission.CAMERA` |
| `details` | null when the permission isn't known on this device, otherwise an object: |
| `details.group_name` | text, or null |
| `details.protection_level` | `dangerous`, `signature`, `internal`, or `normal` |
| `details.protection_flags` | list of `privileged`, `app_op`, `instant`, `development` |
| `details.description` | text, or null |
| `details.declaring_package` | package that defines the permission |

### requested_permission

| Field | Value |
|---|---|
| `permission` | [permission](#permission) record |
| `granted` | true / false |

### activity

| Field | Value |
|---|---|
| `name` | class name |
| `package_name` | package the activity belongs to |
| `label`, `target_activity`, `permission`, `parent_activity` | text, or null |
| `exported` | true / false |
| `launcher` | true / false, or null when unknown |

### service

| Field | Value |
|---|---|
| `name` | class name |
| `permission` | text, or null |
| `exported`, `stop_with_task`, `single_user`, `isolated_process`, `external_service` | true / false |

### receiver

| Field | Value |
|---|---|
| `name` | class name |
| `permission` | text, or null |
| `exported` | true / false |

### provider

| Field | Value |
|---|---|
| `name` | class name |
| `authority`, `read_permission`, `write_permission` | text, or null |
| `exported` | true / false |
| `path_permissions` | list of objects with `path`, `match_type` (`literal`, `prefix`, `simple_glob`, `advanced_glob`, or `suffix`), `read_permission`, and `write_permission` |

### component_intent_filters

| Field | Value |
|---|---|
| `component_name` | class name of the component |
| `component_kind` | `activity`, `service`, `receiver`, or `provider` |
| `filters` | list of intent filters: |
| `filters[].actions`, `filters[].categories` | list of text |
| `filters[].data_rules` | list of objects with `type` (the manifest attribute, such as `scheme`, `host`, `pathPrefix`, or `mimeType`) and `value` |
| `filters[].uri_relative_groups` | list of objects with `allowed` (true / false) and `data_rules` |
| `filters[].priority`, `filters[].order` | number |
| `filters[].auto_verify` | true / false |

### feature

A hardware feature:

| Field | Value |
|---|---|
| `kind` | `hardware` |
| `name` | text, for example `android.hardware.camera` |
| `version` | number, `0` when unspecified |
| `required` | true / false |

An OpenGL ES requirement:

| Field | Value |
|---|---|
| `kind` | `opengl_es` |
| `version` | number as declared in the manifest, for example `196608` |
| `version_name` | text, for example `3.0` |
| `required` | true / false |

### manifest

| Field | Value |
|---|---|
| `xml` | the readable AndroidManifest.xml |
| `additional_installed_splits` | number of installed split APKs whose manifests aren't included |
