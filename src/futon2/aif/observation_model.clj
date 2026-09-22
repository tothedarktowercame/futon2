(ns futon2.aif.observation-model
  "Bounded observation-model query interface. Models are EDN, including their
   parameters and authority. The enumeration backend is the persistent oracle
   for future compiled backends; callers use query, never backend internals.
   This version admits declared synthetic experiments only, not calibration.

   Optional :parameters {id {:value exact-rational :basis record#population}}
   gives rates identity: token rates may be bare probabilities or {:param id}.
   Repeated references name ONE parameter; equal bare values do not. An id
   names an instrument and persists across re-measurement: a follow-up
   declaration changes value/basis, not id. A changed instrument convention or
   channel requires a NEW id, never a quiet redefinition. This identity rule
   governs declaration revisions; this stateless validator cannot compare history.
   Reserved instrument ids: :intake-refusal-fp, :handoff-correction-fp,
   :frame-selfcorrection-fp, :judgement-default-fp, :judgement-default-fn,
   :bad-window-p. This schema change attaches no measured rates.
   Parameter uncertainty belongs on the parameter entry later, never inline
   in rates. probability? governs :value alone, never :variance. Parameter
   metadata is retained, but variance propagation is not implemented here.

   The synthetic-provenance refusal is a gate, not a temporary nuisance:
   when calibrated rates exist (programme point 5) it WIDENS to require a
   calibration record. It is never dropped."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.matched-observation-evidence :as evidence]))

(def max-tokens 10)

(defn refuse! [kind data]
  (throw (ex-info (name kind) (merge {:status :missing :kind kind} data))))

(defn- probability? [x]
  (and (or (integer? x) (ratio? x)) (<= 0 x 1)))

(defn- require-parameters! [model]
  (when (contains? model :parameters)
    (when-not (and (map? (:parameters model))
                   (every? (fn [[id entry]]
                             (and (keyword? id) (map? entry)
                                  (probability? (:value entry))
                                  (string? (:basis entry))
                                  (not (str/blank? (:basis entry)))))
                           (:parameters model)))
      (refuse! :invalid-model-parameters {:parameters (:parameters model)}))))

(defn- rate-value! [parameters value]
  (cond
    (probability? value) value
    (and (map? value) (= #{:param} (set (keys value))))
    (if (contains? parameters (:param value))
      (get-in parameters [(:param value) :value])
      (refuse! :unknown-observation-parameter {:param (:param value)}))
    :else (refuse! :invalid-model-rates {:rate value})))

(defn- require-rates! [universe rates parameters]
  (when-not (map? rates)
    (refuse! :invalid-model-rates {:rates rates}))
  (when (or (contains? rates :variance)
            (some #(and (map? %) (or (contains? % :variance)
                                     (some (fn [v] (and (map? v) (contains? v :variance)))
                                           (vals %)))) (vals rates)))
    (refuse! :inline-rate-variance {:rates rates}))
  (when-not (and (map? rates) (= universe (set (keys rates)))
                 (every? map? (vals rates)))
    (refuse! :invalid-model-rates {:rates rates}))
  (update-vals rates (fn [r] (-> r
                                (update :false-neg #(rate-value! parameters %))
                                (update :false-pos #(rate-value! parameters %))))))

(defn validate!
  "Validate a declared bounded model. No rate, independence or authority default."
  [{:keys [schema backend kind universe rates components provenance parameters
            horizon class-universe acceptance target-class class-preference] :as model}]
  (when-not (= :wm/observation-model-v1 schema)
    (refuse! :invalid-model-schema {}))
  (when-not (= :exact-enumeration backend)
    (refuse! :unsupported-observation-backend {:backend backend}))
  (when-not (if (= :class-emission kind)
              ;; The class model never enumerates the token powerset -- its
              ;; risk lives over at most five classes -- so the enumeration
              ;; backend's token bound does not apply; only set-ness does.
              (set? universe)
              (and (set? universe) (<= 1 (count universe) max-tokens)))
    (refuse! :observation-universe-out-of-bounds
             {:limit (when-not (= :class-emission kind) max-tokens)}))
  (when-not (and (= :synthetic (:status provenance))
                 (false? (:calibrated provenance))
                 (string? (:source provenance)) (seq (:source provenance)))
    (refuse! :synthetic-provenance-required {}))
  (require-parameters! model)
  (case kind
    :exact-checks
    (let [resolved (require-rates! universe rates parameters)]
        (when-not (every? #(and (zero? (:false-neg %))
                                (zero? (:false-pos %))) (vals resolved))
          (refuse! :nonzero-checkable-rate {})))
    :independent-judgement (require-rates! universe rates parameters)
    :coupled-judgement
    (do
      (when-not (and (vector? components) (seq components)
                     (every? #(and (some? (:id %))
                                   (probability? (:weight %))) components)
                     (= (count components) (count (set (map :id components))))
                     (= 1 (reduce + (map :weight components))))
        (refuse! :invalid-common-cause-mixture {}))
      (doseq [component components]
        (require-rates! universe (:rates component) parameters)))
    :class-emission
    (do
      (when-not (and (pos-int? horizon) (<= horizon 10))
        (refuse! :invalid-class-horizon {:horizon horizon}))
      (when-not (= [:focused :related :unrelated :stop-the-line
                    :ending/not-yet-evaluated] class-universe)
        (refuse! :invalid-class-universe {:class-universe (:class-universe model)}))
      (when-not (and (set? acceptance) (set/subset? acceptance universe))
        (refuse! :invalid-class-acceptance {:acceptance acceptance}))
      (when-not (and (map? target-class)
                     (every? #(contains? #{:focused :related :unrelated} (val %)) target-class))
        (refuse! :invalid-target-class {:target-class target-class}))
      (when-not (and (map? class-preference)
                     (= (set (range 1 (inc horizon))) (set (keys class-preference)))
                     (every? (fn [[_pref-tau pref]]
                               (and (map? pref)
                                    (every? #(contains? (set class-universe) %) (keys pref))
                                    (probability? (reduce + (vals pref)))))
                             class-preference))
        (refuse! :invalid-class-preference {:class-preference class-preference})))
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
             (update-vals (m/observation-distribution
                           (require-rates! (:universe model) rates (:parameters model)) state)
                          #(* weight %))))
    (m/observation-distribution
     (require-rates! (:universe model) (:rates model) (:parameters model)) state)))

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

(defn- class-of-state
  "PROOF-wm-works 1.3 build 2/3: a state's run-ending class. A state carrying
   one or more acceptance tokens takes the class of those tokens' targets
   (mass over distinct classes splits equally when several targets are
   accepted in one state); a state carrying none has not ended -- at the
   horizon that is :stop-the-line per Joe's ruling (unmeasured/not reached),
   before it :ending/not-yet-evaluated."
  [{:keys [acceptance target-class]} state terminal?]
  (let [accepted (filter #(contains? state %) acceptance)
        classes (distinct (for [token accepted
                                :let [t (first token)]
                                :when (contains? target-class t)]
                            (get target-class t)))]
    (cond (seq classes)
          (zipmap classes (repeat (/ 1 (count classes))))
          terminal? {:stop-the-line 1}
          :else {:ending/not-yet-evaluated 1})))

(defn- class-predictive
  [{:keys [horizon] :as model} belief tau]
  (let [terminal? (>= tau horizon)]
    (apply merge-with +
           (for [[state mass] belief]
             (update-vals (class-of-state model state terminal?) #(* mass %))))))

(defn- class-preference-for
  [model tau]
  (let [pref (get-in model [:class-preference tau])]
    (when-not (and (map? pref) (seq pref))
      (refuse! :invalid-class-preference {:tau tau :preference pref}))
    pref))

(defn- class-preference!
  "Class preferences are keyed by class keyword, not token subset."
  [model preference]
  (when-not (and (map? preference) (seq preference)
                 (every? #(and (contains? (set (:class-universe model)) (key %))
                               (probability? (val %))) preference)
                 (probability? (reduce + (vals preference))))
    (refuse! :invalid-observation-preference {:preference preference}))
  preference)

(defmulti evaluate
  "Backend dispatch. query validates and records the model around this method."
  (fn [model _request] (:backend model)))

(defmethod evaluate :exact-enumeration
  [model {:keys [op state belief event observation context preference tau]}]
  (if (= :class-emission (:kind model))
    (case op
      :score
      (do
        ;; Class check: a valid distribution over SET states. The token-
        ;; universe subset check is a token-model concern; the class model
        ;; reads states only for acceptance-token membership.
        (when-not (and (map? belief) (seq belief) (m/normalized-exact? belief)
                       (every? set? (keys belief)))
          (refuse! :invalid-state-belief {:belief belief}))
        (let [tau (or tau (:horizon model))
              pref (class-preference! model (class-preference-for model tau))
              prediction (class-predictive model belief tau)
              risk (m/outcome-risk (ordered prediction) pref)]
          ;; Deterministic emission: ambiguity is exactly 0 at every step;
          ;; before the horizon the single :ending/not-yet-evaluated symbol
          ;; against preference 1 gives risk exactly 0.
          {:prediction prediction :risk risk :ambiguity 0.0
           :g-evaluation :class-emission
           :tau tau
           :g (cond (= :infinite risk) ##Inf
                    ;; point mass: -ln C of the single emitted class, exact
                    (= 1 (count (filter pos? (vals prediction))))
                    (- (Math/log (double (get pref (first (filter #(pos? (get prediction %))
                                                                  (keys prediction)))))))
                    :else (double risk))}))
      :condition
      (do (when-not (and (map? belief) (seq belief) (m/normalized-exact? belief)
                         (every? set? (keys belief)))
            (refuse! :invalid-state-belief {:belief belief}))
          ;; No observation to condition on at selection time: the class
          ;; model explains nothing yet, F is explicitly neutral and the
          ;; belief is carried unchanged.
          {:posterior belief :f {:value 0.0 :status :not-supplied
                                 :reason :class-model-unconditioned-at-selection}})
      (refuse! :unsupported-observation-query {:op op}))
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
          {:probability p :f (evidence/surprisal p)
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
    (refuse! :unsupported-observation-query {:op op}))))

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
