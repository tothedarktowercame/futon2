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
