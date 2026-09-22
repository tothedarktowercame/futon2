(ns futon2.aif.accepted-increment-test
  "Predicate unit cases and verbatim retained checkpoint regressions."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
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

(deftest constructed-missing-commit-is-false-naming-conjunct-a
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

(deftest constructed-stale-binding-is-false-naming-its-failing-conjunct
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

(defn retained-input [label]
  (let [f (edn/read-string
           (slurp (io/resource (str "fixtures/accepted-increment/" label ".edn"))))]
    {:binding (get-in f [:build :validation :artifact-binding])
     :token-rows (get-in f [:token-outcome-comparison :tokens])
     :acceptance (get-in f [:selection :controller-decision :action
                            :accepted-increment :acceptance])
     :after-revision (get-in f [:adjudication :build-match :commit])}))

(deftest real-live-row-maps-preserve-both-observations-and-revision
  (let [input (retained-input "machinery-71-attempt-002")
        rows (:token-rows input)
        verdict (ai/evaluate-close input)
        results (get-in verdict [:evidence :produced-token-results])]
    (is (vector? rows))
    (is (every? map? rows))
    (is (= :no-acceptance-declared (:accepted? verdict))
        "The actual selection declares no acceptance; do not invent one.")
    (is (= (into {} (map (juxt :token :observed)) rows)
           (into {} (map (fn [[t r]] [t (:observed r)])) results)))
    (is (= #{(:after-revision input)}
           (set (map #(get-in % [:evidence :resolved-sha]) (vals results)))))))

(deftest retained-older-shapes-do-not-throw
  (doseq [label ["r4-1" "r4-2"]]
    (let [input (retained-input label)
          verdict (ai/evaluate-close input)]
      (is (= :no-acceptance-declared (:accepted? verdict)) label)
      (is (= (count (:token-rows input))
             (count (get-in verdict [:evidence :produced-token-results]))) label)))
  (is (nil? (:token-rows (retained-input "r4-1"))))
  (is (every? #(nil? (:measurement %)) (:token-rows (retained-input "r4-2")))))

(deftest each-conjunct-still-required
  ;; Counterfactuals, explicitly separate from the unchanged record replay:
  ;; declare acceptance using the actual measured true/false locators.
  (let [input (retained-input "machinery-71-attempt-002")
        [false-row true-row] (:token-rows input)
        declaration (fn [row] {:token (:token row)
                              :locator (get-in row [:measurement :after-locator])})
        good (assoc input :token-rows [true-row] :acceptance (declaration true-row))]
    (is (true? (:accepted? (ai/evaluate-close good))))
    (is (= :a (:failed (ai/evaluate-close (assoc good :binding nil)))))
    (is (= :b (:failed (ai/evaluate-close (assoc good :token-rows [false-row true-row])))))
    (is (= :c (:failed (ai/evaluate-close (assoc good :acceptance (declaration false-row))))))
    (is (= :b (:failed (ai/evaluate-close
                       (assoc good :token-rows [(assoc true-row :measurement nil)])))))))

(deftest genuine-adapter-error-is-typed-evidence
  (let [r (ai/evaluate-close (assoc (retained-input "machinery-71-attempt-002")
                                   :token-rows [[:not-a-row]]))]
    (is (= :refused (:accepted? r)))
    (is (= :predicate-evaluation-failed (:reason r)))))
