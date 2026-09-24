# B-D — the scalar-to-Dirichlet correspondence of the learner that already runs

Packet 33 of `PROOF-2-STRATEGY-draft-2026-09-24.md`. Author: kimi-6,
2026-09-24. Discovery/specification only: no code, click, data write, or JVM
reload. Serves clause 5 (W₅/P₅/X₅, `PROOF-2-THEOREM-draft-2026-09-24.md:78-84`,
Correction and Review amendments) and agrees with the certificate schema
(`proof2/packets/CERT-S.md`, B row). All file:line anchors are at futon2
`93531d415a115c7b426f37e06d7d5542ecb5dae4`. Record claims cite key paths in
`data/wm-learning-trials/attempts.edn` (12 rows, read-only) and the close
records named below. Lean anchors are
`mathlib4/DarkTower/WarMachine/DirichletLearning.lean` (module
`DarkTower.WarMachine.DirichletLearning`).

## 1. The exact mapping

The theorem the correspondence rests on is Da Costa et al. 2020 eq. (21),
`a_post = a_prior + Σ_τ o_τ ⊗ s_τ`, formalised as `accumulate` with its
pointwise equation `accumulate_conc` (DirichletLearning.lean:92-104) and the
one-hot special case `accumulate_onehot` (:147-171).

**Outcome carrier O = {achieved, not}.** Two cells. One ledger row carries
one boolean observation: `:observed` (v1 write shape derived as
`(pos? (:success (:increment row)))`, `learning_trial_ledger.clj:139-141`;
the increment is written as `{:success 1 :failure 0}` or `{:success 0
:failure 1}` at `:55-56`). The outcome vector of trial τ is the indicator of
the observed cell: `o_τ(achieved) = 1, o_τ(not) = 0` when `:observed` is
true, else the complement.

**State carrier S = one singleton state per pattern family.** The actual
grain the ledger keys by is **not** the banked `:family` field — that is the
digest of the whole trial *configuration* (target, cascade, patterns,
effect, route), used by `record!` only to detect `:revised-meaning`
(`learning_trial_ledger.clj:47-49`, and `theta-key`'s docstring :96-115
saying exactly this). The parameter key is `:theta-key`, derived at read
time by `producer-of` (:75-93): the `:id` of the pattern that *declares* the
trial's effect among its `:produces` within the recorded precedence. So S
has one cell per *pattern id*; the singleton belief of trial τ is
`s_τ(family) = 1` on the trial's own `:theta-key`. Two pooling decisions are
thereby baked into the carrier, both documented at the consumption site
rather than hidden: trials pool **across targets** (reliability is a
property of the pattern, not the pattern-on-this-target — the
`pattern-theta` docstring :244-251 records this as an assumption and carries
`:targets` so a reader can see a 1/8 that came from three attempts on one
target), and a pattern declaring several tokens pools across its whole
`:produces` set (:252-256).

**Prior concentrations (1/2, 1/2).** Jeffreys prior on the two-cell carrier:
`conc₀(achieved) = conc₀(not) = 1/2`. The update rule is stated in
`b-update`'s docstring and rule string (`learning_trial_ledger.clj:154-173`,
:218-224) as `theta_post = (successes + 1/2) / (trials + 1)` "Laplace (beta
1/2,1/2)", and computed as a literal ratio at :216 and :280
(`(/ (+ successes 1/2) (+ n 1))`).

**The theorem B-N must prove.** With the carriers above, after n
deduplicated trials of one family with s successes:

```
conc_n(achieved) = 1/2 + s        conc_n(not) = 1/2 + (n − s)
```

(by `accumulate_onehot` applied n times with one-hot `(o_τ, s_τ)` ticks;
`accumulate_append` (:106-140) makes the per-trial chaining exact), and

```
theta = conc_n(achieved) / (conc_n(achieved) + conc_n(not))
      = (s + 1/2) / (n + 1)
      = (:theta (pattern-theta family))
```

i.e. **the recorded scalar theta equals the normalized accumulated cell
`conc(achieved)/total` of the posterior** `DirichletParams` — the
normalization W₅ names ("The extracted theta equals a separately recorded
normalization of the relevant posterior concentration row", THEOREM:80).
The statement to formalize is a specialization, not a new rule: for
`O = Fin 2`, `S = Fin 1`, prior `fun _ _ => 1/2`, and one-hot trials,
normalized cell equality. Because every concentration is a rational with
denominator 2 and counts are integers, the equality is over ℚ and needs no
numerical bridge (unlike W₃/W₆); B-N can hold exactly.

## 2. Trial semantics — what one row IS, eligibility, dedup, and the honest remainder

**One row is a whole-attempt outcome for one pattern family.** Written by
`record!` (`learning_trial_ledger.clj:24-72`) from an
`attempt-learning/receipt` assembled at the token-outcome comparison
(`full_loop_runner.clj:3141-3152`, `retain-token-outcome!`). The banked
event (`:54-57`) is

```
{:schema :wm/attempt-learning-count-v1 :mode :record-only
 :identity <dedup identity> :family <configuration digest>
 :meaning-sha256 <…> :observed <boolean> :increment {:success _ :failure _}
 :contract <attempt-learning contract> :trial <the receipt row>}
```

(Live ledger: 12 rows of this shape in `data/wm-learning-trials/attempts.edn`;
a 2026-09-21 write shape additionally carries `:identity`/`:family` at the
top level, and `read-trials` honours both — :134-136.) The contract in each
row carries `:trial-grain :selected-cascade-effect-attempt` and
`:does-not-establish #{:individual-pattern-firing :pattern-causality
:production-parameter-update}` (visible verbatim in row 1's `:contract`).

**Accepted-trial eligibility.** Two gates, both in code:

1. *Write admission* — `record!` appends only receipt rows whose `:status`
   is `:admitted-at-attempt-grain` (:26-28); held rows carry
   `:counted? false` and a reason (:52-54).
2. *Update admission* — `b-update` is invoked only when the close exists
   AND the accepted-increment predicate returned `{:accepted? true}`
   (`full_loop_runner.clj:4207-4212`), and itself refuses anything but
   that verdict (`learning_trial_ledger.clj:175-181`,
   `:missing-accepted-verdict` / `:close-not-accepted`). The effect
   attributed is `[(:target action) [:evidence :acceptance-token]]` of the
   accepted verdict (`full_loop_runner.clj:4213-4216`); attribution by
   `producer-of` yielding `:no-declared-producer` or `:ambiguous-producer`
   writes nothing (`{:status :not-attributed}` :4217-4219).

**Deduplication is three distinct mechanisms, at three layers** — X₅'s
"duplicate one trial identity" must name which layer it exercises:

- *Ledger identity* (`record!` :43-51): a second append with the same
  `:identity` is HELD, with the reason discriminated:
  `:revised-meaning` (same family digest, different meaning hash),
  `:conflicting-observation` (same identity, different observation),
  `:duplicate-replay` (same identity, same observation). The held row is
  not appended, so concentrations cannot change.
- *Update occurrence* (`b-update` :196-201): the occurrence (matched as
  production identifies it, `:trial :deduplication :inputs :occurrence`,
  not the commit sha) already present → `{:status :already-recorded}`,
  no double count.
- *Read-side* (`b-update` :190 and `pattern-theta` :275): both collapse
  rows by `:identity` before counting, so even a ledger that somehow held
  two rows of one identity counts once. The two docstrings name the same
  posterior and must agree.

Note for B-C's emitter (strategy §B, note under the table): `accumulate`
itself is not a deduplicator — a duplicated tick adds twice by
`accumulate_conc`. Deduplication is entirely a property of the recorded
accepted-trial filter above, and the `:dedup` receipt B-C emits must say
which layer fired.

**What the mapping does NOT establish** (the honesty half of W₅'s
interpretation):

1. **A whole-attempt outcome is not an observation of each pattern
   transition.** Attribution is BY DECLARATION (`producer-of` docstring
   :75-93): the pattern that declared the accepted effect, not the pattern
   shown to have fired; with two declaring patterns the record contributes
   to nothing. The contract's `:does-not-establish` says the same in the
   row's own bytes. So `o_τ` observes the attempt's accepted effect, and
   the correspondence assigns that observation to the declaring family's
   cell — it does not observe the precedence's other transitions at all.
2. **The singleton belief is not the token posterior.** Eq. (21)'s `s_τ`
   is a posterior state belief over the model's state space; here S is one
   cell per family and `s_τ` is the constant 1 on it. Nothing in the
   ledger row carries a token-level posterior for τ, and B-C must not
   reconstruct one retrospectively (strategy §1.6: "Do not invent
   posterior trial vectors retrospectively").
3. **Pooling is an assumption, not a measurement.** Cross-target and
   cross-token pooling (:244-256) is what makes a family learnable at all
   and is recorded with its evidence (`:targets`,
   `:unattributed-rows`), but the correspondence does not justify it; it
   only makes it visible.

## 3. Commit point and global order

**Write side.** The ledger append (`record!`) happens inside
`retain-token-outcome!` (`full_loop_runner.clj:3141-3152`), i.e. during the
close's retained-evidence assembly; the B *update computation* (`b-update`)
runs strictly AFTER the close is written, only on `{:accepted? true}`
(:4190-4212, comment :4190-4206). `b-update` itself persists nothing
(:186-194: "the recording itself happens through the existing record!
path"; it computes, states, and refuses re-computation). So the effective
commit point of a new B version is the accepted close: append at close
retention, update stated post-close.

**Read side.** The next click's pre-selection read is the judge's
`theta-consumption` assembly (`scripts/futon2/report/war_machine.clj:6375-
6386`): for every pattern `p` in every joint candidate's precedence it
calls `learning-ledger/pattern-theta` and stamps `:theta`/`:theta-source
:recorded-trials` plus provenance onto the pattern BEFORE scoring
(:6387-6393+). `with-pattern-theta` passes a present `:theta` through and
only defaults when absent (`cascade_model_manifest.clj:271-280`). The read
is per-family, unconditional over the candidate field's patterns, and
happens before the scorer sees the candidates.

**What the record carries today about that order** — and it is thin:

- the ledger's own append order (`attempts.edn` is append-only with an OS
  file lock, :9, :33-35) and each row's `:identity`;
- the close record's `:recorded-at` and the retained
  `token-outcome.edn`/`surprises.edn` files beside it;
- the b-update result map in the run trace (`:status`/`:theta`/`:rule`/
  `:trials-read`/`:read-back` at :217-234), passed into the discharge
  context as `:b-update` (`full_loop_runner.clj:4241`);
- on the decision record, the stamped `:theta-source :recorded-trials`
  with `:theta-provenance` (the Correction's live evidence: run
  `2026-09-23-1790199409` carries it on 85 pattern occurrences, θ = 3/4
  and 1/4 on the selected action's precedence).

What is NOT on any record today: a version/hash of the B array the judge
read, a `:read-at`/commit-point for that read, a predecessor-chain, or any
root `:B-read` key — I could not show one on the runs inspected
(machinery-70 through 76 closes and the 1790199409 trace record; the
absence is by inspection of their key sets, not inferred). `pattern-theta`
recomputes from the append-only file at read time with no version
identity, so "the pre-selection B version equals the latest accumulated
version" (W₅'s last sentence) is today true only by the accident of an
append-only file with no concurrent writer window between close and next
click — nothing records it. **That is exactly B-R's gap** (strategy row
36): the unconditional root `:B-read` with `:version`, `:read-at`,
`:commit-point` and `:predecessor-chain` per CERT-S §1's B row
(`[:decision :selection-certificate :B-read :predecessor-chain]`, value
form "vector of read maps `:version`/`:read-at`/`:commit-point`"), with
`:model-inputs <id> :B` carrying the consumed θ and version per candidate
(B-T, row 37, binding the stamped theta at `war_machine.clj:6380` to the
read version).

## 4. The carrier B-C must emit at the close

Per CERT-S §1 (B row) and P₅, the producing close must record `[:b-update]`
with, in CERT-S value forms:

- **`:prior-concentrations` / `:posterior-concentrations`** — exact-rational
  maps `{achieved r, not r}` per updated family (rationals with
  denominator 2; no doubles, no bridge needed);
- **`:trial-identities`** — content refs of every ledger `:identity`
  contributing to the posterior, in ledger append order, with each row's
  `:observed` cell and `:theta-key`;
- **`:dedup`** — the receipt of which dedup layer fired for this update:
  `:none`, or `{:status :already-recorded :layer :update-occurrence}`, or
  the held-row reason map (`:revised-meaning` / `:conflicting-observation`
  / `:duplicate-replay`, layer `:ledger-identity`);
- **`:version`** — a content hash over the posterior concentrations plus
  the ordered trial identities (the value `:B-read` will later cite);
- **`:normalization`** — the recorded map from
  `{:conc-achieved a, :conc-not b}` to `theta = a/(a+b)`, so X₅'s "scalar
  theta with no concentration array and normalization must fail even at
  3/4" has something to fail against;
- typed absence `{:status :missing :reason :no-eligible-outcome}` when the
  close was not accepted or attribution was `:not-attributed` — never an
  omitted key (CERT-S §1 absence rule).

This agrees with the existing `:b-update` result map's provenance fields
(:217-234) and adds the arrays/trials/dedup/version/normalization P₅
requires; it is a carrier build, not a learner change (Correction:
"must-build shrinks to the certificate carrier").

## 5. Falsifier, restated as a concrete bad case

From strategy row 33: **a whole-attempt success asserted as a token
posterior without a mapping.** Concretely: a close carrier that, on the
strength of one accepted attempt whose `:theta-key` is
`:apparatus/done-is-observed-running`, emits
`s_τ = {["<target>" :restoration-accepted] 1}` as "the posterior state
belief" of that trial. S here is one cell per family; the ledger row
contains no token-level belief; emitting one is exactly the retrospective
invention §2 (2) forbids, and B-C's checker must refuse it. Second
falsifier: **a duplicate trial that changes concentrations** — replay one
ledger `:identity` through the accepted-trial filter: at the
`:ledger-identity` layer the row is held (`:duplicate-replay`), at the
`:update-occurrence` layer the update reports `:already-recorded`, and at
the read layer the identity collapse counts once; if any of the three lets
`conc(achieved)` move, the boundary in §2 has failed and X₅ fires.

## 6. What this packet unblocks

- **B-C (row 34)** has its exact emit contract (§4), including which dedup
  layer the `:dedup` receipt must name and the absence form.
- **B-N (row 35)** has its exact lemma statement (§1): the
  Fin 2 × Fin 1, Jeffreys-prior specialization of `accumulate` with
  normalized-cell equality over ℚ — no numerical bridge required, and the
  `accumulate_onehot`/`accumulate_append` lemmas it reduces to already
  exist.
- **B-R (row 36)** has its named gap (§3): the missing
  version/read-at/commit-point/predecessor-chain record, specified against
  CERT-S's B row.
- **B-T (row 37)** has its join grain: per-pattern-id (`:theta-key`),
  consumed at `war_machine.clj:6375-6393`, substituted through
  `with-pattern-theta` — the join W₅'s "substituting it in
  `InterpretedPattern.theta` reproduces the consumed `cascadeKernel`"
  needs.

## 7. Proposed amendments (missing definitions the theorem needs)

1. **Define "the relevant posterior concentration row" as the singleton
   state row of the declaring family.** W₅ says theta equals the
   normalization "of the relevant posterior concentration row"; §1 shows
   the only well-defined reading under the actual carrier is
   `conc_n(achieved) / total` at the family's singleton state. Proposed
   amendment to W₅: replace that phrase with "the normalized
   achieved-cell `conc(achieved)/(conc(achieved)+conc(not))` of the
   declaring pattern family's two-cell posterior."
2. **Define "trial vector" for X₅/P₅ as the singleton-carrier pair, not a
   token pair.** P₅'s "trial vectors" and X₅'s "alter one state-belief
   component" presuppose an `s_τ` with components to alter. Under S's
   actual grain the only alterable component is `o_τ`'s cell. Proposed
   amendment: X₅'s second clause reads "alter one outcome cell (the
   `:observed` boolean of one contributing trial identity); the pointwise
   accumulated cell must change," and the state-belief-alteration bad case
   is marked inapplicable to the singleton carrier by construction (with
   the reason recorded: no token-level `s_τ` exists in this learner).
3. **Name the dedup layer in X₅.** As in §5: X₅'s duplicate clause should
   enumerate the three layers and require the certificate's `:dedup`
   receipt to state which fired.
