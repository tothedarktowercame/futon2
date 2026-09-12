(ns row14-measured-a-census
  "Read-only census of candidate measured-A (status-at-close, disposition) pairs."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [futon2.aif.belief :as belief]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.trace :as trace])
  (:import [java.security MessageDigest]
           [java.time Instant]))

(def states (vec (sort belief/status-set)))
(def outcomes
  (vec (sort (disj cohort/outcome-kinds
                    :historical-verification-awaiting-validation
                    :historical-verification-refused))))

(defn- sha256 [^bytes bytes]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and 255 %)) digest))))

(defn- file-sha [f]
  (sha256 (java.nio.file.Files/readAllBytes (.toPath (io/file f)))))

(defn- instant [x]
  (when (string? x)
    (try (Instant/parse x) (catch Exception _ nil))))

(defn- edn-file [f]
  (try (edn/read-string (slurp f)) (catch Exception _ nil)))

(defn- closed-files [root]
  (->> (file-seq (io/file root))
       (filter #(.isFile %))
       (filter #(re-matches #"\d+-closed\.edn" (.getName %)))
       (sort-by str)
       vec))

(defn- selection-file [closed]
  (first (sort-by str (filter #(re-matches #"\d+-selection\.edn" (.getName %))
                              (file-seq (.getParentFile closed))))))

(defn- close-row [f]
  (let [close (edn-file f)
        sf (selection-file f)
        selection (when sf (edn-file sf))
        action (get-in selection [:payload :judgment :selected-action])]
    {:source (str f)
     :source-sha256 (file-sha f)
     :selection-source (some-> sf str)
     :cohort/id (:cohort/id close)
     :attempt/id (:attempt/id close)
     :closed-at (:recorded-at close)
     :closed-instant (instant (:recorded-at close))
     :outcome (get-in close [:payload :judgment :outcome])
     ;; Only :target is an entity identity. :target-class denotes an action
     ;; class and must never be renamed into an entity.
     :entity/id (:target action)
     :selected-action action}))

(defn- dedupe-closes [rows]
  (let [content-groups (vals (group-by :source-sha256 rows))
        unique-rows (mapv first content-groups)
        identity-groups (vals (group-by (juxt :cohort/id :attempt/id) unique-rows))]
    {:rows unique-rows
     :physical-count (count rows)
     :unique-count (count unique-rows)
     :duplicate-copies (- (count rows) (count unique-rows))
     :conflicts
     (vec (for [g identity-groups
                :let [signatures (set (map #(select-keys % [:closed-at :outcome :entity/id]) g))]
                :when (> (count signatures) 1)]
            {:identity (select-keys (first g) [:cohort/id :attempt/id])
             :signatures signatures}))}))

(defn- trace-rows [root]
  (->> (trace/read-all-traces :dir root)
       (keep-indexed
        (fn [idx r]
          (when-let [at (instant (:timestamp r))]
            {:trace/index idx :timestamp (:timestamp r) :instant at
             :mu-post (:mu-post r)})))
       vec))

(defn- argmax-status [row]
  (when (and (map? row) (= (set states) (set (keys row)))
             (every? number? (vals row)))
    (let [m (apply max (vals row))
          winners (vec (filter #(= m (get row %)) states))]
      (when (= 1 (count winners)) (first winners)))))

(defn- join-one [traces close]
  (cond
    (nil? (:closed-instant close)) (assoc close :join/status :unparseable-close-time)
    (nil? (:entity/id close)) (assoc close :join/status :missing-entity-id)
    (not (contains? (set outcomes) (:outcome close)))
    (assoc close :join/status :excluded-outcome)
    :else
    (let [candidates (filter #(and (not (.isAfter ^Instant (:instant %) ^Instant (:closed-instant close)))
                                   (contains? (:mu-post %) (:entity/id close)))
                             traces)
          latest-at (when (seq candidates) (last (sort (map :instant candidates))))
          nearest (vec (filter #(= latest-at (:instant %)) candidates))]
      (cond
        (empty? nearest) (assoc close :join/status :no-prior-entity-belief)
        (> (count nearest) 1) (assoc close :join/status :ambiguous-nearest-trace
                                    :candidate-count (count nearest))
        :else
        (let [t (first nearest)
              belief-row (get (:mu-post t) (:entity/id close))
              status (argmax-status belief-row)]
          (if status
            (assoc close :join/status :joined-derived-argmax
                         :status status :trace-at (:timestamp t)
                         :age-ms (- (.toEpochMilli ^Instant (:closed-instant close))
                                    (.toEpochMilli ^Instant (:instant t))))
            (assoc close :join/status :nonunique-or-malformed-argmax
                         :trace-at (:timestamp t))))))))

(defn- matrix [joined]
  (into (sorted-map)
        (for [s states]
          [s (into (sorted-map)
                   (for [o outcomes]
                     [o (count (filter #(and (= s (:status %)) (= o (:outcome %))) joined))]))])))

(defn- date-range [xs field]
  (let [vs (sort (keep field xs))]
    {:first (first vs) :last (last vs)}))

(defn report [cohort-root trace-root]
  (let [cfiles (closed-files cohort-root)
        tfiles (->> (file-seq (io/file trace-root))
                    (filter #(.isFile %))
                    (filter #(re-matches #"wm-trace-.*\.edn" (.getName %)))
                    (sort-by str) vec)
        manifest (mapv (fn [f] [(str f) (file-sha f)]) (concat cfiles tfiles))
        closes (dedupe-closes (mapv close-row cfiles))
        traces (trace-rows trace-root)
        assessed (mapv #(join-one traces %) (:rows closes))
        joined (filterv #(= :joined-derived-argmax (:join/status %)) assessed)
        counts (matrix joined)
        nonzero (count (filter pos? (mapcat vals (vals counts))))]
    {:schema :wm/measured-a-source-census-v1
     :as-of "2026-09-12"
     :authority :read-only-production-record-census
     :inputs {:cohort-root cohort-root :trace-root trace-root
              :manifest-entry-count (count manifest)
              :manifest-sha256 (sha256 (.getBytes (pr-str manifest) "UTF-8"))}
     :support {:states states :outcomes outcomes}
     :populations
     {:cohort-close-files (:physical-count closes)
      :unique-close-records (:unique-count closes)
      :byte-identical-copies (:duplicate-copies closes)
      :duplicate-identity-conflicts (:conflicts closes)
      :close-date-range (date-range (:rows closes) :closed-at)
      :close-outcomes (frequencies (map :outcome (:rows closes)))
      :close-entity-id-present (count (filter :entity/id (:rows closes)))
      :trace-files (count tfiles)
      :trace-records (count traces)
      :trace-date-range (date-range traces :timestamp)
      :trace-records-with-mu-post (count (filter #(seq (:mu-post %)) traces))}
     :join {:rule :latest-prior-trace-containing-exact-entity-id
            :status-source :derived-unique-argmax-of-mu-post
            :assessed (count assessed)
            :status-counts (frequencies (map :join/status assessed))
            :joined-count (count joined)
            :joined-date-range (date-range joined :closed-at)
            :age-ms (when (seq joined)
                      {:min (apply min (map :age-ms joined))
                       :max (apply max (map :age-ms joined))})}
     :counts {:total-pairs (count joined)
              :nonzero-cells nonzero
              :zero-cells (- (* (count states) (count outcomes)) nonzero)
              :matrix counts}
     :joined-pairs
     (mapv #(select-keys % [:cohort/id :attempt/id :entity/id :closed-at
                            :outcome :status :trace-at :age-ms :source
                            :source-sha256]) joined)}))

(defn -main [& [output]]
  (let [r (report "data/wm-full-loop" "data/wm-trace")]
    (if output
      (with-open [w (io/writer output)] (pp/pprint r w))
      (pp/pprint r))))
