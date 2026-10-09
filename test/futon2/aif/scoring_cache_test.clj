(ns futon2.aif.scoring-cache-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.efe :as efe]
            [futon2.aif.observation-model-route-test :as route])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest cache-invalidates-and-refreshes-deterministically
  (let [dir (Files/createTempDirectory "wm-score-cache-" (make-array FileAttribute 0))
        path (str (.resolve dir "global-rank.edn"))
        candidates (mapv #(assoc % :id (keyword (str "C" %2)))
                         (take 4 (cycle (:candidates route/fixture)))
                         (range 4))
        opts (assoc (route/fixture-opts :independent-judgement)
                    :scoring-cache? true
                    :scoring-cache-path path
                    :scoring-cache-top-k 2
                    :scoring-cache-refresh-count 1)
        state {:cascade-belief (:q0 route/fixture)}
        first-run (efe/rank-actions state candidates opts)
        second-run (efe/rank-actions state candidates opts)
        edited (assoc opts :cascade-spec (assoc (:cascade-spec opts) :mu 2))
        edited-run (efe/rank-actions state candidates edited)
        cache-status #(mapv (comp :status :cache) %)]
    (is (= [:fresh :fresh :fresh :fresh] (cache-status first-run)))
    (is (= 3 (count (filter #{:fresh} (cache-status second-run)))))
    (is (= 4 (count (filter #{:fresh} (cache-status edited-run)))))
    (is (= (mapv :controller-score (sort-by :cascade-id first-run))
           (mapv :controller-score
                 (sort-by :cascade-id (efe/rank-actions state candidates
                                                        (assoc opts :scoring-cache? false))))))))
