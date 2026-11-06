(ns futon2.aif.wm.action-scale
  "Operator-curated action-cost scale annotations (M-action-cost-modelling).

  The mission's scope-in names three possible sources for a candidate
  action's cost signal: (a) operator-curated EDN, (b) ARGUE-mined text,
  (c) substrate-2 T-delta.  This namespace lands (a): a curated EDN file
  (`resources/wm/action-scales.edn`) mapping action targets to one of the
  mission's scale keywords, plus attachment functions that give EVERY
  candidate action a `:scale` — curated rows carry `:scale-source
  :operator-curated`, everything else a typed per-action default under
  `:scale-source :default` so a reader can tell annotation from
  judgement.

  Annotation only: nothing here scores, ranks, gates or refuses.  The
  selection law (`futon2.aif.policy/select-action-cascades`) consumes
  nothing from this namespace; attaching a scale never changes a decision.
  "
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def scale-vocabulary
  "The mission's proposed scale ladder (M-action-cost-modelling §Scope in,
   line 'Annotating candidate actions with a :scale field')."
  [:minute :hour :session :multi-session :mission :campaign])

(def ^:private scale-set (set scale-vocabulary))

(def default-path "wm/action-scales.edn")

(defn- default-scales-path []
  (io/resource default-path))

(defn load-action-scales
  "Read the curated scale EDN.  Default: the classpath resource
  `resources/wm/action-scales.edn`.  Returns {:schema ... :scales {target
  {:scale s :rationale r}}} or a typed :absent record — never nil, never
  an empty map masquerading as a read."
  ([] (load-action-scales (default-scales-path)))
  ([source]
   (let [readable (cond
                    (instance? java.net.URL source) source
                    (instance? java.io.File source) source
                    (string? source) (io/resource source)
                    :else nil)]
     (if (nil? readable)
       {:status :absent :reason :no-action-scales-source}
       (let [doc (edn/read-string (slurp readable))]
         (if (map? (:scales doc))
           {:status :loaded :scales (:scales doc)}
           {:status :absent :reason :malformed-action-scales
            :schema (:schema doc)}))))))

(defn default-scale-for
  "Typed fallback for a candidate whose target has no curated entry.
  These are annotations about work shape, not judgements: :no-op does
  nothing, :learn-action-class and :address-sorry are bounded single-step
  moves, and unannotated mission/cascade work defaults to :mission."
  [{:keys [type]}]
  (case type
    :no-op :minute
    :learn-action-class :session
    :address-sorry :session
    :survey :session
    :survey-mission :session
    :close :session
    :close-mission :session
    :close-hole :session
    :advance-ticket :session
    :open-mission :multi-session
    :decompose :session
    :mission))

(defn scale-for-target
  "The curated scale entry for a target id (string or keyword; matched by
  name), or nil when nothing is curated."
  [loaded target]
  (when (and (= :loaded (:status loaded))
             (some? target))
    (get-in loaded [:scales (if (keyword? target) (name target) (str target))])))

(defn attach-scale
  "Return the action map with :scale and :scale-source attached.
  Curated targets keep their curated value under
  :scale-source :operator-curated; everything else carries
  `default-scale-for` under :scale-source :default.  An action with an
  explicit :scale already present is returned unchanged (caller-owned
  annotation wins).  nil stays nil — absence is not annotated."
  ([action] (attach-scale action (load-action-scales)))
  ([action loaded]
   (cond
     (nil? action) nil
     (contains? action :scale) action
     :else
     (let [curated (scale-for-target loaded (:target action))]
       (if (and (map? curated) (contains? scale-set (:scale curated)))
         (assoc action
                :scale (:scale curated)
                :scale-source :operator-curated)
         (assoc action
                :scale (default-scale-for action)
                :scale-source :default))))))

(defn attach-to-ranked
  "Attach :scale to every ranked row's :action (rows shaped {:action a
  ...}, the historical ranked-actions / cascade-lane R5 shape).  Returns a
  vector; every non-nil action in the input carries a scale in the
  output."
  ([rows] (attach-to-ranked rows (load-action-scales)))
  ([rows loaded]
   (mapv (fn [row]
           (if-let [action (:action row)]
             (assoc row :action (attach-scale action loaded))
             row))
         rows)))
