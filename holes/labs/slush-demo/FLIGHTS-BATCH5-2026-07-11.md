# FLIGHTS — Batch 5 (six deposits, flyer zai-6; THE FIRST DEPOSIT-PSI-GRAIN BATCH)

**Arming:** Joe (batch 5 approved; zai-5/zai-6 minted for fold/flight split).
Scope: artifact-only (no substrate-2 writes, no JVM, no production edits;
substrate READS via existing files/git only), operator forks escalate.
RUNNER-CONTRACT.md in force incl. battery pre-check on any EDN artifact.

**NEW LABEL SEMANTICS (first use):** each flight adjudicates ITS DEPOSIT'S
PSI — the question zai-5 authored (author ≠ flyer, enforced by runner split).
The label is boolean AT THE PSI'S OWN GRAIN: `true` only if the psi's want is
discharged as stated; a flight that builds real artifacts but leaves the psi
undischarged is an honest `false` WITH attempt evidence (batch-1 F2
precedent — negatives are first-class). Expected outcomes are preregistered
per flight below; deviations in EITHER direction are findings.

## B5-F1 — ft-fold-ansatz-003 (verification-gate psi) — expect TRUE
Build `verify_fold_outputs.py` in `futon2/holes/labs/M-fold-ansatz/` (create):
given predicted endpoints (f1 outputs — locate the landed f1 results; STOP if
absent), check each has a reproducible twin in the code-graph snapshot AND
flag popularity-flattery (predicted endpoint's degree percentile vs a
degree-baseline pick). 2 inline malformed fixtures rejected.
**Acceptance:** runs on the REAL f1 outputs from any cwd; twin-check +
popularity flag per prediction; fixtures rejected. **Psi adjudication:** the
psi asks for exactly this gate — if the tool runs on real outputs, psi
discharged = TRUE.

## B5-F2 — ft-fold-ansatz-002 (ansatz-verdict psi) — expect FALSE (honest)
The psi asks for the trained-embedding verdict; the training run (Linode
Stages C+D) is out of flight scope BY DESIGN. Fly the laptop-tractable core:
run the degree/popularity baseline on the A-next gold corpus (locate the
landed corpus + baseline scripts; STOP if absent) and produce
`ansatz-attempt-report.edn`: baseline numbers reproduced + exactly what the
verdict still needs (the unrun stages, their authored locations).
**Acceptance:** baseline reproduced with numbers; report enumerates the gap
with file refs. **Psi adjudication:** FALSE (verdict not reached) with attempt
evidence — the preregistered honest negative.

## B5-F3 — ft-bounded-in-flight-state-002 (remaining-gaps psi) — expect TRUE on its core
The psi's first named remainder is the Q-21 guidance note. Write
`Q-21-guidance-hinge-vs-block.md` in the mission's lab dir (create under
futon3c/holes/labs/M-bounded-in-flight-state/): the hinge-vs-Block
relationship + how to identify in-flight Blocks, ANSWERING the 2026-05-04
reopen's actual question, citing the shipped code (metabolic_balance.clj,
commit_ingest.clj Block-footer format) and cook-ting/hinge-point. This is a
writing task with a correctness bar, not a coding task.
**Acceptance:** the note exists, answers the reopen's question with citations
to real code/patterns (no invented APIs), ≤ 2 pages. **Psi adjudication:**
psi lists {Q-21, Q-22, WM views}; Q-21 is its core named item — TRUE iff the
note lands AND the report honestly scopes Q-22/views as remaining (partial
discharge of a conjunctive psi = reviewer judgment, preregistered here as:
TRUE if Q-21 note accepted, since the psi's own text names Q-21 as "the open
item the reopen names").

## B5-F4 — ft-bounded-in-flight-state-003 (same-mission giant psi) — expect FALSE (honest)
Same remaining-gaps psi from the 17-pattern cascade. To avoid duplicating
F3's slice: fly the OPEN-BLOCKS VIEW slice (the WM three-views remainder):
`open_blocks_view.bb` in the same lab dir — parse real `Block:` footers from
futon3c git history (read-only git log; the shipped commit_ingest.clj defines
the format — read it first), list open/closed blocks with dispositions.
**Acceptance:** runs on real git history from any cwd; output lists real
blocks (paste); format matches the shipped parser's semantics.
**Psi adjudication:** FALSE (the psi's conjunction incl. Q-21+Q-22 is not
discharged by one view) with the view as attempt evidence. NOTE: F3 TRUE +
F4 FALSE on near-identical psis is expected and is NOT an inconsistency — F3's
psi centers Q-21 (its cascade grounds calibration-adjacent patterns); F4's
psi carries the full conjunction. Adjudication follows each psi's OWN text.

## B5-F5 — ft-artificial-stack-exchange-001 (full-ArSE psi) — expect FALSE (honest)
The psi asks for the running triadic 100-iteration loop + Phases 2-5. Fly the
landed-code demonstration: RUN the existing Phase-1 scripts (hotspot loop,
arse-store, synthetic QA — locate them; STOP if they don't run without
substrate writes; document any degraded/dry-run mode used) for a bounded
demonstration, and produce `arse-phase1-run-report.edn`: what ran, what it
produced, what the full loop still needs (the fold's h1 mechanism gap).
**Acceptance:** real scripts executed (command + output pasted) or documented
could-not-run finding; report enumerates the loop gap with refs.
**Psi adjudication:** FALSE with attempt evidence.

## B5-F6 — ft-artificial-stack-exchange-002 (loop-mechanism psi) — expect FALSE, informative
The psi asks for the full loop too, but THIS cascade grounds the mechanism
(f6/self-play-loop, graph-enhanced-evaluation). Fly the mechanism skeleton:
`triadic_loop_v0.py` in futon6/holes/labs/M-artificial-stack-exchange/
(create): Asker/Answerer/Critic over a small FIXTURE graph (no substrate) —
gap-derived question, corpus-retrieval answer stub, gate-scored critique,
threshold-gated graph update; 3-5 iterations; deterministic; tests for the
threshold gate (below-threshold update REJECTED).
**Acceptance:** tests green from any cwd; a 3-iteration run log pasted;
threshold rejection demonstrated. **Psi adjudication:** FALSE (100-iteration
run + phases not discharged) with the skeleton as attempt evidence — but the
skeleton quality feeds the PAIRED comparison with F5's attempt.

## Execution
Serial on zai-6 (debut — contract-first welcome). claude-2 reviews per
CONTROL-LAYER (battery + re-run every claimed gate from a different cwd).
After all six: adjudications at :grain :deposit-psi (boolean per psi text),
adjudications_to_ch2.bb re-run, scoreboard row — S1/S2's first psi-grain
points and possibly the FIRST NON-TIE PAIR (F1/F2 expected true/false on
fold-ansatz). Then R̂ retrain (n grows 15 → up to 21) and the LOMO transfer
recheck.
