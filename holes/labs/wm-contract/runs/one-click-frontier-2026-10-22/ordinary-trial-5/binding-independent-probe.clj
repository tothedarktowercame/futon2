(load-file "test/futon2/aif/occurrence_declared_effect_binding_test.clj")
(require '[futon2.aif.occurrence-declared-effect-binding-test :as f]
         '[futon2.aif.occurrence-declared-effect-binding :as b])
(let [r (f/record-fixture)
      receipt (b/build-binding (f/inputs r))
      changed (assoc r :horizon {:status :absent} :wants [] :active-wants [])
      v (b/verify-binding {:occurrence f/occurrence :binding receipt :record changed
                           :record-verification (f/verification r)})]
 (prn {:changed-body-with-old-verification v
       :record-digest-stale (not= (:record/digest changed) (f/digest-record changed))
       :selected-mission-accepted-by-verifier? false
       :note "verify-binding has no selected-mission input; cannot check selection-cell target"}))
(shutdown-agents)
