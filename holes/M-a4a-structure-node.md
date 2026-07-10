# Mission: A4a — the R17 structure-learning node (capability concept-formation)

**Date:** 2026-07-08
**Status (2026-07-08):** Increments 1–2 **BUILT + reviewed-green** (BMR kernel `4064533`, concept
  pipeline `8200f08`) — basic pipeline runs end-to-end. R17 patterns + pattern-map section written.
  **3a** (ambiguity → A1 forward ranker, per Seam-2) = the clean first slice. **3b** (ambiguity → γ
  calibration leg) + **GFlowNets / candidate-slush** are **PARKED pending a loop-closure strategy to
  be agreed with claude-4** — Joe 2026-07-08: "too many liabilities to proceed with confidence."
  Strategy agenda sent to claude-4 (the enact.clj seam liabilities below).
**Owner:** Joe + claude-5 (architecture / review). Build → Codex (handoff, per CLAUDE.md default).
  Interface counterpart: claude-4 (will review the build spec the reviewed-lane way).
**Repos / files:**
  - positions against `futon2/src/futon2/aif/enact.clj` (A5 seam) + `futon2/aif/actuator_a3.clj` (A3).
  - card: `futon2/holes/aif-wiring-actuator.html` (the A1–A7 pipeline + the R12/R13 known-gap).
  - substrate-2 store (XTDB :6768 via authed Drawbridge; `:7071` read is blind — futon1a#6).
**Cross-ref:** [[M-G-over-cascades]] (G over cascades = the candidate-edge downstream) ·
  [[E-gflownets-fold]] (GFN as an *implementation detail* of the slush, earned later) ·
  [[project_bayesian_structure_learning]] (BMR over Dirichlet reliability posteriors) ·
  Neacsu 2024 thesis Ch.2/Ch.4 (SL ≡ Bayesian Model Reduction; concept-formation; the
  flexible-vs-rigid dial) — `~/Downloads/Victorita Oana Neacsu - Structure Learning ...pdf`,
  PLOS ONE 2022 `10.1371/journal.pone.0277199`.

---

## HEAD (operator intake)

*Provenance: Joe, emacs-repl, 2026-07-08.*

The actuator's deferred gap — **"the write feeds capability, and capability re-ranks the WM"** — is a
**structure-learning loop**. Joe's framing: model **cascades as a slush** (a distribution over
compositions held molten) sampled ∝ reward, then let structure learning crystallize it. GFlowNets are
*one implementation* of the slush sampler — a good candidate, but an implementation detail, not the
commitment. The commitment is the module: accumulate discharge evidence → form/prune capability
concepts (BMR) → feed the WM's decision.

The mapping onto Neacsu's three levels is one-to-one: the **A4 write** = Parametric Learning (a
Dirichlet count accrues on the capability likelihood); the **re-rank** = Structure Learning (BMR
concept-formation, offline). Neacsu's own result — BMR *increases precision of the valid
contingencies* and merges look-alike concepts — is exactly what breaks the WM's G-ties false-floor
and populates the sparse capability-star.

## Placement (arranged with claude-4, 2026-07-08)

**A4a = the R17 node**, inserted after A4 (write) and ahead of A5 (measure). It is a **slow loop**:
it reads the *accumulated* `:capability/*` corpus + `:discharge` records, not this tick's single
write. It fills a *specific, real* gap — A5 today grounds only the EFE **risk** leg (R5a); the
**ambiguity** (epistemic) leg (R5b) is open, and **BMR posterior variance over the accumulated counts
IS that ambiguity term.** So A4a is not wedged in — it completes the EFE.

Two downstream edges:
- **ambiguity edge → A5** *(this mission's first slice)* — the BMR posterior's spread grounds A5's
  open epistemic leg.
- **candidate edge → A1** *(deferred loop-closure)* — the candidate-slush proposer feeds "what to
  build next" = the R12/R13 re-ranking (formally A6→A7→A1). **Not** slice 1; it comes after A4 fires
  in prod and there is a real corpus of writes.

## The contract (input / transform / outputs / trigger)

Per the arranged interface (claude-4's live substrate pull; shapes to be re-confirmed at build time):

- **Input:** accumulated `:capability` entities (the vocabulary; 31 exist, currently
  `"demo provenance"` seed data) + `:discharge` records (the A4 write):
  ```clojure
  {:xt/id :discharge/M-learning-loop--CapabilityVocabulary--<ts>
   :entity/type :discharge
   :discharge/mission "futon5a-d/mission/learning-loop"
   :discharge/endpoint "CapabilityVocabulary" :discharge/type :capability
   :discharge/proof-query "..." :discharge/at "<iso-ts>"}
  ```
- **Transform:** BMR over the Dirichlet concentration parameters implied by the accumulated counts →
  a posterior over capability concepts (prune redundant slots when ΔF ≤ −3; merge look-alikes).
- **Output → A5 (slice 1):** the posterior **stddev** (NOT raw variance — variance is
  `endpoint-count²` and trips the SCALE-MATCH pin; stddev is `endpoint-count`, matching the risk leg,
  λ≈1). It attaches to the **expected/forward** side only: A5's `:expected-G` gains
  `:eig <stddev, :endpoint-count>` and enters forward selection EFE as `risk − λ·EIG` (subtracted —
  epistemic value, sought; reclassified 2026-07-08). `:realized-G` stays **pure risk** — the actual
  outcome has no uncertainty.
- **Output → A1 (deferred):** candidate-slush proposals for the next decision.
- **Trigger:** **N new `:capability/*` writes accrued ∧ quiet-window** (BMR is heavy; honours the
  no-heavy-Drawbridge-during-active-work discipline). **Manual override** to exercise BMR on the demo
  counts during validation, since the static demo data means the data-driven trigger never fires yet.

## Locked interface with claude-4 (A6/A7 strand) — 2026-07-08

Two seams locked so A4a/R17 and A6/A7 build in parallel, converging at A1 with no rework.

**Seam 1 — capability-star, TWO entities (no shared doc; substrate-2 `::xt/put` is whole-document,
so shared-doc field-ownership would lose updates — last-writer clobbers):**
- **A4a (this mission) mints/owns identity:** `{:xt/id <star-id> :entity/type :capability-star
  :star/capability <concept> :star/updated-by :a4a-bmr}`.
- **A6 (claude-4) owns status, separate doc keyed off star-id:** `{:xt/id <status-id = det. fn of
  star-id> :entity/type :capability-star-status :status/star <star-id> :status/status :satisfied
  :status/updated-by :a6-discharge :status/at <inst>}`.
- A1 **joins** star + status. I never write status; A6 never writes identity. A6's flips are logged
  no-ops until BMR mints stars (expected on the demo map).

**Seam 2 — A1 ranking (one ranker, two contributors):**
- A1 candidate set = existing missions ∪ A4a new-candidates; **A1 owns dedup** (a new-candidate
  coinciding with an existing mission collapses to the existing entry, ambiguity still applied).
- `EFE = risk − λ·EIG`, both `:endpoint-count`, λ≈1 (SCALE-MATCH lock). **Reclassified 2026-07-08**
  (was `+ λ·ambiguity`): BMR-stddev is *reducible* model uncertainty = **epistemic value (EIG)**,
  SUBTRACTED/sought, not ambiguity-cost. The slush demo's −0.006 pull-toward-certainty caught the
  category error before 3a was coded. Sign fixed (−); tune λ, never the sign. See the pattern
  `posterior-variance-as-epistemic-value`.
- **A4a exposes `{concept → stddev}`;** A1 applies `− λ·EIG` per candidate, **absent-concept → 0**
  (existing missions carry 0 EIG now; extensible to non-zero later with no interface change —
  avoids re-baking the risk-only asymmetry A4a exists to remove). High-EIG candidates rank *better*
  (explore drive); this is what breaks the R13 G-ties floor. claude-4 owns the risk/ascent-progress
  recompute on existing missions.

## Logic model

1. A4 lands a durable `:capability/*` write (Parametric update) — *has fired in test; prod pending.*
2. A4a (slow loop) accumulates the `:capability/*` + `:discharge` corpus.
3. BMR reduces the over-complete count structure → posterior over capability concepts.
4. Posterior **stddev** → A5's `:expected-G` ambiguity leg (endpoint-count). ← **slice 1 ends here.**
5. *(deferred)* Candidate-slush proposals → A1 → EFE re-rank (the R12/R13 loop-closure).

## First slice (build target) + acceptance bar

**Slice 1 = accumulate → BMR → ambiguity-term feeding A5.** Not the A1 candidate edge (deferred).
Not a GFN (the slush sampler is only needed for the A1 edge; slice 1 has no sampling — BMR is
analytic/closed-form).

**Acceptance bar:**
- BMR runs (manual override) over the 31 demo-provenance `:capability` counts + `:discharge` records
  and yields a well-formed posterior over capability concepts.
- Its **stddev** is emitted in `:endpoint-count` units and attaches to `:expected-G` as
  `:ambiguity`, leaving `:realized-G` untouched — A5 consumes it without tripping SCALE-MATCH
  (verify λ≈1, no scale-asymmetry).
- Concepts produced this phase are labelled **demo-validated**, not real capability structure.
- Determinism: same counts → same posterior (no `PYTHONHASHSEED`/ordering nondeterminism).

**Honest status:** mechanism real, data demo — BMR validates on the 31 demo counts until
M-learning-loop's `:capability/*` drift closes (the real write). Real concept-formation lands then.

## Dirichlet parameterization + build increments (pinned 2026-07-08, claude-5 architecture)

**Parameterization (the crux modeling decision).** Following Neacsu Ch.4's *context factor*:
- **States (columns) = capability concepts** (the vocabulary; 31 demo `:capability` entities).
  **Outcomes (rows) = the missions/contexts they discharge into** (from `:capability/*`
  hyperedges `[capability, mission/doc]` + `:discharge` records).
- **Concentration counts:** each `:capability/*` edge and each `:discharge` (type `:capability`)
  adds a count to the (capability, mission) cell; plus a **uniform 10⁻¹ prior** (Neacsu's init) so
  unseen cells aren't zero.
- **Concept-formation = BMR merge-hypotheses.** For candidate capability pairs (i,j), construct a
  **reduced-prior that ties j→i** (j carries no outcome information beyond i — Neacsu Fig 4.4's
  hand-built alternative models), flatten (col_i, col_j) into the kernel's `(a, A, a')` triple,
  score with `futon2.aif.bmr`, **accept the merge when ΔF ≤ −3**. Merged pairs form concept
  equivalence-classes = the surviving concepts. *The reduced-prior construction is the load-bearing
  choice; pinned by synthetic tests (planted-identical pair merges; distinct pair does not — the
  concept-level analogue of increment-1's redundant/informative pin).*
- **`{concept → stddev}`** = `dirichlet-moments` over each surviving concept's row, aggregated to a
  per-concept stddev (`:endpoint-count`) — the Seam-2 ambiguity lookup.

**Demo honesty:** `:capability/*` edges are ~absent and `:discharge` ≈ 1 on the demo map, so the
real corpus has almost nothing to merge. Increment 2 validates the *mechanism* on **synthetic
corpora** (known merges), plus a live *read* smoke-test of the demo substrate — no claim of real
concept-formation until the `:capability/*` prod write flows.

**Increments:**
1. ✅ **Pure BMR kernel** (`futon2.aif.bmr`, commit `4064533`) — reviewed green.
2. **Pipeline + substrate adapter** *(next dispatch)* — pure `corpus→dirichlet→BMR-merge→concepts
   →{concept→stddev}` + `:capability-star` doc builder (Seam 1), unit-tested on synthetic corpora;
   thin adapter **reusing `actuator_a3`'s `drawbridge-eval`/`drawbridge-submit-tx!`/`admin-token`**
   for the demo read + (guarded, manual-override, quiet-window, no auto-write) star mint. **No
   `enact.clj`.**
3. **A5 γ-seam wiring** *(later, claude-5 drafts carefully)* — attach the ambiguity stddev to
   `:expected-G` in `enact.clj` (L208–227 decision map; respect the L182–183 classical-off γ hazard).

## Gates (required of the build handoff)

- clj-kondo clean (Clojure); `futon4/dev/check-parens.el` (parens); the relevant tests green.
- Reviewed-lane: Codex builds; claude-5 reviews as a real gate (read the diff, re-run the BMR on the
  demo counts, confirm the stddev units + the `:realized-G`-untouched invariant, spot-check numbers).
  claude-4 also reviews spec-derived (its offer).
- No server restarts; no heavy synchronous Drawbridge calls; quiet-window for any BMR-over-store run.

## Deferred (named, not buried)

- **The A1 candidate edge / re-ranking** (R12/R13 loop-closure) — after A4 prod write + a real
  corpus. This is where the **slush sampler** (and, if diversity earns it, a **GFlowNet**) lives.
- **Neacsu's flexible-vs-rigid dial** = the slush temperature / when-to-collapse-vs-retain — an open
  design question inherited for the A1 edge, not slice 1.
- **The stack-scale ambiguity** (substrate-2 `T`-field / Ollivier-Ricci, per M-G-over-cascades) — the
  candidate edge's richer reward; not needed for the A5 ambiguity leg.
