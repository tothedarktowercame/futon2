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
  (:require [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.policy-prefix-admission :as prefix-admission]))

(load-identity/register! *ns* *file*)

(def schema :wm/reading-interpretation-debt-v1)
(def request-schema :wm/selected-reading-request-v1)
(def receipt-schema :wm/selected-reading-receipt-v1)
(def validator-id :wm/selected-reading-validator-v1)

(defn seal
  "The digest of VALUE under TAG by the repository's evidence discipline
  (interpretation-evidence/value-digest, the precision-carry seal's): the
  seal of debts, selected-reading requests and receipts. Recomputable by
  anyone holding the value; it detects edits, not authorship."
  [tag value]
  (evidence/value-digest [tag value]))

(def ^:private digest seal)

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

(defn request-intact?
  "True iff REQUEST's :request-id is the seal of the rest of it."
  [request]
  (and (map? request) (= request-schema (:schema request))
       (= (:request-id request) (seal request-schema (dissoc request :request-id)))))

(defn receipt-problem
  "Why RECEIPT does not discharge an occurrence of ACTION's debt under the
  issued REQUEST, or nil. A receipt stands only as the selected-reading
  validator sealed it: schema, validator and result, intact seal, this
  policy's id, key digest and debt digest, an owed occurrence, the intact
  request it answers (same policy and debt), the debt's excerpt and analysis
  digests, a named source, and the library pin the request issued."
  [action request receipt]
  (let [carrier (:interpretation-debt action)
        pattern (:pattern receipt)
        reading (:reading receipt)]
    (cond
      (not (map? receipt)) :receipt-malformed
      (not= receipt-schema (:schema receipt)) :receipt-unsealed
      (not= {:validator validator-id :result :valid} (:validation receipt))
      :receipt-not-validated
      (not= (:digest receipt) (seal receipt-schema (dissoc receipt :digest)))
      :receipt-seal-broken
      (not= :validated (:status receipt)) :receipt-not-validated
      (not (and (= (:id action) (:policy-id receipt))
                (= (:digest carrier) (:debt-digest receipt))))
      :foreign-interpretation-receipt
      (not= (seal :wm/reading-policy-key (:policy-key action)) (:policy-key-digest receipt))
      :receipt-policy-key-mismatch
      (not (contains? (set (:owed carrier)) (select-keys receipt [:occurrence-id :pattern])))
      :receipt-for-unowed-occurrence
      (not (request-intact? request)) :receipt-request-missing-or-broken
      (not (and (= (:request-id request) (:request-id reading))
                (= (:id action) (:policy-id request))
                (= (:digest carrier) (:debt-digest request))))
      :receipt-for-another-request
      (not (and (= (:excerpt-digest carrier) (:excerpt-digest reading))
                (= (:analysis-digest carrier) (:analysis-digest reading))))
      :receipt-reading-mismatch
      (not (and (string? (:by reading)) (seq (str/trim (:by reading)))))
      :receipt-without-source-identity
      (not (and (some? (get-in request [:library-pins pattern]))
                (= (get-in request [:library-pins pattern]) (:library-pin receipt))
                (= (:library-pin receipt) (:source-sha256 receipt))))
      :receipt-library-pin-mismatch
      :else nil)))

(defn enactment-status
  "Whether reading policy ACTION may be enacted. RECORDED-DIGEST is the debt
  digest the decision recorded for this policy apart from the action;
  RECEIPTS are the sealed selected-reading receipts and REQUEST the issued
  request they answer (receipt-problem says what each must be).

  :discharged only when the carrier is valid, matches the recorded digest,
  and every owed occurrence has a validated receipt for this exact policy,
  debt digest, occurrence and pattern (an empty owed set is discharged as it
  stands). A receipt for another policy or debt, or for an occurrence the
  debt does not owe, is :invalid; any owed occurrence without one is
  :interpretation-owed-after-selection, carrying the re-arm condition."
  [action recorded-digest receipts request]
  (let [{:keys [status owed digest reason]} (check action)]
    (cond
      (not= :valid status) {:status :invalid :reason reason}
      (not= recorded-digest digest) {:status :invalid :reason :debt-not-recorded-by-decision}
      (some #(receipt-problem action request %) receipts)
      {:status :invalid :reason (some #(receipt-problem action request %) receipts)}
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
