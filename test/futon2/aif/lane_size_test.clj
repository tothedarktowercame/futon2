(ns futon2.aif.lane-size-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.wm.cascade-decision :as decision]))

(deftest declines-are-carried-once-and-lane-locally
  (let [lanes [{:target "a" :candidates [{:id :a1}]}
               {:target "b" :candidates [{:id :b1}]}
               {:target "c" :candidates [{:id :c1}]}]
        drops [{:target "a" :candidate :a1 :possible-costs (range 20)}
               {:target "b" :candidate :b1 :possible-costs (range 20)}
               {:target "b" :candidate :b2 :possible-costs (range 20)}
               {:target "outside" :reason :admission-refused}]
        localized (decision/lane-local-dropped-candidates lanes drops)]
    (is (= [1 2 0] (mapv #(count (:dropped-candidates %)) localized)))
    (is (= #{"a"} (set (map :target (:dropped-candidates (first localized))))))
    (is (= #{"b"} (set (map :target (:dropped-candidates (second localized))))))
    (is (= drops drops) "the family-level lossless carrier is unchanged")
    (testing "a family-wide decline cannot leak into every lane"
      (is (not-any? #(some (fn [d] (= "outside" (:target d)))
                           (:dropped-candidates %))
                    localized)))))

(deftest scored-target-count-means-numeric-g-not-lane-count
  (let [lanes [{:target "a" :candidates [{:id :a1} {:id :a2}]}
               {:target "b" :candidates [{:id :b1}]}
               {:target "c" :candidates [{:id :c1}]}]]
    (is (= 2 (decision/numeric-g-target-count
              lanes [{:id :a2 :g 1.0} {:id :b1 :g :infinite} {:id :c1 :g 3.0}])))
    (is (= 0 (decision/numeric-g-target-count
              lanes [{:id :a1 :g nil} {:id :b1 :g :infinite}])))
    (is (= 1 (decision/numeric-g-target-count
              lanes [{:id :a1 :g 1.0} {:id :a2 :g 2.0}])))
    (is (= 0 (decision/numeric-g-target-count
              lanes [{:id :not-in-any-lane :g 1.0}])))))
