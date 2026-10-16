(ns futon2.aif.q9-factorization
  "Requirements Q9 on the reading-family route, negative only (codex-68
  ruling (a), 2026-10-16). Q9 asks for a factorized preference: completion
  lexicographically prior to a relevance calibration (focused < related <
  unrelated costs), the calibration and the factorization law each
  authorized in a retained registry by an authority that is not the
  selector (FactorizedPreferenceSemantics.Certificate.Valid).

  The C that selects on this route is cascade-shape-g's progress preference
  over [completed-pattern-count want-met?], weight exp(4 done/n + 2 met?),
  provenance :synthetic, :calibrated false. No authority outside the
  selector calibrates it, and production loads no calibration or
  factorization registry. This carrier therefore certifies only that Q9
  does not hold, and only from the C the selector actually compared.

  For every compared policy (the posterior of the one G selection, as Q4
  counts them) the carrier binds the policy's certified preference receipt
  by policy id, target and the exact G the selector compared, requires its
  schedule to equal the progress preference recomputed from the receipt
  authority's own token count and horizon, and then reads off the schedule
  itself:
  - its outcome space: [count met?] pairs, with no relevance coordinate;
  - its completion ordering: completion is lexicographically prior only if,
    at every step, the least-preferred completed outcome outweighs the
    most-preferred uncompleted one; the carrier records the step and pair
    that decide it, with their weights.
  The receipt authority's calibration status and the decision's declared
  preference authority are recorded as they stand.

  The carrier is :factorization-absent (Q9 false) only when every compared
  C was recomputed and every one lacks a relevance coordinate, read from
  the schedule. Any gap in that chain is :not-recomputable. A decision that
  carries a factorized-preference carrier of its own is refused: nothing on
  this route seals one, so it can only be self-asserted. There is no
  positive status. Making Q9 true needs a calibration and factorization
  authority outside the selector and a decision about the selection law;
  neither exists, and this namespace does not stand in for them.

  The runner seals the carrier into the record; `verify-record` recomputes
  it from the record alone and the exporter reads only a verified carrier."
  (:require [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.reading-policy-preference :as pref]))

(def schema :wm/q9-factorization-v1)
(def seal previous-run/seal)
(def intact? previous-run/intact?)
(def digest evidence/value-digest)

;; -- the schedule, read as a preference over outcomes ---------------------------

(defn outcome-space
  "The coordinates of SCHED's outcomes (step -> outcome -> weight):
  :count-and-completion when every outcome at every step is a
  [completed-count want-met?] pair, else :unrecognized."
  [sched]
  (if (and (seq sched)
           (every? (fn [d]
                     (and (seq d)
                          (every? (fn [o] (and (vector? o) (= 2 (count o))
                                               (nat-int? (first o)) (boolean? (second o))))
                                  (keys d))))
                   (vals sched)))
    :count-and-completion
    :unrecognized))

(defn completion-ordering
  "Whether completion is lexicographically prior in SCHED: at every step,
  every completed outcome strictly outweighs every uncompleted one. Returns
  {:lexicographic? b :witness {...}}; the witness is the first step at
  which the most-preferred uncompleted outcome is at least the
  least-preferred completed one, or nil."
  [sched]
  (let [witness
        (first
         (for [tau (sort (keys sched))
               :let [d (get sched tau)
                     done (filter second (keys d))
                     undone (remove second (keys d))]
               :when (and (seq done) (seq undone))
               :let [least (apply min-key d (sort done))
                     most (apply max-key d (sort undone))]
               :when (>= (d most) (d least))]
           {:step tau
            :uncompleted most :uncompleted-weight (d most)
            :completed least :completed-weight (d least)}))]
    {:lexicographic? (nil? witness) :witness witness}))

;; -- one compared policy --------------------------------------------------------

(defn- policy-c
  "The C compared for policy key K, from its preference RECEIPT and
  certificate ROW, or {:refusal kind}."
  [k receipt row]
  (let [authority (:authority receipt)
        h (:horizon receipt)
        sched (:preference receipt)]
    (cond
      (nil? receipt) {:refusal :compared-policy-without-receipt}
      (not= :certified (:status receipt)) {:refusal :receipt-not-certified}
      (not= (str (:target receipt)) (nth k 1)) {:refusal :receipt-for-another-target}
      (not= (:g receipt) (:g row)) {:refusal :receipt-not-bound-to-compared-g}
      (nil? authority) {:refusal :preference-authority-lost}
      (not= :progress-count (:kind authority)) {:refusal :preference-kind-not-recomputable}
      (not (and (integer? h) (pos? h) (= h (:model-horizon authority))))
      {:refusal :horizon-not-the-authority's}
      (not= sched (pref/recompute-progress-preference (:progress-tokens authority) h))
      {:refusal :preference-not-recomputable}
      :else
      (let [space (outcome-space sched)]
        (if (= :unrecognized space)
          {:refusal :outcome-space-not-recognized}
          {:policy-key k
           :policy-id (:policy-id receipt)
           :g (:g receipt)
           :receipt (digest receipt)
           :schedule (digest sched)
           :horizon h
           :authority (select-keys authority [:source :provenance-status :calibrated
                                              :kind :progress-tokens :model-horizon])
           :outcome-space space
           :completion (completion-ordering sched)})))))

;; -- the carrier ----------------------------------------------------------------

(def ^:private registry-keys
  "Where a decision would carry Q9 registries or a factorized carrier. The
  production route writes none of them."
  [:factorized-preference :calibration-authorizations :factorization-authorizations])

(defn- self-asserted [decision]
  (seq (for [k registry-keys
             path [[k] [:selection-certificate k]]
             :when (some? (get-in decision path))]
         path)))

(defn- negative-reasons
  "What the compared Cs lack for Q9, read from the policies PER and the
  receipt authorities, in a fixed order."
  [per]
  (cond-> []
    (every? #(= :count-and-completion (:outcome-space %)) per)
    (conj :no-relevance-coordinate)
    (some #(not (get-in % [:completion :lexicographic?])) per)
    (conj :completion-not-lexicographic)
    (some #(not (true? (get-in % [:authority :calibrated]))) per)
    (conj :calibration-authority-is-selector)
    true (conj :no-calibration-registry :no-factorization-registry)))

(defn certificate
  "The sealed Q9 carrier from a record's DECISION and RUN-ID."
  [{:keys [decision run-id]}]
  (let [cert (:selection-certificate decision)
        compared (vec (sort-by pr-str (map prefix-admission/candidate-key
                                           (keys (get-in decision [:selection-law :posterior])))))
        selected (some-> (get-in decision [:selection-law :per-policy-argmax :action])
                         prefix-admission/candidate-key)
        rows (into {} (map (fn [r] [(prefix-admission/candidate-key (:id r)) r])) (:candidates cert))
        receipts (group-by :policy-id (:preference-receipts cert))
        per (mapv (fn [k]
                    (let [rs (get receipts (nth k 2 nil))]
                      (if (< 1 (count rs))
                        {:refusal :competing-receipts}
                        (policy-c k (first rs) (get rows k)))))
                  compared)
        base {:schema schema
              :run/id run-id
              :route (:selection-route cert)
              :selected selected
              :compared compared
              ;; the preference and precision authority the decision declares
              :declared {:preference (:preference cert)
                         :precision-identity (:reading-precision-identity cert)}
              :bindings {:posterior (digest compared)
                         :receipts (digest (:preference-receipts cert))
                         :candidates (digest (:candidates cert))}}
        refusal (some :refusal per)
        not-recomputable #(assoc base :status :not-recomputable :reason %)]
    (seal
     (cond
       (not= :wm/reading-policy-family-v1 (:selection-route cert))
       (not-recomputable :not-the-reading-family-route)
       (self-asserted decision)
       (assoc (not-recomputable :self-asserted-factorization-carrier)
              :at (vec (self-asserted decision)))
       (empty? compared) (not-recomputable :no-compared-policy)
       (some nil? compared) (not-recomputable :compared-identity-not-structural)
       (not (some #{selected} compared)) (not-recomputable :selected-policy-not-compared)
       refusal (not-recomputable refusal)
       :else
       (let [reasons (negative-reasons per)]
         (if (some #{:no-relevance-coordinate} reasons)
           (assoc base :status :factorization-absent
                  :policies per
                  :registries {:calibration :none-loaded :factorization :none-loaded}
                  :reasons reasons)
           ;; a C with more than count-and-completion coordinates may carry
           ;; relevance; only a registry could say whether it is authorized
           (not-recomputable :relevance-coordinate-not-excluded)))))))

(defn record-inputs
  "The certificate's inputs, read from a persisted RECORD alone."
  [record]
  {:decision (:decision record) :run-id (:run/id record)})

(defn verify-record
  "The record's own :q9 carrier when it is intact, negative and equal to the
  carrier recomputed from the record; else a typed not-recomputable reason."
  [record]
  (let [q9 (:q9 record)]
    (cond
      (nil? q9) {:status :not-recomputable :reason :q9-carrier-absent}
      (not (and (= schema (:schema q9)) (intact? q9)))
      {:status :not-recomputable :reason :q9-carrier-tampered}
      (not (#{:factorization-absent :not-recomputable} (:status q9)))
      {:status :not-recomputable :reason :q9-positive-carrier-refused}
      (not= q9 (certificate (record-inputs record)))
      {:status :not-recomputable :reason :q9-carrier-not-bound-to-record}
      (not= :factorization-absent (:status q9))
      {:status :not-recomputable :reason (:reason q9)}
      :else {:status :verified :carrier q9})))
