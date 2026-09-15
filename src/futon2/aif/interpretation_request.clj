(ns futon2.aif.interpretation-request
  "Prepare an intermediate unjudged request. No construction or runner caller yet.
  Snapshots are companions for the later interpretation receipt, not a new ledger."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.mission-registry :as registry])
  (:import [java.nio.file Files StandardOpenOption]
           [java.time Instant]
           [java.util.concurrent TimeUnit]))

(def retrievers
  [{:kind :embedding :implementation "/home/joe/code/futon3a/holes/labs/M-memes-arrows/cascade_construct.py"
    :index "/home/joe/code/futon3a/resources/notions/minilm_pattern_embeddings.json" :k 40}
   {:kind :tier0 :implementation "/home/joe/code/futon6/scripts/cas_select.py"
    :index "/home/joe/code/futon3/resources/sigils/patterns-index.tsv" :k 8}])
(def library-root "/home/joe/code/futon3/library")
(def timeout-ms 30000)
(defn- refuse! [kind data]
  (throw (ex-info "Interpretation request refused" (assoc data :interpretation/refusal kind))))
(defn- need! [condition kind data] (when-not condition (refuse! kind data)))

(defn resolve-target [action]
  (if (= :advance-ticket (:type action))
    (registry/ticket-entry (:target action))
    (some #(when (= (:target action) (:id %)) %) (:missions (registry/load-missions-cached)))))

(defn tension-selection
  "Ruled lifecycle sections, otherwise mission body; tickets always use body.
  Citations are contiguous retained lines, never reconstructed or paraphrased."
  [kind source text]
  (let [lines (vec (str/split-lines text))
        headings (keep-indexed (fn [i line]
                                (when-let [[_ hashes title] (re-matches #"^(#{1,6})\s+(.+)$" line)]
                                  {:i i :level (count hashes) :title title})) lines)
        title (or (:i (first (filter #(= 1 (:level %)) headings))) -1)
        lifecycle (when (= kind :mission)
                    (filter #(re-find #"(?i)\b(IDENTIFY|MAP|DERIVE)\b" (:title %)) headings))
        rule (cond (seq lifecycle) :lifecycle-sections (= kind :ticket) :ticket-body :else :mission-body)
        eligible (if (seq lifecycle)
                   (set (mapcat (fn [{:keys [i level]}]
                                  (range (inc i) (or (:i (first (filter #(and (> (:i %) i) (<= (:level %) level)) headings)))
                                                    (count lines)))) lifecycle))
                   (set (range (inc title) (count lines))))
        metadata? #(re-find #"(?i)^\s*(?:[-*]\s+)?(?:\*\*)?(?:parent|date|owner|driver|home|lifecycle|reviewer|opened|closed|cross-references|priority|severity|created|updated|status(?:\s*\([^)]*\))?)\s*(?:\*\*)?:|(?i)^\s*(?:\*\*)?dispatched by\b" %)
        front-end (or (:i (first (filter #(> (:i %) title) headings))) (count lines))
        metadata-lines (:excluded
                        (reduce (fn [{:keys [active excluded]} [i line]]
                                  (let [marker (boolean (metadata? line))
                                        continuation (and active (< i front-end) (not (str/blank? line)))
                                        omit (or marker continuation)]
                                    {:active (and (< i front-end) omit)
                                     :excluded (cond-> excluded omit (conj i))}))
                                {:active false :excluded #{}} (map-indexed vector lines)))
        kept (filter #(and (not= % title) (not (contains? metadata-lines %))) (sort eligible))
        groups (reduce (fn [groups i]
                         (if (= i (some-> groups peek peek inc))
                           (conj (pop groups) (conj (peek groups) i))
                           (conj groups [i]))) [] kept)
        citations (keep (fn [indices]
                          (let [a (first indices) b (inc (last indices)) body (subvec lines a b)]
                            (when (some #(and (not (str/blank? %)) (not (re-matches #"#+.*|---+" %))) body)
                              {:source source :lines [(inc a) b] :quote (str/join "\n" body)}))) groups)]
    {:tension-rule rule :citations (vec citations)}))

(defn tension-citations [kind source text]
  (:citations (tension-selection kind source text)))

(defn- read-bytes [path]
  (try (Files/readAllBytes (.toPath (io/file path)))
       (catch Exception e
         (refuse! :interpretation/source-unavailable {:path (str path) :reason (.getMessage e)}))))

(defn- revision [path]
  (let [dir (str (.getParentFile (io/file path)))
        inside (shell/sh "git" "-C" dir "rev-parse" "--is-inside-work-tree")]
    (if (and (not (zero? (:exit inside)))
             (str/includes? (:err inside) "not a git repository"))
      "untracked:not-in-git-work-tree"
      (let [r (shell/sh "git" "-C" dir "rev-parse" "HEAD")]
        (need! (zero? (:exit r)) :interpretation/source-revision-unavailable {:path path :stderr (:err r)})
        (str/trim (:out r))))))
(defn- pin! [dir path revision-fn]
  (let [file (io/file path)
        path (.getCanonicalPath file)
        bs (read-bytes file)
        digest (evidence/sha256 bs)
        version (revision-fn path)
        name (str (evidence/value-digest path) "-" digest ".source")
        dest (io/file dir name)]
    (if (.exists dest)
      (need! (= digest (evidence/sha256 (Files/readAllBytes (.toPath dest))))
             :interpretation/source-changed {:path path})
      (Files/write (.toPath dest) bs (into-array StandardOpenOption [StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE])))
    {:requested-path (.getAbsolutePath file) :canonical-path path :byte-count (alength bs)
     :source {:id (str path "#" digest) :path path :file name :sha256 digest :revision version}
     :bytes bs :snapshot (.getAbsolutePath dest)}))

(defn python-retrieve!
  "Bounded subprocess; executable code and indices are checked by prepare! around this call."
  [request]
  (let [proc (.start (ProcessBuilder.
                     ^java.util.List ["/home/joe/code/futon3a/.venv/bin/python3" "-B"
                                      "/home/joe/code/futon2/scripts/interpretation_retrieve.py"]))
        out (future (slurp (.getInputStream proc))) err (future (slurp (.getErrorStream proc)))]
    (with-open [w (io/writer (.getOutputStream proc))] (.write w (json/generate-string request)))
    (if (.waitFor proc timeout-ms TimeUnit/MILLISECONDS)
      (do (need! (zero? (.exitValue proc)) :interpretation/retriever-failed
                 {:exit (.exitValue proc) :stderr @err})
          (let [rows (json/parse-string @out true)]
            (need! (sequential? rows) :interpretation/retriever-response-invalid {:stdout @out})
            (vec rows)))
      (do (.destroyForcibly proc)
          (refuse! :interpretation/retriever-timeout {:timeout-ms timeout-ms})))))

(defn- library-paths []
  (->> (.listFiles (io/file library-root)) (filter #(.isDirectory %))
       (mapcat #(or (.listFiles %) []))
       (filter #(and (.isFile %) (str/ends-with? (.getName %) ".flexiarg")))
       (map #(.getCanonicalPath %)) sort vec))

(defn normalize-rows
  "Preserve qualified IDs; resolve bare IDs only against the captured library.
  Ambiguous/unresolved rows retain the raw response and do not become candidates."
  [rows library]
  (let [qualified (map #(str/replace (:relative %) #"\.flexiarg$" "") library)
        by-stem (group-by #(last (str/split % #"/")) qualified)]
    (reduce (fn [result [i row]]
              (let [id (or (:pattern_id row) (:pattern row))
                    matches (if (and (string? id) (str/includes? id "/")) [id] (distinct (get by-stem id)))
                    failure (cond (not (and (string? id) (not (str/blank? id)))) :interpretation/retriever-response-invalid
                                  (> (count matches) 1) :interpretation/ambiguous-pattern-id
                                  (empty? matches) :interpretation/unresolved-pattern-id)]
                (if failure
                  (update result :failures conj {:kind failure :rank (inc i) :raw row :matches (vec matches)})
                  (update result :candidates conj {:pattern (first matches) :rank (inc (count (:candidates result)))
                                                   :retriever-rank (inc i) :raw row :judgment :unjudged}))))
            {:candidates [] :failures []} (map-indexed vector rows))))

(defn prepare!
  "ACTION is the authorized input, not a selection proposal. Ports allow hermetic tests.
  On double retrieval failure, ex-data retains the full partial request and source list."
  ([action identity] (prepare! action identity {}))
  ([action identity {:keys [resolve-fn revision-fn retrieve-fn retriever-specs library-fn now on-capture]
                     :or {resolve-fn resolve-target revision-fn revision retrieve-fn python-retrieve!
                          retriever-specs retrievers library-fn library-paths on-capture (fn [_]) now #(str (Instant/now))}}]
   (need! (and (= 2 (count retriever-specs))
               (= #{:embedding :tier0} (set (map :kind retriever-specs))))
          :interpretation/retriever-set-invalid {})
   (evidence/validate-identity identity)
   (need! (= action (get-in identity [:occurrence :action/value]))
          :interpretation/action-mismatch {:identity identity})
   (need! (#{:advance-mission :open-mission :advance-ticket} (:type action))
          :interpretation/action-type-unsupported {:action action})
   (need! (= (:data-root identity) (.getCanonicalPath (io/file (:data-root identity))))
          :interpretation/attempt-path-invalid {:identity identity})
   (let [cohort (str/replace-first (get-in identity [:occurrence :cohort/id]) #"^:" "")
         attempt (get-in identity [:occurrence :attempt/id])
         _ (doseq [s [cohort attempt]]
             (need! (and (string? s) (re-matches #"[A-Za-z0-9_-]+" s))
                    :interpretation/attempt-path-invalid {:identity identity}))
         root (io/file (:data-root identity) cohort attempt)
         start (read-bytes (io/file root "001-time-step.edn"))
         _ (need! (= (:start-event-sha256 identity) (evidence/sha256 start))
                  :interpretation/attempt-identity-mismatch {:identity identity})
         event (edn/read-string (String. start "UTF-8"))
         _ (need! (and (= attempt (:attempt/id event))
                        (= cohort (str/replace-first (str (:cohort/id event)) #"^:" "")))
                  :interpretation/attempt-identity-mismatch {:identity identity})
         _ (need! (= (:semantic-epoch identity) (get-in event [:payload :judgment :semantic-epoch]))
                  :interpretation/attempt-identity-mismatch {:identity identity})
         entry (resolve-fn action)
         _ (need! (and (= (:target action) (:id entry)) (:path entry))
                  :interpretation/target-unresolved {:action action})
         dir (io/file root "evidence")
         _ (.mkdirs dir)
         target-pin (pin! dir (:path entry) revision-fn)
         _ (on-capture (:source target-pin))
         pinned-at (now)
         kind (if (= :advance-ticket (:type action)) :ticket :mission)
         tension (tension-selection kind (get-in target-pin [:source :id]) (String. ^bytes (:bytes target-pin) "UTF-8"))
         citations (:citations tension)
         _ (need! (seq citations) :interpretation/no-citable-tension
                  {:identity identity :action action :sources [(:source target-pin)]})
         query (str/join "\n\n" (map :quote citations))
         sources (atom [(:source target-pin)])
         pins (atom [target-pin])
         capture! (fn [path] (let [p (pin! dir path revision-fn)] (on-capture (:source p)) (swap! sources conj (:source p)) (swap! pins conj p) p))
         library-pins (delay
                        (mapv (fn [path]
                                (let [p (capture! path) f (io/file path)]
                                  {:snapshot (:snapshot p) :source-id (get-in p [:source :id])
                                   :relative (str (.getName (.getParentFile f)) "/" (.getName f))}))
                              (library-fn)))
         runs (mapv
               (fn [{:keys [kind implementation index k]}]
                 (let [partial (atom {:retriever (name kind) :version "unavailable"
                                      :index-source {:status :none :reason :capture-failed}
                                      :parameters {:k k :scope (if (= kind :tier0) :whole-index :embedding-index)}
                                      :candidates [] :failures []})]
                   (try
                     (let [code (capture! implementation) idx (capture! index)
                           _ (swap! partial assoc :version (get-in code [:source :sha256])
                                    :index-source (get-in idx [:source :id])
                                    :parameters (assoc (:parameters @partial)
                                                       :implementation-source (get-in code [:source :id])))
                           library @library-pins
                           _ (when (= kind :tier0)
                               (swap! partial update :parameters assoc
                                      :library-sources (mapv :source-id library) :extra-library-dirs []))
                           rows (retrieve-fn {:kind (name kind) :implementation implementation
                                              :implementation_snapshot (:snapshot code)
                                              :index (:snapshot idx) :query query :k k
                                              :library library :attempt_dir (.getAbsolutePath root)})]
                       (need! (= (get-in code [:source :sha256])
                                 (evidence/sha256 (Files/readAllBytes (.toPath (io/file implementation)))))
                              :interpretation/source-changed {:path implementation})
                       (need! (sequential? rows) :interpretation/retriever-response-invalid {:retriever kind})
                       (let [normalized (normalize-rows (vec rows) library)]
                         (assoc @partial :candidates (:candidates normalized)
                                :row-failures (:failures normalized))))
                     (catch Exception e
                       (assoc @partial :failures [{:kind (or (:interpretation/refusal (ex-data e))
                                                                           :interpretation/retriever-failed)
                                                    :reason (.getMessage e) :details (ex-data e)}])))))
               retriever-specs)
         request {:schema :wm/interpretation-request-v1 :identity identity
                  :target {:id (:target action) :kind kind :action action
                           :source (get-in target-pin [:source :id]) :citations citations :pinned-at pinned-at :tension-rule (:tension-rule tension)}
                  :sources (vec (vals (into (sorted-map) (map (juxt :id clojure.core/identity)) @sources)))
                  :captured-bytes (reduce + (map :byte-count (vals (into {} (map (juxt :snapshot clojure.core/identity)) @pins))))
                  :source-paths (mapv #(assoc (select-keys % [:requested-path :canonical-path])
                                             :source-id (get-in % [:source :id])) @pins)
                  :retrieval {:query query :citations citations :runs runs}}]
     (need! (= #{:embedding :tier0} (set (map :kind retriever-specs)))
            :interpretation/retriever-set-invalid {:request request})
     (need! (some #(empty? (:failures %)) runs) :interpretation/retrieval-unavailable {:request request})
     request)))
