(ns futon2.aif.ticket-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.action-proposer :as ap]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.controller-authority :as authority]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.report.cascade-lane :as cascade]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest ticket-status-table
  ;; Actual leading status phrases from the 34-ticket census at 8dd24499.
  (doseq [[text expected]
          [["DONE-IN-FACT, widened." :complete]
           ["SUPERSEDED." :inactive] ["DEFERRED / parked 2026-06-10." :inactive]
           ["PARKED" :inactive] ["ARCHIVED" :inactive]
           ["WATCH — not a defect, no action" :not-actionable]
           ["FINDING, not a defect. Do not hotfix." :not-actionable]
           ["DESIGN CONSTRAINT. Blocked on a prior question" :not-actionable]
           ["DESIGNED, NOT STARTED. Joe's call: ticket it rather than deep-dive" :not-actionable]
           ["PARTIAL -- shipped, one contract delta." :live]
           ["STILL-OPEN, deferred by design." :live]
           ["** open. Diagnosis complete and reduced" :live]
           ["SCOPED, NOT STARTED." :live] ["RECLASSIFY CANDIDATE." :live]
           ["Unrecognised lead" :live]]]
    (is (= expected (registry/classify-ticket-status text)))))

(defn fixture-root []
  (.toFile (Files/createTempDirectory "ticket-registry-" (make-array FileAttribute 0))))
(defn write-ticket [root path text]
  (let [f (io/file root path)] (io/make-parents f) (spit f text) f))

(deftest ticket-root-fences-and-documents
  (let [root (fixture-root)
        live-file (write-ticket root "futon3c/holes/tickets/T-live.md"
                                "# Fix retained delivery\n\n**Status (triaged 2026-09-01): PARTIAL -- still work.**\nParent: M-parent\n")]
    (write-ticket root "futon3c/holes/tickets/T-done.md" "# Done\n**Status: DONE-IN-FACT.**")
    (write-ticket root "futon3c/holes/T-note.md" "# Not a ticket\n**Status: OPEN**")
    (write-ticket root "futon3c-index-check/holes/tickets/T-copy.md" "# Copy\n**Status: OPEN**")
    (write-ticket root "futon3c/holes/tickets/nested/T-nested.md" "# Nested\n**Status: OPEN**")
    (let [doc (registry/load-tickets (.getPath root))
          tickets (:tickets doc)
          state {:tickets tickets}
          actions (vec (ap/propose registry/ticket-enumerator-proposer state))
          live (first (filter registry/live-ticket? tickets))]
      (is (= #{"T-live" "T-done"} (set (map :id tickets))))
      (is (= :ticket (:kind live)))
      (is (= "M-parent" (:parent live)))
      (is (= ["T-live"] (mapv :target actions)))
      (is (fm/can-propose? state :advance-ticket))
      (is (fm/can-execute? state (first actions)))
      (is (false? (fm/can-execute? state {:type :advance-ticket :target "T-done"})))
      (with-redefs [registry/load-tickets (constantly doc)]
        (is (= {:open? true :open-hole-count 1} (registry/work-target-status "T-live")))
        (is (false? (:open? (registry/work-target-status "T-done"))))
        (is (= (.getPath live-file) (:path (#'runner/mission-for-decision
                                          {:action (first actions)} "T-live"))))
        (cascade/clear-psi-cache!)
        (let [psi (cascade/mission->psi "T-live")]
          (is (.contains psi "Fix retained delivery"))
          (is (.contains psi "PARTIAL")))
        (let [decision {:action (first actions) :controller-score 0.2
                        :selection-law {:applied :controller-head}}]
          (is (true? (:actuation-authorized? (authority/authorize decision [{:action (first actions)}]))))
        (is (= :target-not-open
               (try (authority/authorize {:action {:type :advance-ticket :target "T-done"}} [])
                    (catch clojure.lang.ExceptionInfo e (:reason (ex-data e))))))))))

)

(deftest excluded-statuses-never-propose-or-execute
  (doseq [status [:complete :inactive :not-actionable]]
    (let [state {:tickets [{:id "T-held" :status-class status}]}]
      (is (empty? (ap/propose registry/ticket-enumerator-proposer state)))
      (is (false? (fm/can-propose? state :advance-ticket)))
      (is (false? (fm/can-execute? state {:type :advance-ticket :target "T-held"}))))))

(deftest unstated-close-condition-excludes-from-selection
  ;; SELECT-FLAT-2: a ticket whose close condition is still the census
  ;; generator's placeholder is not eligible for selection; a hand-written
  ;; close condition, or no heading at all, stays eligible.
  (let [root (fixture-root)
        ;; The placeholder wrapped exactly as failure-tickets.py writes it
        ;; (copied verbatim from futon3c/holes/tickets/
        ;; T-fail-operator-cancelled.md lines 25-28; only the wrapping the
        ;; generator itself uses -- different wrapping must still match).
        placeholder-close
        (str "# T-fail-placeholder\n\n"
             "**Status (triaged 2026-09-01): RECLASSIFY CANDIDATE.**\n\n"
             "<!-- census:begin -->\n- **class**: `x`\n<!-- census:end -->\n\n"
             "## Why this file exists\n\nGenerated.\n\n"
             "## What would close this\n\n"
             "(unstated -- until it is, this class has no promotion test and cannot reach\n"
             "SELECT)\n\n"
             "## Completion criteria\n\n- The decision is taken\n")
        ;; A hand-written close condition under the same heading (the shape
        ;; futon3c/holes/tickets/T-fail-invoke-error.md has carried since the
        ;; 2026-11-07 triage): stays eligible even though it mentions
        ;; "stated" and SELECT-era vocabulary.
        hand-written-close
        (str "# T-fail-stated\n\n"
             "**Status (triaged 2026-09-01): STILL-OPEN.**\n\n"
             "## What would close this\n\n"
             "Split into subclasses and exempt the provider tail (stated from the\n"
             "2026-11-07 triage above): the provider empty-response count stops\n"
             "growing once exempt.\n\n"
             "## Completion criteria\n\n- Subclasses agreed\n")
        no-heading
        "# T-plain\n\n**Status: PARTIAL -- still work.**\n"]
    (write-ticket root "futon3c/holes/tickets/T-fail-placeholder.md" placeholder-close)
    (write-ticket root "futon3c/holes/tickets/T-fail-stated.md" hand-written-close)
    (write-ticket root "futon3c/holes/tickets/T-plain.md" no-heading)
    (let [tickets (:tickets (registry/load-tickets (.getPath root)))
          by-id (into {} (map (juxt :id identity) tickets))
          state {:tickets tickets}
          actions (vec (ap/propose registry/ticket-enumerator-proposer state))]
      ;; the placeholder ticket is flagged and excluded from liveness...
      (is (true? (:close-condition-unstated (by-id "T-fail-placeholder"))))
      (is (false? (registry/live-ticket? (by-id "T-fail-placeholder"))))
      (is (false? (registry/live-ticket-target? tickets "T-fail-placeholder")))
      ;; ...the hand-written and no-heading tickets stay eligible
      (is (false? (:close-condition-unstated (by-id "T-fail-stated"))))
      (is (false? (:close-condition-unstated (by-id "T-plain"))))
      (is (registry/live-ticket? (by-id "T-fail-stated")))
      (is (registry/live-ticket? (by-id "T-plain")))
      ;; the enumerator proposes only the eligible pair, and executing an
      ;; action against the excluded ticket is refused
      (is (= ["T-fail-stated" "T-plain"] (sort (map :target actions))))
      (is (false? (fm/can-execute? state {:type :advance-ticket :target "T-fail-placeholder"})))
      (is (fm/can-execute? state {:type :advance-ticket :target "T-fail-stated"})))))

(deftest ticket-prior-and-observation-channel
  (let [f (fm/advance-mission-ordinal-factor 1)
        prediction (#'fm/predict-effects {} {:type :advance-ticket :target "T-live" :weight 1.0})]
    (is (= {:mission-health (* 0.04 f)} (:obs-delta prediction)))
    (is (= {:mission-health 0.015} (:obs-variance prediction)))
    (is (= [{:entity-id "T-live" :type :addressed :weight f}] (:events prediction))))
  (let [open (wm/scan-mission-triage 1 [] [{:status-class :live}])
        done (wm/scan-mission-triage 1 [] [{:status-class :complete}])]
    (is (= 1 (:ticket-count open)))
    (is (= 0 (:blocked open) (:abandoned-count open) (:active open)))
    (is (< (:health open) (:health done)))))
