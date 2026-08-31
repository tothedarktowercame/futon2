(ns wm-operational-certificate-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [checks.wm-operational-certificate :as cert]))

(def run-path "holes/labs/wm-contract/tick-run-record-2026-08-30.edn")
(def clean-resource (edn/read-string
                     (slurp "test/fixtures/wm-operational-certificate/resource-clean.edn")))
(def run-bytes (java.nio.file.Files/readAllBytes
                (.toPath (io/file run-path))))
(def run-record (edn/read-string (String. run-bytes "UTF-8")))
(defn edn-bytes [x] (.getBytes (pr-str x) "UTF-8"))

(deftest mapped-partial-route-certifies-and-exposes-coverage
  (let [c (cert/certificate run-bytes clean-resource false)]
    (is (= :pass (:verdict c)))
    (is (= {:total 9 :original 3 :measured 6 :undeclared 0}
           (get-in c [:traversal :counts])))
    (is (seq (get-in c [:traversal :declared-not-exercised :original])))
    (is (= :content-sha256-fallback (get-in c [:run :identity-kind])))
    (is (cert/certificate-matches-run? run-bytes c))
    (is (true? (get-in c [:checks :resource-status-clean?])))))

(deftest explicit-run-id-is-stable-and-mismatch-is-rejected
  (let [identified-bytes (edn-bytes (assoc run-record :run/id "run-2026-08-31-test"))
        c (cert/certificate identified-bytes clean-resource false)]
    (is (= {:id "run-2026-08-31-test" :identity-kind :recorded-run-id}
           (select-keys (:run c) [:id :identity-kind])))
    (is (cert/certificate-matches-run? identified-bytes c))
    (is (false? (cert/certificate-matches-run?
                 identified-bytes
                 (assoc-in c [:run :id] "different-run"))))))

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
