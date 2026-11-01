# Major incident, outer selection: questions 1 and 2 (claude-2, 2026-11-01)

Scope set by Joe (2026-11-01): the outer path is the inner path with a different job
(select work rather than do work). It minimises G over cascades built from the
task-selection library and meets the same AIF criteria and the same Lean requirements.
"The outer loop should be able to say what was selected and why, with reference to the
minimising cascade." Joe: "it may be that it didn't stop being conformant and simply
never started."

Question 1: for each 2026-10-31 and 2026-11-01 run, what does the outer section say was
selected, by which cascade, and does it give a reason that meets that standard?
Question 2: build the outer RunFacts by hand and evaluate Q1, Q3-Q7, Q9, to learn whether
the existing requirements would have caught this had the outer run been submitted.

Everything below is read from the run records, `outer-state.edn`, `outer-makeup.edn`,
`src/futon2/aif/outer_select.clj`, `src/futon2/aif/outer_score.clj`,
`scripts/wm_run_facts.clj`, and `DarkTower/WarMachine/Requirements.lean`. No Zai note
and no tracker row was used as a source.

## Records

17 tick run records carry a 10-31 or 11-01 date. 11 have an `:outer/selection` section
(9 on 10-31, 2 on 11-01) and a sibling `.requirements.edn`. 6 (all 10-31 14:47Z to
14:49Z, target `M-current`, patterns `p/x` `p/y`, 3 open tasks) are fixture-shaped, have
no outer section, no projection, and are not in `run-record-manifest.edn`. They sit in the
production run directory. Noted for the reach question; not used below.

Every one of the 17 carries `:outer-task-selection {:status :absent :reason :receipt-not-retained}`.

## Question 1: what and why, per run

Click order from the consumption receipts (`:click-count-before/after`). Score = outer G
per cascade in menu order [what-others-wait-on-first, only-what-can-start-now,
find-out-before-building]; lower wins; `cascade-index` is the chosen one.

| click | run | selected target | relation | scores (menu order) | chosen | predicted landing | run ended |
|---|---|---|---|---|---|---|---|
| 2 | f805ca4b | M-typed-memories | unknown | 4.3134 = 4.3134 = 4.3134 | 0 | stop-the-line/none p=1.0 | incomplete |
| 3 | 4955b6e6 | M-typed-memories | unknown | 3.9870 = 3.9870 = 3.9870 | 0 | stop-the-line/none p=1.0 | already-satisfied |
| 4 | 0ba3370c | M-typed-memories | unknown | 3.7802 = 3.7802 = 3.7802 | 0 | stop-the-line/none p=1.0 | build-failed |
| 5 | ebbbe42a | M-typed-memories | unknown | 3.6352 = 3.6352 < 3.6352(+5e-16) | 0 | stop-the-line/none p=1.0 | grounded-progress |
| 6 | e71c7380 | M-pattern-retrieval-calibration | associated | 2.9055 = 2.9055 < 3.5275 | 0 | related/several p=0.375 | guardrail-refusal |
| 7 | d5b9b513 | M-pattern-retrieval-calibration | associated | 2.9006 = 2.9006 < 3.4443 | 0 | related/several p=0.375 | grounded-progress |
| 8 | adc46631 | M-peradam-grounding | unknown | 3.3781 = 3.3781 = 3.3781 | 0 | stop-the-line/none p=1.0 | incomplete |
| 9 | 0be4fc7d | M-peradam-grounding | unknown | 3.3245 = 3.3245 = 3.3245 | 0 | stop-the-line/none p=1.0 | grounded-progress |
| 10 | f4b95abf | M-pattern-retrieval-calibration | associated | 2.9325 = 2.9325 < 3.2802 | 0 | related/several p=0.375 | guardrail-refusal |
| 11 | c5590f95 | M-pattern-retrieval-calibration | associated | 2.9523 = 2.9523 < 3.2433 | 0 | related/several p=0.375 | grounded-progress |
| 12 | ac5bc477 | E-comprehension-foundation | unknown | 3.2121 = 3.2121 = 3.2121 | 0 | stop-the-line/none p=1.0 | build-failed |

In every run `:selection` is `{:selected {:target T :reach 1/2}}`; `q` is
`{:doable 0.75 :blocked 0.25}`; `:tie? false`; `cascade-index 0`.

**What was selected**: present in all 11 (target, cascade id, cascade index, Giraffe run
id, manifest sha, makeup sha).

**Why**: absent in the 6 unknown-relation runs, weak in the 5 associated runs.

- In the 6 unknown-relation runs all three cascades score identically (to the last digit
  in 5, to 5e-16 in one). The third cascade (`find-out-before-building`) picks nothing
  (`:no-pick` where `cascade-picks` is retained). `outer_score.clj`: a no-pick model is the
  stopped kernel, landing stop-the-line/none; an unknown relation makes `ending-class`
  nil, so the picked model's landing is also stop-the-line/none, both kernel rows are
  point masses (ambiguity 0), and the kernel is uninformative (novelty 0, Lean
  `outerKernelNovelty_uninformative`). G reduces to the risk of stop-the-line/none against
  C in both cases, so picking the target and picking nothing have the same G. The tie is
  broken by `.indexOf` on the scores, i.e. menu order. In these 6 runs the target was
  chosen by file order, and the chosen policy's predicted outcome was identical to
  abstaining, with probability 1.0 on stop-the-line.
- In the 5 associated runs cascades 0 and 1 pick the same target (same G), and the
  no-pick cascade scores worse. This is a minimisation over two effective alternatives
  (that target, or nothing). The reason it gives rests on three inputs: the relation
  `:associated` from the relation producer, the default reach 1/2 from the makeup
  ("Runtime task reach is absent"), and unblocks from Giraffe. The record carries no
  per-term breakdown (risk, ambiguity, novelty) for any cascade, only the sum.
- `:tie? false` in all 11 because the code counts a tie only when the tied cascades pick
  *distinct* targets. Identical scores with the same pick, or with no pick, are not a tie
  by that definition. The field reads as "no tie" while `tied-cascades` lists two or three.

**Learning**: 12 consumptions in `outer-state.edn`. Every consumed run, including the 4
that ended grounded-progress, was classified `:ending-class :not-yet-evaluated` with
`:classification-absence {:reason :classification-receipt-absent}`. The ending-class
counts C are the starter kit decayed 12 times and nothing else (55 x 0.9^12 = 15.5336,
matches `:focused`); the readiness-state counts likewise (3 x 0.9^12 = 0.8473 matches
`:ready :doable`), which is why q is 0.75/0.25 in every run. Only the ready-gain counts
moved: `:none` rose from 1 to 7.14, `:several` took one increment early. After 12
clicks the outer model believes what the makeup file told it, minus decay, plus
"tasks make nothing ready".

## Question 2: the outer run as RunFacts

`Requirements.lean` doc line 23 defines `RunFacts` as "the inner construction census of
one task, made after the outer choice", with the outer choice in a separate `Choice`
that no Q examines. `scripts/wm_run_facts.clj`, which writes the projection the Lean
reads, contains zero occurrences of the string "outer". So the outer run was never a
`RunFacts`; the 73 projections on disk are inner-only. This is the "never started" case.

Building the outer RunFacts by hand for run ac5bc477 (representative; the others differ
only in the numbers):

| Q | outer facts | verdict | note |
|---|---|---|---|
| Q1 enumeratedTasks = openTasks | admitted 545 + excluded 81 = 626 = open 626 | holds | the field covers every open task |
| Q3 cascadesWithoutG = {} | 3 cascades, 3 numeric scores | holds | vacuous: the three scores are equal |
| Q4 | gTerms: risk, ambiguity, novelty all evaluated; comparedPolicies = 3; policyPreferences: none recorded (no horizon, preference steps, semantics or normalization for any cascade) | **fails** (0 records for 3 policies) | by hand one could supply horizon 1, terminalOnly, rawCumulative for all three and Q4 would then **pass**; see below |
| Q5 | selection before interpretation | holds | |
| Q6 | click 6 refused (guardrail) on M-pattern-retrieval-calibration; click 7 chose the same target and cascade; input digest differed | holds | the digest covers the 626-task field, which changes every click, so Q6 cannot bind the outer path in practice |
| Q7 pathAbsenceCount = 0 | `:outer-task-selection :absent` on the selected path in all 11; `:selected-relation :unknown` in 6 | **fails** in all 11 | the projector counted 0 in 7 of 11 because it never reads the outer path |
| Q9 factorized preference certificate | C = normalized ending counts x normalized gain counts (`outer-preference`); no certificate anywhere in the record; the 2026-10-28 note itself says this is "settled in Lean against Q9's ruling that a flat table is not a factorized preference" and it was not | **fails** | |

`conforms` = false for every outer run, on Q4, Q7 and Q9.

**Would the requirements have caught the façade, had the outer run been submitted?**
Partly, and not where it matters.

- Q7 would have failed every run on the retained-receipt absence, and Q9 on the missing
  certificate. Those are the two that fire.
- Q4 fails only for want of declarations. Supply the obvious ones (horizon 1,
  terminal-only, raw cumulative, all terms evaluated) and Q4 passes, because Q4 by its
  own text counts zero-valued ambiguity and information terms "when their canonical
  carriers were evaluated", and its vacuity guard is `comparedPolicies.Nonempty`, a
  count. Three policies with identical G, one of which is "do nothing", pass Q4.
- Q3 passes because every cascade has a number, however equal.
- No requirement states what Joe's standard states: that the chosen cascade is the
  minimiser (strictly, or with a declared tie rule), that the compared policies' predicted
  outcomes differ from each other and from abstention, or that selection was not by menu
  order. The inner path has the same gap in principle; the inner path has not been
  observed to exercise it because its policies differ in their predictions.
- On the policy-construction side, `TargetConstruction` has no outer instance at all. The
  three cascades are hand-listed in `outer-makeup.edn` and use 3 of the 19 task-selection
  patterns (the library directory has 19 `.flexiarg` files plus a README). There is no
  library pin, no slice, no pool, no `sliceFromWholeLibrary`. In Lean's own terms, "a pin
  or digest of 0 is a recorded absence", so every outer construction field is a recorded
  absence.

## What this does and does not establish

Established from primary sources: the outer path was never submitted to the requirements;
6 of 11 selections in the window were made by menu order with a predicted outcome equal to
abstention; 5 of 11 were genuine two-way minimisations resting on one relation producer and
a default reach; the outer model learned nothing about endings or doability in 12 clicks
because no classification receipt was ever produced; the existing Q4 would not reject the
degenerate comparison even if the outer run were submitted.

Not yet examined (questions 3 and 4, held for Joe): the pre-10-29 `outerActions`
parameter and the narrowing from Q1-Q10 to seven requirements; the reach of these
selections into constructed cascades, inner G and grid rows; the relation producer that
reported `:associated` for M-pattern-retrieval-calibration; the six fixture-shaped
records in the production run directory.
