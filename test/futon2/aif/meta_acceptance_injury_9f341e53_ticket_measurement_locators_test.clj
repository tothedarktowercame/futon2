(ns futon2.aif.meta-acceptance-injury-9f341e53-ticket-measurement-locators-test
  "Out-of-band acceptance for injury/9f341e53cf9080500631f69d0c1630440caf4f26."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner]))

(deftest source-backed-ticket-with-unlocated-wants-stays-off-measurement-path
  (let [target "T-car3-queue-routing-design-note"
        carrier {:id target
                 :kind :ticket
                 :source {:path "/repo/holes/tickets/T-car3-queue-routing-design-note.md"
                          :sha256 "e539d69d02ab28760e704dbfa825e015ba49a813d933e1f086471c5e4808282c"}}
        action {:kind :cascade-candidate
                :target target
                :want [[target [:pattern-done ["agent/scope-before-action" 0]]]]
                :observation-locators {}}]
    (is (false?
         (boolean
          ((ns-resolve 'futon2.aif.full-loop-runner
                       'mission-measurement-applicable?)
           carrier action nil nil))))))
