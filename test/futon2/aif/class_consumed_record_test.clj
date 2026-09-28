(ns futon2.aif.class-consumed-record-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.cascade-observation-scoring :as scoring]
            [futon2.aif.g-term-decomposition :as decomposition]
            [futon2.aif.token-belief-carry-test :as fixture]
            [futon2.aif.token-observation-initialization-test :as observation-fixture]))

(def pre-change-score-digest
  ;; Captured from the real fixture at futon2
  ;; 0eecab4ac34a45cc94297f55fb8d7db8c7c7e019, before :consumed-g was added.
  "cbb149b7788171ebe4f8d83c7e0ba60dc7c373001b61b57f7526ec179f886138")

(defn fixture-run []
  (let [call (atom nil)
        score scoring/rank-cascade-actions]
    (with-redefs [scoring/rank-cascade-actions
                  (fn [state candidates opts]
                    (let [result (score state candidates opts)]
                      (reset! call {:state state :candidates candidates :opts opts
                                    :ranked result})
                      result))]
      (let [decision (fixture/decision nil)]
        (assoc @call :decision decision)))))

(defn ranked-fixture [] (:ranked (fixture-run)))

(defn score-projection [ranked]
  (mapv #(select-keys % [:cascade-id :controller-score :rank :posterior]) ranked))

(deftest class-score-records-consumed-a-c-d-and-upstream-q
  (let [{:keys [ranked decision]} (fixture-run)
        candidates (mapv :action ranked)
        census (decomposition/census ranked candidates)
        token-input (get-in decision [:selection-certificate :token-belief-input])]
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
        (is (= {:observation-updates (:observation-updates token-input)
                :conditioning-status (:conditioning-status token-input)
                :reason (:reason token-input)
                :applied-to (get-in decision [:selection-certificate :token-belief-stage
                                              :initialization :value])}
               (get-in consumed [:Q :conditioning])))
        (is (= (:D consumed) (get-in consumed [:Q :initial-belief])
               (:continuation-belief token-input)))))
    (doseq [policy (:policies census)]
      (is (= :present (get-in policy [:terms :A :status])))
      (is (= :deterministic-class-emission
             (get-in policy [:terms :A :reason])))
      (is (= :present (get-in policy [:terms :C :status])))
      (is (= :present (get-in policy [:terms :D :status])))
      (let [q (get-in policy [:terms :Q])]
        (is (= :present (:status q)))
        (is (= :open-loop-no-conditioning (:reason q)))))))

(deftest scorer-without-upstream-conditioning-records-no-q
  (let [{:keys [state candidates opts]} (fixture-run)
        ranked (scoring/rank-cascade-actions
                state candidates (dissoc opts :upstream-initialization-conditioning))]
    (is (every? #(not (contains? (get-in % [:certificate :consumed-g]) :Q)) ranked))))

(deftest live-selection-completes-with-class-a-in-its-census
  (let [decision (fixture/decision nil)
        policies (get-in decision [:selection-certificate :g-term-decomposition :policies])]
    (is (seq policies))
    (doseq [policy policies]
      (is (= :present (get-in policy [:terms :A :status])))
      (is (= :deterministic-class-emission
             (get-in policy [:terms :A :reason]))))))

(deftest signed-false-conditioning-is-carried-verbatim
  (let [ranked-results (atom [])
        score scoring/rank-cascade-actions]
    (with-redefs [scoring/rank-cascade-actions
                  (fn [state candidates opts]
                    (let [result (score state candidates opts)]
                      (swap! ranked-results conj result)
                      result))]
      (observation-fixture/with-two-ticks
        (fn [{:keys [second]}]
          (let [input (get-in second [:selection-certificate :token-belief-input])
                q-values (mapv #(get-in % [:certificate :consumed-g :Q])
                               (last @ranked-results))
                conditioning {:observation-updates (:observation-updates input)
                              :conditioning-status (:conditioning-status input)
                              :reason (:reason input)
                              :applied-to (get-in second [:selection-certificate :token-belief-stage
                                                         :initialization :value])}]
            (is (= :observed-initialization (:conditioning-status conditioning)))
            (is (some #(and (= :updated (:status %)) (false? (:observed %)))
                      (:observation-updates conditioning)))
            (is (every? #(= conditioning (:conditioning %)) q-values))
            (is (every? #(= :observation-conditioned
                            (:reason (decomposition/verdict :Q %))) q-values))
            (println "CLASS-Q-SIGNED-FALSE" (pr-str (first q-values)))))))))

(deftest census-does-not-infer-deleted-d-from-prediction
  (let [entry (first (ranked-fixture))
        without-d (update-in entry [:certificate :consumed-g] dissoc :D)
        census (decomposition/census [without-d] [(:action without-d)])]
    (is (some? (get-in without-d [:prediction :initial-belief])))
    (is (= {:status :missing :value nil :reason :consumed-value-not-recorded}
           (get-in census [:policies 0 :terms :D])))))
