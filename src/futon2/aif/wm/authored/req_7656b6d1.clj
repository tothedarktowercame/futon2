(ns futon2.aif.wm.authored.req-7656b6d1
  "Authored operational clauses for held executable cascade
  7ea0e637a587e5a9a00045b35fecd34f326cd9d83eb8d4ca1cecbbd42ba8d593
  (phase :compile, requisition
  7656b6d1716cf4699d24076d7a576b77b22ccaac4674ed79cff391fe5fdd6205).

  Resolutions for the nine :operational-clause-owed holes of the
  T-kangaroo-caching-and-compaction selected-target reading (ticket
  futon3c/holes/tickets/T-kangaroo-caching-and-compaction.md, opening
  excerpt before the first section).  Each decision is an operational
  clause attributed to the reading's occurrence squares over that
  excerpt: the operation names what the occurrence enacts in the ticket
  text, completion-check names the observable condition in that text,
  and :consumes/:produces describe the transition.  Nothing here claims
  a square-derived completion and no application-order edge is
  authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["agent/scope-before-action" 0]
   {:operation :frame-ticket-header-with-deadline
    :capability :wm/pattern-apply
    :consumes :ticket-title-and-next-day-banner
    :produces :bounded-work-named-before-any-action
    :completion-check
    [:header-names-target-and-deadline
     "the ticket header reads '# T-kangaroo-caching-and-compaction — TOMORROW (2026-06-11)', so the work is named and time-bounded before anyone acts on it"]}
   ["agent/state-is-hypothesis" 1]
   {:operation :revise-status-to-partial-via-triage
    :capability :wm/pattern-apply
    :consumes :standing-ticket-plan-taken-as-current
    :produces :triage-dated-partial-status-with-obsolete-parts
    :completion-check
    [:status-banner-reads-partial-with-triage-date
     "the status banner reads '**Status (triaged 2026-09-01): PARTIAL -- research portions obsolete.**', so the earlier claim is revised rather than defended"]}
   ["aif/valuation-reads-the-paperwork" 2]
   {:operation :ground-unimplemented-claim-in-counted-evidence
    :capability :wm/pattern-apply
    :consumes :planned-rollout-and-recalibration-items
    :produces :zero-occurrence-record-with-current-threshold
    :completion-check
    [:parenthetical-carries-counts-and-threshold
     "the sentence records 'never implemented (0 occurrences; threshold still 2 MiB)', so the not-done judgement is conditioned on recorded paperwork, not assertion"]}
   ["agent/evidence-over-assertion" 3]
   {:operation :name-shipped-compaction-mechanisms
    :capability :wm/pattern-apply
    :consumes :shipped-work-claim
    :produces :checkable-mechanism-names-warm-compact-and-cold-monster
    :completion-check
    [:sentence-names-both-shipped-mechanisms
     "the sentence names 'warm /compact and default-on cold monster compaction both shipped', so the shipped claim points at concrete checkable artifacts"]}
   ["problems/tension-proposes-candidates" 4]
   {:operation :emit-threshold-recalibration-as-decision-candidate
    :capability :wm/pattern-apply
    :consumes :superseded-scope-plus-one-open-item
    :produces :explicit-decision-question-separate-from-close-out
    :completion-check
    [:directive-splits-close-from-decide
     "the directive reads 'Close the superseded parts; decide whether threshold recalibration is still wanted.', so the open item is proposed as a decision rather than dropped"]}
   ["agent/pause-is-not-failure" 5]
   {:operation :record-deliberate-park-with-resumption-context
    :capability :wm/pattern-apply
    :consumes :end-of-day-owner-gated-state
    :produces :parked-note-not-abandonment
    :completion-check
    [:parked-line-carries-date-and-follow-on-context
     "the ticket records 'Parked at end of day 2026-06-10.' followed by the three linked follow-ons, so the pause is a first-class record with resumption context"]}
   ["agent/handoff-preserves-context" 6]
   {:operation :carry-follow-on-linkage-to-joey-gate
    :capability :wm/pattern-apply
    :consumes :ephemeral-end-of-day-session-state
    :produces :persistent-ticket-scoped-follow-on-list
    :completion-check
    [:ticket-lists-three-linked-follow-ons
     "the ticket reads 'Three linked follow-ons to the joey gate', so a successor resumes with the prior-decision linkage intact"]}
   ["aif/valuation-reads-the-paperwork" 7]
   {:operation :pin-dependency-to-exact-commit
    :capability :wm/pattern-apply
    :consumes :follow-on-dependency-reference
    :produces :commit-pinned-conditioning-identity
    :completion-check
    [:dependency-names-commit-042fad2
     "the ticket anchors the dependency with '(commit 042fad2) and the prompt-caching accounting.', so later valuations condition on an identified text state"]}
   ["agent/scope-before-action" 8]
   {:operation :gate-start-on-owner-presence
    :capability :wm/pattern-apply
    :consumes :pickup-intent-for-the-ticket
    :produces :unstarted-ticket-until-joe-confirms-scope
    :completion-check
    [:sentence-forbids-starting-without-joe
     "the ticket closes with 'Do NOT start without Joe.', so substantive action waits for the owner's scope confirmation"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "7ea0e637a587e5a9a00045b35fecd34f326cd9d83eb8d4ca1cecbbd42ba8d593")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
