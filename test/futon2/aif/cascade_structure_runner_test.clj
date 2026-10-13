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
  (let [{:keys [result]}
        (#'fixture/run-token-feature-card-attempt
         (#'fixture/retention-cohort "cascade-structure-runner")
         (#'fixture/token-loop-exercised-decision))
        c (get-in result [:checkpoints :construction :judgment :cascade])
        a (:selected-action c)
        receipt (:cascade-structure c)
        other-action (assoc a :target "M-not-the-selected-population")]
    (is (= :grounded-progress (:outcome result))
        "the current authoritative fixture records a real grounded increment")
    (is (= (structure/receipt a) receipt))
    (is (= :computed (:status receipt)))
    (is (= :singleton (:shape receipt)))
    (is (not (contains? c :semilattice)))
    (is (= c (edn/read-string (pr-str c))))
    (is (not= (:input-sha256 receipt)
              (:input-sha256 (structure/receipt other-action)))
        "a receipt from another target population cannot satisfy exact identity")
    (is (not= receipt (structure/receipt other-action)))))
