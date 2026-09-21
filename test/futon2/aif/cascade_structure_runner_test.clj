(ns futon2.aif.cascade-structure-runner-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-structure :as structure]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(deftest grounded-construction-checkpoint-retains-structural-receipt
  ;; Owner runs on canonical main: source authority is intentionally not stubbed.
  (let [a fixture/selected-action
        {:keys [result]}
        (#'fixture/run-feature-card-attempt {:author-card fixture/feature-card-claim})
        c (get-in result [:checkpoints :construction :judgment :cascade])]
      (is (= :grounded-change (:outcome result)))
      (is (= (structure/receipt a) (:cascade-structure c)))
      ;; The passing fixture uses bare pattern IDs, not interpreted guards.
      ;; Retain the honest typed absence without changing its grounded outcome.
      (is (= :unavailable (get-in c [:cascade-structure :status])))
      (is (= :unsupported-pattern-interpretation
             (get-in c [:cascade-structure :findings 0 :kind])))
      (is (not (contains? c :semilattice)))
      (is (= c (edn/read-string (pr-str c))))))
