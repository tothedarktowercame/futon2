(ns futon2.aif.selection-judge-outer-selection-test
  "The production selection judge forwards the retained outer selection to the
  War Machine judgement, so the reading-policy class model can bind the
  selected target's class to the outer relation (held run
  2026-11-06-05f4bdcd abstained with :target-class-required without it)."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.report.war-machine :as wm]))

(deftest selection-judge-forwards-outer-selection
  (let [seen (atom nil)
        outer {:selection {:selected {:target "M-formal-patterns"}}
               :selected-relation :useful-elsewhere}]
    (with-redefs [wm/generate-war-machine (fn [_days opts]
                                            (reset! seen opts)
                                            {:judgement {:decision {:status :abstained}}})]
      (#'runtime/selection-judge
       {:futon2.aif.full-loop-runtime/preference-carrier (delay {:carrier :stub})
        :task {:id "M-formal-patterns"}
        :outer/selection outer}
       7))
    (is (= outer (:outer/selection @seen)))))
