(ns futon2.report.cascade-shape-discovery-test
  "Discovery-only computation; no production data writes or WM invocation."
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.test :refer [deftest is]]
            [futon2.aif.interpretation-construction :as constructor]
            [futon2.aif.interpretation-construction-test :as fixture]
            [futon2.aif.cascade-policy :as policy]))

(defn family-shape [family]
  (let [f (set family)
        comparable? (fn [a b] (or (set/subset? a b) (set/subset? b a)))
        pairs (for [a f b f :when (not= a b)] [a b])
        overlap? (some (fn [[a b]] (and (seq (set/intersection a b))
                                       (not (comparable? a b)))) pairs)
        missing (set (for [[a b] pairs :let [i (set/intersection a b)]
                           :when (not (contains? f i))] i))]
    {:shape (cond (empty? f) nil
                  (= 1 (count f)) :singleton
                  (every? #(apply comparable? %) pairs) :chain
                  (every? (fn [[a b]] (empty? (set/intersection a b))) pairs) :antichain
                  (not overlap?) :tree
                  (empty? missing) :semilattice
                  :else nil)
     :status (cond (empty? f) :empty-family
                   (and overlap? (seq missing)) :overlap-not-intersection-closed
                   :else :classified)
     :overlap? (boolean overlap?)
     :intersection-closed? (and (seq f) (empty? missing))
     :missing-intersections missing :family f}))

(defn support-family [nodes edges]
  (set (for [v nodes]
         (loop [seen #{v}]
           (let [more (into seen (for [[p q] edges :when (contains? seen q)] p))]
             (if (= more seen) seen (recur more)))))))

(defn action-row [action]
  (let [ps (:precedence action) nodes (set (map :id ps))
        _ (assert (= (count nodes) (count ps)) "duplicate pattern IDs need a separate carrier")
        _ (assert (every? #(and (= :and (get-in % [:guard :operator]))
                               (every? (fn [c] (set? (:present c))) (get-in % [:guard :clauses]))) ps))
        edges (set (for [p ps q ps :when (not= (:id p) (:id q))
                         :let [needs (reduce set/union #{} (map :present (get-in q [:guard :clauses])))]
                         :when (seq (set/intersection (:produces p) needs))] [(:id p) (:id q)]))]
    {:target (:target action) :id (or (:cascade-id action) (:id action))
     :precedence (mapv :id ps) :need-edges edges
     :support (family-shape (support-family nodes edges))
     :outputs (mapv #(select-keys % [:id :produces]) ps)
     :stored-structure (select-keys action [:semilattice :need-edges :organised :repository])}))

(def run-ids ["1789951020" "1789952479" "1789964661"])
(defn read-run [id]
  (edn/read-string (slurp (str "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-09-21-" id ".edn"))))
(def census
  (mapv (fn [id]
          {:run id :candidates (mapv (comp action-row :id)
                                    (get-in (read-run id) [:decision :selection-certificate :candidates]))}) run-ids))
(def pq (first (:candidates (constructor/construct fixture/input))))
(def pq-row
  (action-row {:target (:target pq) :id :PQ
               :precedence (mapv #(policy/token-interpretation % (fixture/interpretations %)) (:precedence pq))}))

(deftest shape-controls
  (is (= :singleton (:shape (family-shape #{#{:a}}))))
  (is (= :chain (:shape (family-shape #{#{:a} #{:a :b} #{:a :b :c}}))))
  (is (= :antichain (:shape (family-shape #{#{:a} #{:b}}))))
  (is (= :tree (:shape (family-shape #{#{:a} #{:b} #{:a :b}}))))
  (is (= :semilattice (:shape (family-shape #{#{:a} #{:a :b} #{:a :c} #{:a :b :c :d}}))))
  (is (= :overlap-not-intersection-closed (:status (family-shape #{#{:a :b} #{:b :c}}))))
  (is (= :empty-family (:status (family-shape #{})))))

(deftest retained-record-controls
  (prn {:census census :pq pq-row})
  (is (every? (fn [r] (= (count (:precedence r)) (count (get-in r [:support :family]))))
              (mapcat :candidates census)) "no cyclic SCC collapsed the principal supports")
  (is (= [24 24 3] (mapv #(count (:candidates %)) census)))
  (is (= [21 21 0] (mapv #(count (filter (comp empty? :precedence) (:candidates %))) census)))
  (is (= [:singleton :singleton :singleton] (mapv #(get-in % [:support :shape]) (:candidates (last census)))))
  (is (= [:tree :tree] (mapv #(get-in % [:support :shape]) (take 2 (:candidates (first census))))))
  (is (= #{[:P :Q]} (:need-edges pq) (:need-edges pq-row)))
  (is (= :chain (get-in pq-row [:support :shape])))
  (is (= #{#{:P} #{:P :Q}} (get-in pq-row [:support :family]))))

