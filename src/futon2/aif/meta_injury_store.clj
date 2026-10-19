(ns futon2.aif.meta-injury-store
  "Durable handoff from a terminal WM failure to the next META observation."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.meta-injury-observation :as injury])
  (:import [java.nio.file Files StandardCopyOption]
           [java.security MessageDigest]
           [java.util UUID]))

(def default-root "/home/joe/code/futon2/data/wm-meta-injuries")

(defn- sha256 [^bytes bytes]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn- read-bytes [path] (Files/readAllBytes (.toPath (io/file path))))

(defn- atomic-write! [target text]
  (let [target (io/file target)
        tmp (io/file (.getParentFile target)
                     (str "." (.getName target) "." (UUID/randomUUID) ".tmp"))]
    (io/make-parents target)
    (spit tmp text)
    (Files/move (.toPath tmp) (.toPath target)
                (into-array StandardCopyOption
                            [StandardCopyOption/ATOMIC_MOVE
                             StandardCopyOption/REPLACE_EXISTING]))
    (.getCanonicalPath target)))

(defn deposit!
  "Project RUN-RECORD-PATH and atomically publish an independently pinned head.
   Non-injuries are recorded as absent observations and cannot admit Algorithm."
  ([run-record-path] (deposit! default-root run-record-path))
  ([root run-record-path]
   (let [record-bytes (read-bytes run-record-path)
         record (edn/read-string (String. record-bytes "UTF-8"))
         run-pin {:path (.getCanonicalPath (io/file run-record-path))
                  :sha256 (sha256 record-bytes)}
         projection (injury/project-run-record record run-pin)
         projection-path (io/file root (str (:run-id projection) ".edn"))
         projection-path (atomic-write! projection-path (str (pr-str projection) "\n"))
         projection-bytes (read-bytes projection-path)
         source-pin {:path projection-path :sha256 (sha256 projection-bytes)}
         authority {:schema :wm/meta-injury-authority-v1
                    :source-pin source-pin :run-record-pin run-pin}
         observation (injury/produce {:source-bytes projection-bytes
                                      :expected-source-pin source-pin
                                      :run-record-bytes record-bytes
                                      :expected-run-record-pin run-pin})
         head (assoc authority :status (:status observation)
                     :capability (:capability observation)
                     :run-id (:run-id projection))
         head-path (atomic-write! (io/file root "HEAD.edn")
                                  (str (pr-str head) "\n"))]
     (assoc head :head-path head-path :observation observation))))

(defn load-authority
  "Load the independently pinned current authority, or nil before any deposit."
  ([] (load-authority default-root))
  ([root]
   (let [head-file (io/file root "HEAD.edn")]
     (when (.isFile head-file)
       (let [{:keys [source-pin run-record-pin]} (edn/read-string (slurp head-file))]
         {:source-bytes (read-bytes (:path source-pin))
          :expected-source-pin source-pin
          :run-record-bytes (read-bytes (:path run-record-pin))
          :expected-run-record-pin run-record-pin})))))
