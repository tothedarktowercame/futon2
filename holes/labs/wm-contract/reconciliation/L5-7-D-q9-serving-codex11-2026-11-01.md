# L5-7-D — Q9 preference certificate into serving

Discovery only. Source snapshot: futon2 `b01c177cb30b962f62ed8adc72eff4316710e401` (main at worktree creation), mathlib4 `221f060818e5da31c4f9552dd07c8f227a99e69a`. Branch `codex11/l5-7-d`. Packet read in full. No source change, tests, data writes, JVM evaluation or reload. This traces checked-out serving code, not the identity of definitions loaded in the running JVM.

## 1. Attachment point and available inputs

The actual class-scoring implementation is **`cascade-observation-scoring/rank-cascade-actions`**, not the posterior selector or decision gate (`src/futon2/aif/cascade_observation_scoring.clj:295–326`). It consumes `:observation-model :class-preference` at every tau and retains the consumed C in its certificate (`:c :steps`, lines 270–276).

The ordinary route is the runtime's `:judge-fn` → `selection-judge` → `wm/generate-war-machine` (`full_loop_runtime.clj:1064–1084,1110–1118`); the inner decision seam is `wm.cascade-decision/cascade-decision` → `reading-family-decision` → `family-selection/select-over-families` (`wm/cascade_decision.clj:1218–1261,1363–1382`). The latter calls `score-family-field` (family_selection.clj:379), which prepares each policy and calls `cascade-shape-g/score-policy` (lines 304–327). `score-policy` calls `score-arranged` (cascade_shape_g.clj:548–573), which makes the progress scorer call at 325 and **the class scorer call at 341**. The class model currently hardcodes terminal 55/35/5/5, with unit not-yet-evaluated mass before the horizon (cascade_shape_g.clj:32–61). `cascade-selection` and `policy/select-action-cascades` subsequently compare/select; `decision-gate/emit!` admits the decision, rather than calculating this C.

There is a second, important route within this same family scorer: `:g-law :canonical-coapply` triggers `family-canonical-g/rescore` after initial scoring (family_selection.clj:380–383). It reconstructs class models at a common field horizon and calls `cascade-coapply-g/compare-at-horizon` (wm/family_canonical_g.clj:62–89); that calls the same observation scorer (cascade_coapply_g.clj:68). Both `compare-at-horizon` (132–136) and `family-canonical-g/certify-score` (127–155) require the existing ruled class kernel. A wiring patch must change those checks to verify the authorized kernel, not disable them or modify only the initial score that is subsequently replaced. Whether the current JVM opts enable this route was not probed.

What exists at score-arranged: candidate identity/precedence, pattern-derived universe and acceptance, initial belief `{#{} 1}`, horizon and step schedule, target-class context, model, predictions and consumed-C certificates (cascade_shape_g.clj:299–355). What is missing: production Q9 registry receipts, independent numeric authority identities, a validated calibration/kernel input, and a retained authorization-to-consumed-C join.

**`compose-and-score!` is not a drop-in serving call.** It takes mode, carrier, typed policy rows, candidates, state, universe, acceptance, horizon and schedule (factorized_preference_sut.clj:92–99). It rejects any mode other than `:controlled-sut` (99–100), rejects unmeasured outcomes (107–109), requires common semantics/horizon/schedule (110–122), and requires injective candidate targets (123–124). Ordinary reading families contain multiple policies for the same task and policy-specific horizons; passing the whole live family violates this SUT's assumptions. It also constructs synthetic provenance and a controlled prediction context (126–140). Reuse its validator/kernel composition, not a production call labelled controlled-SUT, and do not assert future completion merely to satisfy its policy-row fixture.

## 2. Existing authorizations and the decision Joe must make

**No production calibration or factorization authorization found** in canonical futon2 resources/data. Read-only `rg -uuu -l` included ignored files and extensionless stored evidence, searching `completion-first-softmax-v1`, `ordinal-relevance-cost-v1`, `focused-cost`, `focusedCost`, `calibration-authorizations`, `factorization-authorizations`: zero matches, exit 1, no search errors. Searching the carrier schema found 33 report-card renderings (11 runs × EDN/Markdown/HTML), not authorizations. Example `data/wm-runs/2026-10-15-b8f72b63-97c0-4289-948e-d7c370dd2ba4/report-card.md:19` explicitly says the authorized carrier/registries are absent. This is a bounded local search, not a claim about external notes or future writes while the machine runs.

`resources/wm/standing-authority/policy.edn:1–16` is `:wm/standing-recovered-medic-authority-policy-v1`. Its Joe authorization expressly covers exact retained medic bundles and says “No new approvals, no blanket identity trust.” It does **not** authorize preference costs or factorization.

Only controlled test authorizations were found: factorized_preference_sut_test.clj:6–23 uses calibration 101, authority 102, registration token 103, receipts 104/105, factorization identity 106/authority 107, selector 108, costs 0/1/2. Those are fixture choices, not Joe's production decision.

One operator decision must authorize and durably register:

- A specific natural-number cost triple with `focused < related < unrelated`, calibration identity/version, nonzero registration token, authority identity and receipt; the receipt must reproduce the calibration exactly and be authorized by that calibration authority. Joe can be that external authority or explicitly delegate it; the selector cannot authorize itself.
- The exact factorization implementation `:futon2/completion-first-softmax-v1`, version 1, numeric identity, nonzero external authority and receipt, law `:completion-then-conditional-relevance`, and implementation digest. Calibration implementation is `:futon2/ordinal-relevance-cost-v1`, likewise pinned. Both authorities must differ from the selector's recorded authority; they need not differ from each other.
- The selector's distinct identity, the production registry location/loader and identity encoding, and the scope of use (inner model; separately any outer model). No numeric IDs or costs should be minted here and presented as already authorized.

These are concrete validation conditions in factorized_preference_sut.clj:36–70 and mathlib4 `FactorizedPreferenceSemantics.lean:38–55,84–140`. Despite its identifier's “softmax”, the actual current kernel is **normalized reciprocal masses `1/(1+cost)`**, with stopped cost `unrelated+1`, not exponential softmax (factorized_preference_sut.clj:73–90). Authorization should identify these exact bytes/semantics, not rely on the name.

## 3. Record shape and the two runs

`wm_run_facts/factorized-preference-record` takes the first matching schema map at top-level `:factorized-preference`, `[:decision :factorized-preference]`, `[:decision :selection-certificate :factorized-preference]`, or `[:close :factorized-preference]` (scripts/wm_run_facts.clj:78–88). Expected carrier:

```clojure
{:schema :wm/factorized-preference-v1
 :certificate {:calibration {:identity I :version V :authority A :registration-token T
                             :focused-cost F :related-cost R :unrelated-cost U}
               :selector-authority S
               :factorization-identity FI :factorization-version 1
               :factorization-authority FA
               :calibration-authorization CR :factorization-authorization FR}
 :registries {:calibration [{:receipt CR :calibration <same-map> :authorized-by A}]
              :factorization [{:receipt FR :identity FI :version 1 :authority FA}]}}
```

The producer additionally needs implementation names/digests and factorization law to validate these receipts. Retain them even though the current exporter selects fewer fields. The checker currently renders the factorization law constructor literally (wm_check_run_facts.clj:267–283), so that rendering cannot substitute for validating the source receipt.

**Shape mismatch:** the SUT returns its input carrier unchanged, whose registries are top-level `:calibration-authorizations` and `:factorization-authorizations` (factorized_preference_sut.clj:36–42,146; test lines 13–23). The exporter reads `[:registries :calibration]` / `[:registries :factorization]` (wm_run_facts.clj:805–817). Merely attaching the SUT result's carrier would export a certificate with empty authorization registries. Normalize once at the producer boundary, or explicitly adapt the reader, with a test against the actual returned carrier.

For the tick, the safest existing retained location is `[:decision :selection-certificate :factorized-preference]`: `persist-run-record!` preserves selection-certificate through its decision whitelist (`full_loop_runner.clj:1514–1523`). An arbitrary new key on the transient decision is not automatically retained.

For selection, the exporter does copy Q9 fields from its local `work` projection (wm_run_facts.clj:1162–1165), but first resolves the immutable selection record, replaces the source with that record, and only restores the tick's `:decision` when exporting via a reference (1136–1150). Thus an SC carrier can currently appear on both exports from a tick; a standalone selection record cannot depend on the future tick, and a top-level tick-only carrier is lost on reference resolution. This copy is existing compatibility behavior, **not evidence that the outer model consumed the inner kernel**. The outer scorer uses the product of normalized ending/gain counts (outer_score.clj:173–179,231–233), a separate preference. A truthful selection witness belongs on its own decision record with its own consumed-kernel evidence. Sharing external authorization is possible only for semantics actually used by each scorer. Wiring the tick alone must not claim both serving Q9 obligations closed.

## 4. Existing test pins

- `test/futon2/aif/factorized_preference_sut_test.clj:41–51`: `authorized-kernel-reaches-real-scorer` calls the real composition function and checks certified terminal distribution against consumed C. It does not redefine the scorer. Further tests at 53–93 cover typed/unmeasured outcomes, authority substitution, forged flat tables, production-mode refusal, population and posterior-grain mismatch.
- `test/futon2/aif/cascade_efe_sut_test.clj:82–105`: positive end-to-end controlled composition checks actual posterior/preparation, exact G terms, node transition sequence and enacted order. `cascade_efe_sut.clj:107` calls the real preference SUT. Negative test at 144–153 wraps the real call then corrupts a scored node; at 165 it stubs `policy/select-action-cascades` specifically to test identity-divergence refusal. These negative stubs do not make the positive scorer a stub.
- These are controlled fixtures, not serving authorization or a real click witness. No tests were rerun in this report-only discovery packet.

## 5. Smallest honest wiring proposal (not implemented)

1. Add a production authorization loader at the composition root, with exact receipt/implementation pins and independently assigned authorities. Thread the validated carrier through the runtime options whitelist (`full_loop_runtime.clj:1071–1083`) into cascade decision/family scoring. Factor the SUT's validator and deterministic kernel construction into reusable functions while preserving the controlled wrapper and all negative checks. Do not change the wrapper to accept production mode with caller-supplied fake registries.
2. Supply the validated terminal distribution to `cascade-shape-g/class-emission-model` at the real class scorer; preserve the existing preterminal unmeasured schedule and per-policy identities/models. On canonical mode, thread it through `family-canonical-g/rescore` and its certification and `cascade-coapply-g` checks as well. Keep progress scoring, normalization and information terms unchanged unless a separately reviewed model change requires otherwise. Existing inner abstention is scored under that policy's model and must consume the same updated C.
3. Before `decision-gate/emit!`, verify the join from authorized kernel to every compared candidate's retained consumed C, then attach the normalized certificate and registries to SC. Existing persistence will retain it. Add a real producer→persisted-record→exporter→Lean witness; valid metadata alone is insufficient. If both runs are in scope, wire the outer preference law separately and retain its own carrier on the selection decision, rather than exploiting the cross-run copy.
4. Missing production configuration must refuse before posterior selection with `:production-registries-unconfigured`. Preserve existing `:certificate-invalid`, `:calibration-authorization-mismatch`, `:factorization-authorization-mismatch`, `:flat-table-not-a-certificate` and `:computed-kernel-not-consumed` semantics. Add explicitly named pin/read failures if needed (new names are proposals, not existing behavior). No fallback to 55/35/5/5 after a claimed factorized route fails.

This is a small authorization/model/carrier integration, not just an exporter field attachment. Missing production authorizations and the controlled-SUT grain assumptions are the concrete prerequisites; no authorization or implementation is inferred by this report.
