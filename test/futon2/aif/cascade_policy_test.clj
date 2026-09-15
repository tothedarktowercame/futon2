(ns futon2.aif.cascade-policy-test
  "Ruled organiser conformance and Snatch parity; legacy scoring controls."
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-policy :as cp]))

;; The Lean sameBag pair: equal node-bags, differing only in composition
;; (precedence). GOverCascades.sameBagFirst / sameBagSecond.
(def same-bag-first
  {:nodes #{:a :b} :added-by-organise #{} :edges #{}
   :precedence [:a :b] :acyclic? true})
(def same-bag-second
  {:nodes #{:a :b} :added-by-organise #{} :edges #{}
   :precedence [:b :a] :acyclic? true})

(deftest composition-blind-cannot-separate
  ;; compositionBlind_cannot_separate: a score factoring through :nodes
  ;; gives the pair equal values.
  (let [blind (fn [c] (count (:nodes c)))]
    (is (cp/composition-blind? blind [same-bag-first same-bag-second]))
    (is (== (blind same-bag-first) (blind same-bag-second)))))

(deftest cascade-grain-separates-same-bag
  ;; cascadeGrain_separates_sameBag: a risk leg reading the composition
  ;; induces distinct G on the equal bag.
  (let [risk (fn [c] (if (= (:precedence c) [:a :b]) 1.0 0.0))
        eig (constantly 0.0)
        g1 (cp/cascade-grain-G risk eig same-bag-first)
        g2 (cp/cascade-grain-G risk eig same-bag-second)]
    (is (not (cp/composition-blind? #(:value (cp/cascade-grain-G risk eig %))
                                    [same-bag-first same-bag-second])))
    (is (not= (:value g1) (:value g2)))))

(def repo {:patterns #{:a :b :c :d} :stands-on #{[:a :b] [:b :c]} :acyclic? true})
(def authority {:authority :documented-interpretation :source "fixture:admission"})
(defn fixture-opts [temperament]
  {:temperament temperament :acting-order-fn (fn [c] (:precedence c))
   :score-fn (constantly 0)})
(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))
(defn organise-fixture [temperament selected repository admitted]
  (cp/organise cp/first-attempt-cascade selected repository admitted (fixture-opts temperament)))

(deftest organiser-carriers-and-no-bootstrap
  (let [c (organise-fixture cp/up-closure-temperament #{:a} repo {:d authority})]
    (is (= #{:a :b :c :d} (:nodes c)))
    (is (= #{:b :c} (:added-by-organise c)))
    (is (= #{:d} (:admitted-by c)))
    (is (= {:d authority} (get-in c [:provenance :admissions])))
    (is (= #{} (:organised-edges c))))
  (let [c (organise-fixture cp/selected-only-temperament #{:a :c} repo {})]
    (is (= #{[:a :c]} (:organised-edges c)))
    (is (= #{} (:added-by-organise c)))))

(defn subsets [xs]
  (reduce (fn [acc x] (into acc (map #(conj % x) acc))) [#{}] xs))
(defn paths
  "Independent exhaustive DAG path enumeration, used as the test oracle."
  [edges u v]
  (letfn [(walk [path]
            (let [last-node (peek path)]
              (if (and (> (count path) 1) (= last-node v)) [path]
                  (mapcat #(walk (conj path %))
                          (for [[a b] edges :when (and (= a last-node) (not (some #{b} path)))] b)))))]
    (walk [u])))

(deftest seven-laws-on-all-three-node-dags-and-carriers
  ;; 8 DAGs x 8 selected sets x 8 admitted sets x 2 temperaments = 1024.
  (doseq [edges (subsets [[0 1] [0 2] [1 2]])
          sel (subsets [0 1 2]) adm (subsets [0 1 2])
          t [cp/selected-only-temperament cp/up-closure-temperament]]
    (let [r {:patterns #{0 1 2} :stands-on edges}
          attributed (zipmap adm (repeat authority))
          before {:nodes sel :precedence [0 1 2]}
          t (assoc t :precedence [2 1 0])
          d (cp/organise before sel r attributed (fixture-opts t))
          carrier (set/difference (:nodes d) (:added-by-organise d))
          expected (set (for [u carrier v carrier
                             :when (some #(not-any? carrier (butlast (rest %))) (paths edges u v))] [u v]))]
      (is (= sel (:selected d)) "osel")
      (is (= edges (:authored-edges d)) "oauth")
      (is (= adm (:admitted-by d)) "oattr")
      (is (= (:nodes d) (set/union sel (:added-by-organise d) adm)) "o1")
      (is (every? (fn [[u v]] (seq (paths edges u v))) (:organised-edges d)) "o2")
      (is (= expected (:organised-edges d)) "o3, independent path oracle")
      (is (not= (:acting-order-before d) (:acting-order-after d)) "o4, moved precedence")
      (is (= d (cp/validate-cascade-diff! before sel r attributed d))))))

(deftest each-law-has-a-specific-refusal
  (let [before cp/first-attempt-cascade sel #{:a :c} adm {:d authority}
        d (cp/organise before sel repo adm (fixture-opts cp/selected-only-temperament))]
    (doseq [[law mutate] [[:osel #(assoc % :selected #{})]
                          [:oauth #(assoc % :authored-edges #{})]
                          [:oattr #(assoc % :admitted-by #{})]
                          [:o1 #(assoc % :nodes #{:a})]
                          [:o2 #(assoc % :organised-edges #{[:c :a]})]
                          [:o3 #(assoc % :organised-edges #{})]
                          [:o4 #(assoc % :precedence-after [:a])]]]
      (is (= law (:law (refusal #(cp/validate-cascade-diff! before sel repo adm (mutate d))))))))
  (let [d (organise-fixture cp/up-closure-temperament #{:a} repo {})]
    ;; b was added; its reachable edge b->c cannot justify itself under O3.
    (is (= :o3 (:law (refusal #(cp/validate-cascade-diff!
                               cp/first-attempt-cascade #{:a} repo {}
                               (assoc d :organised-edges #{[:b :c]}))))))))

(deftest repository-admissions-and-explicit-ports
  (doseq [[reason f] [[:selected-outside-repository #(organise-fixture cp/up-closure-temperament #{:z} repo {})]
                      [:admitted-outside-repository #(organise-fixture cp/up-closure-temperament #{:a} repo {:z authority})]
                      [:admission-authority-required #(organise-fixture cp/up-closure-temperament #{:a} repo {:c {}})]
                      [:cyclic-stands-on #(organise-fixture cp/up-closure-temperament #{} (update repo :stands-on conj [:c :a]) {})]
                      [:cyclic-stands-on #(organise-fixture cp/up-closure-temperament #{} (update repo :stands-on conj [:a :a]) {})]
                      [:previous-cascade-required #(cp/organise nil #{:a} repo {} (fixture-opts cp/selected-only-temperament))]
                      [:observation-ports-required #(cp/organise cp/first-attempt-cascade #{:a} repo {}
                                                     {:temperament (assoc cp/selected-only-temperament :precedence [:a])})]
                      [:precedence-change-without-consequence #(cp/organise cp/first-attempt-cascade #{:a} repo {}
                                                                {:temperament (assoc cp/selected-only-temperament :precedence [:a])
                                                                 :acting-order-fn (constantly []) :score-fn (constantly 0)})]]]
    (is (= reason (:reason (refusal f)))))
  (is (map? (organise-fixture cp/selected-only-temperament #{:a} (assoc repo :acyclic? false) {})))
  (let [calls (atom []) before {:nodes #{:a} :precedence [:a] :evidence :previous}
        d (cp/organise before #{:a} repo {}
                       {:temperament (assoc cp/selected-only-temperament :precedence [])
                        :acting-order-fn (fn [c] (swap! calls conj c) [])
                        :score-fn (fn [c] (count (:precedence c)))})]
    (is (= before (first @calls)))
    (is (= [1 0] [(:score-before d) (:score-after d)]))
    (is (= [] (:acting-order-before d) (:acting-order-after d)) "score-only O4 arm")))

(deftest snatch-ruled-parity
  ;; Exact F12SnatchExemplar.lean:38-122 data; observations are recorded fixture
  ;; ports, not a claimed implementation of Snatch acting semantics.
  (let [r {:patterns (set (range 24))
           :stands-on #{[0 2] [0 11] [1 7] [2 7] [2 11] [3 9] [4 8] [5 18] [6 18]
                        [7 18] [7 22] [8 18] [8 22] [9 18] [9 22] [10 18] [11 18] [11 22]
                        [12 20] [13 20] [14 21] [15 21] [16 21] [17 21] [18 22] [19 21]}}
        before {:nodes #{0 2 10} :precedence [5 2 0 3 6 10 20]}
        after-order [20 5 2 0 3 6 10]
        recorded {(:precedence before) {:acting [10 2 0] :score 3}
                  after-order {:acting [20] :score -5}}
        d (cp/organise before #{0 2 10} r {}
                       {:temperament (assoc cp/up-closure-temperament :precedence after-order)
                        :acting-order-fn #(get-in recorded [(:precedence %) :acting])
                        :score-fn #(get-in recorded [(:precedence %) :score])})]
    (is (= #{0 2 10} (:selected d)))
    (is (= #{7 11 18 22} (:added-by-organise d)))
    (is (= #{} (:admitted-by d)))
    (is (= #{0 2 10 7 11 18 22} (:nodes d)))
    (is (= (:stands-on r) (:authored-edges d)))
    (is (= #{[0 2]} (:organised-edges d)))
    (is (= (:precedence before) (:precedence-before d)))
    (is (= after-order (:precedence-after d)))
    (is (= [10 2 0] (:acting-order-before d)))
    (is (= [20] (:acting-order-after d)))
    (is (= [3 -5] [(:score-before d) (:score-after d)]))))

(deftest posterior-is-a-distribution-over-cascades
  ;; softmaxWithFPi mirror: normalized, and at zero F_π lower G wins under
  ;; a flat habit.
  (let [risk (fn [c] (if (= (:precedence c) [:a :b]) 1.0 0.0))
        {:keys [posterior selected]}
        (cp/select-over-cascades (constantly 1.0) risk (constantly 0.0)
                                 (constantly 0.0) 1.0
                                 [same-bag-first same-bag-second])]
    (is (< (Math/abs (- 1.0 (reduce + (map :weight posterior)))) 1e-9))
    (is (= same-bag-second (:cascade selected)))
    (testing "the record carries cascade-grain G, not a flat score"
      (is (every? #(contains? % :G) posterior))
      (is (every? #(map? (:cascade %)) posterior)))))

(deftest habit-can-govern-against-G
  ;; The glossary's measured case (rank109): the learned prior can overrule
  ;; the G ranking — the seam must preserve that at cascade grain.
  (let [risk (fn [c] (if (= (:precedence c) [:a :b]) 1.0 0.0))
        habit (fn [c] (if (= (:precedence c) [:a :b]) 100.0 1.0))
        {:keys [selected]}
        (cp/select-over-cascades habit risk (constantly 0.0)
                                 (constantly 0.0) 1.0
                                 [same-bag-first same-bag-second])]
    (is (= same-bag-first (:cascade selected)))))
