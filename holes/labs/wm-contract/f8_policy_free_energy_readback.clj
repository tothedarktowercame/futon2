;; F8 leg 1 slice 4: production policy-free-energy readback.
(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[futon2.aif.policy :as policy]
         '[futon2.aif.policy-free-energy :as fpi])

(def tolerance 1.0e-12)
(defn delta [actual expected] (- (double actual) (double expected)))
(defn outcome [f]
  (try {:status :ok :value (f)}
       (catch clojure.lang.ExceptionInfo e
         {:status :error :error (:error (ex-data e))})))

(def positive
  {:prediction-mean {:a 0.5 :b -0.25}
   :prediction-variance {:a 0.25 :b 1.0}})
(def observation {:a 1.0 :b 0.25})
(def positive-expected
  (+ (* 0.5 (+ (Math/log (* 2.0 Math/PI 0.25)) 1.0))
     (* 0.5 (+ (Math/log (* 2.0 Math/PI 1.0)) 0.25))))
(def positive-result (fpi/f-pi-for-candidate positive observation))

(def deterministic
  {:prediction-mean {:a 0.0} :prediction-variance {:a 0.0}})
(def absent
  (assoc deterministic :variance-status {:a {:status :absent
                                               :reason :not-predicted}}))
(def tolerated (outcome #(fpi/f-pi-for-candidate deterministic {:a 0.001}
                           {:deterministic-tolerance 0.001})))
(def zero-reject (outcome #(fpi/f-pi-for-candidate deterministic {:a 0.01}
                             {:deterministic-tolerance 0.001})))
(def absent-floor (outcome #(fpi/f-pi-for-candidate absent {:a 0.1}
                             {:absent-variance :floor
                              :variance-floor 0.01})))
(def bare-zero-floor (outcome #(fpi/f-pi-for-candidate deterministic {:a 0.1}
                                {:absent-variance :floor
                                 :variance-floor 0.01})))
(def negative (outcome #(fpi/f-pi-for-candidate
                          {:prediction-mean {:a 0.0}
                           :prediction-variance {:a -1.0}}
                          {:a 0.0})))
(def floor-expected (* 0.5 (+ (Math/log (* 2.0 Math/PI 0.01)) 1.0)))

(def candidate-two
  {:prediction-mean {:a 0.0 :b 0.0}
   :prediction-variance {:a 1.0 :b 1.0}})
(def vector-result (fpi/f-pi-vector [positive candidate-two] observation))
(def vector-expected-two
  (+ (* 0.5 (+ (Math/log (* 2.0 Math/PI)) 1.0))
     (* 0.5 (+ (Math/log (* 2.0 Math/PI)) 0.0625))))

(def unscaled
  (policy/selection-scores [4.0 4.0] 2.0 [0.0 0.0]
    {:f-pi-policy-posterior? true :f-pi-values [3.0 0.0]
     :f-pi-scaling :unscaled}))
(def by-tau-two
  (policy/selection-scores [4.0 4.0] 2.0 [0.0 0.0]
    {:f-pi-policy-posterior? true :f-pi-values [3.0 0.0]
     :f-pi-scaling :by-tau}))
(def by-tau-four
  (policy/selection-scores [4.0 4.0] 4.0 [0.0 0.0]
    {:f-pi-policy-posterior? true :f-pi-values [3.0 0.0]
     :f-pi-scaling :by-tau}))

(def lines
  ["F8 leg 1 slice 4 -- production F_pi readback"
   "production calls: f-pi-for-candidate, f-pi-vector, selection-scores"
   "Lean keeps log symbolic; complete floating terms compare at tolerance 1e-12"
   (format "positive two-channel total actual %.17g expected %.17g delta %.17g"
           positive-result positive-expected (delta positive-result positive-expected))
   (str "zero tolerated status=" (:status tolerated) " value=" (:value tolerated))
   (str "zero mismatch status=" (:status zero-reject) " error=" (:error zero-reject))
   (format "absent zero + floor status=%s actual %.17g expected %.17g delta %.17g"
           (name (:status absent-floor)) (:value absent-floor) floor-expected
           (delta (:value absent-floor) floor-expected))
   (str "bare zero + floor status=" (:status bare-zero-floor)
        " error=" (:error bare-zero-floor))
   (str "negative variance status=" (:status negative) " error=" (:error negative))
   (format "f-pi-vector[0] actual %.17g expected %.17g delta %.17g"
           (nth vector-result 0) positive-expected
           (delta (nth vector-result 0) positive-expected))
   (format "f-pi-vector[1] actual %.17g expected %.17g delta %.17g"
           (nth vector-result 1) vector-expected-two
           (delta (nth vector-result 1) vector-expected-two))
   (str "selection unscaled tau=2 actual=" unscaled " expected=[-5.0 -2.0]")
   (str "selection by-tau tau=2 actual=" by-tau-two " expected=[-3.5 -2.0]")
   (str "selection by-tau tau=4 actual=" by-tau-four " expected=[-1.75 -1.0]")
   (if (and (< (Math/abs (delta positive-result positive-expected)) tolerance)
            (= {:status :ok :value 0.0} tolerated)
            (= :deterministic-mismatch (:error zero-reject))
            (< (Math/abs (delta (:value absent-floor) floor-expected)) tolerance)
            (= :deterministic-mismatch (:error bare-zero-floor))
            (= :invalid-variance (:error negative))
            (every? #(< (Math/abs %) tolerance)
                    [(delta (nth vector-result 0) positive-expected)
                     (delta (nth vector-result 1) vector-expected-two)])
            (= [-5.0 -2.0] unscaled)
            (= [-3.5 -2.0] by-tau-two)
            (= [-1.75 -1.0] by-tau-four))
     "VERDICT: every production case matches its Lean branch/reference."
     "VERDICT: MISMATCH.")])

(let [out (io/file "holes/labs/wm-contract/runs/F8-policy-free-energy/clojure-readback.txt")]
  (io/make-parents out)
  (spit out (str (str/join "\n" lines) "\n"))
  (println (slurp out)))
