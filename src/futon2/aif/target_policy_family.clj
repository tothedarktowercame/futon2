(ns futon2.aif.target-policy-family
  "Build one target's structurally distinct policy family from its current 象
  reading and pinned graph. The defaults, k=3 and weights why/how=1,
  co-cited=2, rejected-beside/next-in-session=3, are the parameters recorded
  by the S3c mission-head retraction artifacts. This namespace does not score."
  (:require [futon2.aif.analysis-cascade :as analysis]
            [futon2.aif.cascade-shape-g :as shape-g]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.pattern-retraction :as retraction]))

(identity/register! *ns* *file*)

(def default-retraction {:k 3 :weights graph-pin/default-weights})

(defn- stable-id [target structure]
  (str target "/policy-"
       (subs (identity/sha256 (.getBytes (pr-str structure) "UTF-8")) 0 20)))

(defn- reading-patterns [analysis-map]
  (vec (sort (distinct
              (for [sentence (:sentences analysis-map)
                    fragment (:fragments sentence)
                    ref (:pattern_refs fragment)
                    :when (= "candidate" (:status ref))]
                (:id ref))))))

(defn- reading-pattern-order [analysis-map]
  (vec (distinct
        (for [sentence (:sentences analysis-map)
              fragment (:fragments sentence)
              ref (:pattern_refs fragment)
              :when (= "candidate" (:status ref))]
          (:id ref)))))

(defn- reading-policies [target analysis-map]
  (vec (for [[mode kind] [[:alternatives :reading-alternatives]
                          [:overlap :reading-overlap]]
             cascade (:cascades (analysis/analysis->cascades analysis-map {:mode mode}))]
         {:target target :mission target :kind kind :cascade cascade
          :analysis analysis-map})))

(defn- retraction-edge [{:keys [a b direction] :as edge}]
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
              ;; Cycles are refused by cascade-shape-g. Retain all units here
              ;; so its cycle receipt can name the actual closed path.
              node (or (first ready) (first (sort-by tie-rank left)))
              children (map :to (get outgoing node))]
          (recur (disj left node)
                 (reduce #(update %1 %2 (fnil dec 0)) in children)
                 (conj order node)))))))

(defn- retraction-policies [target analysis-map result]
  (let [reading-order (reading-pattern-order analysis-map)]
    (mapv (fn [r]
            (let [edges (mapv retraction-edge (:edges r))
                  tie-order (unit-tie-order target reading-order (:nodes r))
                  precedence (topological-unit-order (:nodes r) edges tie-order)]
              {:target target :mission target :kind :retraction :analysis analysis-map
               :cascade {:nodes (:nodes r) :edges edges :precedence precedence
                         :unit-order-rule :reading-order-then-target-stable-hash}}))
          (:retractions result))))

(defn- deduplicate [policies]
  (:rows
   (reduce (fn [{:keys [seen] :as acc} policy]
             (let [structure (shape-g/structural-identity policy)]
               (if (seen structure) acc
                   (-> acc (update :seen conj structure)
                       (update :rows conj
                               (assoc policy :policy-id
                                      (stable-id (:target policy) structure)))))))
           {:seen #{} :rows []} policies)))

(defn policy-family
  "Return reading and retraction policies, preserving every counted failure."
  [{:keys [reading graph retraction]}]
  (let [target (:target-id reading)]
    (if (= :absent (:status reading))
      {:status :failed :target-id target :policies []
       :reported-count 0 :distinct-count 0
       :failures [(select-keys reading [:kind :target-id :expected-digest :found-digests])]
       :failure-count 1
       :provenance {:excerpt-digest (:expected-digest reading)}}
      (let [analysis-map (:analysis reading)
            params (merge default-retraction retraction)
            seeds (reading-patterns analysis-map)
            ;; A reading pattern with no graph edges (or outside the library)
            ;; cannot seed a retraction.  It is counted, and the retraction
            ;; runs over the remaining seeds, as the S3c lab artifacts did
            ;; (their refused_no_edges).  The pattern stays in the reading
            ;; cascades.
            library (set (:pattern-ids graph))
            edge-nodes (set (mapcat (juxt :a :b) (:edges graph)))
            unknown (vec (sort (remove library seeds)))
            isolated (vec (sort (filter #(and (library %) (not (edge-nodes %))) seeds)))
            usable (vec (remove (set (concat unknown isolated)) seeds))
            seed-failures (cond-> []
                            (seq unknown) (conj {:kind :seed-not-in-graph :seeds unknown})
                            (seq isolated) (conj {:kind :isolated-seed :seeds isolated}))
            retract (if (seq usable)
                      (retraction/retractions graph (assoc params :seeds usable))
                      {:retractions [] :failures [{:kind :no-usable-retraction-seed
                                                   :seeds (vec (sort seeds))}]})
            reported (vec (concat (reading-policies target analysis-map)
                                  (retraction-policies target analysis-map retract)))
            policies (deduplicate reported)
            failures (into seed-failures (:failures retract))
            failures (cond-> failures
                       (empty? policies) (conj {:kind :empty-policy-family
                                                :target-id target}))]
        {:status (if (seq policies) :computed :failed)
         :target-id target :policies policies
         :reported-count (count reported) :distinct-count (count policies)
         :failures failures :failure-count (count failures)
         :provenance {:excerpt-digest (:excerpt-digest reading)
                      :analysis-digest (:analysis-digest reading)
                      :graph-digest (identity/sha256 (.getBytes (pr-str graph) "UTF-8"))
                      :retraction (assoc params :seeds usable
                                         :all-reading-seeds (vec (sort seeds)))}}))))
