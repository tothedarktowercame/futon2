(ns futon2.aif.seat-call-index
  "Durable identity ledger for model seats invoked during one WM click."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io])
  (:import [java.io FileOutputStream]
           [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]
           [java.time Instant]))

(def default-path
  "/home/joe/code/futon2/data/wm-click-loop/seat-calls.jsonl")

(def ^:dynamic *path* nil)

(defn index-path []
  (or *path* (System/getenv "FUTON_WM_SEAT_CALL_INDEX") default-path))

(defn job-id
  "Retain a job id when the invoke adapter exposed one, otherwise type its absence."
  [result]
  (or (get-in result [:invoke-meta :job-id])
      (:job-id result)
      {:status :typed-missing :reason :job-id-not-retained}))

(defn append!
  "Append one logical seat invocation. FileChannel locking makes Futon2 and
  Futon3c writers share the JSONL safely."
  [{:keys [click-id parent-click-id run-id role phase job-id seat session-id at]}]
  (let [path (index-path)
        row (cond-> {:schema "wm/click-seat-call-v1"
                     :click-id (or click-id {:status :typed-missing
                                             :reason :click-id-not-threaded})
                     :role role :phase phase
                     :job-id (or job-id {:status :typed-missing
                                         :reason :job-id-not-retained})
                     :seat seat :session-id session-id
                     :at (or at (str (Instant/now)))}
              parent-click-id (assoc :parent-click-id parent-click-id)
              run-id (assoc :run-id run-id))
        bytes (.getBytes (str (json/generate-string row) "\n")
                         StandardCharsets/UTF_8)]
    (io/make-parents path)
    (with-open [stream (FileOutputStream. path true)
                channel (.getChannel stream)
                _lock (.lock channel)]
      (.write channel (ByteBuffer/wrap bytes)))
    row))
