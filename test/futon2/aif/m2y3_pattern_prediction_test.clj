(ns futon2.aif.m2y3-pattern-prediction-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.token-outcome :as outcome]))

(def fixture-path "test/resources/m2w/pattern-prediction-90ca.edn")
(def fixture-sha "894ef46746d877abbcc7f3156f494c5006e30e0c9e58274ca362ff3857872d0f")
(defn entry [] (edn/read-string (slurp fixture-path)))

(defn sealed-input [e]
  ;; The retained token model is independent of the removed outer selector.
  (outcome/pattern-prediction-input e))

(defn decision [e]
  {:action (:action e)
   :selection-certificate
   {:selection-route :wm/selected-outer-cascade-policy-v1
    :pattern-prediction-input (sealed-input e)}})

(deftest retained-90ca-freezes-native-pattern-wants
  (let [e (entry)
        prediction (outcome/freeze-prediction (decision e))
        wanted (mapv :token (:wanted prediction))]
    (is (= fixture-sha (identity/sha256 (.getBytes (slurp fixture-path) "UTF-8"))))
    (is (= :frozen (:status prediction)))
    (is (= 10 (:horizon prediction)))
    (is (= {#{} 1} (:initial-belief prediction)))
    (is (= (vec (sort-by pr-str (get-in e [:observation-model :want]))) wanted)
        "Native scorer tokens must not be qualified a second time")
    (is (= 2 (count wanted)))
    (is (every? #(and (vector? %) (= 3 (count %))) wanted))
    (is (not-any? #(and (keyword? %) (= "hole" (namespace %)))
                  (tree-seq coll? seq prediction)))
    (is (not= :frozen (:status (outcome/freeze-prediction
                              (update (decision e) :selection-certificate
                                      dissoc :pattern-prediction-input)))))))

(deftest input-identity-and-malformed-source-controls
  (let [e (entry) input (sealed-input e)]
    (doseq [changed [(-> e (assoc :horizon-steps 9)
                        (assoc-in [:observation-model :horizon] 9))
                     (assoc e :transition-kernel :different-kernel)]]
      (is (not= (:model-id input) (:model-id (sealed-input changed)))))
    (testing "tampered carrier cannot reuse its old identity"
      (is (= :refused
             (:status (outcome/freeze-prediction
                       (assoc-in (decision e)
                                 [:selection-certificate :pattern-prediction-input :horizon] 9)))))
      ;; Consistent tamper: passes shape validation, so only :model-id can catch it.
      (is (= :refused
             (:status (outcome/freeze-prediction
                       (-> (decision e)
                           (assoc-in [:selection-certificate :pattern-prediction-input :horizon] 9)
                           (assoc-in [:selection-certificate :pattern-prediction-input
                                      :observation-model :horizon] 9)))))))
    (doseq [changed [(dissoc e :prediction)
                     (assoc e :horizon-steps nil)
                     (assoc-in e [:observation-model :want]
                               #{[(:target-id e) (first (get-in e [:observation-model :want]))]})]]
      (is (= :absent (:status (sealed-input changed))))
      (is (= :refused (:status (outcome/freeze-prediction (decision changed))))))))
