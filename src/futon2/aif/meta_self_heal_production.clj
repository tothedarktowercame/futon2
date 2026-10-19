(ns futon2.aif.meta-self-heal-production
  "Fail-closed serving arbitration for durable dedicated META repair."
  (:require [clojure.java.io :as io]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-repair-coordinator :as coordinator])
  (:import [java.nio.file Files StandardCopyOption]
           [java.nio.file.attribute FileAttribute]))

(def checkpoints [:claim :author :review :enact])
(def default-ledger-path
  "/home/joe/code/futon2/data/wm-meta-injuries/ledger.edn")
(def default-transaction-path
  "/home/joe/code/futon2/data/wm-meta-injuries/transaction.edn")
(def default-diagnosis-root
  "/home/joe/code/futon2/data/wm-meta-injuries/diagnoses")
(def default-acceptance-root
  "/home/joe/code/futon2/data/wm-meta-injuries/acceptances")
(defn- refusal [reason & [details]] (merge {:status :refused :reason reason} details))

(defn- receipt-bytes [receipt]
  (.getBytes (str (pr-str receipt) "\n") "UTF-8"))

(defn- diagnosis-path [root receipt]
  ;; Receipts are evidence handed to later repair coordination.  Their path is
  ;; content-addressed so a later diagnosis for the same injury cannot revoke
  ;; a previously returned pin by replacing its bytes.
  (str root "/diagnosis-" (load-identity/sha256 (receipt-bytes receipt)) ".edn"))

(defn- active-cause-pin
  "The active projection need not retain the opening record pin.  Recover it
  from the durable transition history rather than accepting a harness pin."
  [durable injury-id]
  (some->> (get-in durable [:ledger :entries])
           (filter #(and (= injury-id (:injury-id %))
                         (or (:new-cause-pin %) (:cause-record-pin %))))
           last
           (#(or (:new-cause-pin %) (:cause-record-pin %)))))

(defn- source-record [pin]
  (try
    (let [bytes (Files/readAllBytes (.toPath (io/file (:path pin))))]
      (if-not (= (:sha256 pin) (load-identity/sha256 bytes))
        (refusal :meta-self-heal/diagnosis-source-pin-mismatch)
        (let [parsed (store/read-one-form (String. bytes "UTF-8"))]
          (if (= :read (:status parsed))
            {:status :available :bytes bytes :record (:value parsed)}
            (refusal :meta-self-heal/diagnosis-source-unreadable
                     {:source-read parsed})))))
    (catch Throwable _
      (refusal :meta-self-heal/diagnosis-source-unreadable))))

(defn persist-diagnosis!
  "Atomically persist a successful diagnosis receipt.  A failed write never
  returns a pin, so callers cannot mistake an outbound answer for durable
  evidence."
  [path receipt]
  (try
    (let [target (.toPath (io/file path))
          parent (or (.getParent target) (.toPath (io/file ".")))
          bytes (receipt-bytes receipt)
          _ (Files/createDirectories parent (make-array FileAttribute 0))
          tmp (Files/createTempFile parent ".meta-diagnosis-" ".edn"
                                    (make-array FileAttribute 0))]
      (try
        (let [same-bytes? #(and (Files/isRegularFile target
                                                       (make-array java.nio.file.LinkOption 0))
                                (= (seq bytes) (seq (Files/readAllBytes target))))
              persisted {:status :persisted :diagnosis-evidence-pin
                         {:path path :sha256 (load-identity/sha256 bytes)}}]
          (cond
            (same-bytes?) persisted
            (Files/exists target (make-array java.nio.file.LinkOption 0))
            (refusal :meta-self-heal/diagnosis-path-conflict)
            :else
            (do
              (Files/write tmp bytes (make-array java.nio.file.OpenOption 0))
              (try
                (Files/move tmp target (into-array StandardCopyOption
                                                   [StandardCopyOption/ATOMIC_MOVE]))
                persisted
                (catch java.nio.file.FileAlreadyExistsException _
                  (if (same-bytes?) persisted
                      (refusal :meta-self-heal/diagnosis-path-conflict)))))))
        (finally (Files/deleteIfExists tmp))))
    (catch Throwable _
      (refusal :meta-self-heal/diagnosis-persist-failed))))

(defn produce-diagnosis!
  "Turn an active injury's exact opening-record pin into a diagnosis request.
  This is deliberately before, and separate from, repair coordination: it
  never claims a repair, and only exposes a diagnosis evidence pin after its
  receipt is durable.  An existing pin remains the compatibility path for
  already-produced diagnoses."
  [opts durable active-entry]
  (if-let [pin (:diagnosis-evidence-pin opts)]
    {:status :present :diagnosis-evidence-pin pin :source :existing}
    (let [injury-id (:injury-id active-entry)
          source-pin (active-cause-pin durable injury-id)
          diagnose (:meta-diagnosis-fn opts)]
      (cond
        (nil? source-pin)
        (refusal :meta-self-heal/diagnosis-source-pin-unavailable
                 {:injury-id injury-id :repair-claimed? false})

        (nil? diagnose)
        (refusal :meta-self-heal/diagnosis-port-unavailable
                 {:injury-id injury-id :source-record-pin source-pin
                  :repair-claimed? false})

        :else
        (let [source (source-record source-pin)]
          (if-not (= :available (:status source))
            (assoc source :injury-id injury-id :source-record-pin source-pin
                   :repair-claimed? false)
            (let [request {:schema :wm/meta-diagnosis-request-v1
                           :injury-id injury-id
                           :source-record-pin source-pin
                           :injury-record (:record source)
                           :repair-claimed? false}
                  response (try (diagnose request)
                                (catch Throwable _
                                  {:status :failed
                                   :reason :meta-self-heal/diagnosis-port-threw}))]
              (cond
                (nil? response)
                (refusal :meta-self-heal/diagnosis-port-returned-nil
                         {:injury-id injury-id :request request :repair-claimed? false})

                (not (map? response))
                (refusal :meta-self-heal/diagnosis-port-malformed
                         {:injury-id injury-id :request request :repair-claimed? false})

                (not= :succeeded (:status response))
                (refusal :meta-self-heal/diagnosis-unavailable
                         {:injury-id injury-id :request request
                          :diagnosis-status (:status response)
                          :diagnosis-reason (or (:reason response)
                                                :meta-self-heal/diagnosis-not-succeeded)
                          :repair-claimed? false})

                (not (map? (:diagnosis response)))
                (refusal :meta-self-heal/diagnosis-response-missing
                         {:injury-id injury-id :request request :repair-claimed? false})

                :else
                (let [receipt {:schema :wm/meta-diagnosis-receipt-v1
                               :status :available :injury-id injury-id
                               :source-record-pin source-pin :request request
                               :diagnosis (:diagnosis response)
                               :repair-claimed? false}
                      written (persist-diagnosis!
                               (diagnosis-path (or (:meta-diagnosis-root opts)
                                                   default-diagnosis-root)
                                               receipt)
                               receipt)]
                  (if (= :persisted (:status written))
                    (merge written {:status :available :injury-id injury-id
                                    :source-record-pin source-pin
                                    :request request :repair-claimed? false})
                    (assoc written :injury-id injury-id :request request
                           :repair-claimed? false)))))))))))

(defn- acceptance-path [root receipt]
  (str root "/acceptance-" (load-identity/sha256 (receipt-bytes receipt)) ".edn"))

(defn- pin? [pin]
  (and (map? pin) (string? (:path pin)) (seq (:path pin))
       (string? (:sha256 pin)) (boolean (re-matches #"[0-9a-f]{64}" (:sha256 pin)))))

(defn- read-diagnosis-receipt [diagnosis]
  (let [pin (:diagnosis-evidence-pin diagnosis)]
    (cond
      (not (pin? pin))
      (refusal :meta-self-heal/acceptance-diagnosis-pin-unavailable)

      :else
      (try
        (let [bytes (Files/readAllBytes (.toPath (io/file (:path pin))))]
          (cond
            (not= (:sha256 pin) (load-identity/sha256 bytes))
            (refusal :meta-self-heal/acceptance-diagnosis-pin-mismatch)

            :else
            (let [parsed (store/read-one-form (String. bytes "UTF-8"))
                  receipt (:value parsed)
                  expected (select-keys diagnosis [:injury-id :source-record-pin
                                                   :diagnosis-evidence-pin])
                  actual (select-keys receipt [:injury-id :source-record-pin])]
              (cond
                (not= :read (:status parsed))
                (refusal :meta-self-heal/acceptance-diagnosis-unreadable)
                (not= :wm/meta-diagnosis-receipt-v1 (:schema receipt))
                (refusal :meta-self-heal/acceptance-diagnosis-unreadable)
                (not= :available (:status receipt))
                (refusal :meta-self-heal/acceptance-diagnosis-unavailable)
                (not (map? (:diagnosis receipt)))
                (refusal :meta-self-heal/acceptance-diagnosis-unavailable)
                (not= (dissoc expected :diagnosis-evidence-pin) actual)
                (refusal :meta-self-heal/acceptance-diagnosis-identity-mismatch)
                :else {:status :available :receipt receipt :pin pin}))))
        (catch Throwable _
          (refusal :meta-self-heal/acceptance-diagnosis-unreadable))))))

(defn- concrete-acceptance? [case]
  (and (map? case) (string? (:id case)) (seq (:id case))
       (vector? (:command case)) (seq (:command case))
       (every? string? (:command case)) (contains? case :expected)))

(defn persist-acceptance!
  "Persist an immutable proposed specification.  It deliberately never runs
  the command or labels the specification accepted/passing."
  [path receipt]
  (try
    (let [target (.toPath (io/file path))
          parent (or (.getParent target) (.toPath (io/file ".")))
          bytes (receipt-bytes receipt)
          _ (Files/createDirectories parent (make-array FileAttribute 0))
          tmp (Files/createTempFile parent ".meta-acceptance-" ".edn"
                                    (make-array FileAttribute 0))]
      (try
        (let [same? #(and (Files/isRegularFile target
                                                 (make-array java.nio.file.LinkOption 0))
                          (= (seq bytes) (seq (Files/readAllBytes target))))
              persisted {:status :persisted :acceptance-evidence-pin
                         {:path path :sha256 (load-identity/sha256 bytes)}}]
          (cond
            (same?) persisted
            (Files/exists target (make-array java.nio.file.LinkOption 0))
            (refusal :meta-self-heal/acceptance-path-conflict)
            :else (do
                    (Files/write tmp bytes (make-array java.nio.file.OpenOption 0))
                    (try
                      (Files/move tmp target (into-array StandardCopyOption
                                                         [StandardCopyOption/ATOMIC_MOVE]))
                      persisted
                      (catch java.nio.file.FileAlreadyExistsException _
                        (if (same?) persisted
                            (refusal :meta-self-heal/acceptance-path-conflict)))))))
        (finally (Files/deleteIfExists tmp))))
    (catch Throwable _
      (refusal :meta-self-heal/acceptance-persist-failed))))

(defn produce-acceptance!
  "Request and retain a concrete, unreviewed acceptance specification for one
  verified diagnosis.  This is proposal production only: no returned command
  is executed and no repair or passing result is claimed."
  [opts diagnosis]
  (if-let [acceptance (:acceptance-test opts)]
    {:status :present :acceptance-test acceptance :source :supplied}
    (let [diagnosis-read (read-diagnosis-receipt diagnosis)
          accept (:meta-acceptance-fn opts)]
      (cond
        (not= :available (:status diagnosis-read))
        (assoc diagnosis-read :repair-claimed? false)

        (nil? accept)
        (refusal :meta-self-heal/acceptance-port-unavailable
                 {:injury-id (:injury-id diagnosis) :repair-claimed? false})

        :else
        (let [request {:schema :wm/meta-acceptance-request-v1
                       :injury-id (:injury-id diagnosis)
                       :source-record-pin (:source-record-pin diagnosis)
                       :diagnosis-evidence-pin (:diagnosis-evidence-pin diagnosis)
                       :diagnosis (:diagnosis (:receipt diagnosis-read))
                       :repair-claimed? false}
              response (try (accept request)
                            (catch Throwable _
                              {:status :failed
                               :reason :meta-self-heal/acceptance-port-threw}))]
          (cond
            (nil? response)
            (refusal :meta-self-heal/acceptance-port-returned-nil
                     {:request request :repair-claimed? false})
            (not (map? response))
            (refusal :meta-self-heal/acceptance-port-malformed
                     {:request request :repair-claimed? false})
            (not= :proposed (:status response))
            (refusal :meta-self-heal/acceptance-unavailable
                     {:request request :acceptance-status (:status response)
                      :acceptance-reason (:reason response) :repair-claimed? false})
            (not (concrete-acceptance? (:acceptance-test response)))
            (refusal :meta-self-heal/acceptance-prose-or-malformed
                     {:request request :repair-claimed? false})
            :else
            (let [receipt {:schema :wm/meta-acceptance-receipt-v1
                           :status :proposed :review-status :unreviewed
                           :result-status :not-run
                           :injury-id (:injury-id diagnosis)
                           :source-record-pin (:source-record-pin diagnosis)
                           :diagnosis-evidence-pin (:diagnosis-evidence-pin diagnosis)
                           :acceptance-test (:acceptance-test response)
                           :repair-claimed? false}
                  written (persist-acceptance!
                           (acceptance-path (or (:meta-acceptance-root opts)
                                                default-acceptance-root) receipt)
                           receipt)]
              (if (= :persisted (:status written))
                (merge written {:status :proposed :injury-id (:injury-id diagnosis)
                                :source-record-pin (:source-record-pin diagnosis)
                                :diagnosis-evidence-pin (:diagnosis-evidence-pin diagnosis)
                                :acceptance-test (:acceptance-test response)
                                :review-status :unreviewed :result-status :not-run
                                :repair-claimed? false})
                (assoc written :injury-id (:injury-id diagnosis)
                       :repair-claimed? false)))))))))
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

(defn diagnosis-context
  "Read the durable injury state once for runtime diagnosis preparation.  The
  context is evidence, not readiness: an unreadable ledger remains available
  to readiness as its original typed defect."
  [opts]
  (let [ledger-path (or (:meta-injury-ledger-path opts) default-ledger-path)
        durable (when (.isFile (io/file ledger-path))
                  (store/load-durable-ledger ledger-path))
        active (when (= :loaded (:status durable))
                 (ledger/active-injuries (:ledger durable)))]
    {:meta-injury-ledger-path ledger-path
     :durable-ledger durable
     :active-injuries active}))

(defn prepare-diagnosis
  "Prepare exactly the injury route that coordinator/prepare will select.  A
  supplied pin is intentionally not re-produced; no active injury is an
  explicit not-required observation."
  [opts {:keys [durable-ledger active-injuries]}]
  (cond
    (:diagnosis-evidence-pin opts)
    {:status :present :diagnosis-evidence-pin (:diagnosis-evidence-pin opts)
     :source :supplied}

    (nil? durable-ledger)
    {:status :not-required :reason :meta-self-heal/no-durable-injury-ledger}

    (not= :loaded (:status durable-ledger))
    {:status :unavailable :reason :meta-self-heal/diagnosis-ledger-unavailable}

    (not= :computed (:status active-injuries))
    {:status :unavailable :reason :meta-self-heal/diagnosis-active-set-unavailable}

    (empty? (:injuries active-injuries))
    {:status :not-required :reason :meta-self-heal/no-active-injury}

    :else
    (let [injury-id (first (sort (keys (:injuries active-injuries))))
          entry (assoc (get-in active-injuries [:injuries injury-id])
                       :injury-id injury-id)]
      (produce-diagnosis! opts durable-ledger entry))))

(defn production-readiness
  "Pre-ration readiness for the dedicated repair boundary.  An absent ledger
  is a valid empty state, but when a durable active injury exists every effect
  authority must already be configured.  Callers can therefore refuse before
  accepting or charging an ordinary click."
  ([opts] (production-readiness opts (diagnosis-context opts)))
  ([opts {:keys [meta-injury-ledger-path durable-ledger active-injuries]}]
  (let [ledger-path meta-injury-ledger-path
        transaction-path (or (:meta-repair-transaction-path opts)
                             default-transaction-path)
        durable durable-ledger
        active active-injuries
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
             :meta-repair-transaction-path transaction-path}))))

(defn- diagnosis-transaction-binding
  "A newly produced receipt belongs to one injury and one causal record.  Do
  not let a ledger change (or resumed transaction) repurpose its pin."
  [opts transaction]
  (let [observation (:meta-diagnosis-observation opts)
        work-order (:work-order transaction)]
    (if (not= :available (:status observation))
      {:status :valid :source :not-newly-produced}
      (let [expected {:injury-id (:injury-id observation)
                      :source-record-pin (:source-record-pin observation)
                      :diagnosis-evidence-pin (:diagnosis-evidence-pin observation)}
            actual {:injury-id (:injury-id work-order)
                    :source-record-pin (:cause-record-pin work-order)
                    :diagnosis-evidence-pin (:diagnosis-evidence-pin work-order)}]
        (if (= expected actual)
          {:status :valid :source :produced-diagnosis}
          (refusal :meta-self-heal/diagnosis-transaction-mismatch
                   {:diagnosis-observation observation
                    :expected expected :actual actual}))))))

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
                                                   durable (work-order-options opts))))
                    binding (diagnosis-transaction-binding opts transaction)]
                (if-not (= :valid (:status binding))
                  (assoc binding :route :dedicated-meta-self-heal
                         :ordinary-selection-called? false)
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
                       :transaction-write tw :transaction (:transaction result)}))))))))))))
