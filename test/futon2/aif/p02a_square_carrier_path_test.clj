(ns futon2.aif.p02a-square-carrier-path-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.occurrence-square-authoring :as square-authoring]
            [futon2.aif.selected-outer-cascade-policy :as selected]
            [futon2.aif.wm.cascade-decision :as cascade]))

(def missing-square-family
  {:target-id "M-missing-square"
   :failures [{:kind :occurrence-squares-not-authored
               :excerpt-digest "excerpt-pin"
               :analysis-digest "analysis-pin"}]})

(deftest selected-outer-projection-does-not-carry-the-prechoice-family
  ;; This is deliberately not an outer-selector integration fixture.  The
  ;; direct selected-need control proves only the consumer seam; the projected
  ;; decision is the actual downstream shape passed by cascade-decision.
  (let [outer-receipt {:policy-field {:families [missing-square-family]}}
        carrier {:selected-entry {:action {:target "M-selected"}}}
        projected (with-redefs [selected/verify (constantly nil)]
                    (cascade/cascade-decision
                     {}
                     {:selected-outer-cascade-policy carrier
                      :outer-policy-selection-claimed? true}))
        projected-judgement {:decision (:decision projected)}
        direct-consumer-fixture
        {:decision {:selection-certificate
                    {:target-policy-field (:policy-field outer-receipt)}}}]
    (is (= :selected (get-in projected [:decision :status])))
    (is (nil? (get-in projected-judgement
                      [:decision :selection-certificate :target-policy-field])))
    (is (nil? (square-authoring/selected-need projected-judgement)))
    (is (= {:target "M-missing-square"
            :excerpt-digest "excerpt-pin"
            :analysis-digest "analysis-pin"}
           (square-authoring/selected-need direct-consumer-fixture)))))
