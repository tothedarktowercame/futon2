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
