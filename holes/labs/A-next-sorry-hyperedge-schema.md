# The interface-sorry hyperedge schema (substrate-2 enrichment, Gap F)

**Purpose.** Land the A-next empirical sorries into substrate-2 as **oriented `code/v05/sorry`
hyperedges over real code nodes** — the semantic reasoning-layer substrate-2 currently lacks
(Gap F of the 15-mission audit). A sorry = a **contract** in the contract-based-programming /
*How to Design Programs* sense: not just a type signature, but an **oriented interface + processual
obligations (invariants)** the build must satisfy. DarkTower checks the *design*; the sorry is the
*spec the design fills*.

## Audit finding (step 0, 2026-07-02)
All **23** existing `code/v05/sorry` are the **logged-issue** kind (single self-endpoint, prose
`:sorry/title`, issue-tracker `:status`) — 0 are interface-shaped. They keep their value (retype,
don't delete): tag them `:sorry/facet :logged`; interface-sorries land as `:sorry/facet :interface`.

## Orientation / hyperorientation (Joe, 2026-07-02)
The hyperedge is **directed** (substrate-2 already uses `dir:A→B`), with role-tagged ends — the
3(+1)-pole hyperorientation:

| pole | role | what it is | resolves to |
|---|---|---|---|
| **input** | `:have` | named ns-vars/entities the interface imports | real `code/v05/var`/`namespace` nodes |
| **output** | `:want` | validated features it causes to exist | a node if it exists, else a *target* (in-substrate2 false) |
| **hungry** | `:hungry` | things the mission must build along the way | target features / intermediate nodes |
| codomain | (prop) | the type it is hungry-for | `:sorry/hungry-for` (a signature, not a node) |

The **processual obligations** — the HtDP "inner structure of the build", the invariants (e.g.
invariant-queue's "coverage can't decrease without a demotion event"; Arxana window-position via
Reazon; futon1a layered integrity) — are **not** types; they ride as `:sorry/typed-holes`, each an
`{:at :wanted :discharged-by :status}` obligation. These are the "intermediate steps thought
processually", the part cascade→sorry most under-determines (the open crux).

## Schema

```clojure
{:hx/id       "hx:code/v05/sorry:<repo>-d/sorry/<mission>-<slug>"
 :hx/type     :code/v05/sorry
 ;; endpoints are REAL substrate-2 node ids (this is what wires the sorry INTO the code graph)
 :hx/endpoints ["<repo>-d/<ns>/<have-var>" ... "<want-node-or-target>"]
 :hx/ends     [{:entity-id "<repo>-d/<ns>/<have-var>" :role :have   :in-substrate2 true  :note "..."}
               {:entity-id "<want-node>"              :role :want   :in-substrate2 false :note "target feature"}
               {:entity-id "<hole-target>"            :role :hungry :in-substrate2 false}]
 :hx/props
 {:sorry/facet         :interface                 ; ← discriminator vs :logged (step-0 retype)
  :sorry/state         :empirical                 ; recovered post-hoc (vs :predicted from a fold)
  :sorry/for-mission   "<repo>-d/mission/<id>"    ; canonical id (NOT the doc-leaf)
  :sorry/want-signature "unstick-queue : (a, b) -> FlowingInvariantStream"
  :sorry/hungry-for    "FlowingInvariantStream"
  :sorry/kind          {:predecessor-shape .. :whats-new .. :target-territory .. :fill-mechanism ..}
  :sorry/cascade       [:agency/single-routing-authority ...]   ; the :applied patterns (the query)
  :sorry/typed-holes   [{:at :coverage-ratchet-reconciliation :wanted "..." :discharged-by "557efe4" :status :discharged}
                        {:at :canary-staleness :wanted "..." :discharged-by :open :status :open}]
  :sorry/wiring        "futon6/holes/clean/<mission>.clean.edn"  ; pairs-with-wiring (DarkTower design)
  :sorry/completeness  :complete                  ; ← LLM-CRITIQUE HOOK: :complete | :partial
  :sorry/provenance    {:method "post-hoc doc+code+git+XTDB" :sealed-by ".." :corpus-index N}}}
```

## LLM-critique hooks (R16 fast-loop: GFN blueprint → LLM elaborate + critique)
The fast solution is a **GFN blueprint the LLM then elaborates AND critiques** (Joe, 2026-07-02).
The schema is built so the LLM critic can find gaps by *query*, not re-reading:
- **`:sorry/completeness :partial`** + per-hole `:status :open` → "this sorry isn't fully specified."
- **`:hx/ends … :in-substrate2 false`** → "this endpoint is a *target*, not yet a real node" — a
  **missing-endpoint-in-the-loop** signal.
- **`:sorry/cascade` vs the have-endpoints' namespaces** → the critic can flag a **missing pattern**
  (a have-endpoint whose subsystem has no warranting cascade pattern).
- A `:predicted` sorry (from the GFN blueprint) vs the `:empirical` gold → the critic scores the
  blueprint against ground truth and names what the blueprint missed.

## Landing plan (steps 0.5 + 2 — substrate-2 WRITES, gated)
- **0.5** retype the 23 loggeds → add `:sorry/facet :logged` (one prop each; reversible).
- **2**  land the 15 A-next sorries as the hyperedges above; each `:have`/`:want` endpoint resolved to
  its canonical node id via the audit method; `:want`/`:hungry` targets flagged `:in-substrate2 false`.
- Both mutate the live `:7071` store — execute only via the **canonical-id write gate**
  (E-futon1a-archivist; needs `:source` + `:external-id`), never a casual POST, never a JVM restart.
  Prepare the exact batch as a dry-run EDN first; land on Joe's go.
