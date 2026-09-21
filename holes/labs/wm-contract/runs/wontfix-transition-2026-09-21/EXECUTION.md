# WONTFIX transition and corrected sweep

Author: codex-1. Implementation: 6b4cfd4254fc1aadaf2610dff6cd466592d657f5.
Authority: Joe's ruling, futon2 6402ed8a789c96d02b73b1f79521acd006a5b8ca.

`dismiss-wontfix!` appends a distinct dismissal with an operator-ruling
citation, a per-finding permanent-unfixability evidence sentence, and actor.
It validates nonblank strings and exact disposition keys. It does not prove
the truth of those human assertions. Missing/blank fields refuse, as do prior
dismissals, resolutions, and findings already awaiting implementation validation.
The original finding stays unchanged and the audit reader retains the disposition.

Executed gates:

- `clj-kondo --lint src/futon2/aif/repair_obligation.clj test/futon2/aif/repair_wontfix_test.clj`: 0 errors, 0 warnings.
- `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- src/futon2/aif/repair_obligation.clj test/futon2/aif/repair_wontfix_test.clj`: OK.
- Precommit: `clojure -M:test -m cognitect.test-runner -n futon2.aif.repair-wontfix-test`: 4 tests, 56 assertions, 0 failures/errors.
- Twice after commit, from futon3c: `clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/wontfix-transition-2026-09-21/registry.edn`.
  Each run: 4 tests, 56 assertions, 0 failures/errors; subject `repair-store/dismiss-wontfix`.

Warrants:

1. `test-registry-bacd9df61dc3de31798a8bd6c81423592100423560f313d7b7fdab367a700a24`
2. `test-registry-3dd8ab12e732854757941921e498718d622364c534545b4ccdd61379bf4fe5f4`

Controls use isolated temporary stores and the real append/read APIs:
valid dismissal removes effective-open and remains audit-visible; original
bytes remain identical; missing/blank authority, reason and actor refuse;
resolved/dismissed/implemented findings refuse; unknown ids, path traversal
and extra disposition keys refuse.

## Production sweep

No store transitions or clicks. See the sibling `wontfix-sweep-2026-09-21.edn`,
field `:corrected-scope-sweep`, for eleven individually timestamped checks.
WONTFIX 0; cleared conditions 3, all retained-no-applicable-API;
retained-live/unregistered 8. These categories do not sum with cleared:
the three cleared conditions are the same three retained-no-applicable-API rows.
repair-attempt-054 is excluded from this corrected scope.

A 404 at a roster endpoint proves current non-registration, not permanence.
The configured memory-seat roles for zai-4 and zai-5 remain at futon3c
4d8a84a74b143fac5ec1d58742e8de3386f56863, resources/memory-seats.edn:4-5.
No permanent retirement ruling was supplied for these identities.
The prior `busy` holds for zai-1 and codex-23 have cleared by current roster
evidence, but none of the four older dismissal categories establishes release
solely from current readiness. No mutation API was attempted on production,
so there are no production API refusals to quote.
