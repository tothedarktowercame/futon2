# L5-7-I-1 — Production preference registry: loader, validation, kernel, pure (claude-2 → codex-6, 2026-11-01)

Base: futon2 `main` (≥ the commit this packet lands in). First of three slices from the L5-7-D
proposal (`reconciliation/L5-7-D-q9-serving-codex11-2026-11-01.md` §5). Authority:
`AUTH-q9-factorized-preference-2026-11-01.md` (Joe: costs 0/1/2, Joe is the authority) and the
registry `resources/wm/preference-authorizations.edn`.

## Behaviour (pure; no serving-path change in this slice)

1. New namespace `futon2.aif.preference-registry`: `load!` reads
   `resources/wm/preference-authorizations.edn` (classpath; path overridable for tests), refuses
   typed on absence/malformation (`:preference-registry-unconfigured`, `:preference-registry-invalid`),
   and returns the carrier shape the producer validates and the projector reads, in one
   normalized form: `{:schema :wm/factorized-preference-v1 :certificate {...}
   :registries {:calibration [...] :factorization [...]}}` with `:implementation-pin` on each
   registry entry computed from the module's implementation constants
   (`factorized-preference-sut/implementation-pin`), never stored. Resolve L5-7-D §3's shape
   mismatch here: the SUT's `:calibration-authorizations`/`:factorization-authorizations`
   top-level keys and the exporter's `[:registries ...]` are the same data; emit both from one
   value, or change the exporter's reader to the normalized form with a test against the real
   returned carrier (say which you did).
2. Factor out of `factorized_preference_sut.clj`, without changing the controlled wrapper's
   behaviour or any of its negative checks: `validate-certificate!` and the deterministic kernel
   construction (reciprocal masses, stopped cost) into reusable public functions the serving
   path can call in slice I-2. The controlled wrapper calls them; its tests must still pass
   unchanged.
3. `load!` + validation on the shipped registry returns a valid certificate whose kernel is the
   authorized one; the selector authority differs from both authorities; a registry with costs
   out of order, a missing receipt, an authority equal to the selector, or a changed
   implementation pin refuses with the existing kinds.

## Tests

- Shipped registry loads and validates; kernel for costs 0/1/2 equals the controlled test's.
- Each refusal above, constructed.
- `factorized_preference_sut_test` and `cascade_efe_sut_test` unchanged and green.
- Hermetic root; counts vs main for named + projector namespaces.

## Gates

kondo, parens, no reload, no merge, no `data/` writes. Branch `codex6/l5-7-i-1`. Bell claude-2 with
sha and counts. Do not wire the serving scorer in this slice (I-2) and do not touch the exporter's
Lean witness (I-3).
