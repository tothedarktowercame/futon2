(ns futon2.aif.interpretation-construction
  "Pure, bounded construction from GIVEN interpretations, not retrieval/admission.
  Searches backward from all wants, then checks each order with the existing
  first-enabled model. Model reachability is not an observed discharge."
  (:require [futon2.aif.load-identity :as load-identity]
            [clojure.set :as set]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.cascade-policy :as policy]
            [futon2.aif.construction :as construction]
            [futon2.aif.construction-moves :as moves]))

(load-identity/register! *ns* *file*)

(defn- refuse [kind & [details]]
  (merge {:status :refused :kind kind :candidates []} details))
(defn- finite? [x] (and (number? x) (Double/isFinite (double x))))
(defn- union-of [f xs] (reduce set/union #{} (map f xs)))
(defn- pattern? [p]
  (and (= #{:guard :produces} (set (keys p)))
       (= #{:needs :forbids} (set (keys (:guard p))))
       (every? set? [(:produces p) (get-in p [:guard :needs]) (get-in p [:guard :forbids])])))

(defn- search-plans [patterns want established limit]
  (loop [queue [{:order [] :needed (set want)}] plans [] findings [] expanded 0]
    (cond
      (empty? queue) {:plans plans :findings findings :expanded expanded}
      (>= expanded limit) (refuse :search-budget-exhausted {:expanded expanded :limit limit})
      :else
      (let [{:keys [order needed]} (peek queue)
            queue (pop queue)
            unmet (set/difference needed established (union-of :produces (map patterns order)))]
        (if (empty? unmet)
          (recur queue (conj plans order) findings (inc expanded))
          (let [token (first (sort-by pr-str unmet))
                producers (sort-by pr-str (for [[id p] patterns :when (contains? (:produces p) token)] id))]
            (recur (into queue (map (fn [id] {:order (conj order id)
                                             :needed (set/union needed (get-in patterns [id :guard :needs]))}) producers))
                   plans (cond-> findings (empty? producers)
                           (conj {:kind :unproduced-need :token token :order order}))
                   (inc expanded))))))))

(defn- compile-plan [patterns established want horizon order cost]
  (let [candidate {:precedence order :patterns (mapv #(assoc (patterns %) :id %) order)}
        ordering ((moves/order-by-need {:cost cost}) [candidate])
        ordered (if (= :no-move (:status ordering)) candidate (first (:proposed-family ordering)))
        precedence (:precedence ordered)
        maps (mapv #(policy/token-interpretation % (patterns %)) precedence)
        row (model/rollout (constantly maps) (model/observed-belief established) horizon)
        edges (set (for [p order q order :when (not= p q)
                         :when (seq (set/intersection (:produces (patterns p))
                                                      (get-in patterns [q :guard :needs])))] [p q]))]
    (cond
      (seq (:cycles ordering)) {:finding {:kind :need-cycle :order order :cycles (:cycles ordering)}}
      (:status row) {:finding {:kind :model-refused :order precedence :model row}}
      (not (some #(set/subset? (set want) %) (keys row)))
      {:finding {:kind :want-unreachable-within-horizon :order precedence :horizon horizon :belief row}}
      :else
      {:candidate (assoc ordered :need-edges edges)
       :ordering {:move-id :order-by-need :before order :after precedence
                  :status (if (= :no-move (:status ordering)) :already-ordered :reordered)}})))

(defn construct
  "Return {:status :constructed :candidates [...]} or a typed refusal.

  Inputs: :target, nonempty :want, :observation {token boolean},
  :interpretations {id {:guard {:needs #{} :forbids #{}} :produces #{}}},
  :interpretation-receipts {id nonempty-map}, :horizon, :move-cost,
  :budget {:max-moves n :max-expansions n}, and :evaluate-g (candidate -> G).
  G is injected unchanged; candidates carry :patterns with ids and :precedence.
  Search bounds are explicit; exhausting search refuses rather than claiming
  a complete family. Only support sets minimal among the reachable plans remain.

  This slice requires fully observed tokens. :observation-required names missing
  or unknown tokens for an upstream measurement/check step; it never treats them
  as false or claims that the target is impossible. No observation/locator store
  or interpreter is called. Receipts report :token-set-not-supplied to the existing
  construction policy: supplying observation locators/check policies is later work."
  [{:keys [target want observation interpretations interpretation-receipts
           budget horizon move-cost evaluate-g]}]
  (cond
    (not (and (map? budget) (integer? (:max-moves budget)) (<= 0 (:max-moves budget))
              (pos-int? (:max-expansions budget)))) (refuse :budget-required)
    (not (pos-int? horizon)) (refuse :horizon-required)
    (not (and target (coll? want) (seq want) (map? observation)
              (map? interpretations) (seq interpretations)
              (every? pattern? (vals interpretations))
              (finite? move-cost) (<= 0 move-cost) (fn? evaluate-g)))
    (refuse :invalid-input)
    :else
    (let [tokens (set/union (set want) (union-of (fn [p] (set/union (:produces p)
                                                              (get-in p [:guard :needs])
                                                              (get-in p [:guard :forbids])))
                                               (vals interpretations)))
          unknown (vec (sort-by pr-str (remove #(boolean? (get observation %)) tokens)))
          missing (vec (sort-by pr-str (remove #(and (map? (get interpretation-receipts %))
                                                     (seq (get interpretation-receipts %)))
                                              (keys interpretations))))
          established (set (keep (fn [[t v]] (when (true? v) t)) observation))]
      (cond
        (seq unknown) (refuse :observation-required {:tokens unknown})
        (seq missing) (refuse :interpretation-receipt-missing {:patterns missing})
        (set/subset? (set want) established) (refuse :want-already-observed)
        :else
        (let [search (search-plans interpretations want established (:max-expansions budget))]
          (if (:status search)
            search
            (let [compiled (mapv #(compile-plan interpretations established want horizon % move-cost) (:plans search))
                  viable (vec (keep :candidate compiled))
                  family (vec (distinct (remove
                                         (fn [c] (some #(and (not= (set (:precedence %)) (set (:precedence c)))
                                                                            (set/subset? (set (:precedence %)) (set (:precedence c)))) viable))
                                         viable)))
                  findings (into (:findings search) (keep :finding compiled))]
              (if (empty? family)
                (refuse :no-supported-order {:findings findings :expanded (:expanded search)})
                (let [move (fn [current]
                             (if (= family current)
                               {:status :no-move :move-id :compose-by-need :reason :family-already-constructed}
                               {:move-id :compose-by-need :proposed-family family :cost move-cost}))
                      evaluated (fn [c]
                                  (let [g (evaluate-g c)]
                                    (when-not (finite? g)
                                      (throw (ex-info "Constructor needs a finite G comparison"
                                                      {:constructor/refusal :nonfinite-g :value g})))
                                    g))
                      result (try
                               (construction/construct
                                {:target target :want (vec want) :q0 (vec established)
                                 :initial-family [{:precedence [] :patterns []}]
                                 :moves [move] :evaluate-g evaluated :budget budget :horizon horizon})
                               (catch clojure.lang.ExceptionInfo e
                                 (if-let [kind (:constructor/refusal (ex-data e))]
                                   (refuse kind (dissoc (ex-data e) :constructor/refusal))
                                   (throw e))))
                      receipt (when (:receipt result)
                                (assoc (:receipt result) :kind :machine-constructed
                                       :search {:expanded (:expanded search) :limit (:max-expansions budget)}
                                       :ordering (vec (keep :ordering compiled))))]
                  (cond
                    (:status result) result
                    (empty? (:moves receipt))
                    (refuse :construction-not-taken {:construction-receipt receipt :findings findings})
                    :else
                    {:status :constructed :findings findings
                     :candidates (mapv (fn [c]
                                         (assoc (select-keys c [:precedence :need-edges])
                                                :kind :cascade-candidate :target target
                                                :construction-receipt receipt
                                                :interpretation-receipts
                                                (select-keys interpretation-receipts (:precedence c))))
                                       (:family result))}))))))))))
