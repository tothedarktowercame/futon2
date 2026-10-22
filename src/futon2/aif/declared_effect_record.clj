(ns futon2.aif.declared-effect-record
  "C2 pure producer/validator: freeze a target-authored declared-effect record
  at selection time, beside (never inside) the scoring certificate.

  Authority contract (claude-2 contract review + followup,
  ordinary-trial-5/declared-effect-contract-*-result.json; reviewer
  REQUEST_CHANGES round, declared-effect-record-independent-review.json):
  - The target's authored cascade-source declaration is the authority for
    wants, locators and horizon, pinned by path and sha256 of its file bytes.
    Only :target, :want, :locators and :horizon-steps are read. The
    declaration's :patterns/:candidates/:precedence/:facts are NEVER imported
    -- the legacy inner route must not leak into the selected compiled policy.
  - Each want's before state comes from an actual observation at the pinned
    base revision through the injected observation reader (default: the real
    C4 check, observation-checks/check-decl-in-file). An observation counts
    ONLY if its evidence proves it read the exact expected locator (:repo,
    :path, :decl), the C4 check, and the base revision reference, with a
    :resolved-sha shared by every row. Anything else -- missing evidence,
    mismatched evidence, a bare boolean, an unsupported locator class, an
    unresolvable or inconsistent base revision -- is a typed unknown and is
    NEVER recorded as observed-before false. Caller-asserted before values
    are never trusted.
  - A want already true at base is listed under :excluded-already-true with
    its observation, never silently dropped.
  - The horizon is the declared :horizon-steps or a typed absence. It is
    never defaulted (to cascade length or anything else).
  - All-true, no declaration, no horizon, a missing locator or an unknown
    observation stay in the record as typed evidence and make :admission
    :eligible false.
  - The record digest uses the existing action-identity canonical printer
    with this schema's tag. verify-record checks BOTH structural integrity
    (digest re-derivation, total EDN, no legacy fields) and authority
    (schema, typed rows, want census, unique target ownership, admission
    consistent with the record's own content, base-revision consistency).
    reconstruct validates the supplied record and then requires FULL
    canonical equality with the rebuilt record, not just stored-digest
    equality.
  - Only the C4 locator class is interpreted (the sole class the existing
    historical target declares). Other or missing classes are typed, never
    silently reinterpreted.

  Invalid or missing inputs produce typed refused records, never thrown
  exceptions. This namespace is a pure producer/validator. No runner wiring:
  the digest is destined for the occurrence mint inputs, not the selection
  certificate."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.observation-checks :as oc]))

(load-identity/register! *ns* *file*)

(def schema :wm/selected-effect-declaration-v1)

(def supported-locator-classes #{:C4})

(def default-declaration-dir
  "Same resolution rule as cascade-sources/default-dir: classpath first,
  working directory as fallback."
  (or (some-> (io/resource "wm/cascade-sources") .getPath)
      "resources/wm/cascade-sources"))

(defn- typed-refusal [reason details]
  {:status :refused :reason reason :details details})

(defn- sha256-of-bytes [^bytes bs]
  (identity/sha256 (String. bs java.nio.charset.StandardCharsets/UTF_8)))

(defn declaration-file-for
  "The path of the declared cascade-source whose :target is TARGET, or nil.
  Reads each *.edn in DIR (default the classpath cascade-sources dir) and
  matches on the declaration's own :target field."
  ([target] (declaration-file-for target default-declaration-dir))
  ([target dir]
   (when (and (string? target) (not (str/blank? target)))
     (->> (file-seq (io/file dir))
          (filter #(.isFile ^java.io.File %))
          (filter #(str/ends-with? (.getName ^java.io.File %) ".edn"))
          (sort-by #(.getPath ^java.io.File %))
          (some (fn [^java.io.File f]
                  (try
                    (when (= target (:target (edn/read-string (slurp f))))
                      (.getPath f))
                    (catch Throwable _ nil))))))))

(defn- valid-base-revision? [base-revision]
  (and (map? base-revision)
       (string? (:repo base-revision)) (not (str/blank? (:repo base-revision)))
       (string? (:sha base-revision)) (not (str/blank? (:sha base-revision)))))

(defn- read-declaration
  "Pinned read of the declaration source. Returns {:declaration .. :sha256 ..
  :path ..} or a typed refusal. Blank/missing paths are typed, not thrown."
  [{:keys [path expected-sha256] :as input}]
  (if-not (and (map? input) (string? path) (not (str/blank? path)))
    (typed-refusal :declaration-source-missing {:path path})
    (let [f (io/file path)]
      (if-not (.isFile f)
        (typed-refusal :declaration-source-missing {:path path})
        (let [bs (java.nio.file.Files/readAllBytes (.toPath f))
              sha (sha256-of-bytes bs)]
          (if (and (string? expected-sha256) (not= expected-sha256 sha))
            (typed-refusal :declaration-source-mismatch
                           {:path path :expected-sha256 expected-sha256
                            :actual-sha256 sha})
            (try
              (let [d (edn/read-string (String. bs java.nio.charset.StandardCharsets/UTF_8))]
                (if (map? d)
                  {:declaration d :sha256 sha :path path}
                  (typed-refusal :declaration-not-a-map {:path path})))
              (catch Throwable t
                (typed-refusal :declaration-unreadable
                               {:path path :error (ex-message t)})))))))))

(defn- declared-horizon [d]
  (let [h (:horizon-steps d)]
    (if (pos-int? h)
      {:status :declared :value h :authority :declaration/horizon-steps}
      {:status :absent :reason :horizon-not-declared})))

(defn- declared-wants
  "The authored want tokens as [target token] pairs. Only :want is read --
  never :facts (those are the legacy route's candidate fuel)."
  [target d]
  (let [raw (:want d)
        toks (cond (vector? raw) raw (keyword? raw) [raw] :else [])]
    (mapv (fn [t] (if (vector? t) t [target t])) toks)))

(defn- classify-reader-result
  "Reader output to a typed observation row, before evidence validation.
  Anything that is not a genuine boolean observation stays typed and NEVER
  becomes observed-before false."
  [result]
  (cond
    (and (map? result) (contains? result :status) (contains? result :observed))
    {:status :unknown :kind :contradictory-observation :reader-result result}
    (and (map? result) (contains? result :status))
    {:status :unknown :kind (:kind result :observation-reader-refused)
     :reader-result result}
    (and (map? result) (boolean? (:observed result)))
    {:status :observed :observed-before (:observed result)
     :check (:check result) :evidence (:evidence result)}
    :else
    {:status :unknown :kind :observation-not-boolean :reader-result result}))

(defn- evidence-proves-locator?
  "The observation's evidence must prove it read the exact locator that was
  asked for, through the C4 check, at the base revision reference."
  [base-revision at-base row]
  (let [ev (:evidence row)]
    (and (map? ev)
         (string? (:resolved-sha ev)) (not (str/blank? (:resolved-sha ev)))
         (= (:check row) :C4)
         (= (:repo ev) (:repo at-base))
         (= (:path ev) (:path at-base))
         (= (:decl ev) (:decl at-base))
         (= (:sha ev) (:sha base-revision)))))

(defn- validate-observation
  "Typed validation of one classified row against the requested locator and
  base revision. A row whose evidence does not prove the locator/base becomes
  a typed unknown; it can never be observed-before false."
  [base-revision at-base row]
  (if (not= :observed (:status row))
    row
    (if-not (evidence-proves-locator? base-revision at-base row)
      {:status :unknown :kind :observation-evidence-mismatch
       :expected {:repo (:repo at-base) :path (:path at-base)
                  :decl (:decl at-base) :check :C4
                  :sha (:sha base-revision)}
       :reader-result row}
      row)))

(defn- locator-refusal [locator kind]
  {:status :unknown :kind kind
   :locator (when (map? locator) locator)})

(defn- observe-want
  "Observe one want's locator at the base revision through the injected
  reader. The locator's authored :repo/:path/:decl and :class are kept; :sha
  is replaced by the base revision reference so the check resolves and pins
  it (:resolved-sha comes back in evidence and is validated). Only the C4
  class is interpreted; anything else is typed, never reinterpreted."
  [observe base-revision want locator]
  (let [class (get locator :class)
        fields-ok? (and (map? locator)
                        (string? (:repo locator)) (not (str/blank? (:repo locator)))
                        (string? (:path locator)) (not (str/blank? (:path locator)))
                        (string? (:decl locator)) (not (str/blank? (:decl locator))))
        class-ok? (contains? supported-locator-classes class)
        at-base (when (and fields-ok? class-ok?)
                  (assoc (select-keys locator [:repo :path :decl :class])
                         :sha (:sha base-revision)))]
    {:want want
     :locator at-base
     :locator-class (when (map? locator) class)
     :observation (cond
                    (not fields-ok?) (locator-refusal locator :missing-locator)
                    (not class-ok?) (locator-refusal locator :unsupported-locator-class)
                    :else (->> (observe at-base)
                               (classify-reader-result)
                               (validate-observation base-revision at-base)))}))

(defn record-digest
  "Digest of a record (any existing :record/digest is ignored) through the
  existing action-identity canonical printer. The :schema field IS covered."
  [record]
  (let [body (dissoc record :record/digest)]
    (identity/sha256 (identity/printed false [schema (identity/canonical body)]))))

(defn- finalize
  "Stamp schema and digest over the record body."
  [body]
  (let [b (assoc body :schema schema)]
    (assoc b :record/digest (record-digest b))))

(defn- empty-want-observations []
  {:active [] :excluded-already-true [] :unknown []})

(defn- invalid-input-record
  "Typed record for invalid build inputs (never thrown)."
  [target base-revision reason details]
  (finalize {:target target
             :declaration (typed-refusal reason details)
             :base-revision base-revision
             :horizon {:status :absent :reason :invalid-build-inputs}
             :wants []
             :want-observations (empty-want-observations)
             :excluded-already-true []
             :active-wants []
             :admission {:eligible false :refusals [:invalid-build-inputs]}}))

(defn- absent-record
  "Typed-absent declaration record: no wants observed, no horizon, admission
  refused. The absence itself is the recorded evidence."
  [target base-revision reason horizon-reason]
  (finalize {:target target
             :declaration {:status :absent :reason reason}
             :base-revision base-revision
             :horizon {:status :absent :reason horizon-reason}
             :wants []
             :want-observations (empty-want-observations)
             :excluded-already-true []
             :active-wants []
             :admission {:eligible false :refusals [:no-declared-effect]}}))

(defn- refused-read-record [target base-revision read]
  (finalize {:target target
             :declaration read
             :base-revision base-revision
             :horizon {:status :absent :reason :declaration-source-refused}
             :wants []
             :want-observations (empty-want-observations)
             :excluded-already-true []
             :active-wants []
             :admission {:eligible false
                         :refusals [:declaration-source-refused]}}))

(defn- base-consistency-refusals
  "Every observed row must resolve the SAME base revision."
  [rows]
  (let [resolved (keep #(get-in % [:observation :evidence :resolved-sha])
                       (filter #(= :observed (get-in % [:observation :status])) rows))]
    (if (or (empty? resolved) (apply = resolved))
      []
      [:observation-base-inconsistent])))

(defn build-record
  "Build the frozen declared-effect record.

  Inputs (all explicit; nothing is inferred):
    :target          string target id
    :declaration     {:path p :expected-sha256 sha?}, or nil to look the
                     declaration up by :target in default-declaration-dir;
                     a lookup that finds nothing is a typed absent record
    :base-revision   {:repo r :sha ref} the pinned revision wants are
                     observed at (the reader resolves the reference; the
                     evidence must prove it)
    :observe         injected observation reader (fn [locator] ...);
                     defaults to the real C4 check

  Returns the record map, always with :schema, :record/digest and
  :admission {:eligible bool :refusals [...]}. Typed refusals are carried
  inside the record, never thrown."
  [{:keys [target declaration base-revision observe] :as inputs}]
  (try
    (let [observe (or observe oc/check-decl-in-file)]
    (if (not (and (or (nil? declaration) (map? declaration))
                  (valid-base-revision? base-revision)))
      (invalid-input-record target base-revision :invalid-build-inputs
                            {:declaration declaration :base-revision base-revision})
      (let [decl-input (if (map? declaration)
                         declaration
                         (when-let [p (declaration-file-for target)]
                           {:path p}))]
        (if (nil? decl-input)
          (absent-record target base-revision :no-target-effect-declaration
                         :no-target-effect-declaration)
          (let [read (read-declaration decl-input)]
            (if (= :refused (:status read))
              (refused-read-record target base-revision read)
              (let [d (:declaration read)
                    target-ok? (= target (:target d))
                    horizon (declared-horizon d)
                    wants (if target-ok? (declared-wants target d) [])
                    rows (mapv (fn [want]
                                 (observe-want observe base-revision want
                                               (get (:locators d) (second want))))
                               wants)
                    observed-rows (filter #(= :observed (get-in % [:observation :status]))
                                          rows)
                    already-true (mapv #(select-keys % [:want :observation])
                                       (filter #(true? (get-in % [:observation :observed-before]))
                                               observed-rows))
                    active (mapv #(select-keys % [:want :locator :locator-class :observation])
                                 (filter #(false? (get-in % [:observation :observed-before]))
                                         observed-rows))
                    unknown (mapv #(select-keys % [:want :locator-class :observation])
                                  (remove #(= :observed (get-in % [:observation :status])) rows))
                    resolved (get-in (first observed-rows)
                                     [:observation :evidence :resolved-sha])
                    base (if resolved (assoc base-revision :resolved-sha resolved)
                             base-revision)
                    base-refusals (base-consistency-refusals rows)
                    no-open (and (seq wants) (empty? unknown) (empty? active)
                                 (empty? base-refusals))
                    refusals (cond-> (vec base-refusals)
                              (not target-ok?) (conj :declaration-target-mismatch)
                              (empty? wants) (conj :declaration-no-want)
                              (= :absent (:status horizon)) (conj :no-declared-horizon)
                              (seq unknown) (conj :unknown-observation)
                              no-open (conj :no-open-declared-want))
                    body {:target target
                          :declaration {:status :pinned :path (:path read)
                                        :sha256 (:sha256 read)}
                          :base-revision base
                          :horizon horizon
                          :wants (mapv :want rows)
                          :want-observations {:active active
                                              :excluded-already-true already-true
                                              :unknown unknown}
                          :excluded-already-true (mapv :want already-true)
                          :active-wants (mapv :want active)
                          :admission {:eligible (empty? refusals)
                                      :refusals refusals}}]
                (finalize body))))))))
    (catch Throwable t
      (invalid-input-record target base-revision :build-error
                            {:error (ex-message t)
                             :inputs (dissoc inputs :observe)}))))

(def legacy-fields
  "Declaration fields that must never enter the record: the legacy inner
  route's candidate fuel and pattern precedence."
  #{:patterns :candidates :precedence :facts :interpretation-receipts})

(defn- want-ok? [target w]
  (and (vector? w) (= 2 (count w)) (= target (first w))))

(defn- derived-refusals
  "Admission eligibility re-derived from the record's own content, so
  verify-record can detect a self-inconsistent record."
  [record]
  (let [decl (get-in record [:declaration :status])
        obs (:want-observations record)
        active (:active-wants record)
        unknown (vec (map :want (:unknown obs)))
        wants (:wants record)]
    (cond
    (= :invalid-build-inputs (get-in record [:horizon :reason]))
    [:invalid-build-inputs]
    (= :absent decl) [:no-declared-effect]
    (= :refused decl) [:declaration-source-refused]
    :else
    (cond-> []
      (= :absent (get-in record [:horizon :status])) (conj :no-declared-horizon)
      (or (empty? wants) (not-every? (partial want-ok? (:target record)) wants))
      (conj :declaration-no-want)
      (seq unknown) (conj :unknown-observation)
      (and (seq wants) (empty? unknown) (empty? active)) (conj :no-open-declared-want)))))

(defn verify-record
  "Structural integrity AND authority verification:
  - the digest re-derives from the record's own body through the canonical
    printer (proving total EDN) and no legacy declaration field is present;
  - the schema is exactly this namespace's schema;
  - every want is a unique [target token] pair owned by :target;
  - the want census closes: active + excluded-already-true + unknown exactly
    partition :wants;
  - :admission matches the eligibility re-derived from the record's own
    content, and rows' resolved base revisions are consistent with
    :base-revision."
  [record]
  (let [legacy (vec (filter legacy-fields (keys record)))
        digest-ok? (and (map? record)
                        (string? (:record/digest record))
                        (= (:record/digest record) (record-digest record)))
        schema-ok? (= schema (:schema record))
        wants (:wants record)
        dupes (->> wants frequencies (keep (fn [[w n]] (when (> n 1) w))) vec)
        owned? (and (string? (:target record))
                    (every? (partial want-ok? (:target record)) wants))
        active (:active-wants record)
        excluded (:excluded-already-true record)
        unknown (vec (keep :want (get-in record [:want-observations :unknown])))
        census-ok? (and (= (frequencies wants)
                            (frequencies (concat active excluded unknown)))
                        (= (count wants)
                           (+ (count active) (count excluded) (count unknown))))
        derived (derived-refusals record)
        admission-ok? (and (map? (:admission record))
                           (= (:refusals (:admission record)) derived)
                           (= (boolean (:eligible (:admission record)))
                              (empty? derived)))
        resolved (keep #(get-in % [:observation :evidence :resolved-sha])
                       (concat (get-in record [:want-observations :active])
                               (get-in record [:want-observations :excluded-already-true])))
        base-consistent? (or (empty? resolved)
                             (and (apply = resolved)
                                  (= (first resolved)
                                     (get-in record [:base-revision :resolved-sha]))))]
    (cond
      (not digest-ok?) {:verified false :reason :record-digest-mismatch}
      (seq legacy) {:verified false :reason :legacy-declaration-fields-present
                    :keys legacy}
      (not schema-ok?) {:verified false :reason :record-schema-mismatch
                        :schema (:schema record)}
      (or (seq dupes) (not owned?))
      {:verified false :reason :want-ownership-invalid
       :target (:target record) :wants wants :duplicates dupes}
      (not census-ok?) {:verified false :reason :want-census-mismatch}
      (not admission-ok?) {:verified false :reason :admission-inconsistent
                           :record (:admission record) :derived derived}
      (not base-consistent?) {:verified false :reason :base-revision-inconsistent}
      :else {:verified true :digest (:record/digest record)})))

(defn reconstruct
  "Validate the supplied record, rebuild from the same inputs, and require
  FULL canonical equality of the two bodies (not merely stored-digest
  equality). With the real reader this re-reads git at the pinned revision."
  [inputs record]
  (let [v (verify-record record)]
    (if-not (:verified v)
      v
      (let [rebuilt (build-record inputs)
            canon #(identity/canonical (dissoc % :record/digest))]
        {:verified (and (= (:record/digest record) (:record/digest rebuilt))
                        (= (canon record) (canon rebuilt)))
         :expected (:record/digest record)
         :rebuilt (:record/digest rebuilt)}))))
