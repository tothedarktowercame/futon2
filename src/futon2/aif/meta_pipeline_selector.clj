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

(def ^:private structural-sections
  [{:section :lineage :status-keys [:lineage "clock/clocked-on"]
    :node-ids (fn [graph]
                (mapcat (juxt :mission :target :predecessor :successor)
                        (:lineage graph)))}
   {:section :clusters :status-keys [:clusters "cascade/cluster-member"]
    :node-ids (fn [graph] (map :mission (:clusters graph)))}
   {:section :arrows :status-keys [:arrows "code/v05/mined-move"]
    :node-ids (fn [graph] (mapcat (juxt :have :want) (:arrows graph)))}
   {:section :held :status-keys [:held "held/on-mission"]
    :node-ids (fn [graph] (map :mission (:held graph)))}
   {:section :patterns :status-keys [:patterns "mission-scope/pattern"]
    :node-ids (fn [graph] (map :mission (get-in graph [:patterns :edges])))}])

(defn- raw-pipeline-node-ids [graph]
  ;; Only structural work positions define membership.  The tickets section is
  ;; an inventory/mtime surface, and :pattern names evidence associated with a
  ;; mission; neither may promote a registry file onto the cascade map.
  (concat
   (mapcat (juxt :mission :target :predecessor :successor) (:lineage graph))
   (map :mission (:clusters graph))
   (mapcat (juxt :have :want) (:arrows graph))
   (map :mission (:held graph))
   (map :mission (get-in graph [:patterns :edges]))))

(defn section-availability
  "Measure supplied cascade sections without treating an unavailable section as
  an observed empty one.  Old partial snapshots named only the sections they
  supplied; those omitted names remain unspecified rather than being invented
  as failures.  An entirely missing status map is an unavailable population."
  [snapshot]
  (let [statuses (get-in snapshot [:graph :section-status])
        status-map? (map? statuses)
        entries (if status-map? statuses {})
        entry (fn [[section value]]
                {:section section
                 :status (:status value)
                 :availability (if (= :ok (:status value))
                                 :available :unavailable)
                 :reason (when-not (= :ok (:status value))
                           (or (:error value) :section-not-ok))})
        measured (->> entries (map entry) (sort-by #(pr-str (:section %))) vec)
        unavailable (filterv #(= :unavailable (:availability %)) measured)]
    {:status (cond
               (not status-map?) :absent
               (empty? entries) :absent
               (empty? unavailable) :complete
               :else :partial)
     :sections measured
     :available-sections (mapv :section (filter #(= :available (:availability %)) measured))
     :unavailable-sections unavailable}))

(defn pipeline-membership
  "Return only node identities evidenced by available structural sections.

  A task absent from these ids is *unknown*, rather than known off-map, when a
  supplied structural section is unavailable.  This is deliberately a
  population measurement: it never promotes an unknown task into membership."
  [snapshot]
  (let [graph (:graph snapshot)
        availability (section-availability snapshot)
        status-by-key (into {} (map (juxt :section identity) (:sections availability)))
        unavailable? (fn [{:keys [status-keys]}]
                       (let [entry (some #(get status-by-key %) status-keys)]
                         (or (= :absent (:status availability))
                             (and entry (= :unavailable (:availability entry))))))
        unavailable-sections (->> structural-sections
                                  (filter unavailable?)
                                  (mapv :section))
        available-raw (mapcat (fn [{:keys [node-ids] :as section}]
                                (if (unavailable? section) [] (node-ids graph)))
                              structural-sections)
        ;; Reuse the canonicalisation/collision rules over precisely the
        ;; available structural rows, not the entire (possibly stale) graph.
        analysis (let [raw-ids (set (filter string? available-raw))
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
                       ids (reduce (fn [ids {:keys [raw canonical]}]
                                     (conj (disj ids raw) canonical)) raw-ids qualified)]
                   {:ids (apply disj ids (concat malformed (keys collisions)))
                    :malformed malformed :collisions collisions})]
    (assoc analysis
           :section-availability availability
           :unavailable-structural-sections unavailable-sections
           :membership-complete? (empty? unavailable-sections))))

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

(defn- snapshot-errors [{:keys [schema summary-source graph-source summary]}]
  (cond-> []
      (not= snapshot-schema schema) (conj :snapshot-schema-invalid)
      (not (pin? summary-source)) (conj :summary-source-unpinned)
      (not (pin? graph-source)) (conj :graph-source-unpinned)
      (not= true (:consistent? summary)) (conj :cascade-inconsistent)
      (not-every? true? (map #(get-in summary [:standards %])
                             [:s1-regenerates :s2-evidence :s3-reconstitution
                              :s4-honest-holes :s5-composed]))
      (conj :cascade-standards-incomplete)))

(defn validate-snapshot
  "Validate the pinned pipeline snapshot without selecting any candidate."
  [snapshot]
  (let [errors (snapshot-errors snapshot)]
    (if (seq errors)
      (refusal :pipeline-snapshot-invalid {:errors errors})
      {:schema schema :status :verified})))

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
  (let [algorithm? (= :algorithm (:kind candidate))
        admission (:algorithm-admission candidate)]
   (cond-> []
    (not (string? (:id candidate))) (conj :identity-invalid)
    (not (contains? task-kinds (:kind candidate))) (conj :kind-invalid)
    (contains? collisions (:id candidate)) (conj :pipeline-identity-collision)
    (and (not algorithm?)
         (not (contains? collisions (:id candidate)))
         (not (contains? nodes (:id candidate)))) (conj :not-in-pipeline-cascade)
    (and algorithm?
         (not (and (= :active (get-in admission [:injury-observation :status]))
                   (= (:repairs-capability candidate)
                      (get-in admission [:injury-observation :capability]))
                   (= :verified
                      (get-in admission [:injury-observation
                                         :injury-verification :status])))))
    (conj :algorithm-injury-admission-invalid)
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
    (conj :task-state-source-mismatch))))

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

(defn- pairwise [a b comparison missing-channel-cost]
  (let [a-channels (set (keys (:channels a)))
        b-channels (set (keys (:channels b)))
        channels (if (= :complete-union comparison)
                   (set/union a-channels b-channels)
                   (set/intersection a-channels b-channels))]
    (if (empty? channels)
      {:status :unrankable :reason :no-shared-current-channel
       :left (:id a) :right (:id b) :shared-channels []}
      (let [value (fn [candidate channel]
                    (if-let [observation (get-in candidate [:channels channel])]
                      (:value observation)
                      missing-channel-cost))
            ga (reduce + (map #(value a %) channels))
            gb (reduce + (map #(value b %) channels))]
        {:status :ranked :left (:id a) :right (:id b)
         :comparison comparison
         :compared-channels (vec (sort channels))
         :shared-channels (vec (sort (set/intersection a-channels b-channels)))
         :missing-channel-cost (when (= :complete-union comparison)
                                 missing-channel-cost)
         :missing-left (vec (sort (set/difference channels a-channels)))
         :missing-right (vec (sort (set/difference channels b-channels)))
         :g-left ga :g-right gb
         :winner (cond (< ga gb) (:id a) (> ga gb) (:id b) :else :tie)
         :epistemic-value-nats 0.0}))))

(defn select
  "Select from pipeline nodes using normalized preference-cost channels.

  Lower task-state cost is preferred.  Candidates with explicit infeasibility
  are excluded; `:unknown` feasibility is retained as unknown support rather
  than converted to failure. By default selection requires one candidate to
  beat every alternative on current G channels. Equal minima are a
  discrimination failure: identity order is not evidence and may not select
  a policy."
  [{:keys [snapshot candidates comparison missing-channel-cost tie-breaker]
    :or {comparison :shared-only}}]
  (let [snapshot-errors* (snapshot-errors snapshot)]
    (cond
      (seq snapshot-errors*)
      (refusal :pipeline-snapshot-invalid {:errors snapshot-errors*})

      (not (vector? candidates))
      (refusal :candidate-field-invalid {:expected :vector})

      (and (= :complete-union comparison)
           (not (and (number? missing-channel-cost)
                     (<= 0.0 (double missing-channel-cost) 1.0))))
      (refusal :missing-channel-cost-required
               {:comparison comparison :value missing-channel-cost})

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
            algorithms (filterv #(= :algorithm (:kind %)) candidates)
            infeasible (filterv #(= :infeasible
                                    (get-in % [:support :automated-feasibility]))
                                candidates)
            ;; A verified injury changes the support, rather than merely adding
            ;; another task to an ordinary comparison.  This is the A arm of
            ;; META: ordinary work cannot outrank repair while the machine is
            ;; known unable to complete a reviewable click.
            admitted (filterv #(and (not= :infeasible
                                          (get-in % [:support :automated-feasibility]))
                                    (or (empty? algorithms)
                                        (= :algorithm (:kind %))))
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
                             (pairwise (nth admitted i) (nth admitted j)
                                       comparison missing-channel-cost)))
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
                       {:undefeated (mapv :id winners)
                        :pairwise pairs
                        :requested-tie-breaker tie-breaker
                        :selection-discrimination
                        {:verdict :fail
                         :reason :equal-G
                         :tie-count (count winners)
                         :contract {:module "DarkTower.WarMachine.Holes"
                                    :definition "selectionDiscriminates"
                                    :lean-rule "chosenByTiebreak iff 1 < tieCount"}}
                        :absent-terms (:unsupported-by-kind coverage)
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
