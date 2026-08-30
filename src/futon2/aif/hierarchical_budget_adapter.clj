(ns futon2.aif.hierarchical-budget-adapter
  "R11 adapter from separately ranked live proposal fields to the exact
   hierarchical shared-budget arbiter.

   A proposal field is a one-of choice: at most one alternative from a source
   field may be selected. The adapter records the normalized input, source
   field/action identity, budget hierarchy, and complete arbitration output so
   the same boundary can be replayed without consulting live proposer state."
  (:require [futon2.aif.hierarchical-budget :as budget]))

(def ^:private schema-version 1)

(defn- fail! [message data]
  (throw (ex-info message
                  (assoc data :failure-kind :invalid-ranked-proposal-fields))))

(defn- normalize-proposal [field-id source-index proposal]
  (when-not (map? proposal)
    (fail! "Every ranked proposal must be a map"
           {:field-id field-id :source-index source-index :proposal proposal}))
  (when-not (contains? proposal :action)
    (fail! "Every ranked proposal must preserve its source :action"
           {:field-id field-id :source-index source-index :proposal proposal}))
  (-> proposal
      (assoc :rank (or (:rank proposal) (inc source-index))
             :proposal/field-id field-id
             :proposal/source-index source-index
             :proposal/action (:action proposal))))

(defn- normalize-field [field]
  (when-not (map? field)
    (fail! "Every proposal field must be a map" {:field field}))
  (when-not (contains? field :id)
    (fail! "Every proposal field must have an :id" {:field field}))
  (when-not (vector? (:proposals field))
    (fail! "Every proposal field must have a ranked vector of :proposals"
           {:field-id (:id field) :proposals (:proposals field)}))
  {:id (:id field)
   :budget (:budget field)
   :selection-limit 1
   :proposals (mapv #(normalize-proposal (:id field) %1 %2)
                    (range)
                    (:proposals field))})

(defn- normalize-input [{:keys [root-id shared-budget fields context]
                         :or {root-id :r11/shared}
                         :as request}]
  (when-not (map? request)
    (fail! "R11 adapter input must be a map" {:input request}))
  (when-not (contains? request :shared-budget)
    (fail! "R11 adapter input must include :shared-budget" {:input request}))
  (when-not (vector? fields)
    (fail! "R11 adapter input must include a vector of :fields"
           {:fields fields}))
  (cond-> {:schema/version schema-version
           :root-id root-id
           :shared-budget shared-budget
           :fields (mapv normalize-field fields)}
    (contains? request :context) (assoc :context context)))

(defn- input->hierarchy [{:keys [root-id shared-budget fields]}]
  {:id root-id
   :budget shared-budget
   :children fields})

(defn- arbitration-output [normalized-input]
  (let [hierarchy (input->hierarchy normalized-input)]
    (assoc (budget/arbitrate hierarchy)
           :adapter/schema-version schema-version
           :adapter/field-selection :at-most-one-ranked-alternative
           :adapter/input normalized-input
           :adapter/hierarchy hierarchy)))

(defn select-ranked-proposal-fields
  "Select a globally feasible portfolio from separately ranked live fields.

   Input:
     {:shared-budget 9
      :root-id :collective                 ; optional
      :context {:campaign/id campaign-1}    ; optional, retained for evidence
      :fields [{:id :planner-a
                :budget 6
                :proposals [{:id :a1 :rank 1 :action {...}
                             :cost 6 :utility 10}]}]}

   The result includes the arbiter's selected/rejected proposals, shared and
   leaf usage/budgets, oversubscription witness, normalized input and hierarchy,
   plus a self-contained :replay/receipt. Proposal order is evidence: omitted
   ranks are assigned from the incoming vector and never inferred from utility."
  [request]
  (let [normalized-input (normalize-input request)
        output (arbitration-output normalized-input)]
    (assoc output :replay/receipt
           {:schema/version schema-version
            :input normalized-input
            :output output})))

(defn replay
  "Replay a receipt without live proposer state and compare the entire output."
  [{:keys [schema/version input output] :as receipt}]
  (when-not (= schema-version version)
    (fail! "Unsupported R11 replay receipt schema"
           {:receipt receipt :supported-schema-version schema-version}))
  (let [actual (arbitration-output input)]
    {:replay/identical? (= output actual)
     :replay/expected output
     :replay/actual actual}))
