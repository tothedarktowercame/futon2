# Clojure census — entry-point-aligned audit of the core, via zai dispatches

Opened 2026-09-18 by claude-12 (Lean-layer owner), under Joe's commission
the same day: for every core Lean term, find the Clojure definition(s),
their entry points and inputs, and whether they match the Lean
specification; record the interpretation each term carries in this domain
(a supplementary harness for off-the-shelf coding agents); expect and
record duplicates and irrelevancy. zai does the scanning and bookkeeping
(token economy: the owner reviews ledgers, not the codebase); the core is
broken into a SEQUENCE of small dispatches, one term-cluster each.

## Ledger schema (every dispatch returns exactly this, one EDN file)

```edn
{:dispatch :D1
 :terms
 [{:term :G
   :lean {:census "DarkTower.AIF.expectedFreeEnergy"
          :carrier "DarkTower.WarMachine.PolicyHorizon.horizonEFE"}
   :interpretation {:text "a policy is a design-pattern cascade; ..."
                    :status :recorded}   ; :recorded (on file) | :proposed (needs owner review)
   :producers
   [{:var "futon2.aif.cascade-model-manifest/horizon-g-sparse"  ; NAMES only, never file:line
     :role :live         ; :live | :shadow | :dead | :duplicate | :test-only
     :entry-point "futon2.aif.efe/rank-cascade-actions"
     :inputs ["..."]      ; argument names + shapes as the code reads them
     :output "..."
     :callers ["..."]
     :evidence ["<command run and its result, verbatim>"]}]
   :duplicates ["..."]    ; producers computing the same term another way
   :irrelevant ["..."]    ; nearby namespaces that look related but are not
   :match {:status :matches | :diverges | :cannot-tell | :not-checked
           :notes "input-by-input vs the Lean signature"}}]
 :evidence-commands ["every grep/clj command run, reproducible"]}
```

Rules: names only (Joe's no-file:line ruling); every claim carries an
evidence command; interpretations found on record cite the record,
interpretations invented are `:proposed`; one EDN file per dispatch
(`D<N>-<slug>.edn` + optional `D<N>-<slug>-notes.md`), validated with a
one-form parse before commit (validate-edn-ledgers rule); commit to
futon2 main, bell back with the sha.

## Standing interpretations (already on record — cite, don't reinvent)

- **π (policy)** = a design-pattern cascade (`policy.clj` ns doc; DAG;
  FutonZero). A cascade IS a policy.
- **o (outcome/observation)** = tokens over the comparison universe
  (cascade grain) / the 13-channel observation vector (channel grain).
- **C** = preference over outcomes as weighted want tokens in the cascade
  spec (`NOTE-joes-view-of-C.md` — plus the institutional half, Lean-side
  only for now).
- **E (habit)** = declared-neutral 1 in production (a named reduction).
- **β** = caller-declared inverse temperature, no default (R14).
- **G** = risk-only on the live path under the identity-A reduction
  (known-failing vs full eq. 4.9; `AUDIT-aif-correspondence-ledger.md`).

## Dispatch sequence

- **D1 — the G cluster duplicate census** (dispatched first because Joe
  suspects duplicates, and this cluster has the most known aliases):
  `cascade_g`, `active_horizon_g`, `cascade_free_energy`, `core_efe`,
  `efe`, `shadow_cascade_g`, `cascade_model_manifest` — for each: role,
  entry points, inputs, callers; which are dead or duplicate; terms
  :G :risk :ambiguity.
- **D2 — selection**: :temperature (β), :policy-posterior
  (selection-posterior), :action (bayes-choice), E and F as inputs;
  vs Lean `OutcomeRiskKL.policyPosterior` / `PolicySelection`.
- **D3 — the model quartet**: :observe/A, B (transitions/cascade kernel),
  D (initial belief); where the Clojure defines an observation model at
  all; vs `ForwardModel` / `CascadeTransition`.
- **D4 — preference carriers**: C (live_c, preferences floor,
  ruled_outcome_c, cascade spec `:want`/`:weights`), E carriers
  (habit namespaces); wiring status of each.
- **D5 — F and Q**: :policy-free-energy, :belief-state, :belief-update
  (exact trajectory fns); vs `PolicyVariationalFreeEnergy` /
  `ExactBeliefTrajectory`.
- **D6 — precisions**: ζ declaration (futon2 fe55a1a0), γ/β machinery
  (policy_precision); vs `temperedLikelihood` and the :temperature row.
- **D7 — the remainder sweep**: every `futon2.aif` namespace not touched
  by D1–D6, classified :relevant-unbound / :duplicate / :irrelevant /
  :dead, so the 169-file surface is fully accounted.

Sequencing: strictly one at a time; the owner reviews each ledger for
format + spot-checks before the next dispatch, and folds results into
`AUDIT-aif-correspondence-ledger.md`.
