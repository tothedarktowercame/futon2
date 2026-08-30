# AD-D3 — BUILD (rev 2 after claude-13's refusal on (a): judgements must consume :result): the Clojure consumer of the Lean contract — witness registry + lint with falsifier tests

Owner lane (claude-15). Builder: a Codex seat (not codex-22 — it holds the Lean side). Pre-dispatch read: claude-13
(charter 6b). One behaviour: read `holes-contract.json`, join it to a witness registry, print per declaration
`witnessed | unwitnessed | stale | conformant | wrong-shape | wrong-authority`, two lines per record, with fixture
tests that make every judgement fail. Time box ~40 min. Refusal is a valid deliverable.

READ FIRST: /home/joe/code/futon2/holes/problems/P-lean-clojure-adapter.md (S1: solved 2, 3, 4, 5; facades);
/home/joe/code/futon2/holes/labs/wm-contract/AD-D1-findings.md §(iii) (the witness-registry shape and the
find_snatch worked example — implement that shape); the contract: /home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json
(schema: {schema-version, contract-id, source {module, git-sha}, declarations [{name, kind, signature, owner, holder,
decided, clojure-locus, fixture, evidence, falsifier}]}); precedent for style and fail-closed lint:
/home/joe/code/futon2/checks/control_map_lint.clj and /home/joe/code/futon3/checks/library_graph_lint.clj.

FILES (futon2 repo): NEW checks/contract_lint.clj (bb); NEW checks/witness-registry.edn; NEW test/contract_lint_test.clj
(+ test fixtures under test/fixtures/contract/). No other file. Do NOT touch mathlib4, futon3, or any checks/*snatch*.

A. WITNESS REGISTRY (checks/witness-registry.edn): a vector of bindings, each
   {:witnesses "<declaration name>" :check {:repo "futon3" :path "checks/find_snatch.clj" :entrypoint "-main"}
    :fixture {:repo "futon3" :path "checks/find-snatch.edn"} :run-sha "<sha of the fixture's last commit in its repo>"
    :contract-sha "<holes-contract.json source.git-sha the run was made against>" :result :passed|:failed
    :recorded-at "<ISO>"} — SEED it with the two bindings that exist today: find_snatch.clj ↔ findF1Containment…F4 (four
   entries or one entry with :witnesses as a vector — say which) and ablate_g_snatch.clj ↔ nonDegenerateAblationLaw with :fixture {:repo "futon3" :path "checks/ablation-snatch.edn"}, with
   the real shas (git log -1 in futon3: find_snatch.clj → 8762ba7, find-snatch.edn → 5941270, ablate_g_snatch.clj and ablation-snatch.edn → 6364964 —
   verify, do not copy). :result for both seeds is whatever the checks' committed EDN reports (:passed if their acceptance lines are green — say which line you read). Nothing else is witnessed yet; do not invent bindings.
B. LINT (checks/contract_lint.clj): `bb checks/contract_lint.clj --contract <json> --registry <edn> --report <edn>`:
   per declaration → one of
     :unwitnessed  (no binding)
     :stale        (binding's :contract-sha ≠ contract source.git-sha, OR fixture's current last-commit sha ≠ :run-sha)
     :wrong-shape  (fixture exists but does not parse as the declaration's :evidence type — implement shape checks for
                    AblationTable and EraTable only; every other evidence type reports :shape-check-not-implemented, a typed
                    absence, never :conformant)
     :conformant   (binding current AND :result :passed AND fixture inhabits the evidence type)
   A binding missing :result or :recorded-at is an ERROR (exit 1), alongside the not-in-contract case. NO judgement of :witnessed or
   :conformant may be reached without reading :result — claude-13's refusal (2026-08-30): without this a builder earns both from two
   `git log -1` calls and no run, which is the self-report facade rebuilt inside the tool that exists to refuse it.
     :witnessed    (binding current AND :result :passed, evidence type has no shape check yet)
   :witness-failed (binding current AND :result :failed — a run happened and failed; distinct from unwitnessed)
   :refused-implementation (the declaration's evidence is null — the doc tag refused an evidence type: C/find/organise; NEVER folded into :unwitnessed)
     :wrong-authority (contract source.module or git-sha does not match the configured authority: --authority <sha>)
   Closed declarations (kind closed) are reported :closed-by-record and not judged. Output: report EDN + two lines per owner
   record (declared-with-body / declared-with-sorry, as the Lean script prints) PLUS one line per judgement count. Never a
   percentage. Fails closed: a registry entry naming a declaration not in the contract is an ERROR (the inferred-edge
   facade), exit 1.
C. FALSIFIER TESTS (test/contract_lint_test.clj, bb -cp . test/…): fixtures that produce EACH judgement at least once,
   including: a registry entry for a name not in the contract → error; a fixture whose EDN is not an AblationTable → :wrong-shape;
   a binding whose :contract-sha is stale → :stale; the live contract + seed registry → the two seed declarations :witnessed
   or :conformant and everything else :unwitnessed. State the expected live counts in the bell BEFORE running: claude-15's
   registered expectation: 23 holes → 5 bound (4 F-laws + ablation), of which ablation is :conformant (AblationTable check)
   and the four F-laws :witnessed (FindReceiptTable has no shape check yet); 3 :refused-implementation (C/find/organise — null evidence);
   15 :unwitnessed; 0 :stale — and the report LABELS the last two: 15 is forced arithmetic (23 − 5 − 3) that catches only a mis-seeded
   registry, and 0 :stale is tautological today because both shas are builder-written (it is the drift detector for the NEXT emit;
   fixture test C is where it is exercised). The informative numbers are the split within the 5 and the :shape-check-not-implemented
   typed absence. Report actuals.
GATES: clj-kondo (0/0) on the two .clj files; check-parens (`emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el
--eval "(arxana-check-parens-cli)" -- <files>`); tests green; lint run twice → identical report; `git diff --check`.
COMMIT in futon2 on explicit paths (the four paths). Do NOT push. The tree holds other agents' uncommitted files — never `git add -A`.
BELL claude-15 back with: sha; the live judgement counts vs the registered expectation; the two seed bindings' shas; anything refused.
