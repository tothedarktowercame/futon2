(ns futon2.aif.run-validity-test
  "Warrant carrier for scripts/wm_run_validity.bb (realness v2, Joe's
  uniformity ruling RULING-run-certificate-uniformity-2026-09-19.md).

  The checker's semantic pins live in its own selftest — one complete
  fixture accepted, eight perturbations exercised, seven of which invalidate
  (the negative control that makes the checker distinguishable from a
  constant; the eighth, f-computed-not-attached, only annotates). This test
  executes
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
    ;; Counted, not derived: a perturbation that silently disappears from
    ;; the list is exactly the failure this assertion exists to catch.
    ;; Seven of the eight invalidate; f-computed-not-attached is NOTICED.
    ;; Went 6 -> 7 when g-terms-census-says-missing was added (claude-4,
    ;; after click 1 of 5 exposed a census placeholder scoring as ok).
    (is (= 7 (count (re-seq #"REJECTED" out)))
        "all seven invalidating perturbations rejected")
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
