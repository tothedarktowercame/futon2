(ns futon2.aif.wm.cascade-enactment-compiler
  "E-implementing-G-over-cascades, Packet D1: compile a CERTIFIED structural
  cascade plus EXPLICITLY ATTRIBUTED operational authoring into the Packet A
  executable inputs, and refine them into a canonical executable record.

  The compiler adds nothing of its own:
  - application order comes ONLY from authored edges carrying independent
    operational evidence; descriptive reading order, fragment order,
    construction order, scope nesting and gluing witnesses are refused as
    evidence (they describe the reading, not a transition);
  - operation, capability, consumes/produces come ONLY from the attributed
    authoring; the compiler never invents them;
  - a completion check is derived from the occurrence's square ONLY when
    that square is present and validated, and an authored square-derived
    completion that the square does not back is refused as fabricated;
  - every occurrence without a complete clause keeps an EXPLICIT open hole
    with a re-entry condition, so an open cascade can later be refined by
    re-compiling with fuller authoring — the earlier record is never
    mutated (records are append-only; refinement is a new digest over the
    SAME structural layer, so the 象 projection identity is preserved)."
  (:require [futon2.aif.load-identity :as load-identity]
            [futon2.aif.three-halves-square :as square]
            [futon2.aif.wm.executable-cascade :as xc]))

(load-identity/register! *ns* *file*)

(def descriptive-evidence-kinds
  "Evidence kinds that describe the reading and can NEVER justify an
  application-order edge."
  #{:reading-order :fragment-order :construction-order :precedence-relation
    :scope-nesting :gluing :gluing-isomorphism :shared-name})

(def operational-evidence-kinds
  "Independently witnessed transition evidence."
  #{:witnessed-transition :observed-state-transition :product-receipt})

(def open-hole-re-entry :packet-d1-refinement)

(defn- hex64? [x]
  (and (string? x) (boolean (re-matches #"[0-9a-f]{64}" x))))

(defn- refusal [kind & [detail]]
  {:refusal (merge {:kind kind :stage :enactment-compilation} detail)})

(defn- hole-for
  [occurrence-id]
  {:owner :wm/enactment-compiler
   :question :operational-clause-owed
   :dependencies []
   :re-entry open-hole-re-entry
   :occurrence-id occurrence-id})

(defn- baseline-inputs
  "All-holes executable inputs: every occurrence an explicit open hole."
  [structural]
  {:application-order []
   :operational-clauses {}
   :guards []
   :holes (into {} (map (fn [n] [(:occurrence-id n) (hole-for
                                                     (:occurrence-id n))]))
                (:nodes structural))})

(defn certified-structural?
  "STRUCTURAL is certified when its all-holes refinement builds and passes
  the one Packet A validator: the cascade is a durable partial design with
  a known frontier and explicit holes."
  [structural]
  (let [record (xc/build structural (baseline-inputs structural))]
    (if (:refusal record)
      {:status :not-certified
       :reason (:kind (:refusal record))}
      (let [v (xc/validate-cascade record)]
        (if (xc/certified? v)
          {:status :certified :baseline record}
          {:status :not-certified :clause (:clause v) :repair (:repair v)})))))

(defn- cycle-in
  [edges]
  (let [outgoing (group-by :from edges)
        found (volatile! nil)
        state (atom {})]
    (letfn [(visit [node path]
              (when-not @found
                (case (get @state node)
                  :done nil
                  :visiting (let [start (.indexOf ^java.util.List path node)]
                              (vreset! found (conj (subvec path start) node)))
                  (do (swap! state assoc node :visiting)
                      (doseq [e (get outgoing node)]
                        (visit (:to e) (conj path node)))
                      (swap! state assoc node :done)))))]
      (doseq [node (distinct (mapcat (juxt :from :to) edges))] (visit node []))
      @found)))

(defn- check-application-order
  "Authored application-order edges: endpoints must be occurrences, evidence
  must be present and operationally independent (descriptive kinds are
  refused, not demoted), and the result must be acyclic."
  [structural application-order]
  (let [ids (set (map :occurrence-id (:nodes structural)))
        foreign (vec (keep #(when-not (and (ids (:from %)) (ids (:to %))) %)
                           application-order))
        unevidenced (vec (remove :evidence application-order))
        malformed-evidence
        (vec (keep #(let [ev (:evidence %)]
                      (when (and ev
                                 (not (and (map? ev)
                                           (keyword? (:kind ev))
                                           (hex64? (:digest ev)))))
                        %))
                   application-order))
        descriptive (vec (keep #(when (and (:evidence %)
                                           (contains? descriptive-evidence-kinds
                                                      (:kind (:evidence %))))
                                  %)
                               application-order))
        unknown-kind (vec (keep #(when (and (:evidence %)
                                            (not (contains?
                                                  (into operational-evidence-kinds
                                                        descriptive-evidence-kinds)
                                                  (:kind (:evidence %)))))
                                   %)
                                application-order))
        cycle (cycle-in application-order)]
    (cond
      (seq foreign) (refusal :foreign-occurrence {:edges foreign})
      (seq unevidenced) (refusal :application-order-evidence-owed
                                 {:edges unevidenced})
      (seq descriptive) (refusal :descriptive-evidence-not-operational
                                 {:edges descriptive})
      (seq unknown-kind) (refusal :evidence-kind-unknown {:edges unknown-kind})
      (seq malformed-evidence)
      (refusal :transition-evidence-malformed {:edges malformed-evidence})
      cycle (refusal :application-cycle {:cycle cycle})
      :else nil)))

(defn square-derived-completion
  "The completion check warranted by a valid 象 square: CHECK on the B
  element, named by the digest of what the square's B actually carries.
  nil when the occurrence has no valid square (nothing is fabricated)."
  [node]
  (when (square/valid? (:square node))
    [:square-B-observed
     (get-in node [:square :objects :B :theory :elems])]))

(defn- check-clauses
  "Authored operational clauses: keys must be occurrences; authored
  square-derived completions must be backed by the occurrence's actual
  square; operation/capability/consumes/produces pass through attributed,
  nothing is invented."
  [structural clauses]
  (let [ids (set (map :occurrence-id (:nodes structural)))
        by-id (into {} (map (juxt :occurrence-id identity)) (:nodes structural))
        foreign (vec (keep (fn [[oid]] (when-not (ids oid) oid)) clauses))
        fabricated
        (vec (keep (fn [[oid clause]]
                     (let [node (by-id oid)
                           derived (square-derived-completion node)
                           claimed (when (vector? (:completion-check clause))
                                     (first (:completion-check clause)))]
                       (when (and (= :square-B-observed claimed)
                                  (not= derived (:completion-check clause)))
                         {:occurrence-id oid
                          :claimed (:completion-check clause)
                          :square-backs derived})))
                   clauses))]
    (cond
      (seq foreign) (refusal :foreign-occurrence {:occurrence-ids foreign})
      (seq fabricated) (refusal :fabricated-square-derivation
                                {:clauses fabricated})
      :else nil)))

(defn- assemble-inputs
  "Executable inputs for xc/build from the checked authoring: attributed
  clauses (square-derived completion only when warranted), authored
  application-order with independent evidence, and explicit holes for every
  occurrence whose clause is not complete."
  [structural authoring]
  (let [attribution (:attribution authoring)
        clauses (or (:clauses authoring) {})
        by-id (into {} (map (juxt :occurrence-id identity)) (:nodes structural))
        clauses (into {}
                      (map (fn [[oid clause]]
                             (let [node (by-id oid)
                                   completion (or (:completion-check clause)
                                                  (square-derived-completion
                                                   node))]
                               [oid (-> (select-keys clause
                                                     [:operation :capability
                                                      :consumes :produces
                                                      :guard])
                                        (assoc :completion-check completion)
                                        (assoc :authoring/attribution
                                               attribution))])))
                      clauses)
        app-order (vec (map #(assoc (select-keys % [:from :to :evidence])
                                    :authoring/attribution attribution)
                            (:application-order authoring)))
        incomplete (keep (fn [n]
                           (let [c (get clauses (:occurrence-id n))]
                             (when-not (and c (:operation c) (:capability c)
                                            (:completion-check c))
                               (:occurrence-id n))))
                         (:nodes structural))
        holes (merge (into {} (map (fn [id] [id (hole-for id)]) incomplete))
                     (or (:holes authoring) {}))]
    {:application-order app-order
     :operational-clauses clauses
     :guards (vec (:guards authoring))
     :holes holes}))

(defn compile-executable
  "STRUCTURAL (a certified structural cascade) + AUTHORING
  {:attribution {:author … :authority …}
   :application-order [{:from :to :evidence {:kind k :digest …}}]
   :clauses {occurrence-id {:operation … :capability … :consumes …
                            :produces …}}}
  -> {:executable-inputs inputs :record canonical-record :digest d
      :certificate validator-certificate} or {:refusal {:kind …}}.

  Refusal kinds: :authoring-unattributed, :structural-not-certified,
  :foreign-occurrence, :application-order-evidence-owed,
  :descriptive-evidence-not-operational, :evidence-kind-unknown,
  :application-cycle, :fabricated-square-derivation,
  :cascade-validation-error.  The emitted record embeds STRUCTURAL
  unchanged, so its 象 projection identity is preserved; refining later
  produces a NEW record and digest, never a mutation."
  [structural authoring]
  (let [attribution (:attribution authoring)]
    (cond
      (not (and (map? attribution)
                (some? (:author attribution))
                (some? (:authority attribution))))
      (refusal :authoring-unattributed {:attribution attribution})

      :else
      (let [baseline (certified-structural? structural)]
        (if (not= :certified (:status baseline))
          (refusal :structural-not-certified
                   (dissoc baseline :status :baseline))
          (let [order-refusal (check-application-order
                               structural (:application-order authoring))
                clause-refusal (when-not order-refusal
                                 (check-clauses structural
                                                (or (:clauses authoring) {})))]
            (cond
              order-refusal order-refusal
              clause-refusal clause-refusal
              :else
              (let [inputs (assemble-inputs structural authoring)
                    record (xc/build structural inputs)]
                (if (:refusal record)
                  (refusal (:kind (:refusal record))
                           (:refusal record))
                  (let [v (xc/validate-cascade record)]
                    (if-not (xc/certified? v)
                      (refusal :cascade-validation-error
                               {:clause (:clause v) :repair (:repair v)})
                      {:executable-inputs inputs
                       :record record
                       :digest (:digest record)
                       :certificate v})))))))))))
