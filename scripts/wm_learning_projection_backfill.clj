(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.meta-field-observation :as field]
         '[futon2.aif.meta-outcome-dataset :as dataset]
         '[futon2.aif.outer-learning-manifest :as manifest])
(import '[java.nio.file Files StandardCopyOption]
        '[java.util UUID])

(defn atomic-write! [path value]
  (let [target (io/file path)
        tmp (io/file (.getParentFile target)
                     (str "." (.getName target) "." (UUID/randomUUID) ".tmp"))]
    (io/make-parents target)
    (spit tmp (str (pr-str value) "\n"))
    (Files/move (.toPath tmp) (.toPath target)
                (into-array StandardCopyOption
                            [StandardCopyOption/ATOMIC_MOVE
                             StandardCopyOption/REPLACE_EXISTING]))))

(defn project-entry! [entry]
  (let [record-file (io/file (:path entry))]
    (if-not (.isFile record-file)
      {:status :absent :entry entry}
      (let [bytes (Files/readAllBytes (.toPath record-file))
            actual (field/sha256 bytes)]
        (when-not (= actual (:sha256 entry))
          (throw (ex-info "Backfill record source drift"
                          {:kind :outer-learning-record-drift
                           :path (:path entry) :expected (:sha256 entry) :actual actual})))
        (when-let [old (:projection entry)]
          (let [old-file (io/file (:path old))
                old-actual (when (.isFile old-file)
                             (field/sha256 (Files/readAllBytes (.toPath old-file))))]
            (when-not (= old-actual (:sha256 old))
              (throw (ex-info "Backfill projection source drift"
                              {:kind :outer-learning-projection-drift
                               :path (:path old) :expected (:sha256 old) :actual old-actual})))))
        (let [record (edn/read-string (String. ^bytes bytes "UTF-8"))
              record-pin (dataset/record-entry entry)
              projected (dataset/projection record record-pin)
              projection-path (str (subs (:path entry) 0 (- (count (:path entry)) 4))
                                   ".learning.edn")]
          (atomic-write! projection-path projected)
          {:status :projected
           :entry (assoc record-pin :projection
                         {:path projection-path
                          :sha256 (field/sha256
                                   (Files/readAllBytes (.toPath (io/file projection-path))))})})))))

(let [manifest-path (or (first *command-line-args*) manifest/default-path)
      before (manifest/read-manifest manifest-path)
      results (mapv project-entry! (:entries before))
      entries (mapv :entry results)
      after (assoc before :entries entries)
      _ (atomic-write! manifest-path after)
      projection-bytes (reduce + 0
                               (keep (fn [entry]
                                       (when-let [p (get-in entry [:projection :path])]
                                         (.length (io/file p))))
                                     entries))]
  (prn {:schema :wm/learning-projection-backfill-v1
        :status :complete :entries (count entries)
        :present (count (filter #(= :projected (:status %)) results))
        :absent (count (filter #(= :absent (:status %)) results))
        :projection-bytes projection-bytes
        :statuses (frequencies (map :status results))})
  (shutdown-agents))
