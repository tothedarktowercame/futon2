(ns futon2.aif.repair-reproduce
  "Reproduce one ledger-bound terminal occurrence without mutating its ledger.
  The only effect is reading the retained record bytes named by the caller."
  (:require [clojure.java.io :as io]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-repair-coordinator]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-terminal-injury :as terminal])
  (:import [java.nio.file Files]))

(def receipt-schema :wm/repair-reproduce-receipt-v1)

(def classifier-revision
  "The loaded Clojure Var carries no git identity, and deriving one at runtime
  would add repository I/O to this replay function."
  :unknown)

(def classifier-revision-reason
  :loaded-var-has-no-git-identity-and-runtime-repository-io-is-forbidden)

(defn- refusal [reason & [details]]
  (merge {:status :refused :reason reason} details))

(defn- occurrence [history injury-id]
  (first (filter #(and (= ledger/transition-schema (:schema %))
                       (= injury-id (:injury-id %))
                       (map? (:cause-record-pin %)))
                 (:entries history))))

(defn- append-transition [history transition]
  (let [result (ledger/append-transition history transition)]
    (if (= :appended (:status result))
      (:ledger result)
      (throw (ex-info "Synthetic enacted-retry history refused"
                      {:repair-reproduce/refusal :synthetic-history-invalid
                       :result result})))))

(defn- enacted-retry-history [classification]
  (let [injury-id "injury/repair-reproduce-control"
        repair-id "repair/repair-reproduce-control"
        click-id (:click-id classification)
        opened (append-transition
                ledger/empty-ledger
                {:injury-id injury-id :state :open :attempt 1
                 :cause-id "cause/repair-reproduce-control"
                 :cause-record-pin {:path "repair-reproduce-control/open.edn"
                                    :sha256 (apply str (repeat 64 "1"))}
                 :failure-stage :reviewer-wait
                 :failure-kind :reviewer-falsifier-failed
                 :detected-click "repair-reproduce-control/detect"})
        claimed (append-transition
                 opened
                 {:injury-id injury-id :state :claimed :attempt 1
                  :repair-id repair-id :algorithm "repair-reproduce-control"
                  :author-session "repair-reproduce-control/author"
                  :reviewer-session "repair-reproduce-control/reviewer"})
        authored (append-transition
                  claimed
                  {:injury-id injury-id :state :authored :attempt 1
                   :repair-id repair-id
                   :patch-sha (apply str (repeat 40 "2"))
                   :worktree "/repair-reproduce-control"})
        reviewed (append-transition
                  authored
                  {:injury-id injury-id :state :reviewed :attempt 1
                   :repair-id repair-id :verdict :approved
                   :reviewer-session "repair-reproduce-control/reviewer"
                   :reviewer-sha (apply str (repeat 40 "2"))})]
    (append-transition
     reviewed
     {:injury-id injury-id :state :enacted :attempt 1
      :repair-id repair-id
      :integration-commit (apply str (repeat 40 "3"))
      :retry-click click-id})))

(defn- seal-receipt [receipt]
  ;; Use the coordinator's underlying sealer so this grammar has its own tag,
  ;; logical path and pin key without pretending to be a coordinator port.
  (let [body (assoc receipt :schema receipt-schema)
        path (str "repair-reproduce-receipts/" (:injury-id body) "/"
                  (get-in body [:record-pin :sha256]) ".edn")
        coordinator-sealed (ns-resolve 'futon2.aif.meta-repair-coordinator 'sealed)]
    (coordinator-sealed :repair-reproduce-receipt path body :receipt-pin)))

(defn reproduce
  "Read and replay the retained terminal record bound to injury-id in ledger.
  Returns a sealed receipt or a typed refusal. Never writes either ledger."
  [{:keys [injury-id ledger record-path]}]
  (try
    (if-let [entry (occurrence ledger injury-id)]
      (let [pin (:cause-record-pin entry)]
        (cond
          (not= record-path (:path pin))
          (refusal :record-path-mismatch
                   {:injury-id injury-id :ledger-record-pin pin
                    :record-path record-path})

          (not (.isFile (io/file record-path)))
          (refusal :record-missing {:injury-id injury-id :record-path record-path})

          :else
          (let [bytes (Files/readAllBytes (.toPath (io/file record-path)))
                actual-sha (load-identity/sha256 bytes)]
            (if (not= actual-sha (:sha256 pin))
              (refusal :record-pin-mismatch
                       {:injury-id injury-id :record-pin pin :actual-sha256 actual-sha})
              (let [parsed (store/read-one-form (String. bytes "UTF-8"))]
                (if (not= :read (:status parsed))
                  (refusal :record-invalid {:injury-id injury-id :read-result parsed})
                  (let [classification (terminal/classify (:value parsed) pin)]
                    (if (not= :classified (:status classification))
                      (refusal :classification-refused
                               {:injury-id injury-id :classification classification})
                      (let [empty-result (terminal/integrate ledger/empty-ledger classification)
                            enacted-result (terminal/integrate
                                            (enacted-retry-history classification)
                                            classification)]
                        (seal-receipt
                         {:injury-id injury-id
                          :record-pin pin
                          :classifier-revision classifier-revision
                          :classifier-revision-reason classifier-revision-reason
                          :classification classification
                          :integrations {:empty-history empty-result
                                         :enacted-retry enacted-result}
                          :reproduced-as (if (= :injury (:classification classification))
                                           :injury :non-injury)}))))))))))
      (refusal :injury-occurrence-not-found {:injury-id injury-id}))
    (catch Throwable t
      (if-let [reason (:repair-reproduce/refusal (ex-data t))]
        (refusal reason {:detail (ex-data t)})
        (refusal :reproduce-failed {:detail (ex-message t)})))))
