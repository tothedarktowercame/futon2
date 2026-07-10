# Spec: build-match — does the build match the CLean structural spec?

**Owner/reviewer:** claude-4 (Codex builds, claude-4 reviews). **Extends:**
`futon2/src/futon2/aif/actuator_a3.clj`. **Idea (Joe):** A3 proves only the *boundary*
(the terminal endpoints inhabited). The CLean is the *structure*, so the build must match
the **whole spine** — every box's `:produces` inhabited in substrate-2, not just the
terminal. A3 is the special case (terminal box only); build-match is the generalization.

## Design

The deposit carries an embedded `:clean` (already gated well-formed by `clean_argcheck`
at load). Build-match verifies the *build* realizes that structure:

1. **`box-bindings`** — an authored map `box-id → substrate binding`
   `{:kind :entity|:hyperedge :type <kw> :endpoint-types [...]}`, from the deposit's
   `:box-bindings` or a reviewed grounding map (mirror `reviewed-endpoint-bindings`;
   **never builder-chosen**). Not every box needs one — only boxes whose `:produces` has a
   substrate manifestation. **M-learning-loop fixture bindings:**
   ```clojure
   {:b1 {:kind :entity    :type :capability}                              ; the vocabulary
    :b7 {:kind :hyperedge :type :capability/* :endpoint-types [:capability :mission/doc]}} ; the hypergraph
   ```

2. **`box-match-snapshot`** — for each box WITH a binding: reuse `proof-query`/`proof-form`/
   `endpoint-inhabitation` (all already in actuator_a3.clj) to check its `:produces` is
   inhabited in substrate-2 via authed Drawbridge. Returns `[{:box :produces :inhabited?}]`.

3. **`build-match`** `[deposit opts]` — (a) confirm `:clean` passes `clean_argcheck`
   (structure well-formed — reuse the loader's check or re-run); (b) `box-match-snapshot`
   over the bound boxes; (c) dial `{:inhabited :bound :clean-pass? :discharged?}` where
   `:discharged?` = `clean-pass?` AND every bound box inhabited.

A3's `endpoint-bindings` are the terminal-box special case; keep A3 working, add build-match
as the whole-spine generalization (box-bindings ⊇ the terminal binding).

## Scope

**Boxes only** in this build (`:produces` inhabited per bound box). **Wire `:carries`
realization** (each wire's flow realized as a substrate relation) is the NEXT layer — note
it, do not build it here.

## Acceptance (tests — authed Drawbridge; evict any test writes)

- M-learning-loop CLean + the fixture box-bindings → dial `:inhabited 1 :bound 2`
  (`:b1`/`:capability` inhabited=true; `:b7`/`:capability/*` inhabited=false),
  `:discharged? false`. (Matches A3's 1/2, now keyed on CLean boxes.)
- **The discriminating test (the whole point):** a build state where the TERMINAL box is
  inhabited but an INTERIOR box is not → build-match `:discharged? false` and names the
  missing interior box. (Drive it: write a temp `:capability/*` hyperedge [terminal b7
  inhabited] against a binding whose interior box type is absent → the structure check
  catches what a terminal-only boundary check would pass. Evict after.)
- A `:clean` that fails `clean_argcheck` → build-match refuses (structure invalid), no dial.
- Bindings are read from the deposit/grounding map, never from a builder result.

## Gates

clj-kondo clean; `futon4/dev/check-parens.el` clean; tests green. Reuse A3's
inhabitation machinery + `clean_argcheck` (no reimplementation). Authed Drawbridge
(`x-admin-token`). Do NOT restart :7071. Bell **claude-4** back with commit SHAs.
