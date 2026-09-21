(require '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[futon2.aif.repair-obligation :as repair]
         '[futon2.aif.tripwire :as trip])

(def root "holes/labs/wm-contract/runs/decision-guard-locators-2026-09-21/")
(def repair-id "repair-occ-7737547f116c5976ba54e7cad66e32de8f1fb7c41bf88f36e9a9a8a5783a5be6")
(def author-id "invoke-1789961383253-22840-39530df4")
(def review-id "invoke-1789962246456-22856-f5ab9079")
(def implementation-commit "561d761ea321adadf8aee3291e3cb8f38aee1c53")

(defn store-state []
  (let [snapshot (trip/repair-snapshot)
        findings (vec (keep (fn [[p v]] (when (str/starts-with? p "findings/") (:record v))) snapshot))
        closed (into #{} (keep (fn [[p v]]
                                (when (or (str/starts-with? p "resolutions/")
                                          (str/starts-with? p "dismissals/"))
                                  (get-in v [:record :repair/id])))) snapshot)]
    {:target-open? (boolean (some #(and (= repair-id (:repair/id %))
                                        (not (contains? closed repair-id))) findings))
     :open-count (count (remove #(contains? closed (:repair/id %)) findings))
     :violations (trip/livelock-violations findings closed)}))

(defn close-reviewed! []
  (let [{:keys [artifact-binding review-evidence validation-run validation-warrant?]}
        (edn/read-string (slurp (str root "closing/computed-evidence.edn")))
        obligation (some #(when (= repair-id (:repair/id %)) %) (repair/open-obligations))
        witness {:resolved? true :dial-moved? true
                 :scope :guard-locator-class-admissibility
                 :evidence [(str root "DESIGN.md") (str root "AFTER-B1.md")
                            (str root "closing/review-job.json")
                            (str root "closing/precommit-pin.log")]
                 :discriminators [:valid-C3-through-C6 :each-required-field-invalid
                                  :C4-requires-decl :C5-requires-bundle-path-and-entry
                                  :nil-empty-unsupported-class-refuse
                                  :handler-field-addition-detected-by-oracle
                                  :unknown-C7-refuses-with-and-without-handler
                                  :pooled-nil-refusal-preserved]
                 ;; Schema-2 writer retains these inside its witness; schema-3
                 ;; has separate top-level slots. Do not rewrite the finding.
                 :review-evidence review-evidence :artifact-binding artifact-binding}]
    (when-not (and obligation (= implementation-commit (:commit artifact-binding))
                   (:fresh-author? artifact-binding) (:descendant? artifact-binding)
                   (:in-author-window? artifact-binding) (:corroborates? artifact-binding)
                   (false? (:disagreement? artifact-binding))
                   (:valid? review-evidence) (= "claude-2" (:reviewer review-evidence))
                   (= review-id (:job-id review-evidence))
                   validation-warrant? (string? validation-run))
      (throw (ex-info "Closing precondition failed" {:obligation obligation
                                                    :review review-evidence
                                                    :binding artifact-binding})))
    (prn {:phase :before :T8 (store-state)})
    (let [implementation (repair/record-implementation!
                           obligation {:attempt-id author-id :commit implementation-commit
                                       :reviewer "claude-2" :review-job review-id
                                       :review-evidence review-evidence
                                       :artifact-binding artifact-binding :witness witness})]
      (prn {:phase :implementation :record implementation}))
    (let [ready (some #(when (= repair-id (:repair/id %)) %) (repair/open-obligations))
          resolution (repair/resolve!
                       ready {:attempt-id validation-run :commit implementation-commit
                              :reviewer "claude-2" :review-job review-id :witness witness
                              :validation {:production-shaped? true :run-id validation-run
                                           :warrant-id "test-registry-09682b8110a1f4aa422fa23193427ab57224b4397f2c37c6b2ed59c7c0bc1177"
                                           :scope :real-handler-locator-checks-and-full-loop-runner
                                           :runner-results {:tests 180 :assertions 998
                                                            :failures 0 :errors 0}}})]
      (prn {:phase :resolution :record resolution}))
    (prn {:phase :after :T8 (store-state)})))

(try (close-reviewed!)
     (catch Throwable e
       (prn {:phase :refused :message (.getMessage e) :data (ex-data e)})
       (prn {:phase :after-refusal :T8 (store-state)})
       (shutdown-agents)
       (System/exit 1)))
(shutdown-agents)
