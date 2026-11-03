(ns futon2.vm.standing-enacted-policy-is-cascade-g-test
  "STANDING requirement (Joe, 2026-09-17, claude-8's finding; FOCUS.md priority 0).

  The production tick's ENACTED choice must be selected by G over cascade
  candidates, at the declared common T and declared beta, through
  cascade-selection's posterior. It must not come from the controller head or
  channel scoring over flat :type actions.

  This test calls the production `judge` WITHOUT :cascade-problem and with the
  caller-supplied task used by the runtime, i.e. the path the machine actually
  runs. The VM cascade lane (`cascade-lane`), shadow
  G and contract bundles do not satisfy it. It fails until the production
  decision itself is a cascade decision. Do not weaken, skip or re-point it
  to the lane.

  The invariant proof controls the two current input producers (published
  construction and admitted flight history) while exercising the real
  production `judge`; live substrate selection has its own repairable
  query-time-slice boundary and cannot be this deterministic proof.

  Moved into the DEFAULT suite on 2026-09-17 (SPEC-flat-removal E2: \"moved
  into the default suite once it passes\"), from vm-test, where it had been
  the requirement the production tick failed. It costs ~1 minute: `judge`
  observes each declared cascade source's facts, and a C1/C2 fact runs the
  Test Registry check in its own JVM."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.enactment-fold-source :as fold-source]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.report.cascade-decision-test :as fixture]
            [futon2.report.preference-schedule-decision-test :as schedule-fixture]
            [futon2.report.war-machine :as wm]))

;; The production judge reads the repair store through its default root; in a
;; test JVM that root is the hermetic data root (b13461d2e), where the store
;; does not exist and the authority guard refuses. The suite fixture gives the
;; test a store of its own.
(use-fixtures :once hermetic/with-hermetic-stores)

(deftest standing-enacted-policy-is-cascade-g
  (let [assembled (#'fixture/assemble*
                   {:targets [fixture/tick-1-target]
                    :sources fixture/tick-1-sources})
        history (#'schedule-fixture/admitted-history assembled)]
    (with-redefs [wm/assemble-cascade-problems-with-published
                  (fn [& _] assembled)
                  fold-source/conditioning-steps
                  (fn [& _] history)]
      (let [decision (:decision
                      (wm/judge {} (assoc fixture/live-c-opts
                                         :task {:id (name fixture/tick-1-target)})))
            law (:selection-law decision)]
        (is (map? decision) "the production tick produces a decision")
        (is (= :cascade-candidate (get-in decision [:action :kind]))
            (str "the enacted action is a cascade candidate, not a flat :type action; got :type "
                 (get-in decision [:action :type])))
        (is (= :cascade-selection-posterior (:applied law))
            (str "the enacted choice applies the cascade G posterior; got :applied "
                 (:applied law)))
        (is (= :declared (get-in decision [:beta :status]))
            "the enacted choice records a declared beta")
        (is (number? (get-in decision [:horizon-steps]))
            "the enacted choice records the declared common horizon T it was scored at")))))
