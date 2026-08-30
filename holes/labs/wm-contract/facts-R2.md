# facts-R2 — pointers and verbatim snippets for the R2 worksheet

**Collected:** 2026-08-30. **Scope:** facts only (file:line + quotes). No interpretation, no classification.
**Model worksheet:** `futon2/holes/labs/wm-contract/R8-glossary-formalisation.md` (its §0 needed: paper name/line, excursion name, quantity, glossary entry, code, trace key; §1: glossary noun → quote → code · trace · Lean; §2: corpus check; §4: edges with file:line).

---

## 1. PAPER

### 1a. `p4ng/sec-catalog.tex`

- `sec-catalog.tex:105` (catalogue table row):
  > ` & R2 --- Structured Observation Vector & Normalises heterogeneous evidence. \\`
- `sec-catalog.tex:194` (the R2 paragraph, full):
  > `\paragraph{Structured Observation Vector (R2).} \textbf{If} you want the agent's observation to be comparable across steps so scoring and learning do not collapse into prose interpretation. \textbf{However} tool output and user feedback arrive as heterogeneous artifacts (JSON, logs, diffs, snippets, natural language) that are hard to compare or weight consistently. \textbf{Then} maintain an explicit observation record as a typed, normalized feature map derived from session state (test status, compile status, diff size, failing-spec count, user-stated constraints, time since last anchor, contradiction flags). \textbf{Because} a stable observation vector is the prerequisite for meaningful precision control and policy scoring.`
- Related, `sec-catalog.tex:366` (R20 paragraph, the HOWEVER clause mentions the observation channel):
  > `the loop's observation channel is task-level only: it observes the world it acts \emph{on} (job outcomes, verdicts, witnesses, QA), while its own machinery is a hidden state with no likelihood mapping`

### 1b. `futon2/docs/futon-aif-completeness.md:72-78` (`### R2 — Observation channel schema`)

> The agent's interface to the world is a fixed, normalised observation shape — a vector or map of bounded channels. The schema is named; observations from different ticks have the same shape and the same channel semantics.
>
> **Operational check.** Find the observation type. Verify the channel set is documented and stable, observations are normalised to [0,1], and all subsequent AIF machinery is keyed off the schema.
>
> **This implementation.** **Satisfied.** `futon2.aif.observation/observation-channels` declares 13 channels with documented semantics and source vocabulary (loop-health, support/attack coverage, mission-health, four commit-percentage channels, active-repo-ratio, sorry-count-norm, coupling-density, ticks-firing-ratio, depositing-signal). All channels return values in [0,1]. Schema is enforced by `observation-channels-test` in `futon2/test/futon2/aif/observation_test.clj` (count = 13, all keywords, no duplicates). `sense->vector` projects observations to the channel-order vector with explicit ordering test.

Adjacent, `futon-aif-completeness.md:86` (R3): "**Satisfied for 4 of 14 channels with the remaining 10 logged as `:prototyping-forward` sorries (v0.11, 2026-05-18).**" and `:90` "R3a (prediction error per channel) — ✓ for 4 of 14 channels." and `:96` "some are candidates for `:n-a-by-design` reclassification … (externally-measured signals like commit-percentages may be inherently belief-independent)."

### 1c. `p4ng/empirics-futon/promotion-tests.edn` — the `:node "R2"` entry (lines 36–96)

File header `promotion-tests.edn:1-7`:
> `;; promotion-tests.edn -- what would move a red-ring card out of \`plan\`.` … `;; Writing an entry is the EVALUATE -> SELECT transition; it is the only way a card reaches SELECT, and until 2026-08-25 no card had one`

`promotion-tests.edn:33` `{:as-of "2026-08-25"`

`promotion-tests.edn:36-50` (`:statement`):
> `{:node "R2" :wr "WR-16"`
> `:statement "observe.clj's channel-keys carries a channel whose value is a function of a typed operator TURN rather than of the operator's absence, and an inference result over a window of >= 111 items DIFFERS from the result the same window produces with that channel held at a constant. (Tightened 2026-08-25, before running, while writing features/operator-turns-enter-the-observation-vector. As first drafted this read '...and the nag class fires at least once in a window of >= 111', which a different repair satisfies on its own: nag's fourth conjunct \`:acknowledged?\` has NO producer -- operator-lane-adapter never sets it, operator-bulletin/newly-acknowledged has no caller outside its test, and the pre-laned needs-you.edn is [] -- so wiring acknowledgement makes nag fire with no operator turn anywhere in the observation vector. The null control would have caught it; the statement should not have admitted it.)"`

`promotion-tests.edn:51-57` (`:null-control`):
> `"the same window with the turn channel held at a constant. If the inference result is unchanged, the channel is being computed and not read. Record which constant, and why that one: a value picked to make the difference show is not a control. Record the mark-recognizer switch states too (FUTON3C_MARK_RECOGNIZER, FUTON3C_SELF_MARKS) -- WR-26; a run under a silenced recognizer produces a zero that means nothing."`

`promotion-tests.edn:58-61` (`:retro-trip`):
> `"the recorded live bulletin -- nag 0 / brief 60 / silent 51 of 111 -- is the known past this must fire on. A classifier that cannot separate 'considered and declined' from 'was elsewhere' on that window has not closed the ring."`

`promotion-tests.edn:62-73` (`:was-blocked-on` — note the key is `:was-blocked-on`, not `:blocked-on`):
> `"prior work on modelling operator turns (Joe, 2026-08-25). DISCHARGED the same day, on Joe's direction to treat C-R2 as a SELECT -> ACT step and specify the feature the way C-R8 was specified. The blocker asked for the modelling work to come first; the build spec at futon3/library/features/operator-turns-enter-the-observation-vector.flexiarg IS that work, and writing it is what found the unwired \`:acknowledged?\` seam above. What remains is not a blocker but the test itself: the pattern's ?evidence(required) -- 'a pilot has not yet shown that processed operator turns enter the inference vector and change an inference result' -- is precisely what :statement now measures. Reversible: restore :blocked-on and the card leaves SELECT."`

`promotion-tests.edn:74-89` (`:external-fixture`): `{:case :swe-bench :phase "PERCEIVE" :dated "annotation campaign 2024-06/2024-08; the paying party stopped using the benchmark ~18 months later" :must-emit "a signal that the measurement has stopped separating states of the world, emitted from the observation channel BEFORE the withdrawal. …top score moved 74.9% to 80.9% over six months while 59.4% of audited hard tasks carried test-design flaws…" :why-external "R9 and WR-0: a verdict the apparatus cannot issue to itself. …"}`

`promotion-tests.edn:90-92` (`:registers-when`):
> `{:built "the channel exists in the observation vector" :ran "one window of >= 111 items is classified with it" :live "the null control fails"}`

`promotion-tests.edn:93-96` (`:not-registered-by`):
> `"wiring the acknowledgement set so nag can fire. That is a real repair to a dangling seam and should be done and reported -- as a repair, under its own name, not as this ring."`

Flexiarg named by the entry: `futon3/library/features/operator-turns-enter-the-observation-vector.flexiarg` (line 67 of the edn).

---

## 2. GLOSSARY — `p4ng/sec-glossary.tex`

### Generative model (`sec-glossary.tex:7`)
> `\paragraph{Generative model.} A generative model is the system's story about how hidden causes produce observable evidence.  In a coding-agent setting, the hidden causes are operational states such as progress, risk, blockage, uncertainty, or pattern fit; the observations are things such as test results, trace records, diffs, and operator feedback \cite{FRISTON2016862,neacsu2024structure}.\footnote{Belief channels and preference scoring are implemented across \srcref{futon2/src/futon2/aif/belief.clj}, \srcref{futon2/src/futon2/aif/preferences.clj}, and \srcref{futon2/src/futon2/aif/efe.clj}.}`

### Belief state μ (`sec-glossary.tex:9`) + QUESTION (`:10`)
> `\paragraph{Belief state $\mu$.} The Greek letter $\mu$ names the system's current best estimate of its situation.  Instead of asking the language model to reread the entire conversation every time, the system stores a compact map of operational hypotheses: what goal is active, what is blocked, what looks uncertain, and what kind of pattern may fit.  Alongside each mean the system tracks a belief variance, updated as an exponential moving average of the squared miss with an added sensor-noise floor $\sigma^2_{\mathrm{sensor}}$: the squaring makes the size of a miss count regardless of sign, and the floor stops the variance collapsing to zero --- false certainty, which would silence the ambiguity terms of $G$ below.\footnote{R1/R8 are described in \srcref{futon2/holes/aif-r1-r16-pattern-map.md}; the mean and variance updates are implemented in \srcref{futon2/src/futon2/aif/belief.clj}.}`
>
> `% QUESTION: How does the "belief state" relate to memory?  Memory seems to be things that were believed to be true at one point, and also worth remembering because they are likely to be true (or at least relevant) in the future.  Since there is a structural analogy I think it would be worth talking about how this does or does not relate to the model.`

### Observation vector o (`sec-glossary.tex:12`) + QUESTION (`:13`)
> `\paragraph{Observation vector $o$.} An observation vector is a standardized summary of what happened.  The system cannot compare ``tests failed,'' ``operator objected,'' and ``diff got larger'' unless these events are normalized into named channels.  That is why the catalogue emphasizes typed observations rather than raw prose.\footnote{Trace fields are guarded in \srcref{futon2/src/futon2/aif/trace.clj}; the observation/scoring path is exercised in \srcref{futon2/test/futon2/aif/efe_test.clj}.}`
>
> `% QUESTION: This also relates to the question of "compare over time".  In an agentic coding system, "raw prose" is often enough if the prose and observations fit within the agent's context window.  But the WM is set up to run over long periods of time, in different sessions, with different agents that do not share a context window.  Although the observation vector has to do with what happened at a given moment, it seems to relate, also, to establishing a shared context over time.`

### Observation model A (`sec-glossary.tex:27`)
> `\paragraph{Observation model $A$.} The observation model $A$ is the agent's theory of its sensors: for each hidden state $s$, it specifies the probability distribution over observations $P(o \mid s)$. In the production War Machine belief path, every event update flows through this declared model --- when the agent observes $o$, it revises its posterior by multiplying the predicted prior by the likelihood column $A[o \mid \cdot]$. The implementation declares $A$ as an explicit, column-normalised $7 \times 7$ matrix over the entity-status vocabulary, with three entry classes: diagonal entries …, lifecycle-adjacent entries …, and contradictory entries …. The transition model $B$ is the identity, so the prediction step preserves the prior posterior, and the initial prior $D$ is uniform. The filter computes the prediction--update cycle $q^- = Bq$, $q \propto A(o \mid \cdot)^{\kappa(w)} q^-$, where $\kappa(w) = \log_2(1+w)$ is a tempered-likelihood exponent controlled by observation weight. … This warrants R3's green, model-relative status: the filter is exactly derived from its declared A/B/D model. It does not establish that those hand-set entries describe the world; calibration against exogenous evidence from adjudicated handoffs, build outcomes, and operator review remains open.\footnote{… \srcref{futon2/src/futon2/aif/belief.clj} and \srcref{futon2/scripts/futon2/report/war_machine.clj}; tests … \srcref{futon2/test/futon2/aif/belief_test.clj} and \srcref{futon2/test/futon2/aif/a_matrix_live_wiring_test.clj}; the simulation is \srcref{futon2/scripts/aif/a_matrix_simulation.bb}; …}`

### Other entries mentioning operator / turn / channel
Command: `grep -n 'operator\|turn\|channel' sec-glossary.tex`.
- `:15` Prediction error ε: "…untrusted channels are given small $\Pi$." (no operator/turn mention)
- `:17` Precision Π: "\footnote{Per-channel evidence precision is the R7 pattern; …}"
- `:19` Variational free energy F: "the subscript $k$ means ``for each evidence channel'' --- tests, logs, traces, operator feedback, and so on."
- `:27` Observation model A: "operator review remains open" (quoted above).
- `:76` Strategic mission selection: "(i) reason-bearing policy support, including hard executability and operator gates".
- "turn" occurs only as the verb ("Softmax turns scores into…" `:31`; "An embedding turns text into…" `:52`; "It turns a sequence of patterns into…" `:64`). **No glossary entry for "operator turn" or "turn" as a noun: not found** (`grep -n -i 'operator turn\|chat-turn' sec-glossary.tex` → no hits).

---

## 3. EXCURSION — `futon3c/holes/excursions/E-R2-red-ring-fill.md` (201 lines)

**Premise as opened** (`:1-9`):
> `# E-R2-red-ring-fill — the operator's turns, and the one missing edge`
> `**Opened:** 2026-08-26 · claude-13 at Joe's direction, *"which I think would round out the collection"*. Excursion from \`futon2/holes/missions/M-formal-war-machine.md\`.`
> `**It does round it out, and in a specific way: R2 is R14's mirror.** R14 — a quantity is computed, recorded, and cannot reach the **action**. R2 — a quantity is computed, recorded, and cannot reach the **belief**. Same defect at opposite ends. **The loop is open at both ends.**`

**Status table** (`:11-19`):
| | state |
|---|---|
| the ring's claim | **CONFIRMED** — and for the first time today, checking it strengthened rather than falsified it |
| the ring's citation | **WRONG FILE**, harmlessly — corrected below |
| the gap | **one edge**, not a corpus and not a mechanism |
| salience | **filled, and half-corrected** — see slice 1: the counts are live and stable, the nag-0 inference does not hold, the 0.009 credit is unverified |
| slice 1 — verify the cost figures | **DONE 2026-08-27** |

**Nouns / the two vectors** (`:23-49`):
- `:23-27`: "R2's pattern and the overlay both cite `futon3c/src/futon3c/aif/observe.clj` — *"ten named channels; all ten read mission state and none reads an operator turn"*. That is **true of that file**: ten channels, `:phase-progress` … `:days-since-last-activity`, and the nearest to the operator measures his **absence**."
- `:29-38`: "**But it is not the War Machine's observation vector.** That file is dated 2026-03-20 and is required by exactly one namespace, `futon3c/src/futon3c/aif/mission_head.clj`. The WM's vector is **`futon2/src/futon2/aif/observation.clj`** — *"The war machine's observation channels, harmonized from all vocabularies"* — and it carries **fourteen**: loop-health · support-coverage · attack-coverage · mission-health · stack-pct · consulting-pct · portfolio-pct · mathematics-pct · active-repo-ratio · sorry-count-norm · coupling-density · ticks-firing-ratio · depositing-signal · annotation-health"
- `:40-41`: "Sources: holistic argument, peripheral-aif, logic model / joe-hud, JSDQ, sorry topology, temporal analysis, daily scan frames, `stack-annotations.edn`."
- `:43-49`: "**None of the fourteen reads an operator turn either.** So the ring's claim survives the correction and is in fact broader than stated: **two independent observation vectors, twenty-four channels between them, zero operator-turn channels.** *(`aif-r1-r16-pattern-map.md` records R2 as "✓ 13 harmonized channels. Real." — the count is now 14, `:annotation-health` added at v0.10, and the ✓ is about schema quality, not operator coverage. The same two-axis divergence as R6 and R8.)*"

**The chain / edges it draws** (`:51-70`):
- `:53-54`: "**The turns exist and are queryable.** `futon1b :7073 /api/alpha/evidence` carries operator turns and context-retrieval events."
- `:56-61`: "**The turn→pattern association exists, is computed live, and is in the paper.** `p4ng/empirics-futon/gen_turn_chain.py` performs the join and emits both the figure and its numbers … Current counts (`turn-chain-counts.tex`): **27 turns, 27 turn→pattern edges, 24 patterns, 2 missions, 323 curated, 11 agree / 16 new.**"
- `:63-66`: "So R2's `THEN` — *"the turn→pattern association will provide enriched material"* — is **already built**. What is missing is not the corpus, not the association, and not the search: it is **a channel in `observation.clj` that reads it.** One edge, between two things that both run today."
- `:68-70`: "The pilot does not need to build the association. It needs to wire one that exists."

**The measured cost** (`:72-88`): quotes the pattern's salience ("nag 0 / brief 60 / silent 51 of 111 … The per-class credit collapsed to **0.009**") and asks: "**which bulletin, on what date, and is 0.009 the current credit or the value at the time of writing?**"

**Slice 1 — DONE 2026-08-27, what it verified** (`:90-136`):
- `:92-95`: "**Provenance found.** The figures come from `futon3c/holes/excursions/E-wm-operator-lane.md` (**2026-06-05**), which shipped the lane classifier and bulletin and recorded *"Live: `GET /api/alpha/war-machine/operator-bulletin` (nag 0 / brief 60 / silent 51 / total 111)"*."
- `:97-106`: "**They are current, not stale.** Queried live: `GET localhost:7070/api/alpha/war-machine/operator-bulletin` / date 2026-08-27 · generated-at 2026-08-27T11:21:24Z / nag [] · brief 60 · silent-count 51 · total 111 — Regenerated today, and **identical to 2026-06-05**. … *(Counts compared, not membership; whether the same 60 items are briefed is unchecked.)*"
- `:108-124`: "**But nag 0 does not mean what R2's salience reads into it.** The source excursion reads the same zero the opposite way: *"Result: all three → Brief; **the nag lane stays empty. This validates the design**"* — and nag is defined as an **AND of three conditions** … And decisively, from its Remaining list: *"acknowledged-state persistence (**so nag can fire**)"* — **the nag gate is blocked on unbuilt state.** … **It fired zero times because it is not wired to fire.**"
- `:126-129`: "**A better datum replaces it, and it is stronger.** The partition is **unchanged — 111 / 60 / 51 — across twelve weeks of live regeneration.** That is a measured inertness of the operator lane, obtained from a working instrument, and it does not depend on interpreting a blocked gate."
- `:131-133`: "**Still unverified, and not to be cited until it is:** the `0.009` per-class credit."
- `:135-136`: "**Recurring shape, fourth time today:** the number was real and the reading of it was not. R8's producer, R14's location, R6's proposer, and now R2's nag lane."

**Requirement in the family vocabulary** (`:138-165`):
- `:140`: "**Family 2 (non-empty handle), at the perception stratum, with R14's clause attached.**"
- `:143-144`: "> A channel that is never read is not a channel. An operator turn must enter the observation vector as content, and must be shown to change an inference result."
- `:146-150`: "R2's pattern already carries the second half — `?evidence(required)`: *"A pilot has not yet shown that processed operator turns enter the inference vector **and change an inference result**."* **That is the R14 lesson, pre-recorded.**"
- `:152-156`: "**❌ The naive fix: add an `:operator-turn-count` channel.** It recreates the defect exactly — it is `:days-since-last-activity` again, a measure of the operator's *presence*, not of what he said. … Counting the turns is not reading them."
- `:158-161`: "**✅ The requirement-satisfying fix:** the channel carries the **association's content** — the turn→pattern edge, which is what makes a turn machine-legible — and the pilot's acceptance is a *changed inference*, demonstrated, not a populated field."
- `:163-165`: "**Acceptance.** One tick where the observation vector differs with and without the operator channel, and the belief or ranking differs as a result. Anything less is `record_sensitivity_is_not_governance` at the other end of the loop."

**Why this one straddles the blanket** (`:167-177`):
- `:169-171`: "*"Unlike the other four rings this one straddles the blanket: the turns exist, are persisted, and are the one channel the apparatus cannot fabricate."*"
- `:173-177`: "R8, R14, R6 are all defects the machine could in principle repair alone. R2 is the only ring whose content originates **outside** the Markov blanket. That makes it the load-bearing ring for *"closing the AIF over the operator"* — and the `NOTE-thirtyfour-steps-both-levels.md` finding applies: the operator half has the feedback the WM lacks, and R2 is the edge that would carry it."

**Slice table** (`:179-190`):
1. ~~Verify the cost figures~~ — **DONE 2026-08-27**. "Residual: the `0.009` credit is still unverified, and brief *membership* was not compared across the twelve weeks, only counts."
2. **The one edge** — "what a `:turn-pattern-*` channel would carry, and whether `gen_turn_chain.py`'s join is the right shape to normalise into `[0,1]`." (no status marker)
3. **The pilot, with R14's acceptance bar** — "a demonstrated inference change, not a populated field." (no status marker)
4. **Formalisation** — "family 2 at the perception stratum; likely shares `inhabitedHandle` and `typedAbsence` with `GainChain.lean` rather than needing new vocabulary. Confirm before writing a module." (no status marker)

**Lean mentions in the excursion:** only slice 4 (`:188-190`, quoted above). No Lean module named for R2.

**Tickets cited:** none with a `T-` prefix in this file (`grep -n 'T-' E-R2-red-ring-fill.md` → no hits). Related list (`:192-201`): `E-R14-red-ring-fill.md`, `E-R6-red-ring-fill.md`, `futon2/src/futon2/aif/observation.clj`, `futon3c/src/futon3c/aif/observe.clj`, `p4ng/empirics-futon/gen_turn_chain.py` + `turn-chain-counts.tex`, `p4ng/empirics-futon/NOTE-thirtyfour-steps-both-levels.md`, `futon3c/holes/excursions/E-wm-operator-lane.md` (2026-06-05), `futon3c/src/futon3c/wm/operator_lane.clj`, `operator_bulletin.clj`.

---

## 4. LEAN — `mathlib4/DarkTower/WarMachine/*.lean`

Files present: `CascadeOrder.lean CommitmentTemperature.lean ContractEmitter.lean CoverageReport.lean GainChain.lean PolicyGrade.lean`.

Command: `grep -n -i 'R2\b\|observation\|operator\|turn\|channel' *.lean`

- `R2`: **not found** in any module.
- `operator` — one hit, `GainChain.lean:25-26`: "Nothing here repairs the running system; excursion slices 4 and 5 remain the operator's decision and are outside this file." (the human operator's decision, not an operator turn)
- `turn` (noun): **not found**.
- `observation` — `ContractEmitter.lean:15`: "The Clojure consumer may validate this document and observations against it; it must not maintain a separate family table." `PolicyGrade.lean:43`: "/-- The observations relevant to policy-grade naming after a finished run. -/ structure Run (Action Score : Type) where actions : List Action; score : Score". Neither refers to the R2 observation vector.
- `channel` — all hits are R14 (temperature→action channel) or R5 (coverage):
  - `CommitmentTemperature.lean:9-12`: "This standalone outline states the temperature-out face of R14. The required channel is behavioural: for some fixed ranking, changing commitment temperature changes the selected action. Merely copying the temperature into a record is explicitly weaker and is refused below."
  - `CommitmentTemperature.lean:66-77`: "/-- R14's uncompromised requirement. For a deterministic selector this is the finite behavioural form of positive information from temperature to the selected action: the channel is not constant. … A constant channel carries zero bits under every law, which is the direction this file uses. -/ def governs (s : Selector) : Prop := ∃ entries τ₁ τ₂, s τ₁ entries ≠ s τ₂ entries"
  - `CommitmentTemperature.lean:203-206`: "/-- The reading is perfect and the action channel is severed: the live record is temperature-sensitive while the live selector fails R14. …"
  - `CoverageReport.lean:33-35`: "* refusing-plausible-fix — `adding_a_channel_does_not_satisfy_coverage` (adding the one known missing outcome is the tempting repair and leaves the next one silent)"
  - `CoverageReport.lean:172-190`: "/-- The naive repair adds the known missing channel, but the next outcome outside the enlarged set is silent again. Klarna is the external analogue … -/ def oneChannelLarger … theorem adding_a_channel_does_not_satisfy_coverage : ¬ outsideIsTyped oneChannelLarger oneChannelLargerEvaluation"
- `inhabitedHandle` / `typedAbsence` (named by excursion slice 4) live in `GainChain.lean` (per R8 worksheet `R8-glossary-formalisation.md:122-123`).

---

## 5. CODE

### 5a. `futon2/src/futon2/aif/observation.clj` (80 lines)

Defns: `observation.clj:11 (def observation-channels`, `:34 (defn observe`, `:76 (defn sense->vector`.

ns docstring `observation.clj:1-9`:
> `"AIF observation layer for the War Machine. Normalises raw scan data (from \`futon2.report.war-machine\`'s \`scan-*\` projection functions) into a 13-channel observation vector in [0,1]. This is the War Machine's g-observe — the bridge between scan data and the AIF loop. cf. cyberants \`ants/aif/observe.clj\` — same pattern, strategic domain."`

`observation-channels` docstring `observation.clj:12-17`:
> `"The war machine's observation channels, harmonized from all vocabularies. Each channel is a named terminal with a source vocabulary and normalization. v0.10 added \`:annotation-health\` — the first belief-derived channel, sourced from \`stack-annotations.edn\`'s \`:lift-anomalies\` density. It is the channel for which R3a (likelihood model) is satisfied in v0.10."`

The vector `observation.clj:18-32` — **14 channels** (ns docstring says "13-channel"; the vector has 14; `observation_test.clj:38-39` asserts `(= 14 (count obs/observation-channels))` under "14 channels declared (v0.10 added :annotation-health)"):
```
:loop-health           ;; overall loop health [0,1] — from holistic argument
:support-coverage      ;; S1-S5 evidence coverage [0,1] — from holistic argument
:attack-coverage       ;; A1-A4 evidence coverage [0,1] — from holistic argument
:mission-health        ;; mission triage health [0,1] — from peripheral-aif
:stack-pct             ;; stack commit % [0,1] — from logic model / joe-hud
:consulting-pct        ;; consulting commit % [0,1] — from JSDQ
:portfolio-pct         ;; portfolio commit % [0,1] — from JSDQ
:mathematics-pct       ;; mathematics commit % [0,1] — from JSDQ
:active-repo-ratio     ;; active repos / total repos [0,1] — from logic model
:sorry-count-norm      ;; open sorrys / 10 (capped at 1) — from sorry topology
:coupling-density      ;; coupling edges / max edges [0,1] — from temporal analysis
:ticks-firing-ratio    ;; firing ticks / total ticks [0,1] — from logic model
:depositing-signal     ;; depositing cardinal direction [0,1] — from daily scan frames
:annotation-health     ;; 1 − (lift-anomalies / sections) — from stack-annotations.edn (v0.10; R3a-likelihood-derived)
```

`observe` docstring `observation.clj:35-42`: "Produce normalized observation vector from raw scan data. Returns a map of channel-id → [0,1] value. This is the war machine's g-observe: the bridge between raw scan data and the AIF loop. … When the field is absent, defaults to 0.0 — the apparatus remains operational under missing-canonical-source conditions." Inputs destructured at `:44`: `{:keys [loop-health support-attack mission-triage graph frames annotation-graph]} data`. No key of `data` relates to operator turns (`grep -n 'turn\|operator\|chat' observation.clj` → no hits).

### 5b. `futon2/src/futon2/aif/belief.clj` (1092 lines) — the 8 vs 6 split

`belief.clj:913-927`:
> `(def channels-with-likelihood "Set of observation channels for which an R3a likelihood model exists. v0.11: 4 channels (annotation-health, sorry-count-norm, mission-health, active-repo-ratio). E-support-coverage Cycle 3 (2026-05-26): +2 channels (support-coverage, attack-coverage), bringing total to 6. WM pilot cycle 2 (2026-05-30): +1 channel (coupling-density), bridging repo-level temporal-coupling edges to entity-level belief by source repo. WM pilot cycle 4 (2026-05-30): +1 channel (ticks-firing-ratio), bridging logic-model tick checks to first-class tick entities in stack annotations. Remaining sorries in \`futon2/data/sorrys.edn\` stay \`:prototyping-forward\` pending their own prerequisites; the 6 reclassified on cg-18b7831b were \`:n-a-by-design\`."`
> `#{:annotation-health :sorry-count-norm :mission-health :active-repo-ratio :support-coverage :attack-coverage :coupling-density :ticks-firing-ratio})`

So 8 with likelihood; the 6 without are `:loop-health :stack-pct :consulting-pct :portfolio-pct :mathematics-pct :depositing-signal` (the complement of the set above in `observation-channels`).

`belief.clj:1053-1069` `predict-observation` docstring: "Predict observation distributions across all channels for which a likelihood model exists. Returns a map of channel-id → `{:mean :variance}`. … Channels in `channels-with-likelihood` are included; others are absent (callers handle absence by falling back to preference-gap-driven scoring)."

`belief.clj:929-943` (channel block of A): "v0.24 (M-aif-faithfulness B-3a): the CHANNEL BLOCK of A. The predict-* likelihood models above ARE the channel half of the observation model — each channel's mean is a per-entity dot product of the posterior with a fixed status→emission row, aggregated over a cohort. … HONESTY: this is a documentation-grade UNIFICATION, consistency-tested against the live helpers (belief-test), NOT the executing path"; `:945`: "`channels-with-likelihood` have no emission row (no likelihood model".

`belief.clj:37-42` `status-set` = `#{:spawned :refined :strengthened :addressed :falsified :foreclosed :reopened}` (7 statuses; the glossary's "7 × 7" A matrix).

`grep -n 'turn\|operator\|chat' belief.clj` → no hits relevant to operator turns (not found).

### 5c. The other observation vector — `futon3c/src/futon3c/aif/observe.clj`

`observe.clj:1-12` ns docstring: "Mission observation channels — 10 normalized [0,1] signals for the Mission Peripheral's AIF head. Each channel is a pure function from mission state to [0,1]. …"
`observe.clj:140-152`:
> `(def channel-keys "Ordered list of mission observation channel keys. Matches peripheral-aif-vocabulary.sexp :mission entry." [:phase-progress :obligation-satisfaction :required-outputs-present :structural-law-compliance :prediction-divergence :evidence-for-completion-criteria :gate-readiness :argument-claim-coverage :cycle-count :days-since-last-activity])`

### 5d. Where operator/chat turns are read

Command: `grep -rn 'chat-turn\|operator-turn\|acknowledged?' /home/joe/code/futon2/src /home/joe/code/futon3c/src`

futon2/src (1 hit):
- `futon2/src/futon2/aif/lane_futility.clj:334`: `:acknowledged? true})))))`

futon3c/src:
- `futon3c/src/futon3c/marks.clj:1-17` ns docstring: "Mark recognizer for chat-turn and turn-round evidence. Vocabulary v0: ✘ correction, ✓ approval, 💡 idea. Two channels, one vocabulary (M-points-de-fuite author-invariance): - operator chat-turns mint the core tags (:correction :approval :idea) — the gold channel with γ/label semantics; - agent turn-rounds … mint self-prefixed tags … Marks are queryable through evidence tags, because text indexing strips the glyphs. The recognizer is pure and the boundary wiring is kill-switchable with FUTON3C_MARK_RECOGNIZER=false (both channels) and FUTON3C_SELF_MARKS=false (agent channel only)."
- `marks.clj:77-79` `(defn- chat-turn? [entry] … (= "chat-turn" (some-> event name)))`; `:81-82` `(defn- operator-turn? [entry] (= "joe" (str/lower-case (str (author-of entry)))))`; `:212` "Decorate an operator chat-turn evidence entry with parsed marks."; `:216`.
- `futon3c/src/futon3c/wm/operator_lane.clj:24` `{:in-joes-model? :futon-important? :risk-mode? :acknowledged?`; `:32-33` `[{:keys [in-joes-model? futon-important? risk-mode? acknowledged?]}] (boolean (and in-joes-model? futon-important? risk-mode? acknowledged?)))` (the nag conjunction).
- `futon3c/src/futon3c/wm/needs_you.clj:156`: `;; Pre-laning is deliberate: operator-lane's :acknowledged? input is not`
- `futon3c/src/futon3c/logic/wm_operator_lane_invariants.clj:47,52,60,207,221,230,239,249` (pldb facts `acknowledgedo`, test fixtures with `:acknowledged? true/false`).
- `futon3c/src/futon3c/portfolio/observe.clj:155`: `(count (filter #(= "chat-turn"` (a portfolio observation, not the WM vector).
- `futon3c/src/futon3c/nlp/classical_pipeline.clj:201-202,216-251` (`(= "chat-turn" (:event body))` and fixture entries `{:event "chat-turn" :text "…" :role "user"}`).
- `futon3c/src/futon3c/peripheral/mission_control_backend.clj:49` "Bound live chat-turn evidence scans. This is ancillary telemetry"; `:1061` `(and (= "chat-turn" (some-> event name))`; `:1074-1077` "chat-turn evidence is :evidence/type :coordination … chat-user-turn-entry? narrows to event=chat-turn + role=user."
- `futon3c/src/futon3c/watcher/commit_ingest.clj:239` `;; (chat-turn, invoke-heartbeat, invoke-complete, …) but does NOT`
- `futon3c/src/futon3c/transport/http.clj:3451` "Semantic turn evidence is recorded elsewhere (`chat-turn`,"

None of the hits is in `futon2/src/futon2/aif/observation.clj` or `belief.clj`.

### 5e. The two flexiargs

`futon3/library/features/operator-turns-enter-the-observation-vector.flexiarg` (231 lines):
- `:1-13` header: `@flexiarg features/operator-turns-enter-the-observation-vector` / `@title Read the Operator's Turns, Not His Absence` / `@style implementation-pattern` / `@why war-room/wr-16-…` / `@holds-at R2` / `@builds-from problems/operator-turns-become-inference-observations` / `@audience whoever picks up C-R2` / `@status [status[specified] blocked-by[none]]`
- `:15-19` conclusion: "Add one channel to the mission observation vector that reads a typed operator TURN, source it from the mark vocabulary that already types those turns, and prove it is read by showing an inference result that moves with the channel and not without it."
- `:36-38`: "Structure -> inference DOES NOT. futon3c.aif.observe/channel-keys is ten channels wide and every one of them is a function of mission state. Nothing in that vector is a function of anything Joe did."
- `:108-113` IF / HOWEVER: "Operator turns are captured, typed, and text-searchable." / "The observation vector cannot see them, and the one place the apparatus does claim to represent Joe is unwired."
- `:115-125`: "`operator-lane/nag?` is a conjunction of four terms and the fourth is `:acknowledged?`. Nothing sets it. … the one path that bypasses the classifier with a pre-set lane reads `data/wm/needs-you.edn`, which is `[]`. So nag 0 of 111 on the live bulletin is not a threshold that wants tuning. The gate cannot open, because the term that would open it has no producer -- and that term is definitionally an operator turn: it means Joe LOOKED."
- `:127-153` THEN: steps 0 (Wire acknowledgement — "NOT this feature"), 1 ("Emit an operator-turn observation. One channel appended to `channel-keys`, normalised to [0,1] like its neighbours, whose value is a function of the marks on turns addressed to the item"), 2 ("Show it is read. Run one window of >= 111 classifications with the channel live, and the same window with it held at a constant").
- `?evidence(required)`: **not in this file** (`grep -n '?evidence' …features/operator-turns-enter-the-observation-vector.flexiarg` → no hits). It is in the problems flexiarg it `@builds-from`:

`futon3/library/problems/operator-turns-become-inference-observations.flexiarg` (44 lines):
- `:1-5`: `@flexiarg problems/operator-turns-become-inference-observations` / `@title Operator Turns Become Inference Observations` / `@style pattern` / `@why war-room/wr-16-…` / `@holds-at R2`. **No `@status` line** (`grep -n '@status'` → no hits).
- `:7` conclusion: "Process captured operator turns into structures that inference can consume, using the turn-to-pattern association as grounded enrichment."
- `:9-12`: "Red ring at R2, ruling WR-16, established 2026-08-25 in p4ng/empirics-futon/wr-overlay.edn. Operator: Joe — the IF, HOWEVER, THEN, BECAUSE and NEXT-STEPS below are his wording. No cascade mission supplies the mechanism, so this pattern has no supplier on its WIP card."
- `:13-25` salience: "futon3c/src/futon3c/aif/observe.clj builds the observation vector from ten named channels; all ten read mission state and none reads an operator turn. The nearest, days-since-last-activity, measures the operator's ABSENCE. Downstream the cost is measured: the operator lane classifies every item into nag/brief/silent and on the live bulletin read nag 0 / brief 60 / silent 51 of 111 — the strongest representation the loop has of the operator fired zero times in a hundred and eleven opportunities. The per-class credit collapsed to 0.009, roughly a sixteenth of where an action starts, …That collapse is not a measurement of the action class; it is the shape of a missing variable."
- `:26-28`: "whose problem: the operator's, and the learning loop's. Unlike the other four rings this one straddles the blanket: the turns exist, are persisted, and are the one channel the apparatus cannot fabricate."
- `:30-44` IF: "we are capturing operator turns" / HOWEVER: "these are opaque to automation" / THEN: "process them into structures that can be used by inference" / BECAUSE: "we know that works for design patterns, and the turn→pattern association will provide enriched material that is grounded in immediate practical considerations"
- `:41`: `?evidence(required): A pilot has not yet shown that processed operator turns enter the inference vector and change an inference result.`
- `:43-44` NEXT-STEPS: "since we have full text search via an XTDB sidecar, this can now be piloted"

---

## 6. BADGES

### `futon2/data/r18-badges.edn`
- `:25` in `:requirements`: `"R2" "Structured observation vector"`
- `:44` in `:nodes`: `{:SI {:requirement "R2" :quantities []}` — the sensory-input node has an empty quantities list (no badge attached). `grep -n ':observation' r18-badges.edn` → **not found**.

### `p4ng/empirics-futon/wr-overlay.edn` — R2 badge, lines 21–36 (the revised note, verbatim)
```
  ;; R2 revised 2026-08-25 (claude-13, at Joe's direction). It read :holds true
  ;; on the strength of the machine-side channel alone. The constraint is not met
  ;; for the operator's side: turns are captured and persisted (:evidence-persistence
  ;; is satisfied on the star map) and nothing renders them into the observation
  ;; vector, so a channel that exists is not first-class. Stated as a fail with a
  ;; note, in the same shape as R5, rather than as a second ring on one node.
  ;; Verified 2026-08-25 against futon3c/src/futon3c/aif/observe.clj: the
  ;; observation vector is built from ten named channels (phase-progress,
  ;; obligation-satisfaction, required-outputs-present, structural-law-compliance,
  ;; prediction-divergence, evidence-for-completion, gate-readiness,
  ;; argument-claim-coverage, cycle-count-signal, days-since-last-activity).
  ;; All ten read mission state; none reads an operator turn. The nearest,
  ;; days-since-last-activity, is a proxy for the operator's ABSENCE.
  ;; The other fifteen badges were NOT re-verified today, so file-level :as-of
  ;; stays 2026-08-22 and this badge carries its own :established instead.
  {:node "R2"  :wr "WR-16" :holds false :note "first-class for machine evidence, not for the operator's -- observe.clj wires ten channels, all mission-state, none reading operator turns; turns persist to the landscape unread" :established "2026-08-25"}
```

### `futon2/holes/aif-r1-r16-pattern-map.md:28` (R2 row)
> `| **R2** | Observation channel schema | \`structured-observation-vector\` | ✓ | 13 harmonized channels. Real. |`

Adjacent R1 row `:27`: "Disaggregated (`3347575`): doc's "4/14" is STALE — **8** channels have likelihood models; the other 6 are **N/A-by-design** (externally-measured, no per-entity belief substrate — a fake mapping would be needed). So channel coverage is COMPLETE for the belief-derivable half."

---

## 7. CORPUS — `futon2/data/wm-trace/`

Directory holds 52 `wm-trace-2026-*.edn` files (05-18 … 07-21) plus `wm-shadow-step.json`.

**Exact command** (reader loop over all top-level forms; `clojure.edn/read` on a `PushbackReader` with `:eof` sentinel and `{:default (fn [t v] v)}`):
```bash
cd /home/joe/code/futon2/data/wm-trace && cat > /tmp/r2-corpus.clj <<'EOF'
(require '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.string :as str])
(defn read-forms [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [acc []]
      (let [x (edn/read {:eof ::eof :default (fn [t v] v)} r)]
        (if (= x ::eof) acc (recur (conj acc x)))))))
(def files (sort (filter #(re-matches #"wm-trace-2026-.*\.edn" (.getName %)) (file-seq (io/file ".")))))
(def recs (vec (mapcat (fn [f] (map #(assoc % ::file (.getName f)) (filter map? (read-forms f)))) files)))
(println "files" (count files) "records" (count recs))
(println "records with :observation" (count (filter #(contains? % :observation) recs)))
(println "\n== :observation key-sets (sorted keys -> count) ==")
(doseq [[ks n] (sort-by (comp - val) (frequencies (map #(vec (sort (keys (:observation %)))) (filter #(map? (:observation %)) recs))))]
  (println n ks))
(println "\n== observation keys matching operator|turn|morning|brief ==")
(println (distinct (filter #(re-find #"(?i)operator|turn|morning|brief" (name %)) (mapcat #(keys (:observation %)) (filter #(map? (:observation %)) recs)))))
(println "\n== ANY top-level record key matching operator|turn|morning|brief|acknowledg ==")
(println (frequencies (filter #(re-find #"(?i)operator|turn|morning|brief|acknowledg" (name %)) (mapcat keys recs))))
(doseq [k [:morning-brief-events :morning-brief-consumed-event-ids :morning-brief-held-events]]
  (let [rs (filter #(contains? % k) recs)
        counts (map #(let [v (get % k)] (if (coll? v) (count v) v)) rs)]
    (println "\n==" k "records:" (count rs) "files:" (distinct (map ::file rs)))
    (println "   inner counts freq:" (frequencies counts))))
EOF
bb /tmp/r2-corpus.clj
```

**Output (2026-08-30):**
```
files 52 records 791
records with :observation 791

== :observation key-sets (sorted keys -> count) ==
789 [:active-repo-ratio :annotation-health :attack-coverage :consulting-pct :coupling-density :depositing-signal :loop-health :mathematics-pct :mission-health :portfolio-pct :sorry-count-norm :stack-pct :support-coverage :ticks-firing-ratio]
2 [:active-repo-ratio :attack-coverage :consulting-pct :coupling-density :depositing-signal :loop-health :mathematics-pct :mission-health :portfolio-pct :sorry-count-norm :stack-pct :support-coverage :ticks-firing-ratio]

== observation keys matching operator|turn|morning|brief ==
()

== ANY top-level record key matching operator|turn|morning|brief|acknowledg ==
{:morning-brief-events 31, :morning-brief-consumed-event-ids 31, :morning-brief-held-events 31}

== :morning-brief-events records: 31 files: (wm-trace-2026-07-14.edn wm-trace-2026-07-15.edn wm-trace-2026-07-16.edn wm-trace-2026-07-17.edn wm-trace-2026-07-18.edn wm-trace-2026-07-19.edn wm-trace-2026-07-21.edn)
   inner counts freq: {0 31}
== :morning-brief-consumed-event-ids records: 31 …same 7 files…
   inner counts freq: {0 31}
== :morning-brief-held-events records: 31 …same 7 files…
   inner counts freq: {0 31}
```

Summary of the numbers:
- 791 records; every record carries `:observation`; two distinct key sets: **14 keys (789 records)** and **13 keys (2 records, both in `wm-trace-2026-05-18.edn`, lacking `:annotation-health`)**.
- **No `:observation` key matches operator|turn|morning|brief** (empty list).
- The only top-level keys matching those terms are the three `:morning-brief-*` keys, present on **31 records** across 7 files (07-14 … 07-21); the value is `[]` in **all 31** (second pass: `morning-brief value types: {[] 31}`).
- There is no schema-version key; the only version-ish key is `:wm-version` (109 records). Records per file range 1–38; 07-17, 07-18, 07-19 have 1 record each, 07-21 has 2.
- Top-level keys of the 07-17 record: `:anticipation :decision :free-energy :habit-prior-state :micro-step-trace :mode :morning-brief-consumed-event-ids :morning-brief-events :morning-brief-held-events :mu-post :mu-pre :observation :policy-support-exclusions :precision-state :prediction-errors :ranked-actions :selection-gain :timestamp :variational-free-energy :wm-version`.
- Example `:observation` (07-17): `{:mathematics-pct 0.0677…, :coupling-density 0.4640…, :support-coverage 0.0, :depositing-signal 0.0, :loop-health 0.1392…, :mission-health 0.0, :portfolio-pct 0.0539…, :annotation-health 0.9951…, :ticks-firing-ratio 0.75, :active-repo-ratio 0.7777…, :attack-coverage 0.0, :consulting-pct 0.0363…, :sorry-count-norm 0.8, :stack-pct 0.8420…}`.

Producer of the `:morning-brief-*` keys: `futon2/src/futon2/aif/trace.clj:257-260` (`:morning-brief-events (:morning-brief-events judge-output [])`, `:morning-brief-held-events …`, `:morning-brief-consumed-event-ids …`); read back by `futon2/src/futon2/aif/tripwire.clj:679`; namespace `futon2.aif.morning-brief` at `futon2/src/futon2/aif/morning_brief.clj`.

### Operator-turn evidence store

- `c1_turn_survival.py`: **not in futon3c** (`find futon3c -name 'c1_turn_survival.py'` → nothing; `ls futon3c/scripts/c1_turn_survival.py` → "No such file or directory"). It is at **`/home/joe/code/futon2/scripts/c1_turn_survival.py`** (13211 bytes, mtime Aug 27 18:03). Lines 1–23:
```python
#!/usr/bin/env python3
"""Report C1 turn survival from an external transcript to Evidence Landscape.

The transcript supplies the denominator.  Evidence is joined by a shared key
when one exists; today it does not, so exact operator-message text is used as a
reported fallback.  A fallback miss is never asserted to mean "not stored".
"""

import collections
import datetime
import glob
import json
import os
import re
import sys
import urllib.parse
import urllib.request


DEFAULT_SESSION = "66f62b84-6002-47d4-9a52-5577415ad163"
TRANSCRIPT_DIR = "/home/joe/.claude/projects/-home-joe-code"
EVIDENCE_URL = "http://localhost:7073/api/alpha/evidence"
LIMIT = 1000
```

- Ticket: **`/home/joe/code/futon3c/holes/tickets/T-wm-turns-are-not-operator-turns.md`**, lines 1–15:
```
# T-wm-turns-are-not-operator-turns — the WM's ticks get none of the treatment operator turns get

**Opened:** 2026-08-27 · claude-13, from Joe: *"my point was to try and align the
WM with operator-facing considerations … WM 'turns' are not stored in quite such
a durable or queriable or annotatable fashion."*

**Status:** open. **Diagnosis substantially corrected 2026-08-27 — see
"Correction" below. The repair is a switch and a verification, not a build.**

## The finding

Operator turns came out of the C1 exercise clean — 20 of 20 stored, and every
reported loss turned out to be my own instrument. The asymmetry is that the War
Machine's ticks were never in that régime at all.
```

---

## 8. MISSION — `futon2/holes/missions/M-formal-war-machine.md`

Command: `grep -n -i 'R2\b\|operator turn\|operator-turn\|observation vector\|\bC1\b\|c1_turn_survival\|FUTON2_WM_EMIT_EVIDENCE\|close over the operator' M-formal-war-machine.md`

- `:328-332`: "This is the same defect as the others in this section, one level up: not a wrong value, a wrong *type*. And unusually, it applies to the operator's half of the loop rather than the machine's — the half that came out of the C1 exercise clean. Operator turns are durable, queryable and annotatable, and still typed as a sequence when they are not one."
- `:447-450` (§3 DERIVE, opened 2026-08-27, Joe): "*"we've made a good modular discussion of the red rings … I suggest that we focus now on these repairs. Our other ideas about AIF-driven metadata for operator turns and so forth are really only useful if we have a well-working WM to plug them into."*"
- `:456-459`: "… R6's "tension-proposer unbuilt" (`aif2/tension.clj`, live since 06-01), R2's citation (the wrong observation vector) and its nag-0 inference (a gate blocked on unbuilt state), and R8's "dead since 07-14" (a deliberate stop with a written re-arm condition). The verdicts mostly survived. **The reporting did not.**"
- `:714-721` "Where the build has got to" table: `| **R2** | \`E-R2-red-ring-fill\` | — | — | no family assigned |` (columns: ring · excursion · Lean module · vocabulary · in the emitted contract). For comparison, R8 → `GainChain.lean`, R14 → `CommitmentTemperature.lean`, R5 → `CoverageReport.lean`/`PolicyGrade.lean`, R6 → "reserved, family 6".
- `:809-814` (Joe, 2026-08-27): "*"I think we need something considerably weaker than `warMachineCompliant` — and this is where the 'closure over the operator' matters. E.g. 'every operator turn is stored in the Evidence Landscape' is an obvious one … But these can be chained: 'every operator turn is stored in the Evidence Landscape, processed with Air, decorated with metadata, forming part of a candidate cascade, and used in problem selection by the WM'."*"
- `:825-831` criterion table: **C1** "every operator turn is stored in the Evidence Landscape"; **C2** "C1, and processed with Air"; **C3** "C2, and decorated with metadata"; **C4** "C3, and forms part of a candidate cascade"; **C5** "C4, and is used in problem selection by the WM".
- `:844-846`: "**The denominator has to be external, and that is the point.** C1 asks what fraction of operator turns reached the store — and the store cannot answer, because the store is the numerator. Anything that never arrived is invisible from inside."
- `:851-857`: "#### C1 is not checkable today, and the reason is not missing data. Measured 2026-08-27 … the operator's own transcript — carries **26 operator turns** (231 `user`-typed records, of which 205 are tool results rather than turns). The Evidence Landscape carries **324** distinct `claude-13-turn-N` ids. **The two numbers are in different units and nothing reconciles them.**"
- `:868-872`: "`futon2/scripts/c1_turn_survival.py` (codex-22, `4e62e97a`; corrected at review, `48bf098`). Its most useful output is the negative one: **there is no per-turn key.** Session ids join (`sessionId` ↔ `:evidence/session-id`); the transcript's `uuid` and `promptId` have zero hits in evidence; `:evidence/in-reply-to` is an emacs transport id that does not appear in the transcript at all."
- `:878-884`: "The evidence-text regex in `c1_turn_survival.py` was anchored on `"}`, so it matched only entries where `:text` is the last field of the body … Unanchored, the current snapshot gives **19 operator turns, 19 matched, no losses.** codex-22's independently produced `missing_chat_turns: 0` was right."
- `:904-907`: "**The root cause is that the instrument was a regex over EDN.** The evidence API serves JSON on `Accept: application/json`; the scraping was never necessary. `c1_turn_survival.py` now parses (`90baa27`), and the whole class of bug goes with it. Current reading: **20 operator turns, 20 matched, no losses.**"
- `:919-926`: "**The external denominator is not stable, and this is the finding.** The transcript shrank from 1,968,337 to 1,945,752 bytes *during* the session, and the operator-turn count fell from 23 to 19: compaction rewrites the record. … **A retrospective C1 therefore self-heals: the losses vanish along with the evidence that they happened.**"
- `:983-985` (repair ordering): "3. **R6 (family 9, weights versus membership)** and **R2** — designed in their excursions, unbuilt. Building them *after* the standard is what tests whether the standard was worth writing."
- `:994-997` (Joe): "*…A WM tick or click is somewhat like an operator turn, but it's also like a series of turns pegged to a Mission, with the strict analogy Flight ≈ Mission."*"
- `:1001-1006` grain table: `| **operator turn** | one submission into the REPL | the atom |` · `| **agent turn** | one reply, with its tool calls | the atom, other side |` · `| **WM tick / click** | one pass of the control loop | **not a turn at all** — see the correction below |` · `| **Flight** | a run of ticks toward one end | **Mission** (Joe's strict analogy) |`
- `:1042-1046`: ""Every operator turn is stored" and "every tick is stored" are not the same shape of claim, because a tick has no boundary in the record until something writes one."
- `:1061-1065`: "**Agent turns are persisted**, in the same shape as operator turns, with a `turn-id` (`claude-13-turn-N`) and a `clocked-mission` on 140 of 144. So the operator/agent *pair* is already recorded at turn grain and already pegged to a mission."
- `:1076`: "| `futon2/src/futon2/aif/evidence_emit.clj` | *"Best-effort Evidence Landscape emitter for War Machine ticks. **Disabled by default.** Set `FUTON2_WM_EMIT_EVIDENCE` to 1/true/yes/on."* Emits `wm-tick`, `wm-click`, `wm-cron`. |"
- `:1112-1116`: "On the WM side the trace goes to `data/wm-trace/*.edn` (gitignored, last written 2026-07-21) **and**, when `FUTON2_WM_EMIT_EVIDENCE` is set, to the Evidence Landscape as `wm-tick` / `wm-click` / `wm-cron`."
- `:1182-1184`: "Three findings were written up here, committed, and wrong: seven lost operator turns that were an anchored regex; a duplicate write that was my own reply quoting Joe; a WM tick record reported absent that a `grep -rl wm-click` would have found."
- `:1475-1478` (Joe, 2026-08-28): "*"'all operator turns are logged to the evidence landscape' is the fundamental invariant, and everything else — metadata, inference — builds from that. …"*"
- `:1519-1523`: "L0 is the analogue of Joe's operator invariant. *All operator turns are logged to the evidence landscape* is checkable because the landscape is somewhere else: the denominator is external (§3.1d). L0 says the same thing for the machine…"
- `:1632-1636`: "The WM's own L0 already has a name and a switch — `FUTON2_WM_EMIT_EVIDENCE`, disabled by default (`T-wm-turns-are-not-operator-turns`) — and the check that would make it real is the operator-side one already written, `c1_turn_survival.py`, whose denominator is external by construction."
- `:1823`: "**This is the same shape as the C1/end-to-end work** (§3.1d): a property over a population rather than over one record — but indexed by *time* instead of by *stage*."
- `:1962-1964`: "- **R2's channel** — one edge, between two things that already run. This is the *"close over the operator"* edge, and Joe's sequencing puts it last for the right reason: it is the plug, and the socket is Tier 1."

---

## Not-found register (commands used)

| looked for | command | result |
|---|---|---|
| `R2` in Lean | `grep -n -i 'R2\b' mathlib4/DarkTower/WarMachine/*.lean` | not found |
| "operator turn"/"turn" noun in glossary | `grep -n -i 'operator turn\|chat-turn' p4ng/sec-glossary.tex` | not found |
| `?evidence` in features flexiarg | `grep -n '?evidence' futon3/library/features/operator-turns-enter-the-observation-vector.flexiarg` | not found (it is in the problems flexiarg, line 41) |
| `@status` in problems flexiarg | `grep -n '@status' futon3/library/problems/operator-turns-become-inference-observations.flexiarg` | not found |
| `:observation` in r18-badges.edn | `grep -n ':observation' futon2/data/r18-badges.edn` | not found |
| `c1_turn_survival.py` in futon3c | `find futon3c -name 'c1_turn_survival.py'` | not found (exists in `futon2/scripts/`) |
| `T-` tickets cited in E-R2 | `grep -n 'T-' futon3c/holes/excursions/E-R2-red-ring-fill.md` | no ticket ids; the only hits are `:68` "NEXT-STEPS" and `:195` "SELECT-stratum" |
| operator/turn keys in `:observation` maps (corpus) | bb script above | not found (empty list) |
| operator turns in `observation.clj` | `grep -n -i 'turn\|operator\|chat' futon2/src/futon2/aif/observation.clj` | only `:36` "Returns a map of channel-id → [0,1] value." (substring of "Returns") |
| operator turns in `belief.clj` | `grep -n -i 'operator\|chat\|\bturn' futon2/src/futon2/aif/belief.clj` | only `:392` "The flip to :a-matrix or :aif is the operator's (arena-*-mode idiom)." and `:520` ";; Entity-tag classification at bootstrap. Decision (operator-approved" — the human operator's decisions, not turns |
