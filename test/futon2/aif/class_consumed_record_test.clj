(ns futon2.aif.class-consumed-record-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.cascade-observation-scoring :as scoring]
            [futon2.aif.g-term-decomposition :as decomposition]
            [futon2.aif.token-belief-carry-test :as fixture]))

(def pre-change-score-digest
  ;; Captured from the real fixture at futon2
  ;; 0eecab4ac34a45cc94297f55fb8d7db8c7c7e019, before :consumed-g was added.
  "cbb149b7788171ebe4f8d83c7e0ba60dc7c373001b61b57f7526ec179f886138")

(defn ranked-fixture []
  (let [ranked (atom nil)
        score scoring/rank-cascade-actions]
    (with-redefs [scoring/rank-cascade-actions
                  (fn [state candidates opts]
                    (let [result (score state candidates opts)]
                      (reset! ranked result)
                      result))]
      (fixture/decision nil))
    @ranked))

(defn score-projection [ranked]
  (mapv #(select-keys % [:cascade-id :controller-score :rank :posterior]) ranked))

(deftest class-score-records-consumed-a-c-and-d
  (let [ranked (ranked-fixture)
        candidates (mapv :action ranked)
        census (decomposition/census ranked candidates)]
    (is (= pre-change-score-digest (identity/digest (score-projection ranked))))
    (doseq [entry ranked]
      (let [certificate (:certificate entry)
            consumed (:consumed-g certificate)
            model (:observation-model certificate)]
        (is (= (get-in entry [:prediction :initial-belief]) (:D consumed)))
        (is (= (mapv :tau (:steps certificate))
               (mapv :tau (get-in consumed [:C :steps]))))
        (doseq [{:keys [tau distribution]} (get-in consumed [:C :steps])]
          (is (= (get-in model [:class-preference tau]) distribution)))
        (is (identical? model (:A consumed)))
        (is (not (contains? consumed :Q)))))
    (doseq [policy (:policies census)]
      (is (= :present (get-in policy [:terms :A :status])))
      (is (= :deterministic-class-emission
             (get-in policy [:terms :A :reason])))
      (is (= :present (get-in policy [:terms :C :status])))
      (is (= :present (get-in policy [:terms :D :status])))
      (is (= {:status :missing :value nil :reason :consumed-value-not-recorded}
             (get-in policy [:terms :Q]))))))

(deftest live-selection-completes-with-class-a-in-its-census
  (let [decision (fixture/decision nil)
        policies (get-in decision [:selection-certificate :g-term-decomposition :policies])]
    (is (seq policies))
    (doseq [policy policies]
      (is (= :present (get-in policy [:terms :A :status])))
      (is (= :deterministic-class-emission
             (get-in policy [:terms :A :reason]))))))

(deftest census-does-not-infer-deleted-d-from-prediction
  (let [entry (first (ranked-fixture))
        without-d (update-in entry [:certificate :consumed-g] dissoc :D)
        census (decomposition/census [without-d] [(:action without-d)])]
    (is (some? (get-in without-d [:prediction :initial-belief])))
    (is (= {:status :missing :value nil :reason :consumed-value-not-recorded}
           (get-in census [:policies 0 :terms :D])))))
