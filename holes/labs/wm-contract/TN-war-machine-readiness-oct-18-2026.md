# War Machine readiness campaign — 18 October 2026

## Campaign status

Active. Joe authorized five real clicks on 2026-10-18. They run sequentially,
one at a time, with an evidence review and any necessary offline repair between
clicks. This is a new campaign; no October 17 run is relabelled or resumed as a
new click.

## Fixed cast and method

Every launch explicitly uses:

- author: `zai-2`
- reviewer: `zai-6`
- repair reviewer: `zai-7`

For each click the debugger is attached. A repairable error is held and retried
at its addressable `run-phase!` boundary without another click or another G
selection. A defect that cannot safely be repaired in the held process stops
the click, is repaired offline, and is replayed or checked before the next
authorized click. No refusal, abstention, incomplete result, or build failure
is counted as progress merely because diagnostics or code were produced.

The campaign records the exact click/run/attempt/cascade identities, phase and
debugger timing, every repair job and commit, reload/retry evidence, report-card
and run-record paths, grounded effects, and the readiness disposition before
the next click. After click 5, an independent Claude peer review will assess
the full series.

## Preconditions incorporated from the October 17 campaign

- agent JSON representation is normalized by the shared handoff boundary;
- selected-target readings receive bounded semantic correction and explicit
  `pattern_refs` requirements;
- occurrence-square authoring uses one snake-case wire schema;
- selected cascades carry occurrence-keyed four-map squares;
- reviewed D4 decisions become attributed operational authoring on same-click
  retry;
- selected-target reading, square authoring, selected-reading review, cascade
  execution, and cascade resume are addressable `run-phase!` boundaries;
- a same-click repair/resume is tested to retain one G selection.

## Runs

### Click 1 — selection abstention; live repair transport failed

- Authorization: campaign click 1 of 5.
- Start: `2026-10-18T03:55:43Z`; issued at `03:56:44Z`; terminal at
  `04:01:26Z` (about 282 seconds after issue).
- Click: `wm-click-e6aae912-6192-4e62-90f8-df6abf67c01a`.
- Run: `2026-10-18-d0537eb0-8e61-4766-8a50-ee79343e7e20`;
  attempt `attempt-001`.
- Preflight: five clicks available; fixed cast idle and invoke-ready; 13/13
  tripwires clear. The 48 open obligations were recorded under the standing
  selection-precedence ruling rather than used as a launch veto.
- Trace: `stop-line-memory` at 20 seconds; `selection` from 40 seconds; the
  debugger held selection at `03:59:55Z` on
  `:wm/selection-terminal-abstention`.
- Substantive condition: outer task selection found no unique task-state
  minimum. Its undefeated set was `M-formal-war-machine` and
  `M-digital-nomad-patterns`; therefore there was no addressable action.
- Outcome: `abstained`, no tick carried. This is not counted as readiness
  progress.
- Run record:
  `data/wm-runs/tick-run-record-2026-10-18-d0537eb0-8e61-4766-8a50-ee79343e7e20.edn`.
- Full held-phase evidence:
  `data/wm-click-loop/debugger/held-2026-10-18-d0537eb0-8e61-4766-8a50-ee79343e7e20.md`.
- Terminal debugger page:
  `data/wm-click-loop/debugger/wm-click-e6aae912-6192-4e62-90f8-df6abf67c01a.md`.
- Click log: `data/wm-click-loop/click-001-1792295744.log`.

The live repair dispatch `invoke-1792295995172-16440-eb338e9e` and terminal
repair dispatch `invoke-1792296086410-16441-3f5192ef` both failed before the
repair seat executed. Agency returned HTTP 400/code 1261, `Prompt exceeds max
length`: the loop had embedded a 6.86 MB held-phase record (and later the large
last-result) directly in the prompt. This is an apparatus defect independent of
the selection abstention.

Between-click repair: `wm_click_repair_loop.py` now sends bounded evidence
excerpts plus the durable local artifact paths, omits unbounded last-result
fields, and initially enforced a 60,000-character dispatch ceiling while retaining the
mandatory repair guide. The regression test plants 200 KB page, log,
last-result, and prior-attempt payloads and checks that the repair packet stays
within the ceiling and retains its evidence addresses. Gate: 3 Python tests
pass; both changed Python files compile.

The substantive selection condition was also repaired before Click 2. Both
undefeated tasks had exactly equal complete scored vectors and their direct
pairwise result was `:tie`; therefore each was a genuine G minimizer. The pure
selector still refuses ambiguity by default. The production live selector now
explicitly requests `:canonical-identity` representative selection only for
an equal-G minimum and records `:reason
:equal-minimum-canonical-representative`, the sorted tied members, the rule,
and `:g-equivalent? true`; it does not rewrite the pairwise tie or invent a
cost difference. Focused gates: 31 tests / 135 assertions / zero failures or
errors. A read-only replay over the current production registries and pinned
live outer-learning authority returned addressable action
`{:type :advance-mission, :target "M-digital-nomad-patterns"}`, with tied
members `M-digital-nomad-patterns` and `M-formal-war-machine` and outer learning
`:live-consumed`. The replay issued no click. The automatically published
repair ticket `T-repair-occ-216c…` is closed with this evidence.

Joe clarified before Click 2 that choosing an equal-G representative keeps the
run moving but does not satisfy the Lean discrimination contract. The selection
receipt therefore also carries `:selection-discrimination {:verdict :fail,
:reason :chosen-by-tiebreak, :tie-count 2, ...}` and cites
`DarkTower.WarMachine.Holes.selectionDiscriminates`, whose rule is that no
recorded choice may have `chosenByTiebreak` (`tieCount > 1`). The canonical
representative is operational continuation, not a green certificate and not a
claim that G discriminated.

Disposition: both actual Click 1 blockers are repaired and gated. Click 2 may
be launched after commit/hot-load; the remaining four campaign authorizations
are intact.

### Click 2 — reached cascade execution; store root and repair transport failed

- Authorization: campaign click 2 of 5.
- Start: `2026-10-18T04:23:32Z`; issued at `04:24:31Z`; terminal at
  `04:33:36Z`.
- Click: `wm-click-ea4e52f7-ee16-4409-b7f4-8f52faa35ebe`.
- Run: `2026-10-18-9c3a318d-a329-4dca-a19b-17976a0acfde`;
  terminal attempt `attempt-002`.
- The run passed the former selection breakpoint, selected
  `M-digital-nomad-patterns`, completed selected-target-reading plus its
  bounded redecision, completed occurrence-square authoring plus its
  redecision, and reached `cascade-execution`.
- The debugger held at `04:31:55Z` on `:wm/phase-failure`:
  `FileNotFoundException` for
  `data/wm-executable-cascades/4372a892…a976.edn`. The production default was
  process-relative and the append-only saver did not create its store root.
- Live repair `invoke-1792297915129-16457-920268db` and terminal repair
  `invoke-1792298016125-16458-f548c34a` were rejected by Agency as oversized.
  The first dispatch ceiling repair was therefore directionally right but its
  60,000-character bound exceeded Agency's actual accepted envelope.
- Outcome: `incomplete`; no tick carried.

Between-click repairs: production cascade and journal roots are now absolute
under `~/code/futon2/data`; append-only cascade persistence creates a missing
store directory or fails typed before writing. A planted absent nested store
checks save/reload identity. Repair dispatches are capped at 20,000 characters
with 4,000-character evidence excerpts; full evidence remains available at the
named local paths.

#### Why the recorded flight did not predict these live failures

The October 17 recorded flight was a deliberately narrower instrument than a
deployment-equivalent click. Its design contract says that it resumes from an
immutable outer-selection receipt, never invokes outer selection, rejects any
unrecorded agent call, and writes beneath a caller-supplied isolated output
root. The implementation preserves those distinctions: it accepts no selector
port, injects the restored outer receipt, consumes pinned agent answers and a
separately attributed `offline-operational-authoring.edn`, and explicitly
creates its isolated journal directory before appending entries. See
`RECORDED-FLIGHT-DESIGN-2026-10-17.md` and
`src/futon2/aif/wm/recorded_flight.clj`.

Consequently, the three new observations are at boundaries that the potted
history intentionally did not exercise:

- the equal-G condition belongs to live outer selection; recorded flight
  restores an already selected receipt and structurally cannot call a selector;
- the oversized debugger repair packet belongs to the live Agency transport;
  recorded flight permits only manifest-pinned answers and makes no repair
  dispatch;
- the missing executable-cascade directory belongs to the production runner's
  default store and first-write behaviour; recorded flight uses an explicit
  isolated output root and creates its journal root itself.

The replay evidence was still predictive inside its stated boundary. Click 2
passed the repaired target-reading and occurrence-square stages, retained the
canonical cascade, and reached cascade execution, which is the same downstream
route exercised by the recorded flight. It then failed at the first production
persistence operation outside the replay envelope. The mistake was therefore
not that deterministic replay produced contradictory evidence; it was treating
post-selection, stubbed-port, isolated-store evidence as proof of the live
outer shell. Future readiness claims must name both layers separately:
`recorded-flight downstream conformance` and `live serving-boundary
conformance`. Neither implies the other.

### Click 3 — selected cascade retained; same-click continuation incomplete

- Authorization: campaign click 3 of 5.
- Start: `2026-10-18T13:42:27Z`; terminal at `13:53:49Z`.
- Click: `wm-click-ff5c61e8-6398-4a36-9555-003a7960b7cb`.
- Run: `2026-10-18-ccaa1478-2c3b-4c84-9593-f11eb5a31e90`;
  attempt `attempt-001`.
- Selected target: `M-formal-war-machine`; its reading policy contains four
  occurrence-distinct patterns: `agent/intent-handshake-is-binding`,
  `agency/self-attribution`, `agent/scope-before-action`, and
  `agent/eat-your-own-tail`.
- The run reached `cascade-resume`, but the same-click retry did not complete
  the production enactment/build-review-close route. Outcome: `incomplete`;
  no tick carried.
- Terminal repair dispatch `invoke-1792331630056-16544-6874dde8` failed
  without a verdict. The serving process did hot-load authored namespace
  `futon2.aif.wm.authored.req-5ca29b22`; the remaining fault was in the
  continuation protocol and repair-review transport, not absent authoring.
- Run record:
  `data/wm-runs/tick-run-record-2026-10-18-ccaa1478-2c3b-4c84-9593-f11eb5a31e90.edn`.
- Click log: `data/wm-click-loop/click-001-1792330949.log`.

#### Between-click production replay and repairs

Click 3 was replayed through the production continuation boundary without
re-running selection or spending a click. The repair sequence preserved
selected identity across resume, initialized the continuation Agency ledger,
passed the resolved Agency endpoint, normalized reviewed JSON status, added a
reviewed effect executor and durable close adapter, and partitioned execution
journals by cascade digest. Each selected occurrence received a separately
reviewed, digest-bound effect receipt.

Final replay `on-prod-live-m-formal-e2e-20261018-f` used Click 3's exact run
and attempt with `selection-ran false` and `clicks-spent 0`. It certified
canonical cascade digest
`7c9601fd5fa56f7f7774d41cbc58a9b5b3bcc1d977115ab55a11c5c45986ceec`,
executable digest
`7f7250db365669fa7c64c4d26b811c5e645a7b5db6fd73f481accee347fc9cf1`,
four verified effects, a four-entry certified journal, and durable close
receipt
`7a4af8ece478ab9561bf37d2977a8a87598b9607a94107cd74470351e9512fd8`.
The close artifact is
`data/wm-cascade-continuation-closes/7a4af8ece478ab9561bf37d2977a8a87598b9607a94107cd74470351e9512fd8.edn`;
the journal is under
`data/wm-execution-journals/7f7250db365669fa7c64c4d26b811c5e645a7b5db6fd73f481accee347fc9cf1/`.

This proves the selected production cascade can proceed through grounded
enactment, certified journalling, and durable build-review-close when resumed
from the recorded production frontier. It does not itself prove that the
ordinary click shell traverses the same route or carries a tick. Click 4 tests
that remaining live composition claim. Two campaign authorizations remain.

### Click 4 — cascade compiled; resume controls were not wired

- Authorization: campaign click 4 of 5.
- Start: `2026-10-18T21:09:22Z`; click accepted at `21:10:18Z`;
  terminal at `21:14:54Z`.
- Click: `wm-click-e783dcda-3ee0-4a21-940e-51d2847287b6`.
- Run: `2026-10-18-78e3527a-052f-4fef-b33e-abd5c67b1f85`;
  terminal attempt `attempt-002`.
- Timing: selection completed in 96,275 ms. The selected-target-reading,
  occurrence-square-authoring, and selected-reading phases reused their
  retained products in 3 ms, 2 ms, and 3 ms respectively. Cascade execution
  compiled successfully in 55 ms with digest
  `94c73bd78c2087bca94da66414a7eb2492c2ffb8dd18a05bc5ef7da304c71aa7`.
  Cascade resume then ran for 81,123 ms.
- Frontier: resume ended `:resume-protocol-violation`. Its diagnostic exposed
  only `{:clicks [nil nil], :selections [nil nil]}`. The nil controls show that
  the standard click did not supply evidence that retry spent no click or ran
  no selection, but the current predicate permits absent controls; therefore
  this diagnostic does not identify which additional invariant failed. The
  likely remaining alternatives are selected-versus-compiled identity or a
  malformed retry result. This is an adapter/observability defect, not evidence
  that a cascade effect failed.
- Outcome: `incomplete`; no tick carried. Finding:
  `data/wm-repair-obligations/findings/repair-occ-fa1ef846963fd8242c0f511cd18c27ff77e3d0c7193b0f8da4b62edb1a8a24f2.edn`.
- Run record:
  `data/wm-runs/tick-run-record-2026-10-18-78e3527a-052f-4fef-b33e-abd5c67b1f85.edn`.
- Click log: `data/wm-click-loop/click-001-1792357763.log`.
- Report card:
  `https://zone.hyperreal.enterprises/wip/wm-runs/2026-10-18-78e3527a-052f-4fef-b33e-abd5c67b1f85.html`.

Terminal repair dispatch `invoke-1792358094080-16640-2fd2c15e` again failed
Agency validation with code 1261, `Prompt exceeds max length`. The durable
evidence paths were bounded, but the shared repair-guide suffix itself still
made the packet too large. This is a second apparatus blocker to repair before
Click 5.

Disposition: one authorized campaign click remains. Do not launch it until
the D4 protocol reports every invariant separately (and the production runner
supplies real click/selection controls), the failing invariant is repaired,
and the terminal repair request fits the actually accepted Agency prompt
envelope.

#### Repairs before Click 5

Commit `706a4da4f` made the held retry contract observable and explicit. Both
`:cascade-ready` returns now retain `:selected-cascade-digest` separately from
the refined executable digest. The production runtime supplies concrete
click-budget and selection-counter atoms, and a protocol violation records five
individual predicates plus whether both controls were present. The terminal
repair packet was reduced from a 9,000-character ceiling to 1,800 characters,
with full evidence left at addressed artifact paths.

Focused gates passed: 36 Clojure tests / 190 assertions across the resume
protocol, production runtime, and cascade runner seam; 3 Python repair-loop
tests also passed. Static lint reported no errors and two existing style
warnings. No click was spent by these gates.

### Click 5 — live route reached resume again; identity/refinement return still malformed

- Authorization: campaign click 5 of 5. No campaign authorization remains.
- Start: `2026-10-18T21:21:45Z`; click accepted at `21:22:48Z`; terminal at
  `21:29:57Z`.
- Click: `wm-click-9b1310c2-5aca-4f56-8feb-0f1db9ee16ed`.
- Run: `2026-10-18-f016ed51-62de-426e-88c2-768fa18b00dc`; attempt
  `attempt-001`; target `M-self-improvement-loop`.
- Timing: substrate preflight 9,251 ms; preference refresh 3,235 ms;
  stop-line memory 4,117 ms; selection 76,341 ms; selected-target-reading
  105,175 ms including a 64,549 ms redecision; occurrence-square authoring
  103,949 ms including a 71,500 ms redecision; selection-world 77 ms;
  cascade execution 64 ms; cascade resume 73,428 ms.
- Cascade execution produced selected/executable digest
  `c935271947ceb7cb220bd7d132a89b938df8e98bb1b31ece1a5743d870ea6a9b`
  and entered the held resume path. Authored namespace
  `futon2.aif.wm.authored.req-790c9db1` was ingested while the phase was held.
- The new diagnostics worked. Click and selection controls were both present
  and unchanged (`[0 0]` for each). The retry returned a map, but
  `:selected-identity-preserved?` and `:compiled-digest-present?` were both
  false. Thus the remaining defect is now localized: the production retry
  adapter does not return the selected identity and refined executable digest
  required by D4. It is not a hidden reselection or extra click.
- Outcome: `incomplete`; no tick carried. Finding:
  `data/wm-repair-obligations/findings/repair-occ-950b5e58640d272150d96d6e2ad8827f89c4cac4dc4097c1cbe9c74d1cd09205.edn`.
- Run record:
  `data/wm-runs/tick-run-record-2026-10-18-f016ed51-62de-426e-88c2-768fa18b00dc.edn`.
- Click log: `data/wm-click-loop/click-001-1792358507.log`.
- Report card:
  `https://zone.hyperreal.enterprises/wip/wm-runs/2026-10-18-f016ed51-62de-426e-88c2-768fa18b00dc.html`.

The compact terminal repair request was accepted and created job
`invoke-1792358997877-16653-858bb78c`, proving the prompt-size repair crossed
the former dispatch boundary. The Zai-7 job nevertheless failed after 60
seconds without a verdict or commit; that is now a repair-seat execution
failure, not an oversized prompt.

The ordinary build/review/close machinery still ran after the terminal
failure and persisted checkpoints through `007-closed.edn`; this is collateral
apparatus completion, not a carried tick and not proof that the selected
cascade was enacted. Campaign result: five clicks, zero ticks carried. The
next repair must make the held retry return a D4-conforming result containing
the unchanged selected-cascade digest and a valid refined compiled digest,
then prove that exact production continuation by replay before any further
click is authorized.

## Addendum — Click 5 retry-product repair

The Click 5 failure was not a bad authored patch. It was a D4 transport-layer
substitution. `hot-load!` executed the reviewed patch function and returned its
verified, occurrence-keyed clause map as `:product`; `handle-error!` then
discarded that product. The held retry consequently consumed the JSON handoff's
`:decisions` carrier instead of the loaded code product. That explains why the
same patch compiled when invoked directly but the live retry returned neither
the selected identity nor a refined compiled digest.

The repair carries `(:product loaded)` into the resume context as
`:authoring-product`. The cascade retry consumes that value preferentially,
retaining the JSON decisions only as a compatibility fallback. A planted test
uses deliberately distinct transport decisions and loaded product, then proves
that the retry receives the loaded product. Focused gates after the change:
36 tests / 191 assertions / zero failures or errors across D4, the production
runtime, and the runner seam.

A zero-click replay used the exact Click 5 checkpoint
`data/wm-on-prod-history/checkpoints/2026-10-18-f016ed51-62de-426e-88c2-768fa18b00dc/attempt-001.edn`
and hot-loaded namespace `futon2.aif.wm.authored.req-790c9db1`. It returned:

- D4 status `:resumed`, `:clicks-spent 0`, `:selections-run 0`;
- unchanged selected digest
  `f79e6e5b560469f5f1a5edb69cda2e990a8f33fd6aa6228c13913e06fb031b4f`;
- `:cascade-ready` with refined executable digest
  `d005b2f842e2922e6fc530320e22f32362a2a41cc5fc3165dcd1b316f547ff7a`;
- an eight-occurrence `:wm/pattern-apply` frontier for the selected
  `M-self-improvement-loop` cascade.

This discharges the specific Click 5 `resume-protocol-violation` by replaying
the saved production selection through the repaired held phase. It does not
retroactively turn Click 5 into a carried tick, and it authorizes no additional
click. The next live run, if separately authorized, must still demonstrate
frontier enactment and tick carriage in the ordinary shell.

### Peer-review clarification and future cast discipline

Independent review `84a6d1f86` correctly identified the equal-G ingest gap,
the remaining JSON compatibility fallback, and the lack of any successful
Zai-7 live repair during this campaign. Its concern about post-failure work
needs one clarification: Click 5's `003` through `006` checkpoints are typed
`:not-reached-*` sorries, and `007-closed.edn` says `:not-a-delivery` because
there was `:no-authored-commit`. The machine recorded and closed a failed
attempt; it did not dispatch or commit unrelated selected work.

Future click campaigns use the stable role identities `wm-author`,
`wm-reviewer`, and `wm-repair-reviewer`. The click loop now resets all three
sessions immediately before each click and records a `cast-reset` ledger row.
It fails closed if a role is missing, busy, not invoke-ready, or cannot be
reset, so stale conversational context cannot silently enter a click. This
separates durable role/provenance identity from disposable model context.

### Post-review repairs: discrimination, authoring authority, and cast backends

Equal-G outer selection no longer chooses the lexically first policy. It
returns `:no-unique-task-state-minimum`, records the Lean
`selectionDiscriminates` failure, and carries the unsupported comparison
terms. The live caller no longer requests `:canonical-identity`; improving
ingestion is now the route to a selection rather than disguising absent
evidence as a deterministic decision.

The held cascade retry now consumes only the occurrence-keyed product returned
by the reviewed, hot-loaded patch. The JSON answer's `:decisions` remains a
handoff description but is no longer executable authority. A missing
`:authoring-product` produces `:verified-authoring-product-missing`.

The cast registry now accepts an EDN configuration through
`FUTON3C_WM_CAST_CONFIG`. Each stable role independently selects `:codex`,
`:claude`, `:zai`, or `:kimi`, allowing homogeneous or mixed casts without
changing role/provenance identity. Every backend receives a real reset hook;
the click-loop preflight from `017a673a1` invokes it before every click and
fails closed when freshness cannot be established.
