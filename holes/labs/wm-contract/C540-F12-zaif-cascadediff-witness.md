# C540 — F12 slice 2: the O-laws on a real library cascade

**Row:** `:F12` slice 2 (`worklist.edn`, `:loop-mode :one-slice-per-invocation`).
Slice 1 is `C539-F12-organise-census.md`; this is the slice its §3 names.

**What this is.** One recorded cascade over the whole `futon3` library — the zaif
run `:widen-to-a-budget`, 20 nodes over 1239 patterns — transcribed into a
`CascadeDiff` fixture in Lean, with O1–O3 stated of it beside the C59 fixture and
both kept. Every number in the fixture is derived by
`f12_zaif_transcription.clj` into `runs/F12-organise/01-zaif-transcription.edn`,
which then re-reads the Lean block and fails if the two disagree.

**What this is NOT.** The `sorry` at `Holes.lean:861` is untouched — slice 1
established it is inert (no term reference to `organise` anywhere in
`DarkTower/**/*.lean`), and discharging it is slice 4, blocked on decision D1.
No O4 is stated of the new fixture (§3). No `:choices` entry, no `:decisions`
entry, no edit to `P-validated-R5.md`, no `futon3` file changed, no registry
moved, no tick, no run lock.

---

## 1. What is now witnessed that was not

| | C59 fixture (`Holes.lean:872`) | zaif fixture (`Holes.lean:974`) |
|---|---|---|
| repository | 3 hand-derived vertices | `library/` at `futon3` `1b8b1d1`, 1239 patterns |
| `selected` | 2 | 11 (`:find :selected`) |
| `addedByOrganise` | `∅` | `∅` |
| `admittedBy` | **`∅`** | **9**, by `:widen-the-cascade-only-on-evidence` |
| `nodes` | 2, `= selected` | 20, **`≠ selected`** |
| authored edges | 2 | 13, over a 27-vertex closure |
| organised edges | 1 | 1 (`18 → 19`) |

The two `∅`s in the C59 column are what slice 1 asked this slice to remove. O1's
three-way union (`Holes.lean:894`) was witnessed there as `nodes = selected ∪ ∅ ∪
∅` — true, and true of any cascade in which organise did nothing and no rule
fired. `organiseO1NodesRecordedZaif` (`Holes.lean:993`) is the same equation
where the third origin supplies 9 of the 20 nodes.

The four new declarations elaborate with no `sorry`:
`organiseO1NodesRecordedZaif` (`propext, Classical.choice, Quot.sound`),
`organiseO2AuthoredReachabilityZaif` (no axioms), `organiseO3FastForwardZaif`
(no axioms), `organiseO3FastForwardOverSelectedFails` (`propext`).

## 2. The finding: the C59 fixture cannot tell the two readings of O3 apart

`organiseO3FastForward` (`Holes.lean:909`) states O3 over `.selected`:

    organisedEdges u v ↔ fastForward wmCascadeDiffFixture.selected … u v

The Clojure law it names as its mirror evaluates `fast-forward` over **`nodes`**
— `o3-fast-forward` (`futon3:checks/find_organise.clj:529`), and so does the
constructor that builds the edges in the first place
(`futon3:checks/construct_cascade.clj:415`).
On the C59 fixture `nodes = selected`, so these are the same proposition and the
file has never had to choose between them.

On this cascade they are not the same proposition, and the difference is not
marginal:

| `fast-forward` over | result |
|---|---|
| the 20 `nodes` | `{[18, 19]}` — one edge |
| the 11 `selected` | `{}` — empty |

Both endpoints of the cascade's one organised edge
(`:math-formalization-CV/frontier-bound-from-arc-hypotheses` →
`:math-formalization-CV/holomorphic-disk-api`) were **admitted**, not selected,
so the `.selected` reading cannot see the edge at all.
`organiseO3FastForwardOverSelectedFails` (`Holes.lean:1034`) states that
disagreement in Lean rather than repairing either side: it proves

    ¬ (∀ u v, organisedEdges u v ↔ fastForward .selected .authoredEdges u v)

for this fixture. **No ruling is taken here.** Which set O3 is owed is a second
question, next to but not the same as decision D1 (C539 §4): D1 asks which
carrier the four laws are stated of, this asks which of that carrier's two
fields O3 quantifies over. Both belong to Joe, not to a slice. What the slice
contributes is that the second question is now a machine-checked disagreement
rather than a difference between two docstrings that no fixture had tested.

## 3. Why no O4, and why the four O4 fields are empty

`construct_cascade.clj` `cascade-of` (`:402`, fields at `:420-421`) sets `:precedence-before []` and
`:precedence-after []` for this run and carries no score, because nothing was
played; the record's own `:o4` reads
`:not-exercised-fewer-than-two-members-carry-a-play-grain-rule`. The Lean fields
are therefore `[]`/`[]`/`[]`/`[]`/`0`/`0` and no O4 statement is made of the
fixture. Filling them with the record's `:precedence` map and `:ordered` vector
would have produced an O4 that passes on a run in which nothing moved. O4 over
the library is slice 3, and it is a `futon3` construction, not a Lean edit.

## 4. What the basis is, and the one recorded number that does not reproduce

The relation is read from `git archive` of `futon3` **`1b8b1d1`**, not from the
checkout. At HEAD the library has moved a long way past the record: the authored
`@why` relation carries 507 edges against the record's 92, and the closure of
these 20 nodes grows from 27 vertices to 41. Transcribing from HEAD would have
produced a fixture that is *a* cascade but not *the recorded* one — this is
control 4 in §5, and it fails loudly.

Three of the record's four `:as-of` counts reproduce exactly at the basis:
sections 98, authored `@why` 92, authored `@why`+`@how` 124. The fourth does
not: **1238 patterns against the recorded 1239.** The delta is the
untracked-scribe condition `construct_cascade.clj:67-73` states and the record
itself flags (`:untracked-flexiargs-are-possible? true`), and it is checked
rather than excused. `git` reports two untracked files under `futon3:library/`;
the script re-derives with each one added and requires the authored relation —
edge counts, the transcribed edge list, the closure, the index and both
`fast-forward` results — to be unchanged:

| untracked file | patterns | relation unchanged? |
|---|---|---|
| `library/math-formalization-FA/lf-map-continuity-via-fixed-support-stages.flexiarg` | 1239 | yes |
| `library/math-formalization/vanishing-derivative-limit-via-fixed-interval-slopes.flexiarg` | 1239 | yes |

So either file restores the recorded count and neither authors an edge: which
one the record read is **undetermined**, and cannot move what the transcription
rests on.

The record's `:read-digest` is **not reproducible and is not used as a check**.
It hashes each entry's `(:file e)` (`construct_cascade.clj:76`), so it moves with
the root path a derivation is run from; three different roots over identical
content gave three different digests here. Recorded, not repaired.

Independent corroboration that the transcription is of the right cascade, from
the record's own controls rather than from this derivation: the record's
`:degree-relation-authors-no-edge :cascade-edges` is `1`, and its
`:O3-dropped-edge` negative control names the pair
`[:math-formalization-CV/frontier-bound-from-arc-hypotheses
:math-formalization-CV/holomorphic-disk-api]` — which is exactly the edge
recomputed here.

## 5. What was checked, so the account is auditable

- `lake build DarkTower.WarMachine.Holes` — 2704 jobs, completed successfully,
  0 errors. `#print axioms` on all four new declarations: no `sorryAx` (§1).
- `f12_zaif_transcription.clj` PASS. Two runs over an unchanged tree are
  byte-identical (`diff -q` exit 0).
- **Six negative controls, each with the plant verified to have landed first**
  (slice 1's lesson: a plant that did not take reads exactly like a control that
  failed to fire). Four against the checker, via the `F12_HOLES`, `F12_OUT` and
  `F12_BASIS` env overrides so the real files are untouched; two against Lean,
  planted into `Holes.lean` and reverted.

  1. Authored edge `18, 19` → `18, 20` in the Lean block — checker `FAIL
     [:authored-edges [[18 20]] [[18 19]]]`. Caught.
  2. `zaifSelected` bound `n < 11` → `n < 12` — `FAIL [:selected-bound 12 11]`.
     Caught.
  3. `organisedEdges := fastForward zaifNodes` → `zaifSelected` — `FAIL
     [:organised-edges-not-over-nodes true false]`. Caught.
  4. Basis `1b8b1d1` → `futon3` HEAD `7c653bb` — `FAIL` on the edge list, on
     `:as-of` (507 `@why` edges against 92) and on `:patterns-unaccounted`.
     Caught.
  5. Lean: authored edge `18, 19` → `18, 21`, so the one organised edge
     disappears — `lake build` error at `Holes.lean:1045`, `Tactic 'assumption'
     failed`. Caught, and it is what makes
     `organiseO3FastForwardOverSelectedFails` non-vacuous.
  6. Lean: `zaifAdmitted` upper bound `n < 20` → `n < 19`, so one admitted node
     leaves the third origin — `lake build` error at `Holes.lean:999`, `omega
     could not prove the goal`. Caught, and it is what makes
     `organiseO1NodesRecordedZaif` non-vacuous.

  After both Lean controls the file was restored and rebuilt: `diff -q` against
  the pre-control copy identical, 2704 jobs, 0 errors.

- **A defect control 4 found in the checker itself, fixed here.**
  `with-untracked-file` first keyed its staging directory on the filename alone
  and reused it across bases, so the HEAD-basis probe re-read the `1b8b1d1`
  probe's library and reported
  `[:untracked-file-moves-the-relation …]` — a true-looking finding produced
  entirely by the cache. The directory is now named for the basis as well and
  rebuilt every run (`f12_zaif_transcription.clj`, `with-untracked-file`), and
  control 4 re-run reports the correct three reasons. Worth naming because the
  false reading was the *more alarming* of the two: it accused the library of
  drifting under the record.
- Gates: `clj-kondo --lint f12_zaif_transcription.clj` 0 errors 0 warnings;
  `check-parens` OK; `negative_controls.sh`; `pointer_check.bb`;
  `worklist_check.bb` — results in the commit message and the row's `:evidence`.
- Not run: `gen_aif_dag.bb` (TN §9a gate rule).

## 6. Where the row stands after this slice

Slice 3 (O4 over the library, a `futon3` construction) and slice 4 (the carrier
reconciliation and the `sorry`, blocked on D1) are unchanged by this slice.
Slice 2 removes the two coincidences that made the C59 witness weaker than it
looked, and turns half of D1 from a reading into a proof.
