(ns futon2.aif.check-error-rates
  "Measured check error rates per check kind (PROOF-2 packet A-S / hole H-A).

  measured-rates consumes a check ledger value of schema
  :m-futon-seams/check-ledger-v1 (22-row exemplar at
  futon3c/holes/labs/M-futon-seams/exemplar/check-ledger.edn) and returns
  per-kind counts, Jeffreys-smoothed rates, and 95% Wilson intervals, with
  typed absences for kinds below the minimum count. rates-measured? refuses
  rate tables that lack counts or provenance, or whose source ids do not
  resolve to ledger rows. Spec: holes/labs/wm-contract/proof2/packets/A-S.md."
  (:require [clojure.set :as set]))

(def ledger-schema :m-futon-seams/check-ledger-v1)

(def min-count
  "Minimum eligible runs below which a kind's rates are a typed
  {:status :insufficient :n ..} rather than numbers (A-S section 2)."
  5)

(def jeffreys-prior
  "Beta(1/2, 1/2): keeps small-sample estimates inside (0, 1) and does not
  privilege passes over fails."
  0.5)

(def ^:private z95 1.959963984540054)

;; ---------------------------------------------------------------- population

(defn eligible-row?
  "A row is eligible when its truth was established independently of the
  check. Machine rule (A-S section 1): it carries a non-absent :truth-source
  naming a constructed bad case, a later review, or an independent
  recomputation; a self-truthed row MUST NOT carry :truth-source and is
  excluded here."
  [row]
  (and (contains? #{true false} (:truth row))
       (contains? #{true false} (:passed row))
       (some? (:truth-source row))
       (some? (:kind row))
       (some? (:id row))))

(defn- expand-row
  "One entry per run; a row with :count k stands for k identical runs."
  [row]
  (repeat (max 1 (long (or (:count row) 1))) (dissoc row :count)))

;; ------------------------------------------------------------------ estimate

(defn- wilson-interval
  "95% Wilson score interval on raw counts k/n."
  [k n]
  (if (zero? n)
    {:status :no-denominator :n 0}
    (let [p (/ (double k) n)
          z2 (* z95 z95)
          denom (+ 1.0 (/ z2 n))
          centre (+ p (/ z2 (* 2.0 n)))
          adj (* z95 (Math/sqrt (+ (/ (* p (- 1.0 p)) n)
                                   (/ z2 (* 4.0 n n)))))]
      {:lo (max 0.0 (/ (- centre adj) denom))
       :hi (min 1.0 (/ (+ centre adj) denom))})))

(defn- smoothed-rate
  "Jeffreys-smoothed estimate (k + 1/2)/(n + 1); typed absence when the
  denominator class is empty."
  [k n]
  (if (zero? n)
    {:status :no-denominator :n 0}
    (/ (+ (double k) jeffreys-prior) (+ n (* 2.0 jeffreys-prior)))))

(defn- kind-rates
  "Per-kind measured rates from the kind's eligible expanded rows."
  [rows]
  (let [n (count rows)
        truth-true (filter :truth rows)
        truth-false (remove :truth rows)
        n-true (count truth-true)
        n-false (count truth-false)
        fp (count (filter :passed truth-false))
        fn_ (count (remove :passed truth-true))
        base {:n n
              :n-true n-true
              :n-false n-false
              :false-pass fp
              :false-fail fn_
              :source-ids (vec (distinct (map :id rows)))}]
    (if (< n min-count)
      (assoc base :status :insufficient)
      (assoc base
             :status :measured
             :fp-rate (smoothed-rate fp n-false)
             :fp-interval (wilson-interval fp n-false)
             :fn-rate (smoothed-rate fn_ n-true)
             :fn-interval (wilson-interval fn_ n-true)))))

(defn measured-rates
  "From a check ledger value of schema :m-futon-seams/check-ledger-v1 to
  {kind {:status :measured|:insufficient :n :n-true :n-false :false-pass
  :false-fail [:fp-rate :fp-interval :fn-rate :fn-interval] :source-ids}}.
  Rows without :truth-source are excluded (self-truthed); the exclusion is
  returned under ::excluded-ids, never counted. Throws typed ex-info on a
  wrong-schema ledger."
  [ledger]
  (when (not= ledger-schema (:schema ledger))
    (throw (ex-info "not a check ledger of the measured schema"
                    {:type :invalid-ledger
                     :expected ledger-schema
                     :actual (:schema ledger)})))
  (let [rows (:rows ledger)
        {eligible true excluded false} (group-by eligible-row? rows)]
    (into {::excluded-ids (mapv :id excluded)}
          (map (fn [[kind kind-rows]]
                 [kind (kind-rates (mapcat expand-row kind-rows))]))
          (group-by :kind eligible))))

;; ------------------------------------------------------------------ falsifier

(defn rates-measured?
  "True only when table is a measured rates table for ledger: every kind
  entry carries its counts and :source-ids resolving to ledger row ids, and
  asserts rates only where the kind's status permits. Returns true or a
  typed refusal map {:status :refused :reason .. :detail ..}."
  [table ledger]
  (let [ledger-ids (set (keep :id (:rows ledger)))
        entries (dissoc table ::excluded-ids)]
    (if (empty? entries)
      {:status :refused :reason :empty-table}
      (or (some (fn [[kind entry]]
                  (cond
                    (not (and (integer? (:n entry))
                              (integer? (:false-pass entry))
                              (integer? (:false-fail entry))))
                    {:status :refused :reason :missing-counts :detail {:kind kind}}

                    (or (not (sequential? (:source-ids entry)))
                        (empty? (:source-ids entry)))
                    {:status :refused :reason :missing-source-ids :detail {:kind kind}}

                    (not (set/subset? (set (:source-ids entry)) ledger-ids))
                    {:status :refused :reason :unresolved-source-ids
                     :detail {:kind kind
                              :unknown (vec (set/difference (set (:source-ids entry))
                                                            ledger-ids))}}

                    (and (not= :measured (:status entry))
                         (or (contains? entry :fp-rate) (contains? entry :fn-rate)))
                    {:status :refused :reason :rates-over-absence :detail {:kind kind}}

                    :else nil))
                entries)
          true))))
