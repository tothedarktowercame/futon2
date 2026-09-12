(ns generate-row16-r6-posterior-witness
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.security MessageDigest]))

(def trace-path "data/wm-trace/wm-trace-2026-09-12.edn.pre-migration-backup")
(def trace-form-index 3)
(def run-dir "holes/labs/wm-contract/runs/row-16-r6-posterior-2026-09-12")
(def fixture-path (str run-dir "/fixture.edn"))
(def manifest-path (str run-dir "/fixture-pin.edn"))
(def lean-path "/home/joe/code/mathlib4/DarkTower/WarMachine/MachinePolicyPosteriorWitness.lean")

(defn sha256-file [path]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream path)]
      (let [buffer (byte-array 65536)]
        (loop []
          (let [n (.read in buffer)]
            (when (pos? n)
              (.update digest buffer 0 n)
              (recur))))))
    (apply str (map #(format "%02x" (bit-and 255 %)) (.digest digest)))))

(defn trace-record []
  (with-open [reader (java.io.PushbackReader. (io/reader trace-path))]
    (nth (vec (repeatedly (inc trace-form-index)
                          #(edn/read {:eof nil} reader)))
         trace-form-index)))

(defn action-id [action]
  (select-keys action [:type :target :target-class]))

(defn exact-ieee-ratio [x]
  (let [bits (Double/doubleToRawLongBits (double x))
        sign (if (neg? bits) -1N 1N)
        exponent (bit-and (unsigned-bit-shift-right bits 52) 0x7ff)
        fraction (bit-and bits 0xfffffffffffff)
        significand (bigint (if (zero? exponent)
                              fraction
                              (+ fraction 0x10000000000000)))
        power (- exponent 1075)
        numerator (* sign significand)]
    (if (neg? power)
      (/ numerator (.shiftLeft java.math.BigInteger/ONE (- power)))
      (* numerator (.shiftLeft java.math.BigInteger/ONE power)))))

(defn lean-rat [x]
  (if (ratio? x)
    (str "(" (numerator x) " / " (denominator x) " : ℚ)")
    (str "(" x " : ℚ)")))

(defn candidate-rows [record]
  (let [ranked (:ranked-actions record)
        by-action (into {} (map (juxt #(action-id (:action %)) identity))
                        (get-in record [:decision :habit-adjusted-ranking]))
        tau (double (get-in record [:decision :tau]))
        inputs (mapv (fn [ranked-row]
                       (let [h (get by-action (action-id (:action ranked-row)))
                             g (double (:controller-score h))
                             ln-e (double (:habit-prior-bias h))
                             score (- ln-e (/ g tau))]
                         (when-not (= score (double (:selection-score h)))
                           (throw (ex-info "retained selection score mismatch"
                                           {:rank (:rank ranked-row)})))
                         {:rank (:rank ranked-row)
                          :action (action-id (:action ranked-row))
                          :G g :lnE ln-e :selection-score score
                          :raw-exp (Math/exp score)}))
                     ranked)
        total (reduce + (map :raw-exp inputs))]
    (mapv (fn [row]
            (let [reference (/ (:raw-exp row) total)
                  retained (double
                            (get-in record
                                    [:decision :softmax-weights-by-candidate-id
                                     (str "rank/" (:rank row))]))]
              (assoc row
                     :reference-weight reference
                     :retained-weight retained
                     :residual (- retained reference))))
          inputs)))

(defn generate! []
  (.mkdirs (io/file run-dir))
  (let [record (trace-record)
        rows (candidate-rows record)
        stable-scores (mapv :selection-score rows)
        max-score (apply max stable-scores)
        stable-exps (mapv #(Math/exp (- % max-score)) stable-scores)
        stable-total (reduce + stable-exps)
        stable-reference (mapv #(/ % stable-total) stable-exps)
        retained (mapv :retained-weight rows)
        stable-deltas (mapv - retained stable-reference)
        rational-rows
        (mapv (fn [row]
                (assoc row
                       :residual-exact-rational
                       (str (exact-ieee-ratio (:residual row)))))
              rows)
        max-row (apply max-key #(Math/abs (double (:residual %))) rows)
        max-residual (abs (exact-ieee-ratio (:residual max-row)))
        bound (/ 1N (.shiftLeft java.math.BigInteger/ONE 45))
        fixture
        {:schema :wm/row16-r6-posterior-fixture-v1
         :scope :production-trace-details-record-20260912T172809-f-absent
         :source {:repo "futon2" :path trace-path
                  :sha256 (sha256-file trace-path)
                  :form-index trace-form-index
                  :timestamp (:timestamp record)
                  :trace-schema 27}
         :extracted-at "2026-09-12T20:40:00Z"
         :subject {:equation :policy-posterior :node :R6 :quantity :Q-pi
                   :declaration "softmaxWithFPi"}
         :tau (get-in record [:decision :tau])
         :f-pi (get-in record [:decision :f-pi-posterior])
         :reference {:method :raw-exponential-normalisation
                     :independent-of-retained-weights true
                     :rounding-bound {:absolute bound
                                      :derivation "2^-45 exceeds gamma_152 at binary64 unit roundoff 2^-53, covering 148-term sequential normalization plus exp/divide roundings."}}
         :coordinates rational-rows
         :comparison {:coordinate-count (count rows)
                      :stable-log-sum-exp-exact-ieee-match?
                      (every? zero? stable-deltas)
                      :raw-carrier-coordinate-exact-ieee-match?
                      (every? #(zero? (:residual %)) rows)
                      :nonzero-raw-residual-count
                      (count (remove #(zero? (:residual %)) rows))
                      :maximum-absolute-residual max-residual
                      :maximum-at-rank (:rank max-row)
                      :bound bound
                      :within-bound? (<= max-residual bound)}
         :limits {:nonzero-f-pi-correspondence :open
                  :live-selector-correspondence :out-of-scope}}
        theorem-lines
        (mapcat
         (fn [{:keys [rank retained-weight reference-weight residual]}]
           (let [retained-q (exact-ieee-ratio retained-weight)
                 reference-q (exact-ieee-ratio reference-weight)
                 residual-q (exact-ieee-ratio residual)]
             [(str "/-- Rank " rank ": retained posterior versus raw-exp carrier reference. -/")
              (str "theorem coordinate" (format "%03d" rank) " : "
                   (lean-rat retained-q) " = " (lean-rat reference-q) " + "
                   (lean-rat residual-q) " ∧ |" (lean-rat residual-q)
                   "| ≤ (1 / 2^45 : ℚ) := by")
              "  norm_num [abs_of_nonneg, abs_of_nonpos]" ""]))
         rows)
        lean
        (str/join
         "\n"
         (concat
          ["import DarkTower.WarMachine.PolicyPosterior" ""
           "namespace DarkTower.WarMachine.MachinePolicyPosteriorWitness" ""
           "/- Generated from the bounded schema-27 production fixture. The reference"
           "uses the declared raw-exponential normalization and never reads retained Q. -/" ""
           "/-- The record's absent F_pi branch is the closed softmax branch of the general carrier. -/"
           "theorem fAbsentBranchCompatibility {PolicyIndex : Type*} (exp log : ℝ → ℝ)"
           "    (habit : PolicyIndex → ℝ)"
           "    (grade : PolicyIndex → DarkTower.WarMachine.Holes.ExpectedFreeEnergyValue)"
           "    (tau : ℝ) (policies : List PolicyIndex) :"
           "    DarkTower.WarMachine.PolicyPosterior.softmaxWithFPi exp log habit grade (fun _ => 0) tau policies ="
           "      DarkTower.WarMachine.Holes.softmax exp log habit grade tau policies := by"
           "  exact DarkTower.WarMachine.PolicyPosterior.softmaxWithFPi_zero exp log habit grade tau policies" ""]
          theorem-lines
          [(str "/-- Maximum observed raw-exp versus retained binary64 residual; all 148"
                " coordinates are bounded above by 2^-45. -/")
           (str "theorem measuredMaximumResidual : " (lean-rat max-residual)
                " ≤ (1 / 2^45 : ℚ) := by")
           "  norm_num" ""
           "end DarkTower.WarMachine.MachinePolicyPosteriorWitness" ""
           "#print axioms DarkTower.WarMachine.MachinePolicyPosteriorWitness.fAbsentBranchCompatibility"
           "#print axioms DarkTower.WarMachine.MachinePolicyPosteriorWitness.measuredMaximumResidual"]))]
    (when-not (true? (get-in fixture [:comparison :within-bound?]))
      (throw (ex-info "contract-v1.2 residual bound exceeded"
                      (:comparison fixture))))
    (spit fixture-path (str (pr-str fixture) "\n"))
    (spit manifest-path
          (str (pr-str {:fixture {:repo "futon2" :path fixture-path
                                  :sha256 (sha256-file fixture-path)}}) "\n"))
    (spit lean-path (str lean "\n"))
    (prn (:comparison fixture))))

(generate!)
