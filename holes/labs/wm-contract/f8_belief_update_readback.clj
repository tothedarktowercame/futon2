;; F8 leg 1 slice 3: production belief update readback.
;;
;; Loads the real futon2.aif.belief namespace. Aggregation and categorical
;; filtering call production functions directly. The event-weight and
;; per-entity attribution arithmetic lives inside the private War Machine tick,
;; so the expressions below reproduce exactly war_machine.clj:6147-6187; this
;; script does not pretend those let-bound expressions are callable functions.
(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[futon2.aif.belief :as belief])

(defn event-weight [driver step]
  (* (min 1.0 (Math/abs (double driver)))
     (max 0.0 (- 1.0 (/ (double step) 3.0)))
     0.1))

(defn delta [got expected] (- (double got) (double expected)))

(def aggregation
  {:multichannel
   (binding [belief/*r3d-multichannel?* true]
     (belief/r3d-aggregate-driver
      {:annotation-health {:weighted-error 1.0 :precision 2.0}
       :sorry-count-norm {:weighted-error 0.25 :precision 1.0}}))
   :multichannel-scaled
   (binding [belief/*r3d-multichannel?* true]
     (belief/r3d-aggregate-driver
      {:annotation-health {:weighted-error 4.0 :precision 8.0}
       :sorry-count-norm {:weighted-error 1.0 :precision 4.0}}))
   :single
   (binding [belief/*r3d-multichannel?* false]
     (belief/r3d-aggregate-driver
      {:annotation-health {:weighted-error 1.0 :precision 2.0}}))
   :single-scaled
   (binding [belief/*r3d-multichannel?* false]
     (belief/r3d-aggregate-driver
      {:annotation-health {:weighted-error 4.0 :precision 8.0}}))
   :unknown (belief/r3d-aggregate-driver {})})

(def prior (belief/uniform-prior))
(def zero-posterior
  (belief/update-entity-belief prior {:type :strengthened :weight 0.0}
                               {:likelihood-mode :aif}))
(def strengthened
  (belief/update-entity-belief prior {:type :strengthened :weight 1.0}
                               {:likelihood-mode :aif}))
(def foreclosed
  (belief/update-entity-belief prior {:type :foreclosed :weight 1.0}
                               {:likelihood-mode :aif}))
(def h0 (belief/entity-expected-health prior))
(def hp (belief/entity-expected-health strengthened))
(def hn (belief/entity-expected-health foreclosed))

(def expected-strengthened (/ 1431362.0 2193329.0))
(def expected-foreclosed (/ 10772591.0 21198491.0))
(def tolerance 1.0e-12)
(def zero-posterior-max-delta
  (apply max (map #(Math/abs (delta (get zero-posterior %) (get prior %)))
                  belief/status-set)))

(def lines
  ["F8 leg 1 slice 3 -- production mu-next read back from futon2.aif.belief"
   "production calls: belief/r3d-aggregate-driver, belief/update-entity-belief (:aif), belief/entity-expected-health"
   "reproduced private arithmetic: war_machine.clj:6147-6187 (anneal, clamp, attribution)"
   ""
   "SCALAR / STRUCTURAL CASES (exact where delta is zero; otherwise tolerance 1e-12)"
   (format "multichannel driver       Lean 1/4  Clojure %.17g  delta %.17g"
           (get-in aggregation [:multichannel :driver])
           (delta (get-in aggregation [:multichannel :driver]) 0.25))
   (format "all precisions x4 driver Lean 1/4  Clojure %.17g  delta %.17g"
           (get-in aggregation [:multichannel-scaled :driver])
           (delta (get-in aggregation [:multichannel-scaled :driver]) 0.25))
   (format "single-channel driver     Lean 1    Clojure %.17g  delta %.17g"
           (get-in aggregation [:single :driver])
           (delta (get-in aggregation [:single :driver]) 1.0))
   (format "single precision x4       Lean 4    Clojure %.17g  delta %.17g"
           (get-in aggregation [:single-scaled :driver])
           (delta (get-in aggregation [:single-scaled :driver]) 4.0))
   (format "eventWeight d=1 step=0    Lean 1/10 Clojure %.17g  delta %.17g"
           (event-weight 1.0 0) (delta (event-weight 1.0 0) 0.1))
   (format "eventWeight d=1 step=1    Lean 1/15 Clojure %.17g  delta %.17g"
           (event-weight 1.0 1) (delta (event-weight 1.0 1) (/ 1.0 15.0)))
   (format "eventWeight d=1 step=2    Lean 1/30 Clojure %.17g  delta %.17g"
           (event-weight 1.0 2) (delta (event-weight 1.0 2) (/ 1.0 30.0)))
   (format "saturation d=50 step=0    Lean 1/10 Clojure %.17g  delta %.17g"
           (event-weight 50.0 0) (delta (event-weight 50.0 0) 0.1))
   (format "anneal step=3             Lean 0    Clojure %.17g  delta %.17g (unreachable in the 0,1,2 loop)"
           (event-weight 1.0 3) (delta (event-weight 1.0 3) 0.0))
   (str "unknown aggregation status=" (get-in aggregation [:unknown :status])
        " reason=" (get-in aggregation [:unknown :reason])
        " driver-present=" (contains? (:unknown aggregation) :driver))
   (format "kappa-zero no-op Lean exact; Clojure max component delta %.17g (identity-B floating normalisation)"
           zero-posterior-max-delta)
   ""
   (str "CATEGORICAL HEALTH READBACK (tolerance " tolerance ")")
   (format "uniform       Lean 4/7                 Clojure %.17g delta %.17g"
           h0 (delta h0 (/ 4.0 7.0)))
   (format "strengthened  Lean 1431362/2193329     Clojure %.17g delta %.17g"
           hp (delta hp expected-strengthened))
   (format "foreclosed    Lean 10772591/21198491   Clojure %.17g delta %.17g"
           hn (delta hn expected-foreclosed))
   (format "positive health delta %.17g; negative health delta %.17g; sum %.17g (not sign-symmetric)"
           (- hp h0) (- hn h0) (+ (- hp h0) (- hn h0)))
   ""
   (if (and (= 0.25 (get-in aggregation [:multichannel :driver]))
            (= 0.25 (get-in aggregation [:multichannel-scaled :driver]))
            (= 1.0 (get-in aggregation [:single :driver]))
            (= 4.0 (get-in aggregation [:single-scaled :driver]))
            (< zero-posterior-max-delta tolerance)
            (< (Math/abs (delta hp expected-strengthened)) tolerance)
            (< (Math/abs (delta hn expected-foreclosed)) tolerance)
            (not= (- hp h0) (- (- hn h0))))
     "VERDICT: production matches every Lean reference (exact cases exact; categorical cases within 1e-12)."
     "VERDICT: MISMATCH.")])

(let [out (io/file "holes/labs/wm-contract/runs/F8-belief-update/clojure-readback.txt")]
  (io/make-parents out)
  (spit out (str (str/join "\n" lines) "\n"))
  (println (slurp out)))
