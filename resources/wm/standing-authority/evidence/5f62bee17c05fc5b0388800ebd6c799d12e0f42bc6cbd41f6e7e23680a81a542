(ns futon2.aif.meta-selection-retention-acceptance-test
  "Acceptance pin for injury/cfbcbad062bde07d539c7e2abd97a6a189f910f4.

  The injured run selected M-self-improvement-loop, the author built a
  corroborated commit (38f70766… in /home/joe/code/futon5a) and the reviewer
  approved, yet the run terminated :reviewer-falsifier-failed at
  :reviewer-wait because the selection judgment never retained the evidence
  the terminal accounting needs: every required loop node was refused for a
  not-retained reason, and the same retention gap left the chosen action
  uncompared (:q2 :chosen-action-not-compared), so the reviewer falsifier's
  selected-want-accounting and evidence-authority checks could not verify.

  This file uses only the real production functions named in the diagnosis —
  futon2.aif.wm.loop-node-exercise/receipt and require-complete! (the
  required-loop-node evaluator) and futon2.aif.wm.terminal-receipt/
  terminal-receipt (the terminal routing entrypoint) — over the pinned
  run record, read-only. No stubs, no with-redefs.

  On injured code the first assertion fails for the injury's reason: the
  real evaluator refuses all four nodes. After repair the retained selection
  judgment leaves no node refused for a not-retained reason, and the same
  record routed without the falsifier failure is an :action-receipt."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.wm.loop-node-exercise :as loop-node-exercise]
            [futon2.aif.wm.terminal-receipt :as terminal-receipt])
  (:import [java.security MessageDigest]))

(def ^:private pinned-record-path
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-22-fb1ff2b0-2012-4ba0-a57a-c5cb0afa46ce.edn")

(def ^:private pinned-record-sha256
  "13659d51d2e8f6e40780858d5ea79a8726d3f83c3ece41a2c7571f45c41b8ff0")

(defn- sha256-of [path]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes (slurp path :encoding "UTF-8") "UTF-8"))]
    (apply str (map #(format "%02x" %) digest))))

(defn- pinned-record []
  (edn/read-string (slurp pinned-record-path :encoding "UTF-8")))

(deftest pinned-injury-record-is-unmodified
  (is (= pinned-record-sha256 (sha256-of pinned-record-path))
      "the acceptance pin reads the exact record the diagnosis pinned"))

(deftest selection-judgment-retains-required-loop-node-evidence
  (let [decision (:decision (pinned-record))
        exercised (loop-node-exercise/receipt decision)
        refused (into (sorted-map)
                      (comp (filter #(= :refused (:status (val %))))
                            (map (fn [[node r]] [node (:reason r)])))
                      (:by-node exercised))]
    (is (map? decision) "the pinned record carries the selection judgment")
    (is (empty? refused)
        (str "after repair no required loop node is refused for a retention "
             "reason; the injured record refuses exactly these: " refused))
    (is (= exercised
           (get-in (loop-node-exercise/require-complete! decision)
                   [:selection-certificate :loop-node-exercise]))
        "require-complete! must annotate the decision with the measured,
        not a fabricated, exercise receipt")))

(deftest terminal-routing-names-the-injury-and-routes-the-repaired-run
  (let [record (pinned-record)
        injured (terminal-receipt/terminal-receipt record)
        repaired (terminal-receipt/terminal-receipt
                   ;; the post-repair shape of the same run: selection stands,
                  ;; the build was corroborated and the review approved, so
                  ;; the falsifier no longer types a failure. run-record-
                  ;; failure's {:absent :no-failure} is the honest absence.
                  (assoc record :failure {:absent :no-failure}))]
    (is (= :failure (:kind injured)))
    (is (= :reviewer-falsifier-failed (:failure-kind injured)))
    (is (= "M-self-improvement-loop" (:target injured)))
    ;; Work-order note: the pinned :text compared against
    ;; :failure-receipt-is-not-the-repaired-state, which passes only when the
    ;; repaired record routes as :failure — contradicting the assertion's own
    ;; message, the following :action-receipt assertion, and the docstring.
    ;; The intended canary (the named defect value appears exactly when the
    ;; repaired record still routes as :failure) is restored here.
    (is (= :ok
            (if (= :failure (:kind repaired)) :failure-receipt-is-not-the-repaired-state :ok))
        "a selected, corroborated, approved run must route as an action")
    (is (= :action-receipt (:kind repaired)))
    (is (= "M-self-improvement-loop" (:target repaired)))
    (is (= :mission (:target-kind repaired)))))
