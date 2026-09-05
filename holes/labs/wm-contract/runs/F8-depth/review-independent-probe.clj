;; :F8 leg 1 slice 7 -- reviewing seat's INDEPENDENT probe of the temporal
;; depth T, run before codex-9 reported. Nothing here reads codex-9's Lean or
;; readback; it calls production directly so the two derivations can be
;; compared rather than one trusted.
(require '[futon2.aif.efe :as efe]
         '[futon2.aif.forward-model :as fm]
         '[futon2.aif.belief :as belief]
         '[futon2.aif.rollout :as rollout])

(def obs {:loop-health 0.9 :support-coverage 0.9 :attack-coverage 0.9
          :mission-health 0.2 :stack-pct 0.20 :consulting-pct 0.25
          :portfolio-pct 0.25 :mathematics-pct 0.20 :active-repo-ratio 0.8
          :sorry-count-norm 0.85 :coupling-density 0.2
          :ticks-firing-ratio 0.0 :depositing-signal 0.1})
(def state {:observation obs :belief (belief/initial-belief-state [:m1 :m2])})
(def action {:type :address-sorry :target :m1})

;; --- the three declared depths ---------------------------------------------
(println "fm/default-horizon-steps =" fm/default-horizon-steps)
(println "rollout-horizon {} =" (#'rollout/rollout-horizon {}))
(println "rollout-horizon {:horizon 5} =" (#'rollout/rollout-horizon {:horizon 5}))
;; war_machine.clj:6283-6285 is a literal 3 under a condition; not callable here.

;; --- predict-multi-horizon: K iterates of the SAME action ------------------
(def m3 (fm/predict-multi-horizon state action 3))
(println "K=3 :horizon-steps" (:horizon-steps m3)
         "trajectory count" (count (:trajectory m3)))
(def step1-mean (get-in (fm/predict state action) [:next-observation :mean]))
(def finalK-mean (get-in m3 [:final-state :observation]))
(println "step-1 sorry-count-norm" (:sorry-count-norm step1-mean)
         "final-K sorry-count-norm" (:sorry-count-norm finalK-mean)
         "delta" (- (double (:sorry-count-norm finalK-mean))
                    (double (:sorry-count-norm step1-mean))))
(println "final-state = first-step mean?" (= finalK-mean step1-mean))
;; the 2-arity is the only reader of default-horizon-steps
(println "2-arity :horizon-steps" (:horizon-steps (fm/predict-multi-horizon state action)))

;; --- efe: none vs 1 vs 3 ---------------------------------------------------
(def e-nil (efe/compute-efe state action))
(def e-1   (efe/compute-efe state action {:horizon-steps 1}))
(def e-3   (efe/compute-efe state action {:horizon-steps 3}))
(println "recorded :horizon-steps nil/1/3 =>"
         (:horizon-steps e-nil) (:horizon-steps e-1) (:horizon-steps e-3))
(println "controller-score nil-vs-1 delta"
         (- (double (:controller-score e-nil)) (double (:controller-score e-1))))
;; the four terms that matter: two read the depth-K mean, two the depth-1 variance
(doseq [k [:G-risk :homeostatic-pressure :G-ambiguity :predictability-bonus
           :G-core :controller-score]]
  (println (format "%-18s K=1 %.17g  K=3 %.17g  delta %.17g"
                   (str k)
                   (double (or (k e-1) 0.0)) (double (or (k e-3) 0.0))
                   (- (double (or (k e-3) 0.0)) (double (or (k e-1) 0.0))))))
