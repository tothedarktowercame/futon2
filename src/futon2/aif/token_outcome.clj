(ns futon2.aif.token-outcome
  "Selection-time token prediction and post-build comparison. No belief update."
  (:require [clojure.set :as set]
            [clojure.walk :as walk]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.interpretation-evidence :as evidence]))

(load-identity/register! *ns* *file*)

(def pattern-input-schema :wm/pattern-done-prediction-input-v1)
(def ^:private pattern-model-keys
  [:q0 :horizon :want :transition-kernel :observation-model])

(defn- pattern-model-id [input]
  (let [order #(compare (pr-str %1) (pr-str %2))
        canonical (walk/postwalk
                   #(cond (map? %) (into (sorted-map-by order) %)
                          (set? %) (into (sorted-set-by order) %)
                          :else %)
                   (select-keys input pattern-model-keys))]
    (load-identity/sha256 (.getBytes (pr-str canonical) "UTF-8"))))

(defn pattern-token?
  "True when TOKEN is a qualified pattern-done occurrence for TARGET."
  [target token]
  (and (vector? token) (= 3 (count token)) (= target (first token))
       (= :pattern-done (second token))
       (vector? (nth token 2)) (= 2 (count (nth token 2)))
       (string? (first (nth token 2))) (nat-int? (second (nth token 2)))))

(defn- pattern-input-valid? [target {:keys [q0 horizon want transition-kernel observation-model]}]
  (and (map? q0) (seq q0) (every? set? (keys q0))
       (every? #(and (number? %) (<= 0 %)) (vals q0))
       (model/normalized-exact? q0)
       (every? #(pattern-token? target %) (mapcat identity (keys q0)))
       (nat-int? horizon) (keyword? transition-kernel)
       (map? observation-model) (= horizon (:horizon observation-model))
       (or (set? want) (vector? want)) (seq want)
       (= (count want) (count (set want)))
       (= want (:want observation-model))
       (every? #(pattern-token? target %) want)))

(defn pattern-prediction-input
  "Copy the selected scorer's inputs before sealing. No precision-family or
   mission-hole defaults: the native pattern-token domain is part of the hash."
  [entry]
  (let [input {:schema pattern-input-schema
               :q0 (get-in entry [:prediction :initial-belief])
               :horizon (:horizon-steps entry)
               :want (get-in entry [:observation-model :want])
               :transition-kernel (:transition-kernel entry)
               :observation-model (:observation-model entry)}]
    (if (pattern-input-valid? (get-in entry [:action :target]) input)
      (assoc input :status :present :model-id (pattern-model-id input))
      {:schema pattern-input-schema :status :absent
       :reason :selected-pattern-model-input-unavailable})))

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
        pattern? (contains? certificate :pattern-prediction-input)
        input (:pattern-prediction-input certificate)
        prediction-model (if pattern? (select-keys input pattern-model-keys) (:model family))
        {:keys [q0 horizon]} prediction-model
        domains (filter #(= target (:target %))
                        (get-in certificate [:token-belief-stage :domain-inputs]))
        want (if pattern? (:want input) (get-in (first domains) [:declaration :want]))
        base {:schema :wm/token-outcome-prediction-v1 :target target
              ;; The pattern prediction retains executable identity, not the
              ;; separate mission-hole obligations or their locators.
              :action (if pattern? (select-keys action [:kind :id :target :precedence]) action)
              :model-id (:model-id (if pattern? input family))
              :prediction-rule :positive-marginal-support}
        step (precedence-step (:precedence action))]
    (if-not (and (= :cascade-candidate (:kind action))
                 (if pattern?
                   (and (= :wm/selected-outer-cascade-policy-v1 (:selection-route certificate))
                        (= pattern-input-schema (:schema input)) (= :present (:status input))
                        (pattern-input-valid? target input)
                        (= (:model-id input) (pattern-model-id input)))
                   (= 1 (count domains))) (seq want)
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
                 :model prediction-model :rollout result
                 :precedence-kind (if (vector? step) :precedence-vector :co-apply-map)
                 :observation-locators (when-not pattern? (:observation-locators action))
                 :intended-outputs (step-intended-outputs step)
                 :wanted (mapv (fn [token]
                                 (let [qualified (if pattern? token [target token])]
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
