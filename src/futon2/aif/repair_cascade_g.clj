(ns futon2.aif.repair-cascade-g
  "Pure runtime mirror of RepairCascadeG at mathlib4 c9b0cc200.
  No route wiring, model discovery, authority minting, or I/O."
  (:require [futon2.aif.cascade-coapply-g :as coapply]
            [futon2.aif.finite-step-g :as finite]))

(def pattern-ids
  [:a-correct-refusal-is-not-the-defect
   :a-different-seat-reviews
   :a-step-that-did-nothing-is-an-observation
   :fix-the-red-before-the-repair
   :introduce-the-pattern-from-the-repair
   :locate-the-producer-first
   :match-the-finding-against-the-library
   :record-the-finding-with-its-target
   :repair-the-producer-not-the-checker
   :reproduce-on-retained-inputs
   :retire-by-appending-to-the-ledger
   :retry-only-the-repaired-phase
   :take-in-what-no-step-predicted
   :witness-the-path-not-the-helper])

(def outcomes
  [:closure :grounded-progress :useful-blocker-discovery
   :downstream-unblocking :information-gain :elapsed-ms :token-use
   :repeated-target :abstention :unreviewable-output :injury-persistence])

(def model-provenance
  {:kind :lean-model
   :definition "DarkTower.WarMachine.RepairCascadeG.costKernel"
   :commit "3b7c08368ea4fea599b2159b916759aabea03daa"})

(def genuine-repair
  {:id :genuine-repair
   :patterns [:record-the-finding-with-its-target
              :locate-the-producer-first
              :reproduce-on-retained-inputs
              :a-correct-refusal-is-not-the-defect
              :fix-the-red-before-the-repair
              :repair-the-producer-not-the-checker
              :witness-the-path-not-the-helper
              :a-different-seat-reviews
              :retire-by-appending-to-the-ledger]})

(def historical-misclassification
  {:id :historical-misclassification
   :patterns [:locate-the-producer-first
              :reproduce-on-retained-inputs
              :a-correct-refusal-is-not-the-defect
              :retire-by-appending-to-the-ledger]})

(defn initial-repair-state [injury]
  {:injury injury :recorded true :producer-located false :reproduced false
   :decision-made false :red-boundary false :producer-fixed false
   :path-witnessed false :reviewed false :retired false})

(defn guard [pattern state]
  (case pattern
    :record-the-finding-with-its-target true
    :locate-the-producer-first (:recorded state)
    :reproduce-on-retained-inputs (:recorded state)
    :a-correct-refusal-is-not-the-defect
    (and (:producer-located state) (:reproduced state))
    :fix-the-red-before-the-repair
    (and (:reproduced state) (:decision-made state) (= :real-defect (:injury state)))
    :repair-the-producer-not-the-checker
    (and (:producer-located state) (:decision-made state) (:red-boundary state))
    :witness-the-path-not-the-helper (:producer-fixed state)
    :a-different-seat-reviews (and (:red-boundary state) (:path-witnessed state))
    :retire-by-appending-to-the-ledger
    (or (:reviewed state) (and (:reproduced state) (= :never-defect (:injury state))))
    :a-step-that-did-nothing-is-an-observation true
    :introduce-the-pattern-from-the-repair (:reviewed state)
    :match-the-finding-against-the-library (:recorded state)
    :retry-only-the-repaired-phase (and (:reviewed state) (:producer-fixed state))
    :take-in-what-no-step-predicted (:recorded state)
    false))

(defn effect [pattern state]
  (case pattern
    :record-the-finding-with-its-target (assoc state :recorded true)
    :locate-the-producer-first (assoc state :producer-located true)
    :reproduce-on-retained-inputs (assoc state :reproduced true)
    :a-correct-refusal-is-not-the-defect (assoc state :decision-made true)
    :fix-the-red-before-the-repair (assoc state :red-boundary true)
    :repair-the-producer-not-the-checker (assoc state :producer-fixed true)
    :witness-the-path-not-the-helper (assoc state :path-witnessed true)
    :a-different-seat-reviews (assoc state :reviewed true)
    :retire-by-appending-to-the-ledger (assoc state :retired true)
    state))

(defn apply-pattern [state pattern]
  (if (guard pattern state) (effect pattern state) state))

(defn apply-cascade [cascade state]
  (reduce apply-pattern state (:patterns cascade)))

(defn predicted-outcome [state]
  (cond
    (or (:retired state) (and (:producer-fixed state) (:path-witnessed state))) :closure
    (and (:reproduced state) (:decision-made state)) :useful-blocker-discovery
    :else :injury-persistence))

(defn admitted-steps [injury cascade]
  (loop [state (initial-repair-state injury), patterns (:patterns cascade), admitted []]
    (if-let [pattern (first patterns)]
      (if (guard pattern state)
        (recur (effect pattern state) (next patterns) (conj admitted pattern))
        (recur state (next patterns) admitted))
      admitted)))

(defn admitted-count [injury cascade]
  (count (admitted-steps injury cascade)))

(defn- refuse! [reason data]
  (throw (ex-info "Repair cascade G refused"
                  (merge {:status :refused :reason reason} data))))

(defn validate-cost-model [{:keys [max-length w] :as cost-model}]
  (when-not (and (integer? max-length) (not (neg? max-length))
                 (number? w) (Double/isFinite (double w)) (not (neg? w)))
    (refuse! :invalid-cost-model {:cost-model cost-model}))
  (when (> (* w max-length) 1)
    (refuse! :cost-bound-exceeded {:cost-model cost-model}))
  cost-model)

(defn cost-kernel [cost-model cascade]
  (let [{:keys [max-length w]} (validate-cost-model cost-model)]
    (when (> (count (:patterns cascade)) max-length)
      (refuse! :cascade-too-long {:cascade-id (:id cascade) :max-length max-length}))
    (into {}
          (for [injury [:real-defect :never-defect]
                :let [n (admitted-count injury cascade)
                      terminal (predicted-outcome
                                (apply-cascade cascade (initial-repair-state injury)))
                      token-mass (* w n)]]
            [injury (assoc (zipmap outcomes (repeat 0))
                           :token-use token-mass
                           terminal (- 1 token-mass))]))))

(defn cascade-novelty [injury cascade pattern-counts]
  (reduce + 0 (map #(coapply/beta-novelty
                     (or (get pattern-counts %)
                         (refuse! :missing-pattern-counts {:pattern %})))
                   (admitted-steps injury cascade))))

(defn score
  [{:keys [injury-belief cascade cost-model preference preference-provenance
           pattern-counts]}]
  (when-not (and (map? preference-provenance) (seq preference-provenance))
    (refuse! :missing-preference-provenance {}))
  (let [kernel (cost-kernel cost-model cascade)
        terms (finite/step-g {:q injury-belief :kernel kernel :preference preference})
        novelty (reduce + 0
                        (for [[injury mass] injury-belief]
                          (* mass (cascade-novelty injury cascade pattern-counts))))
        g (:g terms)]
    (merge terms
           {:cascade-id (:id cascade)
            :model-provenance model-provenance
            :preference-provenance preference-provenance
            :novelty novelty
            :score (if (= :inadmissible g) :inadmissible (- g novelty))
            :admitted-steps (into {} (for [injury (keys injury-belief)]
                                       [injury (admitted-steps injury cascade)]))})))

(defn select [input cascades]
  (let [terms (mapv #(score (assoc input :cascade %)) cascades)
        admissible (filterv #(number? (:score %)) terms)]
    (if (empty? admissible)
      {:status :refused :reason :no-admissible-cascade :cascades terms}
      (let [best (apply min (map :score admissible))
            tied (filterv #(= best (:score %)) admissible)
            ids (mapv :cascade-id tied)]
        {:status (if (= 1 (count tied)) :selected :tie)
         :chosen-id (when (= 1 (count tied)) (first ids))
         :tied-ids (when (< 1 (count tied)) ids)
         :cascades terms
         :admitted-steps (into {} (map (juxt :cascade-id :admitted-steps) terms))}))))
