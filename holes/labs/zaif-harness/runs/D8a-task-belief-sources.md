# D8a — typed sources for ZAIF `:task-belief`

Discovery date: 2026-09-02. This was a read-only survey: no runtime or store
state was changed and no namespace was loaded into either shared JVM.

## Result

No existing typed source has the semantics required by
`zaif-controller/act-value`. The candidates that are populated describe policy
precision, operator-intent uncertainty, retrieval value, mission preference,
mission status, or an earlier controller decision. None says how much acting
now is expected to advance the current task. The design calls for an
actand-indexed world-model ledger, but only the design document and lexical
training counts exist today; there is no named query, table, or hydrator reader
for it.

The controller makes the required meaning mechanically checkable at
`futon3c/src/futon3c/agents/zaif_controller.clj:76-85`: `:task-belief` must
contain `:act-value`, `:pragmatic-value`, or `:expected-utility`, and that number
is multiplied by mission policy precision. The hydrator instead returns `{}`
on both its ordinary and exceptional paths at
`futon3c/src/futon3c/agents/zaif_inputs.clj:177-209`.

## 1. Gamma artifact and its cells

**Location and shape.** `zaif_inputs.clj:28-74` reads
`futon2/holes/labs/M-zaif-harness/b1-gamma-mission.edn`; lines 81-102 expose
each cell's `:policy-precision`, and lines 104-151 derive operator-C uncertainty
from `:perf-history`. The full artifact also contains `:events`, `:provenance`,
and `:summary`. It contains no act value, predicted outcome, or expected
utility.

**Executed probe** (from `/home/joe/code/futon3c`):

```text
$ bb -e '(read b1-gamma-mission.edn; print keys, counts, and first cell)'
{:top-keys (:cells :events :provenance :summary),
 :cell-count 18, :event-count 46,
 :sample-cell ["E-interest-mining"
               {:policy-precision 1.0,
                :perf-history [-0.5], :mean-perf nil, :samples 1}],
 :summary {:events 46, :retro-docs 36, :live-events 1,
           :unattributed-events 11, :cells 18, :burned-in 1,
           :gamma-range [0.7071067811865476 1.0]}}
```

**Population verdict:** populated. If used as task belief, the act term would
mean “confidence in the selected policy” (or historical correction rate), not
“pragmatic value of acting now.” That would count the same precision twice:
once as the belief and again as `gamma-used`. It is not a legitimate source.

## 2. Prior `:zaif-arm-choice` decision evidence

**Location and shape.** The current decision is persisted by
`futon3c/src/futon3c/agents/zaif_controller.clj:185-257`. Its body contains
`:arm`, `:g-terms`, `:gamma-used`, `:round`, `:pairing-key`, and an
`:inputs-snapshot`; paired constants are emitted at
`futon3c/src/futon3c/agents/zai_api.clj:1126-1175`. It is queryable from the
read-only evidence text-search endpoint on `:7073` using `tags=zaif` (the
singular filter is not reliable).

**Executed probe:**

```text
$ curl -fsS \
  'http://127.0.0.1:7073/api/alpha/evidence/text-search?tags=zaif&limit=100'
$ bb -e '(read response; select :zaif-arm-choice records and snapshots)'
{:decisions 56, :nonempty-task-belief 0, :posting-stats-populated 56,
 :prior-choice-sample
 {:turn-id "zai-turn-a5e293f5-a9f5-4d1f-89e0-0f415d1ecaa9",
  :round 1,
  :pairing-key "zai-turn-a5e293f5-a9f5-4d1f-89e0-0f415d1ecaa9:r1",
  :arm :retrieve,
  :g-terms {:retrieve 0.7456643332946383, :act 0.0,
            :ask 0.15, :yield 0.0}}}
```

**Population verdict:** populated, but not a legitimate task-belief source.
The record is the controller's output and contains no subsequent outcome or
reward. Feeding its prior `:g-terms :act` back makes the next decision depend on
the previous decision's already gamma-scaled result; with today's records it
only recirculates zero. The meaning would be “what this controller previously
scored,” not a new belief about task progress. This is a self-referential loop.

## 3. The existing `:observations` channel

**Location and shape.** `estimate-posting-stats` at
`zaif_inputs.clj:153-167` converts the current context into
`{:total-docs n :dfs [...] :estimated-tokens n}`. The controller consumes that
shape exclusively as the retrieve arm's IDF-like information value and token
cost at `zaif_controller.clj:46-74`.

**Executed probe:** the live-evidence probe above found posting stats in all 56
decision records. One returned snapshot was:

```text
{:observations
 {:posting-stats {:total-docs 106,
                  :dfs [1 1 1 1 1 1 1 1 1 1],
                  :estimated-tokens 212}},
 :task-belief {}}
```

**Population verdict:** populated. If mapped into act value it would mean
“rarity and size of the prompt,” which is evidence for whether retrieval may
help, not expected pragmatic benefit from acting. Reusing it would make one
observation reward two different arms and is not legitimate.

## 4. Mission-grain records

There are three distinct typed facilities here; none is task action value.

### Mission registry

`futon2/src/futon2/aif/mission_registry.clj:294-304` returns the deliberately
small shape `{:open? boolean :open-hole-count long}` derived from the file-backed
mission registry.

```text
$ clojure -M -e '(require ...mission-registry)
                  (mission-status "M-zaif-harness-v1")'
{:open? true, :open-hole-count 1}
```

This is populated status/remaining-work evidence. As act value it would mean
“the mission is open or has holes,” not the expected utility of acting now.

### Mission completion-criterion preference (`C_mis`)

`futon2/src/futon2/aif/mission_c.clj:246-265` reads typed completion criteria;
lines 270-315 construct preference distributions and lines 317-370 score
current outcomes as preference surprisal/risk. This is a preference target and
needs a measured outcome; it is not an action-conditioned forward prediction.

```text
$ clojure -M -e '(read-criteria "holes/M-zaif-harness.md" ...; c-mis ...)'
{:version 1, :mission "M-zaif-harness-v1",
 :source "holes/M-zaif-harness.md", :shape :markdown,
 :status :absent, :reason :no-completion-criteria-section}
{:version 1, :mission "M-zaif-harness-v1",
 :criteria-status :absent, :criterion-count 0, :measurable-count 0,
 :weight-basis :uniform-over-measurable,
 :observable-of {}, :unmeasurable []}
```

**Population verdict for this mission:** typed absence. Even where populated,
using `-log C_mis(current outcome)` as positive act value would confuse present
distance from preference with predicted progress caused by an action.

### Maintained WM C-vector

`futon2/src/futon2/aif/c_vector.clj:1-18,196-220` defines a maintained,
read-only goal-outcome preference vector, and its risk path begins at lines
356 onward. Like `C_mis`, it says which outcomes are preferred and evaluates
observations against them. It supplies neither an action-conditioned outcome
distribution nor expected improvement for the ZAIF `:act` arm. Its appropriate
meaning remains mission preference/risk, not task belief.

The closest working computations found elsewhere reinforce the missing join:
`futon3c/src/futon3c/portfolio/policy.clj:49-74,155-180` computes pragmatic
value from a particular action, observations, beliefs, and adjacent missions;
`futon3c/src/futon3c/aif/mission_head.clj:135-176` similarly computes it from a
particular mission action and channel readings. Neither produces a persisted
ZAIF task-belief record, and copying either action vocabulary into the four-arm
controller would be a new model, not hydration.

## 5. What the v0 design meant

The design says the controller holds task belief and defines act as
“pragmatic value now” with rework risk (`holes/M-zaif-harness.md:51-62`). It
then names three separate precision ledgers: gamma for policy choice, C-channel
precision for operator intent, and **actand-indexed** precision for the world
model (`:104-112` and `:140-152`). Mission clocking supplies C, gamma, and
mission wants/open obligations as partial preferences (`:159-179`), but a
preference alone is not an expected action outcome.

Most directly, Z1 was meant to output initial gamma, C, and actand tables with
provenance (`:187-194`). The implemented D-1 checkpoint lists only gamma,
C-belief, and posting stats (`:1053-1076`). Its later replay finding records
that empty task belief makes act zero on all 114 calibration sessions
(`:1242-1249`). `p4ng/app-zaif.tex:3` likewise describes mission preferences
and open obligations entering as risk targets; it does not identify a stored
expected-utility observation.

## Search trail and typed finding

I searched the relevant `futon2/src`, `futon3c/src`, `futon3c/test`, the ZAIF
lab artifacts, `M-zaif-harness.md`, and `app-zaif.tex` for `task-belief`,
`act-value`, `pragmatic-value`, `expected-utility`, and `actand`. `actand`
appears only in the mission design/training narrative; no Clojure namespace,
EDN table, endpoint, or named query implements its promised view. The only
runtime producers of `:task-belief` are caller-supplied test fixtures and the
two empty maps in `zaif_inputs.clj`.

Typed finding: `:d8/task-belief-actand-source-absent` — the specified
world-model source for pragmatic action value has not been built or persisted.

## Recommendation for D8b

**Recommendation: none of the existing sources is semantically valid.** D8b
should make the absence explicit and typed at the hydration boundary, retain
the empty task-belief/default-zero behavior while absent, and pin both paths in
tests. It should accept and propagate a numeric task-belief only from a named,
provenance-bearing actand/world-model query result with the controller's
existing `:act-value` (or documented alias) shape. Creating that query and its
action/outcome semantics is prerequisite work, not a license for D8b to invent
a value from gamma, prompt statistics, mission status, C-vector risk, or a
previous decision.
