(require '[clojure.java.io :as io]
         '[futon2.aif.outer-learning-manifest :as manifest])

(let [manifest-path (or (first *command-line-args*) manifest/default-path)
      before (manifest/read-manifest manifest-path)
      results (mapv (fn [{record-path :path}]
                      (if (.isFile (io/file record-path))
                        (manifest/record! manifest-path record-path)
                        {:status :absent :path record-path}))
                    (:entries before))
      after (manifest/read-manifest manifest-path)
      projection-bytes (reduce + 0
                               (keep (fn [entry]
                                       (when-let [p (get-in entry [:projection :path])]
                                         (when (.isFile (io/file p))
                                           (.length (io/file p)))))
                                     (:entries after)))]
  (prn {:schema :wm/learning-projection-backfill-v1
        :status :complete
        :entries (count (:entries after))
        :present (count (filter #(not= :absent (:status %)) results))
        :absent (count (filter #(= :absent (:status %)) results))
        :projection-bytes projection-bytes
        :statuses (frequencies (map :status results))})
  (shutdown-agents))
