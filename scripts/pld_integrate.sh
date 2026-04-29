#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 <execution-id> [--resume] [--keep-artifacts] [--plan-done-mode coordinator-only|skip]" >&2
}

if [[ $# -lt 1 ]]; then
  usage
  exit 2
fi

execution="$1"
shift

resume=false
keep_artifacts=false
plan_done_mode="coordinator-only"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --resume)
      resume=true
      shift
      ;;
    --keep-artifacts)
      keep_artifacts=true
      shift
      ;;
    --plan-done-mode)
      plan_done_mode="${2:-}"
      shift 2
      ;;
    *)
      usage
      exit 2
      ;;
  esac
done

if [[ "$plan_done_mode" != "coordinator-only" && "$plan_done_mode" != "skip" ]]; then
  echo "Invalid --plan-done-mode: $plan_done_mode" >&2
  exit 2
fi

root="$(git rev-parse --show-toplevel)"
plans_dir="$root/docs/plans/$execution"
state_dir="$root/.pld-run-state"
state_file="$state_dir/$execution.state"
mkdir -p "$state_dir"

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
done_rebase=()
done_merge=()
done_plan=()

if [[ "$resume" == true && -f "$state_file" ]]; then
  while IFS='|' read -r phase value; do
    case "$phase" in
      rebase) done_rebase+=("$value") ;;
      merge) done_merge+=("$value") ;;
      plan) done_plan+=("$value") ;;
    esac
  done < "$state_file"
fi

has_done() {
  local value="$1"
  shift
  local arr=("$@")
  for item in "${arr[@]}"; do
    [[ "$item" == "$value" ]] && return 0
  done
  return 1
}

mark_done() {
  local phase="$1"
  local value="$2"
  printf '%s|%s\n' "$phase" "$value" >> "$state_file"
}

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

  if ! has_done "$branch" "${done_rebase[@]}"; then
    echo "[integrate] rebase $branch on main"
    GIT_EDITOR=true git -C "$worktree" rebase main
    mark_done "rebase" "$branch"
  fi

  if ! has_done "$branch" "${done_merge[@]}"; then
    echo "[integrate] ff-only merge $branch -> main"
    git -C "$root" checkout main >/dev/null
    git -C "$root" merge --ff-only "$branch"
    mark_done "merge" "$branch"
  fi

  branches+=("$branch")
  targets+=("$target")
done

if [[ "$plan_done_mode" == "coordinator-only" ]]; then
  echo "[integrate] coordinator plan-done"
  for target in "${targets[@]}"; do
    if has_done "$target" "${done_plan[@]}"; then
      continue
    fi
    if rg -q "^${target}:" "$root/docs/plan.md"; then
      just -f "$root/justfile" plan-done "$target" --allow-recent-commit
    fi
    mark_done "plan" "$target"
  done
fi

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

if [[ "$keep_artifacts" != true ]]; then
  git -C "$root" clean -fd -- .pld "docs/plans/$execution"
  if [[ -d "$root/docs/plans" ]] && [[ -z "$(ls -A "$root/docs/plans")" ]]; then
    rmdir "$root/docs/plans"
  fi
  rm -f "$state_file"
fi

echo "[integrate] done"
