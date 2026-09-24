# D14-HORIZON-D — where the judge's horizon comes from, and who reads it

Discovery packet for E-cascade-real D14 gap 7, feeding claude-10's D16.
Read-only: no code, no clicks, no writes under `data/`. All line numbers at
futon2 `main` HEAD `cc831860` ("A-S/H-A: measured-rates over the exemplar
check ledger").

## 1. Producers and consumers of a horizon on the tick path

| # | site | file:line at HEAD | produces / consumes | value source | what changes if it were 4 instead of 2 |
|---|------|-------------------|---------------------|--------------|------------------------------------------|
| 1 | source declaration | `resources/wm/cascade-sources/T-repair-occ-444fb018.edn:31` | **produces** `:horizon-steps 4` | declared 4 (longest candidate path C2: declare→collect→calibrate→accept, file comment :23–30). The other four source files (`M-aif-policy-conditioned-eig`, `M-expressions-of-interest`, `M-f11-find-production-successor`, `M-wm-08-external-f2`) declare **nothing** — a typed absence | — (this is the 4) |
| 2 | declaration validation | `src/futon2/aif/cascade_sources.clj:114–118` | produces typed refusal `:invalid-horizon-steps` on non-pos-int | declared value | unchanged |
| 3 | the lift | `src/futon2/aif/cascade_sources.clj:273–297` (landed `1de6aadc`, on main) | **produces** merged `:horizon-steps` = MAX of per-file declarations, plus `:horizon-steps-declarations` provenance; key stays **absent** when nothing declares | declared 4 (when any file declares) | with T-repair declaring, merged = 4 for ALL targets, not per-target |
| 4 | **the judge's horizon binding** | `scripts/futon2/report/war_machine.clj:7059–7063` | **produces** `cascade-horizon {:value … :authority …}` | `(:horizon-steps cascade-sources)` (the lift) **else literal fallback `{:value 2 :authority "p4ng 462aa79 (Joe 2026-09-17: initial T=2)"}`** — this is the D14 gap-7 site | at 4: every consumer below sees 4; at 2 (fallback): a silent substituted value, not a typed absence |
| 5 | stamping into assemble | `war_machine.clj:7084` | produces `:horizon-steps (:value cascade-horizon)` onto the sources map handed to `cascade-problems/assemble` | whatever site 4 resolved | stamps 4 vs 2 onto every problem |
| 6 | assemble's absence gate | `src/futon2/aif/cascade_problems.clj:258–267` | **consumes**; `nil? :horizon-steps` → refuses **ALL** targets `:horizon-not-declared`. Never substitutes | stamped value from site 5 | unchanged (gate is value-agnostic); this is the typed-absence layer the site-4 fallback pre-empts |
| 7 | problem construction | `cascade_problems.clj:112–123` (`:horizon` threaded), `:215`, `:251` (`:horizon-steps T` on the problem) | consumes/stamps per problem | stamped value | each `:cascade-problem` carries 4 vs 2 |
| 8 | cascade-lane R13 | `war_machine.clj:5816–5828` | **consumes** `(:horizon-steps problem)` via `policy-depth/configured`; non-pos-int → typed stop `:missing-common-horizon`, "no default" (docstring 5749–5784) | problem's stamped T | lane refuses vs runs; no fallback here — the lane is honest, the judge binding at site 4 is not |
| 9 | R4 multi-horizon prediction | `war_machine.clj:5831–5883` (`fm/predict-multi-horizon`, `:horizon-steps` from `[:R13 :cascade-rollout]`) | consumes | site 8 | rollouts run to τ=4 vs τ=2 |
| 10 | family commensurability | `war_machine.clj:6066–6078` (`cascade-family-parameters`) | consumes; distinct Ts across problems → `:incommensurable-family`; T = `(first Ts)` | per-problem Ts | a mixed family (one problem at 4, one at 2) refuses outright — relevant to D16's per-click horizon |
| 11 | class observation model + EFE | `war_machine.clj:6038–6058` (`class-observation-model {:horizon T}`), `:6416–6426` (`efe/rank-actions {:horizon-steps T}`), `:6482` (`:horizon-steps T` onto decision) | consumes | family T (site 10) | class-preference mass at τ=T; G sums to τ=4 vs τ=2 |
| 12 | admission (`candidate-want-progress`) | `war_machine.clj:6549–6566`; verdict kinds `:6617–6618` | **consumes**; non-pos-int → `:missing-common-horizon` refusal; else rolls each candidate out at `horizon-steps` on fresh facts, requiring a new want token within T | problem's stamped T | a chain longer than 2 whose want lands at step 3–4 declines `:no-new-wanted-token` → target refuses `:no-constructed-candidate` at 2, passes at 4 (see §3) |
| 13 | constructor `compile-plan` / `construct` | `src/futon2/aif/interpretation_construction.clj:60–94`, `:126–130` | **consumes a caller-supplied horizon**; not pos-int → `:horizon-required`; unreachable → `:want-unreachable-within-horizon` / finding `:beyond-horizon` | **whoever calls it** — no wiring ties it to site 4 (I2 §5 gap 7: authority splits) | constructor refuses `:want-unreachable-within-horizon` for 3–4-step chains at 2 that construct at 4 (I2/D16: seat B's 6-pattern chains refuse at 4, construct at 8) |
| 14 | I2 offline script | `holes/labs/wm-contract/E-cascade-real/i2_construct.clj:83` | produces `(def horizon (or (:horizon-steps declared) 4))` | **literal fallback 4** — the mirror-image defect of site 4 (a substituted value, not a typed absence) | — |
| 15 | model manifest | `src/futon2/aif/cascade_model_manifest.clj:440` | **produces a second, unrelated `:horizon`**: `{:authority :documented-interpretation :rule :firing-pattern-count :value (count patterns)}` | pattern count, not declared T | none — but two different `:horizon` keys with different authorities coexist; `horizon-g` (:553–573) consumes the passed one and refuses `:invalid-horizon` when non-pos-int |
| 16 | prefix evidence | `src/futon2/aif/policy_prefix_evidence.clj:36` | consumes τ-vs-receipts consistency (`:prefix-horizon-mismatch`) | receipt count | unchanged |
| 17 | the *other* horizon (flat EFE depth) | `war_machine.clj:7020–7023` + `src/futon2/aif/policy_depth.clj:26–29` | produces `wm-horizon-steps` (default 3 ready / 1 not-ready) | run config / anticipation, **not** the cascade sources | none — distinct channel; recorded at `war_machine.clj:7292,7302` (`:horizon-steps`, `:policy-depth-used`) and `:1014–1015` (`:effective-horizon`/`:effective-depth`). Do not confuse with the cascade horizon |
| 18 | judgement record | `war_machine.clj:7254` (`:cascade-horizon cascade-horizon`), `:7272–7278` (`:cascade-sources` provenance: `:supplied-by-caller` / declared files / `:none-supplied`) | **produces the certificate's horizon record** | site 4 | certificate carries `{:value 4 :authority {:source :cascade-sources …}}` vs `{:value 2 :authority "p4ng 462aa79…"}` |
| 19 | flight loop judge | `src/futon2/aif/full_loop_runner.clj:4555–4564` | calls `wm/generate-war-machine` with no `:cascade-sources` → declared path (lift applies). But `war_machine.clj:7055` `(or (:cascade-sources judge-opts) …)` lets any caller **supply a sources map that bypasses the lift** — un-lifted maps lack `:horizon-steps`, so site 4 falls to 2 | caller-supplied vs declared | a flight supplying hand-built sources silently reverts the judge to 2 while the files declare 4 — the exact D14 wording, still reachable at HEAD |

## 2. Recorded evidence: which horizon the records carry

**`data/wm-runs/tick-run-record-2026-09-23-1790199409.edn`** (the click that
selected T-repair-occ-444fb018): 16 occurrences of `:horizon 4`, all inside
the selection certificate — 10 `:horizon 4, :acceptance` (class-observation
models, site 11), 4 `:horizon 4, :class-preference`, 2 `:horizon 4, :status
:recorded` (horizon-g evaluation records). The strings `cascade-horizon`,
`horizon-steps`, and `policy-depth-used` do **not** occur anywhere in the
record: the certificate carries the *consumed* value 4 per structure, but
not the *resolved* value-with-authority from site 18. A reader can see the
judge used 4; it cannot see where 4 came from.

**`data/wm-runs/tick-run-record-2026-09-24-1790225596.edn`** (the abstained
click 2): zero occurrences of the string `horizon`. The abstention record
carries no horizon at all — consistent with CLICK2-D's carrier-gap finding
(no decline detail on the abstention path): the record does not say which
horizon admission ran at, only (per the scan markdown) that all targets
declined. This is a typed absence missing its type.

So: 09-23 records 4 (consumed, authority unrecorded); 09-24 records nothing.
The fallback 2 appears in **no** run record — because the lift (`1de6aadc`)
predates both runs, and the live tick path resolves 4. The D14 sentence
("the judge's horizon falls back to T=2 while the sources declare 4")
describes the pre-lift window (the T-repair source comment, written
`aac7ae01` 16:20, says the lift "does not yet" exist; `1de6aadc` landed
16:32 the same day — **that comment is stale at HEAD**) plus the two paths
still open at HEAD: no-declaration families (4 of 5 current source files)
and caller-supplied sources (site 19).

## 3. What changes at 4 vs 2 (CLICK2-D Part 2 reasoning, no clicks run)

CLICK2-D Part 2 ran admission verbatim at the declared horizon 4 on the
current facts; all four admitted targets declined `:no-new-wanted-token`.
None of those four verdicts moves with T:

- wants already true at admission (T-repair-occ, M-wm-08, and the true
  subsets for M-f11 / M-aif-policy-conditioned-eig) yield empty new-wanted
  at **any** T — horizon-independent;
- M-f11's absent want `:hole/h2045faa0e7cc` is produced by no declared
  pattern — unreachable at any T;
- M-aif-policy-conditioned-eig's reachable absent want
  `:hole/h42fceb4ad48b` (`:aif/two-layer-calibration`, one step) is inside
  even T=2 — but no candidate's precedence contains the pattern, so the
  decline is horizon-independent too.

Where T *is* decisive, by the same reasoning: any target whose shortest
honest chain to a new want exceeds T. The existence case is T-repair's own
C2 (4 steps: declare→collect→calibrate→accept — the reason 4 was declared).
On 09-23 the want was still false; at T=2 the C2 rollout cannot reach
`:restoration-accepted` by step 2, so C2 (and any candidate whose first new
want lands past step 2) declines `:no-new-wanted-token`, and with both
candidates declined the target refuses `:no-constructed-candidate` — the
click that selected on 09-23 would have abstained. The constructor mirrors
this one gate earlier: chains of 3–4 steps refuse
`:want-unreachable-within-horizon` at 2 and construct at 4 (I2: seat B's
6-pattern chains refuse at 4, construct at 8 — same gate, larger gap).

## 4. Proposed amendment (single source of the horizon)

**Amendment H1 (proposed; no operator ruling appealed to — settled by the
code above):** the common horizon has exactly one source and one carrier.

1. *Declared in sources.* `:horizon-steps` is declared per source file;
   `cascade-sources/load-declared` lifts the MAX into the merged map
   (already: `cascade_sources.clj:273–297`). A source family in which no
   file declares is a **typed absence**, never a substituted value.
2. *No judge-level fallback.* Delete the `{:value 2 :authority "p4ng
   462aa79…"}` branch (`war_machine.clj:7063`). The absence gate already
   exists and is honest: with nothing stamped, `cascade-problems/assemble`
   refuses every target `:horizon-not-declared` (`cascade_problems.clj:
   262–267`) and the tick abstains with typed refusals. Site 4's fallback
   only masks that absence upstream of the gate that would type it.
3. *Caller-supplied sources go through the same gate.* The
   `(:cascade-sources judge-opts)` path (`war_machine.clj:7055`) must not
   silently acquire a fallback horizon; un-lifted caller maps flow into
   the same `:horizon-not-declared` refusals (automatic once (2) lands).
4. *Carried on the certificate with authority, on both outcomes.*
   `:cascade-horizon {:value … :authority …}` must appear on selection
   **and abstention** records (09-23 has the consumed value without the
   authority; 09-24 has neither).
5. *Constructor and judge read the same value.* The horizon passed to
   `interpretation-construction/construct` is the judge's resolved
   `cascade-horizon`, never a caller literal; `i2_construct.clj:83`'s
   `(or … 4)` is the same defect as site 4 with the sign flipped.
6. *Hygiene.* The T-repair source comment claiming the lift "does not
   yet" exist is stale at HEAD (`1de6aadc`); correct it when the file is
   next touched.

## 5. D16 hook (for claude-10)

The consumer that must accept a per-click horizon computed by the flight
loop is the judge's `cascade-horizon` binding itself
(`war_machine.clj:7059–7063`, inside `judge` at `:6696`), reached through
`full_loop_runner`'s `:judge-fn` (`full_loop_runner.clj:4555–4564`).
Concretely D16 needs:

- a judge-opt (e.g. `:cascade-horizon`) resolved **before** the fallback
  branch, carrying its own authority (`{:value T :authority {:source
  :flight-loop :click-id …}}`), so the flight loop's per-click T is what
  gets stamped at `:7084`, consumed by assemble (`cascade_problems.clj:
  262`), the lane R13 (`war_machine.clj:5819`), admission (`:6549`), and
  recorded at `:7254`;
- the same value handed to any in-click constructor call (site 13), so
  construction and admission cannot disagree about reachability on the
  same tick (I2 §5 gap 7);
- note site 10: a per-click horizon that differs across targets in one
  family trips `:incommensurable-family` — D16's per-click horizon must be
  per-*family* (one T per compared family per click), not per-target.

No clicks were run and nothing under `data/` was written for this packet.
