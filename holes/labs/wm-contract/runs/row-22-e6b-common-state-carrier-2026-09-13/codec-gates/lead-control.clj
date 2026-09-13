(require '[futon2.aif.machine-slow-state-carrier :as c] '[futon2.aif.machine-slow-state-carrier-test :as t])
(let [b (#'t/bundle)]
 (doseq [[label edited] [[:missing-identity (assoc-in b [:proposal-evidence :identity] nil)]
                        [:borrowed-occurrence (assoc-in b [:proposal-evidence :transition/subject :candidate/occurrence-id] "borrowed")]]]
  (let [o (c/project-transition edited)]
   (assert (= :structurally-projected (:status o)))
   (prn {:control label :status (:status o) :original-context-unchanged (= (get-in b [:original-sources :context]) (get-in edited [:original-sources :context]))}))))
