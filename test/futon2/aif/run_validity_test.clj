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
    ;; Nine of the ten invalidate; f-computed-not-attached is NOTICED.
    ;; Went 6 -> 7 when g-terms-census-says-missing was added (claude-4,
    ;; after click 1 of 5 exposed a census placeholder scoring as ok), and
    ;; 7 -> 9 with AR-45's pair (claude-3, 2026-09-25).
    (is (= 9 (count (re-seq #"REJECTED" out)))
        "all nine invalidating perturbations rejected")
    (is (not (str/includes? out "NOT REJECTED")))))

(def ^:private fixture-dir "test/fixtures/run-validity")

(defn- check [file]
  (:out (shell/sh "bb" "scripts/wm_run_validity.bb" (str fixture-dir "/" file)
                  :dir repo-root)))

(defn- c-source-line
  "The REPORT's c-source row. The selftest prints its own `-> c-source:<verdict>`
  lines on every invocation, so match the report's column layout, not the name."
  [out]
  (some #(when (re-find #"^\s+c-source\s" %) (str/trim %)) (str/split-lines out)))

(deftest c-source-reads-live-c-provenance-and-types-its-absence
  ;; AR-45. The two fixtures differ in one value: what sits at
  ;; [:decision :selection-certificate :scoring 0 :c]. Before daf2124e
  ;; (2026-09-22) that path held live-C's provenance; since then it holds the
  ;; scorer's step-indexed preference schedule and the provenance reaches no
  ;; run record. Both shapes are taken from the real records named in the
  ;; fixtures' own headers.
  (testing "provenance present: ok, exactly as before"
    (let [out (check "before-daf2124e.edn")]
      (is (str/includes? out "VALID (5/5 ok)"))
      (is (= "c-source           ok       [:decision :selection-certificate :scoring 0 :c]"
             (c-source-line out)))))
  (testing "a schedule under :c types the absence and is reported, not judged"
    (let [out (check "after-daf2124e.edn")]
      (is (str/includes? out "INVALID (4/5 ok)"))
      (is (= (str "c-source           absent   "
                  "[:decision :selection-certificate :scoring 0 :c]  "
                  "{:c-provenance {:absent :no-c-provenance-on-record}}  "
                  "{:c :preference-schedule, :steps 2}")
             (c-source-line out)))
      (is (not (str/includes? (c-source-line out) "bad"))
          "the schedule is not a wrong value; it is the wrong quantity")))
  (testing "the bad case is in the checker's own negative control"
    ;; A schedule that HAPPENS to carry :status :derived must still read as a
    ;; schedule: the discrimination is by shape, never by :status being there.
    (let [out (:out (shell/sh "bb" "scripts/wm_run_validity.bb" "selftest" :dir repo-root))]
      (is (re-find #"schedule-carrying-status-derived\s+-> c-source:absent" out))
      (is (re-find #"c-is-a-preference-schedule\s+-> c-source:absent" out)))))

(deftest invalid-runs-never-fail-the-process
  ;; Evidence, not a gate: run the checker over whatever run records exist
  ;; today; whatever their verdicts, the process exits 0 and reports.
  (let [{:keys [exit out]} (shell/sh "bb" "scripts/wm_run_validity.bb"
                                     :dir repo-root)]
    (testing "checker exits 0 regardless of run verdicts"
      (is (zero? exit)))
    (is (str/includes? out "== summary"))
    (is (str/includes? out "evidence, not a gate"))))
