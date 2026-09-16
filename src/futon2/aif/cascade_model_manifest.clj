(ns futon2.aif.cascade-model-manifest
  "Partial, source-bound token-frontier model. No scoring or live side effects."
  (:require [clojure.string :as str]
            [clojure.set :as set])
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
    (every? #(and (set/subset? (:present %) state)
                  (empty? (set/intersection (:absent %) state)))
            (get-in pattern [:guard :clauses]))))
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
   guard holds at state — present tokens ⊆ state and absent tokens ∩ state = ∅
   (guard-holds?), so absent tokens act as forbids. nil when none holds."
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
