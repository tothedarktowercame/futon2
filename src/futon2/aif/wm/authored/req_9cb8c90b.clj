(ns futon2.aif.wm.authored.req-9cb8c90b
  "Authored operational clauses for held executable cascade
  bcd74783f3a0a6466998415a59756759fd4c20ebdfd6b55675635e60afc565c8
  (phase :compile, requisition
  9cb8c90b86dc99e9fa17e4b64c8ffae6ff0f8a1c6c88f84c4d6397930289a881).

  Resolutions for the six :operational-clause-owed holes of the
  T-ladder-02-langton-loop reading (target holes/tickets/T-ladder-02-langton-loop.md,
  analysis digest 707757a45e7c04b4aaab42e063fe8618a2e0d31959978b2cccf0a402ab39487b).
  Each decision is an operational clause attributed to the reading's
  occurrence squares over that ticket: the operation names what the
  occurrence enacts in the acceptance-section text, completion-check names
  the observable condition in that text, and :consumes/:produces describe
  the transition.  Nothing here claims a square-derived completion and no
  application-order edge is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["contracts/every-entry-has-a-falsifier" 0]
   {:operation :name-the-refuting-behaviour-per-entry
    :capability :wm/pattern-apply
    :consumes :acceptance-section-checklist-fragment
    :produces :entries-with-built-in-falsifiers
    :completion-check
    [:each-entry-names-what-would-refute-it
     "the acceptance entry reads 'A test rejects a deliberately altered transition', naming the refuting behaviour (the table producing a different result for a wrong rule) rather than a value-replay pass"]}
   ["hygiene/receipt-then-gate" 2]
   {:operation :gate-closure-on-recounted-active-rows
    :capability :wm/pattern-apply
    :consumes :transition-table-classified-correct
    :produces :receipted-row-count-of-exactly-219
    :completion-check
    [:fresh-count-of-219-recorded-before-closure
     "closure of the table clause is gated on a freshly re-counted '219 active rows' recorded as a receipt at evaluation time, so silent row loss cannot pass; the count refusing to be taken leaves the clause open"]}
   ["mmca/threshold-shaped-events" 3]
   {:operation :report-cell-count-with-declared-threshold
    :capability :wm/pattern-apply
    :consumes :seed-liveness-count-fragment
    :produces :threshold-qualified-86-cell-report
    :completion-check
    [:quiescence-threshold-stated-alongside-the-count
     "the '86 non-quiescent cells' claim is reported together with the declared quiescence threshold that classifies a cell as active, so the count is not mistaken for a threshold-free property of the seed"]}
   ["math-informal/exploit-symmetry" 4]
   {:operation :judge-replication-on-canonical-forms
    :capability :wm/pattern-apply
    :consumes :raw-parent-and-daughter-configurations
    :produces :translation-rotation-canonical-comparison-at-generation-151
    :completion-check
    [:equality-judged-on-normalised-representatives
     "the daughter 'equal to the parent up to translation and rotation' at generation 151 is judged on canonical (normalised) representatives of the translation/rotation group, not a raw-grid comparison that would misjudge an exactly-replicated but displaced daughter"]}
   ["test-registry/register-the-run" 5]
   {:operation :record-digests-through-a-registered-run
    :capability :wm/pattern-apply
    :consumes :generation-151-replication-verdict
    :produces :normalised-parent-and-daughter-digests-bound-to-the-run
    :completion-check
    [:digests-recorded-bound-to-what-actually-ran
     "the 'normalised parent/daughter digests recorded' are recorded through a registered run — the digests bind exactly what was compared and with what outcome, so the prose replication claim is replaceable by the record"]}
   ["war-room/wr-25-good-news-gets-the-same-evidence-discipline-as-bad" 6]
   {:operation :hold-favourable-expected-outcome-to-the-checks
    :capability :wm/pattern-apply
    :consumes :pre-declared-closed-pass-expectation
    :produces :closure-gated-on-check-artifacts
    :completion-check
    [:good-news-closure-waits-for-the-artifacts
     "the 'Expected outcome: closed / pass' moves nothing by itself: the clause closes only when the altered-transition rejection, the 219-row receipt, the threshold-qualified 86-cell count, and the generation-151 canonical digests are recorded artifacts, the same discipline a failing expectation would face"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "bcd74783f3a0a6466998415a59756759fd4c20ebdfd6b55675635e60afc565c8")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
