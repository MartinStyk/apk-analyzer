#!/usr/bin/env bash

set -euo pipefail

readonly firebase_version="15.26.0"
readonly firebase_sha256="d6eb340cb4f06dcf5a3015ff588d826ab3bdce75b25855a25866af182417afe7"
readonly firebase_install_dir="${RUNNER_TEMP:?RUNNER_TEMP is required}/firebase-tools-${firebase_version}"
readonly firebase_bin="${firebase_install_dir}/firebase"

download_file=""
credentials_file=""

cleanup() {
  if [[ -n "$download_file" ]]; then
    rm -f -- "$download_file"
  fi
  if [[ -n "$credentials_file" ]]; then
    rm -f -- "$credentials_file"
  fi
}

trap cleanup EXIT

if [[ ! -x "$firebase_bin" ]]; then
  mkdir -p "$firebase_install_dir"
  download_file="$(mktemp "${firebase_bin}.download.XXXXXX")"
  curl \
    --fail \
    --location \
    --proto '=https' \
    --retry 3 \
    --show-error \
    --silent \
    --tlsv1.2 \
    "https://github.com/firebase/firebase-tools/releases/download/v${firebase_version}/firebase-tools-linux" \
    --output "$download_file"
  echo "${firebase_sha256}  ${download_file}" | sha256sum --check -
  chmod +x "$download_file"
  mv "$download_file" "$firebase_bin"
  download_file=""
fi

umask 077
credentials_file="$(mktemp "${RUNNER_TEMP}/firebase-credentials.XXXXXX.json")"
printf '%s' "${GOOGLE_SERVICE_ACCOUNT:?GOOGLE_SERVICE_ACCOUNT is required}" > "$credentials_file"
export GOOGLE_APPLICATION_CREDENTIALS="$credentials_file"
unset GOOGLE_SERVICE_ACCOUNT

firebase_exit=0
"$firebase_bin" "$@" || firebase_exit=$?

if ((firebase_exit != 0)) && [[ -f firebase-debug.log ]]; then
  api_status="$(sed -n 's/.*\[apiv2\]\[status\].* \([0-9][0-9][0-9]\)$/\1/p' firebase-debug.log | tail -n 1)"
  if [[ -n "$api_status" ]]; then
    echo "Firebase API request failed with HTTP ${api_status}" >&2
  fi
fi

exit "$firebase_exit"
