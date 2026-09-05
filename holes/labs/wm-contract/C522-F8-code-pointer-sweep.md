# C522 — :F8 leg 1: the `:code` pointer sweep of all 18 equation rows

**Row:** `:F8` (`:loop-mode :one-slice-per-invocation`). Leg 1 slices so far:
slice 0 the U35 join refresh (C517), slice 1 Π (C518), slice 2 ε (C519),
slice 3 μ-next (C520), slice 4 F_π (C521). **This slice is not a quantity
slice.** It is the sweep slice 4 asked for in those words — "three in three
slices is no longer a pattern to note but a sweep of all 18 rows' `:code`
pointers, a slice of its own" — and it does exactly that: read every `:code`
range in `aif-equations.edn` against futon2 HEAD, decide for each whether the
range names what the row says, and repair the ones that do not.

**Commits.** The registry repair, the negative-control re-anchoring and this
report are one futon2 commit; the ledger is a second. No mathlib4 commit, no
Lean, no `src/` change, no live tick.

## 1. What was swept, and the standard applied

Eighteen equation rows. **Six carry no `:code` key at all** — `:observe`,
`:belief-state`, `:risk`, `:ambiguity`, `:expected-free-energy` and
`:policy-set` — so the sweep has nothing to check on them and does not invent
one; a row with no pointer makes no pointer claim. **Twelve carry `:code`**,
holding **33 individual `file:line` or `file:A-B` pointers** between them.

The standard is the one TN §1 already applies to an edge: a pointer is
justified by what stands at it, or it is decoration. Concretely, for each
pointer: resolve the bare filename the way `pointer_check.bb` resolves it
(first match over its root list, so `policy.clj` is
`src/futon2/aif/policy.clj` and not `src/ants/aif/policy.clj` — four of the
cited filenames exist in two canonical directories), read the range at HEAD,
and ask whether the enclosing form is the one the row's `:formal`, `:eq` or
the pointer's own parenthetical label names.

## 2. The count

| verdict | pointers |
|---|---|
| names what the row says | 10 |
| resolves, but names the wrong thing (mislabelled, wrong subject, or the equation falls outside the range) | 5 |
| lands on unrelated code | 18 |
| **total** | **33** |

**All 33 pass `pointer_check.bb`.** Every one is a real file and a range
inside it; the check is green on a field where 18 of 33 pointers name code
that has nothing to do with the row. That is the finding the sweep exists to
record, and it is not new information about the checker — `pointer_check.bb`'s
own header says it "does NOT check that the pointed code says what the field
claims". What is new is the size of the gap between what the check certifies
and what the field asserts.

## 3. Row by row, as found at HEAD

Every "lands on" below is the enclosing top-level form and its `defn` line.

**`:prediction-error`** — `war_machine.clj:4368-4379` lands inside
`scan-blocks` (`:4363`), the Block-footer commit scanner. MISS. Repaired to
`free_energy.clj:203-278` (`compute-prediction-error`),
`free_energy.clj:280-296` (`channel-prediction-error`) and
`war_machine.clj:6110-6127` (the tick's per-channel call and the
omission/refusal split) — the sites C519's `:lean-note` already named.

**`:precision`** — `precision.clj:116-135` opens on
`update-channel-precision` (`:116`) but stops in its argument list: the
`1/max(V, min-variance)` the `:formal` line states is at `:141-142` and the
floor/cap bound at `:145`, both outside the range. `precision.clj:160-190`
lands on `update-precision-state` (`:160-193`), three lines short of its end.
`war_machine.clj:4380-4388` lands on the tail of `scan-blocks` and a banner
comment. Repaired to `precision.clj:116-158`, `:160-193`, `:212-233`
(`weighted-error`) and `war_machine.clj:6130-6136`. The same `116-135` range
appears in this row's `:note`, making the same claim; repaired there too.

**`:free-energy`** — both pointers miss: `free_energy.clj:184-205` lands
across the tail of `finite-double` (`:182`), `prediction-member` (`:191`) and
the head of `compute-prediction-error` (`:203`); `war_machine.clj:4913-4915`
lands on a `.append sb` call inside `render-war-machine` (`:4505`). **And
there is nothing to repoint them to.** `compute-variational-free-energy` was
deleted by worklist `:I5` slice (c) under Joe's J2 ruling: the deletion and
its grounds are recorded at `free_energy.clj:7-12`, revision 21 removing the
per-tick `:variational-free-energy` scalar from the record is at
`trace.clj:241-247`, and `free_energy_test.clj:293-301` asserts the var no
longer resolves. `:code` now says so.

This row is also where the sweep's own justification is written down. Its
`:note` records that **RUN9 already repaired this same field once** —
`4450-4452` "had drifted onto an unrelated invariant-signal map and kept
resolving because pointer_check verifies line ranges, not content". That
repair had itself drifted by today. A field repaired once by hand and stale
again four days later is not a row's problem; it is the field's.

**`:policy-free-energy`** — the two `policy_free_energy.clj` ranges are exact
(`f-pi-for-candidate` at `:41-144`, `f-pi-vector` at `:146-151`). The four
`war_machine.clj` ranges all miss, as C521 reported: `123-155` in the
`*selection-focus?*`/`*focus-reconcile?*` docstrings, `213-336` from the
`*f-pi-posterior?*` docstring into `selection-law-of`, `384-461` on
`variational-tau-preconditions!` (`:384`), `5095-5096` on a comment block.
**Two misses C521 did not catch**, because it checked that the `policy.clj`
range resolved rather than what stood at it: `policy.clj:147-203` is labelled
`softmax-weights`, which is at `:215-235` — the range spans
`normalise-scores` (`:148-155`) and `selection-scores` (`:157-213`) instead;
and `policy.clj:413-421` is labelled "what the decision records" but is
`default-mode-select`'s abstain/chosen result maps (`:331-433`), while the
F_π-bearing decision record is at `:660-670`. Repaired to the eleven sites
C521's `:lean-note` names plus those two.

**`:belief-update`** — `war_machine.clj:4375-4388` lands in `scan-blocks`,
`4429-4431` on `health-indicator` (`:4429`), the markdown table renderer's
glyph helper. Repaired to `belief.clj:1122-1197` (`r3d-aggregate-driver`),
`:297-346` (the filter step), `:434-445` (`update-belief-batch`),
`war_machine.clj:821-827` (`apply-arena-belief-events`) and `:6147-6190` (the
tick's inner step) — the sites C520's `:lean-note` already named.

**`:forward-model`** — `efe.clj:601-609` lands inside `compute-efe`'s `:or`
default map (`compute-efe` at `:472`), which is a list of weight defaults, not
Q(o|π). MISS. `forward_model.clj:279-324` opens on the blank line after
`default-horizon-steps` (`:278`) and closes inside `predict`'s docstring: it
does cover `predict-multi-horizon` (`:280-311`), so it is right about the
subject and wrong at both boundaries. Repaired to
`forward_model.clj:280-311`, `:312-369`, `:278`, and `efe.clj:629-635`, which
is where the rolled-forward final-state observation is actually consumed.

**`:depth`** — `war_machine.clj:4485-4487` lands on the `durable-clause`
string of `render-mission-focus-line` (`:4463`). The row's claim — "3 when an
anticipation snapshot loads" — is true, and is at `war_machine.clj:6283-6285`.
`rollout.clj:166-171` lands in `move-score-record`'s docstring (`:149`); the
default-2 horizon is `rollout-horizon` at `:474-479`. Both repaired, and
`forward_model.clj:278` and `efe.clj:629-631` added, since T has three
sites and the row named two.

**`:temperature`** — `policy.clj:242-245` lands on `gap-report` (`:242`),
which enumerates capability-gap recommendations. `effective-temperature`, the
function the label names, is at `:77-146`. Repaired, with
`adaptive-temperature` (`:33-45`) and `temperature-source` (`:57-75`) added:
the label said "from selection gain / score spread" and those are the two
layers it means.

**`:policy-posterior`** — `policy.clj:196-201` is inside `selection-scores`
but on the `:f-pi-values must align` argument check; the score expression the
label names is at `:208-213`. `360-362` is labelled "the counterfactual
ordering" and lands in `default-mode-select`'s docstring, on an abstain
example; `:habit-adjusted-ranking` and `:counterfactual` are at `:641-648`.
`413-421` again lands on `default-mode-select`'s result maps. Repaired to
`:157-213`, `:215-235`, `:641-648` and `:660-670`. The row's `:note` repeats
the `360-362` pointer for the same claim and is repaired with it.

**`:action`** — `policy.clj:247-250` is `gap-report`'s body, not the "first
admissible controller entry", which is `head`/`head-idx` at `:567-570`.
`411-438` is labelled `max-key` and runs from `default-mode-select`'s result
`cond` into `numeric-range` (`:435`); the `max-key` argmax on a selection path
is at `:838-839`, and the strategic path deliberately does **not** use
`max-key` — `first-argmax` (`:525-536`) says so in its docstring, because
`max-key` keeps the last maximum and would make a tie look like a law change.
Repaired to `:567-570`, `:503-518` (`selection-laws`), `:525-536` with
`:593-594` (`chosen-idx` under the two laws), and `:838-839`.

**`:dirichlet-accumulation`** — all five pointers land:
`corpus->concentration` (`a4a.clj:85-113`), `read-corpus`
(`a4a_substrate.clj:46-60`), `:type :capability/*` at `actuator_a3.clj:31` and
`:68`, `:entity/type :discharge` inside `discharge-doc` at `:486-487`, and the
`a4a.clj:2-6` ns docstring the row quotes verbatim. **Not touched.**

**`:model-reduction`** — `bmr.clj:108-130` lands on
`bayesian-model-reduction` (`:108`); `a4a.clj:126-159` covers `score-pair`
(`:126-158`) and closes on the `defn` line of `reduce-concepts` (`:159`), the
offline driver the "(offline)" label names — tight, but it names what the row
says. **Not touched.**

Those last two rows are the only ones under a live signature: `:C8` covers
both at its `:review-covers` sha. Repairing a pointer that already names what
the row says would have lapsed a standing signature and corrected nothing, so
neither row moved, and the key-by-key diff below shows it. (`:U4` covers
`:ambiguity`, which has no `:code`.)

## 4. What moved in the registry

Ten rows, verified by reading the file and `git show HEAD:` of it into edn and
diffing key by key: **no key added, none removed, no top-level key other than
`:equations` changed**, and the eight rows not listed as repaired are
byte-identical.

- `:code` on ten rows: `:prediction-error`, `:precision`, `:free-energy`,
  `:policy-free-energy`, `:belief-update`, `:forward-model`, `:depth`,
  `:temperature`, `:policy-posterior`, `:action`.
- `:note` on three: `:precision` (the `116-135` pointer it repeats),
  `:policy-posterior` (the `360-362` pointer it repeats), and `:free-energy`
  (§5 below).
- `:lean-note` on three: `:prediction-error`, `:belief-update`,
  `:policy-free-energy` each ended with "`:code` is the field TN 9a gates, so
  it is named here and left as it stands". That sentence was true when
  written and is false once the sweep has run; each now says the defect was
  named there and repaired here.

`:imports`, `:realised`, `:formal`, `:eq`, `:lean`, `:lean-status`,
`:lean-at`, `:status` and `:retired` did not move anywhere.

## 5. One claim the sweep falsified, and where the correction went

`:free-energy`'s `:note` read "Computed and stored each tick; NO live consumer
reads it", and its `:retired :not-claimed` still reads "The scalar F is STILL
COMPUTED AND STORED by the code above, as `:variational-free-energy` on every
record". Both describe 2026-09-01. `:I5` slice (c) deleted the producer later
the same day (C473), so at HEAD the scalar is neither computed nor stored —
`free_energy.clj:7-12`, `trace.clj:241-247`, `free_energy_test.clj:293-301`.

The correction is written into `:note` only. **`:retired` is a ruling record**
— `:by "joe (J2)"` — and this seat does not edit one; `:note` names the
`:not-claimed` line as overtaken and leaves it standing, which is the same
shape TN §9a gives a signature that has come to describe a file state it never
saw. No ruling was written: `aif-equations.edn :choices` and
`control-map-edges.edn :decisions` are byte-unchanged.

## 6. The negative control was anchored to the defect

`negative_controls.sh` 4e and 4e2 planted their bad pointer by `sed`-ing for
the literal `:code "war_machine.clj:4368-4379"` — `:prediction-error`'s stale
pointer. Repairing that pointer made the `sed` match nothing; the unmutated
copy passed `pointer_check`; and the control reported
`FAIL -- pointer_check accepted war_machine.clj:99999`, a message about the
checker, for a change in the registry. **A control anchored to a defect stops
working when the defect is fixed, and fails in the direction that blames the
fix.**

Both controls now synthesise the planted pointer instead of substituting for a
literal that has to exist:
`edn "$T/eq.edn" "$T/eq-badptr.edn" '#(assoc-in % [:equations 0 :code] "war_machine.clj:99999")'`,
using the `edn` helper already in the file (`negative_controls.sh:17`) on the
temp copy, as every other control there does. Mutation-checked directly, not
inferred from a green suite: the planted value reads back as
`war_machine.clj:99999`, `pointer_check` exits 1 and prints
`UNRESOLVED war_machine.clj:99999 (end beyond file)`. Counts unchanged at 88
negative / 45 positive, the same as C521 reported.

## 7. Found and not repaired

**(a) `pointer_check.bb` cannot catch this class, and adding roots will not
help.** It checks that a file exists and that `A <= B <= linecount`. Every one
of the 18 misses satisfies that. A check that caught them would have to anchor
each pointer to *content* — the enclosing `defn` name, or a hash of the cited
lines — and that is a change to the checker plus a format change to every
`:code` string in the registry. It is the obvious next slice and it is not
this one.

**(b) The same class, a fourth site.** C517 §4(a) found
`CONVERGENCE-draft.edn`'s spec-leg pointers off by one on 14 of 18 rows, each
landing on "a neighbouring table row of the same shape, so it resolves and
reads plausibly rather than failing" — the same failure mode as
`policy.clj:413-421` here. That file is leg 3's subject and leg 3 adopts it;
repairing it here would pre-empt that slice. Left where C517 put it.

**(c) Only the first range per file in a `:code` string was machine-checked
before this slice**, because `pointer_check`'s regex needs the filename
immediately before the colon and the house style wrote `file.clj:A-B, C-D`.
Every pointer written by this sweep repeats its filename: the ten repaired
rows now carry **48 named pointers**, all of them range-checked, where before
they carried 33 of which fewer than half were. Four bare sub-references remain
deliberately — `:141-142` and `:208-213` (twice) and `:528` — each pointing
*inside* a range the same sentence has already named with its filename. The
two untouched rows keep their two pre-existing bare references (`:68`,
`:486-487`).

`:free-energy`'s new `:code` names five pointers, two of which are the stale
ones it used to assert (`free_energy.clj:184-205`, `war_machine.clj:4913-4915`);
they are quoted there as history, labelled as such, and resolve, which is the
whole point being made about them.

**(d) Slice 4's other carry-forwards are untouched**: the published section is
still four rows behind (TN §9a gates regeneration, and this slice ran no
generator), and no `gen_aif_dag.bb` was run.

## 8. Gates

- `bb p4ng/empirics-futon/pointer_check.bb` — 1576 pointers in 3 files, 0
  unresolved, exit 0.
- `bash p4ng/empirics-futon/negative_controls.sh` — PASS, 88 negative / 45
  positive, shared registries untouched, exit 0. Re-run after the 4e/4e2
  re-anchoring, and each re-anchored control mutation-checked by hand.
- `check-parens` (`futon4/dev/check-parens.el`) on `aif-equations.edn` and
  `worklist.edn` — OK, exit 0.
- clj-kondo: **no subject this slice.** Nothing under `src/` or `scripts/`
  changed; the three files this slice touches are an `.edn` registry, a
  `.sh` control script and this report. `bash -n` on the control script is
  implied by its having run to a PASS.
- `bb worklist_check.bb` and `bb ancestry_check.bb` — run after the ledger
  commit; see the row's `:evidence`.

**Not claimed.** No live tick, no run lock, nothing written under `data/`. No
Lean, no mathlib4 commit, no carrier declared or changed. No regeneration into
a publish. No ruling. The six remaining class-(a) quantities (`:observe`,
`:belief-state`, `:policy-set`, `:depth`, `:temperature`, `:action`) are
untouched as *Lean* subjects — this slice repaired their pointers and states
nothing about them in Lean. Legs 2 and 3 remain unstarted.
