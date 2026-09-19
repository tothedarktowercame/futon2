(ns futon2.aif.policy
  "Action selection policy for the WM AIF apparatus.

   `select-action-cascades` is the production selector: G is computed over
   cascade POLICIES (a cascade is a policy), and the enacted choice is the
   Bayes choice over each cascade's first acting pattern at a caller-DECLARED
   β. The flat single-action selectors (`select-action`,
   `strategic-recommendation`, `default-mode-select`) were removed on
   2026-09-17 by Joe's ruling; `futon2.aif.flat-path-gate-test` refuses any
   call site to them in production source.

   The general score/softmax seam (`selection-scores`, `softmax-weights`)
   survives: it is grain-agnostic and is used by the cascade-grain shadow
   lane (`futon2.aif.cascade-prior/shadow-rank`), not only by the retired
   flat law.

   Contract: contributes to R6 (softmax action selection) per
   `futon2/docs/futon-aif-completeness.md`."
  (:require [futon2.aif.g-term-decomposition :as decomposition]
            [futon2.aif.hierarchical-budget :as hierarchical-budget]
            [futon2.aif.cascade-selection :as cascade-selection]))

(defn select-budgeted-actions
  "R11 policy boundary for collective, hierarchical action selection.

   This is separate from `select-action`, whose output is intentionally one
   controller-head recommendation. Local agents submit finite proposal fields;
   the shared-budget arbiter returns a jointly feasible portfolio plus a
   per-node usage witness."
  [hierarchy]
  (hierarchical-budget/arbitrate hierarchy))

(defn- finite-pos?
  "A usable temperature: a number, finite, strictly positive. Mirrors
   `policy-precision/finite-number?` — the same guard the beta carry applies to
   a state read back from a trace, applied again at the point of use so a beta
   that reached here through some other route cannot become a τ."
  [x]
  (and (number? x)
       (let [d (double x)]
         (and (not (Double/isNaN d)) (not (Double/isInfinite d)) (pos? d)))))

(defn- normalise-scores
  "exp/normalise a score vector, numerically stable via the standard
   log-sum-exp trick. The one place the softmax is written."
  [scores]
  (let [max-x (apply max scores)
        exps (mapv #(Math/exp (- % max-x)) scores)
        z (reduce + exps)]
    (mapv #(/ % z) exps)))

(defn selection-scores
  "The per-candidate selection score ln E(a) − G(a)/τ [− F_pi(a)], aligned
   with `g-totals`. THE ONE PLACE THE SCORE EXPRESSION IS WRITTEN.

   `softmax-weights` normalises this and `strategic-recommendation` takes its
   argmax under `:selection-law :full-score-posterior` (U10). Both read the
   same vector, so the posterior a tick RECORDS and the score a tick may CHOOSE
   by cannot drift into two expressions — which is the failure U1 readiness
   point 4 found on the other side of the seam, where the recorded posterior
   carried F_pi and the choice was made by a different rule entirely.

   The opts are the ones `softmax-weights` takes:
     :f-pi-policy-posterior?  flag, DEFAULT false
     :f-pi-values             values aligned with g-totals when enabled
     :f-pi-scaling            :unscaled (DEFAULT, score subtracts F_pi) or
                              :by-tau (score subtracts F_pi / tau)

   Disabled means the old score expression is evaluated without inspecting any
   F_pi option. Enabled input must be numeric and exactly aligned; a misaligned,
   non-numeric or unknown-scaling input throws here rather than scoring one
   candidate against another candidate's fit.

   `log-priors` nil means ln E ≡ 0. ln E enters UNSCALED by τ: controller
   temperature modulates G, never the habit prior.

   Returns nil for an empty candidate list, as `softmax-weights` always has."
  ([g-totals tau log-priors] (selection-scores g-totals tau log-priors {}))
  ([g-totals tau log-priors {:keys [f-pi-policy-posterior? f-pi-values
                                    f-pi-scaling]
                             :or {f-pi-policy-posterior? false
                                  f-pi-scaling :unscaled}}]
   (when-not (finite-pos? tau)
     (let [kind (if (and (number? tau) (Double/isFinite (double tau)))
                  :nonpositive-temperature
                  :nonfinite-temperature)]
       (throw (ex-info (name kind)
                       {:refusal {:kind kind :temperature tau}}))))
   (when (seq g-totals)
     (let [n (count g-totals)
           lps (or log-priors (repeat n 0.0))
           _ (when (and f-pi-policy-posterior?
                        (not (contains? #{:unscaled :by-tau} f-pi-scaling)))
               (throw (ex-info "unknown :f-pi-scaling"
                               {:f-pi-scaling f-pi-scaling})))
           _ (when (and f-pi-policy-posterior?
                        (not= n (count f-pi-values)))
               (throw (ex-info ":f-pi-values must align with g-totals"
                               {:g-count n :f-pi-count (count f-pi-values)})))
           f-terms (when f-pi-policy-posterior?
                     (mapv (fn [f-pi]
                             (when-not (number? f-pi)
                               (throw (ex-info ":f-pi-values must be numeric"
                                               {:f-pi f-pi})))
                             (case f-pi-scaling
                               :unscaled (double f-pi)
                               :by-tau (/ (double f-pi) (double tau))))
                           f-pi-values))]
       (mapv (fn [idx g lp]
               (cond-> (+ (/ (- (double g)) (double tau))
                          (double lp))
                 f-pi-policy-posterior?
                 (- (nth f-terms idx))))
             (range n) g-totals lps)))))

(defn softmax-weights
  "P(a) ∝ exp(ln E(a) − G(a) / τ), optionally including horizon-one
   observed-data free energy F_pi at this same posterior seam. Normalised to
   sum to 1.0 and numerically stable via the standard log-sum-exp trick.

   The score itself is `selection-scores`; this is that vector normalised, and
   the four-arity opts are passed straight through to it.

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
   (softmax-weights g-totals tau log-priors {}))
  ([g-totals tau log-priors opts]
   (some-> (selection-scores g-totals tau log-priors opts) normalise-scores)))

;; ---------------------------------------------------------------------------
;; Cascade-candidate selection (R14 requirement, tick 1 of the virtual WM)
;; ---------------------------------------------------------------------------

(defn- cascade-first-action
  "The action a ranked cascade entry contributes to the action marginal at
  this step: its first acting pattern; a non-cascade entry (e.g. the explicit
  no-op) contributes its :type. Purely additive helper of
  `select-action-cascades`."
  [action]
  (if (and (map? action) (seq (:precedence action)))
    (first (:precedence action))
    (if (map? action) (:type action) action)))

(defn- selection-input
  "Record the historical neutral-input rule, including present null/false.
   These cases consume the same value but are not the same observation."
  [entry field neutral]
  (let [value (get entry field)
        presence (cond
                   (not (contains? entry field)) :absent
                   (nil? value) :null
                   (false? value) :false
                   :else :present)]
    {:presence presence
     :supplied-value value
     :value (if (= :present presence) value neutral)
     :status (if (= :present presence) :attached :declared-neutral)
     :reason (when-not (= :present presence) presence)}))

(defn- selection-candidate
  [entry]
  (let [habit (cond-> (selection-input entry :habit 1)
                (= :neutral-fallback (get-in entry [:habit-provenance :source]))
                (assoc :status :declared-neutral
                       :reason (get-in entry [:habit-provenance :reason])))
        f (selection-input entry :f 0)
        computed-f (get-in entry [:certificate :f])
        unattached? (and (not= :attached (:status f))
                         (= :computed-not-attached (:status computed-f)))
        f-status (if unattached? :computed-not-attached (:status f))]
    {:id (:action entry)
     :habit (:value habit)
     :habit-status (:status habit)
     :habit-provenance (:habit-provenance entry)
     :f (:value f)
     :f-status f-status
     :reason (if unattached? (:reason computed-f) (:reason f))
     :g (:controller-score entry)
     :inputs {:habit habit :f f}
     :computed-f computed-f}))

(defn- selection-certificate
  "One Lean SelectionCertificate per policy; raw computed F stays in the
   accompanying candidates, outside the finite Lean fields. Attached inputs
   map to QuantityStatus.computed; neutral and computedNotAttached retain
   their distinct constructors. This emits evidence, not a runtime gate."
  [beta candidates ranked]
  {:beta {:value beta :status :declared}
   :g-term-decomposition (decomposition/census ranked candidates)
   ;; Retain every candidate's own scorer provenance. Indexing by position
   ;; preserves the exact candidate association even when action names tie.
   :scoring (into (sorted-map)
                  (map-indexed
                   (fn [i entry]
                     [i (assoc (select-keys (:certificate entry) [:c :rates-provenance])
                               :id (:action entry))])
                   ranked))
   :candidates candidates
   :policies (mapv (fn [c]
                     {:id (:id c)
                      :beta-declared beta
                      :habit (:habit c)
                      :habit-status (if (= :attached (:habit-status c))
                                      :computed (:habit-status c))
                      :f (:f c)
                      :f-status (if (= :attached (:f-status c))
                                  :computed (:f-status c))
                      :reason (:reason c)})
                   candidates)})

(defn select-action-cascades
  "Cascade-candidate selection at a DECLARED β (tick 1, R14 requirement).

   Unlike `select-action` — whose single-action behaviour is untouched — this
   path is for cascade candidates carrying G in :controller-score. It selects
   by the aligned model functions, not by re-derived maths:

   - futon2.aif.cascade-selection/selection-posterior at the caller-declared
     β (γ = 1/β): σ(ln E − F − G/β). There is NO default for β: it is not
     approved, so it must be passed by the caller; a missing or non-positive
     β propagates selection-posterior's typed refusal
     :invalid-temperature. The decision records it under
     :beta {:value β :status :declared}.
   - futon2.aif.cascade-selection/bayes-choice over each cascade's first
     acting pattern (the per-state projection of ActionMarginal), with that
     function's declared tie-break rule (:action-name-ascending).

   The learned joint-menu E is read and attached here on every invocation.
   Missing stable identities consume neutral E with the reason recorded.
   Entries may carry :f; missing, null and false F consume the neutral 0.
   Computed non-finite F remains in the run record, with consumed F = 0.
   :selection-certificate carries per-policy Lean fields and the input records.

   Returns a decision in the historical flat selector's shape (action,
   rank, controller-score, authorization envelope), plus:
     :beta                 {:value β :status :declared}
     :selection-law        {:requested :cascade-selection-posterior
                            :applied :cascade-selection-posterior
                            :beta β :beta-status :declared
                            :posterior {cascade-action-map → p}
                            :softmax-weights {first-acting-action → p}
                            :tie-break-rule <rule>
                            :tie-broken? bool}
     :softmax-weights      {first-acting-action → probability}

   `controller-authority/authorize` accepts the result on the admissible set
   (finite :controller-score, admissible action, :selection-law with :applied)."
  [ranked-actions {:keys [beta cascade-habit-path]}]
  ;; Runtime resolution breaks the existing prior -> policy shadow dependency.
  ;; This is the mandatory live seam, not an optional caller-side attachment.
  (let [attach (requiring-resolve 'futon2.aif.cascade-habit-store/attach-habits)
        path (or cascade-habit-path
                 @(requiring-resolve 'futon2.aif.cascade-habit-store/default-path))
        ranked-actions (attach path ranked-actions)
        candidates (mapv selection-candidate ranked-actions)
        posterior (cascade-selection/selection-posterior
                   {:beta beta :candidates candidates})
        ;; bayes-choice takes action-of as a MAP (it does (get action-of id)),
        ;; so build the per-candidate first-acting-action map, not a function.
        action-of (zipmap (map :action ranked-actions)
                          (map (comp cascade-first-action :action) ranked-actions))
        choice (cascade-selection/bayes-choice
                posterior action-of)
        chosen-entry (some (fn [e]
                             (when (= (cascade-first-action (:action e))
                                      (:action choice))
                               e))
                           ranked-actions)
        weights (into {}
                      (map (fn [[a p]] [(cascade-first-action a) p]))
                      posterior)]
    {:action (:action chosen-entry)
     :rank (or (:rank chosen-entry) 1)
     :controller-score (:controller-score chosen-entry)
     :selection-boundary :cascade-selection-posterior
     :recommendation-authority :live
     :requires-operator-override? false
     :actuation-status :pending-downstream-gates
     :actuation-authorized? false
     :beta {:value beta :status :declared}
     :selection-certificate (selection-certificate beta candidates ranked-actions)
     :selection-law
     {:requested :cascade-selection-posterior
      :applied :cascade-selection-posterior
      :beta beta
      :beta-status :declared
      :posterior posterior
      :softmax-weights weights
      :tie-break-rule (:tie-break-rule choice)
      :tie-broken?
      (boolean (some (fn [[a p]]
                       (and (not= a (:action choice))
                            (= p (:mass choice))))
                     weights))}
     :softmax-weights weights
     :chosen-action (:action choice)
     :chosen-action-mass (:mass choice)}))
