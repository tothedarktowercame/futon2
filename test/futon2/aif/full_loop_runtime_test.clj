(ns futon2.aif.full-loop-runtime-test
  (:require [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-feedback :as cascade-feedback]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.meta-live-outer-selector :as meta-live]
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
      (let [defaults (runtime/production-defaults opts)]
        (is (fn? (:cascade-revision-proposals-fn defaults)))
        (is (= expected ((:judge-fn defaults) 17))))
      (is (= 17 (first @seen)))
      (is (= {:accumulation :configured
              :run-id "runtime-test"
              :flight {:target "T"}
              :cascade-feedback-path "/tmp/runtime-test-feedback.edn"
              :outer-task-policy :meta
              :outer-task-selection-fn meta-live/selector
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
             (:cascade-feedback-path @seen)))
      (is (map? (:cascade-feedback-metadata @seen))))))

(deftest production-mints-run-identity-before-closing-over-the-judge
  (let [runner-opts (atom nil)
        judge-opts (atom nil)]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! runner-opts opts)
                    ((:judge-fn runner/*runtime-defaults*) 1)
                    :ran)
                  wm/generate-war-machine
                  (fn [_ opts]
                    (reset! judge-opts opts)
                    {:judgement {}})]
      (is (= :ran (runtime/run-opportunity! {})))
      (is (string? (:run-id @runner-opts)))
      (is (= (:run-id @runner-opts) (:run-id @judge-opts))
          "accumulation and the durable run record must receive one identity"))))
