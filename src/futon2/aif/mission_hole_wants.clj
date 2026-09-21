(ns futon2.aif.mission-hole-wants
  "Mission-stated wants as cascade sources.

  live-C holds a want entry per mission in the corpus -- 465 of them -- and a
  mission's weight can only reach a decision if that mission is a DECLARED
  target with outcome tokens (`live-c/project-want` pairs a mission token with
  the outcomes that mission itself declared). Until now exactly one mission had
  a hand-written declaration, so the decision reached 3 of 465 entries, about
  0.47% of the corpus weight, and 464 sat in `:unreached-in-domain`.

  Missions already state what they want done. `mission-registry/open-holes`
  retains those statements. This namespace turns the MECHANICALLY OBSERVABLE
  ones into the source shape `cascade-problems/assemble` consumes, so a
  mission's own stated work can carry its weight into the decision without
  anyone hand-authoring a declaration per mission.

  WHAT IS DELIBERATELY NOT PROJECTED. Of 441 retained holes, only the 99
  `:unchecked-task` items have an observable closed-form: `- [ ] X` becoming
  `- [x] X`, which `observation-checks/decl-present?` can anchor. Work markers,
  pending-lifecycle lines and open-section items state real work and have no
  checkbox to flip, so no check can witness their closure -- they are retained
  and reported, never projected. Projecting them would raise the coverage number
  while giving the decision nothing it could act on, which is the failure this
  namespace exists to avoid, pointing the other way.

  The witness is affirmation-shaped: the observation is that a CHECKED item is
  present, never that an unchecked one is absent."
  (:require [futon2.aif.load-identity :as load-identity]
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

(defn mission-source
  "One target's worth of sources, or nil when the mission states no observable
   want. Shape matches `cascade-problems/assemble`'s `:sources`."
  [code-root mission]
  (let [terminal? (contains? #{:complete :inactive :draft} (:status-class mission))
        holes (when-not terminal? (filter observable-hole? (:open-holes mission)))]
    (when (seq holes)
      (let [target (str (:id mission))
            tokens (mapv want-token holes)]
        {:target target
         :want tokens
         ;; Stated and not yet witnessed closed. Never :unknown: the check runs.
         :universe (zipmap tokens (repeat false))
         :locators (into {} (map (fn [h] [(want-token h) (hole-locator code-root mission h)])) holes)
         ;; A stated want does not establish any pattern's applicability.
         ;; Agent-authored declarations supply interpretations through the loader.
         :interpretation {:patterns {} :receipts {}}
         :candidates []
         :holes (mapv (fn [h] (select-keys h [:id :kind :line :text])) holes)}))))

(defn mission-sources
  "Sources for every live mission with an observable stated want, plus a typed
   account of what was retained and not projected -- so the gap between 441
   stated holes and the projected subset is visible rather than silent."
  [code-root missions]
  (let [live (remove #(contains? #{:complete :inactive :draft} (:status-class %)) missions)
        sources (keep #(mission-source code-root %) live)
        retained (reduce + (map #(count (:open-holes %)) live))
        projected (reduce + (map #(count (:want %)) sources))]
    {:sources (vec sources)
     :targets (mapv :target sources)
     :coverage {:live-missions (count live)
                :missions-projected (count sources)
                :holes-retained retained
                :holes-projected projected
                :holes-not-projected (- retained projected)
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

   A HAND-WRITTEN DECLARATION WINS on any target it names. An operator wrote it;
   this namespace only reads a document. Generated targets are added, never
   substituted, and the coverage account rides on the returned map so the record
   says how many stated holes were retained, how many were projected, and why
   the rest were not."
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
        fresh (if (and schedule scales)
                (remove #(contains? declared-targets (:target %)) sources)
                [])
        by (fn [k] (into {} (map (juxt :target k)) fresh))]
    (-> declared
        (update :preference-schedules merge
                (into {} (map (fn [f] [(:target f) schedule])) fresh))
        (update :preference-scales merge
                (into {} (map (fn [f] [(:target f) scales])) fresh))
        (update :universes merge (by :universe))
        (update :wants merge (by :want))
        (update :locators merge (by :locators))
        (update :interpretations merge (by :interpretation))
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
                      :targets-deferred-to-declaration
                      (mapv :target (filter #(contains? declared-targets (:target %)) sources)))))))
