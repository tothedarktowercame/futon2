(ns futon2.aif.cascade-observation-scoring
  "Bounded cascade scorer for an explicitly declared observation model.
   Called by efe/rank-cascade-actions; no new selection law and no actuator.
   G is the prior-predictive horizon sum. F explains an explicitly matched
   observation at that horizon. The conditioned belief is retained for the
   next prediction, not substituted into the prediction being evaluated."
  (:require [clojure.set :as set]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.observation-model :as om]
            [futon2.aif.parameter-novelty :as novelty]))

(def max-horizon 10)
(def max-candidates 16)

(defn- subsets [tokens]
  (reduce (fn [ss t] (into ss (map #(conj % t) ss))) [#{}] (sort-by pr-str tokens)))

(defn- checked [result]
  (when (and (map? result) (contains? result :status)
             (not= :computed (:status result)))
    (throw (ex-info "observation route refused" result)))
  result)

(defn- precedence-patterns
  "Return the pattern maps consumed by either supported transition carrier.
  The co-application carrier itself still travels unchanged to rollout."
  [precedence]
  (cond
    (vector? precedence) precedence
    (and (map? precedence)
         (= #{:co-apply} (set (keys precedence)))
         (vector? (get-in precedence [:co-apply :units]))
         (vector? (get-in precedence [:co-apply :descent]))
         (map? (get-in precedence [:co-apply :patterns])))
    (let [{:keys [units patterns]} (:co-apply precedence)
          values (mapv patterns units)]
      (when (every? map? values) values))
    :else nil))

(defn- candidate-tokens [candidates]
  (into #{} (mapcat (fn [p]
                     (concat (:produces p) (get-in p [:guard :needs])
                             (get-in p [:guard :forbids])
                             (mapcat :present (get-in p [:guard :clauses]))
                             (mapcat :absent (get-in p [:guard :clauses])))))
        (mapcat #(precedence-patterns (:precedence %)) candidates)))

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
                       (if (contains? #{:class-emission :progress-count} (:kind observation-model))
                         Long/MAX_VALUE max-candidates))
                   (every? #(let [precedence (:precedence %)
                                  patterns (precedence-patterns precedence)]
                              (and (= :cascade-candidate (:kind %))
                                   (some? (:id %))
                                   (some? patterns)
                                   (or (vector? precedence)
                                       (= (count patterns)
                                          (count (get-in precedence [:co-apply :units]))))))
                           candidates)
                   ;; Id uniqueness guards the token path's id-keyed posterior
                   ;; record; joint families legitimately reuse :C1/:C2 per
                   ;; target, and the class path keys by the full candidate.
                   (or (contains? #{:class-emission :progress-count} (:kind observation-model))
                       (= (count candidates) (count (set (map :id candidates))))))
      (om/refuse! :invalid-bounded-candidates
                  {:limit (when-not (contains? #{:class-emission :progress-count}
                                               (:kind observation-model)) max-candidates)}))
    (when (contains? #{:class-emission :progress-count} (:kind observation-model))
      ;; The class scorer still rolls B through the token transition model,
      ;; so B's token domain holds: set-shaped q0 states, q0 support and
      ;; candidate tokens inside the declared universe, and the model's
      ;; horizon equal to the rollout horizon (handoff A, codex-20).
      (when-not (= (:horizon observation-model) horizon-steps)
        (om/refuse! :class-horizon-mismatch
                    {:model (:horizon observation-model) :rollout horizon-steps})))
    (when-not (and (map? q0) (seq q0) (m/normalized-exact? q0)
                   (every? #(and (set? %) (set/subset? % universe)) (keys q0)))
      (om/refuse! :invalid-state-belief
                   {:non-set-states (count (remove set? (keys q0)))
                    :outside-states (vec (sort-by pr-str (for [st (keys q0)
                                                               :when (and (set? st)
                                                                          (seq (set/difference st universe)))]
                                                           {:state st
                                                            :outside (vec (sort-by pr-str (set/difference st universe)))})))}))
    (when-not (set/subset? (candidate-tokens candidates) universe)
      (om/refuse! :candidate-outside-observation-universe {}))
    (when-not (and (map? cascade-spec)
                   (every? #(set? (get cascade-spec %)) [:want :evidence :zeroed])
                   (every? set? (:zeroed cascade-spec))
                   ;; The want/evidence/zeroed subset-of-universe check is
                   ;; the token preference's domain rule; the class model's
                   ;; preference lives over classes, so only the shape holds.
                   (or (contains? #{:class-emission :progress-count} (:kind observation-model))
                       (set/subset? (set/union (:want cascade-spec) (:evidence cascade-spec)
                                               (into #{} cat (:zeroed cascade-spec))) universe)))
      (om/refuse! :invalid-observation-preference {}))
    ;; Tempering a coupled row requires its own normalization, not a map of
    ;; tempered marginal rates. Never silently ignore the production option.
    (when (or (contains? opts :adjudication-rates)
              (and (contains? opts :zeta) (not= 1 (:zeta opts))))
      (om/refuse! :conflicting-observation-options {}))))

(defn candidate-information-gain
  "Expected information from one Bernoulli observation of each distinct
  pattern parameter. A recorded-trials theta carries its Beta posterior;
  an unexplored pattern carries the declared Jeffreys Beta(1/2,1/2) prior.
  This is subtracted in G: information expected from acting favours acting."
  [candidate]
  (reduce + 0.0
          (for [[_ p] (into {} (map (juxt #(or (:pattern-id %) (:id %)) identity)
                                      (precedence-patterns (:precedence candidate))))
                :let [r (:theta-record p)
                      [a b] (if (= :recorded-trials (:status r))
                              [(+ 1/2 (:successes r))
                               (+ 1/2 (- (:trials-count r) (:successes r)))]
                              [1/2 1/2])]]
            (:nats (novelty/beta-information a b)))))

(defn- score-candidate [q0 candidate opts preference]
  (let [{:keys [observation-model horizon-steps observation prediction-context
                upstream-initialization-conditioning]} opts
        steps (loop [tau 1 q q0 result []]
                (if (> tau horizon-steps)
                  result
                  (let [evaluated (m/rollout-evaluation (constantly (:precedence candidate)) q 1)
                        next-q (checked (:belief evaluated))
                        score (checked (om/query observation-model
                                                 ;; PROOF-wm-works 1.3 handoff A:
                                                 ;; the actual step tau and the
                                                 ;; candidate's own target (its
                                                 ;; attributable ending) travel
                                                 ;; with every score query.
                                                 {:op :score :belief next-q :tau tau
                                                  :target (:target candidate)
                                                  :preference (get-in preference [tau :probabilities])}))]
                    (recur (inc tau) next-q
                           (conj result (assoc score :tau tau :belief next-q
                                               :node-evaluation (assoc (first (:evaluations evaluated)) :tau tau)))))))
        predicted (:belief (peek steps))
        conditioned (om/query observation-model {:op :condition :belief predicted
                                                 :observation observation :context prediction-context})
        raw-risk (reduce + 0.0 (map :risk steps))
        raw-ambiguity (reduce + 0.0 (map :ambiguity steps))
        ;; State EIG is supplied by the observation model.  Parameter
        ;; novelty remains an optional, separate codex-33 seam.
        raw-state-information (reduce + 0.0 (map #(double (or (:information-gain %) 0.0)) steps))
        raw-parameter-information (if (= :beta-pattern (:parameter-information-mode opts))
                                    (candidate-information-gain candidate) 0.0)
        raw-information (+ raw-state-information raw-parameter-information)
        normalize? (= :per-step-capacity-and-pattern (:g-normalization opts))
        pattern-count (max 1 (count (set (map #(or (:pattern-id %) (:id %))
                                                (precedence-patterns (:precedence candidate))))))
        outcome-count (max 2 (count (get-in preference [1 :probabilities])))
        observation-capacity (Math/log (double outcome-count))
        risk (if normalize? (/ raw-risk horizon-steps observation-capacity) raw-risk)
        ambiguity (if normalize? (/ raw-ambiguity horizon-steps) raw-ambiguity)
        information-gain (if normalize? (/ raw-information pattern-count) raw-information)
        g (- (+ risk ambiguity) information-gain)
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
                             :consumed-g
                             (cond->
                              {:C {:form :step-indexed
                                   :schedule (get-in opts [:cascade-spec :c-schedule])
                                   :steps (mapv (fn [step]
                                                  {:tau (:tau step)
                                                   :distribution
                                                   (get-in preference [(:tau step) :probabilities])})
                                                steps)}
                               :D q0}
                               (= :class-emission (:kind observation-model))
                               (assoc :A observation-model)
                               (contains? opts :upstream-initialization-conditioning)
                               (assoc :Q {:form :upstream-initialization-conditioning
                                          :initial-belief q0
                                          :steps (mapv #(select-keys % [:tau :belief]) steps)
                                          :conditioning upstream-initialization-conditioning}))
                             :c {:form :step-indexed :schedule (get-in opts [:cascade-spec :c-schedule])
                                 :steps (mapv (fn [step] {:tau (:tau step)
                                                         :distribution (get-in preference [(:tau step) :distribution])}) steps)}
                             :c-source (or (get-in opts [:cascade-spec :c])
                                           {:absent :no-c-source-in-cascade-spec})
                             :g-terms {:risk risk :ambiguity ambiguity
                                       :expected-information-gain information-gain
                                       :state-information-gain (if normalize?
                                                                 (/ raw-state-information horizon-steps)
                                                                 raw-state-information)
                                       :parameter-information-gain (if normalize?
                                                                      (/ raw-parameter-information pattern-count)
                                                                      raw-parameter-information)
                                       :combination :risk-plus-ambiguity-minus-information-gain
                                       :normalization (if normalize?
                                                        {:risk :per-horizon-step-and-log-outcome-support
                                                         :ambiguity :per-horizon-step
                                                         :information :per-distinct-pattern}
                                                        :none)
                                       :raw {:risk raw-risk :ambiguity raw-ambiguity
                                             :expected-information-gain raw-information}
                                       :units :nats}
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
      (let [preference (if (contains? #{:class-emission :progress-count} (:kind model))
                         ;; PROOF-wm-works 1.3 build 2/3: the class model
                         ;; carries its own per-tau class preference (Joe's
                         ;; ruling at the horizon, unit mass on
                         ;; :ending/not-yet-evaluated before it); classes are
                         ;; NOT routed through preference-member, which is the
                         ;; token-subset construction bound to
                         ;; TokenPreference.preference and stays untouched.
                         (into {} (for [tau (range 1 (inc (:horizon-steps opts)))]
                                    [tau {:probabilities (get-in model [(if (= :class-emission (:kind model))
                                                                         :class-preference
                                                                         :progress-preference)
                                                                       tau])}]))
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
              {:cascade-scoring (cond-> {:model model :scope :synthetic-bounded-replay
                                         :horizon-steps (:horizon-steps opts)}
                                  ;; PROOF-wm-works ⟨1⟩4/⟨1⟩5 (claude-5
                                  ;; handoff): the class path's ranked meta
                                  ;; carries a :precision-model describing
                                  ;; the model ACTUALLY consumed — never a
                                  ;; token precision model, never fields not
                                  ;; consumed. Fields precision-carry needs
                                  ;; that the class path has no honest value
                                  ;; for stay typed-absent with a reason.
                                  (= :class-emission (:kind model))
                                  (assoc :precision-model
                                         {:kind :class-emission
                                          :q0 (:cascade-belief state)
                                          :horizon (:horizon-steps opts)
                                          :class-preference (get-in model [:class-preference (:horizon-steps opts)])
                                          :provenance (:provenance model)
                                          ;; the token model's :rates /
                                          ;; :preference-spec have no class
                                          ;; counterpart — typed absent
                                          :rates {:status :absent
                                                  :reason :class-emission-has-no-token-rates}
                                          :preference-spec {:status :absent
                                                            :reason :class-preference-not-a-token-spec}}))}))))
      (catch clojure.lang.ExceptionInfo e
        (merge {:model model} (ex-data e))))))
