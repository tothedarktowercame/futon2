(ns futon2.aif.wm.family-selection
  "Score target policy families jointly through the existing cascade scorer
  and selector. Failed families remain counted evidence and never contribute a
  placeholder policy. Co-application cascades currently have no stable habit
  identity, so each ranked entry records the selector's existing whole-menu
  neutral fallback explicitly."
  (:require [futon2.aif.cascade-shape-g :as shape-g]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.policy :as policy]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.aif.reading-interpretation-debt :as debt]
            [futon2.aif.target-policy-family :as target-family]
            [futon2.aif.target-reading-registry :as reading-registry]))

(identity/register! *ns* *file*)

(def neutral-co-apply-habit
  {:source :neutral-fallback
   :reason :missing-policy-identity
   :scope :whole-menu})

(defn- annotate-failure [target failure]
  (assoc failure :target-id (or (:target-id failure) target)))

(defn- reading-action
  "The scored policy's action, carrying its history key and the exact
  interpretation it owes, both bound to its identity."
  [policy score]
  (let [action (:candidate score)
        action (assoc action :policy-key (prefix-admission/reading-policy-key action))]
    (assoc action :interpretation-debt
           (debt/debt action (:fit score) (:reading-pin policy)))))

(defn- scored->ranked [policy score rank]
  (let [entry (first (:scorer-result score))
        computed-f (:computed-f score)
        g-terms {:risk (:risk score)
                 :ambiguity (:ambiguity score)
                 :expected-information-gain (:information-gain score)
                 :combination :risk-plus-ambiguity-minus-information-gain
                 :units :nats}
        certificate (-> (:certificate entry)
                        (assoc :f computed-f)
                        (assoc :g-terms (merge (get-in entry [:certificate :g-terms])
                                              g-terms)))]
    (-> entry
        (assoc :action (reading-action policy score)
               :cascade-id (:policy-id policy)
               :policy-id (:policy-id policy)
               :policy-kind (:kind policy)
               :target-id (:target policy)
               :rank rank
               :controller-score (:g score)
               :G-efe (:g score)
               :G-cascade (:g score)
               :f (:f score)
               :f-status (:f-status score)
               :computed-f computed-f
               :habit 1
               :habit-provenance neutral-co-apply-habit
               :transition-kernel :co-application-frontier-theta-v1
               :certificate certificate))))

(defn- policy-summary [policy score]
  {:policy-id (:policy-id policy)
   ;; recorded apart from the action, so enactment can check the action's
   ;; debt carrier against what the decision saw
   :interpretation-owed (get-in score [:fit :interpretation-owed])
   :interpretation-debt-digest (:digest (:interpretation-debt (reading-action policy score)))
   :kind (:kind policy)
   :F (:f score)
   :G (:g score)
   :g-terms {:risk (:risk score)
             :ambiguity (:ambiguity score)
             :information-gain (:information-gain score)
             :combination :risk-plus-ambiguity-minus-information-gain}
   :kernel :co-application-frontier-theta-v1
   :habit {:value 1 :status :declared-neutral
           :provenance neutral-co-apply-habit}})

(defn- failed-field-family [target failure]
  {:status :failed :target-id target :policies []
   :reported-count 0 :distinct-count 0
   :failures [(assoc failure :target-id target)] :failure-count 1})

(defn- family-coverage-kind [family]
  (let [kinds (set (map :kind (:failures family)))]
    (cond
      (kinds :graph-path-not-supplied) :graph-refused
      (kinds :graph-pin-mismatch) :graph-refused
      (kinds :graph-pin-missing) :graph-refused
      (kinds :graph-unreadable) :graph-refused
      (kinds :graph-without-pattern-ids) :graph-refused
      (kinds :graph-endpoint-outside-pattern-ids) :graph-refused
      (kinds :target-source-path-absent) :source-path-absent
      (kinds :target-item-line-absent) :source-path-absent
      (kinds :target-source-conflict) :source-path-absent
      (kinds :target-source-kind-unsupported) :source-kind-unsupported
      (kinds :target-head-template-only) :head-template-only
      (kinds :target-source-unreadable) :source-unreadable
      (kinds :stale-target-reading) :stale
      (kinds :no-current-target-reading) :absent
      :else :current)))

(defn- field-coverage [families]
  (let [counts (frequencies (map family-coverage-kind families))]
    {:targets (count families)
     :current (get counts :current 0)
     :stale (get counts :stale 0)
     :absent (get counts :absent 0)
     :source-path-absent (get counts :source-path-absent 0)
     :source-unreadable (get counts :source-unreadable 0)
     :source-kind-unsupported (get counts :source-kind-unsupported 0)
     :head-template-only (get counts :head-template-only 0)
     :graph-refused (get counts :graph-refused 0)}))

(defn families-for-field
  "Form one policy-family result per target source row, in field order.
  The pinned graph is loaded once before any source or reading lookup; with
  no graph path every target fails :graph-path-not-supplied."
  [{:keys [target-sources reading-root graph-path retraction]}]
  (let [loaded (if (some? graph-path)
                 (graph-pin/load-pinned graph-path)
                 {:status :refused :kind :graph-path-not-supplied})
        graph-ok? (= :loaded (:status loaded))
        graph-summary (if graph-ok?
                        (select-keys loaded [:status :pin])
                        (dissoc loaded :graph :pin))
        graph-failure (dissoc loaded :status :graph :pin)
        families
        (mapv
         (fn [{:keys [target-id source-kind source-path source-absent]}]
           (cond
             (not graph-ok?)
             (failed-field-family target-id graph-failure)

             source-absent
             (failed-field-family target-id {:kind source-absent})

             (not (contains? #{nil :head} source-kind))
             (failed-field-family target-id {:kind :target-source-kind-unsupported
                                             :source-kind source-kind})

             :else
             (try
               (let [digest (reading-registry/excerpt-digest source-path)]
                 (if (map? digest)
                   (failed-field-family target-id (dissoc digest :status))
                   (let [reading (reading-registry/current-reading
                                  reading-root target-id digest)]
                     (target-family/policy-family
                      {:reading reading :graph (:graph loaded)
                       :retraction retraction}))))
               (catch Exception e
                 (failed-field-family
                  target-id {:kind :target-source-unreadable
                             :source-path source-path
                             :message (ex-message e)})))))
         target-sources)]
    {:graph graph-summary
     :families families
     :coverage (field-coverage families)}))

(defn field-census
  "The recordable census of FIELD (families-for-field's result): the graph pin
  or its refusal, the coverage counts, and per target its status, counts,
  stable policy ids and kinds, every failure and the family provenance.
  Policy bodies stay in FIELD; nothing here is scored or selected."
  [field]
  {:schema :wm/target-policy-field-census-v1
   :graph (:graph field)
   :coverage (:coverage field)
   :families (mapv (fn [family]
                     (cond-> (select-keys family [:target-id :status :reported-count
                                                  :distinct-count :failures
                                                  :failure-count])
                       true (assoc :policies (mapv #(select-keys % [:policy-id :kind])
                                                   (:policies family)))
                       (:provenance family)
                       (assoc :provenance (:provenance family))))
                   (:families field))})

(defn select-over-families
  "Score every policy in each computed family, then call
  policy/select-action-cascades once over their union. Failed families add
  counted failures and no candidates. With no computed policies the result is
  a typed abstention and the selector is not called on an empty action set.
  OPTS :attach-f, when given, receives the ranked union and returns it with
  each entry's F carrier (:f-prefix) attached, before the one selector call;
  it is removed from the options the selector sees."
  [families {:keys [attach-f] :as opts}]
  (let [rows
        (mapv
         (fn [family]
           (let [target (:target-id family)
                 ready? (= :computed (:status family))
                 reading-pin (select-keys (:provenance family) [:analysis-digest :excerpt-digest])
                 attempted (if ready?
                             (mapv (fn [p] [(assoc p :reading-pin reading-pin)
                                            (shape-g/score-policy p)])
                                   (:policies family))
                             [])
                 scored (filterv #(= :computed (:status (second %))) attempted)
                 score-failures
                 (mapv (fn [[p score]]
                         (annotate-failure
                          target (merge {:kind (or (:kind score) :policy-scoring-refused)
                                        :policy-id (:policy-id p)}
                                        (select-keys score [:cycle :reason :units :roots
                                                            :bound :limit]))))
                       (remove #(= :computed (:status (second %))) attempted))
                 failures (into (mapv #(annotate-failure target %) (:failures family))
                                score-failures)]
             {:target-id target
              :distinct-count (:distinct-count family)
              :scored scored
              :policies (mapv (fn [[p score]] (policy-summary p score)) scored)
              :failures failures
              :failure-count (count failures)}))
         families)
        scored (vec (mapcat :scored rows))
        ranked (mapv (fn [rank [p score]] (scored->ranked p score rank))
                     (range 1 (inc (count scored))) scored)
        ranked (if attach-f (attach-f ranked) ranked)
        opts (dissoc opts :attach-f)
        failures (vec (mapcat :failures rows))
        summaries (mapv #(dissoc % :scored) rows)]
    (if (seq ranked)
      {:status :selected
       :decision (policy/select-action-cascades ranked opts)
       :ranked ranked
       :target-policy-families summaries
       :failures failures
       :failure-count (count failures)}
      {:status :abstained
       :decision {:status :abstained :kind :no-computed-policy-family}
       :ranked []
       :target-policy-families summaries
       :failures failures
       :failure-count (count failures)})))
