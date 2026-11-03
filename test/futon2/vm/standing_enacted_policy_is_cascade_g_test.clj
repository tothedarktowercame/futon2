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
            [futon2.report.war-machine :as wm]
            [futon2.test-support.reading-route :as route]))

;; The production judge reads the repair store through its default root; in a
;; test JVM that root is the hermetic data root (b13461d2e), where the store
;; does not exist and the authority guard refuses. The suite fixture gives the
;; test a store of its own.
(use-fixtures :once hermetic/with-hermetic-stores)

(deftest standing-enacted-policy-is-cascade-g
  ;; Rebuilt 2026-11-03 on the reading-route fixture: since a04b157d2 judge
  ;; takes the caller's task, and since the 2026-11-01 restoration the task's
  ;; policy family comes from its published reading (:target-sources on the
  ;; assembly, :target-reading-root in opts). The call still goes through the
  ;; production `judge`.
  (let [fx (route/fixture)
        assembled (route/assembled fx 1)
        steps (route/ledger-steps fx)]
    (with-redefs [wm/assemble-cascade-problems-with-published
                  (fn [& _] assembled)
                  fold-source/conditioning-steps
                  (fn [& _] steps)]
      (let [decision (:decision
                      (wm/judge {} (merge fixture/live-c-opts (route/opts fx {}))))
            law (:selection-law decision)]
        (is (map? decision) "the production tick produces a decision")
        (is (= :cascade-candidate (get-in decision [:action :kind]))
            (str "the enacted action is a cascade candidate, not a flat :type action; got :type "
                 (get-in decision [:action :type])))
        (is (= :cascade-selection-posterior (:applied law))
            (str "the enacted choice applies the cascade G posterior; got :applied "
                 (:applied law)))
        ;; Since 6a36b6006 (2026-10-30) the reading-family route scores at the
        ;; R20 fixed-count betaPosterior, not an operator-declared beta; the
        ;; decision records that provenance and the value.
        (is (= :r20-computed (get-in decision [:beta :status]))
            "the enacted choice records the R20-computed beta")
        (is (number? (get-in decision [:beta :value])))
        ;; The common horizon each compared policy was scored at is carried by
        ;; the certificate's Q4 carrier (Requirements Q4), per policy key.
        (let [q4 (get-in decision [:selection-certificate :q4])
              enacted (:enacted law)
              row (some #(when (= enacted (:policy-key %)) %) (:policies q4))]
          (is (= :certified (:status q4)))
          (is (number? (:horizon row))
              "the enacted policy's Q4 row records the horizon it was scored at"))))))
