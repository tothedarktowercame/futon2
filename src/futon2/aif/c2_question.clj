(ns futon2.aif.c2-question
  "Pure checked carrier for RefusalLegality C2 (legitimateQuestion).

  The mission-reading validator remains the authority for what constitutes a
  cited ambiguity. This namespace pins the source bytes and gives the checked
  question a canonical digest; it performs no I/O."
  (:require [futon2.aif.action-identity :as identity]
            [futon2.aif.mission-reading :as mission-reading]))

(def schema :wm/c2-question-v1)

(defn- question-value [question]
  (select-keys question [:question :span :alternatives]))

(defn- digest-value [question source-sha]
  {:question (:question question)
   :span (:span question)
   :alternatives (:alternatives question)
   :source-sha256 source-sha})

(defn- question-refusal [question source-bytes]
  (some-> (mission-reading/question-reasons [question] source-bytes)
          first
          :reason))

(defn question-carrier
  "Return a checked C2 question carrier or a typed refusal.

  SOURCE-PIN is the retained `{:repo :path :sha256}` identity of SOURCE-BYTES.
  READING-QUESTION has mission-reading's checked question shape."
  [target source-bytes source-pin reading-question reading-receipt]
  (let [actual-sha (identity/sha256 source-bytes)
        expected-sha (:sha256 source-pin)
        question (question-value reading-question)
        refusal (question-refusal question source-bytes)]
    (cond
      (not= actual-sha expected-sha)
      {:status :refused :reason :stale-citation}

      refusal
      {:status :refused :reason refusal}

      :else
      {:status :carrier
       :carrier
       {:schema schema
        :target target
        :question (:question question)
        :ambiguity {:source (select-keys source-pin [:repo :path :sha256])
                    :span (:span question)
                    :alternatives (:alternatives question)}
        :reading-receipt reading-receipt
        :question-digest (identity/digest
                          (digest-value question expected-sha))}})))

(defn ambiguity-cited?
  "Recheck a stored carrier against SOURCE-BYTES, including its source pin and
  canonical question digest."
  [carrier source-bytes]
  (let [source (get-in carrier [:ambiguity :source])
        question {:question (:question carrier)
                  :span (get-in carrier [:ambiguity :span])
                  :alternatives (get-in carrier [:ambiguity :alternatives])}
        source-sha (:sha256 source)]
    (and (= schema (:schema carrier))
         (= source-sha (identity/sha256 source-bytes))
         (nil? (question-refusal question source-bytes))
         (= (:question-digest carrier)
            (identity/digest (digest-value question source-sha))))))
