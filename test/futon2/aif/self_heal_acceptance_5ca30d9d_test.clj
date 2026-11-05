(ns futon2.aif.self-heal-acceptance-5ca30d9d-test
  (:require [clojure.test :refer [deftest]]
            [futon2.aif.cause-scope :as cause-scope]))

(deftest injury-5ca30d9d7509ec6d70181a5041df37c972fe2e5f-acceptance
  (futon2.aif.cause-scope/assert-repair
    {:cause-entry "futon2.aif.wm.terminal-receipt/terminal-receipt",
     :input-pin {:path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-05-d78842b4-2dc8-4aca-bab7-c90d982f467f.edn",
                 :sha256 "781a049aff13d55610ef4e9996361fddaba98d74bb10a94013c1db26032d230e"},
     :input-path [],
     :input-digest "4a24d6fbf7978be100110b960344f378718b941c52ab578bdfd1443112949c75",
     :healthy-predicate {:op :result-path-equals,
                         :path [:failure-kind],
                         :value :invalid-checkpoint-cell}}))
