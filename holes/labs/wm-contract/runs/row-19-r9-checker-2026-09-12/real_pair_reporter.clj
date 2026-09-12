(ns real-pair-reporter
  (:require [clojure.edn :as edn]
            [futon2.aif.r9-checker :as r9])
  (:import [java.nio.file Files Paths]
           [java.security MessageDigest]
           [java.time Instant]))

(def ledger-path "/tmp/futon3c-invoke-jobs.edn")
(def producer-id "invoke-1789221108452-20365-a7ce9ab4")
(def reviewer-id "invoke-1789228047569-20412-fc188e87")
(def output-path "holes/labs/wm-contract/runs/row-19-r9-checker-2026-09-12/real-pair-readback.edn")

(defn sha256-bytes [bytes]
  (apply str (map #(format "%02x" (bit-and (int %) 0xff))
                  (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn prompt [job]
  (:text (first (filter #(= "prompt" (:type %)) (:events job)))))

(defn safe-job [job]
  (select-keys job [:job-id :agent-id :caller :surface :request-digest
                    :artifact-ref :trace-id :created-at :started-at :finished-at
                    :state :terminal-code :execution :delivery]))

(let [ledger-bytes (Files/readAllBytes (Paths/get ledger-path (make-array String 0)))
      ledger (edn/read-string (String. ledger-bytes "UTF-8"))
      producer (get-in ledger [:jobs producer-id])
      reviewer (get-in ledger [:jobs reviewer-id])
      reviewer-prompt (prompt reviewer)
      checker-bytes (Files/readAllBytes
                     (Paths/get "src/futon2/aif/r9_checker.clj" (make-array String 0)))
      commission {:agent-id (:agent-id reviewer) :prompt reviewer-prompt
                  :caller (:caller reviewer) :surface (:surface reviewer)
                  :model (:invocation/model reviewer)}
      input {:role-binding {:author (:agent-id producer) :reviewer (:agent-id reviewer)}
             :producer-job producer :reviewer-job reviewer
             :subject {:boundary :witness-admission-candidate
                       :artifact-ref (:artifact-ref producer)
                       :digest (:artifact-ref producer)}
             :review-commission commission
             :verification-receipt nil
             :review-receipt nil
             :trace->job (:trace->job ledger)
             :checker-source-sha256 (sha256-bytes checker-bytes)
             :bootstrap-anchor nil
             :admission-at (str (Instant/now))
             :ledger-source {:path ledger-path :sha256 (sha256-bytes ledger-bytes)}}
      outcome (try (r9/check-independence input)
                   (catch clojure.lang.ExceptionInfo e (ex-data e)))
      readback {:schema :wm/r9-real-pair-readback-v1
                :work-remaining/row 19
                :source {:path ledger-path :sha256 (sha256-bytes ledger-bytes)
                         :extracted-at (:admission-at input)}
                :pair {:producer (safe-job producer)
                       :reviewer (safe-job reviewer)
                       :review-commission {:agent-id (:agent-id commission)
                                           :caller (:caller commission)
                                           :surface (:surface commission)
                                           :model (:model commission)
                                           :prompt-sha256 (sha256-bytes (.getBytes reviewer-prompt "UTF-8"))}
                       :prompt :redacted}
                :joins-before-finding
                {:declared-author-to-producer-agent :passed
                 :declared-reviewer-to-reviewer-agent :passed
                 :distinct-seat-identities :passed
                 :artifact-binding :passed
                 :review-request-digest :passed}
                :outcome outcome
                :finding "The real jobs retain enough input to recompute the reviewer request digest, but no structured review receipt names the same verification receipt and reviewer. The real pair therefore refuses before the anchor check; this is a retention/admission gap, not permission to weaken the join."
                :expected-current-checker-state :awaiting-anchor}]
  (spit output-path (str (pr-str readback) "\n"))
  (println (pr-str {:outcome outcome :passed-joins 5
                    :failed-join (:failed-join outcome)})))
