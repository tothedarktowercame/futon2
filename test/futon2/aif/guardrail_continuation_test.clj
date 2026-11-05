(ns futon2.aif.guardrail-continuation-test
  "The author-wait guardrail seam gets one bounded chance to do useful work."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]))

(defn- refusal [job-id]
  {:job-id job-id :agent-id "zai-5" :state "done"
   :result (str "FULL_LOOP_AUTHOR: REFUSE "
                ":artifact-gate-want-locators-outside-target-repository")})

(defn- committed [job-id]
  {:job-id job-id :agent-id "zai-5" :state "done"
   :artifact-ref "a2396d69"
   :feature-card {:built "useful partial parcel"
                  :want-coverage "one token left false; limitation stated"
                  :matches-intent? true
                  :things-to-try ["focused regression -> pass"]}
   :result (str "Useful partial parcel committed; limitation retained.\n"
                "FULL_LOOP_AUTHOR: DONE a2396d69")})

(defn- correction [corrected]
  (let [dispatches (atom [])
        prompts (atom [])
        opts {:dispatch-fn
              (fn [_ author caller target prompt]
                (swap! dispatches conj [author caller target])
                (swap! prompts conj prompt)
                {:job-id "correction-1"})
              :poll-fn (fn [_ _] corrected)}
        result
        (with-redefs-fn
          {#'runner/run-phase! (fn [_ _ _ thunk & _] (thunk))}
          #(runner/guardrail-continuation-round
            opts {} "zai-5" (atom 0) "M-patterns-done-right" "/repo"
            (refusal "author-1")))]
    {:dispatches @dispatches :prompts @prompts :result result}))

(deftest refusal-then-commit-falls-through-to-review
  (let [{:keys [dispatches prompts result]} (correction (committed "correction-1"))
        author-job (:author-job result)
        binding (runner/fresh-artifact-binding
                 {:repo-head-observation-fn
                  (fn [repo] {:repo repo :head "a2396d69" :observed-at-ms 2000})
                  :resolve-commit-sha-fn
                  (fn [_ sha] (when (= sha "a2396d69") sha))
                  :ancestor-fn (constantly true)
                  :commit-time-ms-fn (constantly 1500)}
                 "/repo" {:repo "/repo" :head "base000" :observed-at-ms 1000}
                 author-job)]
    (is (= [["zai-5" "wm-full-loop" "M-patterns-done-right"]] dispatches))
    (is (re-find #"Partial grounded progress is accepted" (first prompts)))
    (is (re-find #"artifact-gate-want-locators-outside-target-repository"
                 (first prompts)))
    (is (nil? (runner/throw-if-author-refused!
               author-job "M-patterns-done-right" :author-wait)))
    (is (= "a2396d69" (:commit binding)))
    ;; The corrected response passes the production refusal and fresh-artifact
    ;; seams above, which is what the caller requires before reviewer dispatch.
    (is (= :returned (get-in author-job [:guardrail-continuation :status])))))

(deftest two-refusals-preserve-the-existing-terminal-record
  (let [{:keys [dispatches result]} (correction (refusal "correction-1"))
        failure (try
                  (runner/throw-if-author-refused!
                   (:author-job result) "M-patterns-done-right" :author-wait)
                  nil
                  (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (= 1 (count dispatches)))
    (is (= :guardrail-refusal (:outcome failure)))
    (is (= :guardrail-refusal (:failure-kind failure)))
    (is (= :author-wait (:failure-stage failure)))
    (is (= ":artifact-gate-want-locators-outside-target-repository"
           (:refusal-reason failure)))))
