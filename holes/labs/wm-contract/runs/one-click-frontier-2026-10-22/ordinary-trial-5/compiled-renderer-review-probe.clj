(require 'futon2.aif.cascade-plan)
(let [c {:construction-kind :compiled-cascade :operational-plan {:selected-cascade-digest "s" :compiled-cascade-digest "c" :steps [{}]}}]
(prn {:plan (futon2.aif.cascade-plan/compiled-operational-plan-text c) :criteria (futon2.aif.cascade-plan/compiled-completion-criteria-text c)}))
