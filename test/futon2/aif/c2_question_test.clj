(ns futon2.aif.c2-question-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.c2-question :as sut]))

(def mission-text
  (str "# Mission\n"
       "Choose the deployment target.\n"
       "Use the stable channel.\n"))

(def source-pin
  {:repo "futon2"
   :path "holes/missions/M-example.md"
   :sha256 (identity/sha256 mission-text)})

(def question
  {:question "Does stable mean the LTS tag or the last green build?"
   :span {:lines [3 3] :quote "Use the stable channel."}
   :alternatives ["Use the LTS tag" "Use the last green build"]})

(def reading-receipt
  {:request-id "reading-1"
   :response-id "response-1"
   :answered-by {:seat "reader" :job-id "job-1"}})

(deftest question-needs-cited-ambiguity-valid-carrier
  (let [result (sut/question-carrier "M-example" mission-text source-pin
                                     question reading-receipt)
        carrier (:carrier result)]
    (is (= :carrier (:status result)))
    (is (= :wm/c2-question-v1 (:schema carrier)))
    (is (= "M-example" (:target carrier)))
    (is (= (:question question) (:question carrier)))
    (is (= source-pin (get-in carrier [:ambiguity :source])))
    (is (= (:span question) (get-in carrier [:ambiguity :span])))
    (is (= (:alternatives question)
           (get-in carrier [:ambiguity :alternatives])))
    (is (= reading-receipt (:reading-receipt carrier)))
    (is (string? (:question-digest carrier)))
    (is (sut/ambiguity-cited? carrier mission-text))))

(deftest question-needs-cited-ambiguity-refuses-invalid-question
  (doseq [[label changed reason]
          [[:wrong-quote (assoc-in question [:span :quote] "other text")
            :question-span-does-not-resolve]
           [:one-alternative (assoc question :alternatives ["only one"])
            :question-without-alternatives]
           [:blank-question (assoc question :question "  ")
            :question-not-stated]]]
    (testing (name label)
      (let [result (sut/question-carrier "M-example" mission-text source-pin
                                         changed reading-receipt)]
        (is (= :refused (:status result)))
        (is (= reason (:reason result)))))))

(deftest question-needs-cited-ambiguity-refuses-stale-source
  (let [result (sut/question-carrier "M-example"
                                     (str mission-text "Changed.\n")
                                     source-pin question reading-receipt)]
    (is (= {:status :refused :reason :stale-citation}
           result))))

(deftest question-digest-is-canonical-and-span-sensitive
  (let [reordered (array-map :alternatives (:alternatives question)
                             :span (array-map :quote (get-in question [:span :quote])
                                              :lines (get-in question [:span :lines]))
                             :question (:question question))
        a (sut/question-carrier "M-example" mission-text source-pin
                                question reading-receipt)
        b (sut/question-carrier "M-example" mission-text
                                (array-map :sha256 (:sha256 source-pin)
                                           :path (:path source-pin)
                                           :repo (:repo source-pin))
                                reordered reading-receipt)
        changed (assoc-in question [:span :lines] [2 3])
        changed (assoc-in changed [:span :quote]
                          "Choose the deployment target.\nUse the stable channel.")
        c (sut/question-carrier "M-example" mission-text source-pin
                                changed reading-receipt)]
    (is (= (get-in a [:carrier :question-digest])
           (get-in b [:carrier :question-digest])))
    (is (not= (get-in a [:carrier :question-digest])
              (get-in c [:carrier :question-digest])))))
