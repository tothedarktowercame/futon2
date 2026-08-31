#!/usr/bin/env bb
(ns r8-f-contract-test
  (:require [babashka.fs :as fs]
            [checks.r8-f-contract :as r8]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is run-tests testing]]))

(def snapshot-path "test/fixtures/r8-f-contract/snapshot.edn")

(defn- snapshot [] (edn/read-string (slurp snapshot-path)))

(defn- with-corpus [records f]
  (let [dir (fs/create-temp-dir {:prefix "r8-f-contract-"})]
    (try
      (spit (str (fs/path dir "wm-trace-2026-07-14.edn"))
            (apply str (map #(str (pr-str %) "\n") records)))
      (f (str dir))
      (finally (fs/delete-tree dir)))))

(defn- channel [error precision]
  {:errors {:x {:error error}}
   :precision {:x {:precision precision}}})

(defn- tick [{:keys [errors precision stored gain shape]
              :or {shape :controller}}]
  (cond-> {:free-energy (case shape
                          :controller {:controller-score 0.1}
                          :g {:G-total 0.1}
                          :unknown {:other 0.1})}
    (not= errors :absent) (assoc :prediction-errors errors)
    (not= precision :absent) (assoc :precision-state precision)
    (not= stored :absent) (assoc :variational-free-energy stored)
    (not= gain :absent) (assoc :selection-gain gain)))

(deftest committed-r8-snapshot-owns-exact-counts-and-pin
  (let [{:keys [records expected recorded-at]} (snapshot)
        corpus {:files (vec (distinct (map :file records))) :records records}
        report (r8/analyze-corpus corpus r8/default-boundary (:census expected))]
    (is (= "2026-08-31" recorded-at))
    (is (true? (get-in report [:summary :pass?])))
    (is (= (:files expected) (get-in report [:summary :files])))
    (is (= (:forms expected) (get-in report [:summary :forms])))
    (is (= (:census expected) (get-in report [:r8CensusWmTrace :counts])))
    (is (= (:content-pin expected) (get-in report [:content-pin :sha256])))))

(deftest live-r8-gate-enforces-invariants-and-types-append-growth
  (let [report (r8/lint-paths {})
        summary (:summary report)
        delta (:recorded-census-delta report)]
    ;; Moving census values remain evidence. The live verdict is determined by
    ;; era, shape, partition, and numerical invariants instead of old literals.
    (is (every? #(contains? summary %) [:files :forms]))
    (is (true? (:pass? summary)))
    (is (empty? (:failure-classes summary)))
    (is (= {:storedF-iff-selectionGain 0
            :storedF-iff-controllerMap 0
            :storedF-iff-dateSuffix 0}
           (get-in report [:r8EraBoundary :conjunct-violations])))
    (is (zero? (get-in report [:r8EraBoundary :shape-counts :unknown])))
    (is (zero? (get-in report [:F :missing-F-computable :non-finite])))
    (is (= :append-only-growth (:kind delta)))
    (is (= :append-only-growth (:status delta)))
    (is (= 792 (get-in delta [:watermark :forms])))
    (is (= (reduce + (vals (:delta delta)))
           (count (:appended delta))))
    (is (empty? (:reclassified delta)))
    (is (pos? (get-in delta [:delta :stored-F])))))

(deftest pinned-record-reclassification-is-never-append-growth
  (let [corpus (r8/read-corpus r8/default-trace-dir)
        mutation (r8/negative-era-corpus corpus r8/default-boundary)
        report (r8/analyze-corpus
                (:corpus mutation) r8/default-boundary nil
                (r8/recorded-snapshot r8/default-recorded-report))
        delta (:recorded-census-delta report)]
    (is (= :reclassification (:kind delta)))
    (is (not= :append-only-growth (:kind delta)))
    (is (= 1 (count (:reclassified delta))))
    (is (= (:target mutation)
           (select-keys (first (:reclassified delta))
                        [:file :form :timestamp])))))

(deftest disposition-follows-all-three-lean-option-arms
  (let [{:keys [errors precision]} (channel 2.0 3.0)]
    (is (= :missing-F-computable
           (r8/disposition (tick {:errors errors :precision precision
                                  :stored :absent :gain :absent :shape :g}))))
    (is (= :stored-F
           (r8/disposition (tick {:errors errors :precision precision
                                  :stored 6.0 :gain {} :shape :controller}))))
    (is (= :insufficient-inputs
           (r8/disposition (tick {:errors :absent :precision precision
                                  :stored :absent :gain :absent :shape :g}))))
    (is (= :insufficient-inputs
           (r8/disposition (tick {:errors errors :precision :absent
                                  :stored :absent :gain :absent :shape :g}))))))

(deftest missing-prediction-errors-is-not-hidden-by-precision-test
  (let [{:keys [precision]} (channel 1.0 1.0)]
    (with-corpus
      [(tick {:errors :absent :precision precision :stored :absent
              :gain :absent :shape :g})]
      (fn [trace-dir]
        (let [report (r8/lint-paths {:trace-dir trace-dir})]
          (is (= 1 (get-in report [:r8CensusWmTrace :counts
                                   :insufficient-inputs])))
          (is (false? (get-in report [:summary :pass?]))))))))

(deftest era-and-shape-falsifiers-fail-closed
  (let [{:keys [errors precision]} (channel 1.0 1.0)]
    (testing "a post-boundary record without stored F violates non-interleaving"
      (with-corpus
        [(tick {:errors errors :precision precision :stored :absent
                :gain :absent :shape :g})]
        (fn [trace-dir]
          (let [report (r8/lint-paths {:trace-dir trace-dir})]
            (is (= 1 (count (get-in report [:r8EraBoundary :violations
                                            :suffix-without-stored]))))
            (is (= 1 (get-in report [:r8EraBoundary :perEra :after :count])))
            (is (zero? (get-in report
                               [:r8EraBoundary :perEra :after :storedFCount])))
            (is (false? (get-in report [:summary :pass?])))))))
    (testing "an unknown free-energy map is an unexplained regime"
      (is (= :unexplained-regime
             (r8/free-energy-shape
              (tick {:errors errors :precision precision :stored 0.5
                     :gain {} :shape :unknown}))))
      (is (= :unexplained-regime
             (r8/free-energy-shape
              {:free-energy {:G-total 0.1 :controller-score 0.2}}))))))

(deftest recomputation-reports-non-finite-values
  (let [{:keys [errors precision]} (channel Double/POSITIVE_INFINITY 1.0)]
    (is (= 1 (:non-finite
              (r8/distribution
               [(r8/recompute-f (tick {:errors errors :precision precision
                                       :stored :absent :gain :absent
                                       :shape :g}))]))))))

(deftest generated-lean-carries-facts-not-derived-verdicts
  (let [{:keys [errors precision]} (channel 1.0 2.0)
        records [{:file-date 20260709
                  :value (tick {:errors errors :precision precision
                                :stored :absent :gain :absent :shape :g})}]
        report {:summary {:files 1 :forms 1}
                :content-pin {:algorithm :test :sha256 "abc"}
                :r8CensusWmTrace
                {:counts {:missing-F-computable 1
                          :stored-F 0 :insufficient-inputs 0}}}
        lean (r8/lean-fixture-text report records)]
    (is (re-find #"hasControllerScore := false" lean))
    (is (re-find #"hasGTotal := true" lean))
    (is (re-find #"fileDate := 20260709" lean))
    (is (re-find #"noncomputable def generatedEraTable : EraTable" lean))
    (is (re-find #"#print axioms generatedCensus" lean))
    (is (re-find #"r8Census wmTraceR8Generated = \(1, 0, 0\)" lean))
    (is (re-find #"#print axioms generatedEraBoundary" lean))
    (is (not (re-find #"meanPrecision :=|uniform :=" lean)))
    (is (not (re-find #"disposition :=|freeEnergyShape :=" lean)))))

(let [{:keys [fail error]} (run-tests)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
