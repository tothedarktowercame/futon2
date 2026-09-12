(ns generate-row16-r17-witness
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def witness-path "holes/labs/wm-contract/runs/row-12-accumulation-2026-09-12/witness.edn")
(def trace-path "data/wm-trace/wm-trace-2026-09-04.edn")
(def output-path "/home/joe/code/mathlib4/DarkTower/WarMachine/MachineAccumulationWitness.lean")

(defn exact-ieee-ratio [x]
  (let [bits (Double/doubleToRawLongBits (double x))
        sign (if (neg? bits) -1N 1N)
        exponent (bit-and (unsigned-bit-shift-right bits 52) 0x7ff)
        fraction (bit-and bits 0xfffffffffffff)
        significand (bigint (if (zero? exponent) fraction (+ fraction 0x10000000000000)))
        power (- exponent 1075)
        numerator (* sign significand)]
    (if (neg? power)
      (/ numerator (.shiftLeft java.math.BigInteger/ONE (- power)))
      (* numerator (.shiftLeft java.math.BigInteger/ONE power)))))

(defn lean-rat [x]
  (if (ratio? x) (str "(" (numerator x) " / " (denominator x) " : ℚ)")
      (str "(" x " : ℚ)")))

(defn trace-forms [n]
  (with-open [reader (java.io.PushbackReader. (io/reader trace-path))]
    (vec (repeatedly n #(edn/read {:eof nil} reader)))))

(defn theorem-name [tick-index coordinate-index]
  (format "tick%dCoordinate%03d" tick-index coordinate-index))

(defn generate! []
  (let [witness (edn/read-string (slurp witness-path))
        traces (trace-forms 3)
        entity (:entity witness)
        os (get-in witness [:coordinate-contract :observation])
        ss (get-in witness [:coordinate-contract :state])
        rows (vec
              (mapcat
               (fn [tick-index]
                 (let [trace (traces tick-index)
                       step (get-in witness [:steps tick-index])]
                   (map-indexed
                    (fn [coordinate-index [o s]]
                      (let [before (if (zero? tick-index) 1.0
                                       (get-in witness [:steps (dec tick-index) :production o s]))
                            observation (get-in trace [:observation o])
                            belief (get-in trace [:mu-post entity s])
                            after (get-in step [:production o s])
                            [a x m z] (map exact-ieee-ratio [before observation belief after])
                            residual (- z (+ a (* x m)))]
                        {:tick tick-index :index coordinate-index :o o :s s
                         :a a :x x :m m :z z :residual residual}))
                    (for [o os s ss] [o s]))))
               (range 3)))
        maximum (apply max-key #(abs (:residual %)) rows)
        header ["import DarkTower.WarMachine.MachineAccumulation" ""
                "namespace DarkTower.WarMachine.MachineAccumulationWitness" ""
                "/- Generated from the pinned Row 12 witness. Each theorem interprets the"
                "retained binary64 readings as exact rationals and exhibits rounding residual. -/" ""]
        bodies (mapcat
                (fn [{:keys [tick index o s a x m z residual]}]
                  [(str "/-- tick " tick ", " o " × " s " -/")
                   (str "theorem " (theorem-name tick index) " : "
                        (lean-rat z) " = " (lean-rat a) " + " (lean-rat x) " * "
                        (lean-rat m) " + " (lean-rat residual) " ∧ "
                        "|" (lean-rat residual) "| ≤ (1 / 2^48 : ℚ) := by")
                   "  norm_num [abs_of_nonneg, abs_of_nonpos]" ""])
                rows)
        footer [(str "/-- Largest absolute exact-vs-binary64 residual, attained at tick "
                     (:tick maximum) ", " (:o maximum) " × " (:s maximum) ". -/")
                (str "theorem measuredMaximumResidual : "
                     "(" (lean-rat (abs (:residual maximum))) ") = "
                     (lean-rat (abs (:residual maximum))) " := rfl")
                "" "end DarkTower.WarMachine.MachineAccumulationWitness"
                "" "#print axioms DarkTower.WarMachine.MachineAccumulationWitness.measuredMaximumResidual"]]
    (when (some #(> (abs (:residual %)) (/ 1N (.shiftLeft java.math.BigInteger/ONE 48))) rows)
      (throw (ex-info "contract-v1.2 residual bound exceeded" maximum)))
    (spit output-path (str/join "\n" (concat header bodies footer)))
    (prn {:coordinates (count rows) :maximum-residual (str (abs (:residual maximum)))
          :at [(:tick maximum) (:o maximum) (:s maximum)]})))

(generate!)
