(ns futon2.aif.temporal-update
  "Exact temporal computation and publication on the existing enactment carrier."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.cascade-model-manifest :as manifest]
            [futon2.aif.exact-belief-adapter :as exact]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.lane-futility :as lane]
            [futon2.aif.temporal-input :as temporal])
  (:import [java.nio.file Files FileAlreadyExistsException]
           [java.nio.channels OverlappingFileLockException]
           [java.util UUID]))

(defn absent [reason & [detail]]
  {:status :absent :reason reason :detail detail})

(defn compute
  "Apply the exact kernel once to an admitted event. AIF validity: this adapter
   has the deterministic C3 path-existence observation law, whose Boolean is
   read from the admitted check. Other mechanism classes have no law here and
   are absent, never an invented identity A. B is the retained executed
   transition; finite states are every subset of its declared token domain.
   Zero evidence retains exact-update's refusal, never an initialized posterior."
  [input]
  (cond
    (not= :admitted (:status input)) (absent :temporal-input-not-admitted input)
    (not= :C3 (get-in input [:observed :class])) (absent :observation-law-not-declared)
    :else
    (let [{:keys [previous enacted observed consumed-event-id]} input
          domain (:domain previous)
          states (reduce (fn [xs t] (into xs (map #(conj % t) xs))) [#{}] (sort-by pr-str domain))
          token [(:target enacted) (:produced enacted)]
          prior (get-in previous [:record (if (= :posterior (:basis previous))
                                           :posterior :continuation-belief)])]
      (exact/exact-update
       states #(hash-map (contains? % token) 1)
       #(manifest/pattern-kernel (:transition enacted) %)
       (get-in observed [:result :observed]) prior
       {:model-identity (:model-identity previous) :domain domain
        :occurrence-id (get-in previous [:consumed-at :occurrence-id])
        :click-id (:click-id enacted) :executed-action (:pattern enacted)
        :consumed-event-id consumed-event-id}))))

(defn envelope
  "The published posterior keeps its producing occurrence; the next selection
   must separately cite it in :consumed-at. This does not invent a future click."
  [record]
  (when (= :published (get-in record [:temporal-receipt :status]))
    (merge {:basis :posterior :record (:temporal-posterior record)}
           (select-keys (get-in record [:temporal-posterior :model])
                        [:occurrence-id :domain :model-identity])
           (:temporal-cursor record))))

(defn read-receipt
  "Read the enactment bytes once and verify the courier's byte digest before
   parsing. Retain the path/digest citation beside the posterior envelope.
   :consumed-at later cites the canonical value digest of the exact posterior;
   :publication cites its enclosing file. Absence never yields a posterior."
  [{:keys [record-path digest] :as receipt}]
  (try
    (if (= :absent (:status receipt)) receipt
        (let [bytes (when record-path (Files/readAllBytes (.toPath (io/file record-path))))]
          (if (and (= :published (:status receipt)) bytes digest (= digest (evidence/sha256 bytes)))
            (let [r (edn/read-string (String. bytes "UTF-8"))]
              (if-let [previous (envelope r)]
                (assoc previous :publication (select-keys receipt [:record-path :digest]))
                (or (:temporal-receipt r) (absent :temporal-receipt-missing))))
            (absent :temporal-record-digest-mismatch receipt))))
    (catch Exception _ (absent :temporal-record-unreadable receipt))))

(defn previous-for-click
  "Read the click's actual input receipt, not its candidate. The first enactment
   of a flight may start from its retained v3 initialization; later clicks need
   the previous posterior explicitly retained by their selection input. This
   function does not authorize temporal consumption by selection."
  [flight click run-record attempt]
  (let [cert (get-in run-record [:decision :selection-certificate])
        receipt (:token-belief-input cert) stage (:token-belief-stage cert)
        supplied (:temporal-previous receipt)
        start (when (and (empty? (:enactments flight))
                         (= :wm/token-belief-input-v3 (:schema receipt)))
                (let [id (evidence/value-digest [:initial-evidence (:occurrence-id receipt) (:initialization receipt)])]
                  {:basis :declared-initialization :trajectory-start? true
                   :record receipt :stage stage :initialization-authority (:initialization receipt)
                   :occurrence-id (:occurrence-id receipt)
                   :domain (get-in stage [:prospective-carry :universe])
                   :model-identity (:model-identity attempt)
                   :initial-event-id id :consumed-event-ids #{id}}))
        previous (or supplied start)]
    (when previous
      (assoc previous :consumed-at {:click-id (:click-id click)
                                    :occurrence-id (:occurrence-id receipt)
                                    :citation (evidence/value-digest (:record previous))}))))

(defn- history [dir flight-id]
  (try
    (let [directory (io/file dir) files (.listFiles directory)]
      (when (and (.exists directory) (nil? files))
        (throw (ex-info "Cannot enumerate temporal authority" {})))
      {:records (->> files
                   (filter #(.endsWith (.getName %) ".edn"))
                   (map #(edn/read-string (slurp %)))
                   (filter #(and (= :wm/enactment-v1 (:schema %)) (= flight-id (:flight %))))
                   doall)})
    (catch Exception _ {:error (absent :temporal-history-unreadable)})))

(defn- finalize-record [record previous records]
  (let [attempts (:attempts record)
        id (when (= 1 (count attempts)) (temporal/event-id (first attempts) (:check (first attempts))))
        published (filter #(= :published (get-in % [:temporal-receipt :status])) records)
        cursors (map :temporal-cursor published)
        latest-record (last (sort-by #(count (get-in % [:temporal-cursor :consumed-event-ids])) published))
        latest (:temporal-cursor latest-record)
        expected (select-keys previous [:initial-event-id :consumed-event-ids])
        actual (or latest (when (= :declared-initialization (:basis previous)) expected))
        refusal (cond
                  (nil? previous) (absent :no-previous-posterior)
                  (some #(contains? (:consumed-event-ids %) id) cursors)
                  (absent :event-already-consumed {:consumed-event-id id})
                  (some #(= :temporal-contradiction (get-in % [:temporal-receipt :reason])) records)
                  (absent :temporal-contradiction)
                  (or (not= expected actual)
                      (and latest-record
                           (not= (:record previous) (:temporal-posterior latest-record))))
                  (absent :temporal-stale-predecessor {:expected expected :actual actual})
                  (not= 1 (count attempts))
                  (absent :multiple-temporal-steps {:attempts (count attempts)}))
        input (when-not refusal (temporal/temporal-input previous (first attempts) (:check (first attempts))))
        posterior (when (= :admitted (:status input)) (compute input))
        receipt (or refusal
                    (when-not (= :admitted (:status input)) (absent (:reason input) (:detail input)))
                    (when-not (= :ok (:status posterior))
                      (absent (if (= :refused (:status posterior)) :temporal-contradiction
                                  :temporal-update-unavailable) posterior)))
        cursor (when-not receipt (update expected :consumed-event-ids conj id))]
    (cond-> (assoc record :temporal-receipt (or receipt {:status :published}))
      input (assoc :temporal-input input)
      posterior (assoc :temporal-posterior posterior)
      cursor (assoc :temporal-cursor cursor))))

(defn- write-once! [path final]
  (let [file (io/file path) receipt (:temporal-receipt final)
        bytes (.getBytes (pr-str final) "UTF-8")
        tmp (io/file (.getParentFile file) (str ".temporal-" (UUID/randomUUID)))]
    (io/make-parents file)
    (try
      (Files/write (.toPath tmp) bytes (make-array java.nio.file.OpenOption 0))
      ;; An atomic name for a fully written inode; unlike ATOMIC_MOVE this
      ;; cannot replace an existing destination, even when no prior was supplied.
      (Files/createLink (.toPath file) (.toPath tmp))
      {:record final :receipt (assoc receipt :record-path (.getCanonicalPath file)
                                            :digest (evidence/sha256 bytes))}
      (catch FileAlreadyExistsException _
        {:record (edn/read-string (slurp file))
         :receipt (if (= :event-already-consumed (:reason receipt)) receipt
                      (absent :temporal-record-already-exists {:record-path (str path)}))})
      (finally (Files/deleteIfExists (.toPath tmp))))))

(defn publish!
  "Validate the predecessor and write the existing enactment record under the
   SAME cross-process trace/index lock, without appending a trace or touching
   its index. Enactment files are the temporal authority, never overwritten.
   AIF validity: several executed transitions and observations cannot be
   collapsed into this single-update adapter. Same-JVM lock contention prevents
   atomic predecessor validation. Both publish absence,
   never a posterior claim or a gate. No previous envelope also writes only
   absence, so it needs no cursor lock. Reusing an initial cursor after an
   advance is stale, never a restart; contradictions remain recorded."
  [path record previous trace-dir]
  (let [record (dissoc record :temporal-posterior :temporal-input :temporal-cursor :temporal-receipt)
        publish (fn []
                  (let [{:keys [records error]} (history (.getParent (io/file path)) (:flight record))]
                    (write-once! path (if error (assoc record :temporal-receipt error)
                                         (finalize-record record previous records)))))]
    (try
      (if previous (lane/with-index-lock (or trace-dir lane/default-trace-dir) publish)
          (write-once! path (assoc record :temporal-receipt (absent :no-previous-posterior))))
      (catch OverlappingFileLockException _
        (write-once! path (assoc record :temporal-receipt (absent :temporal-publication-busy)))))))
