# PROOF-2a ⟨1⟩2 — Joe's items 1–5

Author: codex-2, 2026-09-27. Implementation on Joe's direct instruction;
independent review remains with claude-8. Item 6 (BMR) was explicitly deferred.
This report does not accept ⟨1⟩2 as a whole.

## Changes and remaining work

| Item | Implementation | Limit |
|---|---|---|
| 1. Three Lean/runtime correspondences | mathlib4 `8d98c63f97`: `Proof2.ChannelIdentity` selects current check name AND source identity; `Proof2.Revision3Rates` represents both-cell minimum 5, authorised Jeffreys estimate and its kernel construction; `Proof2.AccumulationAtRuntime` proves the state-taking outer-product recurrence, concatenation/carry law and positive-prior preservation. futon2 `202b5a026` records the bindings. | These are mathematical correspondences on admitted inputs, not a claim that Lean executed Clojure or that temporal filtering is wired. Raw-count results remain the unsmoothed case. Store admission and uniqueness precede the identity projection. |
| 2. Likelihood precision | `202b5a026`: omission selects ζ=1; explicit nil returns invalid-zeta and no certificate. Registry explicitly says beta-zeta learning is absent and precision is fixed. | Existing support/endpoint rulings unchanged. No beta-learning law invented. |
| 3. Tick-side exact filtering | **Not implemented.** The required temporal input producer is absent; see below. | Initialization, per-policy flight conditioning and arena microsteps are not substitutes for this path. |
| 4. Arena absence through readers | `443a24b0f`, `3b76aa8a5`: missing A cells/models refuse, zero evidence has no posterior, refusal survives subsequent entity updates and carry, health/population predictions and trace. Prediction-error receipts preserve the cause. | Arena remains the declared tempered seven-status model. This does not connect counted token A to that carrier or claim exact temporal filtering. |
| 5. Accumulation continuity and forwarding | `fff29cba1`: preserve initialization, bind entity/model lineage, require actually observed coordinates, reject changed predecessor while holding the existing append lock; configuration failures become receipts. Default flight judge forwards configuration and trace directory; its existing post-construction publisher remains the sole publisher. | No live seeded restart/migration or flight executed. Old tails without origin/lineage, or receipt-only tails, cannot silently initialize or skip. A failed publication is described explicitly below. |

## Item 3: the unresolved producer

`token_belief_carry/stage` explicitly stages initialization. Its v2 carry has
no posterior. `token_belief_predecessor/input-receipt` v3 authorizes signed
observations as **next-selection initialization**, and the observation authority
requires `:consumption :not-authorized` for the temporal claim. The legacy path
records `:conditioning-consumption-not-wired` when task execution is admitted.
`token_initialization_policy/apply-observations` modifies fresh fact initialization;
it does not predict the previous posterior through the previous enacted action.

The arena's micro-loop uses several synthetic events derived from one scan. Those
are not fresh observations at successive prediction times. Its seven statuses
also differ from the token-set carrier of the exact kernel. The existing
flight conditioning is per-policy and is not already a single tick trajectory.

The structural work still needed is one producer/consumer contract binding:

1. A persisted previous posterior and its occurrence, domain and model identity.
2. The independently witnessed previous enacted action, with its transition
   model (selection or a constructed plan is insufficient).
3. A newly observed outcome with occurrence/revision and a consumed-event identity,
   so neither the initial observation nor another tick's evidence is reused.
4. Prediction and conditioning through the existing exact core, followed by
   consumption of that posterior by the next selection and trace.

Tests must distinguish a non-identity previous action from the current candidate,
reject duplicate/stale events and changed model/domain, and retain impossible
observations without fresh initialization. There is no missing arithmetic
primitive; the missing work is the witnessed temporal join. The current
initialization authority must not be promoted by changing its status label.
This is an unresolved implementation requirement, not an optional upgrade and
not a request to waive it by agreement.

## Why each refusal exists, and what it stops

| Case | Mathematical or bookkeeping necessity | Effect |
|---|---|---|
| Explicit nil ζ | A numerical default would contradict a certificate retaining nil. | Scorer returns missing/invalid-zeta and no certificate, as existing score refusals do. |
| Missing/invalid A or B, invalid prior/weight, unknown categorical observation | Missing model cells cannot be asserted zero; malformed quantities do not define the declared filter. | Typed entity refusal; no posterior number. No new exception in the filter. |
| Zero predictive evidence | Normalization is undefined. Uniform is a new prior, not that posterior. | Refusal retained through carry, prediction/error receipts and trace. |
| Missing observation envelope or absent coordinate | The existing observation vector can contain a numeric zero for a missing source. Counting that as observed zero fabricates a trial. | Accumulation absence receipt; selection unchanged. No partial-trial policy is invented. |
| Missing origin, model revision, changed entity/revision or tick identity | A recurrence must update the same model from its declared origin, not mix entities or invent a restart. | Accumulation absence receipt; selection unchanged. |
| Receipt-only or pre-accumulation tail | Skipping the actual predecessor or fresh initialization would break the recorded chain. | Existing migration-required absence retained; no live migration performed. |
| Unreadable configuration | Learning configuration failure does not invalidate selection. | Absence receipt forwarded to the decision rather than configuration-reader exception. |
| Predecessor changed before append / unreadable authoritative history | Two writers must not both publish the same prior as a serial chain. | **Publication throws before append.** Scheduled/flight callers can consequently report run/trace failure. This is the expected-predecessor rejection option from item 5, not merely a passive receipt. No claim that selection continues successfully after this publication failure. |

The publication check is inside `lane_futility/append-indexed-trace!`'s existing
cross-process lock, before updating either trace or index. It does not nest locks.
The test uses two actual disk readers before the first write; the second
publication refuses and the corpus contains only the first successor.

## Evidence

Scoped builds of all three new Lean modules succeeded. Axiom output for the rate
bounds/kernel correspondence and accumulation declarations contains no sorryAx;
existing imported Holes warnings are not described as a clean whole-repository
proof audit.

Registered Lean warrants (all `warrant? true`, postcheck matched):

- ChannelIdentity: `test-registry-1d0fb875d452abee5193ed3d24df6a9a26cacefc15ec615253379888c5a3609a`
- Revision3Rates: `test-registry-04732321ecbea5b690ad6cb1a443397765c8d8b9034a2d394e5c4bf0e24cb919`
- AccumulationAtRuntime: `test-registry-cbf55bdb527c2979bef6143f1caf4b641f448aa8b1dcaa0d8548ca2608332419`

Runtime checks, one namespace at a time: likelihood precision 16 tests/178
assertions; belief 78/2030; refusal continuity 3/31; accumulation kernel 8/250;
WM accumulation and forwarding 12/57; free energy 28/162; trace 40/131;
lane futility 5/18; live arena wiring 3/9. All passed. clj-kondo reports no
errors or warnings on the changed production/new test files; existing belief
tests have an unrelated redundant-let warning. Emacs check-parens passed.
Registry EDN reads successfully; no formal equation was weakened.

At commit time I relied on claude-8's earlier coordination response that no
flight was in the air; I did **not** take a contemporaneous process/park snapshot
at 00:25:43Z. A later process check found only source-inspection commands
containing runner names, not a matching flight process; it cannot establish the
earlier state retrospectively. No live reload, restart, migration, flight or
production observation was performed by this packet.

Final runtime warrants at futon2 `3b76aa8a5` (all warrant true, postcheck matched):

- Likelihood precision: `test-registry-69ddc617a7db5bb93d6e79e9fd4fa654edf66df491001e9e95f9b8f09b4e99bc`
- Arena refusal continuity: `test-registry-0a059c01f2536a584a02b0d05e4d0f1edb314312b1b1b071f45465209077f1ef`
- Accumulation and flight configuration forwarding: `test-registry-590a48d37084c01c8f5238a7131028b7eabdcd52d0e915d086470329fdfd01f0`

The final two warrants supersede earlier registrations made before the explicit
missing-model correction; no obsolete warrant is used as evidence for that fix.
