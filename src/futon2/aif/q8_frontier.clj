(ns futon2.aif.q8-frontier
  "Requirements Q8 on the production route (codex-68 rulings 1a, 2b and 3,
  2026-10-15).

  Outer frontier (ruling 2b). meta-live-outer-selector/select-live calls
  `outer-frontier` with the exact candidates and selection it used. Every
  open task is either eligible (a candidate the selector ranked) or excluded
  with the typed reason the selector recorded; excluded tasks get no key.
  The ranking law is the :complete-union pairwise comparison. For candidates
  a and b over Ca ∪ Cb, g(a) − g(b) = key(a) − key(b) with
  key(a) = Σ_{c∈Ca}(a_c − m), m the missing-channel cost, because the
  channels each lacks add m to both sides. The key is summed exactly
  (BigDecimal of each double) and encoded as the natural number
  (key + 2^64)·2^1074, a fixed monotone injection on dyadic rationals; no
  population ordinal is used. Every recorded pairwise winner is checked
  against the keys; a disagreement is not recomputable.

  Refresh (ruling 3) is an exactness audit of a full recomputation against
  the prior run's persisted, sealed frontier: tasks are compared by
  identity and dependency digest and classified retained, invalidated,
  removed or added. It does not feed the selector and is not learning.

  Selected cascade (ruling 1a). Truthful stages for the selected task:
  constructed (every policy of its family), admitted (certified for the
  selector), scored (computed G), the Q6 requirement exclusion, and the
  posterior. An exclusion is accepted only from the decision's intact Q6
  carrier, bound to the run's predecessor and justified by it.

  `certificate` is computed by the runner from what it persists, and
  `verify-record` recomputes it from the persisted record alone; the
  exporter accepts the carrier only when the two agree."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.aif.previous-run :as previous-run])
  (:import [java.math BigDecimal BigInteger]))

(def outer-schema :wm/q8-outer-frontier-v1)
(def frontier-schema :wm/q8-frontier-v1)
(def certificate-schema :wm/q8-certificate-v1)
(def ranking-law :meta-pipeline/complete-union-channel-sum-v1)
(def ranking-version 1)

(def seal previous-run/seal)
(def intact? previous-run/intact?)
(def digest evidence/value-digest)

;; -- the comparison key -------------------------------------------------------

(def ^:private scale (BigDecimal. (.shiftLeft BigInteger/ONE 1074)))
(def ^:private offset (BigDecimal. (.shiftLeft BigInteger/ONE 64)))

(defn- exact [x]
  (cond
    (instance? Double x) (when (Double/isFinite x) (BigDecimal. (double (+ 0.0 x))))
    (integer? x) (BigDecimal. (biginteger x))
    :else nil))

(defn channel-values
  "{channel value} of a candidate's channel observations."
  [candidate]
  (into (sorted-map) (map (fn [[c o]] [c (:value o)])) (:channels candidate)))

(defn comparison-key
  "The exact natural-number key of CHANNELS ({channel value}) at missing-
  channel cost M, or nil when a value is not an exact finite number or the
  sum is outside (−2^64, ∞)."
  [channels m]
  (let [m' (exact m)
        terms (map (comp exact val) (sort-by key channels))]
    (when (and m' (every? some? terms))
      (let [k (reduce (fn [^BigDecimal a ^BigDecimal b] (.add a (.subtract b m')))
                      BigDecimal/ZERO terms)
            shifted (.add ^BigDecimal k ^BigDecimal offset)]
        (when (pos? (.signum shifted))
          (bigint (.toBigIntegerExact (.multiply shifted ^BigDecimal scale))))))))

(defn pairwise-disagreement
  "The first recorded pair whose winner disagrees with KEYS ({task key}),
  or a typed incompleteness, or nil when every unordered pair of eligible
  tasks is recorded once and agrees."
  [pairs key-of]
  (let [ids (set (keys key-of))
        expected (set (for [a ids b ids :when (neg? (compare a b))] #{a b}))
        seen (map #(hash-set (:left %) (:right %)) pairs)]
    (cond
      (not= (count seen) (count (set seen)))
      {:kind :pairwise-duplicated}
      (not= expected (set seen))
      {:kind :pairwise-incomplete
       :missing (count (set/difference expected (set seen)))
       :foreign (count (set/difference (set seen) expected))}
      :else
      (first (for [{:keys [left right winner comparison] :as p} pairs
                   :let [kl (get key-of left) kr (get key-of right)
                         by-key (cond (< kl kr) left (> kl kr) right :else :tie)]
                   :when (or (not= :complete-union comparison) (not= winner by-key))]
               {:kind :pairwise-key-order-mismatch
                :pair (select-keys p [:left :right :winner :g-left :g-right :comparison])
                :by-key by-key})))))

;; -- the outer frontier, at selection -----------------------------------------

(defn- entry [candidate m]
  (let [id (:id candidate)
        channels (channel-values candidate)
        evidence-digest (digest (select-keys candidate [:id :kind :support :channels]))]
    {:task id
     :channels channels
     :observation (digest channels)
     :evidence evidence-digest
     :ranking-law ranking-law
     :ranking-version ranking-version
     :dependencies (vec (sort [(digest [:wm/q8-task-observation id evidence-digest])
                               (digest [:wm/q8-ranking-law ranking-law ranking-version m])]))
     :comparison-key (comparison-key channels m)
     :absent-terms (vec (sort (:unsupported-channels candidate)))}))

(defn strict-selection-correspondence
  "The single runtime/Lean correspondence boundary for War Machine selection.
  It mirrors SelectionCertificate.Valid: a selected member must have a
  strictly smaller comparison key than every distinct frontier member.  A
  tied minimum is a decided failure and carries the tied members and the
  terms absent from their observations."
  [entries selected]
  (if-let [selected-entry (first (filter #(= selected (:task %)) entries))]
    (let [k (:comparison-key selected-entry)
          tied (vec (sort (map :task (filter #(and (not= selected (:task %))
                                                    (= k (:comparison-key %))) entries))))
          lower (vec (sort (map :task (filter #(< (:comparison-key %) k) entries))))]
      (cond
      (seq lower) {:status :fail :reason :selected-not-strict-key-minimum
                   :lower-members lower}
      (seq tied) {:status :fail :reason :tied-minimum
                  :tied-members (vec (sort (conj tied selected)))
                  :absent-terms
                  (vec (sort (set (mapcat :absent-terms
                                          (filter #(some #{(:task %)} (conj tied selected))
                                                  entries)))))}
        :else {:status :pass}))
    {:status :not-recomputable :reason :selected-entry-absent}))

(defn outer-frontier
  "Sealed outer frontier for one outer selection. TASKS is the open
  population the selector received, EXCLUDED its typed exclusions
  ({:id :ineligible-reason}), CANDIDATES the exact candidates the effective
  selection compared, SELECTION that selection, COMPARISON and M its law."
  [{:keys [tasks excluded candidates selection comparison missing-channel-cost]}]
  (let [infeasible (filter #(= :infeasible (get-in % [:support :automated-feasibility]))
                           candidates)
        eligible (remove (set infeasible) candidates)
        census {:open (vec (sort (map (comp str :id) tasks)))
                :eligible (vec (sort (map (comp str :id) eligible)))
                :excluded (vec (sort-by :task
                                        (concat
                                         (map (fn [x] {:task (str (:id x))
                                                       :reason (:ineligible-reason x)})
                                              excluded)
                                         (map (fn [c] {:task (str (:id c))
                                                       :reason :automated-feasibility/infeasible})
                                              infeasible))))}
        entries (vec (sort-by :task (map #(entry % missing-channel-cost) eligible)))
        ks (into {} (map (juxt :task :comparison-key)) entries)
        selected (:selected selection)
        base {:schema outer-schema
              :ranking {:law ranking-law :version ranking-version
                        :comparison comparison :missing-channel-cost missing-channel-cost}
              :census census
              :selected selected}
        selection-verdict (when (and (= :selected (:status selection))
                                     (= :complete-union comparison)
                                     (every? (comp some? :comparison-key) entries))
                            (strict-selection-correspondence entries selected))
        refusal
        (cond
          (not= :selected (:status selection))
          {:reason :no-outer-selection :selection-reason (:reason selection)}
          (not= :complete-union comparison)
          {:reason :ranking-law-not-scalar :comparison comparison}
          (some (comp nil? :comparison-key) entries)
          {:reason :channel-value-not-exact
           :tasks (vec (keep #(when (nil? (:comparison-key %)) (:task %)) entries))}
          (= :not-recomputable (:status selection-verdict)) selection-verdict
          (and (= :fail (:status selection-verdict))
               (not= :tied-minimum (:reason selection-verdict))) selection-verdict
          (= :tied-minimum (:reason selection-verdict)) nil
          :else
          (if-let [d (pairwise-disagreement (:pairwise selection) ks)]
            (assoc d :reason (:kind d))
            nil))]
    (seal (if refusal
            (merge base {:status :not-recomputable} (dissoc refusal :kind))
            (assoc base
                   :status (if (= :fail (:status selection-verdict))
                             :decided-failure :certified)
                   :selection-verdict selection-verdict
                   :entries entries
                   :pairwise {:pairs (count (:pairwise selection))
                              :digest (digest (vec (:pairwise selection)))})))))

;; -- the canonical-G route frontier (Q8a) --------------------------------------

(def ^:private canonical-law :canonical/co-application-G-min-v1)

(defn canonical-outer-frontier
  "Sealed :wm/q8-outer-frontier-v1 carrier for the canonical-G route, a pure
  projection of the retained outer-task-selection receipt.  Entries are the
  canonical frontier's scored rows (one per task, least G, digest-ordered
  tie-break); the census is the S1 open population with its typed
  task-standing exclusions.  Eligible tasks with no entry are NOT census
  exclusions: they are listed under :unranked with their typed reason and
  stage, and a frontier that does not cover the eligible census is a visible
  :decided-failure.  A task silently unaccounted for is refused
  :not-recomputable (Lean: omitted_open_task_invalid)."
  [receipt]
  (let [support (:support receipt)
        excluded (:excluded receipt)
        families (vec (get-in receipt [:policy-field :families]))
        fam-by (into {} (map (juxt :target-id identity)) families)
        menu (get-in receipt [:policy :selected-outer-cascade-policy :sealed-menu])
        menu-entry-targets (set (map :target-id (:entries menu)))
        menu-failures-by (group-by :target-id (:failures menu))
        menu-refusals-by (fn [target]
                           (filter #(= target (first (str/split
                                                      (str (:policy-id %)) #"/" 2)))
                                   (:refusals menu)))
        frontier (get-in receipt [:policy :canonical-g-frontier])
        fr-rows-by (group-by :target-id (:entries frontier))
        h (get-in frontier [:horizon-choice :horizon])
        cps (get-in receipt [:policy :canonical-policy-selection])
        census {:open (vec (sort (map (comp str :id) (concat support excluded))))
                :eligible (vec (sort (map (comp str :id) support)))
                :excluded (vec (sort-by :task
                                        (map (fn [x] {:task (str (:id x))
                                                      :reason (:ineligible-reason x)})
                                             excluded)))}
        eligible-set (set (:eligible census))
        ;; one entry per task with a scored row: least G, digest-sorted tie-break
        row-choice (fn [rows] (first (sort-by (juxt :G :cascade-digest) rows)))
        entries
        (vec (sort-by :task
                      (for [[t rows] (seq (into (sorted-map)
                                                (map (fn [[t all-rows]]
                                                       [t (filter #(= :scored (:status %)) all-rows)])
                                                     fr-rows-by)))
                            :when (seq rows)
                            :let [row (row-choice rows)
                                  g (:G row)
                                  channels {:G g}
                                  ev (digest (select-keys
                                              row [:policy-id :target-id :cascade-digest
                                                   :G :action-pin]))]]
                          {:task t
                           :channels channels
                           :observation (digest channels)
                           :evidence ev
                           :ranking-law canonical-law
                           :ranking-version 1
                           :dependencies (vec (sort [(digest [:wm/q8-task-observation t ev])
                                                     (digest [:wm/q8-ranking-law
                                                              canonical-law 1 h])]))
                           :comparison-key (comparison-key channels 0)
                           :absent-terms []
                           :policy-id (:policy-id row)
                           :cascade-digest (:cascade-digest row)})))
        entry-tasks (set (map :task entries))
        ;; every eligible task with no entry, with its typed reason and stage
        unranked
        (vec (sort-by :task
                      (keep (fn [t]
                              (let [family (get fam-by t)]
                                (cond
                                  (and family (not= :computed (:status family)))
                                  (let [kinds (map :kind (:failures family))]
                                    (when (seq (distinct kinds))
                                      {:task t :stage :s2
                                       :reason (first (sort (distinct kinds)))
                                       :reasons (vec (sort (distinct kinds)))}))
                                  (and family (not (contains? menu-entry-targets t)))
                                  (let [kinds (concat (map :kind (get menu-failures-by t))
                                                      (map :kind (menu-refusals-by t)))]
                                    (when (seq (distinct kinds))
                                      {:task t :stage :s4
                                       :reason (first (sort (distinct kinds)))
                                       :reasons (vec (sort (distinct kinds)))}))
                                  (contains? menu-entry-targets t)
                                  (let [reasons (vec (sort (distinct
                                                            (keep :reason (fr-rows-by t)))))]
                                    (when (seq reasons)
                                      {:task t :stage :s5
                                       :reason (first reasons)
                                       :reasons reasons}))
                                  :else nil)))
                            (sort eligible-set))))
        unranked-tasks (set (map :task unranked))
        selected (get-in receipt [:policy :canonical-policy-selection :action :target])
        selection-verdict (when (and (= :selected (:status cps)) (seq entries))
                            (strict-selection-correspondence entries selected))
        census-verdict (if (= entry-tasks eligible-set)
                         {:status :pass}
                         {:status :fail
                          :reason :frontier-not-eligible-census
                          :ranked (count entries)
                          :unranked (count unranked)
                          :unranked-by-reason (frequencies (map :reason unranked))})
        base {:schema outer-schema
              :ranking {:law canonical-law :version 1
                        :comparison :canonical-G-min :horizon h}
              :census census
              :unranked unranked
              :selected selected}
        refusal
        (cond
          (some #(nil? (:reason %)) (:excluded census))
          {:reason :exclusion-reason-absent
           :tasks (vec (keep #(when (nil? (:reason %)) (:task %)) (:excluded census)))}
          (not= :produced (:status frontier))
          {:reason :canonical-frontier-absent :frontier-status (:status frontier)}
          (some #(nil? (:comparison-key %)) entries)
          {:reason :channel-value-not-exact
           :tasks (vec (keep #(when (nil? (:comparison-key %)) (:task %)) entries))}
          (seq (remove eligible-set entry-tasks))
          {:reason :frontier-task-not-eligible
           :tasks (vec (sort (remove eligible-set entry-tasks)))}
          (seq (set/intersection entry-tasks unranked-tasks))
          {:reason :entry-also-unranked
           :tasks (vec (sort (set/intersection entry-tasks unranked-tasks)))}
          (not= :selected (:status cps))
          {:reason :no-outer-selection :selection-reason (or (:status cps) :selection-status-absent)}
          (not= selected (str selected))
          {:reason :no-outer-selection :selection-reason :selected-target-not-a-string}
          (= :not-recomputable (:status selection-verdict))
          selection-verdict
          :else
          (let [unaccounted (sort (set/difference eligible-set entry-tasks unranked-tasks))]
            (when (seq unaccounted)
              {:reason :eligible-task-unaccounted :tasks (vec unaccounted)})))]
    (seal (if refusal
            (merge base {:status :not-recomputable} (dissoc refusal :kind))
            (assoc base
                   :status (if (and (= :pass (:status selection-verdict))
                                    (= :pass (:status census-verdict)))
                             :certified :decided-failure)
                   :selection-verdict selection-verdict
                   :census-verdict census-verdict
                   :entries entries)))))

;; -- refresh against the prior frontier ---------------------------------------

(def ^:private lean-entry-keys
  [:task :observation :evidence :ranking-law :ranking-version :dependencies
   :comparison-key])

(defn- lean-entry [e epoch]
  (assoc (select-keys e lean-entry-keys) :freshness-epoch epoch))

(defn- deps [entries] (set (mapcat :dependencies entries)))

(defn refresh
  "Classify OUTER's certified entries against PRIOR (the predecessor's
  sealed frontier, or nil). Returns {:frontier <sealed frontier to persist>
  :refresh <refresh facts or typed not-recomputable>}."
  [outer prior run-id]
  (let [prior-ok? (and (map? prior) (= frontier-schema (:schema prior)) (intact? prior))
        epoch (if prior-ok? (inc (:epoch prior)) 0)
        computed (into {} (map (fn [e] [(:task e) (lean-entry e epoch)])) (:entries outer))
        prior-entries (when prior-ok? (:entries prior))
        prior-by (into {} (map (juxt :task identity)) prior-entries)
        changed (set/difference (deps prior-entries) (deps (vals computed)))
        affected? (fn [e] (boolean (seq (set/intersection (set (:dependencies e)) changed))))
        prior-tasks (set (keys prior-by))
        current-tasks (set (keys computed))
        removed (set/difference prior-tasks current-tasks)
        added (set/difference current-tasks prior-tasks)
        kept (set/intersection prior-tasks current-tasks)
        retained (vec (for [t (sort kept) :let [e (prior-by t)] :when (not (affected? e))] e))
        drift (vec (for [e retained
                         :when (not= (dissoc e :freshness-epoch)
                                     (dissoc (computed (:task e)) :freshness-epoch))]
                     (:task e)))
        invalidated (vec (for [t (sort kept) :let [e (prior-by t)] :when (affected? e)] e))
        recomputed (vec (for [t (sort (into (set (map :task invalidated)) added))]
                          (computed t)))
        current (vec (sort-by :task (concat retained recomputed)))
        frontier (seal {:schema frontier-schema :run/id run-id :epoch epoch :entries current})]
    {:frontier frontier
     :refresh
     (cond
       (nil? prior) {:status :not-recomputable :reason :no-prior-frontier}
       (not prior-ok?) {:status :not-recomputable :reason :prior-frontier-tampered}
       (seq drift) {:status :not-recomputable :reason :retained-entry-changed-without-dependency-change
                    :tasks drift}
       :else
       {:status :present
        :prior (vec prior-entries) :current current
        :retained retained :invalidated invalidated :recomputed recomputed
        :removed (vec (sort removed))
        :delta {:changed-observations (vec (sort changed))
                :added-tasks (vec (sort added))
                :removed-tasks (vec (sort removed))}
        :current-open-tasks (vec (sort current-tasks))})}))

;; -- the selected task's cascade ----------------------------------------------

(defn- q6-exclusion
  "The requirement exclusion the decision's Q6 carrier justifies, or a
  typed refusal."
  [q6 predecessor scored]
  (let [prev (:previous predecessor)]
    (cond
      (not (and (= previous-run/exclusion-schema (:schema q6)) (intact? q6)))
      {:refusal :q6-carrier-absent-or-tampered}
      (not= (:sha256 predecessor) (get-in q6 [:predecessor :sha256]))
      {:refusal :q6-carrier-stale}
      (not= :excluded (:status q6))
      (if (pos? (or (:removed q6) 0)) {:refusal :q6-removal-without-exclusion} {:excluded #{}})
      (not (and (= :previous-choice-refused-on-same-inputs (:reason q6))
                (= :refused (get-in prev [:outcome :outcome]))
                (= (:input-digest prev) (:input-digest q6))
                (= [(get-in prev [:choice :policy-key])] (:excluded-keys q6))))
      {:refusal :q6-exclusion-unjustified}
      (not (set/subset? (set (:excluded-keys q6)) scored))
      {:refusal :q6-exclusion-outside-scored}
      :else {:excluded (set (:excluded-keys q6))})))

(defn cascade
  "The selected task's stages from DECISION (its certificate's
  :policy-stages, Q6 carrier and selection-law posterior)."
  [decision predecessor selected-task]
  (let [cert (:selection-certificate decision)
        stages (get-in cert [:policy-stages selected-task])
        posterior (set (map prefix-admission/candidate-key
                            (keys (get-in decision [:selection-law :posterior]))))
        scored (set (:scored stages))
        q6 (q6-exclusion (:q6 cert) predecessor scored)]
    (cond
      (nil? stages) {:status :not-recomputable :reason :policy-stages-absent}
      (:refusal q6) {:status :not-recomputable :reason (:refusal q6)}
      :else {:status :present
             :selected-task selected-task
             :available-admissible (set (:constructed stages))
             :constructed (set (:constructed stages))
             :admitted (set (:admitted stages))
             :scored scored
             :requirement-excluded (:excluded q6)
             :posterior posterior})))

;; -- the record carrier --------------------------------------------------------

(defn certificate
  "The sealed Q8 carrier from OUTER (the outer receipt's :q8-frontier),
  PREDECESSOR (the run's sealed Q6 predecessor carrier), DECISION and the
  decision's CHOSEN-TARGET."
  [{:keys [outer predecessor decision chosen-target run-id]}]
  (let [outer-ok? (and (= outer-schema (:schema outer)) (intact? outer))
        certified? (and outer-ok? (contains? #{:certified :decided-failure} (:status outer)))
        prior (when (= :present (:status predecessor)) (:frontier predecessor))
        {:keys [frontier] r :refresh} (when certified? (refresh outer prior run-id))
        selected (:selected outer)
        sel-entry (first (filter #(= selected (:task %)) (:current r)))
        c (when certified? (cascade decision predecessor selected))
        reasons (cond-> []
                  (not outer-ok?) (conj :outer-frontier-absent-or-tampered)
                  (and outer-ok? (not certified?)) (conj (:reason outer))
                  (and certified? (not= :present (:status r))) (conj (:reason r))
                  (and certified? (not= :present (:status c))) (conj (:reason c))
                  (and certified? (not= selected chosen-target)) (conj :chosen-target-not-outer-selection))]
    (seal (cond-> {:schema certificate-schema
                   :status (if (empty? reasons)
                             (if (= :decided-failure (:status outer)) :decided-failure :certified)
                             :not-recomputable)
                   :bindings {:outer-frontier (:sha256 outer)
                              :predecessor (:sha256 predecessor)
                              :q6 (get-in decision [:selection-certificate :q6 :sha256])
                              :chosen-target chosen-target}}
            (contains? outer :census-verdict)
            (assoc :census-verdict (:census-verdict outer))
            (seq reasons) (assoc :reasons reasons)
            frontier (assoc :frontier frontier)
            (empty? reasons)
            (assoc :census (:census outer)
                   :refresh (dissoc r :status)
                   :selection {:selected sel-entry :current-epoch (:freshness-epoch sel-entry)}
                   :selection-verdict (:selection-verdict outer)
                   :cascade (dissoc c :status))))))

(defn- retained-outer
  "Recompute the outer carrier from the retained production selection.  This
  also upgrades pre-fix tied records whose old outer carrier discarded its
  entries after classifying the tie as not-recomputable."
  [record]
  (let [selection (get-in record [:outer-task-selection :outer-learning :selection])
        ranked (:ranking selection)
        old (get-in record [:outer-task-selection :q8-frontier])]
    (if (and (= :selected-not-strict-key-minimum (:reason old))
             (seq ranked) (seq (:pairwise selection)))
      (outer-frontier
       {:tasks (get-in record [:outer-task-selection :support])
        :excluded (:typed-exclusions selection)
        :candidates ranked
        :selection selection
        :comparison :complete-union
        :missing-channel-cost (get-in old [:ranking :missing-channel-cost])})
      old)))

(defn record-inputs
  "The certificate's inputs, read from a persisted RECORD alone."
  [record]
  {:outer (retained-outer record)
   :predecessor (get-in record [:q6 :predecessor])
   :decision (:decision record)
   :chosen-target (get-in record [:decision :chosen :target])
   :run-id (:run/id record)})

(defn verify-record
  "The record's own :q8 carrier when it is intact and equals the carrier
  recomputed from the record; else a typed not-recomputable reason."
  [record]
  (let [q8 (:q8 record)
        recomputed (certificate (record-inputs record))
        legacy-tie? (and (= :selected-not-strict-key-minimum (first (:reasons q8)))
                         (= :decided-failure (:status recomputed))
                         (= :tied-minimum (get-in recomputed [:selection-verdict :reason])))]
    (cond
      (nil? q8) {:status :not-recomputable :reason :q8-carrier-absent}
      (not (and (= certificate-schema (:schema q8)) (intact? q8)))
      {:status :not-recomputable :reason :q8-carrier-tampered}
      (and (not legacy-tie?) (not= q8 recomputed))
      {:status :not-recomputable :reason :q8-carrier-not-bound-to-record}
      legacy-tie? {:status :verified :carrier recomputed}
      (not (contains? #{:certified :decided-failure} (:status q8)))
      {:status :not-recomputable :reason (first (:reasons q8))}
      :else {:status :verified :carrier q8})))

;; -- a mirror of Requirements.Q8, for tests ------------------------------------

(defn- task-set [entries] (set (map :task entries)))

(defn lean-q8
  "Requirements.Q8 over exporter-shaped Clojure values: CARRIER (a verified
  :q8 carrier), OPEN (the open-task set) and Q6 facts {:refused? bool
  :same-digest? bool :previous-cascade key}. Mirrors the Lean definition
  clause by clause; Lean remains the authority."
  [carrier open q6]
  (let [{:keys [census] r :refresh s :selection c :cascade} carrier
        excluded-tasks (set (map :task (:excluded census)))
        eligible (set (:eligible census))
        affected? (fn [e] (boolean (seq (set/intersection (set (:dependencies e))
                                                          (set (get-in r [:delta :changed-observations]))))))
        removed (set (:removed r))
        prior (:prior r) current (:current r)
        key-lt (fn [a b] (< (:comparison-key a) (:comparison-key b)))
        sel (:selected s)
        remaining (set/difference (:scored c) (:requirement-excluded c))
        excludable (if (and (:refused? q6) (:same-digest? q6)) #{(:previous-cascade q6)} #{})]
    (boolean
     (and
      ;; census
      (= (count (:excluded census)) (count excluded-tasks))
      (every? #(some? (:reason %)) (:excluded census))
      (empty? (set/intersection eligible excluded-tasks))
      (= (set/union eligible excluded-tasks) (set open))
      ;; refresh
      (= (count prior) (count (task-set prior)))
      (= (count current) (count (task-set current)))
      (= removed (set/intersection (set (get-in r [:delta :removed-tasks])) (task-set prior)))
      (every? #(and (some #{%} prior) (not (affected? %)) (not (removed (:task %)))
                    (some #{%} current)) (:retained r))
      (every? #(and (some #{%} prior) (affected? %) (not (removed (:task %)))) (:invalidated r))
      (every? #(or (removed (:task %)) (= (boolean (some #{%} (:invalidated r))) (affected? %))) prior)
      (= (task-set prior) (set/union (task-set (:retained r)) (task-set (:invalidated r)) removed))
      (= (task-set (:recomputed r)) (set/union (task-set (:invalidated r))
                                               (set (get-in r [:delta :added-tasks]))))
      (every? #(some #{%} current) (:recomputed r))
      (= (task-set current) (set/union (task-set (:retained r)) (task-set (:recomputed r))))
      (= (task-set current) (set (:current-open-tasks r)))
      (empty? (set/intersection removed (set (:current-open-tasks r))))
      (empty? (set/intersection (set (get-in r [:delta :added-tasks])) (task-set prior)))
      (empty? (set/intersection (set (get-in r [:delta :added-tasks]))
                                (set (get-in r [:delta :removed-tasks]))))
      (= (set (:current-open-tasks r)) eligible)
      ;; selection over the eligible frontier
      (some #{sel} current)
      (= (task-set current) eligible)
      (= (:freshness-epoch sel) (:current-epoch s))
      (every? #(or (= sel %) (key-lt sel %)) current)
      ;; selected cascade
      (= (:selected-task c) (:task sel))
      (= (:available-admissible c) (:constructed c) (:admitted c) (:scored c))
      (set/subset? (:requirement-excluded c) (:scored c))
      (= (:posterior c) remaining)
      (or (<= (count remaining) 1) (< 1 (count (:posterior c))))
      (set/subset? (:requirement-excluded c) excludable)))))
