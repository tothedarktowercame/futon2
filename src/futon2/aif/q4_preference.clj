(ns futon2.aif.q4-preference
  "Requirements Q4 on the reading-family route: for every compared policy,
  risk, ambiguity and information gain were evaluated; the horizon is
  positive; C states a preference at every step; and, under progressive
  semantics, every step is graded by completed-progress count.

  Compared means the posterior of the one G selection: the selector's menu
  after a Q6 requirement exclusion, the same set as Q8's posterior. A
  policy Q6 removed was scored and certified but not compared, and does not
  count.

  Every fact comes from the decision's certified preference receipts
  (reading-policy-preference/certify, one per scored policy, made before the
  selector), each bound to the compared policy by structural key, policy id
  and the exact G the selector compared. Per policy:
  - horizon and steps are the receipt's, and its schedule must equal the
    progress preference recomputed from the authority's own token count and
    horizon (one C, recomputable, bound to its authority);
  - a step is graded when, for each want outcome, the preference rises
    strictly with completed count;
  - semantics are :progressive when the authority's declared kind is
    :progress-count, the shape scorer's step-indexed progress C;
  - live C must be diagnostic-only.
  - the G normalization the receipt declares must be recognized and must
    reproduce, from the recorded raw sums, the terms whose sum is the G the
    selector compared.
  A term is evaluated when recorded and finite; zero counts.

  Each compared policy keeps its own horizon. Policies with different
  horizons are comparable only as per-step normalized rates
  (:per-step-capacity-and-pattern), which Requirements.Q4 states over the
  per-policy records; absent, unrecognized or unbound normalization is not
  recomputable, and differing recognized laws are recorded for Q4 to fail.

  The carrier is computed by the decision and recomputed by the exporter
  from the persisted decision alone."
  (:require [futon2.aif.cascade-observation-scoring :as scoring]
            [futon2.aif.cascade-shape-g :as shape]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.reading-policy-preference :as pref]))

(def schema :wm/q4-preference-v2)
(def seal previous-run/seal)
(def intact? previous-run/intact?)

(defn- finite? [x] (and (number? x) (Double/isFinite (double x))))

(defn- selected-normalization
  "Bind normalization to the selected decision row.  Current receipts carry
  the composite binding directly.  Pre-fix receipts carry the normalized
  progress component and fit ambiguity; their selected row retains the class
  emission certificate, so the same composite remains recomputable."
  [receipt row]
  (let [norm (:normalization receipt)]
    (if (:components norm)
      norm
      (if (= :raw-cumulative (get-in norm [:declaration :law]))
        (if (= (:terms norm)
               (scoring/normalize-g-terms (:declaration norm) (:raw norm)))
          (assoc norm :components
                 {:selected-decision {:declaration (:declaration norm)
                                      :raw (:raw norm) :terms (:terms norm)}})
          {:refusal :normalization-not-bound-to-g-terms})
      (let [class-cert (get-in row [:class-emission :certificate :g-terms])
            class-decl (:normalization-declaration class-cert)
            class-raw0 (:raw class-cert)
            class-raw {:risk (:risk class-raw0)
                       :ambiguity (:ambiguity class-raw0)
                       :information-gain (:expected-information-gain class-raw0)}
            class-terms (scoring/normalize-g-terms class-decl class-raw)
            progress (:terms norm)
            fit (:fit-ambiguity norm)
            composed (when (and class-terms (number? fit))
                       {:risk (+ (double (:risk progress)) (double (:risk class-terms)))
                        :ambiguity (+ (double (:ambiguity progress))
                                      (double (:ambiguity class-terms)) (double fit))
                        :information-gain (:information-gain progress)})]
        (if (and composed
                 (every? (fn [k]
                           (<= (Math/abs (- (double (get composed k))
                                            (double (get-in receipt [:g-terms k]))))
                               pref/normalization-tolerance))
                         [:risk :ambiguity :information-gain]))
          {:declaration (:declaration norm)
           :raw (:raw norm)
           :terms (:g-terms receipt)
           :fit-ambiguity fit
           :components {:progress {:declaration (:declaration norm)
                                    :raw (:raw norm) :terms progress}
                        :class-emission {:declaration class-decl
                                         :raw class-raw :terms class-terms}
                        :fit-ambiguity fit}}
          {:refusal :normalization-not-bound-to-g-terms}))))))

(defn graded-step?
  "True when DIST (outcome [done met?] -> weight) rises strictly with done
  for each met?, over at least two done values."
  [dist]
  (let [by-met (group-by second (keys dist))]
    (and (= #{false true} (set (keys by-met)))
         (every? (fn [[_ outcomes]]
                   (let [ws (map dist (sort-by first outcomes))]
                     (and (< 1 (count ws)) (apply < ws))))
                 by-met))))

(defn- normalization-refusal
  "nil when the receipt's normalization NORM is recognized, its inputs are
  the receipt's horizon H and schedule SCHED, and it reproduces the terms
  whose sum is the compared G; else the refusal kind."
  [norm h sched g terms]
  (let [{:keys [declaration raw components]} norm
        bound (:terms norm)
        top-recomputed (scoring/normalize-g-terms declaration raw)
        recomputed-components
        (for [[k c] (dissoc components :fit-ambiguity)]
          [k (scoring/normalize-g-terms (:declaration c) (:raw c))])]
    (cond
      (or (nil? declaration) (= :absent (:status norm))) :normalization-absent
      (:refusal norm) (:refusal norm)
      (or (nil? top-recomputed)
          (some (comp nil? second) recomputed-components)) :normalization-unrecognized
      (and (= :per-step-capacity-and-pattern (:law declaration))
           (not (and (= h (:horizon-steps declaration))
                     (= (max 2 (count (get sched 1))) (:outcome-count declaration)))))
      :normalization-inputs-not-the-receipt's
      (some (fn [[k recomputed]]
              (not= recomputed (get-in components [k :terms])))
            recomputed-components)
      :normalization-not-bound-to-g-terms
      (not= top-recomputed (or (get-in components [:progress :terms])
                               (get-in components [:selected-decision :terms])))
      :normalization-not-bound-to-g-terms
      (not (and (finite? g) (every? finite? (vals bound))
                (<= (Math/abs (- (double g) (- (+ (:risk bound) (:ambiguity bound))
                                               (:information-gain bound))))
                    pref/normalization-tolerance)))
      :normalization-not-bound-to-g-terms
      ;; a recorded finite term must be the normalized one
      (some (fn [[t v]] (and (finite? v) (not= v (get bound t)))) terms)
      :normalization-not-bound-to-g-terms)))

(defn- policy-facts
  "One compared policy's Q4 record from its RECEIPT and certificate ROW, or
  {:refusal kind}."
  [k receipt row]
  (let [authority (:authority receipt)
        h (:horizon receipt)
        sched (:preference receipt)
        terms (:g-terms receipt)
        ;; Rebuild the declared composite normalization from the exact scored
        ;; row whose G the selected decision compared.  The receipt is evidence
        ;; for C and the terms; it is not a second source of G normalization.
        norm (when row (selected-normalization receipt row))]
    (cond
      (nil? receipt) {:refusal :compared-policy-without-receipt}
      (not= :certified (:status receipt)) {:refusal :receipt-not-certified}
      (not= (str (:target receipt)) (nth k 1)) {:refusal :receipt-for-another-target}
      (not= (:g receipt) (:g row)) {:refusal :receipt-not-bound-to-compared-g}
      (nil? authority) {:refusal :preference-authority-lost}
      (not= {:role :diagnostic-only :selected false} (:live-c receipt))
      {:refusal :live-c-not-diagnostic}
      (not (and (integer? h) (pos? h))) {:refusal :horizon-not-positive}
      (not= h (:model-horizon authority)) {:refusal :horizon-not-the-authority's}
      (not= (if (= :canonical-coapply (:g-law receipt)) :class-emission :progress-count)
            (:kind authority)) {:refusal :preference-kind-not-recomputable}
      (not= sched (if (= :canonical-coapply (:g-law receipt))
                    (:class-preference (shape/class-emission-model ::ruled #{} #{} h))
                    (pref/recompute-progress-preference (:progress-tokens authority) h)))
      {:refusal :preference-not-recomputable}
      :else
      (if-let [refusal (normalization-refusal norm h sched (:g receipt) terms)]
        {:refusal refusal}
        {:policy-key k
         :policy-id (:policy-id receipt)
         :receipt (evidence/value-digest receipt)
         :horizon h
         ;; Lean indexes steps from 0; C-tau is stated for tau = 1..H
         :preference-steps (vec (sort (map dec (keys sched))))
         :graded-steps (if (= :canonical-coapply (:g-law receipt)) []
                         (vec (sort (for [[tau d] sched :when (graded-step? d)] (dec tau)))))
         :semantics (if (= :canonical-coapply (:g-law receipt)) :terminal-class :progressive)
         :normalization (select-keys (:declaration norm) [:law :version])
         :terms (into {} (for [t [:risk :ambiguity :information-gain]]
                           [t (finite? (get terms t))]))}))))

(defn carrier
  "The sealed Q4 carrier for DECISION (a reading-family selection)."
  [decision]
  (let [cert (:selection-certificate decision)
        compared (vec (sort-by pr-str (map prefix-admission/candidate-key
                                           (keys (get-in decision [:selection-law :posterior])))))
        rows (into {} (map (fn [r] [(prefix-admission/candidate-key (:id r)) r])) (:candidates cert))
        receipts (group-by :policy-id (remove #(= :canonical-coapply (:g-law %)) (:preference-receipts cert)))
        canonical-receipts (group-by (juxt (comp str :target) :policy-id)
                                    (filter #(= :canonical-coapply (:g-law %)) (:preference-receipts cert)))
        per (mapv (fn [k]
                    (let [rs (or (get canonical-receipts [(nth k 1 nil) (nth k 2 nil)])
                                 (get receipts (nth k 2 nil)))]
                      (if (< 1 (count rs))
                        {:refusal :competing-receipts}
                        (policy-facts k (first rs) (get rows k)))))
                  compared)
        refusal (some :refusal per)
        base {:schema schema :compared compared}]
    (seal
     (cond
       (not= :wm/reading-policy-family-v1 (:selection-route cert))
       (assoc base :status :not-recomputable :reason :not-the-reading-family-route)
       (empty? compared) (assoc base :status :not-recomputable :reason :no-compared-policy)
       (some #(not (prefix-admission/reading-policy-key (:id %))) (vals rows))
       (assoc base :status :not-recomputable :reason :compared-identity-not-structural)
       refusal (assoc base :status :not-recomputable :reason refusal)
       :else
       (let [count-of (fn [t] (count (filter #(get-in % [:terms t]) per)))]
         (assoc base :status :certified
                :policies per
                :g-terms {:risk (every? #(get-in % [:terms :risk]) per)
                          :ambiguity (every? #(get-in % [:terms :ambiguity]) per)
                          :information-gain (every? #(get-in % [:terms :information-gain]) per)}
                :counts {:risk (count-of :risk) :ambiguity (count-of :ambiguity)
                         :information-gain (count-of :information-gain)}))))))

(defn verify-record
  "The persisted decision's Q4 carrier when intact and equal to the carrier
  recomputed from the persisted decision; else a typed reason."
  [record]
  (let [decision (:decision record)
        q4 (get-in decision [:selection-certificate :q4])
        recomputed (carrier (update decision :selection-certificate dissoc :q4))
        legacy-normalization? (and (= :normalization-not-bound-to-g-terms (:reason q4))
                                   (= :certified (:status recomputed)))]
    (cond
      (nil? q4) {:status :not-recomputable :reason :q4-carrier-absent}
      (not (and (= schema (:schema q4)) (intact? q4)))
      {:status :not-recomputable :reason :q4-carrier-tampered}
      (and (not legacy-normalization?) (not= q4 recomputed))
      {:status :not-recomputable :reason :q4-carrier-not-bound-to-record}
      legacy-normalization? {:status :verified :carrier recomputed}
      (not= :certified (:status q4)) {:status :not-recomputable :reason (:reason q4)}
      :else {:status :verified :carrier q4})))

(defn lean-q4
  "Requirements.Q4, mirrored over a verified carrier, for tests."
  [q4]
  (let [ps (:policies q4)
        n (count (:compared q4))
        laws (set (map :normalization ps))]
    (and (every? true? (vals (:g-terms q4)))
         (= n (get-in q4 [:counts :risk]) (get-in q4 [:counts :ambiguity])
            (get-in q4 [:counts :information-gain]))
         ;; the per-policy records are exactly the compared policies
         (= (count ps) (count (set (map :policy-key ps))))
         (= (set (map :policy-key ps)) (set (:compared q4)))
         (every? (fn [p]
                   (let [rng (vec (range (:horizon p)))]
                     (and (every? true? (vals (:terms p)))
                          (pos? (:horizon p))
                          (= (:preference-steps p) rng)
                          (or (not= :progressive (:semantics p)) (= (:graded-steps p) rng)))))
                 ps)
         ;; one common law; only per-step rates compare different horizons
         (= 1 (count laws))
         (or (= :per-step-capacity-and-pattern (:law (first laws)))
             (= 1 (count (set (map :horizon ps))))))))
