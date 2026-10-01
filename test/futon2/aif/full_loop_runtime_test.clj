(ns futon2.aif.full-loop-runtime-test
  (:require [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-feedback :as cascade-feedback]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.report.war-machine :as wm]))

(deftest flight-runner-does-not-load-the-report
  (let [{:keys [exit out err]}
        (shell/sh "clojure" "-M:test" "-e"
                  (str "(require 'futon2.aif.flight-runner) "
                       "(prn (boolean (find-ns 'futon2.report.war-machine)))"))]
    (is (zero? exit) err)
    (is (= false (edn/read-string out)))))

(deftest runtime-supplies-the-report-backed-judge
  (let [seen (atom nil)
        opts {:run-id "runtime-test" :flight {:target "T"}
              :cascade-feedback-path "/tmp/runtime-test-feedback.edn"}
        expected {:judgement :selected}]
    (with-redefs [wm/accumulation-config (constantly {:accumulation :configured})
                  wm/generate-war-machine
                  (fn [days judge-opts]
                    (reset! seen [days judge-opts])
                    expected)]
      (is (= expected ((:judge-fn (runtime/production-defaults opts)) 17)))
      (is (= 17 (first @seen)))
      (is (= {:accumulation :configured
              :run-id "runtime-test"
              :flight {:target "T"}
              :cascade-feedback-path "/tmp/runtime-test-feedback.edn"
              :trace? false
              :include-advisory-lanes? false
              :defer-render? true}
             (second @seen))))))

(deftest runner-refuses-an-absent-runtime-default
  (binding [runner/*runtime-defaults* nil]
    (let [failure (try
                    (runner/runtime-default {} :judge-fn)
                    nil
                    (catch clojure.lang.ExceptionInfo e e))]
      (is (some? failure))
      (is (= {:failure-kind :missing-runtime-default
              :missing-default :judge-fn
              :supplied-by 'futon2.aif.full-loop-runtime}
             (ex-data failure))))))

(deftest production-composition-supplies-the-feedback-store
  (let [seen (atom nil)]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! seen opts)
                    :ran)]
      (is (= :ran (runtime/run-opportunity! {:run-id "feedback-default"})))
      (is (= cascade-feedback/default-path
             (:cascade-feedback-path @seen))))))
