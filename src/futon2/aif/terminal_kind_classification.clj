(ns futon2.aif.terminal-kind-classification
  "Committed authority for every terminal keyword the War Machine may emit."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def resource-path "wm/terminal-kind-classification.edn")

(defonce ^:private table*
  (delay
    (with-open [reader (io/reader (or (io/resource resource-path)
                                      (throw (ex-info "Terminal-kind table missing"
                                                      {:resource resource-path}))))]
      (edn/read (java.io.PushbackReader. reader)))))

(defn table [] @table*)

(defn classified? [kind]
  (contains? (table) kind))

(defn terminal-kind
  "Return a terminal keyword carried by a phase result, if it has one."
  [result]
  (when (map? result)
    (or (when (keyword? (:failure-kind result)) (:failure-kind result))
        (when (contains? #{:error :refused :abstained} (:status result))
          (or (when (keyword? (:kind result)) (:kind result))
              (when (keyword? (:reason result)) (:reason result)))))))

(defn unclassified-condition
  "Turn an unknown terminal keyword into a debugger condition.  This function
  is called only by `run-phase!` while the debugger is attached."
  [phase result]
  (when-let [kind (terminal-kind result)]
    (when-not (classified? kind)
      (ex-info "Terminal kind has no committed classification"
               {:kind :unclassified-terminal-kind
                :original-kind kind
                :seam phase
                :failure-stage phase
                :terminal-result result}))))
