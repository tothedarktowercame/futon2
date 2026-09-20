(require '[futon2.aif.cascade-model-manifest :as m])
(defn rates-for [tokens fn- fp] (into {} (map (fn [t] [t {:false-neg fn- :false-pos fp}])) tokens))
(def u (mapv #(str "tok" %) (range 10)))
(def s (set (take 5 u)))
;; Independent judge: miss 1/10, hallucinate 1/100, all tokens independent.
(def indep (rates-for u 1/10 1/100))
;; Declared experimental configuration, not an empirical estimate:
;; coupled judge with 15% weight on a "bad day" (miss 1/2);
;; good-day miss 1/34 chosen so the MARGINAL miss rate is exactly 1/10:
;; 17/20 * 1/34 + 3/20 * 1/2 = 1/40 + 3/40 = 4/40 = 1/10.
(def d-indep (m/observation-distribution indep s))
(def d-coup (m/mixture-observation-distribution
             [{:weight 17/20 :rates (rates-for u 1/34 1/100)}
              {:weight 3/20 :rates (rates-for u 1/2 1/100)}] s))
(defn seen-marg [d v] (reduce + (map val (filter #(contains? (key %) v) d))))
(println "marginal P(miss tok0) indep =" (- 1 (seen-marg d-indep "tok0"))
         " coupled =" (- 1 (seen-marg d-coup "tok0")))
(defn H [d] (- (reduce + (map (fn [p] (let [p (double p)] (if (pos? p) (* p (Math/log p)) 0.0))) (vals d)))))
;; product C: prefer seeing established tokens (3/4), mildly disprefer others (1/20)
(def c-v (into {} (map (fn [v] [v (if (contains? s v) 3/4 1/20)])) u))
(defn logC [o] (reduce + (map (fn [v] (Math/log (double (if (contains? o v) (c-v v) (- 1 (c-v v)))))) u)))
(defn KL [d] (reduce + (map (fn [[o p]] (let [p (double p)] (if (pos? p) (* p (- (Math/log p) (logC o))) 0.0))) d)))
(defn kl-bern [p c] (let [p (double p) c (double c)]
                      (+ (if (pos? p) (* p (Math/log (/ p c))) 0.0)
                         (if (pos? (- 1 p)) (* (- 1 p) (Math/log (/ (- 1 p) (- 1 c)))) 0.0))))
(let [Hi (H d-indep) Hc (H d-coup)
      marg-H (reduce + (map (fn [v] (let [p (double (seen-marg d-coup v))]
                                      (- (+ (* p (Math/log p)) (* (- 1 p) (Math/log (- 1 p))))))) u))
      TC (- marg-H Hc)
      Ri (KL d-indep) Rc (KL d-coup)
      marg-KL (reduce + (map (fn [v] (kl-bern (seen-marg d-coup v) (c-v v))) u))]
  (println (format "ambiguity H[A(.|s)]:  indep %.4f   coupled %.4f   (sum of marginal H = %.4f)" Hi Hc marg-H))
  (println (format "total correlation TC of coupled row = %.4f nats" TC))
  (println (format "risk KL(Q||C):        indep %.4f   coupled %.4f" Ri Rc))
  (println (format "decomposition check:  sum-marginal-KL + TC = %.4f + %.4f = %.4f  (vs coupled KL %.4f)"
                   marg-KL TC (+ marg-KL TC) Rc)))
;; F on the vivid case: all five established tokens missed at once
(let [pi (double (get d-indep #{} 0)) pc (double (get d-coup #{} 0))]
  (println (format "P(judge reports NOTHING | 5 tokens established): indep %.3e  coupled %.3e  ratio %.0fx"
                   pi pc (/ pc pi)))
  (println (format "F = -ln P:  indep %.2f nats   coupled %.2f nats" (- (Math/log pi)) (- (Math/log pc)))))
