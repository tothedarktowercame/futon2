(ns futon2.aif.artifact-declaration-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]))

;; Tail copied from the retained author reply in
;; data/wm-runs/tick-run-record-2026-09-19-1789828199.edn. Tests add the new
;; declaration immediately before the existing terminal DONE marker.
(def retained-reply-tail
  (str "- No unrelated dirty files included; repair ledger untouched\n\n"
       "FULL_LOOP_AUTHOR: DONE 9a6a012f70bc391123a633b90685017ae045449f"))

(def plan
  {:schema :wm/repository-plan-v1
   :required []
   :allowed [{:repo "/workspace/repo-a" :base-head "a-base"}
             {:repo "/workspace/repo-b" :base-head "b-base"}]
   :digest "plan-digest"})

(defn declared-job [members]
  {:job-id "author-job" :state "done" :artifact-ref "9a6a012f"
   :result (str (subs retained-reply-tail 0
                      (.lastIndexOf retained-reply-tail "FULL_LOOP_AUTHOR:"))
                "FULL_LOOP_ARTIFACTS: " (pr-str members) "\n"
                "FULL_LOOP_AUTHOR: DONE 9a6a012f70bc391123a633b90685017ae045449f")})

(deftest author-artifact-declaration-is-parsed-and-retained
  (let [members [{:repo "/workspace/repo-a" :commit "aaaaaaaa"}
                 {:repo "/workspace/repo-b" :commit "bbbbbbbb"}]
        job (runner/retain-artifact-declaration (declared-job members) plan)]
    (is (= {:schema :wm/artifact-declaration-v1
            :status :present
            :members members
            :plan-digest "plan-digest"}
           (:artifact-declaration job)))
    (is (= "9a6a012f" (:artifact-ref job))
        "retention does not change existing single-repository behavior"))
  (testing "a repository outside the retained plan is typed and retained"
    (let [job (runner/retain-artifact-declaration
               (declared-job [{:repo "/workspace/repo-z" :commit "cccccccc"}])
               plan)]
      (is (= :undeclared-repo (get-in job [:artifact-declaration :status])))
      (is (= ["/workspace/repo-z"]
             (get-in job [:artifact-declaration :undeclared-repos])))))
  (testing "duplicate repositories are typed and retained"
    (is (= :duplicate-repo
           (get-in
            (runner/retain-artifact-declaration
             (declared-job [{:repo "/workspace/repo-a" :commit "aaaaaaaa"}
                            {:repo "/workspace/repo-a" :commit "bbbbbbbb"}])
             plan)
            [:artifact-declaration :status]))))
  (testing "malformed EDN is typed and retained"
    (let [job {:state "done" :artifact-ref "9a6a012f"
               :result (str "FULL_LOOP_ARTIFACTS: [{:repo]\n"
                            "FULL_LOOP_AUTHOR: DONE 9a6a012f") }]
      (is (= :unparseable
             (get-in (runner/retain-artifact-declaration job plan)
                     [:artifact-declaration :status])))))
  (testing "an ordinary retained author reply remains the absent case"
    (is (= {:schema :wm/artifact-declaration-v1
            :status :absent
            :plan-digest "plan-digest"}
           (:artifact-declaration
            (runner/retain-artifact-declaration
             {:state "done" :artifact-ref "9a6a012f" :result retained-reply-tail}
             plan))))))
