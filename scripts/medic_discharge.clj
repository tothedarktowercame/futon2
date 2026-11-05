(ns scripts.medic-discharge
  "Dry-run/apply evidence-bearing discharges for the three legacy enacted
  occurrences covered by checked medic corrections."
  (:require [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.standing-authority :as standing]))

(def occurrences
  [["injury/cfbcbad062bde07d539c7e2abd97a6a189f910f4" 17]
   ["injury/04e14314a0eec3aab88d8e7044be26f3ae5dfe6d" 20]
   ["injury/94091b05988598abb17ce238088862c5d7f47201" 27]])

(defn- option [args flag default]
  (if-let [index (first (keep-indexed #(when (= flag %2) %1) args))]
    (nth args (inc index)) default))

(defn- row [occurrence result]
  (str (subs (first occurrence) 7 15) "@" (second occurrence) "\t"
       (if (= :appended (:status result))
         "would-discharge"
         (str "refused:" (name (or (:failed-check result) (:reason result)))))))

(defn -main [& args]
  (let [apply? (boolean (some #{"--apply"} args))
        ledger-path (option args "--ledger" (str store/default-root "/ledger.edn"))
        repository (option args "--repo" (System/getProperty "user.dir"))
        resolution (standing/standing-resolution)
        verifier (:verifier resolution)
        loaded (store/load-durable-ledger ledger-path verifier)]
    (when-not (= :loaded (:status loaded))
      (throw (ex-info "ledger load refused" loaded)))
    (let [results (reduce (fn [{:keys [ledger rows]} occurrence]
                            (let [result (ledger/append-medic-discharge
                                          ledger occurrence repository verifier)]
                              {:ledger (if (= :appended (:status result))
                                         (:ledger result) ledger)
                               :rows (conj rows (row occurrence result))}))
                          {:ledger (:ledger loaded) :rows []} occurrences)]
      (doseq [line (:rows results)] (println line))
      (when apply?
        (let [persisted (store/persist-ledger! ledger-path (:ledger results) verifier)]
          (when-not (= :persisted (:status persisted))
            (throw (ex-info "ledger persist refused" persisted)))
          (println "applied\t" (:ledger-pin persisted))))
      (shutdown-agents))))
