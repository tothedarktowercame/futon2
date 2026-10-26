(ns futon2.aif.reading-preparation-outcome
  "Typed projection of the selected-target-reading step of a click.

  Pure reader over the selection judgement and whatever record
  full-loop-runner/selected-target-reading-step returned as :record.  No
  IO, never throws, total over nil and unknown shapes.  Never copies the
  reading analyses (:records/:frontier) wholesale."
  (:require [futon2.aif.load-identity :as identity]
            [futon2.aif.selected-target-reading :as reading]))

(identity/register! *ns* *file*)

(def schema :wm/reading-preparation-outcome-v1)

(defn- selected-need-safe
  "selected-need is pure in the live sources; guard anyway so outcome is
  total over adversarial judgements."
  [judgement]
  (try (reading/selected-need judgement)
       (catch Throwable _ nil)))

(defn outcome
  "Project [judgement record] into one typed {:schema
  :wm/reading-preparation-outcome-v1 :status ...} map.  record is nil, a
  :wm/target-reading-preparation-v1 map, a
  :wm/selected-target-reading-request-v1 map, or anything else
  (:unrecognised)."
  [judgement record]
  (let [base {:schema schema
              :policy (select-keys
                       (get-in judgement [:outer-task-selection :policy])
                       [:status :reason])
              :chosen (or (get-in judgement [:outer-task-selection :chosen :id])
                          {:absent :no-chosen-target})}]
    (cond
      (nil? record)
      (if-let [need (selected-need-safe judgement)]
        (assoc base :status :not-run :reason :reading-fn-absent
               :need (select-keys need [:target :failure :excerpt-digest]))
        (assoc base :status :not-run :reason :no-selected-need))

      (= :wm/target-reading-preparation-v1 (:schema record))
      (assoc base :status :ran
             :reason (:reason record)
             :preparation-status (:status record)
             :deadline-ms (:deadline-ms record)
             :field-source-pin (:field-source-pin record)
             :attempted (mapv #(get-in % [:need :target]) (:records record))
             :published (mapv #(get-in % [:need :target])
                              (filter :published (:records record)))
             :refused (mapv #(get-in % [:need :target])
                            (remove :published (:records record)))
             :pending (mapv :target (:pending record)))

      (= :wm/selected-target-reading-request-v1 (:schema record))
      (cond-> (assoc base :status :selected-target-reading
                     :target (:target record)
                     :published (boolean (:published record))
                     :request-status (:status record))
        (contains? record :kind)
        (assoc :kind (:kind record)))

      :else
      (assoc base :status :unrecognised :record-schema (:schema record)))))
