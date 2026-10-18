(ns futon2.aif.wm.executable-cascade
  "E-implementing-G-over-cascades, Packet A: the ONE canonical saved artifact
  for a selected cascade and the ONE pure validator every boundary consumes.

  Two layers, per the 象 / Minus40°DB compatibility ruling (2026-10-16):

  * structural layer (:structural, :wm/structural-cascade-v1) — OCCURRENCES
    only in :nodes, each with genuine occurrence identity (never the bare
    pattern id), the four maps of its 象 square, full 象 provenance, and its
    position in descriptive reading order.  Void and unread citation results
    live ONLY in the construction census, never in :nodes.  Gluings are
    undirected and carry a witness that satisfies a recognised witness
    schema; anything less stays a typed :shared-name-candidate.

  * executable refinement (:wm/executable-cascade-v1) — evidenced
    application-order edges (independent evidence only: arrangement
    :precedes edges are descriptive reading order and never operational),
    operational clauses, the executable frontier, and open holes.

  Identity: the content digest — the repository evidence digest
  (interpretation-evidence/value-digest) over the record minus its :digest
  field — is the cascade identity.  The structural layer and its census are
  additionally bound by their own census digest, so a substitution cannot
  certify merely by remaining internally consistent after re-digesting the
  outer record.

  Records are append-never; save! never overwrites an existing file."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as cset]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.three-halves-square :as square]))

(identity/register! *ns* *file*)

(def structural-schema :wm/structural-cascade-v1)
(def schema :wm/executable-cascade-v1)

(def census-statuses #{:void :unread :open})
(def edge-kinds #{:reading-order :gluing :application-order :guard
                  :shared-name-candidate})
(def witness-kinds #{:isomorphism :consistency})
(def descriptive-edge-kinds
  "Relations emitted by analysis-cascade.  They describe the reading and
  remain non-operational in the structural layer."
  #{:precedes :context-enables :condition-enables :dependency-enables
    :goal-precedes :justified-by :contrasts-with})

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
  minus its :digest field."
  [record]
  (evidence/value-digest (dissoc record :digest)))

(def ^:private hex64? #(and (string? %) (re-matches #"[0-9a-f]{64}" %)))

(defn census-digest
  "The census's own binding digest: over the occurrence identities of the
  :open occurrences, the void citations, and the unread citations.  Any
  substitution in one of the three populations changes it."
  [census]
  (evidence/value-digest (select-keys census [:open :void :unread])))

;; ---------------------------------------------------------------------------
;; 象 provenance
;; ---------------------------------------------------------------------------

(def provenance-fields
  "The 象 provenance every occurrence must carry: the reading record id, the
  exact cited span, the source-text digest, the analysis digest, the cited
  pattern's source_sha256, and the identity (digest) of the square."
  [:reading-record-id :span :source-text-digest :analysis-digest
   :source-sha256 :square-digest])

(defn- provenance-valid?
  "A complete 象 provenance: all fields present, digests well-formed, and a
  concrete span.  Returns nil when valid, else the missing/invalid fields."
  [p]
  (vec (concat
        (keep #(when-not (some? (get p %)) %) provenance-fields)
        (keep #(when (and (get p %) (not (hex64? (get p %))))
                 [:malformed %])
              [:source-text-digest :analysis-digest :source-sha256
               :square-digest])
        (when (and (:span p)
                   (not (and (map? (:span p))
                             (integer? (get-in p [:span :start]))
                             (integer? (get-in p [:span :end]))
                             (<= 0 (get-in p [:span :start]))
                             (< (get-in p [:span :start])
                                (get-in p [:span :end])))))
          [:malformed :span]))))

(defn occurrence-provenance
  "Build the 象 provenance for one occurrence from its reading record entry."
  [{:keys [reading-record-id span source-text-digest analysis-digest
           source-sha256 square]}]
  {:reading-record-id reading-record-id
   :span span
   :source-text-digest source-text-digest
   :analysis-digest analysis-digest
   :source-sha256 source-sha256
   :square-digest (evidence/value-digest square)})

;; ---------------------------------------------------------------------------
;; Structural layer
;; ---------------------------------------------------------------------------

(defn- node-occurrence-id
  "Occurrence identity, never a bare pattern fallback: the node's explicit
  :occurrence-id, else [pattern fragment-index] when a fragment index
  exists.  nil when the node has neither — the builder refuses such a node."
  [node]
  (or (:occurrence-id node)
      (when (and (map? node) (contains? node :fragment-index))
        [(or (:pattern node) (:id node)) (:fragment-index node)])))

(defn- census-entry
  "A void or unread citation result: census-only, never a node.  Carries the
  citation identity and what was observed."
  [oid node kind extra]
  {:citation oid :pattern (or (:pattern node) (:id node))
   :kind kind :observation extra})

(defn- occurrence-node
  "An occurrence with genuine identity, a validated square, and complete 象
  provenance.  Returns the node, or a typed defect."
  [node reading]
  (let [oid (node-occurrence-id node)]
    (cond
      (nil? oid)
      {:defect [:occurrence-identity-absent node]}
      (nil? reading)
      {:census (census-entry oid node :unread
                             {:reason :no-reading-supplied})}
      (= :void (:lookup reading))
      {:census (census-entry oid node :void (:observation reading))}
      (nil? (:square reading))
      {:census (census-entry oid node :unread (:observation reading))}
      (not (square/valid? (:square reading)))
      {:census (census-entry oid node :void
                             {:kind :square-invalid})}
      :else
      (let [prov (or (:provenance reading)
                     (when (contains? reading :reading-record-id)
                       (occurrence-provenance reading)))]
        (if (seq (provenance-valid? prov))
          {:defect [:provenance-invalid oid
                    (provenance-valid? prov)]}
          {:node {:occurrence-id oid
                  :pattern (or (:pattern node) (:id node))
                  :roles (vec (distinct (:roles node)))
                  :fragment-indices (vec (keep identity [(:fragment-index node)]))
                  :origin (or (:origin node) :selected)
                  :provenance prov
                  :square (:square reading)}})))))

(defn- witness-conforming?
  "A gluing witness must satisfy a recognised schema: kind :isomorphism or
  :consistency, a 64-hex evidence digest, and the two glued objects as its
  endpoints.  Anything else — including a truthy but unsupported map — is
  NOT a gluing witness."
  [witness from to]
  (and (map? witness)
       (contains? witness-kinds (:kind witness))
       (hex64? (:digest witness))
       (sequential? (:objects witness))
       (= 2 (count (:objects witness)))
       (= (set (:objects witness)) (set [(str from) (str to)]))))

(defn- classify-edges
  "Split arrangement edges into the separate relations.  :precedes edges are
  DESCRIPTIVE reading order (role-labelled, non-operational): retained as
  :reading-order-edges, never as application order, and no transition
  evidence is demanded of them.  An :overlap edge is an undirected gluing
  candidate — a :gluing only when its witness conforms to the witness
  schema, else a typed :shared-name-candidate."
  [edges]
  (reduce
   (fn [acc edge]
     (let [base {:from (or (:from edge) (:a edge))
                 :to (or (:to edge) (:b edge))}
           kind (or (:kind edge) (:kind-used edge) :precedes)]
       (case kind
         :overlap
         (if (witness-conforming? (:witness edge) (:from base) (:to base))
           (update acc :gluing conj (assoc base :kind :gluing
                                           :witness (:witness edge)))
           (update acc :shared-name-candidates
                   conj (assoc base :kind :shared-name-candidate
                               :offered-witness (:witness edge))))
         (if (contains? descriptive-edge-kinds kind)
           (update acc :reading-order-edges
                   conj (assoc base :kind :reading-order
                               :relation kind
                               :role (:role edge)))
           (update acc :refused conj (assoc base :kind kind))))))
   {:gluing [] :shared-name-candidates [] :reading-order-edges []
    :refused []}
   edges))

(defn build-structural
  "The structural layer of a selected policy's scored candidate.  readings
  is occurrence-id -> {:square <validated receipt> :reading-record-id …
  full 象 provenance inputs} for an occurrence, {:lookup :void …} for a void
  citation; an absent entry is an unread citation.  Void/unread results go
  to the census; ONLY occurrences become nodes.  Pure; no defaults."
  [candidate readings receipts & [{:keys [authored-additions alternatives]}]]
  (let [arrangement (:arrangement candidate)
        classified (classify-edges (:edges arrangement))
        {refused :refused} classified
        results (mapv #(occurrence-node % (get readings
                                               (node-occurrence-id %)))
                      (:nodes arrangement))
        defects (vec (keep :defect results))
        nodes (vec (keep :node results))
        census-entries (vec (keep :census results))
        census {:open (mapv :occurrence-id nodes)
                :void (vec (filter #(= :void (:kind %)) census-entries))
                :unread (vec (filter #(= :unread (:kind %)) census-entries))}]
    (cond
      (seq refused)
      {:refusal {:kind :edge-kind-not-admitted :edges refused}}
      (seq defects)
      {:refusal {:kind (if (some #(= :occurrence-identity-absent (first %))
                                 defects)
                         :occurrence-identity-absent
                         (if (some #(= :reading-absent (first %)) defects)
                           :reading-absent :provenance-invalid))
                 :defects defects}}
      :else
      (let [layer {:schema structural-schema
                   :target (:target candidate)
                   :policy-id (:id candidate)
                   :nodes nodes
                   :reading-order (vec (:precedence arrangement))
                   :reading-order-edges (:reading-order-edges classified)
                   :gluing (:gluing classified)
                   :shared-name-candidates (:shared-name-candidates classified)
                   :construction-order (mapv :occurrence-id nodes)
                   :census (assoc census :census-digest
                                  (census-digest census))
                   :authored-additions (vec authored-additions)
                   :alternatives (vec alternatives)
                   :receipts receipts}]
        (assoc layer :digest (digest layer))))))

;; ---------------------------------------------------------------------------
;; Executable refinement
;; ---------------------------------------------------------------------------

(defn group-digest
  "A declared co-application group's own binding digest, over its identity
  and exact membership."
  [group]
  (evidence/value-digest (select-keys group [:id :members :authority])))

(defn- group-defects
  "Every way a set of declared co-application groups fails the carrier:
  absent/duplicate/colliding identity, foreign/duplicated/short membership,
  or an occurrence claimed by two groups (overlap ambiguity)."
  [groups occurrence-ids]
  (let [ids (mapv :id groups)
        member-lists (mapv :members groups)]
    (vec (concat
          (keep #(when-not (some? (:id %)) [:group-id-absent %]) groups)
          (keep #(when-not (and (map? (:authority %))
                                (seq (:authority %)))
                   [:group-authority-absent %])
                groups)
          (for [[id n] (frequencies ids) :when (> n 1)]
            [:group-id-duplicate id])
          (for [id ids :when (contains? occurrence-ids id)]
            [:group-id-collides-with-occurrence id])
          (keep (fn [g]
                  (when-not (and (vector? (:members g))
                                 (<= 2 (count (:members g)))
                                 (= (count (:members g))
                                    (count (distinct (:members g)))))
                    [:group-membership-invalid g]))
                groups)
          (for [g groups m (:members g)
                :when (not (contains? occurrence-ids m))]
            [:group-member-foreign [(:id g) m]])
          (for [[m n] (frequencies (apply concat member-lists)) :when (> n 1)]
            [:group-member-in-two-groups m])))))

(defn build
  "Refine a structural layer into the canonical :wm/executable-cascade-v1
  record.  executable-inputs supplies ONLY witnessed additions:
    :application-order [{:from :to :evidence}] — independent operational
      evidence; descriptive :precedes/reading order NEVER satisfies this;
    :operational-clauses {occurrence-id {:operation :completion-check
                                         :capability :consumes :produces}}
    :guards
    :holes {occurrence-id {:owner :question :dependencies :re-entry}}
    :co-application-groups [{:id stable-group-identity
                             :members [occurrence-id …]  ;; ≥2, exact,
                                                      ;; each occurrence in
                                                      ;; at most one group
                             :authority …}] — the DECLARED composition
      groups; each is digest-bound on entry and validator-checked forever.
  Every executable node IS an occurrence of the structural layer (there are
  no void/unread executable nodes by construction)."
  [structural executable-inputs]
  (let [{:keys [application-order operational-clauses guards holes
                co-application-groups]}
        executable-inputs
        occurrence-ids (set (map :occurrence-id (:nodes structural)))
        bad-clauses (keep (fn [[oid]] (when-not (occurrence-ids oid) oid))
                          operational-clauses)
        bad-edges (vec (remove :evidence application-order))
        groups (vec co-application-groups)
        g-defects (group-defects groups occurrence-ids)]
    (cond
      (seq bad-clauses)
      {:refusal {:kind :clause-for-non-occurrence
                 :occurrence-ids (vec bad-clauses)}}
      (seq bad-edges)
      {:refusal {:kind :application-order-evidence-owed :edges bad-edges}}
      (seq g-defects)
      {:refusal {:kind :co-application-group-invalid :defects g-defects}}
      :else
      (let [executable-nodes
            (mapv (fn [{:keys [occurrence-id] :as node}]
                    (let [clause (get operational-clauses occurrence-id)
                          hole (get holes occurrence-id)
                          base (select-keys node [:occurrence-id :pattern
                                                  :roles :fragment-indices
                                                  :origin :provenance])]
                      (cond-> (assoc base :enactment-status
                                     (if (and clause (:operation clause)
                                              (:completion-check clause)
                                              (:capability clause))
                                       :enactable :open-hole))
                        clause (assoc :clause clause)
                        hole (assoc :hole hole))))
                  (:nodes structural))
            record {:schema schema
                    :structural structural
                    :executable-nodes executable-nodes
                    :co-application-groups
                    (mapv #(assoc % :group-digest (group-digest %))
                          groups)
                    :relations {:application-order (vec application-order)
                                :guards (vec guards)}
                    :frontier (vec (remove
                                    #(contains? (set (map :to application-order))
                                                (:occurrence-id %))
                                    (filter #(= :enactable
                                                (:enactment-status %))
                                            executable-nodes)))
                    :journal-reference journal-reference-shape}
            ;; the census is bound inside the structural layer by its own
            ;; digest; the executable record cites both identities
            record (assoc record
                          :structural-digest (:digest structural)
                          :census (dissoc (:census structural) :census-digest)
                          :census-digest
                          (get-in structural [:census :census-digest]))]
        (assoc record :digest (digest record))))))

;; ---------------------------------------------------------------------------
;; Projection and persistence
;; ---------------------------------------------------------------------------

(defn project-structural
  "Forget executable additions: the exact structural diagram + census the
  record was built from."
  [record] (:structural record))

(defn save!
  "Append-only persistence: write the record as EDN under its digest.  If a
  file already exists at that path, it must contain the identical record —
  then the existing path is returned — otherwise the collision/mismatch is
  refused.  Never overwrites."
  [record dir]
  (let [root (io/file dir)
        _ (when-not (or (.isDirectory root) (.mkdirs root))
            (throw (ex-info "cascade store directory unavailable"
                            {:path (.getAbsolutePath root)})))
        f (io/file root (str (:digest record) ".edn"))]
    (if (.exists f)
      (let [existing (slurp f)]
        (if (= existing (pr-str record))
          (.getPath f)
          (throw (ex-info "cascade digest collision: existing file differs"
                          {:path (.getPath f)
                           :digest (:digest record)}))))
      (do (spit f (pr-str record))
          (.getPath f)))))

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
  an abstention or policy refusal."
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
        census (:census structural)
        foreign (vec (keep #(when-not (and (id-set (:from %)) (id-set (:to %))) %)
                           (concat app-order gluings shared
                                   (:reading-order-edges structural))))
        unconforming
        (vec (remove #(witness-conforming? (:witness %) (:from %) (:to %))
                     gluings))
        bad-shared
        (vec (filter #(witness-conforming? (:offered-witness %)
                                           (:from %) (:to %))
                     shared))
        provenance-bad (vec (keep #(let [bad (provenance-valid?
                                              (:provenance %))]
                                     (when (seq bad)
                                       {:occurrence-id (:occurrence-id %)
                                        :defects bad}))
                                  nodes))
        identity-collapse (vec (filter #(= (:occurrence-id %) (:pattern %))
                                       nodes))
        groups (:co-application-groups record)
        g-defects (group-defects groups id-set)
        g-digest-bad
        (vec (keep #(when-not (and (hex64? (:group-digest %))
                                   (= (:group-digest %) (group-digest %)))
                      (:id %))
                   groups))
        census-citations (map :citation (concat (:void census) (:unread census)))
        census-ok? (and (map? census)
                        (hex64? (:census-digest census))
                        (= (:census-digest census)
                           (census-digest census))
                        ;; nodes ARE the :open census population
                        (= (set (:open census)) (set node-ids))
                        ;; void/unread entries never appear as nodes
                        (empty? (cset/intersection
                                 (set census-citations) id-set))
                        (= (count census-citations)
                           (count (distinct census-citations))))
        structural-ok? (and (map? structural)
                             (= structural-schema (:schema structural))
                             (hex64? (:digest structural))
                             (= (:digest structural) (digest structural))
                             (vector? nodes)
                             (vector? (:construction-order structural)))
        binding-ok? (and (= (:structural-digest record) (:digest structural))
                         (= (:census-digest record)
                            (:census-digest census))
                         (= (:census record)
                            (dissoc census :census-digest)))
        bad-square-ids
        (vec (keep #(when-not (and (square/valid? (:square %))
                                   (= (get-in % [:provenance :square-digest])
                                      (evidence/value-digest (:square %))))
                      (:occurrence-id %))
                   nodes))
        cycle (application-cycle app-order node-ids)
        order-ok? (= (frequencies (map pr-str (:construction-order structural)))
                     (frequencies (map pr-str node-ids)))
        coverage-bad (vec (concat
                           (keep #(when (and (= :enactable (:enactment-status %))
                                             (not (and (get-in % [:clause :operation])
                                                       (get-in % [:clause :completion-check])
                                                       (get-in % [:clause :capability])))) %)
                                 exec-nodes)
                           (keep #(when (and (= :open-hole (:enactment-status %))
                                             (not (:hole %))) %)
                                 exec-nodes)))]
    (cond
      (not= schema (:schema record))
      (fail :identity-schema {:schema (:schema record) :expected schema})

      (not= (:digest record) (digest record))
      (fail :identity-digest {:digest (:digest record)
                              :recomputed (digest record)})

      (not structural-ok?)
      (fail :projection-structural-lost
            {:present (map? structural)
             :schema (:schema structural)})

      (not binding-ok?)
      (fail :projection-census-unbound
            {:structural-digest (:structural-digest record)
             :census-digest (:census-digest record)})

      (not census-ok?)
      (fail :census-population {:census census})

      (not= node-ids (distinct node-ids))
      (fail :population-duplicate-occurrence
            {:duplicates (vec (keys (filter #(> (val %) 1)
                                            (frequencies (map pr-str node-ids)))))})

      (seq identity-collapse)
      (fail :population-occurrence-collapsed-to-pattern
            {:nodes identity-collapse})

      (not= exec-ids id-set)
      (fail :population-executable-set
            {:executable (vec (sort-by pr-str exec-ids))
             :occurrences (vec (sort-by pr-str id-set))})

      ;; declared co-application groups: stable identity, exact membership,
      ;; no foreign/duplicated members, no overlap ambiguity, digest-bound
      (seq g-defects)
      (fail :co-application-groups-invalid {:defects g-defects})

      (seq g-digest-bad)
      (fail :groups-digest-broken {:groups g-digest-bad})

      (seq provenance-bad)
      (fail :provenance-missing {:occurrences provenance-bad})

      (seq foreign)
      (fail :graph-endpoint-foreign {:edges foreign})

      ;; every carried square is itself valid and its digest matches
      (seq bad-square-ids)
      (fail :pins-square-invalid {:occurrence-ids bad-square-ids})

      (seq unconforming)
      (fail :graph-gluing-witness-invalid {:edges unconforming})

      ;; a hand-built gluing with no witness at all
      (seq (remove :witness gluings))
      (fail :graph-gluing-unwitnessed
            {:edges (vec (remove :witness gluings))})

      (seq bad-shared)
      (fail :cascade-law-shared-name-not-gluing {:edges bad-shared})

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
                     :structural-digest (:digest structural)
                     :census-digest (:census-digest census)
                     :node-count (count nodes)
                     :executable-count (count exec-nodes)
                     :census {:open (count (:open census))
                              :void (count (:void census))
                              :unread (count (:unread census))}
                     :clauses [:identity-schema :identity-digest
                               :projection-structural :projection-census
                               :census-population :population
                               :provenance :graph-well-formedness :pins
                               :cascade-law :operational-coverage]}})))

(defn certified? [result] (= :certified (:status result)))
