(ns wmq-classification-ledger
  (:require [babashka.fs :as fs] [cheshire.core :as json]
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
(defn classify [{:keys [id kind source ineligible-reason]}]
  (let [path (:path source) exists? (and path (fs/exists? path))
        lines (if exists? (str/split-lines (slurp path)) [])
        status (status-line lines) state (state-of status) parent (first-mission-ref lines)
        category (if parent (:id parent) (keyword "repo" (repo-of path)))
        confidence (cond status :high parent :medium :else :low)
        evidence (cond-> []
                   status (conj {:kind :declared-status :pointer (str path ":" (:line status)) :text (:text status)})
                   parent (conj {:kind :mission-context :pointer (str path ":" (:line parent)) :target (:id parent)})
                   ineligible-reason (conj {:kind :selector-context :value (keyword ineligible-reason)})
                   exists? (conj {:kind :source :path path :sha256 (:sha256 source)}))
        ;; A mission mention categorises context but does not name an existing
        ;; cascade cluster.  Inventing a cluster node would violate O4, so the
        ;; ledger leaves edge proposals empty pending an authoritative mapping.
        edges []]
    {:id id :kind (keyword kind) :proposed-state state :category/cluster category
     :proposed-edges edges :evidence evidence :confidence confidence
     :application (if (and status (= confidence :high)) :already-declared :review-only)}))
(defn counts [rows k] (frequencies (map k rows)))
(let [[input output] *command-line-args* receipt (:receipt (json/parse-string (slurp input) true))
      rows (->> (concat (:support receipt) (:excluded receipt))
                (filter #(contains? #{"excursion" "ticket"} (:kind %)))
                (map classify) (sort-by (juxt :kind :id)) vec)
      ledger {:schema :wm/task-content-context-classification-v1 :observed-at "2026-10-09"
              :method {:state "leading Status declaration; ambiguous/missing => unknown"
                       :context "mission references, selector exclusion, source digest"
                       :map "O1-O5 require existing producer identities; mission mentions categorise but do not warrant edges"}
              :summary {:total (count rows) :by-kind (counts rows :kind)
                        :by-state (counts rows :proposed-state) :by-confidence (counts rows :confidence)
                        :by-category (counts rows :category/cluster)
                        :proposed-edge-count (reduce + (map (comp count :proposed-edges) rows))}
              :applied {:document-status-writes [] :substrate-writes []
                        :registry-change ["recognise leading RESOLVED as terminal"]}
              :rows rows}]
  (with-open [w (clojure.java.io/writer output)] (binding [*out* w] (pp/pprint ledger))))
