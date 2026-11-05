(ns futon2.aif.wm.authored.req-0fee33d3
  "Authored operational clauses for held executable cascade
  b4054c26e05f4d5d46385ac6bdc1f75cd7390e7ba970bd5ee159e57b531a2be9
  (phase :compile, requisition 0fee33d3dfb6d47450195d18663636c0fb54dc634630bfd05db877d9af100939).

  Resolutions for the nine :operational-clause-owed holes of the
  T-jvm-provenance-before-multi-jvm reading (analysis digest
  a223d0df5714a74d45d1bac9979604cf0ba4a649e2aec4cd378aeb79273e84dc).
  Each decision is an operational clause attributed to the reading's
  occurrence squares: the operation names what the occurrence enacts in
  the ticket text, completion-check names the observable condition in
  that text, and :consumes/:produces describe the transition.  Nothing
  here claims a square-derived completion and no application-order edge
  is authored."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def clauses
  {["invariant-coherence/shape-first-identify" 0]
   {:operation :record-jvm-provenance-identify-gap
    :capability :wm/pattern-apply
    :consumes :ticket-title-fragment
    :produces :identify-gap-record
    :completion-check
    [:title-names-say-what-code-it-is
     "ticket title states a JVM 'cannot currently say what code it is'"]}
   ["invariant-coherence/single-locus" 0]
   {:operation :assert-one-provenance-record-per-jvm
    :capability :wm/pattern-apply
    :consumes :identify-gap-record
    :produces :jvm-identity-locus-requirement
    :completion-check
    [:gap-read-as-missing-locus
     "the identify gap is read as each running JVM lacking its one authoritative record of which code it is"]}
   ["contracts/holder-states-the-claim" 1]
   {:operation :record-verbatim-operator-quote-holder
    :capability :wm/pattern-apply
    :consumes :ticket-opening-header
    :produces :quote-holder-record
    :completion-check
    [:header-names-claude-13-and-joe
     "ticket header reads 'Opened: 2026-08-28 · claude-13, from Joe:' marking the quoted words as Joe's"]}
   ["invariant-coherence/reachable-from-boot" 2]
   {:operation :trace-one-jvm-rule-construction-path
    :capability :wm/pattern-apply
    :consumes :laptop-premise-fragment
    :produces :premise-construction-path-record
    :completion-check
    [:laptop-premise-named
     "ticket quotes the one-JVM setup as made 'to accommodate my laptop'"]}
   ["invariant-coherence/shape-first-identify" 3]
   {:operation :gate-multiple-jvms-on-identify
    :capability :wm/pattern-apply
    :consumes :capacity-claim-fragment
    :produces :identify-before-multiply-gate
    :completion-check
    [:capacity-claim-read-as-unmapped
     "the claim that 'we could afford to have multiple JVMs' is read as a goal conditioned on the identification capability, not a design"]}
   ["invariant-coherence/single-locus" 4]
   {:operation :enumerate-isolation-host-loci
    :capability :wm/pattern-apply
    :consumes :host-list-fragment
    :produces :isolation-host-loci-record
    :completion-check
    [:three-hosts-named-optional
     "ticket names /home/joe/, /home/dionysus/ and /home/apollo/ as hosts 'in case we wanted' isolation"]}
   ["contracts/every-entry-has-a-falsifier" 6]
   {:operation :void-resource-premise-by-named-falsifier
    :capability :wm/pattern-apply
    :consumes :status-line-premise-fragment
    :produces :premise-void-verdict
    :completion-check
    [:resource-premise-declared-void
     "status line reads 'The resource premise of the one-JVM rule is void' while the identifiability premise is tested separately"]}
   ["invariant-coherence/shape-first-identify" 7]
   {:operation :record-provenance-as-precondition
    :capability :wm/pattern-apply
    :consumes :precondition-fragment
    :produces :precondition-record
    :completion-check
    [:ticket-named-the-precondition
     "status line reads 'this ticket is the precondition' for multi-JVM adoption"]}
   ["invariant-coherence/reachable-from-boot" 7]
   {:operation :require-boot-rooted-jvm-provenance
    :capability :wm/pattern-apply
    :consumes :precondition-record
    :produces :boot-rooted-provenance-requirement
    :completion-check
    [:identifiability-premise-survives
     "the status line keeps the identifiability premise standing, so each JVM's code identity must be reconstructible rather than session folklore"]}})

(defn decisions
  "The hole resolutions for this requisition, bound to the cascade digest
  they repair.  Returns the decision map keyed by hole id when the context
  names this cascade, else a typed mismatch (never a silent wrong answer)."
  [{:keys [cascade-digest phase] :as _ctx}]
  (if (and (= cascade-digest
              "b4054c26e05f4d5d46385ac6bdc1f75cd7390e7ba970bd5ee159e57b531a2be9")
           (= :compile phase))
    clauses
    {:status :error
     :kind :patch-cascade-context-mismatch
     :cascade-digest cascade-digest
     :phase phase}))
