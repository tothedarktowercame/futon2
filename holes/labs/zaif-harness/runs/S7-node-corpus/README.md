# S7 node corpus — cross-link (no copies here)

Written by wm-contract worklist `:U25`, which took zaif `:S7`'s remainder.

**The corpus is not in this directory.** It lives at

```
futon2/holes/labs/wm-contract/runs/U12-c-mis-falsifier/node-fixtures/
```

39 fixtures + `README.md`, one file per `(run-id, node)`, harvested from live
`data/wm-trace/` tick records by `holes/labs/wm-contract/u12_c_mis_falsifier.clj`.
Three run ids × 13 nodes: 30 present, 9 typed absences. Every value is a record
field with the producer copied from the record's own `:route`.

What is here instead is `cross-link.edn`: the canonical path, the per-fixture
sha256, a digest over the whole listing, and the list of zaif rows that read the
corpus and the fixture field each one reads. Copies would have made a second
corpus that drifts apart from the first without saying so; the digest is the
thing a copy cannot give you. To check the corpus has not moved under this
pointer, from the futon2 repository root:

```
cd holes/labs/wm-contract/runs/U12-c-mis-falsifier/node-fixtures \
  && sha256sum *.edn README.md | sort -k2 | sha256sum
```

and compare with `:corpus-digest`.

## Rows on this board that read it

| row | what it takes | fixture field |
|---|---|---|
| `:S7` | this cross-link IS half (b) of its remainder | — |
| `:U7` | what a real tick's observation / precision / prediction-error records look like | `[:observation]`, `[:precision-state]`, `[:prediction-errors]` |
| `:U9` | R7's per-channel `:precision` — the live counterpart to the R7 precision table | `[:precision-state]` |
| `:U10` | the coverage matrix cites these fixtures rather than re-extracting | all |

`:U9`'s number, since it is the one that is easy to get wrong: R7 answers **8**
of R2's **14** channels on all three ticks. The six with no precision entry are
`:consulting-pct :depositing-signal :loop-health :mathematics-pct
:portfolio-pct :stack-pct` — absent from the precision state, not present at
precision 0.

## What U25 re-grounded with it (half (a))

`futon2/test/futon2/aif/zaif_full_loop_test.clj` (the U6 suite) planted four
things at R4/R8 because zaif v0 declares no observation model. Three of them are
quantities the WM produces every tick and the corpus has measured, so the suite
now reads them from the record; two have no counterpart anywhere and stay
planted with the reason recorded. The split is data in that file
(`plant-real-split`) and is asserted, not asserted-about:

| declaration | U6 | now | why |
|---|---|---|---|
| R4 channel placement | `:mission-health` | **plant kept** | nothing in `zaif_controller.clj` names an observation channel |
| R4 level | `0.5` (bottom of the preference range) | `0.4062802408811222` | `0a18c4f7-R8.edn` `:mission-health :predicted-mean` |
| R4 dispersion | `0.01` (the variance floor) | `0.941315784435399` | `0a18c4f7-R8.edn` `:mission-health :predicted-variance` |
| R4 per-arm delta | the arm's G-term added to the level | **plant kept** | `forward_model.clj:25-31` excludes all four arms; no record anywhere is conditioned on a zaif arm |
| R8 realised outcome | `0.5` (= the level: "no gain") | `0.023376623376623377` | `0a18c4f7-R8.edn` `:mission-health :observed`, equal to `0a18c4f7-R2.edn`'s observation |

**The result the re-grounding moves.** U6's headline at R16 was *three laws,
three arms*: zaif chose `:retrieve`, the controller-head law chose `:ask`, the
full-score law chose `:act`, and the arm zaif chose was the WM's least preferred
at G. On the re-grounded level and dispersion all three laws choose `:retrieve`,
the full-score law no longer moves off the controller head, and `:retrieve` goes
from last to first at G. That finding was carried by the two invented numbers.

**The result it does not move.** R5's ambiguity term still cannot discriminate
between the arms — zaif declares no per-arm variance, so the grounded variance
is the same number on every arm exactly as the floor was. Its value turns over
(−0.884 → +1.389) but its non-discrimination is a property of zaif, not of the
plant. And re-running the negative control with level and dispersion grounded on
each of the corpus's seven R8 channels still yields three different R5 orders and
two different head choices: the channel placement, which stays planted, is still
carrying the outcome. Nothing here is a measurement of which arm is right.
