(ns futon2.aif.cascade-pattern-counts-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.cascade-shape-g :as shape]
            [futon2.aif.cascade-coapply-g :as g]
            [futon2.aif.cascade-pattern-counts :as counts]))

(def recorded {:status :recorded-trials :trials-count 3 :successes 2
               :identities ["a" "b" "c"] :theta 5/8})
(defn arranged [id record]
  (with-redefs [ledger/pattern-theta (constantly record)]
    (shape/arranged->candidate "T" id {:nodes [{:pattern :p :occurrence-id :occ}] :edges []})))

(deftest real-ledger-through-arranged-candidate
  (let [root (.toFile (java.nio.file.Files/createTempDirectory "pattern-counts-" (make-array java.nio.file.attribute.FileAttribute 0)))
        read-theta ledger/pattern-theta
        effect ["T" :done]]
    (try
      (ledger/record! (.getPath root)
                      {:trials (mapv (fn [id observed]
                                       {:status :admitted-at-attempt-grain
                                        :deduplication {:identity id}
                                        :learning-family "fixture" :meaning-sha256 "fixture"
                                        :after-observation observed :effect effect
                                        :selected-cascade {:precedence [{:id :p :produces #{effect}}]}})
                                     ["a" "b" "c"] [true true false])})
      (let [c (with-redefs [ledger/pattern-theta #(read-theta % (.getPath root))]
                (shape/arranged->candidate "T" :real
                  {:nodes [{:pattern :p :occurrence-id :seen}
                           {:pattern :unseen :occurrence-id :new}] :edges []}))
            result (counts/family-pattern-counts [c])]
        (is (= [5/2 3/2] (get-in result [:pattern-counts :real :seen])))
        (is (= [1/2 1/2] (get-in result [:pattern-counts :real :new])))
        (is (= :jeffreys-prior (get-in result [:table :unseen :source])))
        (is (= 3 (get-in result [:table :p :trials-count]))))
      (finally
        (doseq [f (reverse (file-seq root))] (io/delete-file f))))))

(deftest shared-pattern-counts-feed-canonical-g
  (let [family [(arranged :a recorded) (arranged :b recorded)]
        result (counts/family-pattern-counts family)
        token ["T" :pattern-done :occ]
        ranked (g/compare-at-horizon {:cascade-belief {#{} 1}} family
                 {:pattern-counts (:pattern-counts result) :horizon-steps 1
                  :prediction-context {:occurrence-id "fixture" :tau 1}
                  :observation-model (shape/class-emission-model "T" #{token} #{token} 1 :focused {:status :synthetic :source "pattern-counts fixture"})
                  :cascade-spec {:want #{token} :evidence #{} :zeroed #{}}})]
    (is (= {:a {:occ [5/2 3/2]} :b {:occ [5/2 3/2]}} (:pattern-counts result)))
    (is (= 1 (count (:table result))))
    (is (= 2 (count ranked)))
    (is (= [1/8 1/8] (mapv #(get-in % [:certificate :parameter-novelty :steps 0 :expected-novelty]) ranked)))))

(deftest inconsistent-snapshot-refuses-family
  (doseq [other [(assoc recorded :successes 1)
                 (assoc recorded :trials-count 4)
                 (assoc recorded :identities ["other"])
                 (assoc recorded :status :no-recorded-trials)]]
    (let [result (counts/family-pattern-counts [(arranged :a recorded) (arranged :b other)])]
      (is (= :refused (:status result)))
      (is (= :inconsistent-pattern-belief (:kind result)))
      (is (= :p (:pattern-id result)))
      (is (= [recorded other] (:records result)))
      (is (not (contains? result :pattern-counts))))))

(deftest defaulted-and-missing-records
  (let [default (arranged :default {:status :defaulted :reason :ledger-unreadable})
        missing (update-in (arranged :missing recorded) [:precedence :co-apply :patterns :occ] dissoc :theta-record)
        result (counts/family-pattern-counts [default missing])]
    (is (= {:counts [1/2 1/2] :source :defaulted :trials-count 0 :successes 0 :reason :ledger-unreadable}
           (get-in result [:table :p])))
    (is (= {:default {:occ [1/2 1/2]}} (:pattern-counts result)))
    (is (= {:missing {:kind :pattern-belief-missing}} (:refused result)))))
