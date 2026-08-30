(ns futon2.aif.hierarchical-budget-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.hierarchical-budget :as budget]
            [futon2.aif.policy :as policy]))

(def contested-hierarchy
  {:id :root
   :budget 9
   :children
   [{:id :agent-a
     :budget 6
     :proposals [{:id :a-expensive :cost 6 :utility 10}
                 {:id :a-compact :cost 3 :utility 7}]}
    {:id :agent-b
     :budget 6
     :proposals [{:id :b-expensive :cost 6 :utility 9}
                 {:id :b-compact :cost 3 :utility 6}]}]})

(deftest independently-feasible-local-winners-cannot-oversubscribe-parent
  (testing "the two local winners cost 12, but the root has only 9"
    (let [out (budget/arbitrate contested-hierarchy)]
      (is (= #{:root :agent-a :agent-b} (:oversubscribed-nodes out)))
      (is (= 9.0 (:total-cost out)))
      (is (= 16.0 (:total-utility out)))
      (is (= #{:a-compact :b-expensive} (:selected-ids out))
          "lexical tie-break chooses deterministically between equal-value portfolios")
      (is (= {:root 9.0 :agent-a 3.0 :agent-b 6.0}
             (:node-usage out)))
      (is (:within-all-budgets? out)))))

(deftest parent-retains-cheaper-child-frontier-instead-of-greedy-local-winner
  (let [hierarchy
        {:id :root :budget 6
         :children
         [{:id :left :budget 6
           :proposals [{:id :left-best :cost 6 :utility 10}
                       {:id :left-small :cost 2 :utility 8}]}
          {:id :right :budget 4
           :proposals [{:id :right-only :cost 4 :utility 9}]}]}
        out (budget/arbitrate hierarchy)]
    (is (= #{:left-small :right-only} (:selected-ids out)))
    (is (= 17.0 (:total-utility out))
        "global optimum needs the locally second-best left proposal")
    (is (= 6.0 (:total-cost out)))))

(deftest every-level-of-a-deep-hierarchy-is-charged
  (let [hierarchy
        {:id :root :budget 8
         :children
         [{:id :team :budget 5
           :children
           [{:id :worker-a :budget 4
             :proposals [{:id :a :cost 4 :utility 9}]}
            {:id :worker-b :budget 4
             :proposals [{:id :b :cost 4 :utility 8}
                         {:id :b-small :cost 1 :utility 4}]}]}
          {:id :worker-c :budget 3
           :proposals [{:id :c :cost 3 :utility 7}]}]}
        out (budget/arbitrate hierarchy)]
    (is (= #{:a :b-small :c} (:selected-ids out)))
    (is (= {:root 8.0 :team 5.0 :worker-a 4.0
            :worker-b 1.0 :worker-c 3.0}
           (:node-usage out)))
    (is (every? (fn [[node used]]
                  (<= used (get (:node-budgets out) node)))
                (:node-usage out)))))

(deftest empty-and-negative-utility-options-do-not-force-spending
  (let [out (budget/arbitrate
             {:id :root :budget 10
              :proposals [{:id :harmful :cost 1 :utility -1}]})]
    (is (empty? (:selected out)))
    (is (zero? (:total-cost out)))
    (is (zero? (:total-utility out)))))

(deftest ranked-alternative-leaf-selects-at-most-one-proposal
  (let [out (budget/arbitrate
             {:id :root :budget 10 :selection-limit 1
              :proposals [{:id :best :cost 2 :utility 5}
                          {:id :also-positive :cost 2 :utility 4}]})]
    (is (= #{:best} (:selected-ids out)))
    (is (= {:root 1} (:node-selection-limits out)))))

(deftest policy-exposes-the-r11-selection-boundary
  (let [out (policy/select-budgeted-actions contested-hierarchy)]
    (is (= :hierarchical-shared-budget (:selection-boundary out)))
    (is (= :exact-pareto-frontier-composition
           (get-in out [:proof :method])))
    (is (:within-all-budgets? out))))

(deftest replay-is-independent-of-child-and-proposal-order
  (let [reordered (update contested-hierarchy :children
                          (fn [children]
                            (mapv #(update % :proposals (comp vec reverse))
                                  (reverse children))))
        original (budget/arbitrate contested-hierarchy)
        replay (budget/arbitrate reordered)]
    (is (= (:selected-ids original) (:selected-ids replay)))
    (is (= (:node-usage original) (:node-usage replay)))
    (is (= (:total-utility original) (:total-utility replay)))))

(deftest malformed-fields-fail-closed
  (testing "negative cost is rejected"
    (let [failure (try
                    (budget/arbitrate
                     {:id :root :budget 1
                      :proposals [{:id :bad :cost -1 :utility 3}]})
                    nil
                    (catch clojure.lang.ExceptionInfo e e))]
      (is (= :invalid-budget-hierarchy
             (:failure-kind (ex-data failure))))))
  (testing "duplicate proposal ids are rejected"
    (let [failure (try
                    (budget/arbitrate
                     {:id :root :budget 2
                      :children
                      [{:id :a :budget 1
                        :proposals [{:id :same :cost 1 :utility 1}]}
                       {:id :b :budget 1
                        :proposals [{:id :same :cost 1 :utility 2}]}]})
                    nil
                    (catch clojure.lang.ExceptionInfo e e))]
      (is (= :proposal (:kind (ex-data failure))))
      (is (= :same (:duplicate-id (ex-data failure)))))))
