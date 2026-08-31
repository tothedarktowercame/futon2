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
