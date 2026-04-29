#!/usr/bin/env python3

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PLAN_PATH = ROOT / "docs" / "plan.md"
SPECS_DIR = ROOT / "docs" / "specs"
PLAN_RE = re.compile(r"^(plan-\d+-[a-z0-9-]+):(.*)$")


@dataclass
class PlanEntry:
    target: str
    deps: list[str]
    body: list[str]

    @property
    def keyword(self) -> str:
        return self.target.split("-", 2)[2]


def read_plan() -> tuple[list[str], list[PlanEntry]]:
    lines = PLAN_PATH.read_text(encoding="utf-8").splitlines()
    preamble: list[str] = []
    entries: list[PlanEntry] = []
    index = 0

    while index < len(lines):
        line = lines[index]
        match = PLAN_RE.match(line)
        if match:
            break
        preamble.append(line)
        index += 1

    while index < len(lines):
        line = lines[index]
        match = PLAN_RE.match(line)
        if not match:
            index += 1
            continue

        target = match.group(1)
        deps = match.group(2).strip().split()
        index += 1

        body: list[str] = []
        while index < len(lines) and not PLAN_RE.match(lines[index]):
            body.append(lines[index])
            index += 1

        while body and body[-1] == "":
            body.pop()

        entries.append(PlanEntry(target=target, deps=deps, body=body))

    return preamble, entries


def write_plan(preamble: list[str], entries: list[PlanEntry]) -> None:
    out: list[str] = list(preamble)
    if out and out[-1] != "":
        out.append("")

    for idx, entry in enumerate(entries):
        deps = f" {' '.join(entry.deps)}" if entry.deps else ""
        out.append(f"{entry.target}:{deps}")
        out.extend(entry.body)
        if idx != len(entries) - 1:
            out.append("")

    PLAN_PATH.write_text("\n".join(out).rstrip() + "\n", encoding="utf-8")


def resolve_target(entries: list[PlanEntry], raw: str) -> PlanEntry:
    exact = [entry for entry in entries if entry.target == raw]
    if exact:
        return exact[0]

    prefixed = [entry for entry in entries if entry.target.startswith(f"{raw}-")]
    if len(prefixed) == 1:
        return prefixed[0]
    if len(prefixed) > 1:
        raise SystemExit(f"Ambiguous target prefix '{raw}': {[entry.target for entry in prefixed]}")

    suffix = [entry for entry in entries if entry.target.endswith(f"-{raw}")]
    if len(suffix) == 1:
        return suffix[0]
    if len(suffix) > 1:
        raise SystemExit(f"Ambiguous target keyword '{raw}': {[entry.target for entry in suffix]}")

    raise SystemExit(
        "Plan target not found: "
        f"{raw}. Use the full target, a `plan-NNN` prefix, or the keyword suffix."
    )


def spec_matches(keyword: str) -> list[Path]:
    lowered_keyword = keyword.lower()
    variants = {lowered_keyword, lowered_keyword.replace("-", " ")}
    matches: list[Path] = []

    for path in sorted(SPECS_DIR.glob("*.md")):
        text = path.read_text(encoding="utf-8").lower()
        if any(variant and variant in text for variant in variants):
            matches.append(path)

    return matches


def changed_spec_paths() -> list[Path]:
    result = subprocess.run(
        ["git", "status", "--short", "--", str(SPECS_DIR)],
        check=True,
        capture_output=True,
        text=True,
        cwd=ROOT,
    )
    changed: list[Path] = []

    for line in result.stdout.splitlines():
        if len(line) < 4:
            continue
        relative_path = line[3:].strip()
        if relative_path:
            changed.append(ROOT / relative_path)

    return changed


def command_next() -> int:
    _, entries = read_plan()
    available = [entry for entry in entries if not entry.deps]
    if not available:
        print("No dependency-free plans remain.")
        return 0

    for entry in available:
        summary = next((line.strip() for line in entry.body if line.strip()), "")
        print(entry.target)
        if summary:
            print(f"  {summary}")

    return 0


def recent_spec_paths(limit: int) -> list[Path]:
    result = subprocess.run(
        ["git", "log", f"-n{limit}", "--name-only", "--pretty=format:"],
        check=True,
        capture_output=True,
        text=True,
        cwd=ROOT,
    )
    paths: list[Path] = []
    for line in result.stdout.splitlines():
        cleaned = line.strip()
        if not cleaned:
            continue
        candidate = ROOT / cleaned
        if str(candidate).startswith(str(SPECS_DIR)):
            paths.append(candidate)
    return paths


def command_done(raw_target: str, allow_recent_commit: bool = False, recent_limit: int = 20) -> int:
    preamble, entries = read_plan()
    current = resolve_target(entries, raw_target)
    matches = spec_matches(current.keyword)
    if not matches:
        raise SystemExit(
            "Plan keyword not found in docs/specs. "
            f"Expected keyword '{current.keyword}' before removing {current.target}."
        )
    changed_matches = [path for path in matches if path in changed_spec_paths()]
    if not changed_matches and allow_recent_commit:
        recent_matches = [path for path in matches if path in recent_spec_paths(recent_limit)]
        if recent_matches:
            changed_matches = recent_matches
    if not changed_matches:
        raise SystemExit(
            "Plan keyword found in docs/specs, but no staged or unstaged spec changes "
            f"currently include '{current.keyword}'. Update a matching spec before removing {current.target}."
        )

    remaining = [entry for entry in entries if entry.target != current.target]
    for entry in remaining:
        entry.deps = [dep for dep in entry.deps if dep != current.target]

    write_plan(preamble, remaining)

    print(f"Removed {current.target}")
    print(f"Verified changed keyword '{current.keyword}' in:")
    for path in changed_matches:
        print(f"  {path.relative_to(ROOT)}")

    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Plan helpers for docs/plan.md")
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("next", help="List plans with no remaining dependencies")

    done_parser = subparsers.add_parser("done", help="Remove a completed plan target")
    done_parser.add_argument("target", help="Full target or keyword suffix")
    done_parser.add_argument(
        "--allow-recent-commit",
        action="store_true",
        help="Allow recently committed matching spec changes to satisfy done checks.",
    )
    done_parser.add_argument(
        "--recent-limit",
        type=int,
        default=20,
        help="How many recent commits to inspect when --allow-recent-commit is enabled.",
    )

    args = parser.parse_args()

    if args.command == "next":
        return command_next()
    if args.command == "done":
        return command_done(args.target, args.allow_recent_commit, args.recent_limit)

    return 1


if __name__ == "__main__":
    sys.exit(main())
