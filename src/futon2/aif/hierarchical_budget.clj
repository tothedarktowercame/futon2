(ns futon2.aif.hierarchical-budget
  "R11 hierarchical shared-budget arbitration.

   Local selectors propose alternatives inside their own sub-budgets. This
   namespace composes their Pareto frontiers upward, so every ancestor can
   choose a globally feasible combination without flattening away local
   factoring. The result is exact for the finite proposal field supplied: no
   greedy choice at a child can hide a cheaper alternative that is better for
   the shared parent budget.")

(def ^:private epsilon 1.0e-9)

(defn- fail! [message data]
  (throw (ex-info message
                  (assoc data :failure-kind :invalid-budget-hierarchy))))

(defn- finite-number? [x]
  (and (number? x) (Double/isFinite (double x))))

(defn- option-id-key [option]
  (->> (:selected option) (map :id) (sort-by pr-str) pr-str))

(defn- option-order-key [option]
  [(- (double (:utility option)))
   (double (:cost option))
   (option-id-key option)])

(defn- pareto-frontier
  "Keep only cost/utility non-dominated options. Equal options resolve by a
   stable lexical proposal-id key, making replay independent of map order."
  [options]
  (first
   (reduce
    (fn [[kept best-utility] option]
      (let [utility (double (:utility option))]
        (if (> utility (+ best-utility epsilon))
          [(conj kept option) utility]
          [kept best-utility])))
    [[] Double/NEGATIVE_INFINITY]
    (sort-by (juxt (comp double :cost)
                   (comp - double :utility)
                   option-id-key)
             options))))

(defn- combine-options [left right budget]
  (pareto-frontier
   (for [a left
         b right
         :let [cost (+ (double (:cost a)) (double (:cost b)))]
         :when (<= cost (+ (double budget) epsilon))]
     {:cost cost
      :utility (+ (double (:utility a)) (double (:utility b)))
      :selected (into (:selected a) (:selected b))})))

(defn- validate-budget! [node]
  (when-not (and (finite-number? (:budget node))
                 (not (neg? (double (:budget node)))))
    (fail! "Every hierarchy node needs a finite, non-negative :budget"
           {:node-id (:id node) :budget (:budget node)})))

(defn- validate-proposal! [node-id proposal]
  (when-not (contains? proposal :id)
    (fail! "Every budget proposal needs an :id"
           {:node-id node-id :proposal proposal}))
  (when-not (and (finite-number? (:cost proposal))
                 (not (neg? (double (:cost proposal)))))
    (fail! "Every proposal needs a finite, non-negative :cost"
           {:node-id node-id :proposal-id (:id proposal)
            :cost (:cost proposal)}))
  (when-not (finite-number? (:utility proposal))
    (fail! "Every proposal needs a finite :utility"
           {:node-id node-id :proposal-id (:id proposal)
            :utility (:utility proposal)})))

(defn- leaf-frontier [node path]
  (let [budget (double (:budget node))
        selection-limit (:selection-limit node)
        _ (when-not (or (nil? selection-limit) (= 1 selection-limit))
            (fail! "Leaf :selection-limit must be 1 when supplied"
                   {:node-id (:id node) :selection-limit selection-limit}))
        proposals
        (mapv (fn [proposal]
                (validate-proposal! (:id node) proposal)
                (assoc proposal :budget/path path))
              (:proposals node))]
    {:frontier
     (if (= 1 selection-limit)
       (pareto-frontier
        (into [{:cost 0.0 :utility 0.0 :selected []}]
              (comp
               (filter #(<= (double (:cost %)) (+ budget epsilon)))
               (map (fn [proposal]
                      {:cost (double (:cost proposal))
                       :utility (double (:utility proposal))
                       :selected [proposal]})))
              proposals))
       (reduce
        (fn [frontier proposal]
          (combine-options
           frontier
           [{:cost 0.0 :utility 0.0 :selected []}
            {:cost (double (:cost proposal))
             :utility (double (:utility proposal))
             :selected [proposal]}]
           budget))
        [{:cost 0.0 :utility 0.0 :selected []}]
        proposals))
     :nodes [(cond-> {:id (:id node) :budget budget :path path}
               selection-limit (assoc :selection-limit selection-limit))]
     :proposals proposals}))

(declare hierarchy-frontier)

(defn- branch-frontier [node path]
  (let [budget (double (:budget node))
        children (mapv #(hierarchy-frontier % (conj path (:id %)))
                       (:children node))]
    {:frontier (reduce #(combine-options %1 %2 budget)
                       [{:cost 0.0 :utility 0.0 :selected []}]
                       (map :frontier children))
     :nodes (into [{:id (:id node) :budget budget :path path}]
                  (mapcat :nodes children))
     :proposals (vec (mapcat :proposals children))}))

(defn- hierarchy-frontier [node path]
  (when-not (contains? node :id)
    (fail! "Every hierarchy node needs an :id" {:node node}))
  (validate-budget! node)
  (let [children? (contains? node :children)
        proposals? (contains? node :proposals)]
    (when (= children? proposals?)
      (fail! "A hierarchy node must contain exactly one of :children or :proposals"
             {:node-id (:id node)}))
    (if children?
      (branch-frontier node path)
      (leaf-frontier node path))))

(defn- assert-unique! [kind ids]
  (when-let [duplicate
             (->> ids frequencies (filter (fn [[_ n]] (> n 1))) ffirst)]
    (fail! (str "Duplicate " (name kind) " id in budget hierarchy")
           {:kind kind :duplicate-id duplicate})))

(defn- usage-by-node [proposals]
  (reduce
   (fn [usage proposal]
     (reduce #(update %1 %2 (fnil + 0.0) (double (:cost proposal)))
             usage
             (:budget/path proposal)))
   {}
   proposals))

(defn- complete-usage [nodes usage]
  (into {} (map (fn [{:keys [id]}]
                  [id (double (get usage id 0.0))])) nodes))

(defn arbitrate
  "Choose the maximum-utility feasible portfolio from HIERARCHY.

     {:id :root :budget 8
      :children [{:id :agent-a :budget 6
                  :proposals [{:id :a1 :cost 6 :utility 10} ...]}
                 {:id :agent-b :budget 6 :proposals [...]}]}

   Children may themselves contain children. Every selected proposal consumes
   its cost at its leaf and at every ancestor. The returned :node-usage map is
   the replayable witness that no local or shared budget was exceeded."
  [hierarchy]
  (let [{:keys [frontier nodes proposals]}
        (hierarchy-frontier hierarchy [(:id hierarchy)])
        _ (assert-unique! :node (map :id nodes))
        _ (assert-unique! :proposal (map :id proposals))
        winner (first (sort-by option-order-key frontier))
        selected (:selected winner)
        selected-ids (set (map :id selected))
        usage (complete-usage nodes (usage-by-node selected))
        requested-usage (complete-usage nodes (usage-by-node proposals))
        node-budgets (into {} (map (juxt :id :budget) nodes))
        node-selection-limits
        (into {} (keep (fn [{:keys [id selection-limit]}]
                         (when selection-limit [id selection-limit]))) nodes)
        within-budget?
        (every? (fn [[node-id used]]
                  (<= (double used)
                      (+ (double (get node-budgets node-id)) epsilon)))
                usage)]
    (when-not within-budget?
      (throw (ex-info "Internal error: arbiter emitted an over-budget portfolio"
                      {:failure-kind :budget-invariant-violation
                       :node-usage usage :node-budgets node-budgets})))
    {:selected selected
     :selected-ids selected-ids
     :rejected (mapv #(assoc % :rejection/reason :shared-budget-arbitration)
                     (remove #(contains? selected-ids (:id %)) proposals))
     :total-cost (double (:cost winner))
     :total-utility (double (:utility winner))
     :node-usage usage
     :node-budgets node-budgets
     :node-selection-limits node-selection-limits
     :requested-usage requested-usage
     :oversubscribed-nodes
     (->> node-budgets
          (keep (fn [[node-id budget]]
                  (when (> (double (get requested-usage node-id 0.0))
                           (+ (double budget) epsilon))
                    node-id)))
          set)
     :within-all-budgets? true
     :selection-boundary :hierarchical-shared-budget
     :proof {:method :exact-pareto-frontier-composition
             :tie-break [:maximum-utility :minimum-cost :lexical-proposal-id]
             :invariant :every-selected-cost-is-charged-at-every-ancestor}}))
