(ns futon2.aif.cascade-structure
  "Record-only shape of declared prerequisite supports; never authority or execution."
  (:require [clojure.set :as set]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(defn- closure [seed edges]
  (loop [seen seed]
    (let [more (into seen (for [[p q] edges :when (contains? seen q)] p))]
      (if (= seen more) seen (recur more)))))

(defn- components [family]
  (loop [remaining family n 0]
    (if (empty? remaining) n
        (let [edges (for [a remaining b remaining :when (seq (set/intersection a b))] [a b])
              reached (closure #{(first remaining)} edges)]
          (recur (set/difference remaining reached) (inc n))))))

(defn classify-family
  "Classify the supplied finite set family without completing missing meets.
  Tree means the laminar-family condition, including disconnected forests.
  Return independent axes: a chain also satisfies intersection closure."
  [family]
  (let [family (set family)
        pairs (for [a family b family :when (not= a b)] [a b])
        comparable? (fn [a b] (or (set/subset? a b) (set/subset? b a)))
        overlaps (set (for [[a b] pairs :when (and (seq (set/intersection a b))
                                                   (not (comparable? a b)))] #{a b}))
        missing (set (for [[a b] pairs :let [meet (set/intersection a b)]
                           :when (not (contains? family meet))] meet))
        n (components family)
        shape (cond (empty? family) :empty-family
                    (= 1 (count family)) :singleton
                    (every? #(apply comparable? %) pairs) :chain
                    (every? (fn [[a b]] (empty? (set/intersection a b))) pairs) :antichain
                    (empty? overlaps) :tree
                    (empty? missing) :semilattice
                    :else :unclassified)]
    {:shape shape :family family :component-count n :connected? (= 1 n)
     :overlap? (boolean (seq overlaps)) :overlap-witnesses overlaps
     :intersection-closed? (boolean (and (seq family) (empty? missing)))
     :missing-intersections missing
     :findings (cond-> []
                 (= shape :empty-family) (conj {:kind :empty-family})
                 (seq missing) (conj {:kind :missing-intersections :sets missing})
                 (= shape :unclassified) (conj {:kind :overlap-not-intersection-closed}))}))

(defn- interpreted? [p]
  (and (map? p) (or (keyword? (:id p)) (string? (:id p)))
       (set? (:produces p))
       (= :interpreted (get-in p [:guard :status]))
       (= :and (get-in p [:guard :operator]))
       (vector? (get-in p [:guard :clauses])) (seq (get-in p [:guard :clauses]))
       (every? #(and (= :interpreted (:status %)) (set? (:present %)) (set? (:absent %)))
               (get-in p [:guard :clauses]))))

(defn receipt
  "Measure only the action supplied. Invalid/cyclic inputs yield typed absence;
  no selection, admission, fold, identity, source lookup or observation changes."
  [action]
  (let [ps (:precedence action)
        inputs {:target (:target action)
                :patterns (if (vector? ps)
                            (mapv #(if (map? %) (select-keys % [:id :guard :produces]) %) ps)
                            ps)}
        base {:schema :wm/cascade-structure-v1 :basis :declared-need-support-v1
              :derivation :positive-producer-to-consumer-reflexive-supports
              :input-sha256 (evidence/value-digest inputs)
              :authority-evidence {:status :unavailable :reason :authority-graph-not-retained}}
        duplicate-ids (when (vector? ps) (set (for [[id n] (frequencies (map :id ps)) :when (> n 1)] id)))
        invalid (cond (not (vector? ps)) {:kind :missing-or-invalid-precedence}
                      (seq duplicate-ids) {:kind :duplicate-pattern-ids :ids duplicate-ids}
                      (not (every? interpreted? ps)) {:kind :unsupported-pattern-interpretation
                                                     :patterns (mapv #(if (map? %) (:id %) %)
                                                                     (remove interpreted? ps))})]
    (if invalid
      (assoc base :status :unavailable :shape :unclassified :findings [invalid])
      (let [carrier (set (map :id ps))
            witnessed (set (for [p ps q ps
                                 :let [tokens (set/intersection
                                               (:produces p)
                                               (reduce set/union #{} (map :present (get-in q [:guard :clauses]))))]
                                 :when (seq tokens)]
                             {:from (:id p) :to (:id q) :tokens tokens}))
            edges (set (map (juxt :from :to) witnessed))
            supports (into {} (for [id carrier] [id (closure #{id} edges)]))
            cyclic (set/union (set (for [[p q] edges :when (= p q)] p))
                              (set (for [p carrier q carrier :when (and (not= p q)
                                                            (contains? (supports p) q)
                                                            (contains? (supports q) p))] p)))]
        (if (seq cyclic)
          (assoc base :status :unavailable :shape :unclassified :carrier carrier :need-edges witnessed
                 :findings [{:kind :cyclic-needs :patterns cyclic}])
          (merge base {:status :computed :carrier carrier :need-edges witnessed :supports supports}
                 (classify-family (set (vals supports)))))))))

(defn description [receipt]
  (str "shape: " (name (:shape receipt)) " (basis: "
       (if (= :declared-need-support-v1 (:basis receipt)) "declared need-support" (str (:basis receipt)))
       "; authority structure not recorded)"))
