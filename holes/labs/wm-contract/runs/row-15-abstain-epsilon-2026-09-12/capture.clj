(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.policy :as policy]
         '[futon2.aif.trace :as trace])

(def run-dir
  "holes/labs/wm-contract/runs/row-15-abstain-epsilon-2026-09-12")

(def redirected-dir (str run-dir "/redirected-trace"))
(def date-str "2026-09-12")
(def epsilon 0.125)
(def ranked
  [{:action {:type :address-sorry} :controller-score 0.1}
   {:action {:type :no-op} :controller-score 0.5}])

(def decision (policy/select-action ranked {:abstain-epsilon epsilon}))
(binding [trace/*persist-policy-trace-details?* false]
  (trace/write-trace! {:ranked-actions ranked
                       :decision decision
                       :mode :machinery-capture}
                      :dir redirected-dir :date-str date-str))
(def persisted (first (trace/read-trace :dir redirected-dir :date-str date-str)))
(def readback
  {:schema :wm/abstain-epsilon-capture-v1
   :scope :redirected-machinery-trace
   :input {:abstain-epsilon epsilon}
   :decision {:action (:action decision)
              :abstain-epsilon (:abstain-epsilon decision)}
   :persisted {:action (get-in persisted [:decision :action])
               :abstain-epsilon (get-in persisted [:decision :abstain-epsilon])}
   :checks {:decision-value? (= epsilon (:abstain-epsilon decision))
            :persisted-present? (contains? (:decision persisted) :abstain-epsilon)
            :persisted-value? (= epsilon
                                 (get-in persisted [:decision :abstain-epsilon]))
            :softmax-stripped? (not (contains? (:decision persisted)
                                               :softmax-weights))}})

(when-not (every? true? (vals (:checks readback)))
  (throw (ex-info "abstain epsilon capture failed" readback)))
(spit (io/file run-dir "capture.edn") (str (pr-str readback) "\n"))
(println (pr-str readback))
