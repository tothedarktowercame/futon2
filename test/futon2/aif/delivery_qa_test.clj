(ns futon2.aif.delivery-qa-test
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.delivery-qa :as qa]))

(def delivered-item
  {:attempt-id "attempt-qa-1"
   :outcome :grounded-change
   :selected-target "M-shared-memory-control-build-test"
   :commit "abc123"
   :witness {:implementation-id "impl-1"
             :discharge-id "discharge-1"}
   :achievement {:summary "Independently reviewed and grounded change"}
   :feature-card {:built "Wired automatic bounded autonomy."
                  :want-coverage "Delivery QA is visible to Joe."}})

(deftest qa-note-is-concrete-and-evidence-bearing
  (let [note (qa/qa-note delivered-item)
        body (read-string (:body note))]
    (is (= "attempt-qa-1" (:attempt-id note)))
    (is (= "note" (:kind note)))
    (is (= ["abc123"] (:delivery/commit-shas body)))
    (is (= #{qa/decision-evidence-id "impl-1" "discharge-1"}
           (set (:delivery/evidence-ids body))))
    (is (= "Wired automatic bounded autonomy."
           (:delivery/built-or-changed body)))))

(deftest emitter-writes-only-through-field-desk-on-7070
  (let [seen (atom nil)]
    (with-redefs
      [http/post
       (fn [url opts]
         (reset! seen {:url url
                       :payload (json/parse-string (:body opts) true)})
         {:status 200
          :body (json/generate-string
                 {:ok true
                  :addendum {:morning-brief/addendum-id "mba-1"}})})]
      (is (= "mba-1"
             (:morning-brief/addendum-id
              (qa/emit! {:agency-base "http://127.0.0.1:7070"}
                        delivered-item))))
      (is (= "http://127.0.0.1:7070/api/alpha/morning-brief/addendum"
             (:url @seen)))))
  (testing "a substrate or alternate API port cannot receive delivery QA"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"port-7070"
         (qa/emit! {:agency-base "http://127.0.0.1:7073"}
                   delivered-item)))))

(deftest rejected-field-desk-write-is-a-gate-failure
  (with-redefs
    [http/post
     (fn [& _]
       {:status 400
        :body (json/generate-string {:ok false :err "invalid"})})]
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"delivery QA gate failed"
         (qa/emit! {:agency-base "http://127.0.0.1:7070"}
                   delivered-item)))))
