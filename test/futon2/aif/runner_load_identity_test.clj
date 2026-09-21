(ns futon2.aif.runner-load-identity-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.full-loop-runner :as runner]))

(deftest runner-must-compare-the-captured-digest
  (let [disk (identity/read-bytes (io/resource "futon2/aif/full_loop_runner.clj"))]
    (with-redefs [identity/registry (atom {'futon2.aif.full-loop-runner
                                         {:sha256 (identity/sha256 (.getBytes "older compiled source" "UTF-8"))}})]
      (let [r (runner/runner-source-drift (constantly disk))]
        (is (= :drift (:runner/source-check r)))
        (is (= :stale (get-in r [:namespaces 'futon2.aif.full-loop-runner :status])))
        (is (= :unregistered (get-in r [:namespaces 'futon2.aif.policy :status])))))))

(deftest other-staleness-is-evidence-not-a-new-refusal
  (let [canonical "/home/joe/code/futon2/src/futon2/aif/full_loop_runner.clj"
        digest (identity/sha256 (identity/read-bytes canonical))]
    (with-redefs [identity/registry (atom {'futon2.aif.full-loop-runner {:sha256 digest}
                                         'futon2.aif.policy {:sha256 "older-policy"}})]
      (let [r (#'runner/refuse-on-runner-source-drift!)]
        (is (= :current (:runner/source-check r)))
        (is (= :stale (get-in r [:namespaces 'futon2.aif.policy :status])))))))

(deftest every-participant-registers-its-resource
  (doseq [[n path] identity/required-sources]
    (require n)
    (let [relative (subs path (count "/home/joe/code/futon2/"))
          resource (subs relative (inc (.indexOf relative "/")))
          entry (get @identity/registry n)]
      (is (= :captured (:status entry)) (str n))
      (is (= (identity/sha256 (identity/read-bytes (io/resource resource))) (:sha256 entry)) (str n)))))
