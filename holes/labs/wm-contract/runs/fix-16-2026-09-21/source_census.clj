(ns source-census
  "Read declared sources and their observation checks; never run a tick or selector."
  (:require [clojure.pprint :as pp]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.cascade-sources :as sources]
            [futon2.report.war-machine :as wm]))

(let [[directory horizon-text] *command-line-args*
      horizon (when horizon-text (parse-long horizon-text))]
  (assert (and directory (pos-int? horizon)) "Supply source directory and declared horizon")
  (let [loaded (sources/load-declared directory)
        targets (vec (sort (keys (:universes loaded))))
        assembled (problems/assemble
                   {:targets targets :sources (sources/with-context-fn (assoc loaded :horizon-steps horizon))})
        admissions (mapv #'wm/admit-cascade-problem (:problems assembled))]
    (pp/pprint {:observed-at (str (java.time.Instant/now))
                :horizon horizon :source-directory directory
                :source-files (:files loaded)
                :facts (:universes loaded) :observations (:observations loaded)
                :assembly-refusals (:refusals assembled)
                :rows (mapv (fn [p a]
                              {:target (:target p)
                               :submitted (count (:constructed-candidates p))
                               :admitted (count (get-in a [:problem :constructed-candidates]))
                               :declines (:declines a) :refusal (:refusal a)})
                            (:problems assembled) admissions)})))
(shutdown-agents)
