(ns futon2.aif.coverage-check
  "Pure validation of the R5 coverage statement carried by a flight close.

  A criterion statement maps named outcomes to booleans.  At least one false
  entry declares the criterion set's boundary.  Missing data is reported as
  :unwitnessable; data that is present but contradicts the close is :failed."
  (:require [clojure.set :as set]))

(def clause-id :r5/coverage-reported)

(def ^:private result-kinds #{:passed :failed :unwitnessable})
(def ^:private statement-path [:payload :coverage-statement])
(def ^:private terminal-outcome-path [:payload :judgment :outcome])
(def ^:private statement-fields #{:criteria :outcome :inside? :report})

(defn- result
  [close kind details]
  (merge {:clause clause-id
          :close-id (:attempt/id close)
          :result kind}
         details))

(defn- unwitnessable
  [close field]
  (result close :unwitnessable {:missing-field field}))

(defn- failed
  [close why]
  (result close :failed {:why why}))

(defn check-coverage
  "Check R5's coverage clause for one close.

  The coverage statement has this shape:

    {:criteria {outcome true, known-outside-outcome false}
     :outcome outcome
     :inside? true
     :report :scored}

  An outside outcome instead carries `:inside? false` and `:report
  :uncovered`.  The terminal outcome remains at `[:payload :judgment
  :outcome]`."
  [close]
  (let [statement (get-in close statement-path ::missing)
        terminal-outcome (get-in close terminal-outcome-path ::missing)]
    (cond
      (= ::missing statement)
      (unwitnessable close :coverage-statement)

      (not (map? statement))
      (failed close :coverage-statement-not-a-map)

      (seq (set/difference statement-fields (set (keys statement))))
      (unwitnessable close
                     (first (sort (set/difference statement-fields
                                                  (set (keys statement))))))

      (= ::missing terminal-outcome)
      (unwitnessable close :judgment/outcome)

      (not (map? (:criteria statement)))
      (failed close :criteria-not-a-map)

      (not-every? boolean? (vals (:criteria statement)))
      (failed close :criteria-values-not-boolean)

      (not (some false? (vals (:criteria statement))))
      (failed close :criterion-set-declares-no-boundary)

      (not (contains? (:criteria statement) (:outcome statement)))
      (failed close :outcome-omitted-from-criterion-set)

      (not= terminal-outcome (:outcome statement))
      (failed close :coverage-outcome-does-not-match-terminal)

      (not (boolean? (:inside? statement)))
      (failed close :inside-not-boolean)

      (not= (:inside? statement)
            (get (:criteria statement) (:outcome statement)))
      (failed close :inside-contradicts-criterion-set)

      (and (false? (:inside? statement))
           (not= :uncovered (:report statement)))
      (failed close :outside-outcome-not-recorded-as-uncovered)

      (and (true? (:inside? statement))
           (not= :scored (:report statement)))
      (failed close :covered-outcome-not-recorded-as-scored)

      :else
      (result close :passed {}))))

(defn summarize
  "Return per-result counts and the individual clause results.

  The summary intentionally has no aggregate success boolean: callers must
  retain the difference between failed and unwitnessable clauses."
  [closes]
  (let [results (mapv check-coverage closes)]
    {:clause clause-id
     :counts (merge (zipmap result-kinds (repeat 0))
                    (frequencies (map :result results)))
     :results results}))
