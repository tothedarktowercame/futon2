(ns futon2.aif.cascade-coapply-g
  "Lean GOverCascades.cascadeGrainG: one comparison horizon, co-application,
  raw risk + ambiguity minus state/frontier-weighted Beta novelty.
  Counts are explicit positive [a b] pairs per occurrence, not inferred priors.
  This is a scoring component, not an admission or execution certificate."
  (:require [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.cascade-observation-scoring :as scorer]))

(defn- refuse! [reason data]
  (throw (ex-info "Canonical co-application G refused"
                  (merge {:status :refused :reason reason} data))))

(defn beta-novelty
  "PatternThetaNovelty.novelty_eq: approximate information, NOT exact Beta KL."
  [counts]
  (when-not (and (vector? counts) (= 2 (count counts))
                 (every? #(and (number? %) (Double/isFinite (double %)) (pos? %)) counts)
                 (Double/isFinite (double (reduce + counts))))
    (refuse! :invalid-pattern-counts {:counts counts}))
  (/ 1 (reduce + counts) 2))

(defn- acyclic? [units descent]
  (loop [remaining (set units)]
    (if (empty? remaining) true
      (let [below (set (for [[a b] descent :when (and (remaining a) (remaining b))] b))
            roots (remove below remaining)]
        (and (seq roots) (recur (apply disj remaining roots)))))))

(defn- candidate! [candidate counts]
  (let [p (:precedence candidate)
        {:keys [units descent patterns]} (:co-apply p)]
    (when-not (and (map? p) (= #{:co-apply} (set (keys p)))
                   (vector? units) (seq units) (= (count units) (count (distinct units)))
                   (vector? descent)
                   (every? #(and (vector? %) (= 2 (count %))
                                 (every? (set units) %)) descent)
                   (map? patterns) (= (set units) (set (keys patterns)))
                   (every? map? (vals patterns)))
      (refuse! :invalid-co-application {:candidate (:id candidate)}))
    (when-not (acyclic? units descent)
      (refuse! :cyclic-containment {:candidate (:id candidate)}))
    (when-not (and (map? counts) (= (set units) (set (keys counts))))
      (refuse! :pattern-counts-missing {:candidate (:id candidate)}))
    (doseq [pair (vals counts)] (beta-novelty pair))))

(defn expected-novelty
  "CascadeCoApplyGNovelty.expectedNovelty, evaluated at the PRE-step belief."
  [candidate counts belief]
  (let [{:keys [units descent patterns]} (get-in candidate [:precedence :co-apply])]
    (reduce + 0
            (for [[state mass] belief]
              (* mass (reduce + 0 (map #(beta-novelty (counts %))
                                       (model/enabled-frontier units descent patterns state))))))))

(defn compare-at-horizon
  "Score a family at opts' single explicit :horizon-steps, as compareAtHorizonN.
  opts :pattern-counts maps candidate IDs to occurrence -> [a b]. The caller
  supplies the current belief, class channel and observation; no synthetic
  observation, q0, horizon, class or count is invented here.
  Conflicting normalization/information modes refuse rather than get ignored."
  [state candidates opts]
  (try
    (when-not (and (vector? candidates) (seq candidates)
                   (every? :id candidates)
                   (= (count candidates) (count (distinct (map :id candidates)))))
      (refuse! :invalid-candidate-family {}))
    (when-not (= :class-emission (get-in opts [:observation-model :kind]))
      (refuse! :class-observation-required {}))
    (when (or (and (contains? opts :g-normalization)
                   (not= :raw-cumulative (:g-normalization opts)))
              (and (contains? opts :parameter-information-mode)
                   (not= :frontier-beta-approximation (:parameter-information-mode opts))))
      (refuse! :conflicting-score-law {}))
    (doseq [c candidates] (candidate! c (get-in opts [:pattern-counts (:id c)])))
    (let [ranked (scorer/rank-cascade-actions state candidates
                    (assoc opts :g-normalization :raw-cumulative :parameter-information-mode :none
                                :require-finite-risk? true))]
      (if-not (vector? ranked) ranked
        (let [entries
              (mapv (fn [e]
                      (let [c (:action e) counts (get-in opts [:pattern-counts (:id c)])
                            steps (get-in e [:certificate :steps])
                            before (cons (:cascade-belief state) (map :belief (butlast steps)))
                            terms (mapv (fn [step q]
                                          {:tau (:tau step) :before q
                                           :expected-novelty (expected-novelty c counts q)}) steps before)
                            novelty (reduce + 0 (map :expected-novelty terms))
                            g (- (:G-efe e) novelty)]
                        (-> e (dissoc :rank :g-tie)
                            (assoc :controller-score g :G-efe g :G-cascade g)
                            (assoc-in [:certificate :steps]
                                      (mapv (fn [step term]
                                              (assoc step :expected-information-gain (:expected-novelty term)
                                                     :g (- (:g step) (:expected-novelty term)))) steps terms))
                            (assoc-in [:certificate :g-terms :expected-information-gain] novelty)
                            (assoc-in [:certificate :g-terms :raw :expected-information-gain] novelty)
                            (assoc-in [:certificate :parameter-novelty]
                                      {:law :frontier-beta-approximation :lean "CascadeCoApplyGNovelty"
                                       :counts counts :steps terms :total novelty})))) ranked)
              sorted (sort-by :controller-score entries)
              ranks (zipmap (distinct (map :controller-score sorted)) (iterate inc 1))]
          (with-meta
            (mapv (fn [e]
                    (let [ties (filter #(= (:controller-score e) (:controller-score %)) sorted)]
                      (cond-> (assoc e :rank (ranks (:controller-score e)))
                        (< 1 (count ties)) (assoc :g-tie (mapv :cascade-id ties))))) sorted)
            (assoc (meta ranked) :formal-contract :cascade-coapply-g-novelty)))))
    (catch clojure.lang.ExceptionInfo e (ex-data e))))
