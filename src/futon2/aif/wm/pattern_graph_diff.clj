(ns futon2.aif.wm.pattern-graph-diff
  "Produce an inspectable, unapplied pattern-graph diff from one WM run."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]))

(def schema "pattern-graph-diff-v1")

(defn- graph-id [x]
  (cond
    (keyword? x) (if-let [n (namespace x)] (str n "/" (name x)) (name x))
    (symbol? x) (str x)
    :else (str x)))

(defn- verified-enactment? [d-task]
  (let [join (or (get-in d-task [:verification :candidate-to-minted-join])
                 (:candidate-to-minted-join d-task))]
    (and (= :admitted (get-in d-task [:verification :status]))
         (= :verified (:status join))
         (string? (:selected-action-sha256 join))
         (= (:selected-action-sha256 join)
            (:enacted-action-sha256 join)))))

(defn- want-token [[_target token]] (graph-id token))

(defn- outcome-index [accounting]
  (if (= :verified (:status accounting))
    (reduce-kv (fn [m class wants]
                 (reduce #(assoc %1 %2 (name class)) m wants))
               {} (:by-class accounting))
    {}))

(defn- use-entry [position pattern outcomes accounting-verified?]
  {:pattern (graph-id (:id pattern))
   :position position
   :wants (->> (:produces pattern)
               (sort-by want-token)
               (mapv (fn [want]
                       {:want (want-token want)
                        :outcome (if accounting-verified?
                                   (get outcomes want "not-recorded")
                                   "not-recorded")})))})

(defn- pair-edge [run-id [i left] [j right]]
  (let [left-id (graph-id (:id left)) right-id (graph-id (:id right))
        [a b] (sort [left-id right-id])]
    {:a a :b b :kind "used-together"
     :evidence [{:run run-id :order [left-id right-id] :positions [i j]}]}))

(defn pattern-graph-diff
  "Return the proposed graph diff for one retained run. Nothing is applied."
  [{:keys [run-id target selected-action outcome terminal d-task-enactment
           artifact want-outcome-accounting graph]}]
  (let [enacted? (verified-enactment? d-task-enactment)
        patterns (vec (:precedence selected-action))
        positioned (mapv vector (range 1 (inc (count patterns))) patterns)
        accounting-verified? (= :verified (:status want-outcome-accounting))
        outcomes (outcome-index want-outcome-accounting)
        base {:schema schema
              :base graph
              :source {:kind "war-machine-run"
                       :run run-id
                       :target target
                       :run_outcome (some-> outcome name)
                       :terminal (some-> terminal name)
                       :enactment (if enacted? "verified" "not-verified")
                       :want_accounting {:status (some-> (:status want-outcome-accounting) name)
                                         :reason (some-> (:reason want-outcome-accounting) name)}
                       :artifact (select-keys artifact [:repo :commit])}
              :add_uses (if enacted?
                          (mapv (fn [[position pattern]]
                                  (use-entry position pattern outcomes accounting-verified?))
                                positioned)
                          [])
              :add_edges (if enacted?
                           (vec (for [left positioned right positioned
                                      :when (< (first left) (first right))]
                                  (pair-edge run-id left right)))
                           [])}]
    (cond-> base (not enacted?)
      (assoc :nothing_to_add "enactment-not-verified"))))

(defn write-diff!
  "Write INPUT's proposed diff as JSON to PATH and return the diff."
  [path input]
  (let [proposal (pattern-graph-diff input)]
    (io/make-parents path)
    (spit path (str (json/generate-string proposal {:pretty true}) "\n"))
    proposal))
