(ns futon2.aif.receipt-construction
  "One receipt -> find -> ruled organise -> fold carrier. No retrieval or dispatch.
  Prior construction is read from closed attempt manifests, never joined by names alone."
  (:refer-clojure :exclude [bytes])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.cascade-policy :as policy]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.evidence-manifest :as manifest]
            [futon2.aif.find-receipt :as finder]
            [futon2.aif.find-expectations :as find-expectations]
            [futon2.aif.find-designation :as find-designation]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.shadow-cascade-g :as shadow])
  (:import [java.nio.file Files]
           [java.time Instant]))

(defn- need! [ok reason data]
  (when-not ok (throw (ex-info "Receipted construction refused"
                              (merge {:interpretation/refusal :interpretation/invalid-receipt
                                      :construction/refusal reason} data)))))
(defn- bytes [file] (Files/readAllBytes (.toPath (io/file file))))
(defn- read-one [file]
  (with-open [r (java.io.PushbackReader. (io/reader file))]
    (let [eof (Object.) x (edn/read {:eof eof} r)]
      (need! (and (map? x) (identical? eof (edn/read {:eof eof} r))) :invalid-history-record {:file (str file)}) x)))
(defn reaches? [edges a b]
  (let [rel (reduce (fn [m [u v]] (update m u (fnil conj #{}) v)) {} edges)]
    (loop [frontier (get rel a #{}) seen #{}]
      (cond (contains? frontier b) true (empty? frontier) false
            :else (recur (set/difference (set (mapcat #(get rel % #{}) frontier)) seen)
                         (into seen frontier))))))
(defn precedence [carrier edges]
  (loop [remaining carrier order []]
    (if (empty? remaining) order
        (let [ready (first (sort (filter (fn [u] (not-any? #(reaches? edges u %) remaining)) remaining)))]
          (need! (some? ready) :precedence-cycle {:remaining remaining})
          (recur (disj remaining ready) (conj order ready))))))
(defn validate-admissible! [order nodes edges]
  (let [firing (vec (filter nodes order))]
    (doseq [i (range (count firing)) j (range (inc i) (count firing))]
      (need! (not (reaches? edges (nth firing i) (nth firing j)))
             :inadmissible-order {:earlier (nth firing i) :later (nth firing j)})))
  true)
(defn acting-order
  "P10 continuing guards: every step rechecks every guard on the current
   facts, and the acting pattern is the first id in order whose guard is
   literal true and whose effect is not already achieved (every [k v] in
   :effect already has (= v (get facts k))). Effects are add-only: any false
   value is refused up front with :retracting-effect-forbidden; withdrawal is
   expressed as a new token the guards forbid. With add-only effects each
   step adds at least one new true fact, so the loop terminates (the set of
   true facts strictly grows and is bounded by the fact universe)."
  [interpretations q0 order]
  (doseq [id order]
    (need! (contains? interpretations id) :previous-or-admitted-interpretation-missing {:pattern id})
    (doseq [[k v] (:effect (get interpretations id))]
      (need! (not (false? v)) :retracting-effect-forbidden {:pattern id :fact k})))
  (loop [facts q0 acted []]
    (if-let [id (first (filter #(and (true? (finder/guard-value facts (:guard (get interpretations %))))
                                     (not-every? (fn [[k v]] (= v (get facts k)))
                                                 (:effect (get interpretations %))))
                              order))]
      (recur (merge facts (:effect (get interpretations id))) (conj acted id))
      acted)))

(defn- history-read! [file]
  (try (read-one file)
       (catch Exception e
         (throw (ex-info "History record cannot be read"
                         {:interpretation/refusal :interpretation/invalid-receipt
                          :construction/refusal :history-discovery-invalid
                          :reason :unreadable-or-malformed-record :file (str file)} e)))))

(defn- history-time! [value file]
  (try (Instant/parse value)
       (catch Exception e
         (throw (ex-info "History ordering is unavailable"
                         {:interpretation/refusal :interpretation/invalid-receipt
                          :construction/refusal :history-discovery-invalid
                          :reason :ordering-unavailable :file (str file) :value value} e)))))

(defn- history-root! [root]
  (need! (or (instance? java.io.File root) (and (string? root) (not (str/blank? root))))
         :history-discovery-invalid {:reason :invalid-search-root :file (str root)})
  (try (.getCanonicalPath (io/file root))
       (catch java.io.IOException e
         (throw (ex-info "History root cannot be resolved"
                         {:interpretation/refusal :interpretation/invalid-receipt
                          :construction/refusal :history-discovery-invalid
                          :reason :root-unresolvable :file (str root)} e)))))

(defn- history-children! [directory]
  (let [children (.listFiles (io/file directory))]
    (need! (some? children) :history-discovery-invalid
           {:reason :root-or-directory-unreadable :file (str directory)})
    children))

(defn- discover-closes! [roots]
  ;; K8 supports only the producer layout and its explicit archive container.
  ;; A recognized attempt may be open; an unknown nested directory is not one.
  (let [closes (atom #{}) layouts (atom []) auxiliary (atom [])]
    (letfn [(children! [root dir]
              (mapv (fn [file]
                      (need! (and (not (Files/isSymbolicLink (.toPath file)))
                                  (.startsWith (.toPath (.getCanonicalFile file))
                                               (.toPath (io/file root))))
                             :history-discovery-invalid
                             {:reason :unsupported-history-link-or-escape :file (str file)})
                      (need! (or (.isDirectory file) (.isFile file)) :history-discovery-invalid
                             {:reason :unclassifiable-history-entry :file (str file)})
                      file)
                    (history-children! dir)))
            (auxiliary! [root dir]
              (let [visited (atom [])]
                (letfn [(scan! [entry]
                          (need! (and (not (and (.isDirectory entry) (str/starts-with? (.getName entry) "attempt-")))
                                      (not= "007-closed.edn" (.getName entry)))
                                 :history-discovery-invalid
                                 {:reason :unsupported-history-layout :file (str entry)})
                          (swap! visited conj (str entry))
                          (when (.isDirectory entry)
                            (doseq [child (children! root entry)] (scan! child))))]
                  (scan! dir)
                  (swap! auxiliary conj {:path (str dir) :reason :complete-no-attempt-no-close-coverage
                                         :visited @visited}))))
            (cohort-or-auxiliary! [root dir]
              (need! (not (str/starts-with? (.getName dir) "attempt-"))
                     :history-discovery-invalid {:reason :unsupported-history-layout :file (str dir)})
              (if (some #(and (.isDirectory %) (re-matches #"attempt-\d{3}" (.getName %)))
                        (children! root dir))
                (walk! root dir :cohort)
                (auxiliary! root dir)))
            (walk! [root dir stage]
              (doseq [file (children! root dir)]
                (if (.isDirectory file)
                  (case stage
                    :root (if (= "archives" (.getName file))
                            (walk! root file :archives)
                            (cohort-or-auxiliary! root file))
                    :archives (do (swap! layouts conj {:root root :layout :archive :path (str file)})
                                  (walk! root file :archive-group))
                    :archive-group (cohort-or-auxiliary! root file)
                    :cohort (if (re-matches #"attempt-\d{3}" (.getName file))
                              (walk! root file :attempt)
                              (auxiliary! root file))
                    :attempt (auxiliary! root file))
                  (when (= "007-closed.edn" (.getName file))
                    (need! (= :attempt stage) :history-discovery-invalid
                           {:reason :misplaced-closed-history :file (str file)})
                    (swap! closes conj (.getCanonicalPath file))))))]
      (doseq [root roots]
        (swap! layouts conj {:root root :layout :direct :path root})
        (walk! root (io/file root) :root))
      {:files (sort @closes) :layouts @layouts :auxiliary-exclusions @auxiliary})))

(defn- distinct-history-identities! [files]
  (let [rows (mapv (fn [path]
                     (let [closed (history-read! path)
                           id (select-keys closed [:cohort/id :attempt/id])
                           dir (.getParentFile (io/file path))]
                       (need! (and (keyword? (:cohort/id id))
                                   (string? (:attempt/id id)) (not (str/blank? (:attempt/id id))))
                              :history-discovery-invalid {:reason :history-identity-unavailable :file path})
                       {:path path :identity id
                        :checkpoints (into (sorted-map)
                                           (map-indexed
                                            (fn [i checkpoint]
                                              (let [f (io/file dir (format "%03d-%s.edn" (inc i) (name checkpoint)))]
                                                [(.getName f) (when (.exists f)
                                                               (history-read! f)
                                                               (evidence/sha256 (bytes f)))]))
                                            cohort/checkpoint-order))})) files)]
    (doseq [[id group] (group-by :identity rows) :when (> (count group) 1)]
      (let [identical? (apply = (map :checkpoints group))]
        (need! identical? :history-discovery-invalid
               {:reason :history-identity-collision
                :file (:path (first group)) :identity id :records (vec group)
                :identical-checkpoint-sets? identical?})))
    rows))

(defn- discovery-target! [value file path]
  (need! (or (keyword? value) (and (string? value) (not (str/blank? value))))
         :history-discovery-invalid {:reason :target-unavailable :file (str file) :path path :value value})
  {:file (str file) :path path :value value :type (if (keyword? value) :keyword :string)
   :comparison (if (keyword? value) (str value) value)})

(defn- rejected-construction? [construction]
  (let [sorry (get-in construction [:payload :sorry])]
    (and (= :invalid-checkpoint-cell (:kind sorry))
         (= :construction (:refused-checkpoint sorry)))))

(defn- relevance-evidence! [{:keys [closed construction selection close-file construction-file selection-file target-evidence]}]
  (let [dir (.getParentFile (io/file close-file))
        sources (cond-> [[3 :construction construction construction-file] [7 :closed closed close-file]]
                  selection (conj [2 :selection selection selection-file]))
        identity (select-keys closed [:cohort/id :attempt/id :attempt/ordinal])
        fail! (fn [ok reason file]
                (need! ok :history-discovery-invalid {:reason reason :file (str file)}))]
    (fail! (and (keyword? (:cohort/id closed))
                (= (name (:cohort/id closed)) (.getName (.getParentFile dir)))
                (= (:attempt/id closed) (.getName dir)) (pos-int? (:attempt/ordinal closed)))
           :relevance-identity-invalid close-file)
    (doseq [[index checkpoint record file] sources]
      (fail! (and (map? record)
                  (= #{:event/schema-version :cohort/id :attempt/id :attempt/ordinal
                       :event/sequence :checkpoint/type :recorded-at :payload} (set (keys record)))
                  (= 1 (:event/schema-version record)) (= index (:event/sequence record))
                  (= checkpoint (:checkpoint/type record))
                  (= identity (select-keys record (keys identity)))
                  (or (and (map? (get-in record [:payload :judgment]))
                           (some? (get-in record [:payload :ground]))
                           (not (contains? (:payload record) :sorry)))
                      (and (= :construction checkpoint) (rejected-construction? record)
                           (= #{:sorry} (set (keys (:payload record))))
                           (= #{:kind :outcome :refused-checkpoint :cell-errors}
                              (set (keys (get-in record [:payload :sorry]))))
                           (= :incomplete (get-in record [:payload :sorry :outcome]))
                           (vector? (get-in record [:payload :sorry :cell-errors])))))
             :relevance-checkpoint-invalid file))
    (let [times (mapv (fn [[_ _ record file]] (history-time! (:recorded-at record) file)) (sort-by first sources))]
      (fail! (every? (fn [[a b]] (not (.isAfter ^Instant a b))) (partition 2 1 times))
             :relevance-ordering-invalid close-file))
    (doseq [[record path file] [[closed [:payload :close-retention :occurrence] close-file]
                               [construction [:payload :judgment :receipted-construction :identity :occurrence] construction-file]]
            :let [occurrence (get-in record path)] :when (some? occurrence)]
      (fail! (and (map? occurrence)
                  (= (str (:cohort/id closed)) (:cohort/id occurrence))
                  (= (:attempt/id closed) (:attempt/id occurrence)))
             :relevance-occurrence-identity-conflict file))
    (let [carrier (get-in construction [:payload :judgment :receipted-construction])
          prior-id (:identity carrier)]
      (when (contains? carrier :cascade-diff-sha256)
        (fail! (= (:cascade-diff-sha256 carrier) (evidence/value-digest (:cascade-diff carrier)))
               :relevance-diff-binding-mismatch construction-file))
      (when (contains? prior-id :start-event-sha256)
        (let [start-file (io/file dir "001-time-step.edn")]
          (history-read! start-file)
          (fail! (= (:start-event-sha256 prior-id) (evidence/sha256 (bytes start-file)))
                 :relevance-start-binding-mismatch start-file))))
    (when (contains? (:payload closed) :close-evidence-manifest)
      (let [m (get-in closed [:payload :close-evidence-manifest])]
        (try
          (manifest/validate-manifest m)
          (when (contains? (:payload closed) :close-retention)
            (manifest/verify-retention-agreement m (get-in closed [:payload :close-retention])))
          (doseq [[_ checkpoint _ file] sources :when (not= :closed checkpoint)]
            (fail! (some #(= (.getCanonicalPath (io/file (:source-path %))) (.getCanonicalPath file)) (:entries m))
                   :relevance-manifest-binding-missing file))
          (doseq [entry (:entries m)]
            (fail! (= (:sha256 entry) (evidence/sha256 (bytes (:source-path entry))))
                   :relevance-manifest-source-mismatch (:source-path entry)))
          (catch clojure.lang.ExceptionInfo e
            (throw (ex-info "Relevance evidence refused"
                            (merge (ex-data e) {:interpretation/refusal :interpretation/invalid-receipt
                                                :construction/refusal :history-discovery-invalid
                                                :file (str close-file)}) e)))
          (catch java.io.IOException e
            (throw (ex-info "Relevance source unreadable"
                            {:interpretation/refusal :interpretation/invalid-receipt
                             :construction/refusal :history-discovery-invalid
                             :reason :relevance-source-unreadable :file (str close-file)} e))))))
    {:reason :producer-recorded-different-target :identity identity :target-evidence target-evidence
     :records (mapv (fn [[_ _ _ file]] {:file (.getCanonicalPath file)
                                       :sha256 (evidence/sha256 (bytes file))}) sources)}))

(defn- non-construction! [close-file closed construction]
  (let [dir (.getParentFile (io/file close-file))
        files (mapv #(io/file dir (format "%03d-%s.edn" %1 (name %2)))
                    (range 1 8) cohort/checkpoint-order)
        records (mapv (fn [file]
                        (case (.getName file)
                          "003-construction.edn" construction
                          "007-closed.edn" closed
                          (history-read! file))) files)
        ordinal (:attempt/ordinal closed)
        identity (select-keys closed [:cohort/id :attempt/id :attempt/ordinal])
        fail! (fn [ok reason file]
                (need! ok :history-discovery-invalid {:reason reason :file (str file)}))
        close-judgment (get-in closed [:payload :judgment])
        outcome (:outcome close-judgment)]
    (fail! (and (keyword? (:cohort/id closed))
                (= (name (:cohort/id closed)) (.getName (.getParentFile dir)))
                (= (:attempt/id closed) (.getName dir))
                (pos-int? ordinal)) :non-construction-identity close-file)
    (doseq [[index checkpoint record file] (map vector (range 1 8) cohort/checkpoint-order records files)]
      (fail! (and (= #{:event/schema-version :cohort/id :attempt/id :attempt/ordinal
                      :event/sequence :checkpoint/type :recorded-at :payload} (set (keys record)))
                  (= 1 (:event/schema-version record))
                  (= identity (select-keys record (keys identity)))
                  (= index (:event/sequence record)) (= checkpoint (:checkpoint/type record))
                  (or (and (map? (get-in record [:payload :sorry]))
                           (keyword? (get-in record [:payload :sorry :kind])))
                      (and (map? (get-in record [:payload :judgment]))
                           (some? (get-in record [:payload :ground]))))
                  (empty? (cohort/checkpoint-cell-errors {} checkpoint (:payload record))))
             :non-construction-checkpoint-invalid file))
    (let [times (mapv #(history-time! (:recorded-at %1) %2) records files)]
      (fail! (every? (fn [[a b]] (not (.isAfter ^Instant a b))) (partition 2 1 times))
             :non-construction-ordering close-file))
    ;; These exact sorries are emitted by close-core! for missing checkpoints.
    ;; A later grounded checkpoint would contradict not having constructed.
    (doseq [index (range 2 6)]
      (let [record (nth records index) checkpoint (nth cohort/checkpoint-order index)]
        (fail! (= {:sorry {:kind (keyword (str "not-reached-" (name checkpoint))) :outcome outcome}}
                  (:payload record)) :non-construction-marker-contradiction (nth files index))))
    (fail! (and (contains? cohort/outcome-kinds outcome)
                (not (#{:grounded-change :grounded-no-change :artifact-only
                        :historical-verification-awaiting-validation} outcome))
                (false? (:grounded? close-judgment)) (false? (:artifact-only? close-judgment))
                (nil? (:witness close-judgment))
                (not-any? #(contains? close-judgment %) [:cascade :receipted-construction :construction :commit])
                (= {:kind :full-loop-outcome :attempt-id (:attempt/id closed)} (get-in closed [:payload :ground])))
           :non-construction-close-contradiction close-file)
    (let [selection (nth records 1)
          cell (:payload selection)
          judgment (:judgment cell)
          action (:selected-action judgment)
          target (:target action)
          ;; action-proposer emits these two targetless forms. The runner's
          ;; selected-mission is a display of target-class/type, not a target.
          targetless? (and (map? action) (not (contains? action :target))
                           (case (:type action)
                             :no-op (not (contains? action :target-class))
                             :learn-action-class (contains? (disj fm/action-types :no-op :learn-action-class)
                                                            (:target-class action))
                             false))
          selection-label (if targetless? (or (:target-class action) (:type action)) target)
          retained (get-in closed [:payload :close-retention])]
      (fail! (or (and (= #{:judgment :ground} (set (keys cell)))
                       (map? action) (keyword? (:type action))
                       (or targetless? (and (not (#{:no-op :learn-action-class} (:type action)))
                                                  (or (keyword? target) (and (string? target) (not (str/blank? target))))))
                       (or (not (contains? judgment :selected-mission))
                           (and (not targetless?) (nil? (:selected-mission judgment)))
                           (= (:comparison (discovery-target! selection-label (nth files 1) [:payload :judgment :selected-action]))
                              (:comparison (discovery-target! (:selected-mission judgment) (nth files 1) [:payload :judgment :selected-mission])))))
                  (and (= #{:sorry} (set (keys cell))) (nil? judgment) (map? (:sorry cell))
                       (#{:no-selection :not-reached-selection} (get-in cell [:sorry :kind]))))
             :non-construction-selection-invalid (nth files 1))
      (when (= :present (get-in close-judgment [:outcome-entity :status]))
        (fail! (and target (= (:comparison (discovery-target! target (nth files 1) [:selected-action :target]))
                                             (:comparison (discovery-target! (get-in close-judgment [:outcome-entity :entity/id]) close-file [:outcome-entity :entity/id]))))
               :non-construction-target-contradiction close-file))
      (when (contains? (:payload closed) :close-retention)
        (try (retention/validate-retention-block retained)
             (catch clojure.lang.ExceptionInfo e
               (throw (ex-info "Invalid non-construction retention"
                               (merge (ex-data e) {:interpretation/refusal :interpretation/invalid-receipt
                                                   :construction/refusal :history-discovery-invalid
                                                   :reason :non-construction-retention-invalid :file (str close-file)}) e))))
        (fail! (and (= (str (:cohort/id closed)) (get-in retained [:occurrence :cohort/id]))
                    (= (:attempt/id closed) (get-in retained [:occurrence :attempt/id]))
                    (= action (get-in retained [:occurrence :action/value]))
                    (= (history-time! (:closed-at retained) close-file)
                       (history-time! (:recorded-at closed) close-file)))
               :non-construction-retention-identity close-file))
      (when (contains? (:payload closed) :close-evidence-manifest)
        (let [m (get-in closed [:payload :close-evidence-manifest])]
          (try
            (manifest/validate-manifest m)
            (manifest/verify-retention-agreement m retained)
            (doseq [file (butlast files)]
              (let [entry (first (filter #(= (.getCanonicalPath file)
                                             (.getCanonicalPath (io/file (:source-path %)))) (:entries m)))]
                (fail! (some? entry) :non-construction-manifest-binding-missing file)))
            (doseq [entry (:entries m)]
              (fail! (not (.isAfter (history-time! (:admitted-at entry) close-file)
                                   (history-time! (:recorded-at closed) close-file)))
                     :non-construction-manifest-after-close close-file)
              (fail! (= (:sha256 entry) (evidence/sha256 (bytes (:source-path entry))))
                     :non-construction-manifest-source-mismatch (:source-path entry)))
            (catch clojure.lang.ExceptionInfo e
              (throw (ex-info "Invalid non-construction manifest"
                              (merge (ex-data e) {:interpretation/refusal :interpretation/invalid-receipt
                                                  :construction/refusal :history-discovery-invalid
                                                  :file (str close-file)}) e)))
            (catch java.io.IOException e
              (throw (ex-info "Unreadable non-construction manifest source"
                              {:interpretation/refusal :interpretation/invalid-receipt
                               :construction/refusal :history-discovery-invalid
                               :reason :non-construction-manifest-source-unreadable :file (str close-file)} e)))))))
    {:non-construction? true :closed-at (history-time! (:recorded-at closed) close-file)
      :exclusion {:reason :producer-not-reached-construction :identity identity
                 :selection-evidence {:file (str (nth files 1))
                                      :path [:payload :judgment]
                                      :value (get-in (nth records 1) [:payload :judgment])}
                 :target-evidence
                 (vec (keep (fn [[record file path]]
                              (let [value (get-in record path ::absent)]
                                (when (and (not= ::absent value) (some? value))
                                  (discovery-target! value file path))))
                            [[(nth records 1) (nth files 1) [:payload :judgment :selected-action :target]]
                             [(when (some? (get-in (nth records 1) [:payload :judgment :selected-action :target]))
                                (nth records 1)) (nth files 1) [:payload :judgment :selected-mission]]
                             [closed close-file [:payload :close-retention :occurrence :action/value :target]]
                             [closed close-file [:payload :judgment :outcome-entity :entity/id]]]))
                 :records (mapv (fn [file] {:file (.getCanonicalPath file)
                                            :sha256 (evidence/sha256 (bytes file))}) files)}}))

(defn- closed-candidate! [close-file]
  (let [closed (history-read! close-file)
        dir (.getParentFile (io/file close-file))
        construction-file (io/file dir "003-construction.edn")
        construction (when (.exists construction-file) (history-read! construction-file))
        selection-file (io/file dir "002-selection.edn")
        selection (when (.exists selection-file) (history-read! selection-file))
        occurrence (get-in closed [:payload :close-retention :occurrence])
        at (history-time! (:recorded-at closed) close-file)]
    (if (= :not-reached-construction (get-in construction [:payload :sorry :kind]))
      (non-construction! close-file closed construction)
      (let [missing (Object.)
            fields (for [[file record paths]
                         [[construction-file construction [[:payload :judgment :mission]
                                                            [:payload :ground :selected-action :target]
                                                            [:payload :judgment :cascade :selected-action :target]
                                                            [:payload :judgment :receipted-construction :identity :occurrence :action/value :target]]]
                          [selection-file selection [[:payload :judgment :selected-mission]
                                                     [:payload :judgment :selected-action :target]
                                                     [:payload :ground :selected-action :target]]]
                          [close-file closed [[:payload :close-retention :occurrence :action/value :target]
                                              [:payload :judgment :outcome-entity :entity/id]
                                              [:payload :judgment :entity-state-at-close :entity/id]]]]
                         path paths :let [value (get-in record path missing)]
                         :when (not (identical? missing value))]
                     (discovery-target! value file path))
            target-evidence (vec fields)
            targets (mapv :comparison target-evidence)]
        (when-let [retained-time (get-in closed [:payload :close-retention :closed-at])]
          (need! (= at (history-time! retained-time close-file)) :history-discovery-invalid
                 {:reason :contradictory-ordering :file (str close-file)}))
        (need! (seq targets) :history-discovery-invalid {:reason :target-unavailable :file (str close-file)})
        (need! (apply = targets) :history-discovery-invalid
               {:reason :contradictory-targets :file (str close-file) :target-evidence target-evidence})
        {:closed closed :construction construction :close-file close-file :construction-file construction-file
         :selection selection :selection-file selection-file :target-evidence target-evidence
         :occurrence occurrence :target (first targets) :closed-at at}))))

(defn- validated-previous! [{:keys [closed construction close-file construction-file] :as latest} roots]
  (try
      (let [_ (when (rejected-construction? construction)
                  (relevance-evidence! latest)
                  (need! false :previous-construction-rejected
                         {:file (str construction-file)
                          :history/construction-sha256 (evidence/sha256 (bytes construction-file))
                          :history/rejection (get-in construction [:payload :sorry])
                          :history/target-evidence (:target-evidence latest)}))
            _ (need! (:occurrence latest) :previous-occurrence-unavailable {:file (str close-file)})
            block (get-in closed [:payload :close-retention])
            m (get-in closed [:payload :close-evidence-manifest])
            retained (get-in construction [:payload :judgment :receipted-construction])
            _ (need! (map? (get-in construction [:payload :judgment :cascade]))
                     :previous-cascade-unavailable {:file (str construction-file)})
            _ (need! (map? retained) :previous-cascade-carrier-unavailable {:file (str construction-file)})
            prior-id (:identity retained)
            start-file (io/file (.getParentFile (io/file close-file)) "001-time-step.edn")
            start (history-read! start-file)
            root (.getCanonicalPath (.getParentFile (.getParentFile (.getParentFile (io/file close-file)))))
            d (:cascade-diff retained)]
        (retention/validate-retention-block block)
        (manifest/validate-manifest m)
        (manifest/verify-retention-agreement m block)
        (doseq [file [construction-file start-file]]
          (let [entry (first (filter #(= (.getCanonicalPath (io/file (:source-path %))) (.getCanonicalPath file)) (:entries m)))]
            (need! (and entry (= (:sha256 entry) (evidence/sha256 (bytes file))))
                   :previous-manifest-source-mismatch {:file (str file)})))
        (evidence/validate-identity prior-id)
        (need! (and (= (:occurrence prior-id) (:occurrence block))
                     (= (:semantic-epoch prior-id) (get-in start [:payload :judgment :semantic-epoch]))
                     (= (:data-root prior-id) root)
                     (= (:start-event-sha256 prior-id) (evidence/sha256 (bytes start-file)))
                     (= (get-in prior-id [:occurrence :cohort/id]) (str (:cohort/id construction)))
                     (= (get-in prior-id [:occurrence :attempt/id]) (:attempt/id construction)))
               :previous-occurrence-mismatch {:file (str construction-file)})
        (need! (= (:cascade-diff-sha256 retained) (evidence/value-digest d)) :previous-diff-digest-mismatch {})
        (need! (and (set? (:nodes d)) (vector? (:precedence-after d))
                     (vector? (:acting-order-after d))
                     (map? (get-in d [:provenance :admissions]))
                     (= (:admitted-by d) (set (keys (get-in d [:provenance :admissions])))))
               :previous-admission-carrier-invalid {})
        {:cascade {:nodes (:nodes d) :edges (:organised-edges d) :precedence (:precedence-after d)}
         ;; The acting order the previous attempt actually recorded; reused as
         ;; this attempt's before arm instead of being re-simulated.
         :acting-order (:acting-order-after d)
         :admitted (get-in d [:provenance :admissions])
         :admission-reason :carried-from-previous-occurrence
         :provenance {:identity prior-id :searched-roots roots :construction-file (.getCanonicalPath construction-file)
                      :construction-sha256 (evidence/sha256 (bytes construction-file))
                      :close-file (.getCanonicalPath (io/file close-file))
                      :close-sha256 (evidence/sha256 (bytes close-file))}})
    (catch clojure.lang.ExceptionInfo e
      (throw (ex-info "Previous construction validation refused"
                      (merge {:interpretation/refusal :interpretation/invalid-receipt
                              :construction/refusal :previous-evidence-invalid}
                             (ex-data e)
                             {:history/close-file (str close-file)
                              :history/construction-file (str construction-file)}) e)))))

(defn previous!
  "Latest relevant closed history in explicit roots, or established absence.
  Damaged or unclassifiable history refuses; missing modern evidence never resets."
  [identity roots]
  (let [requested-target (discovery-target! (get-in identity [:occurrence :action/value :target])
                                           :current-identity [:occurrence :action/value :target])
        target (:comparison requested-target)
        before (history-time! (get-in identity [:occurrence :action-at]) :current-identity)
        roots (vec (distinct (map history-root! roots)))
        _ (need! (seq roots) :history-discovery-invalid {:reason :search-roots-unavailable})
        discovered (discover-closes! roots)
        history-identities (distinct-history-identities! (:files discovered))
        candidates (mapv #(closed-candidate! (io/file %)) (:files discovered))
        earlier-all (filter #(.isBefore ^Instant (:closed-at %) before) candidates)
        earlier (remove :non-construction? earlier-all)
        exclusions (into (mapv :exclusion (filter :non-construction? earlier-all))
                         (map relevance-evidence! (filter #(not= target (:target %)) earlier)))
        ordered (sort-by :closed-at (filter #(= target (:target %)) earlier))
        _ (when (and (> (count ordered) 1)
                     (= (:closed-at (last ordered)) (:closed-at (last (butlast ordered)))))
            (let [paths (set (map #(str (:close-file %)) (take-last 2 ordered)))
                  records (filterv #(contains? paths (:path %)) history-identities)
                  identical? (apply = (map :checkpoints records))]
              (need! false (if identical? :distinct-path-identical-history :ambiguous-previous-construction)
                     {:files (vec (sort paths)) :requested-target requested-target
                      :records records
                      :identical-checkpoint-sets? identical?})))]
    (if-let [latest (last ordered)]
      (try (update (validated-previous! latest roots) :provenance assoc
                   :auxiliary-exclusions (:auxiliary-exclusions discovered) :searched-layouts (:layouts discovered) :excluded-attempts exclusions :requested-target requested-target
                   :target-evidence (:target-evidence latest))
           (catch clojure.lang.ExceptionInfo e
             (throw (ex-info (.getMessage e) (assoc (ex-data e) :history/searched-layouts (:layouts discovered) :history/excluded-attempts exclusions
                                                   :history/requested-target requested-target
                                                   :history/target-evidence (:target-evidence latest)) e))))
      {:cascade policy/first-attempt-cascade :admitted {}
       :provenance {:status :none :reason :no-earlier-target-construction :searched-roots roots
                    :auxiliary-exclusions (:auxiliary-exclusions discovered) :searched-layouts (:layouts discovered) :excluded-attempts exclusions :requested-target requested-target}
       :admission-reason :first-attempt-no-admissions})))

(defn history-roots [identity]
  (vec (distinct (cons (:data-root identity)
                       (map #(.getCanonicalPath %)
                            (filter #(and (.isDirectory %) (str/starts-with? (.getName %) "wm-full-loop"))
                                    (or (.listFiles (io/file "/home/joe/code/futon2/data")) [])))))))

(defn construct
  ([record read-bytes library-root previous designated]
   (construct record read-bytes library-root previous designated nil))
  ([record read-bytes library-root previous designated score-fn-override]
   (let [ctx (finder/context record read-bytes library-root)
        found (finder/find record read-bytes library-root designated)
        selected (set (:selected found))
        r (:repository ctx)
        repository {:patterns (:patterns r) :stands-on (set (map (juxt :from :to) (:edges r)))}
        admitted (:admitted previous)
        carrier (set/union selected (set (keys admitted)))
        order (precedence carrier (:stands-on repository))
        interpretations (:interpretations ctx)
        q0 (into {} (map (juxt :id :value)) (:facts record))
        ;; Before arm: the previous attempt's recorded acting order (organise
        ;; calls this port with the very previous-cascade object it was given),
        ;; so this receipt need not re-interpret every earlier pattern. A first
        ;; attempt or legacy reset has none. After arm: simulated from q0.
        acting (fn [c]
                 (if (identical? c (:cascade previous))
                   (vec (:acting-order previous []))
                   (acting-order interpretations q0 (:precedence c))))
        ;; Shadow cascade G (P11 step 1b-ii): computed ONCE for both arms over
        ;; ONE common universe (per-arm universes shift G by T*k*ln2 and would
        ;; fake candidate differences), then handed to organise's score port.
        ;; SHADOW ONLY: organise records :score-before/:score-after in the diff;
        ;; the shown acting order comes from acting-order-fn and does not read
        ;; them. Exception-safe: any failure becomes a typed missing outcome.
        after-nodes (set/union carrier (shadow/up-closure carrier (:stands-on repository))
                               (set (keys admitted)))
        shadow-arms {:before (:cascade previous)
                     :after {:nodes after-nodes :edges (:stands-on repository) :precedence order}}
        shadow-results (delay (shadow/shadow-cascade-g record shadow-arms {:library-root library-root}))
        score (fn [c]
                (let [arm (if (identical? c (:cascade previous)) :before :after)]
                  (try
                    (get @shadow-results arm
                         {:status :missing :kind :shadow-arm-not-found})
                    (catch Exception e
                      {:status :missing :kind :shadow-scorer-error
                       :message (str (.getMessage e))}))))
        diff (policy/organise (:cascade previous) selected repository admitted
                              {:temperament (assoc policy/up-closure-temperament :precedence order)
                               :acting-order-fn acting
                               :score-fn (or score-fn-override score)})
        _ (need! (= carrier (set/difference (:nodes diff) (:added-by-organise diff)))
                 :bootstrap-carrier-mismatch {})
        _ (policy/validate-cascade-diff! (:cascade previous) selected repository admitted diff)
        _ (validate-admissible! (:precedence-after diff) (:nodes diff) (:organised-edges diff))
        shown (:acting-order-after diff)
        _ (when (empty? shown)
            (throw (ex-info "Nothing fires in receipted construction"
                            {:interpretation/refusal :interpretation/no-relevant-pattern})))
        retained {:identity (:identity record) :previous (:provenance previous)
                  :admission-reason (:admission-reason previous)
                  :precedence-rule :authored-reachability-topological :tie-break :canonical-id
                  :acting-rule :first-true-unachieved-guard-apply-add-only-effects-from-q0
                  :effect-authority :documented-interpretation-not-measured-success
                  :cascade-diff diff :cascade-diff-sha256 (evidence/value-digest diff)
                  :find-result found :find-result-sha256 (evidence/value-digest found)
                  :repository-sha256 (:digest r)}]
    {:shown (mapv #(subs (str %) 1) shown)
     :semilattice {:descent (mapv (fn [[a b]] [(subs (str a) 1) (subs (str b) 1)]) (sort (:organised-edges diff))) :co_app []}
     :construction-kind :receipted-pattern-cascade :selected-action (get-in record [:identity :occurrence :action/value])
     :receipted-construction retained})))

(defn- validate-external-expectations!
  "WIRE (2026-09-18, EV-find-expectations caller integration): the ordinary
   construction seam validates its find result against the REQUIRED external
   F2 expectation artifact when the run carries one.

   opts :external-expectations is
     {:artifact <path to EDN, or a pre-read artifact map>
      :occurrence <the independently captured binding
                   {:target .. :target-source {:path :sha256}
                    :repository-sha256 .. :pinned-at ..
                    :source-digests {..}}>}

   PRESENT → the check is REQUIRED: the artifact is read through
   find-expectations/read-artifact (a path) and validate-external! runs
   against the construction's own find result. A bad artifact, a mismatched
   occurrence binding, an unexpected selection or a four-field disagreement
   is the typed find-expectations refusal — construction NEVER returns a
   result the external producer contradicts, and there is no path that
   catches the refusal and proceeds.

   ABSENT → the retained construction records :external-expectations
   {:status :not-supplied}. The receipt SAYS the external check did not run;
   absence is recorded on the record, never silent. Whether an occurrence
   runs with an artifact is the runner's/operator's configuration decision —
   this seam just never lies about which happened."
  [result opts]
  (if-let [cfg (:external-expectations opts)]
    (let [;; cfg may be the artifact map itself (its own :occurrence is the
          ;; binding) or {:artifact path-or-map :occurrence binding}.
          artifact (cond (string? (:artifact cfg)) (find-expectations/read-artifact (:artifact cfg))
                         (some? (:artifact cfg)) (:artifact cfg)
                         (= :wm/find-expectations-v1 (:schema cfg)) cfg
                         :else (throw (ex-info "external expectations config carries no artifact"
                                               {:construction/refusal :external-expectations-config-invalid
                                                :config (select-keys cfg [:artifact :occurrence])})))
          occurrence (or (:occurrence cfg) (:occurrence artifact))
          found (:find-result (:receipted-construction result))]
      (find-expectations/validate-external! occurrence artifact found)
      (update-in result [:receipted-construction]
                 assoc :external-expectations
                 {:status :validated
                  :author (:author artifact)
                  :expected (count (:expected artifact))}))
    (update-in result [:receipted-construction]
               assoc :external-expectations {:status :not-supplied})))

(defn- resolve-f4-designation
  "WIRE (Option A, elected by claude-4 2026-09-18, PINNED-DISPATCH-wm-08-
  delivery 3): the ordinary construction seam resolves the caller-declared
  F4 designation with the same present/absent discipline as
  validate-external-expectations! — deliberately the same shape, so two
  adjacent features never grow two disciplines.

  opts :f4-designation is a path to EDN, a pre-read artifact map, or
  {:artifact path-or-map :occurrence binding}.

  PRESENT → the artifact is read through find-designation/read-artifact and
  resolved by find-designation/resolve-designation against this run's own
  repository: an author role other than :designation-author (the
  finder's/interpreter's own side of the run) is refused as self-supplied,
  an occurrence-binding mismatch is the typed refusal, and the resolved
  designated set REACHES construct instead of the hardcoded nil this seam
  used to pass. ABSENT → designated stays nil (validate-result! already
  accepts nil as honest vacuity) and the construction RECORDS
  :f4-designation {:status :not-supplied} — the receipt says the external
  designation was not supplied, never silently deciding F4 was vacuous.

  No standing F4 authority is elected here (AUTH-F4-scope stays Joe's);
  the per-occasion artifact is whichever external author supplied one."
  [record read-bytes library-root opts]
  (if-let [cfg (:f4-designation opts)]
    (let [artifact (cond (string? cfg) (find-designation/read-artifact cfg)
                         (string? (:artifact cfg)) (find-designation/read-artifact (:artifact cfg))
                         (some? (:artifact cfg)) (:artifact cfg)
                         (= find-designation/schema-id (:schema cfg)) cfg
                         :else (throw (ex-info "f4 designation config carries no artifact"
                                               {:construction/refusal :f4-designation-config-invalid
                                                :config (select-keys cfg [:artifact :occurrence])})))
          occurrence (or (:occurrence cfg) (:occurrence artifact))
          repository (:repository (finder/context record read-bytes library-root))
          resolved (find-designation/resolve-designation occurrence artifact repository)]
      {:designated (:designated resolved)
       :record (cond-> {:status :validated
                        :author (:author artifact)
                        :f4 (:f4 resolved)
                        :designated (count (:designated resolved))}
                 (:vacuous-because resolved)
                 (assoc :vacuous-because (:vacuous-because resolved)))})
    {:designated nil
     :record {:status :not-supplied}}))

(defn construct! [record read-bytes opts]
  (let [library-root (or (:interpretation-library-root opts) "/home/joe/code/futon3/library")
        f4 (resolve-f4-designation record read-bytes library-root opts)]
    (validate-external-expectations!
     (update-in
      (construct record read-bytes library-root
                 (previous! (:identity record) (or (:interpretation-history-roots opts) (history-roots (:identity record))))
                 (:designated f4))
      [:receipted-construction] assoc :f4-designation (:record f4))
     opts)))
