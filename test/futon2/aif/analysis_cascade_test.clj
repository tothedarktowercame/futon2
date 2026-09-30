(ns futon2.aif.analysis-cascade-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.set :as set]
            [clojure.test :refer [deftest is]]
            [futon2.aif.analysis-cascade :as sut]))

(def fixture-root "test/fixtures/analysis-cascade")
(def analysis-root (str (System/getProperty "user.home")
                        "/.emacs-graph/session-turn-analysis"))
(def published-analysis-name #"turn-.*\.json\.analysis\.json")

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

(deftest overlap-keeps-coapplicable-patterns-in-one-arrangement
  ;; Selection rule: lexicographically first real analysis having two
  ;; validated pattern_refs on one fragment.
  (let [analysis (fixture "turn-6TxxZn")
        result (sut/analysis->cascades analysis {:mode :overlap})
        cascade (first (:cascades result))
        overlap (first (filter #(= :overlap (:kind %)) (:edges cascade)))]
    (is overlap)
    (is (= (:from-fragment overlap) (:to-fragment overlap)))
    (is (= 2 (count (filter #(= (:from-fragment overlap) (:fragment-index %))
                            (:nodes cascade)))))
    (is (every? (validated-patterns analysis) (pattern-set cascade)))))

(deftest task-request-preserves-section-and-exact-offsets
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "wm-task-reading" (make-array java.nio.file.attribute.FileAttribute 0)))
        task (io/file dir "M-example.md")
        out (io/file dir "request.json")
        text "# Mission\n\n## Open work\nContext line.\n- [ ] First observable outcome.\n- [ ] Second outcome.\n\n## Later\nNot included.\n"]
    (spit task text)
    (let [{:keys [exit err]} (shell/sh "python3" "scripts/wm_task_reading.py"
                                       (.getPath task) "--target" "M-example"
                                       "--out" (.getPath out))
          request (read-analysis out)
          source (:source_text request)
          item (get-in request [:task :item])]
      (is (zero? exit) err)
      (is (= "M-example" (get-in request [:task :target_id])))
      (is (.startsWith source "## Open work"))
      (is (not (.contains source "## Later")))
      (is (= (:text item) (subs source (:start item) (:end item))))
      (is (every? #(= (:text %) (subs source (:start %) (:end %)))
                  (:sentences request))))))

(deftest mission-request-prefers-head-and-records-opening-fallback
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "wm-mission-reading" (make-array java.nio.file.attribute.FileAttribute 0)))
        with-head (io/file dir "M-head.md")
        no-head (io/file dir "M-opening.md")
        run (fn [file]
              (let [out (io/file dir (str (.getName file) ".json"))
                    result (shell/sh "python3" "scripts/wm_task_reading.py"
                                     (.getPath file) "--mission-head" "--out" (.getPath out))]
                (is (zero? (:exit result)) (:err result))
                (read-analysis out)))]
    (spit with-head "# M-head\nMetadata.\n\n## HEAD\nJoe's words.\n\n## MAP\nLater.\n")
    (spit no-head "# M-opening\nOperator anchor.\n\n## MAP\nLater.\n")
    (let [head (run with-head) opening (run no-head)]
      (is (= "head-section" (get-in head [:task :source_kind])))
      (is (= "## HEAD\nJoe's words." (:source_text head)))
      (is (= "opening-before-first-section" (get-in opening [:task :source_kind])))
      (is (= "# M-opening\nOperator anchor." (:source_text opening))))))

(defn corpus-report [files mode]
  (let [rows (mapv (fn [file]
                     (let [analysis (read-analysis file)
                           result (sut/analysis->cascades analysis {:mode mode})
                           allowed (validated-patterns analysis)]
                       (is (every? #(every? allowed (pattern-set %))
                                   (:cascades result)))
                       {:file (.getName file) :result result}))
                   files)
        cascade-counts (mapv #(count (get-in % [:result :cascades])) rows)
        cascades (mapcat #(get-in % [:result :cascades]) rows)]
    {:analyses (count rows)
     :cascade-counts {:zero (count (filter zero? cascade-counts))
                      :one (count (filter #{1} cascade-counts))
                      :two-plus (count (filter #(< 1 %) cascade-counts))}
     :node-counts (into (sorted-map) (frequencies (map #(count (:nodes %)) cascades)))
     :edge-counts (into (sorted-map) (frequencies (map #(count (:edges %)) cascades)))
     :bare-singletons (count (filter #(and (= 1 (count (:nodes %)))
                                           (empty? (:edges %))) cascades))
     :failures (reduce + (map #(get-in % [:result :failure-count]) rows))}))

(deftest all-real-analyses-report-cascade-scale
  (let [files (->> (.listFiles (io/file analysis-root))
                   (filter #(and (.isFile %)
                                 (re-matches published-analysis-name (.getName %))
                                 (let [analysis (read-analysis %)]
                                   (and (:request_file analysis) (:status analysis)))))
                   (sort-by #(.getName %)))
        report {:alternatives (corpus-report files :alternatives)
                :overlap (corpus-report files :overlap)}]
    (println "ANALYSIS-CASCADE-CORPUS" (pr-str report))
    (is (pos? (count files)))
    (doseq [mode [:alternatives :overlap]]
      (is (= (count files)
             (reduce + (vals (get-in report [mode :cascade-counts]))))))))
