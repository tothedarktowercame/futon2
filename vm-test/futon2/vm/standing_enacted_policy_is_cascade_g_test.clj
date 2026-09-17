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

  Note: `judge` on {} reads the live mission/ticket substrate for candidates.
  That affects which action is chosen, not the grain these assertions check."
  (:require [clojure.test :refer [deftest is]]
            [futon2.report.war-machine :as wm]))

(deftest standing-enacted-policy-is-cascade-g
  (let [decision (:decision (wm/judge {}))
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
        "the enacted choice records the declared common horizon T it was scored at")))
