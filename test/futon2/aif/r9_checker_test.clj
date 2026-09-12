(ns futon2.aif.r9-checker-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.r9-checker :as r9]))

(def checker-sha (apply str (repeat 64 "a")))
(def verification {:repo "futon2" :path "verification.edn" :sha256 (apply str (repeat 64 "b"))})
(def commission {:agent-id "claude-15" :prompt "review exact artifact abc123"
                 :caller "joe" :surface "invoke-stream"})
(def producer {:job-id "invoke-real-producer" :agent-id "codex-17" :caller "zai-7"
               :artifact-ref "52856d8f" :trace-id "trace-producer"
               :finished-at "2026-09-12T15:00:00Z"})
(def reviewer {:job-id "invoke-real-reviewer" :agent-id "claude-15" :caller "zai-7"
               :artifact-ref "52856d8f" :trace-id "trace-reviewer"
               :request-digest (r9/request-digest commission)
               :execution {:executed true :tool-events 3 :command-events 3}
               :finished-at "2026-09-12T15:51:10Z"})
(def base
  {:role-binding {:author "codex-17" :reviewer "claude-15"}
   :producer-job producer :reviewer-job reviewer
   :subject {:boundary :test/witness-admission :artifact-ref "52856d8f" :digest "subject-digest"}
   :review-commission commission
   :verification-receipt verification
   :review-receipt {:reviewer "claude-15" :verification verification}
   :trace->job {"trace-producer" "invoke-real-producer"
                "trace-reviewer" "invoke-real-reviewer"}
   :checker-source-sha256 checker-sha
   :admission-at "2026-09-12T16:00:00Z"
   :ledger-source {:sha256 (apply str (repeat 64 "c"))}})

(defn refusal [input]
  (try (r9/check-independence input) nil
       (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest complete-joins-stop-at-anchor
  (is (= :r9/anchor-missing (refusal base)))
  (let [anchor {:schema :wm/r9-bootstrap-anchor-v1 :status :anchored
                :authority "operator:joe" :checker-source-sha256 checker-sha}
        result (r9/check-independence (assoc base :bootstrap-anchor anchor))]
    (is (= :wm/r9-independence-admission-v1 (:schema result)))
    (is (= :admitted (:decision result)))
    (is (= {:author "codex-17" :reviewer "claude-15"} (:roles result)))))

(deftest closed-refusals-derived-from-real-pair-shape
  (testing "identity refusals"
    (is (= :r9/author-equals-reviewer
           (refusal (assoc-in base [:role-binding :author] "claude-15"))))
    (is (= :r9/producer-identity-missing
           (refusal (assoc-in base [:producer-job :agent-id] nil))))
    (is (= :r9/reviewer-identity-missing
           (refusal (assoc-in base [:reviewer-job :agent-id] nil)))))
  (testing "binding and join refusals"
    (is (= :r9/artifact-binding-mismatch
           (refusal (assoc-in base [:producer-job :artifact-ref] "wrong"))))
    (is (= :r9/unjoinable-producer-reviewer-pair
           (refusal (assoc-in base [:reviewer-job :request-digest] "mutated"))))
    (is (= :r9/unjoinable-producer-reviewer-pair
           (refusal (assoc-in base [:trace->job "trace-reviewer"] "invoke-real-producer"))))
    (is (= :r9/review-execution-missing
           (refusal (assoc-in base [:reviewer-job :execution]
                              {:executed false :tool-events 0 :command-events 0}))))))
