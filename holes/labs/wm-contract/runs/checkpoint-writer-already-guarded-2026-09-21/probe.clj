(require '[clojure.edn :as edn] '[clojure.string :as str] '[clojure.walk :as walk]
         '[clojure.java.io :as io] '[futon2.aif.full-loop-cohort :as cohort]
         '[futon2.aif.full-loop-cohort-test :as fixtures])
(let [raw (slurp "data/wm-quarantine/machinery-67-attempt-001/002-selection.edn")
      readable (str/replace raw #":hole/([0-9][a-f0-9]*)" ":hole/reconstituted-$1")
      value (walk/postwalk (fn [x] (if (and (keyword? x) (= "hole" (namespace x))
                                          (str/starts-with? (name x) "reconstituted-"))
                                   (keyword "hole" (subs (name x) 14)) x)) (edn/read-string readable))
      root (fixtures/tmp-root)
      path (io/file root "direct.edn")]
  (prn {:poison-sha (#'cohort/sha256 raw)
        :reconstructed-error (try (edn/read-string (pr-str value)) :unexpected-pass
                                 (catch Exception e (.getMessage e)))})
  (#'cohort/write-new! path value)
  (prn {:direct-readable (map? (cohort/read-edn path))
        :placeholders (count (filter #(and (map? %) (= :non-edn-payload (:payload/status %)))
                                    (tree-seq coll? seq (cohort/read-edn path))))})
  (cohort/activate! fixtures/prereg-path root)
  (let [attempt (:attempt/id (fixtures/open! root "checkpoint-poison-probe"))
        returned (cohort/append-checkpoint! fixtures/prereg-path root attempt :selection (:payload value))
        path (io/file root (name (:cohort/id returned)) attempt "002-selection.edn")]
    (prn {:checkpoint-readable (map? (cohort/read-edn path))
          :placeholders (count (filter #(and (map? %) (= :non-edn-payload (:payload/status %)))
                                      (tree-seq coll? seq (cohort/read-edn path))))})))
(shutdown-agents)
