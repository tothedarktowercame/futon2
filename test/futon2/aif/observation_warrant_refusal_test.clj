(ns futon2.aif.observation-warrant-refusal-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.observation-checks :as oc]))

(defn- observe-c2 [locator]
  (oc/observe {:standing-cascade-g-test-passes
               (merge {:class :C2 :repo "futon2" :entry-id "retained-entry"
                       :ns "futon2.vm.standing-enacted-policy-is-cascade-g-test"}
                      locator)}))

(deftest retired-warrant-class-is-a-typed-mechanical-absence
  ;; Warrant consultation was removed from the observation vector in
  ;; 157b5ab80. A retained C2 locator is historical input, not permission to
  ;; revive the registry client inside `observe`.
  (let [observation (observe-c2 {})
        refusal (get-in observation [:refused :standing-cascade-g-test-passes])]
    (is (= {:status :missing :kind :no-mechanical-check :data {:class :C2}}
           refusal))
    (is (empty? (:results observation)))
    (is (empty? (:observed observation)))))

(deftest retained-registry-payload-cannot-revive-warrant-consultation
  (testing "legacy scope-drift detail is not reinterpreted as live evidence"
    (let [observation
          (observe-c2 {:warrant? false
                       :reason "stale-sha"
                       :details {"scope-drift" [{"path" "src/futon2/aif/cascade_sources.clj"
                                                  "classification" "committed"}]}})
          refusal (get-in observation [:refused :standing-cascade-g-test-passes])]
      (is (= :no-mechanical-check (:kind refusal)))
      (is (= {:class :C2} (:data refusal)))
      (is (empty? (:results observation)))
      (is (empty? (:observed observation))))))
