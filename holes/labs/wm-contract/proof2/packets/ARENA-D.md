# ARENA-D — reader-complete arena absence packet (implementation spec)

2026-09-27. Author kimi-7 (E-kimi-task-76, discovery for claude-9, PROOF-2a ⟨2⟩2d).
Discovery only; no code, registry, map, or plan changes.
All file:line references are at futon2 HEAD `94113d29bca7d33e91a4265849dc2fc35b0a5e14`
unless a historical pin is named. LP-A-D was written against pin
`e7bb57ea6cdd50054a73a2f8df9362ed0585448e`; **the tree has moved**, and the
first finding of this packet is that most of what LP-A-D §3 recommended has
already landed. This spec therefore scopes the *residual*.

## 0. Premise correction — the landscape at HEAD

Three commits by Joseph Corneli landed after LP-A-D's pin and implement the
bulk of its §3 recommendation:

- `bf33c4799` (2026-09-02) "AC2: belief aggregation stops substituting
  zeros — omit absent, reject malformed entries" — the R3d driver
  aggregation (`r3d-aggregate-driver`) already omits absent channels and
  rejects malformed entries one at a time rather than substituting zeros.
- `443a24b0f` (2026-09-27) "Preserve arena belief refusals through carry
  and prediction readers" — `normalise` no longer returns a uniform prior
  at zero total; it returns a typed refusal. `update-step` and
  `categorical-filter-step` validate and refuse as data. Readers
  (`entity-expected-health`, the four channel predictions via
  `belief-absence`, `reconcile-belief-carry`, trace `:mu-post`) propagate
  the refusal. **This commit also replaced C9's throw with typed
  refusals** (see §2).
- `3b76aa8a5` (2026-09-27) "Distinguish explicitly unavailable arena
  models from omitted defaults" — an explicit `nil`
  `:observation-model`/`:transition-model` in opts now refuses
  (`:invalid-observation-model` / `:invalid-transition-model`) instead of
  silently selecting the default.

Consequences for the requisition's framing:

1. The "dense impossible observation whose zero total `normalise` turns
   into a uniform distribution" **no longer exists at HEAD**. The witness
   now yields `{:status :refused :reason :impossible-observation}` and
   that refusal is pinned through health, carry, prediction, and trace by
   `test/futon2/aif/belief_refusal_continuity_test.clj`
   (`impossible-evidence-remains-refused-through-readers`).
2. The "sparse-valid A whose missing cell is read as 0" **no longer
   validates**. `valid-observation-model?` (belief.clj:249-260) now
   requires every row's key set to equal `status-set` *before* the column
   sum is checked, so LP-A-D's sparse identity matrix fails validation and
   `categorical-filter-step` returns `:invalid-observation-model`
   (`missing-likelihood-is-not-zero-likelihood` test). The
   `(get row s 0.0)` default LP-A-D named at pin:254 is still present at
   belief.clj:257 but is unreachable for any model that passes the key-set
   check; it is dead defensive code, not a live silent default.
3. "C9's existing malformed-model throw contract" — the throw itself is
   gone (see §2). What must be preserved is the C9 *fail-closed* ruling,
   not the exception mechanism. The requisition's "placed relative to it
   by explicit agreement" is now moot in form but still required in
   substance: the classification ordering (missing cell decided at
   validation, zero-total decided at update) is already implemented and
   pinned, and the implementation packet must not regress it.

The residual work is small and specific: **one live-path reader still
crashes on a refused entity, and the provenance/trace layer has a census
slot with no producer.** Details below.

## 1. Production callers of the belief-update entries

Entries that can hit the missing-cell or zero-total path
(`update-step` belief.clj:315-332, `normalise` 54-61,
`categorical-filter-step` 334-353, `update-entity-belief` 370-428,
`update-belief` 430-439, `update-belief-batch` 441-452):

| Caller | Site | Live tick path? |
| --- | --- | --- |
| `apply-arena-belief-events` | war_machine.clj:844-850 → `belief/update-belief-batch` with `(arena-belief-update-opts)` (837-842; mode from `FUTON_WM_LIKELIHOOD_MODE`, default `:aif`) | **Yes** — the named seam for all arena belief updates |
| `apply-morning-brief-events` | war_machine.clj:853-869 → `apply-arena-belief-events` (864) | **Yes** — judge input fold |
| Judge R3d microstep | war_machine.clj:7357-7359 → `apply-arena-belief-events` | **Yes** — per-microstep attribution update inside `judge` |
| Forward model rollout | forward_model.clj:425 → `belief/update-belief-batch` with caller-supplied `belief-update-opts` | **Yes** — R4 scores every candidate at T via `fm/predict-multi-horizon` (war_machine.clj:5937); `arena-belief-update-opts`' docstring (837-843) states the one opts map is shared by the live filter and rollouts |
| `code_build_match.clj:29` | names `futon2.aif.belief/update-belief` as a code-var for matching | No — reference only |

No other production namespace calls `update-step` or `normalise` directly
(grep over `src/` + `scripts/` at HEAD). `predict-step` (belief.clj:302-313)
is only called from `categorical-filter-step` and is downstream of its
validation.

## 2. Missing-cell default and the C9 contract

**Where a missing A cell was defaulted.** Pin `e7bb57ea`: validator line
254 `(double (get row s 0.0))` and update line 319
`(get-in A [observed s] 0.0)` both read a missing cell as `0.0`. At HEAD:

- belief.clj:257 retains `(double (get row s 0.0))` inside
  `valid-observation-model?`, but the row key-set check at 253-254
  (`(= status-set (set (keys %)))` per row) runs first, so a model with a
  missing cell fails validation outright. The default is unreachable.
- `update-step` (belief.clj:330) reads `(get-in A [observed s])` with **no
  default** and calls `(double a)` on it; it is safe only because
  `update-step` validates `A` at 324-325 before the arithmetic. Order
  matters and is load-bearing: validation precedes the read.

**C9.** The contract text is `holes/missions/M-aif-a-matrix-faithfulness.md`
completion criterion C9 (≈187-190): "Malformed or unavailable model data
fails closed rather than silently selecting `:legacy`." At LP-A-D's pin the
mechanism was an exception — `categorical-filter-step` threw
`(ex-info "Invalid observation model A (fail-closed)" …)` /
`"Invalid transition model B (fail-closed)"` at pin:339-342, introduced by
`863dea72c` (2026-07-13). At HEAD the throw is gone:
`categorical-filter-step` (belief.clj:334-353) returns typed refusals
`:invalid-prior`, `:invalid-observation-model`,
`:invalid-transition-model`, `:unknown-observation`, and `update-step`
adds `:invalid-observation-weight`. The mechanism change is Joe's own
`443a24b0f`; fail-closed is preserved, the signal is now data.

Which inputs do what today (all pinned in
`belief_refusal_continuity_test.clj` and `belief_test.clj`):

| Input | Today |
| --- | --- |
| A row missing a cell (sparse A) | refused `:invalid-observation-model` at validation |
| A column sums ≠ 1, non-finite, out-of-[0,1] entries | refused `:invalid-observation-model` |
| Explicit `nil` model in opts | refused (`3b76aa8a5`); omitted key selects the default |
| Malformed B | refused `:invalid-transition-model` |
| Event type outside `status-set` | `categorical-filter-step`: refused `:unknown-observation`; `update-entity-belief`: posterior unchanged (documented totality over wider event streams, belief.clj:376-379) |
| Non-finite/negative weight | refused `:invalid-observation-weight` |
| Dense impossible observation (zero posterior mass) | refused `:impossible-observation` from `normalise` |
| Valid prior that is not a distribution | refused `:invalid-prior` |

Nothing on the `:aif` path is silently defaulted any more. The `:legacy`
and `:a-matrix` modes of `update-entity-belief` (394-423) still call the
old `normalise`, which now also refuses at zero total — an unadvertised
but fail-closed change to those comparison paths; the implementation
packet should note it in the trace docs, not revert it.

## 3. Typed outcomes (already the HEAD shapes — keep them)

(a) **Missing cell** → model-level refusal, decided **at validation,
before** any update arithmetic (and therefore before the old C9 throw
site, which it replaces):

```clojure
{:status :refused :reason :invalid-observation-model}
```

Reason: a missing cell is a malformed-model fact about A, not a fact
about the observation; classifying it at validation keeps it disjoint
from an explicit measured zero (which is a value, per LP-A-D §1's
authority reading of `fe55a1a0b0`) and from a zero-total outcome. This
ordering is already implemented and pinned by
`missing-likelihood-is-not-zero-likelihood`; the packet preserves it.

(b) **Zero total** → per-entity refusal, decided **at update** in
`normalise` (belief.clj:54-61):

```clojure
{:status :refused :reason :impossible-observation}
```

Reason: zero total depends on q and the event, not only on A, so it
cannot be decided at validation. It is per-entity (one entity's
impossibility must not refuse the population — contrast
`r3d-aggregate-driver`'s per-entry rejection, war_machine.clj:7311-7317
comment). Both refusals are sticky: every entry point short-circuits on
`belief-refusal?` input, so a refusal is never silently healed by a later
valid event (pinned by the continuity test's `next-post` assertion).

Placement relative to C9: (a) is decided *instead of* the old throw site
(same fail-closed ruling, data mechanism); (b) is *after* it — the model
was valid, the observation was impossible under it. No new decision point
is introduced.

## 4. Downstream readers and the residual gaps

Reader contract for an absent entity, per LP-A-D and now HEAD: retain
entity identity with typed absence; no uniform substitution, no
previous-belief substitution, no fabricated numeric health.

| Reader | Site | HEAD behaviour | Status |
| --- | --- | --- | --- |
| Entity update result | `update-belief`/`update-belief-batch` (430-452) | refusal assoc'd under the eid; sticky on later events | Done (`443a24b0f`) |
| Health aggregation | `entity-expected-health` (638-653) returns the refusal unchanged; `predict-annotation-health` (655-687) and the other three channels guard via `belief-absence` (632-636) and return `{:status :refused :reason :belief-unavailable :entity-id … :cause …}` with no `:mean` | Done, pinned |
| Prediction errors | `fe/compute-prediction-error` propagates `:refused` | Done, pinned by continuity test |
| Belief carry | `reconcile-belief-carry` (517-531) carries a refused posterior to the next tick (survivors keep carried value) | Done, pinned |
| Trace record | `trace.clj:445` `:mu-post` round-trips the refusal map | Done, pinned |
| Arena provenance | war_machine.clj:1052-1053 stamps `:likelihood-mode` + `:belief-model-manifest` (model hashes) only | **Gap G2** — no per-occurrence outcome classification |
| **Judge R3d per-entity attribution** | war_machine.clj:7343-7356 | **Gap G1 — live crash.** `incons` maps every `[eid p]` through `(belief/entity-expected-health p)` and then does `(- 1.0 h)` / uses `h` as a number. For a refused `p`, HEAD's `entity-expected-health` returns the refusal *map*, so `(- 1.0 h)` throws `ClassCastException`. Once any entity carries a refusal into a tick (G-preserved by `reconcile-belief-carry`), the first microstep with positive `event-weight` crashes the judge. This is exactly the CCE LP-A-D's probe predicted, still present at the one site `443a24b0f` did not reach. | **To fix** |
| Forward-model rollout `next-belief` | forward_model.clj:425-430 | threads refusals into candidate scoring; refusal-safe only insofar as candidate scoring never does arithmetic on `next-belief` posteriors | **To verify in implementation** (no crash witness found by reading; no test pins it) |
| `entropy` (461-469), `most-likely-status` (454-459) | public, unguarded — `entropy` on a refusal map throws (`pos?` on a keyword) | All internal callers route through `belief-absence` first; decide and document the public contract (recommended: refuse as data, matching the rest) |

**G1 fix shape (spec):** in the `incons` comprehension, omit refused
entities with an explicit omission receipt, and renormalise the remaining
weights so the mean is still `event-weight`:

```clojure
attributable   = entities whose posterior is not a refusal
omitted        = [eid ...] with reason :belief-refused (present-only key)
incons weights computed over attributable only; total/n over attributable
```

The omission list rides in the microstep-trace step entry beside
`driver-omissions`/`driver-rejections` (the present-only pattern already
established at war_machine.clj:7361-7368). No numeric health is invented
for omitted entities; their posterior stays refused through
`apply-arena-belief-events` (sticky refusal), which is the honest outcome.

**G2 fix shape (spec):** produce a belief-refusal census in
`judge-output` and persist it. The trace schema **already has the slot**:
`trace.clj:542` writes `:task-belief-refusals` from
`(:task-belief-refusals judge-output)` — but grep at HEAD finds **no
producer** of that key anywhere in `src/` or `scripts/`; the slot is
always nil. The census should be `{eid :reason}` (present-only: absent
when empty), produced at the judge boundary after the final
`apply-arena-belief-events`, so the provenance stamp (mode + model
manifest, 1052-1053) is complemented by the occurrence record the model
hash cannot express. Do not overload the model hash for this (LP-A-D's
point, confirmed).

## 5. Test list for the implementation

Existing pins (do not regress):

- `belief_refusal_continuity_test.clj`:
  `impossible-evidence-remains-refused-through-readers` (witness (b)
  through real validator/filter → health → carry → next update → trace
  `:mu-post` → `predict-observation` → `compute-prediction-error`);
  `missing-likelihood-is-not-zero-likelihood` (witness (a) through the
  real validator); `explicit-missing-model-does-not-select-the-default`.
- `belief_test.clj` v0.25 block (1003+): validators, column
  normalisation, legacy-reduction (`a-matrix-legacy-reduction-theorem-test`,
  607), `r3d-flag-off-byte-identical` (715), `r3d-absent-channel-is-omitted-not-zero` (798).

New tests required:

1. **G1 judge witness**: a carried belief state containing one refused
   entity through the real judge microstep (the war_machine.clj:7330-7360
   loop, driven as the existing war-machine tests drive judge seams) —
   assert no throw, the refused eid appears in the step entry's omission
   list, remaining entities' weights renormalise to mean `event-weight`,
   and the refused posterior is unchanged in `belief'`.
2. **G2 census witness**: a tick with a refused entity writes
   `:task-belief-refusals {eid :impossible-observation}` in the trace
   record (through `trace/trace-record`, extending the continuity test's
   round-trip), and a refusal-free tick writes no such key.
3. **R4 rollout witness**: `fm/predict-multi-horizon` with a refused
   entity in `belief` — assert typed propagation, no arithmetic on the
   refusal (this pins the to-verify row in §4 either way the reading
   resolves).
4. **Shipped-A healthy output byte-identical**: **no existing test or
   fixture pins whole-tick shipped-A arena output byte-identically.** The
   closest pins are `r3d-flag-off-byte-identical` (aggregation only) and
   the offline `scripts/a_matrix_shadow.bb` /
   `holes/labs/M-aif-faithfulness/a-matrix-shadow.edn` comparison (legacy
   vs :a-matrix, not a tick-output fixture). The implementation must
   therefore add a healthy-path witness: shipped `observation-model-v1` /
   `transition-model-v1` / uniform prior through `apply-arena-belief-events`
   with a normal strengthened/foreclosed event, asserting the posterior
   is numerically identical before/after the change (equality of the
   double-valued maps is sufficient; no byte fixture exists to reuse).
5. **C9 cases unchanged**: extend nothing — the three refusal-continuity
   tests already pin malformed-model, explicit-nil, and
   impossible-observation behaviour; run them as the regression gate.

## 6. Size estimate and split

Residual implementation is small: G1 (the `incons` comprehension +
omission receipt in the step entry, ~25 lines + test), G2 (census at the
judge boundary + the already-slotted trace key, ~15 lines + test), R4
verification + pin (test only, possibly zero code), healthy-path witness
(test only). Call it **~50 lines of production change across two files
(war_machine.clj, possibly belief.clj docstrings) plus ~150 lines of
test**, one packet, no split needed. If the R4 reading surfaces a real
consumer doing arithmetic on refused posteriors, that becomes a second
small packet (rollout reader), separable from G1/G2.

Do **not** re-open: the `normalise` zero-total semantics, the validator
key-set check, the throw→refusal mechanism change, or the `:legacy` /
`:a-matrix` comparison paths' new refusal behaviour (note it in the
packet's trace-docs sentence instead).

## Appendix — line index at HEAD `94113d29b`

belief.clj: `belief-refusal`/`belief-refusal?` 51-52 · `normalise` 54-61 ·
`valid-observation-model?` 249-260 (unreachable `0.0` default 257) ·
`predict-step` 302-313 · `update-step` 315-332 ·
`categorical-filter-step` 334-353 · `update-entity-belief` 370-428 ·
`update-belief` 430-439 · `update-belief-batch` 441-452 ·
`most-likely-status` 454-459 · `entropy` 461-469 ·
`reconcile-belief-carry` 517-531 · `belief-absence` 632-636 ·
`entity-expected-health` 638-653 · `predict-annotation-health` 655-687 ·
`predict-observation` 1222.
war_machine.clj: `arena-belief-update-opts` 837-842 ·
`apply-arena-belief-events` 844-850 · `apply-morning-brief-events`
853-869 · provenance manifest 1052-1053 · carry read 7214-7220 ·
R3d attribution loop (G1) 7343-7356 · microstep belief update 7357-7359 ·
R4 rollout call 5937.
trace.clj: `:mu-post` 445 · `:task-belief-refusals` slot 542.
forward_model.clj: rollout belief update 425.
History: throw introduced `863dea72c` (pin:339-342) · AC2 aggregation
`bf33c4799` · refusal propagation + throw removal `443a24b0f` ·
explicit-nil distinction `3b76aa8a5`.
