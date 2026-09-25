(ns futon2.aif.loop-closure-test
  "The loop-closure boxes of the M-wm-wiring map, one test per step as each
  is wired. Step 7: the flight entry reads :chosen-target (the outer
  cascade's field, written by nothing yet), else takes --target and records
  that it was placed by hand (flight-driver/resolve-target, which
  run-flight!, plan and read-only! call; nothing is flown here)."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.flight-driver :as driver]))

(deftest a-hand-placed-target-says-so
  (is (= {:target "M-autoclock-in" :target-source :hand-placed}
         (driver/resolve-target {:target "M-autoclock-in"}))))

(deftest a-chosen-target-says-so-with-the-seed-or-its-absence
  (is (= {:target "M-autoclock-in" :target-source :chosen :draw-seed {:absent :no-draw-seed}}
         (driver/resolve-target {:chosen-target "M-autoclock-in"})))
  (is (= 42 (:draw-seed (driver/resolve-target {:chosen-target "M-autoclock-in" :draw-seed 42})))))

(deftest the-chosen-target-wins-and-the-hand-target-is-kept
  (is (= {:target "M-chosen" :target-source :chosen :draw-seed {:absent :no-draw-seed}
          :hand-target-overridden "M-autoclock-in"}
         (driver/resolve-target {:chosen-target "M-chosen" :target "M-autoclock-in"}))))

(deftest neither-is-the-drivers-missing-target-refusal
  ;; the same ex-info check-args! has thrown for a missing --target
  (is (= {:missing :target}
         (try (driver/resolve-target {}) nil
              (catch clojure.lang.ExceptionInfo e (ex-data e)))))
  (is (= {:missing :target}
         (try (driver/resolve-target {:target "  " :chosen-target ""}) nil
              (catch clojure.lang.ExceptionInfo e (ex-data e))))
      "blank is not a target"))

(deftest eligibility-is-not-filtered-here
  ;; a chosen target that is not a lifecycle mission is carried as given,
  ;; with the field's entry beside it: eligibility is the field's to record
  (let [entry {:target "M-agent-minted" :reason :not-lifecycle-shaped}]
    (is (= {:target "M-agent-minted" :target-source :chosen :draw-seed {:absent :no-draw-seed}
            :field-entry entry}
           (driver/resolve-target {:chosen-target "M-agent-minted" :field-entry entry})))))

(deftest the-flight-record-carries-the-placement
  (let [f (#'driver/flight-for {:target "M-autoclock-in" :repo "futon3c"
                                :path "holes/missions/M-autoclock-in.md" :read-text (fn [& _] "") :id "f"})]
    (is (= :hand-placed (:target-source f)))
    (is (= "M-autoclock-in" (:target f)))))
