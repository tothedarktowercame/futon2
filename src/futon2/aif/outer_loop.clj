(ns futon2.aif.outer-loop
  "The loop entry's flight path (H-T-CALLER-I): read the target field, let the
  outer cascade choose, and plan the flight on the chosen target.

  This namespace exists so that `scripts/wm_scheduled_run.clj` can reach flight
  code ONLY when `FUTON_WM_FLIGHT` asks for it: the script resolves
  `plan-from-field!` lazily and requires nothing from here at load time, so the
  default tick touches no flight code.

  `plan-from-field!` PLANS. It calls `flight-driver/plan`, which sends nothing
  and writes nothing; running the flight is not here (no flight before the
  spike is belled)."
  (:require [futon2.aif.flight-driver :as driver]
            [futon2.aif.outer-cascade :as outer-cascade]
            [futon2.aif.target-field :as target-field]))

(defn plan-from-field!
  "OPTS: :trigger (which clock fired), :seed (an integer; the wall clock in ms
  when absent, and it goes on the record either way), :seat (the seat that would
  answer the flight's asks; a typed absence when not given), and for tests
  :load-field-fn (default `target-field/load-field`), :plan-fn (default
  `flight-driver/plan`) and :plan-opts (merged into the plan's options: :read-text,
  :observe, :id).
  Returns {:selection the outer cascade's record, :plan the flight's plan}; when
  no target is eligible, :plan is {:absent :no-eligible-target} and nothing is
  planned. The chosen entry's :repo and :path come from the field's :considered
  entry for it (the feasible entry does not carry them)."
  [{:keys [trigger seed seat load-field-fn plan-fn plan-opts]
    :or {load-field-fn target-field/load-field plan-fn driver/plan}}]
  (let [{:keys [field opts]} (load-field-fn)
        seed (if (integer? seed) seed (System/currentTimeMillis))
        chosen (outer-cascade/select {:field field :seed seed :trigger trigger})
        target (:chosen-target chosen)
        entry (first (filter #(= target (:target %)) (:feasible field)))
        considered (first (filter #(= target (:target %)) (:considered field)))]
    (if-not target
      {:selection (:selection chosen) :plan {:absent :no-eligible-target}}
      {:selection (:selection chosen)
       :plan (plan-fn (merge {:chosen-target target
                              :draw-seed (:draw-seed chosen)
                              :selection (:selection chosen)
                              :field-entry entry
                              :repo (:repo considered)
                              :path (:path considered)
                              :seat (or seat {:absent :no-seat-configured})
                              :sources (:sources opts)
                              :store (:store opts)
                              :code-root (:code-root opts)}
                             plan-opts))})))
