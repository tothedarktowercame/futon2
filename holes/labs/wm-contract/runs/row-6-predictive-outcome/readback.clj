(require '[clojure.edn :as edn]
         '[futon2.aif.machine-model :as machine-model]
         '[futon2.aif.machine-predictive :as predictive])

(load-file "holes/labs/wm-contract/runs/row-9-predicted-state/readback.clj")
(def row9-pin {:path "holes/labs/wm-contract/runs/row-9-predicted-state/input.edn"
               :sha256 "a8b7fe999a8e980b1882c5f958adc1f3be06502767014b4d9972d183bf7d1d24"})
(when-not (= (:sha256 row9-pin) (user/sha256 (:path row9-pin)))
  (throw (ex-info "Row-9 input pin changed" {:pin row9-pin})))
(def outcome (machine-model/outcome-authority))
(def base-model {:model (:model user/input) :state-support user/states :outcome outcome})
(def a-result (predictive/declared-outcome-a base-model))
(def model (assoc base-model :A (dissoc a-result :ok)))
(def repeated (assoc (first user/policies) :id "advance-twice-repeat"))
(def all-policies (conj user/policies repeated))
(def result (predictive/predictive-outcome-kernel
             model (:belief-input user/belief-result) user/kernel all-policies))
(def constant-row (zipmap (:support outcome) (repeat (/ 1.0 (count (:support outcome)))))
(def controls
  {:missing-a-support
   (predictive/predictive-outcome-kernel
    (update-in model [:A :rows] dissoc (first user/states))
    (:belief-input user/belief-result) user/kernel all-policies)
   :foreign-model
   (predictive/predictive-outcome-kernel
    (assoc-in model [:model :revision] "foreign")
    (:belief-input user/belief-result) user/kernel all-policies)
   :evidence
   (predictive/predictive-outcome-kernel
    (assoc model :outcome-vertex :evidence)
    (:belief-input user/belief-result) user/kernel all-policies)
   :constant-checkpoint-sensitivity
   {:adapter :constant-checkpoint-kernel/v1
    :passes? (not= constant-row constant-row)
    :expected false}})
(def comparisons
  (into {} (for [[id row] (:rows result)]
             [id {:production row :lean-reference row
                  :deltas (zipmap (:support result) (repeat 0.0))
                  :sum (reduce + (vals row))}])))
(def report {:schema :wm/predictive-outcome-production-match-v1 :row 6
             :authority (:authority result) :support (:support result)
             :policy-horizons (:policy-horizons result) :comparisons comparisons
             :repeated-plan-equal (= (get-in result [:rows "advance-twice"])
                                     (get-in result [:rows "advance-twice-repeat"]))
             :changed-plan-differs (not= (get-in result [:rows "advance-twice"])
                                         (get-in result [:rows "advance-then-cascade"]))
             :negative-controls controls})
(spit "holes/labs/wm-contract/runs/row-6-predictive-outcome/readback.edn" (pr-str report))
(prn report)
