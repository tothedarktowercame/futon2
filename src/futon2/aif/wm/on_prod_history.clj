(ns futon2.aif.wm.on-prod-history
  "Durable production checkpoints for selection-free potted-history resume.

  This is not a replay executor.  It captures the exact selected entry at the
  production runner boundary immediately before cascade execution, so a later
  registered production resume does not have to select or reconstruct it."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.data-paths :as data-paths]
            [futon2.aif.interpretation-evidence :as evidence]))

(def schema :wm/on-prod-history-checkpoint-v1)
(def default-root (data-paths/path "wm-on-prod-history" "checkpoints"))

(defn- sealed [record]
  (assoc record :digest (evidence/value-digest (dissoc record :digest))))

(defn valid? [record]
  (and (= schema (:schema record))
       (string? (:source-run-id record))
       (string? (:source-attempt-id record))
       (= :cascade-execution (:phase record))
       (map? (:selected-entry record))
       (map? (:action (:selected-entry record)))
       (= (:digest record)
          (evidence/value-digest (dissoc record :digest)))))

(defn checkpoint!
  "Append-only capture of ENTRY immediately before production cascade execution.
  An existing identical checkpoint is idempotent; differing bytes at the same
  run/attempt address are a typed collision and are never overwritten."
  ([input] (checkpoint! default-root input))
  ([root {:keys [run-id attempt-id selected-entry selection-receipt
                 world-at-selection]}]
   (let [record (sealed
                 {:schema schema
                  :source-run-id run-id
                  :source-attempt-id attempt-id
                  :phase :cascade-execution
                  :selected-entry selected-entry
                  :selection-receipt selection-receipt
                  :world-at-selection world-at-selection})
         path (io/file root run-id (str attempt-id ".edn"))]
     (when-not (valid? record)
       (throw (ex-info "invalid on-production history checkpoint"
                       {:kind :on-prod-history-checkpoint-invalid})))
     (if (.exists path)
       (let [existing (edn/read-string (slurp path))]
         (when-not (= existing record)
           (throw (ex-info "on-production history checkpoint collision"
                           {:kind :on-prod-history-checkpoint-collision
                            :path (.getPath path)}))))
       (do (io/make-parents path)
           (spit path (pr-str record))))
     {:status :checkpointed :path (.getPath path) :digest (:digest record)})))

(defn load-checkpoint
  ([run-id attempt-id]
   (load-checkpoint default-root run-id attempt-id))
  ([root run-id attempt-id]
   (let [path (io/file root run-id (str attempt-id ".edn"))
         record (edn/read-string (slurp path))]
     (when-not (valid? record)
       (throw (ex-info "on-production history checkpoint seal broken"
                       {:kind :on-prod-history-checkpoint-seal-broken
                        :path (.getPath path)})))
     record)))
