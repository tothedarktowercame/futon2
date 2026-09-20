(ns futon2.aif.matched-observation-evidence-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.matched-observation-evidence :as f]
            [futon2.aif.cascade-model-manifest :as m]))

(def components [{:weight 1/2 :rates {:x {:false-neg 0 :false-pos 0}
                                        :y {:false-neg 0 :false-pos 0}}}
                 {:weight 1/2 :rates {:x {:false-neg 1/2 :false-pos 0}
                                      :y {:false-neg 0 :false-pos 0}}}])
(def pattern {:id :produce-x :theta 1
              :guard {:status :interpreted :operator :and
                      :clauses [{:status :interpreted :present #{} :absent #{}}]}
              :transition {:status :interpreted :operator :union :produces #{:x}}
              :produces #{:x}})
(defn run [o] (f/synthetic-matched-evidence components {#{} 1} (constantly [pattern]) 1 o))
(defn declared? [r]
  (= {:parameter-basis :synthetic :calibration-authority :none :z-semantics :per-step-redraw}
     (select-keys (:model r) [:parameter-basis :calibration-authority :z-semantics])))

(deftest all-constructors-and-both-directions
  (let [missing (run {:option :none})]
    (is (= :missing (:constructor missing)))
    (is (not (contains? missing :value)))
    (is (declared? missing))
    (println "F-PROBE missing" (pr-str missing)))
  (doseq [o [#{} #{:x} #{:y} #{:x :y}]]
    (let [r (run {:option :some :observation o})
          p (get (:predicted-outcome r) o 0)]
      (is (= {#{:x} 1} (get-in r [:rollout :belief])))
      (is (= {#{} 1/4 #{:x} 3/4} (:predicted-outcome r)))
      (is (= (zero? p) (= :contradiction (:constructor r))))
      (is (= (pos? p) (= :value (:constructor r))))
      (is (= o (:observation r)))
      (is (declared? r))
      (if (pos? p)
        (do (is (= (- (Math/log (double p))) (:value r)))
            (is (<= 0 (:value r))))
        (do (is (= :refused (:status r)))
            (is (not (contains? r :value)))))
      (println "F-PROBE received" (pr-str r)))))

(deftest one-prediction-not-a-second-rollout
  (let [original m/rollout-evaluation calls (atom 0)]
    (with-redefs [m/rollout-evaluation (fn [& args] (swap! calls inc) (apply original args))]
      (let [r (run {:option :some :observation #{:x}})]
        (is (= 1 @calls))
        (is (= (get (:predicted-outcome r) #{:x}) (:probability r)))))))

(deftest invalid-is-not-missing-and-certainty-is-a-value
  (doseq [r [(run nil)
             (f/synthetic-matched-evidence [] {#{} 1} (constantly []) 0 {:option :none})
             (f/synthetic-matched-evidence components {#{} 0.5} (constantly []) 0 {:option :none})]]
    (is (= :invalid (:status r)))
    (is (not (contains? r :constructor)))
    (is (declared? r)))
  (let [r (f/synthetic-matched-evidence components {#{} 1} (constantly []) 0
                                       {:option :some :observation #{}})]
    (is (= :value (:constructor r)))
    (is (zero? (:value r)))))

(deftest exact-positive-mass-survives-double-underflow
  (let [p (/ 1 (bigint (.shiftLeft java.math.BigInteger/ONE 2000)))
        r (f/synthetic-matched-evidence
           [{:weight 1 :rates {:x {:false-neg 0 :false-pos p}}}]
           {#{} 1} (constantly []) 0 {:option :some :observation #{:x}})]
    (is (= :value (:constructor r)))
    (is (= p (:probability r)))
    (is (Double/isFinite (:value r)))
    (is (< (Math/abs (- (:value r) (* 2000 (Math/log 2.0)))) 1e-10))))
