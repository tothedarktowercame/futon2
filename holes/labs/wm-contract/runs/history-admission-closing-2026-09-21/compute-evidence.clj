(require '[cheshire.core :as json]
         '[clojure.java.io :as io]
         '[futon2.aif.task-execution-evidence :as task])
(def dir "holes/labs/wm-contract/runs/history-admission-closing-2026-09-21/")
(def review-job (:job (json/parse-string (slurp (str dir "review-job.json")) true)))
(def author-job (:job (json/parse-string (slurp (str dir "author-job.json")) true)))
(def pre-file (io/file "holes/labs/wm-contract/runs/history-admission-split-2026-09-21/precommit.json"))
(def pre (json/parse-string (slurp pre-file) true))
(def before {:head (:base-head pre) :observed-at-ms (.lastModified pre-file)})
(def result
  {:review-evidence (assoc (task/independent-review-evidence
                            ["src/futon2/aif/full_loop_cohort.clj"
                             "src/futon2/aif/full_loop_runner.clj"] review-job)
                          :reviewer (:agent-id review-job))
   :before-source :retained-pre-commit-receipt
   :before-timestamp-source :receipt-file-last-modified
   :before before
   :author-job-id (:job-id author-job)
   :artifact-binding (task/fresh-artifact-binding {} "/home/joe/code/futon2" before author-job)})
(spit (str dir "computed-evidence.edn") (pr-str result))
(prn result)
(shutdown-agents)
