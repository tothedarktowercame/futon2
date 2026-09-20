(ns futon2.aif.conditioned-trajectory
  "Evaluator consumption of a stored filtering result, followed by prediction.
   Received observations belong to the executed history, at scoring tau=0;
   candidate futures do not receive copies of that observation. The future
   precedence may vary. This runtime extension is not the fixed-A/B,
   all-observed conditionedTrajectory theorem. The evaluator supplies its
   existing transition function, so this namespace has no model dependency."
  (:require [clojure.set :as set]))

(defn nonzero-mass
  "Canonical sparse representation for extensional distribution comparisons."
  [belief]
  (into {} (remove (comp zero? val)) belief))

(defn- distribution? [belief]
  (and (map? belief) (seq belief)
       (every? #(and (or (integer? %) (ratio? %)) (<= 0 %)) (vals belief))
       (= 1 (reduce +' 0 (vals belief)))))

(defn- same-belief? [a b]
  (and (distribution? a) (distribution? b)
       (= (nonzero-mass a) (nonzero-mass b))))

(defn vacuity-license
  "Instantiate a vacuity theorem from predicted mass and D's actual likelihoods.
   No posterior calculation occurs here. Zero-mass states are outside the
   hypothesis; every positive-mass state must have the same positive likelihood."
  [predicted likelihoods]
  (if-not (distribution? predicted)
    {:status :invalid :kind :invalid-vacuity-prediction}
    (let [support (set (keys (nonzero-mass predicted)))
          values (when (map? likelihoods) (select-keys likelihoods support))
          evidence {:support support :likelihoods values
                    :predicted-total (reduce +' 0 (vals predicted))}]
      (cond
        (not (and (= support (set (keys values)))
                  (every? #(and (or (integer? %) (ratio? %)) (<= 0 % 1))
                          (vals values))))
        (assoc evidence :status :invalid :kind :missing-or-invalid-vacuity-likelihood)

        (not (apply = (vals values)))
        (assoc evidence :status :invalid :kind :nonconstant-vacuity-likelihood)

        (zero? (first (vals values)))
        (assoc evidence :status :invalid :kind :zero-vacuity-likelihood)

        :else
        (assoc evidence :status :licensed :constant-likelihood (first (vals values))
               :theorem (if (= 1 (count support))
                          'DarkTower.WarMachine.BeliefConditionedRollout.exactUpdate_pointMass_vacuous
                          'DarkTower.WarMachine.BeliefConditionedRollout.exactUpdate_vacuous_of_const_likelihood))))))

(defn- fields? [m ks] (and (map? m) (every? #(contains? m %) ks)))
(defn- absent? [m ks] (not-any? #(contains? m %) ks))
(defn- probability? [p]
  (and (or (integer? p) (ratio? p)) (<= 0 p 1)))

(defn- stored-belief? [belief universe]
  (and (distribution? belief) (every? pos? (vals belief))
       (every? #(and (set? %) (set/subset? % universe)) (keys belief))))

(defn- not-applicable? [license]
  (and (= :not-applicable (:status license)) (some? (:reason license))
       (not (contains? license :theorem))))

(defn- calculation-matches? [r calculation status option]
  (and (= status (:status calculation)) (= option (:option calculation))
       (= (:observation-probability r) (:observation-probability calculation))
       (same-belief? (:predicted-belief r) (:predicted-state calculation))))

(defn- reinitialization-matches? [r q0]
  (let [reinit (:reinitialization r)]
    (and (= :wm/observation-contract-v1 (get-in r [:observation-authority :contract]))
         (= :contract-admitted-checkable-observation (:authority reinit))
         (= (select-keys r [:occurrence-id :tau :observation])
            (select-keys reinit [:occurrence-id :tau :observation]))
         (= q0 (:belief reinit) (get-in reinit [:initialization-receipt :value]))
         (= {:kind :model-misfit
             :reason :checkable-observation-has-zero-predictive-probability}
            (:finding r)))))

(defn- value-license-error [r]
  (let [predicted (:predicted-belief r)
        license (:vacuity-license r)
        vacuous? (same-belief? predicted (:posterior r))]
    (if vacuous?
      (let [actual (vacuity-license predicted (get-in r [:calculation :likelihoods]))
            ks [:status :theorem :support :constant-likelihood :predicted-total]]
        (cond
          (not= :licensed (:status actual)) (:kind actual)
          (not= (select-keys actual ks) (select-keys license ks)) :invalid-vacuity-license))
      (when-not (and (= :not-licensed (:status license))
                     (some? (:reason license)) (not (contains? license :theorem))
                     (= (set (keys predicted)) (:support license)))
        :invalid-vacuity-license))))

(defn- receipt-error [q0 r]
  (let [status (:status r) observation (:observation r)
        universe (get-in r [:carrier :universe])
        context (select-keys r [:occurrence-id :tau])]
    (cond
      (not (and (fields? r [:schema :status :consumed :occurrence-id :tau :observation
                            :z-semantics :vacuity-license])
                (= :wm/token-belief-update-v1 (:schema r))
                (= :per-step-redraw (:z-semantics r))
                (some? (:occurrence-id r))
                (#{:value :refused :missing :invalid} status)
                (= (= :value status) (:consumed r))))
      :invalid-belief-update-receipt

      (= :invalid status)
      (if (and (fields? r [:kind :detail])
               (absent? r [:posterior :continuation-belief])
               (not-applicable? (:vacuity-license r)))
        :producer-invalid-belief-update :invalid-belief-update-receipt)

      (not (and (set? universe)
                (every? map? (map r [:model :carrier :predecessor]))
                (every? #(stored-belief? (get r %) universe)
                        [:pre-belief :predicted-belief :continuation-belief])))
      :invalid-belief-update-domain

      (not= q0 (:continuation-belief r))
      :belief-update-consumption-mismatch

      (= :missing status)
      (when-not (and (nil? (:tau r))
                     (= observation {:status :missing :reason :no-observation
                                     :occurrence-id (:occurrence-id r) :tau nil})
                     (= :no-observation-prediction-only (:policy r))
                     (= q0 (:predicted-belief r))
                     (absent? r [:posterior :observation-probability :calculation :refused-trajectory])
                     (not-applicable? (:vacuity-license r)))
        :invalid-missing-belief-update)

      (not (and (pos-int? (:tau r)) (= :observed (:status observation))
                (= context (select-keys observation [:occurrence-id :tau]))
                (set? (:present observation)) (set? (:absent observation))
                (seq (set/union (:present observation) (:absent observation)))
                (empty? (set/intersection (:present observation) (:absent observation)))
                (set/subset? (set/union (:present observation) (:absent observation)) universe)
                (map? (:observation-authority r))))
      :invalid-belief-update-observation

      (= :value status)
      (if (and (= :conditioned-posterior (:policy r))
               (stored-belief? (:posterior r) universe) (= q0 (:posterior r))
               (probability? (:observation-probability r)) (pos? (:observation-probability r))
               (absent? r [:refused-trajectory :reinitialization])
               (calculation-matches? r (:calculation r) :ok :some)
               (same-belief? q0 (get-in r [:calculation :posterior])))
        (value-license-error r)
        :invalid-belief-update-continuation)

      (= :refused status)
      (when-not
       (and (= :belief-update-refused (:kind r))
            (= 0 (:observation-probability r))
            (absent? r [:posterior])
            (absent? (:refused-trajectory r) [:posterior])
            (calculation-matches? r (:refused-trajectory r) :refused :none)
            (not-applicable? (:vacuity-license r))
            (case (get-in r [:observation-authority :channel])
              :judgement (and (= :refused-observation-discarded (:policy r))
                              (= q0 (:predicted-belief r)) (absent? r [:reinitialization]))
              :checkable (and (= :refused-reinitialized-from-observation (:policy r))
                              (reinitialization-matches? r q0))
              false))
        :invalid-belief-update-continuation))))

(defn intake
  "Consume the frozen D v1 receipt (5a0f6a3f); never condition again or infer
   authority. D validates the underlying producer records. Q checks the
   recorded domain, context, calculation, policy and continuation equalities.
   No active receipt means no update; staged-2a receipts are not accepted."
  [{:keys [q0 belief-update-receipt] :as input}]
  (if-not (contains? input :belief-update-receipt)
    {:status :ready :observation-updates []
     ;; Absence of a receipt says nothing about whether an observation exists
     ;; (e.g. D refused carry admission before any update could run).
     :conditioning-input {:status :not-supplied :kind :belief-update-not-supplied}}
    (let [r belief-update-receipt
          error (receipt-error q0 r)]
      (cond
        (= :producer-invalid-belief-update error) r
        error {:status :invalid :kind error :receipt r}
        (= :missing (:status r))
        {:status :ready :conditioning-input r :observation-updates []}
        :else
        {:status :ready :conditioning-input r
         :observation-updates
         [(cond-> {:tau 0 :receipt r :observation (:observation r)
                   :pre-belief (:pre-belief r) :predicted-belief (:predicted-belief r)
                   :post-belief q0 :status (:status r) :consumed (:consumed r)
                   :channel (get-in r [:observation-authority :channel]) :policy (:policy r)
                   :vacuity-license (:vacuity-license r) :z-semantics :per-step-redraw}
            (= :value (:status r))
            (assoc :vacuous (same-belief? (:predicted-belief r) (:posterior r))))]}))))

(defn predictive-steps
  "Lazy, sequential future from the consumed filtering belief. Each transition
   runs once and the very outgoing belief feeds scoring and the next transition.
   Laziness preserves the evaluator's early stop on infinite risk or refusal."
  [transition precedence-fn q0 record?]
  (letfn [(advance [tau q]
            (lazy-seq
             (let [evaluated (transition (precedence-fn (dec tau)) q record?)
                   next-q (:belief evaluated)
                   step {:tau tau :belief next-q
                         :node-evaluation (when record?
                                            (assoc (:evaluation evaluated) :tau tau))}]
               (cons step (when-not (contains? next-q :status)
                            (advance (inc tau) next-q))))))]
    (advance 1 q0)))
