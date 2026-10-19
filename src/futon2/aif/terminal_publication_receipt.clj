(ns futon2.aif.terminal-publication-receipt
  "One non-recursive, post-run sidecar for terminal publication attempts.

  The run record has already been streamed before this namespace is entered.
  This receipt therefore pins those exact bytes instead of modifying them."
  (:require [clojure.java.io :as io]
            [futon2.aif.load-identity :as identity])
  (:import [java.nio.file Files StandardCopyOption]
           [java.util UUID]))

(def schema :wm/terminal-publication-receipt-v1)

(defn receipt-path [run-record-path run-id]
  (let [record (io/file run-record-path)]
    (str (io/file (.getParentFile record)
                  (str "terminal-publication-" run-id ".edn")))))

(defn read-run-pin [run-record-path]
  (let [record (io/file run-record-path)]
    (when-not (.isFile record)
      (throw (ex-info "Persisted run record is unavailable"
                      {:kind :terminal-publication-run-record-unavailable
                       :path run-record-path})))
    (let [bytes (Files/readAllBytes (.toPath record))]
      {:path (.getCanonicalPath record)
       :sha256 (identity/sha256 bytes)})))

(defn write-receipt! [path value]
  (let [target (io/file path)
        tmp (io/file (.getParentFile target)
                     (str "." (.getName target) "." (UUID/randomUUID) ".tmp"))]
    (try
      (io/make-parents target)
      (spit tmp (str (pr-str value) "\n"))
      (Files/move (.toPath tmp) (.toPath target)
                  (into-array StandardCopyOption
                              [StandardCopyOption/ATOMIC_MOVE
                               StandardCopyOption/REPLACE_EXISTING]))
      (.getCanonicalPath target)
      (catch Throwable t
        (when (.exists tmp) (.delete tmp))
        (throw t)))))

(defn- attempt [{:keys [sink thunk reason]}]
  (if-not thunk
    {:sink sink :status :not-attempted
     :reason (or reason :sink-unavailable)}
    (try
      (let [result (thunk)]
        ;; Invocation completed is deliberately weaker than sink success.  The
        ;; real injury stores can return typed refusals, so keep their own
        ;; status/reason intact instead of upgrading it here.
        (if (nil? result)
          {:sink sink :status :returned-nil}
          {:sink sink :status :returned
           :sink-status (when (map? result) (:status result))
           :result result}))
      (catch Throwable t
        {:sink sink :status :failed
         :reason (or (:kind (ex-data t)) :sink-threw)
         :error-class (.getName (class t))}))))

(defn sink-result
  "Compatibility projection for the runner's pre-P14 top-level sink fields.
  Returned values retain their exact old value.  New failure observations are
  typed, while an intentionally unattempted sink remains absent as before."
  [publication sink]
  (let [attempt (some #(when (= sink (:sink %)) %) (get-in publication [:receipt :sinks]))]
    (case (:status attempt)
      :returned (:result attempt)
      :returned-nil nil
      :failed (dissoc attempt :sink)
      nil)))

(defn publish!
  "Attempt each supplied sink exactly once and persist their independent results.

  SINKS is an ordered sequence of {:sink keyword :thunk fn}, or an explicit
  {:sink keyword :reason keyword} for a sink that was not attempted.  A sink
  exception is retained as data so remaining sinks still run.  Receipt-write
  failure returns no durable-path claim and never rewrites the run record."
  [run-record-path run-id sinks]
  (try
    (let [receipt {:schema schema
                   :run-id run-id
                   :run-record (read-run-pin run-record-path)
                   :sinks (mapv attempt sinks)}]
      (try
        {:status :present
         :path (write-receipt! (receipt-path run-record-path run-id) receipt)
         :receipt receipt}
        (catch Throwable t
          {:status :failed
           :reason :terminal-publication-receipt-persistence-failed
           :error-class (.getName (class t))
           ;; This is the in-memory observation only, not a claim that it was
           ;; written anywhere.
           :receipt receipt})))
    (catch Throwable t
      ;; Without exact persisted bytes there can be neither a linked receipt
      ;; nor a safe publication attempt.  Do not invent a hash or run sinks
      ;; against an unlinked terminal result.
      {:status :unavailable
       :reason (or (:kind (ex-data t)) :terminal-publication-run-link-unavailable)
       :error-class (.getName (class t))
       :sinks (mapv (fn [{:keys [sink]}]
                       {:sink sink :status :not-attempted
                        :reason :run-record-linkage-unavailable})
                     sinks)})))
