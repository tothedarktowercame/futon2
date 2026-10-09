(ns futon2.aif.g-term-decomposition
  "Consumed-value census for AGG-single-kl-reduction. Evidence, never a gate.
   Degenerate means the named census reduction, not an invalid AIF value.
   In particular C means constant over the scored horizon, not uniform;
   E means the enumerated habit vector is UNIFORM (the prior carries no
   information beyond enumeration) — a property of the whole vector,
   evaluated identically for every policy. The pre-2026-09-20 form
   compared one policy's mass to exactly 1, which cannot fire with more
   than one candidate: a verdict that could not come out the other way
   (stop-the-line finding, STOP-THE-LINE-2026-09-20.md)."
  (:require [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.conditioned-trajectory :as trajectory]))

(def terms [:A :C :D :E :F :Q])

(defn- same-belief? [a b]
  (and (model/normalized-exact? a) (model/normalized-exact? b)
       (= (into {} (remove (comp zero? val)) a)
          (into {} (remove (comp zero? val)) b))))

(defn- q-evidence? [value]
  (and (vector? (:observation-updates value))
       (every? (fn [update]
                 (case (:status update)
                   :value (and (true? (:consumed update))
                               (model/normalized-exact? (:predicted-belief update))
                               (same-belief? (:initial-belief value) (:post-belief update))
                               (boolean? (:vacuous update))
                               (= (:vacuous update)
                                  (same-belief? (:predicted-belief update) (:post-belief update)))
                               (or (not (:vacuous update))
                                   (and (= :licensed (get-in update [:vacuity-license :status]))
                                        (= (select-keys (:vacuity-license update)
                                                        [:status :theorem :support :constant-likelihood :predicted-total])
                                           (select-keys
                                            (trajectory/vacuity-license
                                             (:predicted-belief update)
                                             (get-in update [:receipt :calculation :likelihoods]))
                                            [:status :theorem :support :constant-likelihood :predicted-total])))))
                   :refused (false? (:consumed update))
                   false))
               (:observation-updates value))))

(defn- class-a-classification
  "Classify the class-emission rows declared by VALUE.  The model records the
   finite class universe and the terminal class associated with each accepted
   target.  A terminal class may be a keyword (the production point mass) or
   an explicit probability row; the latter keeps stochastic records
   distinguishable.  Missing declarations have no verdict."
  [value]
  (let [required [:universe :horizon :class-universe :acceptance :target-class]
        missing (filterv #(not (contains? value %)) required)]
    (cond
      (and (:policy-target value)
           (not-any? #(= (:policy-target value) (first %)) (:acceptance value)))
      {:status :missing :value value :reason :policy-target-not-in-acceptance}

      (seq missing)
      {:status :missing :value value :reason :class-emission-fields-missing
       :missing-fields missing}

      (not (and (set? (:universe value)) (pos-int? (:horizon value))
                (sequential? (:class-universe value)) (seq (:class-universe value))
                (set? (:acceptance value)) (map? (:target-class value))))
      {:status :missing :value value :reason :invalid-class-emission-shape}

      :else
      (let [classes (set (:class-universe value))
            targets (set (map first (:acceptance value)))
            rows (into {}
                       (for [class classes]
                         [class {class 1}]))
            rows (into rows
                       (for [target targets]
                         (let [emission (get (:target-class value) target)]
                           [target (cond
                                     (keyword? emission) {emission 1}
                                     (map? emission) emission
                                     ;; an undeclared class stays nil and is
                                     ;; reported as an invalid row below
                                     :else emission)])))
            valid-row? (fn [row]
                         (and (map? row) (seq row)
                              (every? #(and (contains? classes (key %))
                                            (number? (val %))
                                            (not (neg? (val %)))) row)
                              (== 1 (reduce + (vals row)))))
            invalid (into {} (remove (comp valid-row? val)) rows)]
        (if (seq invalid)
          {:status :missing :value value :reason :invalid-class-emission-row
           :invalid-rows invalid}
          (let [deterministic? (every? #(= [1] (vec (filter pos? (vals %))))
                                      (vals rows))]
            [deterministic? (if deterministic?
                              :deterministic-class-emission
                              :stochastic-class-emission)]))))))

(defn- a-classification [value]
  (cond
    (and (map? value) (= :class-emission (:kind value)))
    (class-a-classification value)

    (and (map? value) (seq value)
         (every? (fn [[_ cell]]
                   (and (map? cell) (number? (:false-neg cell))
                        (number? (:false-pos cell)))) value))
    (let [identity? (every? #(and (zero? (:false-neg %))
                                  (zero? (:false-pos %))) (vals value))]
      [identity? (if identity? :identity-kernel :non-identity-kernel)])

    :else {:status :missing :value value :reason :unsupported-a-shape}))

(defn- upstream-q-classification
  "Classify from what the producer recorded: the updates, the belief they were
   applied to, and the belief the scorer started from. The census does not
   recompute the producer's work."
  [value]
  (let [initial (:initial-belief value)
        applied (get-in value [:conditioning :applied-to])
        updates (get-in value [:conditioning :observation-updates])]
    (cond
      (not (and (model/normalized-exact? initial)
                (model/normalized-exact? applied)))
      {:status :missing :value value :reason :q-beliefs-not-recorded}

      (not (and (vector? updates) (every? #(contains? % :status) updates)))
      {:status :missing :value value :reason :q-updates-malformed}

      :else
      (let [updated (filter #(= :updated (:status %)) updates)]
        (cond
          (empty? updated) [true :open-loop-no-conditioning]
          (same-belief? initial applied) [true :conditioning-vacuous]
          :else [false :observation-conditioned])))))

(defn verdict
  "Classify a recorded consumed value. Missing evidence has no verdict; it
   must never count as a degenerate value (or a non-degenerate witness).
   :E additionally requires ctx {:all-habits [...]} — the full enumerated
   habit vector. A caller that omits it gets :missing, never a defaulted
   verdict (that omission was exactly the pre-fix facade). :Q requires
   successful consumed updates to distinguish conditioning from prediction;
   unchanged posteriors are still degenerate, and refusals cannot green Q.
   Missing or inconsistent update evidence has no verdict."
  ([term value] (verdict term value nil))
  ([term value ctx]
  (if (or (nil? value) (and (= term :Q) (nil? (:form value)) (not (q-evidence? value)))
          (and (= term :C) (or (not (seq (:steps value)))
                                                        (some #(nil? (:distribution %)) (:steps value)))))
    (if (= term :F)
      ;; F-ABS (PROOF-2 packet 27): an absent F is not a lost value — the
      ;; selection law omits the term (cascade-selection line 112), so the
      ;; record says the term was omitted from the law that ran.
      {:status :absent :value nil :reason :omitted-from-law}
      {:status :missing :value nil :reason :consumed-value-not-recorded})
    (if (and (= term :E) (not (seq (remove nil? (:all-habits ctx)))))
      {:status :missing :value value :reason :habit-vector-not-supplied}
    (let [classification
          (case term
            :A (a-classification value)
            :C (let [distributions (map :distribution (:steps value))
                     constant? (every? #(if (and (contains? (first distributions) :universe)
                                                  (contains? % :universe))
                                           (model/same-preference-distribution? (first distributions) %)
                                           (= (first distributions) %))
                                       (rest distributions))]
                 [constant? (if constant? :constant-across-horizon :varies-across-horizon)])
            :D (let [support (filter (comp pos? val) value)
                     point? (and (= 1 (count support)) (== 1 (val (first support))))]
                 [point? (if point? :point-mass :distributed-belief)])
            :E (let [habits (remove nil? (:all-habits ctx))
                     uniform? (apply == habits)]
                 [uniform? (if uniform? :uniform-habit :informative-habit)])
            :F [(zero? value) (if (zero? value) :zero-consumed-f :nonzero-consumed-f)]
            :Q (case (:form value)
                 nil (let [consumed (filter #(and (= :value (:status %))
                                                  (true? (:consumed %)))
                                            (:observation-updates value))]
                       (cond
                         (empty? consumed) [true :open-loop-no-conditioning]
                         (every? #(same-belief? (:predicted-belief %) (:post-belief %)) consumed)
                         [true :conditioning-vacuous]
                         :else [false :observation-conditioned]))
                 :upstream-initialization-conditioning (upstream-q-classification value)
                 {:status :missing :value value :reason :unsupported-q-form}))]
      (if (map? classification)
        classification
        (let [[degenerate? reason] classification]
          (cond-> {:status :present :value value
                   :verdict (if degenerate? :degenerate :non-degenerate) :reason reason}
            (= term :C) (assoc :C-steps-count (count (:steps value)))))))))))

(defn census
  "Join scoring's consumed A/C/D/Q to selection's consumed E/F, by position
   in the selector's own candidate vector. Computed-but-unattached F is
   retained as provenance only and cannot determine F's verdict."
  [ranked candidates]
  (let [all-habits (mapv :habit candidates)
        policies
        (mapv (fn [entry candidate]
                (let [consumed (get-in entry [:certificate :consumed-g])
                      a (:A consumed)
                      target (get-in candidate [:id :target])
                      ;; Class-emission G consumes only this policy's target
                      ;; (observation-model/class-emission-row). Retain that
                      ;; exact slice for the census; unrelated global rows,
                      ;; including honest :unknown rows declined by scoring,
                      ;; are not observations of this policy.
                      policy-a (if (and (= :class-emission (:kind a)) target)
                                 (assoc a
                                        :policy-target target
                                        :acceptance (set (filter #(= target (first %))
                                                                 (:acceptance a)))
                                        :target-class (select-keys (:target-class a)
                                                                   [target]))
                                 a)
                      values (assoc consumed :A policy-a
                                             :E (:habit candidate) :F (:f candidate))]
                  {:id (:id candidate)
                   :terms (into {} (map (fn [term]
                                          [term (if (= term :E)
                                                  (verdict term (get values term) {:all-habits all-habits})
                                                  (verdict term (get values term)))])
                                        terms))
                   :f-provenance (select-keys candidate [:f-status :reason :computed-f])}))
              ranked candidates)
        complete? (and (seq policies)
                       (every? #(every? (fn [term] (= :present (:status term)))
                                        (vals (:terms %))) policies))]
    {:schema :wm/g-term-decomposition-v1
     :status (if complete? :present :missing)
     :policies policies}))

(defn from-result
  "Read the decision retained by the runner's selection checkpoint. No
   selection (including an operator/repair bypass) is explicit missing data."
  [result]
  (or (get-in result [:checkpoints :selection :judgment :controller-decision
                     :selection-certificate :g-term-decomposition])
      (get-in result [:checkpoints :selection :judgment :decision
                     :selection-certificate :g-term-decomposition])
      {:schema :wm/g-term-decomposition-v1 :status :missing
       :reason :no-recorded-cascade-selection :policies []}))
