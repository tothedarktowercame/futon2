(require '[cheshire.core :as json] '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.task-execution-evidence :as task])
(def root "holes/labs/wm-contract/runs/decision-guard-locators-2026-09-21/")
(defn read-json [p] (json/parse-string (slurp (str root p)) true))
(let [review (:job (read-json "closing/review-job.json"))
      author (:job (read-json "closing/author-job.json"))
      pre (read-json "precommit-hashes.json")
      before {:head (:head pre) :observed-at-ms (.lastModified (io/file (str root "precommit-hashes.json")))}
      binding (task/fresh-artifact-binding {} "/home/joe/code/futon2" before author)
      review-evidence (assoc (task/independent-review-evidence
                              ["src/futon2/aif/decision_gate.clj" "test/futon2/aif/decision_gate_test.clj"] review)
                             :reviewer (:agent-id review))
      validation (edn/read-string (get-in (read-json "runner-after-b1-record.json") [:entry :evidence/body :payload-edn]))
      result {:before-source :retained-precommit-hashes-receipt
              :before-timestamp-source :receipt-file-last-modified
              :before before :artifact-binding binding :review-evidence review-evidence
              :validation-run (:run/id validation) :validation-warrant? (:warrant? validation)}]
  (spit (str root "closing/computed-evidence.edn") (pr-str result))
  (prn result))
(shutdown-agents)
