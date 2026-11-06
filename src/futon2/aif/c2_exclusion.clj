(ns futon2.aif.c2-exclusion
  "Persistent C2 exclusion for a checked question published to Morning Brief."
  (:require [clojure.string :as str]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.outer-refusal-history :as history]))

(defn- c2-data [record]
  (or (:c2 record)
      (get-in record [:failure :detail :c2])
      (let [detail (get-in record [:failure :detail])]
        (when (or (:c2/question detail) (:c2/brief-receipt detail))
          {:question (:c2/question detail)
           :brief-receipt (:c2/brief-receipt detail)}))))

(defn exclusion-row [record]
  (let [target (history/record-target record)
        sha (get-in record [:effective-run-identity :mission-source-sha256])
        c2 (c2-data record)
        checked (:question c2)
        carrier (when (= :carrier (:status checked)) (:carrier checked))
        receipt (:brief-receipt c2)
        question-digest (:question-digest carrier)]
    (cond
      (not= :C2 (get-in record [:refusal/disposition :case]))
      {:status :refused :reason :not-c2}
      (or (not (str/starts-with? (str target) "M-")) (not (string? sha)))
      {:status :refused :reason :digest-carrier-absent}
      (not (and (string? question-digest)
                (= question-digest (:question-digest receipt))
                (string? (:path receipt)) (string? (:sha256 receipt))))
      {:status :refused :reason :question-evidence-absent}
      :else
      {:status :row
       :row {:schema :wm/c2-exclusion-v1 :target target
             :evidence-digest (identity/digest {:mission-source-sha256 sha})
             :question-digest question-digest
             :brief-receipt-pin (identity/digest receipt)
             :run-id (:run/id record)}})))
