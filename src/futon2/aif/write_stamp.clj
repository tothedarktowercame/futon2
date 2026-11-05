(ns futon2.aif.write-stamp
  "Append-only attribution stamps for files written during a WM click."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.time Instant]))

(def ^:private append-lock (Object.))

(defn record!
  "Append one JSON attribution row. A non-click caller writes nothing."
  [{:keys [root click-id run-id writer path]}]
  (if (or (not (string? click-id)) (str/blank? click-id))
    {:stamped? false :reason :no-click-id}
    (let [target (io/file root (str click-id ".jsonl"))
          row {:path (.getCanonicalPath (io/file path))
               :writer writer
               :run-id run-id
               :click-id click-id
               :at (str (Instant/now))}
          line (str (json/generate-string row) "\n")]
      (locking append-lock
        (io/make-parents target)
        (spit target line :append true))
      {:stamped? true :path (.getCanonicalPath target) :stamp row})))
