(ns futon2.aif.outer-learning-runtime-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.outer-learning-runtime :as runtime]))

(deftest pinned-demonstration-run-remains-in-production-learning-fold
  (let [state (runtime/load-shadow)
        learned (:belief state)
        events (:events learned)
        demonstration
        (some #(when (= "2026-10-14-6a64c83f-4732-4af1-b286-561f22109667"
                        (first (:identity %))) %)
              events)]
    (is (= :live (:mode state)))
    (is (= :produced (:status learned)))
    (is (= (count events) (get-in learned [:counts :rows])))
    (is (= 22 (get-in learned [:counts :applied])))
    (is (= 7 (get-in learned [:counts :unresolved])))
    (is (= :grounded-collateral-progress
           (get-in demonstration [:observation :class])))
    (let [event demonstration]
      (is (= :applied (:update event)))
      (is (= 721840 (get-in event [:row-evidence :elapsed :value])))
      (is (= 4223565 (get-in event [:row-evidence :token-use :value])))
      (is (= {:selected 4 :positive 0 :no-want-effect 4}
             (select-keys (get-in event [:row-evidence :pattern-reinforcement])
                          [:selected :positive :no-want-effect]))))
    (let [failed-run "2026-10-18-f016ed51-62de-426e-88c2-768fa18b00dc"
          event (some #(when (= failed-run (first (:identity %))) %) events)
          intero (some #(when (= failed-run (:run/id %)) %)
                       (:interoceptive-evidence learned))
          rebuilt (:belief (runtime/load-shadow))]
      (is (= :not-applied (:update event)))
      (is (= :apparatus-failure-not-task-evidence
             (get-in event [:observation :reason])))
      (is (= :resume-protocol-violation (get-in intero [:cause :kind])))
      (is (= :cascade-resume (get-in intero [:cause :stage])))
      (is (= (:source-pin learned) (:source-pin rebuilt)))
      (is (= (:targets learned) (:targets rebuilt)))
      (is (= (:interoceptive-evidence learned)
             (:interoceptive-evidence rebuilt))))))
