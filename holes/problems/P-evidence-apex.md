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
           :basis "content pin recorded by the witness binding (currently c9add16ac96c973b…)"}
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
2. Must every ratified claim have an independent witness, or may a conformance claim such as R2 admit `:self`
   evidence when an executable negative control can defeat it? The draft recommends independence be a per-contract
   requirement, never a universal synonym for evidence.
3. For R16→R2: rename now to construction observation, build the outward actuator, or both staged. This draft
   recommends the already-recorded staged choice but does not enact it.
4. On ruling, should the first implementation target be EDN + lint, or a Lean `EvidenceContract` projected to
   EDN? No implementation should precede the semantic ruling.

## What would make APEX-D2 dispatchable

Joe's answers turn the fields above into acceptance. Then APEX-D2 can define the schema, fixtures, negative
controls, and lint with typed ports, a named consumer, pinned reads, loud absences, and refusal for every remaining
decision. Until that ruling, producing a checker would merely encode this author's judgement as a standard.
