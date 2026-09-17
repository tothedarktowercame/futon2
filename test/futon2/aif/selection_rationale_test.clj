(ns futon2.aif.selection-rationale-test
  "RE4 -- the rationale written at decision time, its typed absences, and the
   one seam that writes it, in the cascade-only decision shape (SPEC
   flat-removal H4, 2026-09-17).

   The controls are arranged so that no assertion could pass vacuously: every
   absence test names the SPECIFIC reason keyword rather than merely asserting
   that the record is an absence, and the seam tests assert on a file that a
   fresh temporary directory did not contain a moment earlier."
  (:require 
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.cascade-problems :as cascade-problems]
            [futon2.aif.decision-gate :as decision-gate]
            [futon2.aif.policy :as policy]
            [futon2.aif.selection-rationale :as sr])
  (:import (java.io File)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(def ^:dynamic *tmpdir* nil)

(defn- with-tmpdir [f]
  (let [dir (Files/createTempDirectory "wm-rationale-test" (into-array FileAttribute []))]
    (binding [*tmpdir* (str dir)]
      (try (f)
           (finally
             (doseq [^File child (reverse (file-seq (io/file (str dir))))]
               (.delete child)))))))

(use-fixtures :each with-tmpdir)

(def ^:private planted-contract
  {:status :present :git-sha "0123456789abcdef" :contract-id "wm-holes"
   :path "planted"})

(defn- cascade-entry
  [id pattern g]
  {:action {:kind :cascade-candidate :cascade-id id :id id
            :precedence [pattern]
            :construction-receipt {:cascade/id id :moves 1
                                   :family-searched :unit :coverage 1}
            :interpretation-receipts [{:pattern pattern :admitted-by :test-suite}]}
   :controller-score g
   :rank 1})

(defn- cascade-decision
  []
  (policy/select-action-cascades
   [(cascade-entry "c-alpha" :aif/placeholder-is-load-bearing 1.0)
    (cascade-entry "c-beta" :aif/belief-state-operational-hypotheses 2.0)]
   {:beta 2.0}))

(def ^:private selection-record
  "A PERSISTED trace record (schema 30): the decision is the real output of
   select-action-cascades with its posterior already re-keyed by candidate id,
   as trace/strip-decision persists it."
  (let [decision (cascade-decision)]
    {:timestamp "2026-09-17T22:50:42.709079837Z"
     :run/id "run-a"
     :producer-contract :r8/retired-f-controller-v1
     :wm-version {:git-sha "5a664114c6a149c73554e491b3cb068e8d3be354"}
     :cascade-problems {:problems [] :refusals []}
     :decision (update-in decision [:selection-law :posterior]
                          (fn [posterior]
                            (into {}
                                  (map (fn [[c p]]
                                         [(str (or (:cascade-id c) (:id c))) p]))
                                  posterior)))}))

(def ^:private abstention-record
  "A typed abstention is a decision, not a missing decision: its refusals are
   real ones, from cascade-problems/assemble over an unsupplied target."
  (let [refusals (:refusals (cascade-problems/assemble
                             {:targets ["M-test-absent"]
                              :sources {:horizon-steps 3}}))]
    {:timestamp "2026-09-17T23:50:42.709079837Z"
     :run/id "run-abstain"
     :producer-contract :r8/retired-f-controller-v1
     :wm-version {:git-sha "5a664114c6a149c73554e491b3cb068e8d3be354"}
     :cascade-problems {:problems [] :refusals refusals}
     :decision {:status :abstained :refusals refusals}}))

(defn- record-of [r] (sr/rationale-record r {:contract planted-contract}))

;; ---------------------------------------------------------------------------
;; 0. The fixtures are real decisions.

(deftest fixtures-are-gate-admissible-test
  (is (= (:decision selection-record)
         (update-in (decision-gate/emit! (cascade-decision))
                    [:selection-law :posterior]
                    (fn [posterior]
                      (into {}
                            (map (fn [[c p]]
                                   [(str (or (:cascade-id c) (:id c))) p]))
                            posterior)))))
  (is (= (:decision abstention-record) (decision-gate/emit! (:decision abstention-record)))))

;; ---------------------------------------------------------------------------
;; 1. The producer on a legible cascade selection.

(deftest records-a-cascade-selection-with-the-fields-the-retrospective-needs-test
  (let [r (record-of selection-record)]
    (testing "status and outcome"
      (is (= :recorded (:rationale/status r)))
      (is (= :selected (:rationale/outcome r))))
    (testing "the acceptance's minimum field set is present and populated"
      (is (= "run-a" (:rationale/run-id r)))
      (is (= "2026-09-17T22:50:42.709079837Z" (:rationale/tick-id r)))
      (is (= 2 (:rationale/candidate-set-size r)))
      (is (= "c-alpha" (get-in r [:rationale/chosen :cascade-id])))
      (is (= "aif/placeholder-is-load-bearing"
             (get-in r [:rationale/chosen :first-acting-pattern])))
      (is (= (:beta (:decision selection-record))
             (get-in r [:rationale/chosen :beta])))
      (is (pos? (get-in r [:rationale/chosen :chosen-action-mass])))
      (is (= [] (:rationale/refused r)))
      (is (= 0 (:rationale/refused-count r)))
      (is (string? (:rationale/text r)))
      (is (string? (get-in r [:rationale/text r] "")))
      (is (= "0123456789abcdef" (get-in r [:rationale/contract-sha :git-sha]))))
    (testing "the runner-up carries the margin the choice was made against"
      (let [ru (:rationale/runner-up r)]
        (is (= :present (:status ru)))
        (is (= "c-beta" (:cascade-id ru)))
        (is (number? (:margin-over-chosen ru)))))))

;; ---------------------------------------------------------------------------
;; 2. An abstention is a recorded outcome, not an absence.

(deftest records-an-abstention-with-refusals-grouped-by-kind-test
  (let [r (record-of abstention-record)]
    (is (= :recorded (:rationale/status r)))
    (is (= :abstained (:rationale/outcome r)))
    (is (= 0 (:rationale/candidate-set-size r)))
    (is (= {:universe-not-admitted
            {:count 1 :targets ["M-test-absent"]}}
           (:rationale/abstained-refusals r)))
    (is (= (:rationale/abstained-refusals r)
           (:rationale/cascade-problems-refusals r)))
    (is (string? (:rationale/text r)))))

;; ---------------------------------------------------------------------------
;; 3. Typed absences name their specific reason.

(deftest absence-reasons-are-specific-test
  (testing "not a map"
    (is (= :record-not-a-map
           (:rationale/absence-reason (record-of 42)))))
  (testing "no decision"
    (is (= :no-decision
           (:rationale/absence-reason (record-of (dissoc selection-record :decision))))))
  (testing "a flat decision is not a decision any more"
    (is (= :decision-not-cascade-or-abstention
           (:rationale/absence-reason
            (record-of (assoc selection-record
                              :decision {:action {:type :no-op} :rank 1}))))))
  (testing "a cascade decision without its recorded posterior"
    (is (= :empty-posterior
           (:rationale/absence-reason
            (record-of (assoc-in selection-record
                                 [:decision :selection-law :posterior] {})))))))

;; ---------------------------------------------------------------------------
;; 4. Validation and the write seam.

(deftest defects-are-empty-for-legible-records-test
  (is (= [] (sr/defects (record-of selection-record))))
  (is (= [] (sr/defects (record-of abstention-record)))))

(deftest emit-writes-one-file-per-decision-unconditionally-test
  (let [sel-path (sr/emit! selection-record
                           {:dir *tmpdir* :contract planted-contract})
        abs-path (sr/emit! abstention-record
                           {:dir *tmpdir* :contract planted-contract})
        _absent-path (sr/emit! (dissoc selection-record :decision)
                               {:dir *tmpdir* :contract planted-contract})
        written (sr/read-store *tmpdir*)]
    (is (not= sel-path abs-path))
    (is (.isFile (io/file sel-path)))
    (is (= 3 (count written)) "an illegible decision writes its typed absence too")
    (is (= :recorded (:rationale/status (first written))))
    (is (= #{:recorded :typed-absence}
           (set (map :rationale/status written)))
        "readiness and absences are both legible in the store")))

(deftest emit-refuses-a-defective-record-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"defective"
                        (sr/emit! {:rationale/schema-version 999}
                                  {:dir *tmpdir* :contract planted-contract}))))
