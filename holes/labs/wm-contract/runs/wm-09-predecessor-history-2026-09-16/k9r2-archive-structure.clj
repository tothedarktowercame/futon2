(require '[clojure.java.io :as io] '[clojure.edn :as edn]
         '[futon2.aif.receipt-construction :as construction]
         '[futon2.aif.interpretation-evidence :as evidence])
(import '[java.nio.file Files])
(let [[root source output] *command-line-args*]
  (load-file source)
  (let [coverage (@#'construction/discover-closes! [root])
        files (:files coverage)
        rows (mapv (fn [path]
                     (let [closed (edn/read-string (slurp path))
                           dir (.getParentFile (io/file path))]
                       {:path path :identity (select-keys closed [:cohort/id :attempt/id])
                        :checkpoint-hashes (into (sorted-map)
                                                 (for [f (sort-by #(.getName %) (.listFiles dir))
                                                       :when (re-matches #"00[1-7]-.*\.edn" (.getName f))]
                                                   [(.getName f) (evidence/sha256 (Files/readAllBytes (.toPath f)))]))})) files)
        collisions (mapv (fn [[id records]]
                           {:identity id :records (vec records)
                            :identical-checkpoint-sets? (apply = (map :checkpoint-hashes records))})
                         (filter #(> (count (second %)) 1) (group-by :identity rows)))
        barrier (try (@#'construction/distinct-history-identities! files)
                     {:status :unexpected-pass}
                     (catch clojure.lang.ExceptionInfo e {:status :refused :data (ex-data e)}))]
    (assert (= 27 (count files)))
    (assert (= 3 (count collisions)))
    (assert (= :refused (:status barrier)))
    (spit output (pr-str {:source "96d5b5e579c79cd0151bcfe9de820c9de4710feb" :scope :pinned-24-archives-plus-three-named-counterparts
                         :discovered (count files) :coverage coverage :structural-barrier barrier
                         :collisions collisions :all-records rows
                         :target-relative-status :not-run-structural-refusal
                         :requested-target nil}))
    (prn {:discovered (count files) :collisions (count collisions)
          :identical (count (filter :identical-checkpoint-sets? collisions))
          :different (count (remove :identical-checkpoint-sets? collisions))
          :target-relative :not-run})))
(shutdown-agents)
