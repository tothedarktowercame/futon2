# C504 — U50: placing R3a, and the column argument

Trigger: Joe, 2026-09-04, reading plop-2026 Figure 2 on his phone — "I don't
know what R3a is, or why it's absent, or why it's drawn... is very confusing."
R3a was rendering in a red box at x=1110, off to the right of ACT, labelled
"stage absent from registry", with two edges running back across the whole
drawing to R2 and R7. Background and receipts: `NOTE-R3a-placement.md`.

## The argued column, in one sentence

**R3a is in BELIEVE because it reads the BELIEF and not only the world:**
`futon2/scripts/futon2/report/war_machine.clj:5971` calls
`futon2.aif.free-energy/channel-prediction-error` with the observation **and**
`(get predictions ch)`, where `predictions` is `belief/predict-observation` over
the loop's current belief (`war_machine.clj:5959-5966`); the call sits inside
the R3 belief-update micro-step loop (`war_machine.clj:5955-6098`) and re-runs
as that belief moves; and its only measured consumer is R7 at
`war_machine.clj:5989` (`futon2.aif.precision/update-precision-state`), which
the drawing puts in BELIEVE.

PERCEIVE was the other candidate (R3a computes a mismatch, as R8 does). What
rules it out is the second argument: PERCEIVE is where the world is read, and
`eps = o - mu` cannot be computed without `mu`, which is a function of the
current belief and of nothing the observation carries. Producer:
`futon2/src/futon2/aif/free_energy.clj:203`.

## The drawing already answers it, and it answers it the same way

Joe drew R3a onto the base figure in p4ng `0598d19` (2026-08-31), at
`aif-control-map-paper.svg:123-125` — `translate(250 235)`, a 70-wide box, so
centre x=285: **84px from the BELIEVE header at x=369 and 146px from PERCEIVE at
x=139**, sitting between R3 (y=190) and R7 (y=300), above the y=500 loop line.

This was not visible from the placement's own side, because `control-stages.edn`
is generated from `aif-control-map-futon.svg`, which `gen_wr_overlay.bb` derives
from the paper drawing and which has not been regenerated since p4ng `3e375d5`
(2026-08-25) — before that hand. So the row's supersession clause ("if Joe later
draws R3a onto the base figure, the drawing-derived assignment supersedes the
code-side one") is already live, and the code-side placement is a stand-in for a
drawing-derived one that exists but is not yet reachable.

Rather than say that only in prose, `gen_control_stages.py` now **refuses** the
code-side entry the day the drawing it reads carries the node
(`check_code_side_members`, negative control 7d). Control 7e runs the plant
through the generator with `MEASURED_MEMBERS` emptied and shows the drawing's own
geometry emits `{:node "R3a" :stage "BELIEVE" :band :loop` — the same column the
code-side basis argues, from an independent source.

## What this does NOT decide

`aif-equations.edn:77` hosts the eps equation at `:node :R8`, which is drawn in
PERCEIVE, and `war_machine.clj:6109` tags this same call site `:R8` for that
reason — an attribution made when R3a was not a drawn box, and the reason
`gen_aif_dag.bb`'s "runs an equation or is plumbing" check stopped on R3a.
**Which box hosts eps is a different question from which column R3a is in**, and
this row does not decide it. It is not resolved by calling R3a plumbing either:
`:plumbing` means "runs no equation", which is false here. `gen_aif_dag.bb` now
accepts a third, typed branch — a code-side placement carrying a `:basis` — so
accounting stays total without asserting something false. Reconciling the eps
host with the drawn R3a box is its own row, and it is a `:choices` question.

## Checked, not assumed

- Every DAG artifact is byte-identical before and after **except the
  control-stages sha8 in the stamp**: `aif-equation-dag.svg`,
  `aif-conformance.edn` and `sec-aif-conformance-generated.tex` differ only at
  `control-stages 81325431 -> ae4c4c58`. R3a draws no box in the equation DAG and
  joins no edge population. (Run against a probe copy of `gen_aif_dag.bb` with a
  `AIF_STAGES` override, into a temp `AIF_OUT`; the override is not in the
  committed script and nothing was regenerated into the publish — TN 9a.)
- `aif-control-map-live.svg` places R3a at x=320 (the BELIEVE column centre),
  y=298.3, between R3 (y=201.7) and R7 (y=395) in the loop band, with the loop
  fill `#e9f5f3` and the ordinary `#256b67` stroke — not the unplaced `#b42318`.
- The unplaced list is empty, and that is now a bare exit (`--unplaced`) rather
  than something a reader has to see in the figure.
- The red-box fallback still fires: an unknown node id planted into the node
  list renders "stage absent from registry" in `#b42318` (control 4t).
