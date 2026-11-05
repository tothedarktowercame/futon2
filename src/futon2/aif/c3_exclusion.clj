(ns futon2.aif.c3-exclusion
  "Pure C3 exclusion row corresponding to RefusalLegality.nextField."
  (:require [clojure.string :as str]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.outer-refusal-history :as history]))

(defn- outcome [record]
  (or (get-in record [:terminal-receipt :outcome])
      (get-in record [:failure :detail :outcome])
      (:outcome record)
      (get-in record [:q6 :terminal :outcome :outcome])))

(defn- harm-match [record]
  (or (:harm-match record) (get-in record [:failure :detail :harm-match])))

(defn exclusion-row [record]
  (let [target (history/record-target record)
        match (harm-match record)
        sha (get-in record [:effective-run-identity :mission-source-sha256])]
    (cond
      (not= :harm-declined (outcome record))
      {:status :refused :reason :not-c3}

      (not (map? match))
      {:status :refused :reason :harm-match-absent}

      (or (not (str/starts-with? (str target) "M-")) (nil? sha))
      {:status :refused :reason :digest-carrier-absent}

      :else
      {:status :row
       :row {:schema :wm/c3-exclusion-v1
             :target target
             :evidence-digest (identity/digest {:mission-source-sha256 sha})
             :rule (:rule match)
             :declined-action-digest (:declined-action-digest match)
             :run-id (:run/id record)}})))
