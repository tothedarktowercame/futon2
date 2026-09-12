# Phase 0 node witness carrier — 2026-09-12

Implements the adopted `TN-node-witness-carrier-proposal-2026-09-12.md`
carrier in the existing fragment merge. No canonical registry, equation,
control-stage, accounting, dossier generator or Lean source is changed.
Generated control outputs go only to temporary directories.

The three owning fragments contain **24 pending reference claims**, not
admitted witnesses: one theorem for fourteen observation coordinates, ten
precision cases, and eight scored plus five typed prediction-error cases.
The retained Clojure readbacks are cited by byte pin; these are historical
measurements, not executions performed by this change. Each case has its own
qualified theorem selector, claim, scope and revision ID.

| Credit | Pin result | Admission result |
| --- | --- | --- |
| R2 / machineObservation | Current census agrees; historic successful handoff pins witness `81a8d33d…`, current file is `2498ed19…`. Divergence retained. | Pending: current census has nil checker exit; reviewed subject receipt absent. |
| R7 / machinePrecision | Both module pins agree with the retained census. | Pending: census has nil checker exit; reviewed subject receipt absent. |
| R3a / machineChannelPredictionError | Census subject `acc0ec35…` differs from current `86c68198…`; census witness `59cb0ef5…` differs from current `96783ef9…`. Both divergences retained. | Pending: census has nil checker exit; reviewed subject receipt absent. |

No historic receipt has been attached as successful verification of newer
bytes. Precision has clean **pin correspondence**, not a newly verified proof.
No Lean checker was run. The reference claims deliberately exclude universal
or current-production equivalence. The prediction-error floor cases retain
the source module's limit: matching rounded doubles does not make 0.01 dyadic.

The executable gates and their exits are retained alongside this file:
`lint.txt`, `parens.txt`, `bb-tests.txt`, `jvm-tests.txt`, and
`merge-controls.txt`. The last runs the real CLI over just the three genuine
fragments twice and compares complete output bytes, tests split/check
round-trips, and changes the dependency authority to detect stale output.
Synthetic admission receipts then exercise stale bytes, missing declarations,
nil verification, missing readback and changed meaning under the same ID.
Refusals retain the previous output byte-for-byte; artifact failures also
produce no file when no prior output exists. The positive admission fixture
uses actual F8 observation source bytes but **synthetic checker/review data**:
it tests the resolver and is not proof evidence.

The merge returns binding admissions only. `witnessed-bindings` fails closed
unless a consumer supplies a resolved preceding rung of 3 and a FUNDAMENTALS
cap permitting 4. It preserves polarity and scope and never supplies higher
rungs. Integrating this helper with dossier/accounting derivation, including
removing any old readiness bypass, remains the separately reviewed step.
Canonical `--check` will be out of date until that authorized merge occurs.
