(ns futon2.report.cascade-lane-policy-menu-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.report.cascade-lane :as lane]))

(defn- served-cascade
  [epsilon]
  (case epsilon
    0.10 {:size 3
          :shown [{:pattern "p/a"} {:pattern "p/b"} {:pattern "p/c"}]
          :semilattice {:descent [["p/a" "p/b"] ["p/b" "p/c"]]
                        :co_app []}
          :cascade-score 1.2
          :coverage-reward 3.0
          :prior-cost 1.8
          :wholeness 2.1
          :truncated false}
    0.15 {:size 2
          :shown [{:pattern "p/a"} {:pattern "p/b"}]
          :semilattice {:descent [["p/a" "p/b"]] :co_app []}
          :cascade-score 1.4
          :coverage-reward 2.6
          :prior-cost 1.2
          :wholeness 1.9
          :truncated false}
    0.20 {:size 2
          :shown [{:pattern "p/a"} {:pattern "p/b"}]
          :semilattice {:descent [["p/a" "p/b"]] :co_app []}
          :cascade-score 1.4
          :coverage-reward 2.6
          :prior-cost 1.2
          :wholeness 1.9
          :truncated false}))

(deftest dark-policy-menu-admits-complete-distinct-same-mission-cascades
  (let [calls (atom [])]
    (with-redefs [lane/cascade-policy-for
                  (fn [psi budget epsilon]
                    (swap! calls conj [psi budget epsilon])
                    (served-cascade epsilon))]
      (let [menu (lane/cascade-policy-menu-for "M-memory" "typed recall")]
        (is (= [["typed recall" 40 0.10]
                ["typed recall" 40 0.15]
                ["typed recall" 40 0.20]]
               @calls))
        (is (= 2 (:candidate-count menu)) "duplicate policies collapse")
        (is (:policy-choice? menu))
        (is (= :candidate-menu (:status menu)))
        (is (every? #(= "M-memory" (:mission %)) (:candidates menu)))
        (is (every? #(= :pattern-cascade (:policy-grain %))
                    (:candidates menu)))
        (is (= #{["p/a" "p/b"] ["p/a" "p/b" "p/c"]}
               (set (map :shown (:candidates menu)))))))))

(deftest truncated-or-singleton-frontier-is-not-a-policy-choice
  (with-redefs [lane/cascade-policy-for
                (fn [_ _ epsilon]
                  (if (= epsilon 0.10)
                    (assoc (served-cascade epsilon) :truncated true)
                    (served-cascade 0.15)))]
    (let [menu (lane/cascade-policy-menu-for
                "M-memory" "typed recall" {:epsilons [0.10 0.15]})]
      (testing "a score for the unshown suffix is never admitted"
        (is (= 1 (:candidate-count menu)))
        (is (false? (:policy-choice? menu)))
        (is (= :no-policy-choice (:status menu)))))))
