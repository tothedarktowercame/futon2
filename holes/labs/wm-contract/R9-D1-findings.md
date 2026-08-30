# R9-D1 — independence/value-evidence inventory

Status: discovery only, 2026-08-30. No source code changed.

## Instruments and limits

- **Observed.** Located the formal file with `rg --files /home/joe/code | rg
  '/GainChain\.lean$'`: `mathlib4/DarkTower/WarMachine/GainChain.lean`; its own
  scope disclaimer says it is an outline with no emitter, Clojure contract, or
  mutation suite (`mathlib4/DarkTower/WarMachine/GainChain.lean:21-26`).
- **Observed.** Searched the named artifacts for `producingPart`,
  `producing-part`, and `producing part` with `rg -n`; exit 1, zero matches.
  Corpus: R18 badges, R2/R8 contracts, `sec-discussion.tex`, `GainChain.lean`,
  rollout/red-team birth-tag code, calibration, and pilot. This establishes
  absence only in that named corpus, not the whole workspace.
- **Observed.** Parsed the complete one-form XTDB export with `clojure.edn` (not
  `head`): 90,583 records; 90,583 have `:evidence/author`, 56,379 have
  `:evidence/session-id`, and 90,583 have `:evidence/type`. The schema is visible
  in every record on the export's sole physical line
  (`futon1b/migration-export/evidence.edn:1`). A recursive map walk found zero
  `:reviewed-by`, zero `:attested-by`, and zero `:independent?` keys in this
  export. Limit: the export is a snapshot and one physical line, so it cannot
  establish absence in later stores or other corpora.
- **Observed.** Parsed all of `data/r18-badges.edn`: 17 quantities, with
  `{:engineering-control 12, :derived-from-FEP 5}`. This contradicts the file's
  headline “4 derived” (`futon2/data/r18-badges.edn:13-15`).
- **Observed.** `rg -n 'closed thirteen|thirteen|closed' sec-discussion.tex`
  finds only the prose assertion that the author closed thirteen
  (`p4ng/sec-discussion.tex:238`); the thirteen repair rows are not enumerated
  in that file. Therefore this packet's claim that `sec-discussion.tex` is the
  corpus over which to run thirteen per-row verdicts is wrong as stated.

## Inventory

### R18 badges: value awarded without a declared producing part

- **Observed.** The badge vocabulary awards `:derived-from-FEP` and
  `:principled-approximation` alongside weaker classifications
  (`futon2/data/r18-badges.edn:5-11`). Claimant: the R18 badge contract/generator,
  which calls itself canonical (`futon2/data/r18-badges.edn:1-3`). Producer:
  not represented as a typed producer; prose sometimes names a reviewer.
  Layer: undeclared. `producingPart`: absent from the named corpus and would
  have to be declared.
- **Observed.** `G-risk` carries `:derived-from-FEP` while its note says the
  raise was made by reviewer `claude-12` using live-tick evidence and a C9
  census (`futon2/data/r18-badges.edn:97-108`). Claimant: R18 badge contract.
  Witness producer: mixed/underspecified (live WM records, C9 artifact,
  `claude-12`). Layer: **unknown**, because neither the outcome producer nor a
  claim-producing set is recorded. Different reviewer identity alone cannot
  establish L2. `producingPart`: must be declared.
- **Observed.** `G-ambiguity` is likewise `:derived-from-FEP` and names
  `claude-12`, live ticks, C9, and a shadow as support
  (`futon2/data/r18-badges.edn:133-145`). Claimant: R18 badge contract. Producer:
  mixed/underspecified. Layer: unknown; the shadow and model-relative tick
  evidence are L1 unless an externally produced realized outcome is identified.
  `producingPart`: must be declared.
- **Observed.** `G-goal-outcome` is `:derived-from-FEP`; the note names the same
  reviewer and live-tick/C9 rationale, while `:repair-built` says the badge raise
  was still pending live-tick evidence and burn-in
  (`futon2/data/r18-badges.edn:231-243`). Claimant: R18 badge contract. Producer:
  underspecified. Layer: unknown. The two fields disagree about whether the
  raise is complete; independence cannot resolve that without declared claim
  scope and witnesses. `producingPart`: must be declared.
- **Observed.** `channel-precision` and `belief-update` receive
  `:derived-from-FEP` from statements about computation under declared models,
  not external outcome calibration (`futon2/data/r18-badges.edn:189-208`).
  Claimant/producer: the same R18/model implementation unless separately
  declared. Layer: L1/model-relative for value; these entries can support
  implementation-form claims but not independent value. `producingPart`: must
  be declared before either is used as value evidence.
- **Observed.** `policy-performance-ratio` explicitly computes
  realized-versus-expected controller feedback and is labelled engineering
  control (`futon2/data/r18-badges.edn:157-168`); `outcome-feedback-selection-gain`
  consumes realized-versus-expected outcomes and is also engineering control
  (`futon2/data/r18-badges.edn:219-230`). Claimant and producer: WM/R18 unless
  separated by a witness declaration. Layer: L1 by construction. These are
  correctly not awarded canonical value badges, but no machine rule prevents a
  later relabel.

### `:reviewed-by` / `:attested-by`: identity fields are not independence

- **Observed.** The named XTDB migration export contains no such keys (instrument
  and limit above), despite preserving record author/session/type
  (`futon1b/migration-export/evidence.edn:1`). Thus provenance exists, but a
  review-identity field is not part of these exported evidence records.
- **Observed.** One wiring edge is merely typed `:reviewed-by` with note “merge
  to master” (`futon3c/holes/missions/M-war-machine-first-outing-wiring.edn:147`).
  Claimant: ledger/operator edge. Producer/reviewer: not named in the edge.
  Layer: unknown. `producingPart`: not declared.
- **Observed.** Workspace searches also find promotion records where the
  evidence author and `:promotion :reviewed-by` can be the same agent; for
  example `claude-6` authors and reviews the record
  (`futon3c/holes/labs/M-diagramprover/apm-driver/corpus-export/corpus.edn:89`).
  Claimant: promoted memory record. Producer/reviewer: `claude-6`. Layer: self,
  not L2. `producingPart`: absent. This is a concrete instance of why
  `:reviewed-by` cannot itself certify independence.

### Birth tags and calibration: a claimant-supplied Boolean

- **Observed.** `rollout_ledger` copies input `:independent?` into the canonical
  record as a Boolean (`futon0/scripts/futon0/futonzero/rollout_ledger.clj:58-80`).
  Claimant: record producer. Producer: whatever supplied the input map, not
  captured as `{author, session, component}` here. Layer: asserted, therefore
  unknown; `producingPart`: absent.
- **Observed.** The red-team corpus falsifies `:independent? false`, but does not
  establish who may truthfully set it true
  (`futon0/scripts/futon0/futonzero/reward_red_team.clj:68-85`). Claimant and
  witness producer are fixture construction. Layer: L1 test fixture.
  `producingPart`: absent.
- **Observed.** The live pilot sets `:independent? true` when its caller supplies
  `executed?` plus an `evidence-ref`; the docstring calls this independence and
  lets calibration count it (`futon3c/src/futon3c/peripheral/war_machine_pilot.clj:450-469`),
  and the assignment occurs at `:518-522`. Claimant: pilot/caller. Witness:
  opaque `evidence-ref`; its producer is not checked against the claimant.
  Layer: unknown, not established L2. `producingPart`: must be declared.
- **Observed.** Calibration filters on that Boolean plus `:measured` and
  non-transient status, then permits ten such records to reach a value verdict
  (`futon3c/src/futon3c/aif/calibration.clj:261-303`). Claimant: calibration
  reporter. Producer: upstream pilot/caller and realized field. Layer: intended
  L2, but structurally only “tagged L2”; no producer-set exclusion exists.
  `producingPart`: must be declared.
- **Observed.** Flight reconstruction treats `:independent?` as equivalent to
  `executed?` (`futon3c/src/futon3c/aif/flight_record.clj:284-291`). Claimant:
  flight record. Producer: prior trace. Layer: inherited assertion, not an
  independently checked layer. `producingPart`: absent.

### Paper self-audit and the thirteen closures

- **Observed.** The paper's advance rule bars manuscript-internal restatement
  from clearance as Layer 1 (`p4ng/sec-discussion.tex:20-33`). Claimant: paper
  audit. Producer: reviewers/author. `producingPart`: described informally, not
  declared as a producer set.
- **Observed.** The paper says the independent reviewer found a stronger spine,
  while also distinguishing independent evidence from independent *value*
  evidence (`p4ng/sec-discussion.tex:108-124`). Claimant: paper authors.
  Producer: commissioned reviewer. Layer: extraction independence only; value
  layer remains unknown because the proxy was author-chosen. `producingPart`:
  must be declared.
- **Observed.** The R9 verdict explicitly says the author commissioned and
  briefed reviewers, chose suspicions, and then closed thirteen rows without
  independent close verification (`p4ng/sec-discussion.tex:224-240`). Claimant:
  paper author/self-audit. Producer of closure: author. Layer: self/L1. Verdict:
  the registered expectation `self` is supported for the aggregate claim, but
  per-row checker inputs cannot be constructed from this file alone.
- **Observed.** The paper later limits the exercise to the same authors and
  configured reviewers, calling it a second domain rather than a second party
  (`p4ng/sec-discussion.tex:328-336`). This confirms that model-family difference
  is not sufficient independence.

### GainChain L1/L2 gap

- **Observed.** `RealizedOutcome` records one producer enum, expected leg,
  realized measurement, and durability
  (`mathlib4/DarkTower/WarMachine/GainChain.lean:71-95`). Claimant: gain-chain
  model. Producer: `coverageDelta | groundedDial`. Layer: not represented.
  `producingPart`: absent.
- **Observed.** `gainChainSound` checks identity, presence, durability, typed
  absence, domain, precondition, and gain movement, but not independence or
  L1/L2 (`mathlib4/DarkTower/WarMachine/GainChain.lean:121-179`). Therefore a
  self-produced realized leg can satisfy the model. The missing constraint is
  exactly “realized producer outside declared claim-producing set.”

### R2 and R8 evidence contracts under the proposed checker

- **Observed.** R2 e3 is declared exactly-once witnessed operator-turn storage
  and explicitly the only edge whose content the machine cannot produce
  (`futon2/holes/labs/wm-contract/R2-glossary-formalisation.md:100-107`). For
  the narrow **storage** claim: claimant is the R2 observation apparatus;
  witness producer is the operator-turn/store path; layer L2; verdict
  `independent` if that path is excluded from `producingPart` by declaration.
- **Observed.** The same R2 contract explicitly says C1's 20/20 proves storage,
  not reading, and says the reading channel does not exist
  (`futon2/holes/labs/wm-contract/R2-glossary-formalisation.md:110-126`). For
  **reading**: no witness producer; layer unavailable; verdict `unknown`. The
  two claims must remain separate.
- **Observed.** R8's `g` contract requires updates from L2 outcomes, but its
  named method only counts outcomes and diffs `g`; its falsifier currently fires
  on the whole corpus (`futon2/holes/labs/wm-contract/R8-glossary-formalisation.md:147-161`).
  Claimant: gain mechanism. Producer: not declared per outcome. Layer: all
  present corpus pairs are at best L1 under this contract. Verdict: `unknown`
  per witness until producer and `producingPart` are supplied; the current
  corpus-level failure remains valid but is not an L2 certification.

## Refusals / packet defects

- I refuse to report thirteen per-row `self` verdicts: the commissioned corpus
  contains only the aggregate statement, not thirteen identifiable claim/witness
  pairs (`p4ng/sec-discussion.tex:238`). Doing so would invent rows.
- I refuse to treat `:independent?`, reviewer family, `:reviewed-by`, or an opaque
  `evidence-ref` as a producer exclusion. Each is evidence about identity or
  execution, not the required declared `producingPart` relation.
- The packet's statement that every evidence record carries session provenance
  is too strong for the named export: 56,379/90,583 have `:evidence/session-id`,
  while author and type are complete (`futon1b/migration-export/evidence.edn:1`).
