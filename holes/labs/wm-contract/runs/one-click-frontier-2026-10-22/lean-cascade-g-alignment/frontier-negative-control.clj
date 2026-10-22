(require '[clojure.test :as t]
         '[futon2.aif.cascade-model-manifest :as m]
         '[futon2.aif.cascade-coapply-g-test :as checks])
(let [counters (ref t/*initial-report-counters*)]
  (binding [t/*report-counters* counters]
    (with-redefs [m/enabled-frontier
                  (fn [units _descent patterns state]
                    (vec (filter #(true? (m/guard-holds? (patterns %) state)) units)))]
      (t/test-vars [#'checks/frontier-and-shared-horizon])))
  (prn {:negative-control :all-enabled-instead-of-frontier :counters @counters})
  (assert (= 2 (:fail @counters)))
  (assert (zero? (:error @counters))))
