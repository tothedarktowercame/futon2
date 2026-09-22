(ns futon2.aif.cascade-observation-scoring
  "Bounded cascade scorer for an explicitly declared observation model.
   Called by efe/rank-cascade-actions; no new selection law and no actuator.
   G is the prior-predictive horizon sum. F explains an explicitly matched
   observation at that horizon. The conditioned belief is retained for the
   next prediction, not substituted into the prediction being evaluated."
  (:require [clojure.set :as set]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.observation-model :as om]))

(def max-horizon 10)
(def max-candidates 16)

(defn- subsets [tokens]
  (reduce (fn [ss t] (into ss (map #(conj % t) ss))) [#{}] (sort-by pr-str tokens)))

(defn- checked [result]
  (when (and (map? result) (contains? result :status)
             (not= :computed (:status result)))
    (throw (ex-info "observation route refused" result)))
  result)

(defn- candidate-tokens [candidates]
  (into #{} (mapcat (fn [p]
                     (concat (:produces p) (get-in p [:guard :needs])
                             (get-in p [:guard :forbids])
                             (mapcat :present (get-in p [:guard :clauses]))
                             (mapcat :absent (get-in p [:guard :clauses])))))
        (mapcat :precedence candidates)))

(defn- validate-inputs! [q0 candidates opts]
  (let [{:keys [observation-model horizon-steps prediction-context cascade-spec]} opts
        universe (:universe observation-model)]
    (om/validate! observation-model)
    (when-not (and (pos-int? horizon-steps) (<= horizon-steps max-horizon))
      (om/refuse! :invalid-bounded-horizon {:limit max-horizon}))
    (when-not (and (some? (:occurrence-id prediction-context))
                   (= horizon-steps (:tau prediction-context)))
      (om/refuse! :missing-prediction-context {}))
    (when-not (and (vector? candidates)
                   (<= 1 (count candidates)
                       ;; The class-emission model scores candidates
                       ;; independently over at most five classes -- the
                       ;; enumeration cap guards powerset cost that this
                       ;; model does not pay (PROOF-wm-works 1.3 build 2/3).
                       (if (= :class-emission (:kind observation-model)) Long/MAX_VALUE max-candidates))
                   (every? #(and (= :cascade-candidate (:kind %))
                                 (some? (:id %)) (vector? (:precedence %))) candidates)
                   ;; Id uniqueness guards the token path's id-keyed posterior
                   ;; record; joint families legitimately reuse :C1/:C2 per
                   ;; target, and the class path keys by the full candidate.
                   (or (= :class-emission (:kind observation-model))
                       (= (count candidates) (count (set (map :id candidates))))))
      (om/refuse! :invalid-bounded-candidates
                  {:limit (when-not (= :class-emission (:kind observation-model)) max-candidates)}))
    (when-not (and (map? q0) (seq q0) (m/normalized-exact? q0)
                   (or (= :class-emission (:kind observation-model))
                       (every? #(and (set? %) (set/subset? % universe)) (keys q0))))
      (om/refuse! :invalid-state-belief {}))
    (when-not (or (= :class-emission (:kind observation-model))
                  (set/subset? (candidate-tokens candidates) universe))
      (om/refuse! :candidate-outside-observation-universe {}))
    (when-not (and (map? cascade-spec)
                   (every? #(set? (get cascade-spec %)) [:want :evidence :zeroed])
                   (every? set? (:zeroed cascade-spec))
                   ;; The want/evidence/zeroed subset-of-universe check is
                   ;; the token preference's domain rule; the class model's
                   ;; preference lives over classes, so only the shape holds.
                   (or (= :class-emission (:kind observation-model))
                       (set/subset? (set/union (:want cascade-spec) (:evidence cascade-spec)
                                               (into #{} cat (:zeroed cascade-spec))) universe)))
      (om/refuse! :invalid-observation-preference {}))
    ;; Tempering a coupled row requires its own normalization, not a map of
    ;; tempered marginal rates. Never silently ignore the production option.
    (when (or (contains? opts :adjudication-rates)
              (and (contains? opts :zeta) (not= 1 (:zeta opts))))
      (om/refuse! :conflicting-observation-options {}))))

(defn- score-candidate [q0 candidate opts preference]
  (let [{:keys [observation-model horizon-steps observation prediction-context]} opts
        steps (loop [tau 1 q q0 result []]
                (if (> tau horizon-steps)
                  result
                  (let [evaluated (m/rollout-evaluation (constantly (:precedence candidate)) q 1)
                        next-q (checked (:belief evaluated))
                        score (checked (om/query observation-model
                                                 {:op :score :belief next-q :preference (get-in preference [tau :probabilities])}))]
                    (recur (inc tau) next-q
                           (conj result (assoc score :tau tau :belief next-q
                                               :node-evaluation (assoc (first (:evaluations evaluated)) :tau tau)))))))
        predicted (:belief (peek steps))
        conditioned (om/query observation-model {:op :condition :belief predicted
                                                 :observation observation :context prediction-context})
        g (reduce + (map :g steps))
        entry {:action candidate :cascade true :cascade-id (:id candidate)
               :horizon-steps horizon-steps :controller-score g :G-efe g :G-cascade g
               :observation-model observation-model
               :prediction {:context prediction-context :initial-belief q0 :belief predicted}
               :inference conditioned
               :certificate {:schema :wm/bounded-observation-score-v1
                             :evaluation :exact-enumeration
                             :observation-model observation-model
                             :scope :synthetic-bounded-replay
                             :node-evaluations (mapv :node-evaluation steps)
                             :steps steps
                             :c {:form :step-indexed :schedule (get-in opts [:cascade-spec :c-schedule])
                                 :steps (mapv (fn [step] {:tau (:tau step)
                                                         :distribution (get-in preference [(:tau step) :distribution])}) steps)}
                             :rates-provenance {:source :observation-model/query
                                                :model observation-model}
                             :f (assoc conditioned :value (:f conditioned))}}]
    (if (= :computed (:status conditioned))
      (assoc entry :f (:f conditioned) :posterior (:posterior conditioned))
      entry)))

(defn rank-cascade-actions
  "Same arguments as efe's cascade scorer. Presence of :observation-model
   selects this route, including explicit nil (which refuses).
   A missing/impossible observation refuses the family with all per-candidate
   results retained. No candidate is assigned neutral F or silently dropped."
  [state candidates opts]
  (let [model (:observation-model opts)]
    (try
      (validate-inputs! (:cascade-belief state) candidates opts)
      (let [preference (if (= :class-emission (:kind model))
                         ;; PROOF-wm-works 1.3 build 2/3: the class model
                         ;; carries its own per-tau class preference (Joe's
                         ;; ruling at the horizon, unit mass on
                         ;; :ending/not-yet-evaluated before it); classes are
                         ;; NOT routed through preference-member, which is the
                         ;; token-subset construction bound to
                         ;; TokenPreference.preference and stays untouched.
                         (into {} (for [tau (range 1 (inc (:horizon-steps opts)))]
                                    [tau {:probabilities (get-in model [:class-preference tau])}]))
                         (into {} (for [tau (range 1 (inc (:horizon-steps opts)))]
                                    (let [member (checked (m/preference-member (:cascade-spec opts) (:universe model)
                                                                              (:horizon-steps opts) tau))
                                          log-p (m/member-log-probability member)]
                                      [tau {:distribution member
                                            :probabilities (into {} (map (fn [o] [o (Math/exp (log-p o))]))
                                                                 (subsets (:universe model)))}]))))
            entries (mapv #(score-candidate (:cascade-belief state) % opts preference) candidates)
            failures (filterv #(not= :computed (get-in % [:inference :status])) entries)]
        (if (seq failures)
          {:status (if (some #(= :missing (get-in % [:inference :status])) failures)
                     :missing :contradiction)
           :kind :observation-family-not-selectable :model model :candidates entries
           :failures (mapv (fn [e] {:cascade-id (:cascade-id e) :inference (:inference e)}) failures)}
          (let [sorted (sort-by :controller-score entries)
                rank-of (zipmap (distinct (map :controller-score sorted)) (range 1 (inc (count sorted))))]
            (with-meta
              (mapv (fn [e]
                      (let [ties (filter #(= (:controller-score e) (:controller-score %)) sorted)]
                        (cond-> (assoc e :rank (rank-of (:controller-score e)))
                          (< 1 (count ties)) (assoc :g-tie (mapv :cascade-id ties))))) sorted)
              {:cascade-scoring {:model model :scope :synthetic-bounded-replay
                                 :horizon-steps (:horizon-steps opts)}}))))
      (catch clojure.lang.ExceptionInfo e
        (merge {:model model} (ex-data e))))))
