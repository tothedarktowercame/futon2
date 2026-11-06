(ns medic-56278343-operation
  "Dry-run by default; --apply performs the explicit locked ledger transaction
  for injury 56278343 (genuine repair, CSB-CLOSE-1)."
  (:require [clojure.edn :as edn]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.out-of-band-fix :as out-of-band]
            [futon2.aif.repaired-elsewhere-medic :as repaired]
            [futon2.aif.standing-authority :as standing]))

(def injury-id "injury/562783432ea952bb5475898aaaf38100679f8109")
(def ledger-path
  "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22/ledger.edn")
(def record-pin
  {:path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-06-152a1490-23bd-4b2b-a7d3-0c4b86e1ec75.edn"
   :sha256 "4ee5e588d10d56973c8adc308c26e67681ce07c48d54bd86b6dd442e0c6aebcf"})
(def fix-commit "ab5246a71")
(def repository "/home/joe/code/futon2")
(def receipt-root "/home/joe/notes/wm-next/tests/medic-56278343")

(def healthy-condition
  {:op :path-equals :path [:replay]
   :expected {:close-outcome :substrate-unavailable
              :terminal-failure-kind :clock/store-busy}})

(defn- counts [projection]
  (assoc (select-keys projection [:status :raw-active-count :effective-active-count])
         :masked-count (count (:masked projection))))

(defn build-operation! [live-ledger]
  (let [current (ledger/medic-current-occurrence live-ledger injury-id)
        cause (#'repaired/read-pin record-pin)
        ;; Replay through the loaded runner: rebuild the recorded failure's
        ;; ex-data and ask the patched outcome-from for its close outcome.
        injured (edn/read-string (slurp (:path record-pin)))
        failure-kind (get-in injured [:failure :kind])
        replayed (#'runner/outcome-from
                  (ex-info "Agency clock store busy; dispatch not created"
                           {:outcome :clock-store-busy
                            :failure-kind failure-kind
                            :dispatch/not-created? true}))
        rerun-value {:schema :wm/clock-store-busy-close-replay-v1
                     :patch fix-commit
                     :replay {:close-outcome replayed
                              :terminal-failure-kind failure-kind}}
        _ (when-not (#'repaired/condition-holds? rerun-value healthy-condition)
            (throw (ex-info "Post-repair replay did not establish healthy condition"
                            {:rerun rerun-value :condition healthy-condition})))
        rerun-pin (repaired/retain-rerun! receipt-root rerun-value)
        commit-info (#'repaired/commit-evidence
                     repository [fix-commit]
                     (java.time.Instant/parse (:startedAt cause)))
        receipt {:schema repaired/receipt-schema
                 :status :checked
                 :injury-id injury-id
                 :occurrence (select-keys current
                                          [:entry-id :predecessor-entry-id
                                           :cause-record-id :cause-pin])
                 :expected-entry-pin (:entry-pin current)
                 :repository repository
                 :commits commit-info
                 :rerun-pin rerun-pin
                 :healthy-condition healthy-condition
                 :checked-at (str (java.time.Instant/now))
                 :ledger {:ledger-pin (:ledger-pin live-ledger)}}
        receipt-pin (out-of-band/retain! receipt-root
                                         "repaired-elsewhere-medic" receipt)
        candidate (#'repaired/make-candidate current receipt-pin receipt)
        request {:candidate candidate :evidence candidate}
        operation {:operation-id
                   (action-identity/digest
                    [:repaired-elsewhere-medic
                     (:ledger-pin live-ledger) candidate receipt-pin])
                   :expected-ledger-pin (:ledger-pin live-ledger)
                   :request request
                   :authority-receipt-pin receipt-pin}]
    {:current current :receipt receipt :receipt-pin receipt-pin
     :candidate candidate :operation operation}))

(defn -main [& args]
  (let [apply? (= ["--apply"] (vec args))
        _ (when (and (seq args) (not apply?))
            (throw (ex-info "Usage: clojure -M scripts/medic_56278343_operation.clj [--apply]"
                            {:args args})))
        normal-authority (:verifier (standing/standing-resolution))
        loaded (store/load-ledger ledger-path normal-authority)
        _ (when-not (= :loaded (:status loaded))
            (throw (ex-info "Live ledger did not validate" loaded)))
        live-ledger (:ledger loaded)
        {:keys [candidate operation receipt-pin]} (build-operation! live-ledger)
        specific (repaired/retained-authority
                  {:operation operation :candidate candidate :evidence candidate
                   :current (ledger/medic-current-occurrence live-ledger injury-id)})
        _ (when-not specific
            (throw (ex-info "Existing repaired-elsewhere verifier refused operation"
                            {:candidate candidate :receipt-pin receipt-pin})))
        verifier (out-of-band/scoped-medic-authority
                  normal-authority operation repaired/retained-authority)
        before (ledger/effective-active-injuries-from-validated-ledger live-ledger)
        dry-result (ledger/append-medic-repaired
                    live-ledger record-pin operation verifier)
        after (when (= :appended (:status dry-result))
                (ledger/effective-active-injuries-from-validated-ledger
                 (:ledger dry-result)))]
    (prn {:mode (if apply? :apply :dry-run)
          :candidate candidate
          :receipt-pin receipt-pin
          :medic-process-status (get-in dry-result [:admission :result :status])
          :append-status (:status dry-result)
          :before (counts before)
          :after (some-> after counts)})
    (when apply?
      (let [result (store/transact-medic-disposition!
                    ledger-path record-pin operation verifier)]
        (prn {:transaction-status (:status result)
              :durability (:durability result)
              :entry-pin (get-in result [:entry :entry-pin])})
        (when-not (= :appended (:status result))
          (System/exit 1))))
    (shutdown-agents)))

(apply -main *command-line-args*)
