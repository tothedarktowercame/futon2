# Rulings walkthrough — 2026-09-12 (claude-15 session, Box 2 / Box 3)

Recorded by claude-15 from Joe's dictated rulings in session. Earlier
same-day rulings (R17 target semantics; R6->R16 correspondence bar)
are recorded at `aif-equations.edn :choices :r17-target-semantics`
(futon2 b0b2c676) and are not restated here.

## Item 1 — Box 3 presentation: named-as-terminal must be visible

Joe, on the per-node readiness table (Box 3,
`sec-rnode-dossiers-generated.tex`): "in many cases, named [is] the
terminal state. It would be really good if the table was presented in
a way that made that obvious, because otherwise the table makes me
extremely nervous that we're presenting a bunch of stuff that's
just... fluff. ... Some stuff in here is justifiably structured in a
way that is... naturally type theoretic. Now, that doesn't mean that
we couldn't create a lean specification for those nodes, by the way.
Even if it's not part of AIF — but if we're trying to make something
that's AIF complete, we're not obliged to do that. So I would say Box
3 needs to be reconsidered from a point of view of not making me
extremely nervous. And making some other reader understand that we're
basically done with the work that's described in the paper."

Ruling, operationalized (implementation rides the instrument-repair
lane, after the codex-17 join-extension TN lands, one generator
packet):

1. Each node row carries its OBLIGATED TERMINAL RUNG ("ceiling"),
   derived from the registry: plumbing nodes -> named is terminal
   (their real ladder is the process-assurance lifecycle census, which
   the presentation should make primary for them, not a footnote);
   equation-bearing nodes -> the AIF ladder applies.
2. The headline count becomes obligated-state accounting ("X of Y
   nodes at their obligated state; Z short, named individually"), not
   a raw rung tally ("15 named") that reads as unfinished work when
   much of it is finished work at its designed ceiling.
3. Plumbing nodes get a note that a Lean specification is POSSIBLE
   (naturally type-theoretic) but NOT OBLIGED for AIF-completeness —
   optional formalization, distinguished from obligation.
4. Nothing in the re-presentation may soften the honest shortfalls:
   the seven under-reported loop nodes (contract-join gap), the six
   uninhabited constructions, R9's 0/7 lifecycle, and R3a's misfiled
   population remain visible as the actual work remaining.

## Item 2 — The outer loop: acknowledged lost, evidence attached

Joe: "the only problem with that is that... last I checked, we don't
have a working outer loop for our AIF implementation, whereas in the
PLOP 2026 paper we did. So all of this has kind of gotten lost at
this moment, as far as I can tell."

Verified same session (claude-15):

- The live crontab is EMPTY. The entries recorded in
  `futon0/data/cron-jobs.edn` (`:wm-scheduled` full-loop opportunity,
  contract row "WM R10 — Live operation"; `:wm-outer-loop` R12
  hyperparameter inference, `0 4 * * *`) are not installed.
- `logs/wm-scheduled.log` last entry 2026-07-15; last recorded
  scheduled outcome a construction failure.
- `logs/wm-outer-loop.log` last entry 2026-07-14, terminating in
  ERROR; posterior held at prior Beta(1,1), "0 applied".
- The last outer-loop cohort is `wm-outer-loop-46-v1`
  (2026-07-27); everything since is on-demand clicks
  (`duree-click-on-demand`) and RUN4 cohort machinery.
- Box 3's process-assurance column already carries this honestly:
  R10 2/7 (absent: parked, returned, checked, recorded, surfaced),
  R12 2/7, R20 2/7, TRACE 2/7, R9 0/7.
- The retired loop's lane-futility index
  (`data/wm-trace/.lane-futility-index.edn`, 896 records) shows why
  it was not simply left running: e.g. 320 attempts / 0 successes on
  one address-sorry lane — an ungated loop churning.

Disposition (proposed to Joe, not yet ruled): restore live operation
(R10) as a commissioned backlog row, gated — the scheduled entrypoint
drives the SAME gated click path the continuous run uses, admitted
only after the fold-seam bypass repair (F11 first-cycle Finding A)
lands, with the process-assurance lifecycle cells as the acceptance
bar (R10 and R12 reaching 7/7: commissioned, dispatched, parked,
returned, checked, recorded, surfaced). The paper's claim of live
operation is then evidence-bearing again, and the old failure mode
(unattended futile churn) is answered by the gates and I6 streak
stops rather than by not running.

## Item 1b — Process-assurance column: ruled sloppy, redesign required

Joe (same day, later): "Box 3 is telling me that many of the items are
only validated 2 out of 7. And a bunch of them aren't censused. It
just seems like a very sloppy piece of infographic work. And if you
look in the column Process Assurance, there's all this duplicated
text. Absent, parked, returned, checked, recorded, surfaced. I just
don't know what that means. It's not communicating anything."

What the column currently encodes (from
ALIGN-rnode-process-census.md): seven work-lifecycle cells per node
(commissioned, dispatched, parked, returned, checked, recorded,
surfaced), each `exists` (running code refuses advancement or records
the cell), `named-only`, or `absent` (no NODE-LINKED implementation
found). Substantive fact the presentation buries: the Agency has
generic machinery for several cells (dispatch receipts
social/dispatch.clj:238-266, coordination ledger, durable parks
parked_on.clj:332-395) but "none associates that conduct with a
control-stage node. Generic machinery is not credited as an R-node
assurance." So `absent` means not-node-linked, not
no-such-machinery-anywhere — and the RUN4 click path now exercises
most of the seven cells end-to-end without node credit.

Redesign requirements (rides packet C with Item 1):
1. The seven cells defined ONCE in a legend; per-node display is a
   compact seven-cell strip (exists / named-only / absent as glyph
   states), never repeated prose lists of absent cells.
2. "not censused" stated once as scope (census covers the assurance
   band + R16), not repeated per row as if it were a finding.
3. The not-node-linked caveat stated where the column is introduced:
   absent cells are a JOIN gap for generic machinery in several
   cases, same disease as the contract join for the seven
   declarations — and closing that join (crediting the RUN4 lifecycle
   machinery to nodes where evidence supports it) is the census's own
   candidate repair, separate from presentation.

## Item 2 (amended) — Outer loop: on-demand capability at the Empirics standard, never the cron

Joe (superseding Item 2's proposed disposition): "It's not about
reinstalling the cron. No one should probably ever reinstall that
cron job, to be honest. What was more interesting was just that we
had the ability on demand to run the outer loop. ... I don't want
your story about the defects of the old outer loop to prevent us from
creating a new one that works well. The problems with the old outer
loop are well known to me... I think the empirics section of the
paper that we wrote showed how we fixed that. So that would be the
standard that we should think about any new outer loop, not the old
broken one, but the repaired one that actually worked."

Revised disposition: the target is ON-DEMAND outer-loop capability —
runnable when wanted, not scheduled churn — specified against the
repaired Empirics loop (the paper's empirics section) as the
standard: runs that do useful work, findings routed and repaired,
breakdowns fixed in-lane. The gated click path (post fold-seam
repair) is the substrate; the futility-index history is context Joe
already holds, not a blocker. Row minting still awaits Joe's go.
