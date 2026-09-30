(ns futon2.aif.cascade-shape-g-test
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-shape-g :as shape-g]))

(def artifacts "holes/labs/wm-contract/mission-head-cascades-2026-09-30")

(def same-order-a
  {:nodes [{:pattern "p/a"} {:pattern "p/b"} {:pattern "p/c"}]
   :precedence ["p/a" "p/b" "p/c"]
   :edges [{:from "p/a" :to "p/b" :kind :precedes}
           {:from "p/b" :to "p/c" :kind :precedes}]})

(def same-order-b
  {:nodes (:nodes same-order-a)
   :precedence (:precedence same-order-a)
   :edges [{:from "p/a" :to "p/c" :kind :precedes}]})

(deftest arrangement-reaches-the-real-scorer
  (let [a (shape-g/score-arranged "target" "chain" same-order-a)
        b (shape-g/score-arranged "target" "join" same-order-b)]
    (is (= [:computed :computed] [(:status a) (:status b)]))
    (is (not= (:g a) (:g b))
        "same firing list, different edges: compiling only the list makes this fail")
    (doseq [r [a b]]
      (is (every? #(Double/isFinite (double %))
                  ((juxt :g :risk :ambiguity :information-gain) r)))
      (is (= (:horizon r) (count (:preference-at-each-step r)))))))

(deftest overlap-is-a-shared-advance-token
  (let [c (assoc same-order-a :edges [{:from "p/a" :to "p/b" :kind :overlap}])
        candidate (shape-g/arranged->candidate "t" "overlap" c)
        pa (first (:precedence candidate))
        pb (second (:precedence candidate))]
    (is (= 1 (count (set/intersection (:produces pa) (:produces pb)))))
    (is (empty? (get-in pb [:guard :clauses 0 :present]))
        "overlap co-advances shared state; it is not an enabling edge")))

(deftest ^:slow all-recorded-head-policies-have-g
  (let [policies (shape-g/materialize-policies artifacts)
        results (mapv #(shape-g/score-arranged (:target %) (:policy-id %) (:cascade %)) policies)]
    (is (= 81 (count policies)))
    (is (= 81 (count (filter #(= :computed (:status %)) results))))
    (is (zero? (count (remove #(Double/isFinite (double (:g %))) results))))
    (is (every? #(= (:horizon %) (count (:preference-at-each-step %))) results))))
