(ns futon2.aif.meta-self-heal-production
  "Fail-closed serving arbitration for durable dedicated META repair."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
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

(def default-repository-root
  "/home/joe/code/futon2")

(def required-production-roles
  [:selection-entrypoint :required-loop-node-evaluator
   :terminal-routing-entrypoint])

(defn- resolvable-var? [var-name]
  (try
    (let [sym (symbol var-name)]
      (boolean (and (namespace sym) (requiring-resolve sym))))
    (catch Throwable _ false)))

(defn- repository-file? [root file]
  (try
    (let [root-file (.getCanonicalFile (io/file root))
          target (.getCanonicalFile (io/file root-file file))]
      (and (not (.isAbsolute (io/file file)))
           (.startsWith (.toPath target) (.toPath root-file))
           (.isFile target)))
    (catch Throwable _ false)))

(defn production-functions-defects
  "F11 (click wm-click-fca1c921, 2026-10-19): the acceptance author is asked
  for a test using only the production functions the diagnosis names, and
  the diagnosis never named any.  The author seat refused, typed
  `:kind :production-function-identity-absent`, \"diagnosis names production
  namespaces and record fields but no callable production functions\",
  `:missing {:callable-production-vars [:selection-entrypoint
  :required-loop-node-evaluator :terminal-routing-entrypoint]}`.  A diagnosis
  is therefore verified only when `:production-functions` names, for every
  required role, a var that resolves in this JVM and a file under ROOT.
  Returns nil when verified, else the typed defect lists."
  [root diagnosis]
  (let [entries (:production-functions diagnosis)
        entries (if (sequential? entries) entries [])
        well-formed? #(and (map? %) (keyword? (:role %))
                           (string? (:var %)) (not (str/blank? (:var %)))
                           (string? (:file %)) (not (str/blank? (:file %))))
        malformed (vec (remove well-formed? entries))
        entries (filter well-formed? entries)
        unresolvable (vec (distinct (remove resolvable-var? (map :var entries))))
        missing-files (vec (distinct (remove #(repository-file? root %)
                                             (map :file entries))))
        verified-roles (set (map :role (filter #(and (resolvable-var? (:var %))
                                                     (repository-file? root (:file %)))
                                               entries)))
        missing-roles (vec (remove verified-roles required-production-roles))]
    (when (or (not (sequential? (:production-functions diagnosis)))
              (seq malformed) (seq unresolvable) (seq missing-files)
              (seq missing-roles))
      (cond-> {:missing-roles missing-roles}
        (not (sequential? (:production-functions diagnosis)))
        (assoc :production-functions-field :absent)
        (seq malformed) (assoc :malformed-entries malformed)
        (seq unresolvable) (assoc :unresolvable-vars unresolvable)
        (seq missing-files) (assoc :missing-files missing-files)))))

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
                          ;; F9: the producer port's own detail (e.g. the
                          ;; transport's 429 text) travels onto the record.
                          :diagnosis-detail (:detail response)
                          :repair-claimed? false})

                (not (map? (:diagnosis response)))
                (refusal :meta-self-heal/diagnosis-response-missing
                         {:injury-id injury-id :request request :repair-claimed? false})

                (production-functions-defects
                 (or (:meta-repository-root opts) default-repository-root)
                 (:diagnosis response))
                (refusal :meta-self-heal/diagnosis-production-functions-absent
                         (merge (production-functions-defects
                                 (or (:meta-repository-root opts)
                                     default-repository-root)
                                 (:diagnosis response))
                                {:injury-id injury-id :request request
                                 :repair-claimed? false}))

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

(defn- read-diagnosis-receipt [root diagnosis]
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
                ;; F11: a receipt pinned before the contract (e.g.
                ;; diagnosis-83b348...) never reaches the author unverified.
                (production-functions-defects root (:diagnosis receipt))
                (refusal :meta-self-heal/diagnosis-production-functions-absent
                         (production-functions-defects root (:diagnosis receipt)))
                :else {:status :available :receipt receipt :pin pin}))))
        (catch Throwable _
          (refusal :meta-self-heal/acceptance-diagnosis-unreadable))))))

(defn- command-namespace [command]
  (some (fn [[flag value]] (when (= "-n" flag) value))
        (partition 2 1 command)))

(def ^:private namespace-segment-pattern
  ;; The same segment rule the futon3c run port applies when it derives the
  ;; file path (F6b): symbol-safe characters, no empty segment, no "." or
  ;; "..", no leading dash.  A namespace that fails it has no test path, so
  ;; the proposal is a source mismatch and never reaches a port.
  #"(?!-)[A-Za-z0-9_-]+")

(defn- namespace-test-path [namespace]
  (when (and (string? namespace) (not (str/blank? namespace)))
    (let [segments (str/split namespace #"\." -1)]
      (when (every? #(re-matches namespace-segment-pattern %) segments)
        (str "test/" (str/join "/" (map #(str/replace % "-" "_") segments))
             ".clj")))))

(defn- concrete-acceptance? [case]
  (let [source (:source case)]
    (and (map? case) (string? (:id case)) (not (str/blank? (:id case)))
         (vector? (:command case)) (seq (:command case))
         (every? string? (:command case)) (contains? case :expected)
         (map? source)
         (every? #(and (string? %) (not (str/blank? %)))
                 [(:namespace source) (:path source) (:text source)])
         (= (:namespace source) (command-namespace (:command case)))
         (= (:path source) (namespace-test-path (:namespace source))))))

(defn- source-shaped? [case]
  (let [source (:source case)]
    (and (map? source)
         (every? #(and (string? %) (not (str/blank? %)))
                 [(:namespace source) (:path source) (:text source)]))))

(defn- source-mismatch? [case]
  (and (source-shaped? case)
       (or (not= (get-in case [:source :namespace])
                 (command-namespace (:command case)))
           (not= (get-in case [:source :path])
                 (namespace-test-path (get-in case [:source :namespace]))))))

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

(def test-harness
  "The isolated real-input harness an acceptance test may use, given to the
  acceptance author seat.  Maintained by hand until a producer exists for it:
  F12 (replay 2, 2026-10-19 18:40Z, where the author seat proposed a test
  after refusing twice; click wm-click-fca1c921).  Each entrypoint is
  described at request time; the test-support namespace is on the test
  classpath, not the serving one, so it is recorded as unresolvable there."
  {:note "Isolated real-input harness the existing acceptance-style tests use (test classpath). The test may use it; nothing else is stubbed."
   :entrypoints
   [{:role :isolated-environment
     :var "futon2.aif.meta-self-heal-harness/prepare-environment!"
     :file "src/futon2/aif/meta_self_heal_harness.clj"}
    {:role :isolated-runner-options
     :var "futon2.test-support.runner-fixture/isolated-runner-opts"
     :file "test/futon2/test_support/runner_fixture.clj"}
    {:role :hermetic-traces
     :var "futon2.test-support.runner-fixture/with-hermetic-traces"
     :file "test/futon2/test_support/runner_fixture.clj"}
    {:role :loop-exercise-evidence
     :var "futon2.test-support.runner-fixture/with-loop-exercise-evidence"
     :file "test/futon2/test_support/runner_fixture.clj"}]
   :examples ["test/futon2/aif/meta_self_heal_production_test.clj"
              "test/futon2/aif/p37_reading_preparation_runtime_test.clj"
              "test/futon2/test_support/runner_fixture_test.clj"]})

(defn- resolve-var [var-name]
  (when (and (string? var-name) (str/includes? var-name "/"))
    (try (requiring-resolve (symbol var-name)) (catch Throwable _ nil))))

(defn- describe-function
  "Attach the resolved var's :arglists and :doc (at most 700 chars).  Returns
  the entry with :resolvable? false when the var does not resolve here."
  [entry]
  (if-let [v (resolve-var (:var entry))]
    (let [m (meta v) doc (:doc m)]
      (assoc entry :resolvable? true
             :arglists (pr-str (:arglists m))
             :doc (some-> doc (subs 0 (min 700 (count doc))))))
    (assoc entry :resolvable? false)))

(defn- harness-section []
  (update test-harness :entrypoints
          (fn [entries]
            (mapv (fn [e]
                    (let [d (describe-function e)]
                      (cond-> d
                        (not (:resolvable? d))
                        (assoc :note "not on the serving classpath (test-support namespace); read the file"))))
                  entries))))

(defn- request-enrichment
  "What the acceptance seat is given beyond the diagnosis: per production
  function its :arglists/:doc, the test harness, input authority, the
  persistence destination and the repository.  Mechanical: nothing here is
  asked of the seat.  Returns {:unresolvable [vars]} when a named function
  does not resolve."
  [opts diagnosis-map source-record-pin]
  (let [described (mapv describe-function (:production-functions diagnosis-map))
        missing (vec (keep #(when-not (:resolvable? %) (:var %)) described))]
    (if (seq missing)
      {:unresolvable missing}
      {:production-functions described
       :test-harness (harness-section)
       :real-input-authority {:source-record source-record-pin
                              :injury-ledger (or (:meta-injury-ledger-path opts)
                                                 default-ledger-path)
                              :note "read-only in the test; isolated copies via the harness"}
       :persistence-destination {:note "the test writes only under the harness's temporary roots; canonical data/ and src/ are read-only"}
       :repository (or (:meta-repository-root opts) default-repository-root)})))

(defn- producer-trace
  "What the producer did to get its answer, as record keys: F19's reader error
   and one informed re-ask (:read-retry), F9's one rate-limit retry
   (:transport-retry), F21's brace completion (:read-repair, click
   wm-click-4c3c26e2, 2026-10-20: the answer was one `}` short for the fourth
   time and the re-ask did not change that, so the reader completes it and
   says so), and F22's source re-ask (:source-retry, :source-read-error).
   Carried on every result of produce-acceptance-from-request!,
   proposed or refused; never into the content-addressed receipt."
  [response]
  (cond-> {}
    (contains? response :read-error)
    (assoc :acceptance-read-error (:read-error response))
    (contains? response :read-retry)
    (assoc :acceptance-read-retry (:read-retry response))
    (contains? response :transport-retry)
    (assoc :acceptance-transport-retry (:transport-retry response))
    (contains? response :read-repair)
    (assoc :acceptance-read-repair (:read-repair response))
    ;; F22a (click wm-click-79710180, 2026-10-20): the producer's one
    ;; informed re-ask when the proposed test source does not read
    ;; (:source-retry, with the first reader error and its line and column)
    ;; and the error that remained after it (:source-read-error).
    (contains? response :source-retry)
    (assoc :acceptance-source-retry (:source-retry response))
    (contains? response :source-read-error)
    (assoc :acceptance-source-read-error (:source-read-error response))))

(defn- produce-acceptance-from-request!
  "The port call and receipt half of produce-acceptance!, once every
  production function the diagnosis names has resolved."
  [opts diagnosis diagnosis-map enrichment accept]
  (let [request (merge {:schema :wm/meta-acceptance-request-v1
                        :injury-id (:injury-id diagnosis)
                        :source-record-pin (:source-record-pin diagnosis)
                        :diagnosis-evidence-pin (:diagnosis-evidence-pin diagnosis)
                        :diagnosis (cond-> diagnosis-map
                                     (contains? diagnosis-map :production-functions)
                                     (assoc :production-functions
                                            (:production-functions enrichment)))
                        :repair-claimed? false}
                       (dissoc enrichment :production-functions))
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
      ;; F10 (click wm-click-fca1c921, 2026-10-19): the producer's own
      ;; typed refusal (:failure with its kind, reason and :missing) and
      ;; a transport :detail travel onto the record; the refusal said
      ;; only :acceptance-unavailable before.
      ;; F20 (click wm-click-bf3c996a, 2026-10-20): the producer's reader
      ;; error (F19 :read-error), its one informed re-ask (:read-retry
      ;; with the first error and preview) and its one rate-limit retry
      ;; (F9 :transport-retry) were on the producer's answer but not
      ;; copied here, so the record showed a refused proposal and two
      ;; seat calls it could not explain.
      (refusal :meta-self-heal/acceptance-unavailable
               (merge {:request request :acceptance-status (:status response)
                       :acceptance-reason (:reason response)
                       :acceptance-failure (:failure response)
                       :acceptance-detail (:detail response)
                       :repair-claimed? false}
                      (producer-trace response)))
      (source-mismatch? (:acceptance-test response))
      (refusal :meta-self-heal/acceptance-proposal-source-mismatch
               (merge {:request request :repair-claimed? false}
                      (producer-trace response)))
      (not (concrete-acceptance? (:acceptance-test response)))
      (refusal :meta-self-heal/acceptance-prose-or-malformed
               (merge {:request request :repair-claimed? false}
                      (producer-trace response)))
      :else
      (let [receipt {:schema :wm/meta-acceptance-receipt-v1
                     :status :proposed :review-status :unreviewed
                     :result-status :not-run
                     :injury-id (:injury-id diagnosis)
                     :source-record-pin (:source-record-pin diagnosis)
                     :diagnosis-evidence-pin (:diagnosis-evidence-pin diagnosis)
                     :request-enrichment enrichment
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
                          :repair-claimed? false}
                 (producer-trace response))
          (merge (assoc written :injury-id (:injury-id diagnosis)
                        :repair-claimed? false)
                 (producer-trace response)))))))

(defn produce-acceptance!
  "Request and retain a concrete, unreviewed acceptance specification for one
  verified diagnosis.  This is proposal production only: no returned command
  is executed and no repair or passing result is claimed.  Historical inputs
  acceptance-25481c81... and acceptance-405aaeb0... were specifications
  without code; source is now part of the content-addressed receipt.
  F12 (click wm-click-fca1c921; replay 2, 2026-10-19 18:40Z): the request
  binds what a no-stub test needs, mechanically (see request-enrichment),
  and the receipt keeps it verbatim as :request-enrichment.  A diagnosis
  function that does not resolve in this JVM is a typed refusal."
  [opts diagnosis]
  (if-let [acceptance (:acceptance-test opts)]
    {:status :present :acceptance-test acceptance :source :supplied}
    (let [diagnosis-read (read-diagnosis-receipt
                          (or (:meta-repository-root opts) default-repository-root)
                          diagnosis)
          accept (:meta-acceptance-fn opts)]
      (cond
        (not= :available (:status diagnosis-read))
        (assoc diagnosis-read :repair-claimed? false)

        (nil? accept)
        (refusal :meta-self-heal/acceptance-port-unavailable
                 {:injury-id (:injury-id diagnosis) :repair-claimed? false})

        :else
        (let [diagnosis-map (:diagnosis (:receipt diagnosis-read))
              enrichment (request-enrichment opts diagnosis-map
                                             (:source-record-pin diagnosis))]
          (if (:unresolvable enrichment)
            (refusal :meta-self-heal/acceptance-request-function-unresolvable
                     {:injury-id (:injury-id diagnosis)
                      :unresolvable (:unresolvable enrichment)
                      :repair-claimed? false})
            (produce-acceptance-from-request! opts diagnosis diagnosis-map
                                              enrichment accept)))))))

(defn- read-acceptance-proposal [proposal]
  (let [pin (:acceptance-evidence-pin proposal)]
    (if-not (pin? pin)
      (refusal :meta-self-heal/acceptance-proposal-pin-unavailable)
      (try
        (let [bytes (Files/readAllBytes (.toPath (io/file (:path pin))))]
          (if-not (= (:sha256 pin) (load-identity/sha256 bytes))
            (refusal :meta-self-heal/acceptance-proposal-pin-mismatch)
            (let [parsed (store/read-one-form (String. bytes "UTF-8"))
                  receipt (:value parsed)
                  expected (select-keys proposal [:injury-id :source-record-pin
                                                   :diagnosis-evidence-pin :acceptance-test])]
              (cond
                (not= :read (:status parsed))
                (refusal :meta-self-heal/acceptance-proposal-unreadable)
                (not= :wm/meta-acceptance-receipt-v1 (:schema receipt))
                (refusal :meta-self-heal/acceptance-proposal-unreadable)
                (not= :proposed (:status receipt))
                (refusal :meta-self-heal/acceptance-proposal-not-proposed)
                (not= :unreviewed (:review-status receipt))
                (refusal :meta-self-heal/acceptance-proposal-review-state-invalid)
                (not= expected (select-keys receipt (keys expected)))
                (refusal :meta-self-heal/acceptance-proposal-identity-mismatch)
                (source-mismatch? (:acceptance-test receipt))
                (refusal :meta-self-heal/acceptance-proposal-source-mismatch)
                (not (concrete-acceptance? (:acceptance-test receipt)))
                (refusal :meta-self-heal/acceptance-proposal-not-concrete)
                :else {:status :available :pin pin :receipt receipt}))))
        (catch Throwable _
          (refusal :meta-self-heal/acceptance-proposal-unreadable))))))

(defn admit-acceptance!
  "Run, then independently review, one immutable concrete proposal.  The
  machine-run result is evidence shown to the reviewer; no prose, stale pin,
  or response for different diagnosis/source/proposal identity can become
  coordinator authority.  This is the corrected contract exposed by
  wm-click-6cdf2a7c: at 17:37:37Z the reviewer correctly refused the former
  `:result-status :not-run` request with
  `:reason/type :acceptance-test-not-run`."
  [opts proposal]
  (let [proposal-read (read-acceptance-proposal proposal)
        run-acceptance (:meta-acceptance-run-fn opts)
        review (:meta-acceptance-review-fn opts)]
    (cond
      (not= :available (:status proposal-read))
      (assoc proposal-read :repair-claimed? false)
      (not (ifn? run-acceptance))
      (refusal :meta-self-heal/acceptance-run-port-unavailable
               {:injury-id (:injury-id proposal) :repair-claimed? false})
      (not (ifn? review))
      (refusal :meta-self-heal/acceptance-review-port-unavailable
               {:injury-id (:injury-id proposal) :repair-claimed? false})
      :else
      (let [run-result (try (run-acceptance (:acceptance-test proposal))
                            (catch Throwable t
                              {:status :failed
                               :reason :meta-self-heal/acceptance-run-threw
                               :detail (ex-message t)}))
            valid-run? (and (map? run-result)
                            (= :ran (:status run-result))
                            (contains? #{:failed :passed :errored}
                                       (:outcome run-result))
                            (integer? (:exit run-result))
                            (string? (:summary run-result))
                            (pin? (:run-pin run-result)))
            request {:schema :wm/meta-acceptance-review-request-v1
                     :injury-id (:injury-id proposal)
                     :source-record-pin (:source-record-pin proposal)
                     :diagnosis-evidence-pin (:diagnosis-evidence-pin proposal)
                     :proposal-evidence-pin (:acceptance-evidence-pin proposal)
                     :acceptance-test (:acceptance-test proposal)
                     :result-status :ran-before-repair
                     :result run-result :repair-claimed? false}
            ;; F16 (off-click replay 5, 2026-10-19): a proposal whose run
            ;; PASSES before any repair does not pin the injury; it is refused
            ;; here, typed, without spending a reviewer invoke.
            passes-before-repair? (and valid-run? (= :passed (:outcome run-result)))
            response (when (and valid-run? (not passes-before-repair?))
                       (try (review request)
                          (catch Throwable _ {:status :failed
                                               :reason :meta-self-heal/acceptance-review-threw})))
            identity-keys [:injury-id :source-record-pin :diagnosis-evidence-pin
                           :proposal-evidence-pin :acceptance-test]]
        (cond
          (not valid-run?)
          (refusal :meta-self-heal/acceptance-run-refused
                   {:request request :acceptance-result run-result
                    :repair-claimed? false})
          passes-before-repair?
          (refusal :meta-self-heal/acceptance-test-passes-before-repair
                   {:request request :acceptance-result run-result
                    :repair-claimed? false})
          (not (map? response))
          (refusal :meta-self-heal/acceptance-review-malformed {:request request})
          (not= :accepted (:status response))
          (refusal :meta-self-heal/acceptance-review-refused
                   {:request request :review-status (:status response)
                    :review-reason (or (:reason response)
                                       (:reason/type response))
                    ;; F17 (chain 7, 2026-10-19 19:17Z): the adapter keeps the
                    ;; transport's error as :detail (F9); this record dropped it,
                    ;; so a reviewer rate-limit read only as producer-invoke-failed.
                    :review-detail (:detail response)})
          (not= (select-keys request identity-keys)
                (select-keys response identity-keys))
          (refusal :meta-self-heal/acceptance-review-identity-mismatch {:request request})
          :else
          (let [receipt {:schema :wm/meta-acceptance-admission-v1
                         :status :accepted :review-status :accepted
                         :result-status :ran-before-repair
                         :result run-result
                         :injury-id (:injury-id proposal)
                         :source-record-pin (:source-record-pin proposal)
                         :diagnosis-evidence-pin (:diagnosis-evidence-pin proposal)
                         :proposal-evidence-pin (:acceptance-evidence-pin proposal)
                         :acceptance-test (:acceptance-test proposal)
                         :review response :repair-claimed? false}
                written (persist-acceptance!
                         (acceptance-path (or (:meta-acceptance-root opts)
                                              default-acceptance-root) receipt)
                         receipt)]
            (if (= :persisted (:status written))
              (merge written (select-keys receipt [:injury-id :source-record-pin
                                                    :diagnosis-evidence-pin
                                                    :proposal-evidence-pin
                                                    :acceptance-test :review-status
                                                    :result-status :result
                                                    :repair-claimed?])
                     {:status :accepted :acceptance-admission-pin
                      (:acceptance-evidence-pin written)})
              (assoc written :injury-id (:injury-id proposal)
                     :repair-claimed? false))))))))

(defn prepare-acceptance
  "Runtime-facing proposal preparation.  A proposed/unreviewed specification
  remains observation only; this function never returns it as coordinator
  :acceptance-test authority."
  [opts diagnosis-observation]
  (cond
    (:acceptance-test opts)
    {:status :present :acceptance-test (:acceptance-test opts) :source :supplied}

    (= :available (:status diagnosis-observation))
    (let [proposal (produce-acceptance! opts diagnosis-observation)]
      (if (= :proposed (:status proposal))
        (admit-acceptance! opts proposal)
        proposal))

    (= :not-required (:status diagnosis-observation))
    {:status :not-required :reason :meta-self-heal/acceptance-no-diagnosis-required}

    :else
    {:status :unavailable :reason :meta-self-heal/acceptance-diagnosis-unavailable
     :diagnosis-status (:status diagnosis-observation)
     :repair-claimed? false}))
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

(defn agency-port
  "ROLE is :author or :reviewer. The seat invoked is the one the sealed work
  order names for that role (click wm-click-36b47627, 2026-10-19: the seat
  names were fixed here as wm-author / wm-reviewer, so a click cast on other
  seats would still have dispatched its repair phases to the defaults)."
  [invoke role]
  (fn [{:keys [work-order phase predecessor-pin transaction] :as request}]
    (let [seat (get work-order role)
          session-key ({:author :author-session
                        :reviewer :reviewer-session} role)
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
        (cond-> {:author (agency-port (:agency-invoke-fn opts) :author)
                 :review (agency-port (:agency-invoke-fn opts) :reviewer)
                 :enact (endpoint-port (:repair-enact-fn opts) "wm-repair-reviewer")
                 :retry (endpoint-port (:repair-retry-fn opts) "wm-retry")}
          ;; M10b-3 (2026-10-20): the futon3c adapter exposes its pure click
          ;; minting function as :retry-click-id-fn; the coordinator reads it
          ;; as :retry-click-id when it re-queues an inconclusive retry. Its
          ;; absence is not a port defect here: the coordinator refuses
          ;; :coordinator/retry-click-id-unavailable only at the requeue.
          (fn? (:retry-click-id-fn opts))
          (assoc :retry-click-id (:retry-click-id-fn opts))))))
(defn latest-retry-click
  "The retry click the transaction currently names. Receipts are keyed by
   phase, not by index: after author, review, enact and retry, each requeue
   of an inconclusive retry appends a :retry-requeue-<n> receipt (M10b-3b),
   so the click at receipt index 2 (the enact receipt's minted click) goes
   stale on the first requeue. Read the last retry-family receipt's click,
   else the enact receipt's, else nil."
  [transaction]
  (let [receipts (:receipts transaction)
        phase-name #(when (keyword? (:phase %)) (name (:phase %)))
        retry? #(let [n (phase-name %)]
                  (and n (or (= "retry" n) (str/starts-with? n "retry-requeue-"))))]
    (or (get-in (last (filter retry? receipts)) [:payload :retry-click])
        (get-in (some #(when (= :enact (:phase %)) %) receipts)
                [:payload :retry-click]))))
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
  explicit not-required observation.  The read decides first (SelfHealRoute
  rev 6, ledgerRead): a loaded ledger whose active set did not compute is
  unavailable before a supplied pin can short-circuit (M8R finding 15,
  2026-10-20), so run-opportunity! ends the click there under a pin too."
  [opts {:keys [durable-ledger active-injuries]}]
  (cond
    (and (= :loaded (:status durable-ledger))
         (not= :computed (:status active-injuries)))
    {:status :unavailable :reason :meta-self-heal/diagnosis-active-set-unavailable}

    (:diagnosis-evidence-pin opts)
    {:status :present :diagnosis-evidence-pin (:diagnosis-evidence-pin opts)
     :source :supplied}

    (nil? durable-ledger)
    {:status :not-required :reason :meta-self-heal/no-durable-injury-ledger}

    (not= :loaded (:status durable-ledger))
    {:status :unavailable :reason :meta-self-heal/diagnosis-ledger-unavailable}

    (empty? (:injuries active-injuries))
    {:status :not-required :reason :meta-self-heal/no-active-injury}

    :else
    (let [injury-id (first (sort (keys (:injuries active-injuries))))
          entry (assoc (get-in active-injuries [:injuries injury-id])
                       :injury-id injury-id)]
      (produce-diagnosis! opts durable-ledger entry))))

(defn production-readiness
  "Pre-ration readiness for the repair boundary.

  The ordinary route retains unavailable dedicated self-heal machinery as an
  observation, not an ordinary-selection veto. The dedicated route remains
  fail-closed without its required inputs."
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
                  (and active? (not (ifn? (:meta-acceptance-run-fn opts))))
                  (conj :acceptance-run-port)
                  (and active? (nil? (:diagnosis-evidence-pin opts))
                       (not (ifn? (:meta-diagnosis-fn opts))))
                  (conj :diagnosis-port)
                  (and active? (nil? (:acceptance-test opts))
                       (not (ifn? (:meta-acceptance-fn opts))))
                  (conj :acceptance-proposal-port)
                  (and active? (nil? (:acceptance-test opts))
                       (not (ifn? (:meta-acceptance-review-fn opts))))
                  (conj :acceptance-review-port))
        self-heal-absence (vec (filter #{:diagnosis-port
                                         :acceptance-run-port
                                         :acceptance-proposal-port
                                         :acceptance-review-port}
                                       missing))]
    (cond
      (and durable (not= :loaded (:status durable)))
      (refusal :meta-self-heal/ledger-unreadable {:ledger-result durable})

      (and (= :ordinary (:meta-self-heal/readiness-route opts))
           (seq self-heal-absence))
      {:status :ready :active-injury? true
       :meta-injury-ledger-path ledger-path
       :meta-repair-transaction-path transaction-path
       :self-heal {:status :absent :absent-terms self-heal-absence}}

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

(defn- acceptance-transaction-binding
  "An admitted proposal cannot be repurposed by a later transaction.  The
  coordinator receives only the concrete test, so bind it back to the retained
  admission observation immediately before effect coordination."
  [opts transaction]
  (let [observation (:meta-acceptance-observation opts)
        work-order (:work-order transaction)]
    (cond
      (= :present (:status observation)) {:status :valid :source :supplied}
      (not= :accepted (:status observation))
      (refusal :meta-self-heal/acceptance-transaction-unadmitted
               {:acceptance-observation observation})
      :else
      (let [expected (select-keys observation [:injury-id :source-record-pin
                                                :diagnosis-evidence-pin
                                                :acceptance-test])
            actual {:injury-id (:injury-id work-order)
                    :source-record-pin (:cause-record-pin work-order)
                    :diagnosis-evidence-pin (:diagnosis-evidence-pin work-order)
                    :acceptance-test (:acceptance-test work-order)}]
        (if (= expected actual)
          {:status :valid :source :admitted-acceptance}
          (refusal :meta-self-heal/acceptance-transaction-mismatch
                   {:acceptance-observation observation
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
  (let [ledger-path (or (:meta-injury-ledger-path opts) default-ledger-path)
        context (:meta-diagnosis-context opts)
        durable (if context (:durable-ledger context)
                    (when (and ledger-path (.isFile (io/file ledger-path)))
                      (store/load-durable-ledger ledger-path)))
        active (if context (:active-injuries context)
                   (when (= :loaded (:status durable)) (ledger/active-injuries (:ledger durable))))]
    (when durable
      (if (and (= :loaded (:status durable)) (not= :computed (:status active)))
        (refusal :meta-self-heal/active-set-unavailable
                 {:route :dedicated-meta-self-heal :ordinary-selection-called? false
                  :active-injuries (select-keys active [:status :reason :defects])})
        (when (seq (:injuries active))
          (let [transaction-path (:meta-repair-transaction-path opts)
                ports (effect-ports opts)
                missing (cond-> []
                          (nil? transaction-path) (conj :transaction-path)
                          (nil? ports) (conj :repair-ports)
                          (not (ifn? (:meta-acceptance-run-fn opts)))
                          (conj :acceptance-run-port)
                          (nil? (:diagnosis-evidence-pin opts)) (conj :diagnosis-evidence)
                          (nil? (:acceptance-test opts)) (conj :acceptance-test))]
            (if (seq missing)
              (refusal :meta-self-heal/production-config-required
                       {:route :dedicated-meta-self-heal :absent-terms missing
                        :ordinary-selection-called? false})
              (let [resumed (load-transaction transaction-path)
                    prepared (when-not resumed
                               (coordinator/prepare durable (work-order-options opts)))
                    transaction (or resumed (:transaction prepared))
                    ;; Click wm-click-c78d0f27 (2026-10-19): a refused prepare
                    ;; returns no transaction, and comparing the diagnosis
                    ;; observation with an absent work order reported
                    ;; diagnosis-transaction-mismatch for a defect that was not
                    ;; a mismatch. The refusal of preparation is its own reason;
                    ;; no binding check runs without a transaction.
                    diagnosis-binding (if (nil? transaction)
                                        (refusal :meta-self-heal/preparation-refused
                                                 {:prepare (select-keys prepared
                                                                        [:status :reason
                                                                         :defects :detail])})
                                        (diagnosis-transaction-binding opts transaction))
                    acceptance-binding (when (= :valid (:status diagnosis-binding))
                                         (acceptance-transaction-binding opts transaction))
                    binding (or (when-not (= :valid (:status diagnosis-binding)) diagnosis-binding)
                                acceptance-binding)]
                (if-not (= :valid (:status binding))
                  (assoc binding :route :dedicated-meta-self-heal
                         :ordinary-selection-called? false)
                  ;; Review fix for dfe565350 (claude-5, 2026-10-20): the
                  ;; non-checkpoint path inside the loop and the final
                  ;; coordinate call settle the same way, so one helper
                  ;; serves both. A coordinator result that is not a
                  ;; :checkpoint is the disposition: its :status, :reason
                  ;; and :phase are returned as they are, and the loop stops
                  ;; at that phase (refusal-totality rows #66-#68). Its ledger
                  ;; and transaction are written only when the result carries
                  ;; them: a refusal without a ledger has nothing valid to
                  ;; write, and reporting that nil write as
                  ;; :checkpoint-persist-failed was the mask of row #66.
                  (letfn [(settle [result phase receipts]
                            (let [lw (when (contains? result :ledger)
                                       (store/persist-ledger! ledger-path (:ledger result)))
                                  tw (when (contains? result :transaction)
                                       (persist-transaction! transaction-path
                                                             (:transaction result)))]
                              (cond-> {:status (:status result)
                                       :reason (:reason result)
                                       :phase (or (:phase result) phase)
                                       :route :dedicated-meta-self-heal
                                       :ordinary-selection-called? false
                                       :injury-id (get-in result [:transaction :work-order :injury-id])
                                       :repair-id (get-in result [:transaction :work-order :repair-id])
                                       :attempt (get-in result [:transaction :work-order :attempt])
                                       :patch-sha (get-in result [:transaction :receipts 0 :payload :patch-sha])
                                       :retry-click (latest-retry-click (:transaction result))
                                       :checkpoint-receipts receipts}
                                lw (assoc :ledger-write lw)
                                tw (assoc :transaction-write tw
                                          :transaction (:transaction result)))))]
                    (loop [durable durable transaction transaction
                           phases checkpoints receipts []]
                      (if-let [phase (first phases)]
                        (let [result (coordinator/coordinate
                                      durable transaction (assoc ports :stop-after phase))]
                          (if-not (= :checkpoint (:status result))
                            (settle result phase receipts)
                            (let [lw (store/persist-ledger! ledger-path (:ledger result))
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
                                                       (:transaction-pin (:transaction result))}))))))
                        ;; :final is the SelfHealRoute CoordinationPhase for
                        ;; the coordinate call after the loop (mathlib4
                        ;; darktower d6f4b88ca3); the coordinator's own
                        ;; :phase, when it gives one, is kept over it.
                        (settle (coordinator/coordinate durable transaction ports)
                                :final receipts)))))))))))))
