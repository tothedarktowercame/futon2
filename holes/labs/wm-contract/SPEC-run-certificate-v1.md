# Runtime certificate v1 — specification for a pinned re-architected run

Status: **DISCOVERY / PROPOSED FOR REVIEW**, 2026-09-12. No certificate is
minted here, no run is selected, and no Lean declaration or registry row moves.
In particular `:lean-attestation` remains `:suspended-vocabulary` until Joe
accepts a certificate. That is the vocabulary used by the commissioned runtime
record, and the definition of done reserves “backed by Lean” for what the
certificate actually computes
(`holes/NOTE-cascade-structured-proof-validation.md:384-403`;
`holes/labs/wm-contract/RULINGS-walkthrough-2026-09-12.md:168-220`).

Inspected revisions: futon2 `1166a832f0c2e2c27681fbd18c27a8091d527b48`;
futon3c `5e4638e3ca1c3079d2336727b5fa43a472d3affa`; mathlib4
`f4fb8c2713cb4c32916d5b2f6d6db439a6b6ddc2`. File/line pointers below are
against those revisions unless an historical pin is stated explicitly.

## 1. Certificate artifact and pure derivation

The certificate is a strict EDN value. A JSON rendering may be emitted from the
same value, but is not another authority. The proposed shape is:

```clojure
{:certificate/schema :wm/runtime-certificate-v1
 :run {:id string :series-id string :attempt-id string
       :started-at instant-string :closed-at instant-string
       :outcome keyword :record-root string}
 :sources [{:role keyword :path string :sha256 hex-string
            :schema keyword-or-number :recorded-at instant-string} ...]
 :route {:trace-record-sha256 hex-string
         :routes [[node-string ...] ...]
         :hops [{:from string :to string :via string-or-keyword
                 :at string} ...]
         :drawn-wiring {:path string :git-sha string :sha256 hex-string}}
 :deliverable-chain
 {:g-record {:source-sha256 hex-string :pointer vector}
  :cascade {:source-sha256 hex-string :pointer vector :sha256 hex-string}
  :wiring {:source-sha256 hex-string :pointer vector :sha256 hex-string}
  :fold-output {:source-sha256 hex-string :pointer vector :sha256 hex-string}
  :fold-execution {:source-sha256 hex-string :pointer vector}
  :fold-authority {:source-sha256 hex-string :pointer vector}
  :shape-validation {:validator string :version string :ok boolean
                     :findings vector :input-sha256 hex-string}
  :correspondence-validation {:validator string :version string :ok boolean
                              :findings vector :cascade-sha256 hex-string
                              :fold-output-sha256 hex-string}
  :selection-enaction {:source-sha256 hex-string :pointer vector
                       :verdict keyword :selected map :enacted map}}
 :equation-bindings
 [{:registry-row keyword :node keyword :realisation keyword-or-boolean
   :tick-path-evidence {:source-sha256 hex-string :pointer vector}
   :declaration {:module string :full-name string :kind string
                 :contract-id string :source-git-sha string
                 :source-sha256 hex-string}
   :contract-manifest-sha256 hex-string} ...]
 :checker {:generator {:path string :git-sha string :sha256 hex-string}
           :module string :module-sha256 hex-string
           :lean-version string :lake-manifest-sha256 hex-string
           :mathlib-git-sha string :contract-bundle-sha256 hex-string}
 :scope {:checked [keyword ...] :not-checked [{:claim keyword :why string
                                               :source-pointer string} ...]}}
```

Every value is derived by one pure function `derive-certificate(records,
drawn-wiring, equation-registry, declaration-contract, checker-identity)`. It
accepts immutable bytes, parses them, and either returns the certificate or a
typed refusal. It does not consult a live service, infer a missing event, or
accept an operator-authored replacement field.

Derivation rules:

1. Sort checkpoints by `:event/sequence`; require one cohort/attempt identity,
   contiguous sequence, monotone `:recorded-at`, and the expected start/close
   endpoints. The F11 chain supplies sequences 1–7 and timestamps in
   `/home/joe/run4/F11-production-successor-20260912/cohort/run4-f11-production-successor-20260912-v1/attempt-001/001-time-step.edn:1`
   through the sibling `007-closed.edn:1` (the intervening sibling records are
   named `002-selection.edn` through `006-adjudication.edn`, each at line 1).
2. Hash the exact bytes of every source before extracting fields. Store each
   path, hash, schema, and timestamp in `:sources`. All subsequent joins use
   those hashes, not path coincidence.
3. Extract the G/selection record from checkpoint 002; the cascade and wiring
   from checkpoint 003; execution, authority and selection/enaction from that
   same construction judgment when present. The commissioned construction
   demonstrates `:fold/execution`, `:fold/authority`, enriched wiring and a
   `:selection-enaction :match` in
   `futon3c/holes/labs/wm-contract/runs/RUN4-F11-production-successor-2026-09-12/production-fold-commissioning-v5/run4-f11-production-successor-20260912-v1/attempt-001/003-construction.edn:1`.
4. Canonicalize only for sub-value hashing (specified EDN canonical ordering,
   UTF-8, no metadata); retain exact whole-file hashes separately. A digest
   equality proves identity, not semantic correspondence. The latter must be a
   validator result or an explicitly scoped review, as the agreed interface
   requires (`holes/NOTE-apm-wm-convergence-contrast.md:205-213`).
5. Reassemble routes from the trace node sequence into adjacent hops and retain
   `:via` and timestamp evidence. Never synthesize a missing hop. The F11 trace
   record shows the route field and run/provenance identities at
   `/home/joe/run4/F11-production-successor-20260912/run-records/tick-run-record-7b1a1c3e-171e-4bdf-94dd-a27cd5297bd0.edn:1`.
6. Select only registry rows marked realised whose node/quantity is evidenced
   on this tick path. Join each row to a unique checked declaration by full
   `(module, declaration-name)` identity and pin the declaration source and
   contract bundle. Ambiguity, a missing declaration, stale source bytes or a
   realised row without tick-path evidence refuses emission. The checked
   population and strict union join are described at
   `holes/labs/wm-contract/TN-box3-machine-contract-join-discovery-2026-09-12.md:64-74`.

This artifact is a checked view over existing acceptance records. It neither
re-runs validators nor replaces their authority; it binds their exact inputs,
versions and outcomes.

## 2. Meaning of “conforms to AIF” in certificate v1

| Proposition | Evidence and decision today | Lean status |
|---|---|---|
| Route conformance | At least one route; no empty route; every adjacent hop is classified by the pinned drawn/measured layers; no `unmapped` or code-retired `refutation` hop. The exact predicate is `runConformsToDrawnWiring` (`mathlib4/DarkTower/WarMachine/Holes.lean:7579-7593`). | Decidable over transcribed finite lists; `by decide`. |
| Deliverable completeness | Construction wiring and fold output are non-nil; unary enriched-output validation is green; output×cascade correspondence is green; recorded wiring/holes/cascade equal the values whose digests and validator results are bound. The validators landed at `holes/NOTE-cascade-structured-proof-validation.md:404-410`. | New finite Boolean/Prop structure, decidable once certificate fields and expected validator identities are transcribed. Lean checks the recorded, digest-joined result; it does not independently interpret prose warrants. |
| Selection/enaction | A recorded `:match` must carry equal selected/enacted action identities. A non-match is allowed only as an explicit typed divergence with class, grounds and both actions; absence is failure. The commissioned record contains the positive shape at its `003-construction.edn:1`. | New decidable sum type (`match` or typed divergence) and equality/census theorem. Whether a divergence is semantically acceptable remains the named external review encoded by its class; Lean checks that it was not hidden. |
| Realised equation binding | For each realised registry quantity evidenced on the tick route, require a unique declaration in the checked contract population at pinned bytes. Registry examples include closed Lean carriers such as observation at `holes/labs/wm-contract/aif-equations.edn:74-85` and precision at `:86-91`; the emitted population is governed by `machine-contracts/manifest.json:1` and `machine-contracts/machine-contracts.json:1`. | Declaration existence, uniqueness, revision equality and registry/tick membership are decidable finite relations. This certifies binding to a checked declaration, not that every runtime value equals its mathematical denotation unless a row-specific witness theorem is also included. |
| Scope honesty | The certificate contains the exact negative scope below. Missing or broader wording is a checker failure, not optional prose. | Decidable equality/subset checks over a closed scope enum plus pinned explanatory references. |

### Required negative scope

Certificate v1 does **not** prove:

- unrealised R1→R17 or R2→R17 learning edges. They remain not realised until
  U91/U92; a certificate may record their absence but cannot count them as
  conformance (`holes/labs/wm-contract/aif-equations.edn:199-215`;
  `holes/labs/wm-contract/worklist.edn:1615-1626`);
- runtime inhabitants for the uninhabited FUNDAMENTALS constructions, including
  the belief-to-state distribution and controlled transition kernel documented
  at `holes/labs/wm-contract/FUNDAMENTALS.edn:112-176`;
- semantic truth of free-text roles, warrants, conditions or review prose;
- route coverage of edges that did not fire, prediction of the route by the
  diagram, or behavior above the witnessed declaration rung. The precedent
  explicitly reports 19 of 22 drawn edges unfired and warns that measured edges
  weaken the claim (`mathlib4/DarkTower/WarMachine/Holes.lean:7617-7632`);
- task success, mission closure, empirical improvement, or generalization from
  one run. Run count is not validation
  (`holes/labs/wm-contract/RULINGS-walkthrough-2026-09-12.md:209-220`).

An emitted certificate missing any applicable limitation is invalid. A
certificate that overclaims is worse than no certificate.

## 3. Lean checker and generation plan

### Existing precedent

The s5 machinery transcribes the drawn edge layers, retirement grounds and four
recorded routes into finite Lean values (`Holes.lean:7424-7577`), defines the
conformance predicate (`:7579-7593`), then proves both conformance and the full
route census with `by decide` (`:7617-7650`). Its source comment pins the drawn
map and run bytes and says the literals are generated by
`holes/labs/wm-contract/u49_route_transcribe.bb`, not hand-edited
(`Holes.lean:7424-7438`). Per-run regeneration therefore requires: exact run
records, exact map bytes, their revisions/hashes, a deterministic transcription,
and regeneration of routes/census when any pin changes. `wmTraceR2` and
`wmTraceR8` are the same fixture-constant pattern: pinned finite data followed
by decided properties (`Holes.lean:1385-1398`, `:1448`, `:6996-7003`).

### Proposed generated module

Generate `DarkTower/WarMachine/RunCertificate_<run-id>.lean`, importing the
stable route predicate and the checked declaration modules. The generator emits:

- a `RuntimeCertificateV1` term and finite enums for checkpoint roles, route
  nodes, validation outcomes, selection disposition, scope and equation binds;
- exact source and sub-artifact SHA-256 strings as identity pins;
- `runRoutes`, `runHops`, equation-binding rows and the expected scope list;
- `runCertificateConforms : RuntimeCertificateConforms certificate := by
  decide`;
- a census theorem (checkpoint count, route/hop/class counts, boxes, holes,
  realised bindings and typed divergences), also `by decide`.

Cryptographic hashing of arbitrary files is performed by the emitter, not
reimplemented as a Lean theorem. Lean decides equality of emitted digest
strings and all finite relations. Claims requiring interpretation—whether a
typed divergence is justified, whether prose conditions are true, or whether a
row-specific runtime value semantically implements its equation—must have an
explicitly scoped reviewed statement or an existing witness theorem; they are
not smuggled into `decide`.

Workflow:

1. Freeze the candidate run directory and all referenced source bytes; refuse a
   dirty or incomplete source tree.
2. Run the pure certificate emitter into a temporary directory. It validates
   checkpoint continuity, digest joins, validator results, route assembly,
   selection disposition, realised-row population and mandatory negative scope.
3. Emit strict EDN/JSON plus the Lean module from that single intermediate
   value. Re-read both and compare their projected fields before installation.
4. Run Lean elaboration and `#print axioms` on the generated declarations;
   require no `sorryAx`. Record toolchain, lake manifest, mathlib revision,
   emitter hash, module hash and contract-bundle hash in the certificate.
5. Independently rerun the existing Clojure route/validator checks against the
   pinned inputs and compare censuses. Review the generated diff; never edit
   literals by hand.
6. Only after review and Joe's choice of qualifying run, install the generated
   module and certificate. Only Joe's acceptance can close
   `wmRunConformsToWiring`, whose current declaration deliberately remains
   `sorry` (`Holes.lean:7411-7412`).

Required refusal controls mutate one fact at a time and must make generation or
Lean checking fail: source byte/hash mismatch; removed or reordered checkpoint;
empty route; unknown hop; code-retired hop; nil wiring; failed unary validator;
failed cascade correspondence; changed cascade/wiring digest; selection/enaction
mismatch without typed divergence; divergence missing grounds; realised row
with missing, ambiguous or wrong-revision declaration; missing required scope
limitation; wrong checker/toolchain identity. Controls must also show that a
typed, fully populated divergence is structurally accepted without being
silently relabeled a match.

## 4. Candidate qualifying runs — choice reserved to Joe

### F11 re-admitted cycle

The 16:44–16:52 F11 chain has a timestamped seven-checkpoint sequence and a run
trace, but its original construction has `:wiring nil`
(`/home/joe/run4/F11-production-successor-20260912/cohort/run4-f11-production-successor-20260912-v1/attempt-001/003-construction.edn:1`)
and closes with `:outcome :guardrail-refusal`
(`/home/joe/run4/F11-production-successor-20260912/cohort/run4-f11-production-successor-20260912-v1/attempt-001/007-closed.edn:1`). The
17:09 production commissioning has enriched wiring, fold execution/authority
and matching selection/enaction, but currently only checkpoints 001–003
(`futon3c/holes/labs/wm-contract/runs/RUN4-F11-production-successor-2026-09-12/production-fold-commissioning-v5/run4-f11-production-successor-20260912-v1/attempt-001/001-time-step.edn:1`
through the sibling `003-construction.edn:1`). Neither directory alone is the requested
full successful chain. A qualifying F11 rerun would need one identity-bound
001–007 chain containing the enriched construction, green validators,
selection/enaction disposition, typed terminal, complete route and effective
environment/checker pins. It must not splice the later construction into the
earlier refusal run merely because mission/cohort labels resemble each other.

### On-demand outer-loop run at the Empirics standard

This is the natural candidate named by Item 4, provided it performs real work
through the re-architected system, records timestamps and the complete chain,
and meets every certificate input above. It additionally needs the Item-2
on-demand capability evidence, a declared comparison basis to the earlier
Empirics run, and honest reporting of duration, attempts/refusals and feedback
repairs. It need not imitate “77 runs”; one qualifying run plus its checked
certificate is the stated definition of done
(`RULINGS-walkthrough-2026-09-12.md:168-220`).

This specification prepares both. Which run qualifies is Joe's decision at
certificate time.

## 5. Effort split

| Work | Size/status |
|---|---|
| Hashing, checkpoint ordering, pointer extraction, route reassembly, canonical sub-value digests and EDN/JSON emission | **Mechanical, mostly existing shapes.** Reuse the RUN4 checkpoint reader and `u49_route_transcribe.bb` route rules. |
| Enriched-output and cascade-correspondence verdict capture | **Mechanical, landed.** Bind the existing validator identities/results and their exact inputs; do not create another acceptance authority. |
| Route predicate and decided census | **Mechanical extension of proven machinery.** New per-run generated data/module; stable predicate already exists. |
| Certificate Lean structures, completeness predicate, selection disposition, equation-binding relation and mandatory-scope predicate | **New but bounded statements.** All are finite and intended for `decide`; require review of exact proposition strength. |
| Declaration population join | **Mechanism landed, integration required.** Consume the machine-emitted `machine-contracts` bundle plus `holes-contract.json`; preserve unique full-name and revision checks. |
| Semantic equation-to-runtime claims | **Only where existing row-specific witness theorems license them.** Otherwise certificate scope stops at declaration binding. New semantic witnesses are separate work, not certificate-generator conveniences. |
| R1/R2→R17, U91/U92; uninhabited FUNDAMENTALS; claims above witnessed rungs | **Blocked/out of scope and explicitly negative.** They cannot be converted into passes by certificate wording. |
| Qualifying run capture | **Not done; Joe chooses.** F11 needs a single complete enriched rerun; the outer-loop candidate needs an Empirics-standard on-demand run. |
| Acceptance and closure of `wmRunConformsToWiring` | **Joe-gated.** Keep the hole and `:lean-attestation :suspended-vocabulary` until he accepts the generated certificate. |

Discovery identity pins used while writing this plan: `aif-equations.edn`
SHA-256 `51b928561f6d8c13336266f48561295011d3220090c4d8a97b0ebb4cdcb3e99f`;
`machine-contracts/manifest.json`
`1ced38773c04da65e461081bdcfafb494f23b45927ac08c68ef08b273e481516`;
`machine-contracts/machine-contracts.json`
`a2f894f90888ed43fdea89d7ee08f5684c71c699977e2fc499e7a747eb35c488`;
`holes-contract.json`
`333b31739f984d2a291da1ce0551ae219fcc1aa5fb4af4e5a414910b8f69bde9`.
