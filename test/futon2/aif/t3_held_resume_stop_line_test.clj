(ns futon2.aif.t3-held-resume-stop-line-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.tripwire :as tripwire]))

(use-fixtures :once hermetic/with-hermetic-stores)

(deftest held-resume-stop-line-clears-t3-before-opportunity-end
  (let [opts (hermetic/runner-repair-options)
        root (:repair-root opts)
        attempt-id "ea1-held-resume--attempt-002"
        observation {:phase :opportunity
                     :transition :end
                     :outcome :incomplete
                     :external-attempt-id attempt-id
                     :repair-root root
                     :tripwire/force? true}
        witnesses-at-end (atom nil)
        finding {:attempt-id attempt-id
                 :observation {:phase :held-open-cascade-resume}
                 :repair-class :incomplete-recoverable
                 :machine-repo "/home/joe/code/futon2"
                 :target "M-held"
                 :failure-stage :cascade-resume
                 :outcome :incomplete
                 :failure-kind :open-cascade-review-answer-invalid
                 :error "Selected cascade held in open-cascade phase"
                 :failure-data {:held-phase {:open-cascade true
                                             :repairable true}}
                 :discharge-contract
                 {:requires [:complete-existing-author-artifact
                             :independent-review
                             :grounded-existing-commit]
                  :artifact-shape :code-commit}}]
    (testing "the historical producer ordering is red against the real T3"
      (is (= [:missing-durable-stop-line]
             (mapv :kind (tripwire/evaluate-wire :T3 observation))))
      (is (empty? (filter #(and (.isFile %)
                                (.endsWith (.getName %) ".edn"))
                          (file-seq (io/file root "findings"))))))
    (#'runner/record-held-resume-stop-line!
     opts finding
     #(reset! witnesses-at-end (tripwire/evaluate-wire :T3 observation)))
    (let [files (vec (filter #(and (.isFile %)
                                   (.endsWith (.getName %) ".edn"))
                             (file-seq (io/file root "findings"))))
          retained (edn/read-string (slurp (first files)))]
      (testing "the runner uses the ordinary repair producer for one open finding"
        (is (= 1 (count files)))
        (is (= :open (:repair/status retained)))
        (is (= :incomplete-recoverable (:repair/class retained)))
        (is (= attempt-id (:attempt-id retained))))
      (testing "T3 rereads that finding before the end event"
        (is (empty? @witnesses-at-end))
        (is (empty? (tripwire/evaluate-wire :T3 observation)))))))
