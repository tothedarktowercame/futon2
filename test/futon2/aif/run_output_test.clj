(ns futon2.aif.run-output-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.wm.run-output :as output]))

(def grounded
  {:outcome :grounded-progress
   :morning-brief-ref {:id "brief-1"}
   :data {:feature-card
          {:built "Added a current-state mission reader."
           :want-coverage "Completed wants are no longer selected."
           :matches-intent? true
           :things-to-try
           ["clojure -X:test :nses '[futon2.aif.mission-hole-wants-test]' -> tests pass"]
           :reviewer-note "Replayed independently."
           :proof-ref "proof/current-wants.lean"}}})

(deftest grounded-output-retains-summary-and-addressable-demo
  (let [commit {:repo "/home/joe/code/futon2" :sha "abc1234"}
        r (output/receipt grounded commit)]
    (is (= :present (:status r)))
    (is (= commit (get-in r [:demo :pointer])))
    (is (= (get-in grounded [:data :feature-card :things-to-try])
           (get-in r [:demo :replay])))
    (is (= "Added a current-state mission reader."
           (get-in r [:summary :built])))
    (is (= [{:kind :proof :path "proof/current-wants.lean"}
            {:kind :morning-brief :ref {:id "brief-1"}}]
           (:supporting-artifacts r)))))

(deftest absent-or-malformed-output-is-typed
  (testing "a failed run does not claim a demo"
    (is (= :run-not-grounded
           (:reason (output/receipt {:outcome :build-failed} nil)))))
  (testing "grounded work cannot silently acquire invented output prose"
    (is (= :validated-output-evidence-not-retained
           (:reason (output/receipt {:outcome :grounded-progress :data {}}
                                    {:repo "r" :sha "abc1234"}))))))
