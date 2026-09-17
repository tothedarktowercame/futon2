(ns futon2.aif.observation-admission-test
  "Controls for the observation admission steps: blinding by construction,
  every typed refusal, one admitted :present and one admitted :absent, and the
  unobserved-is-not-absent boundary."
  (:require [clojure.test :refer [are deftest is]]
            [futon2.aif.observation-admission :as oa]))

(def subject
  {:token "tests-passed"
   :application "The test suite for the memory carrier ran green at the pinned revision."
   :evidence-pointers [{:repo "futon2" :sha "abc123" :path "test/..."}]
   :recorded-verdict {:disposition :delivered}
   :author "codex-22"
   :enactor "codex-22"})

(deftest observer-view-is-blind-by-construction
  (let [view (oa/observer-view subject)]
    (is (= (:token subject) (:token view)))
    (is (= (:application subject) (:application view)))
    (is (= (:evidence-pointers subject) (:evidence-pointers view)))
    (is (not (contains? view :recorded-verdict)))
    (is (not (contains? view :author)))
    (is (not (contains? view :enactor)))
    (is (not (some #(re-find #"[Cc]odex" (pr-str %)) [(pr-str view)])))))

(deftest admitted-present-and-absent-labels
  (let [view (oa/observer-view subject)
        cutoff {:futon2 "abc123"}
        adj (oa/adjudication "claude-3" view :present cutoff)
        rev (oa/review "codex-5" adj :concur)
        admitted (oa/admit subject adj rev)]
    (is (= :admitted (:status admitted)))
    (is (= :present (:label admitted)))
    (is (= "tests-passed" (:token admitted)))
    (is (= cutoff (:cutoff admitted))))
  (let [subject-absent (assoc subject :token "witness-attached"
                              :application "No witness is attached anywhere in the pinned tree.")
        view (oa/observer-view subject-absent)
        adj (oa/adjudication "claude-3" view :absent {:futon2 "abc123"})
        admitted (oa/admit subject-absent adj (oa/review "codex-5" adj :concur))]
    (is (= :admitted (:status admitted)))
    (is (= :absent (:label admitted)))))

(deftest every-refusal-is-typed
  (let [view (oa/observer-view subject)
        cutoff {:futon2 "abc123"}]
    (are [expected adjudication review-record]
      (= expected [(:status (oa/admit subject adjudication review-record))
                   (:kind (oa/admit subject adjudication review-record))])
      [:missing :invalid-finding] (oa/adjudication "claude-3" view :maybe cutoff)
      (oa/review "codex-5" (oa/adjudication "claude-3" view :present cutoff) :concur)
      [:missing :invalid-verdict] (oa/adjudication "claude-3" view :present cutoff)
      (oa/review "codex-5" (oa/adjudication "claude-3" view :present cutoff) :unsure)
      [:missing :observer-is-author] (oa/adjudication "codex-22" view :present cutoff)
      (oa/review "codex-5" (oa/adjudication "claude-3" view :present cutoff) :concur)
      [:missing :observer-is-reviewer] (oa/adjudication "claude-3" view :present cutoff)
      (oa/review "claude-3" (oa/adjudication "claude-3" view :present cutoff) :concur)
      [:missing :cutoff-missing] (oa/adjudication "claude-3" view :present {})
      (oa/review "codex-5" (oa/adjudication "claude-3" view :present {}) :concur)
      [:missing :view-digest-mismatch] (assoc (oa/adjudication "claude-3" view :present cutoff)
                                              :view-digest "something-else")
      (oa/review "codex-5" (oa/adjudication "claude-3" view :present cutoff) :concur)
      [:missing :no-label] (oa/adjudication "claude-3" view :insufficient cutoff)
      (oa/review "codex-5" (oa/adjudication "claude-3" view :insufficient cutoff) :concur)
      [:missing :no-label] (oa/adjudication "claude-3" view :ambiguous cutoff)
      (oa/review "codex-5" (oa/adjudication "claude-3" view :ambiguous cutoff) :concur)
      [:missing :no-label] (oa/adjudication "claude-3" view :conflicting cutoff)
      (oa/review "codex-5" (oa/adjudication "claude-3" view :conflicting cutoff) :concur)
      [:missing :review-not-concur] (oa/adjudication "claude-3" view :present cutoff)
      (oa/review "codex-5" (oa/adjudication "claude-3" view :present cutoff) :dispute)
      [:missing :review-not-concur] (oa/adjudication "claude-3" view :present cutoff)
      (oa/review "codex-5" (oa/adjudication "claude-3" view :present cutoff) :insufficient))))

(deftest unobserved-is-not-absent
  ;; A subject with a mutated view (observer saw something else) never admits
  ;; ANY label — in particular it is not turned into an :absent one.
  (let [view (oa/observer-view subject)
        adj (oa/adjudication "claude-3" view :absent {:futon2 "abc123"})
        mutated (assoc subject :application "different application text")
        out (oa/admit mutated adj (oa/review "codex-5" adj :concur))]
    (is (= :missing (:status out)))
    (is (= :view-digest-mismatch (:kind out)))
    (is (not (contains? out :label))))
  ;; An adjudicated present with a dispute review admits nothing either.
  (let [view (oa/observer-view subject)
        adj (oa/adjudication "claude-3" view :present {:futon2 "abc123"})
        out (oa/admit subject adj (oa/review "codex-5" adj :dispute))]
    (is (= :missing (:status out)))
    (is (not (contains? out :label)))))
