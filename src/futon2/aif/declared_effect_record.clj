(ns futon2.aif.declared-effect-record
  "C2 pure producer/validator: freeze a target-authored declared-effect record
  at selection time, beside (never inside) the scoring certificate.

  Authority contract (claude-2 contract review + followup,
  ordinary-trial-5/declared-effect-contract-*-result.json; reviewer
  REQUEST_CHANGES rounds 1 and 2, declared-effect-record-*-review.json):
  - The target's authored cascade-source declaration is the authority for
    wants, locators and horizon, pinned by path and sha256 of its file bytes.
    Only :target, :want, :locators and :horizon-steps are read. The
    declaration's :patterns/:candidates/:precedence/:facts are NEVER imported
    -- the legacy inner route must not leak into the selected compiled policy.
  - The base revision is resolved INDEPENDENTLY of any observation return,
    through an injected revision resolver (default: a real
    git rev-parse --verify, mirroring observation-checks). An unresolvable
    base pins nothing: every want is a typed :base-unresolved unknown and
    nothing is observed. A locator whose authored :repo differs from the
    base revision's :repo is a typed :locator-repo-mismatch, never observed.
  - Each want's before state comes from an actual observation through the
    injected reader (default: the real C4 check). An observation counts ONLY
    if its evidence proves it read the exact expected locator (:repo, :path,
    :decl), the C4 check, the base revision reference AND the independently
    resolved base sha. Anything else -- missing or mismatched evidence, a
    bare boolean, an unsupported locator class, an inconsistent base -- is a
    typed unknown and is NEVER recorded as observed-before false.
  - A want already true at base is listed under :excluded-already-true with
    its observation, never silently dropped.
  - The horizon is the declared :horizon-steps or a typed absence. It is
    never defaulted. Admission eligibility requires a pinned declaration,
    a declared horizon, a resolved base and at least one open declared want;
    the refusal list is derived by ONE shared function used by both
    build-record and verify-record.
  - verify-record checks structural integrity (digest re-derivation, total
    EDN, no legacy fields) AND authority shape: exact schema, required
    well-formed :declaration/:base-revision/:horizon, row-level want census
    (actual observation rows, not summary lists; summaries must equal the
    rows), evidence/base consistency, and admission matching the shared
    derivation. It verifies CONSISTENCY of a record's claims -- it does not
    establish that the claims are true; reconstruct re-derives the record
    from the pinned inputs (re-reading the declaration bytes at the pinned
    sha256) and requires full canonical equality.
  - Only the C4 locator class is interpreted. Other or missing classes are
    typed, never silently reinterpreted.

  Invalid or missing inputs produce typed refused records, never thrown
  exceptions. This namespace is a pure producer/validator with no runner
  wiring: the record digest is destined for the runner's occurrence-context
  binding (delivered as a separate binding receipt), not for the scoring
  certificate and not for the occurrence mint itself."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.observation-checks :as oc]))

(load-identity/register! *ns* *file*)

(def schema :wm/selected-effect-declaration-v1)

(def supported-locator-classes #{:C4})

(def valid-declaration-statuses #{:pinned :absent :refused})
(def valid-horizon-statuses #{:declared :absent})

(def default-declaration-dir
  "Same resolution rule as cascade-sources/default-dir: classpath first,
  working directory as fallback."
  (or (some-> (io/resource "wm/cascade-sources") .getPath)
      "resources/wm/cascade-sources"))

(defn default-resolve-revision
  "Production revision resolver: independently resolve {:repo r :sha ref} to
  a commit through git, mirroring observation-checks' private
  resolve-reference. Returns {:repo .. :reference .. :resolved-sha ..} or a
  typed {:status :missing :kind :unknown-sha ..}. Unit tests inject a
  resolver that models revision resolution."
  [{:keys [repo sha]}]
  (let [{:keys [exit out err]}
        (sh/sh "git" "-C" (str oc/repo-root "/" repo)
               "rev-parse" "--verify" "--end-of-options" (str sha "^{commit}"))]
    (if (zero? exit)
      {:repo repo :reference sha :resolved-sha (str/trim out)}
      {:status :missing :kind :unknown-sha :repo repo :sha sha
       :stderr (str/trim err)})))

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

(defn- want-owned? [target w]
  (and (vector? w) (= 2 (count w)) (= target (first w))))

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

(defn- evidence-shape-ok? [ev]
  (and (map? ev)
       (string? (:repo ev)) (not (str/blank? (:repo ev)))
       (string? (:path ev)) (not (str/blank? (:path ev)))
       (string? (:decl ev)) (not (str/blank? (:decl ev)))
       (string? (:sha ev))
       (string? (:resolved-sha ev)) (not (str/blank? (:resolved-sha ev)))))

(defn- evidence-proves-locator?
  "The observation's evidence must prove it read the exact locator that was
  asked for, through the C4 check, at the base revision reference, and must
  carry the INDEPENDENTLY RESOLVED base sha (not an echoed string)."
  [base-pinned at-base row]
  (let [ev (:evidence row)]
    (and (evidence-shape-ok? ev)
         (= (:check row) :C4)
         (= (:repo ev) (:repo at-base))
         (= (:path ev) (:path at-base))
         (= (:decl ev) (:decl at-base))
         (= (:sha ev) (:sha base-pinned))
         (= (:resolved-sha ev) (:resolved-sha base-pinned)))))

(defn- validate-observation
  "Typed validation of one classified row against the requested locator and
  the pinned base. A row whose evidence does not prove the locator/base
  becomes a typed unknown; it can never be observed-before false."
  [base-pinned at-base row]
  (if (not= :observed (:status row))
    row
    (if-not (evidence-proves-locator? base-pinned at-base row)
      {:status :unknown :kind :observation-evidence-mismatch
       :expected {:repo (:repo at-base) :path (:path at-base)
                  :decl (:decl at-base) :check :C4
                  :sha (:sha base-pinned)
                  :resolved-sha (:resolved-sha base-pinned)}
       :reader-result row}
      row)))

(defn- base-unresolved-row [want locator-class resolution]
  {:want want :locator nil :locator-class locator-class
   :observation {:status :unknown :kind :base-unresolved
                 :resolution resolution}})

(defn- observe-want
  "Observe one want's locator at the pinned base revision through the
  injected reader. Repo ownership first: a locator authored for another repo
  is typed, never observed. Only the C4 class is interpreted."
  [observe base-pinned want locator]
  (let [class (get locator :class)
        fields-ok? (and (map? locator)
                        (string? (:repo locator)) (not (str/blank? (:repo locator)))
                        (string? (:path locator)) (not (str/blank? (:path locator)))
                        (string? (:decl locator)) (not (str/blank? (:decl locator))))
        class-ok? (contains? supported-locator-classes class)
        repo-ok? (and fields-ok? (= (:repo locator) (:repo base-pinned)))
        at-base (when (and fields-ok? class-ok? repo-ok?)
                  (assoc (select-keys locator [:repo :path :decl :class])
                         :sha (:sha base-pinned)))]
    {:want want
     :locator at-base
     :locator-class (when (map? locator) class)
     :observation (cond
                    (not fields-ok?) {:status :unknown :kind :missing-locator
                                      :locator (when (map? locator) locator)}
                    (not class-ok?) {:status :unknown :kind :unsupported-locator-class
                                     :locator locator}
                    (not repo-ok?) {:status :unknown :kind :locator-repo-mismatch
                                    :locator locator :base-repo (:repo base-pinned)}
                    :else (->> (observe at-base)
                               (classify-reader-result)
                               (validate-observation base-pinned at-base)))}))

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

(defn- admission-refusals
  "THE single authority for admission eligibility. build-record calls it with
  computed facts; verify-record calls it with facts read back from the
  record, so the two can never disagree without failing verification."
  [{:keys [invalid? declaration-status horizon-status wants-owned?
           base-resolved? base-consistent? unknown? active?]}]
  (if invalid?
    [:invalid-build-inputs]
    (cond-> []
      (= :absent declaration-status) (conj :no-declared-effect)
      (= :refused declaration-status) (conj :declaration-source-refused)
      (and (= :pinned declaration-status) (not wants-owned?))
      (conj :declaration-no-want)
      (and (= :pinned declaration-status) (not= :declared horizon-status))
      (conj :no-declared-horizon)
      (and (= :pinned declaration-status) (not base-resolved?))
      (conj :base-unresolved)
      unknown? (conj :unknown-observation)
      (not base-consistent?) (conj :observation-base-inconsistent)
      (and (= :pinned declaration-status) wants-owned? base-resolved?
           base-consistent? (not unknown?) (not active?))
      (conj :no-open-declared-want))))

(defn build-record
  "Build the frozen declared-effect record.

  Inputs (all explicit; nothing is inferred):
    :target            string target id
    :declaration       {:path p :expected-sha256 sha?}, or nil to look the
                       declaration up by :target in default-declaration-dir;
                       no hit => typed absent record
    :base-revision     {:repo r :sha ref}: resolved INDEPENDENTLY (see
                       :resolve-revision) before any observation; the resolved
                       sha is what observation evidence must carry
    :observe           injected observation reader (fn [locator] ...);
                       defaults to the real C4 check
    :resolve-revision  injected revision resolver (fn [{:repo :sha}] ...);
                       defaults to a real git rev-parse. Unit tests inject a
                       resolver that models revision resolution.

  Returns the record map, always with :schema, :record/digest and
  :admission {:eligible bool :refusals [...]}. Typed refusals are carried
  inside the record, never thrown."
  [{:keys [target declaration base-revision observe resolve-revision] :as inputs}]
  (try
    (let [observe (or observe oc/check-decl-in-file)
          resolve-revision (or resolve-revision default-resolve-revision)]
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
                      ;; Independent base resolution BEFORE any observation.
                      resolution (resolve-revision base-revision)
                      base-pinned (if (:status resolution)
                                    base-revision
                                    (assoc base-revision :resolved-sha
                                           (:resolved-sha resolution)))
                      rows (if (:status resolution)
                             (mapv (fn [w]
                                     (base-unresolved-row
                                      w (get-in d [:locators (second w) :class])
                                      resolution))
                                   wants)
                             (mapv (fn [want]
                                     (observe-want observe base-pinned want
                                                   (get (:locators d) (second want))))
                                   wants))
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
                      resolved-shas (keep #(get-in % [:observation :evidence :resolved-sha])
                                          observed-rows)
                      base-consistent? (or (empty? resolved-shas) (apply = resolved-shas))
                      refusals (admission-refusals
                                {:declaration-status :pinned
                                 :horizon-status (:status horizon)
                                 :wants-owned? (and target-ok? (seq wants))
                                 :base-resolved? (not (:status resolution))
                                 :base-consistent? base-consistent?
                                 :unknown? (boolean (seq unknown))
                                 :active? (boolean (seq active))})
                      body {:target target
                            :declaration {:status :pinned :path (:path read)
                                          :sha256 (:sha256 read)}
                            :base-revision base-pinned
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
                             :inputs (dissoc inputs :observe :resolve-revision)}))))

(def legacy-fields
  "Declaration fields that must never enter the record: the legacy inner
  route's candidate fuel and pattern precedence."
  #{:patterns :candidates :precedence :facts :interpretation-receipts})

(defn- observation-row-ok? [kind row]
  (cond
    (= :unknown kind)
    (and (map? row) (vector? (:want row))
         (map? (:observation row)) (= :unknown (:status (:observation row))))
    :else
    (and (map? row) (vector? (:want row))
         (map? (:observation row))
         (= :observed (:status (:observation row)))
         (boolean? (:observed-before (:observation row)))
         (= (:observed-before (:observation row)) (= kind :excluded-already-true))
         (= :C4 (:check (:observation row)))
         (evidence-shape-ok? (:evidence (:observation row))))))

(defn- census-problem
  "Row-level census: the actual observation rows must be well formed, the
  summary token lists must equal the row tokens, and every declared want must
  appear in exactly one bucket's rows."
  [record]
  (let [obs (:want-observations record)
        buckets (juxt :active :excluded-already-true :unknown)
        [active excluded unknown] (if (map? obs) (buckets obs) [nil nil nil])
        row-wants (fn [rows] (mapv :want rows))]
    (when (or (not (map? obs))
              (not (every? sequential? [active excluded unknown]))
              (not (every? map? (concat active excluded unknown)))
              (not (every? (partial observation-row-ok? :active) active))
              (not (every? (partial observation-row-ok? :excluded-already-true) excluded))
              (not (every? (partial observation-row-ok? :unknown) unknown))
              (not= (frequencies (:wants record))
                    (frequencies (concat (row-wants active)
                                         (row-wants excluded)
                                         (row-wants unknown))))
              (not= (vec (:active-wants record)) (vec (row-wants active)))
              (not= (vec (:excluded-already-true record)) (vec (row-wants excluded))))
      :want-census-mismatch)))

(defn- record-facts
  "Facts read back from a record for the shared admission derivation."
  [record]
  (let [obs (:want-observations record)
        unknown (vec (:unknown obs))
        active (vec (:active obs))
        resolved (keep #(get-in % [:observation :evidence :resolved-sha])
                       (concat active (:excluded-already-true obs)))
        base-resolved? (and (map? (:base-revision record))
                            (string? (get-in record [:base-revision :resolved-sha]))
                            (not (str/blank? (get-in record [:base-revision :resolved-sha]))))
        base-consistent? (or (empty? resolved)
                             (and (apply = resolved)
                                  base-resolved?
                                  (= (first resolved)
                                     (get-in record [:base-revision :resolved-sha]))))]
    {:invalid? (= :invalid-build-inputs (get-in record [:horizon :reason]))
     :declaration-status (get-in record [:declaration :status])
     :horizon-status (get-in record [:horizon :status])
     :wants-owned? (and (string? (:target record)) (seq (:wants record))
                        (every? (partial want-owned? (:target record)) (:wants record)))
     :base-resolved? base-resolved?
     :base-consistent? base-consistent?
     :unknown? (boolean (seq unknown))
     :active? (boolean (seq active))}))

(defn verify-record
  "Structural integrity AND authority-shape verification:
  - the digest re-derives from the record's own body through the canonical
    printer (proving total EDN) and no legacy declaration field is present;
  - the schema is exactly this namespace's schema;
  - :declaration/:horizon carry valid statuses and required fields
    (:pinned needs path+sha256; :declared needs a pos-int value);
  - the want census is checked on the ACTUAL observation rows (statuses,
    evidence shapes, summary/row equality);
  - :admission matches the shared admission derivation over facts read back
    from the record, and rows' resolved base revisions agree with
    :base-revision.

  This verifies the record's internal consistency and shape authority. It
  does NOT establish that the record's claims are true; only reconstruct
  (re-derivation from pinned inputs) can do that."
  [record]
  (let [legacy (vec (filter legacy-fields (keys record)))
        digest-ok? (and (map? record)
                        (string? (:record/digest record))
                        (= (:record/digest record) (record-digest record)))
        schema-ok? (= schema (:schema record))
        decl-status (get-in record [:declaration :status])
        decl-ok? (and (contains? valid-declaration-statuses decl-status)
                      (or (not= :pinned decl-status)
                          (and (string? (get-in record [:declaration :path]))
                               (string? (get-in record [:declaration :sha256])))))
        horizon-status (get-in record [:horizon :status])
        horizon-ok? (and (contains? valid-horizon-statuses horizon-status)
                         (or (not= :declared horizon-status)
                             (pos-int? (get-in record [:horizon :value]))))
        census (census-problem record)
        facts (record-facts record)
        derived (if (or (not decl-ok?) (not horizon-ok?))
                  [:record-shape-invalid]
                  (admission-refusals facts))
        admission-ok? (and (map? (:admission record))
                           (= (:refusals (:admission record)) derived)
                           (= (boolean (:eligible (:admission record)))
                              (empty? derived)))]
    (cond
      (not digest-ok?) {:verified false :reason :record-digest-mismatch}
      (seq legacy) {:verified false :reason :legacy-declaration-fields-present
                    :keys legacy}
      (not schema-ok?) {:verified false :reason :record-schema-mismatch
                        :schema (:schema record)}
      (not decl-ok?) {:verified false :reason :record-shape-invalid
                      :field :declaration}
      (not horizon-ok?) {:verified false :reason :record-shape-invalid
                         :field :horizon}
      census {:verified false :reason census}
      (not admission-ok?) {:verified false :reason :admission-inconsistent
                           :record (:admission record) :derived derived}
      :else {:verified true :digest (:record/digest record)})))

(defn reconstruct
  "Validate the supplied record, re-consume the pinned declaration BYTES
  (re-reading :path and checking the sha256 pin -- a changed or lying
  declaration fails :declaration-pin-broken), rebuild from the same inputs,
  and require FULL canonical equality of the two bodies."
  [inputs record]
  (let [v (verify-record record)]
    (cond
      (not (:verified v)) v
      (= :pinned (get-in record [:declaration :status]))
      (let [re-read (read-declaration
                     {:path (get-in record [:declaration :path])
                      :expected-sha256 (get-in record [:declaration :sha256])})]
        (if (= :refused (:status re-read))
          {:verified false :reason :declaration-pin-broken
           :details (:details re-read)}
          (let [rebuilt (build-record inputs)
                canon #(identity/canonical (dissoc % :record/digest))]
            {:verified (and (= (:record/digest record) (:record/digest rebuilt))
                            (= (canon record) (canon rebuilt)))
             :expected (:record/digest record)
             :rebuilt (:record/digest rebuilt)})))
      :else
      (let [rebuilt (build-record inputs)
            canon #(identity/canonical (dissoc % :record/digest))]
        {:verified (and (= (:record/digest record) (:record/digest rebuilt))
                        (= (canon record) (canon rebuilt)))
         :expected (:record/digest record)
         :rebuilt (:record/digest rebuilt)}))))
