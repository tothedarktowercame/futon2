(ns futon2.aif.decision-gate
  "E1 decision gate (SPEC-flat-removal-and-cascade-decision, 2026-09-17).

   The single place that emits a tick's decision accepts exactly two kinds:

   1. A cascade decision, as emitted by futon2.aif.policy/select-action-cascades
      (policy.clj ~:891): :selection-law {:applied :cascade-selection-posterior},
      β recorded as :declared or :learned, the chosen action's probability equal
      to the MARGINAL of the recorded cascade posterior over candidates whose
      first acting pattern is that action (recomputed here from the decision's
      own :selection-law :posterior and each candidate's precedence — never
      trusted from :softmax-weights or :chosen-action-mass).
      An optional declared ticket queue restricts eligibility to its earliest
      admitted ticket; the same policy law chooses within that stratum. Both
      the full-family and conditional posterior remain recorded and checked.
      Every candidate carries a :construction-receipt and :interpretation-receipts (non-empty
      whenever the candidate's precedence is non-empty). Guard tokens in
      every candidate carry locators with the fields required by their
      production observation class (C3-C6).

      EMPTY CASCADES TAKE NO ACTION MASS. A candidate with no precedence has
      no first acting pattern; pooling those under a shared nil key let their
      COUNT decide a tick (2026-09-21-1789951020). They are excluded from the
      recomputed marginal, a chosen action that is itself an empty cascade is
      refused :chosen-action-is-not-an-action, and an all-empty roster is
      refused :no-acting-candidate.

   2. A typed abstention {:status :abstained :refusals […] } with a NON-EMPTY
      list of per-target refusals, each {:target … :kind k …} with k one of
      :universe-not-admitted :no-admitted-interpretation :want-not-declared
      :beta-not-declared :no-constructed-candidate.

   Anything else throws {:error :inadmissible-decision :reason …}. That
   includes a flat {:action {:type …}} decision, a single-pattern cascade
   without receipts, a bare abstention with no refusals, a decision whose
   chosen mass is not the posterior marginal, and a missing β. There is no
   fallback and no silent default: refusing is the only alternative to
   admitting."
  (:require [futon2.aif.cascade-selection :as selection]
            [futon2.aif.observation-checks :as observations]
            [futon2.aif.ticket-queue :as ticket-queue]))

(def ^:private allowed-refusal-kinds
  "The closed set of per-target refusal kinds (SPEC §Decision 4)."
  #{:universe-not-admitted
    :no-admitted-interpretation
    :want-not-declared
    :beta-not-declared
    :no-constructed-candidate})

(def ^:private mass-tolerance
  "The chosen-action probability must equal its posterior marginal this
  closely (SPEC E1: 'within 1e-9')."
  1e-9)

(defn- refuse!
  [reason detail]
  (throw (ex-info "Inadmissible decision"
                  {:error :inadmissible-decision
                   :reason reason
                   :detail detail})))

(defn- observation-locator-refusal
  "The observation check's own answer (observation-checks/locator-refusal,
  the one authority for each class's locator rule), in the gate's refusal
  shape: {:kind :no-locator :class c :missing [...]} (plus :rule when the
  class's rule is not a field list, as C8's exactly-one-of is),
  {:kind :no-mechanical-check :class c}, or {:kind
  :invalid-observation-locator}. This checks the locator, not whether its
  referenced artifact exists or the observation is true."
  [locator]
  (when-let [r (observations/locator-refusal locator)]
    (case (:kind r)
      :invalid-observation-locator {:kind :invalid-observation-locator}
      :no-mechanical-check {:kind :no-mechanical-check :class (:class locator)}
      (cond-> {:kind (:kind r) :class (:class locator) :missing (get-in r [:data :missing])}
        (get-in r [:data :rule]) (assoc :rule (get-in r [:data :rule]))))))

(defn- check-guard-locators!
  [candidate]
  (let [tokens (set (mapcat (fn [pattern]
                              (mapcat #(concat (:present %) (:absent %))
                                      (get-in pattern [:guard :clauses])))
                            (:precedence candidate)))
        refusals (into {} (keep (fn [token]
                                 (when-let [r (observation-locator-refusal
                                               (get (:observation-locators candidate) token))]
                                   [token r]))) tokens)]
    (when (seq refusals)
      (refuse! :missing-observation-locators
               {:candidate-id (or (:id candidate) (:cascade-id candidate))
                :target (:target candidate)
                :missing-tokens (vec (sort-by pr-str (keys refusals)))
                :locator-refusals refusals}))))

(defn- first-acting-pattern
  "The enacted step of a cascade candidate: its first acting pattern (the
  per-state projection ActionMarginal uses; same rule as policy.clj's
  cascade-first-action)."
  [action]
  (if (and (map? action) (seq (:precedence action)))
    (first (:precedence action))
    (if (map? action) (:type action) action)))

(defn- cascade-decision?
  [decision]
  (= :cascade-selection-posterior
     (get-in decision [:selection-law :applied])))

(defn- check-beta!
  [decision]
  (let [{:keys [value status]} (:beta decision)]
    (when-not (and (map? (:beta decision))
                   (contains? #{:declared :learned} status)
                   (number? value)
                   (pos? value))
      (refuse! :beta-not-recorded
               {:beta (:beta decision)
                :required {:value "positive number" :status "#{:declared :learned}"}}))))

(defn- check-candidate-receipts!
  [posterior]
  (doseq [candidate (keys posterior)]
    (when-not (and (map? candidate) (= :cascade-candidate (:kind candidate)))
      (refuse! :posterior-over-non-cascade {:candidate candidate}))
    (when-not (some? (:construction-receipt candidate))
      (refuse! :missing-construction-receipt
               {:candidate candidate}))
    (when-not (some? (:interpretation-receipts candidate))
      (refuse! :missing-interpretation-receipts
               {:candidate candidate}))
    (when (and (seq (:precedence candidate))
               (empty? (:interpretation-receipts candidate)))
      (refuse! :empty-interpretation-receipts
               {:candidate candidate
                :precedence-count (count (:precedence candidate))}))
    (check-guard-locators! candidate)))

(defn- marginal-mass
  "Sum the recorded posterior over candidates whose first acting pattern is
  `pattern`. Recomputed from the decision's own posterior; never reads
  :softmax-weights."
  [posterior pattern]
  (transduce (comp (filter (fn [[candidate _]]
                             (= (first-acting-pattern candidate) pattern)))
                   (map val))
             + 0.0 posterior))

(defn- check-queue!
  "Independently check the declared order and recompute both choices. The
   unrestricted posterior remains authoritative for recorded global mass;
   the same policy law is normalized within the declared eligible stratum."
  [decision posterior acting]
  (let [receipt (get-in decision [:selection-certificate :ticket-queue])
        law-receipt (get-in decision [:selection-law :ticket-queue])]
    (when (or receipt law-receipt)
      (when-not (= receipt law-receipt)
        (refuse! :ticket-queue-certificate-mismatch {}))
      (let [declaration (ticket-queue/validate! (:declaration receipt))
            candidates (get-in decision [:selection-certificate :candidates])
            _ (when-not (= (set (keys posterior)) (set (map :id candidates)))
                (refuse! :ticket-queue-candidates-mismatch {}))
            supported (set (map (comp :target :id) (filter ticket-queue/supported? candidates)))
            ordered (sort-by (juxt #(java.time.Instant/parse (:inserted-at %)) :ticket)
                             (:entries declaration))
            front (first (filter #(contains? supported (:ticket %)) ordered))
            targets (if front [(:ticket front)] [])
            ids (set (map :id (filter #(and (seq (get-in % [:id :precedence]))
                                          (= (:ticket front) (get-in % [:id :target]))) candidates)))
            conditional (when front
                          (selection/selection-posterior
                           {:beta (get-in decision [:beta :value])
                            :candidates (filterv #(contains? ids (:id %)) candidates)}))
            action-of (into {} (map (fn [id] [id (first-acting-pattern id)])) (keys posterior))
            unrestricted (selection/bayes-choice acting action-of)
            choice (if conditional (selection/bayes-choice conditional action-of) unrestricted)]
        (when-not (and (= :wm/ticket-queue-selection-v1 (:schema receipt))
                       (seq (:entries declaration))
                       (= targets (:eligible-targets receipt))
                       (= (mapv #(select-keys % [:ticket :inserted-at]) (:entries receipt)) (vec ordered))
                       (= (mapv #(if (contains? supported (:ticket %)) :admitted :not-admitted) ordered)
                          (mapv :status (:entries receipt)))
                       (= (if front :front-stratum :no-admitted-front-entry) (:status receipt))
                       (= (if front :ticket-queue :unrestricted-policy) (:decided-by receipt))
                       (= conditional (:stratum-posterior receipt))
                       (= unrestricted (:unrestricted-choice receipt))
                       (= choice (:choice receipt))
                       (= (:action choice) (first-acting-pattern (:action decision)))
                       (or (nil? front) (= (:ticket front) (get-in decision [:action :target]))))
          (refuse! :ticket-queue-choice-invalid {:eligible-targets targets}))
        conditional))))

(defn- check-cascade-decision!
  [decision]
  (check-beta! decision)
  (let [posterior (get-in decision [:selection-law :posterior])]
    (when-not (and (map? posterior) (seq posterior))
      (refuse! :missing-recorded-posterior
               {:selection-law (:selection-law decision)}))
    (check-candidate-receipts! posterior)
    (let [total (reduce + 0.0 (vals posterior))]
      (when (> (abs (- total 1.0)) mass-tolerance)
        (refuse! :posterior-not-normalised {:total total})))
    (when-not (contains? posterior (:action decision))
      (refuse! :chosen-action-not-a-candidate {:action (:action decision)}))
    ;; An EMPTY cascade has no first acting pattern, so its key here is nil.
    ;; Summing those together pools every structurally distinct do-nothing
    ;; into one key whose mass grows with the roster and cannot lose --
    ;; 21 of them carried 0.785275 against 0.179031 for the best acting key in
    ;; run 2026-09-21-1789951020. Absence is not an action and takes no action
    ;; mass. This mirrors policy.clj's rule and is deliberately NOT shared code:
    ;; this gate recomputes independently, and `gate-and-selector-agree-on-what-
    ;; counts-as-an-action` in the tests is what stops the two drifting apart.
    (let [acting (into {} (filter (fn [[c _]] (some? (first-acting-pattern c)))) posterior)
          marginals (reduce (fn [m [c p]] (update m (first-acting-pattern c) (fnil + 0.0) p))
                            {} acting)
          chosen-pattern (first-acting-pattern (:action decision))
          conditional (check-queue! decision posterior acting)
          eligible-marginals (if conditional
                               (reduce-kv (fn [m c p] (update m (first-acting-pattern c) (fnil + 0.0) p)) {} conditional)
                               marginals)]
      (when (nil? chosen-pattern)
        (refuse! :chosen-action-is-not-an-action
                 {:action (:action decision)
                  :reason :empty-cascade-has-no-first-acting-pattern}))
      (when (empty? marginals)
        (refuse! :no-acting-candidate
                 {:candidates (count posterior)
                  :reason :every-candidate-is-an-empty-cascade}))
      (let [best (apply max (vals eligible-marginals))]
        ;; the enacted step is the Bayes action in the declared stratum: no first acting pattern may
        ;; carry more marginal mass than the chosen one (ties are the selector's
        ;; declared tie-break, so equality is admitted)
        (when (> (- best (get eligible-marginals chosen-pattern 0.0)) mass-tolerance)
          (refuse! :chosen-not-bayes-action
                   {:chosen chosen-pattern :chosen-marginal (get marginals chosen-pattern)
                    :best-marginal best}))))
    (let [chosen (:action decision)
          chosen-mass (:chosen-action-mass decision)
          marginal (marginal-mass posterior (first-acting-pattern chosen))]
      (when-not (and (number? chosen-mass)
                     (if (seq (get-in decision [:selection-certificate :ticket-queue :eligible-targets]))
                       ;; A finite-support queued policy can underflow in the
                       ;; full family; the checked conditional mass is positive.
                       (and (not (neg? chosen-mass))
                            (pos? (get-in decision [:selection-certificate :ticket-queue :choice :mass])))
                       (pos? chosen-mass)))
        (refuse! :chosen-mass-not-recorded {:chosen-action-mass chosen-mass}))
      (when (> (abs (- (double chosen-mass) (double marginal))) mass-tolerance)
        (refuse! :chosen-mass-not-marginal
                 {:chosen-action-mass chosen-mass
                  :recomputed-marginal marginal
                  :first-acting-pattern (first-acting-pattern chosen)})))))

(defn- abstention?
  [decision]
  (and (map? decision) (= :abstained (:status decision))))

(defn- check-abstention!
  [decision]
  (let [refusals (:refusals decision)]
    (when-not (and (sequential? refusals) (seq refusals))
      (refuse! :empty-refusals {:refusals refusals}))
    (doseq [r refusals
            :when (not (and (map? r)
                            (contains? r :target)
                            (allowed-refusal-kinds (:kind r))))]
      (refuse! :unknown-refusal-kind {:refusal r}))))

(defn emit!
  "Emit the tick's decision. Returns `decision` unchanged when it is an
  admissible cascade decision or a typed abstention; otherwise throws
  ex-info {:error :inadmissible-decision :reason …}. No fallback, no
  repair, no default."
  [decision]
  (cond
    (abstention? decision) (do (check-abstention! decision) decision)
    (cascade-decision? decision) (do (check-cascade-decision! decision) decision)
    (and (map? decision) (get-in decision [:action :type]))
    (refuse! :flat-action {:action-type (get-in decision [:action :type])})
    :else (refuse! :not-a-decision {:decision decision})))
