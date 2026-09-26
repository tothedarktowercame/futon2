# PROOF-2a: the plan, as a structured proof

*Beside `PROOF-2a-THEOREM-draft-2026-09-24.md`, which states the theorem. This file is the plan to reach the state in which the theorem can be witnessed, written in Lamport's structured style: numbered steps, each a claim with a checkable acceptance, its dependencies, and its dispatch log. Every dispatch is entered under its step before it goes out; every return updates the step's STATUS and log line. Joe, 2026-09-26: the proof is to be understandable from this summary, and dispatches are to be logged into it, not to occur reactively.*

**Conventions.** `⟨n⟩m` is step m at depth n. `ACCEPT:` a predicate someone can check from the record. `DEPENDS:` steps that must be ACCEPTED first. `PAR:` what the step parallelises over, and which seats may take it (Claude seats for map and prover work; Kimi seats, one unique task each via `futon3c/scripts/kimi-task.sh` with a test-registry or `lake build` warrant as acceptance, for per-row, per-edge and per-lane work). `STATUS:` one of PENDING, IN FLIGHT, ACCEPTED, with the date. `LOG:` packet · seat · job · outcome · shas.

**THEOREM** (PROOF-2a, unchanged): the machine, on a lifecycle-shaped mission chosen by its own selection (T), constructs an ordinary, semantically nontrivial field (0), enacts the chosen candidate (C), and satisfies clauses 1–6 on the resulting record.

**PREMISE, corrected 2026-09-26.** The plan had assumed the components were ready and only needed wiring. The wiring map (M-wm-wiring), its adjacency matrix, the registry refreshed to this proof's equations, the Lean dependency map, and the typeset Figure 6A together show otherwise: equations exist as prose, dependencies the registry asserts are not imported in Lean, and no wire past selection has ever carried a value live. Joe's order: components, then wires, then the empirical questions. No flight runs before ⟨1⟩ and ⟨2⟩ are ACCEPTED.

---

## ⟨1⟩ COMPONENTS. Every term the theorem's machine uses has a definition, and the definition is written down.

### ⟨1⟩1. Every registry row carries its equation, or a typed absence.
ACCEPT: `gen_aif_dag.bb`'s census reads N `[latex]`, 0 translated, 0 abridged, M noted, with M equal to the rows carrying `:latex-absent`; no `:formal` changed.
DEPENDS: none. PAR: none (one editor of the registry).
STATUS: IN FLIGHT 2026-09-26.
LOG: WM-REGISTRY-LATEX-I · claude-3 · invoke-…69e33771 · in flight. Prior: WM-EQUATIONS-DELTA-D (a0b6f4a5) and WM-EQUATIONS-APPLY-I (futon2 9c8b3f33, p4ng 791c9f8) refreshed the registry to 29 equations, 37 edges; WM-DAG-NODES-I (p4ng afbc95b) let nodes be declared in data; WM-DAG-EQUATIONS-I (p4ng b217f3a, 1f25f92) typeset the boxes and found 12 of 29 rows prose-only.

### ⟨1⟩2. Every `:latex-absent` row is either written in Lean or ruled not an equation.
ACCEPT: for each row from ⟨1⟩1, either a Lean declaration exists that the row's `:lean` names and `gen_lean_dag.bb` places (with a `lake build` warrant), or the row is reclassified to `:plumbing` with the reason (as the grain gate was: a support restriction, not an equation). Expected rows: `:mission-preference` (C's reading rule), `:enactment-habit` (E's counting rule), `:target-universe`, plus whatever ⟨2⟩1a classes as not-written.
DEPENDS: ⟨1⟩1, ⟨2⟩1a. PAR: per row; Kimi-able (one E-kimi-task per row, `lake build` warrant), Lean layer reviewed by claude-8. Rows from ⟨2⟩1c: C1–C4 (above), plus ⟨1⟩1's typed absences when they land.
STATUS: PENDING; HELD for Joe's look at the list (12:44Z).
LOG: —

### ⟨1⟩3. The Holes table has no open row.
ACCEPT: every row of PROOF-2a's Holes table is CLOSED or has a landed packet with a warrant; specifically H-G-target part 2 (the Lean module's own statement says it is owed), H-C-reach (the served-by rule beyond one mission), the W_c checker's site (`:wc` is `:node nil` in the registry because the map's `:wc-checker` has no site), and H-publish's implementation.
DEPENDS: none. PAR: per row; Claude seats (claude-13 held H-E and H-C-reach; claude-10 the flight side).
STATUS: PENDING (partial: H-E, H-A, H-C, H-order, H-value, H-witness(i) closed earlier).
LOG: —

### ⟨1⟩4. QED for ⟨1⟩: by ⟨1⟩1–⟨1⟩3.

---

## ⟨2⟩ WIRES. Every dependency the equations assert is carried, at three levels, and each carrier is shown to carry something.

### ⟨2⟩1. Lean level: every registry edge is `:imported` in `gen_lean_dag.bb`'s classification.
At 2026-09-26 (p4ng e22c72b): 16 imported, 10 present-not-imported (free parameters), 11 absent, of 37.

#### ⟨2⟩1a. The 21 unimported edges are sorted into name-mismatch, grain, not-written, instantiation.
ACCEPT: `WM-LEAN-ABSENT-TRIAGE-D.md` exists with all 21 classified with declaration-level evidence and a packet list.
DEPENDS: none. STATUS: ACCEPTED 2026-09-26 ~12:44Z.
LOG: WM-LEAN-ABSENT-TRIAGE-D · claude-11 · invoke-…a9407410 · futon2 57849fb9 · 10 instantiation, 5 grain, 3 name-mismatch, 3 not-written. Findings that change the plan: the model's convention agrees with the instantiation reading (each equation is parametric, the machine's value is a separate `machine*` declaration, nothing applies one to the other); E does NOT need u (the registry's R16→R17 import is wrong: the habit counts the enacted candidate with the W_c verdict), so that edge is a registry correction and the counting rule a component; three instantiations are not plain substitutions (`machineTemperature` returns `Except`, so the error arm is a typed absence; `IsBayesAction` is a Prop, so the action needs a chosen maximiser; R2→R3a's two sides carry `o` as different types, needing a stated map first). Prior: WM-LEAN-DEP-MAP-I (p4ng 40a1240), WM-LEAN-VS-REGISTRY-I (p4ng 33d9a72, e22c72b).

#### ⟨2⟩1b. Registry corrections: the name-mismatch and grain rows.
ACCEPT: after the edit, `gen_lean_dag.bb` classes each such edge `:imported` or `:present` under the corrected `:lean-term` / `:enters-through`, and no other edge changes class.
DEPENDS: ⟨2⟩1a; G0 (the generator must read the hints first); ⟨1⟩1 (one registry editor at a time). PAR: none. Seat: claude-11 for G0, claude-3 for RC1–RC6.
STATUS: IN FLIGHT (G0).
LOG: G0 · claude-11 · p4ng c54b00e · ACCEPTED 12:46Z: `:lean-term {sym "binder"}` counts a binder of that name as the term present; `:enters-through {sym carrier}` counts the term present only if the carrier is a binder or section variable, else marks `:enters-through-unverified` so a wrong hint shows rather than hides; labels on the figure; 7 fixture cases, planted bug fails; counts unchanged 16/10/11 and outputs byte-identical to e22c72b. Limit passed to RC: a `:lean` on an exogenous symbol is not read as its carrier, so R2→R4 (interp) turns orange under RC3, not blue; blue needs `:interp` placed at a module (a further generator change, G1, if wanted). G0 was dispatched (12:44Z). RC1–RC6 (the triage's §1: three name-mismatch, five grain, and R16→R17 struck as a wrong import, with `:observe`'s `world` noted as `sPrev`) · claude-3 · queued behind ⟨1⟩1.

#### ⟨2⟩1c. Not-written edges become ⟨1⟩2 rows.
ACCEPT: each not-written edge names the ⟨1⟩2 row that writes it. DEPENDS: ⟨2⟩1a. STATUS: ACCEPTED 12:44Z: the triage names C1 the containment order built from the patterns (R2→R6 interp), C2 adjudication rates counted from records (R2→R7), C3 the β_ζ update of the likelihood precision, C4 the habit prior counted from enactment records (`:enactment-habit`'s `:lean` names only a type). These are ⟨1⟩2's rows. LOG: as ⟨2⟩1a.

#### ⟨2⟩1d. Instantiation: for each present-not-imported edge, a Lean definition instantiating the parametric declaration at the registry's source term (e.g. the policy posterior at `machineTemperature`; two edges into `softmaxWithFPi` share one module).
ACCEPT: the edge is `:imported`; the figure's orange arrow is blue; `lake build` warrant per module.
DEPENDS: ⟨2⟩1a, ⟨2⟩1b. PAR: per module (6): W1 one module for R14→R6 and R8→R6 (both instantiate `softmaxWithFPi`; the temperature's `Except` arm carried as a typed absence); W2 the action from that posterior (needs a chosen maximiser, `IsBayesAction` being a Prop); W3 the closed-loop belief trajectory, four edges; W4 the rollout from the current belief (finer declaration: the rollout starts from the model's `q₀`, and nothing puts the current belief there); W5 the state prediction error; W6 the sensory prediction error, after a discovery settles the carrier of `o` (channel vector vs token observation). Each W re-points the row's `:lean` in the same commit. Kimi-able with a `lake build` warrant, reviewed by claude-8.
STATUS: PENDING; HELD for Joe's look at this list (12:44Z). LOG: —

#### ⟨2⟩1e. QED for ⟨2⟩1: figure `aif-lean-dag-nodes.svg` has no orange or red arrow.

### ⟨2⟩2. Map level: the wiring map is complete.

#### ⟨2⟩2a. Every step boundary is crossed by a declared field with a reader at the step itself, and every carrier is a box.
At 2026-09-26 (futon3c dd0c145f): boundaries 3→4 and 4→5 crossed with readers at the step; islands: lane 6 (rates). Remaining hops in order: H4 ask → store (`:interpretations`), H5 view → construction (`:universes`), H8 assembly `:sources` → `assemble-one`'s `:want` (where C enters risk; the registry's R19→R5), H6 construction → candidate (`:construction-receipt`), then the rates lane. H7 is covered by H3.
ACCEPT: the harness figure's hand-off strip has no "no declared hand-off"; each crossing lists a reader in the entered lane; no island lane.
DEPENDS: none. PAR: none (one editor of the map file); seat claude-10, linear.
STATUS: IN FLIGHT.
LOG: H1 · claude-10 · b9d18dfe, cfc355a0, e31c059d · declared, boundary not crossed (the read step publishes to a store). H2 · claude-10 · b44b63ab, 8dc61b63, 73e335e0 · 3→4 crossed by six store keys. H3 · claude-10 · 5927020d, 5d71e012, 2d4eeeb6 · 4→5 crossed by `:wants` `:universe`. H4 · claude-10 · dd0c145f, dcd55083, 480bd170 · 4→5 gains `:interpretations` with both readers in lane 5; the one writer is `merge-published` (neither textual write in `want_interpretation.clj` is a publish: `:272` is a trial assembly inside `validate-response`, `:473` reads the store back into the sources); `target-view` not boxed (it calls `merge-published`, its form has no `:interpretations`); `:r0-enact-step` reads the store's `:patterns` under an option, not this key; 263 findings (3 to-do), 111 wires, ledger 0/2/109 (12:46Z). H5 · claude-10 · 3597ba36, 6b084cb8, a716e024 · `:universes` from `flight-assembly-input` to `assemble-one`, both lane 5, no boundary (the universe's crossing was H3's); 264 findings (3 to-do), 112 wires, ledger 0/2/110 (12:52Z). Correction to H4 on the record: `merge-published` is the one writer only within `want_interpretation.clj`; the sources' `:interpretations`, `:wants` and `:universes` are also written earlier in `judge` by `cascade_sources/load-declared` and `mission_hole_wants/merge-into-sources`, and the flight's merge runs last. H8 HELD: the sources' `:wants` shares its key with the click's `:wants` on a different record, and the prover keys writers by field name alone (`wiring.clj:77-81` refuses two writers). Settled in lane as a prover schema change, not a code change: WM-PROVER-RECORD-SCOPE-I · claude-2 · futon3c 5e4d9d02 · ACCEPTED 12:58Z: `[field {:record r}]` is the vertex `[field r]` in `wiring.clj` and the port `field@r` in `ct_projection.clj`; attribution only under a `get-in`/`assoc-in`/`update-in` path starting with the record's key or with the record's name as receiver (a receiver threaded through `->` is not seen); 8 new tests (same key on two records accepted; twice on one record refused naming `[:wants :sources]`; occurrence outside the record not attributed), the 14 diagramprover namespaces 100/513 green with the map and fixture byte-identical; warrants for the new test and the map test at 5e4d9d02. Open for H8: `trace-findings` in the map test compares raw entries with `set` and must learn the scoped form. H6 (`:construction-receipt`) + the rates island · claude-10 · 13f41d4e · in flight (12:52Z). H8 · queued behind H6 (claude-10 in flight): `[:wants {:record :sources}]` on `:tick-flight-assembly`, reader `assemble-one`, and `trace-findings` taught the scoped form in the same commit.

#### ⟨2⟩2b. The map is a strict superset of Figure 6A: every DAG edge is declared at function grain.
At 2026-09-26: `spike/wm_vs_equation_dag.bb` reports declared 2, boxed-no-field 11, unboxed 24 of 37.
ACCEPT: the join reports 37 declared, 0 unboxed at var grain.
DEPENDS: ⟨2⟩2a (the carriers), ⟨1⟩1 (the `:code` sites). PAR: per R-node, i.e. per block of the matrix (13 nodes); discovery per node Kimi-able (which `:code` site writes which DAG symbol under which key), the map edit by a Claude seat one at a time.
STATUS: PENDING. LOG: —

#### ⟨2⟩2c. The organisation layer is drawn: the step order, the carriers, and which box calls which, from the source, as a third strip of the harness figure.
ACCEPT: the strip is generated by `wm_wiring_svg.bb` from a call-order discovery with every edge cited to a call site.
DEPENDS: ⟨2⟩2a. PAR: none. Seat: a Claude seat for the discovery, then the generator.
STATUS: PENDING. LOG: —

### ⟨2⟩3. Every matrix entry has a first-layer wire test: a registered test shows something is sent (present, not a typed absence, the writer's).
At 2026-09-26 (futon3c 2d4eeeb6): 0 verified, 2 witnessed hermetically, 107 unverified, of 109.
ACCEPT: `wm-wire-ledger.edn` reports 0 unverified; every entry is verified or witnessed hermetically with a named test.
DEPENDS: within-lane entries, none (start now); hand-off entries, ⟨2⟩2a. PAR: per block diagonal of the matrix, one lane per seat; Kimi-able (one E-kimi-task per lane: write the wire tests for that lane's entries, register them in `wire-test-nses`, warrant them), the definition already fixed in `wm-wire-ledger-test`'s docstring (WM-WIRE-TEST-I, 042a0570).
STATUS: IN FLIGHT (definition and ledger ACCEPTED; two prototypes witnessed; the within-lane blocks: 78 wires in 11 lanes, the first lane out as calibration).
LOG: E-kimi-task-55 · kimi-1 · invoke-…22e4fc80 · lane 3 read step, 5 wires · in flight (12:44Z). WM-WIRE-TEST-I · claude-10 · 042a0570 · definition, ledger, two wires hermetic; found and fixed via WM-CHOSEN-CANDIDATE-I (futon2 f69f103d) and WM-HABIT-FOLD-CALL-I (futon2 663caa7b).

### ⟨2⟩4. QED for ⟨2⟩: by ⟨2⟩1e, ⟨2⟩2a–c, ⟨2⟩3. This is "the map is complete" in the mission's four criteria.

---

## ⟨3⟩ EMPIRICAL. Under the complete map, one flight is a test of the map, not a search for defects.

### ⟨3⟩1. One flight, and every wire's hermetic witness is promoted to a live pin from its record.
ACCEPT: the ledger reports verified = wires, or the flight's first defect is typed as a ⟨1⟩ or ⟨2⟩ finding and sent back to its step (not fixed in place), and the flight is re-run only after that step is re-ACCEPTED.
DEPENDS: ⟨1⟩4, ⟨2⟩4. STATUS: PENDING. LOG: — (eight flights ran before this plan; each stopped at its first defect; their findings are in M-wm-wiring.)

### ⟨3⟩2. The operating parameters are set per instance: clicks per flight, focus versus reselection, with Joe's stated preference (focused work to completion) as the default.
ACCEPT: the instance's record states the parameter and its outcome. DEPENDS: ⟨3⟩1. STATUS: PENDING. LOG: —

### ⟨3⟩3. QED for the THEOREM: clauses T, 0, C and 1–6 are each witnessed on the flight record of ⟨3⟩1, with the wire ledger as the record that every dependency carried a value.
DEPENDS: ⟨3⟩1. STATUS: PENDING.

---

## Order of execution, read off the DEPENDS lines

Now, in parallel: ⟨1⟩1 (claude-3), ⟨2⟩1a (claude-11, landing), ⟨2⟩2a H4 (claude-10), ⟨2⟩3 within-lane wire tests (Kimi, one lane each: lanes 1, 2, 7, 8, 9, 10 have entries), ⟨1⟩3 Holes rows (Claude seats).
Then: ⟨2⟩1b (claude-3, after ⟨2⟩1a), ⟨1⟩2 and ⟨2⟩1d (Kimi per row / per edge, after ⟨1⟩1 and ⟨2⟩1b), ⟨2⟩2a H5, H8, H6, rates (claude-10, linear).
Then: ⟨2⟩2b (per node, after ⟨2⟩2a), ⟨2⟩2c, ⟨2⟩3 hand-off entries.
Then: ⟨3⟩.
