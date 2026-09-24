# I3 — what the outer loop actually did, from the records

Excursion E-outer-loop (`holes/E-outer-loop.md`), investigation I3.
Author: kimi-5, 2026-09-24. Discovery only: no clicks, no writes under
`data/`, no shared-JVM loads; all extraction ran in fresh `clojure -M`
processes. Read-only on `src/`, `scripts/`, `data/`, `resources/`.

**Method.** Every number below is printed by the committed script
`i3_extract.clj` (run from `/home/joe/code/futon2`) over the named record
files listed with sha256-8 in §6, or by the follow-up greps quoted with
paths. The script's full per-tick output is committed alongside as
`i3-report.edn`. Nothing is taken from a docstring or a note.

## 0. Where the records are, and how many

The pre-H5b judge's tick records are the **`data/wm-trace/wm-trace-*.edn`
files**: each file is a sequence of top-level EDN forms, one judge tick per
form, carrying `:ranked-actions`, `:policy-support-exclusions`, and
`:decision`. I found **60 files dated 2026-05-18 through 2026-09-12** (the
cutoff is H5b, 2026-09-17, commit `5d55e7a0`), containing **897 ticks**, all
readable, zero read errors. The next dated trace files are 2026-09-21+,
post-H5b, and are excluded.

Two negative findings on location:

- `:admissible-actions` — the feasibility-excluded-from set named in
  `holes/E-outer-loop.md` §3 — **appears in no trace file** (grep over
  `data/wm-trace/*.edn` found none). What the ticks carry is
  `:ranked-actions` (the ranked survivors) and `:policy-support-exclusions`.
  The admissible set's size is therefore reported as the size of
  `:ranked-actions`, with that caveat.
- The full-loop dossiers — `data/wm-full-loop/wm-outer-loop-40..46-v1`
  (2026-07-21..27) and `data/wm-full-loop-machinery-47..57` (2026-09-13..15,
  the pre-H5b ones) — are per-click cohort records (`activation.edn`,
  `NNN-time-step/selection/construction/dispatch/build/adjudication/closed`).
  Grep finds **no `:ranked-actions`** in them; they record cascade clicks,
  not outer-loop proposals, and answer none of questions 1–3.

The records carry **no proposer field** on ranked actions. Proposer counts
below use the script's stated inference rule (action `:type`, plus the
`:rationale` prefix `tension ...` for tension-derived rows); 453
`:apply-cascade` rows (a late-appearing advisory lane) match no rule and are
counted `:unattributed`, not guessed.

## 1. What was proposed (897 ticks)

Mean admissible-ranked set size 132.9 per tick (min 4, max 218).
Totals over all ticks:

| Action type | proposed | inferred proposer |
|---|---:|---|
| `:advance-mission` | 57,354 | mission 109,896 |
| `:open-mission` | 53,869 | sorry 3,693 |
| `:address-sorry` | 3,693 | bootstrap 2,585 |
| `:learn-action-class` | 1,688 | tension 1,327 |
| `:fire-pattern` | 1,266 | pattern 1,266 |
| `:no-op` | 897 | unattributed 453 |
| `:apply-cascade` | 453 | |

Feasibility exclusions: **137 ticks** record `:policy-support-exclusions`,
always exactly **4** per tick, **548 exclusions total, every one with reason
`:mission-absent-from-capability-graph`** — all excluded type
`:open-mission`. No other exclusion reason appears anywhere in the window.
So feasibility as recorded was a fixed four-mission exclusion list, not a
live filter: the same four, every time it appears.

## 2. What was chosen (`:decision :action :type`, 897 ticks)

| chosen type | ticks | share |
|---|---:|---:|
| `:address-sorry` | 459 | 51% |
| `:advance-mission` | 196 | 22% |
| `:learn-action-class` | 146 | 16% |
| `:open-mission` | 96 | 11% |

**`:no-op` was never chosen. No ticket action was ever chosen. No
`:fire-pattern` was ever chosen**, despite tickets, patterns and the no-op
being proposed on essentially every tick (`:no-op` present in all 897).
Chosen targets are only **17 distinct** values across 897 ticks: 5 sorrys,
11 missions, plus the untargeted `:learn-action-class`.

## 3. How close the choices were

Only 137 ticks (2026-07-14 onward) carry comparable `:controller-score` on
both the ranked list and the decision; earlier ticks record no score on the
decision, so closeness is unknowable there — reported as absence, per the
rules.

- The chosen action is the `:rank 1` entry in **867 of 897** ticks. The
  exceptions are 30 ticks — all on 2026-07-14/15/17/21, the plateau days —
  with chosen ranks 3, 5, 9, 10, 54, 59 (×12), 72, 75, 109: softmax sampling
  landing deep in a flat score field.
- Where scores are comparable, chosen score equals the rank-1 score in
  **109 of 137** ticks (gap 0.0), and is *above* the recorded rank-1 entry's
  score in 28 ticks — i.e. on those ticks the recorded `:rank` ordering is
  not the controller-score ordering, so the record's own rank and score
  disagree; flagged, not resolved.
- Near-ties at threshold |Δscore| ≤ 0.01 around the top score: 106 ticks
  have the top score standing alone; 5 ticks have 2 actions that close; 6
  have 3; and **20 ticks (all 2026-07-14/15) have 23 actions within 0.01 of
  the top** — a plateau exactly of the kind the pre-H5b ladder comment
  anticipated ("55 candidates at one :mission-value-factor").
- No tick carries a tie-break field (`:tie-break-rule` absent from all 897
  decisions; `:selection-law` appears only in the last 15 ticks, 09-12).
  How exact ties were broken is **not in the record**.

## 4. Did the chosen work get done

**Sorrys (459 + the learn ticks' context).** All five chosen sorry targets
(`:sorry/pudding-g1-arrow-witness-binding` ×320,
`:sorry/wm-aif-substrate-addressability` ×108,
`:sorry/r3a-likelihood-coupling-density` ×17,
`:sorry/r3a-likelihood-loop-health` ×13, `:sorry/stub-lifts-pending-aif-edn`
×1) are **legacy `futon2-d` sorrys**. Grep finds none of the five in
`mathlib4/DarkTower`, `futon2/src`, or `futon2/holes` code at HEAD — the
current tree contains them only inside
`holes/labs/M-legacy-sorry-cleanup/legacy-sorries-snapshot.edn`, the legacy
cleanup dossier (which records e.g. `stub-lifts-pending-aif-edn` with
`:sorry/resolved-at "2026-05-30"` and `:resolved-by-cg` entries). So the
loop's dominant choice — 320 ticks on one sorry — was work on a **dead
substrate**: whatever ticks "did", the target no longer exists in the tree
the machine now formalises. No tick record links a chosen sorry to a
resolution event; none found.

**Missions.** Of the 11 chosen missions, only 3 have mission files at HEAD,
all still open: `M-aif-policy-conditioned-eig` ("IDENTIFY agreed in
principle; pure kernel instantiated; generative contract open"),
`M-wm-aif-policy-grain-compliance` ("INSTANTIATE — … persistence/live seams
remain open"), `M-zaif-harness-v1` ("OPEN — …"). The other 8
(`M-first-flights`, `M-canon-fingerprint-store`,
`M-emacs-cursor-peripheral`, `M-capability-star-map`, `M-learning-loop`,
`M-futonzero-mvp`, `M-shared-memory-control-build-test`,
`M-expressions-of-interest`) have **no file under `futon2/holes/missions/`
at HEAD and no deleted-file history there** (git log `--diff-filter=D`
empty) — they survive only as references in lab notes (e.g.
`holes/overnight-flights-2026-07-06.md`). No full-loop close record
connecting a chosen mission to a completion was found; the dossiers record
clicks, not mission advancement.

## 5. Did choices move over time

No. Consecutive-identical-choice streaks (ticks ordered by `:timestamp`):
the top streaks are **306, 128, 108, 69, 44, 29, 21, 17 ticks**. The 306-
and 128-tick streaks are both `:address-sorry
:sorry/pudding-g1-arrow-witness-binding` — together with a further 108-tick
streak of `:sorry/wm-aif-substrate-addressability`, the loop chose the same
two legacy sorrys for weeks at a stretch. By era: 05-18..06-30 chose
sorrys 370 / learn-action-class 139 / open-mission 96 / advance-mission 0;
07-01..09-12 chose sorrys 89 / learn 7 / open-mission 0 / **advance-mission
196** — the mix shifted once (toward mission advancement in July), but
within each era the same target wins tick after tick.

## 6. Record files used (sha256-8)

| `data/wm-trace/wm-trace-2026-05-18.edn` | c0ca99ac |
| `data/wm-trace/wm-trace-2026-05-19.edn` | 03ae6796 |
| `data/wm-trace/wm-trace-2026-05-21.edn` | 1ebc49ee |
| `data/wm-trace/wm-trace-2026-05-22.edn` | 93ec1642 |
| `data/wm-trace/wm-trace-2026-05-23.edn` | 78fe7aaf |
| `data/wm-trace/wm-trace-2026-05-24.edn` | 169fa5ab |
| `data/wm-trace/wm-trace-2026-05-25.edn` | 4dbe5242 |
| `data/wm-trace/wm-trace-2026-05-26.edn` | 6dd57154 |
| `data/wm-trace/wm-trace-2026-05-27.edn` | 15fc8323 |
| `data/wm-trace/wm-trace-2026-05-30.edn` | 39cd226f |
| `data/wm-trace/wm-trace-2026-05-31.edn` | 61f23700 |
| `data/wm-trace/wm-trace-2026-06-01.edn` | 97f4ea22 |
| `data/wm-trace/wm-trace-2026-06-02.edn` | fb28f770 |
| `data/wm-trace/wm-trace-2026-06-03.edn` | e3f025d0 |
| `data/wm-trace/wm-trace-2026-06-04.edn` | 56ff07b8 |
| `data/wm-trace/wm-trace-2026-06-05.edn` | 4c0309f4 |
| `data/wm-trace/wm-trace-2026-06-06.edn` | 8b156227 |
| `data/wm-trace/wm-trace-2026-06-07.edn` | 068acb1b |
| `data/wm-trace/wm-trace-2026-06-08.edn` | 254dac37 |
| `data/wm-trace/wm-trace-2026-06-09.edn` | 18bbeaa1 |
| `data/wm-trace/wm-trace-2026-06-10.edn` | badf4f1f |
| `data/wm-trace/wm-trace-2026-06-12.edn` | 97cf83a7 |
| `data/wm-trace/wm-trace-2026-06-13.edn` | 676f19c4 |
| `data/wm-trace/wm-trace-2026-06-14.edn` | 031887f2 |
| `data/wm-trace/wm-trace-2026-06-15.edn` | a537170a |
| `data/wm-trace/wm-trace-2026-06-16.edn` | 58f60fe9 |
| `data/wm-trace/wm-trace-2026-06-17.edn` | 2fbd2c3b |
| `data/wm-trace/wm-trace-2026-06-18.edn` | 123a7040 |
| `data/wm-trace/wm-trace-2026-06-21.edn` | a38d588d |
| `data/wm-trace/wm-trace-2026-06-22.edn` | f8b22115 |
| `data/wm-trace/wm-trace-2026-06-23.edn` | ead3f793 |
| `data/wm-trace/wm-trace-2026-06-24.edn` | 6838cbfc |
| `data/wm-trace/wm-trace-2026-06-25.edn` | cf1dece9 |
| `data/wm-trace/wm-trace-2026-06-26.edn` | 4c759269 |
| `data/wm-trace/wm-trace-2026-06-27.edn` | 40ae8143 |
| `data/wm-trace/wm-trace-2026-06-28.edn` | 55bd67e1 |
| `data/wm-trace/wm-trace-2026-06-29.edn` | 1e156905 |
| `data/wm-trace/wm-trace-2026-06-30.edn` | 43c07fa2 |
| `data/wm-trace/wm-trace-2026-07-01.edn` | ac294990 |
| `data/wm-trace/wm-trace-2026-07-02.edn` | c2fe4e94 |
| `data/wm-trace/wm-trace-2026-07-03.edn` | 5cae8b2b |
| `data/wm-trace/wm-trace-2026-07-04.edn` | f887895d |
| `data/wm-trace/wm-trace-2026-07-05.edn` | 1e49f622 |
| `data/wm-trace/wm-trace-2026-07-06.edn` | 84e7253a |
| `data/wm-trace/wm-trace-2026-07-09.edn` | 3c3f332d |
| `data/wm-trace/wm-trace-2026-07-14.edn` | 8c56ef7f |
| `data/wm-trace/wm-trace-2026-07-15.edn` | 5294d067 |
| `data/wm-trace/wm-trace-2026-07-16.edn` | 5c4043d5 |
| `data/wm-trace/wm-trace-2026-07-17.edn` | d86f0874 |
| `data/wm-trace/wm-trace-2026-07-18.edn` | fd00d9da |
| `data/wm-trace/wm-trace-2026-07-19.edn` | ec16b3f2 |
| `data/wm-trace/wm-trace-2026-07-21.edn` | 3d0910af |
| `data/wm-trace/wm-trace-2026-08-30.edn` | 708e8939 |
| `data/wm-trace/wm-trace-2026-08-31.edn` | af50766f |
| `data/wm-trace/wm-trace-2026-09-01.edn` | 565690ac |
| `data/wm-trace/wm-trace-2026-09-02.edn` | 935dcb81 |
| `data/wm-trace/wm-trace-2026-09-04.edn` | f343432d |
| `data/wm-trace/wm-trace-2026-09-07.edn` | 71bc68e5 |
| `data/wm-trace/wm-trace-2026-09-11.edn` | 8f23cd6c |
| `data/wm-trace/wm-trace-2026-09-12.edn` | 3b25d2d4 |

Follow-up greps quoted in §4: `holes/labs/M-legacy-sorry-cleanup/legacy-sorries-snapshot.edn`,
`holes/missions/M-aif-policy-conditioned-eig.md`,
`holes/missions/M-wm-aif-policy-grain-compliance.md`,
`holes/missions/M-zaif-harness-v1.md`; absence greps over
`mathlib4/DarkTower`, `futon2/src`, `futon2/holes` (no hits for any of the
five sorry ids), and `git log --diff-filter=D` on the eight missing mission
files (no history).

## Caveats

- Tick timestamps within a file can be non-monotonic in wall-clock file
  order; Q5 orders by the `:timestamp` field, which the script preserves.
- Proposer attribution is an inference rule over `:type`/`:rationale`,
  stated in the script; the records carry no proposer identity. The 453
  `:apply-cascade` rows are counted `:unattributed`.
- `:admissible-actions` never appears in the records; admissible-set size
  is `:ranked-actions` size.
- 760 of 897 ticks record no comparable decision score; closeness claims
  cover only the 137 ticks that do.
