(require '[cheshire.core :as json]
         '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[futon2.aif.repair-obligation :as repair]
         '[futon2.aif.task-execution-evidence :as task]
         '[futon2.aif.tripwire :as trip])

(def dir "/home/joe/code/futon2/holes/labs/wm-contract/runs/history-admission-closing-2026-09-21/")
(def finding-id "repair-occ-f9f4e9708737cb15163f92d0d31419aae2097c5095ffdf6b5c4a3dc08d7057ba")
(def commit "707e680f8ef28dbdc40d4f4741a3ec14d75fb9e3")

(defn store-state []
  (let [snapshot (trip/repair-snapshot)
        findings (vec (keep (fn [[p v]] (when (str/starts-with? p "findings/") (:record v))) snapshot))
        closed (into #{} (keep (fn [[p v]]
                                (when (or (str/starts-with? p "resolutions/")
                                          (str/starts-with? p "dismissals/"))
                                  (get-in v [:record :repair/id])))) snapshot)]
    {:finding-id finding-id :closed? (contains? closed finding-id)
     :open-initialization-findings (mapv :repair/id
                                  (filter #(and (= :initialization-failed (:failure-kind %))
                                                (not (contains? closed (:repair/id %)))) findings))
     :violations (trip/livelock-violations findings closed)}))

(try
  (let [job (:job (json/parse-string (slurp (str dir "review-job.json")) true))
        author (:job (json/parse-string (slurp (str dir "author-job.json")) true))
        review (assoc (task/independent-review-evidence
                        ["src/futon2/aif/full_loop_cohort.clj" "src/futon2/aif/full_loop_runner.clj"] job)
                      :reviewer (:agent-id job))
        binding (:artifact-binding (edn/read-string (slurp (str dir "computed-evidence.edn"))))
        registration (edn/read-string (slurp (str dir "registration-pinned-history.edn")))
        validation-id (get-in registration [:payload :run/id])
        obligation (some #(when (= finding-id (:repair/id %)) %) (repair/open-obligations))
        witness {:resolved? true :dial-moved? true
                 :scope :history-initialization-and-identity-admission
                 :evidence [(str dir "registration-pinned-history.edn")
                            (str dir "review-job.json")
                            "holes/labs/wm-contract/runs/history-admission-split-2026-09-21/poison-proceeds-run-record.edn"
                            "holes/labs/wm-contract/runs/history-admission-split-2026-09-21/identity-refuses-run-record.edn"]
                 :non-identity-poison {:initialization :proceeds
                                       :exclusion :recorded-with-path
                                       :readback :equal}
                 :unreadable-identity {:admission :refused
                                       :failure-kind :history-identity-unavailable
                                       :blocking-path :recorded
                                       :new-attempts 0}
                 :review-probes {:exclusion-roundtrip-passed 6
                                 :valid-keyword-inputs-not-tested 2
                                 :existing-attempt-directories 133
                                 :existing-identity-refusals 0}
                 :claim :scratch-history-production-runner-not-live-click}]

    (when-not (and obligation (= :initialization-failed (:failure-kind obligation))
                   (:valid? review) (= "claude-2" (:agent-id job))
                   (= "invoke-1789963375357-22864-360c27c2" (:job-id job))
                   (= commit (:commit binding))
                   (true? (get-in registration [:payload :warrant?]))
                   (string? validation-id))
      (throw (ex-info "Closing evidence precondition failed"
                      {:review review :binding binding :obligation-present? (some? obligation)
                       :registration-valid? (get-in registration [:payload :warrant?])})))
    (prn {:phase :before :state (store-state)})
    (let [implementation (repair/record-implementation!
                           obligation
                           {:attempt-id (:job-id author) :commit commit
                            :reviewer (:agent-id job) :review-job (:job-id job)
                            :review-evidence review :artifact-binding binding :witness witness})]
      (prn {:phase :implementation :record implementation}))
    (let [ready (some #(when (= finding-id (:repair/id %)) %) (repair/open-obligations))
          resolution (repair/resolve!
                       ready {:attempt-id validation-id :commit commit
                              :reviewer (:agent-id job) :review-job (:job-id job)
                              :witness witness
                              :validation {:production-shaped? true :run-id validation-id
                                           :warrant-id (:evidence/id registration)
                                           :scope :scratch-history-production-runner
                                           :non-identity-poison :proceeds-with-exclusion
                                           :identity-poison :typed-admission-refusal
                                           :live-click? false}})]
      (prn {:phase :resolution :record resolution}))
    (prn {:phase :after :state (store-state)}))
  (catch Throwable e
    (prn {:phase :refused :message (.getMessage e) :data (ex-data e)})
    (prn {:phase :after-refusal :state (store-state)})
    (shutdown-agents)
    (System/exit 1)))
(shutdown-agents)
