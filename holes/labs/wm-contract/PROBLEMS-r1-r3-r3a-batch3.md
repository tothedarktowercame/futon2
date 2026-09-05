# Per-node problem dossiers — batch 3: R1, R3, R3a

**Scope and numbering.** This is discovery, not a change to the War Machine.
`R1`, `R3`, and `R3a` below use
`p4ng/empirics-futon/control-stages.edn` numbering.  Contract and catalogue R1
are aligned and R3 is verbatim (`p4ng/R-concordance.md:20-24`).  R3a is a
later mediator node absent from the concordance, catalogue, and standalone
contract criteria; its recorded placement basis is therefore used directly and
the missing fourth source is stated rather than synthesized.

## R1 — Belief state

### The problem it solves

R1 solves how the loop keeps revisable hypotheses rather than repeatedly
reasoning from raw history: the roster places **“Belief state”** in BELIEVE
(`p4ng/empirics-futon/control-stages.edn:17`; control-stage numbering).  The
operational criterion requires a hidden-state belief distribution **“carried
across ticks, with mean and precision (variance) both explicitly represented”**
(`docs/futon-aif-completeness.md:49-68`; contract R1).  The catalogue says a
conversation-shaped belief cannot be cleanly updated, compared, or falsified,
and instead calls for **“a map of operational hypotheses”** updated from
observations and outcomes (`p4ng/sec-catalog.tex:190-194`; catalogue R1).  The
equation registry states the temporal identity: `mu_t` is the stored belief
after the update at `t-1` (`holes/labs/wm-contract/aif-equations.edn:108-110`;
control-stage R1).  R1 therefore solves both compression and continuity: it
makes uncertainty explicit in a stable state which later observations can
change and later ticks can recover.

### Residual problems it does not solve

- `:implementation-flaw` — Belief state `mu` is runtime-inhabited but has no
  resolving Lean site in the equation join: it reports **“belief-state / R1 / mu / not in
  Lean”** (`p4ng/sec-lean-state-generated.tex:18-20`), while runtime constructs
  the initial posterior and deterministic updates
  (`src/futon2/aif/belief.clj:62,364-445`).  The running state exists, but the
  proof side cannot state agreement with it.
- `:both` — The runtime belief is a normalized distribution over seven entity
  lifecycle statuses, while the generative model needs a distribution over its
  `State`; no map joins those carriers, and the nearest belief-to-channel
  predictors have no caller (`holes/labs/wm-contract/FUNDAMENTALS.edn:112-141`).
  The state relation must be declared in the model and implemented in the
  machine, so this residual belongs to both legs.

**Would we know? — missing at generative-model agreement.** `clojure -M:test
-n futon2.aif.belief-test` checks posterior shape, deterministic update,
entropy, bootstrap, and carry/reconciliation; the scoped criterion and tests are
recorded at `docs/futon-aif-completeness.md:49-70`.  Missing is a Lean `mu`
carrier plus a checked map from the runtime status posterior to a state
distribution consumed by the generative model; the nearest falsifier is
`holes/labs/wm-contract/FUNDAMENTALS.edn:112-141`.  Absence search: `rg -n
"BeliefState|belief-state|defines :mu|ProbabilityKernel.*State"
/home/joe/code/mathlib4/DarkTower/WarMachine /home/joe/code/futon2/src
/home/joe/code/futon2/test`.

## R3 — Belief update

### The problem it solves

R3 solves how observations change belief without either overwriting it or
letting it become immovable: the roster places **“Belief update”** in BELIEVE
(`p4ng/empirics-futon/control-stages.edn:18`; control-stage numbering).  The
operational contract requires prediction error, precision weighting,
variational free energy, and an update step all to be computed by name
(`docs/futon-aif-completeness.md:80-106`; contract R3).  The catalogue prescribes
moving belief toward observation only as far as learning rate and channel trust
allow, while **“never letting that uncertainty fall below a floor”**
(`p4ng/sec-catalog.tex:198`; catalogue R3).  The registry writes the reduction
as `mu <- mu + alpha Pi eps`, importing prior belief, precision, error, and step
size (`holes/labs/wm-contract/aif-equations.edn:104-107`; control-stage R3).
R3 therefore solves controlled revision: the direction comes from error, the
amount from learning rate and trust, and retained uncertainty prevents false
certainty.

### Residual problems it does not solve

- `:implementation-flaw` — Updated belief `mu-next` is runtime-inhabited but
  absent from Lean: the generated join marks **“belief-update / R3 / mu-next /
  not in Lean / missing”** (`p4ng/sec-lean-state-generated.tex:17-19`), while
  `update-entity-belief`, `update-belief`, and `update-belief-batch` implement the
  runtime transition (`src/futon2/aif/belief.clj:364-445`).  A running update
  with no formal counterpart cannot establish cross-language conformance.
- `:implementation-flaw` — The runtime transition model used by belief update is
  action-independent, and its action-conditioned neighbour is only an
  observation-delta predictor with no state input; no machine `B(s'|s,u)` exists
  (`holes/labs/wm-contract/FUNDAMENTALS.edn:143-176`).  Controlled transition is
  already a named AIF object, so its missing machine inhabitant is an
  implementation failure.
- `:implementation-flaw` — The aggregate update is scoped to four of fourteen
  likelihood channels, and the synthesized belief event currently takes its
  sign and magnitude from annotation health alone because the channels lack a
  declared common health direction (`docs/futon-aif-completeness.md:86-106`).
  The update formula already accepts per-channel error and precision; the
  incomplete attribution and coverage are implementation gaps.

**Would we know? — missing at formal and controlled-transition agreement.**
`clojure -M:test -n futon2.aif.belief-test` and `clojure -M:test -n
futon2.aif.free-energy-test` check runtime posterior updates and prediction-error
records; the four-part operational check is
`docs/futon-aif-completeness.md:80-106`.  Missing is a test that a Lean
`mu-next` relation agrees with the runtime update for every admitted channel and
that the update's state prediction uses an action-conditioned `B`; the nearest
recorded falsifiers are `p4ng/sec-lean-state-generated.tex:18` and
`holes/labs/wm-contract/FUNDAMENTALS.edn:143-176`.  Absence search: `rg -n
"mu-next|beliefUpdate|controlled.*transition|TransitionKernel.*Action"
/home/joe/code/mathlib4/DarkTower/WarMachine /home/joe/code/futon2/src
/home/joe/code/futon2/test`.

## R3a — Prediction-error projection

### The problem it solves

R3a solves the join between an observation and the belief-derived prediction it
can contradict: the roster places **“Prediction-error projection”** in BELIEVE
and types it `:mediator` (`p4ng/empirics-futon/control-stages.edn:19`;
control-stage numbering).  Its placement basis says it **“reads the BELIEF and
not only the world”**, is recomputed inside the R3 micro-step as belief moves,
and has R7 precision update as its measured consumer
(`p4ng/empirics-futon/control-stages.edn:19`; control-stage R3a basis).  The R3
operational section names prediction error per channel as its first sub-property
and reports four likelihood-backed channels
(`docs/futon-aif-completeness.md:80-98`; contract R3 sub-property, not a
standalone R3a criterion).  The equation registry assigns `eps_k := o_k - mu_k`
to R8 rather than R3a (`holes/labs/wm-contract/aif-equations.edn:77-80`;
control-stage R8 equation ownership), so R3a is the runtime projection mediator
and not a second owner of `eps`.  There is no catalogue R3a paragraph or
standalone contract R3a row; this dossier does not infer one from neighbouring
patterns.

### Residual problems it does not solve

- `:implementation-flaw` — Prediction-error projection exists only for four of
  fourteen channels; the other ten are recorded as likelihood-model work, with
  some still awaiting a decision whether prediction is applicable by design
  (`docs/futon-aif-completeness.md:86-106`).  A partial projection cannot yet
  mediate the full R2 schema into R3/R7.
- `:undetermined` — The control-stage basis places the mediator in BELIEVE, but
  explicitly says it does not settle which box owns `eps`: the equation registry
  hosts it at R8 and the same runtime call site is tagged R8
  (`p4ng/empirics-futon/control-stages.edn:19`).  This is an unresolved ownership
  question, not evidence that either the AIF model or implementation is wrong.
- `:implementation-flaw` — The generated formal join marks the `eps` equation
  **“not in Lean / missing”** (`p4ng/sec-lean-state-generated.tex:13-15`), while
  runtime `channel-prediction-error` classifies and computes the projection
  (`src/futon2/aif/free_energy.clj:203-224,280`).  Regardless of eventual node
  ownership, the implemented mediator lacks a formal statement against which it
  can be checked.

**Would we know? — missing as a mediator contract.** `clojure -M:test -n
futon2.aif.free-energy-test` exercises runtime prediction-error and refusal
records, while the existing R3 sub-property check is named at
`docs/futon-aif-completeness.md:80-106`.  Missing is a standalone R3a contract
which fixes producer, consumer, channel coverage, and `eps` ownership, plus a
Lean/runtime agreement test; the nearest artifact is the placement basis at
`p4ng/empirics-futon/control-stages.edn:19`.  Absence search: `rg -n
"R3a|Prediction-error projection|prediction-error/v1|defines :eps"
/home/joe/code/p4ng /home/joe/code/futon2/docs /home/joe/code/futon2/src
/home/joe/code/futon2/test`.

## Scope limits

I read the accepted dossiers, the four commissioned source families for R1 and
R3, the R3a placement basis and neighbouring contract/equation rows, the
concordance, the Box-5 generated join, both commissioned FUNDAMENTALS entries,
and only the runtime producers needed to substantiate the formalisation
residuals.  I did not inspect every belief caller, historical trace, likelihood
adapter, Lean witness module, or the drawing source behind R3a's placement.
I did not execute the War Machine or its tests: the `would-we-know` commands
identify existing component checks and missing conformance checks; they are not
claims that those checks passed today.
