(ns futon2.aif.repair-obligation-test
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [clojure.java.shell :as shell]
            [clojure.java.io :as io]
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

(def successor-sha (apply str (repeat 64 "a")))

(defn- successor-fixture []
  (let [repair-id "repair-successor-fixture"]
    {:obligation
     (shaped-obligation
      :code-commit
      {:repair/id repair-id
       :discharge-contract
       {:artifact-shape :code-commit
        :requires [:distinct-repair-commit :independent-review
                   :grounded-repair :distinct-production-shaped-successor]}
       :repair/implementation
       {:implementation-attempt "repair-attempt"
        :replacement-commit "abc1234"}})
     :repair-close
     {:attempt/id "repair-attempt" :run/id "repair-run"
      :repair/id repair-id :closed-at "2026-09-14T20:00:00Z"
      :commit "abc1234" :review-receipt-ids ["review-r.edn"]
      :review-sha256 successor-sha :grounded? true}
     :successor-close
     {:attempt/id "successor-attempt" :run/id "successor-run"
      :repair/id repair-id :closed-at "2026-09-14T21:00:00Z"
      :witness-ref "successor-witness.edn" :witness-sha256 successor-sha
      :grounded? true :production-shaped? true
      :witness {:resolved? true :dial-moved? true :implementation-id "impl-1"}}
     :authority {:decided-by "reviewer-2" :review-job "review-job-2"}}))

(defn- successor-refusal [inputs]
  (try (repair/successor-resolution! inputs) nil
       (catch clojure.lang.ExceptionInfo e
         (:repair-successor/refusal (ex-data e)))))

(deftest successor-discharge-retains-explicit-r-s-relation
  (let [root (temp-root)
        fixture (successor-fixture)
        result (repair/successor-resolution!
                (assoc fixture
                       :resolution-read-fn (constantly nil)
                       :resolve-fn (partial repair/resolve! root)))
        stored (edn/read-string
                (slurp (io/file root "resolutions"
                                "repair-successor-fixture.edn")))]
    (is (= :resolved (:status result)))
    (is (= (:relation result) (:successor-relation stored)))
    (is (= "repair-attempt"
           (get-in stored [:successor-relation :repair-attempt/id])))
    (is (= "successor-attempt"
           (get-in stored [:successor-relation :successor-attempt/id])))
    (is (= successor-sha
           (get-in stored [:successor-relation :successor-witness-sha256])))))

(deftest successor-discharge-refuses-missing-borrowed-self-and-projection
  (let [fixture (assoc (successor-fixture)
                       :resolution-read-fn (constantly nil)
                       :resolve-fn (fn [& _]
                                     (throw (AssertionError. "must not write"))))]
    (is (= :successor-missing
           (successor-refusal (assoc fixture :successor-close nil))))
    (is (= :successor-lineage-mismatch
           (successor-refusal
            (assoc-in fixture [:successor-close :repair/id] "borrowed-repair"))))
    (is (= :self-successor
           (successor-refusal
            (-> fixture
                (assoc-in [:successor-close :attempt/id] "repair-attempt")
                (assoc-in [:successor-close :run/id] "repair-run")))))
    (is (= :successor-evidence-invalid
           (successor-refusal
            (assoc fixture :successor-close
                   {:delivery-projection "not-a-successor-witness"}))))))

(deftest existing-resolution-is-stable-and-cutoff-readback-is-temporal
  (let [{:keys [obligation] :as fixture} (successor-fixture)
        existing {:repair/id (:repair/id obligation) :repair/status :resolved
                  :resolved-at "2026-09-14T21:30:00Z"}
        writes (atom 0)
        result (repair/successor-resolution!
                (assoc fixture
                       :resolution-read-fn (constantly existing)
                       :resolve-fn (fn [& _] (swap! writes inc))))
        before (repair/repair-derived-state
                (:repair/id obligation) "2026-09-14T21:00:00Z"
                obligation existing)
        after (repair/repair-derived-state
               (:repair/id obligation) "2026-09-14T22:00:00Z"
               obligation existing)]
    (is (= :already-resolved (:status result)))
    (is (zero? @writes))
    (is (= :open (:derived-status before)))
    (is (nil? (:resolution before)))
    (is (= :resolved (:derived-status after)))
    (is (= existing (:resolution after)))
    (is (= [:finding :resolution] (:derived-from after)))))

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

(deftest review-failure-finding-carries-typed-discharge-contract
  ;; repair-attempt-054: schema-1 review-failure findings carried no
  ;; discharge contract, so the repair attempt's construction ran with
  ;; :discharge nil and the discharge requirements were never
  ;; machine-visible. Every new finding mints the typed contract.
  (let [root (temp-root)
        base {:target :target/a :commit "bad123"
              :selected-entry {:action {:type :x}}
              :reviewer "codex-1" :review-job "review-1"
              :review-text "keep classification typed"}
        requested (repair/record-review-failure!
                   root (assoc base
                               :attempt-id "failed-rc"
                               :review-verdict :request-changes))
        rejected (repair/record-review-failure!
                  root (assoc base
                              :attempt-id "failed-rj"
                              :review-verdict :reject))]
    (is (= 2 (:repair/schema-version requested)))
    (is (= "repair-failed-rc-review-request-changes"
           (:repair/id requested)))
    (is (= "repair-failed-rj-review-rejected"
           (:repair/id rejected)))
    (is (= :independent-review (:failure-stage requested)))
    (is (= :review-request-changes (:failure-kind requested)))
    (is (= :review-rejected (:failure-kind rejected)))
    (is (= repair/review-failure-discharge-contract
           (:discharge-contract requested)))
    (is (= [:distinct-repair-commit :independent-review
            :grounded-repair :distinct-production-shaped-successor]
           (get-in requested [:discharge-contract :requires])))
    (is (= :code-commit
           (get-in requested [:discharge-contract :artifact-shape])))))

(deftest distinct-review-outcomes-do-not-collide-in-the-finding-store
  (let [root (temp-root)
        base {:attempt-id "attempt-002" :target :target/a :commit "bad123"
              :selected-entry {:action {:type :x}}
              :reviewer "codex-24" :review-job "review-1"
              :review-text "review outcome remains unresolved"}
        requested (repair/record-review-failure!
                   root (assoc base :review-verdict :request-changes))
        rejected (repair/record-review-failure!
                  root (assoc base :review-verdict :reject))]
    (is (not= (:repair/id requested) (:repair/id rejected)))
    (is (= #{:review-request-changes :review-rejected}
           (set (map :failure-kind (repair/open-obligations root)))))))

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
                   :review-evidence
                   {:job-id "review-2" :reviewer "claude-1"
                    :state "done" :verdict :approve :valid? true
                    :execution {:executed true :tool-events 1
                                :command-events 1}}
                   :artifact-binding
                   {:repo "/home/joe/code/futon2" :commit "good456"
                    :fresh-author? true :descendant? true
                    :in-author-window? true :corroborates? true
                    :disagreement? false}
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

(deftest system-finding-replay-is-byte-exact-or-a-typed-conflict
  (let [root (temp-root)
        finding {:attempt-id "cohort--ea1-authority--attempt-001"
                 :repair-class :environmental-hold
                 :failure-stage :agent-readiness
                 :outcome :agent-unavailable
                 :failure-kind :agent-readiness-failed
                 :error "Agency unavailable"
                 :opened-at "2026-09-11T14:45:12Z"
                 :backtrace {:source :disposable}}
        first-record (repair/record-system-failure! root finding)
        replay (repair/record-system-failure! root finding)
        conflict (try
                   (repair/record-system-failure!
                    root (assoc finding :error "different evidence"))
                   nil
                   (catch clojure.lang.ExceptionInfo e e))]
    (is (= first-record replay))
    (is (= :repair-finding-conflict (:reason (ex-data conflict))))
    (is (= [first-record] (repair/open-obligations root)))))

(deftest system-finding-replay-rejects-symlink-authority-and-is-race-safe
  (let [root (temp-root)
        outside-root (temp-root)
        finding {:attempt-id "cohort--ea1-authority--attempt-002"
                 :repair-class :environmental-hold
                 :failure-stage :agent-readiness
                 :outcome :agent-unavailable
                 :failure-kind :agent-readiness-failed
                 :error "Agency unavailable"
                 :opened-at "2026-09-11T14:45:12Z"}
        results (mapv deref
                      [(future (repair/record-system-failure! root finding))
                       (future (repair/record-system-failure! root finding))])
        record (first results)
        target (java.io.File. root
                              (str "findings/" (:repair/id record) ".edn"))
        outside (java.io.File. outside-root "outside.edn")]
    (is (= (first results) (second results)))
    (io/copy target outside)
    (io/delete-file target)
    (java.nio.file.Files/createSymbolicLink
     (.toPath target) (.toPath outside)
     (make-array java.nio.file.attribute.FileAttribute 0))
    (is (= :repair-finding-conflict
           (:reason
            (ex-data
             (try (repair/record-system-failure! root finding) nil
                  (catch clojure.lang.ExceptionInfo e e))))))
    (let [symlink-root (str (temp-root) "-link")]
      (java.nio.file.Files/createSymbolicLink
       (.toPath (java.io.File. symlink-root)) (.toPath (java.io.File. root))
       (make-array java.nio.file.attribute.FileAttribute 0))
      (is (= :repair-finding-root-refused
             (:reason
              (ex-data
               (try (repair/record-system-failure! symlink-root
                                                   (assoc finding :attempt-id "other"))
                    nil
                    (catch clojure.lang.ExceptionInfo e e)))))))
    (let [parent-root (temp-root)
          external-findings (java.io.File. (temp-root) "external-findings")]
      (.mkdir external-findings)
      (java.nio.file.Files/createSymbolicLink
       (.toPath (java.io.File. parent-root "findings"))
       (.toPath external-findings)
       (make-array java.nio.file.attribute.FileAttribute 0))
      (is (= :repair-finding-root-refused
             (:reason
              (ex-data
               (try (repair/record-system-failure! parent-root
                                                   (assoc finding :attempt-id "parent"))
                    nil
                    (catch clojure.lang.ExceptionInfo e e)))))))))

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

(deftest implementation-refusals-name-their-failed-conjuncts
  ;; repair-ea1-7093...-untyped-failure: the stop-line refusal conflated
  ;; eight conjuncts behind one message. The refusal must stay fail-closed
  ;; and identical in kind, but name what failed.
  (let [root (temp-root)
        finding (repair/record-review-failure!
                 root {:attempt-id "same-attempt" :target :target/a
                       :commit "bad123" :selected-entry {:action {:type :x}}
                       :reviewer "reviewer" :review-job "review-1"
                       :review-verdict :request-changes
                       :review-text "defective"})
        refusal (fn [impl]
                  (try (repair/record-implementation! root finding impl)
                       nil
                       (catch clojure.lang.ExceptionInfo e (ex-data e))))]
    (let [d (refusal {:attempt-id "same-attempt" :commit "good456"
                      :reviewer "reviewer" :review-job "review-2"
                      :witness {:resolved? true :dial-moved? true}})]
      (is (= :machine-repair-lacks-grounded-review-evidence (:failure-kind d)))
      (is (= :stop-line-resolution (:failure-stage d)))
      (is (some #{:implementation-attempt-not-distinct} (:failure-detail d)))
      (is (not-any? #{:witness-not-resolved} (:failure-detail d))))
    (let [d (refusal {:attempt-id "repair-2" :commit "good456"
                      :reviewer "reviewer" :review-job "review-2"
                      :witness {:resolved? true :dial-moved? false}})]
      (is (some #{:witness-dial-not-moved} (:failure-detail d)))
      (is (not-any? #{:implementation-attempt-not-distinct} (:failure-detail d))))))

(deftest schema-three-implementation-requires-grounded-independent-review
  (let [root (temp-root)
        obligation {:repair/id "repair-grounded-review"
                    :repair/schema-version 3
                    :repair/status :open
                    :repair/class :machine-failure
                    :attempt-id "ea1-old--attempt-001"
                    :machine-repo "/srv/futon2"
                    :discharge-contract {:artifact-shape :code-commit}}
        base {:attempt-id "ea1-new--attempt-001"
              :commit "abc1234"
              :reviewer "codex-24"
              :review-job "review-42"
              :witness {:resolved? true :dial-moved? true}}
        review-evidence {:job-id "review-42" :reviewer "codex-24"
                         :state "done" :verdict :approve :valid? true
                         :execution {:executed true :tool-events 2
                                     :command-events 1}}
        binding {:repo "/srv/futon2" :commit "abc1234"
                 :fresh-author? true :descendant? true
                 :in-author-window? true :corroborates? true
                 :disagreement? false}
        refusal (fn [implementation]
                  (try (repair/record-implementation! root obligation implementation)
                       nil
                       (catch clojure.lang.ExceptionInfo e (ex-data e))))]
    (testing "self-asserted legacy labels do not discharge a schema-3 finding"
      (is (some #{:grounded-review-evidence-invalid}
                (:failure-detail (refusal base)))))
    (testing "borrowed review and artifact bindings refuse at the same gate"
      (is (some #{:grounded-review-evidence-invalid}
                (:failure-detail
                 (refusal (assoc base
                                 :review-evidence
                                 (assoc review-evidence :job-id "review-other")
                                 :artifact-binding binding)))))
      (is (some #{:grounded-review-evidence-invalid}
                (:failure-detail
                 (refusal (assoc base
                                 :review-evidence review-evidence
                                 :artifact-binding
                                 (assoc binding :commit "def5678")))))))
    (testing "the exact independently reviewed implementation is retained"
      (let [record (repair/record-implementation!
                    root obligation
                    (assoc base :review-evidence review-evidence
                           :artifact-binding binding))]
        (is (= "abc1234" (:replacement-commit record)))
        (is (= review-evidence (:grounded-review-evidence record)))
        (is (= binding (:artifact-binding record)))
        (is (= record
               (edn/read-string
                (slurp (io/file root "implementations"
                                "repair-grounded-review.edn")))))))))

(deftest system-finding-replay-accepts-a-finding-written-before-the-pr-str-switch
  ;; These files were pprinted until 2026-09-19. Measured on a real 19.4 MB
  ;; finding: pprint 47,306 ms, pr-str 212 ms — and those 47 seconds were spent
  ;; holding the contended store lock. Replay here is decided by exact bytes,
  ;; deliberately, so the format change would have turned every replay of an
  ;; already-stored finding into a :repair-finding-conflict. This pins that it
  ;; does not.
  (let [root (temp-root)
        finding {:attempt-id "cohort--ea1-legacy-bytes--attempt-001"
                 :repair-class :environmental-hold
                 :failure-stage :agent-readiness
                 :outcome :agent-unavailable
                 :failure-kind :agent-readiness-failed
                 :error "Agency unavailable"
                 :opened-at "2026-09-11T14:45:12Z"
                 :backtrace {:source :disposable}}
        ;; Publish once, then rewrite the file in the OLD pprint form to stand
        ;; in for everything already on disk.
        record (repair/record-system-failure! root finding)
        path (str root "/findings/" (:repair/id record) ".edn")
        stored (edn/read-string (slurp path))
        _ (spit path (with-out-str (pp/pprint stored)))
        legacy-bytes (count (slurp path))
        replay (repair/record-system-failure! root finding)]

    (testing "the file on disk really is in the legacy pretty-printed form"
      (is (re-find #"\n " (slurp path)))
      (is (> legacy-bytes (count (pr-str stored)))))

    (testing "replaying it is acknowledged, not raised as a byte conflict"
      (is (= record replay)))

    (testing "a genuinely different finding is still a typed conflict"
      (is (= :repair-finding-conflict
             (:reason (ex-data (try (repair/record-system-failure!
                                     root (assoc finding :error "different evidence"))
                                    nil
                                    (catch clojure.lang.ExceptionInfo e e)))))))))
