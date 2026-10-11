(ns futon2.aif.three-halves-square-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.three-halves-square :as sut]))

(def fixture (edn/read-string
              (slurp "test/fixtures/three-halves-square/publication-cadence.edn")))

(deftest real-library-reading-validates
  (is (= :valid (:status (sut/validate fixture)))))

(deftest adversarial-square-evidence-refuses
  (testing "wrong map direction"
    (is (some map? (:missing-evidence
                    (sut/validate (assoc-in fixture [:maps :a1] #{[10 0]}))))))
  (testing "well-formed but noncommuting routes"
    (is (some #{:noncommutation}
              (:missing-evidence
               (sut/validate (assoc-in fixture [:maps :a2] #{}))))))
  (testing "two active routes assigning different blend names are inconsistent"
    (let [bad (-> fixture
                  (assoc-in [:objects :B :theory :elems] #{30 31 32 33})
                  (assoc-in [:objects :B :theory :names 33] "other-address")
                  (assoc-in [:maps :b2] #{[20 33] [21 32]}))
          result (sut/validate bad)]
      (is (some #{:inconsistency} (:missing-evidence result))))))

(deftest uncovered-blend-object-refuses
  (let [result (sut/validate (assoc-in fixture [:maps :b2] #{[20 30]}))]
    (is (= :refused (:status result)))
    (is (= {:kind :pushout-coverage :uncovered #{32}}
           (some #(when (and (map? %) (= :pushout-coverage (:kind %))) %)
                 (:missing-evidence result))))))

(def inconsistent-covered
  (assoc-in fixture [:maps :b2] #{[20 32] [21 30]}))

(deftest auxiliary-flag-matrix-matches-the-lean-certificate
  (testing "without auxiliary flags, inconsistent noncommuting routes refuse"
    (let [result (sut/validate inconsistent-covered)]
      (is (= :refused (:status result)))
      (is (some #{:noncommutation} (:missing-evidence result)))
      (is (some #{:inconsistency} (:missing-evidence result)))
      (is (not-any? #(and (map? %) (= :pushout-coverage (:kind %)))
                    (:missing-evidence result)))))
  (testing "aux1 only accepts by retaining only route2 for consistency"
    (is (= :valid (:status
                   (sut/validate (assoc-in inconsistent-covered
                                           [:auxiliary :b1] true))))))
  (testing "aux2 only accepts by retaining only route1 for consistency"
    (is (= :valid (:status
                   (sut/validate (assoc-in inconsistent-covered
                                           [:auxiliary :b2] true))))))
  (testing "both auxiliary flags make route consistency vacuous"
    (is (= :valid (:status
                   (sut/validate (assoc inconsistent-covered
                                        :auxiliary {:b1 true :b2 true})))))))

(deftest both-auxiliary-flags-do-not-waive-joint-coverage
  (let [receipt (-> fixture
                    (assoc-in [:maps :b2] #{[20 30]})
                    (assoc :auxiliary {:b1 true :b2 true}))
        result (sut/validate receipt)]
    (is (= :refused (:status result)))
    (is (= {:kind :pushout-coverage :uncovered #{32}}
           (some #(when (and (map? %) (= :pushout-coverage (:kind %))) %)
                 (:missing-evidence result))))))

(deftest noncommutation-refuses-even-when-active-routes-are-consistent
  (let [result (sut/validate (assoc-in fixture [:maps :a2] #{}))]
    (is (= :refused (:status result)))
    (is (some #{:noncommutation} (:missing-evidence result)))
    (is (not (some #{:inconsistency} (:missing-evidence result))))))
