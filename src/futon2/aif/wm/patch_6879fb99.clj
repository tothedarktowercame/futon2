(ns futon2.aif.wm.patch-6879fb99
  "Repair patch for requisition 6879fb99fc9e3e63ef06857517c6c0633fd37a2a0ef1e4f63e0718ad848cc1b1,
   cascade 9242fd6ebe09443f21402a62ba472f49c6b8da39e0a5d166ecf58049155523b6 phase :compile.
   Supplies the operational clause owed for each held hole in T-fail-invoke-error."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(defn decisions
  "Function of the resume context {:cascade-digest :phase}; returns the
  operational clause map keyed by hole id."
  [{:keys [cascade-digest phase] :as _ctx}]
  (when (and cascade-digest phase)
    {["agency/loud-failure" 0]
     {:operation :apply
      :capability :wm/pattern-apply
      :completion-check [:failure-class-named-in-census]
      :consumes [:census-class-field]
      :produces [:attributed-invoke-error]}
     ["agency/loud-failure" 1]
     {:operation :apply
      :capability :wm/pattern-apply
      :completion-check [:ticket-still-open-with-triage-need]
      :produces [:explicit-open-verdict]}
     ["data-mining/checkpoint-the-long-run" 2]
     {:operation :apply
      :capability :wm/pattern-apply
      :completion-check [:triage-reads-accumulated-payloads]
      :consumes [:accumulated-failure-census]
      :produces [:exception-payload-triage]}
     ["agency/loud-failure" 3]
     {:operation :apply
      :capability :wm/pattern-apply
      :completion-check [:census-block-parseable-between-markers]
      :produces [:machine-readable-census]}
     ["agency/delivery-receipt" 4]
     {:operation :apply
      :capability :wm/pattern-apply
      :completion-check [:terminal-code-read-from-invoke-jobs]
      :consumes [:invoke-jobs-terminal-code]
      :produces [:explicit-dispatch-failure-count]}
     ["data-mining/checkpoint-the-long-run" 5]
     {:operation :apply
      :capability :wm/pattern-apply
      :completion-check [:window-bounds-persisted-with-count]
      :produces [:bounded-census-window]}
     ["agency/delivery-receipt" 6]
     {:operation :apply
      :capability :wm/pattern-apply
      :completion-check [:failing-edge-named-with-both-agents]
      :produces [:attributed-dispatch-edge]}
     ["agency/delivery-receipt" 7]
     {:operation :apply
      :capability :wm/pattern-apply
      :completion-check [:stage-attribution-present]
      :produces [:stage-localized-failures]}
     ["agency/loud-failure" 8]
     {:operation :apply
      :capability :wm/pattern-apply
      :completion-check [:evidence-field-cited]
      :produces [:queryable-error-surface]}
     ["agency/loud-failure" 9]
     {:operation :apply
      :capability :wm/pattern-apply
      :completion-check [:census-block-delimited-at-end]
      :produces [:closed-machine-readable-census]}}))
