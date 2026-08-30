# CML-D2 — R9→R16 reconciled proposal

Status: discovery-shaped proposal only. No source, Lean, edge EDN, or node record was changed.

## One proposal, endorsed twice

- **Observed.** R9 proposes one schema for R9→R16, R9→R12, and R9→R2: `{claim, witness {id, producer, layer}, verdict ∈ {independent, self, unknown}}`, ExactlyOnce, `(claim-id, witness-id)`, and the verdict record as receipt (`holes/problems/P-R9.md:77-81`).
- **Observed.** R16 explicitly calls its shape “P-R9's proposed `{claim, witness, verdict}`” (`holes/problems/P-R16.md:61-62`). This is one proposal endorsed at the receiving node, not two independently arrived proposals. The agreement therefore supplies no independent confirmation.
- **Observed.** R16 adds a reading, not a field: “an act carries an independent witness of its precondition” (`holes/problems/P-R16.md:61-62`). Its actuation definition separately requires an external witness (`holes/problems/P-R16.md:42-47`). Thus the added consumer constraint is that only verdict `independent` authorizes the precondition; `self` and `unknown` are delivered verdicts but do not authorize acting. No retry, timeout, atomicity, or new payload field is added.

## Reconciled `Delivery`

The record type requires all eight operational/schema fields (`/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:313-322`). Proposed instance:

```edn
{:from :R9
 :to :R16
 :payload {:claim :Claim
           :witness {:id :WitnessId :producer :Producer :layer :Layer}
           :verdict [:enum :independent :self :unknown]}
 :guarantee :ExactlyOnce
 :atomicWith :unspecified
 :retry :unspecified
 :timeoutMs :unspecified
 :idemKey [:claim-id :witness-id]
 :receipt {:claim :Claim
           :witness {:id :WitnessId :producer :Producer :layer :Layer}
           :verdict [:enum :independent :self :unknown]}}
```

- **Observed.** `payload`, `guarantee`, `idemKey`, and “receipt = the verdict record” are stated by R9 (`holes/problems/P-R9.md:79-81`). The receipt above retains the identifiers alongside the verdict so the proposed idem key can be checked; the exact serialization of `Claim`, `Producer`, `Layer`, and IDs remains unspecified.
- **Observed refusal.** `atomicWith`, `retry`, and `timeoutMs` are unspecified because neither endpoint record states them (`holes/problems/P-R9.md:77-81`, `holes/problems/P-R16.md:58-64`). What would settle them is an endpoint record amendment by the owner naming the paired write, retry identity/cap, and deadline. Defaults from R16→R2 are a different edge and were not imported.
- **Inferred, untested.** “Receipt = the verdict record” might mean only `{verdict}` rather than the full correlated record shown above. The idem key requires claim/witness identity (`holes/problems/P-R9.md:80-81`), but the record does not say those identities are embedded in the receipt. Owner clarification must settle the exact receipt schema before EDN entry.

## Can R16 consume all three verdicts?

- **Observed.** Lean makes `unknown` a first-class `IndependenceVerdict`, alongside `independent` and `self` (`/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:222-227`). It returns `unknown` precisely when no producing-part declaration exists (`/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:229-235`).
- **Observed.** R9 requires `producingPart` to be declared, never inferred, and its checker returns all three verdicts (`holes/problems/P-R9.md:28-34`). R16 says the act carries an **independent** witness of its precondition (`holes/problems/P-R16.md:61-62`). Therefore R16 can consume the enum as a decision input, but must abstain/refuse on `self` and `unknown`; treating unknown as permission would contradict the receiving record. This is a verdict constraint, not deletion of `unknown` from the payload.
- **Inferred, untested.** The payload's opaque `claim` must carry or resolve the declared producing part for R9 to compute a non-unknown verdict. The proposal does not specify whether that declaration is embedded or referenced; the Lean `Claim` contains `producingPart` (`/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:211-220`). The owner must settle its wire representation.

## Does the delivery happen today?

- **Observed negative with positive control.** Command: `rg -n 'r9-independence|R9-D2|:verdict|:enactment' src scripts checks holes/labs/wm-contract --glob '*.{clj,edn}'`. The instrument found the R9 producer/checker (`checks/r9_independence.clj:13-22`, `checks/r9_independence.clj:98-129`) and found the R16-side enactment writer/readers (`src/futon2/aif/enact.clj:218-232`, `src/futon2/aif/trace.clj:278-279`), but found no source path passing an R9 verdict into `enact!` or an act-gate. Limit: literal search cannot detect dynamically resolved or renamed data.
- **Observed.** R9-D2 writes verdict tables as report fixtures (`checks/r9_independence.clj:111-129`, `checks/r9_independence.clj:131-138`); it is a checker/run artifact, not a delivery producer. R16 `enact!` accepts `{mission, shown, act-gate}` and emits no claim, witness, or independence verdict (`src/futon2/aif/enact.clj:205-232`).
- **Conclusion, bounded by the instrument.** No explicit implementation currently carries R9 assurance into an act precondition. R9→R16 specifies traffic that does not presently occur.

## Refusals / unresolved points

- Refused to count R16's citation as independent endpoint agreement (`holes/problems/P-R16.md:61-62`).
- Refused to invent `atomicWith`, `retry`, or `timeoutMs` (`holes/problems/P-R9.md:79-81`).
- Refused to let `unknown` authorize an act: it means the declaration needed for the independence decision is absent (`/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:229-235`).
- Refused to claim a fully fixed receipt wire shape until the owner decides whether “verdict record” includes the correlation fields (`holes/problems/P-R9.md:80-81`).
