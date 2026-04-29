#!/usr/bin/env bash
set -euo pipefail

exec "$HOME/.agents/skills/pld/scripts/ensure_git_context.sh" "$@"
