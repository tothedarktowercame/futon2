(ns futon2.aif.c1-exclusion-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.c1-exclusion :as sut]))

(def identity-row
  {:mission-source-sha256 "mission" :policy-id "policy"
   :compiled-check-set-sha256 "checks"})

(defn record [measured?]
  {:run/id "run" :terminal-receipt {:outcome :already-satisfied}
   :run-output {:outcome :already-satisfied}
   :failure {:kind :already-satisfied
             :target "M-target"
             :detail {:outcome :already-satisfied :failure-kind :already-satisfied
                      :target "M-target"
                      :mission-observation (when measured? {:status :admitted})}}
   :effective-run-identity identity-row})

(deftest row-requires-measured-c1-and-digest
  (is (= :satisfaction-unmeasured (:reason (sut/exclusion-row (record false)))))
  (is (= :digest-carrier-absent
         (:reason (sut/exclusion-row (dissoc (record true) :effective-run-identity)))))
  (is (= :row (:status (sut/exclusion-row (record true)))))
  (is (= :not-c1 (:reason (sut/exclusion-row {:outcome :grounded-change})))))

(deftest unchanged-excludes-and-changed-restores
  (let [result (sut/exclusion-row (record true))
        digest (get-in result [:row :evidence-digest])]
    (is (= [{:target "M-target" :reason :c1-already-satisfied :pin digest}]
           (sut/forbidden-evidence [result] (constantly digest) ["M-target"])))
    (is (empty? (sut/forbidden-evidence [result] (constantly "changed") ["M-target"])))
    (is (empty? (sut/forbidden-evidence [result] (constantly digest) ["M-other"])))))

(deftest legal-terminals-are-bounded
  (doseq [n (range 1 12)
          k (range (inc n))]
    (let [field (mapv #(str "M-" %) (range n))
          rows (mapv (fn [target]
                       {:status :row :row {:target target :evidence-digest "d"}})
                     (take k field))
          forbidden (set (map :target
                              (sut/forbidden-evidence rows (constantly "d") field)))]
      (is (<= (count (remove forbidden field)) (- n k))))))

(deftest forbidden-shape-matches-outer-select-contract
  (let [row {:status :row :row {:target "M-a" :evidence-digest "pin"}}
        output (sut/forbidden-evidence [row] (constantly "pin") ["M-a"])]
    (is (every? #(and (contains? #{"M-a"} (:target %))
                      (keyword? (:reason %))
                      (string? (:pin %)) (seq (:pin %))) output))))
