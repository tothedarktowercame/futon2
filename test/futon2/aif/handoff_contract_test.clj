(ns futon2.aif.handoff-contract-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.handoff-contract :as sut]))

(def example
  {:id :wm/example :version 1
   :response {:required [:answer] :enums {:answer [:yes :no]}}
   :validator {:id :wm/example-validator :version 1}})

(deftest one-value-renders-and-identifies-the-consumer-contract
  (let [contract (sut/contract example)
        prompt (sut/prompt-block contract)]
    (is (= sut/schema (:schema contract)))
    (is (.contains prompt ":required [:answer]"))
    (is (.contains prompt ":answer [:yes :no]"))
    (is (= :validated (:state (sut/outcome contract "request-1" :validated {}))))))

(deftest malformed-contracts-and-collapsed-states-refuse
  (is (thrown? clojure.lang.ExceptionInfo
               (sut/contract (dissoc example :validator))))
  (is (thrown? clojure.lang.ExceptionInfo
               (sut/outcome example "request-1" :done {}))))

(deftest agency-terminal-result-and-legacy-text-normalize-identically
  (let [payload {:answer "yes"}
        encoded "{\"answer\":\"yes\"}"]
    (is (= payload (sut/response-map payload)))
    (is (= payload (sut/response-map {:analysis payload})))
    (is (= payload (sut/response-map {:state "done" :result encoded})))
    (is (= payload (sut/response-map {:state "done" :text encoded})))
    (is (nil? (sut/response-map {:state "done" :result "not json"})))))

(deftest one-strict-json-representation-rule-covers-all-handoffs
  (let [payload {:answer "yes"}
        fenced "```json\n{\"answer\":\"yes\"}\n```"]
    (is (= payload (sut/response-json-object payload)))
    (is (= payload (sut/response-json-object "{\"answer\":\"yes\"}")))
    (is (= payload (sut/response-json-object fenced)))
    (is (= payload (sut/response-map {:state "done" :result fenced})))
    ;; occ-c6325db5: prose before or after the one fence is accepted; the
    ;; fenced object is still unique.
    (is (= payload (sut/response-json-object (str "prose\n" fenced))))
    (is (= payload (sut/response-json-object
                    (str fenced "\nValidated; not published."))))
    (is (nil? (sut/response-json-object (str fenced "\n" fenced))))
    (is (nil? (sut/response-json-object "prose only, no fence")))
    (is (nil? (sut/response-json-object "[1,2]")))
    (is (nil? (sut/response-json-object nil)))))

(deftest one-payload-rule-covers-current-legacy-event-and-direct-replies
  (is (= "full" (sut/response-payload
                   {:result "full" :result-summary "prefix" :text "legacy"})))
  (is (= "legacy" (sut/response-payload {:text "legacy"})))
  (is (= "one\ntwo" (sut/response-payload
                       {:events [{:type "text" :text "one"}
                                 {:type "tool_use" :text "ignored"}
                                 {:type "text" :text "two"}]})))
  (is (= {:direct true} (sut/response-payload {:direct true}))))

(deftest one-exchange-owns-contact-envelope-decoding-and-validation
  (let [decode #(json/parse-string % true)
        validate #(assoc % :checked true)
        run (fn [answer]
              (sut/exchange {:contact (fn [_] answer) :request {:id 1}
                             :decode decode :validate validate}))]
    (doseq [answer [{:state "done" :job-id "j-result"
                     :result "{\"answer\":\"yes\"}"}
                    {:state "done" :job-id "j-text"
                     :text "{\"answer\":\"yes\"}"}]]
      (let [got (run answer)]
        (is (= :validated (:status got)))
        (is (= {:answer "yes" :checked true} (:value got)))))
    (is (= :not-contacted
           (:status (sut/exchange {:contact nil :request {}}))))
    (is (= :designated-author-unavailable
           (:reason (run {:state :not-contacted
                          :reason :designated-author-unavailable}))))
    (is (= :job-not-done (:reason (run {:state "failed" :job-id "j"}))))
    (is (= :response-decode-threw
           (:reason (run {:state "done" :result "not-json"}))))
    (is (= :contact-threw
           (:reason (sut/exchange {:contact #(throw (ex-info "boom" {}))
                                   :request {}}))))
    (is (= :response-validation-threw
           (:reason (sut/exchange {:contact (constantly {:ok true}) :request {}
                                   :validate #(throw (ex-info "bad" {}))}))))))

(deftest live-handoffs-use-the-shared-boundary
  (let [source #(slurp (io/resource %))
        target (source "futon2/aif/selected_target_reading.clj")
        runner (source "futon2/aif/full_loop_runner.clj")
        flight (source "futon2/aif/flight_runner.clj")]
    (is (str/includes? target "handoff/exchange"))
    (is (str/includes? runner "handoff/exchange"))
    (is (str/includes? flight "handoff/response-payload"))
    (is (not (str/includes? target "handoff/response-map")))
    (is (not (str/includes? runner "handoff/response-map")))
    (is (not (re-find #"wi/parse-reply(?: schema)? \(:text answer\)" flight)))))
