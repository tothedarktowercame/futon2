(ns operator-load
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-pipeline-selector :as pipeline]))

(def rule
  {:window 10
   :machine-trailer-keys ["Agent-Id" "Agent-Session" "Agency-Job"]
   :markers ["HIT" "Joe decides" "Joe to decide" "operator ruling"
             "needs Joe" "🈸" "ask Joe"]})
(def run-path
  "data/wm-runs/tick-run-record-2026-10-05-c9d25d6a-f2bb-42bf-a162-2c4a000e804f.edn")
(def graph-url "http://127.0.0.1:7070/api/alpha/cascade-real/graph")
(def report-path "holes/labs/wm-contract/REPORT-operator-load-2026-10-05.md")

(defn sha256 [s] (field/sha256 (.getBytes ^String s "UTF-8")))
(defn git [& args]
  (let [{:keys [exit out err]} (apply shell/sh "git" args)]
    (if (zero? exit) out
        (throw (ex-info "git command failed" {:args args :exit exit :stderr err})))))
(defn repo-root [path]
  (loop [f (.getParentFile (.getCanonicalFile (io/file path)))]
    (when f
      (if (.exists (io/file f ".git")) (.getCanonicalPath f) (recur (.getParentFile f))))))

(def log-format "%x1e%H%x1f%aI%x1f%an%x1f%ae%x1f%(trailers)")
(defn commits [repo rel]
  (let [out (git "-C" repo "log" "-n" (str (:window rule))
                 (str "--format=" log-format) "--" rel)]
    (mapv (fn [record]
            (let [[sha date author email trailers] (str/split record #"\u001f" 5)]
              {:sha sha :date date :author author :email email :trailers trailers
               :machine? (boolean
                          (some #(re-find (re-pattern (str "(?m)^" (java.util.regex.Pattern/quote %) ":"))
                                          trailers)
                                (:machine-trailer-keys rule)))}))
          (remove str/blank? (rest (str/split out #"\u001e"))))))

(defn marker-facts [text]
  (let [matches (mapv (fn [line] (filterv #(str/includes? line %) (:markers rule)))
                      (str/split-lines text))]
    {:marker-count (count (filter seq matches))
     :matched-markers (into (sorted-set) cat matches)}))

(defn touch-facts [source]
  (let [path (:path source) file (when path (io/file path))]
    (cond
      (not (string? path)) {:status :typed-absence :reason :source-path-absent}
      (not (.isFile file)) {:status :typed-absence :reason :document-missing :path path}
      :else
      (if-let [repo (repo-root path)]
        (let [rel (str (.relativize (.toPath (io/file repo)) (.toPath (.getCanonicalFile file))))
              history (commits repo rel)]
          (if (empty? history)
            {:status :typed-absence :reason :no-git-history :path path :repo repo :relative-path rel}
            (let [operator (remove :machine? history)
                  text (slurp file)]
              (merge {:status :measured :path path :repo repo :relative-path rel
                      :n (count history) :operator-touches (count operator)
                      :share (/ (count operator) (double (count history)))
                      :last-operator-touch (:date (first operator))}
                     (marker-facts text)))))
        {:status :typed-absence :reason :repository-not-found :path path}))))

(defn fetch-pattern-relation []
  (let [response (http/get graph-url {:timeout 180000 :throw false}) body (str (:body response))]
    (when-not (= 200 (:status response))
      (throw (ex-info "Cascade Live graph refused" {:status (:status response)})))
    (let [graph (json/parse-string body true)
          applied (filter #(= "applied" (:relation %)) (get-in graph [:patterns :edges]))
          pairs (keep (fn [{:keys [mission pattern]}]
                        (when-let [id (pipeline/canonical-work-id mission)] [id pattern])) applied)
          mission-patterns (reduce (fn [m [id p]] (update m id (fnil conj #{}) p)) {} pairs)
          pattern-missions (reduce (fn [m [id p]] (update m p (fnil conj #{}) id)) {} pairs)]
      {:pin {:path graph-url :sha256 (sha256 body)}
       :edge-count (count (get-in graph [:patterns :edges]))
       :applied-count (count applied)
       :mission-count (count mission-patterns)
       :shared (fn [id]
                 (let [patterns (get mission-patterns id #{})]
                   (count (disj (reduce set/union #{} (map pattern-missions patterns)) id))))})))

(defn ranks [xs]
  (let [ordered (sort-by (juxt second first) (map-indexed vector xs))]
    (reduce (fn [out group]
              (let [positions (map #(.indexOf ^java.util.List ordered %) group)
                    average (/ (+ 2 (apply min positions) (apply max positions)) 2.0)]
                (reduce (fn [v [i _]] (assoc v i average)) out group)))
            (vec (repeat (count xs) 0.0)) (partition-by second ordered))))
(defn spearman [xs ys]
  (let [xs (ranks xs) ys (ranks ys) n (count xs)
        mx (/ (reduce + xs) n) my (/ (reduce + ys) n)
        dx (map #(- % mx) xs) dy (map #(- % my) ys)
        denominator (Math/sqrt (* (reduce + (map #(* % %) dx))
                                  (reduce + (map #(* % %) dy))))]
    (when (pos? denominator) (/ (reduce + (map * dx dy)) denominator))))
(defn fmt [x] (if (number? x) (format "%.6f" (double x)) "—"))
(defn mean [xs] (when (seq xs) (/ (reduce + xs) (double (count xs)))))
(defn md [x] (str/replace (str x) "|" "\\|"))

(defn render [{:keys [head run-pin graph-pin graph-facts rows rhos]}]
  (let [measured (filter #(= :measured (:status %)) rows)
        absent (remove #(= :measured (:status %)) rows)
        by-kind (sort-by key (group-by :kind rows))]
    (str "# Operator-turn load for the persisted 107-item ranking — 2026-10-05\n\n"
         "Reproduce in a fresh process from `/home/joe/code/futon2`:\n\n"
         "```sh\nclojure -M scripts/operator_load.clj\n```\n\n"
         "futon2 HEAD before generation: `" head "`. Run-record pin: `" run-path "` / `" (:sha256 run-pin) "`. B1 pattern-edge graph pin: `" (:path graph-pin) "` / `" (:sha256 graph-pin) "`; it contains " (:edge-count graph-facts) " pattern edges, " (:applied-count graph-facts) " applied edges, over " (:mission-count graph-facts) " canonical missions.\n\n"
         "## Rule\n\n```edn\n" (pr-str rule) "\n```\n\n"
         "For each source document, the script runs the equivalent of `git log -n 10 --format='%H %an %ae%n%(trailers)' -- <path>` in its owning repository, adding an ISO author date and record separators solely for parsing. A commit carrying any configured trailer is machine-authored; one carrying none is an operator touch. Touch share is `operator-touches / N-found`. Text-marker count is the number of HEAD document lines containing at least one configured marker; it is a separate classical test, not a verdict.\n\n"
         "## Distribution by kind\n\n"
         "| kind | items | measured | typed absence | mean N found | mean operator touches | mean touch share | items with markers | marker lines |\n"
         "|---|---:|---:|---:|---:|---:|---:|---:|---:|\n"
         (str/join "\n"
                   (for [[kind rs] by-kind
                         :let [ms (filter #(= :measured (:status %)) rs)]]
                     (str "| `" kind "` | " (count rs) " | " (count ms) " | " (- (count rs) (count ms))
                          " | " (fmt (mean (map :n ms))) " | " (fmt (mean (map :operator-touches ms)))
                          " | " (fmt (mean (map :share ms))) " | " (count (filter #(pos? (:marker-count %)) ms))
                          " | " (reduce + 0 (map :marker-count ms)) " |")))
         "\n\n## Correlations\n\nSpearman rho uses average ranks for ties and only the " (count measured)
         " rows with measured touch shares. Occurrence counts are the persisted selector observations; B1 (b) is recomputed from the pinned [G] applied-pattern relation as the number of other missions sharing at least one pattern.\n\n"
         "| comparison with touch share | rho | rows |\n|---|---:|---:|\n"
         "| persisted occurrence count | " (fmt (:occurrence rhos)) " | " (count measured) " |\n"
         "| B1 (b), shared-pattern missions | " (fmt (:shared rhos)) " | " (count measured) " |\n\n"
         "## Proposed token\n\n`{:name :meta/operator-load-share :type :ratio :range [0 1] :raw [:operator-touches :n-found]}` is emitted by the proposed operator-load reading square. As a support change, it could be consumed beside `injury` to admit or exclude a candidate before scoring. As a preference term, it could be consumed by `minimise-g-over-filled-meta-policies` as predicted operator demand. B3 leaves that choice to Joe.\n\n"
         "## Per-item values\n\n"
         "| id | kind | source document / SHA-256 | N found | operator touches | share | last operator touch | `:last-touch` state | marker lines | matched markers | B1 (b) |\n"
         "|---|---|---|---:|---:|---:|---|---|---:|---|---:|\n"
         (str/join "\n"
                   (for [{:keys [id kind status n operator-touches share last-operator-touch
                                  last-touch-state marker-count matched-markers shared reason source]} rows
                         :let [source-cell (str "`" (:path source) "` / `" (:sha256 source) "`")]]
                     (if (= :measured status)
                       (str "| `" id "` | `" kind "` | " source-cell " | " n " | " operator-touches " | " (fmt share)
                            " | " (or last-operator-touch "—") " | `" last-touch-state "` | " marker-count
                            " | " (if (seq matched-markers) (md (pr-str matched-markers)) "—") " | " shared " |")
                       (str "| `" id "` | `" kind "` | " source-cell " | `:typed-absence` (`" reason "`) | — | — | — | `"
                            last-touch-state "` | — | — | " shared " |"))))
         "\n\n## Typed absences\n\n"
         (if (seq absent)
           (str/join "\n" (map #(str "- `" (:id %) "`: `" (:reason %) "`; source `" (get-in % [:source :path]) "`.") absent))
           "None.")
         "\n\n## What the numbers say\n\n"
         (count measured) " of 107 documents have a measurable Git window and " (count absent) " have typed absences. The mean measured touch share is "
         (fmt (mean (map :share measured))) "; " (count (filter #(pos? (:marker-count %)) measured))
         " documents contain at least one configured text marker. Touch share has Spearman rho " (fmt (:occurrence rhos))
         " with persisted occurrence count and " (fmt (:shared rhos)) " with B1 (b). These are two separate observations—commit provenance and literal document markers—not a feasibility verdict.\n")))

(defn -main [& _]
  (let [run-body (slurp run-path) run (edn/read-string run-body)
        ranked (get-in run [:outer-task-selection :policy :meta-selection :ranking])
        support (into {} (map (juxt :id identity)) (get-in run [:outer-task-selection :support]))
        _ (when-not (= 107 (count ranked)) (throw (ex-info "persisted ranking count changed" {:count (count ranked)})))
        graph (fetch-pattern-relation)
        _ (when-not (and (pos? (:applied-count graph)) (pos? (:mission-count graph)))
            (throw (ex-info "B1 applied-pattern relation absent" (dissoc graph :shared))))
        rows (mapv (fn [ranked-row]
                     (let [item (get support (:id ranked-row))
                           base {:id (:id ranked-row) :kind (:kind ranked-row) :source (:source item)
                                 :occurrence (get-in ranked-row [:channels :pipeline-structural-centrality-cost :observation :value])
                                 :last-touch-state (get-in item [:last-touch :state])
                                 :shared ((:shared graph) (:id ranked-row))}]
                       (merge base (touch-facts (:source item))))) ranked)
        measured (filterv #(= :measured (:status %)) rows)
        shares (mapv :share measured)
        result {:head (str/trim (git "rev-parse" "HEAD"))
                :run-pin {:path run-path :sha256 (sha256 run-body)} :graph-pin (:pin graph)
                :graph-facts (dissoc graph :pin :shared) :rows rows
                :rhos {:occurrence (spearman shares (mapv :occurrence measured))
                       :shared (spearman shares (mapv :shared measured))}}]
    (spit report-path (render result))
    (println report-path)))

(apply -main *command-line-args*)
