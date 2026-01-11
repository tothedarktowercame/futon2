(ns ants.cyber
  "Local pattern bridge - reads from futon3 library.
   Replaces futon5 dependency.

   Provides pattern-based configuration for cyber-ants, loading
   pattern definitions from futon3/library/ants/*.flexiarg files."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; -----------------------------------------------------------------------------
;; Configuration

(def ^:private pattern-root
  "Path to futon3 pattern library (relative to futon2 project root)."
  "../futon3/library/ants")

(def default-pattern-id
  "Default pattern key for cyber ants."
  :cyber/baseline)

;; -----------------------------------------------------------------------------
;; Pattern name mapping

(def ^:private pattern-key->filename
  "Map pattern keywords to flexiarg filenames."
  {:cyber/baseline           "baseline-cyber-ant.flexiarg"
   :cyber/hunger-coupling    "hunger-precision-coupling.flexiarg"
   :cyber/cargo-return       "cargo-return-discipline.flexiarg"
   :cyber/pheromone-tuner    "pheromone-trail-tuner.flexiarg"
   :cyber/white-space        "white-space-scout.flexiarg"})

(def ^:private filename->pattern-key
  "Reverse mapping for discovery."
  (into {} (map (fn [[k v]] [v k]) pattern-key->filename)))

;; -----------------------------------------------------------------------------
;; Flexiarg parsing

(defn- parse-field
  "Extract a field value from flexiarg content."
  [content field-name]
  (when-let [match (re-find (re-pattern (str "@" field-name "\\s+(.+?)(?=\\n@|\\n!|$)"))
                            content)]
    (str/trim (second match))))

(defn- parse-title [content]
  (parse-field content "title"))

(defn- parse-flexiarg-id [content]
  (parse-field content "flexiarg"))

(defn- parse-conclusion
  "Extract the conclusion text from flexiarg (text after ! conclusion:)."
  [content]
  (when-let [match (re-find #"!\s*conclusion:\s*\n\s+(.+?)(?=\n\s+\+)" content)]
    (str/trim (second match))))

(defn- parse-aif-delta
  "Extract @aif-delta EDN from flexiarg content.
   Returns nil if not present."
  [content]
  (when-let [match (re-find #"@aif-delta\s+(\{[\s\S]*?\})\s*(?=@|\n!|$)" content)]
    (try
      (edn/read-string (second match))
      (catch Exception _
        nil))))

;; -----------------------------------------------------------------------------
;; Default AIF config deltas per pattern

(def ^:private default-deltas
  "Default AIF config deltas for each pattern.
   Used when @aif-delta is not present in the flexiarg file."
  {:cyber/baseline
   {}  ; baseline uses core defaults

   :cyber/hunger-coupling
   {:precision {:need-gain 0.7
                :dhdt-gain 0.9}
    :efe {:lambda {:survival 1.4}}}

   :cyber/cargo-return
   {:modes {:cargo-high 0.55}
    :precision {:tau-cap 1.2}
    :actions {:return {:cargo-thresh 0.1}}}

   :cyber/pheromone-tuner
   {:precision {:pher-scale 1.3
                :trail-grad-scale 1.2}
    :efe {:lambda {:info 0.5}}}

   :cyber/white-space
   {:precision {:food-scale 1.8
                :novelty-scale 1.4}
    :efe {:lambda {:info 0.6
                   :ambiguity 0.4}}}})

;; -----------------------------------------------------------------------------
;; Pattern loading

(defn- pattern-filename
  "Resolve a pattern filename from a pattern key.
   Falls back to <keyword-name>.flexiarg when not in the mapping."
  [pattern-key]
  (or (get pattern-key->filename pattern-key)
      (str (name pattern-key) ".flexiarg")))

(defn- pattern-file
  "Get the File for a pattern key."
  [pattern-key]
  (let [filename (pattern-filename pattern-key)
        f (io/file pattern-root filename)]
    (when (.exists f) f)))

(defn- load-flexiarg
  "Load and parse a flexiarg file, returning pattern metadata."
  [pattern-key]
  (when-let [f (pattern-file pattern-key)]
    (let [content (slurp f)
          title (or (parse-title content) (name pattern-key))
          summary (or (parse-conclusion content) "")
          aif-delta (or (parse-aif-delta content)
                        (get default-deltas pattern-key {}))]
      {:id pattern-key
       :title title
       :summary summary
       :aif-delta aif-delta
       :source (.getPath f)})))

;; -----------------------------------------------------------------------------
;; Public API

(defn available-patterns
  "List available pattern definitions.
   Returns a seq of maps with :id for each pattern."
  []
  (let [dir (io/file pattern-root)]
    (if (.exists dir)
      (->> (.listFiles dir)
           (filter #(str/ends-with? (.getName %) ".flexiarg"))
           (keep (fn [f]
                   (let [name (.getName f)
                         pk (or (get filename->pattern-key name)
                                (keyword "cyber" (str/replace name #"\.flexiarg$" "")))]
                     {:id pk
                      :file (.getPath f)})))
           vec)
      ;; Fallback: return known patterns even if files missing
      (mapv (fn [pk] {:id pk}) (keys pattern-key->filename)))))

(defn cyber-config
  "Get the full config for a pattern.
   Returns a map with :id and :aif-delta."
  [pattern-key]
  (or (load-flexiarg pattern-key)
      (load-flexiarg default-pattern-id)
      {:id default-pattern-id
       :title "Baseline Cyber-AIF Ant"
       :summary "Default AIF configuration"
       :aif-delta {}}))

(defn describe-pattern
  "Return human-readable description of a pattern.
   Returns map with :pattern, :title, :summary, and optionally :excerpt."
  [pattern-key]
  (let [config (cyber-config pattern-key)]
    {:pattern (:id config)
     :title (:title config)
     :summary (:summary config)}))

(defn- merge-deep
  "Deep merge maps, with later values taking precedence."
  [& maps]
  (letfn [(merge* [a b]
            (if (and (map? a) (map? b))
              (merge-with merge* a b)
              b))]
    (reduce merge* {} (remove nil? maps))))

(defn attach-config
  "Attach pattern-specific AIF config to an ant.
   Called by war.clj at spawn time.

   If pattern-key is not found, falls back to default-pattern-id."
  [ant pattern-key]
  (let [config (cyber-config pattern-key)]
    (-> ant
        (assoc :cyber-pattern {:id (:id config)
                               :title (:title config)
                               :delta (:aif-delta config)})
        (update :aif-config merge-deep (:aif-delta config)))))
