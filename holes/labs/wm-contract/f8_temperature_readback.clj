;; F8 leg 1 slice 8: production selection-temperature readback.
;;
;; PROVENANCE RULE: EVERY expected value below is transcribed from a NAMED Lean
;; theorem, not derived a second time from the production inputs -- otherwise a
;; delta of 0.0 would only say that Clojure agrees with Clojure.
;;
;; Lean theorem (MachineTemperature / ...Witness)   expected here
;; adaptiveTemperatureReference                     tau_spread [1 2 3.5 0.5] = 3/5
;; emptySpreadIsTheFloor                            tau_spread [] = tau-min
;; degenerateSpreadIsTheFloor                       tau_spread [2 2 2] = 1/100
;; threeLawsDisagree / threeLawsReference           0.3, 0.5, 0.25, pairwise distinct
;; engineeringGammaIsNotInverseBeta                 gamma 10/3 and 2, neither = 1/beta = 4
;; variationalGammaIsInverseBeta                    gamma = 1/beta on the variational arm
;; gainOneReductions / productionFloorGainOne       g=1 gives tau_spread and 1 exactly
;; betaIsNotFloored / belowFloorBetaReference       beta below tau-min survives
;; gainFloorPreventsDivisionByZero /
;;   productionGainFloorReference                   g=0 gives 1/tau-min = 100
;; missingBetaNeverFallsBack, nonfiniteBetaReference,
;;   zeroBetaReference, negativeBetaReference       all four reject, none falls back
;; temperatureDefaultsDisagree                      policy default /= arena default
;; variationalModeCanCarryHeldBeta                  3 of 4 beta sources are not solved
;; largerTemperatureFlattensScores                  gap 1.0 at tau=1, 0.1 at tau=10
;; zeroPriorOrderIsTemperatureInvariant             head action same under lo/hi tau
;; habitPriorMakesOrderTemperatureDependent         habit action cautious -> habitual
(require '[futon2.aif.policy :as policy])
(load-file "scripts/futon2/report/war_machine.clj")
(def wm (find-ns 'futon2.report.war-machine))
(def tau-mode-of (ns-resolve wm 'tau-mode-of))

(def ^:const tau-min 0.01)
(def g-totals [1.0 2.0 3.5 0.5])
(def gain 2.0)
(def beta 0.25)

(defn line [theorem label expected actual]
  (println (format "%-42s %-34s expected %-24s actual %-24s %s"
                   theorem label (pr-str expected) (pr-str actual)
                   (if (= expected actual) "MATCH" "MISMATCH"))))
(defn delta [theorem label expected actual]
  (println (format "%-42s %-34s expected %-24s actual %-24s delta %s"
                   theorem label (pr-str expected) (pr-str actual)
                   (pr-str (- (double actual) (double expected))))))

;; --- adaptive-temperature, the spread law's own value ----------------------
(delta "adaptiveTemperatureReference" "tau_spread [1 2 3.5 0.5]" 0.6
       (policy/adaptive-temperature g-totals))
(line "emptySpreadIsTheFloor" "tau_spread []" tau-min
      (policy/adaptive-temperature []))
(line "degenerateSpreadIsTheFloor" "tau_spread [2 2 2]" 0.01
      (policy/adaptive-temperature [2.0 2.0 2.0]))

;; --- the three laws on ONE input, all defined ------------------------------
(defn law [mode] (policy/effective-temperature g-totals gain
                                               {:tau-mode mode :variational-beta beta
                                                :variational-beta-source :converged-posterior}))
(delta "threeLawsDisagree.1" ":spread" 0.3 (law :spread))
(delta "threeLawsDisagree.2" ":selection-gain-only" 0.5 (law :selection-gain-only))
(delta "threeLawsDisagree.3" ":variational-beta-gamma" 0.25 (law :variational-beta-gamma))
(line "threeLawsDisagree.4-6" "three laws pairwise distinct" [true true true]
      [(not= (law :spread) (law :selection-gain-only))
       (not= (law :spread) (law :variational-beta-gamma))
       (not= (law :selection-gain-only) (law :variational-beta-gamma))])

;; --- gamma = 1/tau, against the registry's own :eq line --------------------
(delta "variationalGammaIsInverseBeta" "gamma on the variational arm" 4.0
       (/ 1.0 (law :variational-beta-gamma)))
(delta "engineeringGammaIsNotInverseBeta.1" "gamma :spread" 3.3333333333333335
       (/ 1.0 (law :spread)))
(delta "engineeringGammaIsNotInverseBeta.2" "gamma :selection-gain-only" 2.0
       (/ 1.0 (law :selection-gain-only)))
(line "engineeringGammaIsNotInverseBeta.3-4" "neither engineering gamma = 1/beta" [true true]
      [(not= (/ 1.0 (law :spread)) (/ 1.0 beta))
       (not= (/ 1.0 (law :selection-gain-only)) (/ 1.0 beta))])

;; --- g = 1 reductions ------------------------------------------------------
(line "productionFloorGainOne.1" "g=1 :spread = tau_spread" true
      (= (policy/effective-temperature g-totals 1.0 {:tau-mode :spread})
         (policy/adaptive-temperature g-totals)))
(line "productionFloorGainOne.2" "g=1 :selection-gain-only = 1" 1.0
      (policy/effective-temperature g-totals 1.0 {:tau-mode :selection-gain-only}))

;; --- the floor is the gain's, not beta's ----------------------------------
(line "productionGainFloorReference" "g=0 :selection-gain-only" 100.0
      (policy/effective-temperature g-totals 0.0 {:tau-mode :selection-gain-only}))
(line "belowFloorBetaReference" "beta=0.001 < tau-min survives" 0.001
      (policy/effective-temperature g-totals 1.0 {:tau-mode :variational-beta-gamma
                                                  :variational-beta 0.001}))

;; --- the variational law is partial and never falls back -------------------
(defn refused? [b]
  (try (policy/effective-temperature g-totals gain
         (cond-> {:tau-mode :variational-beta-gamma} (some? b) (assoc :variational-beta b)))
       false
       (catch Exception _ true)))
(line "missingBetaNeverFallsBack" "beta missing refuses" true (refused? nil))
(line "zeroBetaReference" "beta 0 refuses" true (refused? 0.0))
(line "negativeBetaReference" "beta -0.25 refuses" true (refused? -0.25))
(line "nonfiniteBetaReference" "beta NaN refuses" true (refused? Double/NaN))
(line "nonfiniteBetaReference" "beta Inf refuses" true (refused? Double/POSITIVE_INFINITY))
(line "missingBetaNeverFallsBack" "and none of them returned 1/g" true
      (every? true? [(refused? nil) (refused? 0.0) (refused? -0.25)
                     (refused? Double/NaN) (refused? Double/POSITIVE_INFINITY)]))

;; --- the two defaults name different laws ---------------------------------
(line "temperatureDefaultsDisagree.policy" "effective-temperature default" :spread
      (if (= (policy/effective-temperature g-totals gain {})
             (policy/effective-temperature g-totals gain {:tau-mode :spread}))
        :spread :not-spread))
(line "temperatureDefaultsDisagree.arena" "FUTON_WM_TAU_MODE unset" :selection-gain-only
      (tau-mode-of (System/getenv "FUTON_WM_TAU_MODE")))
(line "temperatureDefaultsDisagree" "the two defaults differ" true
      (not= :spread (tau-mode-of (System/getenv "FUTON_WM_TAU_MODE"))))

;; --- the mode names the law; the source says whether it was solved --------
(line "variationalModeCanCarryHeldBeta" "held sources reported unchanged"
      [:converged-posterior :held-unsolved :held-absent :initial]
      (mapv #(policy/temperature-source {:tau-mode :variational-beta-gamma
                                         :variational-beta-source %})
            [:converged-posterior :held-unsolved :held-absent :initial]))
(line "variationalModeCanCarryHeldBeta" "3 of the 4 are not a solve this tick" 3
      (count (remove #{:converged-posterior}
                     [:converged-posterior :held-unsolved :held-absent :initial])))

;; --- what tau does to the scores ------------------------------------------
(def two-g [1.0 0.0])
(defn gap [tau] (let [[a b] (policy/selection-scores two-g tau nil)] (- b a)))
(delta "largerTemperatureFlattensScores.1" "score gap at tau=1" 1.0 (gap 1.0))
(delta "largerTemperatureFlattensScores.2" "score gap at tau=10" 0.1 (gap 10.0))
(line "largerTemperatureFlattensScores" "gap contracts as tau rises" true
      (< (gap 10.0) (gap 1.0)))

;; --- and when it can reach the choice --------------------------------------
(def ranked
  [{:action {:type :a} :rank 1 :controller-score 1.0 :habit-prior-bias 0.0}
   {:action {:type :b} :rank 2 :controller-score 0.0 :habit-prior-bias 0.0}])
(defn head-law [b]
  (policy/select-action ranked
                        {:selection-boundary :strategic-recommendation
                         :temperature-opts {:tau-mode :variational-beta-gamma
                                            :variational-beta b
                                            :variational-beta-source :converged-posterior}}))
(line "zeroPriorOrderIsTemperatureInvariant" "head action at tau 0.1 vs 9.0" true
      (= (:action (head-law 0.1)) (:action (head-law 9.0))))
(line "zeroPriorOrderIsTemperatureInvariant" "the recorded tau DID move" true
      (not= (:tau (head-law 0.1)) (:tau (head-law 9.0))))
(line "zeroPriorOrderIsTemperatureInvariant" "and so did :softmax-weights" true
      (not= (:softmax-weights (head-law 0.1)) (:softmax-weights (head-law 9.0))))

(def ranked-hp
  [{:action {:type :cautious} :rank 1 :controller-score 0.0 :habit-prior-bias 0.0}
   {:action {:type :habitual} :rank 2 :controller-score 2.0 :habit-prior-bias 1.0}])
(defn habit-law [b]
  (policy/select-action ranked-hp
                        {:abstain-epsilon 0.0
                         :temperature-opts {:tau-mode :variational-beta-gamma
                                            :variational-beta b
                                            :variational-beta-source :converged-posterior}}))
(line "habitPriorMakesOrderTemperatureDependent.1" "habit action at tau=0.5" {:type :cautious}
      (:action (habit-law 0.5)))
(line "habitPriorMakesOrderTemperatureDependent.2" "habit action at tau=8.0" {:type :habitual}
      (:action (habit-law 8.0)))
