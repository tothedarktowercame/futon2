(require '[futon2.aif.machine-model :as machine-model]
         '[futon2.aif.machine-predictive :as predictive])

(load-file "holes/labs/wm-contract/runs/row-9-predicted-state/readback.clj")
(def row9-pin {:path "holes/labs/wm-contract/runs/row-9-predicted-state/input.edn"
               :sha256 "a8b7fe999a8e980b1882c5f958adc1f3be06502767014b4d9972d183bf7d1d24"})
(defn row9-var [name] (var-get (resolve (symbol "user" name))))
(when-not (= (:sha256 row9-pin) ((row9-var "sha256") (:path row9-pin)))
  (throw (ex-info "Row-9 input pin changed" {:pin row9-pin})))
(def outcome (machine-model/outcome-authority))
(def base-model {:model (:model (row9-var "input"))
                 :state-support (row9-var "states") :outcome outcome})
(def a-result (predictive/declared-outcome-a base-model))
(def model (assoc base-model :A (dissoc a-result :ok)))
(def repeated (assoc (first (row9-var "policies")) :id "advance-twice-repeat"))
(def all-policies (conj (row9-var "policies") repeated))
(def result (predictive/predictive-outcome-kernel
             model (:belief-input (row9-var "belief-result")) (row9-var "kernel") all-policies))
(def constant-row (zipmap (:support outcome) (repeat (/ 1.0 (count (:support outcome))))))
(def controls
  {:missing-a-support
   (predictive/predictive-outcome-kernel
    (update-in model [:A :rows] dissoc (first (row9-var "states")))
    (:belief-input (row9-var "belief-result")) (row9-var "kernel") all-policies)
   :foreign-model
   (predictive/predictive-outcome-kernel
    (assoc-in model [:model :revision] "foreign")
    (:belief-input (row9-var "belief-result")) (row9-var "kernel") all-policies)
   :evidence
   (predictive/predictive-outcome-kernel
    (assoc model :outcome-vertex :evidence)
    (:belief-input (row9-var "belief-result")) (row9-var "kernel") all-policies)
   :constant-checkpoint-sensitivity
   {:adapter :constant-checkpoint-kernel/v1
    :passes? (not= constant-row constant-row)
    :expected false}})
(defn exact-ieee-ratio
  "The exact rational represented by a finite IEEE-754 binary64 value."
  [x]
  (let [bits (Double/doubleToRawLongBits (double x))
        negative? (neg? bits)
        exponent (bit-and 0x7ff (unsigned-bit-shift-right bits 52))
        fraction (bit-and bits 0x000fffffffffffff)]
    (when (= exponent 0x7ff)
      (throw (ex-info "Nonfinite reference input" {:value x})))
    (let [mantissa (if (zero? exponent) fraction (+ (bit-shift-left 1 52) fraction))
          power (- (if (zero? exponent) 1 exponent) 1023 52)
          numerator (* (if negative? -1 1) (bigint mantissa))]
      (if (neg? power)
        (/ numerator (bigint (.shiftLeft java.math.BigInteger/ONE (- power))))
        (* numerator (bigint (.shiftLeft java.math.BigInteger/ONE power)))))))
(def exact-posterior
  (into {} (map (fn [[state mass]] [state (exact-ieee-ratio mass)]))
        (:posterior (first (:source ((row9-var "pinned-read") (:row-7 (row9-var "input"))))))))
(def successor (get-in ((row9-var "pinned-read") (:row-8 (row9-var "input")))
                       [:complete-rows :successor]))
(defn exact-step [q action]
  (reduce (fn [out [state mass]]
            (update out (get successor [state action]) + mass))
          (zipmap (row9-var "states") (repeat 0)) q))
(defn exact-terminal [q actions] (reduce exact-step q actions))
(def positional-a
  (into {} (map-indexed (fn [i state]
                          [state (nth (:support outcome) (mod i (count (:support outcome))))])
                        (row9-var "states"))))
(defn exact-outcome-row [state-row]
  (reduce (fn [out [state mass]] (update out (get positional-a state) + mass))
          (zipmap (:support outcome) (repeat 0)) state-row))
(def exact-references
  (into {} (for [p all-policies]
             [(:id p) (exact-outcome-row (exact-terminal exact-posterior (:actions p)))])))
(defn exact-delta [production reference]
  (- (exact-ieee-ratio production) reference))
(def comparisons
  (into {} (for [[id row] (:rows result)]
             (let [reference (get exact-references id)
                   deltas (into {} (for [o (:support result)]
                                     [o (exact-delta (get row o) (get reference o))]))
                   decimal-sum (reduce + 0M (map (comp bigdec str val) row))]
               [id {:production row :independent-exact-reference reference
                    :deltas-exact deltas
                    :max-absolute-double-delta
                    (reduce max 0.0 (map #(Math/abs (double %)) (vals deltas)))
                    :production-ieee-exact-sum (reduce + (map exact-ieee-ratio (vals row)))
                    :production-coordinate-decimal-sum decimal-sum
                    :production-reduce-sum (reduce + (vals row))
                    :reference-exact-sum (reduce + (vals reference))
                    :admission (machine-model/row-sum-admission row)}]))))
(def report {:schema :wm/predictive-outcome-production-match-v1 :row 6
             :authority (:authority result) :support (:support result)
             :policy-horizons (:policy-horizons result) :comparisons comparisons
             :repeated-plan-equal (= (get-in result [:rows "advance-twice"])
                                     (get-in result [:rows "advance-twice-repeat"]))
             :changed-plan-differs (not= (get-in result [:rows "advance-twice"])
                                         (get-in result [:rows "advance-then-cascade"]))
             :reference-method :exact-ieee-rational-independent-transition-and-positional-a
             :failed-attempt-history
             {:circular-readback-removed true
              :mathlib4-attempt "c86eed2bba8e9060d6a4824d7a257bd87f9e1b91"
              :mathlib4-removal "f893753dfc"}
             :negative-controls controls})
(spit "holes/labs/wm-contract/runs/row-6-predictive-outcome/readback.edn" (pr-str report))
(prn report)
