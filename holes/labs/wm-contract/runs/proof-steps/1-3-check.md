# Proof ⟨1⟩3 CHECK — read-only, through the LIVE serving JVM

zai-1, 2026-09-22. Evaluated against the serving JVM via
`futon3c/scripts/proof-eval.sh` (freshly reloaded: 42 namespaces current),
on the frozen reference input (`runs/proof-reference-field/`, target
T-repair-occ-444fb018…, candidates :C1 recheck / :C2 declare-the-split).
The probe replicates the production joint-decision wiring (the qualifier,
the class model, T, Joe's C) and calls the real contracted functions:
`efe/rank-actions`, `cascade-selection/selection-posterior`,
`cascade-selection/bayes-choice`. No files changed except this note.

## 1. The reference ticket's classification (live)

`{:class :focus, :kind :ticket-parent, :focus-status :retained,
:focus "WM"}` — the ticket classifies **:focus** through its Parent
(M-aif-policy-conditioned-eig's corpus relation, WM/focus), derivation
recorded `:ticket-parent`; the focus context at the decision time is
**:retained WM** (decision after the last window's validity; original
evidence date 2026-09-22T17:31:44Z).

## 2. Per candidate at T = 4

| | :C2 (declare the split) | :C1 (recheck) |
|---|---|---|
| E (habit) | cold start, uniform 1.0 (no history for either family) | same |
| F | **absent with reason** `{:value 0.0 :status :not-supplied :reason :class-model-unconditioned-at-selection}` — not recorded-as-0, explicitly not supplied | same |
| γ (= 1/β, β declared 1) | 1 | 1 |
| G | **0.5978** (= ln(1/0.55)) | **2.9957** (= ln 20) |
| per-step g | τ1=0, τ2=0, τ3=0, **τ4=0.5978** | τ1=0, τ2=0, τ3=0, **τ4=2.9957** |

Every intermediate step is exactly 0; the whole risk is the horizon's
class KL. C1's recheck guard fails on today's evidence-absent q₀ (its
obstruction-observation is stop-the-line at the horizon); C2 reaches
restoration-accepted on the focus-classified ticket.

## 3. The action marginal

First actions differ (:C2 → `:aif/declare-the-conditioning`,
:C1 → `:apparatus/done-is-observed-running`). The summed marginal over
first actions is **{`:aif/declare-the-conditioning` 0.9167,
`:apparatus/done-is-observed-running` 0.0833} — a UNIQUE maximum** at
C2's first action (posterior {C2 0.9167, C1 0.0833}).

## 4. Full law vs log E − F alone

- **Full law σ(ln E − F − γG):** chooses :C2
  (`:aif/declare-the-conditioning`), mass 0.9167 — a genuine maximum, no
  tie.
- **log E − F alone (G suppressed, same γ):** posterior is
  **{C2 0.5, C1 0.5} — an exact tie** (E uniform, F not supplied, so the
  score is constant). The name-order tie-break resolves the tie to
  `:aif/declare-the-conditioning` — the alphabetically first action name
  ("aif" < "apparatus"), which happens to be C2's. So log E − F alone
  does NOT choose on its own; the tie-break chooses.

## 5. Was the tie-break needed?

- Under the **full law: NO** — the maximum is unique (0.9167 vs 0.0833);
  the tie-break rule is recorded but not exercised.
- Under **log E − F alone: YES** — 0.5/0.5 tie, resolved by
  `:action-name-ascending`.

So G's contribution is exactly Joe's amended reading: it turns a
tie-break-resolved coin-flip into a unique maximum. (Correction to my
earlier design notes: name order picks C2 here, not C1 — "aif/…" sorts
before "apparatus/…".)

## 6. Horizon authority and C provenance

- The tick's horizon: **T = 4**, authority
  `[{:source "T-repair-occ-444fb018.edn" :horizon-steps 4}]` — the
  source-declared common horizon, lifted and named by file
  (war_machine.clj:6821 reads `:horizon-steps-declarations`).
- The scoring spec: `:c {:status :class-observation :source "Joe
  2026-09-22 ruling (55/35/5/5; unmeasured → stop-the-line)"}`, with
  live-c derived and recorded, never scored.

## Honest limitations of this check

- The probe replicates the production wiring by hand around the real
  contracted functions (the tick's own cascade-decision requires a live
  click context); every function called is the production one, and the
  wiring constants match the committed production code.
- Habit E is a cold-start uniform (both families have zero recorded
  trials); the live path's habit read would return the same neutral
  state today.
- F is explicitly not-supplied by the class model at selection
  (unconditioned); the score therefore did not subtract an F term, and
  the record says so rather than silently zeroing it.

## Addendum: build-2 fallout — the two runner-test failures (read-only discovery)

claude-5 is right that these are build 2's, not pre-existing: my stash check
was invalid for two reasons claude-5 named (the test compares serving source
to the canonical checkout, so a stashed tree ERRORS rather than failing; and
tests run in their own JVM, so reloads cannot mask anything). The failing
assertions are exactly what class scoring replaced.

### 1. Where the precision-model q0 is dropped (file:line)

The class path's scorer writes its meta at
`cascade_observation_scoring.clj:170-176`: the ranked vector's meta carries
only `{:cascade-scoring {:model model :scope :synthetic-bounded-replay
:horizon-steps …}}` — **no :precision-model, hence no :q0**. The token path
that the runner test asserts writes it at `efe.clj:1326`:
`{:cascade-scoring {:precision-model {:q0 q0 :rates rates :horizon T
:preference-spec spec :zeta …}}}` — attached only by the token scorer
(efe/rank-cascade-actions' non-observation-model branch). war_machine.clj:6409
reads `(get-in (meta ranked) [:cascade-scoring :precision-model])` → nil under
the class path. The q0 itself is NOT lost — it is `(:cascade-belief state)`
passed INTO the scorer, and each entry's certificate carries the per-step
beliefs (`score-candidate`'s `:prediction {:initial-belief q0 …}`) — what is
missing is the meta-level :precision-model slot the precision-family/carry
machinery reads. So: dropped by omission in
cascade_observation_scoring.clj:170-176, not by any computation.

### 2. What the preference audit should record under class C

The audit's :recorded status requires `common?`
(preference_audit.clj:66-68): every scored candidate's `[:c :value]` equal and
every `(:scoring certificate)` row carrying the same consumed-preference
provenance. The token path wrote those rows (policy.clj:227's
`:rates-provenance`/`:c` select-keys); the class path's certificate rows come
from score-candidate (cascade_observation_scoring.clj:88-101) and carry
`:rates-provenance {:source :observation-model/query :model observation-model}`
plus `:c {:form :step-indexed :schedule … :steps …}` — the shape differs, and
`preference-audit`'s attach sees no shared token preference, so it holds with
`:shared-consumed-preference-not-retained`. Two viable options, not implemented:

- **(a) record the class preference:** teach preference-audit to accept a
  class-scoring provenance — when the certificate's scoring rows share the
  class observation model (:kind :class-emission) and the model's
  :class-preference at the horizon is Joe's fixed 55/35/5/5, the audit records
  THAT as the consumed preference with its provenance (status :recorded, a
  :consumed-preference-kind :class-emission field). The audit stays meaningful
  — "one preference was consumed by every scored candidate" — with the
  class rather than token vocabulary.
- **(b) an honest typed status:** a deliberate `{:status :held :reason
  :class-scoring-consumes-class-c}` — but then (checked): the runner test's
  assertion changes deliberately, and `scoring_input_receipts.clj:54-56`'s
  validate-record* only rejects a :preference-audit-mismatch when
  `preference-audit/valid?` fails — valid? (:150+, build/rebuild equality) is
  unaffected by a held status, so the record validator still accepts it.
  `wm_run_validity.bb` does NOT reference preference-audit or precision-family
  at all (grep: zero hits), so validity scoring is untouched either way.

I recommend (a): the certificate ⟨1⟩5 reads should say what was actually
consumed, and :recorded is true in substance — Joe's class C WAS consumed by
every scored candidate.

### 3. Every consumer of :preference-audit / :precision-family

- `scoring_input_receipts.clj:29-30` (with-preference-audit — attaches it to
  every decision) and `:54-56` (validate-record*: rejects only on
  preference-audit-mismatch via preference-audit/valid?, which compares
  rebuild equality — a held audit does not mismatch).
- `run_narrative.clj:433-434, :496` (preference-audit-text — renders the
  audit into the run narrative; a held audit renders its reason, degraded but
  honest).
- `war_machine.clj:6409` (precision-model → model-id → the
  precision-carry/advance chain → `:policy-precision-state` on the
  certificate): with nil it produces nil model-id — precision_carry.clj:70-75
  holds (:precision-model-invalid / identity-mismatch paths) rather than
  crashing; the state's hold is recorded, and the carry chain reinitialises
  rather than advancing.
- `policy.clj:227` (the token certificate's own :scoring rows — the writer,
  not a consumer).
- The close path: nothing in the close judgment or run-ending
  classification reads either field (grep over run_ending_classification.clj:
  zero hits); the accepted-increment predicate and B update (⟨1⟩4) do not read
  them.
- `wm_run_validity.bb`: zero references — validity scoring treats both as
  absent-and-fine.

So the blast radius of the current :held/nil-q0 state is: one narrative
degradation, one precision-carry hold, and the runner test's two assertions.
Nothing downstream refuses or misjudges a close. The fix is a certificate
honesty question, exactly as claude-5 framed it — best resolved by option (a)
plus attaching the class-path q0 into the precision-model meta (the q0 exists;
the slot is simply not written on the class path).
