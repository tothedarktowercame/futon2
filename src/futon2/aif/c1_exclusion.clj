(ns futon2.aif.c1-exclusion
  "Pure persistent C1 exclusion corresponding to RefusalLegality.nextField,
  legal_terminal_excludes, new_evidence_restores, and legal_terminals_bounded.

  Mission evidence is the canonical digest of mission-source SHA, policy id,
  and compiled-check-set SHA from :effective-run-identity. :base-head is
  deliberately excluded because unrelated commits do not change wants/checks.
  Non-mission targets need a canonical retained field-fact/reading digest;
  that carrier is not built here and is refused as :digest-carrier-absent.
  The Lean specification is unchanged."
  (:require [clojure.string :as str]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.meta-terminal-injury :as terminal]
            [futon2.aif.outer-refusal-history :as history]))

(defn- digest-input [record target]
  (when (str/starts-with? (str target) "M-")
    (let [effective (:effective-run-identity record)
          input (select-keys effective
                             [:mission-source-sha256 :policy-id
                              :compiled-check-set-sha256])]
      (when (every? #(some? (get input %))
                    [:mission-source-sha256 :policy-id
                     :compiled-check-set-sha256])
        input))))

(defn exclusion-row [record]
  (let [outcome (or (get-in record [:terminal-receipt :outcome])
                    (get-in record [:failure :detail :outcome])
                    (:outcome record)
                    (get-in record [:q6 :terminal :outcome :outcome]))
        satisfaction (terminal/satisfaction-evidence record)
        target (history/record-target record)
        input (digest-input record target)]
    (cond
      (not (contains? #{:already-satisfied :grounded-no-change} outcome))
      {:status :refused :reason :not-c1}

      (not (true? (:wants-measured satisfaction)))
      {:status :refused :reason :satisfaction-unmeasured}

      (nil? input)
      {:status :refused :reason :digest-carrier-absent}

      :else
      {:status :row
       :row {:schema :wm/c1-exclusion-v1
             :target target
             :evidence-digest (identity/digest input)
             :satisfaction-pin (identity/digest satisfaction)
             :run-id (:run/id record)}})))

(defn forbidden-evidence [rows current-digest-fn field]
  (let [field (set field)]
    (->> rows
         (keep :row)
         (filter #(and (contains? field (:target %))
                       (some? (current-digest-fn (:target %)))
                       (= (:evidence-digest %)
                          (current-digest-fn (:target %)))))
         ;; One entry per target: outer_select refuses forbidden evidence
         ;; whose target set and entry count differ, so a duplicated row
         ;; (a retried close, a re-appended index) would refuse the whole
         ;; selection.  Matching rows for a target share the current digest,
         ;; so which one is kept does not change the pin.
         (reduce (fn [acc row]
                   (if (some #(= (:target row) (:target %)) acc)
                     acc
                     (conj acc {:target (:target row)
                                :reason :c1-already-satisfied
                                :pin (:evidence-digest row)})))
                 []))))
