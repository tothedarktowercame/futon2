(ns futon2.aif.observation-rates
  "WM-04 S-3: adjudication rates from admitted labels, as raw exact-rational
  counts. No default prior, no smoothing.

  Roles (WM-04 S-2/S-3, correction of 2026-09-17):

    observation procedure = the RECORDED VERDICT — what the step's record
      claimed (e.g. TRACE's :realised);
    reference judgement   = the ADMITTED LABEL — the blinded, @R9-reviewed
      label admitted by futon2.aif.observation-admission (S-2's admit).

  An error count exists only where BOTH are present for the same subject.
  A label with an admitted reference but no recorded verdict adds no error
  count; it counts towards coverage only. A label with a recorded verdict
  but no admitted reference is not a comparison at all.

  Correspondence to Lean (mathlib4 889429e6bf,
  DarkTower/WarMachine/TokenObservation.lean, AdjudicationRates): the
  reference judgement is the STATE s, the recorded verdict is the
  OBSERVATION o, so

    false-neg = P(recorded verdict = false | admitted = :present)
    false-pos = P(recorded verdict = true  | admitted = :absent)

  matching tokenLikelihood's conditioning exactly.

  Rates are reported as raw counts, numerator/denominator, exact rationals;
  a zero denominator is :unobserved for that cell (never 0, never defaulted).
  A prior is applied only when passed in explicitly with an :authority;
  an unauthorised prior is refused.")

(defn- cell
  "One error-rate cell from the labels that condition it (cls, keep).
  Numerator = labels kept whose recorded verdict is event; denominator =
  labels kept with a recorded verdict present at all. Labels without a
  recorded verdict are dropped from both counts (they carry coverage only).
  Zero denominator => :unobserved for this cell."
  [cls keep event]
  (let [compared (filter #(some? (:recorded %)) (filter keep cls))
        n (count compared)]
    (if (zero? n)
      {:status :unobserved}
      {:numerator (count (filter event compared))
       :denominator n
       :rate (/ (count (filter event compared)) n)})))

(def ^:private admit-absent #(= :absent (:admitted %)))
(def ^:private admit-present #(= :present (:admitted %)))

(defn- prior-cell
  "Posterior mean (numerator+alpha)/(denominator+alpha+beta) for an observed
  cell under an explicit prior; :unobserved cells stay :unobserved (a prior
  never turns an unobserved cell into a rate by itself — the assembly layer
  decides whether a prior-bearing unobserved class is usable)."
  [prior observed]
  (if (or (nil? prior) (= :unobserved (:status observed)))
    observed
    (assoc observed :posterior-mean
           (/ (+ (:numerator observed) (:alpha prior))
              (+ (:denominator observed) (:alpha prior) (:beta prior))))))

(defn- check-prior
  "A prior must be nil (raw counts only) or {:alpha a :beta b :authority s}
  with positive rational/integer a, b and a non-blank :authority. Anything
  else is the typed refusal {:status :missing :kind :invalid-prior}."
  [prior]
  (when (and prior (not (and (or (ratio? (:alpha prior)) (integer? (:alpha prior)))
                             (pos? (:alpha prior))
                             (or (ratio? (:beta prior)) (integer? (:beta prior)))
                             (pos? (:beta prior))
                             (string? (:authority prior))
                             (seq (.trim ^String (:authority prior))))))
    {:status :missing :kind :invalid-prior :prior prior}))

(defn rates-by-class
  "Raw error-rate counts per token class. Each label is
  {:token-class c :recordd r :admitted a} where r is the recorded verdict
  (true/false, or absent) and a is the admitted reference label
  (:present/:absent, or absent when not admitted). Returns
  {class {:false-pos {:numerator k :denominator n :rate k/n}
          :false-neg {...}
          :coverage labels-in-class / subjects-in-class}}
  with :unobserved cells for zero denominators. A class with no admitted
  labels at all is wholly {:status :unobserved}. A missing subject count is
  the typed :unknown-subject-count refusal. When a prior is supplied it is
  recorded as :prior on each class entry and adds :posterior-mean to
  observed cells; an invalid or unauthorised prior is the typed
  :invalid-prior refusal for the whole call."
  ([labels subjects] (rates-by-class labels subjects nil))
  ([labels subjects prior]
   (if-let [bad (check-prior prior)]
     bad
     (into {}
           (map (fn [[c cls]]
                  (let [admitted (filter :admitted cls)
                        entry (if (empty? admitted)
                                {:status :unobserved}
                                {:false-pos (prior-cell prior (cell cls admit-absent (fn [l] (true? (:recorded l)))))
                                 :false-neg (prior-cell prior (cell cls admit-present (fn [l] (false? (:recorded l)))))})
                        n-subjects (get subjects c ::absent)]
                    [c (if (= ::absent n-subjects)
                         {:status :missing :kind :unknown-subject-count :class c}
                         (cond-> entry
                           prior (assoc :prior prior)
                           true (assoc :coverage (/ (count cls) n-subjects))))])))
           (group-by :token-class labels)))))

(defn- contract-classes
  "Class-id → class map from the S-1 observation contract
  (resources/wm/observation-contract.edn, :wm/observation-contract-v1)."
  [contract]
  (into {} (map (fn [cls] [(:id cls) cls])) (:classes contract)))

(defn- usable-rate
  "The rate a judgement class contributes to the kernel: the prior-adjusted
  posterior mean when an authorised prior is recorded, else the raw rate.
  An :unobserved cell has no usable rate."
  [cell]
  (when-not (= :unobserved (:status cell))
    (or (:posterior-mean cell) (:rate cell))))

(defn token-likelihood-rates
  "Assemble a rate map for cascade-model-manifest/token-likelihood over a
  universe. token-classes is {token class-id} for every token in the
  universe; contract is the S-1 observation contract. Tokens in a :checkable
  class get the exact zero kernel {:false-neg 0 :false-pos 0 :basis
  :checkable} (tokenLikelihood_checkable). Tokens in a :judgement class get
  the class's usable rate with :basis :estimated (or :prior when a prior is
  recorded). A judgement class with no rate entry, or with any :unobserved
  cell and no prior making it usable, is the typed :unsupported-class
  refusal — never a default. A class id absent from the contract is
  :unknown-class."
  [rates contract token-classes]
  (let [by-id (contract-classes contract)]
    (reduce-kv (fn [acc token class-id]
                 (if-not (contains? by-id class-id)
                   (reduced {:status :missing :kind :unknown-class
                             :class class-id :token token})
                   (let [cls (get by-id class-id)]
                     (if (= :checkable (:kind cls))
                       (assoc acc token {:false-neg 0 :false-pos 0 :basis :checkable})
                       (let [r (get rates class-id)
                             fn-rate (some-> r :false-neg usable-rate)
                             fp-rate (some-> r :false-pos usable-rate)]
                         (if (and fn-rate fp-rate)
                           (assoc acc token {:false-neg fn-rate
                                             :false-pos fp-rate
                                             :basis (if (:prior r) :prior :estimated)})
                           (reduced {:status :missing :kind :unsupported-class
                                     :class class-id :token token})))))))
               {}
               token-classes)))
