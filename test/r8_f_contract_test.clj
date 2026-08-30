#!/usr/bin/env bb
(ns r8-f-contract-test
  (:require [babashka.fs :as fs]
            [checks.r8-f-contract :as r8]
            [clojure.test :refer [deftest is run-tests testing]]))

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

(deftest current-r8-baseline
  (let [report (r8/lint-paths {})]
    (is (true? (get-in report [:summary :pass?])))
    (is (= 53 (get-in report [:summary :files])))
    (is (= 792 (get-in report [:summary :forms])))
    (is (= r8/expected-census (get-in report [:r8CensusWmTrace :counts])))
    (is (= {:without-stored-F 760 :with-stored-F 32}
           (get-in report [:r8EraBoundary :era-counts])))
    (is (= {:g-map 760 :controller-map 32 :unknown 0
            :both-keys 0 :neither-key 0}
           (get-in report [:r8EraBoundary :shape-counts])))
    (is (= {:storedF-iff-selectionGain 0
            :storedF-iff-controllerMap 0
            :storedF-iff-dateSuffix 0}
           (get-in report [:r8EraBoundary :conjunct-violations])))
    (is (= {:latest-pre-boundary 20260709
            :earliest-post-boundary 20260714}
           (get-in report [:r8EraBoundary :date-margin])))
    (is (zero? (get-in report [:F :missing-F-computable :non-finite])))
    (is (= 0.0 (get-in report [:F :stored-recompute-consistency
                               :max-absolute-delta])))))

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
                :content-pin {:algorithm :test :sha256 "abc"}}
        lean (r8/lean-fixture-text report records)]
    (is (re-find #"hasControllerScore := false" lean))
    (is (re-find #"hasGTotal := true" lean))
    (is (re-find #"fileDate := 20260709" lean))
    (is (not (re-find #"disposition :=|freeEnergyShape :=|boundary :=" lean)))))

(let [{:keys [fail error]} (run-tests)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
