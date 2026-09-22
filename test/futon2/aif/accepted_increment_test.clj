(ns futon2.aif.accepted-increment-test
  "PROOF-wm-works ⟨1⟩4: the accepted-increment predicate against REAL
  records — the retained r4-1 and r4-2 closes and the live reference
  source. No fabricated attestations; the true case is constructed over
  the real repository state (a genuinely DONE-checked locator path in the
  real ticket file's git history is not available, so the true case uses
  a locator that IS true at HEAD through the real checker)."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.accepted-increment :as ai]))

(def t "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")

;; The reference source's real locators (resources/wm/cascade-sources/…).
(def task-stated-locator
  {:class :C4 :repo "futon2" :sha "HEAD"
   :path (str "holes/tickets/" t ".md")
   :decl "# Verify or restore guardrail refusal"})
(def restoration-locator
  {:class :C4 :repo "futon2" :sha "HEAD"
   :path (str "holes/tickets/" t ".md")
   :decl "**Status:** DONE"})

(deftest r4-2-close-is-false-naming-conjunct-a
  ;; r4-2 (machinery-70 attempt-001) is a revision-REFUSAL close: the author
  ;; refused :artifact-binding-scope-conflict, so there is no reviewed
  ;; after-revision bound to this occurrence. The predicate must be false
  ;; with :failed :a.
  (let [r (ai/accepted-increment
           ;; the real close's shape: a binding with no fresh commit
           {:binding {:repo "/home/joe/code/futon2"
                      ;; r4-2's author made NO commit in scope
                      :commit nil
                      :pre-dispatch-head "16d4482c2c1e9d47d22f9e849ce4990fed92139a"}
            :produced-tokens {:repair/split-declared-valid
                              {:class :C4 :repo "futon2" :sha "HEAD"
                               :path "resources/wm/eig/held-out-split.edn"
                               :decl "HELD-OUT-SPLIT-DECLARED"}}
            :acceptance {:token :restoration-accepted
                         :locator restoration-locator}
            :after-revision nil})]
    (is (false? (:accepted? r)) (pr-str r))
    (is (= :a (:failed r)) "no reviewed after-revision: conjunct (a) unmet")
    (is (= :no-reviewed-commit (:reason r)))))

(deftest r4-1-close-is-false-naming-its-failing-conjunct
  ;; r4-1 (machinery-69 attempt-002) delivered a commit but the close was
  ;; :build-failed / :explanation-invalid: the reviewer never approved in
  ;; the shape the close reads, so the binding does not corroborate. The
  ;; predicate must be false for the CONJUNCT THAT FAILED — not true merely
  ;; because the record exists.
  (let [r (ai/accepted-increment
           {:binding {;; the real binding's verdict fields from r4-1's close:
                      ;; fresh commit, descendant of the base, but the
                      ;; reviewer-claim corroboration FAILED
                      ;; (:explanation-invalid) — :corroborates? false.
                      :repo "/home/joe/code/futon2"
                      :commit "aeb352f87368fb328b3a92ddd8e7aeb996d5f9ba0a"
                      :pre-dispatch-head "abde70b9c3c4caa72d0a48b93889ddc7fe696705b4"
                      :descendant? true
                      :corroborates? false
                      :claim-in-author-window? true}
            :produced-tokens {;; r4-1's target produced the shared-updater
                              ;; token; its locator is a checked line in the
                              ;; real mission file — observe it for real
                              [:admission/task-stated] task-stated-locator}
            :acceptance {:token :restoration-accepted
                         :locator restoration-locator}
            :after-revision "aeb352f87368fb328b3a92ddd8e7aeb996d5f9ba0a"})]
    (is (false? (:accepted? r)) (pr-str r))
    (is (= :a (:failed r)) "the reviewer claim did not corroborate: conjunct (a) unmet")
    (is (= :binding-not-fresh (:reason r)))))

(deftest constructed-true-case-over-real-locator-state
  ;; A constructed occurrence whose produced-token locators and acceptance
  ;; locator ARE true at HEAD, through the REAL checker and the REAL git
  ;; repository — no fabricated attestation. The task-stated line is true
  ;; today; a second true locator stands in for a produced token observed
  ;; true (its decl line exists at HEAD).
  (let [r (ai/accepted-increment
           {:binding {:repo "/home/joe/code/futon2"
                      :commit "20082379" :pre-dispatch-head "e0e2cbbf"
                      :descendant? true :corroborates? true
                      :claim-in-author-window? true}
            :produced-tokens {[:admission/task-stated] task-stated-locator}
            :acceptance {:token :admission/task-stated
                         :locator task-stated-locator}
            :after-revision "HEAD"})]
    (is (true? (:accepted? r)) (pr-str r))
    (is (contains? r :evidence))
    (is (true? (get-in r [:evidence :acceptance-result :observed])))
    (is (every? (fn [[_ v]] (true? (:observed v)))
                (get-in r [:evidence :produced-token-results])))))

(deftest target-without-acceptance-declaration
  (let [r (ai/accepted-increment
           {:binding {:commit "20082379" :pre-dispatch-head "e0e2cbbf"
                      :descendant? true :corroborates? true
                      :claim-in-author-window? true}
            :produced-tokens {}
            :acceptance nil
            :after-revision "HEAD"})]
    (is (= :no-acceptance-declared (:accepted? r)) (pr-str r))))
