(ns futon2.aif.wm01-numerical-operations-test
  "WM-01-numerical-operations: reviewed numerical relations at the ACTUAL
  composed prediction/scoring operations of the runtime producer
  (cascade-model-manifest: rollout propagation, predict-observations
  marginalization, preference/log arithmetic, rank-cascade-actions ordering)
  against the independent exact-rational scorer (cascade-g: rational atanh
  log enclosures), on ADMITTED inputs (observed-belief q0, rate-derived exact
  A, spec C), preserving the exact-kernel premise against the float-carried
  C row — the non-exact represented row the clause names.

  A row-sum tolerance alone supplies no prediction, KL or ordering bound —
  every relation here is a value-level equality/enclosure/ordering claim at
  composed operations, not a normalization check."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-g :as cg]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.efe :as efe]
            [futon2.aif.machine-model :as model]))

;; ---- the shared admitted inputs (small, exact where the kernel is exact) ----

(def ^:private rates
  {"t0" {:false-neg 1/10 :false-pos 1/20}
   "t1" {:false-neg 1/5 :false-pos 1/10}})

(def ^:private states
  [#{} #{"t0"} #{"t1"} #{"t0" "t1"}])

(def ^:private outcomes states) ;; token universes: outcomes are token-sets too

(def ^:private spec
  ;; both tokens wanted: G separates all four states from a ∅ start
  ;; (a single-token want makes G a function of that token's marginal alone
  ;; and the ordering fixture degenerates to exact ties — found, not assumed)
  (m/preference-spec {:want #{"t0" "t1"} :evidence #{} :lam 1 :mu 0 :zeroed #{}}))

(def ^:private c-float
  "The runtime C producer's own output: preference-distribution (binary64)."
  (m/preference-distribution spec #{"t0" "t1"}))

(def ^:private model-id {:id "wm01-numerical-operations" :revision "v1"})

(defn- a-carrier
  "A in categorical-ambiguity's carrier shape, masses from the runtime
  producer's own exact-rational token-likelihood rows."
  []
  {:model model-id :state-support states :outcome-support outcomes
   :authority :observed-estimate
   :rows (into {} (map (fn [s]
                         [s {:support outcomes
                             :mass (m/observation-distribution rates s)}]))
                     states)})

(def ^:private produce-t0
  {:id :produce-t0 :guard {:status :interpreted :operator :and
                           :clauses [{:status :interpreted :present #{}
                                      :absent #{}}]}
   :transition {:status :interpreted :operator :union :produces #{"t0"}}
   :produces #{"t0"}})

(def ^:private produce-t1
  {:id :produce-t1 :guard {:status :interpreted :operator :and
                           :clauses [{:status :interpreted :present #{}
                                      :absent #{}}]}
   :transition {:status :interpreted :operator :union :produces #{"t1"}}
   :produces #{"t1"}})

;; ---- 1. the duplicated coordinate conversions still agree ----

(deftest duplicated-conversions-direct-battery
  ;; Direct value level: cascade-g's conversion is observable via step-g's
  ;; :joint on a SINGLE-state support (joint = q-mass * A-mass; with A
  ;; identity and single-state support the joint IS the conversion of the q
  ;; mass). machine-model's conversion is observable via numeric-row-admission
  ;; of the single-entry-complement row: exact-total = conv(1-v) + conv(v).
  ;; conv(1-v) for the doubles below is derived by the same public admission
  ;; of {x 1-v} alone. Two admissions, no arithmetic assumptions.
  (let [conv (fn [x] (get-in (model/numeric-row-admission {#{} x}) [:exact-total]))
        ;; two admitted states; A sends both to the single outcome exactly,
        ;; so joint[state1, outcome] = cascade-g's conversion of x exactly.
        joint-entry (fn [x]
                      (get-in (cg/step-g
                               {:q {:model model-id :state-support [:s1 :s2]
                                    :mass {:s1 x :s2 (- 1 x)}
                                    :authority :declared-prior}
                                :a {:model model-id :state-support [:s1 :s2]
                                    :outcome-support [:o]
                                    :rows {:s1 {:support [:o] :mass {:o 1}}
                                           :s2 {:support [:o] :mass {:o 1}}}
                                    :authority :observed-estimate}
                                :c {:model model-id :support [:o]
                                    :mass {:o 1} :provenance {:t 1}}
                                :context {:policy/id :p :occurrence/id :e
                                          :point :pt}})
                              [:joint :s1 :o]))]
    (doseq [v [0.1 0.25 0.5 1/3 2/3 0.9
               0.9999999999999999 (float 0.1) (float 0.7)
               (BigDecimal. "0.3") (BigDecimal. "0.125")]]
      (is (= (conv v) (joint-entry v))
          (str "both conversions of " (pr-str v) " agree")))))

;; ---- 2. propagation: rollout is linear in the belief mixture ----

(deftest rollout-propagation-is-linear
  ;; Independent numerical relation at the composed propagation op: the
  ;; push-forward of a mixture must equal the mixture of the push-forwards of
  ;; its point masses (exact rational arithmetic, exact equality).
  (let [mix {#{} 1/2 #{"t0"} 1/3 #{"t1"} 1/6}
        prec (constantly [produce-t0])
        whole (m/rollout prec mix 1)
        scale (fn [w q-t] (into {} (map (fn [[s p]] [s (*' w p)])) q-t))
        parts (merge-with +'
                          (scale 1/2 (m/rollout prec {#{} 1} 1))
                          (scale 1/3 (m/rollout prec {#{"t0"} 1} 1))
                          (scale 1/6 (m/rollout prec {#{"t1"} 1} 1)))]
    (is (map? whole))
    (is (= parts whole)
        "push-forward of the mixture = mixture of point-mass push-forwards")))

;; ---- 3. marginalization: predict-observations vs direct enumeration ----

(deftest marginalization-equals-direct-sum
  ;; Independent composition: Q(o) computed observation-major (for each o,
  ;; sum A(s,o)·q(s) over states — token-likelihood called per pair) vs the
  ;; runtime producer's state-major predict-observations. Exact rationals,
  ;; exact equality.
  (let [q {#{} 1/2 #{"t0"} 1/2}
        via-producer (m/predict-observations rates q)
        direct (into {} (for [o outcomes]
                          [o (reduce +' 0
                                     (map (fn [[s p]]
                                            (*' p (m/token-likelihood rates s o)))
                                          q))]))]
    (is (= via-producer direct)
        "state-major and observation-major marginalization agree exactly")))

;; ---- 4/5. the producer's floats sit inside the exact scorer's enclosures,
;;          and the producer's ORDERING is decided by disjoint enclosures ----

(defn- cg-step
  "One cascade-g scoring point from the runtime producer's own outputs:
  q_tau by rollout, A rows by token-likelihood, C by preference-distribution."
  [q-tau a point]
  (let [q-support (vec (sort-by count (keys q-tau)))]
    (cg/step-g {:q {:model model-id :state-support q-support
                    :mass q-tau
                    :authority :declared-prior}
                ;; A narrowed to exactly q's support (ambiguity requires the
                ;; supports equal); rows are the producer's own exact rows.
                :a (assoc a :state-support q-support
                          :rows (select-keys (:rows a) q-support))
                :c {:model model-id :support outcomes :mass c-float
                    :provenance {:producer "cascade-model-manifest/preference-distribution"}}
                :context {:policy/id :probe :occurrence/id :wm01
                          :point point}})))

(deftest producer-floats-lie-inside-exact-enclosures
  (let [a (a-carrier)
        cmap (into {} (map (fn [o] [o (get c-float o 0.0)]) outcomes))
        base {:rates rates :q0 (m/observed-belief #{"t0"})}]
    (doseq [prec [(constantly []) (constantly [produce-t0]) (constantly [produce-t1])]
            tau [1 2 3]]
      (let [q-tau (m/rollout prec (:q0 base) tau)
            {:keys [risk ambiguity]} (cg-step q-tau a tau)
            [rlo rhi] risk
            [alo ahi] ambiguity
            ;; the runtime producer's own float composed ops:
            predicted (m/predict-observations rates q-tau)
            risk-float (m/outcome-risk predicted cmap)
            amb-float (m/step-ambiguity rates q-tau)]
        (is (number? risk-float) (str "producer risk finite, prec/tau " tau))
        (is (number? amb-float))
        (is (<= rlo (+ (double risk-float) 1e-12)))
        (is (>= rhi (- (double risk-float) 1e-12)))
        (is (<= alo (+ (double amb-float) 1e-12)))
        (is (>= ahi (- (double amb-float) 1e-12)))
        ;; the enclosure is TIGHT (an actual bound, not a wide interval):
        (is (< (- (double rhi) (double rlo)) 1e-12) "risk enclosure width < 1e-12")
        (is (< (- (double ahi) (double alo)) 1e-12) "ambiguity enclosure width < 1e-12")))))

(deftest exact-kernel-premise-preserved-against-float-c
  ;; q and A are exact-rational admitted (the exact-kernel premise); C is the
  ;; float-carried represented row. step-g must keep the distinction: exact
  ;; admissions for q/a, :float-carried for c, :exactly-normalized-inputs?
  ;; false because of c — the premise is recorded, not silently upgraded.
  (let [a (a-carrier)
        step (cg-step (m/observed-belief #{"t0"}) a :p1)]
    (is (= :exact (get-in step [:admissions :q :admission])))
    (is (every? #(= :exact (:admission %)) (vals (:a (:admissions step)))))
    (is (= :float-carried (get-in step [:admissions :c :admission])))
    (is (false? (:exactly-normalized-inputs? step)))))

(deftest producer-ordering-decided-by-disjoint-enclosures
  ;; The runtime scorer's RANKING (rank-cascade-actions, float G) must be the
  ;; ranking the exact enclosures decide: per policy, total-g over the same
  ;; rollout-produced schedule, enclosures pairwise disjoint and in the same
  ;; order as the float ranking.
  (let [a (a-carrier)
        q0 (m/observed-belief #{})
        horizons 2
        cands [{:kind :cascade-candidate :id :none :precedence []}
               {:kind :cascade-candidate :id :to-t0 :precedence [produce-t0]}
               {:kind :cascade-candidate :id :to-t1 :precedence [produce-t1]}]
        ranked (efe/rank-actions {:cascade-belief q0} cands
                                 {:horizon-steps horizons
                                  :cascade-spec spec
                                  :adjudication-rates rates})
        float-order (mapv :cascade-id ranked)
        enclosure (fn [cand]
                    (let [prec (constantly (:precedence cand))
                          ;; step-g results carry their own :inputs; total-g
                          ;; re-scores the INPUTS over the declared schedule
                          inputs (mapv (fn [tau]
                                         (:inputs (cg-step (m/rollout prec q0 tau) a tau)))
                                       (range 1 (inc horizons)))]
                      (:g (cg/total-g {:schedule (vec (range 1 (inc horizons)))
                                       :steps inputs}))))
        with-g (mapv (fn [cand]
                       (let [[lo hi] (enclosure cand)]
                         {:id (:id cand) :lo (double lo) :hi (double hi)}))
                     cands)
        by-float (sort-by :hi with-g)
        exact-order (mapv :id by-float)]
    (is (= 3 (count ranked)))
    (is (= exact-order float-order)
        (str "exact-enclosure order " exact-order " = float order " float-order))
    ;; the enclosures actually DECIDE the order (pairwise disjoint, tight)
    (doseq [[x y] (partition 2 1 (sort-by :lo with-g))]
      (is (< (:hi x) (:lo y))
          (str "enclosures of " (:id x) " and " (:id y) " are disjoint")))
    (doseq [e with-g]
      (is (< (- (:hi e) (:lo e)) 1e-11) (str (:id e) " total enclosure tight")))))
