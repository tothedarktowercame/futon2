(ns v7-r15-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(def lab (io/file "holes/labs/wm-contract"))
(def equation-file (io/file lab "aif-equations.edn"))
(def temporal-file (io/file "src/futon2/aif/temporal_hierarchy.clj"))
(def rollout-file (io/file "src/futon2/aif/rollout.clj"))
(def budget-file (io/file "src/futon2/aif/hierarchical_budget.clj"))

(defn numbered-lines [f]
  (map-indexed (fn [i text] {:line (inc i) :text text})
               (str/split-lines (slurp f))))

(defn matching-lines [f re]
  (->> (numbered-lines f)
       (filter #(re-find re (:text %)))
       (mapv #(update % :text str/trim))))

(defn lines-in [f lo hi]
  (->> (numbered-lines f)
       (filter #(<= lo (:line %) hi))
       (mapv #(update % :text str/trim))))

(defn source-files [roots]
  (->> roots
       (mapcat #(file-seq (io/file %)))
       (filter #(.isFile %))
       (filter #(re-find #"\.(clj|cljc|bb)$" (.getName %)))
       (sort-by #(.getCanonicalPath %))))

(defn relative-path
  "Repo-relative for files under this repo, absolute for files outside it --
   REVIEW FIX (slice 13): the widened artefact census reaches p4ng, and a
   \"../p4ng/...\" pointer resolves from nowhere in particular."
  [f]
  (let [root (.toPath (.getCanonicalFile (io/file ".")))
        path (.toPath (.getCanonicalFile f))]
    (if (.startsWith path root)
      (str (.relativize root path))
      (str path))))

(defn source-hits [files re]
  (vec
   (mapcat (fn [f]
             (map #(assoc % :file (relative-path f)) (matching-lines f re)))
           files)))

(defn trace-files []
  (->> (.listFiles (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))))

(defn read-records
  "Records of one trace file, plus whether an unreadable form stopped the read.

   REVIEW FIX (slice 13): the delivered form recurred on ::bad WITHOUT consuming
   the offending form -- the same defect the slice 12 review found and fixed in
   the R14 harness, reintroduced here. It spins if an unreadable form ever
   appears, and silently drops a file's tail if it does not. Stopping is now
   reported, so a truncated read is visible in the receipt instead of being
   absorbed into a record count."
  [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [out []]
      (let [v (try (edn/read {:eof ::eof :default (fn [_ value] value)} r)
                   (catch Exception _ ::bad))]
        (cond (= v ::eof) {:records out :truncated? false}
              (= v ::bad) {:records out :truncated? true}
              :else (recur (conj out v)))))))

(def trace-per-file
  (mapv (fn [f] (assoc (read-records f) :file (.getName f))) (trace-files)))

(def truncated-trace-files (mapv :file (filter :truncated? trace-per-file)))

(def trace-records
  (vec (mapcat (fn [{:keys [file records]}]
                 (map-indexed (fn [index record]
                                {:file file :index index :record record})
                              records))
               trace-per-file)))

(defn tree-values [x k]
  (cond (map? x) (concat (when (contains? x k) [(get x k)])
                         (mapcat #(tree-values % k) (vals x)))
        (sequential? x) (mapcat #(tree-values % k) x)
        :else []))

(def slow-keys [:slow/mode :slow/intrinsics :slow/previous-mode])
(defn slow-record? [record]
  (boolean (some #(seq (tree-values record %)) slow-keys)))
(defn r15-route? [record]
  (boolean
   (some #(and (map? %) (= :R15 (:node %)))
         (mapcat #(let [v %] (if (sequential? v) v [v]))
                 (concat (tree-values record :wm/route)
                         (tree-values record :route))))))

(def equation-r15-lines (matching-lines equation-file #":node\s+:R15(?:\s|[,}])"))
(def plumbing-lines (matching-lines equation-file #":plumbing\s+\[.*:R15"))
(def hierarchy-lines (matching-lines equation-file #":hierarchy\s+\{:observed"))

;; REVIEW FIX (slice 13): the delivered harness pinned three windows by line
;; number -- the registry row at 373-376, the registry's own citation target at
;; 166-176, and the budget namespace at 1-3 -- and pinned the quoted phrase as a
;; literal. Slice 9 had already set this sweep's rule ("the evidence window is
;; derived from whatever line the search returns", v7_r20_node_sim.clj:112-115),
;; and slice 10's review found the same defect a second time. All four are now
;; derived: the registry row runs from its :hierarchy line to its :statement
;; line, the cited RANGE and the quoted PHRASE are parsed out of the registry's
;; own :evidence text, and the phrase is located by searching for it. A registry
;; edit that moves the row now moves the measurement with it, instead of turning
;; this check into a false :fail.
(defn row-lines
  "The lines of the registry row starting at start-line, through its :statement."
  [f start-line]
  (let [tail (drop-while #(< (:line %) start-line) (numbered-lines f))
        [before at] (split-with #(not (re-find #"^\s*:statement" (:text %))) tail)]
    (mapv #(update % :text str/trim) (concat before (take 1 at)))))

(def hierarchy-evidence-lines
  (vec (when-let [start (:line (first hierarchy-lines))]
         (row-lines equation-file start))))

(defn parse-citation
  "The file:lo-hi range and the phrase a registry :evidence line quotes for it."
  [lines file-re]
  (first (keep (fn [{:keys [line text]}]
                 (when-let [m (re-find file-re text)]
                   {:registry-line line
                    :lo (Long/parseLong (nth m 1))
                    :hi (Long/parseLong (nth m 2))
                    :quoted-phrase (nth m 3)}))
               lines)))

(def cited-rollout-citation
  (parse-citation hierarchy-evidence-lines #"rollout\.clj:(\d+)-(\d+)\s*\('([^']*)'\)"))

(defn locate-phrase
  "Where a phrase occurs in f, ignoring how it is broken across lines. Returns
   the line the match starts on, or nil. Whitespace-insensitive because the
   registry quotes a docstring sentence that the source wraps."
  [f phrase]
  (let [content (slurp f)
        words (remove str/blank? (str/split (str/trim phrase) #"\s+"))
        pattern (re-pattern (str/join "\\s+" (map #(java.util.regex.Pattern/quote %) words)))
        matcher (re-matcher pattern content)]
    (when (.find matcher)
      {:line (inc (count (re-seq #"\n" (subs content 0 (.start matcher)))))
       :text (str/replace (.group matcher) #"\s+" " ")})))

(defn citation-status
  "Does the range a citation names still contain the phrase it quotes?"
  [citation location]
  (cond (nil? citation) :citation-not-parsed
        (nil? location) :phrase-not-found
        (<= (:lo citation) (:line location) (:hi citation)) :current
        :else :stale))

(def cited-rollout-lines
  (vec (when cited-rollout-citation
         (lines-in rollout-file (:lo cited-rollout-citation) (:hi cited-rollout-citation)))))

(def quoted-phrase-location
  (when cited-rollout-citation
    (locate-phrase rollout-file (:quoted-phrase cited-rollout-citation))))

(def cited-rollout-status (citation-status cited-rollout-citation quoted-phrase-location))

(def actual-flat-lines (vec (when quoted-phrase-location [quoted-phrase-location])))

(def budget-doc-lines
  (let [numbered (numbered-lines budget-file)
        ns-line (first (filter #(re-find #"^\(ns " (:text %)) numbered))
        first-doc-line (first (filter #(and (> (:line %) (:line ns-line))
                                            (not (str/blank? (str/trim (:text %)))))
                                      numbered))]
    (mapv #(update % :text str/trim) (remove nil? [ns-line first-doc-line]))))
(def temporal-registry-hits (matching-lines equation-file #"(?i)temporal[-_ ]hierarchy|temporal_hierarchy"))
(def temporal-doc-r15 (matching-lines temporal-file #"R15 b3"))

(def census-roots ["src" "scripts" "test" "checks" "holes"])
(def census-re
  #"futon2\.aif\.temporal-hierarchy|advance-slow-state|slow-context-from-intrinsics|:slow/mode|:slow/intrinsics|:slow/previous-mode")
(def census-hits (source-hits (source-files census-roots) census-re))
(defn tree-bucket [path]
  (keyword (first (str/split path #"/"))))
(def census-by-tree
  (into (sorted-map)
        (for [[tree hits] (group-by #(tree-bucket (:file %)) census-hits)]
          [tree {:line-count (count hits)
                 :files (vec (sort (distinct (map :file hits))))
                 :hits hits}])))
(def external-runtime-hits
  (vec (remove #(or (= "src/futon2/aif/temporal_hierarchy.clj" (:file %))
                    (= "test/futon2/aif/temporal_hierarchy_test.clj" (:file %))
                    (= "holes/labs/wm-contract/v7_r15_node_sim.clj" (:file %)))
               census-hits)))

(def corpus-slow (vec (filter #(slow-record? (:record %)) trace-records)))
(def corpus-r15 (vec (filter #(r15-route? (:record %)) trace-records)))
(defn file-spread [records]
  (vec (sort (distinct (map :file records)))))

(def apply-prior-sites (matching-lines temporal-file #"defn apply-slow-prior"))
(def hierarchical-sites (matching-lines temporal-file #"defn hierarchical-rollout"))
(def advance-sites (matching-lines temporal-file #"defn advance-slow-state"))
(def own-efe-denial (matching-lines temporal-file #"full nested generative model"))
(def beta-update-sites (matching-lines temporal-file #"next-update-record"))

(def campaign-search
  #"advanced the relevant slow posterior|parameterised Tick B|Beta\(2,1\)")

;; REVIEW FIX (slice 13). The delivered census searched ONE directory --
;; holes/labs/wm-contract/runs -- and reported :not-found. The catalogue's claim
;; is about a run of the machine, and a record of that run has no reason to be
;; under this lab's own run directory, so an absence established there did not
;; carry. Two changes: the roots are widened to the trees where futon2 and p4ng
;; keep records and documents, and a hit is classified BY PARSING rather than by
;; grepping. A file that CARRIES a slow-state key as data (an EDN artefact whose
;; parsed tree holds the keyword) is a machine record; a file that only quotes
;; the key inside a string is prose. That distinction is what keeps this slice's
;; OWN matrix row and ledger entry -- both of which quote the keys -- from
;; flipping the finding on a later run.
(def artefact-roots ["holes" "docs" "/home/joe/code/p4ng"])
(def artefact-extensions #"\.(edn|txt|md|tex|json)$")
(def receipt-file (io/file lab "runs/V7-R15-node-sim/00-r15.edn"))

(def artefact-files
  (->> artefact-roots
       (mapcat #(file-seq (io/file %)))
       (filter #(.isFile %))
       (filter #(re-find artefact-extensions (.getName %)))
       (remove #(str/includes? (.getCanonicalPath %) "/.git/"))
       (remove #(= (.getCanonicalPath receipt-file) (.getCanonicalPath %)))
       (sort-by #(.getCanonicalPath %))))

(defn scan-file
  "One pass over f, collecting the lines matching each named regex."
  [f named-res]
  (let [rel (relative-path f)]
    (with-open [r (io/reader f)]
      (reduce (fn [acc [i line]]
                (reduce (fn [a [k re]]
                          (if (re-find re line)
                            (update a k (fnil conj []) {:file rel :line (inc i) :text (str/trim line)})
                            a))
                        acc named-res))
              {}
              (map-indexed vector (line-seq r))))))

(def slow-key-mention-re #":slow/(?:mode|intrinsics|previous-mode)")

;; THE IDENTIFIERS THE NARRATIVE ITSELF SUPPLIES, added by the review (slice
;; 13). The catalogue's R15 evidence sentence has a longer twin in
;; p4ng/empirics.tex, and that one cites a campaign receipt id and Tick A's
;; attempt id. Two named identifiers are a far sharper search than a phrase: if
;; the campaign left a record anywhere in the trees this row cites, the record
;; carries its own id. Parsed out of the narrative rather than transcribed, so
;; an edit to the narrative moves the search with it.
(def empirics-file (io/file "/home/joe/code/p4ng/empirics.tex"))
(def campaign-narrative-lines (matching-lines empirics-file #"Campaign S supplied"))
(def cited-identifiers
  ;; The campaign receipt, Tick A's attempt, the inter-tick run and the corpus
  ;; hash it froze -- every identifier the narrative hands out. Widened during
  ;; the review after the first two came back absent: the question of whether
  ;; the absence is R15's or the whole campaign's is answerable only by asking
  ;; about the campaign's other artefacts too.
  (vec (distinct (mapcat (fn [{:keys [text]}]
                           (concat (map second (re-seq #"texttt\{((?:e|canary)-[0-9a-f][0-9a-f-]{12,})\}" text))
                                   (map second (re-seq #"texttt\{(wm-[A-Za-z0-9][A-Za-z0-9/_-]+)\}" text))
                                   (map second (re-seq #"texttt\{([0-9a-f]{64})\}" text))))
                         campaign-narrative-lines))))
(def identifier-re
  (when (seq cited-identifiers)
    (re-pattern (str/join "|" (map #(java.util.regex.Pattern/quote %) cited-identifiers)))))

(defn read-forms [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [out []]
      (let [v (try (edn/read {:eof ::eof :default (fn [_ value] value)} r)
                   (catch Exception _ ::bad))]
        (cond (= v ::eof) {:forms out :parsed? true}
              (= v ::bad) {:forms out :parsed? false}
              :else (recur (conj out v)))))))

(defn tree-has-keyword? [x kws]
  (boolean (cond (keyword? x) (contains? kws x)
                 (map? x) (or (some #(tree-has-keyword? % kws) (keys x))
                              (some #(tree-has-keyword? % kws) (vals x)))
                 (coll? x) (some #(tree-has-keyword? % kws) x)
                 :else false)))

(defn classify-mention
  "A file that mentions a slow-state key either CARRIES one as data or only
   TALKS about one. :unparsed is reported rather than absorbed, so a parse
   failure cannot pass for an absence."
  [{:keys [edn? parsed? forms]}]
  (cond (not edn?) :prose
        (not parsed?) :unparsed
        (tree-has-keyword? forms (set slow-keys)) :machine-record
        :else :prose))

(defn mention-entry [f]
  (let [edn? (str/ends-with? (.getName f) ".edn")
        {:keys [forms parsed?]} (if edn? (read-forms f) {:forms [] :parsed? true})
        entry {:file (relative-path f) :edn? edn? :parsed? parsed? :forms forms}]
    (-> entry (dissoc :forms) (assoc :classification (classify-mention entry)))))

(def artefact-scan
  (mapv (fn [f]
          (assoc (scan-file f (cond-> {:slow slow-key-mention-re :prose campaign-search}
                                identifier-re (assoc :identifier identifier-re)))
                 :file f))
        artefact-files))

(def slow-key-mentions
  (mapv #(mention-entry (:file %)) (filter :slow artefact-scan)))

(def machine-records
  (vec (filter #(= :machine-record (:classification %)) slow-key-mentions)))
(def unparsed-mentions
  (vec (filter #(= :unparsed (:classification %)) slow-key-mentions)))

(def campaign-artifact-hits (vec (mapcat :prose artefact-scan)))

;; This sweep's own bookkeeping quotes the identifiers in prose, so it is named
;; and set aside rather than silently filtered: a record of the campaign would
;; be a file the MACHINE wrote, not a row this sweep wrote about it.
(def sweep-bookkeeping
  #{"holes/labs/wm-contract/worklist.edn"
    "holes/labs/wm-contract/VERIFY-r-nodes.edn"
    "holes/labs/wm-contract/runs/V7-R15-node-sim/00-r15.edn"})

(defn narrative-file? [{:keys [file]}]
  (boolean (re-find #"\.(tex|md)$" file)))

(def identifier-hits (vec (mapcat :identifier artefact-scan)))
(def identifier-hits-in-narrative (vec (filter narrative-file? identifier-hits)))
(def identifier-hits-in-this-sweep
  (vec (filter #(contains? sweep-bookkeeping (:file %)) identifier-hits)))
(def identifier-hits-in-records
  (vec (remove #(or (narrative-file? %) (contains? sweep-bookkeeping (:file %)))
               identifier-hits)))

(def required-namespaces
  (vec (sort (distinct (map second (re-seq #"\[([a-z][a-z0-9.\-]*\.[a-z0-9.\-]+)\s+:as"
                                           (slurp temporal-file)))))))
(def inference-namespace-re #"efe|free-energy|free_energy|precision|belief|inference|policy")
(def inference-requires (vec (filter #(re-find inference-namespace-re %) required-namespaces)))

(def synthetic-record {:slow/mode :consolidation})
(def synthetic-source [{:file "synthetic.clj" :line 1
                        :text "[futon2.aif.temporal-hierarchy :as th]"}])
(def synthetic-registry [{:file "synthetic.edn" :line 1
                          :text "{:id :synthetic :node :R15}"}])
(defn plant [id expected got]
  {:id id :expected expected :got got :result (if (= expected got) :caught :escaped)})

;; PLANTS ADDED BY THE REVIEW (slice 13). The three delivered plants all fire a
;; detector on a synthetic POSITIVE and check it notices. The five below check
;; the other half -- that the detectors DISCRIMINATE -- because every finding in
;; this receipt is a negative, and a detector that says the same thing about
;; every input cannot support one.
(def synthetic-current-citation {:lo 470 :hi 480 :quoted-phrase "irrelevant here"})
(def synthetic-prose-mention
  {:edn? true :parsed? true :forms [{:note "prose that quotes :slow/mode in a string"}]})
(def synthetic-data-mention
  {:edn? true :parsed? true :forms [{:slow/mode :consolidation}]})
(def synthetic-unparsed-mention {:edn? true :parsed? false :forms []})

(def review-plant-results
  [(plant :citation-inside-its-range-reads-current
          :current (citation-status synthetic-current-citation {:line 476}))
   (plant :citation-whose-phrase-is-gone-reads-not-found
          :phrase-not-found (citation-status synthetic-current-citation nil))
   (plant :keyword-quoted-in-a-string-is-prose
          :prose (classify-mention synthetic-prose-mention))
   (plant :keyword-carried-as-data-is-a-machine-record
          :machine-record (classify-mention synthetic-data-mention))
   (plant :unparsable-artefact-is-reported-not-absorbed
          :unparsed (classify-mention synthetic-unparsed-mention))
   (plant :an-inference-require-would-be-seen
          ["futon2.aif.efe"]
          (vec (filter #(re-find inference-namespace-re %)
                       ["futon2.aif.rollout" "futon2.aif.efe"])))])

(def plant-results
  [{:id :synthetic-slow-trace
    :detected (count (filter slow-record? [synthetic-record]))
    :result (if (= 1 (count (filter slow-record? [synthetic-record]))) :caught :escaped)}
   {:id :synthetic-temporal-requirer
    :detected (count (filter #(re-find census-re (:text %)) synthetic-source))
    :result (if (= 1 (count (filter #(re-find census-re (:text %)) synthetic-source))) :caught :escaped)}
   {:id :synthetic-r15-equation
    :detected (count (filter #(re-find #":node\s+:R15(?:\s|[,}])" (:text %)) synthetic-registry))
    :result (if (= 1 (count (filter #(re-find #":node\s+:R15(?:\s|[,}])" (:text %)) synthetic-registry))) :caught :escaped)}])

(def validation-catalog (io/file lab "runs/RUNTIME-VALIDATION-CATALOG.edn"))
(def validation-entry-lines
  (vec (when-let [start (:line (first (matching-lines validation-catalog #":id :n/R15-")))]
         (let [tail (drop-while #(< (:line %) start) (numbered-lines validation-catalog))
               [before at] (split-with #(not (re-find #"^\s*:note" (:text %))) tail)]
           (mapv #(update % :text str/trim) (concat before (take 1 at)))))))
(def validation-ns
  (some (fn [{:keys [text]}] (second (re-find #":ns\s+\"([^\"]+)\"" text)))
        validation-entry-lines))
(def validation-ns-file
  (when validation-ns
    (str "test/" (str/replace (str/replace validation-ns "-" "_") "." "/") ".clj")))
(def validation-ns-in-census?
  (boolean (and validation-ns-file (some #(= validation-ns-file (:file %)) census-hits))))

(def registry-basis-pointer
  ;; Derived, not pinned: the row's own first and last line, and the line the
  ;; :plumbing list sits on.
  (format "holes/labs/wm-contract/aif-equations.edn:%d-%d,%d"
          (:line (first hierarchy-evidence-lines))
          (:line (last hierarchy-evidence-lines))
          (:line (first plumbing-lines))))

(def all-plants (vec (concat plant-results review-plant-results)))

(def checks
  [{:id :registry-has-no-r15-equation
    ;; REVIEW FIX (slice 13): the pass condition tested that the pinned window
    ;; 166-176 does NOT contain the quoted phrase, so it held whenever the pin
    ;; pointed anywhere unrelated -- including at a registry that had been
    ;; repaired. It now asserts that the citation PARSED and the phrase was
    ;; FOUND, and staleness is the measurement (:cited-rollout-status), not the
    ;; pass condition.
    :result (if (and (empty? equation-r15-lines)
                     (= 1 (count plumbing-lines))
                     (= 1 (count hierarchy-lines))
                     (some? cited-rollout-citation)
                     (some? quoted-phrase-location)) :pass :fail)
    :equation-search ":node :R15" :equation-row-count (count equation-r15-lines)
    :plumbing-lines plumbing-lines :hierarchy-lines hierarchy-lines
    :hierarchy-entry hierarchy-evidence-lines
    :cited-rollout-range (when cited-rollout-citation
                           (format "src/futon2/aif/rollout.clj:%d-%d"
                                   (:lo cited-rollout-citation)
                                   (:hi cited-rollout-citation)))
    :cited-rollout-quoted-phrase (:quoted-phrase cited-rollout-citation)
    :cited-rollout-text cited-rollout-lines
    :cited-rollout-status cited-rollout-status
    :quoted-phrase-found-at quoted-phrase-location
    :actual-flat-rollout-lines actual-flat-lines
    :hierarchical-budget-characterisation budget-doc-lines
    :basis registry-basis-pointer}
   {:id :the-node-has-a-namespace-the-registry-does-not-cite
    :result (if (and (.exists temporal-file) (seq temporal-doc-r15) (empty? temporal-registry-hits)) :pass :fail)
    :namespace "src/futon2/aif/temporal_hierarchy.clj"
    :namespace-r15-lines temporal-doc-r15
    :registry-search "(?i)temporal[-_ ]hierarchy|temporal_hierarchy"
    :registry-hit-count (count temporal-registry-hits)
    :basis "src/futon2/aif/temporal_hierarchy.clj:1-2"}
   {:id :requirer-census
    ;; REVIEW FIX (slice 13): the pass condition was (and (seq census-hits)
    ;; (map? census-by-tree)) -- `map?` of a map literal is true of every input,
    ;; so the check passed whatever the census found. It now asserts that the
    ;; per-tree partition ACCOUNTS FOR the census: the bucket line counts sum to
    ;; the hit count and the bucket file lists sum to the distinct file count,
    ;; either of which a mis-bucketed path breaks.
    :result (if (and (seq census-hits)
                     (= (count census-hits)
                        (reduce + (map :line-count (vals census-by-tree))))
                     (= (count (distinct (map :file census-hits)))
                        (reduce + (map #(count (:files %)) (vals census-by-tree)))))
              :pass :fail)
    :roots census-roots :search (str census-re)
    :hit-line-count (count census-hits) :file-count (count (distinct (map :file census-hits)))
    :by-tree census-by-tree
    :outside-namespace-test-and-harness-count (count external-runtime-hits)
    :outside-namespace-test-and-harness external-runtime-hits
    :finding (if (zero? (count external-runtime-hits)) :not-wired-outside-own-test :additional-requirers-found)}
   {:id :corpus-carries-no-slow-state
    :result (if (and (empty? corpus-slow) (empty? corpus-r15)) :pass :fail)
    :files-total (count (trace-files)) :records-total (count trace-records)
    :files-truncated-by-an-unreadable-form truncated-trace-files
    :first-file (some-> trace-records first :file) :last-file (some-> trace-records last :file)
    :slow-keys-searched slow-keys :records-with-slow-state (count corpus-slow)
    :slow-state-file-spread (file-spread corpus-slow)
    :records-with-r15-route (count corpus-r15) :r15-route-file-spread (file-spread corpus-r15)
    :basis "data/wm-trace/wm-trace-*.edn (read only)"}
   {:id :contract-criterion-measured
    :result (if (and (seq apply-prior-sites) (seq hierarchical-sites)
                     (seq advance-sites) (seq beta-update-sites) (seq own-efe-denial)) :pass :fail)
    :upper-parameterises-fast-prior? (boolean (and (seq apply-prior-sites) (seq hierarchical-sites)))
    :parameterisation-sites {:apply-slow-prior apply-prior-sites :hierarchical-rollout hierarchical-sites}
    ;; REVIEW FIX (slice 13): this field was the literal `false` -- a conclusion
    ;; transcribed from the docstring, not a measurement. It is now derived from
    ;; what the namespace REQUIRES: a level that ran its own generative
    ;; inference would have to reach an inference namespace to do it, and R15's
    ;; ns form reaches two namespaces, neither of them one.
    :belief-propagated-by-own-generative-inference? (boolean (seq inference-requires))
    :namespaces-required required-namespaces
    :inference-namespaces-required inference-requires
    :inference-namespace-search (str inference-namespace-re)
    :slow-update-kind :witnessed-outcome-beta-counter
    :slow-update-sites {:advance-slow-state advance-sites :beta-update beta-update-sites}
    :implementation-denial-of-full-nested-model own-efe-denial
    :basis "docs/futon-aif-completeness.md:434-439; src/futon2/aif/temporal_hierarchy.clj:1-39,190-237"}
   {:id :campaign-s-record
    ;; REVIEW FIX (slice 13): the delivered pass condition was
    ;; (and (.isDirectory runs) (vector? campaign-artifact-hits)) -- `vector?`
    ;; of a vector is true of every input, so the check passed whether it found
    ;; the campaign or not, which is the third consecutive slice whose review
    ;; found a pass condition that could not fail. The condition now requires
    ;; that every file mentioning a slow-state key was CLASSIFIED and that none
    ;; failed to parse, either of which can fail.
    :result (if (and (every? #{:machine-record :prose} (map :classification slow-key-mentions))
                     (empty? unparsed-mentions)) :pass :fail)
    :roots artefact-roots
    :extensions (str artefact-extensions)
    :files-searched (count artefact-files)
    :slow-key-search (str slow-key-mention-re)
    :files-mentioning-a-slow-key slow-key-mentions
    :machine-record-count (count machine-records)
    :machine-records machine-records
    :unparsed-count (count unparsed-mentions)
    :prose-search (str campaign-search)
    :prose-hit-count (count campaign-artifact-hits)
    :prose-hits campaign-artifact-hits
    :census-is-of-the-tree-at-run-time
    "The mention list is a measurement of the working tree, so a later document that quotes the keys lengthens it. The finding is the machine-record count, which parsing keeps at what the machine actually wrote."
    :finding (if (seq machine-records)
               :machine-record-found
               :no-machine-record-of-the-campaign-s-slow-state)
    :basis "/home/joe/code/p4ng/sec-catalog.tex:245"}
   {:id :campaign-s-cited-identifiers
    ;; ADDED BY THE REVIEW (slice 13). The narrow census asked whether a phrase
    ;; occurred; this one asks whether the two identifiers the campaign's own
    ;; narrative supplies -- its receipt and Tick A's attempt -- occur anywhere
    ;; a record could be. Pass means the identifiers were parsed out of the
    ;; narrative and every hit was classified; the finding is where they landed.
    :result (if (and (seq cited-identifiers)
                     (= (count identifier-hits)
                        (+ (count identifier-hits-in-narrative)
                           (count identifier-hits-in-this-sweep)
                           (count identifier-hits-in-records))))
              :pass :fail)
    :narrative-line campaign-narrative-lines
    :identifiers cited-identifiers
    :identifier-count (count cited-identifiers)
    :roots artefact-roots
    :files-searched (count artefact-files)
    :hits-in-narrative-prose identifier-hits-in-narrative
    :hits-in-this-sweep-s-own-bookkeeping identifier-hits-in-this-sweep
    :this-sweep-s-bookkeeping (vec (sort sweep-bookkeeping))
    :hits-in-records identifier-hits-in-records
    :finding (if (seq identifier-hits-in-records)
               :record-carrying-a-cited-identifier-found
               :no-record-carries-either-cited-identifier)
    :basis "/home/joe/code/p4ng/empirics.tex:50"}
   {:id :registered-runtime-validation-is-a-test-of-this-namespace
    ;; ADDED BY THE REVIEW (slice 13). The lab already registers a per-node
    ;; runtime validation for R15, and it names the node's own test namespace.
    ;; Read together with the requirer census above, that is the whole of R15's
    ;; green evidence: a suite that exercises a namespace nothing else calls.
    :result (if (and (seq validation-entry-lines)
                     (some? validation-ns)
                     validation-ns-in-census?) :pass :fail)
    :entry-search ":id :n/R15-"
    :entry validation-entry-lines
    :declared-ns validation-ns
    :declared-ns-file validation-ns-file
    :declared-ns-is-in-the-requirer-census? validation-ns-in-census?
    :and-nothing-outside-that-ns-requires-the-node (zero? (count external-runtime-hits))
    :basis (format "holes/labs/wm-contract/runs/RUNTIME-VALIDATION-CATALOG.edn:%d-%d"
                   (:line (first validation-entry-lines))
                   (:line (last validation-entry-lines)))}
   {:id :negative-controls-detect-nonzero-cases
    :result (if (every? #(= :caught (:result %)) all-plants) :pass :fail)
    :delivered-plants plant-results
    :review-plants review-plant-results
    :plants all-plants}])

(def receipt
  {:harness :v7-r15-node-sim :row :V7 :slice 13 :node :R15 :stage "SELECT"
   :stage-at "/home/joe/code/p4ng/empirics-futon/control-stages.edn:26"
   :carriers {:used? false :reason :census-node-has-no-equation-or-numerical-reference}
   :account-sites ["holes/labs/wm-contract/aif-equations.edn:373-376,521"
                   "/home/joe/code/p4ng/sec-catalog.tex:243,245"
                   "docs/futon-aif-completeness.md:434-439"
                   "src/futon2/aif/temporal_hierarchy.clj:1-39,190-237"]
   :checks checks
   :verdict (if (and (every? #(= :pass (:result %)) checks)
                     (every? #(= :caught (:result %)) all-plants)) :pass :fail)
   :negative-controls {:plants all-plants
                       :all-caught (every? #(= :caught (:result %)) all-plants)}
   :corpus {:root "data/wm-trace" :read-only true
            :file-count (count (trace-files)) :record-count (count trace-records)}
   :truncated-trace-files truncated-trace-files
   :not-done ["No equation reference exists for R15; this harness measures its process account."
              "No carriers file is needed because no numerical reference is constructed."
              "No live tick or run lock; trace files were read only."
              "No production, registry, ledger, document, p4ng, or data file was modified."
              "The slow-state census reads artefacts under holes, docs and p4ng. It does not read the futon3c or apm-lean trees, so an absence established here is an absence in the two repositories this row cites."]})

(def out receipt-file)
(.mkdirs (.getParentFile out))
(with-open [w (io/writer out)] (binding [*out* w] (pp/pprint receipt)))
(doseq [check checks]
  (println (format "  %-52s %s" (name (:id check)) (name (:result check)))))
(println "  verdict" (:verdict receipt) "receipt" (str out))
(System/exit (if (= :pass (:verdict receipt)) 0 1))
