(ns futon2.aif.accumulation-bootstrap
  "Reviewed, one-time bootstrap for the trace-carried Dirichlet state.

   This is deliberately separate from the live judge: a stateless predecessor
   still refuses there.  `bootstrap!` either returns a dry-run record or appends
   one carry-only record under the ordinary trace/index publication lock."
  (:require [clojure.java.io :as io]
            [futon2.aif.lane-futility :as lane-futility]
            [futon2.aif.machine-accumulation :as accumulation]
            [futon2.aif.trace :as trace])
  (:import (java.time LocalDate ZoneOffset)
           (java.time.format DateTimeFormatter)))

(def ^:private date-format (DateTimeFormatter/ofPattern "yyyy-MM-dd"))

(def ^:dynamic *before-locked-append*
  "Test seam invoked after the unlocked preflight and before lock acquisition."
  (fn [] nil))

(defn- typed-refusal
  ([reason] (typed-refusal reason nil))
  ([reason detail]
   (cond-> {:status :absent :reason reason}
     detail (assoc :detail detail))))

(defn- record-identity [record]
  (or (:run/id record) (:timestamp record)))

(defn- valid-config? [config]
  (and (= :wm/accumulation-live-config-v1 (:schema config))
       (some? (:accumulation-entity-id config))
       (= :declared (get-in config [:accumulation-initialization :authority]))
       (number? (get-in config [:accumulation-initialization :prior]))
       (some? (get-in config [:accumulation-initialization :model/revision]))))

(defn- inspect-tail [trace-dir]
  (let [history (trace/read-history-strict 1 :dir trace-dir)]
    (cond
      (not= :ok (:status history))
      {:refusal (typed-refusal :bootstrap-history-unavailable history)}

      (empty? (:records history))
      {:refusal (typed-refusal :bootstrap-history-empty)}

      :else
      (let [tail (peek (:records history))]
        (if (:accumulation-state tail)
          {:refusal (typed-refusal :bootstrap-not-needed
                                   {:predecessor-id (record-identity tail)})}
          {:tail tail :tail-id (record-identity tail)})))))

(defn- prepare-record
  [{:keys [config observation-support state-support authority]} tail-id]
  (cond
    (not (valid-config? config))
    {:refusal (typed-refusal :bootstrap-configuration-invalid)}

    (or (not (map? authority))
        (nil? (:by authority))
        (nil? (:supports-authority authority)))
    {:refusal (typed-refusal :bootstrap-authority-invalid)}

    (or (empty? observation-support) (empty? state-support))
    {:refusal (typed-refusal :bootstrap-support-empty)}

    (or (not= (count observation-support) (count (set observation-support)))
        (not= (count state-support) (count (set state-support))))
    {:refusal (typed-refusal :bootstrap-support-invalid)}

    :else
    (let [observation-support (vec (sort observation-support))
          state-support (vec (sort state-support))
          initialization (:accumulation-initialization config)
          ;; Its own identity: reusing the predecessor's :run/id makes readers
          ;; that join by run id (run_narrative/load-run,
          ;; cross_ledger_identity) refuse the predecessor as ambiguous.
          bootstrap-id (str (.format (LocalDate/now ZoneOffset/UTC) date-format)
                            "-accumulation-bootstrap-of-" tail-id)
          initialized (accumulation/initialize
                       observation-support state-support (:prior initialization))]
      (if-not (:ok initialized)
        {:refusal (typed-refusal :bootstrap-initialization-refused (:refusal initialized))}
        (let [lineage {:entity/id (:accumulation-entity-id config)
                       :model/revision (:model/revision initialization)}
              state (-> initialized
                        (assoc :last-tick bootstrap-id
                               :lineage lineage))]
          {:record
           {:record/kind :accumulation-bootstrap
            ;; The bootstrap is a carry proxy, not a tick.  The next tick's
            ;; previous-id is this record's :run/id, which machine-accumulation/step
            ;; requires to equal the carried state's :last-tick.
            :run/id bootstrap-id
            :bootstrap {:by (:by authority)
                        :authority (:authority authority)
                        :predecessor-id tail-id
                        :supports-authority (:supports-authority authority)}
            :accumulation-state state
            :accumulation-initialization initialization}})))))

(defn- daily-path [trace-dir]
  (io/file trace-dir
           (str "wm-trace-"
                (.format (LocalDate/now ZoneOffset/UTC) date-format)
                ".edn")))

(defn bootstrap!
  "Return a typed refusal, a dry-run bootstrap record, or append exactly one
   bootstrap record under the trace/index lock.

   Required input keys are :trace-dir, :config, :observation-support,
   :state-support, and :authority.  Authority must name :by and
   :supports-authority.  This function must not be aimed at a live trace until
   its dry-run record has been reviewed by the operator."
  [{:keys [trace-dir dry-run?] :as opts}]
  (if-not (and (string? trace-dir) (not-empty trace-dir))
    (typed-refusal :bootstrap-trace-dir-invalid)
    (if dry-run?
      (lane-futility/with-index-lock
       trace-dir
       (fn []
         (let [{:keys [refusal tail-id]} (inspect-tail trace-dir)]
           (if refusal
             refusal
             (let [{:keys [refusal record]} (prepare-record opts tail-id)]
               (or refusal {:status :dry-run :record record}))))))
      (let [{:keys [refusal tail-id]} (inspect-tail trace-dir)]
        (if refusal
          refusal
          (let [{prepare-refusal :refusal} (prepare-record opts tail-id)]
            (if prepare-refusal
              prepare-refusal
              (do
                (*before-locked-append*)
                (try
                  (let [publication
                        (lane-futility/append-indexed-trace!
                         trace-dir (daily-path trace-dir) nil
                         (fn [_]
                           (let [{locked-refusal :refusal locked-tail-id :tail-id}
                                 (inspect-tail trace-dir)]
                             (cond
                               locked-refusal
                               (throw (ex-info "Bootstrap refused under publication lock"
                                               {:bootstrap/refusal locked-refusal}))

                               (not= tail-id locked-tail-id)
                               (throw (ex-info "Bootstrap predecessor changed"
                                               {:bootstrap/refusal
                                                (typed-refusal :bootstrap-stale-predecessor
                                                               {:expected tail-id
                                                                :actual locked-tail-id})}))

                               :else
                               (:record (prepare-record opts locked-tail-id))))))]
                    {:status :bootstrapped
                     :path (:path publication)
                     :record (:record publication)})
                  (catch clojure.lang.ExceptionInfo e
                    (or (:bootstrap/refusal (ex-data e))
                        (throw e))))))))))))
