(ns futon2.aif.injury-f6be547e-acceptance-test
  (:require [clojure.test :refer [deftest]]
            [futon2.aif.cause-scope]))

(deftest injury-f6be547e-cause-scope-repair
  (futon2.aif.cause-scope/assert-repair
    {:cause-entry "futon2.aif.wm.terminal-receipt/terminal-receipt",
     :input-pin
     {:path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-04-25c81574-a40b-4bce-a5c9-d1cd4c264407.edn",
      :sha256 "0a648f67a2475abe52c9731f6864225f19a04726fd4fdc545a61456cc07a4a51"},
     :input-path [],
     :input-digest "728b9dbcfe8540deb38b07ca48f96fa88772e9347528be638cb5bf4f01604be0",
     :healthy-predicate {:op :result-path-equals, :path [:kind], :value :action-receipt}}))
