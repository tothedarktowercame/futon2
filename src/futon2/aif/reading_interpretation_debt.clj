(ns futon2.aif.reading-interpretation-debt
  "Q5: the interpretation a selected reading policy still owes. score-policy
  marks each arrangement pattern the policy's reading has not read as
  :interpretation-owed; this carrier binds that exact set to the policy's
  history key, id, target, arrangement and order, and the reading it came
  from, on the action itself. Selection happens with the debt open; enactment
  waits until every owed node has been validated for this exact identity.

  The carrier is checked against the action it rides on (key, arrangement
  and order are recomputed), against itself (the owed set must be the
  :not-read patterns of its node statuses, and its digest must recompute),
  and against the digest the decision recorded for that policy apart from
  the action. Pure."
  (:require [futon2.aif.action-identity :as action-identity]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.policy-prefix-admission :as prefix-admission]))

(load-identity/register! *ns* *file*)

(def schema :wm/reading-interpretation-debt-v1)

(defn- digest [tag value]
  (action-identity/sha256
   (action-identity/printed false [tag (action-identity/canonical value)])))

(defn arrangement-digest
  "Digest of the action's arrangement: occurrence nodes, edges and order."
  [{:keys [arrangement]}]
  (digest :wm/reading-arrangement-v1
          {:nodes (mapv (juxt :occurrence-id :pattern) (:nodes arrangement))
           :edges (vec (sort-by pr-str (map (juxt :from :to :kind) (:edges arrangement))))
           :order (vec (:precedence arrangement))}))

(defn- arrangement-patterns [action]
  (vec (distinct (map :pattern (get-in action [:arrangement :nodes])))))

(defn- owed-occurrences
  "Every arrangement occurrence whose pattern the reading did not read, in
  arrangement order. Two occurrences of one pattern stay two debts."
  [action node-status]
  (let [not-read (set (keep (fn [[p s]] (when (= :not-read s) p)) node-status))]
    (vec (for [{:keys [occurrence-id pattern]} (get-in action [:arrangement :nodes])
               :when (not-read pattern)]
           {:occurrence-id occurrence-id :pattern pattern}))))

(defn- carrier-digest [carrier]
  (digest schema (dissoc carrier :digest)))

(defn debt
  "The debt carrier for reading policy ACTION from its circumstance FIT
  (score-policy's :fit) and the reading it was scored against
  ({:analysis-digest :excerpt-digest}). nil when the action has no valid
  reading-policy key."
  [action fit {:keys [analysis-digest excerpt-digest]}]
  (when-let [k (prefix-admission/reading-policy-key (dissoc action :interpretation-debt))]
    (let [status (into {} (map (juxt :pattern :status)) (:nodes fit))
          node-status (mapv (fn [p] [p (get status p :missing)]) (arrangement-patterns action))
          carrier {:schema schema
                   :policy-key k
                   :policy-id (:id action)
                   :target (:target action)
                   :arrangement-digest (arrangement-digest action)
                   :order (vec (get-in action [:arrangement :precedence]))
                   :analysis-digest analysis-digest
                   :excerpt-digest excerpt-digest
                   :node-status node-status
                   :owed (owed-occurrences action node-status)
                   :rearm {:condition :all-owed-nodes-validated-for-this-identity
                           :requires [:policy-id :digest :every-owed-pattern]}}]
      (assoc carrier :digest (carrier-digest carrier)))))

(defn check
  "Validate the debt carried by reading policy ACTION. {:status :valid :owed
  […] :digest d} or {:status :invalid :reason r}."
  [action]
  (let [carrier (:interpretation-debt action)
        k (prefix-admission/reading-policy-key (dissoc action :interpretation-debt))
        patterns (arrangement-patterns action)
        statuses (:node-status carrier)
        invalid (fn [reason] {:status :invalid :reason reason})]
    (cond
      (not (map? carrier)) (invalid :debt-carrier-missing)
      (not= schema (:schema carrier)) (invalid :debt-schema)
      (nil? k) (invalid :reading-policy-key-refused)
      (not= k (:policy-key carrier)) (invalid :debt-policy-key-mismatch)
      (not= (:id action) (:policy-id carrier)) (invalid :debt-policy-id-mismatch)
      (not= (:target action) (:target carrier)) (invalid :debt-target-mismatch)
      (not= (arrangement-digest action) (:arrangement-digest carrier))
      (invalid :debt-arrangement-mismatch)
      (not= (vec (get-in action [:arrangement :precedence])) (:order carrier))
      (invalid :debt-order-mismatch)
      (not= patterns (mapv first statuses)) (invalid :debt-node-coverage)
      (some #(not (#{:accepted :rejected :not-read} (second %))) statuses)
      (invalid :debt-node-status-unknown)
      (not= (:owed carrier) (owed-occurrences action statuses))
      (invalid :debt-owed-set-mismatch)
      (not= (:digest carrier) (carrier-digest carrier)) (invalid :debt-digest-mismatch)
      :else {:status :valid :owed (:owed carrier) :digest (:digest carrier)})))

(defn enactment-status
  "Whether reading policy ACTION may be enacted. RECORDED-DIGEST is the debt
  digest the decision recorded for this policy apart from the action;
  RECEIPTS are post-selection interpretation receipts
  ({:policy-id :debt-digest :occurrence-id :pattern :status :validated}).

  :discharged only when the carrier is valid, matches the recorded digest,
  and every owed occurrence has a validated receipt for this exact policy,
  debt digest, occurrence and pattern (an empty owed set is discharged as it
  stands). A receipt for another policy or debt, or for an occurrence the
  debt does not owe, is :invalid; any owed occurrence without one is
  :interpretation-owed-after-selection, carrying the re-arm condition."
  [action recorded-digest receipts]
  (let [{:keys [status owed digest reason]} (check action)]
    (cond
      (not= :valid status) {:status :invalid :reason reason}
      (not= recorded-digest digest) {:status :invalid :reason :debt-not-recorded-by-decision}
      (some #(not (and (= (:id action) (:policy-id %)) (= digest (:debt-digest %))))
            receipts)
      {:status :invalid :reason :foreign-interpretation-receipt}
      (some #(not (contains? (set owed) (select-keys % [:occurrence-id :pattern]))) receipts)
      {:status :invalid :reason :receipt-for-unowed-occurrence}
      :else
      (let [validated (set (keep #(when (= :validated (:status %))
                                    (select-keys % [:occurrence-id :pattern]))
                                 receipts))
            remaining (vec (remove validated owed))]
        (if (empty? remaining)
          {:status :discharged :digest digest}
          {:status :interpretation-owed-after-selection
           :policy-id (:id action) :target (:target action) :debt-digest digest
           :owed owed :remaining remaining
           :rearm (get-in action [:interpretation-debt :rearm])})))))
