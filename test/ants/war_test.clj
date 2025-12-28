(ns ants.war-test
  (:require [clojure.test :refer [deftest is testing]]
            [ants.war :as war]
            [ants.ui :as ui]))

(deftest new-world-sane
  (let [world (war/new-world {:size [10 10]
                              :ants-per-side 3
                              :ticks 5})]
    (testing "homes placed"
      (is (= (set (or (:armies world) [:classic :aif]))
             (set (keys (:homes world)))))
      (doseq [[species loc] (:homes world)]
        (is (= species (get-in world [:grid :cells loc :home])))))
    (testing "ants spawned"
      (is (= 6 (count (:ants world))))
      (doseq [[id {:keys [loc h]}] (:ants world)]
        (is (= id (get-in world [:grid :cells loc :ant])))
        (is (<= 0.0 h 1.0))))
    (testing "scores initialised"
      (let [expected (into {} (map (fn [sp] [sp 0.0])
                                   (or (:armies world) [:classic :aif])))]
        (is (= expected (:scores world)))))
    (testing "colonies initialised"
      (let [reserve (double (or (get-in world [:config :hunger :queen :initial]) 0.0))]
        (doseq [[_ {:keys [reserves starved-ticks]}] (:colonies world)]
          (is (= reserve reserves))
          (is (zero? starved-ticks)))))))

(deftest step-progresses
  (let [world (war/new-world {:size [8 8]
                              :ants-per-side 2
                              :ticks 2})
        world' (war/step world)]
    (testing "tick increments"
      (is (= 1 (:tick world'))))
    (testing "events emitted"
      (is (seq (:last-events world'))))
    (testing "hud renders"
      (is (string? (ui/scoreboard world'))))))

(deftest simulate-respects-ticks
  (let [world (war/simulate {:size [6 6]
                             :ants-per-side 1
                             :ticks 3}
                            {:hud? false})]
    (is (= 3 (:tick world)))))

(deftest cyber-army-spawns
  (let [world (war/new-world {:size [8 8]
                              :ticks 4
                              :ants-per-side 2
                              :armies [:cyber :aif]
                              :cyber {:pattern :cyber/white-space-scout}})
        cyber-ants (filter #(= :cyber (:species %)) (vals (:ants world)))]
    (is (= 2 (count cyber-ants)))
    (is (every? #(= :aif (:brain %)) cyber-ants))
    (is (every? :aif-config cyber-ants))))

(deftest workers-do-not-gather-from-home
  (let [world (war/new-world {:size [6 6]
                              :ants-per-side 1})
        home (get-in world [:homes :aif])
        ant (->> (:ants world)
                 (vals)
                 (some #(when (and (= :aif (:species %))
                                   (= home (:loc %)))
                          %)))
        start-food (get-in world [:grid :cells home :food])
        {world' :world ant' :ant gather :gather} (#'war/gather-food world ant)
        end-food (get-in world' [:grid :cells home :food])]
    (is (zero? gather))
    (is (= start-food end-food))
    (is (= (:cargo ant) (:cargo ant')))))

(defn- strip-food
  [world]
  (update-in world [:grid :cells]
             (fn [cells]
               (into {}
                     (map (fn [[loc cell]]
                            [loc (assoc cell :food 0.0)])
                          cells)))))

(deftest starvation-kills-ants
  (let [cfg {:size [5 5]
             :ants-per-side 1
             :hunger {:initial 0.99
                      :burn 0.04
                      :feed 0.0
                      :rest 0.0
                      :gather-feed 0.0
                      :deposit-feed 0.0
                      :home-bonus 0.0
                      :death-threshold 0.995
                      :queen {:initial 5.0
                              :burn 0.0
                              :per-ant 0.0
                              :starvation-grace 2
                              :starvation-boost 0.0}}}
        final (loop [w (-> (war/new-world cfg)
                           strip-food)
                     i 0]
                (cond
                  (:terminated? w) w
                  (> i 20) w
                  :else (recur (strip-food (war/step w)) (inc i))))]
    (is (:terminated? final))
    (is (#{:all-ants-dead :queen-starved}
         (get-in final [:termination :reason])))
    (when (= :all-ants-dead (get-in final [:termination :reason]))
      (is (empty? (:ants final))))
    (is (some (comp seq :graves second) (get-in final [:grid :cells])))))

(deftest queen-starvation-terminates
  (let [world (war/new-world {:size [5 5]
                              :ants-per-side 1
                              :hunger {:initial 0.1
                                       :burn 0.0
                                       :queen {:initial 0.0
                                               :burn 0.6
                                               :per-ant 0.0
                                               :starvation-grace 1
                                               :starvation-boost 0.0}}})
        world' (war/step world)]
    (is (:terminated? world'))
    (is (= :queen-starved (get-in world' [:termination :reason])))))

(deftest alice-death-terminates
  (let [cfg {:size [10 10]
             :ants-per-side 6
             :hunger {:initial 0.995
                      :burn 0.2
                      :feed 0.0
                      :rest 0.0
                      :gather-feed 0.0
                      :deposit-feed 0.0
                      :home-bonus 0.0
                      :death-threshold 0.99
                      :queen {:initial 5.0
                              :burn 0.0
                              :per-ant 0.0
                              :starvation-grace 10
                              :starvation-boost 0.0}}}
        final (loop [w (-> (war/new-world cfg)
                           strip-food)
                     i 0]
                (cond
                  (:terminated? w) w
                  (> i 20) w
                  :else (recur (strip-food (war/step w)) (inc i))))]
    (is (:terminated? final))
    (is (= :alice-dead (get-in final [:termination :reason])))
    (is (some (fn [[_ cell]] (some #(= (:id %) :aif-5) (:graves cell)))
             (get-in final [:grid :cells])))
    (is (true? (get-in final [:hero :alice-dead?])))))

(deftest richest-neighbour-avoids-home
  (let [world (war/new-world {:size [6 6]
                              :ants-per-side 1})
        home (get-in world [:homes :aif])
        neighbour (#'war/richest-neighbour world home :aif)]
    (is (not= home neighbour))))
