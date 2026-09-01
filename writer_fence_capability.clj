(ns writer-fence-capability
  "Receipt-validated writer-fence capability. An identifier is transport, not evidence."
  (:require [cheshire.core :as json]
            [clojure.java.shell :as shell])
  (:import [java.time Instant]
           [java.security MessageDigest]
           [java.math BigInteger]))

(def checker "/home/joe/code/futon2/checks/writer_fence_evidence.py")
(def max-receipt-age-seconds 300)

(defn- sha256 [bytes]
  (format "%064x" (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn verify
  "Validate a prior receipt and re-observe the live fence. Returns an in-process
   capability; a caller-supplied map cannot satisfy `observed-held?`."
  [fence-id path]
  (cond
    (and (nil? fence-id) (nil? path))
    {:verified? false :status :absent :reason :not-declared}

    (or (nil? fence-id) (nil? path))
    {:verified? false :status :invalid :reason :id-and-evidence-required-together}

    :else
    (try
      (let [bytes (java.nio.file.Files/readAllBytes
                   (java.nio.file.Paths/get path (make-array String 0)))
            receipt (json/parse-string (String. bytes "UTF-8") true)
            _ (when-not (map? receipt)
                (throw (ex-info "fence receipt must be a JSON object" {})))
            attested (get-in receipt [:classification :attested])
            attestation (:value attested)
            expires (some-> attestation :expires-at Instant/parse)
            prior-start (some-> receipt :observation-interval :started-at Instant/parse)
            prior-finish (some-> receipt :observation-interval :finished-at Instant/parse)
            prior-age (when prior-finish
                        (.getSeconds (java.time.Duration/between prior-finish (Instant/now))))
            temp (java.io.File/createTempFile "writer-fence-attestation-" ".json")]
        (try
          (spit temp (json/generate-string attestation))
          (let [live (shell/sh "python3" checker "--fence-id" fence-id
                               "--attestations" (.getAbsolutePath temp))
                live-receipt (try (json/parse-string (:out live) true)
                                  (catch Throwable _ nil))
                problems (cond-> []
                           (not= "FENCE-VERIFIABLE" (:verdict receipt)) (conj :prior-verdict-not-verifiable)
                           (not= fence-id (:fence-id receipt)) (conj :receipt-fence-id-mismatch)
                           (not= fence-id (get-in receipt [:classification :fence-id])) (conj :classification-fence-id-mismatch)
                           (not= "complete" (:status attested)) (conj :attestation-incomplete)
                           (not= fence-id (:fence-id attestation)) (conj :attestation-fence-id-mismatch)
                           (nil? (:observation-interval receipt)) (conj :observation-interval-absent)
                           (not (seq (get-in receipt [:classification :observed]))) (conj :observed-population-absent)
                           (not (seq (get-in receipt [:classification :observed :start]))) (conj :observed-start-absent)
                           (not (seq (get-in receipt [:classification :observed :finish]))) (conj :observed-finish-absent)
                           (or (nil? prior-start) (nil? prior-finish)
                               (.isAfter prior-start prior-finish)) (conj :prior-observation-interval-invalid)
                           (or (nil? prior-age) (neg? prior-age)
                               (> prior-age max-receipt-age-seconds)) (conj :prior-observation-interval-stale)
                           (or (nil? expires) (.isBefore expires (Instant/now))) (conj :attestation-expired)
                           (not= 0 (:exit live)) (conj :live-fence-check-failed)
                           (not= "FENCE-VERIFIABLE" (:verdict live-receipt)) (conj :live-fence-not-verifiable)
                           (not= fence-id (:fence-id live-receipt)) (conj :live-fence-id-mismatch)
                           (nil? (:observation-interval live-receipt)) (conj :live-observation-interval-absent))
                held? (empty? problems)]
            {:schema :writer-fence-verification/v2
                     :verified? held? :status (if held? :observed-held :unverified)
                     :id fence-id :path path :receipt-sha256 (sha256 bytes)
                     :live-verdict (:verdict live-receipt)
                     :live-observation-interval (:observation-interval live-receipt)
                     :problems problems})
          (finally (.delete temp))))
      (catch Throwable t
        {:schema :writer-fence-verification/v2
         :verified? false :status :unavailable :id fence-id :path path
         :reason :evidence-unreadable :detail (.getMessage t)}))))

(defn assess
  "Sanctioned production API: validates evidence through the fixed subprocess
   and immediately derives the claim. No reusable bearer capability is issued."
  [interval moved? fence-id path]
  (let [verification (verify fence-id path)
        held? (and (= :writer-fence-verification/v2 (:schema verification))
                   (:verified? verification)
                   (= :observed-held (:status verification)))
        public (dissoc verification :verified?)]
    {:claim :event-free
     :interval interval
     :writer-fence public
     :event-free? (cond moved? false held? true :else :unverified)
     :distinguishable-cause? (boolean (or moved? held?))}))
