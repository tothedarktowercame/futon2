(ns futon2.aif.wm.executable-cascade
  "E-implementing-G-over-cascades, Packet A: the ONE canonical saved artifact
  for a selected cascade and the ONE pure validator every boundary consumes.

  Two layers, per the 象 / Minus40°DB compatibility ruling (2026-10-16):

  * structural layer (:structural, :wm/structural-cascade-v1) — occurrence
    nodes plus the 象 census (void/unread results kept, never executable
    nodes), descriptive reading order, witnessed gluings, typed shared-name
    candidates, guards, construction order, authored additions, alternatives,
    and the pins/receipts binding provenance.  This is the
    ThreeHalvesBlend.Policy-shaped diagram; project-structural returns it
    exactly, and refinement may not lose or reinterpret it.

  * executable refinement (the rest, :wm/executable-cascade-v1) — evidenced
    application-order edges, operational clauses, the executable frontier
    (only :open occurrences), and open holes.  It may add operational
    evidence; it may not invent missing 象 readings, reinterpret descriptive
    order as application order, or flatten a co-application group.

  Identity: the content digest — the repository evidence digest
  (interpretation-evidence/value-digest) over the record minus its :digest
  field — is the cascade identity used throughout scoring, selection,
  persistence, validation, and the journal.  Records are append-never; a
  changed record is a new record with a new digest.

  Three absences stay distinct and only :open enters the executable set:
  :void   a square part matched nothing; a census result, not an occurrence;
  :unread the four maps are unauthored; a census result, not an occurrence;
  :open   a valid structural occurrence whose enactment may remain holed.

  Four relations stay separate: :reading-order (descriptive), :gluing
  (undirected, witness-required), :application-order (directed,
  evidence-required), :guards.  An unwitnessed shared name stays a typed
  :shared-name-candidate — excluded from cycle checks, unusable for
  enactment.  The journal is NOT implemented here;
  journal-reference-shape fixes the binding every future entry must carry."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.three-halves-square :as square]))

(identity/register! *ns* *file*)

(def structural-schema :wm/structural-cascade-v1)
(def schema :wm/executable-cascade-v1)

(def census-statuses #{:void :unread :open})
(def edge-kinds #{:reading-order :gluing :application-order :guard
                  :shared-name-candidate})

(def journal-reference-shape
  "Every journal entry (its own namespace, later) must carry these bindings;
  nothing else names a cascade into enactment."
  {:cascade-digest :sha256-hex
   :node-or-group :occurrence-id-or-co-application-group
   :phase :keyword
   :evidence {:kind :keyword :digest :sha256-hex}})

;; ---------------------------------------------------------------------------
;; Identity
;; ---------------------------------------------------------------------------

(defn digest
  "Content digest of a record: the repository value-digest over the record
  minus its :digest field.  The validator recomputes this to seal."
  [record]
  (evidence/value-digest (dissoc record :digest)))

;; ---------------------------------------------------------------------------
;; Structural layer
;; ---------------------------------------------------------------------------

(defn- occurrence-id [node]
  (or (:occurrence-id node)
      (when (and (map? node) (contains? node :fragment-index))
        [(or (:pattern node) (:id node)) (:fragment-index node)])
      (:pattern node)
      (:id node)
      node))

(defn structural-nodes
  "Occurrence-keyed structural nodes from the scored arrangement plus the 象
  squares census.  Pattern identity is retained per node, distinct from
  occurrence identity: two citations of one pattern stay two occurrences."
  [arrangement squares]
  (mapv (fn [node]
          (let [oid (occurrence-id node)
                square (get squares oid)
                status (cond
                         (= :void (:lookup square)) :void
                         (nil? square) :unread
                         (square/valid? square) :open
                         :else :void)]
            (cond-> {:occurrence-id oid
                     :pattern (or (:pattern node) (:id node))
                     :roles (vec (distinct (:roles node)))
                     :fragment-indices (vec (keep identity [(:fragment-index node)]))
                     :status status
                     :origin (or (:origin node) :selected)}
              (and square (not= :void (:lookup square)))
              (assoc :square square))))
        (:nodes arrangement)))

(defn- classify-edges
  "Split arrangement edges into the separate relations.  An :overlap edge is
  an undirected gluing candidate — a :gluing only with an
  isomorphism/consistency witness, else a typed :shared-name-candidate.  A
  directed :precedes edge is application-order only with transition
  evidence; descriptive reading order is carried separately and never stands
  in for it.  Unknown kinds and evidence-less precedes edges are refused."
  [edges]
  (reduce
   (fn [acc edge]
     (let [base {:from (or (:from edge) (:a edge))
                 :to (or (:to edge) (:b edge))}
           kind (or (:kind edge) (:kind-used edge) :precedes)]
       (case kind
         :overlap
         (if (:witness edge)
           (update acc :gluing conj (assoc base :kind :gluing
                                           :witness (:witness edge)))
           (update acc :shared-name-candidates
                   conj (assoc base :kind :shared-name-candidate)))
         :precedes
         (if (:evidence edge)
           (update acc :application-order
                   conj (assoc base :kind :application-order
                               :evidence (:evidence edge)))
           (update acc :owed conj (assoc base :kind
                                         :application-order-evidence-owed)))
         (update acc :refused conj (assoc base :kind kind)))))
   {:gluing [] :shared-name-candidates [] :application-order []
    :owed [] :refused []}
   edges))

(defn build-structural
  "The structural layer of a selected policy's scored candidate.  squares is
  occurrence-id -> validated three-halves square receipt, or {:lookup :void}
  for a refused lookup; an absent entry is :unread.  Pure; no defaults."
  [candidate squares receipts & [{:keys [authored-additions alternatives]}]]
  (let [arrangement (:arrangement candidate)
        nodes (structural-nodes arrangement squares)
        classified (classify-edges (:edges arrangement))
        {gluings :gluing shared :shared-name-candidates
         owed :owed refused :refused} classified]
    (if (or (seq owed) (seq refused))
      {:refusal {:kind (if (seq refused)
                         :edge-kind-not-admitted
                         :application-order-evidence-owed)
                 :edges (vec (concat refused owed))}}
      {:schema structural-schema
       :target (:target candidate)
       :policy-id (:id candidate)
       :nodes nodes
       :reading-order (vec (:precedence arrangement))
       :gluing gluings
       :shared-name-candidates shared
       :construction-order (mapv occurrence-id (:nodes arrangement))
       :authored-additions (vec authored-additions)
       :alternatives (vec alternatives)
       :receipts receipts})))

;; ---------------------------------------------------------------------------
;; Executable refinement
;; ---------------------------------------------------------------------------

(defn build
  "Refine a structural layer into the canonical :wm/executable-cascade-v1
  record.  executable-inputs supplies ONLY witnessed additions:
    :application-order [{:from :to :evidence}]
    :operational-clauses {occurrence-id {:operation :completion-check
                                         :capability :consumes :produces}}
    :guards
    :holes {occurrence-id {:owner :question :dependencies :re-entry}}
  Refuses typed (returns {:refusal ...}) when a clause names a non-:open
  occurrence or an application-order edge lacks evidence.  No capability,
  operation, order, or gluing is invented here."
  [structural executable-inputs]
  (let [{:keys [application-order operational-clauses guards holes]}
        executable-inputs
        open-ids (set (map :occurrence-id
                           (filter #(= :open (:status %)) (:nodes structural))))
        bad-clauses (keep (fn [[oid]] (when-not (open-ids oid) oid))
                          operational-clauses)
        bad-edges (vec (remove :evidence application-order))]
    (cond
      (seq bad-clauses)
      {:refusal {:kind :clause-for-non-open-occurrence
                 :occurrence-ids (vec bad-clauses)}}
      (seq bad-edges)
      {:refusal {:kind :application-order-evidence-owed :edges bad-edges}}
      :else
      (let [executable-nodes
            (mapv (fn [{:keys [occurrence-id] :as node}]
                    (let [clause (get operational-clauses occurrence-id)
                          hole (get holes occurrence-id)
                          base (select-keys node [:occurrence-id :pattern :roles
                                                  :fragment-indices :origin
                                                  :status :square])]
                      (cond-> (assoc base :enactment-status
                                     (if (and clause (:operation clause)
                                              (:completion-check clause)
                                              (:capability clause))
                                       :enactable :open-hole))
                        clause (assoc :clause clause)
                        hole (assoc :hole hole))))
                  (filter #(= :open (:status %)) (:nodes structural)))
            record {:schema schema
                    :structural structural
                    :executable-nodes executable-nodes
                    :relations {:application-order (vec application-order)
                                :guards (vec guards)}
                    :frontier (vec (remove
                                    #(contains? (set (map :to application-order))
                                                (:occurrence-id %))
                                    (filter #(= :enactable
                                                (:enactment-status %))
                                            executable-nodes)))
                    :journal-reference journal-reference-shape
                    :census (frequencies (map :status (:nodes structural)))}]
        (assoc record :digest (digest record))))))

;; ---------------------------------------------------------------------------
;; Projection and persistence
;; ---------------------------------------------------------------------------

(defn project-structural
  "Forget executable additions: the exact structural diagram + census the
  record was built from."
  [record] (:structural record))

(defn save!
  "Persist the immutable record as EDN beside its digest.  Content-addressed:
  the file name is the digest."
  [record dir]
  (let [f (io/file dir (str (:digest record) ".edn"))]
    (spit f (pr-str record))
    (.getPath f)))

(defn load-record
  "Reload a saved record, re-digesting it: identity survives save/reload or
  the load refuses."
  [file]
  (let [record (edn/read-string {:default tagged-literal} (slurp file))]
    (when-not (map? record)
      (throw (ex-info "saved cascade is not a record" {:file (str file)})))
    (when-not (= (:digest record) (digest record))
      (throw (ex-info "saved cascade does not re-digest"
                      {:file (str file) :digest (:digest record)})))
    record))

;; ---------------------------------------------------------------------------
;; The ONE validator
;; ---------------------------------------------------------------------------

(defn- application-cycle [application-order node-ids]
  (let [outgoing (group-by :from application-order)
        found (volatile! nil)
        state (atom {})]
    (letfn [(visit [node path]
              (when-not @found
                (case (get @state node)
                  :done nil
                  :visiting (let [start (.indexOf ^java.util.List path node)]
                              (vreset! found (conj (subvec path start) node)))
                  (do (swap! state assoc node :visiting)
                      (doseq [edge (get outgoing node)]
                        (visit (:to edge) (conj path node)))
                      (swap! state assoc node :done)))))]
      (doseq [node node-ids] (visit node []))
      @found)))

(defn validate-cascade
  "The single pure validator for :wm/executable-cascade-v1.  Returns
  {:status :certified :certificate ...} (a sealed certificate) or
  {:status :cascade-validation-error :clause <key> :repair <inputs>}.  Never
  an abstention or policy refusal: it checks whether the machine faithfully
  represented the policy it already selected."
  [record]
  (let [fail (fn [clause repair]
               {:status :cascade-validation-error :clause clause :repair repair})
        structural (:structural record)
        nodes (:nodes structural)
        node-ids (mapv :occurrence-id nodes)
        id-set (set node-ids)
        app-order (get-in record [:relations :application-order])
        gluings (get structural :gluing)
        shared (get structural :shared-name-candidates)
        exec-nodes (:executable-nodes record)
        exec-ids (set (map :occurrence-id exec-nodes))
        open-ids (set (map :occurrence-id
                           (filter #(= :open (:status %)) nodes)))
        foreign (vec (keep #(when-not (and (id-set (:from %)) (id-set (:to %))) %)
                           (concat app-order gluings shared)))
        unwitnessed (vec (remove :witness gluings))
        witnessed-shared (vec (keep :witness shared))
        cycle (application-cycle app-order node-ids)
        order-ok? (= (frequencies (map pr-str (:construction-order structural)))
                     (frequencies (map pr-str node-ids)))
        collapsed (vec (keep #(when (and (seq (:fragment-indices %))
                                         (= (:occurrence-id %) (:pattern %))) %)
                             nodes))
        invalid-squares (vec (keep #(when (and (:square %)
                                               (not (square/valid? (:square %))))
                                      (:occurrence-id %))
                                   nodes))
        coverage-bad (vec (concat
                           (keep #(when (and (= :enactable (:enactment-status %))
                                             (not (and (get-in % [:clause :operation])
                                                       (get-in % [:clause :completion-check])
                                                       (get-in % [:clause :capability])))) %)
                                 exec-nodes)
                           (keep #(when (and (= :open-hole (:enactment-status %))
                                             (not (:hole %))) %)
                                 exec-nodes)))
        projection-ok? (and (map? structural)
                             (= structural-schema (:schema structural))
                             (vector? (:nodes structural))
                             (vector? (:construction-order structural))
                             (= (:census record)
                                (frequencies (map :status (:nodes structural)))))]
    (cond
      (not= schema (:schema record))
      (fail :identity-schema {:schema (:schema record) :expected schema})

      (not= (:digest record) (digest record))
      (fail :identity-digest {:digest (:digest record)
                              :recomputed (digest record)})

      (not projection-ok?)
      (fail :projection-structural-lost {:structural (:schema structural)})

      (not= node-ids (distinct node-ids))
      (fail :population-duplicate-occurrence
            {:duplicates (vec (keys (filter #(> (val %) 1)
                                            (frequencies (map pr-str node-ids)))))})

      (some #(when-not (census-statuses (:status %)) (:occurrence-id %)) nodes)
      (fail :population-unknown-status
            {:nodes (vec (keep #(when-not (census-statuses (:status %)) %) nodes))})

      (seq collapsed)
      (fail :population-occurrence-collapsed-to-pattern {:nodes collapsed})

      (not= exec-ids open-ids)
      (fail :population-executable-set
            {:executable (vec (sort-by pr-str exec-ids))
             :open (vec (sort-by pr-str open-ids))})

      (seq foreign)
      (fail :graph-endpoint-foreign {:edges foreign})

      (seq invalid-squares)
      (fail :pins-square-invalid {:occurrence-ids invalid-squares})

      (seq unwitnessed)
      (fail :graph-gluing-unwitnessed {:edges unwitnessed})

      (seq witnessed-shared)
      (fail :cascade-law-shared-name-not-gluing {:edges witnessed-shared})

      ;; application order without transition evidence is descriptive order
      ;; smuggled in; reading order may never satisfy this obligation
      (seq (remove :evidence app-order))
      (fail :cascade-law-application-order-without-evidence
            {:edges (vec (remove :evidence app-order))})

      cycle
      (fail :cascade-law-application-cycle {:cycle cycle})

      (not order-ok?)
      (fail :cascade-law-construction-order
            {:construction-order (:construction-order structural)})

      (seq coverage-bad)
      (fail :operational-coverage {:nodes coverage-bad})

      :else
      {:status :certified
       :certificate {:schema schema
                     :digest (:digest record)
                     :node-count (count nodes)
                     :executable-count (count exec-nodes)
                     :census (:census record)
                     :clauses [:identity-schema :identity-digest
                               :projection-structural :population
                               :graph-well-formedness :pins
                               :cascade-law :operational-coverage]}})))

(defn certified? [result] (= :certified (:status result)))
