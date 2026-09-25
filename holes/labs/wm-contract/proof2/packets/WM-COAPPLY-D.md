# WM-COAPPLY-D — step 14, the co-application kernel in the rollout (discovery, read-only)

claude-10, 2026-09-25, for claude-8 (packet WM-COAPPLY-D). Read at futon2
49a2b735, mathlib4 759b8ca884 (`CoApplicationKernel.lean` last changed in
69c2432f2b), and futon3c c2551db1 (the map and the matrix). No code, no test,
no data/, no flight.

## 1. The Lean object

**The pattern** (`CascadeTransition.lean:22-28`). `InterpretedPattern` is
`consumes`, `produces`, `forbids` (token sets) and `theta` in [0,1]. `guard p s`
(`:34-35`) holds when `consumes ⊆ s`, `forbids` is disjoint from `s`, and
`produces ⊄ s`.

**The Clojure counterpart** is a pattern map in the rollout:
- `cascade_model_manifest.clj:122` `guard-holds?`;
- `:282-291` `with-pattern-theta`: `:theta` defaults to 1, recorded as
  `:theta-source :documented-default`.

**The list kernel.**
- **Lean:** `patternKernel p s s'` (`:42-43`) moves to `s ∪ produces` with
  probability θ and stays at `s` with 1−θ. `firstEnabled` (`:76-78`) is the
  first pattern of the precedence list whose guard holds. `cascadeKernel`
  (`:102-106`) is the pattern kernel of the first enabled pattern, or the
  identity when none is enabled.
- **Clojure:** `pattern-kernel` (`:293-311`), `first-enabled` (`:327-332`),
  `cascade-kernel` (`:354-360`). The rollout's `push-forward` (`:364-387`)
  sums `mass × kernel row` over the current belief, and `rollout*`
  (`:389-398`) applies it for `n` steps, taking step k's precedence from
  `precedence-fn`. Its model is named `:first-enabled-union-theta-v1`
  (`:386`; `efe.clj:1389`).

**The co-application kernel** (`CoApplicationKernel.lean`):
- **Units and order.** Patterns are indexed by units `ι`, each with its
  pattern `pat u`. The descent order `r` is a relation on units; `Reach r p q`
  means q is strictly above p.
- **The enabled frontier** (`:90-94`): the units whose guard holds at `s`,
  with no unit whose guard holds strictly above them.
- **`coApplyKernel pat r s s'`** (`:96-103`) sums over every subset `S` of the
  frontier (the units that succeed) the weight `∏_{S} θ · ∏_{F∖S} (1−θ)`,
  moving to `s ∪ ⋃_{p∈S} produces_p`.
- **The conflict flag.** `frontierConflict` (`:105-111`) holds when two
  frontier units conflict, one producing a token the other forbids. The
  kernel still co-applies, and "the certificate carries the flag".
- **The equality.** `chainCondition pat r precedence` (`:118-127`) says: at
  every state, the frontier is empty exactly when `firstEnabled` is `none`,
  and is `{p}` exactly when `pat p` is first-enabled. Under it,
  `coApplyKernel_eq_cascadeKernel_of_chain` (`:174-184`) gives
  `coApplyKernel pat r s s' = cascadeKernel precedence s s'` for all `s`,
  `s'`.
- **The fixture where they differ** (`:186-313`, ConflictFixture). p produces
  `a` and forbids `b`; q produces `b` and forbids `a`; the order is empty.
  Co-application reaches `{a, b}` from `∅` in one step with probability
  `θ_p·θ_q`, 0.64 at θ = 0.8 (`coApply_ne_zero`, `:305-313`). Both list
  orders give 0 (`list_pq_zero`, `list_qp_zero`, `:252-278`).

**The Clojure data these correspond to.** The units and `r` are the
construction receipt's `:order` from `construction/containment-order`
(`construction.clj:113-135`): `:units [{:unit u :pattern id} …]` and
`:descent [[above below] …]`. The unit above contains the one below, meaning
it produces a token the lower unit's guard needs. `pat u` is the pattern map
for `u`'s `:pattern` in the candidate's `:precedence`.

## 2. The Clojure site today

- **Where the order is read.** `rank-cascade-actions` (`efe.clj:1056-`) calls
  `order-use` (`:1006-1054`) for each candidate (`:1209`). It then scores
  `cascade-manifest/horizon-g-sparse-cert` with
  `:precedence-fn (constantly (:precedence ou))` (`:1211-1214`), and records
  `(:meta ou)` as the ranked entry's `:order-use` (`:1258`).
- **A non-chain `:order` with no violations** (`:1041-1047`: the chain test
  counts below-sets; when it fails):
  - It is **scored by the list kernel over the candidate's own
    `:precedence`**, with
    `:order-use {:order-not-used :not-a-chain :needs :co-application-kernel}`
    on the entry.
  - It is not refused, and no refusal kind is reached, so nothing belongs on
    the register.
  - The G is the list kernel's G, labelled as such. `order-use`'s docstring
    says the case is "recorded, never silently scored as a list"
    (`:1026-1028`); more exactly, it is scored as a list, and the record says
    so.
- **Where orders exist.** The `:order` is attached only to constructed
  candidates (`interpretation_construction.clj:319`, since 7a5f6c0b,
  2026-09-25 02:49Z). None of the 67 run records under `data/wm-runs/`
  carries `:descent`, `:precedence-violations` or `:order-use`: no tick since
  that commit has constructed a candidate.
- **Tests.** The one non-chain case is
  `test/futon2/aif/order_kernel_test.clj:41` (`:independent`, units A and C
  with no descent), asserting the label at `:67`.

## 3. What a push-forward implementation is, minimally

- **`co-apply-kernel`** in `cascade_model_manifest.clj`, beside
  `cascade-kernel`, taking `[units descent pattern-of state]`:
  - the frontier: the units whose pattern's `guard-holds?`, with no unit
    whose guard holds strictly above them, by reach over `:descent`
    (`[above below]`);
  - the row: the sparse map `{s' mass}` summed over subsets of the frontier,
    with exact rationals, as `pattern-kernel` keeps them;
  - an empty frontier gives `{state 1}`;
  - refusals as `pattern-kernel`'s: a pattern without interpretation
    (`missing-interpretation`), or θ outside [0,1].
- **`evaluate-state` dispatches** on what `precedence-fn` returns for the step:
  a vector is the list kernel as today; `{:co-apply {:units … :descent …}}` is
  the co-application kernel. Its evaluation record names
  `:kernel-kind :co-application` and model semantics
  `:co-application-frontier-theta-v1`, and carries `:frontier-conflict true`
  when the Lean's `frontierConflict` holds (`:105-111`).
- **`order-use`'s non-chain branch** returns
  `{:co-apply {:units … :descent … :patterns …} :meta {:order :co-application}}`,
  and `rank-cascade-actions` passes it to `precedence-fn`.
- **Chains are untouched.** The chain branch still returns the linear
  precedence: the kernels coincide there by the theorem, and the chain case's
  G is pinned byte-identical to f8e766a5.

**Output on the record:** no new field.
- The ranked entry's existing `:order-use` takes a new value,
  `{:order :co-application}`.
- The G certificate's evaluation carries the co-application semantics and the
  conflict flag.
- A chain carries no co-application field at all, since its kernel is the
  list kernel (there is no absence to type).

**Map boxes:**
- `:r4-kernel` (`rank-cascade-actions`) still writes `:order-use`.
- New component box `:r4-coapply` at
  `cascade_model_manifest.clj` `co-apply-kernel`, reached positionally from
  the rollout. It writes no named field; its result is the belief row.
- `:r4-order-use` reads the receipt's `:descent` and `:units` as it reads
  `:precedence-violations`. Those are two new declared reads, each with a
  writer, `:r4-constructor`.

## 4. The test box

`futon2.aif.coapply-kernel-test`:

- **(a) Where they differ: the Lean ConflictFixture as data.** p `{:produces
  #{:a} :forbids #{:b}}`, q `{:produces #{:b} :forbids #{:a}}`, θ = 4/5 each,
  empty descent. `co-apply-kernel` from `#{}` gives
  `{#{:a :b} 16/25, #{:a} 4/25, #{:b} 4/25, #{} 1/25}` with
  `:frontier-conflict true`. `cascade-kernel [p q]` gives `{#{:a} 4/5, #{} 1/5}`.
  Mass on `#{:a :b}` is 16/25 against 0, the theorems `coApply_ne_zero` and
  `list_pq_zero`.
- **(b) Where they coincide: theorem E as data.** On `order_kernel_test`'s
  `:chain` candidate (descent `[[A B]]`), the two kernels' rows are equal at
  every state the rollout reaches, and the chain candidate's G is unchanged.
  That G is already pinned byte-identical to f8e766a5.
- **(c) Through the ranking.** For the `:independent` candidate, the ranked
  entry's `:order-use` is `{:order :co-application}`, and its G differs from
  the list kernel's G on the same candidate.
- **The bad case** is (a) and (c) run with the co-application branch replaced
  by the list kernel: the numbers must differ.
- **Fixture.** No real receipt with a non-chain order exists in the store or
  the run records (§2), and the test says so in its header. The orders are
  `construction/containment-order` computed on the test's own candidates,
  plus the Lean fixture's two patterns.

## 5. Cost under IDENTIFY 4, and the warrant

- **No new required field, and no new refusal.**
  - A value of an existing field changes (`:order-use`).
  - The certificate's evaluation record gains `:kernel-kind :co-application`,
    the semantics name, and the conflict flag. The flag is the Lean's own
    "the certificate carries the flag": an AIF-validity case, because a
    conflicting frontier is a model property G depends on.
  - A flight must satisfy nothing new.
- **The Lean warrant.** `CoApplicationKernel.lean` has not changed since
  69c2432f2b. Two lake-build logs in the test registry mention it
  (`storage/test-registry/artifacts/a31896db-…log`, 2026-09-25 03:04Z, and
  `1cf9549b-…log`, 03:31Z), both "Build completed successfully (8501 jobs)".
  No re-registration is needed while the Lean is unchanged. If the build
  changes the Lean (it should not), the lake warrant is registered by the
  hand config noted before: `:command ["lake" "build" M]` through
  `futon3c.test-registry`, since `register-warrant.sh` has no lake form.

## 6. Recommendation, size, falsifier

**Build it as one packet.**

**Size:**
- 2 source files:
  - `cascade_model_manifest.clj`: `co-apply-kernel`, and `evaluate-state`
    and `push-forward` dispatching on the step's kernel;
  - `efe.clj`: `order-use`'s non-chain branch, and `rank-cascade-actions`
    passing it.
- 1 test namespace, `futon2.aif.coapply-kernel-test`, with the three cases
  above.
- `order-kernel-test`'s `:independent` assertion updated from
  `:not-a-chain` to `:co-application`.
- **Map:** box `:r4-coapply` added; `:r4-order-use` declares its reads of
  `:descent` and `:units`; re-pinned.
- **Matrix:** row 4's adapt tripwire, "the G change on non-chain containment
  needs the co-application kernel", becomes in place.

**Falsifier:** the first flight record whose click constructs a candidate
with a non-chain order, which no flight reaches today (the clicks abstain at
assembly). Its tick run record must show:
- that candidate's ranked entry `:order-use {:order :co-application}`;
- a certificate evaluation naming `:co-application-frontier-theta-v1`;
- the chain candidates' `:order-use {:order :chain}`, with G unchanged from
  the list kernel on the same record.

A chain candidate whose G moves, or a non-chain one still labelled
`:not-a-chain`, falsifies the build. Until a flight constructs, the falsifier
is the test's (a) to (c) on computed orders.
