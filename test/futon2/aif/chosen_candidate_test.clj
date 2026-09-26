(ns futon2.aif.chosen-candidate-test
  "WM-CHOSEN-CANDIDATE-I: the SELECTION LAW's candidate reaches the enactment
  step, not the chosen action's id.

  The two coincide when an action's id is its cascade-id — the seventh
  flight's :C1, and every decision candidate-derivations admits, since it
  refuses :action-id-not-in-candidates — so the defect is invisible on every
  record written so far. futon3c
  wm_wire_r9_candidate_enact_test/a-cascade-id-unlike-the-action-id-fails-the-wire
  is the case where they differ: with :cascade-id :cas/b and action
  :id :act/other the enactment received :act/other. This is Clause C's wire
  (the enactment is the chosen candidate).

  Nothing is flown here: the run record of test 1 is read from disk, and the
  rest is fixture data through the same three functions the flight uses."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.full-loop-runner :as runner])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-dir [] (str (.toFile (Files/createTempDirectory "chosen" (make-array FileAttribute 0)))))

(defn- sha256-file [path]
  (let [bs (Files/readAllBytes (.toPath (io/file path)))]
    (apply str (map #(format "%02x" (bit-and 0xff %))
                    (.digest (java.security.MessageDigest/getInstance "SHA-256") bs)))))

;; ---------------------------------------------------------------------------
;; 1. The live pin: the seventh flight's tick run record, verbatim.

(def live-run-record
  {:path "/home/joe/code/futon3c/holes/labs/M-wm-wiring/spike/tick-run-record-2026-09-26-flight-278b6988-click-1.edn"
   :sha256 "f634b05c8020472aed90eb3c0333226788264142f572b62b301bf84aee8c6dfa"
   :why (str "the seventh flight (M-autoclock-in), the only record whose decision "
             "carries a selection law with a :candidate; its action id and its "
             "cascade-id are both :C1, so this change does not move it")})

(deftest the-live-run-record-is-unmoved-by-this-change
  (is (= (:sha256 live-run-record) (sha256-file (:path live-run-record)))
      "the pin is the file at this sha, not whatever is at that path")
  (let [record (edn/read-string {:default (fn [_ v] v)} (slurp (:path live-run-record)))
        summary (fr/record-summary "M-autoclock-in" "click-1" record)]
    (testing "on this record the law's candidate and the chosen summary agree"
      (is (= :C1 (get-in record [:decision :selection-law :candidate])))
      (is (= :C1 (get-in record [:decision :chosen :candidate]))))
    (testing "record-summary passes that candidate through unchanged"
      (is (= :C1 (get-in summary [:chosen :candidate]))))
    (testing "and it carries no :id: this record was written before the change"
      (is (not (contains? (get-in record [:decision :chosen]) :id))))))

;; ---------------------------------------------------------------------------
;; 2. The bad case from the wire test: a cascade-id unlike the action id.

(def interps {:p/b {:produces #{:t/b}}})

(defn- enacted
  "CHOSEN through record-click and enact-fn; the enactment record."
  [chosen]
  (let [f (flight/record-click
           (flight/start {:target "M-t" :chosen-because {:kind :requested}}
                         {:kind :a-exits :repo "futon3c" :path "p" :read-text (fn [& _] "")}
                         {:id "flight-chosen"})
           (cond-> {:click-id "click-1" :wants [:t/b] :before {} :after {}}
             chosen (assoc :chosen chosen)))
        out ((fr/enact-fn {:dispatch-step! (fn [s] {:commit "c1"
                                                    :produced (first (get-in s [:interpretation :produces]))
                                                    :check {:class :fixture}})
                           :check-fn (fn [_] {:observed true})
                           :interpretations (constantly interps)
                           :fetch-run-record (constantly {:run-id "click-1"})
                           :record-dir (temp-dir)})
             f (last (:clicks f)))]
    (or (:enactment out) out)))

(defn- summary-for
  "The chosen summary a decision with ACTION-ID and LAW-CANDIDATE produces,
  through the run record's writer and the flight's reader."
  [action-id law-candidate]
  (let [decision (cond-> {:action {:id action-id :cascade-id law-candidate :target "M-t"
                                   :precedence [{:id :p/b}]
                                   :construction-receipt {:kind :fixture}}}
                   law-candidate (assoc :selection-law {:candidate law-candidate}))
        record {:decision {:chosen (runner/chosen-summary decision)}}]
    (get (fr/record-summary "M-t" "click-1" record) :chosen)))

(deftest the-selection-laws-candidate-reaches-the-enactment
  (testing "cascade-id :cas/b, action :id :act/other — the wire test's bad case"
    (let [chosen (summary-for :act/other :cas/b)]
      (is (= :cas/b (:candidate chosen)) "the law's candidate is on the summary")
      (is (= :act/other (:id chosen)) "the action's id is beside it, not instead of it")
      (let [e (enacted chosen)]
        (is (= :cas/b (:decision-candidate e))
            "Clause C's join reads the DECISION's candidate")
        (is (= :act/other (:candidate e))
            "the enactment's own :candidate stays the action id: it is policy-key-for's lookup into :candidate-derivations")
        (is (= [:p/b] (mapv :pattern (:attempts e)))
            "the enactment ran the candidate's patterns, unaffected by either id"))))
  (testing "action id = cascade-id (the seventh flight's shape): both read :C1"
    (let [chosen (summary-for :C1 :C1)
          e (enacted chosen)]
      (is (= :C1 (:candidate chosen)))
      (is (= :C1 (:id chosen)))
      (is (= :C1 (:decision-candidate e)))
      (is (= :C1 (:candidate e))))))

;; ---------------------------------------------------------------------------
;; 3. No candidate on the record: the typed absence, never the action id.

(deftest no-candidate-on-the-record-is-a-typed-absence
  (testing "a decision whose selection law names no candidate omits the key"
    (let [chosen (summary-for :act/other nil)]
      (is (not (contains? chosen :candidate))
          "chosen-summary omits it rather than putting the action id there")
      (is (= :act/other (:id chosen)))))
  (testing "the enactment records the absence and still enacts"
    (let [e (enacted (summary-for :act/other nil))]
      (is (= {:absent :no-candidate-on-run-record} (:decision-candidate e)))
      (is (not= :act/other (:decision-candidate e))
          "the action id must not read as the decision's candidate")
      (is (= :act/other (:candidate e)))
      (is (= 1 (count (:attempts e))) "no refusal: the enactment still runs")))
  (testing "a summary naming no action at all is still :no-decision"
    (is (= :no-decision (:absent (enacted nil))))
    (is (= :no-decision (:absent (enacted {:precedence [:p/b]}))))))

;; ---------------------------------------------------------------------------
;; 4. The bad case, planted: if the reader went back to the action id.

(deftest the-check-would-catch-the-defect-it-names
  (let [chosen (summary-for :act/other :cas/b)]
    (is (not= (:id chosen) (:candidate chosen))
        "the fixture really does separate the two ids; if it did not, every assertion above would pass under the old code")))
