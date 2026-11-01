(ns wm-check-run-facts-test
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [wm-check-run-facts :as sut]))

(def reader-path "/home/joe/code/mathlib4/DarkTower/WarMachine/RequirementsReader.lean")

;; `scripts/wm_run_facts.clj --run` output for the 2026-11-01 record ac5bc477,
;; without `sources`.  Regenerate it rather than editing it.
(def live-export
  (json/parse-string (slurp "test/fixtures/wm-run-facts/ac5bc477-export.json")))

(defn- rational [n d] {"numerator" n "denominator" d})

(def selection-facts
  {"kind" "selection"
   "enumeratedTasks" ["t1" "t2"]
   "currentChoice" {"target" "t1" "cascade" "c1"}
   "targetConstruction" [{"targets" ["t1" "t2"] "action" "a" "reading" "r" "library" "l"
                          "graph" "g" "kernel" "k" "slice" ["p1"] "pool" ["p1"]
                          "sliceFromWholeLibrary" true "policyCount" 2
                          "policies" ["c1" "c2"]}]
   "libraryPatternCount" 19 "constructorPatternCount" 1
   "comparedPolicies" ["c1" "c2"] "constructedCascades" ["c1" "c2"]
   "policyG" [{"policy" "c1" "g" (rational 29949 10000)}
              {"policy" "c2" "g" (rational -1 2)}]
   "abstentionG" (rational 47875 10000)
   "policyPrediction" [{"policy" "c1" "digest" "d1"} {"policy" "c2" "digest" 0}]
   "pathAbsenceCount" 0
   "selectionPath" {"receipt" {"observed" "h1"} "pickRelation" {"observed" 0}
                    "pickReach" "defaulted" "pendingRun" ["run0" "absent"]
                    "consumedRuns" [["run0" {"observed" "h2"}]]
                    "learningApplied" true}})

(deftest partial-run-facts-fields-come-from-lean
  (testing "obsolete generator fields are absent when Lean removes them"
    (let [reader "structure PartialRunFacts where\n  q1 : Evidence RunFacts\n  q3 : Evidence RunFacts\n\ndef assess := true\n"]
      (is (= [:q1 :q3] (sut/partial-run-fact-fields reader)))
      (is (not-any? #{:q2 :q8 :q10}
                    (sut/partial-run-fact-fields reader)))))
  (testing "the current reader has a field per requirement"
    (is (= [:q1 :q2 :q3 :q4 :q5 :q6 :q7 :q9 :q11]
           (sut/partial-run-fact-fields (slurp reader-path))))))

(deftest unknown-lean-field-fails-before-generation
  (testing "a new Lean field without a generator clause cannot be skipped"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"no generator clause"
         (sut/lean-source {"facts" {}} "exported" [:q12]))))
  (testing "it does not fire against the current reader"
    (is (string? (sut/lean-source live-export "exported"
                                  (sut/partial-run-fact-fields (slurp reader-path)))))))

(deftest live-record-prints-every-outcome
  (let [source (sut/lean-source live-export "exported"
                                (sut/partial-run-fact-fields (slurp reader-path)))]
    (is (str/includes? source "#eval outcomes exported"))
    (is (not (str/includes? source "#eval alert")))
    (testing "this record has no target construction, so Q2 cannot be evaluated"
      (is (str/includes? source "q2 := .nr \"not recomputable: targetConstruction,constructorPatternCount\"")))
    (is (str/includes? source "q11 := .nr"))
    (testing "a work run's Q7 needs no selection path"
      (is (re-find #"q7 := \.known \{ Requirements\.click20 with pathAbsenceCount := 5, kind := \.work, selectionPath := none \}"
                   source)))))

(deftest selection-run-renders-its-typed-path
  (let [q7 (sut/evidence selection-facts :q7)]
    (is (str/includes? q7 "kind := .selection"))
    (is (str/includes? q7 "receipt := .observed "))
    (testing "an explicit digest of 0 stays the recorded absence"
      (is (str/includes? q7 "pickRelation := .observed 0,")))
    (is (str/includes? q7 "pickReach := .defaulted"))
    (is (re-find #"pendingRun := some \(\d+, \.absent\)" q7))
    (is (re-find #"consumedRuns := \[\(\d+, \.observed \d+\)\]" q7))
    (is (str/includes? q7 "learningApplied := true"))))

(deftest selection-run-without-a-path-is-not-recomputable
  (is (str/starts-with? (sut/evidence (dissoc selection-facts "selectionPath") :q7) ".nr"))
  (is (str/starts-with? (sut/evidence (assoc selection-facts "selectionPath" nil) :q7) ".nr")))

(deftest a-path-on-a-work-run-reaches-lean
  (is (re-find #"kind := \.work, selectionPath := some "
               (sut/evidence (assoc selection-facts "kind" "work") :q7))))

(deftest unknown-names-render-as-no-constructor
  (is (str/includes? (sut/evidence (assoc selection-facts "kind" "outer") :q2)
                     "kind := .unknownRunKind"))
  (is (str/includes? (sut/evidence (assoc-in selection-facts ["selectionPath" "receipt"] "tolerated") :q7)
                     "receipt := .unknownProvenance")))

(deftest q11-renders-exact-rationals
  (let [q11 (sut/evidence selection-facts :q11)]
    (is (str/starts-with? q11 ".known"))
    (is (str/includes? q11 "(29949 / 10000)"))
    (is (str/includes? q11 "(-1 / 2)"))
    (is (str/includes? q11 "abstentionG := (47875 / 10000)"))
    (testing "a prediction digest of 0 stays the recorded absence"
      (is (re-find #"policyPrediction := \[\(\d+, \d+\), \(\d+, 0\)\]" q11)))))

(deftest q11-refuses-anything-but-an-exact-rational
  (let [nr ".nr \"G not projected at a declared scale\""]
    (is (= nr (sut/evidence (assoc selection-facts "abstentionG" 4.7875) :q11)))
    (is (= nr (sut/evidence (assoc selection-facts "abstentionG" (rational 5 0)) :q11)))
    (is (= nr (sut/evidence (assoc-in selection-facts ["policyG" 0 "g"] 2.9949) :q11)))))

(deftest not-recomputable-inside-a-construction-is-not-hashed-into-ids
  (let [facts (assoc-in selection-facts ["targetConstruction" 0 "slice"]
                        {"not-recomputable" "slice absent"})]
    (is (str/starts-with? (sut/evidence facts :q2) ".nr"))
    (is (str/starts-with? (sut/evidence facts :q11) ".nr"))))
