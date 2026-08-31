(ns wm-operational-certificate-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [checks.wm-operational-certificate :as cert]))

(def run-path "holes/labs/wm-contract/tick-run-record-2026-08-30.edn")
(def clean-resource (edn/read-string
                     (slurp "test/fixtures/wm-operational-certificate/resource-clean.edn")))
(def run-bytes (java.nio.file.Files/readAllBytes
                (.toPath (clojure.java.io/file run-path))))

(deftest mapped-partial-route-certifies-and-exposes-coverage
  (let [c (cert/certificate run-bytes clean-resource false)]
    (is (= :pass (:verdict c)))
    (is (= {:total 9 :original 3 :measured 6 :undeclared 0}
           (get-in c [:traversal :counts])))
    (is (seq (get-in c [:traversal :declared-not-exercised :original])))
    (is (true? (get-in c [:checks :resource-status-clean?])))))

(deftest undeclared-hop-produces-a-failing-certificate
  (let [c (cert/certificate run-bytes clean-resource true)]
    (is (= :fail (:verdict c)))
    (is (= 1 (get-in c [:traversal :counts :undeclared])))
    (is (false? (get-in c [:checks :no-undeclared-traversal?])))))

(deftest dirty-or-missing-resource-status-cannot-certify
  (is (= :fail (:verdict (cert/certificate run-bytes nil false))))
  (is (= :fail (:verdict
                (cert/certificate run-bytes
                                  (assoc clean-resource :native-thread-exhaustion true)
                                  false)))))
