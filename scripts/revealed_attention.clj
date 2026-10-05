(ns revealed-attention
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-pipeline-selector :as pipeline])
  (:import [java.net URLEncoder]
           [java.time Instant ZoneOffset]))

(def run-path "data/wm-runs/tick-run-record-2026-10-05-c9d25d6a-f2bb-42bf-a162-2c4a000e804f.edn")
(def graph-url "http://127.0.0.1:7070/api/alpha/cascade-real/graph")
(def turn-url
  "http://127.0.0.1:7073/api/alpha/evidence?author=joe&since=2026-09-22T00%3A00%3A00Z&limit=1000")
(def turn-code-path "/home/joe/code/futon3c/src/futon3c/xiang/turn_record.clj")
(def local-turn-path "/home/joe/code/storage/operator-turns/operator-turns.jsonl")
(def report-path "holes/labs/wm-contract/REPORT-revealed-attention-2026-10-05.md")
(def window-start (Instant/parse "2026-09-22T00:00:00Z"))
(def window-end (Instant/parse "2026-10-06T00:00:00Z"))
(def mission-fields [:clocked-target :clocked-mission :mission-id :mission
                     :excursion-id :ticket-id])
(def id-pattern
  #"(?i)(?:[a-z0-9_-]+/(?:mission|excursion|ticket)/[a-z0-9][a-z0-9._-]*|[MET]-[a-z0-9][a-z0-9._-]*)")

(defn sha256 [s] (field/sha256 (.getBytes ^String s "UTF-8")))
(defn canonical-id [x]
  (when (string? x)
    (let [x (str/replace x #"[.]+$" "")]
      (if (str/includes? x "/") (pipeline/canonical-work-id x) x))))
(defn fetch-json []
  (let [r (http/get graph-url {:timeout 180000 :throw false}) body (str (:body r))]
    (when-not (= 200 (:status r)) (throw (ex-info "Cascade Live refused" {:status (:status r)})))
    {:body body :pin {:path graph-url :sha256 (sha256 body)} :value (json/parse-string body true)}))
(defn encode [s] (URLEncoder/encode (str s) "UTF-8"))

(defn fetch-operator-turns []
  (loop [cursor nil bodies [] entries [] page-count 0]
    (let [url (cond-> turn-url cursor (str "&before=" (encode cursor)))
          r (http/get url {:timeout 180000 :throw false}) body (str (:body r))]
      (when-not (= 200 (:status r))
        (throw (ex-info "operator-turn evidence API refused" {:status (:status r) :url url})))
      (let [page (:entries (edn/read-string body))
            ats (keep :evidence/at page)
            oldest (first (sort ats))
            recent (filter (fn [e]
                             (let [at (:evidence/at e)]
                               (and at (not (.isBefore (Instant/parse at) window-start))
                                    (.isBefore (Instant/parse at) window-end)))) page)
            entries* (into entries recent)
            bodies* (conj bodies body)]
        (if (or (empty? page) (nil? oldest)
                (.isBefore (Instant/parse oldest) window-start) (= oldest cursor))
          {:entries (vec (distinct entries*)) :page-count (inc page-count)
           :pin {:path (str turn-url "&before=<pagination>")
                 :sha256 (sha256 (str/join "\n" bodies*))}}
          (recur oldest bodies* entries* (inc page-count)))))))

(defn operator-turn [entry]
  (let [body (if (map? (:evidence/body entry)) (:evidence/body entry) {})
        operator? (or (= "joe" (:evidence/author entry))
                      (= :operator (get-in entry [:evidence/origin :kind])))
        chat? (= "chat-turn" (:event body))
        mission-values (keep #(get body %) mission-fields)
        mission (some canonical-id mission-values)]
    (when (and operator? chat?)
      {:id (:evidence/id entry) :at (:evidence/at entry)
       :session (:evidence/session-id entry) :mission mission
       :mission-fields (select-keys body mission-fields)
       :text (or (:text body) (:unified-text body) "")})))

(defn pairs [xs]
  (let [v (vec (sort (set xs)))]
    (for [i (range (count v)) j (range (inc i) (count v))] [(v i) (v j)])))
(defn add-rule [m pair rule] (update m pair (fnil conj #{}) rule))
(defn day [epoch-ms]
  (str (.toLocalDate (.atZone (Instant/ofEpochMilli (long epoch-ms)) ZoneOffset/UTC))))
(defn mentions [text universe]
  (->> (re-seq id-pattern text) (keep canonical-id) (filter universe) set))

(defn co-work [lineage turns]
  (let [targets (set (map :target-id lineage))
        by-session (vals (group-by :session lineage))
        by-agent-day (vals (group-by (juxt :agent #(day (:at %))) lineage))
        from-session (reduce (fn [m rs]
                               (reduce #(add-rule %1 %2 :same-session) m
                                       (pairs (map :target-id rs)))) {} by-session)
        from-agent (reduce (fn [m rs]
                             (reduce #(add-rule %1 %2 :same-agent-utc-day) m
                                     (pairs (map :target-id rs)))) from-session by-agent-day)]
    (reduce (fn [m turn]
              (reduce #(add-rule %1 %2 :operator-turn-co-mention) m
                      (pairs (mentions (:text turn) targets))))
            from-agent turns)))

(defn applied-relation [graph]
  (let [pairs (keep (fn [{:keys [mission pattern relation]}]
                      (when (= "applied" relation)
                        (when-let [id (canonical-id mission)] [id pattern])))
                    (get-in graph [:patterns :edges]))
        mp (reduce (fn [m [id p]] (update m id (fnil conj #{}) p)) {} pairs)
        pm (reduce (fn [m [id p]] (update m p (fnil conj #{}) id)) {} pairs)]
    {:mission-patterns mp :pattern-missions pm}))
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
(defn instant [epoch-ms] (when epoch-ms (str (Instant/ofEpochMilli (long epoch-ms)))))

(defn render [{:keys [head run-pin graph-pin turn-pin turn-pages turn-code-pin local-turn-pin
                      rows lineage lineage-reference turns co-work rhos]}]
  (let [diagram "M-diagramprover"
        neighbours (for [[[a b] rules] (sort-by key co-work)
                         :when (or (= diagram a) (= diagram b))]
                     [(if (= diagram a) b a) rules])
        mission-field-freq (frequencies (mapcat (comp keys :mission-fields) turns))]
    (str "# Revealed attention and co-work — 2026-10-05\n\n"
         "Reproduce from `/home/joe/code/futon2` in a fresh process:\n\n```sh\nclojure -M scripts/revealed_attention.clj\n```\n\n"
         "futon2 HEAD before generation: `" head "`. Ranked population: `" run-path "` / `" (:sha256 run-pin) "`.\n\n"
         "## Sources\n\n"
         "**(i) Dispatch lineage.** Cascade Live `:lineage`, `" (:path graph-pin) "` / `" (:sha256 graph-pin) "`, contains " (count lineage) " records over " (count (set (map :target-id lineage))) " canonical targets. Counts of absent ranked items are zero with this pin.\n\n"
         "Reference counts at this read are `" (pr-str lineage-reference) "` (the earlier hand reference was `{:diagramprover 25 :apm-demonstration 22 :the-perfect-crime 14 :E-aif-cascade 1}`).\n\n"
         "**(ii) Operator turns.** `turn-traced` is backed by operator chat-turn evidence shaped by `" (:path turn-code-pin) "` / `" (:sha256 turn-code-pin) "` and served by futon1b's `/api/alpha/evidence` endpoint. This run paged " turn-pages " response(s), aggregate pin `" (:path turn-pin) "` / `" (:sha256 turn-pin) "`, for the UTC calendar window `[2026-09-22T00:00:00Z, 2026-10-06T00:00:00Z)`. It found " (count turns) " operator chat turns; " (count (filter :mission turns)) " carry a mission-like field. Fields inspected, in precedence order, were `" (pr-str mission-fields) "`; populated-field frequencies were `" (pr-str mission-field-freq) "`. The older local export `" (:path local-turn-pin) "` / `" (:sha256 local-turn-pin) "` was inspected as the harvester output but not counted because it ends before the reporting date. An item with no matching turn receives zero because the mission relation exists.\n\n"
         "**(iii) Co-work.** The undirected graph spans all " (count (set (map :target-id lineage))) " lineage targets and has " (count co-work) " edges. An edge records one or more of `:same-session`, `:same-agent-utc-day`, or `:operator-turn-co-mention`; its degree is an enabler association, not dependency evidence.\n\n"
         "## Per-item values\n\n| id | kind | dispatches | sessions | dispatchers | first dispatch | last dispatch | operator turns 14d | co-work degree |\n|---|---|---:|---:|---:|---|---|---:|---:|\n"
         (str/join "\n" (for [{:keys [id kind dispatches sessions dispatchers first-at last-at operator-turns co-degree]} rows]
                            (str "| `" id "` | `" kind "` | " dispatches " [G] | " sessions " [G] | " dispatchers " [G] | "
                                 (or (instant first-at) "—") " [G] | " (or (instant last-at) "—") " [G] | " operator-turns " [O] | " co-degree " [C] |")))
         "\n\n## `M-diagramprover` co-work example\n\n"
         (if (seq neighbours)
           (str/join "\n" (for [[id rules] neighbours]
                              (str "- `" id "`: `" (pr-str rules) "`.")))
           "`:typed-absence` (`:no-co-work-neighbours`).")
         "\n\n## Correlations\n\nSpearman rho uses average ranks for ties over all 107 persisted items.\n\n| comparison with dispatch count | rho |\n|---|---:|\n| persisted occurrence count | " (fmt (:occurrence rhos)) " |\n| B1 (b), shared-pattern missions | " (fmt (:shared rhos)) " |\n\n"
         "## Proposal\n\nAs a first-cut `unblocks` token, the observation square could emit dispatch attention (raw dispatch count, sessions, dispatchers, recency, and co-work degree) as an **enabler weight**: repeated or joint work is evidence that an item is useful alongside other work. It is explicitly not a dependency and must not assert that completing one item makes another possible. Where a reviewed B1b-style `Unblocks:` declaration exists, that declaration supersedes the inferred attention value for the directed relation.\n")))

(defn -main [& _]
  (let [run-body (slurp run-path) run (edn/read-string run-body)
        ranked (get-in run [:outer-task-selection :policy :meta-selection :ranking])
        fetched (fetch-json) graph (:value fetched)
        lineage (mapv #(assoc % :target-id (canonical-id (:target %))) (:lineage graph))
        _ (when (some #(nil? (:target-id %)) lineage) (throw (ex-info "lineage target did not canonicalise" {})))
        operator-source (fetch-operator-turns)
        turns (vec (keep operator-turn (:entries operator-source)))
        co (co-work lineage turns)
        adjacency (reduce (fn [m [[a b] _]] (-> m (update a (fnil conj #{}) b) (update b (fnil conj #{}) a))) {} co)
        by-target (group-by :target-id lineage) turn-counts (frequencies (keep :mission turns))
        applied (applied-relation graph)
        rows (mapv (fn [r]
                     (let [id (:id r) ls (get by-target id []) ats (map :at ls)]
                       {:id id :kind (:kind r) :dispatches (count ls)
                        :sessions (count (set (keep :session ls)))
                        :dispatchers (count (set (keep :dispatched-by ls)))
                        :first-at (when (seq ats) (apply min ats)) :last-at (when (seq ats) (apply max ats))
                        :operator-turns (get turn-counts id 0) :co-degree (count (get adjacency id #{}))
                        :occurrence (get-in r [:channels :pipeline-structural-centrality-cost :observation :value])
                        :shared (shared-count applied id)})) ranked)
        lineage-counts (frequencies (map :target-id lineage))
        lineage-reference (select-keys lineage-counts
                                       ["M-diagramprover" "M-apm-demonstration"
                                        "M-the-perfect-crime" "E-aif-cascade"])
        counts (mapv :dispatches rows)
        turn-code (slurp turn-code-path) local-turn (slurp local-turn-path)
        result {:head (str/trim (:out (shell/sh "git" "rev-parse" "HEAD")))
                :run-pin {:path run-path :sha256 (sha256 run-body)} :graph-pin (:pin fetched)
                :turn-pin (:pin operator-source) :turn-pages (:page-count operator-source)
                :turn-code-pin {:path turn-code-path :sha256 (sha256 turn-code)}
                :local-turn-pin {:path local-turn-path :sha256 (sha256 local-turn)}
                :rows rows :lineage lineage :lineage-reference lineage-reference
                :turns turns :co-work co
                :rhos {:occurrence (spearman counts (mapv :occurrence rows))
                       :shared (spearman counts (mapv :shared rows))}}]
    (spit report-path (render result))
    (println report-path)))

(apply -main *command-line-args*)
