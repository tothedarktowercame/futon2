(ns futon2.report.beta-habit-wiring-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.beta-habit :as habit]
            [futon2.aif.trace :as trace]
            [futon2.report.war-machine :as wm]
            [futon2.report.war-machine-test :as wm-test]))

(defn fixture []
  (let [f (#'wm-test/real-shape-f-pi-fixture)
        ranked (mapv (fn [i a] (assoc a :controller-score (+ 0.25 (* 0.004 i))
                                       :habit-prior-bias (- (* 0.02 i))
                                       :habit-prior-source :learned-frequency))
                     (range) (:current f))]
    {:ranked ranked
     :fields (wm/f-pi-dark-readback (:previous f) ranked (:observation f))}))

(deftest old-caller-is-byte-identical
  (let [{:keys [ranked fields]} (fixture)
        expected (slurp "test/fixtures/beta-habit/legacy-carry.edn")]
    (is (= expected (pr-str (wm/beta-dark-carry {} fields ranked))))
    (is (= expected (pr-str (wm/beta-dark-carry {} fields ranked false))))))

(deftest configured-arm-reaches-real-carry-and-trace
  (let [{:keys [ranked fields]} (fixture)
        enabled (habit/enabled? {} (constantly {:beta-habit-in-both? true}) nil)
        result (wm/beta-dark-carry {} fields ranked enabled)
        state (:policy-precision-state result)
        record (trace/trace-record result)]
    (is (= :both (get-in state [:habit-provenance :placement])))
    (is (= 108 (count (get-in state [:habit-provenance :candidates]))))
    (is (= state (:policy-precision-state record)))
    (is (every? #(= :learned-frequency (:source %))
                (get-in state [:habit-provenance :candidates])))))

(deftest hold-keeps-the-requested-arm-visible
  (let [state (:policy-precision-state (wm/beta-dark-carry nil nil [] true))]
    (is (= :absent (:status state)))
    (is (= :habit-prior-in-both (get-in state [:habit-provenance :arm])))
    (is (= :unavailable (get-in state [:habit-provenance :source])))))
