(ns futon2.aif.policy-precision
  "Adaptive precision OVER POLICIES — Friston's γ (R14, E-precision-over-policies).

   The policy-scale sibling of R7's channel precision (`futon2.aif.precision`):
   where R7 learns per-channel Π from prediction-error history, this learns a
   single bounded scalar γ — the agent's confidence in its own DECISION-MAKING —
   from the realized-vs-expected outcomes of the policies it chose. γ ≈ 1/τ, the
   inverse selection temperature: high γ ⇒ sharper softmax ⇒ commit harder; low
   γ ⇒ flatter ⇒ hedge / explore. In the brain it is the same VTA/SN dopamine
   signal as R7's channel precision, one level up — over *policies*, not channels.

   Selection wiring (in `futon2.aif.policy`):
       τ_eff = adaptive-temperature(G-spread) / γ
   so γ = 1.0 reduces EXACTLY to today's spread-only temperature.

   THE LEARNING SIGNAL is the POLICY prediction-error: how well the chosen
   policy's *expected* free energy matched its *realized* outcome. v1 uses a
   SYMMETRIC RELATIVE error,
       ρ = |expected − realized| / (|expected| + |realized| + ε)  ∈ [0,1],
   chosen
     - over a raw error, so γ needs no G-unit-scale magic constant (ρ is
       scale-free — EFE G and free-energy F can live in different magnitude
       regimes and ρ is still a meaningful \"how surprised was I\"); and
     - over R7's literal 1/rolling-variance, because variance rewards a
       CONSISTENT BIAS (a policy always wrong by the same amount → low variance
       → spuriously high confidence). Relative error correctly reads a
       consistent miss as low confidence. (E-precision-over-policies §4 ratified
       this divergence from the charter's R7-mirror wording — same shape, sounder
       signal.)

   TRANSFER (after burn-in):
       γ = clamp( 2^(gain·(ρ* − ρ̄)), floor, cap )
   with neutral point ρ* (γ = 1 when the mean relative error equals ρ*), so
   ρ̄ = 0 ⇒ γ = cap (plans realize perfectly ⇒ commit harder) and ρ̄ = 1 ⇒
   γ = floor (total miss ⇒ hedge). Bounds are tight by default ([0.5, 2.0]): γ
   can at most halve or double the spread-derived τ — a bounded, reversible blast
   radius, appropriate while the feed is still the burn-in proxy (below).

   REDUCTION-SAFETY / BURN-IN (the R19-KL pattern): γ stays EXACTLY 1.0 until
   ≥ `min-history` realized samples accrue — so with no outcome history the WM's
   decisiveness is byte-identical to the current τ path, and only sharpens once
   evidence exists.

   STAGING (E-precision-over-policies §3.5): the *rich* realized signal arrives
   when R16 (close-the-loop) returns real post-act outcomes. Until then `judge`
   feeds a loose cross-tick PROXY (the previous tick's chosen-policy expected-G
   vs this tick's realized free-energy). This ns is SIGNAL-AGNOSTIC — it consumes
   a relative error (or an (expected, realized) pair via `observe-outcome`); so
   swapping the proxy for R16's realized ΔG is a one-line change at the feed in
   `judge`, never here.

   State shape (a single scalar, persisted in the trace as `:policy-precision`,
   the policy-scale sibling of R7's `:precision-state`):
     {:policy-precision  <current γ>
      :error-history     <vec of recent relative errors ρ_t, bounded>
      :mean-error        <last computed ρ̄, or nil pre-burn-in, for observability>
      :samples           <count of realized samples seen (monotone)>}

   Contract: R14 (precision over policies) per `M-aif-wiring`. Structural
   template: `futon2.aif.precision` (R7). Sibling outcome-learner: R12
   `futon2.aif.intrinsic-values` (Beta-credit, per-action-class follow-through —
   a DIFFERENT quantity than decision decisiveness).")

;; Window / burn-in.
(def default-window-size 20)        ; match R7's rolling window
(def default-min-history 5)         ; burn-in: γ ≡ 1.0 until this many samples

;; Transfer-function knobs (all dimensionless — ρ is scale-free).
(def default-neutral-error 0.5)     ; ρ* : γ = 1 when mean relative error = ρ*
(def default-gain 2.0)              ; steepness of the 2^(gain·…) map
(def default-gamma-floor 0.5)       ; γ can at most halve the spread-τ
(def default-gamma-cap 2.0)         ; γ can at most double the spread-τ
(def default-initial-gamma 1.0)     ; the prior: ≡ today's behaviour

(def ^:private rel-error-epsilon 1.0e-9)

(defn relative-error
  "Symmetric relative error between an expected and a realized scalar:

       ρ = |expected − realized| / (|expected| + |realized| + ε)  ∈ [0,1].

   Scale-free: 0.0 ⇒ perfect realization, → 1.0 ⇒ maximal surprise (e.g. opposite
   signs). When both are ~0 the denominator's ε guard yields ρ ≈ 0 (nothing was
   expected and nothing happened — no surprise). Pure."
  [expected realized]
  (let [e (double expected)
        r (double realized)
        num (Math/abs (- e r))
        den (+ (Math/abs e) (Math/abs r) rel-error-epsilon)]
    (/ num den)))

(defn initial-policy-precision-state
  "The prior γ-state: γ = 1.0 (≡ today's spread-only temperature), no realized
   history. Sibling of `precision/initial-precision-state`, but a single scalar
   rather than a per-channel map."
  []
  {:policy-precision default-initial-gamma
   :error-history []
   :mean-error nil
   :samples 0})

(defn- mean
  "Arithmetic mean of a non-empty sequence of numbers."
  [xs]
  (/ (reduce + 0.0 xs) (double (count xs))))

(defn- gamma-from-mean-error
  "The transfer: γ = clamp( 2^(gain·(ρ* − ρ̄)), floor, cap )."
  [mean-error {:keys [neutral gain floor cap]
               :or {neutral default-neutral-error
                    gain default-gain
                    floor default-gamma-floor
                    cap default-gamma-cap}}]
  (-> (Math/pow 2.0 (* (double gain) (- (double neutral) (double mean-error))))
      (max floor)
      (min cap)))

(defn update-policy-precision
  "Fold one realized POLICY prediction-error (a relative error ρ ∈ [0,1], the
   canonical signal) into the γ-state.

   - Appends ρ to the bounded rolling history (oldest dropped past window-size).
   - BURN-IN: while fewer than `min-history` samples have accrued, γ stays
     EXACTLY at the prior 1.0 (`:mean-error` nil) — reduction-safe.
   - After burn-in: γ = `gamma-from-mean-error` over the window's mean ρ̄,
     clamped to [floor, cap].

   `:samples` counts realized observations monotonically (independent of the
   bounded window) so the burn-in gate is honest across a long run.

   Opts (all optional): :window-size :min-history :neutral :gain :floor :cap.
   Pure — same inputs give the same output."
  ([prev-state rel-error] (update-policy-precision prev-state rel-error {}))
  ([prev-state rel-error
    {:keys [window-size min-history] :as opts
     :or {window-size default-window-size
          min-history default-min-history}}]
   (let [prev-history (:error-history prev-state [])
         appended (conj prev-history (double rel-error))
         bounded (if (> (count appended) window-size)
                   (vec (subvec appended (- (count appended) window-size)))
                   (vec appended))
         samples (inc (long (:samples prev-state 0)))]
     (if (< samples (long min-history))
       ;; Burn-in: keep the prior, but accrue history + sample count.
       {:policy-precision default-initial-gamma
        :error-history bounded
        :mean-error nil
        :samples samples}
       (let [m (mean bounded)]
         {:policy-precision (gamma-from-mean-error m opts)
          :error-history bounded
          :mean-error m
          :samples samples})))))

(defn observe-outcome
  "Convenience over `update-policy-precision`: fold a realized outcome given as
   an (expected, realized) scalar pair, computing the symmetric `relative-error`
   for you. This is what `judge` calls with the chosen policy's expected free
   energy and the realized free energy. Opts are passed through."
  ([prev-state expected realized]
   (observe-outcome prev-state expected realized {}))
  ([prev-state expected realized opts]
   (update-policy-precision prev-state (relative-error expected realized) opts)))

(defn fold-realized-outcome
  "Fold an R16 `:realized-outcome` trace record into a γ-state, with dedup.

   `realized-outcome` is the committed close-the-loop contract (interface paired
   with claude-10, E-close-the-loop):

     {:policy <id> :expected-G <g> :realized-G <g'> :tick <enactment tick>}

   written by the enactor AT enactment and READ back here next tick (async-clean
   — never a synchronous cross-subsystem call). Both G legs are the SAME EFE
   quantity — the fold's coverage→rollout ΔG, evaluated over the PREDICTED wiring
   (`:expected-G`) vs the actually-ENACTED wiring (`:realized-G`) — so the
   relative error is apples-to-apples, not a ΔG-vs-ΔF mismatch.

   Returns an UPDATED γ-state when the outcome is well-formed AND new (its
   `:tick` differs from the state's `:last-outcome-tick`); otherwise returns the
   state UNCHANGED. The tick-dedup matters because the WM ticks far faster than
   enactment — the same realized outcome must be folded at most once, else it
   would bias γ. Absent / malformed / already-seen ⇒ γ holds at the prior — the
   reduction-safe default that stands until enactment is live-wired (the field is
   ABSENT today, sim-only)."
  ([gamma-state realized-outcome]
   (fold-realized-outcome gamma-state realized-outcome {}))
  ([gamma-state realized-outcome opts]
   (if (and (map? realized-outcome)
            (number? (:expected-G realized-outcome))
            (number? (:realized-G realized-outcome))
            (not= (:tick realized-outcome) (:last-outcome-tick gamma-state)))
     (-> (observe-outcome gamma-state
                          (:expected-G realized-outcome)
                          (:realized-G realized-outcome)
                          opts)
         (assoc :last-outcome-tick (:tick realized-outcome)))
     gamma-state)))

(defn gamma-for
  "Read the current γ from a γ-state, defaulting to the prior 1.0 when absent
   (nil state, or a state with no `:policy-precision`). Mirrors
   `precision/precision-for`. This is the value selection divides τ by."
  [state]
  (double (get state :policy-precision default-initial-gamma)))
