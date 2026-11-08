(ns medic-0cb09c47-operation
  "Dry-run by default; --apply performs the explicit locked ledger transaction
  for injury 0cb09c47 (genuine repair, PROVIDER-FAIL-4a/4b/4c; Joe's one-off
  go, 2026-11-08).

  The cause: click wm-click-70f995eb, the wm-author contact
  invoke-1794040065486-15049-56005758 failed with Zai HTTP 429 code 1308 and
  was recorded :open-cascade-author-answer-invalid.  The replay feeds that
  retained job response, twice (the provider still failing on the retry),
  through the patched code: with-provider-retry -> provider-unavailable-refusal
  -> held-resume-data -> run-record-failure -> the terminal classifier's
  explicit non-injury table."
  (:require [cheshire.core :as json]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-terminal-injury :as terminal]
            [futon2.aif.out-of-band-fix :as out-of-band]
            [futon2.aif.repaired-elsewhere-medic :as repaired]
            [futon2.aif.standing-authority :as standing]))

(def injury-id "injury/0cb09c470db8ba94a5ea649c7ce1c17f24e982ae")
(def ledger-path
  "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22/ledger.edn")
(def author-job-pin
  {:path "/home/joe/notes/wm-next/tests/medic-0cb09c47/author-job-0cb09c47.json"
   :sha256 "3b224f6e2a20239bc83a13bf410e9afc964b42acaba81e21e482806cb164ee85"})
;; 4a (retry + typed refusal), 4b (non-injury entry), the 4c-review fix that
;; keeps the provider fields in the run record.
(def fix-commits ["3b9b3a9e0" "b5deb65f0" "9f4ecba94"])
(def repository "/home/joe/code/futon2")
(def receipt-root "/home/joe/notes/wm-next/tests/medic-0cb09c47")

(def healthy-condition
  {:op :path-equals :path [:replay]
   :expected {:terminal-failure-kind :open-cascade-provider-unavailable
              :non-injury :provider-unavailable
              :provider-class :usage-limit
              :seat "wm-author"}})

(defn- counts [projection]
  (assoc (select-keys projection [:status :raw-active-count :effective-active-count])
         :masked-count (count (:masked projection))))

(defn- author-response []
  (let [bs (java.nio.file.Files/readAllBytes
            (.toPath (java.io.File. ^String (:path author-job-pin))))]
    (when-not (= (:sha256 author-job-pin) (identity/sha256 bs))
      (throw (ex-info "Author job pin changed" {:pin author-job-pin})))
    (:job (json/parse-string (String. bs "UTF-8") true))))

(defn replay []
  (let [response (author-response)
        attempt (#'runtime/with-provider-retry
                 "wm-author" (fn [] {:response response :answer nil}))
        _ (when-not (:provider-stop attempt)
            (throw (ex-info "Patched retry did not stop typed" {:attempt attempt})))
        refusal (#'runtime/provider-unavailable-refusal "wm-author" attempt)
        failure (#'runner/run-record-failure
                 {:data (#'runner/held-resume-data
                         {:failure-kind (:kind refusal)
                          :downstream-refusal refusal}
                         "held" {:repair/id "medic-0cb09c47-replay"})})]
    {:terminal-failure-kind (:kind failure)
     :non-injury (get terminal/explicit-non-injury-kinds (:kind failure))
     :provider-class (get-in failure [:detail :provider-failure :class])
     :seat (get-in failure [:detail :seat])}))

(defn build-operation! [live-ledger]
  (let [current (ledger/medic-current-occurrence live-ledger injury-id)
        cause (#'repaired/read-pin {:path (:cause-record-id current)
                                    :sha256 (:cause-pin current)})
        rerun-value {:schema :wm/provider-unavailable-close-replay-v1
                     :patches fix-commits
                     :author-job-pin author-job-pin
                     :replay (replay)}
        _ (when-not (#'repaired/condition-holds? rerun-value healthy-condition)
            (throw (ex-info "Post-repair replay did not establish healthy condition"
                            {:rerun rerun-value :condition healthy-condition})))
        rerun-pin (repaired/retain-rerun! receipt-root rerun-value)
        commit-info (#'repaired/commit-evidence
                     repository fix-commits
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
    {:current current :rerun rerun-value :receipt-pin receipt-pin
     :candidate candidate :operation operation}))

(defn -main [& args]
  (let [apply? (= ["--apply"] (vec args))
        _ (when (and (seq args) (not apply?))
            (throw (ex-info "Usage: clojure -M scripts/medic_0cb09c47_operation.clj [--apply]"
                            {:args args})))
        normal-authority (:verifier (standing/standing-resolution))
        loaded (store/load-ledger ledger-path normal-authority)
        _ (when-not (= :loaded (:status loaded))
            (throw (ex-info "Live ledger did not validate" loaded)))
        live-ledger (:ledger loaded)
        {:keys [current rerun candidate operation receipt-pin]}
        (build-operation! live-ledger)
        record-pin {:path (:cause-record-id current) :sha256 (:cause-pin current)}
        specific (repaired/retained-authority
                  {:operation operation :candidate candidate :evidence candidate
                   :current current})
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
          :replay (:replay rerun)
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
          (System/exit 1))))))

(apply -main *command-line-args*)
