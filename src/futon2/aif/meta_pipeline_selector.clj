(ns futon2.aif.meta-pipeline-selector
  "Pure proof-of-concept META selector over a pinned pipeline-cascade snapshot.

  The cascade supplies the work map.  Current task-state channels annotate its
  nodes.  Missing channels are absent, feasibility is support, and the
  deterministic information term is exactly zero.  Terminal outcomes are not
  predicted here."
  (:require [clojure.set :as set]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def schema :wm/meta-pipeline-selection-v1)
(def snapshot-schema :wm/pipeline-cascade-snapshot-v1)
(def task-kinds #{:mission :excursion :ticket :algorithm})

(defn- sha256? [x]
  (and (string? x) (boolean (re-matches #"[0-9a-f]{64}" x))))

(defn- pin? [x]
  (and (map? x) (string? (:path x)) (sha256? (:sha256 x))))

(defn- refusal [reason details]
  {:schema schema :status :refused :reason reason :details details})

(def ^:private qualified-work-id
  #"^([^/]+)-d/(mission|excursion|ticket)/([^/]+)$")

(def ^:private kind-prefix
  {"mission" "M-" "excursion" "E-" "ticket" "T-"})

(defn canonical-work-id
  "Map one exact qualified cascade work identity to its registry M/E/T id.
  Canonical registry ids pass through; every other form returns nil."
  [x]
  (cond
    (and (string? x) (re-matches #"^[MET]-[^/]+$" x)) x
    (string? x) (when-let [[_ _ kind stem] (re-matches qualified-work-id x)]
                  (str (kind-prefix kind) stem))))

(defn- raw-pipeline-node-ids [graph]
  (concat
   (mapcat (juxt :mission :target :predecessor :successor) (:lineage graph))
   (map :mission (:clusters graph))
   (mapcat (juxt :have :want) (:arrows graph))
   (map :mission (:held graph))
   (map :stem (get-in graph [:tickets :items]))
   (mapcat (juxt :mission :pattern) (get-in graph [:patterns :edges]))))

(defn- pipeline-identity-analysis [graph]
  (let [raw-ids (set (filter string? (raw-pipeline-node-ids graph)))
        malformed (->> raw-ids
                       (filter #(and (re-find #"-d/(mission|excursion|ticket)/" %)
                                     (not (re-matches qualified-work-id %))))
                       sort vec)
        qualified (keep (fn [raw]
                          (when-let [[_ authority kind stem]
                                     (re-matches qualified-work-id raw)]
                            {:raw raw :authority authority
                             :canonical (str (kind-prefix kind) stem)}))
                        raw-ids)
        by-canonical (group-by :canonical qualified)
        collisions (into (sorted-map)
                         (keep (fn [[canonical rows]]
                                 (when (< 1 (count (set (map :authority rows))))
                                   [canonical (vec (sort (map :raw rows)))])))
                         by-canonical)
        canonicalized (reduce (fn [ids {:keys [raw canonical]}]
                                (conj (disj ids raw) canonical))
                              raw-ids qualified)]
    {:ids canonicalized :malformed malformed :collisions collisions}))

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
  "Return registry-canonical M/E/T ids and exact non-work identities in graph.

  Qualified work ids are accepted only in the exact `AUTHORITY-d/kind/stem`
  form. Ambiguous canonical ids and malformed work ids are omitted rather than
  guessed; unrelated exact identities remain available."
  [graph]
  (let [{:keys [ids malformed collisions]} (pipeline-identity-analysis graph)]
    (apply disj ids (concat malformed (keys collisions)))))

(defn- channel-valid? [[_ {:keys [value source freshness]}]]
  (and (number? value) (Double/isFinite (double value))
       (<= 0.0 (double value) 1.0)
       (pin? source) (= :current freshness)))

(defn- candidate-errors [nodes collisions snapshot candidate]
  (cond-> []
    (not (string? (:id candidate))) (conj :identity-invalid)
    (not (contains? task-kinds (:kind candidate))) (conj :kind-invalid)
    (contains? collisions (:id candidate)) (conj :pipeline-identity-collision)
    (and (not (contains? collisions (:id candidate)))
         (not (contains? nodes (:id candidate)))) (conj :not-in-pipeline-cascade)
    (not (contains? #{:supported :infeasible :unknown}
                    (get-in candidate [:support :automated-feasibility])))
    (conj :feasibility-invalid)
    (not-every? channel-valid? (:channels candidate))
    (conj :task-state-channel-invalid)
    (some (fn [[channel {:keys [source]}]]
            (and (contains? #{:pipeline-structural-centrality-cost
                              :pipeline-freshness-cost} channel)
                 (not= source (:graph-source snapshot))))
          (:channels candidate))
    (conj :task-state-source-mismatch)))

(defn- channel-coverage [candidates]
  (let [channels (->> candidates (mapcat (comp keys :channels)) set sort vec)]
    {:candidate-count (count candidates)
     :by-channel (into (sorted-map)
                       (map (fn [channel]
                              [channel (count (filter #(contains? (:channels %) channel)
                                                     candidates))]))
                       channels)
     :unsupported-by-kind
     (->> candidates
          (mapcat (fn [{:keys [kind unsupported-channels]}]
                    (map (fn [channel] [kind channel]) unsupported-channels)))
          frequencies
          (into (sorted-map)))}))

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
      (let [analysis (pipeline-identity-analysis (:graph snapshot))
            nodes (pipeline-node-ids (:graph snapshot))
            malformed (into {}
                            (keep (fn [c]
                                    (when-let [errors (seq (candidate-errors
                                                           nodes (:collisions analysis)
                                                           snapshot c))]
                                      [(:id c) (vec errors)])))
                            candidates)
            infeasible (filterv #(= :infeasible
                                    (get-in % [:support :automated-feasibility]))
                                candidates)
            admitted (filterv #(not= :infeasible
                                     (get-in % [:support :automated-feasibility]))
                              candidates)
            coverage (channel-coverage admitted)]
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
           :channel-coverage coverage
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
                       {:unrankable-pairs unrankable :pairwise pairs
                        :channel-coverage coverage})

              (not= 1 (count winners))
              (refusal :no-unique-task-state-minimum
                       {:undefeated (mapv :id winners) :pairwise pairs
                        :channel-coverage coverage})

              :else
              {:schema schema :status :selected :selected (:id (first winners))
               :reason :minimum-pairwise-task-state-G
               :epistemic-value-nats 0.0 :pairwise pairs
               :channel-coverage coverage
               :ranking (->> admitted
                             (sort-by (juxt #(get losses (:id %) 0) :id))
                             (map-indexed (fn [i candidate]
                                            {:rank (inc i) :id (:id candidate)
                                             :kind (:kind candidate)
                                             :pairwise-losses (get losses (:id candidate) 0)
                                             :channels (:channels candidate)
                                             :unsupported-channels
                                             (:unsupported-channels candidate)}))
                             vec)
               :snapshot-sources (select-keys snapshot [:summary-source :graph-source])
               :typed-exclusions (mapv :id infeasible)})))))))
