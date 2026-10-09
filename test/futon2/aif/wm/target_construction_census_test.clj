(ns futon2.aif.wm.target-construction-census-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.wm.cascade-decision :as decision]))

(defn pin [id]
  {:id id :sha256 (str "sha-" id) :revision "rev"})

(defn problem [slice]
  {:target "M-x"
   :query-time-slice slice
   :cascade-problem {:interpretations {:p1 {} :p2 {}}}})

(deftest census-records-real-slice-pool-and-policy-count
  (let [slice {:schema :wm/query-time-library-slice-v1
               :candidates [{:pattern :p1} {:pattern :p2}]
               :library-size 3 :library-pins (mapv pin [:p1 :p2 :p3])}
        census (decision/target-construction-census
                [(problem slice)]
                [{:target "M-x" :id :c1} {:id {:target "M-x" :id :c2}}])]
    (is (= [:p1 :p2] (:slice (first census))))
    (is (= #{:p1 :p2} (set (:pool (first census)))))
    (is (true? (:slice-from-whole-library (first census))))
    (is (= 2 (:policy-count (first census))))))

(deftest census-does-not-upgrade-partial-or-missing-library-evidence
  (let [partial {:schema :wm/query-time-library-slice-v1
                 :candidates [{:pattern :p1}] :library-size 3
                 :library-pins [(pin :p1)]}
        row (first (decision/target-construction-census [(problem partial)] []))
        absent (decision/target-construction-census
                [(dissoc (problem partial) :query-time-slice)] [])]
    (is (false? (:slice-from-whole-library row)))
    (is (= 0 (:policy-count row)))
    (is (= {:status :absent
            :reason :query-time-construction-slice-not-recorded
            :targets ["M-x"]}
           absent))
    (is (= :absent
           (:status (decision/target-construction-census [] []))))))
