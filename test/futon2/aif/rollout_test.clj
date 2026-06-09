(ns futon2.aif.rollout-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.rollout :as rollout]
            [meme.step :as meme-step]))

(def cap-snapshot
  {"agency" {:id "scope/capability/agency"
             :props {:capability/frontier? false
                     :capability/status :held}}
   "ai-passes-prelims" {:id "scope/capability/ai-passes-prelims"
                        :props {:capability/frontier? true
                                :capability/status :held}}})

(deftest shared-step-routes-ordinary-and-frontier-caps
  (let [ordinary-state {:arrows {["h" "w"] {:have "h" :want "w"
                                            :status :open
                                            :advances-cap "agency"}}
                        :cap-overlay cap-snapshot
                        :reachable #{"h"}}
        frontier-state {:arrows {["h" "w"] {:have "h" :want "w"
                                            :status :open
                                            :advances-cap "ai-passes-prelims"}}
                        :cap-overlay cap-snapshot
                        :reachable #{"h"}}
        ordinary (meme-step/step ordinary-state {:have "h" :want "w"
                                                 :advances-cap "agency"})
        frontier (meme-step/step frontier-state {:have "h" :want "w"
                                                 :advances-cap "ai-passes-prelims"})]
    (is (= :constructed (get-in ordinary [:arrows ["h" "w"] :status])))
    (is (= :satisfied (get-in ordinary [:cap-overlay "agency" :props :capability/status])))
    (is (= :claimed (get-in frontier [:cap-overlay "ai-passes-prelims" :props :capability/status])))))

(deftest rollout-search-unlocks-second-step
  (let [moves [{:move/id "a" :move/class :close-hole
                :have "root" :want "bridge" :score 1.0 :delta-g -0.1
                :rank 1 :move/terminal? false}
               {:move/id "b" :move/class :advance-capability
                :have "bridge" :want "goal" :advances-cap "agency"
                :score 3.0 :delta-g -1.0 :rank 2 :move/terminal? false}
               {:move/id "greedy" :move/class :close-hole
                :have "root" :want "small" :score 2.0 :delta-g -0.2
                :rank 3 :move/terminal? false}]
        state {:arrows {}
               :cap-overlay cap-snapshot
               :reachable #{"root"}}
        greedy (rollout/greedy-one-step state moves :top-k 3)
        best (rollout/best-rollout state moves :depth 2 :top-k 3 :gamma 0.9)]
    (is (= ["greedy"] (mapv :move/id (:policy greedy))))
    (is (= ["a" "b"] (mapv :move/id (:policy best))))
    (is (< (:G best) (:G greedy)))))

(deftest real-stub-loads-and-masks-reachable-moves
  (let [move-set (rollout/load-move-set)
        ms (rollout/moves move-set)
        state {:reachable #{"scope/interim-director-proxy-metric-inventory/pattern#open"}
               :arrows {}
               :cap-overlay {}}]
    (is (= 19 (count ms)))
    (is (every? :move/id ms))
    (is (= ["scope/interim-director-proxy-metric-inventory/pattern#open->scope/interim-director-proxy-metric-inventory/pattern#closed"]
           (mapv :move/id (rollout/reachable-moves state ms))))))
