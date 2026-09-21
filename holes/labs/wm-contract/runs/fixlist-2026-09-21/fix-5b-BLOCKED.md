# Fix-5b: blocked at the shared G context, before production wiring

2026-09-21, codex-12. Branch `fix/narrative-5b`, base
`38f55e9da00b3b5e3a03306061efb269e1013119` (merged fix-5a).

**Fix-5b is not implemented.** No production or test files were changed.
This note reports an invariant conflict in implementing it as an assembly hook,
with an executable counterexample and a proposed structural prerequisite.
It is not an assertion that machine construction on the serving path is impossible.

## Conflict

The requested invariant is: construction and selection use identical G.
Calling `efe/rank-actions` independently for each target does not establish it.

At this base:

* `scripts/futon2/report/war_machine.clj:6157` initializes the joint belief from
  the admitted problems and applies the token carry/predecessor input path.
* At line 6182, the joint reachable domain comes from the joint candidates,
  joint want and joint belief. At line 6195, it determines the live-C projection.
* At line 6219, selection calls `efe/rank-actions` once over the joint family.
* `src/futon2/aif/efe.clj:1088` derives one common universe from that entire
  family's pattern tokens, q0 support, and wants. At line 1149 it passes that
  universe to `horizon-g-sparse-cert` for every candidate.
* The requested insertion point (war_machine.clj:6727–6753) precedes assembly
  and admission. Some targets do not become problems until construction supplies
  their first candidate; rejected targets do not contribute a joint belief or C
  projection to the eventual selection.

Fix-5a deliberately accepts an injected evaluator. Its current `construct`
function generates prospective orders internally and asks the evaluator to
compare the empty family with the proposed family before emitting receipted
candidates. Thus the source hook cannot obtain the final selection context just
by passing an existing function name. The context depends on which constructed
candidates survive, and that depends on construction's G comparison.

This matters even without live-C or predecessor changes. The exact same public
scorer, candidate, initial belief, preferences and T=2 produce:

```edn
{:single-target-G 3.012817736156336
 :joint-family-G 4.399112097276227
 :difference 1.3862943611198908
 :same-action-state-preference-horizon-and-scorer true
 :only-change :other-candidate-introduces-one-token}
```

The difference is 2 ln 2. This example establishes unequal absolute G, not a
claim that the winning order flips in this particular example. With live-C
projection and admitted-target belief changes, those inputs must also be shared;
it would be unsound to generalize this example's common-offset property.

The workspace instruction applies: “When invariants block progress, the correct
action is to stop, surface the conflict, and propose tests or structural changes
— never to route around them.” In particular, I did not:

* substitute per-target G or `active-horizon-g` for the serving joint scorer;
* score against only today's hand-declared family, then claim equality after
  adding new candidate tokens;
* pad the scorer with fake candidates to make its universe large enough;
* attach fake construction receipts to proposals to get them through assembly;
* invent a new fixed preference/domain authority or silently weaken equality to
  equality up to an additive constant.

## Structural prerequisite proposed for review

1. Split fix-5a's **prospective order preparation** from **construction-policy
   evaluation and receipt emission**. Preserve the current `construct` API as
   their composition. Preparation yields plans and findings, never an admitted
   candidate or successful construction receipt.
2. Extract the serving joint G-context preparation into one reusable function:
   target qualification, admitted problem facts, predecessor consumption,
   common horizon, live-C derivation/freshness/projection, rates and score options.
   Use the same extracted function for construction and final selection. Do not
   replace the existing `horizon-g-sparse-cert` implementation.
3. Specify the treatment of prospective targets that fail construction. To
   meet the requested equality to **final selection**, evaluate against a
   candidate context, remove refused prospective candidates/targets, and
   reconcile until the retained construction decisions use the final context.
   This needs an explicit bounded reconciliation policy (including whether
   dropped candidates may be reconsidered and what happens on instability),
   with a typed nonconvergence result. Those choices belong in a declared
   policy, not hidden literals in the source hook. Keeping rejected proposals
   in selection's universe instead would change the current scoring semantics.
4. Then add the requested declaration/config loader, hand-precedence-wins merge
   with both receipts, constructor outcomes in `:cascade-problems`, and receipt
   preservation into selection. Admission stays unchanged.

The first additional test must compare the constructor's recorded G with the
final selection certificate for a two-target family where the second target
introduces a token. A second arm removes that target by a real construction
refusal and checks equality again. Also exercise a target with no hand-declared
candidate and a nontrivial live-C projection. The single-target P/Q/R fixture
alone cannot distinguish per-target scoring from serving-family scoring.

This structural work can precede the hook as a separate reviewed slice, or be
made an explicit part of fix-5b. I have not chosen a reconciliation policy on the
owner's behalf. The current branch contains only this finding.

## Real declarations: observed constructor outcomes

Read `resources/wm/cascade-sources` from this worktree with the real
`cascade-sources/load-declared`, which uses C3–C6 observations. Passed its exact
`:universes` values to the real constructor. No unknown was converted to false.
The following is a **direct diagnostic probe**, not the requested hermetic
serving-assembly acceptance test. Git observations read the canonical repos;
there was no runner invocation, live reload, click, dispatch, or store write.

Diagnostic parameters were explicitly supplied in the probe: max-moves 1,
max-expansions 64, horizon 2, move-cost 0. These are not claimed to be production
declarations. The evaluator deliberately throws `:scorer-needed` if called,
so the probe cannot pretend to supply joint serving G. No target reached it.

| Target | Constructor result | Finding |
|---|---|---|
| M-aif-policy-conditioned-eig | `:no-supported-order` | `:unproduced-need`, `:hole/h0e270aa090bc` |
| M-expressions-of-interest | `:observation-required` | all three effect tokens unknown; missing locators remain missing |
| M-f11-find-production-successor | `:no-supported-order` | `:unproduced-need`, `:hole/h2045faa0e7cc` |
| M-wm-08-external-f2 | `:want-already-observed` | current C3 observation already establishes the want |

Worktree `data/` file sets matched before/after: **106 → 106**. This is a file
count/set check, not a content digest check. Both probes exited 0. The real
serving path still has no call to the constructor at this base; no passing
serving acceptance test or new test warrant is claimed.

## Reproduce

From `/home/joe/code/futon2-fix-5b`, save the next block as
`/tmp/fix5b-g-context.clj`, then run `clojure -M /tmp/fix5b-g-context.clj`.
The assertions use the real scorer. Clojure is 1.11.1 from this repo's deps;
JVM is OpenJDK 21.0.11. No external data is needed for this counterexample.

```clojure
(require '[futon2.aif.efe :as efe]
         '[futon2.aif.cascade-policy :as cp])
(let [p (cp/token-interpretation :P {:guard {:needs #{} :forbids #{}} :produces #{[:A :q]}})
      q (cp/token-interpretation :Q {:guard {:needs #{[:A :q]} :forbids #{}} :produces #{[:A :w]}})
      r (cp/token-interpretation :R {:guard {:needs #{} :forbids #{}} :produces #{[:B :r]}})
      a {:kind :cascade-candidate :id :A :target :A :precedence [p q]}
      b {:kind :cascade-candidate :id :B :target :B :precedence [r]}
      state {:cascade-belief {#{} 1}}
      opts {:f-prefix-production? true :horizon-steps 2
            :cascade-spec {:want #{[:A :w]} :lam 1 :mu 0}}
      alone (efe/rank-actions state [a] opts)
      joint (efe/rank-actions state [a b] opts)
      ga (:G-efe (first alone))
      gj (:G-efe (first (filter #(= :A (get-in % [:action :id])) joint)))]
  (assert (number? ga))
  (assert (number? gj))
  (assert (> (Math/abs (- ga gj)) 0.1))
  (prn {:single-target-G ga :joint-family-G gj :difference (- gj ga)
        :same-action-state-preference-horizon-and-scorer true
        :only-change :other-candidate-introduces-one-token}))
(shutdown-agents)
```

Save this block as `/tmp/fix5b-declarations.clj`, then run
`clojure -M /tmp/fix5b-declarations.clj`. Canonical Git HEAD observations can
change, so the table is the result at inspection time, not a historical replay.

```clojure
(require '[futon2.aif.cascade-sources :as sources]
         '[futon2.aif.interpretation-construction :as construction]
         '[clojure.java.io :as io])
(defn files [root]
  (set (map str (filter #(.isFile %) (file-seq (io/file root))))))
(let [before (files "data")
      declared (sources/load-declared "resources/wm/cascade-sources")]
  (doseq [target (sort (keys (:interpretations declared)))]
    (let [r (try
              (construction/construct
               {:target target :want (get-in declared [:wants target])
                :observation (get-in declared [:universes target])
                :interpretations (get-in declared [:interpretations target :patterns])
                :interpretation-receipts (get-in declared [:interpretations target :receipts])
                :budget {:max-moves 1 :max-expansions 64} :horizon 2 :move-cost 0
                :evaluate-g (fn [_] (throw (ex-info "Joint serving context required" {:probe :scorer-needed})))})
              (catch clojure.lang.ExceptionInfo e
                (if (= :scorer-needed (:probe (ex-data e)))
                  {:status :probe-stopped :kind :scorer-needed}
                  (throw e))))]
      (prn {:target target :status (:status r) :kind (:kind r)
            :tokens (:tokens r) :findings (:findings r)})))
  (let [after (files "data")]
    (assert (= before after))
    (prn {:store-files-before (count before) :store-files-after (count after)})))
(shutdown-agents)
```
