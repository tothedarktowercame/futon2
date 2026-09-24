(ns futon2.aif.cascade-equivalence
  "Predeclared cascade-equivalence relation for PROOF-2 Step 0 / REPAIR-PLAN
  B4 condition 8. Offline and pure: it reads only the candidate maps already
  present in the run record (see [:decision :selection-certificate
  :candidates] of tick-run-record-2026-09-23-1790131591.edn); no clicks, no
  JVM reload, no data/ writes.

  DECISION (claude-8, 2026-09-24, overriding the PROOF-2 draft's literal
  N(π)): theta is EXCLUDED from the normalization tuple. A learned parameter
  must not split one cascade into two; theta enters only through
  effect-equivalence-at, where it can change a kernel row. The draft's
  sentence 'differing theta alone counts only when it changes a kernel row'
  is preserved exactly by this exclusion.

  The kernels are NOT redefined here: cascade-kernel, first-enabled,
  pattern-kernel and missing-interpretation are the Lean-bound
  (CascadeTransition.lean) implementations in futon2.aif.cascade-model-manifest.
  This namespace only normalizes and compares their outputs."
  (:require [clojure.set :as set]
            [futon2.aif.cascade-model-manifest :as manifest]))

(def ^:private bad-provenance-kinds
  "Provenance kinds that make a candidate inadmissible anywhere in its
  transitive source chain (PROOF-2 P₀ / B4 amendment condition 8)."
  #{:hand-admitted :proof-fixture :reference-field})

(defn- canonical
  "Canonical form for hashing: sets sorted by their printed token
  representation, maps sorted by key, sequences kept in order (precedence is
  ORDERED and that order is semantic). Doubles print as the CERT-S v1 tagged
  literal #wm/double \"0x…p…\" (Double/toHexString) so no bit is lost."
  [x]
  (cond
    (map? x) (into (sorted-map-by (fn [a b] (compare (pr-str a) (pr-str b))))
                   (map (fn [[k v]] [k (canonical v)]))
                   x)
    (set? x) (mapv canonical (sort-by pr-str x))
    (sequential? x) (mapv canonical x)
    (double? x) (tagged-literal 'wm/double (Double/toHexString x))
    :else x))

(defn canonical-sha256
  "CERT-S v1 §3 content hash: sha256 of the canonical EDN bytes of the
  value, written \"sha256:<hex>\". Shared by the candidate-derivations
  carrier's :candidate-payload-sha256 so every section that inlines a
  candidate payload can join by id AND hash. Doubles hash as the
  #wm/double tagged form; ratios and integers print exactly; a reader
  recomputes, never trusts an embedded hash (BJ-3)."
  [x]
  (str "sha256:" (manifest/sha256 (pr-str (canonical x)))))

(defn- content-hash
  "sha256 of the canonical printed form. Deterministic across JVMs because
  the canonical form fixes set order and map order before printing."
  [x]
  (manifest/sha256 (pr-str (canonical x))))

(defn- cascade-base
  "Accepts either a bare cascade map {:target … :precedence […]} or a
  run-record candidate whose payload sits under :id
  {:kind :cascade-candidate :id … :target … :precedence […]}. Ids, kind tags,
  timestamps and author names are never read."
  [cascade]
  (if (and (map? (:id cascade)) (seq (:precedence (:id cascade))))
    (:id cascade)
    cascade))

(def ^:private prose-keys
  "Keys carrying prose readings, discarded per the Step 0 CHECK: ids,
  timestamps, author names, prose readings, storage paths and presentation
  order go; the acceptance PREDICATE itself stays (it is semantic)."
  #{:notes :reading :rationale :prose})

(defn- strip-prose [m] (into {} (remove #(contains? prose-keys (key %))) m))

(defn- pattern-tuple
  "The per-pattern element of N(π) after interpretation: consumes, forbids,
  produces and an authority hash. Theta deliberately absent (see ns
  docstring). Prose readings, ids, :source paths and :theta-source tags are
  discarded by construction — only these four fields are lifted."
  [p]
  {:consumes (reduce set/union #{} (map :present (get-in p [:guard :clauses])))
   :forbids (reduce set/union #{} (map :absent (get-in p [:guard :clauses])))
   :produces (set (or (:produces p) (get-in p [:transition :produces]) #{}))
   :authority-sha256 (content-hash (:authority p))})

(defn normalize
  "N(π) from the Step 0 CHECK: target-authority hash; the ordered list of
  per-pattern (consumes, forbids, produces, authority-hash) tuples after
  interpretation; observation-locator hashes; acceptance-predicate hash;
  feasible-scope hash. Ids, timestamps, author names, prose readings and
  presentation order are discarded, except that a path that is itself an
  acceptance locator is kept inside the locator hash. Returns
  {:normalized <tuple> :normalized-cascade-sha256 <sha>} — the sha is the
  field the certificate carries — or the typed refusal inherited from
  manifest/missing-interpretation when some firing pattern is uninterpreted."
  [cascade]
  (let [base (cascade-base cascade)
        precedence (:precedence base)]
    (if-let [hole (manifest/missing-interpretation precedence)]
      {:status :missing :kind (:kind hole) :pattern (:pattern hole)}
      (let [tuple {:target-authority-sha256
                   (content-hash {:target (:target base)
                                  :target-authority (or (:target-authority base)
                                                        (:authority base))})
                   :interpreted-patterns (mapv pattern-tuple precedence)
                   :observation-locator-sha256s
                   (mapv content-hash (sort-by pr-str (canonical (:locators cascade))))
                   :acceptance-predicate-sha256 (content-hash (strip-prose (:acceptance cascade)))
                   :feasible-scope-sha256 (content-hash (strip-prose (:scope cascade)))}]
        {:normalized tuple
         :normalized-cascade-sha256 (content-hash tuple)}))))

(defn equivalent?
  "N(π₁) = N(π₂). Two cascades are equivalent iff their normalized tuples
  (and therefore their normalized-cascade-sha256s) are equal. A refusal from
  normalize propagates instead of being coerced to false."
  [c1 c2]
  (let [n1 (normalize c1) n2 (normalize c2)]
    (cond
      (:status n1) n1
      (:status n2) n2
      :else (= (:normalized n1) (:normalized n2)))))

(defn- refusal? [x] (and (map? x) (contains? x :status)))

(defn- precedence-of [c] (:precedence (cascade-base c)))

(defn effect-equivalent-at?
  "Effect-equivalence at the recorded initial state s₀ (Step 0 CHECK):
  firstEnabled is none for both, or the two cascadeKernel rows at s₀ are
  pointwise equal over the finite token-state carrier. The kernel rows are
  the sparse exact maps from manifest/cascade-kernel, so map equality IS
  pointwise equality (zero-mass entries are omitted by the kernel itself).
  A kernel refusal propagates as the typed refusal, never a boolean."
  [c1 c2 s0]
  (let [p1 (precedence-of c1) p2 (precedence-of c2)
        k1 (manifest/cascade-kernel p1 s0)
        k2 (manifest/cascade-kernel p2 s0)]
    (cond
      (refusal? k1) k1
      (refusal? k2) k2
      (and (nil? (manifest/first-enabled p1 s0))
           (nil? (manifest/first-enabled p2 s0)))
      true
      :else (= k1 k2))))

(defn distinct-with-differing-effects?
  "'Distinct with differing declared effects' (Step 0 CHECK): requires both
  N(π₁) ≠ N(π₂) AND non-effect-equivalence at s₀. Returns a verdict map,
  never a bare boolean:
    {:verdict :equivalent        :reason …}  N(π₁) = N(π₂)
    {:verdict :effect-equivalent :reason …}  same N-difference, equal rows
    {:verdict :distinct          :reason …}  the B4 admission predicate
  plus each side's :normalized-cascade-sha256. Refusals propagate."
  [c1 c2 s0]
  (let [n1 (normalize c1) n2 (normalize c2)]
    (cond
      (:status n1) n1
      (:status n2) n2
      :else
      (let [sha1 (:normalized-cascade-sha256 n1)
            sha2 (:normalized-cascade-sha256 n2)
            ee (effect-equivalent-at? c1 c2 s0)]
        (cond
          (refusal? ee) ee
          (= sha1 sha2) {:verdict :equivalent
                         :reason "N(π₁) = N(π₂): identical normalized tuples"
                         :normalized-cascade-sha256 [sha1 sha2]}
          (true? ee) {:verdict :effect-equivalent
                      :reason "N(π₁) ≠ N(π₂) but cascadeKernel rows are pointwise equal at s₀"
                      :normalized-cascade-sha256 [sha1 sha2]}
          :else {:verdict :distinct
                 :reason "N(π₁) ≠ N(π₂) and cascadeKernel rows differ at s₀"
                 :normalized-cascade-sha256 [sha1 sha2]})))))

(defn- scan-provenance
  "Depth-first scan of one provenance subtree. Offends when any nested map's
  :kind is a bad provenance kind, at any depth — not only the top level."
  [path x]
  (when (map? x)
    (or (when (contains? bad-provenance-kinds (:kind x))
          {:path path :kind (:kind x)})
        (some (fn [[k v]] (scan-provenance (conj path k) v)) x))))

(defn admissible-provenance?
  "False when :hand-admitted, proof-fixture or reference-field provenance
  appears ANYWHERE in the candidate's transitive source chain. Walks
  :interpretation, :construction, :review-publication, :admission and every
  nested :source inside them, to any depth. Returns {:admissible true} or
  {:admissible false :path […] :kind …} naming the offending path (the path
  is into the derivation map, e.g. [:construction :source])."
  [derivation]
  (if-let [hit (some (fn [k] (scan-provenance [k] (get derivation k)))
                     [:interpretation :construction :review-publication :admission])]
    {:admissible false :path (:path hit) :kind (:kind hit)}
    {:admissible true}))
