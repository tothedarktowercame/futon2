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
        root (.toFile (java.nio.file.Files/createTempDirectory
                       "runtime-predecessor-" (make-array java.nio.file.attribute.FileAttribute 0)))
        opts {:run-id "runtime-test" :flight {:target "T"}
              :run-record-dir (.getPath root)
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
              :token-belief-predecessor-record nil
              :trace? false
              :include-advisory-lanes? false
              :defer-render? true}
             (second @seen))))))

(deftest latest-completed-run-is-the-token-belief-predecessor
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "runtime-predecessor-" (make-array java.nio.file.attribute.FileAttribute 0)))
        older (java.io.File. root "tick-run-record-old.edn")
        newer (java.io.File. root "tick-run-record-new.edn")]
    (spit older (pr-str {:run/id "old" :startedAt "2026-10-09T00:00:00Z"
                         :enactment {:status :admitted}}))
    (spit newer (pr-str {:run/id "new" :startedAt "2026-10-09T00:00:01Z"
                         :realized-outcome {:status :compared}}))
    (.setLastModified older 1000)
    (.setLastModified newer 2000)
    (is (= "new" (:run/id (runtime/predecessor-run-record
                            {:run-record-dir (.getPath root)}))))
    (spit newer "{")
    (is (= "old" (:run/id (runtime/predecessor-run-record
                            {:run-record-dir (.getPath root)}))))))

(deftest completed-run-projects-real-producers-for-token-carry
  (let [selection-enaction {:verdict :match :evidence {:source :runner-selection}}
        enactment {:verification {:status :admitted}}
        outcome {:schema :wm/token-outcome-comparison-v1 :status :compared}
        result {:d-task-context {:occurrence "run-occurrence"}
                :d-task-enactment enactment
                :checkpoints
                {:construction
                 {:judgment {:selection-enaction selection-enaction
                             :selected-action {:id :selected}
                             :cascade {:id :cascade}
                             :patterns [:p]
                             :receipted-construction
                             {:cascade-diff {:acting-order-after [:p]}}}}
                 :closed {:judgment {:token-outcome-comparison outcome}}}}
        projected (runner/completed-predecessor-evidence result)]
    (is (= {:occurrence "run-occurrence"} (:d-task-context projected)))
    (is (= selection-enaction (:selection-enaction projected)))
    (is (= enactment (:enactment projected)))
    (is (= outcome (:realized-outcome projected)))
    (is (= [:p] (:acting-order-after projected)))
    (is (= {:schema :wm/enactment-plan-v1
            :selected-action {:id :selected}
            :cascade {:id :cascade}
            :patterns [:p]}
           (:enactment-plan projected)))
    (let [missing (runner/completed-predecessor-evidence
                   {:checkpoints {:construction {:judgment {}}}})]
      (is (nil? (:selection-enaction missing)))
      (is (nil? (:enactment missing)))
      (is (nil? (:realized-outcome missing))))))

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
      (is (= runtime/default-pattern-graph-path (:pattern-graph-path @seen)))
      (is (= runtime/default-pattern-graph-diff-dir (:pattern-graph-diff-dir @seen)))
      (is (map? (:cascade-feedback-metadata @seen))))))

(deftest production-composition-shares-one-selection-timing-collector
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
      (is (= :ran (runtime/run-opportunity! {:run-id "timing-state"})))
      (is (instance? clojure.lang.IAtom
                     (:selection-timing/state @runner-opts)))
      (is (identical? (:selection-timing/state @runner-opts)
                      (:selection-timing/state @judge-opts))))))

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
