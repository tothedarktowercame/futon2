(ns futon2.aif.open-cascade-refinement-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.morning-brief :as brief]
            [futon2.aif.open-cascade-refinement :as sut]))

(def fixture-revision
  {:round 2 :commits ["aaa1111" "bbb2222"]
   :cascade-revision
   {:status :revised
    :blocker {:stage :reviewer-verdict :kind :review-request-changes}
    :history [{:identity "prior-id" :patterns [:p/old]}
              {:identity "revised-id" :patterns [:p/old :p/new]}]
    :delta {:added [:p/new]}
    :proposal-production
    {:pattern :p/new :admission :want-interpretation-validate-response
     :construction :machine-constructed
     :library-search {:status :recorded :query-sha256 "query"
                      :considered [{:pattern :p/old :disposition :rejected
                                    :reason :does-not-resolve-review}
                                   {:pattern :p/new :disposition :selected
                                    :reason :admitted-reading}]}}
    :revised {:precedence [{:id :p/new}]
              :construction-receipt {:kind :machine-constructed}}}})

(deftest complete-retrieval-refinement-is-durable-and-briefable
  (let [cert (sut/certificate {:revision fixture-revision
                               :enacted-commit "bbb2222"
                               :outcome {:status :recorded :value :grounded-change}})
        summary (brief/item-summary "/fixture/item.edn"
                                    {:attempt-id "attempt-r" :outcome :grounded-change
                                     :open-cascade-refinement cert})]
    (is (= :complete (:status cert)))
    (is (= :retrieved-existing (:branch cert)))
    (is (= :absent (get-in cert [:branch-capabilities :authored-new :status])))
    (is (= {:stage :reviewer-verdict :kind :review-request-changes}
           (get-in summary [:open-cascade-refinement :blocker])))
    (is (= :p/new (get-in summary [:open-cascade-refinement :pattern])))
    (is (= "prior-id" (get-in summary [:open-cascade-refinement :prior-identity])))
    (is (= "revised-id" (get-in summary [:open-cascade-refinement :revised-identity])))
    (is (= :admitted (get-in summary [:open-cascade-refinement :admission-verdict])))
    (is (= :p/new (get-in summary [:open-cascade-refinement :enabled-step])))
    (is (= "bbb2222" (get-in summary [:open-cascade-refinement :enacted-step])))
    (is (= [] (get-in summary [:open-cascade-refinement :typed-gaps])))))

(deftest revised-cascade-must-link-to-enactment
  (let [cert (sut/certificate {:revision fixture-revision
                               :enacted-commit "ccc3333"
                               :outcome {:status :recorded :value :grounded-change}})]
    (is (= :refused (:status cert)))
    (is (false? (get-in cert [:enactment :linked?])))
    (is (= :selected-to-enacted (get-in cert [:typed-gaps 0 :field])))))

(deftest revised-run-data-carries-the-certificate-to-close
  (let [data (sut/attach-to-run-data {:revision fixture-revision :commit "bbb2222"}
                                     :grounded-change)]
    (is (= sut/schema (get-in data [:open-cascade-refinement :schema])))
    (is (= :complete (get-in data [:open-cascade-refinement :status])))
    (is (= "revised-id"
           (get-in data [:open-cascade-refinement :revised :identity])))))
