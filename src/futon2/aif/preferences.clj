(ns futon2.aif.preferences
  "Preferences (C) and avoided states for the War Machine's AIF inference.

   Sourced from `war-machine-terminal-vocabulary.edn` :C/preferred,
   :C/avoided, :C/mode-prior, :G/pragmatic-fn. The data here is the
   substrate the free-energy computation reads against."
  (:require [futon2.aif.intrinsic-values :as iv]))

(def preferences
  "Expected observation ranges from war-machine-terminal-vocabulary.edn :C/preferred.
   Each channel maps to [lo hi] — the range where things are healthy."
  {:loop-health        [0.8 1.0]
   :support-coverage   [0.8 1.0]
   :attack-coverage    [0.8 1.0]
   :mission-health     [0.5 1.0]
   :stack-pct          [0.15 0.25]
   :consulting-pct     [0.20 0.35]
   :portfolio-pct      [0.20 0.35]
   :mathematics-pct    [0.15 0.25]
   :active-repo-ratio  [0.5 1.0]
   :sorry-count-norm   [0.0 0.3]
   :coupling-density   [0.1 0.3]
   :ticks-firing-ratio [0.0 0.0]
   :annotation-health  [0.7 1.0]})

(def avoided-states
  "States the system should not be in. From :C/avoided."
  {:strategic-mode     :hermit
   :stack-pct          [0.7 1.0]
   :consulting-pct     [0.0 0.0]
   :ticks-firing-ratio [0.5 1.0]
   :sorry-count-norm   [0.8 1.0]
   :active-repo-ratio  [0.0 0.2]})

(def mode-prior
  "Prior probability over strategic modes. From :C/mode-prior.
   :stop-the-line is an override-mode (see :μ/override-modes in
   war-machine-strategic-vocabulary.edn) — included here with prior 0.0
   so it never appears as an equilibrium-classification choice; the
   override-check in war-machine/judge sets it directly when the
   metabolic-balance tripwire fires."
  {:multiplied       0.35
   :depositing       0.25
   :foraging-trapped 0.15
   :hermit           0.10
   :stagnant         0.10
   :dark             0.05
   :stop-the-line    0.0})

(def pragmatic-weights
  "Per-channel weights for pragmatic free energy.
   From :G/pragmatic-fn in terminal vocabulary."
  {:stack-pct          0.25
   :consulting-pct     0.25
   :portfolio-pct      0.15
   :mission-health     0.15
   :ticks-firing-ratio 0.10
   :sorry-count-norm   0.10})

(defn current-C
  "The CURRENT channel-preference component of C — the per-channel `[lo hi]`
   ranges EFE's risk measures predicted outcomes against. Indirection seam for
   E-C-vector-live (§4.5): today it returns the static `preferences` floor, so
   it is behaviour-identical to reading `preferences` directly (regression-safe,
   the augment-don't-rip-out floor). The LIVE goal-OUTCOME half of C is
   delivered separately by `futon2.aif.c-vector` (derived, freshness-guarded,
   atom-backed); a future channel-liveness can override here without touching
   the consumers."
  []
  preferences)

(def channel-health-signs
  "v0.16: per-channel sign convention for R3d multi-channel aggregation.
   `+1` = higher observed value is HEALTHIER (positive prediction-error
        indicates the graph is healthier than belief expected → push
        entity beliefs toward :strengthened).
   `-1` = higher observed value is UNHEALTHIER (positive prediction-error
        indicates the graph has MORE undesirable signal than belief
        expected → push toward :foreclosed).

   Used by `futon2.report.war-machine/judge`'s R3 inner-loop to combine
   per-channel weighted-errors into a single signed scalar that drives
   the synthetic global belief-update event.

   Channels not in this map contribute 0 to the aggregate (no
   directional information available for them yet)."
  {:annotation-health  +1   ; high = more in-range entities, fewer anomalies
   :sorry-count-norm   -1   ; high = many open sorrys (unhealthier)
   :mission-health     +1   ; high = mission triage healthy
   :active-repo-ratio  +1   ; high = entities non-dormant (active)
   :support-coverage   +1   ; high = more evidence coverage
   :attack-coverage    +1   ; high = more evidence coverage
   :coupling-density   +1   ; high = more interconnected
   :ticks-firing-ratio +1}) ; high = more ticks passing

;; ---------------------------------------------------------------------------
;; c-distribution — preferences as NORMALISED DENSITIES (M-evaluate-policies
;; D5a; interface contract ratified with W1/claude-4, E-C-vector-live.md:230).
;;
;; UNITS, STATED ONCE (contract pt 2): all log-preferences and KLs below are in
;; NATS; continuous densities are normalised over [0,1] (the channel scale);
;; Bernoulli forms are normalised over {0,1}. Temperature: HIGHER = SOFTER;
;; temperature → 0 recovers the hard hinge (range) / point-mass (binary).
;; Default `default-c-temperature` = 0.1 (on [0,1]-scaled channels: an
;; out-of-range excursion of 0.1 costs ~1 nat of log-preference).
;;
;; Owned by M-evaluate-policies D5a; W1 (E-C-vector-live §11) is a consumer —
;; neither mission builds a private C. Degrade-safe (contract pt 3): nothing in
;; this section is consulted unless a caller opts in (:risk-mode :kl in
;; compute-efe, or W1's own call sites). Pure Clojure — bb-loadable
;; (contract pt 4).
;;
;; HONESTY (badge discipline, dcbe021 layer): `kl` for a Gaussian Q against a
;; [0,1]-supported density is now a TRUE KL — Q is truncated+renormalised to
;; [0,1] (item 1, E-KL-refinements, landed), so both densities share the support
;; and KL ≥ 0 (the untruncated form was a divergence score that could dip < 0).
;; RE-AUDITED 2026-07-03 (claude-5, dcbe021 adversarial rules): this function
;; badges :derived-from-FEP UNDER THE DECLARED GAUSSIAN CHANNEL MODEL (Q
;; truncated to the channel support; erf noise ~1.5e-7 clamped, numerical only).
;; SCOPE CAVEAT — the badge is the function's, not the consumer's: efe.clj's
;; Σ w_ch·KL_ch equals the joint KL (channel independence) only at UNIFORM
;; weights 1.0; any non-uniform weighting (incl. :pragmatic-parity, which
;; exists for hinge-vs-kl comparability) is a weighted aggregate —
;; :principled-approximation at best. The production :G-risk badge tracks the
;; LIVE lane (hinge) in data/r18-badges.edn.
;; ---------------------------------------------------------------------------

(def default-c-temperature
  "Contract pt 2: documented default. Higher = softer tails; → 0 = hard.
   Deliberately UNFITTED/dark (the E6 evidence is anchored to 0.1). Calibration
   apparatus: `scripts/wm_t_calibration.clj`; fitted T* recorded in
   `holes/labs/M-evaluate-policies/t-calibration.edn` (E-KL-refinements item 3).
   The flip to a fitted T is the operator's decision, not this default."
  0.1)

(def c-zone-load-key
  "Named preference channel for capability-zone evidence."
  :c-zone-load)

(defn capability-zone-evidence
  "Return the native-currency inputs for a learn-action target class.

   Load is posterior evidence mass alpha+beta-2, log-normalised against the
   largest currently rehydrated class. The same posterior supplies the
   Bernoulli predictive mean and outcome variance. An absent class degrades to
   Beta(1,1); zero mass leaves the C channel inactive."
  [target-class]
  (let [posteriors (iv/current)
        posterior (get posteriors target-class (iv/fresh-entry))
        alpha (double (:alpha posterior))
        beta (double (:beta posterior))
        mass (max 0.0 (- (+ alpha beta) 2.0))
        log-mass (Math/log1p mass)
        max-log-mass (reduce max 0.0
                             (map (fn [{:keys [alpha beta]}]
                                    (Math/log1p
                                     (max 0.0 (- (+ (double alpha)
                                                    (double beta))
                                                 2.0))))
                                  (vals posteriors)))
        load-weight (if (pos? max-log-mass)
                      (/ log-mass max-log-mass)
                      0.0)
        p (/ alpha (+ alpha beta))]
    {:channel c-zone-load-key
     :class target-class
     :alpha alpha
     :beta beta
     :mass mass
     :load-weight load-weight
     :predictive-probability p
     :predictive-variance (* p (- 1.0 p))
     :active? (pos? mass)}))

(defn- sq [x] (* (double x) (double x)))

(defn- std-normal-cdf
  "Φ(z) via the Abramowitz–Stegun 7.1.26 erf approximation (|err| < 1.5e-7).
   Pure; bb-safe."
  [z]
  (let [z (double z)
        t (/ 1.0 (+ 1.0 (* 0.2316419 (Math/abs z))))
        d (* 0.3989422804014327 (Math/exp (* -0.5 (sq z))))
        p (* d t (+ 0.319381530
                    (* t (+ -0.356563782
                            (* t (+ 1.781477937
                                    (* t (+ -1.821255978
                                            (* t 1.330274429)))))))))]
    (if (pos? z) (- 1.0 p) p)))

(defn- std-normal-pdf [z] (* 0.3989422804014327 (Math/exp (* -0.5 (sq z)))))

(def observable-kinds
  "The kinds a criterion may declare its observable to be. Closed, because an
   undeclared kind and a misspelt one want different answers: the first is a
   typed absence the arms already have a reason for, the second is a caller
   error and throws."
  #{:binary :continuous})

(def bernoulli-declaration-fields
  "The DECLARATION a Bernoulli criterion may carry on its own `spec`, and the
   home J6's ruling gives it (Joe, 2026-09-02, arm 2 `:declared-binarization`):
   `:observable-kind` says what the observable IS, `:threshold` says where a
   continuous one is cut. U16 put both on the per-call selector, which was the
   right shape for COMPARING unshipped arms and is the wrong one for shipping —
   a mission's declaration belongs to the mission's criterion, not to whoever
   calls the scorer. The selector keeps them so the other arms stay runnable;
   where both speak, the spec wins (`log-preference-under`)."
  [:observable-kind :threshold])

(defn- bernoulli-declaration
  "The declaration keys `spec` carries, validated, present-only — so a spec that
   declares nothing builds the same map it built before J6 and
   `mission_c_test/c-distribution-is-the-pinned-constructor` still holds."
  [spec]
  (let [d (select-keys spec bernoulli-declaration-fields)]
    (when-let [k (:observable-kind d)]
      (when-not (contains? observable-kinds k)
        (throw (ex-info "c-distribution: unknown :observable-kind"
                        {:observable-kind k :known observable-kinds}))))
    (when (contains? d :threshold)
      (let [t (:threshold d)]
        (when-not (and (number? t) (Double/isFinite (double t)))
          (throw (ex-info "c-distribution: :threshold must be a finite number"
                          {:threshold t})))))
    d))

(defn declaration-of
  "The declaration a built distribution carries because its SPEC declared it.
   Present-only, so `(merge selector (declaration-of dist))` leaves a selector
   untouched where nothing was declared."
  [dist]
  (select-keys dist bernoulli-declaration-fields))

(defn c-distribution
  "Build a normalised preference density for one channel.

   `spec`:
   - `[lo hi]`        → `{:kind :range}` — density 1 on [lo,hi], exponential
                        tails exp(-gap/T) outside, normalised over [0,1].
   - `{:becomes b}`   → `{:kind :bernoulli}` — target outcome b ∈ {0,1} (or
                        truthy/falsey); preference mass c* = 1/(1+e^(-1/T))
                        on the target (T→0 ⇒ c*→1, point-mass; T→∞ ⇒ 0.5).
   - `{:p1 p}`        → `{:kind :bernoulli}` — explicit preference mass on
                        outcome 1. Used when C itself is empirically measured.

   A `:bernoulli` spec may also DECLARE `:observable-kind` and/or `:threshold`
   (`bernoulli-declaration-fields`); the built distribution carries them, so the
   criterion's own declaration reaches the scorer without a caller passing it.
   Present-only: a spec that declares neither builds exactly the map it built
   before J6.

   Opts: `:temperature` (default `default-c-temperature`)."
  [spec & {:keys [temperature] :or {temperature default-c-temperature}}]
  (let [t (double temperature)]
    (cond
      (and (map? spec) (contains? spec :p1))
      (let [p1 (double (:p1 spec))]
        (when-not (<= 0.0 p1 1.0)
          (throw (ex-info "c-distribution: :p1 must be in [0,1]"
                          {:spec spec})))
        (merge {:kind :bernoulli :temperature t :p1 p1}
               (bernoulli-declaration spec)))

      (and (map? spec) (contains? spec :becomes))
      (let [target (if (or (= 0 (:becomes spec)) (false? (:becomes spec))) 0 1)
            c* (/ 1.0 (+ 1.0 (Math/exp (- (/ 1.0 (max t 1e-9))))))]
        (merge {:kind :bernoulli :target target :temperature t
                ;; mass assigned to outcome 1
                :p1 (if (= 1 target) c* (- 1.0 c*))}
               (bernoulli-declaration spec)))

      (sequential? spec)
      (let [[lo hi] spec
            lo (double lo) hi (double hi)
            t* (max t 1e-9)
            ;; ∫ exp(-gap/T) over [0,lo] + (hi-lo) + ∫ over [hi,1]
            z (+ (- hi lo)
                 (* t* (- 1.0 (Math/exp (- (/ lo t*)))))
                 (* t* (- 1.0 (Math/exp (- (/ (- 1.0 hi) t*))))))]
        {:kind :range :lo lo :hi hi :temperature t* :log-z (Math/log z)})

      :else (throw (ex-info "c-distribution: unsupported preference specification"
                            {:spec spec})))))

(defn log-preference
  "ln C(x) in nats. Range: -(gap/T) - ln Z. Bernoulli: ln of the mass on x."
  [{:keys [kind lo hi temperature log-z p1] :as _dist} x]
  (case kind
    :range (let [gap (max 0.0 (- lo (double x)) (- (double x) hi))]
             (- (- (/ gap temperature)) log-z))
    :bernoulli (Math/log (max 1e-12 (if (or (= 0 x) (false? x)) (- 1.0 p1) p1)))))

;; ---------------------------------------------------------------------------
;; U16 — the three candidate readings of a Bernoulli OUTCOME, built and run
;; behind a declared per-call selector (Joe, 2026-09-01: a choice the theory
;; does not settle gets its branches built and run, not an advance ruling).
;;
;; WHAT IS UNDER TEST. `log-preference`'s Bernoulli branch selects the
;; non-target pole with `(= 0 x)`, Clojure value equality, which is FALSE
;; across number classes: it holds for the long 0 and not for the double 0.0.
;; R2's observation vector is doubles throughout, so on the live path the
;; unsatisfied pole is unreachable and `nil` — an unread channel — also reads
;; as the target. Measured in U12
;; (holes/labs/wm-contract/runs/U12-c-mis-falsifier/measurements.edn
;; :clause-c, :defect); the only production caller of that branch is U11's
;; mission_c (mission_c.clj:333, :370). C_int and the c-vector lane go through
;; `kl` with an explicit `{:kind :bernoulli :p q}` and are not exposed.
;;
;; `log-preference` ITSELF IS UNTOUCHED. The arms live in new functions that a
;; caller must name; nothing acquires an arm by default, so the shipped numbers
;; do not move. THE DECLARATION RIDES ON THE SELECTOR, NOT ON THE DIST: a
;; selector may be a bare arm keyword or a map carrying `:threshold` /
;; `:observable-kind`. That keeps `c-distribution` — shared with C_int and
;; pinned by `mission_c_test/c-distribution-is-the-pinned-constructor` —
;; byte-identical while the arms are being compared. If an arm that needs a
;; declaration wins, the declaration's shipping home is the criterion's
;; `:spec`, and moving it there is a separate change.
;;
;; NIL IS A TYPED ABSENCE UNDER EVERY ARM, and that is not one of the branches:
;; it is C130 discipline (an unread observable is absent, never satisfied)
;; applied here, so no arm can be chosen that keeps today's reading of nil.

(def bernoulli-outcome-arms
  "The three candidate semantics U16 compares. Not a default: every call names
   one.

   `:numeric-equality`      — `==` rather than `=`, so the double 0.0 and the
                              long 0 are the same outcome. Needs no
                              declaration; every other value reads as the
                              target.
   `:declared-binarization` — a Bernoulli spec on a continuous observable must
                              carry a `:threshold`; with one, x < threshold is
                              outcome 0 and x >= threshold is outcome 1.
                              Without one, only an exactly-binary value is
                              read; anything else refuses.
   `:typed-binary-only`     — the outcome is read only where the observable is
                              DECLARED `:binary`, by `==`; a `:continuous`
                              observable under a Bernoulli spec is a typed
                              spec/observable mismatch, and an undeclared kind
                              is its own refusal."
  #{:numeric-equality :declared-binarization :typed-binary-only})

(defn- selector-map [selector]
  (cond (keyword? selector) {:arm selector}
        (map? selector) selector
        :else (throw (ex-info "bernoulli-outcome: selector must be an arm keyword or a map"
                              {:selector selector}))))

(defn- exactly-binary?
  "True for a number that IS one of the two outcomes, under `==` so the class
   does not decide it. This is the value-level test; it says nothing about
   whether the observable it came from is binary."
  [x]
  (and (number? x) (or (== 0 x) (== 1 x))))

(defn bernoulli-outcome
  "Which pole of a `:bernoulli` preference the observed value `x` selects, under
   the declared arm. Returns `{:status :present :outcome 0|1}` or
   `{:status :absent :reason <keyword>}` — never a bare number, because two of
   the three arms refuse on values the third scores.

   `selector`: an arm keyword from `bernoulli-outcome-arms`, or a map
   `{:arm <kw> :threshold <number> :observable-kind :binary|:continuous}`."
  [selector x]
  (let [{:keys [arm threshold observable-kind]} (selector-map selector)]
    (when-not (contains? bernoulli-outcome-arms arm)
      (throw (ex-info "bernoulli-outcome: unknown arm"
                      {:arm arm :known bernoulli-outcome-arms})))
    (cond
      ;; Before any arm: an unread channel is absent, not satisfied (C130).
      (nil? x) {:status :absent :reason :no-outcome-observed :arm arm}

      ;; A boolean already IS an outcome; no arm reads it differently.
      (boolean? x) {:status :present :outcome (if x 1 0) :arm arm}

      (not (number? x)) {:status :absent :reason :uninterpretable-outcome
                         :arm arm :class (.getName (class x))}

      (= :numeric-equality arm)
      {:status :present :outcome (if (== 0 x) 0 1) :arm arm}

      (= :declared-binarization arm)
      (cond
        (number? threshold)
        {:status :present :outcome (if (< (double x) (double threshold)) 0 1)
         :arm arm :threshold (double threshold)}

        (exactly-binary? x)
        {:status :present :outcome (if (== 0 x) 0 1) :arm arm}

        ;; J6: an observable DECLARED binary needs no threshold, so a value that
        ;; is not one of the two outcomes is not a missing declaration — the
        ;; declaration is there and the value contradicts it. Same reason
        ;; `:typed-binary-only` gives, because it is the same failure.
        (= :binary observable-kind)
        {:status :absent :reason :non-binary-value-on-binary-observable :arm arm}

        :else {:status :absent :reason :no-declared-threshold :arm arm})

      (= :typed-binary-only arm)
      (case observable-kind
        :binary (if (exactly-binary? x)
                  {:status :present :outcome (if (== 0 x) 0 1) :arm arm}
                  {:status :absent :reason :non-binary-value-on-binary-observable
                   :arm arm})
        :continuous {:status :absent :reason :spec-observable-mismatch :arm arm}
        {:status :absent :reason :undeclared-observable-kind :arm arm}))))

(defn log-preference-under
  "`log-preference` with the Bernoulli outcome read under a declared arm, and
   with a typed return so a refusal is not a number.

   THE DECLARATION COMES OFF THE DISTRIBUTION FIRST (J6). Whatever
   `:observable-kind` / `:threshold` the criterion's own spec declared is merged
   OVER the selector's, so a per-call selector can fill a declaration the spec
   omits but cannot silently overrule one the mission wrote down. That is the
   whole point of moving the declaration's home into the spec, and it is why
   U16's comparison columns still run: nothing in that corpus declares a spec.

   Returns `{:status :present :log-c <nats>}` or
   `{:status :absent :reason <keyword>}`. The `:range` branch is arithmetically
   unchanged — it already has a gradient and U12's finding does not touch it —
   but it too refuses on `nil` rather than throwing on `(double nil)`."
  [{:keys [kind lo hi temperature log-z p1] :as dist} x selector]
  (case kind
    :range (if (nil? x)
             {:status :absent :reason :no-outcome-observed}
             (let [gap (max 0.0 (- lo (double x)) (- (double x) hi))]
               {:status :present :log-c (- (- (/ gap temperature)) log-z)}))
    :bernoulli (let [o (bernoulli-outcome
                        (merge (selector-map selector) (declaration-of dist)) x)]
                 (if (= :present (:status o))
                   {:status :present
                    :log-c (Math/log (max 1e-12 (if (= 0 (:outcome o)) (- 1.0 p1) p1)))
                    :outcome (:outcome o)}
                   o))))

;; ---------------------------------------------------------------------------
;; U17 — the point-mass term as a DIVERGENCE, so it cannot go below zero.
;;
;; THE HAZARD. Under the v0 status-quo forward model (mission_c §3) Q(o|π) is a
;; point mass at the current reading, so the per-criterion term is −ln C(x).
;; For a `:bernoulli` C that IS the KL exactly — KL(δ_b ‖ C) = −ln C(b) ≥ 0
;; because C(b) ≤ 1, pinned numerically against `kl` by
;; `mission_c_test/surprisal-is-the-point-mass-kl`. For a `:range` C it is NOT:
;; a point mass has no density on a continuous support, KL(δ_x ‖ C) is +∞ at
;; every x, and what the code carries is the CROSS-ENTROPY gap/T + ln Z, fixed
;; only up to an additive constant. When the declared band is narrower than one
;; unit, Z < 1 and ln Z < 0, so that term is NEGATIVE everywhere inside the band
;; (−0.5119492459595545 for [0.5 1.0] at T = 0.1) and a satisfied criterion
;; summed into one G beside C_int's KL ≥ 0 would be paid a BONUS.
;;
;; THE FORM, and it is a choice between two that both clear zero. Fix the
;; constant at the best attainable value — score the EXCESS
;;     KL(δ_x ‖ C) − inf_y KL(δ_y ‖ C)  =  gap/T,
;; the divergent point-mass entropy cancelling between the two terms and ln Z
;; with it. That is exactly 0 on the declared band and grows linearly in
;; temperature units outside it. The alternative U17 names, a clamp
;; `(max 0.0 (+ (/ gap t) log-z))`, also clears zero but flattens the gradient
;; for a further −T·ln Z outside the band (0.0512 for [0.5 1.0] at T = 0.1),
;; reading a value that misses the band by 0.05 as fully satisfied; the excess
;; form keeps that gradient. Same repair class as `kl-gaussian-range` below,
;; which exists because "the untruncated form was a divergence score that could
;; dip below 0".
;;
;; THE BERNOULLI BRANCH IS NOT SHIFTED. Its term is an exact KL and already
;; ≥ 0, so there is no missing constant to fix. Subtracting ITS minimum
;; −ln max(p1, 1−p1) — the 4.539889921682063E-5 that U12 and U16 measured as
;; the T = 0.1 satisfied floor — would be a different change: it would move
;; numbers two committed replay artifacts pin, and the floor it removes is a
;; real property of a soft target, not the missing-constant artefact the range
;; branch has. U12 clause (c)'s "satisfied ⇒ zero" therefore stays unreached for
;; Bernoulli criteria, exactly as U16 recorded it.
;;
;; ORTHOGONAL TO J6. The shift is a function of the SPEC KIND alone and never of
;; which outcome an arm reads, so every arm in `bernoulli-outcome-arms` and the
;; shipped no-arm path take the same constant.

(defn point-mass-divergence-shift
  "The constant subtracted from the point-mass cross-entropy −ln C(x) to make it
   a divergence ≥ 0: `log-z` for `:range`, so the term becomes gap/T; 0.0 for
   `:bernoulli`, whose term is already an exact KL. See the U17 block above for
   why the two branches differ. Throws on an unknown kind rather than defaulting
   to 0.0, because a new spec shape silently taking no shift is the defect this
   exists to remove."
  [{:keys [kind log-z] :as dist}]
  (case kind
    :bernoulli 0.0
    :range (double log-z)
    (throw (ex-info "point-mass-divergence-shift: unsupported preference kind"
                    {:kind kind :dist dist}))))

(defn point-mass-divergence
  "KL(δ_x ‖ C) in nats under the point-mass forward model — exact for
   `:bernoulli`, and for `:range` the excess over the best attainable value,
   gap/T. ≥ 0 on every spec shape, and 0 exactly where x is the target outcome
   (Bernoulli, up to the temperature floor) or inside the declared band (range).

   `x` is the outcome as already read; reading a raw observation under a
   declared arm is `bernoulli-outcome`'s job, and the shift does not depend on
   which arm did it."
  [dist x]
  (- (- (log-preference dist x)) (point-mass-divergence-shift dist)))

(defn- kl-gaussian-range
  "Item 1 (E-KL-refinements): KL(Q~ ‖ C) for Q~ = N(mu,sigma2) TRUNCATED and
   renormalised to [0,1], against a `:range` preference density C on [0,1]. A true
   KL between densities on the SAME support ⇒ ≥ 0 (the untruncated form was a
   divergence score that could dip below 0). All closed-form in Φ/φ.

   With sigma=√s2, alpha=(0-mu)/sigma, beta=(1-mu)/sigma, M=Φ(beta)-Φ(alpha):
     -H[Q~] = -½ln(2πe s2) - ln M - (alpha·φ(alpha) - beta·φ(beta))/(2M)
     E_Q~[gap] = (E_below + E_above)/M   (gap = max(0, lo-x, x-hi))
   Degenerate regime: mu far outside [0,1] with tiny sigma ⇒ M→0; clamped at
   1e-12 (Q~ is then ~a point mass just outside the support; the score stays
   finite and large). Final result clamped `(max 0.0 …)` — a NUMERICAL clamp only,
   guarding ~1e-7 erf-approximation noise, since KL ≥ 0 exactly."
  [mu sigma2 lo hi temperature log-z]
  (let [mu    (double mu)
        s2    (max (double sigma2) 1e-9)
        sigma (Math/sqrt s2)
        lo    (double lo) hi (double hi)
        alpha (/ (- 0.0 mu) sigma)
        beta  (/ (- 1.0 mu) sigma)
        pa    (std-normal-pdf alpha) pb (std-normal-pdf beta)
        m     (max 1e-12 (- (std-normal-cdf beta) (std-normal-cdf alpha)))
        neg-h (- (+ (* 0.5 (Math/log (* 2.0 Math/PI Math/E s2)))
                    (Math/log m)
                    (/ (- (* alpha pa) (* beta pb)) (* 2.0 m))))
        zlo   (/ (- lo mu) sigma) zhi (/ (- hi mu) sigma)
        e-below (- (* (- lo mu) (- (std-normal-cdf zlo) (std-normal-cdf alpha)))
                   (* sigma (- pa (std-normal-pdf zlo))))
        e-above (+ (* (- mu hi) (- (std-normal-cdf beta) (std-normal-cdf zhi)))
                   (* sigma (- (std-normal-pdf zhi) pb)))
        e-gap   (/ (+ e-below e-above) m)]
    (max 0.0 (+ neg-h (/ e-gap (double temperature)) log-z))))

(defn kl
  "Divergence of predicted outcome Q from the preference density, in nats.

   Q forms (contract pt 1):
   - `{:kind :gaussian :mu m :sigma2 s2}` vs a `:range` dist:
       KL(Q~ ‖ C) = -H[Q~] + E_Q~[gap]/T + ln Z,  Q~ = N truncated+renormalised
     to [0,1] (item 1, E-KL-refinements) — a TRUE KL on the shared support ⇒ ≥ 0.
     Closed-form in Φ/φ; see `kl-gaussian-range`. Badge :derived-from-FEP under
     the declared Gaussian channel model (re-audited 2026-07-03; see the HONESTY
     block for the consumer-side weighted-sum caveat).
   - `{:kind :bernoulli :p q}` vs a `:bernoulli` dist: exact
       q·ln(q/c) + (1-q)·ln((1-q)/(1-c)), c = mass on outcome 1."
  [{qkind :kind :as q} {ckind :kind :as dist}]
  (cond
    (and (= qkind :gaussian) (= ckind :range))
    (let [{:keys [mu sigma2]} q
          {:keys [lo hi temperature log-z]} dist]
      (kl-gaussian-range mu sigma2 lo hi temperature log-z))

    (and (= qkind :bernoulli) (= ckind :bernoulli))
    (let [qq (min (max (double (:p q)) 1e-9) (- 1.0 1e-9))
          c (min (max (double (:p1 dist)) 1e-9) (- 1.0 1e-9))]
      (+ (* qq (Math/log (/ qq c)))
         (* (- 1.0 qq) (Math/log (/ (- 1.0 qq) (- 1.0 c))))))

    :else (throw (ex-info "kl: unsupported Q/dist pairing"
                          {:q qkind :dist ckind}))))
