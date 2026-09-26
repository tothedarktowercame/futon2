# WM-CLICK-REFUSAL-D — the fourth flight's click refusal (discovery, read-only)

claude-10, 2026-09-26, for claude-8 (packet WM-CLICK-REFUSAL-D). Read at
futon2 3b847d56 and futon3c 38cb4956 (`holes/labs/M-wm-wiring/spike/`: the
flight record, the tick run record, `attempt-001-flight-e70b4baf/001-007`,
the morning-brief item, the timeline). No code, no test, no flight, nothing
written under `data/` (read only: one repair finding, listed below).

**Reproduction.** In my own JVM (`clojure -M:test`), against a copy of
`data/wm-interpretations` at `/tmp/reqfour/store`, I rebuilt the click's
input as `judge` does (`war_machine.clj:7210-7250`):
- the declared sources;
- `flight/click-wants` on the recorded flight, which gives the same 8 wants
  as the recorded click;
- `flight-assembly-input`;
- `assemble-cascade-problems-with-published`, then `record-supply`.

Then I called `cascade-decision` (`:6803`). It calls no writer: I did not call
`select-and-record-cascade!`, and `emit!` is pure. Script:
`/tmp/repro-four.clj`.

## 1. Which refusal fired

None of the four sites named in the packet fired. On the click's inputs:

| check | result |
|---|---|
| targets | `["M-autoclock-in"]` |
| problems | one: T 4, β 1, 1 candidate |
| refusals | none |
| `cascade-family-parameters` | `{:horizon-steps 4 :beta 1}` |
| `live-c/derive-live-c` refusals | none |
| `live-c/stale?` | false |

`cascade-decision` then threw "cascade decision refused" from the **rethrow
boundary at `war_machine.clj:6624`**. `efe/rank-actions` returned a refusal map
instead of a ranking, and `:6624` merges its `:kind`:

```
{:kind :class-unknown-no-scalar-g :status :missing :query :score
 :target "M-autoclock-in"
 :possible-costs {:focused 0.5978… :related 1.0498… :unrelated 2.9957…}
 :model {:kind :class-emission :horizon 4 :target-class {"M-autoclock-in" :unknown} …}}
```

The refusal is raised in `observation_model.clj:216` (`refuse!`, `:31-32`).

**The live record agrees.** The click's repair finding carries
`:failure-data {:kind :class-unknown-no-scalar-g …}` beside
`:failure-kind :untyped-failure`. That file is
`data/wm-repair-obligations/findings/repair-occ-e46f72e8….edn`, named by the
brief item's `:repair-id`. So the kind that fired first is
**`:class-unknown-no-scalar-g`**.

**Why the class is `:unknown`.** The class model (`:6583-6589`) maps each
target's `focus-receipt/classify-target` class through
`{:focus :focused :associated :related :useful-elsewhere :unrelated}`.
Anything else becomes `:unknown`. For M-autoclock-in, `classify-target`
(`focus_receipt.clj:98-139`) returns `:class :unknown`,
`:reason :relation-not-declared`. The facet corpus
`resources/wm/focus/commit-facets-v1.json` has 9 relation rows and none names
M-autoclock-in. Its focus windows end 2026-09-22T17:31Z; the decision retains
the last one's focus, but the reason is the missing relation row, whichever
focus holds.

Under codex-20's ruling (`observation_model.clj:209-214`) an unresolvable
relation gets no scalar G. A flight family has exactly one target
(`flight-assembly-input`, `:6046`, `:targets [(:target flight)]`), so no
candidate keeps a scalar, and the ranking refuses as a whole
(`class_observation_scoring_test.clj:401` pins that). **Every machine-read
target without a relation row refuses here on every click.**

**The flight target's β and horizon today:**
- **β:** `flight-assembly-input` gives the flight target context `:WM`
  (`:6051-6058`). `beta-for` (`cascade_problems.clj:65-76`) reads
  `:beta-by-context :WM`, which is `{:beta 1 :status :declared}`. All five
  source files under `resources/wm/cascade-sources/` declare `:context :WM`
  with `:beta {:value 1 :status :declared}`.
- **Horizon:** `resolve-cascade-horizon` (`war_machine.clj:6160-6184`) takes a
  declared `:horizon-steps` over a computed one. The sources lift
  `T-repair-occ-444fb018.edn:31`'s `:horizon-steps 4` into the merged map
  (`cascade_sources.clj:302-312`). So M-autoclock-in is scored at T = 4, a
  horizon declared for a repair ticket. Its computed value would be 6, the
  count of its admitted interpretations (`:6172-6177`).

**Can a machine-read target be incommensurable?** Not on a flight click, as
the code stands. The family has one problem, so `distinct` over T and over β
has one element. On an ordinary tick every problem reads the same single
`:horizon-steps`, and β comes per context. `:incommensurable-family` there
needs two contexts with different declared β, and today all declare 1.

**`:live-c-stale` cannot fire in production.** `:6316-6318` compares the
derivation against `(or (:sources-now live-c-opts) live-sources)`, which is
the same read it derived from. The "fresh re-read" named in the comment at
`:6282-6284` does not happen. That is a separate finding, not this click's.

## 2. Why the record says untyped

`failure-kind-from` (`full_loop_runner.clj:3558-3563`) takes
`explicit-failure-kind` (`:3522-3529`), which reads `:failure-kind` or
`:outcome` from the cause chain. This throw carries `:kind` and
`:status :missing`, so it falls to `:untyped-failure`.

**Where the ex-data goes.** It is not dropped entirely: `close!`
(`:5569-5595`) passes it as `:error-data`, and `close-core!` puts it on the
repair finding's `:failure-data` (`:3910`). But it reaches:
- neither the closed checkpoint (`007-closed.edn`'s judgment has only
  `:failure-kind :untyped-failure`);
- nor the brief item;
- nor the run record.

**Why the abstention reads absent.** The abstention path needs the judge to
*return* an abstained decision. `persist-run-record!` builds the carrier from
the selection checkpoint's `:controller-decision`/`:decision` or from the
`:no-selection` sorry cell (`:596-603`; the cell is written at `:4719-4724`).
A throw writes neither, so `abstention-carrier` (`:519-547`) takes its
`(nil? decision)` branch, `:no-selection-decision-recorded`.

**Every throw in the joint decision typed under `:kind`**, none under
`:failure-kind` or `:outcome`:

| site | kind |
|---|---|
| `war_machine.clj:6237`, `:6240` | `:incommensurable-family` (with `:horizon-steps` or `:beta`) |
| `:6301` | `:live-c-refused` |
| `:6323` | `:live-c-stale` |
| `:6437` | live-C's own refusal kind, rethrown whole (every kind except `:no-reachable-want`) |
| `:6624` | the rethrow boundary: `(or (:kind ranked) :rank-refused)` merged with `ranked`, carrying whatever `efe/rank-actions` returns. That includes `observation_model.clj:216` `:class-unknown-no-scalar-g`, and the scoring kinds of register §D2.2: `:missing-common-horizon`, `:missing-cascade-belief`, `:missing-cascade-want`, `:invalid-adjudication-rates`, `:mixed-candidate-kinds`, `:unknown-class`, `:unsupported-class` |
| `:6095`, `:6101` | `constructed-candidate-g`, typed `:constructor/refusal` (a third key, also unread) |
| `ticket_queue.clj:34`, `:52`, `:75` | `ticket-queue/validate!` (`:kind :invalid-ticket-queue`), called at `:6817` |
| `decision_gate.clj:56-60` | `refuse!`, typed `:error :inadmissible-decision :reason …`, a fourth shape |

`cascade_problems.clj` throws nothing in the decision: its refusals are
returned `{:target :kind :missing}` maps.

## 3. The fix: shape and size

**Which side the register expects.** Register §D2.2 (`REFUSAL-REGISTER-D.md:198-200`)
classes every judge and scoring kind **T**: "stops the tick (the abstention
carrier records it, the flight's click-summary reads it)". The class says a T
refusal ends the click as an abstention on the record, which is the runner
side.

**Why not the war machine side.** Adding `:failure-kind` at the throw sites
would type the failure but keep it a machine failure: `repair-class-for`, a
repair obligation, a discharge contract charged against the machine for a
target with no relation row. Making the judge *return* an abstention instead
would push these kinds through `decision-gate/emit!`. Its
`allowed-refusal-kinds` (`decision_gate.clj:43-49`) is the closed set of SPEC
§Decision 4, and changing it is a spec change.

**Proposed (two functions in `full_loop_runner.clj`):**

1. **The selection phase's catch** (around `:4719`). An `ExceptionInfo` from
   the judge whose ex-data carries `:kind` and `:status :missing`, or one of
   the judge kinds above, writes the `:no-selection` sorry cell with
   `{:judge-refusal {:kind k :target t :data (ex-data e)}}`. Here `t` is the
   ex-data's `:target`, or else each target of the family (on a flight, the
   flight target). The click then closes `:incomplete` without a repair
   obligation, as an abstained tick does. Anything without `:kind` still
   falls to `failure-kind-from`.
2. **`abstention-carrier`**: a branch before `(nil? decision)`. A sorry cell
   with `:judge-refusal` gives
   `{:status :abstained :targets [{:target t :kind k :missing <from the kind>}]}`.
   The `:missing` values would be:
   - `:class-unknown-no-scalar-g` → `:target-relation`
   - `:incommensurable-family` → `:common-horizon` or `:common-beta`
   - `:live-c-*` → `:live-c`

   The checkpoints' `:sorry` carries the same `:kind`.

**The flight's click entry** needs no change. `record-summary`
(`flight_runner.clj:402-417`) already reads `[:decision :abstention]`, and
puts this target's `{:target :kind :missing :declines}` on the click as
`:abstention`: fix A's shape.

**New fields:** none on the carrier or the click. The ex-data (e.g.
`:possible-costs`) stays on the sorry cell's `:judge-refusal :data`, where
the checkpoint keeps it. This is the refusable part:
- If `:data` on the sorry cell counts as a new field, its case is red-tape
  removal: the reason already exists and today only the repair finding keeps
  it.
- Without `:data`, the possible costs are lost once no repair finding is
  written.

**Test box `futon2.aif.judge-refusal-abstention-test`** (in the runner, fixture
ports, no seat):
- **The bad case first:** a judge stub throwing
  `(ex-info "cascade decision refused" {:kind :incommensurable-family :horizon-steps [2 4]})`.
  The run record's `[:decision :abstention]` must be
  `{:status :abstained :targets [{:target … :kind :incommensurable-family …}]}`,
  not `:no-selection-decision-recorded`. The closed checkpoint's
  `:failure-kind` must not be `:untyped-failure`, and no repair finding may be
  written.
- **The flight's view:** `record-summary` over that record gives the click's
  `:abstention :kind :incommensurable-family`.
- **The live pin:** the same through the kind that fired, with the ex-data
  captured from the repair finding above.
- **The control:** an untyped `RuntimeException` from the judge stays
  `:untyped-failure`.

**Map:**
- `:r9-selection-law` and `:r6-cascade-lane` do not move.
- The runner box that writes the abstention carrier gains a declared read of
  the judge refusal on the sorry cell.
- The `:r6`/`:r9` boxes' declared writes are unchanged, since the throws
  themselves do not change.

**Register note for claude-3:**
- `:class-unknown-no-scalar-g` (`observation_model.clj:216`, through
  `:6624`) is not on §D2.2, which lists `:unknown-class` and
  `:unsupported-class` from `observation_rates.clj` only. It needs a
  scoring row, class T, case codex-20's handoff-B ruling (a target whose
  relation cannot be resolved gets no scalar G).
- §D2.2's line numbers are at 3bbf5059; at 3b847d56 the judge rows are
  `:6237`, `:6240`, `:6301`, `:6323`, `:6437`, `:6624`.

## 4. The definition question behind (1)

**Horizon and β.** The plan records the common-horizon rule: one T for the
compared family. On a flight the family is one target, so the question is
what declares T and β for a target no source declared.

- **Answer 1: the family's declared values**, the rule as it stands. Today
  that is T 4 from a repair ticket's file and β 1 from the `:WM` context.
  - Cost: nothing new under IDENTIFY 4.
  - But the horizon then belongs to whichever source file declares one. A
    6-interpretation target is cut at 4 (`:beyond-horizon` becomes possible),
    which is the fallback `resolve-cascade-horizon` was written to remove.
- **Answer 2: its own computed values** when no source declares them for it.
  The computed horizon, count of admitted interpretations, is already
  recorded as `:per-target`.
  - Cost: none new either. The declared `:horizon-steps` would have to apply
    only to the targets its file names, which is a narrowing of
    `cascade_sources.clj:302-312`'s lift, not a field.

**The class relation** is what actually fired. A machine-read target has no
row in the facet corpus, so its relation is unresolvable by construction.
Either the corpus gains rows for flight targets (who writes them, from what
evidence), or a flight target's relation is defined some other way, such as
the facet its mission file lives under. Until one is chosen,
`:class-unknown-no-scalar-g` refuses every flight click that reaches
scoring.

## 5. Recommendation and falsifier

**Build the runner-side fix (§3)** as one packet: two functions, one test
namespace, the register row. It turns every T refusal of the joint decision
into the tick's typed abstention, which is what the register already says
they are.

**Put the relation question (§4) to Joe** before a fifth flight, since it
decides whether any flight click can reach selection. Put the horizon
question to Clause T.

**Falsifier on a fifth flight:**
- The run record's `[:decision :abstention]` is
  `{:status :abstained :targets [{:target "M-autoclock-in" :kind :class-unknown-no-scalar-g …}]}`.
- The flight's click entry has `:abstention :kind :class-unknown-no-scalar-g`.
- `007-closed` does not say `:untyped-failure`, and no repair finding is
  written.
- If the relation question is answered by definition: the click reaches
  selection, or refuses at the next registered kind, typed the same way.
