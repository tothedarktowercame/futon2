(ns futon2.aif.learning-trial-ledger
  "Append-only comparison-time trials. Written by record! inside the token
   comparison (before any close verdict); read by pattern-theta and b-update,
   and snapshotted into each close as the B-C carrier (close-b-update).
   OS lock serializes independent writers."
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
  ([_root file]
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

;; ---------------------------------------------------------------------------
;; B-C (PROOF-2 strategy row 34): the concentration carrier recorded at the
;; close. Spec: holes/labs/wm-contract/proof2/packets/B-D.md §4, as revised by
;; reviews/B-D-codex-20.md §3, §6, §7.
;;
;; POPULATION. `record!` runs inside the token comparison
;; (full_loop_runner/retain-token-outcome!), BEFORE the accepted-increment
;; predicate decides anything, and `pattern-theta` reads every appended row
;; with a matching :theta-key. So the population the judge consumes is "rows
;; appended at comparison", not "accepted closes": machinery-71/attempt-002
;; closed with [:accepted-increment :accepted?] = :refused, its row 8e7d1aaf…
;; has [:ledger :status] :appended, and that row is the single trial behind
;; C1's theta 3/4 in tick-run-record 1790199409. The carrier records that
;; population and annotates each row with its close's acceptance status, so a
;; reader can partition consumed trials without recounting.
;; ---------------------------------------------------------------------------

(def carrier-schema :wm/b-update-carrier-v2)

(def carrier-axes
  "O × S: the outcome carrier {achieved, not} at the one (singleton) state."
  {:outcomes [:achieved :not] :states [:singleton]})

(def jeffreys-prior
  "Concentrations as an O×S array, [[a_achieved] [a_not]] over the singleton
   state: the (1/2, 1/2) prior pattern-theta's rule starts from."
  [[1/2] [1/2]])

(defn exact-tree?
  "Every number anywhere in X is an integer or a ratio: no doubles, floats or
   decimals, at any depth, in keys or values."
  [x]
  (cond (number? x) (or (integer? x) (ratio? x))
        (map? x) (every? (fn [[k v]] (and (exact-tree? k) (exact-tree? v))) x)
        (coll? x) (every? exact-tree? x)
        :else true))

(defn carrier-hash
  "Content hash: SHA-256 over the canonical printed form
   (action-identity/canonical: maps and sets unordered, vectors ordered,
   ratios exact, doubles by IEEE hex), prefixed \"sha256:\"."
  [x]
  (str "sha256:" (identity/sha256 (identity/printed false (identity/canonical x)))))

(defn- occurrence-key
  "The three-field occurrence identity a ledger row and a close share. Never
   target/attempt alone: two attempts on one target are two occurrences."
  [m]
  (let [k (select-keys m [:action/id :action/value-sha256 :transition/id])]
    (when (and (= 3 (count k)) (every? some? (vals k))) k)))

(defn row-occurrence
  "The occurrence key a read-trials row was banked under, or nil."
  [r]
  (occurrence-key (or (get-in r [:row :trial :deduplication :inputs :occurrence])
                      (get-in r [:row :deduplication :inputs :occurrence]))))

(defn default-close-roots
  "The cohort data roots beside the ledger root: every wm-full-loop* directory
   under the ledger's parent (data/). A close lives at
   <root>/<cohort>/<attempt>/007-closed.edn."
  [ledger-root]
  (->> (.listFiles (io/file (.getAbsoluteFile (io/file ledger-root)) ".."))
       (filter #(and (.isDirectory ^java.io.File %)
                     (.startsWith (.getName ^java.io.File %) "wm-full-loop")))
       (sort-by str)
       vec))

(defn close-files
  "Read-only discovery of closed checkpoints under ROOTS."
  [roots]
  (vec (sort-by str (for [root roots
                          f (file-seq (io/file root))
                          :when (and (.isFile ^java.io.File f)
                                     (= "007-closed.edn" (.getName ^java.io.File f)))]
                      f))))

(defn- close-status [judgment source]
  (let [v (:accepted-increment judgment)]
    (if (and (map? v) (contains? v :accepted?))
      {:status :recorded
       :accepted? (:accepted? v)
       :reason (if (contains? v :reason)
                 (:reason v)
                 {:status :missing :reason :reason-not-recorded})
       :source source}
      {:status :missing :reason :acceptance-not-recorded :source source})))

(defn annotate-close-statuses
  "Attach each row's close acceptance WITHOUT filtering any row. FILES are
   close records (paths); a file is parsed only if its text mentions one of
   the rows' :action/id values, and a parsed close counts only when all three
   occurrence fields at [:payload :judgment :occurrence] match the row's.
   CURRENT is the judgment being serialized into this very close (its file
   does not exist yet), keyed the same way and sourced :same-close. A row
   whose close is not found, or found twice, gets a typed absence."
  [rows files current current-path]
  (let [needed (set (keep row-occurrence rows))
        ids (set (map :action/id needed))
        entries (reduce
                 (fn [out f]
                   (let [text (slurp f)]
                     (if-not (some #(.contains ^String text ^String %) ids)
                       out
                       (let [event (edn/read-string {:default tagged-literal} text)
                             j (get-in event [:payload :judgment])
                             k (occurrence-key (:occurrence j))]
                         (if (contains? needed k)
                           (update out k (fnil conj [])
                                   (close-status j {:path (str f)
                                                    :raw-sha256 (identity/sha256 text)
                                                    :key-path [:payload :judgment :accepted-increment]}))
                           out)))))
                 {} files)
        current-key (occurrence-key (:occurrence current))
        entries (if (and current-key (contains? needed current-key))
                  (update entries current-key (fnil conj [])
                          (close-status current {:path current-path
                                                 :placement :same-close
                                                 :key-path [:payload :judgment :accepted-increment]}))
                  entries)]
    (mapv (fn [r]
            (let [k (row-occurrence r)
                  matches (get entries k)]
              (assoc r :close-acceptance
                     (cond
                       (nil? k) {:status :missing :reason :occurrence-not-recorded}
                       (= 1 (count matches)) (first matches)
                       (seq matches) {:status :missing :reason :ambiguous-close
                                      :sources (mapv :source matches)}
                       :else {:status :missing :reason :close-not-found
                              :close-files-scanned (count files)}))))
          rows)))

(defn- one-hot [observed] (if observed [1 0] [0 1]))
(defn- outer [o s] (mapv (fn [oi] (mapv #(* oi %) s)) o))
(defn- add-arrays [a b] (mapv #(mapv + %1 %2) a b))

(defn concentration-carrier
  "The B-C carrier for one FAMILY (a pattern id, the :theta-key) over ROWS
   (read-trials output, ledger append order). Exact rationals only.

   Population: every row appended at comparison whose :theta-key is FAMILY,
   regardless of its close's acceptance; each trial carries its
   :close-acceptance (from annotate-close-statuses, or the typed absence
   :close-not-looked-up).

   Read-side dedup collapses repeated :identity values as pattern-theta does
   (last row wins), keeping first-append order; identities whose rows
   disagree on :observed are listed, not hidden.

   :trial-vectors are one-hot over O = [:achieved :not] at the singleton
   state: {:outcome [1 0] | [0 1], :state-belief [1]}, one per identity in
   the same order as :trial-identities. :posterior-concentrations = prior +
   Σ outer(outcome, state-belief), so with s achieved among n trials the
   array is [[s + 1/2] [n - s + 1/2]].

   :normalization is the fixed-state outcome normalization: at the singleton
   state, theta = conc-achieved / (conc-achieved + conc-not)
   = (s + 1/2) / (n + 1), the same rule pattern-theta and b-update state.
   It is NOT a sum across states (which is trivially 1 for one state).

   :dedup :fired names the layers that fired for this emission, in order:
   UPSTREAM's layer when it is not :none ({:layer :ledger-identity :held
   […]} from record!, or {:layer :update-occurrence :status
   :already-recorded} from b-update), then :read-identity when the collapse
   above removed rows. Empty means no layer fired.

   :version hashes {schema family axes prior posterior trial-vectors}: the
   ordered identities and outcomes plus the arrays, not the close-acceptance
   provenance (whose source paths are not part of the posterior)."
  [family rows upstream]
  (let [mine (vec (filter #(= family (:theta-key %)) rows))
        ids (vec (distinct (map :identity mine)))
        by-id (into {} (map (juxt :identity identity)) mine)
        ordered (mapv by-id ids)
        _ (doseq [r ordered]
            (when-not (and (string? (:identity r)) (boolean? (:observed r)))
              (throw (ex-info "Trial cannot supply a one-hot vector"
                              {:learning-ledger/refusal :invalid-trial
                               :identity (:identity r)}))))
        conflicts (vec (sort (for [[id rs] (group-by :identity mine)
                                   :when (> (count (distinct (map :observed rs))) 1)]
                               id)))
        trials (mapv (fn [r]
                       {:identity (:identity r)
                        :theta-key family
                        :cell (if (:observed r) :achieved :not)
                        :observed (:observed r)
                        :content-ref (carrier-hash (:row r))
                        :close-acceptance (or (:close-acceptance r)
                                              {:status :missing :reason :close-not-looked-up})})
                     ordered)
        vectors (mapv (fn [r] {:identity (:identity r)
                               :outcome (one-hot (:observed r))
                               :state-belief [1]})
                      ordered)
        posterior (reduce (fn [acc v] (add-arrays acc (outer (:outcome v) (:state-belief v))))
                          jeffreys-prior vectors)
        a (get-in posterior [0 0])
        b (get-in posterior [1 0])
        s (count (filter :observed ordered))
        n (count ordered)
        collapsed (- (count mine) n)
        fired (cond-> []
                (not= :none (:layer upstream)) (conj (:layer upstream))
                (pos? collapsed) (conj :read-identity))
        value {:schema carrier-schema
               :family family
               :population :rows-appended-at-comparison
               :axes carrier-axes
               :prior-concentrations jeffreys-prior
               :posterior-concentrations posterior
               :trial-identities trials
               :trial-vectors vectors
               :dedup {:fired fired
                       :upstream upstream
                       :read-side {:layer :read-identity
                                   :input-count (count mine)
                                   :counted-count n
                                   :collapsed-count collapsed
                                   :conflicting-identities conflicts
                                   :policy :last-row-wins}}
               :normalization {:axis :outcomes-at-fixed-state
                               :state :singleton
                               :successes s
                               :trials n
                               :conc-achieved a
                               :conc-not b
                               :theta (/ a (+ a b))
                               :rule "theta = conc-achieved / (conc-achieved + conc-not) = (s + 1/2) / (n + 1)"}}]
    (assoc value :version
           (carrier-hash (select-keys value [:schema :family :axes
                                             :prior-concentrations
                                             :posterior-concentrations
                                             :trial-vectors])))))

(defn carrier-refusal
  "The X5 checks the carrier exists to support, as data: the first refusal
   keyword that applies, or nil. A scalar theta with no concentration array
   and normalization fails even at 3/4; a token-level belief is refused (no
   token posterior exists in this learner and none may be synthesized); a
   non-exact number anywhere fails; vectors, arrays, normalization and
   version must agree with each other, recomputed here, not trusted."
  [c]
  (let [ts (:trial-identities c)
        vs (:trial-vectors c)
        expected-vectors (mapv (fn [t] {:identity (:identity t)
                                        :outcome (one-hot (= :achieved (:cell t)))
                                        :state-belief [1]})
                               ts)
        posterior (when (vector? vs)
                    (reduce (fn [acc v] (add-arrays acc (outer (:outcome v) (:state-belief v))))
                            jeffreys-prior vs))
        a (get-in posterior [0 0] 0)
        b (get-in posterior [1 0] 0)]
    (cond
      (not (map? c)) :carrier-not-a-map
      (not (exact-tree? c)) :inexact-number
      (or (nil? (:posterior-concentrations c))
          (nil? (:normalization c))) :scalar-without-concentrations
      (not= carrier-axes (:axes c)) :carrier-axis-mismatch
      (not (vector? vs)) :trial-vectors-missing
      (or (some #(contains? % :token-belief) ts)
          (some #(not= [1] (:state-belief %)) vs)) :token-posterior-not-a-trial
      (not= (mapv :identity ts) (mapv :identity vs)) :trial-identity-mismatch
      (not= (count ts) (count (set (map :identity ts)))) :duplicate-trial-identity
      (not= expected-vectors vs) :trial-vector-mismatch
      (not= jeffreys-prior (:prior-concentrations c)) :prior-mismatch
      (not= posterior (:posterior-concentrations c)) :posterior-mismatch
      (not= {:axis :outcomes-at-fixed-state :state :singleton
             :conc-achieved a :conc-not b :theta (/ a (+ a b))}
            (select-keys (:normalization c)
                         [:axis :state :conc-achieved :conc-not :theta])) :normalization-mismatch
      (not= (:version c)
            (carrier-hash (select-keys c [:schema :family :axes
                                          :prior-concentrations
                                          :posterior-concentrations
                                          :trial-vectors]))) :version-mismatch
      :else nil)))

(defn close-b-update
  "The :b-update snapshot for a close: one concentration-carrier per family
   over the rows appended at comparison, each row annotated with its close's
   acceptance. Typed absence with the reason when the rows cannot be
   supplied ({:status :missing :reason :ledger-unavailable |
   :ledger-or-close-read-failed}); never a prior standing in for data.

   LEARNING-TRIAL-RECEIPT is this close's record! receipt: its :held trials
   are the ledger-identity dedup layer's dispositions for this emission, and
   are reported under the family they attribute to. This is a producer
   receipt of what the ledger holds at this close; it asserts nothing about
   later consumption."
  [{:keys [ledger-root close-path close-judgment close-roots close-record-files
           learning-trial-receipt]}]
  (let [root (or ledger-root default-root)
        file (io/file root "attempts.edn")]
    (try
      (if-not (.isFile file)
        {:status :missing :reason :ledger-unavailable :ledger-path (str file)}
        (let [files (or close-record-files
                        (close-files (or close-roots (default-close-roots root))))
              rows (annotate-close-statuses (read-trials root) files close-judgment close-path)
              held (vec (for [t (:trials learning-trial-receipt)
                              :when (= :held (:status t))]
                          {:identity (get-in t [:deduplication :identity])
                           :reason (:reason t)
                           :theta-key (theta-key t)}))
              families (sort-by str (distinct (filter keyword? (map :theta-key rows))))]
          {:status :recorded
           :schema :wm/b-update-close-snapshot-v1
           :population :rows-appended-at-comparison
           :ledger-path (str file)
           :ledger-sha256 (identity/sha256 (slurp file))
           :rows-read (count rows)
           :close-files-scanned (count files)
           :unattributed-identities (mapv :identity (remove #(keyword? (:theta-key %)) rows))
           :families (into {}
                           (for [family families
                                 :let [mine-held (filterv #(= family (:theta-key %)) held)]]
                             [family (concentration-carrier
                                      family rows
                                      (if (seq mine-held)
                                        {:layer :ledger-identity
                                         :held (mapv #(dissoc % :theta-key) mine-held)}
                                        {:layer :none}))]))}))
      (catch Exception e
        {:status :missing
         :reason :ledger-or-close-read-failed
         :detail (or (:learning-ledger/refusal (ex-data e)) (.getMessage e))}))))

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
        mine-appended (filter #(= family (:theta-key %)) trials)
        ;; one contribution per recorded occurrence, as pattern-theta does:
        ;; two functions whose docstrings name the same posterior must not
        ;; disagree (claude-2's review)
        mine (vals (into {} (map (juxt :identity identity)) mine-appended))
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
     ;; B-C: the carrier over the rows this update read (append order, the
     ;; same rows pattern-theta reads), naming the update-occurrence dedup
     ;; layer when it fired. :normalization :theta is the pre-update read;
     ;; :theta above additionally counts this occurrence when it is new.
     :carrier (concentration-carrier family mine-appended
                                     (if already
                                       {:layer :update-occurrence :status :already-recorded}
                                       {:layer :none}))
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
