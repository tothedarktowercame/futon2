#!/usr/bin/env bb
;;;
;;; z1_views_test.clj — tests for Z1 views (M-zaif-harness).
;;;
;;; Each test runs the view fns from z1_views.clj against the LIVE store
;;; server on :7073 (read-only GETs only). Verifies:
;;;   - each view returns well-formed EDN with the required keys
;;;   - limit is respected (never unbounded)
;;;   - text-search recheck demonstrated (one candidate re-fetched + verified)
;;;
;;; Usage (from any cwd):
;;;   bb <path-to>/z1_views_test.clj
;;;
;;; Exits 0 on all-pass, 1 on any failure.

(ns z1-views-test
  {:clj-kondo/config {:linters {:unresolved-namespace {:level :off}}}}
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]))

;; Load the views — resolve path relative to THIS file's location
(def this-dir (str (fs/parent (fs/absolutize *file*))))
(load-file (str (fs/path this-dir "z1_views.clj")))

;; Bring the view fns into test scope
(def operator-turns #'z1-views/operator-turns)
(def gamma-events #'z1-views/gamma-events)
(def mission-attributed #'z1-views/mission-attributed)
(def text-search-raw #'z1-views/text-search-raw)
(def fetch-by-id #'z1-views/fetch-by-id)
(def api-base #'z1-views/api-base)
(def mission-status #'z1-views/mission-status)
(def parse-doc-header-status #'z1-views/parse-doc-header-status)
(def mission-doc-path #'z1-views/mission-doc-path)
(def classify-status-text #'z1-views/classify-status-text)

;; ---------------------------------------------------------------------------
;; Test constants
;; ---------------------------------------------------------------------------

(def test-limit 5)
(def test-since "2026-07-12T00:00:00Z")  ; recent window for speed

;; ---------------------------------------------------------------------------
;; Tests
;; ---------------------------------------------------------------------------

(deftest operator-turns-well-formed
  (testing "operator-turns returns well-formed EDN with required keys"
    (let [result (operator-turns {:since test-since :limit test-limit})]
      (is (map? result) "result is a map")
      (is (= :operator-turns (:view result)) ":view is :operator-turns")
      (is (map? (:query result)) ":query is a map with provenance")
      (is (contains? (:query result) :endpoint) ":query has :endpoint")
      (is (contains? (:query result) :params) ":query has :params")
      (is (contains? (:query result) :paging) ":query has :paging")
      (is (string? (:as-of result)) ":as-of is a string (ISO timestamp)")
      (is (vector? (:results result)) ":results is a vector")
      (is (<= (count (:results result)) test-limit)
          "results count respects limit")
      (doseq [r (:results result)]
        (is (contains? r :id) "each result has :id")
        (is (contains? r :at) "each result has :at")
        (is (contains? r :text) "each result has :text")))))

(deftest operator-turns-limit-respected
  (testing "operator-turns ALWAYS passes limit (never unbounded)"
    (let [result (operator-turns {:limit 2})]
      (is (<= (count (:results result)) 2) "limit=2 enforced")
      (is (= 2 (get-in result [:query :paging :limit]))
          "limit recorded in provenance"))))

(deftest gamma-events-well-formed
  (testing "gamma-events returns well-formed EDN, handles empty honestly"
    (let [result (gamma-events {:since test-since :limit test-limit})]
      (is (map? result) "result is a map")
      (is (= :gamma-events (:view result)) ":view is :gamma-events")
      (is (map? (:query result)) ":query is a map")
      (is (string? (:as-of result)) ":as-of is a string")
      (is (vector? (:results result)) ":results is a vector")
      ;; Mode must be :tagged or :fallback-scan
      (is (#{:tagged :fallback-scan} (:mode result))
          ":mode is :tagged or :fallback-scan")
      (when (= :fallback-scan (:mode result))
        (is (contains? (:query result) :fallback)
            "fallback mode documents the fallback in :query")))))

(deftest mission-attributed-well-formed
  (testing "mission-attributed returns well-formed EDN grouped per mission"
    (let [result (mission-attributed {:since test-since :limit test-limit})]
      (is (map? result) "result is a map")
      (is (= :mission-attributed (:view result)) ":view is :mission-attributed")
      (is (map? (:query result)) ":query is a map")
      (is (string? (:as-of result)) ":as-of is a string")
      (is (vector? (:results result)) ":results is a vector")
      (is (map? (:summary result)) ":summary is a map")
      ;; Each result is a mission group
      (doseq [mg (:results result)]
        (is (string? (:mission mg)) "each group has :mission string")
        (is (pos-int? (:count mg)) "each group has positive :count")
        (is (vector? (:turns mg)) "each group has :turns vector")
        (is (= (:count mg) (count (:turns mg)))
            "count matches turns length"))
      ;; Counts should sum to total-attributed-turns
      (when (seq (:results result))
        (is (= (:total-attributed-turns (:summary result))
               (reduce + (map :count (:results result))))
            "turn counts sum to summary total")))))

(deftest mission-attributed-limit-respected
  (testing "mission-attributed ALWAYS passes limit (never unbounded)"
    (let [result (mission-attributed {:limit 3})]
      (is (<= (:total-attributed-turns (:summary result)) 3)
          "limit=3 enforced on total turns scanned")
      (is (= 3 (get-in result [:query :paging :limit]))
          "limit recorded in provenance"))))

(deftest text-search-recheck
  (testing "text-search is a candidate generator; membership re-checked against store"
    ;; Step 1: use text-search to get candidates
    (let [search-result (text-search-raw "correction" 5)]
      (is (map? search-result) "text-search returns a map")
      (is (contains? search-result :results) "has :results")
      (is (contains? search-result :count) "has :count")
      (when (seq (:results search-result))
        ;; Step 2: re-fetch ONE candidate by ID from the store
        (let [candidate (first (:results search-result))
              entry (:entry candidate)
              eid (:evidence/id entry)]
          (is (string? eid) "candidate has an evidence/id")
          (let [refetched (fetch-by-id eid)]
            (is (some? refetched) "candidate re-fetched from store by ID")
            (is (= eid (:evidence/id refetched))
                "re-fetched entry has the same ID")
            ;; Step 3: verify the search term appears in the re-fetched body
            ;; (membership re-check — the store is the authority, not BM25)
            (let [body-str (pr-str (:evidence/body refetched))]
              (is (str/includes? (str/lower-case body-str) "correction")
                  "search term confirmed in re-fetched entry body"))))))))

(deftest read-only-verification
  (testing "all queries are GET (read-only) — provenance records the endpoint"
    (let [r1 (operator-turns {:limit 1})
          r2 (gamma-events {:limit 1})
          r3 (mission-attributed {:limit 1})]
      (doseq [r [r1 r2 r3]]
        (is (str/starts-with? (:endpoint (:query r)) "http://127.0.0.1:7073")
            "endpoint is the local store server")
        (is (str/ends-with? (:endpoint (:query r)) "/api/alpha/evidence")
            "endpoint is the evidence API path"))
      ;; mission-status uses text-search candidate generation
      (let [r4 (mission-status {:mission "M-mission-conditional-reward" :limit 1})]
        (is (str/starts-with? (:endpoint (:query r4)) "http://127.0.0.1:7073")
            "mission-status endpoint is the local store server")))))

;; ---------------------------------------------------------------------------
;; View 4: mission-status tests
;; ---------------------------------------------------------------------------

(def target-mission "M-mission-conditional-reward")

(deftest mission-doc-path-resolves
  (testing "mission-doc-path resolves the futon2 holes doc"
    (let [p (mission-doc-path target-mission)]
      (is (some? p) "path found")
      (is (str/includes? (str p) "M-mission-conditional-reward.md")
          "correct filename"))))

(deftest doc-header-current-state
  (testing "current doc header reads CLOSED"
    (let [h (parse-doc-header-status (mission-doc-path target-mission))]
      (is (some? h) "header found")
      (is (= :closed (:keyword h)) "keyword is :closed")
      (is (str/includes? (:status h) "CLOSED") "status string contains CLOSED"))))

(deftest doc-header-prefix-state
  (testing "pre-fix doc header (d9f38f3^) reads DRAFT — the staleness regression"
    ;; The pre-fix state is extracted via: git show d9f38f3^:holes/M-mission-conditional-reward.md
    ;; nil path returns nil
    (is (nil? (parse-doc-header-status nil)) "nil path returns nil")
    ;; Test with a temp file
    (let [tmp (str (fs/create-temp-file) ".md")
          _ (spit tmp "# M-test\n\n**Status: DRAFT 2026-07-12\n")
          h (parse-doc-header-status tmp)]
      (is (= :draft (:keyword h)) "DRAFT parsed correctly")
      (fs/delete tmp))))

(deftest classify-status-keywords
  (testing "classify-status-text maps keywords correctly"
    (is (= :closed (classify-status-text "M-test CLOSED yesterday" "M-test")))
    (is (= :draft (classify-status-text "M-test is in DRAFT" "M-test")))
    (is (= :complete (classify-status-text "M-test COMPLETE" "M-test")))
    (is (nil? (classify-status-text "M-test is great" "M-test")))))

(deftest mission-status-well-formed
  (testing "mission-status returns well-formed EDN with required keys"
    (let [result (mission-status {:mission target-mission :limit 5})]
      (is (map? result) "result is a map")
      (is (= :mission-status (:view result)) ":view is :mission-status")
      (is (= target-mission (:mission result)) "mission id preserved")
      (is (map? (:query result)) ":query is a map with provenance")
      (is (contains? (:query result) :endpoint) ":query has :endpoint")
      (is (vector? (:results result)) ":results is a vector")
      ;; The doc header must be found
      (is (map? (:doc-header result)) ":doc-header is a map")
      (is (contains? (:doc-header result) :keyword) ":doc-header has :keyword")
      ;; Derived status must be :closed (the answer to acceptance criterion a)
      (is (= :closed (:derived-status result))
          "derived-status is :closed from events alone")
      ;; :stale-header? must be false on the CURRENT doc (they agree)
      (is (false? (:stale-header? result))
          "stale-header? is false when doc and events agree"))))

(deftest mission-status-stale-detection
  (testing ":stale-header? fires on pre-fix doc state (acceptance criterion b)"
    ;; Use a temp file with the pre-fix DRAFT header
    (let [tmp (str (fs/create-temp-file) ".md")
          _ (spit tmp "# M-mission-conditional-reward\n\n**Status: DRAFT 2026-07-12\n")
          result (mission-status {:mission target-mission :limit 5 :doc-path tmp})]
      (is (= :closed (:derived-status result))
          "events still say :closed")
      (is (= :draft (get-in result [:doc-header :keyword]))
          "doc header says :draft (pre-fix state)")
      (is (true? (:stale-header? result))
          ":stale-header? fires when events say closed but doc says draft")
      (fs/delete tmp))))

;; ---------------------------------------------------------------------------
;; Run
;; ---------------------------------------------------------------------------

(defn -main [& _args]
  (let [results (t/run-tests 'z1-views-test)]
    (println "\n========================================")
    (println (format "Tests: %d, Assertions: %d, Failures: %d, Errors: %d"
                     (:test results) (+ (:pass results) (:fail results) (:error results))
                     (:fail results) (:error results)))
    (println "========================================")
    (if (zero? (+ (:fail results) (:error results)))
      (do (println "ALL TESTS PASSED") (System/exit 0))
      (do (println "TEST FAILURES") (System/exit 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
