(ns futon2.aif.finite-step-g
  "Pure finite sums for `DarkTower.WarMachine.CascadeEFE.stepG`.

  The supplied A is a model kernel with caller-recorded provenance, not an
  observed estimate.  This namespace performs no authority conversion and no
  I/O; it is the generic arithmetic used where an analytic Lean model must not
  be mislabeled to pass `cascade-g/step-g`'s observed-estimate boundary.")

(defn risk
  "KL(prediction || preference), or `:inadmissible` (Lean infinity) when a
  positive predicted mass meets zero preference mass."
  [prediction preference]
  (if (some (fn [[outcome mass]]
              (and (pos? mass) (not (pos? (get preference outcome 0)))))
            prediction)
    :inadmissible
    (reduce + 0.0
            (for [[outcome mass] prediction :when (pos? mass)]
              (* mass (Math/log (/ mass (get preference outcome))))))))

(defn entropy
  "Categorical entropy, with zero-mass terms omitted."
  [row]
  (- (reduce + 0.0
             (for [[_ mass] row :when (pos? mass)]
               (* mass (Math/log mass))))))

(defn push
  "Marginalize q through a finite categorical kernel."
  [q kernel]
  (let [outcomes (set (mapcat keys (vals kernel)))]
    (into {}
          (for [outcome outcomes]
            [outcome (reduce + 0.0
                             (for [[state mass] q]
                               (* mass (get-in kernel [state outcome] 0))))]))))

(defn ambiguity
  "Expected row entropy over exactly the states supplied by q."
  [q kernel]
  (reduce + 0.0
          (for [[state mass] q]
            (* mass (entropy (get kernel state {}))))))

(defn step-g
  "Return the finite risk, ambiguity and their sum.  Inadmissibility is
  retained in every affected field rather than coerced to a finite number."
  [{:keys [q kernel preference]}]
  (let [prediction (push q kernel)
        r (risk prediction preference)
        a (ambiguity q kernel)]
    {:prediction prediction
     :risk r
     :ambiguity a
     :g (if (= :inadmissible r) :inadmissible (+ r a))}))
