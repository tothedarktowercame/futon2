(ns wm-click-resource-observer-test
  (:require [clojure.test :refer [deftest is]]
            [checks.certify-live-run :as certify]
            [checks.wm-click-resource-observer :as observer]))

(def run
  {:run/id "run-1" :click/id "click-1"
   :startedAt "2026-09-01T00:00:10Z"})

(def clean-envelope
  {:schema "wm-click-resource-v1"
   :observation-scope "shared-serving-jvm"
   :click-id "click-1" :run-id "run-1"
   :started-at "2026-09-01T00:00:00Z"
   :finished-at "2026-09-01T00:00:20Z"
   :resource-status "clean"})

(deftest click-resource-envelope-falsifiers
  (is (true? (observer/envelope-valid? run clean-envelope)))
  (is (false? (observer/envelope-valid?
               run (assoc clean-envelope
                          :started-at "2026-09-01T00:00:11Z"))))
  (is (false? (certify/encloses-run?
               run (assoc clean-envelope :run-id "different-run"))))
  (is (= "unavailable"
         (observer/resource-status
          {:before 0 :after 0 :journal-readable? false
           :native-markers []})))
  (is (false? (observer/envelope-valid?
               run (assoc clean-envelope :resource-status "unavailable")))))
