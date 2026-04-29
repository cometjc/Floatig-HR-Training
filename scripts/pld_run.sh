#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <execution-id>" >&2
  exit 2
fi

execution="$1"
root="$(git rev-parse --show-toplevel)"
start="$(date +%s)"

echo "[pld-run] execution=$execution"

(
  cd "$root"
  just pld-preflight
  just pld-bootstrap "$execution"
  just pld-integrate "$execution" --resume
)

end="$(date +%s)"
elapsed=$((end - start))
mins=$((elapsed / 60))
secs=$((elapsed % 60))

echo "[pld-run] total_seconds=$elapsed"
echo "[pld-run] total_human=${mins}m${secs}s"
