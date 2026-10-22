(ns futon2.aif.declared-effect-record
  "C2 pure producer/validator: freeze a target-authored declared-effect record
  at selection time, beside (never inside) the scoring certificate.

  Authority contract (claude-2 contract review + followup,
  ordinary-trial-5/declared-effect-contract-*-result.json):
  - The target's authored cascade-source declaration is the authority for
    wants, locators and horizon, pinned by path and sha256 of its file bytes.
    Only :target, :want, :locators and :horizon-steps are read. The
    declaration's :patterns/:candidates/:precedence/:facts are NEVER imported
    -- the legacy inner route must not leak into the selected compiled policy.
  - Each want's before state comes from an actual observation at the pinned
    base revision through the injected observation reader (default: the real
    C4 check, observation-checks/check-decl-in-file, which resolves the
    reference and returns :resolved-sha evidence). Caller-asserted before
    values are never trusted and never recorded as observations.
  - A want already true at base is listed under :excluded-already-true with
    its observation, never silently dropped.
  - The horizon is the declared :horizon-steps or a typed absence. It is
    never defaulted (to cascade length or anything else).
  - All-true, no declaration, no horizon, a missing locator or an
    unknown/contradictory observation stay in the record as typed evidence
    and make :admission :eligible false. An unknown observation is NEVER
    recorded as observed-before false.
  - The record digest uses the existing action-identity canonical printer
    (identity/printed + identity/canonical + identity/sha256) with this
    schema's tag; verify-record re-derives it, and reconstruct re-runs the
    build with the same inputs.

  This namespace is a pure producer/validator. No runner wiring: the digest
  is destined for the occurrence mint inputs, not the selection certificate."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.observation-checks :as oc]))

(load-identity/register! *ns* *file*)

(def schema :wm/selected-effect-declaration-v1)

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
   (->> (file-seq (io/file dir))
        (filter #(.isFile ^java.io.File %))
        (filter #(str/ends-with? (.getName ^java.io.File %) ".edn"))
        (sort-by #(.getPath ^java.io.File %))
        (some (fn [^java.io.File f]
                (try
                  (when (= target (:target (edn/read-string (slurp f))))
                    (.getPath f))
                  (catch Throwable _ nil)))))))

(defn- read-declaration
  "Pinned read of the declaration source. Returns {:declaration .. :sha256 ..
  :path ..} or a typed refusal."
  [{:keys [path expected-sha256]}]
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
                             {:path path :error (ex-message t)}))))))))

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

(defn- classify-observation
  "Reader output to a typed observation row. Anything that is not a genuine
  boolean observation stays typed and NEVER becomes observed-before false."
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

(defn- locator-refusal [locator]
  {:status :unknown :kind :missing-locator
   :locator (when (map? locator) locator)})

(defn- observe-want
  "Observe one want's locator at the base revision through the injected
  reader. The locator's authored :repo/:path/:decl are kept; :sha is replaced
  by the base revision reference so the check resolves and pins it
  (:resolved-sha comes back in evidence)."
  [observe base-revision want locator]
  (let [at-base (when (and (map? locator)
                           (string? (:repo locator)) (not (str/blank? (:repo locator)))
                           (string? (:path locator)) (not (str/blank? (:path locator)))
                           (string? (:decl locator)) (not (str/blank? (:decl locator))))
                 (assoc (select-keys locator [:repo :path :decl])
                        :sha (:sha base-revision)))]
    {:want want
     :locator at-base
     :observation (if at-base
                    (classify-observation (observe at-base))
                    (locator-refusal locator))}))

(defn record-digest
  "Digest of a record (any existing :record/digest is ignored) through the
  existing action-identity canonical printer. The :schema field IS covered."
  [record]
  (let [body (dissoc record :record/digest)]
    (identity/sha256 (identity/printed false [schema (identity/canonical body)]))))

(defn- finalize
  "Stamp schema and digest over the record body. The digest covers the body
  WITH its schema, so verify-record (which re-digests the full record minus
  :record/digest) reproduces it."
  [body]
  (let [b (assoc body :schema schema)]
    (assoc b :record/digest (record-digest b))))

(defn- absent-record
  "Typed-absent declaration record: no wants observed, no horizon, admission
  refused. The absence itself is the recorded evidence."
  [target base-revision reason horizon-reason]
  (finalize {:target target
             :declaration {:status :absent :reason reason}
             :base-revision base-revision
             :horizon {:status :absent :reason horizon-reason}
             :wants []
             :want-observations {:active [] :excluded-already-true [] :unknown []}
             :excluded-already-true []
             :active-wants []
             :admission {:eligible false :refusals [:no-declared-effect]}}))

(defn build-record
  "Build the frozen declared-effect record.

  Inputs (all explicit; nothing is inferred):
    :target          string target id
    :declaration     {:path p :expected-sha256 sha?}, or nil to look the
                     declaration up by :target in default-declaration-dir;
                     a lookup that finds nothing is a typed absent record
    :base-revision   {:repo r :sha ref} the pinned revision wants are
                     observed at (the reader resolves the reference)
    :observe         injected observation reader (fn [locator] ...);
                     defaults to the real C4 check

  Returns the record map, always with :schema, :record/digest and
  :admission {:eligible bool :refusals [...]}. Typed refusals are carried
  inside the record, never thrown."
  [{:keys [target declaration base-revision observe]
    :or {observe oc/check-decl-in-file}}]
  (let [decl-input (if (map? declaration)
                     declaration
                     (when (string? target)
                       (when-let [p (declaration-file-for target)]
                         {:path p})))]
    (if (nil? decl-input)
      (absent-record target base-revision :no-target-effect-declaration
                     :no-target-effect-declaration)
      (let [read (read-declaration decl-input)]
        (if (= :refused (:status read))
          (let [body {:target target
                      :declaration read
                      :base-revision base-revision
                      :horizon {:status :absent :reason :declaration-source-refused}
                      :wants []
                      :want-observations {:active [] :excluded-already-true []
                                          :unknown []}
                      :excluded-already-true []
                      :active-wants []
                      :admission {:eligible false
                                  :refusals [:declaration-source-refused]}}]
            (finalize body))
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
                active (mapv #(select-keys % [:want :locator :observation])
                             (filter #(false? (get-in % [:observation :observed-before]))
                                     observed-rows))
                unknown (mapv #(select-keys % [:want :observation])
                              (remove #(= :observed (get-in % [:observation :status])) rows))
                resolved (get-in (first observed-rows)
                                 [:observation :evidence :resolved-sha])
                base (if resolved (assoc base-revision :resolved-sha resolved)
                         base-revision)
                no-open (and (seq wants) (empty? unknown) (empty? active))
                refusals (cond-> []
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
            (finalize body)))))))

(def legacy-fields
  "Declaration fields that must never enter the record: the legacy inner
  route's candidate fuel and pattern precedence."
  #{:patterns :candidates :precedence :facts :interpretation-receipts})

(defn verify-record
  "Self-verification: the digest re-derives from the record's own body through
  the canonical printer (which also proves the record is total EDN), and the
  record carries no legacy declaration fields."
  [record]
  (let [legacy (vec (filter legacy-fields (keys record)))
        digest-ok? (= (:record/digest record) (record-digest record))]
    (cond
      (seq legacy) {:verified false :reason :legacy-declaration-fields-present
                    :keys legacy}
      (not digest-ok?) {:verified false :reason :record-digest-mismatch}
      :else {:verified true :digest (:record/digest record)})))

(defn reconstruct
  "Rebuild from the same inputs and compare digests. With the real reader
  this re-reads git at the pinned revision; with an injected reader it is
  fast and deterministic."
  [inputs record]
  (let [rebuilt (build-record inputs)]
    {:verified (= (:record/digest record) (:record/digest rebuilt))
     :expected (:record/digest record)
     :rebuilt (:record/digest rebuilt)}))
