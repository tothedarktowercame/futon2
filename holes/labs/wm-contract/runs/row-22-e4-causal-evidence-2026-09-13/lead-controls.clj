(require '[futon2.aif.scheduled-route-evidence :as e4]
         '[futon2.aif.scheduled-route-evidence-test :as fixture])
(let [base fixture/records
      controls {:missing-observation
                {:observations (mapv #(dissoc % :observation/channels) (:observations base))}
                :nil-actions
                (into {} (for [role [:predecessor-predictions :r8-occurrences]]
                           [role (mapv #(if (:rows %) (update % :rows (fn [rows] (mapv (fn [r] (dissoc r :action)) rows))) %) (get base role))]))
                :simultaneous-support-deletion
                (into {} (for [role [:predecessor-predictions :r8-occurrences]]
                           [role (mapv #(cond-> (assoc % :candidate/support ["candidate-a"])
                                         (:rows %) (update :rows (fn [rows] [(first rows)]))) (get base role))]))}]
  (doseq [[label overrides] controls]
    (let [result (e4/verify-route! (#'fixture/fixture overrides))]
      (prn {:control label :result result})
      (assert (= :verified-causal-route (:status result))))))
