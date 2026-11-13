(ns futon2.aif.wm.authored.req-20070196
  "Authored operational clauses for held executable cascade
  84df23848cbe2bfba259abb6ee15499105e0e1c658fa17ec23311d66ca46b06d
  (phase :compile, requisition
  2007019652afed74e09b845659caca9c4f0e1e388187135cbf951a840027134d).

  Resolutions for the five :operational-clause-owed holes of the
  T-ladder-01-loop-turns-over reading (target
  futon2 data/wm-executable-cascades/84df2384*.edn, policy
  T-ladder-01-loop-turns-over/policy-edf8b8d5820da1e437a5).  Each decision is
  an operational clause attributed to the reading's occurrence square over
  that ticket's acceptance criteria: the operation names what the occurrence
  enacts in the ticket text, completion-check names the observable condition
  in that text, and :consumes/:produces describe the transition.  No
  application-order edge is authored here."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["contracts/every-entry-has-a-falsifier" 0]
   {:operation :retain-complete-command-output
    :capability :wm/pattern-apply
    :consumes :verification-run-command-output
    :produces :complete-output-retained-as-refuting-evidence
    :completion-check
    [:retained-evidence-contains-complete-command-output
     "the retained evidence contains the complete command output, not a truncation or summary, so any failing behaviour remains visible in it"]}
   ["apparatus/done-is-observed-running" 1]
   {:operation :observe-exit-status-zero
    :capability :wm/pattern-apply
    :consumes :verification-run-command
    :produces :exit-0-observed-in-retained-output
    :completion-check
    [:command-exits-0
     "the command exits 0 and that observed exit status is part of the retained record, so done-ness does not diverge from the actual run"]}
   ["contracts/holder-states-the-claim" 2]
   {:operation :parse-counts-from-retained-output
    :capability :wm/pattern-apply
    :consumes :retained-complete-command-output
    :produces :test-assertion-counts-parsed-from-output
    :completion-check
    [:counts-parsed-from-that-output
     "the test/assertion counts are parsed from that retained output rather than carried by a summary word, so the stated claim is checkable against the evidence"]}
   ["contracts/holder-states-the-claim" 3]
   {:operation :state-expected-outcome-in-advance
    :capability :wm/pattern-apply
    :consumes :acceptance-item-with-expected-disposition
    :produces :expected-outcome-stated-as-closed-pass
    :completion-check
    [:expected-outcome-closed-pass
     "the ticket states 'Expected outcome: **closed / pass**.' exactly, in advance, so the achieved state can be checked against the stated one"]}
   ["apparatus/success-must-not-resemble-failure" 4]
   {:operation :report-failure-without-in-ticket-repair
    :capability :wm/pattern-apply
    :consumes :failing-run-observation
    :produces :failure-reported-as-failed-distinct-from-pass
    :completion-check
    [:failure-reported-as-failed
     "a failure is not repaired within this ticket; it is reported as failed, so a failed run never ends in the same 'closed / pass' state as a passing one"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "84df23848cbe2bfba259abb6ee15499105e0e1c658fa17ec23311d66ca46b06d")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
