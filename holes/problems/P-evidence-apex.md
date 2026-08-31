# P-EVIDENCE-APEX — which evidence may bear on a noun or edge

**Status:** DRAFT for Joe's ruling; no contract below is ratified.  
**Delivery:** APEX-D1.  
**Holder:** `wm-evidence`.  
**Parent:** `M-formal-war-machine` §3 DERIVE; `delivery-lifecycle.md` §0.8, §0.9, §0.12.  
**Consumer:** Joe, for the first apex ruling; after ratification, the contract emitter and witness lint.

## The problem

The build records evidence but has no standard saying which evidence a particular noun or edge requires.
Badges, campaigns, censuses, receipts, and Lean proofs are available evidence; availability does not make any
of them the right evidence for a claim. R18 cannot supply the missing standard because it is one of the nouns
being graded and its badge audit was produced by the code's authors from the code.

APEX-D1 is operator work. Its acceptance is intentionally undefined because defining acceptance is the work.
Against §0.14 it scores **3/7**: named consumer, pinned/loud reads, and bounded/reversible draft are present;
typed uptake, prior acceptance, executable falsifier for the standard, and a fully decided decision surface are
not. This document therefore proposes a shape and exposes decisions. It does not gate R2 or R16→R2.

## Proposed shape

An `EvidenceContract` is a constraint flowing **down** from the big apex. Its `claim-clause` is the one typed
thing a recursing node apex sends **up**. Keeping both in one record makes the contact point inspectable; neither
direction alone is governance.

```clojure
{:evidence-contract/id qualified-keyword
 :status #{:draft :ratified :refused}
 :subject {:kind #{:noun :edge} :id keyword}

 ;; FLOW UP: supplied by the subject's apex/contract emitter.
 :claim-clause
 {:claim-id string-or-qualified-keyword
  :predicate theory-defined-proposition
  :evidence-kind keyword
  :producer-part declaration-reference}

 ;; FLOW DOWN: supplied by the big evidence apex.
 :requirement
 {:evidence-kind keyword
  :artefact-schema schema-reference
  :domain {:population description
           :minimum-diversity decidable-requirement}
  :corpus {:location path-or-endpoint
           :runner command-or-producer
           :basis immutable-revision-or-live-store-basis}
  :method {:command argv-vector
           :accepts decidable-predicate
           :falsifier decidable-predicate
           :negative-control fixture-reference}
  :independence
  {:declared-producing-part declaration-reference
   :producer producer-id
   :verdict #{:independent :self :unknown}}
  :consumer {:id node-agent-or-workflow-step
             :uptake edge-or-command}}

 :not-evidence [typed-facade ...]
 :open-decisions [decision-reference ...]}
```

Normative meanings proposed for ruling:

1. `:evidence-kind` is a type of run output, not “test”, “badge”, or prose approval. The upward kind must equal
   the downward required kind.
2. `:accepts`, `:falsifier`, and the negative control are required before status may become `:ratified`.
3. `:basis` is required and checked at read time. Missing corpus, basis, runner, producer declaration, or
   consumer is a typed refusal, never a default.
4. Independence uses `Holes.lean`'s existing three-valued definition: absent producing-part declaration gives
   `:unknown`; producer inside that declared part gives `:self`; producer outside gives `:independent`. The part
   is declared, not inferred. Only `:independent` satisfies a requirement that explicitly asks for independence.
5. `:self` evidence may still bear on a construction/conformance claim when the contract allows it. It cannot be
   relabelled independent. `:unknown` is a verdict and must propagate to the consumer.
6. A contract with an unresolved operator decision has status `:draft` or `:refused`; it cannot be made green by
   substituting the evidence currently available.

## Worked noun — R2 observation-channel contract

```clojure
{:evidence-contract/id :wm/R2-observation-channel
 :status :draft
 :subject {:kind :noun :id :R2}
 :claim-clause
 {:claim-id "r2ContractCensusWmTrace"
  :predicate "for every WM trace tick, observation keys equal the 14 named Channel constructors; the recorded corpus has exactly two ill-formed ticks"
  :evidence-kind :trace-corpus-census
  :producer-part "P-R2 deliveries plus the R2 trace generator/checker"}
 :requirement
 {:evidence-kind :trace-corpus-census
  :artefact-schema "R2Tick Channel Unit plus a census result"
  :domain {:population "every top-level form in the pinned WM trace corpus"
           :minimum-diversity "at least one conforming form and the two known 13-key refusing forms"}
  :corpus {:location "futon2/data/wm-trace/"
           :runner "the committed R2 generator using a reader loop over all top-level forms"
           :basis "fresh content pin from the same run; the registered c9add16ac96c973b… pin is historical, not current"}
  :method {:command ["bb" "checks/r2_channel_contract.clj"]
           :accepts "ordered channel identity is the declared Channel set; corpus census returns exactly 2"
           :falsifier "an undeclared fifteenth key is accepted, either known 13-key form is accepted, or any additional corpus form is ill-formed"
           :negative-control "the five-record undeclared-fifteenth fixture and the two known 13-key records"}
  :independence {:declared-producing-part "P-R2 deliveries plus the R2 trace generator/checker"
                 :producer "the same R2 generator/checker"
                 :verdict :self}
  :consumer {:id :checks/contract-lint
             :uptake "checks/witness-registry.edn binding for r2ContractCensusWmTrace"}}
 :not-evidence [:docstring-channel-count
                :fin-14-without-channel-identities
                :single-form-edn-read
                :turn-storage-count
                :badge-sentence]
 :open-decisions
 ["Does this conformance claim require independent evidence, or is self evidence admissible because the independently checkable negative control prevents satisfaction by construction?"
  "Ratify the exact command/schema references; the names above describe existing artefacts but APEX-D1 did not certify their current paths."]}
```

This contract is deliberately narrow. It bears on R2's channel-schema/census claim, not the unresolved semantic
content of the operator-turn channel and not the correctness of the shared corpus filter. The digest proves two
enumerations agree; it does not prove they selected the right population.

**Loud current-data finding (2026-08-31):** `bb -cp . test/r2_channel_contract_test.clj` still found exactly
two ill-formed forms, but refused its historical baseline: the live corpus is now 798 forms / 796 conforming,
not 792 / 790, and its pin is `db71e095a81e8620…`, not `c9add16ac96c973b…`. This is precisely why `:basis`
cannot mean “the digest once bound in the registry.” APEX-D1 does not update that binding; its named consumer
must decide whether to requalify the enlarged corpus.

## Worked edge — R16→R2 re-observe

```clojure
{:evidence-contract/id :wm/R16-to-R2-reobserve
 :status :refused
 :subject {:kind :edge :id :control-map/R16-to-R2}
 :claim-clause
 {:claim-id :P-R16/solved-2
  :predicate "an outward actuation yields an external witness that R2 incorporates into a later observation"
  :evidence-kind :external-effect-receipt
  :producer-part "R16 construction/enactment path"}
 :requirement
 {:evidence-kind :external-effect-receipt
  :artefact-schema "{source-tick, mission, effect-address, digest, verifier, observation-tick, channel, status}"
  :domain {:population "each claimed outward actuation"
           :minimum-diversity "one incorporated independent effect and one refused or typed-absence case"}
  :corpus {:location :absent
           :runner :absent
           :basis :absent}
  :method {:command :absent
           :accepts "independent verifier confirms the external effect and R2 receipt confirms incorporation"
           :falsifier "the only witness is R16's own construction, or no correlated R2 incorporation receipt exists"
           :negative-control "the current wiring-map construction returned by enact.clj"}
  :independence {:declared-producing-part "R16 construction/enactment path"
                 :producer "current R16 executor"
                 :verdict :self}
  :consumer {:id :node/R2
             :uptake :absent}}
 :not-evidence [:r16-wiring-map
                :coverage-delta-over-own-diagram
                :enacted-nil
                :edge-schema-with-no-traffic
                :proposed-receipt]
 :open-decisions
 [:joe/R16-build-outward-actuator-or-rename-edge
  :joe/if-renamed-which-construction-claim-and-evidence-kind]}
```

Refusal is the result, not a missing draft field. The pair record says `:traffic-today false`; the edge fragment
says `:specified-no-current-traffic`; the current executor produces a construction inside R16's producing part.
Therefore it is `:self`, not an `:external-effect-receipt`. No corpus, runner, basis, or uptake edge exists, and
those absences are values. Ratifying an outward-actuation contract now would silently decide Joe's fork. If Joe
chooses the immediate rename, a separate construction-observation contract can permit `:self` evidence while
making the smaller claim. If Joe chooses the actuator, this refused contract supplies the required post-arm
receipt shape; the armed action must remain a split workflow under §0.14 criterion 6.

## Decisions requested from Joe

1. Is the proposed split between upward `claim-clause` and downward `requirement`, joined in one record, the
   correct governance contact point?
2. **RESOLVED in APEX-D2 below:** `:self` is an unprocessed observation, not an admissibility verdict.
3. **RESOLVED in APEX-D2 below:** R16→R2 is both, staged — rename to construction observation now, then build
   the outward actuator and independent reader.
4. On ruling, should the first implementation target be EDN + lint, or a Lean `EvidenceContract` projected to
   EDN? No implementation should precede the semantic ruling.

## What would make APEX-D2 dispatchable

Joe's answers turn the fields above into acceptance. Then APEX-D2 can define the schema, fixtures, negative
controls, and lint with typed ports, a named consumer, pinned reads, loud absences, and refusal for every remaining
decision. Until that ruling, producing a checker would merely encode this author's judgement as a standard.

## APEX-D2 — processing a datum into evidence (2026-08-31)

**Ruling applied.** Decision 2 above is dissolved: `:self` is neither admissible nor inadmissible evidence. It is
an **unprocessed observation**. R9 forbids self-certification, not self-observation. A producer may emit its own
datum on a named channel; that datum does not certify its claim. Review, cross-validation, and independent
reproduction bear on the channel's precision rather than changing the datum into an `:independent` boolean.

Decision 3 is decided **both, staged** by the specification: rename R16→R2 now to the construction observation
the machine actually emits, then build the outward actuator and independent effect reader required by P-R16.
The outward step remains operator-armed and split under §0.14 criterion 6.

### Typed processing prefix

This is the pipeline supported by the current closed Lean declarations:

```clojure
{:datum {:channel Holes.Channel
         :observation real
         :producer part-id
         :producing-part declaration-reference
         :basis immutable-basis}
 :belief-mean {:channel Holes.Channel :value real}
 :prediction-error "Holes.predictionError observation beliefMean"
 :precision {:type "Holes.PrecisionMap = Channel → NonnegativeReal"
             :provenance [precision-update-record ...]}
 :accumulation "Holes.variationalFreeEnergy (fun k => (precision k).value) prediction-error"
 :result {:kind :precision-weighted-variational-free-energy :value real}}
```

Precision updates are named operations, not invented arithmetic:

```clojure
{:operation #{:review :cross-validation :independent-reproduction}
 :channel Holes.Channel
 :prior NonnegativeReal
 :posterior NonnegativeReal
 :basis immutable-artefact-basis
 :producer part-id
 :producing-part declaration-reference
 :rule rule-reference
 :consumer :apex/evidence-processor}
```

All three operations may raise precision only through a cited `:rule`; absence of that rule is a typed refusal.
No closed Lean declaration currently says **how much** review, cross-validation, or reproduction changes Π, or
that it must increase it. APEX-D2 therefore names the operations and record shape but does not assign weights.

### The licensing boundary is open

The following tempting composition is **not** currently typed:

```text
variationalFreeEnergy : (Channel → ℝ) → (Channel → ℝ) → ℝ
bayesFactorThreshold  : ℝ → Prop
```

The shared carrier `ℝ` does not establish that the first result is the second function's `deltaF` argument.
`deltaFReduction` does produce a ΔF-shaped value, but it calls the open `logMultivariateBeta`; and
`bayesianModelReduction` transforms Dirichlet count lists without a declaration connecting those counts to the
datum's channel precision, its F value, or a before/after ΔF. BMR is therefore a candidate merge operation, not
yet a proved merge operator for two `EvidenceContract` sources.

The proposed licensing suffix is recorded, not asserted:

```clojure
{:merge {:operation "Holes.bayesianModelReduction"
         :blocked-on [:mapping-evidence-sources-to-dirichlet-counts]}
 :delta-F {:operation "Holes.deltaFReduction"
           :blocked-on [:Holes/logMultivariateBeta
                        :before-after-model-reduction-bridge]}
 :license {:predicate "Holes.bayesFactorThreshold deltaF"
           :threshold "deltaF ≤ -3"
           :blocked-on [:typed-delta-F-from-this-evidence-update]}}
```

No EIG term occurs. `expectedInformationGain` and `modelUncertaintyAndEIG` remain refused/open pending the
Outcome, Q(o∣π), and parameter-posterior kernels.

### Worked case — R2 channel census as a self observation

Take the current R2 checker emission “two key-set mismatches” as a datum on a proposed
`:r2/channel-contract-census` evidence channel. The checker is inside the R2 contract-producing part, so its
provenance verdict is `:self`; under the ruling that means unprocessed, not rejected.

For a finite worked calculation over the existing fourteen `Channel` constructors, let the observation and
belief mean differ by `2` on one named channel and agree on the other thirteen. Let an explicitly supplied
precision map give that channel weight `0.5` and every other channel any nonnegative weight (their errors are
zero). Then:

```text
ε_target = 2
F = ½ × mean_k(Π_k ε_k²)
  = ½ × ((0.5 × 2²) / 14)
  = 1/14
```

This carries the datum through channel → ε → supplied Π → accumulation. It does **not** license the R2 claim:

- the proposed evidence channel is not among the current fourteen observation channels;
- `BeliefState` is a hole, so the example supplies a mean rather than obtaining one from the model;
- no ratified precision-update rule derives `0.5` from self provenance, review, or reproduction;
- no typed bridge turns this F, or a change in it, into the ΔF consumed by `bayesFactorThreshold`.

Consequently the end-to-end result is `:processed-observation`, not `:certified-claim`. This is the executable
prefix and the honest refusal at its first undefined decision surface.

### Blocked carriers and named consumer

| step | state | blocked on |
|---|---|---|
| observation channel and datum | proposed apex record | evidence-channel carrier distinct from or extending WM `Channel` |
| belief mean | supplied in the worked case | `Holes.BeliefState` |
| prediction error | closed | — |
| precision map | closed carrier | ratified update rules for review, cross-validation, independent reproduction |
| F accumulation | closed | projection `NonnegativeReal.value`, which is already defined |
| observation likelihood/model | not used by the prefix | `Holes.observationKernel`, `Holes.GenerativeModel` |
| BMR merge | closed list operation, unconnected | evidence-source → Dirichlet-count interpretation |
| ΔF computation | body over a hole | `Holes.logMultivariateBeta`; before/after bridge |
| threshold | closed predicate | a typed ΔF produced by the preceding evidence update |

The named consumer of this draft is Joe for the semantic ruling, then `P-glossary-mathematics` for witness design.
The first implementable consumer after the bridges are ruled is an apex evidence processor whose output is taken
up by `checks/contract_lint.clj`; until then, no generated contract or witness-registry binding is warranted.

## APEX-D3 — source-to-Dirichlet mapping: REFUSED at the modelling decision (2026-08-31)

**Correction accepted.** There are two paths and two different free-energy quantities:

```text
perceptual:       (observation, mean, Π) → predictionError → variationalFreeEnergy → F
model reduction:  (A, a′, a, A′) → logMultivariateBeta → deltaFReduction → ΔF
                                                         bayesFactorThreshold ΔF
```

No F→ΔF bridge is wanted. The first path measures precision-weighted perceptual surprise. The second compares
model evidence over Dirichlet concentration parameters and can license a reduction when `ΔF ≤ -3`.
`deltaFReduction` and `bayesFactorThreshold` state the latter composition, but `logMultivariateBeta` remains a
hole, so the chain is defined and unwitnessed rather than executable end to end today.

### What the R2 corpus actually supplies

At the fresh basis recorded above, the R2 census supplies 798 classified trace forms:

```clojure
{:outcomes {:channel-key-set-conforming 796
            :channel-key-set-mismatch 2}
 :classification-rule "observed key set equals the fourteen named Channel constructors"
 :basis {:forms 798 :pin-prefix "db71e095a81e8620"}}
```

Those are observations. They do **not** uniquely determine any of the four arguments to
`deltaFReduction (A aPrime a APrime)`.

### The decisions a source-to-counts map would silently make

Even the obvious binary map requires all of the following declarations:

1. **Categorical variable:** whether the Dirichlet row models conformance versus mismatch, each channel's
   presence, trace-era schema membership, or another partition. The 796/2 tally chooses the first.
2. **Unit and exchangeability:** whether every trace form is one exchangeable trial. Forms are repeated ticks
   from one evolving machine and the two mismatches share one date, so this is not supplied by the census.
3. **Full and reduced models:** which hypothesis is `A` and which proposed reduction is `a′`. “The schema
   conforms” is a claim, not yet a pair of model structures.
4. **Priors:** the concentration vector `a` and reduced prior `a′`. Symmetric `[1,1]`, Jeffreys `[0.5,0.5]`,
   and a reliability-informed prior give different Bayes factors; no standing precept selects one.
5. **Posterior construction:** whether `A = a + [796,2]`, and how `A′` is derived under the reduced model.
   `bayesianModelReduction A a′ a` gives one algebraic relation after the priors are chosen; it does not choose
   them or establish that R2 forms are the observations those counts summarize.
6. **Role of ε and Π:** R2's perceptual error and precision do not become Dirichlet pseudo-counts by type or by
   any existing declaration. Weighting the 796/2 outcomes by Π would invent a second mapping.

These are semantic decisions, not implementation details. Choosing them here would let the apex manufacture the
model whose evidence it then certifies — R18 rebuilt one level higher.

### Refusal

```clojure
{:apex-delivery :APEX-D3
 :subject :wm/R2-observation-channel
 :requested :evidence-source-to-dirichlet-counts
 :status :refused-pending-ruling
 :available {:outcome-counts [796 2]
             :basis "798 forms; db71e095a81e8620…"}
 :missing-decisions
 [:categorical-variable
  :exchangeable-trial-unit
  :full-model
  :reduced-model
  :full-prior
  :reduced-prior
  :posterior-update-rule]
 :blocked-on [:Joe/evidence-model-ruling
              :Holes/logMultivariateBeta]
 :not-done [:map-precision-to-pseudocounts
            :choose-symmetric-prior-by-convenience
            :treat-796-over-798-as-a-bayes-factor
            :report-threshold-verdict-with-a-sorry-backed-log-beta]
 :consumer {:id :Joe
            :next "rule on the seven modelling fields, or select a theory-defined evidence model that fixes them"}}
```

No licensing verdict was run. A numerical `ΔF` before those choices would answer a model the commissioner did
not specify; a Lean result while `logMultivariateBeta` is `sorry` would not witness that hole. **APEX-D4
withdraws the requested rulings and the proposed suffix below:** BMR is not a claim-licensing device at all.

## APEX-D4 — claim-licensing device is absent (2026-08-31)

**Typed finding:**

```clojure
{:subject :wm/evidence-apex
 :licensing-device :absent
 :meaning "No formal declaration consumes evidence about a claim and returns a licensed/refused/unknown verdict under a stated rule or threshold."
 :falsifier
 {:exists "a declaration with typed evidence and claim inputs, a licensing verdict output, and stated decision semantics"
  :required-demonstration "one accepting and one refusing case elaborate or execute without sorry"}
 :basis
 {:lean "mathlib4/DarkTower/WarMachine/Holes.lean"
  :glossary "p4ng/sec-glossary.tex"
  :catalogue "p4ng/sec-catalog.tex"
  :negative-finding "holes/labs/slush-demo/findings/bmr_constellation_experiment.out.txt"}
 :consumer {:id :Joe
            :question "The build needs a claim-licensing device and AIF as formalised here does not supply one; what should it be?"}}
```

The falsifier does not currently fire. A numeric function returning `ℝ`, a threshold about a different subject,
or a declaration whose body is `sorry` does not inhabit the required input/output contract.

### Three nearby devices, none a claim licenser

| candidate | what it does | why it is not a claim-licensing device | evidence |
|---|---|---|---|
| `predictionError` → `variationalFreeEnergy` | Computes per-channel error and precision-weighted perceptual F | Terminates at `ℝ`; it takes no claim or evidence provenance and returns no licensing verdict. Nothing in the formal spine consumes this F for certification. | `Holes.lean:461–472` |
| `deltaFReduction` → `bayesFactorThreshold` | Compares a full and reduced Dirichlet model; decides whether a structural reduction clears `ΔF ≤ -3` | Its subject is a proposed model reduction/concept merge, not a claim. The paper explicitly assigns it to R17 structure learning. Count-only use has already over-merged every candidate. `logMultivariateBeta` is also still a hole. | `Holes.lean:482–491`; `sec-glossary.tex:54–60`; `sec-catalog.tex:338,342`; `bmr_constellation_experiment.out.txt:5–20` |
| `expectedInformationGain` / `modelUncertaintyAndEIG` | Intended to represent expected posterior information gain for policy evaluation | It is about information expected from a policy, not certification of a claim; both declarations are open/refused because Outcome, Q(o∣π), and the parameter-posterior kernel are missing. | `Holes.lean:479–480,505–506` |

The recorded BMR negative is discriminating, not a caveat: count-only `reduce-concepts` accepted 6903 of 6903
pairs, collapsed 118 patterns to one concept, and even accepted disjoint pairs at `ΔF=-6.45`. That instrument's
threshold can fail at its R17 job; moving it to the apex would import a known failure and change its subject.

### What would have to be true for a licensing device to exist

Without proposing the rule, its interface would have to establish all of the following:

1. A theory-defined `Claim` input and a typed evidence input whose provenance, basis, domain, and producer are
   part of the value rather than prose beside it.
2. A three-valued or otherwise explicitly typed verdict that distinguishes licensed, refused, and insufficient /
   unknown evidence; absence cannot collapse into refusal or success.
3. Stated decision semantics, including a threshold or rule, fixed independently of the evidence instance being
   graded and with an executable accepting and refusing case.
4. A declared treatment of self-observation and subsequent processing that does not permit self-certification.
5. A named consumer that changes a workflow decision on the verdict, plus a pinned route from evidence emission
   through processing to that consumer.
6. A theory or commissioner outside the producing part that authorises the rule. The apex may encode and enforce
   that rule; it may not invent the rule from the material it is asked to grade.

This is an absence report, not a design proposal. APEX-D4 does not select Bayesian testing, frequentist testing,
formal proof, replication policy, review protocol, or any mixture of them. Selecting one is the operator decision
now sent to Joe. Until then, “gated” remains a workflow judgement rather than a formally licensed claim, and the
`EvidenceContract` draft must not imply otherwise.
