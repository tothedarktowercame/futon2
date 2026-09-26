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

(deftest review-binding-identity-and-authorship-refusals
  ;; claude-4 review additions
  (let [view (oa/observer-view subject)
        cutoff {:futon2 "abc123"}
        adj (oa/adjudication "claude-3" view :present cutoff)
        other (oa/adjudication "claude-3" (assoc view :token "other") :present cutoff)]
    (is (= :review-of-other-adjudication
           (:kind (oa/admit subject adj (oa/review "codex-5" other :concur)))))
    (is (= :observer-missing
           (:kind (oa/admit subject (assoc adj :observer nil) (oa/review "codex-5" adj :concur)))))
    (is (= :reviewer-missing
           (:kind (oa/admit subject adj (assoc (oa/review "codex-5" adj :concur) :reviewer nil)))))
    (is (= :authorship-undeclared
           (:kind (oa/admit (dissoc subject :author) adj (oa/review "codex-5" adj :concur)))))
    (is (= {:disposition :delivered}
           (:recorded-verdict (oa/admit subject adj (oa/review "codex-5" adj :concur)))))))

(def mechanical-subject
  {:token "decl-present" :token-class :C4
   :application "The declaration is present at the pinned revision."
   :evidence-pointers [{:repo "futon2" :sha "HEAD"
                        :resolved-sha "c9cba0b82" :path "src/futon2/aif/observation_admission.clj"
                        :decl "defn admit"}]
   :check-mechanism "C4/decl-present?@c9cba0b82"
   :check-cutoff {:repo "futon2" :sha "c9cba0b82"}
   :recorded-verdict true :author :none :enactor :none})

(defn- recomputation [s]
  (oa/adjudication "C4/reader-form@c9cba0b82" (oa/observer-view s)
                   :present (:check-cutoff s)))

(defn- mechanically-admit [s adj]
  (oa/admit s adj (oa/mechanical-review "mechanical-review-v1" s adj)))

(deftest self-truthed-is-refused-under-an-ordinary-review-too
  ;; A-S Revision 3 falsifier 1 through the non-mechanical path: the check's
  ;; own mechanism as observer, with a human/agent :concur review. admit must
  ;; refuse; before this clause it admitted the label.
  (let [s mechanical-subject
        adj (assoc (recomputation s) :observer (:check-mechanism s))
        result (oa/admit s adj (oa/review "codex-5" adj :concur))]
    (is (= :observer-is-check-mechanism (:kind result)))
    (is (nil? (oa/label-record s result)))))

(deftest mechanical-self-truthed-is-never-a-label
  (let [s mechanical-subject
        adj (assoc (recomputation s) :observer (:check-mechanism s))
        rev (oa/mechanical-review "mechanical-review-v1" s adj)
        result (oa/admit s adj rev)]
    (is (= :dispute (:verdict rev)))
    (is (= :self-truthed (:reason rev)))
    (is (= :review-not-concur (:kind result)))
    (is (= :self-truthed (get-in result [:data :reason])))
    (is (nil? (oa/label-record s result)))))

(deftest mechanical-view-and-cutoff-mismatches
  (let [s mechanical-subject adj (recomputation s)]
    (doseq [[bad reason kind] [[(assoc adj :view-digest "wrong") :view-mismatch :view-digest-mismatch]
                              [(assoc adj :cutoff {:repo "futon2" :sha "different"})
                               :cutoff-mismatch :review-not-concur]]]
      (let [rev (oa/mechanical-review "mechanical-review-v1" s bad)
            result (oa/admit s bad rev)]
        (is (= :dispute (:verdict rev)))
        (is (= reason (:reason rev)))
        (is (= kind (:kind result)))
        (when (= :review-not-concur kind)
          (is (= reason (get-in result [:data :reason]))))
        (is (nil? (oa/label-record s result)))))
    (let [bad (assoc adj :observer (:check-mechanism s) :view-digest "wrong" :cutoff {})]
      (is (= :self-truthed (:reason (oa/mechanical-review "reviewer" s bad))))
      (is (= :view-mismatch
             (:reason (oa/mechanical-review "reviewer" s (assoc bad :observer (:observer adj)))))))))

(deftest mechanical-identities-and-authorship
  (let [s mechanical-subject adj (recomputation s)]
    (doseq [[reviewer kind] [[nil :reviewer-missing]
                             [(:observer adj) :observer-is-reviewer]
                             [(:check-mechanism s) :reviewer-is-check-mechanism]]]
      (let [rev (oa/mechanical-review reviewer s adj)
            result (oa/admit s adj rev)]
        (is (= :dispute (:verdict rev)))
        (is (= kind (:kind rev)))
        (is (= (if (= kind :reviewer-is-check-mechanism) :review-not-concur kind)
               (:kind result)))
        (is (nil? (oa/label-record s result)))))
    (doseq [s [(dissoc s :check-mechanism) (assoc s :check-mechanism nil)
              (assoc s :check-mechanism "")]]
      (let [rev (oa/mechanical-review "reviewer" s adj)]
        (is (= :check-mechanism-undeclared (:kind rev)))
        (is (= :missing (:status rev)))
        (is (= :dispute (:verdict rev)))
        (is (nil? (oa/label-record s (oa/admit s adj rev))))))
    (let [undeclared (dissoc s :author)
          result (mechanically-admit undeclared adj)]
      (is (= :authorship-undeclared (:kind result)))
      (is (nil? (oa/label-record undeclared result))))))

(deftest mechanical-positive-label-and-tick-independent-key
  (let [s mechanical-subject adj (recomputation s)
        rev (oa/mechanical-review "mechanical-review-v1" s adj)
        result (oa/admit s adj rev)
        label (oa/label-record s result)
        key [:C4 "futon2" "c9cba0b82" "src/futon2/aif/observation_admission.clj"
             "defn admit" "C4/decl-present?@c9cba0b82"]]
    (is (= (assoc (oa/review "mechanical-review-v1" adj :concur) :mechanical true) rev))
    (is (= :admitted (:status result)))
    (is (= {:token-class :C4 :recorded true :admitted :present
            :label-key key :admission result} label))
    (is (= 1 (count (distinct (map :label-key
                                  (for [tick [1 2]
                                        :let [s (assoc s :tick tick :run-id (str "run-" tick))]]
                                    (oa/label-record s (mechanically-admit s (recomputation s)))))))))
    (is (not= key (oa/label-key (assoc s :check-mechanism "C4/decl-present?@new-sha"))))
    (is (= [:C4 "futon2" "resolved" "entry" nil (:check-mechanism s)]
           (oa/label-key (assoc s :evidence-pointers [{:repo "futon2" :sha "resolved" :entry "entry"}]))))
    (is (= rev (oa/mechanical-review "mechanical-review-v1" (assoc s :recorded-verdict false) adj))
        "the review does not inspect the recorded verdict")
    (doseq [finding [:insufficient :ambiguous :conflicting]]
      (let [result (mechanically-admit s (assoc adj :finding finding))]
        (is (= :no-label (:kind result)))
        (is (nil? (oa/label-record s result)))))))
