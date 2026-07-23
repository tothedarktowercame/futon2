(ns futon2.aif.mission-control-graph
  "Pure schema and projection helpers for the DARK strategic mission landscape.

   Missions are related to the high-level p4ng control patterns by typed,
   evidence-bearing edges. Embeddings may propose these edges, but only a
   witnessed edge is eligible to support strategic candidate construction.
   Attached memory ids are retrieval handles, never selection weights."
  (:require [clojure.string :as str]))

(def relation-types
  #{:requires-control :repairs-control :instantiates-control
    :produces-evidence-for :blocked-by-control})

(def edge-statuses #{:proposed :witnessed :challenged :retracted})

(defn valid-control-pattern-id?
  [pattern-id]
  (and (string? pattern-id)
       (str/starts-with? pattern-id "p4ng/")
       (> (count pattern-id) (count "p4ng/"))))

(defn validate-edge
  "Return EDGE when it satisfies the strategic relation contract; otherwise
   throw with the failed edge. Provenance is mandatory even for proposals so
   an embedding suggestion can never become an unattributed graph fact."
  [edge]
  (let [memory-ids (or (:memory-ids edge) [])]
    (when-not (and (map? edge)
                   (string? (:mission-id edge))
                   (not (str/blank? (:mission-id edge)))
                   (valid-control-pattern-id? (:control-pattern-id edge))
                   (contains? relation-types (:relation edge))
                   (contains? edge-statuses (:status edge))
                   (vector? (:provenance edge))
                   (seq (:provenance edge))
                   (vector? memory-ids)
                   (every? string? memory-ids))
      (throw (ex-info "invalid mission-control edge" {:edge edge})))
    (assoc edge :memory-ids memory-ids)))

(defn candidate-projection
  "Project witnessed mission candidates for active p4ng control patterns.

   The result is reason-bearing support only: it carries relations,
   provenance, and attached memories but assigns no E, G, utility, or
   embedding-derived score. Proposed/challenged/retracted edges remain in the
   audit counts and cannot silently admit a mission.

   The three-argument form is the Phase-4 strict projection. It admits support
   only when an edge cites at least one concrete, current, reviewed,
   independently witnessed War Machine memory. The returned reasons contain
   those bodies, not retrieval handles alone."
  ([active-pattern-ids edges]
   (candidate-projection active-pattern-ids edges nil))
  ([active-pattern-ids edges memories]
   (let [strict-memory? (some? memories)
         active (set active-pattern-ids)
         _ (when-not (every? valid-control-pattern-id? active)
             (throw (ex-info "invalid active control-pattern id"
                             {:active-pattern-ids active-pattern-ids})))
         checked (mapv validate-edge edges)
         relevant (filterv #(contains? active (:control-pattern-id %)) checked)
         memories-by-id (into {} (map (juxt :memory/id identity)) memories)
         eligible-memory?
         (fn [memory]
           (and (= :war-machine (:memory/domain memory))
                (= :current (:memory/state memory))
                (= :reviewed (:memory/attachment-status memory))
                (= :independently-witnessed (:memory/witness-status memory))
                (map? (:memory/body memory))))
         attach-memories
         (fn [edge]
           (let [attached (into [] (keep memories-by-id) (:memory-ids edge))
                 eligible (filterv eligible-memory? attached)]
             (assoc edge
                    :attached-memories attached
                    :eligible-memories eligible)))
         enriched (if strict-memory?
                    (mapv attach-memories relevant)
                    relevant)
         support-eligible?
         (fn [edge]
           (and (= :witnessed (:status edge))
                (not= :blocked-by-control (:relation edge))
                (or (not strict-memory?)
                    (seq (:eligible-memories edge)))))
         block-eligible?
         (fn [edge]
           (and (= :witnessed (:status edge))
                (= :blocked-by-control (:relation edge))
                (or (not strict-memory?)
                    (seq (:eligible-memories edge)))))
         supporting (filterv support-eligible? enriched)
         blocking (group-by :mission-id (filter block-eligible? enriched))
         blocker-reason
         (fn [edge]
           (cond->
            (select-keys edge
                         [:control-pattern-id :relation :provenance
                          :memory-ids])
             strict-memory?
             (assoc :memories
                    (mapv #(select-keys
                            % [:memory/id :memory/hook :memory/body
                               :memory/witness-status :memory/mission-ids])
                          (:eligible-memories edge)))))
         excluded-missions
         (->> blocking
              (map
               (fn [[mission-id mission-blockers]]
                 {:mission-id mission-id
                  :exclusion :witnessed-block
                  :blocking-relations
                  (mapv blocker-reason
                        (sort-by (juxt :control-pattern-id :relation)
                                 mission-blockers))}))
              (sort-by :mission-id)
              vec)
         candidates
         (->> supporting
              (group-by :mission-id)
              (keep
               (fn [[mission-id mission-edges]]
                 (when-not (seq (get blocking mission-id))
                   {:mission-id mission-id
                    :candidate-source :p4ng-control-hypergraph
                    :control-pattern-ids
                    (vec (sort (set (map :control-pattern-id mission-edges))))
                    :support-relations
                    (mapv
                     (fn [edge]
                       (cond->
                        (select-keys edge
                                     [:control-pattern-id :relation
                                      :provenance :memory-ids])
                         strict-memory?
                         (assoc :memories
                                (mapv #(select-keys
                                       % [:memory/id :memory/hook :memory/body
                                          :memory/witness-status
                                          :memory/mission-ids])
                                      (:eligible-memories edge)))))
                     mission-edges)
                    :memory-ids
                    (vec (sort (set (mapcat :memory-ids mission-edges))))})))
              (sort-by :mission-id)
              vec)
         attached (mapcat :attached-memories enriched)]
     {:active-control-patterns (vec (sort active))
      :candidate-count (count candidates)
      :candidates candidates
      :excluded-missions excluded-missions
      :audit
      (cond->
       {:edge-count (count relevant)
        :witnessed-support-count (count supporting)
        :witnessed-block-count (reduce + 0 (map count (vals blocking)))
        :proposal-count (count (filter #(= :proposed (:status %)) relevant))
        :challenged-count (count (filter #(= :challenged (:status %)) relevant))
        :retracted-count (count (filter #(= :retracted (:status %)) relevant))}
        strict-memory?
        (assoc
         :memory-count (count memories)
         :cross-domain-memory-count
         (count (filter #(not= :war-machine (:memory/domain %)) attached))
         :unwitnessed-memory-count
         (count (filter #(not= :independently-witnessed
                               (:memory/witness-status %))
                        attached))
         :ineligible-witnessed-edge-count
         (count
          (filter #(and (= :witnessed (:status %))
                        (empty? (:eligible-memories %)))
                  enriched))))})))
