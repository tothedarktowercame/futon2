(ns futon2.aif.cascade-model-manifest-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [futon2.aif.cascade-model-manifest :as m]))

(def pattern-text "  + IF: ready and not blocked\n  + HOWEVER: stalled\n  + THEN: produce evidence\n  + BECAUSE: test\n")
(def target-text "# Mission: evidence\n\n**Status:** kernel instantiated; contract open\ntrace accepted dark\n\n**Owner:** someone\n")
(deftest negation-and-uninterpreted-controls
  (let [p (m/interpret-pattern "p" "pattern" pattern-text)]
    (is (true? (m/guard-holds? p #{"ready" "stalled"})))
    (is (false? (m/guard-holds? p #{"ready" "stalled" "blocked"})))
    (is (false? (m/guard-holds? p #{"ready"})))
    (is (= :missing (:status (m/compile-clause "without an explicit dial, either thrash or tunnel")))))
  (let [p (m/interpret-pattern "p" "pattern" "  + THEN: evidence\n")]
    (is (= :missing (get-in p [:transition :status])))
    (is (nil? (m/guard-holds? p #{})))
    (is (= :missing (:status (m/transition-row p #{}))))))
(deftest source-extraction-controls
  (let [t (m/extract-target "m" "mission" target-text)]
    (is (contains? (:have t) "kernel"))
    (is (contains? (:have t) "trace"))
    (is (not (contains? (:have t) "contract")))
    (is (contains? (:want t) "contract"))
    (is (some #(= :built-not-live (:classification %)) (:clauses t))))
  (let [t (m/extract-target "stem-only" "absent" "")]
    (is (not (:ok t)))
    (is (= :refused (:status (m/build-manifest t [])))))
  (let [t (m/extract-target "m" "table" (str target-text "\n| Component | Status |\n|---|---|\n| arithmetic | Built; unwired |\n| observer | Candidate |\n"))]
    (is (= :prefer-status-table (:selection-rule t)))
    (is (some #(= :built-not-live (:classification %)) (:clauses t)))
    (is (contains? (:have t) "arithmetic"))
    (is (not (contains? (:have t) "observer")))))
(deftest kernels-and-roundtrip
  (let [t (m/extract-target "m" "mission" target-text)
        p (m/interpret-pattern "p" "pattern" pattern-text)
        manifest (m/build-manifest t [p])]
    (is (m/normalized-exact? (get-in manifest [:initial-belief :mass])))
    (is (m/normalized-exact? (m/transition-row p #{"ready" "racing"})))
    (is (contains? (first (keys (m/transition-row p #{"ready" "racing"}))) "racing"))
    (is (= {1/3 1} (m/observation-row #{"a" "b" "c"} #{"a"})))
    (is (m/normalized-exact? (m/observation-row #{"a" "b" "c"} #{"a"})))
    (is (not (m/normalized-exact? {:x 1.0})))
    (is (= manifest (edn/read-string (pr-str manifest))))
    (is (some #(= :guard-unreachable-in-declared-universe (:kind %)) (:findings manifest)))
    (is (false? (:scoring-permitted? manifest)))))
(deftest actual-mission-shape
  (let [p "holes/missions/M-aif-policy-conditioned-eig.md"
        t (m/extract-target "M-aif-policy-conditioned-eig" p (slurp p))]
    (is (:ok t))
    (is (= :prefer-status-table (:selection-rule t)))
    (is (some #(= :outstanding (:classification %)) (:clauses t)))
    (is (some #(= :unclassified-clause (:kind %)) (:findings t)))))
