(ns futon2.aif.efe-structural-pressure-mode-test
  "D-1d witness (M-aif-faithfulness §2.1): G-structural-pressure relocation to
   the R12 habit-prior seam, built DARK.

   Goldens captured at `6b4d9a8` (pre-D-1d HEAD) on a fixture whose actions
   CARRY :structural-pressure-per-action (sp .8/.2/.4/0 across the four) — so
   default-mode byte-identity is load-bearing, not vacuous. Two levels:
   G-total (rank-actions) and selection (policy/select-action).

   Dark-mode section documents the FLIP MEMO SEED: what changes about ranking
   when the term moves from G to prior — (a) :G-total rises by exactly w·sp
   (the credit leaves G), (b) the bias re-enters at selection UNSCALED by
   τ_eff (precision stops modulating habit), (c) census/argmin views of G no
   longer see the term."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]
            [futon2.aif.policy :as policy]))

(def ^:private obs
  {:loop-health 0.9 :support-coverage 0.9 :attack-coverage 0.9
   :mission-health 0.7 :stack-pct 0.2 :consulting-pct 0.25
   :portfolio-pct 0.25 :mathematics-pct 0.2 :active-repo-ratio 0.7
   :sorry-count-norm 0.2 :coupling-density 0.2 :ticks-firing-ratio 0.0
   :annotation-health 0.9})

(def ^:private state
  {:observation obs :belief (belief/initial-belief-state [:m1 :m2])})

(def ^:private graph
  {:capabilities {:goal-cap {:status :held :scope []}}
   :missions {"M-app"   {:scope [] :produces [:goal-cap] :open-hole-count 2}
              "M-inapp" {:scope [:blocked-cap] :produces [] :open-hole-count 4}}})

(def ^:private actions
  [{:type :address-sorry :target :m1 :intrinsic-value 0.1
    :structural-pressure-per-action 0.4}
   {:type :open-mission :target "M-app" :weight 1.0
    :structural-pressure-per-action 0.8}
   {:type :open-mission :target "M-inapp" :weight 1.0}
   {:type :open-mission :target "M-off" :weight 1.0
    :structural-pressure-per-action 0.2}])

(def ^:private base-opts
  {:capability-graph graph :pre-registered-goal :goal-cap
   :mission-gap-view {"M-app" 0.5 "M-inapp" 0.9 "M-off" 0.2}
   :goal-outcome-entries []
   :goal-outcome-prob-fn (constantly 0.7)
   :time-pressure 0.5})

(def ^:private kl-opts
  (merge base-opts {:risk-mode :kl :ambiguity-mode :gaussian-entropy
                    :goal-outcome-mode :kl}))

;; Captured at 6b4d9a8 (pre-D-1d). Byte-identity = clojure.core/= on doubles.
(def ^:private golden
  {:hinge {:totals [-22.438000000000002 -6.428000000000001 -5.469 1001.442]
           :order  ["M-app" "M-off" :m1 "M-inapp"]
           :decision ["M-app" -22.438000000000002 204.776]}
   :kl    {:totals [6.9128090379398515 22.922809037939853
                    24.260655551165016 1030.7928090379398]
           :order  ["M-app" "M-off" :m1 "M-inapp"]
           :decision ["M-app" 6.9128090379398515 204.776]}})

(defn- ranked [opts] (efe/rank-actions state actions opts))

(deftest default-mode-byte-identity
  (doseq [[label opts] [[:hinge base-opts] [:kl kl-opts]]
          sp-opts [nil {:structural-pressure-mode :g-summand}]]
    (let [rs (ranked (merge opts sp-opts))
          g (golden label)]
      (testing (str label " " (pr-str sp-opts) " — G-totals byte-identical")
        (is (= (:totals g) (mapv :G-total rs))))
      (testing (str label " — order identical")
        (is (= (:order g) (mapv #(get-in % [:action :target]) rs))))
      (testing (str label " — mode self-describing, no dark key leaks")
        (is (every? #(= :g-summand (:structural-pressure-mode %)) rs))
        (is (not-any? :habit-prior-bias rs))))))

(deftest default-mode-selection-byte-identity
  (doseq [[label opts] [[:hinge base-opts] [:kl kl-opts]]]
    (let [d (policy/select-action (ranked opts) {:policy-precision 1.0})
          [target g-total tau] (get-in golden [label :decision])]
      (testing (str label " — chosen action, G-total, tau all golden")
        (is (= target (get-in d [:action :target])))
        (is (= g-total (:G-total d)))
        (is (= tau (:tau d)))
        (is (not (:habit-prior-applied? d)))))))

(deftest dark-mode-g-relocation
  (let [default-rs (ranked base-opts)
        dark-rs (ranked (assoc base-opts :structural-pressure-mode :habit-prior))
        w efe/default-structural-pressure-weight
        by-target (fn [rs] (into {} (map (juxt #(get-in % [:action :target])
                                               identity) rs)))
        d (by-target default-rs) k (by-target dark-rs)]
    (testing ":G-total rises by exactly w·sp when the credit leaves G"
      (doseq [t ["M-app" "M-off" :m1 "M-inapp"]]
        (let [sp (double (:G-structural-pressure (d t)))]
          (is (< (Math/abs (- (double (:G-total (k t)))
                              (+ (double (:G-total (d t))) (* (double w) sp))))
                 1e-9)))))
    (testing "the layer drops :structural-pressure; decomposition still holds"
      (doseq [r dark-rs]
        (is (not (contains? (:augmentation-terms r) :structural-pressure)))
        (is (< (Math/abs (- (double (:G-total r))
                            (+ (double (:G-core r))
                               (double (:G-augmentation r)))))
               1e-9))))
    (testing ":habit-prior-bias = +w·sp, direction preserved (higher sp ⇒ higher bias)"
      (doseq [r dark-rs]
        (is (< (Math/abs (- (double (:habit-prior-bias r))
                            (* (double w)
                               (double (:G-structural-pressure r)))))
               1e-9)))
      (is (> (:habit-prior-bias (k "M-app")) (:habit-prior-bias (k "M-off")))))
    (testing "raw :G-structural-pressure still emitted (observability unchanged)"
      (is (= (mapv :G-structural-pressure default-rs)
             (mapv :G-structural-pressure dark-rs))))))

(deftest dark-mode-selection-seam
  (let [dark-rs (ranked (assoc base-opts :structural-pressure-mode :habit-prior))
        d (policy/select-action dark-rs {:policy-precision 1.0})]
    (testing "selection engages the prior seam and self-describes"
      (is (:habit-prior-applied? d)))
    (testing "chosen action is argmax(ln E − G/τ), computable independently"
      (let [tau (:tau d)
            scores (mapv #(+ (/ (- (double (:G-total %))) (double tau))
                             (double (:habit-prior-bias %)))
                         dark-rs)
            expect (get-in (nth dark-rs (apply max-key scores
                                               (range (count scores))))
                           [:action :target])]
        (is (= expect (get-in d [:action :target])))))
    (testing "zero-bias entries under dark mode ⇒ historical path (no seam)"
      (let [no-sp-actions (mapv #(dissoc % :structural-pressure-per-action)
                                actions)
            rs (efe/rank-actions state no-sp-actions
                                 (assoc base-opts
                                        :structural-pressure-mode :habit-prior))
            d2 (policy/select-action rs {:policy-precision 1.0})]
        (is (not (:habit-prior-applied? d2)))
        (is (= 1 (:rank d2)))))))
