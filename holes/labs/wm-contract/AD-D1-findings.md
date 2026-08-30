# AD-D1 — Lean declaration registry / Clojure witness adapter findings

Date: 2026-08-30. Reviewer: codex-22. Discovery only.

## Fixed deliverable

> **AD-D1 — discovery, no code** (codex-22, the Lean seat). Read both emitters, the APM consumer, the WM
> families, `Holes.lean`, `count-holes.sh`, and two Clojure check receipts. Produce: (i) a side-by-side of
> the two emitted shapes (fields, who consumes each field, which fields are restated on the Clojure side);
> (ii) the proposed common registry schema (EDN and JSON, one example entry per kind); (iii) the witness
> registry shape and where a check declares it (metadata map in the ns? a sidecar EDN?) with one worked
> example (`find_snatch.clj` ↔ F1–F4); (iv) which of the APM consumer's `required-*` blocks would survive as
> witness bindings and which are pure restatement; (v) refusals: anything in the APM adapter that cannot
> be generalised without changing APM behaviour. ≤ 200 lines, file:line throughout. Refusal permitted.

Source: `futon2/holes/problems/P-lean-clojure-adapter.md:59-66`.

## (i) The two shapes and their consumers

| Aspect | APM | War Machine |
|---|---|---|
| Root identity | `schema-version`, `contract-id` (`APMCycleContractEmitter.lean:110-115` in the regeneration authority) | `contract-version` (`ContractEmitter.lean:201-204`) |
| Declared content | phase order, phase I/O, receipts, submissions, transitions, trace-observation schemas, policies and bounds (`apm-lean/DarkTower/APMCycleContractEmitter.lean:73-122,216-395`) | families plus chain, compliance, coverage, policy-grade and reserved entries (`mathlib4/DarkTower/WarMachine/ContractEmitter.lean:72-209`) |
| Unit of declaration | operational phase/schema/policy maps | family `{id,name,lean-predicate,clauses,clojure-locus}` (`ContractEmitter.lean:63-70`) and named top-level clauses (`ContractEmitter.lean:106-160`) |
| Clojure reader | JSON is parsed to keyword maps (`generated_contract.clj:370-375`) | none; the emitter only prints JSON (`ContractEmitter.lean:211-215`) |
| Validation | structural transition/schema checks plus equality to eleven copied maps (`generated_contract.clj:381-448`) | none |
| Runtime use | trace projection takes observation schemas from the validated JSON (`campaign_trace.clj:26-41`); countdown overlays generated phase I/O, receipts, phase order, bounds and policies (`countdown_control.clj:174-207`) | `clojure-locus` is emitted but not resolved; the policy-grade locus even records typed absence (`ContractEmitter.lean:138-160`) |
| Restated fields | all eleven `required-*` blocks; details in (iv) | no consumer exists, hence no restatement yet |

There are two APM emitter copies. The supplied
`mathlib4-apm-validation/DarkTower/APMCycleContractEmitter.lean` ends its
contract at bounds (`:86-311`), but regeneration sets `lean_root` to
`/home/joe/code/apm-lean` (`regenerate-apm-contract.sh:4-16`). The actual
authority additionally emits trace schemas and later policy fields
(`apm-lean/DarkTower/APMCycleContractEmitter.lean:110-122,200-395`). The v4
artifact consumed at runtime is the path named in
`campaign_trace.clj:9-10` and `countdown_control.clj:49-52`.

`Holes.lean` already supplies declaration-level contact points: `FindResult`
and `find` (`Holes.lean@93f0da26:101-107`), F1–F4 (`:109-132`), and analogous
organise laws (`:144-169`). The current shell ledger merely greps two doc tags
(`scripts/count-holes.sh:7-21`); it cannot identify a witness or detect staleness.

## (ii) Proposed common declaration registry

One schema, represented identically after JSON-key/EDN-key conversion:

```edn
{:schema-version 1
 :contract-id "wm-holes"
 :source {:module "DarkTower.WarMachine.Holes"
          :git-sha "93f0da26"}
 :declarations
 [{:name "fastForward" :kind :closed
   :signature "Set P → (P → P → Prop) → P → P → Prop"
   :owner "P-validated-R5 §3e O3" :holder "claude-15"
   :decided "2026-08-30"
   :clojure-locus "checks/find_snatch.clj" :fixture nil}
  {:name "findF1Containment" :kind :hole
   :signature "∀ tension repo, selected(find tension repo) ⊆ repo.patterns ∧ …"
   :owner "P-validated-R5 §3e F1" :holder "claude-15"
   :decided "2026-08-30"
   :clojure-locus "checks/find_snatch.clj"
   :fixture "checks/find-snatch.edn"}]}
```

```json
{"schema-version":1,"contract-id":"wm-holes",
 "source":{"module":"DarkTower.WarMachine.Holes","git-sha":"93f0da26"},
 "declarations":[
  {"name":"fastForward","kind":"closed",
   "signature":"Set P → (P → P → Prop) → P → P → Prop",
   "owner":"P-validated-R5 §3e O3","holder":"claude-15",
   "decided":"2026-08-30","clojure-locus":"checks/find_snatch.clj","fixture":null},
  {"name":"findF1Containment","kind":"hole",
   "signature":"∀ tension repo, selected(find tension repo) ⊆ repo.patterns ∧ …",
   "owner":"P-validated-R5 §3e F1","holder":"claude-15",
   "decided":"2026-08-30","clojure-locus":"checks/find_snatch.clj",
   "fixture":"checks/find-snatch.edn"}]}
```

The examples correspond to the record-defined `fastForward` body
(`Holes.lean@93f0da26:139-142`) and the still-open F1 law (`:109-114`). Unlike
WM's current family records, whose clause prose and loci are admitted literals
(`ContractEmitter.lean:123-136`), every declaration has source identity and a
typed state.

## (iii) Witness registry and worked `find` binding

Use a sidecar `checks/contract-witnesses.edn`, not namespace metadata. Loading
the namespace performs repository/file discovery at definition time
(`find_snatch.clj:47-69`), whereas a lint must inspect bindings without running
the check. The sidecar also binds several declarations to one run atomically.

Actual metadata the completed run can carry:

```edn
{:schema-version 1
 :contract {:id "wm-holes" :source-git-sha "93f0da26"}
 :witnesses
 [{:declarations
   ["DarkTower.WarMachine.Holes.findF1Containment"
    "DarkTower.WarMachine.Holes.findF2Receipted"
    "DarkTower.WarMachine.Holes.findF3NonSelfCertifying"
    "DarkTower.WarMachine.Holes.findF4Falsifiable"]
   :check {:namespace "find-snatch" :entrypoint "-main"
           :source "checks/find_snatch.clj"}
   :fixture "checks/find-snatch.edn"
   :run-sha "594127059d6a31c5b21f5d34ebfac5b14b1ad993"
   :result :passed}]}
```

The binding is checkable: selection is constrained to repository patterns and
typed absence is emitted (`find_snatch.clj:78-102`); each selected item gets a
structured receipt and file-text warrant (`:71-76,92-101`); F4 is asserted per
scenario (`:121-150`); the report records F1/F4 and its source SHA
(`:152-175`). The fixture's last commit is the stated run SHA; its fixture path
is the check's `output-path` (`find_snatch.clj:12-14`). F2/F3 should not be
inferred merely from the fixture's presence: the registry binds them because
the run populated `:receipts` with `:route` and `:warrant` (`:92-101`).

The same shape covers non-degeneracy: `ablate_g_snatch.clj` computes the
ablation verdict from changed minimisers (`ablate_g_snatch.clj:163-177`) and
records the registered prediction (`:179-196`), writing
`checks/ablation-snatch.edn` at run commit
`636496449066aed88c00e99492bd351cef0f0821`.

## (iv) Every `required-*` block

Classification is against the actual regeneration authority and v4 artifact,
not the stale `mathlib4-apm-validation` copy identified above.

| Block | Verdict | Evidence |
|---|---|---|
| `required-bounds` | RESTATEMENT | copied map at `generated_contract.clj:5-13`; Lean emits bounds at `apm-lean/.../APMCycleContractEmitter.lean:385-393`; compared at `generated_contract.clj:407-408` |
| `required-dispatch-policy` | RESTATEMENT | copy at `:15-95`; Lean field begins at `APMCycleContractEmitter.lean:122`; whole-map equality at `:424-425` |
| `required-memory-policy` | RESTATEMENT | copy at `:97-125`; Lean field begins at `APMCycleContractEmitter.lean:216`; equality at `:426-427` |
| `required-isolation-policy` | RESTATEMENT | copy at `:127-130`; Lean field at `APMCycleContractEmitter.lean:314`; equality at `:430-431` |
| `required-promotion-policy` | RESTATEMENT | copy at `:132-187`; Lean field begins at `APMCycleContractEmitter.lean:251`; equality at `:428-429` |
| `required-terminal-policy` | RESTATEMENT | copy at `:189-232`; Lean field begins at `APMCycleContractEmitter.lean:320`; subset equality at `:432-436` |
| `required-student-candidate-policy` | RESTATEMENT | copied terminal-policy subset at `:234-243`; extracted and compared at `:389-393,437-439`; Lean owns the enclosing field at `APMCycleContractEmitter.lean:320` |
| `required-analyst-policy` | RESTATEMENT | copy at `:245-250`; Lean field at `APMCycleContractEmitter.lean:376`; equality at `:440-441` |
| `required-submission-schemas` | RESTATEMENT | copy at `:252-283`; Lean builder at `APMCycleContractEmitter.lean:86`; equality at `:442-443` |
| `required-phase-io` | RESTATEMENT | copy at `:285-325`; Lean derives phase maps at `APMCycleContractEmitter.lean:73-75`; equality at `:444-445` |
| `required-receipt-schemas` | RESTATEMENT | copy at `:327-368`; Lean derives schemas at `APMCycleContractEmitter.lean:77-84`; equality at `:446-448` |

Count: **11 RESTATEMENT, 0 BINDING**. None survives as a witness binding. The
genuine Clojure-owned bindings are check namespace/source, fixture, run SHA and
observed result, as in (iii); none is a second copy of a Lean value.

## (v) Refusals / behaviour that must remain specific

1. Refuse to choose an APM source silently. Regeneration names `apm-lean`
   (`regenerate-apm-contract.sh:5,13-17`), while this packet named the divergent
   `mathlib4-apm-validation` emitter (`APMCycleContractEmitter.lean:86-313`). A
   shared adapter needs one declared source path/SHA or must fail authority
   validation.
2. Do not generalise away APM's legacy round trip. It compares emitted phase
   order with the live legacy EDN contract and has a named failure
   (`generated_contract.clj:453-465`); replacing it with declaration-witness
   lint would change launch behaviour (`countdown_control.clj:188-207`). Keep it
   as an APM-specific compatibility check.
3. Do not turn trace projection into generic “file exists” witnessing. It
   requires every Lean-declared collection and field source and errors on
   absence (`campaign_trace.clj:26-41`); that is operational validation, not a
   registry join.
4. A runtime emitter cannot obtain a stable pretty-printed signature merely
   from the declaration `Name`: WM currently extracts only `Name.getString!`
   (`ContractEmitter.lean:55-61`). Until the shared helper has environment-aware
   declaration inspection, signature text must be explicitly registered and
   source-SHA-bound, never guessed.
