(ns futon2.vm.tick-001-s04-r13-test
  "Tick 1, step 4, R13 (policy depth): the real tick must expose ONE common
  declared horizon T for the whole candidate family (C0-empty, C1-test-first,
  C2-fix-first, C3-fix-only of vm/tick-001/03-R6.edn).

  Model basis:
  - SPEC-cascade-policy-semantics §2: T is a common declared prediction
    horizon for the candidate family, not each candidate's pattern count.
  - mathlib4 DarkTower/WarMachine/PolicyHorizon.lean: one horizon; candidates
    are compared only at a common declared T (usage rule).

  This tick's declaration is T = 3 (zai-36 judgement: under P10 one pattern
  acts per step, and the family's only order-sensitive completion — C2's
  third step, which establishes :test-covers-missing-total-repos — is
  invisible at T = 2; the horizon must distinguish establishing the full
  want from a partial fix). The pending AUTH-horizon-semantics proposal
  (T = 2) is NOT approved by Joe and is recorded as a typed missing input,
  not adopted.

  These are requirement tests: they assert what the model requires of the
  real tick and fail where the real tick falls short. R13 has no route tag
  in war_machine.clj today, so the routing assertion fails until wired."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.policy-depth :as policy-depth]))

(defn- war-machine-source
  "The real tick's source, from the classpath or the repo checkout."
  []
  (or (some-> (io/resource "futon2/report/war_machine.clj") slurp)
      (try
        (slurp "scripts/futon2/report/war_machine.clj")
        (catch Exception _ nil))))

(deftest s04-r13
  ;; 1. The declared common horizon is readable as a typed value: T = 3,
  ;; declared once for the whole family, read through :cascade-rollout.
  (is (= {:anticipation 3 :cascade-rollout 3}
         (policy-depth/configured {:policy-depth {:anticipation 3 :cascade-rollout 3}}))
      "configured returns the declared common horizon T = 3 as a typed value")
  ;; 2. No implicit default: a horizon the operator did not declare must not
  ;;    be invented (policy-depth docstring: no implicit default changes).
  (is (nil? (policy-depth/validate nil))
      "no implicit horizon default when nothing is declared")
  (is (thrown? clojure.lang.ExceptionInfo (policy-depth/validate {}))
      "an empty declaration is a refusal, not a silently declared horizon")
  ;; 3. Malformed declarations are refused, not coerced to a default T.
  (is (thrown? clojure.lang.ExceptionInfo
               (policy-depth/validate {:anticipation 0 :cascade-rollout 3}))
      "a non-positive horizon is a refusal, not a coerced value")
  ;; 4. The real tick wires R13. war_machine.clj calls
  ;;    policy-depth/configured (~:6024) but tags no :R13 route; the model
  ;;    includes R13 as the node that sets the common horizon, so this fails
  ;;    until the node is wired into the tick's route.
  (let [src (war-machine-source)]
    (is (some? src)
        "war_machine.clj is readable by the test")
    (is (boolean (and src (str/includes? src "policy-depth/configured")))
        "the tick reads its depth through futon2.aif.policy-depth/configured")
    (is (boolean (and src
                      (re-find #":R13\s+\"futon2\.aif\.policy-depth/configured\"" src)))
        "war_machine.clj routes the depth read with an :R13 route tag naming futon2.aif.policy-depth/configured")))
