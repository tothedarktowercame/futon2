(ns futon2.aif.ordinary-author-refusal
  "Restart-safe authority for a historical occurrence that current terminal
  classification proves is an ordinary author refusal, not an apparatus injury."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-terminal-injury :as terminal]))

(def receipt-schema :wm/ordinary-author-refusal-receipt-v1)
(def verifier-id "futon2.aif.ordinary-author-refusal/retained-authority")
(def explicit-author-refusals #{:author-response-unparseable})

(defn pin? [p]
  (and (map? p) (= #{:path :sha256} (set (keys p)))
       (string? (:path p))
       (re-matches #"[0-9a-f]{64}" (or (:sha256 p) ""))))

(defn- resolve-pin [pin]
  (try
    (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file (:path pin))))]
      (when (= (:sha256 pin) (identity/sha256 bytes))
        (let [text (String. bytes "UTF-8")
              value (try (edn/read-string text)
                         (catch Throwable _ (json/parse-string text true)))]
          {:bytes bytes :value value})))
    (catch Throwable _ nil)))

(defn replay-classification
  "Reclassify the retained legacy occurrence under the bounded-correction
  contract introduced by the fix. Only the exact old author-answer-invalid
  occurrence is translated, and only when its retained verbatim answer is
  still undecodable by the current public handoff reader."
  [record record-pin job]
  (when (and (= :open-cascade-author-answer-invalid (get-in record [:failure :kind]))
             (= :cascade-resume (get-in record [:failure :stage]))
             (= "done" (:state job))
             (string? (:result job))
             (nil? (handoff/response-map job)))
    (terminal/classify
     (assoc-in record [:failure :kind] :author-response-unparseable)
     record-pin)))

(defn checked? [receipt candidate]
  (let [record-resolved (resolve-pin (:record-pin receipt))
        job-resolved (resolve-pin (:author-response-pin receipt))
        record (:value record-resolved)
        job-value (:value job-resolved)
        job (or (:job job-value) job-value)
        classification (when (and record job)
                         (replay-classification record (:record-pin receipt) job))
        job-id (:job-id job)
        retained-job-ids (set (keep :job-id
                                    (get-in record [:registered-run/model-usage :jobs])))]
    (and (= receipt-schema (:schema receipt))
         (= verifier-id (:verifier receipt))
         record-resolved job-resolved
         (= candidate (:candidate receipt))
         (= (:record-pin receipt)
            {:path (:original-record-id candidate) :sha256 (:original-pin candidate)})
         (= (:author-response-pin receipt)
            {:path (:author-response-id candidate)
             :sha256 (:author-response-pin candidate)})
         (= (:fix-commit receipt) (:patch-id candidate))
         (= (:fix-commit receipt) (:classifier-revision candidate))
         (re-matches #"[0-9a-f]{40}" (:fix-commit receipt))
         (= (:evidence-pin candidate) (:sha256 (:author-response-pin receipt)))
         (contains? retained-job-ids job-id)
         (string? (:result job))
         (= :non-injury (:classification classification))
         (contains? explicit-author-refusals (:non-injury-reason classification))
         (= (:non-injury-reason classification) (:replay-result receipt))
         (= :ordinary-author-refusal-non-injury (:replay candidate)))))

(defn retained-authority
  [{:keys [operation candidate evidence]}]
  (try
    (let [pin (:authority-receipt-pin operation)
          receipt (:value (resolve-pin pin))]
      (and (= candidate evidence)
           (checked? receipt candidate)))
    (catch Throwable _ false)))
