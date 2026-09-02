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
