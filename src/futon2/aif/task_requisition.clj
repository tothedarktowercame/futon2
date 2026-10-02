(ns futon2.aif.task-requisition
  "The requisition state shared by task registries and task-field views."
  (:require [clojure.string :as str]))

(def states
  {"in-progress" :in-progress "completed" :completed})

(defn read-state
  "Read the first nonblank line below TEXT's H1 as a requisition declaration.
  Returns a state, a malformed line, or a typed absence."
  [text]
  (let [lines (str/split-lines (str text))
        after-h1 (next (drop-while #(not (re-matches #"^#\s+\S.*$" %)) lines))
        line (first (drop-while str/blank? after-h1))]
    (if-not (and line (re-find #"^\*\*Requisition:\*\*" line))
      {:absent :no-requisition}
      (let [[_ word rest] (re-matches #"^\*\*Requisition:\*\*\s+(\S+)\s*(.*)$" line)
            state (states word)
            text (str/trim (str/replace (str rest) #"^[—-]\s*" ""))]
        (if-not state
          {:malformed line}
          (cond-> {:state state} (seq text) (assoc :text text)))))))

(defn ineligible-reason [requisition]
  (when-let [state (:state requisition)]
    (keyword "requisition" (name state))))
