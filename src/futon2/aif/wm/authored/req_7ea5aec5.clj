(ns futon2.aif.wm.authored.req-7ea5aec5
  "Authored operational clauses for held executable cascade
  ef169981296db61de689e27051db7c6c2bf8dd8815469450104b903651e45327
  (phase :compile, requisition
  7ea5aec5c349447e02e1b8f9a3894dd93a00e1c948b1327946ff56e32e74f35a).

  Resolutions for the four :operational-clause-owed holes of the
  T-ladder-03-mmca-von-neumann-mirror reading (analysis digest
  0f7ad66f833dc5755b5cde8f84805d70404d771f53cbf58cd0f31247e1f44d24).
  Each decision is an operational clause attributed to the reading's
  occurrence squares over that ticket's acceptance item: the operation
  names what the occurrence enacts in the acceptance-section text,
  completion-check names the observable condition in that text, and
  :consumes/:produces describe the transition.  Nothing here claims a
  square-derived completion and no application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["ukrns/coding-provenance" 1]
   {:operation :lay-out-one-auditable-row-per-class
    :capability :wm/pattern-apply
    :consumes :per-class-candidate-claims
    :produces :class-keyed-table-with-one-row-per-class
    :completion-check
    [:every-class-has-exactly-one-row
     "the delivered document gives each class its own row — candidate, supporting locator, support kind — so no class-level claim hides inside prose; a class with nothing to put in its row shows up as a visible 'none' row rather than disappearing"]}
   ["ukrns/coding-provenance" 2]
   {:operation :cite-locator-and-kind-per-row
    :capability :wm/pattern-apply
    :consumes :candidate-supporting-evidence
    :produces :rows-citing-file-and-line-or-tn-line-with-declared-kind
    :completion-check
    [:each-row-names-a-locator-and-a-support-kind
     "every row states the file and line or the TN line that supports its candidate, and whether that support is a computed result or a reading, so a 'how do you know?' challenge gets a specific answer instead of 'someone coded it'"]}
   ["pattern-mining/probe-the-claimed-property-not-the-acceptance-proxy" 2]
   {:operation :probe-the-claimed-support-not-the-filled-row
    :capability :wm/pattern-apply
    :consumes :row-with-cited-locator
    :produces :support-verified-against-the-cited-trust-closure
    :completion-check
    [:row-acceptance-rests-on-the-cited-evidence-itself
     "a row counts as supported only because the cited file/line or TN line actually backs the candidate for that class — a plausible-sounding candidate or an analogous class filling the row is a proxy and does not close it"]}
   ["ukrns/coding-provenance" 3]
   {:operation :require-every-cited-path-to-resolve
    :capability :wm/pattern-apply
    :consumes :table-of-cited-paths
    :produces :all-citations-resolving-in-the-sandbox
    :completion-check
    [:fresh-resolution-check-of-every-cited-path
     "every path cited anywhere in the document is checked to resolve in the sandbox at evaluation time, so no row leans on a dead reference; an unresolvable citation leaves the clause open"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "ef169981296db61de689e27051db7c6c2bf8dd8815469450104b903651e45327")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
