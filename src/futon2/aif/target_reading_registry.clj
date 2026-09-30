(ns futon2.aif.target-reading-registry
  "File registry for validated, source-pinned 象 readings of target HEADs.

  A target may retain readings for several source digests, but only the record
  whose digest equals the caller's current source digest is current.  This
  namespace does no reading work and has no network dependency."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.analysis-cascade :as analysis-cascade]
            [futon2.aif.load-identity :as identity])
  (:import [java.nio.file Files StandardCopyOption]
           [java.time Instant]))

(def default-root "/home/joe/code/storage/wm-target-readings")

(identity/register! *ns* *file*)

(def ^:private digest-pattern #"[0-9a-f]{64}")

(defn- sha256 [x]
  (identity/sha256 (.getBytes (pr-str x) "UTF-8")))

(defn source-digest
  "SHA-256 of the complete source file bytes, matching
  wm_task_reading.py/request_base's hashlib.sha256(raw).hexdigest()."
  [path]
  (identity/sha256 (Files/readAllBytes (.toPath (io/file path)))))

(defn- target-key [target-id]
  (identity/sha256 (.getBytes (str target-id) "UTF-8")))

(defn- target-dir [root target-id]
  (io/file root "targets" (target-key target-id)))

(defn- record-file [root target-id source-digest]
  (io/file (target-dir root target-id) (str source-digest ".edn")))

(defn- atomic-write! [file value]
  (.mkdirs (.getParentFile file))
  (let [tmp (io/file (.getParentFile file)
                     (str "." (.getName file) "." (System/nanoTime) ".tmp"))]
    (spit tmp (str (pr-str value) "\n"))
    (Files/move (.toPath tmp) (.toPath file)
                (into-array StandardCopyOption
                            [StandardCopyOption/ATOMIC_MOVE
                             StandardCopyOption/REPLACE_EXISTING]))
    value))

(defn- read-record [file]
  (when (.isFile file) (edn/read-string (slurp file))))

(defn- found-records [root target-id]
  (let [dir (target-dir root target-id)]
    (if-not (.isDirectory dir)
      []
      (->> (.listFiles dir)
           (filter #(and (.isFile %) (.endsWith (.getName %) ".edn")))
           (keep read-record)
           (filter #(= (str target-id) (str (:target-id %))))
           vec))))

(defn- validation [analysis]
  (try
    (let [result (analysis-cascade/analysis->cascades analysis {:mode :overlap})]
      (if (and (= :constructed (:status result)) (seq (:cascades result)))
        {:status :valid :cascade-count (count (:cascades result))}
        {:status :invalid :kind :analysis-without-validated-pattern-ref
         :failures (:failures result)}))
    (catch Exception e
      {:status :invalid :kind :analysis-validation-failed
       :message (ex-message e)})))

(defn publish!
  "Validate and atomically publish one source-pinned reading. Invalid input is
  returned as a typed refusal and is never written."
  ([record] (publish! default-root record))
  ([root {:keys [target-id source-path source-digest request analysis
                 validator-version]}]
   (let [request-digest (get-in request [:task :content_sha256])
         invalid-fields (cond-> []
                          (or (nil? target-id) (= "" (str target-id))) (conj :target-id)
                          (not (and (string? source-path) (seq source-path))) (conj :source-path)
                          (not (and (string? source-digest)
                                    (re-matches digest-pattern source-digest)))
                          (conj :source-digest)
                          (not (map? request)) (conj :request)
                          (not (map? analysis)) (conj :analysis)
                          (nil? validator-version) (conj :validator-version)
                          (and request-digest (not= source-digest request-digest))
                          (conj :request-source-digest))
         checked (when (empty? invalid-fields) (validation analysis))]
     (cond
       (seq invalid-fields)
       {:status :refused :kind :invalid-target-reading
        :target-id target-id :invalid-fields invalid-fields}

       (not= :valid (:status checked))
       {:status :refused :kind :invalid-target-reading
        :target-id target-id :validation checked}

       :else
       (let [stored {:schema :wm/target-reading-v1
                     :status :current-candidate
                     :target-id target-id
                     :source-path source-path
                     :source-digest source-digest
                     :request request
                     :analysis analysis
                     :analysis-digest (sha256 analysis)
                     :validator-version validator-version
                     :validation checked
                     :published-at (str (Instant/now))}]
         (atomic-write! (record-file root target-id source-digest) stored))))))

(defn current-reading
  "Return the reading at CURRENT-SOURCE-DIGEST, or a typed absence."
  ([target-id current-source-digest]
   (current-reading default-root target-id current-source-digest))
  ([root target-id current-source-digest]
   (let [records (found-records root target-id)
         current (some #(when (= current-source-digest (:source-digest %)) %) records)
         found (vec (sort (distinct (keep :source-digest records))))]
     (or current
         {:status :absent
          :kind (if (seq found) :stale-target-reading :no-current-target-reading)
          :target-id target-id
          :expected-digest current-source-digest
          :found-digests found}))))

(defn coverage
  "Count current, stale, and absent readings for target/digest declarations."
  ([targets] (coverage default-root targets))
  ([root targets]
   (let [rows (mapv (fn [{:keys [target-id source-digest]}]
                      {:target-id target-id
                       :result (current-reading root target-id source-digest)})
                    targets)
         classify (fn [{:keys [result]}]
                    (cond
                      (not= :absent (:status result)) :current
                      (= :stale-target-reading (:kind result)) :stale
                      :else :absent))
         ids (fn [kind] (->> rows (filter #(= kind (classify %)))
                             (mapv :target-id)))]
     {:total (count rows)
      :current (count (ids :current)) :current-ids (ids :current)
      :stale (count (ids :stale)) :stale-ids (ids :stale)
      :absent (count (ids :absent)) :absent-ids (ids :absent)})))
