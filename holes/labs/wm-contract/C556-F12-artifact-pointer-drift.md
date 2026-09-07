# C556 — F12 slice 20: the fifth arm registered, and a pointer drift the gate cannot see

**Slice 20 of `:F12`** (`:loop-mode :one-slice-per-invocation`). Two things: the
registry write slice 19 named as next, and a defect found while checking the
pointers for it.

No ruling is taken. No machine was run. `mathlib4/DarkTower/WarMachine/Holes.lean:861`
is untouched and is still neither discharged nor amended.

## 1. What was registered

`aif-equations.edn :choices :organise-o4-denominator` carried
`:arms-not-all-run`, saying that the fifth arm — C541 section 4's option (i) as
written, the split reading the acting order over the transcript rounds and the
score over the primary ones — was not run, and that the entry deliberately
carried no `:all-arms-run` key for that reason. Slice 19 ran it (futon2
`3c0ad833`, C555) and made both sentences false. This slice replaces the arm's
`:run-by :not-run` / `:buys :not-measured` / `:costs :not-measured` /
`:why-not-run-here` with measured content, and `:arms-not-all-run` with
`:all-arms-run`.

Slice 13 set the shape for exactly this amendment on a sibling entry — the
`:organise-carrier` `:all-arms-run` key reads "the sixth arm, registered by C550
as `:not-run` with both fields `:unmeasured`, is run".

**What the arm buys:** nothing over the transcript arm on either construction,
which is a measured result and not a prediction. O4 holds on both and on the
same single disjunct — at HEAD `:o4-holds? true` with
`:disjuncts-that-hold [:acting-order-changed]` and the primary score unmoved at
14 → 14 (`runs/F12-organise/11-d2-denominator.edn:7021-7023`, `:7233-7234`); on
Arm B the same, with the primary score at 15 → 15 (`:6187`, `:6293`,
`:6536-6537`). What it buys is a *statement* and not a number: the score is read
over the 29 rounds the gate grades, so the objection that a score needs a
determined oracle label is answered without moving the acting order off the
transcript.

**What it costs:** it is the transcript arm's verdict with a narrower score, and
no construction anyone has built can tell the two apart.
`:construction-can-separate-them? false` with
`:why-not :acting-order-moves-so-o4-holds-on-that-disjunct-alone` on both
(`:6896-6902` at HEAD, `:6160-6169` on Arm B), recomputed against the parent arms
at `f12_d2_denominator_check.clj:140-171`.

**The mirror is recorded and is not offered as a sixth arm.** Acting order over
the 29 primary rounds and score over the 102 transcript ones gives O4 TRUE on
both constructions where the primary arm gives FALSE, on
`:disjuncts-that-hold [:score-changed]` with the acting order unmoved
(`:7447-7450`, scores 83 → 81 at `:7514-7515`; `:6686-6688`, `:6720`, scores
20 → 19 at `:6890-6891`). It isolates a disjunct the four single-denominator arms
cannot: the transcript arm's TRUE fires *both* disjuncts, so it cannot say
whether the score alone would carry it, and the mirror says it does. C541
section 4 names three options and nobody proposed this reading; it was run to
test whether a split can separate a verdict at all
(`f12_d2_denominator_check.clj:180`), and whether a reading no one proposed
belongs among D2's options is not a slice's to say.

**Two further stale claims in the same entry, repaired:** the transcript arm's
`:what-it-is` pointed at "the unrun arm below", and both
`:what-would-change-the-measurement` and `:not-claimed` described D3 as
"measured, unregistered" — false since slice 18 registered it as
`:organise-o4-after-the-law-encoding`.

**What did not change.** Parsed as values, the registry differs from HEAD at
`:choices :organise-o4-denominator` and nowhere else — 28 `:choices` keys before
and after. The entry's key set is now identical to its two `:all-arms-run`
siblings (`:organise-o3-field`, `:organise-o4-after-the-law-encoding`). No
`:ruling` key; `:status` stays `:observed-not-decided`; no arm is preferred, and
the fifth is not preferred by being last.

## 2. THE FINDING: slice 19 reflowed a cited artifact and re-pointed nothing

`runs/F12-organise/11-d2-denominator.edn` went from **3537 to 7626 lines** at
futon2 `3c0ad833`. Slice 19 reported this as "pprint reflow from the new
top-level key" and it is that — checked here rather than taken: parsed as
values, **every key and value of the committed artifact is preserved exactly in
the new one**, and the only differences are 17 *added* map entries
(`:split-denominator` at top level, and `:splits` / `:split-comparison` /
`:mirror-split-comparison` inside `:head-baseline`, `:arm-b :worker` and the
three `:controls` workers, plus `:head-baseline :rules`). Nothing pre-existing
was edited.

**But every line in the file moved, and no citation of it was re-pointed.**
Counting each `runs/F12-organise/11-d2-denominator.edn:N` and `:N-M` citation and
comparing the content at that span in the committed artifact against the content
at the same span today:

| file | citations | land on different content | written after the reflow |
|---|---|---|---|
| `aif-equations.edn` (before this slice) | 66 | **66** | 0 |
| `worklist.edn` (the `:F12` row) | 30 | **12** | 17 (1 unchanged by luck) |
| `C554-F12-d2-denominator.md` | 13 | **12** | 0 (1 unchanged by luck) |
| `C555-F12-d2-fifth-arm.md` | 5 | 0 | 5 |

So slice 17's registry entry — the one this slice amends — cites `:3021` for
"the primary cohort, 29 rounds", which today lands inside a rule-encoding map,
and `:3484` for `:recorded-carriage`, which today lands on a bare integer.

**`pointer_check` cannot see any of it.** It checks that a span resolves — the
file exists and the lines are in range — not that the span still says what the
citing sentence claims. The reflow made the file *longer*, so every stale
pointer stays in range and the gate reports 0 unresolved. This is the same shape
as C542's finding about the ledger's own text, one level down: there, a pointer
was unchecked because of *when* the check ran; here, a pointer is checked and
passes while being wrong.

**The 21 citations this slice adds into that artifact were each verified by reading the span in the
current file**, not by copying a number from a sibling sentence. Three drafts
did copy one — `:3428-3429` for the nesting fact, and `:3202-3208` / `:278-287`
for the transcript arm's two disjuncts — and each was caught by that check and
re-pointed to `:5323-5324`, `:5087-5090` / `:5303-5305` and `:1555` /
`:1562-1564` / `:1878-1879`. That is the failure mode the table above measures,
reproduced inside the slice that was measuring it.

**Not repaired here, and why.** Re-pointing 66 + 12 + 12 citations is its own
job with its own acceptance and its own controls, not a rider on a registry
write — the row's discipline splits discovery from implementation. It is also
not mechanical: a line-diff alignment between the two artifacts (`difflib` over
stripped lines, order-preserving) remaps only **49 of the 66** registry
citations to a span whose content matches; 14 remap to different content and 3
fall in unaligned regions. A correct repair has to locate each citation's *key
path* and re-derive the line from the current file, and then check the span
against what the sentence says — which is the per-pointer verification this
slice did 21 times by hand. Doing 90 of those blind is how a wrong pointer gets
laundered into a right-looking one.

**A question this slice does not answer:** whether earlier `runs/F12-organise/`
artifacts were reflowed by later slices in the same way, leaving the same
invisible drift elsewhere. The census is one grep and a git range; it is named
here rather than guessed at.

## 3. Gates

- `negative_controls.sh` PASS (133 negative, 53 positive; shared registries untouched).
- `pointer_check.bb` 3122 pointers in 6 files, 0 unresolved. Registry isolated
  (`AIF_EQ`): 707 → 739 against the committed version, so all 32 citations this
  entry adds resolve.
- `check-parens` OK on `aif-equations.edn`; the file re-reads as EDN with 28
  `:choices` keys and `:organise-o4-denominator` present.
- No Clojure source touched, so no `clj-kondo`. No Lean touched, so no `lake
  build`. No machine run, so no run-lock. No `gen_aif_dag.bb` (TN §9a).
