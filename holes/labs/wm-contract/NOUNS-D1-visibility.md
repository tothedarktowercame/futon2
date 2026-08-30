# NOUNS-D1 — cascade / typed hole / wiring diagram visibility

Discovery only, 2026-08-30. Status means: **used** = the site constructs,
checks, or consumes the carrier; **mentioned-only** = prose or a weaker namesake
is present but the new carrier is not operated on; **absent** = the named search
found no occurrence. I reused the prior cascade census rather than recounting
its consumers (`futon2/holes/labs/wm-contract/NOTE-cascade-consumers-census.md:16-23`).

## Cascade

| site | status | observation |
|---|---|---|
| Paper | **used** | The current paper defines a cascade as a staged, priced, checked and folded pattern composition (`p4ng/sec-glossary.tex:33`), then strengthens it to a scored semilattice with sequential and co-application structure (`p4ng/sec-glossary.tex:48`). This is the new specification, not the older “try one pattern” sense. `git show 9b9c5bd:sec-glossary.tex` found the same definitions at lines 33 and 48 in the earliest split PLoP commit I could inspect, so I found no within-`plop-2026` before/after delta; the older-paper contrast is historical prose, not a recoverable edit in this file history. |
| Tick path | **used** | The scheduled path calls `generate-war-machine`, then conditionally calls `enact/close-loop!` only when `live-wire?` is true (`futon2/scripts/wm_scheduled_run.clj:98-109`). That call builds `cascade-lane` entries (`futon2/src/futon2/aif/enact.clj:131-145`), whose live records carry score, shown patterns and semilattice (`futon2/scripts/futon2/report/cascade_lane.clj:390-418`). Important boundary: the newer one-shot invokes only `generate-war-machine` (`futon2/scripts/futon2/run_tick_once.clj:194-212`); it assembles the traced route (`futon2/scripts/futon2/run_tick_once.clj:141-164`) but does **not** invoke `close-loop!`. |
| Fold/diffsub | **used** | This is the fullest operational carrier: cascade-lane writes entries and close-loop/enact/fold readers consume them (`futon2/holes/labs/wm-contract/NOTE-cascade-consumers-census.md:16-23`). The archived Psi flights contain scored constructions, e.g. three boxes, four holes and explicit seq/coproduct wires (`futon2/holes/overnight-flights-2026-07-06.md:48-58`). The flight note's last commit is `284a0f5` (2026-07-14); the cited futon6 mission-fold implementation's last commit is `be47eb6` (2026-06-08), so this rich artifact world predates the present tick wiring. |
| Lean/data | **used** | Lean has an explicit `Cascade` carrier with nodes, authored additions, edges, acyclicity and precedence (`mathlib4/DarkTower/WarMachine/Holes.lean:27-33`), plus meet/descent vocabulary (`mathlib4/DarkTower/WarMachine/CascadeOrder.lean:17-32`). It deliberately models no wiring or fold behavior (`mathlib4/DarkTower/WarMachine/CascadeOrder.lean:1-12`). The prior census found this theory carrier consumed only by the snatch microcosm, not by the live cascade lane (`futon2/holes/labs/wm-contract/NOTE-cascade-consumers-census.md:19-20,25-32`). |

## Typed hole

| site | status | observation |
|---|---|---|
| Paper | **mentioned-only** | The paper says a fold surfaces `policy-holes` in its wiring (`p4ng/sec-glossary.tex:64`) and once calls prose-bearing `:sorry`/`:want` records “typed holes” (`p4ng/sec-operator.tex:384-387`). It does not give those fold holes a type plus discharge predicate; the separate evaluation discipline gives *unverified claims* discharge conditions (`p4ng/sec-evaluation-outline.tex:26-32`). The two ideas are described, not joined into one carrier. |
| Tick path | **used** (weaker carrier) | When armed, the scheduled path shells the deterministic fold engine and reads `:policy-holes` beside `:boxes` (`futon2/src/futon2/aif/enact.clj:113-129`); the trace audit retains only their count (`futon2/src/futon2/aif/enact.clj:205-232`). Thus holes affect coverage and are not fabricated away, but their live shape is an unfolded-pattern record, not a typed obligation with a discharge condition. The one-shot runner does not reach this producer (`futon2/scripts/futon2/run_tick_once.clj:194-212`). |
| Fold/diffsub | **used** | Psi flight records name individual holes `h1…hN`, state what remains undecided, and keep them in the coverage denominator; one example records `h1` and the other holes plus explicit wires (`futon2/holes/overnight-flights-2026-07-06.md:48-58`). The deterministic implementation surfaces every rule-table miss as `{:unfolded-pattern ...}` rather than a box (`futon3a/holes/labs/M-memes-arrows/fold_engine.clj:105-119`). This is honest structured absence, but not yet the richer theorem-hole type. |
| Lean/data | **used** (different carrier) | `Holes.lean` is a registry of declarations whose comments record owner, evidence and falsifier/discharge state; for example `cascadeGrainPi` remains a typed unresolved declaration because two policy grains conflict (`mathlib4/DarkTower/WarMachine/Holes.lean:505-509`). I found no Lean type that identifies these declaration holes with fold `:policy-holes`; the formal `Cascade` itself contains no holes (`mathlib4/DarkTower/WarMachine/Holes.lean:27-33`). |

## Wiring diagram

| site | status | observation |
|---|---|---|
| Paper | **used** | The paper defines a fold literally as boxes, wires, terminals and policy-holes (`p4ng/sec-glossary.tex:64`), and says the cascade's co-application structure is what folds into that diagram (`p4ng/sec-glossary.tex:48`). This is the new boxes+holes+wires sense, not generic “system wiring.” |
| Tick path | **used** | Under `live-wire?`, `close-loop!` admits only a cascade with both a positive cascade score and negative coverage delta (`futon2/src/futon2/aif/close_loop.clj:100-116`), then `engine-wiring` returns the fold engine's `:boxes`/`:policy-holes` wiring (`futon2/src/futon2/aif/enact.clj:113-129`). The result is artifact-only: no substrate write or outward action (`futon2/src/futon2/aif/enact.clj:12-16`). Separately, the one-shot records a route and checks adjacent hops against the drawn control map (`futon2/scripts/futon2/run_tick_once.clj:141-164`); that is control wiring, not the fold diagram inserted into a typed hole. |
| Fold/diffsub | **used** | The July Psi artifacts are literal diagrams with `b1…bN`, `h1…hN`, and typed `:seq`/`:copar`/`:tensor` wires (`futon2/holes/overnight-flights-2026-07-06.md:130-145`). Their current live consumer is indirect: enact shells the futon3a fold engine over the selected cascade (`futon2/src/futon2/aif/enact.clj:113-128`), rather than loading these July diagrams as the running plan. |
| Lean/data | **used** (two distinct carriers) | Lean formalizes the observed route as `RouteHop` and leaves route conformance as a hole (`mathlib4/DarkTower/WarMachine/Holes.lean:583-592`). The drawn control wiring is executable data (`p4ng/empirics-futon/control-map-edges.edn:1-12`) and the one-shot reads it (`futon2/scripts/futon2/run_tick_once.clj:134-164`); typed delivery ports live in the separate hyper-edge schema (`p4ng/empirics-futon/hyper-edge-schema.edn:1-11`). None is the fold engine's boxes/holes/wires type. |

## Verdict

The PLoP-2026 machine **runs a partial cascade → fold-wiring chain, not a unified cascade → typed-hole → wiring-diagram architecture**. Its strongest live chain is the scheduled runner's `live-wire?` branch (`futon2/scripts/wm_scheduled_run.clj:98-109`) → scored/semilattice cascade entries (`futon2/scripts/futon2/report/cascade_lane.clj:390-418`) → the two-leg act gate (`futon2/src/futon2/aif/close_loop.clj:100-116`) → artifact-only boxes and policy-holes (`futon2/src/futon2/aif/enact.clj:113-129`). The biggest gap is that the paper gives the fold a boxes+wires+terminals+holes specification (`p4ng/sec-glossary.tex:64`), while the running trace retains only box/hole counts (`futon2/src/futon2/aif/enact.clj:222-232`), the one-shot nine-hop route never calls the fold/enact path (`futon2/scripts/futon2/run_tick_once.clj:194-212`), and Lean's cascade, theorem-hole registry, and route wiring remain three separate carriers (`mathlib4/DarkTower/WarMachine/Holes.lean:27-33,505-509,583-592`). The machine therefore operates pieces of all three nouns, but it does not pass one typed object through all three meanings.

## Counts, instruments, and refusals

- Cell counts: cascade **4 used / 0 mentioned-only / 0 absent**; typed hole
  **3 used / 1 mentioned-only / 0 absent**; wiring diagram **4 used / 0
  mentioned-only / 0 absent**. Qualifiers in the status cells are material:
  “used” does not assert that similarly named carriers are identical.
- Searches used `rg -n -i 'cascade|typed[- ]hole|wiring diagram|policy-hole|wiring|discharge condition'`
  over the paper inputs; `rg` over the named tick/fold repositories; and direct
  numbered reads with `nl -ba`. Negative claims are limited to those searched
  paths and names.
- Refusal: I do not call fold `:policy-holes` instances of Lean theorem holes,
  because no adapter or shared type was found. I do not call the route tracer a
  fold wiring diagram, because it records node hops rather than boxes and holes.
  I do not claim the one-shot runs enactment: its body contains no such call.
