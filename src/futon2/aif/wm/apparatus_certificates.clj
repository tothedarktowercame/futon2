(ns futon2.aif.wm.apparatus-certificates
  "Fail-closed projection of retained run inputs into the R20 certificate.")

(def schema :wm/r20-certificate-v1)
(def lean-authority {:module "DarkTower.WarMachine.R20Certificate" :mathlib4 "bd04d997aa"})
(def node-order [:precision :evidence :habit :class-preference :strategic-focus
                 :liveness :independence :joint-action])
(def risk-tolerance 1.0e-9)

(defn- absent [node missing & [reason checked]]
  (cond-> {:node node :verdict :not-recorded :missing (vec missing)}
    reason (assoc :reason reason) checked (assoc :checked checked)))
(defn- verdict [node agrees? case checked & [reason]]
  (cond-> {:node node :verdict (if agrees? :agrees :disagrees)
           :case case :checked checked}
    reason (assoc :reason reason)))
(defn- finite-number? [x] (and (number? x) (Double/isFinite (double x))))
(defn- close? [a b] (and (finite-number? a) (finite-number? b)
                          (<= (Math/abs (- (double a) (double b))) risk-tolerance)))

(defn- precision-row [decision]
  (let [path [:selection-certificate :policy-precision-state]
        state (get-in decision path) update (:update state)
        before (:beta-prior update) after (:beta update) solve (:solve update)
        case (when (and (finite-number? before) (finite-number? after))
               (cond (= (double after) (double before)) :unchanged
                     (< (double after) (double before)) :more-decisive
                     :else :less-decisive))]
    (if-not (map? update)
      (absent :precision [:update] (or (:reason state) :precision-update-not-retained)
              {:source path :carry (select-keys state [:status :reason :beta :beta-status])})
      (absent :precision [:prior :posterior] :policy-distributions-not-retained
              {:source path :beta-prior before :beta after
               :solve-converged? (:converged? solve)
               :solve-residual (or (:residual solve) (:error solve) (:precision-error solve))
               :case-if-recorded case}))))

(defn- constant-row [node missing]
  (absent node missing :required-r20-record-fields-not-retained))

(defn- identity-at [participants role]
  (let [v (get-in participants [:roles role])]
    {:path [:participants :roles role] :receipt v
     :identity (when (= :present (:status v)) (:identity v))}))
(defn- independence-row [participants]
  (let [author (identity-at participants :author)
        reviewer (identity-at participants :reviewer-of-record)
        missing (cond-> [] (nil? (:identity author)) (conj :author)
                         (nil? (:identity reviewer)) (conj :reviewer-of-record))
        checked {:author author :reviewer reviewer}]
    (if (seq missing)
      (absent :independence missing :participant-identity-not-observed checked)
      (let [different? (not= (:identity author) (:identity reviewer))]
        (verdict :independence different?
                 (if different? :author-is-not-reviewer :same-agent) checked)))))

(def terminal-preference {:focused 55/100 :related 35/100 :unrelated 5/100 :stop-the-line 5/100})
(def waiting-preference {:ending/not-yet-evaluated 1})
(defn- canonical-preference [horizon tau]
  (if (= tau horizon) terminal-preference waiting-preference))
(defn- unsupported? [prediction preference]
  (boolean (some (fn [[class mass]]
                   (and (number? mass) (pos? mass) (zero? (get preference class 0)))) prediction)))
(defn- class-risk [prediction preference]
  (if (unsupported? prediction preference) :infinite
      (reduce-kv (fn [sum class mass]
                   (if (zero? mass) sum
                       (+ sum (* (double mass)
                                 (Math/log (/ (double mass) (double (get preference class))))))))
                 0.0 prediction)))
(defn- class-step-check [horizon preferences step]
  (let [tau (:tau step) prediction (:prediction step) reported (:risk step)
        preferred (get preferences tau) canonical (canonical-preference horizon tau)
        expected (when (and (map? prediction) (map? preferred)) (class-risk prediction preferred))
        risk-ok? (if (= :infinite expected) (= :infinite reported) (close? expected reported))]
    {:tau tau :prediction prediction :reported-risk reported :preference preferred
     :canonical-preference canonical :preference-ok? (= canonical preferred)
     :computed-risk expected :risk-ok? risk-ok?
     :ok? (and (integer? tau) (map? prediction) (= canonical preferred) risk-ok?)}))
(defn- class-preference-row [decision]
  (let [path [:selection-certificate :scoring]
        scored (vals (or (get-in decision path) {}))
        entries (filter #(= :class-emission (get-in % [:observation-model :kind])) scored)]
    (cond
      (empty? entries) (absent :class-preference [:class-emission-scoring]
                               :no-scored-class-emission-candidate {:source path})
      (some #(not= :none (get-in % [:g-terms :normalization])) entries)
      (absent :class-preference [:unnormalized-risk] :recorded-risk-is-normalized
              {:source path :normalizations (mapv #(get-in % [:g-terms :normalization]) entries)})
      :else
      (let [checks (mapv (fn [entry]
                           (let [model (:observation-model entry) horizon (:horizon model)]
                             {:id (:id entry) :horizon horizon
                              :steps (mapv #(class-step-check horizon (:class-preference model) %)
                                           (:steps entry))})) entries)
            all-steps (mapcat :steps checks)
            selected-id (:action decision)
            selected (first (filter #(= selected-id (:id %)) checks))
            terminal (first (filter #(= (:tau %) (:horizon selected)) (:steps selected)))
            case (cond (nil? terminal) :before-horizon
                       (= :infinite (:computed-risk terminal)) :at-horizon-unsupported
                       :else :at-horizon-supported)
            agrees? (and (seq all-steps) (every? :ok? all-steps))]
        (verdict :class-preference agrees? case
                 {:source path :risk-tolerance risk-tolerance :candidates checks
                  :selected-id selected-id}
                 (when-not agrees? :class-preference-or-risk-mismatch))))))

;; CTauClassPreference.lean:124-138 and R15StrategicTarget.lean:64-67:
;; focus -> focused, associated -> related, usefulElsewhere -> unrelated;
;; other relations are unscored/unknown. The focus receipt keeps the relation
;; vocabulary in :class; :scored-class below records its Lean projection, and
;; :consumed-class is what the scorer's class model held for that target.
(def scorer-class {:focus :focused :associated :related :useful-elsewhere :unrelated})
(defn- relation-key [relation]
  (let [relation (if (map? relation) (:relation relation) relation)]
    (cond (keyword? relation) relation (string? relation) (keyword relation) :else :other)))
(defn- consumed-target-classes
  "The class the scorer consumed for each target: the :target-class map of
   the class-emission model retained with each scored candidate. nil when no
   class-emission score is on the decision."
  [decision]
  (let [maps (keep #(when (= :class-emission (get-in % [:observation-model :kind]))
                      (get-in % [:observation-model :target-class]))
                   (vals (or (get-in decision [:selection-certificate :scoring]) {})))]
    (when (seq maps) (apply merge maps))))

(defn- strategic-focus-row [decision]
  (let [path [:selection-certificate :focus-receipt] receipt (get-in decision path)
        consumed-path [:selection-certificate :scoring :* :observation-model :target-class]
        consumed (consumed-target-classes decision)
        previous (get-in receipt [:context :previous-focus :focus])
        current (get-in receipt [:discovery :focus])
        unscored (when consumed
                   (vec (remove #(contains? consumed (:target %)) (:candidates receipt))))
        missing (cond-> []
                  (nil? previous) (conj :previous-focus)
                  (nil? current) (conj :focus)
                  (or (nil? consumed) (seq unscored)) (conj :scored-class))]
    (if (seq missing)
      (absent :strategic-focus missing :focus-record-not-retained
              (cond-> {:source path :consumed-source consumed-path}
                (seq unscored) (assoc :targets-without-scored-class (mapv :target unscored))))
      (let [checks (mapv (fn [candidate]
                           (let [relation (relation-key (:relation candidate))
                                 scored (get scorer-class relation)
                                 expected-recorded (if scored relation :unknown)
                                 consumed-class (get consumed (:target candidate))
                                 recorded-ok? (= expected-recorded (:class candidate))
                                 consumed-ok? (= (or scored :unknown) consumed-class)]
                             {:target (:target candidate) :relation (:relation candidate)
                              :recorded-class (:class candidate) :scored-class scored
                              :consumed-class consumed-class
                              :recorded-ok? recorded-ok? :consumed-ok? consumed-ok?
                              :ok? (and recorded-ok? consumed-ok?)}))
                         (:candidates receipt))
            agrees? (and (seq checks) (every? :ok? checks))]
        (verdict :strategic-focus agrees? (if (= previous current) :focus-kept :focus-changed)
                 {:source path :consumed-source consumed-path
                  :previous-focus previous :focus current :candidates checks}
                 (when-not agrees? :relation-class-mismatch))))))

(defn receipt [{:keys [decision participants]}]
  (let [rows [(precision-row decision)
              (constant-row :evidence [:likelihood])
              (constant-row :habit [:counts-after :next-consumed])
              (class-preference-row decision)
              (strategic-focus-row decision)
              (constant-row :liveness [:before-state :after-state :reported-live])
              (independence-row participants)
              (constant-row :joint-action [:ownership])]]
    {:schema schema :lean lean-authority :rows rows
     :certified? (every? #(= :agrees (:verdict %)) rows)}))
