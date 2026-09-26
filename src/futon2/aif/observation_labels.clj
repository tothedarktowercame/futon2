(ns futon2.aif.observation-labels
  "A-S Revision 3: C3/C4 labels from blinded, independently recomputed evidence.
   No tick wiring or implicit storage path. Code identities are caller-supplied."
  (:refer-clojure :exclude [load])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.observation-admission :as admission]
            [futon2.aif.observation-checks :as checks]))

(load-identity/register! *ns* *file*)

(defn loaded-identities
  "Read both load registrations once. No disk or HEAD fallback."
  []
  (let [entries @load-identity/registry
        sources [[:mechanism-sha 'futon2.aif.observation-checks]
                 [:code-sha 'futon2.aif.observation-labels]]]
    (or (some (fn [[_ n]]
                (let [entry (get entries n)]
                  (when-not (and (= :captured (:status entry))
                                 (string? (:sha256 entry))
                                 (re-matches #"[0-9a-f]{64}" (:sha256 entry)))
                    {:status :missing :kind :code-identity-unregistered :namespace n})))
              sources)
        (into {} (map (fn [[k n]] [k (str "sha256:" (:sha256 (get entries n)))]) sources)))))

(def ^:dynamic *code-sha*
  "Code identity: a load-identity source digest sha256:<hex>, or a commit
   when the caller holds a verified one. write-labels
   binds it from opts :code-sha; direct recomputation callers must bind it too.
   No HEAD lookup: a later unrelated commit must not rename the mechanism."
  nil)

(defn- required-identity [value field]
  (when-not (and (string? value) (not (str/blank? value)))
    (throw (ex-info "Code identity must be supplied"
                    {:status :missing :kind :code-identity-missing :field field})))
  value)

(def unwitnessed-mechanism {:absent :check-mechanism-unwitnessed})

(defn- check-subject [check-result]
  (let [{:keys [repo resolved-sha] :as evidence} (:evidence check-result)
        c (:check check-result)
        mechanism (:check-mechanism check-result)
        descriptor (checks/loaded-check c)
        subject {:token (:token check-result) :token-class c
                 :evidence-pointers [evidence]
                 :check-mechanism (or mechanism unwitnessed-mechanism)
                 :check-cutoff {:repo repo :sha resolved-sha}
                 :recorded-verdict (:observed check-result)
                 :author :none :enactor :none}
        key (admission/label-key subject)
        name-part (when (string? mechanism) (first (str/split mechanism #"@" 2)))
        kind (cond
               (nil? mechanism) :check-mechanism-unwitnessed
               (:status descriptor) (:kind descriptor)
               (or (not= name-part (:mechanism-name descriptor))
                   (not (and (string? mechanism)
                             (re-matches #"[^@]+@[^@]+" mechanism)))
                   (and (:check-mechanism-name check-result)
                        (not= name-part (:check-mechanism-name check-result))))
               :class-mechanism-mismatch)]
    (if kind
      (throw (ex-info (name kind) {:status :missing :kind kind :reason (name kind) :key key :check c}))
      subject)))

(defn c4-subject
  "Copy the dispatch stamp and evidence; never rename a historical check."
  [check-result]
  (check-subject check-result))

(defn- tokens [line]
  (vec (remove str/blank? (str/split (str/trim line) #"[\s:({\[]+"))))

(defn c4-recompute
  "Read only observer-view, then independently git-show its resolved file.
   Presence means a line's leading tokens equal all of decl's tokens. Tokens
   split on whitespace, colon and opening (, {, [. This is not the check's
   literal anchored regex. An unreadable file yields :insufficient, not absent.
   Bind *code-sha* to this namespace's source digest or a verified commit."
  [subject]
  (let [view (admission/observer-view subject)
        {:keys [repo resolved-sha path decl]} (first (:evidence-pointers view))
        observer (str "C4/reader-form@" (required-identity *code-sha* :code-sha))
        readable-locator? (every? #(and (string? %) (not (str/blank? %)))
                                 [repo resolved-sha path decl])
        result (when readable-locator?
                 (try
                   (sh/sh "git" "-C" (str (io/file checks/repo-root repo))
                          "show" (str resolved-sha ":" path))
                   (catch java.io.IOException _ nil)))
        wanted (when readable-locator? (tokens decl))
        finding (cond
                  (or (not= 0 (:exit result)) (empty? wanted)) :insufficient
                  (some #(= wanted (vec (take (count wanted) (tokens %))))
                        (str/split-lines (:out result))) :present
                  :else :absent)]
    (admission/adjudication observer view finding {:repo repo :sha resolved-sha})))

(defn c3-subject
  "Copy the dispatch stamp; C3 evidence has no declaration slot."
  [check-result]
  (check-subject check-result))

(defn c3-recompute
  "Read only observer-view and independently list the resolved tree. Presence
   requires a listed path field EXACTLY equal to the requested path, never a
   prefix. -z preserves literal path bytes without git's display quoting.
   An empty or nonmatching listing is :absent; git failure is :insufficient.
   Bind *code-sha* to this namespace's source digest or a verified commit."
  [subject]
  (let [view (admission/observer-view subject)
        {:keys [repo resolved-sha path]} (first (:evidence-pointers view))
        observer (str "C3/ls-tree@" (required-identity *code-sha* :code-sha))
        readable-locator? (every? #(and (string? %) (not (str/blank? %)))
                                 [repo resolved-sha path])
        result (when readable-locator?
                 (try
                   (sh/sh "git" "-C" (str (io/file checks/repo-root repo))
                          "ls-tree" "-z" resolved-sha "--" path)
                   (catch java.io.IOException _ nil)))
        finding (cond
                  (not= 0 (:exit result)) :insufficient
                  (some #(= path (second (str/split % #"\t" 2)))
                        (str/split (:out result) #"\u0000")) :present
                  :else :absent)]
    (admission/adjudication observer view finding {:repo repo :sha resolved-sha})))

(defn write-labels
  "CHECK-RESULTS is a sequence of C3/C4 results (optionally carrying :token).
   STORE maps admission/label-key to admission/label-record. OPTS requires
   :code-sha (this namespace's code identity). Check identity is copied from
   each result's dispatch stamp, never from opts. Code identities are
   load-identity source digests sha256:<hex>, or commits when
   the caller holds verified ones. Repeated keys skip recomputation and add no label. Admission
   refusals are returned verbatim with :key; upstream check refusals pass
   through without being reinterpreted as observations. No store IO."
  [check-results store {:keys [code-sha]}]
  (required-identity code-sha :code-sha)
  (binding [*code-sha* code-sha]
    (reduce
     (fn [acc check-result]
       (cond
         (:status check-result) (update acc :refused conj check-result)
         (not (contains? #{:C3 :C4} (:check check-result)))
         (update acc :refused conj {:status :missing :kind :unsupported-check
                                   :data {:check (:check check-result)}})
         :else
         (try
           (let [[make-subject recompute] (case (:check check-result)
                                            :C3 [c3-subject c3-recompute]
                                            :C4 [c4-subject c4-recompute])
                 subject (make-subject check-result)
                 key (admission/label-key subject)]
             (if (contains? (:store acc) key)
               (update acc :skipped conj {:key key :already-labelled true})
               (let [adjudication (recompute subject)
                     review (admission/mechanical-review
                             (str "mechanical-review@" code-sha) subject adjudication)
                     admitted (admission/admit subject adjudication review)]
                 (if-let [record (admission/label-record subject admitted)]
                   (-> acc (assoc-in [:store key] record) (update :written conj record))
                   (update acc :refused conj (assoc admitted :key key))))))
           (catch clojure.lang.ExceptionInfo e
             (update acc :refused conj (ex-data e))))))
     {:store store :written [] :skipped [] :refused []}
     check-results)))

(defn- explicit-path [path]
  (when (or (nil? path) (and (string? path) (str/blank? path)))
    (throw (ex-info "An explicit label-store path is required"
                    {:status :missing :kind :path-required})))
  path)

(defn persist!
  "Write STORE as one EDN value at an explicit PATH, in deterministic UTF-8.
   Re-persisting a loaded store produces identical bytes. Returns PATH."
  [store path]
  (explicit-path path)
  (let [ordered (walk/postwalk
                 #(if (map? %)
                    (into (sorted-map-by (fn [a b] (compare (pr-str a) (pr-str b)))) %)
                    %) store)]
    (spit path (str (pr-str ordered) "\n") :encoding "UTF-8"))
  path)

(defn load
  "Read exactly one EDN store map from an explicit PATH. Missing files remain
   IO errors; neither missing paths nor files become an empty population."
  [path]
  (with-open [reader (java.io.PushbackReader. (io/reader (explicit-path path) :encoding "UTF-8"))]
    (let [eof (Object.)
          store (edn/read {:eof eof} reader)
          trailing (edn/read {:eof eof} reader)]
      (when-not (and (map? store) (identical? eof trailing))
        (throw (ex-info "Expected one EDN label-store map"
                        {:status :missing :kind :invalid-label-store})))
      store)))
