(ns futon2.aif.area-focus
  "Area-derived target relation and pinned operator-focus selection."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.mission-registry :as mission-registry]
            [futon2.aif.operator-focus :as operator-focus])
  (:import [java.time Duration Instant]))

(def corpus-resource "wm/focus/task-areas-v1.json")
(def default-manifest-dir "data/wm-runs")

(defn load-corpus
  ([] (load-corpus (io/resource corpus-resource)))
  ([source]
   (let [text (slurp source) corpus (json/parse-string text true)]
     {:corpus corpus :rows (into {} (map (juxt :target identity) (:rows corpus)))
      :sha256 (identity/sha256 text)})))

(defn focus-areas [focus-result]
  (set (map :area (get-in focus-result [:result :ranked]))))

(defn classify-target
  "Return the existing classification receipt shape from one task-area row and
   one retained focus manifest. No row or absent focus remains unknown."
  [rows corpus-sha focus-result focus-manifest-sha target]
  (let [row (get rows target) result (:result focus-result)
        areas (focus-areas focus-result)
        reason (when-not (= :present (:status result))
                 (or (:reason result) :focus-absent))
        relation (cond reason :unknown
                       (nil? row) :unknown
                       (areas (:area row)) :focus
                       (areas (:also row)) :associated
                       :else :useful-elsewhere)
        ;; The receipt keeps the relation vocabulary in :class — the class the
        ;; scorer's relation fn returns and outer-select's mismatch check
        ;; compares against (same contract as focus-receipt/classify-target,
        ;; codex-20 ruling; see the focus-receipt note in apparatus
        ;; certificates). The Lean terminal-class projection is recorded
        ;; separately in :scored-class.
        scored-class (get {:focus :focused :associated :related
                           :useful-elsewhere :unrelated} relation :unknown)
        derived {:kind :task-area :area (:area row) :also (:also row)
                 :focus-areas (vec (sort areas)) :corpus-sha256 corpus-sha
                 :focus-manifest-sha256 focus-manifest-sha}]
    {:target target :class relation :scored-class scored-class :relation-key relation
     :relation (if (= :unknown relation)
                 {:status :absent :reason (or reason :no-area-row)}
                 {:target target :relation (name relation) :facet (:area row)
                  :source {:kind :task-area :cite (:cite row) :quote (:quote row)}})
     :derived-via derived
     :cite (:cite row) :quote (:quote row)}))

(defn- manifest-files [dir]
  (->> (or (.listFiles (io/file dir)) (make-array java.io.File 0))
       (filter #(.isFile ^java.io.File %))
       (filter #(re-matches #"operator-focus-[0-9a-f]{64}\.edn" (.getName ^java.io.File %)))))

(defn read-manifest [path]
  (let [text (slurp path)]
    {:path (.getCanonicalPath (io/file path)) :sha256 (identity/sha256 text)
     :manifest (edn/read-string text)}))

(defn newest-retained
  "Newest content-addressed focus manifest no more than 60 minutes old."
  ([dir decision-as-of] (newest-retained dir decision-as-of 60))
  ([dir decision-as-of max-age-minutes]
   (let [now (Instant/parse decision-as-of)]
     (->> (manifest-files dir)
          (keep (fn [f] (try (let [pin (read-manifest f)
                                   at (Instant/parse (get-in pin [:manifest :as-of]))
                                   age (.toMinutes (Duration/between at now))]
                               (when (and (not (neg? age)) (<= age max-age-minutes))
                                 (assoc pin :as-of (str at) :age-minutes age)))
                             (catch Exception _ nil))))
          (sort-by :as-of #(compare %2 %1)) first))))

(defn retained-relations
  "Classify TARGETS from the newest retained focus manifest, without acquiring
  or minting focus. Returns nil when no <=60-minute manifest is available."
  [targets {:keys [decision-as-of manifest-dir corpus-source]
            :or {manifest-dir (str mission-registry/default-code-root
                                   "/futon2/" default-manifest-dir)}}]
  (when-let [pin (newest-retained manifest-dir decision-as-of)]
    (let [{:keys [rows sha256]} (if corpus-source
                                  (load-corpus corpus-source)
                                  (load-corpus))
          relations (into {}
                          (keep (fn [target]
                                  (let [relation (:relation-key
                                                  (classify-target rows sha256
                                                                   (:manifest pin)
                                                                   (:sha256 pin) target))]
                                    (when (contains? #{:focus :associated
                                                       :useful-elsewhere}
                                                     relation)
                                      [target relation]))))
                          targets)]
      {:relations relations
       :evidence-pin
       {:focus-manifest (select-keys pin [:path :sha256])
        :area-corpus-sha256 sha256}})))

(defn verify-pin [{:keys [path sha256]}]
  (try (let [loaded (read-manifest path)]
         (if (= sha256 (:sha256 loaded)) loaded
             {:deferred :focus-source-changed :pinned {:path path :sha256 sha256}
              :loaded (select-keys loaded [:path :sha256])}))
       (catch Exception _ {:deferred :focus-source-changed
                           :pinned {:path path :sha256 sha256}})))

(defn acquire-focus!
  "Reuse a <=60-minute retained manifest, otherwise perform one canonical read."
  [{:keys [decision-as-of manifest-dir code-root getenv-fn] :as opts}]
  (or (newest-retained (or manifest-dir default-manifest-dir) decision-as-of)
      (let [root (or code-root ((or getenv-fn #(System/getenv %)) "FUTON_CODE_ROOT")
                     mission-registry/default-code-root)
            pin (operator-focus/read-focus!
                 (merge {:as-of decision-as-of :manifest-dir (or manifest-dir default-manifest-dir)
                         :area-corpus-path (str root "/futon3c/holes/excursions/ant-reads")
                         :prompt-resource "wm/focus/operator-focus-reader-v1.md"
                         :code-root root :task-repo (str root "/futon2")}
                        (dissoc opts :getenv-fn)))]
        (when (= :ok (:status pin))
          {:path (:path pin) :sha256 (:sha256 pin) :manifest (:manifest pin)
           :as-of (get-in pin [:manifest :as-of]) :age-minutes 0}))))
