# H-VALUE-G-D — why the lane's G refuses chains with unwanted intermediates (AR-40)

Discovery packet. claude-6, 2026-09-24/25, on claude-8's requisition (PROOF-2
register row AR-40, `PROOF-2-THEOREM-draft-2026-09-24.md` line 332). Read-only:
no code outside this packet was edited, no clicks fired, nothing written under
`data/`. Reproduction script and its transcript are committed beside this file.

## Cause, in one line

AR-40's candidate cause is **refuted**: nothing in the risk term penalises an
instrumental token. The decline comes from the **partition function's universe**:
`constructed-candidate-g` builds the scored family — and therefore the token
universe normalising `ln Z` — **from the candidate being scored**, so the plan's
G and the empty cascade's G are taken over different universes and differ by
`T·k·ln 2` for the `k` tokens only the plan names. In one common universe the
4-step chain is preferred, 15.3434 against 16.3434.

## 1. Which G this is

| step | function | file |
|---|---|---|
| the constructor's `:evaluate-g` | `constructed-candidate-g` | `scripts/futon2/report/war_machine.clj:6041` (wired at `:7227`) |
| scores by running R1→R6→R13→R4→R5 | `cascade-lane … {:through :R5}` | same file, R5 step at `:5885` |
| R5 for cascade candidates | `efe/rank-cascade-actions` | `src/futon2/aif/efe.clj:1006` |
| G itself | `cascade-model-manifest/horizon-g-sparse` | `src/futon2/aif/cascade_model_manifest.clj:991` |

The first thing this changes about AR-40's framing: cascade candidates do **not**
go through `compute-efe`, so none of `:G-risk`, `:G-ambiguity`,
`:homeostatic-pressure`, `:structural-pressure` or `:predictability-bonus` exists
on a ranked entry here. `rank-actions` dispatches to `rank-cascade-actions`
whenever every candidate is a cascade candidate (`efe.clj:1379-1380`). The terms
that exist are `horizon-g-sparse`'s per-step risk and ambiguity.

## 2. The four AR-40 numbers reproduce exactly

Every row is assembled by the real assembly
(`assemble-cascade-problems-with-published`), so facts, repository, `:c-schedule`,
`:cascade-spec`, preference scales and β are the lane's
(`cascade_problems.clj:154-168`), not the script's. Only the **wants** vary.
Horizon is declared 4 throughout. Full transcript:
`h-value-g-d-repro-output.txt`.

| row | wants | plan | G(plan) | G(empty) | taken | AR-40 says |
|---|---|---|---|---|---|---|
| A | `t1` | 1 step, 0 unwanted | 4.0256 | 8.0256 | yes | 4.03 vs 8.03 taken |
| B | `t2` | 2 steps, 1 unwanted | 7.7982 | 10.7982 | yes | 7.80 vs 10.80 taken |
| C | `t4` | 4 steps, 3 unwanted | 15.3434 | 10.7982 | **no** | 15.34 vs 10.80 not taken |
| D | `t1..t4` | 4 steps, 0 unwanted | 13.4876 | 15.9876 | yes | 13.49 vs 15.99 taken |

Row C is the only row whose plan the constructor refuses to build, so its plan is
declared in the sources instead (with a `:construction-receipt`, which
`cascade_problems.clj:53-63` requires for admission). G never reads that receipt
— `constructed-candidate-g` takes only `:precedence` — so it does not enter any
number above. The constructor's own refusal receipt independently reports the
same baseline: `:stop-reason :acting-worth-more`, `:g-of-best 10.798224194552454`,
`:final-evaluation {:compose-by-need -4.545177444479563}`.

## 3. Decomposition, term by term

Two terms exist. One is identically zero:

```
=== C  want t4, plan = 4 steps, 3 unwanted intermediates (t1,t2,t3)
    G(plan) 15.3434   G(empty) 10.7982   plan taken? false
    plan  call: universe (:s0 :t1 :t2 :t3 :t4) (|V|=5), lnZ 4.0859
         per-step risk ["4.0859" "4.0859" "4.0859" "3.0859"]  sum 15.3434
         per-step ambiguity ["0.0000" "0.0000" "0.0000" "0.0000"]  status (:reduced-identically-zero)
         lane G 15.3434  cert G 15.3434  same-computation? true  closed form T*lnZ-sum u(S) 15.3434
    empty call: universe (:s0 :t1 :t4) (|V|=3), lnZ 2.6996
         per-step risk ["2.6996" "2.6996" "2.6996" "2.6996"]  sum 10.7982
```

- **ambiguity: `:reduced-identically-zero`.** At the default all-zero adjudication
  rates the observation kernel is the identity, so `Q(o_τ|π) = q_τ` and the term
  vanishes by reduction, not by rounding (`cascade_model_manifest.clj:996-997`;
  the certificate keeps `:ambiguity-status` distinct from a 0 value on purpose,
  `:1104-1112`).
- **risk: the whole of G.** Per step, `risk_τ = −ln c(S_τ)`
  (`outcome-risk-pointwise`, `:676`), which for the point-mass rollout of a
  firing precedence is `ln Z − u(S_τ)`.

So, with `utility-weights` (`:588-606`) and `log-preference-fn` (`:608-656`):

```
w_v  = λ/|want|  for v ∈ want,  + μ for v ∈ evidence,  0 otherwise
u(o) = Σ_{v ∈ o ∩ V} w_v
ln Z = Σ_{v ∈ V} ln(1 + e^{w_v})
G    = Σ_{τ=1..T} (ln Z − u(S_τ)) = T·ln Z − Σ_τ u(S_τ)
```

The script computes this closed form independently and it agrees with the lane's
`:G-efe` in all eight candidate scorings (four rows × plan and empty), and the
per-step certificate's `:g` equals the lane's `:G-efe` in each
(`same-computation? true`), so the decomposition is of the identical computation
and not of a re-derivation.

Read the per-step risks and the two behaviours are visible directly:

- **Row C (`t4` wanted, `t1 t2 t3` not):** `4.0859, 4.0859, 4.0859, 3.0859`. The
  first three steps earn **no credit at all** — `u(S_τ) = 0` because an unwanted
  token has weight 0 — and only the last step subtracts λ = 1.
- **Row D (all four wanted):** `3.7469, 3.4969, 3.2469, 2.9969`. Risk falls by
  λ/|want| = 0.25 at every step, because each intermediate is now a want.

## 4. The candidate cause is refuted

AR-40: *"the risk term treats instrumental tokens as deviations from
preference."* If that held, two predicted states differing only in unwanted
tokens would have different preference, hence different risk. They do not:

```
=== refutation check: is an unwanted token PENALISED as a deviation from preference?
  ln c((:s0 :t4)) = -3.0859
  ln c((:s0 :t1 :t2 :t3 :t4)) = -3.0859
  equal? true -> three unwanted tokens change the preference of the state by 0.0000
```

`utility-weights` gives a token outside `want ∪ evidence` weight 0, and `u` sums
weights over `o ∩ V`, so `u` cannot see an instrumental token. There is no
per-token deviation penalty to find. Reaching `t4` by way of three unwanted
tokens is, as a state, exactly as preferred as reaching it alone.

## 5. The actual cause: the universe grows with the candidate

`constructed-candidate-g` builds its family as *the single-pattern order of every
interpretation **enabled on the problem's facts**, plus the candidate's own
order* (`war_machine.clj:6056-6060`). `rank-cascade-actions` then takes the
scored universe as *the union of every candidate's pattern tokens, q0's support
and the want* (`efe.clj:1091-1094`). Those two facts together make the universe
**a function of the candidate being scored**:

```
=== row C: the two G values the constructor compares are taken over DIFFERENT universes
  universe when scoring the PLAN : (:s0 :t1 :t2 :t3 :t4)
  universe when scoring EMPTY   : (:s0 :t1 :t4)
  tokens only in the plan's universe: (:t2 :t3)  k = 2
  G(empty) in its own (smaller) family : 10.7982   <- the number the constructor compares against
  G(empty) in the PLAN's family        : 16.3434   <- the comparable number
  difference                           : 5.5452   predicted T*k*ln2 = 5.5452
  G(plan) 15.3434 vs G(empty) 16.3434 IN ONE UNIVERSE -> plan preferred? true  (by 1.0000 = the final want's lam)
```

Every token in V adds `ln(1 + e^{w_v})` to `ln Z` at every one of the T steps; an
unwanted token has `w_v = 0` and so adds exactly `T·ln 2 = 2.7726`. The plan
names `t2` and `t3`, which are in neither the want nor the enabled singles, so
its own universe is two tokens larger and its G carries `5.5452` of normalisation
that the baseline's G does not. The final want is worth λ = 1. The plan loses by
`5.5452 − 1 = 4.5452`, which is exactly the `:compose-by-need -4.545177444479563`
on the constructor's receipt.

The comparison happens at `construction.clj:135-136`:

```clojure
pragmatic (- (g-norm (best-g evaluate-g family))
             (g-norm (best-g evaluate-g proposed)))
```

Two independent `evaluate-g` calls, each fixing its own universe, subtracted from
one another. `horizon-g-sparse`'s own docstring states the precondition this
breaks — *":universe is the common token universe of the comparison … pass the
same universe for every candidate compared"* — and `rank-cascade-actions` says
why: *"per-candidate universes would shift G by T·k·ln 2 and are never used"*
(`efe.clj:1014-1017`). Within one R5 call that holds, which is why live
**selection** is unaffected: it scores its whole family in one call. It is the
**constructor** that calls G once per candidate.

This also explains AR-40's "past about two unwanted intermediates" without
needing a threshold. The first intermediate is free, because the pattern
producing it is the enabled single already in the family (row B: `t1` is in both
universes, so the 2-step plan is taken). Each further intermediate costs
`T·ln 2 = 2.7726` against a want worth λ = 1. So `k ≥ 1` extra named tokens
already decides it, and row B's "1 unwanted intermediate" has `k = 0`.

**Restatement AR-40 needs:** its row-C pair `15.34 vs 10.80` compares two
universes. The comparable pair is `15.34 vs 16.34`, where the chain **is**
preferred, and the 10.80 is the baseline the constructor actually used.

## 6. Smallest fix

**Give every `evaluate-g` call of one problem the same universe, and take it from
the problem rather than from the candidate.** `cascade-problems/problem-tokens`
(`cascade_problems.clj:86-97`) already computes exactly that set — facts, want,
and every interpreted pattern's guard and produces — and is already used for
locator coverage, so no new notion of "the problem's tokens" is introduced.

Three edits, each a no-op when the new key is absent (so every existing call is
byte-identical):

1. `rank-cascade-actions` honours an explicit `:universe` in opts instead of
   computing it (`efe.clj:1091`); absent, it computes as today.
2. `cascade-lane` passes the problem's declared universe into R5's `base-opts`
   (`war_machine.clj:5891-5893`), beside `:horizon-steps` and `:cascade-spec`.
3. `constructed-candidate-g` supplies `problem-tokens` of its problem
   (`war_machine.clj:6041`).

**Authority: no parameter value is being chosen, so none needs a ruling.** The
fix implements a precondition the scorer states about itself in code
(`horizon-g-sparse` `:1002-1004`, `rank-cascade-actions` `:1013-1017`). The
universe is determined by the problem, not declared by anyone. Nothing here rests
on an operator ruling.

**Effect, measured rather than predicted** — every candidate scored in one lane
run over the common universe:

```
    G(empty)                      = 16.3434
    G(chain, reaches t4)          = 15.3434  improvement 1.0000 -> TAKEN (the AR-40 case, now taken)
    G(3-step prefix, never reaches t4) = 16.3434  improvement 0.0000 -> DECLINED (bad case: no want reached, no credit)
```

### The bad case, and a typed absence in it

Asked for: *a chain whose final want is worth less than its cost must still be
declined.*

- **Expressible today and still declined:** a chain that reaches no want. Its
  `u(S_τ)` is 0 at every step, so its G equals the empty cascade's exactly
  (16.3434 both), improvement 0, declined at any move cost ≥ 0. Shown above.
- **Not expressible today — typed absence:** a want-reaching chain whose cost
  exceeds its value. Under the fix its improvement is exactly λ times the number
  of steps the want is held, and the only cost term is
  `construction-move-cost`, whose declared value is **0**
  (`war_machine.clj:6112`, authority claude-10 2026-09-24, `:ruling :none-found`,
  commit `891b4af6`). At cost 0 every want-reaching plan is taken, so this bad
  case **cannot be constructed** against the current parameters. The arithmetic
  it needs:

  ```
  declined iff moves × move-cost ≥ λ × (steps the want is held)
  4 moves, λ = 1, want held 1 step  ->  move-cost ≥ 0.25
  ```

  (from the transcript: cost 0.00 and 0.20 taken, 0.25 and 0.50 declined.) So the
  fix should not be landed as if it restored a cost discipline that is not there.
  Either a positive move cost is declared with its own authority — claude-10's
  note records that cost 1 made the constructor decline M-aif-eig's plan at
  improvement 0.33 — or the packet's honest statement stands: after this fix the
  constructor takes **every** want-reaching plan it can build within its budget,
  and the only brake is `:max-moves`.

## 7. What this changes in PROOF-2 / PROOF-2a

- **AR-40 (`W6 (G)`, H-value).** Candidate cause refuted; replace it with the
  universe account, and restate the row-C pair as `15.34 vs 16.34` in one
  universe with `10.80` named as the cross-universe baseline the constructor
  used. AR-40's conclusion — "past about two unwanted intermediates the machine
  judges acting worse than doing nothing" — holds as an observation, but not for
  the reason given, and its own remedy ("a preference model for instrumental
  tokens") is not what is needed: instrumental tokens are already unpenalised.
- **PROOF-2a H-value rows (lines 463, 482).** The row currently reads "the lane's
  G charges instrumental tokens as deviations, so chains with 2+ unwanted
  intermediates are judged worse than idling (claude-10's D16 probe, unconfirmed
  cause)". That is now confirmed-false as to cause. H-value's substantive hole —
  no static scalar V reproduces the mission's ranking; timing and degree are
  needed — is untouched by this finding: this is a comparability defect in the
  constructor, not a value model. The two should stop being one row.
- **W6 / P₆ (Clause 6).** W6 requires each per-click input to have a typed
  carrier and the recorded equalities; P₆ joins values through
  `:g-term-decomposition :policies`. Two additions follow:
  1. A G value is only meaningful with the universe it was normalised over, so
     the recorded G terms must carry that universe. The certificate today records
     `:universe-size` (`cascade_model_manifest.clj:1142`) and the per-step C
     distribution, which carries `:universe` — a size alone cannot decide whether
     two G's are comparable, since two different 3-token universes are both
     size 3.
  2. Any recorded **comparison** of G values — the construction receipt's
     `:g-of-best` and `:final-evaluation`, and any `:acting-worth-more` — must
     assert that both values were taken over the same universe. Today
     `:g-of-best 10.798` sits on the same receipt as a plan scored at 15.343 with
     nothing recording that they are not commensurable. That is the defect this
     packet found, and it was invisible in the receipt.
- **Clause 6's falsifier X₆** gains a case: leave every G value correct but score
  two compared candidates over different universes; W6/P₆ must fail. It does not
  fail today.

## 8. Reproducing, and what was not checked

```
cd /home/joe/code/futon2
clojure -M holes/labs/wm-contract/proof2/packets/h-value-g-d-repro.clj
```

Fresh private JVM, read-only, temp store per row (`Files/createTempDirectory`),
no clicks, nothing under `data/`. The transcript committed here was produced at
HEAD `43d88ee7` and is byte-identical to the run at `b7b3583a` the day before, so
the numbers do not depend on the intervening commits.

The script reads one private var, `#'futon2.aif.efe/cascade-candidate-tokens`,
to build the same universe `rank-cascade-actions` builds; it is marked at the
require and used only in the decomposition, never in a reproduced G.

Not checked: (i) the fix is proposed, not implemented — no code outside this
packet was touched, and the "effect" numbers above come from scoring one family
in one lane call, which is what the fix would make the constructor do, not from a
patched constructor; (ii) non-zero adjudication rates, where the ambiguity term
stops being identically zero and the factorized path runs — every number here is
at the default identity kernel; (iii) the flight runner is affected but was not
measured: `flight_runner.clj:112` passes the identical `wm/constructed-candidate-g`,
so a flight's construction runs the same candidate-dependent universe through the
same subtraction at `construction.clj:135-136`; no flight was scored here.
