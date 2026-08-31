(ns preemptive-repair-lint-test
  (:require [clojure.test :refer [deftest is run-tests]]
            [checks.preemptive-repair-lint :as lint]))

(deftest every-negative-control-is-rejected
  (doseq [kind [:acceptance :artefact :stale-baseline :absence :era :record]]
    (is (seq (:findings (lint/run kind true))) (str kind " mutation must be found"))))

(deftest scoped-exemptions-do-not-become-findings
  (is (empty? (lint/stale-baseline-findings
               [{:repo :fixture :path "test/x.clj" :text "(is (= 3 (count fixture-trace)))"}])))
  (is (empty? (lint/era-findings
               [{:repo :fixture :path "test/x.clj"
                 :text "(is (= 3 (count timestamp-records))) ; era :v2"}]))))

(let [{:keys [fail error]} (run-tests)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
