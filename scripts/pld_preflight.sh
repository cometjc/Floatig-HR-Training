#!/usr/bin/env bash
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

echo "[preflight] repo: $ROOT"

if [[ -f local.properties ]]; then
  if ! rg -q "^sdk\\.dir=" local.properties; then
    echo "[preflight] FAIL: local.properties exists but sdk.dir missing" >&2
    exit 1
  fi
else
  if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
    echo "[preflight] FAIL: local.properties missing and ANDROID_HOME/ANDROID_SDK_ROOT not set" >&2
    exit 1
  fi
fi

if [[ ! -d "ble-provider/polar-ble-sdk" ]]; then
  echo "[preflight] FAIL: missing submodule directory ble-provider/polar-ble-sdk" >&2
  exit 1
fi

if [[ -n "$(git status --short)" ]]; then
  echo "[preflight] FAIL: working tree not clean" >&2
  git status --short
  exit 1
fi

echo "[preflight] checking gradle variant health..."
./gradlew :app:compileDebugKotlin >/dev/null

echo "[preflight] PASS"
