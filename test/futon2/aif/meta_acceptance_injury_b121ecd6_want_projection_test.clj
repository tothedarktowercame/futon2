(ns futon2.aif.meta-acceptance-injury-b121ecd6-want-projection-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]))

(deftest ticket-cascade-outputs-survive-as-selected-wants
  (let [target "T-car3-queue-routing-design-note"
        occurrence ["agent/scope-before-action" 0]
        output [target :pattern-done occurrence]
        action {:kind :cascade-candidate
                :id (str target "/policy")
                :target target
                :want []
                :observation-locators {}
                :precedence
                {:co-apply
                 {:patterns
                  {occurrence
                   {:id occurrence :target target :produces #{output}}}}}}
        selected (:selected-action
                  (runner/construct-selected-action {:action action}))]
    (is (= [[target [:pattern-done occurrence]]]
           (:want selected)))))
