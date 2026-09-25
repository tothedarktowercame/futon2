(ns futon2.aif.construction-test
  "H7c-1 requirement tests (SPEC-flat-removal-and-cascade-decision H7):
  construction scoring, the hand-over-when-acting-is-worth-more stopping
  rule, and the construction receipt. Moves are injected fixtures on
  tick-1-like tokens; H7c-2 implements the library's real moves."
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.locator-fixtures :as locfix]
            [futon2.aif.construction :as construction]))

(defn- assemble*
  "problems/assemble with every token given a fixture C3 locator (P5 locator
  requirement); tests about locators call problems/assemble directly."
  [m]
  (problems/assemble (update m :sources locfix/locate-all)))

(def base-input
  {:target :mission/test
   :horizon 2
   :budget {:max-moves 10}})

;; ---------------------------------------------------------------- (a)
(deftest improving-move-taken-then-hand-over
  (testing "a move that lowers G is taken; the next move is worth <= 0, so
            construction stops with :acting-worth-more"
    (let [g (fn [c] {:value (get {:base 3.0 :improved 2.0} (:id c) 3.0)
                     :universe [:a]})
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
      (is (= 2.0 (:value (g (apply min-key (comp :value g) family)))))
      (is (true? (:stopped-is-not-success receipt)))
      (is (= 2 (:horizon receipt))))))

;; --------------------------------------- W6/X6: G comparisons carry universes
;; H-VALUE-G-D §7: a G value is meaningful only with the universe it was
;; normalised over. The receipt must refuse to record a numeric comparison
;; of G values taken over different (or unrecorded) universes.
(deftest g-comparison-over-different-universes-is-incommensurable
  (testing "two values over different universes: :incommensurable, no
            improvement number, and no :acting-worth-more verdict"
    (let [g (fn [c] (if (= :base (:id c))
                      {:value 3.0 :universe [:s0 :t1 :t4]}
                      {:value 2.0 :universe [:s0 :t1 :t2 :t3 :t4]}))
          move (fn [_] {:move-id :improve
                        :proposed-family [{:id :improved :precedence [:a]}]
                        :cost 0.0})
          receipt (:receipt
                   (construction/construct
                    (assoc base-input
                           :initial-family [{:id :base :precedence [:a]}]
                           :moves [move] :evaluate-g g)))]
      (is (= :g-universes-incommensurable (:stop-reason receipt)))
      (is (not= :acting-worth-more (:stop-reason receipt)))
      (is (= [] (:moves receipt)) "no improvement was ever claimed")
      (is (= {:incommensurable {:universes [[:s0 :t1 :t4] [:s0 :t1 :t2 :t3 :t4]]}}
             (get-in receipt [:coverage :final-evaluation :improve])))
      (is (= {:value 3.0 :universe [:s0 :t1 :t4]} (:g-of-best receipt))))))

(deftest g-comparison-over-one-universe-compares-as-today
  (testing "two values over the same universe compare numerically, with the
            universe recorded beside the delta"
    (let [g (fn [c] {:value (get {:base 3.0 :improved 2.0} (:id c) 3.0)
                     ;; unsorted on purpose: the receipt records it sorted
                     :universe [:t4 :s0]})
          move (fn [_] {:move-id :improve
                        :proposed-family [{:id :improved :precedence [:a]}]
                        :cost 0.25})
          receipt (:receipt
                   (construction/construct
                    (assoc base-input
                           :initial-family [{:id :base :precedence [:a]}]
                           :moves [move] :evaluate-g g)))]
      (is (= {:delta 1.0 :universe [:s0 :t4]}
             (get-in receipt [:moves 0 :g-comparison])))
      (is (= 1.0 (get-in receipt [:moves 0 :parts :pragmatic])))
      (is (= 0.75 (get-in receipt [:moves 0 :value])))
      ;; :final-evaluation re-evaluates the move against the improved
      ;; family, where the same proposal is worth -0.25
      (is (= -0.25 (get-in receipt [:coverage :final-evaluation :improve])))
      (is (= {:value 2.0 :universe [:s0 :t4]} (:g-of-best receipt))))))

(deftest legacy-g-without-universe-is-incommensurable
  (testing "a bare-number (legacy) G carries the typed absence :universe
            :not-recorded, and a comparison against it is :incommensurable —
            not silently allowed"
    (let [g (fn [c] (get {:base 3.0 :improved 2.0} (:id c) 3.0))
          move (fn [_] {:move-id :improve
                        :proposed-family [{:id :improved :precedence [:a]}]
                        :cost 0.0})
          receipt (:receipt
                   (construction/construct
                    (assoc base-input
                           :initial-family [{:id :base :precedence [:a]}]
                           :moves [move] :evaluate-g g)))]
      (is (= :g-universes-incommensurable (:stop-reason receipt)))
      (is (= :not-recorded (get-in receipt [:g-of-best :universe])))
      (is (= 3.0 (get-in receipt [:g-of-best :value])))
      (is (= {:incommensurable {:universes [:not-recorded :not-recorded]}}
             (get-in receipt [:coverage :final-evaluation :improve]))))))

;; ---------------------------------------------------------------- (b)
(deftest budget-one-exhausts-after-one-move
  (testing "budget 1 gives :budget-exhausted after one move"
    (let [g (fn [c] {:value (:g c) :universe [:a]})
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
    (let [g (fn [c] {:value (get {:base 3.0 :improved 2.8} (:id c) 3.0)
                     :universe [:a]})
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
          assembled (assemble*
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
             (mapv :construction-receipt (get-in assembled [:problems 0 :constructed-candidates])))))))

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

;; ------------------------------------------------------- clause 0: containment order
;; PROOF-2a lines 128-212: the constructor emits a containment order r over
;; units, not only a precedence list. Fixtures below take the shape of the
;; hand cascades (futon3c holes/labs/M-futon-seams/proto/instance-*.edn):
;; patterns carrying :guard {:needs :forbids} and :produces.
(deftest containment-order-chain-is-the-precedence-case
  (testing "bad case (3): a chain cascade yields an :order whose only
            linear extension is the existing :precedence"
    (let [chain {:precedence [:p1 :p2 :p3]
                 :patterns [{:id :p1 :guard {:needs #{} :forbids #{}} :produces #{:t1}}
                            {:id :p2 :guard {:needs #{:t1} :forbids #{}} :produces #{:t2}}
                            {:id :p3 :guard {:needs #{:t2} :forbids #{}} :produces #{:t3}}]}
          order (construction/containment-order chain)
          idx (into {} (map-indexed (fn [i u] [u i]) (:precedence chain)))
          below (fn [u] ;; reflexive descendants under :descent
                  (loop [seen #{u} frontier [u]]
                    (let [nxt (set (mapcat (fn [x] (keep #(when (= x (first %)) (second %))
                                                         (:descent order)))
                                           frontier))]
                      (if (set/subset? nxt seen) seen (recur (into seen nxt) nxt)))))]
      (is (= [[:p1 :p2] [:p2 :p3]] (:descent order)))
      (is (= [{:unit :p1 :pattern :p1} {:unit :p2 :pattern :p2} {:unit :p3 :pattern :p3}]
             (:units order)))
      ;; every pair overlaps and has its meet; nothing is missing
      (is (empty? (:missing-meets order)))
      (is (= {[:p1 :p2] :p2 [:p1 :p3] :p3 [:p2 :p3] :p3} (:meets order)))
      ;; r is total (a chain) and precedence respects every edge, so
      ;; precedence is the ONLY linear extension of r
      (is (every? (fn [[a b]] (or (contains? (below a) b) (contains? (below b) a)))
                  (for [a [:p1 :p2 :p3] b [:p1 :p2 :p3] :when (not= a b)] [a b])))
      (is (every? (fn [[a b]] (< (idx a) (idx b))) (:descent order))))))

(deftest containment-order-cyclic-containment-refused
  (testing "bad case (2): a cyclic containment is refused with a typed
            reason naming the cycle — no order exists to record"
    (let [cyclic {:precedence [:a :b]
                  :patterns [{:id :a :guard {:needs #{:y} :forbids #{}} :produces #{:x}}
                             {:id :b :guard {:needs #{:x} :forbids #{}} :produces #{:y}}]}
          order (construction/containment-order cyclic)]
      (is (= :refused (:status order)))
      (is (= :cyclic-containment (:kind order)))
      (is (= [:a :b :a] (:cycle order))))))

(deftest containment-order-disjoint-pairs-need-no-meet
  (testing "clause 0 restricts the semilattice condition to OVERLAPPING
            pairs: two patterns sharing no descendant record nothing"
    (let [disjoint {:precedence [:a :b]
                    :patterns [{:id :a :guard {:needs #{} :forbids #{}} :produces #{:x}}
                               {:id :b :guard {:needs #{} :forbids #{}} :produces #{:y}}]}
          order (construction/containment-order disjoint)]
      (is (= [] (:descent order)))
      (is (= {} (:meets order)))
      (is (= [] (:missing-meets order))))))

(deftest receipt-records-locators-and-unlocated-tokens
  ;; claude-4, WM-04: the receipt names the tokens with no checkable locator,
  ;; and records its locator coverage honestly when no token set is supplied.
  (let [base {:target :mission/loc
              :initial-family [{:precedence []}]
              :moves []
              :evaluate-g (constantly 1.0)
              :budget {:max-moves 0}
              :horizon 2}
        with-tokens (assoc base
                           :facts {:f true}
                           :want [:w]
                           :patterns [{:id :p :guard {:needs #{:f} :forbids #{}} :produces #{:w}}]
                           :check-theta 1
                           :locators {:f {:class :C3 :repo "futon2" :sha "x" :path "a"}
                                      :w {:class :J}})
        r (:receipt (construction/construct with-tokens))]
    (is (= #{:f :w} (set (keys (:locators r))))
        "supplied locators for problem tokens are carried, whatever their class")
    (is (= [:w] (:unlocated-tokens r)))
    (is (= :token-set-not-supplied
           (:locator-coverage (:receipt (construction/construct base)))))))
