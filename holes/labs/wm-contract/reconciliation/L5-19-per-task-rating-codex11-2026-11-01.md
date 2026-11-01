# L5-19 — Diagnostic G per admitted task

Base 24581c7ff; branch codex11/l5-19. No merge, reload, live click, data write, Lean or projector change.

## Producer

outer-select/per-task-ratings scores each admitted candidate with outer-score/outer-model on a singleton field and empty steps, exactly the entry used after a cascade has picked. It uses the click's score-input (learned ending/gain/state counts), relation producer, facts and candidate reach. It does not execute a different gate, change eligibility or change the minimum. Unknown relations are retained as {:class :unknown}; the existing model maps them to stopping, not unrelated. The real score-model applies the same stopping altitude as abstention.

Every row retains target, available task status/rank/cone/unblocks, typed relation, terms, G and canonical outcome prediction digest, or typed :absence when the model cannot be formed. Diagnostic errors are caught per candidate, including throwing fact providers. Missing facts are not fabricated. The section gains :per-task-rating and :top-ten before chosen/abstained/refused decision assembly, so scored refusals retain the diagnostic too. Pre-scoring failures cannot supply a score field; this patch does not invent one.

Top ten excludes absence rows and sorts by G then target ID for display. Equal diagnostic scores are not resolved as selection choices. Choice digest, classes, admissibility, refusal rules and projector are untouched; an integration test compares the entire result minus the two diagnostic keys with ratings disabled and finds it equal.

## Retained fixture and consistency

Source: selection-record-2026-11-01-wm-click-0068e006-6fc1-4f29-8a72-5d307748ce12.edn, sha256 d37988e8c480763b093cb7f213e67bdba62b2a87d5ba23e5f91440d7922264ed. The test fixture is a projected, portable 591-task carrier, with the retained task facts, cascade relations, predictions/scores and learned preference state. It does not require a live registry or canonical data at test time.

Correction to the packet's “both compared classes”: this record has ONE compared class (two ready arrangements) plus a stopping-equivalent discovery pick. All three recorded arrangement scores are checked within 1e-9, covering both distinct picks. Proxy-metric-inventory G = 3.3896147707408804. Interim-director/stopping G = 13.278213771580795, altitude 10; separately recomputed abstention agrees.

The makeup's initial preference kit is NOT the learned click preference. An initial replay using it failed the consistency test (4 failures), which exposed that distinction. The read-only outer-state snapshot still has click-count 15, and its consumed-run receipt SHA equals the selection record's :outer/learning receipt for run 4646c7bd. Snapshot sha256 d521600c640deef92da852629d0c7a0f95705c27238663fe1e4808611c0188d3. Those learned counts, copied into the fixture, reproduce all scores. An earlier test load also caught and corrected a test-parenthesis typo; neither failed run is reported as green.

Historical relation limit: the old :field-facts does not retain every task's relation receipt; only cascade-pick relations are available. The historical replay therefore uses those recorded classes and explicit :unknown for all other tasks. It does not classify old tasks against today's changed sources. Production uses the full click relation function, which outer-observation builds from classify-target once per task. Hence this historical table is a faithful diagnostic of available evidence, not a complete informed prioritization of 591 tasks.

## Cost and gates

Fresh JVM, loaded namespaces/fixture first, then one timed per-task-ratings call: **351.388876 ms** wall for 591 rows, zero absence rows, below 5 s. Includes model/scorer/altitude reads and prediction hashing; excludes JVM startup, fixture parsing and relation-production work already performed by outer-observation. Unknown-relation rows have defined stopped models, so they are not absence rows. Earlier measured runs were 470–508 ms; no optimization performed.

Touched namespaces: outer-select-test and outer-task-rating-test, 22 tests / 101 assertions, zero failures/errors. Hermetic FUTON_WM_* path overrides via /home/joe/notes/l5-19/run-tests.py. Final canonical data file count 45993 → 45993. Earlier runs 45991 → 45991 and 45993 → 45993; the between-run change was outside these test runs. Kondo zero errors/warnings; check-parens.el OK; git diff --check clean.

Tests pin: all retained arrangement Gs and abstention; unknown relation is not unrelated; top ten count/order/ranks; 591 rows including a planted missing-reach absence; throwing fact lookup produces 591 typed absences; full integration and decision invariance. No source scorer change was needed.

## Top tens, side by side

Rows 2–10 in the G column are tied at stopping because their historical relations were not retained; target-ID ordering is only a deterministic display order. They are not claimed to be the nine next-best informed tasks.

| # | Diagnostic G task | G | Its rank | Giraffe task |
|---|---|---|---|---|
| 1 | M-interim-director-proxy-metric-inventory | 3.3896147707408804 | 2 | M-interim-director |
| 2 | E-70B-on-raw-control-arm | 13.278213771580795 | 24 | M-interim-director-proxy-metric-inventory |
| 3 | E-A-B-C-career-mode-economics | 13.278213771580795 | 155 | E-memory-whitepaper-v2-programme |
| 4 | E-APM-f11-defects | 13.278213771580795 | 156 | E-queries-and-scopes |
| 5 | E-APM-f12-defects | 13.278213771580795 | 157 | M-typed-memories |
| 6 | E-C-realization | 13.278213771580795 | 158 | M-superpod-mark2 |
| 7 | E-R14-red-ring-fill | 13.278213771580795 | 159 | E-clean-up-substrate-2 |
| 8 | E-R2-red-ring-fill | 13.278213771580795 | 160 | E-implementing-G-over-cascades |
| 9 | E-R6-red-ring-fill | 13.278213771580795 | 161 | E-live-loop-1 |
| 10 | E-R8-red-ring-fill | 13.278213771580795 | 162 | M-apm-capability-ratchet |

## Receipt artifacts

Notes directory /home/joe/notes/l5-19 contains replay.clj, replay.edn, replay.log, fixture.bb, run-tests.py and test receipts. Source and test fixture are committed; no generated data files are added.

- final3.log: sha256 97ec4d329d476019de54bbe498346fe16d70818a8410fd500a2f761e0b7c7fe3
- final3.json: sha256 8926bec9a08aec64b6acaf91eebb566a24d2509dde574e002dcaba769b7e4d2a
- replay.edn: sha256 9826e606a4caa379caa4f76ff6f1833343062e752701a2657ae45114c9bf4ea1
- replay.log: sha256 4638ed9b47fa5af2e55f6845e8ffd563e42fa19fc8ff15cea3a9db06f1bd12f1
- kondo.log: sha256 0e02890603b6d172375a2eb2dd7a802fab773d5e2f70dcdcce6fdd148ece7d65
- parens.log: sha256 a12b7cb43c9d9134b5bb1b35e9096b66775d9e92e7611d1cc92b02edd6782a87
