(ns run-tick-once-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.run-tick-once :as tick]))

(deftest tick-record-preserves-producer-issued-run-id
  (let [record (#'tick/tick-run-record
                "run-issued-before-tick"
                "2026-08-31T00:00:00Z"
                {:count 1 :max-at "2026-08-30T00:00:00Z"}
                {:entries-read 1 :entries-limit 1}
                {:input-status {:inputs-read 1 :issues []}
                 :preference-stack (vec (repeat 5 {}))
                 :wm/route []}
                "stub:test"
                true)]
    (is (= "run-issued-before-tick" (:run/id record)))
    (is (= "2026-08-31T00:00:00Z" (:startedAt record)))
    (is (true? (:traceWritten record)))))
