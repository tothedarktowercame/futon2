(ns futon2.aif.likelihood-precision-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.set :as cset]
            [futon2.aif.likelihood-precision :as lp]
            [futon2.aif.cascade-model-manifest :as m]))

(deftest temper-row-zeta-one-is-identity
  (let [row {:s0 0.5 :s1 0.25 :s2 0.25}
        out (lp/temper-row row 1)]
    (doseq [[k v] row]
      (is (< (Math/abs (- v (get out k))) 1e-12)))))

(deftest temper-row-sharpens-and-flattens-by-ratio
  ;; p^ζ/Z preserves ratios as (p_i/p_j)^ζ — the defining Gibbs property.
  (let [row {:a 0.6 :b 0.3 :c 0.1}
        sharp (lp/temper-row row 2)
        flat (lp/temper-row row 0.5)]
    (is (< (Math/abs (- 4.0 (/ (:a sharp) (:b sharp)))) 1e-9)) ; (0.6/0.3)^2
    (is (< (Math/abs (- 9.0 (/ (:b sharp) (:c sharp)))) 1e-9)) ; (0.3/0.1)^2
    (is (< (Math/abs (- (Math/sqrt 2.0) (/ (:a flat) (:b flat)))) 1e-9))
    (is (< (Math/abs (- 1.0 (reduce + (map val sharp)))) 1e-12))
    (is (< (Math/abs (- 1.0 (reduce + (map val flat)))) 1e-12))))

(deftest temper-row-large-zeta-limits-to-argmax
  (let [row {:a 0.6 :b 0.4 :c 0.0}
        out (lp/temper-row row 64)]
    (is (> (:a out) 0.999999))
    (is (< (:b out) 1e-6))
    ;; a zero entry is never resurrected by any ζ
    (is (zero? (:c out)))
    (is (< (Math/abs (- 1.0 (+ (:a out) (:b out)))) 1e-9))))

(deftest temper-row-zeta-zero-is-declared-uninformative
  ;; ζ = 0 is admitted (nonnegative precision, not silently positive), and is
  ;; uniform over the POSITIVE support — zeros stay zero.
  (let [out (lp/temper-row {:a 0.7 :b 0.3 :c 0.0} 0)]
    (is (= {:a 0.5 :b 0.5 :c 0.0} out))))

(deftest temper-row-refusals
  ;; negative precision: typed refusal, not a flipped/quiet row
  (let [r (lp/temper-row {:a 0.5 :b 0.5} -1)]
    (is (lp/refusal? r))
    (is (= :negative-zeta (:kind r))))
  ;; non-stochastic row: typed refusal with the offending sum
  (let [r (lp/temper-row {:a 0.5 :b 0.4} 1)]
    (is (lp/refusal? r))
    (is (= :row-not-stochastic (:kind r))))
  ;; negative entry
  (let [r (lp/temper-row {:a 1.5 :b -0.5} 1)]
    (is (lp/refusal? r))
    (is (= :row-not-stochastic (:kind r))))
  ;; non-number zeta
  (let [r (lp/temper-row {:a 0.5 :b 0.5} :one)]
    (is (lp/refusal? r))
    (is (= :invalid-zeta (:kind r)))))

(deftest temper-a-tempers-every-row-and-propagates-refusal
  (let [a {"o1" {:s0 0.5 :s1 0.5}
           "o2" {:s0 0.9 :s1 0.1}}
        out (lp/temper-a a 2)]
    (is (= #{"o1" "o2"} (set (keys out))))
    (is (= {:s0 0.5 :s1 0.5} (get out "o1")))
    (is (< (Math/abs (- (/ (* 0.9 0.9) (+ (* 0.9 0.9) (* 0.1 0.1)))
                        (:s0 (get out "o2"))))
           1e-12))
    (let [bad (lp/temper-a {"o1" {:s0 0.3 :s1 0.3} "o2" {:s0 0.5 :s1 0.5}} 1)]
      (is (lp/refusal? bad))
      (is (= "o1" (:observation bad))))))

;; ---------------------------------------------------------------------------
;; R7-3/R7-4 consumer control: the live scoring consumer IGNORES ζ today.
;; This is the rejecting control the cascade asks for — it demonstrates the
;; named gap (no applicable consumer), not successful modulation.
;; ---------------------------------------------------------------------------

(defn- live-tick-spec []
  (m/preference-spec {:want #{"t0" "t1"} :evidence #{"e0"}
                      :lam 2 :mu 1/4 :zeroed #{#{:bad}}}))

(deftest live-tick-consumer-ignores-zeta
  (let [spec (live-tick-spec)
        rates (into {} (map (fn [t] [t {:false-neg 0 :false-pos 0}]))
                    (cset/union (:want spec) (:evidence spec)))
        fire {:id :fire :guard {:status :interpreted :operator :and
                                :clauses [{:status :interpreted :present #{}
                                           :absent #{}}]}
              :transition {:status :interpreted :operator :union
                           :produces #{"t0"}}
              :produces #{"t0"}}
        base {:rates rates :q0 {#{} 1}
              :precedence-fn (constantly [fire])
              :horizon 2 :spec spec}
        g-base (m/horizon-g-sparse base)
        g-half (m/horizon-g-sparse (assoc base :zeta 0.5))
        g-seven (m/horizon-g-sparse (assoc base :zeta 7.0))]
    (is (number? g-base))
    ;; ζ in the opts is silently destructured away: the score cannot be
    ;; modulated by likelihood precision on this path. That is the gap R7
    ;; records, evidenced here rather than assumed.
    (is (= g-base g-half))
    (is (= g-base g-seven))))

(deftest zeta-declaration-names-the-implicit-state
  (let [d lp/zeta-declaration]
    (is (= :R7 (:item d)))
    (is (= :likelihood-precision-zeta (:quantity d)))
    (is (= :zero-adjudication-identity (get-in d [:live-tick :rates])))
    (is (= :implicit-and-vacuous (get-in d [:live-tick :status])))
    (is (= :fixed-vacuous (get-in d [:live-tick :declared])))
    ;; no prior/update law is claimed for ζ
    (is (= :none-declared (get-in d [:prior :law])))
    (is (= :none (:update d)))
    ;; distinctness from R14 policy precision and from the legacy κ exponent
    (is (= :R14 (get-in d [:distinct-from :policy-precision-gamma :item])))
    (is (contains? (:distinct-from d) :legacy-weight-exponent-kappa))
    ;; the gaps are named, not papered over
    (is (= 3 (count (:gaps d))))))
