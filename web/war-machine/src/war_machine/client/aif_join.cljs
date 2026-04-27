(ns war-machine.client.aif-join
  "Join session-evidence to AIF+ spine + conflicts.

   Without this, the C-node :bites edges are purely *logical* — they say
   'C1 bites S6 because the cached AIF+ model decided so.'  With this,
   they become *empirical*: a bite is observed when a session step in
   the visible window touches a repo whose devmap leaf maps to a spine
   node listed in that conflict's :bites.

   The chain:

     session step :repos = ['futon3' 'npt' ...]
       └→ leaf 'devmap-futon3'
            └→ spine origin field 'devmap-futon3#nP1' on S7
                 └→ spine-id S7
                      └→ does any conflict bite S7? if yes, that bite
                         just fired empirically.

   Mission-id → spine resolution falls through to the mission's repo —
   the leaf-registry's :mission column is mostly nil today, so we trade
   precision for honesty: missions hit at repo granularity, not at
   leaf-internal n-id granularity."
  (:require [clojure.string :as str]
            [war-machine.client.state :as s]))

;; ---------- Origin parsing ----------

(defn parse-origin
  "Pull leaf names out of a spine node's :origin string.
   Accepts formats like:
     'leaf-argument#n0'
     'leaf-cycle#nRoot = leaf-argument#nCycle'
     'devmap-futon3#nP1, devmap-futon3a#nP0,nP1'
     'leaf-6-4-4#n5,n6,n7'
   Returns a set of leaf-name strings."
  [origin-str]
  (when origin-str
    (->> (str/split origin-str #"\s*=\s*")
         (mapcat (fn [piece]
                   ;; A leaf name is the run of [a-z0-9-]+ that immediately
                   ;; precedes a '#'.  We use a lookahead so we don't grab
                   ;; the n-id tail.
                   (re-seq #"[a-z][a-z0-9-]*(?=#)" piece)))
         set)))

;; ---------- Mappings ----------

(defn leaf->spine-ids
  "From an AIF+ payload's :stack-nodes, build {leaf-name #{spine-id ...}}."
  [stack-nodes]
  (reduce (fn [m node]
            (reduce (fn [m leaf]
                      (update m leaf (fnil conj #{}) (:id node)))
                    m
                    (parse-origin (:origin node))))
          {}
          stack-nodes))

(defn repo->spine-ids
  "Resolve a repo label to the set of spine-ids whose origin leaf is
   named 'devmap-<repo>'.  Returns a (possibly empty) set — an empty
   result is itself empirical evidence: this repo's activity is invisible
   to the cached spine."
  [leaf->spine repo]
  (when repo
    (or (get leaf->spine (str "devmap-" repo)) #{})))

(defn mission-detail-by-id
  "Index war-machine :mission-detail :missions by :mission/id (string)
   for fast :missions[*] resolution."
  [war-machine-data]
  (into {}
        (keep (fn [m]
                (when-let [id (or (get m :mission/id) (get m "mission/id"))]
                  [(name id) m])))
        (get-in war-machine-data [:mission-detail :missions] [])))

(defn step->spine-ids
  "For one session step, return the set of spine-ids it touches.
   Resolves :repos directly and :missions through the touched mission's
   repo (best available without a mission→leaf catalogue)."
  [leaf->spine mid->detail step]
  (let [from-repos    (mapcat #(repo->spine-ids leaf->spine %) (:repos step))
        from-missions (mapcat (fn [mid]
                                (let [k (if (keyword? mid) (name mid) (str mid))
                                      m (get mid->detail k)
                                      r (some-> (or (get m :mission/repo)
                                                    (get m "mission/repo"))
                                                str)]
                                  (repo->spine-ids leaf->spine r)))
                              (:missions step))]
    (set (concat from-repos from-missions))))

;; ---------- Empirical accounting ----------

(defn spine-hits
  "Cumulative hit count per spine-id across all session steps in `data`.
   Returns {spine-id N}."
  [data leaf->spine mid->detail]
  (let [sessions (get-in data [:sessions :sessions] [])]
    (reduce (fn [m s]
              (reduce (fn [m step]
                        (reduce (fn [m sid] (update m sid (fnil inc 0)))
                                m
                                (step->spine-ids leaf->spine mid->detail step)))
                      m
                      (:steps s)))
            {}
            sessions)))

(defn empirical-bites
  "For each conflict, count how many of its :bites have at least one
   recorded spine-hit in the window.  Returns
   {conflict-id {:hit-bites #{...} :total N :hit N}}."
  [conflicts hits-by-spine]
  (into {}
        (map (fn [c]
               (let [bites (set (map (fn [b] (if (keyword? b) (name b) (str b)))
                                     (:bites c)))
                     hit   (set (filter #(pos? (or (get hits-by-spine %) 0))
                                        bites))]
                 [(:id c) {:hit-bites hit
                           :total     (count bites)
                           :hit       (count hit)}])))
        conflicts))

;; ---------- Reagent-friendly accessors ----------
;; These derive everything from the two ratoms s/data and s/aif-data so
;; callers don't have to thread the same maps through every component.

(defn join-context
  "Pre-computed join data, keyed off the current ratoms.  Returns
     {:leaf->spine ...
      :mid->detail ...
      :hits-by-spine ...
      :empirical-bites ...}
   or nil when the AIF+ payload hasn't loaded yet."
  []
  (let [data @s/data
        aif  @s/aif-data]
    (when (and aif (:stack-nodes aif))
      (let [l->s (leaf->spine-ids (:stack-nodes aif))
            mid  (mission-detail-by-id data)
            hits (spine-hits data l->s mid)
            eb   (empirical-bites (:stack-conflicts aif) hits)]
        {:leaf->spine     l->s
         :mid->detail     mid
         :hits-by-spine   hits
         :empirical-bites eb}))))
