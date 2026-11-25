(ns futon2.aif.cascade-coapply-g-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-coapply-g :as g]
            [futon2.aif.cascade-observation-scoring :as scorer]
            [futon2.aif.cascade-policy :as policy]
            [futon2.aif.cascade-shape-g :as shape]
            [futon2.aif.want-free-energy :as wfe]))

(defn pattern [id needs produces theta]
  (assoc (policy/token-interpretation id {:guard {:needs needs :forbids produces}
                                         :produces produces}) :theta theta))
(defn candidate [id patterns descent]
  {:kind :cascade-candidate :id id :target "T"
   :precedence {:co-apply {:units (vec (keys patterns)) :descent descent :patterns patterns}}})
(def a ["T" :a])
(def b ["T" :b])
(def c ["T" :c])
(def join (candidate :join (array-map :a (pattern :a #{} #{a} 1)
                                      :b (pattern :b #{} #{b} 1)
                                      :c (pattern :c #{a b} #{c} 1)) []))
(def counts {:a [1 1] :b [1 1] :c [1 1]})
(defn opts [h counts-map]
  {:horizon-steps h :pattern-counts counts-map
   :observation-model (shape/class-emission-model "T" #{a b c} #{c} h :focused
                      {:status :synthetic :source "Lean co-application fixture"})
   :prediction-context {:occurrence-id "test-only" :tau h}
   :observation {:status :observed :occurrence-id "test-only" :tau h :present #{c} :absent #{}}
   :cascade-spec {:want #{c} :evidence #{} :zeroed #{}}})
(defn close? [a b] (< (Math/abs (- (double a) (double b))) 1e-10))

(deftest frontier-and-shared-horizon
  (let [chain (assoc-in join [:precedence :co-apply :descent] [[:a :b]])
        chain (assoc chain :id :chain)
        ranked (g/compare-at-horizon {:cascade-belief {#{} 1}} [join chain]
                                    (opts 3 {:join counts :chain counts}))]
    (is (= 1/4 (g/expected-novelty chain counts {#{} 1})))
    (is (= [1/4 1/4 1/4]
           (mapv :expected-novelty
                 (get-in (first (filter #(= :chain (:cascade-id %)) ranked))
                         [:certificate :parameter-novelty :steps]))))
    (is (vector? ranked) (pr-str ranked))
    (is (= #{3} (set (map :horizon-steps ranked))))
    (doseq [e ranked]
      (is (= :none (get-in e [:certificate :g-terms :normalization])))
      (is (= 3 (count (get-in e [:certificate :parameter-novelty :steps])))))
    (let [e (first (filter #(= :join (:cascade-id %)) ranked))
          {:keys [risk ambiguity expected-information-gain]} (get-in e [:certificate :g-terms])]
      (is (= {#{a b} 1} (get-in e [:certificate :steps 0 :belief])))
      (is (= [1/2 1/4 0] (mapv :expected-novelty (get-in e [:certificate :parameter-novelty :steps]))))
      (is (= 3/4 expected-information-gain))
      (is (close? (:G-efe e) (- (+ risk ambiguity) 3/4))))))

(deftest state-weighting-and-no-credit-for-disabled-patterns
  (let [q {#{} 1/2 #{a b} 1/2}]
    (is (= 3/8 (g/expected-novelty join counts q)))
    (is (= 1/2 (g/expected-novelty join (assoc counts :c [100 100]) {#{} 1})))
    (is (> (g/expected-novelty join counts {#{} 1})
           (g/expected-novelty join (assoc counts :a [2 1]) {#{} 1})))
    (is (= 0 (g/expected-novelty join counts {#{a b c} 1}))))
  (let [stochastic (assoc-in join [:precedence :co-apply :patterns :a :theta] 1/2)
        result (g/compare-at-horizon {:cascade-belief {#{} 1}} [stochastic] (opts 2 {:join counts}))]
    (is (vector? result) (pr-str result))
    (is (= {#{b} 1/2 #{a b} 1/2} (get-in result [0 :certificate :steps 0 :belief])))
    (is (= [1/2 1/4] (mapv :expected-novelty (get-in result [0 :certificate :parameter-novelty :steps]))))))

(deftest counts-change-g-with-fixed-transition
  (let [older (assoc join :id :older)
        rs (g/compare-at-horizon {:cascade-belief {#{} 1}} [join older]
                                (opts 2 {:join counts :older (zipmap (keys counts) (repeat [2 2]))}))]
    (is (vector? rs) (pr-str rs))
    (is (= :join (:cascade-id (first rs))))
    (is (< (:G-efe (first rs)) (:G-efe (second rs))))))

(deftest refuses-unsupported-or-incoherent-inputs
  (doseq [[candidate options reason]
          [[join (opts 2 {}) :pattern-counts-missing]
           [join (opts 2 {:join (assoc counts :a [0 1])}) :invalid-pattern-counts]
           [join (opts 2 {:join (assoc counts :a :bad)}) :invalid-pattern-counts]
           [join (assoc (opts 2 {:join counts}) :g-normalization :per-step-capacity-and-pattern) :conflicting-score-law]
           [(assoc-in join [:precedence :co-apply :descent] [[:a :b] [:b :a]])
            (opts 2 {:join counts}) :cyclic-containment]
           [(assoc join :precedence []) (opts 2 {:join counts}) :invalid-co-application]]]
    (is (= reason (let [r (g/compare-at-horizon {:cascade-belief {#{} 1}} [candidate] options)]
                      (or (get-in r [:candidates 0 :refusal :reason]) (:reason r))))))
  (is (= :invalid-pattern-counts
         (try (g/beta-novelty [Double/NaN 1]) (catch clojure.lang.ExceptionInfo e (:reason (ex-data e)))))))

(deftest unequal-lengths-use-one-horizon-and-raw-epoch-sum
  (let [short (candidate :short {:c (pattern :c #{} #{c} 1)} [])
        rs (g/compare-at-horizon {:cascade-belief {#{} 1}} [join short]
                                (opts 3 {:join counts :short {:c [1 1]}}))]
    (is (vector? rs) (pr-str rs))
    (is (= [3 3] (mapv :horizon-steps rs)))
    (doseq [e rs]
      (is (close? (:G-efe e) (reduce + (map :g (get-in e [:certificate :steps]))))))))

(deftest inadmissible-risk-refuses-instead-of-numeric-score
  (let [options (assoc-in (opts 2 {:join counts})
                         [:observation-model :class-preference 2]
                         {:focused 0 :related 1 :unrelated 0 :stop-the-line 0})
        result (g/compare-at-horizon {:cascade-belief {#{} 1}} [join] options)]
    (is (= :inadmissible-risk (get-in result [:candidates 0 :refusal :kind])) (pr-str result))
    (is (not (contains? result :G-efe)))))

(deftest prediction-does-not-require-a-future-observation
  (let [options (dissoc (opts 2 {:join counts}) :observation)
        rs (g/compare-at-horizon {:cascade-belief {#{} 1}} [join] options)
        impossible (g/compare-at-horizon {:cascade-belief {#{} 1}} [join]
                     (assoc options :observation {:status :observed :present #{} :absent #{c}
                                                  :occurrence-id "test-only" :tau 2}))]
    (is (vector? rs) (pr-str rs))
    (is (= (:G-efe (first rs)) (:G-efe (first impossible))))
    (is (= {:status :not-requested :reason :prediction-only-score} (:inference (first rs))))
    (is (not (contains? (first rs) :posterior)))
    ;; 980b78939 (2026-10-29, "Retain class F absence and impossibility
    ;; outcomes") deliberately retains the :f key; under prediction-only
    ;; scoring its value is nil. The pre-980b78939 expectation asserted the
    ;; key was absent.
    (is (nil? (:f (first rs))))))

(deftest legacy-class-conditioning-remains-unchanged
  ;; 0be247a91 (2026-10-29, "Supply reading-policy class free energy") made
  ;; the class-emission :condition query require an explicit
  ;; :class-free-energy history in the prediction context (empty retained
  ;; history computes F = 0.0, as in class-observation-scoring-test's
  ;; empty-retained-history-closes-q7-f-path); this fixture supplies it.
  (let [state {:cascade-belief {#{} 1}}
        options (assoc-in (opts 2 {:join counts})
                          [:prediction-context :class-free-energy]
                          {:policy-key :test-policy :window 0 :sources []})
        missing (scorer/rank-cascade-actions state [join] (dissoc options :observation))
        matched (scorer/rank-cascade-actions state [join] options)]
    (is (vector? missing) (pr-str missing))
    (is (= :computed (get-in missing [0 :f :status])))
    (is (vector? matched) (pr-str matched))
    (is (= :computed (get-in matched [0 :inference :status])))
    (is (contains? (first matched) :posterior))))

(deftest one-refusal-does-not-suppress-an-admissible-candidate
  (let [uc ["U" :c]
        other (assoc (candidate :other {:c (pattern :c #{} #{uc} 1)} []) :target "U")
        options (-> (opts 2 {:join counts :other {:c [1 1]}})
                    (assoc-in [:observation-model :universe] #{a b c uc})
                    (assoc-in [:observation-model :acceptance] #{c uc})
                    (assoc-in [:observation-model :target-class] {"T" :focused "U" :related})
                    (assoc-in [:observation-model :class-preference 2]
                              {:focused 0 :related 1 :unrelated 0 :stop-the-line 0}))
        rs (g/compare-at-horizon {:cascade-belief {#{} 1}} [join other] options)]
    (is (vector? rs) (pr-str rs))
    (is (= :other (:cascade-id (first rs))))
    (is (= 1 (:rank (first rs))))
    (is (= :refused (:status (second rs))))
    (is (= :inadmissible-risk (get-in rs [1 :refusal :kind])))
    (is (not-any? #(contains? (second rs) %) [:G-efe :rank :controller-score])))
  (doseq [bad [(assoc-in join [:precedence :co-apply :descent] [[:a :b] [:b :a]])
               (assoc join :precedence [])]]
    (let [good (assoc join :id :good)
          rs (g/compare-at-horizon {:cascade-belief {#{} 1}} [bad good]
                                  (opts 2 {:join counts :good counts}))]
      (is (= :good (:cascade-id (first rs))))
      (is (= :refused (:status (second rs)))))))

(deftest invalid-shared-input-is-not-a-candidate-refusal
  (doseq [[state options kind]
          [[{:cascade-belief {#{} 1}} (assoc (opts 2 {:join counts}) :horizon-steps 3
                                           :prediction-context {:occurrence-id "x" :tau 3}) :class-horizon-mismatch]
           [{:cascade-belief {#{} 1}} (assoc-in (opts 2 {:join counts}) [:prediction-context :tau] 3)
            :missing-prediction-context]
           [{:cascade-belief {#{} 2}} (opts 2 {:join counts}) :invalid-state-belief]]]
    (let [r (g/compare-at-horizon state [join] options)]
      (is (= kind (:kind r)) (pr-str r))
      (is (not= :no-admissible-candidate (:reason r)))
      (is (not (contains? r :candidates))))))

(defn per-candidate-opts [h candidates models]
  (-> (opts h (into {} (map (fn [candidate]
                              [(:id candidate) (zipmap (keys (get-in candidate [:precedence :co-apply :patterns]))
                                                       (repeat [1 1]))])
                            candidates)))
      (dissoc :observation-model)
      (assoc :observation-model-of models)))

(deftest different-targets-use-their-own-class-model-at-one-ruled-horizon
  (let [u-token ["U" :done]
        other (assoc (candidate :other {:done (pattern :done #{} #{u-token} 1)} []) :target "U")
        model-t (shape/class-emission-model "T" #{a b c} #{c} 2)
        model-u (shape/class-emission-model "U" #{u-token} #{u-token} 2)
        options (assoc (per-candidate-opts 2 [join other] {:join model-t :other model-u})
                       :require-ruled-class-kernel? true)
        result (g/compare-at-horizon {:cascade-belief {#{} 1}} [join other] options)]
    (is (vector? result) (pr-str result))
    (is (= #{:join :other} (set (map :cascade-id result))))
    (is (= #{1 2} (set (map :rank result))))
    (is (= #{model-t model-u} (set (map :observation-model result))))
    (is (= :compareAtHorizonN (:lean-reduction (meta result))))))

(deftest per-candidate-model-family-refusals-are-distinct-from-candidate-refusals
  (let [other (assoc join :id :other)
        model-2 (shape/class-emission-model "T" #{a b c} #{c} 2)
        model-3 (shape/class-emission-model "T" #{a b c} #{c} 3)
        base (per-candidate-opts 2 [join other] {:join model-2 :other model-2})
        unequal (assoc-in model-2 [:class-preference 2]
                          {:focused 0 :related 1 :unrelated 0 :stop-the-line 0})]
    (doseq [[options reason]
            [[(assoc base :observation-model-of {:join model-2 :other model-3})
              :class-horizon-mismatch]
             [(assoc base :observation-model-of {:join model-2 :other unequal})
              :class-preference-not-shared]
             [(assoc base :observation-model-of {:join unequal :other unequal}
                          :require-ruled-class-kernel? true)
              :class-preference-not-ruled]]]
      (let [result (g/compare-at-horizon {:cascade-belief {#{} 1}} [join other] options)]
        (is (= reason (or (:reason result) (:kind result))) (pr-str result))
        (is (not= :no-admissible-candidate (:reason result)))
        (is (not (contains? result :candidates)))))))

(deftest missing-per-candidate-model-refuses-only-that-candidate
  (let [other (assoc join :id :other)
        model-2 (shape/class-emission-model "T" #{a b c} #{c} 2)
        options (per-candidate-opts 2 [join other] {:join model-2})
        result (g/compare-at-horizon {:cascade-belief {#{} 1}} [join other] options)]
    (is (vector? result) (pr-str result))
    (is (= :join (:cascade-id (first result))))
    (is (= 1 (:rank (first result))))
    (is (= :refused (:status (second result))))
    (is (= :observation-model-missing (get-in result [1 :refusal :reason])))
    (is (= :scoreCascadeCoN-map (:lean-reduction (meta result))))))

(deftest empty-model-map-refuses-the-family
  (let [options (-> (opts 2 {:join counts}) (dissoc :observation-model)
                    (assoc :observation-model-of {}))]
    (is (= :class-observation-required
           (:reason (g/compare-at-horizon {:cascade-belief {#{} 1}} [join] options))))))

(deftest class-only-comparison-needs-no-token-preference
  (let [options (dissoc (opts 2 {:join counts}) :cascade-spec)
        r (g/compare-at-horizon {:cascade-belief {#{} 1}} [join] options)]
    (is (vector? r) (pr-str r))
    (is (= :class-model-preference (get-in r [0 :certificate :c-source :source]))))
  (is (= :invalid-observation-preference
         (:kind (g/compare-at-horizon {:cascade-belief {#{} 1}} [join]
                                      (assoc (opts 2 {:join counts}) :cascade-spec {:want :bad}))))))

;;; ------------------------------------------------------------------------
;;; G' (want free energy, E-kimi-task-438). The pragmatic term of the
;;; canonical co-application G is the want term -log(chance) with C read as
;;; a want effect; the expected values below are RE-DERIVED from the
;;; reference port futon2.aif.want-free-energy (chance, want-term), which is
;;; certified against the Lean witnesses in WantFreeEnergyWitness.lean (see
;;; holes/labs/wm-contract/runs/G-prime-want-free-energy/clojure-readback.txt).
;;; Where a fixture's prediction is a point mass, G' equals the old
;;; risk-based G by stepG'_eq_stepG_of_point, and the test says so.

(deftest gprime-point-predictions-keep-the-old-g
  ;; The join fixture's thetas are all 1: every prediction is a point mass,
  ;; so G' == the risk-based G per step (stepG'_eq_stepG_of_point) and no
  ;; numeric pin in this namespace moves for these fixtures.
  (let [rs (g/compare-at-horizon {:cascade-belief {#{} 1}} [join] (opts 2 {:join counts}))
        e (first rs)]
    (is (vector? rs) (pr-str rs))
    (is (= :want-free-energy/v1 (:g-law e)))
    (is (every? (fn [step] (== (:want-term step) (:risk step)))
                (get-in e [:certificate :steps])))
    (is (= (:G-efe e) (reduce + (map :g (get-in e [:certificate :steps])))))
    (is (= (/ (:G-efe e) 2) (:g-per-step e)))))

(deftest gprime-chance-and-want-term-are-the-reference-port's
  ;; Horizon 1, one pattern with theta 9/10: the terminal prediction is
  ;; {:focused 9/10 :stop-the-line 1/10} under the ruled C
  ;; {:focused 11/20 :stop-the-line 1/20 ...}. chance is EXACTLY
  ;; 9/10*11/20 + 1/10*1/20 = 1/2, the same shape as
  ;; WantFreeEnergyWitness.chance_nine_in_ten (bern 9/10 under C 9/10:1/10
  ;; giving 41/50); the expected value here is computed by the reference
  ;; port's chance/want-term, not by the wiring under test.
  (let [theta-candidate (candidate :bern {:c (pattern :c #{} #{c} 9/10)} [])
        want-effect {:focused 11/20 :stop-the-line 1/20}
        expected-chance (wfe/chance {:focused 9/10 :stop-the-line 1/10} want-effect)
        expected-want (wfe/want-term {:focused 9/10 :stop-the-line 1/10} want-effect)
        rs (g/compare-at-horizon {:cascade-belief {#{} 1}} [theta-candidate]
                                 (opts 1 {:bern {:c [1 1]}}))
        e (first rs)
        step (first (get-in e [:certificate :steps]))]
    (is (vector? rs) (pr-str rs))
    (is (= 1/2 expected-chance))
    (is (= 1/2 (:chance step)) "chance is the exact rational sum_o Q(o|pi)*c(o)")
    (is (close? expected-want (:want-term step)))
    (let [[lo hi] (:want-term-enclosure step)]
      (is (<= (double lo) (:want-term step) (double hi))
          "cascade-g/log-bounds encloses the reported want term"))
    ;; G' = want + ambiguity(0) - novelty; counts [1 1] give beta-novelty
    ;; 1/4 at the single pre-step belief {#{} 1}.
    (is (close? (- expected-want 1/4) (:G-efe e)))
    (is (close? (:G-efe e) (:g-per-step e)) "H = 1: per-step is the total")
    ;; The reported risk is computed in the same pass and is NOT the want
    ;; term for this non-point prediction (KL(q||C) != -log chance here).
    (is (not (close? (:risk step) (:want-term step))))))

(deftest gprime-hard-want-refuses-never-minus-log-zero
  ;; Planted bad case: C with 0 mass on a predicted outcome is the deferred
  ;; hard-want case. The candidate refuses with the existing
  ;; :inadmissible-risk shape; -log 0 is never produced or ranked.
  (let [options (assoc-in (opts 1 {:bern {:c [1 1]}})
                          [:observation-model :class-preference 1]
                          {:focused 0 :related 1 :unrelated 0 :stop-the-line 0})
        bad (candidate :bern {:c (pattern :c #{} #{c} 9/10)} [])
        result (g/compare-at-horizon {:cascade-belief {#{} 1}} [bad] options)]
    (is (= :inadmissible-risk (get-in result [:candidates 0 :refusal :kind]))
        (pr-str result))
    (is (not (contains? result :G-efe)))))

(deftest gprime-ranking-mutant-check-risk-order-disagrees
  ;; MUTANT CHECK: ranking by the reported :risk instead of :G-efe must FAIL
  ;; this test. Candidate certain accepts for sure (theta 1): point
  ;; prediction {:focused 1}, so want = risk = -ln(11/20)
  ;; (stepG'_eq_stepG_of_point). Candidate bern has theta 9/10: prediction
  ;; {:focused 9/10 :stop-the-line 1/10}, chance exactly 1/2 (see above).
  ;; risk(bern) = KL < risk(certain) (spreading towards C lowers KL), while
  ;; chance(certain) = 11/20 > 1/2 = chance(bern), so G' ranks certain
  ;; first and the reported-:risk order ranks bern first.
  (let [certain (candidate :certain {:c (pattern :c #{} #{c} 1)} [])
        bern (candidate :bern {:c (pattern :c #{} #{c} 9/10)} [])
        options (opts 1 {:certain {:c [1 1]} :bern {:c [1 1]}})
        rs (g/compare-at-horizon {:cascade-belief {#{} 1}} [certain bern] options)
        by-id (into {} (map (juxt :cascade-id identity) rs))
        risk-of (fn [id] (reduce + (map :risk (get-in by-id [id :certificate :steps]))))
        want-of (fn [id] (reduce + (map :want-term (get-in by-id [id :certificate :steps]))))]
    (is (vector? rs) (pr-str rs))
    (is (< (risk-of :bern) (risk-of :certain))
        "the mutant (rank on reported :risk) prefers bern")
    (is (< (want-of :certain) (want-of :bern))
        "G' prefers certain: its chance the wants are met is higher")
    (is (= :certain (:cascade-id (first rs))))
    (is (= 1 (:rank (first rs))))
    (is (= :want-free-energy/v1 (:g-law (first rs))))))
