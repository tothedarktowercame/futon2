(ns futon2.aif.cascade-observation-scoring
  "Bounded cascade scorer for an explicitly declared observation model.
   Called by efe/rank-cascade-actions; no new selection law and no actuator.
   G is the prior-predictive horizon sum. F explains an explicitly matched
   observation at that horizon. The conditioned belief is retained for the
   next prediction, not substituted into the prediction being evaluated."
  (:require [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.data-paths :as data-paths]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.observation-model :as om]
            [futon2.aif.parameter-novelty :as novelty])
  (:import [java.security MessageDigest]
           [java.io FileOutputStream]
           [java.nio.file Files Path Paths StandardCopyOption]
           [java.util.concurrent Callable Executors]))

(def max-horizon 10)
(def max-candidates 16)
(def cache-top-k 8)
(def cache-refresh-count 8)
(def shared-materiality-margin 1.0e-12)
(def cold-score-budget-ms 120000)

(def ^:private worker-memory-budget-bytes (* 512 1024 1024))

(defn- heap-worker-cap []
  (let [heap (.maxMemory (Runtime/getRuntime))]
    (max 1 (min 64 (quot heap worker-memory-budget-bytes)))))

(defn- canonical-data [x]
  (cond
    (map? x) (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
                   (map (fn [[k v]] [k (canonical-data v)]) x))
    (set? x) (vec (sort-by pr-str (map canonical-data x)))
    (sequential? x) (mapv canonical-data x)
    :else x))

(defn- digest [x]
  (format "%064x" (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256")
                                           (.getBytes (pr-str (canonical-data x)) "UTF-8")))))

(defn- numeric-leaves [x]
  (cond
    (number? x) [x]
    (map? x) (mapcat (fn [[k v]] (concat (numeric-leaves k) (numeric-leaves v))) x)
    (coll? x) (mapcat numeric-leaves x)
    :else []))

(defn- shared-change-bound
  "L1 change in the shared numerical inputs.  Entry-specific derivatives
  convert this input delta into a G interval; equal inputs have zero delta."
  [old-shared new-shared]
  (if (= (canonical-data old-shared) (canonical-data new-shared))
    0.0
    (let [a (vec (numeric-leaves old-shared))
          b (vec (numeric-leaves new-shared))
          n (max (count a) (count b))]
      (let [delta (reduce + 0.0
                           (for [i (range n)]
                             (Math/abs (double (- (get a i 0)
                                                  (get b i 0))))))]
        delta))))

(defn- entropy-sensitivity [distribution]
  (reduce + 0.0 (for [[_ p] distribution :when (pos? (double p))]
                   (+ 1.0 (Math/abs (Math/log (double p)))))))

(defn- entry-sensitivity
  "Derivative envelope for the actually consumed G terms.  Cross-entropy
  contributes 1/p per consumed outcome; entropy contributes |1+log p|;
  state-EIG contributes the same log-ratio envelope.  These are the closed
  form derivatives of the scored folds, summed over this entry's steps."
  [entry shared-delta]
  (let [steps (get-in entry [:certificate :steps])
        coefficient
        (reduce + 0.0
                (for [step steps
                      :let [prediction (:prediction step)
                            p (map second prediction)]]
                  (+ (reduce + 0.0 (map #(if (pos? (double %))
                                           (/ 1.0 (double %))
                                           Double/POSITIVE_INFINITY) p))
                     (entropy-sensitivity prediction)
                     (entropy-sensitivity prediction))))]
    (if (Double/isInfinite (double shared-delta))
      Double/POSITIVE_INFINITY
      (* (double shared-delta) coefficient))))

(defn- cache-path [opts]
  (or (:scoring-cache-path opts)
      (data-paths/path "wm-scoring-cache" "global-rank.edn")))

(defn- read-cache [path]
  (try
    (if-not (.isFile (io/file path))
      {:status :cold-start :reason :missing-cache :value {}}
      (let [value (edn/read-string (slurp path))]
        (if (and (map? value)
                 (= :wm-global-scoring-cache-v2 (:schema value))
                 (integer? (:generation value))
                 (map? (:entries value)))
          {:status :ok :value value}
          {:status :cold-start :reason :invalid-cache-schema :value {}})))
    (catch Exception _
      ;; A damaged cache is data, not a scorer failure.  Never reuse a
      ;; partially readable map: the next invocation is a typed cold start.
      {:status :cold-start :reason :corrupt-cache :value {}})))

(defn- write-cache! [path value]
  (io/make-parents path)
  (let [tmp (str path ".tmp-" (System/nanoTime))
        bytes (.getBytes (pr-str value) "UTF-8")]
    (try
      (with-open [out (FileOutputStream. tmp)]
        (.write out bytes)
        (.flush out)
        (.sync (.getFD out)))
      (try
        (Files/move (Paths/get tmp (make-array String 0))
                    (Paths/get path (make-array String 0))
                    (into-array StandardCopyOption
                                [StandardCopyOption/ATOMIC_MOVE
                                 StandardCopyOption/REPLACE_EXISTING]))
        (catch java.nio.file.AtomicMoveNotSupportedException _
          (Files/move (Paths/get tmp (make-array String 0))
                      (Paths/get path (make-array String 0))
                      (into-array StandardCopyOption
                                  [StandardCopyOption/REPLACE_EXISTING]))))
      (finally
        (when (.exists (io/file tmp)) (.delete (io/file tmp)))))))

(defn- parallel-mapv
  "Evaluate independent target policies concurrently, retaining input order.
  The result order is never used as a ranking tie-break; canonical policy IDs
  decide ties after all workers finish."
  [f xs requested-workers]
  (let [workers (max 1 (min (heap-worker-cap)
                            (or requested-workers
                                (min 16 (.availableProcessors (Runtime/getRuntime))))))
        executor (Executors/newFixedThreadPool workers)
        futures (mapv (fn [x]
                        (.submit executor ^Callable (reify Callable
                                                       (call [_] (f x))))) xs)]
    (try
      (mapv #(.get %) futures)
      (finally
        (.shutdown executor)))))

(def ^:private machine-epsilon (Math/ulp 1.0))

(defn- numerical-resolution
  "Forward error bound for the floating-point G fold.

  Each observation-model term already includes its audited log evaluation;
  this bound covers the subsequent compensated reductions and the final
  risk+ambiguity-information combination.  It is deliberately derived from
  the number and magnitudes of the terms for this policy, never a global
  ranking tolerance."
  [steps raw-risk raw-ambiguity raw-information]
  (let [terms (mapcat (fn [step]
                        (map #(double (or (% step) 0.0))
                             [:risk :ambiguity :information-gain]))
                      steps)
        n (count terms)
        magnitude (+ 1.0 (reduce + 0.0 (map #(Math/abs (double %)) terms)))
        ;; Two rounding points per reduced term plus the final signed fold.
        operations (+ (* 2 (max 1 n)) 1)
        bound (* machine-epsilon operations magnitude)]
    {:value bound
     :machine-epsilon machine-epsilon
     :term-count n
     :operation-count operations
     :absolute-term-sum (- magnitude 1.0)
     :terms [:risk :ambiguity :information-gain]
     :raw {:risk raw-risk :ambiguity raw-ambiguity
           :expected-information-gain raw-information}}))

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
        resolution (numerical-resolution steps raw-risk raw-ambiguity raw-information)
        entry {:action candidate :cascade true :cascade-id (:id candidate)
               :horizon-steps horizon-steps :controller-score g :G g :g g :G-efe g :G-cascade g
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
                                       :numerical-resolution resolution
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
            workers (min (heap-worker-cap)
                         (or (:scoring-parallelism opts)
                             (min 16 (.availableProcessors (Runtime/getRuntime)))))
            cache-enabled? (true? (:scoring-cache? opts))
            cache-file (cache-path opts)
            cache-read (if cache-enabled?
                         (read-cache cache-file)
                         {:status :disabled :value {}})
            old-cache (:value cache-read)
            cache-cold-start? (= :cold-start (:status cache-read))
            generation (inc (long (or (:generation old-cache) 0)))
            old-entries (or (:entries old-cache) {})
            shared-inputs {:observation-model model
                           :preference preference
                           :observation (:observation opts)
                           :prediction-context (:prediction-context opts)}
            shared-bound (shared-change-bound (:shared-inputs old-cache) shared-inputs)
            materiality-margin (double (or (:scoring-cache-materiality-margin opts)
                                           shared-materiality-margin))
            work (mapv (fn [candidate]
                         (let [key (digest {:candidate candidate
                                            :cascade-spec (:cascade-spec opts)})
                               record (get old-entries key)
                               resolution (double (or (get-in record [:entry :certificate
                                                                      :g-terms :numerical-resolution :value])
                                                      0.0))
                               sensitivity (entry-sensitivity (:entry record) shared-bound)]
                           {:candidate candidate :key key :record record
                            :sensitivity sensitivity
                            :shared-bound shared-bound
                           :resolution resolution}))
                       candidates)
            ;; A live click may cold-score, but it is bounded so a pathological
            ;; model cannot monopolise the Agency JVM.  The clock is injectable
            ;; for a deterministic budget test; the prewarm path remains an
            ;; explicit offline caller and is never treated as a live click.
            cold-scoring? (and cache-enabled?
                               (not (:scoring-cache-prewarm? opts))
                               (or cache-cold-start?
                                   (some #(nil? (:record %)) work)))
            cold-budget-ms (long (or (:scoring-cache-time-budget-ms opts)
                                     cold-score-budget-ms))
            scoring-clock (or (:scoring-cache-clock opts)
                              #(quot (System/nanoTime) 1000000))
            cold-start-ms (when cold-scoring? (long (scoring-clock)))
            cached-work (filterv :record work)
            top-k (long (or (:scoring-cache-top-k opts) cache-top-k))
            refresh-count (long (or (:scoring-cache-refresh-count opts) cache-refresh-count))
            boundary-work (sort-by (juxt (comp :controller-score :entry :record)
                                        (comp pr-str :key)) cached-work)
            kth (when (and (pos? top-k) (seq boundary-work))
                  (nth boundary-work (min (dec top-k) (dec (count boundary-work)))))
            boundary-score (double (or (some-> kth :record :entry :controller-score)
                                       Double/POSITIVE_INFINITY))
            boundary-sensitivity (double (or (some-> kth :sensitivity)
                                             0.0))
            decision-refresh?
            (fn [{:keys [record sensitivity resolution]}]
              (or (nil? record)
                  (and (number? (:controller-score (:entry record)))
                       (if kth
                         (or (<= (double (:controller-score (:entry record)))
                                 boundary-score)
                             (<= (Math/abs (- (double (:controller-score (:entry record)))
                                              boundary-score))
                                 (+ (double (or sensitivity 0.0))
                                    boundary-sensitivity
                                    materiality-margin)))
                         (> (double (or sensitivity 0.0))
                            (+ (double resolution) materiality-margin))))))
            head-keys (set (map :key (take top-k
                                           (sort-by (juxt (comp :controller-score :entry :record)
                                                          (comp pr-str :key))
                                                         cached-work))))
            stale-work (take refresh-count
                             (sort-by (juxt (comp - :generation :record)
                                            (comp pr-str :key)) cached-work))
            refresh-keys (set (concat head-keys (map :key stale-work)))
            fresh-work (if cold-scoring?
                         work
                         (if cache-enabled?
                           (filterv #(or (decision-refresh? %)
                                         (refresh-keys (:key %))) work)
                           work))
            fresh-results (into {}
                               (map (fn [[w entry]]
                                      (let [cache {:status (if cold-scoring? :cold-scored :fresh)
                                                   :reason (cond cold-scoring? :cold-scored
                                                                 (:record w) :head-or-stale-refresh
                                                                 :else :cache-miss)
                                                   :generation generation
                                                   :age 0 :digest (:key w)
                                                   :inputs-digest (:key w)
                                                   :shared-generation generation
                                                   :shared-bound (:shared-bound w)
                                                   :sensitivity (:sensitivity w)
                                                   :decision :rescore}]
                                        (let [cache (cond-> cache
                                                       cache-cold-start?
                                                       (assoc :cold-start-reason (:reason cache-read)))]
                                          [(:key w) (assoc entry :cache cache
                                                           :certificate (assoc (:certificate entry)
                                                                               :cache cache))])))
                                    (map vector fresh-work
                                         (parallel-mapv #(score-candidate (:cascade-belief state)
                                                                          (:candidate %) opts preference)
                                                        fresh-work workers))))
            cold-elapsed-ms (when cold-scoring?
                              (- (long (scoring-clock)) cold-start-ms))
            _cold-budget-check (when (and cold-scoring?
                                          (> cold-elapsed-ms cold-budget-ms))
                                 (throw (ex-info "live cold scoring exceeded its time budget"
                                                 {:status :missing
                                                  :kind :scoring-cache-time-budget-exceeded
                                                  :elapsed-ms cold-elapsed-ms
                                                  :budget-ms cold-budget-ms
                                                  :cache-path cache-file})))
            entries (mapv (fn [{:keys [key record] :as w}]
                            (or (get fresh-results key)
                                (let [cache {:status :cached
                                             :reason (if (head-keys key) :top-k-revalidation
                                                         :unchanged)
                                             :generation generation
                                             :age (- generation (long (:generation record)))
                                             :digest key :inputs-digest key
                                             :shared-generation (:shared-generation old-cache)
                                             :shared-bound (:shared-bound w)
                                             :sensitivity (:sensitivity w)
                                             :decision :reuse}
                                      entry (:entry record)]
                                  (assoc entry :cache cache
                                         :certificate (assoc (:certificate entry)
                                                             :cache cache))))) work)
            cache-policy {:schema :wm-global-scoring-cache-policy-v1
                          :top-k top-k
                          :refresh-width refresh-count
                          :shared-materiality {:bound :l1-numeric-leaves
                                               :sensitivity :entry-derived-from-closed-forms
                                               :margin materiality-margin
                                               :reuse-when :bound-at-most-resolution-plus-margin}
                          :max-age-clicks (long (Math/ceil (/ (double (max 1 (count entries)))
                                                               (double (max 1 refresh-count)))))
                          :justification {:top-k :revalidate-resolution-tie-head
                                          :refresh-width :bounded-oldest-first
                                          :age-bound :ceil-entry-count-over-refresh-width}
                          :cold-score-budget-ms cold-budget-ms}
            _cache-written (when cache-enabled?
                             (write-cache! cache-file
                                           {:schema :wm-global-scoring-cache-v2
                                            :generation generation
                                            :shared-generation generation
                                            :shared-inputs shared-inputs
                                            :prewarm (when-let [m (:scoring-cache-prewarm-metadata opts)]
                                                       (assoc m :duration-ms
                                                              (/ (- (System/nanoTime)
                                                                    (long (:scoring-cache-prewarm-start-ns opts)))
                                                                 1e6)))
                                            :cache-policy cache-policy
                                            :cold-score (when cold-scoring?
                                                          {:status :cold-scored
                                                           :budget-ms cold-budget-ms
                                                           :elapsed-ms cold-elapsed-ms})
                                            :entries (into (sorted-map)
                                                           (map (fn [entry]
                                                                  (let [key (get-in entry [:cache :digest])]
                                                                    [key {:generation generation
                                                                          :entry (dissoc entry :cache)}]))
                                                                entries))}))
            failures (filterv #(not= :computed (get-in % [:inference :status])) entries)]
        (if (seq failures)
          {:status (if (some #(= :missing (get-in % [:inference :status])) failures)
                     :missing :contradiction)
           :kind :observation-family-not-selectable :model model :candidates entries
           :failures (mapv (fn [e] {:cascade-id (:cascade-id e) :inference (:inference e)}) failures)}
          (let [;; Resolution-aware comparison: a difference is meaningful
                ;; only when it exceeds both policies' forward-error bounds.
                ;; The id is the declared, input-order-independent action
                ;; tie-break and is also the canonical presentation order.
                sorted (sort-by (juxt :controller-score (comp pr-str :cascade-id)) entries)
                groups (loop [remaining sorted groups []]
                         (if-let [entry (first remaining)]
                           (let [previous (peek groups)
                                 representative (first previous)
                                 resolution (+ (double (get-in representative
                                                               [:certificate :g-terms
                                                                :numerical-resolution :value]
                                                               0.0))
                                               (double (get-in entry
                                                               [:certificate :g-terms
                                                                :numerical-resolution :value]
                                                               0.0)))
                                 tied? (and representative
                                             (<= (Math/abs (- (double (:controller-score entry))
                                                              (double (:controller-score representative))))
                                                 resolution))]
                             (recur (next remaining)
                                    (if tied?
                                      (conj (pop groups) (conj previous entry))
                                      (conj groups [entry]))))
                           groups))
                groups (mapv #(vec (sort-by (comp pr-str :cascade-id) %)) groups)
                ranked-order (vec (mapcat identity groups))
                rank-of (into {}
                              (map-indexed (fn [rank group]
                                             [(set (map :cascade-id group)) (inc rank)])
                                           groups))
                group-for (into {}
                              (mapcat (fn [group]
                                        (map (fn [entry] [(:cascade-id entry) group]) group))
                                      groups))]
            (with-meta
              (mapv (fn [e]
                      (let [group (get group-for (:cascade-id e))
                            ties (map :cascade-id group)
                            resolution (+ (double (get-in (first group)
                                                          [:certificate :g-terms
                                                           :numerical-resolution :value]
                                                          0.0))
                                           (double (get-in e
                                                          [:certificate :g-terms
                                                           :numerical-resolution :value]
                                                          0.0)))
                            rank (get rank-of (set ties))
                            tie-data {:tied-within-resolution (vec ties)
                                      :resolution resolution
                                      :tie-break {:rule :canonical-cascade-id
                                                  :order (vec ties)}}]
                        (cond-> (assoc e :rank rank)
                          (< 1 (count ties))
                          (-> (assoc :g-tie (vec ties))
                              (assoc-in [:certificate :tied-within-resolution] (vec ties))
                              (assoc-in [:certificate :resolution] resolution)
                              (assoc-in [:certificate :tie-break] (:tie-break tie-data))))))
                    ranked-order)
              {:cascade-scoring (cond-> {:model model :scope :synthetic-bounded-replay
                                         :horizon-steps (:horizon-steps opts)
                                         :parallelism workers
                                         :heap-max-bytes (.maxMemory (Runtime/getRuntime))
                                         :worker-memory-budget-bytes worker-memory-budget-bytes
                                         :cache-policy cache-policy
                                         :cold-score (when cold-scoring?
                                                       {:status :cold-scored
                                                        :budget-ms cold-budget-ms
                                                        :elapsed-ms cold-elapsed-ms})
                                         :cache-status (:status cache-read)
                                         :shared-inputs {:generation generation
                                                         :bound shared-bound
                                                         :materiality-margin materiality-margin}}
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
