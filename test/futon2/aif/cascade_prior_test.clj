(ns futon2.aif.cascade-prior-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-prior :as prior]))

(def cascade-a
  {:mission "M-memory"
   :shown ["memory/typed-record" "memory/pattern-conditioned-recall"]
   :semilattice {:descent [["memory/typed-record"
                            "memory/pattern-conditioned-recall"]]
                 :co_app []}
   :cascade-score 3.1})

(def cascade-b
  {:mission "M-memory"
   :shown ["memory/pattern-conditioned-recall" "memory/typed-record"]
   :semilattice {:descent [["memory/pattern-conditioned-recall"
                            "memory/typed-record"]]
                 :co_app []}
   :cascade-score 2.7})

(deftest cascade-policy-identity-is-stable-and-topology-sensitive
  (testing "volatile engineering fields do not define a policy"
    (is (= (prior/policy-key cascade-a)
           (prior/policy-key (assoc cascade-a :cascade-score -99
                                               :psi "changed prose"
                                               :rank 8)))))
  (testing "edge enumeration order does not define semilattice identity"
    (let [edge-1 ["memory/typed-record" "memory/review" 3]
          edge-2 ["memory/review" "memory/typed-record" 2]
          left (assoc-in cascade-a [:semilattice :co_app] [edge-1 edge-2])
          right (assoc-in cascade-a [:semilattice :co_app] [edge-2 edge-1])]
      (is (= (prior/policy-key left) (prior/policy-key right)))))
  (testing "construction order and wiring do define the complete policy"
    (is (not= (prior/policy-key cascade-a) (prior/policy-key cascade-b)))
    (is (not= (prior/policy-key cascade-a)
              (prior/policy-key
               (assoc cascade-a :semilattice
                      {:descent [["memory/pattern-conditioned-recall"
                                  "memory/typed-record"]]
                       :co_app []}))))))

(deftest cold-start-is-uniform-and-observation-learns-frequency
  (let [cold (prior/initial-state)
        cold-log-e (prior/log-priors cold [cascade-a cascade-b])
        learned (reduce prior/observe-policy cold (repeat 3 cascade-b))
        [ln-a ln-b] (prior/log-priors learned [cascade-a cascade-b])]
    (is (< (Math/abs (- 1.0 (reduce + (map #(Math/exp %) cold-log-e))))
           1.0e-12))
    (is (< (Math/abs (- (first cold-log-e) (second cold-log-e))) 1.0e-12))
    (is (> ln-b ln-a))
    (is (= 3 (:samples learned)))
    (is (= :pattern-cascade (:policy-grain (prior/state-stats learned))))))

(deftest duplicate-policy-identities-split-category-mass
  (let [state (prior/observe-policy (prior/initial-state) cascade-a)
        probabilities (mapv #(Math/exp %)
                            (prior/log-priors state
                                              [cascade-a
                                               (assoc cascade-a :rank 2)
                                               cascade-b]))]
    (is (< (Math/abs (- 1.0 (reduce + probabilities))) 1.0e-12))
    (is (= (first probabilities) (second probabilities)))))

(deftest policy-level-boundaries-fail-closed
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"mixes strategic missions"
       (prior/log-priors (prior/initial-state)
                         [cascade-a (assoc cascade-b :mission "M-other")])))
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"no stable policy identity"
       (prior/log-priors (prior/initial-state)
                         [(dissoc cascade-a :semilattice)])))
  (is (= (prior/initial-state)
         (prior/observe-policy (prior/initial-state) {:mission "M-memory"}))))

(deftest dark-ranking-reuses-the-unscaled-prior-seam
  (let [cold (prior/shadow-rank (prior/initial-state)
                                [cascade-a cascade-b])
        learned-state (reduce prior/observe-policy
                              (prior/initial-state)
                              (repeat 20 cascade-b))
        habitual (prior/shadow-rank learned-state [cascade-a cascade-b])]
    (testing "a uniform cold start reduces to engineering-score ordering"
      (is (= (prior/policy-key cascade-a)
             (prior/policy-key (:shadow-winner cold))))
      (is (= :cascade-score (:governed-by cold))))
    (testing "habit is visible when it overturns the score-only winner"
      (is (= (prior/policy-key cascade-b)
             (prior/policy-key (:shadow-winner habitual))))
      (is (= (prior/policy-key cascade-a)
             (prior/policy-key (:score-only-winner habitual))))
      (is (= :cascade-habit (:governed-by habitual)))
      (is (< (Math/abs
              (- 1.0 (reduce + (map :cascade-selection-weight
                                    (:ranked-candidates habitual)))))
             1.0e-12)))))

(deftest dark-ranking-refuses-degenerate-or-unscored-menus
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"at least two"
       (prior/shadow-rank (prior/initial-state) [cascade-a])))
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"finite engineering score"
       (prior/shadow-rank (prior/initial-state)
                          [cascade-a (dissoc cascade-b :cascade-score)]))))
