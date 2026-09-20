(ns futon2.aif.observation-warrant-refusal-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.observation-checks :as oc]))

(defn- observe-refusal [response]
  (with-redefs-fn
    {(ns-resolve 'futon2.aif.observation-checks 'registry-check)
     (fn [_ _] response)}
    #(oc/observe
      {:standing-cascade-g-test-passes
       {:class :C2 :repo "futon2" :entry-id "retained-entry"
        :ns "futon2.vm.standing-enacted-policy-is-cascade-g-test"}})))

(deftest stale-c2-retains-registry-scope-drift
  ;; The actual stale-entry shape: these two producer files moved after mint.
  (let [drift [{"path" "src/futon2/aif/cascade_sources.clj"
                "classification" "committed"}
               {"path" "scripts/futon2/report/war_machine.clj"
                "classification" "committed"}]
        observation (observe-refusal {"warrant?" false "reason" "stale-sha"
                                     "details" {"scope-drift" drift}})
        refusal (get-in observation [:refused :standing-cascade-g-test-passes])]
    (is (= :no-current-warrant (:kind refusal)))
    (is (= "stale-sha" (get-in refusal [:data :registry :reason])))
    (is (= drift (get-in refusal [:data :registry :details :scope-drift])))
    (is (empty? (:results observation)))
    (is (empty? (:observed observation)))))

(deftest missing-scope-drift-is-not-invented
  (let [observation (observe-refusal {"warrant?" false "reason" "not-a-warrant"})
        refusal (get-in observation [:refused :standing-cascade-g-test-passes])]
    (is (= :no-current-warrant (:kind refusal)))
    (is (not (contains? (get-in refusal [:data :registry]) :details)))
    (is (empty? (:results observation)))))
