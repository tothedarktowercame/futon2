(ns ants.clj-ants-aif-test
  (:require [clojure.test :refer [deftest is]]
            [ants.clj-ants-aif :as entry]))

(deftest run!-returns-nil
  (is (nil? (entry/run! 0))))
