(ns capture
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.trace :as trace]))

(def dir "holes/labs/wm-contract/runs/row-15-selector-trace-2026-09-12/redirected")
(def out "holes/labs/wm-contract/runs/row-15-selector-trace-2026-09-12/readback.edn")
(def envelope
  {:schema :wm/selection-proof-input-v1
   :algorithm/revision {:name :shadow-policy :revision 1}
   :decision-id "retained-decision" :temperature 1.0
   :candidate-domain ["M-a" "M-b"]
   :policy-table
   [{:policy-id "pi-a" :mission-ids ["M-a"] :E_S -0.2 :G_S 0.3
     :log-shadow-potential -0.5 :shadow-probability 0.6
     :hard-support {:status :supported} :provenance [:memory/a]}
    {:policy-id "pi-b" :mission-ids ["M-b"] :E_S -0.4 :G_S 0.5
     :log-shadow-potential -0.9 :shadow-probability 0.4
     :hard-support {:status :supported} :provenance [:memory/b]}]
   :tie-break :ascending-policy-id :selected-policy-id "pi-a"})

(defn -main [& _]
  (let [input {:belief {} :observation {} :free-energy {} :ranked-actions []
               :decision {:selection-proof-input envelope} :mode :maintain}
        _ (binding [trace/*persist-policy-trace-details?* false]
            (trace/write-trace! input :dir dir :date-str "2026-09-12"))
        record (first (trace/read-trace :dir dir :date-str "2026-09-12"))
        retained (get-in record [:decision :selection-proof-input])
        result {:schema :wm/selection-proof-input-readback-v1
                :exact? (= envelope retained) :input envelope :retained retained}]
    (io/make-parents out)
    (spit out (str (pr-str result) "\n"))
    (prn (select-keys result [:schema :exact?]))))

(apply -main *command-line-args*)
