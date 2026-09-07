# C553 — `:F12` slice 15: the measured cost of D3's three arms

**What this is.** A measurement of D3, the question whether an O4 witness may
be reached using an encoding written after the law. The probe is
`f12_d3_encoding_check.clj:1`; its deterministic artifact is
`runs/F12-organise/10-d3-encoding.edn:1`.

**What this is NOT.** It takes no ruling, registers no choice, changes no real
`futon3` or `futon3c` file, and does not run the machine. Every construction and
rule-table plant is made in a temporary copy
(`f12_d3_encoding_check.clj:35-53`, `:89-104`).

## Premises recomputed

The slice-3 report regenerates byte-for-byte, the maximum number of
rule-carrying members remains **1**, and the gate remains red with **3 of 4**
THEN correspondences failing and verdict
`:rule-does-not-encode-an-authored-then`
(`runs/F12-organise/10-d3-encoding.edn:264-272`). The guard says unused rules
prevent fitting at `futon3c:scripts/zaif_cascade_gate.clj:53-57`; the unused
rules are computed by membership at
`futon3c:scripts/zaif_cascade_gate.clj:479-480`, and the guard fails only when
that collection is empty at `futon3c:scripts/zaif_cascade_gate.clj:585`.

## Arm A — write a construction after the law

Planting `war-machine/ambient-pattern-retrieval` into the floor arm and bumping
its recorded member count is verified before measurement
(`f12_d3_encoding_check.clj:89-104`). The maximum rule-carrying count moves
**1 → 2**, but over the same **29** primary rounds the primary O4 result remains
**false**: acting order unchanged, score **14 → 14**, with zero primary
contentions (`runs/F12-organise/10-d3-encoding.edn:1-12`). Arm A alone therefore
buys no O4 witness unless D2 also changes which rounds O4 reads.

What stayed the same: the cohort still has **103** rounds, the primary
denominator still has **29**, and the two rules still contend on **0** primary
rounds (`runs/F12-organise/10-d3-encoding.edn:9-12`).

## Arm B — write a play-grain encoding after the law

The fifth rule encodes the authored THEN of the already-carried
`aif/status-gated-belief-update` pattern, whose cited span is
`futon3:library/aif/status-gated-belief-update.flexiarg:26-30`. The planted
source and its read-back verification are at
`f12_d3_encoding_check.clj:24-40`; the copy is placed first on the worker's
classpath at `f12_d3_encoding_check.clj:42-53`.

- **B1:** the un-fitting guard stays silent. Three rules remain outside the
  cascade (`runs/F12-organise/10-d3-encoding.edn:167-170`), so adding a rule to
  make the law testable does not trigger the stated guard.
- **B2:** the fifth rule is live on **102** rounds
  (`runs/F12-organise/10-d3-encoding.edn:32-134`), including all **29** primary
  rounds (`:137-166`), but it contends with the existing member-carried rule on
  **0** primary rounds (`:30`).
- **B3:** the primary O4 verdict moves from **not exercised** to **exercised,
  false**. Swapping precedence changes neither the primary acting order nor the
  score, which remains **15 → 15**
  (`runs/F12-organise/10-d3-encoding.edn:136` and `:15` for the two verdict
  states, `:31` and `:177-244` for the O4 row it is read from).

What stayed the same: the primary denominator remains **29**, the pre-existing
THEN-correspondence failure count remains **3**, and
`:rules-not-in-the-cascade` remains non-empty
(`runs/F12-organise/10-d3-encoding.edn:172-176`). Thus the moved O4 status is
located at the new member-carried rule, not at a denominator or gate repair.

## Arm C — refuse both changes

Across the same **nine** cascade records, recorded runs exercising O4 remain
**zero** (`runs/F12-organise/10-d3-encoding.edn:245-246`). Refusing both arms
therefore leaves D3 with no recorded exercise at all.

## Negative controls

Every plant is read back before its worker runs; the verifier is
`f12_d3_encoding_check.clj:35-40`, and failure of any expected read-back is a
hard probe failure at `f12_d3_encoding_check.clj:210-216`.

| planted copy | verified observation | verdict movement |
|---|---|---|
| fifth rule renamed to a nonmember | marker and new id present | O4 `exercised false → not exercised` |
| fifth rule antecedent replaced by `false` | marker and dead antecedent present | live rounds `102 → 0`; probe verdict becomes `planted-rule-is-live-on-no-round` |
| existing member rule broadened to be live | marker, broad guard, and emitted arm present | primary O4 `false → true` |
| every rule id replaced by a cascade member | all replacement ids present | outside rules `3 → 0`; un-fitting guard fires |

The exact observed results are in
`runs/F12-organise/10-d3-encoding.edn:247-263`; the independent worker plants
are constructed at `f12_d3_encoding_check.clj:150-189`.

## Gates and unsettled question

The probe uses exit convention 0-pass/1-fail
(`f12_d3_encoding_check.clj:235-244`) and fails if any premise, arm result, or
control moves (`f12_d3_encoding_check.clj:191-216`). Two unchanged-tree runs
produce byte-identical artifacts. `clj-kondo`, `check-parens`, the repository
negative controls, and the pointer checker pass. The probe wrote nothing into
either source repository: `futon3c:scripts/zaif_cascade_gate.clj` and every file
under `futon3:checks/` are unmodified after the run (`git status --short` on
those paths is empty). Both repositories do carry unrelated uncommitted work
from other lanes, so no repository-wide cleanliness is claimed here.

This slice does not decide whether either after-the-law intervention is
admissible. It measures that Arm A still needs D2, Arm B evades the present
un-fitting guard while making O4 exercisable but false, and Arm C leaves zero
recorded exercises. The owner can now register the choice and take any ruling.
