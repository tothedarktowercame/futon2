(ns futon2.aif.controller-authority
  "Machine authorization for the actual controller decision; no fixture recall."
  (:require [futon2.aif.mission-registry :as missions]))

(def operator-decision-evidence-id
  "Joe's bounded-autonomy decision, recorded in futon3c
   src/futon3c/peripheral/live_wm_selection.clj (operator-decision-evidence-id)."
  "6e6f56a1-b9d7-4f83-928f-3a211ef890a0")

(def scope-authority
  "invoke-1789477467267-21004-c4169097 (Joe 2026-09-15)")

(defn authorize
  "Check the open target and exact admissible action before granting bounded
   authorization. Execution, tripwires, stop-lines and delivery QA remain owned
   by the runner. A report alone never executes the decision."
  [decision admissible]
  (let [action (:action decision)
        score (:controller-score decision)
        law (:selection-law decision)
        reason (cond
                 (not (true? (:open? (missions/mission-status (:target action)))))
                 :target-not-open
                 (not (some #(= action (:action %)) admissible))
                 :action-not-admissible
                 (not (and (number? score) (Double/isFinite (double score))))
                 :controller-score-missing-or-invalid
                 (not (and (map? law) (keyword? (:applied law))))
                 :selection-law-missing-or-invalid)]
    (when reason
      (throw (ex-info "Controller decision cannot be authorized"
                      {:err :invalid-controller-authorization :reason reason
                       :target (:target action)})))
    (let [no-recall {:status :not-applicable
                     :reason :controller-decision-performs-no-memory-recall}
          actuation
          {:status :machine-authorized-bounded-autonomy
           :authorized? true :executed? false :authority :machine-determined
           :operator-confirmation-required? false
           :operator-decision-evidence-id operator-decision-evidence-id
           :admissible-set :all-open-missions :scope-authority scope-authority
           :machine-gates {:open-mission {:status :passed}
                           :admissible-action {:status :passed}
                           :controller-score-and-law {:status :passed}
                           :serving-cache no-recall :query-bounds no-recall
                           :tripwires {:status :required :owner :full-loop-runner
                                       :count 13}
                           :act-gate {:status :required :owner :full-loop-runner}
                           :stop-lines {:status :required :owner :full-loop-runner}}
           :delivery-qa {:required? true :endpoint "http://127.0.0.1:7070/api/alpha/morning-brief/addendum"}
           :rollback {:of "futon2 fa61e98f fixture-selector override"
                      :recorded-fallback "futon3c e74c7e7"}}]
      (assoc decision :actuation actuation
             :actuation-status (:status actuation) :actuation-authorized? true
             :requires-operator-override? false
             :strategic-selection-boundary :controller-over-all-open-missions
             :scope-authority scope-authority :rollback (:rollback actuation)))))
