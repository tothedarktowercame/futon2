(ns holes.labs.wm-contract.runs.row-16-r6-policy-set-2026-09-12.readback
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [futon2.aif.machine-policy-set :as policy-set]))

(def run-dir "holes/labs/wm-contract/runs/row-16-r6-policy-set-2026-09-12")

(defn refusal [f]
  (try (f) {:refusal :missing}
       (catch clojure.lang.ExceptionInfo e (ex-data e))))

(def fixture (edn/read-string (slurp (str run-dir "/fixture.edn"))))
(def expected (:projected fixture))
(def actual (policy-set/read-projected-policy-set run-dir "policy-set"))

(def output
  {:schema :wm/row16-r6-policy-set-readback-v1
   :subject (:subject fixture)
   :production-reader 'futon2.aif.trace/read-trace
   :projection 'futon2.aif.machine-policy-set/project-ranked-actions
   :positive (policy-set/compare-projections! expected actual)
   :controls
   {:order-mutation
    (refusal #(policy-set/compare-projections! expected
                                                (vec (concat [(second actual) (first actual)]
                                                             (drop 2 actual)))))
    :dropped-candidate
    (refusal #(policy-set/compare-projections! expected (pop actual)))}})

(spit (str run-dir "/readback.edn") (with-out-str (pp/pprint output)))
(prn (select-keys output [:subject :production-reader :projection]))
(prn {:positive (select-keys (:positive output) [:status :candidate-count])
      :controls (update-vals (:controls output) #(select-keys % [:refusal :first-mismatch
                                                                  :expected-count :observed-count]))})
