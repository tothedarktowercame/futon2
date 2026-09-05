(ns futon2.aif.task-belief-ladder
  "U52 -- the three-rung ladder at the selection scoring seam.

  THE RULING THIS IMPLEMENTS. `aif-equations.edn :choices
  :task-belief-actand-source` (ratified by Joe 2026-09-03, futon2 `efdc401`):
  case history wins; where a candidate has no direct precedent the system does
  not flatly refuse but attempts CONSTRUCTIVE GENERALIZATION from kin, marking
  the result `:constructed` (never passed off as observed) with its derivation
  and provenance; only when construction gets nowhere does a TYPED REFUSAL
  follow, and that refusal mints a tension record through the U41 schema
  (`:tension/born-of :refused-prediction`).

  WHAT IT IS FOR. U51 (`C506-u51-plateau-composition.md`) measured the thing
  this ladder is aimed at: on the recorded s5 field, 55 `:advance-mission`
  candidates share one controller score at ranks 73-127, and the machine chose
  inside that tie at rank 123. They tie because every scored channel is silent
  for all of them -- centrality 0.0, cascade role 0.0, an unresolvable phase,
  no gates, no decay -- so each takes the same `:mission-value-factor` 0.09
  (0.30 `:doable` weight x 0.3 `\"unknown\"` phase doability). The mission the
  machine has chosen 44 times is scored identically to 54 it has never chosen.
  The ladder makes case history enter the score.

  WHERE IT SITS. Between `war-machine/enrich-candidates-with-mission-value` and
  `efe/rank-actions`. It reads the enriched candidates and the case-history
  index, and returns candidates whose `:mission-value-factor` has been scaled by
  a support factor, plus the refused ones. `:mission-value-factor` is the only
  number it touches, and that number reaches the controller score by exactly one
  path -- `forward-model/mission-value-factor` (`forward_model.clj:111-126`),
  consumed by `predict-effects :advance-mission`
  (`forward_model.clj:152-168`).

  DEFAULT OFF, AND THIS NAMESPACE DOES NOT DECIDE THAT. The flag is
  `war-machine/*task-belief-ladder?*` / `FUTON_WM_TASK_BELIEF_LADDER`; with it
  off `apply-ladder` is never called and the candidate vector is the identical
  object. Turning it on changes the ranking of every in-scope candidate, which
  is the silent-default change U22 forbade, so the flip goes to Joe through the
  flip-readiness gate like every other flip.

  TWO PARAMETERS ARE DECLARED HERE AND NEITHER IS DERIVED FROM A SOURCE. The
  kin relation (`:relation`, default `:k-doc-xref`) and the support map
  (`support->factor`, with `:generalization-discount` on rung 2) are this row's
  declared inputs, reachable per call, recorded on every classification, and
  NOT written into `:choices`. They are exactly the free hand TN-edge-review
  section 1 point 3 sends to Joe; the default-off flag is what keeps that
  honest. Why `:k-doc-xref` is the default is a measurement, not a preference:
  U51 measured four relations over the same plateau and only this one
  partitions it more finely than the score does (54 distinct kin signatures
  over the 55, largest group 2). `:k-type` reaches 53 of the 55 and leaves 53
  of them sharing one signature -- the plateau again, one derivation deeper --
  so a ladder defaulting to it would report a drained plateau and deliver an
  undrained one."
  (:require [clojure.set :as set]
            [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Scope
;; ---------------------------------------------------------------------------

(def in-scope-types
  "The ladder scores task belief about MISSIONS. The recorded plateau is
   entirely `:advance-mission`, the ruling's grain is the actand table, and the
   remaining candidate types (`:no-op`, `:address-sorry`, `:fire-pattern`) reach
   their value by other channels -- refusing them would remove the machine's
   fallback moves for a reason this ladder has not measured."
  #{:advance-mission})

(defn in-scope? [action] (contains? in-scope-types (:type action)))

(defn action-key
  "The identity a case-history record is counted against: [type target]."
  [action]
  [(:type action) (:target action)])

;; ---------------------------------------------------------------------------
;; Kin relations -- the same four U51 measured, computed by one code path
;; ---------------------------------------------------------------------------

(def mission-id-re
  #"\b(M-[A-Za-z0-9][A-Za-z0-9._-]*?)(?=[\s`'\",.;:)\]}]|\.md|$)")

(defn- home-repo [action]
  (some->> (:mission-path action) str (re-find #"/home/joe/code/([^/]+)/") second))

(defn- doc-refs
  "The candidate's own mission id together with the missions its document names.
   The carrier is the mission documents' own cross-reference prose, declared by
   their authors. `:doc-reader` is injectable so a caller can pin the read."
  [action reader]
  (let [p (:mission-path action)
        self (:target action)
        text (when p (reader (str p)))
        named (if text
                (set (map #(str/replace (second %) #"\.md$" "")
                          (re-seq mission-id-re text)))
                #{})]
    (cond-> named (string? self) (conj self))))

(defn default-doc-reader
  "Reads a mission document if it is there; nil otherwise. A missing document is
   an absence, not an error -- it leaves the candidate with only its own id."
  [path]
  (let [f (java.io.File. ^String path)]
    (when (.isFile f) (slurp f))))

(def relations
  "Relation id -> {:statement :token-fn}. Two candidates are kin under a
   relation when their token sets intersect. Every token set is COMPUTED from
   the candidate map, so a candidate the index has never seen is classified by
   the same rule rather than falling out of a lookup table (U51's `negative-1`
   passed for the wrong reason until this was true)."
  {:k-type
   {:statement "same action :type"
    :token-fn (fn [action _opts] (if-let [t (:type action)] #{t} #{}))}
   :k-repo
   {:statement "same home repository, read off :mission-path"
    :token-fn (fn [action _opts] (if-let [r (home-repo action)] #{r} #{}))}
   :k-produces
   {:statement "shares a :produces entry of the capability-graph input recorded on the candidate"
    :token-fn (fn [action _opts]
                (set (get-in action [:goal-outcome-replay-inputs
                                     :capability-graph-input :produces])))}
   :k-doc-xref
   {:statement "one document names the other mission, or both name a mission in common (the mission documents' own cross-references)"
    :token-fn (fn [action opts]
                (doc-refs action (or (:doc-reader opts) default-doc-reader)))}})

(def default-relation
  "Declared input, not a ruling -- see the namespace docstring."
  :k-doc-xref)

(def default-generalization-discount
  "Rung 2 is CONSTRUCTED and must not outrank observed case history. With the
   support map below, a rung-1 candidate with n >= 1 recorded decisions scores
   at least 0.5 and a rung-2 candidate scores strictly under
   discount x 1.0 = 0.5, so `case history wins` is an ordering property of the
   arithmetic and not a claim about it. Declared here; not sourced."
  0.5)

;; ---------------------------------------------------------------------------
;; The support map
;; ---------------------------------------------------------------------------

(defn support->factor
  "Evidence mass -> a factor in [0,1). n/(n+1): zero evidence gives zero, the
   first record buys half of what is available, and no amount of evidence
   reaches 1. DECLARED, NOT SOURCED -- it is in the neighbourhood of a
   unit-prior Dirichlet posterior mean but no equation in the registry writes
   it, and it is reachable per call as `:support->factor`."
  [n]
  (let [n (max 0.0 (double (or n 0)))]
    (/ n (+ 1.0 n))))

(defn hole-availability-factor
  "Recorded open holes -> a saturating availability factor in [0,1).
   `h/(h+1)` is only applied when the candidate carries a non-negative numeric
   `:open-hole-count`; absence remains an explicit evidence gap rather than
   being silently read as zero."
  [h]
  (when (and (number? h) (not (neg? h)))
    (let [h (double h)]
      (/ h (+ h 1.0)))))

;; ---------------------------------------------------------------------------
;; The field context
;; ---------------------------------------------------------------------------

(defn field-context
  "Everything `classify` needs, computed once per field.

   `history` is action-key -> count of persisted decisions that CHOSE that key.
   The caller owns that read (`war-machine/case-history-index` on the live path,
   the U52 producer on the replay path) so this namespace performs no I/O
   except the mission-document read the kin relation declares."
  [candidates history opts]
  (let [relation-id (get opts :relation default-relation)
        relation (or (get relations relation-id)
                     (throw (ex-info "unknown kin relation"
                                     {:relation relation-id
                                      :known (vec (sort (keys relations)))})))
        token-fn (:token-fn relation)
        in-field (filterv in-scope? candidates)
        tokens (into {} (map (fn [a] [(action-key a) (token-fn a opts)])) in-field)]
    {:relation relation-id
     :relation-statement (:statement relation)
     :token-fn token-fn
     :tokens tokens
     :history (or history {})
     :generalization-discount (get opts :generalization-discount
                                   default-generalization-discount)
     :support->factor (get opts :support->factor support->factor)
     :opts (dissoc opts :doc-reader)
     :doc-reader (:doc-reader opts)}))

(defn kin-with-history
  "The case-history-bearing keys kin to `action` under the context's relation,
   its own key excluded, each with its recorded decision count. The candidate's
   own tokens are computed, never looked up."
  [ctx action]
  (let [k (action-key action)
        toks ((:token-fn ctx) action (cond-> (:opts ctx)
                                       (:doc-reader ctx)
                                       (assoc :doc-reader (:doc-reader ctx))))]
    (if-not (seq toks)
      []
      (->> (:tokens ctx)
           (filter (fn [[k2 t2]]
                     (and (not= k2 k) (seq (set/intersection toks (set t2))))))
           (map first)
           (keep (fn [k2] (let [n (get (:history ctx) k2 0)]
                            (when (pos? n) {:key k2 :decisions n}))))
           (sort-by :key)
           vec))))

;; ---------------------------------------------------------------------------
;; The three rungs
;; ---------------------------------------------------------------------------

(defn- classify-from-case-history
  "One candidate -> its rung, its support, and the derivation that grounds it.
   Rung 3 carries `:basis \"not found\"` rather than a silence."
  [ctx action]
  (let [k (action-key action)
        direct (get (:history ctx) k 0)
        s->f (:support->factor ctx)]
    (if (pos? direct)
      {:task-belief/rung 1
       :task-belief/support (double direct)
       :task-belief/factor (double (s->f direct))
       :task-belief/derivation
       {:rule :direct-case-history
        :relation nil
        :basis (str "chosen action of " direct " persisted decision(s) in the recorded corpus")
        :support-map "n/(n+1)"}}
      (let [kin (kin-with-history ctx action)
            mass (reduce + 0.0 (map :decisions kin))]
        (if (seq kin)
          {:task-belief/rung 2
           :task-belief/constructed true
           :task-belief/support mass
           :task-belief/factor (double (* (:generalization-discount ctx) (s->f mass)))
           :task-belief/derivation
           {:rule :constructive-generalization-from-kin
            :relation (:relation ctx)
            :relation-statement (:relation-statement ctx)
            :basis (str "no persisted decision chose " (pr-str k)
                        "; generalized from " (count kin)
                        " kin key(s) carrying " (long mass) " decision(s)")
            :provenance (mapv (fn [{:keys [key decisions]}]
                                {:kin-key key :decisions decisions})
                              kin)
            :support-map "generalization-discount x n/(n+1)"
            :generalization-discount (:generalization-discount ctx)}}
          {:task-belief/rung 3
           :task-belief/support 0.0
           :task-belief/factor 0.0
           :task-belief/derivation
           {:rule :construction-exhausted
            :relation (:relation ctx)
            :relation-statement (:relation-statement ctx)
            :basis "not found: no persisted decision chose this key, and no kin key under the declared relation carries one"}})))))

(defn classify
  "Classify case-history support, then apply recorded hole availability.

   Zero open holes overrides every support rung with the typed rung-3
   `:no-open-holes` refusal: case history can establish belief in an action but
   cannot make `advance open holes` non-vacuous. Positive availability scales
   rung 1 and rung 2 by h/(h+1). A missing hole count is recorded as an evidence
   gap and leaves the prior ladder arithmetic unchanged."
  [ctx action]
  (let [prior (classify-from-case-history ctx action)
        h (:open-hole-count action)
        availability (hole-availability-factor h)]
    (cond
      (and (number? availability) (zero? availability))
      {:task-belief/rung 3
       :task-belief/support 0.0
       :task-belief/factor 0.0
       :task-belief/derivation
       {:rule :no-open-holes
        :basis :no-open-holes
        :open-hole-count h
        :hole-availability-factor availability
        :support-map "h/(h+1)"
        :overridden-task-belief prior}}

      (and (number? availability) (#{1 2} (:task-belief/rung prior)))
      (-> prior
          (update :task-belief/factor #(* (double %) availability))
          (assoc-in [:task-belief/derivation :hole-availability]
                    {:open-hole-count h
                     :factor availability
                     :support-map "h/(h+1)"})
          (update-in [:task-belief/derivation :support-map]
                     #(str % " x h/(h+1)")))

      :else
      (assoc-in prior [:task-belief/derivation :hole-availability]
                {:status :absent
                 :reason :open-hole-count-not-recorded}))))

(def refusal-reason
  "The typed refusal. One reason, because there is one way to reach rung 3."
  :task-belief/zero-support-construction-exhausted)

(def no-open-holes-refusal-reason :no-open-holes)

(defn refusal-record
  "The typed record a rung-3 candidate is refused with. It names the candidate,
   the reason, the relation that came up empty, and where the ladder looked --
   so a reader can tell a refusal from a candidate that was never considered."
  [ctx action classification]
  {:refusal/reason (if (= :no-open-holes
                          (get-in classification [:task-belief/derivation :rule]))
                     no-open-holes-refusal-reason
                     refusal-reason)
   :refusal/action-key (action-key action)
   :refusal/mission-path (:mission-path action)
   :refusal/relation (:relation ctx)
   :refusal/relation-statement (:relation-statement ctx)
   :refusal/history-size (count (:history ctx))
   :refusal/field-size (count (:tokens ctx))
   :refusal/basis (get-in classification [:task-belief/derivation :basis])
   :refusal/overridden-task-belief
   (get-in classification [:task-belief/derivation :overridden-task-belief])
   :refusal/rung 3})

(defn apply-ladder
  "The seam. Returns

     {:candidates <in-scope candidates rescored, out-of-scope untouched in place>
      :refusals   <one typed record per rung-3 candidate>
      :census     {1 n, 2 n, 3 n, :out-of-scope n}}

   Rung 1 and rung 2 keep their place in the vector with `:mission-value-factor`
   scaled by the support factor; rung 3 is REMOVED from the candidates, which is
   what refusing means at a scoring seam. Out-of-scope candidates are returned
   unchanged except for the `:task-belief/rung :out-of-scope` marker, so a
   record can tell `the ladder did not apply` from `the ladder was off`."
  [candidates ctx]
  (reduce
   (fn [acc action]
     (if-not (in-scope? action)
       ;; counted, so the census sums to the field and `out of scope` is a
       ;; number rather than the difference between two other numbers
       (-> acc
           (update :candidates conj (assoc action :task-belief/rung :out-of-scope))
           (update-in [:census :out-of-scope] (fnil inc 0)))
       (let [c (classify ctx action)
             rung (:task-belief/rung c)
             base (:mission-value-factor action)
             scaled (when (number? base)
                      (* (double base) (:task-belief/factor c)))]
         (cond-> (update-in acc [:census rung] (fnil inc 0))
           (= 3 rung)
           (update :refusals conj (refusal-record ctx action c))

           (not= 3 rung)
           (update :candidates conj
                   (cond-> (merge action c)
                     (number? scaled)
                     (assoc :mission-value-factor scaled
                            :task-belief/pre-ladder-mission-value-factor (double base))))))))
   {:candidates [] :refusals [] :census {}}
   candidates))

;; ---------------------------------------------------------------------------
;; Rung 3's tension mint (U41 schema)
;; ---------------------------------------------------------------------------

(defn refusal-payload
  "The typed refusal a rung-3 PARTITION is refused with. One refusal per
   (subject, reason), because a proto-pattern is the class and not each of its
   instances.

   THE MEMBERS ARE POINTED AT, NOT INLINED. The rung-3 partition of one field is
   80-odd candidates; carrying all of them inside the record would put three
   copies of the list into a curated ledger (id, refusal, verbatim statement)
   and make it unreadable for a set that is already written down, per candidate
   and with its own typed record, in the artifact `:artifact` names. What is
   inline is what a reader needs to check the claim without opening it: the
   count, the relation that came up empty, and a deterministic sample."
  [{:keys [subject-id relation refusals artifact]}]
  {:task-belief/refusal refusal-reason
   :grain :mission
   :subject subject-id
   :relation relation
   :refused-count (count refusals)
   :refused-sample (vec (take 5 (sort (map (comp str second :refusal/action-key) refusals))))
   :artifact artifact
   :kin []})

(defn refusal-tension
  "The U41 tension/event pair a rung-3 partition mints, in the shape the ledger
   declares (`tension-ledger.edn :ledger/schema`) and the zaif rung-3 mint set:
   `:born-of :refused-prediction`, the typed refusal carried in-record at
   `:tension/refusal`, `:tension/statement-source` naming that field, and
   `:tension/statement` the refusal read back verbatim as data -- which u41's
   control 12 checks mechanically.

   TWO DELIBERATE DIFFERENCES FROM THE ZAIF MINT, both stated so a reviewer can
   refuse them: (1) the unit is the partition, not the candidate -- zaif's
   rung-3 group is one (actand, arm) pair, here it is 39 candidates of one
   field, and 39 near-identical tensions would be noise rather than a
   proto-pattern; every refused key is named inside the one record. (2) the id
   is a keyword rather than the refusal vector, so replaying a field is
   `:already-present` without carrying a third copy of the refused set.

   PURE: it BUILDS the pair. The append is `u41_tension_ledger.bb`'s
   `append-tension!`, called by the U52 producer -- the live tick path builds
   refusal records and writes no ledger, see the U52 account."
  [{:keys [subject-id refusals relation at by pointers carried-by] :as args}]
  (let [n (count refusals)
        refusal (refusal-payload args)
        id (keyword "wm-ladder" (str (name subject-id) "-zero-support"))]
    {:tension
     #:tension{:id id
               :status :carried
               :provenance {:who (or by "U52 ladder rung-3 refusal mint")
                            :when at
                            :pointers (vec pointers)}
               :carried-by (or carried-by "M-wm-aif-policy-grain-compliance")
               :resolution-path
               "either a kin relation that reaches these candidates, or recorded material a construction could read (U51: :open-hole-count varies 0-24 over the plateau and enters no score), or the admission that the machine has no belief about them and should not be ranking them"
               :born-of :refused-prediction
               :statement-source ":tension/refusal (in-record; the machine's typed refusal is the primary source for a machine-born tension)"
               :minted-by {:row :U52 :seat "wm-build-loop work seat"}
               :poles ["score them from the silent default and rank missions the machine knows nothing about"
                       "refuse them and lose candidates the machine may need"]
               :pattern-links []
               :refusal refusal
               :note (str n " candidate(s) of " (name subject-id)
                          " reached rung 3 under " (name relation) ".")
               :statement (pr-str refusal)}
     :event
     #:event{:id (keyword "wm-ladder" (str (name subject-id) "-zero-support#mint"))
             :tension id
             :type :carried
             :to-status :carried
             :row :U52
             :at at
             :by (or by "U52 ladder rung-3 refusal mint")
             :evidence (vec pointers)
             :note (str "Minted by the U52 ladder's rung-3 partition; "
                        n " refusal record(s), reason " refusal-reason ".")}}))
