# E-live-loop-1 — from honest nothings to a loop that pays

**Status: OPEN (2026-07-04, late night; operator-directed). Driver:
claude-19. Reviewer: claude-16. Operator gate: Joe (arming decisions are
his alone). Parent context: M-evaluate-policies (C10/E7-adjacent
territory), Flight 1 of the WM transparency flights.**

## Origin — the operator's critique (2026-07-04, verbatim in substance)

We pick M-first-flights as the source of a (have,want) magnet — unclear
why — then go through a whole large process to get "honest nothings."
Considered in functional terms this is a *waste* of energy, not a
minimization of energy.

The system's own ledger agrees: **674 arena ticks, zero cascade wins,
every live fold an abstention.** The cascade lane computes wholeness/F
per tick for a channel that has never influenced a decision and has no
mechanism to learn from being ignored.

## Findings carried in (Flight 1 + same-evening analysis)

1. **The honest nothings are structural, not incidental.** The classical
   fold's entire reach is a 10-entry rule table (devmap/math family);
   the LLM fold — the only constructor for contentful patterns — is held
   unarmed on the live path (correct consent design). Energy spent
   upstream of a permanently-disabled constructor is overhead, however
   honest the abstentions.
2. **ψ grain is wrong.** ψ = mission title + status line (a ~368-byte
   state summary, not a tension statement). rel-max 0.346 on Flight 1
   means the pattern library barely resonates with banner prose; thin ψ
   in → size-1 cascade out is arithmetic. The right-grain magnets exist:
   **sorries** — typed have→want gaps with content (sorry-arrow
   contracts; the mining chain; the held-work ledger).
3. **The escrow was designed for exactly this and has never been fed.**
   fold_llm's real turn-fn is meant to be injected out-of-process or
   replayed from *escrowed fold-turns* — pay the constructive step once,
   out-of-band, under consent; replay cheaply on the live path. The
   architecture anticipated the fix; it has simply had no deposits.
4. **No feedback from futility.** The lane is memoized (so the waste is
   architectural more than thermodynamic) but 0-for-674 is an
   observation the system never updates on. The learned-γ thesis
   (sequel paper) applied to this lane is the meta-fix: precision over
   the channel should fall when the channel never pays — or the
   persistent surprise should drive repair.
5. Supporting record: p4ng/flights/ (two leg logs, aggregation proposal,
   transcript + analysis); the act-gate ΔF∧ΔG verdicts with real numbers
   (canon-fingerprint fail on ΔF=−0.874 despite ΔG=−0.25; bayesian pass
   on ΔF=+1.049 ∧ ΔG=−0.2); the argue-exhibit's two-currencies finding.
6. **0-for-674 was over-determined: the LLM fold has no live caller.**
   (Found during the S2 design pass, 2026-07-04.) The live
   reconciliation — `futon2.aif.close-loop/act-gate-from-lane-entry`,
   `close_loop.clj:30` — tries move-set `:G-rollout`, then the
   *classical* fold, then abstains. `llm-fold` is never invoked on the
   live path. So the honest nothings had two independent sufficient
   causes: the constructor was unarmed (finding 1) AND the lane never
   read it. Feeding the escrow without adding the replay seam would
   change nothing; the seam is therefore an explicit S2 deliverable
   (designed in `E-live-loop-1-s2-escrow-design.md` §C — build gated
   like everything else).

## The fix ordering (the excursion's spine — do them in this order)

- **S1 — sorry-grain ψ.** Re-run the Flight-1 measurement with ψ built
  from sorry/hole text instead of banner text, same constructor, same
  budget. Deliverable: a before/after table (rel distribution, cascade
  sizes, F) over ≥10 real sorries. This is pure measurement — no arming,
  no writes. Prediction to test: rel and size rise materially.
- **S2 — one escrow deposit, operator-armed.** Design the escrowed
  fold-turn shape (what gets recorded, where it lives, how the live loop
  replays it), then — ONLY on Joe's explicit word — run ONE real
  LLM-fold turn (an agent constructs wiring for one S1 cascade) and
  escrow it. Deliverable: the first recorded fold-turn + its replayed
  ΔG on the live path. The arming is a single operator decision, scoped
  to one fold.
- **S3 — γ over the lane (design only in this excursion).** Specify how
  realized lane-outcomes should modulate the energy/precision allocated
  to cascade evaluation (the 0/674 observation becoming an update).
  Build belongs to a successor; this excursion delivers the design note.

## Constraints (hard)

- **Mistakes-ledger gate (operator-directed, 2026-07-04):** before each
  S2/S3 step, check `futon2/holes/mistakes-remedies-ledger.md`; no step
  may regress a listed remedy, and any NEW mistake made during this
  excursion gets a ledger entry (mistake / remedy / evidence check)
  before work continues. Evidence observed for or against existing
  remedies gets logged in the ledger as encountered.

- I-0 throughout: no second serving process, no restarts; in-JVM work
  via Drawbridge read-only evals; dev JVMs short-lived, capped, one at
  a time, timeout_ms always passed.
- S2's arming is the operator's word, given per-fold, never assumed.
- Gates on any code: clj-kondo 0 errors + check-parens; runtime
  assertions per the K2 discipline (silent-wrong-results are the known
  hazard class of this stack).
- Honest-nothing results remain reportable results: if S1's prediction
  fails (sorry-ψ does NOT raise rel/size), that is the finding, not a
  failure — kill criteria: two S1 re-designs without material change →
  stop and report.

## S1 log (driver: claude-19)

**S1 DELIVERED 2026-07-04 late night — prediction CONFIRMED, with
structure. REVIEWED & APPROVED by claude-16 (independent re-run of the
state-snapshot-witness row: byte-identical).** Full table + caveats:
`p4ng/flights/e-live-loop-1-s1-sorry-grain.org` (raw JSON + runner
alongside). Headline:

- Corpus: 12 real sorries (6 A-next EMPIRICAL gold-corpus + 6 CLean
  typed holes), mechanical ψ recipes, baseline banner re-run as live
  control (reproduced Flight 1 exactly: rel 0.346, size 1, F +0.046).
- **rel rose 12/12** (median rel-max 0.485 vs 0.346); **size rose 7/12**
  (median 4 vs 1; five cascades exceeded the budget-6 window — first
  truncations in this lane's record). Not a ψ-length artifact.
- New finding: **F now discriminates.** Two regimes — 5/12 rich
  cascades with F strongly positive (up to +8.6 vs baseline's +0.046)
  vs 7/12 F<0 rejects (rare-pattern singletons + one sprawl). At banner
  grain the gate only ever saw thin marginal accepts; at sorry grain it
  has variance — the raw material S3's γ needs.
- Caveats logged: provenance overlap (eponymous-pattern self-match;
  ablation offered), iChing-singleton oddity, λ=0.25 still provisional.
- S2 escrow candidates from the F>0 regime: state-snapshot-witness/:s4
  (rel 0.610, F +2.654) or autoclock-in (has a sealed ground-truth
  wiring to score against). No arming implied; Joe's word only.
- Ablation (post-review, claude-16's requirement): eponymous-pattern
  exclusion changes 11/12 rows not at all; state-snapshot-witness drops
  to rel 0.530 / F +2.139 / size 7 — still F>0, still above baseline.
  The S1 lift does not rest on self-match. Log: ablation addendum in
  the S1 org file.

## S2 design (driver: claude-19)

**S2 DESIGN DELIVERED 2026-07-04 — no deposit, no arming; those wait on
Joe's word, per-fold.** Full note:
`futon2/holes/E-live-loop-1-s2-escrow-design.md`. Shape of it:

- **Deposit record**: one `ft-*.edn` per fold-turn, a new `:authored`
  confidence tier of the sortie-11 mission-triple schema (hole +
  cascade + wiring + `fold-turn/…` rule family) — not a parallel
  invention. Three hard pins: replay only on exact prompt-sha match
  (drift ⇒ abstain, as today); no arming record, no deposit (Joe's
  verbatim word is a field of the file); stored ΔG re-asserted against
  recomputation on every load.
- **Home**: `futon6/data/fold-turns/`, sibling of
  `futon6/data/mission-triples/`; consumable unchanged by the
  cascade→metric adapter. Not meme.db, not a substrate-2 write.
- **Replay seam**: load-bearing discovery — `close_loop.clj`'s live
  reconciliation calls the *classical* fold only; the LLM fold isn't on
  the live path at all, so 0-for-674 was over-determined. Design: a
  pure `fold-escrow` ns (hash-gated table lookup as `turn-fn`) + one
  added `:delta-G/source :fold-escrow` branch after classical abstain.
  No LLM call in the JVM ever; nil on mismatch ⇒ byte-identical to
  today; I-0 safe.
- **The one deposit, when armed**: recommended candidate =
  `autoclock-in` (its A-next EDN is a sealed ground-truth wiring, so
  the armed turn can be *scored* against an answer key);
  alternative = `state-snapshot-witness/:s4` (strongest cascade
  post-ablation). Joe rules.
- **Phase B convergence** (claude-16's direct-path reading: arming
  condition "terms constructed + metric understood" reads satisfied —
  sortie-11 triples landed, M-substrate-metric at R2 depth): the
  fold-turn record is deliberately a sortie-11-family triple, so each
  deposit feeds Phase B's state-action space and the metric's golden
  triplets at once. Same work stream, converging — whichever way the
  operator rules.
- **Seed patterns as starting places** (operator input, futon3
  `d433bbb`; design in the S2 note §F): the two process-coherence
  seeds are *triply* invisible to the constructor today — not in the
  embeddings artifact (regen step: `futon3a/scripts/index_patterns.sh
  --minilm`), not in `phylogeny["patterns"]` (hard eligibility filter
  at `cascade_construct.py:188-197`: unregistered patterns are
  display-only, never selectable), and
  priced at the unseen-pattern prior (2.398 nats — exactly S1's
  rare-singleton cx; the F-gate would fine every cascade for following
  the operator's discipline). Recommended mechanism: **explicit
  seeding of the accretion** (`chosen` initialized from a
  `pattern-seeds.edn` registry with operator provenance, rel sanity
  floor, normal ledger entries) **+ a designation-prior floor** at the
  median pattern's prior (≈0.52, cost 0.65 nats — self-retiring once
  real co-app mass accrues). Rejected: reserved box outside the ledger
  (exemption by another name), alpha boost (fragile, doesn't touch the
  prior hammer). Costs stated in the note: seeded/unseeded cascades
  not F-comparable (output gains `:seeds`); designation floor audited
  by S3's γ; ingest touches two artifacts (wrap as one documented
  step). No build tonight.
- **Open questions (explicitly open):** (1) the seed prior-floor value
  — median-pattern prior ≈0.524 is the recommendation, not a ruling;
  (2) whether the replay seam build (a few lines in
  `close_loop.clj` + a new pure ns) counts as "trivial" or waits with
  the deposit — reviewer/operator call; (3) deposit candidate
  (autoclock-in recommended, state-snapshot-witness/:s4 alternative) —
  Joe's, with the arming word; (4) lineage-level ablation (via
  `pattern-origin` hyperedges) only if the S1 finding is contested at
  that depth.

## S3 — γ over the lane

**PENDING — next session.** Inputs already on the record: the S1
two-regime F distribution (the lane's first real outcome variance) and
the seed-designation audit hook (§F of the S2 note). Design only, per
the spine.

## PAR (driver, 2026-07-04 close)

- **What worked:** (1) *Measure before designing* — reading
  `cascade_construct.py` instead of trusting the architecture story
  surfaced the phylogeny eligibility gate (`:188-197`: unregistered
  patterns are display-only) that no design discussion had mentioned;
  the same pass explained S1's mystery constant (rare-singleton cx
  2.397 ≡ −log of the unseen-pattern default prior 0.0909) — a
  cross-check that turned an observed number into a mechanism. (2)
  *Live baseline control* — re-running the Flight-1 banner ψ in the
  same process before measuring deltas made the S1 table
  review-verifiable (claude-16 reproduced a row byte-identically). (3)
  K2 assertions in the measurement script caught a real discrepancy
  (rounding residue) cheaply — the discipline paid even when the answer
  was benign; source was checked before dismissing.
- **What to carry:** the no-live-caller finding (finding 6) came from
  tracing the *call path*, not the component — components can each be
  correct while the composition never runs; audit the wiring, not the
  boxes. And the checkpoint discipline held (bells sent AT checkpoints,
  doc updated as the record, not after).

## Exit

S1 table delivered and reviewed; S2 escrow shape designed (deposit made
only if armed); S3 design note written. Then the excursion closes into
whatever mission the operator wants to grow from it — the before-picture
(Flight 1's honest nothings) is already on the record.
