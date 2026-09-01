(ns futon2.aif.policy-precision-test
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.policy-precision :as policy-precision]))

(def ^:private pre-log-prior-revision
  "The last commit that changed policy_precision.clj before I1(b2a) — RUN7's
   dark-beta carry. PINNED, and named for what it is.

   It was `e9bcf35`, which holds byte-identical content for this file and so
   gave the right answer, but is `P-organise-the-library` from an unrelated
   lane: whatever happened to be HEAD when the work started. That is the moving
   anchor the RUN5 review found and 039b0b8 fixed for the sibling controls
   (`trace_test.clj` pins 183749a) — arriving again as \"HEAD at dispatch\"
   rather than \"HEAD~\". The claim this control makes is *I1(b2a) did not move
   the default path RUN7 left*, so the anchor has to be RUN7's commit, and a
   reader has to be able to see that without a git archaeology detour."
  "039b0b8")

(defn- previous-converge-beta
  [beta-prior g-values f-pi-values opts]
  (let [{:keys [exit out err]}
        (shell/sh "git" "show"
                  (str pre-log-prior-revision
                       ":src/futon2/aif/policy_precision.clj"))]
    (when-not (zero? exit)
      (throw (ex-info "could not load pre-log-prior solver" {:err err})))
    (load-string
     (str/replace-first out
                        "(ns futon2.aif.policy-precision"
                        "(ns futon2.aif.policy-precision-before-log-priors"))
    ((ns-resolve 'futon2.aif.policy-precision-before-log-priors 'converge-beta)
     beta-prior g-values f-pi-values opts)))

(defn- close?
  [x y tolerance]
  (<= (Math/abs (- (double x) (double y))) tolerance))

(defn- test-softmax
  [scores]
  (let [maximum (apply max scores)
        exponentials (mapv #(Math/exp (- (double %) maximum)) scores)
        total (reduce + exponentials)]
    (mapv #(/ % total) exponentials)))

(defn- arm-residual
  [beta beta-prior g-values f-pi-values log-priors placement]
  (let [gamma (/ 1.0 beta)
        pi (test-softmax
            (mapv (fn [e f g] (+ e (- f) (- (* gamma g))))
                  log-priors f-pi-values g-values))
        pi-0 (test-softmax
              (mapv (fn [e g]
                      (+ (if (= :both placement) e 0.0) (- (* gamma g))))
                    log-priors g-values))
        policy-error (reduce + (map (fn [p p0 g] (* (- p p0) g))
                                    pi pi-0 g-values))]
    (+ (- beta-prior beta) policy-error)))

(defn- thrown-error
  [f]
  (try
    (f)
    nil
    (catch clojure.lang.ExceptionInfo e
      (:error (ex-data e)))))

(deftest converges-on-real-field-shaped-input-test
  ;; Shape copied from the measured WM field: 110 aligned candidates, rather
  ;; than a two-policy toy. Values span the same order of magnitude as G-total
  ;; and the horizon-one Gaussian F_pi measurements.
  (let [g-values (mapv #(+ 0.25 (* 0.004 %)) (range 110))
        f-pi-values (mapv #(+ -18.0 (* 0.03 (mod % 17))) (range 110))
        result (policy-precision/converge-beta 1.0 g-values f-pi-values)]
    (is (:converged? result))
    (is (false? (:hit-bound? result)))
    (is (<= (Math/abs (:fixed-point-residual result)) 1.0e-9))
    (is (= 110 (count (:pi result))))
    (is (= 110 (count (:pi-0 result))))))

(deftest default-path-is-bit-identical-to-pre-log-prior-solver-test
  (let [g-values (mapv #(+ 0.25 (* 0.004 %)) (range 110))
        f-pi-values (mapv #(+ -18.0 (* 0.03 (mod % 17))) (range 110))
        before (previous-converge-beta 1.0 g-values f-pi-values {})
        after (policy-precision/converge-beta 1.0 g-values f-pi-values {})]
    (is (= before (dissoc after :log-prior-placement))
        "all pre-existing keys and numeric values are bit-identical")
    (is (= :none (:log-prior-placement after)))
    (is (= (conj (set (keys before)) :log-prior-placement)
           (set (keys after))))))

(deftest uniform-log-prior-is-a-no-op-test
  (let [g-values [0.1 0.6 1.4 2.0]
        f-pi-values [-2.0 -1.7 -1.1 -0.8]
        log-priors (vec (repeat 4 3.7))
        none (policy-precision/converge-beta 1.0 g-values f-pi-values)]
    (doseq [placement [:pi :both]]
      (let [placed (policy-precision/converge-beta
                    1.0 g-values f-pi-values
                    {:log-priors log-priors
                     :log-prior-placement placement})]
        (is (close? (:beta-posterior none) (:beta-posterior placed) 1.0e-12))
        (is (close? (:gamma none) (:gamma placed) 1.0e-12))))))

(deftest nonuniform-log-prior-shift-invariance-test
  (let [g-values [0.1 0.6 1.4 2.0]
        f-pi-values [-2.0 -1.7 -1.1 -0.8]
        log-priors [-1.2 0.4 1.1 -0.3]
        shifted (mapv #(+ 8.25 %) log-priors)]
    (doseq [placement [:pi :both]]
      (let [run #(policy-precision/converge-beta
                  1.0 g-values f-pi-values
                  {:log-priors % :log-prior-placement placement})]
        (is (close? (:beta-posterior (run log-priors))
                    (:beta-posterior (run shifted)) 1.0e-12))))))

(deftest three-log-prior-arms-are-distinct-on-field-shaped-input-test
  (let [g-values (mapv #(+ 0.25 (* 0.004 %)) (range 110))
        f-pi-values (mapv #(+ -18.0 (* 0.03 (mod % 17))) (range 110))
        log-priors (mapv #(* 0.07 (mod (* 13 %) 19)) (range 110))
        run (fn [placement]
              (policy-precision/converge-beta
               1.0 g-values f-pi-values
               (if (= :none placement)
                 {}
                 {:log-priors log-priors
                  :log-prior-placement placement})))
        betas (mapv (comp :beta-posterior run) [:none :pi :both])]
    (is (= 3 (count (distinct betas))))))

(deftest returned-beta-zeroes-its-own-log-prior-residual-test
  (let [beta-prior 1.0
        g-values [0.2 1.7]
        f-pi-values [-1.3 -0.2]
        log-priors [-0.8 0.9]
        tolerance 1.0e-10]
    (doseq [placement [:pi :both]]
      (let [result (policy-precision/converge-beta
                    beta-prior g-values f-pi-values
                    {:log-priors log-priors
                     :log-prior-placement placement
                     :tolerance tolerance})]
        (is (:converged? result))
        (is (<= (Math/abs
                 (arm-residual (:beta-posterior result) beta-prior
                               g-values f-pi-values log-priors placement))
                tolerance))))))

(deftest gradient-and-bisect-agree-with-log-priors-test
  (let [g-values (mapv #(+ 0.25 (* 0.004 %)) (range 110))
        f-pi-values (mapv #(+ -18.0 (* 0.03 (mod % 17))) (range 110))
        log-priors (mapv #(* 0.03 (mod (* 7 %) 23)) (range 110))
        opts {:log-priors log-priors :log-prior-placement :pi
              :max-iterations 200000}
        bisect (policy-precision/converge-beta
                1.0 g-values f-pi-values (assoc opts :solver :bisect))
        gradient (policy-precision/converge-beta
                  1.0 g-values f-pi-values (assoc opts :solver :gradient))]
    (is (:converged? bisect))
    (is (:converged? gradient))
    (is (close? (:beta-posterior bisect)
                (:beta-posterior gradient) 1.0e-6))))

(deftest log-prior-validation-is-loud-test
  (let [solve #(policy-precision/converge-beta 1.0 [0.0 1.0] [0.0 0.5] %)]
    (is (= :log-priors-with-no-placement
           (thrown-error #(solve {:log-priors [0.0 1.0]}))))
    (is (= :missing-log-priors
           (thrown-error #(solve {:log-prior-placement :pi}))))
    (is (= :invalid-log-prior-placement
           (thrown-error #(solve {:log-prior-placement :elsewhere}))))
    (is (= :length-mismatch
           (thrown-error #(solve {:log-prior-placement :both
                                  :log-priors [0.0]}))))
    (is (= :non-finite-vector
           (thrown-error #(solve {:log-prior-placement :pi
                                  :log-priors [0.0 ##Inf]}))))))

(deftest beta-floor-is-explicit-test
  (let [result (policy-precision/converge-beta
                0.1 [0.0 0.2] [0.0 100.0]
                ;; the floor is a CLAMP only under the stepping solver; under
                ;; :bisect the floor is the lower bracket end and is never hit
                {:solver :gradient
                 :step-size 0.25 :max-iterations 1 :beta-floor 1.0e-5})]
    (is (:beta-floor-hit? result))
    (is (pos? (:beta-posterior result)))
    (is (pos? (:gamma result)))
    (is (Double/isFinite (:gamma result)))))

(deftest iteration-bound-is-reported-test
  (let [result (policy-precision/converge-beta
                1.0 [0.0 1.0] [0.0 4.0]
                {:solver :gradient :max-iterations 0 :tolerance 0.0})]
    (is (false? (:converged? result)))
    (is (:hit-bound? result))
    (is (= 0 (:iterations result)))))

(deftest beta-zero-is-not-defaulted-test
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"beta-prior is required"
       (apply policy-precision/converge-beta
              [nil [0.0 1.0] [0.0 0.0]])))
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"beta-prior is required"
       (policy-precision/converge-beta 0.0 [0.0 1.0] [0.0 0.0]))))

(deftest higher-gamma-sharpens-the-efe-only-policy-distribution-test
  (let [sharp (policy-precision/converge-beta 0.5 [0.0 1.0] [0.0 0.0])
        diffuse (policy-precision/converge-beta 2.0 [0.0 1.0] [0.0 0.0])]
    (testing "with equal F_pi, higher gamma puts more mass on lower G"
      (is (> (:gamma sharp) (:gamma diffuse)))
      (is (> (first (:pi sharp)) (first (:pi diffuse)))))))

;; ---------------------------------------------------------------------------
;; Owner review (claude-20, 2026-09-01). The delivered measurement reported
;; beta_0 = 2.0 as "did not converge"; it converges at 258 iterations, two past
;; the then-default bound of 256, with the same gamma to eight decimals. What
;; the field actually shows is that the iteration count grows steeply with
;; beta, and that the step size has a stability boundary that moves with beta.

(deftest bound-two-short-is-not-non-convergence-test
  (let [g-values (mapv #(+ 0.25 (* 0.004 %)) (range 110))
        f-pi-values (mapv #(+ -18.0 (* 0.03 (mod % 17))) (range 110))
        truncated (policy-precision/converge-beta
                   1.0 g-values f-pi-values {:max-iterations 3})
        full (policy-precision/converge-beta 1.0 g-values f-pi-values)]
    (testing "a bound set below the required count reports :converged? false"
      (is (false? (:converged? truncated)))
      (is (:hit-bound? truncated)))
    (testing "and the same input converges once the bound allows it"
      (is (:converged? full))
      (is (> (:iterations full) (:iterations truncated))))
    (testing "the truncated gamma is close but NOT equal — so a caller that
              ignores :converged? gets a plausible wrong number, which is why
              the flag is data rather than a log line"
      (is (not= (:gamma truncated) (:gamma full))))))

(deftest beta-ceiling-catches-upward-divergence-test
  ;; Under :solver :gradient only -- :bisect has no step size to destabilise.
  ;; beta -> 0 gives infinite gamma and is floored. beta -> infinity gives
  ;; gamma -> 0 and a near-uniform pi, which looks like an answer. Measured on
  ;; the real-shaped field at :step-size 4.0, beta reached 2e12 and gamma
  ;; 5e-13 without any exception, because those values are finite.
  (let [g-values (mapv #(+ 0.25 (* 0.004 %)) (range 110))
        f-pi-values (mapv #(+ -18.0 (* 0.03 (mod % 17))) (range 110))
        result (policy-precision/converge-beta
                0.5 g-values f-pi-values
                {:solver :gradient
                 :step-size 4.0 :max-iterations 5000 :beta-ceiling 1.0e6})]
    (is (:beta-ceiling-hit? result)
        "upward divergence is recorded, not left as a plausible small gamma")
    (is (pos? (:beta-ceiling-hit-count result)))
    (is (false? (:converged? result)))
    (is (<= (:beta-posterior result) 1.0e6))))

(deftest beta-ceiling-must-exceed-the-floor-test
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"beta-ceiling"
       (policy-precision/converge-beta
        1.0 [0.0 1.0] [0.0 0.0] {:beta-floor 1.0 :beta-ceiling 0.5}))))

(deftest bisect-and-gradient-agree-and-bisect-is-flat-in-beta-test
  ;; claude-1's finding: the 1/beta^2 slowdown is the stepping solver's, not the
  ;; fixed point's. Bisection has no step size, so no stability boundary either.
  (let [g-values (mapv #(+ 0.25 (* 0.004 %)) (range 110))
        f-pi-values (mapv #(+ -18.0 (* 0.03 (mod % 17))) (range 110))
        run (fn [bp solver]
              (policy-precision/converge-beta
               bp g-values f-pi-values
               {:solver solver :max-iterations 200000}))]
    (testing "the two solvers find the same root"
      (doseq [bp [0.5 1.0 2.0]]
        (let [b (run bp :bisect) g (run bp :gradient)]
          (is (:converged? b))
          (is (:converged? g))
          (is (< (Math/abs (- (:gamma b) (:gamma g))) 1.0e-6)
              (str "solvers disagree at beta_prior " bp)))))
    (testing "bisection's cost does not grow with beta; the gradient's does"
      (let [b-small (run 0.5 :bisect) b-large (run 50.0 :bisect)
            g-small (run 0.5 :gradient) g-large (run 50.0 :gradient)]
        (is (< (:iterations b-large) (* 2 (:iterations b-small))))
        (is (> (:iterations g-large) (* 100 (:iterations g-small))))))))

(deftest bisect-reports-an-unbracketed-root-rather-than-a-midpoint-test
  ;; If the residual ever stops being monotone on the bracket, halving still
  ;; returns a number. It must say it found no root instead.
  (let [result (policy-precision/converge-beta
                1.0 [0.0 1.0] [0.0 0.0]
                {:solver :bisect :beta-floor 10.0 :beta-ceiling 1.0e6})]
    (is (false? (:bracketed? result))
        "floor above the root means both ends share a sign")
    (is (false? (:converged? result)))
    (is (some? (:residual-at-floor result)))
    (is (some? (:residual-at-ceiling result)))))

;; ---------------------------------------------------------------------------
;; RUN7 / stage S2 -- the carry lifecycle

(defn- ranked [identity score]
  {:action {:type :address-sorry :target identity}
   :controller-score score})

(def ^:private identity-of (comp :target :action))

(defn- field
  "n candidates with a present F_pi each, and this tick's G beside them."
  [n]
  {:by-candidate-id
   (into {} (for [i (range n)]
              [(str "rank/" (inc i))
               {:candidate-identity (keyword (str "c" i))
                :status :present
                :value (+ -18.0 (* 0.03 (mod i 17)))}]))
   :ranked (mapv #(ranked (keyword (str "c" %)) (+ 0.25 (* 0.004 %))) (range n))})

(deftest carry-solves-and-hands-the-posterior-to-the-next-tick-test
  (let [{:keys [by-candidate-id ranked]} (field 110)
        first-tick (policy-precision/carry-beta nil by-candidate-id ranked
                                                {:identity-fn identity-of})]
    (is (= :present (:status first-tick)))
    (is (= 110 (:f-pi-present-count first-tick)))
    (is (= :converged-posterior (:beta-source first-tick)))
    (is (= 1 (:solved-tick-count first-tick)))
    (testing "the acceptance's five quantities are on the record as data"
      (let [solve (:solve first-tick)]
        (is (number? (:beta-posterior solve)))
        (is (number? (:gamma solve)))
        (is (integer? (:iterations solve)))
        (is (contains? solve :converged?))
        (is (contains? solve :bracketed?))))
    (testing "gamma is 1/beta, so the persisted pair cannot drift apart"
      (is (< (Math/abs (- (:gamma (:solve first-tick))
                          (/ 1.0 (:beta-posterior (:solve first-tick)))))
             1.0e-12)))
    (testing "the distributions are dropped rather than persisted per tick"
      (is (not (contains? (:solve first-tick) :pi)))
      (is (not (contains? (:solve first-tick) :pi-0))))
    (testing "the next tick's prior IS this tick's posterior (worklist J4)"
      (let [second-tick (policy-precision/carry-beta
                         first-tick by-candidate-id ranked
                         {:identity-fn identity-of})]
        (is (= (:beta first-tick) (:beta-prior (:solve second-tick))))
        (is (= 2 (:solved-tick-count second-tick)))))))

(deftest carry-holds-rather-than-resets-when-the-field-is-absent-test
  (let [{:keys [by-candidate-id ranked]} (field 110)
        solved (policy-precision/carry-beta nil by-candidate-id ranked
                                            {:identity-fn identity-of})
        held (policy-precision/carry-beta solved {} ranked
                                          {:identity-fn identity-of})]
    (is (= :absent (:status held)))
    (is (= :empty-f-pi-readback (:reason held)))
    (is (= (:beta solved) (:beta held)) "the carried beta is held, not reset")
    (is (= :held-absent (:beta-source held)))
    (is (not (contains? held :solve))
        "no stale solve rides forward to be read as this tick's")
    (is (= 1 (:solved-tick-count held)))))

(deftest a-tick-that-never-solved-is-on-beta-0-not-holding-test
  (let [held (policy-precision/carry-beta nil {} [] {:identity-fn identity-of})]
    (is (= :initial (:beta-source held))
        "held-absent would imply a solve behind a beta that beta_0 put there")
    (is (= policy-precision/default-initial-beta (:beta held)))
    (is (= 0 (:solved-tick-count held)))))

(deftest unsolved-holds-and-persists-the-failed-solve-test
  (testing "an unbracketed root holds beta and keeps its diagnostics"
    (let [{:keys [by-candidate-id ranked]} (field 110)
          ;; a bracket that cannot straddle the root: both ends above it
          result (policy-precision/carry-beta
                  {:beta 2.0 :solved-tick-count 3} by-candidate-id ranked
                  {:identity-fn identity-of
                   :beta-floor 1.0e5 :beta-ceiling 1.0e6})]
      (is (= :present (:status result)) "the tick DID compute; it did not solve")
      (is (false? (:bracketed? (:solve result))))
      (is (= :held-unsolved (:beta-source result)))
      (is (= 2.0 (:beta result)) "an unsolved tick carries its prior forward")
      (is (= 3 (:solved-tick-count result)) "and does not count as a solve")
      (is (contains? (:solve result) :residual-at-floor)
          "the failed solve is DATA, not a discarded exception"))))

(deftest absent-and-unjoinable-candidates-are-counted-not-dropped-test
  (let [{:keys [ranked]} (field 4)
        mixed {"rank/1" {:candidate-identity :c0 :status :present :value -1.0}
               "rank/2" {:candidate-identity :c1 :status :absent
                         :reason :missing-prediction-details}
               ;; identity present in the readback but gone from this tick
               "rank/3" {:candidate-identity :vanished :status :present :value -2.0}
               ;; a non-numeric F_pi is an absence, not a zero
               "rank/4" {:candidate-identity :c2 :status :present :value nil}}
        result (policy-precision/carry-beta nil mixed ranked
                                            {:identity-fn identity-of})]
    (is (= 1 (:f-pi-present-count result)))
    (is (= 3 (:f-pi-absent-count result)))
    (is (= 1 (:candidate-count (:solve result))))))

(deftest carried-state-is-guarded-not-inherited-test
  (testing "the R14 v0 lesson: a malformed carried state reconstructs the prior"
    (doseq [bad [nil {} {:beta 0.0} {:beta -1.0} {:beta ##NaN} {:beta "1.0"} :not-a-map]]
      (let [coerced (policy-precision/coerce-state bad)]
        (is (= policy-precision/default-initial-beta (:beta coerced))
            (str "reset for " (pr-str bad)))
        (is (= :initial (:beta-source coerced)))
        (is (= 0 (:solved-tick-count coerced))))))
  (testing "and a well-formed one is passed through untouched"
    (let [good {:beta 1.5 :beta-source :converged-posterior :solved-tick-count 7}]
      (is (= good (policy-precision/coerce-state good))))))

(deftest the-join-identity-is-required-rather-than-defaulted-test
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #":identity-fn is required"
       (policy-precision/carry-beta nil {"rank/1" {:status :present :value -1.0}}
                                    [] {}))
      "a defaulted identity would return :no-aligned-candidates every tick"))

(deftest g-is-the-controller-score-selection-itself-uses-test
  (let [by-id {"rank/1" {:candidate-identity :c0 :status :present :value -1.0}
               "rank/2" {:candidate-identity :c1 :status :present :value -2.0}}
        sharp (policy-precision/carry-beta
               nil by-id [(ranked :c0 0.0) (ranked :c1 4.0)]
               {:identity-fn identity-of})
        flat (policy-precision/carry-beta
              nil by-id [(ranked :c0 0.0) (ranked :c1 0.0)]
              {:identity-fn identity-of})]
    (is (not= (:beta sharp) (:beta flat))
        "changing only :controller-score moves the solved beta, so G is read
         from the field selection uses and not from a constant")))
