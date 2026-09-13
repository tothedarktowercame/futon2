(require '[clojure.edn :as edn]
         '[futon2.aif.machine-budget-authority :as authority]
         '[futon2.aif.machine-budget-mapping :as mapping])

(def run-root
  "holes/labs/wm-contract/runs/row-22-e1-authority-resolution-2026-09-13")
(def pins (edn/read-string (slurp (str run-root "/fixture-pins.edn"))))
(def output
  (authority/resolve-and-map
   {:resolver/version authority/resolver-version
    :mode :isolated-test
    :root (str run-root "/fixtures")
    :sources (:files pins)}))
(def replay (mapping/replay (:replay/receipt output)))

(assert (= 3 (count (:ordered-support output))))
(assert (= 5 (count (get-in output [:verification :sources]))))
(assert (= :isolated-test (:scope output)))
(assert (:complete-accounting? output))
(assert (:replay/identical? replay))

(prn {:scope (:scope output)
      :identity (:identity output)
      :source-count (count (get-in output [:verification :sources]))
      :candidate-count (count (:ordered-support output))
      :occurrence-ids (mapv :candidate/id (:ordered-support output))
      :selected-ids (get-in output [:response :selected-ids])
      :dispositions (mapv (juxt :candidate/id :disposition) (:accounting output))
      :replay/identical? (:replay/identical? replay)
      :production-claim :none
      :assertions 5})
