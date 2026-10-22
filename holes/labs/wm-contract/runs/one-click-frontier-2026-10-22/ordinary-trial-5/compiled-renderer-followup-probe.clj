(require '[futon2.aif.cascade-plan :as p])
(let [base {:construction-kind :compiled-cascade :operational-plan {:selected-cascade-digest "s" :compiled-cascade-digest "c" :steps [{:occurrence-id ["p" 1] :clause {:operation :op :completion-check [:ok "check"]}}]}}
      malformed (assoc-in base [:operational-plan :steps 0 :clause :completion-check] false)
      huge (assoc-in base [:operational-plan :selected-cascade-digest] (apply str (repeat 20000 "x")))]
  (prn {:malformed-check (p/compiled-operational-plan-text malformed)
        :malformed-criteria (p/compiled-completion-criteria-text malformed)
        :huge-plan-length (count (:text (p/compiled-operational-plan-text huge)))
        :huge-criteria-length (count (p/compiled-completion-criteria-text huge))}))
