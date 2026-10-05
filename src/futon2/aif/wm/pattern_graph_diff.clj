(ns futon2.aif.wm.pattern-graph-diff
  "Produce an inspectable, unapplied pattern-graph diff from one WM run."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [futon2.aif.load-identity :as load-identity]))

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

(defn- qualified-want
  "A pattern's :produces holds [target token] pairs on a selected action and
   bare tokens on an interpretation that has not been located. The want
   accounting is keyed by the pair."
  [target want]
  (if (vector? want) want [target want]))

(defn- want-token [[_target token]] (graph-id token))

(defn- outcome-index [accounting]
  (if (= :verified (:status accounting))
    (reduce-kv (fn [m class wants]
                 (reduce #(assoc %1 %2 (name class)) m wants))
               {} (:by-class accounting))
    {}))

(defn- use-entry [target position pattern outcomes accounting-verified?]
  {:pattern (graph-id (:id pattern))
   :position position
   :wants (->> (:produces pattern)
               (map #(qualified-want target %))
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
        target (or target (:target selected-action))
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
                                  (use-entry target position pattern outcomes accounting-verified?))
                                positioned)
                          [])
              :add_edges (if enacted?
                           (vec (for [left positioned right positioned
                                      :when (and (< (first left) (first right))
                                                 ;; a pattern repeated in the
                                                 ;; cascade is not linked to itself
                                                 (not= (:id (second left))
                                                       (:id (second right))))]
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

(defn- file-bytes [path]
  (java.nio.file.Files/readAllBytes (.toPath (io/file path))))

(defn publish!
  "Write one run's unapplied diff, returning typed absence instead of throwing."
  [input graph-path output-dir]
  (let [graph-bytes (try (file-bytes graph-path) (catch Exception _ nil))
        run-id (:run-id input)]
    (cond
      ;; The run id names the file, and futon3c's apply names its copy by it.
      (not (and (string? run-id) (seq run-id)
                (= run-id (.getName (io/file run-id)))))
      {:status :not-written :reason :run-id-unusable-as-file-name :run-id run-id}

      (not graph-bytes)
      {:status :not-written :reason :pattern-graph-unreadable :path graph-path}

      :else
      (try
        (let [path (str (io/file output-dir
                                 (str (:run-id input) ".pattern-graph-diff.json")))
              graph {:path graph-path :sha256 (load-identity/sha256 graph-bytes)}
              proposal (pattern-graph-diff (assoc input :graph graph))]
          (write-diff! path (assoc input :graph graph))
          {:status :written :path path
           :sha256 (load-identity/sha256 (file-bytes path))
           :uses (count (:add_uses proposal))
           :links (count (:add_edges proposal))
           :nothing-to-add (:nothing_to_add proposal)})
        (catch Exception e
          {:status :not-written :reason :pattern-graph-diff-write-failed
           :message (.getMessage e)})))))
