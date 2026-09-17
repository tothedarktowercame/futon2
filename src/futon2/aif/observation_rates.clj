(ns futon2.aif.observation-rates
  "WM-04 S-3: adjudication rates from admitted labels, as exact rationals
  under a declared Beta(1,1) prior, and assembly into a
  cascade-model-manifest/token-likelihood rate map. No smoothing beyond the
  declared Beta(1,1); no default rates for unobserved classes.

  Lean correspondence (mathlib4 889429e6bf,
  DarkTower/WarMachine/TokenObservation.lean, AdjudicationRates): falseNeg v
  is the probability that an established token v is missed (v ∈ s, v ∉ o);
  falsePos v is the probability that a non-established token v is reported
  (v ∉ s, v ∈ o). The adjudicated label is the STATE; the recorded verdict
  is the OBSERVATION. Therefore here:

    false-neg = P(recorded verdict = false | admitted label = :present)
    false-pos = P(recorded verdict = true  | admitted label = :absent)

  which matches tokenLikelihood's conditioning exactly: in A(o|s) the state s
  carries the admitted label and the observation o carries the recorded
  verdict.")

(def ^:const beta-prior
  "Declared prior for every rate cell: Beta(1,1). Posterior mean with k
  matching events out of n admitted labels is (k+1)/(n+2), an exact
  rational. Recorded here so no other smoothing can be introduced silently."
  {:alpha 1 :beta 1})

(defn- posterior-mean
  "Exact-rational posterior mean of Beta(1,1) after k events in n trials."
  [k n]
  (/ (+ k (:alpha beta-prior))
     (+ n (:alpha beta-prior) (:beta beta-prior))))

(defn- class-entry
  "One class's rates from its admitted labels. The label fields are
  {:token-class c :recorded r :admitted a} with r boolean and a
  :present/:absent. admitted-absent labels condition false-pos (a recorded
  'present' claim is a false positive); admitted-present labels condition
  false-neg (a recorded 'absent' claim is a false negative). A class with
  zero admitted labels is :unobserved and carries no rate at all — never
  zero, never defaulted. Zero observed errors still yields a posterior rate
  (posterior-mean 0 n) = 1/(n+2), not 0."
  [labels]
  (let [admitted-absent (filter #(= :absent (:admitted %)) labels)
        admitted-present (filter #(= :present (:admitted %)) labels)
        n-absent (count admitted-absent)
        n-present (count admitted-present)]
    (if (zero? (+ n-absent n-present))
      {:status :unobserved :labels (count labels)}
      {:false-pos (posterior-mean (count (filter true? (map :recorded admitted-absent))) n-absent)
       :false-neg (posterior-mean (count (filter false? (map :recorded admitted-present))) n-present)
       :denominators {:admitted-absent n-absent :admitted-present n-present}
       :labels (count labels)})))

(defn rates-by-class
  "Adjudication rates per token class from admitted labels. Each label is
  {:token-class c :recorded r :admitted a} (see class-entry). Returns
  {class {:false-pos rational :false-neg rational
          :denominators {:admitted-absent n :admitted-present m}
          :labels k :coverage rational}}
  where coverage = labels-in-class / subjects-in-class, requiring a
  subjects map {class subject-count}; a class present in labels but missing
  from subjects is the typed refusal
  {:status :missing :kind :unknown-subject-count}. A class with zero
  admitted labels is {:status :unobserved :labels k} with no rates."
  [labels subjects]
  (into {}
        (map (fn [[c cls]]
               (let [entry (class-entry cls)
                     n-subjects (get subjects c ::absent)]
                 [c (if (= ::absent n-subjects)
                      {:status :missing :kind :unknown-subject-count :class c}
                      (assoc entry :coverage (/ (count cls) n-subjects)))])))
        (group-by :token-class labels)))

(defn- contract-classes
  "Class-id → class map from the S-1 observation contract
  (resources/wm/observation-contract.edn, :wm/observation-contract-v1)."
  [contract]
  (into {} (map (fn [cls] [(:id cls) cls])) (:classes contract)))

(defn token-likelihood-rates
  "Assemble a rate map for cascade-model-manifest/token-likelihood over a
  universe. token-classes is {token class-id} for every token in the
  universe; contract is the S-1 observation contract. Tokens in a :checkable
  class get the exact zero kernel {:false-neg 0 :false-pos 0 :basis
  :checkable} (tokenLikelihood_checkable). Tokens in a :judgement class get
  the estimated rates with :basis :estimated. A judgement class with no
  rate entry, or one that is :unobserved, is the typed refusal
  {:status :missing :kind :unsupported-class} — never a default rate. A
  class id absent from the contract is :unknown-class, and a token absent
  from token-classes is not in the universe (token-likelihood itself
  refuses state/observation tokens with no rate entry)."
  [rates contract token-classes]
  (let [by-id (contract-classes contract)]
    (reduce-kv (fn [acc token class-id]
                 (if-not (contains? by-id class-id)
                   (reduced {:status :missing :kind :unknown-class
                             :class class-id :token token})
                   (let [cls (get by-id class-id)]
                     (if (= :checkable (:kind cls))
                       (assoc acc token {:false-neg 0 :false-pos 0 :basis :checkable})
                       (let [r (get rates class-id)]
                         (cond
                           (nil? r) (reduced {:status :missing :kind :unsupported-class
                                              :class class-id :token token})
                           (= :unobserved (:status r)) (reduced {:status :missing :kind :unsupported-class
                                                                 :class class-id :token token})
                           :else (assoc acc token {:false-neg (:false-neg r)
                                                   :false-pos (:false-pos r)
                                                   :basis :estimated})))))))
               {}
               token-classes)))
