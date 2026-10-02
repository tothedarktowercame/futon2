(ns futon2.aif.meta-policy-constructor
  "Pure construction of META policy slots from a verified field and explicit
  canonical adapter evidence. No prediction, preference, EIG, or G values are
  supplied here."
  (:require [futon2.aif.meta-field-observation :as field]))

(def schema :wm/meta-policy-construction-v1)
(def checkable-locator-classes #{:C3 :C4 :C5 :C6 :C8})
(def ordinary-kinds #{:mission :excursion :ticket})
(def stopping-rules
  #{:closed :grounded-progress :typed-blocker :budget-exhausted :debugger-stop})

(defn- refusal [reason details]
  {:schema schema :status :refused :reason reason :details details})

(defn- locator? [x]
  (and (map? x)
       (or (contains? checkable-locator-classes (:class x))
           (and (keyword? (:kind x))
                (or (pos-int? (:line x)) (some? (:id x)))))))

(defn- resource-envelope? [x]
  (and (map? x) (pos-int? (:time-budget-ms x)) (pos-int? (:token-budget x))
       (string? (:author-seat x)) (not (empty? (:author-seat x)))
       (string? (:reviewer-seat x)) (not (empty? (:reviewer-seat x)))))

(defn- next-move [kind adapter]
  (or (:next-move adapter)
      (when (ordinary-kinds kind)
        (when (= :ready (:next-step adapter)) :advance))
      (when (= :algorithm kind) :run-algorithm)))

(defn- permitted-next-move? [kind move]
  (contains? (if (= :algorithm kind)
               #{:advance :unblock :close :repair :run-algorithm}
               #{:advance :unblock :close}) move))

(defn construct
  "Partition every verified field row into a complete slot template or typed
  exclusion. ADAPTERS are explicit current observations keyed by task id; an
  adapter must cite the exact field source and a checkable outcome locator."
  [{:keys [field-observation expected-field-pin resource-envelope adapters]}]
  (let [field-verification
        (field/verify field-observation {:expected-snapshot-pin expected-field-pin})
        adapters (vec (or adapters []))
        adapter-ids (map :id adapters)
        duplicate-adapters (->> adapter-ids frequencies
                                (keep (fn [[id n]] (when (> n 1) id))) vec)
        field-by-id (into {} (map (juxt :id identity)) (:rows field-observation))
        unknown-adapters (vec (remove field-by-id adapter-ids))]
    (cond
      (not= :verified (:status field-verification))
      (refusal :field-not-verified {:verification field-verification})

      (not (resource-envelope? resource-envelope))
      (refusal :resource-envelope-invalid {:resource-envelope resource-envelope})

      (seq duplicate-adapters)
      (refusal :adapter-identities-duplicated {:ids duplicate-adapters})

      (seq unknown-adapters)
      (refusal :adapter-target-not-in-field {:ids unknown-adapters})

      :else
      (let [adapter-by-id (into {} (map (juxt :id identity)) adapters)
            source-mismatches
            (->> adapters
                 (keep (fn [adapter]
                         (let [row (get field-by-id (:id adapter))]
                           (when (not= (:source row) (:source adapter))
                             {:id (:id adapter) :field-source (:source row)
                              :adapter-source (:source adapter)}))))
                 vec)]
        (if (seq source-mismatches)
          (refusal :adapter-source-mismatch {:mismatches source-mismatches})
          (let [partition
                (mapv
                 (fn [row]
                   (let [adapter (get adapter-by-id (:id row))
                         move (next-move (:kind row) adapter)
                         locator (:locator adapter)
                         stopping-rule (or (:stopping-rule adapter) :grounded-progress)]
                     (if (and adapter (locator? locator)
                              (permitted-next-move? (:kind row) move)
                              (contains? stopping-rules stopping-rule))
                       [:constructed
                        {:id (keyword "meta-policy" (:id row))
                         :slots {:task-kind (:kind row) :target (:id row)
                                 :next-move move
                                 :resource-envelope resource-envelope
                                 :evidence-channel {:source (:source row)
                                                    :locator locator}
                                 :stopping-rule stopping-rule}}]
                       [:excluded
                        {:id (:id row) :kind (:kind row) :source (:source row)
                         :reason :evidence-channel-unavailable
                         :evidence {:adapter
                                    (select-keys adapter
                                                 [:adapter :next-step :next-move
                                                  :locator :stopping-rule])}}])))
                 (:rows field-observation))
                templates (mapv second (filter #(= :constructed (first %)) partition))
                exclusions (mapv second (filter #(= :excluded (first %)) partition))]
            {:schema schema :status :constructed
             :field-source-pin expected-field-pin
             :resource-envelope resource-envelope
             :templates templates :exclusions exclusions
             :counts {:field (count (:rows field-observation))
                      :constructed (count templates) :excluded (count exclusions)
                      :constructed-by-kind
                      (into (sorted-map) (frequencies (map #(get-in % [:slots :task-kind]) templates)))
                      :excluded-by-kind
                      (into (sorted-map) (frequencies (map :kind exclusions)))}}))))))

(defn verify
  "Verify construction against independently supplied field, resources, and
  adapter observations. The canonical expected receipt is reconstructed by
  `construct`; receipt-local locator/evidence claims are never authority."
  ([receipt]
   (refusal :external-construction-authority-required
            {:receipt-schema (:schema receipt)}))
  ([receipt {:keys [field-observation expected-field-pin expected-resource-envelope adapters]
             :as authority}]
  (let [adapter-authority? (contains? authority :adapters)
        fv (field/verify field-observation {:expected-snapshot-pin expected-field-pin})
        expected (when adapter-authority?
                   (construct {:field-observation field-observation
                               :expected-field-pin expected-field-pin
                               :resource-envelope expected-resource-envelope
                               :adapters adapters}))
        receipt-fields [:schema :status :field-source-pin :resource-envelope
                        :templates :exclusions :counts]
        rows (:rows field-observation)
        field-by-id (into {} (map (juxt :id identity)) rows)
        represented (concat
                     (map (fn [t] {:id (get-in t [:slots :target])
                                   :kind (get-in t [:slots :task-kind])
                                   :source (get-in t [:slots :evidence-channel :source])
                                   :side :constructed}) (:templates receipt))
                     (map #(assoc (select-keys % [:id :kind :source]) :side :excluded)
                          (:exclusions receipt)))
        ids (map :id represented)
        duplicates (->> ids frequencies (keep (fn [[id n]] (when (> n 1) id))) vec)
        mismatches (->> represented
                        (keep (fn [x]
                                (let [row (get field-by-id (:id x))]
                                  (when (or (nil? row) (not= (:kind row) (:kind x))
                                            (not= (:source row) (:source x)))
                                    {:representation x :field-row row})))) vec)
        expected-ids (set (keys field-by-id))
        template-errors
        (->> (:templates receipt)
             (keep (fn [template]
                     (let [slots (:slots template)
                           kind (:task-kind slots)]
                       (when (or (not= (:id template)
                                      (keyword "meta-policy" (:target slots)))
                                 (not= expected-resource-envelope
                                       (:resource-envelope slots))
                                 (not (permitted-next-move? kind (:next-move slots)))
                                 (not (locator? (get-in slots [:evidence-channel :locator])))
                                 (not (contains? stopping-rules (:stopping-rule slots))))
                         {:id (:id template) :slots slots})))) vec)
        exclusion-errors
        (filterv #(not (and (= :evidence-channel-unavailable (:reason %))
                            (map? (:evidence %))))
                 (:exclusions receipt))
        expected-counts
        {:field (count rows)
         :constructed (count (:templates receipt))
         :excluded (count (:exclusions receipt))
         :constructed-by-kind
         (into (sorted-map)
               (frequencies (map #(get-in % [:slots :task-kind]) (:templates receipt))))
         :excluded-by-kind
         (into (sorted-map) (frequencies (map :kind (:exclusions receipt))))}]
    (cond
      (not adapter-authority?)
      (refusal :external-adapter-authority-required {})
      (not= :verified (:status fv))
      (refusal :field-not-verified {:verification fv})
      (not= :constructed (:status expected))
      (refusal :expected-construction-refused {:construction expected})
      (not= (select-keys expected receipt-fields)
            (select-keys receipt receipt-fields))
      (refusal :construction-does-not-match-authority
               {:expected (select-keys expected receipt-fields)
                :actual (select-keys receipt receipt-fields)})
      (not= schema (:schema receipt))
      (refusal :construction-schema-invalid {:actual (:schema receipt)})
      (not (resource-envelope? expected-resource-envelope))
      (refusal :external-resource-authority-invalid
               {:expected-resource-envelope expected-resource-envelope})
      (not= expected-resource-envelope (:resource-envelope receipt))
      (refusal :construction-resource-mismatch
               {:expected expected-resource-envelope
                :actual (:resource-envelope receipt)})
      (not= expected-counts (:counts receipt))
      (refusal :construction-counts-mismatch
               {:expected expected-counts :actual (:counts receipt)})
      (seq duplicates)
      (refusal :construction-identities-duplicated {:ids duplicates})
      (not= expected-ids (set ids))
      (refusal :construction-coverage-incomplete
               {:missing (vec (sort (remove (set ids) expected-ids)))
                :unknown (vec (sort (remove expected-ids ids)))})
      (seq mismatches)
      (refusal :construction-source-or-kind-mismatch {:mismatches mismatches})
      (seq template-errors)
      (refusal :constructed-template-invalid {:templates template-errors})
      (seq exclusion-errors)
      (refusal :construction-exclusion-invalid {:exclusions exclusion-errors})
      :else {:schema schema :status :verified :field-source-pin expected-field-pin}))))
