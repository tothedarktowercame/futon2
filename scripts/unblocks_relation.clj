(ns unblocks-relation
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-pipeline-selector :as pipeline]))

(def run-path "data/wm-runs/tick-run-record-2026-10-05-c9d25d6a-f2bb-42bf-a162-2c4a000e804f.edn")
(def graph-url "http://127.0.0.1:7070/api/alpha/cascade-real/graph")
(def mined-path "/home/joe/code/storage/operator-turns/mined-pattern-graph.json")
(def lifecycle-path "/home/joe/code/futon4/holes/mission-lifecycle.md")
(def skeleton-path "/home/joe/code/futon3/library/cascades/declared-skeleton.flexiarg")
(def report-path "holes/labs/wm-contract/REPORT-unblocks-relation-2026-10-05.md")

(def cue-directions
  {"depends on" :target-to-document, "after" :target-to-document
   "blocked by" :target-to-document, "blocks" :document-to-target
   "unblocks" :document-to-target, "feeds" :document-to-target
   "prerequisite" :target-to-document, "requires" :target-to-document
   "enables" :document-to-target, "see" :undirected, "cf." :undirected
   "follows" :target-to-document, "successor" :document-to-target
   "predecessor" :target-to-document, "supersedes" :undirected
   "part of" :undirected})
(def cues (sort-by (comp - count) (keys cue-directions)))
(def id-pattern
  #"(?i)(?:[a-z0-9_-]+/(?:mission|excursion|ticket)/[a-z0-9][a-z0-9._-]*|[MET]-[a-z0-9][a-z0-9._-]*)")
(def header-pattern
  #"(?i)^\s*(?:[-*]\s*)?(?:\*\*)?((?:parent(?: mission| theory| audit| endpoints[^:*]*)?|predecessor|successor|consumer(?: mission)?|downstream consumer references|rests on))(?:\*\*)?\s*:\s*(.+)$")

(defn sha256 [s] (field/sha256 (.getBytes ^String s "UTF-8")))
(defn slurp-pin [path] (let [body (slurp path)] {:path path :sha256 (sha256 body) :body body}))
(defn canonical-id [raw]
  (let [clean (str/replace raw #"[.]+$" "")]
    (if (re-find #"/" clean) (pipeline/canonical-work-id clean) clean)))
(defn positions [text needle]
  (let [matcher (re-matcher
                 (re-pattern (str "(?i)(?<![\\p{L}\\p{N}_-])"
                                  (java.util.regex.Pattern/quote needle)
                                  "(?![\\p{L}\\p{N}_-])")) text)]
    (loop [out []]
      (if (.find matcher) (recur (conj out (.start matcher))) out))))
(defn nearest-cue [sentence mention-start]
  (->> cues
       (mapcat (fn [cue] (map (fn [p] {:cue cue :distance (Math/abs (long (- p mention-start)))})
                               (positions sentence cue))))
       (sort-by (juxt :distance #(get % :cue))) first :cue))
(defn sentence-parts [line]
  (str/split line #"(?<=[!?])\s+|(?<=\.)\s+(?=[A-Z`*])"))

(defn document-mentions [item universe]
  (let [path (get-in item [:source :path])]
    (mapcat
     (fn [[line-number line]]
       (mapcat
        (fn [sentence]
          (let [matcher (re-matcher id-pattern sentence)]
            (loop [out []]
              (if (.find matcher)
                (let [raw (.group matcher) target (canonical-id raw)]
                  (recur (if (and (contains? universe target) (not= target (:id item)))
                           (conj out {:document (:id item) :target target :raw raw
                                      :path path :line line-number :text (str/trim line)
                                      :cue (or (nearest-cue sentence (.start matcher)) :no-cue)}) out)))
                out))))
        (sentence-parts line)))
     (map-indexed (fn [i line] [(inc i) line]) (str/split-lines (slurp path))))))

(defn directed-edge [{:keys [document target cue] :as mention}]
  (case (get cue-directions cue)
    :document-to-target (assoc mention :from document :to target)
    :target-to-document (assoc mention :from target :to document)
    nil))

(defn lifecycle-fields [item]
  (let [lines (take 50 (str/split-lines (slurp (get-in item [:source :path]))))]
    (->> lines
         (keep (fn [line]
                 (when-let [[_ label value] (re-matches header-pattern line)]
                   {:field (str/trim label) :value (str/trim value)})))
         vec)))

(defn fetch-graph []
  (let [response (http/get graph-url {:timeout 180000 :throw false}) body (str (:body response))]
    (when-not (= 200 (:status response))
      (throw (ex-info "Cascade Live graph refused" {:status (:status response)})))
    {:body body :pin {:path graph-url :sha256 (sha256 body)}
     :graph (json/parse-string body true)}))
(defn applied-relation [graph]
  (let [pairs (keep (fn [{:keys [mission pattern relation]}]
                      (when (and (= "applied" relation) (pipeline/canonical-work-id mission))
                        [(pipeline/canonical-work-id mission) pattern]))
                    (get-in graph [:patterns :edges]))]
    {:mission-patterns (reduce (fn [m [id p]] (update m id (fnil conj #{}) p)) {} pairs)
     :pattern-missions (reduce (fn [m [id p]] (update m p (fnil conj #{}) id)) {} pairs)
     :pairs (count pairs)}))
(defn lifted-why [applied why-edges]
  (let [pm (:pattern-missions applied)]
    (reduce (fn [edges {:keys [a b]}]
              (into edges (for [x (get pm a #{}) y (get pm b #{}) :when (not= x y)] [x y])))
            #{} why-edges)))
(defn shared-count [{:keys [mission-patterns pattern-missions]} id]
  (count (disj (reduce set/union #{} (map pattern-missions (get mission-patterns id #{}))) id)))

(defn ranks [xs]
  (let [ordered (sort-by (juxt second first) (map-indexed vector xs))]
    (reduce (fn [out group]
              (let [ps (map #(.indexOf ^java.util.List ordered %) group)
                    avg (/ (+ 2 (apply min ps) (apply max ps)) 2.0)]
                (reduce (fn [v [i _]] (assoc v i avg)) out group)))
            (vec (repeat (count xs) 0.0)) (partition-by second ordered))))
(defn spearman [xs ys]
  (let [xs (ranks xs) ys (ranks ys) n (count xs) mx (/ (reduce + xs) n) my (/ (reduce + ys) n)
        dx (map #(- % mx) xs) dy (map #(- % my) ys)
        d (Math/sqrt (* (reduce + (map #(* % %) dx)) (reduce + (map #(* % %) dy))))]
    (when (pos? d) (/ (reduce + (map * dx dy)) d))))
(defn fmt [x] (if (number? x) (format "%.6f" (double x)) "`:undefined-zero-variance`"))
(defn md [x] (-> (str x) (str/replace "|" "\\|") (str/replace "\n" " ")))

(defn render [{:keys [head run-pin field-observation graph-pin mined-pin lifecycle-pin skeleton-pin
                      rows mentions edges why-count applied-count lifted-count lifecycle-labels rhos]}]
  (let [top (take 10 (sort-by (juxt (comp - :cued-out) :id) rows))
        edge-evidence (group-by :from edges)
        fields-filled (count (filter (comp seq :lifecycle-fields) rows))]
    (str "# Inter-item `@why` relation discovery — 2026-10-05\n\n"
         "Reproduce from `/home/joe/code/futon2` in a fresh process:\n\n```sh\nclojure -M scripts/unblocks_relation.clj\n```\n\n"
         "futon2 HEAD before generation: `" head "`. Ranked population: `" (:path run-pin) "` / `" (:sha256 run-pin) "`. HEAD field observation: " (count (:rows field-observation)) " rows, pin `" (get-in field-observation [:source-pin :sha256]) "`. Cascade Live: `" (:sha256 graph-pin) "`. Mined graph: `" (:path mined-pin) "` / `" (:sha256 mined-pin) "`.\n\n"
         "## Cue directions\n\n| cue | direction convention |\n|---|---|\n"
         (str/join "\n" (for [[cue direction] (sort-by key cue-directions)]
                            (str "| `" cue "` | `" direction "` |")))
         "\n\n`document-to-target` means completing the document item is read as unblocking the mentioned target. `target-to-document` means the mentioned item is read as unblocking the document item. `undirected` is a cued cross-reference but contributes no directed edge. The nearest cue in the same sentence is used; otherwise the mention is `:no-cue`.\n\n"
         "## Sources\n\n"
         "**(i) Document cross-references.** " (count mentions) " mentions of another item in the HEAD universe were found: "
         (count (filter #(= :no-cue (:cue %)) mentions)) " uncued and " (count (remove #(= :no-cue (:cue %)) mentions))
         " cued. The directional conventions yield " (count (set (map (juxt :from :to) edges))) " distinct directed edges. Every occurrence is listed below with file and line.\n\n"
         "**(ii) Lifecycle fields.** `" (:path lifecycle-pin) "` / `" (:sha256 lifecycle-pin) "` requires IDENTIFY to state “Relationship to other missions” and “Owner and dependencies,” but defines no machine-readable predecessor, successor, parent, consumer, or rests-on field. A header scan found " fields-filled " of 107 documents filling a prose header whose label contains one of those words. Labels and document counts: `" (pr-str lifecycle-labels) "`. Items without one carry `:typed-absence` (`:no-lifecycle-relation-field`).\n\n"
         "**(iii) Lifted library `why` edges.** The mined graph contains " why-count " `why` edges. Lifting them through "
         applied-count " canonical mission→pattern applications in Cascade Live yields " lifted-count
         " distinct ordered item pairs. For `(X,Y)`, X applies pattern p, Y applies q, and `p why q`; the table reports how many Y each X derived-rests-on and how many X each Y derived-supports. This is a derived relation about methods, not evidence that one item depends on another.\n\n"
         "## Per-item values\n\n| id | kind | (i) cued out | (i) cued in | (i) uncued mentions | (ii) lifecycle fields | (iii) derived rests-on | (iii) derived supports |\n|---|---|---:|---:|---:|---|---:|---:|\n"
         (str/join "\n" (for [{:keys [id kind cued-out cued-in uncued lifecycle-fields derived-out derived-in]} rows]
                            (str "| `" id "` | `" kind "` | " cued-out " | " cued-in " | " uncued " | "
                                 (if (seq lifecycle-fields) (md (pr-str lifecycle-fields)) "`:typed-absence` (`:no-lifecycle-relation-field`)")
                                 " | " derived-out " | " derived-in " |")))
         "\n\n## Top 10 by document-cued out-degree\n\n"
         (str/join "\n\n" (for [{:keys [id cued-out]} top]
                                (str "### `" id "` — " cued-out "\n\n"
                                     (if-let [es (seq (get edge-evidence id))]
                                       (str/join "\n" (for [{:keys [path line to cue text]} es]
                                                          (str "- `" path ":" line "` → `" to "` (`" cue "`): " (md text))))
                                       "- No directed citing line."))))
         "\n\n## Correlations\n\nSpearman rho uses average ranks for ties over all 107 items; a scanned document with no directed cued edge has out-degree zero, not an absence.\n\n| comparison with (i) cued out-degree | rho |\n|---|---:|\n| persisted occurrence count | " (fmt (:occurrence rhos)) " |\n| B1 (b), shared-pattern missions | " (fmt (:shared rhos)) " |\n\n"
         "## All document mentions\n\n| document | target | cue | source | text |\n|---|---|---|---|---|\n"
         (str/join "\n" (for [{:keys [document target cue path line text]} mentions]
                            (str "| `" document "` | `" target "` | `" cue "` | `" path ":" line "` | " (md text) " |")))
         "\n\n## Directed document graph\n\n| unblocks | item resting on it | cue | source |\n|---|---|---|---|\n"
         (str/join "\n" (for [{:keys [from to cue path line]} edges]
                            (str "| `" from "` | `" to "` | `" cue "` | `" path ":" line "` |")))
         "\n\n## Declaration proposal\n\nFollowing `" (:path skeleton-pin) "` / `" (:sha256 skeleton-pin) "`, an item could carry a plural line such as `**Unblocks:** M-x — because <one reviewable sentence>; E-y — because <one reviewable sentence>`. Each edge would point from this item to what rests on it, and would be declared only when its author would defend that completion of the source really changes the target from unanswered to answerable in review. Derived `why` lifts and uncued mentions may prompt that declaration, but must not mint it.\n")))

(defn -main [& _]
  (let [run-pin (slurp-pin run-path) run (edn/read-string (:body run-pin))
        ranked (get-in run [:outer-task-selection :policy :meta-selection :ranking])
        support (into {} (map (juxt :id identity)) (get-in run [:outer-task-selection :support]))
        fo (field/observe {})
        _ (when-not (= :observed (:status fo)) (throw (ex-info "field observation refused" fo)))
        fetched (fetch-graph) graph (:graph fetched) applied (applied-relation graph)
        qualified-ids (set (keep pipeline/canonical-work-id
                                 (concat (map :mission (get-in graph [:patterns :edges]))
                                         (mapcat (juxt :have :want) (:arrows graph)))))
        universe (set/union (set (map :id (:rows fo))) qualified-ids)
        items (mapv #(merge (select-keys % [:id :kind]) (select-keys (get support (:id %)) [:source])) ranked)
        _ (when (some #(nil? (get-in % [:source :path])) items) (throw (ex-info "ranked source absent" {})))
        mentions (->> items (mapcat #(document-mentions % universe)) (sort-by (juxt :document :line :target)) vec)
        edges (->> mentions (keep directed-edge) (sort-by (juxt :from :to :path :line)) vec)
        edge-pairs (set (map (juxt :from :to) edges))
        mined-pin (slurp-pin mined-path) mined (json/parse-string (:body mined-pin) true)
        why-edges (filter #(= "why" (:kind %)) (:edges mined))
        lifted (lifted-why applied why-edges)
        lifecycle-pin (slurp-pin lifecycle-path) skeleton-pin (slurp-pin skeleton-path)
        rows (mapv (fn [{:keys [id kind] :as item}]
                     (let [fields (lifecycle-fields item)]
                       {:id id :kind kind
                        :occurrence (get-in (first (filter #(= id (:id %)) ranked)) [:channels :pipeline-structural-centrality-cost :observation :value])
                        :shared (shared-count applied id)
                        :cued-out (count (set (map second (filter #(= id (first %)) edge-pairs))))
                        :cued-in (count (set (map first (filter #(= id (second %)) edge-pairs))))
                        :uncued (count (filter #(and (= id (:document %)) (= :no-cue (:cue %))) mentions))
                        :lifecycle-fields fields
                        :derived-out (count (set (map second (filter #(= id (first %)) lifted))))
                        :derived-in (count (set (map first (filter #(= id (second %)) lifted))))})) items)
        out (mapv :cued-out rows)
        result {:head (str/trim (:out (shell/sh "git" "rev-parse" "HEAD")))
                :run-pin run-pin :field-observation fo :graph-pin (:pin fetched) :mined-pin mined-pin
                :lifecycle-pin lifecycle-pin :skeleton-pin skeleton-pin :rows rows :mentions mentions :edges edges
                :why-count (count why-edges) :applied-count (:pairs applied) :lifted-count (count lifted)
                :lifecycle-labels (frequencies (map :field (mapcat :lifecycle-fields rows)))
                :rhos {:occurrence (spearman out (mapv :occurrence rows))
                       :shared (spearman out (mapv :shared rows))}}]
    (spit report-path (render result))
    (println report-path)))

(apply -main *command-line-args*)
