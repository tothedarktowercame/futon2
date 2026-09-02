# Gauge wiring belongs in Figure 3 (Joe, 2026-09-02 evening)

Joe: "'wiring the gauges' should definitely be recorded somewhere prominent,
eg. maybe Figure 3. Build state of the machine this paper describes, in one
view." This note holds the directive and the design sketch until the loop
pauses and the row (U19) can be minted without contending the worklist.

## What "the gauges" are

U18(d)'s bindings: each completion criterion's declared :observable resolved
to a READABLE source (criterion-1 -> worklist acceptance state via
worklist_check; criterion-2 -> U8 gate result; criterion-3 -> registry
artifact presence). A criterion without a wired gauge is typed
:unmeasurable on every read (mission_c.clj, C130 discipline) — so "wired /
not wired" is already machine-derivable, never hand-asserted.

## Where it goes (the house path, WR-8)

Figure 3 = the wm-status infographic, generated at every publish from a
committed status receipt: futon2 scripts/wm_status_report.py --receipt ->
p4ng empirics-futon/gen_status_infographic.py. So the recording is NOT a
hand edit to the figure; it is:

1. wm_status_report.py gains a component (working name
   :mission-criteria-gauges): run mission_c/read-criteria over the missions
   that declare criteria (today: M-zaif-harness-v1, 3 criteria;
   M-expressions-of-interest, 6) with the supplied observables map, and
   report {:missions n :criteria m :measurable k} + per-mission detail in
   the receipt (checked against the text report, per WR-8).
2. gen_status_infographic.py renders it as the twelfth component with the
   existing shape vocabulary: hollow when k=0 (today's honest state),
   half-filled when 0<k<m, filled when k=m. The word it carries:
   "gauges k/m".
3. Fixtures: the py test's fixture pair regenerates through the test's own
   write-then-compare path; the dated fixture names stay.

Honest initial render: "gauges 0/9" hollow — that is the true build state
tonight, and U18(d) landing is what moves it, which is exactly why Joe wants
it in the one-view figure.

## U19 mint (verbatim, at next loop pause)

:class :I, :covers-key :none (touches no registry; the receipt is WR-8's
own artifact), depends-on [:U18] NOT required — the component reports 0/9
honestly before U18(d) lands and moves by itself after.
Acceptance: receipt carries the component with per-mission detail; the
infographic renders it in the shape vocabulary with the count word; figure
--check green; paper rebuilds clean; the py test exercises the component
through the fixture path; no hand-maintained number anywhere in the chain.
