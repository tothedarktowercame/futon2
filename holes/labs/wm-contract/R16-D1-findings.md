# R16-D1 discovery findings

Scope: discovery only; no source or specification was changed. Corpus observations use the complete multi-form reader described below.

## Instrument and corpus pin

- **Observed.** Command: a `clojure -M -e` reader loop over regular files matching `data/wm-trace/wm-trace-.*\.edn`, using `clojure.edn/read`, a `PushbackReader`, an identity-tested EOF sentinel, and `:default (fn [_ v] v)`. It read 53 files and 792 forms. SHA-256 of each form's `pr-str`, sorted, newline-joined, then SHA-256 again was `c9add16ac96c973ba4fd9a0c61f3b7319780c304424e2d14ea7b477309947880`: the registered pin matches. The trace writer preserves each judgement as one EDN form (`src/futon2/aif/trace.clj:270-281`).
- **Observed.** The same reader found 792 `:decision` maps, 112 forms with `:act-gate-verdicts`, 88 with `:enactment`, and 88 with `:realized-outcome`; these are fields deliberately attached by the close-loop and trace seams (`src/futon2/aif/enact.clj:234-254`, `src/futon2/aif/trace.clj:270-281`).

## Fixture promoted to grounding domain

- **Observed.** `reviewed-candidate-cleans` is exactly four mission-to-CLean-path entries and calls itself the “A3 live-test suite” (`src/futon2/aif/actuator_a3.clj:372-379`). `a3-live-test` looks up one mission in it (`src/futon2/aif/actuator_a3.clj:381-398`), and `a3-live-tests` enumerates all of its keys (`src/futon2/aif/actuator_a3.clj:400-405`).
- **Observed.** Repository search command `rg -n 'reviewed-candidate-cleans' src test scripts --glob '*.clj'` found one additional production reader: `reviewed-clean-for`, which uses the map to obtain the CLean document for realised folding (`src/futon2/aif/fold_realized.clj:110-116`). No other reader was found in those searched Clojure trees; limit: this does not inspect dynamically constructed Var names or non-Clojure consumers.
- **Observed.** For missions absent from the map, `reviewed-clean-for` returns nil (`src/futon2/aif/fold_realized.clj:110-116`); `grounded-deposit` then retains the mission but has no `:clean` (`src/futon2/aif/fold_realized.clj:118-129`). The live tester represents zero bound boxes as `:domain-mismatch`, not typed absence (`src/futon2/aif/actuator_a3.clj:388-397`).
- **Observed; registered expectation confirmed for actual selections.** The complete reader selected the action where `[:decision :action :type]` was `:open-mission`, normalized its `:target` with `mission-key`, and compared it to the normalized four map keys. There were 96 selections: `M-canon-fingerprint-store` 44, `M-capability-star-map` 29, `M-emacs-cursor-peripheral` 21, `M-futonzero-mvp` 2; inside 0, outside 96. The decision shape is emitted as `:action` under `:decision` (`src/futon2/aif/trace.clj:265-277`). “All 792 selected a mission” would be false: only 96 forms selected an open mission.

## Enactment and downstream absence

- **Observed.** Repository search command `rg -n ':enacted|:enactment' src test scripts --glob '*.clj'` found the operative writer in `enact!`: `:enacted` is `engine-wiring shown` only when `shown` is nonempty, otherwise nil; the same return writes the `:enactment` audit (`src/futon2/aif/enact.clj:205-232`). `close-loop!` copies that audit into the judgement and passes `enacted` to realised folding (`src/futon2/aif/enact.clj:234-254`). The trace layer copies the judgement's enactment into the durable form (`src/futon2/aif/trace.clj:270-281`).
- **Observed.** Nil enactment becomes neither zero nor typed absence in realised gain: `realized-outcome-of` computes `:realized-score` from `realized-coverage`, and the source explicitly documents nil realised coverage as nil score (`src/futon2/aif/fold_realized.clj:95-101`). The complete corpus confirms all 88 `:realized-outcome` records have nil `:realized-score`. Thus it becomes an untyped nil which causes γ to hold, not a numeric score; `enact!` documents that behavior (`src/futon2/aif/enact.clj:205-213`).
- **Observed.** Other downstream projections do turn the audit into labels: evidence emission chooses enactment mission/policy and falls back to top-level `:enacted` (`src/futon2/aif/evidence_emit.clj:122-148`); scheduled-run reporting reads the audit (`scripts/wm_scheduled_run.clj:121-135`). These do not create an external act witness.

## Gate and trace results

- **Observed.** `act-gate-from-lane-entry` emits `:cascade-score`, `:coverage-score-delta`, its source, and fold provenance (`src/futon2/aif/close_loop.clj:65-108`). `preview-verdict` abstains if either leg is nil, passes exactly when cascade is positive and coverage delta negative, otherwise fails (`src/futon2/aif/close_loop.clj:110-116`). Therefore the glossary examples +1.2/-0.8 pass and positive coverage delta fails literally, and the implementation agrees that these are engineering quantities rather than F/G (`/home/joe/code/p4ng/sec-glossary.tex:66`, `src/futon2/aif/close_loop.clj:7-10`).
- **Observed.** The complete reader counted 165 individual gate verdicts with two numeric legs among 112 forms carrying gate verdicts. So ticks do record decisions with both legs; the record's question is answered yes. `close-loop!` projects each verdict and both legs into the judgement (`src/futon2/aif/enact.clj:242-253`).

## Re-observation / external witness

- **Observed negative, with limits.** Instrument: `rg -n ':enacted|:enactment|witness' src/futon2/aif scripts/futon2 --glob '*.clj'`, followed by direct inspection of `observe`. The observation vector contains 14 declared channels (`src/futon2/aif/observation.clj:11-32`) and builds them exclusively from loop/support/mission/graph/frame/annotation scan inputs (`src/futon2/aif/observation.clj:34-74`). The search found no channel sourcing an enactment or act witness. Limit: static text search cannot detect a value hidden under a generic upstream scan field; establishing that would require provenance through every scan producer. Therefore “no channel reads an act witness” is confirmed for explicit wiring, but indirect generic-field flow remains **inferred, untested**.
- **Observed packet discrepancy.** The packet calls these “R2's fourteen channels”; the vector indeed has 14 entries (`src/futon2/aif/observation.clj:18-31`), but the namespace docstring still says “13-channel” (`src/futon2/aif/observation.clj:4-7`).

## Lean-side declarations and consumer status

- **Observed.** `GainChain.lean` is `/home/joe/code/mathlib4/DarkTower/WarMachine/GainChain.lean`; it defines `declaredDomain`, `domainNotNarrowed`, and `typedAbsence` (`/home/joe/code/mathlib4/DarkTower/WarMachine/GainChain.lean:142-168`).
- **Observed negative, with limits.** Instrument: `rg -n 'declaredDomain|typedAbsence|domainNotNarrowed' /home/joe/code/futon2 --glob '*.clj'` returned zero. This establishes no literal Clojure consumer in futon2; macro-generated or differently named implementations are outside the instrument.

## Conclusions and refusals

- The registered selection expectation is confirmed only for the 96 forms that actually select `:open-mission`; treating all 792 forms as mission selections is refused because 696 chose other action types (`src/futon2/aif/trace.clj:265-277`).
- The registered no-reobservation expectation is confirmed for explicit channel wiring, with the indirect-flow limitation above (`src/futon2/aif/observation.clj:34-74`).
- `:enacted nil` is not scored as zero: it remains nil and holds the realised update (`src/futon2/aif/fold_realized.clj:95-101`).
- `producingPart` is not declared by any inspected R16 runtime record; this is **inferred, untested** beyond the named source/search scopes. A future R16 contract must declare it rather than infer it, consistent with the absence of such a field in the current enactment audit (`src/futon2/aif/enact.clj:222-232`).
