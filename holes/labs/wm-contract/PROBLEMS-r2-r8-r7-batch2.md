# Per-node problem dossiers — batch 2: R2, R8, R7

**Scope and numbering.** This is discovery, not a change to the War Machine.
`R2`, `R8`, and `R7` below always mean the numbering in
`p4ng/empirics-futon/control-stages.edn`.  The contract and catalogue align on
R2 and R7, while R8 is only partial: contract R8 is the per-tick trace and
catalogue R8 is the present-fit mismatch scalar
(`p4ng/R-concordance.md:20-29`).  The R8 dossier therefore keeps the diagnostic
quantity and the record which surfaces it distinct.

## R2 — Structured observation

### The problem it solves

R2 solves how heterogeneous evidence becomes comparable input to one control
loop: the roster places **“Structured observation”** in PERCEIVE
(`p4ng/empirics-futon/control-stages.edn:15`; control-stage numbering).  The
operational criterion requires **“a fixed, normalised observation shape”** with
stable channel meanings and downstream machinery keyed to the schema
(`docs/futon-aif-completeness.md:72-78`; contract R2).  The catalogue identifies
the alternative as repeated prose interpretation and requires a **“typed,
normalized feature map derived from session state”**
(`p4ng/sec-catalog.tex:196`; catalogue R2).  The equation registry states the
boundary as `o_t <- structured observation of the world after action u_{t-1}`
(`holes/labs/wm-contract/aif-equations.edn:74-76`; control-stage R2).  Thus R2
solves both representation and interface stability: it translates unlike
artifacts into named bounded channels without letting the meaning or ordering
of those channels drift between ticks.

### Residual problems it does not solve

- `:implementation-flaw` — Observation `o` is runtime-inhabited but has no Lean
  carrier or statement: the generated join reports **“observe / R2 / o / not in
  Lean”** (`p4ng/sec-lean-state-generated.tex:11-14`), while runtime `observe`
  constructs the normalized channel map (`src/futon2/aif/observation.clj:103-146`).
  The control quantity exists, but the formalisation leg cannot state that the
  running value conforms to it.
- `:both` — The operator's turns are persisted but never read into the
  observation vector; the paper records R2's war-room constraint as failing and
  connects this to the operator being absent from both observation and
  preferences (`p4ng/sec-operator.tex:67-90`).  The observation model must say
  what operator evidence means and the runtime must ingest it, so both model
  extension and implementation are required.

**Would we know? — missing at the cross-language conformance bar.** `clojure
-M:test -n futon2.aif.observation-test` checks channel identity, normalization,
ordering, provenance, and vector-envelope refusal (the scoped operational check
is recorded at `docs/futon-aif-completeness.md:72-78`).  Missing is a Lean
observation carrier plus a mirror check that the runtime channel set, bounds,
and absence variants inhabit it; the nearest formal gap is
`p4ng/sec-lean-state-generated.tex:13`.  Absence search: `rg -n
"observation-channels|StructuredObservation|defines :o|Observation.*Channel"
/home/joe/code/mathlib4/DarkTower/WarMachine /home/joe/code/futon2/src
/home/joe/code/futon2/test`.

## R8 — Present-fit mismatch

### The problem it solves

R8 solves how the loop exposes whether its present belief explains the evidence:
the roster places **“Present-fit mismatch”** in PERCEIVE
(`p4ng/empirics-futon/control-stages.edn:16`; control-stage numbering).  The
catalogue requires **“a single number, comparable across time”**, computed only
from belief, observation, and precision, because action scores concern imagined
futures rather than present fit (`p4ng/sec-catalog.tex:202`; catalogue R8).
The operational contract locates that quantity in a reconstructable per-tick
record containing pre-belief, observation, errors, post-belief, candidates,
score terms, choice, temperature, and F
(`docs/futon-aif-completeness.md:226-239`; contract R8).  The registry separates
the computations: `eps_k := o_k - mu_k`, and
`F = 1/2 * mean_k (Pi_k eps_k^2)`
(`holes/labs/wm-contract/aif-equations.edn:77-94`; control-stage R8).  R8's
problem is therefore to compute present mismatch independently of prospective
policy value and surface enough provenance every tick for an observer to
reconstruct it.

### Residual problems it does not solve

- `:implementation-flaw` — Prediction error `eps` runs in Clojure but is absent
  from Lean: the generated table marks the R8 row **“not in Lean / missing”**
  (`p4ng/sec-lean-state-generated.tex:13-16`), while
  `compute-prediction-error` emits observed, predicted, variance, error,
  precision, and weighted error (`src/futon2/aif/free_energy.clj:203-218`).  A
  running quantity with no corresponding formal statement cannot support a
  cross-language conformance decision.
- `:implementation-flaw` — Policy free energy `F-pi` is likewise runtime-inhabited
  but unformalized (`p4ng/sec-lean-state-generated.tex:16-18`):
  `f-pi-for-candidate` computes the horizon-one Gaussian observed-data term and
  refuses channel and variance defects (`src/futon2/aif/policy_free_energy.clj:41-82`).
  The formula is declared and executable, so the missing formal carrier is an
  implementation defect on the proof side rather than a new AIF concept.
- `:implementation-flaw` — The paper records that R8's daily gain-reading
  cadence stopped on 2026-07-14 and the last outer-loop run was 2026-07-27
  (`p4ng/sec-operator.tex:82-86`).  A computed diagnostic that is not read on
  its declared cadence does not solve the catalogue's validation and alarm
  problem.

**Would we know? — missing at the joined diagnostic bar.** `clojure -M:test -n
futon2.aif.free-energy-test`, `clojure -M:test -n
futon2.aif.policy-free-energy-test`, and `clojure -M:test -n
futon2.aif.trace-test` exercise the runtime computations and trace surface; the
trace contract and its tests are named at
`docs/futon-aif-completeness.md:226-241`.  Missing is one check joining the
runtime `eps` and `F-pi` values to Lean statements and confirming the present-fit
scalar is surfaced every tick on a running cadence; the nearest inventory is
`p4ng/sec-lean-state-generated.tex:14-17`.  Absence search: `rg -n
"predictionError|policyFreeEnergy|F_pi|defines :eps|defines :F-pi"
/home/joe/code/mathlib4/DarkTower/WarMachine /home/joe/code/futon2/src
/home/joe/code/futon2/test`.

## R7 — Evidence-channel precision

### The problem it solves

R7 solves how the loop varies trust by evidence channel without discarding
noisy evidence: the roster places **“Evidence-channel precision”** in BELIEVE
(`p4ng/empirics-futon/control-stages.edn:20`; control-stage numbering).  The
operational criterion says channels with persistent high error lose precision
and those with persistent low error gain it, with the update carried across
ticks (`docs/futon-aif-completeness.md:175-190`; contract R7).  The catalogue
requires **“one trust weight per kind of evidence”** applied wherever error,
scoring, or gating occurs, so trust remains auditable rather than prompt rhetoric
(`p4ng/sec-catalog.tex:200`; catalogue R7).  The registry defines the update as
`Pi_k := 1 / max(Var(eps_k), eps0)` and records that variance is taken over the
bounded history of channel-k prediction errors
(`holes/labs/wm-contract/aif-equations.edn:81-84`; control-stage R7).  Here
capital `Pi` means evidence-channel precision (inverse error variance): it is
neither policy carrier `pi` nor policy precision `gamma = 1/beta`, the three-way
symbol hazard recorded at `holes/labs/wm-contract/EPIC-run-era.md:719-721`.

### Residual problems it does not solve

- `:implementation-flaw` — Evidence precision `Pi` is runtime-inhabited but has
  no Lean statement (`p4ng/sec-lean-state-generated.tex:13-16`), while
  `update-channel-precision` computes inverse regularized error variance and
  bounds it by the declared floor and cap
  (`src/futon2/aif/precision.clj:116-158`).  The runtime mechanism exists, but
  formal conformance cannot yet be expressed.
- `:implementation-flaw` — Adaptive precision is satisfied only for the four
  channels with likelihood models; the remaining ten inherit R7 only if their
  likelihoods land (`docs/futon-aif-completeness.md:181-194`).  Applying a trust
  registry to a subset does not solve per-channel precision for the full
  observation schema, although it does satisfy the documented scoped claim.
- `:aif-extension-needed` — The existing window counts raw calls, which the
  recorded forward design says dilutes signal during dormant periods; it calls
  for a bounded dual-clock window combining wall time and evidence events
  (`docs/futon-aif-completeness.md:196-224`).  This changes what temporal evidence
  the precision model represents, so it is an extension to the current AIF
  account rather than a missing line in the existing calculation.

**Would we know? — missing at full-schema/formal agreement.** `clojure -M:test
-n futon2.aif.precision-test` checks rolling history, bounds, adaptation,
untouched channels, weighted errors, and purity (the existing check is listed at
`docs/futon-aif-completeness.md:181-192`).  Missing is a test that every R2
channel has a likelihood-backed precision and that each runtime update agrees
with a Lean `Pi` statement; the nearest formal gap is
`p4ng/sec-lean-state-generated.tex:15`.  Absence search: `rg -n
"PrecisionMap|evidence.*precision|rolling.*variance|defines :Pi"
/home/joe/code/mathlib4/DarkTower/WarMachine /home/joe/code/futon2/src
/home/joe/code/futon2/test`.

## Scope limits

I read the accepted pilot, the four commissioned source families for all three
nodes, the concordance, the per-node operational sections, the Box-5 generated
join, the per-node-problems ruling, and only the runtime producers and named
paper passage needed to substantiate the residuals above.  I did not read every
historical trace, all observation-source adapters, every caller of the three
runtime quantities, or the Lean corpus beyond the generated join's pointers.
I did not execute the War Machine or its tests: the `would-we-know` commands
identify existing component checks and missing joined checks; they are not
claims that those checks passed today.
