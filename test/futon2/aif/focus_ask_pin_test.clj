(ns futon2.aif.focus-ask-pin-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]))

(deftest ask-classifier-uses-selection-pinned-manifest
  (let [corpus {:rows {"x" {:target "x" :area "WM"}} :sha256 "corpus"}
        older {:sha256 "old" :manifest {:result {:status :present :ranked [{:area "WM"}]}}}
        newer {:sha256 "new" :manifest {:result {:status :present :ranked [{:area "APM"}]}}}
        classify (runner/interpretation-ask-classifier {:focus-pin older :area-corpus corpus})]
    ;; A newer available manifest is deliberately irrelevant once selection pinned OLD.
    (is (= :focus (classify "x")))
    (is (= :useful-elsewhere ((runner/interpretation-ask-classifier
                               {:focus-pin newer :area-corpus corpus}) "x")))))
