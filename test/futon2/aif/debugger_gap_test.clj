(ns futon2.aif.debugger-gap-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.tripwire :as tripwire]
            [futon2.aif.wm.debugger :as debugger]))

(use-fixtures
  :each
  (fn [test-fn]
    (debugger/detach!)
    (try
      (binding [runner/*wm-status-reporting?* false]
        (with-redefs [tripwire/observe! (fn [& _])]
          (test-fn)))
      (finally
        (debugger/detach!)))))

(defn- wait-for-stop [run-id running]
  (loop [remaining 4000]
    (if-let [stop (first (filter #(= run-id (:run-id %))
                                 (debugger/stopped)))]
      stop
      (if (pos? remaining)
        (do (Thread/sleep 5) (recur (dec remaining)))
        (throw (ex-info "Debugger stop did not appear"
                        {:run-id run-id
                         :run-realized? (realized? running)}))))))

(defn- authoring-result [published?]
  {:record {:kind (when-not published? :occurrence-square-answer-invalid)
            :published published?}})

(deftest unpublished-square-authoring-stops-and-retry-reruns-phase
  (debugger/attach!)
  (let [run-id "debugger-gap-square-authoring"
        calls (atom 0)
        published? (atom false)
        running
        (future
          (runner/run-phase!
           {:run-id run-id}
           {:opportunity-id "op-square"}
           :occurrence-square-authoring
           #(do (swap! calls inc) (authoring-result @published?))
           nil
           runner/occurrence-square-authoring-terminal-condition))
        stop (wait-for-stop run-id running)]
    (is (= run-id (:run-id stop)))
    (is (= :occurrence-square-authoring (:phase stop)))
    (is (= :occurrence-square-answer-invalid
           (get-in stop [:condition :kind])))
    (reset! published? true)
    (debugger/continue! run-id :retry)
    (is (= true (get-in (deref running 5000 ::timeout) [:record :published])))
    (is (= 2 @calls))))

(deftest detached-square-authoring-keeps-existing-terminal-result
  (let [result (authoring-result false)
        returned
        (runner/run-phase!
         {:run-id "detached-square-authoring"}
         {:opportunity-id "op-square"}
         :occurrence-square-authoring
         (constantly result)
         nil
         runner/occurrence-square-authoring-terminal-condition)]
    (is (= result returned))
    (is (empty? (debugger/stopped)))
    ;; This is the existing outer-boundary classification, which remains in
    ;; charge when no debugger is attached.
    (is (= :occurrence-square-answer-invalid
           (:failure-kind
            (ex-data
             (try
               (throw (ex-info "Occurrence-square authoring failed before G"
                               {:failure-kind :occurrence-square-answer-invalid}))
               (catch clojure.lang.ExceptionInfo e e))))))))
