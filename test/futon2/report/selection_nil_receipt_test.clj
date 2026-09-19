(ns futon2.report.selection-nil-receipt-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.calibration-cycle :as calibration]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.report.war-machine :as wm]))

(def scan-vars
  '[fetch-evidence-result fetch-missions scan-self-watch scan-loop-health
    scan-support-attack scan-mission-triage scan-graph scan-sessions scan-portfolio
    scan-mission-detail scan-patterns scan-frames scan-metabolic-balance
    summarize-working-tree-hygiene scan-blocks scan-window scan-annotation-graph
    scan-strategic-vocabulary scan-r-criteria scan-vsatarcs-status capability-star-map])

(deftest nil-receipt-does-not-end-selection-rendering
  ;; Exact missing carrier identified by b30d915e for cohort 57 attempt-002.
  ;; A missing receipt is a wiring gap, not a falsely claimed admission refusal.
  (let [md (wm/render-war-machine {:r12-apparatus {:available? true :class-count 0}
                                   :r12-admission nil :now "2026-09-18" :days 14})]
    (is (string? md))
    (is (.contains md "NOT WIRED"))
    (is (not (.contains md "REFUSED")))))

(deftest producer-hands-admitted-and-refused-receipts-to-renderer
  (doseq [[receipt expected] [[{:ok true :returned {:return/id "fixture/r12"}} "Admission: admitted"]
                              [{:ok false :error/code :r12/untied-return} "REFUSED `untied-return`"]]]
    (let [captured (atom nil) render wm/render-war-machine
          action {:type :open-mission :target "fixture/valid-target"}
          judgement {:decision {:action action :controller-score 1.0
                                :selection-law {:applied :cascade-selection-posterior}}}
          stubs (into {} (for [sym scan-vars]
                           [(ns-resolve 'futon2.report.war-machine sym) (constantly nil)]))]
      (with-redefs-fn
        (merge stubs
               {#'wm/scan-r12-apparatus (constantly {:available? true :class-count 0})
                #'calibration/admit-apparatus! (fn [& _] receipt)
                #'wm/judge (fn [& _] judgement)
                #'wm/render-war-machine
                (fn [data]
                  (reset! captured data)
                  ;; Isolate the R12 renderer from unrelated report sections.
                  (render (select-keys data [:r12-apparatus :r12-admission :now :days])))})
        (fn []
          (let [result (wm/generate-war-machine 14)]
            (is (= receipt (:r12-admission @captured)))
            (is (= receipt (get-in result [:data :r12-admission])))
            (is (= action (:action (#'runner/selected-entry (:judgement result)))))
            (is (.contains (:markdown result) expected))))))))
