(ns futon2.aif.retraction-cascade
  "The single conversion from a pattern-graph retraction receipt to an arranged
  cascade. Authored direction becomes descent; a relation without authored
  direction becomes overlap. Unit order uses reading order, then a stable
  target-qualified hash, and never treats alphabetical edge storage as shape."
  (:require [futon2.aif.load-identity :as identity]))

(identity/register! *ns* *file*)

(defn- reading-pattern-order [analysis-map]
  (vec (distinct
        (for [sentence (:sentences analysis-map)
              fragment (:fragments sentence)
              ref (:pattern_refs fragment)
              :when (= "candidate" (:status ref))]
          (:id ref)))))

(defn- arrangement-edge [{:keys [a b direction] :as edge}]
  (merge (select-keys edge [:kinds :kind-used :evidence])
         {:from (or (:from direction) a)
          :to (or (:to direction) b)
          :kind (if direction :precedes :overlap)
          :authored-direction direction}))

(defn- unit-tie-order [target reading-order nodes]
  (let [reading-rank (zipmap reading-order (range))]
    (vec (sort-by (fn [node]
                    [(get reading-rank node Long/MAX_VALUE)
                     (identity/sha256 (.getBytes (str target "\u0000" node) "UTF-8"))])
                  nodes))))

(defn- topological-unit-order [nodes edges tie-order]
  (let [directed (remove #(= :overlap (:kind %)) edges)
        incoming (frequencies (map :to directed))
        outgoing (group-by :from directed)
        tie-rank (zipmap tie-order (range))]
    (loop [left (set nodes) in incoming order []]
      (if (empty? left)
        order
        (let [ready (sort-by tie-rank (filter #(zero? (get in % 0)) left))
              ;; Keep cyclic units so cascade-shape-g can return its typed
              ;; :cyclic-arrangement receipt with the actual closed path.
              node (or (first ready) (first (sort-by tie-rank left)))
              children (map :to (get outgoing node))]
          (recur (disj left node)
                 (reduce #(update %1 %2 (fnil dec 0)) in children)
                 (conj order node)))))))

(defn from-retraction
  "Convert one retraction receipt into the arrangement scored by the machine."
  [target analysis-map retraction]
  (let [edges (mapv arrangement-edge (:edges retraction))
        tie-order (unit-tie-order target (reading-pattern-order analysis-map)
                                  (:nodes retraction))
        precedence (topological-unit-order (:nodes retraction) edges tie-order)]
    {:nodes (:nodes retraction) :edges edges :precedence precedence
     :unit-order-rule :reading-order-then-target-stable-hash}))
