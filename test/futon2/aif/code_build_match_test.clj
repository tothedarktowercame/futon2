(ns futon2.aif.code-build-match-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.code-build-match :as cbm]))

(defn- by-box [result]
  (into {} (map (juxt :box identity)) (:boxes result)))

(defn- sorry-doc [& sorrys]
  {:schema-version 2
   :sorrys (vec sorrys)})

(deftest aif-grounded-loop-dashboard-is-derived-test
  (let [result (cbm/aif-grounded-loop-match)
        boxes (by-box result)]
    (is (:clean-pass? result))
    (is (= {:ready 2 :total 7} (:dial result)))
    (is (true? (get-in boxes [:b3 :ready?])) "R5 EFE scorecard is ready")
    (is (true? (get-in boxes [:b5 :ready?])) "R16 actuator/build-match is ready")
    (is (false? (get-in boxes [:b1 :ready?])))
    (is (false? (get-in boxes [:b2 :ready?])))
    (is (true? (get-in boxes [:b2 :faithfulness-gap]))
        "R4 is shape-only and has no open gating sorry, so it must not read green")
    (is (false? (get-in boxes [:b4 :ready?])))
    (is (false? (get-in boxes [:b6 :ready?])))
    (is (false? (get-in boxes [:b7 :ready?])))
    (is (false? (get-in boxes [:b7 :code-present?])) "R12 accumulate has no resolved key var")
    (is (= [:b2] (:faithfulness-gaps result)))))

(deftest sorry-discharge-flips-readiness-without-checker-edit-test
  (let [clean (assoc (cbm/load-clean)
                     :clean/boxes [{:id :b3
                                    :method :evaluate
                                    :text "R5 fixture"
                                    :produces :efe-scored-candidates}]
                     :clean/seq [:evaluate]
                     :clean/wires []
                     :clean/shape {:macro :fixture
                                   :holes-at []
                                   :discharges-at []})
        bindings {:b3 {:code-ns ['futon2.aif.efe]
                       :code-var 'futon2.aif.efe/compute-efe
                       :gating-sorries [:sorry/test-stage]}}
        open-result (cbm/code-box-match
                     clean bindings
                     {:sorry-doc (sorry-doc {:id :sorry/test-stage
                                             :status :open})})
        closed-result (cbm/code-box-match
                       clean bindings
                       {:sorry-doc (sorry-doc {:id :sorry/test-stage
                                               :status :addressed})})]
    (is (= {:ready 0 :total 1} (:dial open-result)))
    (is (= [:sorry/test-stage]
           (get-in (by-box open-result) [:b3 :open-sorries])))
    (is (= {:ready 1 :total 1} (:dial closed-result)))
    (is (true? (get-in (by-box closed-result) [:b3 :ready?])))))

(deftest faithfulness-gap-blocks-silent-green-test
  (let [clean (assoc (cbm/load-clean)
                     :clean/boxes [{:id :b2
                                    :method :predict
                                    :text "R4 fixture"
                                    :produces :predicted-observation}]
                     :clean/seq [:predict]
                     :clean/wires []
                     :clean/shape {:macro :fixture
                                   :holes-at []
                                   :discharges-at []})
        result (cbm/code-box-match
                clean
                {:b2 {:code-ns ['futon2.aif.forward-model]
                      :code-var 'futon2.aif.forward-model/predict
                      :gating-sorries []
                      :known-not-ready? true}})]
    (is (= {:ready 0 :total 1} (:dial result)))
    (is (true? (get-in (by-box result) [:b2 :code-present?])))
    (is (true? (get-in (by-box result) [:b2 :sorry-clear?])))
    (is (true? (get-in (by-box result) [:b2 :faithfulness-gap])))
    (is (false? (get-in (by-box result) [:b2 :ready?])))))

(deftest malformed-clean-refuses-before-readiness-test
  (testing "G5 port mismatch is reported by the canonical CLean checker"
    (let [bad-clean (update-in (cbm/load-clean)
                               [:clean/wires 0]
                               assoc :carries :wrong-port)
          result (cbm/code-box-match bad-clean cbm/reviewed-code-bindings)]
      (is (false? (:clean-pass? result)))
      (is (true? (:refused? result)))
      (is (= :clean-invalid (:reason result)))
      (is (some #(re-find #"G5" %) (:clean-errors result))))))
