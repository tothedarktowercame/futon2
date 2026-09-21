(ns futon2.aif.surprise-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.learning-trial-test :as historical]
            [futon2.aif.attempt-learning-test :as scores]
            [futon2.aif.token-outcome :as outcome]))

;; Exercise the new assertions on unmodified main too, without a load error.
(try (require 'futon2.aif.surprise) (catch java.io.FileNotFoundException _ nil))
(defn records [input]
  (if-let [n (find-ns 'futon2.aif.surprise)] ((ns-resolve n 'records) input) []))

(defn inputs []
  (let [{:keys [action terms source-record]} historical/fixture
        decision {:action action
                  :selection-certificate
                  {:precision-family {:model-id "historical-1789964661"
                                      :model {:q0 (get-in terms [:D :value]) :horizon 2}}
                   :token-belief-stage
                   {:domain-inputs (mapv #(hash-map :target (get-in % [:snapshot :target])
                                                  :declaration (:snapshot %))
                                        (get-in source-record [:dispatch :declarations]))}}}
        prediction (outcome/freeze-prediction decision)]
    {:comparison (outcome/compare-outcomes prediction (:after-token-evidence source-record)
                                            (get-in source-record [:revision-pair :after]))
     :occurrence (get-in source-record [:dispatch :occurrence])
     ;; Historical checkpoint date, original measurement timestamp unavailable.
     :declared-at "2026-09-21T04:26:36.986447255Z"}))

(deftest historical-updater-surprise-is-stable-and-record-only
  (let [input (inputs) before (pr-str input) decision-before (pr-str (scores/production-snapshot))
        rows (records input) row (first rows)]
    (is (= 1 (count rows)) "The retained updater mismatch must produce exactly one surprise")
    (is (= historical/updater (:token row)))
    (is (= :predicted-not-observed (:verdict row)))
    (is (= :B-effect (:model-part row)))
    (is (= :none-yet (get-in row [:revision :status])))
    (is (= {:status :not-recorded} (get-in row [:observation :observed-at])))
    (is (= :positive-marginal-support (get-in row [:expectation :rule])))
    (is (= {:expected-observed true} (get-in row [:expectation :tolerance])))
    (is (string? (:surprise/id row)))
    (is (= rows (records input)))
    (is (= rows (binding [*print-namespace-maps* false *print-length* 1 *print-level* 1]
                  (records input))))
    (is (not= (:surprise/id row)
              (:surprise/id (first (records (assoc-in input [:comparison :prediction :model-id] "revised"))))))
    (is (= before (pr-str input)))
    (is (= decision-before (pr-str (scores/production-snapshot))))))

(deftest only-observed-mismatches-to-a-declared-expectation
  (let [input (inputs) p (get-in input [:comparison :prediction])
        sha (get-in input [:comparison :artifact-sha])]
    (testing "missing evidence and repair incidents do not become surprises"
      (is (empty? (records (assoc input :comparison (outcome/compare-outcomes p [] sha)))))
      (is (empty? (records (assoc input :comparison {:repair/class :machine-failure
                                                     :failure-kind :fold-output-invalid})))))
    (testing "matching positive prediction and neither verdict do not surprise"
      (let [rows (mapv #(if (= historical/updater (:token %))
                          (assoc-in % [:result :observed] true) %)
                       (get-in input [:comparison :measurements]))]
        (is (empty? (records (assoc input :comparison (outcome/compare-outcomes p rows sha)))))))
    (testing "absent declaration and reversed measurement clock"
      (is (empty? (records (dissoc input :declared-at))))
      (is (empty? (records (assoc input :observed-at "2026-09-20T00:00:00Z")))))
    (let [measurements (get-in input [:comparison :measurements])
          token (first (filter #(zero? (:predicted %)) (:wanted p)))
          changed (mapv #(if (= (:token token) (:token %)) (assoc-in % [:result :observed] true) %) measurements)
          comparison (outcome/compare-outcomes p changed sha)
          row (first (filter #(= :not-predicted-observed (:verdict %))
                             (records (assoc input :comparison comparison :observed-at "2026-09-21T04:33:00Z"))))]
      (is (= :D-or-external (:model-part row)))
      (is (= :observed-outside-predicted-support-cause-unattributed (:model-part-reason row))))))
