(ns futon2.aif.terminal-publication-receipt-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.terminal-publication-receipt :as receipt])
  (:import [java.nio.file Files]))

;; Live shape pin: the selected-but-lost terminal record is
;; tick-run-record-2026-10-19-f9abc990-b43a-491b-8f2b-d8b14c7fa796.edn.
;; These tests use isolated bytes, but retain that run-id shape rather than
;; claiming a new runtime record was produced.
(defn- temp-dir []
  (.toFile (Files/createTempDirectory "wm-terminal-publication-"
                                      (make-array java.nio.file.attribute.FileAttribute 0))))

(deftest failed-injury-sink-does-not-suppress-learning-or-change-run-bytes
  (let [dir (temp-dir)
        record (io/file dir "tick-run-record-2026-10-19-f9abc990-b43a-491b-8f2b-d8b14c7fa796.edn")
        original "{:run/id \"2026-10-19-f9abc990-b43a-491b-8f2b-d8b14c7fa796\" :outcome :invalid-close-outcome}\n"
        _ (spit record original)
        before (identity/sha256 (.getBytes original "UTF-8"))
        result (receipt/publish!
                (.getPath record) "2026-10-19-f9abc990-b43a-491b-8f2b-d8b14c7fa796"
                [{:sink :injury :thunk #(throw (ex-info "injury unavailable" {:kind :injury-sink-failed}))}
                 {:sink :learning :thunk #(do :recorded)}
                 {:sink :optional :reason :not-configured}])
        saved (edn/read-string (slurp (:path result)))]
    (is (= :present (:status result)))
    (is (= before (identity/sha256 (Files/readAllBytes (.toPath record)))))
    (is (= before (get-in saved [:run-record :sha256])))
    (is (= [:failed :returned :not-attempted] (mapv :status (:sinks saved))))
    (is (= :injury-sink-failed (get-in saved [:sinks 0 :reason])))
    (is (= :recorded (get-in saved [:sinks 1 :result])))
    (is (re-matches #"tick-run-record-.*\.edn" (.getName record)))
    (is (not (re-matches #"tick-run-record-.*\.edn" (.getName (io/file (:path result))))))))

(deftest receipt-persistence-failure-is-honest-and-keeps-run-bytes
  (let [dir (temp-dir)
        record (io/file dir "tick-run-record-r1.edn")
        original "{:run/id \"r1\" :outcome :refused}\n"
        _ (spit record original)
        before (identity/sha256 (.getBytes original "UTF-8"))]
    (with-redefs [receipt/write-receipt!
                  (fn [& _] (throw (ex-info "receipt disk unavailable" {})))]
      (let [result (receipt/publish! (.getPath record) "r1"
                                     [{:sink :learning :thunk #(do :recorded)}])]
        (is (= :failed (:status result)))
        (is (= :terminal-publication-receipt-persistence-failed (:reason result)))
        (is (nil? (:path result)))
        (is (= :returned (get-in result [:receipt :sinks 0 :status])))
        (is (= before (identity/sha256 (Files/readAllBytes (.toPath record)))))))))

(deftest returned-refusal-nil-and-unavailable-linkage-remain-honest
  (let [dir (temp-dir)
        record (io/file dir "tick-run-record-r2.edn")
        _ (spit record "{:run/id \"r2\" :outcome :refused}\n")
        learning-called (atom 0)
        returned (receipt/publish!
                  (.getPath record) "r2"
                  [{:sink :injury :thunk #(do {:status :refused :reason :fixture-refusal})}
                   {:sink :nil-sink :thunk #(do nil)}
                   {:sink :learning :thunk #(swap! learning-called inc)}])]
    (is (= :returned (get-in returned [:receipt :sinks 0 :status])))
    (is (= :refused (get-in returned [:receipt :sinks 0 :sink-status])))
    (is (= :fixture-refusal (get-in returned [:receipt :sinks 0 :result :reason])))
    (is (= :returned-nil (get-in returned [:receipt :sinks 1 :status])))
    (is (= 1 @learning-called))
    (is (= {:status :refused :reason :fixture-refusal}
           (receipt/sink-result returned :injury)))
    (with-redefs [receipt/read-run-pin
                  (fn [_] (throw (java.io.IOException. "unreadable")))]
      (let [unavailable (receipt/publish! "/missing/run.edn" "r-missing"
                                          [{:sink :injury :thunk #(throw (Exception. "must not run"))}
                                           {:sink :learning :thunk #(throw (Exception. "must not run"))}])]
        (is (= :unavailable (:status unavailable)))
        (is (nil? (:path unavailable)))
        (is (= [:not-attempted :not-attempted] (mapv :status (:sinks unavailable))))
        (is (= [:run-record-linkage-unavailable :run-record-linkage-unavailable]
               (mapv :reason (:sinks unavailable))))))))
