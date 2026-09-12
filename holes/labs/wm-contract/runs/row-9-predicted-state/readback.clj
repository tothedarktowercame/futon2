(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.machine-belief :as machine-belief]
         '[futon2.aif.machine-model :as machine-model]
         '[futon2.aif.machine-predictive :as predictive])
(import '[java.security MessageDigest])

(def run-dir "holes/labs/wm-contract/runs/row-9-predicted-state")
(def input (edn/read-string (slurp (str run-dir "/input.edn"))))

(defn sha256 [path]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (java.nio.file.Files/readAllBytes (.toPath (io/file path))))]
    (apply str (map #(format "%02x" (bit-and 255 %)) digest))))
(defn pinned-read [{:keys [path sha256] :as pin}]
  (when-not (= sha256 (user/sha256 path))
    (throw (ex-info "Pinned input changed" {:pin pin})))
  (edn/read-string (slurp path)))

(def row7 (pinned-read (:row-7 input)))
(def row8 (pinned-read (:row-8 input)))
(def capture (nth (:source row7) (get-in input [:row-7 :capture-index])))
(def entity (:entity/id capture))
(def posterior (:posterior capture))
(def states (:state-support row8))
(def actions (get-in row8 [:real-inputs :action-snapshot :classes]))
(defn point [s] (assoc (zipmap states (repeat 0)) s 1))
(def rows (into {} (for [[[s a] successor] (get-in row8 [:complete-rows :successor])]
                         [[s a] (point successor)])))
(def kernel {:ok true :model (:model input) :state-support states
             :action-support actions :authority :declared-prior
             :name (get-in row8 [:authority :name]) :rows rows})
(def context {:entity/id entity :model (:model input)
              :state-support machine-belief/state-support :mode :single-entity
              :policy-entities [entity entity]})
(def belief-result (machine-belief/belief-state-distribution context {entity posterior}))
(def policies (mapv #(assoc % :entity/id entity :model/revision (get-in input [:model :revision]))
                    (:policies input)))
(def production (predictive/predicted-state-kernel (:belief-input belief-result) kernel policies))

;; Reference recurrence over the pinned successor table, separate from the
;; production apply-belief function. These are the values of the Lean sum for
;; the point-mass B rows at the identical support/model pins.
(defn reference-step [q action]
  (reduce (fn [out [s mass]]
            (update out (get-in row8 [:complete-rows :successor [s action]]) + mass))
          (zipmap states (repeat 0)) q))
(defn reference-steps [q plan]
  (reductions reference-step q plan))
(def references (into {} (for [p policies]
                           [(:id p) (vec (reference-steps posterior (:actions p)))])))
(def comparisons
  (into {}
        (for [p policies
              :let [id (:id p) actual (get-in production [:trajectories id])
                    expected (get references id)]]
          [id (mapv (fn [step reference]
                      {:depth (:depth step) :action (:action step)
                       :production (:distribution step) :lean-reference reference
                       :deltas (into {} (for [s states]
                                          [s (- (double (get-in step [:distribution s]))
                                                (double (get reference s)))]))
                       :production-sum (reduce + (vals (:distribution step)))
                       :admission (:admission step)})
                    actual expected)])))
(def base-policy (first policies))
(def controls
  {:empty-policy (predictive/predicted-state-plan
                  (:belief-input belief-result) kernel (assoc base-policy :actions []))
   :undeclared-action (predictive/predicted-state-plan
                       (:belief-input belief-result) kernel
                       (assoc base-policy :actions [:advance-mission :foreign]))
   :model-revision-mismatch (predictive/predicted-state-plan
                             (:belief-input belief-result) kernel
                             (assoc base-policy :model/revision "foreign"))})
(def report
  {:schema :wm/predicted-state-production-match-v1 :row 9
   :pins (:row-7 input) :transition-pin (:row-8 input)
   :belief-admission (machine-model/row-sum-admission posterior)
   :production-ok (:ok production) :comparisons comparisons
   :horizon-control {:same-first-action true
                     :terminals-differ
                     (not= (get-in production [:rows "advance-twice"])
                           (get-in production [:rows "advance-then-cascade"]))
                     :terminals (select-keys (:rows production)
                                             ["advance-twice" "advance-then-cascade"])}
   :negative-controls controls})
(spit (str run-dir "/readback.edn") (pr-str report))
(prn report)
