(ns futon2.aif.cascade-evaluation-trace
  "Read-only consistency checking of retained evaluation records. No runtime
   access, model replay, gate changes, or authority claims. The checker uses
   the captured candidate/model values and the declared transition semantics."
  (:require [clojure.set :as set]))

(defn- enabled? [pattern state]
  (and (= :interpreted (get-in pattern [:guard :status]))
       (every? (fn [{:keys [present absent]}]
                 (and (set/subset? present state)
                      (empty? (set/intersection absent state))))
               (get-in pattern [:guard :clauses]))
       (not (set/subset? (or (:produces pattern) (get-in pattern [:transition :produces]) #{}) state))))

(defn- effective-pattern [p]
  (if (contains? p :theta) p (assoc p :theta 1 :theta-source :documented-default)))

(defn- expected-kernel [pattern state]
  (if-not pattern
    {state 1}
    (let [target (set/union state (:produces pattern))
          theta (:theta pattern)]
      (if (= state target)
        {state 1}
        (into {} (remove (comp zero? val)) {target theta state (- 1 theta)})))))

(defn- sum-contributions [rows]
  (reduce (fn [acc row] (merge-with + acc (:mass-contribution row))) {} rows))

(defn- state-errors [precedence row]
  (let [{:keys [state mass selected-index pattern-id kernel guard-search mass-contribution]} row
        first-index (first (keep-indexed #(when (enabled? %2 state) %1) precedence))
        expected-pattern (when (some? first-index) (get precedence first-index))
        searched (if (some? first-index) (inc first-index) (count precedence))
        expected-search (mapv (fn [i]
                                {:index i :pattern-id (:id (get precedence i))
                                 :guard-verdict (enabled? (get precedence i) state)
                                 :applied? (= i first-index)})
                              (range searched))
        expected (expected-kernel expected-pattern state)]
    (cond-> []
      (not= :evaluated (:status row)) (conj :state-not-evaluated)
      (not= first-index selected-index) (conj :not-first-enabled)
      (not= (:id expected-pattern) pattern-id) (conj :wrong-pattern-id)
      (not= expected-search guard-search) (conj :guard-search-mismatch)
      (not= (if expected-pattern :pattern-kernel :identity) (:kernel-kind row))
      (conj :wrong-kernel-kind)
      (not= expected kernel) (conj :applied-kernel-mismatch)
      (not= (update-vals kernel #(* mass %)) mass-contribution)
      (conj :state-contribution-mismatch))))

(defn- step-errors [candidate step]
  (let [precedence (mapv effective-pattern (:precedence candidate))
        model (:model step)
        incoming (:incoming-belief step)
        rows (:states step)
        per-state (mapcat (fn [row] (map #(hash-map :kind % :state (:state row))
                                        (state-errors precedence row))) rows)]
    (into
     (cond-> []
       (not= :evaluated (:status step)) (conj {:kind :step-not-evaluated})
       (not= {:schema :wm/cascade-evaluation-model-v1
              :semantics :first-enabled-union-theta-v1 :precedence precedence} model)
       (conj {:kind :model-candidate-mismatch})
       (or (not= (count incoming) (count rows))
           (not= incoming (into {} (map (juxt :state :mass)) rows)))
       (conj {:kind :incoming-state-coverage})
       (not= (sum-contributions rows) (:outgoing-belief step))
       (conj {:kind :outgoing-contributions-mismatch}))
     per-state)))

(defn- trace-errors [{:keys [id status horizon evaluations]}]
  (let [errors (mapcat (fn [step]
                        (map #(assoc % :tau (:tau step)) (step-errors id step))) evaluations)
        continuity (for [[a b] (partition 2 1 evaluations)
                         :when (not= (:outgoing-belief a) (:incoming-belief b))]
                     {:kind :broken-horizon-link :tau (:tau b)})]
    (map #(assoc % :candidate id)
         (concat
          (cond-> []
            (not= :recorded status) (conj {:kind :missing-evaluations})
            (not (and (pos-int? horizon) (= horizon (count evaluations))
                      (= (mapv :tau evaluations) (vec (range 1 (inc horizon))))))
            (conj {:kind :horizon-coverage}))
          errors continuity))))

(defn validate-record
  "Validate joins and arithmetic using only the supplied run record. Removing
   the trace, shortening it, corrupting a contribution/link/guard selection,
   or marking an unselected pattern applied yields :invalid with reasons."
  [record]
  (try
    (let [certificate (get-in record [:decision :selection-certificate])
          ids (mapv :id (:candidates certificate))
          traces (:node-evaluation-traces certificate)
          trace-ids (mapv :id traces)
          errors (into (cond-> []
                         (or (empty? ids) (not= ids trace-ids)
                             (not= (count ids) (count (set ids))))
                         (conj {:kind :candidate-join-mismatch}))
                       (mapcat trace-errors traces))]
      {:status (if (empty? errors) :valid :invalid) :errors errors})
    (catch Exception e
      {:status :invalid :errors [{:kind :malformed-evaluation-record
                                 :message (.getMessage e)}]})))
