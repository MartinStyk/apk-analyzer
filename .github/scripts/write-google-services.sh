#!/usr/bin/env bash

set -euo pipefail

readonly google_services_file="app/google-services.json"

temporary_file=""

cleanup() {
  if [[ -n "$temporary_file" ]]; then
    rm -f -- "$temporary_file"
  fi
}

trap cleanup EXIT

rm -f -- "$google_services_file"
umask 077
temporary_file="$(mktemp "${google_services_file}.XXXXXX")"
printf '%s' "${GOOGLE_SERVICES:?GOOGLE_SERVICES is required}" > "$temporary_file"
unset GOOGLE_SERVICES

if ! jq --exit-status \
  'type == "object" and (.project_info | type == "object") and (.client | type == "array" and length > 0)' \
  "$temporary_file" >/dev/null 2>&1; then
  echo "GOOGLE_SERVICES must contain a valid google-services.json object" >&2
  exit 1
fi

chmod 600 "$temporary_file"
mv "$temporary_file" "$google_services_file"
temporary_file=""
