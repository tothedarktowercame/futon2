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

(defn- counts-produce-rate?
  "The cell's :rate is the ratio of its own counts: integer :numerator over
  positive integer :denominator, and :rate EQUAL to that ratio. The counts
  are what `cell` divided, so the ratio is exact; a :rate that only equals
  it numerically (0.5 beside 1/2) was written by a hand, not divided from
  these counts, and does not pass here."
  [cell]
  (and (integer? (:numerator cell))
       (integer? (:denominator cell))
       (pos? (:denominator cell))
       (= (:rate cell) (/ (:numerator cell) (:denominator cell)))))

(defn- measured-cell
  "What a cell contributes to the kernel, and when it contributes nothing,
  why. Three outcomes:

  - {:rate r} — its counts produce its rate (see
    `counts-produce-rate?`), so r is its usable rate: the prior-adjusted
    posterior mean when an authorised prior recorded one, else the raw
    rate;
  - {:status :unobserved} — no comparison was made in this cell. An
    absence, neither a measurement nor a claim of one;
  - {:status :declared-not-measured} — the cell carries a rate its counts
    do not produce: a bare :rate with no counts, counts with no :rate, or
    counts that divide to something else.

  A-S section 5 (proof2/packets/A-S.md) states the falsifier this
  distinguishes: \"a rate table with no counts, or with declared numbers
  wearing the shape of measured ones, is not measured and must be
  refused.\" The case for refusing rather than consuming: a rate consumed
  as measured that was never counted is a value standing in for an
  absence, and the kernel cannot tell the two apart afterwards -- the
  number reaches G either way, with :basis :estimated on the record."
  [cell]
  (cond
    (= :unobserved (:status cell)) {:status :unobserved}
    (counts-produce-rate? cell) {:rate (or (:posterior-mean cell) (:rate cell))}
    :else {:status :declared-not-measured}))

(defn- cell-counts [cell]
  (select-keys cell [:numerator :denominator]))

(defn token-likelihood-rates
  "Assemble a rate map for cascade-model-manifest/token-likelihood over a
  universe. token-classes is {token class-id} for every token in the
  universe; contract is the S-1 observation contract. Every class, checkable
  or judgement, is read from RATES first (H-A-CONSUMER-I):

  - both cells usable: the class's usable rates, :basis :estimated (or
    :prior when a prior is recorded), and :counts {:false-neg {:numerator n
    :denominator d} :false-pos {...}} read from the cells;
  - an entry with a cell :unobserved, or any other entry that yields no
    usable pair: the typed :unsupported-class refusal. A measured cell is
    never discarded, a partial measurement never padded. When a cell
    carried a rate its own counts do not produce (A-S section 5's declared
    number wearing a measured shape, see `measured-cell`) the refusal
    carries :cell-reason :declared-not-measured — a reason on the existing
    refusal, not a second kind of refusal;
  - no entry, or the whole entry {:status :unobserved}: a :checkable class
    takes the zero kernel {:false-neg 0 :false-pos 0 :basis :checkable
    :measurement :absent}; a :judgement class is :unsupported-class.

  The zero kernel is the UNMEASURED DEFAULT for a checkable class, not an
  observation that the check is exact: tokenLikelihood is defined for any
  rates in [0,1], and tokenLikelihood_checkable (the identity kernel) is
  conditional on the rates being zero. :measurement :absent records that
  the condition was assumed, not measured. A class id absent from the
  contract is :unknown-class."
  [rates contract token-classes]
  (let [by-id (contract-classes contract)]
    (reduce-kv (fn [acc token class-id]
                 (if-not (contains? by-id class-id)
                   (reduced {:status :missing :kind :unknown-class
                             :class class-id :token token})
                   (let [cls (get by-id class-id)
                         r (get rates class-id)
                         unmeasured? (or (nil? r) (= :unobserved (:status r)))
                         fn-cell (some-> r :false-neg measured-cell)
                         fp-cell (some-> r :false-pos measured-cell)
                         fn-rate (:rate fn-cell)
                         fp-rate (:rate fp-cell)
                         declared? (boolean
                                    (some #(= :declared-not-measured (:status %))
                                          [fn-cell fp-cell]))]
                     (cond
                       (and fn-rate fp-rate)
                       (assoc acc token {:false-neg fn-rate
                                         :false-pos fp-rate
                                         :basis (if (:prior r) :prior :estimated)
                                         :counts {:false-neg (cell-counts (:false-neg r))
                                                  :false-pos (cell-counts (:false-pos r))}})

                       (and unmeasured? (= :checkable (:kind cls)))
                       (assoc acc token {:false-neg 0 :false-pos 0 :basis :checkable
                                         :measurement :absent})

                       :else
                       (reduced (cond-> {:status :missing :kind :unsupported-class
                                         :class class-id :token token}
                                  declared? (assoc :cell-reason
                                                   :declared-not-measured)))))))
               {}
               token-classes)))

(defn sourced-rates
  "Adjudication rates for a cascade scoring universe, SOURCED from this
  namespace rather than declared by a caller (WIRE-5). LABELS, SUBJECTS and
  PRIOR are `rates-by-class`'s inputs. A class with admitted labels takes
  its measured rates, checkable or not; a checkable class with none takes
  the zero kernel as the unmeasured default (tokenLikelihood_checkable,
  conditional on zero rates), recorded as :measurement :absent; a judgement
  class with no admitted rate, or any class measured on one cell only, is
  the typed :unsupported-class refusal, never padded; a class whose rate
  is not the ratio of its own counts refuses the same way, with
  :cell-reason :declared-not-measured. A refused
  rates-by-class (:invalid-prior) is returned as is. LOCATORS is the cascade
  problem's {token {:class class-id}}; CONTRACT is the S-1 observation
  contract. Returns

    {:status :sourced
     :source :futon2.aif.observation-rates/sourced-rates
     :rates {token {:false-neg r :false-pos r}}   ; every LOCATED token
     :basis {token :checkable|:estimated|:prior}
     :measurement {token :absent | {:false-neg {:numerator n :denominator d}
                                    :false-pos {...}}}
     :class-of {token class-id}}

  or token-likelihood-rates' typed refusal (:unknown-class /
  :unsupported-class). Tokens WITHOUT a locator are simply absent from
  :rates — the consumer's own coverage check names them; no rate is
  invented for an unlocated token."
  [labels subjects prior locators contract]
  (let [rates (rates-by-class labels subjects prior)
        class-of (into {} (map (fn [[t l]] [t (:class l)])) locators)
        assembled (if (contains? rates :status)
                    rates
                    (token-likelihood-rates rates contract class-of))]
    (if (contains? assembled :status)
      assembled
      {:status :sourced
       :source :futon2.aif.observation-rates/sourced-rates
       :rates (into {} (map (fn [[t r]] [t (select-keys r [:false-neg :false-pos])]))
                    assembled)
       :basis (into {} (map (fn [[t r]] [t (:basis r)])) assembled)
       :measurement (into {} (map (fn [[t r]] [t (or (:counts r) (:measurement r))]))
                          assembled)
       :class-of class-of})))
