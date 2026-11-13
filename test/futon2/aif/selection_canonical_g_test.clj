(ns futon2.aif.selection-canonical-g-test
  "Selection G over cascades (Joe, 2026-11-13; Lean SelectionGOverCascades).
  The field is the Lucy ladder case that abstained :insufficient-comparison:
  tickets 08 and 09 ready with equal Giraffe facts, 09 alone placed under a way."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.selection-canonical-g :as sg]))

(def facts
  {"T-08" {:status "ready" :unblocks 0 :cone 0 :prerequisites []
           :placement {:status "open" :action "work" :ways []}}
   "T-09" {:status "ready" :unblocks 0 :cone 0 :prerequisites []
           :placement {:status "open" :action "work" :ways ["one-queryable-self-account"]}}})

(defn- g [entries id] (get-in (sg/score entries facts) [:by-id id :G-cascade]))

(def only ["only-what-can-start-now"])
(def giraffe ["only-what-can-start-now" "prepare-the-ground" "what-others-wait-on-first"])
(def serves ["only-what-can-start-now" "serves-a-way" "what-others-wait-on-first"])

(deftest same-pick-same-patterns-same-g
  (let [es [{:id :a :patterns only :pick "T-08" :relation-class :useful-elsewhere}
            {:id :b :patterns only :pick "T-08" :relation-class :useful-elsewhere}]]
    (is (number? (g es :a)))
    (is (= (g es :a) (g es :b)))))

(deftest same-pick-different-firing-patterns-different-g
  (testing "Joe's ruling: G is over the cascade, not its pick. The pick-only
            scorer (outer-score) gives these two one G by construction."
    (let [es [{:id :plain :patterns only :pick "T-09" :relation-class :useful-elsewhere}
              {:id :way :patterns serves :pick "T-09" :relation-class :useful-elsewhere}]]
      (is (number? (g es :plain)))
      (is (not= (g es :plain) (g es :way)))
      (is (< (g es :way) (g es :plain))
          "serves-a-way fires on T-09's placement: one more pattern's novelty"))))

(deftest ladder-field-gives-two-classes-below-stopping
  (let [es [{:id :only-what-can-start-now :patterns only :pick "T-08" :relation-class :useful-elsewhere}
            {:id :find-out-before-building :patterns ["find-out-before-building"] :pick :no-pick}
            {:id :giraffe-order :patterns giraffe :pick "T-08" :relation-class :useful-elsewhere}
            {:id :serves-a-way :patterns serves :pick "T-09" :relation-class :useful-elsewhere}]
        r (sg/score es facts)
        G #(get-in r [:by-id % :G-cascade])
        stop (get-in r [:stopping :G-cascade])]
    (is (nil? (:refusal r)))
    (testing "patterns that cannot fire on the pick add nothing"
      (is (= (G :only-what-can-start-now) (G :giraffe-order))))
    (is (< (G :serves-a-way) (G :only-what-can-start-now) stop))
    (is (= stop (G :find-out-before-building)) "no pick scores as stopping")))

(deftest patternless-cascade-is-the-empty-policy
  (let [es [{:id :empty :patterns [] :pick "T-08" :relation-class :useful-elsewhere}]
        r (sg/score es facts)]
    (is (= (get-in r [:stopping :G-cascade]) (get-in r [:by-id :empty :G-cascade])))))

(deftest uninterpreted-pattern-refuses
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"no interpretation"
                        (sg/score [{:id :x :patterns ["take-the-cheap-win"] :pick "T-08"
                                    :relation-class :useful-elsewhere}] facts))))

(deftest fired-patterns-are-recorded-for-requirements-q12
  (let [es [{:id :only-what-can-start-now :patterns only :pick "T-08" :relation-class :useful-elsewhere}
            {:id :giraffe-order :patterns giraffe :pick "T-08" :relation-class :useful-elsewhere}
            {:id :serves-a-way :patterns serves :pick "T-09" :relation-class :useful-elsewhere}]
        fired (:fired (sg/score es facts))]
    (is (= ["only-what-can-start-now"] (fired :only-what-can-start-now)))
    (is (= ["only-what-can-start-now"] (fired :giraffe-order))
        "no cone, no unblocks on T-08: prepare-the-ground and what-others-wait-on-first cannot fire")
    (is (= ["only-what-can-start-now" "serves-a-way"] (fired :serves-a-way)))
    (is (= [] (fired :futon2.aif.selection-canonical-g/stopping)))))
