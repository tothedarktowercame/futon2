(ns ants.food-symmetry-test
  "Food-field symmetry and scenario-selection guards.

   Context: the :patchy/:sparse scenarios were unreachable through
   `make-seeded-world` because it passed :food where `build-grid` reads
   :food-distribution, so every arm silently ran on snowdrift. These tests pin
   both the key name and the symmetry invariant that makes a two-army
   head-to-head measure brains rather than spawn corners."
  (:require [clojure.test :refer [deftest is testing]]
            [ants.war :as war]
            [ants.aif.experiment :as exp]))

(defn- food-map
  [world]
  (into {} (map (fn [[loc cell]] [loc (:food cell)])) (get-in world [:grid :cells])))

(defn- total-food
  [world]
  (reduce + 0.0 (vals (food-map world))))

(defn- rot180
  [[w h] [x y]]
  [(- w 1 x) (- h 1 y)])

(deftest scenario-key-selects-distribution
  (testing "food-distribution config key actually reaches build-grid"
    (doseq [d [:snowdrift :patchy :sparse]]
      (is (= d (get-in (war/new-world {:size [20 20] :food-distribution d})
                       [:grid :food-distribution]))
          (str "distribution " d " should be recorded on the grid")))))

(deftest patchy-and-sparse-differ-from-snowdrift
  (testing "the three scenarios build genuinely different food fields"
    (let [size [20 20]
          w-of (fn [d] (war/new-world {:size size :food-distribution d
                                       :food-opts {:seed 7}}))
          snow (food-map (w-of :snowdrift))
          patchy (food-map (w-of :patchy))
          sparse (food-map (w-of :sparse))]
      (is (not= snow patchy) "patchy must not collapse to snowdrift")
      (is (not= snow sparse) "sparse must not collapse to snowdrift")
      (is (not= patchy sparse) "sparse must not collapse to patchy"))))

(deftest patchy-food-seed-changes-layout
  (testing "distinct seeds give distinct patchy layouts"
    (let [f (fn [seed] (food-map (war/new-world {:size [20 20]
                                                 :food-distribution :patchy
                                                 :food-opts {:seed seed}})))]
      (is (not= (f 1) (f 2))))))

(deftest make-seeded-world-honours-scenario
  (testing "the R16 witness harness reaches the scenario it is asked for"
    (doseq [d [:snowdrift :patchy :sparse]]
      (is (= d (get-in (exp/make-seeded-world :aif d 11 1 [16 16] 5)
                       [:grid :food-distribution]))))
    (let [snow (food-map (exp/make-seeded-world :aif :snowdrift 11 1 [16 16] 5))
          patchy (food-map (exp/make-seeded-world :aif :patchy 11 1 [16 16] 5))]
      (is (not= snow patchy)
          "patchy arm must not silently run on snowdrift"))))

(deftest symmetric-patchy-is-rot180-invariant
  (testing "symmetric? closes the patch set under rot180, so food is invariant"
    (let [size [30 30]
          world (war/new-world {:size size
                                :food-distribution :patchy
                                :food-opts {:seed 7 :num-patches 5
                                            :patch-radius 3 :symmetric? true}})
          foods (food-map world)]
      (is (every? (fn [[loc v]] (== v (get foods (rot180 size loc))))
                  foods)
          "every cell must equal its 180-degree image")
      (is (pos? (total-food world)) "sanity: the field is not empty"))))

(deftest symmetric-patchy-equalises-the-two-homes
  (testing "both colony homes see identical food within a local window"
    (let [size [30 30]
          world (war/new-world {:size size
                                :armies [:classic :aif]
                                :food-distribution :patchy
                                :food-opts {:seed 7 :num-patches 5
                                            :patch-radius 3 :symmetric? true}})
          foods (food-map world)
          near (fn [[hx hy]]
                 (reduce + 0.0
                         (for [dx (range -3 4) dy (range -3 4)]
                           (get foods [(+ hx dx) (+ hy dy)] 0.0))))
          homes (:homes world)]
      (is (= 2 (count homes)))
      ;; Per-cell equality is exact (see symmetric-patchy-is-rot180-invariant);
      ;; this window sum adds those same values in mirrored order, and float
      ;; addition is not associative, so compare within a tolerance.
      (is (< (abs (- (near (:classic homes)) (near (:aif homes)))) 1e-9)
          "the spawn-position food confound must vanish at both homes"))))

(deftest asymmetric-patchy-still-available
  (testing "symmetric? defaults off, preserving existing patchy behaviour"
    (let [size [30 30]
          opts {:seed 7 :num-patches 5 :patch-radius 3}
          plain (food-map (war/new-world {:size size :food-distribution :patchy
                                          :food-opts opts}))
          sym (food-map (war/new-world {:size size :food-distribution :patchy
                                        :food-opts (assoc opts :symmetric? true)}))]
      (is (not= plain sym)
          "symmetric? must actually change the field"))))
