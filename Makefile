.PHONY: ci workspace-gate pre-merge status status-control certify-run run-readiness run-readiness-control run-readiness-tree-control run-readiness-resolution-control run-readiness-serving-code-control run-readiness-workspace-receipt-control runner-reload-preflight

# C216: `make` exits 2 for any failing recipe, and 2 means "mutation slipped"
# in the 0-pass/1-fail/2-mutation-slipped convention these scripts print.
# Each wrapped target therefore reports its script's own exit code on a
# named line; read that line, not make's.

# Hermetic repository boundary: futon2 sources, tests, and build only.
ci:
	clojure -T:build ci

# Four-repository reviewer boundary. Missing sibling repositories are loud
# failures in the checks; they are never silently skipped.
workspace-gate:
	@python3 scripts/run_workspace_gate_bounded.py; c=$$?; \
	 echo "workspace-gate: script-exit=$$c (house convention; make reports 2 for any nonzero)"; \
	 exit $$c

# Required War Machine pre-merge/reviewer command. Keep the two boundaries
# explicit so ordinary futon2 CI remains usable in an isolated checkout.
pre-merge:
	$(MAKE) ci
	$(MAKE) workspace-gate

# Generated, non-short-circuiting operational report. New/unaccepted red is
# nonzero; exact, referenced, expiring accepted red remains visible at exit 0.
status:
	@python3 scripts/wm_status_report.py; c=$$?; \
	 echo "status: script-exit=$$c (house convention; make reports 2 for any nonzero)"; \
	 exit $$c

status-control:
	@python3 scripts/wm_status_report.py --source-control; c=$$?; \
	 echo "status-control: script-exit=$$c (house convention; make reports 2 for any nonzero)"; \
	 exit $$c

# Certify one completed operator/diagnostic run without guessing "latest".
# TESTED_JOB_ID is the Futon2 CI bounded-job id recorded by tested-commit.
# Usage: make certify-run RUN_ID=<uuid> TESTED_JOB_ID=<bounded-id> FENCE_ID=<quiet-run-id>
certify-run:
	@test -n "$(RUN_ID)" || (echo "certify-run: RUN_ID is required" >&2; exit 1)
	@test -n "$(TESTED_JOB_ID)" || (echo "certify-run: TESTED_JOB_ID is required" >&2; exit 1)
	@test -n "$(FENCE_ID)" || (echo "certify-run: FENCE_ID is required" >&2; exit 1)
	bb -cp . checks/certify_live_run.clj --run-id "$(RUN_ID)" --tested-job-id "$(TESTED_JOB_ID)" --fence-id "$(FENCE_ID)"

# Read-only operator preflight. It runs checks and reads receipts/roster state;
# it never starts a tick or dispatches an agent.
run-readiness:
	@python3 scripts/run_readiness.py; c=$$?; \
	 echo "run-readiness: script-exit=$$c (house convention; make reports 2 for any nonzero)"; \
	 exit $$c

run-readiness-control:
	@python3 scripts/run_readiness.py --negative-reviewer; c=$$?; \
	 echo "run-readiness-control: script-exit=$$c (house convention; make reports 2 for any nonzero)"; \
	 exit $$c

run-readiness-tree-control:
	@python3 scripts/run_readiness.py --tree-control; c=$$?; \
	 echo "run-readiness-tree-control: script-exit=$$c (house convention; make reports 2 for any nonzero)"; \
	 exit $$c

run-readiness-resolution-control:
	@python3 scripts/run_readiness.py --resolution-control; c=$$?; \
	 echo "run-readiness-resolution-control: script-exit=$$c (house convention; make reports 2 for any nonzero)"; \
	 exit $$c

run-readiness-serving-code-control:
	@python3 scripts/run_readiness.py --serving-code-control; c=$$?; \
	 echo "run-readiness-serving-code-control: script-exit=$$c (house convention; make reports 2 for any nonzero)"; \
	 exit $$c

run-readiness-workspace-receipt-control:
	@python3 scripts/run_readiness.py --workspace-gate-receipt-control; c=$$?; \
	 echo "run-readiness-workspace-receipt-control: script-exit=$$c (house convention; make reports 2 for any nonzero)"; \
	 exit $$c

# Preparation only. Prints the canonical dev-admin reload command iff every
# clean-tree/tested-commit precondition passes; never contacts the admin route.
runner-reload-preflight:
	@python3 scripts/prepare_runner_reload.py; c=$$?; \
	 echo "runner-reload-preflight: script-exit=$$c (house convention; make reports 2 for any nonzero)"; \
	 exit $$c
