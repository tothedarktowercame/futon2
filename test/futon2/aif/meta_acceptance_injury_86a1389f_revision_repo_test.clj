(ns futon2.aif.meta-acceptance-injury-86a1389f-revision-repo-test
  "Out-of-band acceptance for injury/86a1389f8ca991f9fff0bc6d8abe12ab0e5b984f."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner]))

(def repo "/home/joe/code/futon2")
(def base-head "fbe2d376e47a097a7b5e6cc0737ef3f092459391")
(def findings
  (str "FULL_LOOP_REVIEW: REQUEST_CHANGES the ticket status line is stale; "
       "commit in futon4, or wherever the ticket is canonically edited."))

(defn- revision-prompt []
  (let [prompt-fn (var-get
                   (ns-resolve 'futon2.aif.full-loop-runner
                               'revision-author-prompt))
        common ["author" "reviewer" "/tmp/attempt-001/evidence"]
        tail ["T-arxana-direct-reads" {} [base-head] findings]]
    (try
      (apply prompt-fn (concat common [repo base-head] tail))
      (catch clojure.lang.ArityException _
        ;; The counterfactual's prompt has the former arity.  Calling it lets
        ;; the missing repository contract fail below as assertions, not error.
        (apply prompt-fn (concat common tail))))))

(deftest injury-86a1389f-revision-is-bound-to-the-run-repository
  (let [prompt (revision-prompt)]
    (testing "the revision author is told the run's bound repository"
      (is (re-find #"TARGET REPOSITORY: \"/home/joe/code/futon2\"" prompt)))
    (testing "a reviewer-named repository cannot move the artifact binding"
      (is (re-find #"a commit in any other repository" prompt))
      (is (re-find #"artifact-binding mismatch" prompt)))))
