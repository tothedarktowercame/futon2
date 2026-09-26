(ns futon2.aif.observation-labels
  "A-S Revision 3: C4 labels from blinded, independently recomputed evidence.
   No tick wiring or implicit storage path. Code identities are caller-supplied."
  (:refer-clojure :exclude [load])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [futon2.aif.observation-admission :as admission]
            [futon2.aif.observation-checks :as checks]))

(def ^:dynamic *code-sha*
  "The commit identifying this namespace, supplied by the caller. write-labels
   binds it from opts :code-sha; direct c4-recompute callers must bind it too.
   No HEAD lookup: a later unrelated commit must not rename the mechanism."
  nil)

(defn- required-identity [value field]
  (when-not (and (string? value) (not (str/blank? value)))
    (throw (ex-info "Code identity must be supplied"
                    {:status :missing :kind :code-identity-missing :field field})))
  value)

(defn c4-subject
  "Adapt a tick-time check-decl-in-file result. :check carries the class id;
   the first evidence pointer remains the check's verbatim locator. The cutoff
   uses admission's {:repo repo :sha resolved-sha} shape. This constructor
   explicitly declares the tick-time author and enactor absent."
  [check-result mechanism-sha]
  (required-identity mechanism-sha :mechanism-sha)
  (let [{:keys [repo resolved-sha] :as evidence} (:evidence check-result)]
    {:token (:token check-result)
     :token-class (:check check-result)
     :evidence-pointers [evidence]
     :check-mechanism (str "C4/decl-present?@" mechanism-sha)
     :check-cutoff {:repo repo :sha resolved-sha}
     :recorded-verdict (:observed check-result)
     :author :none :enactor :none}))

(defn- tokens [line]
  (vec (remove str/blank? (str/split (str/trim line) #"[\s:({\[]+"))))

(defn c4-recompute
  "Read only observer-view, then independently git-show its resolved file.
   Presence means a line's leading tokens equal all of decl's tokens. Tokens
   split on whitespace, colon and opening (, {, [. This is not the check's
   literal anchored regex. An unreadable file yields :insufficient, not absent.
   Bind *code-sha* to the commit identifying this namespace."
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

(defn write-labels
  "CHECK-RESULTS is a sequence of C4 results (optionally carrying :token).
   STORE maps admission/label-key to admission/label-record. OPTS requires
   :mechanism-sha (the check's code commit) and :code-sha (this namespace's
   code commit). Repeated keys skip recomputation and add no label. Admission
   refusals are returned verbatim with :key; upstream check refusals pass
   through without being reinterpreted as observations. No store IO."
  [check-results store {:keys [mechanism-sha code-sha]}]
  (required-identity mechanism-sha :mechanism-sha)
  (required-identity code-sha :code-sha)
  (binding [*code-sha* code-sha]
    (reduce
     (fn [acc check-result]
       (cond
         (:status check-result) (update acc :refused conj check-result)
         (not= :C4 (:check check-result))
         (update acc :refused conj {:status :missing :kind :unsupported-check
                                   :data {:check (:check check-result)}})
         :else
         (let [subject (c4-subject check-result mechanism-sha)
               key (admission/label-key subject)]
           (if (contains? (:store acc) key)
             (update acc :skipped conj {:key key :already-labelled true})
             (let [adjudication (c4-recompute subject)
                   review (admission/mechanical-review
                           (str "mechanical-review@" code-sha) subject adjudication)
                   admitted (admission/admit subject adjudication review)]
               (if-let [record (admission/label-record subject admitted)]
                 (-> acc (assoc-in [:store key] record) (update :written conj record))
                 (update acc :refused conj (assoc admitted :key key))))))))
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
