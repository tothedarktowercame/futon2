(ns futon2.aif.preferences
  "Preferences (C) and avoided states for the War Machine's AIF inference.

   Sourced from `war-machine-terminal-vocabulary.edn` :C/preferred,
   :C/avoided, :C/mode-prior, :G/pragmatic-fn. The data here is the
   substrate the free-energy computation reads against.")

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
   :active-repo-ratio  +1}) ; high = entities non-dormant (active)

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
  "Contract pt 2: documented default. Higher = softer tails; → 0 = hard."
  0.1)

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

(defn c-distribution
  "Build a normalised preference density for one channel.

   `spec`:
   - `[lo hi]`        → `{:kind :range}` — density 1 on [lo,hi], exponential
                        tails exp(-gap/T) outside, normalised over [0,1].
   - `{:becomes b}`   → `{:kind :bernoulli}` — target outcome b ∈ {0,1} (or
                        truthy/falsey); preference mass c* = 1/(1+e^(-1/T))
                        on the target (T→0 ⇒ c*→1, point-mass; T→∞ ⇒ 0.5).

   Opts: `:temperature` (default `default-c-temperature`)."
  [spec & {:keys [temperature] :or {temperature default-c-temperature}}]
  (let [t (double temperature)]
    (cond
      (and (map? spec) (contains? spec :becomes))
      (let [target (if (or (= 0 (:becomes spec)) (false? (:becomes spec))) 0 1)
            c* (/ 1.0 (+ 1.0 (Math/exp (- (/ 1.0 (max t 1e-9))))))]
        {:kind :bernoulli :target target :temperature t
         ;; mass assigned to outcome 1
         :p1 (if (= 1 target) c* (- 1.0 c*))})

      (sequential? spec)
      (let [[lo hi] spec
            lo (double lo) hi (double hi)
            t* (max t 1e-9)
            ;; ∫ exp(-gap/T) over [0,lo] + (hi-lo) + ∫ over [hi,1]
            z (+ (- hi lo)
                 (* t* (- 1.0 (Math/exp (- (/ lo t*)))))
                 (* t* (- 1.0 (Math/exp (- (/ (- 1.0 hi) t*))))))]
        {:kind :range :lo lo :hi hi :temperature t* :log-z (Math/log z)})

      :else (throw (ex-info "c-distribution: spec must be [lo hi] or {:becomes b}"
                            {:spec spec})))))

(defn log-preference
  "ln C(x) in nats. Range: -(gap/T) - ln Z. Bernoulli: ln of the mass on x."
  [{:keys [kind lo hi temperature log-z p1] :as _dist} x]
  (case kind
    :range (let [gap (max 0.0 (- lo (double x)) (- (double x) hi))]
             (- (- (/ gap temperature)) log-z))
    :bernoulli (Math/log (max 1e-12 (if (or (= 0 x) (false? x)) (- 1.0 p1) p1)))))

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
