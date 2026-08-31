.PHONY: ci workspace-gate pre-merge

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
