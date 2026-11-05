(ns futon2.aif.c1-exclusion-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.c1-exclusion :as sut]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.outer-refusal-history :as history]
            [futon2.aif.outer-refusal-index :as index]
            [futon2.aif.refusal-legality :as refusal-legality]
            [futon2.aif.outer-select :as outer-select])
  (:import [java.nio.file Files]))

(defn temp-dir []
  (.toFile (Files/createTempDirectory
            "c1-exclusion-"
            (make-array java.nio.file.attribute.FileAttribute 0))))

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

(deftest c3-new-evidence-restores-and-current-evidence-excludes
  (let [old-pin (identity/digest {:mission-source-sha256 "old"})
        rows [{:status :row :exclusion-reason :c3-harm-declined
               :row {:target "M-a" :evidence-digest old-pin :run-id "r"}}]]
    (is (empty? (sut/field-exclusions rows (constantly "new") ["M-a"])))
    (is (= :c3-harm-declined
           (:reason (first (sut/field-exclusions rows (constantly "old") ["M-a"])))))))

(deftest c1-and-c3-current-rows-produce-one-first-index-exclusion
  (let [pin (identity/digest {:mission-source-sha256 "sha"})
        rows [{:status :row :exclusion-reason :c3-harm-declined
               :row {:target "M-a" :evidence-digest pin :run-id "c3-first"}}
              {:status :row :exclusion-reason :c1-already-satisfied
               :row {:target "M-a" :evidence-digest pin :run-id "c1-second"}}]
        exclusions (sut/field-exclusions rows (constantly "sha") ["M-a"])]
    (is (= 1 (count exclusions)))
    (is (= :c3-harm-declined (:reason (first exclusions))))
    (is (= "c3-first" (:run-id (first exclusions))))
    (is (map? (try
                (outer-select/apply-c1-exclusions
                 {:field [{:target "M-a"}] :excluded []}
                 [{:target "M-a"}] exclusions)
                (catch Exception _ nil))))))

(deftest unknown-legal-terminal-exclusion-reason-is-refused
  (is (= :invalid-c1-exclusions
         (try
           (outer-select/apply-c1-exclusions
            {:field [{:target "M-a"}] :excluded []}
            [{:target "M-a"}]
            [{:target "M-a" :reason :guardrail-refusal :pin "pin"}])
           nil
           (catch clojure.lang.ExceptionInfo e (:kind (ex-data e)))))))

(deftest pending-c1-is-resolved-in-the-single-index-line
  (let [dir (temp-dir)
        source (io/file dir "tick-run-record-run.edn")
        pending (assoc (record true) :refusal/disposition
                       {:disposition :pending-exclusion :case :C1})]
    (spit source (pr-str pending))
    (index/append-run-record! dir source pending)
    (let [lines (history/read-index dir)
          line (first lines)
          exclusions (sut/field-exclusions
                      (keep :c1-exclusion lines)
                      (constantly "mission") ["M-target" "M-next"])
          next-field (outer-select/apply-c1-exclusions
                      {:field [{:target "M-target"} {:target "M-next"}]
                       :excluded []}
                      [{:target "M-target"} {:target "M-next"}]
                      exclusions)]
      (is (= 1 (count lines)))
      (is (= :click-terminal
             (get-in line [:refusal/disposition :disposition])))
      (is (= :row (get-in line [:c1-exclusion :status])))
      (is (= "M-target" (get-in line [:c1-exclusion :row :target])))
      (is (= ["M-next"] (mapv :target (:field next-field)))))))

(deftest refused-c1-resolution-is-debugger-error-without-exclusion-row
  (let [dir (temp-dir)
        source (io/file dir "tick-run-record-run.edn")
        pending (assoc (record false) :refusal/disposition
                       {:disposition :pending-exclusion :case :C1})]
    (spit source (pr-str pending))
    (index/append-run-record! dir source pending)
    (let [line (first (history/read-index dir))]
      (is (= :debugger-error
             (get-in line [:refusal/disposition :disposition])))
      (is (= :exclusion-not-recorded
             (get-in line [:refusal/disposition :reason])))
      (is (nil? (:c1-exclusion line))))))

(deftest append-failure-publishes-no-index-line
  (let [dir (temp-dir)
        source (io/file dir "tick-run-record-run.edn")
        pending (assoc (record true) :refusal/disposition
                       {:disposition :pending-exclusion :case :C1})]
    (spit source (pr-str pending))
    (with-redefs [index/append-entry!
                  (fn [& _] (throw (ex-info "append failed" {:kind :test})))]
      (let [failure (try
                      (index/append-run-record! dir source pending)
                      nil
                      (catch clojure.lang.ExceptionInfo e e))]
        (is (= :exclusion-not-recorded (:failure-kind (ex-data failure))))
        (is (= :debugger-error
               (get-in (ex-data failure)
                       [:refusal/disposition :disposition])))))
    (is (not (.exists (io/file dir history/index-name))))))
