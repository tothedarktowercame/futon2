# M-a-wmc-scaling: Compiled Observation Queries and Rate Uncertainty for A

**Date:** 2026-09-20
**Status:** IDENTIFY (successor mission — activates after plop-2026 completion)
**Owner:** TBD (proposed by claude-12 at Joe's direction)
**Cross-ref:**
* `holes/labs/wm-contract/DESIGN-a-plain-2026-09-20.md` — the A this
  mission succeeds; WMC/variance-wmc are its parked appendix by ruling
* `holes/labs/wm-contract/PLAN-a-programme-2026-09-20.md` points 3, 6
* `p4ng/wm-walkthroughs/build-loop/closure/CLOSURE-DAG.md` rows
  PILOT-coupled-A-wmc, PILOT-variance-wmc-rate-uncertainty
* Lean: `DarkTower/WarMachine/{MixedTokenObservation,
  MixtureJointSeparationWitness, TwoProductEvaluation,
  GTotalMarginalInvariance, GNonPointMassDecomposition}.lean`
* github.com/nttcslab/variance-wmc (AAAI 2026; C++, BN -> ENC2 CNF ->
  SDD -> WMC); arxiv 2601.03523

## 1. IDENTIFY

### Why this exists: three limitations of the A now being built

The current A (design-of-record 67152883) is deliberately minimal:
judgement tokens in declarations, channel rates from counted-denominator
record searches, one binary common-cause coupling parameter, exact
computation at live scale. It is plausible and cheap BECAUSE live
decisions observe a handful of tokens. Three limitations are built into
that choice, and this mission is where each is addressed rather than
denied:

1. **Scale.** Exact joint computation over observation sets dies
   combinatorially (the "2^45 powerset" is the joint-query space over
   the full token vocabulary). Today no live decision performs such a
   query; the hole-retention work (441 retained holes across 86
   missions, futon2 3b5557aa) is the visible pressure that could grow
   per-decision universes past exact-enumeration comfort. WMC is the
   scaling implementation of the SAME common-cause model: compile the
   Bayesian network (ENC2, Sang-Beame-Kautz) to CNF, then SDD, and
   answer marginal and joint queries by weighted model counting,
   with compiled artifacts cached by model structure.
2. **Rate uncertainty.** The channel rates are declared point values
   from small counted populations (codex-intake 0.14 = 9/64,
   MEASUREMENT-dispatch-premise-rates d594140c). Nothing propagates
   estimation uncertainty into query probabilities, so a ranking could
   silently rest on a rate known only to one significant figure.
   variance-wmc propagates rate means AND variances through the same
   compiled queries, yielding error bars, and identifies which
   additional adjudications most reduce decision-relevant uncertainty
   — which feeds the calibration design (programme point 5) instead of
   competing with it.
3. **Dependence at spread beliefs.** `GNonPointMassDecomposition`
   proves dependence enters total G exactly through the mutual
   information between state and observation. For point-mass beliefs
   MI = 0 and today's exact machinery suffices; for spread beliefs
   with coupled kernels, computing MI is a joint-query workload —
   precisely what compilation serves.

### Acceptance (inherited, not invented)

- WMC-computed queries agree with the exact enumeration record on disk
  at n <= 10 (the DAG's stated acceptance), on JOINT events, never
  marginals alone — `MixtureJointSeparationWitness` is the proof that
  marginal agreement cannot certify (the witness pair matches all
  marginals and differs 2x on a joint; the recorded sidebyside shows
  469x). Total-G invariance at matched marginals
  (`GTotalMarginalInvariance`) and the MI-difference law are further
  free discriminators.
- variance-wmc means AND variances verified against small-model
  calculations; a shared estimated rate reused across tokens must not
  silently become several independent uncertain parameters (the
  paper's parameter-dependence assumptions, programme point 6).
  Query means/variances alone do not establish credible intervals or
  ranking uncertainty; those need their own propagation and validation.
- Compilation cost, memory, and repeated-query latency measured on
  actual candidate families before any production claim.

### Activation triggers (per the parked-appendix ruling)

Named trigger, not drift: (a) a live decision's judgement-token
universe grows past exact-enumeration comfort (order ~12 tokens /
measured latency), e.g. via hole-retention feeding declarations at
scale; or (b) a demonstrated case where rate uncertainty at current
sample sizes flips a ranking. Whoever hits either names it as the
trigger and this mission leaves IDENTIFY; nobody widens quietly.

### Scope out

Everything the plain A design covers today; the calibration collection
itself (point 5, its own thread); any change to checkable-channel
exactness (proven immune to coupling — that theorem is a boundary, not
a limitation).

### Relationship to plop-2026

Successor: it exists so the paper's A can be honest about its scale and
uncertainty limits and point HERE, rather than gesturing at compilers
mid-design. Nothing in it blocks or is blocked by the current A work.
