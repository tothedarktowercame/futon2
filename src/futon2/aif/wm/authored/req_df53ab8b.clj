(ns futon2.aif.wm.authored.req-df53ab8b
  "Authored operational clause for held executable cascade
  3cfadf80023c325ab6232b3311ec1138fbc236d9566f15523159ade448127f3c
  (phase :compile, requisition
  df53ab8b1e6bfcd63449252effcf8173de838bcb48f5d4178e4e831df2d7417a).

  One :operational-clause-owed hole, grounded in the pinned library body
  process-coherence/status-refresh-before-work
  (sha256 8976e27af52c114c3a21ccabdb6f4afc65621c17e4b41fc1c9d2766d9a5a84b2).
  The operation names what the pattern's THEN clause enacts (re-derive the
  phase from the body and dependencies before any other work), the
  completion-check names the pattern's own evidence condition (status line
  updated citing what was checked), and :consumes/:produces describe the
  transition.  Nothing here claims a completion the pattern text does not
  state and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["process-coherence/status-refresh-before-work" 4]
   {:operation :refresh-status-before-work
    :capability :wm/pattern-apply
    :consumes :cached-status-line-and-ticket-body
    :produces :re-derived-status-line
    :completion-check
    [:status-line-cites-what-was-checked
     "the status line was updated to the re-derived phase after reading the body past the banner and chasing each named dependency to its current state, citing what was checked; work begins from the re-derived point, not the cached one"]}})

(defn decisions
  "The hole resolution for this requisition, bound to the cascade digest
  it repairs.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "3cfadf80023c325ab6232b3311ec1138fbc236d9566f15523159ade448127f3c")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
