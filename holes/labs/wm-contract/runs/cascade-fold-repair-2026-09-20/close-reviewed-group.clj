(require '[cheshire.core :as json]
         '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[futon2.aif.repair-obligation :as repair]
         '[futon2.aif.task-execution-evidence :as task]
         '[futon2.aif.tripwire :as trip])

(def evidence-dir "holes/labs/wm-contract/runs/cascade-fold-repair-2026-09-20/")
(def implementation-commit "c91261fdbc7b80e903d3203ca53cc865291cae9f")
(def review-job-id "invoke-1789939508578-22785-f44e1b13")
(def author-job-id "invoke-1789938649366-22774-8cf6da5a")

(defn store-state []
  (let [snapshot (trip/repair-snapshot)
        findings (vec (keep (fn [[p v]]
                              (when (str/starts-with? p "findings/") (:record v)))
                            snapshot))
        closed (into #{} (keep (fn [[p v]]
                                (when (or (str/starts-with? p "resolutions/")
                                          (str/starts-with? p "dismissals/"))
                                  (get-in v [:record :repair/id])))) snapshot)]
    {:closed-repair-ids closed
     :open-signatures (frequencies
                       (map (juxt :failure-kind :target :failed-commit)
                            (remove #(contains? closed (:repair/id %)) findings)))
     :violations (trip/livelock-violations findings closed)}))

(defn run-closing-sequence! []
  (let [job (:job (json/parse-string (slurp (str evidence-dir "independent-review-job.json")) true))
        computed-review (task/independent-review-evidence
                         ["src/futon2/aif/full_loop_runner.clj"
                          "test/futon2/aif/cascade_fold_repair_test.clj"] job)
        ;; Reviewer identity comes from the fetched job, not a supplied gate flag.
        review-evidence (assoc computed-review :reviewer (:agent-id job))
        binding (:artifact-binding
                 (edn/read-string (slurp (str evidence-dir "computed-artifact-binding.edn"))))
        registration (edn/read-string
                      (slurp (str evidence-dir "review-followup-registration.log")))
        validation-run (get-in registration [:payload :run/id])
        obligations (filterv #(and (= :fold-output-invalid (:failure-kind %))
                                    (= :C1 (:target %)))
                             (repair/open-obligations))]
    (when-not (and (= review-job-id (:job-id job))
                   (= "claude-4" (:agent-id job))
                   (:valid? computed-review)
                   (= implementation-commit (:commit binding))
                   (true? (get-in registration [:payload :warrant?]))
                   (string? validation-run)
                   (= 8 (count obligations)))
      (throw (ex-info "Closing evidence precondition failed"
                      {:review computed-review :artifact-binding binding
                       :validation-run validation-run :finding-count (count obligations)})))
    (prn {:phase :before :T8 (store-state)})
    (doseq [obligation obligations]
      (let [implementation
            (repair/record-implementation!
             obligation
             {:attempt-id author-job-id :commit implementation-commit
              :reviewer (:agent-id job) :review-job review-job-id
              :review-evidence review-evidence :artifact-binding binding
              :witness {:resolved? true :dial-moved? true
                        :scope :fold-output-schema
                        :evidence (str evidence-dir "review-followup-receipt.json")
                        :construction-success? false}})
            ready (some #(when (= (:repair/id obligation) (:repair/id %)) %)
                        (repair/open-obligations))]
        (prn {:phase :implementation :record implementation})
        ;; The library supplies the attached implementation; do not manufacture it.
        (let [resolution
              (repair/resolve!
               ready
               {:attempt-id validation-run :commit implementation-commit
                :reviewer (:agent-id job) :review-job review-job-id
                :witness {:resolved? true :dial-moved? true
                          :scope :fold-output-schema
                          :evidence (str evidence-dir "review-followup-registration.log")
                          :construction-success? false}
                :validation {:production-shaped? true
                             :run-id validation-run
                             :warrant-id (:evidence/id registration)
                             :scope :recorded-input-production-constructor-and-validator
                             :disposition :contract-valid-incomplete
                             :boxes 0 :coverage-score-delta nil}})]
          (prn {:phase :resolution :record resolution}))))
    (let [new-finding
          (repair/record-system-failure!
           {:attempt-id validation-run
            :repair-class :machine-failure
            :machine-repo "/home/joe/code/futon2"
            :target ["M-wm-aif-policy-grain-compliance" "M-expressions-of-interest"]
            :failure-stage :construction :outcome :incomplete
            :failure-kind :construction-yields-no-boxes
            :error "Recorded ordinary cascades yield zero boxes and nil coverage after schema repair and target preservation"
            :failure-data
            {:repair-commit implementation-commit
             :replay-evidence (str evidence-dir "review-followup-registration.log")
             :review-job review-job-id
             :inputs [{:target "M-wm-aif-policy-grain-compliance" :boxes 0
                       :policy-holes 1 :coverage-score-delta nil}
                      {:target "M-expressions-of-interest" :boxes 0
                       :policy-holes 3 :coverage-score-delta nil}]
             :schema-valid? true :target-preserved? true
             :successful-construction? false
             :superseded-schema-findings (mapv :repair/id obligations)
             :successor-required {:boxes-positive? true :negative-coverage-delta? true}}
            :backtrace {:validation-run validation-run
                        :warrant-id (:evidence/id registration)}
            :discharge-contract repair/review-failure-discharge-contract})]
      (prn {:phase :new-finding :record new-finding}))
    (prn {:phase :after :T8 (store-state)})))

(try
  (run-closing-sequence!)
  (catch Throwable e
    (prn {:phase :refused :message (.getMessage e) :data (ex-data e)
          :sequence-stopped true})
    (prn {:phase :after-refusal :T8 (store-state)})
    (shutdown-agents)
    (System/exit 1)))
(shutdown-agents)
