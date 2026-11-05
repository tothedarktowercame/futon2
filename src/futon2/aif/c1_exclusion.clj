(ns futon2.aif.c1-exclusion
  "Pure persistent C1 exclusion corresponding to RefusalLegality.nextField,
  legal_terminal_excludes, new_evidence_restores, and legal_terminals_bounded.

  Mission evidence is the canonical digest of mission-source SHA alone.
  Policy id and compiled-check-set SHA are retained as provenance but are not
  compared. Accepted cost: changing policy/checks without changing mission
  source does not restore the target. :base-head is deliberately excluded.
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
          sha (:mission-source-sha256 effective)]
      (when (some? sha) {:mission-source-sha256 sha}))))

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
             :policy-id (get-in record [:effective-run-identity :policy-id])
             :compiled-check-set-sha256
             (get-in record [:effective-run-identity :compiled-check-set-sha256])
             :satisfaction-pin (identity/digest satisfaction)
             :run-id (:run/id record)}})))

(defn field-exclusions [rows current-mission-sha-fn field]
  (let [field (set field)]
    (->> rows
         (keep (fn [{:keys [row exclusion-reason]}]
                 (when row
                   (assoc row ::reason (or exclusion-reason
                                           :c1-already-satisfied)))))
         (filter #(and (contains? field (:target %))
                       (some? (current-mission-sha-fn (:target %)))
                       (= (:evidence-digest %)
                          (identity/digest
                           {:mission-source-sha256
                            (current-mission-sha-fn (:target %))}))))
         ;; One entry per target: outer_select refuses evidence whose target
         ;; set and entry count differ. Rows remain in durable index order;
         ;; when C1 and C3 both exclude one target, the first indexed row wins.
         (reduce (fn [acc row]
                   (if (some #(= (:target row) (:target %)) acc)
                     acc
                     (conj acc {:target (:target row)
                                :reason (::reason row)
                                :pin (:evidence-digest row)
                                :run-id (:run-id row)})))
                 []))))
