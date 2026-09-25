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
  ;; Backward search from ALL wants. A WANT with no producer no longer kills
  ;; the branch (D15: admission needs only one newly satisfied want, and an
  ;; honest partial interpretation must not refuse the whole target): the
  ;; :unproduced-need finding is recorded, the token is dropped from THIS
  ;; branch's needs, and the search continues over the remaining wants. The
  ;; want stays in the target's want set — only the branch's copy of needed
  ;; loses it — and compile-plan names it :no-producer in the candidate's
  ;; :unreached-wants. An unproduced GUARD NEED still kills the branch: the
  ;; pattern that needs it cannot fire, so the order is not a plan at all.
  ;; Because only producerless wants are dropped, every plan this returns
  ;; still produces every want that has any producer; a plan that newly
  ;; produces nothing (including the empty plan) is filtered at compile.
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
            (if (empty? producers)
              (recur (if (contains? (set want) token)
                       (conj queue {:order order :needed (disj needed token)})
                       queue)
                     plans
                     (conj findings {:kind :unproduced-need :token token :order order})
                     (inc expanded))
              (recur (into queue (map (fn [id] {:order (conj order id)
                                                :needed (set/union needed (get-in patterns [id :guard :needs]))}) producers))
                     plans findings
                     (inc expanded)))))))))

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
      :else
      (let [reachable (union-of identity (keys row))
            produced (union-of :produces (map patterns precedence))
            new-wants (set/difference (set want) established)
            reached-new (set/intersection new-wants reachable)
            ;; Typed apart (D15): a want the plan cannot produce because NO
            ;; pattern produces it is :no-producer; a want the plan produces
            ;; but the model does not reach within horizon is
            ;; :beyond-horizon. With horizon 4 a long honest chain must not
            ;; look like a missing producer.
            unreached (vec (sort-by (comp pr-str :token)
                                    (for [w (set/difference new-wants reached-new)]
                                      {:token w
                                       :reason (if (contains? produced w)
                                                 :beyond-horizon
                                                 :no-producer)})))]
        (if (empty? reached-new)
          {:finding (if (empty? (set/intersection produced new-wants))
                      ;; The plan produces no want that is not already
                      ;; established (the empty plan never constructs).
                      {:kind :no-new-want-produced :order precedence}
                      {:kind :want-unreachable-within-horizon :order precedence :horizon horizon :belief row})}
          {:candidate (assoc ordered :need-edges edges
                             :reached-wants (vec (sort-by pr-str reached-new))
                             :unreached-wants unreached)
           :ordering {:move-id :order-by-need :before order :after precedence
                      :status (if (= :no-move (:status ordering)) :already-ordered :reordered)}})))))

(defn support
  "The constructor's support step, before any G: the family of minimal plans
  over the GIVEN interpretations that each newly produce at least one want,
  within HORIZON and the search budget. Returns {:status :supported :family
  [...] :findings [...] :expanded n :tokens #{...} :established #{...}} or the
  same typed refusal `construct` gives (:observation-required,
  :interpretation-receipt-missing, :want-already-observed,
  :search-budget-exhausted, :no-supported-order). `construct` is this step
  followed by the G comparison, so a target is in the support exactly when
  construct gets past :no-supported-order; whether G then takes a move is
  scoring, not support."
  [{:keys [target want observation interpretations interpretation-receipts
           budget horizon move-cost]}]
  (cond
    (not (and (map? budget) (pos-int? (:max-expansions budget)))) (refuse :budget-required)
    (not (pos-int? horizon)) (refuse :horizon-required)
    (not (and target (coll? want) (seq want) (map? observation)
              (map? interpretations) (seq interpretations)
              (every? pattern? (vals interpretations))))
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
                  ;; Minimal support among plans reaching the SAME wants: a
                  ;; partial plan does not eclipse a full-want plan just
                  ;; because its precedence is smaller (D15).
                  family (vec (distinct (remove
                                         (fn [c] (some #(and (not= (set (:precedence %)) (set (:precedence c)))
                                                                            (set/subset? (set (:precedence %)) (set (:precedence c)))
                                                                            (set/subset? (set (:reached-wants c)) (set (:reached-wants %)))) viable))
                                         viable)))
                  findings (into (:findings search) (keep :finding compiled))]
              (if (empty? family)
                (refuse :no-supported-order {:findings findings :expanded (:expanded search)})
                {:status :supported :family family :findings findings :compiled compiled
                 :search search :expanded (:expanded search)
                 :tokens tokens :established established}))))))))

(defn construct
  "Return {:status :constructed :candidates [...]} or a typed refusal.

  Inputs: :target, nonempty :want, :observation {token boolean},
  :interpretations {id {:guard {:needs #{} :forbids #{}} :produces #{}}},
  :interpretation-receipts {id nonempty-map}, :horizon, :move-cost,
  :budget {:max-moves n :max-expansions n}, and :evaluate-g (candidate -> G).
  G is injected unchanged; candidates carry :patterns with ids and :precedence.
  Search bounds are explicit; exhausting search refuses rather than claiming
  a complete family. Only support sets minimal among the reachable plans remain.

  Partial wants (D15): a plan is admissible when it newly produces at least
  one want; the empty plan never constructs. Each candidate's
  :construction-receipt names what it leaves as :unreached-wants
  [{:token ... :reason :no-producer|:beyond-horizon}] and the candidate
  carries the target's full :want so the judge's G scores partial plans
  against all wants. Full-want plans come first in :candidates. Unreached
  wants are never dropped from the target.

  This slice requires fully observed tokens. :observation-required names missing
  or unknown tokens for an upstream measurement/check step; it never treats them
  as false or claims that the target is impossible. No observation/locator store
  or interpreter is called. Receipts report :token-set-not-supplied to the existing
  construction policy: supplying observation locators/check policies is later work."
  [{:keys [target want observation interpretations interpretation-receipts
           budget horizon move-cost evaluate-g] :as input}]
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
    (let [supported (support input)]
      (if (not= :supported (:status supported))
        supported
        (let [{:keys [family findings compiled search tokens established]} supported
              move (fn [current]
                             (if (= family current)
                               {:status :no-move :move-id :compose-by-need :reason :family-already-constructed}
                               {:move-id :compose-by-need :proposed-family family :cost move-cost}))
                      ;; W6: a G value is comparable only with the universe
                      ;; it was normalised over. Every candidate here is
                      ;; scored against this target's token set, so declare
                      ;; it as the universe; an injected map result's own
                      ;; :universe wins when it carries one.
                      universe (vec (sort-by pr-str tokens))
                      evaluated (fn [c]
                                  (let [r (evaluate-g c)
                                        g (if (map? r) (:value r) r)]
                                    (when-not (finite? g)
                                      (throw (ex-info "Constructor needs a finite G comparison"
                                                      {:constructor/refusal :nonfinite-g :value g})))
                                    {:value g
                                     :universe (if (and (map? r) (some? (:universe r)))
                                                 (:universe r)
                                                 universe)}))
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
                     ;; Full-want plans first (stable): inside the
                     ;; constructor a partial plan never ranks above a plan
                     ;; that reaches every want. G scoring against the full
                     ;; want set is the judge's job; each candidate carries
                     ;; the target's full :want so it can.
                     :candidates (mapv (fn [c]
                                         (assoc (select-keys c [:precedence :need-edges])
                                                :kind :cascade-candidate :target target
                                                :want (vec want)
                                                :construction-receipt
                                                (assoc receipt
                                                       :unreached-wants (:unreached-wants c)
                                                       ;; clause 0: this
                                                       ;; candidate's
                                                       ;; containment order
                                                       ;; over units (or the
                                                       ;; typed
                                                       ;; :cyclic-containment
                                                       ;; refusal)
                                                       :order (construction/containment-order c))
                                                :interpretation-receipts
                                                (select-keys interpretation-receipts (:precedence c))))
                                       (sort-by (fn [c] (if (seq (:unreached-wants c)) 1 0))
                                                (:family result)))}))))))
