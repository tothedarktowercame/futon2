(ns futon2.aif.meta-pipeline-selector
  "Pure proof-of-concept META selector over a pinned pipeline-cascade snapshot.

  The cascade supplies the work map.  Current task-state channels annotate its
  nodes.  Missing channels are absent, feasibility is support, and the
  deterministic information term is exactly zero.  Terminal outcomes are not
  predicted here."
  (:require [clojure.set :as set]))

(def schema :wm/meta-pipeline-selection-v1)
(def snapshot-schema :wm/pipeline-cascade-snapshot-v1)
(def task-kinds #{:mission :excursion :ticket :algorithm})

(defn- sha256? [x]
  (and (string? x) (boolean (re-matches #"[0-9a-f]{64}" x))))

(defn- pin? [x]
  (and (map? x) (string? (:path x)) (sha256? (:sha256 x))))

(defn- refusal [reason details]
  {:schema schema :status :refused :reason reason :details details})

(defn- snapshot-errors [{:keys [schema summary-source graph-source summary graph]}]
  (let [section-status (:section-status graph)]
    (cond-> []
      (not= snapshot-schema schema) (conj :snapshot-schema-invalid)
      (not (pin? summary-source)) (conj :summary-source-unpinned)
      (not (pin? graph-source)) (conj :graph-source-unpinned)
      (not= true (:consistent? summary)) (conj :cascade-inconsistent)
      (not-every? true? (map #(get-in summary [:standards %])
                             [:s1-regenerates :s2-evidence :s3-reconstitution
                              :s4-honest-holes :s5-composed]))
      (conj :cascade-standards-incomplete)
      (or (empty? section-status)
          (some #(not= :ok (:status %)) (vals section-status)))
      (conj :cascade-section-incomplete))))

(defn pipeline-node-ids
  "Return the exact task/pattern identities exposed by a verified graph value."
  [graph]
  (set (concat
        (mapcat (juxt :mission :predecessor :successor) (:lineage graph))
        (map :mission (:clusters graph))
        (mapcat (juxt :have :want) (:arrows graph))
        (map :mission (:held graph))
        (map :stem (get-in graph [:tickets :items]))
        (mapcat (juxt :mission :pattern) (get-in graph [:patterns :edges])))))

(defn- channel-valid? [[_ {:keys [value source freshness]}]]
  (and (number? value) (Double/isFinite (double value))
       (<= 0.0 (double value) 1.0)
       (pin? source) (= :current freshness)))

(defn- candidate-errors [nodes candidate]
  (cond-> []
    (not (string? (:id candidate))) (conj :identity-invalid)
    (not (contains? task-kinds (:kind candidate))) (conj :kind-invalid)
    (not (contains? nodes (:id candidate))) (conj :not-in-pipeline-cascade)
    (not (contains? #{:supported :infeasible :unknown}
                    (get-in candidate [:support :automated-feasibility])))
    (conj :feasibility-invalid)
    (not-every? channel-valid? (:channels candidate))
    (conj :task-state-channel-invalid)))

(defn- pairwise [a b]
  (let [shared (set/intersection (set (keys (:channels a)))
                                 (set (keys (:channels b))))]
    (if (empty? shared)
      {:status :unrankable :reason :no-shared-current-channel
       :left (:id a) :right (:id b) :shared-channels []}
      (let [ga (reduce + (map #(get-in a [:channels % :value]) shared))
            gb (reduce + (map #(get-in b [:channels % :value]) shared))]
        {:status :ranked :left (:id a) :right (:id b)
         :shared-channels (vec (sort shared))
         :g-left ga :g-right gb
         :winner (cond (< ga gb) (:id a) (> ga gb) (:id b) :else :tie)
         :epistemic-value-nats 0.0}))))

(defn select
  "Select from pipeline nodes using normalized preference-cost channels.

  Lower task-state cost is preferred.  Candidates with explicit infeasibility
  are excluded; `:unknown` feasibility is retained as unknown support rather
  than converted to failure.  Selection requires one candidate to beat every
  alternative on shared current channels."
  [{:keys [snapshot candidates]}]
  (let [snapshot-errors* (snapshot-errors snapshot)]
    (cond
      (seq snapshot-errors*)
      (refusal :pipeline-snapshot-invalid {:errors snapshot-errors*})

      (not (vector? candidates))
      (refusal :candidate-field-invalid {:expected :vector})

      :else
      (let [nodes (pipeline-node-ids (:graph snapshot))
            malformed (into {}
                            (keep (fn [c]
                                    (when-let [errors (seq (candidate-errors nodes c))]
                                      [(:id c) (vec errors)])))
                            candidates)
            infeasible (filterv #(= :infeasible
                                    (get-in % [:support :automated-feasibility]))
                                candidates)
            admitted (filterv #(not= :infeasible
                                     (get-in % [:support :automated-feasibility]))
                              candidates)]
        (cond
          (seq malformed)
          (refusal :candidate-invalid {:candidate-errors malformed})

          (empty? admitted)
          (refusal :no-supported-pipeline-item
                   {:typed-exclusions (mapv :id infeasible)})

          (= 1 (count admitted))
          {:schema schema :status :selected :selected (:id (first admitted))
           :reason :singleton-supported-pipeline-item
           :epistemic-value-nats 0.0
           :snapshot-sources (select-keys snapshot [:summary-source :graph-source])
           :typed-exclusions (mapv :id infeasible)}

          :else
          (let [pairs (vec (for [i (range (count admitted))
                                 j (range (inc i) (count admitted))]
                             (pairwise (nth admitted i) (nth admitted j))))
                unrankable (filterv #(= :unrankable (:status %)) pairs)
                losses (frequencies (keep (fn [{:keys [winner left right]}]
                                            (when (and winner (not= :tie winner))
                                              (if (= winner left) right left))) pairs))
                winners (filterv #(zero? (get losses (:id %) 0)) admitted)]
            (cond
              (seq unrankable)
              (refusal :shared-current-channel-unavailable
                       {:unrankable-pairs unrankable :pairwise pairs})

              (not= 1 (count winners))
              (refusal :no-unique-task-state-minimum
                       {:undefeated (mapv :id winners) :pairwise pairs})

              :else
              {:schema schema :status :selected :selected (:id (first winners))
               :reason :minimum-pairwise-task-state-G
               :epistemic-value-nats 0.0 :pairwise pairs
               :snapshot-sources (select-keys snapshot [:summary-source :graph-source])
               :typed-exclusions (mapv :id infeasible)})))))))
