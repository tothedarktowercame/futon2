(ns futon2.aif.meta-acceptance-injury-57c88b37-author-response-test
  "Acceptance for the historical misclassification of injury/57c88b37."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.meta-terminal-injury :as terminal]
            [futon2.aif.occurrence-square-authoring :as square-authoring])
  (:import (java.nio.file Files Paths)
           (java.security MessageDigest)))

(def record-pin
  {:path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-05-c226e5de-f597-45a0-b420-3219db50de94.edn"
   :sha256 "52dc5f8ae5033b401038339bd45f439828f6d3ca039831a28db200c5a40e9cc8"})

(def author-response-pin
  {:path "/home/joe/notes/wm-next/tests/occ195-author-response.txt"
   :sha256 "2f2ba87dad8d1ced9e8c15a81d265c61a0ddbe1de3b688e024a2f62b40d55e71"})

(defn- sha256-bytes [bytes]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn- read-pinned-bytes [{:keys [path sha256]}]
  (let [bytes (Files/readAllBytes (Paths/get path (make-array String 0)))]
    (is (= sha256 (sha256-bytes bytes)) (str "historical pin changed: " path))
    bytes))

(deftest malformed-occurrence-square-answer-is-an-explicit-non-injury
  (let [record (edn/read-string (String. (read-pinned-bytes record-pin) "UTF-8"))
        response (String. (read-pinned-bytes author-response-pin) "UTF-8")
        exchange (handoff/exchange
                  {:contact (fn [_] {:state "done" :job-id "invoke-1793853933302-12828-7ec2dcd1"
                                     :result response})
                   :request {:kind :occurrence-square-authoring}
                   :decode #'square-authoring/decode})
        reason (:reason exchange)
        reconstructed (assoc-in record
                                [:failure :detail :occurrence-square-authoring :answer]
                                {:reason reason :job-id (:job-id exchange)})
        classification (terminal/classify reconstructed record-pin)]
    (testing "the retained author response is genuinely malformed"
      (is (nil? (handoff/response-json-object response)))
      (is (re-find #"Unexpected character.*expecting double-quote"
                   (handoff/response-parse-error response)))
      (is (= :response-invalid reason))
      (is (contains? #{:response-invalid :response-decode-threw
                       :response-validation-threw} reason)))
    (testing "the malformed author response is not a machine injury"
      (is (= :non-injury (:classification classification)))
      (is (= :occurrence-square-author-response-invalid
             (:non-injury-reason classification)))
      (is (= :occurrence-square-answer-invalid
             (:failure-kind classification))))
    (testing "the immutable record alone does not prove an author refusal"
      (is (= :injury (:classification (terminal/classify record record-pin)))))))
