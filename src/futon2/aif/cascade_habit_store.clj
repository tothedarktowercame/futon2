(ns futon2.aif.cascade-habit-store
  "Record selected cascade representatives without feeding selection. Counts
   occupy one entry per distinct policy; no per-tick history is retained."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.cascade-prior :as prior])
  (:import [java.io RandomAccessFile]
           [java.nio.file Files StandardCopyOption]
           [java.nio.file.attribute FileAttribute]))

(def default-path
  (str (System/getProperty "user.home") "/code/futon2/data/wm-habit/cascade-prior.edn"))

(def selection-basis :first-ranked-sharing-chosen-action)
(defonce ^:private monitor (Object.))

(defn policy-view
  "Map the receipted live representation to the ruled habit identity. Empty
   precedence is a policy; absent precedence is not. Real topology, if present,
   is retained. A flat precedence contributes no invented topology."
  [candidate]
  (when (and (= :cascade-candidate (:kind candidate))
             (map? (:construction-receipt candidate))
             (vector? (:precedence candidate)))
    {:mission (:target candidate)
     :shown (mapv :id (:precedence candidate))
     :semilattice (get candidate :semilattice {})}))

(defn read-state [path]
  (let [file (io/file path)]
    (prior/coerce-state (when (.exists file) (edn/read-string (slurp file))))))

(defn- publish! [file state]
  (let [parent (.toPath (.getParentFile file))
        temporary (Files/createTempFile parent "cascade-prior-" ".edn"
                                        (make-array FileAttribute 0))]
    (try
      (with-open [out (java.io.FileOutputStream. (.toFile temporary))]
        (.write out (.getBytes (str (pr-str state) "\n") "UTF-8"))
        (.sync (.getFD out)))
      (Files/move temporary (.toPath file)
                  (into-array StandardCopyOption
                              [StandardCopyOption/ATOMIC_MOVE
                               StandardCopyOption/REPLACE_EXISTING]))
      (finally (Files/deleteIfExists temporary)))))

(defn record-selection!
  "Persist one observation of a receipted selected representative. Return the
   exact decision object. Abstentions and unconstructible actions count nothing.
   The JVM monitor and stable sidecar file lock serialize read/fold/replace,
   including writers in separate processes. Invalid stored state is not reset."
  ([decision] (record-selection! default-path decision))
  ([path decision]
   (let [view (policy-view (:action decision))]
     (when-let [key (and view (prior/policy-key view))]
       (locking monitor
         (let [file (.getAbsoluteFile (io/file path))]
           (.mkdirs (.getParentFile file))
           (with-open [lock-file (RandomAccessFile. (str file ".lock") "rw")
                       _lock (.lock (.getChannel lock-file))]
             (let [state (-> (prior/observe-policy (read-state path) view)
                             (assoc-in [:selection-bases key] selection-basis))]
               (publish! file state))))))
     decision)))
