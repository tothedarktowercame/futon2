# P2-form twenty-criterion matrix — run 2026-10-31-ebbbe42a (click 9028a8a7) — zai-6, 2026-10-31

Owner: zai-6. Evidence lane, read-only (a click was running during analysis; no
canonical store, ledger, or runner interaction). Method: structural read
(Babashka/edn) of the retained record + sidecars. Same row set and vocabulary as
the 4955b6e6 matrix and the P2 matrices rev 4:
:observed :violated :unknown :not-exercised; Q verdicts kept separate.

This is **the first run reaching terminal `:grounded-progress`** — the first run
in which the selected wants were actually measured (D1) and cascade-feedback
verified with a publication receipt (D2).

## Run identity

- Run `2026-10-31-ebbbe42a-9889-4bff-921d-0843ea85a699`; click
  `wm-click-9028a8a7-a2ff-4ec4-8129-06cbbce7a3b3`; issuing caller `claude-1`.
- Record SHA256 `04bb8e1b15234b022e948784feec87c721b70ae872f325cafaaa73b1fd23f699`;
  `.requirements.edn` `557be3fc616eb3e1cab52f529df708baed6ba88dfa4853e38ade4f48fd8f7176`;
  `.learning.edn` `b393f5ed4a225841f15187e436cf37f73f78eac2c0efc8d654941c29483259dc`;
  terminal-publication `226691e0…`; `.scan.md`; report-card set; trace publication
  `data/wm-trace/wm-trace-2026-10-31.edn` `:persistence :record-returned`.
- Started `2026-10-31T16:48:47.462Z`, wall 632 628 ms; route RUNNER→FULL_LOOP_CLOSE
  via **`:grounded-progress`** at `16:59:20.292Z`; target `M-typed-memories` (same
  policy `M-typed-memories/policy-ed6ff8783a9a9b045f59`, same four wants as 4955b6e6:
  h4c0da9d5dca9, ha4d7c19e5cdd, h502c4971a438, hc32323ac62d1).
- Q verdicts (requirements export, checked 17:00:01Z): **Q1 :violated**,
  Q3/Q4/Q5/**Q6**/Q7 :holds, Q9 :unverifiable, conforms false.
- Grounded commit `4fd00fa63f1aff49c5b4b1dd033571166072d5b9` (futon3c):
  `futon3c.peripheral.memory-supersession-asof-test`, replay
  `1 test, 22 assertions, 0 failures` (P2 supersession limb).

## Headline — rows this run MOVES

- **D1: NOT-EXERCISED → OBSERVED.** `:execution-outcomes {:status :verified}`:
  before/after measured for all four selected wants. One reached
  (`ha4d7c19e5cdd`, P2 supersession), three unreached (P1b, P3, P4), none blocked,
  none progressed. Mission-state: reached 1 / unreached 3 / untouched 3;
  `:artifact {:grounded? true}`; `:terminal-outcome :grounded-progress`.
- **D2: NOT-EXERCISED → OBSERVED.** `:cascade-feedback {:status :verified}` and
  `:publication {:status :recorded, path data/wm-pattern-feedback/events.edn,
  receipt/id 0dfdae29a44705b3d7383dc06fc0eb91157c0a11a4e7315637c684f51e14c675}` —
  the first pattern-publication receipt of these matrices. Patterns
  selected/applications still empty; blocker honestly disclosed
  (`:target-has-no-mechanical-acceptance-declaration`).
- **Q6: :unverifiable → :holds** (first time): predecessor run
  `2026-10-31-0ba3370c` closed with `{:outcome :invalid, :raw :build-failed}`
  retained in-record, so a previous outcome exists to repeat on (inputs changed:
  selection-input-digest differs, so no repeat).
- Still UNKNOWN: A2, A3, B3, C2. Still NOT-EXERCISED: B2 (no failure this run).
- Newly adverse facts inside OBSERVED rows: C1 carries an unremediated
  `:author-response/decisions-mismatch` (`:claim-matches? false`); E4 is violated
  again for the pair 0ba3370c→ebbbe42a on the serving-composition component only.

## D1 answer (asked): mission checkbox alone, or also the committed test?

**The reached-want verdict rests on the mission checkbox alone.** The measurement
for `ha4d7c19e5cdd` is a `:C4` declaration-presence check: authority
`:mission-observation`, after-locator resolving to `holes/missions/M-typed-memories.md`
at futon3c sha `4fd00fa63…` with `:decl "- [x] P2 demonstrates supersession…"` —
`before {:observed false}` → `after {:observed true}` with no other instrument in
the outcome object (no test execution, no replay reference). The committed test at
that same sha (`4fd00fa63`, `memory-supersession-asof-test`, replay
`1 test / 22 assertions / 0 failures`, grounding phase 11 647 ms) is retained
separately in `:run-output` as the built demo + replay receipt and is what the run
*grounded its progress on* — but it is **not** what measured the want. So: checkbox
is the measurement; the test is contemporaneous corroborating evidence at the same
commit. (The other three wants measured `false` at the same sha — correctly, since
their `- [x]` limbs describe arcs the P2 commit does not complete.)

## Matrix

| ID | Status | Expected predicate | Evidence (path/key = value, hash/pin) | Branch / intervention |
|---|---|---|---|---|
| A1 | OBSERVED | one consumed credit per admitted issuance | `wm-ordinary-clicks/consumption.jsonl` line `wm-click-9028a8a7…` issued `16:48:39.820Z`, authorization `AUTH-ordinary-click-budget-renewal-7…` sha `326826ac`, caller `claude-1` | none |
| A2 | **UNKNOWN** | serving envelope/resources match declaration | observed: `:runner/loaded-sha256 "8f3529f1d503cc25155962d86f602b2ce39fd6f98c98c9ff21e6d888c7c86c79"`, `:apparatus-certificates :certified? false` (Lean/mathlib `de33cabb48`; R20 rows `:not-recorded`). NOT observed: heap measurement, time-valid serving-envelope/admission-opts receipt joined to this run | none |
| A3 | **UNKNOWN** | blockers + no incompatible work | observed: `:open-stop-lines {:count 199}` (up from 197), `:job-liveness []`, `:refresh :ok` (scanned 342, created 0, updated 2, unchanged 340). NOT observed: authority-aware effective-injury join — injury `a67d3e49` (duplicate-effective-run, opened by click 2's failed close) was open at selection and is not mentioned anywhere in the record (no `:injuries`/`:runtime-disposition` key retained) | none |
| A4 | OBSERVED | census/locators/revision/baseline before dispatch | mission-want source futon3c `holes/missions/M-typed-memories.md` sha `fa9566fb…`; four want tokens with `:C4` locators (`- [x]` P1b/P2/P3/P4 limbs); `mission-baseline-sha256 "25454471…"`, `base-head "d5adbceb05068ce518c1d7f59fb7a84e0cc2aa98"`; `compiled-check-set-sha256 "4f53cda1…"`; candidate `M-typed-memories/policy-ed6ff8783a9a9b045f59` | none |
| B1 | OBSERVED (scoped) | enumerated population consistent | giraffe manifest `5cf3904e…` (same as 4955b6e6); **Q1's 610/611 gap is inside this scope** — the enumeration omits `E-kimi-task-171`, an excursion (not a repair ticket this time; zai-7's ticket-only account of the 608/609 gap does not cover it) | none |
| B2 | NOT-EXERCISED (no failure this run) | failure retention for this run's failure | `:failure {:absent :no-failure}` — terminal `:grounded-progress`; nothing to retain. (Contrast 4955b6e6: `:already-satisfied` retained 1/1) | none |
| B3 | UNKNOWN (target-identity question) | scores + declared G; ties disclosed | `:cascade-scores [3.635233482234763 3.635233482234763 3.6352334822347636]`, `:cascade-index 0`, `:tie? false`, `:tied-cascades [:what-others-wait-on-first :only-what-can-start-now]` (2 this run). Same distinct-field semantics as adjudication `ec15959e8`; no recomputation performed or claimed | none |
| B4 | OBSERVED (outer) / inner unresolved | prediction frozen pre-effect with identity | `:prediction {:status :ready, :q {:doable 0.75 :blocked 0.25}, :landing [:stop-the-line :none] 0.9999999999999999}`. Boundary as before: `:grounded-progress` is not an outcome class in the frozen prediction set — the actual terminal class was never scored | none |
| C1 | OBSERVED (**with unremediated defect**) | identity through construction/dispatch | `:answer-delivery {:source :reviewed-patch-function, :patch {:namespace futon2.aif.wm.authored.req-6a8fe374, :var decisions, :source-digest "ac1e6f7b…", :commit "4560d631b443d51e385c1bd32fb5d5aff247c2a2"}, :requisition-id "6a8fe374…"}`; delivery-qa 9 811 ms. **`:claim-matches? false` with 1 defect `:author-response/decisions-mismatch`** (claimed decisions differ from returned patch decisions) — disclosed in-record, not remediated, run proceeded to grounded-progress anyway; route attestation `b20a1931…` present | none |
| C2 | **UNKNOWN** | durable checkpoint before send | `:close-evidence-manifest` 18 ms vs `:author-dispatch` 267 ms; `:trace-publication {:persistence :record-returned}` pinned. Pre-send durability (vs end-of-run write) unproven by timing alone — same boundary as the P2 matrices | none |
| C3 | OBSERVED | descriptor contract (full SHA) | patch commit `4560d631b443d51e385c1bd32fb5d5aff247c2a2` (40-hex), source-digest `ac1e6f7b…`, requisition `6a8fe3749588…` (full 64-hex); grounded commit `4fd00fa63f1aff49c5b4b1dd033571166072d5b9` (40-hex). The f805-era short-sha descriptor defect is not reproduced | none |
| C4 | OBSERVED (boundary) | review of same artifact/contract | reviewer-of-record `wm-reviewer` present (`:participants`); in-run review evidence is the reviewer-note "standing decision deposited (:still-live); P1b/P3/P4 remain open on M-typed-memories" plus the demo replay in `:run-output`. Boundary: thinner than 4955b6e6's tool-evented review gate (no `:review-gate` map retained this run); the C1 decisions-mismatch was not a review finding | none |
| D1 | **OBSERVED (moved from NOT-EXERCISED)** | before/after per selected want | `[:cascade-feedback :mission-state :execution-outcomes] {:status :verified}`; per-want before/after with declared + after locators resolving at futon3c `4fd00fa63…`: reached `ha4d7c19e5cdd` (P2) only; `h4c0da9d5dca9`/`h502c4971a438`/`hc32323ac62d1` measured `false`; `:artifact {:repo futon3c, :commit 4fd00fa63…, :grounded? true}`. **Measurement instrument = C4 mission checkbox (see D1 answer above); the committed test is corroborating demo evidence, not the measurer** | none |
| D2 | **OBSERVED (moved from NOT-EXERCISED)** | executable checks + learning writes | `:cascade-feedback {:status :verified}`; `:publication {:status :recorded, :path data/wm-pattern-feedback/events.edn, :receipt/id "0dfdae29a44705b3d7383dc06fc0eb91157d0a11a4e7315637c684f51e14c675"}`; executable check = demo replay `clojure -M:test -n futon3c.peripheral.memory-supersession-asof-test → 1 test, 22 assertions, 0 failures`; `:patterns {:selected [] :applications []}`; blocker `:target-has-no-mechanical-acceptance-declaration` disclosed; observation-labels snapshot `07fc0358…` (written 24, results 34, drop-reason check-refused 6) | none |
| D3 | OBSERVED | truthful classification | no failure (`:failure {:absent :no-failure}`), terminal `:grounded-progress` matches grounded commit + replay; no injury minted this run; blocker/still-live note honest about P1b/P3/P4 remaining open | none |
| D4 | OBSERVED (retention gaps) | retention completeness | gaps: `:outer-task-selection {:status :absent, :reason :receipt-not-retained}`; `:habit-reinforcement {:absent :legacy-habit-store-retired}`; `:habit-fold-snapshot {:absent, :fold-not-passed-to-runner}`; Q6 focus `:focus-not-retained`; R20 certificate rows `:not-recorded`; model-usage `:status :partial` (one empty job slot) | none |
| E1 | OBSERVED (success limb) | ending recorded with receipt | terminal-receipt `:grounded-progress`, digest `26176c5bc267b5bb2eabfc073de761d9527d8cb431065f0a9c1e42a1fa12ae2d`; route RUNNER→FULL_LOOP_CLOSE 16:59:20.292Z; learning projection retained (`.learning.edn` `b393f5ed…`). Failure limb not exercised (no failure) | none |
| E2 | OBSERVED (predecessor sense) | prior ending visible in this run | `[:q6 :previous]` carries run `2026-10-31-0ba3370c` started `16:15:56.773Z` closed `{:outcome :invalid, :raw :build-failed}` with input-digest `3aa8ad0a…` and retained choice/policy-key — the first pair in these matrices where the predecessor's closed outcome is present | none |
| E3 | OBSERVED (comparisons) | no repeat on unchanged inputs | `:outer-repeat-comparison {:status :inputs-changed}` (selection-input-digest `821c94ee…` vs retained comparisons incl. `2026-10-29-4961d278` record `28b8e31d…`); **and** `:effective-run-comparison {:status :no-duplicate, :matched-run nil}` — the duplicate-run guard (which refused click 2's repeat and minted injury `a67d3e49`) ran here and found no duplicate | none |
| E4 | **VIOLATED** (pair 0ba3370c→ebbbe42a, serving-composition component only) | no unrecorded intervention between pair | predecessor `0ba3370c` ran 16:15:56Z with runner sha `4803cc9264fc8bcb18c65c4a53b4ab94cee1b63a7ffb67c1b94ba7185c551148`; this run 16:48:47Z with runner sha `8f3529f1d503cc25155962d86f602b2ce39fd6f98c98c9ff21e6d888c7c86c79` — serving composition changed between the pair. Mitigating: canonical futon3c base-head identical both sides (`d5adbceb05068ce518c1d7f59fb7a84e0cc2aa98`), predecessor properly closed, no intervening runs (the six `:incomplete` records on disk are all 14:47–14:49Z, before the pair) | interventions recorded as contrary evidence, not erased |

## Launcher-relevant deltas vs the 4955b6e6 matrix

1. **D1 and D2 move to OBSERVED; Q6 moves to :holds.** Everything else holds its
   previous classification (A2/A3/B3/C2 UNKNOWN; B2 flips to NOT-EXERCISED for a
   benign reason — no failure occurred).
2. **Q1 violated again but on a different item**: `E-kimi-task-171`
   (611 open / 610 enumerated) — an excursion, so the 4955-era explanation
   (duplicate-id dedup of attempt-suffixed repair tickets) does not apply; the
   excursion lane is invisible to the outer population the same way.
3. **C1 defect worth watching**: `:claim-matches? false`
   (`:author-response/decisions-mismatch`) did not block grounding. The reviewer
   note does not mention it. If that is by design (delivery-qa absorbs it) it
   deserves a written condition; if not, it is a new small hole next to the
   declaration-presence boundary already flagged at C4.
4. **E4's violation is narrower than 4955's**: only the runner sha changed between
   the pair; base-head, closure, and no-intervening-run all check clean now.
