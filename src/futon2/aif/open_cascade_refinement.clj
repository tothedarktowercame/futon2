(ns futon2.aif.open-cascade-refinement
  "Compact durable certificate for the reviewer-triggered refinement boundary."
  (:require [futon2.aif.action-identity :as identity]))

(def schema :wm/open-cascade-refinement-v1)
(defn- pattern-id [step] (if (map? step) (:id step) step))

(defn certificate [{:keys [revision enacted-commit outcome]}]
  (let [cascade (:cascade-revision revision)
        production (:proposal-production cascade)
        [prior revised] (:history cascade)
        revised-action (:revised cascade)
        enaction (:selection-enaction revision)
        admission (:admission production)
        revision-commit (second (:commits revision))
        branch (if (= :revised (:status cascade)) :retrieved-existing :none)
        gaps (cond-> []
               (not= :revised (:status cascade)) (conj {:field :revised-cascade :reason :no-admitted-revision})
               (nil? (:identity prior)) (conj {:field :prior-identity :reason :missing})
               (nil? (:identity revised)) (conj {:field :revised-identity :reason :missing})
               (not= :admitted (:status admission))
               (conj {:field :canonical-admission :reason :admission-receipt-missing})
               (not= :match (:verdict enaction))
               (conj {:field :selected-to-enacted :reason :action-or-step-identity-mismatch})
               (nil? (:selected-action-sha256 enaction))
               (conj {:field :selected-action-identity :reason :missing})
               (nil? (:enacted-action-sha256 enaction))
               (conj {:field :enacted-action-identity :reason :missing})
               (nil? (:enacted-step-sha256 enaction))
               (conj {:field :enacted-step-identity :reason :missing})
               (nil? enacted-commit) (conj {:field :artifact-binding :reason :missing-commit})
               (and revision-commit enacted-commit (not= revision-commit enacted-commit))
               (conj {:field :artifact-binding :reason :commit-mismatch
                      :selected revision-commit :enacted enacted-commit}))]
    {:schema schema :status (if (seq gaps) :refused :complete)
     :boundary :reviewer-negative-verdict :blocker (:blocker cascade)
     :branch branch
     :branch-capabilities
     {:retrieved-existing {:status (if (= branch :retrieved-existing) :used :available)}
      :authored-new {:status :absent :reason :revision-producer-cannot-author-pattern}}
     :library-search (or (:library-search production)
                         {:status :absent :reason :search-evidence-not-retained})
     :pattern (or (:pattern production) (first (get-in cascade [:delta :added])))
     :prior {:identity (:identity prior) :patterns (:patterns prior)}
     :revised {:identity (:identity revised) :patterns (:patterns revised)
               :admission {:status (or (:status admission) :absent)
                           :receipt admission
                           :construction (:construction production)
                           :construction-receipt-sha256
                           (when (:construction-receipt revised-action)
                             (identity/digest (:construction-receipt revised-action)))}}
     :selection-enaction enaction
     :enabled-step {:pattern (pattern-id (:selected-step enaction))
                    :identity (:selected-step-sha256 enaction)}
     :enactment {:action-identity (:enacted-action-sha256 enaction)
                 :step (:enacted-step enaction)
                 :step-identity (:enacted-step-sha256 enaction)}
     :artifact {:selected-commit revision-commit :enacted-commit enacted-commit
                :linked? (and revision-commit (= revision-commit enacted-commit))}
     :outcome (or outcome {:status :absent :reason :outcome-not-yet-recorded})
     :typed-gaps gaps}))

(defn attach-to-run-data [data outcome]
  (cond-> data
    (:revision data)
    (assoc :open-cascade-refinement
           (certificate {:revision (:revision data)
                         :enacted-commit (:commit data)
                         :outcome {:status :recorded :value outcome}}))))
