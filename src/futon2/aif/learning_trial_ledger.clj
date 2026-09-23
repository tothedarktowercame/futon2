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

(defn producer-of
  "The pattern in PRECEDENCE that declares EFFECT among its :produces.
   Exactly one declaring pattern -> that pattern's :id; none or several ->
   a typed status, never a guess.

   The declared consumption grain (attempt-learning-contract v2) is \"the
   selected pattern family's theta (its declared effect's transition
   probability)\", and the same contract's :does-not-establish names
   :individual-pattern-firing. So attribution is BY DECLARATION: the pattern
   that declared the effect, not the pattern that can be shown to have
   produced it. With two declaring patterns the record genuinely does not
   say which fired, so it contributes to nothing and says so (claude-2's
   grain ruling, 2026-09-23)."
  [precedence effect]
  (let [producers (filter #(contains? (set (:produces %)) effect) precedence)]
    (cond
      (= 1 (count producers)) (:id (first producers))
      (empty? producers) {:status :no-declared-producer :effect effect}
      :else {:status :ambiguous-producer :effect effect
             :candidates (mapv :id producers)})))

(defn theta-key
  "The parameter key a recorded trial contributes to: the pattern that
   declared the trial's effect within the precedence that trial recorded.
   Derived from what the event already stores -- the ledger's :family is a
   digest of the whole trial CONFIGURATION (target, cascade, patterns,
   effect, route), which `record!` uses to detect :revised-meaning. That
   digest is not a parameter key: it can only be matched again on an
   identical target and precedence, so a theta stored under it could never
   transfer. Two identities for one thing is what hid this bug, so the key
   is derived here rather than written a second time.

   Both banked row shapes are read: the 2026-09-21 top-level shape as well
   as the :trial-nested one, so a top-level row attributes instead of
   degrading silently to :no-declared-producer (claude-2's review)."
  [row]
  (producer-of (or (get-in row [:trial :selected-cascade :precedence])
                   (get-in row [:selected-cascade :precedence]))
               (or (get-in row [:trial :effect])
                   (:effect row))))

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
                    ;; the parameter key, derived (see theta-key): the
                    ;; :family above is the trial-CONFIGURATION digest
                    :theta-key (theta-key row)
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
  [{:keys [family occurrence occurrence-identity accepted-verdict ledger-root]}]
  (when-not (map? accepted-verdict)
    (throw (ex-info "B update requires the accepted-increment verdict"
                    {:learning-ledger/refusal :missing-accepted-verdict})))
  (when-not (true? (:accepted? accepted-verdict))
    (throw (ex-info "B update writes only after an accepted close"
                    {:learning-ledger/refusal :close-not-accepted
                     :verdict accepted-verdict})))
  (let [trials (read-trials (or ledger-root default-root))
        ;; FAMILY is a pattern id -- the parameter key. It was compared
        ;; against :family, the trial-CONFIGURATION digest, so it never
        ;; matched: trials-n was always 0 and the reported theta was always
        ;; a first-trial value regardless of history (claude-2's review,
        ;; 2026-09-23). The key is :theta-key, derived by producer-of.
        mine (filter #(= family (:theta-key %)) trials)
        ;; one contribution per recorded occurrence, as pattern-theta does:
        ;; two functions whose docstrings name the same posterior must not
        ;; disagree (claude-2's review)
        mine (vals (into {} (map (juxt :identity identity)) mine))
        ;; Exactly once. OCCURRENCE-IDENTITY is a commit sha; the row's
        ;; :identity is a digest of {:occurrence :effect :grain}, so the two
        ;; can never be equal and every occurrence looked new -- it
        ;; double-counted an attempt whose row record! had already banked
        ;; at the close (claude-2's review, 2026-09-23). The occurrence is
        ;; matched as production identifies it: attempt_learning stores the
        ;; dedup key in plain form beside the digest, so the occurrence map
        ;; is compared directly, no digest reconstruction. The commit sha
        ;; stays as recorded provenance; it is not the key.
        occurrence-of (fn [r] (or (get-in r [:row :trial :deduplication :inputs :occurrence])
                                  (get-in r [:row :deduplication :inputs :occurrence])))
        already (boolean (when occurrence
                           (some #(= occurrence (occurrence-of %)) mine)))
        successes (count (filter true? (map :observed mine)))
        trials-n (count mine)
        trials' (+ trials-n (if already 0 1))
        ;; the observation comes from the verdict's own evidence: the
        ;; predicate's accepted shape carries it under
        ;; [:evidence :acceptance-result :observed] (a hand-built shape may
        ;; carry a top-level :observed — both are honoured)
        verdict-observed (or (when (contains? accepted-verdict :observed)
                              (true? (:observed accepted-verdict)))
                            (true? (get-in accepted-verdict
                                           [:evidence :acceptance-result :observed])))
        successes' (+ successes (if already 0 (if verdict-observed 1 0)))
        theta (/ (+ successes' 1/2) (+ trials' 1))]
    {:status (if already :already-recorded :updated)
     :family family
     :occurrence-identity occurrence-identity
     :occurrence occurrence
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
                   ;; by the PARAMETER key: this counted on :family, the
                   ;; configuration digest, so the field whose job is to
                   ;; show the value survives a read-back reported 0
                   ;; forever (claude-2's review)
                   :same-family-count (count (filter #(= family (:theta-key %)) again))})}))

(defn pattern-theta
  "PROOF-wm-works ⟨1⟩8 second half: the scorer's read of the recorded
   trials, keyed by PATTERN-ID -- the grain the kernel applies theta at
   (cascade-model-manifest/with-pattern-theta) and the grain the v2
   contract declares its consumption at. Returns the b-update rule's
   posterior -- (successes + 1/2)/(trials + 1) over the trials that
   attribute to this pattern -- WITH provenance: {:theta … :status
   :recorded-trials :trials-count n :successes n :identities […] :targets
   […]}. No trials: {:status :no-recorded-trials}. An unreadable or
   malformed ledger: {:status :defaulted :reason …} -- NEVER a refusal; the
   caller keeps the documented default with the typed reason recorded.

   :targets is provenance the reviewer needs: trials POOL ACROSS TARGETS,
   which is the claim that reliability is a property of the pattern rather
   than of the pattern-on-this-target. Key it by target instead and every
   key holds one or two trials forever, so nothing is ever learned; but the
   pooling is an assumption, so the contributing targets are recorded and a
   reader can see that a 1/8 came from three attempts on ONE target
   (claude-2, 2026-09-23).

   A pattern declaring several tokens pools its trials across those
   effects: the kernel fires the whole :produces set at once, so theta
   reads as the pattern's per-token delivery rate -- the approximation the
   Lean InterpretedPattern already makes."
  ([pattern-id] (pattern-theta pattern-id default-root))
  ([pattern-id root]
   (try
     (let [all (read-trials root)
           ;; A row whose key is a typed status (no declaring pattern, or
           ;; several) is not the same as an absent row, and the difference
           ;; must be visible: a qualification mismatch at the join would
           ;; otherwise read as silence rather than as "6 rows, 6
           ;; unattributed" (claude-2's answer to the quiet-degradation
           ;; question, 2026-09-23).
           unattributed (count (filter #(map? (:theta-key %)) all))
           rows (filter #(= pattern-id (:theta-key %)) all)
           ;; one contribution per recorded occurrence
           rows (vals (into {} (map (juxt :identity identity)) rows))
           n (count rows)
           successes (count (filter (comp true? :observed) rows))]
       (if (zero? n)
         {:status :no-recorded-trials :unattributed-rows unattributed}
         {:theta (/ (+ successes 1/2) (+ n 1))
          :unattributed-rows unattributed
          :status :recorded-trials
          :trials-count n
          :successes successes
          :identities (vec (sort (keep :identity rows)))
          :targets (vec (distinct (keep #(first (get-in % [:row :trial :effect])) rows)))}))
     (catch Exception e
       {:status :defaulted
        :reason (or (:learning-ledger/refusal (ex-data e))
                    :ledger-unreadable)
        :message (.getMessage e)}))))
