(ns ants.aif-policy-test
  (:require [clojure.test :refer :all]
            [ants.aif.policy :as policy]
            [ants.aif.core   :as core]
            [ants.aif.observe :as observe]))

;; --- [1] AIF action selection sign check -------------------------------
;; Policy currently samples softmax over -G/tau already (see choose-action). :contentReference[oaicite:0]{index=0}
;; We expose a tiny helper so we can unit-test the sign directly.

(defn probs-from-G
  "Return {action p} softmax over -G/tau."
  [G tau]
  (let [scores (map (fn [[a g]] {:action a :logit (/ (- (double g)) (double tau))})
                    G)
        tau (max 1e-3 (double tau))
        max-logit (apply max (map :logit scores))
        exps (map #(Math/exp (- (:logit %) max-logit)) scores)
        Z (double (reduce + exps))]
    (into {}
          (map (fn [{:keys [action]} e]
                 [action (if (zero? Z) (/ 1.0 (count scores)) (/ e Z))])
               scores exps))))

(deftest efe-softmax-minimises-G
  (let [G {:forage 1.2 :return 3.0 :hold 2.0}
        tau 1.0
        p (probs-from-G G tau)]
    (is (> (p :forage) (p :return)))
    (is (> (p :forage) (p :hold)))))

;; --- [2] need-error calibration ----------------------------------------
;; Matches the existing definition in ants.aif.affect/need-error. :contentReference[oaicite:1]{index=1}

(defn need-error
  [state] ((requiring-resolve 'ants.aif.affect/need-error) state))

(deftest need-error-behaviour
  (is (<= (need-error {:obs {:hunger 0.35 :ingest 0.65}}) 0.05)) ; near setpoint
  (is (>  (need-error {:obs {:hunger 0.80 :ingest 0.05}}) 0.7))
  (is (>  (need-error {:obs {:hunger 0.20 :ingest 0.00}}) 0.5)))

;; --- [7] Temperature / precision dynamics ------------------------------
;; update-tau increases with need; the cap is 1.5 in defaults. :contentReference[oaicite:2]{index=2} :contentReference[oaicite:3]{index=3}

(defn update-tau [prec st] ((requiring-resolve 'ants.aif.affect/update-tau) prec st))

(deftest tau-responds-to-need
  (let [cool (update-tau {:tau 0.4} {:obs {:hunger 0.35 :ingest 0.7}})
        hot  (update-tau {:tau 0.4} {:obs {:hunger 0.8  :ingest 0.0}})]
    (is (< (:tau cool) 0.5))
    (is (> (:tau hot)  0.5))))
