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

(defn relative-path [f]
  (str (.relativize (.toPath (.getCanonicalFile (io/file ".")))
                    (.toPath (.getCanonicalFile f)))))

(defn source-hits [files re]
  (vec
   (mapcat (fn [f]
             (map #(assoc % :file (relative-path f)) (matching-lines f re)))
           files)))

(defn trace-files []
  (->> (.listFiles (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))))

(defn read-records [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [out []]
      (let [v (try (edn/read {:eof ::eof :default (fn [_ value] value)} r)
                   (catch Exception _ ::bad))]
        (cond (= v ::eof) out
              (= v ::bad) (recur out)
              :else (recur (conj out v)))))))

(def trace-records
  (vec (mapcat (fn [f]
                 (map-indexed (fn [index record]
                                {:file (.getName f) :index index :record record})
                              (read-records f)))
               (trace-files))))

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
(def hierarchy-evidence-lines (lines-in equation-file 373 376))
(def cited-rollout-lines (lines-in rollout-file 166 176))
(def actual-flat-lines (matching-lines rollout-file #"flat temporal rollout, not nested"))
(def budget-doc-lines (lines-in budget-file 1 3))
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
  #"advanced the relevant slow posterior|parameterised Tick B|Beta\(2,1\)|:slow/previous-mode")
(def run-artifact-files
  (->> (file-seq (io/file lab "runs"))
       (filter #(.isFile %))
       (filter #(re-find #"\.(edn|txt)$" (.getName %)))
       (remove #(= "holes/labs/wm-contract/runs/V7-R15-node-sim/00-r15.edn"
                   (relative-path %)))
       (sort-by #(.getCanonicalPath %))))
(def campaign-artifact-hits (source-hits run-artifact-files campaign-search))

(def synthetic-record {:slow/mode :consolidation})
(def synthetic-source [{:file "synthetic.clj" :line 1
                        :text "[futon2.aif.temporal-hierarchy :as th]"}])
(def synthetic-registry [{:file "synthetic.edn" :line 1
                          :text "{:id :synthetic :node :R15}"}])
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

(def checks
  [{:id :registry-has-no-r15-equation
    :result (if (and (empty? equation-r15-lines)
                     (= 1 (count plumbing-lines))
                     (= 1 (count hierarchy-lines))
                     (empty? (filter #(str/includes? (:text %) "flat temporal rollout") cited-rollout-lines))
                     (seq actual-flat-lines)) :pass :fail)
    :equation-search ":node :R15" :equation-row-count (count equation-r15-lines)
    :plumbing-lines plumbing-lines :hierarchy-lines hierarchy-lines
    :hierarchy-entry hierarchy-evidence-lines
    :cited-rollout-range "src/futon2/aif/rollout.clj:166-176"
    :cited-rollout-text cited-rollout-lines :cited-rollout-status :stale
    :actual-flat-rollout-lines actual-flat-lines
    :hierarchical-budget-characterisation budget-doc-lines
    :basis "holes/labs/wm-contract/aif-equations.edn:373-376,521"}
   {:id :the-node-has-a-namespace-the-registry-does-not-cite
    :result (if (and (.exists temporal-file) (seq temporal-doc-r15) (empty? temporal-registry-hits)) :pass :fail)
    :namespace "src/futon2/aif/temporal_hierarchy.clj"
    :namespace-r15-lines temporal-doc-r15
    :registry-search "(?i)temporal[-_ ]hierarchy|temporal_hierarchy"
    :registry-hit-count (count temporal-registry-hits)
    :basis "src/futon2/aif/temporal_hierarchy.clj:1-2"}
   {:id :requirer-census
    :result (if (and (seq census-hits) (map? census-by-tree)) :pass :fail)
    :roots census-roots :search (str census-re)
    :hit-line-count (count census-hits) :file-count (count (distinct (map :file census-hits)))
    :by-tree census-by-tree
    :outside-namespace-test-and-harness-count (count external-runtime-hits)
    :outside-namespace-test-and-harness external-runtime-hits
    :finding (if (zero? (count external-runtime-hits)) :not-wired-outside-own-test :additional-requirers-found)}
   {:id :corpus-carries-no-slow-state
    :result (if (and (empty? corpus-slow) (empty? corpus-r15)) :pass :fail)
    :files-total (count (trace-files)) :records-total (count trace-records)
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
    :belief-propagated-by-own-generative-inference? false
    :slow-update-kind :witnessed-outcome-beta-counter
    :slow-update-sites {:advance-slow-state advance-sites :beta-update beta-update-sites}
    :implementation-denial-of-full-nested-model own-efe-denial
    :basis "docs/futon-aif-completeness.md:434-439; src/futon2/aif/temporal_hierarchy.clj:1-39,190-237"}
   {:id :campaign-s-artifact
    :result (if (and (.isDirectory (io/file lab "runs"))
                     (vector? campaign-artifact-hits)) :pass :fail)
    :search (str campaign-search)
    :roots ["holes/labs/wm-contract/runs/**/*.edn" "holes/labs/wm-contract/runs/**/*.txt"]
    :artifact-hit-count (count campaign-artifact-hits)
    :artifact-hits campaign-artifact-hits
    :recorded-numbers (if (seq campaign-artifact-hits) :inspect-hits :not-found)
    :finding (if (seq campaign-artifact-hits) :candidate-artifact-found :not-found)
    :basis "/home/joe/code/p4ng/sec-catalog.tex:245"}
   {:id :negative-controls-detect-nonzero-cases
    :result (if (every? #(= :caught (:result %)) plant-results) :pass :fail)
    :plants plant-results}])

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
                     (every? #(= :caught (:result %)) plant-results)) :pass :fail)
   :negative-controls {:plants plant-results
                       :all-caught (every? #(= :caught (:result %)) plant-results)}
   :corpus {:root "data/wm-trace" :read-only true
            :file-count (count (trace-files)) :record-count (count trace-records)}
   :not-done ["No equation reference exists for R15; this harness measures its process account."
              "No carriers file is needed because no numerical reference is constructed."
              "No live tick or run lock; trace files were read only."
              "No production, registry, ledger, document, p4ng, or data file was modified."]})

(def out (io/file lab "runs/V7-R15-node-sim/00-r15.edn"))
(.mkdirs (.getParentFile out))
(with-open [w (io/writer out)] (binding [*out* w] (pp/pprint receipt)))
(doseq [check checks]
  (println (format "  %-52s %s" (name (:id check)) (name (:result check)))))
(println "  verdict" (:verdict receipt) "receipt" (str out))
(System/exit (if (= :pass (:verdict receipt)) 0 1))
