(ns futon2.aif.node-sim
  "PER-NODE SIMULATION HARNESS: run ONE control-stages node in isolation
   against declared carriers, and check what it computes against an
   independently evaluated reference and against the laws its own
   `aif-equations.edn` rows state.

   WHY A NODE RUNS ALONE. `holes/labs/wm-contract/worklist.edn` `:F3`. A node
   of the control map is a set of equations over named symbols
   (`aif-equations.edn`); nothing about running it needs the loop, the
   scheduler, a tick or a trace. What it needs is its imports supplied as
   values -- which is exactly what a caller of this namespace does. No live
   tick is taken here, no run lock, nothing is written under `data/`.

   WHAT THE HARNESS IS, PRECISELY. Three things, kept apart:

     THE NODE       the transcription under test -- `(:run spec)`, or a
                    `:node-fn` a caller substitutes. A caller substitutes a
                    WRONG one on purpose: that is the negative control.
     THE REFERENCE  the same formulas evaluated by a different route --
                    exact rational masses, prime-factorised logarithms, one
                    `Math/log` per prime. Derived from the CARRIERS, never
                    from the node, which is what makes a planted formula
                    visible. It is NOT an independent derivation of the
                    formulas themselves: both routes transcribe
                    `mathlib4/DarkTower/WarMachine/Holes.lean:6867-6884`.
     THE LAWS       statements the node's output must satisfy whatever its
                    inputs are -- Gibbs, the entropy range, the two-term
                    identity, and the flat-plan control that says a
                    policy-conditioned difference comes from the planned step.

   R5, THE PILOT NODE. `risk`, `ambiguity` and `expected-free-energy`
   (`aif-equations.edn:116-142`), at the DECLARED OUTCOME ALPHABET grain --
   the grain `Q(o|pi)` is stated over. This is NOT the grain the running
   machine's R5 uses: `futon2.aif.core-efe` scores per-channel Gaussians
   (`src/futon2/aif/core_efe.clj:56-92`) and the live blend adds engineering
   legs (`src/futon2/aif/efe.clj:842-872`), neither of which can consume a
   distribution over six declared outcomes. The three R5 rows of the registry
   carry no `:code` pointer, and that absence is the reason: nothing in `src/`
   ran these equations before this namespace. So a passing R5 simulation is
   evidence about the TRANSCRIPTION and its carriers, and about nothing that
   ticked.

   THE CARRIERS ARE DECLARED, NOT INHABITED. Every carrier a node needs that
   the War Machine does not have -- the state space, the policy family, the
   belief reading, the preference distribution `C` -- is supplied by the
   caller and refused when absent, the same stance
   `futon2.aif.machine-q` takes for `Q(o|pi)`. Under
   `holes/labs/wm-contract/FUNDAMENTALS.edn` `:what-does-not-count` a carrier
   declared for a demonstration inhabits nothing, so nothing here closes a
   fundamental and nothing here is a ruling.

   EXACT CARRIERS. The reference route needs rationals, so a carrier stated as
   a double is refused rather than rounded: `:refusal/inexact-carrier-mass`."
  (:require [futon2.aif.machine-q :as mq]))

(def tolerance
  "Agreement tolerance between the node's double arithmetic and the exact
   reference. Both evaluate the same sum; they differ only in rounding, which
   for these magnitudes lands near 1e-16, so 1e-12 is loose by four orders and
   still far tighter than any formula error could hide under."
  1.0e-12)

(defn- refuse!
  [reason message data]
  (throw (ex-info message (assoc data :refusal reason))))

;; ---------------------------------------------------------------------------
;; Exact arithmetic: the reference route
;; ---------------------------------------------------------------------------

(defn- num-den
  "Numerator and denominator of an exact rational. A double is refused: the
   reference's whole claim is that it did not round before the last step."
  [x]
  (cond
    (ratio? x) [(numerator x) (denominator x)]
    (integer? x) [x 1]
    :else (refuse! :refusal/inexact-carrier-mass
                   "the reference route is stated over exact rational masses"
                   {:value x :type (str (type x))})))

(defn- prime-exponents
  "The prime factorisation of a positive integer as {prime exponent}."
  [n]
  (let [n (long n)]
    (when-not (pos? n)
      (refuse! :refusal/non-positive-mass
               "a logarithm was asked for at a non-positive mass" {:value n}))
    (loop [m n d 2 acc {}]
      (cond
        (= m 1) acc
        (> (* d d) m) (update acc m (fnil inc 0))
        (zero? (rem m d)) (recur (quot m d) d (update acc d (fnil inc 0)))
        :else (recur m (inc d) acc)))))

(defn- log-coefficients
  "ln(x) for an exact positive rational x, as {prime exponent} -- so that a sum
   of many logarithms becomes one exact coefficient per prime, evaluated in
   floating point exactly once at the end."
  [x]
  (let [[n d] (num-den x)]
    (merge-with + (prime-exponents n)
                (into {} (map (fn [[p e]] [p (- e)])) (prime-exponents d)))))

(defn- add-scaled
  "acc + weight * coeffs, with weight an exact rational."
  [acc weight coeffs]
  (reduce-kv (fn [m p e] (update m p (fnil + 0) (* weight e))) acc coeffs))

(defn- evaluate-log-coefficients
  "Sum coeff * ln(prime) over the primes in key order, so the value does not
   depend on map iteration order."
  [coeffs]
  (reduce (fn [acc [p c]] (+ acc (* (double c) (Math/log (double p)))))
          0.0
          (sort-by key coeffs)))

;; ---------------------------------------------------------------------------
;; Carriers
;; ---------------------------------------------------------------------------

(defn preference-distribution!
  "Validate `C`, the preference distribution over the declared outcome
   alphabet, and return it.

   STRICT POSITIVITY IS THE LEAN HYPOTHESIS, NOT AN EXTRA. `predictiveOutcomeRisk`
   takes `_positivePreference : forall pi o, o IN Q.support pi -> 0 < Cdist.mass () o`
   (`Holes.lean:6867-6871`); a zero preferred mass puts the real-valued formula
   outside its domain, and the row's own recorded falsifier is exactly
   \"predictive support contains an outcome with zero preference mass\". A
   runtime that renormalised or floored it would be answering a different
   question, so this refuses."
  [c outcomes]
  (let [declared (set outcomes)]
    (when-not (map? c)
      (refuse! :refusal/preference-not-a-map
               "C is a map from declared outcome to preferred mass" {:got (str (type c))}))
    (when-not (= (set (keys c)) declared)
      (refuse! :refusal/preference-support-mismatch
               "C is not stated over the declared outcome alphabet"
               {:support (set (keys c)) :outcomes declared}))
    (doseq [[o m] c]
      (when-not (and (number? m) (pos? (double m)))
        (refuse! :refusal/preference-zero-on-support
                 "C puts no mass on a declared outcome; the risk term has no value there"
                 {:outcome o :mass m})))
    (let [total (reduce + 0.0 (map (comp double val) c))]
      (when (>= (Math/abs (- 1.0 total)) mq/tolerance)
        (refuse! :refusal/preference-unnormalised
                 "C does not sum to one" {:sum total})))
    c))

(defn- expand-transition
  "The carriers state B action-conditioned; `machine-q` keys it by [state
   action]. The expansion is where state-independence stops being implicit:
   every declared state gets the same row for a given action, and a carriers
   file that meant something else would have to say so in another field."
  [transition-by-action states]
  (into {} (for [s states [u row] transition-by-action] [[s u] row])))

(defn carriers!
  "Validate a carriers declaration and return the runtime form the harness
   runs a node against.

     {:node       :R5
      :id         <keyword naming the declaration, for the receipt>
      :states     [s ..]      :outcomes [o ..]
      :observation {s {o mass}}
      :transition-by-action {u {s' mass}}
      :plan {pi u}            :flat-plan {pi u}
      :beliefs [{:id .. :mass {s mass}} ..]
      :preference {o mass}}

   The model and the reading are validated by `machine-q` itself -- the same
   `generative-model!` and `q-reading!` the F1 seam uses -- so a carriers file
   the harness accepts is one that composition accepts."
  [{:keys [node id states outcomes observation transition-by-action
           plan flat-plan beliefs preference pinned]}]
  (when-not (keyword? node)
    (refuse! :refusal/carriers-unnamed-node "a carriers declaration names its node" {}))
  (when-not (keyword? id)
    (refuse! :refusal/carriers-unnamed "a carriers declaration carries an :id" {:node node}))
  (when-not (seq beliefs)
    (refuse! :refusal/carriers-without-belief
             "a carriers declaration states the belief the node is run at" {:id id}))
  (let [model (mq/generative-model!
               {:states states :outcomes outcomes
                :transition (expand-transition transition-by-action states)
                :observation observation})
        belief-mass (fn [b s] (get b s 0))
        reading (mq/q-reading! {:id id :plan plan :belief-mass belief-mass} model)
        flat-id (keyword (namespace id) (str (name id) "-flat"))
        flat (mq/q-reading! {:id flat-id :plan flat-plan :belief-mass belief-mass} model)]
    (preference-distribution! preference outcomes)
    (doseq [b beliefs]
      (mq/belief-distribution! model reading (:mass b)))
    {:node node :id id :model model :preference preference
     :plan plan :flat-plan flat-plan
     :reading reading :flat-reading flat
     :beliefs (vec beliefs) :pinned pinned}))

;; ---------------------------------------------------------------------------
;; R5: the node under test, and its reference
;; ---------------------------------------------------------------------------

(defn kl-divergence
  "`predictiveOutcomeRisk` (`Holes.lean:6867-6871`), transcribed:
   KL[Q(o|pi) || C] = SUM_o Q(o) ln(Q(o)/C(o)), over the declared alphabet.

   Mathlib's `Real.log 0 = 0` gives `0 * log 0 = 0`; that convention is
   written out here rather than inherited from the floating-point library,
   which would produce NaN."
  [q c]
  (reduce (fn [acc [o mass]]
            (let [qo (double mass)]
              (if (zero? qo)
                acc
                (+ acc (* qo (Math/log (/ qo (double (get c o 0.0)))))))))
          0.0
          (sort-by key q)))

(defn row-entropy
  "`observationEntropy` (`Holes.lean:6875-6877`): the Shannon entropy in nats
   of one observation-kernel row."
  [row]
  (- (reduce (fn [acc [_ mass]]
               (let [p (double mass)]
                 (if (zero? p) acc (+ acc (* p (Math/log p))))))
             0.0
             (sort-by key row))))

(defn expected-observation-entropy
  "`ambiguity` (`Holes.lean:6880-6884`): the predicted state mass weighting the
   entropy of that state's observation row, E_{Q(s|pi)}[H(P(o|s))]."
  [state-row observation]
  (reduce (fn [acc [s mass]]
            (+ acc (* (double mass) (row-entropy (get observation s)))))
          0.0
          (sort-by key state-row)))

(defn r5
  "The R5 node: the two-term core, and nothing else.

   THE R5 INVARIANT the pilot exists to hold: what comes out is `risk`,
   `ambiguity` and their sum. No temperature, no habit prior, no structural
   pressure, no augmentation leg -- those live in the controller blend
   (`src/futon2/aif/efe.clj:842-872`) and the harness refuses an output that
   carries them, so \"the two-term core persisted apart from any engineering
   controls\" is a checked property of this map rather than a claim about it."
  [{:keys [policies outcome-rows state-rows observation preference]}]
  (let [risk (into {} (map (fn [pi] [pi (kl-divergence (get outcome-rows pi) preference)])) policies)
        amb (into {} (map (fn [pi] [pi (expected-observation-entropy (get state-rows pi) observation)])) policies)]
    {:risk risk
     :ambiguity amb
     :G (into {} (map (fn [pi] [pi (+ (get risk pi) (get amb pi))])) policies)}))

(defn- exact-state-row
  [{:keys [states transition]} plan belief policy]
  (let [u (get plan policy)]
    (into {} (map (fn [s']
                    [s' (reduce (fn [acc s]
                                  (+ acc (* (get belief s 0)
                                            (get-in transition [[s u] s'] 0))))
                                0
                                states)]))
          states)))

(defn- exact-outcome-row
  [{:keys [states outcomes observation] :as model} plan belief policy]
  (let [q-s (exact-state-row model plan belief policy)]
    (into {} (map (fn [o]
                    [o (reduce (fn [acc s]
                                 (+ acc (* (get q-s s 0) (get-in observation [s o] 0))))
                               0
                               states)]))
          outcomes)))

(defn reference-r5
  "R5 by the other route. Every mass is an exact rational, every logarithm is
   expanded over the primes of its argument, and floating point enters once per
   prime at the end -- so the reference and the node share their formulas and
   share nothing else. The prime coefficients are returned as well as the
   values: they are exact rationals a reviewer can check by hand."
  [{:keys [model plan belief preference policies]}]
  (let [{:keys [observation]} model
        risk-coeffs
        (into {} (map (fn [pi]
                        (let [row (exact-outcome-row model plan belief pi)]
                          [pi (reduce (fn [acc [o q]]
                                        (if (zero? q)
                                          acc
                                          (add-scaled acc q (log-coefficients
                                                             (/ q (get preference o))))))
                                      {}
                                      (sort-by key row))])))
              policies)
        amb-coeffs
        (into {} (map (fn [pi]
                        (let [q-s (exact-state-row model plan belief pi)]
                          [pi (reduce (fn [acc [s qs]]
                                        (reduce (fn [a [_ p]]
                                                  (if (zero? p)
                                                    a
                                                    (add-scaled a (* qs (- p)) (log-coefficients p))))
                                                acc
                                                (sort-by key (get observation s))))
                                      {}
                                      (sort-by key q-s))])))
              policies)
        risk (into {} (map (fn [[pi c]] [pi (evaluate-log-coefficients c)])) risk-coeffs)
        amb (into {} (map (fn [[pi c]] [pi (evaluate-log-coefficients c)])) amb-coeffs)]
    {:risk risk
     :ambiguity amb
     :G (into {} (map (fn [pi] [pi (+ (get risk pi) (get amb pi))])) policies)
     :exact {:risk (into {} (map (fn [[pi c]] [pi (into (sorted-map) c)])) risk-coeffs)
             :ambiguity (into {} (map (fn [[pi c]] [pi (into (sorted-map) c)])) amb-coeffs)}}))

;; ---------------------------------------------------------------------------
;; The node registry
;; ---------------------------------------------------------------------------

(def node-registry
  "One entry per control-map node the harness can run. `:equations` are the
   `aif-equations.edn` row ids the node hosts; `:consumes` is what the
   transcription actually reads, stated separately from the registry's own
   `:imports` so the two can be COMPARED rather than assumed equal."
  {:R5 {:node :R5
        :label "Expected free energy core"
        :equations #{:risk :ambiguity :expected-free-energy}
        :consumes {:risk #{:Q-o-pi :C}
                   :ambiguity #{:Q-s-pi :A}
                   :expected-free-energy #{:risk :ambiguity}}
        :output-keys #{:risk :ambiguity :G}
        :run r5
        :reference reference-r5}})

;; ---------------------------------------------------------------------------
;; The harness
;; ---------------------------------------------------------------------------

(defn node-inputs
  "The node's imports, supplied as values: this is the whole of \"in
   isolation\". `machine-q` builds Q(s|pi) and Q(o|pi) -- the same functions
   F1 pinned -- and nothing else is read."
  [{:keys [model reading preference]} belief]
  (let [policies (vec (sort (keys (:plan reading))))]
    {:policies policies
     :outcomes (:outcomes model)
     :observation (:observation model)
     :preference preference
     :state-rows (into {} (map (fn [pi] [pi (mq/predicted-state-distribution model reading belief pi)])) policies)
     :outcome-rows (:rows (mq/predictive-outcome-kernel model reading belief))}))

(defn- chk
  [id status data]
  (merge {:check id :status status} data))

(defn- close?
  [a b]
  (< (Math/abs (- (double a) (double b))) tolerance))

(defn- rows-close?
  [a b]
  (every? (fn [o] (close? (get a o 0.0) (get b o 0.0)))
          (into #{} (concat (keys a) (keys b)))))

(defn- max-deviation
  [a b]
  (reduce max 0.0 (map (fn [k] (Math/abs (- (double (get a k 0.0)) (double (get b k 0.0)))))
                       (into #{} (concat (keys a) (keys b))))))

(defn- zeroed-preference
  "The declared C with the last declared outcome's mass moved onto the first,
   so the only thing wrong with it is the zero. Normalisation survives, which
   is what makes the refusal attributable to positivity."
  [preference outcomes]
  (let [lo (last outcomes) fo (first outcomes)]
    (-> preference
        (assoc fo (+ (get preference fo) (get preference lo)))
        (assoc lo 0))))

(defn simulate-node
  "Run one node against declared carriers and return the full record.

     {:carriers ..  :registry-rows [..]  :node-fn <optional substitute>}

   `:node-fn` is how a WRONG node is planted: the reference and every law are
   computed from the carriers, so a substituted formula has nothing to hide
   behind. `:verdict` is `:fail` if any check failed, and `:failed` names them."
  [{:keys [carriers registry-rows node-fn]}]
  (let [{:keys [node model flat-reading plan flat-plan beliefs preference pinned id]} carriers
        spec (get node-registry node)
        _ (when-not spec
            (refuse! :refusal/node-not-in-registry
                     "the harness has no simulation for this node" {:node node}))
        run (or node-fn (:run spec))
        primary (first beliefs)
        inputs (node-inputs carriers (:mass primary))
        policies (:policies inputs)
        values (run inputs)
        reference ((:reference spec) {:model model :plan plan :belief (:mass primary)
                                      :preference preference :policies policies})
        outcome-rows (:outcome-rows inputs)
        ;; laws that need the node run again on altered inputs
        own-row-risk (into {} (map (fn [pi]
                                     [pi (get-in (run (assoc inputs :preference (get outcome-rows pi)))
                                                 [:risk pi])]))
                           policies)
        flat-inputs (node-inputs (assoc carriers :reading flat-reading) (:mass primary))
        flat-values (run flat-inputs)
        registry-imports (into {} (map (fn [r] [(:id r) (set (:imports r))])) registry-rows)
        registry-at-node (set (keys registry-imports))
        checks
        [(chk :equations-implemented
              (if (= registry-at-node (:equations spec)) :pass :fail)
              {:registry registry-at-node :harness (:equations spec)})
         (chk :registry-imports
              (if (= registry-imports (:consumes spec)) :pass :finding)
              {:per-equation
               (into {} (map (fn [[eq declared]]
                               (let [reads (get (:consumes spec) eq #{})]
                                 [eq (if (= declared reads)
                                       {:status :agrees :imports declared}
                                       {:status :differs
                                        :only-in-registry (into #{} (remove reads declared))
                                        :only-in-harness (into #{} (remove declared reads))})])))
                     registry-imports)})
         (chk :kernel-reproduces-pinned
              (if (and (every? (fn [pi] (rows-close? (get outcome-rows pi) (get-in pinned [:rows pi])))
                               policies)
                       (every? (fn [pi] (close? (get-in outcome-rows [pi (:pinned-outcome pinned)])
                                                (get-in pinned [:lean-row-masses pi])))
                               policies))
                :pass :fail)
              {:max-deviation (reduce max 0.0 (map (fn [pi] (max-deviation (get outcome-rows pi)
                                                                           (get-in pinned [:rows pi])))
                                                   policies))
               :lean-source (:lean-source pinned)
               :rows-source (:rows-source pinned)})
         (chk :belief-independence
              (if (every? (fn [b]
                            (let [other (node-inputs carriers (:mass b))]
                              (every? (fn [pi] (rows-close? (get outcome-rows pi)
                                                            (get-in other [:outcome-rows pi])))
                                      policies)))
                          (rest beliefs))
                :pass :fail)
              {:beliefs (mapv :id beliefs)
               :reads (str "MachineQWitness.lean:229-241 states the row masses are independent of "
                           "the belief because this model's transition rows do not depend on the "
                           "source state; the harness runs every declared belief and compares.")})
         (chk :node-agrees-with-reference
              (if (every? (fn [k] (every? (fn [pi] (close? (get-in values [k pi]) (get-in reference [k pi])))
                                          policies))
                          [:risk :ambiguity :G])
                :pass :fail)
              {:max-deviation (reduce max 0.0 (map (fn [k] (max-deviation (get values k) (get reference k)))
                                                   [:risk :ambiguity :G]))})
         (chk :two-term-core
              (if (and (= (set (keys values)) (:output-keys spec))
                       (every? (fn [pi] (close? (get-in values [:G pi])
                                                (+ (get-in values [:risk pi]) (get-in values [:ambiguity pi]))))
                               policies))
                :pass :fail)
              {:output-keys (set (keys values)) :expected (:output-keys spec)})
         (chk :risk-nonnegative
              (if (every? (fn [pi] (>= (double (get-in values [:risk pi])) (- tolerance))) policies) :pass :fail)
              {:risk (:risk values)})
         (chk :risk-zero-at-own-row
              (if (every? (fn [pi] (close? (get own-row-risk pi) 0.0)) policies) :pass :fail)
              {:max-deviation (reduce max 0.0 (map (fn [pi] (Math/abs (double (get own-row-risk pi)))) policies))
               :reads "Gibbs' equality case: KL[Q||C] = 0 exactly when C is Q."})
         (chk :ambiguity-in-entropy-range
              (let [hi (Math/log (double (count (:outcomes inputs))))]
                (if (every? (fn [pi] (let [a (double (get-in values [:ambiguity pi]))]
                                       (and (>= a (- tolerance)) (<= a (+ hi tolerance)))))
                            policies)
                  :pass :fail))
              {:upper-bound (Math/log (double (count (:outcomes inputs))))
               :ambiguity (:ambiguity values)})
         (chk :flat-plan-control
              (if (and (apply = (map (fn [pi] (get-in flat-values [:risk pi])) policies))
                       (apply = (map (fn [pi] (get-in flat-values [:ambiguity pi])) policies)))
                :pass :fail)
              {:reads (str "MachineQWitness.lean:267-274 flatReadingRowsCoincide, carried through R5: "
                           "with both policies planning the same step the two terms coincide, so a "
                           "difference above is produced by the planned action and not by the node.")
               :flat-plan flat-plan
               :risk (:risk flat-values)
               :ambiguity (:ambiguity flat-values)})
         (chk :preference-positivity-refused
              (let [reason (try (preference-distribution! (zeroed-preference preference (:outcomes inputs))
                                                          (:outcomes inputs))
                                nil
                                (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e))))]
                (if (= :refusal/preference-zero-on-support reason) :pass :fail))
              {:reads (str "Holes.lean:6869 (the positivePreference hypothesis), and the row's own "
                           "recorded falsifier, as a runtime refusal.")})]
        failed (mapv :check (filter (comp #{:fail} :status) checks))]
    {:node node
     :carriers-id id
     :tolerance tolerance
     :policies policies
     :inputs {:state-rows (:state-rows inputs)
              :outcome-rows outcome-rows
              :preference preference}
     :values values
     :reference reference
     :checks checks
     :failed failed
     :verdict (if (seq failed) :fail :pass)}))
