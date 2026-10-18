# Review 2: inner selection and canonical-cascade identity

Reviewer: Claude (subagent), 2026-10-18. Reviewed at futon2 HEAD `0975c7df7`.
Read only: no tests run, no JVM queried. Every finding below comes from reading
the code path, and each one says how far the reading went.

## Findings

### F1. A held cascade's D4 retry recompiles with the same empty authoring, so a "successful" resume is still open (blocks a tick at the first live click that reaches cascade execution)

- `src/futon2/aif/wm/cascade_decision.clj:1468-1477` `seam-authoring`
  returns `(:cascade-operational-authoring opts)`, or else an empty attributed
  authoring (`:clauses {}`). Nothing in `src/` or `scripts/` sets
  `:cascade-operational-authoring` (grep: the only hits are inside
  cascade_decision.clj). So in production every selected cascade compiles with
  every occurrence as an open hole. That leads to
  `:wm/open-cascade-execution-error`, and then
  `full_loop_runtime/open-cascade-resume-handler`.
- The handler asks the Zai author to "implement resolutions ... commit the
  source", has the reviewer accept it, and hot-loads the patch
  (`open_cascade_resume.clj:257-280`). The patch's `product` goes only into
  the resume ledger (`:attempts ... :product`). It is never passed to the
  retry.
- The retry closure (`cascade_decision.clj:1670-1680`) is
  `(fn [_] (execute-selected-cascade! entry opts))`. It ignores its context
  and uses the same `opts`, so it recompiles with the same empty authoring and
  gets the same holes and the same compiled digest `cd`.
  `resume!` (`open_cascade_resume.clj:296-306`) accepts any result whose
  `:cascade-digest` equals the held one, so it returns `:resumed` with a
  result that is still `:cascade-enactment-open`. The runner
  (`full_loop_runner.clj:6511-6521`) takes `(:result handled)`, so `entry`
  is nil and the click closes `:incomplete`.
- Scenario: the next click that selects a co-apply cascade will spend
  author and reviewer jobs and then close with no tick. The tests do not
  catch this because they stub the retry (`open_cascade_resume_test.clj`
  `held-error {:retry ...}`), and `cascade_runner_seam_test`
  `authored-frontier-routes-into-the-existing-full-loop` supplies
  `:cascade-operational-authoring` directly, which production never does.
  The recorded flight hides it as well: `recorded_flight.clj:127-141`
  compiles from the manifest's offline `:operational-authoring` and never
  calls `execute-selected-cascade!`.
- Severity: blocks-next-click (from carrying a tick). Confidence: confirmed
  by reading, apart from one alternative I cannot rule out: a patch that
  redefines `cascade-decision` itself would be picked up because the retry
  resolves the var at call time. That would mean a Zai seat hot-loading WM
  code, and no packet says that is intended. This finding is downstream of my
  area (execution seam), but it is the place where the selected cascade is
  consumed.

### F2. The retained-record path holds in production; the construction fallback is unreachable on the live path but still present (minor)

- Production sets `:require-occurrence-squares? true`
  (`full_loop_runtime.clj:256`, passed on at `:151`). That makes
  `reading-family-decision` supply `:bind-cascade-identity`
  (`cascade_decision.clj:1258-1263`), so `bound` is non-nil in
  `family_selection.clj:276`. The selected entry is found by digest and its
  record is attached at `[:selection-certificate :selected-canonical-cascade]`
  (`:346-358`). `selected-entry` copies that onto the entry
  (`full_loop_runner.clj:2171`). `controller-authority/authorize`,
  `decision-gate/emit!` and `selected-reading-phase` all keep the selection
  certificate. Redecision recomputes the whole decision, retained record
  included. The D4 retry reuses the same `entry`.
- So the fallback at `cascade_decision.clj:1568-1574` is reached only when
  `:canonical-cascade` is nil. On the live path that needs `bound` nil
  (not the case in production) or no menu entry matching the selected digest
  (in which case `verify-selected-identity` has already refused). No live
  path reaches it.
- What is left is the second copy itself. The fallback rebuilds from
  `{:action :policy-id (:id action) :target-id target}`, and here `target`
  is `(:target entry)`, which is **always nil** because `selected-entry`
  never sets `:target` (`full_loop_runner.clj:2160-2186`). So
  `occurrence-readings` writes `reading-record-id` as `"?/<id>/<oid>"`,
  where the pre-selector bind wrote `"<target-id>/..."`
  (`cascade_decision.clj:1453`). That difference alone changes the digest.
  It may be the real mechanism behind the click-2 `:identity-forged`; the TN
  says only "a smaller receipt map". The fallback also passes
  `:cascade-operational-authoring` (compiler shape: `:clauses`,
  `:application-order`) into `bind-entry`, which expects the identity shape
  (`:operational-clauses`, `:application-order`, `:guards`, `:holes`;
  `selection_cascade_identity.clj:43-56`). These are two authoring schemas
  under one option name.
- The same `target` being nil also puts `:target nil` into every
  `fail`/open-cascade carrier from the seam (`cascade_decision.clj:1505`,
  `:1530`, `:1689`), so a held cascade does not name its target.
- Test adequacy: `selected-canonical-record-is-not-rebuilt-after-selection`
  (`cascade_runner_seam_test.clj:280-301`) asserts only
  `(string? (:selected-cascade-digest held))`. It does not assert that the
  held digest equals the decision's `[:action :cascade-digest]`. It also
  feeds both binding sides stubbed readings rather than the production
  `occurrence-readings`. Either way the bad case (a rebuild) would still be
  caught, because the rebuild refuses and the phase is never held.
- Severity: minor (fallback), likely-later (`:target nil` in held carriers).
  Confidence: confirmed by reading.

### F3. Other places that rebuild after selection (minor)

- `execute-selected-cascade!` rebuilds the structural layer as a "preview"
  from the seam's own readings before it looks at the retained record
  (`cascade_decision.clj:1516-1532`). If that preview has no nodes, the seam
  returns `:cascade-enactment-open` with `:kind :cascade-enactment-open`
  (not `:wm/open-cascade-execution-error`, and with no `:resume-port`), even
  when a certified retained record exists. In production both sides use the
  same `occurrence-readings` over the same action and pins, so I found no
  input that makes them disagree. It is still a second construction kept in
  front of the retained record.
- Save, reload and compile: the seam compiles `(:structural reloaded)`
  (EDN round trip through `save!`/`load-record`), while the recorded flight
  compiles `(:structural canonical)` in memory. So the round trip is
  exercised only in the seam tests, never on the replayed click-5 data.
  `save!` (`executable_cascade.clj:385-400`) also throws an untyped
  `ex-info` when a file with the same digest exists but its `pr-str`
  differs. `value-digest`'s `stable` sorts maps but not sets or lists, so
  the same value printed in a different order collides instead of matching.
  Plausible, not seen live.

### F4. The class-emission term is a real model, but its relevance coordinate is constant by construction, and the launch gate checks only its shape (likely-later)

Commit 48e8ae2d0. What the model does: `class-of-state`
(`observation_model.clj:225-243`) emits `:focused` at the horizon when the
state holds any of the target's terminal `[target :pattern-done p]` tokens,
and `:stop-the-line` otherwise. Because `target-class` is always
`{target :focused}` (`cascade_shape_g.clj:38-58`), `:related` and
`:unrelated` can never be predicted. The 55/35/5/5 table therefore collapses
to one Bernoulli on p = P(some terminal reached), with risk running from
ln(1/0.55)=0.60 at p=1 to ln(1/0.05)=3.00 at p=0. That is a real function of
the rollout, so the term is not vacuous. But it is a monotone terminal bonus
over a quantity the progress channel already scores (completed count and
`want-met?`). It adds no relevance information.

Bad cases I worked through:

- (a) Two same-target policies with equal terminal-reach probability get
  identical class risk, so the term cannot change their ranking.
- (b) The per-click CTAU gate (`loop_node_exercise.clj:34-37,97-109`) passes
  any candidate whose `:class-emission` has `:model :kind :class-emission`,
  `:consumed-in-g? true`, a numeric `:risk` and a map
  `:class-preference`. It does not check that G contains the term.
  Consumption is actually enforced elsewhere: the preference receipt's
  composition check (`reading_policy_preference.clj:222-243`,
  risk = progress + class) and the readiness probe's arithmetic on a
  synthetic one-node fixture. Neither recomputes the class risk from the
  model, so the class risk value itself is taken from the scorer as given.
- (c) The normalisation is uneven across horizons. Class risk is divided by
  `H * ln(max 2 |C_1|)` (`cascade_observation_scoring.clj:180-184`), and
  `C_1` is the tau=1 row. For H=1 that row is the 4-class terminal table
  (divisor ln 4). For H≥2 it is `{:ending/not-yet-evaluated 1}` (divisor
  H ln 2). A cascade that never reaches its terminal pays 2.16 at H=1 or 2,
  but 0.43 at H=10, so the class term favours longer policies for the same
  failure. The spec's `perStepCapacityAndPattern` says
  "log |outcomes|", and the class outcome space is 4–5 classes, not 2.
  Confidence: confirmed by reading.

### F5. The certificates and Requirements carriers now misdescribe what G consumed (likely-later)

- **Q7 (Requirements.lean:316).** `policy.clj:295` now copies
  `:class-emission` (including its scorer `:certificate`) onto every selection
  candidate row. That certificate's `:f` is the class model's `:condition`
  result, which is always
  `{:f {:value 0.0 :status :not-supplied :reason :class-model-unconditioned-at-selection}}`
  (`observation_model.clj:318-325`, `cascade_observation_scoring.clj:240`).
  `scripts/wm_run_facts.clj:282-316` `absence-paths` walks the selected
  candidate recursively and counts any map whose `:status` is
  `:not-supplied`. So every selected run since 48e8ae2d0 gets at least one
  new Q7 path absence at
  `[:decision :selected-candidate :class-emission :certificate :f :f]`, and
  Q7 is violated on a run that would otherwise conform. (`q7-candidate`
  dissocs only `:computed-f`.) The progress model's matching `:not-supplied`
  never reached the candidate row; the class one does. Confidence: confirmed
  by reading, exporter not run.
- **One C.** `reading_policy_preference/validate` refuses
  `:competing-preferences`, and its own docstring
  (`reading_policy_preference.clj:50-66`) says "a `:class-preference` on the
  model is a second, competing shape". It looks for that only in
  `(meta (:scorer-result record))`, though. 48e8ae2d0 puts the class model at
  `[:class-emission :model]`, so the receipt certifies one C while G
  consumes two: the progress preference and the 55/35/5/5 table. The check
  is still there, but the second C sits at a key it does not read.
- **Q9.** `q9_factorization.clj` (docstring lines 9-14 and `policy-c`) still
  says "The C that selects on this route is cascade-shape-g's progress
  preference" and records `:no-relevance-coordinate`. The selecting G now
  also consumes a class table that does have relevance coordinates. Q9 stays
  false either way ("A flat four-way probability table is not a Q9 witness",
  Requirements.lean:371), so the verdict is right but its recorded reason is
  stale.
- **Precision identity.** `precision-config` (`cascade_decision.clj:1062-1105`)
  binds the progress preference authority `"... progress-preference v1"` and
  the scorer version. The class preference values reach the identity only
  through `scorer-version`. A change to 55/35/5/5 without a version bump
  would carry beta silently. Minor.

### F6. Beta identity changes almost every click; campaign slots are spent even when the carry would succeed, and the "closed" campaign is re-armed from disk (likely-later, plus an authority problem)

- The identity (`reading_policy_precision.clj` `identity-map`) covers the
  graph pin, the reading digests of every computed target, and every ranked
  policy's arrangement. Any newly published reading or mined-graph refresh
  changes it, so beta carries across clicks only when nothing upstream has
  moved. In practice each click needs a declaration.
- `certify-reading-family` (`cascade_decision.clj:1133-1134`) evaluates
  `(precision/declaration-for identity)` **before** `carry-beta`, on every
  call. `declaration-for` (`reading_policy_precision.clj:279-298`) issues a
  campaign declaration and spends one `:identity-limit` slot even when the
  previous receipt would have carried, or when there is no previous receipt
  (`:initialized`). So debugger retries and the redecision pass each spend a
  slot whenever the identity has moved.
- The campaign state lives in a `defonce` atom. `ensure-reading-precision-campaign!`
  (`full_loop_runtime.clj:35-43`) re-registers
  `resources/wm/reading-precision-beta/wm-readiness-2026-10-17-campaign.edn`
  whenever the in-memory sha differs, which is always true after a JVM
  restart, and `register-campaign!` resets `:issued {}`. The TN (lines
  403-405) says the campaign is closed and that another live run needs a new
  authorisation, but the code still installs that file as the default on
  every production click.
- Will the next click stop on beta? On the current JVM, probably not: the TN
  records three identities remaining after click 3, and clicks 4 and 5
  abstained before construction. After a restart it gets four fresh slots.
  It stops (as `:model-contract-refused`) only once the in-memory count
  reaches 4. So the next click will not stop on beta, but it will run on an
  authorisation Joe has been told is closed.
- Confidence: confirmed by reading. I did not read the live count (that
  would need a JVM eval, which is outside the read-only rules).

## What I checked / did not check

Checked: `family_selection.clj` (all), `selection_cascade_identity.clj` (all),
`cascade_decision.clj` 1060-1420 and the D3 seam 1420-1716,
`full_loop_runner.clj` `selected-entry` and the seam call site,
`full_loop_runtime.clj` composition, `recorded_flight.clj` selection→compile,
`cascade_shape_g.clj` scoring plus the 48e8ae2d0 diff,
`cascade_observation_scoring.clj` and `observation_model.clj` class paths,
`reading_policy_preference.clj` validate/normalisation,
`reading_policy_precision.clj` (all), `loop_node_exercise.clj` receipt,
`q9_factorization.clj`, the `wm_run_facts.clj` Q7 walker, the Requirements.lean
Q1–Q10 definitions, the 6b9f02660 and 0975c7df7 test diffs, and
`open_cascade_resume.clj`.

Not checked: `three_halves_*.clj`, `target_policy_family.clj`,
`policy_prefix_admission.clj` and `decision_gate.clj` internals (skimmed
`emit!` only); Q4/Q8/Q2 carrier code; whether Q7 gates anything at click
time rather than only in the exporter. No test was run, so F5-Q7 has not been
confirmed by execution.

## Readiness verdict for my area

Selection itself (G scoring, the one selector call, canonical binding, and the
retained record reaching the seam) looks sound on the live path. The click-2
identity defect is fixed for production, and I found no live input that
reaches the old rebuild. The next predictable stop comes just after selection
(F1): production has no source of operational authoring, and the D4 resume
discards the reviewed patch's product, so a click that selects a co-apply
cascade will spend author and reviewer work and close `:incomplete`. The
recorded flight did not hit this because it compiled with offline authoring
and bypassed `execute-selected-cascade!`. Beta will not stop the next click,
but it will run under the campaign the TN calls closed (F6). The class-emission
change (F4/F5) does not block a click. It does add a Q7 absence to every
selected record and leaves the one-C and Q9 certificates describing a G that
is no longer the one being computed.
