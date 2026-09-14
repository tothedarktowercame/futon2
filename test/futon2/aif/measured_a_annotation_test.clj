(ns futon2.aif.measured-a-annotation-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.measured-a-annotation :as sut]))

(defn- annotation []
  (let [bytes (pr-str {:schema :wm/close-evidence-v1
                       :entity-id "M-row-14" :disposition :build-failed})]
    {:schema :wm/measured-a-annotation-v1
     :annotation/id "fixture-annotation-1"
     :subject {:entity/id "M-row-14" :run/id "fixture-run-1"
               :cohort/id "wm-fixture-cohort" :attempt/id "fixture-attempt-1"
               :checkpoint :closed :state :addressed
               :state-at "2026-09-13T12:00:01Z" :model/revision "fixture-model-v1"}
     :label :addressed
     :rubric {:id :wm/measured-a-status-rubric :version 1
              :criterion/id :test/independent-addressed-evidence}
     :conditioning {:point :post-action-at-close :transition/id "transition-1"
                    :action/id "action-1" :action-at "2026-09-13T12:00:00Z"
                    :close/disposition :build-failed
                    :closed-at "2026-09-13T12:00:02Z"
                    :evidence-cutoff "2026-09-13T12:00:03Z"}
     :evidence [{:role :retained-close-record :bytes bytes
                 :sha256 (sut/sha256 bytes) :observed-at "2026-09-13T12:00:02Z"
                 :supports [:addressed] :conflicts []}]
     :provenance {:mode :contemporaneous
                  :label/derived-from :independent-adjudication
                  :retrospective {:status :not-applicable}
                  :source-shape {:scope :isolated-test
                                 :record-type :wm/tick-run-record
                                 :based-on "retained WM close-record shape"}}
     :created-at "2026-09-13T12:00:04Z"}))

(defn- authority [a]
  (let [origin-bytes (pr-str {:kind :fixture-observer-origin :id "observer-1"})
        auth-bytes (pr-str {:kind :fixture-reviewer-authorization :id "reviewer-1"})
        subject (sut/acceptance-subject a)
        core {:status :accepted :subject subject
              :subject-sha256 (sut/sha256 (pr-str subject))
              :observer/id "observer-1" :reviewer/id "reviewer-1"
              :reviewed-at "2026-09-13T12:00:05Z"}
        artifact-bytes (pr-str core)]
    {:schema :wm/measured-a-annotation-authority-v1 :scope :isolated-test
     :rubric {:id :wm/measured-a-status-rubric :version 1
              :status-support (vec sut/status-support)
              :criteria (zipmap sut/status-support
                                (map #(keyword "test" (str "criterion-" (name %)))
                                     sut/status-support))}
     :observer {:id "observer-1"
                :origin {:schema :wm/measured-a-authority-source-v1
                         :kind :test-fixture :id "observer-origin-1"
                         :bytes origin-bytes :sha256 (sut/sha256 origin-bytes)}}
     :reviewer {:id "reviewer-1"
                :authorization {:schema :wm/measured-a-authority-source-v1
                                :kind :test-fixture :id "reviewer-auth-1"
                                :bytes auth-bytes :sha256 (sut/sha256 auth-bytes)}}
     :acceptance (assoc core :artifact
                        {:schema :wm/measured-a-authority-source-v1
                         :kind :test-fixture :id "acceptance-1"
                         :bytes artifact-bytes :sha256 (sut/sha256 artifact-bytes)})}))

(defn- fixture []
  (let [a (annotation)
        au (authority a)
        criterion (get-in au [:rubric :criteria (:label a)])]
    [(assoc-in a [:rubric :criterion/id] criterion) au]))

(defn- reason [a au]
  (get-in (sut/validate a au) [:refusal :reason]))

(deftest accepts-structurally-complete-isolated-fixture
  (let [[a au] (fixture)
        result (sut/validate a au)]
    (is (:ok result))
    (is (= :addressed (:status result)))
    (is (= :isolated-test (:authority/scope result)))))

(deftest authority-and-binding-refusals
  (let [[a au] (fixture)]
    (testing "missing authority stays absent"
      (is (= :authority-missing (reason a nil))))
    (testing "borrowed acceptance"
      (let [[other other-au] (fixture)
            borrowed (assoc other :annotation/id "other-annotation")]
        (is (= :acceptance-subject-mismatch (reason a (authority borrowed))))))
    (testing "candidate-forged identity"
      (is (= :unexpected-field
             (reason (assoc a :observer {:id "forged"}) au))))
    (testing "rewritten label with unchanged acceptance"
      (let [rewritten (-> a
                          (assoc :label :refined)
                          (assoc-in [:subject :state] :refined)
                          (assoc-in [:rubric :criterion/id]
                                    (get-in au [:rubric :criteria :refined]))
                          (assoc-in [:evidence 0 :supports] [:refined]))]
        (is (= :acceptance-subject-mismatch (reason rewritten au))))
    (testing "source bytes are recomputed"
      (is (= :source-byte-hash-mismatch
             (reason a (assoc-in au [:observer :origin :bytes] "forged")))))))

(deftest evidence-and-time-refusals
  (let [[a au] (fixture)]
    (is (= :evidence-ambiguous
           (reason (assoc-in a [:evidence 0 :supports] [:addressed :refined]) au)))
    (is (= :evidence-conflicting
           (reason (assoc-in a [:evidence 0 :conflicts] [:falsified]) au)))
    (is (= :evidence-insufficient (reason (assoc a :evidence []) au)))
    (is (= :prohibited-label-source
           (reason (assoc-in a [:evidence 0 :role] :model-posterior) au)))
    (is (= :label-derived-from-forbidden-source
           (reason (assoc-in a [:provenance :label/derived-from] :model-posterior) au)))
    (is (= :temporal-order-invalid
           (reason (assoc-in a [:conditioning :action-at] "2026-09-13T12:00:03Z") au)))))

(deftest rubric-and-retrospective-refusals
  (let [[a au] (fixture)]
    (is (= :rubric-incomplete
           (reason a (update-in au [:rubric :criteria] dissoc :reopened))))
    (is (= :rubric-binding-mismatch
           (reason (assoc-in a [:rubric :criterion/id] :test/arbitrary-keyword) au)))
    (is (= :retrospective-provenance-missing
           (reason (-> a
                       (assoc-in [:provenance :mode] :retrospective)
                       (assoc-in [:provenance :retrospective]
                                 {:status :not-applicable}))
                   au)))))
