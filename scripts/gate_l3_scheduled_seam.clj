;; gate_l3_scheduled_seam.clj -- E-live-loop-3 gate L3 (standing evidence check).
;; Gate command (in e-live-loop-3-steps.edn) runs the test runner directly:
;;   clojure -A:test -m cognitect.test-runner -n futon2.aif.enact-scheduled-path-test -n futon2.aif.fold-escrow-seam-test -n futon2.aif.fold-escrow-test 2>&1 | grep -q '0 failures, 0 errors'
;;
;; PASS iff: (a) the scheduled-path test suite is green (3 tests); (b) the
;; existing seam suite is green (2 tests); (c) the fold-escrow loader suite is
;; green (3 tests). Together these cover: the deposit-grain circumstance
;; reconstruction works, prompt-drift abstains (pin working), no-deposit
;; falls through cleanly, and the pre-L3 seam behavior is unchanged.
;;
;; This file documents the gate; the actual gate is a test-runner invocation
;; (see e-live-loop-3-steps.edn :cmd), not a standalone script — the test
;; namespaces need the :test alias's extra-paths to be on the classpath.
(ns gate-l3-scheduled-seam
  {:clj-kondo/config {:linters {:unused-namespace {:level :off}}}})
