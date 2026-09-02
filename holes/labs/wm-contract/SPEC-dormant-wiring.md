# SPEC: the dormant wiring — what each unit should be doing

Author: Claude (claude-1), 2026-09-02, at Joe's request ("the unwired
non-functional units just seem like straightforward deficiencies… if we have a
free hand with determining how those are to be wired, we need to think it
through a bit… probably we could write down at least a specification").

Scope: every unit the equation registry (`aif-equations.edn`) records as
computed-but-unconsumed, implemented-but-unapplied, or off-by-default, as of
the board closing at 86/88 on 2026-09-02. One section per unit: current state
with pointers, what it should do, the free-hand choice named, and a falsifier
in the amendment-12 style (prediction stated before the flip, so the flip can
fail).

**The finding that shapes this spec**: the register's dormant units are not
missing builds. Scalar F is retired AND removed (I5, reviewed 09-01). Its
replacement F_pi runs live behind `FUTON_WM_FPI_POSTERIOR=1` (RUN9/S4: entered
the live posterior on 3 of 4 ticks, 133/145 posterior ranks moved, argmax
unchanged, replay control at delta 0.0). Variational tau runs live behind
`FUTON_WM_TAU_MODE=variational-beta-gamma` (RUN8/S3: gamma moves tick to tick
from G and pi). What remains is one composite default decision (U1), one
adjudication with a stated vehicle (U2), one theory branch with a
recommendation (U3), one empirical question (U4), and one invariant to hold
while the library loop builds the cascade constructor (U5).

## U1. The theory-aligned selection score, default-on

Current: live default selection is the first non-no-op of the G-ordered list
(policy.clj:283-285); tau is inert (:selection-gain-only, fold never fires);
ln E and F_pi enter nothing. The full score `ln E − G/tau_eff − F_pi` exists at
the one seam I2(b2) built (policy/softmax-weights, war_machine.clj:102-121)
behind four coupled flags (TRACE_POLICY_DETAILS → FPI_DARK → FPI_POSTERIOR,
plus TAU_MODE), with `f-pi-posterior-preconditions!` refusing an incoherent
flag set rather than running degraded.

Should: that score IS the live selection law, no flags. The flag chain becomes
the reversal path, not the activation path. The preconditions collapse (trace
details and dark readback always on); every record keeps :tau-source, the
per-candidate F_pi, and the coverage refusals (S4's fourth tick declining on
1/145 uncovered is the declaresCoverage discipline working — keep it).

Falsifier, stated before the flip: over the first 20 default-on ticks, the
replay control reproduces every recorded posterior at delta 0, and every
argmax change relative to the old law carries in its record the term that
moved it (E, tau, or F_pi — computable from the recorded components). A tick
whose argmax changes unexplainably, or a replay delta ≠ 0, reverts the default
and the failure is a C row.

Note the register's own caveat holds until this lands: separated is not
adjudicated. U1 does not claim the new law selects better — it claims the law
the registry cites sources for is the one that runs. Whether it selects
better is U2/U3's question, measurable only after U1.

### READINESS (worklist :U1, 2026-09-02) — what the falsifier run still needs

The readiness suite is `test/futon2/aif/selection_score_readiness_test.clj`
(8 deftests, 193 assertions). It pins the four properties against the recorded
fields S2 (20 ticks) and S4 (4 ticks) plus planted synthetic fields, and it
does not flip anything. What it establishes:

- **(a) replay determinism holds, at delta exactly 0.0, on all 24 recorded
  ticks** — every recorded `:softmax-weights-by-candidate-id` is reproduced by
  `policy/softmax-weights` (src/futon2/aif/policy.clj:148) from that record's
  own `:controller-score`, `:habit-prior-bias`, `:tau` and F_π readback.
- **(b) the attribution is total and, on these fields, unique.** Every one of
  the 24 ticks changes argmax against the default law, and each names one
  minimal set: S2's 20 ticks and S4's declining tick move on `{ln E}`; S4's
  three applied ticks move on `{ln E, F_π}` together. The per-candidate
  identity `full − default = ln E + τ + F_π` is checked exactly (< 1e-9).
- **(c)/(d)** the precondition refusal and the S4 coverage decline are pinned
  as declared behaviour, including that the declining tick's recorded posterior
  is the old law's and *not* what a zero-filled F_π would have produced.

Five things the falsifier run still needs, none of them supplied by any
committed field:

1. **A field where τ ≠ 1.** τ = 1.0 on every one of the 24 recorded ticks
   (`:tau-source :selection-gain-only`, g = 1.0), so no recorded tick can
   attribute anything to τ; the τ arm of the attribution is exercised only by a
   planted field. A τ-attributable argmax change is measurable only under
   `FUTON_WM_TAU_MODE=variational-beta-gamma`.
2. **Sixteen more default-on ticks.** The falsifier is stated over the first 20;
   S4 supplies 4 (3 applied, 1 declined).
3. **S3 is NOT FOUND.** `runs/2026-09-01-s3` holds `ARMS.txt` and `README.md`
   only, and `README.md:3` says the directory is a replay, not a 20-tick stage
   run. The live-τ field the falsifier presumes does not exist yet.
4. **The flip as specified changes the recorded posterior, not the chosen
   action.** S2/S4 ran on `:selection-boundary :strategic-recommendation`, where
   the choice is the head of the G-ordered list (policy.clj:499) and the
   F_π-bearing posterior at `:softmax-weights` (policy.clj:551) is recorded and
   never read. The other boundary (`:actuation`) *does* select by
   `ln E − G/τ` (policy.clj:696), but `f-pi-opts` reaches only
   `strategic-recommendation` (policy.clj:491), so F_π never enters a selection
   on either path. "The score IS the live selection law" therefore needs a
   selector change that no row has yet minted; U1's flip alone would make the
   falsifier's "argmax change" a statement about a recorded posterior.
5. **A decline rate.** 1 of 4 S4 ticks declined on 1 uncovered candidate of 145.
   Over 20 ticks the rate is unmeasured, and the falsifier should state it
   before the flip rather than read it after.

One defect found while building (a), recorded here because it is a trap for
anything that reads these records: **the `rank/N` keys of
`:f-pi-by-candidate-id` are the PRODUCING tick's ranks, not the current
tick's.** Joining F_π by that key instead of by `:candidate-identity`
transposes it between candidates whose ordering moved — on S4 tick 0, 12 of
145 entries sit under a key whose current action has a different identity, and
the replay leaves delta 0 (max weight error 5.3e-6, which reads as a
floating-point wobble rather than as the misalignment it is). Pinned as a
negative control in `rank-key-join-breaks-the-replay-test`.

## U1'. The selector consult — pushed onto this list 2026-09-02

Found by U1's own readiness work (point 4 above) and named by Joe as another
instance of this spec's class: the F_pi-bearing posterior is recorded on every
flagged tick and consulted by no selection path. U1's flip alone would wire the
score into the RECORD, not the CHOICE. The build is worklist row :U10 — a
selection mode whose chosen action is the argmax of the full-score posterior,
off by default, J-gated flip, attribution extended to the chosen action.
Priority within this list: ahead of U3 (the pi_0 comparison reads selection
behaviour) and behind nothing — every claim U1 makes about "the live selection
law" is conditional on it.

### U10 BUILT (2026-09-02) — the selection law, and the prediction it is judged against

The mode exists and is **off by default**. `:selection-law` is a declared option
on `policy/select-action` (`policy.clj:749,754`) with a closed two-member set
(`policy/selection-laws`, `policy.clj:503-519`); on the live path it is read
from `FUTON_WM_SELECTION_LAW` (`war_machine.clj:165-196`) and threaded to the
one call site (`war_machine.clj:5311,5378`). Under `:full-score-posterior` the
chosen action is the argmax of the SAME `policy/selection-scores` vector the
decision's `:softmax-weights` normalises (`policy.clj:157-213, 563-567, 593,
668-670`), so "the score is the selection law" is a fact about one expression
rather than a claim about two that agree. Ties break to the first maximum
(`policy.clj:525-536`) so a tie cannot read as a law change, and the candidate
stays opaque at the seam (U5): the law reads (G, ln E, F_pi) and nothing inside
the action, pinned by
`full-score-law-selects-on-terms-not-on-candidate-internals-test`.

**Two failures, two answers.** Asking for the law when the flag chain can never
produce F_pi throws at the top of the tick (`selection-law-preconditions!`,
`war_machine.clj:198-228`, called at `:5311-5312` **outside** the
`default-mode-select` try/catch) — otherwise every tick records a law it never
ran. A per-tick coverage decline is not that: coverage is complete-or-off by
design, so the selector takes the head it would have taken and records
`:selection-law :refusal` with the envelope's own reason. Throwing there would
kill the tick, and — because the call is wrapped in a fallback — kill it
silently.

**THE PREDICTION, stated before the flip.** Replaying every recorded tick's own
components through `select-action` under both laws
(`chosen-action-attribution-on-the-recorded-fields-test`):

1. **The flip moves the chosen action on 3 of the 3 ticks where the law can
   run** — S4 ticks 0, 2 and 3 choose the **rank-130** candidate where
   `:controller-head` takes the rank-1 head. A live flipped run is predicted to
   move the choice at a comparable rate; a run that moves none is the falsifier
   firing.
2. **The mover set is {ln E, F_pi} together on each**, which is the same
   minimal set (b) attributes the recorded posterior's argmax change to. So the
   posterior and the choice now agree about what moved — the thing point 4
   above said they did not. A field where the choice moves on a set the
   posterior does not is the second way this fails.
3. **21 of 24 ticks fall back, for two different reasons, and they are not one
   number.** S2's 20 carry no `:f-pi-posterior` envelope at all (RUN7's dark
   carry, `:no-f-pi-opts`); S4's remaining tick declines on
   `:incomplete-coverage`, 1 uncovered candidate of 145. Prediction for a live
   flipped run: the decline turns the term off on the first tick of the run and
   leaves it on afterwards (`f-pi-posterior-opts`' own measured cost on S2 —
   19 of 20 ticks at `:unmatched-current-count` 0, tick 1 at 2).

All three numbers are literals in the test, so a field that breaks them fails
the suite rather than being read off a run afterwards.

**What U10 did NOT reach, measured rather than hedged.** On **24 of 24**
recorded ticks the action the record carries is not this selector's output at
all: `war_machine.clj:5438` replaces the controller decision's `:action` with
the mission the R14 strategic selector returned, and that selector is handed
three mission-id strings and a trace id (`war_machine.clj:5406-5410`) — no tau,
no posterior, no score crosses the call. U3 found this and recorded it at
`aif-equations.edn :choices :temperature-update :sharper-than-the-headroom`.
U10 wires the R6 seam and leaves the overwrite untouched, because the overwrite
was outside its acceptance. **The mode existing is not the machine acting on
the full score**; that is one more call away and needs its own row.

**Still J-gated.** No flip is made and no ruling is written. What is now
available to rule on is a mode that runs, a prediction with numbers, and a
suite that fails if the numbers move.

## U2. tau: adjudicate the three arms, then retire the dead fold

Current: three arms run and separate on S2 (three different gammas, all
converge and bracket, 20/20 ticks); nothing measured says which is better.
The default selection-gain fold checks every tick and has never fired live
(:realized-outcome simulation-only, selection_gain.clj:187-193).

Should: adjudicate by retrodiction on the recorded fields — for each arm,
score the arm's posterior against the tick's own recorded next observation
across S2/S3/S4 (no new runs needed; the records carry both halves — this is
RUN4's stated purpose, and its choice-point branches are exactly these). If
the arms do not discriminate on retrodiction, choose by source fidelity
(friston2017's iterate-2.7 is "the formalism's answer, not an option" — the
register's words) and record :grounds :ruling. Then: the selection-gain fold
either leaves the code (I5-style removal row) or stays as an explicitly typed
diagnostic column — never again as the default that silently never fires.

Blocked-on-Joe portion: RUN4's Lean edit (wmRunConformsToWiring,
Holes.lean:6593) is owner joe by his own naming. The retrodiction study does
not need it; the Lean closure does.

## U3. pi_0: recommend the SPM form (ln E in both pi and pi_0)

Current: :open-branches. friston2017's printed form has no E in pi_0; the
authors' own implementation puts it in both (spm_MDP_VB_X.m:963-964, verified
in clone). Ours has ln E in neither. The arms separate whenever ln E is
non-uniform over the aligned field — which it is (3 distinct values, spread
3.04 nats, 20/20 ticks).

Should: the SPM form, for the register's own stated reason — with ln E in
both, (pi − pi_0) isolates what F_pi alone does, which is what the beta
update is meant to measure. Where text and implementation disagree, the
implementation is the authors' answer to their own ambiguity. Falsifier: once
U1 puts ln E live, run the specified comparison (the :interim already
specifies it); if the in-pi-only arm brackets the H1b bound better across the
recorded fields, flip and record why.

## U4. Ambiguity: establish whether it discriminates, or type why not

Current: theory-defined, Lean closed (dacosta2020 eq. 45-48), and empirically
inert on the old records (0 winners / 674 ticks — the S1 "inert quantity"
facade). No row currently asks whether that is a property of the term or of
our C.

Should: a dark retrodiction row — on every recorded field (674 old ticks plus
S2-S4), re-score with the ambiguity term dropped and count argmax changes.
Three typed outcomes, each a finding: (a) changes exist → the term
discriminates and the old zero was field-poverty; (b) zero everywhere because
H(P(o|s)) is near-constant across candidates → the inertness lives in our A
matrix, and the row says so with the variance; (c) zero because C is flat over
the outcomes ambiguity distinguishes → the deficiency is the channel-grain C
(the S1 refusal "a C over channels offered as a C over outcomes"), and the
fix belongs to the outcome-carrier work, not to G. Never leave "ambiguity is
inert" as an untyped zero again.

## U5. The invariant while the constructor lands: wire at the seam, not below it

Everything above lands at policy/softmax-weights — a score per candidate,
softmaxed. That is exactly the seam where the candidate type changes from
action to cascade when the library board's constructor (L6, LA2-LA3; ungated
by Joe's §1c ruling today) arrives. The invariant this spec imposes on U1-U4:
no wiring may read fields that exist only at action grain. Score components
must be functions of (candidate, field) where candidate is opaque; anything
that peeks inside an action to compute its term acquires a named migration
row at the moment it is written. This is what keeps today's defaults from
becoming tomorrow's re-plumbing.

## Proposed rows (stubs — not minted; Joe's nod mints them)

- :U1 :class :I — flip the selection-score defaults; acceptance = the U1
  falsifier run and green, flags demoted to reversal path.
- :U2 :class :RUN — three-arm retrodiction on S2-S4 records; acceptance = a
  table per arm per tick, a chosen arm with grounds, and the fold's
  disposition row minted.
- :U3 :class :C — pi_0 ruling recorded after U1, with the comparison's
  numbers.
- :U4 :class :RUN — ambiguity discrimination sweep with the three typed
  outcomes.
- (U5 is not a row; it is an acceptance clause added to U1-U4 and to LA2/LA3.)
