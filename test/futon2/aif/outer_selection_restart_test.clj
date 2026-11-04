(ns futon2.aif.outer-selection-restart-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.effective-run-history :as effective-run-history]
            [futon2.aif.focus-receipt :as focus-receipt]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.outer-observation :as outer-observation]
            [futon2.aif.outer-select :as outer-select]
            [futon2.aif.outer-state :as outer-state]
            [futon2.aif.placement-adapter :as placement-adapter]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.wm.debugger :as debugger]))

(use-fixtures
  :each
  (fn [f]
    (debugger/detach!)
    (try (f) (finally (debugger/detach!)))))

(defn- wait-for-stop [run-id running]
  (loop [remaining 400]
    (if-let [stop (first (filter #(= run-id (:run-id %)) (debugger/stopped)))]
      stop
      (cond
        (realized? running) nil
        (pos? remaining) (do (Thread/sleep 5) (recur (dec remaining)))
        :else nil))))

(defn- opts [run-id]
  {:run-id run-id
   :selection-record/evidence (atom {})
   :selection-record/census (delay {})
   :selection-record/decided (atom nil)
   :selection-record/state (atom nil)})

(defn- with-outer-selection-fixture
  ([select-fn f]
   (with-outer-selection-fixture select-fn (fn [& _]) f))
  ([select-fn write-state-fn f]
   (with-redefs-fn
    {#'runtime/find-giraffe-run (constantly (java.io.File. "."))
     #'outer-state/load-makeup (constantly {})
     #'outer-state/read-state (constantly {})
     #'outer-state/write-state! write-state-fn
     #'focus-receipt/read-inputs (constantly {})
     #'outer-observation/outer-observation
     (constantly {:relation (fn [& _] :fixture-relation)})
     #'focus-receipt/relation-context (constantly {})
     #'placement-adapter/load-placements (constantly {})
     #'placement-adapter/annotate (fn [observation _] observation)
     #'runtime/resolve-pending-learning
     (fn [_ _ state _ _] {:status :none :state state})
     #'effective-run-history/index-rows (constantly nil)
     #'previous-run/selection-predecessor (constantly nil)
     #'outer-select/select select-fn
     #'runtime/retain-selection! (fn [_ section outcome]
                                  {:section section :outcome outcome})
     #'runtime/run-ordinary-opportunity-with-task!
     (constantly {:selection-result :success})}
    f)))

(deftest attached-refusal-retry-reruns-outer-selection
  (debugger/attach!)
  (let [run-id "outer-selection-retry"
        calls (atom 0)
        writes (atom 0)
        selection {:selection {:selected {:target "M-repaired"}}
                   :chosen-class :mission}
        running
        (future
          (with-outer-selection-fixture
            (fn [& _]
              (if (= 1 (swap! calls inc))
                (throw (ex-info "selection broke" {:kind :test/outer-selection}))
                selection))
            (fn [& _] (swap! writes inc))
            #(#'runtime/run-ordinary-opportunity-impl! (opts run-id))))
        stop (wait-for-stop run-id running)]
    (is (= :outer-selection (:phase stop)))
    (debugger/continue! run-id :retry)
    (let [result (deref running 2000 ::timeout)]
      (is (= 2 @calls))
      (is (= 1 @writes))
      (is (= {:selection-result :success} result))
      (is (not= :outer-selection-refused (:absent result))))))

(deftest attached-refusal-abort-preserves-the-ordinary-result
  (debugger/attach!)
  (let [run-id "outer-selection-abort"
        running
        (future
          (with-outer-selection-fixture
            (fn [& _]
              (throw (ex-info "selection broke"
                              {:kind :test/outer-selection :detail :bad-wire})))
            #(#'runtime/run-ordinary-opportunity-impl! (opts run-id))))
        stop (wait-for-stop run-id running)]
    (is (= :outer-selection (:phase stop)))
    (debugger/continue! run-id :abort)
    (is (= {:absent :outer-selection-refused
            :reason :test/outer-selection
            :details {:detail :bad-wire}}
           (deref running 2000 ::timeout)))))

(deftest detached-refusal-preserves-the-ordinary-result-without-a-stop
  (let [run-id "outer-selection-detached"
        result
        (with-outer-selection-fixture
          (fn [& _]
            (throw (ex-info "selection broke"
                            {:kind :test/outer-selection :detail :bad-wire})))
          #(#'runtime/run-ordinary-opportunity-impl! (opts run-id)))]
    (is (= {:absent :outer-selection-refused
            :reason :test/outer-selection
            :details {:detail :bad-wire}}
           result))
    (is (empty? (debugger/stopped)))))

(deftest attached-work-failure-after-decision-does-not-stop-as-selection
  (debugger/attach!)
  (let [run-id "outer-selection-work-failure"
        work-calls (atom 0)
        run-opts (opts run-id)
        selection {:selection {:selected {:target "M-selected"}}
                   :chosen-class :mission}
        running
        (future
          (with-outer-selection-fixture
            (constantly selection)
            (fn []
              (with-redefs-fn
                {#'runtime/run-ordinary-opportunity-with-task!
                 (fn [_]
                   (swap! work-calls inc)
                   (throw (ex-info "work broke"
                                   {:kind :test/work-failure
                                    :detail :outside-phase})))}
                #(#'runtime/run-ordinary-opportunity-impl! run-opts)))))
        erroneous-stop (wait-for-stop run-id running)]
    (is (empty? (debugger/stopped)))
    (when erroneous-stop
      (debugger/continue! run-id :abort))
    (is (= {:absent :outer-selection-refused
            :reason :test/work-failure
            :details {:detail :outside-phase}}
           (deref running 2000 ::timeout)))
    (is (= 1 @work-calls))))
