# L5-4d-5 — Outer prediction classes

Base `3c6add76d`; branch `codex11/l5-4d-5-outer-classes`.
No merge, reload, click or canonical data writes.

## Producer and projector

Every arrangement is scored and retained. Classes use
`[action-identity/digest(prediction.outcomes), G-units]`. The digest canonicalizes
map keys exactly; G-units mirrors the exporter's BigDecimal(double), six-place
HALF_EVEN conversion. The requested cascade-selection module at this base has
no g-units or prediction-digest helper, so no inner namespace was imported.
Class id = SHA256 of pr-str of that vector. An assertion pins every member's
G-units to the class key (equality at the declared millionth scale).

Minimum and ties are over classes. The representative is the first arrangement
in menu order within one class, explicitly recorded. All arrangement rows remain;
construction policy-count counts classes, each construction arrangement carries
its class id. Abstention is unchanged. Forbidden representatives remain forbidden;
grouping does not erase a refusal or remove the task from the field.

The projector exports class identities for construction/compared policies,
G, outcomes-prediction digests, and the chosen decision. `classMembers` carries
arrangement names. Records without classes retain their prior projection.

## Actual retained-field replay and classes

Production select, pinned 609-row pass + 17 unread exclusions, live relation
context from `focus/relation-context`, and makeup-initializer state, exactly as
L5-4d-4. This is an offline tooling replay, not a newly charged live run.

Chosen class:
```edn
{:id "ef0fe6fc38e0d40537aa5f3164c14cbe6700a1db6d76592a0643b58527e72f26"
 :members [:only-what-can-start-now :giraffe-order]
 :representative :only-what-can-start-now
 :pick "M-pattern-retrieval-calibration"
 :G 2.768527879893126 :g-units 2768528}
```
Other class:
```edn
{:id "6fce40cce8cc1dcf1139a478a5077d0227c05ed2e571b2cc7336bd2ab5bb2a2b"
 :members [:find-out-before-building]
 :representative :find-out-before-building
 :pick "M-interim-director"
 :G 4.029697095623827 :g-units 4029697}
```
Abstention G = **4.787491742782046**. Full classes, terms, predictions and relation
receipts: `~/notes/l5-4d-5/summary.edn`, `selection-result.edn`,
`relation-receipts.edn`. No prediction or relation was substituted.

## Checker and actual Lean evaluation

The script copied ac5bc477, replaced its selection section and removed its old
synthetic outer-task-selection stub. It exported both work and selection runs,
generated Lean via wm-check-run-facts, then evaluated each using:
`lake env lean /home/joe/notes/l5-4d-5/{selection,work}.lean`
(separate invocations; both exit 0). Mathlib4 head `5025ad1263`.

Selection outcomes, exactly as printed:

| Requirement | Outcome |
|---|---|
| Q1 | holds |
| Q2 | holds |
| Q3 | holds |
| Q4 | unverifiable |
| Q5 | unverifiable |
| Q6 | unverifiable |
| Q7 | unverifiable |
| Q9 | violated |
| Q11 | unverifiable |

Q4's preference/terms projection remains NR despite retained class terms (outside
this slice). Q5 has no selection interpretation carrier. Q6/Q11 lack the
selection-level previous/current decision-input carriers. Q7 has no outer-learning
status because this is a direct select replay, not the pending-learning runtime.
The supplied offline snapshot has an empty seat set; Q9 is not a finding about
the live roster. No NR value was supplied solely to make Lean pass.

## Tests and gates

- Base: Ran 155 tests containing 626 assertions., 20 failures, 3 errors.
- Branch: Ran 156 tests containing 631 assertions., 20 failures, 3 errors.
- Identical pre-existing failure names/multiplicities; full suite is not green.
- Focused scorer/relation/projector/checker: **84 tests, 280 assertions, 0/0**.
- Requested 12 outer namespaces + both projector namespaces, plus the two menu
  namespaces; scratch FUTON_WM overrides are listed in the notes JSON.
- Canonical data counts **45737 before and after** baseline, branch and replay.
- Kondo 0/0, parens OK, git diff whitespace check clean.

Changed old tests: same-prediction same-pick ties now select one class; unknown
relations collapse to one stopped policy and refuse on the unchanged abstention
floor; the retained field now selects with two classes; construction equality
ignores the newly attached class label; projector choice/prediction comparisons
use class identity/outcomes. The equal-G distinct-prediction negative is a
controlled score-boundary fixture with different picks; real scoring remains
covered by the pinned-field witness. No baseline failures were suppressed.

Complete log hashes and failure comparison are in the adjacent JSON. The raw
replay, record copy, both exports, generated Lean, and stdout are retained in
`~/notes/l5-4d-5/`. No source beyond the selector/projector and their tests changed.
