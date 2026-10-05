(ns centrality-tokens
  (:require [babashka.http-client :as http] [cheshire.core :as json]
            [clojure.edn :as edn] [clojure.java.shell :as shell]
            [clojure.set :as set] [clojure.string :as str]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-pipeline-selector :as pipeline]))

(def run-path "data/wm-runs/tick-run-record-2026-10-05-c9d25d6a-f2bb-42bf-a162-2c4a000e804f.edn")
(def graph-url "http://127.0.0.1:7070/api/alpha/cascade-real/graph")
(def pattern-path "/home/joe/code/storage/operator-turns/mined-pattern-graph.json")
(def report-path "holes/labs/wm-contract/REPORT-centrality-tokens-2026-10-05.md")
(def reference-id "M-interim-director-proxy-metric-inventory")
(def reviewed-sha "2895d7132e86b388861e5d1b9a40588b4166082e197049ebd22632e996396858")

(defn pin [path body] {:path path :sha256 (field/sha256 (.getBytes ^String body "UTF-8"))})
(defn pin-text [{:keys [path sha256]}] (str "`" path "` / `" sha256 "`"))
(defn fetch-graph []
  (let [r (http/get graph-url {:timeout 180000 :throw false}) body (str (:body r))]
    (when-not (= 200 (:status r))
      (throw (ex-info "Cascade Live graph refused" {:status (:status r) :body-prefix (subs body 0 (min 500 (count body)))})))
    {:body body :pin (pin graph-url body) :graph (json/parse-string body true)}))

(defn ranks [xs]
  (let [ordered (sort-by (juxt second first) (map-indexed vector xs))]
    (reduce (fn [out group]
              (let [ps (map #(.indexOf ^java.util.List ordered %) group)
                    avg (/ (+ 2 (apply min ps) (apply max ps)) 2.0)]
                (reduce (fn [v [i _]] (assoc v i avg)) out group)))
            (vec (repeat (count xs) 0.0)) (partition-by second ordered))))
(defn rho [xs ys]
  (let [xs (ranks xs) ys (ranks ys) n (count xs)
        mx (/ (reduce + xs) n) my (/ (reduce + ys) n)
        dx (map #(- % mx) xs) dy (map #(- % my) ys)
        d (Math/sqrt (* (reduce + (map #(* % %) dx)) (reduce + (map #(* % %) dy))))]
    (when (pos? d) (/ (reduce + (map * dx dy)) d))))
(defn fmt [x] (format "%.6f" (double x)))
(defn occurrence [row] (get-in row [:channels :pipeline-structural-centrality-cost :observation :value]))

(defn arrow-facts [graph ranked-ids]
  (let [arrows (:arrows graph) haves (set (map :have arrows)) wants (set (map :want arrows))
        canonical-wants (set (keep pipeline/canonical-work-id wants))]
    {:count (count arrows) :haves (count haves) :wants (count wants)
     :ranked-wants (count (set/intersection ranked-ids canonical-wants))}))

(defn relation [graph]
  (let [all (get-in graph [:patterns :edges]) applied (filter #(= "applied" (:relation %)) all)
        rows (keep (fn [{:keys [mission pattern]}]
                     (when-let [id (pipeline/canonical-work-id mission)] [id pattern])) applied)
        mp (reduce (fn [m [id p]] (update m id (fnil conj #{}) p)) {} rows)
        pm (reduce (fn [m [id p]] (update m p (fnil conj #{}) id)) {} rows)]
    {:edge-count (count all) :applied-count (count applied) :canonical-count (count rows)
     :mission-count (count mp) :mission-patterns mp :pattern-missions pm}))

(defn measures [{:keys [mission-patterns pattern-missions]} id]
  (let [ps (get mission-patterns id #{}) others (disj (reduce set/union #{} (map pattern-missions ps)) id)]
    {:patterns (count ps) :shared (count others)
     :incidences (reduce + 0 (map #(count (disj (get pattern-missions % #{}) id)) ps))}))
(defn mean [xs] (/ (reduce + xs) (double (count xs))))

(defn render [{:keys [head run-pin run-graph-pin graph-pin graph arrows rel pattern-pin pattern-graph rows rhos reference]}]
  (let [by-kind (sort-by key (group-by :kind rows)) moved? (not= reviewed-sha (:sha256 graph-pin))]
    (str "# Centrality tokens for the persisted 107-item ranking — 2026-10-05\n\n"
         "Reproduce in a fresh process from `/home/joe/code/futon2`:\n\n```sh\nclojure -M scripts/centrality_tokens.clj\n```\n\n"
         "futon2 HEAD before generation: `" head "`. The 107 IDs and occurrence counts are read from the persisted ranking; no fresh selection is performed.\n\n"
         "## Sources and relations\n\n"
         "**[R] Persisted run record.** " (pin-text run-pin) ". Its ranking rows carry `:pipeline-structural-occurrences`; their graph source pin is " (pin-text run-graph-pin) ".\n\n"
         "**[G] Cascade Live graph fetched today.** " (pin-text graph-pin) ". It has " (:count arrows) " arrows, " (:haves arrows) " distinct haves, " (:wants arrows) " distinct wants, and " (:ranked-wants arrows) " wants that canonicalise to a persisted task ID. The arrows relate each mission to its own next hole, not tasks to other tasks. Both requested degrees are therefore `:typed-absence` (`:arrows-are-mission-to-own-hole`). `:lineage` has " (count (:lineage graph)) " agent/target/session dispatch records; it is dispatch lineage, not predecessor/successor.\n\n"
         "[G] `[:patterns :edges]` has " (:edge-count rel) " rows, of which " (:applied-count rel) " are `\"applied\"`; canonicalisation retained " (:canonical-count rel) " rows over " (:mission-count rel) " missions. Missions with no pattern edge get zero. "
         (if moved? (str "The graph moved from reviewed SHA `" reviewed-sha "`; current SHA is `" (:sha256 graph-pin) "`. ")
             (str "The graph matches reviewed SHA `" reviewed-sha "`. "))
         "The current `" reference-id "` reference is (a) " (:patterns reference) ", (b) " (:shared reference)
         (if (= [34 68] [(:patterns reference) (:shared reference)]) ", matching 34/68.\n\n" ", differing from 34/68.\n\n")
         "**[P] Mined pattern graph.** " (pin-text pattern-pin) ". Its " (count (:edges pattern-graph)) " edges are pattern-to-pattern relations and contain no task-to-pattern relation: `:typed-absence` (`:no-task-to-pattern-relation`). It is not used to approximate [G].\n\n"
         "**[L] claude-4 Lean rows record.** Agency reports mission `M-diagramprover`. A bounded search of `futon2/holes`, `futon3c/holes`, and `mathlib4/DarkTower` for claude-4 plus rows/unblock/sorry found no defining or tabulating record: `:typed-absence` (`:no-recorded-lean-row-unblocking-measure-found`). No number was recomputed.\n\n"
         "## Measures and token\n\n(a) counts distinct applied patterns; (b) counts distinct **other** missions sharing at least one pattern; (c) sums, over the item's patterns, the number of other missions applying that pattern. Thus (c) can count one mission repeatedly.\n\n"
         "| task kind | items | mean (a) | mean (b) | mean (c) |\n|---|---:|---:|---:|---:|\n"
         (str/join "\n" (for [[kind rs] by-kind]
                            (str "| `" kind "` | " (count rs) " | " (fmt (mean (map :patterns rs))) " | "
                                 (fmt (mean (map :shared rs))) " | " (fmt (mean (map :incidences rs))) " |")))
         "\n\nProposed token: `:meta/downstream-unblocking-count`, type `:non-negative-integer`, emitted by the centrality reading square and consumed by `minimise-g-over-filled-meta-policies` through `:downstream-unblocking`. Its value is either (b) or (c), to be chosen at B3; this report does not choose.\n\n"
         "## Correlations\n\nSpearman rho uses average ranks for ties over 107 items. No rho is reported for arrow degrees: the task-to-task relation is absent, and treating absence as zero would manufacture a measure.\n\n"
         "| comparison with persisted occurrence count | rho |\n|---|---:|\n"
         "| (a) distinct patterns | " (fmt (:patterns rhos)) " |\n| (b) shared missions | " (fmt (:shared rhos)) " |\n| (c) shared incidences | " (fmt (:incidences rhos)) " |\n\n"
         "## Per-item values\n\n| id | kind | occurrence | arrow out | arrow in | (a) | (b) | (c) | [P] relation |\n|---|---|---:|---|---|---:|---:|---:|---|\n"
         (str/join "\n" (for [{:keys [id kind occurrence patterns shared incidences]} rows]
                            (str "| `" id "` | `" kind "` | " occurrence " [R] | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | `:typed-absence` [G] `:arrows-are-mission-to-own-hole` | " patterns " [G] | " shared " [G] | " incidences " [G] | `:typed-absence` [P] `:no-task-to-pattern-relation` |")))
         "\n\n## What the numbers say\n\nAcross the 107 rows, occurrence count has Spearman rho " (fmt (:patterns rhos)) ", " (fmt (:shared rhos)) ", and " (fmt (:incidences rhos)) " with (a), (b), and (c). [G] supplies these via applied-pattern edges. Its arrows and lineage describe different relations; [P] and [L] remain typed absences.\n")))

(defn -main [& _]
  (let [run-body (slurp run-path) run (edn/read-string run-body)
        ranked (get-in run [:outer-task-selection :policy :meta-selection :ranking])
        _ (when-not (= 107 (count ranked)) (throw (ex-info "persisted ranking count changed" {:count (count ranked)})))
        run-graph-pin (get-in (first ranked) [:channels :pipeline-structural-centrality-cost :source])
        _ (when-not (every? #(= run-graph-pin (get-in % [:channels :pipeline-structural-centrality-cost :source])) ranked)
            (throw (ex-info "ranking rows do not share one graph pin" {})))
        fetched (fetch-graph) graph (:graph fetched) rel (relation graph)
        rows (mapv (fn [r] (merge {:id (:id r) :kind (:kind r) :occurrence (occurrence r)} (measures rel (:id r)))) ranked)
        _ (when (some #(nil? (:occurrence %)) rows) (throw (ex-info "ranked row lacks occurrence" {})))
        occurrences (mapv :occurrence rows) pattern-body (slurp pattern-path)
        result {:head (str/trim (:out (shell/sh "git" "rev-parse" "HEAD")))
                :run-pin (pin run-path run-body) :run-graph-pin run-graph-pin
                :graph-pin (:pin fetched) :graph graph :arrows (arrow-facts graph (set (map :id ranked))) :rel rel
                :pattern-pin (pin pattern-path pattern-body) :pattern-graph (json/parse-string pattern-body true)
                :rows rows :reference (measures rel reference-id)
                :rhos {:patterns (rho occurrences (mapv :patterns rows))
                       :shared (rho occurrences (mapv :shared rows))
                       :incidences (rho occurrences (mapv :incidences rows))}}]
    (spit report-path (render result))
    (println report-path)))

(apply -main *command-line-args*)
