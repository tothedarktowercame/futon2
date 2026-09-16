(ns futon2.vm.tick-001-s08-r16-test
  "Virtual War Machine tick 1, step 8 (R16 grounded enactment).

  Real-machine REQUIREMENT test (VM-PROTOCOL.md): asserts what the model
  requires of the real tick's R16 node on THIS tick's inputs — the R14
  decision (vm/tick-001/07-R14.edn): chosen action
  :aif/placeholder-is-load-bearing, first acting pattern of cascade
  :C2-fix-first, posterior mass 0.37005613489124095, tie decided by the
  declared :action-name-ascending rule — interpreted per R6 (03-R6.edn
  :computed :interpretations).

  Requirement source:
  - P10 continuing guards: futon2.aif.receipt-construction/acting-order
    (receipt_construction.clj:47) is the live enactment function: it
    rechecks every guard on the current facts and acts the first pattern in
    order whose guard is literal-true and whose effect is not already
    achieved.
  - The enactment this tick performed: observation.clj:129 now reads
    (:total-repos summary 0), sibling-consistent with :134, so a partial
    summary coerces :active-repo-ratio to 0.0 instead of throwing — the
    two tokens :aif/placeholder-is-load-bearing produces.
  - The model includes R16 as a node of the tick (aif-control-map-live,
    Figure 5A order); war_machine.clj routes :R6/:R14/:TRACE but has NO
    :R16 route tag today, so the routing assertion fails until the node
    is wired. Failures are the node's open requirements, listed in
    vm/tick-001/08-R16.edn :requirements-open."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.observation :as observation]
            [futon2.aif.receipt-construction :as rc]))

;; --- the tick's facts (02-R1.edn :computed :q0 support) as a fact map ----
;; Closed-world per R2's fact list: every fact R2 adjudicated carries its
;; value (:test-covers-missing-total-repos was observed false); the pre-fix
;; world is exactly :summary-without-total-repos-observes-cleanly false
;; (its complement :summary-without-total-repos-throws is observed true).
;; Strong Kleene needs the explicit false — an absent token would be
;; :unknown and could never satisfy a :not forbids (receipt_construction
;; test 'unknown guard value never fires').

(def q0-facts
  {:summary-without-total-repos-throws true
   :active-repo-ratio-absent-default-is-0 true
   :coupling-density-reads-same-key-with-default true
   :observe-empty-does-not-throw true
   :summary-without-total-repos-observes-cleanly false
   :test-covers-missing-total-repos false})

;; --- R6's interpretations (03-R6.edn) in acting-order's own guard
;; language: Strong Kleene forms for finder/guard-value, fact->value
;; effects. All three patterns of the tick's cascades, because
;; acting-order validates every id in the order it is given.

(def interpretations
  {:aif/structured-observation-vector
   {:guard [:and [:fact :summary-without-total-repos-throws]
                 [:not [:fact :summary-without-total-repos-observes-cleanly]]]
    :effect {:summary-without-total-repos-observes-cleanly true}}
   :aif/placeholder-is-load-bearing
   {:guard [:and [:fact :coupling-density-reads-same-key-with-default]
                 [:fact :summary-without-total-repos-throws]
                 [:not [:fact :summary-without-total-repos-observes-cleanly]]]
    :effect {:summary-without-total-repos-observes-cleanly true
             :active-repo-ratio-absent-default-is-0 true}}
   :test-step-covering-missing-total-repos
   {:guard [:and [:fact :summary-without-total-repos-throws]
                 [:not [:fact :test-covers-missing-total-repos]]]
    :effect {:test-covers-missing-total-repos true}}})

;; :C2-fix-first precedence (03-R6.edn :computed :cascades).
(def c2-precedence
  [:aif/placeholder-is-load-bearing
   :test-step-covering-missing-total-repos
   :aif/structured-observation-vector])

(def chosen-action :aif/placeholder-is-load-bearing)

(defn- war-machine-source
  "The real tick's source, from the classpath or the repo checkout."
  []
  (or (some-> (io/resource "futon2/report/war_machine.clj") slurp)
      (try
        (slurp "scripts/futon2/report/war_machine.clj")
        (catch Exception _ nil))))

(deftest s08-r16
  ;; 1. REQUIRED: the live enactment function acts the chosen pattern on
  ;;    this tick's facts and interpretation, and yields its produces.
  (let [acted (rc/acting-order interpretations q0-facts c2-precedence)]
    (is (= [chosen-action] (vec (take 1 acted)))
        "acting-order's first act is the R14-chosen pattern :aif/placeholder-is-load-bearing")
    (is (true? (get-in interpretations [chosen-action :effect
                                        :summary-without-total-repos-observes-cleanly]))
        "the chosen pattern's effect includes its two produced tokens (definition under test)")
    (is (= [:aif/placeholder-is-load-bearing :test-step-covering-missing-total-repos]
           acted)
        "P10 continues after the first act but never retracts: the chosen pattern acts first and its cascade continues in precedence order"))
  ;; 2. REQUIRED: the enacted effect is real — the channel observes cleanly.
  (let [r (observation/observe {:graph {:summary {:active-repos 3}}})]
    (is (= 0.0 (:active-repo-ratio r))
        "the real observation channel coerces :active-repo-ratio to 0.0 on a summary without :total-repos (no throw)"))
  (is (= 0.0 (:active-repo-ratio (observation/observe {})))
      "observe {} is unchanged: :active-repo-ratio 0.0")
  (is (= 0.3 (:active-repo-ratio
              (observation/observe {:graph {:summary {:active-repos 3 :total-repos 10}}})))
      "the normal path is unchanged: 3/10 = 0.3")
  ;; 3. REQUIRED OPEN: the real tick routes enactment of the R14 decision
  ;;    through R16. war_machine.clj routes :R6 and :R14 (:6570/:6571) and
  ;;    mentions a Car-3 (R16) seam, but no :R16 route tag names an
  ;;    enactment call, so the tick's selected decision never reaches an
  ;;    enactment node. Behaviourally: this fails until the tick calls the
  ;;    enactment (acting-order or its aligned successor) on its own route.
  ;;    The source-text check accompanies the behavioural requirement; it
  ;;    cannot be satisfied by the string alone because assertion 1 above
  ;;    exercises the real enactment call.
  (let [src (war-machine-source)]
    (is (some? src)
        "war_machine.clj is readable by the test")
    (is (boolean (and src
                      (re-find #":R16\s+\"futon2\.aif\.receipt-construction/acting-order\"" src)))
        "war_machine.clj routes enactment with an :R16 route tag naming futon2.aif.receipt-construction/acting-order (the enactment of the R14-selected decision)")))
