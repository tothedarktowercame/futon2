# L5-4e — Selection Q4 terms and Q6 carriers

Base `e8bbde110`; branch `codex11/l5-4e-selection-q4-q6`.
No merge, reload, click or live store writes.

## Implemented and authorized clarifications

Q4 projects a horizon-one terminal-only, rawCumulative preference for each
recorded class, with step {0}, no graded steps, and risk/ambiguity/information
terms from numeric risk/ambiguity/novelty. Missing terms make every affected
carrier NR naming the class; neither finite G nor a boolean producer assertion
substitutes for terms.

Choice digest is retained as {:sha256 :covers :excludes}. Covers: canonical
sorted class prediction-digest/G-unit/representative-pick triples, abstention
G units, sorted forbidden evidence, focus, library and kernel pins. Under
claude-2's clarification 9334, whole-field reading and graph pins remain in
construction and are explicitly excluded from this digest. Otherwise unrelated
field changes would defeat the repeat-on-unchanged-choice check. Clarification
9336 explicitly adds representative picks. Both rulings are retained verbatim
in the adjacent comparison JSON and notes/rulings.json.

Each successful selector result now records :outcome {:status :chosen}; scored
refusals retain their outcome/kind in :selection-evidence. The runtime supplies
:previous before select. previous-run/selection-predecessor reuses the sealed
stream's predecessor and its exact record-file (no filename search). The record
run id and Q6 terminal must match that sealed predecessor. The reader extracts
class/pick/digest and selection outcome. A chosen selection with a recorded work
decision projects changed; downstream work refusal is not relabeled selection
refusal. Missing evidence, first run and identity mismatch remain typed.

This reads the prior record; it does not claim the whole record has a content seal.
The sealed inner terminal identity is checked. The first selection's retained
:decision is the evidence used by the record-summary helper for work entry.

## Explicit retention limit for L5-4f

The runtime catches outer selection failure at full_loop_runtime.clj:1385/1387,
returning through outer-selection-refusal at line 1195. This is before the inner
runner's start!/close! and writes no selection tick record. No writer was invented.
The reader's scope is explicitly :previous-completed-tick with retention-gap
{:status :absent :reason :refused-selection-left-no-record}. When :previous itself
carries that absence, the projector returns NR with the same reason (tested).
A missing stream cannot distinguish a first click from an unrecorded refusal;
it honestly retains no-earlier-started-run. An intervening outer refusal cannot
be discovered from this stream. Thus this packet does NOT establish complete
live selection-Q6 history. L5-4f must retain every selection outcome and prefer
that history; that is where the missing fact must be supplied.

## Two-selection replay and actual Lean output

Same pinned 626-task input as L5-4d-5, same live relation-context producer,
makeup initializer, class construction and chosen M-pattern-retrieval-calibration.
The first produced section was retained in an ac5bc477 copy. The record-summary
helper read that first record as the second select's predecessor. This is a tooling
fixture: no two real clicks or new work executions are claimed. Both exports and
Lean terms were produced for the second record copy; original ac5bc477 remains
sha 7dbf1614668d9f8c565562447c5d336b9424e6bb93d42335fde1ba1c32e7a9ac.

Actual `lake env lean` output (mathlib4 5025ad1263; both files exit 0):

| Q1 | Q2 | Q3 | Q4 | Q5 | Q6 | Q7 | Q9 | Q11 |
|---|---|---|---|---|---|---|---|---|
| holds | holds | holds | holds | unverifiable | holds | unverifiable | violated | holds |

Q5 is outside this slice. Q7 has no outer-learning runtime carrier in this direct
select replay. Q9 uses the offline empty seat set, not a live roster finding.
Q6 and Q11 hold for the supplied predecessor; the live history limitation above
is not discharged by those results. No missing field was filled just to pass Lean.

## Validation

- Base: Ran 156 tests containing 631 assertions., 20 failures, 3 errors.
- Branch full comparison: Ran 159 tests containing 653 assertions., 20 failures, 3 errors.
- Failure names/multiplicities identical. Full suite is not green.
- Focused latest assertions: 85 tests / 291 assertions, zero failures/errors.
  Two final missing-work/unrecorded-refusal assertions were added after the full
  comparison and exercised in this focused run; no source changed afterward.
- Full comparison includes the original 12 outer namespaces, both projector
  namespaces, two menu namespaces and the new outer-selection-history namespace.
- All discovered FUTON_WM store overrides go to scratch. Canonical data count
  **45737 before and after** baseline, branch, focused runs and replay.
- Kondo 0/0, parens OK, diff whitespace check clean.
- Existing Q11-first-selection expectation now omits currentInputDigest from its
  missing-fields list; the new digest really exists, previous remains absent.
- Initial replay script had an unquoted require vector and failed compilation;
  corrected before execution. No broad-suite alias accident.

Artifacts: ~/notes/l5-4e/{first-record-copy.edn,second-selection.edn,record-copy.edn,
selection.json,work.json,selection.lean,work.lean,selection-lean.log,work-lean.log}.
Adjacent JSON holds hashes, exact test distributions and both coordinator rulings.
