(ns cheat-sheets
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
(def report-path "holes/labs/wm-contract/REPORT-cheat-sheets-2026-10-06.md")
(def markers ["HIT" "Joe decides" "Joe to decide" "operator ruling" "needs Joe" "🈸" "ask Joe"])
(def stopwords
  (set (str/split
        "a an and are as at be been but by can could did do does for from had has have he her here him his how i if in into is it its may more most not of on one only or our out over same she should so some such than that the their them then there these they this those through to too under up use used using very was we were what when where which who why will with would you your also all any each no yes true false nil md markdown section sections status open closed mission missions item items table report note notes line lines code file files see cf" #"\s+")))
(def rule
  {:scope {:tokens :path-like-or-futon-repo-name :top 3}
   :keywords {:count 6 :weight :raw-term-frequency-times-ln-document-count-over-document-frequency
              :strip [:fenced-code :inline-code :markdown-syntax]
              :stoplist {:source :embedded-common-english-plus-markdown-project-words}}
   :register {:numerator :nonblank-lines-containing-any-of
              :signals [:path :sha-7-plus :number :checkbox :iso-date]
              :denominator :nonblank-lines
              :sections :level-2-headings
              :section-trajectory {:form :line-weighted-last-ceiling-third-minus-first-ceiling-third
                                   :minimum-section-lines 15}
              :history-trajectory :added-line-register-last-3-commits-minus-first-3-commits}})

(defn sha256 [s] (field/sha256 (.getBytes ^String s "UTF-8")))
(defn sh! [& args]
  (let [{:keys [exit out err]} (apply shell/sh args)]
    (when-not (zero? exit) (throw (ex-info "command refused" {:args args :exit exit :err err}))) out))
(defn fmt [x] (format "%.3f" (double (or x 0))))
(defn fmt6 [x] (format "%.6f" (double (or x 0))))
(defn md [x] (-> (str x) (str/replace "|" "\\|") (str/replace #"\s+" " ") str/trim))
(defn ranks [xs]
  (reduce (fn [v g]
            (let [positions (map first g) avg (/ (+ 2 (apply min positions) (apply max positions)) 2.0)]
              (reduce #(assoc %1 (nth %2 2) avg) v g)))
          (vec (repeat (count xs) 0.0))
          (partition-by second
                        (map-indexed (fn [rank [_ value original]] [rank value original])
                                     (sort-by second (map-indexed (fn [i x] [nil x i]) xs))))))
(defn rho [xs ys]
  (let [x (ranks xs) y (ranks ys) n (count x) mx (/ (reduce + x) n) my (/ (reduce + y) n)
        dx (map #(- % mx) x) dy (map #(- % my) y)
        den (Math/sqrt (* (reduce + (map #(* % %) dx)) (reduce + (map #(* % %) dy))))]
    (when (pos? den) (/ (reduce + (map * dx dy)) den))))

(def path-re #"(?i)(?:/home/joe/code/|~/code/)?(?:futon\d[a-z]?|mathlib4|p4ng|storage)(?:/[A-Za-z0-9._@+-]+)+|(?:[A-Za-z0-9._-]+/){1,}[A-Za-z0-9._@+-]+|\bfuton\d[a-z]?\b")
(defn repo-of-path [path] (second (re-find #"/home/joe/code/([^/]+)" path)))
(defn scope-key [token _local-repo]
  (let [s (str/replace token #"^[`'(\[]|[`'),.;:\]]$" "")
        [_ explicit tail] (re-find #"(?i)(?:/home/joe/code/|~/code/)?(futon\d[a-z]?|mathlib4|p4ng|storage)(?:/([^/\s]+))?" s)]
    (if explicit
      (if tail (str explicit "/" tail) explicit)
      (str "relative/" (first (str/split s #"/"))))))
(defn scopes [text path]
  (let [repo (repo-of-path path) ks (keep #(scope-key % repo) (re-seq path-re text))]
    {:counts (frequencies ks)
     :top (take 3 (sort-by (juxt (comp - val) key) (frequencies ks)))}))
(defn legacy-scope-key [token local-repo]
  (let [s (str/replace token #"^[`'(\[]|[`'),.;:\]]$" "")
        [_ explicit tail] (re-find #"(?i)(?:/home/joe/code/|~/code/)?(futon\d[a-z]?|mathlib4|p4ng|storage)(?:/([^/\s]+))?" s)
        repo (or explicit local-repo)
        top (or tail (when (and repo (not explicit)) (first (str/split s #"/"))))]
    (if (and repo top (not= repo top)) (str repo "/" top) repo)))
(defn legacy-scopes [text path]
  (let [repo (repo-of-path path) ks (keep #(legacy-scope-key % repo) (re-seq path-re text))]
    {:top (take 3 (sort-by (juxt (comp - val) key) (frequencies ks)))}))

(defn clean-text [s]
  (-> s (str/replace #"(?s)```.*?```" " ") (str/replace #"(?s)~~~.*?~~~" " ")
      (str/replace #"`[^`]*`" " ") (str/replace #"https?://\S+" " ")
      (str/replace #"[#>*_~|\[\]()]" " ") str/lower-case))
(defn terms [s]
  (->> (re-seq #"[a-z][a-z0-9-]{2,}" (clean-text s)) (remove stopwords) vec))
(defn declared-keywords [text]
  (->> (str/split-lines text)
       (keep #(second (re-find #"(?i)^\s*(?:@keywords|\*\*keywords\*\*)\s*:?\s*(.+)$" %)))
       (mapcat #(str/split % #"\s*[,;]\s*")) (map str/trim) (remove empty?) distinct vec))
(defn keyword-map [docs]
  (let [n (count docs) dfs (frequencies (mapcat (comp set terms :text) docs))]
    (into {} (for [{:keys [id text]} docs]
               (let [tf (frequencies (terms text))
                     scored (sort-by (juxt (comp - second) first)
                                     (for [[term cnt] tf] [term (* cnt (Math/log (/ n (double (dfs term)))))]))]
                 [id (mapv first (take 6 scored))])))))

(defn concrete? [line]
  (boolean (or (re-find path-re line) (re-find #"(?i)\b[0-9a-f]{7,64}\b" line)
               (re-find #"\d" line) (re-find #"- \[[ xX]\]" line)
               (re-find #"\b\d{4}-\d{2}-\d{2}\b" line))))
(defn line-register [lines]
  (let [ls (remove str/blank? lines) n (count ls) c (count (filter concrete? ls))]
    {:lines n :concrete c :value (if (pos? n) (/ c (double n)) 0.0)}))
(defn sections [text]
  (reduce (fn [out line]
            (if-let [[_ title] (re-find #"^##\s+(.+)$" line)]
              (conj out {:title title :lines []})
              (update-in out [(dec (count out)) :lines] conj line)))
          [{:title "Preamble" :lines []}] (str/split-lines text)))
(defn mean [xs] (if (seq xs) (/ (reduce + xs) (double (count xs))) 0.0))
(def minimum-trajectory-section-lines 15)
(defn weighted-register [ss]
  (let [lines (reduce + (map :lines ss)) concrete (reduce + (map :concrete ss))]
    (if (pos? lines) (/ concrete (double lines)) 0.0)))
(defn section-registers [text]
  (let [ss (mapv #(merge (select-keys % [:title]) (line-register (:lines %))) (sections text))
        included (filterv #(>= (:lines %) minimum-trajectory-section-lines) ss)
        excluded (filterv #(< (:lines %) minimum-trajectory-section-lines) ss)
        k (max 1 (long (Math/ceil (/ (count ss) 3.0))))
        first-third (filterv #(>= (:lines %) minimum-trajectory-section-lines) (take k ss))
        last-third (filterv #(>= (:lines %) minimum-trajectory-section-lines) (take-last k ss))]
    {:sections ss :included included :excluded excluded
     :first-third first-third :last-third last-third
     :delta (- (weighted-register last-third) (weighted-register first-third))}))
(defn legacy-section-registers [text]
  (let [ss (mapv #(merge (select-keys % [:title]) (line-register (:lines %))) (sections text))
        k (max 1 (long (Math/ceil (/ (count ss) 3.0))))]
    {:delta (- (mean (map :value (take-last k ss)))
               (mean (map :value (take k ss))))}))
(defn history-register [path]
  (let [repo (str "/home/joe/code/" (repo-of-path path)) rel (subs path (inc (count repo)))
        raw (sh! "git" "-C" repo "log" "--follow" "--format=%x1e%H" "--patch" "--unified=0" "--" rel)
        records (->> (str/split raw #"\u001e") rest
                     (map (fn [record]
                            (let [[sha & ls] (str/split-lines record)
                                  adds (->> ls (filter #(and (str/starts-with? % "+") (not (str/starts-with? % "+++")))) (map #(subs % 1)))]
                              {:sha sha :lines adds}))) vec)
        value (fn [rs] (:value (line-register (mapcat :lines rs))))
        newest (take 3 records) oldest (take-last 3 records)]
    {:commits (count records) :newest (value newest) :oldest (value oldest)
     :delta (- (value newest) (value oldest))}))
(defn arrow [x] (cond (> x 1.0e-9) "↑" (< x -1.0e-9) "↓" :else "→"))

(defn fetch-graph []
  (let [r (http/get graph-url {:timeout 180000 :throw false}) body (str (:body r))]
    (when-not (= 200 (:status r)) (throw (ex-info "graph refused" {:status (:status r)})))
    {:body body :graph (json/parse-string body true)}))
(defn applied [graph]
  (reduce (fn [m {:keys [mission pattern relation]}]
            (if (and (= "applied" relation) (pipeline/canonical-work-id mission))
              (update m (pipeline/canonical-work-id mission) (fnil conj #{}) pattern) m))
          {} (get-in graph [:patterns :edges])))
(defn pattern-order [patterns text]
  (let [low (str/lower-case text)
        rows (for [p patterns :let [ident (last (str/split p #"/"))
                                    a (.indexOf low (str/lower-case p)) b (.indexOf low (str/lower-case ident))
                                    at (cond (<= 0 a) a (<= 0 b) b :else nil)]] {:pattern p :at at})]
    {:mentioned (mapv :pattern (sort-by (juxt :at :pattern) (filter :at rows)))
     :unmentioned (mapv :pattern (sort-by :pattern (remove :at rows)))}))
(defn status [text]
  (let [s (some #(second (re-find #"(?i)^\s*(?:[-*]\s+)?\*\*Status:?\*\*\s*:?[ \t]*(.+)$" %)) (str/split-lines text))
        life (some #(second (re-find #"(?i)^\s*(?:[-*]\s+)?(?:\*\*)?Lifecycle(?: phase)?(?:\*\*)?\s*:[ \t]*(.+)$" %)) (str/split-lines text))
        phases (->> (re-seq #"(?i)\b(HEAD|IDENTIFY|MAP|DERIVE|ARGUE|VERIFY|INSTANTIATE|DOCUMENT)\b" (str s " " life))
                    (map (comp str/upper-case second)) distinct vec)]
    {:status (or s ":typed-absence (:no-status-line)") :phases phases}))
(defn marker-count [text] (count (filter (fn [line] (some #(str/includes? line %) markers)) (str/split-lines text))))
(defn shared-count [app id]
  (let [ps (get app id #{})] (count (disj (set (for [[other ops] app :when (seq (set/intersection ps ops))] other)) id))))
(defn changes-text [{:keys [mentioned unmentioned]}]
  (str (if (seq mentioned) (str/join " → " mentioned) "—")
       (when (seq unmentioned) (str " · " (str/join ", " unmentioned)))))
(defn scopes-text [top] (if (seq top) (str/join " · " (map (fn [[k n]] (str k " ×" n)) top)) "—"))
(defn check-scope! []
  (let [path "/home/joe/code/futon3c/holes/missions/M-the-perfect-crime.md"
        actual (scopes-text (get-in (scopes (slurp path) path) [:top]))
        expected "relative/code ×23 · futon1b ×15 · futon0/analysis ×9"]
    (when-not (= expected actual)
      (throw (ex-info "real-document scope check failed" {:expected expected :actual actual})))
    (when (str/includes? actual "futon3c/code")
      (throw (ex-info "bare scope was attributed to the document repository" {:actual actual})))
    (println "GREEN scope:" actual)))
(defn check-trajectory! []
  (let [path "/home/joe/code/futon3/holes/missions/M-weird-modernism.md"
        result (section-registers (slurp path))
        checklist (first (filter #(= "Acceptance checklist (2026-09-30)" (:title %))
                                 (:excluded result)))
        log (first (filter #(= "Mission log" (:title %)) (:last-third result)))]
    (when-not (= "0.034961" (format "%.6f" (:delta result)))
      (throw (ex-info "real-document trajectory delta check failed"
                      {:expected 0.034961 :actual (:delta result)})))
    (when-not (= 5 (:lines checklist))
      (throw (ex-info "short acceptance checklist was not excluded"
                      {:excluded (:excluded result)})))
    (when-not (and (= 120 (:lines log)) (= "0.367" (format "%.3f" (:value log))))
      (throw (ex-info "remaining last-third contributor changed" {:mission-log log})))
    (println "GREEN trajectory: delta=0.034961; excluded=Acceptance checklist (2026-09-30) [5 lines]; remaining=Mission log [120 lines, register 0.367]")))
(defn sheet-line [r]
  (str "| `" (:id r) "` | scope " (scopes-text (get-in r [:scopes :top]))
       " | keys " (str/join ", " (concat (:keywords r) (map #(str "declared:" %) (:declared r))))
       " | changes " (changes-text (:patterns r))
       " | register " (fmt (:register r)) " " (arrow (:section-delta r)) "section " (arrow (:history-delta r)) "history"
       (when (:disagree r) " ⚠ disagreement") " | " (md (:status r))
       (when (seq (:phases r)) (str " · phase " (str/join "/" (:phases r)))) " |"))
(defn quantile [xs p] (nth (vec (sort xs)) (long (Math/floor (* p (dec (count xs)))))))

(defn render [{:keys [head graph-pin run-pin rows rhos changes]}]
  (let [examples (map #(first (filter (fn [r] (= % (:id r))) rows)) ["M-weird-modernism" "M-the-perfect-crime"])
        by-kind (sort-by key (group-by :kind rows))]
    (str "# Deterministic cheat sheets for the persisted 107-item ranking — 2026-10-06\n\n"
         "Reproduce from `/home/joe/code/futon2` in a fresh process:\n\n```sh\nclojure -M scripts/cheat_sheets.clj\n```\n\n"
         "futon2 HEAD before generation: `" head "`. Run record pin: `" (:sha256 run-pin) "`. Cascade Live applied-pattern relation: `" graph-url "` / `" graph-pin "` (sha256 of sorted `[:patterns :edges]`; volatile lineage is outside this input).\n\n"
         "## Rule\n\n```clojure\n" (pr-str rule) "\n```\n\nThe stoplist is an embedded, version-controlled list of common English function words plus Markdown/project-format words; the complete list is in the script. TF is raw count and IDF is `ln(107 / document-frequency)`. Explicit repository tokens are grouped as `repo/top-dir`; bare tokens retain their first segment as `relative/<segment>` and are never assigned to the document's repository. Sections are divided into first and last ceiling thirds in document order; sections with fewer than **15 nonblank lines** are then excluded from those trajectory groups, and the retained registers are weighted by nonblank-line count. A small positive value after exclusion is a measurement, not a flag; a materiality threshold is deferred to E-aif-cascade R-list tuning. All calculations use whole HEAD documents.\n\n"
         "## Worked examples (first)\n\n"
         (str/join "\n\n" (for [r examples]
                              (str "### `" (:id r) "`\n\n" (sheet-line r) "\n\n"
                                   "Trace counts: " (get-in r [:diagnostics :source-lines]) " source lines; "
                                   (get-in r [:diagnostics :path-lines]) " path-bearing lines; "
                                   (get-in r [:diagnostics :checkboxes]) " checkboxes; "
                                   (get-in r [:diagnostics :sha-tokens]) " SHA-like tokens.\n\n"
                                   "| section | nonblank lines | concrete lines | register |\n|---|---:|---:|---:|\n"
                                   (str/join "\n" (for [s (:section-registers r)]
                                                        (str "| " (md (:title s)) " | " (:lines s) " | " (:concrete s) " | " (fmt (:value s)) " |")))
                                   "\n\nExcluded from section trajectory (<15 lines): "
                                   (if (seq (:section-excluded r))
                                     (str/join ", " (map #(str "`" (:title %) "` (" (:lines %) " lines)") (:section-excluded r)))
                                     "none")
                                   ". Last-third contributors: "
                                   (str/join ", " (map #(str "`" (:title %) "` (" (:lines %) " lines, register " (fmt (:value %)) ")") (:section-last-third r)))
                                   ". Section delta: " (fmt (:section-delta r)) "; history added-line delta (last 3 minus first 3 of " (get-in r [:history :commits]) " commits): " (fmt (:history-delta r)) ".")))
         "\n\n## Changes from the first cut\n\n"
         "The 2026-10-05 report remains unchanged because B4 part 2 pins it by SHA. This 2026-10-06 report is a new input for a later rerun. Against the first-cut algorithms on the same 107 HEAD documents, **" (:scope-count changes) " scope lines changed** and **" (:sign-count changes) " section-trajectory signs changed**.\n\n"
         "| id | scope before | scope after | section before | section after |\n|---|---|---|---:|---:|\n"
         (str/join "\n" (for [{:keys [id old-scope new-scope old-section new-section]} (:sample changes)]
                            (str "| `" id "` | " old-scope " | " new-scope " | " (fmt old-section) " " (arrow old-section) " | " (fmt new-section) " " (arrow new-section) " |")))
         "\n\n## The 107 one-line sheets\n\n| id | scopes | keywords | changes | register trajectory | status |\n|---|---|---|---|---|---|\n"
         (str/join "\n" (map sheet-line rows))
         "\n\n## Register distribution by kind\n\n| kind | n | min | q1 | median | q3 | max | mean |\n|---|---:|---:|---:|---:|---:|---:|---:|\n"
         (str/join "\n" (for [[kind rs] by-kind :let [xs (mapv :register rs)]]
                              (str "| `" kind "` | " (count rs) " | " (fmt (apply min xs)) " | " (fmt (quantile xs 0.25)) " | " (fmt (quantile xs 0.5)) " | " (fmt (quantile xs 0.75)) " | " (fmt (apply max xs)) " | " (fmt (mean xs)) " |")))
         "\n\n## Correlations\n\nSpearman rho uses average ranks for ties over all 107 items.\n\n| comparison | rho |\n|---|---:|\n| whole-document register vs B2 marker count | " (fmt (:markers rhos)) " |\n| whole-document register vs B1(b) shared missions | " (fmt (:shared rhos)) " |\n\nFirst-cut rounded values were 0.224 and 0.040; against those recorded values the regenerated deltas are " (fmt6 (- (:markers rhos) 0.224)) " and " (fmt6 (- (:shared rhos) 0.040)) ". Neither moved by more than 0.05. Trajectory/history disagreement changed from 8 to " (:disagreement-count changes) " items (delta " (- (:disagreement-count changes) 8) ").\n\n"
         "## Observe-square token fields\n\nThe observe square could emit the mechanically traceable fields `:scope-counts`, `:tf-idf-keywords`, `:declared-keywords`, `:applied-pattern-order`, `:register`, `:section-register-trajectory`, `:history-register-trajectory`, `:status`, and `:lifecycle-phase`. Register and its two trajectories are feasibility observations; disagreement between the two trajectories remains explicit rather than being collapsed into one direction.\n")))

(defn -main [& _]
  (let [run-body (slurp run-path) run (edn/read-string run-body)
        ranked (get-in run [:outer-task-selection :policy :meta-selection :ranking])
        _ (when-not (= 107 (count ranked)) (throw (ex-info "ranking count changed" {:count (count ranked)})))
        support (into {} (map (juxt :id identity)) (get-in run [:outer-task-selection :support]))
        docs (mapv (fn [{:keys [id kind] :as rank}]
                     (let [path (get-in (support id) [:source :path])]
                       (when-not (and path (.isFile (java.io.File. path)))
                         (throw (ex-info "ranked document unavailable" {:id id :path path})))
                       {:id id :kind kind :rank rank :path path :text (slurp path)})) ranked)
        kws (keyword-map docs) fetched (fetch-graph) app (applied (:graph fetched))
        rows (mapv (fn [{:keys [id kind path text rank]}]
                     (let [sr (section-registers text) old-sr (legacy-section-registers text)
                           hr (history-register path) st (status text)
                           sd (:delta sr) hd (:delta hr)]
                       (merge {:id id :kind kind :path path :scopes (scopes text path) :keywords (kws id)
                               :legacy-scopes (legacy-scopes text path) :legacy-section-delta (:delta old-sr)
                              :declared (declared-keywords text) :patterns (pattern-order (get app id #{}) text)
                               :diagnostics {:source-lines (count (str/split-lines text))
                                             :path-lines (count (filter #(str/includes? % "/") (str/split-lines text)))
                                             :checkboxes (count (re-seq #"(?m)^\s*- \[[ xX]\]" text))
                                             :sha-tokens (count (re-seq #"(?i)\b[0-9a-f]{7,64}\b" text))}
                               :register (:value (line-register (str/split-lines text)))
                               :section-registers (:sections sr) :section-excluded (:excluded sr)
                               :section-last-third (:last-third sr) :section-delta sd :history hr :history-delta hd
                               :disagree (neg? (* sd hd)) :markers (marker-count text) :shared (shared-count app id)
                               :occurrence (get-in rank [:channels :pipeline-structural-centrality-cost :observation :value])}
                              st))) docs)
        comparisons (mapv (fn [row]
                            {:id (:id row)
                             :old-scope (scopes-text (get-in row [:legacy-scopes :top]))
                             :new-scope (scopes-text (get-in row [:scopes :top]))
                             :old-section (:legacy-section-delta row)
                             :new-section (:section-delta row)}) rows)
        changed (filterv #(or (not= (:old-scope %) (:new-scope %))
                              (not= (arrow (:old-section %)) (arrow (:new-section %)))) comparisons)
        changes {:scope-count (count (filter #(not= (:old-scope %) (:new-scope %)) comparisons))
                 :sign-count (count (filter #(not= (arrow (:old-section %))
                                                 (arrow (:new-section %))) comparisons))
                 :disagreement-count (count (filter :disagree rows))
                 :sample (vec (take 10 changed))}
        _ (when-not (= [94 58] [(:scope-count changes) (:sign-count changes)])
            (throw (ex-info "first-cut comparison changed" {:changes changes})))
        result {:head (str/trim (sh! "git" "rev-parse" "HEAD"))
                :run-pin {:sha256 (sha256 run-body)}
                :graph-pin (sha256 (pr-str (sort-by pr-str (get-in fetched [:graph :patterns :edges]))))
                :rows rows
                :changes changes
                :rhos {:markers (rho (mapv :register rows) (mapv :markers rows))
                       :shared (rho (mapv :register rows) (mapv :shared rows))}}]
    (spit report-path (render result))
    (println report-path)))

(case (first *command-line-args*)
  "--check-scope" (check-scope!)
  "--check" (do (check-scope!) (check-trajectory!))
  (apply -main *command-line-args*))
