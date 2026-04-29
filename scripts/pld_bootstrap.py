#!/usr/bin/env python3

from __future__ import annotations

import argparse
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PLAN_MD = ROOT / "docs" / "plan.md"
PLANS_ROOT = ROOT / "docs" / "plans"
PLAN_LINE_RE = re.compile(r"^(plan-\d+-[a-z0-9-]+):(.*)$")


def plan_candidates() -> list[str]:
    lines = PLAN_MD.read_text(encoding="utf-8").splitlines()
    targets: list[str] = []
    for line in lines:
        match = PLAN_LINE_RE.match(line)
        if not match:
            continue
        deps = [d for d in match.group(2).strip().split() if d]
        if not deps:
            targets.append(match.group(1))
    return targets


def lane_markdown(lane_no: int, target: str) -> str:
    keyword = target.split("-", 2)[2]
    return f"""# Lane {lane_no}

PLD worktree: `.worktrees/{target}`

> Ownership family:
> `app/`
> `docs/specs/`

> Lane-local verification required:
> `./gradlew :app:compileDebugKotlin`

> Lane-local verification advisory:
> `./gradlew :app:testDebugUnitTest`

## Tasks

- [ ] Implement `{target}` in its dedicated worktree/branch.
- [ ] Update `docs/specs/*.md` and include keyword `{keyword}`.
- [ ] Commit lane changes with a conventional commit message.
"""


def run(*args: str) -> None:
    subprocess.run(args, cwd=ROOT, check=True)


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate PLD lane plans from docs/plan.md")
    parser.add_argument("--execution", required=True, help="Execution id under docs/plans/")
    args = parser.parse_args()

    targets = plan_candidates()
    if not targets:
        print("No dependency-free plan targets found.")
        return 0

    execution_dir = PLANS_ROOT / args.execution
    execution_dir.mkdir(parents=True, exist_ok=True)

    for idx, target in enumerate(targets, start=1):
        path = execution_dir / f"run-next-batch-lane-{idx}.md"
        path.write_text(lane_markdown(idx, target), encoding="utf-8")

    pld = str(Path.home() / ".agents" / "skills" / "pld" / "scripts" / "pld.cjs")
    run("node", pld, "--role", "coordinator", "import-plans", "--json")
    run("node", pld, "--role", "coordinator", "audit", "--json")
    run("node", pld, "--role", "coordinator", "go", "--json")

    print(f"Prepared execution '{args.execution}' with {len(targets)} lanes.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
