(ns futon2.aif.find-receipt
  "Receipted find over captured bytes. No live library reads or construction.
  Reader, warrant and core find ported from futon3/checks/find_organise.clj
  lines 84-258, commit f49da8ee791b966a7bc077629337c4b68369436f.
  Adaptation: byte-reader injection, pinned digests, strict interpreted guards,
  independent F2 expectations and F1-F4 validation. Canonical ids are keywords
  of the captured family/name path, never @flexiarg aliases."
  (:refer-clojure :exclude [find])
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]))

(defn- need! [ok law reason data]
  (when-not ok (throw (ex-info "Receipted find refused"
                              (merge {:finding :find/refusal :law law :reason reason} data)))))

(def target-pattern #"[A-Za-z0-9_.-]+/[A-Za-z0-9_./'-]+")
(def edge-directive-re #"\s*@(why-posthoc|why|how|see-also)\s+(.+?)\s*")

(defn- normalise [s]
  (some-> s str/trim (str/replace #"\s+" " ")))

(defn- clause-block
  "The `+ LABEL:` block of a flexiarg body, with the 1-based line span of its
   content.  find_snatch.clj:31-44, moved here unchanged."
  [lines label]
  (when-let [marker (first (keep-indexed
                            (fn [i line]
                              (when (re-matches
                                     (re-pattern (str "^\\s+\\+ " label ":\\s*$"))
                                     line)
                                i))
                            lines))]
    (let [content (->> (subvec lines (inc marker))
                       (take-while #(and (not (str/blank? %))
                                         (not (re-matches #"^\s+\+ \S.*" %))))
                       vec)]
      (when (seq content)
        {:lines [(+ marker 2) (+ marker 1 (count content))]
         :text (normalise (str/join " " content))}))))

(defn qualified
  "The canonical pattern id: section-qualified, as the library graph lint keys
   it (`library_graph_lint.clj:174-177`), so that a cross-section `@why` target
   resolves instead of turning into a bare name that collides."
  [section local]
  (keyword (str (name section) "/" (name local))))

(defn local
  "The bare name, for callers whose artefacts are keyed inside one section."
  [id]
  (keyword (name id)))

(defn- parse-pattern-file [library-root section file source captured]
  (let [lines (vec (str/split-lines (String. ^bytes captured "UTF-8")))
        path-id (qualified section (str/replace (.getName ^java.io.File file)
                                                #"\.flexiarg$" ""))
        directive-id (some #(some-> (re-matches #"@flexiarg (\S+)" %) second keyword)
                           lines)
        if-clause (clause-block lines "IF")
        however-clause (clause-block lines "HOWEVER")
        edges (into []
                    (comp (map-indexed vector)
                          (mapcat (fn [[i line]]
                                    (let [code (first (str/split line #";;" 2))]
                                      (when-let [[_ kind tail] (re-matches edge-directive-re code)]
                                        (for [token (str/split tail #"\s+")
                                              :when (re-matches target-pattern token)]
                                          {:from path-id :to (keyword token)
                                           :kind (keyword kind) :line (inc i)}))))))
                    lines)]
    [path-id (cond-> {:id path-id
                      :directive-id directive-id
                      ;; Derived from `library-root`, not the literal "library":
                      ;; `citations-verified` (construct_cascade.clj:465) SLURPS this
                      ;; path, so a repository read from another checkout -- worklist
                      ;; row :LA6 reads futon3c/library/alfworld in place rather than
                      ;; importing it -- must cite where the file actually is.  For
                      ;; `library-root` "library" the string is unchanged, which is
                      ;; what the byte-identical regeneration of the four existing
                      ;; cascade artefacts checks.
                      :file (str library-root "/" (name section) "/" (.getName ^java.io.File file))
                      :edges edges :source source :captured-lines lines}
               if-clause (assoc :if-lines (:lines if-clause) :if-text (:text if-clause))
               however-clause (assoc :however-lines (:lines however-clause)
                                     :however-text (:text however-clause)))]))

(defn- descend
  "Every id reachable from `start` under `rel`, `start` excluded unless it is
   reachable from itself."
  [rel start]
  (loop [seen #{} frontier (get rel start #{})]
    (if (empty? frontier)
      seen
      (let [seen' (into seen frontier)]
        (recur seen' (set/difference (into #{} (mapcat #(get rel % #{})) frontier)
                                     seen'))))))

(defn read-repository
  "Read only SOURCE companion bytes through READ-BYTES. Original paths supply
  family/name layout and citations, never file contents. Cycles are refused."
  [library-root sources read-bytes]
  (let [prefix (str (str/replace library-root #"/+$" "") "/")
        pins (filter #(and (str/starts-with? (:path %) prefix)
                           (str/ends-with? (:path %) ".flexiarg")) sources)
        rows (mapv (fn [s]
                     (let [relative (subs (:path s) (count prefix))
                           parts (str/split relative #"/")
                           bs (read-bytes (:file s))]
                       (need! (= 2 (count parts)) :F1 :unsupported-library-layout {:path (:path s)})
                       (need! (and (bytes? bs) (= (:sha256 s) (evidence/sha256 bs)))
                              :F3 :source-digest-mismatch {:source (:id s)})
                       (parse-pattern-file (str/replace library-root #"/+$" "")
                                           (first parts) (io/file (:path s)) s bs))) pins)
        _ (need! (= (count rows) (count (set (map first rows)))) :F1 :duplicate-path-id {})
        entries (into (sorted-map) rows)
        patterns (set (keys entries))
        all-edges (sort-by (juxt :from :to :kind) (filter #(= :why (:kind %)) (mapcat :edges (vals entries))))
        authored (filter #(contains? patterns (:to %)) all-edges)
        dangling (remove #(contains? patterns (:to %)) all-edges)
        stands-on (reduce (fn [m {:keys [from to]}] (update m from (fnil conj #{}) to)) {} authored)
        cycles (vec (sort (filter #(contains? (descend stands-on %) %) patterns)))
        mismatches (into (sorted-set) (keep #(when (not= (:id %) (:directive-id %)) (:id %))) (vals entries))]
    (need! (empty? cycles) :F1 :cyclic-repository {:cycles cycles})
    {:library-root library-root :sections (vec (sort (set (map namespace patterns))))
     :patterns patterns :entries entries :stands-on stands-on :edges (vec authored)
     :dangling (vec dangling) :acyclic? true :cycles cycles :id-directive-mismatches mismatches
     :digest (evidence/value-digest (into (sorted-map) (map (fn [[id e]] [id (select-keys (:source e) [:path :sha256])])) entries))
     :findings (mapv (fn [id] {:kind :find/id-directive-mismatch :path-id id
                               :directive-id (get-in entries [id :directive-id])}) mismatches)}))

(defn warrant
  "F3's citation: the pattern's own file and the line span of the clause the
   receipt names.  A receipt that cites this cites TEXT, never a score."
  [repository id]
  (let [{:keys [file if-lines if-text however-lines however-text]}
        (get-in repository [:entries id])]
    (cond-> (sorted-map :file file :if-lines if-lines :if-text if-text)
      however-text (assoc :however-lines however-lines
                          :however-text however-text))))

;; ---------------------------------------------------------------------------
;; find -- Holes.lean:264
;; ---------------------------------------------------------------------------

(defn- reference-find
  "Tension -> Repository -> FindResult.

   The tension supplies its own antecedent evaluator, `:fires?`, a predicate on
   `[id context]`.  That is the whole of what is domain-specific: Snatch reads a
   game state, another domain reads whatever its states are, and neither can
   reach a pattern the repository does not hold -- F1 is true by CONSTRUCTION
   here, because the candidate set IS `repository.patterns`.  (`find_snatch.clj`
   filtered a separately held runner collection and then checked containment
   afterwards.  The check it kept -- that no runner id is unauthored -- is the
   other direction and stays where it is.)

   `:receipt` may add fields, but supplying any core key (`:if`, `:route`,
   `:warrant`) throws a typed :receipt-core-key-collision naming the pattern
   and keys. No caller may overwrite the evaluator's result or warrant (F3)."
  [{:keys [context fires? route receipt] :or {route :structured-antecedent}} repository]
  (let [firing (into [] (filter #(fires? % context)) (sort (:patterns repository)))]
    (sorted-map
     :absence (when (empty? firing) :no-pattern-addresses-this-tension)
     :receipts (into (sorted-map)
                     (map (fn [id]
                            (let [core (sorted-map :if true :route route
                                                   :warrant (warrant repository id))
                                  extension (when receipt (receipt id))
                                  collisions (vec (filter #(contains? extension %)
                                                          (keys core)))]
                              (when (seq collisions)
                                (throw (ex-info "Find receipt extension supplies reserved core keys"
                                                {:finding :receipt-core-key-collision
                                                 :pattern id :keys collisions})))
                              [id (into core extension)])))
                     firing)
     :selected (vec firing))))

(defn guard-value
  "Strong Kleene Boolean evaluation: NOT unknown is unknown, never true.
  Only literal true fires. AND false and OR true remain decisive."
  [facts guard]
  (case (first guard)
    :fact (get facts (second guard) :unknown)
    :not (let [v (guard-value facts (second guard))] (if (= :unknown v) :unknown (not v)))
    :and (let [vs (map #(guard-value facts %) (rest guard))]
           (cond (some false? vs) false (every? true? vs) true :else :unknown))
    :or (let [vs (map #(guard-value facts %) (rest guard))]
          (cond (some true? vs) true (every? false? vs) false :else :unknown))
    (need! false :F2 :guard-invalid {:guard guard})))

(defn- canonical! [repository pattern source-id]
  (let [id (keyword pattern) e (get-in repository [:entries id])]
    (need! (some? e) :F1
           (if (some #(= id (:directive-id %)) (vals (:entries repository)))
             :directive-form-id :pattern-outside-repository) {:pattern pattern})
    (need! (= source-id (get-in e [:source :id])) :F1 :pattern-source-mismatch {:pattern pattern})
    id))

(defn- text-at [entry [a b :as span]]
  (need! (and (= 2 (count span)) (pos-int? a) (pos-int? b)
               (<= a b (count (:captured-lines entry)))) :F3 :invalid-span {:span span})
  (str/join "\n" (subvec (:captured-lines entry) (dec a) b)))

(defn- clause-expectation [entry clause label]
  (let [[a b] (:lines clause) [lo hi] (get entry (keyword (str (name label) "-lines")))]
    ;; The agent may cite a subspan, but cannot label arbitrary text as IF.
    ;; Expectations read that span from the independently captured IF block.
    (need! (and lo hi (<= lo a b hi)) :F2 :not-authored-clause
           {:pattern (:id entry) :clause label :span (:lines clause)})
    {:text (text-at entry (:lines clause)) :lines (:lines clause)}))

(defn context
  "Compile expectations independently from captured pattern bytes. Pass this
  context separately to validate-result!; never construct it from FindResult.
  The packet-1 record is revalidated here, including every source and citation."
  [record read-bytes library-root]
  (evidence/validate-sources! record read-bytes)
  (need! (and (= :wm/interpreted-pattern-set-v1 (:schema record)) (nil? (:failure record)))
         :F1 :successful-interpretation-required {})
  (let [repository (read-repository library-root (:sources record) read-bytes)
        sources (into {} (map (juxt :id identity)) (:sources record))
        facts (into {} (map (juxt :id :value)) (:facts record))
        as-of {:target-sha256 (get-in sources [(get-in record [:target :source]) :sha256])
               :repository-sha256 (:digest repository) :pinned-at (get-in record [:target :pinned-at])}
        candidates (mapcat :candidates (get-in record [:retrieval :runs]))
        _ (doseq [c candidates :when (string? (:source c))]
            (canonical! repository (:pattern c) (:source c)))
        interpretations (into (sorted-map)
                              (map (fn [x] [(canonical! repository (:pattern x) (:source x)) x]))
                              (:interpretations record))
        expectations (into (sorted-map)
                           (for [[id x] interpretations
                                 :let [entry (get-in repository [:entries id])]]
                             [id {:clause-kind :if-clause
                                  :acknowledged-clause (clause-expectation entry (get-in x [:clauses :if]) :if)
                                  :route :structured-antecedent :as-of as-of}]))
        _ (doseq [[id x] interpretations]
            (clause-expectation (get-in repository [:entries id]) (get-in x [:clauses :however]) :however))
        values (into {} (map (fn [[id x]] [id (guard-value facts (:guard x))])) interpretations)
        deferred (mapv (fn [id] {:pattern id :guard-value (get values id :unknown)
                                 :reason (if (contains? interpretations id) :guard-not-true :interpretation-missing)})
                       (sort (set (for [c candidates
                                        :when (and (get-in c [:judgment :relevant?])
                                                   (not (true? (get values (keyword (:pattern c))))))]
                                    (keyword (:pattern c))))))]
    {:repository repository :expectations expectations :interpretations interpretations
     :values values :deferred deferred :as-of as-of}))

(def core-keys #{:if :route :warrant :clause-kind :acknowledged-clause :as-of :citation :however})
(defn extend-receipt
  "Extensions never overwrite evaluator, expectation or citation core fields."
  [receipt extension]
  (let [collisions (set/intersection core-keys (set (keys extension)))]
    (need! (empty? collisions) :F3 :receipt-core-key-collision {:keys collisions})
    (merge receipt extension)))

(defn- validate-citation! [repository id citation]
  (case (:kind citation)
    :pattern-text
    (let [entry (get-in repository [:entries id]) s (:source entry)]
      (need! (and (= (:path s) (:path citation)) (= (:sha256 s) (:sha256 citation))
                   (= (text-at entry (:lines citation)) (:quote citation)))
             :F3 :citation-mismatch {:pattern id}))
    :authored-edges
    (let [tail (:tail citation) chain (into [id] tail)]
      (need! (and (vector? tail) (every? (:patterns repository) tail)
                   (every? (fn [[a b]] (contains? (get-in repository [:stands-on a] #{}) b))
                           (partition 2 1 chain))) :F3 :invalid-authored-descent {:pattern id}))
    (need! false :F3 :citation-kind-invalid {:pattern id})))

(defn validate-result!
  "F1-F4 against independent CONTEXT and external DESIGNATED set (or nil).
  The interpreter's relevance judgments never serve as the F4 designation.
  Designated ids, like repository ids, must be family/name keywords."
  [{:keys [repository expectations values] :as ctx} designated result]
  (need! (and (or (nil? designated) (set? designated))
               (every? #(and (keyword? %) (namespace %)) designated))
         :F4 :canonical-designation-set-required {})
  (let [selected (:selected result) ids (set selected)
        designated (or designated #{}) applicable (set/intersection designated (:patterns repository))]
    (need! (and (vector? selected) (= (count ids) (count selected))
                 (set/subset? ids (:patterns repository))) :F1 :selection-outside-repository {})
    (need! (= (:digest repository) (:repository-sha256 result)) :F3 :repository-digest-mismatch {})
    (need! (= ids (set (keys (:receipts result)))) :F2 :receipt-coverage-mismatch {})
    (need! (or (seq ids) (= :no-pattern-addresses-this-tension (:absence result))) :F1 :absence-required {})
    (doseq [id ids :let [receipt (get-in result [:receipts id]) expected (get expectations id)]]
      (need! (and expected (true? (get values id))) :F2 :pattern-not-firing {:pattern id})
      (need! (= expected (select-keys receipt [:clause-kind :acknowledged-clause :route :as-of]))
             :F2 :expectation-mismatch {:pattern id})
      (need! (and (true? (:if receipt)) (= (warrant repository id) (:warrant receipt)))
             :F3 :core-warrant-mismatch {:pattern id})
      (validate-citation! repository id (:citation receipt))
      (let [x (get-in ctx [:interpretations id])]
        (need! (= (get-in x [:clauses :however]) (:however receipt)) :F3 :however-mismatch {:pattern id})))
    (need! (empty? (set/intersection ids applicable)) :F4 :designated-pattern-fired {:designated applicable})
    (need! (= (if (seq applicable) :discriminating :vacuous) (:f4 result)) :F4 :designation-status-mismatch {})
    result))

(defn find
  "Receipted find; not yet wired into construction. Packet 1 only represents
  text clause citations, so this producer emits pattern-text; the validator
  also checks the Lean authoredEdges chain form. No descent is invented."
  ([record read-bytes library-root designated] (find record read-bytes library-root designated {}))
  ([record read-bytes library-root designated {:keys [receipt-extension]}]
   (let [{:keys [repository expectations interpretations values] :as ctx} (context record read-bytes library-root)
         result (reference-find {:fires? (fn [id _] (true? (get values id)))} repository)
         receipts (into (sorted-map)
                        (for [[id receipt] (:receipts result)
                              :let [x (get interpretations id) entry (get-in repository [:entries id])
                                    c (get-in x [:clauses :if])]]
                          [id (extend-receipt
                               (merge receipt (get expectations id)
                                      {:acknowledged-clause {:text (:quote c) :lines (:lines c)}
                                       :citation {:kind :pattern-text :path (get-in entry [:source :path])
                                                  :sha256 (get-in entry [:source :sha256])
                                                  :lines (:lines c) :quote (:quote c)}
                                       :however (get-in x [:clauses :however])})
                               (when receipt-extension (receipt-extension id)))]))
         result (assoc result :receipts receipts :repository-sha256 (:digest repository)
                       :deferred (:deferred ctx) :findings (:findings repository)
                       :f4 (if (seq (set/intersection (or designated #{}) (:patterns repository))) :discriminating :vacuous))]
     (validate-result! ctx designated result))))
