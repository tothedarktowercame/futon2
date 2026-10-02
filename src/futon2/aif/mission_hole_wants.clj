(ns futon2.aif.mission-hole-wants
  "Mission-stated wants as cascade sources.

  live-C holds a want entry per mission in the corpus -- 465 of them -- and a
  mission's weight can only reach a decision if that mission is a DECLARED
  target with outcome tokens (`live-c/project-want` pairs a mission token with
  the outcomes that mission itself declared). Until now exactly one mission had
  a hand-written declaration, so the decision reached 3 of 465 entries, about
  0.47% of the corpus weight, and 464 sat in `:unreached-in-domain`.

  Missions already state what they want done. This namespace reads each
  mission's current HEAD bytes and turns unchecked tasks plus stated completion
  criteria into the source shape `cascade-problems/assemble` consumes. Registry
  `:open-holes` remains discovery metadata and has no decision authority.

  Free-form work markers still have no checkable closure form and are reported
  through coverage rather than invented as false facts. Completion criteria
  use `mission-criteria`'s verdict-aware locators; an unlocated criterion stays
  explicit and makes assembly refuse rather than silently disappearing.

  The witness is affirmation-shaped: the observation is that a CHECKED item is
  present, never that an unchecked one is absent."
  (:require [futon2.aif.load-identity :as load-identity]
            [futon2.aif.mission-criteria :as criteria]
            [futon2.aif.observation-checks :as checks]
            [clojure.java.shell :as sh]
            [clojure.string :as str]))

(load-identity/register! *ns* *file*)

(defn observable-hole?
  "A retained hole whose closure some check can witness. Only unchecked tasks
   have one: the line carries a checkbox that closing it flips."
  [hole]
  (and (= :unchecked-task (:kind hole))
       (re-find #"^[-*]\s+\[\s\]\s+\S" (str (:text hole)))))

(defn want-token
  "The outcome token for a hole, from its stable content-addressed id.

  The name is prefixed with `h` because the ids are hex digests and Clojure's
  READER rejects a keyword whose name starts with a digit -- `:hole/2f9b03b16170`
  prints without complaint and then throws `Invalid token` when anything reads
  the record back. That is what killed the tick of 2026-09-21-1789948972 at
  :initialization, after the token had already travelled through scoring and
  been written to a run record. The prefix is unconditional so the mapping from
  id to token stays injective."
  [hole]
  (let [id (str (:id hole))
        h (str/index-of id "#")]
    (keyword "hole" (str "h" (if h (subs id (inc h)) id)))))

(defn closed-form
  "The line the document carries once this hole is closed: the same item with
   its box ticked. This is the declaration head C4 anchors."
  [hole]
  (str/replace-first (str/trim (:text hole)) #"^([-*]\s+)\[\s\]" "$1[x]"))

(defn hole-locator
  "C4 over the mission's own document: the checked item is present at HEAD."
  [code-root mission hole]
  (let [path (str (:path mission))
        rel (str/replace-first path (str code-root "/") "")
        repo (first (str/split rel #"/"))]
    {:class :C4
     :repo repo
     :sha "HEAD"
     :path (str/replace-first rel (str repo "/") "")
     :decl (closed-form hole)}))

(defn read-current-mission
  "Read MISSION from its repository HEAD.  The registry's `:open-holes` is
  discovery metadata, not decision state; selection must derive wants and
  observations from one current byte string."
  [code-root mission]
  (let [abs (str (:path mission))
        rel (str/replace-first abs (str code-root "/") "")
        [repo & parts] (str/split rel #"/")
        path (str/join "/" parts)]
    (if (contains? mission :text)
      {:repo repo :path path :text (:text mission)}
      (let [{:keys [exit out]} (sh/sh "git" "-C" (str code-root "/" repo)
                                      "show" (str "HEAD:" path))]
        (when (zero? exit) {:repo repo :path path :text out})))))

(defn- current-checkboxes [target text]
  (vec (keep-indexed
        (fn [i line]
          (when (re-find #"^[-*]\s+\[\s\]\s+\S" line)
            {:id (str target "#" (subs (load-identity/sha256
                                         (.getBytes (str/trim line) "UTF-8")) 0 12))
             :kind :unchecked-task :line (inc i) :text line}))
        (str/split-lines text))))

(defn mission-source
  "One target's worth of sources, or nil when the mission states no observable
   want. Shape matches `cascade-problems/assemble`'s `:sources`."
  [code-root mission]
  (let [terminal? (contains? #{:complete :inactive} (:status-class mission))
        target (str (:id mission))
        {:keys [repo path text]} (when-not terminal? (read-current-mission code-root mission))
        holes (when text (current-checkboxes target text))
        checkbox-tokens (mapv want-token holes)
        checkbox-locators (into {} (map (fn [h] [(want-token h) (hole-locator code-root mission h)])) holes)
        criterion-result (when text
                           (criteria/wants (criteria/criteria target text)
                                           {:repo repo :path path
                                            :observe #(true? (:observed (checks/check-decl-in-file %)))}))
        ;; A current checkbox is an operational closure surface.  When one
        ;; exists, do not let older prose criteria with no stated verdict
        ;; make the whole mission unselectable: retain them under :unlocated,
        ;; but admit only criteria that themselves have a checkable locator.
        ;; With no checkbox surface, the existing fail-closed behaviour stays:
        ;; unlocated criteria remain wants and assembly names the defect.
        criterion-tokens (if (seq checkbox-tokens)
                           (filterv #(contains? (:locators criterion-result) %)
                                    (:wants criterion-result))
                           (:wants criterion-result))
        tokens (vec (distinct (concat checkbox-tokens criterion-tokens)))]
    (when (seq tokens)
      (let [criterion-locators (:locators criterion-result)
            criterion-universe (:universe criterion-result)]
        {:target target
         :want tokens
         ;; Checkbox and criterion observations are read from the same HEAD
         ;; whose text produced the wants.  No retained substrate fact is
         ;; allowed to assert false against a current checked locator.
         :universe (merge (zipmap checkbox-tokens (repeat false)) criterion-universe)
         :locators (merge checkbox-locators criterion-locators)
         ;; A stated want does not establish any pattern's applicability.
         ;; Agent-authored declarations supply interpretations through the loader.
         :interpretation {:patterns {} :receipts {}}
         :candidates []
         :holes (mapv (fn [h] (select-keys h [:id :kind :line :text])) holes)
         :criteria (:criteria criterion-result)
         :unlocated (:unlocated criterion-result)
         :source {:kind :current-mission-head
                  :repo repo :path path
                  :sha256 (load-identity/sha256 (.getBytes text "UTF-8"))}}))))

(defn mission-sources
  "Sources for every live mission with an observable stated want, plus a typed
   account of what was retained and not projected -- so the gap between 441
   stated holes and the projected subset is visible rather than silent."
  [code-root missions]
  (let [live (remove #(contains? #{:complete :inactive} (:status-class %)) missions)
        sources (keep #(mission-source code-root %) live)
        retained (reduce + (map #(count (:open-holes %)) live))
        projected (reduce + (map #(count (:holes %)) sources))
        current-wants (reduce + (map #(count (:want %)) sources))]
    {:sources (vec sources)
     :targets (mapv :target sources)
     :coverage {:live-missions (count live)
                :missions-projected (count sources)
                :holes-retained retained
                :holes-projected projected
                :holes-not-projected (- retained projected)
                :current-head-wants current-wants
                :retained-by-kind (frequencies (map :kind (mapcat :open-holes live)))
                :projected-by-kind (frequencies (map :kind (filter observable-hole?
                                                                  (mapcat :open-holes live))))
                :not-projected-by-kind
                (frequencies (map :kind (remove observable-hole?
                                                (mapcat :open-holes live))))
                :reason-not-projected
                "no check can witness closure: the item carries no checkbox to flip"}}))

(defn merge-into-sources
  "Merge mission-stated wants into the declared source map, so a mission's own
   document can carry its live-C weight into the decision.

   Current mission HEAD wins for wants, observations and locators. A declaration
   may supply pattern interpretations and family parameters, but it is not a
   canonical snapshot of a mission and cannot freeze an older want set. Candidate
   orders are reconstructed for the current problem."
  [declared code-root missions context]
  (let [{:keys [sources coverage]} (mission-sources code-root missions)
        declared-targets (set (keys (:universes declared)))
        ;; `live-c/family-schedule` and `family-scales` require ONE value
        ;; across every problem in the compared family, and a target carrying
        ;; none falls back to the defaulted :every-step / default scales. A
        ;; generated target without them therefore makes the family
        ;; incommensurable and the tick refuses to select at all -- observed
        ;; 2026-09-20 in run 2026-09-20-1789940260, which produced no cascade
        ;; selection because these sources defaulted to :every-step beside a
        ;; declared :terminal.
        ;;
        ;; So ADOPT the one value the declared sources already agree on. This
        ;; namespace reads documents; it has no authority to choose a schedule
        ;; or a scale. When the declared sources do not agree on exactly one,
        ;; generate NOTHING and say why -- an unusable family is worse than an
        ;; unextended one.
        one-of (fn [m] (let [vs (distinct (keep #(get m %) declared-targets))]
                         (when (= 1 (count vs)) (first vs))))
        schedule (one-of (:preference-schedules declared))
        scales (one-of (:preference-scales declared))
        fresh (if (and schedule scales) sources [])
        by (fn [k] (into {} (map (juxt :target k)) fresh))]
    (-> declared
        (update :preference-schedules merge
                (into {} (map (fn [f] [(:target f) schedule])) fresh))
        (update :preference-scales merge
                (into {} (map (fn [f] [(:target f) scales])) fresh))
        (update :universes merge (by :universe))
        (update :wants merge (by :want))
        (update :locators merge (by :locators))
        ;; Declarations may remain useful interpretation evidence, but never
        ;; override the mission's current state or supply a canonical order.
        (update :interpretations #(or % {}))
        (update :candidates merge (by :candidates))
        (update :context-by-target merge
                (into {} (map (fn [f] [(:target f) context])) fresh))
        (assoc :mission-hole-coverage
               (assoc coverage
                      :adopted-schedule schedule
                      :adopted-scales scales
                      :not-generated-reason
                      (when-not (and schedule scales)
                        :declared-sources-lack-one-agreed-schedule-or-scales)
                      :targets-added (count fresh)
                      :declared-targets-refreshed
                      (mapv :target (filter #(contains? declared-targets (:target %)) sources)))))))
