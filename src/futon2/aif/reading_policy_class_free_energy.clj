(ns futon2.aif.reading-policy-class-free-energy
  "Per-reading-policy class-channel F from retained completed-run records.")

(def terminal-classes #{:focused :related :unrelated :stop-the-line})

(defn- q6-terminal [record]
  (some #(when (and (map? %) (= :wm/q6-terminal-v1 (:schema %))) %)
        (tree-seq coll? seq record)))

(defn retained-tick
  "Extract one typed class observation. SOURCE is {:path p :record r}; malformed,
  incomplete, and nonterminal records are not observations."
  [{:keys [path record]}]
  (let [choice (:choice (q6-terminal record))
        class (get-in record [:run-ending-classification :class])]
    (when (and (= :present (:status choice))
               (vector? (:policy-key choice))
               (contains? terminal-classes class))
      {:policy-key (:policy-key choice)
       :class class
       :pin {:path (str path)
             :run-id (or (:run/id record) (:run-id record))}})))

(defn select-history [policy-key window sources]
  (when-not (nat-int? window)
    (throw (ex-info "Class-F history window must be a natural number"
                    {:kind :invalid-class-f-history-window :window window})))
  (->> sources
       (keep retained-tick)
       (filter #(= policy-key (:policy-key %)))
       (take-last window)
       vec))

(defn evaluate
  "Evaluate the exact-posterior class F. PREDICTION is P(class|policy).
  A zero-evidence retained class is a typed impossibility, never skipped."
  [policy-key window sources prediction]
  (let [history (select-history policy-key window sources)]
    (loop [ticks history value 0.0]
      (if-let [{:keys [class pin]} (first ticks)]
        (let [z (double (get prediction class 0.0))]
          (if (zero? z)
            {:status :observation-impossible-under-policy
             :value nil :policy-key policy-key :window window :history history
             :failure {:run pin :class class :evidence z}}
            (recur (next ticks) (+ value (- (Math/log z))))))
        {:status :computed :value value :policy-key policy-key
         :window window :history history}))))
