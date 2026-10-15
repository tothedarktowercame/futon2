(ns futon2.aif.reading-policy-precision
  "Versioned identity and beta-continuity for reading-family precision,
   NOTE-wiring-reading-to-decision §7.5.4 (handoff 4): restate beta/model
   continuity over the pinned reading-family scoring model, and never let an
   old class/q0 identity carry beta into the progress/co-application model.

  Identity binds what the reading-family scorer actually consumed:
  - the scorer/kernel version (keyword, e.g. :co-application-frontier-theta-v1),
  - the pinned graph digest (the .pin's sha256 over the graph bytes),
  - the reading analysis and excerpt digests per target, in target-id order,
  - the structural policy family: stable policy keys/ids in canonical order
    (structural arrangement: nodes/edges shape, roles, occurrence ids), with
    numeric thetas and field enumeration order deliberately excluded,
  - the progress observation model (progress-token domain and observation
    schedule authority/version),
  - the horizon/preference authority/version,
  - any declared beta authority (:declared/:learned plus its version tag).

  Pure: no store, no IO beyond hashing.  Returns receipts/refusals; selects
  nothing."
  (:require [futon2.aif.interpretation-evidence :as evidence]))

(def schema :wm/reading-precision-identity-v1)
(def carry-schema :wm/reading-precision-carry-v1)

(defn- require! [p kind]
  (when-not p (throw (ex-info (name kind) {:kind kind :status :invalid}))))

(defn seal [value] (assoc value :sha256 (evidence/value-digest value)))

(defn intact?
  [value]
  (and (map? value)
       (= (:sha256 value) (evidence/value-digest (dissoc value :sha256)))))

;; -- canonical structural form ------------------------------------------------

(def ^:private numeric-learning-fields
  "Learning-value fields excluded wherever they appear (top level, nodes,
  edge maps): they are theta, not structure.  Every other field is kept."
  #{:theta :thetas :g :f :habit :computed-f :controller-score
    :G-efe :G-cascade :rank :habit-provenance-value})

(def ^:private structural-numeric-fields
  "Numeric fields that are legitimate structure, never theta."
  #{:fragment-index :precedence :stack-size})

(defn- strip-theta
  "Recursively remove numeric learning values and the named learning fields.
  A numeric value is dropped only when its key is neither in
  `numeric-learning-fields` (the learning vocabulary) nor in
  `structural-numeric-fields` (legitimate structural ids); named learning
  fields go even when their value is not numeric."
  [x]
  (cond
    (map? x) (into {}
                   (remove (fn [[k v]]
                             (boolean
                              (or (numeric-learning-fields k)
                                  (and (number? v)
                                       (not (structural-numeric-fields k)))))))
                   x)
    (sequential? x) (mapv strip-theta x)
    :else x))

(defn- canonical-arrangement
  "The explicit structural projection of an arrangement: per node, its
  occurrence id, pattern, sorted roles and structural indices; per edge, its
  endpoints and kind.  Nodes and edges each in canonical sorted order."
  [arrangement]
  (let [node (fn [n]
               (let [n (strip-theta n)]
                 {:occurrence-id (:occurrence-id n)
                  :pattern (:pattern n)
                  :roles (vec (sort (map str (:roles n))))
                  :fragment-indices (:fragment-indices n)
                  :fragment-index (:fragment-index n)}))
        edge (fn [e]
               (let [e (strip-theta e)]
                 (cond-> {:from (:from e) :to (:to e)}
                   (:kind e) (assoc :kind (:kind e)))))]
    (when arrangement
      {:nodes (vec (sort-by pr-str (map node (:nodes arrangement))))
       :edges (vec (sort-by pr-str (map edge (:edges arrangement))))})))

(defn- canonical-policy
  "A policy's structural identity: stable id/key, kind, target, kernel, and
  the canonical arrangement.  Numeric learning fields never enter."
  [policy]
  (require! (and (:policy-id policy) (:kind policy)) :invalid-policy-shape)
  (let [arrangement (canonical-arrangement (:arrangement policy))]
    (cond-> {:policy-id (:policy-id policy)
             :kind (:kind policy)
             :target-id (:target-id policy)
             :transition-kernel (:transition-kernel policy)}
      arrangement (assoc :arrangement arrangement))))

(defn- canonical-reading-digests
  "Reading digests keyed by target id, target-id sorted: map enumeration
  order can never change identity."
  [readings]
  (require! (map? readings) :invalid-readings)
  (into (sorted-map)
        (for [[target-id r] readings]
          (do (require! (and (string? (or (:analysis-digest r) (:analysis-sha256 r)))
                             (string? (or (:excerpt-digest r) (:excerpt-sha256 r))))
                        :invalid-reading-digest)
              [target-id {:analysis-digest (or (:analysis-digest r) (:analysis-sha256 r))
                          :excerpt-digest (or (:excerpt-digest r) (:excerpt-sha256 r))}]))))

(defn- canonical-policies
  "Structural policies, sorted by policy-id: enumeration order invariant."
  [policies]
  (require! (sequential? policies) :invalid-policies)
  (vec (sort-by :policy-id (map canonical-policy policies))))

(defn- canonical-progress-model
  "The progress observation model: token domain (sorted) and observation
  schedule authority/version.  Numeric rates are excluded: they are theta."
  [progress]
  (require! (map? progress) :invalid-progress-model)
  (require! (and (keyword? (get progress :authority))
                 (string? (get progress :version))) :invalid-progress-authority)
  {:authority (:authority progress)
   :version (:version progress)
   :progress-tokens (vec (sort (map str (:progress-tokens progress))))})

(defn- canonical-preference
  "Horizon/preference authority and version."
  [preference]
  (require! (map? preference) :invalid-preference)
  (require! (and (keyword? (get preference :authority))
                 (string? (get preference :version))
                 (nat-int? (get preference :horizon))) :invalid-preference-authority)
  (select-keys preference [:authority :version :horizon]))

(defn- canonical-beta-authority
  [beta-authority]
  (when beta-authority
    (require! (map? beta-authority) :invalid-beta-authority)
    (require! (contains? #{:declared :learned} (get beta-authority :status))
              :invalid-beta-status)
    (require! (string? (get beta-authority :version)) :invalid-beta-authority-version)
    (select-keys beta-authority [:status :version])))

;; -- identity ------------------------------------------------------------------

(defn identity-map
  "The pure identity map (unsealed).  Every argument is canonicalized first,
  so numeric theta values and field enumeration order cannot change it.
  :scorer-version and :kernel-version are bound independently and both are
  required in production: no alias or default may make a missing scorer
  look like a kernel or vice versa."
  [{:keys [scorer-version kernel-version graph-digest readings policies
           progress-model preference beta-authority]}]
  (require! (keyword? scorer-version) :invalid-scorer-version)
  (require! (keyword? kernel-version) :invalid-kernel-version)
  (require! (string? graph-digest) :invalid-graph-digest)
  {:schema schema
   :scorer-version scorer-version
   :kernel-version kernel-version
   :graph-digest graph-digest
   :readings (canonical-reading-digests readings)
   :policies (canonical-policies policies)
   :progress-model (canonical-progress-model progress-model)
   :preference (canonical-preference preference)
   :beta-authority (canonical-beta-authority beta-authority)})

(defn identity-of
  "The digest of the identity map: what beta continuity is defined over."
  [config]
  (evidence/value-digest (identity-map config)))

;; -- beta continuity -----------------------------------------------------------

(defn- rpp-seal [value] (seal value))

(defn- old-model? [previous]
  (contains? #{:wm/precision-carry-v1 :wm/precision-family-v1} (:schema previous)))

(defn- valid-previous-beta? [previous]
  (and (number? (:beta previous))
       (pos? (:beta previous))
       (Double/isFinite (double (:beta previous)))
       (string? (:identity previous))
       (= carry-schema (:schema previous))))

(defn carry-beta
  "Pure, self-consumable beta-continuity decision.  Every returned
  receipt/refusal is sealed by this module, so the result of call N feeds
  call N+1 directly.  Carries only when `previous` is intact, of this
  carry schema, with a finite positive beta and exactly the identity of
  `config`.  An old class/q0 model identity is a typed refusal, never a
  silent reset or preservation.  Returns a receipt map; selects nothing."
  [{:keys [previous initialized-beta config]}]
  (require! (and (number? initialized-beta) (pos? initialized-beta)
                (Double/isFinite (double initialized-beta)))
            :invalid-initialized-beta)
  (let [id (identity-of config)
        emit (fn [m] (rpp-seal (merge {:schema carry-schema :identity id} m)))]
    (cond
      (nil? previous)
      (emit {:status :initialized :beta initialized-beta :reason :no-previous-receipt})

      (not (intact? previous))
      (emit {:status :refused
             :reason :previous-receipt-tampered})

      (old-model? previous)
      (emit {:status :refused
             :reason :old-class-q0-model-identity
             :previous-schema (:schema previous)
             :note "old-model identity cannot carry into the reading-family model;
              re-declare beta over the reading-precision identity"})

      (not= carry-schema (:schema previous))
      (emit {:status :refused
             :reason :previous-receipt-schema-mismatch
             :previous-schema (:schema previous)})

      ;; intact, right schema: the beta itself must be carryable
      (not (valid-previous-beta? previous))
      (emit {:status :refused
             :reason :previous-beta-not-carryable
             :previous-beta (:beta previous)})

      (not= id (:identity previous))
      (emit {:status :refused
             :reason :reading-precision-identity-changed
             :previous-identity (:identity previous)
             ;; explicit re-declaration required; nothing is silently preserved
             :reinitialization {:policy :redeclare-beta}})

      :else
      (emit {:status :carried
             :beta (:beta previous)
             :previous-sha256 (:sha256 previous)}))))
