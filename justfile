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

pld-preflight:
    ./scripts/pld_preflight.sh

pld-bootstrap execution:
    python3 scripts/pld_bootstrap.py --execution {{execution}}

pld-integrate execution:
    ./scripts/pld_integrate.sh {{execution}}
