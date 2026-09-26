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
  (:require [futon2.aif.cascade-prior :as prior]))

(defn candidate-key
  "The candidate's cascade-prior policy key, the scheme
  enactment-habit/policy-key-for uses for an enacted candidate:
  [:pattern-cascade target ordered-pattern-ids {}]; nil when malformed."
  [{:keys [target precedence]}]
  (prior/policy-key {:mission target :shown (mapv #(if (map? %) (:id %) %) precedence) :semilattice {}}))

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
            [(:id c)
             (cond
               (nil? k) {:conditioning-status :no-policy-key}
               none? (cond-> {:policy-key k :conditioning-status :no-flight-records}
                       (:dir-status flight-steps) (assoc :dir-status (:dir-status flight-steps)))
               :else (admit k steps))]))))
