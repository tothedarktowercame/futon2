(ns futon2.aif.free-energy-test
  "Tests for the War Machine AIF free-energy computation and mode inference."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.free-energy :as fe]
            [futon2.aif.observation :as obs]))

(def ^:private healthy-obs
  "Observation vector that falls within preferred ranges on weighted channels."
  {:loop-health 0.9
   :support-coverage 0.9
   :attack-coverage 0.9
   :mission-health 0.7
   :stack-pct 0.20
   :consulting-pct 0.25
   :portfolio-pct 0.25
   :mathematics-pct 0.20
   :active-repo-ratio 0.8
   :sorry-count-norm 0.1
   :coupling-density 0.2
   :ticks-firing-ratio 0.0
   :depositing-signal 0.1})

(def ^:private hermit-obs
  "Observation matching the hermit avoidance pattern."
  {:loop-health 0.5
   :support-coverage 0.5
   :attack-coverage 0.5
   :mission-health 0.5
   :stack-pct 0.85
   :consulting-pct 0.0
   :portfolio-pct 0.05
   :mathematics-pct 0.10
   :active-repo-ratio 0.5
   :sorry-count-norm 0.2
   :coupling-density 0.1
   :ticks-firing-ratio 0.0
   :depositing-signal 0.0})

(deftest compute-controller-diagnostics-shape-test
  (testing "compute-controller-diagnostics returns the documented keys"
    (let [g (fe/compute-controller-diagnostics {})]
      (is (contains? g :controller-score))
      (is (contains? g :preference-gap-score))
      (is (contains? g :coverage-uncertainty-pressure))
      (is (contains? g :per-channel))
      (is (contains? g :avoided-active))))
  (testing "controller-score respects the 0.65 / 0.35 mix"
    (let [g (fe/compute-controller-diagnostics hermit-obs)
          expected (+ (* 0.65 (:preference-gap-score g))
                      (* 0.35 (:coverage-uncertainty-pressure g)))]
      (is (< (Math/abs (- expected (:controller-score g))) 1e-9)
          "controller-score = 0.65*pragmatic + 0.35*epistemic")))
  (testing "G components are non-negative for in-distribution input"
    (let [g (fe/compute-controller-diagnostics healthy-obs)]
      (is (>= (:preference-gap-score g) 0.0))
      (is (>= (:coverage-uncertainty-pressure g) 0.0))
      (is (>= (:controller-score g) 0.0)))))

(deftest compute-controller-diagnostics-per-channel-test
  (testing "per-channel entries cover every preference key"
    (let [g (fe/compute-controller-diagnostics healthy-obs)
          ch-keys (set (keys (:per-channel g)))]
      (is (every? ch-keys
                  (->> (fe/compute-controller-diagnostics healthy-obs)
                       :per-channel keys set)))))
  (testing "in-range observations yield zero gap"
    (let [g (fe/compute-controller-diagnostics healthy-obs)
          loop-entry (get-in g [:per-channel :loop-health])]
      (is (true? (:in-range? loop-entry)))
      (is (zero? (:gap loop-entry))))))

(deftest absent-controller-input-contributes-no-score-test
  (testing "absence is retained and contributes neither a pragmatic nor epistemic term"
    (let [g (fe/compute-controller-diagnostics (obs/observe {}))]
      (is (zero? (:preference-gap-score g)))
      (is (zero? (:coverage-uncertainty-pressure g)))
      (is (every? #(= :absent (:status %)) (vals (:per-channel g))))
      (is (every? #(= :absent (:status %)) (vals (:epistemic-terms g))))
      (is (empty? (get-in g [:score-support :pragmatic :present])))
      (is (seq (get-in g [:score-support :pragmatic :absent])))))
  (testing "an explicit measured zero remains present evidence"
    (let [g (fe/compute-controller-diagnostics {:loop-health 0.0})]
      (is (= :present (get-in g [:per-channel :loop-health :status])))
      (is (= 0.4 (get-in g [:epistemic-terms :loop-health :weighted-term])))))
  (testing "the full-support scalar decomposition is unchanged"
    (let [g (fe/compute-controller-diagnostics healthy-obs)]
      (is (< (Math/abs (- (:controller-score g)
                          (+ (* 0.65 (:preference-gap-score g))
                             (* 0.35 (:coverage-uncertainty-pressure g)))))
             1e-12)))))

(deftest avoided-active-test
  (testing "healthy observation triggers no avoided ranges"
    (is (empty? (:avoided-active (fe/compute-controller-diagnostics healthy-obs)))))
  (testing "hermit observation triggers stack-pct + consulting-pct avoidance"
    (let [avoided (set (:avoided-active (fe/compute-controller-diagnostics hermit-obs)))]
      (is (contains? avoided :stack-pct)
          "stack-pct 0.85 falls in avoided [0.7 1.0]")
      (is (contains? avoided :consulting-pct)
          "consulting-pct 0.0 falls in avoided [0.0 0.0]"))))

(deftest absent-avoidance-is-unknown-test
  (testing "missing source fields never become satisfied or violated guards"
    (let [g (fe/compute-controller-diagnostics (obs/observe {}))
          verdicts (vals (:avoidance-by-channel g))]
      (is (= 5 (count verdicts)))
      (is (every? #(= :unknown (:status %)) verdicts))
      (is (empty? (:avoided-active g))
          "unknown channels are not smuggled into the violated-only view")))
  (testing "a measured zero remains a real observation"
    (let [g (fe/compute-controller-diagnostics
             (obs/observe {:support-attack {:support-coverage 0.0
                                             :attack-coverage 0.0}
                           :loop-health {:overall 0.0}
                           :mission-triage {:health 0.0}
                           :graph {:dynamics {:commit-percentages
                                              {:stack 0.0 :consulting 0.0
                                               :portfolio 0.0 :mathematics 0.0}
                                             :ticks []}
                                   :summary {:active-repos 0 :total-repos 1
                                             :total-sorrys 0 :coupling-edges 0
                                             :ticks-firing 0}}
                           :frames {:depositing-signal 0.0}
                           :annotation-graph {:health 0.0}}))]
      (is (= :violated
             (get-in g [:avoidance-by-channel :consulting-pct :status])))
      (is (some #{:consulting-pct} (:avoided-active g))))))

(defn- with-alive-defaults
  "Avoid the :dark branch (active < 0.2 AND loop-health < 0.3) for cases
   that want to test other mode classifications."
  [m]
  (merge {:active-repo-ratio 0.5 :loop-health 0.5} m))

(deftest infer-mode-test
  (testing "dark mode: nothing happening"
    (is (= :dark (fe/infer-mode {:active-repo-ratio 0.1 :loop-health 0.2}))))
  (testing "depositing: consulting activity"
    (is (= :depositing
           (fe/infer-mode (with-alive-defaults {:consulting-pct 0.3 :stack-pct 0.4})))))
  (testing "hermit: stack-dominated, no consulting, no depositing"
    (is (= :hermit
           (fe/infer-mode (with-alive-defaults
                            {:stack-pct 0.85 :consulting-pct 0.0 :depositing-signal 0.0})))))
  (testing "scanning: stack-dominated but daily scans active"
    (is (= :scanning
           (fe/infer-mode (with-alive-defaults
                            {:stack-pct 0.8 :consulting-pct 0.0 :depositing-signal 0.05})))))
  (testing "foraging-trapped: stack + ticks firing"
    (is (= :foraging-trapped
           (fe/infer-mode (with-alive-defaults
                            {:stack-pct 0.6 :consulting-pct 0.05 :ticks-firing-ratio 0.6
                             :depositing-signal 0.0})))))
  (testing "multiplied: catch-all when no other branch fires"
    (is (= :multiplied
           (fe/infer-mode {:stack-pct 0.2 :consulting-pct 0.2 :portfolio-pct 0.3
                           :loop-health 0.8 :active-repo-ratio 0.7
                           :ticks-firing-ratio 0.0 :depositing-signal 0.0})))))

(deftest pipeline-integration-test
  (testing "observe → compute-controller-diagnostics composes for empty scan data"
    (let [o (obs/observe {})
          g (fe/compute-controller-diagnostics o)]
      (is (map? g))
      (is (number? (:controller-score g))))))

;; ---------------------------------------------------------------------------
;; v0.10: compute-prediction-error (R3a + R3b)
;; ---------------------------------------------------------------------------

(deftest compute-prediction-error-shape-test
  (testing "compute-prediction-error returns documented fields"
    (let [e (fe/compute-prediction-error 0.5 {:mean 0.4 :variance 0.01})]
      (is (contains? e :observed))
      (is (contains? e :predicted-mean))
      (is (contains? e :predicted-variance))
      (is (contains? e :error))
      (is (contains? e :precision))
      (is (contains? e :weighted-error)))))

(deftest compute-prediction-error-positive-test
  (testing "observed > predicted → positive error and positive weighted-error"
    (let [e (fe/compute-prediction-error 0.6 {:mean 0.4 :variance 0.04})]
      (is (< (Math/abs (- 0.2 (:error e))) 1e-9))
      (is (= 25.0 (:precision e)) "precision = 1 / variance for variance > min")
      (is (< (Math/abs (- 5.0 (:weighted-error e))) 1e-9)))))

(deftest compute-prediction-error-negative-test
  (testing "observed < predicted → negative error"
    (let [e (fe/compute-prediction-error 0.3 {:mean 0.5 :variance 0.04})]
      (is (= -0.2 (:error e)))
      (is (neg? (:weighted-error e))))))

(deftest compute-prediction-error-min-variance-floor-test
  (testing "min-variance floor prevents division-by-zero when variance = 0"
    (let [e (fe/compute-prediction-error 0.5 {:mean 0.4 :variance 0.0})]
      ;; precision = 1 / 0.01 (default min-variance) = 100
      (is (= 100.0 (:precision e)))
      (is (not (Double/isInfinite (:weighted-error e)))))))

(deftest compute-prediction-error-min-variance-custom-test
  (testing "custom min-variance opt is respected"
    (let [e (fe/compute-prediction-error 0.5 {:mean 0.4 :variance 0.0}
                                          {:min-variance 0.5})]
      (is (= 2.0 (:precision e)) "precision = 1 / custom min-variance"))))

;; ---------------------------------------------------------------------------
;; AC1 (Joe's 2026-09-02 ruling on C130 2): the prediction triple no longer
;; substitutes 0.0 for any of its three members. Absent and malformed are
;; different verdicts and these tests are what hold them apart -- an absent
;; observation omits the channel, a malformed or missing model parameter
;; refuses the update.
;; ---------------------------------------------------------------------------

(deftest compute-prediction-error-present-status-test
  (testing "a complete triple is typed :present and scores as before"
    (let [e (fe/compute-prediction-error 0.6 {:mean 0.4 :variance 0.04})]
      (is (= :present (:status e)))
      (is (= :prediction-error/v1 (:producer-contract e)))
      (is (< (Math/abs (- 0.2 (:error e))) 1e-9)))))

(deftest compute-prediction-error-absent-observation-test
  (testing "an unobserved channel is omitted with a reason, never scored as 0.0"
    (let [e (fe/compute-prediction-error nil {:mean 0.5 :variance 0.04})]
      (is (= :absent (:status e)))
      (is (= :observed (:absent-member e)))
      (is (= :observation-absent (:reason e)))
      (is (not (contains? e :observed)) "no fabricated observation")
      (is (not (contains? e :error)) "no fabricated error")
      (is (not (contains? e :weighted-error)) "no fabricated weighted error"))))

(deftest channel-prediction-error-carries-envelope-reason-test
  (testing "an absent channel carries the observation envelope's own reason"
    (let [obs (obs/observe {})
          e (fe/channel-prediction-error obs :annotation-health
                                         {:mean 0.5 :variance 0.04})]
      (is (= :absent (:status e)))
      (is (= :annotation-health (:channel e)))
      (is (= :source-field-missing (:reason e)))
      (is (seq (:paths e)) "the missing source paths travel with the record")
      (is (not (contains? e :observed))))))

(deftest channel-prediction-error-observed-channel-scores-test
  (testing "an observed channel still scores, and a measured zero is present"
    (let [obs (obs/observe {:annotation-graph {:health 0.0}})
          e (fe/channel-prediction-error obs :annotation-health
                                         {:mean 0.4 :variance 0.04})]
      (is (= :present (:status e)))
      (is (= 0.0 (:observed e)) "a measured zero is an observation, not an absence")
      (is (< (Math/abs (- -0.4 (:error e))) 1e-9)))))

(deftest compute-prediction-error-missing-model-parameter-refuses-test
  (testing "a missing likelihood member refuses the update (C130 2 option B)"
    (doseq [prediction [{:variance 0.04} {:mean 0.5} {} nil]]
      (let [e (fe/compute-prediction-error 0.6 prediction)]
        (is (= :refused (:status e))
            (str "expected refusal for prediction " (pr-str prediction)))
        (is (= :malformed-prediction-triple (:reason e)))
        (is (seq (:offending e)))
        (is (not (contains? e :error)) "a refusal fabricates no error")))))

(deftest compute-prediction-error-malformed-members-refuse-test
  (testing "a present-but-not-finite member refuses and names itself"
    (let [e (fe/compute-prediction-error 0.6 {:mean "0.4" :variance 0.04})]
      (is (= :refused (:status e)))
      (is (= [:mean] (mapv :member (:offending e)))))
    (let [e (fe/compute-prediction-error 0.6 {:mean 0.4 :variance ##NaN})]
      (is (= :refused (:status e)))
      (is (= [:variance] (mapv :member (:offending e)))))
    (let [e (fe/compute-prediction-error ##Inf {:mean 0.4 :variance 0.04})]
      (is (= :refused (:status e)) "a non-finite OBSERVATION is malformed, not absent")
      (is (= [:observed] (mapv :member (:offending e)))))))

(deftest compute-prediction-error-absent-is-not-malformed-test
  (testing "absent and malformed are different verdicts on the same channel"
    (let [absent (fe/compute-prediction-error nil {:mean 0.5 :variance 0.04})
          malformed (fe/compute-prediction-error 0.5 {:mean nil :variance 0.04})]
      (is (not= (:status absent) (:status malformed)))
      (is (= :absent (:status absent)))
      (is (= :refused (:status malformed))))))

;; I5 slice (c) removed `compute-variational-free-energy`. What its tests
;; covered that nothing else does is the ABSENCE itself: a later reader who
;; needs a per-tick scalar should reach for F_pi
;; (`futon2.aif.policy-free-energy`), not reinstate this one.
(deftest variational-free-energy-stays-retired-test
  (testing "the retired scalar has no var in this namespace"
    (is (nil? (resolve 'futon2.aif.free-energy/compute-variational-free-energy)))
    (is (nil? (ns-resolve 'futon2.aif.free-energy
                          'compute-variational-free-energy)))))
