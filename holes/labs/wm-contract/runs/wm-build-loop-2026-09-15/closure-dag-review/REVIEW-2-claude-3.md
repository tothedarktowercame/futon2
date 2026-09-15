# Closure DAG revision 2: claude-3 review

Read-only review of codex-28's revision 2 (p4ng `4012254`), requested in job `invoke-1789514752705-21289-41df5fe6`. My revision-1 review is `REVIEW-claude-3.md` in this directory (futon2 `e99c08c4`).

| File | sha256 |
|---|---|
| `CLOSURE-DAG.md` | `1df6eb83…` |
| `closure-dag.json` | `b1486fb9…` |
| `LOOP-CONTRACT.md` | `4b27b9b7…` |
| `REVISION-2.md` | `09291821…` |
| `validation.json` | `8b851cb5…` |

## Verdict: ACCEPT, with two residual corrections

Revision 2 addresses all seven correction categories. The edge semantics and authority gates are sound, and the graph is fit to drive scheduling now. Two closure nodes still lack any production gate (section 4); neither is dispatchable, so neither blocks electing the next packet.

## 1. Structural checks, re-derived independently

I did not rely on `validation.json` or `validate.py`.

- 73 nodes: 46 `checklist-closure`, 16 `implementation-delivery`, 7 `authority-gate`, 4 `shared-evidence`.
- **No closure node appears in any `dispatch-requires`.** (My first pass of this check was vacuous: I matched the kind name `checklist-closure` wrongly and compared against an empty set. Redone properly, it holds: 0 occurrences.)
- Acyclic under `dispatch-requires`, under `accept-requires`, and under their union.
- No dangling references, and no legacy `depends-on` remains.
- `checklist-sha256` `f2194faf…` equals the current checklist, and all 73 cascade pins equal their files.
- The Markdown table matches the JSON for all 73 rows, in both edge columns.
- **Every closure node's acceptance text is verbatim from the current checklist** (46/46), so the refresh to revision 11 did not paraphrase any clause.
- `dispatchable` is false everywhere, consistent with narrowing into pinned packets.

Edge kinds are disciplined: `dispatch-requires` only ever points at deliveries, authority gates or shared evidence; closure nodes only carry `accept-requires`.

## 2. The corrections, as landed

| Correction | Status |
|---|---|
| Typed edges replacing untyped `depends-on` | Done: `dispatch-requires` / `accept-requires`, with the meanings stated in the JSON and in LOOP-CONTRACT's new "Typed projection rule" |
| WM-11 ↔ R16 | `EV-enactment` required by both |
| WM-02/03 ↔ R3 | `EV-update` required by WM-02, WM-03, R2, R3 and R10 |
| R14 ↔ R20 | `EV-trip-commitment` required by both |
| WM-01-delivery too coarse | Split into `WM-01-carrier-contract` (state `review-needed`), `WM-01-bindings`, `WM-01-numerical-operations` |
| WM-04 acquisition gating implementation | Split into `WM-04-machinery` (delivery) and `WM-04-evidence` (shared evidence, requiring `AUTH-observation-commission`) |
| WM-05/WM-07 ← WM-04-delivery | Now `WM-04-machinery`; measured-A moved to the WM-05 and WM-07 acceptance |
| WM-05 ← WM-09, WM-12 | Removed from dispatch; both are acceptance requirements of WM-05 |
| WM-06 ← WM-13 | Removed. WM-06 acceptance now carries `AUTH-C-bridge` |
| WM-08 ← WM-01 | Removed; `WM-08-delivery` has no dispatch prerequisites |
| WM-02 ← WM-11-delivery | Removed |
| WM-04 ← WM-05/WM-07 | Removed; WM-04 acceptance is machinery plus evidence |
| WM-01 acceptance missing the C family | `WM-13-delivery` added |
| Authority gates | Seven added, each with an `owner` field (Joe or codex-28) and a `consumer` |
| R10/R15/R16/R17/E08 relations | Bound through `EV-update`, `WM-04-evidence` and delivery nodes rather than whole closures, as the disposition describes |

I checked each of these against the node records, not just the disposition text.

## 3. Points I specifically agree with

- **`WM-01-carrier-contract` is `review-needed`, not accepted.** The accepted subclauses (numeric-1, support-1, float-carrier-1) are scheduling inputs; the node does not claim the numerical operations are correct. That matches what I reviewed.
- **Gates resolve existing authority** rather than requesting fresh permission. `OPS-serving-activation` records that reload permission is not revoked while restart stays separately controlled, and `OPS-ordinary-run/click` requires authorization for the particular occurrence with "no inference from source work".
- **Shared-evidence nodes are occurrence-bound** ("one authorized occurrence", "one exact observation-to-update join"), so they cannot be discharged by a generic capability claim.

## 4. Residual corrections

**R9 and E02 demand a production operation but can reach no gate**, even transitively through acceptance edges. Both have empty `accept-requires`.

- **R9** requires "authenticated genesis, retained exact review commissions, independent producer/reviewer binding and **production consumer**".
- **E02** states that "close-retention source acceptance at `3a6f4f61` does not establish that a **post-change production close** used it".

For comparison, the other production-dependent clauses do reach gates: WM-08 and R5 reach `OPS-serving-activation`; WM-09 and R6 reach both OPS gates plus `EV-enactment`.

Suggested fix: give R9 and E02 acceptance edges to `OPS-serving-activation` and `OPS-ordinary-run/click` (or to a shared close-occurrence evidence node, if you prefer one that R10 and E03 can share). Without it, a supervisor would report these two as closable on documentary evidence alone.

**Also empty, and I am not asking for edges**: R7, R12, E07 and E09. R7 and R12 describe current producer/consumer chains, so they may deserve `EV-update`; E07 and E09 look documentary. Your call, and worth recording either way so the emptiness is deliberate rather than an omission.

## 5. Loop integration

Unchanged from my revision-1 review, and REVISION-2.md keeps the five topology incompatibilities explicit as integration work rather than claiming them fixed. Nothing was started or modified, which matches what I see: no new supervisor, and `inflight-packet` is now null.

The attended bellback path remains the working mechanism and needs no code.

## Limits

- Read-only. I started no loop and changed no code.
- I checked structure, edge kinds, pins, acceptance text and the specific corrections. I did not re-derive every acceptance edge in the graph from its cascade; section 4 lists what I did check for gate coverage.
- Accepting this graph accepts a scheduling proposal. It closes no checkbox and asserts no WM item is complete.
