(ns futon2.aif.outer-task-selection
  "Outer-loop task selection, intentionally ignorant of inner cascade data.

  This boundary chooses an M/E/T(/A) task identity.  Cascade retrieval,
  interpretation, construction and G evaluation happen only after this choice.
  Callers may pass rich task records, but `task-view` projects the closed set of
  fields the outer selector is allowed to observe."
  (:import [java.util SplittableRandom]))

(def schema :wm/outer-task-selection-v1)

(def ^:private kind-order
  {:mission 0 :excursion 1 :ticket 2 :algorithm 3})

(def ^:private action-type
  {:mission :advance-mission
   :excursion :advance-excursion
   :ticket :advance-ticket
   :algorithm :run-algorithm})

(defn task-view
  "Project TASK onto the complete observation surface of the outer selector.
  In particular, cascade candidates, interpretations, precedence and G terms
  cannot cross this boundary."
  [task]
  (select-keys task [:id :kind :status-class :open-hole-count :priority]))

(defn- supported? [{:keys [id kind]}]
  (and (string? id) (not (empty? id)) (contains? kind-order kind)))

(defn select-task
  "Select one current task using an explicit E-only baseline policy.

  The support is sorted by declared numeric priority (lower first), task kind,
  then id.  Selection is a reproducible seeded uniform draw over that support;
  priority is retained as an ordering/audit input but is not yet a probability
  term.  The receipt records the closed observation field, making accidental
  use of inner-loop cascade material testable."
  [{:keys [tasks seed]}]
  (let [projected (mapv task-view (or tasks []))
        support (->> projected
                     (filter supported?)
                     (sort-by (juxt #(if (number? (:priority %)) (:priority %) 0)
                                    #(get kind-order (:kind %)) :id))
                     vec)
        excluded (->> projected (remove supported?) vec)
        n (count support)
        seeded? (integer? seed)
        index (when (and (pos? n) seeded?)
                (.nextInt (SplittableRandom. (long seed)) n))
        chosen (when (some? index) (nth support index))]
    (cond->
     {:schema schema
      :policy {:kind :seeded-uniform-task-support
               :uses [:id :kind :status-class :open-hole-count :priority]
               :forbids [:cascade :candidates :constructed-candidates
                         :interpretations :precedence :g :g-terms]}
      :support support
      :excluded excluded
      :draw (cond
              (zero? n) {:absent :no-selectable-task}
              (not seeded?) {:absent :no-seed}
              :else {:generator "java.util.SplittableRandom"
                     :seed seed :index index :support-count n})
      :chosen (or chosen (if (zero? n)
                           {:absent :no-selectable-task}
                           {:absent :no-seed}))}
      chosen
      (assoc :action {:type (get action-type (:kind chosen))
                      :target (:id chosen)}))))
