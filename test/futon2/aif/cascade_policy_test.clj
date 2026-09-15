(ns futon2.aif.cascade-policy-test
  "Mirrors of the GOverCascades.lean theorems plus F12 organise conformance
  cases. The Lean module is the specification; these tests keep the Clojure
  mirror honest against it."
  (:require [clojure.test :refer [deftest is testing]]
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

;; F12 organise conformance (mirror of F12Conformance O1-O3 on unit cases).
(def repo
  ;; a → b → c authored descent; d isolated
  {:patterns #{:a :b :c :d} :stands-on #{[:a :b] [:b :c]} :acyclic? true})

(deftest organise-o1-two-way-union
  (let [c (cp/organise cp/up-closure-temperament #{:a} repo)]
    (is (= #{:a :b :c} (:nodes c)))
    (is (= #{:b :c} (:added-by-organise c)))
    (is (= (:nodes c) (into (:selected c) (:added-by-organise c))))
    (is (= #{} (:admitted-by c)))))

(deftest organise-selected-only-adds-nothing
  (let [c (cp/organise cp/selected-only-temperament #{:a :c} repo)]
    (is (= #{:a :c} (:nodes c)))
    (is (= #{} (:added-by-organise c)))))

(deftest organise-o3-fast-forward-through-unselected
  ;; a and c selected, b not: the authored path a→b→c fast-forwards to a
  ;; single a→c edge over the selected carrier.
  (let [c (cp/organise cp/selected-only-temperament #{:a :c} repo)]
    (is (contains? (:edges c) [:a :c]))
    (is (not-any? (fn [[u v]] (or (= u :b) (= v :b))) (:edges c)))))

(deftest organise-o3-introduced-nodes-cannot-justify-edges
  ;; The ruled O3 bootstrap rejection: nodes organise itself introduced are
  ;; excluded from the fast-forward carrier, so up-closure introduction of
  ;; b and c yields NO edges among them from the introduction itself.
  (let [c (cp/organise cp/up-closure-temperament #{:a} repo)]
    (is (= #{} (:edges c)))))

(deftest organise-refuses-selection-outside-repository
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"escapes the repository"
                        (cp/organise cp/up-closure-temperament #{:z} repo))))

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
