(ns futon2.aif.wm.family-selection
  "Score target policy families jointly through the existing cascade scorer
  and selector. Failed families remain counted evidence and never contribute a
  placeholder policy. Co-application cascades currently have no stable habit
  identity, so each ranked entry records the selector's existing whole-menu
  neutral fallback explicitly."
  (:require [futon2.aif.cascade-shape-g :as shape-g]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.policy :as policy]))

(identity/register! *ns* *file*)

(def neutral-co-apply-habit
  {:source :neutral-fallback
   :reason :missing-policy-identity
   :scope :whole-menu})

(defn- annotate-failure [target failure]
  (assoc failure :target-id (or (:target-id failure) target)))

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
        (assoc :action (:candidate score)
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

(defn select-over-families
  "Score every policy in each computed family, then call
  policy/select-action-cascades once over their union. Failed families add
  counted failures and no candidates. With no computed policies the result is
  a typed abstention and the selector is not called on an empty action set."
  [families opts]
  (let [rows
        (mapv
         (fn [family]
           (let [target (:target-id family)
                 ready? (= :computed (:status family))
                 scored (if ready?
                          (mapv (fn [p] [p (shape-g/score-policy p)]) (:policies family))
                          [])
                 failures (mapv #(annotate-failure target %) (:failures family))]
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
