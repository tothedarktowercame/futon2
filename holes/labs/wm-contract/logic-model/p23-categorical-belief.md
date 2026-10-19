# P23 — R1 categorical belief description

**Disposition:** documentation discrepancy only.  The implementation described
below is an AIF-valid discrete categorical filter; this packet proposes a paper
correction and an annotation/correlation mapping, and does not change the
belief implementation, its observations, or its witnesses.

**Reading base.**  This is a bounded source reading at futon2
`ce554eb4a56d7120c4499e2bc4e330428e7a1c1a`; the P23 starting instruction is
at `PACKETS-2026-10-19.md:547-559` on that commit.  The annotation anchor is
the older futon2 `e6a0a5f36932d0c8931f52cd3a6d032894a8159b` and is unreviewed
(`p4ng/plop-2026-code-annotations.json`, annotation A6).  The paper and
annotation file were read at p4ng `2e67e34d7bbc237518b7387ebf8b2c10fd293399`.
Joe's 2026-10-19 frontier ruling applies: unavailable evidence is retained as
unavailable, not supplied by a zero, a made-up posterior, or a claimed
execution.

## Finding

R1's current `Then` says that the belief is one six-key map:
`:goal`, `:subgoals`, `:blockers`, `:pattern-fit`,
`:expected-next-observation`, and `:uncertainty` (p4ng
`sec-catalog.tex:220-224`, p4ng `2e67e34d`).  That is not the belief-state
contract implemented by futon2.  `status-set` is the seven-element categorical
vocabulary `#{:spawned :refined :strengthened :addressed :falsified
:foreclosed :reopened}` (`src/futon2/aif/belief.clj:37-42`, futon2
`ce554eb4a`).  `initial-belief-state` builds `{entity-id -> uniform posterior
over that set}` and contains no enclosing operational-hypothesis record
(`belief.clj:64-68`).

This was checked rather than inherited from A6: an exact-key census of the
current `belief.clj` found **zero occurrences of each** of the six names.  That
is the relevant claim.  A broader source-tree absence claim would be false or
ambiguous: `:blockers` appears in `full_loop_runner.clj` as a want-data field,
and `:uncertainty` occurs in other reporting/observation shapes.  Neither is a
key in this posterior representation.  `:goal`, `:subgoals`, `:pattern-fit`,
and `:expected-next-observation` were also not located as exact keys in the
current `src/`, `scripts/`, `test/`, and `resources/` census, but that broader
negative result is not needed to establish R1's representation mismatch.

## Actual representation and update boundary

For each tracked entity `e`, the stored value is a normalized posterior
`q_e(s)` over the seven statuses.  Cold-start entities get the uniform prior
`D` (`uniform-prior`, `belief.clj:44-49`; `initial-prior-v1`,
`belief.clj:230-234`).  Stack bootstrap supplies entity identifiers from the
stack annotations plus supplied sorry ids, then delegates to
`initial-belief-state` (`bootstrap-from-stack-annotations`,
`belief.clj:577-596`).  The live War Machine obtains that fresh domain and,
when carrying is enabled, reconciles the prior tick's `:mu-post` onto it
(`scripts/futon2/report/war_machine.clj:6474-6483`).  Thus an entity is an
identifier in the outer map; it is not a field such as `:goal` inside one
global belief map.

The declared formal route has:

- **A**, `observation-model-v1`: a column-normalized 7x7
  `P(observation | status)` matrix (`belief.clj:201-207`, validated at
  `249-260`).
- **B**, `transition-model-v1`: a 7x7 identity transition model, explicitly
  meaning no dynamics beyond carrying the prior posterior (`belief.clj:216-228`,
  validated at `262-271`).
- **D**, `initial-prior-v1`: the uniform distribution above.  The model
  manifest hashes A, B, and D for provenance (`belief.clj:301-310`).

In `:aif` mode, `categorical-filter-step` performs
`q^- = B q`, then a normalized, tempered A likelihood update
`q(s) ∝ A^κ(o|s) q^-(s)` (`belief.clj:407-425`; update-mode routing at
`443-504`).  This describes the code's categorical posterior precisely; it
does not assert that the hand-set A matrix is externally calibrated.

An event whose `:type` is outside `status-set` is not coerced into a status or
an observed zero.  At the per-entity level it leaves the posterior unchanged
and attaches `{:status :omitted :reason :unknown-observation :event ...}` as
metadata (`omitted-observation`, `belief.clj:400-405`).  At the population
level `update-belief` does **not create an entity** for such an event; it
retains the same typed omission in `::omitted-events` metadata
(`belief.clj:506-520`).  The trace writer persists these entries as
`:belief-event-omissions` only when present (`src/futon2/aif/trace.clj:489-498`).
This is absence/omission evidence, not a posterior value.

## Consumers and retained representation

The production seam applies batches with the arena's declared likelihood mode
(`apply-arena-belief-events`, `war_machine.clj:825-837`) and sends the result
through the R3 inner loop (`war_machine.clj:6528-6541`).  The trace schema
retains `:mu-pre` and `:mu-post` as `entity-id -> posterior` maps and describes
carry to the next prior (`trace.clj:20-30`); `trace-record` writes them from
the judgement's `:belief-pre`/`:belief` (`trace.clj:422-446`).
The predictive-model path is a separate real consumer: it calls
`update-belief-batch` when constructing its next belief
(`src/futon2/aif/forward_model.clj:425-427`).  These source-level consumers
establish the carrier and call boundaries, not that either path was reached by
a current serving run.

A retained historical judgement/trace artifact is available at
`holes/labs/wm-contract/runs/2026-09-05-u60/wm-trace-2026-09-05.edn` (its
recorded futon2 SHA is in that artifact).  Its `:mu-post` contains string entity
ids whose values are seven status probabilities—for example the
`arxana/stack/futon-v1/leaf/2/2` entry—not any of the six R1 keys.  That is
evidence of the representation in a retained historical record, not evidence
that the current serving JVM has run this code or that the A/B parameters are
calibrated.  The 2026-10-19 packet explicitly says not to upgrade static facts
to live claims without a retained run ID and evidence; this packet makes no
such upgrade.

The two Lean claims mentioned by the present R1 worknote are therefore not
redeclared as a fresh witness here.  The adjacent R1 update annotation (A7,
not P23's A6 shape annotation) says they were not re-checked, and P23
authorizes no validation action.

## Precise paper replacement

Replace the `Then` map display and the following words “updated each step using
observation and outcomes” in p4ng `sec-catalog.tex:220-224` with:

> **Then** represent belief as a map from each tracked entity identifier to a
> normalized categorical posterior over the statuses
> `:spawned`, `:refined`, `:strengthened`, `:addressed`, `:falsified`,
> `:foreclosed`, and `:reopened`.  Start a new entity from a uniform prior
> (D); for an admitted status observation, predict with the declared
> transition model (B) and update with the declared observation model (A),
> retaining the resulting posterior.  Observations outside that status
> vocabulary leave the posterior unchanged and are retained as typed omitted
> observations rather than treated as zero evidence or invented state fields.

The existing `If`, `However`, and `Because` can remain: their requirement for
tractable, testable hypotheses is met by this representation.  The replacement
does not claim outcome calibration, a non-identity B, external validation, or
that a current production judgement exists.

## Proposed shared-table mapping (not applied here)

Add one row to `logic-model/correlation.md` section 3, after its heading at
current lines 244-246:

| pattern | annotation verdict | anchor | what the tables say about that code on the live path |
|---|---|---|---|
| R1 | **REFUTED as paper shape; docs correction proposed; implementation AIF-valid but live/calibration status not upgraded** | `belief.clj::initial-belief-state` (A6, pin `e6a0a5f36932`; current source `ce554eb4a:64-68`), plus `categorical-filter-step` (`ce554eb4a:407-425`) | The actual carrier is `entity-id ->` seven-status categorical posterior; the declared A/B/D model is wired through the arena batch seam and retained as `:mu-pre`/`:mu-post`. Unknown-status events are explicitly omitted, not zeroed. Historical retained trace shows this shape; this packet does not establish a current live run or calibration. |

This mapping corrects paper-to-code correspondence without turning the R1
annotation into a runtime gate.  It also distinguishes the two R1 annotation
entries: A6 is `REFUTED` because the prose names the wrong shape; A7 remains
`EXISTS-UNVALIDATED` for predict/observe/update and must not be silently
promoted merely because the documentation is repaired.

## Limits and concrete discrepancy

No implementation discrepancy was found within P23's assigned boundary:
`initial-belief-state`, A/B/D, categorical update, omission retention, and
trace shape agree with the proposed categorical description.  The concrete
discrepancy is paper R1's six-key `Then` versus that code contract.  It is
docs-only under the annotation policy, so no belief rewrite is proposed.

No runtime, test, serving, ledger, or witness-validation action was performed.
