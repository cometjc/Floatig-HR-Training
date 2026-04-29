default:
    @just --list

plan-ready:
    python3 scripts/plan_tools.py next

plan-next:
    @just plan-ready

plan-done target:
    python3 scripts/plan_tools.py done {{target}}

plan-done-recent target:
    python3 scripts/plan_tools.py done {{target}} --allow-recent-commit

plan-complete target:
    @just plan-done {{target}}

pld-preflight:
    bash -lc 'root="$$(git rev-parse --show-toplevel)"; pld_root="$${PLD_SKILL_ROOT:-$$HOME/.agents/skills/pld}"; export PATH="$$pld_root/scripts:$$PATH"; if command -v pld_preflight.sh >/dev/null 2>&1; then pld_preflight.sh --project-root "$$root"; else "$$pld_root/scripts/pld_preflight.sh" --project-root "$$root"; fi'

pld-bootstrap execution:
    bash -lc 'root="$$(git rev-parse --show-toplevel)"; pld_root="$${PLD_SKILL_ROOT:-$$HOME/.agents/skills/pld}"; export PATH="$$pld_root/scripts:$$PATH"; if command -v pld_bootstrap.py >/dev/null 2>&1; then pld_bootstrap.py --execution {{execution}} --project-root "$$root"; else python3 "$$pld_root/scripts/pld_bootstrap.py" --execution {{execution}} --project-root "$$root"; fi'

pld-integrate execution +opts:
    bash -lc 'root="$$(git rev-parse --show-toplevel)"; pld_root="$${PLD_SKILL_ROOT:-$$HOME/.agents/skills/pld}"; export PATH="$$pld_root/scripts:$$PATH"; if command -v pld_integrate.sh >/dev/null 2>&1; then pld_integrate.sh --project-root "$$root" {{execution}} {{opts}}; else "$$pld_root/scripts/pld_integrate.sh" --project-root "$$root" {{execution}} {{opts}}; fi'

pld-run execution:
    bash -lc 'root="$$(git rev-parse --show-toplevel)"; pld_root="$${PLD_SKILL_ROOT:-$$HOME/.agents/skills/pld}"; export PATH="$$pld_root/scripts:$$PATH"; if command -v pld_run.sh >/dev/null 2>&1; then pld_run.sh --project-root "$$root" {{execution}}; else "$$pld_root/scripts/pld_run.sh" --project-root "$$root" {{execution}}; fi'
