(ns futon2.aif.likelihood-precision
  "R7: likelihood precision ζ as a Gibbs inverse temperature on the likelihood
   A, DECLARED as a named quantity — distinct from policy precision γ (R14).

  What exists on the live tick today (2026-09-18, after
  22ad77ea/021f129b/e26c31fb): efe/rank-cascade-actions no longer hardcodes
  zero rates — :adjudication-rates and :zeta on the scoring opts reach the
  factorized scorer, and absent both the call defaults EXPLICITLY to the
  identity kernel (zero rates: A is the identity kernel, Lean
  tokenLikelihood_checkable, so no likelihood matrix is evaluated, the
  ambiguity term is identically 0, and ζ multiplies nothing — declared
  FIXED-VACUOUS on that path; see `zeta-declaration` and its
  :certificate-statuses). No production caller SOURCES real rates from
  futon2.aif.observation-rates yet; that sourcing is the next WIRE slice.

  What this namespace does NOT do: choose a prior or update law for ζ. Parr
   2022 B.14–B.19 gamma-prior/fixed-point machinery is pinned in futon2 only
   for POLICY precision (futon2.aif.policy-precision, R14). A ζ-side prior and
   posterior update is a new numerical law; per the r7 cascade README ('No new
   precision law, floor or gain is chosen here') it is left as a named gap.

  Also distinct: belief.clj's κ(w) = log₂(1+w) is a legacy channel-weight
   exponent on A rows — retained there, not reinterpreted as ζ here.")

(def ^:private stochastic-tolerance 1e-9)

(defn refusal?
  [x]
  (and (map? x) (= :missing (:status x))))

(defn temper-row
  "Gibbs inverse-temperature tempering of ONE stochastic row of A: the row
   raised to the power ζ and renormalized, p ↦ p^ζ / Σ p^ζ. ζ = 1 is the
   identity; ζ > 1 sharpens toward the row max; 0 < ζ < 1 flattens.

   ζ = 0 is DECLARED uninformative, not an error: every strictly positive
   entry becomes the uniform mass over the positive support, and entries that
   were exactly 0 stay 0 (0^0 := 0 here — a zero-probability observation
   cannot be resurrected by removing precision; absence of data is not
   infinite confidence). ζ < 0 is the typed refusal {:status :missing
   :kind :negative-zeta}: precision is nonnegative, and this namespace does
   not silently strengthen that to positivity — ζ = 0 is admitted on purpose.

   A row whose entries are not nonnegative finite numbers summing to 1
   (within tolerance) refuses with :kind :row-not-stochastic. ROW is a map or
   a sequential of probabilities. Pure."
  [row zeta]
  (cond
    (not (number? zeta)) {:status :missing :kind :invalid-zeta :zeta zeta}
    (Double/isNaN (double zeta)) {:status :missing :kind :invalid-zeta :zeta zeta}
    (neg? (double zeta)) {:status :missing :kind :negative-zeta :zeta zeta}
    :else
    (let [entries (cond
                    (map? row) (seq row)
                    (sequential? row) (map vector (range) row)
                    :else nil)]
      (if (nil? entries)
        {:status :missing :kind :invalid-row :row row}
        (let [bad-entry (some (fn [[k v]]
                                (when-not (and (number? v)
                                               (Double/isFinite (double v))
                                               (not (neg? (double v))))
                                  [k v]))
                              entries)]
          (if bad-entry
            {:status :missing :kind :row-not-stochastic :entry (first bad-entry)
             :value (second bad-entry)}
            (let [total (reduce + (map second entries))]
              (if (> (Math/abs (- 1.0 (double total))) stochastic-tolerance)
                {:status :missing :kind :row-not-stochastic :row-sum total}
                (let [positive (filter (fn [[_ v]] (pos? (double v))) entries)]
                  (if (zero? zeta)
                    (if (map? row)
                      (into {} (map (fn [[k v]] [k (if (pos? (double v))
                                                     (/ 1.0 (count positive))
                                                     0.0)]))
                            entries)
                      (mapv (fn [[_ v]] (if (pos? (double v))
                                          (/ 1.0 (count positive))
                                          0.0))
                            entries))
                    (let [z (double zeta)
                          tempered (map (fn [[k v]] [k (Math/pow (double v) z)])
                                        positive)
                          z-total (reduce + (map second tempered))]
                      (if (not (pos? z-total))
                        {:status :missing :kind :tempered-row-underflow :zeta zeta}
                        (if (map? row)
                          (into {} (map (fn [[k v]]
                                          [k (if (pos? (double v))
                                               (/ (Math/pow (double v) z) z-total)
                                               0.0)]))
                                entries)
                          (mapv (fn [[_ v]] (if (pos? (double v))
                                              (/ (Math/pow (double v) z) z-total)
                                              0.0))
                                entries))))))))))))))

(defn temper-a
  "Apply `temper-row` at the same ζ to every row of a likelihood A, given as
   {observation {state p}} (or {observation [p…]}). Returns the tempered map
   with identical keys, or the first typed refusal. Pure; no wiring — no
   consumer of the tempered A exists on the live tick path yet (see
   `zeta-declaration`)."
  [a zeta]
  (let [result (reduce (fn [acc [obs row]]
                         (let [r (temper-row row zeta)]
                           (if (refusal? r)
                             (reduced (assoc r :observation obs))
                             (assoc acc obs r))))
                       {} a)]
    result))

(defn temper-bernoulli
  "Gibbs tempering at ζ of ONE Bernoulli parameter p (a two-outcome row of
   A): p ↦ p^ζ / (p^ζ + (1−p)^ζ), i.e. sigmoid(ζ·logit p). This is exactly
   `temper-row` applied to the row [p, 1−p] — the audited law, not a
   reimplementation — so its refusals and its ζ=0 declaration (p ∈ (0,1)
   becomes 1/2; p = 0 stays 0; p = 1 stays 1) are inherited verbatim.

   EXACTNESS: when p is rational and ζ is a nonnegative integer the result
   is the exact rational p^ζ/(p^ζ+(1−p)^ζ) (verified against temper-row's
   double path to 1e-12 in the tests) — the exact-rational consumers
   (token-likelihood, observation-distribution, predict-observations,
   horizon-g) accept tempered rates unchanged. Non-integer ζ falls back to
   temper-row's doubles. Returns the tempered p or the typed refusal."
  [p zeta]
  (let [r (temper-row [(double p) (- 1.0 (double p))] zeta)]
    (if (refusal? r)
      r
      (if (and (rational? p) (integer? zeta) (not (neg? zeta)))
        (if (zero? zeta)
          ;; ζ = 0: temper-row's declared 0^0 := 0 — 0 stays 0, 1 stays 1,
          ;; every interior p is the fair coin — never 0^0 = 1.
          (cond (zero? p) 0 (= 1 p) 1 :else 1/2)
          (let [e    (fn [x] (reduce * 1 (repeat zeta x)))
                a    (e p)
                b    (e (- 1 p))]
            (/ a (+ a b))))
        (first r)))))

(defn tempered-rates
  "R7 wiring: the EFFECTIVE adjudication rates whose token-likelihood IS the
   ζ-tempered likelihood. Feeding the returned map to
   `futon2.aif.cascade-model-manifest/token-likelihood` (and therefore to
   observation-distribution, predict-observations and horizon-g/horizon-g-sparse)
   computes exactly Lean `AIF.Terms.temperedLikelihood` M ζ s o = A(s,o)^ζ / Z(ζ)_s
   for the token observation model.

   Why the substitution is exact: token-likelihood factorizes over the token
   universe as A(s,o) = ∏_v Bern_v(p_v(s)) with p_v(s) = 1−falseNeg_v when
   v ∈ s, else falsePos_v (TokenObservation.tokenLikelihood). Raising the
   whole product to ζ and normalizing over observations o ∈ 2^U factorizes
   the normalizer too — Z(ζ)_s = ∑_o A(s,o)^ζ = ∏_v (p_v(s)^ζ + (1−p_v(s))^ζ)
   — so the tempered distribution is the product of INDEPENDENTLY tempered
   Bernoullis: A_ζ(o|s) = ∏_v Bern(temper-bernoulli p_v(s) ζ)(o_v). Hence
   replacing each p_v(s) by its tempered Bernoulli inside the existing
   token-likelihood formula yields the tempered likelihood exactly; verified
   against direct temper-row tempering of the enumerating
   observation-distribution (likelihood-precision-test, tolerance 1e-12).

   ζ = 1 is the identity (temperedLikelihood_one): the returned map equals
   the input modulo doubles. ζ = 0 declares the uninformative kernel: every
   interior p_v becomes 1/2 (a fair coin per token), p = 0 stays 0, p = 1
   stays 1. ζ < 0, non-number ζ, or any rate whose two-outcome row is not
   stochastic (NaN, outside [0,1]) refuses with the typed
   temper-row/temper-bernoulli refusal. RATES is {token {:false-neg fn
   :false-pos fp}} as on the cascade model manifest."
  [rates zeta]
  (reduce-kv
   (fn [acc token {:keys [false-neg false-pos]}]
     (let [fp' (temper-bernoulli false-pos zeta)]
       (if (refusal? fp')
         (reduced (assoc fp' :token token :branch :false-pos))
         (let [fn' (temper-bernoulli (- 1 false-neg) zeta)]
           (if (refusal? fn')
             (reduced (assoc fn' :token token :branch :false-neg))
             ;; p_v(s) for v ∈ s is 1−fn; its tempered value is fn',
             ;; so the effective false-neg is 1−temper(1−fn).
             (assoc acc token {:false-neg (- 1 fn') :false-pos fp'}))))))
   {} rates))

(def zeta-declaration
  "The named declaration of the likelihood precision the running machine uses
   today (R7-1). This is a record of provenance, not a new law: it names the
   implicit ζ of the live tick and refuses to invent a prior or update for it."
  {:item :R7
   :quantity :likelihood-precision-zeta
   :gibbs-operator "A(o|s)^ζ, row-renormalized (temper-row/temper-a)"
   :live-tick {:consumer "futon2.aif.efe/rank-cascade-actions → cascade-model-manifest/horizon-g-sparse (:adjudication-rates + :zeta on the scoring opts, 2026-09-18)"
               :rates :caller-declared
               :status :wired-declared-fixed
               :declared :fixed
               :reason "the zero-rate hardcode in rank-cascade-actions is gone: a caller may now declare non-zero adjudication rates (typed refusal :invalid-adjudication-rates if they do not cover the scored universe) and a declared FIXED ζ (threaded to the scorer, byte-identical at ζ=1). What no caller does yet is SOURCE real rates from futon2.aif.observation-rates (built, zero live consumers) — that sourcing is the next WIRE slice; until a production caller passes them, live runs still default to the identity kernel, now as an explicit default rather than a hardcode"}
   :prior {:law :none-declared
           :reason "Parr 2022 B.14–B.19 gamma-prior/fixed-point machinery is pinned in futon2 only for POLICY precision (futon2.aif.policy-precision, R14); a ζ-side prior is a new numerical law and is not chosen here (r7 cascade README: 'No new precision law, floor or gain is chosen here')"}
   :update :none
   :distinct-from
   {:policy-precision-gamma {:item :R14
                             :ns "futon2.aif.policy-precision"
                             :meaning "softmax inverse temperature on −F−γ·G over policies (β = 1/γ, B.19 fixed point); NOT the likelihood tempering exponent on A"}
    :legacy-weight-exponent-kappa {:ns "futon2.aif.belief"
                                   :law "κ(w) = log₂(1+w) applied to A rows"
                                   :meaning "legacy channel-weight exponent; retained in belief.clj, not reinterpreted as ζ"}}
   :wiring {:tempered-rates "tempered-rates — effective rates whose token-likelihood IS A_ζ (verified against temper-row of the enumerating observation-distribution); applicable to any consumer of rates: observation-distribution, predict-observations (Q(o|π)), horizon-g and horizon-g-sparse's factorized path"
            :demonstration "RUN-r7-zeta-effect-2026-09-18 (futon2/holes/labs/wm-contract/): recorded run showing a declared FIXED ζ measurably changing Q(o|π) and G through predict-observations, horizon-g and (addenda 2) the full efe/rank-actions tick path"
            :status "ζ is DECLARED FIXED (1 on the default identity path; other values declared explicitly per call); no prior/update law is invented"}
   ;; The SINGLE SOURCE of the GCertificate's ζ-provenance vocabulary
   ;; (claude-4 ruling, 2026-09-18): cascade-model-manifest's certificate
   ;; reads these keywords from here rather than restating them, so the
   ;; declaration and the certificate cannot drift apart. A declaration
   ;; EXISTS on the identity path too — the fixed ζ is vacuous there, not
   ;; absent — and the certificate must carry that distinction the same way
   ;; WIRE-2's :computed-not-attached carries it for F.
   :certificate-statuses
   {:identity-path :declared-fixed-vacuous
    :tempered-path :declared-fixed-applied
    :absent :absent}
   :gaps ["no production caller SOURCES real adjudication rates yet: futon2.aif.observation-rates is built with zero live consumers, and until a tick caller passes :adjudication-rates the live run still defaults to the identity kernel (an explicit default now, not a hardcode) — that sourcing is the next WIRE slice"
          "no gamma prior or B.14–B.19-style posterior update for ζ is pinned (R7-2 open; ζ is declared FIXED, which the checklist accepts as such)"]})

(def zeta-certificate-statuses
  "The GCertificate's ζ-provenance vocabulary, EXTRACTED from
   `zeta-declaration`'s :certificate-statuses at load time — the declaration
   is the single source; cascade-model-manifest's certificate reads these
   keywords from here rather than restating them, so the two cannot drift.
   :identity-path is declared-fixed-VACUOUS (the declaration exists; the
   identity kernel evaluates no likelihood matrix, so ζ multiplies nothing),
   :tempered-path is declared-fixed-APPLIED, :absent is reserved for a future
   run where ζ was never considered — the three states stay distinguishable."
  (:certificate-statuses zeta-declaration))

