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

   This is only the horizon-one term for the WM's single-action candidate grain;
   it does not claim to compute the tau>=2 trajectory sum in Parr 2022 B.4."
  ([prediction observation]
   (f-pi-for-candidate prediction observation {}))
  ([prediction observation {:keys [deterministic-tolerance]
                            :or {deterministic-tolerance 0.0}}]
   (when (neg? (double deterministic-tolerance))
     (fail! ":deterministic-tolerance must be non-negative"
            {:error :invalid-deterministic-tolerance
             :value deterministic-tolerance}))
   (let [mean (numeric-channel-map! :prediction-mean (:prediction-mean prediction))
         variance (numeric-channel-map! :prediction-variance
                                        (:prediction-variance prediction))
         observation (numeric-channel-map! :observation observation)]
     (require-same-channels! mean variance observation)
     (reduce-kv
      (fn [total channel predicted]
        (let [observed (double (get observation channel))
              predicted (double predicted)
              v (double (get variance channel))
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
                      :residual residual}))

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
