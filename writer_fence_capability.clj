(ns writer-fence-capability
  "Receipt-validated writer-fence capability. An identifier is transport, not evidence."
  (:require [cheshire.core :as json]
            [clojure.java.shell :as shell])
  (:import [java.time Instant]
           [java.security MessageDigest]
           [java.math BigInteger]))

(def ^:private capability-token (Object.))
(def checker "/home/joe/code/futon2/checks/writer_fence_evidence.py")

(def ^:dynamic *run-evidence*
  (fn [fence-id attestation-path]
    (shell/sh "python3" checker "--fence-id" fence-id
              "--attestations" attestation-path)))

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
            attested (get-in receipt [:classification :attested])
            attestation (:value attested)
            expires (some-> attestation :expires-at Instant/parse)
            temp (java.io.File/createTempFile "writer-fence-attestation-" ".json")]
        (try
          (spit temp (json/generate-string attestation))
          (let [live (*run-evidence* fence-id (.getAbsolutePath temp))
                live-receipt (try (json/parse-string (:out live) true)
                                  (catch Throwable _ nil))
                problems (cond-> []
                           (not= "FENCE-VERIFIABLE" (:verdict receipt)) (conj :prior-verdict-not-verifiable)
                           (not= fence-id (:fence-id receipt)) (conj :receipt-fence-id-mismatch)
                           (not= fence-id (get-in receipt [:classification :fence-id])) (conj :classification-fence-id-mismatch)
                           (not= "complete" (:status attested)) (conj :attestation-incomplete)
                           (not= fence-id (:fence-id attestation)) (conj :attestation-fence-id-mismatch)
                           (nil? (:observation-interval receipt)) (conj :observation-interval-absent)
                           (or (nil? expires) (.isBefore expires (Instant/now))) (conj :attestation-expired)
                           (not= 0 (:exit live)) (conj :live-fence-check-failed)
                           (not= "FENCE-VERIFIABLE" (:verdict live-receipt)) (conj :live-fence-not-verifiable)
                           (not= fence-id (:fence-id live-receipt)) (conj :live-fence-id-mismatch)
                           (nil? (:observation-interval live-receipt)) (conj :live-observation-interval-absent))
                held? (empty? problems)]
            (cond-> {:schema :writer-fence-capability/v1
                     :verified? held? :status (if held? :observed-held :unverified)
                     :id fence-id :path path :receipt-sha256 (sha256 bytes)
                     :live-verdict (:verdict live-receipt)
                     :live-observation-interval (:observation-interval live-receipt)
                     :problems problems}
              held? (assoc ::token capability-token)))
          (finally (.delete temp))))
      (catch Throwable t
        {:schema :writer-fence-capability/v1
         :verified? false :status :unavailable :id fence-id :path path
         :reason :evidence-unreadable :detail (.getMessage t)}))))

(defn observed-held? [capability]
  (and (= :writer-fence-capability/v1 (:schema capability))
       (= :observed-held (:status capability))
       (:verified? capability)
       (identical? capability-token (::token capability))))

(defn public-view [capability]
  (dissoc capability ::token :verified?))

(defn event-claim [interval moved? capability]
  (let [held? (observed-held? capability)]
    {:claim :event-free
     :interval interval
     :writer-fence (public-view capability)
     :event-free? (cond moved? false held? true :else :unverified)
     :distinguishable-cause? (boolean (or moved? held?))}))
