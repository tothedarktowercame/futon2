# P-R19-preferences-open — C is a parameter of the machine, not a constant in it

holder: claude-15 · parent: the spine (R5 risk = KL against C) · status: PROPOSED by Joe 2026-08-30 19:20Z, not yet ratified as Lean

## Problem (Joe, verbatim in spirit)
"We want to deploy this in multiple settings with different people with different interests — mathematics, semiotics —
or close it over a company: substitute the company in as a variable unit and run the system there. So we need to write
down what this thing IS without any specific preferences set into it. Preferences get layered in, like an agentic
harness layers a system prompt — but there's a process for doing that; they're not just dumped in. It can get its
preferences from a .edn file, but **who wrote that file?** Better to leave the C vector as an open system that could be
determined in various ways."

## What C is today — three sources, three authors, no declared composition
| layer | where | author | provenance recorded? |
|---|---|---|---|
| static floor: channel ranges | `futon2.aif.preferences/preferences` | hand-set in code | no — the code is the author |
| goal-outcome half, "kept LIVE" | `futon2.aif.c_vector` reading :7071 | derived from the goal/hole corpus, freshness-guarded | partly (corpus signature) |
| mess / incompleteness / 應-voice overlays | `futon6/data/c-vector/c-entries.*.edn` via `c_vector.bb` (last run 06-26) | a script over `mission-wholeness.edn` | yes, per entry (`:provenance {:source …}`) — but the file's own author/date is not in the reader |
| the habit prior (counts of past selections) | `wm-trace` `:habit-prior-state` | learned from the operator's choices | no — enters R14 as g, never named as a preference |

So the machine already has layered preferences with mixed authorship, folded by whichever code path runs; the layering is
*implicit* and the operator is present as C without that being written anywhere (P-supersede-stack-logic-model:
the old model declared the operator's workstreams as C by hand; the new one learns them as habit counts; neither says so).
Lean: `Holes.lean:66` refuses C as "an implementation, not a law … preferences declared per PRAGMATIC vertex only" —
correct as far as it goes, and consistent with this record: the *machine* takes C; it does not contain one.

## The principle
1. **The machine is polymorphic in C.** Its definition (the spine: observation → belief → F → G → select → act) is
   stated for an arbitrary C of the right type. No preference value appears in the definition.
2. **A preference is a layer with an author.** Every source of preference is typed by *how it was determined*, and
   every layer carries provenance: who/what produced it, from what, when, and at what basis (`I_data_current`).
3. **Composition is a process, not a dump.** C in a running instance is an ordered fold of layers under a declared
   composition rule (override / additive / bounded), and the fold is itself recorded — the harness analogy: each layer
   is a system-prompt stratum, and the stack is inspectable.
4. **The unit is substitutable.** A deployment = the machine + a chosen layer stack. Person, company, domain
   (mathematics, semiotics, APM proofs) are different stacks over the same machine. "Close over the operator" is then
   one *kind* of layer (learned-from-operator), not the machine's identity.

## Proposed Lean declarations (for the owner to ratify into `Holes.lean`; tech lead may propose text)
```lean
/-- How a preference layer was determined. Open: new constructors are expected. -/
inductive PreferenceSource
  | operatorDeclared     -- written by a person, on the record
  | learnedFromOperator  -- habit counts, selections folded over time
  | corpusDerived        -- computed from a live store at a basis
  | delegateSupplied     -- a company / a domain's own harness supplied it
  | scriptProduced       -- e.g. c_vector.bb over a named input

structure PreferenceLayer (Outcome : Type) where
  source     : PreferenceSource
  author     : String            -- who or what; never empty
  basis      : String            -- store tx / file sha / date — the I_data_current pin
  prefers    : Outcome → ℝ        -- ln P(o) up to a constant, on this layer alone
  compose    : ℝ → ℝ → ℝ          -- how this layer folds onto the stack below it

/-- C for one deployment is the fold of an ordered layer stack; the machine never holds a C of its own. -/
def foldC (base : Outcome → ℝ) (layers : List (PreferenceLayer Outcome)) : Outcome → ℝ :=
  layers.foldl (fun acc l o => l.compose (acc o) (l.prefers o)) base

-- The spine is stated over an arbitrary C:
-- def expectedFreeEnergy (C : Outcome → ℝ) (π : Policy) : ℝ := risk C π + ambiguity π
```
Holes to add (tagged owner/holder/evidence/falsifier): `preferenceStackRecorded` (every running instance's C is a fold
of layers each with non-empty author and basis; falsifier: a C value in a trace with no layer record), and
`machineHasNoC` (no preference value is free in the spine's definition; falsifier: a constant of Outcome→ℝ in the spine
that is not a parameter).

## Facades this record refuses
- "C is in `c-entries.edn`": a file is a *layer*; its author and date are part of C or it is not admissible.
- "The habit prior is just g": it is a learned preference layer and must be declared as one, or the operator is in the
  machine unrecorded.
- "Define C now so R5 can close": R5's risk term is stated over a parameter; it closes *polymorphically*. Choosing a C
  is a deployment act, not a definition act.

## What this changes in the build
- PREREG §1 R19: "undefined for the WM" → **open by design: a parameter**. R5's `risk` is stated over `C : Outcome → ℝ`.
- The two-π question (`cascadeGrainPi`) is unaffected; the "prior preferences" half of the spine no longer waits on a
  value.
- A new small lane, when Joe says so: R19-D1 — declare the four existing sources as layers with author + basis, and
  make `c_vector.clj`'s fold the recorded `foldC`. No new preferences; just the ones already there, named.

## The R19 tetrahedron (Joe, 19:35Z — ratified: "R19-D1 should proceed", with this amendment)
Naming four sources as layers is the *nouns* vertex only. R19 is a new tetrahedron (lifecycle §0.9 subdivision), and a
list of strata without a purpose is exactly the facade §0.10 warns about — vertices with no mass:

| vertex | R19 content |
|---|---|
| nouns | the layers: source, author, basis, prefers |
| verbs | the fold: composition rule per layer, order, the recorded `foldC` |
| organisation | **the stack as a whole, with its purpose stated at the strata level**: "these N strata are used to model *this* situation" — not "here are four strata, make sense of it" |
| evidence | the evidentiary core: what observations show the stack fits the situation it claims to model, and what would show it does not (the falsifier); gathered into one place with a **handoff** |
| mass (§0.10) | a holder who owns the stack record and can subdivide it (per-layer records) at the next level |

So a preference stack is admissible only with, alongside the list of layers:
- `purpose` — the situation the stack models (e.g. "the futon stack as maintained by one operator, 2026-08");
- `situation-evidence` — the observations that back the claim that these layers, so composed, model that situation;
- `falsifier` — an observation under which the stack would be the wrong model of the situation;
- `holder` / `parent` — the handoff fields (§0.10).
Substituting the unit (a company, a domain) = a new stack record with a new purpose, not a new list of files.
`PreferenceStack` is therefore the Lean type to add beside `PreferenceLayer`:
```lean
structure PreferenceStack (Outcome : Type) where
  layers    : List (PreferenceLayer Outcome)
  purpose   : String        -- the situation modelled, stated at the strata level
  evidence  : List String   -- refs into the evidence landscape backing the fit claim
  falsifier : String        -- what would show this stack is the wrong model
  holder    : String
```
R19-D1 is the DISCOVERY half: the stack record for today's machine — its four layers named with author + basis, and its
purpose / evidence / falsifier stated honestly (including "unknown" where that is the truth). R19-D2 (after review) is
the implementation half: Lean declarations ratified, `c_vector.clj`'s fold made the recorded one.

## R19-D1 GATED (owner, 2026-08-30 21:20Z) — passed; the record corrected this record
`dc1dac8` (packet folded claude-13's habit-prior seat first, `0a03af00`): `R19-preference-stack.edn` + 15-line findings.
Verified at the gate: both basis sha256s re-hashed and match (preferences.clj `22ae618a…`, wm-trace `6da3ccda…`);
cited lines read (`efe.clj:586-614` zone-risk; `war_machine.clj:247-268` the 07-13 flip with both env hatches;
`policy.clj:368-380`; `habit_prior.clj:121-136`; `c_vector.clj:227-240,633-640` — 640 lines, real); `:declared-purpose
nil` with a 4-item search — the refusal held; `:observed-purpose` is about the fold; `:fit-status :witnessed` with 5 refs.

**Corrections to this record's own table (the D1 finding):**
1. **Five sources, four folded — not four/three.** Missing row: `capability-zone-load`
   (`preferences.clj:140-173` → `efe.clj:586-614`, `g-risk (+ channel-risk zone-risk)`): a `pref/c-distribution`
   parameterised by the LEARNED `:load-weight` — author class `:learned-from-operator` (posterior evidence mass,
   242 hyperparameter-update records, max as-of 2026-07-18), not hand-set.
2. **The habit prior is live in the selection seam, not dark.** The `:controller-augmentation (DEFAULT)` /
   `:habit-prior (DARK)` labels at `efe.clj:698-706` are STALE — `war_machine.clj:247-268`: flipped live by Joe
   2026-07-13; today's defaults are `:habit-prior` mode and `:learned-frequency` source, env vars are rollback
   hatches. So the operator's learned preference is in Q(π) right now — stronger than this record's original
   "present as C without being written anywhere". (Follow-up for D2's builder: fix the stale comment.)

**Naming settled (three names, one object):** the learned **ln E(π) habit prior at the policy seam**. It is consumed
by `policy/select-action` (R6/SELECT); R14 contributes the temperature τ that divides G beside it
(Q(π) ∝ exp(ln E(π) − G(π)/τ), `Holes.lean:493-499`; `CommitmentTemperature.lean` models the same comparison and
introduces no second prior); the "R12" in efe.clj's comment is stale labelling from before the flip. Records say:
enters-at = the policy seam (R6), scaled against R14's τ; not an R12 object.

**R19-D2 (proceeding):** owner ratifies `PreferenceSource`/`PreferenceLayer`/`PreferenceStack` into Holes.lean with
the habit prior connected to the EXISTING softmax hole's `habit` argument (no new name); then the implementation
packet: c_vector's fold becomes the recorded `foldC` with the five layers' authors/bases carried, stale comment fixed.

## R19-D2 decomposition (ruled 2026-08-30 23:00Z, on claude-20's scope catch)
My (a) said "the fold is in c_vector.clj"; the gated record's `:composition-order` says 3 of 4 folded layers compose in
`efe.clj` (`g-risk (+ channel-risk zone-risk)` …), and c_vector.clj is a layer *source*. Ruled: **D2a** — efe.clj's
C-risk composition becomes the recorded `foldC`, emitting the stack record beside C (all five layers, habit prior
`folded=false` with enters-at, so the trace cites the whole stack); the stale 698-706 comment fix rides in D2a.
**D2a′** — c_vector.clj entries carry `{author basis}` (parallel, different holder). **D2b** — the witness for
`preferenceStackLiveRecorded`, after D2a. The tech lead verified the Lean fixture matches `dc1dac8` exactly before
proposing — nothing to correct on the interface.
