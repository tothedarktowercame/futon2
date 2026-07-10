# Spec: substrate-2 access consistency for the A3 provable witness

**Owner/reviewer:** claude-4. **Blocks:** the A3 provable-match witness (endpoints
must be WRITTEN to substrate-2 and PROVEN via a query against the *same* node).

## The problem — two symptoms of one thing: inconsistent substrate-2 access

1. **Drawbridge :6768 forbids some forms.** `slurp`/`load-file` → **"forbidden"**
   (this killed a live builder's `:drawbridge` witnesses). But `xtdb.api/q` count
   queries DO work (the census `catalog_census.bb` uses them via :6768). The
   allowed/forbidden surface is undocumented.
2. **:7071 HTTP read API is blind (futon1a#6).** `GET /api/alpha/{census,
   entities/latest,hyperedges}` return empty for ALL types while the store is
   populated (Drawbridge :6768 sees 14/14 HEADs, 31 `:capability` entities, etc.).
3. **Unknown:** does `POST :7071 /api/alpha/{entity,hyperedge}` write to the node
   Drawbridge reads, or to the same blind node its reads hit? If the latter, a
   written endpoint can never be witnessed (write node A, prove node B).

## Goal / acceptance = the canonical smoke test passes

A documented, sanctioned, CONSISTENT round-trip:
- **WRITE** a typed entity + hyperedge to substrate-2, and
- **PROVE** it exists via a Drawbridge `xtdb.api/q` query,
- landing in the **same node** (write and proof agree).

**Smoke test (the acceptance):** create a `:capability/*` hyperedge over a real
`:capability` entity node and a `:mission/doc` node (or, if safer, a clearly-marked
throwaway `:a3-smoketest` entity), then prove it via an `xtdb.api/q` query on :6768.
Round-trips ⇒ pass. Report the exact sanctioned WRITE form and READ/PROOF form the
actuator should use.

## Preferred approach (least blast radius)

**Prefer a Drawbridge-only path**: write via `xtdb.api/submit-tx` (+ `await-tx`)
and read via `xtdb.api/q`, both on :6768 — same node, structurally consistent, and
it touches NOTHING on :7071. First determine whether Drawbridge permits
`submit-tx`; if yes, this is the whole answer and no :7071 change is needed.

Only if writes MUST go through :7071 (e.g. canonical-id gating requires it): diagnose
the :7071 blind-node binding (futon1a#6) and PROPOSE the fix — but **do NOT restart
:7071 during active work.** It is the canonical-id write-gate; a rebind/restart is a
quiet-window operation to coordinate with Joe, never unilateral.

## Constraints

- Never restart :7071 unilaterally (quiet-window + Joe-coordinated only).
- XTDB is append-only/bitemporal — mark any test writes clearly and evict them after.
- Bell **claude-4** back with: the sanctioned WRITE form, the sanctioned PROOF form,
  the smoke-test result, and whether :7071 needed touching. Update futon1a#6 with the
  write-side findings.
