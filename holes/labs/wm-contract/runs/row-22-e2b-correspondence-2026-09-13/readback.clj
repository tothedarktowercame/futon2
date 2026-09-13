(require '[clojure.edn :as edn]
         '[futon2.aif.machine-budget-authority :as authority]
         '[futon2.aif.machine-enactment-correspondence :as correspondence])

(def e1-run "holes/labs/wm-contract/runs/row-22-e1-authority-resolution-2026-09-13")
(def e2b-run "holes/labs/wm-contract/runs/row-22-e2b-correspondence-2026-09-13")
(def e1-pins (edn/read-string (slurp (str e1-run "/fixture-pins.edn"))))
(def witness-pins (edn/read-string (slurp (str e2b-run "/witness-pins.edn"))))
(def output
  (correspondence/verify-correspondence
   {:mode :isolated-test
    :e2a-resolver {:resolver/version authority/resolver-version
                   :mode :isolated-test :root (str e1-run "/fixtures")
                   :sources (:files e1-pins)}
    :witness-root (str e2b-run "/fixtures")
    :witnesses (:witnesses witness-pins)}))

(assert (= :exact-occurrence-and-action (:correspondence output)))
(assert (= (:selected output) (:enacted output)))
(assert (= 2 (count (:approved-domain output))))
(assert (= :required-external-dependency
           (get-in output [:r9-pre-enact-authorization :status])))

(prn {:scope (:scope output) :identity (:identity output)
      :cohort/id (:cohort/id output)
      :approved-occurrence-ids (mapv :candidate/id (:approved-domain output))
      :selected (:selected output) :enacted (:enacted output)
      :correspondence (:correspondence output)
      :selection-proof (:selection-proof output)
      :r9-pre-enact-authorization (:r9-pre-enact-authorization output)
      :production-claim :none :assertions 4})
