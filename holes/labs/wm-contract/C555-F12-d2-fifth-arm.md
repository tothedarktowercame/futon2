# C555 — `:F12` slice 19: D2's fifth arm, run

**What this is.** Slice 17 registered D2 with four measured arms and recorded a
fifth as **not run**: C541 §4's option (i) *as written* — the acting order read
over the transcript rounds and the score over the primary ones
(`C541-F12-o4-reachability.md:157-158`). The registry entry named exactly what
running it would take: "one more denominator spec that reads the two fields
from two round sets"
(`aif-equations.edn :choices :organise-o4-denominator :arms-not-all-run`).
This slice writes that spec and runs it, on both constructions the file already
measures. **It chooses no arm and writes no ruling**: the checker's only write
is its own artifact (`f12_d2_denominator_check.clj:16`, `:418-419`), which
carries no `:choices`, `:decisions`, `:ruling` or `:chosen-arm` key.

Why no arm above could express it: `arm` builds **one** round set from its
`keep?` and reads both fields off it (`f12_d2_denominator_check.clj:44-71`), so
the four measured arms are four denominators, not four readings. `split-arm`
takes two predicates — one for the acting order, one for the score
(`f12_d2_denominator_check.clj:88-138`) — and with the two equal it is `arm`'s
row, which the identity control checks rather than assumes.

## The fifth arm, measured

Both constructions are the ones already in this file: HEAD, exchanging
`war-machine/ambient-pattern-retrieval` and
`math-strategy/missing-dependency-protocol`; and Arm B, slice 15's planted
fifth rule, exchanging `math-strategy/missing-dependency-protocol` and
`aif/status-gated-belief-update`.

| construction | acting order over | score over | order moves | score | O4 | carried by |
|---|---|---|---|---|---:|---|
| HEAD | transcript, 102 | primary, 29 | yes | 14→14 | true | acting order alone |
| Arm B | transcript, 102 | primary, 29 | yes | 15→15 | true | acting order alone |

HEAD at `runs/F12-organise/11-d2-denominator.edn:7020-7023`, `:7233-7234`;
Arm B at `:6186-6188`, `:6293`, `:6325`, `:6536-6537`. `:disjuncts-that-hold`
is recorded because O4 is a disjunction and a `true` otherwise says nothing
about which disjunct carried it (`f12_d2_denominator_check.clj:130-137`).

**Option (i) buys nothing over the transcript arm on either construction, and
the reason is measured rather than argued.** The split's verdict is `true` on
both, the transcript arm's verdict is `true` on both, and the split's primary
score does not move on either — so its `true` rests on the acting-order
disjunct alone, which is the transcript arm's own disjunct. The artifact
records the separation condition evaluated on each construction:
`:construction-can-separate-them? false` with
`:why-not :acting-order-moves-so-o4-holds-on-that-disjunct-alone`
(`runs/F12-organise/11-d2-denominator.edn:6160-6169`, `:6893-6902`;
computed at `f12_d2_denominator_check.clj:140-171`). Slice 17 stated this
condition as what a separating construction would need; it is now the measured
value of a recorded field on the two constructions that exist.

## The mirror, which is not one of C541's options and does separate

Run as the control on whether the split machinery can separate a verdict at
all, the **mirror** reading — acting order over the primary rounds, score over
the transcript ones — gives O4 **true** on both constructions
(`runs/F12-organise/11-d2-denominator.edn:6720`, `:7450`) where the primary arm
gives **false**, and it is carried by the **score** disjunct alone:
`:disjuncts-that-hold [:score-changed]` with the acting order unmoved over the
29 primary rounds (`:6686-6688`, `:7447-7449`), scores 20→19 on Arm B (`:6890-6891`)
and 83→81 at HEAD (`:7514-7515`). Its `:construction-can-separate-them?` is
**true** on both (`:6170-6179`, `:6903-6912`).

This isolates a disjunct the four measured arms could not. Slice 16 recorded
that the transcript arm's `true` fires **both** disjuncts
(`C554-F12-d2-denominator.md`, run record `:arm-b/:worker/:arms`), so that arm
cannot say whether the score alone would carry it. The mirror says it does:
reading the score over the transcript rounds is sufficient for O4 on this
exchange with the acting order held at primary. **The primary arm's `false`
therefore depends on both fields being primary, not on the acting order alone.**

**This is not a fifth option and no option has been added to D2.** C541 §4
names three; the mirror is a measurement made to test the apparatus, and it is
recorded as such. Whether a reading no one proposed belongs among D2's options
is not this slice's to say.

## What this slice does not claim

No `:choices` or `:decisions` key was written. No arm is preferred, and no
verdict is taken on O4 itself: each of the five is a verdict **at** a reading.
The `:not-a-witness` blocker stands unchanged — zero of the nine recorded
cascade files carry a run with both rule-bearing patterns
(`runs/F12-organise/11-d2-denominator.edn:5379`), so Arm B remains what a
construction carrying two would record and not a recorded witness. The gate's
`:rule-does-not-encode-an-authored-then` failure is untouched and is not this
row's (`futon3c:scripts/zaif_cascade_gate.clj:579-582`, thrown at `:589-591`).
Nothing here bears on D1, D3, the third origin, the O3 field, or the `sorry` at
`mathlib4/DarkTower/WarMachine/Holes.lean:861`, which is still neither
discharged nor amended.

**One registry claim is now stale, by this slice's own doing.**
`aif-equations.edn :choices :organise-o4-denominator` carries
`:arms-not-all-run` saying the fifth is not run, and no `:all-arms-run` key for
that reason. After this commit that is false. Repairing it is a registry write
of a measured arm — the shape of write slices 17 and 18 made — and is the next
slice. It is named here so the gap is visible to the reviewer rather than
discovered.

## Controls and gates

The four arms measured before this slice did not move: every pre-existing
number in the artifact — the three baseline arms, Arm B's three arms and its
full play, both nested floors, the carriage census, the gate failure count and
the five earlier controls — is unchanged from the committed version, checked
key by key and not by eye. The 5,742-line diff is `pprint` reflow from the new
top-level key plus the new content.

Three plants, each in a temporary copy of the checker put first on the
classpath, each moving the acceptance run from `PASS {:problems []}` to `FAIL`:

1. Read the score over the acting-order round set (break the composition):
   `FAIL {:problems [[:split-does-not-compose-its-parents :head-baseline]
   [:split-does-not-compose-its-parents :head-baseline-mirror]]}`. The guard
   compares the split's two field pairs against the two parent arms' field
   pairs (`f12_d2_denominator_check.clj:159-162`, `:380-387`).
2. Give the identity collapse a paired acting-order side:
   `FAIL {:problems [[:split-identity-collapse-is-not-the-primary-arm
   :head-baseline]]}` — the collapse must reproduce `arm`'s primary row exactly
   (`f12_d2_denominator_check.clj:388-392`).
3. Make the forced-empty side non-empty:
   `FAIL {:problems [[:split-empty-side-was-not-refused :head-baseline]]}` —
   an empty side must refuse with `:empty-denominator` rather than return a
   vacuous true (`f12_d2_denominator_check.clj:98-101`, `:393-395`).

Each control moves the `:head-baseline` entries only. The Arm-B splits are
computed in the worker subprocess, whose classpath carries the committed
checker and the planted *gate*, not a planted checker
(`f12_d2_denominator_check.clj:248-254`, `d3/classpath-tail`), so a main-process
plant cannot reach them. The same expression guards both.

The fifth arm's verdicts are pinned like the other arms', so a later run reports
a move instead of silently restating today's numbers
(`f12_d2_denominator_check.clj:376-379`), and a HEAD rule pair that is not two
is a failure rather than a silent truncation of the precedence map (`:374-375`).

The acceptance run prints `PASS` with `:problems []`
(`runs/F12-organise/11-d2-denominator.edn:6157`); two unchanged-tree runs
produce a byte-identical artifact, sha256
`8239f3db8d1bdf82e191ead5d3102f99cc520a08cf289f05a36947a513248454`. Before the
edit, the committed checker reproduced the committed artifact byte for byte at
today's HEAD (sha256 `e166e4df22c98887020cd06db0b2842eb8b195f127e8e0ba4f9ab511e4b1d5e9`,
`git status` clean for that path), so the new content is the only change this
slice makes to it. clj-kondo 0 errors 0 warnings and `check-parens` OK on
`f12_d2_denominator_check.clj`. No Lean command and no machine run, so no
`lake build` and no run-lock. `git status --short` is empty specifically for
`futon3c:scripts/zaif_cascade_gate.clj` and for `futon3:checks/`; no broader
repository-cleanliness claim is made. Gate results are in the ledger row.
