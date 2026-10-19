(ns futon2.aif.selection-refusal-record-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.meta-terminal-injury :as terminal]))

(def missing-provider :outer-policy-evidence-provider-required)
(def source-pin {:path "fixtures/selection.edn" :sha256 (apply str (repeat 64 "a"))})

(defn recorded [reason]
  (let [judgement {:decision {:status :abstained
                              :refusals [{:target {:absent :outer-task-not-selected}
                                          :kind :outer-task-selection-refused
                                          :reason reason}]}
                   :outer-task-selection {:status :refused
                                          :policy {:status :refused :reason reason}}}
        error (runner/selection-terminal-condition :selection judgement)]
    {:run/id "run-refused" :click/id "click-refused"
     :failure (#'runner/run-record-failure
                {:data {:failure-kind :abstained :failure-stage :selection
                        :error (ex-message error) :error-data (ex-data error)}})}))

(deftest missing-provider-survives-the-terminal-record-and-opens-an-injury
  (let [record (recorded missing-provider)]
    (is (= missing-provider (get-in record [:failure :detail :refusals 0 :reason])))
    (is (= missing-provider (get-in record [:failure :detail :outer-task-selection :policy :reason])))
    (is (= :injury (:classification (terminal/classify record source-pin))))))

(deftest scoring-abstention-remains-a-non-injury
  (is (= :non-injury (:classification (terminal/classify
                                      (recorded :policy-nondiscrimination) source-pin)))))

(deftest refusal-retention-is-bounded-and-reports-truncation
  (let [detail (#'runner/failure-detail
                {:refusals (vec (repeat 40 {:kind missing-provider}))})]
    (is (= 33 (count (:refusals detail))))
    (is (= {:omitted :entry-limit} (last (:refusals detail))))))
