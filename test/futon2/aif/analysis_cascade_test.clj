(ns futon2.aif.analysis-cascade-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.test :refer [deftest is]]
            [futon2.aif.analysis-cascade :as sut]))

(def fixture-root "test/fixtures/analysis-cascade")
(def analysis-root (str (System/getProperty "user.home")
                        "/.emacs-graph/session-turn-analysis"))

(defn read-analysis [path]
  (json/parse-string (slurp path) true))

(defn fixture [name]
  (read-analysis (str fixture-root "/" name ".json.analysis.json")))

(defn pattern-set [cascade]
  (set (map :pattern (:nodes cascade))))

(defn validated-patterns [analysis]
  (set (for [sentence (:sentences analysis)
             fragment (:fragments sentence)
             ref (:pattern_refs fragment)
             :when (= "candidate" (:status ref))]
         (:id ref))))

(deftest real-analysis-produces-role-traceable-multipattern-cascade
  ;; Selection rule: lexicographically first real result with >=3 validated
  ;; refs on >=3 distinct fragments (measured over the corpus on 2026-09-30).
  (let [analysis (fixture "turn-0eoQWs")
        result (sut/analysis->cascades analysis)
        cascade (first (:cascades result))]
    (is (<= 3 (count (:nodes cascade))))
    (is (<= 2 (count (:edges cascade))))
    (is (every? #(and (int? (:from-fragment %))
                      (int? (:to-fragment %))
                      (seq (:from-roles %))
                      (seq (:to-roles %)))
                (:edges cascade)))
    (is (every? (validated-patterns analysis) (pattern-set cascade)))))

(deftest roles-distinguish-the-same-pattern-set
  ;; Selection rule: first lexicographic pair with the same validated pattern
  ;; set and different role vectors.
  (let [a (fixture "turn-1hDrqF")
        b (fixture "turn-2D9L5X")
        ca (first (:cascades (sut/analysis->cascades a)))
        cb (first (:cascades (sut/analysis->cascades b)))
        topology #(select-keys % [:nodes :edges :precedence])]
    (is (= (pattern-set ca) (pattern-set cb)))
    (is (not= (topology ca) (topology cb)))))

(deftest alternatives-fork-cascades-without-admitting-rejections
  ;; Selection rule: first lexicographic result where a fragment has multiple
  ;; refs or has a validated ref plus recorded rejected alternatives.
  (let [analysis (fixture "turn-0YLtCD")
        result (sut/analysis->cascades analysis)
        cascades (:cascades result)
        rejected (set (for [sentence (:sentences analysis)
                            fragment (:fragments sentence)
                            ref (:pattern_rejections fragment)] (:id ref)))]
    (is (< 1 (count cascades)))
    (is (some #(seq (:alternatives %)) cascades))
    (is (every? #(empty? (set/intersection rejected (pattern-set %)))
                cascades))))

(deftest all-real-analyses-report-cascade-scale
  (let [files (->> (.listFiles (io/file analysis-root))
                   (filter #(and (.isFile %)
                                 (.endsWith (.getName %) ".analysis.json")))
                   (sort-by #(.getName %)))
        rows (mapv (fn [file]
                     (let [analysis (read-analysis file)
                           result (sut/analysis->cascades analysis)
                           allowed (validated-patterns analysis)]
                       (is (every? #(every? allowed (pattern-set %))
                                   (:cascades result)))
                       {:file (.getName file) :result result}))
                   files)
        cascade-counts (mapv #(count (get-in % [:result :cascades])) rows)
        cascades (mapcat #(get-in % [:result :cascades]) rows)
        report {:analyses (count rows)
                :cascade-counts {:zero (count (filter zero? cascade-counts))
                                 :one (count (filter #{1} cascade-counts))
                                 :two-plus (count (filter #(< 1 %) cascade-counts))}
                :node-counts (into (sorted-map) (frequencies (map #(count (:nodes %)) cascades)))
                :edge-counts (into (sorted-map) (frequencies (map #(count (:edges %)) cascades)))
                :bare-singletons (count (filter #(and (= 1 (count (:nodes %)))
                                                      (empty? (:edges %))) cascades))
                :failures (reduce + (map #(get-in % [:result :failure-count]) rows))}]
    (println "ANALYSIS-CASCADE-CORPUS" (pr-str report))
    (is (pos? (count rows)))
    (is (= (count rows) (reduce + (vals (:cascade-counts report)))))))
