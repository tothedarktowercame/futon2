;; f8_precision_readback.clj -- :F8 leg 1 slice 1.
;;
;; Reads production Pi (R7) out of the implementation itself for the exact
;; histories DarkTower.WarMachine.MachinePrecisionWitness states as theorems,
;; so the Lean rationals and the Clojure doubles can be compared without a
;; tolerance.  Every history is dyadic, so each double below is the exact
;; rational the Lean witness proves.
;;
;;   run: clojure -M holes/labs/wm-contract/f8_precision_readback.clj
;;   out: holes/labs/wm-contract/runs/F8-precision/clojure-readback.txt
;;
;; Deterministic: no clock, no run id, no state read.  Re-running overwrites
;; the artifact with the same bytes.
(require '[futon2.aif.precision :as p]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(defn pi-after
  "Production Pi for one channel after feeding `errors` in order, from the
   initial state.  :salience-mode defaults to :separate, the production path."
  [errors]
  (-> (reduce (fn [state e]
                (p/update-precision-state state {:loop-health {:error e :observed 0.75}}))
              {} errors)
      (get-in [:loop-health :precision])))

(def cases
  ;; [lean-theorem  history  lean-rational  expected-double]
  [["oneStillError"          [0.0]                    "2"      2.0]
   ["threeStillErrors"       (vec (repeat 3 0.0))     "4"      4.0]
   ["sevenStillErrors"       (vec (repeat 7 0.0))     "8"      8.0]
   ["fifteenStillErrors"     (vec (repeat 15 0.0))    "16"     16.0]
   ["fullWindowOfStillErrors" (vec (repeat 20 0.0))   "21"     21.0]
   ["windowBinds"            (vec (repeat 25 0.0))    "21"     21.0]
   ["unitError"              [1.0]                    "1"      1.0]
   ["halfError"              [0.5]                    "8/5"    1.6]
   ["tripleError"            [3.0]                    "1/5"    0.2]
   ["floorIsReached"         (vec (repeat 20 10.0))   "1/10"   0.1]])

(def lines
  (concat
   ["F8 leg 1 slice 1 -- production Pi read back from futon2.aif.precision"
    "source: futon2/src/futon2/aif/precision.clj (update-precision-state, :salience-mode :separate)"
    "lean:   mathlib4/DarkTower/WarMachine/MachinePrecisionWitness.lean"
    ""
    (format "defaults: window-size=%s min-variance=%s prior-variance=%s prior-strength=%s floor=%s cap=%s salience-mode=%s"
            p/default-window-size p/default-min-variance p/default-prior-variance
            p/default-prior-strength p/default-precision-floor p/default-precision-cap
            p/default-salience-mode)
    ""
    (format "%-24s %-8s %-8s %-24s %s" "lean theorem" "n" "lean" "clojure (%.17g)" "delta")]
   (for [[nm hist rat expected] cases]
     (let [got (double (pi-after hist))]
       (format "%-24s %-8d %-8s %-24s %s"
               nm (count hist) rat (format "%.17g" got)
               (format "%.17g" (- got expected)))))
   [""
    (if (every? (fn [[_ hist _ expected]] (= (double (pi-after hist)) expected)) cases)
      "VERDICT: every case matches its Lean rational EXACTLY (delta 0.0 on all 10)."
      "VERDICT: MISMATCH -- at least one case differs from its Lean rational.")]))

(let [out (io/file "holes/labs/wm-contract/runs/F8-precision/clojure-readback.txt")]
  (io/make-parents out)
  (spit out (str (str/join "\n" lines) "\n"))
  (println (slurp out)))
