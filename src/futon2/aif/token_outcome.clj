(ns futon2.aif.token-outcome
  "Selection-time token prediction and post-build comparison. No belief update."
  (:require [clojure.set :as set]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.interpretation-evidence :as evidence]))

(load-identity/register! *ns* *file*)

(defn- co-apply-input
  "Structural validation of the {:co-apply {:units … :descent … :patterns …}}
   precedence shape that cascade-model-manifest/evaluate-state already
   evaluates (Lean coApplyKernel). This gate rejects malformed structure —
   not-a-map patterns, descent pairs naming unknown units, patterns missing a
   unit. Semantic refusals (uninterpreted patterns, invalid theta) are NOT
   re-implemented here: the model's own missing-interpretation and
   pattern-theta checks refuse the rollout and surface as
   :model-rollout-refused, the same authority the vector path uses.
   Returns the validated {:co-apply …} map, or a typed finding."
  [precedence]
  (let [{:keys [units descent patterns]} (:co-apply precedence)]
    (cond
      (not (and (map? precedence) (= #{:co-apply} (set (keys precedence)))))
      {:status :missing :kind :malformed-co-apply-step :detail :not-sole-co-apply-key}
      (not (and (sequential? units) (seq units) (apply distinct? units)))
      {:status :missing :kind :malformed-co-apply-step :detail :units}
      (not (and (sequential? descent)
                (every? #(and (sequential? %) (= 2 (count %))) descent)))
      {:status :missing :kind :malformed-co-apply-step :detail :descent}
      (not (every? (set units) (apply concat descent)))
      {:status :missing :kind :malformed-co-apply-step :detail :descent-unknown-unit}
      (not (and (map? patterns) (every? map? (vals patterns))
                (set/superset? (set (keys patterns)) (set units))))
      {:status :missing :kind :malformed-co-apply-step :detail :patterns}
      :else precedence)))

(defn precedence-step
  "The frozen step freeze-prediction rolls out: the legacy precedence VECTOR
   (first-enabled cascade kernel), or the co-apply MAP the same push-forward
   in cascade-model-manifest evaluates when the containment order is not a
   chain. A precedence that is neither shape — or a structurally malformed
   co-apply map — is a typed finding and the prediction refuses."
  [precedence]
  (cond
    (vector? precedence) precedence
    (map? precedence) (co-apply-input precedence)
    :else {:status :missing :kind :malformed-precedence-step}))

(defn- step-intended-outputs
  "Tokens the step's patterns declare they produce. Legacy vector semantics
   are preserved exactly (mapcat :produces, the frozen schema). The co-apply
   map unions the produces of every declared unit's pattern, accepting either
   spelling of the produces field (top-level or nested :transition) as
   cascade-model-manifest/produces-of does — mapcat :produces alone would
   silently drop outputs there."
  [step]
  (if (vector? step)
    (into #{} (mapcat :produces) step)
    (let [{:keys [units patterns]} (:co-apply step)]
      (into #{} (mapcat (fn [p] (or (:produces p)
                                    (get-in p [:transition :produces])
                                    nil)))
            (map patterns units)))))

(defn freeze-prediction
  "Freeze the scorer's terminal rollout for ALL wants of the selected target.
   Verdicts use positive marginal support, not a hidden probability threshold."
  [decision]
  (let [action (:action decision)
        target (:target action)
        certificate (:selection-certificate decision)
        family (:precision-family certificate)
        {:keys [q0 horizon]} (:model family)
        domains (filter #(= target (:target %))
                        (get-in certificate [:token-belief-stage :domain-inputs]))
        want (get-in (first domains) [:declaration :want])
        base {:schema :wm/token-outcome-prediction-v1 :target target
              :action action :model-id (:model-id family)
              :prediction-rule :positive-marginal-support}
        step (precedence-step (:precedence action))]
    (if-not (and (= :cascade-candidate (:kind action))
                 (= 1 (count domains)) (seq want)
                 (map? q0) (seq q0) (model/normalized-exact? q0)
                 (every? set? (keys q0))
                 (integer? horizon) (<= 0 horizon)
                 step (not (contains? step :status)))
      (cond-> (assoc base :status :refused :reason :prediction-input-unavailable)
        (and (map? step) (contains? step :status))
        (assoc :precedence-finding step))
      (let [result (model/rollout-evaluation (constantly step) q0 horizon)
            belief (:belief result)]
        (if (contains? belief :status)
          (assoc base :status :refused :reason :model-rollout-refused :finding belief)
          (assoc base :status :frozen :initial-belief q0 :horizon horizon
                 :model (:model family) :rollout result
                 :precedence-kind (if (vector? step) :precedence-vector :co-apply-map)
                 :observation-locators (:observation-locators action)
                 :intended-outputs (step-intended-outputs step)
                 :wanted (mapv (fn [token]
                                 (let [qualified [target token]]
                                   {:token qualified
                                    :predicted (reduce-kv
                                                (fn [p state mass]
                                                  (+ p (if (contains? state qualified) mass 0)))
                                                0 belief)}))
                               (sort-by pr-str want))))))))

(defn compare-outcomes
  "Compare raw measurements, never the positive-only D-task carry projection.
   Missing, duplicate, or wrong-artifact evidence remains typed missing."
  [prediction measurements artifact-sha]
  (let [base {:schema :wm/token-outcome-comparison-v1
              :prediction prediction :artifact-sha artifact-sha
              :measurements (vec measurements)
              :evidence-sha256 (evidence/value-digest (vec measurements))}]
    (if-not (= :frozen (:status prediction))
      (assoc base :status :refused :reason :prediction-unavailable)
      (assoc base :status :compared
             :tokens
             (mapv
              (fn [{:keys [token predicted]}]
                (let [rows (filter #(= token (:token %)) measurements)
                      row (first rows)
                      result (:result row)
                      observed (:observed result)
                      missing (cond
                                (empty? rows) :measurement-unavailable
                                (not= 1 (count rows)) :ambiguous-measurement
                                (not (boolean? observed)) (or (:kind result) :measurement-unavailable)
                                (or (nil? artifact-sha)
                                    (not= artifact-sha (get-in result [:evidence :resolved-sha])))
                                :artifact-revision-mismatch)
                      verdict (cond missing :observation-missing
                                    (pos? predicted) (if observed :predicted-and-observed
                                                        :predicted-not-observed)
                                    observed :not-predicted-observed
                                    :else :neither)]
                  (cond-> {:token token :predicted predicted
                           :observed (if missing {:status :missing :kind missing} observed)
                           :verdict verdict :measurement row}
                    missing (assoc :reason missing))))
              (:wanted prediction))))))
