(ns futon2.aif.selected-outer-assessed-policy-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.assessed-policy-basis :as assessed-basis]
            [futon2.aif.outer-policy-evidence :as evidence]
            [futon2.aif.outer-policy-evidence-test :as fixture]
            [futon2.aif.outer-policy-objective :as objective]
            [futon2.aif.selected-outer-cascade-policy :as selected]
            [futon2.aif.wm.executable-cascade :as xc]
            [futon2.aif.wm.family-selection :as family]))

(defn partial-inputs []
  (let [executable-action (fn [policy-id]
                            {:type :execute-reading-cascade
                             :target "T"
                             :entry-policy policy-id
                             :precedence {:co-apply {:policy policy-id}}})
        body (update fixture/sealed-body :entries
                     (fn [entries]
                       (conj (mapv #(assoc % :action (executable-action (:policy-id %))) entries)
                             {:policy-id "p3" :target-id "T" :cascade-digest "d3"
                              :canonical-cascade {:digest "d3"}
                              :action (executable-action "p3")})))
        menu (assoc body :status :sealed :menu-pin (family/sealed-menu-pin body))
        spec (fixture/spec [1.0 1.0])
        ev (evidence/join menu (fixture/declaration {"d1" spec "d2" spec}) [])
        receipt (objective/evaluate ev menu 0.99)]
    {:menu menu :evidence ev :receipt receipt}))

(defn repin [carrier]
  (assoc carrier :carrier-pin (selected/carrier-pin carrier)))

(deftest assessed-basis-becomes-verified-executable-identity-transport
  ;; P05 join and P06 objective/basis are real.  The two stubs only make the
  ;; synthetic fixture's canonical cascade satisfy its documented port.
  (with-redefs [xc/digest :digest
                xc/validate-cascade (constantly {:status :certified})]
    (let [{:keys [menu evidence receipt]} (partial-inputs)
          basis (assessed-basis/seal menu evidence receipt)
          carrier (selected/seal-assessed basis)
          projected (selected/decision carrier)
          basis-certificate (get-in projected [:decision :selection-certificate :selection-basis])]
      (is (= :sealed (:status carrier)))
      (is (nil? (selected/verify carrier)))
      (is (= menu (:sealed-menu carrier)))
      (is (= evidence (:evidence-carrier carrier)))
      (is (= (:action (:selected-entry carrier))
             (get-in projected [:decision :action])))
      (is (not= (:action (:assessed-selection receipt))
                (get-in projected [:decision :action])))
      (is (= :assessed-population (:kind basis-certificate)))
      (is (= ["d3"] (mapv :cascade-digest (:absent basis-certificate))))
      (is (= 0.99 (get-in basis-certificate [:draw :u])))
      (doseq [change [#(assoc % :basis-kind :strict-objective)
                      #(assoc % :cascade-digest "d3")
                      #(assoc-in % [:selected-entry :canonical-cascade :digest] "d3")
                      #(assoc-in % [:selected-entry :action :entry-policy] "forged")
                      #(assoc % :scope :full-menu)
                      #(assoc-in % [:assessed-basis :scope] :full-menu)
                      #(assoc-in % [:assessed-basis :receipt :assessed-selection :assessed-ids] [])]]
        (is (keyword? (selected/verify (repin (change carrier))))))
      (let [empty-ev (evidence/join menu (fixture/declaration {}) [])
            empty-basis (assessed-basis/seal menu empty-ev
                                             (objective/evaluate empty-ev menu))]
        (is (= :refused (:status (selected/seal-assessed empty-basis))))))))

(deftest strict-carriers-remain-compatible
  (with-redefs [xc/digest :digest
                xc/validate-cascade (constantly {:status :certified})]
    (let [{:keys [menu]} (partial-inputs)
          spec (fixture/spec [1.0 1.0])
          ev (evidence/join menu (fixture/declaration {"d1" spec "d2" spec "d3" spec}) [])
          strict (selected/seal menu ev (objective/evaluate ev menu 0.99))]
      (is (= :sealed (:status strict)))
      (is (nil? (selected/verify strict)))
      (is (= :strict-objective
             (get-in (selected/decision strict)
                     [:decision :selection-certificate :selection-basis :kind])))
      (let [legacy (-> strict
                       (dissoc :basis-kind)
                       repin)]
        (is (nil? (selected/verify legacy)))))))
