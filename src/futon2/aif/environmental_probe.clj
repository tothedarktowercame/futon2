(ns futon2.aif.environmental-probe
  "One fresh environmental read and its append-only medic disposition."
  (:require [babashka.http-client :as http]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.out-of-band-fix :as out-of-band]))

(def receipt-schema :wm/environmental-probe-receipt-v1)
(def checked-medic-verifier
  "futon2.aif.environmental-probe/checked-medic-authority")

(def ^:dynamic *http-get*
  (fn [url opts] (http/get url opts)))

(defn- sha-bytes [^bytes bytes] (identity/sha256 bytes))
(defn- sha-text [text] (sha-bytes (.getBytes text "UTF-8")))

(defn- read-pin [pin]
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file (:path pin))))]
    (when-not (= (:sha256 pin) (sha-bytes bytes))
      (throw (ex-info "Environmental probe pin mismatch"
                      {:status :refused :reason :environmental-probe/pin-mismatch})))
    (edn/read-string (String. bytes "UTF-8"))))

(defn- environmental-classification? [x]
  (and (keyword? x) (str/starts-with? (name x) "environmental-")))

(defn- diagnosis-classification [diagnosis]
  (or (get-in diagnosis [:diagnosis :classification])
      (:classification diagnosis)))

(defn- diagnosis-injury-id [diagnosis]
  (or (get-in diagnosis [:diagnosis :injury-id]) (:injury-id diagnosis)))

(defn- diagnosis-source-pin [diagnosis]
  (or (get-in diagnosis [:diagnosis :source-record-pin])
      (:source-record-pin diagnosis)))

(defn- request-digest [request]
  (action-identity/digest [:environmental-probe-request request]))

(defn probe!
  "Re-run exactly one diagnosed environmental GET and retain its receipt.
  Returns a refusal before the HTTP port when the diagnosis is not environmental."
  [{:keys [acceptance-root injury-id cause-pin diagnosis-pin probed-by
           source-revision]}]
  (try
    (let [diagnosis (read-pin diagnosis-pin)
          classification (diagnosis-classification diagnosis)]
      (if-not (and (environmental-classification? classification)
                   (= injury-id (diagnosis-injury-id diagnosis))
                   (= cause-pin (diagnosis-source-pin diagnosis)))
        {:status :refused :reason :environmental-probe/classification-required}
        (let [cause (read-pin cause-pin)
              detail (get-in cause [:failure :detail])
              url (:url detail)
              method (keyword (str/lower-case (name (or (:method detail) :get))))
              deadline-ms (:timeout-ms detail)]
          (if-not (and (= :get method) (string? url) (not (str/blank? url))
                       (pos-int? deadline-ms))
            {:status :refused :reason :environmental-probe/request-unretained}
            (let [request {:method :get :url url :deadline-ms deadline-ms}
                  digest (request-digest request)
                  started (java.time.Instant/now)
                  start-ns (System/nanoTime)
                  result (try
                           (let [response (*http-get* url {:timeout deadline-ms
                                                          :throw false})
                                 status (:status response)
                                 body (or (:body response) "")]
                             (if (<= 200 (long (or status 0)) 299)
                               {:status :checked :http-status status
                                :response-digest (sha-text (str body))}
                               {:status :refused :http-status status
                                :typed-failure :environmental-probe/http-status}))
                           (catch java.net.http.HttpTimeoutException _
                             {:status :refused
                              :typed-failure :environmental-probe/timeout})
                           (catch Throwable t
                             {:status :refused
                              :typed-failure :environmental-probe/read-failed
                              :failure-class (.getName (class t))}))
                  ended (java.time.Instant/now)
                  elapsed-ms (long (Math/ceil (/ (- (System/nanoTime) start-ns)
                                                 1000000.0)))
                  result (if (> elapsed-ms deadline-ms)
                           {:status :refused
                            :typed-failure :environmental-probe/deadline-exceeded}
                           result)
                  receipt (merge {:schema receipt-schema :injury-id injury-id
                                  :cause-pin cause-pin :classification classification
                                  :evidence-pin diagnosis-pin
                                  :request request :request-digest digest
                                  :started-at (str started) :ended-at (str ended)
                                  :elapsed-ms elapsed-ms :probed-by probed-by
                                  :source-revision source-revision}
                                 result)
                  pin (out-of-band/retain! acceptance-root
                                           "environmental-probes" receipt)]
              {:status (:status receipt) :receipt receipt :receipt-pin pin})))))
    (catch clojure.lang.ExceptionInfo e
      (merge {:status :refused :reason :environmental-probe/input-refused}
             (ex-data e)))
    (catch Throwable t
      {:status :refused :reason :environmental-probe/input-unreadable
       :detail (ex-message t)})))

(defn- receipt-fields-match? [candidate receipt receipt-pin]
  (and (= (:injury-id receipt) (:injury-id candidate))
       (= (get-in receipt [:cause-pin :path]) (:cause-record-id candidate))
       (= (get-in receipt [:cause-pin :sha256]) (:cause-pin candidate))
       (= (:classification receipt) (:classification candidate))
       (= (:sha256 receipt-pin) (:probe-receipt-pin candidate))
       (= (:request-digest receipt) (:probe-request-digest candidate))
       (= (:status receipt) (:probe-status candidate))
       (= (:elapsed-ms receipt) (:probe-elapsed-ms candidate))
       (= (get-in receipt [:request :deadline-ms]) (:probe-deadline-ms candidate))
       (= (:ended-at receipt) (:probed-at candidate))
       (= (:sha256 (:evidence-pin receipt)) (:evidence-pin candidate))))

(defn checked-medic-authority
  "Authority for one exact persisted checked environmental probe."
  [expected-operation receipt-pin receipt]
  (fn [{:keys [operation candidate evidence]}]
    (try
      (let [persisted (read-pin receipt-pin)]
        (and (= receipt-schema (:schema receipt))
             (= :checked (:status receipt))
             (= receipt persisted)
             (= expected-operation operation)
             (= candidate evidence (get-in operation [:request :candidate]))
             (= receipt-pin (:authority-receipt-pin operation))
             (receipt-fields-match? candidate receipt receipt-pin)))
      (catch Throwable _ false))))

(defn retained-checked-medic-authority
  "Re-establish environmental probe authority from an operation's own pin."
  [{:keys [ledger operation candidate evidence]}]
  (try
    (let [pin (:authority-receipt-pin operation)
          receipt (read-pin pin)]
      (and (= :environmental-recovery (:kind candidate))
           (= :checked (:status receipt))
           (= candidate evidence (get-in operation [:request :candidate]))
           (= pin (:authority-receipt-pin operation))
           (= (action-identity/digest candidate) (:operation-id operation))
           (= (:ledger-pin ledger) (:expected-ledger-pin operation))
           (receipt-fields-match? candidate receipt pin)))
    (catch Throwable _ false)))

(defn discharge!
  "Append one checked environmental probe as a medic disposition."
  [{:keys [ledger-path injury-id receipt receipt-pin normal-authority]}]
  (if-not (= :checked (:status receipt))
    {:status :refused :reason :medic/environmental-probe-not-checked}
    (let [load-ledger (requiring-resolve
                       'futon2.aif.meta-injury-store/load-durable-ledger)
          transact (requiring-resolve
                    'futon2.aif.meta-injury-store/transact-medic-disposition!)
          loaded (load-ledger ledger-path normal-authority)]
      (if-not (= :loaded (:status loaded))
        loaded
        (let [history (:ledger loaded)
              active (ledger/effective-active-injuries history normal-authority)
              open-entry (some-> (get-in active [:injuries injury-id])
                                 (assoc :injury-id injury-id))]
          (if-not open-entry
            {:status :duplicate :reason :medic/environmental-already-recovered}
            (let [candidate {:kind :environmental-recovery
                             :injury-id injury-id
                             :entry-id (str (:ordinal open-entry))
                             :predecessor-entry-id nil
                             :patch-id (:source-revision receipt)
                             :reviewer "DarkTower.WarMachine.EnvironmentalProbe"
                             :evaluator checked-medic-verifier
                             :evidence-pin (get-in receipt [:evidence-pin :sha256])
                             :cause-record-id (get-in open-entry [:cause-record-pin :path])
                             :cause-pin (get-in open-entry [:cause-record-pin :sha256])
                             :classification (:classification receipt)
                             :probe-receipt-pin (:sha256 receipt-pin)
                             :probe-request-digest (:request-digest receipt)
                             :probe-status (:status receipt)
                             :probe-elapsed-ms (:elapsed-ms receipt)
                             :probe-deadline-ms (get-in receipt [:request :deadline-ms])
                             :probed-at (:ended-at receipt)}
                  request {:candidate candidate :evidence candidate}
                  operation {:operation-id (action-identity/digest candidate)
                             :expected-ledger-pin (:ledger-pin history)
                             :request request :authority-receipt-pin receipt-pin}
                  specific (checked-medic-authority operation receipt-pin receipt)
                  authority (out-of-band/scoped-medic-authority
                             normal-authority operation specific)
                  attribution {:medic-disposition :environmental-recovery
                               :fixed-by nil
                               :out-of-band-receipt-pin receipt-pin
                               :medic-stamp {:schema :wm/environmental-probe-medic-stamp-v1
                                             :verifier checked-medic-verifier
                                             :receipt-pin receipt-pin}}]
              (transact ledger-path receipt-pin operation attribution authority))))))))
