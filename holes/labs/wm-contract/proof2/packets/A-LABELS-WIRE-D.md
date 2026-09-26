# A-LABELS-WIRE-D — production token checks, label storage, and rate consumers

Discovery by codex-1, 2026-09-26, for claude-8. Futon2 code pin:
`e66699e1a616d707697b572fbfd7b766105bbaf1`. All futon2 file:line references below
are at that pin (src/scripts remained unchanged during the survey). Companion
futon3c checkout: `e323d609790ab9f4ecfde874bd6d1fe8cde2214c`.
Read-only source/artifact inspection; no tests, flight, click, reload, or map edit.
Only this note is committed. Recommendations below are proposed work, not existing behavior.

## 1. Where checks execute, and what survives

Paths below are relative to futon2; `WM` abbreviates
`scripts/futon2/report/war_machine.clj`, and `src/` below means `src/futon2/aif/`.
The observation entry point is `src/observation_checks.clj:557` (`observe*`),
with class dispatch at :548–550: `{:C3 check-path-exists :C4 check-decl-in-file …}`.
`observe` returns `{:observed #{token …} :results {token result} :refused {token refusal}}`.
Individual checks return `{:check :C3|:C4 :observed boolean :evidence …}`, or a
refusal. The token is normally an OUTER map key, not a field of that result.

| Site and one-line call quote | Reachability / population / result |
|---|---|
| `src/cascade_sources.clj:223`: `(oc/observe located)` | Production selection. `WM:7383` calls `(cascade-sources/load-declared …)`; loader :275 calls `observe-facts`. Checks only declaration `:facts`, looked up in its `:locators`, not automatically every want or every locator. Keeps token-keyed `:results` and `:refused`, converts to true/false/:unknown `:universe` (:219–229). Loader stores them per target (:310), per read occurrence (:276), and in token-initialization inputs (:282–284). |
| `src/flight.clj:78`: `(checks/observe {::t %})` | Production flight planning/source refresh for `:a-exits`, via `source-wants` :48. Checks machine-located criteria. Temporary `::t` is discarded into a boolean, then associated with the actual criterion token in :88. Full result is lost. |
| `src/mission_criteria.clj:130`: `(checks/check-decl-in-file %)` | Production `:a-exits` caller is `flight.clj:70`, `(criteria/wants cs …)`. Checks mechanically located mission criteria; :138 retains `{token boolean}` and locators, not the individual result. |
| `src/flight_runner.clj:84`: `(observe locators)`; default :81 `(observe-fn checks/observe)` | Production flight before/after want observations; `flight_driver.clj:259` supplies it, `flight.clj:498,522` calls it before/after the click/enactment. :85 converts `:results` to token booleans, merges refused tokens as unknown. Token survives, detailed result does not. |
| `src/mission_reading.clj:204`: `(observe {token locator})` | Production conditional locator-reading/validation, default `checks/observe` :179; `flight_runner.clj:630` calls `(reading/validate-locator …)`. Checks a newly supplied criterion locator. :207 returns valid locator and boolean; :269 records `:observed-at-validation`. Token is on the issued request/reading; full check evidence is discarded. |
| `src/fold_cascade.clj:58`: `(observation/observe (or locators {}))` | Production cascade realization, `full_loop_runner.clj:1703` `(fold-cascade/realize construction)`. Locators are the selected action's `:observation-locators` (:57). Full map is local; each relevant guard's witness retains `{:token … :observation result}` (:32–46). This is an enactment/guard read, not a test-only simulator. |
| `src/accepted_increment.clj:30–31`: `(checks/check-path-exists locator)` / `(checks/check-decl-in-file locator)` | Production close, `full_loop_runner.clj:4359` `(accepted-increment/evaluate-close …)`. Checks the selected step's declared produced tokens through their after-locators, plus target acceptance. :71–81 builds token-keyed `b-results` and single `c-result`. Adapter :146–153 takes `[:measurement :after-locator]`; it does not invent a locator from the verdict. |
| `src/flight_runner.clj:819–820`: `(if-let [f (get checks/checks (:class check))] (f check) …)` | Additional production enactment site: the dispatched seat's check, not necessarily the entire cascade universe. :758 `(check-fn check)` is retained as `:check :result`; :774–775 carries `:produced`, `:check`, and `:success` on each step attempt. Token attribution is in `:produced`/the attempt, not necessarily the result itself. `flight.clj:244` consumes these boolean observations. |
| `src/d_predecessor_task_authority.clj:79`: `((if (= class :C3) observation/check-path-exists observation/check-decl-in-file) after-locator)` | Production D-task artifact evidence and verification. Enumerates qualified `[target token]` in dispatch universe, checks only C3/C4 in the artifact repository at the claimed after commit; others are typed unavailable (:66–85). Rows preserve `:token`, original and after locators, and `:result`. Runner captures/completes this at :5330 / :4253. |
| same file :325–326: `(observation/check-path-exists locator)` / `(observation/check-decl-in-file locator)` | Conditional production predecessor reader, via `token_belief_predecessor.clj:92` `(task/read-observations-v2 …)`. `signed-observations` :371 separately rechecks a declared historical revision, retaining `:declared-revision-observation :result`. Artifact and historical propositions are explicitly different (:357–374). |
| `src/held_out_observations.clj:277`: `(checks/decl-present? (render-packet packet) (str disposition))` | Held-out snapshot writer's rendering guard; NOT a C4 git check or located token observation. No `:check/:evidence` result to label. Do not turn it into training labels. |
| `src/held_out_calibration.clj:95`: `(checks/observe {token (assoc locator :sha artifact-sha)})` | Additional held-out calibration site found by caller census. Returns a realised boolean or refusal, not a production tick label stream (:95–99). Keep held-out data out of training. |
| `src/repair_recheck.clj:36`: `(checks/decl-present? text (str calibration-disposition))` | Dated repair/calibration artifact writer (:56); also validates rendered output at :62. Not a call to `check-decl-in-file`; no single C4 result. No runner caller found. |
| `src/repair_discharge_receipt.clj:78`: `:c3-locator {:class :C3 :repo repo-name :sha commit :path relative}` | This is a locator PRODUCER, not a C3/C4 checker. Runner calls its `catch-up!` at :5888, but `published` :73–78 only constructs the receipt locator. `publish!` :116–118 commits/readbacks a receipt; it does not call `observation/check-path-exists`. |
| `scripts/wm04/evidence_run.clj:30`: `(oc/observe tokens)` | Diagnostic evidence script, not reached from the production runner in the caller census. |

`flight_click` is a diagram/test name, not a futon2 namespace file. The actual
HTTP flight adapter is `flight_runner/http-click-fn` (:494), which posts
`:flight-edn` (:527) and reads `tick-run-record-<run-id>.edn` (:520).
`flight_driver.clj:255` supplies that adapter to the flight.

One further boundary: `WM:7069` names R2 as `futon2.aif.observation/observe`.
That is the general channel observation, NOT `observation-checks/observe`.
Token fact reads happen in source assembly later. Also
`src/mission_hole_wants.clj:85` says `:universe (zipmap tokens (repeat false))`:
the fallback mission wants are not all newly checked by `load-declared`.
Do not report those booleans as C3/C4 verdicts without actual check receipts.

## 2. Which site should produce labels?

**Start with the declared-source fact observations, at the production owner of
`load-declared`'s result.** They are the check outputs used to establish the
current token facts. Read their existing receipts; do not run a second CHECK
and attach its newer verdict to the first result. The writer's independent
recomputation must use each receipt's already-resolved commit.

The rate population is a class's individual observation checks, not successful
acceptance predicates. Therefore the raw C3/C4 produced-token/acceptance checks
at close are also eligible samples in principle, including false results;
`:accepted?` itself is never an admitted observation or reference. These sites
differ in scope (facts versus selected products/acceptance) and often in
resolved revision (selection revision versus after-artifact revision). With
identical class/repo/resolved-sha/path/decl/mechanism, they are the SAME key and
must add only once (`observation_admission.clj:168`, `(defn label-key …)`).
Changed revision or declaration is a different subject. Token aliases and
repeated ticks alone are not new subjects.

Recommendation for the smallest first wire: collect declaration-read receipts
only, explicitly record that scope, and add the close stream in a separate
follow-up if required. Do not claim whole-tick coverage: flight want reads,
mission locator validation, and fallback wants have the losses noted above.
The accepted-increment success/failure branches retain different subsets of
results (:85–136), so mining only successful closes would select the sample by
outcome and miss checks. A future close collector must capture raw `b-results`
and `c-result` before that conditional, not scrape `:accepted?`.

Adapter: `(map (fn [[token r]] (assoc r :token token)) (:results observations))`,
restricted to C3/C4; carry target/read occurrence beside the write receipt.
Do not rewrite `:evidence` or replace its resolved SHA with HEAD.

## 3. Existing records and a concrete live artifact

`WM:7644–7647`: `{:declared-files … :observations (:observations declared-sources)
:read-occurrences (:read-occurrences declared-sources) …}` retains receipts in
the judgement's `:cascade-sources` (injected sources instead record
`:supplied-by-caller`, so that branch is not proof of a real read).

The durable tick record is stronger evidence: `full_loop_runner.clj:5893`
binds `cascade-sources/*read-occurrences*` to a run-owned atom, and :705 writes
`:declaration-reads (cascade-sources/provenance …)`. `cascade_sources.clj:34–38`
puts the accumulated reads under `:occurrences`. The exact durable path is
`[:declaration-reads :occurrences i :observations :results token]`.

Inspected futon3c artifact:
`holes/labs/M-wm-wiring/spike/tick-run-record-2026-09-26-flight-7f89646a-click-1.edn:1`
(single-line EDN). Recursive inspection found 28 C4 result maps. Example:

```clojure
;; [:declaration-reads :occurrences 0 :observations :results :hole/h42fceb4ad48b]
{:check :C4 :observed false
 :evidence {:repo "futon2" :sha "HEAD"
            :resolved-sha "564f32ab2c07fee7e17a6ab39f1ee396cd87b43e"
            :path "holes/missions/M-aif-policy-conditioned-eig.md"
            :decl "- [x] **Collect a default-off EIG shadow and calibration packet.** Prior"
            :file-present true}}
```

Other matching spike records: `flight-ada87008/tick-run-record-2026-09-26-flight-ada87008-click-1.edn`,
and the `e70b4baf`, `ffcd772b`, `278b6988` tick records. Grep establishes presence;
only the example above was recursively enumerated.

Enactment checks survive in flight enactment records:
`flight_runner.clj:774` `:check chk`, :906–910 writes the record;
`flight_driver.clj:229` supplies `<store>/flights/enactments`.
Accepted-increment checks survive conditionally under its `:evidence
:produced-token-results`, `:all-results`, or `:acceptance-result`
(`accepted_increment.clj:85–136`); runner retains the verdict in close judgement
(:4417, :4431). Do not assert a uniform top-level tick-record field for them.

Important record gap: `full_loop_runner.clj:725–727` persists only
`[:selection-law :selection-certificate :initial-belief-receipt :enumeration-completeness]`
from the decision. `:measured-a` and arbitrary new top-level decision fields
are not automatically in the tick-run-record. By contrast `trace.clj:137–158`
keeps the whole cascade decision (only posterior keys are transformed), and
:464 writes it. Any new exclusions/write-receipt field needs an explicit durable
record assertion, not merely an assertion on the in-memory judgement.

## 4. Code identities: recommendation and prerequisite

`load_identity.clj:103–122`: `(defn register! …)` captures namespace, source URL,
canonical path, capture time, status and `:sha256` of source-file bytes read at
load time. It is NOT a git commit and NOT a bytecode hash. The namespace warning
(:1–4) excludes edits during compiler read, partial loads, load-string and later
Var mutations. :124 `(defn check …)` compares captured bytes with disk, reporting
`:unregistered/:unavailable/:current/:stale`; :135 `report` includes required names.
Neither `observation_checks.clj` nor `observation_labels.clj` calls `register!`,
and neither appears in `required-sources` at this pin.

The git alternative, probed read-only, gives:

- checks file last-touch commit: `367be490f8e4a7ba8a1569f29c1652ea7bbd9d5d`;
- labels file last-touch commit: `78b767abeb6c412ff5a2dc76243f64feec0bd8c5`.

`git log -1 --format=%H -- <file>` is stable across unrelated commits, but says
nothing about which version the serving JVM loaded, and ignores uncommitted
source changes. A tick-time invocation alone is insufficient evidence.

**Recommend captured load-identity SHA256s**, with both namespaces explicitly
registered and included in required-source checks. The runner already obtains
`source-check (refuse-on-runner-source-drift!)` at :5887 and passes it as
`:loaded-code-identity` (:5896); the selection adapter forwards that key at
:4832. Read the two checked entries from this one run's captured report, once;
pass their digests as `:mechanism-sha` / `:code-sha`. Missing/stale identities
must yield a typed refusal, never fall back to current HEAD.

This recommendation requires an explicit contract adjustment: the accepted
writer's `*code-sha*` doc currently says **commit** (`observation_labels.clj:13`),
although the implementation accepts a string and Revision 3 calls for code
identity. Do not silently relabel a file digest as a commit. The wiring packet
must authorize/document source-digest identities (e.g. `sha256:<hex>`) or choose
verified load-bound git commit identities instead. This is a prerequisite,
not an implemented identity source.

## 5. Persistent store and in-flight behavior

Existing conventions:

- `cascade_habit_store.clj:15–16`: `data/wm-habit/cascade-prior.edn`; :33 reads a
  snapshot, :143–155 publishes via fsync/temp/atomic replace, :160–164 holds both
  a JVM monitor and file lock across read/update. This is the closest model for
  cross-tick accumulated labels.
- `trace.clj:63–64,87`: `data/wm-trace/wm-trace-YYYY-MM-DD.edn`; :646 writes traces,
  :684 reads them. `WM:7122` `(peek recent-trace-records)` supplies prior state.
- `full_loop_runner.clj:93,680`: `data/wm-runs/tick-run-record-<run-id>.edn`;
  :775–780 uses a temporary file and atomic replace. These are run receipts,
  not a shared mutable label population.
- `want_interpretation.clj:307–311`: `data/wm-interpretations`; flight_driver
  :237–239 writes `<store>/flights/<flight-id>.edn`, with enactments beside it.
- `d_predecessor_task_authority.clj:26`: `data/wm-d-task-enactment` stores
  occurrence evidence (:244–254). No evidence found that `data/wm-step` is the
  current authority for these cross-tick consumers.

Propose a configured explicit path `data/wm-observation-labels/labels.edn`,
resolved by the runner to an absolute canonical path and passed to the writer
owner (the low-level API still has no default). `.gitignore:67`, `data/*`, covers
this location. It is runtime data: write during a flight without git commits;
do not edit/reload runner source during the flight. The node packet must be
landed before a subsequent flight starts.

Persistence prerequisites: `observation_labels.clj:149–159` currently uses
plain `spit`, and `load` :161 refuses missing/corrupt files rather than silently
creating a population. The production owner needs explicit first-store
initialization, locked read/merge/write and atomic publication; errors must be
recorded, never replaced by an empty successful store. The habit store supplies
an existing implementation pattern, not permission to bypass durability.

**Subjects-count gap:** the present store contains only admitted records
(:137–140). Revision 3 asks for distinct located subjects *seen*, including
those whose recomputation refused. That number cannot be reconstructed from
admitted labels alone. Retain a separate seen-subject index/receipt, using the
same locator identity and active check mechanism, or extend the store envelope
explicitly. Keep `write-labels`' accepted map contract unless the packet
expressly changes it. A refused subject must count as seen without becoming a
label. Publish labels plus that index consistently.

## 6. One reader result, two consumers, and exclusions

Current calls:

- `WM:5915–5917`: `(observation-rates/sourced-rates labels (or (:subjects observation-labels) {}) nil locators …)`;
- `WM:6350–6352`: `(observation-rates/sourced-rates (vec labels) (or subjects {}) nil locators contract)`;
- `WM:6470`: `(cascade-lane (:cascade-problem problem))` — no labels supplied;
- `WM:6859`: `(measured-a-version problems (:observation-labels opts))`.

Both consumers hard-code **nil prior**. Adding `:prior` to opts without changing
these calls would not apply Jeffreys. `observation_rates.clj:62–73` accepts exact
positive integer/ratio alpha and beta and a nonblank authority; the stipulated
`{:alpha 1/2 :beta 1/2 :authority "A-S §2 (Jeffreys), Revision 3"}` meets that
rule. :50–60 computes the posterior mean; `sourced-rates` :245 passes prior to
`rates-by-class`. Unobserved cells remain unobserved, even with a prior.

Proposed placement: read ONE immutable store snapshot at selection entry,
before assembly/construction can invoke scoring. Build the class population
under the active check mechanism identities; never pool obsolete mechanism
populations. Count distinct seen subjects (not tick checks), count admitted
present/absent labels separately, and retain a class's labels only if BOTH
counts >=5. Zero cell -> `:one-cell-unobserved`; otherwise a cell <5 ->
`:below-minimum`. Return raw admitted records, subjects, explicit prior, typed
exclusions and snapshot/code identity. Do not fabricate absent observations.
A class with no labels stays wholly unobserved at the kernel, not one-sided.

Build the per-target view once from that snapshot and pass the very same value
to lane opts and `measured-a-version`. These are class-conditioned estimates:
different targets using a class should not independently estimate it from
repeated aliases of the same evidence. Target views may select relevant classes
but must share the same population and prior. At `WM:7437` add the map to
`select-and-record-cascade!` opts; :6470 must forward its target's view, and both
sourcing calls must pass its prior. `full_loop_runner.clj:4832–4833` explicitly
selects forwarded opts: any runner-owned path/snapshot option must be added
there, not merely supplied upstream.

There is a THIRD scoring reachability concern: constructor evaluation calls
`cascade-lane` at `WM:6113`, through R5, before joint selection. If its rates
must match the lane/record, supply the same snapshot during assembly as well;
otherwise document the inconsistency rather than claiming the tick used one A.
The joint decision itself still scores a class model; attaching token measured-A
does not by itself change that model. This packet must not claim otherwise.

Recommend `[:decision :selection-certificate :observation-labels]` for the
snapshot identity, active mechanisms and per-target exclusions/counts. That
subtree survives BOTH trace persistence and the tick-run-record's select-keys.
The actual `:measured-a` record must also be deliberately retained in the desired
run-record field (gap in §3). Writer outcomes need a separate run receipt with
written/skipped/refused keys and seen-subject counts. Fix the timing explicitly:
consume the entry snapshot for this selection and publish this tick's new
labels for the NEXT selection; do not let a partial read/write change A midway
through a decision or constructor search.

`futon3c/test/futon3c/diagramprover/wm_wire_rates_support.clj:25–46` constructs
`[[:present true] [:absent false]]` and calls the lane with `:subjects {:C3 2}`.
The Revision 3 reader excludes that class as `:below-minimum` (1/1); it must not
remain a measured-rate positive fixture. Keep it as a negative minimum test.
For the positive wire, create at least FIVE distinct located subjects in EACH
cell under one mechanism (ten unique label keys, not repeated copies), pass
through store -> real reader -> both consumers, assert the actual prior and
matching sourced rates. With zero errors and n=5, each Jeffreys mean is 1/12,
not the old raw 0/1. Preserve tamper tests at both ends of the wire.

## 7. Proposed map changes (for the node packet only)

- Existing declaration-observation producer: expose target, token-keyed C3/C4
  `:results`, refusals and resolved evidence; distinguish it from general R2 channels.
- Writer box for `observation-labels/write-labels`: reads check results, existing
  label map, check/writer code identities; writes label keys/records and written,
  skipped, refused receipts. C3/C4 recomputation and mechanical review are real
  dependencies, not arbitrary author claims.
- Explicit persisted-store/seen-subject owner: reads snapshot/path, publishes
  labels and seen identities atomically; writes snapshot and publication receipt.
- Reader box: reads pinned store, seen-subject index, active mechanisms and class
  relevance; writes `{:labels :subjects :prior :excluded …}` and snapshot identity.
- `:r6-cascade-lane`: add `:observation-labels` input (including prior); wire to
  `:r6-sourced-rates`, and retain measurement/exclusions provenance.
- `:r9-measured-a-version`: wire the SAME target value and prior, retain measured-A
  plus exclusions/snapshot; `:r9-decision` distributes that value, not an independent read.
- Constructor/evaluate-G caller: represent its use of the same snapshot if wired;
  keep the separate joint class observation scorer explicit.
- Tick-record and trace writers: expose the retained selection-certificate
  receipt; do not infer persistence merely from an in-memory decision field.

## Smallest implementation packets and tests

**A-LABELS-WIRE-I — persist labels from actual declared-source reads.** One
behavior: a completed production declaration read supplies its exact C3/C4
verdicts to the accepted writer and durably accumulates the population for later
selections. Include explicit identities, configured path, seen-subject tracking,
atomic/locked ownership and durable written/skipped/refused receipt. No rate
consumption yet. Acceptance: a hermetic production-source read at a real pinned
repo, two tick-boundary invocations, disk reload, one label per stable key and
both verdicts intact; pin the declaration-read receipt shape shown in §3.
Bad cases: self-truthed recomputation stores no label but retains seen subject;
changed check identity creates a separate population; concurrent updates or
failed publication cannot report success or lose the prior snapshot. Code
identity registration/representation must be settled as part of this packet,
not deferred behind a HEAD fallback. Keep close-product collection a separate
follow-up, with all raw results before acceptance branching, if authorized.

**A-LABELS-READ-I — one admitted population supplies token A and its record.**
One behavior: one entry snapshot is filtered by Revision 3 and supplies the
same labels/subjects/Jeffreys prior to lane scoring and measured-A recording
(and the constructor scoring invocation). Acceptance: >=5 distinct subjects per
cell, real reader and sourced-rates, exact matching rates/digest/provenance in
both consumers and the persisted tick record. Assert snapshot stability despite
writes for the next tick. Bad cases: 4/5 or 0/5 excludes the entire class with the
correct typed reason; duplicated subjects and old-mechanism labels cannot meet
the minimum; different snapshots/prior at the two consumers fails the wire.
Use a temp store and hermetic tick/selection dependencies, never a live click.

Unfixed findings belonging to the wider wiring contract: lost flight boolean
receipts; fallback mission wants are initialized false rather than checked;
accepted-increment branch-dependent result retention; no loaded identities for
the two observation namespaces; admitted-only store cannot count all seen
subjects; plain-spit persistence is not a concurrent production update;
constructor's extra scoring call; nil prior at both consumers; top-level
measured-A omitted by tick-run-record projection. None is repaired by this note.
