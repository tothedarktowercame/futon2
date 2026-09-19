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
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.observation-checks :as oc]))

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
            file (if (.isAbsolute file) file (io/file oc/repo-root path))
            hash (try (file-sha file)
                      (catch java.io.IOException e
                        (refuse! :interpretation-source-unreadable
                                 {:path path :exception (.getName (class e))}))
                      (catch SecurityException e
                        (refuse! :interpretation-source-unreadable
                                 {:path path :exception (.getName (class e))})))]
        (when (and (contains? source :sha256) (not= (:sha256 source) hash))
          (refuse! :interpretation-source-hash-mismatch
                   {:path path :declared (:sha256 source) :observed hash}))
        (assoc receipt :source (assoc source :sha256 hash))))))

(defn- check-file! [path d]
  (when-not (= :wm/cascade-source-v1 (:schema d))
    (refuse! :schema {:path path :schema (:schema d)}))
  (doseq [k [:target :context :beta :facts :want :locators :patterns :interpretation-receipts :candidates]]
    (when-not (contains? d k) (refuse! :missing-key {:path path :key k})))
  (when-not (and (number? (get-in d [:beta :value])) (pos? (get-in d [:beta :value]))
                 (#{:declared :learned} (get-in d [:beta :status])))
    (refuse! :beta {:path path :beta (:beta d)}))
  d)

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
       (oc/with-registry-runs*
        (fn []
       (reduce
        (fn [acc f]
          (let [path (.getPath f)
                snapshot (java.nio.file.Files/readAllBytes (.toPath f))
                hash (evidence/sha256 snapshot)
                d (check-file! path (edn/read-string (String. snapshot java.nio.charset.StandardCharsets/UTF_8)))
                receipts (into {} (map (fn [[id receipt]] [id (read-receipt-source receipt)]))
                               (:interpretation-receipts d))
                t (:target d)
                {:keys [universe observations]} (observe-facts (:facts d) (:locators d))
                occurrence {:path path :sha256 hash :target t :observations observations}
                _ (when *read-occurrences* (swap! *read-occurrences* (fnil conj []) occurrence))]
            (-> acc
                (assoc-in [:universes t] universe)
                (assoc-in [:wants t] (vec (:want d)))
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
                (assoc-in [:observations t] observations))))
        {}
        files)))))))

(defn with-context-fn
  "Add the :context-of function cascade-problems needs (it cannot live in data)."
  [sources]
  (when sources
    (assoc sources :context-of (fn [t] (get-in sources [:context-by-target t])))))
