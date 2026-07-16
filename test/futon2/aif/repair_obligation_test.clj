(ns futon2.aif.repair-obligation-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.repair-obligation :as repair]))

(defn- temp-root []
  (let [f (java.io.File/createTempFile "wm-repair-" "")]
    (.delete f)
    (.mkdirs f)
    (.getPath f)))

(deftest finding-remains-open-until-grounded-successor-resolution
  (let [root (temp-root)
        finding (repair/record-review-failure!
                 root {:attempt-id "failed-1"
                       :target :sorry/g2
                       :commit "bad123"
                       :selected-entry {:action {:type :address-sorry
                                                 :target :sorry/g2}}
                       :reviewer "codex-7"
                       :review-job "review-1"
                       :review-verdict :request-changes
                       :review-text "provenance gate is optional"})]
    (is (= [finding] (repair/open-obligations root)))
    (testing "tests or prose without a grounded witness cannot clear the line"
      (is (thrown? clojure.lang.ExceptionInfo
                   (repair/resolve! root finding
                                    {:attempt-id "repair-1" :commit "good456"
                                     :reviewer "codex-7" :review-job "review-2"
                                     :witness {:resolved? true :dial-moved? false}}))))
    (repair/record-implementation!
     root finding {:attempt-id "repair-1" :commit "good456"
                   :reviewer "codex-7" :review-job "review-2"
                   :witness {:resolved? true :dial-moved? true}})
    (let [awaiting (first (repair/open-obligations root))]
      (is (= :awaiting-validation (:repair/status awaiting)))
      (repair/resolve! root awaiting
                       {:attempt-id "successor-1" :commit "next789"
                        :reviewer "codex-7" :review-job "review-3"
                        :witness {:resolved? true :dial-moved? true}
                        :validation {:production-shaped? true}}))
    (is (empty? (repair/open-obligations root)))))

(deftest system-actuation-failure-is-distinct-durable-stop-line-memory
  (let [root (temp-root)
        finding (repair/record-system-failure!
                 root {:attempt-id "attempt-002"
                       :repair-class :machine-failure
                       :machine-repo "/home/joe/code/futon2"
                       :target :fire-pattern
                       :selected-entry
                       {:action {:type :learn-action-class
                                 :target-class :fire-pattern}}
                       :failure-stage :construction
                       :outcome :construction-failed
                       :error "No construction for selected decision"})]
    (is (= :machine-failure (:repair/class finding)))
    (is (= "/home/joe/code/futon2" (:machine-repo finding)))
    (is (= [finding] (repair/open-obligations root)))
    (repair/record-implementation!
     root finding {:attempt-id "canary-repair" :commit "good456"
                   :reviewer "claude-1" :review-job "review-2"
                   :witness {:resolved? true :dial-moved? true}})
    (repair/resolve! root (first (repair/open-obligations root))
                     {:attempt-id "canary-successor" :commit "next789"
                      :reviewer "claude-1" :review-job "review-3"
                      :witness {:resolved? true :dial-moved? true}
                      :validation {:production-shaped? true}})
    (is (empty? (repair/open-obligations root)))))

(deftest one-attempt-can-open-independent-typed-findings
  (let [root (temp-root)
        common {:attempt-id "attempt-006"
                :failure-stage :author-wait
                :outcome :incomplete
                :error "work was incorrectly declared stalled"}
        machine (repair/record-system-failure!
                 root (assoc common
                             :repair-class :machine-failure
                             :failure-kind :false-timeout))
        artifact (repair/record-system-failure!
                  root (assoc common
                              :repair-class :incomplete-recoverable
                              :failure-kind :late-author-artifact))]
    (is (= #{"repair-attempt-006-false-timeout"
             "repair-attempt-006-late-author-artifact"}
           (set (map :repair/id (repair/open-obligations root)))))
    (is (not= (:repair/id machine) (:repair/id artifact)))))

(deftest failed-commit-cannot-be-its-own-repair-implementation
  (let [root (temp-root)
        finding (repair/record-review-failure!
                 root {:attempt-id "failed-same" :target :target/a
                       :commit "bad123" :selected-entry {:action {:type :x}}
                       :reviewer "reviewer" :review-job "review-1"
                       :review-verdict :request-changes
                       :review-text "still defective"})]
    (is (thrown? clojure.lang.ExceptionInfo
                 (repair/record-implementation!
                  root finding {:attempt-id "repair-attempt"
                                :commit "bad123"
                                :reviewer "reviewer" :review-job "review-2"
                                :witness {:resolved? true :dial-moved? true}})))))

(deftest impossible-recovery-is-immutably-superseded-by-typed-successor
  (let [root (temp-root)
        old (repair/record-system-failure!
             root {:attempt-id "old" :repair-class :incomplete-recoverable
                   :failure-stage :author-wait :outcome :incomplete
                   :failure-kind :agent-budget-expired :error "budget"})
        successor (repair/record-system-failure!
                   root {:attempt-id "new" :repair-class :machine-failure
                         :failure-stage :author-wait :outcome :incomplete
                         :failure-kind :recovery-job-terminal
                         :error "job failed"})]
    (repair/supersede! root old successor :recovery-job-terminal)
    (is (= [(:repair/id successor)]
           (mapv :repair/id (repair/open-obligations root))))
    (is (thrown? java.nio.file.FileAlreadyExistsException
                 (repair/supersede! root old successor
                                    :recovery-job-terminal)))))
