(ns futon2.aif.repair-obligation-test
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.repair-obligation :as repair]))

(defn- temp-root []
  (let [f (java.io.File/createTempFile "wm-repair-" "")]
    (.delete f)
    (.mkdirs f)
    (.getPath f)))

(def grounded-review
  {:reviewer "reviewer" :review-job "review-job"
   :witness {:resolved? true :dial-moved? true}})

(defn- shaped-obligation [shape & [extra]]
  (merge {:repair/id (str "repair-" (name shape))
          :repair/status :open
          :repair/class :machine-failure
          :attempt-id "failed-attempt"
          :discharge-contract {:artifact-shape shape}}
         extra))

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

(deftest artifact-shapes-validate-only-their-own-evidence
  (testing "an absent shape remains the historical code-commit contract"
    (let [root (temp-root)
          obligation (dissoc (shaped-obligation :code-commit
                                                {:failed-commit "bad123"})
                             :discharge-contract)
          record (repair/record-implementation!
                  root obligation
                  (merge grounded-review
                         {:attempt-id "repair-code" :commit "good456"}))]
      (is (= "good456" (:replacement-commit record)))
      (is (nil? (:artifact-shape record)))))

  (testing "code contracts reject deposit evidence"
    (is (thrown? clojure.lang.ExceptionInfo
                 (repair/record-implementation!
                  (temp-root) (shaped-obligation :code-commit)
                  (merge grounded-review
                         {:attempt-id "wrong-code"
                          :store-url "http://store" :record-type :records
                          :count-before 1 :count-after 2
                          :deposit-run-id "deposit-1"})))))

  (testing "data contracts reject a bare commit"
    (is (thrown? clojure.lang.ExceptionInfo
                 (repair/record-implementation!
                  (temp-root) (shaped-obligation :data-deposit)
                  (merge grounded-review
                         {:attempt-id "wrong-data" :commit "good456"})))))

  (testing "spec contracts reject a bare commit"
    (is (thrown? clojure.lang.ExceptionInfo
                 (repair/record-implementation!
                  (temp-root) (shaped-obligation :spec-document)
                  (merge grounded-review
                         {:attempt-id "wrong-spec" :commit "good456"}))))))

(deftest data-deposit-validation-reads-current-count-without-writing
  (let [root (temp-root)
        reads (atom [])
        obligation (shaped-obligation :data-deposit)
        evidence {:store-url "http://read-only-store"
                  :record-type :wm-hyperparameter-update
                  :count-before 4 :count-after 7
                  :deposit-run-id "deposit-run-7"}]
    (binding [repair/*store-count-reader*
              (fn [url record-type]
                (swap! reads conj [url record-type])
                7)]
      (let [implementation
            (repair/record-implementation!
             root obligation
             (merge grounded-review evidence {:attempt-id "repair-data"}))]
        (repair/resolve!
         root (assoc obligation
                     :repair/status :awaiting-validation
                     :repair/implementation implementation)
         (merge grounded-review evidence
                {:attempt-id "validate-data"
                 :validation {:production-shaped? true}}))))
    (is (= [["http://read-only-store" :wm-hyperparameter-update]
            ["http://read-only-store" :wm-hyperparameter-update]]
           @reads))
    (testing "the declared after-count must match the independent store read"
      (binding [repair/*store-count-reader* (fn [_ _] 6)]
        (is (thrown? clojure.lang.ExceptionInfo
                     (repair/record-implementation!
                      (temp-root) obligation
                      (merge grounded-review evidence
                             {:attempt-id "stale-data-evidence"}))))))))

(deftest spec-document-requires-ancestor-commit-that-touched-existing-path
  (let [root (temp-root)
        repo (temp-root)
        path "repair-spec.md"
        file (java.io.File. repo path)]
    (is (zero? (:exit (shell/sh "git" "-C" repo "init" "-q"))))
    (is (zero? (:exit (shell/sh "git" "-C" repo "config"
                                "user.email" "repair-test@example.invalid"))))
    (is (zero? (:exit (shell/sh "git" "-C" repo "config"
                                "user.name" "Repair Test"))))
    (spit file "declared repair contract\n")
    (is (zero? (:exit (shell/sh "git" "-C" repo "add" path))))
    (is (zero? (:exit (shell/sh "git" "-C" repo "commit" "-q"
                                "-m" "Add repair spec"))))
    (let [sha (str/trim
               (:out (shell/sh "git" "-C" repo "rev-parse" "HEAD")))
          obligation (shaped-obligation :spec-document {:machine-repo repo})
          evidence {:path path :git-sha sha}
          implementation (repair/record-implementation!
                          root obligation
                          (merge grounded-review evidence
                                 {:attempt-id "repair-spec"}))]
      (is (= evidence (:replacement-artifact implementation)))
      (is (thrown? clojure.lang.ExceptionInfo
                   (repair/record-implementation!
                    (temp-root) obligation
                    (merge grounded-review {:attempt-id "bad-spec"
                                            :path path :git-sha "deadbeef"}))))
      (let [resolved (repair/resolve!
                      root (assoc obligation
                                  :repair/status :awaiting-validation
                                  :repair/implementation implementation)
                      (merge grounded-review evidence
                             {:attempt-id "validate-spec"
                              :validation {:production-shaped? true}}))]
        (is (= :spec-document (:artifact-shape resolved)))
        (is (= evidence (:validation-artifact resolved)))))))

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
