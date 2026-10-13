(ns futon2.vm.standing-enacted-policy-is-cascade-g-test
  "STANDING requirement (Joe, 2026-09-17, claude-8's finding; FOCUS.md priority 0).

  The production tick's ENACTED choice must be selected by G over cascade
  candidates, at the declared common T and declared beta, through
  cascade-selection's posterior. It must not come from the controller head or
  channel scoring over flat :type actions.

  This test calls the production `judge` WITHOUT :cascade-problem, i.e. the
  path the machine actually runs. The VM cascade lane (`cascade-lane`), shadow
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
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.enactment-fold-source :as fold-source]
            [futon2.report.cascade-decision-test :as fixture]
            [futon2.report.preference-schedule-decision-test :as schedule-fixture]
            [futon2.report.war-machine :as wm]))

(deftest standing-enacted-policy-is-cascade-g
  (let [assembled (#'fixture/assemble*
                   {:targets [fixture/tick-1-target]
                    :sources fixture/tick-1-sources})
        history (#'schedule-fixture/admitted-history assembled)]
    (with-redefs [wm/assemble-cascade-problems-with-published
                  (fn [& _] assembled)
                  fold-source/conditioning-steps
                  (fn [& _] history)]
      (let [decision (:decision (wm/judge {} fixture/live-c-opts))
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
