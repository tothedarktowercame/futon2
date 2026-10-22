(ns futon2.aif.wm.compiled-cascade-construction
  "Carry certified operational clauses into the ordinary build prompt and fold.
  Construction is a plan; it never asserts that its completion checks passed."
  (:require [futon2.aif.wm.executable-cascade :as cascade]))

(defn construct [entry compiled]
  (let [selected (:canonical-cascade entry)
        action (:action entry)
        nodes (:executable-nodes compiled)
        selected-check (cascade/validate-cascade selected)
        compiled-check (cascade/validate-cascade compiled)]
    (when-not (and (cascade/certified? selected-check)
                   (cascade/certified? compiled-check)
                   (= (:cascade-digest action) (:digest selected))
                   (= (:structural selected) (:structural compiled))
                   (seq nodes)
                   (every? #(= :enactable (:enactment-status %)) nodes))
      (throw (ex-info "Construction requires a fully authored refinement of the selected cascade"
                      {:kind :compiled-construction-invalid
                       :selected-validation selected-check
                       :compiled-validation compiled-check})))
    (let [steps (mapv #(select-keys % [:occurrence-id :pattern :provenance :clause]) nodes)
          shown (mapv :pattern nodes)
          boxes (mapv (fn [{:keys [occurrence-id pattern provenance clause]}]
                        {:id occurrence-id
                         :fits-pattern {:pattern/id pattern
                                        :pattern/revision (:source-sha256 provenance)}
                         :warrant-kind :pattern
                         :operation (:operation clause)
                         :capability (:capability clause)
                         :conditions [{:condition (str "Verify completion of " (:operation clause)
                                                       ": " (pr-str (:completion-check clause)))
                                       :status :unchecked
                                       :witness {:compiled-cascade (:digest compiled)
                                                 :occurrence-id occurrence-id
                                                 :authored-clause clause}}]}) steps)
          fold {:fold/route :certified-executable-cascade
                :wiring {:generated-by :certified-executable-cascade
                         :boxes boxes :wires (get-in compiled [:relations :application-order])}
                :coverage-score-delta nil :policy-holes []}]
      {:mission (:target action)
       :psi (str "execute reviewed cascade " (:id action))
       :construction-kind :compiled-cascade
       :selected-action action
       :shown shown
       :operational-plan {:selected-cascade-digest (:digest selected)
                          :compiled-cascade-digest (:digest compiled)
                          :steps steps
                          :application-order (get-in compiled [:relations :application-order])
                          :guards (get-in compiled [:relations :guards])
                          :effect-status :not-executed}
       :compiled-fold fold :policy-holes []})))
