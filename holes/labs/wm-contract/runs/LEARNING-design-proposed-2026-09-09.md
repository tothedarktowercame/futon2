# PROPOSED: AIF-compatible learning for the War Machine

2026-09-09. Design only. Joe must rule before implementation. Authority:
SESSION-model-choices-2026-09-09.md sections 3–4; Item 25 commissions this
note, not the learner. LR1–LR7 are evidence references, not a restoration list.

## Intended claim

A named, single-level categorical model updates state beliefs as observations
arrive, learns its observation likelihood from admitted evidence, persists the
posterior and uses it in later inference and prediction. Slower structural
revision proposes a new model version with explicit admission. Claim only the
parts whose execution and subsequent consumption have been demonstrated.
Do not call scheduler-frequency accumulation observation-model learning, or
call an unused BMR report a closed learning loop.

Theory: Da Costa et al., *Active inference on discrete state-spaces*,
https://arxiv.org/html/2001.07203v2, sections 5, 8–9 and Appendix A.1. Its
Dirichlet learning uses observation/state sufficient statistics; trial-boundary
updates can become the next trial's prior. This licenses a schedule, not one
terminal learning phase. The model and schedule below are implementation
proposals, not statements that the source mandates these engineering choices.

## Model and boundaries

Start with the entity-status domain already enumerated by
src/futon2/aif/belief.clj:38 and its explicit categorical filter at :297-345.
Keep the observation alphabet, latent status alphabet, orientation of tables,
and entity IDs in a versioned manifest. A[o,s] is a normalized observation
likelihood conditional on status. Existing transition-model-v1 and initial
prior remain declared fixed components in the first implementation; they are
not claimed learned. The entity event alphabet must be enumerated and joined
explicitly to A's rows before code is commissioned. Unknown labels refuse
admission rather than silently extending the alphabet.

Do not claim this entity model is the entire WM outcome model. Its predictions
already serve entity-health estimates; the continuous fourteen-channel model,
the twelve-outcome disposition model and its open observation bridge are
separate. Learning A does not close E-C-realization section 1b. No scalar risk
is inserted as a preference distribution. C remains an operator-authored
preference module, not a success-frequency estimate.

## Variables and their complete update contracts

### 1. q_t(s): state belief, updated at each admitted observation

Evidence: a typed entity event with observation category o, entity ID, unique
event ID and timestamp; use the model version captured before the event.
Equation for the proposed untempered categorical case:
q^-_t(s') = sum_s B(s'|s,u) q_(t-1)(s);
q_t(s') is proportional to A(o_t|s') q^-_t(s').
This is inference about current state, not parameter learning. Current filter
uses an evidence-dependent exponent kappa(w), so an implementation must either
record that declared powered-likelihood model or introduce an explicitly
versioned untempered mode; do not quietly treat the current tempering as the
untempered equation. This is an explicit design choice for Joe's review.

Schedule: once per unique admitted event, before the next decision using it.
State: entity posterior, event frontier and A/B/D version hashes; retain absent
and superseded observation status. Next consumers: categorical-filter-step and
entity-health predictions feeding candidate scoring. New entities use the
named D prior; removal is a recorded domain transition, not data deletion.
Zero normalizer produces a typed model/evidence contradiction, not a uniform
posterior. This refusal is a proposed change from the historical fallback and
must be tested rather than assumed already true.

### 2. a[o,s]: observation-likelihood Dirichlet parameters

Evidence: admitted o_t and the corresponding q_t produced using the PRE-update
model; learning record cites both event and posterior. For one observation:
a_new[o,s] = a_old[o,s] + 1[o=o_t] q_t(s).
For a batch, sum each event's sufficient statistic once. The variational
expected-log likelihood is digamma(a[o,s])-digamma(sum_o a[o,s]); the posterior
predictive is a[o,s]/sum_o a[o,s]. They are different consumers and must never
be substituted without an explicit inference approximation. The first design
uses posterior-predictive filtering as an explicitly named approximation; its
claim is categorical Bayesian adaptation, not exact reproduction of every
variational update in the paper. Review must settle that claim boundary.

Schedule: commit after every accepted observation batch, between decisions;
freeze the model throughout one candidate comparison. Persist the posterior
before selecting under it, with a transactional event frontier and hash.
This can occur repeatedly within RUN4, not only at the run's end.
State: schema, domain hash, prior provenance, sufficient-statistic matrix,
event-to-statistic ledger, source model version and successor hash. Initial
concentrations must be declared and reviewed (proposal: total prior strength
one per latent-state column, distributed according to the existing A column).
A numerical zero is not automatically a structural zero. If an A column is
not strictly positive, represent its support explicitly and ask whether zeros
are structural; do not fabricate positive concentrations by flooring.
Next consumers: load the successor A for next categorical filtering and
prediction, emit its hash in both records. A model activation failure refuses
the claimed learning run rather than continuing with an unreported old A.

Correction handling: retain old contributions, retract/replace only under the
observation's authorized supersession. Recompute from prior + admitted active
contributions; no repeated addition when a step is replayed or accepted twice.
This learner does not train the action-conditioned B table: that requires paired
state-transition evidence, not a capability×mission discharge counter.

### 3. n_E(k): habit counts, distinct from success or model likelihood

Evidence: actual selected identity at the declared grain. Existing tactical
habit_prior and forward-only strategic_habit remain separate. Update
n_E(k) <- n_E(k)+1 once per admitted selection; predictive mass is proportional
to n_E(k)+alpha over the feasible menu, with duplicate identity handling as
already declared by habit_prior. Alpha is persisted; no decay or forgetting is
newly proposed. Schedule: the selection boundary, with recorded event identity.
State: existing grain-labelled stores and provenance. Next consumer: the
already-authorized scheduler habit/beta boundary. Strategic counts are collected
only; final strategic selector retains fixture E_S until separately authorized.
A habit-in-both beta rule does not authorize strategic-prior promotion.

### 4. M: slower structural model revision

Evidence: frozen prior, accumulated observations/statistics and parent model
hash. Proposal: compare a finite set of explicitly specified reduced priors
using the existing BMR kernel and its delta-F convention/threshold (-3), with
both acceptance and principled-no-change recorded. A reduction must refer to
THIS learner's domain. Existing capability×mission A4a counts cannot be imported
as if they were observation/status counts. Candidate equivalences are proposals
requiring interpretation; do not mechanically merge distinct authored statuses.

Schedule: an explicit review boundary after a configured number of new admitted
events (number to be set with the run budget), while the active version stays
frozen. This is slower than state inference, not necessarily post-RUN4.
State: parent hash, proposal list, evidence terms, rejected/accepted dispositions,
projection of old sufficient statistics and versioned successor manifest.
Next consumer: model loader and next inference/prediction record after owner
admission. A no-change record retains the current model and explains why.
No available admissible reduction is an honest outcome, not grounds to invent
one. Initial implementation can validate the proposal/hold path without claiming
an accepted change; an accepted-change claim needs a separate executed case.

### 5. Optional cascade proposal model: not required as a second AIF learner

If commissioned, maintain a separate success/failure posterior per executed
cascade identity, update from independently adjudicated outcomes (unknown holds),
and fit a proposal reward by held-out predictive loss. Do not assign a whole
cascade's success to every member without an explicit credit model. A GFN can
then fit P(S|m) proportional to exp(beta R_hat(S,m)) using trajectory balance,
including the backward-probability term and conditional partition function.
Schedule: at named adjudication/retraining boundaries with frozen training/test
splits. State: labelled outcomes, posterior/reward/trainer versions, library
membership hash and proposal distribution. Next consumer: candidate proposer
must identify the new model version and the candidates it actually supplied.
This is an auxiliary proposal mechanism, not canonical AIF parameter learning
by virtue of using GFN. Preserve July's diversity-only standing, shuffle and
anti-gaming controls, and no claim of superior success. Not selected by this
proposal as a prerequisite to implementing observation-model learning.

## Validation and proposed order

1. Settle the observation alphabet, support/prior and tempered-versus-untempered
   inference choice. Bind each input to the existing observation admission
   envelope; published decisions must not double as self-authored success labels.
2. Pure learning kernel: normalized predictions, sufficient-statistic update,
   duplicate-event identity, correction replay and explicit support violations.
   Use at least one retained record-shaped fixture, not only two-state toys.
3. Two successive isolated decision executions: old model predicts; independent
   observation is admitted; q and a update; successor persists; next decision
   consumes its hash. Inspect predictions and score inputs, not just winner.
4. Disconnect the successor consumer: the test MUST reject the stale hash even
   if selection is unchanged. Shuffle labels and hold the model fixed as separate
   controls for predictive improvement; a functioning update alone earns no
   accuracy claim. Report predictive log score on observations not used to fit
   that version, including failures and unknowns with separate denominators.
5. Structural proposal/replay and admission control; only then optional cascade
   proposal retraining. Each implementation slice gets an independent reviewer.

Ruling questions are concrete: approve the first learned object A over the
entity-status model (rather than falsely claiming a learned full WM model),
settle its inference approximation/support/prior, and choose the structural
review cadence. Exact run configuration and a RUN4 learning gate follow that
ruling. Nothing in this document implements or silently defers those choices.
