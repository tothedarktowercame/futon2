(ns futon2.aif.decision-gate
  "E1 decision gate (SPEC-flat-removal-and-cascade-decision, 2026-09-17).

   The single place that emits a tick's decision accepts exactly two kinds:

   1. A cascade decision, as emitted by futon2.aif.policy/select-action-cascades
      (policy.clj ~:891): :selection-law {:applied :cascade-selection-posterior},
      β recorded as :declared or :learned, the chosen action a member of the
      strictly minimal prediction class (recomputed here from the certificate's
      own G and predictions, never trusted from the decision's law fields).
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
      :beta-not-declared :no-query-time-slice :no-constructed-candidate
      :outer-task-selection-refused.

   Anything else throws {:error :inadmissible-decision :reason …}. That
   includes a flat {:action {:type …}} decision, a single-pattern cascade
   without receipts, a bare abstention with no refusals, a decision whose
   chosen mass is not the posterior marginal, and a missing β. There is no
   fallback and no silent default: refusing is the only alternative to
   admitting."
  (:require [futon2.aif.inner-abstention :as stop]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.observation-checks :as observations]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.aif.reading-interpretation-debt :as reading-debt]
            [futon2.aif.ticket-queue :as ticket-queue]))

(def ^:private allowed-refusal-kinds
  "The closed set of per-target refusal kinds (SPEC §Decision 4).
   :class-unknown-no-scalar-g (M-a-wmc-scaling, click 16) admits the
   all-declined-family abstention: the target's relation is unresolvable
   (codex-20 handoff B), so its candidates got no scalar G and the tick
   abstains with the admission refusals plus this one."
  #{:universe-not-admitted
    :no-admitted-interpretation
    :want-not-declared
    :beta-not-declared
    :no-query-time-slice
    :no-constructed-candidate
    :outer-task-selection-refused
    :class-unknown-no-scalar-g
    ;; Q5 reading-policy route: a target whose policy family produced no
    ;; scored policy (its typed failures ride on the refusal), and a field
    ;; whose declared temperatures differ
    :no-policy-family
    :no-candidates
    :incommensurable-family
    ;; the reading family's precision or preference certification refused
    ;; before selection (reading-policy-precision / -preference)
    :model-contract-refused
    ;; Requirements Q6: every admissible policy is the previous run's
    ;; refused choice on the same selection inputs (previous-run)
    :repeat-choice-after-refusal
    ;; Requirements Q11: no strict minimum exists (cascade-selection)
    :tie-at-minimum
    :no-admissible-class
    :pick-not-below-abstention
    :computed-kernel-not-consumed})

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
  cascade-first-action). For co-application precedence it is the ordered
  vector of root pattern maps, the units nothing descends into, so two
  co-apply cascades with the same roots share one key. Taking the first
  entry of the {:co-apply ...} map instead keyed each cascade by its whole
  arrangement, split the mass the selector pooled, and refused the
  selector's Bayes action on a reading whose policies share a root
  (chosen 0.166 vs best 0.417, 2026-10-16)."
  [action]
  (if (map? action)
    (let [precedence (:precedence action)
          co-apply (when (map? precedence) (:co-apply precedence))]
      (cond
        (and (vector? precedence) (seq precedence)) (first precedence)
        (map? co-apply)
        (let [{:keys [units descent patterns]} co-apply
              below (set (map second descent))
              roots (mapv patterns (remove below units))]
          (when (seq roots) roots))
        :else (:type action)))
    action))

(defn- cascade-decision?
  [decision]
  (= :cascade-selection-posterior
     (get-in decision [:selection-law :applied])))

(defn- check-beta!
  [decision]
  (let [{:keys [value status]} (:beta decision)]
    (when-not (and (map? (:beta decision))
                   (or (contains? #{:declared :learned} status)
                       (and (= :r20-computed status)
                            (let [r (get-in decision [:selection-certificate :reading-precision-carry])]
                              (and (= :wm/r20-reading-precision-v1 (:schema r))
                                   (= :computed (:status r))
                                   (= :r20-beta-from-valence (:authority r))
                                   (= value (:beta-posterior r))
                                   ((requiring-resolve 'futon2.aif.reading-policy-precision/intact?) r)))))
                   (number? value)
                   (pos? value))
      (refuse! :beta-not-recorded
               {:beta (:beta decision)
                :required {:value "positive number" :status "declared, learned, or sealed r20-computed"}}))))

(defn- reading-policy? [candidate]
  (map? (get-in candidate [:precedence :co-apply])))

(defn- check-reading-policy!
  "A reading policy is admitted by its structural identity, not by
  interpretation receipts: interpreting its owed nodes is post-selection debt
  (Q5). Its carried history key must be the one its structure determines."
  [candidate]
  (let [key (prefix-admission/reading-policy-key candidate)]
    (when-not (and (some? key) (= key (:policy-key candidate)))
      (refuse! :reading-policy-identity-invalid
               {:candidate-id (:id candidate) :target (:target candidate)}))
    ;; the debt it owes must be bound to that identity; an open debt is
    ;; admissible here and refused at enactment
    (let [{:keys [status reason]} (reading-debt/check candidate)]
      (when-not (= :valid status)
        (refuse! :reading-policy-debt-invalid
                 {:candidate-id (:id candidate) :reason reason})))))

(defn- check-interpreted-candidate!
  [candidate]
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
  (check-guard-locators! candidate))

(defn- check-candidate-receipts!
  [posterior]
  (doseq [candidate (keys posterior)]
    (when-not (and (map? candidate) (= :cascade-candidate (:kind candidate)))
      (refuse! :posterior-over-non-cascade {:candidate candidate}))
    (if (reading-policy? candidate)
      (check-reading-policy! candidate)
      (check-interpreted-candidate! candidate))))

(defn- certificate-rows
  "The certificate's candidates as `minimum-class` reads them, restricted to
  SUPPORTED, in the certificate's own (ranked) order. Recomputed from what was
  scored, so the gate does not take the selector's word."
  [decision supported]
  (let [certificate (:selection-certificate decision)
        scored (into {} (map (fn [[_ row]] [(:id row) row])) (:scoring certificate))]
    (for [{:keys [id g class-emission]} (:candidates certificate)
          :when (pos? (get supported id 0.0))]
      {:action id :g g :policy-key (when (map? id) (:policy-key id))
       :prediction-digest (selection/prediction-digest
                           (:steps (get scored id))
                           (get-in class-emission [:certificate :steps]))})))

(defn- check-minimum!
  "The chosen action is a member of the strictly minimal class among those a Q6
  refusal leaves, no class ties it, and it is not in a forbidden class."
  [decision supported]
  (let [rows (certificate-rows decision supported)
        q6 (get-in decision [:selection-certificate :q6])
        keys (:forbidden-keys q6)
        forbidden {:keys keys :classes (selection/forbidden-classes rows keys)}
        chosen (first (filter #(= (:action decision) (:action %)) rows))
        _ (when (and chosen (selection/forbidden-row? forbidden chosen))
            (refuse! :chosen-in-forbidden-class {:action (:action decision)
                                                 :forbidden-keys keys}))
        {:keys [status detail class]}
        (selection/minimum-class (remove #(selection/forbidden-row? forbidden %) rows))]
    (case status
      :unique (when-not (some #(= (:action decision) (:action %)) (:members class))
                (refuse! :chosen-not-minimum {:action (:action decision)
                                              :minimum (mapv :action (:members class))
                                              :g (:g class)}))
      :tied (refuse! :minimum-not-unique detail)
      (refuse! :no-acting-candidate {:reason :no-scored-candidate}))))

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
            receipt-of (fn [{:keys [status class]}]
                         (cond-> {:status status}
                           class (assoc :digest (:digest class) :g (:g class)
                                        :members (mapv :action (:members class)))))
            allowed (fn [supported]
                      (let [rows (certificate-rows decision supported)
                            ks (get-in decision [:selection-certificate :q6 :forbidden-keys])
                            bar {:keys ks :classes (selection/forbidden-classes rows ks)}]
                        (remove #(selection/forbidden-row? bar %) rows)))
            unrestricted (receipt-of (selection/minimum-class (allowed acting)))
            choice (if conditional
                     (receipt-of (selection/minimum-class (allowed conditional)))
                     unrestricted)]
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
                       (some #{(:action decision)} (:members choice))
                       (or (nil? front) (= (:ticket front) (get-in decision [:action :target]))))
          (refuse! :ticket-queue-choice-invalid {:eligible-targets targets}))
        conditional))))

(defn- check-stop! [decision]
  (when-let [carrier (get-in decision [:selection-certificate :abstention])]
    (stop/validate! carrier (get-in decision [:selection-certificate :candidates]))
    (when (= :abstain-on-task (get-in decision [:abstention :case]))
      (let [supported (or (get-in decision [:selection-certificate :ticket-queue :stratum-posterior])
                          (get-in decision [:selection-law :posterior]))
            rows (certificate-rows decision supported)
            ks (get-in decision [:selection-certificate :q6 :forbidden-keys])
            forbidden {:keys ks :classes (selection/forbidden-classes rows ks)}
            allowed (remove #(selection/forbidden-row? forbidden %) rows)
            minimum (selection/minimum-class allowed)
            reference (get-in minimum [:class :members 0 :action :id])]
        (when-not (and (= :unique (:status minimum))
                       (= reference (:chosen-reference carrier))
                       (:cross-lens carrier)
                       (every? #(>= (:g %) (get-in carrier [:per-policy (get-in % [:action :id]) :G]))
                               allowed))
          (refuse! :invalid-stop-minimum-reference {:reference (:chosen-reference carrier)}))))
    (when-let [action (:action decision)]
      (when-not (= (:id action) (:chosen-reference carrier))
        (refuse! :stop-reference-not-chosen {:reference (:chosen-reference carrier)}))
      (when-not (< (:g (first (filter #(= action (:id %))
                                      (get-in decision [:selection-certificate :candidates]))))
                   (get-in carrier [:per-policy (:id action) :G]))
        (refuse! :pick-not-below-abstention {:reference (:id action)})))))

(defn- check-cascade-decision!
  [decision]
  (check-stop! decision)
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
    ;; An EMPTY cascade has no first acting pattern. Absence is not an action
    ;; and cannot be chosen. This mirrors policy.clj's rule and is deliberately
    ;; NOT shared code: `gate-and-selector-agree-on-what-counts-as-an-action`
    ;; in the tests is what stops the two drifting apart.
    (let [acting (into {} (filter (fn [[c _]] (some? (first-acting-pattern c)))) posterior)
          conditional (check-queue! decision posterior acting)
          supported (if conditional (into {} (filter (comp pos? val)) conditional) acting)
          chosen-mass (:chosen-action-mass decision)]
      (when (nil? (first-acting-pattern (:action decision)))
        (refuse! :chosen-action-is-not-an-action
                 {:action (:action decision)
                  :reason :empty-cascade-has-no-first-acting-pattern}))
      (when (empty? acting)
        (refuse! :no-acting-candidate
                 {:candidates (count posterior)
                  :reason :every-candidate-is-an-empty-cascade}))
      (check-minimum! decision supported)
      (when-not (number? chosen-mass)
        (refuse! :chosen-mass-not-recorded {:chosen-action-mass chosen-mass})))))

(defn- abstention?
  [decision]
  (and (map? decision) (= :abstained (:status decision))))

(defn- check-abstention!
  [decision]
  (check-stop! decision)
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
