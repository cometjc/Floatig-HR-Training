default:
    @just --list

plan-ready:
    python3 scripts/plan_tools.py next

plan-next:
    @just plan-ready

plan-done target:
    python3 scripts/plan_tools.py done {{target}}

plan-complete target:
    @just plan-done {{target}}
