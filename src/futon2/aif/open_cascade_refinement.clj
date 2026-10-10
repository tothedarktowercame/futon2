(ns futon2.aif.open-cascade-refinement
  "Compact durable certificate for the reviewer-triggered refinement boundary."
  (:require [futon2.aif.action-identity :as identity]))

(def schema :wm/open-cascade-refinement-v1)
(defn- pattern-id [step] (if (map? step) (:id step) step))

(def pattern-use-schema :wm/revision-pattern-use-observation-v1)

(defn certificate [{:keys [revision artifact-commit outcome pattern-use]}]
  (let [cascade (:cascade-revision revision)
        production (:proposal-production cascade)
        [prior revised] (:history cascade)
        revised-action (:revised cascade)
        dispatch (:selection-dispatch revision)
        admission (:admission production)
        revision-commit (second (:commits revision))
        branch (if (= :revised (:status cascade)) :retrieved-existing :none)
        selected-pattern (or (:pattern production) (first (get-in cascade [:delta :added])))
        pattern-use (or pattern-use (:pattern-use-observation revision))
        use-present? (and (map? pattern-use) (not= :absent (:status pattern-use)))
        use-matches? (and (= pattern-use-schema (:schema pattern-use))
                          (= :verified (:status pattern-use))
                          (= selected-pattern (:pattern pattern-use))
                          (= (:dispatched-action-sha256 dispatch)
                             (:action-sha256 pattern-use))
                          (= artifact-commit (:artifact-commit pattern-use))
                          (seq (:application-claims pattern-use))
                          (string? (get-in pattern-use [:reviewer :job-id]))
                          (= :approve (get-in pattern-use [:reviewer :verdict]))
                          (string? (:observation-sha256 pattern-use)))
        hard-gaps (cond-> []
               (not= :revised (:status cascade)) (conj {:field :revised-cascade :reason :no-admitted-revision})
               (nil? (:identity prior)) (conj {:field :prior-identity :reason :missing})
               (nil? (:identity revised)) (conj {:field :revised-identity :reason :missing})
               (not= :admitted (:status admission))
               (conj {:field :canonical-admission :reason :admission-receipt-missing})
               (not= :match (:verdict dispatch))
               (conj {:field :selected-to-dispatched :reason :action-or-step-contract-mismatch})
               (nil? (:selected-action-sha256 dispatch))
               (conj {:field :selected-action-identity :reason :missing})
               (nil? (:dispatched-action-sha256 dispatch))
               (conj {:field :dispatched-action-identity :reason :missing})
               (nil? (:dispatched-step-sha256 dispatch))
               (conj {:field :dispatched-step-identity :reason :missing})
               (nil? artifact-commit) (conj {:field :artifact-binding :reason :missing-commit})
               (and revision-commit artifact-commit (not= revision-commit artifact-commit))
               (conj {:field :artifact-binding :reason :commit-mismatch
                      :selected revision-commit :observed artifact-commit}))
        use-gap (when-not use-matches?
                  {:field :pattern-use
                   :reason (if use-present? :observation-mismatch
                               :observation-unavailable)
                   :expected-pattern selected-pattern
                   :observed-pattern (:pattern pattern-use)})
        gaps (cond-> hard-gaps use-gap (conj use-gap))
        status (cond
                 (seq hard-gaps) :refused
                 (and use-present? (not use-matches?)) :refused
                 use-matches? :verified-used
                 :else :dispatched)]
    {:schema schema :status status
     :boundary :reviewer-negative-verdict :blocker (:blocker cascade)
     :branch branch
     :branch-capabilities
     {:retrieved-existing {:status (if (= branch :retrieved-existing) :used :available)}
      :authored-new {:status :absent :reason :revision-producer-cannot-author-pattern}}
     :library-search (or (:library-search production)
                         {:status :absent :reason :search-evidence-not-retained})
     :pattern selected-pattern
     :prior {:identity (:identity prior) :patterns (:patterns prior)}
     :revised {:identity (:identity revised) :patterns (:patterns revised)
               :admission {:status (or (:status admission) :absent)
                           :receipt admission
                           :construction (:construction production)
                           :construction-receipt-sha256
                           (when (:construction-receipt revised-action)
                             (identity/digest (:construction-receipt revised-action)))}}
     :selection-dispatch dispatch
     :enabled-step {:pattern (pattern-id (:selected-step dispatch))
                    :identity (:selected-step-sha256 dispatch)}
     :dispatch {:action-identity (:dispatched-action-sha256 dispatch)
                 :step (:dispatched-step dispatch)
                 :step-identity (:dispatched-step-sha256 dispatch)}
     :artifact {:selected-commit revision-commit :observed-commit artifact-commit
                :linked? (and revision-commit (= revision-commit artifact-commit))}
     :pattern-use (or pattern-use
                      {:schema pattern-use-schema :status :absent
                       :reason :revision-pattern-use-observation-not-produced})
     :outcome (or outcome {:status :absent :reason :outcome-not-yet-recorded})
     :typed-gaps gaps}))

(defn attach-to-run-data [data outcome]
  (cond-> data
    (:revision data)
    (assoc :open-cascade-refinement
           (certificate {:revision (:revision data)
                         :artifact-commit (:commit data)
                         :pattern-use (:revision-pattern-use data)
                         :outcome {:status :recorded :value outcome}}))))
