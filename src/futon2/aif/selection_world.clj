(ns futon2.aif.selection-world
  "Record-only census of the world visible when selection finishes.

  This value is attached after the decision has been computed and is never
  returned to scoring. Every set is a sorted id vector with count and digest;
  failed parts are named in :failures and contribute to :failure-count."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.enumeration-completeness :as enumeration]
            [futon2.aif.mission-registry :as registry])
  (:import [java.security MessageDigest]
           [java.time Instant]))

(defn sha256 [x]
  (let [d (.digest (MessageDigest/getInstance "SHA-256")
                   (.getBytes (str x) "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) d))))

(defn id-set [xs]
  (let [ids (vec (sort (map str (distinct xs))))]
    {:ids ids :count (count ids) :digest (sha256 (pr-str ids))}))

(defn- pattern-manifest [root]
  (let [files (->> (file-seq (io/file root))
                   (filter #(.isFile ^java.io.File %))
                   (filter #(str/ends-with? (.getName ^java.io.File %) ".flexiarg"))
                   (map (fn [f] [(.getCanonicalPath ^java.io.File f)
                                 (sha256 (slurp f))]))
                   sort vec)]
    {:file-count (count files) :digest (sha256 (pr-str files))}))

(defn- default-task-sets [code-root]
  (let [missions (registry/load-missions code-root)
        excursions (registry/load-excursions code-root)
        tickets (registry/load-tickets code-root)]
    {:missions (map :id (registry/open-missions missions))
     :excursions (map :id (filter registry/live-excursion? (:excursions excursions)))
     :tickets (map :id (filter registry/live-ticket? (:tickets tickets)))}))

(defn critical-task-counts
  "Counts of tasks available to the selector for a registered run. Reuse a
  successful selection-world snapshot when present. Runs that terminate before
  selection take a fresh, explicitly terminal-time census rather than claiming
  it was observed at selection. Census failure is retained as data."
  [selection-world opts]
  (let [from-selection (when (and (= :wm/world-at-selection-v1
                                      (:schema selection-world))
                                  (map? (:open-tasks selection-world))
                                  (every? number?
                                          (map #(get-in selection-world
                                                        [:open-tasks % :count])
                                               [:missions :excursions :tickets])))
                         {:basis :selection-world
                          :observed-at (:selection-ended-at selection-world)
                          :available-to-choose
                          (into {}
                                (map (fn [kind]
                                       [kind (get-in selection-world
                                                     [:open-tasks kind :count])]))
                                [:missions :excursions :tickets])})]
    (if from-selection
      (assoc from-selection :schema :wm/critical-task-counts-v1)
      (try
        (let [sets ((or (:world-task-sets-fn opts) default-task-sets)
                    (or (:code-root opts) "/home/joe/code"))
              now-fn (or (:world-now-fn opts) (fn [] (Instant/now)))]
          {:schema :wm/critical-task-counts-v1
           :basis :terminal-fallback
           :observed-at (str (now-fn))
           :available-to-choose
           (into {}
                 (map (fn [kind] [kind (count (distinct (get sets kind [])))]))
                 [:missions :excursions :tickets])})
        (catch Throwable e
          {:schema :wm/critical-task-counts-v1
           :basis :terminal-fallback
           :status :failed
           :error (ex-message e)})))))

(defn- roster-by-type [roster]
  (reduce-kv (fn [m id seat]
               (update m (keyword (name (or (:type seat) (:kind seat) :untyped)))
                       (fnil conj []) (name id)))
             {} (or roster {})))

(defn- live-enumerated-targets
  "Targets which actually entered the live decision field.  The current
  cascade selector retains these under :selection-certificate/:candidates;
  :controller-ranking is the legacy action-ranking carrier and is absent on
  production cascade clicks.  Reading only it produced the impossible receipt
  `0 enumerated` beside a selected policy."
  [decision]
  (let [legacy (mapcat (fn [kind]
                         (enumeration/enumerated-targets kind
                                                         (:controller-ranking decision)))
                       [:mission :excursion :ticket])
        policies (get-in decision [:selection-certificate :candidates])
        live (keep (fn [candidate]
                     (or (:target candidate)
                         (get-in candidate [:policy :target])
                         (get-in candidate [:f-prefix :policy :target])
                         (get-in candidate [:action :target])))
                   policies)]
    (distinct (concat legacy live))))

(defn- outer-task-population
  "Return the exact task field retained by the outer selector.  Support and
  exclusions are both members of the enumerated field: exclusion is a typed
  selection result, not evidence that the task was never seen."
  [outer-selection]
  (when outer-selection
    (when-not (= :wm/outer-task-selection-v1 (:schema outer-selection))
      (throw (ex-info "Outer task selection has the wrong schema"
                      {:schema (:schema outer-selection)})))
    (let [rows (vec (concat (:support outer-selection)
                            (:excluded outer-selection)))
          invalid (filterv #(or (not (some? (:id %)))
                                (not (contains? #{:mission :excursion :ticket}
                                                (:kind %))))
                           rows)
          ids (mapv (comp str :id) rows)]
      (when (seq invalid)
        (throw (ex-info "Outer task selection contains malformed task rows"
                        {:invalid-count (count invalid)})))
      (when-not (= (count ids) (count (distinct ids)))
        (throw (ex-info "Outer task selection contains duplicate task ids"
                        {:task-count (count ids)
                         :distinct-count (count (distinct ids))})))
      rows)))

(defn- population-task-sets [population]
  (reduce (fn [sets {:keys [id kind]}]
            (update sets (keyword (str (name kind) "s")) conj (str id)))
          {:missions [] :excursions [] :tickets []}
          population))

(defn capture
  "Capture after DECISION is final. Dependencies are injectable for hermetic
  tests. A failed part is explicit and does not throw into the runner."
  ([decision roster interpretation-ask opts]
   (capture decision roster interpretation-ask nil opts))
  ([decision roster interpretation-ask outer-selection opts]
  (let [failures (atom [])
        part (fn [name f]
               (try (f) (catch Throwable e
                          (swap! failures conj {:part name :status :failed
                                                :error (ex-message e)})
                          {:status :failed :part name})))
        code-root (or (:code-root opts) "/home/joe/code")
        outer-population (part :outer-task-population
                               #(outer-task-population outer-selection))
        retained-outer-population? (and outer-selection
                                        (not= :failed (:status outer-population)))
        task-sets (part :open-tasks
                        #(if retained-outer-population?
                           (population-task-sets outer-population)
                           ((or (:world-task-sets-fn opts)
                                default-task-sets) code-root)))
        enumerated (part :enumerated-tasks
                         #(if retained-outer-population?
                            (map :id outer-population)
                            (live-enumerated-targets decision)))
        library (part :pattern-library
                      #((or (:world-pattern-manifest-fn opts) pattern-manifest)
                        (or (:pattern-library-root opts)
                            (str code-root "/futon3/library"))))
        seats (part :seat-roster #(if (map? roster)
                                    (roster-by-type roster)
                                    (throw (ex-info "Agency roster unavailable" {}))))
        task-carrier (if (= :failed (:status task-sets)) task-sets
                       (into {} (map (fn [[k ids]] [k (id-set ids)]) task-sets)))
        enumerated-carrier (if (= :failed (:status enumerated)) enumerated
                               (id-set enumerated))
        seat-carrier (if (= :failed (:status seats)) seats
                         (into {} (map (fn [[k ids]] [k (id-set ids)]) seats)))
        enumerator-source "src/futon2/aif/mission_registry.clj"
        now-fn (or (:world-now-fn opts) (fn [] (Instant/now)))
        inputs {:open-tasks task-carrier :enumerated enumerated-carrier
                :pattern-library library :seat-roster seat-carrier
                :decision-input (select-keys decision
                                             [:controller-ranking :proposal-supply
                                              :mission-hole-coverage])
                :outer-task-population
                (when retained-outer-population?
                  (select-keys outer-selection [:schema :support :excluded]))}]
    {:schema :wm/world-at-selection-v1
     :open-tasks task-carrier
     :enumerator {:source enumerator-source
                  :code-sha256 (part :enumerator-code
                                     #(sha256 (slurp enumerator-source)))}
     :enumerated-tasks enumerated-carrier
     :pattern-library library
     :seat-roster seat-carrier
     :selection-input-digest (sha256 (pr-str inputs))
     :selection-ended-at (str (now-fn))
     :interpretation-issued-at (when (and interpretation-ask
                                           (not= :absent (:status interpretation-ask)))
                                  (or (:issued-at interpretation-ask)
                                      (:requested-at interpretation-ask)
                                      (:at interpretation-ask)
                                      (str (now-fn))))
     :failure-count (count @failures)
     :failures @failures})))
