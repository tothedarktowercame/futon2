(ns futon2.aif.evidence-manifest
  "Pure construction and validation of the close-evidence manifest v1."
  (:require [clojure.string :as str])
  (:import (java.nio.file InvalidPathException Paths)
           (java.security MessageDigest)
           (java.time Instant)))

(def manifest-schema :wm/close-evidence-manifest-v1)

(defn- refuse! [code path & [data]]
  (throw (ex-info "Close evidence manifest refused"
                  (merge {:evidence-manifest/refusal code :path path} data))))

(defn- exact-map! [x ks path]
  (when-not (and (map? x) (= ks (set (keys x))))
    (refuse! :shape-invalid path
             {:expected ks :actual (some-> x keys set)}))
  x)

(defn- text! [x path]
  (when-not (and (string? x) (not (str/blank? x)))
    (refuse! :identity-invalid path))
  x)

(defn- instant! [x path]
  (try
    (Instant/parse (text! x path))
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable _ (refuse! :timestamp-invalid path))))

(defn- absolute-path! [x path]
  (text! x path)
  (try
    (when-not (.isAbsolute (Paths/get x (make-array String 0)))
      (refuse! :source-path-invalid path))
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch InvalidPathException _ (refuse! :source-path-invalid path)))
  x)

(defn- sha256-format! [x path]
  (when-not (and (string? x) (boolean (re-matches #"[0-9a-f]{64}" x)))
    (refuse! :sha256-invalid path))
  x)

(defn- sha256-bytes [bytes]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and 0xff %)) digest))))

(defn- canonical-entry [{:keys [evidence/id source-path sha256 admitted-at]}]
  (array-map :evidence/id id
             :source-path source-path
             :sha256 sha256
             :admitted-at admitted-at))

(defn- entries-sha256 [entries]
  (sha256-bytes (.getBytes ^String (pr-str entries) "UTF-8")))

(defn- validate-entry [entry i]
  (let [path [:entries i]]
    (exact-map! entry #{:evidence/id :source-path :sha256 :admitted-at} path)
    (text! (:evidence/id entry) (conj path :evidence/id))
    (absolute-path! (:source-path entry) (conj path :source-path))
    (sha256-format! (:sha256 entry) (conj path :sha256))
    (instant! (:admitted-at entry) (conj path :admitted-at))
    (canonical-entry entry)))

(defn validate-manifest
  "Validate the closed manifest value without rereading its admitted sources."
  [manifest]
  (exact-map! manifest #{:schema :entries :manifest-sha256} [:manifest])
  (when-not (= manifest-schema (:schema manifest))
    (refuse! :schema-mismatch [:manifest :schema]))
  (when-not (vector? (:entries manifest))
    (refuse! :shape-invalid [:manifest :entries]
             {:expected :vector :actual (type (:entries manifest))}))
  (let [entries (mapv validate-entry (:entries manifest) (range))
        ids (mapv :evidence/id entries)]
    (when-not (= (count ids) (count (distinct ids)))
      (refuse! :duplicate-evidence-id [:manifest :entries]))
    (sha256-format! (:manifest-sha256 manifest) [:manifest :manifest-sha256])
    (let [actual (entries-sha256 entries)]
      (when-not (= actual (:manifest-sha256 manifest))
        (refuse! :manifest-sha256-mismatch [:manifest :manifest-sha256]
                 {:expected (:manifest-sha256 manifest) :actual actual})))
    (array-map :schema manifest-schema
               :entries entries
               :manifest-sha256 (:manifest-sha256 manifest))))

(defn- read-source! [read-bytes source-path path]
  (let [bytes (try
                (read-bytes source-path)
                (catch Throwable e
                  (refuse! :source-unavailable path
                           {:source-path source-path
                            :cause (.getName (class e))})))]
    (when-not (instance? (Class/forName "[B") bytes)
      (refuse! :source-unavailable path {:source-path source-path}))
    bytes))

(defn- admit-entry [read-bytes entry i]
  (let [path [:entries i]
        ks (set (keys entry))]
    (when-not (or (= ks #{:evidence/id :source-path :admitted-at})
                  (= ks #{:evidence/id :source-path :expected-sha256 :admitted-at}))
      (refuse! :shape-invalid path
               {:expected [#{:evidence/id :source-path :admitted-at}
                           #{:evidence/id :source-path :expected-sha256 :admitted-at}]
                :actual ks}))
    (let [id (text! (:evidence/id entry) (conj path :evidence/id))
          source-path (absolute-path! (:source-path entry) (conj path :source-path))
          admitted-at (do (instant! (:admitted-at entry) (conj path :admitted-at))
                          (:admitted-at entry))
          expected (:expected-sha256 entry)
          _ (when (contains? entry :expected-sha256)
              (sha256-format! expected (conj path :expected-sha256)))
          actual (sha256-bytes
                  (read-source! read-bytes source-path (conj path :source-path)))]
      (when (and expected (not= expected actual))
        (refuse! :source-sha256-mismatch (conj path :expected-sha256)
                 {:expected expected :actual actual :source-path source-path}))
      (canonical-entry {:evidence/id id
                        :source-path source-path
                        :sha256 actual
                        :admitted-at admitted-at}))))

(defn build-manifest
  "Read each source once through the mandatory READ-BYTES capability and admit
  its literal bytes. Admission time is supplied per entry; this function has
  no clock or filesystem default."
  [{:keys [entries read-bytes] :as inputs}]
  (exact-map! inputs #{:entries :read-bytes} [:build-input])
  (when-not (vector? entries)
    (refuse! :shape-invalid [:build-input :entries]
             {:expected :vector :actual (type entries)}))
  (when-not (fn? read-bytes)
    (refuse! :read-capability-missing [:build-input :read-bytes]))
  (let [admitted (mapv #(admit-entry read-bytes %1 %2) entries (range))
        ids (mapv :evidence/id admitted)]
    (when-not (= (count ids) (count (distinct ids)))
      (refuse! :duplicate-evidence-id [:entries]))
    (validate-manifest
     (array-map :schema manifest-schema
                :entries admitted
                :manifest-sha256 (entries-sha256 admitted)))))

(defn verify-retention-agreement
  "Require exact ordered identity agreement between a validated manifest and
  the close-retention block's admitted-evidence vector."
  [manifest retention-block]
  (let [manifest (validate-manifest manifest)
        admitted (:admitted-evidence retention-block)]
    (when-not (and (map? retention-block) (vector? admitted))
      (refuse! :retention-evidence-invalid [:retention :admitted-evidence]))
    (let [manifest-ids (mapv :evidence/id (:entries manifest))]
      (when-not (= manifest-ids admitted)
        (refuse! :retention-evidence-mismatch [:retention :admitted-evidence]
                 {:manifest-ids manifest-ids :retention-ids admitted})))
    true))
