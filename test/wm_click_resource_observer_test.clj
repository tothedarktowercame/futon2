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
   :resource-status "clean"
   :serving-runner-code {:availability "available"
                         :identity {:git-head "tested"}}})

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

(deftest serving-code-identity-survives-resource-normalization
  (is (= (:serving-runner-code clean-envelope)
         (:serving-runner-code
          (observer/certificate-resource "run-1" "fixture" clean-envelope)))))

(deftest tested-job-from-another-attempt-is-rejected
  (let [receipt {:command "clojure -T:build ci" :outer-exit 0 :verdict "pass"
                 :repository-basis-stable true
                 :repository-basis-start {:dirty false}
                 :repository-basis-finish {:dirty false :head "same-head"}}
        foreign {:id "job-foreign" :agent-id "other-fence"
                 :systemd {:ActiveState "inactive"} :receipt receipt}]
    (try
      (certify/tested-commit-from-record foreign "job-foreign" "this-fence")
      (is false "foreign attempt unexpectedly accepted")
      (catch clojure.lang.ExceptionInfo failure
        (is (= :tested-job-attempt-mismatch (:reason (ex-data failure))))))))
