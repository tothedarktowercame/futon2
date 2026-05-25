(ns futon2.aif.mission-registry
  "Mission-doc substrate adapter for the WM AIF apparatus.

   This namespace is the first concrete adapter for `:open-mission`.
   It scans top-level mission docs at `*/holes/missions/M-*.md`,
   extracts a lightweight status/title view, filters to missions that are
   not complete, and exposes them as addressable targets for the WM's
   action layer.

   Honest scope: file-backed and heuristic. The adapter reads mission docs'
   `**Status:**` line rather than a typed substrate. This is still an honest
   improvement over the prior state where `:open-mission` was permanently
   gated and only surfaced as `:learn-action-class`.

   The interface (`load-missions` / `open-missions` /
   `can-propose? :open-mission` / the enumerator proposer) is the swap
   target for a richer substrate if mission metadata becomes canonical
   elsewhere."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.action-proposer :as ap]
            [futon2.aif.forward-model :as fm]))

(def ^:private default-code-root
  (str (System/getProperty "user.home") "/code"))

(def ^:private mission-path-pattern
  #".*/holes/missions/(M-[^/]+)\.md$")

(def ^:private status-line-pattern
  #"(?i)^\*\*Status:\*\*\s*(.+)$")

(defn- mission-id-from-path
  [path]
  (second (re-matches mission-path-pattern path)))

(defn- mission-title-from-lines
  [mission-id lines]
  (or (some (fn [line]
              (when-let [[_ title] (re-matches #"^#\s+(.+)$" line)]
                title))
            lines)
      mission-id))

(defn- classify-status
  [status-line]
  (let [upper (str/upper-case (or status-line ""))]
    (cond
      (str/includes? upper "COMPLETE") :complete
      (str/includes? upper "COMPLETED") :complete
      (str/includes? upper "CLOSED") :complete
      (str/includes? upper "ACTIVE") :active
      (str/includes? upper "OPEN") :open
      (str/includes? upper "PARTIAL") :partial
      (str/includes? upper "IDENTIFY") :identify
      :else :unknown)))

(defn- mission-doc->entry
  [path]
  (let [lines (str/split-lines (slurp path))
        mission-id (mission-id-from-path path)
        status-line (some (fn [line]
                            (when-let [[_ status] (re-matches status-line-pattern line)]
                              status))
                          (take 20 lines))]
    {:id mission-id
     :path path
     :title (mission-title-from-lines mission-id lines)
     :status-line status-line
     :status-class (classify-status status-line)}))

(defn load-missions
  "Scan the code root for top-level mission docs and return
   `{:missions [...]}` with lightweight metadata for each one.

   Only files matching `*/holes/missions/M-*.md` at the immediate mission-doc
   level are included; nested handoff/journal/support docs are excluded."
  ([] (load-missions default-code-root))
  ([code-root]
   (let [root (io/file code-root)
         missions (->> (file-seq root)
                       (filter #(.isFile %))
                       (map #(.getAbsolutePath %))
                       (filter #(re-matches mission-path-pattern %))
                       sort
                       (mapv mission-doc->entry))]
     {:missions missions})))

(defn open-missions
  "Filter loaded missions to those that are not complete. Zero-arg variant
   scans the default code root; one-arg variant accepts a pre-loaded doc."
  ([] (open-missions (load-missions)))
  ([loaded]
   (vec (remove #(= :complete (:status-class %))
                (:missions loaded)))))

(defmethod fm/can-propose? :open-mission
  [state _action-type]
  (boolean (seq (:missions state))))

(defmethod fm/can-execute? :open-mission
  [state action]
  (boolean (some #(= (:target action) (:id %))
                 (:missions state []))))

(def mission-enumerator-proposer
  "Proposer that emits one `:open-mission` candidate per addressable mission
   in the state map. State must carry `:missions` (populated by
   `open-missions`)."
  (reify ap/ActionProposer
    (propose [_ state]
      (for [m (:missions state)]
        {:type :open-mission
         :target (:id m)
         :weight 1.0
         :mission-path (:path m)
         :rationale (str "mission substrate: " (:title m)
                         " [" (name (:status-class m)) "]")}))
    (proposer-id [_] :mission-enumerator)))
