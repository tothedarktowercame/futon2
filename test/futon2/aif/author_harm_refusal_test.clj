(ns futon2.aif.author-harm-refusal-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.harm-rules]))

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

(def push-to-main
  "FULL_LOOP_AUTHOR: REFUSE-HARM {:rule :H2 :declined-action {:operation :git-push :repo \"/repo\" :remote \"origin\" :ref \"main\" :force? false}}")

(deftest unforced-push-to-default-branch-matches-in-either-ref-spelling
  (is (= :harm-declined (:outcome (classify push-to-main))))
  (is (= :harm-declined
         (:outcome (classify (str/replace push-to-main "\"main\"" "\"refs/heads/main\""))))))

(deftest unknown-default-branch-never-matches-an-unforced-push
  (doseq [default [nil "" "  "]]
    (let [result (runner/author-refusal-classification
                  {:result (str push-to-main "\n")} nil nil false
                  (assoc test-world :default-branch (fn [_ _] default)))]
      (is (= :invalid-author-refusal (:outcome result)))
      (is (= :check-failed (get-in result [:harm-refusal :reason]))))))

(deftest production-world-treats-urls-as-leaving-the-host
  (let [local? (:local-destination? (runner/production-harm-world))]
    (doseq [d ["https://github.com/x/y" "http://example.org" "ssh://host/repo"
               "git@github.com:x/y.git"]]
      (is (false? (local? d)) d))
    (doseq [d ["/home/joe/notes/x.md" "notes/x.md" "/var/tmp/x" "file:///tmp/x"]]
      (is (true? (local? d)) d))))

(deftest external-post-by-another-actor-matches-h4-in-production-world
  (let [world (assoc (runner/production-harm-world)
                     :joe-identities (constantly #{"joe"}))
        job {:result "FULL_LOOP_AUTHOR: REFUSE-HARM {:rule :H4 :declined-action {:operation :publish :actor \"codex-15\" :destination \"https://github.com/x/y\"}}\n"}]
    (is (= :harm-declined
           (:outcome (runner/author-refusal-classification job nil nil false world))))))

(deftest author-prompt-is-generated-from-the-rule-file
  (let [prompt (#'runner/author-prompt
                {:author "author" :reviewer "reviewer"
                 :target-repository "/repo" :target-repository-head "head"
                 :repository-plan {:allowed []}}
                "target" {:id "target"} {} [])]
    (doseq [{:keys [doc requires operations]}
            (:rules (futon2.aif.harm-rules/load-rules))]
      (is (str/includes? prompt doc))
      (doseq [f (concat requires operations)]
        (is (str/includes? prompt (str f)))))))
