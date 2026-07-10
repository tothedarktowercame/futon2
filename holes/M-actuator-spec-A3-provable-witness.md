# Spec: A3's real acceptance — the provable-match substrate-2 witness

**Owner/reviewer:** claude-4 (Codex builds, claude-4 reviews the ungameability).
**Extends:** `src/futon2/aif/actuator_a3.clj`. **Foundation (verified):** authed
Drawbridge `submit-tx` writes + `q` proves on the same live node (smoke test PASS,
commit futon1a `e60e1ab`); the executor's `drawbridge-resolves?` now authenticates
(`x-admin-token`) and already returns `(:capability inhabited) => true`.

## The principle (from Joe)

A witness is NOT gameable if it is the fold's own provable output. The dial is not
a checkbox count and not `file-exists` — it is: **are the sorry's typed substrate-2
endpoints provably inhabited?** You cannot `touch` a typed `:capability/*` hyperedge
over real `:capability`×`:mission/doc` nodes into existence — producing that
structure IS the work. The proof is derived from the fold's typed endpoints, NOT
chosen by the builder, so there is nothing to game.

## Design

1. **Endpoint bindings (authored upstream, NOT builder-chosen).** Each output
   endpoint of the want-signature binds to a substrate-2 type-signature:
   ```clojure
   {:endpoint "CapabilityVocabulary"  :kind :entity    :type :capability}
   {:endpoint "CapabilityHypergraph"  :kind :hyperedge :type :capability/*
    :endpoint-types [:capability :mission/doc]}
   ```
   Source: the deposit's `:endpoint-bindings` if present; else a reviewed grounding
   map (we already grounded M-learning-loop). **The builder never supplies the
   binding** — that would let it choose a trivially-true query. Bindings are the
   fold/grounding's job.

2. **Proof-query derivation** (`binding -> xtdb.api/q form`, deterministic):
   - entity:   `'{:find [e] :where [[e :entity/type <type>]] :limit 1}`
   - hyperedge: `'{:find [e] :where [[e :hx/type <type>]] :limit 1}` (and, when
     `:endpoint-types` given, verify an instance carries those roles/types).

3. **Inhabitation check** — run the derived query via authed Drawbridge (reuse the
   now-authed `drawbridge-resolves?`/`result-truthy?` path): non-empty ⇒ provably
   inhabited. This is the whole witness.

4. **The dial** = number of the want-signature's endpoints provably inhabited.
   `dial-moved?` ⇔ a previously-uninhabited endpoint becomes inhabited (compare a
   before-snapshot to an after-snapshot, both taken by the executor via Drawbridge).
   **Discharged** ⇔ all endpoints inhabited. (For M-learning-loop: before = 1/2
   — `:capability` ✓, `:capability/*` ✗ — discharged when the builder writes the
   `:capability/*` hyperedges.)

5. **Builder's job** (update `build-prompt`): write the bound endpoints INTO
   substrate-2 via authed Drawbridge `submit-tx` (the sanctioned write form) so they
   become inhabited. No checkbox, no `file-exists`-as-dial. The executor proves
   inhabitation itself.

6. **Retire as the dial**: the `- [ ]` checkbox count and `advance-mission-doc!`
   become optional downstream bookkeeping, NOT the acceptance. The anti-gaming
   doc-edit machinery is no longer load-bearing (the substrate proof is ungameable).

7. **`review-partial` (reuse/adapt)**: per-endpoint `{:endpoint :inhabited? :query
   :reason}`, plus `:endpoints-remaining` and `:next-feedback`.

## Scope note

This build is the **substrate-2 endpoint match** — the tractable, ungameable core,
and the one whose foundation is proven. The **wiring-diagram structural match**
(built code ⟼ code-graph nodes composed per the DarkTower diagram) is the second
provable layer Joe named; note it as the next spec, do not build it here. The
hyperedge endpoint-type check (step 2) already captures the relational slice of the
wiring.

## Acceptance (tests)

- `binding -> proof-query` derivation is correct for entity and hyperedge bindings.
- Inhabitation check against the LIVE store (authed Drawbridge) on the known
  before-state: `:capability` ⇒ inhabited (true); `:capability/*` ⇒ not inhabited
  (false). (Stable facts; this is an integration test and may hit :6768.)
- Dial from an endpoint-binding set: M-learning-loop `{:capability ✓, :capability/* ✗}`
  ⇒ dial = 1/2, `discharged? false`.
- **Ungameability test:** the dial is computed from executor Drawbridge queries over
  the bindings, NOT from any builder-supplied claim — a builder result with no real
  write leaves the dial unmoved. (Unit-level: feed a binding whose type is absent ⇒
  not inhabited, regardless of what the builder "claims".)
- `review-partial` breakdown correct on a mixed inhabited/not set.

## Gates

clj-kondo clean; `futon4/dev/check-parens.el` clean; the tests above green. Reuse
the authed Drawbridge path (`x-admin-token` from `futon3c/.admintoken`). Do NOT
restart :7071. Bell **claude-4** back with a summary + commit SHAs.
