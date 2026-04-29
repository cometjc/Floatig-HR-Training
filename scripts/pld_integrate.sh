#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <execution-id>" >&2
  exit 2
fi

execution="$1"
root="$(git rev-parse --show-toplevel)"
plans_dir="$root/docs/plans/$execution"

if [[ ! -d "$plans_dir" ]]; then
  echo "Execution plans directory not found: $plans_dir" >&2
  exit 1
fi

mapfile -t lane_files < <(ls "$plans_dir"/run-*-lane-*.md 2>/dev/null | sort)
if [[ ${#lane_files[@]} -eq 0 ]]; then
  echo "No lane plans found under $plans_dir" >&2
  exit 1
fi

branches=()
targets=()
for lane_file in "${lane_files[@]}"; do
  worktree_rel="$(sed -n 's/^PLD worktree: `\([^`]*\)`/\1/p' "$lane_file")"
  if [[ -z "$worktree_rel" ]]; then
    echo "Missing PLD worktree in $lane_file" >&2
    exit 1
  fi
  worktree="$root/$worktree_rel"
  branch="$(basename "$worktree_rel")"
  target="$branch"

  if [[ ! -d "$worktree" ]]; then
    echo "Worktree missing: $worktree" >&2
    exit 1
  fi

  echo "[integrate] rebase $branch on main"
  GIT_EDITOR=true git -C "$worktree" rebase main

  echo "[integrate] ff-only merge $branch -> main"
  git -C "$root" checkout main >/dev/null
  git -C "$root" merge --ff-only "$branch"

  branches+=("$branch")
  targets+=("$target")
done

echo "[integrate] coordinator plan-done"
for target in "${targets[@]}"; do
  if rg -q "^${target}:" "$root/docs/plan.md"; then
    just -f "$root/justfile" plan-done "$target"
  fi
done

echo "[integrate] cleanup branches/worktrees"
for branch in "${branches[@]}"; do
  wt="$root/.worktrees/$branch"
  if [[ -d "$wt" ]]; then
    git -C "$root" worktree remove "$wt"
  fi
  if git -C "$root" show-ref --verify --quiet "refs/heads/$branch"; then
    git -C "$root" branch -d "$branch"
  fi
done

git -C "$root" clean -fd -- .pld "docs/plans/$execution"
if [[ -d "$root/docs/plans" ]] && [[ -z "$(ls -A "$root/docs/plans")" ]]; then
  rmdir "$root/docs/plans"
fi

echo "[integrate] done"
