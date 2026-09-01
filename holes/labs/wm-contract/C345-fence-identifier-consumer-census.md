# C345 — fence identifier consumer census

Date: 2026-09-01. Owner: `wm-evidence`. Assessment only; no consumer changed.

## Result

The defect population is **three direct evidence acceptors plus one transport
seam**, not two isolated sites. Two consumers are receipt-backed and sound.
There is no current fence consumer in the operational certificate or
`run-readiness`.

A pure-function counterexample supplied `"fabricated"` to all three direct
acceptors with otherwise equal endpoints. All three returned `:event-free?
true` and `:distinguishable-cause? true`:

```clojure
{:gate     {:event-free? true :distinguishable-cause? true}
 :contract {:event-free? true :distinguishable-cause? true}
 :read-set {:verdict :satisfied :event-free? true
            :distinguishable-cause? true}}
```

## Population

| Component | Input | Consumes receipt? | Disposition |
|---|---|---:|---|
| `checks/writer_fence_evidence.py` | `--fence-id` plus structured attestation | yes, plus two live world observations | **sound**; observed findings precede attestation completion |
| `scripts/wm_preflight.clj` | ID plus evidence receipt | yes, and reruns the live evidence bundle | **sound**; only verified evidence yields `:observed-held` and event freedom |
| `scripts/run_workspace_gate_bounded.py` | `FUTON_WRITER_FENCE_ID` | no | **transport seam**; validates only lexical shape and injects the ID into the bounded command |
| `checks/wm_workspace_gate.clj` | inherited ID | no | **direct defect**; stable endpoints plus any ID yield `:status :held`, `:event-free? true`, and `:fence-conditional` |
| `checks/contract_authority_current.clj` | inherited environment ID or `--writer-fence` | no | **direct defect**; any ID yields `PASS (FENCE-CONDITIONAL ID)`, `:status :held`, and event freedom when its own source endpoints did not move |
| `checks/mutable_read_set.clj` | caller-provided `{:declared-fence {:status :held ...}}` | no | **latent direct defect**; equal endpoints plus any asserted held map satisfy the event-free claim. There are currently no non-test callers of this option. |
| C319 step 3 | ID exported to `make workspace-gate` | no receipt passed | **procedural instance** of the transport/gate defects; it relies on operator continuity rather than a machine join |
| Operational certificate and `run-readiness` | none | n/a | **not in population**; neither currently reads a fence ID or emits the surveyed fence vocabulary |

The bounded wrapper is not itself an event-verdict producer, but it is the
load-bearing route by which one asserted name reaches two producers in the same
gate run. The mutable-read-set API is dormant, not harmless: its contract makes
the next caller able to recreate the defect without using the environment
variable.

## Reachability and meaning

The bounded gate's `:event-free? true` is reachable in both a genuinely fenced
world and an unfenced world. The branch tests only whether the repository
endpoint basis is stable and the ID is nonempty. Therefore the current gate
claim is **unconditional with respect to fence evidence**: in a real fenced
window it happens to be true, while outside one the identical output is
unearned. The receipt cannot distinguish those worlds.

`FENCE-CONDITIONAL` is an honest label only when interpreted as “conditional on
an externally established premise.” It is not evidence that the premise was
established. The simultaneous `:writer-fence {:status :held}` and
`:event-free? true` fields overstate that weaker meaning.

## Repair shape implied by the census

This is no longer safely described as two edits. The gate, its contract
constituent, and the generic read-set substrate each have an independent
identifier/status-to-evidence conversion. A single receipt-validated fence
capability or shared verifier should be the only value capable of satisfying
those branches; the bounded wrapper should transport that evidence or its
verified capability, not promote a name. This is a census conclusion, not an
implemented repair.

## Inventory

The delivery inventory command is:

```sh
bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn (g/inventory-result))"
```

Result: `{:name :check-inventory, :exit 0, :unknown (), :missing ()}`.
