(ns futon2.test-support.watchdog-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.test-support.watchdog :as watchdog]))

(deftest a-timed-out-deftest-reports-and-returns
  (let [reports (atom [])
        hung (with-meta
               (fn [])
               {:ns (the-ns 'futon2.test-support.watchdog-test)
                :name 'fixture-hang
                :test (fn [] (loop [] (recur)))})]
    (binding [watchdog/*test-timeout-ms* 25]
      (with-redefs [clojure.test/report #(swap! reports conj %)]
        (watchdog/timed-test-var clojure.test/test-var hung)))
    (let [timeout-report (first (filter #(= :fail (:type %)) @reports))]
      (is timeout-report)
      (is (str/includes? (:message timeout-report) "TIMEOUT:"))
      (is (str/includes? (:message timeout-report) "THREAD DUMP")))))
