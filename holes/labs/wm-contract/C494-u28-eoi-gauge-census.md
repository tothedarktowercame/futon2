# C494 — U28: the gauge census for M-expressions-of-interest

**Row:** U28 (worklist.edn), "CLOSE THE RED, part 3: the gauge row". **Date:**
2026-09-03. **Seam:** `futon2/scripts/futon2/report/war_machine.clj`
`mission-c-declared-gauges`.

## What the row asked for and what the mission supplies

U28 asks for U18 (d)'s move: read M-eoi's criteria and "derive each criterion's
gauge from its own measurable-by/carrier prose where one is genuinely readable".

**That prose does not exist for this mission.** The zaif exemplar U18 worked
from is an IDENTIFY ingest whose `:preferences/c` rows carry `:carrier` strings
(`holes/labs/zaif-harness/runs/S4-identify-ingest.edn:31-32`), which is why its
three criteria read `:unresolved-observable` — a measurement stated for a human.
M-expressions-of-interest is a mission document read by
`mission-c/criteria-from-markdown`; its criteria are six numbered sentences with
no measurement field of any kind, and the reader returns
`:no-declared-measurement` for all six. Reproduced before any change:

```
clojure -M -e "(mc/read-criteria .../M-expressions-of-interest.md :observables {})"
:criterion-1..6 | :unmeasurable | :no-declared-measurement
```

Search for a measurement field in the document: `grep -n -i
"measurable\|carrier\|observable" M-expressions-of-interest.md` — **not found**
(zero hits).

So there is nothing to derive a binding FROM, and the row forbids inventing one
("never an invented binding"). All six criteria take the row's other branch: a
typed `:no-producer` entry naming what would have to exist.

## Why each criterion does not bind

Five of the six conjoin an artifact clause with a clause only Joe can
discharge. A gauge reading the artifact clause alone would measure a *different*
criterion and record the score against this one — the plant U12 was corrected
for. The sixth (criterion-5) has no Joe-clause and fails for the other reason.

| criterion | pointer | the clause that cannot be read | named producer that would have to exist |
|---|---|---|---|
| 1 | `M-expressions-of-interest.md:174-176` | "Joe reports that re-reading it ... actually constrains the drafting" | a declared path for the type-signature self-note **and** a dated ledger of drafting occasions recording whether the note was re-read and whether it constrained the draft |
| 2 | `:177-180` | "at least one non-default basin **reads as** genuinely live rather than perfunctorily listed" | a basin register at a declared path with the three geometry fields the criterion names (satisfies / forecloses / costs), plus a recorded per-basin liveness verdict |
| 3 | `:181-182` | "pass Joe's \"really?\"-test on his own re-reading" | a per-artefact record of Template conformance **and** a `really?` verdict Joe entered per artefact |
| 4 | `:183-184` | "its existence does not foreclose work on alternative basins" | the brief at a declared path with a stated page-equivalent measure, plus a recorded non-foreclosure judgement |
| 5 | `:185-187` | *(none — the criterion is entirely about artifacts)* | one diagnosis artifact per named specimen, at the path MAP move M-3 settles on, each carrying a speech-act attribution from the Template's vocabulary (`:229`) |
| 6 | `:188-189` | "evidence that the mission is generative, not just descriptive" | a basin register with a first-named date per basin, so "previously-unnamed" is a diff and not a judgement |

## The artifact half, searched

Recorded because "the criterion's artifact does not exist" and "the mission
never said where it would live" are different repairs, and four of the six are
the second.

- **Self-note (criterion 1).** `grep -rn "self-note" futon5a --include=*.md
  --include=*.edn`: four hits, all inside the mission document
  (`:127`, `:158`, `:174`, `:1109`). No path is named; `:1109` still lists it as
  work to do under INSTANTIATE step (d), and `:321-326` (MAP move M-5) says the
  artefact is downstream of the prior network rather than being it.
- **Basin enumeration (criteria 2, 6).** `:298-303` (MAP move M-2, *Started*)
  puts basin enumeration in `M-expressions-of-interest.strawmen/` and names
  `hyperreal-freelancer` as the **missing** strawman. Found but **not declared
  by the mission**: `futon6/data/dry-basins/M-expressions-of-interest.edn`,
  whose own `:confidence` is `:reconstructed-thin`. Recorded here in the shape
  U42 used for `known-gap-list-artifacts`: an artifact that exists is not an
  artifact the criterion points at.
- **`really?` verdicts (criterion 3).** The corpus the mission declares
  authoritative is `~/code/atthangika-buckets.json` (named at `:1183-1200`). It
  carries 17 `eoi_instances`; the union of their keys holds no template-
  conformance field and no `really?` verdict. `grep -rn "really" futon5a
  --include=*.md --include=*.edn --include=*.json`: 102 hits, all prose;
  `grep -rn "really.\?-test\|:really"` over the same set — **not found** (zero
  hits). Nothing anywhere records a verdict.
- **Hyperreal brief (criterion 4).** No path anywhere; and whether it is to be
  written at all is open — `:1101-1102` states the step as "commit (1-3 pages
  with a date) **or** formally drop the §1 criterion". The two page figures also
  disagree with the criterion's own "20-page-equivalent" (`:183`).
- **Gary and Bristol diagnoses (criterion 5).** `grep -rn -i "gary" futon5a`:
  five files, none a diagnosis (`TN-mvsg.md:72,:81` is a journal mention;
  `glasgow-cogito-neurotech-RA/annotations.edn:92` and
  `strawmen/anthropic-institute-analyst.md:177`,
  `strawmen/glasgow-cogito-neurotech-RA-198593.md:110` cite the proposal as a
  comparison, not a diagnosis of it). The Bristol EoI is in the corpus as
  `ukrn-sra-bristol-2026-07-18` and carries **no** `speech_act` key — 1 of 17
  instances does (`hyperreal-director-side-b-2026-05-14`). The mission's own
  status agrees: `:304-308` (M-3, *Pending*) — "Gary proposal and Bristol
  sentence not yet diagnosed under the speech-act taxonomy. Should become two
  additional strawmen (or a separate `diagnostic-specimens/` subdirectory)".
  An `or` is not a path, which is why this one is typed rather than bound.
- **EoI type coverage (criterion 6).**
  `futon5a/interest-scanning/eoi-network-v1.edn:1516-1531` carries
  `:basin-coverage {:classified 17 :unclassified []}`. Its `:observed-types` are
  EoI *types* (`outward-one-shot`, `inward-mission-HEAD`, ...), not the career
  basins criterion 6 means (the provisional list is prose at `:132-136`:
  frontier-AI employee, postdoc, startup, Hyperreal freelancer, UKRN-S, hybrid).
  Reading the one as the other would be the plant.

## What moved

- `mission-c/apply-gauge` accepts a `:no-producer` gauge; `resolve-measurement`
  types it `:reason :no-producer` with `:because` and `:would-need` on the row.
  It is a third *finding*, not a third test: `:no-declared-measurement` says
  nobody has written a measurement down, `:no-producer` says someone looked and
  recorded what would have to exist. The document still wins both ways — a
  criterion that declares its own measurement keeps it and the `:no-producer`
  declaration is dropped.
- `war-machine/mission-c-declared-gauges` gains
  `"M-expressions-of-interest"` — six `:no-producer` entries, no observables.
- The readback's `:declared-gauges` maps an unbound criterion to `:no-producer`
  rather than to a nil where an observable would be.
- `scripts/wm_status_report.py` builds the observable vocabulary only from
  gauges that name an observable, so a non-binding cannot be counted as one.

## The number

**Unchanged at 3/9** (M-zaif-harness-v1 3/3, M-expressions-of-interest 0/6).
U28 expected `>3/9`; the honest number wins, and the honest number is that six
criteria that name no measurement produce no bindings. What changed is the
*reason* on the record: the six read `:no-producer` with a named producer each,
where before they read `:no-declared-measurement` — un-looked-at.

The nearest repair is criterion 5: settle M-3's `or` into a path, write the two
diagnoses with a speech-act attribution, and a producer becomes writable in the
`futon2.aif.mission-gauges` shape. That is a mission-side decision, not one this
row takes.

## One committed artifact now disagrees, and is left alone

`runs/U21-selection-focus/measurements.edn:164-168` records the six criteria as
`:no-declared-measurement`. That is what the reader returned on the run U21 took,
and it is a run record, so it is not rewritten. A re-run of the same replay after
this row returns `:no-producer` for the same six, for the reason this note gives.
U21's own row already assigned the disposition here ("M-expressions-of-interest's
6/6 :no-declared-measurement is NOT minted here -- U28 already owns it",
worklist.edn U21 `:evidence`).
