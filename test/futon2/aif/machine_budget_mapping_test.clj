(ns futon2.aif.machine-budget-mapping-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-budget-mapping :as mapping]))

(def fixture-path
  "holes/labs/wm-contract/runs/row-22-e1-mapping-2026-09-13/production-shaped-candidate-field.edn")

(defn- fixture [] (edn/read-string (slurp (io/file fixture-path))))

(def fixture-binding
  {:model/id :wm-test-model :model/revision "fixture-revision-1"
   :run/id "commissioned-e1-run" :tick/index 7})

(def pin-a "b19ab31679a5667a2b6e4ae4afc2f9d7277cb9ce9bb4574b8f03227075022366")
(def pin-b "77586955c82d217eda2a822107b160ee0c4bde2a13e0d561e5dcc124f06649ee")
(def pin-c "5dedbcabeeda0e61e3da708fce7f51e184e24da516b98b8ab795e0cb7c71fb72")
(def pin-d "2ec01de0d4fcf16713af34cb7192ab3fcfdd68f25fb9b54cd17ba07d1efab324")

(defn- declared [id path sha values]
  {:authority :declared-source
   :source/id id :source/revision "commissioned-fixture-v1"
   :artifact/path path :artifact/sha256 sha
   :binding fixture-binding :values values})

(defn- complete-input []
  (let [{:keys [ranked-support]} (fixture)
        ids (mapv :candidate/id ranked-support)]
    (merge {:schema/version mapping/schema-version
            :scope :commissioned-production-shaped-fixture
            :ranked-support ranked-support
            :authorities
            {:field-membership
             (declared :commissioned-field-assignment "machinery-capture.edn" pin-a
                       {(ids 0) :mission-field (ids 1) :mission-field
                        (ids 2) :idle-field})
             :costs
             (declared :commissioned-cost-table "controller-head.edn" pin-b
                       {(ids 0) 4 (ids 1) 2 (ids 2) 1})
             :utilities
             (declared :commissioned-utility-table "full-score-first-max.edn" pin-c
                       {(ids 0) 8 (ids 1) 7 (ids 2) 1})
             :budgets
             (declared :commissioned-budget-table "first-max-tie-control.edn" pin-d
                       {:root 5 :fields {:mission-field 5 :idle-field 1}})}}
           fixture-binding)))

(defn- refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest full-mapping-preserves-occurrences-actions-ranks-and-accounting
  (let [input (complete-input)
        out (mapping/map-ranked-support input)
        support (:ranked-support input)]
    (is (= 3 (count (:accounting out))))
    (is (:complete-accounting? out))
    (is (= (mapv :candidate/id support) (mapv :candidate/id (:bijection out))))
    (is (= (mapv :rank support) (mapv :rank (:bijection out))))
    (is (= (mapv :action support) (mapv :action (:bijection out))))
    (is (= (:action (support 0)) (:action (support 1))))
    (is (not= (:candidate/id (support 0)) (:candidate/id (support 1))))
    (is (= #{:selected :rejected} (set (map :disposition (:accounting out)))))
    (is (= #{[:commissioned-e1-run 7 0] [:commissioned-e1-run 7 2]}
           (get-in out [:response :selected-ids])))
    (is (get-in out [:response :within-all-budgets?]))))

(deftest exact-replay-and-action-mismatch-detection
  (let [out (mapping/map-ranked-support (complete-input))
        receipt (:replay/receipt out)]
    (is (:replay/identical? (mapping/replay receipt)))
    (is (false? (:replay/identical?
                 (mapping/replay
                  (assoc-in receipt [:output :accounting 0 :action :target] "wrong")))))))

(deftest current-production-shaped-data-is-a-typed-authority-absence
  (let [f (fixture)]
    (is (= :typed-absence (get-in f [:production-authority-status :status])))
    (is (= :r6-r11/authority-missing
           (refusal #(mapping/map-ranked-support
                      (merge {:schema/version mapping/schema-version
                              :scope (:scope f)
                              :ranked-support (:ranked-support f)
                              :authorities {}}
                             (:identity f))))))))

(deftest commissioned-negative-controls-refuse
  (testing "dropped occurrence leaves authority coverage unequal"
    (is (= :r6-r11/support-mismatch
           (refusal #(mapping/map-ranked-support
                      (update (complete-input) :ranked-support pop))))))
  (testing "reordering without changing occurrence ranks is detected"
    (is (= :r6-r11/support-reordered
           (refusal #(mapping/map-ranked-support
                      (update (complete-input) :ranked-support
                              (fn [[a b c]] [b a c])))))))
  (testing "duplicate occurrence identity is not semantic-action deduplication"
    (is (= :r6-r11/duplicate-occurrence-id
           (refusal #(mapping/map-ranked-support
                      (assoc-in (complete-input) [:ranked-support 1 :candidate/id]
                                [:commissioned-e1-run 7 0]))))))
  (testing "unattributed cost refuses"
    (is (= :r6-r11/support-mismatch
           (refusal #(mapping/map-ranked-support
                      (update-in (complete-input) [:authorities :costs :values]
                                 dissoc [:commissioned-e1-run 7 1]))))))
  (testing "cross-run authority refuses"
    (is (= :r6-r11/cross-run-authority
           (refusal #(mapping/map-ranked-support
                      (assoc-in (complete-input)
                                [:authorities :utilities :binding :run/id]
                                "other-run"))))))
  (testing "unknown field budget refuses"
    (is (= :r6-r11/unknown-budget
           (refusal #(mapping/map-ranked-support
                      (update-in (complete-input) [:authorities :budgets :values :fields]
                                 dissoc :idle-field))))))
  (testing "posterior mass cannot substitute for utility"
    (is (= :r6-r11/posterior-substitution
           (refusal #(mapping/map-ranked-support
                      (assoc (complete-input) :Q-pi [0.8 0.1 0.1])))))))

(deftest supported-transformation-requires-pinned-inputs
  (is (= :r6-r11/transformation-unsupported
         (refusal #(mapping/map-ranked-support
                    (-> (complete-input)
                        (assoc-in [:authorities :costs :authority]
                                  :supported-transformation)
                        (assoc-in [:authorities :costs :transformation]
                                  {:id :cost-law :revision "v1" :input-pins []})))))))
