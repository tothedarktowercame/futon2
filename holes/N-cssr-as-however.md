# N-cssr-as-however — CSSR gives predicates, not measures, and that is the point

**Provenance:** Joe, 2026-07-16: "Do the CSSR ideas give us any other conditions
that could be used 'inline' to drive behaviour? They could provide interesting
HOWEVER clauses for example."

**Answer: yes, four are live today, and they dodge the theorem that parked the
tokamak — because none of them asks the undecidable question.**

## The distinction Joe's question turns on

`M-propagators` §2 banked **Cμ** — the causal-state *count* — as a failure:

> **Cμ (causal-state count)** — clears the Rule-110 bar on ECA, then fails on
> MetaCA: a *frozen barcode* scores **39–44** vs Rule 110's **26.4**. It reads the
> temporal stability of the local rule, not complexity, and it grows with sample
> size (38 over 160 gens, ~17 over 40-gen windows). The binning drives the
> verdict (k=2 vs k=4 flips it).

**That is a verdict on the SCALAR, not on the machinery.** Cμ is one number
squeezed out of a rich object and compared across alphabets where the comparison
is invalid. Joe's reframe — conditions, inline, as HOWEVERs — asks for
*predicates on the model* instead, and those are a different kind of thing.
A count can be gamed by sample size. "Has the model never seen this past?" cannot.

## Why they escape §4b's parking reason

§4b parked the tokamak on a **theorem**, not a gap:

> There is no effective test, and this is a theorem. Wolfram class membership is
> undecidable (Culik & Yu 1988); nilpotency is undecidable (Kari 1992). Cook
> proved Rule 110 universal by **construction**, never by measurement.

Every predicate below is a property of **the observer's own model**, not of the
system's class:

- "is this past unseen?" — a map lookup
- "did the model just split?" — a recorded decision
- "is this state's morph a point mass?" — arithmetic on a distribution

Undecidability bites *class membership*. It does not bite the model's own
bookkeeping. **CSSR conditions are usable inline precisely because they decline
to ask the undecidable question.** That is not a workaround; it is the difference
between asking "is this system class 4?" and asking "does my model of it still
fit?".

## The four that are LIVE (API: `futon5/src/futon5/mmca/local_causal_states.clj`)

| condition | source | reads |
|---|---|---|
| **:unseen** | `causal-state-field` — *"Invalid margins and unsupported/unseen pasts are **nil**"* | the field returns `nil` at a point |
| **:split** | `reconstruct-model` → `:decisions`, entries `{:past … :state … :action :split}` | the model split a state: a past that looked familiar had a different future |
| **:unresolved** | `reconstruct-model` → `:unresolved-pasts` | pasts the model could not place at all |
| **:settled** | `:future-probabilities` per state (`:166`) | the morph is a point mass ⇒ the future is deterministic |

`morph-comparison` (`:80`) — a chi-square homogeneity test returning a p-value
and `:equivalent?` at α — is the machinery underneath `:split`, so the confidence
is available too, not just the boolean.

**`:settled` is the one that matters most.** A causal state whose morph is a
point mass has a deterministic future — the machine has stopped surprising
itself. That is `settles_if_nu_has_fixed_point` **as an inline detector**: the
theorem says settling is death, and this reads settling off the data without an
EoC oracle. It is also exactly what the tokamak needed and did not have — §4b's
"the trap is only a trap if held" was discovered by *running into it*, and
`:settled` would have said so at the time.

## What this fixes in the cascade

Every HOWEVER in `cascade-tokamak-v0.edn` is **static** — an assertion about an
arm ("`:rotate+1` IS the trap"), true before the run starts and unchanged by it.
None of them reads anything. A CSSR HOWEVER is a **sensor**:

```
tkc-mid
  IF       13-24 distinct rules
  HOWEVER  :rotate+1 is the trap                       ; static — a note
  HOWEVER  the current causal state's morph is a       ; LIVE — a reading
           point mass: the field has already settled
  THEN     select :rotate+1 — and never twice running
```

The second HOWEVER can fire mid-run and stop the pattern. The first can only be
read by a human beforehand. **That is the difference between a HOWEVER that
documents a risk and one that detects it.**

And it supplies something egoLambda forced us to build separately. §3's sign
channel is `e : W(A) → Σ`, an emission that reads the whole context and stages
revision — the layer the refuted λ proved could not ride the writer. **CSSR is a
ready-made `e`**, with `Σ = {:unseen :split :settled :unresolved}`. Four signs,
each computed, none authored. That is a better Σ than `{bored, interesting}`
because nobody chose it.

## The fifth condition, and it is BLOCKED

`causal-state-mapping.md` (M-aif-tokamak slice 4.5, status DERIVE, "build
STOPPED at a genuine modeling question") maps EVALUATOR-SPEC §3.8–3.9's EoC
signature as a **conjunction**, not a scalar:

- **domainCoverage** — fraction covered by large homogeneous causal-state regions
- **particleSparsity** — sparse coherent boundaries between domains (gliders)
- **EoC = domainCoverage × particleSparsity**: complex → high AND nonzero;
  chaotic → low coverage; **frozen → high coverage but ZERO particles → low**

**That conjunction is built to reject the exact null that killed L3.** §4b:
diagonal transport "FAILS its null — the frozen barcode scored 0.1814, above
Rule 110's five-seed range". A frozen barcode sustains correlated activity, so a
transport statistic cannot tell it from computation. The conjunction can: high
coverage, zero particles, product low. The instrument that beat us is precisely
the case this one is designed to catch.

**But it is not implemented.** The note is explicit: the causal-state field
exists, "the domain/particle DECOMPOSITION … is **NOT yet implemented**".
So this is the one condition that would bear on EoC, and it is the one we cannot
run. The four above need none of it.

## Honest limits

- These predicates do **not** give an objective, and this note is not smuggling
  one in. `:settled` says the machine stopped surprising itself; it does not say
  that is bad. Reading "settled ⇒ death" requires
  `settles_if_nu_has_fixed_point`, which is **not at the emitted sha** (see
  `darktower-claims.edn`) — I am leaning on a theorem I could not audit.
- Cμ's sample-size sensitivity ("38 over 160 gens, ~17 over 40-gen windows")
  should be assumed to infect anything counting states. None of the four counts
  states. `:split` counts *events*, `:unseen` is a lookup — but a model trained
  on a short window will call more pasts unseen, so `:unseen`'s RATE is
  window-dependent even though the predicate is not. Use the predicate; do not
  build a scalar out of it and compare across windows. That is how Cμ died.
- CSSR reconstruction per window is expensive; slice 4.5 offers the cheap proxy
  (apply the trained `past->state` without re-clustering). The cheap proxy makes
  `:unseen` *more* frequent by construction — a fixed model sees more novelty
  than a refitted one. That is a feature for a surprise signal and a bug for a
  coverage statistic, and the two must not be mixed.

## Disposition

A **note**. The claim is narrow and I think it holds: CSSR's *predicates* survive
the objection that killed CSSR's *measure*, because they are about the model and
not about the class. First slice, if promoted: add `:settled` as a live HOWEVER
to `tkc-mid` and see whether it fires before the field dies — the tokamak's own
trap, detected rather than remembered.
