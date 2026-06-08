(ns futon2.report.wm-regulator-sweep
  "Deterministic possible-world regulator for War Machine EFE weights.

   The v1 automatable axis deliberately uses a futon2-local proxy:
   an automatable action is an executable :open-mission action whose target has
   at least one open hole. FAST-FOLLOW: wire the real
   futon3c.wm.guardrails/autonomous-admissible? classification."
  (:require [futon2.aif.efe :as efe]
            [futon2.aif.forward-model :as fm]
            [futon2.report.war-machine :as wm]))

(def default-k 5)
(def default-centrality-weight 0.5)
(def default-ascent-weight 0.5)
(def default-sustainability-lambda 0.001)
(def default-discrimination-min 2)

(defn- action-target [entry]
  (get-in entry [:action :target]))

(defn- open-mission-action? [entry]
  (= :open-mission (get-in entry [:action :type])))

(defn- mission-open-hole-count
  "Open-hole count for a mission. REGISTRY-FIRST: the live `:wm-missions` carry
   the current count and cover the full candidate set (~80); the capability-graph
   is the star-map subset (~31) and may be stale, so it is only a fallback.
   (F2 fix, 2026-06-08: the graph-first version blinded the useful axis to the
   ~58 candidates that are not star-map nodes — `mission-applicable?` returned
   false for them, so `:useful` was a measurement artifact, not a true signal.)"
  [snapshot mission-id]
  (long (or (some (fn [{:keys [id open-hole-count]}]
                    (when (= id mission-id) open-hole-count))
                  (:wm-missions snapshot))
            (get-in snapshot [:structure :capability-graph :missions mission-id :open-hole-count])
            0)))

(defn automatable-entry?
  "Futon2-local proxy for the real futon3c guardrail classification.
   A v1 automatable entry is an executable open-mission action with at least one
   open hole. FAST-FOLLOW: replace this with
   futon3c.wm.guardrails/autonomous-admissible? once dependency direction allows."
  [snapshot entry]
  (and (open-mission-action? entry)
       (pos? (mission-open-hole-count snapshot (action-target entry)))
       (fm/can-execute? (:wm-state snapshot) (:action entry))))

(defn- mean [xs]
  (let [xs (vec xs)]
    (if (seq xs)
      (/ (reduce + 0.0 xs) (double (count xs)))
      0.0)))

(defn score-vector
  "Compute the v1 regulator score-vector for a pinned snapshot rollout.
   Useful-work uses equal centrality/ascent weights by default (0.5/0.5).

   The single-cycle-leaf gate reads the LIVE registry hole-count (via
   `mission-open-hole-count`), so it covers every candidate, not only star-map
   nodes (F2 fix). Bundle entries are already admissible (executable open
   missions), so `applicable?` holds by construction; leaf := exactly-1-open-hole.
   NOTE: the ascent term is still graph-defined (`mission-ascent-progress`), so it
   contributes 0 for non-star-map missions; `c_joint` carries their useful signal.
   Extending ascent past the graph is a separate follow-up."
  ([snapshot rollout]
   (score-vector snapshot rollout {}))
  ([snapshot rollout {:keys [k centrality-weight ascent-weight]
                      :or {k default-k
                           centrality-weight default-centrality-weight
                           ascent-weight default-ascent-weight}}]
   (let [bundle (vec (take k (:bundle rollout)))
         graph (get-in snapshot [:structure :capability-graph])
         goal (get-in snapshot [:structure :pre-registered-goal])
         centrality (get-in snapshot [:grounding :centrality] {})
         roi-map (get-in snapshot [:grounding :roi-map] {})
         ranked (:ranked rollout)
         useful (mean
                 (for [entry bundle
                       :let [m (action-target entry)
                             leaf? (= 1 (mission-open-hole-count snapshot m))
                             c (double (or (get centrality m) 0.0))
                             ascent (double (efe/mission-ascent-progress graph goal m))]]
                   (if leaf?
                     (+ (* (double centrality-weight) c)
                        (* (double ascent-weight) ascent))
                     0.0)))
         automatable-count (count (filter #(automatable-entry? snapshot %) bundle))
         first-autonomous-rank (or (some (fn [entry]
                                           (when (automatable-entry? snapshot entry)
                                             (:rank entry)))
                                         ranked)
                                   ##Inf)
         sustainability (reduce + 0.0
                                (for [entry bundle
                                      :let [m (action-target entry)]]
                                  (double (or (get-in roi-map [m :expected-roi-gbp]) 0.0))))
         discrimination (count (distinct (map :G-total (take k ranked))))]
     {:useful useful
      :automatable (/ (double automatable-count) (double k))
      :first-autonomous-rank first-autonomous-rank
      :sustainability sustainability
      :discrimination discrimination})))

(defn question-best-bundle
  "Best capability bundle plus sparse sustainability tiebreak.
   Returns -Inf when discrimination is below d-min."
  ([score] (question-best-bundle score {}))
  ([score {:keys [lambda-s d-min]
           :or {lambda-s default-sustainability-lambda
                d-min default-discrimination-min}}]
   (if (< (:discrimination score 0) d-min)
     ##-Inf
     (+ (double (:useful score 0.0))
        (* (double lambda-s) (double (:sustainability score 0.0)))))))

(defn question-most-automatable-now
  "Most automatable now, with useful-work as lexicographic tiebreak packed into
   a small numeric epsilon. Returns -Inf below the discrimination floor."
  ([score] (question-most-automatable-now score {}))
  ([score {:keys [d-min useful-epsilon]
           :or {d-min default-discrimination-min
                useful-epsilon 1.0e-6}}]
   (if (< (:discrimination score 0) d-min)
     ##-Inf
     (+ (double (:automatable score 0.0))
        (* (double useful-epsilon) (double (:useful score 0.0)))))))

(defn sweep
  "Replay SNAPSHOT over each weight map in WEIGHT-GRID."
  [snapshot weight-grid]
  (mapv (fn [weights]
          (let [rollout (wm/rollout-snapshot-under-weights snapshot weights)]
            {:weights weights
             :rollout rollout
             :score (score-vector snapshot rollout)}))
        weight-grid))

(defn- dominates? [a b axes]
  (let [sa (:score a)
        sb (:score b)]
    (and (every? #(>= (double (get sa % 0.0))
                      (double (get sb % 0.0)))
                 axes)
         (some #(> (double (get sa % 0.0))
                   (double (get sb % 0.0)))
               axes))))

(defn pareto-front
  "Pareto front over useful, automatable, and sustainability.
   Discrimination is treated as feasibility by the question functions."
  [rows]
  (let [axes [:useful :automatable :sustainability]]
    (vec (remove (fn [row]
                   (some #(dominates? % row axes) rows))
                 rows))))

(defn rank-by-question
  "Return rows sorted by QUESTION-FN plus the Pareto front of the sweep table."
  [swept question-fn]
  (let [ranked (->> swept
                    (map #(assoc % :question-score (question-fn (:score %))))
                    (sort-by (comp - :question-score))
                    vec)]
    {:ranked ranked
     :pareto-front (pareto-front swept)}))

(defn gap-weight-acceptance
  "Validate the live saturation finding over gap weights [0 2 2.5 4 6].
   Throws if 6.0 is not the least discriminating setting or if 6.0 maximizes
   useful-work or automatable-now."
  [snapshot]
  (let [weights [0 2 2.5 4 6]
        table (sweep snapshot (mapv #(hash-map :gap-weight %) weights))
        by-gap (into {} (map (juxt (comp :gap-weight :weights) identity) table))
        score6 (:score (get by-gap 6))
        min-discrimination (apply min (map (comp :discrimination :score) table))
        max-useful (apply max (map (comp :useful :score) table))
        max-automatable (apply max (map (comp :automatable :score) table))]
    (when-not (= (:discrimination score6) min-discrimination)
      (throw (ex-info "gap-weight 6.0 did not reproduce saturation"
                      {:table table :score-6 score6})))
    (when (or (= (:useful score6) max-useful)
              (= (:automatable score6) max-automatable))
      (throw (ex-info "gap-weight 6.0 still maximized a primary score"
                      {:table table :score-6 score6})))
    table))
