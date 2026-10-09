(ns futon2.aif.provisional-interpretation-guard-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]))

(def provisional
  {:decision
   {:selection-law {:applied :cascade-selection-posterior}
    :action {:kind :cascade-candidate :target "M-x"
             :precedence [{:id :p1}]
             :construction-receipt {:kind :query-time-pattern-selection}}
    :selection-certificate
    {:interpretations-owed [{:kind :interpretation-owed-after-selection
                             :target "M-x" :pattern :p1 :attested? false}]}}})

(deftest provisional-winner-is-asked-for-and-never-enacted
  (let [needed (ns-resolve 'futon2.aif.full-loop-runner
                           'interpretation-needed-refusals)
        selected (ns-resolve 'futon2.aif.full-loop-runner 'selected-entry)]
    (is (= [{:target "M-x" :kind :no-admitted-interpretation
             :missing :chosen-pattern-interpretation
             :selected-pattern :p1}]
           (needed (:decision provisional))))
    (is (nil? (selected provisional)))
    (is (map? (selected (update-in provisional
                                   [:decision :action :construction-receipt]
                                   assoc :kind :machine-constructed))))))
