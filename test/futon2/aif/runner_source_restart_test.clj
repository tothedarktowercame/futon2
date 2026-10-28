(ns futon2.aif.runner-source-restart-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.wm.debugger :as debugger]))

;; Actual pre-attempt drift shape from one-click-frontier-2026-10-22/accepted.json.
(def drift
  {:runner/source-check :drift
   :runner/loaded-sha256 "9174b3c2272dd7c1ba5e15b1a1ba7e4710ae97cae2c7301b0f6c76ea35f09224"
   :runner/canonical-sha256 "acf4080b443c8574aaba28601b83509acd56fbd264dacbd09b0485e36d6be063"})
(def current
  (assoc drift :runner/source-check :current
         :namespaces (zipmap '[futon2.aif.full-loop-runtime
                               futon2.report.war-machine
                               futon2.aif.wm.family-selection
                               futon2.aif.meta-pipeline-selector]
                             (repeat {:status :current}))))

(use-fixtures :each
  (fn [f]
    (debugger/detach!)
    (try (binding [runner/*wm-status-reporting?* false] (f))
         (finally (debugger/detach!)))))

(defn wait-stop [id]
  (let [deadline (+ (System/nanoTime) 5000000000)]
    (loop []
      (or (some #(when (= id (:run-id %)) %) (debugger/stopped))
          (if (< (System/nanoTime) deadline)
            (do (Thread/sleep 5) (recur))
            (throw (ex-info "No source stop" {:run-id id})))))))

(deftest reload-and-retry-preserves-prefix-and-enters-next-section-once
  (let [source (atom drift) reads (atom 0) prefix (atom 0) next-section (atom 0)
        events (atom [])]
    (debugger/attach!)
    (with-redefs [runner/runner-source-drift #(do (swap! reads inc) @source)]
      (let [worker (future
                     (swap! prefix inc)
                     (let [identity (runner/check-runner-source-phase!
                                     {:run-id "source-recovery"
                                      :phase-log-fn #(swap! events conj %)})]
                       (swap! next-section inc)
                       identity))]
        (try
          (let [stop (wait-stop "source-recovery")]
            (is (= :runner-source (:phase stop)))
            (is (= :stale-runner-source (get-in stop [:ex-data :failure-kind])))
            (is (= 0 @next-section))
            (reset! source current)
            (debugger/continue! "source-recovery" :retry)
            (is (= current (deref worker 5000 ::timeout)))
            (is (= 1 @prefix))
            (is (= 1 @next-section))
            (is (= 3 @reads)))
          (finally
            (when-not (realized? worker)
              (debugger/continue! "source-recovery" :abort)
              (deref worker 5000 nil))))))))

(deftest supplied-value-cannot-substitute-for-current-source
  (debugger/attach!)
  (with-redefs [runner/runner-source-drift (constantly drift)]
    (let [worker (future
                   (try
                     (runner/check-runner-source-phase!
                      {:run-id "source-no-substitute" :phase-log-fn (fn [_])})
                     :wrongly-entered
                     (catch Exception e (:failure-kind (ex-data e)))))]
      (try
        (wait-stop "source-no-substitute")
        (debugger/continue! "source-no-substitute" [:use-value current])
        (is (= :stale-runner-source (deref worker 5000 ::timeout)))
        (finally
          (when-not (realized? worker)
            (debugger/continue! "source-no-substitute" :abort)
            (deref worker 5000 nil)))))))
