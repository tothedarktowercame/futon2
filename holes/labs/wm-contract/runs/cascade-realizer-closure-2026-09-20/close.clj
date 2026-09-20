(require '[cheshire.core :as json]
         '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[futon2.aif.repair-obligation :as repair]
         '[futon2.aif.task-execution-evidence :as task]
         '[futon2.aif.tripwire :as trip])

(def dir "holes/labs/wm-contract/runs/cascade-realizer-closure-2026-09-20/")
(def finding-id "repair-02ca4839-5879-44ea-8f59-2839c540814a-construction-yields-no-boxes")
(def commit "28a90c731d30fab8d8e7f8ed7bf0a840c6301f3d")

(defn store-state []
  (let [snapshot (trip/repair-snapshot)
        findings (vec (keep (fn [[p v]] (when (str/starts-with? p "findings/") (:record v))) snapshot))
        closed (into #{} (keep (fn [[p v]]
                                (when (or (str/starts-with? p "resolutions/")
                                          (str/starts-with? p "dismissals/"))
                                  (get-in v [:record :repair/id])))) snapshot)]
    {:finding-id finding-id :closed? (contains? closed finding-id)
     :open-zero-box-findings (mapv :repair/id
                                  (filter #(and (= :construction-yields-no-boxes (:failure-kind %))
                                                (not (contains? closed (:repair/id %)))) findings))
     :violations (trip/livelock-violations findings closed)}))

(try
  (let [job (:job (json/parse-string (slurp (str dir "review-job.json")) true))
        author (:job (json/parse-string (slurp (str dir "author-job.json")) true))
        review (assoc (task/independent-review-evidence
                        ["src/futon2/aif/fold_cascade.clj" "src/futon2/aif/full_loop_runner.clj"] job)
                      :reviewer (:agent-id job))
        binding (:artifact-binding (edn/read-string (slurp (str dir "computed-evidence.edn"))))
        registration (edn/read-string (slurp (str dir "registration.edn")))
        validation-id (get-in registration [:payload :run/id])
        obligation (some #(when (= finding-id (:repair/id %)) %) (repair/open-obligations))
        witness {:resolved? true :dial-moved? true
                 :scope :witnessed-cascade-construction
                 :evidence [(str dir "registration.edn")
                            (str dir "review-job.json")
                            "holes/labs/wm-contract/runs/cascade-realizer-2026-09-20/recorded-expressions-replay.edn"]
                 :positive {:input-kind :constructed-production-shaped :boxes 1
                            :policy-holes 0 :coverage-score-delta -1.0}
                 :negative {:boxes 0 :policy-holes 1 :coverage-score-delta nil}
                 :recorded-c6-mission {:boxes 0 :policy-holes 3 :coverage-score-delta nil}
                 :claim :construction-capability-not-mission-completion}]
    (when-not (and obligation (= :construction-yields-no-boxes (:failure-kind obligation))
                   (:valid? review) (= "claude-2" (:agent-id job))
                   (= "invoke-1789947598636-22808-85f9818f" (:job-id job))
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
                                           :boxes 1 :coverage-score-delta -1.0
                                           :scope :constructed-production-shaped-successor
                                           :recorded-mission-completion? false}})]
      (prn {:phase :resolution :record resolution}))
    (prn {:phase :after :state (store-state)}))
  (catch Throwable e
    (prn {:phase :refused :message (.getMessage e) :data (ex-data e)})
    (prn {:phase :after-refusal :state (store-state)})
    (shutdown-agents)
    (System/exit 1)))
(shutdown-agents)
