(ns futon2.aif.efe-cascade-spec-receipt-test
  "The ranker records its received spec separately from the derived scoring spec."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.efe :as efe]
            [futon2.aif.efe-certificate-test :as cert]))

(defn rank [mutation]
  (let [{:keys [q0 spec candidates]} (#'cert/fixture)
        spec (assoc spec :preference-scales {:authority :fixture})]
    (efe/rank-cascade-actions {:cascade-belief q0} candidates
                             (mutation {:cascade-spec spec :horizon-steps 3}))))

(deftest input-spec-is-retained-before-transformation
  (let [r (rank identity)
        receipt (:cascade-scoring (meta r))]
    (is (vector? r))
    (is (= {:authority :fixture} (get-in receipt [:spec-in :preference-scales])))
    (is (not (contains? (:spec receipt) :preference-scales)))
    (is (= (:want (:spec-in receipt)) (:want (:spec receipt))))))

(deftest refusal-retains-the-input-without-defaulting
  (doseq [[mutation expected] [[#(dissoc % :cascade-spec) {:absent :no-cascade-spec}]
                               [#(assoc % :cascade-spec {:absent :no-cascade-spec})
                                {:absent :no-cascade-spec}]
                               [#(assoc % :cascade-spec {}) {}]
                               [#(assoc % :cascade-spec nil) nil]]]
    (let [r (rank mutation)]
      (is (= :missing-cascade-want (:kind r)))
      (is (= expected (get-in (meta r) [:cascade-scoring :spec-in]))))))
