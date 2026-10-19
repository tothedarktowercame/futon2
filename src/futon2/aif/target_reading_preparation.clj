(ns futon2.aif.target-reading-preparation
  "Resumable pre-choice reading preparation. Never fabricates an outer choice."
  (:require [futon2.aif.selected-target-reading :as reading]))

(defn plan
  "Extract exact missing/stale reading needs from a pre-choice field.
  Current readings are reused. Other failures remain visible, not reclassified."
  [field]
  (let [failures (for [family (:families field), failure (:failures family)]
                   (assoc failure :target-id (:target-id family)))
        reading? #(contains? #{:no-current-target-reading :stale-target-reading}
                             (:kind %))]
    {:schema :wm/target-reading-preparation-plan-v1
     :field-source-pin (:source-pin field)
     :needs (reading/prechoice-needs field)
     :other-failures (vec (remove reading? failures))}))

(defn prepare!
  "Execute a finite batch through the existing validated publication path.
  The caller supplies an absolute deadline and a batch limit. Each published
  reading is durable in the registry, so replanning skips completed work.
  :prepared means readings published, NOT that policy selection succeeded."
  [opts field {:keys [deadline-ms limit now-fn repair-fn]
               :or {limit 8 now-fn #(System/currentTimeMillis)
                    repair-fn reading/repair-need!}}]
  (when-not (and (integer? deadline-ms) (pos-int? limit))
    (throw (ex-info "Preparation requires a deadline and positive batch limit"
                    {:kind :reading-preparation-invalid-bounds})))
  (let [{:keys [needs other-failures] :as planned} (plan field)]
    (loop [pending needs, records []]
      (let [failed? (some #(not (:published %)) records)
            reason (cond
                     failed? :publication-refused
                     (empty? pending) :batch-complete
                     (>= (count records) limit) :batch-limit
                     (>= (now-fn) deadline-ms) :deadline)]
        (if reason
          {:schema :wm/target-reading-preparation-v1
           :status (cond failed? :refused (seq pending) :pending :else :prepared)
           :reason reason :deadline-ms deadline-ms
           :field-source-pin (:field-source-pin planned)
           :records records :pending pending :other-failures other-failures}
          (let [need (first pending)
                result (try (repair-fn opts need)
                            (catch Exception e
                              {:status :refused :published false
                               :kind :reading-preparation-threw
                               :message (ex-message e)}))
                record (assoc result :need need)]
            (recur (if (:published record) (subvec pending 1) pending)
                   (conj records record))))))))
