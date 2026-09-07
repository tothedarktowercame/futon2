# Figure 3 red components — tracked so they are not forgotten (2026-09-07)

Joe, 2026-09-07: "this still has many red rows, even if they don't have the
same priority as our fundamentals it would be good to ensure they aren't
forgotten." This file is the register. One section per red component of the
wm-status infographic (Figure 3), each with: the measured signature, the
diagnosed cause (measured today, not restated from the stale 2026-09-03
receipt where fresher evidence exists), a class, and the route to green.
None of these outranks the fundamentals (F10/F11/F12, V7/V8, RUN4/RUN13);
the point of this file is that "lower priority" stops meaning "unowned".

Classes used below:
- **stale-attestation** — the machine moved; a pin/binding/fixture captured
  earlier did not. Remediation is a re-run of the capturing machinery and an
  honest re-pin (live-pin rule: pins come verbatim from a live record).
- **genuine-debt** — the red measures something really missing or broken.
- **by-design** — the red is the honest rendering of a deliberate state
  (ruled refusals, mid-work dirt); candidate for the accepted-red register
  (`futon2/checks/wm-status-accepted-red.json`) with a review-by date, not
  for "fixing".
- **environmental** — the measurement process, not the measured thing.

Baseline receipt: p4ng `empirics-futon/wm-status-receipt.json`
(2026-09-03T22:40Z, U45, overall DEGRADED-NEW, 7 red / 5 green). A fresh
`wm_status_report.py` run was started 2026-09-07 ~19:40Z; its bounded-suite
halves already re-measured (receipts under /tmp/futon-bounded-tests/,
stamp 1788810006) and the per-component diagnoses below used those and
direct re-runs of the underlying checks.

## 1. strict-lint (exit 1) — stale-attestation

Signature (2026-09-03 strict report `/tmp/wm-status-strict-1788475216.edn`):
structural `:pass? true`, but `:bindings-fresh? false` — **18 declarations
stale**, remediation `{:rerun-and-rebind 18}`, zero uninspectable. The 18
are the core AIF vocabulary: softmax, variationalFreeEnergy, GenerativeModel,
expectedInformationGain, ambiguity, actGate, aliveness, the four kernels
(Predictive/Parameter/Transition/PolicyPrior/observation), PrecisionMap,
DirichletConcentrations, bayesFactorThreshold, HaveWantArrow, Fold,
modelReductionFreeEnergyChange. The bound Clojure code drifted under them
(the loop's own productive edits).

Route: mechanical — re-run the binder against current code and let the
honest freshness verdict win. One small worklist row.

## 2. futon2-suite (exit 125, 1 failure) — stale-attestation

`positive_proof_receipt_test/honest-positive-still-passes` fails because
`receipt/validate` on `softmax-positive-receipt.edn` returns
`{:pass? false :failures [:positive-source-drift]}` (re-run 2026-09-07):
the receipt pins Lean source that has since moved (F8/F12 Lean work).

Route: re-capture the positive receipt against current source via its own
generator (never hand-edit), which also exercises the receipt machinery —
a cheap conformance re-demonstration of exactly the kind the D1 live-run
rider prefers.

## 3. futon3-suite (exit 125, 8+ failures, 4 errors) — three distinct things

Measured 2026-09-07 (receipt stamp 1788810006, log read in full):

- **aif_sampling_test (4 errors + ~7 failures): stale-attestation.** futon2
  commit `497dca72` (AC7, 2026-09-02) deliberately made the Fulab adapter
  *refuse to sample* when the context lacks a measured
  `:outcome-size-surplus` — no `:chosen`, no `:tau/:logits/:probs`, an
  `:absent` record instead. The tests' contexts never supply the field, so
  every selection is the refusal record (`frequencies {nil 100}`, NPEs on
  nil probs) and the tests still assert pre-AC7 semantics. Route: update
  the tests to AC7's contract — supply the surplus on sampling paths, and
  add the missing case that *asserts the refusal record* when it is absent.
- **library_graph_lint_test/live-library-passes (5 failures): stale pins
  AND genuine debt.** The test pins 92 why-edges / 104 patterns; the live
  library measures 1160 / 859 (the why-how loop grew it ~10x) — re-pin per
  the live-pin rule. But the same lint run also reports **683 unresolved
  why-targets and 5 why-cycles** in the live library: genuine library debt,
  directly relevant to the historical-cascade mining loop, which reads this
  library (MINING-historical-cascades.md). The debt half belongs to the
  library-loop lab, not this worklist; the pointer lives here so Figure 3's
  red is attributed.
- **pthread EAGAIN (native-thread-exhaustion true, 2 markers at ~29.8s):
  environmental.** The bounded runner's thread budget saturates
  (pids-peak 1280); the resource-status is dirty, so some failures may be
  double-counted. Worth one look at the bounded runner's limits, after the
  substantive fixes, to see if the suite goes green without it.

## 4. referent-drift (exit 1, findings) — stale-attestation, needs eyes

`p4ng/detect_drift.py` re-run 2026-09-07: cited units whose content hash
drifted under paper citations (sec-glossary.tex, sec-catalog.tex,
app-eqtutorial.tex, sec-future-work.tex), across enact.clj,
preferences.clj, trace.clj, belief_test.clj, cascade_lane.clj,
M-zaif-harness.md, holes/ …, plus **1 never-vetted new file**
(`futon2/src/futon2/aif/policy_free_energy.clj`, cited by
app-eqtutorial.tex). Unlike strict-lint this is not a blind re-run: each
drifted unit needs a human-or-agent check that the citation still says
something true of the moved code, then a re-vet stamp.

Route: one row, sliceable per cited section; the vetting is the work.

## 5. mission-criteria-gauges (3/9 measurable) — genuine-debt

M-zaif-harness-v1: 3/3 measurable, gauges supplied. M-expressions-of-
interest: **0/6, every criterion `no-producer`** — nothing in the machine
emits an observable for any of the six EOI criteria. The component renders
honestly (per the WR-8 row's "never a hand number"). Green requires
building producers for M-eoi's criteria, or revising the mission's criteria
to ones the machine can measure — the latter is a Joe-shaped call.

Route: park as a named row; surface the produce-vs-revise choice to Joe
when it reaches the front.

## 6. sorry-category (exit 1, 10 sorries) — by-design, accepted-red candidate

3 deliberate-implementation-refusal + 7 permanent-external-attestation
(+ 4 witnessed-instance-obligation labels). The 7 are permanent by
declaration; the 3 are now governed by the F12 sorry ruling (staged
amend-to-existence — they retire as their witnessed instances accumulate,
see aif-equations.edn :choices :organise-sorry). This component is the
honest rendering of ruled state, red forever unless accepted.

Route: propose an accepted-red register entry with signature pinned to the
exact classification counts and clears-when tied to any count moving.
Writing the acceptance is a policy act — proposed here, not done; needs
Joe's nod.

## 7. workspace-gate (exit 1) — by-design (mid-work dirt), verify then accept

futon2 and p4ng measured dirty at report time — expected while the loop
and lanes are writing. Before accepting: one pass over the actual dirt to
confirm it is all work-in-flight (no orphaned junk like stray .bak files);
then either an accepted-red entry scoped to dirty-while-loop-runs, or a
gate refinement that distinguishes tracked-dirty from untracked-orphan.

## 8. preemptive-repair gate (build-gate test red) — genuine-debt, one finding

The entire red is one artefact-boundary finding:
`C484-refusal-harvester.md:23` cites
`holes/labs/wm-contract/refusal-sweep-state.edn`, which exists on disk
(regenerated at every publish by the harvester) but is **not tracked** —
it was tracked once (810d8ca4) and later dropped, so the committed doc
cites a path a fresh clone will not have. Fix is small but has two honest
arms (re-track the generated file vs annotate the citation as
generated-not-committed) and the lint's convention decides which; a
one-slice row, not a blind edit.

## Sequencing

The F10/F11/F12 + V7/V8 + RUN4/RUN13 track keeps priority. The rows
proposed above enter worklist.edn in a post-publish idle gap (board-write
discipline), **after** the F10 :outcome-domain transcription lands, as
small rows in the house style — most are one-file/one-behaviour slices.
Items 6 and 7's acceptances wait on Joe. This file is the meanwhile-record:
if the rows are not yet on the board, this is where the reds are owned.
