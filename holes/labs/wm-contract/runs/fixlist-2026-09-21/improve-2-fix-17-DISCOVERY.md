# improve-2 + fix-17 — which outcomes, and what preference odds?

2026-09-21, codex-12. Discovery only, branch `fix/narrative-improve-2`,
futon2 base `6857774c`. No production edits, clicks, serving-JVM evaluation,
store writes, or new preference ruling.

**Finding:** live C is already normalized over outcomes. Its weak discrimination
comes from the *utility law before that normalization*: a one-nat budget is
allocated across the entire source inventory, only a small fraction projects
into this menu, and some additional wants receive a different fallback weight.
Changing the outcome normalizer alone does not repair the spread. Renormalizing
utility weights changes preferences; several plausible domains give materially
different answers, including reversing AIF/F11. A fitted disposition model
cannot supply the missing preference strength: it supplies predicted
consequences, which are a different input.

Read the fix-6 discovery and R5/C rows of
[MAP-rnode-to-lean](../../MAP-rnode-to-lean-2026-09-21.md) alongside this note.
[The probe](improve_2_normalization_probe.clj) and its
[fresh results](improve-2-normalization-results.edn) reproduce all 27 Gs in the
two requested records, within 1e-8, using the actual sparse model. Source hashes
of both records are retained in the results. Other source revisions inspected:
mathlib4 `77fdbda5b5629b3c8f6c7f9bbb027da0436ba1b3`, futon3 library
`7fc6a05004aa3e601e53eba633d62213da0af4b2`, p4ng
`d493134b0daa6b72815b364074ebb849cde6fc14`. The library census additionally
pins an inventory digest of the files actually read. No Lake build is claimed.

## 1. What preference statement does live C make?

For these records, a terminal observation is a **set of target-qualified
established tokens**, not a selected mission, candidate, pattern id, or flight
disposition. With no ruled-zero outcomes in these two models:

```
u(S) = sum(w_v for v in S)
C(S) = exp(u(S)) / Z
C(X) / C(Y) = exp(u(X) - u(Y))
```

Thus the machine's current preference can be read as follows. These are the
preferences its implemented model *asserts*, not independently recovered
verbatim statements of Joe's desired odds:

| At the terminal step, all other tokens held fixed | Preferred-outcome odds: present versus absent |
|---|---:|
| Updater completion claim, `[M-aif-policy-conditioned-eig :hole/h6378c65a4012]` | exp(229/175347) = **1.001306835 : 1** |
| F11 successor claim, `[M-f11-find-production-successor :hole/h9ab212b3281d]` | exp(5/116898) = **1.000042773 : 1** |
| External-F2 `:route-a-rehearsal-reported` | exp(1/6) = **1.181360413 : 1** |
| Either earlier EOI candidate's two new wanted tokens, versus neither | exp(61/19483) = **1.003135841 : 1** |

The F2 token is already present initially, so its much stronger odds do not
favor its candidate in this run. EOI C1 and C2 establish different pairs of
equal-weight tokens and tie in G. At step 1 C is uniform: none of these token
preferences discriminates then. Both runs declare T=2 with terminal-only
weighting. Identity A and point-mass predictions make risk differences exactly
negative utility differences; ambiguity is zero. Preference odds are not
posterior policy odds: E, F and policy precision must also be included there.

The updater token observes a **checked completion declaration**, not the
actual existence and correctness of the shared posterior updater. The source's
`:observation-limit` says this explicitly. Fix-10's reference build is a concrete
counterexample to identifying grounded change with that wanted completion.
This semantic limit matters before assigning stronger odds.

### Where the global budget came from

This is an explicit implementation/design choice, not an accidental missing
partition function. [live_c.clj:31–37](/home/joe/code/futon2/src/futon2/aif/live_c.clj:31)
says weighted tokens sum to lam to match the uniform coverage law's total.
`normalise-weights` at line 169 and `derive-live-c` at line 236 implement
`raw_weight / sum(all deduplicated entry weights)` at initial lam=1.
Commit `b707c561` explicitly says “Weights are normalised to sum = lam”; its
[test](/home/joe/code/futon2/test/futon2/aif/live_c_test.clj:30) pins that law.
The named sources are alive-mission L=T×H, open-mission closure weight 1,
and unsatisfied-capability weight 1. Duplicate source tokens retain their
strongest declaration. This is normalization over **468 weighted entries**, not
uniform division by 468.

`project-want` (line 277) divides a mission source's mass among that mission's
wanted tokens and adds contributions from multiple sources. Filtering retains
these masses without restoring a unit budget. In the latest record, alive AIF
and closed AIF together supply 229/58449, shared among three wants; closed F11
supplies 5/58449, shared among two. Unprojected sources still affected the
original denominator. Capability stars have no mission-token projection.

The approved-model provenance is narrower than “Joe explicitly chose global
normalization over the current inventory.”
[P6's design](/home/joe/code/p4ng/wm-walkthroughs/build-loop/closure/PROPOSAL-wm-model-design-2026-09-16.md:63)
proposes a named default of **one nat per unit coverage**, explicitly not
empirically derived. Lean TokenPreference cites P6 as approved. The first-cut
live-C commit explains the extension to all source entries. I found no separate
operator statement there specifying how an unrelated new source should dilute
an existing mission's odds. Treat that dilution as the implemented law whose
scope needs confirmation, not as something required by KL or inferred merely
from Git author attribution.

There is a second law at
[cascade_model_manifest.clj:577](/home/joe/code/futon2/src/futon2/aif/cascade_model_manifest.clj:577):
named `:weights` replace lam/|want|; unnamed wants retain that uniform share.
Consequently the *consumed* weights do not sum to one either. The latest
fallback is F2 at 1/6; the earlier record has seven fallbacks at 1/120 (six
M-canon-fingerprint-store wants and F2). These must be included in any scale
proposal, not omitted by looking only at `:weights-echo`.

## 2. Which normalization does Lean require, and does changing it help?

[Holes.PreferenceDistribution](/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:7106)
is `ProbabilityKernel Unit (Outcome Obs)`.
[OutcomeRiskKL.outcomeRisk](/home/joe/code/mathlib4/DarkTower/WarMachine/OutcomeRiskKL.lean:33)
uses Q and C on that same carrier; positive Q at zero C gives infinity.
[TokenPreference](/home/joe/code/mathlib4/DarkTower/WarMachine/TokenPreference.lean:44)
normalizes exp(utility) across all non-ruled-zero `Finset V` outcomes.
[PolicyHorizon.horizonEFE](/home/joe/code/mathlib4/DarkTower/WarMachine/PolicyHorizon.lean:61)
sums step risks and ambiguities on one common model/carrier and declared Cτ.

Therefore: **normalize C over the declared observation-outcome space shared
by this comparison, at each τ.** In this implementation that is the powerset
of the tick's common token universe: 2^11 or 2^123 possible states. Lean does
not require a utility budget per target, per menu, or per global corpus. Its
uniform coverage constructor's division by |want| is a chosen utility function,
not a theorem that arbitrary weighted utilities must sum to one. Nor is
normalization over policies Π the same operation: that belongs to selection's
softmax, not C. Separately normalizing each candidate's own reachable outcomes
would give different preference objectives to the compared policies.

For these sparse, zero-exclusion-free models,
`log Z = sum_v log(1+exp(w_v))`. It cancels in pairwise terminal risk differences.
The probe conditions terminal C on the **shared set of distinct terminal
states reached by the whole menu** (three in each run), retaining their utility.
This changes Z and absolute KL, but leaves both spreads unchanged. It is a
counterfactual support restriction, not a claim that those are the only possible
outcomes. A proper marginal discarding fixed token coordinates also preserves
the spread here; this conclusion uses the records' additive factorized C and
deterministic Q, not an arbitrary coupled model.

| Quantity / counterfactual | 1789964661 | 1789952479 |
|---|---:|---:|
| Candidates / token universe / weighted wants | 3 / 11 / 6 | 24 / 123 / 120 |
| Projected source tokens / projected outcome tokens | 3 / 5 | 26 / 113 |
| Retained live utility total | 78/19483 = .0040034902 | 3143/58449 = .0537733751 |
| All consumed utility total, including fallback | 19951/116898 = .1706701569 | 87367/779320 = .1121067084 |
| **Recorded full G spread (nats)** | **.0013059819** | **.0031309347** |
| Only terminal outcome-space renormalization | .0013059819 | .0031309347 |
| Marginalize fixed coordinates (2 / 3 varying tokens remain) | .0013059819 | .0031309347 |
| Rescale *all consumed weights* to sum one | .0076520809 | .0279281651 |
| Rescale projected live weights to sum one; keep fallback fixed | .3262108262 | .0582246262 |
| Rescale each target's consumed weights to sum one | .5 | .6666666667 |

The final three rows recompute normalized C and G through the actual model.
They are **different utility laws**, not alternate computations of the same KL.
All-weight renormalization is equivalent to k=5.8593 or 8.9201 in the respective
records. Live-only renormalization uses k=249.7821 or 18.5966 on just one part
of C. Per-target normalization makes an F11 token worth 1/2 and an AIF token
1/3: F11 then beats AIF, reversing their current order. It erases their original
cross-mission priority ratio. EOI C1/C2 still tie under every listed change.

So fix-17 is partly a **scope/budget question**, not a defective KL normalizer.
The present projection shrinks represented live utility by factors of about
250 and 19. But deciding that the current menu should inherit the entire
budget is itself a preference decision and makes values depend on menu
membership. Even that change leaves the earlier spread at only .0582 nats,
far below a 5:1 habit log-ratio ln(5), and cannot separate EOI's equal utility.
A family scope does not eliminate the need to justify scale and tradeoffs.

## 3. What would the disposition bridge require?

[DispositionKernel](/home/joe/code/mathlib4/DarkTower/WarMachine/PreferenceLadderDraft.lean:49)
is a normalized conditional with explicit zero support. Its bridge is
`Q(d|π) = sum_o P(d|o) Q(o|π)`. The same file's PreferenceFamily supplies a
common tagged observation/terminal carrier. It supplies neither training data
nor a fitted instance for today's target-qualified token states.

What already exists is more than “no kernel,” but less than the required one:

- [machineC](/home/joe/code/mathlib4/DarkTower/WarMachine/MachinePreferenceDistribution.lean:29)
  is an unconditional distribution over twelve organization dispositions in
  SeedObs. The runtime seed prefers grounded-change mass 1/2 to four kinds at
  1/8 each: odds 4:1, ln(4) contrast. Seven other kinds have named zero mass.
  Nouns/verbs are empty; evidence is unruled. These are not 12 token states.
- [checks/disposition_kernel.clj:20](/home/joe/code/futon2/checks/disposition_kernel.clj:20)
  fits a checkpoint-trajectory conditional because its ledger contains no
  channel observation vector. The pinned RUN4 artifact has three samples,
  all grounded-change. `constant-checkpoint-kernel` explicitly ignores predicted
  observations: fresh evaluation returns ln(2)=.6931471806 for every input.
- That adapter enters the **channel** EFE path at efe.clj:710, taking predicted
  means, not marginalizing a token-outcome distribution. The all-cascade branch
  of `rank-actions` at line 1378 dispatches to a different scorer. Enabling the
  old C-fold flag is not a token→disposition bridge for these two runs.
- Freshly refitting the old ledger on this main produces **14** support members,
  because the fitter uses current `cohort/outcome-kinds`. The ruled seed correctly
  excludes the two administrative historical-verification outcomes and remains
  12-wide. Feeding that refit into the adapter refuses
  `:outcome-outside-disposition-support`. The pinned 12-wide artifact still
  computes ln(2). Both results and the unchanged ledger digest are in the probe.
  No support entry was dropped or historical artifact rewritten to make it fit.
- Fix-10a now retains model predictions and post-build comparisons;
  [D-task v2](/home/joe/code/futon2/src/futon2/aif/d_predecessor_task_authority.clj:372)
  retains signed C3/C4 observations with identity, meaning and missingness.
  These provide ingredients for aligned examples. They do not themselves
  constitute a fitted conditional or prove that checkbox claims mean work done.

A useful fit needs versioned tuples tying together (i) a declared observation
grain and token meanings, (ii) predicted Q with horizon/model revision,
(iii) independently measured outcomes at that same grain and time, including
unknown/refused readings, and (iv) an independently adjudicated disposition,
with occurrence, artifact and evidence identity. Historical pinned claims must
stay distinct from current artifact observations. Unsuccessful and early-stopped
attempts must be counted: training only on completed, measured builds would
select away dispatch/agent/substrate failures. Context or lifecycle stage may
be needed; assuming `d` is independent of policy given the chosen `o` requires
support, not just naming a function P(d|o).

There is no evidence here supporting an unconstrained table with 2^123 rows.
A smaller feature/factorization and its scope must be declared, trained from
aligned observations, and tested on held-out whole attempts. Unseen rows and
unmeasured labels remain unknown/refused, not asserted zero probability.
Predictive calibration/likelihood tests evaluate P; operator preference evidence
evaluates C. Neither retrieval rank nor frequency of past choices supplies both.
The earlier single-support fit is an important negative control: normalization
and a finite KL alone do not demonstrate discrimination.

### Replace token risk or sit above it?

The older C591 proposal calls disposition risk an added layer of G. P5/P6's
later design calls for intermediate token observations and a terminal seed
**tagged with coverage**. These are evidence for an intended relationship,
not a license to splice the current channel adapter into cascade G. Today's
machineC has no coverage coordinate; today's recorded terminal C has no
disposition coordinate. Their empirical and type-level discrepancy remains.

Replacing terminal token risk by disposition-only KL would make any two
outcomes with the same P(d|o) indistinguishable, even when they discharge
different wants. The reference grounded-change with updater still absent makes
that loss concrete. Fix-13's counts could still be retained, but they would
then describe available tokens, **not which mission preferences affected risk**.

Keeping token assessment at its own rung, with a later disposition rung, best
preserves that distinction. To match the stated P6 terminal design, declare a
joint/tagged terminal outcome carrying disposition **and** coverage, and its
preference law, rather than silently erasing one. Alternatively declare two
separate objectives and their composition. `KL(Q_o||C_o)+KL(Q_d||C_d)` is not
automatically KL on a common joint outcome model; the marginals are connected
through P and can double-count the same evidence. A joint model needs an
explicit joint C; a hierarchy needs explicit domains, schedules and tradeoffs.
No choice is made operational in this discovery.

Under either future choice, preserve fix-13's source-entry, source-token,
projected-token and comparison-domain counts *before* any renormalization.
Add a separate kernel-domain account: observed examples, missing measurements,
covered contexts/rows, disposition support and calibration coverage. The 5/113
figures are projected outcome-token counts, not 5/113 fitted kernel rows;
468 remains source entries. Replacing these denominators by “100% of the
selected menu” would hide the very omission fix-13 is intended to expose.

## 4. What evidence could justify a scale, and what must Joe state?

There are already distinct kinds of declaration, with distinct authority:

| Source | What it licenses | What it does not license |
|---|---|---|
| P6 / declared lam=1 | A named coverage-scale starting point, explicitly not learned | Global-corpus dilution as an empirical value judgment |
| Live source L, mission closure, capability stars | Desired directions and implemented relative source weights | Treating L or a count as nats without a stated conversion |
| Ruled disposition seed / Item 6 fold ruling | Current disposition masses and named zeros, on that carrier | Transplanting its 4:1 odds to arbitrary checkbox pairs |
| Pattern purpose, context, mechanism, violations, warrants | Candidate criteria, scope, explanatory ordering and proposed outcomes | Numerical operator preference or empirical success probability without evidence |
| Aligned execution records | Evidence for model likelihoods and measurement limits | Equating frequently chosen/completed work with desired preference strength |

The seed's original D1 worked example explicitly said “illustration ... not a
ruling”; `ruled_outcome_c.clj:5` records the subsequent Item 6 authority for its
live fold. Preserve that chain rather than treating an illustrative number or
a “ruling” label alone as fresh operator calibration.

The library **does say what patterns are for**. The real selected
[one-authority-per-question](/home/joe/code/futon3/library/apparatus/one-authority-per-question.flexiarg:1)
contains @why incidents, @how mechanism, a violation signature, IF/HOWEVER/THEN,
and an observable ownership criterion. Its target-specific admission receipt
then supplies the updater reading and scope. It supplies no odds that updater
completion is preferable to F11 completion. The workshop
[progress-is-a-witnessed-state-change](/home/joe/code/futon3/library/workshop/progress-is-a-witnessed-state-change.flexiarg:1)
is a useful criterion source, but its own review remains pending: normative
language is not automatic numerical authority.

The reproducible census covered all 1,404 `.flexiarg` files under futon3/library:
932 have @why, 647 @how, 71 @violation-signature. There are no headers named
@weight, @utility, @preference, @odds, @probability, @cost, @benefit, @priority
or @score. This is a bounded metadata finding, not a claim that no number or
preference occurs anywhere in their prose. Six @confidence values are “high”;
ten @epistemic-value fields describe mechanisms, not measured nats; eight
@energy fields name martial techniques; 256 @hamming-weight fields describe
encodings. Five ant patterns **do** carry numeric @aif-delta parameters, e.g.
[hunger-precision-coupling](/home/joe/code/futon3/library/ants/hunger-precision-coupling.flexiarg:9)
has survival lambda 1.4 and need-gain .7. They are scoped ant controller/golden
parameters, not a warranted conversion to these missions' outcome odds.
`pattern_registry.clj:195`'s retrieval score/rank is search evidence, not C.

The existing [preference-module instantiate boundary](/home/joe/code/futon2/src/futon2/aif/preference_module.clj:57)
already requires an explicit exact p and authority for a symbolic binary
preference. [DERIVE/ARGUE](/home/joe/code/futon2/holes/labs/wm-contract/DERIVE-ARGUE-C-realization-2026-09-09.md:49)
specifies source, criterion, domain, version and review, and says evidence
review does not confer Joe's authority over strength. Reuse those distinctions.

What Joe needs to settle, with proposed criteria and examples prepared first:

1. **What is valued:** an attested work result, a completion claim, a flight
   disposition, or an explicitly structured combination; with scope, horizon,
   and acceptable positive/negative witnesses. Pattern text can propose the
   criterion, but cannot decide its applicability or completion by similarity.
2. **Which budget scope:** should an existing task's odds remain unchanged when
   an unrelated source/menu member is added? Is one unit full coverage of a
   target, a comparison family, or the whole portfolio? Per-target normalization
   equalizes full targets and changes current relative priorities.
3. **Strength/tradeoffs:** for specified outcomes X/Y differing only in the
   named criterion, declare odds r (or an interval/partial order if exact odds
   are unwarranted). Under the fixed additive law, k = log(r)/(u(X)-u(Y)).
   Multiple comparisons test whether one k fits; inconsistent comparisons call
   for a different preference law, not an averaged knob. Equal-utility EOI
   outcomes cannot be distinguished by any k: Joe would need a criterion that
   differentiates those outcomes if their difference matters.
4. **Composition and zeros:** whether disposition preference replaces a rung,
   participates in a joint outcome, or is a separate objective; which zero
   preferences remain hard exclusions. Neither an unknown observation nor an
   unseen training label authorizes a new zero or epsilon smoothing.

## Proposed slices, one behavior each

1. **Record-only preference audit** (~150–250 lines plus tests; scoring input
   receipts/narrative): retain outcome-domain id, source-budget domain, utility
   totals before/after projection, fallback origins, schedule, full C normalizer
   and a small set of named outcome odds. Keep G, E and selection unchanged.
   First acceptance: replay the two frozen records and assert the exact totals
   and spreads above, including F2's fallback; candidate scores and posterior
   remain identical. A planted omitted fallback or conflated 468/5 denominator
   must fail. Report held/missing when a declared odds comparison lacks meaning.
2. **Source-attributed preference proposal** (~200–300 lines; existing preference
   module/registry plus fixtures): turn one real mission/pattern criterion into
   a reviewed candidate entry with symbolic strength. No parser invents odds.
   Only an explicit accepted operator parameter instantiates it. Tests distinguish
   a purpose citation from a strength citation and reject missing authority.
3. **Record-only aligned kernel examples** (~250–400 lines; token-outcome/D-task
   evidence adapters and tests): one occurrence binds prediction, signed observed
   state, missingness and disposition. Test the reference updater-negative /
   grounded-change pair and failure attempts without complete observations;
   refuse wrong occurrence, meaning or artifact. Fix the 14/12 vocabulary join
   through a versioned declaration, never by silently dropping support.
4. **Offline fitted conditional** (size depends on the declared factorization;
   likely 300–500 lines plus data validation): emit a pinned model with explicit
   support and unseen-context refusal. Acceptance includes distinct independently
   labelled outcomes, normalization, held-out likelihood/calibration, named-zero
   handling and the constant-ledger negative control. Current three samples
   cannot pass a claim of useful token-conditioned discrimination.
5. **Declared-switch default-OFF scoring experiment**, only after domain,
   composition and strength are accepted: recompute C on the declared carrier,
   preserve fix-13 accounts, record before/after odds and full kernel/parameter
   provenance. Do not pick k by observing which value makes a desired policy win.

Slice 1 can proceed without settling the later model choice. This discovery
implements none of these slices.

## Reproduction and validation

From the worktree root (Clojure 1.11.1 / repository deps):

```sh
clojure -M holes/labs/wm-contract/runs/fixlist-2026-09-21/improve_2_normalization_probe.clj > holes/labs/wm-contract/runs/fixlist-2026-09-21/improve-2-normalization-results.edn
clj-kondo --lint holes/labs/wm-contract/runs/fixlist-2026-09-21/improve_2_normalization_probe.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- holes/labs/wm-contract/runs/fixlist-2026-09-21/improve_2_normalization_probe.clj
```

Fresh replay exit 0; its assertions reproduce all 27 scores and show unchanged
spread under common outcome normalization/removal of fixed coordinates. Kondo:
0 errors / 0 warnings. Parens: `OK`. The registry does not warrant standalone
assertion scripts (namespace test/Lean-build commands and nonzero parsed test
counts are required); no test namespace or production code changed, and no
failing-on-main regression claim is made for commissioned discovery. The retained
inputs, script, numeric results and source locators are the review evidence.
