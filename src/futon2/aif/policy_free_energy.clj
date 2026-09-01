(ns futon2.aif.policy-free-energy
  "Horizon-one observed-data free energy for candidate-policy predictions."
  (:require [clojure.set :as set]))

(defn- fail!
  [message data]
  (throw (ex-info message data)))

(defn- numeric-channel-map!
  [label x]
  (when-not (map? x)
    (fail! (str label " must be a map")
           {:error :invalid-channel-map :field label :value x}))
  (doseq [[channel value] x]
    (when-not (number? value)
      (fail! (str label " contains a non-numeric channel value")
             {:error :non-numeric-channel
              :field label
              :channel channel
              :value value})))
  x)

(defn- require-same-channels!
  [mean variance observation]
  (let [mean-channels (set (keys mean))
        variance-channels (set (keys variance))
        observation-channels (set (keys observation))]
    (when-not (and (= mean-channels variance-channels)
                   (= mean-channels observation-channels))
      (fail! "prediction mean, prediction variance, and observation must have identical channels"
             {:error :channel-mismatch
              :prediction-mean-only
              (vec (sort-by pr-str (set/difference mean-channels observation-channels)))
              :observation-only
              (vec (sort-by pr-str (set/difference observation-channels mean-channels)))
              :variance-only
              (vec (sort-by pr-str (set/difference variance-channels mean-channels)))
              :mean-without-variance
              (vec (sort-by pr-str (set/difference mean-channels variance-channels)))}))))

(defn f-pi-for-candidate
  "Return horizon-one Gaussian observed-data free energy for one candidate.

   `prediction` must contain numeric channel maps under `:prediction-mean` and
   `:prediction-variance`; `observation` is a numeric map over exactly the same
   channels. Missing or extra channels are rejected with `ExceptionInfo` rather
   than skipped or filled with zero.

   For every positive variance v, the contribution is
   1/2 * (log(2*pi*v) + (observation-mean)^2/v). Contributions are summed, so
   lower F_pi means a better fit. This is the sign used when B.9 subtracts F_pi.

   A zero variance denotes a deterministic channel, not a Gaussian with a
   representable finite density. If its residual is within
   `:deterministic-tolerance` (default 0.0), its contribution is defined as 0.0.
   Otherwise the candidate is rejected with `ExceptionInfo`; the function never
   returns Infinity or NaN. Negative variance is always rejected.

   BUT NOT EVERY ZERO MEANS THE SAME THING, and the difference decides whether
   this function can be called on WM data at all. `forward-model/predict`
   gives every channel the action model does not touch a variance of 0.0 and
   marks it `:variance-status {:status :absent :reason
   :deterministic-by-action-model}` — that is *no prediction*, not a claim to
   predict the channel exactly. On a real tick 12 of 14 channels are of this
   kind, and 7 of 14 channels move between ticks, so scoring a tick-t
   prediction against the tick-(t+1) observation rejects on the first such
   channel to move (measured: `:mathematics-pct`, residual 0.001).

   `:absent-variance` therefore selects what happens to a channel the action
   model declared absent:
     :reject (DEFAULT)  as above — a zero is a deterministic claim. Correct
                        when the caller has verified the zeros are real.
     :floor             use `:variance-floor` (default 0.01, the same floor
                        `futon2.aif.precision` applies to its own variance)
                        for absent channels only. A genuinely declared zero —
                        one carrying no `:absent` status — still rejects.
   There is no default that silently turns a rejection into a number: choosing
   `:floor` is a caller's declaration that the absent channels carry no
   prediction, and it is recorded in the trace by `:effects-mode` alongside.

   This is only the horizon-one term for the WM's single-action candidate grain;
   it does not claim to compute the tau>=2 trajectory sum in Parr 2022 B.4."
  ([prediction observation]
   (f-pi-for-candidate prediction observation {}))
  ([prediction observation {:keys [deterministic-tolerance absent-variance
                                   variance-floor]
                            :or {deterministic-tolerance 0.0
                                 absent-variance :reject
                                 variance-floor 0.01}}]
   (when (neg? (double deterministic-tolerance))
     (fail! ":deterministic-tolerance must be non-negative"
            {:error :invalid-deterministic-tolerance
             :value deterministic-tolerance}))
   (when-not (#{:reject :floor} absent-variance)
     (fail! ":absent-variance must be :reject or :floor"
            {:error :invalid-absent-variance :value absent-variance}))
   (when-not (pos? (double variance-floor))
     (fail! ":variance-floor must be positive"
            {:error :invalid-variance-floor :value variance-floor}))
   (let [mean (numeric-channel-map! :prediction-mean (:prediction-mean prediction))
         variance (numeric-channel-map! :prediction-variance
                                        (:prediction-variance prediction))
         observation (numeric-channel-map! :observation observation)]
     (require-same-channels! mean variance observation)
     (reduce-kv
      (fn [total channel predicted]
        (let [observed (double (get observation channel))
              predicted (double predicted)
              raw-v (double (get variance channel))
              ;; a zero the action model declared absent carries no prediction;
              ;; a zero with no such status is a real deterministic claim
              absent? (= :absent
                         (get-in prediction
                                 [:variance-status channel :status]))
              v (if (and (zero? raw-v) absent? (= :floor absent-variance))
                  (double variance-floor)
                  raw-v)
              residual (- observed predicted)]
          (cond
            (neg? v)
            (fail! "prediction variance must be non-negative"
                   {:error :invalid-variance :channel channel :variance v})

            (zero? v)
            (if (<= (Math/abs residual) (double deterministic-tolerance))
              total
              (fail! "observation violates a deterministic zero-variance prediction"
                     {:error :deterministic-mismatch
                      :channel channel
                      :prediction predicted
                      :observation observed
                      :residual residual
                      :variance-status
                      (get-in prediction [:variance-status channel])
                      :hint (if absent?
                              "this channel's variance was declared :absent by the action model; :absent-variance :floor is the option for it"
                              "this zero variance carries no :absent status, so it is a real deterministic claim")}))

            :else
            (+ total (* 0.5
                        (+ (Math/log (* 2.0 Math/PI v))
                           (/ (* residual residual) v)))))))
      0.0
      mean))))

(defn f-pi-vector
  "Return `f-pi-for-candidate` values aligned with candidate input order."
  ([candidates observation]
   (f-pi-vector candidates observation {}))
  ([candidates observation opts]
   (mapv #(f-pi-for-candidate % observation opts) candidates)))
