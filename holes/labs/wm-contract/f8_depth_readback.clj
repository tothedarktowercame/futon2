;; F8 leg 1 slice 7: production temporal-depth readback.
;;
;; PROVENANCE RULE: EVERY expected value below is transcribed from a NAMED Lean
;; theorem, not derived a second time from the production inputs -- otherwise a
;; delta of 0.0 would only say that Clojure agrees with Clojure.
;;
;; Lean theorem (MachineDepth / MachineDepthWitness)   expected here
;; depthThreeTrajectory                                :horizon-steps 3, |trajectory| 3
;; depthThreeDiffersFromFirst                          final mean /= first mean
;; forwardDefaultReference                             forward-model default 3
;; rolloutDefaultReference                             rollout default 2
;; cascadeLaneReference                                cascade lane depth 5
;; someOneEqualsNone / nilDepthReference               nil and 1 score identically
;; gTermsDisagreeOnDepth                               risk + homeostatic move with K,
;;                                                     ambiguity + information do not
;; trajectoryVarianceIsOne                             every step's variance = step 1's
;; epistemicAtThreeEqualsAtOne                         the epistemic delta is exactly 0
(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[futon2.aif.belief :as belief]
         '[futon2.aif.efe :as efe]
         '[futon2.aif.forward-model :as fm]
         '[futon2.aif.rollout :as rollout])

(def obs {:loop-health 0.9 :support-coverage 0.9 :attack-coverage 0.9
          :mission-health 0.2 :stack-pct 0.2 :consulting-pct 0.25
          :portfolio-pct 0.25 :mathematics-pct 0.2 :active-repo-ratio 0.8
          :sorry-count-norm 0.85 :coupling-density 0.2
          :ticks-firing-ratio 0.0 :depositing-signal 0.1})
(def state {:observation obs :belief (belief/initial-belief-state [:m1 :m2])})
(def action {:type :address-sorry :target :m1})
(def multi (fm/predict-multi-horizon state action 3))
(def first-mean (get-in multi [:trajectory 0 :next-observation :mean :sorry-count-norm]))
(def final-mean (get-in multi [:final-state :observation :sorry-count-norm]))
(def first-var (get-in multi [:trajectory 0 :next-observation :variance]))
(def final-var (get-in multi [:trajectory 2 :next-observation :variance]))
(def nil-score (efe/compute-efe state action))
(def one-score (efe/compute-efe state action {:horizon-steps 1}))
(def three-score (efe/compute-efe state action {:horizon-steps 3}))
;; rollout-horizon is private; the two call shapes are rollout.clj:474-479's
;; default and the cascade lane's explicit :depth (cascade_lane.clj:381).
(def rollout-depth (#'rollout/rollout-horizon {}))
(def cascade-depth (#'rollout/rollout-horizon {:depth 5}))
(defn delta [a b] (- (double a) (double b)))
(def variance-max-delta
  (apply max (map (fn [k] (Math/abs (delta (get final-var k 0.0) (get first-var k 0.0))))
                  (distinct (concat (keys first-var) (keys final-var))))))

(def lines
  ["F8 leg 1 slice 7 -- production temporal-depth readback"
   (str "multi horizon actual=" (:horizon-steps multi)
        " Lean=3 (depthThreeTrajectory) delta=" (delta (:horizon-steps multi) 3))
   (str "trajectory count actual=" (count (:trajectory multi))
        " Lean=3 (depthThreeTrajectory) delta=" (delta (count (:trajectory multi)) 3))
   (str "step-1 mean=" first-mean " final-K mean=" final-mean
        " final-minus-first=" (delta final-mean first-mean)
        " Lean=distinct (depthThreeDiffersFromFirst)")
   ;; THE FOUR DECLARED DEPTHS, each measured through the code that reads it.
   (str "forward-model default actual=" fm/default-horizon-steps
        " Lean=3 (forwardDefaultReference) delta=" (delta fm/default-horizon-steps 3))
   (str "rollout default actual=" rollout-depth
        " Lean=2 (rolloutDefaultReference) delta=" (delta rollout-depth 2))
   (str "cascade lane depth actual=" cascade-depth
        " Lean=5 (cascadeLaneReference) delta=" (delta cascade-depth 5))
   (str "live tick literal is 3 under loaded events (war_machine.clj:6283-6285),"
        " Lean=3 (liveDepthWitness) -- not callable from here, not measured")
   ;; nil and 1 are the same depth.
   (str "nil horizon=" (pr-str (:horizon-steps nil-score))
        " one horizon=" (pr-str (:horizon-steps one-score))
        " three horizon=" (pr-str (:horizon-steps three-score))
        " Lean=nil,nil,3 (someOneEqualsNone)")
   (str "nil-vs-one controller delta="
        (delta (:controller-score nil-score) (:controller-score one-score))
        " Lean=0 (someOneEqualsNone)")
   ;; THE DISAGREEMENT: two terms move with K, two are frozen at depth 1.
   (str "K3-minus-K1 G-risk=" (delta (:G-risk three-score) (:G-risk one-score))
        " Lean=nonzero (gTermsDisagreeOnDepth, risk at depth K)")
   (str "K3-minus-K1 homeostatic-pressure="
        (delta (:homeostatic-pressure three-score) (:homeostatic-pressure one-score))
        " Lean=nonzero (gTermsDisagreeOnDepth, homeostatic at depth K)")
   (str "K3-minus-K1 G-ambiguity="
        (delta (:G-ambiguity three-score) (:G-ambiguity one-score))
        " Lean=0 (epistemicAtThreeEqualsAtOne)")
   (str "K3-minus-K1 predictability-bonus="
        (delta (:predictability-bonus three-score) (:predictability-bonus one-score))
        " Lean=0 (epistemicAtThreeEqualsAtOne)")
   ;; WHY the epistemic terms cannot move: the variance is state-blind, so the
   ;; depth-K variance efe.clj:636 discards EQUALS the depth-1 one it keeps.
   (str "step-3 variance minus step-1 variance, max over channels="
        variance-max-delta " Lean=0 (trajectoryVarianceIsOne)")
   (str "step-3 variance = step-1 variance actual=" (= first-var final-var)
        " Lean=true (trajectoryVarianceIsConstant)")
   (if (and (= 3 (:horizon-steps multi) (count (:trajectory multi))
               fm/default-horizon-steps)
            (= 2 rollout-depth)
            (= 5 cascade-depth)
            (not= first-mean final-mean)
            (nil? (:horizon-steps nil-score)) (nil? (:horizon-steps one-score))
            (= 3 (:horizon-steps three-score))
            (zero? (delta (:controller-score nil-score) (:controller-score one-score)))
            (not (zero? (delta (:G-risk three-score) (:G-risk one-score))))
            (not (zero? (delta (:homeostatic-pressure three-score)
                               (:homeostatic-pressure one-score))))
            (zero? (delta (:G-ambiguity three-score) (:G-ambiguity one-score)))
            (zero? (delta (:predictability-bonus three-score)
                          (:predictability-bonus one-score)))
            (zero? variance-max-delta)
            (= first-var final-var))
     "VERDICT: production matches every Lean depth witness."
     "VERDICT: MISMATCH.")])

(let [out (io/file "holes/labs/wm-contract/runs/F8-depth/clojure-readback.txt")]
  (io/make-parents out)
  (spit out (str (str/join "\n" lines) "\n"))
  (println (slurp out)))
