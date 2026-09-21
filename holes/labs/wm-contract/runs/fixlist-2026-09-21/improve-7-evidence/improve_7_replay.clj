(ns improve-7-replay
  "Offline declared focus sensitivity. Not a production preference proposal."
  (:require [clojure.pprint :as pp] [clojure.set :as set]
            [futon2.report.novelty-discovery-test :as frozen]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.policy :as policy]))
(def focus #{"M-aif-policy-conditioned-eig" "M-f11-find-production-successor"
             "M-wm-08-external-f2" "M-G-wm-wiring" "M-wm-aif-policy-grain-compliance"
             "M-apm-capability-ratchet"})
(def adjacent #{"M-action-cost-modelling" "M-futonzero-generative" "M-aif4iad"})
(defn topic [target] (cond (focus target) :focus (adjacent target) :adjacent :else :irrelevant))
(defn utility [w s] (reduce + 0.0 (map #(double (get w % 0)) s)))
(defn report-case [f name masses]
  (let [w (get-in f [:terminal :weights]) v (get-in f [:terminal :universe])
        initial (ffirst (:q0 f)) fresh (set/difference (set (keys w)) initial)
        groups (group-by #(topic (first %)) fresh)
        lp (fn [tokens] (reduce + 0.0 (map #(Math/log1p (Math/exp (double (get w % 0)))) tokens)))
        lF (lp (:focus groups)) lA (lp (:adjacent groups)) lI (lp (:irrelevant groups))
        free (lp (set/difference v fresh))
        nonempty #(if (zero? %) Double/NEGATIVE_INFINITY (Math/log (Math/expm1 %)))
        log-z {:focus (+ free (nonempty lF) lA) :adjacent (+ free (nonempty lA))
               :irrelevant (+ free lF lA (nonempty lI)) :nothing free}
        classify (fn [state]
                   (let [new (set/intersection fresh state) cs (set (map #(topic (first %)) new))]
                     (cond (:irrelevant cs) :irrelevant (:focus cs) :focus (:adjacent cs) :adjacent :else :nothing)))
        logc (fn [s] (let [c (classify s) mass (masses c)]
                       (if (zero? mass) Double/NEGATIVE_INFINITY
                         (+ (Math/log (double mass)) (utility w s) (- (log-z c))))))
        cfn (fn [tau] (if (= tau (:horizon f)) #(Math/exp (logc %)) (constantly (Math/pow 2.0 (- (count v))))))
        _ (when (<= (count v) 12)
            (let [states (reduce (fn [acc t] (concat acc (map #(conj % t) acc))) [#{}] v)
                  represented (reduce + (map #((cfn (:horizon f)) %) states))
                  reserved (reduce + (for [[k m] masses :when (= Double/NEGATIVE_INFINITY (log-z k))] m))]
              (assert (frozen/close? 1 (+ represented reserved)))))
        rows (mapv (fn [r]
                     (let [g (model/horizon-g-sparse
                              {:rates (get-in f [:terms :A :value]) :q0 (:q0 f) :horizon (:horizon f)
                               :universe v :c-fn-pointwise cfn
                               :precedence-fn (constantly (get-in r [:candidate :id :precedence]))})]
                       (assert (or (number? g) (= :infinite g)) g)
                       (assoc r :g (if (= :infinite g) Double/POSITIVE_INFINITY g) :outcome-class (classify (:state r))))) (:rows f))
        cs (mapv #(assoc (:candidate %) :g (:g %)) rows)
        beta (get-in f [:law :beta]) posterior-result (try {:posterior (selection/selection-posterior {:beta beta :candidates cs})}
                                                          (catch clojure.lang.ExceptionInfo e (ex-data e)))
        posterior (:posterior posterior-result {})
        action-of (into {} (map (fn [c] [(:id c) (#'policy/cascade-first-action (:id c))]) cs))
        active (into {} (filter (fn [[id p]] (and (pos? p) (some? (action-of id))))) posterior)
        choice (when (seq active) (selection/bayes-choice active action-of))
        diag (when choice (selection/selection-comparisons {:beta beta :candidates cs :posterior posterior :action-of action-of :choice choice}))
        odds (fn [t]
               (let [absent (disj initial t) present (conj absent t) a (logc absent) p (logc present)]
                 {:token t :absent-class (classify absent) :present-class (classify present)
                  :old-odds (Math/exp (double (get w t 0)))
                  :odds (cond (= a p Double/NEGATIVE_INFINITY) :both-ruled-zero
                              (= a Double/NEGATIVE_INFINITY) :infinite
                              (= p Double/NEGATIVE_INFINITY) 0
                              :else (Math/exp (- p a)))}))]
    {:case name :class-masses masses :new-want-counts (into {} (map (fn [[k vs]] [k (count vs)])) groups)
     :log-class-normalizers log-z
     :unrepresented-class-mass (into {} (filter (fn [[k m]] (and (pos? m) (= Double/NEGATIVE_INFINITY (log-z k))))) masses)
     :outcome-domain :frozen-token-powerset-plus-unrepresented-class-atoms
     :selection-refusal (:refusal posterior-result)
     :choice (if choice (update choice :action frozen/label) :no-positive-acting-policy)
     :policy-decided-by (get-in diag [:policy-comparison :decided-by])
     :action-decided-by (get-in diag [:action-comparison :decided-by])
     :odds-at-initial-context (mapv odds (sort-by pr-str (keys w)))
     :rows (mapv (fn [r] (merge (frozen/label (get-in r [:candidate :id]))
                               {:topic-class (topic (get-in r [:candidate :id :target]))
                                :outcome-class (:outcome-class r) :G (:g r)
                                :posterior (get posterior (get-in r [:candidate :id]) 0)})) rows)}))
(def cases [[:local-focus-token-proxy {:focus 1.0 :adjacent 0.0 :irrelevant 0.0 :nothing 0.0}]
            [:elsewhere-5-nothing-zero {:focus (* 0.95 (/ 11.0 18)) :adjacent (* 0.95 (/ 7.0 18)) :irrelevant 0.05 :nothing 0.0}]
            [:elsewhere-5-nothing-10 {:focus (* 0.85 (/ 11.0 18)) :adjacent (* 0.85 (/ 7.0 18)) :irrelevant 0.05 :nothing 0.1}]
            [:literal-60-40-zero {:focus 0.6 :adjacent 0.4 :irrelevant 0.0 :nothing 0.0}]
            [:hard-zero-nothing-10 {:focus 0.54 :adjacent 0.36 :irrelevant 0.0 :nothing 0.1}]
            [:epsilon-nothing-zero {:focus 0.5999994 :adjacent 0.3999996 :irrelevant 0.000001 :nothing 0.0}]
            [:epsilon-nothing-10 {:focus 0.5399994 :adjacent 0.3599996 :irrelevant 0.000001 :nothing 0.1}]
            [:global-split-on-token-powerset-counterexample {:focus 0.55 :adjacent 0.35 :irrelevant 0.05 :nothing 0.05}]])
(def results
 (mapv (fn [run]
         (let [f (frozen/prepare (frozen/fixture run))
               baseline (frozen/evaluate f (repeat (count (:rows f)) [9 1]) :fixed-recorded-G 0)]
           (doseq [[c row] (map vector (:candidates f) (:rows baseline))]
             (assert (frozen/close? (get-in f [:law :posterior (:id c)]) (:posterior row))))
           (doseq [[_ masses] cases] (assert (frozen/close? 1 (reduce + (vals masses)))))
           {:run run :source-sha256 (:sha256 f)
            :baseline (select-keys (frozen/evaluate f (repeat (count (:rows f)) [9 1]) :fixed-recorded-G 0)
                                   [:choice :policy-comparison :action-comparison :rows])
            :cases (mapv (fn [[label masses]] (report-case f label masses)) cases)}))
       ["1789964661" "1789952479"]))
(pp/pprint results)
