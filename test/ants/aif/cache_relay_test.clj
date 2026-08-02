(ns ants.aif.cache-relay-test
  (:require [ants.aif.forward :as forward]
            [ants.war :as war]
            [clojure.test :refer [deftest is testing]]))

(defn- view
  [cells]
  {:size [24 24]
   :cells cells
   :ants {}
   :home [2 2]
   :max-food 5.0
   :max-pher 3.0})

(deftest carrying-pressure-makes-drop-visible-without-a-bonus
  (let [ant {:id :a :species :aif :loc [12 12] :cargo 0.7 :h 0.5}
        local (view {[12 12] {:food 0.0 :pher 0.0}})
        dropped (forward/forward-predict local ant :drop {:seed 7})
        held (forward/forward-predict local ant :hold {:seed 7})]
    (is (zero? (get-in dropped [:mean :cargo])))
    (is (< (get-in dropped [:mean :h]) (get-in held [:mean :h]))
        "the existing cargo term, not a drop bonus, lowers predicted hunger")))

(deftest cache-food-is-conserved-and-provenance-is-analytical-only
  (let [a {:id :a :species :aif :loc [12 12] :cargo 0.6 :h 0.4}
        initial (war/new-world {:size [24 24] :armies [:aif] :ants-per-side 1})
        initial (-> initial
                    (assoc-in [:grid :cells [12 12] :food] 0.0)
                    (assoc-in [:grid :cells [12 12] :cache-provenance] {}))
        drop-result (forward/ant-kernel (view {[12 12] {:food 0.0 :pher 0.0}}) a :drop)
        after-drop (#'war/apply-kernel-effects initial (:ant drop-result) (:effects drop-result))]
    (is (= 0.6 (get-in after-drop [:grid :cells [12 12] :food])))
    (is (= {:a 0.6} (get-in after-drop [:grid :cells [12 12] :cache-provenance])))
    (is (zero? (get-in after-drop [:ants :a :cargo])))
    (is (= 1 (get-in after-drop [:stats :relays :aif :cache-drops])))))

(deftest another-ant-can-pick-up-and-complete-a-relay
  (let [b {:id :b :species :aif :loc [12 12] :cargo 0.0 :h 0.4}
        cache-cell {:food 1.0 :pher 0.0 :cache-provenance {:a 0.6}}
        gathered (#'forward/gather-food (view {[12 12] cache-cell}) b)
        b-loaded (:ant gathered)
        initial (-> (war/new-world {:size [24 24] :armies [:aif] :ants-per-side 1})
                    (assoc-in [:grid :cells [12 12]] cache-cell))
        after-pickup (#'war/apply-kernel-effects
                      initial b-loaded
                      {:food-deltas (:food-delta gathered)
                       :cache-provenance (:cache-provenance gathered)
                       :cache-pickup (:cache-pickup gathered)})
        b-home (assoc b-loaded :loc [2 2])
        delivered (#'forward/deposit-food (view {}) b-home)
        final (#'war/apply-kernel-effects
               after-pickup (:ant delivered)
               {:score-delta (:score-delta delivered)
                :reserve-delta (:reserve-delta delivered)
                :relay-delivery (:relay-delivery delivered)})]
    (testing "food and cached fraction are conserved during proportional pickup"
      (is (= 0.7 (:gather gathered)))
      (is (< (Math/abs (- 0.3 (get-in after-pickup [:grid :cells [12 12] :food]))) 1.0e-12))
      (is (< (Math/abs (- 0.42 (get-in b-loaded [:cargo-provenance :a]))) 1.0e-12))
      (is (< (Math/abs (- 0.18 (get-in after-pickup
                                       [:grid :cells [12 12] :cache-provenance :a])))
             1.0e-12)))
    (testing "A's cache, carried home by B, is one completed relay"
      (is (= 1 (get-in final [:stats :relays :aif :cross-ant-pickups])))
      (is (= 1 (get-in final [:stats :relays :aif :completed])))
      (is (< (Math/abs (- 0.42 (get-in final [:stats :relays :aif :delivered-amount])))
             1.0e-12)))))
