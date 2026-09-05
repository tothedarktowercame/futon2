# Per-node problem dossiers — pilot on R4, R5, R6

**Scope and numbering.** This is a discovery census, not a change to the War
Machine.  `R4`, `R5`, and `R6` below always mean the numbering in
`p4ng/empirics-futon/control-stages.edn`.  The contract and catalogue agree on
R4 and R5, but diverge on R6: contract R6 is softmax selection with abstention,
whereas catalogue R6 is the candidate pattern action space
(`p4ng/R-concordance.md:20-27,48-54`).  Accordingly the R6 dossier states both
neighbouring problems and identifies the scheme at every citation.

## R4 — Forward model

### The problem it solves

R4 solves the problem of evaluating an action before enacting it: the
control-stage roster places **“Forward model”** in EVALUATE
(`p4ng/empirics-futon/control-stages.edn:21`; control-stage numbering), and the
operational criterion requires a pure `(state, action) -> next-state-distribution`
which **“the agent uses to score candidate actions before taking them”**
(`docs/futon-aif-completeness.md:110-114`;
contract R4).  The catalogue states the maintenance and fidelity problem more
strongly: **“two implementations of the same dynamics is two places for bugs
to hide”**, so
both should call one pure deterministic dynamics kernel and the predictor alone
should add uncertainty (`p4ng/sec-catalog.tex:204`; catalogue R4; the current
paragraph has no `pat:r4` label).  The equation
registry gives the object that such evaluation must produce: **“Q(o|pi) :=
sum_s A(o|s) Q(s|pi)”**, with state rolled forward from `mu` through `B` over
depth `T` (`holes/labs/wm-contract/aif-equations.edn:111-115`; control-stage
R4).  Thus the node's problem is not merely forecasting a channel delta: it is
constructing a policy-conditioned outcome distribution from the current belief
and the declared observation and transition models, while ensuring the live and
predicted dynamics cannot silently become different programs.

### Residual problems it does not solve

- `:implementation-flaw` — The shipped producer is only an action-grain,
  one-step Gaussian-moment proxy: it passes `nil` as state, uses hand-tuned
  action-type deltas, and does not return a normalized policy-indexed outcome
  distribution (`holes/labs/wm-contract/FUNDAMENTALS.edn:93-102`).  This is an
  implementation flaw because the required Q constructor is already specified;
  the census finds no runtime function constructing it from machine belief.
- `:both` — There is no machine-level map from the belief carrier to the model's
  state distribution, and the nearest four belief-to-channel predictors have no
  runtime caller (`holes/labs/wm-contract/FUNDAMENTALS.edn:112-141`).  The types
  must be joined in the AIF model and that join must then be implemented, so this
  residual belongs to both the declared model and its runtime.
- `:implementation-flaw` — The runtime transition used by belief update is
  action-independent, while the only action-conditioned predictor is not a
  state-transition kernel; consequently there is no `B(s'|s,u)` to roll forward
  (`holes/labs/wm-contract/FUNDAMENTALS.edn:143-176`).  The standard controlled
  transition is already named, so the missing machine inhabitant is an
  implementation failure rather than a new AIF term.
- `:both` — R4 predicts operational effects but not the parameter-learning
  observations required for policy-conditioned EIG
  (`holes/missions/M-aif-policy-conditioned-eig.md:108-129`).  The mission must
  declare a versioned experiment model and the runtime must implement it; an
  entropy helper alone cannot close either half.

**Would we know? — missing.** `clojure -M:test -n
futon2.aif.forward-model-test` checks the scoped proxy (the existing check and
its limits are catalogued at `docs/futon-aif-completeness.md:116-123`), but no
test demands a machine-derived, normalized `Q(o|pi)` with two policy rows or a
shared live-step caller.  Nearest executable artifact:
`holes/labs/wm-contract/FUNDAMENTALS.edn:65-110`, whose falsifier names exactly
the constructor that would overturn the finding.  Absence search:
`rg -n "PredictiveOutcomeKernel|Q\\(o\\|pi\\)|normalized.*outcome|shared.*step" src scripts checks test holes/labs/wm-contract`.

## R5 — Expected free energy core

### The problem it solves

R5 solves how to compare predicted policies for both what they are expected to
achieve and what they leave uncertain: the roster places **“Expected free
energy core”** in EVALUATE (`p4ng/empirics-futon/control-stages.edn:22`;
control-stage numbering).  The operational criterion requires at least risk and
ambiguity, **“both ... computed against the predictive forward model from R4”**
(`docs/futon-aif-completeness.md:125-138`; contract R5).  The catalogue adds the
audit problem: **“calling every useful controller term ‘EFE’ makes tuning
indistinguishable from storytelling”**, so canonical risk and ambiguity must be
persisted separately from named engineering augmentations
(`p4ng/sec-catalog.tex:237`; catalogue R5).  The registry makes those meanings
precise: risk is `D_KL[Q(o|pi) || C]` and ambiguity is
`E_{Q(s|pi)}[H(P(o|s))]` (`holes/labs/wm-contract/aif-equations.edn:116-121`;
control-stage R5).  R5 therefore solves both a ranking problem and an
accountability problem: compute the canonical terms over the same predictive
objects, retain their identities and units, and expose why the ordering moved.

### Residual problems it does not solve

- `:implementation-flaw` — The mathematical EIG kernel exists, but the
  generative experiment model, `Q(o|pi)`, and simulated posterior updates do not
  (`holes/missions/M-aif-policy-conditioned-eig.md:20-42,177-180`).  Canonical
  nats-EFE therefore still lacks its parameter-learning leg; posterior spread or
  action-posterior entropy is explicitly not a substitute.
- `:both` — The WM has neither a declared joint Outcome/C suitable for `G(pi)`
  nor a runtime Q conditioned on a cascade; current formulas have theory-shaped
  names but action- and channel-grain arguments
  (`holes/problems/P-validated-R5.md:24-29,79-90`).  Declaring the carriers
  extends the current AIF account, and rejecting action/channel substitutions
  requires implementation, hence `:both`.
- `:implementation-flaw` — An epistemic term that never changes a choice is
  decoration rather than a non-degenerate active-inference contribution; the
  named validation record says the WM's ambiguity term changed zero winners in
  the cited 674-tick measurement (`holes/problems/P-validated-R5.md:148-170`).
  The non-degeneracy property is already specified, so this is an implementation
  failure rather than a need to extend AIF.

**Would we know? — missing at the conformance bar.** `clojure -M:test -n
futon2.aif.efe-test` checks additivity, ordering, modes, and the existing
action-grain implementation (the scoped check is listed at
`docs/futon-aif-completeness.md:131-145`).  The missing check is the executable
property specified in `holes/problems/P-validated-R5.md:49-69`: a declared
Policy/Cascade/Outcome/C, a derived Q, `G(pi)` over any mission, and refusals for
the named facades.  Absence search:
`rg -n "nonDegenerate|G\\(pi\\)|G_eq_expectedFreeEnergy|derived.*Q|refus.*single action" src test checks`.

## R6 — Candidate action space and selection

### The problem it solves

Control-stage R6 occupies SELECT and is labelled **“Candidate action space”**
(`p4ng/empirics-futon/control-stages.edn:23`; control-stage numbering), although
the original contract R6 criterion is the adjacent operation: softmax selection
with temperature and an abstain branch, where **“the agent declines to act”**
under too much predictive uncertainty (`docs/futon-aif-completeness.md:147-157`;
contract R6).  Catalogue R6 states the domain problem: an unrestricted menu
makes evaluation unstructured and **“selection is dominated by recency”**, so
retrieval and gates must
construct a bounded set of pattern IDs with explicit inclusion and exclusion
reasons (`p4ng/sec-catalog.tex:239`; catalogue R6).  The equation registry
contains both halves: `pi` ranges over the candidate action space
(`holes/labs/wm-contract/aif-equations.edn:143-145`; control-stage R6), and the
posterior is `softmax(ln E(pi) - G(pi)/tau - F_pi(pi))`
(`holes/labs/wm-contract/aif-equations.edn:152-157`; control-stage R6).  The node
therefore solves formation of a meaningful comparison set and selection from
it, including the ability to decline when that set does not support a warranted
choice; treating either half alone as the whole R6 problem hides the concordance
mismatch.

### Residual problems it does not solve

- `:implementation-flaw` — The scheduler ranks single action maps, while the
  declared policy carrier is a complete pattern cascade; no runtime decision
  constructs and scores two admissible cascades for one mission
  (`holes/labs/wm-contract/FUNDAMENTALS.edn:243-272`).  The declared grain already
  exists, so substituting actions for it is an implementation flaw.
- `:implementation-flaw` — Even where cascades are constructed, truncation can
  leave the enacted prefix carrying the full untruncated cascade's score, and
  the tactical lane lacks both a same-circumstance candidate set and `E(pi)`
  (`holes/missions/M-wm-aif-policy-grain-compliance.md:10-26`).  This violates
  identity/score agreement and candidate comparability already required by the
  declared policy-grain contract.
- `:undetermined` — The corpus contains 234 records with a cascade candidate but
  zero records offering two distinct cascades for one target, while its live
  Dirichlet counts selections rather than outcomes
  (`holes/labs/wm-contract/runs/F1-machine-q/02-policy-family-census.edn:23-64`).
  The census explicitly leaves Joe the ruling whether to adopt action grain or
  first define an evidence event (`.../02-policy-family-census.edn:83-91`), so
  discovery cannot classify this as extension or faulty implementation yet.
- `:aif-extension-needed` — The paper's operator comparison finds no scheduled
  mechanism that lets tension generate a missing option rather than merely rank
  the current menu (`p4ng/sec-operator.tex:317-324`).  This is a missing
  candidate-generation capability in the present AIF control map, upstream of
  the already implemented selector.

**Would we know? — missing at policy grain.** `clojure -M:test -n
futon2.aif.action-proposer-test` and `clojure -M:test -n
futon2.aif.policy-test` exercise proposal, softmax, temperature, and abstention
(the existing operational claim is `docs/futon-aif-completeness.md:147-165`).
The missing check is a recorded same-mission menu with at least two complete,
distinct, foldable cascades, candidate-local scores, and a refusal on zero or
one candidate; the nearest concrete acceptance artifact is
`holes/missions/M-wm-aif-policy-grain-compliance.md:91-123`.  Absence search:
`rg -n "two.*cascade|same-mission|no-policy-choice|candidate-local|prefix-local" src test scripts checks`.

## Scope limits

I read the four commissioned source families for all three nodes, the R-number
concordance, and only the named residual sources needed above: the two missions,
`FUNDAMENTALS.edn`, the F1 policy-family census, `P-validated-R5.md`, and the
R6 red-ring passage in `sec-operator.tex`.  I did not read every historical WM
trace, every mission linked from those documents, the Lean witness modules, or
all implementations behind cited summaries.  I did not execute the War Machine
or its tests: the `would-we-know` commands identify current checks and missing
conformance checks; they are not claims that those checks passed today.
