(ns futon2.aif.machine-belief-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-belief :as mb]))

(def entity "arxana/stack/futon-v1/leaf/2/2")
(def row {:spawned 1/10 :refined 1/10 :strengthened 2/5 :addressed 1/10
          :falsified 1/10 :foreclosed 1/10 :reopened 1/10})
(def context {:entity/id entity :model {:id "wm" :revision "captured"}
              :state-support mb/state-support :mode :single-entity
              :policy-entities [entity]})

(deftest constructs-contract-belief-input-without-rewriting
  (let [result (mb/belief-state-distribution context {entity row})]
    (is (:ok result))
    (is (= {:mode :single-entity :posteriors {entity row}}
           (:belief-input result)))
    (is (identical? row (get-in result [:belief-input :posteriors entity])))
    (is (= (:model context) (:model result)))
    (is (= {:entity/id entity} (:context result)))
    (is (= mb/state-support (:state-support result)))))

(deftest commissioned-refusals
  (doseq [[label expected c b]
          [[:missing :missing-entity context {}]
           [:deleted :missing-entity context (dissoc {entity row} entity)]
           [:reordered :state-support-order-mismatch
            (update context :state-support #(vec (reverse %))) {entity row}]
           [:invalid-mass :invalid-mass context {entity (assoc row :spawned -1/10 :strengthened 3/5)}]
           [:unnormalised :invalid-mass context {entity (assoc row :spawned 1/5)}]
           [:unnormalised-float :invalid-mass context
            {entity (update (zipmap mb/state-support (repeat (/ 1.0 7.0)))
                            :spawned + 0.001)}]
           [:multi-entity :joint-construction-required
            (assoc context :policy-entities [entity "other"]) {entity row}]]]
    (testing (name label)
      (is (= expected (get-in (mb/belief-state-distribution c b)
                              [:refusal :kind]))))))
