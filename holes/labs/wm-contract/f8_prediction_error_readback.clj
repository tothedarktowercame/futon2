;; f8_prediction_error_readback.clj -- :F8 leg 1 slice 2.
;;
;; Reads the production prediction-error producer (R8, eps) out of the
;; implementation itself for exactly the cases
;; DarkTower.WarMachine.MachinePredictionErrorWitness states as theorems, so
;; the Lean rationals and the Clojure doubles can be compared without a
;; tolerance.
;;
;; EXACTNESS, WITH ITS ONE EXCEPTION.  Every observation, mean and variance
;; below is dyadic, so each double IS the rational the Lean witness proves --
;; except in the two floored cases, which divide by the producer's default
;; min-variance 0.01, a value no double represents exactly.  The script prints
;; that double's full decimal expansion so the reader can see that the
;; agreement there is a rounding fact (1.0/0.01 rounds back to exactly 100.0)
;; and not a representability one.
;;
;;   run: clojure -M holes/labs/wm-contract/f8_prediction_error_readback.clj
;;   out: holes/labs/wm-contract/runs/F8-prediction-error/clojure-readback.txt
;;
;; Deterministic: no clock, no run id, no state read.  Re-running overwrites
;; the artifact with the same bytes.
(require '[futon2.aif.free-energy :as fe]
         '[futon2.aif.precision :as p]
         '[futon2.aif.belief :as b]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(defn triple [o m v] (fe/compute-prediction-error o {:mean m :variance v}))

;; (1) The scored arm: [lean-theorem observed mean variance lean-e lean-pi lean-w]
(def scored
  [["basicTriple"                    0.75  0.25   0.25  "1/2"  "4"   "2"]
   ["unitVariance"                   0.75  0.25   1.0   "1/2"  "1"   "1/2"]
   ["emptyBeliefReadsTheObservation" 0.75  0.0    1.0   "3/4"  "1"   "3/4"]
   ["negativeError"                  0.25  0.75   0.25  "-1/2" "4"   "-2"]
   ["exactZeroError"                 0.5   0.5    0.25  "0"    "4"   "0"]
   ["eighthVariance"                 0.75  0.25   0.125 "1/2"  "8"   "4"]
   ["zeroVarianceFloored"            0.75  0.25   0.0   "1/2"  "100" "50"]
   ["negativeVarianceFloored"        0.75  0.25  -4.0   "1/2"  "100" "50"]])

;; The same values as doubles, written out rather than recomputed, so the
;; comparison is against the Lean rational and not against the code's own
;; answer.
(def expected
  {"basicTriple"                    [0.5   4.0   2.0]
   "unitVariance"                   [0.5   1.0   0.5]
   "emptyBeliefReadsTheObservation" [0.75  1.0   0.75]
   "negativeError"                  [-0.5  4.0  -2.0]
   "exactZeroError"                 [0.0   4.0   0.0]
   "eighthVariance"                 [0.5   8.0   4.0]
   "zeroVarianceFloored"            [0.5 100.0  50.0]
   "negativeVarianceFloored"        [0.5 100.0  50.0]})

;; (2) The typed arms: [lean-theorem observed prediction status offending]
(def typed
  [["unobservedIsOmitted"              nil  {:mean 0.25 :variance 0.25} :absent []]
   ["brokenModelRefuses"               0.75 {:variance 0.25} :refused
    [[:mean :missing]]]
   ["unobservedWithBrokenModelRefuses" nil  {:variance 0.25} :refused
    [[:mean :missing]]]
   ["malformedObservationRefuses"      "not-a-number" {:mean 0.25 :variance 0.25} :refused
    [[:observed :not-finite]]]
   ["bothModelMembersNamed"            0.75 {:variance ##NaN} :refused
    [[:mean :missing] [:variance :not-finite]]]])

;; The eight channels Lean's MachinePredictionError.channelsWithLikelihood
;; names, in the order it lists them.
(def lean-channels-with-likelihood
  [:annotation-health :sorry-count-norm :mission-health :active-repo-ratio
   :support-coverage :attack-coverage :coupling-density :ticks-firing-ratio])

;; The fourteen declared channels, Holes.Channel.all in declaration order.
(def all-channels
  [:loop-health :support-coverage :attack-coverage :mission-health :stack-pct
   :consulting-pct :portfolio-pct :mathematics-pct :active-repo-ratio
   :sorry-count-norm :coupling-density :ticks-firing-ratio :depositing-signal
   :annotation-health])

(def scored-rows
  (vec (for [[nm o m v lean-e lean-pi lean-w] scored]
         (let [r (triple o m v)
               want (get expected nm)
               got [(:error r) (:precision r) (:weighted-error r)]]
           {:name nm :lean [lean-e lean-pi lean-w] :got got
            :delta (mapv - got want) :ok (= got want)}))))

(def typed-rows
  (vec (for [[nm o pred status offending] typed]
         (let [r (fe/compute-prediction-error o pred)
               got (mapv (juxt :member :status) (:offending r []))]
           {:name nm :status (:status r) :offending got
            :ok (and (= status (:status r)) (= offending got))}))))

;; (3) The two precisions, on one channel in one tick.
(def overwrite
  (let [t (triple 0.75 0.25 0.25)
        st (p/update-precision-state {} {:loop-health t})
        w (p/weighted-error st :loop-health t)]
    {:per-call (:precision t)
     :per-call-weighted (:weighted-error t)
     :r7 (:precision w)
     :r7-weighted (:weighted-error w)
     :preserved (:per-call-precision w)}))

(def channels-ok
  (= (set lean-channels-with-likelihood) (set b/channels-with-likelihood)))

(def without-likelihood
  (vec (remove (set lean-channels-with-likelihood) all-channels)))

(def lines
  (concat
   ["F8 leg 1 slice 2 -- production eps read back from futon2.aif.free-energy"
    "source: futon2/src/futon2/aif/free_energy.clj (compute-prediction-error, :prediction-error/v1)"
    "lean:   mathlib4/DarkTower/WarMachine/MachinePredictionErrorWitness.lean"
    ""
    "producer default min-variance = 0.01, whose double is exactly"
    (str "  " (.toString (java.math.BigDecimal. 0.01)))
    (str "  and 1.0/0.01 = " (.toString (java.math.BigDecimal. (/ 1.0 0.01)))
         " -- exact by rounding, not by representability")
    ""
    "(1) THE SCORED ARM"
    (format "%-32s %-7s %-7s %-7s %-22s %s"
            "lean theorem" "lean e" "lean pi" "lean w" "clojure [e pi w]"
            "delta [e pi w]")]
   (for [{:keys [name lean got delta]} scored-rows]
     (format "%-32s %-7s %-7s %-7s %-22s %s"
             name (nth lean 0) (nth lean 1) (nth lean 2)
             (pr-str (mapv double got)) (pr-str (mapv double delta))))
   [""
    "(2) THE TYPED ABSENCES AND REFUSALS"
    (format "%-36s %-9s %s" "lean theorem" "status" "offending [member status]")]
   (for [{:keys [name status offending]} typed-rows]
     (format "%-36s %-9s %s" name status (pr-str offending)))
   [""
    "(3) THE TWO PRECISIONS, ONE KEY (perCallPrecisionIsNotMachinePrecision)"
    (format "  producer :precision (from the likelihood variance 0.25) = %s, :weighted-error = %s"
            (:per-call overwrite) (:per-call-weighted overwrite))
    (format "  after precision/weighted-error:  :precision = %s, :weighted-error = %s, :per-call-precision = %s"
            (:r7 overwrite) (:r7-weighted overwrite) (:preserved overwrite))
    "  the R7 value 1.6 is MachinePrecisionWitness.halfError (8/5), slice 1's theorem for history [1/2]"
    ""
    "(4) THE EIGHT CHANNELS eps IS COMPUTED ON"
    (str "  clojure belief/channels-with-likelihood: " (pr-str (vec (sort b/channels-with-likelihood))))
    (str "  lean channelsWithLikelihood:             " (pr-str (vec (sort lean-channels-with-likelihood))))
    (str "  same set: " channels-ok ", count " (count b/channels-with-likelihood))
    (str "  the six declared channels with NO likelihood model, so no eps record at all: "
         (pr-str without-likelihood))
    ""
    (if (and (every? :ok scored-rows) (every? :ok typed-rows) channels-ok
             (= 6 (count without-likelihood))
             (= 4.0 (:per-call overwrite)) (= 1.6 (:r7 overwrite)))
      "VERDICT: every case matches its Lean statement EXACTLY (8/8 scored at delta 0.0, 5/5 typed outcomes, channel sets equal)."
      "VERDICT: MISMATCH -- at least one case differs from its Lean statement.")]))

(let [out (io/file "holes/labs/wm-contract/runs/F8-prediction-error/clojure-readback.txt")]
  (io/make-parents out)
  (spit out (str (str/join "\n" lines) "\n"))
  (println (slurp out)))
