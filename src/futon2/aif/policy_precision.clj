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

   THE LEARNING SIGNAL is the chosen policy's realized PERFORMANCE: how much its
   *realized* outcome beat (or missed) its *expected* outcome. Free energy
   convention — LOWER G is better — so a realized G below expected means the plan
   over-delivered. v1 uses a SIGNED, SCALE-FREE performance ratio:

       perf = (expected-G − realized-G) / (|expected-G| + |realized-G| + ε) ∈ [−1,1]

     perf > 0  ⇒ BEAT expectation (plan paid off better than predicted) ⇒ commit
     perf = 0  ⇒ realized exactly as predicted                          ⇒ neutral
     perf < 0  ⇒ MISSED / underperformed                               ⇒ hedge

   Chosen
     - SIGNED (directional), not a symmetric |error|: the excursion's goal (HEAD
       + Joe, 2026-06-27) is decisiveness EARNED BY GOOD OUTCOMES — 'commit harder
       after a run of plans that paid off, hedge after a run that didn't'. A
       symmetric error measures only CALIBRATION (did realized match expected?),
       so a large over-delivery — a big mismatch — would spuriously LOWER γ. The
       signed ratio is monotone in performance and reads a consistent
       under-delivery as a consistent hedge (correct on the bias axis too).
     - SCALE-FREE (ratio): no G-unit magic constant; EFE G and free-energy F can
       live in different magnitude regimes and perf is still meaningful.

   TRANSFER (after burn-in):
       γ = clamp( 2^(gain · perf̄), floor, cap )      ; gain 1.0, floor 0.5, cap 2.0
   so perf̄ = +1 ⇒ γ = cap (every plan over-delivered ⇒ commit), perf̄ = −1 ⇒
   γ = floor (every plan missed ⇒ hedge), perf̄ = 0 ⇒ γ = 1 (use the spread-τ
   as-is). Bounds are tight by default ([0.5, 2.0]): γ can at most halve or double
   the spread-derived τ — a bounded, reversible blast radius.

   REDUCTION-SAFETY / BURN-IN (the R19-KL pattern): γ stays EXACTLY 1.0 until
   ≥ `min-history` realized samples accrue — so with no outcome history the WM's
   decisiveness is byte-identical to the current τ path, and only sharpens once
   evidence exists.

   STAGING (E-precision-over-policies §3.5): the realized signal arrives when R16
   (close-the-loop) returns real post-act outcomes. Until then `judge` reads R16's
   `:realized-outcome` trace contract; absent today ⇒ no sample ⇒ γ holds at 1.0.
   This ns is SIGNAL-AGNOSTIC at its core — `update-policy-precision` consumes a
   performance value in [−1,1]; `observe-outcome` / `fold-realized-outcome` adapt
   the (expected, realized) pair to it. Swapping the realized term is a one-line
   change at the feed in `judge`, never here.

   State shape (a single scalar, persisted in the trace as `:policy-precision`,
   the policy-scale sibling of R7's `:precision-state`):
     {:policy-precision  <current γ>
      :perf-history      <vec of recent signed performances perf_t ∈ [−1,1], bounded>
      :mean-perf         <last computed perf̄, or nil pre-burn-in, for observability>
      :samples           <count of realized samples seen (monotone)>}

   Contract: R14 (precision over policies) per `M-aif-wiring`. Structural
   template: `futon2.aif.precision` (R7). Sibling outcome-learner: R12
   `futon2.aif.intrinsic-values` (Beta-credit, per-action-class follow-through —
   a DIFFERENT quantity than decision decisiveness).")

;; Window / burn-in.
(def default-window-size 20)        ; match R7's rolling window
(def default-min-history 5)         ; burn-in: γ ≡ 1.0 until this many samples

;; Transfer-function knobs (all dimensionless — perf is scale-free, in [−1,1]).
(def default-gain 1.0)              ; steepness of 2^(gain·perf̄); gain 1 maps ±1 → cap/floor
(def default-gamma-floor 0.5)       ; γ can at most halve the spread-τ
(def default-gamma-cap 2.0)         ; γ can at most double the spread-τ
(def default-initial-gamma 1.0)     ; the prior: ≡ today's behaviour

(def ^:private perf-epsilon 1.0e-9)

(defn policy-performance
  "Signed, scale-free performance of a chosen policy: how much its REALIZED
   outcome beat (+) or missed (−) its EXPECTED outcome. Free-energy convention —
   LOWER G is better — so realized BELOW expected is over-delivery:

       perf = (expected-G − realized-G) / (|expected-G| + |realized-G| + ε) ∈ [−1,1].

   perf > 0 ⇒ beat (commit), perf < 0 ⇒ miss (hedge), perf = 0 ⇒ exactly as
   predicted (neutral). When both are ~0 the ε guard yields perf ≈ 0. Pure."
  [expected realized]
  (let [e (double expected)
        r (double realized)
        num (- e r)
        den (+ (Math/abs e) (Math/abs r) perf-epsilon)]
    (/ num den)))

(defn initial-policy-precision-state
  "The prior γ-state: γ = 1.0 (≡ today's spread-only temperature), no realized
   history. Sibling of `precision/initial-precision-state`, but a single scalar
   rather than a per-channel map."
  []
  {:policy-precision default-initial-gamma
   :perf-history []
   :mean-perf nil
   :samples 0})

(defn- mean
  "Arithmetic mean of a non-empty sequence of numbers."
  [xs]
  (/ (reduce + 0.0 xs) (double (count xs))))

(defn- gamma-from-mean-perf
  "The transfer: γ = clamp( 2^(gain · perf̄), floor, cap )."
  [mean-perf {:keys [gain floor cap]
              :or {gain default-gain
                   floor default-gamma-floor
                   cap default-gamma-cap}}]
  (-> (Math/pow 2.0 (* (double gain) (double mean-perf)))
      (max floor)
      (min cap)))

(defn update-policy-precision
  "Fold one realized POLICY PERFORMANCE (a signed value in [−1,1], the canonical
   signal — see `policy-performance`) into the γ-state.

   - Appends perf to the bounded rolling history (oldest dropped past window-size).
   - BURN-IN: while fewer than `min-history` samples have accrued, γ stays
     EXACTLY at the prior 1.0 (`:mean-perf` nil) — reduction-safe.
   - After burn-in: γ = `gamma-from-mean-perf` over the window's mean perf̄,
     clamped to [floor, cap].

   `:samples` counts realized observations monotonically (independent of the
   bounded window) so the burn-in gate is honest across a long run.

   Opts (all optional): :window-size :min-history :gain :floor :cap.
   Pure — same inputs give the same output."
  ([prev-state perf] (update-policy-precision prev-state perf {}))
  ([prev-state perf
    {:keys [window-size min-history] :as opts
     :or {window-size default-window-size
          min-history default-min-history}}]
   (let [prev-history (:perf-history prev-state [])
         appended (conj prev-history (double perf))
         bounded (if (> (count appended) window-size)
                   (vec (subvec appended (- (count appended) window-size)))
                   (vec appended))
         samples (inc (long (:samples prev-state 0)))]
     (if (< samples (long min-history))
       ;; Burn-in: keep the prior, but accrue history + sample count.
       {:policy-precision default-initial-gamma
        :perf-history bounded
        :mean-perf nil
        :samples samples}
       (let [m (mean bounded)]
         {:policy-precision (gamma-from-mean-perf m opts)
          :perf-history bounded
          :mean-perf m
          :samples samples})))))

(defn observe-outcome
  "Convenience over `update-policy-precision`: fold a realized outcome given as
   an (expected, realized) scalar pair, computing the signed `policy-performance`
   for you. This is what `judge` (via `fold-realized-outcome`) calls with the
   chosen policy's expected free energy and the realized free energy. Opts are
   passed through."
  ([prev-state expected realized]
   (observe-outcome prev-state expected realized {}))
  ([prev-state expected realized opts]
   (update-policy-precision prev-state (policy-performance expected realized) opts)))

(defn fold-realized-outcome
  "Fold an R16 `:realized-outcome` trace record into a γ-state, with dedup.

   `realized-outcome` is the committed close-the-loop contract (interface paired
   with claude-10, E-close-the-loop):

     {:policy <id> :expected-G <g> :realized-G <g'> :tick <enactment tick>}

   written by the enactor AT enactment and READ back here next tick (async-clean
   — never a synchronous cross-subsystem call). Both G legs are the SAME EFE
   quantity — the fold's coverage→rollout ΔG, evaluated over the PREDICTED wiring
   (`:expected-G`) vs the actually-ENACTED wiring (`:realized-G`) — so the
   performance ratio is apples-to-apples, not a ΔG-vs-ΔF mismatch.

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

(defn coerce-state
  "Schema guard for a γ-state read back from a persisted trace record.

   The R14 v0 feed (live for one day, 2026-06-27, retired for the signed-perf
   redesign) persisted a symmetric-|error| schema (`:error-history`/`:mean-error`)
   whose 8 samples were DEGENERATE — a scale-mismatched expected-vs-realized
   comparison drove error ≈ 1.0 every time, pinning γ to the floor 0.5 — and the
   stale state then rode the trace forward verbatim for days (found 2026-07-02:
   the live WM hedging 2× on junk). A state without a `:perf-history` vector is
   that retired schema (or otherwise malformed): reconstruct the honest prior
   instead of inheriting it. Real samples re-accrue through burn-in from live
   R16 `:realized-outcome` records."
  [state]
  (if (and (map? state) (vector? (:perf-history state)))
    state
    (initial-policy-precision-state)))

(defn gamma-for
  "Read the current γ from a γ-state, defaulting to the prior 1.0 when absent
   (nil state, or a state with no `:policy-precision`). Mirrors
   `precision/precision-for`. This is the value selection divides τ by."
  [state]
  (double (get state :policy-precision default-initial-gamma)))
