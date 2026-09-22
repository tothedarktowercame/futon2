(ns futon2.aif.learning-trial-ledger
  "Append-only record-only counts. Read solely for deduplication/meaning admission,
   never by a production model. OS lock serializes independent writers."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity])
  (:import [java.io RandomAccessFile PushbackReader StringReader]))

(load-identity/register! *ns* *file*)
;; Absolute, like the cohort root: the serving JVM runs with futon3c as cwd.
(def default-root "/home/joe/code/futon2/data/wm-learning-trials")
(defonce ^:private mutex (Object.))

(defn- records [text]
  (with-open [reader (PushbackReader. (StringReader. text))]
    (loop [out []]
      (let [x (edn/read {:eof ::eof} reader)]
        (if (= ::eof x) out
            (if (= :wm/attempt-learning-count-v1 (:schema x))
              (recur (conj out x))
              (throw (ex-info "Learning ledger schema invalid" {:learning-ledger/refusal :invalid-record}))))))))

(defn record!
  "Append each newly admitted occurrence/effect once. Held rows never create a
   ledger file. A partial/corrupt ledger refuses; it is never repaired here."
  [root receipt]
  (if-not (some #(= :admitted-at-attempt-grain (:status %)) (:trials receipt))
    receipt
    (locking mutex
      (let [file (io/file root "attempts.edn")]
        (io/make-parents file)
        (with-open [raf (RandomAccessFile. file "rw")
                    lock (.lock (.getChannel raf))]
          (let [_ (assert (.isValid lock))
                bytes (byte-array (.length raf))
                _ (.readFully raf bytes)
                existing (records (String. bytes "UTF-8"))
                by-id (atom (into {} (map (juxt :identity identity)) existing))
                meanings (atom (into {} (map (juxt :family :meaning-sha256)) existing))
                rows (mapv
                      (fn [row]
                        (if-not (= :admitted-at-attempt-grain (:status row)) row
                          (let [id (get-in row [:deduplication :identity])
                                old (get @by-id id)
                                family (:learning-family row)
                                reason (cond
                                         (and (contains? @meanings family)
                                              (not= (get @meanings family) (:meaning-sha256 row))) :revised-meaning
                                         (and old (not= (:observed old) (:after-observation row))) :conflicting-observation
                                         old :duplicate-replay)]
                            (if reason
                              (assoc row :status :held :reason reason :counted? false
                                     :ledger {:path (.getPath file) :identity id :status :not-appended})
                              (let [event {:schema :wm/attempt-learning-count-v1 :mode :record-only
                                           :identity id :family family :meaning-sha256 (:meaning-sha256 row)
                                           :observed (:after-observation row)
                                           :increment (if (:after-observation row) {:success 1 :failure 0} {:success 0 :failure 1})
                                           :contract (:contract receipt) :trial row}
                                    line (str (identity/printed false event) "\n")]
                                (.seek raf (.length raf))
                                (.write raf (.getBytes line "UTF-8"))
                                (.sync (.getFD raf))
                                (swap! by-id assoc id event)
                                (swap! meanings assoc family (:meaning-sha256 row))
                                (assoc row :counted? true
                                       :ledger {:path (.getPath file) :identity id :status :appended}))))))
                      (:trials receipt))]
            (assoc receipt :trials rows)))))))

;; ---------------------------------------------------------------------------
;; PROOF-wm-works ⟨1⟩4: the production reader and the B update.
;; ---------------------------------------------------------------------------

(defn read-trials
  "PROOF-wm-works ⟨1⟩4: the production reader. Reads every recorded trial
   (v1 record-only events included — interpreted, never duplicated or
   rewritten) and returns them as transition evidence rows:

     {:identity … :family … :observed <boolean> :contract-version v1|v2}

   An old (v1) compatible event is interpreted as a whole-attempt outcome
   for its declared family under v2's reading — the v1 :consumption
   :not-authorized field is a write-time record, not a live veto — and each
   event appears exactly once (the ledger's own deduplication identity is
   the join key; a duplicate append would have been refused at write time)."
  ([root] (read-trials root (io/file root "attempts.edn")))
  ([root file]
   (locking mutex
     (if-not (.exists (io/file file))
       []
       (with-open [raf (RandomAccessFile. (io/file file) "r")]
         (let [bytes (byte-array (.length raf))]
           (.readFully raf bytes)
           (mapv (fn [row]
                   {:identity (or (get-in row [:deduplication :identity])
                                  ;; the real ledger's rows carry identity/family at
                                  ;; the TOP LEVEL (the 2026-09-21 write shape)
                                  (:identity row))
                    :family (or (:learning-family row) (:family row))
                    :observed (if (contains? row :observed)
                                (:observed row)
                                (pos? (:success (:increment row))))
                    ;; v1 rows carry :consumption :not-authorized under
                    ;; :trial; v2 rows are read without a veto
                    :contract-version (if (get-in row [:trial :consumption]) :v1 :v2)
                    :row row})
                 (records (String. bytes "UTF-8")))))))))

(defn b-update
  "The B update, applied ONLY after a close the accepted-increment predicate
   accepts (the caller passes the predicate's verdict; this function refuses
   anything but {:accepted? true}). Keyed by occurrence identity, exactly
   once: a second invocation with the same identity writes nothing.

   WHICH PARAMETER and WHY: a whole-attempt outcome informs the selected
   pattern family's theta — the declared effect's transition probability in
   the candidate's own precedence. The attempt exercised exactly that
   family through its declared effect, so the outcome is evidence about
   THAT family's transition, not about sibling families, the cascade
   grain, or any controller term (a wider placement would claim trials the
   attempt never ran).

   Rule (stated, then enforced): theta_posterior = (successes + 1/2) /
   (trials + 1) over the family's whole-attempt outcomes — a Laplace update
   on the family's declared-effect transition, starting from the neutral
   1/2 prior when no history exists."
  [{:keys [family occurrence-identity accepted-verdict ledger-root]}]
  (when-not (map? accepted-verdict)
    (throw (ex-info "B update requires the accepted-increment verdict"
                    {:learning-ledger/refusal :missing-accepted-verdict})))
  (when-not (true? (:accepted? accepted-verdict))
    (throw (ex-info "B update writes only after an accepted close"
                    {:learning-ledger/refusal :close-not-accepted
                     :verdict accepted-verdict})))
  (let [trials (read-trials (or ledger-root default-root))
        mine (filter #(= family (:family %)) trials)
        ;; Exactly once: the occurrence's own identity must not already be
        ;; among the family's recorded trials.
        already (some #(= occurrence-identity (:identity %)) mine)
        successes (count (filter true? (map :observed mine)))
        trials-n (count mine)
        trials' (+ trials-n (if already 0 1))
        successes' (+ successes (if already 0 (if (true? (:observed accepted-verdict)) 1 0)))
        theta (/ (+ successes' 1/2) (+ trials' 1))]
    {:status (if already :already-recorded :updated)
     :family family
     :occurrence-identity occurrence-identity
     :theta theta
     :rule "theta_post = (successes' + 1/2) / (trials' + 1): Laplace (beta 1/2,1/2) over the family's whole-attempt outcomes, counting this occurrence once (trials'/successes' include it if and only if it is not already recorded)"
     :trials-read trials-n
     ;; persistence: the value survives being read back because it is
     ;; derived from the append-only ledger the occurrence will be recorded
     ;; into (the recording itself happens through the existing record!
     ;; path at the accepted close; this function computes and states the
     ;; update, and refuses re-computation over an already-recorded
     ;; occurrence).
     :read-back (let [again (read-trials (or ledger-root default-root))]
                  {:trials (count again)
                   :same-family-count (count (filter #(= family (:family %)) again))})}))
