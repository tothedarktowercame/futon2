# REFUSAL-REGISTER-D — the refusals a flight of M-autoclock-in can reach at HEAD

Date: 2026-09-25. Author: claude-3. Read-only: no code change, no click, no
flight, no write under `data/`, no shared-JVM load. Nothing here proposes a
removal; the mission's owner (claude-10) decides per row.

Read at futon2 `4c0fedb4` (2026-09-25 15:55:37Z) and futon3c `a9eb3ee7`.
Serves M-wm-wiring IDENTIFY criterion 4(a) (futon3c
`holes/missions/M-wm-wiring.md:72`): "the refusals the flight can reach are
enumerated in this mission before the flight, each with its case; a refusal
reached that is not on the list is a defect of the wiring, not of the target."

## 0. What was traced, and what counts

The path: `flight/start` → `run!` (`src/futon2/aif/flight.clj:246-297`), whose
steps are `read-fn` → `click-wants` → the owner-question check → `observe-fn` →
`ask-fn` → `click-fn` → `observe-fn` → `record-click`. The adapters are
`src/futon2/aif/flight_runner.clj`; through them the trace enters
`mission_criteria`, `mission_reading`, `want_interpretation`,
`observation_checks`, `interpretation_construction`, and — through the click —
`cascade_problems`, `observation_rates`, `efe` and the war-machine lane.

**A refusal** is a return or throw that stops the step, the flight or the tick
rather than recording a typed absence and continuing. Three classes turned up,
and the third matters for reading the counts:

- **stops the flight** — an uncaught `ex-info` on the flight's own path;
- **caught by the flight** — a callee throws, the flight catches it and records
  a `:need` (`flight_runner.clj:119-128`); the flight continues;
- **stops the tick** — the click's own refusal; `click-summary`
  (`flight_runner.clj:30-45`) and `record-summary` (`:238-253`) record it as the
  click's `:abstention` and the flight continues to the next click.

## 1. Refusals, with their cases

All eleven `throw (ex-info …)` sites on the flight's own namespaces are listed
(§5 reconciles the count), plus the tick-level refusals a first click reaches.

| # | kind | site (futon2 4c0fedb4) | trigger | stops | case |
|---|---|---|---|---|---|
| 1 | `:want/declared-constraint-not-in-text` | `flight_runner.clj:196` in `constraints-for`, called from `ask-fn:218` | a caller's declared `:constraints` names an edge the mission text does not state | flight, before the click | PROOF-2a draft `:671`: "the declared carrier is dropped and a declared edge is accepted only if the text states the same one, else `:want/declared-constraint-not-in-text`" — a hand declaration cannot add to the mission's own words |
| 2 | `:want/not-validated` | `want_interpretation.clj:412` in `publish!` | `publish!` called with a result whose `:status` is not `:valid` | flight | guard on the publication rule of draft `:671`: "a validated response to a machine-issued request publishes without an operator step; the click remains the only gate". Not reachable from `settle`/`revalidate` (`flight_runner.clj:150,165`), which publish only on `:valid` |
| 3 | `:want/request-not-issued` | `want_interpretation.clj:418` in `publish!` | the request id is not recorded under `data/wm-interpretations/requests/`, or its target/want differ from the validated result | flight | draft `:671`: "hardened at ddb81953 … `publish!` refuses `:want/request-not-issued` for an unrecorded request id or one whose target/want differ from the validated result". This binding is what lets publication skip an operator step |
| 4 | `:want/conflicting-publication` | `want_interpretation.clj:430` in `publish!` | a different reading of a pattern already published for that target | flight | draft `:671`: "a different reading of an already-published pattern refuses `:want/conflicting-publication`" — one pattern, one published reading |
| 5 | `:reading/request-not-issued` | `mission_reading.clj:221` in `bound!`, called by all four reading publishers | same as 3, for a reading request | flight (the read step) | the same binding as 3; **no reading-specific case found** in the rulings or the AR register |
| 6 | `:reading/not-validated` | `mission_reading.clj:229` `publish-locator!` | publish called without a `:valid` reading | flight | guard, as 2. Not reachable from `read-one` (`flight_runner.clj:319-324`), which publishes only on `:valid` (or `:questions` to the questions publisher) |
| 7 | `:reading/not-validated` | `mission_reading.clj:248` `publish-criteria!` | as 6 | flight | as 6 |
| 8 | `:reading/not-validated` | `mission_reading.clj:336` `publish-constraints!` | as 6 | flight | as 6 |
| 9 | `:reading/not-validated` | `mission_reading.clj:396` `publish-coverage!` | as 6 | flight | as 6 |
| 10 | `:want/criterion-absent` | `want_interpretation.clj:54` (the `refuse` throw at `:50`) in `citation-for`, reached from `request!` | the criterion's stated text no longer occurs in the mission text | **caught**: `issue-request` (`flight_runner.clj:127`) turns it into outcome `:request-refused`, a `:need`; the flight continues | draft `:671`: "`citation-for` locates the criterion by its stated text, one occurrence cited at its current lines with `:relocated-from`, none refuses `:want/criterion-absent` (the criterion itself altered)"; warrant test-registry-a026f436 at 4e77df7c |
| 11 | `:want/criterion-ambiguous` | `want_interpretation.clj:61`, same `refuse` | the stated text occurs more than once | **caught**, as 10 | draft `:671`: "more than one refuses `:want/criterion-ambiguous`"; the site's own reason (`want_interpretation.clj:43-45`): "the want token is keyed on that text, so two occurrences cannot say which was meant" |
| 12 | `:constructor/refusal :nonfinite-g` | `interpretation_construction.clj:210` in `construct` | a candidate's G is not finite | tick | **no case found** in the rulings or the AR register; the site states the requirement only ("Constructor needs a finite G comparison") |
| 13 | `:universe-not-admitted` | `cascade_problems.clj:198` (`refusal`, `:49-51`) | the target has tokens with no checkable locator | tick | PROOF-2a draft `:597`: exclusions are only non-targets — but this is assembly's, not the field's; **no case found** for the assembly refusal itself |
| 14 | `:no-admitted-interpretation` | `cascade_problems.clj:201,206` | no admitted interpretation for a pattern the problem needs | tick | as 13: **no case found** |
| 15 | `:no-constructed-candidate` | `cascade_problems.clj:222,230,231` | no constructed cascade advances an open want | tick | draft `:671` records it as the masking bad case ("the masking bad case deletes the producing interpretation and asserts `:no-constructed-candidate` at assemble's boundary") — a case for the *carrier*, not for refusing |
| 16 | `:unknown-class` | `observation_rates.clj:186` | a locator's class is not in the observation contract | tick (stops the lane at R5) | the contract is the S-1 declaration; **no case found** beyond it |
| 17 | `:unsupported-class` (with `:cell-reason :declared-not-measured` where it applies) | `observation_rates.clj:211` | a judgement class with no admitted rate, a class measured on one cell only, or a rate its own counts do not produce | tick (R5) | A-S §5 (`proof2/packets/A-S.md`): "a rate table with no counts, or with declared numbers wearing the shape of measured ones, is not measured and must be refused"; landed 64f005d4 |
| 18 | `:invalid-adjudication-rates` | `efe.clj:1118` | the rates map does not cover every token of the scored universe | tick (R5) | `efe.clj:1105-1112`: "never a silent projection onto zero" — refusing beats padding the kernel |

## 2. Typed absences — recorded, and the flight continues

These are not refusals. They are listed so the two classes can be read side by
side, as criterion 4(a) asks.

| kind | site (futon2 4c0fedb4) |
|---|---|
| a refused check reads `:unknown`, never true or false | `flight_runner.clj:61-70`, over `observation_checks/refuse` (`:28`) and the `:refused` map (`:507-510`) |
| `:no-criterion`, `:request-refused`, `:not-answered`, `:unparseable-response`, `:declined`, `:rejected` — each a flight `:need` with its job id | `flight_runner.clj:171-182`, `:230-233` (docstring `:212-214`) |
| the read step's `:not-answered`, `:unparseable-response`, `:declined`, `:rejected`, `:questions` — each a `:need` | `flight_runner.clj:315-324`, `:411-419` |
| `:owner-question` needs, addressed to the mission's owner | `flight_runner.clj:396-410`, `:416-419` |
| `:readings-needed` (`:criteria?`, `:coverage?`, `:locators`, `:constraints?`) — what the source still needs, asked before the click | `flight.clj:95-105` |
| `:criteria-from :none` — no criteria in any recognised form | `flight.clj:89` |
| `:locator-declined` — a seat declined to give a locator, recorded per text and not asked again | `flight.clj:91-93`, `flight_runner.clj:375-378` |
| `:out-of-view` — data-only phases, retained findings, scope-outs, owner-questioned tokens | `flight.clj:136-138` |
| `:click-not-started`, `:run-record-missing`, `:target-not-in-refusals` — a click the server did not start, a missing record, a target absent from the abstention carrier | `flight_runner.clj:289`, `:253`, `:38` |
| `{:absent :no-instances-anchor}` — the served-by half on a mission with no `## … instances` heading | not at HEAD: `src/futon2/aif/served_by_reading.clj` does not exist (M-wm-wiring row 2 calls it the first packet) |

Flight-ending statuses, which are also not refusals: `:closed` and
`:no-progress` (`flight.clj:218-220`), `:click-limit` (`:264`),
`:not-a-target-yet` (`:280`). The last has Joe's words on it in the code
(`flight.clj:277-279`): "good questions logged, not a refusal and not bad work
against a vague specification".

## 3. The M-autoclock-in walk

Read, not run. Field entry at `test/fixtures/target-field/target-field@futon2-7bd17dfb.edn`:
`{:target "M-autoclock-in" :kind :mission :next-step :read-criteria :finding
{:kind :criteria-not-stated}}`; text futon3c `holes/missions/M-autoclock-in.md`,
Status line `INSTANTIATE-1 …`, 15 phase headings, no `**Exit criterion:**`
paragraph.

1. **`read-fn` first** (`flight.clj:270`). `:criteria?` is true (the mission
   states none in the reader's form), so a criteria request is issued and a seat
   is asked. Every outcome other than publication is a `:need`; none stops the
   flight.
2. **Coverage is skipped.** `:coverage?` requires `(seq (criteria/criteria
   target text))` (`flight.clj:98`) — the text has none, so no coverage request.
3. **Locators**, one per criterion with no stated verdict, after any criteria
   publication (`flight_runner.clj:363-373`). `:questions` here become owner
   questions; a `:declined` is recorded for this text and not asked again.
4. **Constraints**, once per text (`flight.clj:102`).
5. **The served-by half records `{:absent :no-instances-anchor}`** — an absence,
   not a refusal — and at HEAD it is not reached at all, the namespace being
   absent.
6. **`click-wants`, then the owner-question check** (`flight.clj:274-280`). If
   the criteria reading published nothing and the locator readings raised
   questions, the wants are empty and the flight ends `:not-a-target-yet`
   with the questions on its record.
7. **`observe-fn`**: each want's locator; a refused check is `:unknown`.
8. **`ask-fn`**: `constraints-for` runs first (refusal 1 — only if a caller
   declared constraints; the flight does not), then a request per want no
   interpretation produces. `:want/criterion-absent` and
   `:want/criterion-ambiguous` are caught here (10, 11).
9. **The click**, and with it refusals 12-18 at tick level, recorded as the
   click's abstention.

**Which refusal stops it first:** none of 1-11 is on the path a first flight of
M-autoclock-in would take. Refusals 2 and 6-9 are guards the callers cannot
trigger; 3, 4 and 5 need a store that lost or contradicts a request the machine
itself wrote; 1 needs a declared constraint no caller passes; 10 and 11 are
caught and recorded. **The first thing that would stop this flight is not a
refusal**: either `:not-a-target-yet` at step 6, or `:no-progress` after a
first click that advances no want. That is the answer criterion 4(a) wants on
record before the flight: on this target the read-and-ask path is refusal-free,
and every gap it meets is recorded and carried.

## 4. Counts

- **Refusals listed: 18** (11 `throw` sites in the flight's own namespaces,
  counting `citation-for`'s two kinds separately, plus 7 tick-level).
- **With a case: 12** (1, 2, 3, 4, 6, 7, 8, 9, 10, 11, 17, 18; 6-9 share one
  guard case). **Without: 6** — 5 (`:reading/request-not-issued`), 12
  (`:nonfinite-g`), 13 (`:universe-not-admitted`), 14
  (`:no-admitted-interpretation`), 15 (`:no-constructed-candidate`: the case on
  record is for recording the carrier, not for refusing), 16
  (`:unknown-class`).
- **By level:** stops the flight 9 sites (kinds 1-9); caught and recorded 2
  (10, 11); stops the tick 7 (12-18).
- **Typed absences: 10 classes** (§2), plus 4 flight-ending statuses.

## 5. Falsifiers

A reader shows this list incomplete by finding a stopping site it does not
account for. The greps, run at `4c0fedb4` from `/home/joe/code/futon2`:

```sh
# (a) every throw on the flight's own namespaces — must equal 11
P="src/futon2/aif/flight.clj src/futon2/aif/flight_runner.clj \
   src/futon2/aif/mission_criteria.clj src/futon2/aif/mission_reading.clj \
   src/futon2/aif/want_interpretation.clj src/futon2/aif/observation_checks.clj \
   src/futon2/aif/interpretation_construction.clj"
grep -h 'throw (ex-info' $P | wc -l        # 11; rows 1-12 of §1

# (b) every typed refusal keyword on those namespaces
grep -ho ':interpretation/refusal :[a-z/-]*' $P | sort -u | wc -l   # 6 kinds
grep -ho ':constructor/refusal :[a-z-]*' $P | sort -u               # :nonfinite-g

# (c) the typed-absence returns that must NOT be on the refusal list
grep -c ':status :missing' $P    # observation_checks 2, every other file 0
```

Counts that do not reconcile, and what they would mean:

- (a) > 11: a throw this register missed. Each new site is a row or a defect.
- (b) naming a kind not in §1: the same.
- A refusal reached at flight time whose kind is in §2: the classification is
  wrong, because a typed absence stopped something.
- The tick's surface is **not** exhaustively enumerated here: rows 12-18 are the
  refusals on the R1→R14 path a first click reaches, not every refusal the
  war-machine lane can raise. `grep -rc 'throw (ex-info' src/futon2/aif/*.clj |
  awk -F: '{s+=$2} END {print s}'` counts the repository-wide total; the
  difference is the unenumerated remainder, and a flight that reaches one of
  those is not thereby a wiring defect under 4(a) until this register is
  extended to cover the tick.

---

# D2 — the tick's surface (appended 2026-09-25)

Read at futon2 `3bbf5059`; same bar, read-only, nothing proposed for removal.
Extends §1 at the owner's request (claude-10, ~19:10Z, recorded at futon3c
`holes/missions/M-wm-wiring.md:72`), because M-autoclock-in reaches a click as
soon as one criterion publishes. §1-§5 stand, two corrections in §D2.5.

## D2.1 The owner's decisions on D's six

| D row | kind | decision |
|---|---|---|
| 5 | `:reading/request-not-issued` | KEEP — the row-3 issued-request binding (draft `:671`); a reading publishes without an operator step only because it answers a request the machine issued |
| 13 | `:universe-not-admitted` | KEEP — dropping unlocated tokens would change the universe G is taken over (W6 comparability); the read step's locator request is the route out |
| 14 | `:no-admitted-interpretation` | KEEP — P₀/W₀: a candidate's G cannot be recomputed without its patterns' interpretations and receipts |
| 15 | `:no-constructed-candidate` | KEEP — W₀ requires machine-constructed candidates; selecting a declared hand candidate after the constructor refused would substitute one for the other |
| 16 | `:unknown-class` | KEEP — H-A `7ba427ab`: the zero kernel is the unmeasured default only for a class in the contract; zero for a class outside it is absence read as value |
| 12 | `:nonfinite-g` | CHANGES — becomes a typed absence per candidate, that candidate left out of the comparison; packet NONFINITE-G-I (claude-13, author ≠ reviewer) |

## D2.2 The tick's refusals, by stage

Class: **T** stops the tick (the abstention carrier records it, the flight's
click-summary reads it: `full_loop_runner.clj:515-521`); **C** caught and
recorded; **F** stops the flight.

| stage | kind(s) | site (futon2 3bbf5059) | trigger | class | case |
|---|---|---|---|---|---|
| assembly | `:universe-not-admitted` | `cascade_problems.clj:198` (universes), `:218` (locators) | no admitted fact universe; a token with no checkable locator | T | D2.1 row 13 |
| assembly | `:no-admitted-interpretation` | `:201`, `:206` | no admitted interpretation, or a candidate pattern without one | T | D2.1 row 14 |
| assembly | `:want-not-declared` | `:211` | no want for the target | T | **no case found**; the ns states the rule only (`:24`) |
| assembly | `:no-constructed-candidate` | `:222`, `:230`, `:231` | no non-empty precedence carrying a construction receipt | T | D2.1 row 15 |
| assembly | `:beta-not-declared` | `:234` | no declared β for the target's context | T | **no case found**; β has no default by `policy.clj:92`'s refusal, which is its consequence |
| assembly | `:horizon-not-declared` | `cascade_problems.clj:30,274` | `:horizon-steps` absent — refuses ALL targets, not one | T | P7's common horizon: candidates compared at one T |
| constructor | `:nonfinite-g` | `interpretation_construction.clj:210`, **caught at `:222-223`** and returned `{:status :refused :kind :nonfinite-g}` | a candidate's G is not finite | C→T | changing, D2.1 row 12 |
| constructor | `:construction-not-taken` | `interpretation_construction.clj:228` | the receipt records no moves | T | the site: the empty cascade alone is never "constructed" (`cascade_problems.clj:24-26`) |
| scoring | `:missing-common-horizon`, `:missing-cascade-belief`, `:missing-cascade-want` | `efe.clj:1060`, `:1064`, `:1067` | a scoring input absent | T | the inputs G is defined over; typed `:status :missing` returns, rethrown at the judge boundary (below) |
| scoring | `:invalid-adjudication-rates` | `efe.clj:1117` | the rates map does not cover the scored universe | T | `efe.clj:1105-1112`: "never a silent projection onto zero" |
| scoring | `:mixed-candidate-kinds` | `efe.clj:1390` | a family mixing candidate kinds | T | **no case found** |
| scoring | `:unknown-class`, `:unsupported-class` | `observation_rates.clj:186`, `:211` | class outside the contract; no usable measured pair | T | D2.1 row 16; A-S §5 |
| selection | `:nonpositive-temperature`, `:nonfinite-temperature` | `policy.clj:92` | β not a positive finite number | T | a posterior needs a valid temperature; no default β (see `:beta-not-declared`) |
| selection | `:f-pi-scaling`, `:f-pi-values` alignment, `:f-pi` non-numeric | `policy.clj:99`, `:103`, `:108` | F_π inputs malformed when the F_π posterior is on | T | **no case found** |
| selection | `:invalid-policy-prefix` | `policy.clj:229` | a candidate's prefix is not a policy prefix | T | **no case found** |
| selection | `:precision-consumption-mismatch` | `policy.clj:371` | the precision consumed differs from the one recorded | T | **no case found** |
| selection | `:no-acting-cascade-candidate` | `policy.clj:393` | no candidate has an acting first pattern | T | an all-empty family has no action to enact |
| selection | `:invalid-temperature`, `:invalid-habit`, `:invalid-free-energy`, `:no-admissible-candidate`, `:unmapped-candidate` | `cascade_selection.clj:75,78,91,106,150,154` (via `refuse!` `:36`) | the posterior's own inputs, and a family with no admissible candidate | T | the selection law's inputs must be the ones recorded; **no ruling found** |
| gate | `:inadmissible-decision` with 14 reasons: `:missing-observation-locators` `:beta-not-recorded` `:posterior-over-non-cascade` `:missing-construction-receipt` `:missing-interpretation-receipts` `:empty-interpretation-receipts` `:ticket-queue-certificate-mismatch` `:ticket-queue-candidates-mismatch` `:ticket-queue-choice-invalid` `:missing-recorded-posterior` `:posterior-not-normalised` `:chosen-action-not-a-candidate` `:chosen-action-is-not-an-action` `:no-acting-candidate` | `decision_gate.clj:96,123,131,133,136,140,164,168,196,204,209,211,229,233` (via `refuse!` `:58`) | the emitted decision does not recompute from its own record | T | the ns `:1-14`: the gate is the single place a decision is emitted and recomputes the marginal from the decision's own posterior, "never trusted from `:softmax-weights` or `:chosen-action-mass`" |
| gate | `:invalid-controller-authorization`, reasons `:target-not-open` `:action-not-admissible` `:controller-score-missing-or-invalid` `:selection-law-missing-or-invalid` | `controller_authority.clj:43`, reasons `:30-41` | the action is not enactable, or its score/law is not on the record | T | the ns `:1`: "Machine authorization for the actual controller decision; no fixture recall" |
| judge | `:incommensurable-family` | `war_machine.clj:6224`, `:6227` | candidates with incompatible preference schedules or scales | T | comparability: a family scored under different C is not one comparison (the `a38becc9` schedule fix) |
| judge | `:live-c-refused`, `:live-c-stale` | `war_machine.clj:6288`, `:6310` | C unavailable or older than its source | T | C must be the one this tick's sources give; a grain mismatch (`:no-reachable-want`) is deliberately NOT refused (`:6422`) |
| judge | rethrow boundary — carries the callee's kind | `war_machine.clj:6424`, `:6611` | a live-C refusal, or `ranked` carrying a `:status` | T | this is where efe's typed `:status :missing` returns become the tick's refusal |
| judge | belief accumulation: `:accumulation-configuration-invalid` `:accumulation-identity-missing` `:single-entity-belief-missing` `:accumulation-migration-required` `:accumulation-initialization-required` + one generic | `war_machine.clj:111`, `:1529`, `:1533`, `:1537`, `:1541`, `:1553` | the tick's belief accumulation cannot be identified, migrated or initialised | T | **no case found** |
| judge | three precondition gates | `war_machine.clj:352` (F_π posterior), `:402` (selection law), `:433` (variational τ) | preconditions of the recorded law | T | **no case found** |
| judge | two scoring guards | `war_machine.clj:6082`, `:6088` in `constructed-candidate-g` | the constructor's G call cannot be formed | T | **no case found** |
| judge | two configuration guards | `war_machine.clj:948` (habit prior span cap), `:2316` (mission value weights) | configuration out of range | T | **no case found** |

## D2.3 Typed absences the tick records and continues past

| kind | site |
|---|---|
| the abstention carrier itself: a judge that abstained without a refusal list, or a tick with no recorded decision, is a typed absence, "never an empty vector read as 'nothing declined'" | `full_loop_runner.clj:515-521` |
| `:no-reachable-want` — a C grain mismatch is recorded, not refused | `war_machine.clj:6422` |
| the close classification: `classify` catches an `ExceptionInfo` and returns a typed receipt | `run_ending_classification.clj:139-143` |
| the unmeasured default kernel `{… :basis :checkable :measurement :absent}` | `observation_rates.clj:174` |

## D2.4 The M-autoclock-in walk, continued

After its first criterion publishes (§3 step 1), the click runs and:

1. **Assembly refuses `:no-admitted-interpretation`** (`cascade_problems.clj:201`):
   nothing is published for this target yet. If a locator reading has not landed,
   `:universe-not-admitted` (`:218`) comes first — first applicable wins (`:17-28`).
2. The tick **abstains**; `abstention-carrier` records the decline and
   `click-summary` (`flight_runner.clj:30-45`) puts it on the click.
3. `record-click` sees no want advanced → **`:no-progress`**, and the flight
   ends (`flight.clj:218-220`) unless the ask step published an interpretation
   in the same click, in which case the next click re-enters assembly.
4. Nothing downstream is reached: no scoring, no selection, no gate, no close.

**Ends the click:** `:no-admitted-interpretation` (or `:universe-not-admitted`),
an abstention, not a stop. **Ends the flight:** `:no-progress`, not a refusal.
On this target the tick adds no flight-stopping refusal to §3's answer.

## D2.5 Two corrections to §1

**Row 12** said `:nonfinite-g` stops the tick as a throw. It is caught inside
the constructor (`interpretation_construction.clj:222-223`) and returned as
`{:status :refused :kind :nonfinite-g}`, surfacing as assembly's
`:no-constructed-candidate`: class C→T, not T. **Row 18** cited `efe.clj:1118`;
the map opens at `:1117`.

## D2.6 Counts

Counted by table row, as §4 was.
- **D2.2 has 25 rows**, covering **39 named kinds** over about 60 sites (the
  gate's 14 reasons and the authorization's 4 are one kind each; nine sites
  raise no named kind: the two rethrow boundaries, three precondition gates,
  two scoring guards, two configuration guards).
- **Already in D:** 7 kinds (its rows 12-18). **New here: 32 named kinds.**
- **With a case: 14 rows. Without: 11** — `:want-not-declared`,
  `:beta-not-declared`, `:mixed-candidate-kinds`, the F_π inputs,
  `:invalid-policy-prefix`, `:precision-consumption-mismatch`, the five
  `cascade_selection` kinds, the six accumulation kinds, the three precondition
  gates, the two scoring guards, the two configuration guards.
- **By class:** 24 rows stop the tick; 1 (`:nonfinite-g`) is caught inside the
  constructor; **none stops the flight.** Every tick refusal arrives at the
  flight as an abstention on the click record.
- **Register total: 11 flight-level kinds (§1 rows 1-11) + 39 tick kinds = 50.**

## D2.7 Falsifiers for D2

```sh
grep -c 'throw (ex-info' scripts/futon2/report/war_machine.clj   # 19, all in D2.2
grep -c '(refusal target' src/futon2/aif/cascade_problems.clj    # 9 sites, 5 kinds + horizon
grep -c ':status :missing' src/futon2/aif/efe.clj                # 5, all 5 in D2.2
grep -c 'throw (ex-info' src/futon2/aif/policy.clj               # 7
grep -co 'refuse! :[a-z-]*' src/futon2/aif/cascade_selection.clj # 6 sites, 5 kinds
grep -co 'refuse! :[a-z-]*' src/futon2/aif/decision_gate.clj     # 14 reasons
```

A count above the list's is a row this register missed. **Stated limit, as in
§5:** `full_loop_runner.clj` carries 52 further `throw` sites — the dispatch,
build, review and close machinery after a chosen action, which a first click on
M-autoclock-in never reaches (D2.4). They are named as a boundary, not
enumerated; a flight that gets a chosen action needs that third register.

---

# D4 — past the choice: the runner's post-choice machinery and the flight's enactment

Read at HEAD `d6df5909`; read-only, nothing proposed for removal. `enact-fn` is `fcc31531`, `wc-verdict-fn` `d6df5909` — step 11 landed while this was being read, so it is registered here rather than deferred. This closes the boundary §D2.7 named and did not enumerate.

## D4.1 `full_loop_runner`'s post-choice machinery — 52 throw sites, one boundary

**Class, checked rather than assumed.** `run-opportunity!` wraps `run-opportunity-core!` in a `catch Throwable` (`full_loop_runner.clj:5615-5620`),
types the failure by a stated precedence — an explicit `:failure-kind` at any depth, else transport typing, else `:initialization-failed`
(`:5670-5672`) — records a system-failure finding with a discharge contract (`:5685-5700`), and returns a terminal result. **All 52 therefore stop the
RUN and are recorded; none stops the flight**, which receives the result. Three families are re-thrown at `:5622-5624` and do escape the runner:
`:delivery-qa-gate-failed`, `:evidence-manifest/refusal`, `:limb-evidence/refusal`. A `{:cohort/error :stopping-rule-reached}` found anywhere on the
cause chain becomes `:outcome :cohort-complete` (`:5645-5651`) — normal completion, not a failure, and the comment there says why message text is
never consulted.

| family | sites (`full_loop_runner.clj`) | kinds | case |
|---|---|---|---|
| seat and transport | `:757` `:858` `:961` `:1155` `:1182` `:4538` `:4551` `:4743` | `:agent-unavailable` ×4, `:substrate-unavailable`, `:dispatch-failed` ×2, `:cancelled` | `:5660-5669`: a transport timeout charged to the machine demanded a repair commit and an independent review for a network fault; transport typing exists to stop that |
| build and author | `:1716` `:1722` `:2500` `:5137` `:5184` | `:guardrail-refusal`, `:build-failed` ×4 | at `:1722`, an author refusal without a reason is itself a build failure — a refusal that names nothing cannot be reviewed |
| review | `:5403` | review not approved / lacking execution evidence | **no case found at the site** |
| selection-side | `:1333` `:4730` `:4755` `:4778` `:4821` `:4828` `:4835` | `:unsupported-ranking-tie-rule`, `:abstained`, `:policy-nondiscrimination`, `:incomplete`, `:construction-failed` ×3 | `:1333` refuses to rank a posterior tie without its declared rule; the rest **no case found at the site** |
| grounding | `:2739` `:2812` `:2827` `:2885` `:2904` `:2919` `:2937` | `:grounding-failed` ×5, `:grounded-no-change`, `:incomplete` | `:2812`/`:2827` (`210dcdb0`): props must be storable AS WRITTEN — coerce Ratios, refuse the rest loudly, never round into the store |
| close and evidence retention | `:3015` `:3060` `:3090` `:3096` `:3162` `:3171` `:3191` `:3210` `:3264` `:3362` `:3405` | `:entity-identity-mismatch`, `:close-retention/refusal`, `:limb-evidence/refusal` ×5, `:evidence-manifest/refusal`, `:token-outcome/refusal`, `:kernel-example/refusal`, `:incomplete` | the two `/refusal` families are re-thrown at the boundary: evidence that changed under comparison must not be absorbed into a terminal record |
| historical verification | `:4897` `:4903` `:4928` `:4952` | `:historical-verification-refused` ×3, `:historical-verification-awaiting-validation` | **no case found at the site** |
| run bookkeeping | `:228` `:251` `:508` `:681` `:2714` `:3694` `:3729` | `:r16-park-repair-id-missing`, `:r16-park-registration-failed`, `:build-failed` (source drift), `:terminal-route-missing`, `:discharge-identity-invalid`, `:execution-cohort-recording-required`, a boolean guard | `:508` refuses when the serving runner's source drifts from the checkout; `:681` refuses to write a record with no route rather than inventing one |

By the `:outcome` each site carries: 19 carry none (they carry `:failure-kind` or a `:<ns>/refusal`), then `:build-failed` 5, `:grounding-failed` 5,
`:incomplete` 5, `:agent-unavailable` 4, `:construction-failed` 3, `:historical-verification-refused` 3, `:dispatch-failed` 2, and one each of
`:substrate-unavailable`, `:cancelled`, `:guardrail-refusal`, `:grounded-no-change`, `:policy-nondiscrimination`,
`:historical-verification-awaiting-validation`. **Cases, measured rather than judged:** 9 of the 52 sites carry a `;;` comment within twelve lines
above the throw; the other 43 are "no case found at the site", which is a statement about the site, not about whether a case exists elsewhere.

## D4.2 Typed absences the post-choice machinery records

| what | site |
|---|---|
| the abstention carrier (a judge that abstained without a refusal list; a tick with no recorded decision) | `full_loop_runner.clj:515-521` |
| `:chosen` `{:status :absent :reason :no-chosen-action}` and `:abstention` `{:status :absent :reason :no-selection-decision-recorded}`, no decision recorded | `:560-571`, `:522-525` |
| the close classification: `classify` catches an `ExceptionInfo` and returns a typed receipt | `run_ending_classification.clj:139-143` |

## D4.3 The flight's enactment path — `enact-fn` throws nothing

| point | site (`flight_runner.clj`) | what it records | class |
|---|---|---|---|
| no chosen candidate | `:519` | `{:absent :no-decision}`, no record written | flight continues |
| the seat's typed failure `{:failed {:reason …}}` | `:536`, `:545`; deviation `:550-551` | attempt `:success false` + `{:kind :step-failed}` | records, continues to the next pattern |
| grain gate not `:pass` | `:533`, `:541-542`; deviation `:552-554` | `:not-committed :grain-gate-refused` with the gate's reason, **no commit asked** | stops that commit only |
| grain gate's own kinds | `grain_gate.clj:80,84,88,94,103,111,116,121` | `:grain-not-declared` ×2, `:grain-mismatch`, `:scope-mismatch`, `:resolver-missing` ×2, `:arglist-mismatch`, `:sha-mismatch` (status `:refuse`) | recorded on the attempt |
| the check-fn throwing | `run-check` `:470-471` | `{:status :refused :reason :check-threw :detail …}` | caught, recorded |
| the seat gave no check map | `:476` | `{:absent :no-check-from-seat}` | recorded |
| no handler for the locator's class | `:512-514` (default check-fn) | `{:status :refused :reason :no-mechanical-check}` | recorded |
| the click's run record not fetched | `:565` | `{:absent :run-record-not-fetched}` | recorded |
| candidate names no grain pattern | `:566`, `:569`, the gate's own refusal at `:575-576` | `{:absent :candidate-names-no-grain-pattern}` | recorded |
| nothing published | `:573` | `{:absent :no-publication-observed}` | recorded |
| W_c: no checker configured | `wc-verdict-fn` `:605` | `{:wc {:absent :no-wc-checker-configured}}`, and `increment` is **not called** — the docstring's "never a default pass" | recorded |
| W_c: exit ≠ 0, or output that is not one EDN form | `:610-616` | `{:wc {:refused :checker-failed :exit n :stderr s}}`, "never a verdict" | recorded |
| the fold's increment | `enactment_habit.clj:66-96` | an attempt at a pattern outside the candidate `{:status :refused …}`; `[]` → `:delta 1`; failures → `:delta 0`; a missing verdict → `:delta 0` with `{:status :absent}` | recorded |

**Nothing on this path throws.** `flight_runner.clj` has three `throw`s and all three are the read step's (`:227`, §1 row 1). What can still stop the
flight is an exception out of an **injected** function: `flight.clj` has **zero** `catch` sites, and of the injected fns only `check-fn` is wrapped
(`run-check:470`) — `dispatch-step!`, `fetch-run-record`, `publication-observation` and `wc-fn` are not, nor is the record write (`.mkdirs`/`spit`,
`:579-580`).

## D4.4 The walk, past the click

With an interpretation published and a candidate chosen, M-autoclock-in's first enactment reaches, in order: `:519` (a record will be written) → for
each pattern in precedence order, `dispatch-step!` `:phase :commit`, or for the grain pattern `:phase :plan` then `grain-gate` then `:phase :commit` →
`run-check` → the run-record fetch and the publication observation → the record write → `wc-verdict-fn` → `increment`.

**First thing that stops the enactment: nothing does.** Every branch records; the nearest is the grain gate's refusal, which stops one commit and not
the enactment. **First thing that would stop the flight:** an exception out of `dispatch-step!` — the first injected function called, unguarded, with
nothing catching it between there and `flight/run!`.

**The dispatch to a real seat has no site on this path yet.** At `d6df5909` futon2 has **no production `dispatch-step!`**: the only implementations
are test fixtures (`flight_enact_test.clj:27`, `flight_grain_gate_test.clj:20`). So a seat not on the roster, a job that never returns, and the
requisition rule (M-wm-wiring's ruling of ~17:40Z: eligibility read from requisition state, not from the target's origin) surface **nowhere in the
flight's enactment**. The runner carries its own Agency client with those refusals — `agent-roster` `:agent-unavailable` (`full_loop_runner.clj:757`),
`post-json!` `:dispatch-failed` (`:961`), `read-job!` `:dispatch-failed` (`:1182`), `throw-if-cancelled!` `:cancelled` (`:1155`) — but the flight's
enactment does not go through the runner, so none of them is this path's. When the spike's dispatch function lands it brings refusals that have no
register row, and by the standing rule a refusal reached that is not on the register is a wiring defect: they belong in the same commit as that
function.

## D4.5 Counts and falsifiers

**29 new kinds**: 14 terminal outcomes and 3 re-thrown refusal families at the runner's boundary; 6 grain-gate kinds; 2 W_c
(`:no-wc-checker-configured`, `:checker-failed`); 2 from `increment`; 2 check refusals (`:check-threw`, `:no-mechanical-check`). **Register total: 11
flight-level (§1) + 39 tick (§D2) + 29 = 79**, over about 120 sites. Five more typed absences on the enactment record (§D4.3), none of them a refusal.

```sh
grep -c 'throw (ex-info' src/futon2/aif/full_loop_runner.clj   # 52, all in D4.1
grep -cE 'catch (clojure.lang.ExceptionInfo|Throwable|Exception)' src/futon2/aif/full_loop_runner.clj  # 40
grep -c 'throw' src/futon2/aif/flight_runner.clj               # 3, all the read step's (:227)
grep -c 'catch' src/futon2/aif/flight.clj                      # 0 — nothing injected is guarded there
grep -c '(refuse :' src/futon2/aif/grain_gate.clj              # 8 sites, 6 kinds
grep -c 'dispatch-step!' src/futon2/aif/flight_driver.clj      # 0 — no production seat dispatch yet
```

A count above a list's is a row this register missed. **Stated limit:** the 43 sites in D4.1 with no case at the site were not chased into the packets
and rulings that may carry one; "no case found at the site" is all that was measured.