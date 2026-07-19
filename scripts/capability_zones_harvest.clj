(ns capability-zones-harvest
  "Pure artifact builder for M-capability-zones S1.  No store API is called."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [futon2.aif.capability-zones :as zones]
            [futon2.aif.intrinsic-values :as iv])
  (:import (java.time YearMonth)))

(def margin-threshold
  "One global, class-independent resistance threshold in cosine-margin units."
  0.01)

(def prepared-path
  "/home/joe/code/futon2/data/capability_zones/harvest-prepared.json")
(def artifact-path
  "/home/joe/code/futon2/data/capability_zones/harvest-2026-07-19.edn")
(def external-seeds-path
  "/home/joe/code/data/notions/bge_action_class_seeds.json")
(def checked-seeds-path
  "/home/joe/code/futon2/resources/capability_zones/action_class_seeds.json")
(def coverage-path
  "/home/joe/code/futon0/holes/missions/M-capability-zones-S1-coverage.md")

(defn- month-of [timestamp] (subs timestamp 0 7))

(defn- centroid [vectors]
  (let [n (double (count vectors))]
    (mapv #(/ (double %) n) (apply map + vectors))))

(defn- record-entry [record]
  {:alpha (:alpha-post record)
   :beta (:beta-post record)
   :intrinsic-value (:intrinsic-value-post record)
   :n-emissions (:n-emissions-in-window record)
   :n-followthrough (:n-followthrough-in-window record)
   :as-of (:as-of record)})

(defn- annotate [item]
  (let [membership (zones/zone-of (:embedding item))
        resisted? (< (:margin membership) margin-threshold)]
    (merge (dissoc item :embedding)
           membership
           {:resisted? resisted?
            :month (month-of (:committed_at item))})))

(defn- update-records [assignments]
  (let [accepted (remove :resisted? assignments)
        windows (sort-by key (group-by (juxt :month :class) accepted))]
    (:records
     (reduce
      (fn [{:keys [priors records]} [[[month class] items]]]
        (let [prior (get priors class (iv/fresh-entry))
              emissions (count items)
              followthrough (count (filter :followthrough items))
              latest (last (sort (map :committed_at items)))
              record (iv/next-update-record
                      class prior emissions followthrough
                      {:as-of latest
                       :outer-loop-run-id (str "capability-zone-retro:" month)
                       :window-days (.lengthOfMonth (YearMonth/parse month))
                       :evidence-refs (mapv :id items)})]
          {:priors (assoc priors class (record-entry record))
           :records (conj records record)}))
      {:priors {} :records []}
      (map vector windows)))))

(defn- drifted-seeds [seed-records items assignments]
  (let [item-by-id (into {} (map (juxt :id identity) items))
        accepted-by-class (group-by :class (remove :resisted? assignments))]
    (mapv
     (fn [seed]
       (let [class (keyword (:class seed))
             evidence (get accepted-by-class class)
             vectors (mapv #(get-in item-by-id [(:id %) :embedding]) evidence)
             text-seed (:text_seed seed)
             centroid-seed (if (seq vectors) (centroid vectors) text-seed)
             distance (- 1.0 (zones/cosine text-seed centroid-seed))]
         (assoc seed
                :centroid_seed centroid-seed
                :centroid_evidence_count (count vectors)
                :old_to_new_cosine_distance distance
                :seed_generations ["text-seed" "centroid-seed"])))
     seed-records)))

(defn- fraction [n d]
  (if (zero? d) "0/0 (n/a)" (format "%d/%d (%.4f)" n d (/ n (double d)))))

(defn- coverage-markdown [prepared assignments]
  (let [by-repo (group-by :repo assignments)
        by-grain (group-by :grain assignments)
        coverage (:coverage prepared)]
    (str
     "# M-capability-zones S1 honest coverage — 2026-07-19\n\n"
     "## Scope and semantics\n\n"
     "The pinned 17-name FUTON corpus was traversed without a commit cap. For each Git "
     "repository, every commit reachable from its detected default branch was described. "
     "Merge-reachable history is included; non-default and unreachable orphan branches are not. "
     "`futonY` was present as a directory but was not a Git repository. Worktree clones were "
     "excluded. Classification used BGE `BAAI/bge-large-en-v1.5` and text-seed nearest cosine. "
     "A global margin below 0.01 resisted description; resisted items are retained.\n\n"
     "Grains are best-first: attempt/feature text when its markers and current reachable files "
     "exist; mission/pattern text when those paths exist at HEAD; otherwise raw commit message "
     "plus paths. Historical file bodies that no longer exist at HEAD were unreachable as rich "
     "grains and therefore used the raw fallback rather than being dropped.\n\n"
     "## Per-grain fractions\n\n"
     "| Grain | Described / corpus | Resisted / described |\n|---|---:|---:|\n"
     (apply str
            (for [[grain xs] (sort-by key by-grain)
                  :let [resisted (count (filter :resisted? xs))]]
              (format "| `%s` | %s | %s |\n" (name grain)
                      (fraction (count xs) (count assignments))
                      (fraction resisted (count xs)))))
     "\n## Per-repository fractions\n\n"
     "| Repository | Default ref | Described / reachable history | Resisted / described | Reachability |\n"
     "|---|---|---:|---:|---|\n"
     (apply str
            (for [[repo stats] (sort-by key coverage)
                  ;; coverage keys arrive keywordized from JSON; assignment
                  ;; :repo values are strings — align before lookup.
                  :let [xs (get by-repo (name repo) [])
                        resisted (count (filter :resisted? xs))]]
              (if (:reachable stats)
                (format "| `%s` | `%s` | %s | %s | reachable |\n"
                        (name repo) (:default_ref stats)
                        (fraction (:described stats) (:total stats))
                        (fraction resisted (count xs)))
                (format "| `%s` | — | %s | %s | unreachable: %s |\n"
                        (name repo) (fraction 0 0) (fraction 0 0) (:reason stats)))))
     "\n## Resistance and unreachable material\n\n"
     "Resistance means the top two seed cosines were separated by less than 0.01; it is an "
     "explicit ambiguity result, not a discarded item. Full SHAs and margins are retained in "
     "`futon2/data/capability_zones/harvest-2026-07-19.edn`. No live store was read for "
     "classification or written by the harvest. Attempt/feature and mission/pattern bodies "
     "deleted from the default-branch checkout were unreachable as rich text; their commits "
     "remain covered at raw grain.\n")))

(defn -main [& _]
  (let [prepared (json/parse-string (slurp prepared-path) true)
        seeds (json/parse-string (slurp external-seeds-path) true)
        items (:items prepared)
        assignments (binding [zones/*seed-generation* :text
                              zones/*seed-records* seeds]
                      (mapv annotate items))
        records (update-records assignments)
        drifted (drifted-seeds seeds items assignments)
        artifact {:schema :capability-zone-harvest.v1
                  :generated-at "2026-07-19T00:00:00Z"
                  :corpus {:repos (:corpus prepared)
                           :history :default-branch-reachable
                           :worktree-clones-excluded true}
                  :embedding {:model (:embed_model prepared)
                              :dimension (:embed_dim prepared)
                              :metric :interim-metric}
                  :classification {:seed-generation :text-seed
                                   :membership :nearest-cosine
                                   :margin-threshold margin-threshold
                                   :resisted-policy :top1-minus-top2-below-threshold}
                  :observation-semantics
                  {:emission "A non-resisted default-branch commit assigned to the class."
                   :followthrough "The assigned commit remains reachable and is not named by a later reachable `This reverts commit <sha>` commit."
                   :attribution :stigmergic-not-biographical
                   :chunking :calendar-month-per-class}
                  :coverage (:coverage prepared)
                  :seed-drift (mapv #(select-keys % [:class :centroid_evidence_count
                                                     :old_to_new_cosine_distance])
                                    drifted)
                  :summary {:items (count assignments)
                            :assigned (count (remove :resisted? assignments))
                            :resisted (count (filter :resisted? assignments))
                            :records (count records)}
                  :records records
                  :assignments assignments}]
    (io/make-parents artifact-path)
    (spit artifact-path (str (pr-str artifact) "\n"))
    (let [seed-json (str (json/generate-string drifted) "\n")]
      (spit external-seeds-path seed-json)
      (spit checked-seeds-path seed-json))
    (spit coverage-path (coverage-markdown prepared assignments))
    (println (pr-str (:summary artifact)))))

(apply -main *command-line-args*)
