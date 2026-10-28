# Discovery: the present self-heal pipeline against the repair patterns

Date: 2026-10-28.  Scope: canonical checkouts only.  This is the S2 reading of
the present implementation; it does not propose a replacement pipeline.

## 1. Checking the README mapping

The README's sequence is accurate as a coarse inventory, but several pattern
matches are broader than the code warrants.

1. **Diagnose -> `locate-the-producer-first`: partial.** `prepare-diagnosis`
   chooses the injury that `prepare` will choose and calls the diagnosis
   producer (`src/futon2/aif/meta_self_heal_production.clj:1395-1426`).  The
   resulting evidence is bound to the transaction so that a changed ledger
   cannot repurpose it (`meta_self_heal_production.clj:1481-1500`).  This is a
   durable diagnosis step, but the coordinator does not check that the
   diagnosis located the first wrong producer, nor does it separately replay a
   retained input before diagnosis.  Thus the README's “roughly” is important
   (`../futon3/library/self-heal/README.md:92-93`).

2. **Acceptance -> `fix-the-red-before-the-repair`: right, with a limit.** The
   proposed test is run before repair and recorded as `:ran-before-repair`
   (`meta_self_heal_production.clj:799-818`); a passing test is refused before
   reviewer invocation (`:819-849`), and an independent reviewer must accept
   the immutable proposal/result identity (`:850-896`).  This is a real
   discriminating red-before boundary.  It does not itself ensure a path-level
   witness after enactment.

3. **Author/review/enact -> producer repair plus different-seat review:
   partial.** The work order requires three distinct sessions
   (`src/futon2/aif/meta_repair_coordinator.clj:40-68`), author returns a patch
   and worktree (`:502-534`), review must approve that same patch (`:535-563`),
   and enact must preserve the patch identity and supply a retry click
   (`:564-598`).  This establishes a different review seat and controlled
   activation.  It does **not** establish `repair-the-producer-not-the-checker`:
   neither the author receipt nor review contract classifies which file or
   producer was changed.  Enact is activation, not evidence that the producer
   rather than the checker was repaired.

4. **Retry -> `retry-only-the-repaired-phase`: partial, and misleading if read
   literally.** The retry must cite the enacted repair, patch, and retry click
   (`meta_repair_coordinator.clj:599-609`), and the live ordinary retry is
   authorized only for that ledger attribution
   (`meta_self_heal_production.clj:1527-1555`).  But this is an ordinary child
   retry after enactment, not an enforced replay of only the repaired phase.
   Moreover an author failure reopens the injury (`meta_repair_coordinator.clj:
   507-518`) and production may prepare a fresh whole attempt
   (`meta_self_heal_production.clj:1723-1778`).

5. **Medic disposition outside the pipeline: right.** The coordinator claims,
   authors, reviews, enacts, retries and discharges/reopens
   (`meta_repair_coordinator.clj:473-500,502-639`).  It uses medic authority to
   validate the ledger, but has no medic-disposition phase.  The README's
   boundary statement is accurate (`../futon3/library/self-heal/README.md:
   101-102`).

Steps the README inventory misses are: readiness/configuration checks
(`meta_self_heal_production.clj:1428-1479`); injury selection, deferred
transaction movement and stale-transaction retirement (`:1588-1624`);
diagnosis/acceptance-to-transaction binding (`:1625-1648`); claim
(`meta_repair_coordinator.clj:473-500`); sealed receipt verification and
resume-identity checks (`:73-114,137-187`); per-phase persistence checkpoints
(`meta_self_heal_production.clj:1694-1715`); failed-child observation
(`meta_repair_coordinator.clj:307-358`); and author-failure continuation to a
new attempt (`meta_self_heal_production.clj:1723-1778`).

## 2. Patterns said to be absent

- **`a-correct-refusal-is-not-the-defect`: partial under narrower names, absent
  as the semantic decision step.** Typed guards refuse invalid transaction,
  ledger, active injury, attempt, or repair identity before effects
  (`meta_repair_coordinator.clj:455-471`), and retry attribution can refuse
  nonmatching ordinary work (`meta_self_heal_production.clj:1527-1555`).  These
  are real “premise unproved, stop” cases.  There is no branch that determines
  that the observed refusal is correct behavior and reclassifies the alleged
  injury; searches for `misclass` and `premise unproved` in both implementation
  files found nothing.  So README lines 104-105 are too absolute about guards,
  but correct about the missing repair/no-repair classification.

- **`witness-the-path-not-the-helper`: absent.** The pre-repair acceptance run
  and post-enact retry are retained, but no step requires a seam-crossing/path
  witness, negative control, or comparison against a helper-only test.  Searches
  for `path witness` and `negative-control` in both files found nothing.  Patch
  and retry identity checks (`meta_repair_coordinator.clj:573-609`) are
  attribution, not a path witness.

- **`a-step-that-did-nothing-is-an-observation`: partial under receipt and
  checkpoint guards.** A reused phase receipt is retained rather than silently
  redispatched (`meta_repair_coordinator.clj:253-280`); a receipt not admitted
  by the ledger is refused (`:137-166`); and every checkpoint must persist both
  ledger and transaction (`meta_self_heal_production.clj:1694-1715`).  A failed
  retry child can also become a pinned `:retry-inconclusive` observation
  (`meta_repair_coordinator.clj:307-358`).  What is absent is the general pattern:
  no phase declares an expected state change and compares before/after state so
  that a successful no-op becomes a new finding.

- **`take-in-what-no-step-predicted`: partial analogue only.** The failed-child
  ingestion above records an uncompleted retry, and author failure is retained
  before a new attempt (`meta_repair_coordinator.clj:507-518`).  Neither compares
  the result with a phase prediction or generically admits an unpredicted fact.

- **`match-the-finding-against-the-library`: absent.** No implementation step
  looks up a repair-pattern library or matches a new observation to one.
  Searches for `pattern library` and `introduce-pattern` in both files found
  nothing.

- **`introduce-a-pattern-from-the-repair`: absent.** The code can append a
  ledger observation, but cannot author/review/install a repair pattern in the
  library.  Thus all three closing patterns remain absent as compositional
  steps, despite the two narrow receipt/observation analogues above.

## 3. The `:self-heal` outer-policy arm and G

The declaration has a `:self-heal` arm with slots `:repair`, `:acceptance`, and
`:retry`, but no policy values are filled there
(`../futon3/library/meta/meta-outer-policy-cascade.edn:34-39`); the file contains
only slot/arm/generative declarations (`:1-86`).  In the canonical `futon2`
checkout I found no `meta_live_outer_selector.clj` or `outer_canonical_g.clj`,
and a search of `futon2/src` for `compare-at-horizon` found calls only in
`src/futon2/aif/wm/family_canonical_g.clj:89,139`, not in self-heal.  Therefore
the live `:self-heal` arm is currently filled with **no policies**.

The canonical mathematical G is Lean's alias
`cascadeGrainG := CascadeCoApplyGNovelty.scoreCascadeCoN`
(`../mathlib4/DarkTower/WarMachine/GOverCascades.lean:59-67`).  Its runtime
implementation scores candidates in `score-one` and exposes
`compare-at-horizon` (`src/futon2/aif/cascade_coapply_g.clj:56-98`); the current
ordinary family call sites invoke it in `family_canonical_g.clj:75-90,125-140`.

Self-heal does not reach that call chain.  The live runtime arbitrates an active
META injury before ordinary policy construction
(`src/futon2/aif/full_loop_runtime.clj:743-824`), while `route!` returns nil only
when there is no active injury and otherwise marks every disposition
`:ordinary-selection-called? false`
(`meta_self_heal_production.clj:1557-1587,1660-1677`).  Hence self-heal is
routed by an injury guard with **no G comparison** against ordinary candidates.
The README/E reading that one filled self-heal candidate is scored by the same
G describes the intended S1-S7 design, not present live code.

I used `DISCOVERY-meta-cascade-g-2026-10-28.md` as the starting map, then
rechecked canonical source.  Its cited live outer-selector files exist in
noncanonical worktrees but not canonical `futon2`; per the task constraint I did
not use those copies as evidence.

## 4. Existing records and the S5 gap

Already retained:

- A sealed transaction contains the sealed work order and an ordered receipt
  vector (`meta_repair_coordinator.clj:99-114,202-249`).
- Each port receipt is stored beside the repair by repair/attempt/phase and is
  checked for phase, injury, repair, attempt, predecessor, status and seal
  (`:73-97`). `invoke-once` appends it once (`:253-280`).
- Ledger transitions bind claim/session identities (`:473-500`), authored patch
  and author receipt (`:524-534`), reviewer verdict/seat and receipt (`:550-563`),
  enacted retry identity and receipt (`:582-598`), and discharge/reopen to the
  retry receipt (`:615-639`).
- Production checkpoint receipts retain phase plus ledger and transaction pins
  (`meta_self_heal_production.clj:1694-1715`); the final disposition retains
  status, reason, phase, repair identity and those receipts (`:1660-1693`).

These records can name the last attempted phase and distinguish several
failures. A typed refusal before port invocation identifies a **guard** failure;
a failed/rejected sealed receipt identifies that phase's attempted **effect**;
a missing or mismatched ledger receipt pin identifies failure to admit the
effect; and a persistence refusal identifies the checkpoint write. They also
support exact replay/attribution through predecessor pins.

What they cannot say is whether a repair-pattern step's guard, declared effect,
or prediction failed, because no selected pattern/cascade identity, guard
evaluation, declared before/after effect, or predicted observation is recorded.
Diagnosis and acceptance evidence are pinned into the work order, but they are
not transaction phases.  There is also no unified record relating a path-level
post-repair witness to the pre-repair red, and no closing record saying whether
an unexpected finding matched an existing pattern or caused a reviewed pattern
extension.  Those are the missing S5 fields/steps; the raw transaction, port,
ledger and checkpoint receipts should remain their evidence inputs.

## Search boundary

I read the named README, cascade declaration, discovery starting point, both
canonical self-heal implementation files, the live runtime arbitration, the
canonical family G wrapper, and the Lean G definition.  I searched canonical
`futon2/src` for the G callers and the two implementation files for the absent
branch/step terms listed above.  I did not inspect tests, historical run data,
noncanonical worktrees, or medic implementation internals.
