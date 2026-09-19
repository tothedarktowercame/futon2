(ns futon2.aif.cascade-model-manifest
  "Partial, source-bound token-frontier model. No scoring or live side effects."
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [futon2.aif.likelihood-precision :as lprec])
  (:import [java.security MessageDigest]))

(def affirmative-markers
  ["instantiated" "accepted" "done" "landed" "live" "built" "merged" "agreed" "dark" "shadow"])
(def outstanding-markers
  ["open" "remain open" "owed" "absent" "missing" "pending" "candidate" "not yet" "no" "not" "never"])
(def extraction-rule
  "a. Source: the mission's Status line INCLUDING its continuation line(s) (fix the adapter's omission you found), plus any structured status/component table in the same document (e.g. M-aif-policy-conditioned-eig.md:112-115 built/unwired, candidate, absent). Prefer the table where it exists; cite the lines used.
b. Split into clauses at \";\", \"—\", sentence ends, and table rows.
c. A clause contributes its salient tokens to q0 only if it affirms the capability exists, via declared affirmative markers (e.g. instantiated, accepted, done, landed, live, built, merged, agreed). Built-but-not-live clauses (\"dark\", \"shadow\", \"built/unwired\") count as available for construction and are tagged :built-not-live, so the distinction is kept.
d. A clause with an outstanding marker (open, remain open, owed, absent, missing, pending, candidate, not yet) contributes its tokens to WANT, never to q0.
e. A clause with neither marker is excluded and recorded as an :unclassified-clause finding.
f. Negation words are never dropped in any of this. Declare both marker lists in the manifest. If q0 comes out empty for a mission, that is a finding, not a default.")
(def stopwords #{"the" "and" "you" "are" "for" "from" "with" "that" "this" "its" "want" "also" "need" "into" "than"})
(defn tokens [s]
  (into (sorted-set) (remove stopwords) (re-seq #"[\p{L}][\p{L}\p{N}]*" (str/lower-case s))))
(defn sha256 [s]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8")))))
(defn source [path text] {:path path :sha256 (sha256 text) :authority :source-document})
(defn- marked? [s markers]
  (some #(re-find (re-pattern (str "(?i)\\b" % "\\b")) s) markers))
(defn- classify [clause]
  (cond (marked? clause outstanding-markers) :outstanding
        (marked? clause affirmative-markers)
        (if (marked? clause ["dark" "shadow" "unwired"]) :built-not-live :available)
        :else :unclassified))

(defn extract-target
  "Tables with a Status column take precedence over Status prose. All clauses
   remain in the record, including excluded prose. No id-stem fallback."
  [id path text]
  (let [lines (vec (str/split-lines text))
        status-index (first (keep-indexed #(when (re-find #"^\*\*Status:\*\*" %2) %1) lines))
        status-lines (when status-index
                       (take-while #(and (not (str/blank? (second %)))
                                         (not (re-find #"^\*\*(?!Status:)" (second %))))
                                   (map-indexed #(vector (+ status-index %1 1) %2) (subvec lines status-index))))
        tables (mapcat (fn [i]
                         (when (and (str/starts-with? (get lines i) "|")
                                    (re-find #"(?i)\|\s*status\s*\|" (get lines i)))
                           (take-while #(str/starts-with? (second %) "|")
                                       (map-indexed #(vector (+ i %1 3) %2) (subvec lines (min (count lines) (+ i 2)))))))
                       (range (count lines)))
        selected (if (seq tables) tables status-lines)
        clauses (vec (for [[line s] selected
                           c (str/split s #";|—|\.(?:\s+|$)")
                           :when (not (str/blank? c))]
                       {:line line :text (str/trim c) :classification (let [k (classify c)] (if (and (= :available k) (marked? s ["dark" "shadow" "unwired"])) :built-not-live k))
                        :tokens (tokens c)}))
        have (into (sorted-set) (mapcat :tokens) (filter #(#{:available :built-not-live} (:classification %)) clauses))
        title (some #(second (re-find #"^# (?:Mission: )?(.+)" %)) lines)
        want (into (tokens (or title "")) (mapcat :tokens (filter #(= :outstanding (:classification %)) clauses)))
        findings (vec (concat
                       (for [c clauses :when (= :unclassified (:classification c))]
                         {:kind :unclassified-clause :line (:line c) :text (:text c)})
                       (when (empty? have) [{:kind :missing-source-bound-have :mission id}])))]
    {:id id :source (source path text) :authority :documented-interpretation
     :ok (and (seq have) (some? title)) :have have :want want
     :selection-rule (if (seq tables) :prefer-status-table :status-with-continuations)
     :status-lines (vec status-lines) :clauses clauses :findings findings}))

(defn pattern-block [text label]
  (some-> (re-find (re-pattern (str "(?ms)^  \\+ " label ":(.*?)(?=^  \\+ |\\z)")) text)
          second str/trim))

(def guard-interpretation
  {:authority :documented-interpretation
   :positive "Conjunctive presence of all lexical tokens in IF and HOWEVER after the declared function-word removal; a token-level interpretation, not natural-language truth."
   :negative "Exact literal conjunctions such as ready and not blocked compile to presence and absence; other negation scopes, disjunctions, nested conditionals, modalities and substructures are MISSING."
   :stopwords stopwords})

(defn compile-clause [s]
  (let [s (some-> s str/trim str/lower-case)
        literal? (and s (not (re-find #"\b(no|nor|never|without|missing|absent)\b" s)) (re-matches #"(?:not )?[a-z]+(?: and (?:not )?[a-z]+)*" s))
        unsupported? (and s (re-find #"\b(or|if|when|unless|without|not|no|nor|never|cannot|can|could|may|might|missing|absent|fail|fails|rarely|rather|versus|doesn|isn)\b|\+ |[?]" s))]
    (cond
      (str/blank? s) {:status :missing :kind :missing-guard-clause}
      literal? (let [parts (str/split s #" and ")]
                 {:status :interpreted
                  :present (into (sorted-set) (remove #(str/starts-with? % "not ")) parts)
                  :absent (into (sorted-set) (map #(subs % 4)) (filter #(str/starts-with? % "not ") parts))})
      unsupported? {:status :missing :kind :uninterpretable-guard :text s}
      (empty? (tokens s)) {:status :missing :kind :empty-guard}
      :else {:status :interpreted :present (tokens s) :absent (sorted-set)})))

(defn interpret-pattern [id path text]
  (let [if-text (pattern-block text "IF") however-text (pattern-block text "HOWEVER")
        then-text (pattern-block text "THEN")
        guards (mapv compile-clause [if-text however-text])
        ok (and (every? #(= :interpreted (:status %)) guards) (seq (tokens (or then-text ""))))]
    {:id id :source (source path text) :authority :documented-interpretation
     :produces (tokens (or then-text ""))
     :clauses {:if if-text :however however-text :then then-text}
     :guard (if ok {:status :interpreted :operator :and :clauses guards}
                {:status :missing :kind :missing-pattern-interpretation :details guards})
     :transition (if ok {:status :interpreted :operator :union :produces (tokens then-text)
                         :authority :documented-interpretation}
                     {:status :missing :kind :missing-pattern-interpretation})}))

(defn guard-holds? [pattern state]
  (when (= :interpreted (get-in pattern [:guard :status]))
    (and (every? #(and (set/subset? (:present %) state) (empty? (set/intersection (:absent %) state)))
                 (get-in pattern [:guard :clauses]))
         (not (set/subset? (or (:produces pattern) (get-in pattern [:transition :produces]) #{}) state)))))
(defn transition-row [pattern state]
  (if (= :interpreted (get-in pattern [:transition :status]))
    {(set/union state (get-in pattern [:transition :produces])) 1}
    {:status :missing :kind :missing-pattern-interpretation}))
(defn observed-belief
  "Lean DarkTower.WarMachine.TokenState.observedBelief (mathlib4
   DarkTower/WarMachine/TokenState.lean): the point mass at the observed
   token state. Sums to 1 (observedBelief_sum)."
  [state]
  {state 1})

(defn- powerset [coll]
  (reduce (fn [ss v] (into ss (map #(conj % v)) ss)) #{#{}} coll))

(defn independent-belief
  "Lean DarkTower.WarMachine.TokenState.independentBelief: each token v is
   established independently with probability (p v); a subset s of universe
   has mass ∏_v (if v ∈ s then p v else 1 − p v), exact rationals. Sums to 1
   over the powerset (independentBelief_sum), and reduces to
   observed-belief when every (p v) is 0 or 1 (independentBelief_eq_observedBelief).
   Refuses with {:status :missing :kind :invalid-token-probability} when any
   probability is outside [0,1] or missing."
  [p universe]
  (let [universe (set universe)
        bad (some (fn [v] (let [x (get p v)]
                            (when-not (and (or (ratio? x) (integer? x)) (<= 0 x 1))
                              v)))
                  universe)]
    (if bad
      {:status :missing :kind :invalid-token-probability :token bad :value (get p bad)}
      (into {}
            (map (fn [s]
                   [s (reduce * (map #(if (contains? s %) (get p %) (- 1 (get p %))) universe))]))
            (powerset universe)))))

(defn coverage
  "Lean DarkTower.WarMachine.TokenState.coverage: |want ∩ state| / |want|,
   an exact rational; = 1 exactly when want ⊆ state (coverage_eq_one_iff),
   monotone in state (coverage_mono). Lean requires want.Nonempty, so an
   empty want refuses with the typed empty-want-signature finding."
  [want state]
  (if (seq want)
    (/ (count (set/intersection (set want) (set state))) (count want))
    {:status :missing :kind :empty-want-signature}))

(defn observation-row [want state]
  (let [c (coverage want state)]
    (if (map? c) c {c 1})))

(defn- rate-bad-token
  "The first token whose adjudication rate entry is missing, non-rational or
   outside [0,1], else nil. Lean bounds both structure fields with
   Set.Icc 0 1 (AdjudicationRates.falseNeg_mem/falsePos_mem)."
  [rates]
  (some (fn [v] (let [{:keys [false-neg false-pos]} (get rates v)]
                  (when-not (and (or (ratio? false-neg) (integer? false-neg))
                                 (<= 0 false-neg 1)
                                 (or (ratio? false-pos) (integer? false-pos))
                                 (<= 0 false-pos 1))
                    v)))
        (set (keys rates))))

(defn- refusal-map? [x] (and (map? x) (contains? x :status)))

(defn token-likelihood
  "Lean DarkTower.WarMachine.TokenObservation.tokenLikelihood (mathlib4
   889429e6bf): A(o|s) = ∏_v over the universe (all tokens in rates) of
   (if v ∈ s then (if v ∈ o then 1 − falseNeg v else falseNeg v)
                    else (if v ∈ o then falsePos v else 1 − falsePos v)),
   exact rationals; nonnegative (tokenLikelihood_nonneg) and column-summing
   to 1 over observations (tokenLikelihood_colsum); the identity kernel when
   every rate is 0 (tokenLikelihood_checkable). Refuses with the typed
   {:status :missing :kind :invalid-adjudication-rate} for a missing,
   non-rational or out-of-[0,1] rate, including a state/observation token
   with no rate entry at all."
  [rates state obs]
  (or (if-let [outside (some #(when-not (contains? rates %) %)
                              (set/union (set state) (set obs)))]
        {:status :missing :kind :invalid-adjudication-rate :token outside :value nil}
        nil)
      (when-let [bad (rate-bad-token rates)]
        {:status :missing :kind :invalid-adjudication-rate :token bad :value (get rates bad)})
      (reduce * (map (fn [v]
                       (let [{:keys [false-neg false-pos]} (get rates v)]
                         (if (contains? state v)
                           (if (contains? obs v) (- 1 false-neg) false-neg)
                           (if (contains? obs v) false-pos (- 1 false-pos)))))
                     (set (keys rates))))
      (throw (ex-info "unreachable" {}))))

(defn observation-distribution
  "Lean TokenObservation.tokenLikelihood_colsum: the full observation row
   {obs tokenLikelihood rates state obs} over every subset obs of the
   universe, zero-mass entries omitted (sparse representation, as
   pattern-kernel); the retained entries sum to exactly 1. Rate refusals
   propagate."
  [rates state]
  (if (or (rate-bad-token rates)
          (some #(not (contains? rates %)) (set state)))
    {:status :missing :kind :invalid-adjudication-rate
     :token (or (rate-bad-token rates) (some #(when-not (contains? rates %) %) (set state)))
     :value (get rates (rate-bad-token rates))}
    (let [universe (set (keys rates))
          subsets (powerset universe)]
      (into {} (remove (comp zero? val))
            (zipmap subsets (map (partial token-likelihood rates state) subsets))))))

(defn predict-observations
  "Lean PolicyRollout.predictedOutcome composed with TokenObservation's A:
   Q(o) = Σ_s A(s,o) · q(s) over a state distribution q. With zero rates
   (tokenLikelihood_checkable) this equals q itself
   (TokenObservation.predictedOutcome_eq_rolloutState). Rate refusals
   propagate."
  [rates q]
  (reduce (fn [acc [s mass]]
            (let [d (observation-distribution rates s)]
              (if (refusal-map? d)
                (reduced d)
                (reduce-kv (fn [acc' o p] (update acc' o (fnil + 0) (* mass p))) acc d))))
          {} q))

(defn with-pattern-theta
  "Lean DarkTower.WarMachine.CascadeTransition.InterpretedPattern (mathlib4
   c1caf481a2): every pattern carries an interpretation theta ∈ [0,1]. A
   Clojure pattern without :theta carries the declared documented default
   interpretation 1; the default is recorded on the pattern
   (:theta-source :documented-default), never silent."
  [pattern]
  (if (contains? pattern :theta)
    pattern
    (assoc pattern :theta 1 :theta-source :documented-default)))

(defn pattern-kernel
  "Lean CascadeTransition.patternKernel: to (set/union state produces) with
   probability theta and stay at state with 1 − theta, exact rationals; the
   two masses merge to {state 1} when the pattern is already achieved
   (patternKernel_of_achieved, theta + (1 − theta) = 1 at state). A theta
   outside [0,1] refuses with the typed
   {:status :missing :kind :invalid-pattern-interpretation} outcome."
  [pattern state]
  (let [pattern (with-pattern-theta pattern)
        theta (:theta pattern)]
    (if-not (and (or (ratio? theta) (integer? theta)) (<= 0 theta 1))
      {:status :missing :kind :invalid-pattern-interpretation
       :pattern (:id pattern) :theta theta}
      (let [target (set/union state (:produces pattern))]
        (if (= target state)
          {state 1}
          ;; sparse representation of the Lean row: zero-mass entries (theta
          ;; = 0 or 1) are omitted, as in observed-belief
          (into {} (remove (comp zero? val)) {target theta state (- 1 theta)}))))))

(defn first-enabled
  "Lean CascadeTransition.firstEnabled: the first pattern in precedence whose
   guard holds at state — present ⊆ s, absent ∩ s = ∅, and produces ⊄ s
   (guard-holds?): a completed pattern is skipped. nil when none holds."
  [precedence state]
  (first (filter #(true? (guard-holds? % state)) precedence)))

(defn missing-interpretation
  "Lean CascadeTransition.interpret_eq_none_iff: a precedence list is a typed
   hole when some firing pattern in it lacks an interpretation. Returns that
   typed refusal, or nil when every pattern is interpreted."
  [precedence]
  (when-let [p (first (filter #(not= :interpreted (get-in % [:guard :status])) precedence))]
    {:status :missing :kind :missing-pattern-interpretation :pattern (:id p)}))

(defn cascade-kernel
  "Lean CascadeTransition.cascadeKernel: pattern-kernel of the first enabled
   pattern, or the identity {state 1} when none is enabled
   (cascadeKernel_of_noEnabled). A precedence containing a pattern without an
   interpretation is the typed hole (interpret_eq_none_iff) and refuses."
  [precedence state]
  (or (missing-interpretation precedence)
      (if-let [p (first-enabled precedence state)]
        (pattern-kernel p state)
        {state 1})))

(defn- refusal? [x] (and (map? x) (contains? x :status)))

(defn- push-forward [prec q]
  (reduce (fn [acc [s mass]]
            (let [k (cascade-kernel prec s)]
              (if (refusal? k)
                (reduced k)
                (reduce-kv (fn [acc' s' p] (update acc' s' (fnil + 0) (* mass p))) acc k))))
          {} q))

(defn rollout
  "Lean PolicyRollout.rolloutState (mathlib4 07b094c59b): push the
   distribution map q0 forward n steps; step k uses (precedence-fn k) as the
   cascade policy (the action space U is the precedence list). Refusals
   propagate."
  [precedence-fn q0 n]
  (loop [k 0 q q0]
    (if (= k n)
      q
      (let [q' (push-forward (precedence-fn k) q)]
        (if (refusal? q')
          q'
          (recur (inc k) q'))))))

(defn normalized-exact? [row]
  (and (map? row) (every? #(and (or (integer? %) (ratio? %)) (<= 0 %)) (vals row))
       (= 1 (reduce + (vals row)))))

(defn build-manifest [target patterns]
  (if-not (and (:ok target) (seq (:have target)) (seq (:want target)))
    {:status :refused :kind :missing-source-bound-have :target target}
    (let [produces (mapcat :produces patterns)
        universe (into (set/union (:have target) (:want target)) produces)
        missing (filter #(= :missing (get-in % [:guard :status])) patterns)]
    {:schema :wm/cascade-model-manifest-v1 :status :partial :scoring-permitted? false
     :authority :documented-interpretation :target target
     :extraction {:rule extraction-rule :affirmative-markers affirmative-markers
                  :outstanding-markers outstanding-markers :priority :outstanding-first :stopwords stopwords}
     :state {:universe universe :carrier {:kind :powerset :of universe}
             :representation :symbolic-finite-powerset :authority :documented-interpretation}
     :patterns patterns :guard-interpretation guard-interpretation
     :initial-belief {:authority :documented-interpretation :source (:source target)
                      :mass (observed-belief (:have target))}
     :observation {:authority :documented-interpretation :kind :deterministic-want-coverage
                   :label "token-level proxy for true discharge" :want (:want target)
                   :source (:source target)
                   :alphabet (when (seq (:want target)) (mapv #(/ % (count (:want target))) (range (inc (count (:want target))))))
                   :ambiguity 0 :consequence :risk-only}
     :preference {:status :missing :kind :missing-coverage-preference-map}
     :horizon {:authority :documented-interpretation :rule :firing-pattern-count :value (count patterns)}
     :firing (mapv :id patterns)
     :notes {:precedence :construction-order-provisional-not-admissibility-witness
             :future-precedence "Enumerate linear extensions v before u when u stands on v, transitively; declare cap, refuse overflow, no sampling."
             :future-prior {:habit :uniform :authority :cold-start :gamma 1 :form :B.7
                            :choice :argmax :ties :canonical-pair-identity}
             :H1b "Must genuinely select rank one and pass its gate."
             :observation-validator "Documented A is not submitted to categorical-ambiguity observed-estimate admission; contract change remains separate."}
     :findings (vec (concat (:findings target)
                            [{:kind :missing-coverage-preference-map :input :C_tau}
                             {:kind :admissible-precedence-not-yet-commissioned}]
                            (for [p missing] {:kind :missing-pattern-interpretation :pattern (:id p) :source (:source p)})
                            (for [p patterns
                                  :when (= :interpreted (get-in p [:guard :status]))
                                  :let [outside (set/difference
                                                  (into #{} (mapcat :present) (get-in p [:guard :clauses]))
                                                  universe)]
                                  :when (seq outside)]
                              {:kind :guard-unreachable-in-declared-universe :pattern (:id p)
                               :required-outside-universe outside :source (:source p)
                               :action :review-interpretation-without-widening-carrier})))})))

(defn preference-spec
  "Lean DarkTower.WarMachine.TokenPreference.PreferenceSpec (mathlib4
   678c797666): want nonempty (want_nonempty), lam > 0 (lam_pos),
   mu >= 0 (mu_nonneg), and zeroed not every subset of the universe
   want ∪ evidence ∪ zeroed tokens (zeroed_proper). lam and mu are exact
   rationals (ratio or integer), the represented reals of the Lean fields.
   Returns the validated spec; each violation refuses with the typed
   {:status :missing :kind :invalid-preference-spec} outcome."
  [{:keys [want evidence lam mu zeroed] :as spec}]
  (let [want (set want) evidence (set evidence) zeroed (set zeroed)
        exact? (fn [x] (or (ratio? x) (integer? x)))
        universe (set/union want evidence (into #{} (mapcat identity) zeroed))]
    (cond
      (empty? want)
      {:status :missing :kind :invalid-preference-spec :field :want :reason :empty-want}

      (not (and (exact? lam) (pos? lam)))
      {:status :missing :kind :invalid-preference-spec :field :lam :value lam :reason :lam-not-positive}

      (not (and (exact? mu) (<= 0 mu)))
      {:status :missing :kind :invalid-preference-spec :field :mu :value mu :reason :mu-negative}

      (= (powerset universe) zeroed)
      {:status :missing :kind :invalid-preference-spec :field :zeroed :reason :zeroed-covers-universe}

      :else
      (assoc spec :want want :evidence evidence :zeroed zeroed :universe universe
             :schema :wm/token-preference-spec-v1))))

(defn token-utility
  "Lean TokenPreference.utility: lam·(|want ∩ obs| / |want|) +
   mu·|evidence ∩ obs|, an exact rational. The coverage term reuses
   `coverage` (Lean TokenState.coverage)."
  [spec obs]
  (let [c (coverage (:want spec) obs)]
    (if (refusal? c)
      c
      (+ (* (:lam spec) c)
         (* (:mu spec) (count (set/intersection (:evidence spec) (set obs))))))))

(defn preference-distribution
  "Lean TokenPreference.preference over every subset of the universe:
   zeroed subsets map to 0.0 (preference_eq_zero_iff), the others to
   exp(utility)/Z with Z = Σ exp(utility) over non-zeroed subsets (Z_pos ⇒
   the distribution is well defined; preference_sum holds up to double
   rounding, checked to 1e-12 in tests). exp is Math/exp on doubles — the
   one non-exact step, standing for the Lean reals."
  [spec universe]
  (let [universe (set universe)
        subsets (powerset universe)
        zeroed (:zeroed spec)
        u (fn [s] (token-utility spec s))
        z (reduce + (map #(Math/exp (double (u %))) (remove #(contains? zeroed %) subsets)))]
    (into {} (map (fn [s]
                    [s (if (contains? zeroed s)
                         0.0
                         (/ (Math/exp (double (u s))) z))])
                  subsets))))

(defn outcome-risk
  "Lean DarkTower.WarMachine.OutcomeRiskKL.outcomeRisk (mathlib4 f4fed50271):
   D_KL[q ‖ c] in extended reals. Returns :infinite exactly when some
   observation has q(o) > 0 and c(o) = 0 (a missing key counts as 0);
   otherwise the Gibbs sum Σ_{q(o)>0} q(o)·ln(q(o)/c(o)) as a double."
  [q c]
  (if (some (fn [[o p]] (and (pos? p) (zero? (get c o 0)))) q)
    :infinite
    (double (reduce + 0.0
                    (for [[o p] q :when (pos? p)]
                      (* (double p) (Math/log (/ (double p) (double (get c o))))))))))

(defn step-ambiguity
  "Lean DarkTower.WarMachine.PolicyHorizon.stepAmbiguity (mathlib4 6d80f56f5d):
   Σ_s q(s)·H[A(·|s)] with H = −Σ_o A(o|s)·ln A(o|s) and 0·ln 0 = 0
   (zero-mass entries are omitted by observation-distribution, so every
   retained p is positive). With all-zero adjudication rates A is the identity
   kernel (tokenLikelihood_checkable) and this is 0.0. Rate refusals
   propagate as the typed {:status :missing ...} outcome."
  [rates q-state]
  (if (rate-bad-token rates)
    {:status :missing :kind :invalid-adjudication-rate
     :token (rate-bad-token rates) :value (get rates (rate-bad-token rates))}
    (double (reduce + 0.0
                    (map (fn [[s mass]]
                           (* (double mass)
                              (- (reduce + 0.0
                                         (map (fn [[_ p]]
                                                (* (double p) (Math/log (double p))))
                                              (observation-distribution rates s))))))
                         q-state)))))

(defn horizon-g
  "Lean DarkTower.WarMachine.PolicyHorizon.horizonEFE (mathlib4 6d80f56f5d):
   G(π) = Σ_{τ=1}^{T} [ D_KL[Q(o_τ|π) ‖ C_τ] + Σ_s Q(s_τ|π)·H[A(·|s)] ], one
   horizon T for the candidate (audit A4 §2; design P7, step 1a). q_τ is
   (rollout precedence-fn q0 τ), Q_τ is (predict-observations rates q_τ), and
   C_τ is step-indexed via :c-fn as the re-audit requires
   (fixture_stepIndexed_preference; a constant C is the special case, not the
   definition). Returns the double sum, :infinite when any step's risk is
   infinite (horizonEFE_eq_top_iff), or the first typed refusal from the
   rates, the rollout, or the observation model. :horizon must be a positive
   integer. Pure function; no scoring port, selection or live wiring."
  [{:keys [rates q0 precedence-fn horizon c-fn]}]
  (or (when-let [bad (rate-bad-token rates)]
        {:status :missing :kind :invalid-adjudication-rate
         :token bad :value (get rates bad)})
      (when-not (and (integer? horizon) (pos? horizon))
        {:status :missing :kind :invalid-horizon :horizon horizon})
      (when-not (and (ifn? c-fn) (ifn? precedence-fn) (map? q0))
        {:status :missing :kind :invalid-horizon-g-input})
      (loop [tau 1 total 0.0]
        (if (> tau horizon)
          (double total)
          (let [q (rollout precedence-fn q0 tau)]
            (cond
              (refusal? q) q
              :else (let [predicted (predict-observations rates q)]
                      (cond
                        (refusal? predicted) predicted
                        :else (let [risk (outcome-risk predicted (c-fn tau))
                                    amb (step-ambiguity rates q)]
                                (cond
                                  (= risk :infinite) :infinite
                                  (refusal? amb) amb
                                  :else (recur (inc tau) (+ total risk amb))))))))))))

(defn- utility-weights
  "Per-token additive weights of TokenPreference.utility: utility(o) =
   Σ_{v ∈ o} w_v with w_v = lam/|want| for v ∈ want, plus mu for v ∈
   evidence, and nothing for any other token. The empty sum is utility 0,
  matching token-utility on the empty subset.

  Spec extension (live C, 2026-09-17): an optional :weights {token w} map
  replaces the UNIFORM per-token share for the tokens it names — w_v = w
  instead of lam/|want| — so a derived C can weight its want tokens by
  source evidence. Unnamed want tokens keep the uniform share."
  [spec]
  (let [want (:want spec) evidence (:evidence spec) weights (:weights spec)
        per (if (seq want) (/ (:lam spec) (count want)) 0)]
    (into {} (map (fn [v] [v (+ (cond
                                  (contains? weights v) (get weights v)
                                  (contains? want v) per
                                  :else 0)
                                (if (contains? evidence v) (:mu spec) 0))]))
          (set/union want evidence))))

(defn log-preference-fn
  "Log-space pointwise form of Lean TokenPreference.preference over the token
   universe V = want ∪ evidence ∪ tokens(zeroed) ∪ (the optional
   `universe` argument). Returns o ↦ ln c(o): ##-Inf for o ∈ zeroed
   (preference_eq_zero_iff), else u(o) − ln Z, with
   ln Z = Σ_{v ∈ V} ln(1 + e^{w_v}) + ln(1 − Σ_{z ∈ zeroed} e^{u(z) − ln Z₀}).
   The product form is exact because utility is a per-token sum; working in
   logs keeps Z finite for universes of thousands of tokens (a plain product
   of (1 + e^{w_v}) overflows near 1000 tokens, since every weight-0 token
   contributes a factor 2). V must be the observation space of the comparison:
   tokens outside want/evidence carry weight 0 but still enlarge Z, so the
   caller passes the common universe of the candidates being compared.
   zeroed_proper is decided by counting, and cannot fail for |V| ≥ 62 (a
   zeroed set cannot hold 2^62 subsets). Refuses exactly like preference-spec."
  ([spec] (log-preference-fn spec nil))
  ([spec extra-universe]
   (let [want (set (:want spec)) evidence (set (:evidence spec))
         zeroed (set (:zeroed spec))
         lam (:lam spec) mu (:mu spec)
         weights (:weights spec)
         exact? (fn [x] (or (ratio? x) (integer? x)))
         universe (set/union want evidence (into #{} (mapcat identity) zeroed) (set extra-universe))]
     (cond
       (empty? want)
       {:status :missing :kind :invalid-preference-spec :field :want :reason :empty-want}
       (not (and (exact? lam) (pos? lam)))
       {:status :missing :kind :invalid-preference-spec :field :lam :value lam :reason :lam-not-positive}
       (not (and (exact? mu) (<= 0 mu)))
       {:status :missing :kind :invalid-preference-spec :field :mu :value mu :reason :mu-negative}
       (and (contains? spec :weights)
            (not (and (map? weights)
                      (every? (fn [[t w]] (and (contains? want t) (exact? w) (pos? w)))
                              weights))))
       {:status :missing :kind :invalid-preference-spec :field :weights
        :reason (cond (not (map? weights)) :weights-not-a-map
                      (some (comp not #(contains? want %)) (keys weights)) :weight-token-not-in-want
                      :else :weight-not-positive-rational)
        :weights weights}
       (and (< (count universe) 62) (= (count zeroed) (bit-shift-left 1 (count universe))))
       {:status :missing :kind :invalid-preference-spec :field :zeroed :reason :zeroed-covers-universe}
       :else
       (let [w (utility-weights {:want want :evidence evidence :lam lam :mu mu
                                 :weights weights})
             u (fn [o] (reduce + 0.0 (map (fn [t] (double (get w t 0))) (set/intersection (set o) universe))))
             log-z0 (reduce + 0.0 (map (fn [t] (Math/log1p (Math/exp (double (get w t 0))))) universe))
             zeroed-share (reduce + 0.0 (map (fn [zp] (Math/exp (- (u zp) log-z0))) zeroed))
             log-z (+ log-z0 (Math/log1p (- zeroed-share)))]
         (fn log-pointwise-preference [o]
           (if (contains? zeroed o) ##-Inf (- (u o) log-z))))))))

(defn preference-fn
  "Pointwise closed form of Lean TokenPreference.preference: o ↦ c(o) =
   exp(ln c(o)) from log-preference-fn over the same universe (0.0 on zeroed).
   Equals preference-distribution on every subset of the universe
   (preference_sum). Refuses exactly like preference-spec."
  ([spec] (preference-fn spec nil))
  ([spec extra-universe]
   (let [lpf (log-preference-fn spec extra-universe)]
     (if (refusal? lpf)
       lpf
       (fn pointwise-preference [o] (Math/exp (lpf o)))))))

(defn- zero-rates?
  "Every adjudication rate entry is exactly zero, the precondition of the
   identity observation kernel (Lean tokenLikelihood_checkable)."
  [rates]
  (every? (fn [t] (and (zero? (:false-neg t)) (zero? (:false-pos t)))) (vals rates)))

(defn- outcome-risk-pointwise
  "outcome-risk with C supplied as a pointwise log-preference, so no C over a
   powerset is materialised and tiny c(o) never underflows to a false ⊤.
   Same Lean OutcomeRiskKL.outcomeRisk: ⊤ iff some q(o) > 0 has c(o) = 0
   (ln c = ##-Inf), else Σ_{q(o)>0} q(o)·(ln q(o) − ln c(o))."
  [q log-c-of]
  (if (some (fn [[o p]] (and (pos? p) (= ##-Inf (log-c-of o)))) q)
    :infinite
    (double (reduce + 0.0
                    (for [[o p] q :when (pos? p)]
                      (* (double p) (- (Math/log (double p)) (double (log-c-of o)))))))))

(defn- horizon-g-sparse*
  "The shared evaluation core of horizon-g-sparse. Same refusals, same
   arithmetic, same iteration order; when RECORD? is true the per-step risk
   (a local summed and discarded before the WIRE-1 emission slice) is also
   returned under :steps, one {:tau tau :risk risk} per tau actually
   iterated. The infinite-risk step records :risk :infinite and stops,
   matching the scalar path's early return. Returns {:g <scalar, :infinite
   or typed refusal> :steps <vector or nil>}."
  [{:keys [rates q0 precedence-fn horizon spec c-fn-pointwise universe zeta]} record?]
  (let [bad (rate-bad-token rates)
        zeta (or zeta 1)
        ;; R7 (declared FIXED ζ): temper the per-token observation kernel ONCE,
        ;; up front, with likelihood-precision's audited law — tempering each
        ;; token's Bernoulli (fn,fp) pair is exactly row-wise A^ζ/Z for the
        ;; product kernel, so qbar_v (risk) and p_v(s) (ambiguity) both score
        ;; the tempered kernel automatically with no second code path. ζ = 1
        ;; keeps `rates` EXACTLY as passed (byte-identical discipline, same as
        ;; the zero-rate guard); a ζ ≠ 1 with ALL-ZERO rates is refused below,
        ;; never silently ignored. Typed refusals (:invalid-zeta,
        ;; :negative-zeta) come from lprec/tempered-rates, one law one place.
        tempered (if (or (= 1 zeta) bad (zero-rates? rates))
                   rates
                   (lprec/tempered-rates rates zeta))]
    (cond
      bad {:g {:status :missing :kind :invalid-adjudication-rate
               :token bad :value (get rates bad)} :steps nil}
      ;; A declared fixed ζ ≠ 1 with the identity observation kernel is a
      ;; configuration error: ζ multiplies nothing here, and silently ignoring
      ;; it would hide that (zai-55/zai-30 ruling, 2026-09-18).
      (and (zero-rates? rates) (not= 1 zeta))
      {:g {:status :missing :kind :zeta-with-identity-rates :zeta zeta
           :limitation "a declared fixed zeta ≠ 1 with all-zero adjudication rates tempers an identity kernel that is never evaluated; refuse rather than silently ignore"} :steps nil}
      (lprec/refusal? tempered)
      {:g tempered :steps nil}
      (not (and (integer? horizon) (pos? horizon)))
      {:g {:status :missing :kind :invalid-horizon :horizon horizon} :steps nil}
      (not (and (ifn? precedence-fn) (map? q0)))
      {:g {:status :missing :kind :invalid-horizon-g-input} :steps nil}
      (and (nil? c-fn-pointwise) (nil? spec))
      {:g {:status :missing :kind :missing-preference-spec} :steps nil}
      :else
      ;; WIRE-4: zero rates keep the identity-A path EXACTLY as it was
      ;; (byte-identical numbers, refusals and iteration order); non-zero
      ;; rates now score by the factorized closed forms instead of
      ;; refusing — see the factorized body and its docstring below.
      (if (zero-rates? rates)
        (let [lpf (when (nil? c-fn-pointwise) (log-preference-fn spec universe))
            point-c (cond
                      c-fn-pointwise (fn [tau o] (let [c ((c-fn-pointwise tau) o)]
                                                   (if (zero? c) ##-Inf (Math/log (double c)))))
                      (refusal? lpf) lpf
                      :else (fn [_tau o] (lpf o)))]
        (if (refusal? point-c)
          {:g point-c :steps nil}
          ;; WM-06 domain meeting: when C is the spec seed, every positive-mass
          ;; Q outcome must be a subset of C's universe — the passed
          ;; :universe, or the spec's own want ∪ evidence ∪ zeroed tokens when
          ;; none was passed. A state carrying tokens outside it would be
          ;; scored by C as if those tokens were absent — two distinct Q
          ;; outcomes collapsing to one C value — so it is the typed refusal,
          ;; never a silent projection. (:c-fn-pointwise declares no domain
          ;; here; skipped.)
          (let [c-universe (when (nil? c-fn-pointwise)
                             (if (set? universe) universe
                                 (set/union (set (:want spec)) (set (:evidence spec))
                                            (into #{} (mapcat identity) (:zeroed spec)))))
                q-outside (fn [q]
                            (when c-universe
                              (some (fn [[s p]]
                                    (when (and (pos? p)
                                               (not (set/subset? (set s) c-universe)))
                                      s))
                                  (seq q))))]
            (if-let [s (q-outside q0)]
              {:g {:status :missing :kind :q-support-outside-c-universe
                   :state s :universe (count c-universe)}
               :steps nil}
              (loop [tau 1 total 0.0 steps (transient [])]
                (if (> tau horizon)
                  {:g (double total) :steps (when record? (persistent! steps))}
                  (let [q (rollout precedence-fn q0 tau)]
                    (if (refusal? q)
                      {:g q :steps nil}
                      (if-let [s (q-outside q)]
                        {:g {:status :missing :kind :q-support-outside-c-universe
                             :state s :step tau :universe (count c-universe)}
                         :steps nil}
                        (let [risk (outcome-risk-pointwise q (fn [o] (point-c tau o)))]
                          (if (= risk :infinite)
                            {:g :infinite
                             :steps (when record?
                                      (persistent! (conj! steps {:tau tau :risk :infinite
                                                                 :belief q :rates rates})))}
                            (recur (inc tau) (+ total risk)
                                   (if record?
                                     (conj! steps {:tau tau :risk risk :belief q :rates rates})
                                     steps)))))))))))))
        ;; WIRE-4: non-zero adjudication rates score by the FACTORIZED
        ;; closed forms — O(|universe|) per step, no powerset anywhere:
        ;;
        ;;   stepRisk      = Σ_v KL(Bern(qbar_v) ‖ Bern(c_v))
        ;;   stepAmbiguity = Σ_s q(s) · Σ_v H(Bern(p_v(s)))
        ;;
        ;; with qbar_v = m_v·(1−falseNeg_v) + (1−m_v)·falsePos_v (m_v the
        ;; belief marginal), p_v(s) = (1−falseNeg_v) if v ∈ s else
        ;; falsePos_v, and c_v = sigmoid(w_v) the per-token marginal of C
        ;; (log-preference-fn's ln c(o) = u(o) − ln Z with additive u and
        ;; ln Z = Σ_v ln(1+e^{w_v}) is exactly the log-normalizer of a
        ;; product of independent Bernoullis; entropy/KL are additive over
        ;; independent coordinates). Verified against the enumerating
        ;; horizon-g to ~1e-15 — see the WIRE-4 tests.
        ;;
        ;; Preconditions, each a TYPED refusal, never an assumption:
        ;; - C must come from :spec with an EMPTY :zeroed (a zeroed
        ;;   outcome makes risk identically infinite under non-zero rates,
        ;;   because Q then has full support; and a non-empty zeroed-share
        ;;   correction breaks C's pure product form).
        ;; - each step's rollout belief must be a POINT MASS or PRODUCT
        ;;   FORM (the live q0, observed-belief, is a point mass; a theta
        ;;   < 1 kernel makes the rollout a correlated mixture, which is
        ;;   refused, not approximated).
        (let [zeroed (set (:zeroed spec))
              ;; R7: the factorized closed forms score the TEMPERED kernel —
              ;; identical arithmetic, tempered (fn,fp) pairs.
              rates tempered
              rates-universe (set (keys rates))]
          (if c-fn-pointwise
            {:g {:status :missing :kind :c-form-unsupported-with-rates
                 :limitation "the factorized path needs C's per-token marginals; a step-indexed pointwise C cannot supply them"}
             :steps nil}
            (if (seq zeroed)
              {:g {:status :missing :kind :zeroed-unsupported-with-rates
                   :zeroed (count zeroed)
                   :limitation "a non-empty zeroed set makes risk identically infinite under non-zero rates (Q has full support and puts positive mass on an outcome C assigns zero), and it breaks C's product form"}
               :steps nil}
              (let [lpf (log-preference-fn spec rates-universe)]
                (if (refusal? lpf)
                  {:g lpf :steps nil}
                  (let [w (utility-weights spec)
                        ;; ln c_v = −softplus(−w_v), ln(1−c_v) = −softplus(w_v)
                        softplus (fn [x] (if (pos? x)
                                           (+ x (Math/log1p (Math/exp (- x))))
                                           (Math/log1p (Math/exp x))))
                        ln-c (into {} (map (fn [v]
                                             (let [wv (double (get w v 0))]
                                               [v (- (softplus (- wv)))]))
                                          rates-universe))
                        ln-1mc (into {} (map (fn [v]
                                               (let [wv (double (get w v 0))]
                                                 [v (- (softplus wv))]))
                                     rates-universe))
                        point-mass? (fn [q] (and (= 1 (count q))
                                                 (= 1 (val (first q)))))
                        marginals (fn [q]
                                    (into {}
                                          (map (fn [v]
                                                 [v (reduce + (map (fn [[s p]]
                                                                     (if (contains? s v) p 0))
                                                                   q))]))
                                          rates-universe))
                        product-form? (fn [q ms]
                                        (every? (fn [[s p]]
                                                  (= p (reduce *
                                                              (map (fn [v]
                                                                     (if (contains? s v)
                                                                       (get ms v)
                                                                       (- 1 (get ms v))))
                                                                   rates-universe))))
                                                q))
                        q-outside (fn [q]
                                    (some (fn [[s p]]
                                            (when (and (pos? p)
                                                       (not (set/subset? (set s) rates-universe)))
                                              s))
                                          (seq q)))]
                    (if-let [s (q-outside q0)]
                      {:g {:status :missing :kind :q-support-outside-c-universe
                           :state s :universe (count rates-universe)}
                       :steps nil}
                      (loop [tau 1 total 0.0 steps (transient [])]
                        (if (> tau horizon)
                          {:g (double total) :steps (when record? (persistent! steps))}
                          (let [q (rollout precedence-fn q0 tau)]
                            (if (refusal? q)
                              {:g q :steps nil}
                              (if-let [s (q-outside q)]
                                {:g {:status :missing :kind :q-support-outside-c-universe
                                     :state s :step tau :universe (count rates-universe)}
                                 :steps nil}
                                (let [ms (marginals q)
                                      factorizable? (or (point-mass? q)
                                                        (product-form? q ms))]
                                  (if-not factorizable?
                                    {:g {:status :missing
                                         :kind :non-factorizable-belief
                                         :step tau :support (count q)
                                         :limitation "the factorized risk form needs a point-mass or product-form rollout belief; a correlated mixture (e.g. a theta < 1 kernel) is refused, not approximated"}
                                     :steps nil}
                                    (let [risk (reduce + 0.0
                                                       (map (fn [v]
                                                              (let [{:keys [false-neg false-pos]} (get rates v)
                                                                    ;; the observation marginal:
                                                                    ;; qbar_v = m_v·(1−falseNeg_v) +
                                                                    ;;         (1−m_v)·falsePos_v
                                                                    m (double (get ms v))
                                                                    qbar (+ (* m (- 1.0 (double false-neg)))
                                                                            (* (- 1.0 m) (double false-pos)))
                                                                    lc (get ln-c v)
                                                                    l1 (get ln-1mc v)]
                                                                (+ (if (pos? qbar)
                                                                     (* qbar (- (Math/log qbar) lc))
                                                                     0.0)
                                                                   (let [qb (- 1.0 qbar)]
                                                                     (if (pos? qb)
                                                                       (* qb (- (Math/log qb) l1))
                                                                       0.0)))))
                                                            rates-universe))
                                          amb (reduce + 0.0
                                                      (map (fn [[s mass]]
                                                             (let [dm (double mass)]
                                                               (* dm
                                                                  (reduce + 0.0
                                                                          (map (fn [v]
                                                                                 (let [{:keys [false-neg false-pos]} (get rates v)
                                                                                       p (double (if (contains? s v)
                                                                                                   (- 1 false-neg)
                                                                                                   false-pos))]
                                                                                   (+
                                                                                    (if (pos? p) (* -1.0 p (Math/log p)) 0.0)
                                                                                    (let [q1 (- 1.0 p)]
                                                                                      (if (pos? q1) (* -1.0 q1 (Math/log q1)) 0.0)))))
                                                                               rates-universe)))))
                                                           q))]
                                      (recur (inc tau) (+ total risk amb)
                                             (if record?
                                               (conj! steps {:tau tau :risk risk :ambiguity amb
                                                             :belief q :rates rates})
                                               steps)))))))))))))))))))))

(defn horizon-g-sparse
  "Lean PolicyHorizon.horizonEFE at mission scale: exactly the numbers of
   horizon-g (futon2 233ad909) without ever enumerating the powerset. Two
   exact reductions: (1) C is evaluated pointwise by preference-fn's
   closed-form Z; (2) at zero adjudication rates A is the identity kernel
   (tokenLikelihood_checkable), so Q(o_τ|π) = q_τ and the ambiguity term is
   identically 0 — q_τ comes from rollout and only its support is scored.
   Non-zero rates refuse with the typed
   {:status :missing :kind :judgement-rates-not-supported-at-scale}: a
   declared current limitation, not a silent approximation. C is supplied
   either as :c-fn-pointwise (τ ↦ (o ↦ c(o)), step-indexed) or as :spec (a
   preference spec used as a DECLARED CONSTANT C_τ at every τ — the constant
   case, not a claim that C_τ is constant in general). :universe is the
   common token universe of the comparison (observation space of C); pass
   the same universe for every candidate compared. Returns the double
   sum, :infinite when any step's risk is infinite
   (horizonEFE_eq_top_iff), or the first typed refusal. Pure; no wiring.
   WIRE-1: the per-step record this function always had (and discarded)
   is exposed by horizon-g-sparse-cert; this function's return is
   unchanged.
   WIRE-4: NON-ZERO adjudication rates now score by the FACTORIZED closed
   forms (stepRisk = Σ_v KL(Bern(qbar_v)‖Bern(c_v)), stepAmbiguity = Σ_s
   q(s)·Σ_v H(Bern(p_v(s))), both O(|universe|) — no powerset), verified
   against the enumerating horizon-g to ~1e-13. Preconditions, each a
   typed refusal rather than an assumption: C from :spec with an EMPTY
   :zeroed (:zeroed-unsupported-with-rates), and each step's rollout
   belief a point mass or product form (a theta < 1 kernel's correlated
   mixture refuses :non-factorizable-belief; independent-belief is the
   product-form carrier). A step-indexed :c-fn-pointwise cannot supply
   per-token marginals and refuses :c-form-unsupported-with-rates.
   R7: an optional model-map `:zeta` (default 1, DECLARED FIXED) Gibbs-tempers
   the observation kernel on the factorized path via
   likelihood-precision/tempered-rates (tempering each token's Bernoulli pair
   IS row-wise A^ζ/Z for the product kernel, so one code path scores the
   tempered qbar_v and p_v(s) automatically). ζ = 1 is byte-identical to the
   untempered call; a ζ ≠ 1 with all-zero rates is the typed refusal
   :zeta-with-identity-rates, never a silent no-op."
  [m]
  (:g (horizon-g-sparse* m false)))

(defn horizon-g-sparse-cert
  "WIRE-1 emission slice: horizon-g-sparse with the per-step certificate
   the Lean specification DarkTower/AIF/Certificates.lean (GCertificate)
   requires. Returns {:g <exactly what horizon-g-sparse returns on the
   same input> :certificate <map or nil>}. The certificate records, per
   tau actually iterated, the risk with :risk-status :computed and the
   ambiguity with :ambiguity-status :reduced-identically-zero under the
   named reduction \"identity-A-zero-rates\" — the three QuantityStatus
   constructors stay distinct, so a 0 value alone never carries the
   distinction (Certificates.lean rule 2). :c-form is :constant-spec when
   C came from :spec and :step-indexed when :c-fn-pointwise was supplied.
   :rates-all-zero and :universe-size are read off the actual rates map
   of this call. WIRE-4: :evaluation names the path that ran
   (:identity-A-zero-rates versus :factorized-nonzero-rates), :rates
   echoes the rates used, and the factorized path's steps record the
   ambiguity they COMPUTED (:ambiguity-status :computed, reduction
   \"factorized-nonzero-rates\") instead of the identity path's
   reduced-identically-zero record. R7: :zeta echoes the declared FIXED ζ of
   the call (default 1) and :zeta-tempered? says whether the kernel was
   actually tempered, so a tempered run is distinguishable from an untempered
   one even when the numbers coincide. A refused computation returns
   {:certificate nil} — no certificate is fabricated for a computation
   that did not run; the refusal IS the record. Pure; emits, changes
   nothing."
  [m]
  (let [{:keys [g steps]} (horizon-g-sparse* m true)]
    (if (and (map? g) (contains? g :status))
      {:g g :certificate nil}
      {:g g
       :certificate {:consumed-g
                     {:A (:rates (first steps))
                      ;; A spec denotes the whole distribution, without
                      ;; enumerating its exponential outcome space. The live
                      ;; caller uses this path. A function-valued C cannot be
                      ;; serialized; absence is explicit, never guessed.
                      :C (when-not (:c-fn-pointwise m)
                           {:form :constant-spec :spec (:spec m)
                            :universe (:universe m)})
                      :D (:q0 m)
                      ;; These are the beliefs the risk calculation just
                      ;; consumed, not a second rollout. This evaluator calls
                      ;; rollout, with no observation updates, on both paths.
                      :Q {:steps (mapv #(select-keys % [:tau :belief]) steps)
                          :observation-updates []}}
                     :horizon (:horizon m)
                     :steps (mapv (fn [step]
                                   {:tau (:tau step)
                                    :risk (:risk step)
                                    :risk-status :computed
                                    ;; WIRE-4: the factorized path records the
                                    ;; ambiguity it COMPUTED per step; the
                                    ;; identity path's steps carry no
                                    ;; :ambiguity and keep the
                                    ;; reduced-identically-zero record.
                                    :ambiguity (:ambiguity step 0)
                                    :ambiguity-status (if (contains? step :ambiguity)
                                                        :computed
                                                        :reduced-identically-zero)
                                    :reduction (if (contains? step :ambiguity)
                                                 "factorized-nonzero-rates"
                                                 "identity-A-zero-rates")})
                                 (or steps []))
                     :total g
                     :c-form (if (:c-fn-pointwise m) :step-indexed :constant-spec)
                     ;; WIRE-4 provenance: which evaluation path ran and the
                     ;; rates it ran with — a run that scored a real
                     ;; observation model is distinguishable from one that
                     ;; assumed the identity kernel.
                     :evaluation (if (zero-rates? (:rates m))
                                   :identity-A-zero-rates
                                   :factorized-nonzero-rates)
                     :rates (:rates m)
                     :rates-all-zero (zero-rates? (:rates m))
                     ;; R7 provenance: which declared FIXED ζ the kernel was
                     ;; tempered at — a tempered run is distinguishable from
                     ;; an untempered one even when the numbers coincide.
                     :zeta (get m :zeta 1)
                     :zeta-tempered? (and (not (zero-rates? (:rates m)))
                                          (not= 1 (get m :zeta 1)))
                     ;; R7 (claude-4 ruling 2026-09-18): the DECLARATION is
                     ;; echoed, not just the number — on the identity path a
                     ;; fixed ζ exists and is VACUOUS (no likelihood matrix
                     ;; is evaluated), which is a different fact from ζ never
                     ;; being considered. The keywords are SOURCED from
                     ;; likelihood-precision/zeta-declaration's
                     ;; :certificate-statuses so this cannot drift from the
                     ;; declaration (same discipline as WIRE-2's
                     ;; :computed-not-attached for F).
                     :zeta-status (if (zero-rates? (:rates m))
                                    (:identity-path lprec/zeta-certificate-statuses)
                                    (:tempered-path lprec/zeta-certificate-statuses))
                     :universe-size (count (:rates m))}})))

;; ===== WM-02 design P12: the stored belief as the exact categorical posterior =====
;; Lean DarkTower.WarMachine.ExactBeliefTrajectory (mathlib4 ba0eda16df).

(defn exact-update
  "Lean ExactBeliefTrajectory.exactUpdate: the normalised exact categorical
   posterior s ↦ A(o|s) · (B q_prev)(s) / P(o), where
   P(o) = Σ_x A(o|x) · (B q_prev)(x) (observationProbability). Exact
   rationals throughout.

   Signature: (exact-update likelihood-of prior-pushed o), where
   likelihood-of is a function (fn [state observation] -> A(o|state)) and
   prior-pushed is the map {state (B q_prev)(state)} over states with
   predicted support. Returns the belief map {state mass} summing to 1
   (exactUpdate_dist), or the typed
   {:status :missing :kind :zero-predictive-probability} exactly when
   P(o) = 0 (exactUpdate_eq_none_iff). Refusals from likelihood-of or
   prior-pushed propagate."
  [likelihood-of prior-pushed o]
  (cond
    (refusal? prior-pushed) prior-pushed
    :else
    (let [likes (into {} (map (fn [s] [s (likelihood-of s o)]) (keys prior-pushed)))
          bad (first (filter refusal? (vals likes)))]
      (if bad
        bad
        (let [weighted (into {} (map (fn [[s pushed]] [s (* (get likes s) pushed)]) prior-pushed))
              po (reduce + (vals weighted))]
          (if (zero? po)
            {:status :missing :kind :zero-predictive-probability :observation o}
            (into {} (map (fn [[s w]] [s (/ w po)])) weighted)))))))

(defn token-belief-at
  "Lean ExactBeliefTrajectory.tokenBeliefAt over the approved carriers
   (P2–P5): μ₀ is the observed token-state belief q₀ (observed-belief,
   P4); step k pushes the current belief through cascade-kernel with the
   precedence list (precedence-fn k) (P3) and then conditions on the
   observation (observations (inc k)) with token-likelihood (P5) via
   exact-update. Exact rationals. Refusals — a zero-predictive-probability
   observation, or a carrier refusal — propagate and are carried forward
   for all later t (exactBeliefAt's Option.none)."
  [rates q0 precedence-fn observations t]
  (loop [k 0 q q0]
    (if (= k t)
      q
      (let [pushed (push-forward (precedence-fn k) q)]
        (if (refusal? pushed)
          pushed
          (let [updated (exact-update (fn [s o] (token-likelihood rates s o)) pushed
                                      (observations (inc k)))]
            (if (refusal? updated)
              updated
              (recur (inc k) updated))))))))
