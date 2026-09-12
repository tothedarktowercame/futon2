(ns futon2.aif.r9-checker
  "Pure R9 identity and evidence-join checker. This module is not integrated at
  any admission boundary; its own admission remains awaiting an operator anchor."
  (:require [clojure.string :as str])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest]
           [java.time Instant]))

(def admission-schema :wm/r9-independence-admission-v1)
(def checker-admission-status :awaiting-anchor)

(defn- refuse! [cause & [data]]
  (throw (ex-info (name cause) (merge {:refusal cause} data))))

(defn- canonical [x]
  (cond
    (map? x) (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
                   (map (fn [[k v]] [k (canonical v)])) x)
    (set? x) (vec (sort-by pr-str (map canonical x)))
    (sequential? x) (mapv canonical x)
    :else x))

(defn- sha256 [s]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes (str s) StandardCharsets/UTF_8))]
    (apply str (map #(format "%02x" (bit-and (int %) 0xff)) digest))))

(defn request-digest
  "Reproduce the Agency invoke request digest from its complete commission."
  [{:keys [agent-id prompt caller surface model]}]
  (binding [*print-namespace-maps* true]
    (sha256 (pr-str (canonical
                     [(cond-> {:agent-id (str agent-id)
                               :prompt (str prompt)
                               :caller (str (or caller "http-caller"))
                               :surface (str (or surface "http"))}
                        model (assoc :model model))])))))

(defn- seat-id [x cause]
  (let [s (some-> x str str/trim)]
    (when (str/blank? s) (refuse! cause))
    s))

(defn- require-job [job cause]
  (when-not (and (map? job) (not (str/blank? (str (:job-id job)))))
    (refuse! cause))
  job)

(defn- join! [ok join & [data]]
  (when-not ok
    (refuse! :r9/unjoinable-producer-reviewer-pair
             (merge {:failed-join join} data))))

(defn- executed-review? [job]
  (let [reported (:execution job)
        event-count (count (filter #(and (= "tool_use" (str (:type %)))
                                         (seq (:tools %)))
                                   (:events job)))]
    (and (or (true? (:executed reported)) (true? (:executed? reported)))
         (pos? (max (long (or (:tool-events reported) 0)) event-count)))))

(defn- before-or-equal? [a b]
  (try (not (.isAfter (Instant/parse (str a)) (Instant/parse (str b))))
       (catch Exception _ false)))

(defn- valid-anchor? [anchor checker-source-sha]
  (and (= :wm/r9-bootstrap-anchor-v1 (:schema anchor))
       (= :anchored (:status anchor))
       (not (str/blank? (str (:authority anchor))))
       (= checker-source-sha (:checker-source-sha256 anchor))))

(defn check-independence
  "Validate the eight R9 joins and emit an admission record. Missing or unequal
  evidence refuses closed. CALLER is retained provenance and never supplies a
  role. Without a valid bootstrap anchor, otherwise-valid evidence refuses
  :r9/anchor-missing."
  [{:keys [role-binding producer-job reviewer-job subject review-commission
           review-receipt verification-receipt trace->job checker-source-sha256
           bootstrap-anchor admission-at ledger-source] :as _input}]
  (let [author (seat-id (get role-binding :author) :r9/producer-identity-missing)
        reviewer (seat-id (get role-binding :reviewer) :r9/reviewer-identity-missing)
        producer-job (require-job producer-job :r9/producer-job-missing)
        reviewer-job (require-job reviewer-job :r9/reviewer-job-missing)
        producer-agent (seat-id (:agent-id producer-job) :r9/producer-identity-missing)
        reviewer-agent (seat-id (:agent-id reviewer-job) :r9/reviewer-identity-missing)]
    (when (= author reviewer) (refuse! :r9/author-equals-reviewer {:identity author}))
    (join! (= author producer-agent) :declared-author-to-producer-agent)
    (join! (= reviewer reviewer-agent) :declared-reviewer-to-reviewer-agent)
    (when-not (= (:artifact-ref producer-job) (:artifact-ref subject))
      (refuse! :r9/artifact-binding-mismatch
               {:expected (:artifact-ref subject) :observed (:artifact-ref producer-job)}))
    (join! (and (map? review-commission)
                (= reviewer (:agent-id review-commission))
                (= (:request-digest reviewer-job) (request-digest review-commission)))
           :review-request-digest)
    (join! (and (map? review-receipt)
                (= reviewer (some-> (:reviewer review-receipt) str))
                (= verification-receipt (:verification review-receipt)))
           :review-receipt-verification)
    (join! (and (not (str/blank? (str (:trace-id producer-job))))
                (= (:job-id producer-job) (get trace->job (:trace-id producer-job))))
           :producer-trace-to-job)
    (join! (and (not (str/blank? (str (:trace-id reviewer-job))))
                (= (:job-id reviewer-job) (get trace->job (:trace-id reviewer-job))))
           :reviewer-trace-to-job)
    (when-not (executed-review? reviewer-job)
      (refuse! :r9/review-execution-missing {:job-id (:job-id reviewer-job)}))
    (join! (and admission-at (:finished-at reviewer-job)
                (before-or-equal? (:finished-at reviewer-job) admission-at))
           :review-precedes-admission)
    (when-not (valid-anchor? bootstrap-anchor checker-source-sha256)
      (refuse! :r9/anchor-missing
               {:checker-source-sha256 checker-source-sha256
                :anchor-status (:status bootstrap-anchor)}))
    {:schema admission-schema
     :boundary (:boundary subject)
     :subject-digest (:digest subject)
     :roles {:author author :reviewer reviewer}
     :producer {:job-id (:job-id producer-job) :trace-id (:trace-id producer-job)}
     :reviewer {:job-id (:job-id reviewer-job) :trace-id (:trace-id reviewer-job)}
     :joins {:declared-author-to-producer-agent :passed
             :declared-reviewer-to-reviewer-agent :passed
             :distinct-seat-identities :passed
             :artifact-binding :passed
             :review-request-digest :passed
             :review-receipt-verification :passed
             :trace-to-job :passed
             :review-execution-and-chronology :passed}
     :ledger-source ledger-source
     :checker-source-sha256 checker-source-sha256
     :bootstrap-anchor bootstrap-anchor
     :decision :admitted
     :at admission-at}))
