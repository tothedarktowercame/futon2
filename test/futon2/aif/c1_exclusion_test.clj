(ns futon2.aif.c1-exclusion-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.c1-exclusion :as sut]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.refusal-legality :as refusal-legality]
            [futon2.aif.outer-select :as outer-select]))

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
    (is (= [{:target "M-target" :reason :c1-already-satisfied :pin digest :run-id "run"}]
           (sut/field-exclusions [result] (constantly "mission") ["M-target"])))
    (is (empty? (sut/field-exclusions [result] (constantly "changed") ["M-target"])))
    (is (empty? (sut/field-exclusions [result] (constantly "mission") ["M-other"])))))

(deftest legal-terminals-are-bounded
  (doseq [n (range 1 12)
          k (range (inc n))]
    (let [field (mapv #(str "M-" %) (range n))
          digest (identity/digest {:mission-source-sha256 "d"})
          rows (mapv (fn [target]
                       {:status :row :row {:target target :evidence-digest digest}})
                     (take k field))
          forbidden (set (map :target
                              (sut/field-exclusions rows (constantly "d") field)))]
      (is (<= (count (remove forbidden field)) (- n k))))))

(deftest forbidden-shape-matches-outer-select-contract
  ;; the real predicate `outer-select/select` applies, not a copy of it
  (let [digest (identity/digest {:mission-source-sha256 "pin"})
        row {:status :row :row {:target "M-a" :evidence-digest digest}}
        field [{:target "M-a"} {:target "M-b"}]
        output (sut/field-exclusions [row row] (constantly "pin") ["M-a" "M-b"])]
    (is (outer-select/valid-forbidden-evidence?
         (set (map :target output)) output field))))

(deftest duplicate-rows-yield-one-forbidden-entry
  ;; bad case: a re-appended row must not break outer_select's
  ;; forbidden-set/evidence-count equality and refuse the selection
  (let [digest (identity/digest {:mission-source-sha256 "d"})
        row {:status :row :row {:target "M-a" :evidence-digest digest}}
        out (sut/field-exclusions [row row] (constantly "d") ["M-a" "M-b"])]
    (is (= 1 (count out)))
    (is (= (count (set (map :target out))) (count out)))))

(deftest legal-terminal-excludes-from-field-partition
  (let [digest (identity/digest {:mission-source-sha256 "sha"})
        exclusions (sut/field-exclusions
                    [{:status :row :row {:target "M-a" :evidence-digest digest
                                        :run-id "r"}}]
                    (constantly "sha") ["M-a" "M-b"])
        result (outer-select/apply-c1-exclusions
                {:field [{:target "M-a"} {:target "M-b"}] :excluded []}
                [{:target "M-a"} {:target "M-b"}] exclusions)]
    (is (= ["M-b"] (mapv :target (:field result))))
    (is (= :c1-already-satisfied (get-in result [:excluded 0 :reason])))))

(deftest legal-terminals-bounded-makes-l1-reachable
  (let [digest (identity/digest {:mission-source-sha256 "sha"})
        targets ["M-a" "M-b"]
        rows (mapv #(hash-map :status :row :row
                              {:target % :evidence-digest digest :run-id "r"}) targets)
        exclusions (sut/field-exclusions rows (constantly "sha") targets)
        result (outer-select/apply-c1-exclusions
                {:field (mapv #(hash-map :target %) targets) :excluded []}
                (mapv #(hash-map :target %) targets) exclusions)]
    (is (empty? (:field result)))
    (is (= :loop-stop
           (-> (refusal-legality/evidence-for-close
                :abstained {:failure-kind :empty-field} [] false)
               refusal-legality/disposition)))))

(deftest no-c1-rows-leave-partition-identical
  (let [partition {:field [{:target "M-a"}] :excluded [{:target "E-x"}]}]
    (is (= partition
           (outer-select/apply-c1-exclusions partition (:field partition) [])))))
