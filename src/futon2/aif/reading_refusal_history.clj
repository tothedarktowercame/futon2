(ns futon2.aif.reading-refusal-history
  "Pre-contact comparison of reading refusals; distinct from terminal E3."
  (:require [clojure.java.io :as io]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.outer-refusal-history :as history]))

(def identity-fields [:target :source-path :source-sha256 :excerpt-sha256
                      :request-sha256 :contract-sha256 :validator-sha256
                      :delivery :reader])
(defn absence [reason] {:status :absent :reason reason})
(defn reader-configuration
  "Only metadata attached by the actual invoke producer is authoritative.
  Registry display labels and this process's environment do not prove the
  configuration captured inside an already-created invoke closure."
  [seat]
  (try
    (let [get-agent (requiring-resolve 'futon3c.agency.registry/get-agent)
          agent (get-agent seat)
          config (:reader/configuration (meta (:agent/invoke-fn agent)))]
      {:seat seat
       :provider (or (:provider config) (absence :invoke-provider-not-retained))
       :model (or (:model config) (absence :invoke-model-not-retained))
       :output-budget (or (:output-budget config) (absence :invoke-output-budget-not-retained))})
    (catch Exception _
      {:seat seat :provider (absence :reader-configuration-unavailable)
       :model (absence :reader-configuration-unavailable)
       :output-budget (absence :reader-configuration-unavailable)})))

(defn file-sha [path]
  (try (identity/sha256 (java.nio.file.Files/readAllBytes (.toPath (io/file path))))
       (catch Exception _ (absence :source-unreadable))))

(defn inputs [request request-sha contract validator reader]
  {:schema :wm/reading-inputs-v1
   :target (get-in request [:task :target_id])
   :source-path (get-in request [:task :file_path])
   :source-sha256 (get-in request [:task :content_sha256])
   :excerpt-sha256 (get-in request [:task :excerpt_sha256])
   :request-sha256 request-sha
   :contract-sha256 (identity/sha256 (.getBytes (pr-str contract) "UTF-8"))
   :validator-sha256 (file-sha validator)
   :delivery {:version :pinned-file-or-inline-v1 :format-correction-limit 1}
   :reader reader})

(defn complete? [x]
  (and (every? #(some? (get x %)) identity-fields)
       (every? #(some? (get (:reader x) %)) [:seat :provider :model :output-budget])
       (not-any? #(and (map? %) (= :absent (:status %))) (tree-seq coll? seq x))))

(defn compare-rows [current rows]
  (let [comparisons
        (mapv (fn [row]
                (let [prior (:reading-inputs row)
                      complete (and (complete? current) (complete? prior))
                      differences (when complete
                                    (vec (filter #(not= (get prior %) (get current %)) identity-fields)))]
                  {:record-pin {:path (:record-path row) :sha256 (:record-sha256 row)}
                   :run-id (:run-id row) :target (:target row)
                   :status (cond (not complete) :incomparable
                                 (seq differences) :changed :else :unchanged)
                   :reason (when-not complete :reading-identity-incomplete)
                   :differing-identities differences
                   :prior-inputs prior :current-inputs current
                   :reader-job-ids (:reader-job-ids row) :response-pins (:response-pins row)})) rows)
        unchanged (first (filter #(= :unchanged (:status %)) comparisons))]
    {:schema :wm/reading-repeat-comparison-v1 :inputs current
     :status (cond unchanged :unchanged-refused-reading
                   (some #(= :changed (:status %)) comparisons) :inputs-changed
                   (seq comparisons) :incomparable :else :no-prior-refusal)
     :matched-refusal unchanged :comparisons comparisons}))

(defn compare-history [dir current]
  (if-not dir
    {:schema :wm/reading-repeat-comparison-v1 :status :incomparable
     :reason :reading-history-root-absent :inputs current :comparisons []}
    (let [rows (history/read-index dir)]
      (if (map? rows)
        (assoc rows :inputs current)
        (let [priors (filterv #(and (= (:target current) (:target %))
                                   (or (= :wm/reading-refusal-index-entry-v1 (:schema %))
                                       (keyword? (get-in % [:failure :kind])))) rows)
              drift (first (filter #(or (not= (:record-sha256 %) (file-sha (:record-path %)))
                                       (some (fn [pin] (not= (:sha256 pin) (file-sha (:path pin))))
                                             (:response-pins %))) priors))]
          (if drift
            {:schema :wm/reading-repeat-comparison-v1 :status :history-pin-mismatch
             :inputs current :record-path (:record-path drift) :comparisons []}
            (compare-rows current priors)))))))
