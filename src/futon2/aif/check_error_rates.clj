(ns futon2.aif.check-error-rates
  "Measured check error rates per check kind (PROOF-2 packet A-S / hole H-A).

  measured-rates consumes a check ledger value of schema
  :m-futon-seams/check-ledger-v1 (22-row exemplar at
  futon3c/holes/labs/M-futon-seams/exemplar/check-ledger.edn) and returns
  per-kind counts, Jeffreys-smoothed rates, and 95% Wilson intervals, with
  typed absences for kinds below the minimum count. Eligibility is decided
  by a recorded truth KIND (truth-kinds), never by free-text :truth-source
  (Revision 2, after claude-8's review of cc831860). rates-measured? refuses
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

(def truth-kinds
  "The kinds of independently established truth that make a ledger row
  eligible (A-S Revision 2). Eligibility is decided by a recorded KIND,
  never by the presence of free text: a non-nil :truth-source string is
  not evidence of independence (claude-8's review of cc831860: six rows
  whose :truth-source is 'the check itself passed, so it held' were
  counted as eligible under the old rule)."
  #{:constructed-bad-case :later-review :independent-recomputation})

(defn- classification-kind
  "The truth kind a classification map records for a row id. The entry
  may be a bare kind keyword or a map carrying :kind (and e.g. :reason)."
  [classification id]
  (let [v (get classification id)]
    (cond (keyword? v) v
          (map? v) (:kind v)
          :else nil)))

(defn- row-truth-kind
  "The row's own :truth-kind if present, else the classification's entry."
  [row classification]
  (or (:truth-kind row)
      (classification-kind classification (:id row))))

(defn eligible-row?
  "A row is eligible when its truth was established independently of the
  check, decided by KIND (A-S Revision 2): its truth kind — :truth-kind on
  the row if present, else the classification map's entry for its :id —
  must be in truth-kinds. Free-text :truth-source plays no part."
  [row classification]
  (and (contains? #{true false} (:truth row))
       (contains? #{true false} (:passed row))
       (some? (:kind row))
       (some? (:id row))
       (contains? truth-kinds (row-truth-kind row classification))))

(defn- exclusion-reason
  "Why a row is ineligible: :missing-fields (no boolean :truth/:passed, or
  no :kind/:id), :self-truthed (kind recorded as :self-truthed), or
  :unclassified (no kind recorded at all — including a row carrying only
  free-text :truth-source)."
  [row classification]
  (if (not (and (contains? #{true false} (:truth row))
                (contains? #{true false} (:passed row))
                (some? (:kind row))
                (some? (:id row))))
    :missing-fields
    (let [k (row-truth-kind row classification)]
      (if (= :self-truthed k) :self-truthed :unclassified))))

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

  Eligibility is by truth KIND (A-S Revision 2): a row counts only when
  its :truth-kind, or the classification map's entry for its :id, is in
  truth-kinds. classification is {row-id kind-or-{:kind .. :reason ..}}
  plus :classification-source naming who classified and when. Exclusions
  are returned under ::excluded-ids as {id reason} with reason
  :unclassified | :self-truthed | :missing-fields, never counted; every
  kind present in the ledger appears in the result even when no row of it
  is eligible (its counts are then 0 and its status :insufficient).
  ::classification-source carries the classification's provenance, or a
  typed absence.

  The one-arg form passes no classification: every row is excluded
  :unclassified and every kind is {:status :insufficient :n 0 ...} — the
  true state of a ledger nobody has classified. Throws typed ex-info on a
  wrong-schema ledger."
  ([ledger]
   (measured-rates ledger nil))
  ([ledger classification]
   (when (not= ledger-schema (:schema ledger))
     (throw (ex-info "not a check ledger of the measured schema"
                     {:type :invalid-ledger
                      :expected ledger-schema
                      :actual (:schema ledger)})))
   (let [rows (:rows ledger)
         eligible? #(eligible-row? % classification)
         excluded (remove eligible? rows)]
     (into {::excluded-ids (into {}
                                 (map (fn [r] [(:id r) (exclusion-reason r classification)]))
                                 excluded)
            ::classification-source (or (:classification-source classification)
                                        {:status :absent :reason :no-classification})}
           (map (fn [[kind kind-rows]]
                  [kind (kind-rates (mapcat expand-row (filter eligible? kind-rows)))]))
           (group-by :kind (filter :kind rows))))))

;; ------------------------------------------------------------------ falsifier

(defn rates-measured?
  "True only when table is a measured rates table for ledger: every kind
  entry carries its counts and :source-ids resolving to ledger row ids, and
  asserts rates only where the kind's status permits. Returns true or a
  typed refusal map {:status :refused :reason .. :detail ..}."
  [table ledger]
  (let [ledger-ids (set (keep :id (:rows ledger)))
        entries (dissoc table ::excluded-ids ::classification-source)]
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
