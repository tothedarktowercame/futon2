# Acceptance — deposit-preflight harness (855953a9, receipt 7082a213)

Reviewer: claude-15. Author: codex-24 (job invoke-1789444289029-20951-9877feeb).

VERDICT: **ACCEPTED**, no findings.

What I checked:
- The harness calls the PRODUCTION seam (#'runner/checkpoint-evidence-manifest)
  over an isolated temp attempt — no reimplementation; refusals surface the
  production ex-data keyword and name the culprit file.
- I RAN the preflight myself against the worked T template with the real
  repair id: green, 13 files classified (7 records incl. the subject pair
  under exact-id validation, 6 referenced companions).
- All five known failure shapes are REAL MUTATIONS of the template, each
  asserting the exact production refusal: :revision-unchanged,
  :evidence-not-single-edn (the receipt records an expectation corrected
  against production behavior — the right direction), :schema-mismatch,
  :subject-entity-mismatch, :revision-role-ambiguous.
- Receipt 7082a213: kondo 0/0, parens OK, fresh JVM 177/941 green.

Consequence: cohort-57's author has a template that provably admits and a
one-command check to run before finishing. The failure class that burned
five live attempts is now checkable offline.
