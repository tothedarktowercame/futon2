(ns futon2.aif.repository-plan-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]))

(deftest repository-plan-binds-required-and-allowed-repositories
  (let [heads (atom {"/workspace/repo-a" "a-base"
                     "/workspace/repo-b" "b-base"
                     "/workspace/repo-c" "c-base"})
        opts {:repo-head-observation-fn
              (fn [repo] {:repo repo :head (get @heads repo)
                          :observed-at-ms 100})
              :repo-for-path-fn
              (fn [path] (when (= path "/workspace/repo-b/work.md")
                           "/workspace/repo-b"))}
        code-state {:repo-heads (zipmap (keys @heads) (repeat "earlier"))}
        entry {:action {:mission-path "/workspace/repo-b/work.md"}}
        locators {[:want-a] {:class :C4 :repo "repo-a" :path "mission.md"}
                  [:unknown] {:class :C4 :repo "missing" :path "lost.md"}}
        plan (runner/repository-plan opts code-state entry locators)
        required-repos (set (keep :repo (:required plan)))]
    (is (= #{"/workspace/repo-a" "/workspace/repo-b"} required-repos))
    (is (= (set (keys @heads)) (set (map :repo (:allowed plan)))))
    (is (every? :base-head (:allowed plan)))
    (is (= :want-locator (get-in plan [:required 0 :reasons 0 :kind])))
    (is (= :action-path (get-in plan [:required 1 :reasons 0 :kind])))
    (is (some :unresolved (:required plan)))
    (let [original-digest (:digest plan)
          changed (do (swap! heads assoc "/workspace/repo-c" "c-next")
                      (runner/repository-plan opts code-state entry locators))]
      (is (not= original-digest (:digest changed)))
      (swap! heads assoc "/workspace/repo-c" "c-base")
      (is (= original-digest
             (:digest (runner/repository-plan
                       (assoc opts :repo-head-observation-fn
                              (fn [repo] {:repo repo :head (get @heads repo)
                                          :observed-at-ms 999}))
                       code-state entry locators)))
          "same heads observed later give the same plan"))
    (testing "a deferred context cannot drop a member or substitute a head"
      (let [context {:schema :wm/deferred-author-dispatch-context-v1
                     :target "target" :repository "/workspace/repo-a"
                     :pre-author-head {:head "a-base" :observed-at-ms 100}
                     :repository-plan plan
                     :job-id "job" :prompt-sha256 "prompt"}
            obligation {:target "target"
                        :failure-data {:deferred-author-dispatch-context context}}
            job {:job-id "job"}]
        (is (nil? (runner/deferred-author-context-failure obligation job)))
        (is (= :deferred-author-repository-plan-mismatch
               (runner/deferred-author-context-failure
                (assoc-in obligation
                          [:failure-data :deferred-author-dispatch-context
                           :repository-plan :allowed]
                          (pop (:allowed plan)))
                job)))
        (is (= :deferred-author-repository-plan-mismatch
               (runner/deferred-author-context-failure
                (assoc-in obligation
                          [:failure-data :deferred-author-dispatch-context
                           :repository-plan :allowed 0 :base-head]
                          "substituted")
                job)))))))
