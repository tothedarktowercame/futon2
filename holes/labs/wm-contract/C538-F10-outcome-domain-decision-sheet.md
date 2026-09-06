# C538 — F10 slice 1: the outcome-domain decision sheet for Joe

**Row:** `:F10` slice 1 (`worklist.edn`, `:loop-mode :one-slice-per-invocation`).
**What this is.** The single sheet the row's `:acceptance` asks for: the
outcome-domain candidates, what each makes expressible and what it excludes,
the C_int/C_mis/C_ser family-vs-single-wider-C choice, and the F1 constraint.
**What this is NOT.** No ruling. No write to `aif-equations.edn` `:choices` or
`control-map-edges.edn` `:decisions`. No Lean edit. Nothing here changes a
number. Every claim below carries a `file:line`, a run-record path, or the
literal **not found**; where a pointer another document gave has drifted, the
drift is recorded rather than quietly re-anchored.

Three decisions are asked at the end (§6). One of them (D3) is a conflict
between two records Joe authored on the same day, and it has to be settled
before any of the rest can be typed.

---

## 1. The constraint that prunes the list: KL[Q(o|π) ‖ C] must be computable

**In Lean it is satisfied by construction, and that is the whole of what is
settled.** `PredictiveOutcomeKernel` and `PreferenceDistribution` are the same
type constructor applied to the same outcome carrier:

- `Outcome Obs := Sigma Obs` — `mathlib4/DarkTower/WarMachine/Holes.lean:150`
- `PredictiveOutcomeKernel PolicyIndex Obs := ProbabilityKernel PolicyIndex
  (Outcome Obs)` — `Holes.lean:6759-6760`
- `PreferenceDistribution Obs := ProbabilityKernel Unit (Outcome Obs)` —
  `Holes.lean:6778-6780`

So whatever `Obs : Vertex → Type` is declared, the KL is well-typed. The
declaration of `Obs` is therefore the entire decision; nothing downstream in
Lean constrains it further.

**In the runtime it is not satisfied, and the two sides are over disjoint
alphabets.** This is the fact that prunes hardest:

| object | alphabet | where |
|---|---|---|
| the Q(o\|π) F1 slice 2 measured | the **7 M-INC `state/*` events** — `:spawned :refined :strengthened :addressed :falsified :foreclosed :reopened` | `src/futon2/aif/belief.clj:44`; the A it reads, `belief.clj:199-206`; receipt `runs/F1-machine-q/03-machine-grain-q.edn` |
| the C the risk term consumes | **13 channel `[lo hi]` ranges** (`:loop-health`, `:mission-health`, `:sorry-count-norm`, …) | `src/futon2/aif/preferences.clj:9-24`, read live through `current-C` at `preferences.clj:60-70` |

No outcome is in both. The risk the machine actually computes is
`Σ_ch w_ch · KL(N(μ_ch,σ²_ch) ‖ C_ch)` (`src/futon2/aif/efe.clj:644-645`), and
its first argument is the per-channel Gaussian `forward-model/predict` emits —
which `src/futon2/aif/machine_q.clj:1-14` **explicitly refuses** to accept as
`Q(o|π)`, with a typed reason, alongside `Q(π)` and the two-point binary-latent
map. So: today the machine computes no KL[Q(o|π) ‖ C] anywhere. Every candidate
in §2 is a proposal to create one, and none of them can be discharged by
pointing at code that already runs.

**The observation that is not on the candidate list.** The only alphabet a real
Q ranges over today is the 7 `state/*` events. It appears in none of the five
candidates the row names. Choosing any candidate below therefore commits to
**building a new Q over the chosen domain**, not to wiring an existing one — and
F1's own finding says what that costs: the rows coincided across two genuinely
different cascades because `transition-model-v1` is a 7×7 identity with no
action index (`belief.clj:216-231`), so the missing piece is B's dependence on
u, not π's reach (`C516-F1-machine-grain-q.md` §2, arms 1–3).

---

## 2. The five candidate domains

### A — belly goal-states (C533)

**The object.** `c-entry` records over the formed goal/hole corpus:
`{:flavour :outcome-ref :preferred :weight :status :provenance}`, refusing
construction without flavour, outcome reference, preference and provenance
(`src/futon2/aif/c_vector.clj:48-56`).

**Expressible.** Preference over progress on goals and holes the machine has
already formed, each entry carrying its own weight *with its basis* and its
provenance — the only candidate with per-outcome auditable weight built in.

**Excluded.** Everything before a goal exists (`C537-serendipity-shapes-C.md`
§2); anything not in the goal/hole corpus.

**Measured state, and it is worse than "dark".** Only the `:stated` channel is
constructed in this repository (`c_vector.clj:152,164`); the other four
flavours arrive as **overlay EDN produced in a different repository**
(`c_vector.clj:19-20,213-216`), read from `/home/joe/code/futon6/data/c-vector`
(`c_vector.clj:218-223`). Of the three overlay files that path names
(`c_vector.clj:222-223`), `c-entries.mess.edn` (44 entries) and
`c-entries.incomplete.edn` (81 entries) exist and were last written
**2026-06-26**; `c-entries.yingvoice.edn` is **not found**. On top of that,
`ensure-belly-fresh!` has no caller and the 2026-09-04 trace carries
`:c-entries []` with the provenance still declared (`C533-belly-node-history.md`
§4). So candidate A's domain is, today, 125 entries 72 days stale in another
repo, one third of it missing, reaching the scorer as empty.

**F1 status.** No Q over goal-states exists. The seam that would build one is
named and un-removable by Joe's instruction, and it says what it waits on: the
forward model predicting goal progress under π, i.e. the goals↔methods proof
join (`c_vector.clj:23-27`).

### B — mission observation closure

**The object.** A mission's declared completion criteria, each bound to an
observable declared `:binary`. Reader: `mission-c/read-criteria`
(`src/futon2/aif/mission_c.clj:377`), builder `c-mis` (`mission_c.clj:422`),
composition `log-c-mis` (`mission_c.clj:500`), term `risk-mis`
(`mission_c.clj:558`). The gauges that bind prose criteria to observables are
declared once, in one place, with a pointer back to themselves
(`scripts/futon2/report/war_machine.clj:2065-2095`).

**Expressible.** Pragmatic preference at mission grain — and it is the only
candidate that is implemented end to end *and* already carries a ruling: J6,
`:declared-binarization` (recorded at `aif-equations.edn` `:choices` `:c-grain`
`:outcome-semantics-ruled`).

**Excluded.** Anything no mission document writes down. The domain is exactly as
wide as the missions declare, and that is narrow: M-zaif-harness-v1 declares
**three** criteria (`holes/missions/M-zaif-harness-v1.md:86-94`), and
M-expressions-of-interest is six `:no-producer` rows binding **nothing**
(`war_machine.clj:2082-2086`).

**F1 status.** No Q(o|π) over criteria. The v0 term scores the **status quo for
every candidate** — the per-criterion surprisal at the current measured value,
hence identical for every policy (`mission_c.clj:558` and the registry's own
`:v0-forward-model` note). U12 measured that this does not discriminate: on each
clocked tick 133 mission actions carried one distinct `risk_mis`,
4.539889921682063E-5.

### C — typed-absence vocabulary

**The object.** The reasons an outcome could not be read:
`:no-outcome-observed`, `:uninterpretable-outcome`,
`:non-binary-value-on-binary-observable`, `:no-declared-threshold`,
`:spec-observable-mismatch`, `:undeclared-observable-kind`
(`src/futon2/aif/preferences.clj:360-408`).

**Expressible.** Preference over the machine's own **measurability** — a C that
prefers being in states where its criteria are readable at all.

**Excluded.** The world. This is a preference over the instrument, not over the
outcome, and adopting it as *the* domain would make the reading gate itself the
thing scored: the cheapest way to lower risk becomes declaring more gauges
rather than changing anything. Stated as the objection, not as a ruling.

**F1 status.** These values are already emitted per call, so a Q over them is
the most buildable of the five — which is precisely why it is worth naming the
objection rather than letting buildability decide.

### D — grounded-change events (terminal flight dispositions)

**The object.** The full loop's terminal dispositions — `:ok`,
`:grounded-change`, `:grounded-no-change`, `:no-selection`, `:artifact-only`,
`:build-failed`, `:guardrail-refusal`, `:policy-nondiscrimination`, … —
fourteen of them (`holes/NOTE-pattern-as-production-rule-and-Q.md:174-178`;
in code, e.g. `src/futon2/aif/full_loop_runner.clj:1738`, `:2613`, and the
failure-kind classification at `full_loop_runner.clj:2101-2113`).

**Expressible.** This is the only candidate with a **measured
policy-conditional distribution**: over 82 closed attempts,
P(`:grounded-change` | `:repair-machine-failure`) ≈ 11/28 = 0.39,
| `:advance-mission` ≈ 3/8 = 0.38, | `:learn-action-class` = 3/25 = 0.12
(`NOTE-pattern-as-production-rule-and-Q.md:181-183`). A Q(o|π) with
demonstrated discrimination, which no other candidate has.

**It is also the one P-validated-R5 already assigned a vertex to**: "the machine
acts at the organisations vertex (missions advanced, code built — the fourteen
flight dispositions are `Obs organisations`)"
(`holes/problems/P-validated-R5.md:128-131`).

**Excluded, and the bounds are already on the record** (same note, `:188-192`):
21% of flights have no recoverable π (mostly `:agent-unavailable`/`:incomplete`,
failing before selection — missing-not-at-random, not droppable silently);
`:address-sorry` is n=2, not a rate; **an outcome is not a reward, so a C over
these is a further stateable choice**; and `:ok` is a step outcome, not a
terminal disposition. Also excluded by grain: nothing finer than a whole flight,
so no tick-level preference.

### E — question-formation events (C537)

**The object.** Pre-goal interestingness: question-identified,
anomaly-accommodated, interesting-potential observations
(`C537-serendipity-shapes-C.md:44-58`), vocabulary from arXiv:1411.0440's six
phases.

**Expressible.** Serendipity potential as preference mass — the paper's design
claim that serendipity potential can be raised architecturally, which a C typed
only over goal-advancement makes **type-level impossible** (`C537:50-58`).

**Excluded / producer status: not found.** `grep -rn 'question-formation|
question-identified|anomaly-accommodated' src/ scripts/` over futon2 returns
nothing. C537 §5 records the same gap as an open R2 question — whether these are
observable on today's channels at all.

**F1 status.** No Q, no C, no producer. This candidate is a reservation, not a
domain — which is a reason to keep a **named empty slice** for it rather than a
reason to drop it (see §5).

---

## 3. Lean's two C surfaces disagree about the domain

Not previously written down anywhere I could find, and it changes what "family
vs single wider C" even means:

- `def C {Obs} (v : Vertex) (_pragmatic : v ≠ Vertex.evidence) : Obs v → ℝ`
  (`Holes.lean:151-152`) — the machine's own C, a **per-vertex** function that
  **excludes the evidence vertex by an explicit hypothesis**, and is `sorry`,
  marked DELIBERATE IMPLEMENTATION REFUSAL.
- `PreferenceDistribution Obs := ProbabilityKernel Unit (Outcome Obs)`
  (`Holes.lean:6778-6780`) — the carrier, a **single** distribution over the
  **whole tagged sum**, and `Vertex` has four constructors including `evidence`
  (`Holes.lean:142-147`).

So one surface says C is a family indexed by pragmatic vertices; the other says C
is one distribution over everything including evidence. **The disagreement is not
a type error, and that matters for the choice:** `ProbabilityKernel` carries
`support : S → List O` with `normalised` summing over that support
(`Holes.lean:6751-6755`), so a single wider C can carry **no evidence-tagged
outcomes in its support** and satisfy §2a′'s exclusion without any type change.
The exclusion is expressible as a support fact. That makes arm 2 in §4 cheaper
than it looks.

Relatedly: `machineHasNoC` (`Holes.lean:7117-7120`) proves the free-preference
list is non-empty, and its own docstring records that the historical claim ("C is
a parameter everywhere") was **refuted** by the in-language census — the free
constant is exactly the vertex-local `C` at `:151-152`. The hole is that one
declaration, not the carrier.

---

## 4. The family-vs-single-wider-C choice, as three arms

**Arm 1 — a family, no join.** C_int, C_mis, C_ser stay three objects over three
domains, each with its own risk term, summed into G.
*Buys:* nothing has to be rewritten; C_int keeps running (`preferences.clj:9-24`,
`:60-70`) and C_mis keeps its J6 ruling.
*Costs:* the sum of three KLs against three unrelated C's is not KL[Q(o|π) ‖ C]
for any C — it is the shape the runtime already has, and the shape §1 shows is
not the required object. It also leaves `Holes.lean:6778-6780` uninhabited
forever, because the carrier is a *single* kernel.

**Arm 2 — one C over the tagged sum; the family becomes a support partition.**
Declare `Obs v` per vertex; C is the one `PreferenceDistribution`; C_mis and
C_ser are names for regions of its support, not separate objects.
*Buys:* inhabits the Lean carrier as written; makes KL[Q(o|π) ‖ C] the literal
computation; the evidence-vertex exclusion is a support fact (§3), not a type
fight; C537's serendipity slice can be **named and empty** so the type does not
exclude it before a producer exists.
*Costs:* C_int does not fit — the 13 channels are proprioception, not an outcome
at any of the four vertices, so arm 2 requires saying explicitly that C_int is a
*different object at a different grain* that stays outside the sum. It also
requires a Q over the chosen domain, which does not exist (§1).

**Arm 3 — one domain only, everything else typed-absent.** Pick one candidate,
declare the rest `:absent` with a reason from `preferences.clj:360-408`.
*Buys:* smallest declaration; every absence is on the record.
*Costs:* whichever is picked, the excluded kinds are excluded **by the type**, and
C537's argument is that doing this to pre-goal outcomes forecloses serendipity
architecturally.

---

## 5. Recommendation (a recommendation, not a ruling)

**Arm 2, with the domain taken from P-validated-R5 §2a rather than freshly
invented, because §2a already answers this question and the five candidates are
slices of its answer.** Concretely:

1. `Obs organisations` := the **terminal flight dispositions** (candidate D).
   Reason: it is the only candidate with a measured policy-conditional
   distribution (`NOTE-pattern-as-production-rule-and-Q.md:181-183`), and
   `P-validated-R5.md:128-131` already assigns exactly these to this vertex. The
   mission criteria (B) and the goal-states (A) are further slices of the *same*
   vertex, added when their producers are honest — B's is (`mission_c.clj:377`,
   `war_machine.clj:2065-2095`), A's is stale and cross-repo (§2A).
2. `Obs evidence` := certification/update records, **valued by EIG with no C**,
   per `P-validated-R5.md:187-194`.
3. **C_int stays outside the sum**, declared as what it is: a preference over the
   machine's own health channels at tick grain, not an outcome at a vertex. Say
   so in the declaration rather than letting the two be conflated — the recorded
   S1 refusal is exactly "a C over channels offered as a C over outcomes"
   (`P-validated-R5.md:58`).
4. **C_ser is declared as a named, currently empty region of the support** — so
   the type does not exclude pre-goal outcomes (C537's design claim) while the
   producer honestly reads **not found**.
5. Candidate C (typed-absence vocabulary) is **not** part of the domain; it stays
   what it already is, the typed reason a missing outcome reads as
   (`preferences.clj:360-408`).

**What this recommendation does not solve:** the Q. Arm 2 makes the KL well-typed
and still leaves `Q(o|π)` over flight dispositions to be built, with F1's finding
(`C516-F1-machine-grain-q.md` §2) saying the blocking piece is B's dependence on
u (`belief.clj:216-231`), not the policy index.

---

## 6. What Joe is asked to rule

**D1 — the domain.** Which `Obs v` declarations? (Recommendation: §5.1–5.2.)

**D2 — family or one C.** Arm 1, 2 or 3 (§4)? A ruling here also settles which of
the two disagreeing Lean surfaces (§3) is the one the model keeps.

**D3 — where serendipity value lives, and this one blocks the others.** Two
records Joe authored conflict:

- `C537-serendipity-shapes-C.md:38-48` — serendipity potential is
  **preference-side**: "C putting mass on being-in-interesting-territory",
  explicitly *not* EIG, because EIG rewards information gain under the current
  model.
- `P-validated-R5.md:187-193` (§2a′) — the evidence vertex carries **no C**:
  "Putting a preference on 'having learned' would be the move that turns learning
  into another pragmatic term, which is one more way to be greedy."

A possible reconciliation, offered as a candidate reading rather than a ruling:
the two agree if C_ser's preferred object is the **occasion** (an
observation-state at the organisations vertex — an anomaly is on the board) and
not the **update** (an evidence-vertex outcome — having learned). If that
distinction is the intended one it has to be declared, because no producer
exists on either reading (§2E) and the type follows the declaration.

---

## Scope limits

Checked: `mathlib4/DarkTower/WarMachine/Holes.lean` at the four pointer ranges
quoted (all four resolve); `src/futon2/aif/` (`preferences.clj`, `belief.clj`,
`efe.clj`, `mission_c.clj`, `machine_q.clj`, `c_vector.clj`,
`full_loop_runner.clj`); `scripts/futon2/report/war_machine.clj`;
`holes/problems/P-validated-R5.md`; `holes/NOTE-pattern-as-production-rule-and-Q.md`;
`aif-equations.edn` `:choices` `:c-grain`; the futon6 overlay directory listing.
Not checked: no live JVM was queried or reloaded; no run was executed; the 82
closed attempts and the three conditional rates are quoted from
`NOTE-pattern-as-production-rule-and-Q.md:181-183` and were **not** recomputed
here; `futon6/scripts/c_vector.bb` was read only for the flavour constructors
(`:49-52`, `:89`, `:95`, `:145`, `:162-164`), not audited.

Pointer drift found and recorded, not silently repaired: `C533` cites
`c_vector.clj:45-56` for `c-entry`, which is at `c_vector.clj:48-56` today;
`aif-equations.edn` `:c-grain` `:evidence` cites `mission_c.clj:246-268`,
`:270-315`, `:317-336`, `:338-376` for `read-criteria`, `c-mis`, `log-c-mis` and
`risk-mis`, whose `defn` lines are now `377`, `422`, `500` and `558`. The
registry already warns that a 2026-09-02 commit moved this file and that
re-anchoring text a signature covers is not done silently; this sheet cites the
current lines and leaves the registry alone.
