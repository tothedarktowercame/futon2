# C539 — F12 slice 1: where `organise` is witnessed today

**Row:** `:F12` slice 1 (`worklist.edn`, `:loop-mode :one-slice-per-invocation`;
the row carried no `:progress`, so this is its first slice).

**What this is.** The census the row needed before any Lean could be written:
for `organise` (P-validated-R5 §3e, `P-validated-R5.md:443`), what is stated
where, what is witnessed on what, and which halves of the `:acceptance` are
already met. Every number below is recomputed by `f12_organise_census.bb` into
`runs/F12-organise/00-census.edn`; none is read off by hand.

**What this is NOT.** No Lean was written or elaborated. The `sorry` at
`Holes.lean:861` is untouched. No `:choices` entry, no `:decisions` entry, no
edit to `P-validated-R5.md`, no edit to any `futon3` file, no registry moved, no
tick, no run lock. Two of the findings below name a decision; neither is taken
here.

---

## 1. The four findings, each with what depends on it

### F12-a. The `sorry` at `Holes.lean:861` is inert — nothing in Lean uses `organise`

`def organise {Policy P : Type*} : Cascade Policy → Set P → Repository P →
Cascade P := sorry` (`Holes.lean:861`). Across `DarkTower/**/*.lean` the bare
identifier `organise` occurs seven times outside that declaration, and the
census classifies each rather than counting them:

| kind | lines | what they are |
|---|---|---|
| doc-comment prose | `Holes.lean:844,860,893,901,908` | text inside `/-- … -/` blocks |
| string literal | `Holes.lean:7899` | `mkRefused "organise" "P-validated-R5 §3e organise" "implementation, not a law"` — the hole registry naming it |
| **term reference** | **none** | — |

So no declaration elaborates through `organise`. Discharging the `sorry` would
change the proof obligation of nothing; the four O-law declarations
(`organiseO1NodesRecorded` `:894`, `organiseO2AuthoredReachability` `:902`,
`organiseO3FastForward` `:909`, `organiseO4PrecedenceGovernance` `:916`) do not
mention it. **What this means for the row:** the `:acceptance`'s two clauses —
"the four O-laws … witnessed on a REAL constructed cascade" and "discharge or
amend the `sorry`" — are today two disjoint jobs. Making the second serve the
first needs a statement that quantifies over what `organise` *returns*, which
does not exist yet and which §2 below shows cannot be written of the current
return type.

### F12-b. The O-laws are not statable of `Cascade`, which is what `organise` returns

The `:acceptance` asks for "the four O-laws stated in Lean **against the
existing `Cascade` carrier**". There are two carriers, and the laws are stated
of the other one:

| carrier | line | fields |
|---|---|---|
| `Cascade P` | `Holes.lean:29` | `nodes` `addedByOrganise` `edges` `acyclic` `precedence` |
| `CascadeDiff P Score` | `Holes.lean:846` | `selected` `nodes` `addedByOrganise` `admittedBy` `authoredEdges` `organisedEdges` `precedenceBefore` `precedenceAfter` `actingOrderBefore` `actingOrderAfter` `scoreBefore` `scoreAfter` |

`organise`'s codomain is `Cascade P`. All four O-laws quantify over
`wmCascadeDiffFixture : CascadeDiff Nat Int` (`Holes.lean:872`). `Cascade P`
carries neither `selected` nor `admittedBy`, so O1's three-origin equation —
`nodes = selected ∪ addedByOrganise ∪ admittedBy`, `Holes.lean:895-897` — cannot
be *written* of a `Cascade P` value at all, and O4 needs the before/after pairs
that only `CascadeDiff` has. **What this means for the row:** the acceptance's
phrase names the one of the two carriers that cannot carry O1. Three repairs
are available — widen `Cascade` with the two missing origin fields, retype
`organise`'s codomain to `CascadeDiff`, or state the laws of a `(Cascade,
Repository)` pair — and choosing among them is an amendment to the `:acceptance`,
which is not a slice's to take. **Decision D1, §4.**

### F12-c. O1–O3 are witnessed on real library cascades; O4 is witnessed nowhere on the library

Nine recorded cascade files were surveyed. Four carry `:runs` with per-law
verdicts, and every run in all four reports `{:O1 true, :O2 true, :O3 true}`:

| record | repository | authored `@why` edges | tension | runs | O4 |
|---|---|---|---|---|---|
| `futon3:checks/construct-cascade.edn` | 1239 patterns | 92 | `:organise-the-library` | 2, both O1/O2/O3 true | **key absent** |
| `futon3:checks/zaif-cascade.edn` | 1239 patterns | 92 | `:the-seat-does-not-stop` | 2, both O1/O2/O3 true | `:not-exercised-fewer-than-two-members-carry-a-play-grain-rule` |
| `futon3:checks/ants-cascade.edn` | 5 patterns | 0 | `:the-colony-does-not-eat` | 2, both O1/O2/O3 true | **exercised, `:holds? true`** |
| `futon3:checks/alfworld-cascade.edn` | 10 patterns | 0 | `:the-step-budget-is-spent-finding-not-doing` | 2, both O1/O2/O3 true | `:not-exercised-no-authority-gate-and-alfworld-is-not-installed` |

The other five (`snatch-cascade.edn`, `open-cascade.edn`,
`open-cascade-short-cue.edn`, `retrodiction-cascade.edn`,
`retrodiction-cascade-per-clause.edn`) carry no `:runs`; four say
`:not-exercised-nothing-is-played` and `snatch-cascade.edn` has no `:o4` key.

So O4 is exercised and holding on **exactly one** record, `ants-cascade.edn`,
and it holds through the acting-order arm alone: `:precedence-changed? true`,
`:acting-order-changed? true`, `:score-changed? false`. That repository is five
patterns with **zero authored `@why` edges** — the edges O2 and O3 are about.
The two records over the 1239-pattern library, which is what the
`:acceptance` means by "the library", do not exercise O4 at all, and they fail
to for two different reasons: `zaif-cascade.edn` records why (fewer than two
members carry a play-grain rule), `construct-cascade.edn` records nothing, its
`:o4` key being absent rather than declined. **What this means for the row:**
"each law witnessed on a REAL constructed cascade" is three-quarters met in
Clojure already, and the missing quarter is specifically *O4 over the library*.

### F12-d. The F7 record cannot witness any O-law without a translation step

The `:acceptance` offers "F7 record **or** fresh construction from the library".
`runs/F7-cascade-policy/f7-cascade-policy-decision.edn` has two candidates
carrying eighteen keys: `:H-coherence :T-intensity :budget :candidate-source
:cascade-score :coverage-reward :coverage-saturation-epsilon :lambda :mission
:policy-grain :policy-key :prior-cost :psi :semilattice :shown :size :truncated
:wholeness`. Of the six fields an O-law needs to be stated of a value —
`:nodes :selected :added-by-organise :admitted-by :edges :precedence` — **zero
are present**. The record is a scored *menu* at policy grain, not a cascade
carrier. **What this means for the row:** the first half of that alternative is
not available as it stands; the witness has to come from the `futon3`
constructor's records, which is the second half.

---

## 2. Two things the census corrected, rather than repeated

**The Lean doc comment at `Holes.lean:843-845` is true only under its narrow
reading.** It says `admittedBy` is `∅` "in the C59 fixture below, and in every
cascade `futon3:checks/find_organise.clj` `organise` builds, since neither
temperament that file carries emits an `admit`." Scoped to the *function*
`organise` that is exact: `find_organise.clj:337` hard-codes `:admitted-by #{}`.
Scoped to the *file* it is false, and the file is what the sentence names:
`construct` (`find_organise.clj:439`) via `apply-edit` (`find_organise.clj:422`)
writes `[:admitted-by by]` into a cascade's `:provenance`, and the recorded
library runs carry 11 and 4 admitted nodes (`construct-cascade.edn`) and 9 and 6
(`zaif-cascade.edn`). This is not pedantry about scope: O1's third origin is
`∅` in the Lean fixture, so the three-way union is currently witnessed
*trivially*, and the census's point is that a **non-trivial** O1 witness became
possible when `construct` moved into that file and was not available when the
fixture was written.

**The spec bullet and the Lean statement of O1 have diverged.**
`P-validated-R5.md:499` states O1 with two origins — `cascade.nodes = selected`,
up-closure "recorded as *added by organise*". `Holes.lean:895-897` states it
with three, and its docstring says so: "UNION AMENDMENT 2026-09-02 (worklist
`:LA2`): the union is three-way." The Lean was amended; the §3e bullet was not.
Recorded, not repaired: `P-validated-R5.md` is a problem dossier and this row's
`:acceptance` does not reach it.

---

## 3. The proposed slice sequence

Slice 1 is this census. The row stays `:open`.

- **Slice 2 — witness O1–O3 in Lean on a real construction.** Transcribe one
  recorded library cascade (`zaif-cascade.edn` or `construct-cascade.edn`) into
  a `CascadeDiff` fixture and re-state `organiseO1`–`O3` of it beside the C59
  fixture, keeping both. This needs no carrier ruling: it uses `CascadeDiff`,
  which is the carrier the four laws already use, and it turns the trivial
  `admittedBy = ∅` witness into a real one (9 or 11 admitted nodes). Small,
  Lean-only, `lake build` gated.
- **Slice 3 — O4 over the library.** The gap F12-c names. A `futon3`
  construction whose members carry at least two play-grain rules, so the
  precedence change has something to reorder; produces an `:o4` map on a
  1239-pattern record. This is a `futon3` run, not a Lean slice.
- **Slice 4 — the carrier reconciliation and the `sorry`.** Blocked on D1
  below. Whichever repair is chosen, this is where `Holes.lean:861` is
  discharged or amended and where the swappability clause
  (`P-validated-R5.md:513`) gets a Lean statement; today it has a Clojure one
  only (`organise-reproduces-record?`, `find_organise.clj:635`, and the per-law
  mutations in `mutate-diff`, `find_organise.clj:679`).

## 4. The one decision this census raises

**D1 — which carrier the O-laws are stated against.** F12-b: `organise` returns
`Cascade P`, which lacks `selected` and `admittedBy`, so O1 is unwritable of it;
the laws are stated of `CascadeDiff P Score`. The `:acceptance` says "against
the existing `Cascade` carrier". Options: (i) widen `Cascade` with the two
fields, (ii) retype `organise`'s codomain to `CascadeDiff`, (iii) state the laws
of a `Cascade`-plus-`Repository` pair and leave both carriers alone. Slice 4
cannot start without it; slices 2 and 3 can, and do not prejudge it.

---

## 5. What was checked, so the account is auditable

- `f12_organise_census.bb` recomputes every number above and writes
  `runs/F12-organise/00-census.edn`. Two runs over an unchanged tree are
  byte-identical (`diff -q` exit 0); receipt SHA-256 recorded in the ledger row.
- **Four negative controls, run against temp copies via the script's four env
  overrides** (`F12_HOLES`, `F12_FIND_ORGANISE`, `F12_CODE_ROOT`, `F12_OUT`,
  added for exactly this reason — without them the census would report the same
  summary whether or not it had read the files it names):
  1. `:O3 true → false` in a copy of `zaif-cascade.edn` — witness count 4 → 3. Caught.
  2. `:o4 :holds? true → false` in a copy of `ants-cascade.edn` — O4 witness count 1 → 0. Caught.
  3. `wmCascadeDiffFixture → wmCascadeDiffPlant` inside O1's body only — `:stated-of` `wmCascadeDiffFixture` → `:other`. Caught.
  4. A real term use of `organise` appended to a copy of `Holes.lean` — term-reference count 0 → 1. Caught.
- **Two of those four plants were written wrong the first time and reported a
  false miss.** Plant 2's `sed` matched a single-line shape the pprinted `.edn`
  does not have, and plant 3's used `.` for the multibyte `∪`; neither landed,
  and both looked like the census failing to notice. Each was re-run with the
  plant verified to have landed first (`:o4 :holds?` read back as `false`; four
  occurrences of `wmCascadeDiffPlant`) before the result was believed. The
  defect was in the control, not in the thing under test — which is only
  distinguishable by checking that the plant took.
- Gates: `clj-kondo` on the new script, `check-parens`, `negative_controls.sh`,
  `pointer_check.bb`, `worklist_check.bb` — results in the commit message and
  the row's `:evidence`.
- Not run: `gen_aif_dag.bb` (TN §9a gate rule). No Lean elaborated — this slice
  writes none.
