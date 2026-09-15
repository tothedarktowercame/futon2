;; claude-2 P1b-1 review: run the committed store tests against each mutant in
;; one JVM (require tests, load-file each mutant over the namespace, run, then
;; reload the original). KILLED = at least one failing or erroring assertion.
;; Each mutant's expectation ("kill" or "survive") is compared with the result.
;; Run from futon2:  clojure -M:test holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/p1b-1-review/run_mutants.clj
(require '[clojure.test :as t]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         'futon2.aif.work-target-store-test)

(def dir "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/p1b-1-review/mutants")

(defn run-suite []
  (let [out (java.io.StringWriter.)
        r (binding [t/*test-out* out]
            (t/run-tests 'futon2.aif.work-target-store-test))]
    (select-keys r [:test :pass :fail :error])))

(def results
  (vec (for [^java.io.File f (sort-by #(.getName ^java.io.File %) (.listFiles (io/file dir)))
             :when (str/ends-with? (.getName f) ".clj")]
         (let [expect (str/trim (slurp (str/replace (.getPath f) #"\.clj$" ".expect")))]
           (load-file (.getPath f))
           (let [r (run-suite)
                 killed? (pos? (+ (:fail r) (:error r)))]
             {:mutant (.getName f) :expect expect :result r :killed? killed?
              :as-expected? (= killed? (= "kill" expect))})))))

(load-file "src/futon2/aif/work_target_store.clj")
(def original (run-suite))

(prn {:mutants results
      :all-as-expected? (every? :as-expected? results)
      :original-after-reload original
      :original-clean? (zero? (+ (:fail original) (:error original)))})
(shutdown-agents)
