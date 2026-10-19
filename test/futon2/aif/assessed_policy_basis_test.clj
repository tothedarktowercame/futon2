(ns futon2.aif.assessed-policy-basis-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.assessed-policy-basis :as basis]
            [futon2.aif.outer-policy-evidence :as evidence]
            [futon2.aif.outer-policy-evidence-test :as fixture]
            [futon2.aif.outer-policy-objective :as objective]
            [futon2.aif.wm.executable-cascade :as xc]
            [futon2.aif.wm.family-selection :as family]))

(deftest producer-basis-recomputes-and-rejects-repinned-substitution
  ;; Synthetic canonical fixtures only: join, objective, identity binding and
  ;; basis verification all run their real implementations.
  (with-redefs [xc/digest :digest
                xc/validate-cascade (constantly {:status :certified})]
    (let [body (update fixture/sealed-body :entries conj
                       {:policy-id "p3" :target-id "T" :cascade-digest "d3"
                        :canonical-cascade {:digest "d3"}})
          menu (assoc body :status :sealed :menu-pin (family/sealed-menu-pin body))
          spec (fixture/spec [1.0 1.0])
          ev (evidence/join menu (fixture/declaration {"d1" spec "d2" spec}) [])
          receipt (objective/evaluate ev menu 0.99)
          carrier (basis/seal menu ev receipt)
          repin #(assoc % :carrier-pin (basis/carrier-pin %))]
      (is (= :sealed (:status carrier)))
      (is (nil? (basis/verify carrier)))
      (is (= menu (:menu carrier)))
      (is (= "d2" (get-in carrier [:receipt :assessed-selection :action :cascade-digest])))
      (is (= ["d3"] (mapv :cascade-digest (get-in carrier [:receipt :assessed-selection :absent]))))
      (is (= receipt (objective/evaluate (:evidence carrier) (:menu carrier)
                                         (get-in carrier [:receipt :assessed-selection :draw :u]))))
      (doseq [change [#(assoc % :scope :full-menu)
                      #(assoc-in % [:receipt :assessed-selection :action :cascade-digest] "d3")
                      #(assoc-in % [:receipt :assessed-selection :scope] :full-menu)
                      #(update-in % [:receipt :assessed-selection :assessed-ids] pop)
                      #(assoc-in % [:receipt :assessed-selection :draw :u] 0.0)
                      #(assoc-in % [:evidence :evidence-pin :sha256] "changed")]]
        (is (keyword? (basis/verify (repin (change carrier))))))
      (let [empty-ev (evidence/join menu (fixture/declaration {}) [])]
        (is (= :basis-not-selected
               (:reason (basis/seal menu empty-ev (objective/evaluate empty-ev menu))))))
      (is (= :basis-schema (basis/verify 42))))))
