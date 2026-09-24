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
  (:require [futon2.aif.load-identity :as load-identity]
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
      (when-not (if (#{:not-supplied :zero-support} (:f-status c))
                  (nil? f)
                  (and (number? f) (Double/isFinite (double f))))
        (refuse! :invalid-free-energy {:id (:id c) :f f}))))
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
                            ;; Missing prefix contributes no term, not a measured F=0.
                            (if (= :not-supplied (:f-status c)) 0.0 (- (double (:f c))))
                            (- (/ (double (:g c)) (double beta)))))
                       finite)
          lse (log-sum-exp scores)
          probs (mapv #(math/exp (- % lse)) scores)]
      (into {}
            (concat (map (fn [c p] [(:id c) (double p)]) finite probs)
                    (map (fn [c] [(:id c) 0.0]) infinite))))))

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
  (if (or (= :not-supplied (:f-status c)) (nil? (:f c)))
    {:status :absent :reason :not-supplied}
    (double (:f c))))

(defn law-receipt
  "Per-decision receipt naming the selection law that actually ran, under the
   wm-policy-selection contract (cascade_selection.clj:51; PolicySelection.lean
   @ a434947c63).

   - every candidate has finite consumed F → the full law
     σ(log E − F − γG);
   - every candidate's F absent → the reduced law σ(log E − γG) with the F
     term omitted, listing the candidates;
   - a mixed field is its own case — named with BOTH per-candidate lists,
     never collapsed to either pure law.

   Evidence only: same input, same scores; nothing refuses."
  [candidates]
  (let [absent? (fn [c] (or (= :not-supplied (:f-status c)) (nil? (:f c))))
        without (mapv :id (filter absent? candidates))
        with (mapv :id (remove absent? candidates))]
    (cond
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
       :candidates-without-f without})))

(defn- ordered-masses [m]
  (sort-by (fn [[id p]] [(- p) (pr-str id)]) (filter (comp pos? val) m)))

(defn- consumed-f [c]
  ;; Exactly selection-posterior's absent-prefix semantics. Legacy unattached
  ;; F already carries its neutral consumed value; retain its status below.
  (if (= :not-supplied (:f-status c)) 0.0 (double (:f c))))

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
  "Recording only. Compare the top two positive-mass ACTING policies, then
   diagnose the actual action marginal using the same posterior/Bayes code.
   Policy ties use printed identity ascending; equal positive contributions
   use the declared order [:habit :free-energy :G]. Neither chooses an action.
   Neutralizations preserve zero-support F and non-finite G exclusions: these
   are support constraints, not finite score contributions. Missing or invalid
   near-tie declarations are typed telemetry, never new selection gates."
  [{:keys [beta candidates posterior action-of choice near-tie-threshold]}]
  (let [by-id (into {} (map (juxt :id identity)) candidates)
        acting (into {} (filter (fn [[id _]] (some? (get action-of id)))) posterior)
        [[w wp] [r rp]] (ordered-masses acting)
        wc (get by-id w) rc (get by-id r)
        policy-row (fn [c p]
                     (assoc (select-keys c [:id :habit :f :f-status :computed-f :reason])
                            :posterior p))
        terms (when r
                (let [habit (- (math/log (double (:habit wc))) (math/log (double (:habit rc))))
                      f (- (consumed-f rc) (consumed-f wc))
                      g (/ (- (double (:g rc)) (double (:g wc))) (double beta))]
                  {:habit habit :free-energy f :G g :total (+ habit f g)}))
        dominant (when terms
                   (if (zero? (:total terms)) :tie-break
                       (first (sort-by #(- (get terms %))
                                       (filter #(pos? (get terms %)) [:habit :free-energy :G])))))
        policy-record (merge
                       {:comparison-domain :acting-policy
                        :status (if r :compared :no-competing-policy)
                        :winner (policy-row wc wp)
                        :runner-up (if r (policy-row rc rp) :no-competing-policy)
                        :contributions terms :decided-by (or dominant :no-competing-policy)
                        :tie-break-rule :policy-printed-identity-ascending
                        :contribution-tie-order [:habit :free-energy :G]}
                       (threshold-record near-tie-threshold (:total terms)))
        marginal (reduce-kv (fn [m id p] (update m (get action-of id) (fnil + 0.0) p)) {} acting)
        other (first (remove #(= (key %) (:action choice)) (ordered-masses marginal)))
        tied? (and other (= (val other) (:mass choice)))
        flips (into {}
                    (for [term [:habit :free-energy :G]]
                      (let [neutral (mapv
                                     (fn [c]
                                       (case term
                                         :habit (assoc c :habit 1.0)
                                         :free-energy (if (= :zero-support (:f-status c)) c
                                                        (assoc c :f 0.0 :f-status :attached))
                                         :G (if (and (number? (:g c)) (Double/isFinite (double (:g c))))
                                              (assoc c :g 0.0) c))) candidates)
                            post (selection-posterior {:beta beta :candidates neutral})
                            active (into {} (filter (fn [[id _]] (some? (get action-of id)))) post)
                            next-choice (bayes-choice active action-of)]
                        [term {:flipped? (not= (:action choice) (:action next-choice))
                               :winner (select-keys next-choice [:action :mass])}])))
        flipped (into #{} (keep (fn [[term result]] (when (:flipped? result) term))) flips)]
    {:policy-comparison policy-record
     :action-comparison
     {:comparison-domain :action :winner (select-keys choice [:action :mass])
      :runner-up (if other {:action (key other) :mass (val other)} :no-competing-action)
      :flips flips :neutralization-support :preserve-exclusions
      :decided-by (cond tied? :tie-break (seq flipped) flipped :else :robust)
      :tie-break-rule (:tie-break-rule choice)}}))
