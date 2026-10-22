(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.meta-injury-store :as store]
         '[futon2.aif.meta-repair-coordinator :as coordinator]
         '[futon2.aif.load-identity :as identity])

;; Offline replay: no production ports and no production writes.
(let [root (or (first *command-line-args*)
               "holes/labs/wm-contract/runs/one-click-frontier-2026-10-22")
      read-local #(edn/read-string (slurp (io/file root %)))
      durable (store/load-durable-ledger (str (io/file root "pre-recovery-ledger.edn")))
      transaction (read-local "pre-recovery-transaction.edn")
      actual-after (read-local "post-recovery-ledger.edn")
      child-file (io/file root "recovery-child-bytes.edn")
      child (read-local "recovery-child-bytes.edn")
      ;; Retain the live observation's provenance while verifying its frozen bytes.
      live-observation (nth (:entries actual-after) 50)
      pin (:record-pin live-observation)
      _ (assert (= (:sha256 pin)
                   (identity/sha256 (java.nio.file.Files/readAllBytes (.toPath child-file)))))
      calls (atom [])
      forbidden (fn [& args]
                  (swap! calls conj args)
                  (throw (ex-info "Replay tried to actuate" {:args args})))
      ports {:author forbidden :review forbidden :enact forbidden :retry forbidden
             :retry-click-id (fn [& _] "offline-never-enqueued")}
      before (coordinator/coordinate durable transaction ports)
      after (coordinator/coordinate
             durable transaction
             (assoc ports :retry-click-status-fn
                    (fn [id]
                      (assert (= (:click-id child) id))
                      {:status (:status child) :result (:result child) :evidence-pin pin})))
      _ (assert (= :awaiting-retry (:status before)))
      _ (assert (= :abandoned (:status after)))
      _ (assert (= :retry-budget-spent (:reason after)))
      _ (assert (empty? @calls))
      project #(select-keys % [:ordinal :state :observation-kind :injury-id
                              :retry-click :record-pin :reason])
      reproduced (mapv project (take-last 2 (get-in after [:ledger :entries])))
      observed (mapv project (take-last 2 (:entries actual-after)))
      _ (assert (= observed reproduced))]
  (prn {:schema :wm/frozen-failed-child-replay-v1
        :before (:status before) :after (:status after) :reason (:reason after)
        :matches-live-transition true :actuation-calls (count @calls)
        :ordinary-task-complete false :entries reproduced}))
