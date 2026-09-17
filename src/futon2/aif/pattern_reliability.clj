(ns futon2.aif.pattern-reliability
  "Attested θ seed counts per pattern, per context family
   (PROPOSAL-policy-precision-learning §3–§4, approved by Joe 2026-09-17).
   Seeds Beta(1 + realised, 1 + not-realised) per [context pattern] from the
   retrospective cascade outcomes; the counts are upper bounds and biased
   upwards because only worked steps got observed (:unknown is unobserved,
   never failure — that bias is stated on every θ). Additive namespace: no
   live callers, existing behaviour unchanged."
  (:require [clojure.string :as str]))

(def trace-belongs-to-r
  "TRACE targets belong to the :R family, per the data file's own note
   (DATA-cascade-outcomes-2026-09-17.edn).")

(def not-realised-note
  "The only :contradicted case, WM-10-C6 (connect G to the choice consumer),
   is unmet rather than failed: it was never attempted on production, so it
   counts as not realised and is NOT evidence that the pattern failed when
   applied. Source evidence field, DATA-cascade-outcomes-2026-09-17.edn.")

(defn- family-of
  "The context family of a cascade target: :WM, :R (TRACE included) or :E."
  [target]
  (cond
    (not (string? target)) nil
    (str/starts-with? target "WM") :WM
    (or (str/starts-with? target "R") (str/starts-with? target "TRACE")) :R
    (str/starts-with? target "E") :E
    :else nil))

(defn- count-node [acc context {:keys [pattern outcome]}]
  (cond
    (not (string? pattern)) {:status :missing :kind :invalid-node :reason :pattern}
    (not (keyword? outcome)) {:status :missing :kind :invalid-node :reason :outcome}
    (not (contains? #{:realised :contradicted :unknown} outcome))
    {:status :missing :kind :invalid-node-outcome :outcome outcome :pattern pattern}
    :else (let [kk (case outcome :realised :realised :contradicted :not-realised :unobserved)]
            (update acc [context pattern]
                    (fn [m] (-> {:realised 0 :not-realised 0 :unobserved 0}
                                (merge m)
                                (update kk inc)))))))

(defn seed-counts
  "From the parsed outcome-data map (the EDN of
   DATA-cascade-outcomes-2026-09-17.edn), {[context pattern]
   {:realised r :not-realised n :unobserved u}}. Context is the target family
   (:R, :WM or :E; TRACE belongs to :R). :realised counts as realised;
   :contradicted counts as not realised (unmet rather than failed, see
   not-realised-note); :unknown is unobserved, not failure. Malformed input
   is a typed refusal."
  [outcome-data]
  (if-not (and (map? outcome-data) (vector? (:cascades outcome-data)))
    {:status :missing :kind :invalid-outcome-data}
    (reduce
      (fn [acc {:keys [target nodes]}]
        (let [context (family-of target)]
          (cond
            (nil? context) (reduced {:status :missing :kind :invalid-target :target target})
            (not (vector? nodes)) (reduced {:status :missing :kind :invalid-cascade :target target})
            :else (reduce (fn [acc node]
                            (let [acc' (count-node acc context node)]
                              (if (contains? acc' :status)
                                (reduced (reduced acc'))
                                acc')))
                          acc nodes))))
      {}
      (:cascades outcome-data))))

(defn- pattern-key
  "Pattern ids arrive as strings from the outcome data (\"ns/name\") and as
   keywords from manifest patterns (:ns/name); both key the same pattern."
  [id]
  (if (keyword? id) (subs (str id) 1) (str id)))

(defn theta
  "The attested interpretation θ of a pattern in a context:
   {:theta (1 + r) / (2 + r + n) :basis … :bias :observed-only-when-worked},
   an exact rational, never 1. A pair with no observed outcomes, including a
   pair absent from the counts, returns the Beta(1,1) prior mean θ = 1/2 with
   :basis :prior-only and :prior-reason naming why. It is recorded, not silent,
   so patterns new since the retrospective data can still be scored. The bias
   note is required (proposal §3): only worked steps got observed, so these
   are upper bounds."
  [counts context pattern]
  (let [m (get counts [context (pattern-key pattern)])
        r (or (:realised m) 0)
        n (or (:not-realised m) 0)
        u (or (:unobserved m) 0)]
    (if (pos? (+ r n))
      {:theta (/ (+ 1 r) (+ 2 r n))
       :basis {:realised r :not-realised n :unobserved u}
       :bias :observed-only-when-worked}
      {:theta 1/2 :basis :prior-only
       :prior-reason (if (nil? m) :pattern-absent-from-outcome-data :no-observed-outcomes)
       :bias :observed-only-when-worked})))

(defn attest-patterns
  "Set each manifest pattern's :theta from theta (replacing the documented
   default 1 that cascade-model-manifest/with-pattern-theta would otherwise
   apply) and record :theta-basis, :theta-prior-reason when prior-only, and
   :theta-source (:attested-observation or :beta-1-1-prior) on the pattern."
  [patterns counts context]
  (mapv (fn [p]
          (let [t (theta counts context (:id p))]
            (cond-> (assoc p :theta (:theta t)
                             :theta-basis (:basis t)
                             :theta-source (if (= :prior-only (:basis t))
                                             :beta-1-1-prior
                                             :attested-observation))
              (:prior-reason t) (assoc :theta-prior-reason (:prior-reason t)))))
        patterns))
