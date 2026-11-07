(ns futon2.aif.stop-line-abstention-test
  "REPAIR-ABSTAIN-1: a legal abstention with its recorded reason requires no
  stop-line finding; an unexplained or illegal abstention still requires one.

  The abstention record shape is copied from the retained selection the
  runtime writes on the legal empty-field path (commit 345ebdfb9 and the C3
  extension), as pinned end-to-end by
  futon2.aif.outer-selection-wiring-test/legal-terminal-exclusions-make-work-exhaustion-reachable:
  an :outcome of {:status :abstained :case :empty-field} and
  :outer-field/excluded rows of {:target ... :reason ... :pin ... :run-id ...}
  as produced by c1-exclusion/field-exclusions."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.stop-line :as stop-line]))

(def ^:private mission-sha "fixture-sha")

(defn- exclusion-row [target reason]
  {:target target
   :reason reason
   ;; The real pin: identity/digest of the target's mission source sha, as
   ;; field-exclusions verified before retaining the exclusion.
   :pin (identity/digest {:mission-source-sha256 mission-sha})
   :run-id (str (case reason
                  :c1-already-satisfied "satisfied-"
                  :c3-harm-declined "harm-"
                  "defect-")
                target)})

(def ^:private legal-empty-field-close
  {:outcome :abstained
   :failure-kind :abstained
   :selection-outcome {:status :abstained :case :empty-field}
   :outer-field/excluded
   [(exclusion-row "M-ready" :c1-already-satisfied)
    (exclusion-row "M-unknown" :c3-harm-declined)]})

(deftest legal-empty-field-abstention-with-evidence-requires-no-finding
  (is (stop-line/no-finding-required? legal-empty-field-close)))

(deftest abstention-without-the-legal-case-still-requires-a-finding
  ;; The abstained close the runner actually opens findings for today: the
  ;; wm judge's typed refusal abstention (finding repair-occ-df511c1b...,
  ;; :no-policy-family/:target-class-required), which retains no empty-field
  ;; selection record at all.
  (is (not (stop-line/no-finding-required?
            {:outcome :abstained :failure-kind :abstained})))
  ;; A selection outcome with any other case is not the legal record,
  ;; whatever the exclusions say.
  (is (not (stop-line/no-finding-required?
            (assoc legal-empty-field-close
                   :selection-outcome
                   {:status :abstained :case :insufficient-comparison}))))
  (is (not (stop-line/no-finding-required?
            (assoc legal-empty-field-close
                   :selection-outcome
                   {:status :abstained :case :all-policies-predict-stopping})))))

(deftest abstention-without-checked-exclusion-evidence-still-requires-a-finding
  ;; No exclusions retained: the abstention is unexplained.
  (is (not (stop-line/no-finding-required?
            (assoc legal-empty-field-close :outer-field/excluded []))))
  ;; An exclusion whose reason is not a legal terminal (missing ingest or
  ;; relation evidence) makes the emptied field a debugger error, not a
  ;; legal abstention.
  (is (not (stop-line/no-finding-required?
            (assoc legal-empty-field-close
                   :outer-field/excluded
                   [(exclusion-row "M-ready" :c1-already-satisfied)
                    (exclusion-row "M-unknown" :relation-missing)]))))
  ;; A case keyword without any exclusion carrier at all is not evidence.
  (is (not (stop-line/no-finding-required?
            (dissoc legal-empty-field-close :outer-field/excluded)))))

(deftest exemption-is-inert-for-non-abstained-outcomes
  ;; The retained record alone must not exempt any other outcome.
  (is (not (stop-line/no-finding-required?
            (assoc legal-empty-field-close :outcome :build-failed)))))
