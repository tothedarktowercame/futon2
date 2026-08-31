.PHONY: ci workspace-gate pre-merge status status-control

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
