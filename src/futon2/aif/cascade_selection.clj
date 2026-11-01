(ns futon2.aif.cascade-selection
  "Cascade selection posterior and Bayes action choice (WM-11, design P7).

   Lean to align with (mathlib4, DarkTower/WarMachine):
   - PolicySelection.lean @ a434947c63 — `selectionWeight`,
     `selectionPosterior`, `selectionWeight_top`,
     `selectionPosterior_eq_zero_of_risk_top`, `selectionPosterior_finite`,
     `selectionPosterior_all_finite`: one law, σ(log E − F − γG) with γ = 1/β,
     and a candidate whose G is infinite (it predicts, with positive mass, an
     outcome the preferences exclude) has probability exactly zero.
   - PolicyPrecision.lean @ ba1b2c42dd — β > 0, `policyPrecision` γ = 1/β,
     `precisionWeightedPosterior` (higher precision sharpens).
   - ActionMarginal.lean @ 6b55652425 — `actionMarginal` / `IsBayesAction`:
     the Bayes action maximises the posterior mass *summed over the policies
     that choose it* (per-state form `exists_bayesAction_at_state`), not the
     per-policy argmax.

   This namespace is the runtime quantity the P11 selector calls at step 3.
   It is NOT yet wired into the live path (the current selection head is the
   controller's; rebinding is a separate commission per ALIGNMENT.md order).
   Refusals are typed outcomes the Lean also states: β must be positive
   (PolicyTemperature), habit must be positive (habit prior mass), and at
   least one candidate must have finite G (selectionPosterior is undefined
   otherwise — division by a zero total)."
  (:require [futon2.aif.action-identity :as action-identity]
            [futon2.aif.load-identity :as load-identity]
            [clojure.math :as math]))

(load-identity/register! *ns* *file*)

;; ---------------------------------------------------------------------------
;; Refusals (typed, matching cascade-g's {:refusal {:kind ...}} convention)
;; ---------------------------------------------------------------------------

(defn- refuse!
  [kind detail]
  (throw (ex-info "Cascade selection refused"
                  {:refusal {:kind kind :detail detail}})))

(defn- valid-habit?
  [x]
  (and (number? x) (pos? x)))

;; ---------------------------------------------------------------------------
;; selection-posterior
;; ---------------------------------------------------------------------------

(defn- log-sum-exp
  "Stable log-sum-exp of a nonempty seq of scores."
  [scores]
  (let [m (reduce max scores)
        sum (transduce (map #(math/exp (- % m))) + 0.0 scores)]
    (+ m (math/log sum))))

(defn selection-posterior
  "Cascade selection posterior: {id probability} over candidate cascades.

   Args: {:beta β :candidates [{:id :habit :f :g} ...]}
   - β is the selection temperature, must be > 0 (γ = 1/β).
   - :habit is the cascade's habit mass E(π), must be > 0.
   - :f is F_π, a real number.
   - :g is the cascade's risk G, a real number or :infinite (EReal ⊤).

   Per PolicySelection.selectionPosterior_finite: every infinite-G candidate
   gets probability exactly 0.0; each finite candidate gets
   habit·exp(−F − G/β) (γ = 1/β), normalised over the finite candidates only.
   Computed in log space (log-sum-exp) so large G cannot overflow.

   Refuses with typed outcomes:
   - :invalid-temperature  — β not a positive number (PolicyPrecision: β > 0).
   - :invalid-habit        — any candidate's :habit not a positive number.
   - :free-energy-not-supplied — at least one menu policy has no admitted
     observed prefix (PrefixFreeEnergyPosterior.machineWeightsAtPrefixF).
   - :no-admissible-candidate — no candidates, or all have G = :infinite
     (the posterior normaliser would be 0)."
  [{:keys [beta candidates]}]
  (when-not (and (number? beta) (pos? beta))
    (refuse! :invalid-temperature {:beta beta}))
  (doseq [c candidates]
    (when-not (valid-habit? (:habit c))
      (refuse! :invalid-habit {:id (:id c) :habit (:habit c)})))
  ;; A non-finite F_pi makes every score -Inf, and log-sum-exp then divides
  ;; -Inf by -Inf: the posterior comes out NaN for every candidate and the
  ;; caller still gets a decision, chosen by tie-break, with NaN recorded as
  ;; the selection law. That is the one hole in this function's refusal
  ;; discipline (2026-09-18, found reviewing WIRE-2: under the identity-A
  ;; reduction F is infinite for every cascade that produces anything, so it
  ;; was not a corner case but the ordinary one). Refuse instead.
  (doseq [c candidates]
    (let [f (:f c)]
      (when-not (if (#{:not-supplied :zero-support :not-applicable} (:f-status c))
                  (nil? f)
                  (and (number? f) (Double/isFinite (double f))))
        (refuse! :invalid-free-energy {:id (:id c) :f f}))))
  ;; PrefixFreeEnergyPosterior.machineWeightsAtPrefixF is an Except: a menu
  ;; policy with no admitted history makes the weights absent.  Earlier code
  ;; silently omitted -F (numerically substituting the same contribution as
  ;; F=0), which is precisely the false carrier the Lean model forbids.
  (let [without-f (filterv #(= :not-supplied (:f-status %)) candidates)]
    (when (seq without-f)
      (refuse! :free-energy-not-supplied
               {:candidate-ids (mapv :id without-f)
                :n-candidates (count candidates)
                :n-without-f (count without-f)})))
  (let [;; WM-06 C-4 (2026-09-18): a numeric ##Inf G is EReal ⊤ arriving as a
        ;; double, not as the :infinite keyword — the same hole the F fix
        ;; above closed one field over. rank-cascade-actions attaches ##Inf
        ;; as controller-score for every refused/infinite G, so under the
        ;; ruled zeroed-under-rates family refusal (D-ZERO-PATH a) ALL
        ;; candidates arrive at ##Inf, every score is -Inf, log-sum-exp
        ;; divides -Inf by -Inf and the posterior is NaN — while the caller
        ;; still receives a tie-broken decision. Not-finite G is infinite G.
        finite-g? (fn [c] (and (not= :zero-support (:f-status c))
                               (number? (:g c))
                               (Double/isFinite (double (:g c)))))
        finite (filter finite-g? candidates)
        infinite (remove finite-g? candidates)]
    (when (empty? finite)
      (refuse! :no-admissible-candidate
               {:n-candidates (count candidates)
                :n-infinite (count infinite)}))
    (let [scores (mapv (fn [c]
                         (+ (math/log (double (:habit c)))
                            ;; certified zero history: the historical-prefix
                            ;; term is outside its domain and contributes none
                            (if (= :not-applicable (:f-status c))
                              0.0
                              (- (double (:f c))))
                            (- (/ (double (:g c)) (double beta)))))
                       finite)
          lse (log-sum-exp scores)
          probs (mapv #(math/exp (- % lse)) scores)]
      (into {}
            (concat (map (fn [c p] [(:id c) (double p)]) finite probs)
                    (map (fn [c] [(:id c) 0.0]) infinite))))))

(defn iterate-precision
  "R20Step.lean:690-692: fixed-count firstIterate, starting at betaPrior.
  PrecisionFromObservation.lean:35-52: priorPolicy, posteriorPolicy,
  expectedGChange and firstIterate. Uses selection-posterior's exact domain
  and validation; no convergence solver, clipping or implicit iteration count."
  [beta-prior iterations candidates]
  (when-not (nat-int? iterations)
    (refuse! :precision-iterations-unavailable {:iterations iterations}))
  (when-not (and (number? beta-prior) (pos? beta-prior)
                 (Double/isFinite (double beta-prior)))
    (refuse! :valence-beta-unavailable {:reason beta-prior}))
  (loop [i 0 beta beta-prior trace []]
    ;; Validate even the zero-iteration case.
    (let [posterior (selection-posterior {:beta beta :candidates candidates})]
      (if (= i iterations)
        {:beta-prior beta-prior :iterations iterations :beta-posterior beta :trace trace}
        (let [prior (selection-posterior
                     {:beta beta :candidates
                      ;; priorPolicy has no F term, so an impossible observed
                      ;; prefix (:zero-support, F = +inf) still has prior mass;
                      ;; only its posterior mass is 0.
                      (mapv #(assoc % :f 0.0 :f-status :computed) candidates)})
              delta (reduce + 0.0
                            (for [c candidates
                                  :when (and (number? (:g c))
                                             (Double/isFinite (double (:g c))))]
                              (* (- (get posterior (:id c)) (get prior (:id c))) (:g c))))
              next-beta (+ beta-prior delta)]
          (when-not (and (pos? next-beta) (Double/isFinite (double next-beta)))
            (refuse! :precision-iterate-nonpositive
                     {:iteration (inc i) :beta next-beta :beta-prior beta-prior}))
          (recur (inc i) next-beta (conj trace {:iteration (inc i) :beta next-beta})))))))

;; ---------------------------------------------------------------------------
;; bayes-choice
;; ---------------------------------------------------------------------------

(def ^:const bayes-choice-tie-rule
  "Declared stable tie-break: on equal action mass, the action whose printed
   name (str action) sorts first wins. Ascending lexicographic order over
   names, so the winner is independent of map/seq iteration order."
  :action-name-ascending)

(defn bayes-choice
  "Bayes action under the selection posterior (ActionMarginal.IsBayesAction).

   `posterior` is {id probability} (from selection-posterior); `action-of`
   maps a candidate id to its current action (the per-state projection
   `proj π s_t` of exists_bayesAction_at_state). Returns the action with
   maximum total posterior mass, i.e. the argmax over actions of
   Σ_{π : action-of(π) = a} Q(π) — the action marginal, NOT the per-policy
   argmax.

   Ties are broken by `bayes-choice-tie-rule` (ascending action name); the
   rule is recorded in the result under :tie-break-rule.

   Returns {:action a :mass m :tie-break-rule ...}.
   Refuses :unmapped-candidate if a posterior id has no action."
  [posterior action-of]
  (let [masses (reduce-kv (fn [acc id p]
                            (let [a (get action-of id ::unmapped)]
                              (when (= a ::unmapped)
                                (refuse! :unmapped-candidate {:id id}))
                              (update acc a (fnil + 0.0) p)))
                          {} posterior)]
    (when (empty? masses)
      (refuse! :no-admissible-candidate {:posterior posterior}))
    ;; stable order: sort by action name ascending, then take the max mass;
    ;; `max-key` keeps the FIRST of equal keys, so the smallest name wins ties.
    (let [ordered (sort-by (comp str key) masses)
          [a m] (reduce (fn [[_ best-m :as best] [a' m']]
                          (if (> m' best-m) [a' m'] best))
                        (first ordered) (rest ordered))]
      {:action a :mass (double m) :tie-break-rule bayes-choice-tie-rule})))

;; ---------------------------------------------------------------------------
;; The enacted object is a cascade (Joe, 2026-11-01; Requirements Q11)
;;
;; The runner commits to one whole cascade, so the law is the strict G minimum
;; over the compared policies. Policies predicting the same outcomes are one
;; policy under the model, and their count must not count for anything. An exact
;; tie refuses: no name, list or action order stands in for G. `bayes-choice`
;; stays for a grain where one unit is enacted and the agent re-selects.
;; ---------------------------------------------------------------------------

(def minimum-law :strict-G-minimum-over-prediction-classes)

(def representative-rule :first-in-ranked-order-within-one-prediction-class)

(def g-scale
  "Units of G per nat. G is compared at this scale here and in Lean, so scores
  within a unit are one score: arithmetic noise must not decide a selection."
  1000000)

(defn g-units
  "G as an integer count of `g-scale` units, HALF_EVEN; nil unless finite."
  [g]
  (when (and (number? g) (Double/isFinite (double g)))
    (-> (BigDecimal. (double g))
        (.setScale 6 java.math.RoundingMode/HALF_EVEN)
        (.movePointRight 6)
        .longValueExact)))

(defn prediction-digest
  "What the law scored for one policy, step by step: its progress and class
  predictions. Nil when the policy has no recorded progress predictions."
  [progress-steps class-steps]
  (when (seq progress-steps)
    (action-identity/digest [(mapv :prediction progress-steps)
                             (mapv :prediction class-steps)])))

(defn- prediction-class
  "One policy under the model: the same predicted outcomes at the same G. Two
  policies with one digest and different G differ in what the digest leaves out
  (fit-ambiguity, parameter information), so they compete by G. Entries with no
  recorded prediction are each their own class: nothing says they are one policy."
  [{:keys [action g prediction-digest]}]
  (if prediction-digest
    [prediction-digest (g-units g)]
    [:no-recorded-prediction action]))

(defn minimum-class
  "Group ROWS ({:action :g :prediction-digest}, ranked) into classes and find the
  strictly smallest G at `g-scale`. Returns {:status :unique :class … :runner-up …}
  or {:status :tied :detail …}. A class is {:digest :g :members [row…]}, members in
  ranked order."
  [rows]
  (let [classes (->> rows
                     (filter #(some? (g-units (:g %))))
                     (group-by prediction-class)
                     (mapv (fn [[digest members]]
                             {:digest digest :members members
                              :g (:g (first members))
                              :units (g-units (:g (first members)))})))
        ordered (sort-by (juxt :units (comp pr-str :digest)) classes)
        [best runner] ordered
        tied (filterv #(= (:units best) (:units %)) ordered)
        summary #(select-keys % [:digest :g :members])]
    (cond
      (empty? classes) {:status :no-scored-class}
      (< 1 (count tied)) {:status :tied
                          :detail {:digests (mapv :digest tied) :g (:g best)}}
      :else (cond-> {:status :unique :class (summary best)}
              runner (assoc :runner-up (summary runner))))))

(defn selection-row
  "A ranked entry as `minimum-class` reads it."
  [entry]
  {:action (:action entry)
   :g (:controller-score entry)
   :prediction-digest
   (prediction-digest (get-in entry [:certificate :steps])
                      (get-in entry [:class-emission :certificate :steps]))})

(defn enacted-action
  "The whole action that ran. A live decision carries it; a run record keeps it as
  the certificate candidate naming `[:decision :chosen]`. `:per-policy-argmax` is
  the posterior's mode and read only where a record names no chosen policy."
  [decision]
  (let [{:keys [id target]} (:chosen decision)]
    (or (:action decision)
        (when id
          (:id (first (filter #(and (= id (get-in % [:id :id]))
                                    (= target (get-in % [:id :target])))
                              (get-in decision [:selection-certificate :candidates])))))
        (get-in decision [:selection-law :per-policy-argmax :action]))))

(defn strict-minimum
  "`minimum-class`, refusing unless the minimum is unique."
  [rows]
  (let [{:keys [status detail] :as result} (minimum-class rows)]
    (case status
      :unique result
      :tied (refuse! :tie-at-minimum detail)
      (refuse! :no-admissible-candidate {:rows (count rows)}))))

;; ---------------------------------------------------------------------------
;; law receipt (PROOF-2 packet 27 / F-ABS, 2026-09-24)
;;
;; The `wm-policy-selection` contract bound at cascade_selection.clj:51
;; (PolicySelection.lean @ a434947c63: one law, σ(log E − F − γG), γ = 1/β)
;; presents the full law. When F is absent for a candidate, selection-posterior
;; omits the term (line 112: `(if (= :not-supplied (:f-status c)) 0.0 …)`) —
;; the arithmetic is UNCHANGED by this receipt; the receipt only says which law
;; actually ran. F absence is typed evidence on the record, not a refusal,
;; for an engineering reason: F is absent on every recorded click, so a
;; refusal would halt selection everywhere and hide what ran; the receipt
;; keeps the machine running AND makes the record say which law it ran.
;; ---------------------------------------------------------------------------

(defn f-consumed-record
  "The exact F value the selection law consumed for one candidate, or the
   typed absence. A missing prefix contributes NO term — that is not a
   measured F = 0, and the record must not say one was consumed."
  [c]
  (cond
    (= :not-applicable (:f-status c))
    {:status :not-applicable :reason :certified-zero-history
     :term :historical-prefix-f :consumed :no-term}

    (or (= :not-supplied (:f-status c)) (nil? (:f c)))
    {:status :absent :reason :not-supplied}

    :else (double (:f c))))

(defn law-receipt
  "Per-decision receipt naming the selection law that actually ran, under the
   wm-policy-selection contract (cascade_selection.clj:51; PolicySelection.lean
   @ a434947c63).

   - every candidate has finite consumed F → the full law
     σ(log E − F − γG);
   - every candidate's F absent → the reduced law σ(log E − γG) with the F
     term omitted, listing the candidates;
   - a mixed field is its own case — named with BOTH per-candidate lists,
     never collapsed to either pure law;
   - a candidate whose F is :not-applicable (certified zero history) has the
     historical-prefix term omitted for that proven domain reason, listed
     under :candidates-f-not-applicable; it is never counted as missing F.

   Evidence only: same input, same scores; nothing refuses."
  [candidates]
  (let [not-applicable? #(= :not-applicable (:f-status %))
        absent? (fn [c] (and (not (not-applicable? c))
                             (or (= :not-supplied (:f-status c)) (nil? (:f c)))))
        without (mapv :id (filter absent? candidates))
        outside-domain (mapv :id (filter not-applicable? candidates))
        with (mapv :id (remove #(or (absent? %) (not-applicable? %)) candidates))
        receipt
        (cond
          (and (empty? without) (seq outside-domain))
          {:law (if (seq with)
                  :sigma-log-E-minus-gamma-G-with-F-where-applicable
                  :sigma-log-E-minus-gamma-G)
           :omitted-terms [:F]
           :reason :f-not-applicable-certified-zero-history
           :candidates-with-f with}

          (empty? without)
          {:law :sigma-log-E-minus-F-minus-gamma-G}

          (empty? with)
          {:law :sigma-log-E-minus-gamma-G
           :omitted-terms [:F]
           :reason :f-not-supplied
           :candidates-without-f without}

          :else
          {:law :sigma-log-E-minus-gamma-G-with-F-where-supplied
           :omitted-terms [:F]
           :reason :f-partially-supplied
           :candidates-with-f with
           :candidates-without-f without})]
    ;; the proven-empty-history domain reason is named per candidate,
    ;; separately from any missing F
    (cond-> receipt
      (seq outside-domain)
      (assoc :candidates-f-not-applicable outside-domain
             :f-not-applicable-reason :certified-zero-history))))

(defn- consumed-f [c]
  ;; Exactly selection-posterior's absent-prefix semantics. Legacy unattached
  ;; F already carries its neutral consumed value; retain its status below.
  (if (#{:not-supplied :not-applicable} (:f-status c)) 0.0 (double (:f c))))

(defn- threshold-record [declaration total]
  (let [v (:value declaration)
        status (cond
                 (nil? declaration) :undeclared
                 (and (= :declared (:status declaration))
                      (number? v) (Double/isFinite (double v)) (not (neg? v))) :declared
                 :else :invalid)]
    {:near-tie-threshold (if (= :undeclared status) {:status :undeclared} declaration)
     :near-tie? (case status
                  :declared (if (number? total) (<= (abs total) v) :no-competing-policy)
                  :undeclared :threshold-undeclared
                  :invalid :threshold-invalid)}))

(defn selection-comparisons
  "Recording only: the enacted class against the runner-up class. G decides, so
  `:decided-by` is :G whenever a class competed. `:favoured-by` is the term of
  the posterior that most favoured the enacted class over the runner-up, which
  says how the record's other quantities leaned and decides nothing. Missing or
  invalid near-tie declarations are typed telemetry, never new selection gates."
  [{:keys [beta candidates posterior minimum near-tie-threshold]}]
  (let [by-id (into {} (map (juxt :id identity)) candidates)
        w (-> minimum :class :members first :action)
        r (-> minimum :runner-up :members first :action)
        wc (get by-id w) rc (get by-id r)
        policy-row (fn [c p]
                     (assoc (select-keys c [:id :habit :f :f-status :computed-f :reason])
                            :posterior p))
        terms (when rc
                (let [habit (- (math/log (double (:habit wc))) (math/log (double (:habit rc))))
                      f (- (consumed-f rc) (consumed-f wc))
                      g (/ (- (double (:g rc)) (double (:g wc))) (double beta))]
                  {:habit habit :free-energy f :G g :total (+ habit f g)}))
        favoured (when terms
                   (first (sort-by #(- (get terms %))
                                   (filter #(pos? (get terms %)) [:habit :free-energy :G]))))]
    {:policy-comparison
     (merge
      {:comparison-domain :prediction-class
       :status (if rc :compared :no-competing-policy)
       :winner (policy-row wc (get posterior w))
       :runner-up (if rc (policy-row rc (get posterior r)) :no-competing-policy)
       :contributions terms
       :decided-by (if rc :G :no-competing-policy)
       :favoured-by (or favoured :no-competing-policy)
       :contribution-tie-order [:habit :free-energy :G]}
      (threshold-record near-tie-threshold (:total terms)))}))
