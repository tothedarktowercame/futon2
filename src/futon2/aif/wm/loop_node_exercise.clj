(ns futon2.aif.wm.loop-node-exercise
  "Per-click evidence that selected loop machinery ran, rather than merely
  existing in code or appearing in a declared model."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def schema :wm/loop-node-exercise-v1)

(defn- present [node evidence]
  {:node node :status :present :evidence evidence})

(defn- refused [node reason]
  {:node node :status :refused :reason reason})

(defn- decision-bypass [node reason evidence]
  {:node node :status :bypassed :because reason :evidence evidence})

(defn- bypassed [node refusals]
  {:node node
   :status :bypassed
   :decision :selection-abstained
   :because :typed-selection-refusal
   :evidence (mapv #(select-keys % [:target :kind :missing]) refusals)})

(defn- q4-candidate-rows
  "The live outer-policy route (selection-route
  :wm/selected-outer-cascade-policy-v1) retains its candidate space as the
  selection certificate's :q4 rows: one row per admissible policy with its
  identity, per-policy posterior, and the canonical cascade's reading order
  (the multi-step rollout the policy would enact). Project those rows into
  the candidate shape the evaluator reads, keeping only the small fields —
  never the whole canonical cascade."
  [certificate]
  (mapv (fn [row]
          {:policy {:id (:policy-id row) :target (:target-id row)}
           :posterior (:posterior row)
           :reading-order (or (get-in row [:canonical-cascade :reading-order])
                              (get-in row [:canonical-cascade
                                           :structural :reading-order]))})
        (:q4 certificate)))

(defn- selected-entry-class-emission
  "The class emission the live route scored for the selected policy and
  consumed into its G (selection-certificate :selected-outer-cascade-policy
  :selected-entry :class-emission), or nil."
  [certificate]
  (get-in certificate [:selected-outer-cascade-policy
                       :selected-entry :class-emission]))

(defn receipt [decision]
  (let [certificate (:selection-certificate decision)
        candidates (or (:candidates certificate) (:candidates decision)
                       (q4-candidate-rows certificate))
        policies (or (:policies certificate) (:policies decision)
                     (when-let [prior (get-in certificate
                                             [:precision-row :prior])]
                       (keys prior)))
        comparison (or (get-in decision [:selection-law :policy-comparison])
                       (:policy-comparison decision))
        policy-of (fn [candidate]
                    (or (:policy candidate) (:id candidate)
                        (get-in candidate [:f-prefix :policy])))
        horizons (keep #(get-in % [:class-emission :model :horizon]) candidates)
        selected-ce (selected-entry-class-emission certificate)
        horizons (cond-> horizons
                   (and (map? selected-ce)
                        (number? (get-in selected-ce [:model :horizon])))
                   (conj (get-in selected-ce [:model :horizon])))
        precedences
        (let [per-candidate
              (mapv #(mapv (fn [entry]
                             (if (map? entry) (:id entry) entry))
                           (or (:precedence (policy-of %))
                               ;; the live route's per-policy rollout: the
                               ;; canonical cascade's reading order
                               (:reading-order %)))
                    candidates)]
          (or (when (some seq per-candidate) per-candidate)
              (when-let [p (:precedence comparison)]
                [(mapv #(if (map? %) (:id %) %) p)])
              []))
        q4-posteriors (into {} (map (juxt :policy-id :posterior))
                                   (:q4 certificate))
        probabilities (or (get-in certificate [:policy-precision-state :probabilities])
                          (get-in certificate [:precision-family :probabilities])
                          (get-in decision [:policy-precision-state :probabilities])
                          (get-in decision [:selection-law :posterior])
                          ;; the live route retains the per-policy prior and
                          ;; the per-policy posteriors of the :q4 rows
                          (get-in certificate [:precision-row :prior])
                          (when (seq q4-posteriors) q4-posteriors))
        temperature (or (get-in certificate [:policy-precision-state :temperature])
                        (get-in certificate [:precision-family :temperature])
                        (get-in decision [:policy-precision-state :temperature])
                        (get-in decision [:selection-law :beta])
                        (get-in certificate [:precision-row :beta-prior]))
        abstention (get-in decision [:abstention :status])
        typed-abstention? (and (= :abstained (:status decision))
                               (seq (:refusals decision)))
        meta-algorithm? (= :meta-algorithm-selection
                           (get-in decision [:selection-law :applied]))
        by-node
        (cond
          meta-algorithm?
          (into {}
                (map (fn [node]
                       [node (refused node :meta-algorithm-wrong-route)])
                     [:R6 :R13 :R14 :CTAU-CLASS]))

          typed-abstention?
          (into {} (map (fn [node] [node (bypassed node (:refusals decision))])
                        [:R6 :R13 :R14 :CTAU-CLASS]))

          :else
          {:R6 (cond
                 (> (count candidates) 1)
                 (present :R6 {:candidate-count (count candidates)
                               :policy-count (count policies)})

                 (= 1 (count candidates))
                 (decision-bypass :R6 :singleton-admissible-candidate
                                  {:candidate-count 1 :policy-count (count policies)
                                   :candidate (select-keys (policy-of (first candidates))
                                                           [:kind :id :target])})

                 :else (refused :R6 :candidate-space-not-retained))
         :R13 (if (and (some #(> % 1) horizons)
                       (some #(> (count %) 1) precedences))
                (present :R13 {:horizons (vec horizons)
                               :precedences precedences
                               :basis :multi-step-policy-rollout})
                (if (and (= 1 (count candidates))
                         (= 1 (count (first precedences))))
                  (decision-bypass :R13 :single-step-policy
                                   {:horizons (vec horizons)
                                    :precedences precedences})
                  (refused :R13 :policy-rollout-evidence-not-retained)))
         :R14 (if (> (count candidates) 1)
                (if (and (number? temperature) (map? probabilities))
                  (present :R14 {:temperature temperature
                                 :probability-count (count probabilities)
                                 :abstention-status abstention})
                  (refused :R14 :selection-posterior-not-retained))
                (if (and (= 1 (count candidates))
                         (number? temperature) (map? probabilities)
                         (= 1 (count probabilities)))
                  (decision-bypass :R14 :singleton-policy-posterior
                                   {:temperature temperature
                                    :probability-count 1
                                    :candidate (select-keys (policy-of (first candidates))
                                                            [:kind :id :target])})
                  (refused :R14 :selection-posterior-not-retained)))
         :CTAU-CLASS
         (let [scored? (fn [ce]
                         (and (= :class-emission (get-in ce [:model :kind]))
                              (true? (:consumed-in-g? ce))
                              (number? (:risk ce))))
               scored (filterv #(scored? (:class-emission %)) candidates)
               selected-scored? (scored? selected-ce)
               candidate-count (if (and (zero? (count candidates))
                                        selected-scored?)
                                 1
                                 (count candidates))
               scored-count (+ (count scored) (if selected-scored? 1 0))]
           (cond
             (and (pos? candidate-count) (= scored-count candidate-count)
                  (every? #(map? (get-in % [:class-emission :model
                                            :class-preference]))
                          (concat scored
                                  (when selected-scored? [{:class-emission selected-ce}]))))
             (present :CTAU-CLASS
                      {:scored-candidate-count scored-count
                       :comparison-status (:status comparison)
                       :horizons (vec (concat
                                       (keep #(get-in % [:class-emission :model :horizon])
                                             scored)
                                       (when selected-scored?
                                         [(get-in selected-ce [:model :horizon])])))
                       :preference-kind :class-emission
                       :consumption :terminal-class-cost-added-to-G})

             ;; the live route class-scores the selected policy's emission and
             ;; consumes it into its G; the other rows retain no class
             ;; preference. That is exercised machinery over a partially
             ;; scored space — typed as bypassed, never as not-retained.
             (and (pos? scored-count) (< scored-count candidate-count))
             (decision-bypass :CTAU-CLASS :selected-candidate-only-class-scoring
                              {:scored-candidate-count scored-count
                               :candidate-count candidate-count
                               :horizon (when selected-scored?
                                          (get-in selected-ce [:model :horizon]))
                               :preference-kind :class-emission
                               :consumption :terminal-class-cost-added-to-G})

             :else (refused :CTAU-CLASS
                            :class-preference-evidence-not-retained)))})
        counts (frequencies (map :status (vals by-node)))]
    {:schema schema
     :status (if (zero? (get counts :refused 0)) :complete :incomplete)
     :by-node by-node
     :counts (merge {:present 0 :bypassed 0 :refused 0} counts)}))

(defn require-complete!
  "Compatibility entry point: annotate DECISION with measured node exercise.
  Under the 2026-10-19 frontier ruling, missing evidence is not a veto.
  :refused counts remain evidence of absence, never a claim of exercise."
  [decision]
  (assoc-in decision [:selection-certificate :loop-node-exercise]
            (receipt decision)))
