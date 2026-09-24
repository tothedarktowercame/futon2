(ns futon2.aif.cascade-sources
  "Declared cascade sources for the production tick. Each file in
  resources/wm/cascade-sources/ declares one target: its fact and want tokens
  with locators, its interpreted patterns with receipts, its constructed
  candidates with construction receipts, and its context's beta.

  Facts are not declared true or false. They are observed on every load
  through futon2.aif.observation-checks (WM-04, P5):
  - observed present => true;
  - a valid check observing absence => false;
  - a refused check (no current warrant, no locator, unknown sha) => :unknown.
  Unknown is never false (D3).

  load-declared returns the source map futon2.aif.cascade-problems/assemble
  takes, plus :files (path and sha256 of every file read) and :observations
  (every check result or refusal). :read-occurrences is the ordered provenance
  authority: unlike the legacy per-target maps it preserves duplicate targets.
  :target-collisions surfaces those duplicates without deciding merge policy."
  (:require [futon2.aif.load-identity :as load-identity]
            [futon2.aif.token-initialization-policy :as token-policy]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.live-c :as live-c]
            [futon2.aif.observation-checks :as oc]))

(load-identity/register! *ns* *file*)

(def ^:dynamic *read-occurrences*
  "Optional run-owned atom. Nil means no completed declaration read or empty scan observed."
  nil)

(defn provenance [occurrences]
  {:schema :wm/declaration-reads-v1
   :status (cond (nil? occurrences) :not-observed (empty? occurrences) :absent :else :present)
   :reason (when (and (some? occurrences) (empty? occurrences)) :none-supplied)
   :occurrences (vec occurrences)
   :target-collisions
   (into {} (for [[target reads] (group-by :target (map-indexed #(assoc %2 :index %1) occurrences))
                  :when (> (count reads) 1)]
              [target (mapv #(select-keys % [:index :path :sha256]) reads)]))})

(def default-dir
  "Where the declared sources live, resolved on the CLASSPATH rather than
  relative to the working directory: the tick runs in the serving JVM, whose
  working directory is futon3c, and a relative path there found no files and
  loaded no sources at all — silently, since \"no sources\" is a legitimate
  state. The relative path remains the fallback for a JVM without futon2's
  resources on its classpath."
  ;; Resolved from a sibling FILE resource, never by asking the classloader
  ;; for the directory itself. The Test Registry's runner records every
  ;; resource a run looks up and the registry hashes each one, so a directory
  ;; lookup made every warrant unmintable with
  ;; :closure-unavailable "(Is a directory)" -- which is how this was found.
  (or (some-> (io/resource "wm/observation-contract.edn")
              io/file .getParentFile (io/file "cascade-sources") .getPath)
      "resources/wm/cascade-sources"))

(defn- refuse! [reason data]
  (throw (ex-info (str "cascade-sources: " (name reason))
                  (assoc data :error :invalid-cascade-source :reason reason))))

(defn- file-sha [f] (evidence/sha256 (java.nio.file.Files/readAllBytes (.toPath f))))

(def ^:dynamic *code-roots*
  "Additional code roots against which a receipt :source's relative :path
  resolves during replay admission, tried after
  futon2.aif.observation-checks/repo-root. A receipt binds ONLY to bytes
  that hash to its pinned :sha256: listing a root never weakens the pin --
  when no admitted root holds the pinned bytes the refusal stands
  (:interpretation-source-unreadable when the file is absent everywhere,
  :interpretation-source-hash-mismatch otherwise). Empty (default):
  production behaviour unchanged, byte for byte.

  Replay of a RECORDED decision needs this when two pin generations share
  one relative path: the current declared sources pin futon3 at one
  revision while a recorded decision pins the same files at an older one
  (RUNNER-SUITE-D, futon2 795d4001). A single code root cannot hold two
  generations at one path; a vector of generation trees can
  (test/fixtures/library-pins/<generation>/..., see its README)."
  [])

(defn- read-receipt-source
  "Bind a document-backed interpretation to bytes read during admission.
   Source-less judgement receipts remain judgement receipts. A supplied hash
   is a pin: disagreement refuses rather than rebinding an old reading."
  [receipt]
  (if-not (contains? receipt :source)
    receipt
    (let [source (:source receipt)
          path (:path source)]
      (when-not (and (string? path) (not (str/blank? path)))
        (refuse! :interpretation-source-path {:source source}))
      (let [file (io/file path)
            files (if (.isAbsolute file)
                    [file]
                    (mapv #(io/file % path) (cons oc/repo-root *code-roots*)))
            attempts (mapv (fn [f]
                             (try {:file f :hash (file-sha f)}
                                  (catch java.io.IOException e
                                    {:file f :hash ::unreadable :exception (.getName (class e))})
                                  (catch SecurityException e
                                    {:file f :hash ::unreadable :exception (.getName (class e))})))
                           files)
            pin (:sha256 source)
            bound (if (contains? source :sha256)
                    (some #(when (= pin (:hash %)) %) attempts)
                    (some #(when (not= ::unreadable (:hash %)) %) attempts))]
        (if bound
          (assoc receipt :source (assoc source :sha256 (:hash bound)))
          (let [readable (filter #(not= ::unreadable (:hash %)) attempts)]
            (if (seq readable)
              (refuse! :interpretation-source-hash-mismatch
                       {:path path :declared pin :observed (:hash (first readable))})
              (refuse! :interpretation-source-unreadable
                       {:path path :exception (or (:exception (first attempts))
                                                  "java.nio.file.NoSuchFileException")}))))))))

(defn observation-schedule
  "A declared observation clock, independent of C's preference placement.
   Omission is held, never inferred from the preference schedule or horizon."
  [declaration]
  (if-not (contains? declaration :observation-schedule)
    {:status :held :reason :observation-placement-not-declared}
    (let [schedule (:observation-schedule declaration)
          tau (:tau schedule)]
      (when-not (and (map? schedule) (= #{:tau} (set (keys schedule)))
                     (map? tau) (= #{:value :status} (set (keys tau)))
                     (= :declared (:status tau))
                     (integer? (:value tau)) (<= 0 (:value tau)))
        (refuse! :invalid-observation-schedule {:value schedule}))
      schedule)))

(defn- check-file! [path d]
  (when-not (= :wm/cascade-source-v1 (:schema d))
    (refuse! :schema {:path path :schema (:schema d)}))
  (doseq [k [:target :context :beta :facts :want :locators :patterns :interpretation-receipts :candidates]]
    (when-not (contains? d k) (refuse! :missing-key {:path path :key k})))
  (when-not (and (number? (get-in d [:beta :value])) (pos? (get-in d [:beta :value]))
                 (#{:declared :learned} (get-in d [:beta :status])))
    (refuse! :beta {:path path :beta (:beta d)}))
  ;; A source-declared common horizon must be a positive integer when present
  ;; (PROOF-wm-works 1.3 build 2/3, 2026-09-22); absence declares nothing.
  (when (contains? d :horizon-steps)
    (when-not (and (integer? (:horizon-steps d)) (pos? (:horizon-steps d)))
      (refuse! :invalid-horizon-steps {:path path :value (:horizon-steps d)})))
  (doseq [[token locator] (:locators d)
          field [:class :check]
          :let [check (get locator field)]
          :when (#{:C1 :C2} check)]
    (refuse! :removed-observation-check
             {:path path :target (:target d) :token token :check check :field field}))
  d)

(defn canonical-pattern-id
  "D17: one canonical pattern-id form, the namespaced keyword. Authors have
  written the same id in three spellings -- seat A keywords, seat B
  namespaced strings (the E-cascade-real probe files on disk; the D17 note
  remembers them as symbols) -- and a naive comparison saw two patterns
  where there was one. The loader canonicalises at load so the constructor
  and any agreement check see one id. An id with no namespace, or of any
  other type, is a typed refusal naming the path and value, never a silent
  string coercion."
  [id path field]
  (let [canonical
        (cond
          (keyword? id) (when (namespace id) id)
          (symbol? id) (when (namespace id) (keyword (namespace id) (name id)))
          (string? id) (let [slash (str/index-of id "/")]
                         (when (and slash (pos? slash) (< slash (dec (count id))))
                           (keyword (subs id 0 slash) (subs id (inc slash)))))
          :else nil)]
    (when-not canonical
      (refuse! :invalid-pattern-id {:path path :field field :value id}))
    canonical))

(defn- normalize-pattern-ids
  "Canonicalise every pattern id in a checked declaration: the keys of
  :patterns, each pattern's own :id, the keys of :interpretation-receipts,
  and each candidate's :precedence entries (bare ids and :id inside pattern
  maps). Returns [declaration' n] where n counts the ids rewritten; the
  per-file occurrence records n as :id-normalization so provenance shows
  the rewrite happened."
  [d path]
  (let [rewritten (volatile! 0)
        canon (fn [field id]
                (let [c (canonical-pattern-id id path field)]
                  (when (not= c id) (vswap! rewritten inc))
                  c))
        patterns (into {}
                       (map (fn [[id pat]]
                              [(canon :patterns id)
                               (if (and (map? pat) (contains? pat :id))
                                 (assoc pat :id (canon :patterns (:id pat)))
                                 pat)]))
                       (:patterns d))
        receipts (into {}
                       (map (fn [[id receipt]] [(canon :interpretation-receipts id) receipt]))
                       (:interpretation-receipts d))
        candidates (mapv (fn [candidate]
                           (if (contains? candidate :precedence)
                             (update candidate :precedence
                                     (fn [entries]
                                       (mapv (fn [entry]
                                               (cond
                                                 (and (map? entry) (contains? entry :id))
                                                 (assoc entry :id (canon :precedence (:id entry)))
                                                 (map? entry) entry
                                                 :else (canon :precedence entry)))
                                             entries)))
                             candidate))
                         (:candidates d))]
    [(assoc d :patterns patterns
             :interpretation-receipts receipts
             :candidates candidates)
     @rewritten]))

(defn- observe-facts
  "Fact tokens to true/false/:unknown through their locators."
  [facts locators]
  (let [located (into {} (for [f facts] [f (get locators f)]))
        {:keys [observed results refused]} (oc/observe located)]
    {:universe (into {} (for [f facts]
                          [f (cond (contains? observed f) true
                                   (contains? results f) false
                                   :else :unknown)]))
     :observations {:results results :refused refused}}))

(defn load-declared
  "Read every *.edn under DIR (default resources/wm/cascade-sources) and build
  cascade-problems sources. An empty or missing directory gives nil, so the
  caller records :none-supplied. A malformed file throws; it is never skipped."
  ([] (load-declared default-dir))
  ([dir]
   (let [files (->> (file-seq (io/file dir))
                    (filter #(.isFile %))
                    (filter #(.endsWith (.getName %) ".edn"))
                    (sort-by #(.getPath %)))
         ;; Look each source up BY RESOURCE NAME as well, when it lives on the
         ;; classpath. Enumerating with file-seq alone reads plain file paths,
         ;; which the Test Registry's recording loader never sees -- so a
         ;; warrant would not pin the declared sources, and editing a target's
         ;; declaration would leave every warrant looking current. This is what
         ;; makes a source change stale the warrants that depend on it
         ;; (zai-16's finding, 2026-09-17: the directory entry was their only
         ;; trace, and it killed the warrant instead of pinning them).
         _ (doseq [f files]
             (when-let [u (io/resource (str "wm/cascade-sources/" (.getName f)))]
               (.getPath u)))]
     (when (and (empty? files) *read-occurrences*)
       (swap! *read-occurrences* #(or % [])))
     (when (seq files)
       (let [merged (reduce
        (fn [acc f]
          (let [path (.getPath f)
                snapshot (java.nio.file.Files/readAllBytes (.toPath f))
                hash (evidence/sha256 snapshot)
                d (check-file! path (edn/read-string (String. snapshot java.nio.charset.StandardCharsets/UTF_8)))
                [d id-normalization] (normalize-pattern-ids d path)
                policy (get d :token-initialization token-policy/disabled)
                _ (when-not (token-policy/valid-policy? policy)
                    (refuse! :invalid-token-initialization-policy {:path path :value policy}))
                receipts (into {} (map (fn [[id receipt]] [id (read-receipt-source receipt)]))
                               (:interpretation-receipts d))
                scales (live-c/preference-scales d)
                schedule (live-c/preference-schedule d)
                observation-clock (observation-schedule d)
                t (:target d)
                prior-beta (get-in acc [:beta-by-context (:context d)])
                this-beta {:beta (get-in d [:beta :value]) :status (get-in d [:beta :status])}
                _ (when (and prior-beta (not= prior-beta this-beta))
                    (refuse! :incommensurable-family {:context (:context d)
                                                     :rates [prior-beta this-beta]}))
                {:keys [universe observations]} (observe-facts (:facts d) (:locators d))
                occurrence {:path path :sha256 hash :target t :observations observations
                            :token-initialization-policy policy
                            :id-normalization id-normalization}
                _ (when *read-occurrences* (swap! *read-occurrences* (fnil conj []) occurrence))]
            (-> acc
                (assoc-in [:token-initialization t]
                          {:policy policy :declaration-sha256 hash :locators (:locators d)
                           :schedule observation-clock :observations observations})
                (assoc-in [:universes t] universe)
                (assoc-in [:wants t] (vec (:want d)))
                (assoc-in [:preference-scales t] scales)
                (assoc-in [:preference-schedules t] schedule)
                (assoc-in [:observation-schedules t] observation-clock)
                (assoc-in [:locators t] (:locators d))
                (assoc-in [:interpretations t] {:patterns (:patterns d)
                                                :receipts receipts})
                (assoc-in [:candidates t] (vec (:candidates d)))
                (assoc-in [:beta-by-context (:context d)] {:beta (get-in d [:beta :value]) :status (get-in d [:beta :status])})
                (assoc-in [:context-by-target t] (:context d))
                (update :files (fnil conj []) {:path path :sha256 hash :target t})
                (update :read-occurrences (fnil conj []) occurrence)
                (update :target-collisions
                        (fn [collisions]
                          (if (contains? (:universes acc) t)
                            (assoc collisions t :multiple-declarations)
                            (or collisions {}))))
                (update :horizon-steps-declarations
                        (fn [ds]
                          (if (contains? d :horizon-steps)
                            (conj (or ds [])
                                  {:source (.getName ^java.io.File f)
                                   :horizon-steps (:horizon-steps d)})
                            ds)))
                (assoc-in [:observations t] observations))))
                  {} files)]
          ;; Lift the source-declared common horizon into the merged map the
          ;; tick reads (war_machine.clj:6821): the MAXIMUM of the declared
          ;; values -- the horizon must cover the longest declared episode,
          ;; and with terminal scoring shorter episodes simply reach their
          ;; end state earlier. No refusal on disagreement (it is not a
          ;; conflict); sources declaring nothing contribute nothing, and
          ;; with none declared the key stays absent so the runner's T=2
          ;; default path is untouched. :horizon-steps stays a plain integer
          ;; (the tick reads it as the value); :horizon-steps-declarations
          ;; carries the per-file records so the judgement's :authority can
          ;; name the declaring sources.
          (cond-> merged
            (seq (:horizon-steps-declarations merged))
            (assoc :horizon-steps
                   (apply max (map :horizon-steps
                                   (:horizon-steps-declarations merged))))))))))

(defn acceptance-of
  "PROOF-wm-works ⟨1⟩6 part 1: a target's OWN acceptance declaration from its
   cascade source — the declared want token with its locator, carried WITH
   provenance (which source file and target declared it). A source declaring
   no want, or a want with no locator, declares no acceptance: nil, never
   invented. Sources are read from DIR (default the canonical dir); callers
   that already hold the sources may pass them via :sources."
  ([target]
   (acceptance-of target nil))
  ([target {:keys [dir sources]}]
   (let [srcs (or sources (load-declared (or dir default-dir)))
         want (first (get-in srcs [:wants target]))
         locator (get-in srcs [:locators target want])
         file (some (fn [f] (when (= target (:target f)) f)) (:files srcs))]
     (when (and (some? want) (map? locator))
       {:token want
        :locator locator
        :provenance {:source-file (:path file)
                     :source-sha256 (:sha256 file)
                     :target target
                     :declaration :cascade-source-want-locator}}))))

(defn with-context-fn
  "Add the :context-of function cascade-problems needs (it cannot live in data)."
  [sources]
  (when sources
    (assoc sources :context-of (fn [t] (get-in sources [:context-by-target t])))))
