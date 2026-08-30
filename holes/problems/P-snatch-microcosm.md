# P-snatch-microcosm — extend the Snatch pilot until it is a microcosm of the programme

**Status:** DRAFT problem record, 2026-08-30 (claude-15, from Joe's direction). v2 form. S1
from Joe's words, his to confirm. Packet A dispatched; B and C held for review of A.
**Gate:** operator-acceptance — Joe.

> *"Let's see if we can extend/repair the Snatch pilot so it becomes a better microcosm for
> our overall programme of work — this might involve both the Markov category layer and the
> ablation."* (Joe, 2026-08-30)

## 0. What the pilot already is — every layer has a foothold

| programme layer | Snatch artefact (all in `futon3/checks/` unless noted) | state |
|---|---|---|
| nouns — patterns as production rules | `library/snatch/` 18 patterns; `playout_snatch.clj` `collection` encodes each IF as a guard | built |
| cascade — authored DAG | 9 `@why`/`@how`/`@see-also` edges; `playout_snatch.clj` reads `@why`, takes the up-closure of what acted, reports meets | built; `snatch-cascade.edn` records closure per scenario |
| policy = an operation on the cascade (target first, then construct — §3d′) | `pattern-policy`: antecedent filter → precedence order → first THEN; `pi-exchange-first` re-wires precedence only; `pi-grim` hardcoded, unreachable by the collection | built — the re-wiring moves G4 from +3 to −5 |
| outcome carrier — `Σ v, Obs v` (P-validated-R5 §2a) | `snatch-outcomes.edn`: leaves per treatment, and the finding that **institutions act on the space** (G2 removes O1; G3/G4 split O4) | built for `Obs organisations`; **`Obs evidence` (the posterior) is only `:disposition-known` in the trace** |
| Markov layer — an edge as a kernel | `how_kernel_snatch.clj`: support from the relation, mass from a declared Beta(1,1), entropy 1.3863 → 1.3121 nats under attestation, mirror zero mass, `discriminates?` | **built, one edge**; not composed |
| Q(o∣π) | `item-s001.edn` **states** `{O1 0, O2 .5, O3 0, O4 .5}` with falsifier O3; `score_item.clj` scores the receipt | stated, not derived (codex-22 Q6) |
| G(π) — the two terms | `NOTE-G-of-pi-in-snatch.md`: risk vs the game's stipulated C (own 1, other's 2); ambiguity as unpredictability of the response given the disposition; **each treatment intervenes on a different term** | stated in prose; no number |
| the apex — a falsifier before the outcome | the item: π, prior, Q, zero-mass O3, receipt contract | built, for one item |
| non-degeneracy (§2a′) | grim vs `probe-one-token` disagree on the two values | the accepting witness for conjunct 1 exists in the paper's table; **conjunct 2 (the ablation) has never been run** |

What is missing is precisely Joe's two things: the Markov layer *composed* into a derived Q,
and the epistemic term with its ablation. Both are small because the runner already plays.

## 1. The problem record (S1)

```
problem:   The Snatch pilot states Q, plays only one fixed disposition at a time, and scores
           only the pragmatic term — so it cannot yet show the two things the programme needs
           a microcosm to show: Q derived by composition rather than authored, and a selection
           that changes when the epistemic term is removed.               [Joe: confirm/rewrite]
now:       playout_snatch.clj:204 `play [policy treatment disposition rounds]` — disposition FIXED
           playout_snatch.clj:159 `p2-response` — P(o∣s) is a deterministic table per disposition
           playout_snatch.clj:191 `modelled #{:O1 :O2 :O4}` — S-001's support, copied in
           how_kernel_snatch.clj — kernel/entropy/discriminates?, one edge
           item-s001.edn — Q stated; score_item.clj — "The item states Q; this says how Q fared"
           NOTE-G-of-pi-in-snatch.md §"What would have to be measured": a prior over s; P(o∣s)
             per treatment; C given; Π enumerated — all four available here by construction
solved:    (checked before any new data)
           A. Q(o∣π) is DERIVED: for each π ∈ {grim, patterns, exchange-first, probe-one-token}
              and treatment, a DECLARED prior over dispositions is pushed through `play`
              (one run per disposition, weighted) to a distribution over that treatment's
              leaves (snatch-outcomes.edn :per-treatment); the derived Q for probe-one-token
              under G1 equals S-001's stated Q; the zero-mass set per policy is reported
              (codex-22 Q4 becomes determinable); the mirror leaf keeps zero mass unless
              `cautious` is given prior mass — which is how the falsifier enters honestly.
           B. The epistemic term exists and the ABLATION is run: a posterior over the
              disposition after each round (Bayes over the p2-response table), EIG per policy
              = expected entropy reduction of that posterior; risk = KL of derived Q against
              C ∝ exp(payoff) (the game's stipulated 1/2 asymmetry, DECLARED as the form per
              S-G3); G = risk − EIG (or + ambiguity — state which and why); then argmin G
              with and without the epistemic term across the four policies, per treatment.
              ACCEPTANCE = nonDegenerate (P-validated-R5 §2a′): the argmin moves in ≥1
              treatment. Expected: G1, probe-one-token vs grim. If it never moves, that is a
              finding about the game, reported, not adjusted away.
           C. The finite-case theorem: `discriminates` and `entropy` as properties of one
              kernel, and `nonDegenerate` as a predicate on a finite policy set, with
              packet B's numbers as the accepting witness and the July WM (ambiguity
              constant, 0 flips / 674) as the refusing one.               [Joe: confirm]
facades:   Q "derived" by re-stating S-001's numbers (must come from `play` under the prior)
           a posterior that is the fixed disposition read back (must be computed from outcomes)
           C invented to make the ablation move (the exp-payoff form is declared, once, before running)
           an ablation that removes a term already at zero (report the term's range first)
           "cautious" given mass so O3 is never a falsifier (the mirror must still be able to fail)
owner:     joe; A and B: codex-22 (knows the artefacts); C: a Claude seat with Lean, after B
status:    open — packet A DELIVERED and REVIEWED 2026-08-30 (futon3 46f3527 codex-22; review fix b304ee7
           claude-15: the acceptance target is now read from item-s001.edn rather than restated).
             Acceptance TRUE: probe-one-token/default/g1/round-1 == S-001 {O1 0 O2 .5 O3 0 O4 .5}.
             80 cells (2 priors × 4 policies × 5 treatments × 2 grains); every Q inside its treatment's
             carrier; :line-stopped kept outside it and reported; under the cautious prior O3 acquires
             0.10 for every offering policy; G2's zero-mass set is then empty, stated with its reason.
             Q4 of the re-examination is now determinable per declared prior.
             FINDING that shapes B: under the default prior all four policies have the IDENTICAL
             round-1 Q and entropy (0.6931) — each offers one token in round 1 — so the epistemic term
             has no range on that set; B must add always-abstain (zero EIG) and report both terms'
             ranges before ablating.
           packet B DELIVERED (bae103d) and REVIEWED (probe 1437fd5): verdict nonDegenerate-does-not-hold, diagnosed as
           an artefact of terminal-grain Q and KL-vs-exp(payoff) risk (§1b); packet B′ specified with a registered
           prediction; B′ DISPATCHED 2026-08-30 (Joe: "let's do the B′ computation") to codex-22:
           job invoke-1788096634837-3933-fef65f29, park park-862397a6-3aa5-4673-9ef7-a48cbc0c14a3; C held
deliveries: none
```

## 1b. Packet B's result, reviewed: an honest negative that turns out to be an artefact of the risk term (2026-08-30)

**Delivered** (`bae103d`, codex-22): five policies (+`always-abstain`), ranges before ablation,
C ∝ exp(payoff) declared once with `:line-stopped` given payoff 0 as a stated stipulation,
posterior by Bayes over the `p2-response` table from the *observed* outcome, G = risk − EIG
with the reason stated (a deterministic likelihood makes ambiguity 0 for every policy — a
finding about the game), G2 honestly "not ablated" (zero epistemic range once abstention
is removed). Verdict: **`nonDegenerate-does-not-hold`** — grim / probe-one-token are the
argmin of both G and risk under every prior. Review (claude-15): reproduced in 1.2 s; kondo
0/0; parens OK; a third, declared prior `snatcher-heavy {sharer .2 snatcher .8}` added as a
probe (`1437fd5`) — verdict unchanged.

**Why it cannot move, and it is not the game.** The runner's own trajectories give the
expected total score after five rounds, the quantity a pragmatic term should track:

| prior | grim | patterns | exchange-first | probe-one-token | always-abstain |
|---|---:|---:|---:|---:|---:|
| default (½/½) | 2.0 | **5.0** | **5.0** | 2.0 | 0 |
| cautious | 1.8 | **4.5** | **4.5** | 1.8 | 0 |
| snatcher-heavy (.2/.8) | **0.2** | −1.0 | −1.0 | **0.2** | 0 |

B's risk term (KL of terminal-grain Q against C ∝ exp(payoff)) ranks grim / probe *best*
under the default prior — where they earn 2.0 against patterns' 5.0. So the "pragmatic"
term was not measuring pragmatic value, and an ablation against it says nothing about
`nonDegenerate`. Two causes, both modelling choices in B: (i) **terminal grain** — the
terminal leaf of a policy that was snatched in round 1 and then abstained is O1, so the
token it lost never enters the term; (ii) **KL against a softmax-of-payoff C rewards
spread** — a point-mass policy pays −log C(leaf) in full, so any offering policy beats
`always-abstain` whenever C(O2) > C(O1), whatever the prior. The negative is an artefact of
the risk form and the grain, and it was caught by the apex question — *is this the right
evidence?* — applied to the term itself.

**Packet B′ — the correction, with a registered prediction (held for Joe's go).**
Risk over the **total-score carrier**: Q(score∣π) from the runner's five-round trajectories
under the declared prior; C ∝ exp(score) over the achievable scores, declared once.
*Sanity acceptance before any ablation:* the risk ranking must agree with the E[score]
ranking above for all three priors — if it does not, the term is still wrong and the packet
stops there. Then the ablation as in B, plus a fourth declared prior
`{sharer 0.10 snatcher 0.90}`. **Registered prediction, before running:** E[probe] = 6p − 1
crosses zero at sharer-mass p = 1/6, so at p = 0.10 the risk-argmin is `always-abstain` and,
EIG being 0.325 nats for every offering policy, the G-argmin is `probe-one-token`:
`nonDegenerate` holds at that prior and at no prior with p ≥ 1/6 (where offering is already
pragmatically best). If the argmin does *not* move at p = 0.10, the microcosm has no
explore/exploit trade-off at size-1 offers, and the next lever is offer size, not C.

**B′ RESULT (2026-08-30, codex-22 futon3 `6364964`; reviewed and re-run by claude-15):
prediction PARTLY confirmed.** Sanity passed in all 20 prior × treatment cells (risk ranking
agrees with the E[score] ranking above) before any ablation ran; the script now stops with an
exception if it does not. Ablation, each cell `argmin-risk → argmin-G`:

| prior | G1 | G2 | G3 | G4 | G5 |
|---|---|---|---|---|---|
| default (½/½) | patterns/exchange-first → same | n/a (EIG range 0) | same | patterns → same | same |
| cautious | same | n/a | same | patterns → same | same |
| snatcher-heavy (.2/.8) | grim/probe → same | n/a | same | patterns → same | same |
| snatcher-dominant (.1/.9) | **always-abstain → grim/probe** | n/a | **moves** | patterns → same | **moves** |

Verdict `:nonDegenerate-holds` at the .1/.9 prior in G1/G3/G5 — the first computed witness
for §2a′ of `P-validated-R5`. Two departures from the registered prediction, both mine:

1. **G4 does not move at any prior.** The remedy (+3 per repair) gives `patterns` E[score]
   = 12p + 3 under G4, positive even at p = 0, so offering is pragmatically best there
   whatever the prior. The prediction should have exempted G4 alongside G2; the code
   transcribed the claim as written (`:moves-only-for-snatcher-dominant-outside-g2`) and did
   not adjust it to fit — which is how the miss is visible at all.
2. **grim and probe-one-token tie in every cell** (same E[score], same EIG). With cautious
   mass 0 in all four priors the two policies generate identical trajectories, so the
   argmin-G is the pair, not `probe-one-token` alone.
   *Correction, same day (Joe asked whether a further dispatch would separate them):* no
   prior can. S-001's rule for `probe-one-token` is "offer 1 turkey, ask 1 corn; on a snatch,
   never offer again" (`item-s001.edn`) — that *is* grim trigger at size 1; the runner's
   `pi-grim` differs only by a tokens-exhausted guard that never fires in five rounds
   (`playout_snatch.clj:164`, `derive_q_snatch.clj:30`). A cautious partner refuses (`O3`)
   and both keep offering, so cautious mass changes the numbers, not the tie. The policy
   that *acts on* what the probe learned is the library's own pair
   `snatch/probe-before-committing` → `snatch/escalate-only-as-far-as-you-can-lose`, and
   that is already in the table as `patterns` (5.0 vs grim's 2.0 under the default prior is
   exactly the escalation). So the separation exists — grim vs patterns — and
   `probe-one-token` should be read as *grim at round-1 grain*, which is the grain S-001
   declares. No further Snatch dispatch for this.

Confirmed as registered: no movement at p ≥ 1/6 (default, cautious, snatcher-heavy: 15
cells, none moved); movement at p = 0.10 outside G2 and G4; EIG 0.325 nats for every offering
policy at that prior. Packet B's terminal-leaf risk is kept in the report under
`:terminal-leaf-baseline` so the carrier change is inspectable. Determinism: claude-15's
re-run left `checks/ablation-snatch.edn` byte-identical; kondo 0/0; check-parens OK; ~1.5 s.
**Packet C is held** (Joe's call) — what it would now formalise is stated: `nonDegenerate`
witnessed by one prior, with the two-readings claim still a conjecture.

## 2. The three packets — one behaviour, one acceptance test each

**A · `checks/derive_q_snatch.clj`** — the declared prior over dispositions
`{:sharer 0.5 :snatcher 0.5 :cautious 0.0}` (S-G3: printed, not absorbed; a second run with
`:cautious 0.1` shows the falsifier acquiring mass); for each policy × treatment, run `play`
per disposition, map each trajectory to its terminal leaf in `snatch-outcomes.edn`'s
per-treatment set (and, separately, the round-1 leaf, which is what S-001 predicts), weight
by the prior, and print Q with its entropy and zero-mass set. **Acceptance:** the round-1 Q
for `probe-one-token` under G1 equals `{O1 0 O2 .5 O3 0 O4 .5}`; every treatment's Q is
supported inside that treatment's leaf set (G2 never emits O1); the report is EDN.

**B · `checks/ablate_g_snatch.clj`** — consumes A's Q and trajectories; posterior over
disposition per round from the p2-response table; EIG per policy; risk against declared C;
G both ways; the ablation table `treatment × policy → argmin with / argmin without`.
**Acceptance:** `nonDegenerate` holds in at least one treatment, or the report says it does
not and why.

**C · Lean** — `DarkTower/WarMachine/SnatchKernel.lean` (finite, Mathlib-free, in
`CommitmentTemperature`'s style): a kernel as `Disposition → Dist Leaf`; `discriminates`;
`entropy`; the relation between them; `nonDegenerate` over a finite `List Policy`; three
polarities named. After B.

## 3. What the microcosm then shows, layer by layer

Nouns (a pattern is a guard + a THEN) → cascade (authored DAG, up-closed by the run) →
policy (an operation: precedence over the cascade, target-first) → kernel (an edge with mass
from a declared prior) → composition (the run, weighted over a declared prior on the hidden
state) → Q derived → G with two terms → a selection that the epistemic term can change → a
falsifier that can fire → a theorem that the two readings are one morphism. Every step has
a facade named against it, and the whole thing runs in seconds on six patterns — which is
what makes it a microcosm rather than a demonstration.
