(ns futon2.aif.exact-belief-adapter-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.exact-belief-adapter :as adapter]))

(def states [#{} #{:judgement}])
(def uniform {#{} 1/2 #{:judgement} 1/2})
(def point {#{} 0 #{:judgement} 1})
(defn identity-b [s] {s 1})
(defn mixing-b [_] uniform)
(def noisy [{:weight 1/2 :rates {:judgement {:false-neg 0 :false-pos 0}}}
            {:weight 1/2 :rates {:judgement {:false-neg 1/2 :false-pos 1/2}}}])
(def perfect (mapv #(assoc % :rates {:judgement {:false-neg 0 :false-pos 0}}) noisy))
(def uninformative
  (mapv #(assoc % :rates {:judgement {:false-neg 1/2 :false-pos 1/2}}) noisy))

(defn controls []
  {:noisy (adapter/synthetic-mixture-update states noisy identity-b #{:judgement} uniform)
   :impossible (adapter/synthetic-mixture-update states perfect identity-b #{} point)
   :perfect (adapter/synthetic-mixture-update states perfect identity-b #{:judgement} uniform)
   :point (adapter/synthetic-mixture-update states noisy identity-b #{} point)
   :spread (adapter/synthetic-mixture-update states noisy mixing-b #{:judgement} point)
   :uninformative (adapter/synthetic-mixture-update states uninformative identity-b #{} uniform)})

(deftest distribution-movement-and-iff
  (let [{:keys [noisy impossible]} (controls)]
    (is (= :ok (:status noisy)))
    (is (= :some (:option noisy)))
    (is (= uniform (:predicted-state noisy)))
    (is (= 1 (reduce + (vals (:predicted-state noisy)))))
    (is (= 1 (reduce + (vals (:posterior noisy)))))
    (is (every? #(<= 0 %) (vals (:posterior noisy))))
    (is (= {#{} 1/4 #{:judgement} 3/4} (:posterior noisy)))
    (is (not= (:predicted-state noisy) (:posterior noisy)))
    (is (= 1/2 (:observation-probability noisy)))
    (is (= :refused (:status impossible)))
    (is (= :none (:option impossible)))
    (is (= :impossible-observation (:kind impossible)))
    (is (zero? (:observation-probability impossible)))
    (is (not (contains? impossible :posterior))))
  (doseq [components [perfect noisy] prior [uniform point] observation states]
    (let [r (adapter/synthetic-mixture-update states components identity-b observation prior)]
      (is (= (zero? (:observation-probability r)) (= :none (:option r)))))))

(deftest perfect-observer-and-qualified-vacuity
  (let [{:keys [perfect point spread uninformative]} (controls)
        predicted (:predicted-state perfect)
        observation-dist (reduce (fn [acc [s mass]]
                                   (merge-with + acc
                                               (update-vals (get-in perfect [:observation-rows s])
                                                            #(* mass %)))) {} predicted)]
    (testing "predictedOutcome_eq_rolloutState is predictive, not posterior equality"
      (is (= predicted observation-dist))
      (is (= {#{} 0 #{:judgement} 1} (:posterior perfect))))
    (testing "exactUpdate_pointMass_vacuous: even a misleading possible report cannot move known state"
      (is (= 1/4 (:observation-probability point)))
      (is (= (:predicted-state point) (:posterior point))))
    (testing "a point-mass incoming prior may spread under B"
      (is (= uniform (:predicted-state spread)))
      (is (= {#{} 1/4 #{:judgement} 3/4} (:posterior spread)))
      (is (not= (:prior spread) (:posterior spread))))
    (testing "exactUpdate_vacuous_of_const_likelihood"
      (is (= uniform (:posterior uninformative))))))

(deftest synthetic-declaration-on-every-outcome
  (doseq [r (vals (controls))]
    (is (= {:status :declared :parameter-basis :synthetic :z-semantics :per-step-redraw
            :calibration-authority :none}
           (dissoc (:model r) :components)))
    (is (= 2 (count (get-in r [:model :components]))))))

(deftest invalid-domain-is-not-impossible-observation
  (doseq [[components B prior kind]
          [[noisy identity-b {#{} 1/3} :invalid-prior]
           [(assoc-in noisy [0 :weight] 1/3) identity-b uniform :invalid-observation-kernel]
           [noisy (fn [_] {#{} -1 #{:judgement} 2}) uniform :invalid-transition-kernel]
           [noisy (fn [_] {#{:outside} 1}) uniform :invalid-transition-kernel]
           [noisy identity-b {#{} 0.5 #{:judgement} 0.5} :invalid-prior]]]
    (let [r (adapter/synthetic-mixture-update states components B #{} prior)]
      (is (= :invalid (:status r)))
      (is (= kind (:kind r)))
      (is (not (contains? r :option)))
      (is (= :per-step-redraw (get-in r [:model :z-semantics]))))))

(deftest asymmetric-transition-orientation
  (let [B (fn [s] (if (empty? s) {#{} 3/4 #{:judgement} 1/4} {#{:judgement} 1}))
        r (adapter/synthetic-mixture-update states uninformative B #{} uniform)]
    (is (= {#{} 3/8 #{:judgement} 5/8} (:predicted-state r)))
    (is (= (:predicted-state r) (:posterior r)))))

(deftest posterior-attains-lean-vfe-bound-on-fixture
  ;; Test-only evaluation of PolicyVariationalFreeEnergy.variationalFreeEnergy,
  ;; lines 43-46. No change to the F consumer; finite comparisons are not a proof.
  (let [r (:noisy (controls))
        joint (into {} (for [s states]
                         [s (* (get-in r [:predicted-state s])
                               (get-in r [:observation-rows s (:observation r)] 0))]))
        vfe (fn [q]
              (reduce + 0.0 (for [[s p] q :when (pos? p)]
                              (* (double p) (- (Math/log (double p))
                                               (Math/log (double (get joint s))))))))
        optimum (vfe (:posterior r))]
    (is (< (abs (- optimum (- (Math/log (double (:observation-probability r)))))) 1e-12))
    (is (> (vfe uniform) optimum))
    (doseq [i (range 21)]
      (is (<= optimum (+ 1e-12 (vfe {#{} (/ i 20) #{:judgement} (- 1 (/ i 20))})))))))
