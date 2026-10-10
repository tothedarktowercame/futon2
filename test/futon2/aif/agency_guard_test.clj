(ns futon2.aif.agency-guard-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.agency-guard :as guard]
            [futon2.data-paths :as data-paths]))

(deftest a-test-jvm-may-not-act-on-the-production-agency
  ;; D23: the offline replay dispatched a real interpretation-ask job.
  (is (true? data-paths/test-mode?))
  (doseq [base ["http://127.0.0.1:7070" "http://localhost:7070" "http://localhost:7070/"]]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"production Agency"
                          (guard/dispatch-url base "/api/alpha/bell"))))
  (is (= "http://127.0.0.1:17070/api/alpha/bell"
         (guard/dispatch-url "http://127.0.0.1:17070" "/api/alpha/bell"))
      "an explicitly supplied test Agency is allowed"))
