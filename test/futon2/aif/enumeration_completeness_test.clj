(ns futon2.aif.enumeration-completeness-test
  "U37: the enumeration-completeness check, exercised against a code root built
   for the test rather than against ~/code.

   Every test here plants a defect and requires the check to name it. The
   defect the row exists for is the four-mission whitelist
   (`holes/NOTE-the-whitelist-provenance.md`): a producer whose domain
   contracted, recorded nowhere."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.enumeration-completeness :as ec])
  (:import (java.io File)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(def ^:dynamic *root* nil)

(defn- with-tmp-root [f]
  (let [dir (Files/createTempDirectory "u37-root" (into-array FileAttribute []))]
    (binding [*root* (str dir)]
      (try (f)
           (finally
             (doseq [^File child (reverse (file-seq (io/file (str dir))))]
               (.delete child)))))))

(use-fixtures :each with-tmp-root)

(defn- mission!
  "Write a mission doc into REPO under the tmp root and return its id."
  [repo id status]
  (let [f (io/file *root* repo "holes" "missions" (str id ".md"))]
    (io/make-parents f)
    (spit f (str "# " id "\n\nStatus: " status "\n\n- [ ] something to do\n"))
    id))

(defn- candidate [id]
  {:action {:type :advance-mission :target id}})

(defn- mission-compare [candidates]
  (ec/compare-kind (ec/scan-population :mission *root*) candidates))

(deftest full-enumeration-is-complete-test
  (testing "a tick that enumerated every live mission is :complete, both
            directions of the membership diff empty"
    (let [ids [(mission! "repo-a" "M-one" "ACTIVE")
               (mission! "repo-a" "M-two" "OPEN")
               (mission! "repo-b" "M-three" "IDENTIFY drafted; MAP pending")]
          c (mission-compare (map candidate ids))]
      (is (= :complete (:verdict c)))
      (is (= 3 (:available-count c) (:enumerated-count c)))
      (is (= [] (:missing c)) "nothing available went unenumerated")
      (is (= [] (:phantom c)) "nothing enumerated is absent from disk"))))

(deftest planted-whitelist-is-refused-test
  (testing "THE INCIDENT: a producer narrowed to four of the available missions
            fails, and the missing ones are named -- a shorter candidate list
            must not read as a smaller world"
    (let [ids (mapv #(mission! "repo-a" (str "M-" %) "ACTIVE") (range 10))
          whitelisted (take 4 ids)
          c (mission-compare (map candidate whitelisted))]
      (is (= :incomplete (:verdict c)))
      (is (= 10 (:available-count c)))
      (is (= 4 (:enumerated-count c)))
      (is (= 6 (count (:missing c))))
      (is (= (set (drop 4 ids)) (set (:missing c)))
          "the check names which six were lost, not just how many"))))

(deftest single-lost-mission-is-refused-test
  (testing "there is no tolerance below which a silent loss passes: losing one
            of ten fails exactly as four-of-ten does"
    (let [ids (mapv #(mission! "repo-a" (str "M-" %) "ACTIVE") (range 10))
          c (mission-compare (map candidate (butlast ids)))]
      (is (= :incomplete (:verdict c)))
      (is (= [(last ids)] (:missing c))))))

(deftest phantom-candidate-is-refused-test
  (testing "a candidate for a mission with no doc is a phantom, not coverage"
    (let [ids [(mission! "repo-a" "M-real" "ACTIVE")]
          c (mission-compare (map candidate (conj ids "M-invented")))]
      (is (= :incomplete (:verdict c)))
      (is (= ["M-invented"] (:phantom c)))
      (is (= [] (:missing c))
          "the available mission was enumerated; the failure is the extra one"))))

(deftest exclusions-carry-typed-reasons-test
  (testing "every doc the scan withholds carries a reason, and the reasons are
            the declared ones -- silence is not a filter"
    (mission! "repo-a" "M-live" "ACTIVE")
    (mission! "repo-a" "M-done" "COMPLETE")
    (mission! "repo-a" "M-gone" "SUPERSEDED-AS-MISSION by M-live")
    (mission! "repo-a" "M-sketch" "DRAFT")
    (mission! "repo-a" "M-live.handoff" "ACTIVE")
    (spit (doto (io/file *root* "repo-a" "holes" "missions" "notes.md")
            io/make-parents)
          "not a mission doc")
    (let [scan (ec/scan-population :mission *root*)]
      (is (= ["M-live"] (:available-ids scan)))
      (is (= {:derived-id 1 :non-contract-filename 1
              :status-draft 1 :status-terminal 2}
             (into {} (:exclusions-by-reason scan))))
      (is (every? :reason (:excluded scan))
          "no excluded file is dropped without a reason")
      (is (= (+ (:available (:counts scan)) (:excluded (:counts scan)))
             (:files-under-dir (:counts scan)))
          "the ledger accounts for every file under the directory"))))

(deftest duplicate-checkouts-resolve-to-the-shortest-path-test
  (testing "a sibling checkout duplicating an id contributes the duplicate, not
            a second candidate -- and the primary copy is the one kept"
    (mission! "repo" "M-shared" "ACTIVE")
    (mission! "repo-worktree-copy" "M-shared" "ACTIVE")
    (let [scan (ec/scan-population :mission *root*)]
      (is (= ["M-shared"] (:available-ids scan)))
      (is (= {"repo" 1} (:available-by-repo scan))
          "the surviving copy is the shorter path, i.e. the primary checkout")
      (is (= [:duplicate-id] (map :reason (:excluded scan)))))))

(deftest leading-token-decides-status-test
  (testing "mission Status lines describe per-phase progress, so a mid-line
            'complete' must not retire a live mission"
    (is (= :live (:status-class (ec/classify-doc-status
                                 "# M\n\nStatus: HEAD complete; IDENTIFY drafted\n"))))
    (is (= :terminal (:status-class (ec/classify-doc-status
                                     "# M\n\nStatus: COMPLETE (all phases)\n"))))
    (is (= :terminal (:status-class (ec/classify-doc-status
                                     "# M\n\n**Status:** SUPERSEDED-AS-MISSION\n"))))
    (is (= :draft (:status-class (ec/classify-doc-status
                                  "# M\n\nStatus: specified, not yet implemented\n"))))
    (is (= :live (:status-class (ec/classify-doc-status "# M\n\nno status line\n")))
        "an unreadable or absent status keeps the item in the queue")))

(deftest kind-without-a-proposer-is-a-typed-absence-test
  (testing "excursions have no proposer, so their whole population
            is outside the selector's view -- typed, with the pointer to the
            proposer list that omits them, and never counted as agreement"
    (spit (doto (io/file *root* "repo" "holes" "excursions" "E-one.md")
            io/make-parents)
          "# E-one\n")
    (let [c (ec/compare-kind (ec/scan-population :excursion *root*) [])]
      (is (= :kind-not-enumerated (:verdict c)))
      (is (= :no-proposer-for-kind (:reason c)))
      (is (= 1 (:available-count c)))
      (is (= 0 (:enumerated-count c)))
      (is (= ec/proposer-list-pointer (:pointer (:enumerator c)))
          "the absence points at the code that would have to change"))))

(deftest record-verdict-ignores-unenumerated-kinds-test
  (testing "a kind with no proposer can neither green nor red the record: it is
            carried in :typed-kind-absences where a reader counts it"
    (mission! "repo" "M-live" "ACTIVE")
    (spit (doto (io/file *root* "repo" "holes" "tickets" "T-one.md")
            io/make-parents)
          "# T-one\n")
    (let [r (ec/completeness-record [(candidate "M-live") {:action {:type :advance-ticket :target "T-one"}}] {:code-root *root*})]
      (is (= :complete (:verdict r))
          "the one kind that HAS a proposer agrees with its scan")
      (is (= #{:excursion} (set (map :kind (:typed-kind-absences r)))))
      (is (= :complete (some #(when (= :ticket (:kind %)) (:verdict %))
                             (:kinds r)))
          "tickets now participate in completeness"))))

(deftest assertion-flag-defaults-off-test
  (testing "the live assertion is opt-in: nothing scans unless the flag is set"
    (is (= (= "1" (System/getenv "FUTON_WM_ENUMERATION_ASSERT"))
           ec/*enumeration-assert?*)
        "the var is the environment read, not a hardcoded default")))
