(ns futon2.aif.injury-76d912d3-acceptance-test
  (:require [clojure.test :refer [deftest]]
            [futon2.aif.cause-scope]))

(deftest injury-76d912d3-repair
  (futon2.aif.cause-scope/assert-repair
   {:cause-entry "futon2.aif.wm.terminal-receipt/terminal-receipt",
    :input-pin {:path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-05-7bc9acb9-e46c-41ba-9a09-1516fec6c0ac.edn",
                :sha256 "0792e160b1d9f203fd39a6babd7f5edfe753b7d63681c3d533328e52423f344d"},
    :input-path [],
    :input-digest "d602dd335a27dc6aa1bd32b1ff5d2c5e09866d27c959631851295f00280bc0e2",
    :healthy-predicate {:op :result-path-equals, :path [:kind], :value :action-receipt}}))
