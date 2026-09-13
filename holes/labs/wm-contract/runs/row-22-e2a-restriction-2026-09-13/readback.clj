(require '[clojure.edn :as edn]
         '[futon2.aif.machine-budget-authority :as authority]
         '[futon2.aif.machine-portfolio-restriction :as restriction])

(def e1-root
  "holes/labs/wm-contract/runs/row-22-e1-authority-resolution-2026-09-13")
(def pins (edn/read-string (slurp (str e1-root "/fixture-pins.edn"))))
(def resolver-config
  {:resolver/version authority/resolver-version
   :mode :isolated-test
   :root (str e1-root "/fixtures")
   :sources (:files pins)})
(def output (restriction/restrict-portfolio resolver-config))
(def replay (restriction/replay (:replay/receipt output)))

(assert (= 3 (count (:full-support output))))
(assert (= 2 (count (:approved-support output))))
(assert (= 1 (count (:excluded-support output))))
(assert (= (mapv :candidate/id (:approved-support output))
           (:approved-occurrence-ids output)))
(assert (:replay/identical? replay))

(prn {:scope (:scope output)
      :identity (:identity output)
      :full-occurrence-ids (mapv :candidate/id (:full-support output))
      :approved-occurrence-ids (:approved-occurrence-ids output)
      :excluded-occurrence-ids (:excluded-occurrence-ids output)
      :actions-preserved?
      (= (mapv :action (:approved-support output))
         (mapv :action (filterv #(contains? (set (:approved-occurrence-ids output))
                                             (:candidate/id %))
                                (:full-support output))))
      :replay/identical? (:replay/identical? replay)
      :production-claim :none
      :assertions 5})
