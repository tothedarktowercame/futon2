(ns futon2.aif.selected-want-two-instrument-test
  "MISSION-TEXT-ONLY-WANT-LOCATORS: the two-instrument rule at measurement."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.selected-want-outcome :as outcome]))

(def record-path
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-31-ebbbe42a-9889-4bff-921d-0843ea85a699.edn")

(def retained (delay (edn/read-string (slurp record-path))))

(def p2 ["M-typed-memories" :hole/ha4d7c19e5cdd])

(defn- retained-input
  "Rebuild the receipt input from the retained ebbbe42a record's own
  execution-outcomes rows, locators and :run-output demo receipt."
  [& [overrides]]
  (let [rows (get-in @retained [:cascade-feedback :mission-state
                                :execution-outcomes :outcomes])
        demo (:run-output @retained)]
    (merge
     {:selected-action
      {:target "M-typed-memories"
       :want (mapv :want rows)
       :observation-locators
       (into {} (map (fn [row]
                       [(:want row)
                        (get-in row [:after :measurement :declared-locator])])
                     rows))}
      :mission-observation
      {:status :admitted
       :rows (mapv (fn [row]
                     {:token (:want row)
                      :before {:observed false}
                      :after (select-keys (:after row) [:observed :measurement])})
                   rows)}
      :demo-receipt demo}
     overrides)))

(deftest retained-ebbbe42a-p2-stays-reached-and-corroborated
  (let [receipt (outcome/receipt (retained-input))
        p2-outcome (some #(when (= p2 (:want %)) %) (:outcomes receipt))]
    (is (= :verified (:status receipt)) (pr-str receipt))
    (is (= :reached (:classification p2-outcome)))
    (is (= :corroborated (:instrument p2-outcome)))
    (is (= [p2] (:reached (:by-class receipt))))
    (is (= [] (:checkbox-only (:by-class receipt))))
    ;; The demo pointer names the revision the checkbox was measured at.
    (is (= (get-in @retained [:run-output :demo :pointer :sha])
           (get-in p2-outcome [:after :measurement :after-locator :sha])))))

(deftest checkbox-flip-without-demo-receipt-does-not-reach
  (let [receipt (outcome/receipt (retained-input {:demo-receipt nil}))
        p2-outcome (some #(when (= p2 (:want %)) %) (:outcomes receipt))]
    ;; Nothing is refused: the receipt stays verified, the observation stays
    ;; recorded, the want is simply not promoted and stays open/selectable.
    (is (= :verified (:status receipt)) (pr-str receipt))
    (is (= true (get-in p2-outcome [:after :observed]))
        "the checkbox observation is still recorded")
    (is (= :checkbox-only (:classification p2-outcome)))
    (is (= :checkbox-only (:instrument p2-outcome)))
    (is (= [] (:reached (:by-class receipt)))
        "no want reaches on a checkbox flip alone")
    (is (= [p2] (:checkbox-only (:by-class receipt))))))

(deftest demo-at-another-revision-does-not-corroborate
  ;; Corroboration is bound to the measured revision: a present demo receipt
  ;; whose pointer names some other sha is not a second instrument.
  (let [other (assoc-in @retained [:run-output :demo :pointer :sha]
                        (apply str (repeat 40 "e")))
        receipt (outcome/receipt (retained-input {:demo-receipt other}))
        p2-outcome (some #(when (= p2 (:want %)) %) (:outcomes receipt))]
    (is (= :checkbox-only (:classification p2-outcome)))
    (is (= [] (:reached (:by-class receipt))))))

(deftest c8-locator-is-a-second-instrument
  ;; A retained C8 after-locator (registered passing run / named command)
  ;; corroborates even without a demo receipt. The C8 sha must equal the
  ;; measured resolved-sha or the source-mismatch problem fires first.
  (let [sha (get-in @retained [:run-output :demo :pointer :sha])
        c8 {:class :C8 :repo "futon3c" :sha sha
            :namespace "futon3c.peripheral.memory-supersession-asof-test"}
        input (retained-input {:demo-receipt nil})
        tweak (fn [row]
                (if (= p2 (:token row))
                  (assoc-in row [:after :measurement :after-locator] c8)
                  row))]
    (let [receipt (outcome/receipt
                   (update-in input [:mission-observation :rows] #(mapv tweak %)))
          p2-outcome (some #(when (= p2 (:want %)) %) (:outcomes receipt))]
      (is (= :verified (:status receipt)) (pr-str receipt))
      (is (= :reached (:classification p2-outcome)))
      (is (= :corroborated (:instrument p2-outcome))))))
