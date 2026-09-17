(ns futon2.aif.construction-test
  "H7c-1 requirement tests (SPEC-flat-removal-and-cascade-decision H7):
  construction scoring, the hand-over-when-acting-is-worth-more stopping
  rule, and the construction receipt. Moves are injected fixtures on
  tick-1-like tokens; H7c-2 implements the library's real moves."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.construction :as construction]))

(def base-input
  {:target :mission/test
   :horizon 2
   :budget {:max-moves 10}})

;; ---------------------------------------------------------------- (a)
(deftest improving-move-taken-then-hand-over
  (testing "a move that lowers G is taken; the next move is worth <= 0, so
            construction stops with :acting-worth-more"
    (let [g (fn [c] (get {:base 3.0 :improved 2.0} (:id c) 3.0))
          ;; proposes the same improved family every time: worth +0.75 once
          ;; (3.0 -> 2.0, state-information recorded not added, cost 0.25),
          ;; then worth -0.25 against the improved current best
          move (fn [_family]
                 {:move-id :improve
                  :proposed-family [{:id :improved :precedence [:a]
                                     :construction-receipt {:stub true}}]
                  :epistemic-estimate {:value 0.5 :kind :state-information
                                       :basis "fixture"}
                  :cost 0.25})
          {:keys [family receipt]}
          (construction/construct
           (assoc base-input
                  :initial-family [{:id :base :precedence [:a]}]
                  :moves [move]
                  :evaluate-g g))]
      (is (= :acting-worth-more (:stop-reason receipt)))
      (is (= 1 (count (:moves receipt))))
      (is (= :improve (get-in receipt [:moves 0 :move-id])))
      ;; state information is recorded but NOT added a second time
      (is (= 0.0 (get-in receipt [:moves 0 :parts :epistemic-added])))
      (is (= :state-information
             (get-in receipt [:moves 0 :parts :epistemic :kind])))
      (is (not (get-in receipt [:moves 0 :parts
                                :includes-unformalised-novelty])))
      (is (= 2.0 (g (apply min-key g family))))
      (is (true? (:stopped-is-not-success receipt)))
      (is (= 2 (:horizon receipt))))))

;; ---------------------------------------------------------------- (b)
(deftest budget-one-exhausts-after-one-move
  (testing "budget 1 gives :budget-exhausted after one move"
    (let [g (fn [c] (:g c))
          ;; always proposes a strictly better family (g strictly
          ;; decreasing), so only the budget can stop it
          move (fn [family]
                 {:move-id :always-improve
                  :proposed-family (conj (vec family)
                                         {:id :extra
                                          :g (- (double (count family)))
                                          :precedence [:x]})
                  :cost 0.0})
          receipt (:receipt
                   (construction/construct
                    (assoc base-input
                           :budget {:max-moves 1}
                           :initial-family [{:id :base :g 5.0 :precedence [:a]}]
                           :moves [move]
                           :evaluate-g g)))]
      (is (= :budget-exhausted (:stop-reason receipt)))
      (is (= 1 (:budget-used receipt)))
      (is (= 1 (count (:moves receipt)))))))

;; ---------------------------------------------------------------- (c)
(deftest all-no-move-gives-no-admitted-move
  (testing "every move :no-move gives :no-admitted-move; every remaining
            move needing human input gives :needs-routed-human-input"
    (let [g (fn [_c] 1.0)
          no-move (fn [_] {:status :no-move :reason :nothing-left
                           :move-id :m})
          human (fn [_] {:status :no-move :reason :needs-human-input
                         :move-id :h})
          r1 (:receipt (construction/construct
                        (assoc base-input
                               :initial-family [{:id :base :precedence [:a]}]
                               :moves [no-move] :evaluate-g g)))
          r2 (:receipt (construction/construct
                        (assoc base-input
                               :initial-family [{:id :base :precedence [:a]}]
                               :moves [human] :evaluate-g g)))]
      (is (= :no-admitted-move (:stop-reason r1)))
      (is (= :needs-routed-human-input (:stop-reason r2)))
      (is (true? (:stopped-is-not-success r2))))))

;; ---------------------------------------------------------------- (d)
(deftest novelty-recorded-with-flag
  (testing "a novelty estimate is recorded as a typed estimate and marks the
            value :includes-unformalised-novelty"
    (let [g (fn [c] (get {:base 3.0 :improved 2.8} (:id c) 3.0))
          called? (atom false)
          move (fn [_]
                 ;; interpretation information is revealed once; the same
                 ;; review has no novelty left the second time
                 {:move-id :meaning-review
                  :proposed-family [{:id :improved :precedence [:a]}]
                  :epistemic-estimate
                  (if (compare-and-set! called? false true)
                    {:value 0.9 :kind :novelty
                     :basis "fixture: interpretation info"}
                    {:value 0.0 :kind :novelty
                     :basis "fixture: nothing left to reveal"})
                  :cost 0.1})
          receipt (:receipt
                   (construction/construct
                    (assoc base-input
                           :initial-family [{:id :base :precedence [:a]}]
                           :moves [move] :evaluate-g g)))]
      ;; value 0.2 + 0.9 - 0.1 = 1.0 > 0, so the move is taken and recorded
      (is (= 1 (count (:moves receipt))))
      (is (= :novelty (get-in receipt [:moves 0 :parts :epistemic :kind])))
      (is (true? (get-in receipt [:moves 0 :parts
                                  :includes-unformalised-novelty])))
      (is (= 0.9 (get-in receipt [:moves 0 :parts :epistemic-added]))))))

;; ---------------------------------------------------------------- (e)
(deftest gating-unknown-becomes-check-candidate-before-scoring
  (testing "a gating unknown fact adds its check candidate to the family
            before scoring (D3: unknown means find out, by acting)"
    (let [seen (atom [])
          g (fn [c] (swap! seen conj c)
                   (or (:g c) 1.0))
          ;; :gate/journal gates the only pattern reaching the want
          receipt (:receipt
                   (construction/construct
                    (assoc base-input
                           :initial-family [{:id :base :precedence [:p/open]}]
                           :moves [(fn [_] {:status :no-move
                                            :reason :nothing-left
                                            :move-id :m})]
                           :evaluate-g g
                           :facts {:gate/journal :unknown}
                           :want [:inbox/zero]
                           :patterns [{:id :p/open
                                       :guard {:needs #{:gate/journal}}
                                       :produces #{:inbox/zero}}]
                           :check-theta 0.9)))]
      (is (= :no-admitted-move (:stop-reason receipt)))
      (is (= 1 (count (:checks-added receipt))))
      (is (= :check/journal (get-in receipt [:checks-added 0 :id])))
      ;; every candidate the scorer saw carries the check, before any move
      (is (seq @seen))
      (is (every? #(some (fn [p] (= :check/journal (:id p)))
                         (:patterns %))
                  @seen)))))

;; ---------------------------------------------------------------- (f)
(deftest receipt-accepted-by-cascade-problems
  (testing "the receipt is accepted by cascade-problems/assemble as a
            :construction-receipt"
    (let [g (fn [_c] 1.0)
          {:keys [receipt]}
          (construction/construct
           (assoc base-input
                  :initial-family [{:id :base :precedence [:a]}]
                  :moves [(fn [_] {:status :no-move :reason :nothing-left
                                   :move-id :m})]
                  :evaluate-g g))
          assembled (problems/assemble
                     {:targets [:mission/test]
                      :sources {:universes {:mission/test {:f true}}
                                :interpretations
                                {:mission/test
                                 {:patterns {:a {:guard {:needs #{} :forbids #{}}
                                                 :produces #{}}}}}
                                :wants {:mission/test [:f]}
                                :candidates
                                {:mission/test
                                 [{:precedence [:a]
                                   :construction-receipt receipt}]}
                                :horizon-steps 2
                                :beta-by-context {:ctx {:beta 1.0}}
                                :context-of (constantly :ctx)}})]
      (is (= [] (:refusals assembled)))
      (is (= 1 (count (:problems assembled))))
      (is (= [receipt]
             (get-in assembled [:problems 0 :construction-receipts]))))))

;; ------------------------------------------------------- refusals/wants
(deftest typed-refusals-and-observed-want
  (testing "missing budget and horizon are typed refusals; an already
            observed want stops construction immediately"
    (is (= :budget-required (:kind (construction/construct
                                    (dissoc base-input :budget)))))
    (is (= :horizon-required (:kind (construction/construct
                                     (assoc base-input :horizon nil)))))
    (let [receipt (:receipt
                   (construction/construct
                    (assoc base-input
                           :initial-family [{:id :base :precedence [:a]}]
                           :moves [(fn [_] {:status :no-move :reason :x
                                            :move-id :m})]
                           :evaluate-g (fn [_] 1.0)
                           :want [:inbox/zero]
                           :q0 [:inbox/zero])))]
      (is (= :want-already-observed (:stop-reason receipt)))
      (is (zero? (:budget-used receipt))))))
