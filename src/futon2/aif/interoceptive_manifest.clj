(ns futon2.aif.interoceptive-manifest
  "Complete-directory manifest adapter for R20 trip/repair evidence.

  Production roots are owned constants, never caller labels. Coordinated
  source exists, but production qualification refuses until deployment
  evidence establishes that every live writer executes it."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.c-fold-config :as digest]
            [futon2.aif.interoceptive-commitment :as commitment])
  (:import [java.nio ByteBuffer]
           [java.nio.charset CodingErrorAction StandardCharsets]
           [java.nio.file Files]
           [java.security MessageDigest]))

(def repair-children ["findings" "implementations" "resolutions"])
(def ^:dynamic *after-capture-hook* (fn [] nil))

(defn- sha256-bytes [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn- refuse! [reason data]
  (throw (ex-info (str "Interoceptive manifest refused: " (name reason))
                  (assoc data :refusal reason))))

(defn- strict-edn [bytes path]
  (let [text (try
               (str (.decode (doto (.newDecoder StandardCharsets/UTF_8)
                               (.onMalformedInput CodingErrorAction/REPORT)
                               (.onUnmappableCharacter CodingErrorAction/REPORT))
                             (ByteBuffer/wrap bytes)))
               (catch Throwable e
                 (refuse! :interoceptive/non-utf8-artifact
                          {:path path :cause (.getMessage e)})))]
    (with-open [r (java.io.PushbackReader. (java.io.StringReader. text))]
      (try
        (let [v (edn/read {:eof ::empty} r)]
          (when (or (= ::empty v) (not= ::end (edn/read {:eof ::end} r)))
            (refuse! :interoceptive/not-one-edn-form {:path path}))
          v)
        (catch clojure.lang.ExceptionInfo e (throw e))
        (catch Throwable e
          (refuse! :interoceptive/malformed-edn
                   {:path path :cause (.getMessage e)}))))))

(defn- directory! [path role]
  (let [f (.getAbsoluteFile (io/file path))
        target (.toPath f)]
    (loop [p (.getRoot target) names (iterator-seq (.iterator target))]
      (when-let [name (first names)]
        (let [candidate (.resolve p name)]
          (when (Files/isSymbolicLink candidate)
            (refuse! :interoceptive/source-path-refused
                     {:path path :role role :entry (str candidate)}))
          (recur candidate (next names)))))
    (when-not (and (.isDirectory f) (not (Files/isSymbolicLink (.toPath f))))
      (refuse! :interoceptive/directory-unavailable {:path path :role role}))
    f))

(defn- read-bytes! [f phase]
  (try
    (Files/readAllBytes (.toPath ^java.io.File f))
    (catch Throwable e
      (refuse! :interoceptive/source-read-failed
               {:path (.getPath ^java.io.File f)
                :phase phase :cause (.getMessage e)}))))

(defn- list-files! [dir role]
  (let [xs (.listFiles ^java.io.File dir)]
    (when (nil? xs)
      (refuse! :interoceptive/directory-listing-failed
               {:path (.getPath ^java.io.File dir) :role role}))
    (doseq [f xs]
      (when-not (or (and (= role :repair-findings)
                         (= ".publication.lock" (.getName ^java.io.File f))
                         (.isFile ^java.io.File f))
                    (and (.isFile ^java.io.File f)
                         (not (Files/isSymbolicLink (.toPath ^java.io.File f)))
                         (str/ends-with? (.getName ^java.io.File f) ".edn")))
        (refuse! :interoceptive/unexpected-structural-entry
                 {:path (.getPath ^java.io.File f) :role role})))
    (->> xs (filter #(str/ends-with? (.getName ^java.io.File %) ".edn"))
         (sort-by #(.getName ^java.io.File %)) vec)))

(defn- capture-files [files prefix]
  (mapv (fn [f]
          (let [bytes (read-bytes! f :capture)
                relative (str prefix (.getName ^java.io.File f))]
            {:path relative :sha256 (sha256-bytes bytes)
             :byte-count (alength bytes) :record (strict-edn bytes relative)}))
        files))

(defn- recensus! [groups]
  (mapv (fn [{:keys [prefix dir role]}]
          [prefix (mapv (fn [f]
                          (let [bytes (read-bytes! f :recensus)]
                            [(.getName ^java.io.File f)
                             (sha256-bytes bytes)]))
                        (list-files! dir role))]) groups))

(defn capture
  "Capture all four owned directories. CLASS must be :test for caller roots;
  production callers use `production-manifest!` with canonical roots."
  [trip-root repair-root authority-class]
  (when-not (= :test authority-class)
    (refuse! :interoceptive/authority-spoof {:authority-class authority-class}))
  (let [trip-dir (directory! trip-root :trips)
        repair-dir (directory! repair-root :repair-root)
        groups (into [{:prefix "trips/" :dir trip-dir :role :trips
                       :files (list-files! trip-dir :trips)}]
                     (map (fn [child]
                            (let [role (keyword (str "repair-" child))
                                  dir (directory! (io/file repair-dir child) role)]
                              {:prefix (str child "/") :dir dir :role role
                               :files (list-files! dir role)}))
                          repair-children))
        before (mapv (fn [{:keys [prefix files]}]
                       [prefix (mapv #(.getName ^java.io.File %) files)]) groups)
        rows (mapcat (fn [{:keys [prefix files]}] (capture-files files prefix)) groups)
        _ (*after-capture-hook*)
        after (recensus! groups)
        captured (mapv (fn [[prefix names]]
                         [prefix (mapv (fn [name]
                                         (let [row (some #(when (= (str prefix name) (:path %)) %) rows)]
                                           [name (:sha256 row)])) names)]) before)]
    (when-not (= captured after)
      (refuse! :interoceptive/store-changed-during-capture
               {:before captured :after after}))
    (let [trip-rows (filterv #(str/starts-with? (:path %) "trips/") rows)
          repair-rows (remove #(str/starts-with? (:path %) "trips/") rows)
          trip-ids (mapv #(get-in % [:record :trip/id]) trip-rows)]
      (when-not (= (count trip-ids) (count (distinct trip-ids)))
        (refuse! :interoceptive/duplicate-trip-identity {:trip/ids trip-ids}))
      {:schema :wm/interoceptive-store-manifest-v1
       :authority-class :test :stable-census? true
       :manifest (mapv #(dissoc % :record) rows)
       :constructor-input
       {:trip-authority {:source-root (str trip-root)
                         :revision (digest/sha256 (pr-str (mapv #(dissoc % :record) trip-rows)))
                         :read-status :ok :authority-class :test
                         :records (mapv #(select-keys % [:path :sha256 :record]) trip-rows)}
        :repair-authority {:source-root (str repair-root)
                           :revision (digest/sha256 (pr-str (mapv #(dissoc % :record) repair-rows)))
                           :read-status :ok :authority-class :test
                           :records (into {} (map (juxt :path #(select-keys % [:sha256 :record]))) repair-rows)}}})))

(defn test-snapshot [trip-root repair-root]
  (let [manifest (capture trip-root repair-root :test)]
    (assoc manifest :snapshot (commitment/confidence-snapshot (:constructor-input manifest)))))

(defn production-manifest!
  "Refuse until deployment evidence establishes that every live canonical
  writer is executing the coordinated source. Source presence is not
  deployment evidence."
  []
  (refuse! :interoceptive/writer-participation-unverified
           {:required :all-live-canonical-trip-and-repair-writers
            :activation :reload-or-restart-with-independent-deployment-receipt
            :scope :source-implemented-not-deployed}))
