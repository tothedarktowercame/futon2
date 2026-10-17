(ns futon2.aif.wm.selection-cascade-identity
  "E-implementing-G-over-cascades, Packet B: join the occurrence-grain scored
  candidate (cascade-shape-g/arranged->candidate) to the canonical
  :wm/executable-cascade-v1 identity BEFORE the one selector.

  This namespace does not score and does not select.  For each ranked entry
  it builds the canonical structural layer from the entry's exact arranged
  candidate and the occurrence-keyed 象 readings, refines it into the
  executable record (operational additions only as evidenced; unauthored
  nodes become EXPLICIT open holes, never defaults), validates it with the
  one Packet A validator, and binds the certified cascade digest into:

    - the ranked entry and its scorer certificate,
    - the entry's preference (selection-law) receipt,
    - the entry's action (what the selector chooses), and
    - the row's policy summary.

  An entry whose canonical construction or validation refuses is REMOVED
  from the ranked set with a typed refusal — a pattern-grain fallback
  candidate is therefore unselectable, and the selector is never offered a
  rebuilt or substituted chain.  A forged :cascade-digest arriving on an
  action is caught because the join rebuilds and re-validates from the
  arrangement and refuses on mismatch."
  (:require [futon2.aif.load-identity :as load-identity]
            [futon2.aif.wm.executable-cascade :as xc]))

(load-identity/register! *ns* *file*)

(def open-hole-reentry :packet-c-enactment-compiler)

(defn- default-hole
  "The explicit open hole for an occurrence whose operational clause has not
  been authored at scoring time.  The excursion's rule: unresolved design
  decisions are explicit holes, never concealed defaults and never terminal
  refusals."
  [occurrence-id]
  {:owner :wm/enactment-compiler
   :question :operational-clause-owed
   :dependencies []
   :re-entry open-hole-reentry
   :occurrence-id occurrence-id})

(defn- executable-inputs-for
  "Executable refinement inputs for STRUCTURAL.  AUTHORED (from the join
  options) supplies only witnessed additions: :application-order with
  independent evidence, :operational-clauses, :guards.  Every occurrence
  without an authored clause gets an explicit open hole."
  [structural authored]
  (let [ids (mapv :occurrence-id (:nodes structural))
        authored (or authored {})
        clauses (or (:operational-clauses authored) {})
        holes (merge (into {} (map (fn [id] [id (default-hole id)]) ids))
                     (or (:holes authored) {}))]
    {:application-order (vec (:application-order authored))
     :operational-clauses clauses
     :guards (vec (:guards authored))
     :holes holes}))

(defn- receipts-for
  "The construction/scoring/selection receipts that establish this record is
  the exact object compared and chosen.  Derived ONLY from the action (plus
  the policy id), so the digest a pre-selector bind seals is exactly the
  digest a later seam rebuild of the SAME action re-derives — a rebuild may
  not silently differ because the entry carried transient rank/scorer
  fields."
  [entry]
  (let [action (:action entry)]
    {:construction {:policy-id (or (:policy-id entry) (:id action))
                    :policy-key (:policy-key action)}
     :scoring {:scorer-version (:scorer-version action)
               :g-terms (get-in action [:certificate :g-terms])}
     :selection {:stage :pre-selector-binding}}))

(defn bind-entry
  "Build and validate the canonical cascade for one ranked ENTRY.

  Returns {:cascade record :digest d :validation certificate :entry bound}
  where bound carries :cascade-digest on the entry, its :action, its
  :certificate and its :selection-law; or {:refusal {:kind …}} with a typed
  reason (:occurrence-identity-absent, :edge-kind-not-admitted,
  :provenance-invalid, :cascade-validation-error, :identity-forged) — never
  a silent pattern-grain fallback.

  READINGS: entry -> {occurrence-id reading} (the 象 reading receipts; an
  absent entry is an unread citation, {:lookup :void …} a void one).
  AUTHORED: entry -> executable additions (optional).
  An action that already carries a :cascade-digest must match the rebuilt
  digest — a pre-substituted chain is refused, not adopted."
  ([entry readings] (bind-entry entry readings nil))
  ([entry readings authored]
   (let [authored (or authored (fn [_] nil))
         candidate (:action entry)
         ;; the shape scorer's arranged candidate falls back to the bare
         ;; pattern id when 象 gave no fragment index; that is a pattern-grain
         ;; candidate, and the canonical join refuses it at the earliest,
         ;; clearest point rather than letting the validator report a
         ;; downstream symptom
         collapsed (vec (keep #(when (= (:occurrence-id %) (:pattern %)) %)
                              (get-in candidate [:arrangement :nodes])))]
     (if (seq collapsed)
       {:refusal {:kind :occurrence-identity-absent
                  :policy-id (:policy-id entry)
                  :stage :pre-selector-identity-check
                  :nodes collapsed}}
       (let [rebuilt-structural
             (xc/build-structural candidate (readings entry)
                                  (receipts-for entry))]
         (if (:refusal rebuilt-structural)
       {:refusal (assoc (:refusal rebuilt-structural)
                        :policy-id (:policy-id entry)
                        :stage :canonical-structural-build)}
       (let [built (xc/build rebuilt-structural
                             (executable-inputs-for rebuilt-structural
                                                    (authored entry)))
             validation-refusal
             (when-not (:refusal built)
               (let [v (xc/validate-cascade built)]
                 (when-not (xc/certified? v)
                   {:refusal {:kind :cascade-validation-error
                              :clause (:clause v)
                              :policy-id (:policy-id entry)
                              :stage :canonical-validation
                              :repair (:repair v)}})))
             record (when-not (:refusal built) built)
             d (:digest record)]
         (cond
           (:refusal built)
           {:refusal (assoc (:refusal built) :policy-id (:policy-id entry)
                            :stage :canonical-executable-build)}

           (:refusal validation-refusal)
           {:refusal (:refusal validation-refusal)}

           :else
           (let [incoming (get-in entry [:action :cascade-digest])]
             (if (and (some? incoming) (not= incoming d))
               {:refusal {:kind :identity-forged
                          :policy-id (:policy-id entry)
                          :carried incoming :rebuilt d
                          :stage :pre-selector-identity-check}}
               {:cascade record
                :digest d
                :validation (xc/validate-cascade record)
                :entry (-> entry
                           (assoc :cascade-digest d)
                           (assoc-in [:action :cascade-digest] d)
                           (update :certificate assoc :cascade-digest d)
                           (update :selection-law assoc :cascade-digest d)
                           (assoc :canonical-cascade record))}))))))))))

(defn bind-ranked
  "The pre-selector join over a whole ranked set.  Returns {:ranked kept
  :refusals [typed refusals] :digests {policy-id digest}}.  Kept entries are
  bound per bind-entry; refused entries are removed — the selector sees only
  canonically bound candidates."
  ([ranked readings] (bind-ranked ranked readings nil))
  ([ranked readings authored]
   (let [authored (or authored (fn [_] nil))]
   (loop [[entry & more] ranked kept [] refusals [] digests {}]
     (if (nil? entry)
       {:ranked (vec kept) :refusals (vec refusals) :digests digests}
       (let [r (bind-entry entry readings authored)]
         (if (:refusal r)
           (recur more kept (conj refusals (assoc (:refusal r)
                                                  :target-id (:target-id entry)))
                  digests)
           (recur more (conj kept (:entry r)) refusals
                  (assoc digests (:policy-id entry) (:digest r))))))))))

(defn verify-selected-identity
  "Post-selection identity check: the chosen action's :cascade-digest must
  name exactly one ranked entry's certified digest, and that entry's carried
  canonical record must still re-digest and re-validate to the same
  identity.  A whole-chain re-digest substitution (a different, internally
  consistent record) has a different digest and fails here."
  [decision ranked]
  (let [d (get-in decision [:action :cascade-digest])
        matches (filter #(= d (:cascade-digest %)) ranked)
        entry (first matches)]
    (cond
      (nil? d) {:status :cascade-identity-mismatch
                :clause :chosen-action-unbound}
      (not= 1 (count matches)) {:status :cascade-identity-mismatch
                                :clause :digest-not-unique-in-menu
                                :digest d}
      (not= d (xc/digest (:canonical-cascade entry)))
      {:status :cascade-identity-mismatch :clause :record-re-digest-differs
       :digest d}
      (not (xc/certified? (xc/validate-cascade (:canonical-cascade entry))))
      {:status :cascade-identity-mismatch :clause :record-no-longer-validates
       :digest d}
      :else {:status :verified :digest d
             :policy-id (:policy-id entry)})))
