(ns futon2.aif.meta-self-heal-production
  "Fail-closed serving arbitration for durable dedicated META repair."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-repair-coordinator :as coordinator])
  (:import [java.nio.file Files StandardCopyOption]
           [java.nio.file.attribute FileAttribute]))

(def checkpoints [:claim :author :review :enact])
;; All live store defaults derive from the single live root in
;; meta-injury-store (fresh store opened 2026-10-22; the pre-2026-10-22
;; data directory is a frozen archive that no production path reads or writes).
(def default-ledger-path (str store/default-root "/ledger.edn"))
(def default-transaction-path (str store/default-root "/transaction.edn"))
(def default-diagnosis-root (str store/default-root "/diagnoses"))
(def default-acceptance-root (str store/default-root "/acceptances"))
(defn- refusal [reason & [details]] (merge {:status :refused :reason reason} details))
(defn- medic-authority-verifier [opts]
  (:meta-medic-authority-verifier opts))
(defn- load-durable [path authority-verifier]
  (if authority-verifier
    (store/load-durable-ledger path authority-verifier)
    (store/load-durable-ledger path)))
(defn- effective-active [durable-or-history authority-verifier]
  (if (and (= :wm/durable-meta-injury-ledger-v1 (:schema durable-or-history))
           (map? (:source-pin durable-or-history)))
    ;; `load-durable` validated this exact value under the selected verifier
    ;; and pinned the bytes it came from.  Keep the durable result intact
    ;; across this boundary so the projection cannot be asked to trust
    ;; another value.  A merely `:loaded` map is not validation evidence.
    (ledger/effective-active-injuries-from-validated-ledger
     (:ledger durable-or-history))
    (if authority-verifier
      (ledger/effective-active-injuries durable-or-history authority-verifier)
      (ledger/active-injuries durable-or-history))))
(defn- prepare-repair [durable options authority-verifier]
  (if authority-verifier
    (coordinator/prepare durable options authority-verifier)
    (coordinator/prepare durable options)))
(defn- coordinate-repair [durable transaction ports authority-verifier]
  (if authority-verifier
    (coordinator/coordinate durable transaction ports authority-verifier)
    (coordinator/coordinate durable transaction ports)))
(defn- persist-ledger [path history authority-verifier]
  (if authority-verifier
    (store/persist-ledger! path history authority-verifier)
    (store/persist-ledger! path history)))

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

(def diagnosis-request-budget-bytes
  "128 KiB for the serialized request, leaving substantial room below the
  adapter's 128k-token carried-context floor for history, tools and output."
  131072)

(defn- diagnosis-text [x]
  (binding [*print-length* nil *print-level* nil *print-namespace-maps* false]
    (pr-str x)))

(defn- diagnosis-bytes [x]
  (count (.getBytes (diagnosis-text x) "UTF-8")))

(defn bounded-diagnosis-request
  "Keep causal values verbatim when small, otherwise explicitly preview them.
  The index locates every top-level value in the unchanged pinned EDN record.
  Missing check details are not invented: the original failure's omission
  list remains visible. Oversized metadata refuses before any producer call."
  [injury-id source-pin record]
  (let [causal-keys #{:failure :failed-checks :checks :reviewer-falsifier}
        index (mapv (fn [[k v]] {:path [k] :serialized-bytes (diagnosis-bytes v)})
                    (sort-by (comp pr-str key) record))
        causal (into {} (filter #(contains? causal-keys (key %))) record)
        preview (fn [path value]
                  (let [text (diagnosis-text value)]
                    {:status :abbreviated :reason :causal-field-over-budget
                     :path path :serialized-bytes (count (.getBytes text "UTF-8"))
                     ;; At most 1024 code units each, also safe with EDN escaping.
                     :head (subs text 0 (min 1024 (count text)))
                     :tail (subs text (max 0 (- (count text) 1024)))}))
        retained (into {} (map (fn [[k v]]
                                [k (if (> (diagnosis-bytes v) 16384)
                                     (preview [k] v) v)])) causal)
        abbreviated (mapv (fn [{:keys [path serialized-bytes]}]
                            {:path path :serialized-bytes serialized-bytes
                             :reason (if (contains? causal-keys (first path))
                                       :causal-field-over-budget :available-in-pinned-record)})
                          (filter (fn [{:keys [path serialized-bytes]}]
                                    (or (not (contains? causal-keys (first path)))
                                        (> serialized-bytes 16384))) index))
        request {:schema :wm/meta-diagnosis-request-v1
                 :injury-id injury-id :source-record-pin source-pin
                 :injury-record retained :repair-claimed? false
                 :record-index index :abbreviated-fields abbreviated
                 :evidence-access {:format :edn :root :source-record-pin
                                   :instruction "Read full values at the indexed paths from the pinned record; verify SHA256. Previews are not complete evidence."}
                 :request-budget-bytes diagnosis-request-budget-bytes}
        ;; Either namespace-map printer convention may be used by the adapter.
        size (max (diagnosis-bytes request)
                  (binding [*print-length* nil *print-level* nil *print-namespace-maps* true]
                    (count (.getBytes (pr-str request) "UTF-8"))))]
    (when (> size diagnosis-request-budget-bytes)
      (throw (ex-info "Diagnosis metadata exceeds request budget"
                      {:kind :meta-self-heal/diagnosis-request-over-budget
                       :bytes size :budget diagnosis-request-budget-bytes})))
    request))

(defn- produce-diagnosis-once!
  "Turn an active injury's exact opening-record pin into a diagnosis request.
  This is deliberately before, and separate from, repair coordination: it
  never claims a repair, and only exposes a diagnosis evidence pin after its
  receipt is durable.  An existing pin remains the compatibility path for
  already-produced diagnoses."
  [opts durable active-entry corrected-request]
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
            (let [request (try (or corrected-request (bounded-diagnosis-request injury-id source-pin (:record source)))
                               (catch clojure.lang.ExceptionInfo e
                                 (if (= :meta-self-heal/diagnosis-request-over-budget (:kind (ex-data e)))
                                   (refusal (:kind (ex-data e))
                                            (assoc (ex-data e) :injury-id injury-id
                                                   :source-record-pin source-pin :repair-claimed? false))
                                   (throw e))))
                  response (cond
                             (= :refused (:status request)) request
                             :else (try (diagnose request)
                                (catch Throwable _
                                  {:status :failed
                                   :reason :meta-self-heal/diagnosis-port-threw})))]
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
                                 ::diagnosis (:diagnosis response)
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

(defn- nested-production-function-paths [diagnosis]
  (letfn [(walk [path value]
            (cond
              (map? value)
              (mapcat (fn [[k v]]
                        (let [p (conj path k)]
                          (concat (when (and (= k :production-functions) (seq path)) [p])
                                  (walk p v)))) value)
              (sequential? value)
              (mapcat (fn [[i v]] (walk (conj path i) v)) (map-indexed vector value))
              :else []))]
    (vec (sort-by pr-str (walk [] diagnosis)))))

(defn- diagnosis-defects [result]
  (dissoc result :status :reason :injury-id :request :repair-claimed? ::diagnosis))

(defn produce-diagnosis!
  "Produce a durable diagnosis, with one bounded correction for invalid
  production-function identities; never reinterpret a nested declaration."
  [opts durable active-entry]
  (let [first-result (produce-diagnosis-once! opts durable active-entry nil)]
    (if-not (= :meta-self-heal/diagnosis-production-functions-absent (:reason first-result))
      first-result
      (let [diagnosis (::diagnosis first-result)
            defects (diagnosis-defects first-result)
            found-at (nested-production-function-paths diagnosis)
            text (diagnosis-text diagnosis)
            correction {:defects defects :found-at found-at
                        :first-diagnosis (subs text 0 (min 8000 (count text)))
                        :instruction
                        (str ":production-functions must be a key of the :diagnosis map itself, "
                             "sibling of :classification and :primary-cause, with an entry for each missing role "
                             (pr-str (:missing-roles defects))
                             (when (seq found-at) (str "; it was found at " (pr-str found-at)))
                             "; return a corrected diagnosis.")}
            request (assoc (:request first-result) :correction correction)
            size (max (diagnosis-bytes request)
                      (binding [*print-length* nil *print-level* nil *print-namespace-maps* true]
                        (count (.getBytes (pr-str request) "UTF-8"))))]
        (if (> size diagnosis-request-budget-bytes)
          (refusal :meta-self-heal/diagnosis-unavailable
                   {:injury-id (:injury-id active-entry) :repair-claimed? false
                    :diagnosis-reason :meta-self-heal/diagnosis-request-over-budget
                    :bytes size :budget diagnosis-request-budget-bytes})
          (let [second-result (produce-diagnosis-once! opts durable active-entry request)]
            (if (= :meta-self-heal/diagnosis-production-functions-absent (:reason second-result))
              (refusal :meta-self-heal/diagnosis-correction-exhausted
                       {:injury-id (:injury-id active-entry) :repair-claimed? false
                        :first defects :second (diagnosis-defects second-result)})
              second-result)))))))

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

(def ^:private producer-trace-keys
  "The record keys producer-trace writes; admit-acceptance! carries them from
   the proposal onto every admission result (F22b)."
  [:acceptance-read-error :acceptance-read-retry :acceptance-transport-retry
   :acceptance-read-repair :acceptance-source-retry :acceptance-source-read-error
   :acceptance-source-repair :acceptance-source-transport-fallback])

(defn- producer-trace
  "What the producer did to get its answer, as record keys: F19's reader error
   and one informed re-ask (:read-retry), F9's one rate-limit retry
   (:transport-retry), F21's brace completion (:read-repair, click
   wm-click-4c3c26e2, 2026-10-20: the answer was one `}` short for the fourth
   time and the re-ask did not change that, so the reader completes it and
   says so), F22's source re-ask (:source-retry, :source-read-error), and
   F23's one-character source repair (:source-repair).
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
    (assoc :acceptance-source-read-error (:source-read-error response))
    ;; F23a (clicks wm-click-79710180 and wm-click-d26194dd, 2026-10-20):
    ;; the producer removed one surplus delimiter at the reader's position
    ;; (:source-repair, with the character, its line and column and the
    ;; reader error); the proposal's source text is the repaired text.
    (contains? response :source-repair)
    (assoc :acceptance-source-repair (:source-repair response))
    (contains? response :source-transport-fallback)
    (assoc :acceptance-source-transport-fallback
           (:source-transport-fallback response))))

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
                       (dissoc enrichment :production-functions)
                       (when-let [correction (:meta-acceptance-correction opts)]
                         {:correction correction})
                       (when-let [correction (:meta-acceptance-review-correction opts)]
                         {:review-correction correction})
                       (when-let [correction (:meta-acceptance-static-correction opts)]
                         {:static-correction correction})
                       (when-let [claim (:meta-acceptance-unsatisfiable-claim opts)]
                         {:author-unsatisfiable-claim claim}))
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

(def ^:private static-finding-types
  #{:unresolved-symbol :unresolved-namespace :syntax})

(defn- acceptance-static-check-result [source result]
  (let [parsed (try {:report (edn/read-string (:out result))}
                    (catch Throwable t {:parse-error (ex-message t)}))
        findings (->> (get-in parsed [:report :findings])
                      (filter #(contains? static-finding-types (:type %))) vec)]
    (cond
      (:parse-error parsed)
      {:status :failed :diagnostics [{:type :tooling-failure
                                     :message (:parse-error parsed)}]
       :source source}
      (not (contains? #{0 2 3} (:exit result)))
      {:status :failed :diagnostics [{:type :tooling-failure
                                     :exit (:exit result)
                                     :message (str/trim (or (:err result) ""))}]
       :source source}
      (seq findings)
      {:status :failed :diagnostics findings :source source}
      :else {:status :clean})))

(defn- acceptance-static-check [opts source]
  (let [repository (or (:meta-repository-root opts) default-repository-root)]
    (acceptance-static-check-result
     source
     (shell/sh "clj-kondo" "--lint" "-" "--config"
               "{:output {:format :edn}}"
               :dir repository :in source))))

(defn- statically-prepare-proposal [opts diagnosis-observation proposal]
  (if-not (nat-int? (:meta-acceptance-static-correction-limit opts))
    (refusal :meta-self-heal/acceptance-static-correction-limit-required
             {:injury-id (:injury-id diagnosis-observation)
              :repair-claimed? false})
    (loop [proposal proposal
         remaining (:meta-acceptance-static-correction-limit opts)]
    (if-not (= :proposed (:status proposal))
      proposal
      (let [source (get-in proposal [:acceptance-test :source :text])
            check ((or (:meta-acceptance-static-check-fn opts)
                       #(acceptance-static-check opts %)) source)]
        (if (= :clean (:status check))
          proposal
          (if-not (pos? remaining)
            (refusal :meta-self-heal/acceptance-static-correction-exhausted
                     {:injury-id (:injury-id diagnosis-observation)
                      :static-diagnostics (:diagnostics check)
                      :proposed-source source :repair-claimed? false})
            (recur (produce-acceptance!
                    (assoc opts :meta-acceptance-static-correction
                           {:diagnostics (:diagnostics check) :source source
                            :instruction (str "Correct only the reported static errors; "
                                              "preserve the test intent and assertions.")})
                    diagnosis-observation)
                   (dec remaining)))))))))

(declare admit-acceptance*)

(defn admit-acceptance!
  "Run, then independently review, one immutable concrete proposal.  The
  machine-run result is evidence shown to the reviewer; no prose, stale pin,
  or response for different diagnosis/source/proposal identity can become
  coordinator authority.  This is the corrected contract exposed by
  wm-click-6cdf2a7c: at 17:37:37Z the reviewer correctly refused the former
  `:result-status :not-run` request with
  `:reason/type :acceptance-test-not-run`.

  F22b (click wm-click-d26194dd, 2026-10-20): the producer trace that
  produce-acceptance! put on the proposal (its re-asks, repair and reader
  errors) is carried onto every admission result, refused or accepted, so
  the click record can say what the author was asked.  The admission
  receipt itself does not carry it.  Click 5's author was re-asked once
  for a source that did not read and answered with another that did not;
  the reviewer's refusal was the only trace on the record."
  [opts proposal]
  (merge (admit-acceptance* opts proposal)
         (select-keys proposal producer-trace-keys)))

(defn- admit-acceptance*
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
            request (cond-> {:schema :wm/meta-acceptance-review-request-v1
                             :injury-id (:injury-id proposal)
                             :source-record-pin (:source-record-pin proposal)
                             :diagnosis-evidence-pin (:diagnosis-evidence-pin proposal)
                             :proposal-evidence-pin (:acceptance-evidence-pin proposal)
                             :acceptance-test (:acceptance-test proposal)
                             :result-status :ran-before-repair
                             :result run-result :repair-claimed? false}
                      (:meta-acceptance-unsatisfiable-claim opts)
                      (assoc :author-unsatisfiable-claim
                             (:meta-acceptance-unsatisfiable-claim opts)))
            ;; F16 (off-click replay 5, 2026-10-19): a proposal whose run
            ;; PASSES before any repair does not pin the injury; it is refused
            ;; here, typed, without spending a reviewer invoke.
            passes-before-repair? (and valid-run? (= :passed (:outcome run-result)))
            does-not-compile? (and valid-run? (contains? run-result :compile-diagnostic))
            response (when (and valid-run? (not passes-before-repair?)
                                (not does-not-compile?))
                       (try (review request)
                          (catch Throwable _ {:status :failed
                                               :reason :meta-self-heal/acceptance-review-threw})))
            identity-keys [:injury-id :source-record-pin :diagnosis-evidence-pin
                           :proposal-evidence-pin :acceptance-test]
            review-identity (fn [value]
                              (-> (select-keys value identity-keys)
                                  (update :acceptance-test
                                          #(select-keys % [:id :command :expected]))))
            request-identity (review-identity request)
            response-identity (when (map? response) (review-identity response))]
        (cond
          (not valid-run?)
          (refusal :meta-self-heal/acceptance-run-refused
                   {:request request :acceptance-result run-result
                    :repair-claimed? false})
          does-not-compile?
          (refusal :meta-self-heal/acceptance-test-does-not-compile
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
                    :review-detail (:detail response)
                    :review-refusal (if (contains? response :refusal)
                                      (:refusal response)
                                      (select-keys response [:reason :reason/type :detail]))})
          (and (:author-unsatisfiable-claim request)
               (not (and (true? (get-in response [:satisfiability
                                                   :can-pass-after-correct-repair?]))
                    (string? (get-in response [:satisfiability :reason]))
                    (not (str/blank? (get-in response [:satisfiability :reason]))))))
          (refusal :meta-self-heal/acceptance-review-satisfiability-absent
                   {:request request :review response})
          (not= request-identity response-identity)
          (refusal :meta-self-heal/acceptance-review-identity-mismatch
                   {:request request
                    :review-identity
                    {:differs (->> identity-keys
                                   (filter #(not= (get request-identity %)
                                                  (get response-identity %)))
                                   vec)
                     :response-keys (->> (keys response) sort vec)}})
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
                         :satisfiability (:satisfiability response)
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

(defn- prepare-acceptance-before-review-correction
  "Runtime-facing proposal preparation.  A proposed/unreviewed specification
  remains observation only; this function never returns it as coordinator
  :acceptance-test authority."
  [opts diagnosis-observation]
  (cond
    (:acceptance-test opts)
    {:status :present :acceptance-test (:acceptance-test opts) :source :supplied}

    (= :available (:status diagnosis-observation))
    (let [proposal (statically-prepare-proposal
                    opts diagnosis-observation
                    (produce-acceptance! opts diagnosis-observation))
          first-result (if (= :proposed (:status proposal))
                         (admit-acceptance! opts proposal)
                         proposal)]
      (if-not (= :meta-self-heal/acceptance-test-does-not-compile
                 (:reason first-result))
        first-result
        (let [pinned-proposal (read-acceptance-proposal proposal)]
          (if-not (= :available (:status pinned-proposal))
            (refusal :meta-self-heal/acceptance-compile-correction-source-unavailable
                     {:injury-id (:injury-id diagnosis-observation)
                      :proposal-read (select-keys pinned-proposal [:status :reason])
                      :repair-claimed? false})
            (let [run (:acceptance-result first-result)
                  diagnostic (:compile-diagnostic run)
                  pins {:first-run-pin (:run-pin run)
                        :first-proposal-pin (:acceptance-evidence-pin proposal)}
                  correction (assoc pins
                                    :first-proposal-source
                                    (get-in pinned-proposal
                                            [:receipt :acceptance-test :source])
                                    :compile-diagnostic diagnostic
                                    :instruction
                                    (str "Make the minimal diagnostic correction to the "
                                         "provided failing source at "
                                         (:file diagnostic) ":" (:line diagnostic) ":"
                                         (:col diagnostic) ": " (:message diagnostic)
                                         "; preserve the test's intent and assertions; do not "
                                         "rebuild or replace the test. Use only the vars in "
                                         ":production-functions and vars that exist in the repository."))
                  revised (produce-acceptance!
                           (assoc opts :meta-acceptance-correction correction)
                           diagnosis-observation)
                  second-result (if (= :proposed (:status revised))
                                  (admit-acceptance! opts revised)
                                  revised)]
              ;; Deliberately not recursive: a second compiler failure exhausts
              ;; this correction, and neither failed run grants admission.
              (if (= :meta-self-heal/acceptance-test-does-not-compile
                     (:reason second-result))
                (refusal :meta-self-heal/acceptance-compile-correction-exhausted
                         {:injury-id (:injury-id diagnosis-observation)
                          :first run :second (:acceptance-result second-result)
                          :repair-claimed? false})
                (assoc second-result :acceptance-compile-correction pins)))))))

    (= :not-required (:status diagnosis-observation))
    {:status :not-required :reason :meta-self-heal/acceptance-no-diagnosis-required}

    :else
    {:status :unavailable :reason :meta-self-heal/acceptance-diagnosis-unavailable
     :diagnosis-status (:status diagnosis-observation)
     :repair-claimed? false}))
(defn prepare-acceptance
  "Prepare and admit independently executed acceptance proposals, with at most
  one compile correction followed by at most one review correction."
  [opts diagnosis-observation]
  (let [first-result (prepare-acceptance-before-review-correction opts diagnosis-observation)]
    (if-not (= :meta-self-heal/acceptance-review-refused (:reason first-result))
      first-result
      (let [request (:request first-result)
            run (:result request)
            review-refusal (:review-refusal first-result)
            pins {:first-run-pin (:run-pin run)
                  :first-proposal-pin (:proposal-evidence-pin request)}
            correction (assoc pins
                              :review-refusal review-refusal
                              :instruction
                              (str "The reviewer refused the previous test ("
                                   (or (:type review-refusal) (:reason review-refusal)
                                       (:reason/type review-refusal) :unspecified)
                                   "); return a corrected test whose failures before repair "
                                   "are the injury's own reason, with :expected {:exit 0}."))
            ;; Do not call preparation recursively: this revision has its own
            ;; run and review, but cannot spend either correction budget again.
            revised (produce-acceptance!
                     (-> opts
                         (dissoc :meta-acceptance-correction)
                         (assoc :meta-acceptance-review-correction correction))
                     diagnosis-observation)
            second-result (if (= :proposed (:status revised))
                            (admit-acceptance! opts revised)
                            revised)
            result (case (:reason second-result)
                     :meta-self-heal/acceptance-review-refused
                     (refusal :meta-self-heal/acceptance-review-correction-exhausted
                              {:injury-id (:injury-id diagnosis-observation)
                               :first first-result :second second-result
                               :first-run-pin (:run-pin run)
                               :second-run-pin (get-in second-result [:request :result :run-pin])
                               :repair-claimed? false})
                     :meta-self-heal/acceptance-test-does-not-compile
                     (refusal :meta-self-heal/acceptance-compile-correction-exhausted
                              {:injury-id (:injury-id diagnosis-observation)
                               :first run :second (:acceptance-result second-result)
                               :repair-claimed? false})
                     second-result)]
        (cond-> (assoc result :acceptance-review-correction pins)
          (:acceptance-compile-correction first-result)
          (assoc :acceptance-compile-correction
                 (:acceptance-compile-correction first-result)))))))

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

(defn- transaction-snapshot [path]
  (when (and path (.isFile (io/file path)))
    (let [bytes (Files/readAllBytes (.toPath (io/file path)))
          read (store/read-one-form (String. bytes "UTF-8"))
          transaction (:value read)]
      (when (and (= :read (:status read))
                 (= :valid (:status (coordinator/verify-transaction transaction))))
        {:transaction transaction :transaction-path (str path)
         :transaction-sha256 (load-identity/sha256 bytes)}))))

(defn- recorded-blocker [durable injury-id]
  (let [latest (last (filter #(and (= injury-id (:injury-id %)) (:state %))
                             (get-in durable [:ledger :entries])))
        cause (:requeue-cause latest)]
    (when (and (= :enacted (:state latest))
               (= :other-open-injury (:kind cause))
               (not= injury-id (:injury-id cause)))
      (:injury-id cause))))

(defn injury-selection
  "Read-only selection shared by intake and route. The transaction byte pin
  binds a later move to the file read here. No file is moved during intake."
  [path durable active]
  (let [injuries (:injuries active)
        file (when path (io/file path))
        prefix (when file (str (.getName file) ".deferred-"))
        select-snapshot (fn [snapshot]
                          (assoc (dissoc snapshot :transaction)
                                 :injury-id (get-in snapshot [:transaction :work-order :injury-id])))
        defer? (fn [injury-id]
                 (let [blocker (recorded-blocker durable injury-id)]
                   (when (contains? injuries blocker) blocker)))
        restorable (fn []
                     (when file
                       (->> (.listFiles (.getAbsoluteFile
                                         (or (.getParentFile file) (io/file "."))))
                            (filter #(and (str/starts-with? (.getName %) prefix)
                                          (str/ends-with? (.getName %) ".edn")))
                            (keep #(transaction-snapshot (.getPath %)))
                            (map select-snapshot)
                            (filter #(let [id (:injury-id %)
                                           blocker (recorded-blocker durable id)]
                                       (and (contains? injuries id) blocker
                                            (not (contains? injuries blocker)))))
                            (sort-by (juxt :injury-id :transaction-path)) first)))]
    (if-let [snapshot (transaction-snapshot path)]
      (let [selection (select-snapshot snapshot) injury-id (:injury-id selection)]
        (cond
          (not (contains? injuries injury-id))
          (if-let [deferred (restorable)]
            (assoc deferred :mode :restore :restore-to path
                   :stale-transaction selection)
            {:mode :fresh :injury-id (first (sort (keys injuries)))
             :stale-transaction selection})

          (defer? injury-id)
          (let [blocker (defer? injury-id)]
            (assoc selection :mode :defer :injury-id blocker
                   :deferred-injury injury-id :blocking-injury blocker
                   :deferred-path (str path ".deferred-"
                                       (str/replace injury-id #"[^A-Za-z0-9._-]" "_") ".edn")))

          :else (assoc selection :mode :resume)))
      (let [deferred (restorable)]
        (if deferred
          (assoc deferred :mode :restore :restore-to path)
          ;; A deferred injury whose blocker is still active yields to the
          ;; blocker here too, e.g. when the blocker's prepare refused after
          ;; the defer moved the old transaction aside.
          (let [injury-id (first (sort (keys injuries)))]
            {:mode :fresh :injury-id (or (defer? injury-id) injury-id)}))))))

(defn- move-selected-transaction! [selection]
  (when (#{:defer :restore} (:mode selection))
    (let [source (:transaction-path selection)
          target (or (:deferred-path selection) (:restore-to selection))]
      (try
        (cond
          (not= (:transaction-sha256 selection)
                (load-identity/sha256 (Files/readAllBytes (.toPath (io/file source)))))
          (refusal :meta-self-heal/selected-transaction-changed {:path source})
          (.exists (io/file target))
          (refusal :meta-self-heal/transaction-move-conflict {:path target})
          :else
          (do (Files/move (.toPath (io/file source)) (.toPath (io/file target))
                          (into-array StandardCopyOption [StandardCopyOption/ATOMIC_MOVE]))
              {:status :moved :path target}))
        (catch Throwable t
          (refusal :meta-self-heal/transaction-move-failed
                   {:path source :target target :detail (ex-message t)}))))))

(defn carried-bindings-observation
  "Read a valid persisted repair transaction once and describe whether its
  diagnosis and acceptance bindings belong to an active injury."
  [opts context]
  (let [active (:active-injuries context)]
    (if (or (not= :computed (:status active))
            (empty? (:injuries active)))
      {:status :absent :reason :no-active-injury}
      (let [selection (or (:injury-selection context)
                          (injury-selection (:meta-repair-transaction-path opts)
                                            (:durable-ledger context) active))]
        (cond
          (and (:transaction-path selection)
               (not= (:meta-repair-transaction-path opts)
                     (or (:restore-to selection) (:transaction-path selection))))
          {:status :absent :reason :transaction-selection-path-mismatch}
          (= :defer (:mode selection))
          {:status :absent :reason :transaction-deferred
           :blocking-injury (:blocking-injury selection)}
          :else
          (if-let [transaction (when-let [path (:transaction-path selection)]
                                 (load-transaction path))]
            (let [work-order (:work-order transaction)
                  claim (->> (get-in context [:durable-ledger :ledger :entries])
                             (filter #(and (= ledger/observation-schema (:schema %))
                                           (= :acceptance-unsatisfiable-claimed
                                              (:observation-kind %))
                                           (= (:injury-id work-order) (:injury-id %))
                                           (= (:repair-id work-order) (:repair-id %))
                                           (= (:attempt work-order) (:attempt %))))
                             last)]
              (if (contains? (:injuries active) (:injury-id work-order))
                (cond->
                 {:diagnosis-evidence-pin (:diagnosis-evidence-pin work-order)
                  :source-record-pin (:cause-record-pin work-order)
                  :acceptance-test (:acceptance-test work-order)
                  :injury-id (:injury-id work-order)
                  :repair-id (:repair-id work-order)
                  :source :carried-transaction
                  :transaction-pin (:transaction-pin transaction)
                  :attempt (:attempt work-order)}
                  claim
                  (assoc :acceptance-unsatisfiable-claim
                         {:attempt (:attempt claim)
                          :acceptance-test (:acceptance-test work-order)
                          :author-evidence (:author-evidence claim)
                          :observation-pin (:entry-pin claim)}))
                {:status :absent :reason :transaction-injury-not-active}))
            {:status :absent :reason :no-transaction}))))))

(defn carried-bindings
  "Return persisted diagnosis and acceptance bindings for an active injury."
  [opts context]
  (let [observation (carried-bindings-observation opts context)]
    (when (= :carried-transaction (:source observation))
      observation)))

(defn- retire-stale-transaction!
  "Preserve a resumed transaction that belongs to an earlier authoring attempt.
  A reopened ledger is authoritative; replacing this file would otherwise make
  the old failed author receipt replay forever."
  [path transaction]
  (let [attempt (get-in transaction [:work-order :attempt])
        retired-path (str path ".attempt-" attempt ".retired.edn")]
    (try
      (Files/move (.toPath (io/file path))
                  (.toPath (io/file retired-path))
                  (into-array StandardCopyOption [StandardCopyOption/ATOMIC_MOVE]))
      {:status :retired :path retired-path :attempt attempt}
      (catch Throwable _
        (refusal :meta-self-heal/transaction-retire-failed
                 {:path path :attempt attempt})))))

(defn recover-unadmitted-transaction!
  "Medic-authorized recovery for one same-attempt ledger/transaction author
  divergence. A ledger-admitted deferred transaction must already exist. The
  unadmitted canonical transaction is archived byte-for-byte, leaving the
  existing injury-selection restore path to reinstate the admitted transaction.
  The ledger is never changed."
  [{:keys [ledger-path transaction-path injury-id repair-id attempt
           medic-authority authority-verifier]}]
  (try
    (let [authority-pin (:authority-receipt-pin medic-authority)
          durable (when (and (fn? authority-verifier) (map? medic-authority))
                    (store/load-durable-ledger ledger-path authority-verifier))
          history (:ledger durable)
          snapshot (transaction-snapshot transaction-path)
          transaction (:transaction snapshot)
          order (:work-order transaction)
          latest (last (filter #(and (= ledger/transition-schema (:schema %))
                                     (= injury-id (:injury-id %)))
                               (:entries history)))
          ledger-author (last (filter #(and (= :authored (:state %))
                                            (= injury-id (:injury-id %))
                                            (= repair-id (:repair-id %))
                                            (= attempt (:attempt %)))
                                      (:entries history)))
          ledger-author-evidenced?
          (and (map? (:port-receipt-pin ledger-author))
               (string? (get-in ledger-author [:port-receipt-pin :path]))
               (boolean (re-matches #"[0-9a-f]{64}"
                                    (str (get-in ledger-author
                                                 [:port-receipt-pin :sha256]))))
               (string? (:patch-sha ledger-author))
               (boolean (re-matches #"[0-9a-f]{40}" (:patch-sha ledger-author))))
          transaction-file (io/file transaction-path)
          deferred-prefix (str (.getName transaction-file) ".deferred-")
          admitted-deferred
          (when ledger-author-evidenced?
            (when-let [files (.listFiles (.getAbsoluteFile
                                          (or (.getParentFile transaction-file)
                                              (io/file "."))))]
              (some (fn [file]
                      (when (and (str/starts-with? (.getName file) deferred-prefix)
                                 (str/ends-with? (.getName file) ".edn"))
                        (when-let [{deferred-transaction :transaction :as deferred}
                                   (transaction-snapshot (.getPath file))]
                          (let [deferred-order (:work-order deferred-transaction)
                                deferred-author (coordinator/current-receipt-for
                                                 deferred-transaction :author)]
                            (when (and (= [injury-id repair-id attempt]
                                          [(:injury-id deferred-order)
                                           (:repair-id deferred-order)
                                           (:attempt deferred-order)])
                                       (= (:port-receipt-pin ledger-author)
                                          (:receipt-pin deferred-author))
                                       (= (:patch-sha ledger-author)
                                          (get-in deferred-author
                                                  [:payload :patch-sha])))
                              deferred)))))
                    (sort-by #(.getName %) files))))
          evidence {:injury-id injury-id :repair-id repair-id :attempt attempt
                    :ledger-entry-id (:entry-id latest)
                    :ledger-entry-pin (:entry-pin latest)
                    :canonical-transaction-path (str transaction-path)
                    :canonical-transaction-sha256 (:transaction-sha256 snapshot)
                    :deferred-transaction-path (:transaction-path admitted-deferred)
                    :deferred-transaction-sha256
                    (:transaction-sha256 admitted-deferred)
                    :ledger-author-receipt-pin (:port-receipt-pin ledger-author)
                    :ledger-author-patch (:patch-sha ledger-author)}
          authority-context {:operation medic-authority :current latest
                             :ledger history :candidate evidence :evidence evidence}]
      (cond
        (not (and (fn? authority-verifier) (map? medic-authority)
                  (map? authority-pin) (string? (:path authority-pin))
                  (boolean (re-matches #"[0-9a-f]{64}" (str (:sha256 authority-pin))))))
        (refusal :meta-self-heal/recovery-medic-authority-required)

        (not= :loaded (:status durable))
        (refusal :meta-self-heal/recovery-ledger-unavailable
                 {:ledger-status (:status durable) :ledger-reason (:reason durable)})

        (nil? snapshot)
        (refusal :meta-self-heal/recovery-transaction-unavailable)

        (not= [injury-id repair-id attempt]
              [(:injury-id order) (:repair-id order) (:attempt order)])
        (refusal :meta-self-heal/recovery-identity-mismatch)

        (not (true? (authority-verifier authority-context)))
        (refusal :meta-self-heal/recovery-medic-authority-refused)

        (not ledger-author-evidenced?)
        (refusal :meta-self-heal/recovery-ledger-author-missing)

        (nil? admitted-deferred)
        (refusal :meta-self-heal/recovery-no-admitted-transaction)

        :else
        (let [identity-check (coordinator/coordinate durable transaction {}
                                                     authority-verifier)]
          (if-not (= :coordinator/resume-identity-refused (:reason identity-check))
            (refusal :meta-self-heal/recovery-divergence-not-confirmed)
            (let [archive-path (str transaction-path ".attempt-" attempt
                                    ".unadmitted-"
                                    (:transaction-sha256 snapshot)
                                    ".retired.edn")]
              (try
                (cond
                  (not= (:transaction-sha256 snapshot)
                        (load-identity/sha256
                         (Files/readAllBytes (.toPath transaction-file))))
                  (refusal :meta-self-heal/recovery-transaction-changed)

                  (.exists (io/file archive-path))
                  (refusal :meta-self-heal/recovery-archive-conflict
                           {:path archive-path})

                  :else
                  (do
                    (Files/move (.toPath transaction-file)
                                (.toPath (io/file archive-path))
                                (into-array StandardCopyOption
                                            [StandardCopyOption/ATOMIC_MOVE]))
                    {:status :archived
                     :archive-pin {:path archive-path
                                   :sha256 (:transaction-sha256 snapshot)}
                     :deferred-path (:transaction-path admitted-deferred)}))
                (catch Throwable t
                  (refusal :meta-self-heal/transaction-retire-failed
                           {:path transaction-path :attempt attempt
                            :detail (ex-message t)}))))))))
    (catch Throwable t
      (refusal :meta-self-heal/recovery-failed {:detail (ex-message t)}))))

(defn agency-port
  "ROLE is :author or :reviewer. The seat invoked is the one the sealed work
  order names for that role (click wm-click-36b47627, 2026-10-19: the seat
  names were fixed here as wm-author / wm-reviewer, so a click cast on other
  seats would still have dispatched its repair phases to the defaults)."
  ([invoke role] (agency-port invoke role nil))
  ([invoke role prepare-worktree]
  (fn [{:keys [work-order phase predecessor-pin transaction] :as request}]
    (let [workspace (when (and (= role :author) prepare-worktree)
                      (prepare-worktree work-order))
          seat (get work-order role)
          session-key ({:author :author-session
                        :reviewer :reviewer-session} role)
          ;; Enact/review adapters receive the sealed transaction too, so a
          ;; concrete serving boundary can bind its effect to the exact prior
          ;; author/reviewer evidence rather than reconstructing it.
          response (if (and workspace (= :refused (:status workspace)))
                     {:status :failed :payload {:reason (:reason workspace)}}
                     (invoke {:seat seat :session-id (get work-order session-key)
                              :transaction transaction :request request}))]
      (coordinator/seal-port-receipt
       {:phase phase :injury-id (:injury-id work-order)
        :repair-id (:repair-id work-order) :attempt (:attempt work-order)
        :predecessor-pin predecessor-pin :status (:status response)
        :payload (assoc (:payload response) :worktree-provision workspace
                        :agency-job-id (:job-id response)
                        :agency-session-id (:session-id response)
                        :agency-seat seat)})))))
(defn endpoint-port [invoke adapter]
  (fn [{:keys [work-order phase predecessor-pin] :as request}]
    (let [response (invoke request)]
      (coordinator/seal-port-receipt
       {:phase phase :injury-id (:injury-id work-order)
        :repair-id (:repair-id work-order) :attempt (:attempt work-order)
        :predecessor-pin predecessor-pin :status (:status response)
        :payload (assoc (:payload response) :adapter adapter)}))))

(defn- read-pinned-edn [pin]
  (try
    (when (pin? pin)
      (let [bytes (Files/readAllBytes (.toPath (io/file (:path pin))))]
        (when (= (:sha256 pin) (load-identity/sha256 bytes))
          (edn/read-string (String. bytes "UTF-8")))))
    (catch Throwable _ nil)))

(defn- acceptance-source-identity [acceptance-test]
  (when-let [source (get-in acceptance-test [:source :text])]
    (load-identity/sha256 (.getBytes source "UTF-8"))))

(defn- acceptance-test-identity [acceptance-test]
  (when-let [source-sha (acceptance-source-identity acceptance-test)]
    {:id (:id acceptance-test)
     :command (:command acceptance-test)
     :source-sha256 source-sha}))

(defn resolve-acceptance-admission
  "Resolve one legacy admission by injury and exact admitted-test identity.
   Only content-addressed acceptance files whose names match their bytes are
   candidates. Zero or several matches deliberately return nil."
  [acceptance-root work-order]
  (let [dir (io/file acceptance-root)
        wanted (acceptance-test-identity (:acceptance-test work-order))
        matches (when (and wanted (.isDirectory dir))
                  (->> (.listFiles dir)
                       (filter #(.isFile %))
                       (keep (fn [file]
                               (try
                                 (let [bytes (Files/readAllBytes (.toPath file))
                                       sha (load-identity/sha256 bytes)
                                       pin {:path (.getPath file) :sha256 sha}
                                       admission (edn/read-string (String. bytes "UTF-8"))]
                                   (when (and (= (.getName file)
                                                 (str "acceptance-" sha ".edn"))
                                              (= :wm/meta-acceptance-admission-v1
                                                 (:schema admission))
                                              (= :accepted (:status admission))
                                              (= (:injury-id work-order)
                                                 (:injury-id admission))
                                              (= wanted
                                                 (acceptance-test-identity
                                                  (:acceptance-test admission))))
                                     {:pin pin :admission admission}))
                                 (catch Throwable _ nil))))))]
    (when (= 1 (count matches)) (first matches))))

(defn- read-pinned-text [pin]
  (try
    (when (pin? pin)
      (let [bytes (Files/readAllBytes (.toPath (io/file (:path pin))))]
        (when (= (:sha256 pin) (load-identity/sha256 bytes))
          (String. bytes "UTF-8"))))
    (catch Throwable _ nil)))

(defn- acceptance-run-report [run-pin expected-namespace]
  (when-let [log (read-pinned-text run-pin)]
    (let [namespaces (mapv second (re-seq #"(?m)^Testing\s+(\S+)\s*$" log))
          summary (re-find #"(?m)^Ran\s+(\d+)\s+tests?\s+containing\s+\d+\s+assertions?\.\s*$[\r\n]+^(\d+)\s+failures?,\s+(\d+)\s+errors?\.\s*$"
                           log)]
      (when summary
        {:namespace-witness (when (= [expected-namespace] namespaces)
                              expected-namespace)
         :testing-namespaces namespaces
         :tests (Long/parseLong (nth summary 1))
         :failures (Long/parseLong (nth summary 2))
         :errors (Long/parseLong (nth summary 3))}))))

(defn acceptance-replay-port
  "Build the tooling-process port that compares a retained red admission run
  with one current run of the identical admitted acceptance source. Missing or
  mismatched pins are reported as input-unavailable so the coordinator can use
  its existing whole-child retry."
  ([run-acceptance]
   (acceptance-replay-port run-acceptance default-acceptance-root))
  ([run-acceptance acceptance-root]
  (fn [{:keys [work-order transaction phase predecessor-pin]}]
    (let [explicit-pin (:acceptance-admission-pin work-order)
          resolved (when-not explicit-pin
                     (resolve-acceptance-admission acceptance-root work-order))
          admission-pin (or explicit-pin (:pin resolved))
          admission (or (read-pinned-edn explicit-pin) (:admission resolved))
          acceptance-test (:acceptance-test work-order)
          admitted-test (:acceptance-test admission)
          admitted-source (acceptance-source-identity admitted-test)
          current-source (acceptance-source-identity acceptance-test)
          admitted-run (:result admission)
          available? (and (= :wm/meta-acceptance-admission-v1 (:schema admission))
                          (= :accepted (:status admission))
                          (contains? #{:failed :errored} (:outcome admitted-run))
                          (pin? (:run-pin admitted-run))
                          (some? admitted-source)
                          (= admitted-source current-source))
          current-run (when available?
                        (try (run-acceptance acceptance-test)
                             (catch Throwable t
                               {:status :failed :outcome :errored :exit 1
                                :summary (ex-message t)})))
          checked? (and (= :ran (:status current-run))
                        (contains? #{:passed :failed :errored} (:outcome current-run))
                        (pin? (:run-pin current-run)))
          entry-var (get-in admitted-test [:source :namespace])
          report (when checked?
                   (acceptance-run-report (:run-pin current-run) entry-var))
          path-witness (when (and (= entry-var (:namespace-witness report))
                                  (pos? (:tests report 0)))
                         entry-var)
          outcome (cond
                    (not available?) :input-unavailable
                    (not checked?) :input-unavailable
                    (nil? path-witness) :phase-mismatch
                    (and (zero? (:failures report)) (zero? (:errors report))) :repaired
                    :else :same-failure)
          author (coordinator/current-receipt-for transaction :author)
          enact (coordinator/current-receipt-for transaction :enact)
          replay {:schema :wm/pinned-admission-phase-replay-receipt-v1
                  :injury-id (:injury-id work-order)
                  :repair-id (:repair-id work-order)
                  :patch-sha (get-in author [:payload :patch-sha])
                  :phase-id :admitted-acceptance
                  :entry-var entry-var
                  :path-witness path-witness
                  :admission-pin admission-pin
                  :admission-resolution (if resolved :lookup :pinned)
                  :admission-run-pin (:run-pin admitted-run)
                  :current-run-pin (:run-pin current-run)
                  :admission-source-identity admitted-source
                  :current-source-identity current-source
                  :before-predicate (when available? :failing)
                  :after-predicate (when path-witness
                                     (if (and (zero? (:failures report))
                                              (zero? (:errors report)))
                                       :passing :failing))
                  :run-report report
                  :input-available (boolean (and available? checked?))
                  :outcome outcome}]
      (coordinator/seal-port-receipt
       {:phase phase :injury-id (:injury-id work-order)
        :repair-id (:repair-id work-order) :attempt (:attempt work-order)
        :predecessor-pin predecessor-pin
        :status (if (= :repaired outcome) :succeeded :failed)
        :payload {:repair-id (:repair-id work-order)
                  :patch-sha (get-in author [:payload :patch-sha])
                  :retry-click (get-in enact [:payload :retry-click])
                  :phase-replay-receipt replay
                  :adapter "wm-acceptance-replay"}})))))

(defn- effect-ports [opts]
  (or (:meta-repair-ports opts)
      (when (and (:agency-invoke-fn opts) (:repair-enact-fn opts)
                 (:repair-retry-fn opts))
        (cond-> {:author (agency-port (:agency-invoke-fn opts) :author
                                     (:repair-worktree-fn opts))
                 :review (agency-port (:agency-invoke-fn opts) :reviewer)
                 :enact (endpoint-port (:repair-enact-fn opts) "wm-repair-reviewer")
                 :retry (endpoint-port (:repair-retry-fn opts) "wm-retry")
                 :acceptance-replay
                 (acceptance-replay-port (:meta-acceptance-run-fn opts))}
          ;; M10b-3 (2026-10-20): the futon3c adapter exposes its pure click
          ;; minting function as :retry-click-id-fn; the coordinator reads it
          ;; as :retry-click-id when it re-queues an inconclusive retry. Its
          ;; absence is not a port defect here: the coordinator refuses
          ;; :coordinator/retry-click-id-unavailable only at the requeue.
          (fn? (:retry-click-id-fn opts))
          (assoc :retry-click-id (:retry-click-id-fn opts))
          (fn? (:repair-integration-equivalent-fn opts))
          (assoc :integration-equivalent?
                 (:repair-integration-equivalent-fn opts))
          ;; One-click frontier (2026-10-22): the coordinator reads this
          ;; read-only port to observe a meta-retry child that failed
          ;; service-side before any run record existed. Absence is not a
          ;; defect: without it such a child simply stays :awaiting-retry.
          (fn? (:retry-click-status-fn opts))
          (assoc :retry-click-status-fn (:retry-click-status-fn opts))))))
(defn latest-retry-click
  "The retry click the transaction currently names. Receipts are keyed by
   phase, not by index: after author, review, enact and retry, each requeue
   of an inconclusive retry appends a :retry-requeue-<n> receipt (M10b-3b),
   so the click at receipt index 2 (the enact receipt's minted click) goes
   stale on the first requeue. Read the last retry-family receipt's click,
   else the enact receipt's, else nil."
  [transaction]
  (let [receipts (coordinator/current-receipts transaction)
        phase-name #(when (keyword? (:phase %)) (name (:phase %)))
        retry? #(let [n (phase-name %)]
                  (and n (or (= "retry" n) (str/starts-with? n "retry-requeue-"))))]
    (or (get-in (last (filter retry? receipts)) [:payload :retry-click])
        (get-in (coordinator/current-receipt-for transaction :enact)
                [:payload :retry-click]))))
(defn- work-order-options [opts]
  (cond-> (assoc (select-keys opts [:diagnosis-evidence-pin :acceptance-test :author :reviewer
                                    :repair-reviewer :author-session :reviewer-session
                                    :repair-reviewer-session :target-repository :target-worktree
                                    :target-worktree-root :canonical-base-sha])
                 :acceptance-return-limit (:meta-acceptance-return-limit opts))
    (or (:acceptance-admission-pin opts)
        (get-in opts [:meta-acceptance-observation :acceptance-admission-pin]))
    (assoc :acceptance-admission-pin
           (or (:acceptance-admission-pin opts)
               (get-in opts [:meta-acceptance-observation :acceptance-admission-pin])))))

(defn- continued-work-order-options
  "A continued author attempt needs new port-session identities even though its
  diagnosis and admission evidence remain bound to the same injury."
  [opts attempt]
  (reduce (fn [options key]
            (update options key #(when % (str % "/attempt-" attempt))))
          (work-order-options opts)
          [:author-session :reviewer-session :repair-reviewer-session]))

(defn diagnosis-context
  "Read the durable injury state once for runtime diagnosis preparation.  The
  context is evidence, not readiness: an unreadable ledger remains available
  to readiness as its original typed defect."
  [opts]
  (let [ledger-path (or (:meta-injury-ledger-path opts) default-ledger-path)
        authority-verifier (medic-authority-verifier opts)
        durable (when (.isFile (io/file ledger-path))
                  (load-durable ledger-path authority-verifier))
        active (when (= :loaded (:status durable))
                 (effective-active durable authority-verifier))]
    {:meta-injury-ledger-path ledger-path
     :durable-ledger durable
     :active-injuries active
     :injury-selection (injury-selection (:meta-repair-transaction-path opts)
                                        durable active)}))

(defn prepare-diagnosis
  "Prepare exactly the injury route that coordinator/prepare will select.  A
  supplied pin is intentionally not re-produced; no active injury is an
  explicit not-required observation.  The read decides first (SelfHealRoute
  rev 6, ledgerRead): a loaded ledger whose active set did not compute is
  unavailable before a supplied pin can short-circuit (M8R finding 15,
  2026-10-20), so run-opportunity! ends the click there under a pin too."
  [opts {:keys [durable-ledger active-injuries injury-selection]}]
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
    (let [injury-id (or (:injury-id injury-selection)
                        (first (sort (keys (:injuries active-injuries)))))
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
          (let [claim (->> (get-in opts [:meta-diagnosis-context :durable-ledger
                                         :ledger :entries])
                           (filter #(and (= :acceptance-unsatisfiable-claimed
                                            (:observation-kind %))
                                         (= (:injury-id work-order) (:injury-id %))
                                         (= (:repair-id work-order) (:repair-id %))
                                         (= (:attempt work-order) (:attempt %))))
                           last)]
            (if-not claim
              (refusal :meta-self-heal/acceptance-transaction-mismatch
                       {:acceptance-observation observation
                        :expected expected :actual actual})
              (let [old-pin (or (:acceptance-admission-pin work-order)
                                (some (fn [file]
                                        (try
                                          (let [bytes (Files/readAllBytes (.toPath file))
                                                receipt (:value (store/read-one-form
                                                                 (String. bytes "UTF-8")))]
                                            (when (and (= :wm/meta-acceptance-admission-v1
                                                          (:schema receipt))
                                                       (= (:acceptance-test work-order)
                                                          (:acceptance-test receipt)))
                                              {:path (.getCanonicalPath file)
                                               :sha256 (load-identity/sha256 bytes)}))
                                          (catch Throwable _ nil)))
                                      (seq (.listFiles
                                            (io/file (or (:meta-acceptance-root opts)
                                                         default-acceptance-root))))))
                    new-pin (:acceptance-admission-pin observation)
                    rebound (when (and (pin? old-pin) (pin? new-pin))
                              (coordinator/supersede-acceptance
                               transaction (:acceptance-test observation)
                               old-pin new-pin (:entry-pin claim)))]
                (if-not rebound
                  (refusal :meta-self-heal/acceptance-supersession-pin-unavailable
                           {:superseded-admission-pin old-pin
                            :current-admission-pin new-pin})
                  {:status :valid :source :claim-authorized-supersession
                   :transaction rebound
                   :acceptance-supersession (:acceptance-supersession rebound)})))))))))

(defn attributable-retry-readiness
  "Authorize the one ordinary retry named by the latest enacted transition.
   This is the only path allowed to run ordinary machinery while an injury is
   active; arbitrary maps cannot bypass repair arbitration."
  [opts retry]
  (let [ledger-path (or (:meta-injury-ledger-path opts) default-ledger-path)
        authority-verifier (medic-authority-verifier opts)
        durable (when (.isFile (io/file ledger-path))
                  (load-durable ledger-path authority-verifier))
        history (:ledger durable)
        effective (when (= :loaded (:status durable))
                    (effective-active history authority-verifier))
        injury-id (:injury-id retry)
        enacted (get-in effective [:injuries injury-id])
        entries (:entries history)
        authored (last (filter #(and (= :authored (:state %))
                                     (= injury-id (:injury-id %))
                                     (= (:repair-id enacted) (:repair-id %))
                                     (= (:attempt enacted) (:attempt %))) entries))
        expected (select-keys enacted [:injury-id :repair-id :retry-click])
        supplied (select-keys retry [:injury-id :repair-id :retry-click])]
    (if (and (= :loaded (:status durable))
             (= :computed (:status effective))
             (= :enacted (:state enacted)) authored
             (= expected supplied) (= (:patch-sha authored) (:patch-sha retry)))
      {:status :ready :attribution expected :patch-sha (:patch-sha retry)}
      (refusal :meta-self-heal/retry-attribution-refused
               {:expected (assoc expected :patch-sha (:patch-sha authored))
                :supplied (assoc supplied :patch-sha (:patch-sha retry))}))))

(defn route!
  "Nil means no active durable injury. A map consumes the opportunity."
  [opts]
  (let [ledger-path (or (:meta-injury-ledger-path opts) default-ledger-path)
        authority-verifier (medic-authority-verifier opts)
        context (:meta-diagnosis-context opts)
        durable (if context (:durable-ledger context)
                    (when (and ledger-path (.isFile (io/file ledger-path)))
                      (load-durable ledger-path authority-verifier)))
        active (if context (:active-injuries context)
                   (when (= :loaded (:status durable))
                     (effective-active (:ledger durable) authority-verifier)))]
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
              (let [initial-selection (if context (:injury-selection context)
                                          (injury-selection transaction-path durable active))
                    ;; Deferred-transaction restoration is a new filesystem
                    ;; decision: reacquire authority and current active state.
                    refreshed (when (= :restore (:mode initial-selection))
                                (load-durable ledger-path authority-verifier))
                    refreshed-active (when (= :loaded (:status refreshed))
                                       (effective-active (:ledger refreshed) authority-verifier))
                    selection (if refreshed
                                (injury-selection transaction-path refreshed refreshed-active)
                                initial-selection)
                    durable (or refreshed durable)
                    active (or refreshed-active active)
                    stale-retirement
                    (when (:stale-transaction selection)
                      (if-let [stale (load-transaction transaction-path)]
                        (retire-stale-transaction! transaction-path stale)
                        (refusal :meta-self-heal/stale-transaction-unreadable)))
                    movement (cond
                               (nil? selection)
                               (refusal :meta-self-heal/injury-selection-missing)
                               (= :refused (:status stale-retirement))
                               stale-retirement
                               (and stale-retirement (= :restore (:mode selection)))
                               (move-selected-transaction! selection)
                               stale-retirement stale-retirement
                               :else (move-selected-transaction! selection))
                    resumed0 (when-not (or (= :refused (:status movement))
                                           (= :retired (:status movement))
                                           (= :defer (:mode selection)))
                               (load-transaction transaction-path))
                    resumed-injury (get-in resumed0 [:work-order :injury-id])
                    ledger-attempt (get-in active [:injuries resumed-injury :attempt])
                    retirement (when (and resumed0 (integer? ledger-attempt)
                                          (< (get-in resumed0 [:work-order :attempt])
                                             ledger-attempt))
                                 (retire-stale-transaction! transaction-path resumed0))
                    resumed (when-not (= :retired (:status retirement)) resumed0)
                    prepared (when-not (or resumed (= :refused (:status movement)))
                               (prepare-repair durable
                                 (assoc (work-order-options opts) :injury-id (:injury-id selection))
                                 authority-verifier))
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
                    transaction (or (:transaction acceptance-binding) transaction)
                    binding (or (when-not (= :valid (:status diagnosis-binding)) diagnosis-binding)
                                acceptance-binding)]
                (if (= :refused (:status movement))
                  (assoc movement :route :dedicated-meta-self-heal :ordinary-selection-called? false)
                  (if (= :refused (:status retirement))
                  (assoc retirement :route :dedicated-meta-self-heal
                         :ordinary-selection-called? false)
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
                                       (persist-ledger ledger-path (:ledger result)
                                                       authority-verifier))
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
                                       :patch-sha (get-in (coordinator/current-receipt-for
                                                           (:transaction result) :author)
                                                          [:payload :patch-sha])
                                       :retry-click (latest-retry-click (:transaction result))
                                       :checkpoint-receipts receipts}
                                (string? (:detail result))
                                (assoc :detail
                                       (subs (:detail result)
                                             0 (min 2000 (count (:detail result)))))
                                (= :retired (:status retirement))
                                (assoc :transaction-retired
                                       (select-keys retirement [:path :attempt]))
                                (= :defer (:mode selection))
                                (assoc :transaction-deferred
                                       {:path (:path movement) :injury-id (:deferred-injury selection)
                                        :blocking-injury (:blocking-injury selection)})
                                (= :restore (:mode selection))
                                (assoc :transaction-restored {:path (:path movement)})
                                lw (assoc :ledger-write lw)
                                tw (assoc :transaction-write tw
                                          :transaction (:transaction result)))))
                          (run-attempt [attempt-durable attempt-transaction]
                            (loop [durable attempt-durable transaction attempt-transaction
                                   phases checkpoints receipts []]
                              (if-let [phase (first phases)]
                                (let [result (coordinate-repair
                                              durable transaction (assoc ports :stop-after phase)
                                              authority-verifier)]
                                  (if-not (= :checkpoint (:status result))
                                    (settle result phase receipts)
                                    (let [lw (persist-ledger ledger-path (:ledger result)
                                                             authority-verifier)
                                          tw (persist-transaction! transaction-path (:transaction result))]
                                      (if-not (and (= :persisted (:status lw))
                                                   (= :persisted (:status tw)))
                                        (refusal :meta-self-heal/checkpoint-persist-failed
                                                 {:phase phase :ledger-write lw :transaction-write tw})
                                        (recur (load-durable ledger-path authority-verifier)
                                               (:transaction result) (next phases)
                                               (conj receipts {:phase phase
                                                               :ledger-pin (:ledger-pin (:ledger result))
                                                               :transaction-pin
                                                               (:transaction-pin (:transaction result))}))))))
                                ;; :final is the SelfHealRoute CoordinationPhase for
                                ;; the coordinate call after the loop (mathlib4
                                ;; darktower d6f4b88ca3); the coordinator's own
                                ;; :phase, when it gives one, is kept over it.
                                (settle (coordinate-repair durable transaction ports
                                                           authority-verifier)
                                        :final receipts))))]
                    (loop [attempt-durable durable attempt-transaction transaction
                           continued []]
                      (let [result (run-attempt attempt-durable attempt-transaction)
                            reconsidered (when (= :acceptance-test-unsatisfiable
                                                  (:reason result))
                                           (prepare-acceptance
                                            (-> opts
                                                (dissoc :acceptance-test)
                                                (assoc :meta-acceptance-unsatisfiable-claim
                                                       {:attempt (:attempt result)
                                                        :acceptance-test
                                                        (get-in result [:transaction :work-order
                                                                        :acceptance-test])
                                                        :author-evidence
                                                        (get-in (coordinator/current-receipt-for
                                                                 (:transaction result) :author)
                                                                [:payload :evidence])}))
                                            (:meta-diagnosis-observation opts)))
                            next-durable (when (and (= :held (:status result))
                                                    (= :author-failed (:reason result))
                                                    (= :persisted (get-in result [:ledger-write :status]))
                                                    (= :persisted (get-in result [:transaction-write :status])))
                                           (load-durable ledger-path authority-verifier))
                            injury-id (:injury-id result)
                            reopened (get-in (effective-active
                                              (:ledger next-durable) authority-verifier)
                                             [:injuries injury-id])]
                        (if reconsidered
                          (assoc result :status :acceptance-reconsidered
                                 :acceptance-observation reconsidered
                                 :attempt-unchanged true)
                        (if (and (= :loaded (:status next-durable))
                                 (= :reopened (:state reopened)))
                          ;; F32 wake fix (claude-5, 2026-10-20): the record of
                          ;; earlier attempts is carried on every return from
                          ;; the continuation. Before this, a click that ran
                          ;; attempts 1 and 2 and then had attempt 2's retirement
                          ;; or attempt 3's preparation refused reported only
                          ;; the refusal. A refused retirement keeps its status
                          ;; and reason in the entry; a retired one keeps the
                          ;; path and attempt as before.
                          (let [retired (retire-stale-transaction!
                                         transaction-path (:transaction result))
                                continued (conj continued
                                                {:attempt (:attempt result)
                                                 :reason :author-failed
                                                 :transaction-retired
                                                 (if (= :refused (:status retired))
                                                   (select-keys retired [:status :reason :path :attempt])
                                                   (select-keys retired [:path :attempt]))})]
                            (if (= :refused (:status retired))
                              (assoc retired :route :dedicated-meta-self-heal
                                     :ordinary-selection-called? false
                                     :continued-from continued)
                              (let [prepared (prepare-repair
                                              next-durable
                                              (assoc (continued-work-order-options
                                                      opts (:attempt reopened))
                                                     :injury-id injury-id)
                                              authority-verifier)
                                    next-transaction (:transaction prepared)
                                    next-diagnosis (if next-transaction
                                                     (diagnosis-transaction-binding opts next-transaction)
                                                     (refusal :meta-self-heal/preparation-refused
                                                              {:prepare (select-keys prepared
                                                                                     [:status :reason :defects :detail])}))
                                    next-acceptance (when (= :valid (:status next-diagnosis))
                                                      (acceptance-transaction-binding opts next-transaction))
                                    next-transaction (or (:transaction next-acceptance)
                                                         next-transaction)
                                    next-binding (or (when-not (= :valid (:status next-diagnosis))
                                                      next-diagnosis)
                                                     next-acceptance)]
                                (if-not (= :valid (:status next-binding))
                                  (assoc next-binding :route :dedicated-meta-self-heal
                                         :ordinary-selection-called? false
                                         :continued-from continued)
                                  (recur next-durable next-transaction continued)))))
                          ;; F32 wake fix (claude-5, 2026-10-20): when both writes
                          ;; persisted after an author failure but the loop does
                          ;; not continue, the click says why. A reload that did
                          ;; not load is a refusal on the result (the reopened
                          ;; entry, if any, waits for a later click); an injury
                          ;; whose latest entry is not :reopened is the ledger's
                          ;; own stop, reported with that entry's state.
                          (cond-> result
                            (seq continued) (assoc :continued-from continued)
                            (and next-durable (not= :loaded (:status next-durable)))
                            (assoc :continuation
                                   (refusal :meta-self-heal/reload-unavailable
                                            {:durable (select-keys next-durable
                                                                   [:status :reason :defects])}))
                            (= :loaded (:status next-durable))
                            (assoc :continuation
                                   (if reopened
                                     {:status :not-continued :reason :entry-not-reopened
                                      :entry (select-keys reopened [:state :attempt :reason])}
                                     {:status :not-continued :reason :injury-not-active
                                      :entry (->> (get-in next-durable [:ledger :entries])
                                                  (filter #(= injury-id (:injury-id %)))
                                                  last
                                                  (#(select-keys % [:state :attempt :reason])))})))))))))))))))))))
