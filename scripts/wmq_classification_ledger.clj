(ns wmq-classification-ledger
  (:require [babashka.fs :as fs] [cheshire.core :as json] [clojure.edn]
            [clojure.pprint :as pp] [clojure.string :as str]))

(def terminal #{"COMPLETE" "COMPLETED" "CLOSED" "DONE" "DISCHARGED" "ANSWERED" "DISSOLVED" "RESOLVED"})
(def superseded #{"SUPERSEDED" "ARCHIVED"})
(def abandoned #{"ABANDONED"})
(def active #{"ACTIVE" "PARTIAL" "IDENTIFY" "MAP" "DERIVE" "ARGUE" "VERIFY" "INSTANTIATE"})
(defn status-line [lines]
  (first (keep-indexed (fn [i line]
    (when-let [[_ value] (re-find #"(?i)^\s*(?:[-*]\s*)?(?:#+\s*)?(?:\*\*)?Status:?(?:\*\*)?\s*:?\s*(.+)$" line)]
      {:line (inc i) :text (str/trim value)})) lines)))
(defn lead [s] (some-> s str/upper-case (str/replace #"^[\s>*#`_~-]+" "") (str/split #"[^A-Z-]+") first))
(defn state-of [status]
  (let [h (lead (:text status))]
    (cond (terminal h) :done (superseded h) :superseded (abandoned h) :abandoned
          (= h "OPEN") :open (active h) :active :else :unknown)))
(defn repo-of [path] (or (second (re-find #"/code/([^/]+)/" (str path))) "unknown"))
(defn first-mission-ref [lines]
  (some (fn [[i line]] (when-let [m (re-find #"\b(M-[A-Za-z0-9_.-]+)\b" line)]
                         {:id (str/replace (second m) #"\.md$" "") :line (inc i)}))
        (map-indexed vector lines)))
(defn first-match [lines kind pattern]
  (some (fn [[i line]] (when (re-find pattern line)
                         {:kind kind :line (inc i) :text (str/trim line)}))
        (map-indexed vector lines)))
(defn content-reading [lines]
  (let [sup (first-match lines :content-superseded #"(?i)^\s*(?:[-*]\s*)?(?:result|outcome|resolution|verdict|disposition)\s*:\s*(?:superseded|obsolete|replaced by)\b")
        aban (first-match lines :content-abandoned #"(?i)^\s*(?:[-*]\s*)?(?:result|outcome|resolution|verdict|disposition)\s*:\s*(?:abandoned|wontfix|won't fix|not pursuing)\b")
        done (first-match lines :content-terminal
                          #"(?i)^\s*(?:[-*]\s*)?(?:result|outcome|resolution|closed by|verdict|disposition)\s*:\s*.{0,80}\b(?:resolved|completed|fixed|shipped|landed|all checks pass(?:ed)?|all criteria met)\b")
        unchecked (first-match lines :unchecked-task #"^\s*[-*]\s+\[\s\]\s+\S")
        active-work (first-match lines :active-work #"(?i)\b(?:in progress|currently working|underway)\b")
        open-work (or unchecked (first-match lines :open-work
                    #"(?i)^\s*(?:[-*]\s*)?(?:next action|remaining work|todo)\s*:\s*\S"))]
    (cond sup {:state :superseded :confidence :high :signal sup}
          aban {:state :abandoned :confidence :high :signal aban}
          ;; Open work defeats incidental retrospective uses of "fixed/landed".
          (and done (not open-work)) {:state :done :confidence :high :signal done}
          active-work {:state :active :confidence :medium :signal active-work}
          open-work {:state :open :confidence :medium :signal open-work}
          :else {:state :open :confidence :low :basis :default-open})))
(defn repair-finding [id]
  (when-let [[_ digest] (re-matches #"T-(repair-occ-.+)" id)]
    (let [path (str "/home/joe/code/futon2/data/wm-repair-obligations/findings/" digest ".edn")]
      (when (fs/exists? path)
        (let [m (clojure.edn/read-string (slurp path)) s (:repair/status m)]
          {:state (case s (:done :resolved :closed :complete) :done :open)
           :confidence :high :signal {:kind :finding-status :path path :value s}})))))
(defn classify [{:keys [id kind source ineligible-reason]}]
  (let [path (:path source) exists? (and path (fs/exists? path))
        lines (if exists? (str/split-lines (slurp path)) [])
        status (status-line lines) declared-state (state-of status) parent (first-mission-ref lines)
        content (content-reading lines) authority (repair-finding id)
        reading (or authority content)
        state (if (and status (not= :unknown declared-state)) declared-state (:state reading))
        category (if parent (:id parent) (keyword "repo" (repo-of path)))
        confidence (if (and status (not= :unknown declared-state)) :high (:confidence reading))
        evidence (cond-> []
                   status (conj {:kind :declared-status :pointer (str path ":" (:line status)) :text (:text status)})
                   parent (conj {:kind :mission-context :pointer (str path ":" (:line parent)) :target (:id parent)})
                   (:signal reading) (conj (cond-> (:signal reading)
                                            (:line (:signal reading))
                                            (assoc :pointer (str path ":" (:line (:signal reading))))))
                   ineligible-reason (conj {:kind :selector-context :value (keyword ineligible-reason)})
                   exists? (conj {:kind :source :path path :sha256 (:sha256 source)}))
        ;; A mission mention categorises context but does not name an existing
        ;; cascade cluster.  Inventing a cluster node would violate O4, so the
        ;; ledger leaves edge proposals empty pending an authoritative mapping.
        edges []]
    {:id id :kind (keyword kind) :proposed-state state :content-state (:state content)
     :declared-state (when status declared-state) :category/cluster category
     :proposed-edges edges :evidence evidence :confidence confidence
     :application (if (and status (= confidence :high)) :already-declared :review-only)}))
(defn counts [rows k] (frequencies (map k rows)))
(let [[input output] *command-line-args* receipt (:receipt (json/parse-string (slurp input) true))
      rows (->> (concat (:support receipt) (:excluded receipt))
                (filter #(contains? #{"excursion" "ticket"} (:kind %)))
                (map classify) (sort-by (juxt :kind :id)) vec)
      calibration (let [sample (filter #(and (:declared-state %) (not= :unknown (:declared-state %))) rows)
                        predicted (filter #(some (fn [e] (contains? #{:content-terminal :content-superseded
                                                                      :content-abandoned :unchecked-task
                                                                      :active-work :open-work}
                                                                    (:kind e))) (:evidence %)) sample)
                        agree (count (filter #(= (:declared-state %) (:content-state %)) predicted))]
                    {:declared-sample (count sample) :content-predicted (count predicted)
                     :agreements agree :agreement-rate (if (seq predicted) (/ agree (double (count predicted))) 0.0)})
      ledger {:schema :wm/task-content-context-classification-v2 :observed-at "2026-10-09"
              :method {:state "declared state, then finding authority, then strong content signals; unresolved task docs => conservative open/low"
                       :context "finding status, outcome/work markers, mission references, selector exclusion, source digest"
                       :map "O1-O5 require existing producer identities; mission mentions categorise but do not warrant edges"}
              :calibration calibration
              :summary {:total (count rows) :by-kind (counts rows :kind)
                        :by-state (counts rows :proposed-state) :by-confidence (counts rows :confidence)
                        :by-category (counts rows :category/cluster)
                        :proposed-edge-count (reduce + (map (comp count :proposed-edges) rows))}
              :applied {:document-status-writes [] :substrate-writes []
                        :registry-change ["recognise leading RESOLVED as terminal"]}
              :rows rows}]
  (with-open [w (clojure.java.io/writer output)] (binding [*out* w] (pp/pprint ledger))))
