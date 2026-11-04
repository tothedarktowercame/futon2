(ns futon2.aif.meta-acceptance-injury-046ae7d7-ticket-carrier-test
  "Out-of-band acceptance for injury/046ae7d74d4d70b6b9d7b25bb4506e158615930d."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.mission-registry :as missions]))

(deftest cascade-selected-ticket-retains-its-completion-source-pin
  (let [target "T-car3-queue-routing-design-note"
        source {:path "/home/joe/code/futon3c/holes/tickets/T-car3-queue-routing-design-note.md"
                :sha256 "e539d69d02ab28760e704dbfa825e015ba49a813d933e1f086471c5e4808282c"}
        ticket {:id target :kind :ticket :path (:path source) :source source}]
    (with-redefs [missions/ticket-entry #(when (= target %) ticket)]
      (let [carrier (#'runner/mission-for-decision
                     {:action {:kind :cascade-candidate :target target}}
                     target)]
        (is (= target (:id carrier)))
        (is (= :ticket (:kind carrier)))
        (is (= source (:source carrier)))))))
