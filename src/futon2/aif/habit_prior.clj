(ns futon2.aif.habit-prior
  "A learned categorical habit prior E(π) from observed policy frequencies.

   This namespace is deliberately small and pure. It treats each stable action
   identity as a category in a symmetric Dirichlet-multinomial model. The
   posterior predictive mean is

     E(π=i | Π_feasible) = (n_i + α) / Σ_{j∈Π_feasible}(n_j + α)

   and `log-priors` returns ln E for the candidate menu consumed by
   `futon2.aif.policy/softmax-weights`.

   HONESTY: this is a learned habit/frequency prior, not evidence that a policy
   is good. v1 uses α=1 and no recency decay. Abstentions and malformed trace
   decisions add no count. The dark arena wiring must replace—not add to—the
   caller-supplied structural-pressure bias when enabled.

   POLICY: capability repair preempts mission work only when the capability is
   a live prerequisite for useful mission enactment. Frequency alone cannot
   establish that fact. Selection gain g and the prior's alpha/decay/span set
   the numerical prior-vs-G balance; the optional span cap is default-off, and
   flipping its default requires an explicit operator (Joe) decision.")

(def default-alpha 1.0)
(def state-version 1)

(defn policy-key
  "Stable categorical identity for an action. Volatile rationale, scores and
   cascade payloads are intentionally excluded. Returns nil for abstention or
   a malformed action."
  [action]
  (when (and (map? action) (keyword? (:type action)))
    [(:type action)
     (cond
       (some? (:target action)) [:target (:target action)]
       (some? (:target-class action)) [:target-class (:target-class action)]
       :else [:unscoped nil])]))

(defn initial-state
  ([] (initial-state {}))
  ([{:keys [alpha] :or {alpha default-alpha}}]
   (let [a (double alpha)]
     (when-not (and (Double/isFinite a) (pos? a))
       (throw (ex-info "habit-prior alpha must be positive" {:alpha alpha})))
     {:version state-version
      :alpha a
      :counts {}
      :samples 0
      :recency-decay :none})))

(defn coerce-state
  "Accept a persisted v1 state. Nil is a cold start; any present malformed
   state fails closed rather than silently discarding learned history."
  [state]
  (cond
    (nil? state) (initial-state)
    (and (= state-version (:version state))
         (number? (:alpha state))
         (Double/isFinite (double (:alpha state)))
         (pos? (double (:alpha state)))
         (map? (:counts state))
         (every? (fn [[k n]] (and (vector? k) (integer? n) (not (neg? n))))
                 (:counts state))
         (integer? (:samples state)) (not (neg? (:samples state)))
         (= (:samples state) (reduce + 0 (vals (:counts state)))))
    state
    :else
    (throw (ex-info "malformed persisted habit-prior state" {:state state}))))

(defn observe-action
  "Fold one selected action into the Dirichlet count state. Nil, abstention and
   malformed decisions are reduction-safe no-ops."
  [state action]
  (let [state (coerce-state state)]
    (if-let [k (policy-key action)]
      (-> state
          (update-in [:counts k] (fnil inc 0))
          (update :samples inc))
      state)))

(defn fold-record
  "Fold one trace record's selected action."
  [state record]
  (observe-action state (get-in record [:decision :action])))

(defn fold-records
  "Chronologically fold trace records into a learned habit-prior state."
  ([records] (fold-records (initial-state) records))
  ([state records] (reduce fold-record (coerce-state state) records)))

(defn state-stats
  "Compact sufficient statistics for selection-time decision explanations."
  [state]
  (let [{:keys [alpha counts samples recency-decay]} (coerce-state state)]
    {:class-count (count counts)
     :samples samples
     :alpha alpha
     :recency-decay recency-decay}))

(defn log-priors
  "Return a vector of ln posterior-predictive probabilities aligned with
   `actions`. Historical counts supply the concentrations; normalization is
   over the currently feasible menu. Duplicate action identities divide their
   category mass evenly, so the returned probabilities still sum to one."
  [state actions]
  (let [{:keys [alpha counts]} (coerce-state state)
        candidate-keys (mapv policy-key actions)
        _ (when (some nil? candidate-keys)
            (throw (ex-info "habit-prior candidate has no stable policy identity"
                            {:actions actions})))
        multiplicities (frequencies candidate-keys)
        menu (keys multiplicities)
        denominator (reduce + 0.0
                            (map #(+ (double (get counts % 0)) alpha) menu))]
    (mapv (fn [key]
            (Math/log (/ (/ (+ (double (get counts key 0)) alpha)
                              (double (get multiplicities key)))
                         denominator)))
          candidate-keys)))

(defn attach-log-priors
  "Replace candidate `:habit-prior-bias` values with learned ln E(π).

   The two-arity form is the historical/default behavior. The optional
   `:span-ratio-cap` bounds range(ln E) to R × range(G), preserving ordering
   and the prior maximum while compressing its span. This makes the
   prior-vs-G balance arena-configurable without silently changing today's
   policy. The cap is DEFAULT-OFF; changing that default is an operator (Joe)
   decision."
  ([state ranked-actions]
   (let [biases (log-priors state (mapv :action ranked-actions))]
     (mapv (fn [entry bias]
             (assoc entry
                    :habit-prior-bias bias
                    :habit-prior-source :learned-frequency))
           ranked-actions biases)))
  ([state ranked-actions {:keys [span-ratio-cap]}]
   (if (nil? span-ratio-cap)
     (attach-log-priors state ranked-actions)
     (let [ratio (double span-ratio-cap)
           _ (when-not (and (Double/isFinite ratio) (not (neg? ratio)))
               (throw (ex-info "habit-prior span-ratio-cap must be finite and nonnegative"
                               {:span-ratio-cap span-ratio-cap})))
           raw-biases (log-priors state (mapv :action ranked-actions))
           g-values (mapv (comp double :controller-score) ranked-actions)
           g-span (if (seq g-values)
                    (- (apply max g-values) (apply min g-values))
                    0.0)
           bias-span (if (seq raw-biases)
                       (- (apply max raw-biases) (apply min raw-biases))
                       0.0)
           allowed-span (* ratio g-span)
           scale (if (pos? bias-span)
                   (min 1.0 (/ allowed-span bias-span))
                   1.0)
           bias-max (if (seq raw-biases) (apply max raw-biases) 0.0)
           biases (mapv #(+ bias-max (* scale (- (double %) bias-max)))
                        raw-biases)]
       (mapv (fn [entry bias]
               (assoc entry
                      :habit-prior-bias bias
                      :habit-prior-source :learned-frequency
                      :habit-prior-span-ratio-cap ratio))
             ranked-actions biases)))))
