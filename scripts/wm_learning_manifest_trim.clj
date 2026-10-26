(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[futon2.aif.meta-field-observation :as field]
         '[futon2.aif.outer-learning-manifest :as manifest])
(import '[java.nio.file Files StandardCopyOption]
        '[java.util UUID])

(defn atomic-write! [path value]
  (let [target (io/file path)
        tmp (io/file (.getParentFile target)
                     (str "." (.getName target) "." (UUID/randomUUID) ".tmp"))]
    (spit tmp (str (pr-str value) "\n"))
    (Files/move (.toPath tmp) (.toPath target)
                (into-array StandardCopyOption
                            [StandardCopyOption/ATOMIC_MOVE
                             StandardCopyOption/REPLACE_EXISTING]))))

(defn verify-kept! [entry]
  (let [record (io/file (:path entry))]
    (when (.isFile record)
      (let [actual (field/sha256 (Files/readAllBytes (.toPath record)))]
        (when-not (= actual (:sha256 entry))
          (throw (ex-info "Kept learning record drifted"
                          {:kind :outer-learning-record-drift :entry entry :actual actual})))))
    (when-let [pin (:projection entry)]
      (let [file (io/file (:path pin))
            actual (when (.isFile file)
                     (field/sha256 (Files/readAllBytes (.toPath file))))]
        (when-not (= actual (:sha256 pin))
          (throw (ex-info "Kept learning projection drifted"
                          {:kind :outer-learning-projection-drift :entry entry :actual actual})))))
    entry))

(let [path (or (first *command-line-args*) manifest/default-path)
      before (manifest/read-manifest path)
      dropped (filterv #(and (str/starts-with? (:path %) "/tmp/")
                             (not (.isFile (io/file (:path %)))))
                       (:entries before))
      kept (->> (:entries before) (remove (set dropped)) (mapv verify-kept!))
      after (assoc before :entries kept)]
  (atomic-write! path after)
  (prn {:schema :wm/learning-manifest-trim-v1
        :status :trimmed
        :before-count (count (:entries before))
        :after-count (count kept)
        :dropped-count (count dropped)
        :before-digest (field/digest before)
        :after-digest (field/digest after)
        :absent-kept (count (filter #(not (.isFile (io/file (:path %)))) kept))})
  (shutdown-agents))
