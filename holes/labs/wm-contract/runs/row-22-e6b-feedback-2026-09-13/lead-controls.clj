(require '[futon2.aif.machine-slow-feedback-evidence :as e]
         '[futon2.aif.machine-slow-feedback-evidence-test :as fixture])
(doseq [[label mutate]
        [[:extra-conflicting-application
          (fn [rs] (update-in rs [:application-ledger :entries] conj
                     (assoc (first (get-in rs [:application-ledger :entries])) :application/id "apply-other")))]
         [:unresolved-authority-references
          (fn [rs]
            (let [changed (-> rs
                              (assoc-in [:e2b-subject :r9/authorization-ref] "nonexistent-r9")
                              (assoc-in [:outcome :outcome/authority-ref] "nonexistent-review"))]
              (-> changed
                  (assoc-in [:application-ledger :entries 0 :input/digests :e2b]
                            (#'fixture/vd (:e2b-subject changed)))
                  (assoc-in [:application-ledger :entries 0 :input/digests :outcome]
                            (#'fixture/vd (:outcome changed))))))]]]
 (let [out (e/verify-feedback (#'fixture/fixture mutate))]
  (prn {:control label :replay (:replay/identical? out) :application (:application out)})
  (assert (true? (:replay/identical? out)))))
