(ns futon2.aif.wm.execution-journal
  "Packet C (execution journal only): the append-only execution journal
  named by Packet A's journal-reference-shape
  (futon2.aif.wm.executable-cascade/journal-reference-shape — required
  read-only here, never edited).

  Every entry is bound to ONE canonical cascade digest (:journal-id — the
  :cascade-digest of the reference shape), names one occurrence or one
  DECLARED co-application group (:node-or-group), a phase, evidence as
  kind+digest, and both valid-time and recorded-time (Minus40°DB's
  valid-time/recorded-time model: valid-time is when the transition held in
  the world, recorded-time is when the journal learned it).

  The journal is append-never-mutate: entries are content-addressed by
  their own :entry-id digest, supersession adds a NEW entry naming the
  superseded :entry-id (the old entry stays byte-identical in history), and
  a sorry records a contradiction between entries without rewriting either.

  load/validate/replay verify seals and produce typed
  :journal-validation-error values — never abstention, never a policy
  refusal.  Learning reads the replay: enactment is inferred ONLY from
  entries whose evidence kind is :verified-effect and which are not
  superseded; an open-hole entry records that a design hole was reached,
  and the journal remains open for continuation (that is not an error)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.wm.executable-cascade :as xc])
  (:import [java.time Instant]))

(def entry-schema :wm/execution-journal-entry-v1)

;; the binding shape Packet A fixed for every journal entry (referenced,
;; not redefined)
(def reference-shape xc/journal-reference-shape)

(def phases #{:attempted :enacted :open-hole :closed :sorry})
(def evidence-kinds #{:verified-effect :witnessed-attempt :hole-witness
                      :sorry-witness})
(def phase-evidence
  {:attempted :witnessed-attempt
   :enacted :verified-effect
   :open-hole :hole-witness
   :closed :verified-effect
   :sorry :sorry-witness})

;; ---------------------------------------------------------------------------
;; Sealing
;; ---------------------------------------------------------------------------

(defn- hex64? [s] (and (string? s) (re-matches #"[0-9a-f]{64}" s)))

(defn entry-digest
  "The content digest of an entry: over the entry minus its :entry-id."
  [entry]
  (evidence/value-digest (dissoc entry :entry-id)))

(defn- instant? [s]
  (and (string? s)
       (try (Instant/parse s) true (catch Exception _ false))))

(defn entry-defects
  "Every way an entry fails the reference shape.  nil when the entry is
  well-formed."
  [entry]
  (cond-> []
    (not= entry-schema (:schema entry)) (conj :schema)
    (not (hex64? (:journal-id entry))) (conj :journal-id-not-a-cascade-digest)
    (nil? (:node-or-group entry)) (conj :node-or-group-absent)
    (not (contains? phases (:phase entry))) (conj :phase-unknown)
    (not (and (map? (:evidence entry))
              (contains? evidence-kinds (:kind (:evidence entry)))
              (hex64? (:digest (:evidence entry)))))
    (conj :evidence-invalid)
    (not (instant? (:valid-time entry))) (conj :valid-time-invalid)
    (not (instant? (:recorded-time entry))) (conj :recorded-time-invalid)
    (and (:supersedes entry) (not (hex64? (:supersedes entry))))
    (conj :supersedes-invalid)
    (and (:sorry entry) (not (map? (:sorry entry)))) (conj :sorry-invalid)
    (not= (get phase-evidence (:phase entry))
          (get-in entry [:evidence :kind]))
    (conj :phase-evidence-mismatch)
    (and (= :sorry (:phase entry)) (not (map? (:sorry entry))))
    (conj :sorry-absent)
    (and (not= :sorry (:phase entry)) (contains? entry :sorry))
    (conj :sorry-on-non-sorry)
    ;; a sorry is the only phase that may carry :sorry; it must not also
    ;; supersede silently
    (and (= :sorry (:phase entry)) (:supersedes entry))
    (conj :sorry-with-supersede)))

(defn entry
  "Build a sealed journal entry from the reference-shape fields.  Returns
  the entry with its :entry-id, or {:refusal {:kind :journal-entry-invalid
  :defects …}} — construction-time shape errors are refusals to WRITE,
  which is honest: they are not decisions about a policy."
  [{:keys [journal-id node-or-group phase evidence valid-time
           recorded-time supersedes sorry]}]
  (let [e {:schema entry-schema
           :journal-id journal-id
           :node-or-group node-or-group
           :phase phase
           :evidence evidence
           :valid-time valid-time
           :recorded-time recorded-time}
        e (cond-> e
            supersedes (assoc :supersedes supersedes)
            sorry (assoc :sorry sorry))
        defects (entry-defects e)]
    (if (seq defects)
      {:refusal {:kind :journal-entry-invalid :defects defects}}
      (assoc e :entry-id (entry-digest e)))))

;; ---------------------------------------------------------------------------
;; Append-only store
;; ---------------------------------------------------------------------------

(defn- entry-path [store entry-id]
  (io/file store (str entry-id ".edn")))

(declare load-journal)

(defn append!
  "Append one sealed entry to the store directory.  Append-only:
  - an existing IDENTICAL entry at the entry-id path is an idempotent
    success (returns {:appended false :path …});
  - an existing DIFFERENT file at that path is a collision and is refused;
  - nothing is ever overwritten or removed.
  A malformed or unsealed entry is refused before any IO."
  [store entry]
  (let [defects (entry-defects entry)]
    (when (or (seq defects) (not= (:entry-id entry) (entry-digest entry)))
      (throw (ex-info "journal entry refused"
                      {:kind :journal-entry-invalid
                       :defects (vec (concat defects
                                             (when (not= (:entry-id entry)
                                                         (entry-digest entry))
                                               [:entry-id-unsealed])))})))
    (let [f (entry-path store (:entry-id entry))]
      (if (.exists f)
        (if (= (slurp f) (pr-str entry))
          {:appended false :path (.getPath f)}
          (throw (ex-info "journal entry-id collision: existing file differs"
                          {:kind :journal-entry-id-collision
                           :path (.getPath f)
                           :entry-id (:entry-id entry)})))
        (let [loaded (load-journal store)
              _ (when (map? loaded)
                  (throw (ex-info "existing journal is invalid"
                                  {:kind :journal-store-invalid
                                   :validation loaded})))
              latest (last loaded)
              _ (when (and latest
                           (not (neg? (compare (:recorded-time latest)
                                               (:recorded-time entry)))))
                  (throw (ex-info "journal recorded time must increase"
                                  {:kind :journal-recorded-time-not-increasing
                                   :latest (:recorded-time latest)
                                   :offered (:recorded-time entry)})))]
          (spit f (pr-str entry))
          {:appended true :path (.getPath f)})))))

(defn load-journal
  "Read every entry from the store directory in recorded-time order,
  verifying each entry's seal (:entry-id matches its content) and shape.
  A tampered or invalid file is a typed :journal-validation-error — the
  journal never silently drops or repairs history."
  [store]
  (let [entries-from (fn [f]
                       (edn/read-string {:default tagged-literal}
                                        (slurp f)))
        files (->> (filter #(.isFile ^java.io.File %)
                           (file-seq (io/file store)))
                   ;; append! requires strictly increasing recorded time,
                   ;; making append order reconstructable without mutable
                   ;; manifests or filesystem mtimes.
                   (map (fn [f] {:rt (:recorded-time (entries-from f))
                                 :file f}))
                   (sort-by :rt)
                   (mapv :file))
        entries (mapv entries-from files)]
    (reduce
     (fn [acc e]
       (let [defects (entry-defects e)]
         (cond
           (not= (:entry-id e) (entry-digest e))
           (reduced {:status :journal-validation-error
                     :clause :journal-entry-seal-broken
                     :entry-id (:entry-id e)
                     :recomputed (entry-digest e)})
           (seq defects)
           (reduced {:status :journal-validation-error
                     :clause :journal-entry-invalid
                     :entry (dissoc e :square) :defects defects})
           :else (conj acc e))))
     []
     entries)))

;; ---------------------------------------------------------------------------
;; Validation against the cascade
;; ---------------------------------------------------------------------------

(defn cascade-index
  "The index replay validates against, built from a Packet A record: the
  cascade digest, the occurrence ids of its executable nodes, and the
  DECLARED co-application groups DERIVED ONLY from the record's certified
  carrier (:co-application-groups, each digest-bound).  Caller-supplied
  group authority is gone: a group the carrier did not declare cannot be
  admitted here.  Refuses typed when the record is not the executable
  cascade carrier or a group's digest does not bind its identity and
  membership."
  [record]
  (when-not (and (map? record) (= xc/schema (:schema record)))
    (throw (ex-info "cascade-index requires the executable cascade carrier"
                    {:kind :journal-index-not-a-cascade
                     :schema (:schema record)})))
  (let [groups (:co-application-groups record)
        broken (vec (keep #(when-not (and (map? %)
                                          (some? (:group-digest %))
                                          (= (:group-digest %)
                                             (xc/group-digest %)))
                             (:id %))
                          groups))]
    (when (seq broken)
      (throw (ex-info "cascade-index groups not digest-bound"
                      {:kind :journal-index-groups-unbound
                       :groups broken})))
    {:digest (:digest record)
     :nodes (set (map :occurrence-id (:executable-nodes record)))
     :groups (set (map :id groups))}))

(defn- node-or-group-admitted? [idx ng]
  (or (contains? (:nodes idx) ng) (contains? (:groups idx) ng)))

(defn validate-journal
  "Validate loaded entries against one cascade index.  Returns
  {:status :certified :certificate …} or {:status :journal-validation-error
  :clause … :repair …} — never abstention, never a policy refusal."
  ([entries] (validate-journal entries nil))
  ([entries idx]
   (let [fail (fn [clause repair]
                {:status :journal-validation-error :clause clause
                 :repair repair})
         by-id (into {} (map (juxt :entry-id identity)) entries)
         superseded (set (keep :supersedes entries))
         ;; validation runs over the entries AS GIVEN (append order); the
         ;; recorded-time clause must see the order history was written,
         ;; not a re-sorted copy
         ordered entries
         recorded-inversions
         (vec (for [[a b] (partition 2 1 ordered)
                    :when (= (:journal-id a) (:journal-id b))
                    :when (pos? (compare (:recorded-time a)
                                         (:recorded-time b)))]
                [(:entry-id a) (:entry-id b)]))
         duplicate-recorded-times
         (vec (keys (filter #(> (val %) 1)
                            (frequencies (map (juxt :journal-id
                                                    :recorded-time)
                                              ordered)))))
         valid-inversions
         (vec (mapcat
               (fn [[_ history]]
                 (for [[a b] (partition 2 1 history)
                       :when (pos? (compare (:valid-time a)
                                            (:valid-time b)))]
                   [(:entry-id a) (:entry-id b)]))
               (group-by (juxt :journal-id :node-or-group) ordered)))
         supersede-unknown
         (vec (remove #(contains? by-id %) (keep :supersedes entries)))
         supersede-foreign
         (vec (distinct
               (keep (fn [e]
                       (when-let [t (by-id (:supersedes e))]
                         (when-not (= (:journal-id e) (:journal-id t))
                           (:supersedes e))))
                     entries)))
         supersede-node-mismatch
         (vec (keep (fn [e]
                      (when-let [t (by-id (:supersedes e))]
                        (when-not (= (:node-or-group e) (:node-or-group t))
                          (:supersedes e))))
                    entries))
         supersede-dupes
         (vec (keys (filter #(> (val %) 1)
                            (frequencies (keep :supersedes entries)))))]
     (cond
       ;; one journal, one cascade: every entry names the index digest
       (and idx (some #(not= (:digest idx) (:journal-id %)) entries))
       (fail :journal-foreign-cascade
             {:expected (:digest idx)
              :foreign (vec (distinct (filter #(not= (:digest idx)
                                                     (:journal-id %))
                                              (map :journal-id entries))))})

       (and idx (seq (remove #(node-or-group-admitted? idx
                                                       (:node-or-group %))
                             entries)))
       (fail :journal-foreign-node-or-group
             {:entries (vec (mapv (juxt :entry-id :node-or-group)
                                  (remove #(node-or-group-admitted?
                                            idx (:node-or-group %))
                                          entries)))
              :admitted {:nodes (sort-by pr-str (:nodes idx))
                         :groups (sort-by pr-str (:groups idx))}})

       ;; recorded-time is the ledger's own clock: it must not run backwards
       (seq recorded-inversions)
       (fail :journal-recorded-time-inversion {:pairs recorded-inversions})

       (seq duplicate-recorded-times)
       (fail :journal-recorded-time-duplicate
             {:times duplicate-recorded-times})

       ;; per node/group, valid-time must not run backwards either
       (seq valid-inversions)
       (fail :journal-valid-time-inversion {:pairs valid-inversions})

       ;; every supersede names a real entry of the SAME journal, and no
       ;; entry is superseded twice
       (seq supersede-unknown)
       (fail :journal-supersede-unknown {:supersedes supersede-unknown})

       (seq supersede-foreign)
       (fail :journal-supersede-foreign {:supersedes supersede-foreign})

       (seq supersede-node-mismatch)
       (fail :journal-supersede-node-mismatch
             {:supersedes supersede-node-mismatch})

       (seq supersede-dupes)
       (fail :journal-supersede-conflict {:supersedes supersede-dupes})

       :else
       {:status :certified
        :certificate {:schema :wm/execution-journal-v1
                      :cascade-digest (when idx (:digest idx))
                      :entry-count (count entries)
                      :superseded-count (count superseded)
                      :clauses [:foreign-cascade :foreign-node-or-group
                                :recorded-time :valid-time
                                :supersede-known :supersede-same-journal
                                :supersede-unique]}}))))

;; ---------------------------------------------------------------------------
;; Replay
;; ---------------------------------------------------------------------------

(defn replay
  "Replay the journal into the state learning reads.  Enactment is inferred
  ONLY from :enacted entries with :verified-effect evidence that are not
  superseded.  Superseded entries contribute nothing.  Open holes are
  carried as an open continuation of the same cascade — the journal stays
  usable; reaching a design hole is a recorded fact, not a failure."
  [entries]
  (let [superseded (set (keep :supersedes entries))
         live (remove #(contains? superseded (:entry-id %)) entries)]
    {:enacted (set (keep #(when (and (= :enacted (:phase %))
                                     (= :verified-effect
                                        (:kind (:evidence %))))
                            (:node-or-group %))
                         live))
     :attempted (set (keep #(when (= :attempted (:phase %))
                              (:node-or-group %))
                           live))
     :open-holes (set (keep #(when (= :open-hole (:phase %))
                               (:node-or-group %))
                            live))
     :sorries (set (keep #(when (= :sorry (:phase %))
                            (:node-or-group %))
                         live))
     :superseded superseded
     ;; continuation: the cascade is open while any node has an open hole
     ;; and no superseded-out enactment
     :open-cascade? (boolean (some #(= :open-hole (:phase %)) live))}))

(defn certified? [result] (= :certified (:status result)))
