# L5-3 implementation — inner stopping carrier

Base 3aab49c91, branch codex11/l5-3-impl-inner-abstention. No reload or merge.

## Behavior

inner_abstention.clj moves the scratch model comparison into production, version 1.
It scores no actions under each acting candidate's own retained progress/class
models, C, horizon and initial belief; empty wants refuse :empty-want-stop.
Node-fit ambiguity and transition information are zero for no action.
Full predictions, their digest, model digests, horizon, initial belief and
normalization declarations travel alongside terms and G.

family_selection scores/attaches that carrier beside each computed shape-G
candidate. policy selects the existing admissible minimum first, then compares
that reference to its own stopping G. If it beats stopping, enactment continues.
If it does not but another admissible candidate beats its own stopping score,
the selector refuses :pick-not-below-abstention; no convenient alternate reference.
If none beats stopping, it returns :abstained, :case :abstain-on-task with the
admissible minimum as :chosen-reference and :cross-lens true.
The per-policy map retains every compared entry, including forbidden candidates;
the comparison itself uses the law's admissible domain.

SC carries :abstention {:per-policy ... :chosen-reference ...
:rule :stop-under-the-minimum :cross-lens ...}. The family/declaration paths
preserve the whole certificate on a stopped decision; no repair/author runs.

The decision gate validates complete candidate/reference joins, finite terms
summing to G, prediction digest and horizon lengths, and the rule version.
A chosen decision is checked against the candidate's certificate G, not a
duplicated top-level score. For abstention it independently recomputes the
admissible minimum/reference and checks no admissible candidate beats stopping.
The existing posterior/selection checks remain in force for acting decisions.

The projector validates the carrier, emits exact rational G at 10^6 plus its
source double, prediction digest, and diagnostics.abstentionAudit. Audit metadata
is outside the fixed RunFacts field set. Historical records without the carrier
keep their previous typed absence. Legacy generic selector callers with no
stopping carriers retain their path; partial attachment refuses. This packet
arms the shape-G family producer, not a fictitious legacy carrier.

Necessary adjacent change: full_loop_runner/abstention-carrier rebuilt the
decision's map and would drop :case/:target. It now preserves those two fields;
the new regression verifies preservation. That lets the projector emit
abstain-on-task from the final record, not just the in-memory decision.

## Validation

- Main baseline: 136 tests / 907 assertions, 0 failures / 0 errors.
- Changed: same seven namespaces plus inner-abstention-test:
  140 tests / 933 assertions, 0 failures / 0 errors.
- Final gate hardening (certificate G source): inner-abstention + decision-gate
  rerun, 24 tests / 505 assertions, 0 failures / 0 errors.
- Namespaces: inner-strict-minimum, selection-law-candidate, decision-gate,
  cascade-shape-g, wm.family-selection, wm-run-facts, wm-check-run-facts.
- Three pinned original models (376d76ff, 3415b671, ac5bc477) match the prototype
  stopping G to 1e-9; their file SHAs are asserted.
- Controls: empty-want refusal; no policy beats stop; reference must be the
  admissible minimum; minimum loses but another wins => typed refusal;
  malformed reference/G/digest refused; historical projection stays absent.
- Kondo: zero errors; four runner warnings and informational findings identical
  to baseline (line offset +1). New/other touched files have no warnings.
- check-parens.el and git diff --check pass.
- Test env redirects all discovered FUTON_WM_* root/dir/log/lock/config inputs
  to scratch; canonical data/ count 45737 before and after every test run.

## Hand-built replay and Lean

Artifacts under ~/notes/l5-3-impl/: replay.clj, each *-record.edn, .json, .lean,
and *-lean.log. Both checker-generated Lean files execute with exit 0.
Historical records are never overwritten. Ranked entries use retained candidate
G, predictions, models and beta. The new selector supplies the chosen action and
stop reference. The copied Q6 terminal is re-sealed with that action; other
historical facts/absences are left intact. Empty snapshot fallbacks are explicitly
not live census evidence. No historical conformance claim follows.

376d76ff: minimum policy-34c85cf68e52022bd8ae, acting G 1.4034185419566536,
stopping G 3.9376369783846075. This differs from the historically enacted policy
used in the discovery table; the reference is now the new law's minimum.

ac5bc477: policy-78fb2aa6ddcc719988e1, acting G 1.6359186228431266,
stopping G 2.8636216006595965.

| Run | Q1 | Q2 | Q3 | Q4 | Q5 | Q6 | Q7 | Q9 | Q11 |
|---|---|---|---|---|---|---|---|---|---|
| 376d76ff | violated | holds | holds | holds | holds | unverifiable | holds | violated | holds |
| ac5bc477 | holds | holds | holds | holds | holds | unverifiable | violated | violated | holds |

Q6's historical refusal-history carriers remain absent; no history was invented.
Q9 has no authorized factorized certificate. This packet supplies the stopping
comparison, not a whole-run closure.

## Explicit limits

Cross-lens comparison is the authorized approximation, not one shared terminal C.
Ties between acting prediction classes still take the existing typed refusal
before this comparison. If an acting G exactly equals its stop G, the authorized
“no policy beats stop” branch abstains, but Lean's strict abstention inequality
will not hold: this packet does not weaken Q11 or claim equality conforms.
No new preference numbers or shared terminal model are introduced.

## Evidence SHA256

- replay.clj: a2111882b8bd285d5f117cbe4ade4851ee4e5047c3a7ab794c82295d98e063ed
- replay.log: 53a586c9cb116add6bd5f12781fd95affcba3a70a2c423a970e3f47c50972abf
- 376d76ff-lean.log: 87415e0e2cf0056d671d4623e5b87063e6066084fc1a236885453f192a7bdbd2
- ac5bc477-lean.log: cab4e61b7fb2144ee4d7d11551075d4bb656db1c2d133ca63792fab689c2bc16
- baseline.log: c596831e6a0c90f2aea5deca7adc03d8bda6badcf31c9963cb198d74e4788356
- final2.log: 391ce0c074a2c79a156d022b3c124aec757468af76cf4fea0530397f2fb70a56
- gate-final.log: bbc91983406ce41c4ad05bfb5f088f648e891ba93836ad93918bd118cab5c8af
