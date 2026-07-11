# FLIGHTS — Batch 6 (eight deposits, CROSS-LANE: zai-8 flies lane A, zai-7 flies lane B)

**Arming:** Joe (batch 6, parallel lanes). Scope: artifact-only (no substrate
writes, NO production-code edits, no JVM/process launching; reads via files
and git only), lab dirs only, operator forks escalate. RUNNER-CONTRACT.md in
force (battery pre-check on EDN artifacts; check-parens + exactly-one-form on
all .edn). Label semantics: boolean AT EACH PSI'S OWN TEXT (deposit-psi
grain); honest FALSE with attempt evidence is first-class; expected outcomes
preregistered below — do not stretch toward TRUE.

**Preregistration note:** seven of eight psis are conjunctive/practice-bound
(operator cadences, unrun infrastructure) → expected FALSE with real attempt
evidence; one (F8) is representational and plausibly dischargeable. If pairs
tie on honest negatives, that is data, not failure. The emerging psi-ambition
confound (richer cascades → more conjunctive psis) is under observation —
adjudicate strictly per psi text.

## zai-8 flies LANE A:

### B6-F1 — ft-daily-scan-001 (GFN-authored psi: 20 scan-days + publication + evidence emission) — expect FALSE
Fly the publication-surface slice: `render_scan_trail.py` in
futon7/holes/labs/M-daily-scan/ (create) — parse the REAL landed scan-day
frames (frame-daily-scan-001..006; locate them, STOP if absent) into a single
browsable index (markdown): per scan-day date, probes run, findings, links.
**Acceptance:** runs from any cwd on the real frames; index lists all 6
scan-days with real content (paste); no invented entries.
**Adjudication:** FALSE (20 scan-days + emission not discharged) + attempt.

### B6-F2 — ft-daily-scan-002 (incumbent-authored psi: trail-indexing + budget + bridge + discipline) — expect FALSE
Fly the trail-index slice: `scan_trail_index.py` same lab dir — build a
QUERYABLE index over the same frames (by probe-type, by finding-keyword, by
date; query CLI with 3 demonstrated queries on real data).
**Acceptance:** 3 real queries pasted with real results; any cwd.
**Adjudication:** FALSE (conjunctive psi; trail-indexing alone ≠ discharge) +
attempt. NOTE vs F1: different psi, different slice — F1 renders, F2 queries.

### B6-F3 — ft-single-entry-point-001 (GFN psi: dev/status listing + A/B resolved + orphan attested) — expect FALSE
Fly the visibility slice AS A LAB TOOL (production dev.clj is out of scope):
`dev_status_probe.bb` in futon3c/holes/labs/M-single-entry-point/ (create) —
read the REAL PID files / process table the dev-laptop-env script maintains
(read the script first for paths) and list each subsystem with
running/not-running. Do NOT launch or signal anything.
**Acceptance:** real probe output pasted (whatever is actually running);
subsystem list matches the script's managed set; any cwd.
**Adjudication:** FALSE (visibility criterion met as lab tool, not in
dev/status; orphan attestation requires a live SIGINT test = out of scope) +
attempt.

### B6-F4 — ft-single-entry-point-002 (incumbent psi: + seam/budget/hot-reload conjunction) — expect FALSE
Fly the open-question slice: `Q-classpath-compat-analysis.md` same lab dir —
answer the mission's Q-classpath-compat from the REAL configs (read deps.edn,
shadow-cljs config, the dev-laptop-env script; cite real :paths/:deps values;
no invented config). A writing task with a correctness bar.
**Acceptance:** the note answers the question with real cited config values;
<= 2 pages. **Adjudication:** FALSE (conjunction) + attempt.

## zai-7 flies LANE B:

### B6-F5 — ft-war-machine-wiring-001 (GFN psi: D-7.5 v1 + V-1 + V-2.4 + Checkpoint 2) — expect FALSE
Fly the D-7.5 vocabulary slice: `D-7.5-v1-vocabulary-draft.md` in
futon5a/holes/labs/M-war-machine-wiring/ (create) — draft the v1 vocabulary
the psi names as not-landed, from the mission doc's three contract
signature-sketches (read them; cite the actual signatures; no invented
contract terms). Writing task, correctness bar.
**Acceptance:** every vocabulary term traces to a real signature-sketch or
doc section (cited); <= 2 pages.
**Adjudication:** FALSE (V-1/V-2.4/Checkpoint-2 undischarged) + attempt.

### B6-F6 — ft-war-machine-wiring-002 (incumbent psi: full two-loop AIF wiring spec) — expect FALSE
Fly the V-1 evidence slice: `commit_vertex_staleness.bb` same lab dir —
measure the commit-vertex coverage staleness the fold identified (git-based:
compare newest commit vertices in the code-graph snapshot data vs actual git
HEAD dates across the futon repos the snapshot covers; read-only).
**Acceptance:** real staleness numbers pasted per repo; method documented in
the script header. **Adjudication:** FALSE + attempt (one measurement ≠ the
spec's discharge).

### B6-F7 — ft-trip-journal-001 (GFN psi: adoption validation + void-sorry + 3 deferred infra) — expect FALSE
Fly the smallest deferred-infrastructure piece THE DOC NAMES (read the
mission doc's deferred list; pick the smallest buildable-as-lab-artifact;
STOP and report if all three require operator practice or substrate writes).
Build it in futon5a/holes/labs/M-trip-journal/.
**Acceptance:** the artifact runs/validates against real mission content
(paste); the report names which deferred piece it is and why the other two
were not flyable. **Adjudication:** FALSE (adoption + void-sorry are
operator-practice by nature) + attempt.

### B6-F8 — ft-trip-journal-002 (incumbent psi: DRs as two-step escrow + tripwires as stop-the-line + negative port specs) — expect TRUE (the batch's one dischargeable psi)
The psi is REPRESENTATIONAL — it wants the hinge-log's protective apparatus
expressed in those three disciplines. Fly it whole:
`dr_escrow_ledger.bb` + `protections-as-specs.md` in
futon5a/holes/labs/M-trip-journal/: (a) parse the mission's REAL 3 DRs into
an escrow ledger EDN with two-step release semantics (:held →
:contract-released → :satisfied) and a checker that evaluates each re-entry
condition's status from the doc's own text; (b) the note mapping each of the
5 tripwires to a stop-the-line trigger (condition → halt → resume) and each
anti-life-logging protection to a negative port specification.
**Acceptance:** ledger built from the real DRs (paste); checker runs from any
cwd; all 5 tripwires and all protections mapped with doc citations.
**Adjudication:** TRUE iff both artifacts land as specified — the psi's own
text asks for exactly this expression.

## Execution
Parallel cross-lane (zai-8: F1→F4; zai-7: F5→F8), serial within lane.
claude-2 reviews per CONTROL-LAYER. After all eight: adjudications at
:grain :deposit-psi, adjudications_to_ch2.bb, scoreboard row, retrain
(n up to 29), LOMO recheck. Four new paired comparisons enter the sign test.
