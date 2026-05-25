(ns futon2.aif.policy
  "Action selection policy for the WM AIF apparatus.

   `select-action` is the top-level R6 deliverable: take a ranked-action
   list (from `efe/rank-actions`), apply softmax over G-totals with
   adaptive temperature τ, and return either the chosen action or an
   abstain branch with a structured gap-report.

   Contract: contributes to R6 (softmax action selection with abstain)
   per `futon2/docs/futon-aif-completeness.md`. Cross-maps to F6
   (operator inhabitation) — the operator-facing output of the WM IS
   the result of `select-action`.

   Theory: AIF softmax selection — `P(a) ∝ exp(−G(a) / τ)`. Adaptive
   τ scales with EFE spread: tight spreads → high τ → diffuse selection
   → abstain trips. Abstain semantics: when the best action's G-total is
   not meaningfully below `:no-op`'s G-total, the WM declines to act
   and surfaces a gap-report enumerating the `:learn-action-class`
   recommendations the bootstrap proposer detected.")

(defn adaptive-temperature
  "Compute τ from the EFE spread of a candidate set. High spread → low
   τ (sharp pick); tight spread → high τ (diffuse / abstain-leaning).
   Floored at `tau-min` so degenerate (identical) EFE inputs don't divide
   by zero downstream.

   Defaults: tau-min 0.01, k 5.0. Both tunable via opt-map."
  ([g-totals] (adaptive-temperature g-totals {}))
  ([g-totals {:keys [tau-min k] :or {tau-min 0.01 k 5.0}}]
   (if (empty? g-totals)
     tau-min
     (let [spread (- (apply max g-totals) (apply min g-totals))]
       (max tau-min (/ spread k))))))

(defn softmax-weights
  "P(a) ∝ exp(−G(a) / τ), normalised to sum to 1.0. Numerically stable
   via the standard log-sum-exp trick."
  [g-totals tau]
  (when (seq g-totals)
    (let [neg-g-over-tau (mapv #(/ (- (double %)) (double tau)) g-totals)
          max-x (apply max neg-g-over-tau)
          exps (mapv #(Math/exp (- % max-x)) neg-g-over-tau)
          z (reduce + exps)]
      (mapv #(/ % z) exps))))

(defn- find-no-op
  "Locate the :no-op entry in a ranked-action list, or nil."
  [ranked]
  (first (filter #(= :no-op (-> % :action :type)) ranked)))

(defn- gap-report
  "Enumerate the :learn-action-class recommendations in a ranked-action
   list (their action maps), in order of appearance. Used by abstain to
   surface capability gaps to the operator."
  [ranked]
  (->> ranked
       (filter #(= :learn-action-class (-> % :action :type)))
       (mapv :action)))

(defn default-mode-select
  "Pre-deliberative tropism-based action selection from observation +
   ranked candidates. Used as I6 compositional closure when the
   deliberative EFE path is unavailable or returns a degenerate result.

   Ports the structural shape from `~/code/futon2/src/ants/aif/default_mode.clj`:
   accepts observation alone (plus the proposer's candidate set), no EFE
   weights, no preferences, simple rule-based dispatch.

   For v0.13 WM (no concrete action substrate beyond sorries):

   1. If `:sorry-count-norm > 0.3` in observation AND `:address-sorry`
      candidates exist → recommend the first one (sorry pressure dominates).
   2. Else if any `:learn-action-class` candidates exist → recommend the
      one with highest `:intrinsic-value` (capability-gap-closing
      remains the meaningful work).
   3. Else if any `:address-sorry` candidates exist → recommend the first
      (even at low sorry pressure, addressable work is better than no-op).
   4. Else `:no-op`.

   Returns the same chosen-action shape as `select-action` (with
   `:source :default-mode`), or an abstain branch if there are
   genuinely no candidates."
  [state ranked-actions]
  (let [observation (:observation state {})
        sorry-pressure (double (get observation :sorry-count-norm 0.0))
        actions-by-type (group-by #(-> % :action :type) ranked-actions)
        addr-sorrys (get actions-by-type :address-sorry [])
        learns (get actions-by-type :learn-action-class [])
        no-ops (get actions-by-type :no-op [])
        chosen (cond
                 (and (> sorry-pressure 0.3) (seq addr-sorrys))
                 (first addr-sorrys)

                 (seq learns)
                 (apply max-key #(double (or (-> % :action :intrinsic-value) 0))
                        learns)

                 (seq addr-sorrys) (first addr-sorrys)
                 (seq no-ops) (first no-ops)
                 :else nil)]
    (if chosen
      {:action (:action chosen)
       :rank (:rank chosen)
       :G-total (:G-total chosen)
       :source :default-mode}
      {:action :abstain
       :reason :no-candidates
       :gap-report []
       :source :default-mode})))

(defn select-action
  "Top-level action selection.

   Input: ranked-actions (output of `efe/rank-actions`).
   Optional kwargs:
     :abstain-epsilon — minimum (no-op.G-total − best.G-total) required
                        to NOT abstain. Default 0.01.
     :temperature-opts — passed to `adaptive-temperature`.

   Returns one of:

   Chosen-action branch:
     {:action <action map>
      :rank 1
      :G-total <number>
      :tau <number>
      :softmax-weights {<action> → <probability>}}

   Abstain branch:
     {:action :abstain
      :reason :no-action-beats-no-op | :no-candidates
      :gap-report <vec of :learn-action-class action maps, possibly empty>
      :ranked-actions <input ranked-actions, for trace>}

   Abstain fires when:
   - ranked-actions is empty, OR
   - :no-op is present AND (no-op.G-total − best.G-total) < ε
     (i.e. the best action isn't meaningfully better than doing nothing)."
  ([ranked-actions] (select-action ranked-actions {}))
  ([ranked-actions {:keys [abstain-epsilon temperature-opts]
                    :or {abstain-epsilon 0.01
                         temperature-opts {}}}]
   (cond
     (empty? ranked-actions)
     {:action :abstain
      :reason :no-candidates
      :gap-report []
      :ranked-actions ranked-actions}

     :else
     (let [best (first ranked-actions)
           no-op-entry (find-no-op ranked-actions)
           best-g (:G-total best)
           no-op-g (when no-op-entry (:G-total no-op-entry))
           abstain? (and no-op-entry
                         (< (- no-op-g best-g) abstain-epsilon))]
       (if abstain?
         {:action :abstain
          :reason :no-action-beats-no-op
          :gap-report (gap-report ranked-actions)
          :ranked-actions ranked-actions}
         (let [g-totals (mapv :G-total ranked-actions)
               tau (adaptive-temperature g-totals temperature-opts)
               weights (softmax-weights g-totals tau)]
           {:action (:action best)
            :rank 1
            :G-total best-g
            :tau tau
            :softmax-weights (zipmap (mapv :action ranked-actions) weights)}))))))
