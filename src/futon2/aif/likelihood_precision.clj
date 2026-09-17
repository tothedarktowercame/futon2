(ns futon2.aif.likelihood-precision
  "R7: likelihood precision ζ as a Gibbs inverse temperature on the likelihood
   A, DECLARED as a named quantity — distinct from policy precision γ (R14).

  What exists on the live tick today (2026-09-18 census, efe.clj
  rank-cascade-actions → cascade_model_manifest/horizon-g-sparse at
  :rates :zero-adjudication-identity): at zero adjudication rates A is the
  identity kernel (Lean tokenLikelihood_checkable), so no likelihood matrix
   is ever evaluated and the ambiguity term is identically 0. Whatever ζ the
   machine 'uses' is therefore implicit and vacuous — this namespace names it
   as a FIXED declaration rather than leaving it undeclared.

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

(def zeta-declaration
  "The named declaration of the likelihood precision the running machine uses
   today (R7-1). This is a record of provenance, not a new law: it names the
   implicit ζ of the live tick and refuses to invent a prior or update for it."
  {:item :R7
   :quantity :likelihood-precision-zeta
   :gibbs-operator "A(o|s)^ζ, row-renormalized (temper-row/temper-a)"
   :live-tick {:consumer "futon2.aif.cascade-model-manifest/horizon-g-sparse"
               :via "futon2.aif.efe/rank-cascade-actions"
               :rates :zero-adjudication-identity
               :status :implicit-and-vacuous
               :declared :fixed-vacuous
               :reason "at zero adjudication rates A is the identity kernel (Lean tokenLikelihood_checkable); the ambiguity term is identically 0 and no likelihood matrix is evaluated, so ζ multiplies nothing on the live scoring path — declared FIXED rather than left implicit"}
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
   :gaps ["no consumer on the live tick path evaluates a likelihood matrix, so an applicable consumer for a learned ζ does not exist yet (R7-3 open)"
          "no gamma prior or B.14–B.19-style posterior update for ζ is pinned (R7-2 open)"
          "the enumerating horizon-g path with non-zero judgement rates refuses (:judgement-rates-not-supported-at-scale), so no scored A exists anywhere in the tick today"]})
