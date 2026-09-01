# C469 — RUN8 / stage S3: τ = β on the live path, and what it does and does not move

2026-09-01, claude-20 (wm-build-loop work seat). Item RUN8. Evidence for the
row; no ruling is written here and nothing in `aif-equations.edn :choices` or
`control-map-edges.edn :decisions` was touched.

## 1. The wiring, and why the two sites had to change together

`FUTON_WM_TAU_MODE=variational-beta-gamma` → τ_eff = β, the friston2017 eq. 2.7
root, γ = 1/β. Parser and dispatch:

- `war_machine.clj:494-508` — `tau-mode-of`, the string→mode map, extracted as a
  pure fn so a test can drive it with strings instead of mutating the process
  environment. `war_machine.clj:510-528` — `arena-tau-mode` is that fn applied to
  the env read and nothing else.
- `policy.clj:76-145` — `effective-temperature`, the closed dispatch. It throws
  on any mode it does not know, which is why the pair had to move together: the
  parser admitting a mode the dispatch rejects is a tick-time exception.
- The pointer in RUN8 `:acceptance` (`war_machine.clj:238-248`) was already
  corrected in I1 `:slice-b2b` (3a): `039b0b8` put `beta-dark-carry` in that
  range. The parser was at `447-457` at that reading and is at `510-528` now.

Pinned by `war-machine-test/tau-mode-parser-admits-exactly-what-the-dispatch-accepts-test`,
which asserts both directions: every mode the parser can return is one
`effective-temperature` accepts, and `arena-tau-mode` equals `tau-mode-of`
applied to the env.

## 2. The hold, and the prohibition on crossing laws

`policy.clj:56-75` — `temperature-source` puts the LAW that produced τ on the
decision beside `:tau`, per tick, as I1 (b2b) (2) requires: `:selection-gain-spread`,
`:selection-gain-only`, or `carry-beta`'s own `:beta-source`
(`:converged-posterior` | `:held-unsolved` | `:held-absent` | `:initial`).
Emitted at `policy.clj:318` (the explanation) and `369`, `519`, `547` (the three
decision maps), and persisted — `strip-decision` (`trace.clj:168-178`) drops only
`:softmax-weights` and `:ranked-actions`.

A tick that did not solve does not fall back to 1/g. The hold happens upstream in
`carry-beta` (`policy_precision.clj:544-560`), which always yields a finite
positive `:beta` — the last solved posterior, or β₀ on a cold start — so the
variational mode always has a β to use and the source says which it is. If no β
reaches `effective-temperature` at all, that is a wiring error and it throws
(`policy.clj:134-142`); the test asserts the throw is not mistakable for the 1/g
answer.

`variational-tau-preconditions!` (`war_machine.clj:102-130`) refuses the mode
without `FUTON_WM_BETA_DARK=1`, `FUTON_WM_FPI_DARK=1` and
`FUTON_WM_TRACE_POLICY_DETAILS=1`. Without them every tick holds β₀ and τ ≡ 1.0 —
correct, useless, and indistinguishable in a record from a converged carry
sitting at 1.0.

## 3. One structural change to the tick, stated because it is the risk

The β solve is the selection temperature, so it must be computed BEFORE
selection. `cascade-policies` / `cascade-actions` / `wm-ranked+cascades`
(`war_machine.clj:4903-4931`) and the two dark-field bindings
(`war_machine.clj:4932-4948`) were moved from after `controller-decision` to
before it. They are pure `let` bindings whose only inputs (`include-advisory-lanes?`,
`wm-ranked`, `prev-trace-record`, `observation`) are all bound earlier, and
nothing between the old and new positions reads them, so the values are
unchanged; the join field is still `wm-ranked+cascades`, the same field the S2
dark carry used, so the β series stays comparable across stages.

Evidence that the reorder did not disturb the default path: a real diagnostic
tick under the unchanged default (`r6_zero_post_preflight.clj`) — PASS in 29.7 s,
0 POSTs, 0 `.admintoken` reads, run lock released.

## 4. It runs live, measured on a real tick

`run8_s3_preflight.clj`, twice, with the env set in the shell so the parser reads
the real environment:

| | control (default) | S3 |
|---|---|---|
| `FUTON_WM_TAU_MODE` | unset | `variational-beta-gamma` |
| `:tau` | 1.0 | **1.0364669814843985** |
| `:tau-source` | `:selection-gain-only` | **`:converged-posterior`** |
| `:tau-spread` | 25.06726787845154 | 25.067267878451535 |
| `:selection-gain` | 1.0 | 1.0 |
| `:wm-version :tau-mode` | `:selection-gain-only` | `:variational-beta-gamma` |
| β carry | not computed (flags off) | β 1.0364669814843985, γ 0.9648160702310347, converged+bracketed |
| POSTs / token reads | 0 / 0 | 0 / 0 |

τ equals β exactly and is not the selection-gain law's 1/g = 1.0. The carry
continues the S2 series (S2 closed at β = 1.034342317).

## 5. Rank and argmax movement against the S1 field — the acceptance

`run8_tau_arms.clj`, output `runs/2026-09-01-s3/ARMS.txt`. Three fields × 20
ticks × 145 candidates, replayed with `policy/softmax-weights` and
`policy/effective-temperature` themselves rather than a re-implementation.

**Control first.** At τ_live the replay reproduces the record exactly on 60/60
ticks: max |−G/τ delta| 0.0, the `:habit-adjusted-ranking` order reproduced 20/20
per field with max |score delta| 0.0, and max |softmax delta| 0.0.

**β series.** Only S2 solved one (20 ticks, 1.0026460727 → 1.0343423166, all
`:converged-posterior`). S1 and S1b persisted no F_pi readback, so their β is not
recoverable from their records; the arm transplants S2's β tick-for-tick by
index. That makes the S1 arm *the S1 field under the temperatures S2 measured*,
not *the β S1 would have solved*. S2 is the native pairing.

| | S1 | S1b | S2 |
|---|---|---|---|
| controller rank moves (argmin G) | 0 | 0 | 0 |
| habit-adjusted rank moves | 0 | 0 | 0 |
| habit-adjusted argmax moved | 0/20 | 0/20 | 0/20 |
| softmax argmax moved | 0/20 | 0/20 | 0/20 |
| mean softmax TV distance | 0.0003573076 | 0.0003572421 | 0.0003571305 |
| mean softmax entropy | 4.6672728725 → 4.6672519263 | 4.6672343598 → 4.6672136586 | 4.6671761687 → 4.6671558858 |
| recorded `:action` = controller rank 1 | 0/20 | 0/20 | 0/20 |

**The zeros are structural, and the arms say how far from moving the field was
rather than only that it did not move.** Ordering is by ln E − G/τ, so raising τ
adds +G(1/τ_live − 1/τ_s3) to every score — monotone in G. Within a set of
candidates sharing an ln E value the order therefore *cannot* change; only a pair
straddling two ln E values can cross. On this field ln E takes **3 distinct
values, spread 3.0445 nats**, and the smallest cross-group adjacent margin
evaluated at τ_s3 is **+0.7356** (negative would be a flip). So the nearest pair
was 0.74 nats from moving, not 1e-6 from moving.

## 6. The premise in RUN8 `:statement` is false, and this is the correction

`:statement` calls S3 "the first stage that changes what the machine selects".
I1 (b2b) (3b) recorded that τ does not reach the selected action and left it for
this row. It does not, and the argmax measurement above adds a second reason the
first one did not cover:

1. **τ never reaches `:action`.** `strategic-recommendation` picks the first
   non-`:no-op` entry of the G-ordered list (`policy.clj:353-355`), and τ feeds
   only `:softmax-weights` and the counterfactual habit ordering
   (`:habit-prior-applied? false`, `:habit-authority :counterfactual-only`).
   Downstream, `war_machine.clj:5011` REPLACES `:action` with the strategic
   selector's pick, which is handed only `:scheduler-habit-ranking` and a trace
   id. Measured, not just read: the recorded `:action` equals controller rank 1
   on **0 of 60** ticks.
2. **Even where τ does enter, the argmax is τ-invariant here.** −G/τ is monotone
   in G for every τ > 0, so the softmax argmax and the controller ranking cannot
   move at all; and the one ordering that *can* move (habit-adjusted, where ln E
   competes with −G/τ) did not, for the margin reason in §5.

So what S3 changes on this field is the recorded `:tau`, `:tau-source`, the
`:controller-ranking` selection scores and the softmax posterior — a mean TV
distance of 3.6e-4 — and nothing else. **No ruling is written here**: whether S3
should be re-scoped, or the stage list re-ordered so a stage that can move the
action comes first, is Joe's, via `:choices` / `:decisions`. What is recorded is
the code-backed fact with its pointers.

## 7. The `:tau` / `:softmax-weights` consumer audit I1 (b2b) (3b) asked RUN8 for

"Whether anything downstream of the decision map consumes `:tau` or
`:softmax-weights`" — audited rather than assumed. `grep -rn ':tau\b|:softmax-weights\b'`
over `src/futon2` and `scripts`:

- **Consumers of the WM decision's `:tau`: exactly one, and it is reporting.**
  `full_loop_runner.clj:2570-2577` copies `[:source :rank :controller-score :tau
  :selection-gain :habit-prior-applied?]` into a construction record's
  `:selection-reasons`. It changes no selection, and RUN6's enumeration
  established `full_loop_runner` is not reachable from `run_tick_once`.
- **Consumers of `:softmax-weights`: exactly one, and it is persistence.**
  `trace.clj:175-178` re-keys it to `:softmax-weights-by-candidate-id` under the
  policy-details flag and otherwise drops it.
- **Not consumers, though they match the grep.** `war_machine.clj:2377`, `2387`,
  `3682`, `3690`, `4098`, `4108`, `4151`, `4155` and
  `war_machine_visual.clj:1119` all read a `:tau` belonging to a DIFFERENT
  apparatus — the futon3c portfolio-inference head (`/api/alpha/portfolio/state`)
  and `futon3c.aif.mission-head` — not this decision's. `cascade_prior.clj:176,205`
  and `adapters/fulab.clj` carry their own temperatures. Everything under
  `src/ants/` is the ant apparatus.
- `dark_mode_shadow.bb:65` reads `(get-in t [:decision :tau])` off persisted
  records, read-only.

So nothing downstream of the decision map acts on τ or on the posterior; the two
readers report and persist. That is a fact about today's code, not a guarantee —
`:tau-source` is what makes a later consumer's inputs legible.

## 8. Stale labels corrected (RUN8 `:statement`)

`dark_mode_shadow.bb:9-20, 128-140, 160, 170` called the live mode `:gamma-only`
and bound `(:decision :selection-gain)` to a variable named `gamma`. The mode has
been `:selection-gain-only` since B-2d, and g is the R14 *engineering* selection
gain — writing it γ collides with the variational policy precision γ = 1/β this
row puts on the live path. Renamed throughout; **no computation changed**, only
what it is called, and the script now says it does not shadow the S3 arm at all
(β is not recoverable from the pre-S2 corpus it reads).

## 9. Gates

clj-kondo 0 errors / 0 warnings and `check-parens` exit 0 on every changed file.
Tests, gates and the two pre-flights are listed in the RUN8 `:evidence` row.
