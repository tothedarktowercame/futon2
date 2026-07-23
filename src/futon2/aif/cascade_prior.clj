(ns futon2.aif.cascade-prior
  "A categorical habit prior over complete pattern-cascade policies.

   This is the cascade-grain counterpart of `futon2.aif.habit-prior`.  A policy
   identity includes the selected mission, the ordered pattern construction,
   and the semilattice wiring.  Consequently two cascades containing the same
   patterns but connecting them differently are different policies.

   The namespace is deliberately pure and DARK: it does not construct,
   select, persist, or enact a cascade.  Live use is valid only after the War
   Machine exposes two or more admissible cascades for the same selected
   mission and supplies an honest cascade-level score."
  (:require [clojure.walk :as walk]
            [futon2.aif.policy :as policy]))

(def default-alpha 1.0)
(def state-version 1)

(defn- canonical-form
  "Recursively make unordered EDN collections stable for policy identity.
   Vectors remain ordered because cascade construction order is meaningful."
  [value]
  (walk/postwalk
   (fn [x]
     (cond
       (map? x) (into (sorted-map-by #(compare (pr-str %1) (pr-str %2))) x)
       (set? x) (vec (sort-by pr-str x))
       :else x))
   value))

(defn- canonical-semilattice
  [semilattice]
  (cond
    (map? semilattice)
    (canonical-form
     (into {}
           (map (fn [[edge-type edges]]
                  [edge-type (if (sequential? edges)
                               (vec (sort-by pr-str (map canonical-form edges)))
                               (canonical-form edges))]))
           semilattice))

    (sequential? semilattice)
    (vec (sort-by pr-str (map canonical-form semilattice)))

    :else nil))

(defn policy-key
  "Stable identity for a complete cascade policy, or nil when it is malformed.

   Volatile scores, prose, ranks, and construction telemetry are excluded.
   Semilattice edge order is ignored, but pattern order in `:shown` is kept."
  [cascade]
  (let [mission (:mission cascade)
        shown (:shown cascade)
        semilattice (:semilattice cascade)]
    (when (and (map? cascade)
               (some? mission)
               (vector? shown)
               (seq shown)
               (every? string? shown)
               (or (map? semilattice) (sequential? semilattice)))
      [:pattern-cascade
       (str mission)
       shown
       (canonical-semilattice semilattice)])))

(defn initial-state
  ([] (initial-state {}))
  ([{:keys [alpha] :or {alpha default-alpha}}]
   (let [a (double alpha)]
     (when-not (and (Double/isFinite a) (pos? a))
       (throw (ex-info "cascade-prior alpha must be positive" {:alpha alpha})))
     {:version state-version
      :policy-grain :pattern-cascade
      :alpha a
      :counts {}
      :samples 0
      :recency-decay :none})))

(defn coerce-state
  "Accept a persisted v1 cascade-prior state. Nil is a cold start; malformed
   present state fails closed rather than silently losing history."
  [state]
  (cond
    (nil? state) (initial-state)
    (and (= state-version (:version state))
         (= :pattern-cascade (:policy-grain state))
         (number? (:alpha state))
         (Double/isFinite (double (:alpha state)))
         (pos? (double (:alpha state)))
         (map? (:counts state))
         (every? (fn [[k n]]
                   (and (vector? k) (= :pattern-cascade (first k))
                        (integer? n) (not (neg? n))))
                 (:counts state))
         (integer? (:samples state))
         (not (neg? (:samples state)))
         (= (:samples state) (reduce + 0 (vals (:counts state)))))
    state
    :else
    (throw (ex-info "malformed persisted cascade-prior state" {:state state}))))

(defn observe-policy
  "Fold one selected, constructible cascade into the habit state. Malformed
   candidates are reduction-safe no-ops; callers decide which lifecycle event
   is warranted to count."
  [state cascade]
  (let [state (coerce-state state)]
    (if-let [key (policy-key cascade)]
      (-> state
          (update-in [:counts key] (fnil inc 0))
          (update :samples inc))
      state)))

(defn state-stats
  [state]
  (let [{:keys [alpha counts samples recency-decay]} (coerce-state state)]
    {:policy-grain :pattern-cascade
     :class-count (count counts)
     :samples samples
     :alpha alpha
     :recency-decay recency-decay}))

(defn log-priors
  "Return ln E(pi) aligned with a same-mission feasible cascade menu.

   Historical counts provide the Dirichlet concentrations. Duplicate policy
   identities split their category mass, keeping the returned probabilities
   normalized over the concrete menu. Mixed-mission menus fail closed: the
   strategic mission choice and tactical cascade choice are separate levels."
  [state cascades]
  (let [{:keys [alpha counts]} (coerce-state state)
        policy-keys (mapv policy-key cascades)
        _ (when (some nil? policy-keys)
            (throw (ex-info "cascade-prior candidate has no stable policy identity"
                            {:cascades cascades})))
        missions (set (map second policy-keys))
        _ (when (> (count missions) 1)
            (throw (ex-info "cascade-prior menu mixes strategic missions"
                            {:missions missions})))
        multiplicities (frequencies policy-keys)
        denominator (reduce + 0.0
                            (map #(+ (double (get counts % 0)) alpha)
                                 (keys multiplicities)))]
    (mapv (fn [key]
            (Math/log (/ (/ (+ (double (get counts key 0)) alpha)
                              (double (get multiplicities key)))
                         denominator)))
          policy-keys)))

(defn attach-log-priors
  "Attach cascade-grain ln E(pi) without selecting or changing a score."
  [state cascades]
  (mapv (fn [cascade log-prior]
          (assoc cascade
                 :cascade-prior-bias log-prior
                 :cascade-prior-source :learned-frequency
                 :policy-grain :pattern-cascade))
        cascades
        (log-priors state cascades)))

(defn shadow-rank
  "Rank a non-degenerate same-mission cascade menu in DARK mode.

   `:cascade-score` is the existing higher-is-better engineering score, so its
   lower-is-better cost at the shared softmax seam is its negation. The result
   therefore implements exp(ln E_cascade + cascade-score/tau) while preserving
   the score's honest non-EFE name. This function has no persistence or live
   selection effects."
  ([state cascades] (shadow-rank state cascades {}))
  ([state cascades {:keys [tau] :or {tau 1.0}}]
   (let [temperature (double tau)
         _ (when-not (and (Double/isFinite temperature) (pos? temperature))
             (throw (ex-info "cascade shadow temperature must be positive"
                             {:tau tau})))
         _ (when (< (count cascades) 2)
             (throw (ex-info "cascade shadow selection requires at least two policies"
                             {:candidate-count (count cascades)})))
         attached (attach-log-priors state cascades)
         scores (mapv :cascade-score attached)
         _ (when-not (every? #(and (number? %)
                                   (Double/isFinite (double %)))
                             scores)
             (throw (ex-info "cascade candidate lacks a finite engineering score"
                             {:scores scores})))
         costs (mapv #(- (double %)) scores)
         log-e (mapv :cascade-prior-bias attached)
         weights (policy/softmax-weights costs temperature log-e)
         ranked (->> (mapv (fn [candidate cost weight]
                             (assoc candidate
                                    :cascade-cost cost
                                    :cascade-selection-weight weight
                                    :cascade-selection-score
                                    (+ (/ (- cost) temperature)
                                       (:cascade-prior-bias candidate))))
                           attached costs weights)
                     (sort-by (juxt (comp - :cascade-selection-weight)
                                    (comp pr-str policy-key)))
                     vec)
         winner (first ranked)
         score-winner (apply max-key :cascade-score attached)]
     {:status :dark-cascade-ranking
      :policy-grain :pattern-cascade
      :tau temperature
      :candidate-count (count ranked)
      :ranked-candidates ranked
      :shadow-winner winner
      :score-only-winner score-winner
      :governed-by (if (= (policy-key winner) (policy-key score-winner))
                     :cascade-score
                     :cascade-habit)})))
