(ns futon2.aif.cascade-efe-sut
  "Controlled composition of the accepted 3/2, order, preference and Prefix-F seams."
  (:require [futon2.aif.application-order-certificate :as application-order]
            [futon2.aif.factorized-preference-sut :as preference]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.policy :as policy]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.aif.prefix-free-energy-sut :as prefix-f]
            [futon2.aif.three-halves-policy :as three-halves]
            [futon2.aif.wm.cascade-decision :as decision]))

(def schema :wm/cascade-efe-controlled-v1)
(defn- refuse [reason & [details]]
  {:schema schema :status :refused :reason reason :details details})
(defn- finite-number? [x] (and (number? x) (Double/isFinite (double x))))
(defn- distribution? [m]
  (and (map? m) (seq m) (every? finite-number? (vals m))
       (= 1 (reduce + (vals m)))))
(defn- subsets [tokens]
  (reduce (fn [ss t] (into ss (map #(conj % t) ss))) [#{}]
          (sort-by pr-str tokens)))
(defn- enabled? [state {:keys [guard produces]}]
  (and (every? state (:needs guard))
       (not-any? state (:forbids guard))
       (not-every? state produces)))
(defn- derived-transition [state-space interpretation]
  (let [produces (:produces interpretation)]
    (into {} (for [state state-space]
               [state {(if (enabled? state interpretation)
                         (into state produces)
                         state) 1}]))))
(defn- pinned? [x captured]
  (three-halves/declaration-source-valid? (:source x) x captured))

(defn- dag? [nodes edges]
  (letfn [(visit [u visiting done]
            (cond (visiting u) nil (done u) done
                  :else (reduce (fn [d [_ v]] (when d (visit v (conj visiting u) d)))
                                (conj done u) (filter #(= u (first %)) edges))))]
    (boolean (reduce (fn [done n] (when done (visit n #{} done))) #{} nodes))))

(defn compose!
  [{:keys [mode problem order-control predictive preference-input
           prefix-input prefix-context beta]}]
  (if-not (= :controlled-sut mode)
    (refuse :production-registries-unconfigured)
    (let [ordered (decision/controlled-application-order-run order-control problem)]
      (if-not (= :prepared-for-enactment (:status ordered))
        (refuse :application-order-refused ordered)
        (let [policy-carrier (:policy order-control)
              captured (:captured-sources order-control)
              sequence (get-in ordered [:application-order :computed :pattern-sequence])
              squares (mapv :square-id (:squares policy-carrier))
              expected-construction (application-order/policy-construction-order policy-carrier)
              transitions (:transitions predictive)
              state-space (:state-space predictive)
              declared-universe (:universe preference-input)
              selector-authority (:selector-authority predictive)
              additions (:organizer-additions predictive)
              all-edges (vec (concat expected-construction additions))
              structural-gaps
              (cond-> []
                (not (pinned? predictive captured)) (conj :predictive-source-pin)
                (not= squares (:nodes predictive)) (conj :node-population)
                (not= expected-construction (:construction-edges predictive))
                (conj :construction-edges)
                (not= sequence (:authorized-precedence predictive))
                (conj :authorized-precedence)
                (not (pos-int? selector-authority)) (conj :selector-authority)
                (not= (set state-space) (set (subsets declared-universe)))
                (conj :finite-state-space)
                (not (dag? (range (count squares)) all-edges)) (conj :organizer-cycle)
                (not (pos-int? (:horizon predictive))) (conj :finite-horizon)
                (not= (:horizon predictive) (get-in problem [:cascade-problem :horizon-steps]))
                (conj :horizon-mismatch)
                (not (and (pinned? (:initial-state predictive) captured)
                          (pos-int? (get-in predictive [:initial-state :authority]))
                          (not= selector-authority
                                (get-in predictive [:initial-state :authority]))
                          (some? (get-in predictive [:initial-state :implementation-pin]))
                          (distribution? (get-in predictive [:initial-state :kernel]))))
                (conj :initial-state-kernel)
                (not (and (pinned? (:observation predictive) captured)
                          (pos-int? (get-in predictive [:observation :authority]))
                          (not= selector-authority
                                (get-in predictive [:observation :authority]))
                          (some? (get-in predictive [:observation :implementation-pin]))
                          (map? (get-in predictive [:observation :kernel]))))
                (conj :observation-kernel)
                (not= (set sequence) (set (keys transitions))) (conj :transition-population)
                (some (fn [[pattern-id t]]
                        (let [interpretation (get-in problem
                                                     [:cascade-problem :interpretations pattern-id])]
                          (not (and (pinned? t captured)
                                    (pos-int? (:authority t))
                                    (not= selector-authority (:authority t))
                                    (some? (:implementation-pin t))
                                    (= (:guard t) (:guard interpretation))
                                    (= (:produces t) (:produces interpretation))
                                    (= (:kernel t)
                                       (derived-transition state-space interpretation))))))
                      transitions)
                (conj :transition-kernel))]
          (if (seq structural-gaps)
            (refuse (first structural-gaps) structural-gaps)
            (let [preferred (preference/compose-and-score! preference-input)]
              (if-not (= :scored (:status preferred))
                (refuse :factorized-preference-refused preferred)
                (let [certified-observation (get-in predictive [:observation :kernel])
                      consumed-observation (get-in preferred [:kernel :probabilities])
                      initial (get-in predictive [:initial-state :kernel])
                      node-rows (mapcat #(get-in % [:certificate :node-evaluations])
                                        (:scored preferred))
                      transition-consumed?
                      (and (= (* (count (:scored preferred)) (:horizon predictive))
                              (count node-rows))
                           (every?
                            (fn [node]
                              (and (seq (:states node))
                                   (every? (fn [{:keys [pattern-id state kernel]}]
                                             (= kernel
                                                (get-in transitions
                                                        [pattern-id :kernel state])))
                                           (:states node))))
                            node-rows))]
                  (if (or (not= certified-observation consumed-observation)
                          (not= initial (get-in preference-input [:state :cascade-belief]))
                          (not transition-consumed?))
                    (refuse :predictive-kernel-consumption-mismatch)
                    (let [prefix-result (prefix-f/compose-menu!
                                         (assoc prefix-input :ranked (:scored preferred))
                                         prefix-context)]
                      (if-not (= :ready-for-posterior (:status prefix-result))
                        (refuse :prefix-f-refused prefix-result)
                        (let [candidate-keys (mapv prefix-admission/candidate-key
                                                   (:candidates prefix-input))
                              posterior-keys (mapv #(prefix-admission/candidate-key (:action %))
                                                   (:posterior-input prefix-result))]
                          (if (not= candidate-keys posterior-keys)
                            (refuse :prefix-f-population-mismatch)
                            (let [posterior (policy/select-action-cascades
                                             (:posterior-input prefix-result) {:beta beta})
                                  selected (:action posterior)
                                  selected-key (prefix-admission/candidate-key selected)
                                  expected-key (first candidate-keys)
                                  dispatch-key (prefix-admission/candidate-key
                                                (assoc (get-in ordered [:lane :selected-action])
                                                       :target (:target selected)))
                                  ids {:constructed expected-key
                                       :composition-admitted expected-key
                                       :predictive-admitted expected-key
                                       :scored (prefix-admission/candidate-key
                                                (:action (first (:scored preferred))))
                                       :posterior selected-key :selected selected-key
                                       :dispatch-prepared dispatch-key
                                       :enacted-plan dispatch-key}
                                  digests (update-vals ids evidence/value-digest)]
                              (if (or (some nil? (vals ids))
                                      (not= 1 (count (set (vals ids)))))
                                (refuse :policy-identity-divergence ids)
                                {:schema schema :status :prepared-for-enactment
                                 :structure {:nodes squares :construction-edges expected-construction
                                             :organizer-additions additions
                                             :authorized-precedence sequence}
                                 :predictive predictive :preference preferred
                                 :prefix-f prefix-result :posterior posterior
                                 :order ordered :identity-chain ids
                                 :identity-digests digests
                                 :efe-terms
                                 (mapv (fn [row]
                                         {:G-efe (:G-efe row)
                                          :risk (get-in row [:certificate :g-terms :risk])
                                          :ambiguity (get-in row [:certificate :g-terms :ambiguity])
                                          :EIG (get-in row [:certificate :g-terms
                                                            :expected-information-gain])
                                          :preference (get-in row [:certificate :consumed-g :C])
                                          :observation-model
                                          (get-in row [:certificate :consumed-g :A])})
                                       (:scored preferred))}))))))))))))))))
