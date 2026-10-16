(ns futon2.aif.reading-policy-preference
  "Certify the ONE step-indexed C/preference that contributed to a reading
  policy's G (NOTE-wiring-reading-to-decision 2026-09-30 §7.3/§7.5.5;
  DarkTower/WarMachine/Requirements.lean Q4).

  The reading-policy shape scorer (cascade_shape_g.clj) scores each arranged
  cascade against a step-indexed progress preference over
  [completed-pattern-count want-met?] and returns, per policy, the schedule
  (:preference-at-each-step), the horizon, and the G terms the scorer
  recorded.  The old live-C derived inside cascade_decision is NOT a second
  selection preference: per §7.3 the class-scoring branch records live-C
  without scoring it (cascade_decision.clj:972-985), so this receipt types
  its role as :diagnostic-only/did-not-select.

  Pure: no IO, no scoring, no selection.  The validator reads a scored
  record (the map cascade-shape-g/score-arranged returns), checks it, and
  returns a typed receipt or an equally typed refusal.  Where the scored
  record lacks evidence the validator needs (model provenance/pins), it
  returns a precise typed absence and names the architectural gap rather
  than fabricating the missing authority."
  (:require [clojure.set :as cset]
            [futon2.aif.cascade-observation-scoring :as scoring]))

(def receipt-schema (keyword "wm/reading-policy-preference-receipt/v1"))

(def normalization-tolerance 1e-9)

;; ---------------------------------------------------------------------------
;; Authority: the one declared C of the shape scorer.  The scorer's model
;; carries the schedule under :progress-preference with provenance; the
;; recorded result carries it as :preference-at-each-step.  Live-C from the
;; old decision is diagnostic only and never certifiable here.
;; ---------------------------------------------------------------------------

(defn- double-finite? [x]
  (and (number? x) (Double/isFinite (double x))))

(defn normalized-distribution?
  "True when DIST (a map outcome -> weight) sums to 1 within tolerance,
  every weight is a finite number, and no weight is negative (a negative
  weight paired with one above 1 still sums to 1 but is not a
  distribution)."
  [dist]
  (and (map? dist) (seq dist)
       (every? (comp double-finite? val) dist)
       (every? (comp #(>= % 0.0) val) dist)
       (let [sum (reduce + 0.0 (map double (vals dist)))]
         (<= (Math/abs (- sum 1.0)) 1e-9))))

(defn preference-distributions
  "Every preference schedule the scored RECORD states, with where it was
  found.  Returns a vector of {:at ... :distribution ...}.  A record whose
  model authority survived (the scorer's :cascade-scoring meta on
  :scorer-result) contributes its :progress-preference; the recorded
  :preference-at-each-step contributes one; a :class-preference on the model
  is a second, competing shape."
  [record]
  (keep identity
        [(when-let [sched (:preference-at-each-step record)]
           {:at :recorded-schedule :distribution sched})
         (when-let [model (get-in (meta (:scorer-result record)) [:cascade-scoring :model])]
           (when-let [sched (:progress-preference model)]
             {:at :model-progress-preference :distribution sched}))
         (when-let [model (get-in (meta (:scorer-result record)) [:cascade-scoring :model])]
           (when-let [sched (:class-preference model)]
             {:at :model-class-preference :distribution sched}))]))

(defn- schedule= [a b]
  (= a b))

;; ---------------------------------------------------------------------------
;; Recomputability: the shape scorer's progress preference is a function of
;; the progress-token count and the horizon alone (cascade_shape_g.clj
;; progress-preference, private there).  This is a local mirror so the
;; receipt can recompute the schedule from the model's declared inputs; the
;; test suite asserts equality against the private original.
;; ---------------------------------------------------------------------------

(defn recompute-progress-preference
  "Mirror of cascade_shape_g's progress-preference: exp(4*done/n + 2*met?),
  normalized, over [done met?] for done in 0..n, repeated per step 1..H."
  [n horizon]
  (let [outcomes (for [done (range (inc n)) met? [false true]] [done met?])
        weights (into {} (for [[done met? :as outcome] outcomes]
                           [outcome (Math/exp (+ (* 4.0 (/ done (max 1 n)))
                                                  (if met? 2.0 0.0)))]))
        total (reduce + (vals weights))
        distribution (update-vals weights #(/ % total))]
    (into {} (for [tau (range 1 (inc horizon))] [tau distribution]))))

(defn- model-authority [record]
  (let [model (get-in (meta (:scorer-result record)) [:cascade-scoring :model])]
    (when model
      {:source (get-in model [:provenance :source])
       :provenance-status (get-in model [:provenance :status])
       :calibrated (get-in model [:provenance :calibrated])
       :model-schema (:schema model)
       :backend (:backend model)
       :kind (:kind model)
       :model-horizon (:horizon model)
       :progress-tokens (count (:progress-tokens model))})))

;; ---------------------------------------------------------------------------
;; Validation and receipt
;; ---------------------------------------------------------------------------

(defn normalization-binding
  "The G normalization the scored RECORD declares, bound to the terms the
  scorer produced, against a SCHEDULE of horizon H; or {:refusal kind}.
  The scorer's terms (before the reading fit's ambiguity is added) must be
  exactly the declared normalization of the recorded raw sums, and the
  declaration's horizon, outcome count and pattern count must be the
  record's own. A record with no declaration yields {:status :absent}."
  [record sched h]
  (let [decl (:g-normalization record)
        raw (:raw-g-terms record)
        terms {:risk (:risk record)
               :ambiguity (if (contains? record :scorer-ambiguity)
                            (:scorer-ambiguity record) (:ambiguity record))
               :information-gain (:information-gain record)}
        per-step? (= :per-step-capacity-and-pattern (:law decl))]
    (cond
      (nil? decl) {:status :absent :reason :no-normalization-declaration-in-scored-record}
      (nil? (scoring/normalize-g-terms decl raw)) {:refusal :g-normalization-unrecognized}
      (and per-step?
           (not (and (= h (:horizon-steps decl))
                     (= (max 2 (count (get sched 1))) (:outcome-count decl))
                     (= (scoring/candidate-pattern-count (:candidate record))
                        (:pattern-count decl)))))
      {:refusal :g-normalization-inputs-not-the-record's}
      (not= terms (scoring/normalize-g-terms decl raw))
      {:refusal :g-normalization-not-bound-to-terms}
      :else {:declaration decl :raw raw :terms terms
             :fit-ambiguity (get-in record [:fit :fit-ambiguity] 0.0)})))

(defn- refuse [record kind & extras]
  (merge {:schema receipt-schema
          :status :refused :kind kind
          :policy-id (:policy-id record) :target (:target record)}
         (apply hash-map extras)))

(defn validate
  "Validate one scored reading-policy RECORD and return its typed receipt,
  a typed refusal, or a typed absence.  Checks: exactly one declared C;
  per-step normalization and finiteness; step indices exactly 1..horizon;
  model horizon agreement; recomputability of the schedule from the model's
  declared inputs; presence, finiteness and internal agreement of the
  recorded G terms (risk/ambiguity/EIG/G) as far as the scorer's evidence
  supports them.  Never scores, never selects, never invents authority."
  [record]
  (let [stated (preference-distributions record)
        ;; A second, distinct schedule among the declared-c slots is §7.5.5's
        ;; two-competing-Cs bad case; identical restatements are one C.
        ;; Set-based, so A,B,A is still exactly two distinct schedules, and
        ;; the diagnostic names where each distinct schedule was stated.
        distinct-at (into (sorted-set-by (fn [a b] (compare (pr-str a) (pr-str b))))
                          (map :distribution) stated)]
    (cond
      (not (seq stated))
      (refuse record :preference-schedule-absent
              :reason "no :preference-at-each-step and no model authority on the scored record")

      (> (count distinct-at) 1)
      (refuse record :competing-preferences
              :found (mapv :at stated)
              :stated-at (mapv (fn [d] {:at (mapv :at (filter #(= (:distribution %) d) stated))})
                               distinct-at)
              :reason "two different preference schedules both stated as C (§7.3/§7.5.5)")

      :else
      (let [sched (:distribution (first stated))
            horizon (:horizon record)
            authority (model-authority record)
            taus (set (keys sched))
            expected-taus (set (range 1 (inc horizon)))
            unnormalized (not (every? normalized-distribution? (vals sched)))
            bad-steps (not= taus expected-taus)
            horizon-mismatch (and authority (:model-horizon authority)
                                  (not= (:model-horizon authority) horizon))
            n (if authority
                (or (:progress-tokens authority)
                    ;; fall back to the schedule's own outcome space
                    (count (distinct (map first (keys (first (vals sched)))))))
                (count (distinct (map first (keys (first (vals sched)))))))
            recomputed (recompute-progress-preference n horizon)
            shape-scorer-c? (= (:kind authority) :progress-count)
            recompute-mismatch (and shape-scorer-c? (not (schedule= sched recomputed)))
            g-terms {:risk (:risk record)
                     :ambiguity (:ambiguity record)
                     :information-gain (:information-gain record)}
            missing-terms (cset/difference (set (keys g-terms))
                                           (set (for [[k v] g-terms :when (some? v)] k)))
            nonfinite-terms (set (for [[k v] g-terms
                                       :when (and v (not (double-finite? v)))] k))
            g (:g record)
            cert-terms (get-in record [:scorer-result 0 :certificate :g-terms])
            ;; score-policy (cascade_shape_g) adds the reading fit's stated
            ;; uncertainty to the arranged scorer's ambiguity and recomputes
            ;; G, keeping the scorer's own value as :scorer-ambiguity. The
            ;; certificate holds the scorer's terms, so compare the
            ;; scorer's ambiguity with it, and require the record's
            ;; ambiguity and G to be exactly that composition.
            fit-adjusted? (contains? record :scorer-ambiguity)
            scorer-terms (cond-> g-terms
                           fit-adjusted? (assoc :ambiguity (:scorer-ambiguity record)))
            terms-disagree (seq (for [k [:risk :ambiguity :information-gain]
                                      :when (and (contains? cert-terms k)
                                                 (some? (get scorer-terms k))
                                                 (not= (get cert-terms k) (get scorer-terms k)))]
                                  k))
            normalization (normalization-binding record sched horizon)
            fit-ambiguity (get-in record [:fit :fit-ambiguity])
            composition-wrong
            (and fit-adjusted?
                 (or (not (and (double-finite? (:scorer-ambiguity record))
                               (double-finite? fit-ambiguity)))
                     (> (Math/abs (- (double (:ambiguity record))
                                     (+ (double (:scorer-ambiguity record))
                                        (double fit-ambiguity))))
                        normalization-tolerance)
                     (and (double-finite? g)
                          (every? double-finite? (vals g-terms))
                          (> (Math/abs (- (double g)
                                          (- (+ (double (:risk record))
                                                (double (:ambiguity record)))
                                             (double (:information-gain record)))))
                             normalization-tolerance))))]
        (cond
          unnormalized
          (refuse record :preference-not-normalized
                  :steps (vec (for [[tau d] sched :when (not (normalized-distribution? d))] tau)))

          bad-steps
          (refuse record :preference-steps-mismatch
                  :stated (sort taus) :expected (sort expected-taus) :horizon horizon)

          horizon-mismatch
          (refuse record :horizon-mismatch
                  :record-horizon horizon :model-horizon (:model-horizon authority))

          recompute-mismatch
          (refuse record :preference-not-recomputable
                  :reason "stated schedule differs from the progress preference recomputed from the model's own n and horizon")

          (seq missing-terms)
          (refuse record :g-terms-absent :missing (vec missing-terms))

          (seq nonfinite-terms)
          (refuse record :g-terms-not-finite :nonfinite (vec nonfinite-terms))

          terms-disagree
          (refuse record :g-terms-mismatch :differing (vec terms-disagree)
                  :reason "record's G terms differ from the scorer certificate's")

          composition-wrong
          (refuse record :g-composition-mismatch
                  :reason "record's ambiguity or G is not the scorer's term plus the stated fit ambiguity")

          (:refusal normalization)
          (refuse record (:refusal normalization)
                  :reason "the declared G normalization does not reproduce the scorer's terms from the recorded raw sums")

          (nil? g)
          (refuse record :g-absent)

          (not (double-finite? g))
          (refuse record :g-not-finite :g g)

          ;; The package purpose is to bind the one C to its declared
          ;; authority; a record whose model authority was lost (JSON
          ;; round-trip strips the scorer-result metadata) certifies
          ;; nothing.  Typed absence, not a degraded certificate.
          (nil? authority)
          (refuse record :preference-authority-absent
                  :reason "scoring model authority absent (metadata lost or never recorded); the schedule cannot be bound to its declared authority"
                  :architectural-gap :model-provenance-not-retained-in-record)

          ;; An authority whose model kind is not the shape scorer's
          ;; progress-count cannot have its schedule recomputed by this
          ;; validator; claiming recomputation would be fabrication.
          (not shape-scorer-c?)
          (refuse record :preference-kind-not-recomputable
                  :model-kind (or (:kind authority) :none)
                  :reason "no recomputation rule for this model kind; refusing rather than certifying an unrecomputed schedule")

          :else
          (cond-> {:schema receipt-schema :status :certified
                   :policy-id (:policy-id record) :target (:target record)
                   :horizon horizon
                   :preference sched
                   :g g :g-terms g-terms
                   :normalization normalization
                   ;; The old live-C is diagnostic only and did not select
                   ;; (§7.3: recorded, not scored, cascade_decision.clj:972-985).
                   :live-c {:role :diagnostic-only :selected false}
                   :checks {:one-declared-c true
                            :normalized true :finite true
                            :steps-cover-horizon true
                            :schedule-recomputed true
                            :g-terms-finite-and-agreeing true}}
            authority (assoc :authority (assoc authority
                                               ;; the shape scorer pins no
                                               ;; library bytes into its C
                                               :pins {:status :absent
                                                      :reason :no-pins-in-provenance})))
          )))))

(defn certify
  "Convenience: validate and return the receipt, or the typed
  refusal/absence as-is."
  [record]
  (validate record))

(def architectural-gaps
  "Gaps in the existing scorer evidence this validator had to type rather
  than bridge (reported, not worked around).  Both are now typed refusals
  (:preference-authority-absent) rather than silently degraded
  certificates:
  - cascade_shape_g/score-arranged does not copy the scoring model or its
    provenance into the returned record; the authority survives only as
    metadata on :scorer-result, so any JSON round-trip loses it.
  - The synthetic progress C carries no version or library pins, so
    'bound to its declared authority/version and pins' can only bind to
    the model's :provenance source string; pin absence is typed, not
    guessed."
  [:model-provenance-not-retained-in-record :no-pins-in-synthetic-c])
