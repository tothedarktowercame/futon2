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
  (if (or (nil? value) (and (= term :Q) (not (q-evidence? value)))
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
    (let [[degenerate? reason]
          (case term
            :A (let [identity? (every? #(and (zero? (:false-neg %))
                                             (zero? (:false-pos %))) (vals value))]
                 [identity? (if identity? :identity-kernel :non-identity-kernel)])
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
            :Q (let [consumed (filter #(and (= :value (:status %))
                                            (true? (:consumed %)))
                                     (:observation-updates value))]
                 (cond
                   (empty? consumed) [true :open-loop-no-conditioning]
                   (every? #(same-belief? (:predicted-belief %) (:post-belief %)) consumed)
                   [true :conditioning-vacuous]
                   :else [false :observation-conditioned])))]
      (cond-> {:status :present :value value
       :verdict (if degenerate? :degenerate :non-degenerate) :reason reason}
        (= term :C) (assoc :C-steps-count (count (:steps value)))))))))

(defn census
  "Join scoring's consumed A/C/D/Q to selection's consumed E/F, by position
   in the selector's own candidate vector. Computed-but-unattached F is
   retained as provenance only and cannot determine F's verdict."
  [ranked candidates]
  (let [all-habits (mapv :habit candidates)
        policies
        (mapv (fn [entry candidate]
                (let [values (assoc (get-in entry [:certificate :consumed-g])
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
