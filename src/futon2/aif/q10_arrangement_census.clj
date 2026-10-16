(ns futon2.aif.q10-arrangement-census
  "Requirements Q10 on the reading-family route. Q10 counts compared cascade
  pairs over the same pattern set in different arrangements
  (differentArrangementPairs) and those represented as distinct policies,
  each with its own numeric G (arrangementPairsDistinguishedByG); it holds
  when there is at least one such pair and every one is distinguished.

  The population is the selected task's construction as the verified Q2
  carrier records it: every structurally keyed policy, under the reading,
  graph, library and kernel pins Q2 retains. Each policy's stage comes
  from the decision's :policy-stages and Q6 carrier, as q8-frontier/cascade
  reads them, and the compared set is the selection-law posterior. A
  certified Q8 carrier must hold the same stages. Every member is
  :compared, :requirement-excluded (Q6, with the Q6 carrier's reason),
  :not-scored (with the scorer's recorded failure) or :not-keyed (Q2's
  :policy-arrangement-not-keyable failure). A constructed policy whose
  absence from the comparison no record explains is a hidden refusal, and
  the census is not recomputable.

  Every unordered pair of keyed members with the same pattern set is
  enumerated. A pair whose arrangements are equal is refused
  (:arrangement-equal); a pair with a member that was not compared is
  refused (:member-not-compared, with that member's reason). The rest are
  the compared controls Q10 counts. A control is distinguished when both
  members carry their own numeric G: one candidate row each, bound to the
  policy's key, finite, and equal to the G its family summary reports.
  Whether the two G values differ is recorded, not required: the Lean
  docstring asks for distinct policies with their own G.

  An arrangement is compared by pattern when every pattern occurs once in
  the policy (occurrence names renamed to their patterns, so two spellings
  of one arrangement are equal), and by occurrence otherwise, which the
  pair records.

  The runner seals the carrier into the record; `verify-record` recomputes
  it from the record alone and the exporter reads only a verified carrier."
  (:require [clojure.set :as set]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.q2-construction :as q2]
            [futon2.aif.q8-frontier :as q8]))

(def schema :wm/q10-arrangement-census-v1)
(def seal previous-run/seal)
(def intact? previous-run/intact?)
(def digest evidence/value-digest)

;; -- one policy's patterns and arrangement --------------------------------------

(defn- keyed? [k] (and (vector? k) (= :wm/reading-policy-v1 (first k))))

(defn pattern-set
  "The set of patterns policy key K arranges, sorted."
  [k]
  (vec (sort (distinct (map second (:nodes (nth k 3)))))))

(defn arrangement
  "Policy key K's arrangement: by pattern when each pattern occurs once in
  it (its occurrences renamed to their patterns), else by occurrence."
  [k]
  (let [{:keys [nodes edges order]} (nth k 3)
        patterns (map second nodes)]
    (if (= (count patterns) (count (distinct patterns)))
      (let [p (into {} nodes)]
        {:by :pattern
         :edges (vec (sort-by pr-str (map (fn [[from to kind]] [(p from) (p to) kind]) edges)))
         :order (mapv p order)})
      {:by :occurrence :nodes nodes :edges edges :order order})))

;; -- the population ---------------------------------------------------------------

(defn- g-rows
  "Candidate rows of the decision's certificate, by policy key."
  [cert]
  (group-by #(prefix-admission/candidate-key (:id %)) (:candidates cert)))

(defn- summary-g
  "The G the selected task's family summary reports for each policy id."
  [cert task]
  (into {} (for [f (:target-policy-families cert)
                 :when (= (str task) (str (:target-id f)))
                 p (:policies f)]
             [(:policy-id p) (:G p)])))

(defn- own-g
  "Policy K's own numeric G, or {:refusal kind}."
  [k rows summaries]
  (let [rs (get rows k)
        g (:g (first rs))]
    (cond
      (empty? rs) {:refusal :compared-without-g-row}
      (< 1 (count rs)) {:refusal :g-row-shared}
      (not (and (number? g) (Double/isFinite (double g)))) {:refusal :g-not-numeric}
      (not= g (get summaries (nth k 2))) {:refusal :g-not-the-family-summary's}
      :else {:g g})))

(defn- member
  "Constructed policy K's stage, or {:hidden k} when nothing explains it."
  [k {:keys [scored requirement-excluded posterior]} q6-reason failures unkeyable]
  (let [id (nth k 2 nil)]
    (cond
      (not (keyed? k))
      (if (contains? unkeyable id)
        {:key k :status :not-keyed :reason :policy-arrangement-not-keyable}
        {:hidden k})
      (contains? posterior k) {:key k :status :compared}
      (contains? requirement-excluded k)
      {:key k :status :requirement-excluded :reason q6-reason}
      (and (not (contains? scored k)) (contains? failures id))
      {:key k :status :not-scored :reason (get failures id)}
      ;; scored or admitted, yet neither compared nor excluded by Q6
      :else {:hidden k})))

(defn- pairs
  "Every unordered pair of keyed MEMBERS over the same pattern set, typed."
  [members]
  (let [keyed (vec (sort-by (comp pr-str :key) (filter #(keyed? (:key %)) members)))]
    (vec
     (for [[i a] (map-indexed vector keyed)
           b (subvec keyed (inc i))
           :when (= (:patterns a) (:patterns b))
           :let [same? (= (:arrangement a) (:arrangement b))
                 off (remove #(= :compared (:status %)) [a b])
                 gs (map :g [a b])
                 base {:members [(:key a) (:key b)]
                       :patterns (:patterns a)
                       :compared-by (if (= :pattern (get-in a [:arrangement :by])
                                           (get-in b [:arrangement :by]))
                                      :pattern :occurrence)}]]
       (cond
         same? (assoc base :status :refused :reason :arrangement-equal)
         (seq off) (assoc base :status :refused :reason :member-not-compared
                          :members-not-compared
                          (mapv #(select-keys % [:key :status :reason]) off))
         (every? some? gs)
         (assoc base :status :control :distinguished true :g (vec gs)
                :g-differs (not= (first gs) (second gs)))
         :else
         (assoc base :status :control :distinguished false
                :reason (some :g-refusal [a b])))))))

;; -- the carrier ------------------------------------------------------------------

(def ^:private q8-unbound
  #{:q8-carrier-absent :q8-carrier-tampered :q8-carrier-not-bound-to-record})

(defn- q8-binding
  "STAGES against the record's Q8 carrier: nil when a certified carrier
  holds them, or the carrier is a bound, typed not-recomputable one; else
  the refusal."
  [record stages]
  (let [v (q8/verify-record record)]
    (cond
      (= :verified (:status v))
      (when-not (= (select-keys stages [:constructed :scored :admitted
                                        :requirement-excluded :posterior])
                   (select-keys (get-in v [:carrier :cascade])
                                [:constructed :scored :admitted
                                 :requirement-excluded :posterior]))
        :q8-stages-not-the-census')
      (q8-unbound (:reason v)) (:reason v)
      :else nil)))

(defn certificate
  "The sealed Q10 census from a persisted RECORD (its decision, :q2, :q8 and
  Q6 predecessor)."
  [record]
  (let [decision (:decision record)
        cert (:selection-certificate decision)
        task (get-in decision [:chosen :target])
        q2v (q2/verify-record record)
        entry (when (= :verified (:status q2v))
                (let [es (filter #(= (str task) (:task %)) (get-in q2v [:carrier :entries]))]
                  (when (= 1 (count es)) (first es))))
        raw-stages (get-in cert [:policy-stages task])
        c (q8/cascade decision (get-in record [:q6 :predecessor]) task)
        posterior-map (get-in decision [:selection-law :posterior])
        compared (set (map prefix-admission/candidate-key (keys posterior-map)))
        selected (some-> (get-in decision [:selection-law :per-policy-argmax :action])
                         prefix-admission/candidate-key)
        constructed (:constructed raw-stages)
        failures (into {} (for [f (:family-failures cert) :when (:policy-id f)]
                            [(:policy-id f) (:kind f)]))
        unkeyable (set (for [f (:failures entry)
                             :when (= :policy-arrangement-not-keyable (:kind f))]
                         (:policy-id f)))
        rows (g-rows cert)
        summaries (summary-g cert task)
        base {:schema schema
              :run/id (:run/id record)
              :route (:selection-route cert)
              :selected-task task
              :selected selected
              :pins (when entry
                      (assoc (select-keys entry [:reading-id :graph-id :library-id :kernel-id])
                             :q2 (get-in q2v [:carrier :sha256])))
              :bindings {:stages (digest raw-stages)
                         :q6 (get-in cert [:q6 :sha256])
                         :q8 (get-in record [:q8 :sha256])
                         :posterior (digest (vec (sort-by pr-str compared)))
                         :candidates (digest (:candidates cert))
                         :families (digest (:target-policy-families cert))}}
        not-recomputable #(seal (assoc base :status :not-recomputable :reason %))]
    (cond
      (not= :wm/reading-policy-family-v1 (:selection-route cert))
      (not-recomputable :not-the-reading-family-route)
      (not= :verified (:status q2v)) (not-recomputable (:reason q2v))
      (nil? entry) (not-recomputable :no-q2-construction-for-the-selected-task)
      (not= :present (:status c)) (not-recomputable (:reason c))
      (q8-binding record c) (not-recomputable (q8-binding record c))
      (not= (count constructed) (count (set constructed)))
      (not-recomputable :constructed-policy-duplicated)
      (not= (count posterior-map) (count compared))
      (not-recomputable :compared-policy-duplicated)
      (not= (set (:policies entry)) (set (filter keyed? constructed)))
      (not-recomputable :constructed-population-not-q2's)
      (not= compared (:posterior c)) (not-recomputable :compared-not-the-stages-posterior)
      (not (set/subset? compared (set constructed))) (not-recomputable :foreign-compared-policy)
      (not= compared (set/difference (:scored c) (:requirement-excluded c)))
      (not-recomputable :compared-not-scored-minus-q6-excluded)
      (and selected (not (contains? compared selected)))
      (not-recomputable :selected-policy-not-compared)
      :else
      (let [q6-reason (get-in cert [:q6 :reason])
            members (mapv (fn [k]
                            (let [m (member k c q6-reason failures unkeyable)]
                              (if (or (:hidden m) (not (keyed? k)))
                                m
                                (let [g (when (= :compared (:status m)) (own-g k rows summaries))]
                                  (cond-> (assoc m :patterns (pattern-set k)
                                                 :arrangement (arrangement k))
                                    (:g g) (assoc :g (:g g))
                                    (:refusal g) (assoc :g-refusal (:refusal g)))))))
                          (sort-by pr-str constructed))
            hidden (keep :hidden members)
            ps (pairs members)
            controls (filter #(= :control (:status %)) ps)]
        (if (seq hidden)
          (seal (assoc base :status :not-recomputable :reason :hidden-refusal
                       :hidden (vec hidden)))
          (seal (assoc base :status :counted
                       :population (mapv #(dissoc % :g-refusal) members)
                       :pairs ps
                       :selected-in-control (boolean (some #(some #{selected} (:members %))
                                                           controls))
                       :counts {:different-arrangement-pairs (count controls)
                                :distinguished-by-g (count (filter :distinguished controls))})))))))

(defn verify-record
  "The record's own :q10 carrier when it is intact, counted and equal to the
  census recomputed from the record; else a typed not-recomputable reason."
  [record]
  (let [q10 (:q10 record)]
    (cond
      (nil? q10) {:status :not-recomputable :reason :q10-carrier-absent}
      (not (and (= schema (:schema q10)) (intact? q10)))
      {:status :not-recomputable :reason :q10-carrier-tampered}
      (not= q10 (certificate (dissoc record :q10)))
      {:status :not-recomputable :reason :q10-carrier-not-bound-to-record}
      (not= :counted (:status q10))
      {:status :not-recomputable :reason (:reason q10)}
      :else {:status :verified :carrier q10})))

;; -- a mirror of Requirements.Q10, for tests --------------------------------------

(defn lean-q10
  "Requirements.Q10 over a verified carrier's counts."
  [carrier]
  (let [{:keys [different-arrangement-pairs distinguished-by-g]} (:counts carrier)]
    (and (pos? different-arrangement-pairs)
         (= distinguished-by-g different-arrangement-pairs))))
