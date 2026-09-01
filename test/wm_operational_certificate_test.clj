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

(deftest selector-seam-is-recorded-but-does-not-govern-certification
  (let [production-seam "agency-http:verified-live-selection"
        production-bytes (edn-bytes (assoc run-record :run/id "production-shape-control"
                                           :selectorSeam production-seam))
        missing-bytes (edn-bytes (-> run-record
                                     (assoc :run/id "missing-seam-control")
                                     (dissoc :selectorSeam)))
        production (cert/certificate production-bytes clean-resource false)
        missing (cert/certificate missing-bytes clean-resource false)]
    (is (= :pass (:verdict production)))
    (is (= {:status :present :value production-seam}
           (get-in production [:run :selector-seam])))
    (is (= (get-in (cert/certificate run-bytes clean-resource false)
                   [:traversal :counts])
           (get-in production [:traversal :counts])))
    (is (= :pass (:verdict missing)))
    (is (= {:status :absent :reason :not-recorded}
           (get-in missing [:run :selector-seam])))))

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

(deftest incomplete-click-is-not-certified-as-a-complete-run
  (let [partial-run (assoc run-record
                           :run/id "partial-failed-run"
                           :click/id "partial-click"
                           :traceWritten false
                           :route [{:fromNode "R1" :toNode "R4"
                                    :via "partial-before-failure"
                                    :at_ "2026-09-01T00:00:01Z"}])
        partial-resource {:schema 2
                          :run/id "partial-failed-run"
                          :source-schema :wm-click-resource-v1
                          :observation-scope :shared-serving-jvm
                          :status :clean
                          :execution-outcome :incomplete
                          :pids-events-max-delta 0
                          :native-thread-exhaustion false
                          :tasks-peak 1}
        c (cert/certificate (edn-bytes partial-run) partial-resource false)]
    (is (= :incomplete (:verdict c)))
    (is (= :clean (get-in c [:resource-status :status])))
    (is (= :incomplete (get-in c [:execution-status :status])))
    (is (true? (get-in c [:checks :resource-status-clean?])))
    (is (false? (get-in c [:checks :execution-complete?])))))
