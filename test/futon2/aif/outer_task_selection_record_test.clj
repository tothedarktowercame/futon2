(ns futon2.aif.outer-task-selection-record-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def receipt
  {:schema :wm/outer-task-selection-v1
   :policy {:kind :seeded-uniform-task-support
            :uses [:id :kind :requisition :source]
            :forbids [:cascade :universes :candidates :g]}
   :support [{:id "M-live" :kind :mission
              :requisition {:absent :no-requisition}
              :source {:path "/repo/M-live.md" :sha256 (apply str (repeat 64 "a"))}}]
   :excluded [{:id "E-done" :kind :excursion :eligible false
               :ineligible-reason :requisition/completed
               :ineligibility-evidence
               {:requisition {:state :completed}
                :source {:path "/repo/E-done.md"
                         :sha256 (apply str (repeat 64 "b"))}}}]
   :draw {:generator "java.util.SplittableRandom"
          :seed 17 :index 0 :support-count 1}
   :chosen {:id "M-live" :kind :mission}
   :action {:type :advance-mission :target "M-live"}})

(defn- temp-dir []
  (.toFile (Files/createTempDirectory
            "outer-task-record-" (make-array FileAttribute 0))))

(defn- persist [dir id selection outcome]
  (let [written (#'runner/persist-run-record!
                 {:run-record-dir (str dir) :scan-render-fn (fn [& _] nil)}
                 id "2026-10-02T00:00:00Z"
                 {:outcome outcome :checkpoints {:selection selection}})]
    (edn/read-string (slurp (:run-record written)))))

(deftest successful-progression-preserves-the-complete-receipt
  (let [dir (temp-dir)
        decision {:status :selected :action {:target "M-live"}}
        record (persist dir "selected"
                        (runner/retain-outer-task-selection
                         {:judgment {:controller-decision decision}}
                         true receipt)
                        :grounded-change)]
    (try
      (is (= receipt (:outer-task-selection record)))
      (is (= (:excluded receipt)
             (get-in record [:outer-task-selection :excluded])))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))

(deftest terminal-abstention-preserves-the-same-receipt
  (let [dir (temp-dir)
        decision {:status :abstained
                  :refusals [{:target "M-live" :kind :universe-not-admitted
                              :missing :universes}]}
        record (persist dir "abstained"
                        (runner/retain-outer-task-selection
                         {:sorry {:kind :no-selection :decision decision}}
                         false receipt)
                        :abstained)]
    (try
      (is (= receipt (:outer-task-selection record)))
      (is (= 17 (get-in record [:outer-task-selection :draw :seed])))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))

(deftest missing-and-mutated-receipts-fail-closed-without-recomputation
  (let [dir (temp-dir)
        decision {:status :selected :action {:target "M-live"}}
        missing (persist dir "missing"
                         {:judgment {:controller-decision decision}}
                         :grounded-change)
        mutated-receipt (assoc-in receipt [:chosen :id] "M-other")
        mutated (persist dir "mutated"
                         {:judgment {:controller-decision decision
                                     :outer-task-selection mutated-receipt}}
                         :grounded-change)]
    (try
      (is (= {:schema :wm/outer-task-selection-v1
              :status :absent :reason :receipt-not-retained}
             (:outer-task-selection missing)))
      (is (= :receipt-identity-inconsistent
             (get-in mutated [:outer-task-selection :reason])))
      (is (= "M-other"
             (get-in mutated [:outer-task-selection :receipt-target])))
      (is (= ["M-live"]
             (get-in mutated [:outer-task-selection :decision-targets])))
      (is (= mutated-receipt
             (get-in mutated [:outer-task-selection :observed-receipt])))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))
