(ns futon2.aif.policy-prefix-admission
  "F1b-admit-I (PROOF-2a-PLAN <2>2d F1; F1c-D futon3c 8cc2d425 s4-5; SPEC-F
  s1): the admitted observed prefix of each candidate policy, from the
  conditioning steps the flights wrote (flight/conditioning-step).

  For one candidate policy key, the flight steps are ordered by occurrence
  and walked from the first: a step under another policy key is never
  admitted (:foreign-policy, SPEC-F's 'a prefix joined to another policy'),
  and the rest are admitted in order until the first that cannot be:
    - a step that is not :present ends the prefix with its own reason (a
      refused step, :unmeasured-class, is F1c-D's rule; an absent step
      cannot be chained across);
    - a repeated occurrence ends it (:duplicate-occurrence);
    - a step whose sPrev is not the previous admitted step's q ends it
      (:chain-broken; the first step's sPrev is its boundary belief).
  Pure. The result is recorded; nothing is refused and nothing scores it yet
  (F1c-I consumes it)."
  (:require [clojure.string :as str]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.cascade-prior :as prior]))

(defn reading-policy-key
  "The history identity of a reading policy (cascade-shape-g/arranged->candidate:
  :precedence {:co-apply …} plus :arrangement): [:wm/reading-policy-v1 target
  policy-id {:nodes [[occurrence pattern] …] :edges [[from to kind] …] :order
  [occurrence …]}]. Built from structure only, so theta and learning updates
  leave it unchanged; it names the stable policy id, so the key and the id
  travel together. nil (refused) when the action is not a reading policy, its
  id is not a policy id of its target, its co-apply units, patterns or nodes
  disagree with its arrangement, or it carries a :policy-key other than this."
  [{:keys [target id precedence arrangement] :as action}]
  (let [co (when (map? precedence) (:co-apply precedence))]
    (when (and (map? co) (map? arrangement) (some? target) (string? id)
               (str/starts-with? id (str target "/policy-")))
      (let [nodes (mapv (juxt :occurrence-id :pattern) (:nodes arrangement))
            order (vec (:precedence arrangement))
            key [:wm/reading-policy-v1 (str target) id
                 {:nodes nodes
                  :edges (vec (sort-by pr-str (map (juxt :from :to :kind)
                                                   (:edges arrangement))))
                  :order order}]]
        (when (and (seq nodes)
                   (= order (vec (:units co)))
                   (= (set (map first nodes)) (set (keys (:patterns co))))
                   (every? (fn [[occurrence pattern]]
                             (= pattern (get-in co [:patterns occurrence :pattern-id])))
                           nodes)
                   (or (not (contains? action :policy-key))
                       (= key (:policy-key action))))
          key)))))

(defn candidate-key
  "The candidate's policy history key. A reading policy's is its
  reading-policy-key; an interpreted cascade's is the cascade-prior key
  enactment-habit/policy-key-for uses for an enacted candidate:
  [:pattern-cascade target ordered-pattern-ids {}]. nil when malformed."
  [{:keys [target precedence] :as action}]
  (if (map? precedence)
    (reading-policy-key action)
    (prior/policy-key {:mission target :shown (mapv #(if (map? %) (:id %) %) precedence)
                       :semilattice {}})))

(defn- occurrence-order [s]
  (let [{:keys [click flight]} (get-in s [:step :occurrence])]
    [(str click) (str flight)]))

(defn admit
  "The admitted prefix of POLICY-KEY among STEPS (conditioning-steps' :steps,
  each {:step s :path p :sha256 h}). Returns {:policy-key k :conditioning-status
  :admitted|:no-steps|<the reason the prefix ended> :observation-updates
  [admitted steps in order] :foreign <count> :ended-at {...}}."
  [policy-key steps]
  (let [ordered (sort-by occurrence-order steps)
        ;; a step names its policy (present, refused or absent alike, when the
        ;; key was known); one naming another policy is foreign, one naming
        ;; none cannot be attributed to any prefix
        mine (filter #(= policy-key (get-in % [:step :policy-key])) ordered)
        foreign (count (filter #(let [k (get-in % [:step :policy-key])] (and k (not= policy-key k))) ordered))
        unattributed (count (filter #(nil? (get-in % [:step :policy-key])) ordered))]
    (loop [[s & more] mine admitted [] seen #{} i 0]
      (let [done (fn [status & [ended]]
                   (cond-> {:policy-key policy-key
                            :conditioning-status status
                            :observation-updates admitted
                            :foreign foreign
                            :unattributed unattributed}
                     ended (assoc :ended-at (assoc ended :index i))))]
        (if (nil? s)
          (done (if (seq admitted) :admitted :no-steps))
          (let [step (:step s)
                occ (:occurrence step)
                prev (peek admitted)]
            (cond
              (not= :present (:status step))
              (done (or (:reason step) :step-not-present) {:occurrence occ :status (:status step) :path (:path s)})

              (contains? seen occ)
              (done :duplicate-occurrence {:occurrence occ :path (:path s)})

              (and prev (not= (get-in step [:s-prev :value]) (:q prev)))
              (done :chain-broken {:occurrence occ :path (:path s)})

              :else
              (recur more (conj admitted (assoc step :source {:path (:path s) :sha256 (:sha256 s)}))
                     (conj seen occ) (inc i)))))))))

(defn prefixes
  "For every candidate (the ranked set: {:id :target :precedence}), its
  admitted prefix from FLIGHT-STEPS (conditioning-steps' result), keyed by
  candidate id. With no readable flight records (a missing flights directory,
  or no steps in any record read) every candidate gets the typed absence
  {:conditioning-status :no-flight-records}."
  [candidates flight-steps]
  (let [steps (:steps flight-steps)
        none? (or (:dir-status flight-steps) (empty? steps))]
    (into (sorted-map-by #(compare (str %1) (str %2)))
          (for [c candidates
                :let [k (candidate-key c)]]
            ;; Local constructor labels repeat across targets. Prefix evidence
            ;; is policy-grain, so key it by the complete PolicyKey carried by
            ;; the conditioning steps.
            [k
             (cond
               (nil? k) {:conditioning-status :no-policy-key}
               none? (cond-> {:policy-key k :conditioning-status :no-flight-records}
                       (:dir-status flight-steps) (assoc :dir-status (:dir-status flight-steps)))
               :else (admit k steps))]))))

(defn- census-digest [census]
  (action-identity/sha256
   (action-identity/printed
    false [:wm/policy-history-census-v1
           (action-identity/canonical
            (select-keys census [:dir :dir-status :records :unread
                                 :unattributed-steps]))])))

(defn history-census
  "A pinned census of the flight ledger the prefix F reads (conditioning-steps'
  result): per record its path, sha256, enactment count, step count per
  policy key and unattributed enactments, with a digest over all of it.

  :status :complete only when the directory was read, no record was unread,
  and every enactment is either an explicit absence or a step naming its
  policy. Only a complete census can certify that a policy has no history:
  an unread file or an unattributed enactment could be that policy's."
  [flight-steps]
  (let [steps-by-path (group-by :path (:steps flight-steps))
        records (->> (:read flight-steps)
                     (mapv (fn [{:keys [path sha256 enactments unattributed-enactments]}]
                             {:path path :sha256 sha256
                              :enactments (or enactments :not-counted)
                              :unattributed-enactments (or unattributed-enactments :not-counted)
                              :policy-steps (->> (get steps-by-path path)
                                                 (keep #(get-in % [:step :policy-key]))
                                                 frequencies
                                                 (sort-by (comp pr-str key))
                                                 (mapv vec))}))
                     (sort-by :path)
                     vec)
        unattributed-steps (count (remove #(get-in % [:step :policy-key])
                                          (:steps flight-steps)))
        unread (vec (sort-by :path (:unread flight-steps)))
        reasons (cond-> []
                  (:dir-status flight-steps) (conj :ledger-unread)
                  (nil? (:dir flight-steps)) (conj :ledger-unpinned)
                  (seq unread) (conj :unread-records)
                  (some #(not (integer? (:unattributed-enactments %))) records)
                  (conj :enactments-not-counted)
                  (some #(and (integer? (:unattributed-enactments %))
                              (pos? (:unattributed-enactments %))) records)
                  (conj :unattributed-enactments)
                  (pos? unattributed-steps) (conj :unattributed-steps))
        census (cond-> {:schema :wm/policy-history-census-v1
                        :dir (:dir flight-steps)
                        :records records
                        :unread unread
                        :unattributed-steps unattributed-steps
                        :status (if (seq reasons) :incomplete :complete)}
                 (:dir-status flight-steps) (assoc :dir-status (:dir-status flight-steps))
                 (seq reasons) (assoc :reasons reasons))]
    (assoc census :digest (census-digest census))))

(defn census-history-count
  "How many recorded steps in CENSUS name POLICY-KEY, or nil when the census
  is not complete or its digest does not match its content (then the history
  is unknown, not zero)."
  [census policy-key]
  (when (and (= :wm/policy-history-census-v1 (:schema census))
             (= :complete (:status census))
             (some? policy-key)
             (string? (:dir census))
             (= (:digest census) (census-digest census)))
    (reduce + 0 (for [r (:records census)
                      [k n] (:policy-steps r)
                      :when (= k policy-key)]
                  n))))

(defn census-pin
  "The authoritative pin of CENSUS: what a decision records independently of
  any candidate, and what every zero-history carrier must match exactly."
  [census]
  (select-keys census [:schema :dir :digest :status]))
