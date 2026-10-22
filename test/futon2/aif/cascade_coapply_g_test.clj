(ns futon2.aif.cascade-coapply-g-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-coapply-g :as g]
            [futon2.aif.cascade-policy :as policy]
            [futon2.aif.cascade-shape-g :as shape]))

(defn pattern [id needs produces theta]
  (assoc (policy/token-interpretation id {:guard {:needs needs :forbids produces}
                                         :produces produces}) :theta theta))
(defn candidate [id patterns descent]
  {:kind :cascade-candidate :id id :target "T"
   :precedence {:co-apply {:units (vec (keys patterns)) :descent descent :patterns patterns}}})
(def a ["T" :a])
(def b ["T" :b])
(def c ["T" :c])
(def join (candidate :join (array-map :a (pattern :a #{} #{a} 1)
                                      :b (pattern :b #{} #{b} 1)
                                      :c (pattern :c #{a b} #{c} 1)) []))
(def counts {:a [1 1] :b [1 1] :c [1 1]})
(defn opts [h counts-map]
  {:horizon-steps h :pattern-counts counts-map
   :observation-model (shape/class-emission-model "T" #{a b c} #{c} h :focused
                      {:status :synthetic :source "Lean co-application fixture"})
   :prediction-context {:occurrence-id "test-only" :tau h}
   :observation {:status :observed :occurrence-id "test-only" :tau h :present #{c} :absent #{}}
   :cascade-spec {:want #{c} :evidence #{} :zeroed #{}}})
(defn close? [a b] (< (Math/abs (- (double a) (double b))) 1e-10))

(deftest frontier-and-shared-horizon
  (let [chain (assoc-in join [:precedence :co-apply :descent] [[:a :b]])
        chain (assoc chain :id :chain)
        ranked (g/compare-at-horizon {:cascade-belief {#{} 1}} [join chain]
                                    (opts 3 {:join counts :chain counts}))]
    (is (vector? ranked) (pr-str ranked))
    (is (= #{3} (set (map :horizon-steps ranked))))
    (doseq [e ranked]
      (is (= :none (get-in e [:certificate :g-terms :normalization])))
      (is (= 3 (count (get-in e [:certificate :parameter-novelty :steps])))))
    (let [e (first (filter #(= :join (:cascade-id %)) ranked))
          {:keys [risk ambiguity expected-information-gain]} (get-in e [:certificate :g-terms])]
      (is (= {#{a b} 1} (get-in e [:certificate :steps 0 :belief])))
      (is (= [1/2 1/4 0] (mapv :expected-novelty (get-in e [:certificate :parameter-novelty :steps]))))
      (is (= 3/4 expected-information-gain))
      (is (close? (:G-efe e) (- (+ risk ambiguity) 3/4))))))

(deftest state-weighting-and-no-credit-for-disabled-patterns
  (let [q {#{} 1/2 #{a b} 1/2}]
    (is (= 3/8 (g/expected-novelty join counts q)))
    (is (= 1/2 (g/expected-novelty join (assoc counts :c [100 100]) {#{} 1})))
    (is (> (g/expected-novelty join counts {#{} 1})
           (g/expected-novelty join (assoc counts :a [2 1]) {#{} 1})))
    (is (= 0 (g/expected-novelty join counts {#{a b c} 1}))))
  (let [stochastic (assoc-in join [:precedence :co-apply :patterns :a :theta] 1/2)
        result (g/compare-at-horizon {:cascade-belief {#{} 1}} [stochastic] (opts 2 {:join counts}))]
    (is (vector? result) (pr-str result))
    (is (= {#{b} 1/2 #{a b} 1/2} (get-in result [0 :certificate :steps 0 :belief])))
    (is (= [1/2 1/4] (mapv :expected-novelty (get-in result [0 :certificate :parameter-novelty :steps]))))))

(deftest counts-change-g-with-fixed-transition
  (let [older (assoc join :id :older)
        rs (g/compare-at-horizon {:cascade-belief {#{} 1}} [join older]
                                (opts 2 {:join counts :older (zipmap (keys counts) (repeat [2 2]))}))]
    (is (vector? rs) (pr-str rs))
    (is (= :join (:cascade-id (first rs))))
    (is (< (:G-efe (first rs)) (:G-efe (second rs))))))

(deftest refuses-unsupported-or-incoherent-inputs
  (doseq [[candidate options reason]
          [[join (opts 2 {}) :pattern-counts-missing]
           [join (opts 2 {:join (assoc counts :a [0 1])}) :invalid-pattern-counts]
           [join (opts 2 {:join (assoc counts :a :bad)}) :invalid-pattern-counts]
           [join (assoc (opts 2 {:join counts}) :g-normalization :per-step-capacity-and-pattern) :conflicting-score-law]
           [(assoc-in join [:precedence :co-apply :descent] [[:a :b] [:b :a]])
            (opts 2 {:join counts}) :cyclic-containment]
           [(assoc join :precedence []) (opts 2 {:join counts}) :invalid-co-application]]]
    (is (= reason (:reason (g/compare-at-horizon {:cascade-belief {#{} 1}} [candidate] options)))))
  (is (= :invalid-pattern-counts
         (try (g/beta-novelty [Double/NaN 1]) (catch clojure.lang.ExceptionInfo e (:reason (ex-data e)))))))

(deftest unequal-lengths-use-one-horizon-and-raw-epoch-sum
  (let [short (candidate :short {:c (pattern :c #{} #{c} 1)} [])
        rs (g/compare-at-horizon {:cascade-belief {#{} 1}} [join short]
                                (opts 3 {:join counts :short {:c [1 1]}}))]
    (is (vector? rs) (pr-str rs))
    (is (= [3 3] (mapv :horizon-steps rs)))
    (doseq [e rs]
      (is (close? (:G-efe e) (reduce + (map :g (get-in e [:certificate :steps]))))))))
