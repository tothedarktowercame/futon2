# WM-08 Route A rehearsal — F13-redo occurrence re-expressed and run

Author: zai-6, 2026-09-17. **This is a REHEARSAL, not ordinary-run evidence.**
The F13 retrieval was performed by the futon3a embedding constructor and
futon6 tier-0 scripts, not the receipted path; a re-expression is hermetic by
construction (ORDINARY-RUN-SCOPE.md §4). Ordinary credit requires Route B.

## Artifacts

- `REEXPRESSED-RECORD.edn` (sha256 `c56c3b00b9037092588b2393025f52af65e62c034a8afc6b1df7422b1467a9e8`)
  — the F13-redo `interpreted-pattern-set.edn` translated verbatim into the
  `find-receipt/context` / `find` record shape: schema keyword cast, minted
  occurrence identity (deterministic: fixed run-id, seeded UUIDs, pinned-at
  `2026-09-15T13:02:32Z` from FROZEN-CONTEXT), 31 sources with companion
  byte files (`companions/`, 31 files, every digest verified against bytes),
  9 facts (`:q0`→`:value`, `:method :mission-document-assertion` retained —
  the record's own `:runtime-state-unverified` finding is copied into
  `:holes`, NOT acted on), 7 interpretations with guards copied verbatim and
  line spans located in the captured bytes by normalised text search,
  candidates reshaped per §1 (tier-0 bare names normalised to canonical
  section/name from their pinned source paths, exactly as
  `find-receipt-test/sample` does), membership citations from the frozen
  `retrieval-index.tsv` — except the genesis pattern
  (`coordination/bind-promotion-to-post-repair-replay`), whose registration
  row was captured at occurrence time as `new-index-row.patch` (the frozen
  index is PRE-genesis and cannot contain it).
- `test/futon2/aif/wm08_route_a_test.clj` — the translation and the
  rehearsal, as tests. `route-a-reexpression-artifact` proves a fresh
  deterministic build is byte-identical to the committed artifact;
  `route-a-rehearsal` consumes the committed files from disk.

Translation rule honoured: verbatim-copy-or-refuse. Nothing was improved,
re-judged, or repaired. Two shape adaptations were precedented mechanical
moves (candidate id normalisation from pinned paths; membership source for
the genesis row), both documented in the test.

## The run

`find-receipt/find` over the re-expressed record + companion bytes,
library-root `/home/joe/code/futon3/library`; no designation artifact.

- **Selected (7):** `:agent/evidence-over-assertion`,
  `:coordination/bind-promotion-to-post-repair-replay`,
  `:coordination/cross-validation-protocol`, `:social/tension-before-code`,
  `:social/verify-before-compose`, `:stack-coherence/ready-blocked-triage`,
  `:war-machine/operational-not-decorative`.
- **`validate-result!` (F1–F4) against the independently compiled context: PASS**
  — result returned unchanged. Repository value-digest
  `6f4927b6c1dfa289fb23a3a381087979284748a0dc04820e2aec2532a0de9a5e`.
  Note the two-digest rule (§3): this is NOT the frozen index digest
  `0cb12b24…`, and they must never be unified.
- **`find-designation/resolve-designation` with no artifact:** exactly
  `{:designated nil :f4 :vacuous :vacuous-because :no-designation-supplied}` —
  the honest vacuity §F4 requires.

## The two known disagreements, per §6.2 (guard truth on recorded frozen facts)

1. **`coordination/session-durability-check`** (zai-45 expects it; interpreter
   judged it not relevant): there is **no recorded interpretation** for it, so
   there is no recorded guard — it **cannot fire** on the frozen facts.
   Refusal shape in the counterfactual: `:expected-pattern-not-selected`.
2. **`social/tension-before-code`** (interpreter judged it relevant; zai-45
   wrote no row): its recorded guard
   `[:and [:fact "node-spec-map-exists"] [:not [:fact "complete-per-node-suite-exists"]]]`
   **fires** on the recorded facts (true ∧ ¬false), so it is selected and
   receipted regardless of any relevance judgment. Refusal shape:
   `:unexpected-selected-pattern`.

Both disagreements are data, not defects (§6.1). Neither side was amended.

## What the external-expectations validator actually said

`EXPECTATIONS.edn` was not opened for editing and not modified.

1. **As committed, `find-expectations/read-artifact` refuses it**:
   `{:reason :invalid-expectation-artifact}` — the artifact's `:occurrence`
   has no flat `:target-source` map (its `:target` is the nested
   FROZEN-CONTEXT map). zai-45 wrote from FROZEN-CONTEXT; the validator wants
   the flat binding shape. A v2 shape question for zai-45.
2. **Counterfactual content check** (diagnostic only; artifact shape
   normalised in memory, `:expected` rows verbatim):
   - unexpected-selected: `:coordination/bind-promotion-to-post-repair-replay`
     (no row can exist for a pattern this occurrence authored) and
     `:social/tension-before-code` (disagreement #2).
   - expected-not-selected: `:agent/provisional-claims-ledger`,
     `:coordination/session-durability-check` (disagreement #1),
     `:stack-coherence/maturity-evidence-audit` (also no recorded
     interpretation — its missing-pattern-interpretation finding is in `:holes`).
   - Even the first shared row (`:agent/evidence-over-assertion`) refuses
     `:expectation-mismatch` on TWO fields: (a) the receipt's
     `:as-of :repository-sha256` is read-repository's VALUE digest
     (`6f4927b6…`) while a blind external producer can only pin the frozen
     INDEX digest (`0cb12b24…`) — the two-digest rule means the validator's
     `:as-of` comparison can never be satisfied by a truly blind artifact;
     (b) acknowledged-clause text differs by leading indentation (receipt
     quotes the raw line, expectations trim it).

## Findings for zai-45 (v2, a future occurrence — not this one)

1. Artifact `:occurrence` needs the validator's flat shape
   (`:target` id string, `:target-source {:path :sha256}`, …).
2. `:as-of :repository-sha256` in expectation rows cannot be produced blind
   under the current comparison — either the comparison field set or the
   producer's provenance rule must change; that is zai-16/zai-45's call.
3. Clause text normalisation (trim) differs between receipt quotes and
   expectation rows.

## Gates

- `clojure -M:test -m cognitect.test-runner -n futon2.aif.wm08-route-a-test`:
  2 tests, 15 assertions, 0 failures.
- find-receipt-test + find-expectations-test + find-designation-test:
  22 tests, 182 assertions, 0 failures.
- clj-kondo: 0 errors, 0 warnings. check-parens: clean.
- No production namespace touched. No edits to `EXPECTATIONS.edn`,
  `FROZEN-CONTEXT.edn`, or the F13-redo record.

## Provenance note

The test namespace was amended mid-dispatch by a concurrent hand (commit
`d9b906f3` plus uncommitted diffs, author recorded as Joe's committer
identity): a linear clause locator replacing my quadratic one (which had
hung the suite on the 1587-line frozen index) and the `new-index-row.patch`
membership route for the genesis pattern. I reviewed both changes, adopted
them, and added the absolute-path fix (`:path-not-absolute` refusal),
packet-2 candidate-id normalisation, and the honest counterfactual
assertions. Reviewer should diff against both intentions.
