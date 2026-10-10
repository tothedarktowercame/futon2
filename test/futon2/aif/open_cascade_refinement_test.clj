(ns futon2.aif.open-cascade-refinement-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.morning-brief :as brief]
            [futon2.aif.open-cascade-refinement :as sut]))

(def revised-action
  {:precedence [{:id :p/new}]
   :construction-receipt {:kind :machine-constructed}})

(defn fixture-revision [dispatched-action artifact-commit]
  {:round 2 :commits ["aaa1111" "bbb2222"]
   :artifact-binding {:commit artifact-commit}
   :selection-dispatch
   (let [selected-step {:id :p/new}
         dispatched-step (first (:precedence dispatched-action))]
     {:schema :wm/revision-selection-dispatch-v1
      :verdict (if (= revised-action dispatched-action) :match :typed-divergence)
      :selected-action-sha256 (identity/digest revised-action)
      :dispatched-action-sha256 (identity/digest dispatched-action)
      :selected-step-sha256 (identity/digest selected-step)
      :dispatched-step-sha256 (identity/digest dispatched-step)
      :selected-step selected-step :dispatched-step dispatched-step})
   :cascade-revision
   {:status :revised
    :blocker {:stage :reviewer-verdict :kind :review-request-changes}
    :history [{:identity "prior-id" :patterns [:p/old]}
              {:identity "revised-id" :patterns [:p/old :p/new]}]
    :delta {:added [:p/new]}
    :proposal-production
    {:pattern :p/new
     :admission {:status :admitted :authority :want-interpretation-validate-response
                 :construction-receipt-sha256 "receipt-digest"}
     :construction :machine-constructed
     :library-search {:status :recorded :query-sha256 "query"
                      :considered [{:pattern :p/old :disposition :rejected
                                    :reason :does-not-resolve-review}
                                   {:pattern :p/new :disposition :selected
                                    :reason :admitted-reading}]}}
    :revised revised-action}})

(def matching-pattern-use
  {:schema sut/pattern-use-schema :status :verified :pattern :p/new
   :action-sha256 (identity/digest revised-action)
   :source {:authority :reviewed-pattern-application}})

(deftest matching-observed-use-is-durable-and-briefable
  (let [cert (sut/certificate {:revision (fixture-revision revised-action "bbb2222")
                               :artifact-commit "bbb2222"
                               :pattern-use matching-pattern-use
                               :outcome {:status :recorded :value :grounded-change}})
        summary (brief/item-summary "/fixture/item.edn"
                                    {:attempt-id "attempt-r" :outcome :grounded-change
                                     :open-cascade-refinement cert})]
    (is (= :verified-used (:status cert)))
    (is (= :retrieved-existing (:branch cert)))
    (is (= :absent (get-in cert [:branch-capabilities :authored-new :status])))
    (is (= {:stage :reviewer-verdict :kind :review-request-changes}
           (get-in summary [:open-cascade-refinement :blocker])))
    (is (= :p/new (get-in summary [:open-cascade-refinement :pattern])))
    (is (= "prior-id" (get-in summary [:open-cascade-refinement :prior-identity])))
    (is (= "revised-id" (get-in summary [:open-cascade-refinement :revised-identity])))
    (is (= :admitted (get-in summary [:open-cascade-refinement :admission-verdict])))
    (is (= :p/new (get-in summary [:open-cascade-refinement :enabled-step])))
    (is (= {:id :p/new} (get-in summary [:open-cascade-refinement :dispatched-step])))
    (is (= "bbb2222" (get-in summary [:open-cascade-refinement :artifact-commit])))
    (is (= :verified (get-in summary [:open-cascade-refinement :pattern-use-status])))
    (is (= [] (get-in summary [:open-cascade-refinement :typed-gaps])))))

(deftest same-commit-with-different-dispatched-action-refuses
  (let [different (assoc revised-action :precedence [{:id :p/different}])
        cert (sut/certificate {:revision (fixture-revision different "bbb2222")
                               :artifact-commit "bbb2222"
                               :outcome {:status :recorded :value :grounded-change}})]
    (is (= :refused (:status cert)))
    (is (= :selected-to-dispatched (get-in cert [:typed-gaps 0 :field])))))

(deftest matching-dispatch-and-commit-without-observed-use-is-not-verified-use
  (let [cert (sut/certificate {:revision (fixture-revision revised-action "bbb2222")
                               :artifact-commit "bbb2222"
                               :outcome {:status :recorded :value :grounded-change}})]
    (is (= :dispatched (:status cert)))
    (is (= :pattern-use (get-in cert [:typed-gaps 0 :field])))
    (is (= :observation-unavailable (get-in cert [:typed-gaps 0 :reason])))))

(deftest observed-use-for-a-different-pattern-refuses
  (let [cert (sut/certificate {:revision (fixture-revision revised-action "bbb2222")
                               :artifact-commit "bbb2222"
                               :pattern-use (assoc matching-pattern-use :pattern :p/other)
                               :outcome {:status :recorded :value :grounded-change}})]
    (is (= :refused (:status cert)))
    (is (= :observation-mismatch (get-in cert [:typed-gaps 0 :reason])))))

(deftest matching-action-with-wrong-artifact-commit-refuses-separately
  (let [cert (sut/certificate {:revision (fixture-revision revised-action "bbb2222")
                               :artifact-commit "ccc3333"
                               :outcome {:status :recorded :value :grounded-change}})]
    (is (= :refused (:status cert)))
    (is (= :artifact-binding (get-in cert [:typed-gaps 0 :field])))
    (is (= :commit-mismatch (get-in cert [:typed-gaps 0 :reason])))))

(deftest revised-run-data-carries-the-certificate-to-close
  (let [data (sut/attach-to-run-data {:revision (fixture-revision revised-action "bbb2222")
                                      :commit "bbb2222"}
                                     :grounded-change)]
    (is (= sut/schema (get-in data [:open-cascade-refinement :schema])))
    (is (= :dispatched (get-in data [:open-cascade-refinement :status])))
    (is (= "revised-id"
           (get-in data [:open-cascade-refinement :revised :identity])))))
