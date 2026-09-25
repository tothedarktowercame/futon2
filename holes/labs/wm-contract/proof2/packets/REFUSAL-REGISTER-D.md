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
