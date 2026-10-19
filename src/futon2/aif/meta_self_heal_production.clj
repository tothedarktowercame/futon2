(ns futon2.aif.meta-self-heal-production
  "Fail-closed serving arbitration for durable dedicated META repair."
  (:require [clojure.java.io :as io]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-repair-coordinator :as coordinator])
  (:import [java.nio.file Files StandardCopyOption]
           [java.nio.file.attribute FileAttribute]))

(def checkpoints [:claim :author :review :enact])
(def default-ledger-path
  "/home/joe/code/futon2/data/wm-meta-injuries/ledger.edn")
(def default-transaction-path
  "/home/joe/code/futon2/data/wm-meta-injuries/transaction.edn")
(defn- refusal [reason & [details]] (merge {:status :refused :reason reason} details))
(defn- persist-transaction! [path transaction]
  (try
    (if-not (= :valid (:status (coordinator/verify-transaction transaction)))
      (refusal :meta-self-heal/transaction-invalid)
      (let [target (.toPath (io/file path)) parent (.getParent target)
            _ (Files/createDirectories parent (make-array FileAttribute 0))
            tmp (Files/createTempFile parent ".meta-repair-" ".edn"
                                      (make-array FileAttribute 0))]
        (spit (.toFile tmp) (str (pr-str transaction) "\n"))
        (Files/move tmp target (into-array StandardCopyOption
                                           [StandardCopyOption/ATOMIC_MOVE
                                            StandardCopyOption/REPLACE_EXISTING]))
        {:status :persisted :path path
         :transaction-pin (:transaction-pin transaction)}))
    (catch Throwable t
      (refusal :meta-self-heal/transaction-persist-failed
               {:detail (ex-message t)}))))
(defn- load-transaction [path]
  (when (.isFile (io/file path))
    (let [read (store/read-one-form (slurp path))]
      (when (and (= :read (:status read))
                 (= :valid (:status (coordinator/verify-transaction (:value read)))))
        (:value read)))))

(defn agency-port [invoke seat]
  (fn [{:keys [work-order phase predecessor-pin transaction] :as request}]
    (let [session-key ({"wm-author" :author-session
                        "wm-reviewer" :reviewer-session} seat)
          ;; Enact/review adapters receive the sealed transaction too, so a
          ;; concrete serving boundary can bind its effect to the exact prior
          ;; author/reviewer evidence rather than reconstructing it.
          response (invoke {:seat seat :session-id (get work-order session-key)
                            :transaction transaction :request request})]
      (coordinator/seal-port-receipt
       {:phase phase :injury-id (:injury-id work-order)
        :repair-id (:repair-id work-order) :attempt (:attempt work-order)
        :predecessor-pin predecessor-pin :status (:status response)
        :payload (assoc (:payload response) :agency-job-id (:job-id response)
                        :agency-session-id (:session-id response)
                        :agency-seat seat)}))))
(defn endpoint-port [invoke adapter]
  (fn [{:keys [work-order phase predecessor-pin] :as request}]
    (let [response (invoke request)]
      (coordinator/seal-port-receipt
       {:phase phase :injury-id (:injury-id work-order)
        :repair-id (:repair-id work-order) :attempt (:attempt work-order)
        :predecessor-pin predecessor-pin :status (:status response)
        :payload (assoc (:payload response) :adapter adapter)}))))
(defn- effect-ports [opts]
  (or (:meta-repair-ports opts)
      (when (and (:agency-invoke-fn opts) (:repair-enact-fn opts)
                 (:repair-retry-fn opts))
        {:author (agency-port (:agency-invoke-fn opts) "wm-author")
         :review (agency-port (:agency-invoke-fn opts) "wm-reviewer")
         :enact (endpoint-port (:repair-enact-fn opts) "wm-repair-reviewer")
         :retry (endpoint-port (:repair-retry-fn opts) "wm-retry")})))
(defn- work-order-options [opts]
  (select-keys opts [:diagnosis-evidence-pin :acceptance-test :author :reviewer
                     :repair-reviewer :author-session :reviewer-session
                     :repair-reviewer-session :target-repository :target-worktree]))

(defn production-readiness
  "Pre-ration readiness for the dedicated repair boundary.  An absent ledger
  is a valid empty state, but when a durable active injury exists every effect
  authority must already be configured.  Callers can therefore refuse before
  accepting or charging an ordinary click."
  [opts]
  (let [ledger-path (or (:meta-injury-ledger-path opts) default-ledger-path)
        transaction-path (or (:meta-repair-transaction-path opts)
                             default-transaction-path)
        durable (when (.isFile (io/file ledger-path))
                  (store/load-durable-ledger ledger-path))
        active (when (= :loaded (:status durable))
                 (ledger/active-injuries (:ledger durable)))
        active? (seq (:injuries active))
        missing (cond-> []
                  (nil? transaction-path) (conj :transaction-path)
                  (and active? (nil? (effect-ports opts))) (conj :repair-ports)
                  (and active? (nil? (:diagnosis-evidence-pin opts)))
                  (conj :diagnosis-evidence)
                  (and active? (nil? (:acceptance-test opts)))
                  (conj :acceptance-test))]
    (cond
      (and durable (not= :loaded (:status durable)))
      (refusal :meta-self-heal/ledger-unreadable {:ledger-result durable})

      (seq missing)
      (refusal :meta-self-heal/production-config-required
               {:route :dedicated-meta-self-heal :absent-terms missing
                :ordinary-selection-called? false :before-ration-spend? true})

      :else {:status :ready :active-injury? (boolean active?)
             :meta-injury-ledger-path ledger-path
             :meta-repair-transaction-path transaction-path})))

(defn attributable-retry-readiness
  "Authorize the one ordinary retry named by the latest enacted transition.
   This is the only path allowed to run ordinary machinery while an injury is
   active; arbitrary maps cannot bypass repair arbitration."
  [opts retry]
  (let [ledger-path (or (:meta-injury-ledger-path opts) default-ledger-path)
        durable (when (.isFile (io/file ledger-path))
                  (store/load-durable-ledger ledger-path))
        entries (get-in durable [:ledger :entries])
        enacted (last (filter #(= :enacted (:state %)) entries))
        authored (last (filter #(and (= :authored (:state %))
                                     (= (:injury-id enacted) (:injury-id %))
                                     (= (:repair-id enacted) (:repair-id %))) entries))
        expected (select-keys enacted [:injury-id :repair-id :retry-click])
        supplied (select-keys retry [:injury-id :repair-id :retry-click])]
    (if (and (= :loaded (:status durable)) enacted authored
             (= expected supplied) (= (:patch-sha authored) (:patch-sha retry)))
      {:status :ready :attribution expected :patch-sha (:patch-sha retry)}
      (refusal :meta-self-heal/retry-attribution-refused
               {:expected (assoc expected :patch-sha (:patch-sha authored))
                :supplied (assoc supplied :patch-sha (:patch-sha retry))}))))

(defn route!
  "Nil means no active durable injury. A map consumes the opportunity."
  [opts]
  (let [ledger-path (or (:meta-injury-ledger-path opts) default-ledger-path)]
    (when (and ledger-path (.isFile (io/file ledger-path)))
      (let [durable (store/load-durable-ledger ledger-path)
            active (when (= :loaded (:status durable))
                     (ledger/active-injuries (:ledger durable)))]
        (when (seq (:injuries active))
          (let [transaction-path (:meta-repair-transaction-path opts)
                ports (effect-ports opts)
                missing (cond-> []
                          (nil? transaction-path) (conj :transaction-path)
                          (nil? ports) (conj :repair-ports)
                          (nil? (:diagnosis-evidence-pin opts)) (conj :diagnosis-evidence)
                          (nil? (:acceptance-test opts)) (conj :acceptance-test))]
            (if (seq missing)
              (refusal :meta-self-heal/production-config-required
                       {:route :dedicated-meta-self-heal :absent-terms missing
                        :ordinary-selection-called? false})
              (let [transaction (or (load-transaction transaction-path)
                                    (:transaction (coordinator/prepare
                                                   durable (work-order-options opts))))]
                (loop [durable durable transaction transaction
                       phases checkpoints receipts []]
                  (if-let [phase (first phases)]
                    (let [result (coordinator/coordinate
                                  durable transaction (assoc ports :stop-after phase))
                          lw (store/persist-ledger! ledger-path (:ledger result))
                          tw (persist-transaction! transaction-path (:transaction result))]
                      (if-not (and (= :persisted (:status lw))
                                   (= :persisted (:status tw)))
                        (refusal :meta-self-heal/checkpoint-persist-failed
                                 {:phase phase :ledger-write lw :transaction-write tw})
                        (recur (store/load-durable-ledger ledger-path)
                               (:transaction result) (next phases)
                               (conj receipts {:phase phase
                                               :ledger-pin (:ledger-pin (:ledger result))
                                               :transaction-pin
                                               (:transaction-pin (:transaction result))}))))
                    (let [result (coordinator/coordinate durable transaction ports)
                          lw (store/persist-ledger! ledger-path (:ledger result))
                          tw (persist-transaction! transaction-path (:transaction result))]
                      {:status (:status result) :route :dedicated-meta-self-heal
                       :ordinary-selection-called? false
                       :injury-id (get-in result [:transaction :work-order :injury-id])
                       :repair-id (get-in result [:transaction :work-order :repair-id])
                       :attempt (get-in result [:transaction :work-order :attempt])
                       :patch-sha (get-in result [:transaction :receipts 0 :payload :patch-sha])
                       :retry-click (get-in result [:transaction :receipts 2
                                                    :payload :retry-click])
                       :checkpoint-receipts receipts :ledger-write lw
                       :transaction-write tw :transaction (:transaction result)})))))))))))
