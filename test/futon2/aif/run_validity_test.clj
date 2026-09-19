(ns futon2.aif.run-validity-test
  "Warrant carrier for scripts/wm_run_validity.bb (realness v2, Joe's
  uniformity ruling RULING-run-certificate-uniformity-2026-09-19.md).

  The checker's semantic pins live in its own selftest — one complete
  fixture accepted, six perturbations rejected (the negative control that
  makes the checker distinguishable from a constant). This test executes
  that selftest and asserts the checker's evidence-not-gate contract on a
  live record. It deliberately does NOT pin any live run's verdict: the
  EV-uniform-run-record lanes flipping runs from :invalid to :valid must
  not redden the checker's own warrant."
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(def repo-root (str (System/getProperty "user.home") "/code/futon2"))

(deftest selftest-is-the-negative-control
  (let [{:keys [exit out]} (shell/sh "bb" "scripts/wm_run_validity.bb" "selftest"
                                     :dir repo-root)]
    (is (zero? exit) "selftest exits 0 when and only when all controls hold")
    (is (str/includes? out "SELFTEST PASS") "complete fixture accepted")
    (is (= 6 (count (re-seq #"REJECTED" out)))
        "all six perturbations rejected")
    (is (not (str/includes? out "NOT REJECTED")))))

(deftest invalid-runs-never-fail-the-process
  ;; Evidence, not a gate: run the checker over whatever run records exist
  ;; today; whatever their verdicts, the process exits 0 and reports.
  (let [{:keys [exit out]} (shell/sh "bb" "scripts/wm_run_validity.bb"
                                     :dir repo-root)]
    (testing "checker exits 0 regardless of run verdicts"
      (is (zero? exit)))
    (is (str/includes? out "== summary"))
    (is (str/includes? out "evidence, not a gate"))))
