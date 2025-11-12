(ns ants.aif.core-test
  (:require [clojure.test :refer [deftest is]]
            [ants.aif.core :as core]))

(deftest dhdt-computes-trend
  (let [trend (core/dhdt {:recent [{:obs {:hunger 0.4}}
                                   {:obs {:hunger 0.6}}]})]
    (is (< (Math/abs (- 0.2 trend)) 1e-9))))
