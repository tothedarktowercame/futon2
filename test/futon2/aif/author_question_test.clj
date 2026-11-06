(ns futon2.aif.author-question-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.morning-brief :as brief]
            [futon2.aif.refusal-legality :as refusal-legality]))

(def mission-text
  "# Mission\nThe channel may mean stable or last green.\n")

(def question
  {:question "Which channel applies?"
   :span {:lines [2 2] :quote "The channel may mean stable or last green."}
   :alternatives ["Use stable" "Use last green"]})

(defn- job [q]
  {:result (str "FULL_LOOP_AUTHOR: QUESTION " (pr-str q))})

(defn- thrown-data [author-job target stage mission brief-root]
  (try
    (runner/throw-if-author-refused! author-job target stage mission brief-root)
    nil
    (catch clojure.lang.ExceptionInfo e (ex-data e))))

(defn- with-mission [f]
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "author-question" (make-array java.nio.file.attribute.FileAttribute 0)))
        file (io/file dir "M-example.md")]
    (spit file mission-text)
    (f file {:id "M-example"
             :source {:repo "test" :path (.getPath file)
                      :sha256 (identity/sha256 mission-text)}}
       (.getPath (io/file dir "brief")))))

(deftest cited-question-builds-checked-carrier-on-author-refusal
  (with-mission
    (fn [_ mission brief-root]
      (let [data (thrown-data (job question) "M-example" :author-wait mission brief-root)
            checked (:c2/question data)]
        (is (= :author-question (:outcome data)))
        (is (= :author-question (:failure-kind data)))
        (is (= :carrier (:status checked)))
        (is (= :wm/c2-question-v1 (get-in checked [:carrier :schema])))
        (is (= question
               (select-keys (merge {:span (get-in checked [:carrier :ambiguity :span])
                                    :alternatives (get-in checked [:carrier :ambiguity :alternatives])}
                                   (:carrier checked))
                            [:question :span :alternatives])))
        (is (brief/c2-receipt-valid? (:c2/brief-receipt data)))
        (is (= 1 (count (brief/items brief-root))))))))

(deftest invalid-author-questions-retain-typed-refusals-not-carriers
  (with-mission
    (fn [file mission brief-root]
      (doseq [[label q expected]
              [[:wrong-quote (assoc-in question [:span :quote] "not in the mission")
                :question-span-does-not-resolve]
               [:one-alternative (assoc question :alternatives ["only one"])
                :question-without-alternatives]]]
        (testing (name label)
          (let [checked (:c2/question
                         (thrown-data (job q) "M-example" :author-wait mission brief-root))]
            (is (= :refused (:status checked)))
            (is (= expected (:reason checked)))
            (is (nil? (:carrier checked))))))
      (is (empty? (brief/items brief-root)))
      (testing "bytes changed after selection"
        (spit file (str mission-text "drift\n"))
        (let [checked (:c2/question
                       (thrown-data (job question) "M-example" :author-wait mission brief-root))]
          (is (= {:status :refused :reason :stale-citation} checked)))))))

(deftest brief-publication-failures-and-refused-carriers-are-typed
  (with-mission
    (fn [_ mission brief-root]
      (testing "publication throws"
        (let [data (with-redefs [brief/queue-c2-question!
                                 (fn [& _] (throw (ex-info "no store" {})))]
                     (thrown-data (job question) "M-example" :author-wait
                                  mission brief-root))]
          (is (= :brief-publication-threw
                 (get-in data [:c2/brief-receipt :reason])))
          (is (nil? (:path (:c2/brief-receipt data))))))
      (testing "receipt belongs to another question digest"
        (let [other (assoc question :question "A different question?")
              other-data (thrown-data (job other) "M-example" :author-wait
                                      mission brief-root)
              other-receipt (:c2/brief-receipt other-data)
              data (with-redefs [brief/queue-c2-question!
                                 (fn [& _] other-receipt)]
                     (thrown-data (job question) "M-example" :author-wait
                                  mission brief-root))]
          (is (brief/c2-receipt-valid? other-receipt))
          (is (= {:status :failed :reason :brief-receipt-invalid}
                 (:c2/brief-receipt data)))))
      (testing "refused carrier does not call the brief writer"
        (let [calls (atom 0)
              bad (assoc-in question [:span :quote] "not present")
              data (with-redefs [brief/queue-c2-question!
                                 (fn [& _] (swap! calls inc))]
                     (thrown-data (job bad) "M-example" :author-wait
                                  mission brief-root))]
          (is (zero? @calls))
          (is (= {:status :not-published :reason :question-not-carrier}
                 (:c2/brief-receipt data))))))))

(deftest plain-refuse-remains-the-existing-guardrail-outcome
  (let [root (.getPath (.toFile (java.nio.file.Files/createTempDirectory
                                 "plain-refuse-brief"
                                 (make-array java.nio.file.attribute.FileAttribute 0))))
        brief-root (.getPath (io/file root "brief"))
        author-job {:result "FULL_LOOP_AUTHOR: REFUSE :mission-slice-freeze-standing"}
        classification (runner/author-refusal-classification author-job)
        data (try
               (runner/throw-if-author-refused! author-job "M-example" :author-wait
                                                nil brief-root)
               nil
               (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (= {:outcome :guardrail-refusal
            :refusal-reason ":mission-slice-freeze-standing"}
           classification))
    (is (= :guardrail-refusal (:outcome data)))
    (is (not (contains? data :c2/question)))
    (is (not (contains? data :c2/brief-receipt)))
    (is (not (.exists (io/file brief-root))))))

(deftest author-question-is-not-yet-a-legal-close
  (let [evidence (refusal-legality/evidence-for-close
                  :author-question {:failure-kind :author-question} nil false)]
    (is (= {:refusal :other} evidence))
    (is (= :debugger-error (refusal-legality/disposition evidence)))))

(deftest author-prompts-name-the-question-wire-and-its-limit
  (let [primary (#'runner/author-prompt
                 {:author "author" :reviewer "reviewer"
                  :target-repository "/repo" :target-repository-head "head"
                  :repository-plan {:allowed []}}
                 "M-example" {:id "M-example"} {} [])
        correction (#'runner/guardrail-continuation-prompt
                    "author" "M-example" "/repo" ":blocked" {})]
    (doseq [prompt [primary correction]]
      (is (str/includes? prompt "FULL_LOOP_AUTHOR: QUESTION"))
      (is (str/includes? prompt ":span {:lines [first last]"))
      (is (str/includes? prompt "not a substitute for bounded work")))))
