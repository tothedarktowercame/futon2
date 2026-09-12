(require '[clojure.java.io :as io]
         '[futon2.aif.trace :as trace])
(load-file "scripts/futon2/report/war_machine.clj")

(def output-path
  "holes/labs/wm-contract/runs/row-15-depth-capture-2026-09-12/capture.edn")
(defn machinery-selector [{:keys [controller-ranking]}]
  (let [mission-id (some #(get-in % [:action :target]) controller-ranking)]
    {:status :verified-live-selection
     :selected-mission-ids [mission-id]
     :consulted-ranking :machinery-test-controller-ranking
     :actuation {:status :machine-authorized-bounded-autonomy
                 :authorized? true :executed? false}
     :provenance {:selector-seam :explicit-machinery-test}}))
(def result
  ((requiring-resolve 'futon2.report.war-machine/generate-war-machine)
   1 {:trace? false
      :step-portfolio? false
      :step-mission-detail-portfolio? false
      :strategic-selection-fn machinery-selector
      :policy-depth {:anticipation 3 :cascade-rollout 3}}))
(def judgement (:judgement result))
(def record (trace/trace-record judgement))
(def capture
  {:schema :wm/depth-capture-v1
   :redirected-output output-path
   :daily-trace-written? false
   :scorer-input {:horizon-steps (:horizon-steps judgement)}
   :actual-policy-depth (:policy-depth-used judgement)
   :trace-record (select-keys record [:timestamp :horizon-steps
                                      :policy-depth-used :policy-depth])})
(spit (io/file output-path) (pr-str capture))
(prn capture)
