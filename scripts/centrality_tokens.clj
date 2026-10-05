(ns centrality-tokens
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-pipeline-selector :as pipeline]))

(def run-path
  "data/wm-runs/tick-run-record-2026-10-05-c9d25d6a-f2bb-42bf-a162-2c4a000e804f.edn")
(def graph-url "http://127.0.0.1:7070/api/alpha/cascade-real/graph")
(def pattern-path "/home/joe/code/storage/operator-turns/mined-pattern-graph.json")
(def report-path "holes/labs/wm-contract/REPORT-centrality-tokens-2026-10-05.md")

(defn sha-pin [path body]
  {:path path :sha256 (field/sha256 (.getBytes ^String body "UTF-8"))})

(defn fetch-graph []
  (let [response (http/get graph-url {:headers {"Accept" "application/json"}
                                      :timeout 180000 :throw false})
        body (str (:body response))]
    (when-not (= 200 (:status response))
      (throw (ex-info "Cascade Live graph refused"
                      {:status (:status response) :url graph-url
                       :body-prefix (subs body 0 (min 500 (count body)))})))
    {:body body :pin (sha-pin graph-url body) :value (json/parse-string body true)}))

(defn rank-values [values]
  (let [indexed (sort-by (juxt second first) (map-indexed vector values))]
    (reduce (fn [out same-value]
              (let [positions (map #(.indexOf ^java.util.List indexed %) same-value)
                    avg (/ (+ (inc (apply min positions)) (inc (apply max positions))) 2.0)]
                (reduce (fn [o [original-index _]] (assoc o original-index avg)) out same-value)))
            (vec (repeat (count values) 0.0))
            (partition-by second indexed))))

(defn pearson [xs ys]
  (let [n (count xs) mx (/ (reduce + xs) n) my (/ (reduce + ys) n)
        dx (map #(- % mx) xs) dy (map #(- % my) ys)
        denominator (Math/sqrt (* (reduce + (map #(* % %) dx))
                                  (reduce + (map #(* % %) dy))))]
    (when (pos? denominator)
      (/ (reduce + (map * dx dy)) denominator))))

(defn spearman [xs ys] (pearson (rank-values xs) (rank-values ys)))
(defn fmt-rho [x] (if (some? x) (format "%.6f" (double x)) "typed absence"))
(defn fmt-pin [{:keys [path sha256]}] (str "`" path "` / `" sha256 "`"))
(defn cell [x source]
  (if (number? x) (str x " [" source "]")
      (str "`:typed-absence` [" source "]")))

(defn graph-occurrence [row]
  (get-in row [:channels :pipeline-structural-centrality-cost :observation :value]))

(defn canonical-arrows [graph]
  (keep (fn [{:keys [have want] :as arrow}]
          (let [from (pipeline/canonical-work-id have)
                to (pipeline/canonical-work-id want)]
            (when (and from to) (assoc arrow :from-id from :to-id to))))
        (:arrows graph)))

(defn pattern-relation [pattern-graph]
  (let [edges (:edges pattern-graph)
        task-id? #(and (string? %) (boolean (re-matches #"^[MET]-[^/]+$" %)))
        item-links (filter #(or (task-id? (:a %)) (task-id? (:b %))) edges)]
    {:edge-count (count edges)
     :edge-kinds (into (sorted-map) (frequencies (map :kind edges)))
     :item-pattern-links (count item-links)
     :status (if (seq item-links) :present :typed-absence)
     :reason (when-not (seq item-links) :no-task-to-pattern-relation)}))

(defn render [{:keys [futon2-sha ranked graph-pin run-graph-pin graph pattern-pin
                      pattern-graph pattern-relation rows correlations]}]
  (let [graph-keys (sort (keys graph))
        pattern-keys (sort (keys pattern-graph))
        kinds (frequencies (map :kind rows))
        max-out (apply max (map :out-degree rows))
        max-in (apply max (map :in-degree rows))
        by-kind (sort-by key (group-by :kind rows))]
    (str "# Centrality tokens for the persisted 107-item ranking — 2026-10-05\n\n"
         "Reproduce in a fresh process from `/home/joe/code/futon2`:\n\n"
         "```sh\nclojure -M scripts/centrality_tokens.clj\n```\n\n"
         "futon2 HEAD: `" futon2-sha "`. The item population and live occurrence counts come from the persisted run ranking; no fresh selection is performed.\n\n"
         "## Sources and relations\n\n"
         "**[R] Persisted run record.** " (fmt-pin {:path run-path :sha256 (field/sha256 (.getBytes (slurp run-path) "UTF-8"))})
         ". It contains " (count ranked) " ranking rows (" (str/join ", " (map (fn [[k n]] (str k " " n)) (sort-by key kinds)))
         "), each with the selector's `:pipeline-structural-occurrences` observation. Its graph source pin is " (fmt-pin run-graph-pin) ".\n\n"
         "**[G] Cascade Live graph fetched today.** " (fmt-pin graph-pin) ". Top-level keys: "
         (str/join ", " (map #(str "`" % "`") graph-keys)) ". It contains " (count (:arrows graph))
         " arrows; each arrow exposes `:have` and `:want`. "
         (if (= graph-pin run-graph-pin) "Its bytes match the run's graph pin."
             "Its bytes do not match the run's graph pin; the table therefore labels persisted occurrences [R] and today's degrees [G] separately.") "\n\n"
         "**[P] Mined pattern graph.** " (fmt-pin pattern-pin) ". Top-level keys: "
         (str/join ", " (map #(str "`" % "`") pattern-keys)) ". It contains " (:edge-count pattern-relation)
         " pattern edges with kinds " (pr-str (:edge-kinds pattern-relation)) ". It contains "
         (:item-pattern-links pattern-relation) " task-to-pattern links, so `pattern-shared-count` is `:typed-absence` (`"
         (:reason pattern-relation) "`) for every ranked item.\n\n"
         "**[L] claude-4 Lean rows record.** Agency reports claude-4's mission as `M-diagramprover`. A bounded search of `futon2/holes`, `futon3c/holes`, and `mathlib4/DarkTower` for claude-4 plus rows/unblock/sorry found no record defining or tabulating the requested measure. Status: `:typed-absence`; reason: `:no-recorded-lean-row-unblocking-measure-found`. No number was recomputed.\n\n"
         "## Measure and token\n\n"
         "For missions, excursions, and tickets, `have→want-out-degree` is the number of distinct **other** canonical task IDs appearing as the `:want` endpoint of an arrow whose `:have` endpoint is the item. `have→want-in-degree` reverses that question: distinct other `:have` endpoints on arrows whose `:want` is the item. The ranked population has no algorithm rows; the separately requested Lean-row definition is [L]'s typed absence.\n\n"
         "| task kind | ranked items | mean out-degree | max out-degree | mean in-degree | max in-degree |\n"
         "|---|---:|---:|---:|---:|---:|\n"
         (str/join "\n" (for [[kind rs] by-kind]
                            (str "| `" kind "` | " (count rs)
                                 " | " (format "%.6f" (/ (reduce + (map :out-degree rs)) (double (count rs))))
                                 " | " (apply max (map :out-degree rs))
                                 " | " (format "%.6f" (/ (reduce + (map :in-degree rs)) (double (count rs))))
                                 " | " (apply max (map :in-degree rs)) " |")))
         "\n\n"
         "Proposed token: `:meta/downstream-unblocking-count`, type `:non-negative-integer`, value `have→want-out-degree`, with the graph source pin carried alongside it. The proposed centrality reading square emits it; `minimise-g-over-filled-meta-policies` consumes it through the existing generative-model outcome `:downstream-unblocking`. This names the measured value only; it does not recommend a scoring transform.\n\n"
         "## Correlations\n\n"
         "Spearman's rho uses average ranks for ties over all 107 persisted items. Occurrence count is oriented as a count (larger means more occurrences), not as the selector's inverse cost.\n\n"
         "| comparison with live occurrence count | rho |\n|---|---:|\n"
         "| have→want out-degree | " (fmt-rho (:out correlations)) " |\n"
         "| have→want in-degree | " (if (some? (:in correlations))
                                        (fmt-rho (:in correlations))
                                        "typed absence (`:undefined-zero-variance`; all 107 values are 0)") " |\n"
         "| pattern-shared count | typed absence (`:no-task-to-pattern-relation`) |\n\n"
         "## Per-item values\n\n"
         "| id | kind | live occurrence count | have→want out-degree | have→want in-degree | pattern-shared count |\n"
         "|---|---|---:|---:|---:|---|\n"
         (str/join "\n" (for [{:keys [id kind occurrence out-degree in-degree]} rows]
                            (str "| `" id "` | `" kind "` | " (cell occurrence "R") " | "
                                 (cell out-degree "G") " | " (cell in-degree "G") " | "
                                 (cell nil "P") " |")))
         "\n\n## What the numbers say\n\n"
         "All 107 persisted rows have an occurrence count. Today's direct have→want graph has out-degree values from 0 to " max-out
         " and in-degree values from 0 to " max-in ". Their Spearman correlations with the persisted occurrence count are "
         (fmt-rho (:out correlations)) " and " (fmt-rho (:in correlations))
         ", respectively. The mined pattern graph cannot contribute an item-level value because it records pattern-to-pattern relations but no task-to-pattern relation; the requested Lean-row comparison is likewise absent from the searched record.\n")))

(defn -main [& _]
  (let [run-body (slurp run-path)
        run (edn/read-string run-body)
        ranked (get-in run [:outer-task-selection :policy :meta-selection :ranking])
        _ (when-not (= 107 (count ranked))
            (throw (ex-info "persisted ranking count changed" {:count (count ranked)})))
        run-graph-pin (get-in (first ranked) [:channels :pipeline-structural-centrality-cost :source])
        _ (when-not (every? #(= run-graph-pin
                                (get-in % [:channels :pipeline-structural-centrality-cost :source])) ranked)
            (throw (ex-info "ranking rows do not share one graph pin" {})))
        graph-result (fetch-graph)
        graph (:value graph-result)
        arrows (vec (canonical-arrows graph))
        outgoing (update-vals (group-by :from-id arrows) #(set (map :to-id %)))
        incoming (update-vals (group-by :to-id arrows) #(set (map :from-id %)))
        pattern-body (slurp pattern-path)
        pattern-graph (json/parse-string pattern-body true)
        relation (pattern-relation pattern-graph)
        rows (mapv (fn [r]
                     {:id (:id r) :kind (:kind r) :occurrence (graph-occurrence r)
                      :out-degree (count (disj (get outgoing (:id r) #{}) (:id r)))
                      :in-degree (count (disj (get incoming (:id r) #{}) (:id r)))}) ranked)
        _ (when (some #(nil? (:occurrence %)) rows)
            (throw (ex-info "ranked row lacks persisted occurrence count"
                            {:ids (mapv :id (filter #(nil? (:occurrence %)) rows))})))
        occurrence (mapv :occurrence rows)
        result {:futon2-sha (str/trim (:out (shell/sh "git" "rev-parse" "HEAD")))
                :ranked ranked :run-graph-pin run-graph-pin
                :graph-pin (:pin graph-result) :graph graph
                :pattern-pin (sha-pin pattern-path pattern-body)
                :pattern-graph pattern-graph :pattern-relation relation :rows rows
                :correlations {:out (spearman occurrence (mapv :out-degree rows))
                               :in (spearman occurrence (mapv :in-degree rows))}}]
    (spit report-path (render result))
    (println report-path)))

(apply -main *command-line-args*)
