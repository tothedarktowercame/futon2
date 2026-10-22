(ns futon2.aif.wm.compiled-cascade-construction-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.wm.frozen-cascade-steps :as steps])
  (:import [java.nio.file Files]))

(def selected-path "holes/labs/wm-contract/runs/frozen-selection-step-2026-10-22/pass-1/selected-boundary.edn.gz")
(def compiled-path "test/fixtures/frozen-selection-step/recorded-compiled-426fa181.edn.gz")

(deftest real-reading-cascade-reaches-wiring-and-the-build-prompt
  (let [selected (steps/read-gzip selected-path)
        compiled (steps/read-gzip compiled-path)
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

(deftest restart-from-last-success-through-a-repaired-section
  (let [root (.toFile (Files/createTempDirectory "wm-frozen-sections-"
                       (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try
      (steps/initialize! selected-path compiled-path root)
      (doseq [stage [:materialized :compiled :constructed]]
        (is (= stage (:stage (steps/advance! root)))))
      (let [before (steps/history root)
            failed (with-redefs [runner/construction-wiring-result
                                 (fn [& _] (throw (ex-info "interrupted wiring" {})))]
                     (steps/advance! root))]
        (is (= :failed (:status failed)))
        (is (= before (steps/history root)) "failure does not replace the successful prefix")
        (is (= :wired (:stage (steps/advance! root))))
        (is (= before (subvec (steps/history root) 0 (count before))))
        (is (= :dispatch-prepared (:stage (steps/advance! root))))
        (let [complete (steps/history root)]
          (is (= :prepared-new-work (get-in (last complete) [:payload :scope])))
          (is (= 1 (count (get-in (last complete) [:payload :requests]))))
          (is (= "futon-theory/mission-lifecycle"
                 (get-in (last complete) [:payload :requests 0 :pattern])))
          (is (true? (:reused (steps/advance! root))))
          (is (= complete (steps/history root)))))
      (finally
        (doseq [file (reverse (file-seq (io/file root)))] (.delete file))))))
