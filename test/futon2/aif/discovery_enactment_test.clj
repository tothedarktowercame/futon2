(ns futon2.aif.discovery-enactment-test
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]))

(def construction
  (edn/read-string
   (slurp "test/fixtures/narrative-trace/construction-1789964661.edn")))

(def prompt-opts
  {:author "author" :reviewer "reviewer"
   :target-repository "/repo" :target-repository-head "head"
   :repository-plan {:allowed []}})

(def discovery-construction
  (assoc construction :outer-enactment
         {:mode :discovery :outer-cascade-id :find-out-before-building}))

(deftest discovery-prompt-is-gated-and-implementation-is-unchanged
  (let [target (:mission construction)
        mission {:id target}
        old-prompt (#'runner/author-prompt prompt-opts target mission
                                           discovery-construction [])
        off-prompt (runner/author-enactment-prompt
                    {:wm/discovery-enactment :off} prompt-opts target mission
                    discovery-construction [])
        on-prompt (runner/author-enactment-prompt
                   {:wm/discovery-enactment :on} prompt-opts target mission
                   discovery-construction [])
        implementation (assoc discovery-construction :outer-enactment
                              {:mode :implementation
                               :outer-cascade-id :giraffe-order})]
    (is (= old-prompt off-prompt))
    (is (str/includes? on-prompt "FULL-LOOP DISCOVERY OPPORTUNITY"))
    (is (str/includes? on-prompt "FULL_LOOP_DISCOVERY:"))
    (is (str/includes? on-prompt "read-only task"))
    (is (not (str/includes? on-prompt "REPOSITORY ARTIFACT CONTRACT")))
    (is (str/includes? on-prompt "Do not emit FULL_LOOP_ARTIFACTS"))
    (is (str/includes? on-prompt "do not emit FULL_LOOP_AUTHOR: DONE"))
    (doseq [switch [:on :off]]
      (is (= (#'runner/author-prompt prompt-opts target mission implementation [])
             (runner/author-enactment-prompt
              {:wm/discovery-enactment switch} prompt-opts target mission
              implementation []))))
    (is (= :discovery-enactment-option-required
           (:failure-kind
            (try
              (runner/author-enactment-prompt
               {} prompt-opts target mission discovery-construction [])
              (catch clojure.lang.ExceptionInfo e (ex-data e))))))))

(defn- wire [item]
  (str "FULL_LOOP_DISCOVERY: "
       (pr-str {:schema :wm/find-out-result-v1
                :target "M-example" :items [item]})))

(def good-question
  {:question "Which reading applies?"
   :span {:lines [4 5] :quote "ambiguous mission text"}
   :alternatives ["reading A" "reading B"]})

(deftest discovery-result-parser-types-malformed-items
  (is (= :accepted
         (:status (runner/parse-discovery-result
                   (wire {:verdict :current
                          :evidence ["holes/M-example.md:25-33"]})))))
  (doseq [[label item kind]
          [["missing verdict" {:evidence ["x:1"]}
            :find-out-result/verdict-missing]
           ["unknown verdict" {:verdict :maybe :evidence ["x:1"]}
            :find-out-result/verdict-unknown]
           ["missing evidence" {:verdict :current :evidence []}
            :find-out-result/evidence-missing]
           ["blocked without questions" {:verdict :blocked :evidence ["x:1"]}
            :find-out-result/blocked-questions-missing]]]
    (testing label
      (is (= kind (:kind (runner/parse-discovery-result (wire item)))))))
  (is (= :accepted
         (:status (runner/parse-discovery-result
                   (wire {:verdict :blocked :evidence ["x:1"]
                          :questions [good-question]}))))))
