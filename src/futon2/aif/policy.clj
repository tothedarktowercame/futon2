(ns futon2.aif.policy
  "Action selection policy for the WM AIF apparatus.

   `select-action` is the top-level R6 deliverable: take a ranked-action
   list (from `efe/rank-actions`), apply softmax over controller-scores with
   adaptive temperature τ, and return either the chosen action or an
   abstain branch with a structured gap-report.

   Contract: contributes to R6 (softmax action selection with abstain)
   per `futon2/docs/futon-aif-completeness.md`. Cross-maps to F6
   (operator inhabitation) — the operator-facing output of the WM IS
   the result of `select-action`.

   Theory: AIF softmax selection — `P(a) ∝ exp(−G(a) / τ)`. Adaptive
   τ scales with EFE spread: tight spreads → high τ → diffuse selection
   → abstain trips. Abstain semantics: when the best action's controller-score is
   not meaningfully below `:no-op`'s controller-score, the WM declines to act
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

(defn effective-temperature
  "The selection temperature actually used. TWO layers, separated per the R6
   faithfulness audit (M-aif-faithfulness §2.2 B-2d):

     g        — R14 engineering outcome-feedback selection gain. High g ⇒
                lower τ_eff ⇒ sharper commitment; low g ⇒ flatter. It is
                not variational policy precision.
     τ_spread — `adaptive-temperature` = range(G)/k: an adaptive-calibration
                heuristic (tight spread → diffuse selection → abstain trips)
                historically STACKED on g. Not part of the canonical form; now
                a separately-justified layer that can be switched off.

   `:tau-mode` in the opt-map selects the layering:

     :spread (DEFAULT)   τ_eff = τ_spread / g — byte-identical to the
                         historical stacked behaviour.
     :selection-gain-only τ_eff = 1 / g — fixed-baseline controller mode; the spread
                         calibration layer is OFF.

   g is floored at `tau-min` defensively in both modes so a degenerate g can
   never divide τ to zero or flip its sign. g = 1.0 (the default and burn-in
   prior) reduces :spread EXACTLY to the spread-only temperature, and
   :selection-gain-only to τ_eff = 1.

   Both modes are engineering calibration policies and are reported as such."
  ([g-totals selection-gain] (effective-temperature g-totals selection-gain {}))
  ([g-totals selection-gain {:keys [tau-min tau-mode]
                    :or {tau-min 0.01 tau-mode :spread}
                    :as temperature-opts}]
   (let [g (max (double tau-min) (double selection-gain))]
     (case tau-mode
       :spread (/ (adaptive-temperature g-totals temperature-opts) g)
       :selection-gain-only (/ 1.0 g)
       (throw (ex-info "unknown :tau-mode (expected :spread or :selection-gain-only)"
                       {:tau-mode tau-mode}))))))

(defn softmax-weights
  "P(a) ∝ exp(ln E(a) − G(a) / τ), normalised to sum to 1.0. Numerically
   stable via the standard log-sum-exp trick.

   The 2-arity form is the historical σ(−G/τ) — equivalently ln E ≡ 0 — and
   is byte-identical to its pre-D-1d behaviour. The 3-arity form is the R12
   HABIT-PRIOR SEAM (M-aif-faithfulness D-1d): `log-priors` aligns with
   `g-totals` and enters the score UNSCALED by τ. The semantic point is that
   controller temperature modulates G, never the habit prior. This seam is
   THE place a future real ln E(π) (R12 per-action-class posteriors) enters —
   do not add a second prior site."
  ([g-totals tau]
   (softmax-weights g-totals tau nil))
  ([g-totals tau log-priors]
   (when (seq g-totals)
     (let [lps (or log-priors (repeat (count g-totals) 0.0))
           scores (mapv (fn [g lp] (+ (/ (- (double g)) (double tau))
                                      (double lp)))
                        g-totals lps)
           max-x (apply max scores)
           exps (mapv #(Math/exp (- % max-x)) scores)
           z (reduce + exps)]
       (mapv #(/ % z) exps)))))

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
       :controller-score (:controller-score chosen)
       :source :default-mode}
      {:action :abstain
       :reason :no-candidates
       :gap-report []
       :source :default-mode})))

(defn- numeric-range [xs]
  (let [xs (mapv double xs)]
    (if (seq xs)
      (let [lo (apply min xs)
            hi (apply max xs)]
        {:min lo :max hi :range (- hi lo)})
      {:min 0.0 :max 0.0 :range 0.0})))

(defn- candidate-explanation [entry idx tau ln-e]
  (let [action (:action entry)
        g (double (:controller-score entry))
        neg-g-over-tau (/ (- g) (double tau))]
    (merge {:action (select-keys action [:type :target :target-class])
            :rank (or (:rank entry) (inc idx))
            :G g
            :neg-G-over-tau neg-g-over-tau
            :lnE (double ln-e)
            :habit-prior-bias (double ln-e)
            :total-score (+ neg-g-over-tau (double ln-e))}
           (select-keys action [:central :strategic :doable
                                :mission-value-factor]))))

(defn- decision-explanation
  [ranked-actions chosen-idx tau selection-gain temperature-opts log-priors
   habit-prior-stats]
  (let [candidates (mapv (fn [idx entry ln-e]
                           (candidate-explanation entry idx tau ln-e))
                         (range)
                         ranked-actions
                         log-priors)
        top-g (apply min-key :G candidates)
        mission-candidates (filterv #(number? (:mission-value-factor %)) candidates)
        top-mission-value (when (seq mission-candidates)
                            (apply max-key :mission-value-factor mission-candidates))
        winner (nth candidates chosen-idx)
        g-span (numeric-range (mapv :G candidates))
        ln-e-span (numeric-range (mapv :lnE candidates))
        scaled-g-span (numeric-range (mapv :neg-G-over-tau candidates))]
    {:winner winner
     :top-G top-g
     :top-mission-value-factor top-mission-value
     :tau-mode (get temperature-opts :tau-mode :spread)
     :tau-effective (double tau)
     :selection-gain (double selection-gain)
     :habit-prior-stats (or habit-prior-stats
                            {:class-count 0 :samples 0})
     :span-diagnostics {:G g-span
                        :lnE ln-e-span
                        :neg-G-over-tau scaled-g-span
                        :range-G (:range g-span)
                        :range-lnE (:range ln-e-span)}
     ;; A prior that changes the argmin-G winner governed the actual choice;
     ;; an aligned prior may have a wide span without deciding the winner.
     :governed-by (if (= winner top-g) :G :habit-prior)}))

(defn select-action
  "Top-level action selection.

   Input: ranked-actions (output of `efe/rank-actions`).
   Optional kwargs:
     :abstain-epsilon — minimum (no-op.controller-score − best.controller-score) required
                        to NOT abstain. Default 0.01.
     :temperature-opts — passed to `adaptive-temperature` AND
                        `effective-temperature`; may carry `:tau-mode`
                        (:spread default | :selection-gain-only — see
                        `effective-temperature`, B-2d τ-layer separation).
     :selection-gain — g, the R14 learned inverse-temperature
                        (`futon2.aif.selection-gain`). τ_eff = τ_spread / g.
                        Default 1.0 ⇒ behaviour identical to the spread-only path.
     :habit-prior-stats — optional sufficient-statistic summary for the
                          decision explanation; it never changes selection.

   Capability-gap preemption policy: a `:learn-action-class` repair may
   preempt mission work only when the proposal/admissibility layers establish
   a live, addressable capability gap whose repair is prerequisite to useful
   mission enactment—the analogue of livelihood preempting cascade work.
   Frequency (`ln E`) is habit, not evidence that a capability is broken.
   The selector compares all admitted actions at the common
   `-G/τ_eff + ln E` seam; operators control prior-vs-G authority through
   selection gain `g` and the habit prior's alpha/decay/span policy. The live
   arena's optional span cap is deliberately default-off: changing that
   default is an operator decision, not an implementation-side rebalance.

   Returns one of:

   Chosen-action branch:
     {:action <action map>
      :rank 1
      :controller-score <number>
      :tau <number>
      :softmax-weights {<action> → <probability>}}

   Abstain branch:
     {:action :abstain
      :reason :no-action-beats-no-op | :no-candidates
      :gap-report <vec of :learn-action-class action maps, possibly empty>
      :ranked-actions <input ranked-actions, for trace>}

   Abstain fires when:
   - ranked-actions is empty, OR
   - :no-op is present AND (no-op.controller-score − best.controller-score) < ε
     (i.e. the best action isn't meaningfully better than doing nothing)."
  ([ranked-actions] (select-action ranked-actions {}))
  ([ranked-actions {:keys [abstain-epsilon temperature-opts selection-gain
                           habit-prior-stats]
                    :or {abstain-epsilon 0.01
                         temperature-opts {}
                         selection-gain 1.0}}]
   (cond
     (empty? ranked-actions)
     {:action :abstain
      :reason :no-candidates
      :gap-report []
      :ranked-actions ranked-actions}

     :else
     ;; D-1d habit-prior seam: entries MAY carry :habit-prior-bias (ln E
     ;; contribution, emitted by compute-efe only under the dark
     ;; :structural-pressure-mode :habit-prior). When every bias is zero —
     ;; the production default — the historical code path below runs
     ;; UNTOUCHED (byte-identity is structural, not numeric luck). When a
     ;; bias exists, choice becomes argmax(ln E − G/τ_eff) — the canonical
     ;; σ(ln E − gG) selection — and the abstain margin is applied to the
     ;; prior-adjusted CHOICE, still in G units (documented flip-memo item:
     ;; abstain semantics keep their G-unit ε; only who gets compared moves).
     (let [log-priors (mapv #(double (or (:habit-prior-bias %) 0.0))
                            ranked-actions)
           priors? (boolean (some #(not (zero? (double %))) log-priors))
           g-totals (mapv :controller-score ranked-actions)
           no-op-entry (find-no-op ranked-actions)
           no-op-g (when no-op-entry (:controller-score no-op-entry))]
       (if-not priors?
         (let [best (first ranked-actions)
               best-g (:controller-score best)
               abstain? (and no-op-entry
                             (< (- no-op-g best-g) abstain-epsilon))]
           (if abstain?
             {:action :abstain
              :reason :no-action-beats-no-op
              :gap-report (gap-report ranked-actions)
              :ranked-actions ranked-actions}
             (let [tau-spread (adaptive-temperature g-totals temperature-opts)
                   tau (effective-temperature g-totals selection-gain temperature-opts)
                   weights (softmax-weights g-totals tau)
                   explanation (decision-explanation
                                ranked-actions 0 tau selection-gain
                                temperature-opts log-priors habit-prior-stats)]
               {:action (:action best)
                :rank 1
                :controller-score best-g
                :tau tau
                :tau-spread tau-spread
                :selection-gain (double selection-gain)
                :decision-explanation explanation
                :softmax-weights (zipmap (mapv :action ranked-actions) weights)})))
         (let [tau-spread (adaptive-temperature g-totals temperature-opts)
               tau (effective-temperature g-totals selection-gain temperature-opts)
               scores (mapv (fn [g lp] (+ (/ (- (double g)) (double tau))
                                          (double lp)))
                            g-totals log-priors)
               chosen-idx (apply max-key scores (range (count scores)))
               chosen (nth ranked-actions chosen-idx)
               chosen-g (:controller-score chosen)
               abstain? (and no-op-entry
                             (< (- no-op-g chosen-g) abstain-epsilon))]
           (if abstain?
             {:action :abstain
              :reason :no-action-beats-no-op
              :gap-report (gap-report ranked-actions)
              :ranked-actions ranked-actions}
             (let [weights (softmax-weights g-totals tau log-priors)
                   explanation (decision-explanation
                                ranked-actions chosen-idx tau selection-gain
                                temperature-opts log-priors habit-prior-stats)]
               {:action (:action chosen)
                :rank (:rank chosen)
                :controller-score chosen-g
                :tau tau
                :tau-spread tau-spread
                :selection-gain (double selection-gain)
                :habit-prior-applied? true
                :decision-explanation explanation
                :softmax-weights (zipmap (mapv :action ranked-actions)
                                         weights)}))))))))
