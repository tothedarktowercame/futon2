(ns futon2.aif.outer-state-wiring-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.focus-receipt :as focus-receipt]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.outer-observation :as outer-observation]
            [futon2.aif.outer-state :as outer-state]
            [futon2.aif.run-ending-classification :as run-ending]))

(def makeup
  {:schema :wm/outer-makeup-v1
   :makeup/sha256 (apply str (repeat 64 "b"))
   :default-reach 1/2
   :cascades [{:id :ready :patterns ["test"] :steps [:ready-gate]}]
   :c-kit {:focused 55/100 :related 35/100 :unrelated 5/100
           :stop-the-line 5/100 :not-yet-evaluated 0}
   :kappa 1
   :state-kit {:ready {:doable 1 :blocked 1}
               :not-ready {:doable 1 :blocked 3}
               :undetermined {:doable 1 :blocked 1}}
   :gain-kit {:none 1 :one 2 :several 3}
   :eta-c 1 :omega-c 9/10
   :eta-state 1 :omega-state 9/10
   :eta-gain 1 :omega-gain 9/10})

(def candidate {:target "M-selected" :reach 1/2})

(defn observation [run-id field facts]
  {:giraffe/run-id run-id
   :giraffe/manifest-sha256 (apply str (repeat 64 "c"))
   :focus "M-focus"
   :relation (fn [_ _] :associated)
   :field field
   :facts facts})

(def old-observation
  (observation "old-run" [candidate]
               {"M-selected" {:status :ready :unblocks 1 :cone 1}}))

(def current-observation
  (observation "current-run" [candidate]
               {"M-selected" {:status :ready :unblocks 1 :cone 1}}))

(defn temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "outer-state-wiring-test"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defn approx= [expected actual]
  (< (Math/abs (- (double expected) (double actual))) 1.0e-12))

(defn invoke-missing-task
  [state-path run-record-dir current-dir current runner]
  (with-redefs-fn
    {#'runtime/find-giraffe-run (constantly current-dir)
     #'outer-state/load-makeup (constantly makeup)
     #'outer-observation/outer-observation (constantly current)
     #'focus-receipt/read-inputs (constantly {})
     #'focus-receipt/relation-context (constantly {})
     #'runtime/run-ordinary-opportunity-with-task! runner}
    (fn []
      (#'runtime/run-ordinary-opportunity!
       {:outer/state-path (.getPath state-path)
        :run-record-dir (.getPath run-record-dir)}))))

(deftest selection-pends-and-next-selection-learns
  (testing "the next selection consumes the previous click and creates its own pending row"
    (let [root (temp-dir)
          state-file (io/file root "state.edn")
          records (doto (io/file root "records") .mkdirs)
          old-dir (doto (io/file root "old-run") .mkdirs)
          first-opts (atom nil)]
      (invoke-missing-task state-file records old-dir old-observation
                           (fn [opts] (reset! first-opts opts) {:ran true}))
      (let [after-first (outer-state/read-state makeup state-file)
            first-run-id (:run-id @first-opts)
            pending (:pending after-first)]
        (is (= first-run-id (:run-id pending)))
        (is (= "old-run" (:giraffe/run-id pending)))
        (is (= :ready (get-in pending [:selection :cascade-id])))
        (spit (io/file records (str "tick-run-record-" first-run-id ".edn"))
              (pr-str {:run/id first-run-id
                       :run-ending-classification {:class :related}}))
        (let [current-dir (doto (io/file root "current-run") .mkdirs)
              second-opts (atom nil)]
          (with-redefs-fn
            {#'runtime/previous-giraffe-observation (fn [_ _ _] old-observation)}
            #(invoke-missing-task state-file records current-dir current-observation
                                  (fn [opts] (reset! second-opts opts) {:ran true})))
          (let [after-second (outer-state/read-state makeup state-file)]
            (is (= 1 (:click-count after-second)))
            (is (= 263/200 (get-in after-second [:c :related])))
            (is (= 19/10 (get-in after-second [:gain-counts :none])))
            (is (approx= 9/10 (get-in after-second [:state-counts :ready :doable])))
            (is (approx= 9/10 (get-in after-second [:state-counts :ready :blocked])))
            (is (= :applied
                   (get-in @second-opts [:outer/selection :outer/learning :status])))
            (is (= (:run-id @second-opts) (get-in after-second [:pending :run-id])))))))))

(deftest missing-run-record-defers-without-replacing-pending
  (let [root (temp-dir)
        state-file (io/file root "state.edn")
        records (doto (io/file root "records") .mkdirs)
        old-dir (doto (io/file root "old-run") .mkdirs)]
    (invoke-missing-task state-file records old-dir old-observation (constantly {:ran true}))
    (let [before (slurp state-file)
          current-dir (doto (io/file root "current-run") .mkdirs)
          received (atom nil)]
      (invoke-missing-task state-file records current-dir current-observation
                           (fn [opts] (reset! received opts) {:ran true}))
      (is (= before (slurp state-file)))
      (is (= {:status :deferred :reason :run-record-absent}
             (get-in @received [:outer/selection :outer/learning]))))))

(deftest gain-is-observed-only-after-selected-task-closes
  (let [before (observation "before" [candidate {:target "M-other" :reach 1/2}]
                            {"M-selected" {:status :ready :unblocks 0 :cone 0}
                             "M-other" {:status :not-ready :unblocks 0 :cone 0}})
        still-open (observation "open" [candidate {:target "M-other" :reach 1/2}]
                                {"M-selected" {:status :ready :unblocks 0 :cone 0}
                                 "M-other" {:status :ready :unblocks 0 :cone 0}})
        closed-one (update still-open :field #(vec (remove (fn [c]
                                                             (= "M-selected" (:target c))) %)))
        closed-several (assoc closed-one
                              :field [{:target "M-other" :reach 1/2}
                                      {:target "M-third" :reach 1/2}]
                              :facts {"M-other" {:status :ready}
                                      "M-third" {:status :ready}})]
    (is (= :none (#'runtime/ready-gain before still-open "M-selected")))
    (is (= :one (#'runtime/ready-gain before closed-one "M-selected")))
    (is (= :several (#'runtime/ready-gain before closed-several "M-selected")))))

(deftest caller-task-never-touches-outer-state
  (let [root (temp-dir)
        state-file (io/file root "state.edn")
        _ (spit state-file "sentinel\n")
        received (atom nil)]
    (with-redefs-fn
      {#'runtime/run-ordinary-opportunity-with-task!
       (fn [opts] (reset! received opts) {:ran true})}
      #(is (= {:ran true}
              (#'runtime/run-ordinary-opportunity!
               {:task {:id "M-caller"} :outer/state-path (.getPath state-file)}))))
    (is (= "sentinel\n" (slurp state-file)))
    (is (= {:id "M-caller"} (:task @received)))))

(deftest retained-qualified-already-satisfied-advances-c
  (let [attempt-root "/home/joe/code/futon2/data/wm-full-loop-machinery-156/wm-contract-machinery-156-v1/attempt-002"
        retained-run "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-29-b1a049d7-d4f8-4f63-be69-6872d86e8827.edn"
        close (edn/read-string {:default tagged-literal}
                               (slurp (str attempt-root "/007-closed.edn")))
        route (edn/read-string
               (slurp (str attempt-root "/retained/route-attestation.edn")))
        run-record (edn/read-string {:default tagged-literal} (slurp retained-run))
        grounded (get-in run-record [:failure :detail :grounded-commit])
        classification (run-ending/classify
                        {:close (assoc-in close [:payload :judgment :grounded-commit] grounded)
                         :route-attestation route
                         :outer-selection (:outer/selection run-record)})
        unqualified (run-ending/classify
                     {:close close
                      :route-attestation route
                      :outer-selection (:outer/selection run-record)})
        root (temp-dir)
        records (doto (io/file root "records") .mkdirs)
        current-dir (doto (io/file root "current-run") .mkdirs)
        run-id "retained-already-satisfied"
        selected {:cascade-index 0 :cascade-id :ready
                  :selection {:selected candidate}
                  :focus "WM" :selected-relation :associated}
        state (assoc (outer-state/init-state makeup)
                     :pending {:run-id run-id :giraffe/run-id "old-run"
                               :selection selected})]
    (spit (io/file records (str "tick-run-record-" run-id ".edn"))
          (pr-str {:run/id run-id :run-ending-classification classification}))
    (let [learned (with-redefs-fn
                    {#'runtime/previous-giraffe-observation
                     (fn [_ _ _] old-observation)}
                    #(#'runtime/resolve-pending-learning
                      {:run-record-dir (.getPath records)} makeup state current-dir
                      current-observation))]
      (is (= :related (:class classification)) (pr-str classification))
      (is (= :applied (:status learned)) (pr-str learned))
      (is (= 263/200 (get-in learned [:state :c :related])))
      (is (not= (:c state) (get-in learned [:state :c]))))
    (spit (io/file records (str "tick-run-record-" run-id ".edn"))
          (pr-str {:run/id run-id :run-ending-classification unqualified}))
    (let [deferred (with-redefs-fn
                     {#'runtime/previous-giraffe-observation
                      (fn [_ _ _] old-observation)}
                     #(#'runtime/resolve-pending-learning
                       {:run-record-dir (.getPath records)} makeup state current-dir
                       current-observation))]
      (is (= :known-typed-failure (:class unqualified)))
      (is (= :deferred (:status deferred)))
      (is (= :terminal-ending-class-absent (:reason deferred)))
      (is (= state (:state deferred))))))
