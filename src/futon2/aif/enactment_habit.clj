(ns futon2.aif.enactment-habit
  "E, the cascade habit prior, counted from Clause C enactment records
   (PROOF-2a hole H-E; counting rule: holes/labs/wm-contract/proof2/packets/
   H-E-D.md §3, futon2 69bfd1ab).

   One enactment record adds at most one count, to its candidate's policy key,
   and only when the W_c verdict passes. Attempts are trials of their
   pattern's theta (clause 5), not of E. The W_c verdict is input data: the
   vector of failure strings futon3c's exemplar proof2a_check.clj `check-c`
   returns, empty meaning pass. This namespace neither re-implements W_c nor
   reads G_c. Under X_c(d), once the checker reads G_c, its verdict on
   click-001 becomes a fail (the grain attempt names an :artifact check, not a
   G_c pass); the fold needs no change for that.

   Pure: no file, store or data/ access."
  (:require [futon2.aif.cascade-prior :as prior]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def basis :wm/enactment-habit-v1)

(defn- refused [reason detail]
  {:status :refused :reason reason :detail detail})

(defn- replay-precedences [replay candidate]
  (->> (:replays replay)
       (filter #(= candidate (:candidate %)))
       (mapcat :constructed)
       (keep :precedence)
       distinct
       vec))

(defn policy-key-for
  "The cascade-prior policy key of the enactment's candidate, joined by
   candidate id: :mission from the click's candidate derivation :target,
   :shown from the constructor replay's :precedence, :semilattice {}.

   Typed absences stay absences (H-E-D §1): the :target slot names an instance
   when the target is below the mission (A1, taken as the live seam's
   policy-view takes it); the click carries no precedence (A2, read from the
   replay); no reader maps the click's containment and co-application edges
   to :semilattice (A3, {} as policy-view defaults). A missing slot refuses
   with a typed reason; no field is proposed."
  [enactment click replay]
  (let [candidate (:candidate enactment)
        derivation (get-in click [:decision :selection-certificate
                                  :candidate-derivations candidate])
        precedences (replay-precedences replay candidate)]
    (cond
      (nil? candidate) (refused :enactment-names-no-candidate {})
      (nil? derivation) (refused :candidate-not-in-click {:candidate candidate})
      (nil? (:target derivation)) (refused :target-absent {:candidate candidate})
      (not= 1 (count precedences))
      (refused :precedence-absent {:candidate candidate
                                   :distinct-precedences (count precedences)})
      :else
      (or (prior/policy-key {:mission (:target derivation)
                             :shown (first precedences)
                             :semilattice {}})
          (refused :no-policy-identity {:candidate candidate})))))

(defn- key->view [[_ mission shown semilattice]]
  {:mission mission :shown shown :semilattice semilattice})

(defn increment
  "One enactment record -> one increment receipt (H-E-D §3 table).
   `identity` is policy-key-for's result; `wc-verdict` is check-c's vector of
   failure strings. An attempt at a pattern outside the candidate refuses;
   otherwise W_c pass (an empty vector) gives :delta 1, W_c fail :delta 0
   with the failures, and a missing verdict :delta 0 with its absence typed:
   no verdict is not a pass."
  [enactment identity wc-verdict]
  (let [record-id [(:click enactment) (:candidate enactment)]
        attempts (:attempts enactment)
        in-candidate (when (vector? identity) (set (nth identity 2)))
        outside (when in-candidate
                  (vec (distinct (remove in-candidate (map :pattern attempts)))))]
    (cond
      (not (vector? identity)) identity
      (seq outside)
      (assoc (refused :attempt-outside-candidate {:record-id record-id})
             :outside outside)
      :else
      (cond-> {:record-id record-id
               :delta (if (and (vector? wc-verdict) (empty? wc-verdict)) 1 0)
               :policy-key identity
               :attempts (count attempts)
               :deviations (mapv :kind (get-in enactment [:conformance :deviations]))
               :basis basis}
        ;; a map verdict is check-c's typed non-verdict (e.g. {:status
        ;; :join-unverifiable}, futon3c 4bc95005): counted 0, its status kept;
        ;; anything else is no verdict at all
        (not (vector? wc-verdict)) (assoc :wc-verdict (if (and (map? wc-verdict) (keyword? (:status wc-verdict)))
                                                         (select-keys wc-verdict [:status])
                                                         {:status :absent}))
        (and (vector? wc-verdict) (seq wc-verdict)) (assoc :wc-failures wc-verdict)))))

(defn fold
  "Apply increment receipts to a cascade-prior state. A :delta 1 receipt
   counts once per :record-id; a later one with the same id is listed under
   :duplicates and not counted. :delta 0 and refused receipts count nothing.
   Counted records are kept under :enactment-records (record-id -> receipt)."
  [state receipts]
  (reduce
   (fn [st {:keys [record-id delta policy-key] :as receipt}]
     (cond
       (not= 1 delta) st
       (contains? (:enactment-records st) record-id)
       (update st :duplicates (fnil conj [])
               {:record-id record-id
                :duplicate-of (get-in st [:enactment-records record-id])})
       :else
       (-> (prior/observe-policy st (key->view policy-key))
           (assoc-in [:enactment-records record-id] receipt))))
   (prior/coerce-state state)
   receipts))

(defn masses
  "E over a menu of policy keys, by cascade-prior/habit-masses."
  [state keys]
  (prior/habit-masses state (mapv key->view keys)))
