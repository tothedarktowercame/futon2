(ns futon2.aif.mission-registry
  "Mission-doc substrate adapter for the WM AIF apparatus.

   This namespace is the first concrete adapter for `:open-mission`.
   It scans live top-level mission docs at `*/holes/missions/M-*.md`,
   extracts a lightweight status/title view, filters to missions that are
   not closed/draft/sandbox, and exposes them as addressable targets for the WM's
   action layer.

   Honest scope: file-backed and heuristic. The adapter reads mission docs'
   `Status:` line rather than a typed substrate. This is still an honest
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
  #"(?i)^\s*(?:[-*]\s*)?(?:#+\s*)?(?:\*\*)?Status:?(?:\*\*)?\s*:?\s*(.+)$")

(defn- mission-id-from-path
  [path]
  (second (re-matches mission-path-pattern path)))

(defn- sandbox-path?
  [path]
  (str/includes? path "/.state/"))

(defn- derived-mission-id?
  [mission-id]
  (str/includes? (or mission-id "") "."))

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
      (or (re-find #"\bDRAFT\b" upper)
          (str/includes? upper "SPECIFIED, NOT YET IMPLEMENTED")) :draft
      (re-find #"\b(ARCHIVED|PARKED|SUPERSEDED|DEFERRED|ABANDONED)\b" upper) :inactive
      (re-find #"\b(COMPLETE|COMPLETED|CLOSED|DONE|DISCHARGED|ANSWERED|DISSOLVED)\b" upper) :complete
      (str/includes? upper "ACTIVE") :active
      (str/includes? upper "OPEN") :open
      (str/includes? upper "PARTIAL") :partial
      (str/includes? upper "IDENTIFY") :identify
      (str/includes? upper "HEAD") :open
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
                       (remove sandbox-path?)
                       sort
                       (map mission-doc->entry)
                       (remove #(derived-mission-id? (:id %)))
                       vec)]
     {:missions missions})))

(defn live-mission?
  "True when a loaded mission entry is eligible for WM `:open-mission`
   enumeration/ranking."
  [mission]
  (not (contains? #{:complete :inactive :draft}
                  (:status-class mission))))

(defn open-missions
  "Filter loaded missions to those that are live. Zero-arg variant
   scans the default code root; one-arg variant accepts a pre-loaded doc."
  ([] (open-missions (load-missions)))
  ([loaded]
   (vec (filter live-mission? (:missions loaded)))))

(defn mission-target-id
  "Normalize a mission action target to the file-backed registry id when possible.
   Substrate-2 mission endpoints such as `futon4-d/mission/foo` normalize to
   `M-foo`; ordinary registry ids such as `M-foo` are returned unchanged."
  [target]
  (let [target (cond
                 (keyword? target) (name target)
                 (some? target) (str target)
                 :else nil)]
    (cond
      (nil? target) nil
      (str/starts-with? target "M-") target
      :else (when-let [[_ local-id] (re-find #"/mission/([^/]+)$" target)]
              (str "M-" local-id)))))

(defn live-mission-target?
  "True if TARGET resolves to one of MISSIONS' live registry ids."
  [missions target]
  (let [live-ids (set (map :id (filter live-mission? missions)))]
    (contains? live-ids (mission-target-id target))))

(defmethod fm/can-propose? :open-mission
  [state _action-type]
  (boolean (seq (:missions state))))

(defmethod fm/can-execute? :open-mission
  [state action]
  (live-mission-target? (:missions state []) (:target action)))

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
