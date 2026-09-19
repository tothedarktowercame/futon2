(ns futon2.aif.observation-model
  "Bounded observation-model query interface. Models are EDN, including their
   parameters and authority. The enumeration backend is the persistent oracle
   for future compiled backends; callers use query, never backend internals.
   This version admits declared synthetic experiments only, not calibration."
  (:require [clojure.set :as set]
            [futon2.aif.cascade-model-manifest :as m]))

(def max-tokens 10)

(defn refuse! [kind data]
  (throw (ex-info (name kind) (merge {:status :missing :kind kind} data))))

(defn- probability? [x]
  (and (or (integer? x) (ratio? x)) (<= 0 x 1)))

(defn- require-rates! [universe rates]
  (when-not (and (map? rates) (= universe (set (keys rates)))
                 (every? #(and (probability? (:false-neg %))
                               (probability? (:false-pos %))) (vals rates)))
    (refuse! :invalid-model-rates {:rates rates})))

(defn validate!
  "Validate a declared bounded model. No rate, independence or authority default."
  [{:keys [schema backend kind universe rates components provenance] :as model}]
  (when-not (= :wm/observation-model-v1 schema)
    (refuse! :invalid-model-schema {}))
  (when-not (= :exact-enumeration backend)
    (refuse! :unsupported-observation-backend {:backend backend}))
  (when-not (and (set? universe) (<= 1 (count universe) max-tokens))
    (refuse! :observation-universe-out-of-bounds {:limit max-tokens}))
  (when-not (and (= :synthetic (:status provenance))
                 (false? (:calibrated provenance))
                 (string? (:source provenance)) (seq (:source provenance)))
    (refuse! :synthetic-provenance-required {}))
  (case kind
    :exact-checks
    (do (require-rates! universe rates)
        (when-not (every? #(and (zero? (:false-neg %))
                                (zero? (:false-pos %))) (vals rates))
          (refuse! :nonzero-checkable-rate {})))
    :independent-judgement (require-rates! universe rates)
    :coupled-judgement
    (do
      (when-not (and (vector? components) (seq components)
                     (every? #(and (some? (:id %))
                                   (probability? (:weight %))) components)
                     (= (count components) (count (set (map :id components))))
                     (= 1 (reduce + (map :weight components))))
        (refuse! :invalid-common-cause-mixture {}))
      (doseq [component components]
        (require-rates! universe (:rates component))))
    (refuse! :unknown-observation-model {:kind-declared kind}))
  model)

(defn- state! [model state]
  (when-not (and (set? state) (set/subset? state (:universe model)))
    (refuse! :state-outside-model {:state state})))

(defn- belief! [model belief]
  (when-not (and (map? belief) (seq belief) (m/normalized-exact? belief))
    (refuse! :invalid-state-belief {:belief belief}))
  (doseq [s (keys belief)] (state! model s)))

(defn- event! [model {:keys [present absent] :as event}]
  (when-not (and (map? event) (set? present) (set? absent)
                 (empty? (set/intersection present absent))
                 (set/subset? (set/union present absent) (:universe model)))
    (refuse! :invalid-observation-event {:event event})))

(defn- row [model state]
  (if (= :coupled-judgement (:kind model))
    (apply merge-with +
           (for [{:keys [weight rates]} (:components model)]
             (update-vals (m/observation-distribution rates state) #(* weight %))))
    (m/observation-distribution (:rates model) state)))

(defn- predictive [model belief]
  (apply merge-with +
         (for [[state mass] belief]
           (update-vals (row model state) #(* mass %)))))

(defn- event-probability [distribution {:keys [present absent]}]
  (reduce + 0 (for [[o p] distribution
                    :when (and (set/subset? present o)
                               (empty? (set/intersection absent o)))] p)))

(defn- ordered [distribution]
  (sort-by (fn [[state _]] (pr-str (if (set? state) (sort-by pr-str state) state))) distribution))

(defn entropy
  "Natural-log entropy; zero masses contribute zero."
  [distribution]
  (- (reduce + 0.0 (for [[_ p] (ordered distribution) :when (pos? p)]
                     (* (double p) (Math/log (double p)))))))

(defn- observation!
  [model observation context]
  (when-not (= :observed (:status observation))
    (refuse! :missing-observation {:observation observation}))
  (when-not (and (some? (:occurrence-id context))
                 (pos-int? (:tau context))
                 (= (select-keys observation [:occurrence-id :tau])
                    (select-keys context [:occurrence-id :tau])))
    (refuse! :observation-context-mismatch
             {:observation observation :prediction-context context}))
  (event! model observation)
  (when (empty? (set/union (:present observation) (:absent observation)))
    (refuse! :missing-observation {:observation observation})))

(defmulti evaluate
  "Backend dispatch. query validates and records the model around this method."
  (fn [model _request] (:backend model)))

(defmethod evaluate :exact-enumeration
  [model {:keys [op state belief event observation context preference]}]
  (case op
    :row (do (state! model state) {:distribution (row model state)})
    :likelihood (do (state! model state) (event! model event)
                    {:probability (event-probability (row model state) event)})
    :predict (do (belief! model belief) {:distribution (predictive model belief)})
    :condition
    (do
      (belief! model belief)
      (observation! model observation context)
      (let [weights (into {} (for [[s mass] belief]
                              [s (* mass (event-probability (row model s) observation))]))
            p (reduce + (vals weights))]
        (if (zero? p)
          {:status :contradiction :kind :impossible-observation
           :probability 0 :f ##Inf :observation observation :context context}
          {:probability p :f (- (Math/log (double p)))
           :posterior (into {} (for [[s w] weights :when (pos? w)] [s (/ w p)]))
           :observation observation :context context})))
    :score
    (do
      (belief! model belief)
      (when-not (and (map? preference) (seq preference)
                     (every? #(and (number? %) (Double/isFinite (double %))
                                   (<= 0 % 1)) (vals preference))
                     (< (Math/abs (- 1.0 (double (reduce + (vals preference))))) 1e-10))
        (refuse! :invalid-observation-preference {}))
      (doseq [o (keys preference)] (state! model o))
      (let [prediction (predictive model belief)
            risk (m/outcome-risk (ordered prediction) preference)
            ambiguity (reduce + 0.0 (for [[s p] (ordered belief)]
                                     (* (double p) (entropy (row model s)))))
            point-mass? (= 1 (count (filter (comp pos? val) belief)))
            ;; For a point mass, risk + ambiguity = E_A[-ln C]. Aggregate
            ;; equal C values with EXACT masses before taking logs. This
            ;; preserves symmetry under token permutations instead of letting
            ;; floating summation order split mathematically tied policies.
            c-masses (reduce (fn [acc [o p]] (update acc (get preference o 0) (fnil + 0) p))
                             (sorted-map) prediction)
            cross-entropy (- (reduce + 0.0 (for [[c p] c-masses :when (and (pos? c) (pos? p))]
                                              (* (double p) (Math/log (double c))))))]
        {:prediction prediction :risk risk :ambiguity ambiguity
         :g-evaluation (if point-mass? :point-mass-cross-entropy :risk-plus-ambiguity)
         :g (cond (= :infinite risk) ##Inf
                  point-mass? cross-entropy
                  :else (+ risk ambiguity))}))
    (refuse! :unsupported-observation-query {:op op})))

(defn query
  "One production-facing query contract, independent of representation.
   Every answer/refusal carries the exact model including synthetic authority.
   Partial events marginalize unmentioned tokens; conditioning requires at
   least one observed token and an explicit matching occurrence/horizon."
  [model request]
  (try
    (validate! model)
    (merge {:status :computed :model model :query (:op request)}
           (evaluate model request))
    (catch clojure.lang.ExceptionInfo e
      (merge {:model model :query (:op request)} (ex-data e)))))
