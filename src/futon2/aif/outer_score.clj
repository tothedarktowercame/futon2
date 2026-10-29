(ns futon2.aif.outer-score
  "Pure task-selection cascade application and expected-free-energy scoring.

  This namespace follows DarkTower.WarMachine.R20Step.  It performs no I/O,
  constructs no field, and carries no state between clicks.")

(def ^:private endings
  [:focused :related :unrelated :stop-the-line :not-yet-evaluated])
(def ^:private gains [:none :one :several])
(def ^:private states [:doable :blocked])
(def ^:private outcomes (vec (for [ending endings gain gains] [ending gain])))

(defn- value-of [x k]
  (if (fn? x) (x k) (get x k)))

(defn- stable-sort-by [keyfn direction xs]
  (->> xs
       (map-indexed (fn [index x] [(direction (keyfn x)) index x]))
       (sort-by #(subvec % 0 2))
       (mapv #(nth % 2))))

(defn- graph-term [facts target term]
  (let [fact (value-of facts target) value (get fact term)]
    (when-not (nat-int? value)
      (throw (ex-info "Numeric ranking requires a known graph term"
                      {:kind :outer-score/graph-term-unavailable :target target :term term
                       :graph-terms (:graph-terms fact)
                       :producer (or (get-in fact [:graph-terms :producer]) :dependency-reader-reviewer)})))
    value))

(defn apply-selection-step
  "Lean `applySelectionStep`: apply one gate or stable ordering using Giraffe facts."
  [facts field step]
  (cond
    (and (map? step) (contains? step :gate))
    (filterv (:gate step) field)

    (and (map? step) (contains? step :order))
    (stable-sort-by (:order step) identity field)

    (= :ready-gate step)
    (filterv #(= :ready (:status (value-of facts (:target %)))) field)

    (and (map? step) (contains? step :gate-status))
    (filterv #(= (:gate-status step)
                 (:status (value-of facts (:target %)))) field)

    (= :by-unblocks step)
    (stable-sort-by #(graph-term facts (:target %) :unblocks) - field)

    (= :by-cone step)
    (stable-sort-by #(graph-term facts (:target %) :cone) - field)

    :else
    (throw (ex-info "Unknown outer selection step"
                    {:kind :outer-score/unknown-selection-step :step step}))))

(defn apply-selection-cascade
  "Lean `applySelectionCascade`: apply selection steps from left to right."
  [cascade facts field]
  (reduce (partial apply-selection-step facts) (vec field) cascade))

(defn cascade-pick
  "Lean `cascadePick`: return the first candidate left by the cascade, or nil."
  [cascade facts field]
  (first (apply-selection-cascade cascade facts field)))

(defn- normalize [counts support]
  (let [total (reduce + 0 (map #(double (or (value-of counts %) 0)) support))]
    (when-not (pos? total)
      (throw (ex-info "Outer count row has no positive mass"
                      {:kind :outer-score/nonpositive-count-total})))
    (into {} (map (fn [x] [x (/ (double (or (value-of counts x) 0)) total)]) support))))

(defn- bounded-reach [reach]
  (max 0.0 (min 1.0 (double reach))))

(defn- ready-gain [unblocks]
  (cond (zero? unblocks) :none (= 1 unblocks) :one :else :several))

(defn- ending-class [relation focus target]
  (case (relation focus target)
    :focus :focused
    :associated :related
    :useful-elsewhere :unrelated
    nil))

(defn- stopped-kernel []
  (into {} (for [state states]
             [state (assoc (zipmap outcomes (repeat 0.0))
                           [:stop-the-line :none] 1.0)])))

(defn- picked-kernel [landing raw-reach]
  (let [reach (bounded-reach raw-reach)
        stopped [:stop-the-line :none]
        row0 (zipmap outcomes (repeat 0.0))
        blocked (assoc row0 stopped 1.0)
        doable (if (= landing stopped)
                 blocked
                 (assoc row0 landing reach stopped (- 1.0 reach)))]
    {:doable doable :blocked blocked}))

(defn outer-model
  "Lean `outerModel`: construct the doable/blocked prior and picked-task kernel.

  Input is `{:focus :relation :facts :field :state-counts :cascade}`."
  [{:keys [focus relation facts field state-counts cascade]}]
  (if-let [candidate (cascade-pick cascade facts field)]
    (let [fact (value-of facts (:target candidate))
          status (:status fact)
          ending (ending-class relation focus (:target candidate))
          landing (if ending [ending (ready-gain (graph-term facts (:target candidate) :unblocks))]
                      [:stop-the-line :none])]
      {:pick candidate
       :status status
       :counts (value-of state-counts status)
       :q (normalize (value-of state-counts status) states)
       :kernel (picked-kernel landing (:reach candidate))
       :landing landing})
    {:pick nil
     :status nil
     :counts {:doable 0 :blocked 1}
     :q {:doable 0.0 :blocked 1.0}
     :kernel (stopped-kernel)
     :landing [:stop-the-line :none]}))

(defn outer-preference
  "Lean `outerPreferenceKernel`: product of normalized ending and gain counts."
  [ending-counts gain-counts]
  (let [ending (normalize ending-counts endings)
        gain (normalize gain-counts gains)]
    (into {} (for [[e g :as outcome] outcomes]
               [outcome (* (ending e) (gain g))]))))

(defn outer-novelty
  "Lean `outerKernelNovelty` / `outerNovelty`: Fisher-information novelty.

  A model with no pick has zero novelty."
  [{:keys [pick counts kernel]}]
  (if-not pick
    0.0
    (let [a (double (value-of counts :doable))
          b (double (value-of counts :blocked))
          total (+ a b)]
      (when-not (and (pos? a) (pos? b))
        (throw (ex-info "Outer novelty requires positive Beta counts"
                        {:kind :outer-score/nonpositive-state-counts :counts counts})))
      (let [variance (/ (* a b) (* total total (inc total)))
            fisher (reduce + 0.0
                           (for [outcome outcomes
                                 :let [ad (get-in kernel [:doable outcome] 0.0)
                                       ab (get-in kernel [:blocked outcome] 0.0)
                                       prediction (/ (+ (* a ad) (* b ab)) total)]
                                 :when (pos? prediction)]
                             (/ (* (- ad ab) (- ad ab)) prediction)))]
        (* 0.5 variance fisher)))))

(defn push
  "Lean `push q a`: marginalize an outer model's state prior through its kernel."
  [{:keys [q kernel]}]
  (into {} (for [outcome outcomes]
             [outcome (reduce + 0.0
                              (for [state states]
                                (* (q state) (get-in kernel [state outcome] 0.0))))])))

(defn- risk [prediction preference]
  (if (some (fn [[outcome mass]]
              (and (pos? mass) (not (pos? (get preference outcome 0.0)))))
            prediction)
    :inadmissible
    (reduce + 0.0
            (for [[outcome mass] prediction :when (pos? mass)]
              (* mass (Math/log (/ mass (preference outcome))))))))

(defn- entropy [row]
  (- (reduce + 0.0 (for [[_ mass] row :when (pos? mass)]
                         (* mass (Math/log mass))))))

(defn- ambiguity [{:keys [q kernel]}]
  (reduce + 0.0 (for [state states] (* (q state) (entropy (kernel state))))))

(defn outer-score
  "Lean `outerScore`: risk plus ambiguity minus `outerNovelty`.

  Returns `:inadmissible`, the Clojure counterpart of Lean `⊤`, when a
  predicted positive outcome has zero preference mass.  Risk and ambiguity
  are evaluated directly from the Lean finite sums: `cascade-g/step-g`
  requires model authority/provenance which this pure, caller-supplied outer
  model must not fabricate."
  [{:keys [ending-counts gain-counts] :as input}]
  (let [model (outer-model input)
        preference (outer-preference ending-counts gain-counts)
        r (risk (push model) preference)]
    (if (= :inadmissible r)
      :inadmissible
      (- (+ r (ambiguity model)) (outer-novelty model)))))
