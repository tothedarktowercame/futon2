(ns futon2.aif.mission-observation-deferred-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as owner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.mission-observation :as mission-observation]
            [futon2.aif.observation-checks :as observation-checks]
            [futon2.aif.policy :as policy]
            [futon2.aif.token-outcome-test :as token-fixture]
            [futon2.test-support.runner-fixture :as runner-fixture]))

(use-fixtures :once hermetic/with-hermetic-stores
              runner-fixture/with-hermetic-traces)
(use-fixtures :each (var owner/with-field-desk-stub)
              (var owner/without-live-wm-status))

(defn- isolated [f]
  (with-redefs [runner/refuse-on-runner-source-drift!
                (constantly {:runner/source-check :isolated-worktree})
                runner/selected-entry
                (fn [j]
                  (when-let [a (get-in j [:decision :action])]
                    {:action a :rank 1 :G-efe -1 :controller-score -1}))]
    (f)))

(defn- scenario [sidecar-change]
  (token-fixture/with-artifact
   (fn [sha]
     (let [action (assoc (edn/read-string
                          (slurp "test/fixtures/narrative-trace/selected-action-1789964661.edn"))
                         :want [:hole/h6378c65a4012])
           decision ((var owner/with-token-fixture-carry)
                     (policy/select-action-cascades
                      [(runner-fixture/loop-exercised-ranked-entry action -2.0 1)]
                      {:beta 2.0}))
           cohort ((var owner/retention-cohort) (str "deferred-owner-" (name sidecar-change)))
           first-run
           (isolated
            #((var owner/run-token-feature-card-attempt)
              cohort decision
              {:artifact-sha sha
               :runner-options
               {:poll-fn (fn [_ job-id]
                           (throw (ex-info "synthetic held author"
                                           {:failure-kind :agent-budget-expired
                                            :job-id job-id})))}}))
           stop-line (assoc (get-in first-run [:result :data :repair-obligation])
                            :repair/class :incomplete-recoverable)
           sidecar (io/file (:root cohort) "test-cohort-exhaustion" "attempt-001"
                            "pre-author-mission-baseline.edn")
           original (slurp sidecar)
           repo (str observation-checks/repo-root "/futon2")
           _ (case sidecar-change
               :missing (io/delete-file sidecar)
               :tampered (spit sidecar (pr-str (assoc-in (edn/read-string original)
                                                         [:baseline :target] "M-foreign")))
               :valid (do
                        (let [mission (io/file repo "holes/missions/M-aif-policy-conditioned-eig.md")]
                          (spit mission
                                (str/replace-first
                                 (slurp mission)
                                 "- [ ] **Mint the shared posterior updater"
                                 "- [x] **Mint the shared posterior updater")))
                        (token-fixture/command repo "add"
                                               "holes/missions/M-aif-policy-conditioned-eig.md")
                        (token-fixture/command repo "-c" "user.name=Fixture" "-c"
                                               "user.email=fixture@example.invalid"
                                               "commit" "-m" "advance fixture head")))
           moved-head (token-fixture/command repo "rev-parse" "HEAD")
           second-run
           (isolated
            #((var owner/run-token-feature-card-attempt)
              cohort decision
              {:artifact-sha moved-head
               :runner-options
               {:repair-open-fn (constantly [stop-line])
                :judge-fn (fn [_]
                            {:judgement
                             ((var owner/explicitly-selected-repair-judgement) stop-line)})
                :read-job-fn
                (fn [_ job-id]
                  {:job-id job-id :state "done" :artifact-ref moved-head
                   :feature-card owner/feature-card-claim
                   :execution owner/successful-execution
                   :result-summary (str "FULL_LOOP_AUTHOR: DONE " moved-head)})}}))]
       {:sha sha :moved-head moved-head :first first-run :second second-run
        :after (mission-observation/capture-after
                {:dispatch (get-in (edn/read-string original)
                                   [:baseline :request :dispatch])
                 :repository repo :revision moved-head})
        :close (let [f (io/file (:root cohort) "test-cohort-exhaustion"
                                "attempt-002" "007-closed.edn")]
                 (when (.isFile f) (edn/read-string (slurp f))))
        :origin (get-in stop-line [:failure-data :deferred-author-dispatch-context
                                   :mission-observation-origin])}))))

(deftest shared-artifact-deferred-resume-and-sidecar-refusals
  (let [{:keys [sha moved-head first second origin close after]} (scenario :valid)
        validation (get-in second [:result :checkpoints :build :judgment :validation])]
    (is (= :incomplete (get-in first [:result :outcome])))
    (is (= "attempt-001" (:attempt-id origin)))
    (is (not= sha moved-head))
    (is (= [true] (mapv #(get-in % [:result :observed]) after)) (pr-str after))
    (is (= :grounded-change (get-in second [:result :outcome])))
    (is (= '("feature-review") (sort (keys (:dispatch-requests second)))))
    (is (= :pass (get-in validation [:reviewer-falsifier :checks
                                     :selected-want-accounting :status]))
        (pr-str (get-in validation [:reviewer-falsifier :checks
                                    :selected-want-accounting])))
    (is (map? (get-in close [:payload :judgment :token-outcome-comparison]))
        "pattern comparison remains separately retained"))
  (doseq [change [:missing :tampered]]
    (let [{:keys [second]} (scenario change)]
      (is (= :incomplete (get-in second [:result :outcome])))
      (is (empty? (:dispatch-requests second))
          (str change " sidecar refuses before a new external send")))))
