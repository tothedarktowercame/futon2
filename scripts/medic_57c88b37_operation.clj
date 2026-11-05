(ns medic-57c88b37-operation
  "Dry-run by default; --apply performs the explicit locked ledger transaction."
  (:require [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.ordinary-author-refusal :as refusal]
            [futon2.aif.standing-authority :as standing]))

(def injury-id "injury/57c88b37ed879eb3eccbdce1af3710aecd276a4e")
(def ledger-path "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22/ledger.edn")
(def record-pin
  {:path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-05-c226e5de-f597-45a0-b420-3219db50de94.edn"
   :sha256 "52dc5f8ae5033b401038339bd45f439828f6d3ca039831a28db200c5a40e9cc8"})
(def author-response-pin
  {:path "/home/joe/notes/wm-next/tests/occ195-author-job.json"
   :sha256 "0f5d1cc8cdd737858231238cf46a253cb7197eeec25f0412a83f09d43419e375"})
(def fix-commit "affc4c8c935bc1dbd62d032fa256c0de8b9b71c0")
(def receipt-path
  "/home/joe/notes/wm-next/tests/occ195-ordinary-author-refusal-receipt.edn")

(defn- write-receipt! [receipt]
  (let [bytes (.getBytes (str (pr-str receipt) "\n") "UTF-8")]
    (spit receipt-path (String. bytes "UTF-8"))
    {:path receipt-path :sha256 (identity/sha256 bytes)}))

(defn- counts [projection]
  (assoc (select-keys projection [:status :raw-active-count :effective-active-count])
         :masked-count (count (:masked projection))))

(defn build-operation! [live-ledger]
  (let [current (ledger/medic-current-occurrence live-ledger injury-id)
        candidate {:kind :ordinary-author-refusal
                   :injury-id injury-id
                   :entry-id (:entry-id current)
                   :predecessor-entry-id (:predecessor-entry-id current)
                   :patch-id fix-commit
                   :reviewer "claude-6"
                   :evaluator refusal/verifier-id
                   :evidence-pin (:sha256 author-response-pin)
                   :original-record-id (:path record-pin)
                   :original-pin (:sha256 record-pin)
                   :author-response-id (:path author-response-pin)
                   :author-response-pin (:sha256 author-response-pin)
                   :classifier-revision fix-commit
                   :replay :ordinary-author-refusal-non-injury}
        receipt {:schema refusal/receipt-schema
                 :verifier refusal/verifier-id
                 :candidate candidate
                 :record-pin record-pin
                 :author-response-pin author-response-pin
                 :fix-commit fix-commit
                 :replay-result :occurrence-square-author-response-invalid}
        receipt-pin (write-receipt! receipt)
        request {:candidate candidate :evidence candidate}
        operation {:operation-id (str "operation/"
                                      (identity/sha256
                                       (.getBytes
                                        (pr-str [:ordinary-author-refusal
                                                 request receipt-pin])
                                        "UTF-8")))
                   :expected-ledger-pin (:ledger-pin live-ledger)
                   :request request
                   :authority-receipt-pin receipt-pin}]
    {:candidate candidate :receipt receipt :receipt-pin receipt-pin
     :operation operation}))

(defn -main [& args]
  (let [apply? (= ["--apply"] (vec args))
        _ (when (and (seq args) (not apply?))
            (throw (ex-info "Usage: clojure -M scripts/medic_57c88b37_operation.clj [--apply]"
                            {:args args})))
        resolution (standing/standing-resolution)
        verifier (:verifier resolution)
        loaded (store/load-ledger ledger-path verifier)
        _ (when-not (= :loaded (:status loaded))
            (throw (ex-info "Live ledger did not validate" loaded)))
        live-ledger (:ledger loaded)
        {:keys [candidate receipt-pin operation]} (build-operation! live-ledger)
        before (ledger/effective-active-injuries-from-validated-ledger live-ledger)
        dry-result (ledger/append-medic-repaired live-ledger record-pin operation verifier)
        after (when (= :appended (:status dry-result))
                (ledger/effective-active-injuries-from-validated-ledger
                 (:ledger dry-result)))
        summary {:mode (if apply? :apply :dry-run)
                 :candidate candidate
                 :receipt-pin receipt-pin
                 :medic-process-status (get-in dry-result [:admission :result :status])
                 :append-status (:status dry-result)
                 :before (counts before)
                 :after (some-> after counts)}]
    (prn summary)
    (when apply?
      (let [result (store/transact-medic-disposition!
                    ledger-path record-pin operation verifier)]
        (prn {:transaction-status (:status result)
              :durability (:durability result)
              :entry-pin (get-in result [:entry :entry-pin])})
        (when-not (= :appended (:status result))
          (System/exit 1))))))

(apply -main *command-line-args*)
