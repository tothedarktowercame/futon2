(ns futon2.aif.efe-struct-split-test
  "B-2a/B-2b witness (M-aif-faithfulness §2.2): the G-total struct split and the
   G-graph-pragmatic split are RELABELS — behaviour must be BYTE-IDENTICAL.

   The golden vectors below were captured at `c11e162` (pre-split HEAD) by
   running this exact fixture through `efe/rank-actions` — if any of these
   assertions fails, the split changed live behaviour and must go dark behind
   a flag instead (§2.4; flips are Joe's).

   Decomposition invariants use 1e-9 tolerance, NOT exactness: `:G-total`
   keeps its historical left-to-right summation order (that is what byte-
   identity means), while `:G-augmentation` sums the same six signed
   contributions in one go — float associativity may differ in the last ulp."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]))

(def ^:private obs
  {:loop-health 0.9 :support-coverage 0.9 :attack-coverage 0.9
   :mission-health 0.7 :stack-pct 0.2 :consulting-pct 0.25
   :portfolio-pct 0.25 :mathematics-pct 0.2 :active-repo-ratio 0.7
   :sorry-count-norm 0.2 :coupling-density 0.2 :ticks-firing-ratio 0.0
   :annotation-health 0.9})

(def ^:private state
  {:observation obs :belief (belief/initial-belief-state [:m1 :m2])})

(def ^:private graph
  {:capabilities {:goal-cap {:status :held :scope []}
                  :missing-cap {:status :held :scope []}}
   :missions {"M-app"   {:scope [] :produces [:goal-cap] :open-hole-count 2}
              "M-inapp" {:scope [:blocked-cap] :produces [] :open-hole-count 4}}})

(def ^:private entries
  [{:status :open :preferred {:op :becomes :value :attested}
    :weight {:value 0.6} :outcome-ref {:kind :capability :id "cap/one"}}
   {:status :open :preferred {:op :becomes :value :closed}
    :weight {:value 0.4} :outcome-ref {:kind :capability :id "cap/two"}}])

(def ^:private actions
  [{:type :address-sorry :target :m1 :intrinsic-value 0.1}
   {:type :open-mission :target "M-app" :weight 1.0}
   {:type :open-mission :target "M-inapp" :weight 1.0}
   {:type :open-mission :target "M-off" :weight 1.0}])

(def ^:private base-opts
  {:capability-graph graph :pre-registered-goal :goal-cap
   :mission-gap-view {"M-app" 0.5 "M-inapp" 0.9 "M-off" 0.2}
   :goal-outcome-entries entries
   :goal-outcome-prob-fn (constantly 0.7)
   :time-pressure 0.5})

(def ^:private kl-opts
  (merge base-opts {:risk-mode :kl :ambiguity-mode :gaussian-entropy
                    :goal-outcome-mode :kl}))

;; Captured at c11e162 (pre-split). Byte-identity = clojure.core/= on doubles.
(def ^:private golden
  {:hinge {:totals [-21.658 -5.8580000000000005 -4.829000000000001 1001.942]
           :risks  [0.0 0.0 -0.15000000000000002 0.0]
           :graph  [-14.0 0.0 0.0 1012.0]
           :order  ["M-app" "M-off" :m1 "M-inapp"]}
   :kl    {:totals [12.192831721528313 27.99283172152831
                    29.400678234753478 1035.7928317215283]
           :risks  [129.1711668051925 129.1711668051925
                    130.0781604989776 129.1711668051925]
           :graph  [-14.0 0.0 0.0 1012.0]
           :order  ["M-app" "M-off" :m1 "M-inapp"]}})

(defn- ranked [opts] (efe/rank-actions state actions opts))

(deftest byte-identity-witness
  (doseq [[label opts] [[:hinge base-opts] [:kl kl-opts]]]
    (let [rs (ranked opts)
          g (golden label)]
      (testing (str label " — G-total byte-identical to pre-split golden")
        (is (= (:totals g) (mapv :G-total rs))))
      (testing (str label " — G-risk byte-identical")
        (is (= (:risks g) (mapv :G-risk rs))))
      (testing (str label " — G-graph-pragmatic byte-identical")
        (is (= (:graph g) (mapv :G-graph-pragmatic rs))))
      (testing (str label " — ranking order identical")
        (is (= (:order g) (mapv #(get-in % [:action :target]) rs)))))))

(deftest core-augmentation-decomposition
  (doseq [[label opts] [[:hinge base-opts] [:kl kl-opts]]]
    (doseq [r (ranked opts)]
      (testing (str label " — :G-total ≈ :G-core + :G-augmentation (1e-9)")
        (is (< (Math/abs (- (double (:G-total r))
                            (+ (double (:G-core r))
                               (double (:G-augmentation r)))))
               1e-9)))
      (testing (str label " — :G-augmentation = Σ of the seven named layer terms")
        (is (< (Math/abs (- (double (:G-augmentation r))
                            (reduce + 0.0 (vals (:augmentation-terms r)))))
               1e-9)))
      (testing (str label " — the layer names exactly the seven demoted terms")
        (is (= #{:info :survival :structural-pressure :graph-pragmatic
                 :eig-bmr :gap :goal-outcome}
               (set (keys (:augmentation-terms r)))))))))

(deftest graph-pragmatic-split
  (let [rs (ranked base-opts)
        by-target (into {} (map (juxt #(get-in % [:action :target]) identity) rs))]
    (testing "in-map applicable: feasibility 0, proxy = body − ascent"
      (let [r (by-target "M-app")]
        (is (= 0.0 (:G-graph-feasibility r)))
        (is (< (Math/abs (- -14.0 (double (:G-graph-pragmatic-proxy r)))) 1e-9))))
    (testing "in-map inapplicable: the 1000·mask sits in :G-graph-feasibility"
      (let [r (by-target "M-inapp")]
        (is (= 1000.0 (:G-graph-feasibility r)))
        (is (< (Math/abs (- 12.0 (double (:G-graph-pragmatic-proxy r)))) 1e-9))))
    (testing "off-map: penalty (default 0.0) is feasibility-class, proxy 0"
      (let [r (by-target "M-off")]
        (is (= 0.0 (:G-graph-feasibility r)))
        (is (= 0.0 (:G-graph-pragmatic-proxy r)))))
    (testing "split invariant: pragmatic ≈ feasibility + proxy (1e-9)"
      (doseq [r rs :when (contains? r :G-graph-feasibility)]
        (is (< (Math/abs (- (double (:G-graph-pragmatic r 0.0))
                            (+ (double (:G-graph-feasibility r))
                               (double (:G-graph-pragmatic-proxy r)))))
               1e-9))))))
