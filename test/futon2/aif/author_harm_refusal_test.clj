(ns futon2.aif.author-harm-refusal-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]))

(def test-world
  {:path-exists? (constantly true)
   :production-data-roots (constantly ["/prod"])
   :default-branch (fn [_ _] "refs/heads/main")
   :joe-identities (constantly #{"joe" "@joe"})
   :local-destination? (constantly true)})

(defn classify [line]
  (runner/author-refusal-classification
   {:result (str line "\n")} nil nil false test-world))

(deftest harm-rule-matched-valid-force-push-is-harm-declined
  (let [result (classify
                "FULL_LOOP_AUTHOR: REFUSE-HARM {:rule :H2 :declined-action {:operation :git-push :repo \"/repo\" :remote \"origin\" :ref \"refs/heads/topic\" :force? true}}")]
    (is (= :harm-declined (:outcome result)))
    (is (= :H2 (get-in result [:harm-match :rule])))
    (is (re-matches #"[0-9a-f]{64}"
                    (get-in result [:harm-match :declined-action-digest])))))

(deftest harm-rule-matched-thrown-close-retains-the-match
  (let [job {:result
             "FULL_LOOP_AUTHOR: REFUSE-HARM {:rule :H2 :declined-action {:operation :git-push :repo \"/repo\" :remote \"origin\" :ref \"refs/heads/topic\" :force? true}}\n"}
        data (try
               (runner/throw-if-author-refused! job "M-target" :author-wait)
               nil
               (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (= :harm-declined (:outcome data)))
    (is (= :harm-declined (:failure-kind data)))
    (is (= :H2 (get-in data [:harm-match :rule])))
    (is (map? (get-in data [:harm-match :declined-action])))))

(deftest harm-rule-matched-cross-rule-action-is-invalid
  (let [result (classify
                "FULL_LOOP_AUTHOR: REFUSE-HARM {:rule :H2 :declined-action {:operation :delete :paths [\"/prod/a\"]}}")]
    (is (= :invalid-author-refusal (:outcome result)))
    (is (= :operation-not-in-rule (get-in result [:harm-refusal :reason])))))

(deftest harm-rule-matched-unreadable-claim-is-invalid
  (let [result (classify "FULL_LOOP_AUTHOR: REFUSE-HARM {not-edn")]
    (is (= :invalid-author-refusal (:outcome result)))
    (is (= :unreadable (get-in result [:harm-refusal :reason])))))

(deftest plain-refuse-remains-a-guardrail-refusal
  (is (= {:outcome :guardrail-refusal :refusal-reason ":blocked"}
         (classify "FULL_LOOP_AUTHOR: REFUSE :blocked"))))

(deftest author-prompt-lists-the-loaded-harm-rules
  (let [prompt (#'runner/author-prompt
                {:author "author" :reviewer "reviewer"
                 :target-repository "/repo" :target-repository-head "head"
                 :repository-plan {:allowed []}}
                "target" {:id "target"} {} [])]
    (is (str/includes? prompt "FULL_LOOP_AUTHOR: REFUSE-HARM"))
    (doseq [rule [:H1 :H2 :H3 :H4 :H5 :H6]]
      (is (str/includes? prompt (str rule))))))
