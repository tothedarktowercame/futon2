# Q9 disposition: the production C certifies Q9 false (2026-10-16)

Requested by codex-68 (invoke-1792115632471-15004-59128ba5), following the
diagnosis `Q9-diagnosis-2026-10-16.md` (`319c02f74`). Ruling: option (a),
certify the negative. Branch: futon2 `claude-10-q5-family` from `319c02f74`.
Lean unchanged: mathlib4 `9e79af1bee` (`darktower`).

## What was not done, by ruling

The production C is unchanged. No relevance coordinate, calibration
authority, registry or positive factorization was added. Selection, Q5's
precision identity and the Q4/Q6 inputs are untouched.

**Making Q9 true remains experimental.** It needs:
1. an authority outside the selector that is separately commissioned and
   supplies pinned calibration and factorization registries;
2. a decision about the selection law, because the C that selects would
   have to put completion lexicographically first and gain a relevance
   coordinate. That changes selection.

Neither exists. This branch does not stand in for either.

## Runtime (futon2)

**Carrier** (`src/futon2/aif/q9_factorization.clj`,
`:wm/q9-factorization-v1`, sealed). It is computed from a record's decision
and run id. For every compared policy (the posterior of the one G
selection, the same set Q4 counts) it:
- binds the policy's certified preference receipt by policy id, by target
  and by the exact G the selector compared;
- requires the receipt's schedule to equal the progress preference
  recomputed from the authority's own token count and horizon;
- reads two things off the schedule itself, without trusting labels:
  - **outcome space**: whether every outcome at every step is a
    `[completed-count want-met?]` pair. If so, there is no relevance
    coordinate.
  - **completion ordering**: completion is lexicographically prior only if,
    at every step, the least-preferred completed outcome strictly outweighs
    the most-preferred uncompleted one. The carrier records the first step
    where that fails, with the pair and both weights.

It also records:
- the selected policy, which must be one of those compared;
- the schedule and receipt digests;
- the receipt authority's source, provenance status and `:calibrated`;
- the decision's declared preference (`:cascade-shape-g/progress-preference`)
  and its reading-precision identity;
- digests binding the posterior, the receipts and the certificate's
  candidates.

**Status.**
- `:factorization-absent` requires every compared C to be recomputed and
  every one to lack a relevance coordinate. Its reasons, in this order:
  - `:no-relevance-coordinate`;
  - `:completion-not-lexicographic`, if any C fails the ordering;
  - `:calibration-authority-is-selector`, if any receipt authority is not
    `:calibrated true`;
  - `:no-calibration-registry` and `:no-factorization-registry`.
- Any gap in that chain gives `:not-recomputable` with its reason:
  - wrong route;
  - no compared policy;
  - a compared identity that is not structural;
  - the selected policy not among those compared;
  - a missing, duplicated, uncertified or foreign-target receipt;
  - a receipt G that is not the compared G;
  - a lost authority, or one that is not `:progress-count`;
  - a horizon mismatch;
  - a schedule that does not recompute.
- A decision that carries `:factorized-preference`,
  `:calibration-authorizations` or `:factorization-authorizations` (at the
  decision or certificate level) is `:not-recomputable
  :self-asserted-factorization-carrier`. Nothing on this route writes one.
- The carrier has no positive status.

**Runner seam** (`full_loop_runner.clj`). The record gets `:q9` from
`(q9-factorization/certificate (record-inputs r))`, beside `:q2`. An
abstention is sealed as `:not-recomputable :not-the-reading-family-route`.

**Exporter** (`scripts/wm_run_facts.clj`, `q9-facts`).
- **Reading-family route.** The exporter reads only the `:q9` carrier.
  `verify-record` accepts a carrier only when all of these hold:
  - it is intact;
  - it has the right schema;
  - its status is `:factorization-absent` or `:not-recomputable` (anything
    else is `:q9-positive-carrier-refused`);
  - it equals the carrier recomputed from the record;
  - it is `:factorization-absent`.

  A verified carrier exports `factorizedPreference = nil` and both
  registries as `[]`. Anything else exports NR with the reason.
- **Other routes.** An unsealed `[:decision :factorized-preference]`
  carrier is now refused (NR), however well joined. Before this change it
  exported Q9 true. No producer persists one: the controlled SUT returns
  its carrier, and `integrated-controlled-sut` files it under
  `[:controlled-sut :certificates]`, which the exporter never read. That
  path was not supported by anything that runs, so I did not keep it.
- **Removed.** `factorized-preference-facts`, the old Clojure check of
  registry joins, had no producer.

**Lean term** (`scripts/wm_check_run_facts.clj`). A nil certificate emits
`factorizedPreference := none, calibrationAuthorizations := [],
factorizationAuthorizations := []`. The positive emitter is kept, for a
future commissioned authority, and is now tested on facts directly.

**Report card** (`scripts/wm_report_card.clj`). When
`factorizedPreference` is none, the card reads Q9 as `:fail` (Lean's Q9 is
false on `none`). A certificate still goes to the generated Lean checker.
This also replaces a stale `:Q9` clause that read pre-Q9 fields.

## Correction to the diagnosis

The diagnosis said progress outweighs completion "for n ≥ 5 progress
tokens". That figure was for one pair at comparable progress,
`[n−1, not met]` against `[1, met]`. Lexicographic priority fails at every
n ≥ 1: `[n, not met]` has weight e^4 and `[0, met]` has weight e^2. The
fixture (n = 2) shows it: weights 0.1033 against 0.0140 at step 1. The
n = 5 pair is pinned in a test as well.

## Historical fixture

`wm_run_facts_test` now looks for the 2026-10-05 record in three places,
in this order:
1. `$FUTON2_DATA_ROOT`;
2. this checkout's `data/`;
3. the shared checkout's `/home/joe/code/futon2/data`.

If none holds it, the test prints a typed skip
(`{:status :skipped :reason :historical-record-absent :record … :searched
[…]}`) and asserts on that map, so it does not error. In this worktree it
finds the record in the shared checkout and asserts Q9 NR. The skip map is
pinned by `a-missing-historical-record-is-a-typed-skip`. The record was not
copied or reconstructed.

## Tests

**Production runner seam** (`test/futon2/aif/wm/q9_factorization_route_test.clj`,
7 tests, 145 assertions; the Q8 route's production clicks):

- **Q9 false on both clicks of a stream.**
  - The carrier is verified and bound to the run id.
  - The compared set is Q4's, and the selected policy is among it.
  - Each C's receipt and schedule digests match, and each schedule equals
    the recomputation.
  - Each witness's weights are the schedule's own.
  - The authority is synthetic and uncalibrated.
  - The export is none with empty registries; the exact Lean term is
    pinned; the report card says `:fail`.
- **Reasons come from the record, not from labels.**
  - Relabelling the receipts `:calibrated true` (resealed) drops only
    `:calibration-authority-is-selector`; Q9 stays false.
  - A sealed carrier claiming completion first is refused as not bound to
    the record.
- **Adversarial: never false or true by default.** Each of these is NR, with
  its own reason:
  - absent;
  - tampered status;
  - edited reasons;
  - a foreign carrier (click one's carrier in click two's record);
  - incomplete (one policy dropped, resealed);
  - wrong schema.
- **Self-asserted positives refused.**
  - A sealed `:q9` with a positive status and a full certificate.
  - An unsealed factorized carrier at three decision paths, with `:q9`
    resealed around it.
  - The same carrier without resealing.
- **A C that cannot be recomputed certifies nothing.** Each with its typed
  reason, both on the carrier and from `verify-record`:
  - edited schedule;
  - dropped receipt;
  - duplicated receipt;
  - G not the compared G;
  - lost authority;
  - a kind other than `:progress-count`;
  - selected policy not compared;
  - another route.
- **Abstention.** The carrier is not recomputable, and the export is NR.
- **Unit tests.**
  - The ordering at n = 1, 2, 5, 8.
  - A completion-first schedule is lexicographic; a tie is not.
  - Outcome-space recognition, including a relevance-carrying space and a
    flat class table.

**Exporter unit tests** (`wm_run_facts_test`):
- the unsealed carrier and its three variants are refused;
- the none emitter, the positive emitter and the multi-entry registry
  emitter are tested on facts;
- the historical record and the typed skip.

**Planted bugs** (`~/notes/q9/plants/plant.py`, logs beside it). Each run
covered the route test and `wm_run_facts_test`. Every plant was caught:

| plant | failures |
|---|---|
| P1 runner does not attach `:q9` | 32 |
| P2 completion ordering always lexicographic | 10 |
| P3 verify accepts an unbound carrier | 8 |
| P4 schedule not recomputed | 4 |
| P5 self-assertion unchecked | 11 |
| P6 exporter skips verify | 19 |
| P7 positive status accepted | 1 |
| P8 receipt refusal ignored | 12 |
| P9 G binding unchecked | 4 |
| P10 report card reads none as NR | 2 |
| P11 Lean emits `some default` for none | 3 |
| P12 selected-in-compared unchecked | 4 |
| P13 historical unsealed carrier accepted as none | 16 |

**Lean** (`test/fixtures/q9-lean/generate.clj`). Exported through the real
runner and checked with `lake env lean` at mathlib4 `9e79af1bee`:
- `Q9First` and `Q9Repeat`: Q9 **violated**;
- `Q9Missing`, `Q9Tampered` and `Q9SelfAsserted` (a factorized carrier in
  the decision, with `:q9` resealed): Q9 **unverifiable**;
- other requirements in `Q9Repeat`: Q2, Q4, Q5, Q6 and Q8 hold; Q7 is
  violated (pathAbsenceCount 1) and Q10 is unverifiable, as in the Q2
  fixtures.

The committed Q2/Q8 fixtures still show Q9 as NR, because they predate the
carrier. They were not regenerated.

**Focused controls.** 16 namespaces: the exporter, report card and Lean
term tests, the controlled SUT tests, and the Q2/Q4/Q6/Q8/Q9 route,
reading-family, preference and precision tests (`~/notes/q9/focused.txt`).
Branch against base `319c02f74`:
- branch: 149 tests, 1165 passes, 0 failures, 0 errors;
- base: 140 tests, 1004 passes, 0 failures, 1 error. The error is the
  missing historical record (`FileNotFoundException`), which this branch
  fixes.

The branch adds 9 tests: 7 in the route test, and a net 2 in
`wm_run_facts_test`.

**Static gates.**
- clj-kondo on the changed source and test files: 0 errors, and the same
  warning counts as base (runner 1, report card 2). `generate.clj` has the
  same `in-ns` script findings as `q2-lean/generate.clj`.
- check-parens (`arxana-check-parens-cli`, checked to catch a planted
  unmatched bracket): OK on 8 files.
- `git diff --check` clean.

Not run: full CI, the full affected-namespace sweep, a live click, a hot
load.

## Open

- **Q9 true** needs the commission and the selection-law decision named
  above.
- **What "relevance" means inside one task's family** is still a Theory
  question (option (d) in the diagnosis). The single-task field has no
  focused/related/unrelated distinction for the carrier to look for.
- **The positive Lean emitter is kept but nothing produces input for it.**
  When an authority is commissioned, its carrier should be sealed and
  record-bound like this one, never read unsealed from a decision.
