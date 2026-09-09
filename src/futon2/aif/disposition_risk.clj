(ns futon2.aif.disposition-risk
  "Project canonical predicted observations through a disposition kernel and
   score the resulting distribution against the ruled outcome preference."
  (:require [clojure.set :as set]))

(def producer-contract :disposition-risk/v1)

(defn seeded-c-record
  "Classify the ruled preference input without supplying a default. A caller
   that enables disposition risk must provide the seed it intends to score
   against; absence is data, not permission to invent preference masses."
  [opts required?]
  (let [present? (contains? opts :seeded-c)
        seeded-c (:seeded-c opts)]
    (if (some? seeded-c)
      {:producer-contract producer-contract
       :status :present
       :field :seeded-c
       :value seeded-c}
      {:producer-contract producer-contract
       :status :absent
       :reason :seeded-c-not-supplied
       :required? (boolean required?)
       :absent [{:field :seeded-c :key-present? present?}]})))

(defn seeded-c-events [record]
  (if (= :present (:status record)) [] [record]))

(defn- refuse! [reason data]
  (throw (ex-info (name reason) (assoc data :reason reason :refused? true))))

(defn- require-support! [label support mass]
  (let [support (set support)
        actual (set (keys mass))
        missing (sort (set/difference support actual))
        outside (sort (set/difference actual support))]
    (when (seq missing)
      (refuse! :missing-disposition-support
               {:distribution label :missing (vec missing)}))
    (when (seq outside)
      (refuse! :outcome-outside-disposition-support
               {:distribution label :outside (vec outside)}))))

(defn- require-distribution! [label support mass]
  (require-support! label support mass)
  (when-let [[kind value]
             (first (filter (fn [[_ value]]
                              (or (not (number? value))
                                  (neg? (double value))
                                  (not (Double/isFinite (double value)))))
                            mass))]
    (refuse! :invalid-disposition-mass
             {:distribution label :disposition kind :value value}))
  (let [total (reduce + 0.0 (map double (vals mass)))]
    (when (> (Math/abs (- total 1.0)) 1.0e-12)
      (refuse! :disposition-mass-not-normalised
               {:distribution label :total total}))))

(defn constant-checkpoint-kernel
  "Explicit constant adapter for checks/disposition_kernel.clj's fitted
   :wm/disposition-kernel-v1 checkpoint-trajectory artifact (Item 24,
   RULINGS-walkthrough-2026-09-09.md). Accept only nonempty valid support and
   sampled rows that all give the SAME distribution. Preserve every named zero;
   never average, smooth, or select one conditioning row.

   The returned function accepts predicted channel observations but ignores them.
   For the recorded grounded-change-only cohort and ruled_outcome_c.clj's seed,
   disposition-risk yields ln 2. This is NOT a fitted channel observation model:
   holes/E-C-realization.md §1b remains open. Metadata retains the source,
   checkpoint conditioning, support and explicit constant-adapter limitation."
  [artifact]
  (let [{:keys [support states source conditioning]} artifact]
    (when-not (and (= :wm/disposition-kernel-v1 (:schema artifact))
                   (= :checkpoint-trajectory (:grain conditioning))
                   (map? source) (string? (:ledger source))
                   (string? (:sha256 source))
                   (vector? support) (seq support)
                   (every? keyword? support)
                   (= (count support) (count (set support)))
                   (vector? states) (seq states))
      (refuse! :invalid-constant-kernel-artifact
               {:field :disposition-kernel :schema (:schema artifact)}))
    (doseq [row states]
      (when-not (and (map? row) (map? (:probability row))
                     (integer? (:sample-size row)) (pos? (:sample-size row)))
        (refuse! :invalid-constant-kernel-row {:row row}))
      (require-distribution! :checkpoint-kernel support (:probability row)))
    (let [distributions (distinct (map :probability states))]
      (when-not (= 1 (count distributions))
        (refuse! :nonconstant-checkpoint-kernel
                 {:distinct-distributions (count distributions)
                  :reason-detail :observation-model-required}))
      (let [mass (first distributions)]
        (with-meta (fn [_predicted-channel-observations] mass)
          {:adapter :constant-checkpoint-kernel/v1
           :source source :conditioning conditioning :support support
           :sample-size (reduce + (map :sample-size states))
           :ignores-channel-observations? true
           :observation-model-bridge :open})))))

(defn predict-dispositions
  "Apply P(d|o) to the canonical observation prediction for one policy.

   KERNEL is a function from the forward model's predicted observation map to
   a complete disposition-mass map. Keeping this seam explicit prevents a
   checkpoint-trajectory kernel from being treated as a channel model."
  [predicted-observation kernel support]
  (when-not (ifn? kernel)
    (refuse! :disposition-kernel-not-supplied {:field :disposition-kernel}))
  (let [mass (kernel predicted-observation)]
    (when-not (map? mass)
      (refuse! :disposition-kernel-returned-no-distribution {:value mass}))
    (require-distribution! :predicted support mass)
    mass))

(defn disposition-risk
  "Return KL[Q(d|pi) || C]. Zero Q mass contributes zero. Positive Q mass at
   a ruled zero is refused instead of being smoothed or made infinite."
  [predicted-observation kernel {:keys [support mass]}]
  (when-not (and (set? support) (map? mass))
    (refuse! :seeded-c-not-supplied {:field :seeded-c}))
  (require-distribution! :preference support mass)
  (let [q (predict-dispositions predicted-observation kernel support)]
    (reduce-kv
     (fn [total kind qd]
       (let [qd (double qd)
             cd (double (get mass kind))]
         (cond
           (zero? qd) total
           (zero? cd) (refuse! :positive-prediction-at-named-zero
                               {:disposition kind :predicted-mass qd})
           :else (+ total (* qd (Math/log (/ qd cd)))))))
     0.0 q)))
