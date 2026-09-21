(ns futon2.aif.cascade-habit-reinforcement-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-habit-reinforcement :as reinforcement]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.cascade-prior :as prior]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.report.cascade-habit-read-test :as fixture]))

(defn comparison [decision observed]
  ;; fix-10a token-outcome/compare-outcomes shape, including target-qualified token.
  (let [action (:action decision) token [(:target action) :shared-updater]]
    {:schema :wm/token-outcome-comparison-v1 :status :compared :artifact-sha "fixture-sha"
     :evidence-sha256 "fixture-evidence-digest"
     :prediction {:schema :wm/token-outcome-prediction-v1 :status :frozen
                  :target (:target action) :action action
                  :wanted [{:token token :predicted 1}]}
     :tokens [{:token token :predicted 1 :observed observed
               :verdict (if observed :predicted-and-observed :predicted-not-observed)}]}))

(deftest close-reinforces-only-observed-predictions
  (fixture/with-store
   (fn [path]
     (let [decision {:action (second (fixture/menu))}
           key (prior/policy-key (habit/policy-view (:action decision)))
           yes (comparison decision true)
           no (comparison decision false)]
       (doseq [[outcome receipt reason] [[:grounded-change nil :no-outcome-comparison]
                                         [:build-failed nil :no-outcome-comparison]
                                         [:grounded-change no :no-predicted-and-observed]]]
         (let [r (reinforcement/close! path decision outcome receipt)]
           (is (= :none (:reinforcement r)))
           (is (= 0 (:delta r)))
           (is (= reason (:reason r)))
           (is (= reinforcement/rule-id (:rule/id r)))
           (is (not (.exists (io/file path))))))
       (let [r (reinforcement/close! path decision :grounded-change yes)]
         (is (= :increment (:reinforcement r)))
         (is (= 1 (:delta r)))
         (is (= {key 1} (:counts (habit/read-state path))))
         (is (= {key reinforcement/rule-id} (:reinforcement-bases (habit/read-state path)))))
       (let [before (slurp path)]
         (reinforcement/close! path decision :grounded-change no)
         (is (= before (slurp path))))
       ;; The outcome label is not the rule: observed evidence can survive a later failure.
       (reinforcement/close! path decision :build-failed yes)
       (is (= 2 (:samples (habit/read-state path))))))))

(deftest comparison-must-belong-to-selected-action-and-be-consistent
  (fixture/with-store
   (fn [path]
     (let [decision {:action (second (fixture/menu))}
           yes (comparison decision true)]
       (doseq [bad [(assoc yes :schema :unknown)
                    (assoc yes :status :refused)
                    (assoc-in yes [:prediction :action :target] "another-target")
                    (assoc-in yes [:tokens 0 :observed] false)
                    (assoc-in yes [:tokens 0 :predicted] 0)
                    (assoc-in yes [:tokens 0 :token] ["another-target" :shared-updater])
                    (update yes :tokens conj (first (:tokens yes)))]]
         (let [r (reinforcement/close! path decision :grounded-change bad)]
           (is (= :none (:reinforcement r)))
           (is (= :invalid-outcome-comparison (:reason r)))))
       (doseq [[p observed verdict] [[0 true :not-predicted-observed]
                                     [0 false :neither]
                                     [1 {:status :missing :kind :no-measurement} :observation-missing]]]
         (let [receipt (-> yes
                           (assoc-in [:prediction :wanted 0 :predicted] p)
                           (assoc-in [:tokens 0 :predicted] p)
                           (assoc-in [:tokens 0 :observed] observed)
                           (assoc-in [:tokens 0 :verdict] verdict))]
           (is (= :no-predicted-and-observed
                  (:reason (reinforcement/close! path decision :grounded-change receipt))))))
       (is (not (.exists (io/file path))))))))

(deftest run-record-separates-selection-from-reinforcement
  (fixture/with-store
   (fn [path]
     (let [decision {:action (second (fixture/menu))}
           r (reinforcement/close! path decision :grounded-change (comparison decision true))
           saved (#'runner/persist-run-record!
                  {:run-record-dir (str (.getParentFile (io/file path)))} "habit" "2026-09-21"
                  {:outcome :grounded-change :habit-reinforcement r
                   :checkpoints {:selection {:judgment {:controller-decision decision}}}})
           record (edn/read-string (slurp (:run-record saved)))]
       (is (= r (:habit-reinforcement record)))
       (is (= :cascade-selected (get-in record [:selection-event :event])))
       (is (= :none (get-in record [:selection-event :reinforcement])))
       (is (= :selection-is-not-outcome (get-in record [:selection-event :reason])))))))
