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

(defn effective-temperature
  "The selection temperature actually used. TWO layers, separated per the R6
   faithfulness audit (M-aif-faithfulness §2.2 B-2d):

     γ        — R14 precision-over-policies: the agent's learned confidence in
                its own decision-making (`futon2.aif.policy-precision`). In the
                canonical AIF form P(π) = σ(−γ·G), γ ALONE carries selection
                sharpness. High γ ⇒ lower τ_eff ⇒ sharper commitment; low γ ⇒
                flatter / abstain-leaning.
     τ_spread — `adaptive-temperature` = range(G)/k: an adaptive-calibration
                heuristic (tight spread → diffuse selection → abstain trips)
                historically STACKED on γ. Not part of the canonical form; now
                a separately-justified layer that can be switched off.

   `:tau-mode` in the opt-map selects the layering:

     :spread (DEFAULT)   τ_eff = τ_spread / γ — byte-identical to the
                         historical stacked behaviour.
     :gamma-only         τ_eff = 1 / γ — the canonical form; the spread
                         calibration layer is OFF.

   γ is floored at `tau-min` defensively in both modes so a degenerate γ can
   never divide τ to zero or flip its sign. γ = 1.0 (the default and burn-in
   prior) reduces :spread EXACTLY to the spread-only temperature, and
   :gamma-only to τ_eff = 1.

   HONESTY (B-2d dark build, 2026-07-04): the arena default is :spread — live
   selection behaviour is UNCHANGED by this separation. :gamma-only is built
   dark behind `arena-tau-mode` (scripts/futon2/report/war_machine.clj; env
   hatch FUTON_WM_TAU_MODE=gamma-only). The flip is Joe's (§2.1 verdict
   ledger). Evidence a flip decision needs: an E6-style shadow comparison over
   the trace corpus — chosen action, abstain rate, and softmax entropy under
   both modes side by side — decided JOINTLY with B-3b (R14 γ β-update): both
   layers shape selection sharpness, so the spread layer should not switch off
   in the same step γ starts moving off 1.0, or the two effects on selection
   are unattributable."
  ([g-totals gamma] (effective-temperature g-totals gamma {}))
  ([g-totals gamma {:keys [tau-min tau-mode]
                    :or {tau-min 0.01 tau-mode :spread}
                    :as temperature-opts}]
   (let [g (max (double tau-min) (double gamma))]
     (case tau-mode
       :spread (/ (adaptive-temperature g-totals temperature-opts) g)
       :gamma-only (/ 1.0 g)
       (throw (ex-info "unknown :tau-mode (expected :spread or :gamma-only)"
                       {:tau-mode tau-mode}))))))

(defn softmax-weights
  "P(a) ∝ exp(ln E(a) − G(a) / τ), normalised to sum to 1.0. Numerically
   stable via the standard log-sum-exp trick.

   The 2-arity form is the historical σ(−G/τ) — equivalently ln E ≡ 0 — and
   is byte-identical to its pre-D-1d behaviour. The 3-arity form is the R12
   HABIT-PRIOR SEAM (M-aif-faithfulness D-1d): `log-priors` aligns with
   `g-totals` and enters the score UNSCALED by τ — that is the canonical
   σ(ln E − γ·G) shape, and the semantic point of relocating a term here:
   precision (γ = 1/τ_eff) modulates G, never the habit prior. This seam is
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
     :temperature-opts — passed to `adaptive-temperature` AND
                        `effective-temperature`; may carry `:tau-mode`
                        (:spread default | :gamma-only — see
                        `effective-temperature`, B-2d τ-layer separation).
     :policy-precision — γ, the R14 learned inverse-temperature
                        (`futon2.aif.policy-precision`). τ_eff = τ_spread / γ.
                        Default 1.0 ⇒ behaviour identical to the spread-only path.

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
  ([ranked-actions {:keys [abstain-epsilon temperature-opts policy-precision]
                    :or {abstain-epsilon 0.01
                         temperature-opts {}
                         policy-precision 1.0}}]
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
     ;; σ(ln E − γG) selection — and the abstain margin is applied to the
     ;; prior-adjusted CHOICE, still in G units (documented flip-memo item:
     ;; abstain semantics keep their G-unit ε; only who gets compared moves).
     (let [log-priors (mapv #(double (or (:habit-prior-bias %) 0.0))
                            ranked-actions)
           priors? (boolean (some #(not (zero? (double %))) log-priors))
           g-totals (mapv :G-total ranked-actions)
           no-op-entry (find-no-op ranked-actions)
           no-op-g (when no-op-entry (:G-total no-op-entry))]
       (if-not priors?
         (let [best (first ranked-actions)
               best-g (:G-total best)
               abstain? (and no-op-entry
                             (< (- no-op-g best-g) abstain-epsilon))]
           (if abstain?
             {:action :abstain
              :reason :no-action-beats-no-op
              :gap-report (gap-report ranked-actions)
              :ranked-actions ranked-actions}
             (let [tau-spread (adaptive-temperature g-totals temperature-opts)
                   tau (effective-temperature g-totals policy-precision temperature-opts)
                   weights (softmax-weights g-totals tau)]
               {:action (:action best)
                :rank 1
                :G-total best-g
                :tau tau
                :tau-spread tau-spread
                :policy-precision (double policy-precision)
                :softmax-weights (zipmap (mapv :action ranked-actions) weights)})))
         (let [tau-spread (adaptive-temperature g-totals temperature-opts)
               tau (effective-temperature g-totals policy-precision temperature-opts)
               scores (mapv (fn [g lp] (+ (/ (- (double g)) (double tau))
                                          (double lp)))
                            g-totals log-priors)
               chosen-idx (apply max-key scores (range (count scores)))
               chosen (nth ranked-actions chosen-idx)
               chosen-g (:G-total chosen)
               abstain? (and no-op-entry
                             (< (- no-op-g chosen-g) abstain-epsilon))]
           (if abstain?
             {:action :abstain
              :reason :no-action-beats-no-op
              :gap-report (gap-report ranked-actions)
              :ranked-actions ranked-actions}
             (let [weights (softmax-weights g-totals tau log-priors)]
               {:action (:action chosen)
                :rank (:rank chosen)
                :G-total chosen-g
                :tau tau
                :tau-spread tau-spread
                :policy-precision (double policy-precision)
                :habit-prior-applied? true
                :softmax-weights (zipmap (mapv :action ranked-actions)
                                         weights)}))))))))
