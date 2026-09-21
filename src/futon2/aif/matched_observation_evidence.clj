(ns futon2.aif.matched-observation-evidence
  "Standalone synthetic MatchedObservationEvidence adapter. No live wiring.
   Exact rational prediction, with a double approximation only at Real.log."
  (:require [clojure.set :as set]
            [futon2.aif.cascade-model-manifest :as m]))

(defn- distribution? [q]
  (and (map? q) (seq q)
       (every? #(and (or (integer? %) (ratio? %)) (<= 0 %)) (vals q))
       (= 1 (reduce +' 0 (vals q)))))

(defn- log-integer [n]
  (let [n (biginteger n)
        shift (max 0 (- (.bitLength n) 53))]
    (+ (Math/log (.doubleValue (.shiftRight n shift)))
       (* shift (Math/log 2.0)))))

(defn surprisal
  "Negative log of a positive exact probability, without double underflow."
  [p]
  (let [d (double p)]
    (if (pos? d)
      (- (Math/log d))
      ;; Positive exact mass must not become a contradiction through underflow.
      (- (log-integer (denominator p)) (log-integer (numerator p))))))

(defn synthetic-matched-evidence
  "Roll out q0 once to n, form Q(o)=sum_s A(s,o)q_n(s) once, then match.
   observation is {:option :none} or {:option :some :observation <token set>}.
   :constructor is :missing, :contradiction or :value on valid model inputs;
   :status :invalid is outside those mathematical constructors. The exact
   distribution consumed by the match is retained as :predicted-outcome."
  [components q0 precedence-fn n observation]
  (let [declaration {:status :declared :parameter-basis :synthetic
                     :calibration-authority :none :z-semantics :per-step-redraw
                     :components components}
        receipt {:schema :wm/matched-observation-evidence-v1 :model declaration
                 :step n :received observation :prior q0}
        invalid (fn [kind detail] (assoc receipt :status :invalid :kind kind :detail detail))
        probe (m/mixture-observation-distribution components #{})
        universe (when-not (contains? probe :status)
                   (set (keys (:rates (first components)))))]
    (cond
      (contains? probe :status) (invalid :invalid-observation-model probe)
      (not (and (integer? n) (<= 0 n) (ifn? precedence-fn)))
      (invalid :invalid-rollout-input {:step n})
      (not (and (distribution? q0)
                (every? #(and (set? %) (set/subset? % universe)) (keys q0))))
      (invalid :invalid-prior q0)
      (not (and (map? observation)
                (or (= observation {:option :none})
                    (and (= :some (:option observation))
                         (= #{:option :observation} (set (keys observation)))
                         (set? (:observation observation))
                         (set/subset? (:observation observation) universe)))))
      (invalid :invalid-received-observation observation)
      :else
      (let [rollout (m/rollout-evaluation precedence-fn q0 n)
            q (:belief rollout)]
        (if-not (and (distribution? q)
                     (every? #(and (set? %) (set/subset? % universe)) (keys q)))
          (invalid :invalid-rollout-belief rollout)
          (let [rows (into {} (map (fn [s] [s (m/mixture-observation-distribution components s)]) (keys q)))
                prediction (reduce-kv
                            (fn [acc s mass]
                              (merge-with +' acc (update-vals (get rows s) #(*' mass %)))) {} q)
                result (assoc receipt :rollout rollout :observation-rows rows :predicted-outcome prediction)]
            (if (= :none (:option observation))
              (assoc result :status :ok :constructor :missing)
              (let [o (:observation observation) p (get prediction o 0)]
                (if (zero? p)
                  (assoc result :status :refused :constructor :contradiction
                         :observation o :probability p)
                  (assoc result :status :ok :constructor :value
                         :observation o :probability p :value (surprisal p)))))))))))
