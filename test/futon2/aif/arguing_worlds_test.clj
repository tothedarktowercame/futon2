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

(defn- bo
  [id policy c]
  {:buildout/id id
   :policy policy
   :pattern-set (aw/buildout-pattern-set policy)
   :implied-moves (mapv :move/id policy)
   :C c})

(deftest assemble-circumstances-from-freeze-is-deterministic
  (let [freeze {:ranked-actions (mapv (fn [rank]
                                        {:type "advance-mission"
                                         :target (str "M-" rank)
                                         :weight 1.0
                                         :open-hole-count (inc (mod rank 3))
                                         :rank rank})
                                      (range 1 13))}
        a (aw/assemble-circumstances freeze {:n 2 :budget 4})
        b (aw/assemble-circumstances freeze {:n 2 :budget 4})]
    (is (= a b))
    (is (= 2 (count a)))
    (is (every? #(= 4 (count (:moves %))) a))
    (is (every? #(contains? % :psi) a))))

(deftest referee-field-harness-picks-best-of-four-samplers
  (let [a (bo :a [(mv "a" :close-hole "root" "a" 1 -0.1)] 10)
        b (bo :b [(mv "b" :close-hole "root" "b" 1 -0.6)] 8)
        c (bo :c [(mv "c" :close-hole "root" "c" 1 -2.0)] 1)
        d (bo :d [(mv "d" :close-hole "root" "d" 1 -0.3)] 12)
        result (aw/referee-field-harness fixture-state
                                         [{:sampler/id :s-a :buildouts [a] :wall-clock-ms 4}
                                          {:sampler/id :s-b :buildouts [b] :wall-clock-ms 3}
                                          {:sampler/id :s-c :buildouts [c] :wall-clock-ms 2}
                                          {:sampler/id :s-d :buildouts [d] :wall-clock-ms 1}]
                                         :diversity-opts {:max-overlap 1.0})]
    (is (= :sampler-wins (:verdict result)))
    (is (= :s-c (-> result :winner :sampler/id)))
    (is (= :realized-G (-> result :winner :yardstick)))))

(deftest referee-field-harness-tie-breaks-by-moves-then-wall-clock-then-tie
  (let [one-move (bo :one [(mv "one" :close-hole "root" "one" 1 -1.0)] 1)
        two-move (bo :two [(mv "two-a" :close-hole "root" "two-a" 1 -0.5)
                           (mv "two-b" :close-hole "two-a" "two-b" 1 -0.5)] 2)
        fewer (aw/referee-field-harness fixture-state
                                        [{:sampler/id :one :buildouts [one-move] :wall-clock-ms 10}
                                         {:sampler/id :two :buildouts [two-move] :wall-clock-ms 1}]
                                        :diversity-opts {:max-overlap 0.4})
        slow (bo :slow [(mv "slow" :close-hole "root" "slow" 1 -1.0)] 1)
        fast (bo :fast [(mv "fast" :close-hole "root" "fast" 1 -1.0)] 1)
        wall (aw/referee-field-harness fixture-state
                                      [{:sampler/id :slow :buildouts [slow] :wall-clock-ms 9}
                                       {:sampler/id :fast :buildouts [fast] :wall-clock-ms 2}]
                                      :diversity-opts {:max-overlap 1.0})
        tie-a (bo :tie-a [(mv "tie-a" :close-hole "root" "tie-a" 1 -1.0)] 1)
        tie-b (bo :tie-b [(mv "tie-b" :close-hole "root" "tie-b" 1 -1.0)] 1)
        tied (aw/referee-field-harness fixture-state
                                      [{:sampler/id :tie-a :buildouts [tie-a] :wall-clock-ms 5}
                                       {:sampler/id :tie-b :buildouts [tie-b] :wall-clock-ms 5}]
                                      :diversity-opts {:max-overlap 1.0})]
    (is (= :one (-> fewer :winner :sampler/id)))
    (is (= :fast (-> wall :winner :sampler/id)))
    (is (= :tie (:verdict tied)))
    (is (= #{:tie-a :tie-b} (set (map :sampler/id (:ties tied)))))))

(deftest referee-field-harness-culls-non-diverse-and-reports-partials
  (let [same (bo :same [(mv "same" :close-hole "root" "same" 1 -1.0)] 1)
        monotone (aw/referee-field-harness fixture-state
                                          [{:sampler/id :a :buildouts [same]}
                                           {:sampler/id :b :buildouts [(assoc same :buildout/id :same-2)]}])
        good (bo :good [(mv "good" :close-hole "root" "good" 1 -1.0)] 1)
        other (bo :other [(mv "other" :graft-pattern "root" "other" 1 -0.2)] 1)
        partial (aw/referee-field-harness fixture-state
                                         [{:sampler/id :empty :buildouts []}
                                          {:sampler/id :good :buildouts [good]}
                                          {:sampler/id :other :buildouts [other]}]
                                         :diversity-opts {:max-overlap 1.0})]
    (is (= :monotone-generator (:verdict monotone)))
    (is (= :sampler-wins (:verdict partial)))
    (is (= [{:sampler/id :empty :status :partial :reason :zero-valid-buildouts}]
           (:partials partial)))))

(deftest sampler-entrants-emit-open-protocol-shape
  (let [circ {:circumstance/id :sampler-shape
              :state fixture-state
              :moves [(mv "m1" :close-hole "root" "m1" 3 -0.5)
                      (mv "m2" :advance-capability "root" "m2" 2 -0.2)
                      (mv "m3" :graft-pattern "root" "m3" 1 -0.1)
                      (mv "m4" :centre-mess "root" "m4" 1 -0.1)
                      (mv "m5" :close-hole "root" "m5" 1 -0.1)
                      (mv "m6" :close-hole "root" "m6" 1 -0.1)
                      (mv "m7" :graft-pattern "root" "m7" 1 -0.1)]}
        entries (concat (aw/incumbent-sampler circ)
                        (aw/greedy-eps-sampler circ)
                        (aw/random-under-budget-sampler circ))]
    (is (= 3 (count entries)))
    (is (every? #(and (:buildout/id %)
                      (seq (:policy %))
                      (:pattern-set %)
                      (:implied-moves %)
                      (number? (:C %)))
                entries))))
