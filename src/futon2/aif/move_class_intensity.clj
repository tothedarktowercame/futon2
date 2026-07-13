(ns futon2.aif.move-class-intensity
  "M-action-vocabulary P2: dark move-class-conditional value terms.

   The formulas implement the ft-action-vocabulary-005 P1 boxes:
   advance x (1 - resolvedness), close x resolvedness, survey for stale-early
   targets, and apply-cascade from escrowed coverage-dG. The scorer is pure and
   opt-in; production ranking only consumes it when `efe/compute-efe` is called
   with :move-class-intensity-mode :v1.")

(def stale-half-life-days
  "Age at which the normalized staleness signal reaches 0.5."
  7.0)

(def diversity-bonus-per-extra-class 0.1)
(def diversity-bonus-cap 0.3)

(defn clamp01 [x]
  (-> (double (or x 0.0))
      (max 0.0)
      (min 1.0)))

(defn action-class
  [action]
  (case (:type action)
    (:advance-mission :open-mission) :advance
    (:close :close-mission :close-hole) :close
    (:survey :survey-mission) :survey
    :apply-cascade :apply-cascade
    nil))

(defn metric-weight
  "W1, the substrate metric weight for the target. The action's ordinary
   :weight is the current mission-substrate carrier; explicit W1 keys win."
  [action]
  (double (or (:W1 action) (:w1 action) (:weight action) 1.0)))

(defn kappa
  "Use explicit substrate curvature when present. Missing kappa defaults to
   1.0 so the dark path remains auditable on today's action maps."
  [action]
  (Math/abs
   (double (or (:kappa action)
               (:min-kappa action)
               (:curvature/min-incident-kappa action)
               1.0))))

(defn resolvedness
  "Resolvedness in [0,1].

   Explicit substrate fields win. Today's mission substrate supplies
   :open-hole-count; v1 maps remaining work N to 1/(1+N), so zero visible
   holes is saturated and many holes is early/open."
  [action]
  (cond
    (some? (:resolvedness action))
    (clamp01 (:resolvedness action))

    (some? (:resolution-state/resolvedness action))
    (clamp01 (:resolution-state/resolvedness action))

    (number? (:open-hole-count action))
    (/ 1.0 (inc (double (:open-hole-count action))))

    :else
    0.0))

(defn- age-days
  [action]
  (when-let [then-ms (or (:last-touched-ms action)
                        (:mtime-ms action)
                        (:updated-at-ms action))]
    (let [now-ms (or (:now-ms action) (System/currentTimeMillis))]
      (max 0.0 (/ (- (double now-ms) (double then-ms))
                  86400000.0)))))

(defn staleness
  "Staleness in [0,1].

   Explicit :staleness wins. :staleness-days and mtime-like millisecond fields
   use days/(days+7), a bounded half-life curve with no magic cliff."
  [action]
  (cond
    (some? (:staleness action))
    (clamp01 (:staleness action))

    (some? (:staleness-days action))
    (let [d (max 0.0 (double (:staleness-days action)))]
      (/ d (+ d stale-half-life-days)))

    (age-days action)
    (let [d (age-days action)]
      (/ d (+ d stale-half-life-days)))

    :else
    0.0))

(defn escrowed-dg
  [action]
  (or (:escrowed-dG action)
      (:escrow-dG action)
      (:coverage-dG action)
      (get-in action [:fold-escrow :coverage-score-delta])
      (get-in action [:fold-escrow :coverage-score-delta])
      (:coverage-score-delta action)))

(defn intensity
  "Return a self-describing intensity map for ACTION, or nil when the action
   class is outside this vocabulary."
  [action]
  (when-let [class (action-class action)]
    (let [r (resolvedness action)
          s (staleness action)
          k (kappa action)
          w (metric-weight action)
          base (* k w)
          value (case class
                  :advance (* base (- 1.0 r))
                  :close (* base r)
                  :survey (* base s (- 1.0 r))
                  :apply-cascade (max 0.0 (- (double (or (escrowed-dg action) 0.0)))))]
      {:class class
       :value (double value)
       :components (cond-> {:resolvedness r
                            :staleness s
                            :kappa k
                            :W1 w
                            :formula class}
                     (= class :apply-cascade)
                     (assoc :escrowed-dG (escrowed-dg action)))
       :conditioning (cond-> {:source :action-map}
                       (:target action) (assoc :target (:target action))
                       (:mission-path action) (assoc :mission-path (:mission-path action)))})))

(defn intensity-value [action]
  (double (or (:value (intensity action)) 0.0)))

(defn score-bundle
  "Composition-aware portfolio score for a bundle of action maps or precomputed
   intensity maps. Higher :score is better; callers that need G units subtract
   it from controller-score.

   v1 composition rule:
   - within each class, sorted values earn diminishing returns v/(rank+1);
   - mixed portfolios get a small diversity multiplier, capped at +30%;
   - optional :budget penalizes overrun by raw cost units.
   This keeps the portfolio valued as a portfolio without introducing an
   ungrounded exchange-rate table."
  ([items] (score-bundle items {}))
  ([items {:keys [budget costs] :or {costs {}}}]
   (let [ints (vec (keep #(if (and (map? %) (:class %) (contains? % :value))
                            %
                            (intensity %))
                         items))
         by-class (group-by :class ints)
         class-score (fn [xs]
                       (reduce + 0.0
                               (map-indexed (fn [idx x]
                                              (/ (double (:value x))
                                                 (inc idx)))
                                            (sort-by (comp - :value) xs))))
         subtotal (reduce + 0.0 (map class-score (vals by-class)))
         n-classes (count by-class)
         diversity (min diversity-bonus-cap
                        (* diversity-bonus-per-extra-class
                           (max 0 (dec n-classes))))
         gross (* subtotal (+ 1.0 diversity))
         cost (reduce + 0.0
                      (map #(double (or (:cost %)
                                        (get costs (:class %) 1.0)))
                           ints))
         overrun (if (and budget (> cost (double budget)))
                   (- cost (double budget))
                   0.0)
         score (- gross overrun)]
     {:score score
      :subtotal subtotal
      :diversity-multiplier (+ 1.0 diversity)
      :budget budget
      :cost cost
      :overrun overrun
      :classes (set (keys by-class))
      :items ints
      :conditioning {:merge-rule :diminishing-returns-plus-diversity
                     :unit "positive move-class value; subtract from G for ranking"}})))
