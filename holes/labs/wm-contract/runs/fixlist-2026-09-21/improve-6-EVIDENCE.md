# improve-6 — machine-side evidence for accounts of learning and capability

2026-09-21, codex-11. Discovery only, branch `fix/narrative-improve-6`,
base `b25126c2`. No production edits, clicks, serving-JVM evaluation, or Lean
builds. This inventories evidence for Joe and claude-5's account design; it
does not settle their definitions.

**The machine can already join some failures to reviewed repairs. It cannot
yet automatically establish “surprise followed by structural model revision”.**
Repair resolutions have explicit finding IDs and replacement commits. They do
not classify the changed model part, establish that it was loaded and consumed,
or demonstrate that the original prediction changed. The attempt learning
ledger deliberately has no production A/B consumer.

## 1. Inventory and counting boundary

Reproduction scripts and machine-readable results are in [improve-6/](improve-6/).
`inventory.edn` records **1,345 files, 1,366 EDN forms, 328,483,225 bytes**, with
paths, SHA256s, event paths and extracted identities; no parse errors.
`summary.edn` provides date bounds, classes, pending findings and explicit joins.

Scope: canonical futon2 and futon3c `data/wm-runs`, `wm-full-loop*`,
`wm-d-task-enactment`, `wm-learning-trials`, and `wm-repair-obligations`, recursively,
`.edn` files only. This chooses original runs, checkpoints and the repair store.
It excludes trace/step/brief mirrors, `.edn.log` phase streams, proposal stores,
tripwire aggregates, arbitrary Agency job history, and scratch/worktree tests.
Thus these are **counts in the retained authoritative-file census, not a claim
of every historical incident in every store**. Legacy attempt-only repair IDs
can collide across cohorts; uniqueness in today’s files does not prove that no
earlier finding was overwritten. In particular, no transcript-only
surprises are included. Evidence notes elsewhere in the repository are used
below for explicit historical joins, not added to the event counts.

Dates are fields in the records, not filesystem mtimes. D-task measurement date
bounds use the retained review job's date, not an invented observation timestamp.
The fingerprint pass follows parsing: this is a read-only scan, not an atomic
snapshot of the serving filesystem. Git heads at the history scan are retained
in `history.json` (futon2 had advanced to `920ee7b2`; this branch stays at its
stated base). The result files freeze the observations used here.

| Record family | Count/unit | First / last dated occurrence | Interpretation and limits |
|---|---:|---|---|
| Token outcome `:predicted-not-observed` | 0 persisted comparison rows | — | The API exists; the historical mismatch below is reconstructed, not a newly retained comparison. |
| Token outcome `:not-predicted-observed` | 0 persisted comparison rows | — | Absence here is not evidence all predictions matched. |
| D-task signed-observation **v2 receipts** | 0 persisted schema matches | — | Boolean polarity is “signed”; no cryptographic-signature claim. |
| Raw D-task after-token evidence | 11 rows in 1 artifact: 6 true, 5 false | Sep 21 / Sep 21 | One selected attempt, not 11 independent trials. Observation semantics differ by token. |
| D-task artifacts | 13 | Sep 21 / Sep 21 for 2; 11 undated | 1 canonical artifact with measurements, 1 deferred artifact with none, 11 `/tmp/runner-token-initialization…` fixture artifacts with none. Do not count fixture residue as execution learning. |
| Repair/stop-line findings | 118 distinct repair IDs | Jul 14 / Sep 21 | Original finding files, not each repeated open-stop-line projection. |
| Independent reviewer rejections | 16 of those 118 | Jul 14 / Sep 21 | 15 `:request-changes`, 1 `:reject`; not an additional 16 incidents. Scope excludes rejections never minted into a finding. |
| Candidate/proposal declines | 3 appearances = 1 evidenced decline | Sep 21 / Sep 21 | `:repair-finding-evidence-missing`, target `T-repair-attempt-054`; twice in the selection checkpoint and once in its tick record. |
| Candidate `:no-new-wanted-token`, `:empty-cascade`, target-admission declines | 0 retained rows in scope | — | Fixes landed after the reference run; acceptance fixtures are not live refusals. |
| Learning-trial v1/v2 receipts, held or admitted rows | 0 persisted receipts/rows | — | Both canonical learning-ledger directories absent in this scan. Historical held result exists in improve-1b replay evidence, not a live ledger entry. |
| Other typed statuses | 42 appearances | Sep 20 / Sep 21; 11 undated | Details below; these are not 42 independent surprises. |

The 42 statuses are: `:carry-no-predecessor` 15 appearances in 8 files
(Sep 20–21); `:carry-domain-changed` 3 in 2 (Sep 21);
`:e2b/production-authority-unavailable` 9 in 6 (Sep 20–21);
`:recovered-artifact-not-fresh-execution` 4 in 3 (Sep 21); and 11 undated
`:observation-placement-not-declared` holds, one per fixture D-task artifact.
Those last holds are **not learning-trial rows**. No additional `:refusal`-map
matches were found. A correct refusal enforcing a known contract is not
necessarily a surprise to that contract's owner.

Repair classes: 79 machine failures (Jul 16–Sep 21), 20 environmental holds
(Jul 16–Sep 21), 16 independent-review failures (Jul 14–Sep 21), 2 system-actuation
failures (Jul 15), 1 incomplete-recoverable (Jul 16). There are 59 implementation
records and 59 resolution records (58 dated resolutions Jul 14–Sep 21; one
undated), 17 dismissals (Sep 19–21), 18 occurrence-evidence records and 8 each
verification/evidence records. Resolution and dismissal ID sets are disjoint;
42 findings have neither. Finding files can still say `:open` after resolution:
**join the stores before classifying current disposition**. In the Sep 7–21
window: 64 findings, 32 with neither resolution nor dismissal. These are store
states, not an independently verified count of outstanding real defects.

### What the evidence is about, and how to identify it

| Carrier | Possible model part challenged | Identity needed for a revision join |
|---|---|---|
| `token_outcome/compare-outcomes` prediction, token, verdict, measurement | B delivery prediction **conditional on** interpretation/route; A/locator semantics can instead explain the mismatch. Not directly a disagreement about C's value. | Run and occurrence; selected cascade/target; token; frozen predicted probability; artifact SHA; measurement/evidence digest; declaration, interpretation and guard/effect digests. |
| `d_predecessor_task_authority/verify-observations-v2`, raw D-task evidence | D's next initial state and the validity of the observation channel A; not independent evidence of an individual pattern firing. | `:action/id`, `:transition/id`, `:run/id`, `:cohort/id`, `:attempt/id`, canonical action identity version, dispatch digest, before/after revision, signed token, author/review job IDs. |
| Admission drops and cascade refusals | Feasibility/support declarations, horizon, target interpretation; `:no-new-wanted-token` rejects redundant predicted progress, not an observed B failure. | Source declaration revision/digest, target, candidate/order, stage, reason, initial/terminal wanted-token evidence and horizon. Run/occurrence scopes local `:C1`. |
| Repair finding / stop line | Usually runner, dispatch, artifact binding, construction or review contract; inspect failure data before assigning A/B/C/D. | `:repair/id`, newer `:repair/occurrence` origin/event/kind, attempt/cohort, failure stage/kind, selected entry, failed commit, backtrace. Bare `attempt-001` is not globally unique. |
| Held/admitted attempt learning trial | End-to-end selected-cascade delivery calibration, **not pattern causality**. Held means evidence inadmissible, not observed failure. | Trial grain and contract version, occurrence, selected cascade, target/effect, route and meaning digests, before/after observations at the same artifact revision, deduplication identity and ledger disposition. |
| Reviewer rejection | Implementation's claimed capability or conformity to a contract; may concern a proposed interpretation or model change, but rejection alone supplies no A/B sample. | Repair ID, failed artifact commit/repository, review job, reviewer/author separation, verdict and executed evidence, replacement commit plus subsequent independent review. |

Concrete observation: futon3c
`data/wm-d-task-enactment/action-a5d2326d-a73a-447c-8bc7-7f77c8bbc187.edn`
retains the Sep 21 reference occurrence and 11 rows. AIF has 1 true/3 false,
F11 1 true/2 false, F2 4 true. Only AIF was selected. Its updater token
`:hole/h6378c65a4012` was predicted positive but observed false at
`7a9daa0f2ef9850aff01f1631dbc93c59625b586`. C4 observes a mission checkbox
statement, not the truth of implementation acceptance. Hence the mismatch
challenges declared delivery/meaning, and does not uniquely diagnose B.
[improve-1-DISCOVERY.md](improve-1-DISCOVERY.md) reproduces the prediction;
[improve-1b-VALIDATION.md](improve-1b-VALIDATION.md) documents why the v1-legacy
occurrence remains **held `:occurrence-v2-required`**. The 11 raw observations
cannot be promoted to 11 admissible attempts.

## 2. Structural revisions versus refreshed data

A structural revision changes a carrier, possible outcomes, transition/likelihood
rule, observation meaning, preference ordering/domain, admissibility condition,
or execution/clock contract. New measurements at the same meaning, refreshed
counts, additional ledger rows, regenerated hashes and re-emitted manifests are
data refreshes. Updating theta under a fixed Beta model is parameter learning,
not automatically structural revision under Joe's working definition. Refactoring
the same function and adding observability need their own categories.

| Model part / authority | Revision sites and concrete commits | What is structural; what is not |
|---|---|---|
| C and wanted-token domain | `resources/wm/cascade-sources/*.edn`, `live_c.clj`; `55cab932` declared scales, `c322c22f` terminal family, `c4fd0afd` per-step evaluation, `f7f02c3c` explicit step-indexed C | Changing normalization domain, preference meaning or schedule is structural; recomputing masses at unchanged declared rules is a refresh. A deliberate owner preference revision need not be caused by surprise. |
| A / observation interpretation | Source locators, `observation` checks, `cascade_model_manifest.clj`; `19a3af6b` deletes phantom witness locators, `cee539e6` adds exact latent-mixture observation distribution | Changing what counts as observed, or introducing shared latent dependence, changes the model. New measurements under the same locator are data. Availability of mixture code is not evidence a click consumed it. |
| B / pattern effect meaning | Source `:interpretations`, `:guard`, `:produces`, `:transition`, interpretation receipts and admission ledgers; `6592c58b` target-specific readings, `cb2045b8` their admission limits | A new effect/guard or attributed meaning changes support/transition semantics. A new source hash alone does not. Current default theta is supplied; illustrative `learning-trial-prior.edn` and `56593d8c` ledger do not revise production B. |
| D / belief boundary | `token_belief_carry.clj`, `scoring_input_receipts.clj`, `token_initialization_policy.clj`; `c300c67b` staged carry, `08f58453` consumed frozen D, `5d4c3648` signed observations, `77fea6af` declared initialization policy | Switching positive-only carry to signed observation consumption changes the state-input contract. An updated belief at a fixed contract is a state refresh. Declared switch and actual consumption receipt must agree. |
| Trial/occurrence contract | `action_identity.clj`, `attempt_learning.clj`, `resources/wm/attempt-learning-contract.edn`; `4c3b4b7a`, `56593d8c` | Canonical v2 identity and attempt-grain admissibility are structural evidence-contract revisions, not learned pattern physics. New admitted +/- rows would update a record-only calibration ledger. |
| Runner / repair contracts | `full_loop_runner.clj`, `full_loop_cohort.clj`, `decision_gate.clj`, fold contract; `707e680f`, `561d761e`, `c91261fd`, `1d20ca88` | Repairs alter the machine's execution/evidence rules. Distinguish this from a change to the generative model used for selection. |
| Lean specification | `mathlib4/DarkTower/WarMachine/{CascadeEFE,PolicyHorizon,OutcomeRiskKL,DirichletLearning}.lean` and related modules | `3578a62212` conditioned/open-loop license, `782d9b05da` F comparison semantics, `b79463e0a2` ruled time-indexed C, `8569c576fb` Dirichlet/organise import contracts are specification changes. `4199d8ebe0` canonical manifest re-emission is not by itself new model semantics. No Lean build was run. |
| Rulings and authored accounts | `RULING-*.md`, FIXLIST, admission receipts, tracked repair closure notes | Identify who changed a meaning and why. A ruling is evidence of intent; a downstream declaration/code change and a consumed revision are separate events. |

Declarations under `data/` need the same field-level test. Much of `data/wm-*`
is runtime evidence, often untracked: changing such a file is not itself a
structural revision. A typed semantic diff and a named consumer are needed;
“file changed” or “commit happened” is insufficient. `history.json` retains
local futon2 and Lean histories for the two-week window, not just chosen examples.

## 3. Evidenced joins in the last two weeks

Paths below are relative to canonical futon2 unless another repository is named.
The full IDs, source files and replacement SHAs for **29 finding→resolution
pairs with either endpoint in Sep 7–21** are in `summary.edn :recent-joins`.
These 29 links are repair records, not 29 proven instances of structural learning.

| Surprise evidence | Revision evidence | What the join establishes |
|---|---|---|
| Eight Sep 20 `:fold-output-invalid` findings, including `findings/repair-occ-09fab69174ee3f5985f572f08db809e27603ceee7e77e300114aa429a182135c.edn` under `data/wm-repair-obligations/` | Same-ID resolutions name `c91261fdbc7b80e903d3203ca53cc865291cae9f` (ground ordinary cascade fold remainders in declared outputs); reviewed follow-up `77897664`; closure `24219601` | Explicit identity join to a construction contract repair. `runs/cascade-fold-repair-2026-09-20/CLOSING.md` records all eight closures, but **zero boxes and nil coverage delta still remained**. Schema validity is not successful construction. |
| `findings/repair-occ-f9f4e9708737cb15163f92d0d31419aae2097c5095ffdf6b5c4a3dc08d7057ba.edn`: poisoned selection artifact makes later initialization throw before any run receipt | Same-ID resolution → `707e680f8ef28dbdc40d4f4741a3ec14d75fb9e3`, follow-up `7fef1200`; `2ea86caf` retains `runs/history-admission-closing-2026-09-21/CLOSURE.md` | Runner history-admission contract revised: unreadable non-identity history can be recorded separately while unknown opportunity identity still refuses admission. Tested successor scope is scratch-history initialization, explicitly not a live click. |
| `findings/repair-occ-7737547f116c5976ba54e7cad66e32de8f1fb7c41bf88f36e9a9a8a5783a5be6.edn`, Sep 21 review-request-changes for `7ac238d4…`, run `1789952479` | Same-ID resolution names `561d761ea321adadf8aee3291e3cb8f38aee1c53`, “Validate guard observation locators by production class”; design/gates in `runs/decision-guard-locators-2026-09-21/` | Reviewer rejection joined to revised observation-locator validation. This is a contract correction, not a token-frequency update. |
| EoI's nonexistent C6 witness artifacts reported false; explicit operator finding in `RULING-witnesses-are-produced-artifacts-2026-09-21.md` | `19a3af6b` removes `:locators` from `resources/wm/cascade-sources/M-expressions-of-interest.edn`; ruling names introducing commit `e4a6084d` | Strong human surprise→observation-declaration revision. Wants remain, now unknown rather than false from nonexistent producer. Do not call deletion of locators removal of the wants. This is an authored ruling join, not a repair-store join. |
| Identical historical action hashes differently under `*print-namespace-maps*`; `fix-10c-then.edn`, `fix-10c-now.edn`, discovery `3b482e15` | `4c3b4b7a` versions occurrence identities with canonical encoding and explicit legacy replay | Identity-contract revision with reproducible bad case, not B revision. The discovery's old “runner digest matches” evidence must now be read with fix-20's disk-versus-loaded limitation. |
| Reference F2 cascade adds no wanted token within T=2; `fix-6-results.edn`, run `1789964661` | `187296bb` refuses no-new-want candidates using the existing rollout | Explicit discovery→admission revision; not yet a counted live refusal in this census. |
| Loaded-source probe cannot distinguish old functions from disk; old runner guard compared resource bytes at check time | `1d20ca88` load-time digest registry; `runs/fix-20-2026-09-21/{before.log,README.md}` | Runner identity-contract repair with failing-before control. Loaded/consumed status on the serving JVM remains unmeasured by this discovery. |
| AIF updater's positive prediction / negative checkbox observation in `1789964661` | `cab63449` comparisons, `e568e5b2` held trial receipts, `56593d8c` declared attempt ledger | Evidence-contract revisions in response to the gap. **No production B update**: legacy occurrence stays held. Do not claim the ledger closes the learning loop. |

### Repetition without an established consumed revision

* Eight fold failures were closed by the repair above, then
  `repair-occ-0576180e03115d74ce0a32eff59df655649d732e8cdc10f12a86f98b15f51879`
  at **Sep 21 00:56:55Z** again records `:fold-output-invalid`, with missing
  free/why/obligation-ID findings, target `:C1`, run
  `1789952178`. It has no resolution/dismissal in the scanned store. This is a
  **friction candidate**, not proof the patch failed: the records do not pin the
  loaded namespace closure, and sameness of failure kind is weaker than sameness
  of cause. There *was* an intervening source revision.
* Among the 32 recent findings without disposition joins are 6 initialization
  failures (Sep 11–21), 6 untyped failures (Sep 11–20), 5 agent-unavailable
  findings (Sep 11–14), and 2 artifact-binding mismatches (Sep 13–15). Full IDs
  and paths are in `:pending-findings`. These give candidates for account review,
  not an automatic conclusion “no one revised anything”: broad kinds conflate
  causes, stale/fixture findings exist, and repairs can be recorded elsewhere.
* The selected AIF mismatch and held legacy replay are the **same occurrence**,
  not repeated failures. EoI's earlier equal-G runs `1789951020` / `1789952479`
  are repeated nondiscrimination evidence (see fix-6), but equal scores under
  declared C are not themselves a surprise unless a discrimination expectation
  was declared. The missing distinction should not be supplied retrospectively.
* The declined `repair-attempt-054` persists from Jul 25 to the Sep 21 proposal
  record because its repair evidence is incomplete. Three copies of that decline
  show one selection's propagation, not three attempted repairs.

### What is missing for automatic learning/friction accounts

A small **proposed**, not implemented, join record could name:
`surprise-id + occurrence + model-part + old-model/meaning digest + predicted
and observed claims + revision-commit/declaration + new digest + revision-kind
+ authority/reviewer + loaded/consumed successor receipt + disposition`.
The fixing commit should cite the surprise ID; declarations should retain a
`supersedes`/reason link. Reuse repair IDs and canonical occurrence IDs instead
of generating identities from prose or local candidate names.

Required distinctions: structural vs parameter/state/data revision; operational
repair vs A/B/C/D revision; committed vs loaded vs consumed; held/unknown vs
negative; admitted observation vs fixture; repeated occurrence vs new occurrence;
rejection vs inability to obtain review; resolved vs dismissed as condition-cleared
or never-executed. Surprise itself needs a declared expectation and tolerance,
not merely an exception or low-probability-looking value. No universal mapping
from failure kind to model part currently exists. Credit for learning requires
an evidenced revision relation; credit for capability additionally requires an
appropriate successful successor, which several current closures explicitly lack.

## 4. August 30 facade example

The records reach back. The current paper `/home/joe/code/p4ng/sec-case-study-vetting.tex`
(§4) cites `NOTE-cascade-consumers-census.md`, the Aug 31 REPL snapshot,
`BUILD-ledger.md` and the PLoP vetting ledger. The consumer census was committed
as **`22379b5b` on Aug 30**, though its document heading says Aug 31; preserve
that distinction rather than inventing one exact observation clock.

Evidence chain:

1. Operator asks who consumes Learn's output. The census names the missing
   independently witnessed input since Jul 6, the nightly observer's last run
   Jul 5, and dormant writeback deposits. This is surprise about the claimed
   producer/consumer loop and its instrumentation, not an inferred Bayesian
   surprise about a token. The prose census is the retained discovery record;
   this scan did not independently replay the old observer's absence.
2. Instrumented WM-RUN2 measures **3 of 9 actual hops matching the drawing**,
   six unmapped, drawn edges fired 3/21. `BUILD-ledger.md`'s WM-RUN2 owner gate
   cites implementation `b458855` + `c3a3644`. **`ad80436d`** commits the measured
   mismatch and six proposed wiring amendments in `PREREG-war-machine.md §2f`.
   The conformance hole remains explicitly unwitnessed. This is a concrete
   account/specification revision following contradictory execution evidence;
   it is not evidence that Learn resumed.
3. The paper separately reports 2 unsupported closures in a 24-row obligation
   ledger, later corrected/resolved/withdrawn. Those are paper-vetting outcomes,
   not 24 machine trials or two repaired runtime transitions.

The supplied CSV covers **Jul 21–Sep 21**. `history.py` reads it without
regenerating or changing it. Its generator counts unique reachable commits
across all refs by UTC committer date; the Lean series is path-filtered to
`DarkTower/WarMachine/`. `war_machine` is exactly the futon2 column, not an
additional independent series.

| Seven-day window | futon2 / War Machine commits | War Machine Lean commits |
|---|---:|---:|
| Aug 23–29 | 59 | 16 |
| Aug 30–Sep 5 | 2,538 | 211 |

On Aug 30 itself: 248 / 38; Aug 31: 561 / 102. This is a large contemporaneous
change in recorded activity, **not a count of learning, capability gained, or a
causal estimate of the audit's effect**. Merges, bookkeeping and documentation
are included. A return to producing/consuming Learn outcomes would need a
separate successor witness. The facade episode supports the human-mediated
surprise→revised account chain; it does not establish a functioning learned A/B.

## 5. Reproduction and validation

From this worktree (all scripts read canonical inputs, write only stdout):

```sh
D=holes/labs/wm-contract/runs/fixlist-2026-09-21/improve-6
bb "$D/inventory.clj" > "$D/inventory.edn"
bb "$D/summarize.clj" "$D/inventory.edn" > "$D/summary.edn"
python3 "$D/history.py" > "$D/history.json"
clj-kondo --lint "$D/inventory.clj" "$D/summarize.clj"
emacs --batch -Q -l /home/joe/code/futon4/dev/check-parens.el -f arxana-check-parens-cli -- "$D/inventory.clj" "$D/summarize.clj"
```

Environment: Babashka 1.13.219, Python 3.12.3, local Linux workspace.
Fresh execution: inventory and summary exit 0; 0 parse errors; source file paths
unique, finding IDs unique, resolution IDs unique (asserted). Kondo: 0 errors,
0 warnings; check-parens: OK. `history.py` executed successfully. These are
read-only discovery scripts, not production namespaces; no production test
namespace changed and no test-suite warrant is claimed. The committed outputs
are the evidence, not a fabricated failing-on-main regression for a discovery.
