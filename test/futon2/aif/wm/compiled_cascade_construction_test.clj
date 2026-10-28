(ns futon2.aif.wm.compiled-cascade-construction-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [clojure.edn :as edn])
  (:import [java.util.zip GZIPInputStream]))

(defn- read-gzip [path]
  (with-open [in (GZIPInputStream. (io/input-stream path))]
    (edn/read-string (slurp in))))

(def selected-path "holes/labs/wm-contract/runs/frozen-selection-step-2026-10-22/pass-1/selected-boundary.edn.gz")
(def compiled-path "test/fixtures/frozen-selection-step/recorded-compiled-426fa181.edn.gz")

(deftest real-reading-cascade-reaches-wiring-and-the-build-prompt
  (let [selected (read-gzip selected-path)
        compiled (read-gzip compiled-path)
        entry (get-in selected [:selected-outer-cascade-policy :selected-entry])
        construction (runner/construct-for-decision (assoc entry :compiled-cascade compiled))
        wiring (runner/construction-wiring-result construction)
        prompt (#'runner/prompt-construction construction)]
    (is (= :wired (:status wiring)))
    (is (= ["futon-theory/mission-lifecycle"] (:shown construction)))
    (is (= 1 (count (get-in wiring [:wiring :boxes]))))
    (is (empty? (get-in wiring [:fold-output :policy-holes])))
    (is (= :declare-mission-lifecycle-state
           (get-in prompt [:operational-plan :steps 0 :clause :operation])))
    (is (= {:author "zai-2" :reviewer "zai-3" :authority :wm/reviewed-resume}
           (get-in prompt [:operational-plan :steps 0 :clause :authoring/attribution])))
    (is (= :unchecked (get-in wiring [:wiring :boxes 0 :conditions 0 :status])))
    (is (= :not-executed (get-in prompt [:operational-plan :effect-status])))
    (is (= (:action entry) (:selected-action construction)))
    (is (thrown? clojure.lang.ExceptionInfo
                 (runner/construct-for-decision
                  (assoc entry :compiled-cascade (assoc compiled :digest "wrong")))))))
