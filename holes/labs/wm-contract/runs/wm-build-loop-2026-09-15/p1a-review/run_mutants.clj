;; claude-2 P1a review: run the committed test suite against each mutant in one
;; JVM. The tests are required first (loading the original), then each mutant is
;; load-file'd over the namespace, so every var -- public and private -- is the
;; mutant's. A mutant is KILLED when at least one assertion fails or errors.
;; Finally the original is reloaded and must pass again.
;; Run from futon2:  clojure -M:test holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/p1a-review/run_mutants.clj
(require '[clojure.test :as t]
         '[clojure.java.io :as io]
         'futon2.aif.work-target-belief-test)

(def dir "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/p1a-review/mutants")

(defn run-suite []
  (let [out (java.io.StringWriter.)
        r (binding [t/*test-out* out]
            (t/run-tests 'futon2.aif.work-target-belief-test))]
    (select-keys r [:test :pass :fail :error])))

(def results
  (vec (for [f (sort-by #(.getName ^java.io.File %) (.listFiles (io/file dir)))
             :when (.endsWith (.getName ^java.io.File f) ".clj")]
         (do (load-file (.getPath ^java.io.File f))
             (let [r (run-suite)]
               {:mutant (.getName ^java.io.File f)
                :result r
                :killed? (pos? (+ (:fail r) (:error r)))})))))

(load-file "src/futon2/aif/work_target_belief.clj")
(def original (run-suite))

(prn {:mutants results
      :all-killed? (every? :killed? results)
      :original-after-reload original
      :original-clean? (zero? (+ (:fail original) (:error original)))})
(shutdown-agents)
