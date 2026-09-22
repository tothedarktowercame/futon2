# Repairs become ordinary tickets — discovery, 2026-09-22

Base: futon2 `b188bcc2`, branch `fix/repairs-as-tickets`. Source references below
are relative to that revision; unqualified Clojure filenames mean
`src/futon2/aif/`. This is discovery only: no production edits, ticket creation,
clicks, shared-JVM evaluation, or store writes.

Joe's instruction is the design constraint: a finding creates a T- ticket at
the front; thereafter selection, construction, authoring, review and closure
operate on an ordinary ticket. Provenance belongs at creation and in the
store's closure bookkeeping, not in a special execution lane.

The existing registry discovers tickets, but discovery is not cascade
admission. Both existing futon2 tickets are live by the registry's default and
both lack cascade sources. There are also generic T-target dispatch and closure
gaps. Writing 44 Markdown files and removing the repair exclusion alone would
not make 44 executable candidates. Nor does an existing live priority field
implement “front”: the registry sorts paths, while selection ranks policies.

## 1. The ordinary path, including its refusals

| Stage | Actual implementation and consequence |
|---|---|
| Discover | `mission_registry.clj:603–628`, `load-tickets`: immediate primary-checkout `holes/tickets/T-*.md` under each repository in `/home/joe/code`; worktree/sandbox fences precede ID deduplication. Filename is identity. |
| Parse | `mission_registry.clj:589–601`: title, bold Status line, optional parent M-id; absent/unknown status is **live**. DONE is complete; SUPERSEDED/DEFERRED/PARKED/ARCHIVED inactive; WATCH/FINDING/DESIGN CONSTRAINT and awaiting Joe not actionable. |
| Resolve | `mission_registry.clj:630–646`: T- identities use only the ticket registry; `ticket-status` exposes open boolean and 0/1 open-hole count. |
| Flat proposal | `mission_registry.clj:648–662`: `:advance-ticket`, target, weight 1.0, ticket path, one open hole. `forward_model.clj:187–195` provides channel prediction, not a cascade interpretation. |
| Live cascade target enumeration | `cascade_problems.clj:36–47`: open missions followed by live tickets. `scripts/futon2/report/war_machine.clj:6790–6834` unions these with declared-source and proposal targets before assembly. The flat proposer is not a translator into cascade sources. |
| Supply | `cascade_sources.clj:106–120,133–206` loads pinned `:wm/cascade-source-v1` declarations and interpretation receipts, observes tokens, and supplies contexts. `mission_hole_wants.clj:73–118` derives wants from **missions**, not ticket Markdown. |
| Admission | `cascade_problems.clj:106–188,195–229` requires a nonempty universe, admitted interpretations for every pattern, nonempty wanted tokens, checkable locators for facts/wants/guards/produces, a nonempty constructed precedence with construction receipt, declared beta for context, and declared horizon. These remain necessary for every ticket. |
| Choose | `war_machine.clj:6438` `select-and-record-cascade!` ranks the admitted policies through the cascade selection law. Traversal order is not a FIFO promise. |
| Dispatch | `full_loop_runner.clj:1575–1596` resolves `:type :advance-ticket` via `ticket-entry`, but a plain `:kind :cascade-candidate` T-target falls through to mission lookup. This is a **generic ticket bug**, currently hidden for repair aliases by `bind-selected!`. |
| Close | No ordinary Markdown ticket-status writer/closure hook was found in the bounded `src/`/`scripts/` survey. A reviewed change can mark the ticket DONE; the registry notices on the next read. A run labelled `:grounded-change` does not itself mean ticket DONE. |

The existing source declaration already has `:target`, `:context`, `:beta`,
`:facts`, `:want`, `:locators`, `:patterns`, `:interpretation-receipts`, and
`:candidates`. A ticket still needs concrete observable task outcomes, authored
pattern interpretations and their admitted construction; neither an issue title
nor its existence supplies these. Class-J judgement cannot silently become a
checkable token. Invalid declared sources are not equivalent to missing sources.

Supporting ordinary-ticket machinery is real but does not fill these gaps:
`work_target_belief.clj:51–116` admits ticket actions against a pinned registry
snapshot; `scripts/futon2/report/cascade_lane.clj:164–169,308–342` retrieves ticket text;
`interpretation_request.clj:25–55,158,263` supports ticket requests. The latter
filters Priority/Severity metadata from retrieval text at line 48; that is not
a priority consumer. `enumeration_completeness.clj:102–112` naming
`:advance-ticket` also does not establish executable admission.

Two additional generic fixes are needed before claiming this path works:
`full_loop_runner.clj:633–656` must resolve ticket repository/path from the
registry (its action path search omits `:ticket-path`), and
`controller_authority.clj:27–35` currently checks ticket openness only for
`:type :advance-ticket`. Completed T-targets must not re-enter through stale
source declarations merely because the candidate kind is cascade-candidate.

### The two existing files

| Ticket | Registry result | Cascade result at this base |
|---|---|---|
| `T-wm-excessive-guardrails-19092026` | No Status or parent; live by fallback. Historical discussion/ruling, not a scoped closure contract. | No declaration for its target; first refusal `:universe-not-admitted`, missing `:universes`. |
| `T-wm01-runtime-error-reference-19092026` | No Status or parent; live by fallback. Partial-delivery runtime-boundary note. | Same missing-source refusal. |

`probe.clj` calls the actual read-only registry, enumerates all declared-source
EDN targets without invoking observation ports, and calls the actual pure
assembler with these two IDs and an empty source map with horizon 2. This
isolates their first refusal, rather than pretending to have run a live tick.
Mission-hole merging cannot supply either because its input is missions.
The captured result is `probe-output.edn`.

## 2. Creation, order and the 44 outstanding findings

### Write an ordinary ticket, not an alias proposal

At the successful finding-creation boundary (`repair_obligation.clj:500–588`),
create one ticket idempotently in the primary repository's `holes/tickets/`.
The existing `repair_proposals.clj:13` identity function is usable **at this
boundary**: `T-` plus the finding ID. Keeping that opaque identity is compatible
with Joe's instruction; parsing `T-repair-` during execution is not.

For the frozen real `:explanation-invalid` finding
`repair-occ-6c6ecaf858aff5c7f9aeef1b92ad205e7ed31ed29882e89fa52f2724d3d039c8`,
the path would be:

`futon2/holes/tickets/T-repair-occ-6c6ecaf858aff5c7f9aeef1b92ad205e7ed31ed29882e89fa52f2724d3d039c8.md`.

Use existing Markdown/registry conventions:

- A heading describing the actual failed behaviour, not a special repair role.
- `**Status:** OPEN` (or the ordinary non-actionable status after an evidenced disposition).
- Parent M-id only when the finding actually names a mission; do not manufacture a parent for nil or a previous finding ID.
- Body: observed failure, scoped task, acceptance evidence required, existing partial work, and provenance link to the immutable finding, its digest and retained attempt evidence. These are body content, not already-parsed registry fields.
- At accepted completion, `**Status:** DONE` in the reviewed ticket revision.

This uses the same file format and parser as the two existing tickets, but
adds the Status line those files lack. There is **no existing** structured
finding-to-ticket link field, ticket publication receipt, acceptance-criterion
parser, or generic queue-priority field. Those contracts must be introduced
explicitly; this report does not pretend that `:priority` or `:repair/id` is
already an ordinary-ticket field. The creation bridge must retain a durable
provenance association for the store; it must not inject native repair keys
into the selected action. Retry after either half of publication must finish
the missing half without duplicating or overwriting an edited ticket.

### A mechanical done observation is possible with existing locator fields

An ordinary C4 locator can check the anchored declaration `**Status:** DONE`
at the ticket path, using existing `:class :C4`, `:repo`, `:sha`, `:path`,
`:decl` fields. `observation_checks.clj:31–39,54–70` resolves the declared
revision and checks the declaration in that revision; it is not restricted
to Lean files. The pure probe confirms DONE true, OPEN false, embedded example
false, and registry classification complete. Use a declared reference resolved
at observation time and retain the resolved commit; pinning forever to the
opening revision would never see completion.

This observes **the ticket's declared status**, not the truth of its bug fix.
The ordinary authored acceptance criteria and reviewed evidence must justify
that status change. A separately declared want/locator can observe the actual
fix where mechanically expressible. C3 file existence observes creation only.
Do not use finding resolution as the ticket's done token: closure would then
require its own consequence. A token name and its interpretation for this task
are missing declarations, not something inferred automatically from the title.

### “Front” has no declared live priority carrier yet

`load-tickets` sorts by `(juxt count identity)` over absolute paths before
identity deduplication. Substrate enumeration puts missions before tickets.
The legacy proposer gives every ticket weight 1.0. The live posterior uses
policy scores/habits/temperature, not “first item wins”. The old private repair
entry's extreme rank/score and repair prompt wording are not an ordinary queue.

Keep the existing discovery and selection mechanisms. Add the smallest
**generic** declared queue-order contract and consumer, applicable to any
ordinary ticket, with creation requesting front placement through that
contract. Exact schema is missing and must be specified before implementation.
For Joe's strict reading, choose the earliest *admitted* queue stratum, then
use the existing policy law within that stratum; record refused front entries
instead of fabricating executable policies. This is a proposed generic
selection-domain rule, not existing behaviour or a repair score bonus.
Do not simulate priority by shortening filenames, modifying habits, or putting
repair-type checks back into G. Unordered/no-ticket cases must retain their
current decision bytes. The concrete remaining order question is whether
multiple simultaneous front insertions are FIFO or newest-first; recommend
stable finding opening time then identity for deterministic backfill, but do
not claim that tie rule is already specified by Joe.

### Outstanding store census and disposition

The actual `open-obligations` reader (`repair_obligation.clj:909–933`) excludes
resolutions/dismissals and derives awaiting-validation from implementation or
verification records. The read-only snapshot contains **44**:

| Class / derived status | Count | Ordinary-ticket treatment |
|---|---:|---|
| Machine failure / open | 10 | Create scoped task tickets; retain original evidence, declare missing acceptance inputs; do not fake admission. |
| Machine failure / awaiting-validation | 19 | Create tickets for the remaining validation, retaining implementation evidence and incremental credit; do not redo the implementation by default. |
| Independent review failure / awaiting-validation | 3 | Same remaining-work treatment. One older record has no failure-kind/discharge-contract: preserve that explicit gap, not an invented modern contract. |
| Environmental hold / open | 12 | Ordinary verify/restore-precondition tickets if work remains. Already-cleared conditions require dated recheck evidence and the store's condition-cleared disposition. Pure external waiting uses ordinary WATCH/DEFERRED status; no pretend code implementation. |
| Incomplete recoverable | 0 | No current migration; future resumable work is an ordinary task with retained job evidence, not a special repair reviewer. |

Environmental failure kinds: agent-unavailable 7, substrate-unavailable 1,
agent-readiness-failed 1, guardrail-refusal 3. Machine kinds and all 44 IDs,
statuses, original targets and contracts are in `probe-output.edn`; the
one-row-per-finding appendix identifies each migration class. This is not
permission to bulk dismiss old records, assume a precondition is clear, or
reclassify a guardrail refusal from its name. Creation should not exclude
an entire environmental class. Existing non-actionable ticket status determines
eligibility after an evidenced disposition, just as for other tickets.

## 3. Repair-specific branch inventory

A = creation/provenance/store bookkeeping; retain at that boundary.
B = special treatment after creation; remove or replace with the ordinary
capability named below. A/B rows split two concerns currently co-located.
These are branch/function sites, not a count of keyword occurrences. Legacy
readers and evidence validators should not be deleted merely for mentioning
repair. `repair-sites.txt` and `runner-sites.txt` retain the bounded source
search so grouped sites can be checked against the revision.

### Selection, dispatch and closure

| Site | Class | Reason and ordinary replacement / missing capability |
|---|---|---|
| `repair_proposals.clj:13,34–67` | A/B | Identity and pinned finding provenance belong at creation; emitting `:repair-finding-proposed` with unavailable closure in place of a real ticket is B. |
| `repair_proposals.clj:70–85` | B | Open store becomes a repair proposal menu each tick; publish once through the ticket registry instead. |
| `cascade_proposals.clj:123–128` | B | Merges a repair-specific supply stream; ordinary ticket/source enumeration replaces it. |
| `cascade_proposals.clj:136–145,169–177` | B | Repair-target set forcibly withholds/removes problems with `:repair-closure-observation-unavailable`; generic locator/admission refusals already express missing evidence. |
| `full_loop_runner.clj:74,254–267,751–759,3355,3409–3417` | B | Separate repair reviewer config/readiness/roles; use ordinary reviewer selection and retain author/reviewer independence. |
| `full_loop_runner.clj:214–252` | A/B | Finding link is provenance; special stop-line parking/dependency is B. Generic ticket dependency/resumption is missing, not a reason for an origin-specific lane. |
| `full_loop_runner.clj:368–388` | B | Special packet routes for repair/historical validation; use the ordinary selected ticket route. |
| `full_loop_runner.clj:419–426,557–564` | A/B | Retained historical repair context/receipts are A; deriving the current run route from them is B. Keep old dossier readers. |
| `full_loop_runner.clj:633–654` | B | Borrows failed-action/machine-repair repository; resolve the ordinary ticket's registry path and task locators (generic gap). |
| `full_loop_runner.clj:1289–1299` | B, legacy | Private repair entry with artificial extreme score/rank; not current ordinary cascade enumeration, and not an acceptable queue implementation. |
| `full_loop_runner.clj:1374–1398` | B | Dedicated repair construction; ordinary cascade construction already exists around 1320. |
| `full_loop_runner.clj:1400–1430` | B | Dedicated historical validation entry/constructor; an ordinary ticket can ask for validation with declared acceptance evidence. |
| `full_loop_runner.clj:1499,1524–1532` | B | Native repair identity changes construction wiring; generic construction holes must describe the ticket's work. |
| `full_loop_runner.clj:1575–1596` | B | Prefix/native-id binding to finding; all T-targets must use ticket-entry, including cascade-candidate (missing generic dispatch case). |
| `full_loop_runner.clj:1821–1837` | A/B | Compact finding projection is A when retained as provenance; attaching special repair-obligation prompt context is B. Use ticket body and referenced evidence. |
| `full_loop_runner.clj:1864–1870` | B | Limb completion requires native repair identity/resolution evidence; ordinary target/revision evidence must suffice under a versioned generic receipt contract. |
| `full_loop_runner.clj:1933–1939,1953–1958` | B | Finding replaces ticket context and prompts command repair priority; generic task context plus declared queue order replaces both. |
| `full_loop_runner.clj:2822–2850` | A/B | Store resolution reader is store-bound evidence; the subject/entity equality validator itself is generic and should remain. Its caller must supply ordinary ticket acceptance evidence. |
| `full_loop_runner.clj:3230,3261,3479,3547–3593,3905–3948` | A | Failure classification, contract and new finding creation; attach the idempotent ticket publication boundary here through the store, not at each branch independently. |
| `full_loop_runner.clj:3289` | B | Incomplete-recoverable origin decides deferred completion; generic resumable task/job bookkeeping is needed. |
| `full_loop_runner.clj:3838–3864` | B | Finalizer calls repair discharge on selected action; ordinary durable ticket closure should notify the store-owned provenance bridge. |
| `full_loop_runner.clj:4028–4039,4059–4075` | A/B | Reading open finding history is A; class-specific validation/repair routing material and legacy type flags are B. Ordinary cascade selection already runs here; do not reintroduce forced repair selection. |
| `full_loop_runner.clj:4077,4082–4105,4111–4112,4160,4178,4206,4298` | A/B | New occurrence/supersession and memory audit are A. Binding selected-stop-line, switching participants, forcing review control, skipping normal traces are B. Ordinary selected ticket gets normal participants and trace. |
| `full_loop_runner.clj:4347–4411` | B | Historical validation has a separate admission/execution/early terminal branch; express validation as ordinary ticket work. |
| `full_loop_runner.clj:4412–4488` | A/B | New failed successor record is A; selecting/reusing old jobs because of stop-line class is B. Generic resumption must preserve job evidence without origin routing. |
| `full_loop_runner.clj:4522,4537,4659,4670` | A/B | Old attempt reference is A; assembling special failed-commit/reused-job reviewer context by repair origin is B. Use ordinary task prior-work references. |
| `full_loop_runner.clj:4742–4760` | B | Measured completion uses finding discharge contract/resolution reader; generic ticket done/acceptance evidence adapter is missing. Retain measured-completion checks. |
| `full_loop_runner.clj:4790–4828,4869,4892–4920,4951–4989,5093–5141` | A/B | New failure/provenance records are A; deferred-review special routing/supersession decisions are B. Generic failed task handling must still create findings/tickets. |
| `full_loop_runner.clj:5031–5033` | B | Repair publication catch-up runs on every click; move provenance publication/reconciliation to the store's lifecycle, not ordinary selection. |

### Discharge, tripwires and other consumers

| Site | Class | Reason and replacement / gap |
|---|---|---|
| `repair_discharge.clj:15–41` | B | Selected `T-repair-`/native key/legacy type selects a special binder. Move finding pin verification to creation/closure bridge; ordinary dispatcher resolves all T-targets alike. |
| `repair_discharge.clj:43–60` | A | Implementation/successor contexts and operation evidence can remain store bookkeeping. |
| `repair_discharge.clj:62–171` | B/A | Selected-action finalization switches on finding class/phase, invokes special evaluators, records implementation or successor, then publishes. Remove that execution hook; preserve store verbs/evidence checks behind the ticket closure bridge. |
| `repair_discharge_receipt.clj:18–59,64` | A | Derive/verify immutable provenance; legacy receipt remains readable, not a ticket admission requirement. |
| `repair_discharge_receipt.clj:83–138` | A/B | Independent provenance publication/retry is A; requiring an extra repair publication commit for ordinary ticket progress is B. |
| `repair_discharge_receipt.clj:140` | B at caller | Catch-up utility is bookkeeping, but its unconditional click-path caller is misplaced. |
| `tripwire.clj:110–181,245–278` | A | Store snapshot/status transitions and T6 mutation checks protect immutable findings; preserve these invariants when adding a bridge. |
| `tripwire.clj:292–315` | B | T7 selects stop-line IDs for repetition detection; generalize to repeated ordinary ticket/target with no fresh artifact, retaining the actual evidence comparison. |
| `tripwire.clj:350–400` | A | T8 duplicate-finding evidence is creation/store deduplication, not a separate execution role. |
| `tripwire.clj:898–920,938–953` | A/B | Finding/resolution closed-set audit is A; selected mission-to-native-finding history join is B. Use ordinary target history; legacy IDs may be normalized only in historical readers. |
| `tripwire.clj:1040–1090,1143–1150` | B | Special repair coverage defers tripwires and excludes environmental holds. If a declared coverage rule is retained, it must concern available ordinary tickets, not origin. Preserve unrelated/default non-halting tripwire policy. |
| `interoceptive_commitment.clj:16–20,47–107` | A | Finding/status joins are store provenance. A new store transition would require updating its versioned status contract, not weakening validation. |
| `interoceptive_commitment.clj:109–171` | B, dark | Global confidence factor depends on unresolved repair ledger. Only `interoceptive_manifest.clj:154–177` calls this in bounded src/scripts search; no live cascade scorer call found. Any activation needs generic unresolved-work semantics, not a repair exemption. |
| `interoceptive_manifest.clj:21,111,154–177` | A/B | Pinned store capture is A; feeding repair-only confidence to a future controller is B. Retain store-lock authority; don't bypass it to migrate records. |
| `run_participants.clj:5,22,29–30` | A/B | Historical role audit is A; origin chooses repair reviewer is B. Ordinary participants already support independent review. |
| `on_demand_entrypoint.clj:49,80–87`; `run4_task_pin.clj:97–98` | B | Requires/propagates special reviewer casting; use generic author/reviewer roles. |
| `full_loop_cli.clj:39,631–642,481–485` | B | Special reviewer options and repair-before-ordinary QA assertion; replace current-run semantics with generic queue priority. |
| `full_loop_cli.clj:84–91,594–610` | A | Historical inspection of old discharge evidence can remain. |
| `scripts/futon2/report/war_machine.clj:2248–2262` | B, legacy diagnostic | Repair-selection predicate skips repair traces from nonprogress; generic target/progress history replaces this distinction. Not evidence of a current cascade G term. |
| `repair_evaluators.clj:36–77`; `repair_discharge_evidence.clj:82–138` | B/A | Failure-kind/native-id selects required evaluator: B. Pinned replay, artifact and reviewer evidence checks: reusable A/general validators, usable as ordinary authored acceptance checks. |
| `limb_evidence.clj:55–67` | B | Required `:repair/id` in limb receipt is not an ordinary target contract. Introduce generic target receipt version, keep legacy readers. |
| `deposit_preflight.clj:45–58` | A/general | Parameter called repair-id is passed as target, not a repair-type dispatch branch; rename only with contract compatibility. |
| `task_execution_evidence.clj` repair mentions | General, retain | Comments do not make generic artifact-binding/review checks a repair special case. |
| `tripwire_calibration.clj:53–84,152–163` | A/offline | Calibration fixtures reproduce selected-stop-line history in temporary stores. Update controls alongside generic repetition detection; not a live selector. |
| `repair_history_replay.clj` | A/offline | Historical fixture replay is evidence tooling; its special selected-action caller, not ability to replay, is B. |
| `repair_obligation.clj:500–588,909–933,939–1305,1307–1529,1544+` | A | Authoritative creation, views, dispositions, operation receipts, implementation and resolution. Keep invariants; move calls to creation/provenance/closure boundary rather than bypassing the store. |

This separates post-creation origin routing from legitimate failure detection.
A new failure while building any ordinary ticket still opens a finding and
another ordinary ticket; forbidding that would suppress evidence, not unify
selection. Old record fields need compatibility readers, not current-run flags.

## 4. Closing the ticket must resolve through the store

Use a **store-owned observer of durable ordinary ticket closure**, not a
prefix-conditioned runner finalizer. The ordinary close path emits/retains
its target, committed ticket revision and acceptance evidence regardless of
origin. The store joins that closure to its creation provenance association.
It verifies the reviewed DONE revision and criterion evidence, then invokes
its existing verbs. Reconciliation from retained closure events/status must
be idempotent and recover after a crash. A bare unreviewed edit to Status, or
a run outcome label without ticket closure, is insufficient.

There is no such generic closure event/bridge today. A polling reconciler over
committed ticket statuses can be the first implementation, provided it retains
the same closure evidence and never treats “file says DONE” alone as proof.
The selection/build/review path does not inspect provenance. The store bridge
necessarily does, exactly for Joe's requested finding resolution.

**Existing evidence contracts are a constraint, not an inconvenience:**

- `record-implementation!` (`repair_obligation.clj:1365–1439`) records machine/review partial progress with grounded, independently reviewed artifact evidence; it does not resolve the finding.
- `resolve!` (`1468–1529`) requires grounded artifact/witness and a distinct attempt from the failure. Machine/review classes also require an implementation and a production-shaped validation attempt distinct from it. Environmental resolution requires production-shaped validation. Those checks cannot be replaced by `(resolve! ... {:status :done})`.
- `dismiss-condition-cleared!` (`975–1014`) accepts dated checked evidence (`:checked-at`, `:source`, nonempty `:observation`), actor and reason. This is the appropriate disposition for an evidenced vanished precondition, not a fabricated successor commit.
- `dismiss-wontfix!`, unexecuted/echo/fixture-pollution/superseded-attempt dispositions have their own authority/evidence requirements (`939–1305`). No bulk age-based dismissal follows from this discovery.
- `supersede!` (`1441`) concerns incomplete recoverable work, not proof of success. Existing immutable operation receipts (`1335`) are useful bookkeeping, but do not already implement a ticket link or closure hook.

Smallest correct version preserving current invariants: ordinary ticket
criteria retain any still-required implementation/validation evidence; the
bridge calls record-implementation when that evidence exists and resolves only
when the complete closure evidence satisfies resolve!. The 22 awaiting records
reuse their implementation/verification rather than starting over. Evidence
may span ordinary attempts; there is no specially cast repair reviewer. If a
DONE ticket lacks required store evidence, retain a visible reconciliation
failure and leave the finding open; never write a resolution file directly.

If Joe instead intends **one ordinary reviewed commit alone** to discharge all
legacy two-stage obligations, that requires an explicit versioned store
contract change and evidence-preserving migration. It is not currently a
legal call to the existing verbs. The missing contract must be decided before
that variant is implemented. This is the principal invariant conflict to
surface, rather than route around with a success label.

## 5. Implementation slices (one reviewable behaviour at a time)

| Slice / size | Change | Required tests |
|---|---|---|
| 1. Generic queue declaration / medium | Specify and implement ordinary ticket front insertion and stable ordering, with recorded eligibility/refusals. No origin field reaches the ordering consumer. | Ordinary non-repair tickets also support front insertion; same-rank tie deterministic; inadmissible front entry produces ordinary refusal; no declared queue entries leaves frozen selection bytes identical. |
| 2. Finding → ticket / medium | Store-owned idempotent publication, durable provenance link, existing Markdown fields, queue insertion via slice 1; no special candidate supply. No automatic invented interpretations. | Frozen real explanation-invalid finding creates exactly one correctly linked OPEN T-file at the front in a temp primary-checkout fixture; retries/crashes/collisions preserve edits and identity; environment hold creates a normal verify/restore task; missing scope remains explicit. Production store file counts unchanged. |
| 3. Ordinary T cascade path / medium | Fix generic registry lookup, repository path and live-ticket guards; declare actual task wants, locators and admitted construction through existing machinery. Remove repair supply withholding and prefix binder. | A source-admitted T-ticket from slice 2 is selected through the same path as an unrelated T-ticket. Fail test on any call to repair_proposals/bind-selected!/special constructor during selection. Missing source still refuses; DONE T-source cannot re-enter; repair-shaped opaque ID has no semantic effect. Neither current reference-only ticket is asserted magically admitted. |
| 4. Ordinary execution / large, split by call boundary | Remove special casting/prompts/trace skips and legacy selected-action branches; retain generic artifact/role/grounding checks and old dossier readers. General resumption, if needed, is a separate ordinary-task change. | Passing ordinary grounded fixture builds/reviews/closes T target with normal participants, full trace and ordinary criterion evidence. Special repair reviewer/evaluator ports throw if called. New failure still creates a finding/ticket. Frozen no-repair decisions byte-identical, not just equal chosen target. |
| 5. Ticket closure → store disposition / medium | Generic durable closure evidence plus store-owned provenance join and existing store verbs; separately version any approved discharge-contract change. Remove selected-action discharge/catch-up hooks. | Closing an evidence-complete ticket resolves its finding using actual temp-store verbs; wrong ticket/revision, missing independent review, missing required successor, bare grounded-change and bare DONE all fail safely; duplicate closure is idempotent; partial implementation remains open; dated condition-cleared disposition tested. No production stores touched. |
| 6. Backfill and diagnostics / medium | Migrate the 44 from frozen census using the same publisher; generalize repetition/coverage to ordinary work and retain historical audit readers. | One row→one disposition/ticket, all 44 accounted for; awaiting-validation credit preserved; old missing contract typed; no silent environmental exclusion or automatic dismissal; old dossiers readable; no active selection-to-close branch tests repair origin. |

Slices 1–3 together establish the requested “real finding becomes a ticket at
the front and can be selected normally” behaviour. Slice 2 alone must not claim
that acceptance: source admission and generic dispatch must also be repaired.
Use tiny hermetic repository/store fixtures and pinned declarations; no live
click or serving-JVM evaluation is necessary for any of these tests.

## 6. Evidence and gates

`probe.clj` is read-only and uses the real registry/store readers plus pure
assembly and declaration checks. It neither loads the runner nor calls Agency,
futon1b, observation shell ports, or a mutating repair verb. Its output retains
the 44 records' relevant fields and the two ticket refusals. Store file set and
count were unchanged (292 before and after). This count is not a universal
claim about concurrent external writers or content hashes.

The frozen finding is retained verbatim as `finding.edn`; `finding.sha256`
pins its bytes. No finding evidence was amended. `outstanding.tsv` maps every
snapshot ID to its class/status/remaining-work treatment. These are discovery
artifacts in this worktree, not publication to the live stores.

Commands and fresh results are in `VALIDATION.txt`. Required gates: clj-kondo
on the probe, check-parens on the probe, and git diff --check. No production
code changed, so production test namespaces were not run. No warrant was
registered because this packet explicitly prohibits store writes.
