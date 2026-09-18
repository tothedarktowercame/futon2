;; Exact evaluation cost of the token observation model at forty-five tokens.
;; Run from /home/joe/code/futon2 with: clojure -M holes/labs/wm-contract/runs/factorized-eval-timing-2026-09-18/timing.clj
;;
;; Licence for the closed forms: mathlib4 DarkTower/AIF/ProductFactorization.lean
;; (sum_negMulLog_prodB, sum_klTerm_prodB) — the entropy of a row of the
;; per-token-independent kernel, and its KL against a product reference,
;; each equal a sum of one binary term per token. This script times those
;; sums at n=45 and cross-checks them against full powerset enumeration at
;; n=15 (32768 observations), where enumeration is still feasible.
(require '[futon2.aif.cascade-model-manifest :as m])

(defn rates-for [tokens fn- fp] (into {} (map (fn [t] [t {:false-neg fn- :false-pos fp}])) tokens))
(defn h-bern [p] (let [p (double p) q (- 1.0 p)]
                   (- (+ (if (pos? p) (* p (Math/log p)) 0.0)
                         (if (pos? q) (* q (Math/log q)) 0.0)))))
(defn kl-bern [p c] (let [p (double p) c (double c)]
                      (+ (if (pos? p) (* p (Math/log (/ p c))) 0.0)
                         (if (pos? (- 1 p)) (* (- 1 p) (Math/log (/ (- 1 p) (- 1 c)))) 0.0))))

;; -- cross-check at n=15: closed forms vs full enumeration --------------
(let [n 15
      u (mapv #(str "tok" %) (range n))
      s (set (take 7 u))
      rates (rates-for u 1/10 1/100)
      p-of (fn [v] (if (contains? s v) (- 1 1/10) 1/100))
      c-v (into {} (map (fn [v] [v (if (contains? s v) 3/4 1/20)])) u)
      d (m/observation-distribution rates s)
      H-enum (- (reduce + (map (fn [p] (let [p (double p)] (if (pos? p) (* p (Math/log p)) 0.0))) (vals d))))
      logC (fn [o] (reduce + (map (fn [v] (Math/log (double (if (contains? o v) (c-v v) (- 1 (c-v v)))))) u)))
      KL-enum (reduce + (map (fn [[o p]] (let [p (double p)] (if (pos? p) (* p (- (Math/log p) (logC o))) 0.0))) d))
      H-fact (reduce + (map (fn [v] (h-bern (p-of v))) u))
      KL-fact (reduce + (map (fn [v] (kl-bern (p-of v) (c-v v))) u))]
  (println (format "n=15 cross-check over %d enumerated observations:" (count d)))
  (println (format "  entropy: enumerated %.12f  factorized %.12f" H-enum H-fact))
  (println (format "  KL:      enumerated %.12f  factorized %.12f" KL-enum KL-fact)))

;; -- timing at n=45 ------------------------------------------------------
(let [n 45
      u (mapv #(str "tok" %) (range n))
      s (set (take 20 u))
      p-of (fn [v] (if (contains? s v) 0.9 0.01))
      c-v (into {} (map (fn [v] [v (if (contains? s v) 0.75 0.05)])) u)
      ambiguity (fn [] (reduce + (map (fn [v] (h-bern (p-of v))) u)))
      risk (fn [] (reduce + (map (fn [v] (kl-bern (p-of v) (c-v v))) u)))
      reps 10000]
  ;; warm-up so the JIT-compiled cost is what gets reported
  (dotimes [_ 2000] (ambiguity) (risk))
  (let [t0 (System/nanoTime)
        _ (dotimes [_ reps] (ambiguity) (risk))
        t1 (System/nanoTime)]
    (println (format "n=45: entropy %.4f nats, KL %.4f nats" (ambiguity) (risk)))
    (println (format "n=45: %.4f ms per exact (entropy+KL) evaluation, mean of %d reps"
                     (/ (- t1 t0) 1e6 reps) reps))
    (println (format "n=45: enumeration would visit 2^45 = %.3e observations; not attempted"
                     (Math/pow 2 45)))))
