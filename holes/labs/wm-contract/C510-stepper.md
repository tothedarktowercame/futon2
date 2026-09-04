# C510 — the stepper (worklist `:U55`)

Date: 2026-09-04. Row: `U55` (class `:I`). Epic: `EPIC-run-era.md`, the step-era
ruling. Depends on `U54` / `C509-inter-tick-state-boundary.md`, which is the
checkpoint spec this builds to.

> "get a version of the machine set up that is stepable and resetable, so that
> we don't immediately have to deal with its peculiar behaviour between runs to
> make some headway... we could run it forward step by step, fix defects, and
> only then move on to the next step and eventual continuous running."
> — Joe, 2026-09-04

Four commands: `init <work-dir>`, `step`, `reset`, `accept`, plus `determinism`,
`compare`, `plant`, `check`, `deposit` and `status`
(`wm_step.sh:141,178,197,318,368,384,412,461,539,556`). Eleven recorded steps ran
through it (plus one smoke step whose work directory was removed when the pin
was re-taken, and the pre-flight's own tick). **The live stores were never
written**: after all eleven,
`data/wm-trace/wm-trace-2026-09-04.edn` still carries the mtime and the sha it
had when the re5 run left it at 07:50:47
(`f343432d772986ddc2f7fe38107913afc997201ebae9b6240dff152e235b1120`),
`data/wm-rationale/` still holds re5's four files, and no `.run-lock` was left
behind.

## The seam that did not exist, and now does

C509's first handoff item was that the sandbox lever exists in war-machine but
the entrypoint never pulls it: `:trace-dir` in judge-opts redirects the trace,
the habit-prior fold, the case-history index and the RE4 rationale store
(`war_machine.clj:2273-2282,5957,5983-5987,6274,6732`), and
`diagnostic-judge-opts` never set it; the receipt path had no seam at all.

`run_tick_once.clj:41-78` adds one: `FUTON_WM_TRACE_DIR` and
`FUTON_WM_RECEIPT_DIR`, resolved by `sandbox`, which is **pure in a getenv
function** so the seam is tested without mutating a process environment
(`test/run_tick_once_test.clj`, four cases). Unset is the live path and passes
NO `:trace-dir` at all (`:264`), so an unredirected tick's bytes are unchanged —
and the pre-flight run at 15:52, after the edit, reported the same three write
targets and the same 1663 paths read as before it. A sandboxed receipt carries
`:stepSandbox` (`:295`), present-only, so a step's receipt deposited into a run
store cannot be mistaken for a live tick's.

The run lock is deliberately NOT redirected. A sandboxed step writes nothing
live, but it is still one machine and one runner: `init` holds the live lock
while it copies the corpus, so it cannot pin a file mid-append, and every step
holds it across the tick with the tick nesting inside via
`FUTON_WM_RUN_LOCK_TOKEN`, exactly as `wm_run.sh`'s ticks do.

## The pin is the corpus, and two things are excluded on purpose

57 trace files, hashed into `pin/manifest.edn` (`wm_step_records.bb:77`). Not the
last record: three readers reach past it — `recent-trace-records 12`, the
cold-start habit prior that folds every file, and the ladder's case-history
index. `.run-lock` is excluded because replaying a pinned pid/pid-start-ms/token
re-asserts a dead holder; `.lane-futility-index.edn` is excluded because it is
**derived** and rebuilt when its `(name, length, mtime)` fingerprint goes stale
(`lane_futility.clj:82-86`) — restoring the corpus restores it by construction.

`reset` restores and then **verifies by content hash against the pin as it
stands** (`verify-dirs`, `wm_step_records.bb:99`). That is deliberately not the
manifest: the manifest answers a different question — *has an input moved under
the pin since the pin was taken?* — which `step` asks before it runs. Collapsing
the two made a legitimately advanced pin look like drift, and cost one debugging
round here.

## The evidence store: C509 said it could not be pinned; its answers can

C509 established that the tick's evidence input has no cursor — `:limit` plus
`:since <date>`, a sliding wall-clock window over a store that grows — and that
services cannot be pinned. The store cannot be. `wm_step_evidence.bb` pins what
the tick actually consumes: a recording GET seam reached through the EXISTING
`FUTON3C_EVIDENCE_BASE` configuration entry (`pattern_registry.clj:33-38`). The
first step fills the cassette from the live store; later steps are served the
same bytes.

Seven distinct URLs cover the whole tick. The positive control is `--frozen`,
which refuses to reach upstream: both determinism steps ran frozen with **10
hits, 0 fills, 0 misses and 0 non-GET requests** (`controls/cassette-frozen-011.edn`,
`-012.edn`). Two facts follow. The evidence input is completely pinned — not
"no symptom appeared", but "the seam was closed and nothing asked for anything
else". And the tick issued no POST, measured from the server side this time,
which is the pre-flight's claim arrived at by a second route.

The cassette is written only when something is recorded. Before that fix, the
shutdown hook rewrote `:cassette/saved-at` on every replay, so two frozen steps
that served identical answers left two different cassette shas in their step
records — a pin whose content hash flapped for a step that changed nothing.

## Determinism: identical, and the exclusion list is C509's, not a new one

`wm_step.sh determinism` runs two steps from one pin and compares the trace
records they appended, after scrubbing exactly the five things a tick mints
fresh: `:run/id`, `:timestamp`, `:startedAt`, and the per-hop route `:at` /
`:at_` stamps (C509 handoff item 4). `:at` is dropped only inside a route, never
wherever else it appears. Everything the tick decided — the ranking, every
controller score, the chosen action, the posterior, the precision state — is
inside the compared form, and the comparison is a sha over a canonical
serialisation (`wm_step_records.bb:312-395`).

**011-det-a and 012-det-b are identical**:
`98bdc350d97e785785b726e78550bfaf66f31a36f0d0bf15005ebb4fd8e7ec85` both, with 0
of 17 hashed world inputs moved and the commit census unchanged at 3762
(`controls/determinism-011-vs-012.edn`, `controls/world-drift-011-vs-012.edn`).
An earlier pair on the generation-0 pin, 008 and 009, is identical too
(`d8c444bf…`) — and there the mana snapshot's content DID move between the two
steps without moving the decision.

Two more comparisons are worth their space. 001-warm filled the cassette from
the live store; 002-det-a ran frozen off it; they are identical
(`controls/cassette-faithful-001-vs-002.edn`), which is what says the replay is
faithful rather than merely repeatable. And every one of these holds *within a
pin generation*: 011/012 differ from 008/009 because an `accept` advanced the
pin between them, which is the pin moving, not the machine wobbling.

## The planted divergent input is detected twice, and absorbed nowhere

`wm_step.sh plant <work> mu-uniform` perturbs ONE pinned datum: the last pinned
record's `:mu-post`, the strategic belief the next tick carries as its prior.
Each entity's channel distribution becomes the uniform one over the channels it
already had, so the domain and the shape are untouched and only the numbers
move (`controls/plant-mu-uniform.edn`: 417 entities, mu sha
`497c04ed…` → `0a13cec1…`).

1. **The tool refuses.** The next `step` verifies the pin against its manifest,
   names the changed file, and exits 4 without running a tick. Detection is a
   refusal, not a note a reader might skip.
2. **The decision diverges.** Forced through with `--allow-pin-drift`, the step
   produces `663380a44221…` against 002-det-a's `42dd426a9b21…`, and the
   comparison localises it to `[:decision :controller-ranking N :controller-score]`
   and `:selection-score` (`controls/planted-input-002-vs-005.edn`).

## The world that still moves, and how the tool learned to say so

Two steps ten minutes apart diverged while every hashed file input was
unchanged. The difference was confined to four fields of `:observation` —
`:stack-pct`, `:consulting-pct`, `:portfolio-pct`, `:mathematics-pct` —
and it is fully attributable: those channels are a `git log --oneline --since`
census over 18 sibling repos, folded into percentages
(`war_machine.clj:2794-2801,3652-3668,3828-3836`, read at
`observation.clj:53-56`). Reproducing that census by hand gives
stack 3749 / mathematics 3 / portfolio 5 / consulting 5 over a total of 3762,
whose ratios are **byte-identical** to the later step's four channel values
(0.9965443912812334, 7.974481658692185E-4, 0.0013290802764486975,
0.0013290802764486975). The earlier step's values are the same census over a
total of 3763 with stack 3750. One stack commit left the 14-day window between
the two steps. *Which repo and by what mechanism is not established.* No repo
in the list carries a commit dated inside that ten-minute window, so the change
is not a commit arriving; the candidate explanations for a commit LEAVING a
fixed date window — an amend, a rebase, a branch move under `HEAD` in one of the
eighteen — are untested and named as such rather than picked.

The tool now hashes that census with the world
(`wm_step_records.bb:196-227`), so the next such divergence is attributed by
`compare` instead of by hand. That is the whole reason it is there: an input a
comparison cannot see is an input a reader will explain wrongly.

**The mana-snapshot age hazard is recorded, not solved.** C509's sharpest
finding was that `:stale?` is an AGE — `> 60` minutes over the snapshot's mtime
— consumed to suppress a `:stop-the-line` override, so a byte-exact restore an
hour later runs the tick in a different MODE. Every step records the age it saw
and which side of the threshold it was on. Every step in this session ran under
5 minutes of age, so the hazard is **unexercised**: nothing here shows what the
stepper does across that boundary, and a step taken from this pin tomorrow will
not reproduce these records.

## The fix-defect loop, exercised for real

Not simulated. `src/futon2/aif/efe.clj:909` was edited to quantise
`:controller-score` to 0.5 (`defect-loop/PLANTED-DEFECT.diff`, verbatim), and
the loop ran:

| step | tree | RE7 `:selection-discrimination` | chosen candidate |
|---|---|---|---|
| 006-defect | quantised | **`:defect`** (deposits `:red`) | rank 1 inside a **136-wide** tie, band [1 136], field 146 |
| 007-fixed | reverted | **`:green`** | rank 1, tie **1** — the score picked it |

Nine RE7 controls pass in both directions, four of them negative
(`defect-loop/*/03-controls.edn`). This is the shape Joe named — "a 55-way tie
should be seen as an obvious defect" — planted, seen by a catalogued check
against a single step, fixed, and seen to be fixed after a reset and a re-step.
The committed tree never carried the defect; `wm_step.sh check` deposits nothing,
because a ledger row is a claim about an accepted run and a step under
inspection is not one.

## Accept: the pin advances, the run store is deposited, the battery runs

`accept` refuses a step whose tick did not complete, and refuses any step but
the most recent — every `step` resets the sandbox first, so only the last one's
post-state is still there to advance from. Reconstructing an older step's
post-state by replaying its records would be a second, unverified way of
building the state the machine actually had.

It then deposits `runs/2026-09-04-010-accepted` — the appended record as
`wm-trace-2026-09-04.edn`, the receipt, the RE4 rationale, the world census
before and after, and a README naming the sha — and runs all nine catalogued
checks against that single step (`battery.log`). Every check produced a verdict:
RUN3 conformant (9 hops, 2 drawn, 5 route-measured, 1 excluded at dependency
grain, 1 ruling-unrealised, 0 refutations), U49's transcription reproducing the
pinned verdict with C1-C4 and C7 PASS, RE7 `:green`. The pin advanced to
generation 2 and the sandbox was restored from it.

**The battery is two-pass, and that is the ledger's rule.** `append-row!`
refuses a row whose artifact is untracked or dirty — "a ledger row may only
point at a committed, unmodified file" — so the first pass produces receipts and
its deposits are refused, the operator commits the run store, and
`wm_step.sh deposit` re-runs the same battery so the rows land. Each check
rewrites its receipt byte-identically, so the second pass is a replay and not a
second measurement.

Two of the nine load only from the repo root, and that is a classpath fact:
`bb.edn` declares `:paths ["."]`, so `checks/contract_authority_current.clj`
resolves `writer-fence-capability` only from there, and
`u37_enumeration_replay.clj` needs `src/` on the classpath, which only
`clojure -M:test` gives it (as its own controls run it,
`u37_enumeration_controls.sh:31`). Run from the lab under `bb` they fail to
LOAD — and a check that failed to load reads exactly like a check that had
nothing to say. That was the first pass's result before the fix.

## What this does not do

- **One JVM per step**, as `wm_run.sh` does. Every `defonce` resets, so the
  eight atoms C509 censused are not inter-tick state here. A later in-JVM
  stepper — which is what would make steps fast — reintroduces three of them:
  the never-invalidated scan caches, the habit-seed `delay`, and
  `case-history-index-memo`, which is keyed by trace directory and so does NOT
  escape a redirected trace dir.
- **Only the tick's closure.** The seven store-owning namespaces C509 named as
  outside the closure (tripwire, repair-obligation, actuator-a3/a6, enact,
  close-loop, fold) are still outside it. Pointing the stepper at the full-loop
  click path is a different census and a different sandbox.
- **The five unparameterised absolute paths** C509 named remain
  unparameterised: a sandboxed step reads the real p4ng control map, the real
  curvature JSON, the real holes contract, the real pattern library and the real
  c-vector overlay. They are hashed, not redirected.
- No ruling: no `aif-equations.edn` `:choices` entry and no
  `control-map-edges.edn` `:decisions` entry. `gen_aif_dag.bb` was not run and
  nothing was regenerated into a publish (TN §9a).
- `runs/latest-conformance.edn` is modified: RUN3 rewrites it on every run, and
  the latest run3 conformance is now this step's.

## Gates

```text
clojure -X:test :nses '[run-tick-once-test]'          # 11 tests, 35 assertions, 0 failures
clj-kondo --lint scripts/futon2/run_tick_once.clj test/run_tick_once_test.clj
emacs -Q --batch -l futon4/dev/check-parens.el ...    # OK
clojure -M:test holes/labs/wm-contract/r6_zero_post_preflight.clj   # PASS, before the first step
bash p4ng/empirics-futon/negative_controls.sh
bb   p4ng/empirics-futon/pointer_check.bb
bb   holes/labs/wm-contract/worklist_check.bb
```
