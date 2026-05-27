(ns futon2.aif.precision-test
  "Tests for R7 adaptive precision (per-channel Π tracked across calls
   via prediction-error history)."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]
            [futon2.aif.precision :as precision]))

(deftest initial-precision-state-shape-test
  (testing "default initial state covers every channel-with-likelihood"
    (let [s (precision/initial-precision-state)]
      (is (= belief/channels-with-likelihood (set (keys s))))
      (is (every? #(= 1.0 (:precision %)) (vals s)))
      (is (every? #(= [] (:error-history %)) (vals s)))))
  (testing "custom channel-set respected"
    (let [s (precision/initial-precision-state #{:ch1 :ch2})]
      (is (= #{:ch1 :ch2} (set (keys s)))))))

(deftest update-appends-error-to-history-test
  (testing "single error update appends to history"
    (let [s0 (precision/initial-precision-state #{:ch})
          s1 (precision/update-precision-state s0 {:ch {:error 0.3}})]
      (is (= [0.3] (get-in s1 [:ch :error-history]))))))

(deftest update-bounded-window-test
  (testing "history bounded to window-size; oldest errors drop"
    (let [s0 (precision/initial-precision-state #{:ch})
          errors-seq (mapv #(double %) (range 30))
          final-state (reduce (fn [s e]
                                (precision/update-precision-state
                                 s {:ch {:error e}}
                                 {:window-size 5}))
                              s0 errors-seq)]
      (is (= 5 (count (get-in final-state [:ch :error-history])))
          "history bounded to 5")
      (is (= [25.0 26.0 27.0 28.0 29.0]
             (get-in final-state [:ch :error-history]))
          "oldest dropped, newest kept"))))

(deftest precision-stable-under-zero-error-test
  (testing "history of zero errors → low variance → high precision (capped at 1/min-variance)"
    (let [s0 (precision/initial-precision-state #{:ch})
          s (reduce (fn [s _]
                      (precision/update-precision-state s {:ch {:error 0.0}}))
                    s0 (range 10))]
      (is (= 100.0 (get-in s [:ch :precision]))
          "zero error → variance 0 → precision = 1 / min-variance (0.01) = 100.0"))))

(deftest precision-drops-under-sustained-high-error-test
  (testing "high-variance error history → low precision"
    (let [s0 (precision/initial-precision-state #{:ch})
          high-var-errors [0.5 -0.5 0.5 -0.5 0.5 -0.5 0.5 -0.5 0.5 -0.5]
          s (reduce (fn [s e]
                      (precision/update-precision-state s {:ch {:error e}}))
                    s0 high-var-errors)]
      ;; variance ≈ 0.25 → precision = 1/0.25 = 4.0
      (is (< (Math/abs (- 4.0 (get-in s [:ch :precision]))) 1e-6)
          (str "precision should approach 4.0 for variance 0.25; got "
               (get-in s [:ch :precision]))))))

(deftest precision-rises-as-history-converges-test
  (testing "precision INCREASES as a high-error channel stabilises (variance drops)"
    (let [s0 (precision/initial-precision-state #{:ch})
          ;; first 5 high-spread errors, then 15 low-spread errors
          mixed-errors (concat [0.5 -0.5 0.5 -0.5 0.5]
                               (repeat 15 0.05))
          s (reduce (fn [s e]
                      (precision/update-precision-state s {:ch {:error e}}
                                                        {:window-size 15}))
                    s0 mixed-errors)]
      ;; final history is last 15 errors; mostly 0.05 → variance ≈ 0
      ;; → precision should be near 1/min-variance (=100)
      (is (> (get-in s [:ch :precision]) 50.0)
          (str "precision should rise as channel converges; got "
               (get-in s [:ch :precision]))))))

(deftest untouched-channels-pass-through-test
  (testing "channels in prev-state without new error pass through unchanged"
    (let [s0 {:ch1 {:precision 5.0 :error-history [0.1 0.2]}
              :ch2 {:precision 3.0 :error-history [0.3]}}
          s1 (precision/update-precision-state s0 {:ch1 {:error 0.4}})]
      (is (= {:precision 3.0 :error-history [0.3]} (get s1 :ch2))
          "ch2 unchanged")
      (is (not= s0 (get s1 :ch1))
          "ch1 updated"))))

(deftest new-channels-initialised-on-first-error-test
  (testing "channel not in prev-state gets initialised on first error"
    (let [s1 (precision/update-precision-state {} {:new-ch {:error 0.1}})]
      (is (contains? s1 :new-ch))
      (is (= [0.1] (get-in s1 [:new-ch :error-history]))))))

(deftest precision-for-lookup-test
  (testing "precision-for returns the tracked precision"
    (let [s {:ch1 {:precision 7.5 :error-history []}}]
      (is (= 7.5 (precision/precision-for s :ch1)))))
  (testing "precision-for falls back to default for unknown channels"
    (is (= 1.0 (precision/precision-for {} :unknown)))))

(deftest weighted-error-uses-adaptive-precision-test
  (testing "weighted-error replaces per-call precision with adaptive precision"
    (let [s {:ch {:precision 8.0 :error-history [0.1 0.1]}}
          err {:error 0.5 :precision 100.0 :weighted-error 50.0 :observed 0.5
               :predicted-mean 0.0 :predicted-variance 0.01}
          wt (precision/weighted-error s :ch err)]
      (is (= 8.0 (:precision wt))
          "precision overridden with adaptive value")
      (is (= 100.0 (:per-call-precision wt))
          "original per-call precision preserved under :per-call-precision")
      (is (= 4.0 (:weighted-error wt))
          "weighted-error = error * adaptive precision = 0.5 * 8.0"))))

(deftest update-preserves-purity-test
  (testing "update-precision-state is pure: same input → same output"
    (let [s0 (precision/initial-precision-state #{:ch})
          errors {:ch {:error 0.3}}
          s1a (precision/update-precision-state s0 errors)
          s1b (precision/update-precision-state s0 errors)]
      (is (= s1a s1b)))))

;; ---------------------------------------------------------------------------
;; v0.13: need-component added — precision = variance-component + need-component
;; ---------------------------------------------------------------------------

(deftest need-component-in-preferred-range-test
  (testing "channel with observed value in preferred range → need-component 0"
    ;; :annotation-health preference [0.7 1.0]; observed 0.85 is in-range
    (let [s0 (precision/initial-precision-state #{:annotation-health})
          errors {:annotation-health {:error 0.0 :observed 0.85}}
          s1 (precision/update-precision-state s0 errors)]
      (is (= 0.0 (get-in s1 [:annotation-health :need-component]))
          "in-range observation contributes zero need-component"))))

(deftest need-component-below-preferred-range-test
  (testing "observed below preference range → positive need-component"
    ;; :annotation-health preference [0.7 1.0]; observed 0.4 is 0.3 below
    (let [s0 (precision/initial-precision-state #{:annotation-health})
          errors {:annotation-health {:error 0.0 :observed 0.4}}
          s1 (precision/update-precision-state s0 errors)
          nc (get-in s1 [:annotation-health :need-component])]
      (is (< (Math/abs (- (* 5.0 0.3) nc)) 1e-6)
          (str "need-component should be need-scale × gap = 5 × 0.3 = 1.5; got " nc)))))

(deftest need-component-above-preferred-range-test
  (testing "observed above preference range → positive need-component"
    ;; :sorry-count-norm preference [0.0 0.3]; observed 0.9 is 0.6 above
    (let [s0 (precision/initial-precision-state #{:sorry-count-norm})
          errors {:sorry-count-norm {:error 0.0 :observed 0.9}}
          s1 (precision/update-precision-state s0 errors)
          nc (get-in s1 [:sorry-count-norm :need-component])]
      (is (< (Math/abs (- (* 5.0 0.6) nc)) 1e-6)
          (str "need-component should be 5 × 0.6 = 3.0; got " nc)))))

(deftest precision-combines-both-components-test
  (testing "precision = variance-component + need-component, bounded"
    (let [s0 (precision/initial-precision-state #{:annotation-health})
          ;; observed 0.4 is below preference [0.7 1.0]; error tiny so variance ≈ 0 → variance-component = 100
          errors {:annotation-health {:error 0.001 :observed 0.4}}
          s1 (precision/update-precision-state s0 errors)
          channel (get s1 :annotation-health)]
      (is (pos? (:variance-component channel)))
      (is (pos? (:need-component channel)))
      (is (= (:precision channel)
             (-> (+ (:variance-component channel) (:need-component channel))
                 (max 0.1)
                 (min 200.0)))
          "precision is bounded sum of components"))))

(deftest precision-cap-bounds-runaway-test
  (testing "precision capped at precision-cap"
    (let [s0 (precision/initial-precision-state #{:annotation-health})
          ;; in-range observation (need = 0); variance-component will be 100 (= 1/min-variance)
          ;; cap = 200 so we'd need need + variance > 200 to hit it
          ;; force extreme gap to trigger cap: observed at -10 (impossible but tests cap)
          errors {:annotation-health {:error 0.0 :observed -100.0}}
          s1 (precision/update-precision-state s0 errors)]
      (is (= 200.0 (get-in s1 [:annotation-health :precision]))
          "precision capped at 200.0 even under extreme gap"))))

(deftest ad-hoc-channels-unaffected-by-need-component-test
  (testing "channels not in pref/preferences have need-component = 0"
    (let [s0 (precision/initial-precision-state #{:not-a-real-channel})
          errors {:not-a-real-channel {:error 0.5 :observed 99.0}}
          s1 (precision/update-precision-state s0 errors)]
      (is (= 0.0 (get-in s1 [:not-a-real-channel :need-component]))
          "unknown channels get zero need-component (no preference to compare to)"))))
