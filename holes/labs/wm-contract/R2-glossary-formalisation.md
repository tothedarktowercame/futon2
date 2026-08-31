# R2 worked through the glossary — nouns, their Gate-0 class, and what a formal R2 would have to state

**Date:** 2026-08-30. **Author:** claude-15, from `facts-R2.md` (gathered 2026-08-30, spot-checked: `:observation` carries 14 keys in 789 records and 13 in 2 (both 05-18) against a ns docstring that says "13-channel"; no `:observation` key matches operator/turn/morning/brief; `:morning-brief-events` = `[]` in all 31 records that carry it; `c1_turn_survival.py` is at `futon2/scripts/`, not futon3c).
**Status:** worksheet, not specification. Format and classes as `R8-glossary-formalisation.md`.
**Sources:** `p4ng/sec-glossary.tex` (incl. its `% QUESTION` comments); `p4ng/sec-catalog.tex:194`; `futon-aif-completeness.md:72–78`; `promotion-tests.edn:36–96`; `futon2/src/futon2/aif/{observation,belief}.clj`; `futon3c/src/futon3c/aif/observe.clj`; `futon3c/src/futon3c/{marks,wm/operator_lane}.clj`; `futon3/library/{features,problems}/operator-turns-*.flexiarg`; `futon2/data/wm-trace/`; `E-R2-red-ring-fill.md`.

---

## 0. First finding: the ring is on the one channel the machine cannot fabricate, and it is the one channel it does not read

| | R2 as certified | the red ring (WR-16) |
|---|---|---|
| noun | **observation vector o**: "a standardized summary of what happened … normalized into named channels"; completeness: "**Satisfied.** 13 channels"; pattern map `✓ 13 harmonized channels. Real.` | **the operator's turns as observation**: turns "are captured, typed, and text-searchable" and "the observation vector cannot see them" (features flexiarg `:108–113`) |
| what exists | two vectors — `futon2/aif/observation.clj` (14 channels, the WM's) and `futon3c/aif/observe.clj` (10, the mission head's) — "twenty-four channels between them, **zero** operator-turn channels" (`E-R2:43–49`) | turns persisted to `futon1b :7073 /api/alpha/evidence`; `gen_turn_chain.py` joins them to patterns live (27 turns / 27 edges / 24 patterns) and the figure is in the paper |
| the gap | | **one edge**: "a channel in `observation.clj` that reads it" (`E-R2:63–66`) |

`E-R2` names the symmetry: *"R14 — a quantity is computed, recorded, and cannot
reach the action. R2 — a quantity is computed, recorded, and cannot reach the
belief … The loop is open at both ends."* And the asymmetry that makes this the
important ring: *"R8, R14, R6 are all defects the machine could in principle
repair alone. R2 is the only ring whose content originates outside the Markov
blanket."* The C1 exercise found operator turns stored 20 of 20 — the only part
of the loop that came out clean.

## 1. R2's closure in the glossary, classified (Gate 0)

| glossary noun | what the glossary says | class | code · trace · Lean |
|---|---|---|---|
| **Observation vector o** | "standardized summary … named channels"; `% QUESTION` in the source: *"it seems to relate, also, to establishing a shared context over time"* | **stack-defined**: 14 channels "harmonized from all vocabularies", each `;; — from <scan source>`; the ns docstring says 13 | `observation.clj:11,18–32` · `:observation` (14 keys ×789) · none |
| **Generative model** | "the system's story about how hidden causes produce observable evidence … the observations are things such as test results, trace records, diffs, and **operator feedback**" | **stack-defined** (pointer to three `.clj` files); note "operator feedback" is *in the definition* and in no channel | `belief.clj`, `preferences.clj`, `efe.clj` · — · none |
| **Belief state μ** | "compact map of operational hypotheses"; `% QUESTION`: *"How does the belief state relate to memory?"* | **stack-defined**; 8 of 14 channels have a likelihood model, 6 are `:n-a-by-design` | `belief.clj:913–927 channels-with-likelihood` · `:mu-pre :mu-post` · none |
| **Observation model A** | "for each hidden state s … P(o∣s)"; "explicit, column-normalised 7×7 matrix over the entity-status vocabulary … three entry classes" | **theory-shaped** (a likelihood matrix, column-normalised) over a **stack-defined** 7-status vocabulary; the *channel* half is the 8 predict-* models | `belief.clj:37–42, 929–943` · — · none |
| **Prediction error ε, Precision Π** | (see R8) | theory-defined formulas over the stack-defined channels | `:prediction-errors :precision-state` |
| **operator turn** | **no glossary entry**; "turn" occurs only as a verb | **undefined** in the glossary; **stack-defined and well-typed elsewhere**: `marks.clj:77–82` `chat-turn?` / `operator-turn?` (author "joe"); the mark vocabulary ✘ ✓ 💡 | `futon3c/marks.clj` · futon1b evidence store · none |
| **acknowledged?** | not in the glossary; `operator-lane/nag?` is "a conjunction of four terms and the fourth is `:acknowledged?`. **Nothing sets it.**" (features flexiarg `:115–125`) | **undefined** — a declared input with no producer | `operator_lane.clj:24,32–33` · — · none |

**Gate-0 verdict for R2.** The machine-side vector is stack-defined and
honestly so — every channel names its scan source — and the likelihood matrix
is theory-shaped. What is *undefined* is the noun the ring is about: the
glossary has no entry for an operator turn as an observation, though its
*Generative model* entry lists "operator feedback" among the observations, and
its two `% QUESTION` comments (memory; shared context over time) are the
author asking for exactly that noun. R2 is the node where the glossary's own
unanswered questions are the requirement.

## 2. What the corpus holds — checked 2026-08-30

- **14 keys, not 13.** 789 records carry 14 observation keys; the two 05-18 records carry 13 (no `:annotation-health`). The ns docstring, the completeness doc, and the pattern map say 13. Three sources, two numbers, one vector — a schema that drifted by one channel without any surface noticing.
- **No operator content in any observation.** Zero keys matching operator / turn / morning / brief across 791 records. The nearest channel in either vector is `days-since-last-activity` — "a proxy for the operator's ABSENCE" (`wr-overlay.edn`).
- **The morning-brief keys exist and are empty.** `:morning-brief-events`, `:morning-brief-consumed-event-ids`, `:morning-brief-held-events` are present in 31 records (07-14..07-21), every value `[]`. A channel with a name, a producer (`trace.clj:257–260`), and nothing ever in it — the *present-and-empty* facade, at the perception stratum.
- **The turns themselves are fine.** `c1_turn_survival.py` (`futon2/scripts/`): 20 of 20 operator turns stored; "every reported loss turned out to be my own instrument" (facts-R2 §"The finding").

## 3. What a formal R2 would have to state — signatures, in dependency order

**Definable now — the machine-side vector:**

```
Channel        : Type                             -- 14, enumerated in observation.clj:18–32 (fix the docstring)
Observation    := Channel → [0,1]                 -- observe : Scan → Observation      [observation.clj:34]
Status         : Type                             -- 7 statuses                          [belief.clj:37–42]
A              : Matrix Status Status              -- column-normalised; three entry classes   [belief.clj:929–943]
likelihood     : Channel → Option (Belief → Gaussian)   -- Some for 8 channels, None for 6 (typed absence, not 0)
```
Record contracts, statable now: `∀ tick, keys tick.observation = Channel`
(refused by the two 13-key records — a real schema-drift witness);
`∀ c, likelihood c = None → c ∉ channels used by predict-observation`
(the `:n-a-by-design` six, stated as a type rather than a docstring).

**Definable now — the requirement (from the promotion test, `promotion-tests.edn:36–50`):**

```
Turn           : Type                             -- a typed operator turn: author = joe, mark ∈ {✘, ✓, 💡}   [marks.clj]
turnChannel    : List Turn → [0,1]                -- a channel "whose value is a function of a typed operator TURN
                                                  --  rather than of the operator's absence"
readsTurns     : (infer : Observation → Belief) → Prop
               -- ∃ window ≥ 111 items, infer (o with turnChannel) ≠ infer (o with turnChannel held constant)
```
`readsTurns` is R14's `governs` at the perception end — the excursion says so:
*"Anything less is `record_sensitivity_is_not_governance` at the observation
end."* Refusing-broken witness: the current vector (no such channel).
Refusing-plausible-fix witness, named by the excursion: `:operator-turn-count`
— "it recreates the defect exactly — it is `days-since-last-activity` again, a
measure of the operator's *presence*, not of what he said." The accepting
witness does not exist: no tick has ever carried the channel.

**Blocked, and on what:**

```
turnChannel's value function     -- BLOCKED on a design choice, not a noun: what of the turn→pattern
                                  --   association (gen_turn_chain.py's join) normalises to [0,1] as CONTENT
                                  --   (E-R2 slice 2, no status)
generative model of turns        -- BLOCKED: what hidden state does a ✘ / ✓ / 💡 turn bear on? (A's row for it)
```
Unlike R8/R14/R5/R6, nothing here is blocked on Policy, Q(o∣π) or an outcome
space. R2's blocked items are the operator's — which is the design-research
question in its purest form.

## 4. R2's internal wiring as typed deliveries

| edge | from → to | payload | guarantee as built | the undeclared field |
|---|---|---|---|---|
| e1 | `scan-*` → `observe` → `:observation` | 14 channels in [0,1] | per tick; schema drifted 13→14 on 05-18 with no receipt | `payload : Schema` — the docstring still says 13 |
| e2 | `:observation` → `belief update` | per channel | 8 channels consumed; 6 delivered to no consumer (`:n-a-by-design`) | `receipt`: a channel with no likelihood is delivered and dropped, silently — should be typed `None` |
| e3 | operator turn → futon1b evidence store | typed turn (author, mark, text) | **exactly-once, witnessed** (C1: 20/20) | — the one well-typed edge in this node, and the only one whose content the machine cannot produce |
| e4 | evidence store → `gen_turn_chain.py` → figure | turn→pattern edges | live, in the paper | `to`: the paper, not the vector |
| e5 | evidence store → `:observation` | — | **missing edge** — the ring | everything |
| e6 | `:acknowledged?` → `operator-lane/nag?` | Bool | **never set**; nag is a 4-term AND with one input unwired | `from`: no producer — a declared input with no delivery (the promotion test says: repair it *as a repair, under its own name, not as this ring*) |
| e7 | `morning-brief` producer → `:morning-brief-*` keys | event lists | present in 31 records, `[]` in all | `receipt`: present-and-empty ≠ absent ≠ consumed — three states under one `[]` |

## 5. R2's evidence contract — draft for the apex

```
subject      R2-vector                                         R2-operator
claim        every tick's observation has exactly the           the observation vector carries a channel that is a
             declared channels, each with a stated source        function of typed operator turns, and an inference
             and a stated consumer (likelihood or None)          over a ≥111-item window differs when it is held constant
artefact     :observation on the tick record                    a tick record with the turn channel + the same tick's
                                                                 null control (channel constant) — two records
domain       every tick                                          every window ≥ 111 items (promotion-tests.edn:36)
corpus       wm-trace (reader loop)                              wm-trace + futon1b evidence (the turns; C1 script)
method       key-set census; join to channels-with-likelihood   run the null control; diff belief/ranking
falsifier    a tick whose key set ≠ Channel  ← fires (×2, 05-18)  the null control yields the same inference
                                                                 ← cannot be run: no channel exists
not-evidence "13 harmonized channels. Real." (a count, and       :morning-brief-* present-and-empty; turn COUNTS
             the wrong one); a docstring                          (presence, not content); days-since-last-activity;
                                                                 the C1 20/20 (proves storage, not reading)
```

## 6. Glossary → specification: the `Formal:` line each entry would carry

- *Observation vector o*: `Formal: Observation := Channel(14) → [0,1] — definable; the % QUESTION about shared context over time is R2's requirement, unanswered`.
- *Belief state μ*: `Formal: record shape; likelihood : Channel → Option …; the % QUESTION about memory is open`.
- *Observation model A*: `Formal: 7×7 column-normalised matrix — definable; no row for an operator turn`.
- *Generative model*: `Formal: none; its own definition lists "operator feedback" among observations, and no channel carries it`.
- **missing entry** — *Operator turn (as observation)*: `Formal: Turn (marks.clj) definable; turnChannel BLOCKED on design; readsTurns is the requirement (= governs at the perception end)`.

## 7. What this worksheet does not do

It does not design the turn channel — the excursion's slice 2 ("what a
`:turn-pattern-*` channel would carry, and whether the join is the right shape
to normalise") has no status and is the operator's design question. It does
not treat the `:acknowledged?` seam as R2; the promotion test is explicit that
it is a repair under its own name. It records that this is the one ring whose
content the apparatus cannot fabricate, that the storage side of it is the one
edge in the whole machine measured clean, and that the glossary's two
unanswered questions are the requirement written in the author's hand.
