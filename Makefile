.PHONY: ci workspace-gate pre-merge status status-control certify-run run-readiness run-readiness-control run-readiness-tree-control

# Hermetic repository boundary: futon2 sources, tests, and build only.
ci:
	clojure -T:build ci

# Four-repository reviewer boundary. Missing sibling repositories are loud
# failures in the checks; they are never silently skipped.
workspace-gate:
	bb -cp . checks/wm_workspace_gate.clj

# Required War Machine pre-merge/reviewer command. Keep the two boundaries
# explicit so ordinary futon2 CI remains usable in an isolated checkout.
pre-merge:
	$(MAKE) ci
	$(MAKE) workspace-gate

# Generated, non-short-circuiting operational report. New/unaccepted red is
# nonzero; exact, referenced, expiring accepted red remains visible at exit 0.
status:
	python3 scripts/wm_status_report.py

status-control:
	python3 scripts/wm_status_report.py --source-control

# Certify one completed operator/diagnostic run without guessing "latest".
# Usage: make certify-run RUN_ID=<uuid>
certify-run:
	@test -n "$(RUN_ID)" || (echo "certify-run: RUN_ID is required" >&2; exit 1)
	bb -cp . checks/certify_live_run.clj --run-id "$(RUN_ID)"

# Read-only operator preflight. It runs checks and reads receipts/roster state;
# it never starts a tick or dispatches an agent.
run-readiness:
	python3 scripts/run_readiness.py

run-readiness-control:
	python3 scripts/run_readiness.py --negative-reviewer

run-readiness-tree-control:
	python3 scripts/run_readiness.py --tree-control
