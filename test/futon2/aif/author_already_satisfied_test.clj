(ns futon2.aif.author-already-satisfied-test
  "PROOF-2b (click 14, tick-run-record-2026-09-30-1790742487): the machine
  re-selected already-committed work; the author answered correctly — a
  feature card stating the want already holds and :artifact-ref naming futon7
  891001d — but the runner closed :invalid-author-refusal and opened a repair
  finding. Reporting already-satisfied work, naming the commit that made it
  hold, is a valid answer: it closes :already-satisfied with that commit as
  the grounded commit. The reply grammar is the prompt's own feature card
  (:built / :want-coverage); no new marker."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]))

(def futon7-sha "891001d08d8b37eaf50e13f985dee30c8d34340a")

(def futon7-root "/home/joe/code/futon7") ;; the real repo: click 14 grounded commit 891001d

(def click-14-reply
  (str "The PSR pattern library change already landed; nothing to build.\n\n"
       "FULL_LOOP_AUTHOR: REFUSE the want already holds\n\n"
       "FULL_LOOP_FEATURE_CARD: {:built \"none: PSR already landed\", "
       ":want-coverage \"already true\", :matches-intent? true, "
       ":things-to-try [\"mission checklist -> 891001d\"]}\n"))

(defn- author-job [& {:as extra}]
  (merge {:result click-14-reply} extra))

(deftest already-true-with-an-existing-commit-is-already-satisfied
  (let [r (runner/author-refusal-classification
           (author-job :artifact-ref futon7-sha)
           "/home/joe/code/futon7"
           (fn [repo sha] (and (= "/home/joe/code/futon7" repo)
                               (= futon7-sha sha))))]
    (is (= {:outcome :already-satisfied
            :grounded-commit {:repo "futon7" :sha futon7-sha}}
           r))
    (is (= {:repo "futon7" :sha futon7-sha}
           (:grounded-commit
            (try (runner/throw-if-author-refused!
                  (author-job :artifact-ref futon7-sha
                              :artifact-binding {:repo futon7-root})
                  "M-x" :author-wait)
                 nil
                 (catch clojure.lang.ExceptionInfo e (ex-data e)))))
        "the close carries the named commit as the grounded commit")))

(deftest already-true-with-a-nonexistent-commit-stays-invalid
  (is (= {:outcome :invalid-author-refusal}
         (runner/author-refusal-classification
          (author-job :artifact-ref futon7-sha)
          "/home/joe/code/futon7"
          (constantly false)))))

(deftest already-true-with-no-commit-named-stays-invalid
  (is (= {:outcome :invalid-author-refusal}
         (runner/author-refusal-classification
          (author-job)
          "/home/joe/code/futon7"
          (constantly true)))))

(deftest a-reasoned-refusal-without-a-card-or-artifact-is-still-guardrail
  (is (= {:outcome :guardrail-refusal :refusal-reason "out of scope"}
         (runner/author-refusal-classification
          {:result "FULL_LOOP_AUTHOR: REFUSE out of scope\n"}
          "/home/joe/code/futon7"
          (constantly true)))))

(deftest already-satisfied-is-a-valid-cohort-close-outcome
  ;; demo clicks run as cohort clicks; an outcome missing here fails
  ;; close-attempt! :unknown-outcome and opens a machine-failure finding
  (is (contains? @(requiring-resolve 'futon2.aif.full-loop-cohort/outcome-kinds)
                 :already-satisfied)))
