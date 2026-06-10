(ns futon2.aif.arguing-worlds-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.arguing-worlds :as aw]))

(defn- mv
  [id class have want score delta-g]
  {:move/id id
   :move/class class
   :have have
   :want want
   :score score
   :delta-g delta-g
   :rank 1
   :move/terminal? false})

(def fixture-state {:arrows {} :cap-overlay {} :reachable #{"root"}})

(deftest semantic-witness-dialectic-beats-single-c-buildout
  (let [bad-c {:buildout/id :shiny-single
               :policy [(mv "bad" :advance-capability "root" "bad" 10.0 -0.1)]
               :pattern-set #{:shiny "bad"}
               :implied-moves ["bad"]
               :C 10.0}
        good-g {:buildout/id :two-step-discharge
                :policy [(mv "bridge" :close-hole "root" "bridge" 1.0 -0.2)
                         (mv "finish" :close-hole "bridge" "done" 1.0 -2.0)]
                :pattern-set #{:discharge "bridge" "finish"}
                :implied-moves ["bridge" "finish"]
                :C 2.0}
        third {:buildout/id :other-world
               :policy [(mv "other" :graft-pattern "root" "other" 1.0 -0.3)]
               :pattern-set #{:other "other"}
               :implied-moves ["other"]
               :C 1.0}
        result (aw/referee-harness fixture-state [bad-c good-g third]
                                  :diversity-opts {:max-overlap 0.4})]
    (is (= :dialectic-wins (:verdict result)))
    (is (= :realized-G (-> result :winner :yardstick)))
    (is (= :two-step-discharge (-> result :winner :buildout/id)))
    (is (= :shiny-single (-> result :top-c-buildout :buildout/id)))))

(deftest semantic-null-single-best-c-also-wins-realized-g
  (let [best {:buildout/id :best-both
              :policy [(mv "best" :close-hole "root" "best" 3.0 -3.0)]
              :pattern-set #{:best "best"}
              :implied-moves ["best"]
              :C 3.0}
        weaker {:buildout/id :weaker
                :policy [(mv "weak" :graft-pattern "root" "weak" 1.0 -0.2)]
                :pattern-set #{:weak "weak"}
                :implied-moves ["weak"]
                :C 1.0}
        other {:buildout/id :other
               :policy [(mv "other" :advance-capability "root" "other" 0.5 -0.1)]
               :pattern-set #{:other "other"}
               :implied-moves ["other"]
               :C 0.5}
        result (aw/referee-harness fixture-state [best weaker other]
                                  :diversity-opts {:max-overlap 0.4})]
    (is (= :single-best-holds (:verdict result)))
    (is (= :best-both (-> result :winner :buildout/id)))
    (is (= :best-both (-> result :top-c-buildout :buildout/id)))))

(deftest non-diverse-generator-is-reported-before-judging
  (let [a {:buildout/id :a
           :policy [(mv "same" :close-hole "root" "x" 1.0 -1.0)]
           :pattern-set #{:same "same"}
           :implied-moves ["same"]
           :C 1.0}
        b (assoc a :buildout/id :b)
        result (aw/referee-harness fixture-state [a b])]
    (is (= :monotone-generator (:verdict result)))
    (is (false? (get-in result [:diversity :diverse?])))
    (is (nil? (:winner result)))))

(deftest generated-buildouts-report-diversity-on-realistic-moves
  (let [moves [(mv "cap" :advance-capability "root" "cap" 2.0 -0.5)
               (mv "hole" :close-hole "root" "hole" 2.0 -0.8)
               (assoc (mv "terminal" :centre-mess "root" "terminal" 1.5 -0.3)
                      :move/terminal? true)
               (mv "graft" :graft-pattern "root" "graft" 1.8 -0.4)]
        buildouts (aw/generate-buildouts fixture-state moves :n 3 :depth 1 :top-k 4)
        diversity (aw/diversity-report buildouts :max-overlap 0.95)]
    (is (= 3 (count buildouts)))
    (is (:diverse? diversity))
    (is (true? (:disagreeing-implied-moves? diversity)))))

(deftest experiment-runner-uses-realized-g-floor-shape
  (let [moves [(mv "cap" :advance-capability "scope/capability/a" "scope/capability/b" 2.0 -0.5)
               (mv "mission" :close-hole "futon2-d/mission/a" "a/map" 1.0 -0.2)
               (mv "proxy" :close-hole "scope/interim-director-proxy-metric-inventory/pattern#open" "closed" 1.0 -0.2)]
        result (aw/experiment-runner moves)]
    (is (= :realized-G (:yardstick result)))
    (is (= :escrowed (:peradam-grounding result)))
    (is (= 2 (count (:results result))))
    (is (every? #(contains? % :verdict) (:results result)))))
